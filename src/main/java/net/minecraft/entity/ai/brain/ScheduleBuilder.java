package net.minecraft.entity.ai.brain;

import com.google.common.collect.Lists;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

//builder
public class ScheduleBuilder {


	private final Schedule schedule;
	private final List<ActivityEntry> activities = Lists.<ActivityEntry>newArrayList();

	/**
	 * 构造
	 * @param schedule ScheduleBuilder
	 */
	public ScheduleBuilder(Schedule schedule) {
		this.schedule = schedule;
	}


	public ScheduleBuilder withActivity(int startTime, Activity activity) {
		this.activities.add(new ActivityEntry(startTime, activity));
		return this;
	}

	/**
	 * build
	 * @return Schedule
	 */
	public Schedule build() {
		((Set)this.activities.stream().map(ActivityEntry::getActivity).collect(Collectors.toSet())).forEach(this.schedule::addActivity);
		this.activities.forEach(activity -> {
			Activity activity2 = activity.getActivity();
			this.schedule.getOtherRules(activity2).forEach(rule -> rule.add(activity.getStartTime(), 0.0F));
			this.schedule.getRule(activity2).add(activity.getStartTime(), 1.0F);
		});
		return this.schedule;
	}

	//内部类
	static class ActivityEntry {
		//开始时间
		private final int startTime;
		//活动
		private final Activity activity;

		public ActivityEntry(int startTime, Activity activity) {
			this.startTime = startTime;
			this.activity = activity;
		}

		public int getStartTime() {
			return this.startTime;
		}

		public Activity getActivity() {
			return this.activity;
		}
	}
}
