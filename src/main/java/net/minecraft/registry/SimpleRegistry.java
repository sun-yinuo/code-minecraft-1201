package net.minecraft.registry;

import com.google.common.collect.Iterators;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import com.mojang.datafixers.util.Pair;
import com.mojang.logging.LogUtils;
import com.mojang.serialization.Lifecycle;
import it.unimi.dsi.fastutil.objects.Object2IntMap;
import it.unimi.dsi.fastutil.objects.Object2IntOpenCustomHashMap;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import it.unimi.dsi.fastutil.objects.ObjectList;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.Map.Entry;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import net.minecraft.Bootstrap;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.registry.entry.RegistryEntryList;
import net.minecraft.registry.entry.RegistryEntryOwner;
import net.minecraft.registry.tag.TagKey;
import net.minecraft.util.Identifier;
import net.minecraft.util.Util;
import net.minecraft.util.math.random.Random;
import org.apache.commons.lang3.Validate;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;

/**
 * An implementation of a mutable registry. All vanilla registries use this (or its
 * subclass, {@link DefaultedRegistry}).
 * 
 * @see Registry
 */
public class SimpleRegistry<T> implements MutableRegistry<T> {
	private static final Logger LOGGER = LogUtils.getLogger();
	final RegistryKey<? extends Registry<T>> key;
	private final ObjectList<RegistryEntry.Reference<T>> rawIdToEntry = new ObjectArrayList<>(256);
	private final Object2IntMap<T> entryToRawId = Util.make(new Object2IntOpenCustomHashMap<>(Util.identityHashStrategy()), map -> map.defaultReturnValue(-1));
	private final Map<Identifier, RegistryEntry.Reference<T>> idToEntry = new HashMap();
	private final Map<RegistryKey<T>, RegistryEntry.Reference<T>> keyToEntry = new HashMap();
	private final Map<T, RegistryEntry.Reference<T>> valueToEntry = new IdentityHashMap();
	private final Map<T, Lifecycle> entryToLifecycle = new IdentityHashMap();
	private Lifecycle lifecycle;
	private volatile Map<TagKey<T>, RegistryEntryList.Named<T>> tagToEntryList = new IdentityHashMap();
	private boolean frozen;
	@Nullable
	private Map<T, RegistryEntry.Reference<T>> intrusiveValueToEntry;
	@Nullable
	private List<RegistryEntry.Reference<T>> cachedEntries;
	private int nextId;
	private final RegistryWrapper.Impl<T> wrapper = new RegistryWrapper.Impl<T>() {
		@Override
		public RegistryKey<? extends Registry<? extends T>> getRegistryKey() {
			return SimpleRegistry.this.key;
		}

		@Override
		public Lifecycle getLifecycle() {
			return SimpleRegistry.this.getLifecycle();
		}

		@Override
		public Optional<RegistryEntry.Reference<T>> getOptional(RegistryKey<T> key) {
			return SimpleRegistry.this.getEntry(key);
		}

		@Override
		public Stream<RegistryEntry.Reference<T>> streamEntries() {
			return SimpleRegistry.this.streamEntries();
		}

		@Override
		public Optional<RegistryEntryList.Named<T>> getOptional(TagKey<T> tag) {
			return SimpleRegistry.this.getEntryList(tag);
		}

		@Override
		public Stream<RegistryEntryList.Named<T>> streamTags() {
			return SimpleRegistry.this.streamTagsAndEntries().map(Pair::getSecond);
		}
	};

	public SimpleRegistry(RegistryKey<? extends Registry<T>> key, Lifecycle lifecycle) {
		this(key, lifecycle, false);
	}

	public SimpleRegistry(RegistryKey<? extends Registry<T>> key, Lifecycle lifecycle, boolean intrusive) {
		Bootstrap.ensureBootstrapped(() -> "registry " + key);
		this.key = key;
		this.lifecycle = lifecycle;
		if (intrusive) {
			this.intrusiveValueToEntry = new IdentityHashMap();
		}
	}

	@Override
	public RegistryKey<? extends Registry<T>> getKey() {
		return this.key;
	}

	public String toString() {
		return "Registry[" + this.key + " (" + this.lifecycle + ")]";
	}

