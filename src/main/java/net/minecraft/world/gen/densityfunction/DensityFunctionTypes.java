package net.minecraft.world.gen.densityfunction;

import com.mojang.datafixers.util.Either;
import com.mojang.logging.LogUtils;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import it.unimi.dsi.fastutil.doubles.Double2DoubleFunction;
import java.util.Arrays;
import java.util.Optional;
import java.util.function.BiFunction;
import java.util.function.Function;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.util.StringIdentifiable;
import net.minecraft.util.annotation.Debug;
import net.minecraft.util.dynamic.CodecHolder;
import net.minecraft.util.function.ToFloatFunction;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.noise.DoublePerlinNoiseSampler;
import net.minecraft.util.math.noise.InterpolatedNoiseSampler;
import net.minecraft.util.math.noise.SimplexNoiseSampler;
import net.minecraft.util.math.random.CheckedRandom;
import net.minecraft.util.math.random.Random;
import net.minecraft.world.dimension.DimensionType;
import org.slf4j.Logger;

public final class DensityFunctionTypes {
	private static final Codec<DensityFunction> DYNAMIC_RANGE = Registries.DENSITY_FUNCTION_TYPE
		.getCodec()
		.dispatch(densityFunction -> densityFunction.getCodecHolder().codec(), Function.identity());
	protected static final double MAX_CONSTANT_VALUE = 1000000.0;
	static final Codec<Double> CONSTANT_RANGE = Codec.doubleRange(-1000000.0, 1000000.0);
	public static final Codec<DensityFunction> CODEC = Codec.either(CONSTANT_RANGE, DYNAMIC_RANGE)
		.xmap(
			either -> either.map(DensityFunctionTypes::constant, Function.identity()),
			densityFunction -> densityFunction instanceof Constant constant ? Either.left(constant.value()) : Either.right(densityFunction)
		);

	public static Codec<? extends DensityFunction> registerAndGetDefault(Registry<Codec<? extends DensityFunction>> registry) {
		register(registry, "blend_alpha", BlendAlpha.CODEC);
		register(registry, "blend_offset", BlendOffset.CODEC);
		register(registry, "beardifier", Beardifier.CODEC_HOLDER);
		register(registry, "old_blended_noise", InterpolatedNoiseSampler.CODEC);

		for (Wrapping.Type type : Wrapping.Type.values()) {
			register(registry, type.asString(), type.codec);
		}

		register(registry, "noise", Noise.CODEC_HOLDER);
		register(registry, "end_islands", EndIslands.CODEC_HOLDER);
		register(registry, "weird_scaled_sampler", WeirdScaledSampler.CODEC_HOLDER);
		register(registry, "shifted_noise", ShiftedNoise.CODEC_HOLDER);
		register(registry, "range_choice", RangeChoice.CODEC_HOLDER);
		register(registry, "shift_a", ShiftA.CODEC_HOLDER);
		register(registry, "shift_b", ShiftB.CODEC_HOLDER);
		register(registry, "shift", Shift.CODEC_HOLDER);
		register(registry, "blend_density", BlendDensity.CODEC_HOLDER);
		register(registry, "clamp", Clamp.CODEC_HOLDER);

		for (UnaryOperation.Type type2 : UnaryOperation.Type.values()) {
			register(registry, type2.asString(), type2.codecHolder);
		}

		for (BinaryOperationLike.Type type3 : BinaryOperationLike.Type.values()) {
			register(registry, type3.asString(), type3.codecHolder);
		}

		register(registry, "spline", Spline.CODEC_HOLDER);
		register(registry, "constant", Constant.CODEC_HOLDER);
		return register(registry, "y_clamped_gradient", YClampedGradient.CODEC_HOLDER);
	}

	private static Codec<? extends DensityFunction> register(
		Registry<Codec<? extends DensityFunction>> registry, String id, CodecHolder<? extends DensityFunction> codecHolder
	) {
		return Registry.register(registry, id, codecHolder.codec());
	}

	static <A, O> CodecHolder<O> holderOf(Codec<A> codec, Function<A, O> creator, Function<O, A> argumentGetter) {
		return CodecHolder.of(codec.fieldOf("argument").xmap(creator, argumentGetter));
	}

	static <O> CodecHolder<O> holderOf(Function<DensityFunction, O> creator, Function<O, DensityFunction> argumentGetter) {
		return holderOf(DensityFunction.FUNCTION_CODEC, creator, argumentGetter);
	}

	static <O> CodecHolder<O> holderOf(
		BiFunction<DensityFunction, DensityFunction, O> creator, Function<O, DensityFunction> argument1Getter, Function<O, DensityFunction> argument2Getter
	) {
		return CodecHolder.of(
			RecordCodecBuilder.mapCodec(
				instance -> instance.group(
							DensityFunction.FUNCTION_CODEC.fieldOf("argument1").forGetter(argument1Getter),
							DensityFunction.FUNCTION_CODEC.fieldOf("argument2").forGetter(argument2Getter)
						)
						.apply(instance, creator)
			)
		);
	}

	static <O> CodecHolder<O> holderOf(MapCodec<O> mapCodec) {
		return CodecHolder.of(mapCodec);
	}

	private DensityFunctionTypes() {
	}

	public static DensityFunction interpolated(DensityFunction inputFunction) {
		return new Wrapping(Wrapping.Type.INTERPOLATED, inputFunction);
	}

	public static DensityFunction flatCache(DensityFunction inputFunction) {
		return new Wrapping(Wrapping.Type.FLAT_CACHE, inputFunction);
	}

	public static DensityFunction cache2d(DensityFunction inputFunction) {
		return new Wrapping(Wrapping.Type.CACHE2D, inputFunction);
	}

	public static DensityFunction cacheOnce(DensityFunction inputFunction) {
		return new Wrapping(Wrapping.Type.CACHE_ONCE, inputFunction);
	}

	public static DensityFunction cacheAllInCell(DensityFunction inputFunction) {
		return new Wrapping(Wrapping.Type.CACHE_ALL_IN_CELL, inputFunction);
	}

