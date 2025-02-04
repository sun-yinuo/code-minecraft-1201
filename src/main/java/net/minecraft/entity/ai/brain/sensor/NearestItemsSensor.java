package net.minecraft.entity.ai.brain.sensor;

import com.google.common.collect.ImmutableSet;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import net.minecraft.entity.ItemEntity;
import net.minecraft.entity.ai.brain.Brain;
import net.minecraft.entity.ai.brain.MemoryModuleType;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.server.world.ServerWorld;

//最近物体的检测
//影响NEAREST_VISIBLE_WANTED_ITEM记忆
public class NearestItemsSensor extends Sensor<MobEntity> {
	private static final long HORIZONTAL_RANGE = 32L;
	private static final long VERTICAL_RANGE = 16L;
	public static final int MAX_RANGE = 32;


	/**
	 * 返回产生的记忆类型
	 * @return 记忆set
	 */
	@Override
	public Set<MemoryModuleType<?>> getOutputMemoryModules() {
		//返回包含MemoryModuleType.NEAREST_VISIBLE_WANTED_ITEM的set
		return ImmutableSet.of(MemoryModuleType.NEAREST_VISIBLE_WANTED_ITEM);
	}


	protected void sense(ServerWorld serverWorld, MobEntity mobEntity) {
		//获取大脑
		Brain<?> brain = mobEntity.getBrain();
		//获取当前实体周围 32x16x32内的
		List<ItemEntity> list = serverWorld.getEntitiesByClass(ItemEntity.class, mobEntity.getBoundingBox().expand(32.0, 16.0, 32.0), itemEntity -> true);
		//按照实体与物体的距离排序
		list.sort(Comparator.comparingDouble(mobEntity::squaredDistanceTo));

		Optional<ItemEntity> optional = list.stream()
			//检查是否可以收集
			.filter(itemEntity -> mobEntity.canGather(itemEntity.getStack()))
			//检查在不在32范围内
			.filter(itemEntity -> itemEntity.isInRange(mobEntity, 32.0))
			//检查是否可以看到
			.filter(mobEntity::canSee)
			//返回第一个
			.findFirst();
		//修改记忆
		brain.remember(MemoryModuleType.NEAREST_VISIBLE_WANTED_ITEM, optional);
	}
}
