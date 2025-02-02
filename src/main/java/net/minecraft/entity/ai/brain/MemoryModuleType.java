package net.minecraft.entity.ai.brain;

import com.google.common.annotations.VisibleForTesting;
import com.mojang.serialization.Codec;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import net.minecraft.entity.Entity;
import net.minecraft.entity.ItemEntity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.ai.pathing.Path;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.mob.AbstractPiglinEntity;
import net.minecraft.entity.mob.HoglinEntity;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.entity.passive.PassiveEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.util.Identifier;
import net.minecraft.util.Unit;
import net.minecraft.util.Uuids;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.GlobalPos;
import net.minecraft.util.math.Vec3d;

/**
 * A memory module type represents a type of data stored in a brain. The memory
 * data can be shared by different tasks once they are updated by a sensor or
 * created by some task. This can avoid some redundant calculations.
 * 
 * @see Brain
 * @see Memory
 */
public class MemoryModuleType<U> {
	//虚拟记忆 意义不明
	public static final MemoryModuleType<Void> DUMMY = register("dummy");
	//家的位置
	public static final MemoryModuleType<GlobalPos> HOME = register("home", GlobalPos.CODEC);
	//实体的工作站点
	public static final MemoryModuleType<GlobalPos> JOB_SITE = register("job_site", GlobalPos.CODEC);
	//可能的工作站
	public static final MemoryModuleType<GlobalPos> POTENTIAL_JOB_SITE = register("potential_job_site", GlobalPos.CODEC);
	//开会的地方
	public static final MemoryModuleType<GlobalPos> MEETING_POINT = register("meeting_point", GlobalPos.CODEC);
	//额外工作站点
	public static final MemoryModuleType<List<GlobalPos>> SECONDARY_JOB_SITE = register("secondary_job_site");
	//附近的敌对生物 /生物
	public static final MemoryModuleType<List<LivingEntity>> MOBS = register("mobs");
	//可见的生物体
	public static final MemoryModuleType<LivingTargetCache> VISIBLE_MOBS = register("visible_mobs");
	//附近可见的村民宝宝
	public static final MemoryModuleType<List<LivingEntity>> VISIBLE_VILLAGER_BABIES = register("visible_villager_babies");
	//附近的玩家
	public static final MemoryModuleType<List<PlayerEntity>> NEAREST_PLAYERS = register("nearest_players");
	//附近的可见玩家
	public static final MemoryModuleType<PlayerEntity> NEAREST_VISIBLE_PLAYER = register("nearest_visible_player");
    //最近的可见且可攻击的对象
	public static final MemoryModuleType<PlayerEntity> NEAREST_VISIBLE_TARGETABLE_PLAYER = register("nearest_visible_targetable_player");
	//需要行走到的目标
	public static final MemoryModuleType<WalkTarget> WALK_TARGET = register("walk_target");
	//需要👀的目标
	public static final MemoryModuleType<LookTarget> LOOK_TARGET = register("look_target");
	//当前的攻击目标
	public static final MemoryModuleType<LivingEntity> ATTACK_TARGET = register("attack_target");
	//是否处于冷却
	public static final MemoryModuleType<Boolean> ATTACK_COOLING_DOWN = register("attack_cooling_down");
	//当前的交互目标
	public static final MemoryModuleType<LivingEntity> INTERACTION_TARGET = register("interaction_target");
	//当前要繁殖的目标
	public static final MemoryModuleType<PassiveEntity> BREED_TARGET = register("breed_target");
	//当前骑乘的目标
	public static final MemoryModuleType<Entity> RIDE_TARGET = register("ride_target");
	//存储移动的路径
	public static final MemoryModuleType<Path> PATH = register("path");
	//存储可以打开的门
	public static final MemoryModuleType<List<GlobalPos>> INTERACTABLE_DOORS = register("interactable_doors");
	//存储经过后需要关门的门的位置
	public static final MemoryModuleType<Set<GlobalPos>> DOORS_TO_CLOSE = register("doors_to_close");
	//最近的床的位置
	public static final MemoryModuleType<BlockPos> NEAREST_BED = register("nearest_bed");
	//最近的伤害来源
	public static final MemoryModuleType<DamageSource> HURT_BY = register("hurt_by");
	//攻击的人
	public static final MemoryModuleType<LivingEntity> HURT_BY_ENTITY = register("hurt_by_entity");
	//需要逃跑的目标
	public static final MemoryModuleType<LivingEntity> AVOID_TARGET = register("avoid_target");
	//最近的敌对生物
	public static final MemoryModuleType<LivingEntity> NEAREST_HOSTILE = register("nearest_hostile");
	//最近的可攻击对象
	public static final MemoryModuleType<LivingEntity> NEAREST_ATTACKABLE = register("nearest_attackable");
	//躲避的地方
	public static final MemoryModuleType<GlobalPos> HIDING_PLACE = register("hiding_place");
	//听到钟声的时间
	public static final MemoryModuleType<Long> HEARD_BELL_TIME = register("heard_bell_time");
	//无法达到的目标
	public static final MemoryModuleType<Long> CANT_REACH_WALK_TARGET_SINCE = register("cant_reach_walk_target_since");
	//是否监测到铁傀儡
	public static final MemoryModuleType<Boolean> GOLEM_DETECTED_RECENTLY = register("golem_detected_recently", Codec.BOOL);
	//最近的睡觉时间
	public static final MemoryModuleType<Long> LAST_SLEPT = register("last_slept", Codec.LONG);
	//最近起床的时间
	public static final MemoryModuleType<Long> LAST_WOKEN = register("last_woken", Codec.LONG);
	//最近在工作方块工作的时间
	public static final MemoryModuleType<Long> LAST_WORKED_AT_POI = register("last_worked_at_poi", Codec.LONG);
	//最近可见的成年村民
	public static final MemoryModuleType<PassiveEntity> NEAREST_VISIBLE_ADULT = register("nearest_visible_adult");
	//最近的可见的想要的物品
	public static final MemoryModuleType<ItemEntity> NEAREST_VISIBLE_WANTED_ITEM = register("nearest_visible_wanted_item");
	//最近可见的克星
	public static final MemoryModuleType<MobEntity> NEAREST_VISIBLE_NEMESIS = register("nearest_visible_nemesis");
	//装死时间
	public static final MemoryModuleType<Integer> PLAY_DEAD_TICKS = register("play_dead_ticks", Codec.INT);
	//吸引实体的玩家
	public static final MemoryModuleType<PlayerEntity> TEMPTING_PLAYER = register("tempting_player");
	//收到诱惑都的冷却时间
	public static final MemoryModuleType<Integer> TEMPTATION_COOLDOWN_TICKS = register("temptation_cooldown_ticks", Codec.INT);
	//注视冷却时间
	public static final MemoryModuleType<Integer> GAZE_COOLDOWN_TICKS = register("gaze_cooldown_ticks", Codec.INT);
	//是否被诱惑
	public static final MemoryModuleType<Boolean> IS_TEMPTED = register("is_tempted", Codec.BOOL);
	//长跳冷却
	public static final MemoryModuleType<Integer> LONG_JUMP_COOLING_DOWN = register("long_jump_cooling_down", Codec.INT);
	//长跳进行中
	public static final MemoryModuleType<Boolean> LONG_JUMP_MID_JUMP = register("long_jump_mid_jump");
	//是否有狩猎冷却时间
	public static final MemoryModuleType<Boolean> HAS_HUNTING_COOLDOWN = register("has_hunting_cooldown", Codec.BOOL);
	//冲撞冷却时间
	public static final MemoryModuleType<Integer> RAM_COOLDOWN_TICKS = register("ram_cooldown_ticks", Codec.INT);
	//冲撞目标
	public static final MemoryModuleType<Vec3d> RAM_TARGET = register("ram_target");
	//是否在水中
	public static final MemoryModuleType<Unit> IS_IN_WATER = register("is_in_water", Codec.unit(Unit.INSTANCE));
	//是否怀孕
	public static final MemoryModuleType<Unit> IS_PREGNANT = register("is_pregnant", Codec.unit(Unit.INSTANCE));
	//是否处于恐慌状态
	public static final MemoryModuleType<Boolean> IS_PANICKING = register("is_panicking", Codec.BOOL);
	//无法用舌头攻击的目标列表
	public static final MemoryModuleType<List<UUID>> UNREACHABLE_TONGUE_TARGETS = register("unreachable_tongue_targets");
	//愤怒目标
	public static final MemoryModuleType<UUID> ANGRY_AT = register("angry_at", Uuids.INT_STREAM_CODEC);
	//是否处于普遍愤怒状态
	public static final MemoryModuleType<Boolean> UNIVERSAL_ANGER = register("universal_anger", Codec.BOOL);
	//是否正在欣赏物品
	public static final MemoryModuleType<Boolean> ADMIRING_ITEM = register("admiring_item", Codec.BOOL);
	//尝试接近欣赏物品的时间
	public static final MemoryModuleType<Integer> TIME_TRYING_TO_REACH_ADMIRE_ITEM = register("time_trying_to_reach_admire_item");
	//是否禁用走向欣赏物品的行为
	public static final MemoryModuleType<Boolean> DISABLE_WALK_TO_ADMIRE_ITEM = register("disable_walk_to_admire_item");
	//是否禁用欣赏物品
	public static final MemoryModuleType<Boolean> ADMIRING_DISABLED = register("admiring_disabled", Codec.BOOL);
	//是否最近被狩猎
	public static final MemoryModuleType<Boolean> HUNTED_RECENTLY = register("hunted_recently", Codec.BOOL);
	//庆祝位置
	public static final MemoryModuleType<BlockPos> CELEBRATE_LOCATION = register("celebrate_location");
	//是否正在跳舞
	public static final MemoryModuleType<Boolean> DANCING = register("dancing");
	//可见的可狩猎的猪灵兽
	public static final MemoryModuleType<HoglinEntity> NEAREST_VISIBLE_HUNTABLE_HOGLIN = register("nearest_visible_huntable_hoglin");
	//最近可见的幼年猪灵兽
	public static final MemoryModuleType<HoglinEntity> NEAREST_VISIBLE_BABY_HOGLIN = register("nearest_visible_baby_hoglin");
	//最近可见的未穿戴金制装备的目标玩家
	public static final MemoryModuleType<PlayerEntity> NEAREST_TARGETABLE_PLAYER_NOT_WEARING_GOLD = register("nearest_targetable_player_not_wearing_gold");
	//附近成年猪灵
	public static final MemoryModuleType<List<AbstractPiglinEntity>> NEARBY_ADULT_PIGLINS = register("nearby_adult_piglins");
	//最近可见的成年猪灵
	public static final MemoryModuleType<List<AbstractPiglinEntity>> NEAREST_VISIBLE_ADULT_PIGLINS = register("nearest_visible_adult_piglins");
	//最近可见的成年猪灵兽实体
	public static final MemoryModuleType<List<HoglinEntity>> NEAREST_VISIBLE_ADULT_HOGLINS = register("nearest_visible_adult_hoglins");
	//最近可见的成年猪灵实体
	public static final MemoryModuleType<AbstractPiglinEntity> NEAREST_VISIBLE_ADULT_PIGLIN = register("nearest_visible_adult_piglin");
	//最近可见的僵尸化实体
	public static final MemoryModuleType<LivingEntity> NEAREST_VISIBLE_ZOMBIFIED = register("nearest_visible_zombified");
	//可见的成年猪灵实体数量
	public static final MemoryModuleType<Integer> VISIBLE_ADULT_PIGLIN_COUNT = register("visible_adult_piglin_count");
	// 记录当前可见的成年疣猪兽数量
	public static final MemoryModuleType<Integer> VISIBLE_ADULT_HOGLIN_COUNT = register("visible_adult_hoglin_count");

