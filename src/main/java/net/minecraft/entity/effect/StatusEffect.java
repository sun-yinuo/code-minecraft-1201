package net.minecraft.entity.effect;

import com.google.common.collect.Maps;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.Map.Entry;
import java.util.function.Supplier;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.attribute.AttributeContainer;
import net.minecraft.entity.attribute.EntityAttribute;
import net.minecraft.entity.attribute.EntityAttributeInstance;
import net.minecraft.entity.attribute.EntityAttributeModifier;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.registry.Registries;
import net.minecraft.text.Text;
import net.minecraft.util.Util;
import org.jetbrains.annotations.Nullable;

public class StatusEffect {
	private final Map<EntityAttribute, EntityAttributeModifier> attributeModifiers = Maps.<EntityAttribute, EntityAttributeModifier>newHashMap();
	private final StatusEffectCategory category;
	private final int color;
	@Nullable
	private String translationKey;
	private Supplier<StatusEffectInstance.FactorCalculationData> factorCalculationDataSupplier = () -> null;

	@Nullable
	public static StatusEffect byRawId(int rawId) {
		return Registries.STATUS_EFFECT.get(rawId);
	}

	public static int getRawId(StatusEffect type) {
		return Registries.STATUS_EFFECT.getRawId(type);
	}

	public static int getRawIdNullable(@Nullable StatusEffect type) {
		return Registries.STATUS_EFFECT.getRawId(type);
	}

	protected StatusEffect(StatusEffectCategory category, int color) {
		this.category = category;
		this.color = color;
	}

	public Optional<StatusEffectInstance.FactorCalculationData> getFactorCalculationDataSupplier() {
		return Optional.ofNullable((StatusEffectInstance.FactorCalculationData)this.factorCalculationDataSupplier.get());
	}

	public void applyUpdateEffect(LivingEntity entity, int amplifier) {
		if (this == StatusEffects.REGENERATION) {
			if (entity.getHealth() < entity.getMaxHealth()) {
				entity.heal(1.0F);
			}
		} else if (this == StatusEffects.POISON) {
			if (entity.getHealth() > 1.0F) {
				entity.damage(entity.getDamageSources().magic(), 1.0F);
			}
		} else if (this == StatusEffects.WITHER) {
			entity.damage(entity.getDamageSources().wither(), 1.0F);
		} else if (this == StatusEffects.HUNGER && entity instanceof PlayerEntity) {
			((PlayerEntity)entity).addExhaustion(0.005F * (float)(amplifier + 1));
		} else if (this == StatusEffects.SATURATION && entity instanceof PlayerEntity) {
			if (!entity.getWorld().isClient) {
				((PlayerEntity)entity).getHungerManager().add(amplifier + 1, 1.0F);
			}
		} else if ((this != StatusEffects.INSTANT_HEALTH || entity.isUndead()) && (this != StatusEffects.INSTANT_DAMAGE || !entity.isUndead())) {
			if (this == StatusEffects.INSTANT_DAMAGE && !entity.isUndead() || this == StatusEffects.INSTANT_HEALTH && entity.isUndead()) {
				entity.damage(entity.getDamageSources().magic(), (float)(6 << amplifier));
			}
		} else {
			entity.heal((float)Math.max(4 << amplifier, 0));
		}
	}

	public void applyInstantEffect(@Nullable Entity source, @Nullable Entity attacker, LivingEntity target, int amplifier, double proximity) {
		if ((this != StatusEffects.INSTANT_HEALTH || target.isUndead()) && (this != StatusEffects.INSTANT_DAMAGE || !target.isUndead())) {
			if (this == StatusEffects.INSTANT_DAMAGE && !target.isUndead() || this == StatusEffects.INSTANT_HEALTH && target.isUndead()) {
				int i = (int)(proximity * (double)(6 << amplifier) + 0.5);
				if (source == null) {
					target.damage(target.getDamageSources().magic(), (float)i);
				} else {
					target.damage(target.getDamageSources().indirectMagic(source, attacker), (float)i);
				}
			} else {
				this.applyUpdateEffect(target, amplifier);
			}
		} else {
			int i = (int)(proximity * (double)(4 << amplifier) + 0.5);
			target.heal((float)i);
		}
	}

