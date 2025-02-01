package net.minecraft.world.gen.surfacebuilder;

import com.google.common.base.Suppliers;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableList.Builder;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.Supplier;
import net.minecraft.block.BlockState;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.util.Identifier;
import net.minecraft.util.dynamic.CodecHolder;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.VerticalSurfaceType;
import net.minecraft.util.math.noise.DoublePerlinNoiseSampler;
import net.minecraft.util.math.random.Random;
import net.minecraft.util.math.random.RandomSplitter;
import net.minecraft.world.Heightmap;
import net.minecraft.world.biome.Biome;
import net.minecraft.world.chunk.Chunk;
import net.minecraft.world.gen.HeightContext;
import net.minecraft.world.gen.YOffset;
import net.minecraft.world.gen.chunk.ChunkNoiseSampler;
import net.minecraft.world.gen.noise.NoiseConfig;
import org.jetbrains.annotations.Nullable;

public class MaterialRules {
	public static final MaterialCondition STONE_DEPTH_FLOOR = stoneDepth(0, false, VerticalSurfaceType.FLOOR);
	public static final MaterialCondition STONE_DEPTH_FLOOR_WITH_SURFACE_DEPTH = stoneDepth(0, true, VerticalSurfaceType.FLOOR);
	public static final MaterialCondition STONE_DEPTH_FLOOR_WITH_SURFACE_DEPTH_RANGE_6 = stoneDepth(0, true, 6, VerticalSurfaceType.FLOOR);
	public static final MaterialCondition STONE_DEPTH_FLOOR_WITH_SURFACE_DEPTH_RANGE_30 = stoneDepth(0, true, 30, VerticalSurfaceType.FLOOR);
	public static final MaterialCondition STONE_DEPTH_CEILING = stoneDepth(0, false, VerticalSurfaceType.CEILING);
	public static final MaterialCondition STONE_DEPTH_CEILING_WITH_SURFACE_DEPTH = stoneDepth(0, true, VerticalSurfaceType.CEILING);

	public static MaterialCondition stoneDepth(int offset, boolean addSurfaceDepth, VerticalSurfaceType verticalSurfaceType) {
		return new StoneDepthMaterialCondition(offset, addSurfaceDepth, 0, verticalSurfaceType);
	}

	public static MaterialCondition stoneDepth(int offset, boolean addSurfaceDepth, int secondaryDepthRange, VerticalSurfaceType verticalSurfaceType) {
		return new StoneDepthMaterialCondition(offset, addSurfaceDepth, secondaryDepthRange, verticalSurfaceType);
	}

	public static MaterialCondition not(MaterialCondition target) {
		return new NotMaterialCondition(target);
	}

	public static MaterialCondition aboveY(YOffset anchor, int runDepthMultiplier) {
		return new AboveYMaterialCondition(anchor, runDepthMultiplier, false);
	}

	public static MaterialCondition aboveYWithStoneDepth(YOffset anchor, int runDepthMultiplier) {
		return new AboveYMaterialCondition(anchor, runDepthMultiplier, true);
	}

	public static MaterialCondition water(int offset, int runDepthMultiplier) {
		return new WaterMaterialCondition(offset, runDepthMultiplier, false);
	}

	public static MaterialCondition waterWithStoneDepth(int offset, int runDepthMultiplier) {
		return new WaterMaterialCondition(offset, runDepthMultiplier, true);
	}

	@SafeVarargs
	public static MaterialCondition biome(RegistryKey<Biome>... biomes) {
		return biome(List.of(biomes));
	}

	private static BiomeMaterialCondition biome(List<RegistryKey<Biome>> biomes) {
		return new BiomeMaterialCondition(biomes);
	}

	public static MaterialCondition noiseThreshold(RegistryKey<DoublePerlinNoiseSampler.NoiseParameters> noise, double min) {
		return noiseThreshold(noise, min, Double.MAX_VALUE);
	}

	public static MaterialCondition noiseThreshold(RegistryKey<DoublePerlinNoiseSampler.NoiseParameters> noise, double min, double max) {
		return new NoiseThresholdMaterialCondition(noise, min, max);
	}

	public static MaterialCondition verticalGradient(String id, YOffset trueAtAndBelow, YOffset falseAtAndAbove) {
		return new VerticalGradientMaterialCondition(new Identifier(id), trueAtAndBelow, falseAtAndAbove);
	}

