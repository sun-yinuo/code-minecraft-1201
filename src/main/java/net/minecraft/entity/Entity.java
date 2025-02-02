package net.minecraft.entity;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import com.google.common.collect.ImmutableList.Builder;
import com.mojang.logging.LogUtils;
import it.unimi.dsi.fastutil.objects.Object2DoubleArrayMap;
import it.unimi.dsi.fastutil.objects.Object2DoubleMap;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.BiConsumer;
import java.util.function.Predicate;
import java.util.stream.Stream;
import net.fabricmc.fabric.api.attachment.v1.AttachmentTarget;
import net.minecraft.advancement.criterion.Criteria;
import net.minecraft.block.Block;
import net.minecraft.block.BlockRenderType;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.FenceGateBlock;
import net.minecraft.block.HoneyBlock;
import net.minecraft.block.ShapeContext;
import net.minecraft.block.piston.PistonBehavior;
import net.minecraft.command.argument.EntityAnchorArgumentType;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.enchantment.ProtectionEnchantment;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.damage.DamageSources;
import net.minecraft.entity.data.DataTracker;
import net.minecraft.entity.data.TrackedData;
import net.minecraft.entity.data.TrackedDataHandlerRegistry;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.vehicle.BoatEntity;
import net.minecraft.fluid.Fluid;
import net.minecraft.fluid.FluidState;
import net.minecraft.inventory.StackReference;
import net.minecraft.item.Item;
import net.minecraft.item.ItemConvertible;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtDouble;
import net.minecraft.nbt.NbtElement;
import net.minecraft.nbt.NbtFloat;
import net.minecraft.nbt.NbtList;
import net.minecraft.nbt.NbtString;
import net.minecraft.network.listener.ClientPlayPacketListener;
import net.minecraft.network.packet.Packet;
import net.minecraft.network.packet.s2c.play.EntitySpawnS2CPacket;
import net.minecraft.network.packet.s2c.play.PositionFlag;
import net.minecraft.particle.BlockStateParticleEffect;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.tag.BlockTags;
import net.minecraft.registry.tag.DamageTypeTags;
import net.minecraft.registry.tag.EntityTypeTags;
import net.minecraft.registry.tag.FluidTags;
import net.minecraft.registry.tag.TagKey;
import net.minecraft.scoreboard.AbstractTeam;
import net.minecraft.scoreboard.Team;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.command.CommandOutput;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ChunkTicketType;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.BlockSoundGroup;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvent;
import net.minecraft.sound.SoundEvents;
import net.minecraft.state.property.Properties;
import net.minecraft.text.HoverEvent;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Arm;
import net.minecraft.util.BlockMirror;
import net.minecraft.util.BlockRotation;
import net.minecraft.util.Hand;
import net.minecraft.util.Identifier;
import net.minecraft.util.Nameable;
import net.minecraft.util.Util;
import net.minecraft.util.crash.CrashCallable;
import net.minecraft.util.crash.CrashException;
import net.minecraft.util.crash.CrashReport;
import net.minecraft.util.crash.CrashReportSection;
import net.minecraft.util.function.BooleanBiFunction;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.util.math.ChunkSectionPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec2f;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.math.random.Random;
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.util.shape.VoxelShapes;
import net.minecraft.world.BlockLocating;
import net.minecraft.world.BlockView;
import net.minecraft.world.GameRules;
import net.minecraft.world.Heightmap;
import net.minecraft.world.RaycastContext;
import net.minecraft.world.TeleportTarget;
import net.minecraft.world.World;
import net.minecraft.world.WorldEvents;
import net.minecraft.world.border.WorldBorder;
import net.minecraft.world.dimension.DimensionType;
import net.minecraft.world.dimension.NetherPortal;
import net.minecraft.world.entity.EntityChangeListener;
import net.minecraft.world.entity.EntityLike;
import net.minecraft.world.event.GameEvent;
import net.minecraft.world.event.listener.EntityGameEventHandler;
import net.minecraft.world.explosion.Explosion;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;

/**
 * An object that exists in a world and has {@code double}-precision position.
 * They are registered in {@link EntityType}.
 * 
 * <p>Examples of entities include players, mobs, minecarts, projectiles, and
 * dropped items.
 * 
 * <p>Entity can be identified by the {@link #id ID} or the {@link #uuid UUID}.
 * Entity ID is an integer used in networking, and is not saved on disk. UUID is
 * used to identify an entity in NBT and other places where persistence is required.
 * 
 * <h2 id="spawning">Creating and spawning entities</h2>
 * Entities must be created first, which then can be added to a world ("spawning").
 * There are multiple methods of doing this, shown on the table below:
 * 
 * <div class="fabric">
 * <table border=1>
 * <caption>Creation &amp; Spawning (at once)</caption>
 * <tr>
 * 	<th>Method</th><th>Recommended usage</th><th>Additional note</th>
 * </tr>
 * <tr>
 * 	<td>{@link EntityType#spawn}</td><td>Any entity</td><td>Initializes mobs.</td>
 * </tr>
 * <tr>
 * 	<td>{@link EntityType#spawnFromItemStack}</td><td>Entities in items (such as buckets)</td><td>Initializes mobs.</td>
 * </tr>
 * <tr>
 * 	<td>{@link ExperienceOrbEntity#spawn}</td><td>Experience orbs with set amount</td><td>Can spawn multiple orbs.</td>
 * </tr>
 * <tr>
 * 	<td>{@link net.minecraft.util.ItemScatterer} methods</td><td>Items</td><td>Will spawn with random velocity.</td>
 * </tr>
 * </table>
 * 
 * <table border=1>
 * <caption>Creation only</caption>
 * <tr>
 * 	<th>Method</th><th>Recommended usage</th><th>Additional note</th>
 * </tr>
 * <tr>
 * 	<td>Subclass constructors</td><td>Non-mob entities (such as projectiles)</td><td>NBT and other data must be set manually.</td>
 * </tr>
 * <tr>
 * 	<td>{@link EntityType#create}</td><td>Any entity</td><td>Initializes mobs and supports custom NBT.</td>
 * </tr>
 * <tr>
 * 	<td>{@link EntityType#getEntityFromNbt}</td><td>Entities stored in NBT</td><td>Can throw exceptions.</td>
 * </tr>
 * <tr>
 * 	<td>{@link EntityType#loadEntityFromNbt}</td><td>Entities stored in user-provided NBT</td><td>Ignores exceptions.</td>
 * </tr>
 * <tr>
 * 	<td>{@link EntityType#loadEntityWithPassengers}</td><td>Entities with passengers stored in user-provided NBT</td><td>Ignores exceptions. Initializes rides.</td>
 * </tr>
 * <tr>
 * 	<td>{@link EntityType#streamFromNbt}</td><td>Entities with passengers stored in NBT</td><td>Ignores exceptions.</td>
 * </tr>
 * </table>
 * 
 * <table border=1>
 * <caption>Spawning only</caption>
 * <tr>
 * 	<th>Method</th><th>Recommended usage</th><th>Additional note</th>
 * </tr>
 * <tr>
 * 	<td>{@link net.minecraft.world.ServerWorldAccess#spawnEntityAndPassengers}</td><td>Any entity</td><td>Does not check duplicate UUID.</td>
 * </tr>
 * <tr>
 * 	<td>{@link ServerWorld#spawnNewEntityAndPassengers}</td><td>Any entity</td><td>Checks duplicate UUID.</td>
 * </tr>
 * <tr>
 * 	<td>{@link net.minecraft.world.ModifiableWorld#spawnEntity}</td><td>Any entity</td><td>Does not spawn passengers.</td>
 * </tr>
 * </table>
 * </div>
 * 
 * <p><strong>Warning</strong>: When using constructors to spawn mobs instead of
 * {@link EntityType#create}, they must be manually
 * {@link net.minecraft.entity.mob.MobEntity#initialize initialized} before spawning.
 * 
 * <h2 id="discarding">Discarding</h2>
 * Entities can be discarded (despawned) by calling {@link #discard}. This does not drop loot.
 * To kill entities and drop loot, call {@link #kill} or {@link damage} (with large enough damage amount).
 */
public abstract class Entity implements Nameable, EntityLike, CommandOutput, AttachmentTarget {
	private static final Logger LOGGER = LogUtils.getLogger();
	public static final String ID_KEY = "id";
	public static final String PASSENGERS_KEY = "Passengers";
	/**
	 * A generator of unique entity {@link #id network IDs}. The generated
	 * ID for client entities are useless and discarded subsequently through
	 * {@link #setId(int)} calls.
	 */
	private static final AtomicInteger CURRENT_ID = new AtomicInteger();
	private static final List<ItemStack> EMPTY_STACK_LIST = Collections.emptyList();
	/**
	 * @see Entity#removePassenger
	 */
	public static final int MAX_RIDING_COOLDOWN = 60;
	/**
	 * @see Entity#getDefaultPortalCooldown
	 */
	public static final int DEFAULT_PORTAL_COOLDOWN = 300;
	/**
	 * @see Entity#addCommandTag
	 * @see Entity#readNbt
	 */
	public static final int MAX_COMMAND_TAGS = 1024;
	public static final float field_44870 = 0.2F;
	public static final double field_44871 = 0.500001;
	public static final double field_44872 = 0.999999;
	public static final float field_29991 = 0.11111111F;
	/**
	 * @see Entity#getMinFreezeDamageTicks
	 */
	public static final int DEFAULT_MIN_FREEZE_DAMAGE_TICKS = 140;
	/**
	 * @see LivingEntity#tickMovement
	 */
	public static final int FREEZING_DAMAGE_INTERVAL = 40;
	private static final Box NULL_BOX = new Box(0.0, 0.0, 0.0, 0.0, 0.0, 0.0);
	private static final double SPEED_IN_WATER = 0.014;
	private static final double SPEED_IN_LAVA_IN_NETHER = 0.007;
	private static final double SPEED_IN_LAVA = 0.0023333333333333335;
	public static final String UUID_KEY = "UUID";
	private static double renderDistanceMultiplier = 1.0;
	private final EntityType<?> type;
	/**
	 * The entity's network ID, used as a reference for synchronization over network.
	 * This is not persistent across save and loads; use {@link #uuid} to identify
	 * an entity in those cases.
	 * 
	 * @see #getId()
	 */
	private int id = CURRENT_ID.incrementAndGet();
	/**
	 * Whether the entity should be included in intersection checks.
	 * 
	 * <p>An intersection check is used to prevent block placement or
	 * mob spawning within the bounding box.
	 * 
	 * @implNote Intersection is always checked for
	 * {@link net.minecraft.entity.vehicle.BoatEntity},
	 * {@link net.minecraft.entity.vehicle.AbstractMinecartEntity}, {@link TntEntity},
	 * {@link net.minecraft.entity.decoration.EndCrystalEntity},
	 * {@link FallingBlockEntity}, and {@link LivingEntity}.
	 * Intersection with {@link net.minecraft.entity.decoration.ArmorStandEntity} is checked if
	 * it is not a {@link net.minecraft.entity.decoration.ArmorStandEntity#isMarker marker}.
	 */
	public boolean intersectionChecked;
	private ImmutableList<Entity> passengerList = ImmutableList.of();
	protected int ridingCooldown;
	@Nullable
	private Entity vehicle;
	private World world;
	public double prevX;
	public double prevY;
	public double prevZ;
	private Vec3d pos;
	private BlockPos blockPos;
	private ChunkPos chunkPos;
	private Vec3d velocity = Vec3d.ZERO;
	private float yaw;
	private float pitch;
	public float prevYaw;
	public float prevPitch;
	private Box boundingBox = NULL_BOX;
	private boolean onGround;
	public boolean horizontalCollision;
	public boolean verticalCollision;
	public boolean groundCollision;
	public boolean collidedSoftly;
	public boolean velocityModified;
	protected Vec3d movementMultiplier = Vec3d.ZERO;
	@Nullable
	private Entity.RemovalReason removalReason;
	/**
	 * The factor by which an entity's speed is reduced every tick.
	 * <p>
	 * For example: {@code horizontalSpeed = velocity.horizontalSpeed() * FRICTION_RATE}
	 */
	public static final float DEFAULT_FRICTION = 0.6F;
	public static final float MIN_RISING_BUBBLE_COLUMN_SPEED = 1.8F;
	public float prevHorizontalSpeed;
	public float horizontalSpeed;
	public float distanceTraveled;
	public float speed;
	public float fallDistance;
	private float nextStepSoundDistance = 1.0F;
	public double lastRenderX;
	public double lastRenderY;
	public double lastRenderZ;
	private float stepHeight;
	public boolean noClip;
	protected final Random random = Random.create();
	public int age;
	private int fireTicks = -this.getBurningDuration();
	protected boolean touchingWater;
	protected Object2DoubleMap<TagKey<Fluid>> fluidHeight = new Object2DoubleArrayMap<>(2);
	protected boolean submergedInWater;
	private final Set<TagKey<Fluid>> submergedFluidTag = new HashSet();
	public int timeUntilRegen;
	protected boolean firstUpdate = true;
	protected final DataTracker dataTracker;
	protected static final TrackedData<Byte> FLAGS = DataTracker.registerData(Entity.class, TrackedDataHandlerRegistry.BYTE);
	protected static final int ON_FIRE_FLAG_INDEX = 0;
	private static final int SNEAKING_FLAG_INDEX = 1;
	private static final int SPRINTING_FLAG_INDEX = 3;
	private static final int SWIMMING_FLAG_INDEX = 4;
	private static final int INVISIBLE_FLAG_INDEX = 5;
	protected static final int GLOWING_FLAG_INDEX = 6;
	protected static final int FALL_FLYING_FLAG_INDEX = 7;
	private static final TrackedData<Integer> AIR = DataTracker.registerData(Entity.class, TrackedDataHandlerRegistry.INTEGER);
	private static final TrackedData<Optional<Text>> CUSTOM_NAME = DataTracker.registerData(Entity.class, TrackedDataHandlerRegistry.OPTIONAL_TEXT_COMPONENT);
	private static final TrackedData<Boolean> NAME_VISIBLE = DataTracker.registerData(Entity.class, TrackedDataHandlerRegistry.BOOLEAN);
	private static final TrackedData<Boolean> SILENT = DataTracker.registerData(Entity.class, TrackedDataHandlerRegistry.BOOLEAN);
	private static final TrackedData<Boolean> NO_GRAVITY = DataTracker.registerData(Entity.class, TrackedDataHandlerRegistry.BOOLEAN);
	protected static final TrackedData<EntityPose> POSE = DataTracker.registerData(Entity.class, TrackedDataHandlerRegistry.ENTITY_POSE);
	private static final TrackedData<Integer> FROZEN_TICKS = DataTracker.registerData(Entity.class, TrackedDataHandlerRegistry.INTEGER);
	private EntityChangeListener changeListener = EntityChangeListener.NONE;
	private final TrackedPosition trackedPosition = new TrackedPosition();
	public boolean ignoreCameraFrustum;
	public boolean velocityDirty;
	private int portalCooldown;
	protected boolean inNetherPortal;
	protected int netherPortalTime;
	protected BlockPos lastNetherPortalPosition;
	private boolean invulnerable;
	protected UUID uuid = MathHelper.randomUuid(this.random);
	protected String uuidString = this.uuid.toString();
	private boolean glowing;
	private final Set<String> commandTags = Sets.<String>newHashSet();
	private final double[] pistonMovementDelta = new double[]{0.0, 0.0, 0.0};
	private long pistonMovementTick;
	private EntityDimensions dimensions;
	private float standingEyeHeight;
	public boolean inPowderSnow;
	public boolean wasInPowderSnow;
	public boolean wasOnFire;
	public Optional<BlockPos> supportingBlockPos = Optional.empty();
	private boolean forceUpdateSupportingBlockPos = false;
	private float lastChimeIntensity;
	private int lastChimeAge;
	private boolean hasVisualFire;
	@Nullable
	private BlockState blockStateAtPos = null;

	public Entity(EntityType<?> type, World world) {
		this.type = type;
		this.world = world;
		this.dimensions = type.getDimensions();
		this.pos = Vec3d.ZERO;
		this.blockPos = BlockPos.ORIGIN;
		this.chunkPos = ChunkPos.ORIGIN;
		this.dataTracker = new DataTracker(this);
		this.dataTracker.startTracking(FLAGS, (byte)0);
		this.dataTracker.startTracking(AIR, this.getMaxAir());
		this.dataTracker.startTracking(NAME_VISIBLE, false);
		this.dataTracker.startTracking(CUSTOM_NAME, Optional.empty());
		this.dataTracker.startTracking(SILENT, false);
		this.dataTracker.startTracking(NO_GRAVITY, false);
		this.dataTracker.startTracking(POSE, EntityPose.STANDING);
		this.dataTracker.startTracking(FROZEN_TICKS, 0);
		this.initDataTracker();
		this.setPosition(0.0, 0.0, 0.0);
		this.standingEyeHeight = this.getEyeHeight(EntityPose.STANDING, this.dimensions);
	}

	/**
	 * {@return whether the entity collides with the block {@code state} at {@code pos}}
	 */
	public boolean collidesWithStateAtPos(BlockPos pos, BlockState state) {
		VoxelShape voxelShape = state.getCollisionShape(this.getWorld(), pos, ShapeContext.of(this));
		VoxelShape voxelShape2 = voxelShape.offset((double)pos.getX(), (double)pos.getY(), (double)pos.getZ());
		return VoxelShapes.matchesAnywhere(voxelShape2, VoxelShapes.cuboid(this.getBoundingBox()), BooleanBiFunction.AND);
	}

	/**
	 * {@return the team color value, or {@code 0xFFFFFF} if the entity is not in
	 * a team or the color is not set}
	 */
	public int getTeamColorValue() {
		AbstractTeam abstractTeam = this.getScoreboardTeam();
		return abstractTeam != null && abstractTeam.getColor().getColorValue() != null ? abstractTeam.getColor().getColorValue() : 16777215;
	}

	/**
	 * {@return whether the entity is a spectator}
	 * 
	 * <p>This returns {@code false} unless the entity is a player in spectator game mode.
	 */
	public boolean isSpectator() {
		return false;
	}

	/**
	 * Removes all the passengers and removes this entity from any vehicles it is riding.
	 */
	public final void detach() {
		if (this.hasPassengers()) {
			this.removeAllPassengers();
		}

		if (this.hasVehicle()) {
			this.stopRiding();
		}
	}

	public void updateTrackedPosition(double x, double y, double z) {
		this.trackedPosition.setPos(new Vec3d(x, y, z));
	}

	public TrackedPosition getTrackedPosition() {
		return this.trackedPosition;
	}

	public EntityType<?> getType() {
		return this.type;
	}

	@Override
	public int getId() {
		return this.id;
	}

	/**
	 * Sets the network ID of this entity.
	 * 
	 * @apiNote This is used by client-side networking logic to set up the network
	 * ID of entities from the server. This shouldn't be used by server-side logic
	 * as the network ID is already properly initialized on entity object construction.
	 * 
	 * @see #getId()
	 */
	public void setId(int id) {
		this.id = id;
	}

	/**
	 * {@return all command tags the entity belongs to}
	 * 
	 * <p>Scoreboard tags are set using the {@linkplain net.minecraft.server.command.TagCommand
	 * /tag command}, and is different from entity type tags defined in data packs.
	 */
	public Set<String> getCommandTags() {
		return this.commandTags;
	}

	/**
	 * Adds a command tag to this entity. An entity can have up to {@code 1024}
	 * command tags.
	 * 
	 * <p>Command tags are set using the {@linkplain net.minecraft.server.command.TagCommand
	 * /tag command}, and is different from entity type tags defined in data packs.
	 * 
	 * @return whether the command tag was successfully added
	 */
	public boolean addCommandTag(String tag) {
		return this.commandTags.size() >= 1024 ? false : this.commandTags.add(tag);
	}

	/**
	 * Removes a command tag from this entity.
	 * 
	 * <p>Command tags are set using the {@linkplain net.minecraft.server.command.TagCommand
	 * /tag command}, and is different from entity type tags defined in data packs.
	 * 
	 * @return whether the command tag was successfully removed
	 */
	public boolean removeScoreboardTag(String tag) {
		return this.commandTags.remove(tag);
	}

	/**
	 * Kills the entity.
	 * 
	 * <p>This drops loot when applicable, and emits the {@link GameEvent#ENTITY_DIE} game event.
	 */
	public void kill() {
		this.remove(Entity.RemovalReason.KILLED);
		this.emitGameEvent(GameEvent.ENTITY_DIE);
	}

	/**
	 * Discards the entity. This is also referred to as "despawning".
	 * 
	 * <p>This does not cause the entity to drop loot.
	 */
	public final void discard() {
		this.remove(Entity.RemovalReason.DISCARDED);
	}

	/**
	 * Initializes data tracker.
	 * 
	 * @apiNote Subclasses should override this and call {@link DataTracker#startTracking}
	 * for any data that needs to be tracked.
	 */
	protected abstract void initDataTracker();

	public DataTracker getDataTracker() {
		return this.dataTracker;
	}

	public boolean equals(Object o) {
		return o instanceof Entity ? ((Entity)o).id == this.id : false;
	}

	public int hashCode() {
		return this.id;
	}

	/**
	 * Removes the entity.
	 * 
	 * @see #kill
	 * @see #discard
	 */
	public void remove(Entity.RemovalReason reason) {
		this.setRemoved(reason);
	}

	/**
	 * Called on the client side when the entity is removed.
	 * 
	 * @apiNote To handle entity removal server-side, override {@link #remove} and
	 * add custom logic there.
	 */
	public void onRemoved() {
	}

	public void setPose(EntityPose pose) {
		this.dataTracker.set(POSE, pose);
	}

	public EntityPose getPose() {
		return this.dataTracker.get(POSE);
	}

	public boolean isInPose(EntityPose pose) {
		return this.getPose() == pose;
	}

	/**
	 * {@return whether the distance between this entity and {@code entity} is below
	 * {@code radius}}
	 */
	public boolean isInRange(Entity entity, double radius) {
		return this.getPos().isInRange(entity.getPos(), radius);
	}

	/**
	 * {@return whether both the horizontal and vertical distances between this entity and
	 * {@code entity} are below the passed values}
	 */
	public boolean isInRange(Entity entity, double horizontalRadius, double verticalRadius) {
		double d = entity.getX() - this.getX();
		double e = entity.getY() - this.getY();
		double f = entity.getZ() - this.getZ();
		return MathHelper.squaredHypot(d, f) < MathHelper.square(horizontalRadius) && MathHelper.square(e) < MathHelper.square(verticalRadius);
	}

	/**
	 * Sets the entity's yaw and pitch.
	 */
	protected void setRotation(float yaw, float pitch) {
		this.setYaw(yaw % 360.0F);
		this.setPitch(pitch % 360.0F);
	}

	/**
	 * Sets the position and refreshes the bounding box.
	 * 
	 * <p>This should be called after creating an instance of non-living entities.
	 * For living entities, {@link #refreshPositionAndAngles} should be used instead.
	 * 
	 * @see #refreshPositionAndAngles
	 * @see #teleport(double, double, double)
	 */
	public final void setPosition(Vec3d pos) {
		this.setPosition(pos.getX(), pos.getY(), pos.getZ());
	}

	/**
	 * Sets the position and refreshes the bounding box.
	 * 
	 * <p>This should be called after creating an instance of non-living entities.
	 * For living entities, {@link #refreshPositionAndAngles} should be used instead.
	 * 
	 * @see #refreshPositionAndAngles
	 * @see #teleport(double, double, double)
	 */
	public void setPosition(double x, double y, double z) {
		this.setPos(x, y, z);
		this.setBoundingBox(this.calculateBoundingBox());
	}

	protected Box calculateBoundingBox() {
		return this.dimensions.getBoxAt(this.pos);
	}

