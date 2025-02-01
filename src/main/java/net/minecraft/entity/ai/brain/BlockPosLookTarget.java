package net.minecraft.entity.ai.brain;

import net.minecraft.entity.LivingEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;

public class BlockPosLookTarget implements LookTarget {
	//方块坐标
	private final BlockPos blockPos;

	private final Vec3d pos;


	public BlockPosLookTarget(BlockPos blockPos) {
		//返回一个不可变的块位置，其 x、y 和 z 与 this 相同的位置
		this.blockPos = blockPos.toImmutable();
		//创建一个表示给定块位置的中心的向量
		this.pos = Vec3d.ofCenter(blockPos);
	}


	public BlockPosLookTarget(Vec3d pos) {
		//取整
		this.blockPos = BlockPos.ofFloored(pos);
		this.pos = pos;
	}


	/**
	 * getPos
	 * @return Vec3d
	 */
	@Override
	public Vec3d getPos() {
		return this.pos;
	}

	/**
	 * 获取方块位置
	 * @return BlockPos
	 */
	@Override
	public BlockPos getBlockPos() {
		return this.blockPos;
	}

	/**
	 * 被看到？意义不明
	 * @param entity entity
	 * @return boolean
	 */
	@Override
	public boolean isSeenBy(LivingEntity entity) {
		return true;
	}

	public String toString() {
		return "BlockPosTracker{blockPos=" + this.blockPos + ", centerPosition=" + this.pos + "}";
	}
}
