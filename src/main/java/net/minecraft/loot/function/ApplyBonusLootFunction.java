package net.minecraft.loot.function;

import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Maps;
import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonSerializationContext;
import java.util.Map;
import java.util.Set;
import net.minecraft.enchantment.Enchantment;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.item.ItemStack;
import net.minecraft.loot.condition.LootCondition;
import net.minecraft.loot.context.LootContext;
import net.minecraft.loot.context.LootContextParameter;
import net.minecraft.loot.context.LootContextParameters;
import net.minecraft.registry.Registries;
import net.minecraft.util.Identifier;
import net.minecraft.util.JsonHelper;
import net.minecraft.util.math.random.Random;

public class ApplyBonusLootFunction extends ConditionalLootFunction {
	static final Map<Identifier, FormulaFactory> FACTORIES = Maps.<Identifier, FormulaFactory>newHashMap();
	final Enchantment enchantment;
	final Formula formula;

	ApplyBonusLootFunction(LootCondition[] conditions, Enchantment enchantment, Formula formula) {
		super(conditions);
		this.enchantment = enchantment;
		this.formula = formula;
	}

	@Override
	public LootFunctionType getType() {
		return LootFunctionTypes.APPLY_BONUS;
	}

	@Override
	public Set<LootContextParameter<?>> getRequiredParameters() {
		return ImmutableSet.of(LootContextParameters.TOOL);
	}

	@Override
	public ItemStack process(ItemStack stack, LootContext context) {
		ItemStack itemStack = context.get(LootContextParameters.TOOL);
		if (itemStack != null) {
			int i = EnchantmentHelper.getLevel(this.enchantment, itemStack);
			int j = this.formula.getValue(context.getRandom(), stack.getCount(), i);
			stack.setCount(j);
		}

		return stack;
	}

	public static Builder<?> binomialWithBonusCount(Enchantment enchantment, float probability, int extra) {
		return builder(conditions -> new ApplyBonusLootFunction(conditions, enchantment, new BinomialWithBonusCount(extra, probability)));
	}

	public static Builder<?> oreDrops(Enchantment enchantment) {
		return builder(conditions -> new ApplyBonusLootFunction(conditions, enchantment, new OreDrops()));
	}

	public static Builder<?> uniformBonusCount(Enchantment enchantment) {
		return builder(conditions -> new ApplyBonusLootFunction(conditions, enchantment, new UniformBonusCount(1)));
	}

	public static Builder<?> uniformBonusCount(Enchantment enchantment, int bonusMultiplier) {
		return builder(conditions -> new ApplyBonusLootFunction(conditions, enchantment, new UniformBonusCount(bonusMultiplier)));
	}

	static {
		FACTORIES.put(BinomialWithBonusCount.ID, BinomialWithBonusCount::fromJson);
		FACTORIES.put(OreDrops.ID, OreDrops::fromJson);
		FACTORIES.put(UniformBonusCount.ID, UniformBonusCount::fromJson);
	}

	static final class BinomialWithBonusCount implements Formula {
		public static final Identifier ID = new Identifier("binomial_with_bonus_count");
		private final int extra;
		private final float probability;

		public BinomialWithBonusCount(int extra, float probability) {
			this.extra = extra;
			this.probability = probability;
		}

		@Override
		public int getValue(Random random, int initialCount, int enchantmentLevel) {
			for (int i = 0; i < enchantmentLevel + this.extra; i++) {
				if (random.nextFloat() < this.probability) {
					initialCount++;
				}
			}

			return initialCount;
		}

		@Override
		public void toJson(JsonObject json, JsonSerializationContext context) {
			json.addProperty("extra", this.extra);
			json.addProperty("probability", this.probability);
		}

		public static Formula fromJson(JsonObject json, JsonDeserializationContext context) {
			int i = JsonHelper.getInt(json, "extra");
			float f = JsonHelper.getFloat(json, "probability");
			return new BinomialWithBonusCount(i, f);
		}

		@Override
		public Identifier getId() {
			return ID;
		}
	}

