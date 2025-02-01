package net.minecraft.entity.ai.brain;

import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;

//生物活动定义
public class Activity {
	// 核心活动，通常是生物的基础行为
	public static final Activity CORE = register("core");

	// 闲置状态，生物无特定活动时的默认行为
	public static final Activity IDLE = register("idle");

	// 工作状态，通常用于村民等需要工作的生物
	public static final Activity WORK = register("work");

	// 玩耍状态，某些生物可能会进入该状态进行互动或娱乐
	public static final Activity PLAY = register("play");

	// 休息状态，例如村民夜间睡觉或其他生物恢复精力
	public static final Activity REST = register("rest");

	// 进行社交活动，如村民开会、动物群体交流等
	public static final Activity MEET = register("meet");

	// 恐慌状态，当生物感到威胁或受到攻击时触发
	public static final Activity PANIC = register("panic");

	// 袭击状态，通常用于袭击或攻击某些目标（如掠夺者袭击村庄）
	public static final Activity RAID = register("raid");

	// 袭击前的准备状态，生物可能会聚集、待命等
	public static final Activity PRE_RAID = register("pre_raid");

	// 隐藏状态，通常用于避免危险的情况下
	public static final Activity HIDE = register("hide");

	// 战斗状态，生物进入战斗模式，与敌对生物交战
	public static final Activity FIGHT = register("fight");

	// 庆祝状态，例如袭击成功后村民的庆祝行为
	public static final Activity CELEBRATE = register("celebrate");

	// 观察物品，例如村民或猪灵欣赏某个物品
	public static final Activity ADMIRE_ITEM = register("admire_item");

	// 规避行为，通常用于生物躲避危险
	public static final Activity AVOID = register("avoid");

	// 骑乘状态，例如骑马、骑猪等
	public static final Activity RIDE = register("ride");

	// 装死状态，例如负鼠或恼鬼模拟死亡
	public static final Activity PLAY_DEAD = register("play_dead");

	// 长跳状态，如跃行动物或某些怪物的特殊跳跃行为
	public static final Activity LONG_JUMP = register("long_jump");

	// 冲撞状态，例如山羊冲撞其他实体
	public static final Activity RAM = register("ram");

	// 舌头攻击，例如青蛙捕食小型生物时使用
	public static final Activity TONGUE = register("tongue");

	// 游泳状态，水生生物或其他能在水中活动的生物使用
	public static final Activity SWIM = register("swim");

	// 产卵状态，例如青蛙或海龟等生物在特定环境下产卵
	public static final Activity LAY_SPAWN = register("lay_spawn");

	// 嗅探状态，例如骷髅狼或其他嗅探生物的探测行为
	public static final Activity SNIFF = register("sniff");

	// 调查状态，生物发现新事物或可疑情况时的行为
	public static final Activity INVESTIGATE = register("investigate");

	// 怒吼状态，例如凋灵、蛮族猪灵等生物的咆哮
	public static final Activity ROAR = register("roar");

	// 从地下或其他隐藏状态中出现，例如伏击怪物或幼年螨虫
	public static final Activity EMERGE = register("emerge");

	// 挖掘状态，例如守卫者或掠夺者破坏方块
	public static final Activity DIG = register("dig");
	private final String id;
	private final int hashCode;


	public Activity(String id) {
		this.id = id;
		this.hashCode = id.hashCode();
	}

	public String getId() {
		return this.id;
	}

	private static Activity register(String id) {
		return Registry.register(Registries.ACTIVITY, id, new Activity(id));
	}

	public boolean equals(Object o) {
		if (this == o) {
			return true;
		} else if (o != null && this.getClass() == o.getClass()) {
			Activity activity = (Activity)o;
			return this.id.equals(activity.id);
		} else {
			return false;
		}
	}

	public int hashCode() {
		return this.hashCode;
	}

	public String toString() {
		return this.getId();
	}
}
