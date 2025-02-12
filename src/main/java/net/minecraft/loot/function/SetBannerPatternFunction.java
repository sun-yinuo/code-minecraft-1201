package net.minecraft.loot.function;

import com.google.common.collect.ImmutableList;
import com.google.gson.JsonArray;
import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonObject;
import com.google.gson.JsonSerializationContext;
import com.google.gson.JsonSyntaxException;
import com.mojang.datafixers.util.Pair;
import java.util.List;
import java.util.Optional;
import net.minecraft.block.entity.BannerPattern;
import net.minecraft.block.entity.BlockEntityType;
import net.minecraft.item.BlockItem;
import net.minecraft.item.ItemStack;
import net.minecraft.loot.condition.LootCondition;
import net.minecraft.loot.context.LootContext;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;
import net.minecraft.nbt.NbtList;
import net.minecraft.registry.Registries;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.util.DyeColor;
import net.minecraft.util.Identifier;
import net.minecraft.util.JsonHelper;

public class SetBannerPatternFunction extends ConditionalLootFunction {
	final List<Pair<RegistryEntry<BannerPattern>, DyeColor>> patterns;
	final boolean append;

	SetBannerPatternFunction(LootCondition[] conditions, List<Pair<RegistryEntry<BannerPattern>, DyeColor>> patterns, boolean append) {
		super(conditions);
		this.patterns = patterns;
		this.append = append;
	}

	@Override
	protected ItemStack process(ItemStack stack, LootContext context) {
		NbtCompound nbtCompound = BlockItem.getBlockEntityNbt(stack);
		if (nbtCompound == null) {
			nbtCompound = new NbtCompound();
		}

		BannerPattern.Patterns patterns = new BannerPattern.Patterns();
		this.patterns.forEach(patterns::add);
		NbtList nbtList = patterns.toNbt();
		NbtList nbtList2;
		if (this.append) {
			nbtList2 = nbtCompound.getList("Patterns", NbtElement.COMPOUND_TYPE).copy();
			nbtList2.addAll(nbtList);
		} else {
			nbtList2 = nbtList;
		}

		nbtCompound.put("Patterns", nbtList2);
		BlockItem.setBlockEntityNbt(stack, BlockEntityType.BANNER, nbtCompound);
		return stack;
	}

	@Override
	public LootFunctionType getType() {
		return LootFunctionTypes.SET_BANNER_PATTERN;
	}

	public static Builder builder(boolean append) {
		return new Builder(append);
	}

	public static class Builder extends ConditionalLootFunction.Builder<Builder> {
		private final ImmutableList.Builder<Pair<RegistryEntry<BannerPattern>, DyeColor>> patterns = ImmutableList.builder();
		private final boolean append;

		Builder(boolean append) {
			this.append = append;
		}

		protected Builder getThisBuilder() {
			return this;
		}

		@Override
		public LootFunction build() {
			return new SetBannerPatternFunction(this.getConditions(), this.patterns.build(), this.append);
		}

		public Builder pattern(RegistryKey<BannerPattern> pattern, DyeColor color) {
			return this.pattern(Registries.BANNER_PATTERN.entryOf(pattern), color);
		}

		public Builder pattern(RegistryEntry<BannerPattern> pattern, DyeColor color) {
			this.patterns.add(Pair.of(pattern, color));
			return this;
		}
	}

	public static class Serializer extends ConditionalLootFunction.Serializer<SetBannerPatternFunction> {
		public void toJson(JsonObject jsonObject, SetBannerPatternFunction setBannerPatternFunction, JsonSerializationContext jsonSerializationContext) {
			super.toJson(jsonObject, setBannerPatternFunction, jsonSerializationContext);
			JsonArray jsonArray = new JsonArray();
			setBannerPatternFunction.patterns
				.forEach(
					pair -> {
						JsonObject jsonObjectx = new JsonObject();
						jsonObjectx.addProperty(
							"pattern",
							((RegistryKey)((RegistryEntry)pair.getFirst()).getKey().orElseThrow(() -> new JsonSyntaxException("Unknown pattern: " + pair.getFirst())))
								.getValue()
								.toString()
						);
						jsonObjectx.addProperty("color", ((DyeColor)pair.getSecond()).getName());
						jsonArray.add(jsonObjectx);
					}
				);
			jsonObject.add("patterns", jsonArray);
			jsonObject.addProperty("append", setBannerPatternFunction.append);
		}

		public SetBannerPatternFunction fromJson(JsonObject jsonObject, JsonDeserializationContext jsonDeserializationContext, LootCondition[] lootConditions) {
			ImmutableList.Builder<Pair<RegistryEntry<BannerPattern>, DyeColor>> builder = ImmutableList.builder();
			JsonArray jsonArray = JsonHelper.getArray(jsonObject, "patterns");

			for (int i = 0; i < jsonArray.size(); i++) {
				JsonObject jsonObject2 = JsonHelper.asObject(jsonArray.get(i), "pattern[" + i + "]");
				String string = JsonHelper.getString(jsonObject2, "pattern");
				Optional<? extends RegistryEntry<BannerPattern>> optional = Registries.BANNER_PATTERN
					.getEntry(RegistryKey.of(RegistryKeys.BANNER_PATTERN, new Identifier(string)));
				if (optional.isEmpty()) {
					throw new JsonSyntaxException("Unknown pattern: " + string);
				}

				String string2 = JsonHelper.getString(jsonObject2, "color");
				DyeColor dyeColor = DyeColor.byName(string2, null);
				if (dyeColor == null) {
					throw new JsonSyntaxException("Unknown color: " + string2);
				}

				builder.add(Pair.of((RegistryEntry<BannerPattern>)optional.get(), dyeColor));
			}

			boolean bl = JsonHelper.getBoolean(jsonObject, "append");
			return new SetBannerPatternFunction(lootConditions, builder.build(), bl);
		}
	}
}
