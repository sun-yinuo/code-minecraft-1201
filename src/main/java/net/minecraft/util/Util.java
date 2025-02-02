package net.minecraft.util;

import com.google.common.base.Ticker;
import com.google.common.collect.Iterators;
import com.google.common.collect.Lists;
import com.google.common.util.concurrent.MoreExecutors;
import com.mojang.datafixers.DataFixUtils;
import com.mojang.datafixers.DSL.TypeReference;
import com.mojang.datafixers.types.Type;
import com.mojang.logging.LogUtils;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.DataResult.PartialResult;
import it.unimi.dsi.fastutil.Hash.Strategy;
import it.unimi.dsi.fastutil.ints.IntArrayList;
import it.unimi.dsi.fastutil.objects.Object2IntMap;
import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import java.io.File;
import java.io.IOException;
import java.lang.management.ManagementFactory;
import java.lang.management.RuntimeMXBean;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.nio.file.spi.FileSystemProvider;
import java.security.AccessController;
import java.security.PrivilegedActionException;
import java.time.Duration;
import java.time.Instant;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.Map.Entry;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.ForkJoinWorkerThread;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.BiFunction;
import java.util.function.BooleanSupplier;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.IntFunction;
import java.util.function.Predicate;
import java.util.function.Supplier;
import java.util.function.ToIntFunction;
import java.util.stream.Collector;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.LongStream;
import java.util.stream.Stream;
import net.minecraft.Bootstrap;
import net.minecraft.SharedConstants;
import net.minecraft.datafixer.Schemas;
import net.minecraft.state.property.Property;
import net.minecraft.util.crash.CrashException;
import net.minecraft.util.function.CharPredicate;
import net.minecraft.util.logging.UncaughtExceptionLogger;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.random.Random;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;

/**
 * A class holding various utility methods.
 */
public class Util {
	static final Logger LOGGER = LogUtils.getLogger();
	private static final int MAX_PARALLELISM = 255;
	private static final String MAX_BG_THREADS_PROPERTY = "max.bg.threads";
	private static final AtomicInteger NEXT_WORKER_ID = new AtomicInteger(1);
	private static final ExecutorService MAIN_WORKER_EXECUTOR = createWorker("Main");
	private static final ExecutorService IO_WORKER_EXECUTOR = createIoWorker();
	/**
	 * A locale-independent datetime formatter that uses {@code yyyy-MM-dd_HH.mm.ss}
	 * as the format string. Example: {@code 2022-01-01_00.00.00}
	 */
	private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd_HH.mm.ss", Locale.ROOT);
	public static TimeSupplier.Nanoseconds nanoTimeSupplier = System::nanoTime;
	public static final Ticker TICKER = new Ticker() {
		@Override
		public long read() {
			return Util.nanoTimeSupplier.getAsLong();
		}
	};
	/**
	 * The "nil UUID" that represents lack of a UUID.
	 */
	public static final UUID NIL_UUID = new UUID(0L, 0L);
	/**
	 * The file system provider for handling jar and zip files.
	 */
	public static final FileSystemProvider JAR_FILE_SYSTEM_PROVIDER = (FileSystemProvider)FileSystemProvider.installedProviders()
		.stream()
		.filter(fileSystemProvider -> fileSystemProvider.getScheme().equalsIgnoreCase("jar"))
		.findFirst()
		.orElseThrow(() -> new IllegalStateException("No jar file system provider found"));
	private static Consumer<String> missingBreakpointHandler = message -> {
	};

	public static <K, V> Collector<Entry<? extends K, ? extends V>, ?, Map<K, V>> toMap() {
		return Collectors.toMap(Entry::getKey, Entry::getValue);
	}

	public static <T extends Comparable<T>> String getValueAsString(Property<T> property, Object value) {
		return property.name((T)value);
	}

	/**
	 * {@return the translation key constructed from {@code type} and {@code id}}
	 * 
	 * <p>If {@code id} is {@code null}, {@code unregistered_sadface} is used as the ID.
	 * 
	 * @see Identifier#toTranslationKey(String)
	 */
	public static String createTranslationKey(String type, @Nullable Identifier id) {
		return id == null ? type + ".unregistered_sadface" : type + "." + id.getNamespace() + "." + id.getPath().replace('/', '.');
	}

	/**
	 * {@return the current time in milliseconds, to be used for measuring a duration}
	 * 
	 * <p>This is not the Unix epoch time, and can only be used to determine the duration
	 * between two calls of this method.
	 * 
	 * @see #getMeasuringTimeNano
	 * @see #getEpochTimeMs
	 */
	public static long getMeasuringTimeMs() {
		return getMeasuringTimeNano() / 1000000L;
	}

	/**
	 * {@return the current time in nanoseconds, to be used for measuring a duration}
	 * 
	 * <p>This is not the Unix epoch time, and can only be used to determine the duration
	 * between two calls of this method.
	 * 
	 * @see #getMeasuringTimeMs
	 * @see #getEpochTimeMs
	 */
	public static long getMeasuringTimeNano() {
		return nanoTimeSupplier.getAsLong();
	}