	public static MaterialCondition steepSlope() {
		return SteepMaterialCondition.INSTANCE;
	}

	public static MaterialCondition hole() {
		return HoleMaterialCondition.INSTANCE;
	}

	public static MaterialCondition surface() {
		return SurfaceMaterialCondition.INSTANCE;
	}

	public static MaterialCondition temperature() {
		return TemperatureMaterialCondition.INSTANCE;
	}

	public static MaterialRule condition(MaterialCondition condition, MaterialRule rule) {
		return new ConditionMaterialRule(condition, rule);
	}

	public static MaterialRule sequence(MaterialRule... rules) {
		if (rules.length == 0) {
			throw new IllegalArgumentException("Need at least 1 rule for a sequence");
		} else {
			return new SequenceMaterialRule(Arrays.asList(rules));
		}
	}

	public static MaterialRule block(BlockState state) {
		return new BlockMaterialRule(state);
	}

	public static MaterialRule terracottaBands() {
		return TerracottaBandsMaterialRule.INSTANCE;
	}

	static <A> Codec<? extends A> register(Registry<Codec<? extends A>> registry, String id, CodecHolder<? extends A> codecHolder) {
		return Registry.register(registry, id, codecHolder.codec());
	}

	static record AboveYMaterialCondition(YOffset anchor, int surfaceDepthMultiplier, boolean addStoneDepth) implements MaterialCondition {
		static final CodecHolder<AboveYMaterialCondition> CODEC = CodecHolder.of(
			RecordCodecBuilder.mapCodec(
				instance -> instance.group(
							YOffset.OFFSET_CODEC.fieldOf("anchor").forGetter(AboveYMaterialCondition::anchor),
							Codec.intRange(-20, 20).fieldOf("surface_depth_multiplier").forGetter(AboveYMaterialCondition::surfaceDepthMultiplier),
							Codec.BOOL.fieldOf("add_stone_depth").forGetter(AboveYMaterialCondition::addStoneDepth)
						)
						.apply(instance, AboveYMaterialCondition::new)
			)
		);

		@Override
		public CodecHolder<? extends MaterialCondition> codec() {
			return CODEC;
		}

		public BooleanSupplier apply(MaterialRuleContext materialRuleContext) {
			class AboveYPredicate extends FullLazyAbstractPredicate {
				AboveYPredicate() {
					super(materialRuleContext);
				}

				@Override
				protected boolean test() {
					return this.context.blockY + (AboveYMaterialCondition.this.addStoneDepth ? this.context.stoneDepthAbove : 0)
						>= AboveYMaterialCondition.this.anchor.getY(this.context.heightContext) + this.context.runDepth * AboveYMaterialCondition.this.surfaceDepthMultiplier;
				}
			}

			return new AboveYPredicate();
		}
	}

	static final class BiomeMaterialCondition implements MaterialCondition {
		static final CodecHolder<BiomeMaterialCondition> CODEC = CodecHolder.of(
			RegistryKey.createCodec(RegistryKeys.BIOME).listOf().fieldOf("biome_is").xmap(MaterialRules::biome, biomeMaterialCondition -> biomeMaterialCondition.biomes)
		);
		private final List<RegistryKey<Biome>> biomes;
		final Predicate<RegistryKey<Biome>> predicate;

		BiomeMaterialCondition(List<RegistryKey<Biome>> biomes) {
			this.biomes = biomes;
			this.predicate = Set.copyOf(biomes)::contains;
		}

		@Override
		public CodecHolder<? extends MaterialCondition> codec() {
			return CODEC;
		}

		public BooleanSupplier apply(MaterialRuleContext materialRuleContext) {
			class BiomePredicate extends FullLazyAbstractPredicate {
				BiomePredicate() {
					super(materialRuleContext);
				}

				@Override
				protected boolean test() {
					return ((RegistryEntry)this.context.biomeSupplier.get()).matches(BiomeMaterialCondition.this.predicate);
				}
			}

			return new BiomePredicate();
		}

		public boolean equals(Object object) {
			if (this == object) {
				return true;
			} else {
				return object instanceof BiomeMaterialCondition biomeMaterialCondition ? this.biomes.equals(biomeMaterialCondition.biomes) : false;
			}
		}

		public int hashCode() {
			return this.biomes.hashCode();
		}

		public String toString() {
			return "BiomeConditionSource[biomes=" + this.biomes + "]";
		}
	}

