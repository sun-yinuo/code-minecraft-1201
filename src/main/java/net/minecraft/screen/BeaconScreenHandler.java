package net.minecraft.screen;

import java.util.Optional;
import net.minecraft.block.Blocks;
import net.minecraft.entity.effect.StatusEffect;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.inventory.Inventory;
import net.minecraft.inventory.SimpleInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.tag.ItemTags;
import net.minecraft.screen.slot.Slot;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;

public class BeaconScreenHandler extends ScreenHandler {
	private static final int PAYMENT_SLOT_ID = 0;
	private static final int BEACON_INVENTORY_SIZE = 1;
	private static final int PROPERTY_COUNT = 3;
	private static final int INVENTORY_START = 1;
	private static final int INVENTORY_END = 28;
	private static final int HOTBAR_START = 28;
	private static final int HOTBAR_END = 37;
	private final Inventory payment = new SimpleInventory(1) {
		@Override
		public boolean isValid(int slot, ItemStack stack) {
			return stack.isIn(ItemTags.BEACON_PAYMENT_ITEMS);
		}

		@Override
		public int getMaxCountPerStack() {
			return 1;
		}
	};
	private final BeaconScreenHandler.PaymentSlot paymentSlot;
	private final ScreenHandlerContext context;
	private final PropertyDelegate propertyDelegate;

	public BeaconScreenHandler(int syncId, Inventory inventory) {
		this(syncId, inventory, new ArrayPropertyDelegate(3), ScreenHandlerContext.EMPTY);
	}

	public BeaconScreenHandler(int syncId, Inventory inventory, PropertyDelegate propertyDelegate, ScreenHandlerContext context) {
		super(ScreenHandlerType.BEACON, syncId);
		checkDataCount(propertyDelegate, 3);
		this.propertyDelegate = propertyDelegate;
		this.context = context;
		this.paymentSlot = new BeaconScreenHandler.PaymentSlot(this.payment, 0, 136, 110);
		this.addSlot(this.paymentSlot);
		this.addProperties(propertyDelegate);
		int i = 36;
		int j = 137;

		for (int k = 0; k < 3; k++) {
			for (int l = 0; l < 9; l++) {
				this.addSlot(new Slot(inventory, l + k * 9 + 9, 36 + l * 18, 137 + k * 18));
			}
		}

		for (int k = 0; k < 9; k++) {
			this.addSlot(new Slot(inventory, k, 36 + k * 18, 195));
		}
	}

	@Override
	public void onClosed(PlayerEntity player) {
		super.onClosed(player);
		if (!player.getWorld().isClient) {
			ItemStack itemStack = this.paymentSlot.takeStack(this.paymentSlot.getMaxItemCount());
			if (!itemStack.isEmpty()) {
				player.dropItem(itemStack, false);
			}
		}
	}

	@Override
	public boolean canUse(PlayerEntity player) {
		return canUse(this.context, player, Blocks.BEACON);
	}

	@Override
	public void setProperty(int id, int value) {
		super.setProperty(id, value);
		this.sendContentUpdates();
	}

	@Override
	public ItemStack quickMove(PlayerEntity player, int slot) {
		ItemStack itemStack = ItemStack.EMPTY;
		Slot slot2 = this.slots.get(slot);
		if (slot2 != null && slot2.hasStack()) {
			ItemStack itemStack2 = slot2.getStack();
			itemStack = itemStack2.copy();
			if (slot == 0) {
				if (!this.insertItem(itemStack2, 1, 37, true)) {
					return ItemStack.EMPTY;
				}

				slot2.onQuickTransfer(itemStack2, itemStack);
			} else if (!this.paymentSlot.hasStack() && this.paymentSlot.canInsert(itemStack2) && itemStack2.getCount() == 1) {
				if (!this.insertItem(itemStack2, 0, 1, false)) {
					return ItemStack.EMPTY;
				}
			} else if (slot >= 1 && slot < 28) {
				if (!this.insertItem(itemStack2, 28, 37, false)) {
					return ItemStack.EMPTY;
				}
			} else if (slot >= 28 && slot < 37) {
				if (!this.insertItem(itemStack2, 1, 28, false)) {
					return ItemStack.EMPTY;
				}
			} else if (!this.insertItem(itemStack2, 1, 37, false)) {
				return ItemStack.EMPTY;
			}

			if (itemStack2.isEmpty()) {
				slot2.setStack(ItemStack.EMPTY);
			} else {
				slot2.markDirty();
			}

			if (itemStack2.getCount() == itemStack.getCount()) {
				return ItemStack.EMPTY;
			}

			slot2.onTakeItem(player, itemStack2);
		}

		return itemStack;
	}

	public int getProperties() {
		return this.propertyDelegate.get(0);
	}

	@Nullable
	public StatusEffect getPrimaryEffect() {
		return StatusEffect.byRawId(this.propertyDelegate.get(1));
	}

	@Nullable
	public StatusEffect getSecondaryEffect() {
		return StatusEffect.byRawId(this.propertyDelegate.get(2));
	}

	public void setEffects(Optional<StatusEffect> primary, Optional<StatusEffect> secondary) {
		if (this.paymentSlot.hasStack()) {
			this.propertyDelegate.set(1, (Integer)primary.map(StatusEffect::getRawId).orElse(-1));
			this.propertyDelegate.set(2, (Integer)secondary.map(StatusEffect::getRawId).orElse(-1));
			this.paymentSlot.takeStack(1);
			this.context.run(World::markDirty);
		}
	}

	public boolean hasPayment() {
		return !this.payment.getStack(0).isEmpty();
	}

	class PaymentSlot extends Slot {
		public PaymentSlot(Inventory inventory, int index, int x, int y) {
			super(inventory, index, x, y);
		}

		@Override
		public boolean canInsert(ItemStack stack) {
			return stack.isIn(ItemTags.BEACON_PAYMENT_ITEMS);
		}

		@Override
		public int getMaxItemCount() {
			return 1;
		}
	}
}
