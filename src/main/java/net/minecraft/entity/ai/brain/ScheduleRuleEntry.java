package net.minecraft.entity.ai.brain;

//时间调度规则
public class ScheduleRuleEntry {
	//开始时间
	private final int startTime;
	//优先级
	private final float priority;

	public ScheduleRuleEntry(int startTime, float priority) {
		this.startTime = startTime;
		this.priority = priority;
	}

	public int getStartTime() {
		return this.startTime;
	}

	public float getPriority() {
		return this.priority;
	}
}
