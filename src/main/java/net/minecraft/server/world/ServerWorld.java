package net.minecraft.server.world;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.Lists;
import com.mojang.datafixers.DataFixer;
import com.mojang.datafixers.util.Pair;
import com.mojang.logging.LogUtils;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.longs.LongSet;
import it.unimi.dsi.fastutil.longs.LongSets;
import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import it.unimi.dsi.fastutil.objects.ObjectLinkedOpenHashSet;
import it.unimi.dsi.fastutil.objects.ObjectOpenHashSet;
import it.unimi.dsi.fastutil.objects.Object2IntMap.Entry;
import java.io.IOException;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.Executor;
import java.util.function.BooleanSupplier;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import net.fabricmc.fabric.api.attachment.v1.AttachmentTarget;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.SnowBlock;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityInteraction;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.InteractionObserver;
import net.minecraft.entity.LightningEntity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.Npc;
import net.minecraft.entity.SpawnGroup;
import net.minecraft.entity.ai.pathing.EntityNavigation;
import net.minecraft.entity.boss.dragon.EnderDragonEntity;
import net.minecraft.entity.boss.dragon.EnderDragonFight;
import net.minecraft.entity.boss.dragon.EnderDragonPart;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.entity.mob.SkeletonHorseEntity;
import net.minecraft.entity.mob.WaterCreatureEntity;
import net.minecraft.entity.passive.AnimalEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.fluid.Fluid;
import net.minecraft.fluid.FluidState;
import net.minecraft.item.map.MapState;
import net.minecraft.network.packet.Packet;
import net.minecraft.network.packet.s2c.play.BlockBreakingProgressS2CPacket;
import net.minecraft.network.packet.s2c.play.BlockEventS2CPacket;
import net.minecraft.network.packet.s2c.play.EntityDamageS2CPacket;
import net.minecraft.network.packet.s2c.play.EntityStatusS2CPacket;
import net.minecraft.network.packet.s2c.play.ExplosionS2CPacket;
import net.minecraft.network.packet.s2c.play.GameStateChangeS2CPacket;
import net.minecraft.network.packet.s2c.play.ParticleS2CPacket;
import net.minecraft.network.packet.s2c.play.PlaySoundFromEntityS2CPacket;
import net.minecraft.network.packet.s2c.play.PlaySoundS2CPacket;
import net.minecraft.network.packet.s2c.play.PlayerSpawnPositionS2CPacket;
import net.minecraft.network.packet.s2c.play.WorldEventS2CPacket;
import net.minecraft.particle.ParticleEffect;
import net.minecraft.recipe.RecipeManager;
import net.minecraft.registry.Registries;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.registry.entry.RegistryEntryList;
import net.minecraft.registry.tag.TagKey;
import net.minecraft.resource.featuretoggle.FeatureSet;
import net.minecraft.scoreboard.ServerScoreboard;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.WorldGenerationProgressListener;
import net.minecraft.server.network.DebugInfoSender;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvent;
import net.minecraft.structure.StructureTemplateManager;
import net.minecraft.text.Text;
import net.minecraft.util.CsvWriter;
import net.minecraft.util.Identifier;
import net.minecraft.util.ProgressListener;
import net.minecraft.util.TypeFilter;
import net.minecraft.util.Unit;
import net.minecraft.util.Util;
import net.minecraft.util.crash.CrashReport;
import net.minecraft.util.function.BooleanBiFunction;
import net.minecraft.util.function.LazyIterationConsumer;
import net.minecraft.util.math.BlockBox;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.util.math.ChunkSectionPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.math.intprovider.IntProvider;
import net.minecraft.util.math.intprovider.UniformIntProvider;
import net.minecraft.util.math.random.Random;
import net.minecraft.util.math.random.RandomSequencesState;
import net.minecraft.util.profiler.Profiler;
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.util.shape.VoxelShapes;
import net.minecraft.village.raid.Raid;
import net.minecraft.village.raid.RaidManager;
import net.minecraft.world.EntityList;
import net.minecraft.world.ForcedChunkState;
import net.minecraft.world.GameRules;
import net.minecraft.world.Heightmap;
import net.minecraft.world.IdCountsState;
import net.minecraft.world.LocalDifficulty;
import net.minecraft.world.PersistentStateManager;
import net.minecraft.world.PortalForcer;
import net.minecraft.world.SpawnHelper;
import net.minecraft.world.StructureLocator;
import net.minecraft.world.StructureWorldAccess;
import net.minecraft.world.World;
import net.minecraft.world.biome.Biome;
import net.minecraft.world.chunk.BlockEntityTickInvoker;
import net.minecraft.world.chunk.Chunk;
import net.minecraft.world.chunk.ChunkSection;
import net.minecraft.world.chunk.WorldChunk;
import net.minecraft.world.dimension.DimensionOptions;
import net.minecraft.world.dimension.DimensionTypes;
import net.minecraft.world.entity.EntityHandler;
import net.minecraft.world.entity.EntityLookup;
import net.minecraft.world.event.GameEvent;
import net.minecraft.world.event.listener.EntityGameEventHandler;
import net.minecraft.world.event.listener.GameEventDispatchManager;
import net.minecraft.world.explosion.Explosion;
import net.minecraft.world.explosion.ExplosionBehavior;
import net.minecraft.world.gen.StructureAccessor;
import net.minecraft.world.gen.chunk.ChunkGenerator;
import net.minecraft.world.gen.structure.Structure;
import net.minecraft.world.level.ServerWorldProperties;
import net.minecraft.world.level.storage.LevelStorage;
import net.minecraft.world.poi.PointOfInterestStorage;
import net.minecraft.world.poi.PointOfInterestType;
import net.minecraft.world.poi.PointOfInterestTypes;
import net.minecraft.world.spawner.Spawner;
import net.minecraft.world.storage.ChunkDataAccess;
import net.minecraft.world.storage.EntityChunkDataAccess;
import net.minecraft.world.tick.WorldTickScheduler;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;

public class ServerWorld extends World implements StructureWorldAccess, AttachmentTarget {
	public static final BlockPos END_SPAWN_POS = new BlockPos(100, 50, 0);
	public static final IntProvider CLEAR_WEATHER_DURATION_PROVIDER = UniformIntProvider.create(12000, 180000);
	public static final IntProvider RAIN_WEATHER_DURATION_PROVIDER = UniformIntProvider.create(12000, 24000);
	private static final IntProvider CLEAR_THUNDER_WEATHER_DURATION_PROVIDER = UniformIntProvider.create(12000, 180000);
	public static final IntProvider THUNDER_WEATHER_DURATION_PROVIDER = UniformIntProvider.create(3600, 15600);
	private static final Logger LOGGER = LogUtils.getLogger();
	/**
	 * The number of ticks ({@value}) the world will continue to tick entities after
	 * all players have left and the world does not contain any forced chunks.
	 */
	private static final int SERVER_IDLE_COOLDOWN = 300;
	private static final int MAX_TICKS = 65536;
	final List<ServerPlayerEntity> players = Lists.<ServerPlayerEntity>newArrayList();
	private final ServerChunkManager chunkManager;
	private final MinecraftServer server;
	private final ServerWorldProperties worldProperties;
	final EntityList entityList = new EntityList();
	private final ServerEntityManager<Entity> entityManager;
	private final GameEventDispatchManager gameEventDispatchManager;
	public boolean savingDisabled;
	private final SleepManager sleepManager;
	private int idleTimeout;
	private final PortalForcer portalForcer;
	private final WorldTickScheduler<Block> blockTickScheduler = new WorldTickScheduler<>(this::isTickingFutureReady, this.getProfilerSupplier());
	private final WorldTickScheduler<Fluid> fluidTickScheduler = new WorldTickScheduler<>(this::isTickingFutureReady, this.getProfilerSupplier());
	final Set<MobEntity> loadedMobs = new ObjectOpenHashSet<>();
	volatile boolean duringListenerUpdate;
	protected final RaidManager raidManager;
	private final ObjectLinkedOpenHashSet<BlockEvent> syncedBlockEventQueue = new ObjectLinkedOpenHashSet<>();
	private final List<BlockEvent> blockEventQueue = new ArrayList(64);
	private boolean inBlockTick;
	private final List<Spawner> spawners;
	@Nullable
	private EnderDragonFight enderDragonFight;
	final Int2ObjectMap<EnderDragonPart> dragonParts = new Int2ObjectOpenHashMap<>();
	private final StructureAccessor structureAccessor;
	private final StructureLocator structureLocator;
	private final boolean shouldTickTime;
	private final RandomSequencesState randomSequences;