	static record BlockMaterialRule(BlockState resultState, SimpleBlockStateRule rule) implements MaterialRule {
		static final CodecHolder<BlockMaterialRule> CODEC = CodecHolder.of(
			BlockState.CODEC
				.<BlockMaterialRule>xmap(BlockMaterialRule::new, BlockMaterialRule::resultState)
				.fieldOf("result_state")
		);

		BlockMaterialRule(BlockState resultState) {
			this(resultState, new SimpleBlockStateRule(resultState));
		}

		@Override
		public CodecHolder<? extends MaterialRule> codec() {
			return CODEC;
		}

		public BlockStateRule apply(MaterialRuleContext materialRuleContext) {
			return this.rule;
		}
	}

	/**
	 * Returns a {@link BlockState} to generate at a given position, or {@code null}.
	 */
	protected interface BlockStateRule {
		@Nullable
		BlockState tryApply(int x, int y, int z);
	}

	interface BooleanSupplier {
		boolean get();
	}

	static record ConditionMaterialRule(MaterialCondition ifTrue, MaterialRule thenRun) implements MaterialRule {
		static final CodecHolder<ConditionMaterialRule> CODEC = CodecHolder.of(
			RecordCodecBuilder.mapCodec(
				instance -> instance.group(
							MaterialCondition.CODEC.fieldOf("if_true").forGetter(ConditionMaterialRule::ifTrue),
							MaterialRule.CODEC.fieldOf("then_run").forGetter(ConditionMaterialRule::thenRun)
						)
						.apply(instance, ConditionMaterialRule::new)
			)
		);

		@Override
		public CodecHolder<? extends MaterialRule> codec() {
			return CODEC;
		}

		public BlockStateRule apply(MaterialRuleContext materialRuleContext) {
			return new ConditionalBlockStateRule(
				(BooleanSupplier)this.ifTrue.apply(materialRuleContext), (BlockStateRule)this.thenRun.apply(materialRuleContext)
			);
		}
	}

	/**
	 * Applies another block state rule if the given predicate matches, and returns
	 * {@code null} otherwise.
	 */
	static record ConditionalBlockStateRule(BooleanSupplier condition, BlockStateRule followup)
		implements BlockStateRule {
		@Nullable
		@Override
		public BlockState tryApply(int i, int j, int k) {
			return !this.condition.get() ? null : this.followup.tryApply(i, j, k);
		}
	}

	abstract static class FullLazyAbstractPredicate extends LazyAbstractPredicate {
		protected FullLazyAbstractPredicate(MaterialRuleContext materialRuleContext) {
			super(materialRuleContext);
		}

		@Override
		protected long getCurrentUniqueValue() {
			return this.context.uniquePosValue;
		}
	}

	static enum HoleMaterialCondition implements MaterialCondition {
		INSTANCE;

		static final CodecHolder<HoleMaterialCondition> CODEC = CodecHolder.of(MapCodec.unit(INSTANCE));

		@Override
		public CodecHolder<? extends MaterialCondition> codec() {
			return CODEC;
		}

		public BooleanSupplier apply(MaterialRuleContext materialRuleContext) {
			return materialRuleContext.negativeRunDepthPredicate;
		}
	}

	abstract static class HorizontalLazyAbstractPredicate extends LazyAbstractPredicate {
		protected HorizontalLazyAbstractPredicate(MaterialRuleContext materialRuleContext) {
			super(materialRuleContext);
		}

		@Override
		protected long getCurrentUniqueValue() {
			return this.context.uniqueHorizontalPosValue;
		}
	}

	static record InvertedBooleanSupplier(BooleanSupplier target) implements BooleanSupplier {
		@Override
		public boolean get() {
			return !this.target.get();
		}
	}

	abstract static class LazyAbstractPredicate implements BooleanSupplier {
		protected final MaterialRuleContext context;
		private long uniqueValue;
		@Nullable
		Boolean result;

		protected LazyAbstractPredicate(MaterialRuleContext context) {
			this.context = context;
			this.uniqueValue = this.getCurrentUniqueValue() - 1L;
		}

		@Override
		public boolean get() {
			long l = this.getCurrentUniqueValue();
			if (l == this.uniqueValue) {
				if (this.result == null) {
					throw new IllegalStateException("Update triggered but the result is null");
				} else {
					return this.result;
				}
			} else {
				this.uniqueValue = l;
				this.result = this.test();
				return this.result;
			}
		}

