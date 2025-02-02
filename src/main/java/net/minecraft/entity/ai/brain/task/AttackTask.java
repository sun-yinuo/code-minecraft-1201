package net.minecraft.entity.ai.brain.task;

import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.ai.brain.EntityLookTarget;
import net.minecraft.entity.ai.brain.LivingTargetCache;
import net.minecraft.entity.ai.brain.MemoryModuleType;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.util.math.MathHelper;

public class AttackTask {
	/**
	 * 处理怪物的攻击逻辑
	 * @param distance 攻击范围，在这个范围内才会触发攻击
	 * @param forwardMovement 前后移动距离(貌似负数为倒退)
	 * @return 一个单次触发的任务(SingleTickTask<MobEntity>),用于在特定条件下触发攻击行为
	 */
	public static SingleTickTask<MobEntity> create(int distance, float forwardMovement) {
		return TaskTriggerer.task(
			context -> context.group(
						context.queryMemoryAbsent(MemoryModuleType.WALK_TARGET),
						context.queryMemoryOptional(MemoryModuleType.LOOK_TARGET),
						context.queryMemoryValue(MemoryModuleType.ATTACK_TARGET),
						context.queryMemoryValue(MemoryModuleType.VISIBLE_MOBS)
					)
					.apply(context, (walkTarget, lookTarget, attackTarget, visibleMobs) -> (world, entity, time) -> {
							LivingEntity livingEntity = context.getValue(attackTarget);//获取攻击目标
							if (livingEntity.isInRange(entity, distance) && context.getValue(visibleMobs).contains(livingEntity)) {//判断攻击目标是否在攻击范围内
								lookTarget.remember(new EntityLookTarget(livingEntity, true));//记住攻击目标
								entity.getMoveControl().strafeTo(-forwardMovement, 0.0F);//向攻击目标移动(倒退?)
								entity.setYaw(MathHelper.clampAngle(entity.getYaw(), entity.headYaw, 0.0F));//调整朝向，使身体朝向与头部朝向一致
								return true;//返回true表示任务执行成功
							} else {
								return false;//返回false表示不满足inRange条件
							}
						})
		);
	}
}