	public ServerWorld(
		MinecraftServer server,
		Executor workerExecutor,
		LevelStorage.Session session,
		ServerWorldProperties properties,
		RegistryKey<World> worldKey,
		DimensionOptions dimensionOptions,
		WorldGenerationProgressListener worldGenerationProgressListener,
		boolean debugWorld,
		long seed,
		List<Spawner> spawners,
		boolean shouldTickTime,
		@Nullable RandomSequencesState randomSequencesState
	) {
		super(
			properties,
			worldKey,
			server.getRegistryManager(),
			dimensionOptions.dimensionTypeEntry(),
			server::getProfiler,
			false,
			debugWorld,
			seed,
			server.getMaxChainedNeighborUpdates()
		);
		this.shouldTickTime = shouldTickTime;
		this.server = server;
		this.spawners = spawners;
		this.worldProperties = properties;
		ChunkGenerator chunkGenerator = dimensionOptions.chunkGenerator();
		boolean bl = server.syncChunkWrites();
		DataFixer dataFixer = server.getDataFixer();
		ChunkDataAccess<Entity> chunkDataAccess = new EntityChunkDataAccess(this, session.getWorldDirectory(worldKey).resolve("entities"), dataFixer, bl, server);
		this.entityManager = new ServerEntityManager<>(Entity.class, new ServerWorld.ServerEntityHandler(), chunkDataAccess);
		this.chunkManager = new ServerChunkManager(
			this,
			session,
			dataFixer,
			server.getStructureTemplateManager(),
			workerExecutor,
			chunkGenerator,
			server.getPlayerManager().getViewDistance(),
			server.getPlayerManager().getSimulationDistance(),
			bl,
			worldGenerationProgressListener,
			this.entityManager::updateTrackingStatus,
			() -> server.getOverworld().getPersistentStateManager()
		);
		this.chunkManager.getStructurePlacementCalculator().tryCalculate();
		this.portalForcer = new PortalForcer(this);
		this.calculateAmbientDarkness();
		this.initWeatherGradients();
		this.getWorldBorder().setMaxRadius(server.getMaxWorldBorderRadius());
		this.raidManager = this.getPersistentStateManager()
			.getOrCreate(nbt -> RaidManager.fromNbt(this, nbt), () -> new RaidManager(this), RaidManager.nameFor(this.getDimensionEntry()));
		if (!server.isSingleplayer()) {
			properties.setGameMode(server.getDefaultGameMode());
		}

		long l = server.getSaveProperties().getGeneratorOptions().getSeed();
		this.structureLocator = new StructureLocator(
			this.chunkManager.getChunkIoWorker(),
			this.getRegistryManager(),
			server.getStructureTemplateManager(),
			worldKey,
			chunkGenerator,
			this.chunkManager.getNoiseConfig(),
			this,
			chunkGenerator.getBiomeSource(),
			l,
			dataFixer
		);
		this.structureAccessor = new StructureAccessor(this, server.getSaveProperties().getGeneratorOptions(), this.structureLocator);
		if (this.getRegistryKey() == World.END && this.getDimensionEntry().matchesKey(DimensionTypes.THE_END)) {
			this.enderDragonFight = new EnderDragonFight(this, l, server.getSaveProperties().getDragonFight());
		} else {
			this.enderDragonFight = null;
		}

		this.sleepManager = new SleepManager();
		this.gameEventDispatchManager = new GameEventDispatchManager(this);
		this.randomSequences = (RandomSequencesState)Objects.requireNonNullElseGet(
			randomSequencesState,
			() -> this.getPersistentStateManager().getOrCreate(nbt -> RandomSequencesState.fromNbt(l, nbt), () -> new RandomSequencesState(l), "random_sequences")
		);
	}

	@Deprecated
	@VisibleForTesting
	public void setEnderDragonFight(@Nullable EnderDragonFight enderDragonFight) {
		this.enderDragonFight = enderDragonFight;
	}

	/**
	 * Sets the current weather, as well as how long it should last.
	 * 
	 * @see ServerWorldProperties#setClearWeatherTime
	 * @see ServerWorldProperties#setRainTime
	 * @see ServerWorldProperties#setThunderTime
	 * @see ServerWorldProperties#setRaining
	 * @see ServerWorldProperties#setThundering
	 * 
	 * @param thundering whether a thunderstorm is ongoing
	 * @param clearDuration how long the clear weather should last, in seconds
	 * @param raining whether a rain is ongoing
	 * @param rainDuration how long the rain or the thunderstorm should last, in seconds
	 */
	public void setWeather(int clearDuration, int rainDuration, boolean raining, boolean thundering) {
		this.worldProperties.setClearWeatherTime(clearDuration);
		this.worldProperties.setRainTime(rainDuration);
		this.worldProperties.setThunderTime(rainDuration);
		this.worldProperties.setRaining(raining);
		this.worldProperties.setThundering(thundering);
	}

	@Override
	public RegistryEntry<Biome> getGeneratorStoredBiome(int biomeX, int biomeY, int biomeZ) {
		return this.getChunkManager()
			.getChunkGenerator()
			.getBiomeSource()
			.getBiome(biomeX, biomeY, biomeZ, this.getChunkManager().getNoiseConfig().getMultiNoiseSampler());
	}

	public StructureAccessor getStructureAccessor() {
		return this.structureAccessor;
	}

	public void tick(BooleanSupplier shouldKeepTicking) {
		Profiler profiler = this.getProfiler();
		this.inBlockTick = true;
		profiler.push("world border");
		this.getWorldBorder().tick();
		profiler.swap("weather");
		this.tickWeather();
		int i = this.getGameRules().getInt(GameRules.PLAYERS_SLEEPING_PERCENTAGE);
		if (this.sleepManager.canSkipNight(i) && this.sleepManager.canResetTime(i, this.players)) {
			if (this.getGameRules().getBoolean(GameRules.DO_DAYLIGHT_CYCLE)) {
				long l = this.properties.getTimeOfDay() + 24000L;
				this.setTimeOfDay(l - l % 24000L);
			}

			this.wakeSleepingPlayers();
			if (this.getGameRules().getBoolean(GameRules.DO_WEATHER_CYCLE) && this.isRaining()) {
				this.resetWeather();
			}
		}

		this.calculateAmbientDarkness();
		this.tickTime();
		profiler.swap("tickPending");
		if (!this.isDebugWorld()) {
			long l = this.getTime();
			profiler.push("blockTicks");
			this.blockTickScheduler.tick(l, 65536, this::tickBlock);
			profiler.swap("fluidTicks");
			this.fluidTickScheduler.tick(l, 65536, this::tickFluid);
			profiler.pop();
		}

		profiler.swap("raid");
		this.raidManager.tick();
		profiler.swap("chunkSource");
		this.getChunkManager().tick(shouldKeepTicking, true);
		profiler.swap("blockEvents");
		this.processSyncedBlockEvents();
		this.inBlockTick = false;
		profiler.pop();
		boolean bl = !this.players.isEmpty() || !this.getForcedChunks().isEmpty();
		if (bl) {
			this.resetIdleTimeout();
		}

		if (bl || this.idleTimeout++ < 300) {
			profiler.push("entities");
			if (this.enderDragonFight != null) {
				profiler.push("dragonFight");
				this.enderDragonFight.tick();
				profiler.pop();
			}

			this.entityList.forEach(entity -> {
				if (!entity.isRemoved()) {
					if (this.shouldCancelSpawn(entity)) {
						entity.discard();
					} else {
						profiler.push("checkDespawn");
						entity.checkDespawn();
						profiler.pop();
						if (this.chunkManager.threadedAnvilChunkStorage.getTicketManager().shouldTickEntities(entity.getChunkPos().toLong())) {
							Entity entity2 = entity.getVehicle();
							if (entity2 != null) {
								if (!entity2.isRemoved() && entity2.hasPassenger(entity)) {
									return;
								}

								entity.stopRiding();
							}

							profiler.push("tick");
							this.tickEntity(this::tickEntity, entity);
							profiler.pop();
						}
					}
				}
			});
			profiler.pop();
			this.tickBlockEntities();
		}

		profiler.push("entityManagement");
		this.entityManager.tick();
		profiler.pop();
	}

	@Override
	public boolean shouldTickBlocksInChunk(long chunkPos) {
		return this.chunkManager.threadedAnvilChunkStorage.getTicketManager().shouldTickBlocks(chunkPos);
	}

	protected void tickTime() {
		if (this.shouldTickTime) {
			long l = this.properties.getTime() + 1L;
			this.worldProperties.setTime(l);
			this.worldProperties.getScheduledEvents().processEvents(this.server, l);
			if (this.properties.getGameRules().getBoolean(GameRules.DO_DAYLIGHT_CYCLE)) {
				this.setTimeOfDay(this.properties.getTimeOfDay() + 1L);
			}
		}
	}

	/**
	 * Sets the time of day.
	 * 
	 * <p>Time of day is different to "time", which is incremented on every tick and
	 * cannot be modified; Time of day affects the day-night cycle, can be changed using
	 * {@link net.minecraft.server.command.TimeCommand /time command}, and can be frozen
	 * if {@link net.minecraft.world.GameRules#DO_DAYLIGHT_CYCLE doDaylightCycle} gamerule is turned off.
	 * Time is used to track scheduled ticks and cannot be modified or frozen.
	 * 
	 * @see net.minecraft.world.level.ServerWorldProperties#setTimeOfDay
	 */
	public void setTimeOfDay(long timeOfDay) {
		this.worldProperties.setTimeOfDay(timeOfDay);
	}

