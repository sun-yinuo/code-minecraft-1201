package net.minecraft.data.server.recipe;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Map.Entry;
import java.util.function.Consumer;
import net.minecraft.advancement.Advancement;
import net.minecraft.advancement.AdvancementRewards;
import net.minecraft.advancement.CriterionMerger;
import net.minecraft.advancement.criterion.CriterionConditions;
import net.minecraft.advancement.criterion.RecipeUnlockedCriterion;
import net.minecraft.item.Item;
import net.minecraft.item.ItemConvertible;
import net.minecraft.recipe.Ingredient;
import net.minecraft.recipe.RecipeSerializer;
import net.minecraft.recipe.book.CraftingRecipeCategory;
import net.minecraft.recipe.book.RecipeCategory;
import net.minecraft.registry.Registries;
import net.minecraft.registry.tag.TagKey;
import net.minecraft.util.Identifier;
import org.jetbrains.annotations.Nullable;

public class ShapedRecipeJsonBuilder extends RecipeJsonBuilder implements CraftingRecipeJsonBuilder {
	private final RecipeCategory category;
	private final Item output;
	private final int count;
	private final List<String> pattern = Lists.<String>newArrayList();
	private final Map<Character, Ingredient> inputs = Maps.<Character, Ingredient>newLinkedHashMap();
	private final Advancement.Builder advancementBuilder = Advancement.Builder.createUntelemetered();
	@Nullable
	private String group;
	private boolean showNotification = true;

	public ShapedRecipeJsonBuilder(RecipeCategory category, ItemConvertible output, int count) {
		this.category = category;
		this.output = output.asItem();
		this.count = count;
	}

	public static ShapedRecipeJsonBuilder create(RecipeCategory category, ItemConvertible output) {
		return create(category, output, 1);
	}

	public static ShapedRecipeJsonBuilder create(RecipeCategory category, ItemConvertible output, int count) {
		return new ShapedRecipeJsonBuilder(category, output, count);
	}

	public ShapedRecipeJsonBuilder input(Character c, TagKey<Item> tag) {
		return this.input(c, Ingredient.fromTag(tag));
	}

	public ShapedRecipeJsonBuilder input(Character c, ItemConvertible itemProvider) {
		return this.input(c, Ingredient.ofItems(itemProvider));
	}

	public ShapedRecipeJsonBuilder input(Character c, Ingredient ingredient) {
		if (this.inputs.containsKey(c)) {
			throw new IllegalArgumentException("Symbol '" + c + "' is already defined!");
		} else if (c == ' ') {
			throw new IllegalArgumentException("Symbol ' ' (whitespace) is reserved and cannot be defined");
		} else {
			this.inputs.put(c, ingredient);
			return this;
		}
	}

	public ShapedRecipeJsonBuilder pattern(String patternStr) {
		if (!this.pattern.isEmpty() && patternStr.length() != ((String)this.pattern.get(0)).length()) {
			throw new IllegalArgumentException("Pattern must be the same width on every line!");
		} else {
			this.pattern.add(patternStr);
			return this;
		}
	}

	public ShapedRecipeJsonBuilder criterion(String string, CriterionConditions criterionConditions) {
		this.advancementBuilder.criterion(string, criterionConditions);
		return this;
	}

	public ShapedRecipeJsonBuilder group(@Nullable String string) {
		this.group = string;
		return this;
	}

	public ShapedRecipeJsonBuilder showNotification(boolean showNotification) {
		this.showNotification = showNotification;
		return this;
	}

	@Override
	public Item getOutputItem() {
		return this.output;
	}

	@Override
	public void offerTo(Consumer<RecipeJsonProvider> exporter, Identifier recipeId) {
		this.validate(recipeId);
		this.advancementBuilder
			.parent(ROOT)
			.criterion("has_the_recipe", RecipeUnlockedCriterion.create(recipeId))
			.rewards(AdvancementRewards.Builder.recipe(recipeId))
			.criteriaMerger(CriterionMerger.OR);
		exporter.accept(
			new ShapedRecipeJsonProvider(
				recipeId,
				this.output,
				this.count,
				this.group == null ? "" : this.group,
				getCraftingCategory(this.category),
				this.pattern,
				this.inputs,
				this.advancementBuilder,
				recipeId.withPrefixedPath("recipes/" + this.category.getName() + "/"),
				this.showNotification
			)
		);
	}

