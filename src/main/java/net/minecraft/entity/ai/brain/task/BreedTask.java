package net.minecraft.entity.ai.brain.task;

import com.google.common.collect.ImmutableMap;
import java.util.Optional;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.ai.brain.Brain;
import net.minecraft.entity.ai.brain.LivingTargetCache;
import net.minecraft.entity.ai.brain.MemoryModuleState;
import net.minecraft.entity.ai.brain.MemoryModuleType;
import net.minecraft.entity.passive.AnimalEntity;
import net.minecraft.entity.passive.PassiveEntity;
import net.minecraft.server.world.ServerWorld;

public class BreedTask extends MultiTickTask<AnimalEntity> {

	private static final int MAX_RANGE = 3;
	private static final int MIN_BREED_TIME = 60;
	private static final int RUN_TIME = 110;
	private final EntityType<? extends AnimalEntity> targetType;
	private final float speed;
	private long breedTime;

	public BreedTask(EntityType<? extends AnimalEntity> targetType, float speed) {
		super(
			ImmutableMap.of(
				MemoryModuleType.VISIBLE_MOBS,
				MemoryModuleState.VALUE_PRESENT,
				MemoryModuleType.BREED_TARGET,
				MemoryModuleState.VALUE_ABSENT,
				MemoryModuleType.WALK_TARGET,
				MemoryModuleState.REGISTERED,
				MemoryModuleType.LOOK_TARGET,
				MemoryModuleState.REGISTERED
			),
			RUN_TIME
		);
		this.targetType = targetType;
		this.speed = speed;
	}

	protected boolean shouldRun(ServerWorld serverWorld, AnimalEntity animalEntity) {
		return animalEntity.isInLove() && this.findBreedTarget(animalEntity).isPresent();//生物在发情且找到了交配对象
	}

	protected void run(ServerWorld serverWorld, AnimalEntity animalEntity, long l) {
		AnimalEntity animalEntity2 = (AnimalEntity)this.findBreedTarget(animalEntity).get();//获取交配对象(animalEntity1与animalEntity2)
		animalEntity.getBrain().remember(MemoryModuleType.BREED_TARGET, animalEntity2);//设置animalEntity1的交配对象为animalEntity2
		animalEntity2.getBrain().remember(MemoryModuleType.BREED_TARGET, animalEntity);//设置animalEntity2的交配对象为animalEntity1(互相设置为对方的交配对象)
		LookTargetUtil.lookAtAndWalkTowardsEachOther(animalEntity, animalEntity2, this.speed);//让两个生物互相看向对方并以speed速度向对方移动
		int i = MIN_BREED_TIME + animalEntity.getRandom().nextInt(50);//随机生成一个MIN_BREED_TIME到MIN_BREED_TIME+50的整数，作为交配时间
		this.breedTime = l + (long)i;//设置交配时间
	}

	protected boolean shouldKeepRunning(ServerWorld serverWorld, AnimalEntity animalEntity, long l) {
		if (!this.hasBreedTarget(animalEntity)) {
			return false;//如果animalEntity没有交配对象，返回false,即shouldn't keep running
		} else {
			AnimalEntity animalEntity2 = this.getBreedTarget(animalEntity);//获取交配对象
			return animalEntity2.isAlive()//交配对象存活
				&& animalEntity.canBreedWith(animalEntity2)//animalEntity有没有雕
				&& LookTargetUtil.canSee(animalEntity.getBrain(), animalEntity2)//animalEntity能不能看到交配对象
				&& l <= this.breedTime;//没看懂(l变量意义不明)
		}
	}

	protected void keepRunning(ServerWorld serverWorld, AnimalEntity animalEntity, long l) {
		AnimalEntity animalEntity2 = this.getBreedTarget(animalEntity);//获取交配对象
		LookTargetUtil.lookAtAndWalkTowardsEachOther(animalEntity, animalEntity2, this.speed);//让两个生物互相看向对方并以speed速度向对方移动
		if (animalEntity.isInRange(animalEntity2, MAX_RANGE)) {//如果两个生物距离小于等于MAX_RANGE
			if (l >= this.breedTime) {//没看懂
				animalEntity.breed(serverWorld, animalEntity2);//交配❤️
				animalEntity.getBrain().forget(MemoryModuleType.BREED_TARGET);//animalEntity忘记交配对象
				animalEntity2.getBrain().forget(MemoryModuleType.BREED_TARGET);//animalEntity2忘记交配对象(互相忘记)
			}
		}
	}

	protected void finishRunning(ServerWorld serverWorld, AnimalEntity animalEntity, long l) {
		animalEntity.getBrain().forget(MemoryModuleType.BREED_TARGET);//animalEntity忘记交配对象
		animalEntity.getBrain().forget(MemoryModuleType.WALK_TARGET);//animalEntity忘记走向目标
		animalEntity.getBrain().forget(MemoryModuleType.LOOK_TARGET);//animalEntity忘记看向目标
		this.breedTime = 0L;//重置交配时间
	}

	private AnimalEntity getBreedTarget(AnimalEntity animal) {
		return (AnimalEntity)animal.getBrain().getOptionalRegisteredMemory(MemoryModuleType.BREED_TARGET).get();//获取交配对象
	}

	private boolean hasBreedTarget(AnimalEntity animal) {//返回是否有交配对象
		Brain<?> brain = animal.getBrain();//获取animal的大脑
		return brain.hasMemoryModule(MemoryModuleType.BREED_TARGET)//判断是否有交配对象
			&& ((PassiveEntity)brain.getOptionalRegisteredMemory(MemoryModuleType.BREED_TARGET).get()).getType() == this.targetType;//判断交配对象是否是目标类型
	}

	private Optional<? extends AnimalEntity> findBreedTarget(AnimalEntity animal) {//返回一个交配对象
		return ((LivingTargetCache)animal.getBrain().getOptionalRegisteredMemory(MemoryModuleType.VISIBLE_MOBS).get()).findFirst(entity -> {//获取animal的可见生物(貌似是视线内的生物)
			if (entity.getType() == this.targetType && entity instanceof AnimalEntity animalEntity2 && animal.canBreedWith(animalEntity2)) {//判断entity是否是目标类型且可以与animal交配
				return true;
			}

			return false;
		}).map(AnimalEntity.class::cast);//转换为AnimalEntity类型
	}
}