	public void tickSpawners(boolean spawnMonsters, boolean spawnAnimals) {
		for (Spawner spawner : this.spawners) {
			spawner.spawn(this, spawnMonsters, spawnAnimals);
		}
	}

	private boolean shouldCancelSpawn(Entity entity) {
		return this.server.shouldSpawnAnimals() || !(entity instanceof AnimalEntity) && !(entity instanceof WaterCreatureEntity)
			? !this.server.shouldSpawnNpcs() && entity instanceof Npc
			: true;
	}

	private void wakeSleepingPlayers() {
		this.sleepManager.clearSleeping();
		((List)this.players.stream().filter(LivingEntity::isSleeping).collect(Collectors.toList())).forEach(player -> player.wakeUp(false, false));
	}

	public void tickChunk(WorldChunk chunk, int randomTickSpeed) {
		ChunkPos chunkPos = chunk.getPos();
		boolean bl = this.isRaining();
		int i = chunkPos.getStartX();
		int j = chunkPos.getStartZ();
		Profiler profiler = this.getProfiler();
		profiler.push("thunder");
		if (bl && this.isThundering() && this.random.nextInt(100000) == 0) {
			BlockPos blockPos = this.getLightningPos(this.getRandomPosInChunk(i, 0, j, 15));
			if (this.hasRain(blockPos)) {
				LocalDifficulty localDifficulty = this.getLocalDifficulty(blockPos);
				boolean bl2 = this.getGameRules().getBoolean(GameRules.DO_MOB_SPAWNING)
					&& this.random.nextDouble() < (double)localDifficulty.getLocalDifficulty() * 0.01
					&& !this.getBlockState(blockPos.down()).isOf(Blocks.LIGHTNING_ROD);
				if (bl2) {
					SkeletonHorseEntity skeletonHorseEntity = EntityType.SKELETON_HORSE.create(this);
					if (skeletonHorseEntity != null) {
						skeletonHorseEntity.setTrapped(true);
						skeletonHorseEntity.setBreedingAge(0);
						skeletonHorseEntity.setPosition((double)blockPos.getX(), (double)blockPos.getY(), (double)blockPos.getZ());
						this.spawnEntity(skeletonHorseEntity);
					}
				}

				LightningEntity lightningEntity = EntityType.LIGHTNING_BOLT.create(this);
				if (lightningEntity != null) {
					lightningEntity.refreshPositionAfterTeleport(Vec3d.ofBottomCenter(blockPos));
					lightningEntity.setCosmetic(bl2);
					this.spawnEntity(lightningEntity);
				}
			}
		}

		profiler.swap("iceandsnow");
		if (this.random.nextInt(16) == 0) {
			BlockPos blockPos = this.getTopPosition(Heightmap.Type.MOTION_BLOCKING, this.getRandomPosInChunk(i, 0, j, 15));
			BlockPos blockPos2 = blockPos.down();
			Biome biome = this.getBiome(blockPos).value();
			if (biome.canSetIce(this, blockPos2)) {
				this.setBlockState(blockPos2, Blocks.ICE.getDefaultState());
			}

			if (bl) {
				int k = this.getGameRules().getInt(GameRules.SNOW_ACCUMULATION_HEIGHT);
				if (k > 0 && biome.canSetSnow(this, blockPos)) {
					BlockState blockState = this.getBlockState(blockPos);
					if (blockState.isOf(Blocks.SNOW)) {
						int l = (Integer)blockState.get(SnowBlock.LAYERS);
						if (l < Math.min(k, 8)) {
							BlockState blockState2 = blockState.with(SnowBlock.LAYERS, Integer.valueOf(l + 1));
							Block.pushEntitiesUpBeforeBlockChange(blockState, blockState2, this, blockPos);
							this.setBlockState(blockPos, blockState2);
						}
					} else {
						this.setBlockState(blockPos, Blocks.SNOW.getDefaultState());
					}
				}

				Biome.Precipitation precipitation = biome.getPrecipitation(blockPos2);
				if (precipitation != Biome.Precipitation.NONE) {
					BlockState blockState3 = this.getBlockState(blockPos2);
					blockState3.getBlock().precipitationTick(blockState3, this, blockPos2, precipitation);
				}
			}
		}

		profiler.swap("tickBlocks");
		if (randomTickSpeed > 0) {
			ChunkSection[] chunkSections = chunk.getSectionArray();

			for (int m = 0; m < chunkSections.length; m++) {
				ChunkSection chunkSection = chunkSections[m];
				if (chunkSection.hasRandomTicks()) {
					int kx = chunk.sectionIndexToCoord(m);
					int n = ChunkSectionPos.getBlockCoord(kx);

					for (int l = 0; l < randomTickSpeed; l++) {
						BlockPos blockPos3 = this.getRandomPosInChunk(i, n, j, 15);
						profiler.push("randomTick");
						BlockState blockState4 = chunkSection.getBlockState(blockPos3.getX() - i, blockPos3.getY() - n, blockPos3.getZ() - j);
						if (blockState4.hasRandomTicks()) {
							blockState4.randomTick(this, blockPos3, this.random);
						}

						FluidState fluidState = blockState4.getFluidState();
						if (fluidState.hasRandomTicks()) {
							fluidState.onRandomTick(this, blockPos3, this.random);
						}

						profiler.pop();
					}
				}
			}
		}

		profiler.pop();
	}

	private Optional<BlockPos> getLightningRodPos(BlockPos pos) {
		Optional<BlockPos> optional = this.getPointOfInterestStorage()
			.getNearestPosition(
				poiType -> poiType.matchesKey(PointOfInterestTypes.LIGHTNING_ROD),
				innerPos -> innerPos.getY() == this.getTopY(Heightmap.Type.WORLD_SURFACE, innerPos.getX(), innerPos.getZ()) - 1,
				pos,
				128,
				PointOfInterestStorage.OccupationStatus.ANY
			);
		return optional.map(innerPos -> innerPos.up(1));
	}

	protected BlockPos getLightningPos(BlockPos pos) {
		BlockPos blockPos = this.getTopPosition(Heightmap.Type.MOTION_BLOCKING, pos);
		Optional<BlockPos> optional = this.getLightningRodPos(blockPos);
		if (optional.isPresent()) {
			return (BlockPos)optional.get();
		} else {
			Box box = new Box(blockPos, new BlockPos(blockPos.getX(), this.getTopY(), blockPos.getZ())).expand(3.0);
			List<LivingEntity> list = this.getEntitiesByClass(
				LivingEntity.class, box, entity -> entity != null && entity.isAlive() && this.isSkyVisible(entity.getBlockPos())
			);
			if (!list.isEmpty()) {
				return ((LivingEntity)list.get(this.random.nextInt(list.size()))).getBlockPos();
			} else {
				if (blockPos.getY() == this.getBottomY() - 1) {
					blockPos = blockPos.up(2);
				}

				return blockPos;
			}
		}
	}

	public boolean isInBlockTick() {
		return this.inBlockTick;
	}

	/**
	 * {@return whether sleeping can cause the night to skip}
	 */
	public boolean isSleepingEnabled() {
		return this.getGameRules().getInt(GameRules.PLAYERS_SLEEPING_PERCENTAGE) <= 100;
	}

	/**
	 * Sends sleeping status action bar messages to players in this world.
	 */
	private void sendSleepingStatus() {
		if (this.isSleepingEnabled()) {
			if (!this.getServer().isSingleplayer() || this.getServer().isRemote()) {
				int i = this.getGameRules().getInt(GameRules.PLAYERS_SLEEPING_PERCENTAGE);
				Text text;
				if (this.sleepManager.canSkipNight(i)) {
					text = Text.translatable("sleep.skipping_night");
				} else {
					text = Text.translatable("sleep.players_sleeping", this.sleepManager.getSleeping(), this.sleepManager.getNightSkippingRequirement(i));
				}

				for (ServerPlayerEntity serverPlayerEntity : this.players) {
					serverPlayerEntity.sendMessage(text, true);
				}
			}
		}
	}

	public void updateSleepingPlayers() {
		if (!this.players.isEmpty() && this.sleepManager.update(this.players)) {
			this.sendSleepingStatus();
		}
	}

	public ServerScoreboard getScoreboard() {
		return this.server.getScoreboard();
	}