		/**
		 * Returns a unique value for each block position. The result of this predicate
		 * will not be recalculated until this value changes.
		 * 
		 * @return the unique value for this position
		 */
		protected abstract long getCurrentUniqueValue();

		protected abstract boolean test();
	}

	public interface MaterialCondition extends Function<MaterialRuleContext, BooleanSupplier> {
		Codec<MaterialCondition> CODEC = Registries.MATERIAL_CONDITION
			.getCodec()
			.dispatch(materialCondition -> materialCondition.codec().codec(), Function.identity());

		static Codec<? extends MaterialCondition> registerAndGetDefault(Registry<Codec<? extends MaterialCondition>> registry) {
			MaterialRules.register(registry, "biome", BiomeMaterialCondition.CODEC);
			MaterialRules.register(registry, "noise_threshold", NoiseThresholdMaterialCondition.CODEC);
			MaterialRules.register(registry, "vertical_gradient", VerticalGradientMaterialCondition.CODEC);
			MaterialRules.register(registry, "y_above", AboveYMaterialCondition.CODEC);
			MaterialRules.register(registry, "water", WaterMaterialCondition.CODEC);
			MaterialRules.register(registry, "temperature", TemperatureMaterialCondition.CODEC);
			MaterialRules.register(registry, "steep", SteepMaterialCondition.CODEC);
			MaterialRules.register(registry, "not", NotMaterialCondition.CODEC);
			MaterialRules.register(registry, "hole", HoleMaterialCondition.CODEC);
			MaterialRules.register(registry, "above_preliminary_surface", SurfaceMaterialCondition.CODEC);
			return MaterialRules.register(registry, "stone_depth", StoneDepthMaterialCondition.CODEC);
		}

		CodecHolder<? extends MaterialCondition> codec();
	}

	public interface MaterialRule extends Function<MaterialRuleContext, BlockStateRule> {
		Codec<MaterialRule> CODEC = Registries.MATERIAL_RULE.getCodec().dispatch(materialRule -> materialRule.codec().codec(), Function.identity());

		static Codec<? extends MaterialRule> registerAndGetDefault(Registry<Codec<? extends MaterialRule>> registry) {
			MaterialRules.register(registry, "bandlands", TerracottaBandsMaterialRule.CODEC);
			MaterialRules.register(registry, "block", BlockMaterialRule.CODEC);
			MaterialRules.register(registry, "sequence", SequenceMaterialRule.CODEC);
			return MaterialRules.register(registry, "condition", ConditionMaterialRule.CODEC);
		}

		CodecHolder<? extends MaterialRule> codec();
	}

	protected static final class MaterialRuleContext {
		private static final int field_36274 = 8;
		private static final int field_36275 = 4;
		private static final int field_36276 = 16;
		private static final int field_36277 = 15;
		final SurfaceBuilder surfaceBuilder;
		final BooleanSupplier biomeTemperaturePredicate = new BiomeTemperaturePredicate(this);
		final BooleanSupplier steepSlopePredicate = new SteepSlopePredicate(this);
		final BooleanSupplier negativeRunDepthPredicate = new NegativeRunDepthPredicate(this);
		final BooleanSupplier surfacePredicate = new SurfacePredicate();
		final NoiseConfig noiseConfig;
		final Chunk chunk;
		private final ChunkNoiseSampler chunkNoiseSampler;
		private final Function<BlockPos, RegistryEntry<Biome>> posToBiome;
		final HeightContext heightContext;
		private long packedChunkPos = Long.MAX_VALUE;
		private final int[] estimatedSurfaceHeights = new int[4];
		long uniqueHorizontalPosValue = -9223372036854775807L;
		int blockX;
		int blockZ;
		int runDepth;
		private long field_35677 = this.uniqueHorizontalPosValue - 1L;
		private double secondaryDepth;
		private long field_35679 = this.uniqueHorizontalPosValue - 1L;
		private int surfaceMinY;
		long uniquePosValue = -9223372036854775807L;
		final BlockPos.Mutable pos = new BlockPos.Mutable();
		Supplier<RegistryEntry<Biome>> biomeSupplier;
		int blockY;
		int fluidHeight;
		int stoneDepthBelow;
		int stoneDepthAbove;

