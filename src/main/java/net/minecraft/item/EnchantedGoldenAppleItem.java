package net.minecraft.item;

public class EnchantedGoldenAppleItem extends Item {
	public EnchantedGoldenAppleItem(Settings settings) {
		super(settings);
	}

	@Override
	public boolean hasGlint(ItemStack stack) {
		return true;
	}
}