	private void tickWeather() {
		boolean bl = this.isRaining();
		if (this.getDimension().hasSkyLight()) {
			if (this.getGameRules().getBoolean(GameRules.DO_WEATHER_CYCLE)) {
				int i = this.worldProperties.getClearWeatherTime();
				int j = this.worldProperties.getThunderTime();
				int k = this.worldProperties.getRainTime();
				boolean bl2 = this.properties.isThundering();
				boolean bl3 = this.properties.isRaining();
				if (i > 0) {
					i--;
					j = bl2 ? 0 : 1;
					k = bl3 ? 0 : 1;
					bl2 = false;
					bl3 = false;
				} else {
					if (j > 0) {
						if (--j == 0) {
							bl2 = !bl2;
						}
					} else if (bl2) {
						j = THUNDER_WEATHER_DURATION_PROVIDER.get(this.random);
					} else {
						j = CLEAR_THUNDER_WEATHER_DURATION_PROVIDER.get(this.random);
					}

					if (k > 0) {
						if (--k == 0) {
							bl3 = !bl3;
						}
					} else if (bl3) {
						k = RAIN_WEATHER_DURATION_PROVIDER.get(this.random);
					} else {
						k = CLEAR_WEATHER_DURATION_PROVIDER.get(this.random);
					}
				}

				this.worldProperties.setThunderTime(j);
				this.worldProperties.setRainTime(k);
				this.worldProperties.setClearWeatherTime(i);
				this.worldProperties.setThundering(bl2);
				this.worldProperties.setRaining(bl3);
			}

			this.thunderGradientPrev = this.thunderGradient;
			if (this.properties.isThundering()) {
				this.thunderGradient += 0.01F;
			} else {
				this.thunderGradient -= 0.01F;
			}

			this.thunderGradient = MathHelper.clamp(this.thunderGradient, 0.0F, 1.0F);
			this.rainGradientPrev = this.rainGradient;
			if (this.properties.isRaining()) {
				this.rainGradient += 0.01F;
			} else {
				this.rainGradient -= 0.01F;
			}

			this.rainGradient = MathHelper.clamp(this.rainGradient, 0.0F, 1.0F);
		}

		if (this.rainGradientPrev != this.rainGradient) {
			this.server
				.getPlayerManager()
				.sendToDimension(new GameStateChangeS2CPacket(GameStateChangeS2CPacket.RAIN_GRADIENT_CHANGED, this.rainGradient), this.getRegistryKey());
		}

		if (this.thunderGradientPrev != this.thunderGradient) {
			this.server
				.getPlayerManager()
				.sendToDimension(new GameStateChangeS2CPacket(GameStateChangeS2CPacket.THUNDER_GRADIENT_CHANGED, this.thunderGradient), this.getRegistryKey());
		}

		if (bl != this.isRaining()) {
			if (bl) {
				this.server.getPlayerManager().sendToAll(new GameStateChangeS2CPacket(GameStateChangeS2CPacket.RAIN_STOPPED, GameStateChangeS2CPacket.DEMO_OPEN_SCREEN));
			} else {
				this.server.getPlayerManager().sendToAll(new GameStateChangeS2CPacket(GameStateChangeS2CPacket.RAIN_STARTED, GameStateChangeS2CPacket.DEMO_OPEN_SCREEN));
			}

			this.server.getPlayerManager().sendToAll(new GameStateChangeS2CPacket(GameStateChangeS2CPacket.RAIN_GRADIENT_CHANGED, this.rainGradient));
			this.server.getPlayerManager().sendToAll(new GameStateChangeS2CPacket(GameStateChangeS2CPacket.THUNDER_GRADIENT_CHANGED, this.thunderGradient));
		}
	}

	private void resetWeather() {
		this.worldProperties.setRainTime(0);
		this.worldProperties.setRaining(false);
		this.worldProperties.setThunderTime(0);
		this.worldProperties.setThundering(false);
	}

	public void resetIdleTimeout() {
		this.idleTimeout = 0;
	}

	private void tickFluid(BlockPos pos, Fluid fluid) {
		FluidState fluidState = this.getFluidState(pos);
		if (fluidState.isOf(fluid)) {
			fluidState.onScheduledTick(this, pos);
		}
	}

	private void tickBlock(BlockPos pos, Block block) {
		BlockState blockState = this.getBlockState(pos);
		if (blockState.isOf(block)) {
			blockState.scheduledTick(this, pos, this.random);
		}
	}

	public void tickEntity(Entity entity) {
		entity.resetPosition();
		Profiler profiler = this.getProfiler();
		entity.age++;
		this.getProfiler().push((Supplier<String>)(() -> Registries.ENTITY_TYPE.getId(entity.getType()).toString()));
		profiler.visit("tickNonPassenger");
		entity.tick();
		this.getProfiler().pop();

		for (Entity entity2 : entity.getPassengerList()) {
			this.tickPassenger(entity, entity2);
		}
	}

	private void tickPassenger(Entity vehicle, Entity passenger) {
		if (passenger.isRemoved() || passenger.getVehicle() != vehicle) {
			passenger.stopRiding();
		} else if (passenger instanceof PlayerEntity || this.entityList.has(passenger)) {
			passenger.resetPosition();
			passenger.age++;
			Profiler profiler = this.getProfiler();
			profiler.push((Supplier<String>)(() -> Registries.ENTITY_TYPE.getId(passenger.getType()).toString()));
			profiler.visit("tickPassenger");
			passenger.tickRiding();
			profiler.pop();

			for (Entity entity : passenger.getPassengerList()) {
				this.tickPassenger(passenger, entity);
			}
		}
	}

	@Override
	public boolean canPlayerModifyAt(PlayerEntity player, BlockPos pos) {
		return !this.server.isSpawnProtected(this, pos, player) && this.getWorldBorder().contains(pos);
	}

	/**
	 * Saves the world.
	 * 
	 * @param flush if it should immediately write all data to storage device
	 * @param progressListener the listener for the saving process, or {@code null} to specify none
	 * @param savingDisabled whether to return early without doing anything
	 */
	public void save(@Nullable ProgressListener progressListener, boolean flush, boolean savingDisabled) {
		ServerChunkManager serverChunkManager = this.getChunkManager();
		if (!savingDisabled) {
			if (progressListener != null) {
				progressListener.setTitle(Text.translatable("menu.savingLevel"));
			}

			this.saveLevel();
			if (progressListener != null) {
				progressListener.setTask(Text.translatable("menu.savingChunks"));
			}

			serverChunkManager.save(flush);
			if (flush) {
				this.entityManager.flush();
			} else {
				this.entityManager.save();
			}
		}
	}

	private void saveLevel() {
		if (this.enderDragonFight != null) {
			this.server.getSaveProperties().setDragonFight(this.enderDragonFight.toData());
		}

		this.getChunkManager().getPersistentStateManager().save();
	}

	/**
	 * Computes a list of entities of the given type.
	 * 
	 * <strong>Warning:</strong> If {@code null} is passed as the entity type filter, care should be
	 * taken that the type argument {@code T} is set to {@link Entity}, otherwise heap pollution
	 * in the returned list or {@link ClassCastException} can occur.
	 * 
	 * @return a list of entities of the given type
	 * 
	 * @param predicate a predicate which returned entities must satisfy
	 */
	public <T extends Entity> List<? extends T> getEntitiesByType(TypeFilter<Entity, T> filter, Predicate<? super T> predicate) {
		List<T> list = Lists.<T>newArrayList();
		this.collectEntitiesByType(filter, predicate, list);
		return list;
	}

	public <T extends Entity> void collectEntitiesByType(TypeFilter<Entity, T> filter, Predicate<? super T> predicate, List<? super T> result) {
		this.collectEntitiesByType(filter, predicate, result, Integer.MAX_VALUE);
	}

	/**
	 * Collects entities of the given type, up to {@code limit}. Using this can improve
	 * performance, especially if {@code limit} is small.
	 * 
	 * @see #getEntitiesByType
	 */
	public <T extends Entity> void collectEntitiesByType(TypeFilter<Entity, T> filter, Predicate<? super T> predicate, List<? super T> result, int limit) {
		this.getEntityLookup().forEach(filter, entity -> {
			if (predicate.test(entity)) {
				result.add(entity);
				if (result.size() >= limit) {
					return LazyIterationConsumer.NextIteration.ABORT;
				}
			}

			return LazyIterationConsumer.NextIteration.CONTINUE;
		});
	}

	/**
	 * {@return the list of alive ender dragons in the world}
	 */
	public List<? extends EnderDragonEntity> getAliveEnderDragons() {
		return this.getEntitiesByType(EntityType.ENDER_DRAGON, LivingEntity::isAlive);
	}

	/**
	 * {@return the list of players filtered using {@code predicate}}
	 */
	public List<ServerPlayerEntity> getPlayers(Predicate<? super ServerPlayerEntity> predicate) {
		return this.getPlayers(predicate, Integer.MAX_VALUE);
	}

	/**
	 * {@return the list of players filtered using {@code predicate}, up to {@code limit}}
	 */
	public List<ServerPlayerEntity> getPlayers(Predicate<? super ServerPlayerEntity> predicate, int limit) {
		List<ServerPlayerEntity> list = Lists.<ServerPlayerEntity>newArrayList();

		for (ServerPlayerEntity serverPlayerEntity : this.players) {
			if (predicate.test(serverPlayerEntity)) {
				list.add(serverPlayerEntity);
				if (list.size() >= limit) {
					return list;
				}
			}
		}

		return list;
	}

	/**
	 * {@return a randomly selected alive player, or {@code null} if there is none}
	 */
	@Nullable
	public ServerPlayerEntity getRandomAlivePlayer() {
		List<ServerPlayerEntity> list = this.getPlayers(LivingEntity::isAlive);
		return list.isEmpty() ? null : (ServerPlayerEntity)list.get(this.random.nextInt(list.size()));
	}

