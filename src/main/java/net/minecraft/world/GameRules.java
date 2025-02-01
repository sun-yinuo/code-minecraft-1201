package net.minecraft.world;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Maps;
import com.mojang.brigadier.arguments.ArgumentType;
import com.mojang.brigadier.arguments.BoolArgumentType;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.builder.RequiredArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.logging.LogUtils;
import com.mojang.serialization.DynamicLike;
import java.util.Comparator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.function.BiConsumer;
import java.util.function.Function;
import java.util.function.Supplier;
import net.minecraft.entity.EntityStatuses;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.network.packet.s2c.play.EntityStatusS2CPacket;
import net.minecraft.network.packet.s2c.play.GameStateChangeS2CPacket;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;

public class GameRules {
	public static final int DEFAULT_RANDOM_TICK_SPEED = 3;
	static final Logger LOGGER = LogUtils.getLogger();
	private static final Map<Key<?>, Type<?>> RULE_TYPES = Maps.newTreeMap(Comparator.comparing(key -> key.name));
	public static final Key<BooleanRule> DO_FIRE_TICK = register("doFireTick", Category.UPDATES, BooleanRule.create(true));
	/**
	 * A {@linkplain Rule game rule} which regulates whether mobs can modify the world.
	 * 
	 * <p>Generally one is expected to test this rule before an entity modifies the world.
	 * 
	 * <p>In vanilla, this includes:
	 * <ul>
	 * <li>Whether creeper explosions destroy blocks
	 * <li>Whether a zombie can break down a door
	 * <li>Whether a wither killing an entity will place or drop a wither rose
	 * </ul>
	 */
	public static final Key<BooleanRule> DO_MOB_GRIEFING = register("mobGriefing", Category.MOBS, BooleanRule.create(true));
	/**
	 * A {@linkplain Rule game rule} which regulates whether player inventories should be persist through respawning.
	 */
	public static final Key<BooleanRule> KEEP_INVENTORY = register(
		"keepInventory", Category.PLAYER, BooleanRule.create(false)
	);
	/**
	 * A {@linkplain Rule game rule} which regulates whether mobs can spawn naturally.
	 */
	public static final Key<BooleanRule> DO_MOB_SPAWNING = register(
		"doMobSpawning", Category.SPAWNING, BooleanRule.create(true)
	);
	/**
	 * A {@linkplain Rule game rule} which regulates whether mobs should drop loot on death.
	 */
	public static final Key<BooleanRule> DO_MOB_LOOT = register("doMobLoot", Category.DROPS, BooleanRule.create(true));
	/**
	 * A {@linkplain Rule game rule} which regulates whether blocks should drop their items when broken.
	 */
	public static final Key<BooleanRule> DO_TILE_DROPS = register("doTileDrops", Category.DROPS, BooleanRule.create(true));
	public static final Key<BooleanRule> DO_ENTITY_DROPS = register(
		"doEntityDrops", Category.DROPS, BooleanRule.create(true)
	);
	public static final Key<BooleanRule> COMMAND_BLOCK_OUTPUT = register(
		"commandBlockOutput", Category.CHAT, BooleanRule.create(true)
	);
	public static final Key<BooleanRule> NATURAL_REGENERATION = register(
		"naturalRegeneration", Category.PLAYER, BooleanRule.create(true)
	);
	public static final Key<BooleanRule> DO_DAYLIGHT_CYCLE = register(
		"doDaylightCycle", Category.UPDATES, BooleanRule.create(true)
	);
	public static final Key<BooleanRule> LOG_ADMIN_COMMANDS = register(
		"logAdminCommands", Category.CHAT, BooleanRule.create(true)
	);
	public static final Key<BooleanRule> SHOW_DEATH_MESSAGES = register(
		"showDeathMessages", Category.CHAT, BooleanRule.create(true)
	);
	public static final Key<IntRule> RANDOM_TICK_SPEED = register("randomTickSpeed", Category.UPDATES, IntRule.create(3));
	public static final Key<BooleanRule> SEND_COMMAND_FEEDBACK = register(
		"sendCommandFeedback", Category.CHAT, BooleanRule.create(true)
	);
	/**
	 * A {@linkplain Rule game rule} which regulates whether clients' {@linkplain net.minecraft.client.gui.hud.DebugHud debug HUD}s show reduced information.
	 * 
	 * <p>When the value of this rule is changed, all connected clients will be notified to update their display.
	 * In vanilla, this includes the visibility of coordinates on the clients' debug HUDs.
	 */
	public static final Key<BooleanRule> REDUCED_DEBUG_INFO = register(
		"reducedDebugInfo", Category.MISC, BooleanRule.create(false, (server, rule) -> {
			byte b = rule.get() ? EntityStatuses.USE_REDUCED_DEBUG_INFO : EntityStatuses.USE_FULL_DEBUG_INFO;

			for (ServerPlayerEntity serverPlayerEntity : server.getPlayerManager().getPlayerList()) {
				serverPlayerEntity.networkHandler.sendPacket(new EntityStatusS2CPacket(serverPlayerEntity, b));
			}
		})
	);
	public static final Key<BooleanRule> SPECTATORS_GENERATE_CHUNKS = register(
		"spectatorsGenerateChunks", Category.PLAYER, BooleanRule.create(true)
	);
	public static final Key<IntRule> SPAWN_RADIUS = register("spawnRadius", Category.PLAYER, IntRule.create(10));
	public static final Key<BooleanRule> DISABLE_ELYTRA_MOVEMENT_CHECK = register(
		"disableElytraMovementCheck", Category.PLAYER, BooleanRule.create(false)
	);
	/**
	 * A {@linkplain Rule game rule} which regulates the number of entities that can be crammed into a block space before they incur cramming damage.
	 */
	public static final Key<IntRule> MAX_ENTITY_CRAMMING = register("maxEntityCramming", Category.MOBS, IntRule.create(24));
	public static final Key<BooleanRule> DO_WEATHER_CYCLE = register(
		"doWeatherCycle", Category.UPDATES, BooleanRule.create(true)
	);
	public static final Key<BooleanRule> DO_LIMITED_CRAFTING = register(
		"doLimitedCrafting", Category.PLAYER, BooleanRule.create(false)
	);
	public static final Key<IntRule> MAX_COMMAND_CHAIN_LENGTH = register(
		"maxCommandChainLength", Category.MISC, IntRule.create(65536)
	);
	public static final Key<IntRule> COMMAND_MODIFICATION_BLOCK_LIMIT = register(
		"commandModificationBlockLimit", Category.MISC, IntRule.create(32768)
	);
	/**
	 * A {@linkplain Rule game rule} which regulates whether a player's advancements should be announced in chat.
	 */
	public static final Key<BooleanRule> ANNOUNCE_ADVANCEMENTS = register(
		"announceAdvancements", Category.CHAT, BooleanRule.create(true)
	);
	/**
	 * A {@linkplain Rule game rule} which regulates whether raids should occur.
	 * 
	 * <p>If this rule is set to {@code true} while raids are occurring, the raids will be stopped.
	 */
	public static final Key<BooleanRule> DISABLE_RAIDS = register("disableRaids", Category.MOBS, BooleanRule.create(false));
	public static final Key<BooleanRule> DO_INSOMNIA = register("doInsomnia", Category.SPAWNING, BooleanRule.create(true));
	/**
	 * A {@linkplain Rule game rule} which regulates whether a player should immediately respawn upon death.
	 */
	public static final Key<BooleanRule> DO_IMMEDIATE_RESPAWN = register(
		"doImmediateRespawn",
		Category.PLAYER,
		BooleanRule.create(
			false,
			(server, rule) -> {
				for (ServerPlayerEntity serverPlayerEntity : server.getPlayerManager().getPlayerList()) {
					serverPlayerEntity.networkHandler
						.sendPacket(new GameStateChangeS2CPacket(GameStateChangeS2CPacket.IMMEDIATE_RESPAWN, rule.get() ? 1.0F : GameStateChangeS2CPacket.DEMO_OPEN_SCREEN));
				}
			}
		)
	);
	public static final Key<BooleanRule> DROWNING_DAMAGE = register(
		"drowningDamage", Category.PLAYER, BooleanRule.create(true)
	);
	public static final Key<BooleanRule> FALL_DAMAGE = register("fallDamage", Category.PLAYER, BooleanRule.create(true));
	public static final Key<BooleanRule> FIRE_DAMAGE = register("fireDamage", Category.PLAYER, BooleanRule.create(true));
	public static final Key<BooleanRule> FREEZE_DAMAGE = register(
		"freezeDamage", Category.PLAYER, BooleanRule.create(true)
	);
	public static final Key<BooleanRule> DO_PATROL_SPAWNING = register(
		"doPatrolSpawning", Category.SPAWNING, BooleanRule.create(true)
	);
	public static final Key<BooleanRule> DO_TRADER_SPAWNING = register(
		"doTraderSpawning", Category.SPAWNING, BooleanRule.create(true)
	);
	public static final Key<BooleanRule> DO_WARDEN_SPAWNING = register(
		"doWardenSpawning", Category.SPAWNING, BooleanRule.create(true)
	);
	public static final Key<BooleanRule> FORGIVE_DEAD_PLAYERS = register(
		"forgiveDeadPlayers", Category.MOBS, BooleanRule.create(true)
	);
	public static final Key<BooleanRule> UNIVERSAL_ANGER = register(
		"universalAnger", Category.MOBS, BooleanRule.create(false)
	);
	public static final Key<IntRule> PLAYERS_SLEEPING_PERCENTAGE = register(
		"playersSleepingPercentage", Category.PLAYER, IntRule.create(100)
	);
	public static final Key<BooleanRule> BLOCK_EXPLOSION_DROP_DECAY = register(
		"blockExplosionDropDecay", Category.DROPS, BooleanRule.create(true)
	);
	public static final Key<BooleanRule> MOB_EXPLOSION_DROP_DECAY = register(
		"mobExplosionDropDecay", Category.DROPS, BooleanRule.create(true)
	);
	public static final Key<BooleanRule> TNT_EXPLOSION_DROP_DECAY = register(
		"tntExplosionDropDecay", Category.DROPS, BooleanRule.create(false)
	);
	public static final Key<IntRule> SNOW_ACCUMULATION_HEIGHT = register(
		"snowAccumulationHeight", Category.UPDATES, IntRule.create(1)
	);
	public static final Key<BooleanRule> WATER_SOURCE_CONVERSION = register(
		"waterSourceConversion", Category.UPDATES, BooleanRule.create(true)
	);
	public static final Key<BooleanRule> LAVA_SOURCE_CONVERSION = register(
		"lavaSourceConversion", Category.UPDATES, BooleanRule.create(false)
	);
	public static final Key<BooleanRule> GLOBAL_SOUND_EVENTS = register(
		"globalSoundEvents", Category.MISC, BooleanRule.create(true)
	);
	public static final Key<BooleanRule> DO_VINES_SPREAD = register(
		"doVinesSpread", Category.UPDATES, BooleanRule.create(true)
	);
	private final Map<Key<?>, Rule<?>> rules;

