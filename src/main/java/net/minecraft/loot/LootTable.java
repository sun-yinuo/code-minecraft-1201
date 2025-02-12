package net.minecraft.loot;

import com.google.common.collect.Lists;
import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonSerializationContext;
import com.google.gson.JsonSerializer;
import com.mojang.logging.LogUtils;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import java.lang.reflect.Type;
import java.util.Iterator;
import java.util.List;
import java.util.function.BiFunction;
import java.util.function.Consumer;
import net.fabricmc.fabric.api.loot.v2.FabricLootTableBuilder;
import net.minecraft.inventory.Inventory;
import net.minecraft.item.ItemStack;
import net.minecraft.loot.context.LootContext;
import net.minecraft.loot.context.LootContextParameterSet;
import net.minecraft.loot.context.LootContextType;
import net.minecraft.loot.context.LootContextTypes;
import net.minecraft.loot.function.LootFunction;
import net.minecraft.loot.function.LootFunctionConsumingBuilder;
import net.minecraft.loot.function.LootFunctionTypes;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.Identifier;
import net.minecraft.util.JsonHelper;
import net.minecraft.util.Util;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.random.Random;
import org.apache.commons.lang3.ArrayUtils;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;

public class LootTable {
	static final Logger LOGGER = LogUtils.getLogger();
	public static final LootTable EMPTY = new LootTable(LootContextTypes.EMPTY, null, new LootPool[0], new LootFunction[0]);
	public static final LootContextType GENERIC = LootContextTypes.GENERIC;
	final LootContextType type;
	@Nullable
	final Identifier randomSequenceId;
	public final LootPool[] pools;
	public final LootFunction[] functions;
	private final BiFunction<ItemStack, LootContext, ItemStack> combinedFunction;

	LootTable(LootContextType type, @Nullable Identifier randomSequenceId, LootPool[] pools, LootFunction[] functions) {
		this.type = type;
		this.randomSequenceId = randomSequenceId;
		this.pools = pools;
		this.functions = functions;
		this.combinedFunction = LootFunctionTypes.join(functions);
	}

	public static Consumer<ItemStack> processStacks(ServerWorld world, Consumer<ItemStack> consumer) {
		return stack -> {
			if (stack.isItemEnabled(world.getEnabledFeatures())) {
				if (stack.getCount() < stack.getMaxCount()) {
					consumer.accept(stack);
				} else {
					int i = stack.getCount();

					while (i > 0) {
						ItemStack itemStack = stack.copyWithCount(Math.min(stack.getMaxCount(), i));
						i -= itemStack.getCount();
						consumer.accept(itemStack);
					}
				}
			}
		};
	}

	public void generateUnprocessedLoot(LootContextParameterSet parameters, Consumer<ItemStack> lootConsumer) {
		this.generateUnprocessedLoot(new LootContext.Builder(parameters).build(this.randomSequenceId), lootConsumer);
	}

	public void generateUnprocessedLoot(LootContext context, Consumer<ItemStack> lootConsumer) {
		LootContext.Entry<?> entry = LootContext.table(this);
		if (context.markActive(entry)) {
			Consumer<ItemStack> consumer = LootFunction.apply(this.combinedFunction, lootConsumer, context);

			for (LootPool lootPool : this.pools) {
				lootPool.addGeneratedLoot(consumer, context);
			}

			context.markInactive(entry);
		} else {
			LOGGER.warn("Detected infinite loop in loot tables");
		}
	}

	public void generateLoot(LootContextParameterSet parameters, long seed, Consumer<ItemStack> lootConsumer) {
		this.generateUnprocessedLoot(
			new LootContext.Builder(parameters).random(seed).build(this.randomSequenceId), processStacks(parameters.getWorld(), lootConsumer)
		);
	}

	public void generateLoot(LootContextParameterSet parameters, Consumer<ItemStack> lootConsumer) {
		this.generateUnprocessedLoot(parameters, processStacks(parameters.getWorld(), lootConsumer));
	}

	public void generateLoot(LootContext context, Consumer<ItemStack> lootConsumer) {
		this.generateUnprocessedLoot(context, processStacks(context.getWorld(), lootConsumer));
	}

	public ObjectArrayList<ItemStack> generateLoot(LootContextParameterSet parameters, long seed) {
		return this.generateLoot(new LootContext.Builder(parameters).random(seed).build(this.randomSequenceId));
	}

	public ObjectArrayList<ItemStack> generateLoot(LootContextParameterSet parameters) {
		return this.generateLoot(new LootContext.Builder(parameters).build(this.randomSequenceId));
	}

	private ObjectArrayList<ItemStack> generateLoot(LootContext context) {
		ObjectArrayList<ItemStack> objectArrayList = new ObjectArrayList<>();
		this.generateLoot(context, objectArrayList::add);
		return objectArrayList;
	}

	public LootContextType getType() {
		return this.type;
	}

	public void validate(LootTableReporter reporter) {
		for (int i = 0; i < this.pools.length; i++) {
			this.pools[i].validate(reporter.makeChild(".pools[" + i + "]"));
		}

		for (int i = 0; i < this.functions.length; i++) {
			this.functions[i].validate(reporter.makeChild(".functions[" + i + "]"));
		}
	}