	@Override
	public boolean spawnEntity(Entity entity) {
		return this.addEntity(entity);
	}

	public boolean tryLoadEntity(Entity entity) {
		return this.addEntity(entity);
	}

	/**
	 * Called on the destination world when an entity changed the dimension.
	 * 
	 * <p>This does not get called for players changing dimensions.
	 * Use {@link #onPlayerChangeDimension} (for portals) or
	 * {@link #onPlayerTeleport} (for teleportation) instead.
	 * 
	 * @see Entity#moveToWorld
	 * @see #onPlayerTeleport
	 * @see #onPlayerChangeDimension
	 */
	public void onDimensionChanged(Entity entity) {
		this.addEntity(entity);
	}

	/**
	 * Called on the destination world when a player changed the dimension
	 * by teleportation.
	 * 
	 * @see ServerPlayerEntity#moveToWorld
	 * @see #onDimensionChanged
	 * @see #onPlayerChangeDimension
	 */
	public void onPlayerTeleport(ServerPlayerEntity player) {
		this.addPlayer(player);
	}

	/**
	 * Called on the destination world when a player changed the dimension using portals.
	 * 
	 * @see ServerPlayerEntity#moveToWorld
	 * @see #onDimensionChanged
	 * @see #onPlayerTeleport
	 */
	public void onPlayerChangeDimension(ServerPlayerEntity player) {
		this.addPlayer(player);
	}

	/**
	 * Called on the player's world when the player connected to the server and spawned.
	 */
	public void onPlayerConnected(ServerPlayerEntity player) {
		this.addPlayer(player);
	}

	/**
	 * Called on the world that has the player's respawn point when the player respawned.
	 */
	public void onPlayerRespawned(ServerPlayerEntity player) {
		this.addPlayer(player);
	}

	private void addPlayer(ServerPlayerEntity player) {
		Entity entity = this.getEntityLookup().get(player.getUuid());
		if (entity != null) {
			LOGGER.warn("Force-added player with duplicate UUID {}", player.getUuid().toString());
			entity.detach();
			this.removePlayer((ServerPlayerEntity)entity, Entity.RemovalReason.DISCARDED);
		}

		this.entityManager.addEntity(player);
	}

	private boolean addEntity(Entity entity) {
		if (entity.isRemoved()) {
			LOGGER.warn("Tried to add entity {} but it was marked as removed already", EntityType.getId(entity.getType()));
			return false;
		} else {
			return this.entityManager.addEntity(entity);
		}
	}

	/**
	 * Checks whether any of the entity and its passengers already exist
	 * in the world, and if not, spawns the entity with its passengers.
	 * 
	 * @return {@code true} if the spawning was successful, otherwise {@code false}
	 * 
	 * @see net.minecraft.world.ServerWorldAccess#spawnEntityAndPassengers
	 */
	public boolean spawnNewEntityAndPassengers(Entity entity) {
		if (entity.streamSelfAndPassengers().map(Entity::getUuid).anyMatch(this.entityManager::has)) {
			return false;
		} else {
			this.spawnEntityAndPassengers(entity);
			return true;
		}
	}

	public void unloadEntities(WorldChunk chunk) {
		chunk.clear();
		chunk.removeChunkTickSchedulers(this);
	}

	public void removePlayer(ServerPlayerEntity player, Entity.RemovalReason reason) {
		player.remove(reason);
	}

	@Override
	public void setBlockBreakingInfo(int entityId, BlockPos pos, int progress) {
		for (ServerPlayerEntity serverPlayerEntity : this.server.getPlayerManager().getPlayerList()) {
			if (serverPlayerEntity != null && serverPlayerEntity.getWorld() == this && serverPlayerEntity.getId() != entityId) {
				double d = (double)pos.getX() - serverPlayerEntity.getX();
				double e = (double)pos.getY() - serverPlayerEntity.getY();
				double f = (double)pos.getZ() - serverPlayerEntity.getZ();
				if (d * d + e * e + f * f < 1024.0) {
					serverPlayerEntity.networkHandler.sendPacket(new BlockBreakingProgressS2CPacket(entityId, pos, progress));
				}
			}
		}
	}

	@Override
	public void playSound(
		@Nullable PlayerEntity except, double x, double y, double z, RegistryEntry<SoundEvent> sound, SoundCategory category, float volume, float pitch, long seed
	) {
		this.server
			.getPlayerManager()
			.sendToAround(
				except,
				x,
				y,
				z,
				(double)sound.value().getDistanceToTravel(volume),
				this.getRegistryKey(),
				new PlaySoundS2CPacket(sound, category, x, y, z, volume, pitch, seed)
			);
	}

	@Override
	public void playSoundFromEntity(
		@Nullable PlayerEntity except, Entity entity, RegistryEntry<SoundEvent> sound, SoundCategory category, float volume, float pitch, long seed
	) {
		this.server
			.getPlayerManager()
			.sendToAround(
				except,
				entity.getX(),
				entity.getY(),
				entity.getZ(),
				(double)sound.value().getDistanceToTravel(volume),
				this.getRegistryKey(),
				new PlaySoundFromEntityS2CPacket(sound, category, entity, volume, pitch, seed)
			);
	}

	@Override
	public void syncGlobalEvent(int eventId, BlockPos pos, int data) {
		if (this.getGameRules().getBoolean(GameRules.GLOBAL_SOUND_EVENTS)) {
			this.server.getPlayerManager().sendToAll(new WorldEventS2CPacket(eventId, pos, data, true));
		} else {
			this.syncWorldEvent(null, eventId, pos, data);
		}
	}

	@Override
	public void syncWorldEvent(@Nullable PlayerEntity player, int eventId, BlockPos pos, int data) {
		this.server
			.getPlayerManager()
			.sendToAround(
				player, (double)pos.getX(), (double)pos.getY(), (double)pos.getZ(), 64.0, this.getRegistryKey(), new WorldEventS2CPacket(eventId, pos, data, false)
			);
	}

	public int getLogicalHeight() {
		return this.getDimension().logicalHeight();
	}

	@Override
	public void emitGameEvent(GameEvent event, Vec3d emitterPos, GameEvent.Emitter emitter) {
		this.gameEventDispatchManager.dispatch(event, emitterPos, emitter);
	}

	@Override
	public void updateListeners(BlockPos pos, BlockState oldState, BlockState newState, int flags) {
		if (this.duringListenerUpdate) {
			String string = "recursive call to sendBlockUpdated";
			Util.error("recursive call to sendBlockUpdated", new IllegalStateException("recursive call to sendBlockUpdated"));
		}

		this.getChunkManager().markForUpdate(pos);
		VoxelShape voxelShape = oldState.getCollisionShape(this, pos);
		VoxelShape voxelShape2 = newState.getCollisionShape(this, pos);
		if (VoxelShapes.matchesAnywhere(voxelShape, voxelShape2, BooleanBiFunction.NOT_SAME)) {
			List<EntityNavigation> list = new ObjectArrayList<>();

			for (MobEntity mobEntity : this.loadedMobs) {
				EntityNavigation entityNavigation = mobEntity.getNavigation();
				if (entityNavigation.shouldRecalculatePath(pos)) {
					list.add(entityNavigation);
				}
			}

			try {
				this.duringListenerUpdate = true;

				for (EntityNavigation entityNavigation2 : list) {
					entityNavigation2.recalculatePath();
				}
			} finally {
				this.duringListenerUpdate = false;
			}
		}
	}

	@Override
	public void updateNeighborsAlways(BlockPos pos, Block sourceBlock) {
		this.neighborUpdater.updateNeighbors(pos, sourceBlock, null);
	}

	@Override
	public void updateNeighborsExcept(BlockPos pos, Block sourceBlock, Direction direction) {
		this.neighborUpdater.updateNeighbors(pos, sourceBlock, direction);
	}

	@Override
	public void updateNeighbor(BlockPos pos, Block sourceBlock, BlockPos sourcePos) {
		this.neighborUpdater.updateNeighbor(pos, sourceBlock, sourcePos);
	}

	@Override
	public void updateNeighbor(BlockState state, BlockPos pos, Block sourceBlock, BlockPos sourcePos, boolean notify) {
		this.neighborUpdater.updateNeighbor(state, pos, sourceBlock, sourcePos, notify);
	}

	@Override
	public void sendEntityStatus(Entity entity, byte status) {
		this.getChunkManager().sendToNearbyPlayers(entity, new EntityStatusS2CPacket(entity, status));
	}

	@Override
	public void sendEntityDamage(Entity entity, DamageSource damageSource) {
		this.getChunkManager().sendToNearbyPlayers(entity, new EntityDamageS2CPacket(entity, damageSource));
	}

	public ServerChunkManager getChunkManager() {
		return this.chunkManager;
	}

