package net.minecraft.loot.function;

import com.google.common.collect.ImmutableSet;
import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonObject;
import com.google.gson.JsonSerializationContext;
import com.mojang.logging.LogUtils;
import java.util.Locale;
import java.util.Set;
import net.minecraft.item.FilledMapItem;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.item.map.MapIcon;
import net.minecraft.item.map.MapState;
import net.minecraft.loot.condition.LootCondition;
import net.minecraft.loot.context.LootContext;
import net.minecraft.loot.context.LootContextParameter;
import net.minecraft.loot.context.LootContextParameters;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.registry.tag.StructureTags;
import net.minecraft.registry.tag.TagKey;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.Identifier;
import net.minecraft.util.JsonHelper;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.gen.structure.Structure;
import org.slf4j.Logger;

public class ExplorationMapLootFunction extends ConditionalLootFunction {
	static final Logger LOGGER = LogUtils.getLogger();
	public static final TagKey<Structure> DEFAULT_DESTINATION = StructureTags.ON_TREASURE_MAPS;
	public static final String MANSION = "mansion";
	public static final MapIcon.Type DEFAULT_DECORATION = MapIcon.Type.MANSION;
	public static final byte field_31851 = 2;
	public static final int field_31852 = 50;
	public static final boolean field_31853 = true;
	final TagKey<Structure> destination;
	final MapIcon.Type decoration;
	final byte zoom;
	final int searchRadius;
	final boolean skipExistingChunks;

	ExplorationMapLootFunction(
		LootCondition[] conditions, TagKey<Structure> destination, MapIcon.Type decoration, byte zoom, int searchRadius, boolean skipExistingChunks
	) {
		super(conditions);
		this.destination = destination;
		this.decoration = decoration;
		this.zoom = zoom;
		this.searchRadius = searchRadius;
		this.skipExistingChunks = skipExistingChunks;
	}

	@Override
	public LootFunctionType getType() {
		return LootFunctionTypes.EXPLORATION_MAP;
	}

	@Override
	public Set<LootContextParameter<?>> getRequiredParameters() {
		return ImmutableSet.of(LootContextParameters.ORIGIN);
	}

	@Override
	public ItemStack process(ItemStack stack, LootContext context) {
		if (!stack.isOf(Items.MAP)) {
			return stack;
		} else {
			Vec3d vec3d = context.get(LootContextParameters.ORIGIN);
			if (vec3d != null) {
				ServerWorld serverWorld = context.getWorld();
				BlockPos blockPos = serverWorld.locateStructure(this.destination, BlockPos.ofFloored(vec3d), this.searchRadius, this.skipExistingChunks);
				if (blockPos != null) {
					ItemStack itemStack = FilledMapItem.createMap(serverWorld, blockPos.getX(), blockPos.getZ(), this.zoom, true, true);
					FilledMapItem.fillExplorationMap(serverWorld, itemStack);
					MapState.addDecorationsNbt(itemStack, blockPos, "+", this.decoration);
					return itemStack;
				}
			}

			return stack;
		}
	}

	public static Builder builder() {
		return new Builder();
	}

	public static class Builder extends ConditionalLootFunction.Builder<Builder> {
		private TagKey<Structure> destination = ExplorationMapLootFunction.DEFAULT_DESTINATION;
		private MapIcon.Type decoration = ExplorationMapLootFunction.DEFAULT_DECORATION;
		private byte zoom = 2;
		private int searchRadius = 50;
		private boolean skipExistingChunks = true;

		protected Builder getThisBuilder() {
			return this;
		}

		public Builder withDestination(TagKey<Structure> destination) {
			this.destination = destination;
			return this;
		}

		public Builder withDecoration(MapIcon.Type decoration) {
			this.decoration = decoration;
			return this;
		}

		public Builder withZoom(byte zoom) {
			this.zoom = zoom;
			return this;
		}

		public Builder searchRadius(int searchRadius) {
			this.searchRadius = searchRadius;
			return this;
		}

		public Builder withSkipExistingChunks(boolean skipExistingChunks) {
			this.skipExistingChunks = skipExistingChunks;
			return this;
		}

		@Override
		public LootFunction build() {
			return new ExplorationMapLootFunction(this.getConditions(), this.destination, this.decoration, this.zoom, this.searchRadius, this.skipExistingChunks);
		}
	}

	public static class Serializer extends ConditionalLootFunction.Serializer<ExplorationMapLootFunction> {
		public void toJson(JsonObject jsonObject, ExplorationMapLootFunction explorationMapLootFunction, JsonSerializationContext jsonSerializationContext) {
			super.toJson(jsonObject, explorationMapLootFunction, jsonSerializationContext);
			if (!explorationMapLootFunction.destination.equals(ExplorationMapLootFunction.DEFAULT_DESTINATION)) {
				jsonObject.addProperty("destination", explorationMapLootFunction.destination.id().toString());
			}

			if (explorationMapLootFunction.decoration != ExplorationMapLootFunction.DEFAULT_DECORATION) {
				jsonObject.add("decoration", jsonSerializationContext.serialize(explorationMapLootFunction.decoration.toString().toLowerCase(Locale.ROOT)));
			}

			if (explorationMapLootFunction.zoom != 2) {
				jsonObject.addProperty("zoom", explorationMapLootFunction.zoom);
			}

			if (explorationMapLootFunction.searchRadius != 50) {
				jsonObject.addProperty("search_radius", explorationMapLootFunction.searchRadius);
			}

			if (!explorationMapLootFunction.skipExistingChunks) {
				jsonObject.addProperty("skip_existing_chunks", explorationMapLootFunction.skipExistingChunks);
			}
		}

		public ExplorationMapLootFunction fromJson(JsonObject jsonObject, JsonDeserializationContext jsonDeserializationContext, LootCondition[] lootConditions) {
			TagKey<Structure> tagKey = getDestination(jsonObject);
			String string = jsonObject.has("decoration") ? JsonHelper.getString(jsonObject, "decoration") : "mansion";
			MapIcon.Type type = ExplorationMapLootFunction.DEFAULT_DECORATION;

			try {
				type = MapIcon.Type.valueOf(string.toUpperCase(Locale.ROOT));
			} catch (IllegalArgumentException var10) {
				ExplorationMapLootFunction.LOGGER
					.error("Error while parsing loot table decoration entry. Found {}. Defaulting to {}", string, ExplorationMapLootFunction.DEFAULT_DECORATION);
			}

			byte b = JsonHelper.getByte(jsonObject, "zoom", (byte)2);
			int i = JsonHelper.getInt(jsonObject, "search_radius", 50);
			boolean bl = JsonHelper.getBoolean(jsonObject, "skip_existing_chunks", true);
			return new ExplorationMapLootFunction(lootConditions, tagKey, type, b, i, bl);
		}

		private static TagKey<Structure> getDestination(JsonObject json) {
			if (json.has("destination")) {
				String string = JsonHelper.getString(json, "destination");
				return TagKey.of(RegistryKeys.STRUCTURE, new Identifier(string));
			} else {
				return ExplorationMapLootFunction.DEFAULT_DESTINATION;
			}
		}
	}
}
