package net.minecraft.world.gen.chunk;

import java.util.Arrays;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.util.math.ChunkSectionPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.random.Random;
import net.minecraft.util.math.random.RandomSplitter;
import net.minecraft.world.biome.source.util.VanillaBiomeParameters;
import net.minecraft.world.dimension.DimensionType;
import net.minecraft.world.gen.densityfunction.DensityFunction;
import net.minecraft.world.gen.noise.NoiseRouter;
import org.apache.commons.lang3.mutable.MutableDouble;
import org.jetbrains.annotations.Nullable;

public interface AquiferSampler {
	static AquiferSampler aquifer(
		ChunkNoiseSampler chunkNoiseSampler,
		ChunkPos chunkPos,
		NoiseRouter noiseRouter,
		RandomSplitter randomSplitter,
		int minimumY,
		int height,
		FluidLevelSampler fluidLevelSampler
	) {
		return new Impl(chunkNoiseSampler, chunkPos, noiseRouter, randomSplitter, minimumY, height, fluidLevelSampler);
	}

	static AquiferSampler seaLevel(FluidLevelSampler fluidLevelSampler) {
		return new AquiferSampler() {
			@Nullable
			@Override
			public BlockState apply(DensityFunction.NoisePos pos, double density) {
				return density > 0.0 ? null : fluidLevelSampler.getFluidLevel(pos.blockX(), pos.blockY(), pos.blockZ()).getBlockState(pos.blockY());
			}

			@Override
			public boolean needsFluidTick() {
				return false;
			}
		};
	}

	@Nullable
	BlockState apply(DensityFunction.NoisePos pos, double density);

	boolean needsFluidTick();

	public static final class FluidLevel {
		final int y;
		final BlockState state;

		public FluidLevel(int y, BlockState state) {
			this.y = y;
			this.state = state;
		}

		public BlockState getBlockState(int y) {
			return y < this.y ? this.state : Blocks.AIR.getDefaultState();
		}
	}

	public interface FluidLevelSampler {
		FluidLevel getFluidLevel(int x, int y, int z);
	}

	public static class Impl implements AquiferSampler {
		private static final int field_31451 = 10;
		private static final int field_31452 = 9;
		private static final int field_31453 = 10;
		private static final int field_31454 = 6;
		private static final int field_31455 = 3;
		private static final int field_31456 = 6;
		private static final int field_31457 = 16;
		private static final int field_31458 = 12;
		private static final int field_31459 = 16;
		private static final int field_36220 = 11;
		private static final double NEEDS_FLUID_TICK_DISTANCE_THRESHOLD = maxDistance(MathHelper.square(10), MathHelper.square(12));
		private final ChunkNoiseSampler chunkNoiseSampler;
		private final DensityFunction barrierNoise;
		private final DensityFunction fluidLevelFloodednessNoise;
		private final DensityFunction fluidLevelSpreadNoise;
		private final DensityFunction fluidTypeNoise;
		private final RandomSplitter randomDeriver;
		private final FluidLevel[] waterLevels;
		private final long[] blockPositions;
		private final FluidLevelSampler fluidLevelSampler;
		private final DensityFunction erosionDensityFunction;
		private final DensityFunction depthDensityFunction;
		private boolean needsFluidTick;
		private final int startX;
		private final int startY;
		private final int startZ;
		private final int sizeX;
		private final int sizeZ;
		private static final int[][] CHUNK_POS_OFFSETS = new int[][]{
			{0, 0}, {-2, -1}, {-1, -1}, {0, -1}, {1, -1}, {-3, 0}, {-2, 0}, {-1, 0}, {1, 0}, {-2, 1}, {-1, 1}, {0, 1}, {1, 1}
		};

