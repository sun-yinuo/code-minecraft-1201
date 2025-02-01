package net.minecraft.registry.tag;

import net.minecraft.entity.damage.DamageType;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.util.Identifier;

//参见https://zh.minecraft.wiki/w/%E4%BC%A4%E5%AE%B3%E7%B1%BB%E5%9E%8B
public interface DamageTypeTags {
	//损坏的头盔？
	TagKey<DamageType> DAMAGES_HELMET = of("damages_helmet");
	//绕过盔甲
	TagKey<DamageType> BYPASSES_ARMOR = of("bypasses_armor");
	//绕过屏障
	TagKey<DamageType> BYPASSES_SHIELD = of("bypasses_shield");
	//绕过无敌，kill命令，
	TagKey<DamageType> BYPASSES_INVULNERABILITY = of("bypasses_invulnerability");
	//🚁🚁🚁🚁🚁🚁🚁尊贵的马桶塞子正在巡视群聊🚁🚁🚁🚁🚁🚁🚁🚁🚁🚁
	//绕过冷却时间
	TagKey<DamageType> BYPASSES_COOLDOWN = of("bypasses_cooldown");
	//绕过效果
	TagKey<DamageType> BYPASSES_EFFECTS = of("bypasses_effects");
	//绕过阻力？
	TagKey<DamageType> BYPASSES_RESISTANCE = of("bypasses_resistance");
	//绕过魔法
	TagKey<DamageType> BYPASSES_ENCHANTMENTS = of("bypasses_enchantments");
	//火？
	TagKey<DamageType> IS_FIRE = of("is_fire");
	//弹射物伤害
	TagKey<DamageType> IS_PROJECTILE = of("is_projectile");
	//抵抗女巫的
	TagKey<DamageType> WITCH_RESISTANT_TO = of("witch_resistant_to");
	//爆炸
	TagKey<DamageType> IS_EXPLOSION = of("is_explosion");
	//摔落伤害
	TagKey<DamageType> IS_FALL = of("is_fall");
	TagKey<DamageType> IS_DROWNING = of("is_drowning");
	TagKey<DamageType> IS_FREEZING = of("is_freezing");
	TagKey<DamageType> IS_LIGHTNING = of("is_lightning");
	TagKey<DamageType> NO_ANGER = of("no_anger");
	TagKey<DamageType> NO_IMPACT = of("no_impact");
	TagKey<DamageType> ALWAYS_MOST_SIGNIFICANT_FALL = of("always_most_significant_fall");
	TagKey<DamageType> WITHER_IMMUNE_TO = of("wither_immune_to");
	TagKey<DamageType> IGNITES_ARMOR_STANDS = of("ignites_armor_stands");
	TagKey<DamageType> BURNS_ARMOR_STANDS = of("burns_armor_stands");
	TagKey<DamageType> AVOIDS_GUARDIAN_THORNS = of("avoids_guardian_thorns");
	TagKey<DamageType> ALWAYS_TRIGGERS_SILVERFISH = of("always_triggers_silverfish");
	TagKey<DamageType> ALWAYS_HURTS_ENDER_DRAGONS = of("always_hurts_ender_dragons");

	private static TagKey<DamageType> of(String id) {
		return TagKey.of(RegistryKeys.DAMAGE_TYPE, new Identifier(id));
	}
}