	protected void refreshPosition() {
		this.setPosition(this.pos.x, this.pos.y, this.pos.z);
	}

	public void changeLookDirection(double cursorDeltaX, double cursorDeltaY) {
		float f = (float)cursorDeltaY * 0.15F;
		float g = (float)cursorDeltaX * 0.15F;
		this.setPitch(this.getPitch() + f);
		this.setYaw(this.getYaw() + g);
		this.setPitch(MathHelper.clamp(this.getPitch(), -90.0F, 90.0F));
		this.prevPitch += f;
		this.prevYaw += g;
		this.prevPitch = MathHelper.clamp(this.prevPitch, -90.0F, 90.0F);
		if (this.vehicle != null) {
			this.vehicle.onPassengerLookAround(this);
		}
	}

	/**
	 * Ticks this entity.
	 * 
	 * @apiNote This can be overridden to add additional logics. {@code super.tick();}
	 * should be called in those cases.
	 * 
	 * @implNote By default, this delegates all logics to {@link #baseTick}.
	 * 
	 * @see LivingEntity#tickMovement
	 * @see net.minecraft.entity.mob.MobEntity#mobTick
	 */
	public void tick() {
		this.baseTick();
	}

	public void baseTick() {
		this.getWorld().getProfiler().push("entityBaseTick");
		this.blockStateAtPos = null;
		if (this.hasVehicle() && this.getVehicle().isRemoved()) {
			this.stopRiding();
		}

		if (this.ridingCooldown > 0) {
			this.ridingCooldown--;
		}

		this.prevHorizontalSpeed = this.horizontalSpeed;
		this.prevPitch = this.getPitch();
		this.prevYaw = this.getYaw();
		this.tickPortal();
		if (this.shouldSpawnSprintingParticles()) {
			this.spawnSprintingParticles();
		}

		this.wasInPowderSnow = this.inPowderSnow;
		this.inPowderSnow = false;
		this.updateWaterState();
		this.updateSubmergedInWaterState();
		this.updateSwimming();
		if (this.getWorld().isClient) {
			this.extinguish();
		} else if (this.fireTicks > 0) {
			if (this.isFireImmune()) {
				this.setFireTicks(this.fireTicks - 4);
				if (this.fireTicks < 0) {
					this.extinguish();
				}
			} else {
				if (this.fireTicks % 20 == 0 && !this.isInLava()) {
					this.damage(this.getDamageSources().onFire(), 1.0F);
				}

				this.setFireTicks(this.fireTicks - 1);
			}

			if (this.getFrozenTicks() > 0) {
				this.setFrozenTicks(0);
				this.getWorld().syncWorldEvent(null, WorldEvents.FIRE_EXTINGUISHED, this.blockPos, 1);
			}
		}

		if (this.isInLava()) {
			this.setOnFireFromLava();
			this.fallDistance *= 0.5F;
		}

		this.attemptTickInVoid();
		if (!this.getWorld().isClient) {
			this.setOnFire(this.fireTicks > 0);
		}

		this.firstUpdate = false;
		this.getWorld().getProfiler().pop();
	}

	public void setOnFire(boolean onFire) {
		this.setFlag(ON_FIRE_FLAG_INDEX, onFire || this.hasVisualFire);
	}

	/**
	 * Calls {@link #tickInVoid()} when the entity is 64 blocks below the world's {@linkplain net.minecraft.world.HeightLimitView#getBottomY() minimum Y position}.
	 */
	public void attemptTickInVoid() {
		if (this.getY() < (double)(this.getWorld().getBottomY() - 64)) {
			this.tickInVoid();
		}
	}

	/**
	 * Resets the entity's portal cooldown to the default.
	 * 
	 * @see #getDefaultPortalCooldown
	 */
	public void resetPortalCooldown() {
		this.portalCooldown = this.getDefaultPortalCooldown();
	}

	public void setPortalCooldown(int portalCooldown) {
		this.portalCooldown = portalCooldown;
	}

	public int getPortalCooldown() {
		return this.portalCooldown;
	}

	/**
	 * {@return whether the entity's portal cooldown is in effect}
	 */
	public boolean hasPortalCooldown() {
		return this.portalCooldown > 0;
	}

	protected void tickPortalCooldown() {
		if (this.hasPortalCooldown()) {
			this.portalCooldown--;
		}
	}

	/**
	 * {@return how long entities can be inside the nether portal without teleporting,
	 * in ticks}
	 */
	public int getMaxNetherPortalTime() {
		return 0;
	}

	/**
	 * Sets the entity on fire from lava, applies lava damage, and plays the burning sound.
	 * 
	 * @implNote Fire from lava lasts 15 seconds by default.
	 */
	public void setOnFireFromLava() {
		if (!this.isFireImmune()) {
			this.setOnFireFor(15);
			if (this.damage(this.getDamageSources().lava(), 4.0F)) {
				this.playSound(SoundEvents.ENTITY_GENERIC_BURN, 0.4F, 2.0F + this.random.nextFloat() * 0.4F);
			}
		}
	}

	/**
	 * Sets the entity on fire for {@code seconds} seconds.
	 * 
	 * @implNote The actual duration can be reduced using the
	 * {@linkplain net.minecraft.enchantment.ProtectionEnchantment.Type#FIRE
	 * fire protection} enchantment.
	 * 
	 * @see net.minecraft.enchantment.ProtectionEnchantment#transformFireDuration
	 */
	public void setOnFireFor(int seconds) {
		int i = seconds * 20;
		if (this instanceof LivingEntity) {
			i = ProtectionEnchantment.transformFireDuration((LivingEntity)this, i);
		}

		if (this.fireTicks < i) {
			this.setFireTicks(i);
		}
	}

	/**
	 * Sets the entity on fire for {@code ticks} ticks.
	 * 
	 * @see #setOnFireFor
	 */
	public void setFireTicks(int fireTicks) {
		this.fireTicks = fireTicks;
	}

	public int getFireTicks() {
		return this.fireTicks;
	}

	/**
	 * Extinguishes this entity.
	 * 
	 * @apiNote This is used by water, {@link net.minecraft.block.LeveledCauldronBlock},
	 * and splash water bottles in vanilla.
	 */
	public void extinguish() {
		this.setFireTicks(0);
	}

	/**
	 * Called when the entity is 64 blocks below the world's {@linkplain net.minecraft.world.HeightLimitView#getBottomY() minimum Y position}.
	 * 
	 * <p>{@linkplain LivingEntity Living entities} use this to deal {@linkplain net.minecraft.entity.damage.DamageTypes#OUT_OF_WORLD out of world damage}.
	 */
	protected void tickInVoid() {
		this.discard();
	}

	/**
	 * {@return whether the bounding box with the given offsets do not collide with
	 * blocks or fluids}
	 */
	public boolean doesNotCollide(double offsetX, double offsetY, double offsetZ) {
		return this.doesNotCollide(this.getBoundingBox().offset(offsetX, offsetY, offsetZ));
	}

	private boolean doesNotCollide(Box box) {
		return this.getWorld().isSpaceEmpty(this, box) && !this.getWorld().containsFluid(box);
	}

	public void setOnGround(boolean onGround) {
		this.onGround = onGround;
		this.updateSupportingBlockPos(onGround, null);
	}

	public void setOnGround(boolean onGround, Vec3d movement) {
		this.onGround = onGround;
		this.updateSupportingBlockPos(onGround, movement);
	}

	public boolean isSupportedBy(BlockPos pos) {
		return this.supportingBlockPos.isPresent() && ((BlockPos)this.supportingBlockPos.get()).equals(pos);
	}

	protected void updateSupportingBlockPos(boolean onGround, @Nullable Vec3d movement) {
		if (onGround) {
			Box box = this.getBoundingBox();
			Box box2 = new Box(box.minX, box.minY - 1.0E-6, box.minZ, box.maxX, box.minY, box.maxZ);
			Optional<BlockPos> optional = this.world.findSupportingBlockPos(this, box2);
			if (optional.isPresent() || this.forceUpdateSupportingBlockPos) {
				this.supportingBlockPos = optional;
			} else if (movement != null) {
				Box box3 = box2.offset(-movement.x, 0.0, -movement.z);
				optional = this.world.findSupportingBlockPos(this, box3);
				this.supportingBlockPos = optional;
			}

			this.forceUpdateSupportingBlockPos = optional.isEmpty();
		} else {
			this.forceUpdateSupportingBlockPos = false;
			if (this.supportingBlockPos.isPresent()) {
				this.supportingBlockPos = Optional.empty();
			}
		}
	}

	/**
	 * {@return whether the entity is on the ground}
	 */
	public boolean isOnGround() {
		return this.onGround;
	}

	public void move(MovementType movementType, Vec3d movement) {
		if (this.noClip) {
			this.setPosition(this.getX() + movement.x, this.getY() + movement.y, this.getZ() + movement.z);
		} else {
			this.wasOnFire = this.isOnFire();
			if (movementType == MovementType.PISTON) {
				movement = this.adjustMovementForPiston(movement);
				if (movement.equals(Vec3d.ZERO)) {
					return;
				}
			}

			this.getWorld().getProfiler().push("move");
			if (this.movementMultiplier.lengthSquared() > 1.0E-7) {
				movement = movement.multiply(this.movementMultiplier);
				this.movementMultiplier = Vec3d.ZERO;
				this.setVelocity(Vec3d.ZERO);
			}

			movement = this.adjustMovementForSneaking(movement, movementType);
			Vec3d vec3d = this.adjustMovementForCollisions(movement);
			double d = vec3d.lengthSquared();
			if (d > 1.0E-7) {
				if (this.fallDistance != 0.0F && d >= 1.0) {
					BlockHitResult blockHitResult = this.getWorld()
						.raycast(
							new RaycastContext(this.getPos(), this.getPos().add(vec3d), RaycastContext.ShapeType.FALLDAMAGE_RESETTING, RaycastContext.FluidHandling.WATER, this)
						);
					if (blockHitResult.getType() != HitResult.Type.MISS) {
						this.onLanding();
					}
				}

				this.setPosition(this.getX() + vec3d.x, this.getY() + vec3d.y, this.getZ() + vec3d.z);
			}

			this.getWorld().getProfiler().pop();
			this.getWorld().getProfiler().push("rest");
			boolean bl = !MathHelper.approximatelyEquals(movement.x, vec3d.x);
			boolean bl2 = !MathHelper.approximatelyEquals(movement.z, vec3d.z);
			this.horizontalCollision = bl || bl2;
			this.verticalCollision = movement.y != vec3d.y;
			this.groundCollision = this.verticalCollision && movement.y < 0.0;
			if (this.horizontalCollision) {
				this.collidedSoftly = this.hasCollidedSoftly(vec3d);
			} else {
				this.collidedSoftly = false;
			}

			this.setOnGround(this.groundCollision, vec3d);
			BlockPos blockPos = this.getLandingPos();
			BlockState blockState = this.getWorld().getBlockState(blockPos);
			this.fall(vec3d.y, this.isOnGround(), blockState, blockPos);
			if (this.isRemoved()) {
				this.getWorld().getProfiler().pop();
			} else {
				if (this.horizontalCollision) {
					Vec3d vec3d2 = this.getVelocity();
					this.setVelocity(bl ? 0.0 : vec3d2.x, vec3d2.y, bl2 ? 0.0 : vec3d2.z);
				}

				Block block = blockState.getBlock();
				if (movement.y != vec3d.y) {
					block.onEntityLand(this.getWorld(), this);
				}

				if (this.isOnGround()) {
					block.onSteppedOn(this.getWorld(), blockPos, blockState, this);
				}

				Entity.MoveEffect moveEffect = this.getMoveEffect();
				if (moveEffect.hasAny() && !this.hasVehicle()) {
					double e = vec3d.x;
					double f = vec3d.y;
					double g = vec3d.z;
					this.speed = this.speed + (float)(vec3d.length() * 0.6);
					BlockPos blockPos2 = this.getSteppingPos();
					BlockState blockState2 = this.getWorld().getBlockState(blockPos2);
					boolean bl3 = this.canClimb(blockState2);
					if (!bl3) {
						f = 0.0;
					}

					this.horizontalSpeed = this.horizontalSpeed + (float)vec3d.horizontalLength() * 0.6F;
					this.distanceTraveled = this.distanceTraveled + (float)Math.sqrt(e * e + f * f + g * g) * 0.6F;
					if (this.distanceTraveled > this.nextStepSoundDistance && !blockState2.isAir()) {
						boolean bl4 = blockPos2.equals(blockPos);
						boolean bl5 = this.stepOnBlock(blockPos, blockState, moveEffect.playsSounds(), bl4, movement);
						if (!bl4) {
							bl5 |= this.stepOnBlock(blockPos2, blockState2, false, moveEffect.emitsGameEvents(), movement);
						}

						if (bl5) {
							this.nextStepSoundDistance = this.calculateNextStepSoundDistance();
						} else if (this.isTouchingWater()) {
							this.nextStepSoundDistance = this.calculateNextStepSoundDistance();
							if (moveEffect.playsSounds()) {
								this.playSwimSound();
							}

							if (moveEffect.emitsGameEvents()) {
								this.emitGameEvent(GameEvent.SWIM);
							}
						}
					} else if (blockState2.isAir()) {
						this.addAirTravelEffects();
					}
				}

				this.tryCheckBlockCollision();
				float h = this.getVelocityMultiplier();
				this.setVelocity(this.getVelocity().multiply((double)h, 1.0, (double)h));
				if (this.getWorld()
					.getStatesInBoxIfLoaded(this.getBoundingBox().contract(1.0E-6))
					.noneMatch(state -> state.isIn(BlockTags.FIRE) || state.isOf(Blocks.LAVA))) {
					if (this.fireTicks <= 0) {
						this.setFireTicks(-this.getBurningDuration());
					}

					if (this.wasOnFire && (this.inPowderSnow || this.isWet())) {
						this.playExtinguishSound();
					}
				}

				if (this.isOnFire() && (this.inPowderSnow || this.isWet())) {
					this.setFireTicks(-this.getBurningDuration());
				}

				this.getWorld().getProfiler().pop();
			}
		}
	}

	private boolean canClimb(BlockState state) {
		return state.isIn(BlockTags.CLIMBABLE) || state.isOf(Blocks.POWDER_SNOW);
	}

	private boolean stepOnBlock(BlockPos pos, BlockState state, boolean playSound, boolean emitEvent, Vec3d movement) {
		if (state.isAir()) {
			return false;
		} else {
			boolean bl = this.canClimb(state);
			if ((this.isOnGround() || bl || this.isInSneakingPose() && movement.y == 0.0 || this.isOnRail()) && !this.isSwimming()) {
				if (playSound) {
					this.playStepSounds(pos, state);
				}

				if (emitEvent) {
					this.getWorld().emitGameEvent(GameEvent.STEP, this.getPos(), GameEvent.Emitter.of(this, state));
				}

				return true;
			} else {
				return false;
			}
		}
	}

	protected boolean hasCollidedSoftly(Vec3d adjustedMovement) {
		return false;
	}

	protected void tryCheckBlockCollision() {
		try {
			this.checkBlockCollision();
		} catch (Throwable var4) {
			CrashReport crashReport = CrashReport.create(var4, "Checking entity block collision");
			CrashReportSection crashReportSection = crashReport.addElement("Entity being checked for collision");
			this.populateCrashReport(crashReportSection);
			throw new CrashException(crashReport);
		}
	}

	/**
	 * Plays the {@link
	 * net.minecraft.sound.SoundEvents#ENTITY_GENERIC_EXTINGUISH_FIRE} sound.
	 */
	protected void playExtinguishSound() {
		this.playSound(SoundEvents.ENTITY_GENERIC_EXTINGUISH_FIRE, 0.7F, 1.6F + (this.random.nextFloat() - this.random.nextFloat()) * 0.4F);
	}

	public void extinguishWithSound() {
		if (!this.getWorld().isClient && this.wasOnFire) {
			this.playExtinguishSound();
		}

		this.extinguish();
	}

	/**
	 * Adds the effects of this entity when it travels in air, usually to the
	 * world the entity is in.
	 * 
	 * <p>This is only called when the entity {@linkplain #getMoveEffect() has
	 * any move effect}, from {@link #move(MovementType, Vec3d)}
	 */
	protected void addAirTravelEffects() {
		if (this.isFlappingWings()) {
			this.addFlapEffects();
			if (this.getMoveEffect().emitsGameEvents()) {
				this.emitGameEvent(GameEvent.FLAP);
			}
		}
	}

	/**
	 * {@return the landing position}
	 * 
	 * @implNote Landing position is the entity's position, with {@code 0.2} subtracted
	 * from the Y coordinate. This means that, for example, if a player is on a carpet on
	 * a soul soil, the soul soil's position would be returned.
	 * 
	 * @see #getSteppingPos()
	 * @see #getLandingBlockState()
	 */
	@Deprecated
	public BlockPos getLandingPos() {
		return this.getPosWithYOffset(0.2F);
	}

	protected BlockPos getVelocityAffectingPos() {
		return this.getPosWithYOffset(0.500001F);
	}

	/**
	 * {@return the stepping position}
	 * 
	 * @implNote Stepping position is the entity's position, with {@code 1e-05} subtracted
	 * from the Y coordinate. This means that, for example, if a player is on a carpet on
	 * a soul soil, the carpet's position would be returned.
	 * 
	 * @see #getLandingPos()
	 * @see #getSteppingBlockState()
	 */
	public BlockPos getSteppingPos() {
		return this.getPosWithYOffset(1.0E-5F);
	}

	protected BlockPos getPosWithYOffset(float offset) {
		if (this.supportingBlockPos.isPresent()) {
			BlockPos blockPos = (BlockPos)this.supportingBlockPos.get();
			if (!(offset > 1.0E-5F)) {
				return blockPos;
			} else {
				BlockState blockState = this.getWorld().getBlockState(blockPos);
				return (!((double)offset <= 0.5) || !blockState.isIn(BlockTags.FENCES))
						&& !blockState.isIn(BlockTags.WALLS)
						&& !(blockState.getBlock() instanceof FenceGateBlock)
					? blockPos.withY(MathHelper.floor(this.pos.y - (double)offset))
					: blockPos;
			}
		} else {
			int i = MathHelper.floor(this.pos.x);
			int j = MathHelper.floor(this.pos.y - (double)offset);
			int k = MathHelper.floor(this.pos.z);
			return new BlockPos(i, j, k);
		}
	}

	protected float getJumpVelocityMultiplier() {
		float f = this.getWorld().getBlockState(this.getBlockPos()).getBlock().getJumpVelocityMultiplier();
		float g = this.getWorld().getBlockState(this.getVelocityAffectingPos()).getBlock().getJumpVelocityMultiplier();
		return (double)f == 1.0 ? g : f;
	}

	protected float getVelocityMultiplier() {
		BlockState blockState = this.getWorld().getBlockState(this.getBlockPos());
		float f = blockState.getBlock().getVelocityMultiplier();
		if (!blockState.isOf(Blocks.WATER) && !blockState.isOf(Blocks.BUBBLE_COLUMN)) {
			return (double)f == 1.0 ? this.getWorld().getBlockState(this.getVelocityAffectingPos()).getBlock().getVelocityMultiplier() : f;
		} else {
			return f;
		}
	}

	protected Vec3d adjustMovementForSneaking(Vec3d movement, MovementType type) {
		return movement;
	}

	protected Vec3d adjustMovementForPiston(Vec3d movement) {
		if (movement.lengthSquared() <= 1.0E-7) {
			return movement;
		} else {
			long l = this.getWorld().getTime();
			if (l != this.pistonMovementTick) {
				Arrays.fill(this.pistonMovementDelta, 0.0);
				this.pistonMovementTick = l;
			}

			if (movement.x != 0.0) {
				double d = this.calculatePistonMovementFactor(Direction.Axis.X, movement.x);
				return Math.abs(d) <= 1.0E-5F ? Vec3d.ZERO : new Vec3d(d, 0.0, 0.0);
			} else if (movement.y != 0.0) {
				double d = this.calculatePistonMovementFactor(Direction.Axis.Y, movement.y);
				return Math.abs(d) <= 1.0E-5F ? Vec3d.ZERO : new Vec3d(0.0, d, 0.0);
			} else if (movement.z != 0.0) {
				double d = this.calculatePistonMovementFactor(Direction.Axis.Z, movement.z);
				return Math.abs(d) <= 1.0E-5F ? Vec3d.ZERO : new Vec3d(0.0, 0.0, d);
			} else {
				return Vec3d.ZERO;
			}
		}
	}

	private double calculatePistonMovementFactor(Direction.Axis axis, double offsetFactor) {
		int i = axis.ordinal();
		double d = MathHelper.clamp(offsetFactor + this.pistonMovementDelta[i], -0.51, 0.51);
		offsetFactor = d - this.pistonMovementDelta[i];
		this.pistonMovementDelta[i] = d;
		return offsetFactor;
	}

	private Vec3d adjustMovementForCollisions(Vec3d movement) {
		Box box = this.getBoundingBox();
		List<VoxelShape> list = this.getWorld().getEntityCollisions(this, box.stretch(movement));
		Vec3d vec3d = movement.lengthSquared() == 0.0 ? movement : adjustMovementForCollisions(this, movement, box, this.getWorld(), list);
		boolean bl = movement.x != vec3d.x;
		boolean bl2 = movement.y != vec3d.y;
		boolean bl3 = movement.z != vec3d.z;
		boolean bl4 = this.isOnGround() || bl2 && movement.y < 0.0;
		if (this.getStepHeight() > 0.0F && bl4 && (bl || bl3)) {
			Vec3d vec3d2 = adjustMovementForCollisions(this, new Vec3d(movement.x, (double)this.getStepHeight(), movement.z), box, this.getWorld(), list);
			Vec3d vec3d3 = adjustMovementForCollisions(
				this, new Vec3d(0.0, (double)this.getStepHeight(), 0.0), box.stretch(movement.x, 0.0, movement.z), this.getWorld(), list
			);
			if (vec3d3.y < (double)this.getStepHeight()) {
				Vec3d vec3d4 = adjustMovementForCollisions(this, new Vec3d(movement.x, 0.0, movement.z), box.offset(vec3d3), this.getWorld(), list).add(vec3d3);
				if (vec3d4.horizontalLengthSquared() > vec3d2.horizontalLengthSquared()) {
					vec3d2 = vec3d4;
				}
			}

			if (vec3d2.horizontalLengthSquared() > vec3d.horizontalLengthSquared()) {
				return vec3d2.add(adjustMovementForCollisions(this, new Vec3d(0.0, -vec3d2.y + movement.y, 0.0), box.offset(vec3d2), this.getWorld(), list));
			}
		}

		return vec3d;
	}

	public static Vec3d adjustMovementForCollisions(@Nullable Entity entity, Vec3d movement, Box entityBoundingBox, World world, List<VoxelShape> collisions) {
		Builder<VoxelShape> builder = ImmutableList.builderWithExpectedSize(collisions.size() + 1);
		if (!collisions.isEmpty()) {
			builder.addAll(collisions);
		}

		WorldBorder worldBorder = world.getWorldBorder();
		boolean bl = entity != null && worldBorder.canCollide(entity, entityBoundingBox.stretch(movement));
		if (bl) {
			builder.add(worldBorder.asVoxelShape());
		}

		builder.addAll(world.getBlockCollisions(entity, entityBoundingBox.stretch(movement)));
		return adjustMovementForCollisions(movement, entityBoundingBox, builder.build());
	}

