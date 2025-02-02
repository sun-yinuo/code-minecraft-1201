package net.minecraft.entity.ai.brain;

import com.google.common.collect.Maps;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.stream.Collectors;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;

//事务基类
public class Schedule {
	public static final int WORK_TIME = 2000;
	public static final int field_30693 = 7000;

	//闲置状态
	public static final Schedule EMPTY = register("empty").withActivity(0, Activity.IDLE).build();

	public static final Schedule SIMPLE = register("simple").withActivity(5000, Activity.WORK).withActivity(11000, Activity.REST).build();

	//村民宝宝一天的生活
	public static final Schedule VILLAGER_BABY = register("villager_baby")
		.withActivity(10, Activity.IDLE)
		.withActivity(3000, Activity.PLAY)
		.withActivity(6000, Activity.IDLE)
		.withActivity(10000, Activity.PLAY)
		.withActivity(12000, Activity.REST)
		.build();
	//正常村民的生活
	public static final Schedule VILLAGER_DEFAULT = register("villager_default")
		.withActivity(10, Activity.IDLE)
		.withActivity(2000, Activity.WORK)
		.withActivity(9000, Activity.MEET)
		.withActivity(11000, Activity.IDLE)
		.withActivity(12000, Activity.REST)
		.build();
	//日程规则
	private final Map<Activity, ScheduleRule> scheduleRules = Maps.<Activity, ScheduleRule>newHashMap();


	protected static ScheduleBuilder register(String id) {
		Schedule schedule = Registry.register(Registries.SCHEDULE, id, new Schedule());
		return new ScheduleBuilder(schedule);
	}

	protected void addActivity(Activity activity) {
		if (!this.scheduleRules.containsKey(activity)) {
			this.scheduleRules.put(activity, new ScheduleRule());
		}
	}

	protected ScheduleRule getRule(Activity activity) {
		return (ScheduleRule)this.scheduleRules.get(activity);
	}

	protected List<ScheduleRule> getOtherRules(Activity activity) {
		return (List<ScheduleRule>)this.scheduleRules.entrySet().stream().filter(rule -> rule.getKey() != activity).map(Entry::getValue).collect(Collectors.toList());
	}


	/**
	 * 根据时间time 从scheduleRules中获取最高优先级活动并返回
	 * @param time
	 * @return
	 */
	public Activity getActivityForTime(int time) {
		return (Activity)this.scheduleRules
			.entrySet()
			.stream()
			.max(Comparator.comparingDouble(rule -> (double)((ScheduleRule)rule.getValue()).getPriority(time)))
			.map(Entry::getKey)
			//找不到，返回IDLE
			.orElse(Activity.IDLE);
	}
}
