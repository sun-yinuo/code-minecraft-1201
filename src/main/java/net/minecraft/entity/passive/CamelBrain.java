package net.minecraft.entity.passive;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.mojang.datafixers.util.Pair;
import java.util.function.Predicate;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.ai.brain.Activity;
import net.minecraft.entity.ai.brain.Brain;
import net.minecraft.entity.ai.brain.MemoryModuleState;
import net.minecraft.entity.ai.brain.MemoryModuleType;
import net.minecraft.entity.ai.brain.sensor.Sensor;
import net.minecraft.entity.ai.brain.sensor.SensorType;
import net.minecraft.entity.ai.brain.task.BreedTask;
import net.minecraft.entity.ai.brain.task.FleeTask;
import net.minecraft.entity.ai.brain.task.GoTowardsLookTargetTask;
import net.minecraft.entity.ai.brain.task.LookAroundTask;
import net.minecraft.entity.ai.brain.task.LookAtMobWithIntervalTask;
import net.minecraft.entity.ai.brain.task.MultiTickTask;
import net.minecraft.entity.ai.brain.task.RandomLookAroundTask;
import net.minecraft.entity.ai.brain.task.RandomTask;
import net.minecraft.entity.ai.brain.task.StayAboveWaterTask;
import net.minecraft.entity.ai.brain.task.StrollTask;
import net.minecraft.entity.ai.brain.task.TaskTriggerer;
import net.minecraft.entity.ai.brain.task.TemptTask;
import net.minecraft.entity.ai.brain.task.TemptationCooldownTask;
import net.minecraft.entity.ai.brain.task.WaitTask;
import net.minecraft.entity.ai.brain.task.WalkTowardClosestAdultTask;
import net.minecraft.entity.ai.brain.task.WanderAroundTask;
import net.minecraft.entity.mob.PathAwareEntity;
import net.minecraft.recipe.Ingredient;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.intprovider.UniformIntProvider;
import net.minecraft.util.math.random.Random;

public class CamelBrain {
	private static final float WALK_SPEED = 4.0F;
	private static final float field_40153 = 2.0F;
	private static final float field_40154 = 2.5F;
	private static final float field_40155 = 2.5F;
	private static final float BREED_SPEED = 1.0F;
	private static final UniformIntProvider WALK_TOWARD_ADULT_RANGE = UniformIntProvider.create(5, 16);
	private static final ImmutableList<SensorType<? extends Sensor<? super CamelEntity>>> SENSORS = ImmutableList.of(
		SensorType.NEAREST_LIVING_ENTITIES, SensorType.HURT_BY, SensorType.CAMEL_TEMPTATIONS, SensorType.NEAREST_ADULT
	);
	private static final ImmutableList<MemoryModuleType<?>> MEMORY_MODULES = ImmutableList.of(
		MemoryModuleType.IS_PANICKING,
		MemoryModuleType.HURT_BY,
		MemoryModuleType.HURT_BY_ENTITY,
		MemoryModuleType.WALK_TARGET,
		MemoryModuleType.LOOK_TARGET,
		MemoryModuleType.CANT_REACH_WALK_TARGET_SINCE,
		MemoryModuleType.PATH,
		MemoryModuleType.VISIBLE_MOBS,
		MemoryModuleType.TEMPTING_PLAYER,
		MemoryModuleType.TEMPTATION_COOLDOWN_TICKS,
		MemoryModuleType.GAZE_COOLDOWN_TICKS,
		MemoryModuleType.IS_TEMPTED,
		MemoryModuleType.BREED_TARGET,
		MemoryModuleType.NEAREST_VISIBLE_ADULT
	);

	protected static void initialize(CamelEntity camel, Random random) {
	}

	public static Brain.Profile<CamelEntity> createProfile() {
		return Brain.createProfile(MEMORY_MODULES, SENSORS);
	}

