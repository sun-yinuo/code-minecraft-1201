package net.minecraft.util.collection;

import org.jetbrains.annotations.Nullable;

public interface IndexedIterable<T> extends Iterable<T> {
	int ABSENT_RAW_ID = -1;

	int getRawId(T value);

	@Nullable
	T get(int index);

	/**
	 * {@return the value at {@code index}}
	 * 
	 * @throws IllegalArgumentException if the value is {@code null}
	 */
	default T getOrThrow(int index) {
		T object = this.get(index);
		if (object == null) {
			throw new IllegalArgumentException("No value with id " + index);
		} else {
			return object;
		}
	}

	int size();
}