	private static <T extends Rule<T>> Key<T> register(String name, Category category, Type<T> type) {
		Key<T> key = new Key<>(name, category);
		Type<?> type2 = (Type<?>)RULE_TYPES.put(key, type);
		if (type2 != null) {
			throw new IllegalStateException("Duplicate game rule registration for " + name);
		} else {
			return key;
		}
	}

	public GameRules(DynamicLike<?> dynamic) {
		this();
		this.load(dynamic);
	}

	public GameRules() {
		this.rules = (Map<Key<?>, Rule<?>>)RULE_TYPES.entrySet()
			.stream()
			.collect(ImmutableMap.toImmutableMap(Entry::getKey, e -> ((Type)e.getValue()).createRule()));
	}

	private GameRules(Map<Key<?>, Rule<?>> rules) {
		this.rules = rules;
	}

	public <T extends Rule<T>> T get(Key<T> key) {
		return (T)this.rules.get(key);
	}

	public NbtCompound toNbt() {
		NbtCompound nbtCompound = new NbtCompound();
		this.rules.forEach((key, rule) -> nbtCompound.putString(key.name, rule.serialize()));
		return nbtCompound;
	}

	private void load(DynamicLike<?> dynamic) {
		this.rules.forEach((key, rule) -> dynamic.get(key.name).asString().result().ifPresent(rule::deserialize));
	}