	public static DensityFunction noiseInRange(
		RegistryEntry<DoublePerlinNoiseSampler.NoiseParameters> noiseParameters, @Deprecated double scaleXz, double scaleY, double min, double max
	) {
		return mapRange(new Noise(new DensityFunction.Noise(noiseParameters), scaleXz, scaleY), min, max);
	}

	public static DensityFunction noiseInRange(RegistryEntry<DoublePerlinNoiseSampler.NoiseParameters> noiseParameters, double scaleY, double min, double max) {
		return noiseInRange(noiseParameters, 1.0, scaleY, min, max);
	}

	public static DensityFunction noiseInRange(RegistryEntry<DoublePerlinNoiseSampler.NoiseParameters> noiseParameters, double min, double max) {
		return noiseInRange(noiseParameters, 1.0, 1.0, min, max);
	}

	public static DensityFunction shiftedNoise(
		DensityFunction shiftX, DensityFunction shiftZ, double xzScale, RegistryEntry<DoublePerlinNoiseSampler.NoiseParameters> noiseParameters
	) {
		return new ShiftedNoise(shiftX, zero(), shiftZ, xzScale, 0.0, new DensityFunction.Noise(noiseParameters));
	}

	public static DensityFunction noise(RegistryEntry<DoublePerlinNoiseSampler.NoiseParameters> noiseParameters) {
		return noise(noiseParameters, 1.0, 1.0);
	}

	public static DensityFunction noise(RegistryEntry<DoublePerlinNoiseSampler.NoiseParameters> noiseParameters, double scaleXz, double scaleY) {
		return new Noise(new DensityFunction.Noise(noiseParameters), scaleXz, scaleY);
	}

	public static DensityFunction noise(RegistryEntry<DoublePerlinNoiseSampler.NoiseParameters> noiseParameters, double scaleY) {
		return noise(noiseParameters, 1.0, scaleY);
	}

	public static DensityFunction rangeChoice(
		DensityFunction input, double minInclusive, double maxExclusive, DensityFunction whenInRange, DensityFunction whenOutOfRange
	) {
		return new RangeChoice(input, minInclusive, maxExclusive, whenInRange, whenOutOfRange);
	}

	public static DensityFunction shiftA(RegistryEntry<DoublePerlinNoiseSampler.NoiseParameters> noiseParameters) {
		return new ShiftA(new DensityFunction.Noise(noiseParameters));
	}

	public static DensityFunction shiftB(RegistryEntry<DoublePerlinNoiseSampler.NoiseParameters> noiseParameters) {
		return new ShiftB(new DensityFunction.Noise(noiseParameters));
	}

	public static DensityFunction shift(RegistryEntry<DoublePerlinNoiseSampler.NoiseParameters> noiseParameters) {
		return new Shift(new DensityFunction.Noise(noiseParameters));
	}

	public static DensityFunction blendDensity(DensityFunction input) {
		return new BlendDensity(input);
	}

	public static DensityFunction endIslands(long seed) {
		return new EndIslands(seed);
	}

	public static DensityFunction weirdScaledSampler(
		DensityFunction input, RegistryEntry<DoublePerlinNoiseSampler.NoiseParameters> parameters, WeirdScaledSampler.RarityValueMapper mapper
	) {
		return new WeirdScaledSampler(input, new DensityFunction.Noise(parameters), mapper);
	}

	public static DensityFunction add(DensityFunction a, DensityFunction b) {
		return BinaryOperationLike.create(BinaryOperationLike.Type.ADD, a, b);
	}

	public static DensityFunction mul(DensityFunction a, DensityFunction b) {
		return BinaryOperationLike.create(BinaryOperationLike.Type.MUL, a, b);
	}

	public static DensityFunction min(DensityFunction a, DensityFunction b) {
		return BinaryOperationLike.create(BinaryOperationLike.Type.MIN, a, b);
	}

	public static DensityFunction max(DensityFunction a, DensityFunction b) {
		return BinaryOperationLike.create(BinaryOperationLike.Type.MAX, a, b);
	}

	public static DensityFunction spline(
		net.minecraft.util.math.Spline<Spline.SplinePos, Spline.DensityFunctionWrapper> spline
	) {
		return new Spline(spline);
	}

	public static DensityFunction zero() {
		return Constant.ZERO;
	}

	public static DensityFunction constant(double density) {
		return new Constant(density);
	}

	public static DensityFunction yClampedGradient(int fromY, int toY, double fromValue, double toValue) {
		return new YClampedGradient(fromY, toY, fromValue, toValue);
	}

	public static DensityFunction unary(DensityFunction input, UnaryOperation.Type type) {
		return UnaryOperation.create(type, input);
	}

	/**
	 * Creates a new density function based on {@code function}, but with a different range.
	 * {@code function} is assumed to be in the range {@code -1.0} to {@code 1.0},
	 * while the new function will be in the range {@code min} to {@code max}.
	 * 
	 * @return the created density function
	 * 
	 * @param max the new maximum value
	 * @param min the new minimum value
	 */
	private static DensityFunction mapRange(DensityFunction function, double min, double max) {
		double d = (min + max) * 0.5;
		double e = (max - min) * 0.5;
		return add(constant(d), mul(constant(e), function));
	}

	public static DensityFunction blendAlpha() {
		return BlendAlpha.INSTANCE;
	}

	public static DensityFunction blendOffset() {
		return BlendOffset.INSTANCE;
	}

	/**
	 * Creates a new density function which interpolates between the values of
	 * {@code start} and {@code end}, based on {@code delta}.
	 * 
	 * @return the created density function
	 * 
	 * @param delta the function used for the delta value
	 * @param start the function used for the start value, for the {@code delta} value {@code 0.0}
	 * @param end the function used for the end value, for the {@code delta} value {@code 1.0}
	 */
	public static DensityFunction lerp(DensityFunction delta, DensityFunction start, DensityFunction end) {
		if (start instanceof Constant constant) {
			return lerp(delta, constant.value, end);
		} else {
			DensityFunction densityFunction = cacheOnce(delta);
			DensityFunction densityFunction2 = add(mul(densityFunction, constant(-1.0)), constant(1.0));
			return add(mul(start, densityFunction2), mul(end, densityFunction));
		}
	}

