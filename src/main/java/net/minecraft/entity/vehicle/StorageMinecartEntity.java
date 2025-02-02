package net.minecraft.entity.vehicle;

import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.inventory.StackReference;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.Identifier;
import net.minecraft.util.ItemScatterer;
import net.minecraft.util.collection.DefaultedList;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;

public abstract class StorageMinecartEntity extends AbstractMinecartEntity implements VehicleInventory {
	private DefaultedList<ItemStack> inventory = DefaultedList.ofSize(36, ItemStack.EMPTY);
	@Nullable
	private Identifier lootTableId;
	private long lootSeed;

	protected StorageMinecartEntity(EntityType<?> entityType, World world) {
		super(entityType, world);
	}

	protected StorageMinecartEntity(EntityType<?> type, double x, double y, double z, World world) {
		super(type, world, x, y, z);
	}

	@Override
	public void dropItems(DamageSource damageSource) {
		super.dropItems(damageSource);
		this.onBroken(damageSource, this.getWorld(), this);
	}

	@Override
	public ItemStack getStack(int slot) {
		return this.getInventoryStack(slot);
	}

	@Override
	public ItemStack removeStack(int slot, int amount) {
		return this.removeInventoryStack(slot, amount);
	}

	@Override
	public ItemStack removeStack(int slot) {
		return this.removeInventoryStack(slot);
	}

	@Override
	public void setStack(int slot, ItemStack stack) {
		this.setInventoryStack(slot, stack);
	}

	@Override
	public StackReference getStackReference(int mappedIndex) {
		return this.getInventoryStackReference(mappedIndex);
	}

	@Override
	public void markDirty() {
	}

	@Override
	public boolean canPlayerUse(PlayerEntity player) {
		return this.canPlayerAccess(player);
	}

	@Override
	public void remove(RemovalReason reason) {
		if (!this.getWorld().isClient && reason.shouldDestroy()) {
			ItemScatterer.spawn(this.getWorld(), this, this);
		}

		super.remove(reason);
	}

	@Override
	protected void writeCustomDataToNbt(NbtCompound nbt) {
		super.writeCustomDataToNbt(nbt);
		this.writeInventoryToNbt(nbt);
	}

	@Override
	protected void readCustomDataFromNbt(NbtCompound nbt) {
		super.readCustomDataFromNbt(nbt);
		this.readInventoryFromNbt(nbt);
	}

	@Override
	public ActionResult interact(PlayerEntity player, Hand hand) {
		return this.open(player);
	}

	@Override
	protected void applySlowdown() {
		float f = 0.98F;
		if (this.lootTableId == null) {
			int i = 15 - ScreenHandler.calculateComparatorOutput(this);
			f += (float)i * 0.001F;
		}

		if (this.isTouchingWater()) {
			f *= 0.95F;
		}

		this.setVelocity(this.getVelocity().multiply((double)f, 0.0, (double)f));
	}

	@Override
	public void clear() {
		this.clearInventory();
	}

	public void setLootTable(Identifier id, long lootSeed) {
		this.lootTableId = id;
		this.lootSeed = lootSeed;
	}

	@Nullable
	@Override
	public ScreenHandler createMenu(int i, PlayerInventory playerInventory, PlayerEntity playerEntity) {
		if (this.lootTableId != null && playerEntity.isSpectator()) {
			return null;
		} else {
			this.generateInventoryLoot(playerInventory.player);
			return this.getScreenHandler(i, playerInventory);
		}
	}

	protected abstract ScreenHandler getScreenHandler(int syncId, PlayerInventory playerInventory);

	@Nullable
	@Override
	public Identifier getLootTableId() {
		return this.lootTableId;
	}

	@Override
	public void setLootTableId(@Nullable Identifier lootTableId) {
		this.lootTableId = lootTableId;
	}

	@Override
	public long getLootTableSeed() {
		return this.lootSeed;
	}

	@Override
	public void setLootTableSeed(long lootTableSeed) {
		this.lootSeed = lootTableSeed;
	}

	@Override
	public DefaultedList<ItemStack> getInventory() {
		return this.inventory;
	}

	@Override
	public void resetInventory() {
		this.inventory = DefaultedList.ofSize(this.size(), ItemStack.EMPTY);
	}
}
