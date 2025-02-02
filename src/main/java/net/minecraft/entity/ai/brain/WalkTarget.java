package net.minecraft.entity.ai.brain;

import net.minecraft.entity.Entity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;

//行走
public class WalkTarget {
	//目标
	private final LookTarget lookTarget;
	//速度
	private final float speed;
	//到达目标的范围
	private final int completionRange;

	/**
	 * 使用 BlockPos（方块坐标）创建 WalkTarget
	 * @param pos 方块坐标
	 * @param speed 速度
	 * @param completionRange 到达目标的范围
	 */
	public WalkTarget(BlockPos pos, float speed, int completionRange) {
		this(new BlockPosLookTarget(pos), speed, completionRange);
	}

	/**
	 *使 用 Vec3d（浮点坐标）创建 WalkTarget
	 * @param pos Vec3d
	 * @param speed 速度
	 * @param completionRange 到达目标的范围
	 */
	public WalkTarget(Vec3d pos, float speed, int completionRange) {
		this(new BlockPosLookTarget(BlockPos.ofFloored(pos)), speed, completionRange);
	}

	/**
	 * 使用 Entity（实体）作为目标
	 * @param entity 实体
	 * @param speed 速度
	 * @param completionRange 到达目标的范围
	 */
	public WalkTarget(Entity entity, float speed, int completionRange) {
		this(new EntityLookTarget(entity, false), speed, completionRange);
	}

	/**
	 * 构造
	 * @param lookTarget
	 * @param speed
	 * @param completionRange
	 */
	public WalkTarget(LookTarget lookTarget, float speed, int completionRange) {
		this.lookTarget = lookTarget;
		this.speed = speed;
		this.completionRange = completionRange;
	}

	public LookTarget getLookTarget() {
		return this.lookTarget;
	}

	public float getSpeed() {
		return this.speed;
	}

	public int getCompletionRange() {
		return this.completionRange;
	}
}
