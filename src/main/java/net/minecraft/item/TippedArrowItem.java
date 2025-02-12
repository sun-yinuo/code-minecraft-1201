package net.minecraft.item;

import java.util.List;
import net.minecraft.client.item.TooltipContext;
import net.minecraft.potion.PotionUtil;
import net.minecraft.potion.Potions;
import net.minecraft.text.Text;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;

public class TippedArrowItem extends ArrowItem {
	public TippedArrowItem(Settings settings) {
		super(settings);
	}

	@Override
	public ItemStack getDefaultStack() {
		return PotionUtil.setPotion(super.getDefaultStack(), Potions.POISON);
	}

	@Override
	public void appendTooltip(ItemStack stack, @Nullable World world, List<Text> tooltip, TooltipContext context) {
		PotionUtil.buildTooltip(stack, tooltip, 0.125F);
	}

	@Override
	public String getTranslationKey(ItemStack stack) {
		return PotionUtil.getPotion(stack).finishTranslationKey(this.getTranslationKey() + ".effect.");
	}
}
