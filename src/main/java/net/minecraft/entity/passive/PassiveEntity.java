package net.minecraft.entity.passive;

import net.minecraft.entity.EntityData;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.SpawnReason;
import net.minecraft.entity.data.DataTracker;
import net.minecraft.entity.data.TrackedData;
import net.minecraft.entity.data.TrackedDataHandlerRegistry;
import net.minecraft.entity.mob.PathAwareEntity;
import net.minecraft.entity.vehicle.BoatEntity;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.world.LocalDifficulty;
import net.minecraft.world.ServerWorldAccess;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;

public abstract class PassiveEntity extends PathAwareEntity {
	private static final TrackedData<Boolean> CHILD = DataTracker.registerData(PassiveEntity.class, TrackedDataHandlerRegistry.BOOLEAN);
	public static final int BABY_AGE = -24000;
	private static final int HAPPY_TICKS = 40;
	protected int breedingAge;
	protected int forcedAge;
	protected int happyTicksRemaining;

	protected PassiveEntity(EntityType<? extends PassiveEntity> entityType, World world) {
		super(entityType, world);
	}

	@Override
	public EntityData initialize(
		ServerWorldAccess world, LocalDifficulty difficulty, SpawnReason spawnReason, @Nullable EntityData entityData, @Nullable NbtCompound entityNbt
	) {
		if (entityData == null) {
			entityData = new PassiveData(true);
		}

		PassiveData passiveData = (PassiveData)entityData;
		if (passiveData.canSpawnBaby() && passiveData.getSpawnedCount() > 0 && world.getRandom().nextFloat() <= passiveData.getBabyChance()) {
			this.setBreedingAge(-24000);
		}

		passiveData.countSpawned();
		return super.initialize(world, difficulty, spawnReason, entityData, entityNbt);
	}

	@Nullable
	public abstract PassiveEntity createChild(ServerWorld world, PassiveEntity entity);

	@Override
	protected void initDataTracker() {
		super.initDataTracker();
		this.dataTracker.startTracking(CHILD, false);
	}

	public boolean isReadyToBreed() {
		return false;
	}

	public int getBreedingAge() {
		if (this.getWorld().isClient) {
			return this.dataTracker.get(CHILD) ? -1 : 1;
		} else {
			return this.breedingAge;
		}
	}

	public void growUp(int age, boolean overGrow) {
		int i = this.getBreedingAge();
		i += age * 20;
		if (i > 0) {
			i = 0;
		}

		int k = i - i;
		this.setBreedingAge(i);
		if (overGrow) {
			this.forcedAge += k;
			if (this.happyTicksRemaining == 0) {
				this.happyTicksRemaining = 40;
			}
		}

		if (this.getBreedingAge() == 0) {
			this.setBreedingAge(this.forcedAge);
		}
	}

	public void growUp(int age) {
		this.growUp(age, false);
	}

	public void setBreedingAge(int age) {
		int i = this.getBreedingAge();
		this.breedingAge = age;
		if (i < 0 && age >= 0 || i >= 0 && age < 0) {
			this.dataTracker.set(CHILD, age < 0);
			this.onGrowUp();
		}
	}

	@Override
	public void writeCustomDataToNbt(NbtCompound nbt) {
		super.writeCustomDataToNbt(nbt);
		nbt.putInt("Age", this.getBreedingAge());
		nbt.putInt("ForcedAge", this.forcedAge);
	}

	@Override
	public void readCustomDataFromNbt(NbtCompound nbt) {
		super.readCustomDataFromNbt(nbt);
		this.setBreedingAge(nbt.getInt("Age"));
		this.forcedAge = nbt.getInt("ForcedAge");
	}

	@Override
	public void onTrackedDataSet(TrackedData<?> data) {
		if (CHILD.equals(data)) {
			this.calculateDimensions();
		}

		super.onTrackedDataSet(data);
	}

	@Override
	public void tickMovement() {
		super.tickMovement();
		if (this.getWorld().isClient) {
			if (this.happyTicksRemaining > 0) {
				if (this.happyTicksRemaining % 4 == 0) {
					this.getWorld().addParticle(ParticleTypes.HAPPY_VILLAGER, this.getParticleX(1.0), this.getRandomBodyY() + 0.5, this.getParticleZ(1.0), 0.0, 0.0, 0.0);
				}

				this.happyTicksRemaining--;
			}
		} else if (this.isAlive()) {
			int i = this.getBreedingAge();
			if (i < 0) {
				this.setBreedingAge(++i);
			} else if (i > 0) {
				this.setBreedingAge(--i);
			}
		}
	}

	protected void onGrowUp() {
		if (!this.isBaby() && this.hasVehicle() && this.getVehicle() instanceof BoatEntity boatEntity && !boatEntity.isSmallerThanBoat(this)) {
			this.stopRiding();
		}
	}

	@Override
	public boolean isBaby() {
		return this.getBreedingAge() < 0;
	}

	@Override
	public void setBaby(boolean baby) {
		this.setBreedingAge(baby ? -24000 : 0);
	}

	/**
	 * Calculates the age to pass to {@link #growUp(int)} in seconds.
	 * 
	 * @apiNote When passing the value from {@link #getBreedingAge()}, make sure to
	 * negate the value; otherwise, the entity's age will decrease.
	 * 
	 * @param breedingAge the current, negated breeding age (in ticks)
	 */
	public static int toGrowUpAge(int breedingAge) {
		return (int)((float)(breedingAge / 20) * 0.1F);
	}

	public static class PassiveData implements EntityData {
		private int spawnCount;
		private final boolean babyAllowed;
		private final float babyChance;

		private PassiveData(boolean babyAllowed, float babyChance) {
			this.babyAllowed = babyAllowed;
			this.babyChance = babyChance;
		}

		public PassiveData(boolean babyAllowed) {
			this(babyAllowed, 0.05F);
		}

		public PassiveData(float babyChance) {
			this(true, babyChance);
		}

		public int getSpawnedCount() {
			return this.spawnCount;
		}

		public void countSpawned() {
			this.spawnCount++;
		}

		public boolean canSpawnBaby() {
			return this.babyAllowed;
		}

		public float getBabyChance() {
			return this.babyChance;
		}
	}
}