	interface Formula {
		int getValue(Random random, int initialCount, int enchantmentLevel);

		void toJson(JsonObject json, JsonSerializationContext context);

		Identifier getId();
	}

	interface FormulaFactory {
		Formula deserialize(JsonObject functionJson, JsonDeserializationContext context);
	}

	static final class OreDrops implements Formula {
		public static final Identifier ID = new Identifier("ore_drops");

		@Override
		public int getValue(Random random, int initialCount, int enchantmentLevel) {
			if (enchantmentLevel > 0) {
				int i = random.nextInt(enchantmentLevel + 2) - 1;
				if (i < 0) {
					i = 0;
				}

				return initialCount * (i + 1);
			} else {
				return initialCount;
			}
		}

		@Override
		public void toJson(JsonObject json, JsonSerializationContext context) {
		}

		public static Formula fromJson(JsonObject json, JsonDeserializationContext context) {
			return new OreDrops();
		}

		@Override
		public Identifier getId() {
			return ID;
		}
	}

	public static class Serializer extends ConditionalLootFunction.Serializer<ApplyBonusLootFunction> {
		public void toJson(JsonObject jsonObject, ApplyBonusLootFunction applyBonusLootFunction, JsonSerializationContext jsonSerializationContext) {
			super.toJson(jsonObject, applyBonusLootFunction, jsonSerializationContext);
			jsonObject.addProperty("enchantment", Registries.ENCHANTMENT.getId(applyBonusLootFunction.enchantment).toString());
			jsonObject.addProperty("formula", applyBonusLootFunction.formula.getId().toString());
			JsonObject jsonObject2 = new JsonObject();
			applyBonusLootFunction.formula.toJson(jsonObject2, jsonSerializationContext);
			if (jsonObject2.size() > 0) {
				jsonObject.add("parameters", jsonObject2);
			}
		}

		public ApplyBonusLootFunction fromJson(JsonObject jsonObject, JsonDeserializationContext jsonDeserializationContext, LootCondition[] lootConditions) {
			Identifier identifier = new Identifier(JsonHelper.getString(jsonObject, "enchantment"));
			Enchantment enchantment = (Enchantment)Registries.ENCHANTMENT
				.getOrEmpty(identifier)
				.orElseThrow(() -> new JsonParseException("Invalid enchantment id: " + identifier));
			Identifier identifier2 = new Identifier(JsonHelper.getString(jsonObject, "formula"));
			FormulaFactory formulaFactory = (FormulaFactory)ApplyBonusLootFunction.FACTORIES.get(identifier2);
			if (formulaFactory == null) {
				throw new JsonParseException("Invalid formula id: " + identifier2);
			} else {
				Formula formula;
				if (jsonObject.has("parameters")) {
					formula = formulaFactory.deserialize(JsonHelper.getObject(jsonObject, "parameters"), jsonDeserializationContext);
				} else {
					formula = formulaFactory.deserialize(new JsonObject(), jsonDeserializationContext);
				}

				return new ApplyBonusLootFunction(lootConditions, enchantment, formula);
			}
		}
	}

	static final class UniformBonusCount implements Formula {
		public static final Identifier ID = new Identifier("uniform_bonus_count");
		private final int bonusMultiplier;

		public UniformBonusCount(int bonusMultiplier) {
			this.bonusMultiplier = bonusMultiplier;
		}

		@Override
		public int getValue(Random random, int initialCount, int enchantmentLevel) {
			return initialCount + random.nextInt(this.bonusMultiplier * enchantmentLevel + 1);
		}

		@Override
		public void toJson(JsonObject json, JsonSerializationContext context) {
			json.addProperty("bonusMultiplier", this.bonusMultiplier);
		}

		public static Formula fromJson(JsonObject json, JsonDeserializationContext context) {
			int i = JsonHelper.getInt(json, "bonusMultiplier");
			return new UniformBonusCount(i);
		}

		@Override
		public Identifier getId() {
			return ID;
		}
	}
}