	@Override
	public Explosion createExplosion(
		@Nullable Entity entity,
		@Nullable DamageSource damageSource,
		@Nullable ExplosionBehavior behavior,
		double x,
		double y,
		double z,
		float power,
		boolean createFire,
		World.ExplosionSourceType explosionSourceType
	) {
		Explosion explosion = this.createExplosion(entity, damageSource, behavior, x, y, z, power, createFire, explosionSourceType, false);
		if (!explosion.shouldDestroy()) {
			explosion.clearAffectedBlocks();
		}

		for (ServerPlayerEntity serverPlayerEntity : this.players) {
			if (serverPlayerEntity.squaredDistanceTo(x, y, z) < 4096.0) {
				serverPlayerEntity.networkHandler
					.sendPacket(new ExplosionS2CPacket(x, y, z, power, explosion.getAffectedBlocks(), (Vec3d)explosion.getAffectedPlayers().get(serverPlayerEntity)));
			}
		}

		return explosion;
	}

	@Override
	public void addSyncedBlockEvent(BlockPos pos, Block block, int type, int data) {
		this.syncedBlockEventQueue.add(new BlockEvent(pos, block, type, data));
	}

	private void processSyncedBlockEvents() {
		this.blockEventQueue.clear();

		while (!this.syncedBlockEventQueue.isEmpty()) {
			BlockEvent blockEvent = this.syncedBlockEventQueue.removeFirst();
			if (this.shouldTickBlockPos(blockEvent.pos())) {
				if (this.processBlockEvent(blockEvent)) {
					this.server
						.getPlayerManager()
						.sendToAround(
							null,
							(double)blockEvent.pos().getX(),
							(double)blockEvent.pos().getY(),
							(double)blockEvent.pos().getZ(),
							64.0,
							this.getRegistryKey(),
							new BlockEventS2CPacket(blockEvent.pos(), blockEvent.block(), blockEvent.type(), blockEvent.data())
						);
				}
			} else {
				this.blockEventQueue.add(blockEvent);
			}
		}

		this.syncedBlockEventQueue.addAll(this.blockEventQueue);
	}

	private boolean processBlockEvent(BlockEvent event) {
		BlockState blockState = this.getBlockState(event.pos());
		return blockState.isOf(event.block()) ? blockState.onSyncedBlockEvent(this, event.pos(), event.type(), event.data()) : false;
	}

	public WorldTickScheduler<Block> getBlockTickScheduler() {
		return this.blockTickScheduler;
	}

	public WorldTickScheduler<Fluid> getFluidTickScheduler() {
		return this.fluidTickScheduler;
	}

	@NotNull
	@Override
	public MinecraftServer getServer() {
		return this.server;
	}

	public PortalForcer getPortalForcer() {
		return this.portalForcer;
	}

	public StructureTemplateManager getStructureTemplateManager() {
		return this.server.getStructureTemplateManager();
	}

	/**
	 * Spawns a particle visible to nearby players.
	 * 
	 * @return the number of players the particle packet was sent to
	 * 
	 * @see #spawnParticles(ServerPlayerEntity, ParticleEffect, boolean, double, double, double, int, double, double, double, double)
	 */
	public <T extends ParticleEffect> int spawnParticles(
		T particle, double x, double y, double z, int count, double deltaX, double deltaY, double deltaZ, double speed
	) {
		ParticleS2CPacket particleS2CPacket = new ParticleS2CPacket(particle, false, x, y, z, (float)deltaX, (float)deltaY, (float)deltaZ, (float)speed, count);
		int i = 0;

		for (int j = 0; j < this.players.size(); j++) {
			ServerPlayerEntity serverPlayerEntity = (ServerPlayerEntity)this.players.get(j);
			if (this.sendToPlayerIfNearby(serverPlayerEntity, false, x, y, z, particleS2CPacket)) {
				i++;
			}
		}

		return i;
	}

	/**
	 * Spawns a particle visible to {@code viewer}, if the viewer is near the provided
	 * coordinates.
	 * 
	 * @return whether the particle packet was sent
	 * 
	 * @see #spawnParticles(ParticleEffect, double, double, double, int, double, double, double, double)
	 */
	public <T extends ParticleEffect> boolean spawnParticles(
		ServerPlayerEntity viewer, T particle, boolean force, double x, double y, double z, int count, double deltaX, double deltaY, double deltaZ, double speed
	) {
		Packet<?> packet = new ParticleS2CPacket(particle, force, x, y, z, (float)deltaX, (float)deltaY, (float)deltaZ, (float)speed, count);
		return this.sendToPlayerIfNearby(viewer, force, x, y, z, packet);
	}

	/**
	 * Sends the {@code packet} to {@code player} if the player
	 * is near the provided coordinates.
	 * 
	 * @return whether the packet was sent
	 * 
	 * @implNote The threshold is 32 blocks if {@code force} is {@code false}, and
	 * 512 blocks if {@code force} is {@code true}.
	 */
	public final boolean sendToPlayerIfNearby(ServerPlayerEntity player, boolean force, double x, double y, double z, Packet<?> packet) {
		if (player.getWorld() != this) {
			return false;
		} else {
			BlockPos blockPos = player.getBlockPos();
			if (blockPos.isWithinDistance(new Vec3d(x, y, z), force ? 512.0 : 32.0)) {
				player.networkHandler.sendPacket(packet);
				return true;
			} else {
				return false;
			}
		}
	}

	@Nullable
	@Override
	public Entity getEntityById(int id) {
		return this.getEntityLookup().get(id);
	}

	@Deprecated
	@Nullable
	public Entity getDragonPart(int id) {
		Entity entity = this.getEntityLookup().get(id);
		return entity != null ? entity : this.dragonParts.get(id);
	}

	/**
	 * {@return the entity using the UUID, or {@code null} if none was found}
	 * 
	 * @see World#getEntityById
	 */
	@Nullable
	public Entity getEntity(UUID uuid) {
		return this.getEntityLookup().get(uuid);
	}

	/**
	 * Tries to find the closest structure of a given type near a given block.
	 * <p>
	 * New chunks will only be generated up to the {@link net.minecraft.world.chunk.ChunkStatus#STRUCTURE_STARTS} phase by this method.
	 * <p>
	 * The radius is ignored for strongholds.
	 * 
	 * @return the position of the structure, or {@code null} if no structure could be found within the given search radius
	 * 
	 * @see net.minecraft.world.gen.chunk.ChunkGenerator#locateStructure(ServerWorld, RegistryEntryList, BlockPos, int, boolean)
	 * 
	 * @param pos the position to start the searching at
	 * @param skipReferencedStructures whether to exclude structures that were previously located (has positive
	 * {@link net.minecraft.structure.StructureStart#references})
	 * @param radius the search radius in chunks around the chunk the given block position is in; a radius of 0 will only search in the given chunk
	 */
	@Nullable
	public BlockPos locateStructure(TagKey<Structure> structureTag, BlockPos pos, int radius, boolean skipReferencedStructures) {
		if (!this.server.getSaveProperties().getGeneratorOptions().shouldGenerateStructures()) {
			return null;
		} else {
			Optional<RegistryEntryList.Named<Structure>> optional = this.getRegistryManager().get(RegistryKeys.STRUCTURE).getEntryList(structureTag);
			if (optional.isEmpty()) {
				return null;
			} else {
				Pair<BlockPos, RegistryEntry<Structure>> pair = this.getChunkManager()
					.getChunkGenerator()
					.locateStructure(this, (RegistryEntryList<Structure>)optional.get(), pos, radius, skipReferencedStructures);
				return pair != null ? pair.getFirst() : null;
			}
		}
	}

	@Nullable
	public Pair<BlockPos, RegistryEntry<Biome>> locateBiome(
		Predicate<RegistryEntry<Biome>> predicate, BlockPos pos, int radius, int horizontalBlockCheckInterval, int verticalBlockCheckInterval
	) {
		return this.getChunkManager()
			.getChunkGenerator()
			.getBiomeSource()
			.locateBiome(
				pos, radius, horizontalBlockCheckInterval, verticalBlockCheckInterval, predicate, this.getChunkManager().getNoiseConfig().getMultiNoiseSampler(), this
			);
	}

	@Override
	public RecipeManager getRecipeManager() {
		return this.server.getRecipeManager();
	}

	@Override
	public boolean isSavingDisabled() {
		return this.savingDisabled;
	}

	public PersistentStateManager getPersistentStateManager() {
		return this.getChunkManager().getPersistentStateManager();
	}

	@Nullable
	@Override
	public MapState getMapState(String id) {
		return this.getServer().getOverworld().getPersistentStateManager().get(MapState::fromNbt, id);
	}

	@Override
	public void putMapState(String id, MapState state) {
		this.getServer().getOverworld().getPersistentStateManager().set(id, state);
	}

	@Override
	public int getNextMapId() {
		return this.getServer()
			.getOverworld()
			.getPersistentStateManager()
			.<IdCountsState>getOrCreate(IdCountsState::fromNbt, IdCountsState::new, "idcounts")
			.getNextMapId();
	}

