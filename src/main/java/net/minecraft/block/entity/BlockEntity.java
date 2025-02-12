package net.minecraft.block.entity;

import com.mojang.logging.LogUtils;
import net.fabricmc.fabric.api.attachment.v1.AttachmentTarget;
import net.fabricmc.fabric.api.blockview.v2.RenderDataBlockEntity;
import net.minecraft.block.BlockState;
import net.minecraft.item.BlockItem;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.network.listener.ClientPlayPacketListener;
import net.minecraft.network.packet.Packet;
import net.minecraft.registry.Registries;
import net.minecraft.util.Identifier;
import net.minecraft.util.crash.CrashCallable;
import net.minecraft.util.crash.CrashReportSection;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;

/**
 * A block entity is an object holding extra data about a block in a world.
 * Blocks hold their data using pre-defined, finite sets of {@link BlockState};
 * however, some blocks need to hold data that cannot be pre-defined, such as
 * inventories of chests, texts of signs, or pattern combinations of banners.
 * Block entities can hold these data.
 * 
 * <p>Block entities have two other important additions to normal blocks: they
 * can define custom rendering behaviors, and they can tick on every server tick
 * instead of randomly. Some block entities only use these without any extra data.
 * 
 * <p>Block entities are bound to a world and there is one instance of {@link
 * BlockEntity} per the block position, unlike {@link net.minecraft.block.Block}
 * or {@link BlockState} which are reused. Block entities are created using {@link
 * BlockEntityType}, a type of block entities. In most cases, block entities do not
 * have to be constructed manually except in {@link
 * net.minecraft.block.BlockEntityProvider#createBlockEntity}.
 * 
 * <p>To get the block entity at a certain position, use {@link World#getBlockEntity}.
 * Note that the block entity returned can be, in rare cases, different from the
 * one associated with the block at that position. For this reason the return value
 * should not be cast unsafely.
 * 
 * <p>Block entities, like entities, use NBT for the storage of data. The data is
 * loaded to the instance's fields in {@link #readNbt} and written to NBT in
 * {@link #writeNbt}. When a data that needs to be saved has changed, always make sure
 * to call {@link #markDirty()}.
 * 
 * <p>See {@link net.minecraft.block.BlockEntityProvider} and {@link BlockEntityType}
 * for information on creating a block with block entities.
 * 
 * <p>Block entity's data, unlike block states, are not automatically synced. Block
 * entities declare when and which data to sync. In general, block entities need to
 * sync states observable from the clients without specific interaction (such as opening
 * a container). {@link #toUpdatePacket} and {@link #toInitialChunkDataNbt} control
 * which data is sent to the client. To sync the block entity to the client, call
 * {@code serverWorld.getChunkManager().markForUpdate(this.getPos());}.
 */
public abstract class BlockEntity implements RenderDataBlockEntity, AttachmentTarget {
	private static final Logger LOGGER = LogUtils.getLogger();
	private final BlockEntityType<?> type;
	@Nullable
	protected World world;
	protected final BlockPos pos;
	protected boolean removed;
	private BlockState cachedState;