		Impl(
			ChunkNoiseSampler chunkNoiseSampler,
			ChunkPos chunkPos,
			NoiseRouter noiseRouter,
			RandomSplitter randomSplitter,
			int minimumY,
			int height,
			FluidLevelSampler fluidLevelSampler
		) {
			this.chunkNoiseSampler = chunkNoiseSampler;
			this.barrierNoise = noiseRouter.barrierNoise();
			this.fluidLevelFloodednessNoise = noiseRouter.fluidLevelFloodednessNoise();
			this.fluidLevelSpreadNoise = noiseRouter.fluidLevelSpreadNoise();
			this.fluidTypeNoise = noiseRouter.lavaNoise();
			this.erosionDensityFunction = noiseRouter.erosion();
			this.depthDensityFunction = noiseRouter.depth();
			this.randomDeriver = randomSplitter;
			this.startX = this.getLocalX(chunkPos.getStartX()) - 1;
			this.fluidLevelSampler = fluidLevelSampler;
			int i = this.getLocalX(chunkPos.getEndX()) + 1;
			this.sizeX = i - this.startX + 1;
			this.startY = this.getLocalY(minimumY) - 1;
			int j = this.getLocalY(minimumY + height) + 1;
			int k = j - this.startY + 1;
			this.startZ = this.getLocalZ(chunkPos.getStartZ()) - 1;
			int l = this.getLocalZ(chunkPos.getEndZ()) + 1;
			this.sizeZ = l - this.startZ + 1;
			int m = this.sizeX * k * this.sizeZ;
			this.waterLevels = new FluidLevel[m];
			this.blockPositions = new long[m];
			Arrays.fill(this.blockPositions, Long.MAX_VALUE);
		}

		private int index(int x, int y, int z) {
			int i = x - this.startX;
			int j = y - this.startY;
			int k = z - this.startZ;
			return (j * this.sizeZ + k) * this.sizeX + i;
		}

		@Nullable
		@Override
		public BlockState apply(DensityFunction.NoisePos pos, double density) {
			int i = pos.blockX();
			int j = pos.blockY();
			int k = pos.blockZ();
			if (density > 0.0) {
				this.needsFluidTick = false;
				return null;
			} else {
				FluidLevel fluidLevel = this.fluidLevelSampler.getFluidLevel(i, j, k);
				if (fluidLevel.getBlockState(j).isOf(Blocks.LAVA)) {
					this.needsFluidTick = false;
					return Blocks.LAVA.getDefaultState();
				} else {
					int l = Math.floorDiv(i - 5, 16);
					int m = Math.floorDiv(j + 1, 12);
					int n = Math.floorDiv(k - 5, 16);
					int o = Integer.MAX_VALUE;
					int p = Integer.MAX_VALUE;
					int q = Integer.MAX_VALUE;
					long r = 0L;
					long s = 0L;
					long t = 0L;

					for (int u = 0; u <= 1; u++) {
						for (int v = -1; v <= 1; v++) {
							for (int w = 0; w <= 1; w++) {
								int x = l + u;
								int y = m + v;
								int z = n + w;
								int aa = this.index(x, y, z);
								long ab = this.blockPositions[aa];
								long ac;
								if (ab != Long.MAX_VALUE) {
									ac = ab;
								} else {
									Random random = this.randomDeriver.split(x, y, z);
									ac = BlockPos.asLong(x * 16 + random.nextInt(10), y * 12 + random.nextInt(9), z * 16 + random.nextInt(10));
									this.blockPositions[aa] = ac;
								}

								int ad = BlockPos.unpackLongX(ac) - i;
								int ae = BlockPos.unpackLongY(ac) - j;
								int af = BlockPos.unpackLongZ(ac) - k;
								int ag = ad * ad + ae * ae + af * af;
								if (o >= ag) {
									t = s;
									s = r;
									r = ac;
									q = p;
									p = o;
									o = ag;
								} else if (p >= ag) {
									t = s;
									s = ac;
									q = p;
									p = ag;
								} else if (q >= ag) {
									t = ac;
									q = ag;
								}
							}
						}
					}

					FluidLevel fluidLevel2 = this.getWaterLevel(r);
					double d = maxDistance(o, p);
					BlockState blockState = fluidLevel2.getBlockState(j);
					if (d <= 0.0) {
						this.needsFluidTick = d >= NEEDS_FLUID_TICK_DISTANCE_THRESHOLD;
						return blockState;
					} else if (blockState.isOf(Blocks.WATER) && this.fluidLevelSampler.getFluidLevel(i, j - 1, k).getBlockState(j - 1).isOf(Blocks.LAVA)) {
						this.needsFluidTick = true;
						return blockState;
					} else {
						MutableDouble mutableDouble = new MutableDouble(Double.NaN);
						FluidLevel fluidLevel3 = this.getWaterLevel(s);
						double e = d * this.calculateDensity(pos, mutableDouble, fluidLevel2, fluidLevel3);
						if (density + e > 0.0) {
							this.needsFluidTick = false;
							return null;
						} else {
							FluidLevel fluidLevel4 = this.getWaterLevel(t);
							double f = maxDistance(o, q);
							if (f > 0.0) {
								double g = d * f * this.calculateDensity(pos, mutableDouble, fluidLevel2, fluidLevel4);
								if (density + g > 0.0) {
									this.needsFluidTick = false;
									return null;
								}
							}

							double g = maxDistance(p, q);
							if (g > 0.0) {
								double h = d * g * this.calculateDensity(pos, mutableDouble, fluidLevel3, fluidLevel4);
								if (density + h > 0.0) {
									this.needsFluidTick = false;
									return null;
								}
							}

							this.needsFluidTick = true;
							return blockState;
						}
					}
				}
			}
		}