	private List<RegistryEntry.Reference<T>> getEntries() {
		if (this.cachedEntries == null) {
			this.cachedEntries = this.rawIdToEntry.stream().filter(Objects::nonNull).toList();
		}

		return this.cachedEntries;
	}

	private void assertNotFrozen() {
		if (this.frozen) {
			throw new IllegalStateException("Registry is already frozen");
		}
	}

	private void assertNotFrozen(RegistryKey<T> key) {
		if (this.frozen) {
			throw new IllegalStateException("Registry is already frozen (trying to add key " + key + ")");
		}
	}

	public RegistryEntry.Reference<T> set(int i, RegistryKey<T> registryKey, T object, Lifecycle lifecycle) {
		this.assertNotFrozen(registryKey);
		Validate.notNull(registryKey);
		Validate.notNull(object);
		if (this.idToEntry.containsKey(registryKey.getValue())) {
			Util.throwOrPause((T)(new IllegalStateException("Adding duplicate key '" + registryKey + "' to registry")));
		}

		if (this.valueToEntry.containsKey(object)) {
			Util.throwOrPause((T)(new IllegalStateException("Adding duplicate value '" + object + "' to registry")));
		}

		RegistryEntry.Reference<T> reference;
		if (this.intrusiveValueToEntry != null) {
			reference = (RegistryEntry.Reference<T>)this.intrusiveValueToEntry.remove(object);
			if (reference == null) {
				throw new AssertionError("Missing intrusive holder for " + registryKey + ":" + object);
			}

			reference.setRegistryKey(registryKey);
		} else {
			reference = (RegistryEntry.Reference<T>)this.keyToEntry.computeIfAbsent(registryKey, key -> RegistryEntry.Reference.standAlone(this.getEntryOwner(), key));
		}

		this.keyToEntry.put(registryKey, reference);
		this.idToEntry.put(registryKey.getValue(), reference);
		this.valueToEntry.put(object, reference);
		this.rawIdToEntry.size(Math.max(this.rawIdToEntry.size(), i + 1));
		this.rawIdToEntry.set(i, reference);
		this.entryToRawId.put(object, i);
		if (this.nextId <= i) {
			this.nextId = i + 1;
		}

		this.entryToLifecycle.put(object, lifecycle);
		this.lifecycle = this.lifecycle.add(lifecycle);
		this.cachedEntries = null;
		return reference;
	}

	@Override
	public RegistryEntry.Reference<T> add(RegistryKey<T> key, T entry, Lifecycle lifecycle) {
		return this.set(this.nextId, key, entry, lifecycle);
	}

	@Nullable
	@Override
	public Identifier getId(T value) {
		RegistryEntry.Reference<T> reference = (RegistryEntry.Reference<T>)this.valueToEntry.get(value);
		return reference != null ? reference.registryKey().getValue() : null;
	}

	@Override
	public Optional<RegistryKey<T>> getKey(T entry) {
		return Optional.ofNullable((RegistryEntry.Reference)this.valueToEntry.get(entry)).map(RegistryEntry.Reference::registryKey);
	}

	@Override
	public int getRawId(@Nullable T value) {
		return this.entryToRawId.getInt(value);
	}

	@Nullable
	@Override
	public T get(@Nullable RegistryKey<T> key) {
		return getValue((RegistryEntry.Reference<T>)this.keyToEntry.get(key));
	}

	@Nullable
	@Override
	public T get(int index) {
		return index >= 0 && index < this.rawIdToEntry.size() ? getValue((RegistryEntry.Reference<T>)this.rawIdToEntry.get(index)) : null;
	}

	@Override
	public Optional<RegistryEntry.Reference<T>> getEntry(int rawId) {
		return rawId >= 0 && rawId < this.rawIdToEntry.size() ? Optional.ofNullable((RegistryEntry.Reference)this.rawIdToEntry.get(rawId)) : Optional.empty();
	}

	@Override
	public Optional<RegistryEntry.Reference<T>> getEntry(RegistryKey<T> key) {
		return Optional.ofNullable((RegistryEntry.Reference)this.keyToEntry.get(key));
	}