	/**
	 * Creates a new density function which interpolates between the values of
	 * {@code start} and {@code end}, based on {@code delta}.
	 * 
	 * @return the created density function
	 * 
	 * @param end the function used for the end value, for the {@code delta} value {@code 1.0}
	 * @param start the start value, for the {@code delta} value {@code 0.0}
	 */
	public static DensityFunction lerp(DensityFunction delta, double start, DensityFunction end) {
		return add(mul(delta, add(end, constant(-start))), constant(start));
	}

	protected static enum Beardifier implements Beardifying {
		INSTANCE;

		@Override
		public double sample(NoisePos pos) {
			return 0.0;
		}

		@Override
		public void fill(double[] densities, EachApplier applier) {
			Arrays.fill(densities, 0.0);
		}

		@Override
		public double minValue() {
			return 0.0;
		}

		@Override
		public double maxValue() {
			return 0.0;
		}
	}

	public interface Beardifying extends DensityFunction.Base {
		CodecHolder<DensityFunction> CODEC_HOLDER = CodecHolder.of(MapCodec.unit(Beardifier.INSTANCE));

		@Override
		default CodecHolder<? extends DensityFunction> getCodecHolder() {
			return CODEC_HOLDER;
		}
	}

	static record BinaryOperation(
		Type type, DensityFunction argument1, DensityFunction argument2, double minValue, double maxValue
	) implements BinaryOperationLike {
		@Override
		public double sample(NoisePos pos) {
			double d = this.argument1.sample(pos);

			return switch (this.type) {
				case ADD -> d + this.argument2.sample(pos);
				case MAX -> d > this.argument2.maxValue() ? d : Math.max(d, this.argument2.sample(pos));
				case MIN -> d < this.argument2.minValue() ? d : Math.min(d, this.argument2.sample(pos));
				case MUL -> d == 0.0 ? 0.0 : d * this.argument2.sample(pos);
			};
		}

		@Override
		public void fill(double[] densities, EachApplier applier) {
			this.argument1.fill(densities, applier);
			switch (this.type) {
				case ADD:
					double[] ds = new double[densities.length];
					this.argument2.fill(ds, applier);

					for (int i = 0; i < densities.length; i++) {
						densities[i] += ds[i];
					}
					break;
				case MAX:
					double e = this.argument2.maxValue();

					for (int k = 0; k < densities.length; k++) {
						double f = densities[k];
						densities[k] = f > e ? f : Math.max(f, this.argument2.sample(applier.at(k)));
					}
					break;
				case MIN:
					double e = this.argument2.minValue();

					for (int k = 0; k < densities.length; k++) {
						double f = densities[k];
						densities[k] = f < e ? f : Math.min(f, this.argument2.sample(applier.at(k)));
					}
					break;
				case MUL:
					for (int j = 0; j < densities.length; j++) {
						double d = densities[j];
						densities[j] = d == 0.0 ? 0.0 : d * this.argument2.sample(applier.at(j));
					}
			}
		}

		@Override
		public DensityFunction apply(DensityFunctionVisitor visitor) {
			return visitor.apply(BinaryOperationLike.create(this.type, this.argument1.apply(visitor), this.argument2.apply(visitor)));
		}
	}

	interface BinaryOperationLike extends DensityFunction {
		Logger LOGGER = LogUtils.getLogger();

		static BinaryOperationLike create(
			Type type, DensityFunction argument1, DensityFunction argument2
		) {
			double d = argument1.minValue();
			double e = argument2.minValue();
			double f = argument1.maxValue();
			double g = argument2.maxValue();
			if (type == Type.MIN || type == Type.MAX) {
				boolean bl = d >= g;
				boolean bl2 = e >= f;
				if (bl || bl2) {
					LOGGER.warn("Creating a " + type + " function between two non-overlapping inputs: " + argument1 + " and " + argument2);
				}
			}
			double h = switch (type) {
				case ADD -> d + e;
				case MAX -> Math.max(d, e);
				case MIN -> Math.min(d, e);
				case MUL -> d > 0.0 && e > 0.0 ? d * e : (f < 0.0 && g < 0.0 ? f * g : Math.min(d * g, f * e));
			};

			double i = switch (type) {
				case ADD -> f + g;
				case MAX -> Math.max(f, g);
				case MIN -> Math.min(f, g);
				case MUL -> d > 0.0 && e > 0.0 ? f * g : (f < 0.0 && g < 0.0 ? d * e : Math.max(d * e, f * g));
			};
			if (type == Type.MUL || type == Type.ADD) {
				if (argument1 instanceof Constant constant) {
					return new LinearOperation(
						type == Type.ADD
							? LinearOperation.SpecificType.ADD
							: LinearOperation.SpecificType.MUL,
						argument2,
						h,
						i,
						constant.value
					);
				}

				if (argument2 instanceof Constant constant) {
					return new LinearOperation(
						type == Type.ADD
							? LinearOperation.SpecificType.ADD
							: LinearOperation.SpecificType.MUL,
						argument1,
						h,
						i,
						constant.value
					);
				}
			}

			return new BinaryOperation(type, argument1, argument2, h, i);
		}

		Type type();

		DensityFunction argument1();

		DensityFunction argument2();

		@Override
		default CodecHolder<? extends DensityFunction> getCodecHolder() {
			return this.type().codecHolder;
		}

		public static enum Type implements StringIdentifiable {
			ADD("add"),
			MUL("mul"),
			MIN("min"),
			MAX("max");

			final CodecHolder<BinaryOperationLike> codecHolder = DensityFunctionTypes.holderOf(
				(densityFunction, densityFunction2) -> BinaryOperationLike.create(this, densityFunction, densityFunction2),
				BinaryOperationLike::argument1,
				BinaryOperationLike::argument2
			);
			private final String name;

