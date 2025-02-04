package net.minecraft.entity.ai.brain.sensor;

import java.util.function.Supplier;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.passive.AxolotlBrain;
import net.minecraft.entity.passive.CamelBrain;
import net.minecraft.entity.passive.FrogBrain;
import net.minecraft.entity.passive.GoatBrain;
import net.minecraft.entity.passive.SnifferBrain;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.util.Identifier;

public class SensorType<U extends Sensor<?>> {
	//虚拟 占位
	public static final SensorType<DummySensor> DUMMY = register("dummy", DummySensor::new);
	//附近物品
	public static final SensorType<NearestItemsSensor> NEAREST_ITEMS = register("nearest_items", NearestItemsSensor::new);
	//附近生物
	public static final SensorType<NearestLivingEntitiesSensor<LivingEntity>> NEAREST_LIVING_ENTITIES = register(
		"nearest_living_entities", NearestLivingEntitiesSensor::new
	);
	//附近玩家
	public static final SensorType<NearestPlayersSensor> NEAREST_PLAYERS = register("nearest_players", NearestPlayersSensor::new);
	//最近床
	public static final SensorType<NearestBedSensor> NEAREST_BED = register("nearest_bed", NearestBedSensor::new);
	//攻击
	public static final SensorType<HurtBySensor> HURT_BY = register("hurt_by", HurtBySensor::new);
	//村民附近敌对生物
	public static final SensorType<VillagerHostilesSensor> VILLAGER_HOSTILES = register("villager_hostiles", VillagerHostilesSensor::new);
	//村民小孩
	public static final SensorType<VillagerBabiesSensor> VILLAGER_BABIES = register("villager_babies", VillagerBabiesSensor::new);
	//次要兴趣点
	public static final SensorType<SecondaryPointsOfInterestSensor> SECONDARY_POIS = register("secondary_pois", SecondaryPointsOfInterestSensor::new);
	//最近的铁傀儡
	public static final SensorType<GolemLastSeenSensor> GOLEM_DETECTED = register("golem_detected", GolemLastSeenSensor::new);
	//Piglin 相关的特定事件
	public static final SensorType<PiglinSpecificSensor> PIGLIN_SPECIFIC_SENSOR = register("piglin_specific_sensor", PiglinSpecificSensor::new);
	//Piglin Brute 猪灵蛮兵 相关的特定事件
	public static final SensorType<PiglinBruteSpecificSensor> PIGLIN_BRUTE_SPECIFIC_SENSOR = register(
		"piglin_brute_specific_sensor", PiglinBruteSpecificSensor::new
	);
	//Hoglin 疣猪兽 相关的特定事件
	public static final SensorType<HoglinSpecificSensor> HOGLIN_SPECIFIC_SENSOR = register("hoglin_specific_sensor", HoglinSpecificSensor::new);
	//最近可见的成年实体
	public static final SensorType<NearestVisibleAdultSensor> NEAREST_ADULT = register("nearest_adult", NearestVisibleAdultSensor::new);
	//Axolotl（美西螈）能够攻击的目标
	public static final SensorType<AxolotlAttackablesSensor> AXOLOTL_ATTACKABLES = register("axolotl_attackables", AxolotlAttackablesSensor::new);
	//Axolotl 被诱惑的对象
	public static final SensorType<TemptationsSensor> AXOLOTL_TEMPTATIONS = register(
		"axolotl_temptations", () -> new TemptationsSensor(AxolotlBrain.getTemptItems())
	);
	//Goat（山羊）被诱惑的对象
	public static final SensorType<TemptationsSensor> GOAT_TEMPTATIONS = register("goat_temptations", () -> new TemptationsSensor(GoatBrain.getTemptItems()));
	//Frog（青蛙）被诱惑的对象
	public static final SensorType<TemptationsSensor> FROG_TEMPTATIONS = register("frog_temptations", () -> new TemptationsSensor(FrogBrain.getTemptItems()));
	//Camel（骆驼）被诱惑的对象
	public static final SensorType<TemptationsSensor> CAMEL_TEMPTATIONS = register("camel_temptations", () -> new TemptationsSensor(CamelBrain.getTemptItems()));
	//Frog 可以攻击的目标
	public static final SensorType<FrogAttackablesSensor> FROG_ATTACKABLES = register("frog_attackables", FrogAttackablesSensor::new);
	//是否处于水中
	public static final SensorType<IsInWaterSensor> IS_IN_WATER = register("is_in_water", IsInWaterSensor::new);
	//Warden（坚守者）攻击目标
	public static final SensorType<WardenAttackablesSensor> WARDEN_ENTITY_SENSOR = register("warden_entity_sensor", WardenAttackablesSensor::new);
	//Sniffer（嗅探者）被诱惑的对象
	public static final SensorType<TemptationsSensor> SNIFFER_TEMPTATIONS = register(
		"sniffer_temptations", () -> new TemptationsSensor(SnifferBrain.getTemptItems())
	);
	private final Supplier<U> factory;

	public SensorType(Supplier<U> factory) {
		this.factory = factory;
	}

	public U create() {
		return (U)this.factory.get();
	}

	private static <U extends Sensor<?>> SensorType<U> register(String id, Supplier<U> factory) {
		return Registry.register(Registries.SENSOR_TYPE, new Identifier(id), new SensorType<>(factory));
	}
}