	// 记录最近的、手持目标物品的玩家（如金锭对猪灵）
	public static final MemoryModuleType<PlayerEntity> NEAREST_PLAYER_HOLDING_WANTED_ITEM = register("nearest_player_holding_wanted_item");

	// 记录生物是否最近进食
	public static final MemoryModuleType<Boolean> ATE_RECENTLY = register("ate_recently");

	// 记录最近的驱避物品（如疣猪兽讨厌的疣猪兽驱避方块）
	public static final MemoryModuleType<BlockPos> NEAREST_REPELLENT = register("nearest_repellent");

	// 记录生物是否处于被驯服/被动状态
	public static final MemoryModuleType<Boolean> PACIFIED = register("pacified");

	// 记录当前吼叫（Roar）技能的目标
	public static final MemoryModuleType<LivingEntity> ROAR_TARGET = register("roar_target");

	// 记录最近的干扰事件发生位置（可能用于警觉 AI）
	public static final MemoryModuleType<BlockPos> DISTURBANCE_LOCATION = register("disturbance_location");

	// 记录是否最近有投射物（如箭或雪球）击中
	public static final MemoryModuleType<Unit> RECENT_PROJECTILE = register("recent_projectile", Codec.unit(Unit.INSTANCE));

	// 记录生物是否正在嗅探（如嗅探兽的行为）
	public static final MemoryModuleType<Unit> IS_SNIFFING = register("is_sniffing", Codec.unit(Unit.INSTANCE));

