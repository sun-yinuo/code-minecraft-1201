package net.minecraft.util.function;

import java.util.function.Consumer;

/**
 * A consumer used in an iteration that can be aborted early.
 */
@FunctionalInterface
public interface LazyIterationConsumer<T> {
	NextIteration accept(T value);

	static <T> LazyIterationConsumer<T> forConsumer(Consumer<T> consumer) {
		return value -> {
			consumer.accept(value);
			return NextIteration.CONTINUE;
		};
	}

	/**
	 * Indicates whether to perform the next iteration.
	 */
	public static enum NextIteration {
		CONTINUE,
		ABORT;

		public boolean shouldAbort() {
			return this == ABORT;
		}
	}
}