	public GameRules copy() {
		return new GameRules(
			(Map<Key<?>, Rule<?>>)this.rules
				.entrySet()
				.stream()
				.collect(ImmutableMap.toImmutableMap(Entry::getKey, entry -> ((Rule)entry.getValue()).copy()))
		);
	}

	/**
	 * Make the visitor visit all registered game rules.
	 * 
	 * <p>The visitation involves calling both {@link Visitor#visit(Key, Type)} and {@code visitX} for every game rule, where X is the current rule's concrete type such as a boolean.
	 */
	public static void accept(Visitor visitor) {
		RULE_TYPES.forEach((key, type) -> accept(visitor, key, type));
	}

	private static <T extends Rule<T>> void accept(Visitor consumer, Key<?> key, Type<?> type) {
		consumer.visit(key, type);
		type.accept(consumer, key);
	}

	public void setAllValues(GameRules rules, @Nullable MinecraftServer server) {
		rules.rules.keySet().forEach(key -> this.setValue(key, rules, server));
	}

	private <T extends Rule<T>> void setValue(Key<T> key, GameRules rules, @Nullable MinecraftServer server) {
		T rule = rules.get(key);
		this.<T>get(key).setValue(rule, server);
	}

	public boolean getBoolean(Key<BooleanRule> rule) {
		return this.get(rule).get();
	}

