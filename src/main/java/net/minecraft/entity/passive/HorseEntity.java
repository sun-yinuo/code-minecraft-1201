package net.minecraft.entity.passive;

import java.util.UUID;
import net.minecraft.entity.EntityData;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.SpawnReason;
import net.minecraft.entity.VariantHolder;
import net.minecraft.entity.attribute.EntityAttributeModifier;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.data.DataTracker;
import net.minecraft.entity.data.TrackedData;
import net.minecraft.entity.data.TrackedDataHandlerRegistry;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.inventory.Inventory;
import net.minecraft.item.HorseArmorItem;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.BlockSoundGroup;
import net.minecraft.sound.SoundEvent;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.Util;
import net.minecraft.util.math.random.Random;
import net.minecraft.world.LocalDifficulty;
import net.minecraft.world.ServerWorldAccess;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;

public class HorseEntity extends AbstractHorseEntity implements VariantHolder<HorseColor> {
	private static final UUID HORSE_ARMOR_BONUS_ID = UUID.fromString("556E1665-8B10-40C8-8F9D-CF9B1667F295");
	private static final TrackedData<Integer> VARIANT = DataTracker.registerData(HorseEntity.class, TrackedDataHandlerRegistry.INTEGER);

	public HorseEntity(EntityType<? extends HorseEntity> entityType, World world) {
		super(entityType, world);
	}

	@Override
	protected void initAttributes(Random random) {
		this.getAttributeInstance(EntityAttributes.GENERIC_MAX_HEALTH).setBaseValue((double)getChildHealthBonus(random::nextInt));
		this.getAttributeInstance(EntityAttributes.GENERIC_MOVEMENT_SPEED).setBaseValue(getChildMovementSpeedBonus(random::nextDouble));
		this.getAttributeInstance(EntityAttributes.HORSE_JUMP_STRENGTH).setBaseValue(getChildJumpStrengthBonus(random::nextDouble));
	}

	@Override
	protected void initDataTracker() {
		super.initDataTracker();
		this.dataTracker.startTracking(VARIANT, 0);
	}

	@Override
	public void writeCustomDataToNbt(NbtCompound nbt) {
		super.writeCustomDataToNbt(nbt);
		nbt.putInt("Variant", this.getHorseVariant());
		if (!this.items.getStack(1).isEmpty()) {
			nbt.put("ArmorItem", this.items.getStack(1).writeNbt(new NbtCompound()));
		}
	}

	public ItemStack getArmorType() {
		return this.getEquippedStack(EquipmentSlot.CHEST);
	}

	private void equipArmor(ItemStack stack) {
		this.equipStack(EquipmentSlot.CHEST, stack);
		this.setEquipmentDropChance(EquipmentSlot.CHEST, 0.0F);
	}

	@Override
	public void readCustomDataFromNbt(NbtCompound nbt) {
		super.readCustomDataFromNbt(nbt);
		this.setHorseVariant(nbt.getInt("Variant"));
		if (nbt.contains("ArmorItem", NbtElement.COMPOUND_TYPE)) {
			ItemStack itemStack = ItemStack.fromNbt(nbt.getCompound("ArmorItem"));
			if (!itemStack.isEmpty() && this.isHorseArmor(itemStack)) {
				this.items.setStack(1, itemStack);
			}
		}

		this.updateSaddle();
	}

	private void setHorseVariant(int variant) {
		this.dataTracker.set(VARIANT, variant);
	}

	private int getHorseVariant() {
		return this.dataTracker.get(VARIANT);
	}

	private void setHorseVariant(HorseColor color, HorseMarking marking) {
		this.setHorseVariant(color.getId() & 0xFF | marking.getId() << 8 & 0xFF00);
	}

	public HorseColor getVariant() {
		return HorseColor.byId(this.getHorseVariant() & 0xFF);
	}

	public void setVariant(HorseColor horseColor) {
		this.setHorseVariant(horseColor.getId() & 0xFF | this.getHorseVariant() & -256);
	}

	public HorseMarking getMarking() {
		return HorseMarking.byIndex((this.getHorseVariant() & 0xFF00) >> 8);
	}

	@Override
	protected void updateSaddle() {
		if (!this.getWorld().isClient) {
			super.updateSaddle();
			this.setArmorTypeFromStack(this.items.getStack(1));
			this.setEquipmentDropChance(EquipmentSlot.CHEST, 0.0F);
		}
	}

	private void setArmorTypeFromStack(ItemStack stack) {
		this.equipArmor(stack);
		if (!this.getWorld().isClient) {
			this.getAttributeInstance(EntityAttributes.GENERIC_ARMOR).removeModifier(HORSE_ARMOR_BONUS_ID);
			if (this.isHorseArmor(stack)) {
				int i = ((HorseArmorItem)stack.getItem()).getBonus();
				if (i != 0) {
					this.getAttributeInstance(EntityAttributes.GENERIC_ARMOR)
						.addTemporaryModifier(new EntityAttributeModifier(HORSE_ARMOR_BONUS_ID, "Horse armor bonus", (double)i, EntityAttributeModifier.Operation.ADDITION));
				}
			}
		}
	}