	private void validate(Identifier recipeId) {
		if (this.pattern.isEmpty()) {
			throw new IllegalStateException("No pattern is defined for shaped recipe " + recipeId + "!");
		} else {
			Set<Character> set = Sets.<Character>newHashSet(this.inputs.keySet());
			set.remove(' ');

			for (String string : this.pattern) {
				for (int i = 0; i < string.length(); i++) {
					char c = string.charAt(i);
					if (!this.inputs.containsKey(c) && c != ' ') {
						throw new IllegalStateException("Pattern in recipe " + recipeId + " uses undefined symbol '" + c + "'");
					}

					set.remove(c);
				}
			}

			if (!set.isEmpty()) {
				throw new IllegalStateException("Ingredients are defined but not used in pattern for recipe " + recipeId);
			} else if (this.pattern.size() == 1 && ((String)this.pattern.get(0)).length() == 1) {
				throw new IllegalStateException("Shaped recipe " + recipeId + " only takes in a single item - should it be a shapeless recipe instead?");
			} else if (this.advancementBuilder.getCriteria().isEmpty()) {
				throw new IllegalStateException("No way of obtaining recipe " + recipeId);
			}
		}
	}

	static class ShapedRecipeJsonProvider extends CraftingRecipeJsonProvider {
		private final Identifier recipeId;
		private final Item output;
		private final int resultCount;
		private final String group;
		private final List<String> pattern;
		private final Map<Character, Ingredient> inputs;
		private final Advancement.Builder advancementBuilder;
		private final Identifier advancementId;
		private final boolean showNotification;

		public ShapedRecipeJsonProvider(
			Identifier recipeId,
			Item output,
			int resultCount,
			String group,
			CraftingRecipeCategory craftingCategory,
			List<String> pattern,
			Map<Character, Ingredient> inputs,
			Advancement.Builder advancementBuilder,
			Identifier advancementId,
			boolean showNotification
		) {
			super(craftingCategory);
			this.recipeId = recipeId;
			this.output = output;
			this.resultCount = resultCount;
			this.group = group;
			this.pattern = pattern;
			this.inputs = inputs;
			this.advancementBuilder = advancementBuilder;
			this.advancementId = advancementId;
			this.showNotification = showNotification;
		}

		@Override
		public void serialize(JsonObject json) {
			super.serialize(json);
			if (!this.group.isEmpty()) {
				json.addProperty("group", this.group);
			}

			JsonArray jsonArray = new JsonArray();

			for (String string : this.pattern) {
				jsonArray.add(string);
			}

			json.add("pattern", jsonArray);
			JsonObject jsonObject = new JsonObject();

			for (Entry<Character, Ingredient> entry : this.inputs.entrySet()) {
				jsonObject.add(String.valueOf(entry.getKey()), ((Ingredient)entry.getValue()).toJson());
			}

			json.add("key", jsonObject);
			JsonObject jsonObject2 = new JsonObject();
			jsonObject2.addProperty("item", Registries.ITEM.getId(this.output).toString());
			if (this.resultCount > 1) {
				jsonObject2.addProperty("count", this.resultCount);
			}

			json.add("result", jsonObject2);
			json.addProperty("show_notification", this.showNotification);
		}

		@Override
		public RecipeSerializer<?> getSerializer() {
			return RecipeSerializer.SHAPED;
		}

		@Override
		public Identifier getRecipeId() {
			return this.recipeId;
		}

		@Nullable
		@Override
		public JsonObject toAdvancementJson() {
			return this.advancementBuilder.toJson();
		}

		@Nullable
		@Override
		public Identifier getAdvancementId() {
			return this.advancementId;
		}
	}
}