	public int getInt(Key<IntRule> rule) {
		return this.get(rule).get();
	}

	interface Acceptor<T extends Rule<T>> {
		void call(Visitor consumer, Key<T> key, Type<T> type);
	}

	public static class BooleanRule extends Rule<BooleanRule> {
		private boolean value;

		static Type<BooleanRule> create(boolean initialValue, BiConsumer<MinecraftServer, BooleanRule> changeCallback) {
			return new Type<>(BoolArgumentType::bool, type -> new BooleanRule(type, initialValue), changeCallback, Visitor::visitBoolean);
		}

		static Type<BooleanRule> create(boolean initialValue) {
			return create(initialValue, (server, rule) -> {
			});
		}

		public BooleanRule(Type<BooleanRule> type, boolean initialValue) {
			super(type);
			this.value = initialValue;
		}

		@Override
		protected void setFromArgument(CommandContext<ServerCommandSource> context, String name) {
			this.value = BoolArgumentType.getBool(context, name);
		}

		public boolean get() {
			return this.value;
		}

		public void set(boolean value, @Nullable MinecraftServer server) {
			this.value = value;
			this.changed(server);
		}

		@Override
		public String serialize() {
			return Boolean.toString(this.value);
		}

		@Override
		protected void deserialize(String value) {
			this.value = Boolean.parseBoolean(value);
		}

		@Override
		public int getCommandResult() {
			return this.value ? 1 : 0;
		}

		protected BooleanRule getThis() {
			return this;
		}

		protected BooleanRule copy() {
			return new BooleanRule(this.type, this.value);
		}

		public void setValue(BooleanRule booleanRule, @Nullable MinecraftServer minecraftServer) {
			this.value = booleanRule.value;
			this.changed(minecraftServer);
		}
	}

	public static enum Category {
		PLAYER("gamerule.category.player"),
		MOBS("gamerule.category.mobs"),
		SPAWNING("gamerule.category.spawning"),
		DROPS("gamerule.category.drops"),
		UPDATES("gamerule.category.updates"),
		CHAT("gamerule.category.chat"),
		MISC("gamerule.category.misc");

		private final String category;

		private Category(String category) {
			this.category = category;
		}

		public String getCategory() {
			return this.category;
		}
	}

	public static class IntRule extends Rule<IntRule> {
		private int value;

		private static Type<IntRule> create(int initialValue, BiConsumer<MinecraftServer, IntRule> changeCallback) {
			return new Type<>(IntegerArgumentType::integer, type -> new IntRule(type, initialValue), changeCallback, Visitor::visitInt);
		}

		static Type<IntRule> create(int initialValue) {
			return create(initialValue, (server, rule) -> {
			});
		}

		public IntRule(Type<IntRule> rule, int initialValue) {
			super(rule);
			this.value = initialValue;
		}

		@Override
		protected void setFromArgument(CommandContext<ServerCommandSource> context, String name) {
			this.value = IntegerArgumentType.getInteger(context, name);
		}

		public int get() {
			return this.value;
		}

		public void set(int value, @Nullable MinecraftServer server) {
			this.value = value;
			this.changed(server);
		}

		@Override
		public String serialize() {
			return Integer.toString(this.value);
		}