			private Type(String name) {
				this.name = name;
			}

			@Override
			public String asString() {
				return this.name;
			}
		}
	}

	protected static enum BlendAlpha implements DensityFunction.Base {
		INSTANCE;

		public static final CodecHolder<DensityFunction> CODEC = CodecHolder.of(MapCodec.unit(INSTANCE));

		@Override
		public double sample(NoisePos pos) {
			return 1.0;
		}

		@Override
		public void fill(double[] densities, EachApplier applier) {
			Arrays.fill(densities, 1.0);
		}

		@Override
		public double minValue() {
			return 1.0;
		}

		@Override
		public double maxValue() {
			return 1.0;
		}

		@Override
		public CodecHolder<? extends DensityFunction> getCodecHolder() {
			return CODEC;
		}
	}

	static record BlendDensity(DensityFunction input) implements Positional {
		static final CodecHolder<BlendDensity> CODEC_HOLDER = DensityFunctionTypes.holderOf(
			BlendDensity::new, BlendDensity::input
		);

		@Override
		public double apply(NoisePos pos, double density) {
			return pos.getBlender().applyBlendDensity(pos, density);
		}

		@Override
		public DensityFunction apply(DensityFunctionVisitor visitor) {
			return visitor.apply(new BlendDensity(this.input.apply(visitor)));
		}

		@Override
		public double minValue() {
			return Double.NEGATIVE_INFINITY;
		}

		@Override
		public double maxValue() {
			return Double.POSITIVE_INFINITY;
		}

		@Override
		public CodecHolder<? extends DensityFunction> getCodecHolder() {
			return CODEC_HOLDER;
		}
	}

	protected static enum BlendOffset implements DensityFunction.Base {
		INSTANCE;

		public static final CodecHolder<DensityFunction> CODEC = CodecHolder.of(MapCodec.unit(INSTANCE));

		@Override
		public double sample(NoisePos pos) {
			return 0.0;
		}

		@Override
		public void fill(double[] densities, EachApplier applier) {
			Arrays.fill(densities, 0.0);
		}

		@Override
		public double minValue() {
			return 0.0;
		}

		@Override
		public double maxValue() {
			return 0.0;
		}

		@Override
		public CodecHolder<? extends DensityFunction> getCodecHolder() {
			return CODEC;
		}
	}

	protected static record Clamp(DensityFunction input, double minValue, double maxValue) implements Unary {
		private static final MapCodec<Clamp> CLAMP_CODEC = RecordCodecBuilder.mapCodec(
			instance -> instance.group(
						DensityFunction.CODEC.fieldOf("input").forGetter(Clamp::input),
						DensityFunctionTypes.CONSTANT_RANGE.fieldOf("min").forGetter(Clamp::minValue),
						DensityFunctionTypes.CONSTANT_RANGE.fieldOf("max").forGetter(Clamp::maxValue)
					)
					.apply(instance, Clamp::new)
		);
		public static final CodecHolder<Clamp> CODEC_HOLDER = DensityFunctionTypes.holderOf(CLAMP_CODEC);

		@Override
		public double apply(double density) {
			return MathHelper.clamp(density, this.minValue, this.maxValue);
		}

		@Override
		public DensityFunction apply(DensityFunctionVisitor visitor) {
			return new Clamp(this.input.apply(visitor), this.minValue, this.maxValue);
		}

		@Override
		public CodecHolder<? extends DensityFunction> getCodecHolder() {
			return CODEC_HOLDER;
		}
	}

	static record Constant(double value) implements DensityFunction.Base {
		static final CodecHolder<Constant> CODEC_HOLDER = DensityFunctionTypes.holderOf(
			DensityFunctionTypes.CONSTANT_RANGE, Constant::new, Constant::value
		);
		static final Constant ZERO = new Constant(0.0);

		@Override
		public double sample(NoisePos pos) {
			return this.value;
		}

		@Override
		public void fill(double[] densities, EachApplier applier) {
			Arrays.fill(densities, this.value);
		}

		@Override
		public double minValue() {
			return this.value;
		}

		@Override
		public double maxValue() {
			return this.value;
		}

		@Override
		public CodecHolder<? extends DensityFunction> getCodecHolder() {
			return CODEC_HOLDER;
		}
	}

	protected static final class EndIslands implements DensityFunction.Base {
		public static final CodecHolder<EndIslands> CODEC_HOLDER = CodecHolder.of(MapCodec.unit(new EndIslands(0L)));
		private static final float field_37677 = -0.9F;
		private final SimplexNoiseSampler sampler;

		public EndIslands(long seed) {
			Random random = new CheckedRandom(seed);
			random.skip(17292);
			this.sampler = new SimplexNoiseSampler(random);
		}

		private static float sample(SimplexNoiseSampler sampler, int x, int z) {
			int i = x / 2;
			int j = z / 2;
			int k = x % 2;
			int l = z % 2;
			float f = 100.0F - MathHelper.sqrt((float)(x * x + z * z)) * 8.0F;
			f = MathHelper.clamp(f, -100.0F, 80.0F);

			for (int m = -12; m <= 12; m++) {
				for (int n = -12; n <= 12; n++) {
					long o = (long)(i + m);
					long p = (long)(j + n);
					if (o * o + p * p > 4096L && sampler.sample((double)o, (double)p) < -0.9F) {
						float g = (MathHelper.abs((float)o) * 3439.0F + MathHelper.abs((float)p) * 147.0F) % 13.0F + 9.0F;
						float h = (float)(k - m * 2);
						float q = (float)(l - n * 2);
						float r = 100.0F - MathHelper.sqrt(h * h + q * q) * g;
						r = MathHelper.clamp(r, -100.0F, 80.0F);
						f = Math.max(f, r);
					}
				}
			}

			return f;
		}

		@Override
		public double sample(NoisePos pos) {
			return ((double)sample(this.sampler, pos.blockX() / 8, pos.blockZ() / 8) - 8.0) / 128.0;
		}

