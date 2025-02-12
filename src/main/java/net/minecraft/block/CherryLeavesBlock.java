package net.minecraft.block;

import net.minecraft.client.util.ParticleUtil;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.random.Random;
import net.minecraft.world.World;

public class CherryLeavesBlock extends LeavesBlock {
	public CherryLeavesBlock(Settings settings) {
		super(settings);
	}

	@Override
	public void randomDisplayTick(BlockState state, World world, BlockPos pos, Random random) {
		super.randomDisplayTick(state, world, pos, random);
		if (random.nextInt(10) == 0) {
			BlockPos blockPos = pos.down();
			BlockState blockState = world.getBlockState(blockPos);
			if (!isFaceFullSquare(blockState.getCollisionShape(world, blockPos), Direction.UP)) {
				ParticleUtil.spawnParticle(world, pos, random, ParticleTypes.CHERRY_LEAVES);
			}
		}
	}
}
