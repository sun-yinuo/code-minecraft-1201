package net.minecraft.block;

import net.minecraft.util.math.BlockPos;
import net.minecraft.world.BlockView;

public class TintedGlassBlock extends AbstractGlassBlock {
	public TintedGlassBlock(Settings settings) {
		super(settings);
	}

	@Override
	public boolean isTransparent(BlockState state, BlockView world, BlockPos pos) {
		return false;
	}

	@Override
	public int getOpacity(BlockState state, BlockView world, BlockPos pos) {
		return world.getMaxLightLevel();
	}
}
