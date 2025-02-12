package net.minecraft.world;

import java.util.List;
import java.util.Optional;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.block.entity.BlockEntityType;
import net.minecraft.entity.Entity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.shape.VoxelShape;
import org.jetbrains.annotations.Nullable;

/**
 * A world view or {@link World}'s superinterface that exposes access to
 * a registry manager.
 * 
 * @see #getRegistryManager()
 */
public interface RegistryWorldView extends EntityView, WorldView, ModifiableTestableWorld {
	@Override
	default <T extends BlockEntity> Optional<T> getBlockEntity(BlockPos pos, BlockEntityType<T> type) {
		return WorldView.super.getBlockEntity(pos, type);
	}

	@Override
	default List<VoxelShape> getEntityCollisions(@Nullable Entity entity, Box box) {
		return EntityView.super.getEntityCollisions(entity, box);
	}

	@Override
	default boolean doesNotIntersectEntities(@Nullable Entity except, VoxelShape shape) {
		return EntityView.super.doesNotIntersectEntities(except, shape);
	}

	@Override
	default BlockPos getTopPosition(Heightmap.Type heightmap, BlockPos pos) {
		return WorldView.super.getTopPosition(heightmap, pos);
	}
}
