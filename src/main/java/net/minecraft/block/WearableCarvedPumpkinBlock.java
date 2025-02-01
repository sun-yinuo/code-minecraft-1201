package net.minecraft.block;

import net.minecraft.entity.EquipmentSlot;
import net.minecraft.item.Equipment;

public class WearableCarvedPumpkinBlock extends CarvedPumpkinBlock implements Equipment {
	public WearableCarvedPumpkinBlock(Settings settings) {
		super(settings);
	}

	@Override
	public EquipmentSlot getSlotType() {
		return EquipmentSlot.HEAD;
	}
}