		@Override
		public double minValue() {
			return -0.84375;
		}

		@Override
		public double maxValue() {
			return 0.5625;
		}

		@Override
		public CodecHolder<? extends DensityFunction> getCodecHolder() {
			return CODEC_HOLDER;
		}
	}

	static record LinearOperation(
		SpecificType specificType, DensityFunction input, double minValue, double maxValue, double argument
	) implements Unary, BinaryOperationLike {
		@Override
		public Type type() {
			return this.specificType == SpecificType.MUL
				? Type.MUL
				: Type.ADD;
		}

		@Override
		public DensityFunction argument1() {
			return DensityFunctionTypes.constant(this.argument);
		}

		@Override
		public DensityFunction argument2() {
			return this.input;
		}

		@Override
		public double apply(double density) {
			return switch (this.specificType) {
				case MUL -> density * this.argument;
				case ADD -> density + this.argument;
			};
		}

		@Override
		public DensityFunction apply(DensityFunctionVisitor visitor) {
			DensityFunction densityFunction = this.input.apply(visitor);
			double d = densityFunction.minValue();
			double e = densityFunction.maxValue();
			double f;
			double g;
			if (this.specificType == SpecificType.ADD) {
				f = d + this.argument;
				g = e + this.argument;
			} else if (this.argument >= 0.0) {
				f = d * this.argument;
				g = e * this.argument;
			} else {
				f = e * this.argument;
				g = d * this.argument;
			}

			return new LinearOperation(this.specificType, densityFunction, f, g, this.argument);
		}

		static enum SpecificType {
			MUL,
			ADD;
		}
	}

	protected static record Noise(Noise noise, @Deprecated double xzScale, double yScale) implements DensityFunction {
		public static final MapCodec<DensityFunctionTypes.Noise> NOISE_CODEC = RecordCodecBuilder.mapCodec(
			instance -> instance.group(
						Noise.CODEC.fieldOf("noise").forGetter(DensityFunctionTypes.Noise::noise),
						Codec.DOUBLE.fieldOf("xz_scale").forGetter(DensityFunctionTypes.Noise::xzScale),
						Codec.DOUBLE.fieldOf("y_scale").forGetter(DensityFunctionTypes.Noise::yScale)
					)
					.apply(instance, DensityFunctionTypes.Noise::new)
		);
		public static final CodecHolder<DensityFunctionTypes.Noise> CODEC_HOLDER = DensityFunctionTypes.holderOf(NOISE_CODEC);

		@Override
		public double sample(NoisePos pos) {
			return this.noise.sample((double)pos.blockX() * this.xzScale, (double)pos.blockY() * this.yScale, (double)pos.blockZ() * this.xzScale);
		}

		@Override
		public void fill(double[] densities, EachApplier applier) {
			applier.fill(densities, this);
		}

		@Override
		public DensityFunction apply(DensityFunctionVisitor visitor) {
			return visitor.apply(new DensityFunctionTypes.Noise(visitor.apply(this.noise), this.xzScale, this.yScale));
		}

		@Override
		public double minValue() {
			return -this.maxValue();
		}

		@Override
		public double maxValue() {
			return this.noise.getMaxValue();
		}

		@Override
		public CodecHolder<? extends DensityFunction> getCodecHolder() {
			return CODEC_HOLDER;
		}
	}

	interface Offset extends DensityFunction {
		Noise offsetNoise();

		@Override
		default double minValue() {
			return -this.maxValue();
		}

		@Override
		default double maxValue() {
			return this.offsetNoise().getMaxValue() * 4.0;
		}

		default double sample(double x, double y, double z) {
			return this.offsetNoise().sample(x * 0.25, y * 0.25, z * 0.25) * 4.0;
		}

		@Override
		default void fill(double[] densities, EachApplier applier) {
			applier.fill(densities, this);
		}
	}

	interface Positional extends DensityFunction {
		DensityFunction input();

		@Override
		default double sample(NoisePos pos) {
			return this.apply(pos, this.input().sample(pos));
		}

		@Override
		default void fill(double[] densities, EachApplier applier) {
			this.input().fill(densities, applier);

			for (int i = 0; i < densities.length; i++) {
				densities[i] = this.apply(applier.at(i), densities[i]);
			}
		}

		double apply(NoisePos pos, double density);
	}

	static record RangeChoice(DensityFunction input, double minInclusive, double maxExclusive, DensityFunction whenInRange, DensityFunction whenOutOfRange)
		implements DensityFunction {
		public static final MapCodec<RangeChoice> RANGE_CHOICE_CODEC = RecordCodecBuilder.mapCodec(
			instance -> instance.group(
						DensityFunction.FUNCTION_CODEC.fieldOf("input").forGetter(RangeChoice::input),
						DensityFunctionTypes.CONSTANT_RANGE.fieldOf("min_inclusive").forGetter(RangeChoice::minInclusive),
						DensityFunctionTypes.CONSTANT_RANGE.fieldOf("max_exclusive").forGetter(RangeChoice::maxExclusive),
						DensityFunction.FUNCTION_CODEC.fieldOf("when_in_range").forGetter(RangeChoice::whenInRange),
						DensityFunction.FUNCTION_CODEC.fieldOf("when_out_of_range").forGetter(RangeChoice::whenOutOfRange)
					)
					.apply(instance, RangeChoice::new)
		);
		public static final CodecHolder<RangeChoice> CODEC_HOLDER = DensityFunctionTypes.holderOf(RANGE_CHOICE_CODEC);

		@Override
		public double sample(NoisePos pos) {
			double d = this.input.sample(pos);
			return d >= this.minInclusive && d < this.maxExclusive ? this.whenInRange.sample(pos) : this.whenOutOfRange.sample(pos);
		}

		@Override
		public void fill(double[] densities, EachApplier applier) {
			this.input.fill(densities, applier);

			for (int i = 0; i < densities.length; i++) {
				double d = densities[i];
				if (d >= this.minInclusive && d < this.maxExclusive) {
					densities[i] = this.whenInRange.sample(applier.at(i));
				} else {
					densities[i] = this.whenOutOfRange.sample(applier.at(i));
				}
			}
		}

		@Override
		public DensityFunction apply(DensityFunctionVisitor visitor) {
			return visitor.apply(
				new RangeChoice(
					this.input.apply(visitor), this.minInclusive, this.maxExclusive, this.whenInRange.apply(visitor), this.whenOutOfRange.apply(visitor)
				)
			);
		}

		@Override
		public double minValue() {
			return Math.min(this.whenInRange.minValue(), this.whenOutOfRange.minValue());
		}

		@Override
		public double maxValue() {
			return Math.max(this.whenInRange.maxValue(), this.whenOutOfRange.maxValue());
		}

		@Override
		public CodecHolder<? extends DensityFunction> getCodecHolder() {
			return CODEC_HOLDER;
		}
	}

