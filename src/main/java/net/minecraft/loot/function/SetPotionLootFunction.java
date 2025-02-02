package net.minecraft.loot.function;

import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonObject;
import com.google.gson.JsonSerializationContext;
import com.google.gson.JsonSyntaxException;
import net.minecraft.item.ItemStack;
import net.minecraft.loot.condition.LootCondition;
import net.minecraft.loot.context.LootContext;
import net.minecraft.potion.Potion;
import net.minecraft.potion.PotionUtil;
import net.minecraft.registry.Registries;
import net.minecraft.util.Identifier;
import net.minecraft.util.JsonHelper;

public class SetPotionLootFunction extends ConditionalLootFunction {
	final Potion potion;

	SetPotionLootFunction(LootCondition[] conditions, Potion potion) {
		super(conditions);
		this.potion = potion;
	}

	@Override
	public LootFunctionType getType() {
		return LootFunctionTypes.SET_POTION;
	}

	@Override
	public ItemStack process(ItemStack stack, LootContext context) {
		PotionUtil.setPotion(stack, this.potion);
		return stack;
	}

	public static Builder<?> builder(Potion potion) {
		return builder(conditions -> new SetPotionLootFunction(conditions, potion));
	}

	public static class Serializer extends ConditionalLootFunction.Serializer<SetPotionLootFunction> {
		public void toJson(JsonObject jsonObject, SetPotionLootFunction setPotionLootFunction, JsonSerializationContext jsonSerializationContext) {
			super.toJson(jsonObject, setPotionLootFunction, jsonSerializationContext);
			jsonObject.addProperty("id", Registries.POTION.getId(setPotionLootFunction.potion).toString());
		}

		public SetPotionLootFunction fromJson(JsonObject jsonObject, JsonDeserializationContext jsonDeserializationContext, LootCondition[] lootConditions) {
			String string = JsonHelper.getString(jsonObject, "id");
			Potion potion = (Potion)Registries.POTION
				.getOrEmpty(Identifier.tryParse(string))
				.orElseThrow(() -> new JsonSyntaxException("Unknown potion '" + string + "'"));
			return new SetPotionLootFunction(lootConditions, potion);
		}
	}
}