	private static Vec3d adjustMovementForCollisions(Vec3d movement, Box entityBoundingBox, List<VoxelShape> collisions) {
		if (collisions.isEmpty()) {
			return movement;
		} else {
			double d = movement.x;
			double e = movement.y;
			double f = movement.z;
			if (e != 0.0) {
				e = VoxelShapes.calculateMaxOffset(Direction.Axis.Y, entityBoundingBox, collisions, e);
				if (e != 0.0) {
					entityBoundingBox = entityBoundingBox.offset(0.0, e, 0.0);
				}
			}

			boolean bl = Math.abs(d) < Math.abs(f);
			if (bl && f != 0.0) {
				f = VoxelShapes.calculateMaxOffset(Direction.Axis.Z, entityBoundingBox, collisions, f);
				if (f != 0.0) {
					entityBoundingBox = entityBoundingBox.offset(0.0, 0.0, f);
				}
			}

			if (d != 0.0) {
				d = VoxelShapes.calculateMaxOffset(Direction.Axis.X, entityBoundingBox, collisions, d);
				if (!bl && d != 0.0) {
					entityBoundingBox = entityBoundingBox.offset(d, 0.0, 0.0);
				}
			}

			if (!bl && f != 0.0) {
				f = VoxelShapes.calculateMaxOffset(Direction.Axis.Z, entityBoundingBox, collisions, f);
			}

			return new Vec3d(d, e, f);
		}
	}

	protected float calculateNextStepSoundDistance() {
		return (float)((int)this.distanceTraveled + 1);
	}

	protected SoundEvent getSwimSound() {
		return SoundEvents.ENTITY_GENERIC_SWIM;
	}

	protected SoundEvent getSplashSound() {
		return SoundEvents.ENTITY_GENERIC_SPLASH;
	}

	protected SoundEvent getHighSpeedSplashSound() {
		return SoundEvents.ENTITY_GENERIC_SPLASH;
	}

	/**
	 * Checks the entity's block collision, calling {@link
	 * net.minecraft.block.AbstractBlock#onEntityCollision} and {@link #onBlockCollision}.
	 * This should be called manually if {@link #tick} is overridden.
	 */
	protected void checkBlockCollision() {
		Box box = this.getBoundingBox();
		BlockPos blockPos = BlockPos.ofFloored(box.minX + 1.0E-7, box.minY + 1.0E-7, box.minZ + 1.0E-7);
		BlockPos blockPos2 = BlockPos.ofFloored(box.maxX - 1.0E-7, box.maxY - 1.0E-7, box.maxZ - 1.0E-7);
		if (this.getWorld().isRegionLoaded(blockPos, blockPos2)) {
			BlockPos.Mutable mutable = new BlockPos.Mutable();

			for (int i = blockPos.getX(); i <= blockPos2.getX(); i++) {
				for (int j = blockPos.getY(); j <= blockPos2.getY(); j++) {
					for (int k = blockPos.getZ(); k <= blockPos2.getZ(); k++) {
						mutable.set(i, j, k);
						BlockState blockState = this.getWorld().getBlockState(mutable);

						try {
							blockState.onEntityCollision(this.getWorld(), mutable, this);
							this.onBlockCollision(blockState);
						} catch (Throwable var12) {
							CrashReport crashReport = CrashReport.create(var12, "Colliding entity with block");
							CrashReportSection crashReportSection = crashReport.addElement("Block being collided with");
							CrashReportSection.addBlockInfo(crashReportSection, this.getWorld(), mutable, blockState);
							throw new CrashException(crashReport);
						}
					}
				}
			}
		}
	}

	/**
	 * Called when this entity's collision box intersects {@code state}.
	 * 
	 * @see net.minecraft.block.AbstractBlock#onEntityCollision
	 */
	protected void onBlockCollision(BlockState state) {
	}

	/**
	 * Emits a game event originating from another entity at this entity's position.
	 * 
	 * <p>A common example is a game event called in {@link #interact}, where the player
	 * interacting with the entity is the emitter of the event.
	 * 
	 * @see #emitGameEvent(GameEvent)
	 * 
	 * @param entity the entity that emitted the game event, or {@code null} if there is none
	 */
	public void emitGameEvent(GameEvent event, @Nullable Entity entity) {
		this.getWorld().emitGameEvent(entity, event, this.pos);
	}

	/**
	 * Emits a game event originating from this entity at this entity's position.
	 * 
	 * @see #emitGameEvent(GameEvent, Entity)
	 */
	public void emitGameEvent(GameEvent event) {
		this.emitGameEvent(event, this);
	}

	private void playStepSounds(BlockPos pos, BlockState state) {
		this.playStepSound(pos, state);
		if (this.shouldPlayAmethystChimeSound(state)) {
			this.playAmethystChimeSound();
		}
	}

	protected void playSwimSound() {
		Entity entity = (Entity)(this.hasPassengers() && this.getControllingPassenger() != null ? this.getControllingPassenger() : this);
		float f = entity == this ? 0.35F : 0.4F;
		Vec3d vec3d = entity.getVelocity();
		float g = Math.min(1.0F, (float)Math.sqrt(vec3d.x * vec3d.x * 0.2F + vec3d.y * vec3d.y + vec3d.z * vec3d.z * 0.2F) * f);
		this.playSwimSound(g);
	}

	protected BlockPos getStepSoundPos(BlockPos pos) {
		BlockPos blockPos = pos.up();
		BlockState blockState = this.getWorld().getBlockState(blockPos);
		return !blockState.isIn(BlockTags.INSIDE_STEP_SOUND_BLOCKS) && !blockState.isIn(BlockTags.COMBINATION_STEP_SOUND_BLOCKS) ? pos : blockPos;
	}

	protected void playCombinationStepSounds(BlockState primaryState, BlockState secondaryState) {
		BlockSoundGroup blockSoundGroup = primaryState.getSoundGroup();
		this.playSound(blockSoundGroup.getStepSound(), blockSoundGroup.getVolume() * 0.15F, blockSoundGroup.getPitch());
		this.playSecondaryStepSound(secondaryState);
	}

	protected void playSecondaryStepSound(BlockState state) {
		BlockSoundGroup blockSoundGroup = state.getSoundGroup();
		this.playSound(blockSoundGroup.getStepSound(), blockSoundGroup.getVolume() * 0.05F, blockSoundGroup.getPitch() * 0.8F);
	}

	protected void playStepSound(BlockPos pos, BlockState state) {
		BlockSoundGroup blockSoundGroup = state.getSoundGroup();
		this.playSound(blockSoundGroup.getStepSound(), blockSoundGroup.getVolume() * 0.15F, blockSoundGroup.getPitch());
	}

	private boolean shouldPlayAmethystChimeSound(BlockState state) {
		return state.isIn(BlockTags.CRYSTAL_SOUND_BLOCKS) && this.age >= this.lastChimeAge + 20;
	}

	private void playAmethystChimeSound() {
		this.lastChimeIntensity = this.lastChimeIntensity * (float)Math.pow(0.997, (double)(this.age - this.lastChimeAge));
		this.lastChimeIntensity = Math.min(1.0F, this.lastChimeIntensity + 0.07F);
		float f = 0.5F + this.lastChimeIntensity * this.random.nextFloat() * 1.2F;
		float g = 0.1F + this.lastChimeIntensity * 1.2F;
		this.playSound(SoundEvents.BLOCK_AMETHYST_BLOCK_CHIME, g, f);
		this.lastChimeAge = this.age;
	}

	protected void playSwimSound(float volume) {
		this.playSound(this.getSwimSound(), volume, 1.0F + (this.random.nextFloat() - this.random.nextFloat()) * 0.4F);
	}

	/**
	 * Adds the effects of this entity flapping, usually to the world the entity
	 * is in.
	 * 
	 * <p>The actual flapping logic should be done in {@link #tick()} instead.
	 * 
	 * <p>This is only called when the entity {@linkplain #isFlappingWings() is flapping wings}
	 * and the entity {@linkplain #getMoveEffect() has any move effect}, from
	 * {@link #addAirTravelEffects()}.
	 */
	protected void addFlapEffects() {
	}

	/**
	 * {@return whether the entity is flapping their wings}
	 * 
	 * <p>Entities flapping their wings will call {@link #addFlapEffects} inside
	 * {@link #addAirTravelEffects}.
	 */
	protected boolean isFlappingWings() {
		return false;
	}

	/**
	 * Plays {@code sound} at this entity's position with the entity's {@linkplain
	 * #getSoundCategory sound category} if the entity is {@linkplain #isSilent not silent}.
	 */
	public void playSound(SoundEvent sound, float volume, float pitch) {
		if (!this.isSilent()) {
			this.getWorld().playSound(null, this.getX(), this.getY(), this.getZ(), sound, this.getSoundCategory(), volume, pitch);
		}
	}

	public void playSoundIfNotSilent(SoundEvent event) {
		if (!this.isSilent()) {
			this.playSound(event, 1.0F, 1.0F);
		}
	}

	/**
	 * {@return whether the entity is silent}
	 * 
	 * <p>Silent entities should not make sounds. {@link #playSound} checks this method by
	 * default, but if a sound is played manually, this has to be checked too.
	 * 
	 * <p>This is saved under the {@code Silent} NBT key.
	 */
	public boolean isSilent() {
		return this.dataTracker.get(SILENT);
	}

	/**
	 * Sets whether the entity is silent.
	 * 
	 * <p>This is saved under the {@code Silent} NBT key.
	 */
	public void setSilent(boolean silent) {
		this.dataTracker.set(SILENT, silent);
	}

	/**
	 * {@return whether the entity has no gravity}
	 * 
	 * <p>Entities using {@link net.minecraft.entity.ai.control.FlightMoveControl} has
	 * no gravity. This is saved under the {@code NoGravity} NBT key.
	 */
	public boolean hasNoGravity() {
		return this.dataTracker.get(NO_GRAVITY);
	}

	/**
	 * Sets  whether the entity has no gravity.
	 * 
	 * <p>Entities using {@link net.minecraft.entity.ai.control.FlightMoveControl} has
	 * no gravity. This is saved under the {@code NoGravity} NBT key.
	 */
	public void setNoGravity(boolean noGravity) {
		this.dataTracker.set(NO_GRAVITY, noGravity);
	}

	/**
	 * Returns the possible effect(s) of an entity moving.
	 * 
	 * @implNote If an entity does not emit game events or play move sounds, this
	 * method should be overridden as returning a value other than
	 * {@linkplain Entity.MoveEffect#ALL ALL} allows skipping some movement logic
	 * and boost ticking performance.
	 */
	protected Entity.MoveEffect getMoveEffect() {
		return Entity.MoveEffect.ALL;
	}

	/**
	 * {@return whether the entity should not emit vibrations}
	 * 
	 * <p>By default, wool or carpet {@linkplain ItemEntity item entities}, and
	 * {@link net.minecraft.entity.mob.WardenEntity} do not emit vibrations.
	 */
	public boolean occludeVibrationSignals() {
		return false;
	}

	/**
	 * Called when the entity falls. Flying mobs should override this to do nothing.
	 * 
	 * @implNote If on ground, this calls {@link net.minecraft.block.Block#onLandedUpon}, which can add or
	 * reduce fall damage, emits {@link GameEvent#HIT_GROUND}, then calls {@link #onLanding}.
	 * Otherwise, if {@code heightDifference} is negative, it subtracts that value from
	 * {@link #fallDistance}.
	 */
	protected void fall(double heightDifference, boolean onGround, BlockState state, BlockPos landedPosition) {
		if (onGround) {
			if (this.fallDistance > 0.0F) {
				state.getBlock().onLandedUpon(this.getWorld(), state, landedPosition, this, this.fallDistance);
				this.getWorld()
					.emitGameEvent(
						GameEvent.HIT_GROUND,
						this.pos,
						GameEvent.Emitter.of(this, (BlockState)this.supportingBlockPos.map(blockPos -> this.getWorld().getBlockState(blockPos)).orElse(state))
					);
			}

			this.onLanding();
		} else if (heightDifference < 0.0) {
			this.fallDistance -= (float)heightDifference;
		}
	}

	/**
	 * {@return whether the entity is immune to {@linkplain
	 * net.minecraft.registry.tag.DamageTypeTags#IS_FIRE fire damage}}
	 * 
	 * @see EntityType.Builder#makeFireImmune
	 */
	public boolean isFireImmune() {
		return this.getType().isFireImmune();
	}

	/**
	 * Called when an entity falls.
	 * 
	 * <p>Flying mobs and mobs immune to fall damage should override this to do nothing.
	 * Mobs with reduced fall damage should override this method to apply reduced damage instead.
	 * Some entities explode instead of applying fall damage, like {@link
	 * net.minecraft.entity.vehicle.TntMinecartEntity}.
	 * 
	 * @return whether to play the sound when falling on honey block; {@code false} for all
	 * entities except horses and llamas
	 */
	public boolean handleFallDamage(float fallDistance, float damageMultiplier, DamageSource damageSource) {
		if (this.type.isIn(EntityTypeTags.FALL_DAMAGE_IMMUNE)) {
			return false;
		} else {
			if (this.hasPassengers()) {
				for (Entity entity : this.getPassengerList()) {
					entity.handleFallDamage(fallDistance, damageMultiplier, damageSource);
				}
			}

			return false;
		}
	}

	/**
	 * Returns whether this entity's hitbox is touching water fluid.
	 */
	public boolean isTouchingWater() {
		return this.touchingWater;
	}

	/**
	 * {@return whether it is raining at the entity's position}
	 */
	private boolean isBeingRainedOn() {
		BlockPos blockPos = this.getBlockPos();
		return this.getWorld().hasRain(blockPos)
			|| this.getWorld().hasRain(BlockPos.ofFloored((double)blockPos.getX(), this.getBoundingBox().maxY, (double)blockPos.getZ()));
	}

	/**
	 * {@return whether the block at the entity's position is a bubble column}
	 */
	private boolean isInsideBubbleColumn() {
		return this.getWorld().getBlockState(this.getBlockPos()).isOf(Blocks.BUBBLE_COLUMN);
	}

	/**
	 * {@return whether this entity is touching water or is being rained on (but does not check
	 * for a bubble column)}
	 * 
	 * @see net.minecraft.entity.Entity#isTouchingWater()
	 * @see net.minecraft.entity.Entity#isBeingRainedOn()
	 * @see net.minecraft.entity.Entity#isWet()
	 * @see net.minecraft.entity.Entity#isInsideWaterOrBubbleColumn()
	 */
	public boolean isTouchingWaterOrRain() {
		return this.isTouchingWater() || this.isBeingRainedOn();
	}

	/**
	 * {@return whether this entity is touching water, or is being rained on, or is
	 * inside a bubble column}
	 * 
	 * @see net.minecraft.entity.Entity#isTouchingWater()
	 * @see net.minecraft.entity.Entity#isBeingRainedOn()
	 * @see net.minecraft.entity.Entity#isInsideBubbleColumn()
	 * @see net.minecraft.entity.Entity#isInsideWaterOrBubbleColumn()
	 */
	public boolean isWet() {
		return this.isTouchingWater() || this.isBeingRainedOn() || this.isInsideBubbleColumn();
	}

	/**
	 * {@return whether this entity is touching water or a bubble column}
	 * 
	 * @see net.minecraft.entity.Entity#isTouchingWater()
	 * @see net.minecraft.entity.Entity#isBeingRainedOn()
	 * @see net.minecraft.entity.Entity#isInsideBubbleColumn()
	 * @see net.minecraft.entity.Entity#isWet()
	 */
	public boolean isInsideWaterOrBubbleColumn() {
		return this.isTouchingWater() || this.isInsideBubbleColumn();
	}

	/**
	 * {@return whether this entity's hitbox is fully submerged in water}
	 */
	public boolean isSubmergedInWater() {
		return this.submergedInWater && this.isTouchingWater();
	}

	public void updateSwimming() {
		if (this.isSwimming()) {
			this.setSwimming(this.isSprinting() && this.isTouchingWater() && !this.hasVehicle());
		} else {
			this.setSwimming(this.isSprinting() && this.isSubmergedInWater() && !this.hasVehicle() && this.getWorld().getFluidState(this.blockPos).isIn(FluidTags.WATER));
		}
	}

	protected boolean updateWaterState() {
		this.fluidHeight.clear();
		this.checkWaterState();
		double d = this.getWorld().getDimension().ultrawarm() ? 0.007 : 0.0023333333333333335;
		boolean bl = this.updateMovementInFluid(FluidTags.LAVA, d);
		return this.isTouchingWater() || bl;
	}

	void checkWaterState() {
		if (this.getVehicle() instanceof BoatEntity boatEntity && !boatEntity.isSubmergedInWater()) {
			this.touchingWater = false;
			return;
		}

		if (this.updateMovementInFluid(FluidTags.WATER, 0.014)) {
			if (!this.touchingWater && !this.firstUpdate) {
				this.onSwimmingStart();
			}

			this.onLanding();
			this.touchingWater = true;
			this.extinguish();
		} else {
			this.touchingWater = false;
		}
	}

	private void updateSubmergedInWaterState() {
		this.submergedInWater = this.isSubmergedIn(FluidTags.WATER);
		this.submergedFluidTag.clear();
		double d = this.getEyeY() - 0.11111111F;
		if (this.getVehicle() instanceof BoatEntity boatEntity
			&& !boatEntity.isSubmergedInWater()
			&& boatEntity.getBoundingBox().maxY >= d
			&& boatEntity.getBoundingBox().minY <= d) {
			return;
		}

		BlockPos blockPos = BlockPos.ofFloored(this.getX(), d, this.getZ());
		FluidState fluidState = this.getWorld().getFluidState(blockPos);
		double e = (double)((float)blockPos.getY() + fluidState.getHeight(this.getWorld(), blockPos));
		if (e > d) {
			fluidState.streamTags().forEach(this.submergedFluidTag::add);
		}
	}

	protected void onSwimmingStart() {
		Entity entity = (Entity)(this.hasPassengers() && this.getControllingPassenger() != null ? this.getControllingPassenger() : this);
		float f = entity == this ? 0.2F : 0.9F;
		Vec3d vec3d = entity.getVelocity();
		float g = Math.min(1.0F, (float)Math.sqrt(vec3d.x * vec3d.x * 0.2F + vec3d.y * vec3d.y + vec3d.z * vec3d.z * 0.2F) * f);
		if (g < 0.25F) {
			this.playSound(this.getSplashSound(), g, 1.0F + (this.random.nextFloat() - this.random.nextFloat()) * 0.4F);
		} else {
			this.playSound(this.getHighSpeedSplashSound(), g, 1.0F + (this.random.nextFloat() - this.random.nextFloat()) * 0.4F);
		}

		float h = (float)MathHelper.floor(this.getY());

		for (int i = 0; (float)i < 1.0F + this.dimensions.width * 20.0F; i++) {
			double d = (this.random.nextDouble() * 2.0 - 1.0) * (double)this.dimensions.width;
			double e = (this.random.nextDouble() * 2.0 - 1.0) * (double)this.dimensions.width;
			this.getWorld()
				.addParticle(ParticleTypes.BUBBLE, this.getX() + d, (double)(h + 1.0F), this.getZ() + e, vec3d.x, vec3d.y - this.random.nextDouble() * 0.2F, vec3d.z);
		}

		for (int i = 0; (float)i < 1.0F + this.dimensions.width * 20.0F; i++) {
			double d = (this.random.nextDouble() * 2.0 - 1.0) * (double)this.dimensions.width;
			double e = (this.random.nextDouble() * 2.0 - 1.0) * (double)this.dimensions.width;
			this.getWorld().addParticle(ParticleTypes.SPLASH, this.getX() + d, (double)(h + 1.0F), this.getZ() + e, vec3d.x, vec3d.y, vec3d.z);
		}

		this.emitGameEvent(GameEvent.SPLASH);
	}

	/**
	 * {@return the block state at the landing position}
	 * 
	 * @implNote Landing position is the entity's position, with {@code 0.2} subtracted
	 * from the Y coordinate. This means that, for example, if a player is on a carpet on
	 * a soul soil, the soul soil's position would be returned.
	 * 
	 * @see #getLandingPos()
	 */
	@Deprecated
	protected BlockState getLandingBlockState() {
		return this.getWorld().getBlockState(this.getLandingPos());
	}

	/**
	 * {@return the block state at the stepping position}
	 * 
	 * @implNote Stepping position is the entity's position, with {@code 1e-05} subtracted
	 * from the Y coordinate. This means that, for example, if a player is on a carpet on
	 * a soul soil, the carpet's position would be returned.
	 * 
	 * @see #getSteppingPos()
	 */
	public BlockState getSteppingBlockState() {
		return this.getWorld().getBlockState(this.getSteppingPos());
	}

	public boolean shouldSpawnSprintingParticles() {
		return this.isSprinting() && !this.isTouchingWater() && !this.isSpectator() && !this.isInSneakingPose() && !this.isInLava() && this.isAlive();
	}

	protected void spawnSprintingParticles() {
		BlockPos blockPos = this.getLandingPos();
		BlockState blockState = this.getWorld().getBlockState(blockPos);
		if (blockState.getRenderType() != BlockRenderType.INVISIBLE) {
			Vec3d vec3d = this.getVelocity();
			BlockPos blockPos2 = this.getBlockPos();
			double d = this.getX() + (this.random.nextDouble() - 0.5) * (double)this.dimensions.width;
			double e = this.getZ() + (this.random.nextDouble() - 0.5) * (double)this.dimensions.width;
			if (blockPos2.getX() != blockPos.getX()) {
				d = MathHelper.clamp(d, (double)blockPos.getX(), (double)blockPos.getX() + 1.0);
			}

			if (blockPos2.getZ() != blockPos.getZ()) {
				e = MathHelper.clamp(e, (double)blockPos.getZ(), (double)blockPos.getZ() + 1.0);
			}

			this.getWorld().addParticle(new BlockStateParticleEffect(ParticleTypes.BLOCK, blockState), d, this.getY() + 0.1, e, vec3d.x * -4.0, 1.5, vec3d.z * -4.0);
		}
	}

	/**
	 * {@return whether the entity is submerged in a fluid in {@code fluidTag}}
	 */
	public boolean isSubmergedIn(TagKey<Fluid> fluidTag) {
		return this.submergedFluidTag.contains(fluidTag);
	}

	/**
	 * {@return whether the entity is in lava}
	 */
	public boolean isInLava() {
		return !this.firstUpdate && this.fluidHeight.getDouble(FluidTags.LAVA) > 0.0;
	}

	/**
	 * Updates the entity's velocity to add a vector in the direction of the entity's yaw
	 * whose absolute value is {@code movementInput} normalized and multiplied by {@code speed}.
	 * 
	 * <p>This is usually called inside overridden {@link LivingEntity#travel} if the entity is
	 * touching water; see {@link net.minecraft.entity.passive.FishEntity} for an example.
	 */
	public void updateVelocity(float speed, Vec3d movementInput) {
		Vec3d vec3d = movementInputToVelocity(movementInput, speed, this.getYaw());
		this.setVelocity(this.getVelocity().add(vec3d));
	}

	/**
	 * {@return a vector with the horizontal direction being {@code yaw} degrees and the
	 * absolute value being {@code movementInput} normalized and multiplied by {@code speed}}
	 */
	private static Vec3d movementInputToVelocity(Vec3d movementInput, float speed, float yaw) {
		double d = movementInput.lengthSquared();
		if (d < 1.0E-7) {
			return Vec3d.ZERO;
		} else {
			Vec3d vec3d = (d > 1.0 ? movementInput.normalize() : movementInput).multiply((double)speed);
			float f = MathHelper.sin(yaw * (float) (Math.PI / 180.0));
			float g = MathHelper.cos(yaw * (float) (Math.PI / 180.0));
			return new Vec3d(vec3d.x * (double)g - vec3d.z * (double)f, vec3d.y, vec3d.z * (double)g + vec3d.x * (double)f);
		}
	}

	@Deprecated
	public float getBrightnessAtEyes() {
		return this.getWorld().isPosLoaded(this.getBlockX(), this.getBlockZ())
			? this.getWorld().getBrightness(BlockPos.ofFloored(this.getX(), this.getEyeY(), this.getZ()))
			: 0.0F;
	}