	public BlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
		this.type = type;
		this.pos = pos.toImmutable();
		this.cachedState = state;
	}

	/**
	 * {@return the block position from {@code nbt}}
	 * 
	 * <p>The passed NBT should use lowercase {@code x}, {@code y}, and {@code z}
	 * keys to store the position. This is incompatible with {@link
	 * net.minecraft.nbt.NbtHelper#fromBlockPos} that use uppercase keys.
	 */
	public static BlockPos posFromNbt(NbtCompound nbt) {
		return new BlockPos(nbt.getInt("x"), nbt.getInt("y"), nbt.getInt("z"));
	}

	/**
	 * {@return the world the block entity belongs to}
	 * 
	 * <p>This can return {@code null} during world generation.
	 */
	@Nullable
	public World getWorld() {
		return this.world;
	}

	/**
	 * Sets the world the block entity belongs to.
	 * 
	 * <p>This should not be called manually; however, this can be overridden
	 * to initialize fields dependent on the world.
	 */
	public void setWorld(World world) {
		this.world = world;
	}

	public boolean hasWorld() {
		return this.world != null;
	}

	/**
	 * Reads data from {@code nbt}. Subclasses should override this if they
	 * store a persistent data.
	 * 
	 * <p>NBT is a storage format; therefore, a data from NBT is loaded to a
	 * block entity instance's fields, which are used for other operations instead
	 * of the NBT. The data is written back to NBT when saving the block entity.
	 * 
	 * <p>{@code nbt} might not have all expected keys, or might have a key whose
	 * value does not meet the requirement (such as the type or the range). This
	 * method should fall back to a reasonable default value instead of throwing an
	 * exception.
	 * 
	 * @see #writeNbt
	 */
	public void readNbt(NbtCompound nbt) {
	}

	/**
	 * Writes data to {@code nbt}. Subclasses should override this if they
	 * store a persistent data.
	 * 
	 * <p>NBT is a storage format; therefore, a data from NBT is loaded to a
	 * block entity instance's fields, which are used for other operations instead
	 * of the NBT. The data is written back to NBT when saving the block entity.
	 * 
	 * @see #readNbt
	 */
	protected void writeNbt(NbtCompound nbt) {
	}

	/**
	 * {@return the block entity's NBT data with identifying data}
	 * 
	 * <p>In addition to data written at {@link #writeNbt}, this also
	 * writes the {@linkplain #writeIdToNbt block entity type ID} and the
	 * position of the block entity.
	 * 
	 * @see #createNbt
	 * @see #createNbtWithId
	 */
	public final NbtCompound createNbtWithIdentifyingData() {
		NbtCompound nbtCompound = this.createNbt();
		this.writeIdentifyingData(nbtCompound);
		return nbtCompound;
	}

	/**
	 * {@return the block entity's NBT data with block entity type ID}
	 * 
	 * <p>In addition to data written at {@link #writeNbt}, this also
	 * writes the {@linkplain #writeIdToNbt block entity type ID}.
	 * 
	 * @see #createNbt
	 * @see #createNbtWithIdentifyingData
	 */
	public final NbtCompound createNbtWithId() {
		NbtCompound nbtCompound = this.createNbt();
		this.writeIdToNbt(nbtCompound);
		return nbtCompound;
	}

	/**
	 * {@return the block entity's NBT data}
	 * 
	 * <p>Internally, this calls {@link #writeNbt} with a new {@link NbtCompound}
	 * and returns the compound.
	 * 
	 * @see #writeNbt
	 * @see #createNbtWithIdentifyingData
	 * @see #createNbtWithId
	 */
	public final NbtCompound createNbt() {
		NbtCompound nbtCompound = new NbtCompound();
		this.writeNbt(nbtCompound);
		return nbtCompound;
	}

	/**
	 * Writes the block entity type ID to {@code nbt} under the {@code id} key.
	 * 
	 * @throws RuntimeException if the block entity type is not registered in
	 * the registry
	 */
	private void writeIdToNbt(NbtCompound nbt) {
		Identifier identifier = BlockEntityType.getId(this.getType());
		if (identifier == null) {
			throw new RuntimeException(this.getClass() + " is missing a mapping! This is a bug!");
		} else {
			nbt.putString("id", identifier.toString());
		}
	}

	/**
	 * Writes the ID of {@code type} to {@code nbt} under the {@code id} key.
	 */
	public static void writeIdToNbt(NbtCompound nbt, BlockEntityType<?> type) {
		nbt.putString("id", BlockEntityType.getId(type).toString());
	}

	/**
	 * Sets {@code stack}'s {@code net.minecraft.item.BlockItem#BLOCK_ENTITY_TAG_KEY}
	 * NBT value to {@linkplain #createNbt the block entity's NBT data}.
	 */
	public void setStackNbt(ItemStack stack) {
		BlockItem.setBlockEntityNbt(stack, this.getType(), this.createNbt());
	}

	/**
	 * Writes to {@code nbt} the block entity type ID under the {@code id} key,
	 * and the block's position under {@code x}, {@code y}, and {@code z} keys.
	 * 
	 * @throws RuntimeException if the block entity type is not registered in
	 * the registry
	 */
	private void writeIdentifyingData(NbtCompound nbt) {
		this.writeIdToNbt(nbt);
		nbt.putInt("x", this.pos.getX());
		nbt.putInt("y", this.pos.getY());
		nbt.putInt("z", this.pos.getZ());
	}

	/**
	 * {@return the new block entity loaded from {@code nbt}, or {@code null} if it fails}
	 * 
	 * <p>This is used during chunk loading. This can fail if {@code nbt} has an improper or
	 * unregistered {@code id}, or if {@link #readNbt} throws an exception; in these cases,
	 * this logs an error and returns {@code null}.
	 */
	@Nullable
	public static BlockEntity createFromNbt(BlockPos pos, BlockState state, NbtCompound nbt) {
		String string = nbt.getString("id");
		Identifier identifier = Identifier.tryParse(string);
		if (identifier == null) {
			LOGGER.error("Block entity has invalid type: {}", string);
			return null;
		} else {
			return (BlockEntity)Registries.BLOCK_ENTITY_TYPE.getOrEmpty(identifier).map(type -> {
				try {
					return type.instantiate(pos, state);
				} catch (Throwable var5) {
					LOGGER.error("Failed to create block entity {}", string, var5);
					return null;
				}
			}).map(blockEntity -> {
				try {
					blockEntity.readNbt(nbt);
					return blockEntity;
				} catch (Throwable var4x) {
					LOGGER.error("Failed to load data for block entity {}", string, var4x);
					return null;
				}
			}).orElseGet(() -> {
				LOGGER.warn("Skipping BlockEntity with id {}", string);
				return null;
			});
		}
	}

	/**
	 * Marks this block entity as dirty and that it needs to be saved.
	 * This also triggers {@linkplain World#updateComparators comparator update}.
	 * 
	 * <p>This <strong>must be called</strong> when something changed in a way that
	 * affects the saved NBT; otherwise, the game might not save the block entity.
	 */
	public void markDirty() {
		if (this.world != null) {
			markDirty(this.world, this.pos, this.cachedState);
		}
	}

	protected static void markDirty(World world, BlockPos pos, BlockState state) {
		world.markDirty(pos);
		if (!state.isAir()) {
			world.updateComparators(pos, state.getBlock());
		}
	}

	/**
	 * {@return the block entity's position}
	 */
	public BlockPos getPos() {
		return this.pos;
	}

	/**
	 * {@return the cached block state at the block entity's position}
	 * 
	 * <p>This is faster than calling {@link World#getBlockState}.
	 */
	public BlockState getCachedState() {
		return this.cachedState;
	}

	/**
	 * {@return the packet to send to nearby players when the block entity's observable
	 * state changes, or {@code null} to not send the packet}
	 * 
	 * <p>If the data returned by {@link #toInitialChunkDataNbt initial chunk data} is suitable
	 * for updates, the following shortcut can be used to create an update packet: {@code
	 * BlockEntityUpdateS2CPacket.create(this)}. The NBT will be passed to {@link #readNbt}
	 * on the client.
	 * 
	 * <p>"Observable state" is a state that clients can observe without specific interaction.
	 * For example, {@link CampfireBlockEntity}'s cooked items are observable states,
	 * but chests' inventories are not observable states, since the player must first open
	 * that chest before they can see the contents.
	 * 
	 * <p>To sync block entity data using this method, use {@code
	 * serverWorld.getChunkManager().markForUpdate(this.getPos());}.
	 * 
	 * @see #toInitialChunkDataNbt
	 */
	@Nullable
	public Packet<ClientPlayPacketListener> toUpdatePacket() {
		return null;
	}

	/**
	 * {@return the serialized state of this block entity that is observable by clients}
	 * 
	 * <p>This is sent alongside the initial chunk data, as well as when the block
	 * entity implements {@link #toUpdatePacket} and decides to use the default
	 * {@link net.minecraft.network.packet.s2c.play.BlockEntityUpdateS2CPacket}.
	 * 
	 * <p>"Observable state" is a state that clients can observe without specific interaction.
	 * For example, {@link CampfireBlockEntity}'s cooked items are observable states,
	 * but chests' inventories are not observable states, since the player must first open
	 * that chest before they can see the contents.
	 * 
	 * <p>To send all NBT data of this block entity saved to disk, return {@link #createNbt}.
	 * 
	 * @see #toUpdatePacket
	 */
	public NbtCompound toInitialChunkDataNbt() {
		return new NbtCompound();
	}

	public boolean isRemoved() {
		return this.removed;
	}

	public void markRemoved() {
		this.removed = true;
	}

	public void cancelRemoval() {
		this.removed = false;
	}

	/**
	 * If this block entity's block extends {@link net.minecraft.block.BlockWithEntity},
	 * this is called inside {@link net.minecraft.block.AbstractBlock#onSyncedBlockEvent}.
	 * 
	 * @see net.minecraft.block.AbstractBlock#onSyncedBlockEvent
	 */
	public boolean onSyncedBlockEvent(int type, int data) {
		return false;
	}

	public void populateCrashReport(CrashReportSection crashReportSection) {
		crashReportSection.add(
			"Name", (CrashCallable<String>)(() -> Registries.BLOCK_ENTITY_TYPE.getId(this.getType()) + " // " + this.getClass().getCanonicalName())
		);
		if (this.world != null) {
			CrashReportSection.addBlockInfo(crashReportSection, this.world, this.pos, this.getCachedState());
			CrashReportSection.addBlockInfo(crashReportSection, this.world, this.pos, this.world.getBlockState(this.pos));
		}
	}

	/**
	 * {@return whether the block item should require the player to have operator
	 * permissions to copy the block entity data on placement}
	 * 
	 * <p>Block entities that can execute commands should override this to return
	 * {@code true}.
	 * 
	 * @see net.minecraft.entity.player.PlayerEntity#isCreativeLevelTwoOp
	 */
	public boolean copyItemDataRequiresOperator() {
		return false;
	}

	public BlockEntityType<?> getType() {
		return this.type;
	}

	@Deprecated
	public void setCachedState(BlockState state) {
		this.cachedState = state;
	}
}
