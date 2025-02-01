package net.minecraft.util.collection;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

public interface Weighted {
	Weight getWeight();

	static <T> Present<T> of(T data, int weight) {
		return new Present<>(data, Weight.of(weight));
	}

	public static class Absent implements Weighted {
		private final Weight weight;

		public Absent(int weight) {
			this.weight = Weight.of(weight);
		}

		public Absent(Weight weight) {
			this.weight = weight;
		}

		@Override
		public Weight getWeight() {
			return this.weight;
		}
	}

	public static class Present<T> implements Weighted {
		private final T data;
		private final Weight weight;

		Present(T data, Weight weight) {
			this.data = data;
			this.weight = weight;
		}

		public T getData() {
			return this.data;
		}

		@Override
		public Weight getWeight() {
			return this.weight;
		}

		public static <E> Codec<Present<E>> createCodec(Codec<E> dataCodec) {
			return RecordCodecBuilder.create(
				instance -> instance.group(
							dataCodec.fieldOf("data").forGetter(Present::getData), Weight.CODEC.fieldOf("weight").forGetter(Present::getWeight)
						)
						.apply(instance, Present::new)
			);
		}
	}
}