	public void supplyInventory(Inventory inventory, LootContextParameterSet parameters, long seed) {
		LootContext lootContext = new LootContext.Builder(parameters).random(seed).build(this.randomSequenceId);
		ObjectArrayList<ItemStack> objectArrayList = this.generateLoot(lootContext);
		Random random = lootContext.getRandom();
		List<Integer> list = this.getFreeSlots(inventory, random);
		this.shuffle(objectArrayList, list.size(), random);

		for (ItemStack itemStack : objectArrayList) {
			if (list.isEmpty()) {
				LOGGER.warn("Tried to over-fill a container");
				return;
			}

			if (itemStack.isEmpty()) {
				inventory.setStack((Integer)list.remove(list.size() - 1), ItemStack.EMPTY);
			} else {
				inventory.setStack((Integer)list.remove(list.size() - 1), itemStack);
			}
		}
	}

	private void shuffle(ObjectArrayList<ItemStack> drops, int freeSlots, Random random) {
		List<ItemStack> list = Lists.<ItemStack>newArrayList();
		Iterator<ItemStack> iterator = drops.iterator();

		while (iterator.hasNext()) {
			ItemStack itemStack = (ItemStack)iterator.next();
			if (itemStack.isEmpty()) {
				iterator.remove();
			} else if (itemStack.getCount() > 1) {
				list.add(itemStack);
				iterator.remove();
			}
		}

		while (freeSlots - drops.size() - list.size() > 0 && !list.isEmpty()) {
			ItemStack itemStack2 = (ItemStack)list.remove(MathHelper.nextInt(random, 0, list.size() - 1));
			int i = MathHelper.nextInt(random, 1, itemStack2.getCount() / 2);
			ItemStack itemStack3 = itemStack2.split(i);
			if (itemStack2.getCount() > 1 && random.nextBoolean()) {
				list.add(itemStack2);
			} else {
				drops.add(itemStack2);
			}

			if (itemStack3.getCount() > 1 && random.nextBoolean()) {
				list.add(itemStack3);
			} else {
				drops.add(itemStack3);
			}
		}

		drops.addAll(list);
		Util.shuffle(drops, random);
	}

	private List<Integer> getFreeSlots(Inventory inventory, Random random) {
		ObjectArrayList<Integer> objectArrayList = new ObjectArrayList<>();

		for (int i = 0; i < inventory.size(); i++) {
			if (inventory.getStack(i).isEmpty()) {
				objectArrayList.add(i);
			}
		}

		Util.shuffle(objectArrayList, random);
		return objectArrayList;
	}

	public static Builder builder() {
		return new Builder();
	}

	public static class Builder implements LootFunctionConsumingBuilder<Builder>, FabricLootTableBuilder {
		private final List<LootPool> pools = Lists.<LootPool>newArrayList();
		private final List<LootFunction> functions = Lists.<LootFunction>newArrayList();
		private LootContextType type = LootTable.GENERIC;
		@Nullable
		private Identifier randomSequenceId = null;

		public Builder pool(LootPool.Builder poolBuilder) {
			this.pools.add(poolBuilder.build());
			return this;
		}

		public Builder type(LootContextType context) {
			this.type = context;
			return this;
		}

		public Builder randomSequenceId(Identifier randomSequenceId) {
			this.randomSequenceId = randomSequenceId;
			return this;
		}

		public Builder apply(LootFunction.Builder builder) {
			this.functions.add(builder.build());
			return this;
		}

		public Builder getThisFunctionConsumingBuilder() {
			return this;
		}

		public LootTable build() {
			return new LootTable(
				this.type, this.randomSequenceId, (LootPool[])this.pools.toArray(new LootPool[0]), (LootFunction[])this.functions.toArray(new LootFunction[0])
			);
		}
	}

	public static class Serializer implements JsonDeserializer<LootTable>, JsonSerializer<LootTable> {
		public LootTable deserialize(JsonElement jsonElement, Type type, JsonDeserializationContext jsonDeserializationContext) throws JsonParseException {
			JsonObject jsonObject = JsonHelper.asObject(jsonElement, "loot table");
			LootPool[] lootPools = JsonHelper.deserialize(jsonObject, "pools", new LootPool[0], jsonDeserializationContext, LootPool[].class);
			LootContextType lootContextType = null;
			if (jsonObject.has("type")) {
				String string = JsonHelper.getString(jsonObject, "type");
				lootContextType = LootContextTypes.get(new Identifier(string));
			}

			Identifier identifier;
			if (jsonObject.has("random_sequence")) {
				String string2 = JsonHelper.getString(jsonObject, "random_sequence");
				identifier = new Identifier(string2);
			} else {
				identifier = null;
			}

			LootFunction[] lootFunctions = JsonHelper.deserialize(jsonObject, "functions", new LootFunction[0], jsonDeserializationContext, LootFunction[].class);
			return new LootTable(lootContextType != null ? lootContextType : LootContextTypes.GENERIC, identifier, lootPools, lootFunctions);
		}

		public JsonElement serialize(LootTable lootTable, Type type, JsonSerializationContext jsonSerializationContext) {
			JsonObject jsonObject = new JsonObject();
			if (lootTable.type != LootTable.GENERIC) {
				Identifier identifier = LootContextTypes.getId(lootTable.type);
				if (identifier != null) {
					jsonObject.addProperty("type", identifier.toString());
				} else {
					LootTable.LOGGER.warn("Failed to find id for param set {}", lootTable.type);
				}
			}

			if (lootTable.randomSequenceId != null) {
				jsonObject.addProperty("random_sequence", lootTable.randomSequenceId.toString());
			}

			if (lootTable.pools.length > 0) {
				jsonObject.add("pools", jsonSerializationContext.serialize(lootTable.pools));
			}

			if (!ArrayUtils.isEmpty((Object[])lootTable.functions)) {
				jsonObject.add("functions", jsonSerializationContext.serialize(lootTable.functions));
			}

			return jsonObject;
		}
	}
}