		protected MaterialRuleContext(
			SurfaceBuilder surfaceBuilder,
			NoiseConfig noiseConfig,
			Chunk chunk,
			ChunkNoiseSampler chunkNoiseSampler,
			Function<BlockPos, RegistryEntry<Biome>> posToBiome,
			Registry<Biome> registry,
			HeightContext heightContext
		) {
			this.surfaceBuilder = surfaceBuilder;
			this.noiseConfig = noiseConfig;
			this.chunk = chunk;
			this.chunkNoiseSampler = chunkNoiseSampler;
			this.posToBiome = posToBiome;
			this.heightContext = heightContext;
		}

		protected void initHorizontalContext(int blockX, int blockZ) {
			this.uniqueHorizontalPosValue++;
			this.uniquePosValue++;
			this.blockX = blockX;
			this.blockZ = blockZ;
			this.runDepth = this.surfaceBuilder.sampleRunDepth(blockX, blockZ);
		}

		protected void initVerticalContext(int stoneDepthAbove, int stoneDepthBelow, int fluidHeight, int blockX, int blockY, int blockZ) {
			this.uniquePosValue++;
			this.biomeSupplier = Suppliers.memoize(() -> (RegistryEntry<Biome>)this.posToBiome.apply(this.pos.set(blockX, blockY, blockZ)));
			this.blockY = blockY;
			this.fluidHeight = fluidHeight;
			this.stoneDepthBelow = stoneDepthBelow;
			this.stoneDepthAbove = stoneDepthAbove;
		}

		protected double getSecondaryDepth() {
			if (this.field_35677 != this.uniqueHorizontalPosValue) {
				this.field_35677 = this.uniqueHorizontalPosValue;
				this.secondaryDepth = this.surfaceBuilder.sampleSecondaryDepth(this.blockX, this.blockZ);
			}

			return this.secondaryDepth;
		}

		private static int blockToChunkCoord(int blockCoord) {
			return blockCoord >> 4;
		}

		private static int chunkToBlockCoord(int chunkCoord) {
			return chunkCoord << 4;
		}

		protected int estimateSurfaceHeight() {
			if (this.field_35679 != this.uniqueHorizontalPosValue) {
				this.field_35679 = this.uniqueHorizontalPosValue;
				int i = blockToChunkCoord(this.blockX);
				int j = blockToChunkCoord(this.blockZ);
				long l = ChunkPos.toLong(i, j);
				if (this.packedChunkPos != l) {
					this.packedChunkPos = l;
					this.estimatedSurfaceHeights[0] = this.chunkNoiseSampler.estimateSurfaceHeight(chunkToBlockCoord(i), chunkToBlockCoord(j));
					this.estimatedSurfaceHeights[1] = this.chunkNoiseSampler.estimateSurfaceHeight(chunkToBlockCoord(i + 1), chunkToBlockCoord(j));
					this.estimatedSurfaceHeights[2] = this.chunkNoiseSampler.estimateSurfaceHeight(chunkToBlockCoord(i), chunkToBlockCoord(j + 1));
					this.estimatedSurfaceHeights[3] = this.chunkNoiseSampler.estimateSurfaceHeight(chunkToBlockCoord(i + 1), chunkToBlockCoord(j + 1));
				}

				int k = MathHelper.floor(
					MathHelper.lerp2(
						(double)((float)(this.blockX & 15) / 16.0F),
						(double)((float)(this.blockZ & 15) / 16.0F),
						(double)this.estimatedSurfaceHeights[0],
						(double)this.estimatedSurfaceHeights[1],
						(double)this.estimatedSurfaceHeights[2],
						(double)this.estimatedSurfaceHeights[3]
					)
				);
				this.surfaceMinY = k + this.runDepth - 8;
			}

			return this.surfaceMinY;
		}

		static class BiomeTemperaturePredicate extends FullLazyAbstractPredicate {
			BiomeTemperaturePredicate(MaterialRuleContext materialRuleContext) {
				super(materialRuleContext);
			}

			@Override
			protected boolean test() {
				return ((Biome)((RegistryEntry)this.context.biomeSupplier.get()).value())
					.isCold(this.context.pos.set(this.context.blockX, this.context.blockY, this.context.blockZ));
			}
		}

		static final class NegativeRunDepthPredicate extends HorizontalLazyAbstractPredicate {
			NegativeRunDepthPredicate(MaterialRuleContext materialRuleContext) {
				super(materialRuleContext);
			}