	/**
	 * Sets the world spawn point.
	 * 
	 * @param angle the angle of the spawned entity
	 * @param pos the position of the spawn point
	 */
	public void setSpawnPos(BlockPos pos, float angle) {
		ChunkPos chunkPos = new ChunkPos(new BlockPos(this.properties.getSpawnX(), 0, this.properties.getSpawnZ()));
		this.properties.setSpawnPos(pos, angle);
		this.getChunkManager().removeTicket(ChunkTicketType.START, chunkPos, 11, Unit.INSTANCE);
		this.getChunkManager().addTicket(ChunkTicketType.START, new ChunkPos(pos), 11, Unit.INSTANCE);
		this.getServer().getPlayerManager().sendToAll(new PlayerSpawnPositionS2CPacket(pos, angle));
	}

	/**
	 * {@return the set that contains {@link ChunkPos} of forced chunks serialized as a long}
	 */
	public LongSet getForcedChunks() {
		ForcedChunkState forcedChunkState = this.getPersistentStateManager().get(ForcedChunkState::fromNbt, "chunks");
		return (LongSet)(forcedChunkState != null ? LongSets.unmodifiable(forcedChunkState.getChunks()) : LongSets.EMPTY_SET);
	}

	/**
	 * Sets the forced status of the chunk.
	 * 
	 * <p>Forced chunks are created in-game using the
	 * {@linkplain net.minecraft.server.command.ForceLoadCommand {@code /forceload} command}.
	 * 
	 * @param forced whether to mark the chunk as forced
	 * @param z the chunk's Z coordinate
	 * @param x the chunk's X coordinate
	 */
	public boolean setChunkForced(int x, int z, boolean forced) {
		ForcedChunkState forcedChunkState = this.getPersistentStateManager().getOrCreate(ForcedChunkState::fromNbt, ForcedChunkState::new, "chunks");
		ChunkPos chunkPos = new ChunkPos(x, z);
		long l = chunkPos.toLong();
		boolean bl;
		if (forced) {
			bl = forcedChunkState.getChunks().add(l);
			if (bl) {
				this.getChunk(x, z);
			}
		} else {
			bl = forcedChunkState.getChunks().remove(l);
		}

		forcedChunkState.setDirty(bl);
		if (bl) {
			this.getChunkManager().setChunkForced(chunkPos, forced);
		}

		return bl;
	}

	@Override
	public List<ServerPlayerEntity> getPlayers() {
		return this.players;
	}

	@Override
	public void onBlockChanged(BlockPos pos, BlockState oldBlock, BlockState newBlock) {
		Optional<RegistryEntry<PointOfInterestType>> optional = PointOfInterestTypes.getTypeForState(oldBlock);
		Optional<RegistryEntry<PointOfInterestType>> optional2 = PointOfInterestTypes.getTypeForState(newBlock);
		if (!Objects.equals(optional, optional2)) {
			BlockPos blockPos = pos.toImmutable();
			optional.ifPresent(oldPoiType -> this.getServer().execute(() -> {
					this.getPointOfInterestStorage().remove(blockPos);
					DebugInfoSender.sendPoiRemoval(this, blockPos);
				}));
			optional2.ifPresent(newPoiType -> this.getServer().execute(() -> {
					this.getPointOfInterestStorage().add(blockPos, newPoiType);
					DebugInfoSender.sendPoiAddition(this, blockPos);
				}));
		}
	}

	public PointOfInterestStorage getPointOfInterestStorage() {
		return this.getChunkManager().getPointOfInterestStorage();
	}

	public boolean isNearOccupiedPointOfInterest(BlockPos pos) {
		return this.isNearOccupiedPointOfInterest(pos, 1);
	}

	public boolean isNearOccupiedPointOfInterest(ChunkSectionPos sectionPos) {
		return this.isNearOccupiedPointOfInterest(sectionPos.getCenterPos());
	}

	public boolean isNearOccupiedPointOfInterest(BlockPos pos, int maxDistance) {
		return maxDistance > 6 ? false : this.getOccupiedPointOfInterestDistance(ChunkSectionPos.from(pos)) <= maxDistance;
	}

	public int getOccupiedPointOfInterestDistance(ChunkSectionPos pos) {
		return this.getPointOfInterestStorage().getDistanceFromNearestOccupied(pos);
	}

	public RaidManager getRaidManager() {
		return this.raidManager;
	}

	/**
	 * {@return the raid occurring within 96 block radius, or {@code null} if there is none}
	 */
	@Nullable
	public Raid getRaidAt(BlockPos pos) {
		return this.raidManager.getRaidAt(pos, 9216);
	}

	/**
	 * {@return {@code true} if a raid exists within 96 block radius of {@code pos}}
	 */
	public boolean hasRaidAt(BlockPos pos) {
		return this.getRaidAt(pos) != null;
	}

	public void handleInteraction(EntityInteraction interaction, Entity entity, InteractionObserver observer) {
		observer.onInteractionWith(interaction, entity);
	}

	public void dump(Path path) throws IOException {
		ThreadedAnvilChunkStorage threadedAnvilChunkStorage = this.getChunkManager().threadedAnvilChunkStorage;
		Writer writer = Files.newBufferedWriter(path.resolve("stats.txt"));

		try {
			writer.write(String.format(Locale.ROOT, "spawning_chunks: %d\n", threadedAnvilChunkStorage.getTicketManager().getTickedChunkCount()));
			SpawnHelper.Info info = this.getChunkManager().getSpawnInfo();
			if (info != null) {
				for (Entry<SpawnGroup> entry : info.getGroupToCount().object2IntEntrySet()) {
					writer.write(String.format(Locale.ROOT, "spawn_count.%s: %d\n", ((SpawnGroup)entry.getKey()).getName(), entry.getIntValue()));
				}
			}

			writer.write(String.format(Locale.ROOT, "entities: %s\n", this.entityManager.getDebugString()));
			writer.write(String.format(Locale.ROOT, "block_entity_tickers: %d\n", this.blockEntityTickers.size()));
			writer.write(String.format(Locale.ROOT, "block_ticks: %d\n", this.getBlockTickScheduler().getTickCount()));
			writer.write(String.format(Locale.ROOT, "fluid_ticks: %d\n", this.getFluidTickScheduler().getTickCount()));
			writer.write("distance_manager: " + threadedAnvilChunkStorage.getTicketManager().toDumpString() + "\n");
			writer.write(String.format(Locale.ROOT, "pending_tasks: %d\n", this.getChunkManager().getPendingTasks()));
		} catch (Throwable var22) {
			if (writer != null) {
				try {
					writer.close();
				} catch (Throwable var16) {
					var22.addSuppressed(var16);
				}
			}

			throw var22;
		}

		if (writer != null) {
			writer.close();
		}

		CrashReport crashReport = new CrashReport("Level dump", new Exception("dummy"));
		this.addDetailsToCrashReport(crashReport);
		Writer writer2 = Files.newBufferedWriter(path.resolve("example_crash.txt"));

		try {
			writer2.write(crashReport.asString());
		} catch (Throwable var21) {
			if (writer2 != null) {
				try {
					writer2.close();
				} catch (Throwable var15) {
					var21.addSuppressed(var15);
				}
			}

			throw var21;
		}

		if (writer2 != null) {
			writer2.close();
		}

		Path path2 = path.resolve("chunks.csv");
		Writer writer3 = Files.newBufferedWriter(path2);

		try {
			threadedAnvilChunkStorage.dump(writer3);
		} catch (Throwable var20) {
			if (writer3 != null) {
				try {
					writer3.close();
				} catch (Throwable var14) {
					var20.addSuppressed(var14);
				}
			}

			throw var20;
		}

		if (writer3 != null) {
			writer3.close();
		}

		Path path3 = path.resolve("entity_chunks.csv");
		Writer writer4 = Files.newBufferedWriter(path3);

		try {
			this.entityManager.dump(writer4);
		} catch (Throwable var19) {
			if (writer4 != null) {
				try {
					writer4.close();
				} catch (Throwable var13) {
					var19.addSuppressed(var13);
				}
			}

			throw var19;
		}

		if (writer4 != null) {
			writer4.close();
		}

		Path path4 = path.resolve("entities.csv");
		Writer writer5 = Files.newBufferedWriter(path4);

		try {
			dumpEntities(writer5, this.getEntityLookup().iterate());
		} catch (Throwable var18) {
			if (writer5 != null) {
				try {
					writer5.close();
				} catch (Throwable var12) {
					var18.addSuppressed(var12);
				}
			}

			throw var18;
		}

		if (writer5 != null) {
			writer5.close();
		}

		Path path5 = path.resolve("block_entities.csv");
		Writer writer6 = Files.newBufferedWriter(path5);

		try {
			this.dumpBlockEntities(writer6);
		} catch (Throwable var17) {
			if (writer6 != null) {
				try {
					writer6.close();
				} catch (Throwable var11) {
					var17.addSuppressed(var11);
				}
			}

			throw var17;
		}

		if (writer6 != null) {
			writer6.close();
		}
	}