	// 记录生物是否正在从地下冒出（如嗅探兽从地下苏醒）
	public static final MemoryModuleType<Unit> IS_EMERGING = register("is_emerging", Codec.unit(Unit.INSTANCE));

	// 记录吼叫（Roar）声音播放的延迟
	public static final MemoryModuleType<Unit> ROAR_SOUND_DELAY = register("roar_sound_delay", Codec.unit(Unit.INSTANCE));

	// 记录挖掘冷却时间（如嗅探兽挖掘行为）
	public static final MemoryModuleType<Unit> DIG_COOLDOWN = register("dig_cooldown", Codec.unit(Unit.INSTANCE));

	// 记录吼叫（Roar）声音冷却时间，防止频繁吼叫
	public static final MemoryModuleType<Unit> ROAR_SOUND_COOLDOWN = register("roar_sound_cooldown", Codec.unit(Unit.INSTANCE));

	// 记录嗅探行为的冷却时间
	public static final MemoryModuleType<Unit> SNIFF_COOLDOWN = register("sniff_cooldown", Codec.unit(Unit.INSTANCE));

	// 记录触摸（Touch）行为的冷却时间
	public static final MemoryModuleType<Unit> TOUCH_COOLDOWN = register("touch_cooldown", Codec.unit(Unit.INSTANCE));