		@Override
		public boolean needsFluidTick() {
			return this.needsFluidTick;
		}

		private static double maxDistance(int i, int a) {
			double d = 25.0;
			return 1.0 - (double)Math.abs(a - i) / 25.0;
		}

		private double calculateDensity(
			DensityFunction.NoisePos pos, MutableDouble mutableDouble, FluidLevel fluidLevel, FluidLevel fluidLevel2
		) {
			int i = pos.blockY();
			BlockState blockState = fluidLevel.getBlockState(i);
			BlockState blockState2 = fluidLevel2.getBlockState(i);
			if ((!blockState.isOf(Blocks.LAVA) || !blockState2.isOf(Blocks.WATER)) && (!blockState.isOf(Blocks.WATER) || !blockState2.isOf(Blocks.LAVA))) {
				int j = Math.abs(fluidLevel.y - fluidLevel2.y);
				if (j == 0) {
					return 0.0;
				} else {
					double d = 0.5 * (double)(fluidLevel.y + fluidLevel2.y);
					double e = (double)i + 0.5 - d;
					double f = (double)j / 2.0;
					double g = 0.0;
					double h = 2.5;
					double k = 1.5;
					double l = 3.0;
					double m = 10.0;
					double n = 3.0;
					double o = f - Math.abs(e);
					double q;
					if (e > 0.0) {
						double p = 0.0 + o;
						if (p > 0.0) {
							q = p / 1.5;
						} else {
							q = p / 2.5;
						}
					} else {
						double p = 3.0 + o;
						if (p > 0.0) {
							q = p / 3.0;
						} else {
							q = p / 10.0;
						}
					}

					double px = 2.0;
					double r;
					if (!(q < -2.0) && !(q > 2.0)) {
						double s = mutableDouble.getValue();
						if (Double.isNaN(s)) {
							double t = this.barrierNoise.sample(pos);
							mutableDouble.setValue(t);
							r = t;
						} else {
							r = s;
						}
					} else {
						r = 0.0;
					}

					return 2.0 * (r + q);
				}
			} else {
				return 2.0;
			}
		}

		private int getLocalX(int x) {
			return Math.floorDiv(x, 16);
		}

		private int getLocalY(int y) {
			return Math.floorDiv(y, 12);
		}

		private int getLocalZ(int z) {
			return Math.floorDiv(z, 16);
		}

		private FluidLevel getWaterLevel(long pos) {
			int i = BlockPos.unpackLongX(pos);
			int j = BlockPos.unpackLongY(pos);
			int k = BlockPos.unpackLongZ(pos);
			int l = this.getLocalX(i);
			int m = this.getLocalY(j);
			int n = this.getLocalZ(k);
			int o = this.index(l, m, n);
			FluidLevel fluidLevel = this.waterLevels[o];
			if (fluidLevel != null) {
				return fluidLevel;
			} else {
				FluidLevel fluidLevel2 = this.getFluidLevel(i, j, k);
				this.waterLevels[o] = fluidLevel2;
				return fluidLevel2;
			}
		}

