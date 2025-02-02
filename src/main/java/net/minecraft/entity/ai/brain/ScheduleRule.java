package net.minecraft.entity.ai.brain;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import it.unimi.dsi.fastutil.ints.Int2ObjectAVLTreeMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectSortedMap;
import java.util.Collection;
import java.util.List;

//调度管理
public class ScheduleRule {
	//所有调度
	private final List<ScheduleRuleEntry> entries = Lists.<ScheduleRuleEntry>newArrayList();
	//记录 当前最优先的调度规则索引
	private int prioritizedEntryIndex;


	public ImmutableList<ScheduleRuleEntry> getEntries() {
		return ImmutableList.copyOf(this.entries);
	}

	/**
	 * 添加调度规则
	 * @param startTime 开始时间
	 * @param priority 优先级
	 * @return ScheduleRule
	 */
	public ScheduleRule add(int startTime, float priority) {
		this.entries.add(new ScheduleRuleEntry(startTime, priority));
		//排序
		this.sort();
		return this;
	}

	/**
	 * 批量添加
	 * @param entries 调度集合
	 * @return ScheduleRule
	 */
	public ScheduleRule add(Collection<ScheduleRuleEntry> entries) {
		this.entries.addAll(entries);
		this.sort();
		return this;
	}

	/**
	 * 排序，按时间
	 */
	private void sort() {
		Int2ObjectSortedMap<ScheduleRuleEntry> int2ObjectSortedMap = new Int2ObjectAVLTreeMap<>();
		this.entries.forEach(scheduleRuleEntry -> int2ObjectSortedMap.put(scheduleRuleEntry.getStartTime(), scheduleRuleEntry));
		this.entries.clear();
		this.entries.addAll(int2ObjectSortedMap.values());
		this.prioritizedEntryIndex = 0;
	}

	/**
	 * 获取当前时间的优先级
	 * @param time 时间
	 * @return float
	 */
	public float getPriority(int time) {
		if (this.entries.size() <= 0) {
			//entries为空直接返回没有优先级
			return 0.0F;
		} else {

			//
			ScheduleRuleEntry scheduleRuleEntry = (ScheduleRuleEntry)this.entries.get(this.prioritizedEntryIndex);
			ScheduleRuleEntry scheduleRuleEntry2 = (ScheduleRuleEntry)this.entries.get(this.entries.size() - 1);


			boolean bl = time < scheduleRuleEntry.getStartTime();
			//true 0
			//false this.prioritizedEntryIndex
			int i = bl ? 0 : this.prioritizedEntryIndex;
			//true scheduleRuleEntry2.getPriority()
			//false scheduleRuleEntry.getPriority()
			float f = bl ? scheduleRuleEntry2.getPriority() : scheduleRuleEntry.getPriority();

			for (int j = i; j < this.entries.size(); j++) {
				ScheduleRuleEntry scheduleRuleEntry3 = (ScheduleRuleEntry)this.entries.get(j);
				if (scheduleRuleEntry3.getStartTime() > time) {
					break;
				}

				this.prioritizedEntryIndex = j;
				f = scheduleRuleEntry3.getPriority();
			}

			return f;
		}
	}
}