	public void updatePositionAndAngles(double x, double y, double z, float yaw, float pitch) {
		this.updatePosition(x, y, z);
		this.setYaw(yaw % 360.0F);
		this.setPitch(MathHelper.clamp(pitch, -90.0F, 90.0F) % 360.0F);
		this.prevYaw = this.getYaw();
		this.prevPitch = this.getPitch();
	}

	public void updatePosition(double x, double y, double z) {
		double d = MathHelper.clamp(x, -3.0E7, 3.0E7);
		double e = MathHelper.clamp(z, -3.0E7, 3.0E7);
		this.prevX = d;
		this.prevY = y;
		this.prevZ = e;
		this.setPosition(d, y, e);
	}

	public void refreshPositionAfterTeleport(Vec3d pos) {
		this.refreshPositionAfterTeleport(pos.x, pos.y, pos.z);
	}

	public void refreshPositionAfterTeleport(double x, double y, double z) {
		this.refreshPositionAndAngles(x, y, z, this.getYaw(), this.getPitch());
	}

	/**
	 * Sets the entity's position, yaw, and pitch, and refreshes several position-related
	 * fields.
	 * 
	 * <p>This should be used over other methods for setting positions of mobs.
	 * 
	 * @see #refreshPositionAndAngles(double, double, double, float, float)
	 */
	public void refreshPositionAndAngles(BlockPos pos, float yaw, float pitch) {
		this.refreshPositionAndAngles((double)pos.getX() + 0.5, (double)pos.getY(), (double)pos.getZ() + 0.5, yaw, pitch);
	}

	/**
	 * Sets the entity's position, yaw, and pitch, and refreshes several position-related
	 * fields.
	 * 
	 * <p>This should be used over other methods for setting positions of mobs.
	 * 
	 * @see #refreshPositionAndAngles(BlockPos, float, float)
	 */
	public void refreshPositionAndAngles(double x, double y, double z, float yaw, float pitch) {
		this.setPos(x, y, z);
		this.setYaw(yaw);
		this.setPitch(pitch);
		this.resetPosition();
		this.refreshPosition();
	}

	public final void resetPosition() {
		double d = this.getX();
		double e = this.getY();
		double f = this.getZ();
		this.prevX = d;
		this.prevY = e;
		this.prevZ = f;
		this.lastRenderX = d;
		this.lastRenderY = e;
		this.lastRenderZ = f;
		this.prevYaw = this.getYaw();
		this.prevPitch = this.getPitch();
	}

	/**
	 * {@return the distance between this entity and {@code entity}}
	 */
	public float distanceTo(Entity entity) {
		float f = (float)(this.getX() - entity.getX());
		float g = (float)(this.getY() - entity.getY());
		float h = (float)(this.getZ() - entity.getZ());
		return MathHelper.sqrt(f * f + g * g + h * h);
	}

	/**
	 * {@return the squared distance between this entity and the given position}
	 */
	public double squaredDistanceTo(double x, double y, double z) {
		double d = this.getX() - x;
		double e = this.getY() - y;
		double f = this.getZ() - z;
		return d * d + e * e + f * f;
	}

	/**
	 * {@return the squared distance between this entity and {@code entity}}
	 */
	public double squaredDistanceTo(Entity entity) {
		return this.squaredDistanceTo(entity.getPos());
	}

	/**
	 * {@return the squared distance between this entity and the given position}
	 */
	public double squaredDistanceTo(Vec3d vector) {
		double d = this.getX() - vector.x;
		double e = this.getY() - vector.y;
		double f = this.getZ() - vector.z;
		return d * d + e * e + f * f;
	}

	/**
	 * Called when a player collides with the entity. Does nothing by default.
	 * 
	 * <p>This should be overridden if the collision logic is specific to players,
	 * such as picking up item entities, experience orbs, or arrows.
	 */
	public void onPlayerCollision(PlayerEntity player) {
	}

	public void pushAwayFrom(Entity entity) {
		if (!this.isConnectedThroughVehicle(entity)) {
			if (!entity.noClip && !this.noClip) {
				double d = entity.getX() - this.getX();
				double e = entity.getZ() - this.getZ();
				double f = MathHelper.absMax(d, e);
				if (f >= 0.01F) {
					f = Math.sqrt(f);
					d /= f;
					e /= f;
					double g = 1.0 / f;
					if (g > 1.0) {
						g = 1.0;
					}

					d *= g;
					e *= g;
					d *= 0.05F;
					e *= 0.05F;
					if (!this.hasPassengers() && this.isPushable()) {
						this.addVelocity(-d, 0.0, -e);
					}

					if (!entity.hasPassengers() && entity.isPushable()) {
						entity.addVelocity(d, 0.0, e);
					}
				}
			}
		}
	}

	public void addVelocity(double deltaX, double deltaY, double deltaZ) {
		this.setVelocity(this.getVelocity().add(deltaX, deltaY, deltaZ));
		this.velocityDirty = true;
	}

	protected void scheduleVelocityUpdate() {
		this.velocityModified = true;
	}

	/**
	 * Applies a damage to this entity. The exact implementation differs between subclasses.
	 * 
	 * <p>{@link LivingEntity} has health value, and damaging the entity decreases it. This
	 * also handles shields, extra damage to helmets for falling blocks, setting the attacker,
	 * playing hurt sound, etc.
	 * 
	 * <p>Some entities like {@link ItemEntity} also have health value, which the overridden
	 * method decrements. There also exist several entities, like {@link
	 * net.minecraft.entity.decoration.EndCrystalEntity}, where any damage discards the entity
	 * (perhaps with an explosion).
	 * 
	 * <p>If this is overridden, it must check the result of {@link #isInvulnerableTo} and
	 * return early.
	 * 
	 * @return whether the entity was actually damaged
	 * 
	 * @see #isInvulnerableTo
	 * @see LivingEntity#modifyAppliedDamage
	 */
	public boolean damage(DamageSource source, float amount) {
		if (this.isInvulnerableTo(source)) {
			return false;
		} else {
			this.scheduleVelocityUpdate();
			return false;
		}
	}

	public final Vec3d getRotationVec(float tickDelta) {
		return this.getRotationVector(this.getPitch(tickDelta), this.getYaw(tickDelta));
	}

	public float getPitch(float tickDelta) {
		return tickDelta == 1.0F ? this.getPitch() : MathHelper.lerp(tickDelta, this.prevPitch, this.getPitch());
	}

	public float getYaw(float tickDelta) {
		return tickDelta == 1.0F ? this.getYaw() : MathHelper.lerp(tickDelta, this.prevYaw, this.getYaw());
	}

	protected final Vec3d getRotationVector(float pitch, float yaw) {
		float f = pitch * (float) (Math.PI / 180.0);
		float g = -yaw * (float) (Math.PI / 180.0);
		float h = MathHelper.cos(g);
		float i = MathHelper.sin(g);
		float j = MathHelper.cos(f);
		float k = MathHelper.sin(f);
		return new Vec3d((double)(i * j), (double)(-k), (double)(h * j));
	}

	public final Vec3d getOppositeRotationVector(float tickDelta) {
		return this.getOppositeRotationVector(this.getPitch(tickDelta), this.getYaw(tickDelta));
	}

	protected final Vec3d getOppositeRotationVector(float pitch, float yaw) {
		return this.getRotationVector(pitch - 90.0F, yaw);
	}

	/**
	 * {@return the position of the eye}
	 * 
	 * @see #getEyeY
	 */
	public final Vec3d getEyePos() {
		return new Vec3d(this.getX(), this.getEyeY(), this.getZ());
	}

	public final Vec3d getCameraPosVec(float tickDelta) {
		double d = MathHelper.lerp((double)tickDelta, this.prevX, this.getX());
		double e = MathHelper.lerp((double)tickDelta, this.prevY, this.getY()) + (double)this.getStandingEyeHeight();
		double f = MathHelper.lerp((double)tickDelta, this.prevZ, this.getZ());
		return new Vec3d(d, e, f);
	}

	public Vec3d getClientCameraPosVec(float tickDelta) {
		return this.getCameraPosVec(tickDelta);
	}

	public final Vec3d getLerpedPos(float delta) {
		double d = MathHelper.lerp((double)delta, this.prevX, this.getX());
		double e = MathHelper.lerp((double)delta, this.prevY, this.getY());
		double f = MathHelper.lerp((double)delta, this.prevZ, this.getZ());
		return new Vec3d(d, e, f);
	}

	public HitResult raycast(double maxDistance, float tickDelta, boolean includeFluids) {
		Vec3d vec3d = this.getCameraPosVec(tickDelta);
		Vec3d vec3d2 = this.getRotationVec(tickDelta);
		Vec3d vec3d3 = vec3d.add(vec3d2.x * maxDistance, vec3d2.y * maxDistance, vec3d2.z * maxDistance);
		return this.getWorld()
			.raycast(
				new RaycastContext(
					vec3d, vec3d3, RaycastContext.ShapeType.OUTLINE, includeFluids ? RaycastContext.FluidHandling.ANY : RaycastContext.FluidHandling.NONE, this
				)
			);
	}

	public boolean canBeHitByProjectile() {
		return this.isAlive() && this.canHit();
	}

	/**
	 * {@return whether the entity can be hit with a projectile or be targeted by
	 * the player crosshair}
	 */
	public boolean canHit() {
		return false;
	}

	/**
	 * {@return whether the entity can be pushed by other entities}
	 */
	public boolean isPushable() {
		return false;
	}

	public void updateKilledAdvancementCriterion(Entity entityKilled, int score, DamageSource damageSource) {
		if (entityKilled instanceof ServerPlayerEntity) {
			Criteria.ENTITY_KILLED_PLAYER.trigger((ServerPlayerEntity)entityKilled, this, damageSource);
		}
	}

	public boolean shouldRender(double cameraX, double cameraY, double cameraZ) {
		double d = this.getX() - cameraX;
		double e = this.getY() - cameraY;
		double f = this.getZ() - cameraZ;
		double g = d * d + e * e + f * f;
		return this.shouldRender(g);
	}

	public boolean shouldRender(double distance) {
		double d = this.getBoundingBox().getAverageSideLength();
		if (Double.isNaN(d)) {
			d = 1.0;
		}

		d *= 64.0 * renderDistanceMultiplier;
		return distance < d * d;
	}

	public boolean saveSelfNbt(NbtCompound nbt) {
		if (this.removalReason != null && !this.removalReason.shouldSave()) {
			return false;
		} else {
			String string = this.getSavedEntityId();
			if (string == null) {
				return false;
			} else {
				nbt.putString("id", string);
				this.writeNbt(nbt);
				return true;
			}
		}
	}

	public boolean saveNbt(NbtCompound nbt) {
		return this.hasVehicle() ? false : this.saveSelfNbt(nbt);
	}

	public NbtCompound writeNbt(NbtCompound nbt) {
		try {
			if (this.vehicle != null) {
				nbt.put("Pos", this.toNbtList(this.vehicle.getX(), this.getY(), this.vehicle.getZ()));
			} else {
				nbt.put("Pos", this.toNbtList(this.getX(), this.getY(), this.getZ()));
			}

			Vec3d vec3d = this.getVelocity();
			nbt.put("Motion", this.toNbtList(vec3d.x, vec3d.y, vec3d.z));
			nbt.put("Rotation", this.toNbtList(this.getYaw(), this.getPitch()));
			nbt.putFloat("FallDistance", this.fallDistance);
			nbt.putShort("Fire", (short)this.fireTicks);
			nbt.putShort("Air", (short)this.getAir());
			nbt.putBoolean("OnGround", this.isOnGround());
			nbt.putBoolean("Invulnerable", this.invulnerable);
			nbt.putInt("PortalCooldown", this.portalCooldown);
			nbt.putUuid("UUID", this.getUuid());
			Text text = this.getCustomName();
			if (text != null) {
				nbt.putString("CustomName", Text.Serializer.toJson(text));
			}

			if (this.isCustomNameVisible()) {
				nbt.putBoolean("CustomNameVisible", this.isCustomNameVisible());
			}

			if (this.isSilent()) {
				nbt.putBoolean("Silent", this.isSilent());
			}

			if (this.hasNoGravity()) {
				nbt.putBoolean("NoGravity", this.hasNoGravity());
			}

			if (this.glowing) {
				nbt.putBoolean("Glowing", true);
			}

			int i = this.getFrozenTicks();
			if (i > 0) {
				nbt.putInt("TicksFrozen", this.getFrozenTicks());
			}

			if (this.hasVisualFire) {
				nbt.putBoolean("HasVisualFire", this.hasVisualFire);
			}

			if (!this.commandTags.isEmpty()) {
				NbtList nbtList = new NbtList();

				for (String string : this.commandTags) {
					nbtList.add(NbtString.of(string));
				}

				nbt.put("Tags", nbtList);
			}

			this.writeCustomDataToNbt(nbt);
			if (this.hasPassengers()) {
				NbtList nbtList = new NbtList();

				for (Entity entity : this.getPassengerList()) {
					NbtCompound nbtCompound = new NbtCompound();
					if (entity.saveSelfNbt(nbtCompound)) {
						nbtList.add(nbtCompound);
					}
				}

				if (!nbtList.isEmpty()) {
					nbt.put("Passengers", nbtList);
				}
			}

			return nbt;
		} catch (Throwable var9) {
			CrashReport crashReport = CrashReport.create(var9, "Saving entity NBT");
			CrashReportSection crashReportSection = crashReport.addElement("Entity being saved");
			this.populateCrashReport(crashReportSection);
			throw new CrashException(crashReport);
		}
	}

	public void readNbt(NbtCompound nbt) {
		try {
			NbtList nbtList = nbt.getList("Pos", NbtElement.DOUBLE_TYPE);
			NbtList nbtList2 = nbt.getList("Motion", NbtElement.DOUBLE_TYPE);
			NbtList nbtList3 = nbt.getList("Rotation", NbtElement.FLOAT_TYPE);
			double d = nbtList2.getDouble(0);
			double e = nbtList2.getDouble(1);
			double f = nbtList2.getDouble(2);
			this.setVelocity(Math.abs(d) > 10.0 ? 0.0 : d, Math.abs(e) > 10.0 ? 0.0 : e, Math.abs(f) > 10.0 ? 0.0 : f);
			double g = 3.0000512E7;
			this.setPos(
				MathHelper.clamp(nbtList.getDouble(0), -3.0000512E7, 3.0000512E7),
				MathHelper.clamp(nbtList.getDouble(1), -2.0E7, 2.0E7),
				MathHelper.clamp(nbtList.getDouble(2), -3.0000512E7, 3.0000512E7)
			);
			this.setYaw(nbtList3.getFloat(0));
			this.setPitch(nbtList3.getFloat(1));
			this.resetPosition();
			this.setHeadYaw(this.getYaw());
			this.setBodyYaw(this.getYaw());
			this.fallDistance = nbt.getFloat("FallDistance");
			this.fireTicks = nbt.getShort("Fire");
			if (nbt.contains("Air")) {
				this.setAir(nbt.getShort("Air"));
			}

			this.onGround = nbt.getBoolean("OnGround");
			this.invulnerable = nbt.getBoolean("Invulnerable");
			this.portalCooldown = nbt.getInt("PortalCooldown");
			if (nbt.containsUuid("UUID")) {
				this.uuid = nbt.getUuid("UUID");
				this.uuidString = this.uuid.toString();
			}

			if (!Double.isFinite(this.getX()) || !Double.isFinite(this.getY()) || !Double.isFinite(this.getZ())) {
				throw new IllegalStateException("Entity has invalid position");
			} else if (Double.isFinite((double)this.getYaw()) && Double.isFinite((double)this.getPitch())) {
				this.refreshPosition();
				this.setRotation(this.getYaw(), this.getPitch());
				if (nbt.contains("CustomName", NbtElement.STRING_TYPE)) {
					String string = nbt.getString("CustomName");

					try {
						this.setCustomName(Text.Serializer.fromJson(string));
					} catch (Exception var16) {
						LOGGER.warn("Failed to parse entity custom name {}", string, var16);
					}
				}

				this.setCustomNameVisible(nbt.getBoolean("CustomNameVisible"));
				this.setSilent(nbt.getBoolean("Silent"));
				this.setNoGravity(nbt.getBoolean("NoGravity"));
				this.setGlowing(nbt.getBoolean("Glowing"));
				this.setFrozenTicks(nbt.getInt("TicksFrozen"));
				this.hasVisualFire = nbt.getBoolean("HasVisualFire");
				if (nbt.contains("Tags", NbtElement.LIST_TYPE)) {
					this.commandTags.clear();
					NbtList nbtList4 = nbt.getList("Tags", NbtElement.STRING_TYPE);
					int i = Math.min(nbtList4.size(), 1024);

					for (int j = 0; j < i; j++) {
						this.commandTags.add(nbtList4.getString(j));
					}
				}

				this.readCustomDataFromNbt(nbt);
				if (this.shouldSetPositionOnLoad()) {
					this.refreshPosition();
				}
			} else {
				throw new IllegalStateException("Entity has invalid rotation");
			}
		} catch (Throwable var17) {
			CrashReport crashReport = CrashReport.create(var17, "Loading entity NBT");
			CrashReportSection crashReportSection = crashReport.addElement("Entity being loaded");
			this.populateCrashReport(crashReportSection);
			throw new CrashException(crashReport);
		}
	}

	protected boolean shouldSetPositionOnLoad() {
		return true;
	}

	@Nullable
	protected final String getSavedEntityId() {
		EntityType<?> entityType = this.getType();
		Identifier identifier = EntityType.getId(entityType);
		return entityType.isSaveable() && identifier != null ? identifier.toString() : null;
	}

	/**
	 * Reads custom data from {@code nbt}. Subclasses has to implement this.
	 * 
	 * <p>NBT is a storage format; therefore, a data from NBT is loaded to an entity instance's
	 * fields, which are used for other operations instead of the NBT. The data is written
	 * back to NBT when saving the entity.
	 * 
	 * <p>{@code nbt} might not have all expected keys, or might have a key whose value
	 * does not meet the requirement (such as the type or the range). This method should
	 * fall back to a reasonable default value instead of throwing an exception.
	 * 
	 * @see #writeCustomDataToNbt
	 */
	protected abstract void readCustomDataFromNbt(NbtCompound nbt);

	/**
	 * Writes custom data to {@code nbt}. Subclasses has to implement this.
	 * 
	 * <p>NBT is a storage format; therefore, a data from NBT is loaded to an entity instance's
	 * fields, which are used for other operations instead of the NBT. The data is written
	 * back to NBT when saving the entity.
	 * 
	 * @see #readCustomDataFromNbt
	 */
	protected abstract void writeCustomDataToNbt(NbtCompound nbt);

	protected NbtList toNbtList(double... values) {
		NbtList nbtList = new NbtList();

		for (double d : values) {
			nbtList.add(NbtDouble.of(d));
		}

		return nbtList;
	}

	protected NbtList toNbtList(float... values) {
		NbtList nbtList = new NbtList();

		for (float f : values) {
			nbtList.add(NbtFloat.of(f));
		}

		return nbtList;
	}

	/**
	 * Drops one {@code item} at the entity's position.
	 * 
	 * @return the spawned item entity, or {@code null} if called on the client
	 * 
	 * @see #dropItem(ItemConvertible, int)
	 * @see #dropStack(ItemStack)
	 * @see #dropStack(ItemStack, float)
	 */
	@Nullable
	public ItemEntity dropItem(ItemConvertible item) {
		return this.dropItem(item, 0);
	}

	/**
	 * Drops one {@code item} at the entity's position with the given Y offset.
	 * 
	 * @return the spawned item entity, or {@code null} if called on the client
	 * 
	 * @see #dropItem(ItemConvertible)
	 * @see #dropStack(ItemStack)
	 * @see #dropStack(ItemStack, float)
	 */
	@Nullable
	public ItemEntity dropItem(ItemConvertible item, int yOffset) {
		return this.dropStack(new ItemStack(item), (float)yOffset);
	}

	/**
	 * Drops {@code stack} at the entity's position.
	 * 
	 * @return the spawned item entity, or {@code null} if the stack is empty or if called
	 * on the client
	 * 
	 * @see #dropItem(ItemConvertible)
	 * @see #dropItem(ItemConvertible, int)
	 * @see #dropStack(ItemStack, float)
	 */
	@Nullable
	public ItemEntity dropStack(ItemStack stack) {
		return this.dropStack(stack, 0.0F);
	}

	/**
	 * Drops {@code stack} at the entity's position with the given Y offset.
	 * 
	 * @return the spawned item entity, or {@code null} if the stack is empty or if called
	 * on the client
	 * 
	 * @see #dropItem(ItemConvertible)
	 * @see #dropItem(ItemConvertible, int)
	 * @see #dropStack(ItemStack)
	 */
	@Nullable
	public ItemEntity dropStack(ItemStack stack, float yOffset) {
		if (stack.isEmpty()) {
			return null;
		} else if (this.getWorld().isClient) {
			return null;
		} else {
			ItemEntity itemEntity = new ItemEntity(this.getWorld(), this.getX(), this.getY() + (double)yOffset, this.getZ(), stack);
			itemEntity.setToDefaultPickupDelay();
			this.getWorld().spawnEntity(itemEntity);
			return itemEntity;
		}
	}

	/**
	 * {@return whether the entity is alive}
	 * 
	 * <p>For non-{@link LivingEntity}, this is the same as negating {@link #isRemoved}.
	 * {@link LivingEntity} checks the entity's health in addition to the removal.
	 */
	public boolean isAlive() {
		return !this.isRemoved();
	}

	/**
	 * {@return whether the entity is in a wall and should suffocate}
	 * 
	 * <p>This returns {@code false} if {@link #noClip} is {@code true}; otherwise,
	 * this returns {@code true} if the eye position is occupied by a {@linkplain
	 * net.minecraft.block.AbstractBlock.Settings#suffocates block that can suffocate}.
	 */
	public boolean isInsideWall() {
		if (this.noClip) {
			return false;
		} else {
			float f = this.dimensions.width * 0.8F;
			Box box = Box.of(this.getEyePos(), (double)f, 1.0E-6, (double)f);
			return BlockPos.stream(box)
				.anyMatch(
					pos -> {
						BlockState blockState = this.getWorld().getBlockState(pos);
						return !blockState.isAir()
							&& blockState.shouldSuffocate(this.getWorld(), pos)
							&& VoxelShapes.matchesAnywhere(
								blockState.getCollisionShape(this.getWorld(), pos).offset((double)pos.getX(), (double)pos.getY(), (double)pos.getZ()),
								VoxelShapes.cuboid(box),
								BooleanBiFunction.AND
							);
					}
				);
		}
	}

	/**
	 * Called when a player interacts with this entity.
	 * 
	 * @param hand the hand the player used to interact with this entity
	 * @param player the player
	 */
	public ActionResult interact(PlayerEntity player, Hand hand) {
		return ActionResult.PASS;
	}

	/**
	 * {@return whether this entity cannot occupy the same space with {@code other}}
	 * 
	 * <p>This returns {@code false} if {@code other} is {@linkplain #isConnectedThroughVehicle
	 * connected through vehicles}.
	 * 
	 * @see #isCollidable
	 */
	public boolean collidesWith(Entity other) {
		return other.isCollidable() && !this.isConnectedThroughVehicle(other);
	}

	/**
	 * {@return whether other entities cannot occupy the same space with this entity}
	 * 
	 * <p>If {@code true}, other entities can stand on this entity without falling.
	 * {@link net.minecraft.entity.vehicle.BoatEntity} and {@link
	 * net.minecraft.entity.mob.ShulkerEntity} has this behavior.
	 * 
	 * @see #collidesWith
	 */
	public boolean isCollidable() {
		return false;
	}

	public void tickRiding() {
		this.setVelocity(Vec3d.ZERO);
		this.tick();
		if (this.hasVehicle()) {
			this.getVehicle().updatePassengerPosition(this);
		}
	}