			@Override
			protected boolean test() {
				return this.context.runDepth <= 0;
			}
		}

		static class SteepSlopePredicate extends HorizontalLazyAbstractPredicate {
			SteepSlopePredicate(MaterialRuleContext materialRuleContext) {
				super(materialRuleContext);
			}

			@Override
			protected boolean test() {
				int i = this.context.blockX & 15;
				int j = this.context.blockZ & 15;
				int k = Math.max(j - 1, 0);
				int l = Math.min(j + 1, 15);
				Chunk chunk = this.context.chunk;
				int m = chunk.sampleHeightmap(Heightmap.Type.WORLD_SURFACE_WG, i, k);
				int n = chunk.sampleHeightmap(Heightmap.Type.WORLD_SURFACE_WG, i, l);
				if (n >= m + 4) {
					return true;
				} else {
					int o = Math.max(i - 1, 0);
					int p = Math.min(i + 1, 15);
					int q = chunk.sampleHeightmap(Heightmap.Type.WORLD_SURFACE_WG, o, j);
					int r = chunk.sampleHeightmap(Heightmap.Type.WORLD_SURFACE_WG, p, j);
					return q >= r + 4;
				}
			}
		}

		final class SurfacePredicate implements BooleanSupplier {
			@Override
			public boolean get() {
				return MaterialRuleContext.this.blockY >= MaterialRuleContext.this.estimateSurfaceHeight();
			}
		}
	}

	static record NoiseThresholdMaterialCondition(RegistryKey<DoublePerlinNoiseSampler.NoiseParameters> noise, double minThreshold, double maxThreshold)
		implements MaterialCondition {
		static final CodecHolder<NoiseThresholdMaterialCondition> CODEC = CodecHolder.of(
			RecordCodecBuilder.mapCodec(
				instance -> instance.group(
							RegistryKey.createCodec(RegistryKeys.NOISE_PARAMETERS).fieldOf("noise").forGetter(NoiseThresholdMaterialCondition::noise),
							Codec.DOUBLE.fieldOf("min_threshold").forGetter(NoiseThresholdMaterialCondition::minThreshold),
							Codec.DOUBLE.fieldOf("max_threshold").forGetter(NoiseThresholdMaterialCondition::maxThreshold)
						)
						.apply(instance, NoiseThresholdMaterialCondition::new)
			)
		);

		@Override
		public CodecHolder<? extends MaterialCondition> codec() {
			return CODEC;
		}

		public BooleanSupplier apply(MaterialRuleContext materialRuleContext) {
			final DoublePerlinNoiseSampler doublePerlinNoiseSampler = materialRuleContext.noiseConfig.getOrCreateSampler(this.noise);

			class NoiseThresholdPredicate extends HorizontalLazyAbstractPredicate {
				NoiseThresholdPredicate() {
					super(materialRuleContext);
				}

				@Override
				protected boolean test() {
					double d = doublePerlinNoiseSampler.sample((double)this.context.blockX, 0.0, (double)this.context.blockZ);
					return d >= NoiseThresholdMaterialCondition.this.minThreshold && d <= NoiseThresholdMaterialCondition.this.maxThreshold;
				}
			}

			return new NoiseThresholdPredicate();
		}
	}

	static record NotMaterialCondition(MaterialCondition target) implements MaterialCondition {
		static final CodecHolder<NotMaterialCondition> CODEC = CodecHolder.of(
			MaterialCondition.CODEC
				.<NotMaterialCondition>xmap(NotMaterialCondition::new, NotMaterialCondition::target)
				.fieldOf("invert")
		);

		@Override
		public CodecHolder<? extends MaterialCondition> codec() {
			return CODEC;
		}

		public BooleanSupplier apply(MaterialRuleContext materialRuleContext) {
			return new InvertedBooleanSupplier((BooleanSupplier)this.target.apply(materialRuleContext));
		}
	}

	/**
	 * Applies the given block state rules in sequence, and returns the first result that
	 * isn't {@code null}. Returns {@code null} if none of the passed rules match.
	 */
	static record SequenceBlockStateRule(List<BlockStateRule> rules) implements BlockStateRule {
		@Nullable
		@Override
		public BlockState tryApply(int i, int j, int k) {
			for (BlockStateRule blockStateRule : this.rules) {
				BlockState blockState = blockStateRule.tryApply(i, j, k);
				if (blockState != null) {
					return blockState;
				}
			}

			return null;
		}
	}