	private static void dumpEntities(Writer writer, Iterable<Entity> entities) throws IOException {
		CsvWriter csvWriter = CsvWriter.makeHeader()
			.addColumn("x")
			.addColumn("y")
			.addColumn("z")
			.addColumn("uuid")
			.addColumn("type")
			.addColumn("alive")
			.addColumn("display_name")
			.addColumn("custom_name")
			.startBody(writer);

		for (Entity entity : entities) {
			Text text = entity.getCustomName();
			Text text2 = entity.getDisplayName();
			csvWriter.printRow(
				entity.getX(),
				entity.getY(),
				entity.getZ(),
				entity.getUuid(),
				Registries.ENTITY_TYPE.getId(entity.getType()),
				entity.isAlive(),
				text2.getString(),
				text != null ? text.getString() : null
			);
		}
	}

	private void dumpBlockEntities(Writer writer) throws IOException {
		CsvWriter csvWriter = CsvWriter.makeHeader().addColumn("x").addColumn("y").addColumn("z").addColumn("type").startBody(writer);

		for (BlockEntityTickInvoker blockEntityTickInvoker : this.blockEntityTickers) {
			BlockPos blockPos = blockEntityTickInvoker.getPos();
			csvWriter.printRow(blockPos.getX(), blockPos.getY(), blockPos.getZ(), blockEntityTickInvoker.getName());
		}
	}

	@VisibleForTesting
	public void clearUpdatesInArea(BlockBox box) {
		this.syncedBlockEventQueue.removeIf(event -> box.contains(event.pos()));
	}

	@Override
	public void updateNeighbors(BlockPos pos, Block block) {
		if (!this.isDebugWorld()) {
			this.updateNeighborsAlways(pos, block);
		}
	}

	@Override
	public float getBrightness(Direction direction, boolean shaded) {
		return 1.0F;
	}

	public Iterable<Entity> iterateEntities() {
		return this.getEntityLookup().iterate();
	}

	public String toString() {
		return "ServerLevel[" + this.worldProperties.getLevelName() + "]";
	}

	public boolean isFlat() {
		return this.server.getSaveProperties().isFlatWorld();
	}

	@Override
	public long getSeed() {
		return this.server.getSaveProperties().getGeneratorOptions().getSeed();
	}

	@Nullable
	public EnderDragonFight getEnderDragonFight() {
		return this.enderDragonFight;
	}

	@Override
	public ServerWorld toServerWorld() {
		return this;
	}

	@VisibleForTesting
	public String getDebugString() {
		return String.format(
			Locale.ROOT,
			"players: %s, entities: %s [%s], block_entities: %d [%s], block_ticks: %d, fluid_ticks: %d, chunk_source: %s",
			this.players.size(),
			this.entityManager.getDebugString(),
			getTopFive(this.entityManager.getLookup().iterate(), entity -> Registries.ENTITY_TYPE.getId(entity.getType()).toString()),
			this.blockEntityTickers.size(),
			getTopFive(this.blockEntityTickers, BlockEntityTickInvoker::getName),
			this.getBlockTickScheduler().getTickCount(),
			this.getFluidTickScheduler().getTickCount(),
			this.asString()
		);
	}

	/**
	 * Categories {@code items} with the {@code classifier} and reports a message
	 * indicating the top five biggest categories.
	 * 
	 * @param items the items to classify
	 * @param classifier the classifier that determines the category of any item
	 */
	private static <T> String getTopFive(Iterable<T> items, Function<T, String> classifier) {
		try {
			Object2IntOpenHashMap<String> object2IntOpenHashMap = new Object2IntOpenHashMap<>();

			for (T object : items) {
				String string = (String)classifier.apply(object);
				object2IntOpenHashMap.addTo(string, 1);
			}

			return (String)object2IntOpenHashMap.object2IntEntrySet()
				.stream()
				.sorted(Comparator.comparing(Entry::getIntValue).reversed())
				.limit(5L)
				.map(entry -> (String)entry.getKey() + ":" + entry.getIntValue())
				.collect(Collectors.joining(","));
		} catch (Exception var6) {
			return "";
		}
	}

	public static void createEndSpawnPlatform(ServerWorld world) {
		BlockPos blockPos = END_SPAWN_POS;
		int i = blockPos.getX();
		int j = blockPos.getY() - 2;
		int k = blockPos.getZ();
		BlockPos.iterate(i - 2, j + 1, k - 2, i + 2, j + 3, k + 2).forEach(pos -> world.setBlockState(pos, Blocks.AIR.getDefaultState()));
		BlockPos.iterate(i - 2, j, k - 2, i + 2, j, k + 2).forEach(pos -> world.setBlockState(pos, Blocks.OBSIDIAN.getDefaultState()));
	}

	@Override
	protected EntityLookup<Entity> getEntityLookup() {
		return this.entityManager.getLookup();
	}

	public void loadEntities(Stream<Entity> entities) {
		this.entityManager.loadEntities(entities);
	}

	public void addEntities(Stream<Entity> entities) {
		this.entityManager.addEntities(entities);
	}

	public void disableTickSchedulers(WorldChunk chunk) {
		chunk.disableTickSchedulers(this.getLevelProperties().getTime());
	}

	public void cacheStructures(Chunk chunk) {
		this.server.execute(() -> this.structureLocator.cache(chunk.getPos(), chunk.getStructureStarts()));
	}

	@Override
	public void close() throws IOException {
		super.close();
		this.entityManager.close();
	}

	@Override
	public String asString() {
		return "Chunks[S] W: " + this.chunkManager.getDebugString() + " E: " + this.entityManager.getDebugString();
	}

	/**
	 * {@return {@code true} if the chunk {@code chunkPos} is loaded}
	 */
	public boolean isChunkLoaded(long chunkPos) {
		return this.entityManager.isLoaded(chunkPos);
	}

	private boolean isTickingFutureReady(long chunkPos) {
		return this.isChunkLoaded(chunkPos) && this.chunkManager.isTickingFutureReady(chunkPos);
	}

	/**
	 * {@return whether to tick entities at {@code pos}}
	 */
	public boolean shouldTickEntity(BlockPos pos) {
		return this.entityManager.shouldTick(pos) && this.chunkManager.threadedAnvilChunkStorage.getTicketManager().shouldTickEntities(ChunkPos.toLong(pos));
	}

	public boolean shouldTick(BlockPos pos) {
		return this.entityManager.shouldTick(pos);
	}

	public boolean shouldTick(ChunkPos pos) {
		return this.entityManager.shouldTick(pos);
	}

	@Override
	public FeatureSet getEnabledFeatures() {
		return this.server.getSaveProperties().getEnabledFeatures();
	}

	public Random getOrCreateRandom(Identifier id) {
		return this.randomSequences.getOrCreate(id);
	}

	public RandomSequencesState getRandomSequences() {
		return this.randomSequences;
	}

	final class ServerEntityHandler implements EntityHandler<Entity> {
		public void create(Entity entity) {
		}

		public void destroy(Entity entity) {
			ServerWorld.this.getScoreboard().resetEntityScore(entity);
		}

		public void startTicking(Entity entity) {
			ServerWorld.this.entityList.add(entity);
		}

		public void stopTicking(Entity entity) {
			ServerWorld.this.entityList.remove(entity);
		}

		public void startTracking(Entity entity) {
			ServerWorld.this.getChunkManager().loadEntity(entity);
			if (entity instanceof ServerPlayerEntity serverPlayerEntity) {
				ServerWorld.this.players.add(serverPlayerEntity);
				ServerWorld.this.updateSleepingPlayers();
			}

			if (entity instanceof MobEntity mobEntity) {
				if (ServerWorld.this.duringListenerUpdate) {
					String string = "onTrackingStart called during navigation iteration";
					Util.error("onTrackingStart called during navigation iteration", new IllegalStateException("onTrackingStart called during navigation iteration"));
				}

				ServerWorld.this.loadedMobs.add(mobEntity);
			}

			if (entity instanceof EnderDragonEntity enderDragonEntity) {
				for (EnderDragonPart enderDragonPart : enderDragonEntity.getBodyParts()) {
					ServerWorld.this.dragonParts.put(enderDragonPart.getId(), enderDragonPart);
				}
			}

			entity.updateEventHandler(EntityGameEventHandler::onEntitySetPosCallback);
		}

		public void stopTracking(Entity entity) {
			ServerWorld.this.getChunkManager().unloadEntity(entity);
			if (entity instanceof ServerPlayerEntity serverPlayerEntity) {
				ServerWorld.this.players.remove(serverPlayerEntity);
				ServerWorld.this.updateSleepingPlayers();
			}

			if (entity instanceof MobEntity mobEntity) {
				if (ServerWorld.this.duringListenerUpdate) {
					String string = "onTrackingStart called during navigation iteration";
					Util.error("onTrackingStart called during navigation iteration", new IllegalStateException("onTrackingStart called during navigation iteration"));
				}

				ServerWorld.this.loadedMobs.remove(mobEntity);
			}

			if (entity instanceof EnderDragonEntity enderDragonEntity) {
				for (EnderDragonPart enderDragonPart : enderDragonEntity.getBodyParts()) {
					ServerWorld.this.dragonParts.remove(enderDragonPart.getId());
				}
			}

			entity.updateEventHandler(EntityGameEventHandler::onEntityRemoval);
		}

		public void updateLoadStatus(Entity entity) {
			entity.updateEventHandler(EntityGameEventHandler::onEntitySetPos);
		}
	}
}
