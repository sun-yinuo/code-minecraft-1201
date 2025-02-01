package net.minecraft.entity;

import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvent;
import net.minecraft.sound.SoundEvents;
import org.jetbrains.annotations.Nullable;

/**
 * Represents an entity that can be saddled, either by a player or a
 * dispenser.
 */
public interface Saddleable {
	boolean canBeSaddled();

	void saddle(@Nullable SoundCategory sound);

	default SoundEvent getSaddleSound() {
		return SoundEvents.ENTITY_HORSE_SADDLE;
	}

	boolean isSaddled();
}