	/**
	 * {@return the milliseconds passed since the Unix epoch}
	 * 
	 * <p>This should be used to display or store the current time. {@link #getMeasuringTimeMs}
	 * should be used for determining the duration between two calls.
	 * 
	 * @see #getMeasuringTimeMs
	 * @see #getMeasuringTimeNano
	 */
	public static long getEpochTimeMs() {
		return Instant.now().toEpochMilli();
	}

	/**
	 * {@return the current time formatted using {@link #DATE_TIME_FORMATTER}}
	 */
	public static String getFormattedCurrentTime() {
		return DATE_TIME_FORMATTER.format(ZonedDateTime.now());
	}

	private static ExecutorService createWorker(String name) {
		int i = MathHelper.clamp(Runtime.getRuntime().availableProcessors() - 1, 1, getMaxBackgroundThreads());
		ExecutorService executorService;
		if (i <= 0) {
			executorService = MoreExecutors.newDirectExecutorService();
		} else {
			executorService = new ForkJoinPool(i, forkJoinPool -> {
				ForkJoinWorkerThread forkJoinWorkerThread = new ForkJoinWorkerThread(forkJoinPool) {
					protected void onTermination(Throwable throwable) {
						if (throwable != null) {
							Util.LOGGER.warn("{} died", this.getName(), throwable);
						} else {
							Util.LOGGER.debug("{} shutdown", this.getName());
						}

						super.onTermination(throwable);
					}
				};
				forkJoinWorkerThread.setName("Worker-" + name + "-" + NEXT_WORKER_ID.getAndIncrement());
				return forkJoinWorkerThread;
			}, Util::uncaughtExceptionHandler, true);
		}

		return executorService;
	}

	private static int getMaxBackgroundThreads() {
		String string = System.getProperty("max.bg.threads");
		if (string != null) {
			try {
				int i = Integer.parseInt(string);
				if (i >= 1 && i <= 255) {
					return i;
				}

				LOGGER.error("Wrong {} property value '{}'. Should be an integer value between 1 and {}.", "max.bg.threads", string, 255);
			} catch (NumberFormatException var2) {
				LOGGER.error("Could not parse {} property value '{}'. Should be an integer value between 1 and {}.", "max.bg.threads", string, 255);
			}
		}

		return 255;
	}

	/**
	 * {@return the main worker executor for miscellaneous asynchronous tasks}
	 */
	public static ExecutorService getMainWorkerExecutor() {
		return MAIN_WORKER_EXECUTOR;
	}

	/**
	 * {@return the executor for disk or network IO tasks}
	 */
	public static ExecutorService getIoWorkerExecutor() {
		return IO_WORKER_EXECUTOR;
	}

	public static void shutdownExecutors() {
		attemptShutdown(MAIN_WORKER_EXECUTOR);
		attemptShutdown(IO_WORKER_EXECUTOR);
	}

	private static void attemptShutdown(ExecutorService service) {
		service.shutdown();

		boolean bl;
		try {
			bl = service.awaitTermination(3L, TimeUnit.SECONDS);
		} catch (InterruptedException var3) {
			bl = false;
		}

		if (!bl) {
			service.shutdownNow();
		}
	}

	private static ExecutorService createIoWorker() {
		return Executors.newCachedThreadPool(runnable -> {
			Thread thread = new Thread(runnable);
			thread.setName("IO-Worker-" + NEXT_WORKER_ID.getAndIncrement());
			thread.setUncaughtExceptionHandler(Util::uncaughtExceptionHandler);
			return thread;
		});
	}

	/**
	 * Throws {@code t} if it's a {@link RuntimeException} (or any of its subclass), otherwise
	 * {@code t} wrapped in a RuntimeException.
	 * 
	 * <p>{@link Error} is wrapped as well, despite being unchecked.
	 */
	public static void throwUnchecked(Throwable t) {
		throw t instanceof RuntimeException ? (RuntimeException)t : new RuntimeException(t);
	}

	private static void uncaughtExceptionHandler(Thread thread, Throwable t) {
		throwOrPause(t);
		if (t instanceof CompletionException) {
			t = t.getCause();
		}

		if (t instanceof CrashException) {
			Bootstrap.println(((CrashException)t).getReport().asString());
			System.exit(-1);
		}

		LOGGER.error(String.format(Locale.ROOT, "Caught exception in thread %s", thread), t);
	}

	@Nullable
	public static Type<?> getChoiceType(TypeReference typeReference, String id) {
		return !SharedConstants.useChoiceTypeRegistrations ? null : getChoiceTypeInternal(typeReference, id);
	}

	@Nullable
	private static Type<?> getChoiceTypeInternal(TypeReference typeReference, String id) {
		Type<?> type = null;

		try {
			type = Schemas.getFixer().getSchema(DataFixUtils.makeKey(SharedConstants.getGameVersion().getSaveVersion().getId())).getChoiceType(typeReference, id);
		} catch (IllegalArgumentException var4) {
			LOGGER.error("No data fixer registered for {}", id);
			if (SharedConstants.isDevelopment) {
				throw var4;
			}
		}

		return type;
	}