	@Override
	public RegistryEntry<T> getEntry(T value) {
		RegistryEntry.Reference<T> reference = (RegistryEntry.Reference<T>)this.valueToEntry.get(value);
		return (RegistryEntry<T>)(reference != null ? reference : RegistryEntry.of(value));
	}

	RegistryEntry.Reference<T> getOrCreateEntry(RegistryKey<T> key) {
		return (RegistryEntry.Reference<T>)this.keyToEntry.computeIfAbsent(key, key2 -> {
			if (this.intrusiveValueToEntry != null) {
				throw new IllegalStateException("This registry can't create new holders without value");
			} else {
				this.assertNotFrozen(key2);
				return RegistryEntry.Reference.standAlone(this.getEntryOwner(), key2);
			}
		});
	}

	@Override
	public int size() {
		return this.keyToEntry.size();
	}

	@Override
	public Lifecycle getEntryLifecycle(T entry) {
		return (Lifecycle)this.entryToLifecycle.get(entry);
	}

	@Override
	public Lifecycle getLifecycle() {
		return this.lifecycle;
	}

	public Iterator<T> iterator() {
		return Iterators.transform(this.getEntries().iterator(), RegistryEntry::value);
	}

	@Nullable
	@Override
	public T get(@Nullable Identifier id) {
		RegistryEntry.Reference<T> reference = (RegistryEntry.Reference<T>)this.idToEntry.get(id);
		return getValue(reference);
	}

	@Nullable
	private static <T> T getValue(@Nullable RegistryEntry.Reference<T> entry) {
		return entry != null ? entry.value() : null;
	}

	@Override
	public Set<Identifier> getIds() {
		return Collections.unmodifiableSet(this.idToEntry.keySet());
	}

	@Override
	public Set<RegistryKey<T>> getKeys() {
		return Collections.unmodifiableSet(this.keyToEntry.keySet());
	}

	@Override
	public Set<Entry<RegistryKey<T>, T>> getEntrySet() {
		return Collections.unmodifiableSet(Maps.transformValues(this.keyToEntry, RegistryEntry::value).entrySet());
	}

	@Override
	public Stream<RegistryEntry.Reference<T>> streamEntries() {
		return this.getEntries().stream();
	}

	@Override
	public Stream<Pair<TagKey<T>, RegistryEntryList.Named<T>>> streamTagsAndEntries() {
		return this.tagToEntryList.entrySet().stream().map(entry -> Pair.of((TagKey)entry.getKey(), (RegistryEntryList.Named)entry.getValue()));
	}

	@Override
	public RegistryEntryList.Named<T> getOrCreateEntryList(TagKey<T> tag) {
		RegistryEntryList.Named<T> named = (RegistryEntryList.Named<T>)this.tagToEntryList.get(tag);
		if (named == null) {
			named = this.createNamedEntryList(tag);
			Map<TagKey<T>, RegistryEntryList.Named<T>> map = new IdentityHashMap(this.tagToEntryList);
			map.put(tag, named);
			this.tagToEntryList = map;
		}

		return named;
	}

	private RegistryEntryList.Named<T> createNamedEntryList(TagKey<T> tag) {
		return new RegistryEntryList.Named<>(this.getEntryOwner(), tag);
	}

	@Override
	public Stream<TagKey<T>> streamTags() {
		return this.tagToEntryList.keySet().stream();
	}

	@Override
	public boolean isEmpty() {
		return this.keyToEntry.isEmpty();
	}

	@Override
	public Optional<RegistryEntry.Reference<T>> getRandom(Random random) {
		return Util.getRandomOrEmpty(this.getEntries(), random);
	}

	@Override
	public boolean containsId(Identifier id) {
		return this.idToEntry.containsKey(id);
	}

	@Override
	public boolean contains(RegistryKey<T> key) {
		return this.keyToEntry.containsKey(key);
	}

	@Override
	public Registry<T> freeze() {
		if (this.frozen) {
			return this;
		} else {
			this.frozen = true;
			this.valueToEntry.forEach((value, entry) -> entry.setValue(value));
			List<Identifier> list = this.keyToEntry
				.entrySet()
				.stream()
				.filter(entry -> !((RegistryEntry.Reference)entry.getValue()).hasKeyAndValue())
				.map(entry -> ((RegistryKey)entry.getKey()).getValue())
				.sorted()
				.toList();
			if (!list.isEmpty()) {
				throw new IllegalStateException("Unbound values in registry " + this.getKey() + ": " + list);
			} else {
				if (this.intrusiveValueToEntry != null) {
					if (!this.intrusiveValueToEntry.isEmpty()) {
						throw new IllegalStateException("Some intrusive holders were not registered: " + this.intrusiveValueToEntry.values());
					}

					this.intrusiveValueToEntry = null;
				}

				return this;
			}
		}
	}

