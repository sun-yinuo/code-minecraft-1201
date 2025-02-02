package net.minecraft.entity.ai.brain;

import java.util.Optional;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;

//实体注视目标
public class EntityLookTarget implements LookTarget {
	//实体
	private final Entity entity;
	//否使用实体的眼睛高度作为注视点
	private final boolean useEyeHeight;

	/**
	 * 构造
	 * @param entity
	 * @param useEyeHeight
	 */
	public EntityLookTarget(Entity entity, boolean useEyeHeight) {
		this.entity = entity;
		this.useEyeHeight = useEyeHeight;
	}

	/**
	 * 获取注视位置
	 * @return 位置
	 */
	@Override
	public Vec3d getPos() {
		//true => 实体基础位置+眼镜高度
		//false => 直接返回基础位置
		return this.useEyeHeight ? this.entity.getPos().add(0.0, (double)this.entity.getStandingEyeHeight(), 0.0) : this.entity.getPos();
	}

	/**
	 * 获取目标所在的方块位置
	 * @return 位置
	 */
	@Override
	public BlockPos getBlockPos() {
		return this.entity.getBlockPos();
	}

	/**
	 * 是否被任意一个LivingEntity entity注视👀
	 * @param entity entity
	 * @return result
	 */
	@Override
	public boolean isSeenBy(LivingEntity entity) {
		//检查是否是LivingEntity
		if (this.entity instanceof LivingEntity livingEntity) {
			//是否存活
			if (!livingEntity.isAlive()) {
				//不存活，false
				return false;
			} else {
				//
				Optional<LivingTargetCache> optional = entity.getBrain().getOptionalRegisteredMemory(MemoryModuleType.VISIBLE_MOBS);
				return optional.isPresent() && ((LivingTargetCache)optional.get()).contains(livingEntity);
			}
		} else {
			return true;
		}
	}

	public Entity getEntity() {
		return this.entity;
	}

	public String toString() {
		return "EntityTracker for " + this.entity;
	}
}
