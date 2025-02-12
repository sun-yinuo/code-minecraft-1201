package net.minecraft.advancement.criterion;

import com.google.gson.JsonObject;
import com.google.gson.JsonSyntaxException;
import net.minecraft.potion.Potion;
import net.minecraft.predicate.entity.AdvancementEntityPredicateDeserializer;
import net.minecraft.predicate.entity.AdvancementEntityPredicateSerializer;
import net.minecraft.predicate.entity.LootContextPredicate;
import net.minecraft.registry.Registries;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.Identifier;
import net.minecraft.util.JsonHelper;
import org.jetbrains.annotations.Nullable;

public class BrewedPotionCriterion extends AbstractCriterion<BrewedPotionCriterion.Conditions> {
	static final Identifier ID = new Identifier("brewed_potion");

	@Override
	public Identifier getId() {
		return ID;
	}

	public BrewedPotionCriterion.Conditions conditionsFromJson(
		JsonObject jsonObject, LootContextPredicate lootContextPredicate, AdvancementEntityPredicateDeserializer advancementEntityPredicateDeserializer
	) {
		Potion potion = null;
		if (jsonObject.has("potion")) {
			Identifier identifier = new Identifier(JsonHelper.getString(jsonObject, "potion"));
			potion = (Potion)Registries.POTION.getOrEmpty(identifier).orElseThrow(() -> new JsonSyntaxException("Unknown potion '" + identifier + "'"));
		}

		return new BrewedPotionCriterion.Conditions(lootContextPredicate, potion);
	}

	public void trigger(ServerPlayerEntity player, Potion potion) {
		this.trigger(player, conditions -> conditions.matches(potion));
	}

	public static class Conditions extends AbstractCriterionConditions {
		@Nullable
		private final Potion potion;

		public Conditions(LootContextPredicate player, @Nullable Potion potion) {
			super(BrewedPotionCriterion.ID, player);
			this.potion = potion;
		}

		public static BrewedPotionCriterion.Conditions any() {
			return new BrewedPotionCriterion.Conditions(LootContextPredicate.EMPTY, null);
		}

		public boolean matches(Potion potion) {
			return this.potion == null || this.potion == potion;
		}

		@Override
		public JsonObject toJson(AdvancementEntityPredicateSerializer predicateSerializer) {
			JsonObject jsonObject = super.toJson(predicateSerializer);
			if (this.potion != null) {
				jsonObject.addProperty("potion", Registries.POTION.getId(this.potion).toString());
			}

			return jsonObject;
		}
	}
}