	public final void updatePassengerPosition(Entity passenger) {
		this.updatePassengerPosition(passenger, Entity::setPosition);
	}

	protected void updatePassengerPosition(Entity passenger, Entity.PositionUpdater positionUpdater) {
		if (this.hasPassenger(passenger)) {
			double d = this.getY() + this.getMountedHeightOffset() + passenger.getHeightOffset();
			positionUpdater.accept(passenger, this.getX(), d, this.getZ());
		}
	}

	public void onPassengerLookAround(Entity passenger) {
	}

	public double getHeightOffset() {
		return 0.0;
	}

	public double getMountedHeightOffset() {
		return (double)this.dimensions.height * 0.75;
	}

	/**
	 * Starts riding {@code entity}.
	 * 
	 * <p>For example, {@code player.startRiding(horse)} causes the player to ride a
	 * horse; the opposite, {@code horse.startRiding(player)}, will cause the horse
	 * to ride a player.
	 * 
	 * <p>This fails when this entity is already riding the entity (or vice versa),
	 * or when this entity {@linkplain #canStartRiding does not allow riding other entities}
	 * (or {@linkplain #canAddPassenger vice versa}).
	 * If this entity is already riding another entity, it will stop riding that entity first.
	 * 
	 * @return whether this entity successfully started riding
	 * 
	 * @see #startRiding(Entity, boolean)
	 * @see #canAddPassenger
	 * @see #canStartRiding
	 * @see #stopRiding
	 * @see #hasVehicle
	 */
	public boolean startRiding(Entity entity) {
		return this.startRiding(entity, false);
	}

	public boolean isLiving() {
		return this instanceof LivingEntity;
	}

	/**
	 * Starts riding {@code entity}.
	 * 
	 * <p>For example, {@code player.startRiding(horse)} causes the player to ride a
	 * horse; the opposite, {@code horse.startRiding(player)}, will cause the horse
	 * to ride a player.
	 * 
	 * <p>This fails when this entity is already riding the entity (or vice versa),
	 * or when this entity {@linkplain #canStartRiding does not allow riding other entities}
	 * (or {@linkplain #canAddPassenger vice versa}) unless {@code force} is {@code true}.
	 * If this entity is already riding another entity, it will stop riding that entity first.
	 * 
	 * @return whether this entity successfully started riding
	 * 
	 * @see #startRiding(Entity)
	 * @see #canAddPassenger
	 * @see #canStartRiding
	 * @see #stopRiding
	 * @see #hasVehicle
	 * 
	 * @param force whether to bypass the entity's rideability check
	 */
	public boolean startRiding(Entity entity, boolean force) {
		if (entity == this.vehicle) {
			return false;
		} else if (!entity.couldAcceptPassenger()) {
			return false;
		} else {
			for (Entity entity2 = entity; entity2.vehicle != null; entity2 = entity2.vehicle) {
				if (entity2.vehicle == this) {
					return false;
				}
			}

			if (force || this.canStartRiding(entity) && entity.canAddPassenger(this)) {
				if (this.hasVehicle()) {
					this.stopRiding();
				}

				this.setPose(EntityPose.STANDING);
				this.vehicle = entity;
				this.vehicle.addPassenger(this);
				entity.streamIntoPassengers()
					.filter(passenger -> passenger instanceof ServerPlayerEntity)
					.forEach(player -> Criteria.STARTED_RIDING.trigger((ServerPlayerEntity)player));
				return true;
			} else {
				return false;
			}
		}
	}

	/**
	 * {@return whether <strong>this entity can ride</strong> {@code entity}}
	 * 
	 * <p>Returning {@code false} causes the entity to be unable to ride other entities. For
	 * example, {@link net.minecraft.entity.boss.WitherEntity} overrides this to return
	 * {@code false}, so withers cannot ride boats or minecarts. Note that this check can be
	 * bypassed by passing {@code true} to {@link #startRiding(Entity, boolean)}.
	 * 
	 * <p>This is the opposite of {@link #canAddPassenger}.
	 * 
	 * @see #startRiding(Entity)
	 * @see #startRiding(Entity, boolean)
	 * @see #canAddPassenger
	 * @see #stopRiding
	 * @see #hasVehicle
	 */
	protected boolean canStartRiding(Entity entity) {
		return !this.isSneaking() && this.ridingCooldown <= 0;
	}

	/**
	 * {@return {@code true} if the entity would not collide with blocks if the pose is
	 * {@code pose}}
	 */
	protected boolean wouldPoseNotCollide(EntityPose pose) {
		return this.getWorld().isSpaceEmpty(this, this.calculateBoundsForPose(pose).contract(1.0E-7));
	}

	/**
	 * Causes all passengers of this entity to stop riding this entity.
	 * 
	 * <p>For example, {@code boat.removeAllPassengers()} will dismount all passengers of
	 * the boat.
	 * 
	 * @see #stopRiding
	 */
	public void removeAllPassengers() {
		for (int i = this.passengerList.size() - 1; i >= 0; i--) {
			((Entity)this.passengerList.get(i)).stopRiding();
		}
	}

	/**
	 * Dismounts the vehicle if present.
	 * <p>
	 * For players, will not trigger any networking changes. Use {@link #stopRiding()} instead.
	 * 
	 * @see #stopRiding()
	 */
	public void dismountVehicle() {
		if (this.vehicle != null) {
			Entity entity = this.vehicle;
			this.vehicle = null;
			entity.removePassenger(this);
		}
	}

	/**
	 * Stops riding the vehicle if present.
	 * 
	 * <p>For example, if {@code player} is riding on a horse, {@code player.stopRiding()}
	 * will dismount that player from the horse.
	 * 
	 * @see #removeAllPassengers
	 */
	public void stopRiding() {
		this.dismountVehicle();
	}

	/**
	 * Adds {@code passenger} as a passenger. <strong>This should not be called
	 * normally; call {@link #startRiding(Entity)} instead.</strong> (Note that
	 * the entity to pass and the entity to call are swapped in this case;
	 * {@code entity.startRiding(vehicle)} is the equivalent of {@code
	 * vehicle.addPassenger(entity)}.)
	 * 
	 * @throws IllegalStateException when the method is called directly
	 */
	protected void addPassenger(Entity passenger) {
		if (passenger.getVehicle() != this) {
			throw new IllegalStateException("Use x.startRiding(y), not y.addPassenger(x)");
		} else {
			if (this.passengerList.isEmpty()) {
				this.passengerList = ImmutableList.of(passenger);
			} else {
				List<Entity> list = Lists.<Entity>newArrayList(this.passengerList);
				if (!this.getWorld().isClient && passenger instanceof PlayerEntity && !(this.getFirstPassenger() instanceof PlayerEntity)) {
					list.add(0, passenger);
				} else {
					list.add(passenger);
				}

				this.passengerList = ImmutableList.copyOf(list);
			}

			this.emitGameEvent(GameEvent.ENTITY_MOUNT, passenger);
		}
	}

	/**
	 * Removes {@code passenger} from the passengers. <strong>This should not be called
	 * normally; call {@link #stopRiding} instead.</strong> (Note that vehicles are not
	 * passed to that method; {@code entity.stopRiding()} is the equivalent of {@code
	 * vehicle.removePassenger(entity)}.)
	 * 
	 * @throws IllegalStateException when the method is called directly
	 */
	protected void removePassenger(Entity passenger) {
		if (passenger.getVehicle() == this) {
			throw new IllegalStateException("Use x.stopRiding(y), not y.removePassenger(x)");
		} else {
			if (this.passengerList.size() == 1 && this.passengerList.get(0) == passenger) {
				this.passengerList = ImmutableList.of();
			} else {
				this.passengerList = (ImmutableList<Entity>)this.passengerList.stream().filter(entity -> entity != passenger).collect(ImmutableList.toImmutableList());
			}

			passenger.ridingCooldown = 60;
			this.emitGameEvent(GameEvent.ENTITY_DISMOUNT, passenger);
		}
	}

	/**
	 * {@return whether {@code entity} <strong>can ride this entity</strong>}
	 * 
	 * <p>Returning {@code false} causes other entities to be unable to ride this entity. For
	 * example, {@link net.minecraft.entity.vehicle.BoatEntity} uses this to restrict how many
	 * passengers can ride the same boat (2 for normal, 1 for chest boat).
	 * 
	 * <p>This is the opposite of {@link #canStartRiding}.
	 * 
	 * @see #startRiding(Entity)
	 * @see #startRiding(Entity, boolean)
	 * @see #canStartRiding
	 * @see #stopRiding
	 * @see #hasVehicle
	 */
	protected boolean canAddPassenger(Entity passenger) {
		return this.passengerList.isEmpty();
	}

	/**
	 * {@return {@code true} if this entity supports passengers in general}
	 */
	protected boolean couldAcceptPassenger() {
		return true;
	}

	public void updateTrackedPositionAndAngles(double x, double y, double z, float yaw, float pitch, int interpolationSteps, boolean interpolate) {
		this.setPosition(x, y, z);
		this.setRotation(yaw, pitch);
	}

	public void updateTrackedHeadRotation(float yaw, int interpolationSteps) {
		this.setHeadYaw(yaw);
	}

	/**
	 * {@return the margin around the entity's bounding box where the entity
	 * targeting is still successful}
	 * 
	 * @apiNote {@link net.minecraft.entity.projectile.ExplosiveProjectileEntity}
	 * overrides this method to return {@code 1.0f}, which expands the ghast fireball's
	 * effective hitbox.
	 */
	public float getTargetingMargin() {
		return 0.0F;
	}

	public Vec3d getRotationVector() {
		return this.getRotationVector(this.getPitch(), this.getYaw());
	}

	/**
	 * {@return the offset of the hand that holds {@code item}}
	 * 
	 * <p>This returns {@link Vec3d#ZERO} if the entity is not a player.
	 * 
	 * @apiNote The offset is applied to the position of the firework rocket particle
	 * when used by players.
	 */
	public Vec3d getHandPosOffset(Item item) {
		if (!(this instanceof PlayerEntity playerEntity)) {
			return Vec3d.ZERO;
		} else {
			boolean bl = playerEntity.getOffHandStack().isOf(item) && !playerEntity.getMainHandStack().isOf(item);
			Arm arm = bl ? playerEntity.getMainArm().getOpposite() : playerEntity.getMainArm();
			return this.getRotationVector(0.0F, this.getYaw() + (float)(arm == Arm.RIGHT ? 80 : -80)).multiply(0.5);
		}
	}

	public Vec2f getRotationClient() {
		return new Vec2f(this.getPitch(), this.getYaw());
	}

	public Vec3d getRotationVecClient() {
		return Vec3d.fromPolar(this.getRotationClient());
	}

	public void setInNetherPortal(BlockPos pos) {
		if (this.hasPortalCooldown()) {
			this.resetPortalCooldown();
		} else {
			if (!this.getWorld().isClient && !pos.equals(this.lastNetherPortalPosition)) {
				this.lastNetherPortalPosition = pos.toImmutable();
			}

			this.inNetherPortal = true;
		}
	}

	protected void tickPortal() {
		if (this.getWorld() instanceof ServerWorld) {
			int i = this.getMaxNetherPortalTime();
			ServerWorld serverWorld = (ServerWorld)this.getWorld();
			if (this.inNetherPortal) {
				MinecraftServer minecraftServer = serverWorld.getServer();
				RegistryKey<World> registryKey = this.getWorld().getRegistryKey() == World.NETHER ? World.OVERWORLD : World.NETHER;
				ServerWorld serverWorld2 = minecraftServer.getWorld(registryKey);
				if (serverWorld2 != null && minecraftServer.isNetherAllowed() && !this.hasVehicle() && this.netherPortalTime++ >= i) {
					this.getWorld().getProfiler().push("portal");
					this.netherPortalTime = i;
					this.resetPortalCooldown();
					this.moveToWorld(serverWorld2);
					this.getWorld().getProfiler().pop();
				}

				this.inNetherPortal = false;
			} else {
				if (this.netherPortalTime > 0) {
					this.netherPortalTime -= 4;
				}

				if (this.netherPortalTime < 0) {
					this.netherPortalTime = 0;
				}
			}

			this.tickPortalCooldown();
		}
	}

	/**
	 * {@return the entity's default portal cooldown}
	 * 
	 * <p>This is 300 ticks by default, or 10 ticks for players.
	 * 
	 * @see #resetPortalCooldown
	 */
	public int getDefaultPortalCooldown() {
		return 300;
	}

	public void setVelocityClient(double x, double y, double z) {
		this.setVelocity(x, y, z);
	}

	public void onDamaged(DamageSource damageSource) {
	}

	/**
	 * Called on the client when the entity receives an entity status from the server.
	 * They are often used to spawn particles or play sounds.
	 * Subclasses can override this method to handle custom entity status.
	 * 
	 * @apiNote To send an entity status, use {@link World#sendEntityStatus}.
	 * 
	 * @see net.minecraft.entity.EntityStatuses
	 */
	public void handleStatus(byte status) {
		switch (status) {
			case 53:
				HoneyBlock.addRegularParticles(this);
		}
	}

	/**
	 * Called on the client to animate the entity's damage (the wobble).
	 */
	public void animateDamage(float yaw) {
	}

	/**
	 * {@return an iterable of item stacks held in the hands}
	 * 
	 * @see #getArmorItems
	 * @see #getItemsEquipped
	 */
	public Iterable<ItemStack> getHandItems() {
		return EMPTY_STACK_LIST;
	}

	/**
	 * {@return an iterable of item stacks equipped as armor}
	 * 
	 * @see #getHandItems
	 * @see #getItemsEquipped
	 */
	public Iterable<ItemStack> getArmorItems() {
		return EMPTY_STACK_LIST;
	}

	/**
	 * {@return an iterable of item stacks held in the hands or equipped as armor}
	 * 
	 * @see #getHandItems
	 * @see #getArmorItems
	 */
	public Iterable<ItemStack> getItemsEquipped() {
		return Iterables.concat(this.getHandItems(), this.getArmorItems());
	}

	/**
	 * Equips {@code stack} at {@code slot}.This is also used to set an entity's
	 * mainhand or offhand stack. This overwrites any stacks present in that slot
	 * without dropping them.
	 */
	public void equipStack(EquipmentSlot slot, ItemStack stack) {
	}

	/**
	 * {@return whether the entity is on fire and is not fire immune}
	 * 
	 * @see #isFireImmune
	 */
	public boolean isOnFire() {
		boolean bl = this.getWorld() != null && this.getWorld().isClient;
		return !this.isFireImmune() && (this.fireTicks > 0 || bl && this.getFlag(ON_FIRE_FLAG_INDEX));
	}

	/**
	 * {@return whether this entity is riding an entity}
	 * 
	 * <p>This is the opposite of {@link #hasPassengers}.
	 * 
	 * @see #startRiding(Entity)
	 * @see #startRiding(Entity, boolean)
	 * @see #stopRiding
	 * @see #hasPassengers
	 */
	public boolean hasVehicle() {
		return this.getVehicle() != null;
	}

	/**
	 * {@return whether another entity is riding this entity}
	 * 
	 * <p>This is the opposite of {@link #hasVehicle}.
	 * 
	 * @see #startRiding(Entity)
	 * @see #startRiding(Entity, boolean)
	 * @see #stopRiding
	 * @see #hasVehicle
	 */
	public boolean hasPassengers() {
		return !this.passengerList.isEmpty();
	}

	/**
	 * {@return whether this vehicle should dismount the passenger if submerged underwater}
	 */
	public boolean shouldDismountUnderwater() {
		return this.getType().isIn(EntityTypeTags.DISMOUNTS_UNDERWATER);
	}

	/**
	 * Sets whether the entity is sneaking.
	 * 
	 * @see #isSneaking
	 * @see #isInSneakingPose
	 */
	public void setSneaking(boolean sneaking) {
		this.setFlag(SNEAKING_FLAG_INDEX, sneaking);
	}

	/**
	 * {@return whether the entity is sneaking}
	 * 
	 * <p>This only returns {@code true} if the entity is a player and that player
	 * is pressing the Sneak key. See also {@link #isInSneakingPose}.
	 * 
	 * @see #setSneaking
	 * @see #isInSneakingPose
	 */
	public boolean isSneaking() {
		return this.getFlag(SNEAKING_FLAG_INDEX);
	}

	/**
	 * {@return whether the entity should bypass effects caused by stepping}
	 * 
	 * <p>This returns {@link #isSneaking} by default.
	 * 
	 * @apiNote Stepping effects include magma blocks dealing fire damage, turtle eggs
	 * breaking, or sculk sensors triggering.
	 * 
	 * @see #bypassesLandingEffects
	 */
	public boolean bypassesSteppingEffects() {
		return this.isSneaking();
	}

	/**
	 * {@return whether the entity should bypass effects caused by landing on a block}
	 * 
	 * <p>This returns {@link #isSneaking} by default.
	 * 
	 * @apiNote Landing effects include slime blocks nullifying the fall damage and
	 * slime blocks and beds bouncing the entity.
	 * 
	 * @see #bypassesSteppingEffects
	 */
	public boolean bypassesLandingEffects() {
		return this.isSneaking();
	}

	public boolean isSneaky() {
		return this.isSneaking();
	}

	/**
	 * {@return whether the entity is actively descending}
	 * 
	 * <p>This affects scaffolding and powder snow (if the entity can walk on it), and
	 * returns {@link #isSneaking} by default. This returns {@code false} for entities
	 * descending a ladder, since the entity is not actively doing so, instead letting
	 * the gravity to do so.
	 */
	public boolean isDescending() {
		return this.isSneaking();
	}

	/**
	 * {@return whether the entity is in a crouching pose}
	 * 
	 * <p>Compared to {@link #isSneaking()}, it only makes the entity appear
	 * crouching and does not bring other effects of sneaking, such as no less
	 * obvious name label rendering, no dismounting while riding, etc.
	 * 
	 * <p>This is used by vanilla for non-player entities to crouch, such as
	 * for foxes and cats. This is also used when the entity is a player and
	 * the player would otherwise collide with blocks (for example, when the
	 * player is in a 1.5 blocks tall tunnel).
	 */
	public boolean isInSneakingPose() {
		return this.isInPose(EntityPose.CROUCHING);
	}

	/**
	 * {@return whether the entity is sprinting}
	 * 
	 * <p>Swimming is also considered as sprinting.
	 * 
	 * #setSprinting
	 */
	public boolean isSprinting() {
		return this.getFlag(SPRINTING_FLAG_INDEX);
	}

	/**
	 * Sets whether the entity is sprinting.
	 * 
	 * @see #isSprinting
	 */
	public void setSprinting(boolean sprinting) {
		this.setFlag(SPRINTING_FLAG_INDEX, sprinting);
	}

	/**
	 * {@return whether the entity is swimming}
	 * 
	 * <p>An entity is swimming if it is touching water, not riding any entities, and is
	 * sprinting. Note that to start swimming, the entity must first be submerged in
	 * water.
	 * 
	 * @see #setSwimming
	 */
	public boolean isSwimming() {
		return this.getFlag(SWIMMING_FLAG_INDEX);
	}

	/**
	 * {@return whether the entity is in swimming pose}
	 * 
	 * <p>This includes crawling entities and entities using elytra that aren't fall-flying.
	 * Players start crawling if they would otherwise collide with blocks (for example,
	 * when the player is in a 1 block tall tunnel).
	 * 
	 * @see #isCrawling
	 */
	public boolean isInSwimmingPose() {
		return this.isInPose(EntityPose.SWIMMING);
	}

	/**
	 * {@return whether the entity is crawling}
	 * 
	 * <p>An entity is crawling if it is in swimming pose, but is not touching water.
	 * Players start crawling if they would otherwise collide with blocks (for example,
	 * when the player is in a 1 block tall tunnel).
	 * 
	 * @see #isInSwimmingPose
	 */
	public boolean isCrawling() {
		return this.isInSwimmingPose() && !this.isTouchingWater();
	}

	/**
	 * Sets whether the entity is swimming.
	 * 
	 * @see #isSwimming
	 */
	public void setSwimming(boolean swimming) {
		this.setFlag(SWIMMING_FLAG_INDEX, swimming);
	}

	/**
	 * {@return whether the entity is glowing, without checking the entity flags}
	 * 
	 * @apiNote This is only used to copy entity data to NBT when bucketing.
	 * 
	 * @see #isGlowing
	 * @see #setGlowing
	 */
	public final boolean isGlowingLocal() {
		return this.glowing;
	}

	/**
	 * Sets whether the entity is glowing.
	 * 
	 * <p>Glowing entities have an outline when rendered.
	 * 
	 * @see #isGlowing
	 */
	public final void setGlowing(boolean glowing) {
		this.glowing = glowing;
		this.setFlag(GLOWING_FLAG_INDEX, this.isGlowing());
	}

	/**
	 * {@return whether the entity is glowing, checking the entity flags on the client}
	 * 
	 * <p>Glowing entities have an outline when rendered.
	 * 
	 * @see #isGlowingLocal
	 * @see #setGlowing
	 */
	public boolean isGlowing() {
		return this.getWorld().isClient() ? this.getFlag(GLOWING_FLAG_INDEX) : this.glowing;
	}

	/**
	 * {@return whether the entity is invisible to everyone}
	 * 
	 * <p>Invisibility status effect and {@link
	 * net.minecraft.entity.decoration.ArmorStandEntity}'s {@code Invisible} NBT key can
	 * cause an entity to be invisible.
	 * 
	 * @see #isInvisibleTo
	 * @see #setInvisible
	 */
	public boolean isInvisible() {
		return this.getFlag(INVISIBLE_FLAG_INDEX);
	}

	/**
	 * {@return whether the entity is invisible to {@code player}}
	 * 
	 * <p>Spectators can see all entities, and entities on the same team as player's can
	 * see all entities if {@link AbstractTeam#shouldShowFriendlyInvisibles} returns
	 * {@code true}. Otherwise, this returns {@link #isInvisible}.
	 * 
	 * @see AbstractTeam#shouldShowFriendlyInvisibles
	 * @see #isInvisible
	 */
	public boolean isInvisibleTo(PlayerEntity player) {
		if (player.isSpectator()) {
			return false;
		} else {
			AbstractTeam abstractTeam = this.getScoreboardTeam();
			return abstractTeam != null && player != null && player.getScoreboardTeam() == abstractTeam && abstractTeam.shouldShowFriendlyInvisibles()
				? false
				: this.isInvisible();
		}
	}

	public boolean isOnRail() {
		return false;
	}

	/**
	 * Called when the entity is loaded to register game event handlers.
	 * 
	 * <p>Entities that listen to game events should first create an instance of
	 * {@link net.minecraft.world.event.listener.EntityGameEventHandler} in the
	 * entity's constructor, and override this to call {@code callback}. For example:
	 * 
	 * <pre>{@code
	 * if (this.world instanceof ServerWorld serverWorld) {
	 *   callback.accept(this.handler, serverWorld);
	 * }
	 * }</pre>
	 */
	public void updateEventHandler(BiConsumer<EntityGameEventHandler<?>, ServerWorld> callback) {
	}

	/**
	 * {@return the scoreboard team the entity belongs to, or {@code null} if there is none}
	 */
	@Nullable
	public AbstractTeam getScoreboardTeam() {
		return this.getWorld().getScoreboard().getPlayerTeam(this.getEntityName());
	}

	/**
	 * {@return whether this entity and {@code other} are in the same team}
	 * 
	 * <p>This returns {@code false} if this entity is not in any team.
	 */
	public boolean isTeammate(Entity other) {
		return this.isTeamPlayer(other.getScoreboardTeam());
	}

	/**
	 * {@return whether this entity is in {@code team}}
	 * 
	 * <p>This returns {@code false} if this entity is not in any team.
	 */
	public boolean isTeamPlayer(AbstractTeam team) {
		return this.getScoreboardTeam() != null ? this.getScoreboardTeam().isEqual(team) : false;
	}