	@Debug
	public static record RegistryEntryHolder(RegistryEntry<DensityFunction> function) implements DensityFunction {
		@Override
		public double sample(NoisePos pos) {
			return this.function.value().sample(pos);
		}

		@Override
		public void fill(double[] densities, EachApplier applier) {
			this.function.value().fill(densities, applier);
		}

		@Override
		public DensityFunction apply(DensityFunctionVisitor visitor) {
			return visitor.apply(new RegistryEntryHolder(new RegistryEntry.Direct<>(this.function.value().apply(visitor))));
		}

		@Override
		public double minValue() {
			return this.function.hasKeyAndValue() ? this.function.value().minValue() : Double.NEGATIVE_INFINITY;
		}

		@Override
		public double maxValue() {
			return this.function.hasKeyAndValue() ? this.function.value().maxValue() : Double.POSITIVE_INFINITY;
		}

		@Override
		public CodecHolder<? extends DensityFunction> getCodecHolder() {
			throw new UnsupportedOperationException("Calling .codec() on HolderHolder");
		}
	}

	protected static record Shift(Noise offsetNoise) implements Offset {
		static final CodecHolder<Shift> CODEC_HOLDER = DensityFunctionTypes.holderOf(
			Noise.CODEC, Shift::new, Shift::offsetNoise
		);

		@Override
		public double sample(NoisePos pos) {
			return this.sample((double)pos.blockX(), (double)pos.blockY(), (double)pos.blockZ());
		}

		@Override
		public DensityFunction apply(DensityFunctionVisitor visitor) {
			return visitor.apply(new Shift(visitor.apply(this.offsetNoise)));
		}

		@Override
		public CodecHolder<? extends DensityFunction> getCodecHolder() {
			return CODEC_HOLDER;
		}
	}

	protected static record ShiftA(Noise offsetNoise) implements Offset {
		static final CodecHolder<ShiftA> CODEC_HOLDER = DensityFunctionTypes.holderOf(
			Noise.CODEC, ShiftA::new, ShiftA::offsetNoise
		);

		@Override
		public double sample(NoisePos pos) {
			return this.sample((double)pos.blockX(), 0.0, (double)pos.blockZ());
		}

		@Override
		public DensityFunction apply(DensityFunctionVisitor visitor) {
			return visitor.apply(new ShiftA(visitor.apply(this.offsetNoise)));
		}

		@Override
		public CodecHolder<? extends DensityFunction> getCodecHolder() {
			return CODEC_HOLDER;
		}
	}

	protected static record ShiftB(Noise offsetNoise) implements Offset {
		static final CodecHolder<ShiftB> CODEC_HOLDER = DensityFunctionTypes.holderOf(
			Noise.CODEC, ShiftB::new, ShiftB::offsetNoise
		);

		@Override
		public double sample(NoisePos pos) {
			return this.sample((double)pos.blockZ(), (double)pos.blockX(), 0.0);
		}

		@Override
		public DensityFunction apply(DensityFunctionVisitor visitor) {
			return visitor.apply(new ShiftB(visitor.apply(this.offsetNoise)));
		}

		@Override
		public CodecHolder<? extends DensityFunction> getCodecHolder() {
			return CODEC_HOLDER;
		}
	}

	protected static record ShiftedNoise(
		DensityFunction shiftX, DensityFunction shiftY, DensityFunction shiftZ, double xzScale, double yScale, Noise noise
	) implements DensityFunction {
		private static final MapCodec<ShiftedNoise> SHIFTED_NOISE_CODEC = RecordCodecBuilder.mapCodec(
			instance -> instance.group(
						DensityFunction.FUNCTION_CODEC.fieldOf("shift_x").forGetter(ShiftedNoise::shiftX),
						DensityFunction.FUNCTION_CODEC.fieldOf("shift_y").forGetter(ShiftedNoise::shiftY),
						DensityFunction.FUNCTION_CODEC.fieldOf("shift_z").forGetter(ShiftedNoise::shiftZ),
						Codec.DOUBLE.fieldOf("xz_scale").forGetter(ShiftedNoise::xzScale),
						Codec.DOUBLE.fieldOf("y_scale").forGetter(ShiftedNoise::yScale),
						Noise.CODEC.fieldOf("noise").forGetter(ShiftedNoise::noise)
					)
					.apply(instance, ShiftedNoise::new)
		);
		public static final CodecHolder<ShiftedNoise> CODEC_HOLDER = DensityFunctionTypes.holderOf(SHIFTED_NOISE_CODEC);

		@Override
		public double sample(NoisePos pos) {
			double d = (double)pos.blockX() * this.xzScale + this.shiftX.sample(pos);
			double e = (double)pos.blockY() * this.yScale + this.shiftY.sample(pos);
			double f = (double)pos.blockZ() * this.xzScale + this.shiftZ.sample(pos);
			return this.noise.sample(d, e, f);
		}

		@Override
		public void fill(double[] densities, EachApplier applier) {
			applier.fill(densities, this);
		}

		@Override
		public DensityFunction apply(DensityFunctionVisitor visitor) {
			return visitor.apply(
				new ShiftedNoise(
					this.shiftX.apply(visitor), this.shiftY.apply(visitor), this.shiftZ.apply(visitor), this.xzScale, this.yScale, visitor.apply(this.noise)
				)
			);
		}

		@Override
		public double minValue() {
			return -this.maxValue();
		}

		@Override
		public double maxValue() {
			return this.noise.getMaxValue();
		}

		@Override
		public CodecHolder<? extends DensityFunction> getCodecHolder() {
			return CODEC_HOLDER;
		}
	}