	protected static Brain<?> create(Brain<CamelEntity> brain) {
		addCoreActivities(brain);
		addIdleActivities(brain);
		brain.setCoreActivities(ImmutableSet.of(Activity.CORE));
		brain.setDefaultActivity(Activity.IDLE);
		brain.resetPossibleActivities();
		return brain;
	}

	private static void addCoreActivities(Brain<CamelEntity> brain) {
		brain.setTaskList(
			Activity.CORE,
			0,
			ImmutableList.of(
				new StayAboveWaterTask(0.8F),
				new CamelWalkTask(4.0F),
				new LookAroundTask(45, 90),
				new WanderAroundTask(),
				new TemptationCooldownTask(MemoryModuleType.TEMPTATION_COOLDOWN_TICKS),
				new TemptationCooldownTask(MemoryModuleType.GAZE_COOLDOWN_TICKS)
			)
		);
	}

	private static void addIdleActivities(Brain<CamelEntity> brain) {
		brain.setTaskList(
			Activity.IDLE,
			ImmutableList.of(
				Pair.of(0, LookAtMobWithIntervalTask.follow(EntityType.PLAYER, 6.0F, UniformIntProvider.create(30, 60))),
				Pair.of(1, new BreedTask(EntityType.CAMEL, 1.0F)),
				Pair.of(2, new TemptTask(entity -> 2.5F)),
				Pair.of(3, TaskTriggerer.runIf(Predicate.not(CamelEntity::isStationary), WalkTowardClosestAdultTask.create(WALK_TOWARD_ADULT_RANGE, 2.5F))),
				Pair.of(4, new RandomLookAroundTask(UniformIntProvider.create(150, 250), 30.0F, 0.0F, 0.0F)),
				Pair.of(
					5,
					new RandomTask<>(
						ImmutableMap.of(MemoryModuleType.WALK_TARGET, MemoryModuleState.VALUE_ABSENT),
						ImmutableList.of(
							Pair.of(TaskTriggerer.runIf(Predicate.not(CamelEntity::isStationary), StrollTask.create(2.0F)), 1),
							Pair.of(TaskTriggerer.runIf(Predicate.not(CamelEntity::isStationary), GoTowardsLookTargetTask.create(2.0F, 3)), 1),
							Pair.of(new SitOrStandTask(20), 1),
							Pair.of(new WaitTask(30, 60), 1)
						)
					)
				)
			)
		);
	}

	public static void updateActivities(CamelEntity camel) {
		camel.getBrain().resetPossibleActivities(ImmutableList.of(Activity.IDLE));
	}

	public static Ingredient getTemptItems() {
		return CamelEntity.BREEDING_INGREDIENT;
	}

	public static class CamelWalkTask extends FleeTask {
		public CamelWalkTask(float f) {
			super(f);
		}

		@Override
		protected void run(ServerWorld serverWorld, PathAwareEntity pathAwareEntity, long l) {
			if (pathAwareEntity instanceof CamelEntity camelEntity) {
				camelEntity.setStanding();
			}

			super.run(serverWorld, pathAwareEntity, l);
		}
	}

	public static class SitOrStandTask extends MultiTickTask<CamelEntity> {
		private final int lastPoseTickDelta;

		public SitOrStandTask(int lastPoseSecondsDelta) {
			super(ImmutableMap.of());
			this.lastPoseTickDelta = lastPoseSecondsDelta * 20;
		}

		protected boolean shouldRun(ServerWorld serverWorld, CamelEntity camelEntity) {
			return !camelEntity.isTouchingWater()
				&& camelEntity.getLastPoseTickDelta() >= (long)this.lastPoseTickDelta
				&& !camelEntity.isLeashed()
				&& camelEntity.isOnGround()
				&& !camelEntity.hasControllingPassenger();
		}

		protected void run(ServerWorld serverWorld, CamelEntity camelEntity, long l) {
			if (camelEntity.isSitting()) {
				camelEntity.startStanding();
			} else if (!camelEntity.isPanicking()) {
				camelEntity.startSitting();
			}
		}
	}
}