	@Override
	public RegistryEntry.Reference<T> createEntry(T value) {
		if (this.intrusiveValueToEntry == null) {
			throw new IllegalStateException("This registry can't create intrusive holders");
		} else {
			this.assertNotFrozen();
			return (RegistryEntry.Reference<T>)this.intrusiveValueToEntry
				.computeIfAbsent(value, valuex -> RegistryEntry.Reference.intrusive(this.getReadOnlyWrapper(), (T)valuex));
		}
	}

	@Override
	public Optional<RegistryEntryList.Named<T>> getEntryList(TagKey<T> tag) {
		return Optional.ofNullable((RegistryEntryList.Named)this.tagToEntryList.get(tag));
	}

	@Override
	public void populateTags(Map<TagKey<T>, List<RegistryEntry<T>>> tagEntries) {
		Map<RegistryEntry.Reference<T>, List<TagKey<T>>> map = new IdentityHashMap();
		this.keyToEntry.values().forEach(entry -> map.put(entry, new ArrayList()));
		tagEntries.forEach((tag, entries) -> {
			for (RegistryEntry<T> registryEntry : entries) {
				if (!registryEntry.ownerEquals(this.getReadOnlyWrapper())) {
					throw new IllegalStateException("Can't create named set " + tag + " containing value " + registryEntry + " from outside registry " + this);
				}

				if (!(registryEntry instanceof RegistryEntry.Reference<T> reference)) {
					throw new IllegalStateException("Found direct holder " + registryEntry + " value in tag " + tag);
				}

				((List)map.get(reference)).add(tag);
			}
		});
		Set<TagKey<T>> set = Sets.<TagKey<T>>difference(this.tagToEntryList.keySet(), tagEntries.keySet());
		if (!set.isEmpty()) {
			LOGGER.warn(
				"Not all defined tags for registry {} are present in data pack: {}",
				this.getKey(),
				set.stream().map(tag -> tag.id().toString()).sorted().collect(Collectors.joining(", "))
			);
		}

		Map<TagKey<T>, RegistryEntryList.Named<T>> map2 = new IdentityHashMap(this.tagToEntryList);
		tagEntries.forEach((tag, entries) -> ((RegistryEntryList.Named)map2.computeIfAbsent(tag, this::createNamedEntryList)).copyOf(entries));
		map.forEach(RegistryEntry.Reference::setTags);
		this.tagToEntryList = map2;
	}

	@Override
	public void clearTags() {
		this.tagToEntryList.values().forEach(entryList -> entryList.copyOf(List.of()));
		this.keyToEntry.values().forEach(entry -> entry.setTags(Set.of()));
	}

	@Override
	public RegistryEntryLookup<T> createMutableEntryLookup() {
		this.assertNotFrozen();
		return new RegistryEntryLookup<T>() {
			@Override
			public Optional<RegistryEntry.Reference<T>> getOptional(RegistryKey<T> key) {
				return Optional.of(this.getOrThrow(key));
			}

			@Override
			public RegistryEntry.Reference<T> getOrThrow(RegistryKey<T> key) {
				return SimpleRegistry.this.getOrCreateEntry(key);
			}

			@Override
			public Optional<RegistryEntryList.Named<T>> getOptional(TagKey<T> tag) {
				return Optional.of(this.getOrThrow(tag));
			}

			@Override
			public RegistryEntryList.Named<T> getOrThrow(TagKey<T> tag) {
				return SimpleRegistry.this.getOrCreateEntryList(tag);
			}
		};
	}

	@Override
	public RegistryEntryOwner<T> getEntryOwner() {
		return this.wrapper;
	}

	@Override
	public RegistryWrapper.Impl<T> getReadOnlyWrapper() {
		return this.wrapper;
	}
}
