package net.minecraft.entity.ai.brain;

import net.minecraft.entity.LivingEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;

//look接口
public interface LookTarget {
	/**
	 * 坐标
	 * @return 坐标
	 */
	Vec3d getPos();

	/**
	 * 方块坐标
	 * @return 坐标
	 */
	BlockPos getBlockPos();

	/**
	 * 是否被看到
	 * @param entity 实体
	 * @return boolean
	 */
	boolean isSeenBy(LivingEntity entity);
}