	public static Runnable debugRunnable(String activeThreadName, Runnable task) {
		return SharedConstants.isDevelopment ? () -> {
			Thread thread = Thread.currentThread();
			String string2 = thread.getName();
			thread.setName(activeThreadName);

			try {
				task.run();
			} finally {
				thread.setName(string2);
			}
		} : task;
	}

	public static <V> Supplier<V> debugSupplier(String activeThreadName, Supplier<V> supplier) {
		return SharedConstants.isDevelopment ? () -> {
			Thread thread = Thread.currentThread();
			String string2 = thread.getName();
			thread.setName(activeThreadName);

			Object var4;
			try {
				var4 = supplier.get();
			} finally {
				thread.setName(string2);
			}

			return var4;
		} : supplier;
	}

	/**
	 * {@return the operating system instance for the current platform}
	 * 
	 * @implNote This uses the {@code os.name} system property to determine the operating system.
	 * @apiNote This is used for opening links.
	 */
	public static OperatingSystem getOperatingSystem() {
		String string = System.getProperty("os.name").toLowerCase(Locale.ROOT);
		if (string.contains("win")) {
			return OperatingSystem.WINDOWS;
		} else if (string.contains("mac")) {
			return OperatingSystem.OSX;
		} else if (string.contains("solaris")) {
			return OperatingSystem.SOLARIS;
		} else if (string.contains("sunos")) {
			return OperatingSystem.SOLARIS;
		} else if (string.contains("linux")) {
			return OperatingSystem.LINUX;
		} else {
			return string.contains("unix") ? OperatingSystem.LINUX : OperatingSystem.UNKNOWN;
		}
	}

	/**
	 * {@return a stream of JVM flags passed when launching}
	 * 
	 * <p>The streamed strings include the {@code -X} prefix.
	 */
	public static Stream<String> getJVMFlags() {
		RuntimeMXBean runtimeMXBean = ManagementFactory.getRuntimeMXBean();
		return runtimeMXBean.getInputArguments().stream().filter(runtimeArg -> runtimeArg.startsWith("-X"));
	}

	/**
	 * {@return the last item of {@code list}}
	 * 
	 * @throws IndexOutOfBoundsException if {@code list} is empty
	 */
	public static <T> T getLast(List<T> list) {
		return (T)list.get(list.size() - 1);
	}

	/**
	 * {@return the item succeeding {@code object} in {@code iterable}}
	 * 
	 * @implNote If {@code object} is {@code null}, this returns the first item of the iterable.
	 * If {@code object} is not in {@code iterable}, this enters into an infinite loop.
	 * {@code object} is compared using the {@code ==} operator.
	 */
	public static <T> T next(Iterable<T> iterable, @Nullable T object) {
		Iterator<T> iterator = iterable.iterator();
		T object2 = (T)iterator.next();
		if (object != null) {
			T object3 = object2;

			while (object3 != object) {
				if (iterator.hasNext()) {
					object3 = (T)iterator.next();
				}
			}

			if (iterator.hasNext()) {
				return (T)iterator.next();
			}
		}

		return object2;
	}

	/**
	 * {@return the item preceding {@code object} in {@code iterable}}
	 * 
	 * <p>If {@code object} is not in {@code iterable}, this returns the last item of the iterable.
	 * {@code object} is compared using the {@code ==} operator.
	 */
	public static <T> T previous(Iterable<T> iterable, @Nullable T object) {
		Iterator<T> iterator = iterable.iterator();
		T object2 = null;

		while (iterator.hasNext()) {
			T object3 = (T)iterator.next();
			if (object3 == object) {
				if (object2 == null) {
					object2 = iterator.hasNext() ? Iterators.getLast(iterator) : object;
				}
				break;
			}

			object2 = object3;
		}

		return object2;
	}

	/**
	 * {@return the value supplied from {@code factory}}
	 * 
	 * <p>This is useful when initializing static fields:
	 * <pre>{@code
	 * private static final Map<String, String> MAP = Util.make(() -> {
	 *     Map<String, String> map = new HashMap<>();
	 *     map.put("example", "hello");
	 *     return map;
	 * });
	 * }</pre>
	 */
	public static <T> T make(Supplier<T> factory) {
		return (T)factory.get();
	}

	/**
	 * {@return {@code object} initialized with {@code initializer}}
	 * 
	 * <p>This is useful when initializing static fields:
	 * <pre>{@code
	 * private static final Map<String, String> MAP = Util.make(new HashMap<>(), (map) -> {
	 *     map.put("example", "hello");
	 * });
	 * }</pre>
	 */
	public static <T> T make(T object, Consumer<T> initializer) {
		initializer.accept(object);
		return object;
	}