	/**
	 * Sets whether the entity is invisible to everyone.
	 * 
	 * <p>Invisibility status effect and {@link
	 * net.minecraft.entity.decoration.ArmorStandEntity}'s {@code Invisible} NBT key can
	 * cause an entity to be invisible.
	 * 
	 * @see #isInvisible
	 * @see #isInvisibleTo
	 */
	public void setInvisible(boolean invisible) {
		this.setFlag(INVISIBLE_FLAG_INDEX, invisible);
	}

	/**
	 * {@return the entity flag with index {@code flag}}
	 * 
	 * <p>Entity flag is used to track whether the entity is sneaking, sprinting, invisible,
	 * etc.
	 */
	protected boolean getFlag(int index) {
		return (this.dataTracker.get(FLAGS) & 1 << index) != 0;
	}

	/**
	 * Sets the entity flag with index {@code flag} to {@code value}.
	 * 
	 * <p>Entity flag is used to track whether the entity is sneaking, sprinting, invisible,
	 * etc.
	 */
	protected void setFlag(int index, boolean value) {
		byte b = this.dataTracker.get(FLAGS);
		if (value) {
			this.dataTracker.set(FLAGS, (byte)(b | 1 << index));
		} else {
			this.dataTracker.set(FLAGS, (byte)(b & ~(1 << index)));
		}
	}

	/**
	 * {@return the maximum amount of air the entity can hold, in ticks}
	 * 
	 * <p>Most entities have the max air of 300 ticks, or 15 seconds.
	 * {@link net.minecraft.entity.passive.DolphinEntity} has 4800 ticks or 4
	 * minutes; {@link net.minecraft.entity.passive.AxolotlEntity} has 6000 ticks
	 * or 5 minutes. Note that this does not include enchantments.
	 * 
	 * @see #getAir
	 * @see #setAir
	 */
	public int getMaxAir() {
		return 300;
	}

	/**
	 * {@return the air left for the entity, in ticks}
	 * 
	 * <p>Air is decremented every tick if the entity's eye is submerged in water.
	 * If this is {@code -20}, the air will be reset to {@code 0} and the entity takes
	 * a drowning damage.
	 * 
	 * @apiNote {@link net.minecraft.entity.mob.WaterCreatureEntity} reuses the air to
	 * indicate the entity's air breathed when the entity is in water. If the entity is
	 * not touching a water, the air decrements, and the entity drowns in the same way
	 * as other entities.
	 * 
	 * @see #getMaxAir
	 * @see #setAir
	 * @see net.minecraft.entity.mob.WaterCreatureEntity#tickWaterBreathingAir
	 */
	public int getAir() {
		return this.dataTracker.get(AIR);
	}

	/**
	 * Sets the air left for the entity in ticks.
	 * 
	 * <p>Air is decremented every tick if the entity's eye is submerged in water.
	 * If this is {@code -20}, the air will be reset to {@code 0} and the entity takes
	 * a drowning damage.
	 * 
	 * @apiNote {@link net.minecraft.entity.mob.WaterCreatureEntity} reuses the air to
	 * indicate the entity's air breathed when the entity is in water. If the entity is
	 * not touching a water, the air decrements, and the entity drowns in the same way
	 * as other entities.
	 * 
	 * @see #getMaxAir
	 * @see #getAir
	 * @see net.minecraft.entity.mob.WaterCreatureEntity#tickWaterBreathingAir
	 */
	public void setAir(int air) {
		this.dataTracker.set(AIR, air);
	}

	/**
	 * {@return how long the entity is freezing, in ticks}
	 * 
	 * <p>If this is equal to or above {@link #getMinFreezeDamageTicks}, the entity
	 * receives freezing damage.
	 * 
	 * @see #setFrozenTicks
	 * @see #getFreezingScale
	 * @see #isFrozen
	 * @see #getMinFreezeDamageTicks
	 */
	public int getFrozenTicks() {
		return this.dataTracker.get(FROZEN_TICKS);
	}

	/**
	 * Sets how long the entity is freezing in ticks.
	 * 
	 * <p>If this is equal to or above {@link #getMinFreezeDamageTicks}, the entity
	 * receives freezing damage.
	 * 
	 * @see #setFrozenTicks
	 * @see #getFreezingScale
	 * @see #isFrozen
	 * @see #getMinFreezeDamageTicks
	 */
	public void setFrozenTicks(int frozenTicks) {
		this.dataTracker.set(FROZEN_TICKS, frozenTicks);
	}

	/**
	 * {@return the current freezing scale}
	 * 
	 * <p>Freezing scale is calculated as {@code
	 * Math.min(1, getFrozenTicks() / getMinFreezeDamageTicks())}.
	 * 
	 * @see #setFrozenTicks
	 * @see #getFrozenTicks
	 * @see #isFrozen
	 * @see #getMinFreezeDamageTicks
	 */
	public float getFreezingScale() {
		int i = this.getMinFreezeDamageTicks();
		return (float)Math.min(this.getFrozenTicks(), i) / (float)i;
	}

	/**
	 * {@return whether the entity is frozen}
	 * 
	 * <p>Frozen entities take freezing damage. Entity becomes frozen {@link
	 * #getMinFreezeDamageTicks} ticks after starting to freeze.
	 * 
	 * @see #getFrozenTicks
	 * @see #setFrozenTicks
	 * @see #getFreezingScale
	 * @see #getMinFreezeDamageTicks
	 */
	public boolean isFrozen() {
		return this.getFrozenTicks() >= this.getMinFreezeDamageTicks();
	}

	/**
	 * {@return how long it takes for the entity to be completely frozen and receive
	 * freezing damage, in ticks}
	 * 
	 * @see #getFrozenTicks
	 * @see #setFrozenTicks
	 * @see #getFreezingScale
	 * @see #isFrozen
	 */
	public int getMinFreezeDamageTicks() {
		return 140;
	}

	/**
	 * Called when the entity is struck by lightning. This sets the entity on fire and
	 * deals lightning damage by default; entities that do not take such damage should
	 * override this method to do nothing.
	 */
	public void onStruckByLightning(ServerWorld world, LightningEntity lightning) {
		this.setFireTicks(this.fireTicks + 1);
		if (this.fireTicks == 0) {
			this.setOnFireFor(8);
		}

		this.damage(this.getDamageSources().lightningBolt(), 5.0F);
	}

	/**
	 * Called when the entity collides with a bubble column with an air above.
	 * 
	 * <p>This applies the bubble column velocity by default. {@link
	 * net.minecraft.entity.vehicle.BoatEntity} uses this to spawn splash particles.
	 * 
	 * @param drag whether the entity should be dragged downwards
	 */
	public void onBubbleColumnSurfaceCollision(boolean drag) {
		Vec3d vec3d = this.getVelocity();
		double d;
		if (drag) {
			d = Math.max(-0.9, vec3d.y - 0.03);
		} else {
			d = Math.min(1.8, vec3d.y + 0.1);
		}

		this.setVelocity(vec3d.x, d, vec3d.z);
	}

	/**
	 * Called when the entity collides with a bubble column without an air above.
	 * 
	 * <p>This applies the bubble column velocity by default.
	 * 
	 * @param drag whether the entity should be dragged downwards
	 */
	public void onBubbleColumnCollision(boolean drag) {
		Vec3d vec3d = this.getVelocity();
		double d;
		if (drag) {
			d = Math.max(-0.3, vec3d.y - 0.03);
		} else {
			d = Math.min(0.7, vec3d.y + 0.06);
		}

		this.setVelocity(vec3d.x, d, vec3d.z);
		this.onLanding();
	}

	/**
	 * Called when this entity kills {@code other}.
	 * 
	 * @apiNote {@link net.minecraft.entity.mob.ZombieEntity} overrides this to convert the
	 * killed villager to a zombie villager.
	 * 
	 * @return whether the entity died (and not converted to another entity)
	 */
	public boolean onKilledOther(ServerWorld world, LivingEntity other) {
		return true;
	}

	/**
	 * Called when this entity is fall flying or on a lead.
	 * 
	 * <p>Limits this entity's {@code fallDistance} if its downward velocity isn't fast enough
	 * in order to prevent unwarranted fall damage.
	 */
	public void limitFallDistance() {
		if (this.getVelocity().getY() > -0.5 && this.fallDistance > 1.0F) {
			this.fallDistance = 1.0F;
		}
	}

	/**
	 * Called when the entity lands on a block.
	 */
	public void onLanding() {
		this.fallDistance = 0.0F;
	}

	/**
	 * Pushes this entity out of blocks.
	 * 
	 * @apiNote This is used by {@link ItemEntity} and {@link ExperienceOrbEntity}.
	 * 
	 * @param x the entity's X position
	 * @param y the entity bounding box's center Y position
	 * @param z the entity's Z position
	 */
	protected void pushOutOfBlocks(double x, double y, double z) {
		BlockPos blockPos = BlockPos.ofFloored(x, y, z);
		Vec3d vec3d = new Vec3d(x - (double)blockPos.getX(), y - (double)blockPos.getY(), z - (double)blockPos.getZ());
		BlockPos.Mutable mutable = new BlockPos.Mutable();
		Direction direction = Direction.UP;
		double d = Double.MAX_VALUE;

		for (Direction direction2 : new Direction[]{Direction.NORTH, Direction.SOUTH, Direction.WEST, Direction.EAST, Direction.UP}) {
			mutable.set(blockPos, direction2);
			if (!this.getWorld().getBlockState(mutable).isFullCube(this.getWorld(), mutable)) {
				double e = vec3d.getComponentAlongAxis(direction2.getAxis());
				double f = direction2.getDirection() == Direction.AxisDirection.POSITIVE ? 1.0 - e : e;
				if (f < d) {
					d = f;
					direction = direction2;
				}
			}
		}

		float g = this.random.nextFloat() * 0.2F + 0.1F;
		float h = (float)direction.getDirection().offset();
		Vec3d vec3d2 = this.getVelocity().multiply(0.75);
		if (direction.getAxis() == Direction.Axis.X) {
			this.setVelocity((double)(h * g), vec3d2.y, vec3d2.z);
		} else if (direction.getAxis() == Direction.Axis.Y) {
			this.setVelocity(vec3d2.x, (double)(h * g), vec3d2.z);
		} else if (direction.getAxis() == Direction.Axis.Z) {
			this.setVelocity(vec3d2.x, vec3d2.y, (double)(h * g));
		}
	}

	/**
	 * Calls {@link #onLanding} and slows this entity.
	 * 
	 * <p>This means that the entity will avoid taking fall damage.
	 * 
	 * @apiNote This should be called inside {@link
	 * net.minecraft.block.AbstractBlock#onEntityCollision}. This is used by cobwebs,
	 * sweet berries, and powder snow.
	 */
	public void slowMovement(BlockState state, Vec3d multiplier) {
		this.onLanding();
		this.movementMultiplier = multiplier;
	}

	private static Text removeClickEvents(Text textComponent) {
		MutableText mutableText = textComponent.copyContentOnly().setStyle(textComponent.getStyle().withClickEvent(null));

		for (Text text : textComponent.getSiblings()) {
			mutableText.append(removeClickEvents(text));
		}

		return mutableText;
	}

	@Override
	public Text getName() {
		Text text = this.getCustomName();
		return text != null ? removeClickEvents(text) : this.getDefaultName();
	}

	/**
	 * {@return the default name of the entity}
	 * 
	 * @see EntityType#getName
	 */
	protected Text getDefaultName() {
		return this.type.getName();
	}

	/**
	 * {@return whether this entity is part of {@code entity}}
	 * 
	 * <p>This is just an equality check for all entities except the ender dragon part.
	 * An ender dragon is composed of several entity parts; each part returns {@code true}
	 * for {@code part.isPartOf(dragon)}.
	 */
	public boolean isPartOf(Entity entity) {
		return this == entity;
	}

	/**
	 * {@return the head yaw of the entity}
	 * 
	 * @see #setHeadYaw
	 */
	public float getHeadYaw() {
		return 0.0F;
	}

	/**
	 * Sets the head yaw of this entity.
	 * 
	 * @see #getHeadYaw
	 */
	public void setHeadYaw(float headYaw) {
	}

	/**
	 * Sets the body yaw of this entity.
	 * 
	 * @see #getBodyYaw
	 */
	public void setBodyYaw(float bodyYaw) {
	}

	/**
	 * {@return whether the entity can be attacked by players}
	 * 
	 * <p>Note that this is not called for most entities defined in vanilla as unattackable
	 * (such as {@link ItemEntity} and {@link ExperienceOrbEntity}) as trying to attack them
	 * kicks the player.
	 * 
	 * @see net.minecraft.server.network.ServerPlayNetworkHandler#onPlayerInteractEntity
	 */
	public boolean isAttackable() {
		return true;
	}

	/**
	 * Handles a player attacking the entity. This is called before {@link
	 * #damage} and can be used to restrict players from attacking the entity
	 * by returning {@code true}.
	 * 
	 * @apiNote For example, {@link net.minecraft.entity.decoration.ArmorStandEntity}
	 * checks whether the player can modify blocks at the entity's position.
	 * 
	 * @return whether to stop handling the attack
	 * 
	 * @see World#canPlayerModifyAt
	 */
	public boolean handleAttack(Entity attacker) {
		return false;
	}

	public String toString() {
		String string = this.getWorld() == null ? "~NULL~" : this.getWorld().toString();
		return this.removalReason != null
			? String.format(
				Locale.ROOT,
				"%s['%s'/%d, l='%s', x=%.2f, y=%.2f, z=%.2f, removed=%s]",
				this.getClass().getSimpleName(),
				this.getName().getString(),
				this.id,
				string,
				this.getX(),
				this.getY(),
				this.getZ(),
				this.removalReason
			)
			: String.format(
				Locale.ROOT,
				"%s['%s'/%d, l='%s', x=%.2f, y=%.2f, z=%.2f]",
				this.getClass().getSimpleName(),
				this.getName().getString(),
				this.id,
				string,
				this.getX(),
				this.getY(),
				this.getZ()
			);
	}

	/**
	 * {@return whether the entity is invulnerable to {@code damageSource}}
	 * 
	 * <p>This can be overridden to make the entity invulnerable to some damages, but
	 * {@code super.isInvulnerableTo()} should be called in this case.
	 * 
	 * @implNote Entity is invulnerable to all damages if it is {@linkplain #isRemoved
	 * removed}, and is invulnerable to all damages except {@link
	 * net.minecraft.entity.damage.DamageTypes#OUT_OF_WORLD}
	 * or damages from creative mode players if the entity is {@linkplain #isInvulnerable
	 * invulnerable}. This also checks {@link #isFireImmune}.
	 * 
	 * @see net.minecraft.entity.damage.DamageSources
	 * @see net.minecraft.registry.tag.DamageTypeTags
	 * @see #isFireImmune
	 * @see #damage
	 * @see #isInvulnerable
	 */
	public boolean isInvulnerableTo(DamageSource damageSource) {
		return this.isRemoved()
			|| this.invulnerable && !damageSource.isIn(DamageTypeTags.BYPASSES_INVULNERABILITY) && !damageSource.isSourceCreativePlayer()
			|| damageSource.isIn(DamageTypeTags.IS_FIRE) && this.isFireImmune()
			|| damageSource.isIn(DamageTypeTags.IS_FALL) && this.getType().isIn(EntityTypeTags.FALL_DAMAGE_IMMUNE);
	}

	/**
	 * {@return whether the entity is invulnerable}
	 * 
	 * <p>This is saved on the {@code Invulnerable} NBT key.
	 * 
	 * @implNote Invulnerable entities are immune from all damages except {@link
	 * net.minecraft.entity.damage.DamageTypes#OUT_OF_WORLD}
	 * and damages by creative mode players by default.
	 * 
	 * @see #isInvulnerableTo
	 * @see #setInvulnerable
	 */
	public boolean isInvulnerable() {
		return this.invulnerable;
	}

	/**
	 * Sets whether the entity is invulnerable.
	 * 
	 * <p>This is saved on the {@code Invulnerable} NBT key.
	 * 
	 * @implNote Invulnerable entities are immune from all damages except {@link
	 * net.minecraft.entity.damage.DamageTypes#OUT_OF_WORLD}
	 * and damages by creative mode players by default.
	 * 
	 * @see #isInvulnerableTo
	 * @see #isInvulnerable
	 */
	public void setInvulnerable(boolean invulnerable) {
		this.invulnerable = invulnerable;
	}

	/**
	 * Sets the entity's position and rotation the same as {@code entity}.
	 * 
	 * @see #refreshPositionAndAngles(double, double, double, float, float)
	 */
	public void copyPositionAndRotation(Entity entity) {
		this.refreshPositionAndAngles(entity.getX(), entity.getY(), entity.getZ(), entity.getYaw(), entity.getPitch());
	}

	/**
	 * Copies serializable data and nether portal data from {@code original}.
	 * 
	 * @see #readNbt
	 * @see #moveToWorld
	 */
	public void copyFrom(Entity original) {
		NbtCompound nbtCompound = original.writeNbt(new NbtCompound());
		nbtCompound.remove("Dimension");
		this.readNbt(nbtCompound);
		this.portalCooldown = original.portalCooldown;
		this.lastNetherPortalPosition = original.lastNetherPortalPosition;
	}

	/**
	 * Moves this entity to another world.
	 * 
	 * <p>Note all entities except server player entities are completely recreated at the destination.
	 * 
	 * @return the entity in the other world
	 */
	@Nullable
	public Entity moveToWorld(ServerWorld destination) {
		if (this.getWorld() instanceof ServerWorld && !this.isRemoved()) {
			this.getWorld().getProfiler().push("changeDimension");
			this.detach();
			this.getWorld().getProfiler().push("reposition");
			TeleportTarget teleportTarget = this.getTeleportTarget(destination);
			if (teleportTarget == null) {
				return null;
			} else {
				this.getWorld().getProfiler().swap("reloading");
				Entity entity = this.getType().create(destination);
				if (entity != null) {
					entity.copyFrom(this);
					entity.refreshPositionAndAngles(teleportTarget.position.x, teleportTarget.position.y, teleportTarget.position.z, teleportTarget.yaw, entity.getPitch());
					entity.setVelocity(teleportTarget.velocity);
					destination.onDimensionChanged(entity);
					if (destination.getRegistryKey() == World.END) {
						ServerWorld.createEndSpawnPlatform(destination);
					}
				}

				this.removeFromDimension();
				this.getWorld().getProfiler().pop();
				((ServerWorld)this.getWorld()).resetIdleTimeout();
				destination.resetIdleTimeout();
				this.getWorld().getProfiler().pop();
				return entity;
			}
		} else {
			return null;
		}
	}

	/**
	 * Removes this entity from the current dimension.
	 * 
	 * <p>This calls {@link #setRemoved} by default. Subclasses can add other logics,
	 * such as setting the stack count of {@linkplain #getItemsEquipped equipped stacks}
	 * to zero.
	 * 
	 * @see #moveToWorld
	 */
	protected void removeFromDimension() {
		this.setRemoved(Entity.RemovalReason.CHANGED_DIMENSION);
	}

	/**
	 * {@return a {@link TeleportTarget} for the entity based on its current and
	 * destination worlds, plus any nether portals that may be present}
	 */
	@Nullable
	protected TeleportTarget getTeleportTarget(ServerWorld destination) {
		boolean bl = this.getWorld().getRegistryKey() == World.END && destination.getRegistryKey() == World.OVERWORLD;
		boolean bl2 = destination.getRegistryKey() == World.END;
		if (!bl && !bl2) {
			boolean bl3 = destination.getRegistryKey() == World.NETHER;
			if (this.getWorld().getRegistryKey() != World.NETHER && !bl3) {
				return null;
			} else {
				WorldBorder worldBorder = destination.getWorldBorder();
				double d = DimensionType.getCoordinateScaleFactor(this.getWorld().getDimension(), destination.getDimension());
				BlockPos blockPos2 = worldBorder.clamp(this.getX() * d, this.getY(), this.getZ() * d);
				return (TeleportTarget)this.getPortalRect(destination, blockPos2, bl3, worldBorder)
					.map(
						rect -> {
							BlockState blockState = this.getWorld().getBlockState(this.lastNetherPortalPosition);
							Direction.Axis axis;
							Vec3d vec3d;
							if (blockState.contains(Properties.HORIZONTAL_AXIS)) {
								axis = blockState.get(Properties.HORIZONTAL_AXIS);
								BlockLocating.Rectangle rectangle = BlockLocating.getLargestRectangle(
									this.lastNetherPortalPosition, axis, 21, Direction.Axis.Y, 21, pos -> this.getWorld().getBlockState(pos) == blockState
								);
								vec3d = this.positionInPortal(axis, rectangle);
							} else {
								axis = Direction.Axis.X;
								vec3d = new Vec3d(0.5, 0.0, 0.0);
							}

							return NetherPortal.getNetherTeleportTarget(destination, rect, axis, vec3d, this, this.getVelocity(), this.getYaw(), this.getPitch());
						}
					)
					.orElse(null);
			}
		} else {
			BlockPos blockPos;
			if (bl2) {
				blockPos = ServerWorld.END_SPAWN_POS;
			} else {
				blockPos = destination.getTopPosition(Heightmap.Type.MOTION_BLOCKING_NO_LEAVES, destination.getSpawnPos());
			}

			return new TeleportTarget(
				new Vec3d((double)blockPos.getX() + 0.5, (double)blockPos.getY(), (double)blockPos.getZ() + 0.5), this.getVelocity(), this.getYaw(), this.getPitch()
			);
		}
	}

	/**
	 * {@return the entity's position in the portal after teleportation}
	 * 
	 * @see net.minecraft.world.dimension.NetherPortal#entityPosInPortal
	 */
	protected Vec3d positionInPortal(Direction.Axis portalAxis, BlockLocating.Rectangle portalRect) {
		return NetherPortal.entityPosInPortal(portalRect, portalAxis, this.getPos(), this.getDimensions(this.getPose()));
	}

	/**
	 * {@return the portal rect at {@code destPos}}
	 * 
	 * @see net.minecraft.world.PortalForcer#getPortalRect
	 */
	protected Optional<BlockLocating.Rectangle> getPortalRect(ServerWorld destWorld, BlockPos destPos, boolean destIsNether, WorldBorder worldBorder) {
		return destWorld.getPortalForcer().getPortalRect(destPos, destIsNether, worldBorder);
	}

	/**
	 * {@return whether the entity can use nether portals and end portals}
	 * 
	 * <p>{@link net.minecraft.entity.boss.dragon.EnderDragonEntity},
	 * {@link net.minecraft.entity.boss.WitherEntity}, and {@link
	 * net.minecraft.entity.projectile.FishingBobberEntity} cannot use portals.
	 */
	public boolean canUsePortals() {
		return !this.hasVehicle() && !this.hasPassengers();
	}

	/**
	 * {@return the blast resistance of {@code blockState} for an explosion caused
	 * by this entity}
	 * 
	 * @apiNote {@link net.minecraft.entity.projectile.WitherSkullEntity} overrides
	 * this to implement the "charged/blue skull" behavior.
	 * 
	 * @see net.minecraft.world.explosion.ExplosionBehavior#getBlastResistance
	 */
	public float getEffectiveExplosionResistance(Explosion explosion, BlockView world, BlockPos pos, BlockState blockState, FluidState fluidState, float max) {
		return max;
	}

	/**
	 * {@return whether {@code explosion} from this entity can destroy {@code state}}
	 * 
	 * @apiNote This is used by {@link
	 * net.minecraft.entity.vehicle.TntMinecartEntity} to prevent the rail from being
	 * destroyed by explosion.
	 * 
	 * @see net.minecraft.world.explosion.ExplosionBehavior#canDestroyBlock
	 */
	public boolean canExplosionDestroyBlock(Explosion explosion, BlockView world, BlockPos pos, BlockState state, float explosionPower) {
		return true;
	}

