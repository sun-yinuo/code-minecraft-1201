package net.minecraft.item;

import java.util.List;
import net.minecraft.client.item.TooltipContext;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.enchantment.EnchantmentLevelEntry;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;
import net.minecraft.nbt.NbtList;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;

public class EnchantedBookItem extends Item {
	public static final String STORED_ENCHANTMENTS_KEY = "StoredEnchantments";

	public EnchantedBookItem(Settings settings) {
		super(settings);
	}

	@Override
	public boolean hasGlint(ItemStack stack) {
		return true;
	}

	@Override
	public boolean isEnchantable(ItemStack stack) {
		return false;
	}

	public static NbtList getEnchantmentNbt(ItemStack stack) {
		NbtCompound nbtCompound = stack.getNbt();
		return nbtCompound != null ? nbtCompound.getList("StoredEnchantments", NbtElement.COMPOUND_TYPE) : new NbtList();
	}

	@Override
	public void appendTooltip(ItemStack stack, @Nullable World world, List<Text> tooltip, TooltipContext context) {
		super.appendTooltip(stack, world, tooltip, context);
		ItemStack.appendEnchantments(tooltip, getEnchantmentNbt(stack));
	}

	public static void addEnchantment(ItemStack stack, EnchantmentLevelEntry entry) {
		NbtList nbtList = getEnchantmentNbt(stack);
		boolean bl = true;
		Identifier identifier = EnchantmentHelper.getEnchantmentId(entry.enchantment);

		for (int i = 0; i < nbtList.size(); i++) {
			NbtCompound nbtCompound = nbtList.getCompound(i);
			Identifier identifier2 = EnchantmentHelper.getIdFromNbt(nbtCompound);
			if (identifier2 != null && identifier2.equals(identifier)) {
				if (EnchantmentHelper.getLevelFromNbt(nbtCompound) < entry.level) {
					EnchantmentHelper.writeLevelToNbt(nbtCompound, entry.level);
				}

				bl = false;
				break;
			}
		}

		if (bl) {
			nbtList.add(EnchantmentHelper.createNbt(identifier, entry.level));
		}

		stack.getOrCreateNbt().put("StoredEnchantments", nbtList);
	}

	public static ItemStack forEnchantment(EnchantmentLevelEntry info) {
		ItemStack itemStack = new ItemStack(Items.ENCHANTED_BOOK);
		addEnchantment(itemStack, info);
		return itemStack;
	}
}
