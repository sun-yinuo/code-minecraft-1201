package net.minecraft.item;

import net.minecraft.block.AbstractSignBlock;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.entity.SignBlockEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;

public class SignItem extends VerticallyAttachableBlockItem {
	public SignItem(Settings settings, Block standingBlock, Block wallBlock) {
		super(standingBlock, wallBlock, settings, Direction.DOWN);
	}

	public SignItem(Settings settings, Block standingBlock, Block wallBlock, Direction verticalAttachmentDirection) {
		super(standingBlock, wallBlock, settings, verticalAttachmentDirection);
	}

	@Override
	protected boolean postPlacement(BlockPos pos, World world, @Nullable PlayerEntity player, ItemStack stack, BlockState state) {
		boolean bl = super.postPlacement(pos, world, player, stack, state);
		if (!world.isClient
			&& !bl
			&& player != null
			&& world.getBlockEntity(pos) instanceof SignBlockEntity signBlockEntity
			&& world.getBlockState(pos).getBlock() instanceof AbstractSignBlock abstractSignBlock) {
			abstractSignBlock.openEditScreen(player, signBlockEntity, true);
		}

		return bl;
	}
}
