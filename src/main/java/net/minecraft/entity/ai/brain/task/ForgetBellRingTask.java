package net.minecraft.entity.ai.brain.task;

import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.ai.brain.MemoryModuleType;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.GlobalPos;
import org.apache.commons.lang3.mutable.MutableInt;

public class ForgetBellRingTask {
	private static final int MIN_HEARD_BELL_TIME = 300;

	/**
	 *
	 * @param maxHiddenSeconds 最大隐藏时间
	 * @param distance 隐藏位置的有效半径,生物需在此范围内累计隐藏时间
	 * @return Task&lt;LivingEntity&gt;
	 */
	public static Task<LivingEntity> create(int maxHiddenSeconds, int distance) {
		int i = maxHiddenSeconds * 20;//最大隐藏时间转换为gt
		MutableInt mutableInt = new MutableInt(0);//隐藏时间计数器
		return TaskTriggerer.task(
			context -> context.group(context.queryMemoryValue(MemoryModuleType.HIDING_PLACE), context.queryMemoryValue(MemoryModuleType.HEARD_BELL_TIME))
					//获取隐藏位置和听到铃声的时间(时间戳)
					.apply(context, (hidingPlace, heardBellTime) -> (world, entity, time) -> {
							long l = context.<Long>getValue(heardBellTime);//听到铃声的时间
							boolean bl = l + MIN_HEARD_BELL_TIME <= time;//听到铃声的时间是否超过300gt(最低隐藏时间)
							if (mutableInt.getValue() <= i && !bl) {//未超过最大隐藏时间且未满足最低时间
								BlockPos blockPos = context.<GlobalPos>getValue(hidingPlace).getPos();//获取隐藏位置
								if (blockPos.isWithinDistance(entity.getBlockPos(), (double)distance)) {//判断生物是否在隐藏位置的有效半径内
									mutableInt.increment();//隐藏时间计数器+1
								}

								return true;
							} else {//超过最大隐藏时间或满足最低时间
								heardBellTime.forget();//忘记听到铃声的时间
								hidingPlace.forget();//忘记隐藏位置
								entity.getBrain().refreshActivities(world.getTimeOfDay(), world.getTime());//刷新大脑活动
								mutableInt.setValue(0);//隐藏时间计数器清零
								return true;
							}
						})
		);
	}
}
