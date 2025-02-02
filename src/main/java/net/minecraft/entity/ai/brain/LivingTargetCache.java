package net.minecraft.entity.ai.brain;

import com.google.common.collect.Iterables;
import it.unimi.dsi.fastutil.objects.Object2BooleanOpenHashMap;
import java.util.List;
import java.util.Optional;
import java.util.function.Predicate;
import java.util.stream.Stream;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.ai.brain.sensor.Sensor;

//目标实体缓存
public class LivingTargetCache {

	private static final LivingTargetCache EMPTY = new LivingTargetCache();
	//list
	private final List<LivingEntity> entities;
	//过滤条件
	private final Predicate<LivingEntity> targetPredicate;

	/**
	 * 构造
	 */
	private LivingTargetCache() {
		this.entities = List.of();
		this.targetPredicate = entity -> false;
	}

	/**
	 * 参数构造
	 * @param owner
	 * @param entities
	 */
	public LivingTargetCache(LivingEntity owner, List<LivingEntity> entities) {
		this.entities = entities;
		Object2BooleanOpenHashMap<LivingEntity> object2BooleanOpenHashMap = new Object2BooleanOpenHashMap<>(entities.size());
		Predicate<LivingEntity> predicate = entity -> Sensor.testTargetPredicate(owner, entity);
		this.targetPredicate = entity -> object2BooleanOpenHashMap.computeIfAbsent(entity, predicate);
	}


	public static LivingTargetCache empty() {
		return EMPTY;
	}


	/**
	 * 查找第一个符合条件的实体
	 * @param predicate 条件
	 * @return Optional
	 */
	public Optional<LivingEntity> findFirst(Predicate<LivingEntity> predicate) {
		for (LivingEntity livingEntity : this.entities) {
			if (predicate.test(livingEntity) && this.targetPredicate.test(livingEntity)) {
				return Optional.of(livingEntity);
			}
		}

		return Optional.empty();
	}

	/**
	 * 返回满足条件的实体可迭代对象
	 * @param predicate 条件
	 * @return Iterable
	 */
	public Iterable<LivingEntity> iterate(Predicate<LivingEntity> predicate) {
		return Iterables.filter(this.entities, entity -> predicate.test(entity) && this.targetPredicate.test(entity));
	}

	/**
	 * 返回满足条件的实体流
	 * @param predicate 条件
	 * @return 流
	 */
	public Stream<LivingEntity> stream(Predicate<LivingEntity> predicate) {
		return this.entities.stream().filter(entity -> predicate.test(entity) && this.targetPredicate.test(entity));
	}

	/**
	 * 判断某个实体是否在缓存里
	 * @param entity 实体
	 * @return boolean
	 */
	public boolean contains(LivingEntity entity) {
		//先检查在不在 再检查满不满足条件
		return this.entities.contains(entity) && this.targetPredicate.test(entity);
	}

	/**
	 * 判断是否存在至少一个符合条件的实体
	 * @param predicate 条件
	 * @return boolean
	 */
	public boolean anyMatch(Predicate<LivingEntity> predicate) {
		for (LivingEntity livingEntity : this.entities) {
			if (predicate.test(livingEntity) && this.targetPredicate.test(livingEntity)) {
				return true;
			}
		}

		return false;
	}
}