	// 记录震动感知的冷却时间（可能用于潜影怪或监守者）
	public static final MemoryModuleType<Unit> VIBRATION_COOLDOWN = register("vibration_cooldown", Codec.unit(Unit.INSTANCE));

	// 记录音波攻击（Sonic Boom）的冷却时间（适用于监守者）
	public static final MemoryModuleType<Unit> SONIC_BOOM_COOLDOWN = register("sonic_boom_cooldown", Codec.unit(Unit.INSTANCE));

	// 记录音波攻击（Sonic Boom）声音播放的冷却时间
	public static final MemoryModuleType<Unit> SONIC_BOOM_SOUND_COOLDOWN = register("sonic_boom_sound_cooldown", Codec.unit(Unit.INSTANCE));

	// 记录音波攻击（Sonic Boom）声音播放的延迟
	public static final MemoryModuleType<Unit> SONIC_BOOM_SOUND_DELAY = register("sonic_boom_sound_delay", Codec.unit(Unit.INSTANCE));

	// 记录生物“喜欢”的玩家（如嗅探兽可能记住一个特定的玩家）
	public static final MemoryModuleType<UUID> LIKED_PLAYER = register("liked_player", Uuids.INT_STREAM_CODEC);

	// 记录生物“喜欢”的音符盒位置（如悦灵对音符盒有特殊反应）
	public static final MemoryModuleType<GlobalPos> LIKED_NOTEBLOCK = register("liked_noteblock", GlobalPos.CODEC);

	// 记录音符盒的冷却时间，避免生物频繁反应
	public static final MemoryModuleType<Integer> LIKED_NOTEBLOCK_COOLDOWN_TICKS = register("liked_noteblock_cooldown_ticks", Codec.INT);

	// 记录物品拾取的冷却时间（如猪灵或拾荒者）
	public static final MemoryModuleType<Integer> ITEM_PICKUP_COOLDOWN_TICKS = register("item_pickup_cooldown_ticks", Codec.INT);

	// 记录嗅探兽已经探索过的位置，防止重复探索
	public static final MemoryModuleType<List<GlobalPos>> SNIFFER_EXPLORED_POSITIONS = register("sniffer_explored_positions", Codec.list(GlobalPos.CODEC));

	// 记录嗅探兽当前的嗅探目标位置
	public static final MemoryModuleType<BlockPos> SNIFFER_SNIFFING_TARGET = register("sniffer_sniffing_target");

	// 记录嗅探兽是否正在挖掘
	public static final MemoryModuleType<Boolean> SNIFFER_DIGGING = register("sniffer_digging");

	// 记录嗅探兽是否处于快乐状态（可能影响动画或行为）
	public static final MemoryModuleType<Boolean> SNIFFER_HAPPY = register("sniffer_happy");


	private final Optional<Codec<Memory<U>>> codec;

	@VisibleForTesting
	public MemoryModuleType(Optional<Codec<U>> codec) {
		this.codec = codec.map(Memory::createCodec);
	}

	public String toString() {
		return Registries.MEMORY_MODULE_TYPE.getId(this).toString();
	}

	public Optional<Codec<Memory<U>>> getCodec() {
		return this.codec;
	}

	private static <U> MemoryModuleType<U> register(String id, Codec<U> codec) {
		return Registry.register(Registries.MEMORY_MODULE_TYPE, new Identifier(id), new MemoryModuleType<>(Optional.of(codec)));
	}

	private static <U> MemoryModuleType<U> register(String id) {
		return Registry.register(Registries.MEMORY_MODULE_TYPE, new Identifier(id), new MemoryModuleType<>(Optional.empty()));
	}
}
