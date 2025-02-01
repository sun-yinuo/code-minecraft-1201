package net.minecraft.loot.condition;

public class AnyOfLootCondition extends AlternativeLootCondition {
	AnyOfLootCondition(LootCondition[] terms) {
		super(terms, LootConditionTypes.matchingAny(terms));
	}

	@Override
	public LootConditionType getType() {
		return LootConditionTypes.ANY_OF;
	}

	public static Builder builder(LootCondition.Builder... terms) {
		return new Builder(terms);
	}

	public static class Builder extends AlternativeLootCondition.Builder {
		public Builder(LootCondition.Builder... builders) {
			super(builders);
		}

		@Override
		public Builder or(LootCondition.Builder builder) {
			this.add(builder);
			return this;
		}

		@Override
		protected LootCondition build(LootCondition[] terms) {
			return new AnyOfLootCondition(terms);
		}
	}

	public static class Serializer extends AlternativeLootCondition.Serializer<AnyOfLootCondition> {
		protected AnyOfLootCondition fromTerms(LootCondition[] lootConditions) {
			return new AnyOfLootCondition(lootConditions);
		}
	}
}