		private FluidLevel getFluidLevel(int blockX, int blockY, int blockZ) {
			FluidLevel fluidLevel = this.fluidLevelSampler.getFluidLevel(blockX, blockY, blockZ);
			int i = Integer.MAX_VALUE;
			int j = blockY + 12;
			int k = blockY - 12;
			boolean bl = false;

			for (int[] is : CHUNK_POS_OFFSETS) {
				int l = blockX + ChunkSectionPos.getBlockCoord(is[0]);
				int m = blockZ + ChunkSectionPos.getBlockCoord(is[1]);
				int n = this.chunkNoiseSampler.estimateSurfaceHeight(l, m);
				int o = n + 8;
				boolean bl2 = is[0] == 0 && is[1] == 0;
				if (bl2 && k > o) {
					return fluidLevel;
				}

				boolean bl3 = j > o;
				if (bl3 || bl2) {
					FluidLevel fluidLevel2 = this.fluidLevelSampler.getFluidLevel(l, o, m);
					if (!fluidLevel2.getBlockState(o).isAir()) {
						if (bl2) {
							bl = true;
						}

						if (bl3) {
							return fluidLevel2;
						}
					}
				}

				i = Math.min(i, n);
			}

			int p = this.getFluidBlockY(blockX, blockY, blockZ, fluidLevel, i, bl);
			return new FluidLevel(p, this.getFluidBlockState(blockX, blockY, blockZ, fluidLevel, p));
		}

		private int getFluidBlockY(int blockX, int blockY, int blockZ, FluidLevel defaultFluidLevel, int surfaceHeightEstimate, boolean bl) {
			DensityFunction.UnblendedNoisePos unblendedNoisePos = new DensityFunction.UnblendedNoisePos(blockX, blockY, blockZ);
			double d;
			double e;
			if (VanillaBiomeParameters.method_43718(this.erosionDensityFunction, this.depthDensityFunction, unblendedNoisePos)) {
				d = -1.0;
				e = -1.0;
			} else {
				int i = surfaceHeightEstimate + 8 - blockY;
				int j = 64;
				double f = bl ? MathHelper.clampedMap((double)i, 0.0, 64.0, 1.0, 0.0) : 0.0;
				double g = MathHelper.clamp(this.fluidLevelFloodednessNoise.sample(unblendedNoisePos), -1.0, 1.0);
				double h = MathHelper.map(f, 1.0, 0.0, -0.3, 0.8);
				double k = MathHelper.map(f, 1.0, 0.0, -0.8, 0.4);
				d = g - k;
				e = g - h;
			}

			int i;
			if (e > 0.0) {
				i = defaultFluidLevel.y;
			} else if (d > 0.0) {
				i = this.getNoiseBasedFluidLevel(blockX, blockY, blockZ, surfaceHeightEstimate);
			} else {
				i = DimensionType.field_35479;
			}

			return i;
		}

		private int getNoiseBasedFluidLevel(int blockX, int blockY, int blockZ, int surfaceHeightEstimate) {
			int i = 16;
			int j = 40;
			int k = Math.floorDiv(blockX, 16);
			int l = Math.floorDiv(blockY, 40);
			int m = Math.floorDiv(blockZ, 16);
			int n = l * 40 + 20;
			int o = 10;
			double d = this.fluidLevelSpreadNoise.sample(new DensityFunction.UnblendedNoisePos(k, l, m)) * 10.0;
			int p = MathHelper.roundDownToMultiple(d, 3);
			int q = n + p;
			return Math.min(surfaceHeightEstimate, q);
		}

		private BlockState getFluidBlockState(int blockX, int blockY, int blockZ, FluidLevel defaultFluidLevel, int fluidLevel) {
			BlockState blockState = defaultFluidLevel.state;
			if (fluidLevel <= -10 && fluidLevel != DimensionType.field_35479 && defaultFluidLevel.state != Blocks.LAVA.getDefaultState()) {
				int i = 64;
				int j = 40;
				int k = Math.floorDiv(blockX, 64);
				int l = Math.floorDiv(blockY, 40);
				int m = Math.floorDiv(blockZ, 64);
				double d = this.fluidTypeNoise.sample(new DensityFunction.UnblendedNoisePos(k, l, m));
				if (Math.abs(d) > 0.3) {
					blockState = Blocks.LAVA.getDefaultState();
				}
			}

			return blockState;
		}
	}
}
