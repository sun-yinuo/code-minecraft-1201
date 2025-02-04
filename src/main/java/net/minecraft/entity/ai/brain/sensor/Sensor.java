package net.minecraft.entity.ai.brain.sensor;

import java.util.Set;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.ai.TargetPredicate;
import net.minecraft.entity.ai.brain.MemoryModuleType;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.random.Random;

/**
 * A sensor can update memories over time in a brain. The sensor's computation
 * replaces that of individual tasks, so that it is more efficient than the goal
 * system.
 * 
 * @see net.minecraft.entity.ai.brain.Brain#sensors
 */

/**
 * 感知器（Sensor）可以随着时间的推移更新大脑中的记忆。感知器的计算替代了单独任务的计算，因此比目标系统（Goal System）更加高效。
 * @see net.minecraft.entity.ai.brain.Brain#sensors
 */
public abstract class Sensor<E extends LivingEntity> {
	//线程安全的随机数生成器
	private static final Random RANDOM = Random.createThreadSafe();
	private static final int DEFAULT_RUN_TIME = 20;
	protected static final int BASE_MAX_DISTANCE = 16;

	//判断目标是否符合特定的攻击条件
	private static final TargetPredicate TARGET_PREDICATE = TargetPredicate.createNonAttackable().setBaseMaxDistance(16.0);
	private static final TargetPredicate TARGET_PREDICATE_IGNORE_DISTANCE_SCALING = TargetPredicate.createNonAttackable()
		.setBaseMaxDistance(16.0)
		.ignoreDistanceScalingFactor();
	private static final TargetPredicate ATTACKABLE_TARGET_PREDICATE = TargetPredicate.createAttackable().setBaseMaxDistance(16.0);
	private static final TargetPredicate ATTACKABLE_TARGET_PREDICATE_IGNORE_DISTANCE_SCALING = TargetPredicate.createAttackable()
		.setBaseMaxDistance(16.0)
		.ignoreDistanceScalingFactor();
	private static final TargetPredicate ATTACKABLE_TARGET_PREDICATE_IGNORE_VISIBILITY = TargetPredicate.createAttackable()
		.setBaseMaxDistance(16.0)
		.ignoreVisibility();
	private static final TargetPredicate ATTACKABLE_TARGET_PREDICATE_IGNORE_VISIBILITY_OR_DISTANCE_SCALING = TargetPredicate.createAttackable()
		.setBaseMaxDistance(16.0)
		.ignoreVisibility()
		.ignoreDistanceScalingFactor();
	//多少tick传感一次
	private final int senseInterval;
	//上次感知的时间
	private long lastSenseTime;

	/**
	 * 构造
	 * @param senseInterval
	 */
	public Sensor(int senseInterval) {
		this.senseInterval = senseInterval;
		this.lastSenseTime = (long)RANDOM.nextInt(senseInterval);
	}

	/**
	 * 构造
	 */
	public Sensor() {
		this(20);
	}

	/**
	 * tick
	 * @param world
	 * @param entity
	 */
	public final void tick(ServerWorld world, E entity) {
		//递减 到了<=0?
		if (--this.lastSenseTime <= 0L) {
			//赋值
			this.lastSenseTime = (long)this.senseInterval;
			//sense
			this.sense(world, entity);
		}
	}

	//抽象
	protected abstract void sense(ServerWorld world, E entity);

	//获取输出的记忆
	public abstract Set<MemoryModuleType<?>> getOutputMemoryModules();

	/**
	 * 用于测试目标是否符合条件
	 * @param entity
	 * @param target
	 * @return
	 */
	public static boolean testTargetPredicate(LivingEntity entity, LivingEntity target) {
		//有ATTACK_TARGET记忆 使用TARGET_PREDICATE_IGNORE_DISTANCE_SCALING
		//没有 TARGET_PREDICATE
		return entity.getBrain().hasMemoryModuleWithValue(MemoryModuleType.ATTACK_TARGET, target)
			? TARGET_PREDICATE_IGNORE_DISTANCE_SCALING.test(entity, target)
			: TARGET_PREDICATE.test(entity, target);
	}

	/**
	 * 测试目标是否可以被攻击
	 * @param entity
	 * @param target
	 * @return
	 */
	public static boolean testAttackableTargetPredicate(LivingEntity entity, LivingEntity target) {
		//有ATTACK_TARGET记忆 ATTACKABLE_TARGET_PREDICATE_IGNORE_DISTANCE_SCALING
		//没有 ATTACKABLE_TARGET_PREDICATE
		return entity.getBrain().hasMemoryModuleWithValue(MemoryModuleType.ATTACK_TARGET, target)
			? ATTACKABLE_TARGET_PREDICATE_IGNORE_DISTANCE_SCALING.test(entity, target)
			: ATTACKABLE_TARGET_PREDICATE.test(entity, target);
	}

	/**
	 * 检测是否能攻击但不考虑目标是否在视野范围内
	 * @param entity
	 * @param target
	 * @return
	 */
	public static boolean testAttackableTargetPredicateIgnoreVisibility(LivingEntity entity, LivingEntity target) {
		return entity.getBrain().hasMemoryModuleWithValue(MemoryModuleType.ATTACK_TARGET, target)
			? ATTACKABLE_TARGET_PREDICATE_IGNORE_VISIBILITY_OR_DISTANCE_SCALING.test(entity, target)
			: ATTACKABLE_TARGET_PREDICATE_IGNORE_VISIBILITY.test(entity, target);
	}
}