	public boolean canApplyUpdateEffect(int duration, int amplifier) {
		if (this == StatusEffects.REGENERATION) {
			int i = 50 >> amplifier;
			return i > 0 ? duration % i == 0 : true;
		} else if (this == StatusEffects.POISON) {
			int i = 25 >> amplifier;
			return i > 0 ? duration % i == 0 : true;
		} else if (this == StatusEffects.WITHER) {
			int i = 40 >> amplifier;
			return i > 0 ? duration % i == 0 : true;
		} else {
			return this == StatusEffects.HUNGER;
		}
	}

	public boolean isInstant() {
		return false;
	}

	protected String loadTranslationKey() {
		if (this.translationKey == null) {
			this.translationKey = Util.createTranslationKey("effect", Registries.STATUS_EFFECT.getId(this));
		}

		return this.translationKey;
	}

	public String getTranslationKey() {
		return this.loadTranslationKey();
	}

	public Text getName() {
		return Text.translatable(this.getTranslationKey());
	}

	public StatusEffectCategory getCategory() {
		return this.category;
	}

	public int getColor() {
		return this.color;
	}

	public StatusEffect addAttributeModifier(EntityAttribute attribute, String uuid, double amount, EntityAttributeModifier.Operation operation) {
		EntityAttributeModifier entityAttributeModifier = new EntityAttributeModifier(UUID.fromString(uuid), this::getTranslationKey, amount, operation);
		this.attributeModifiers.put(attribute, entityAttributeModifier);
		return this;
	}

	public StatusEffect setFactorCalculationDataSupplier(Supplier<StatusEffectInstance.FactorCalculationData> factorCalculationDataSupplier) {
		this.factorCalculationDataSupplier = factorCalculationDataSupplier;
		return this;
	}

	public Map<EntityAttribute, EntityAttributeModifier> getAttributeModifiers() {
		return this.attributeModifiers;
	}

	public void onRemoved(LivingEntity entity, AttributeContainer attributes, int amplifier) {
		for (Entry<EntityAttribute, EntityAttributeModifier> entry : this.attributeModifiers.entrySet()) {
			EntityAttributeInstance entityAttributeInstance = attributes.getCustomInstance((EntityAttribute)entry.getKey());
			if (entityAttributeInstance != null) {
				entityAttributeInstance.removeModifier((EntityAttributeModifier)entry.getValue());
			}
		}
	}

	public void onApplied(LivingEntity entity, AttributeContainer attributes, int amplifier) {
		for (Entry<EntityAttribute, EntityAttributeModifier> entry : this.attributeModifiers.entrySet()) {
			EntityAttributeInstance entityAttributeInstance = attributes.getCustomInstance((EntityAttribute)entry.getKey());
			if (entityAttributeInstance != null) {
				EntityAttributeModifier entityAttributeModifier = (EntityAttributeModifier)entry.getValue();
				entityAttributeInstance.removeModifier(entityAttributeModifier);
				entityAttributeInstance.addPersistentModifier(
					new EntityAttributeModifier(
						entityAttributeModifier.getId(),
						this.getTranslationKey() + " " + amplifier,
						this.adjustModifierAmount(amplifier, entityAttributeModifier),
						entityAttributeModifier.getOperation()
					)
				);
			}
		}
	}

	public double adjustModifierAmount(int amplifier, EntityAttributeModifier modifier) {
		return modifier.getValue() * (double)(amplifier + 1);
	}

	public boolean isBeneficial() {
		return this.category == StatusEffectCategory.BENEFICIAL;
	}
}