	static record SequenceMaterialRule(List<MaterialRule> sequence) implements MaterialRule {
		static final CodecHolder<SequenceMaterialRule> CODEC = CodecHolder.of(
			MaterialRule.CODEC
				.listOf()
				.<SequenceMaterialRule>xmap(SequenceMaterialRule::new, SequenceMaterialRule::sequence)
				.fieldOf("sequence")
		);

		@Override
		public CodecHolder<? extends MaterialRule> codec() {
			return CODEC;
		}

		public BlockStateRule apply(MaterialRuleContext materialRuleContext) {
			if (this.sequence.size() == 1) {
				return (BlockStateRule)((MaterialRule)this.sequence.get(0)).apply(materialRuleContext);
			} else {
				Builder<BlockStateRule> builder = ImmutableList.builder();

				for (MaterialRule materialRule : this.sequence) {
					builder.add((BlockStateRule)materialRule.apply(materialRuleContext));
				}

				return new SequenceBlockStateRule(builder.build());
			}
		}
	}

	/**
	 * Always returns the given {@link BlockState}.
	 */
	static record SimpleBlockStateRule(BlockState state) implements BlockStateRule {
		@Override
		public BlockState tryApply(int i, int j, int k) {
			return this.state;
		}
	}

	static enum SteepMaterialCondition implements MaterialCondition {
		INSTANCE;

		static final CodecHolder<SteepMaterialCondition> CODEC = CodecHolder.of(MapCodec.unit(INSTANCE));

		@Override
		public CodecHolder<? extends MaterialCondition> codec() {
			return CODEC;
		}

		public BooleanSupplier apply(MaterialRuleContext materialRuleContext) {
			return materialRuleContext.steepSlopePredicate;
		}
	}

	static record StoneDepthMaterialCondition(int offset, boolean addSurfaceDepth, int secondaryDepthRange, VerticalSurfaceType surfaceType)
		implements MaterialCondition {
		static final CodecHolder<StoneDepthMaterialCondition> CODEC = CodecHolder.of(
			RecordCodecBuilder.mapCodec(
				instance -> instance.group(
							Codec.INT.fieldOf("offset").forGetter(StoneDepthMaterialCondition::offset),
							Codec.BOOL.fieldOf("add_surface_depth").forGetter(StoneDepthMaterialCondition::addSurfaceDepth),
							Codec.INT.fieldOf("secondary_depth_range").forGetter(StoneDepthMaterialCondition::secondaryDepthRange),
							VerticalSurfaceType.CODEC.fieldOf("surface_type").forGetter(StoneDepthMaterialCondition::surfaceType)
						)
						.apply(instance, StoneDepthMaterialCondition::new)
			)
		);

		@Override
		public CodecHolder<? extends MaterialCondition> codec() {
			return CODEC;
		}

		public BooleanSupplier apply(MaterialRuleContext materialRuleContext) {
			final boolean bl = this.surfaceType == VerticalSurfaceType.CEILING;

			class StoneDepthPredicate extends FullLazyAbstractPredicate {
				StoneDepthPredicate() {
					super(materialRuleContext);
				}

				@Override
				protected boolean test() {
					int i = bl ? this.context.stoneDepthBelow : this.context.stoneDepthAbove;
					int j = StoneDepthMaterialCondition.this.addSurfaceDepth ? this.context.runDepth : 0;
					int k = StoneDepthMaterialCondition.this.secondaryDepthRange == 0
						? 0
						: (int)MathHelper.map(this.context.getSecondaryDepth(), -1.0, 1.0, 0.0, (double)StoneDepthMaterialCondition.this.secondaryDepthRange);
					return i <= 1 + StoneDepthMaterialCondition.this.offset + j + k;
				}
			}

			return new StoneDepthPredicate();
		}
	}

	static enum SurfaceMaterialCondition implements MaterialCondition {
		INSTANCE;

		static final CodecHolder<SurfaceMaterialCondition> CODEC = CodecHolder.of(MapCodec.unit(INSTANCE));

		@Override
		public CodecHolder<? extends MaterialCondition> codec() {
			return CODEC;
		}

		public BooleanSupplier apply(MaterialRuleContext materialRuleContext) {
			return materialRuleContext.surfacePredicate;
		}
	}

	static enum TemperatureMaterialCondition implements MaterialCondition {
		INSTANCE;