	/**
	 * {@return the {@link Hash.Strategy} that uses identity comparison}
	 * 
	 * <p>fastutil's "reference" object types should be used instead in most cases.
	 */
	public static <K> Strategy<K> identityHashStrategy() {
		return IdentityHashStrategy.INSTANCE;
	}

	/**
	 * Combines a list of {@code futures} into one future that holds a list
	 * of their results.
	 * 
	 * <p>This version expects all futures to complete successfully and is not
	 * optimized in case any of the input futures throws.
	 * 
	 * @return the combined future
	 * @see #combine(List)
	 * 
	 * @param futures the completable futures to combine
	 */
	public static <V> CompletableFuture<List<V>> combineSafe(List<? extends CompletableFuture<V>> futures) {
		if (futures.isEmpty()) {
			return CompletableFuture.completedFuture(List.of());
		} else if (futures.size() == 1) {
			return ((CompletableFuture)futures.get(0)).thenApply(List::of);
		} else {
			CompletableFuture<Void> completableFuture = CompletableFuture.allOf((CompletableFuture[])futures.toArray(new CompletableFuture[0]));
			return completableFuture.thenApply(void_ -> futures.stream().map(CompletableFuture::join).toList());
		}
	}

	/**
	 * Combines a list of {@code futures} into one future that holds a list
	 * of their results.
	 * 
	 * <p>The returned future is fail-fast; if any of the input futures fails,
	 * this returned future will be immediately completed exceptionally than
	 * waiting for other input futures.
	 * 
	 * @return the combined future
	 * @see #combineCancellable(List)
	 * @see #combineSafe(List)
	 * 
	 * @param futures the completable futures to combine
	 */
	public static <V> CompletableFuture<List<V>> combine(List<? extends CompletableFuture<? extends V>> futures) {
		CompletableFuture<List<V>> completableFuture = new CompletableFuture();
		return combine(futures, completableFuture::completeExceptionally).applyToEither(completableFuture, Function.identity());
	}

	/**
	 * Combines a list of {@code futures} into one future that holds a list
	 * of their results.
	 * 
	 * <p>The returned future is fail-fast; if any of the input futures fails,
	 * this returned future will be immediately completed exceptionally than
	 * waiting for other input futures. Additionally, all other futures will
	 * be canceled.
	 * 
	 * @return the combined future
	 * @see #combine(List)
	 * @see #combineSafe(List)
	 */
	public static <V> CompletableFuture<List<V>> combineCancellable(List<? extends CompletableFuture<? extends V>> futures) {
		CompletableFuture<List<V>> completableFuture = new CompletableFuture();
		return combine(futures, throwable -> {
			if (completableFuture.completeExceptionally(throwable)) {
				for (CompletableFuture<? extends V> completableFuture2 : futures) {
					completableFuture2.cancel(true);
				}
			}
		}).applyToEither(completableFuture, Function.identity());
	}

	private static <V> CompletableFuture<List<V>> combine(List<? extends CompletableFuture<? extends V>> futures, Consumer<Throwable> exceptionHandler) {
		List<V> list = Lists.<V>newArrayListWithCapacity(futures.size());
		CompletableFuture<?>[] completableFutures = new CompletableFuture[futures.size()];
		futures.forEach(future -> {
			int i = list.size();
			list.add(null);
			completableFutures[i] = future.whenComplete((value, throwable) -> {
				if (throwable != null) {
					exceptionHandler.accept(throwable);
				} else {
					list.set(i, value);
				}
			});
		});
		return CompletableFuture.allOf(completableFutures).thenApply(void_ -> list);
	}

	/**
	 * If {@code optional} has value, calls {@code presentAction} with the value,
	 * otherwise calls {@code elseAction}.
	 * 
	 * @return the passed {@code optional}
	 */
	public static <T> Optional<T> ifPresentOrElse(Optional<T> optional, Consumer<T> presentAction, Runnable elseAction) {
		if (optional.isPresent()) {
			presentAction.accept(optional.get());
		} else {
			elseAction.run();
		}

		return optional;
	}

	public static <T> Supplier<T> debugSupplier(Supplier<T> supplier, Supplier<String> messageSupplier) {
		return supplier;
	}

	public static Runnable debugRunnable(Runnable runnable, Supplier<String> messageSupplier) {
		return runnable;
	}

	public static void error(String message) {
		LOGGER.error(message);
		if (SharedConstants.isDevelopment) {
			pause(message);
		}
	}

	public static void error(String message, Throwable throwable) {
		LOGGER.error(message, throwable);
		if (SharedConstants.isDevelopment) {
			pause(message);
		}
	}

	public static <T extends Throwable> T throwOrPause(T t) {
		if (SharedConstants.isDevelopment) {
			LOGGER.error("Trying to throw a fatal exception, pausing in IDE", t);
			pause(t.getMessage());
		}

		return t;
	}

	public static void setMissingBreakpointHandler(Consumer<String> missingBreakpointHandler) {
		Util.missingBreakpointHandler = missingBreakpointHandler;
	}