		@Override
		protected void deserialize(String value) {
			this.value = parseInt(value);
		}

		/**
		 * Validates that an input is valid for this rule.
		 */
		public boolean validate(String input) {
			try {
				this.value = Integer.parseInt(input);
				return true;
			} catch (NumberFormatException var3) {
				return false;
			}
		}

		private static int parseInt(String input) {
			if (!input.isEmpty()) {
				try {
					return Integer.parseInt(input);
				} catch (NumberFormatException var2) {
					GameRules.LOGGER.warn("Failed to parse integer {}", input);
				}
			}

			return 0;
		}

		@Override
		public int getCommandResult() {
			return this.value;
		}

		protected IntRule getThis() {
			return this;
		}

		protected IntRule copy() {
			return new IntRule(this.type, this.value);
		}

		public void setValue(IntRule intRule, @Nullable MinecraftServer minecraftServer) {
			this.value = intRule.value;
			this.changed(minecraftServer);
		}
	}

	public static final class Key<T extends Rule<T>> {
		final String name;
		private final Category category;

		public Key(String name, Category category) {
			this.name = name;
			this.category = category;
		}

		public String toString() {
			return this.name;
		}

		public boolean equals(Object o) {
			return this == o ? true : o instanceof Key && ((Key)o).name.equals(this.name);
		}

		public int hashCode() {
			return this.name.hashCode();
		}

		public String getName() {
			return this.name;
		}

		public String getTranslationKey() {
			return "gamerule." + this.name;
		}

		public Category getCategory() {
			return this.category;
		}
	}

	public abstract static class Rule<T extends Rule<T>> {
		protected final Type<T> type;

		public Rule(Type<T> type) {
			this.type = type;
		}

		protected abstract void setFromArgument(CommandContext<ServerCommandSource> context, String name);

		public void set(CommandContext<ServerCommandSource> context, String name) {
			this.setFromArgument(context, name);
			this.changed(context.getSource().getServer());
		}

		protected void changed(@Nullable MinecraftServer server) {
			if (server != null) {
				this.type.changeCallback.accept(server, this.getThis());
			}
		}

		protected abstract void deserialize(String value);

		public abstract String serialize();

		public String toString() {
			return this.serialize();
		}

		public abstract int getCommandResult();

		protected abstract T getThis();

		protected abstract T copy();

		public abstract void setValue(T rule, @Nullable MinecraftServer server);
	}

	public static class Type<T extends Rule<T>> {
		private final Supplier<ArgumentType<?>> argumentType;
		private final Function<Type<T>, T> ruleFactory;
		final BiConsumer<MinecraftServer, T> changeCallback;
		private final Acceptor<T> ruleAcceptor;

		Type(
			Supplier<ArgumentType<?>> argumentType,
			Function<Type<T>, T> ruleFactory,
			BiConsumer<MinecraftServer, T> changeCallback,
			Acceptor<T> ruleAcceptor
		) {
			this.argumentType = argumentType;
			this.ruleFactory = ruleFactory;
			this.changeCallback = changeCallback;
			this.ruleAcceptor = ruleAcceptor;
		}

		public RequiredArgumentBuilder<ServerCommandSource, ?> argument(String name) {
			return CommandManager.argument(name, (ArgumentType<T>)this.argumentType.get());
		}

		public T createRule() {
			return (T)this.ruleFactory.apply(this);
		}

		public void accept(Visitor consumer, Key<T> key) {
			this.ruleAcceptor.call(consumer, key, this);
		}
	}

	/**
	 * A visitor used to visit all game rules.
	 */
	public interface Visitor {
		/**
		 * Visit a game rule.
		 * 
		 * <p>It is expected all game rules regardless of type will be visited using this method.
		 */
		default <T extends Rule<T>> void visit(Key<T> key, Type<T> type) {
		}

		/**
		 * Visit a boolean rule.
		 * 
		 * <p>Note {@link #visit(Key, Type)} will be called before this method.
		 */
		default void visitBoolean(Key<BooleanRule> key, Type<BooleanRule> type) {
		}

		/**
		 * Visit an integer rule.
		 * 
		 * <p>Note {@link #visit(Key, Type)} will be called before this method.
		 */
		default void visitInt(Key<IntRule> key, Type<IntRule> type) {
		}
	}
}