		static final CodecHolder<TemperatureMaterialCondition> CODEC = CodecHolder.of(MapCodec.unit(INSTANCE));

		@Override
		public CodecHolder<? extends MaterialCondition> codec() {
			return CODEC;
		}

		public BooleanSupplier apply(MaterialRuleContext materialRuleContext) {
			return materialRuleContext.biomeTemperaturePredicate;
		}
	}

	static enum TerracottaBandsMaterialRule implements MaterialRule {
		INSTANCE;

		static final CodecHolder<TerracottaBandsMaterialRule> CODEC = CodecHolder.of(MapCodec.unit(INSTANCE));

		@Override
		public CodecHolder<? extends MaterialRule> codec() {
			return CODEC;
		}

		public BlockStateRule apply(MaterialRuleContext materialRuleContext) {
			return materialRuleContext.surfaceBuilder::getTerracottaBlock;
		}
	}

	static record VerticalGradientMaterialCondition(Identifier randomName, YOffset trueAtAndBelow, YOffset falseAtAndAbove)
		implements MaterialCondition {
		static final CodecHolder<VerticalGradientMaterialCondition> CODEC = CodecHolder.of(
			RecordCodecBuilder.mapCodec(
				instance -> instance.group(
							Identifier.CODEC.fieldOf("random_name").forGetter(VerticalGradientMaterialCondition::randomName),
							YOffset.OFFSET_CODEC.fieldOf("true_at_and_below").forGetter(VerticalGradientMaterialCondition::trueAtAndBelow),
							YOffset.OFFSET_CODEC.fieldOf("false_at_and_above").forGetter(VerticalGradientMaterialCondition::falseAtAndAbove)
						)
						.apply(instance, VerticalGradientMaterialCondition::new)
			)
		);

		@Override
		public CodecHolder<? extends MaterialCondition> codec() {
			return CODEC;
		}

		public BooleanSupplier apply(MaterialRuleContext materialRuleContext) {
			final int i = this.trueAtAndBelow().getY(materialRuleContext.heightContext);
			final int j = this.falseAtAndAbove().getY(materialRuleContext.heightContext);
			final RandomSplitter randomSplitter = materialRuleContext.noiseConfig.getOrCreateRandomDeriver(this.randomName());

			class VerticalGradientPredicate extends FullLazyAbstractPredicate {
				VerticalGradientPredicate() {
					super(materialRuleContext);
				}

				@Override
				protected boolean test() {
					int i = this.context.blockY;
					if (i <= i) {
						return true;
					} else if (i >= j) {
						return false;
					} else {
						double d = MathHelper.map((double)i, (double)i, (double)j, 1.0, 0.0);
						Random random = randomSplitter.split(this.context.blockX, i, this.context.blockZ);
						return (double)random.nextFloat() < d;
					}
				}
			}

			return new VerticalGradientPredicate();
		}
	}

	static record WaterMaterialCondition(int offset, int surfaceDepthMultiplier, boolean addStoneDepth) implements MaterialCondition {
		static final CodecHolder<WaterMaterialCondition> CODEC = CodecHolder.of(
			RecordCodecBuilder.mapCodec(
				instance -> instance.group(
							Codec.INT.fieldOf("offset").forGetter(WaterMaterialCondition::offset),
							Codec.intRange(-20, 20).fieldOf("surface_depth_multiplier").forGetter(WaterMaterialCondition::surfaceDepthMultiplier),
							Codec.BOOL.fieldOf("add_stone_depth").forGetter(WaterMaterialCondition::addStoneDepth)
						)
						.apply(instance, WaterMaterialCondition::new)
			)
		);

		@Override
		public CodecHolder<? extends MaterialCondition> codec() {
			return CODEC;
		}

		public BooleanSupplier apply(MaterialRuleContext materialRuleContext) {
			class WaterPredicate extends FullLazyAbstractPredicate {
				WaterPredicate() {
					super(materialRuleContext);
				}

				@Override
				protected boolean test() {
					return this.context.fluidHeight == Integer.MIN_VALUE
						|| this.context.blockY + (WaterMaterialCondition.this.addStoneDepth ? this.context.stoneDepthAbove : 0)
							>= this.context.fluidHeight + WaterMaterialCondition.this.offset + this.context.runDepth * WaterMaterialCondition.this.surfaceDepthMultiplier;
				}
			}

			return new WaterPredicate();
		}
	}
}