	private static void pause(String message) {
		Instant instant = Instant.now();
		LOGGER.warn("Did you remember to set a breakpoint here?");
		boolean bl = Duration.between(instant, Instant.now()).toMillis() > 500L;
		if (!bl) {
			missingBreakpointHandler.accept(message);
		}
	}

	public static String getInnermostMessage(Throwable t) {
		if (t.getCause() != null) {
			return getInnermostMessage(t.getCause());
		} else {
			return t.getMessage() != null ? t.getMessage() : t.toString();
		}
	}

	/**
	 * {@return a random item from {@code array}}
	 * 
	 * @throws IllegalArgumentException if {@code array} is empty
	 */
	public static <T> T getRandom(T[] array, Random random) {
		return array[random.nextInt(array.length)];
	}

	/**
	 * {@return a random integer from {@code array}}
	 * 
	 * @throws IllegalArgumentException if {@code array} is empty
	 */
	public static int getRandom(int[] array, Random random) {
		return array[random.nextInt(array.length)];
	}

	/**
	 * {@return a random item from {@code list}}
	 * 
	 * @throws IllegalArgumentException if {@code list} is empty
	 * 
	 * @see #getRandomOrEmpty
	 */
	public static <T> T getRandom(List<T> list, Random random) {
		return (T)list.get(random.nextInt(list.size()));
	}

	/**
	 * {@return an {@link Optional} of a random item from {@code list}, or an empty optional
	 * if the list is empty}
	 * 
	 * @see #getRandom(List, Random)
	 */
	public static <T> Optional<T> getRandomOrEmpty(List<T> list, Random random) {
		return list.isEmpty() ? Optional.empty() : Optional.of(getRandom(list, random));
	}

	private static BooleanSupplier renameTask(Path src, Path dest) {
		return new BooleanSupplier() {
			public boolean getAsBoolean() {
				try {
					Files.move(src, dest);
					return true;
				} catch (IOException var2) {
					Util.LOGGER.error("Failed to rename", (Throwable)var2);
					return false;
				}
			}

			public String toString() {
				return "rename " + src + " to " + dest;
			}
		};
	}

	private static BooleanSupplier deleteTask(Path path) {
		return new BooleanSupplier() {
			public boolean getAsBoolean() {
				try {
					Files.deleteIfExists(path);
					return true;
				} catch (IOException var2) {
					Util.LOGGER.warn("Failed to delete", (Throwable)var2);
					return false;
				}
			}

			public String toString() {
				return "delete old " + path;
			}
		};
	}

	private static BooleanSupplier deletionVerifyTask(Path path) {
		return new BooleanSupplier() {
			public boolean getAsBoolean() {
				return !Files.exists(path, new LinkOption[0]);
			}

			public String toString() {
				return "verify that " + path + " is deleted";
			}
		};
	}

	private static BooleanSupplier existenceCheckTask(Path path) {
		return new BooleanSupplier() {
			public boolean getAsBoolean() {
				return Files.isRegularFile(path, new LinkOption[0]);
			}

			public String toString() {
				return "verify that " + path + " is present";
			}
		};
	}

	private static boolean attemptTasks(BooleanSupplier... tasks) {
		for (BooleanSupplier booleanSupplier : tasks) {
			if (!booleanSupplier.getAsBoolean()) {
				LOGGER.warn("Failed to execute {}", booleanSupplier);
				return false;
			}
		}

		return true;
	}

	private static boolean attemptTasks(int retries, String taskName, BooleanSupplier... tasks) {
		for (int i = 0; i < retries; i++) {
			if (attemptTasks(tasks)) {
				return true;
			}

			LOGGER.error("Failed to {}, retrying {}/{}", taskName, i, retries);
		}

		LOGGER.error("Failed to {}, aborting, progress might be lost", taskName);
		return false;
	}

	/**
	 * Copies {@code current} to {@code backup} and then replaces {@code current} with {@code newPath}.
	 */
	public static void backupAndReplace(File current, File newFile, File backup) {
		backupAndReplace(current.toPath(), newFile.toPath(), backup.toPath());
	}

	/**
	 * Copies {@code current} to {@code backup} and then replaces {@code current} with {@code newPath}.
	 */
	public static void backupAndReplace(Path current, Path newPath, Path backup) {
		backupAndReplace(current, newPath, backup, false);
	}

	/**
	 * Copies {@code current} to {@code backup} and then replaces {@code current} with {@code newPath}.
	 * 
	 * @param noRestoreOnFail if {@code true}, does not restore the current file when replacing fails
	 */
	public static void backupAndReplace(File current, File newPath, File backup, boolean noRestoreOnFail) {
		backupAndReplace(current.toPath(), newPath.toPath(), backup.toPath(), noRestoreOnFail);
	}