	@Override
	public void onInventoryChanged(Inventory sender) {
		ItemStack itemStack = this.getArmorType();
		super.onInventoryChanged(sender);
		ItemStack itemStack2 = this.getArmorType();
		if (this.age > 20 && this.isHorseArmor(itemStack2) && itemStack != itemStack2) {
			this.playSound(SoundEvents.ENTITY_HORSE_ARMOR, 0.5F, 1.0F);
		}
	}

	@Override
	protected void playWalkSound(BlockSoundGroup group) {
		super.playWalkSound(group);
		if (this.random.nextInt(10) == 0) {
			this.playSound(SoundEvents.ENTITY_HORSE_BREATHE, group.getVolume() * 0.6F, group.getPitch());
		}
	}

	@Override
	protected SoundEvent getAmbientSound() {
		return SoundEvents.ENTITY_HORSE_AMBIENT;
	}

	@Override
	protected SoundEvent getDeathSound() {
		return SoundEvents.ENTITY_HORSE_DEATH;
	}

	@Nullable
	@Override
	protected SoundEvent getEatSound() {
		return SoundEvents.ENTITY_HORSE_EAT;
	}

	@Override
	protected SoundEvent getHurtSound(DamageSource source) {
		return SoundEvents.ENTITY_HORSE_HURT;
	}

	@Override
	protected SoundEvent getAngrySound() {
		return SoundEvents.ENTITY_HORSE_ANGRY;
	}

	@Override
	public ActionResult interactMob(PlayerEntity player, Hand hand) {
		boolean bl = !this.isBaby() && this.isTame() && player.shouldCancelInteraction();
		if (!this.hasPassengers() && !bl) {
			ItemStack itemStack = player.getStackInHand(hand);
			if (!itemStack.isEmpty()) {
				if (this.isBreedingItem(itemStack)) {
					return this.interactHorse(player, itemStack);
				}

				if (!this.isTame()) {
					this.playAngrySound();
					return ActionResult.success(this.getWorld().isClient);
				}
			}

			return super.interactMob(player, hand);
		} else {
			return super.interactMob(player, hand);
		}
	}

	@Override
	public boolean canBreedWith(AnimalEntity other) {
		if (other == this) {
			return false;
		} else {
			return !(other instanceof DonkeyEntity) && !(other instanceof HorseEntity) ? false : this.canBreed() && ((AbstractHorseEntity)other).canBreed();
		}
	}

	@Nullable
	@Override
	public PassiveEntity createChild(ServerWorld world, PassiveEntity entity) {
		if (entity instanceof DonkeyEntity) {
			MuleEntity muleEntity = EntityType.MULE.create(world);
			if (muleEntity != null) {
				this.setChildAttributes(entity, muleEntity);
			}

			return muleEntity;
		} else {
			HorseEntity horseEntity = (HorseEntity)entity;
			HorseEntity horseEntity2 = EntityType.HORSE.create(world);
			if (horseEntity2 != null) {
				int i = this.random.nextInt(9);
				HorseColor horseColor;
				if (i < 4) {
					horseColor = this.getVariant();
				} else if (i < 8) {
					horseColor = horseEntity.getVariant();
				} else {
					horseColor = Util.getRandom(HorseColor.values(), this.random);
				}

				int j = this.random.nextInt(5);
				HorseMarking horseMarking;
				if (j < 2) {
					horseMarking = this.getMarking();
				} else if (j < 4) {
					horseMarking = horseEntity.getMarking();
				} else {
					horseMarking = Util.getRandom(HorseMarking.values(), this.random);
				}

				horseEntity2.setHorseVariant(horseColor, horseMarking);
				this.setChildAttributes(entity, horseEntity2);
			}

			return horseEntity2;
		}
	}

	@Override
	public boolean hasArmorSlot() {
		return true;
	}

	@Override
	public boolean isHorseArmor(ItemStack item) {
		return item.getItem() instanceof HorseArmorItem;
	}

	@Nullable
	@Override
	public EntityData initialize(
		ServerWorldAccess world, LocalDifficulty difficulty, SpawnReason spawnReason, @Nullable EntityData entityData, @Nullable NbtCompound entityNbt
	) {
		Random random = world.getRandom();
		HorseColor horseColor;
		if (entityData instanceof HorseData) {
			horseColor = ((HorseData)entityData).color;
		} else {
			horseColor = Util.getRandom(HorseColor.values(), random);
			entityData = new HorseData(horseColor);
		}

		this.setHorseVariant(horseColor, Util.getRandom(HorseMarking.values(), random));
		return super.initialize(world, difficulty, spawnReason, entityData, entityNbt);
	}

	public static class HorseData extends PassiveData {
		public final HorseColor color;

		public HorseData(HorseColor color) {
			super(true);
			this.color = color;
		}
	}
}
