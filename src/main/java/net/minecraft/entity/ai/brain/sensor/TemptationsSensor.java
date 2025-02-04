package net.minecraft.entity.ai.brain.sensor;

import com.google.common.collect.ImmutableSet;
import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import net.minecraft.entity.ai.TargetPredicate;
import net.minecraft.entity.ai.brain.Brain;
import net.minecraft.entity.ai.brain.MemoryModuleType;
import net.minecraft.entity.mob.PathAwareEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.predicate.entity.EntityPredicates;
import net.minecraft.recipe.Ingredient;
import net.minecraft.server.world.ServerWorld;

//诱惑传感器
//影响TEMPTING_PLAYER记忆
public class TemptationsSensor extends Sensor<PathAwareEntity> {
	//无用法 最大检测距离
	public static final int MAX_DISTANCE = 10;
	//检测目标的条件
	private static final TargetPredicate TEMPTER_PREDICATE = TargetPredicate
			//不能被攻击
			.createNonAttackable()
			//实体之间的最大距离为10
			.setBaseMaxDistance(10.0)
			//忽略目标的可见性
			.ignoreVisibility();

	private final Ingredient ingredient;


	public TemptationsSensor(Ingredient ingredient) {
		this.ingredient = ingredient;
	}

	protected void sense(ServerWorld serverWorld, PathAwareEntity pathAwareEntity) {
		//大脑
		Brain<?> brain = pathAwareEntity.getBrain();
		List<PlayerEntity> list = (List<PlayerEntity>)serverWorld.getPlayers()
			.stream()
			//排除旁观者玩家
			.filter(EntityPredicates.EXCEPT_SPECTATOR)
			//TEMPTER_PREDICATE过滤玩家
			.filter(player -> TEMPTER_PREDICATE.test(pathAwareEntity, player))
			//在实体的10个范围内
			.filter(player -> pathAwareEntity.isInRange(player, 10.0))
			//过滤
			.filter(this::test)
			//玩家不能是实体的乘客
			.filter(serverPlayerEntity -> !pathAwareEntity.hasPassenger(serverPlayerEntity))
			//按玩家与实体之间的距离排序
			.sorted(Comparator.comparingDouble(pathAwareEntity::squaredDistanceTo))
			//copy到列表
			.collect(Collectors.toList());

		//不为空
		if (!list.isEmpty()) {
			//第一个
			PlayerEntity playerEntity = (PlayerEntity)list.get(0);
			//remember
			brain.remember(MemoryModuleType.TEMPTING_PLAYER, playerEntity);
		} else {
			//忘记
			brain.forget(MemoryModuleType.TEMPTING_PLAYER);
		}
	}

	//是否在主手或副手中持有符合诱惑条件的物品
	private boolean test(PlayerEntity player) {
		return this.test(player.getMainHandStack()) || this.test(player.getOffHandStack());
	}

	//是否符合诱惑条件
	private boolean test(ItemStack stack) {
		return this.ingredient.test(stack);
	}

	@Override
	public Set<MemoryModuleType<?>> getOutputMemoryModules() {
		return ImmutableSet.of(MemoryModuleType.TEMPTING_PLAYER);
	}
}