	/**
	 * Copies {@code current} to {@code backup} and then replaces {@code current} with {@code newPath}.
	 * 
	 * @param noRestoreOnFail if {@code true}, does not restore the current file when replacing fails
	 */
	public static void backupAndReplace(Path current, Path newPath, Path backup, boolean noRestoreOnFail) {
		int i = 10;
		if (!Files.exists(current, new LinkOption[0])
			|| attemptTasks(10, "create backup " + backup, deleteTask(backup), renameTask(current, backup), existenceCheckTask(backup))) {
			if (attemptTasks(10, "remove old " + current, deleteTask(current), deletionVerifyTask(current))) {
				if (!attemptTasks(10, "replace " + current + " with " + newPath, renameTask(newPath, current), existenceCheckTask(current)) && !noRestoreOnFail) {
					attemptTasks(10, "restore " + current + " from " + backup, renameTask(backup, current), existenceCheckTask(current));
				}
			}
		}
	}

	/**
	 * Moves the {@code cursor} in the {@code string} by a {@code delta} amount.
	 * Skips surrogate characters.
	 */
	public static int moveCursor(String string, int cursor, int delta) {
		int i = string.length();
		if (delta >= 0) {
			for (int j = 0; cursor < i && j < delta; j++) {
				if (Character.isHighSurrogate(string.charAt(cursor++)) && cursor < i && Character.isLowSurrogate(string.charAt(cursor))) {
					cursor++;
				}
			}
		} else {
			for (int jx = delta; cursor > 0 && jx < 0; jx++) {
				cursor--;
				if (Character.isLowSurrogate(string.charAt(cursor)) && cursor > 0 && Character.isHighSurrogate(string.charAt(cursor - 1))) {
					cursor--;
				}
			}
		}

		return cursor;
	}

	/**
	 * {@return a consumer that first prepends {@code prefix} to its input
	 * string and passes the result to {@code consumer}}
	 * 
	 * @apiNote This is useful in codec-based deserialization when passing the
	 * error consumer to some methods, e.g. {@code
	 * Util.addPrefix("Could not parse Example", LOGGER::error)}.
	 */
	public static Consumer<String> addPrefix(String prefix, Consumer<String> consumer) {
		return string -> consumer.accept(prefix + string);
	}

	public static DataResult<int[]> decodeFixedLengthArray(IntStream stream, int length) {
		int[] is = stream.limit((long)(length + 1)).toArray();
		if (is.length != length) {
			Supplier<String> supplier = () -> "Input is not a list of " + length + " ints";
			return is.length >= length ? DataResult.error(supplier, Arrays.copyOf(is, length)) : DataResult.error(supplier);
		} else {
			return DataResult.success(is);
		}
	}

	public static DataResult<long[]> decodeFixedLengthArray(LongStream stream, int length) {
		long[] ls = stream.limit((long)(length + 1)).toArray();
		if (ls.length != length) {
			Supplier<String> supplier = () -> "Input is not a list of " + length + " longs";
			return ls.length >= length ? DataResult.error(supplier, Arrays.copyOf(ls, length)) : DataResult.error(supplier);
		} else {
			return DataResult.success(ls);
		}
	}

	public static <T> DataResult<List<T>> decodeFixedLengthList(List<T> list, int length) {
		if (list.size() != length) {
			Supplier<String> supplier = () -> "Input is not a list of " + length + " elements";
			return list.size() >= length ? DataResult.error(supplier, list.subList(0, length)) : DataResult.error(supplier);
		} else {
			return DataResult.success(list);
		}
	}

	public static void startTimerHack() {
		Thread thread = new Thread("Timer hack thread") {
			public void run() {
				while (true) {
					try {
						Thread.sleep(2147483647L);
					} catch (InterruptedException var2) {
						Util.LOGGER.warn("Timer hack thread interrupted, that really should not happen");
						return;
					}
				}
			}
		};
		thread.setDaemon(true);
		thread.setUncaughtExceptionHandler(new UncaughtExceptionLogger(LOGGER));
		thread.start();
	}

	/**
	 * Copies a file contained in the folder {@code src} to the folder {@code dest}.
	 * This will replicate any path structure that may exist between {@code src} and {@code toCopy}.
	 */
	public static void relativeCopy(Path src, Path dest, Path toCopy) throws IOException {
		Path path = src.relativize(toCopy);
		Path path2 = dest.resolve(path);
		Files.copy(toCopy, path2);
	}

	public static String replaceInvalidChars(String string, CharPredicate predicate) {
		return (String)string.toLowerCase(Locale.ROOT)
			.chars()
			.mapToObj(charCode -> predicate.test((char)charCode) ? Character.toString((char)charCode) : "_")
			.collect(Collectors.joining());
	}

	public static <K, V> CachedMapper<K, V> cachedMapper(Function<K, V> mapper) {
		return new CachedMapper<>(mapper);
	}

	public static <T, R> Function<T, R> memoize(Function<T, R> function) {
		return new Function<T, R>() {
			private final Map<T, R> cache = new ConcurrentHashMap();

			public R apply(T object) {
				return (R)this.cache.computeIfAbsent(object, function);
			}

			public String toString() {
				return "memoize/1[function=" + function + ", size=" + this.cache.size() + "]";
			}
		};
	}