	/**
	 * {@return the maximum height of a fall the entity takes during pathfinding}
	 */
	public int getSafeFallDistance() {
		return 3;
	}

	/**
	 * {@return whether the entity cannot trigger pressure plates or tripwires}
	 * 
	 * <p>{@link net.minecraft.entity.passive.BatEntity} is the only entity in vanilla
	 * that can avoid traps.
	 */
	public boolean canAvoidTraps() {
		return false;
	}

	/**
	 * Populates the crash report section to include the entity's information.
	 */
	public void populateCrashReport(CrashReportSection section) {
		section.add("Entity Type", (CrashCallable<String>)(() -> EntityType.getId(this.getType()) + " (" + this.getClass().getCanonicalName() + ")"));
		section.add("Entity ID", this.id);
		section.add("Entity Name", (CrashCallable<String>)(() -> this.getName().getString()));
		section.add("Entity's Exact location", String.format(Locale.ROOT, "%.2f, %.2f, %.2f", this.getX(), this.getY(), this.getZ()));
		section.add(
			"Entity's Block location",
			CrashReportSection.createPositionString(this.getWorld(), MathHelper.floor(this.getX()), MathHelper.floor(this.getY()), MathHelper.floor(this.getZ()))
		);
		Vec3d vec3d = this.getVelocity();
		section.add("Entity's Momentum", String.format(Locale.ROOT, "%.2f, %.2f, %.2f", vec3d.x, vec3d.y, vec3d.z));
		section.add("Entity's Passengers", (CrashCallable<String>)(() -> this.getPassengerList().toString()));
		section.add("Entity's Vehicle", (CrashCallable<String>)(() -> String.valueOf(this.getVehicle())));
	}

	/**
	 * {@return whether an entity should render as being on fire}
	 * 
	 * <p>This returns whether the entity {@linkplain #isOnFire is on fire} and
	 * is not a spectator.
	 * 
	 * @see #isOnFire
	 */
	public boolean doesRenderOnFire() {
		return this.isOnFire() && !this.isSpectator();
	}

	/**
	 * Sets the UUID of the entity to {@code uuid}.
	 * 
	 * <p>This should not be called after spawning the entity.
	 * 
	 * @see #getUuid
	 * @see #getUuidAsString
	 */
	public void setUuid(UUID uuid) {
		this.uuid = uuid;
		this.uuidString = this.uuid.toString();
	}

	@Override
	public UUID getUuid() {
		return this.uuid;
	}

	/**
	 * {@return the entity's UUID as string}
	 * 
	 * <p>This is a shortcut of {@code getUuid().toString()}.
	 * 
	 * @see #getUuid
	 */
	public String getUuidAsString() {
		return this.uuidString;
	}

	/**
	 * {@return the name uniquely identifying the entity}
	 * 
	 * <p>Unlike {@link #getName}, this is guaranteed to be unique. This is the UUID
	 * for all entities except players (which use the player's username).
	 * This is mostly used when passing the player name to {@code
	 * net.minecraft.scoreboard.Scoreboard} methods.
	 * 
	 * @see #getName
	 * @see #getUuidAsString
	 */
	public String getEntityName() {
		return this.uuidString;
	}

	/**
	 * {@return whether the entity is pushed by fluids}
	 * 
	 * @apiNote Aquatic mobs should override this to return {@code false}.
	 * Players are not pushed by fluids if they can fly (e.g. because of game mode).
	 */
	public boolean isPushedByFluids() {
		return true;
	}

	/**
	 * {@return the entity render distance multiplier}
	 * 
	 * <p>This is only usable on the client.
	 */
	public static double getRenderDistanceMultiplier() {
		return renderDistanceMultiplier;
	}

	/**
	 * Sets the render distance multiplier.
	 * 
	 * <p>This is only used on the client.
	 */
	public static void setRenderDistanceMultiplier(double value) {
		renderDistanceMultiplier = value;
	}

	@Override
	public Text getDisplayName() {
		return Team.decorateName(this.getScoreboardTeam(), this.getName())
			.styled(style -> style.withHoverEvent(this.getHoverEvent()).withInsertion(this.getUuidAsString()));
	}

	/**
	 * Sets the custom name of the entity to {@code name} (or {@code null} to
	 * remove the custom name).
	 */
	public void setCustomName(@Nullable Text name) {
		this.dataTracker.set(CUSTOM_NAME, Optional.ofNullable(name));
	}

	@Nullable
	@Override
	public Text getCustomName() {
		return (Text)this.dataTracker.get(CUSTOM_NAME).orElse(null);
	}

	@Override
	public boolean hasCustomName() {
		return this.dataTracker.get(CUSTOM_NAME).isPresent();
	}

	/**
	 * Sets whether the custom name should be shown.
	 * 
	 * <p>This is stored on {@code CustomNameVisible} NBT key.
	 * 
	 * @see #isCustomNameVisible
	 */
	public void setCustomNameVisible(boolean visible) {
		this.dataTracker.set(NAME_VISIBLE, visible);
	}

	/**
	 * {@return whether the custom name should be shown}
	 * 
	 * <p>This is stored on {@code CustomNameVisible} NBT key.
	 * 
	 * @see #setCustomNameVisible
	 */
	public boolean isCustomNameVisible() {
		return this.dataTracker.get(NAME_VISIBLE);
	}

	/**
	 * Teleports the entity to the given position, loading the chunk with
	 * {@link net.minecraft.server.world.ChunkTicketType#POST_TELEPORT}.
	 * 
	 * @see #requestTeleportAndDismount
	 * @see #requestTeleport
	 * @see #teleport(ServerWorld, double, double, double, Set, float, float)
	 * @see #refreshPositionAndAngles(double, double, double, float, float)
	 */
	public final void teleport(double destX, double destY, double destZ) {
		if (this.getWorld() instanceof ServerWorld) {
			ChunkPos chunkPos = new ChunkPos(BlockPos.ofFloored(destX, destY, destZ));
			((ServerWorld)this.getWorld()).getChunkManager().addTicket(ChunkTicketType.POST_TELEPORT, chunkPos, 0, this.getId());
			this.getWorld().getChunk(chunkPos.x, chunkPos.z);
			this.requestTeleport(destX, destY, destZ);
		}
	}

	/**
	 * Teleports the entity to the given position. If {@code world} differs from
	 * the current world, it copies the entity and discards the current one.
	 * 
	 * @see #requestTeleportAndDismount
	 * @see #requestTeleport
	 * @see #teleport(double, double, double)
	 * @see #refreshPositionAndAngles(double, double, double, float, float)
	 */
	public boolean teleport(ServerWorld world, double destX, double destY, double destZ, Set<PositionFlag> flags, float yaw, float pitch) {
		float f = MathHelper.clamp(pitch, -90.0F, 90.0F);
		if (world == this.getWorld()) {
			this.refreshPositionAndAngles(destX, destY, destZ, yaw, f);
			this.teleportPassengers();
			this.setHeadYaw(yaw);
		} else {
			this.detach();
			Entity entity = this.getType().create(world);
			if (entity == null) {
				return false;
			}

			entity.copyFrom(this);
			entity.refreshPositionAndAngles(destX, destY, destZ, yaw, f);
			entity.setHeadYaw(yaw);
			this.setRemoved(Entity.RemovalReason.CHANGED_DIMENSION);
			world.onDimensionChanged(entity);
		}

		return true;
	}

	/**
	 * Requests the entity to teleport to the given position. If the entity is
	 * a player, this also dismounts the player.
	 * 
	 * @see #teleport(double, double, double)
	 * @see #teleport(ServerWorld, double, double, double, Set, float, float)
	 * @see #requestTeleport
	 * @see #refreshPositionAndAngles(double, double, double, float, float)
	 */
	public void requestTeleportAndDismount(double destX, double destY, double destZ) {
		this.requestTeleport(destX, destY, destZ);
	}

	/**
	 * Requests the entity to teleport to the given position.
	 * 
	 * <p>For players, this sends the teleport packet. For other entities,
	 * this just sets the position of the entity and its passengers.
	 * 
	 * @see #teleport(double, double, double)
	 * @see #teleport(ServerWorld, double, double, double, Set, float, float)
	 * @see #requestTeleportOffset(double, double, double)
	 * @see #requestTeleportAndDismount
	 * @see #refreshPositionAndAngles(double, double, double, float, float)
	 */
	public void requestTeleport(double destX, double destY, double destZ) {
		if (this.getWorld() instanceof ServerWorld) {
			this.refreshPositionAndAngles(destX, destY, destZ, this.getYaw(), this.getPitch());
			this.teleportPassengers();
		}
	}

	private void teleportPassengers() {
		this.streamSelfAndPassengers().forEach(entity -> {
			for (Entity entity2 : entity.passengerList) {
				entity.updatePassengerPosition(entity2, Entity::refreshPositionAfterTeleport);
			}
		});
	}

	/**
	 * Requests the entity to teleport to the current position offset by the given amount.
	 * 
	 * <p>For players, this sends the teleport packet. For other entities,
	 * this just sets the position of the entity and its passengers.
	 * 
	 * @see #teleport
	 * @see #requestTeleport(double, double, double)
	 * @see #requestTeleportAndDismount
	 * @see #refreshPositionAndAngles(double, double, double, float, float)
	 */
	public void requestTeleportOffset(double offsetX, double offsetY, double offsetZ) {
		this.requestTeleport(this.getX() + offsetX, this.getY() + offsetY, this.getZ() + offsetZ);
	}

	/**
	 * {@return whether to render the name of the entity}
	 * 
	 * <p>This returns {@code true} for players and {@link #isCustomNameVisible} for
	 * other entities.
	 * 
	 * @see #isCustomNameVisible
	 */
	public boolean shouldRenderName() {
		return this.isCustomNameVisible();
	}

	public void onDataTrackerUpdate(List<DataTracker.SerializedEntry<?>> dataEntries) {
	}

	/**
	 * Called on the client when the tracked data is set.
	 * 
	 * <p>This can be overridden to refresh other fields when the tracked data
	 * is set or changed.
	 */
	public void onTrackedDataSet(TrackedData<?> data) {
		if (POSE.equals(data)) {
			this.calculateDimensions();
		}
	}

	@Deprecated
	protected void reinitDimensions() {
		EntityPose entityPose = this.getPose();
		EntityDimensions entityDimensions = this.getDimensions(entityPose);
		this.dimensions = entityDimensions;
		this.standingEyeHeight = this.getEyeHeight(entityPose, entityDimensions);
	}

	/**
	 * Calculates and sets the dimension (bounding box) of the entity and refreshes
	 * its position.
	 */
	public void calculateDimensions() {
		EntityDimensions entityDimensions = this.dimensions;
		EntityPose entityPose = this.getPose();
		EntityDimensions entityDimensions2 = this.getDimensions(entityPose);
		this.dimensions = entityDimensions2;
		this.standingEyeHeight = this.getEyeHeight(entityPose, entityDimensions2);
		this.refreshPosition();
		boolean bl = (double)entityDimensions2.width <= 4.0 && (double)entityDimensions2.height <= 4.0;
		if (!this.getWorld().isClient
			&& !this.firstUpdate
			&& !this.noClip
			&& bl
			&& (entityDimensions2.width > entityDimensions.width || entityDimensions2.height > entityDimensions.height)
			&& !(this instanceof PlayerEntity)) {
			Vec3d vec3d = this.getPos().add(0.0, (double)entityDimensions.height / 2.0, 0.0);
			double d = (double)Math.max(0.0F, entityDimensions2.width - entityDimensions.width) + 1.0E-6;
			double e = (double)Math.max(0.0F, entityDimensions2.height - entityDimensions.height) + 1.0E-6;
			VoxelShape voxelShape = VoxelShapes.cuboid(Box.of(vec3d, d, e, d));
			this.getWorld()
				.findClosestCollision(this, voxelShape, vec3d, (double)entityDimensions2.width, (double)entityDimensions2.height, (double)entityDimensions2.width)
				.ifPresent(pos -> this.setPosition(pos.add(0.0, (double)(-entityDimensions2.height) / 2.0, 0.0)));
		}
	}

	public Direction getHorizontalFacing() {
		return Direction.fromRotation((double)this.getYaw());
	}

	public Direction getMovementDirection() {
		return this.getHorizontalFacing();
	}

	/**
	 * {@return the hover event referencing this entity}
	 */
	protected HoverEvent getHoverEvent() {
		return new HoverEvent(HoverEvent.Action.SHOW_ENTITY, new HoverEvent.EntityContent(this.getType(), this.getUuid(), this.getName()));
	}

	/**
	 * {@return whether {@code spectator} can spectate this entity}
	 * 
	 * <p>Spectator players (other than themselves) cannot be spectated.
	 */
	public boolean canBeSpectated(ServerPlayerEntity spectator) {
		return true;
	}

	@Override
	public final Box getBoundingBox() {
		return this.boundingBox;
	}

	public Box getVisibilityBoundingBox() {
		return this.getBoundingBox();
	}

	protected Box calculateBoundsForPose(EntityPose pos) {
		EntityDimensions entityDimensions = this.getDimensions(pos);
		float f = entityDimensions.width / 2.0F;
		Vec3d vec3d = new Vec3d(this.getX() - (double)f, this.getY(), this.getZ() - (double)f);
		Vec3d vec3d2 = new Vec3d(this.getX() + (double)f, this.getY() + (double)entityDimensions.height, this.getZ() + (double)f);
		return new Box(vec3d, vec3d2);
	}

	public final void setBoundingBox(Box boundingBox) {
		this.boundingBox = boundingBox;
	}

	protected float getEyeHeight(EntityPose pose, EntityDimensions dimensions) {
		return dimensions.height * 0.85F;
	}

	/**
	 * {@return the eye height for {@code pose}}
	 */
	public float getEyeHeight(EntityPose pose) {
		return this.getEyeHeight(pose, this.getDimensions(pose));
	}

	/**
	 * {@return the standing eye height}
	 * 
	 * <p>This is used for calculating the leash offset.
	 * 
	 * @see #getLeashOffset
	 */
	public final float getStandingEyeHeight() {
		return this.standingEyeHeight;
	}

	public Vec3d getLeashOffset(float tickDelta) {
		return this.getLeashOffset();
	}

	/**
	 * {@return the offset from the entity's position where the leash is attached to}
	 * 
	 * <p>This is different from {@link #getLeashPos}; this method is called on the entity
	 * which a leash is attached to.
	 * 
	 * @see #getLeashPos
	 * @see #getStandingEyeHeight
	 */
	protected Vec3d getLeashOffset() {
		return new Vec3d(0.0, (double)this.getStandingEyeHeight(), (double)(this.getWidth() * 0.4F));
	}

	/**
	 * Obtains a stack reference to be modified. Used by commands like
	 * {@code /loot} or {@code /item}.
	 * 
	 * @see net.minecraft.command.argument.ItemSlotArgumentType
	 * 
	 * @param mappedIndex the mapped index as given by the item slot argument
	 */
	public StackReference getStackReference(int mappedIndex) {
		return StackReference.EMPTY;
	}

	@Override
	public void sendMessage(Text message) {
	}

	public World getEntityWorld() {
		return this.getWorld();
	}

	/**
	 * {@return the server the entity is in, or {@code null} if called on the client side}
	 */
	@Nullable
	public MinecraftServer getServer() {
		return this.getWorld().getServer();
	}

	/**
	 * Called when the player interacts with the entity at the specific position.
	 * 
	 * <p>This should not be used in most cases; {@link #interact} should be used.
	 * This should be used if the interaction's result depends on which part of the
	 * entity was interacted at.
	 * 
	 * @param hitPos the interaction's position offset from the entity's position
	 */
	public ActionResult interactAt(PlayerEntity player, Vec3d hitPos, Hand hand) {
		return ActionResult.PASS;
	}

	/**
	 * {@return whether the entity is immune from explosion knockback and damage}
	 * 
	 * <p>Invisible {@link net.minecraft.entity.decoration.ArmorStandEntity} and
	 * emerging or digging {@link net.minecraft.entity.mob.WardenEntity} are
	 * immune from explosions.
	 */
	public boolean isImmuneToExplosion() {
		return false;
	}

	/**
	 * Applies damage effects to {@code attacker} or {@code target}.
	 * 
	 * <p>Called when {@code attacker} damages {@code target}.
	 * 
	 * <p>Used to apply damage effects based on enchantments, such
	 * as Thorns attacker damage or slowness from Bane of Arthropods.
	 * 
	 * @implNote Although this method is non-static, {@code this} keyword is
	 * not used anywhere in this method.
	 * 
	 * @param attacker the attacker; usually this entity, but may be a {@linkplain
	 * net.minecraft.entity.projectile.ProjectileEntity#getOwner() projectile's
	 * owner entity}
	 */
	public void applyDamageEffects(LivingEntity attacker, Entity target) {
		if (target instanceof LivingEntity) {
			EnchantmentHelper.onUserDamaged((LivingEntity)target, attacker);
		}

		EnchantmentHelper.onTargetDamaged(attacker, target);
	}

	/**
	 * Called when {@code player} starts tracking this entity.
	 * 
	 * <p>Entities with boss bars like {@link net.minecraft.entity.boss.WitherEntity}
	 * should override this to add the player to the boss bar.
	 */
	public void onStartedTrackingBy(ServerPlayerEntity player) {
	}

	/**
	 * Called when {@code player} stops tracking this entity.
	 * 
	 * <p>Entities with boss bars like {@link net.minecraft.entity.boss.WitherEntity}
	 * should override this to remove the player from the boss bar.
	 */
	public void onStoppedTrackingBy(ServerPlayerEntity player) {
	}

	/**
	 * Applies {@code rotation} to the entity's yaw.
	 */
	public float applyRotation(BlockRotation rotation) {
		float f = MathHelper.wrapDegrees(this.getYaw());
		switch (rotation) {
			case CLOCKWISE_180:
				return f + 180.0F;
			case COUNTERCLOCKWISE_90:
				return f + 270.0F;
			case CLOCKWISE_90:
				return f + 90.0F;
			default:
				return f;
		}
	}

	/**
	 * Applies {@code mirror} to the entity's yaw.
	 */
	public float applyMirror(BlockMirror mirror) {
		float f = MathHelper.wrapDegrees(this.getYaw());
		switch (mirror) {
			case FRONT_BACK:
				return -f;
			case LEFT_RIGHT:
				return 180.0F - f;
			default:
				return f;
		}
	}

	/**
	 * {@return whether {@link EntityType#loadFromEntityNbt} should reject entity
	 * NBTs unless called by an operator}
	 * 
	 * <p>This is {@code false} by default, and should be overridden if the entity
	 * provides functionality that is usually restricted to operators (such as {@link
	 * net.minecraft.entity.vehicle.CommandBlockMinecartEntity}).
	 */
	public boolean entityDataRequiresOperator() {
		return false;
	}

	/**
	 * {@return the passenger in control of this entity, or {@code null} if there is none}
	 * 
	 * <p>Rideable entities should override this to return the entity. This is
	 * usually {@code #getFirstPassenger}.
	 * 
	 * @see #hasControllingPassenger
	 * @see #getPassengerList
	 * @see #getFirstPassenger
	 */
	@Nullable
	public LivingEntity getControllingPassenger() {
		return null;
	}

	/**
	 * {@return whether there is a passenger in control of this entity}
	 * 
	 * @see #getControllingPassenger
	 * @see #getPassengerList
	 * @see #getFirstPassenger
	 */
	public final boolean hasControllingPassenger() {
		return this.getControllingPassenger() != null;
	}

	/**
	 * {@return the list of passengers of this entity}
	 * 
	 * @see #getControllingPassenger
	 * @see #getFirstPassenger
	 * @see #streamIntoPassengers
	 * @see #streamSelfAndPassengers
	 * @see #streamPassengersAndSelf
	 * @see #getPassengersDeep
	 */
	public final List<Entity> getPassengerList() {
		return this.passengerList;
	}

	/**
	 * {@return the first passenger of the {@linkplain #getPassengerList passenger list},
	 * or {@code null} if there is no passengers}
	 * 
	 * <p>Such passenger is usually also the {@linkplain #getControllingPassenger the
	 * controlling passenger}.
	 * 
	 * @see #getControllingPassenger
	 * @see #hasControllingPassenger
	 * @see #getPassengerList
	 */
	@Nullable
	public Entity getFirstPassenger() {
		return this.passengerList.isEmpty() ? null : (Entity)this.passengerList.get(0);
	}

	/**
	 * {@return whether {@code passenger} is a passenger of this entity}
	 * 
	 * @see #getPassengerList
	 * @see #streamIntoPassengers
	 * @see #streamSelfAndPassengers
	 * @see #streamPassengersAndSelf
	 * @see #getPassengersDeep
	 * @see #hasPassenger(Predicate)
	 */
	public boolean hasPassenger(Entity passenger) {
		return this.passengerList.contains(passenger);
	}

	/**
	 * {@return whether there is a passenger of this entity matching {@code predicate}}
	 * 
	 * @see #getPassengerList
	 * @see #streamIntoPassengers
	 * @see #streamSelfAndPassengers
	 * @see #streamPassengersAndSelf
	 * @see #getPassengersDeep
	 * @see #hasPassenger(Entity)
	 */
	public boolean hasPassenger(Predicate<Entity> predicate) {
		for (Entity entity : this.passengerList) {
			if (predicate.test(entity)) {
				return true;
			}
		}

		return false;
	}

	/**
	 * {@return a recursive stream of all passengers}
	 * 
	 * <p>This is recursive; for example, if a boat has 2 pigs, ridden by player A and
	 * player B, then {@code boat.streamIntoPassengers()} would return a stream of
	 * the first pig, player A, the second pig, and player B. This does not stream
	 * the vehicle itself.
	 * 
	 * @see #getPassengerList
	 * @see #streamSelfAndPassengers
	 * @see #streamPassengersAndSelf
	 * @see #getPassengersDeep
	 */
	private Stream<Entity> streamIntoPassengers() {
		return this.passengerList.stream().flatMap(Entity::streamSelfAndPassengers);
	}

	@Override
	public Stream<Entity> streamSelfAndPassengers() {
		return Stream.concat(Stream.of(this), this.streamIntoPassengers());
	}

	@Override
	public Stream<Entity> streamPassengersAndSelf() {
		return Stream.concat(this.passengerList.stream().flatMap(Entity::streamPassengersAndSelf), Stream.of(this));
	}

	/**
	 * {@return an iterable of all passengers}
	 * 
	 * <p>This is recursive; for example, if a boat has 2 pigs, ridden by player A and
	 * player B, then {@code boat.streamIntoPassengers()} would return a stream of
	 * the first pig, player A, the second pig, and player B. This does not stream
	 * the vehicle itself.
	 * 
	 * @see #getPassengerList
	 * @see #streamIntoPassengers
	 * @see #streamSelfAndPassengers
	 * @see #streamPassengersAndSelf
	 */
	public Iterable<Entity> getPassengersDeep() {
		return () -> this.streamIntoPassengers().iterator();
	}

	/**
	 * {@return whether a player is riding this entity or any of its passengers}
	 * 
	 * @implNote The default implementation is very inefficient.
	 * 
	 * @see #getPassengerList
	 * @see #streamIntoPassengers
	 * @see #streamSelfAndPassengers
	 * @see #streamPassengersAndSelf
	 * @see #getPassengersDeep
	 * @see #hasPassengerDeep
	 */
	public boolean hasPlayerRider() {
		return this.streamIntoPassengers().filter(entity -> entity instanceof PlayerEntity).count() == 1L;
	}

	/**
	 * {@return the lowest entity this entity is riding}
	 * 
	 * @see #getVehicle
	 */
	public Entity getRootVehicle() {
		Entity entity = this;

		while (entity.hasVehicle()) {
			entity = entity.getVehicle();
		}

		return entity;
	}

