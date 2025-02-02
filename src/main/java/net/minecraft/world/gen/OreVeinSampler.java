package net.minecraft.world.gen;

import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.random.Random;
import net.minecraft.util.math.random.RandomSplitter;
import net.minecraft.world.gen.chunk.ChunkNoiseSampler;
import net.minecraft.world.gen.densityfunction.DensityFunction;

public final class OreVeinSampler {
	/**
	 * The density threshold required to generate any blocks as part of an ore vein.
	 */
	private static final float DENSITY_THRESHOLD = 0.4F;
	/**
	 * The number of blocks away from the minimum or maximum height at which
	 * ores in an ore vein generates at the maximum density.
	 */
	private static final int MAX_DENSITY_INTRUSION = 20;
	/**
	 * The decrease in density at the minimum or maximum height compared to the
	 * maximum density.
	 */
	private static final double LIMINAL_DENSITY_REDUCTION = 0.2;
	/**
	 * The probability that a given block will be replaced by an ore vein,
	 * given that the density check has passed.
	 */
	private static final float BLOCK_GENERATION_CHANCE = 0.7F;
	private static final float MIN_ORE_CHANCE = 0.1F;
	private static final float MAX_ORE_CHANCE = 0.3F;
	private static final float DENSITY_FOR_MAX_ORE_CHANCE = 0.6F;
	private static final float RAW_ORE_BLOCK_CHANCE = 0.02F;
	private static final float VEIN_GAP_THRESHOLD = -0.3F;

	private OreVeinSampler() {
	}

	protected static ChunkNoiseSampler.BlockStateSampler create(
		DensityFunction veinToggle, DensityFunction veinRidged, DensityFunction veinGap, RandomSplitter randomDeriver
	) {
		BlockState blockState = null;
		return pos -> {
			double d = veinToggle.sample(pos);
			int i = pos.blockY();
			VeinType veinType = d > 0.0 ? VeinType.COPPER : VeinType.IRON;
			double e = Math.abs(d);
			int j = veinType.maxY - i;
			int k = i - veinType.minY;
			if (k >= 0 && j >= 0) {
				int l = Math.min(j, k);
				double f = MathHelper.clampedMap((double)l, 0.0, 20.0, -0.2, 0.0);
				if (e + f < 0.4F) {
					return blockState;
				} else {
					Random random = randomDeriver.split(pos.blockX(), i, pos.blockZ());
					if (random.nextFloat() > 0.7F) {
						return blockState;
					} else if (veinRidged.sample(pos) >= 0.0) {
						return blockState;
					} else {
						double g = MathHelper.clampedMap(e, 0.4F, 0.6F, 0.1F, 0.3F);
						if ((double)random.nextFloat() < g && veinGap.sample(pos) > -0.3F) {
							return random.nextFloat() < 0.02F ? veinType.rawOreBlock : veinType.ore;
						} else {
							return veinType.stone;
						}
					}
				}
			} else {
				return blockState;
			}
		};
	}

	protected static enum VeinType {
		COPPER(Blocks.COPPER_ORE.getDefaultState(), Blocks.RAW_COPPER_BLOCK.getDefaultState(), Blocks.GRANITE.getDefaultState(), 0, 50),
		IRON(Blocks.DEEPSLATE_IRON_ORE.getDefaultState(), Blocks.RAW_IRON_BLOCK.getDefaultState(), Blocks.TUFF.getDefaultState(), -60, -8);

		final BlockState ore;
		final BlockState rawOreBlock;
		final BlockState stone;
		protected final int minY;
		protected final int maxY;

		private VeinType(BlockState ore, BlockState rawOreBlock, BlockState stone, int minY, int maxY) {
			this.ore = ore;
			this.rawOreBlock = rawOreBlock;
			this.stone = stone;
			this.minY = minY;
			this.maxY = maxY;
		}
	}
}