	public static <T, U, R> BiFunction<T, U, R> memoize(BiFunction<T, U, R> biFunction) {
		return new BiFunction<T, U, R>() {
			private final Map<com.mojang.datafixers.util.Pair<T, U>, R> cache = new ConcurrentHashMap();

			public R apply(T a, U b) {
				return (R)this.cache.computeIfAbsent(com.mojang.datafixers.util.Pair.of(a, b), pair -> biFunction.apply(pair.getFirst(), pair.getSecond()));
			}

			public String toString() {
				return "memoize/2[function=" + biFunction + ", size=" + this.cache.size() + "]";
			}
		};
	}

	/**
	 * {@return the contents of {@code stream} copied to a list and then shuffled}
	 */
	public static <T> List<T> copyShuffled(Stream<T> stream, Random random) {
		ObjectArrayList<T> objectArrayList = (ObjectArrayList<T>)stream.collect(ObjectArrayList.toList());
		shuffle(objectArrayList, random);
		return objectArrayList;
	}

	/**
	 * {@return the contents of {@code stream} copied to a list and then shuffled}
	 */
	public static IntArrayList shuffle(IntStream stream, Random random) {
		IntArrayList intArrayList = IntArrayList.wrap(stream.toArray());
		int i = intArrayList.size();

		for (int j = i; j > 1; j--) {
			int k = random.nextInt(j);
			intArrayList.set(j - 1, intArrayList.set(k, intArrayList.getInt(j - 1)));
		}

		return intArrayList;
	}

	/**
	 * {@return the contents of {@code array} copied to a list and then shuffled}
	 */
	public static <T> List<T> copyShuffled(T[] array, Random random) {
		ObjectArrayList<T> objectArrayList = new ObjectArrayList<>(array);
		shuffle(objectArrayList, random);
		return objectArrayList;
	}

	/**
	 * {@return the contents of {@code stream} copied to a list and then shuffled}
	 */
	public static <T> List<T> copyShuffled(ObjectArrayList<T> list, Random random) {
		ObjectArrayList<T> objectArrayList = new ObjectArrayList<>(list);
		shuffle(objectArrayList, random);
		return objectArrayList;
	}

	/**
	 * Shuffles {@code list}, modifying the passed list in place.
	 */
	public static <T> void shuffle(ObjectArrayList<T> list, Random random) {
		int i = list.size();

		for (int j = i; j > 1; j--) {
			int k = random.nextInt(j);
			list.set(j - 1, list.set(k, list.get(j - 1)));
		}
	}

	/**
	 * Runs tasks using the prepare-apply model, such as creation of a {@link
	 * net.minecraft.server.SaveLoader}.
	 * 
	 * @apiNote This method takes a function that supplies an executor to use in the
	 * apply stage. Inside the function, callers should run the preparation,
	 * and use the passed executor for applying.
	 * 
	 * @param resultFactory a function that takes the apply-stage executor and returns the future
	 */
	public static <T> CompletableFuture<T> waitAndApply(Function<Executor, CompletableFuture<T>> resultFactory) {
		return waitAndApply(resultFactory, CompletableFuture::isDone);
	}

	/**
	 * Runs tasks using the prepare-apply model.
	 * 
	 * @apiNote This method takes a function that supplies an executor to use in the
	 * apply stage. Inside the function, callers should run the preparation,
	 * and use the passed executor for applying.
	 * 
	 * @param donePredicate a predicate that, given the result, checks whether applying has finished
	 * @param resultFactory a function that takes the apply-stage executor and returns the preliminary result
	 */
	public static <T> T waitAndApply(Function<Executor, T> resultFactory, Predicate<T> donePredicate) {
		BlockingQueue<Runnable> blockingQueue = new LinkedBlockingQueue();
		T object = (T)resultFactory.apply(blockingQueue::add);

		while (!donePredicate.test(object)) {
			try {
				Runnable runnable = (Runnable)blockingQueue.poll(100L, TimeUnit.MILLISECONDS);
				if (runnable != null) {
					runnable.run();
				}
			} catch (InterruptedException var5) {
				LOGGER.warn("Interrupted wait");
				break;
			}
		}

		int i = blockingQueue.size();
		if (i > 0) {
			LOGGER.warn("Tasks left in queue: {}", i);
		}

		return object;
	}

	/**
	 * {@return a function that, when given a value in {@code values}, returns the last
	 * index of the value in the list}
	 * 
	 * @implNote Unlike {@link List#lastIndexOf}, the returned function will
	 * return {@code 0} when given values not in the passed list.
	 */
	public static <T> ToIntFunction<T> lastIndexGetter(List<T> values) {
		return lastIndexGetter(values, Object2IntOpenHashMap::new);
	}