	public static record Spline(net.minecraft.util.math.Spline<SplinePos, DensityFunctionWrapper> spline)
		implements DensityFunction {
		private static final Codec<net.minecraft.util.math.Spline<SplinePos, DensityFunctionWrapper>> SPLINE_CODEC = net.minecraft.util.math.Spline.createCodec(
			DensityFunctionWrapper.CODEC
		);
		private static final MapCodec<Spline> SPLINE_FUNCTION_CODEC = SPLINE_CODEC.fieldOf("spline")
			.xmap(Spline::new, Spline::spline);
		public static final CodecHolder<Spline> CODEC_HOLDER = DensityFunctionTypes.holderOf(SPLINE_FUNCTION_CODEC);

		@Override
		public double sample(NoisePos pos) {
			return (double)this.spline.apply(new SplinePos(pos));
		}

		@Override
		public double minValue() {
			return (double)this.spline.min();
		}

		@Override
		public double maxValue() {
			return (double)this.spline.max();
		}

		@Override
		public void fill(double[] densities, EachApplier applier) {
			applier.fill(densities, this);
		}

		@Override
		public DensityFunction apply(DensityFunctionVisitor visitor) {
			return visitor.apply(new Spline(this.spline.apply(densityFunctionWrapper -> densityFunctionWrapper.apply(visitor))));
		}

		@Override
		public CodecHolder<? extends DensityFunction> getCodecHolder() {
			return CODEC_HOLDER;
		}

		public static record DensityFunctionWrapper(RegistryEntry<DensityFunction> function) implements ToFloatFunction<SplinePos> {
			public static final Codec<DensityFunctionWrapper> CODEC = DensityFunction.REGISTRY_ENTRY_CODEC
				.xmap(DensityFunctionWrapper::new, DensityFunctionWrapper::function);

			public String toString() {
				Optional<RegistryKey<DensityFunction>> optional = this.function.getKey();
				if (optional.isPresent()) {
					RegistryKey<DensityFunction> registryKey = (RegistryKey<DensityFunction>)optional.get();
					if (registryKey == DensityFunctions.CONTINENTS_OVERWORLD) {
						return "continents";
					}

					if (registryKey == DensityFunctions.EROSION_OVERWORLD) {
						return "erosion";
					}

					if (registryKey == DensityFunctions.RIDGES_OVERWORLD) {
						return "weirdness";
					}

					if (registryKey == DensityFunctions.RIDGES_FOLDED_OVERWORLD) {
						return "ridges";
					}
				}

				return "Coordinate[" + this.function + "]";
			}

			public float apply(SplinePos splinePos) {
				return (float)this.function.value().sample(splinePos.context());
			}

			@Override
			public float min() {
				return this.function.hasKeyAndValue() ? (float)this.function.value().minValue() : Float.NEGATIVE_INFINITY;
			}

			@Override
			public float max() {
				return this.function.hasKeyAndValue() ? (float)this.function.value().maxValue() : Float.POSITIVE_INFINITY;
			}

			public DensityFunctionWrapper apply(DensityFunctionVisitor visitor) {
				return new DensityFunctionWrapper(new RegistryEntry.Direct<>(this.function.value().apply(visitor)));
			}
		}

		public static record SplinePos(NoisePos context) {
		}
	}

	interface Unary extends DensityFunction {
		DensityFunction input();

		@Override
		default double sample(NoisePos pos) {
			return this.apply(this.input().sample(pos));
		}

		@Override
		default void fill(double[] densities, EachApplier applier) {
			this.input().fill(densities, applier);

			for (int i = 0; i < densities.length; i++) {
				densities[i] = this.apply(densities[i]);
			}
		}

		double apply(double density);
	}

	protected static record UnaryOperation(Type type, DensityFunction input, double minValue, double maxValue)
		implements Unary {
		public static UnaryOperation create(Type type, DensityFunction input) {
			double d = input.minValue();
			double e = apply(type, d);
			double f = apply(type, input.maxValue());
			return type != Type.ABS && type != Type.SQUARE
				? new UnaryOperation(type, input, e, f)
				: new UnaryOperation(type, input, Math.max(0.0, d), Math.max(e, f));
		}

		private static double apply(Type type, double density) {
			return switch (type) {
				case ABS -> Math.abs(density);
				case SQUARE -> density * density;
				case CUBE -> density * density * density;
				case HALF_NEGATIVE -> density > 0.0 ? density : density * 0.5;
				case QUARTER_NEGATIVE -> density > 0.0 ? density : density * 0.25;
				case SQUEEZE -> {
					double d = MathHelper.clamp(density, -1.0, 1.0);
					yield d / 2.0 - d * d * d / 24.0;
				}
			};
		}

		@Override
		public double apply(double density) {
			return apply(this.type, density);
		}

		public UnaryOperation apply(DensityFunctionVisitor densityFunctionVisitor) {
			return create(this.type, this.input.apply(densityFunctionVisitor));
		}

		@Override
		public CodecHolder<? extends DensityFunction> getCodecHolder() {
			return this.type.codecHolder;
		}

		static enum Type implements StringIdentifiable {
			ABS("abs"),
			SQUARE("square"),
			CUBE("cube"),
			HALF_NEGATIVE("half_negative"),
			QUARTER_NEGATIVE("quarter_negative"),
			SQUEEZE("squeeze");

			private final String name;
			final CodecHolder<UnaryOperation> codecHolder = DensityFunctionTypes.holderOf(
				input -> UnaryOperation.create(this, input), UnaryOperation::input
			);

			private Type(String name) {
				this.name = name;
			}

			@Override
			public String asString() {
				return this.name;
			}
		}
	}

