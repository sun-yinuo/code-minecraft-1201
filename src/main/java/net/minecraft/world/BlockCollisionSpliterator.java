package net.minecraft.world;

import com.google.common.collect.AbstractIterator;
import java.util.function.BiFunction;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.ShapeContext;
import net.minecraft.entity.Entity;
import net.minecraft.util.CuboidBlockIterator;
import net.minecraft.util.function.BooleanBiFunction;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.util.math.ChunkSectionPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.util.shape.VoxelShapes;
import org.jetbrains.annotations.Nullable;

public class BlockCollisionSpliterator<T> extends AbstractIterator<T> {
	private final Box box;
	private final ShapeContext context;
	private final CuboidBlockIterator blockIterator;
	private final BlockPos.Mutable pos;
	private final VoxelShape boxShape;
	private final CollisionView world;
	private final boolean forEntity;
	@Nullable
	private BlockView chunk;
	private long chunkPos;
	private final BiFunction<BlockPos.Mutable, VoxelShape, T> resultFunction;

	public BlockCollisionSpliterator(
		CollisionView world, @Nullable Entity entity, Box box, boolean forEntity, BiFunction<BlockPos.Mutable, VoxelShape, T> resultFunction
	) {
		this.context = entity == null ? ShapeContext.absent() : ShapeContext.of(entity);
		this.pos = new BlockPos.Mutable();
		this.boxShape = VoxelShapes.cuboid(box);
		this.world = world;
		this.box = box;
		this.forEntity = forEntity;
		this.resultFunction = resultFunction;
		int i = MathHelper.floor(box.minX - 1.0E-7) - 1;
		int j = MathHelper.floor(box.maxX + 1.0E-7) + 1;
		int k = MathHelper.floor(box.minY - 1.0E-7) - 1;
		int l = MathHelper.floor(box.maxY + 1.0E-7) + 1;
		int m = MathHelper.floor(box.minZ - 1.0E-7) - 1;
		int n = MathHelper.floor(box.maxZ + 1.0E-7) + 1;
		this.blockIterator = new CuboidBlockIterator(i, k, m, j, l, n);
	}

	@Nullable
	private BlockView getChunk(int x, int z) {
		int i = ChunkSectionPos.getSectionCoord(x);
		int j = ChunkSectionPos.getSectionCoord(z);
		long l = ChunkPos.toLong(i, j);
		if (this.chunk != null && this.chunkPos == l) {
			return this.chunk;
		} else {
			BlockView blockView = this.world.getChunkAsView(i, j);
			this.chunk = blockView;
			this.chunkPos = l;
			return blockView;
		}
	}

	@Override
	protected T computeNext() {
		while (this.blockIterator.step()) {
			int i = this.blockIterator.getX();
			int j = this.blockIterator.getY();
			int k = this.blockIterator.getZ();
			int l = this.blockIterator.getEdgeCoordinatesCount();
			if (l != 3) {
				BlockView blockView = this.getChunk(i, k);
				if (blockView != null) {
					this.pos.set(i, j, k);
					BlockState blockState = blockView.getBlockState(this.pos);
					if ((!this.forEntity || blockState.shouldSuffocate(blockView, this.pos))
						&& (l != 1 || blockState.exceedsCube())
						&& (l != 2 || blockState.isOf(Blocks.MOVING_PISTON))) {
						VoxelShape voxelShape = blockState.getCollisionShape(this.world, this.pos, this.context);
						if (voxelShape == VoxelShapes.fullCube()) {
							if (this.box.intersects((double)i, (double)j, (double)k, (double)i + 1.0, (double)j + 1.0, (double)k + 1.0)) {
								return (T)this.resultFunction.apply(this.pos, voxelShape.offset((double)i, (double)j, (double)k));
							}
						} else {
							VoxelShape voxelShape2 = voxelShape.offset((double)i, (double)j, (double)k);
							if (!voxelShape2.isEmpty() && VoxelShapes.matchesAnywhere(voxelShape2, this.boxShape, BooleanBiFunction.AND)) {
								return (T)this.resultFunction.apply(this.pos, voxelShape2);
							}
						}
					}
				}
			}
		}

		return this.endOfData();
	}
}