	/**
	 * {@return whether this entity and another entity share the same root vehicle}
	 * 
	 * @see #getRootVehicle
	 * @see #getVehicle
	 * 
	 * @param entity the other entity
	 */
	public boolean isConnectedThroughVehicle(Entity entity) {
		return this.getRootVehicle() == entity.getRootVehicle();
	}

	/**
	 * {@return whether {@code passenger} is riding this entity or any of its passengers}
	 * 
	 * @see #getPassengerList
	 * @see #streamIntoPassengers
	 * @see #streamSelfAndPassengers
	 * @see #streamPassengersAndSelf
	 * @see #getPassengersDeep
	 * @see #hasPlayerRider
	 */
	public boolean hasPassengerDeep(Entity passenger) {
		if (!passenger.hasVehicle()) {
			return false;
		} else {
			Entity entity = passenger.getVehicle();
			return entity == this ? true : this.hasPassengerDeep(entity);
		}
	}

	/**
	 * {@return whether the current side is responsible for updating the movement}
	 * 
	 * <p>For non-player ridden entities, this checks whether the entity is
	 * on the server; for player-ridden entities, this checks whether the entity
	 * is on the client.
	 * 
	 * @see #getControllingPassenger
	 */
	public boolean isLogicalSideForUpdatingMovement() {
		return this.getControllingPassenger() instanceof PlayerEntity playerEntity ? playerEntity.isMainPlayer() : this.canMoveVoluntarily();
	}

	public boolean canMoveVoluntarily() {
		return !this.getWorld().isClient;
	}

	/**
	 * {@return the offset for dismounting the passenger}
	 */
	protected static Vec3d getPassengerDismountOffset(double vehicleWidth, double passengerWidth, float passengerYaw) {
		double d = (vehicleWidth + passengerWidth + 1.0E-5F) / 2.0;
		float f = -MathHelper.sin(passengerYaw * (float) (Math.PI / 180.0));
		float g = MathHelper.cos(passengerYaw * (float) (Math.PI / 180.0));
		float h = Math.max(Math.abs(f), Math.abs(g));
		return new Vec3d((double)f * d / (double)h, 0.0, (double)g * d / (double)h);
	}

	/**
	 * {@return the position of the dismounted {@code passenger}}
	 * 
	 * <p>Vehicles should override this to return a suitable dismounting position
	 * for the passenger. Check the implementation of the subclass for details.
	 * 
	 * @see #getPassengerDismountOffset
	 * @see Dismounting
	 */
	public Vec3d updatePassengerForDismount(LivingEntity passenger) {
		return new Vec3d(this.getX(), this.getBoundingBox().maxY, this.getZ());
	}

	/**
	 * {@return the entity this entity rides, or {@code null} if there is none}
	 * 
	 * @see #getRootVehicle
	 * @see #getControllingVehicle
	 */
	@Nullable
	public Entity getVehicle() {
		return this.vehicle;
	}

	/**
	 * {@return the entity this entity rides and controls, or {@code null} if there is none}
	 * 
	 * @see #getRootVehicle
	 * @see #getVehicle
	 */
	@Nullable
	public Entity getControllingVehicle() {
		return this.vehicle != null && this.vehicle.getControllingPassenger() == this ? this.vehicle : null;
	}

	/**
	 * {@return the behavior of the piston for this entity}
	 * 
	 * <p>This is {@link PistonBehavior#NORMAL} by default. {@link AreaEffectCloudEntity},
	 * {@link MarkerEntity}, and marker {@link net.minecraft.entity.decoration.ArmorStandEntity}
	 * return {@link PistonBehavior#IGNORE}, causing the piston to not affect the entity's
	 * position. Other piston behaviors are inapplicable to entities, and treated like
	 * {@link PistonBehavior#NORMAL}.
	 */
	public PistonBehavior getPistonBehavior() {
		return PistonBehavior.NORMAL;
	}

	/**
	 * {@return the sound category for sounds from this entity}
	 * 
	 * <p>This is used by {@link #playSound(SoundEvent, float, float)} and defaults to
	 * {@link SoundCategory#NEUTRAL}. Hostile entities should override this to
	 * return {@link SoundCategory#HOSTILE}.
	 * 
	 * @see #playSound(SoundEvent, float, float)
	 */
	public SoundCategory getSoundCategory() {
		return SoundCategory.NEUTRAL;
	}

	protected int getBurningDuration() {
		return 1;
	}

	/**
	 * {@return a command source which represents this entity}
	 */
	public ServerCommandSource getCommandSource() {
		return new ServerCommandSource(
			this,
			this.getPos(),
			this.getRotationClient(),
			this.getWorld() instanceof ServerWorld ? (ServerWorld)this.getWorld() : null,
			this.getPermissionLevel(),
			this.getName().getString(),
			this.getDisplayName(),
			this.getWorld().getServer(),
			this
		);
	}

	/**
	 * {@return the permission level of this entity}
	 * 
	 * <p>This is {@code 0} for non-players.
	 * 
	 * @see #hasPermissionLevel
	 */
	protected int getPermissionLevel() {
		return 0;
	}

	/**
	 * {@return whether this entity has at least permission level {@code permissionLevel}}
	 * 
	 * <p>This is always {@code false} for non-players.
	 * 
	 * @see #getPermissionLevel
	 */
	public boolean hasPermissionLevel(int permissionLevel) {
		return this.getPermissionLevel() >= permissionLevel;
	}

	@Override
	public boolean shouldReceiveFeedback() {
		return this.getWorld().getGameRules().getBoolean(GameRules.SEND_COMMAND_FEEDBACK);
	}

	@Override
	public boolean shouldTrackOutput() {
		return true;
	}

	@Override
	public boolean shouldBroadcastConsoleToOps() {
		return true;
	}

	/**
	 * Changes this entity's pitch and yaw to look at {@code target}.
	 */
	public void lookAt(EntityAnchorArgumentType.EntityAnchor anchorPoint, Vec3d target) {
		Vec3d vec3d = anchorPoint.positionAt(this);
		double d = target.x - vec3d.x;
		double e = target.y - vec3d.y;
		double f = target.z - vec3d.z;
		double g = Math.sqrt(d * d + f * f);
		this.setPitch(MathHelper.wrapDegrees((float)(-(MathHelper.atan2(e, g) * 180.0F / (float)Math.PI))));
		this.setYaw(MathHelper.wrapDegrees((float)(MathHelper.atan2(f, d) * 180.0F / (float)Math.PI) - 90.0F));
		this.setHeadYaw(this.getYaw());
		this.prevPitch = this.getPitch();
		this.prevYaw = this.getYaw();
	}

	public boolean updateMovementInFluid(TagKey<Fluid> tag, double speed) {
		if (this.isRegionUnloaded()) {
			return false;
		} else {
			Box box = this.getBoundingBox().contract(0.001);
			int i = MathHelper.floor(box.minX);
			int j = MathHelper.ceil(box.maxX);
			int k = MathHelper.floor(box.minY);
			int l = MathHelper.ceil(box.maxY);
			int m = MathHelper.floor(box.minZ);
			int n = MathHelper.ceil(box.maxZ);
			double d = 0.0;
			boolean bl = this.isPushedByFluids();
			boolean bl2 = false;
			Vec3d vec3d = Vec3d.ZERO;
			int o = 0;
			BlockPos.Mutable mutable = new BlockPos.Mutable();

			for (int p = i; p < j; p++) {
				for (int q = k; q < l; q++) {
					for (int r = m; r < n; r++) {
						mutable.set(p, q, r);
						FluidState fluidState = this.getWorld().getFluidState(mutable);
						if (fluidState.isIn(tag)) {
							double e = (double)((float)q + fluidState.getHeight(this.getWorld(), mutable));
							if (e >= box.minY) {
								bl2 = true;
								d = Math.max(e - box.minY, d);
								if (bl) {
									Vec3d vec3d2 = fluidState.getVelocity(this.getWorld(), mutable);
									if (d < 0.4) {
										vec3d2 = vec3d2.multiply(d);
									}

									vec3d = vec3d.add(vec3d2);
									o++;
								}
							}
						}
					}
				}
			}

			if (vec3d.length() > 0.0) {
				if (o > 0) {
					vec3d = vec3d.multiply(1.0 / (double)o);
				}

				if (!(this instanceof PlayerEntity)) {
					vec3d = vec3d.normalize();
				}

				Vec3d vec3d3 = this.getVelocity();
				vec3d = vec3d.multiply(speed * 1.0);
				double f = 0.003;
				if (Math.abs(vec3d3.x) < 0.003 && Math.abs(vec3d3.z) < 0.003 && vec3d.length() < 0.0045000000000000005) {
					vec3d = vec3d.normalize().multiply(0.0045000000000000005);
				}

				this.setVelocity(this.getVelocity().add(vec3d));
			}

			this.fluidHeight.put(tag, d);
			return bl2;
		}
	}

	/**
	 * {@return whether any part of this entity's bounding box is in an unloaded
	 * region of the world the entity is in}
	 * 
	 * @implNote This implementation expands this entity's bounding box by 1 in
	 * each axis and checks whether the expanded box's smallest enclosing
	 * axis-aligned integer box is fully loaded in the world.
	 */
	public boolean isRegionUnloaded() {
		Box box = this.getBoundingBox().expand(1.0);
		int i = MathHelper.floor(box.minX);
		int j = MathHelper.ceil(box.maxX);
		int k = MathHelper.floor(box.minZ);
		int l = MathHelper.ceil(box.maxZ);
		return !this.getWorld().isRegionLoaded(i, k, j, l);
	}

	/**
	 * {@return the height of the fluid in {@code fluid} tag}
	 */
	public double getFluidHeight(TagKey<Fluid> fluid) {
		return this.fluidHeight.getDouble(fluid);
	}

	/**
	 * {@return the minimum submerged height of this entity in fluid so that it
	 * would be affected by fluid physics}
	 * 
	 * @apiNote This is also used by living entities for checking whether to
	 * start swimming.
	 * 
	 * @implNote This implementation returns {@code 0.4} if its
	 * {@linkplain #getStandingEyeHeight standing eye height} is larger than
	 * {@code 0.4}; otherwise it returns {@code 0.0} for shorter entities.
	 * The swim height of 0 allows short entities like baby animals
	 * to start swimming to avoid suffocation.
	 */
	public double getSwimHeight() {
		return (double)this.getStandingEyeHeight() < 0.4 ? 0.0 : 0.4;
	}

	/**
	 * {@return the width of the entity's current dimension}
	 */
	public final float getWidth() {
		return this.dimensions.width;
	}

	/**
	 * {@return the height of the entity's current dimension}
	 */
	public final float getHeight() {
		return this.dimensions.height;
	}

	public float getNameLabelHeight() {
		return this.getHeight() + 0.5F;
	}

	/**
	 * {@return a packet to notify the clients of the entity's spawning}
	 * 
	 * @apiNote Subclasses should return {@code new EntitySpawnS2CPacket(this)},
	 * unless they use a custom spawning packet.
	 */
	public Packet<ClientPlayPacketListener> createSpawnPacket() {
		return new EntitySpawnS2CPacket(this);
	}

	/**
	 * {@return the dimensions of the entity with the given {@code pose}}
	 * 
	 * @see #getWidth
	 * @see #getHeight
	 */
	public EntityDimensions getDimensions(EntityPose pose) {
		return this.type.getDimensions();
	}

	/**
	 * {@return the exact position of the entity}
	 * 
	 * @see #getSyncedPos
	 * @see #getBlockPos
	 * @see #getChunkPos
	 */
	public Vec3d getPos() {
		return this.pos;
	}

	/**
	 * {@return the position of the entity synced to clients}
	 * 
	 * <p>This is the same as {@link #getPos} except for paintings which return the
	 * attachment position.
	 * 
	 * @see #getPos
	 * @see #getBlockPos
	 * @see #getChunkPos
	 */
	public Vec3d getSyncedPos() {
		return this.getPos();
	}

	@Override
	public BlockPos getBlockPos() {
		return this.blockPos;
	}

	/**
	 * {@return the block state at the entity's position}
	 * 
	 * <p>The result is cached.
	 * 
	 * @see #getBlockPos
	 * @see #getLandingBlockState
	 * @see #getSteppingBlockState
	 */
	public BlockState getBlockStateAtPos() {
		if (this.blockStateAtPos == null) {
			this.blockStateAtPos = this.getWorld().getBlockState(this.getBlockPos());
		}

		return this.blockStateAtPos;
	}

	/**
	 * {@return the chunk position of the entity}
	 */
	public ChunkPos getChunkPos() {
		return this.chunkPos;
	}

	public Vec3d getVelocity() {
		return this.velocity;
	}

	public void setVelocity(Vec3d velocity) {
		this.velocity = velocity;
	}

	public void addVelocity(Vec3d velocity) {
		this.setVelocity(this.getVelocity().add(velocity));
	}

	public void setVelocity(double x, double y, double z) {
		this.setVelocity(new Vec3d(x, y, z));
	}

	public final int getBlockX() {
		return this.blockPos.getX();
	}

	public final double getX() {
		return this.pos.x;
	}

	public double offsetX(double widthScale) {
		return this.pos.x + (double)this.getWidth() * widthScale;
	}

	public double getParticleX(double widthScale) {
		return this.offsetX((2.0 * this.random.nextDouble() - 1.0) * widthScale);
	}

	public final int getBlockY() {
		return this.blockPos.getY();
	}

	public final double getY() {
		return this.pos.y;
	}

	public double getBodyY(double heightScale) {
		return this.pos.y + (double)this.getHeight() * heightScale;
	}

	public double getRandomBodyY() {
		return this.getBodyY(this.random.nextDouble());
	}

	public double getEyeY() {
		return this.pos.y + (double)this.standingEyeHeight;
	}

	public final int getBlockZ() {
		return this.blockPos.getZ();
	}

	public final double getZ() {
		return this.pos.z;
	}

	public double offsetZ(double widthScale) {
		return this.pos.z + (double)this.getWidth() * widthScale;
	}

	public double getParticleZ(double widthScale) {
		return this.offsetZ((2.0 * this.random.nextDouble() - 1.0) * widthScale);
	}

	/**
	 * Sets the position of this entity.
	 * 
	 * <p>This should be used when overriding {@link #tick} to change the
	 * entity's position; in other cases, use {@link #setPosition(double, double, double)}
	 * or {@link #refreshPositionAndAngles(double, double, double, float, float)}.
	 * 
	 * @see #setPosition(double, double, double)
	 * @see #refreshPositionAndAngles(double, double, double, float, float)
	 */
	public final void setPos(double x, double y, double z) {
		if (this.pos.x != x || this.pos.y != y || this.pos.z != z) {
			this.pos = new Vec3d(x, y, z);
			int i = MathHelper.floor(x);
			int j = MathHelper.floor(y);
			int k = MathHelper.floor(z);
			if (i != this.blockPos.getX() || j != this.blockPos.getY() || k != this.blockPos.getZ()) {
				this.blockPos = new BlockPos(i, j, k);
				this.blockStateAtPos = null;
				if (ChunkSectionPos.getSectionCoord(i) != this.chunkPos.x || ChunkSectionPos.getSectionCoord(k) != this.chunkPos.z) {
					this.chunkPos = new ChunkPos(this.blockPos);
				}
			}

			this.changeListener.updateEntityPosition();
		}
	}

	/**
	 * Checks whether the entity should be despawned.
	 * 
	 * <p>To despawn this entity, call {@link #discard}.
	 * 
	 * @see #discard
	 */
	public void checkDespawn() {
	}

	/**
	 * {@return the position of the leash this entity holds}
	 * 
	 * <p>This is different from {@link #getLeashOffset}; this method is called on the entity
	 * that holds the leash.
	 * 
	 * @see #getLeashOffset
	 * @see #getStandingEyeHeight
	 */
	public Vec3d getLeashPos(float delta) {
		return this.getLerpedPos(delta).add(0.0, (double)this.standingEyeHeight * 0.7, 0.0);
	}

	/**
	 * Called on the client when the entity receives a spawn packet.
	 * 
	 * <p>This sets the entity's position, angles, ID, and UUID. Subclasses
	 * can override this to initialize additional fields.
	 */
	public void onSpawnPacket(EntitySpawnS2CPacket packet) {
		int i = packet.getId();
		double d = packet.getX();
		double e = packet.getY();
		double f = packet.getZ();
		this.updateTrackedPosition(d, e, f);
		this.refreshPositionAfterTeleport(d, e, f);
		this.setPitch(packet.getPitch());
		this.setYaw(packet.getYaw());
		this.setId(i);
		this.setUuid(packet.getUuid());
	}

	/**
	 * {@return the stack for creative "pick block" functionality, or {@code null}
	 * if there is none}
	 * 
	 * <p>If the entity has an item representation (such as boats or minecarts),
	 * this should be overridden to return a new stack. Note that {@link
	 * net.minecraft.entity.mob.MobEntity} handles the spawn eggs.
	 * {@link net.minecraft.entity.decoration.ItemFrameEntity} instead returns
	 * the copy of the stack held in the frame.
	 */
	@Nullable
	public ItemStack getPickBlockStack() {
		return null;
	}

	public void setInPowderSnow(boolean inPowderSnow) {
		this.inPowderSnow = inPowderSnow;
	}

	/**
	 * {@return whether the entity can freeze}
	 * 
	 * @implNote Entities cannot be frozen if they are in the {@link
	 * net.minecraft.registry.tag.EntityTypeTags#FREEZE_IMMUNE_ENTITY_TYPES} tag. In addition to this, {@link
	 * LivingEntity} cannot be frozen if they are spectator or if they wear an
	 * item inside {@link net.minecraft.registry.tag.ItemTags#FREEZE_IMMUNE_WEARABLES} tag.
	 */
	public boolean canFreeze() {
		return !this.getType().isIn(EntityTypeTags.FREEZE_IMMUNE_ENTITY_TYPES);
	}

	/**
	 * {@return whether the entity should escape from powder snow}
	 * 
	 * <p>This returns {@code true} if the entity is/was in powder snow and
	 * if it can freeze.
	 * 
	 * @see #canFreeze
	 * @see #isFrozen
	 */
	public boolean shouldEscapePowderSnow() {
		return (this.inPowderSnow || this.wasInPowderSnow) && this.canFreeze();
	}

	public float getYaw() {
		System.out.println(this.yaw);
		return this.yaw;
	}

	/**
	 * {@return the body yaw of the entity}
	 * 
	 * @see #setBodyYaw
	 */
	public float getBodyYaw() {
		return this.getYaw();
	}

	public void setYaw(float yaw) {
		if (!Float.isFinite(yaw)) {
			Util.error("Invalid entity rotation: " + yaw + ", discarding.");
		} else {
			this.yaw = yaw;
		}
	}

	public float getPitch() {
		return this.pitch;
	}

	public void setPitch(float pitch) {
		if (!Float.isFinite(pitch)) {
			Util.error("Invalid entity rotation: " + pitch + ", discarding.");
		} else {
			this.pitch = pitch;
		}
	}

	public boolean canSprintAsVehicle() {
		return false;
	}

	public float getStepHeight() {
		return this.stepHeight;
	}

	public void setStepHeight(float stepHeight) {
		this.stepHeight = stepHeight;
	}

	/**
	 * {@return whether the entity is removed}
	 * 
	 * <p>Removed entities should not be interacted with.
	 */
	public final boolean isRemoved() {
		return this.removalReason != null;
	}

	/**
	 * {@return the reason for the entity's removal, or {@code null} if it is not removed}
	 */
	@Nullable
	public Entity.RemovalReason getRemovalReason() {
		return this.removalReason;
	}

	@Override
	public final void setRemoved(Entity.RemovalReason reason) {
		if (this.removalReason == null) {
			this.removalReason = reason;
		}

		if (this.removalReason.shouldDestroy()) {
			this.stopRiding();
		}

		this.getPassengerList().forEach(Entity::stopRiding);
		this.changeListener.remove(reason);
	}

	/**
	 * Unsets this entity's removal.
	 * 
	 * <p>This should rarely be used; this is only used by players during teleportation.
	 */
	protected void unsetRemoved() {
		this.removalReason = null;
	}

	@Override
	public void setChangeListener(EntityChangeListener changeListener) {
		this.changeListener = changeListener;
	}

	@Override
	public boolean shouldSave() {
		if (this.removalReason != null && !this.removalReason.shouldSave()) {
			return false;
		} else {
			return this.hasVehicle() ? false : !this.hasPassengers() || !this.hasPlayerRider();
		}
	}

	@Override
	public boolean isPlayer() {
		return false;
	}

	/**
	 * {@return whether the entity can modify the world at {@code pos}}
	 * 
	 * <p>This returns {@code true} for most entities. Players check {@link
	 * World#canPlayerModifyAt} to prevent them from modifying entities in the spawn
	 * protection or outside the world border. {@link
	 * net.minecraft.entity.projectile.ProjectileEntity} delegates it to the owner
	 * if the owner is a player; if the owner is a non-player entity, this returns
	 * the value of {@link net.minecraft.world.GameRules#DO_MOB_GRIEFING}, and ownerless
	 * projectiles are always allowed to modify the world.
	 * 
	 * @see World#canPlayerModifyAt
	 */
	public boolean canModifyAt(World world, BlockPos pos) {
		return true;
	}

	public World getWorld() {
		return this.world;
	}

	protected void setWorld(World world) {
		this.world = world;
	}

	public DamageSources getDamageSources() {
		return this.getWorld().getDamageSources();
	}

	/**
	 * The move effect represents possible effects of an entity moving, such as
	 * playing sounds, emitting game events, none, or both.
	 * 
	 * @see Entity#getMoveEffect()
	 */
	public static enum MoveEffect {
		NONE(false, false),
		SOUNDS(true, false),
		EVENTS(false, true),
		ALL(true, true);

		final boolean sounds;
		final boolean events;

		private MoveEffect(boolean sounds, boolean events) {
			this.sounds = sounds;
			this.events = events;
		}

		/**
		 * Returns whether this means an entity may emit game events or play sounds
		 * as it moves.
		 */
		public boolean hasAny() {
			return this.events || this.sounds;
		}

		/**
		 * Returns whether this means an entity may emit game events as it moves.
		 */
		public boolean emitsGameEvents() {
			return this.events;
		}

		/**
		 * Returns whether this means an entity may play sounds as it moves.
		 */
		public boolean playsSounds() {
			return this.sounds;
		}
	}

	@FunctionalInterface
	public interface PositionUpdater {
		void accept(Entity entity, double x, double y, double z);
	}

	/**
	 * The reason of the entity's removal.
	 * 
	 * @see Entity#setRemoved
	 */
	public static enum RemovalReason {
		/**
		 * The entity is killed.
		 */
		KILLED(true, false),
		/**
		 * The entity is discarded (despawned).
		 */
		DISCARDED(true, false),
		/**
		 * The entity is unloaded to chunk.
		 * <p>
		 * The entity should be saved.
		 */
		UNLOADED_TO_CHUNK(false, true),
		/**
		 * The entity is unloaded because the player was riding it and the player
		 * disconnected.
		 */
		UNLOADED_WITH_PLAYER(false, false),
		/**
		 * The entity changed dimension.
		 */
		CHANGED_DIMENSION(false, false);

		private final boolean destroy;
		private final boolean save;

		private RemovalReason(boolean destroy, boolean save) {
			this.destroy = destroy;
			this.save = save;
		}

		/**
		 * Returns whether the entity should be destroyed or not.
		 * <p>
		 * If an entity should be destroyed, then the entity should not be re-used and any external data on the entity will be cleared.
		 */
		public boolean shouldDestroy() {
			return this.destroy;
		}

		/**
		 * Returns whether the entity should be saved or not.
		 */
		public boolean shouldSave() {
			return this.save;
		}
	}
}
