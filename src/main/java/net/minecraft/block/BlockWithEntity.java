package net.minecraft.block;

import net.minecraft.block.entity.BlockEntity;
import net.minecraft.block.entity.BlockEntityTicker;
import net.minecraft.block.entity.BlockEntityType;
import net.minecraft.screen.NamedScreenHandlerFactory;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;

/**
 * A convenience class for a block with a {@link BlockEntity}.
 * While blocks with block entity only have to implement {@link BlockEntityProvider}
 * and do not have to subclass this, it overrides several methods to delegate its logic
 * to the block entity. However, <strong>it is generally easier to just implement
 * {@link BlockEntityProvider}</strong>.
 * 
 * <p><strong>Subclasses must override {@link #getRenderType}</strong> to render the
 * block entity. By default, all block entities are rendered invisible, which is not
 * intended in most, if not all, cases.
 * 
 * @see BlockEntity
 * @see BlockEntityProvider
 */
public abstract class BlockWithEntity extends Block implements BlockEntityProvider {
	protected BlockWithEntity(Settings settings) {
		super(settings);
	}

	@Override
	public BlockRenderType getRenderType(BlockState state) {
		return BlockRenderType.INVISIBLE;
	}

	@Override
	public boolean onSyncedBlockEvent(BlockState state, World world, BlockPos pos, int type, int data) {
		super.onSyncedBlockEvent(state, world, pos, type, data);
		BlockEntity blockEntity = world.getBlockEntity(pos);
		return blockEntity == null ? false : blockEntity.onSyncedBlockEvent(type, data);
	}

	@Nullable
	@Override
	public NamedScreenHandlerFactory createScreenHandlerFactory(BlockState state, World world, BlockPos pos) {
		BlockEntity blockEntity = world.getBlockEntity(pos);
		return blockEntity instanceof NamedScreenHandlerFactory ? (NamedScreenHandlerFactory)blockEntity : null;
	}

	/**
	 * {@return the ticker if the given type and expected type are the same, or {@code null} if they are different}
	 */
	@Nullable
	protected static <E extends BlockEntity, A extends BlockEntity> BlockEntityTicker<A> checkType(
		BlockEntityType<A> givenType, BlockEntityType<E> expectedType, BlockEntityTicker<? super E> ticker
	) {
		return expectedType == givenType ? ticker : null;
	}
}