	protected static record WeirdScaledSampler(
		DensityFunction input, Noise noise, RarityValueMapper rarityValueMapper
	) implements Positional {
		private static final MapCodec<WeirdScaledSampler> WEIRD_SCALED_SAMPLER_CODEC = RecordCodecBuilder.mapCodec(
			instance -> instance.group(
						DensityFunction.FUNCTION_CODEC.fieldOf("input").forGetter(WeirdScaledSampler::input),
						Noise.CODEC.fieldOf("noise").forGetter(WeirdScaledSampler::noise),
						RarityValueMapper.CODEC
							.fieldOf("rarity_value_mapper")
							.forGetter(WeirdScaledSampler::rarityValueMapper)
					)
					.apply(instance, WeirdScaledSampler::new)
		);
		public static final CodecHolder<WeirdScaledSampler> CODEC_HOLDER = DensityFunctionTypes.holderOf(WEIRD_SCALED_SAMPLER_CODEC);

		@Override
		public double apply(NoisePos pos, double density) {
			double d = this.rarityValueMapper.scaleFunction.get(density);
			return d * Math.abs(this.noise.sample((double)pos.blockX() / d, (double)pos.blockY() / d, (double)pos.blockZ() / d));
		}

		@Override
		public DensityFunction apply(DensityFunctionVisitor visitor) {
			return visitor.apply(new WeirdScaledSampler(this.input.apply(visitor), visitor.apply(this.noise), this.rarityValueMapper));
		}

		@Override
		public double minValue() {
			return 0.0;
		}

		@Override
		public double maxValue() {
			return this.rarityValueMapper.maxValueMultiplier * this.noise.getMaxValue();
		}

		@Override
		public CodecHolder<? extends DensityFunction> getCodecHolder() {
			return CODEC_HOLDER;
		}

		public static enum RarityValueMapper implements StringIdentifiable {
			TYPE1("type_1", DensityFunctions.CaveScaler::scaleTunnels, 2.0),
			TYPE2("type_2", DensityFunctions.CaveScaler::scaleCaves, 3.0);

			public static final com.mojang.serialization.Codec<RarityValueMapper> CODEC = StringIdentifiable.createCodec(
				RarityValueMapper::values
			);
			private final String name;
			final Double2DoubleFunction scaleFunction;
			final double maxValueMultiplier;

			private RarityValueMapper(String name, Double2DoubleFunction scaleFunction, double maxValueMultiplier) {
				this.name = name;
				this.scaleFunction = scaleFunction;
				this.maxValueMultiplier = maxValueMultiplier;
			}

			@Override
			public String asString() {
				return this.name;
			}
		}
	}

	public interface Wrapper extends DensityFunction {
		Wrapping.Type type();

		DensityFunction wrapped();

		@Override
		default CodecHolder<? extends DensityFunction> getCodecHolder() {
			return this.type().codec;
		}

		@Override
		default DensityFunction apply(DensityFunctionVisitor visitor) {
			return visitor.apply(new Wrapping(this.type(), this.wrapped().apply(visitor)));
		}
	}

	protected static record Wrapping(Type type, DensityFunction wrapped) implements Wrapper {
		@Override
		public double sample(NoisePos pos) {
			return this.wrapped.sample(pos);
		}

		@Override
		public void fill(double[] densities, EachApplier applier) {
			this.wrapped.fill(densities, applier);
		}

		@Override
		public double minValue() {
			return this.wrapped.minValue();
		}

		@Override
		public double maxValue() {
			return this.wrapped.maxValue();
		}

		static enum Type implements StringIdentifiable {
			INTERPOLATED("interpolated"),
			FLAT_CACHE("flat_cache"),
			CACHE2D("cache_2d"),
			CACHE_ONCE("cache_once"),
			CACHE_ALL_IN_CELL("cache_all_in_cell");

			private final String name;
			final CodecHolder<Wrapper> codec = DensityFunctionTypes.holderOf(
				densityFunction -> new Wrapping(this, densityFunction), Wrapper::wrapped
			);

			private Type(String name) {
				this.name = name;
			}

			@Override
			public String asString() {
				return this.name;
			}
		}
	}

	static record YClampedGradient(int fromY, int toY, double fromValue, double toValue) implements DensityFunction.Base {
		private static final MapCodec<YClampedGradient> Y_CLAMPED_GRADIENT_CODEC = RecordCodecBuilder.mapCodec(
			instance -> instance.group(
						Codec.intRange(DimensionType.MIN_HEIGHT * 2, DimensionType.MAX_COLUMN_HEIGHT * 2)
							.fieldOf("from_y")
							.forGetter(YClampedGradient::fromY),
						Codec.intRange(DimensionType.MIN_HEIGHT * 2, DimensionType.MAX_COLUMN_HEIGHT * 2).fieldOf("to_y").forGetter(YClampedGradient::toY),
						DensityFunctionTypes.CONSTANT_RANGE.fieldOf("from_value").forGetter(YClampedGradient::fromValue),
						DensityFunctionTypes.CONSTANT_RANGE.fieldOf("to_value").forGetter(YClampedGradient::toValue)
					)
					.apply(instance, YClampedGradient::new)
		);
		public static final CodecHolder<YClampedGradient> CODEC_HOLDER = DensityFunctionTypes.holderOf(Y_CLAMPED_GRADIENT_CODEC);

		@Override
		public double sample(NoisePos pos) {
			return MathHelper.clampedMap((double)pos.blockY(), (double)this.fromY, (double)this.toY, this.fromValue, this.toValue);
		}

		@Override
		public double minValue() {
			return Math.min(this.fromValue, this.toValue);
		}

		@Override
		public double maxValue() {
			return Math.max(this.fromValue, this.toValue);
		}

		@Override
		public CodecHolder<? extends DensityFunction> getCodecHolder() {
			return CODEC_HOLDER;
		}
	}
}
