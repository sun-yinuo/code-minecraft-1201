package net.minecraft.entity.ai.brain.task;

import java.util.function.Predicate;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.ai.brain.MemoryModuleType;

public class ForgetTask {
	/**
	 *如果实体满足条件，则忘记记忆。
	 * @param condition 测试实体是否应该忘记记忆的条件
	 * @param memory 要忘记的记忆类型
	 * @return 是否忘记记忆
	 * @param <E> 实体类型(LivingEntity的子类)
	 */
	public static <E extends LivingEntity> Task<E> create(Predicate<E> condition, MemoryModuleType<?> memory) {//下面的lambda表达式不想看了
		return TaskTriggerer.task(context -> context.group(context.queryMemoryValue(memory)).apply(context, queryResult -> (world, entity, time) -> {
					if (condition.test(entity)) {//判断实体是否满足遗忘条件
						queryResult.forget();//遗忘记忆
						return true;
					} else {
						return false;
					}
				}));
	}
}