	/**
	 * {@return a function that, when given a value in {@code values}, returns the last
	 * index of the value in the list}
	 * 
	 * @implNote Unlike {@link List#lastIndexOf}, the returned function will
	 * return {@code 0} when given values not in the passed list.
	 * 
	 * @param mapCreator a function that, when given the size of {@code values},
	 * returns a map for storing the indices of the values
	 */
	public static <T> ToIntFunction<T> lastIndexGetter(List<T> values, IntFunction<Object2IntMap<T>> mapCreator) {
		Object2IntMap<T> object2IntMap = (Object2IntMap<T>)mapCreator.apply(values.size());

		for (int i = 0; i < values.size(); i++) {
			object2IntMap.put((T)values.get(i), i);
		}

		return object2IntMap;
	}

	/**
	 * {@return the result wrapped in {@code result}}
	 */
	public static <T, E extends Exception> T getResult(DataResult<T> result, Function<String, E> exceptionGetter) throws E {
		Optional<PartialResult<T>> optional = result.error();
		if (optional.isPresent()) {
			throw (Exception)exceptionGetter.apply(((PartialResult)optional.get()).message());
		} else {
			return (T)result.result().orElseThrow();
		}
	}

	/**
	 * {@return whether {@code c} represents a space character}
	 * 
	 * @see Character#isWhitespace
	 * @see Character#isSpaceChar
	 */
	public static boolean isWhitespace(int c) {
		return Character.isWhitespace(c) || Character.isSpaceChar(c);
	}

	/**
	 * {@return whether {@code string} is {@code null}, empty, or composed entirely
	 * of {@linkplain #isWhitespace} spaces}
	 */
	public static boolean isBlank(@Nullable String string) {
		return string != null && string.length() != 0 ? string.chars().allMatch(Util::isWhitespace) : true;
	}

	static enum IdentityHashStrategy implements Strategy<Object> {
		INSTANCE;

		@Override
		public int hashCode(Object o) {
			return System.identityHashCode(o);
		}

		@Override
		public boolean equals(Object o, Object o2) {
			return o == o2;
		}
	}

	/**
	 * An enum representing the operating system of the current platform.
	 * This defines the behavior for opening links.
	 * The current one can be obtained via {@link Util#getOperatingSystem}.
	 */
	public static enum OperatingSystem {
		LINUX("linux"),
		SOLARIS("solaris"),
		WINDOWS("windows") {
			@Override
			protected String[] getURLOpenCommand(URL url) {
				return new String[]{"rundll32", "url.dll,FileProtocolHandler", url.toString()};
			}
		},
		OSX("mac") {
			@Override
			protected String[] getURLOpenCommand(URL url) {
				return new String[]{"open", url.toString()};
			}
		},
		UNKNOWN("unknown");

		private final String name;

		OperatingSystem(String name) {
			this.name = name;
		}

		/**
		 * Opens {@code url}. If this points to an HTTP(S) URL, it is usually opened using
		 * the system's default browser. Otherwise, it is opened directly.
		 * 
		 * <p><strong>Always validate the passed URL's schema</strong> as some values can
		 * execute code.
		 */
		public void open(URL url) {
			try {
				Process process = (Process)AccessController.doPrivileged(() -> Runtime.getRuntime().exec(this.getURLOpenCommand(url)));
				process.getInputStream().close();
				process.getErrorStream().close();
				process.getOutputStream().close();
			} catch (IOException | PrivilegedActionException var3) {
				Util.LOGGER.error("Couldn't open url '{}'", url, var3);
			}
		}

		/**
		 * Opens {@code uri}. If this points to an HTTP(S) URI, it is usually opened using
		 * the system's default browser. Otherwise, it is opened directly.
		 * 
		 * <p><strong>Always validate the passed URI's schema</strong> as some values can
		 * execute code.
		 */
		public void open(URI uri) {
			try {
				this.open(uri.toURL());
			} catch (MalformedURLException var3) {
				Util.LOGGER.error("Couldn't open uri '{}'", uri, var3);
			}
		}

		/**
		 * Opens {@code file}.
		 * 
		 * <p><strong>Do not pass untrusted file to this method</strong> as some values can
		 * execute code.
		 */
		public void open(File file) {
			try {
				this.open(file.toURI().toURL());
			} catch (MalformedURLException var3) {
				Util.LOGGER.error("Couldn't open file '{}'", file, var3);
			}
		}

		protected String[] getURLOpenCommand(URL url) {
			String string = url.toString();
			if ("file".equals(url.getProtocol())) {
				string = string.replace("file:", "file://");
			}

			return new String[]{"xdg-open", string};
		}

		/**
		 * Opens {@code uri}. If this points to an HTTP(S) URI, it is usually opened using
		 * the system's default browser. Otherwise, it is opened directly.
		 * 
		 * <p><strong>Always validate the passed URI's schema</strong> as some values can
		 * execute code.
		 */
		public void open(String uri) {
			try {
				this.open(new URI(uri).toURL());
			} catch (MalformedURLException | IllegalArgumentException | URISyntaxException var3) {
				Util.LOGGER.error("Couldn't open uri '{}'", uri, var3);
			}
		}

		public String getName() {
			return this.name;
		}
	}
}
