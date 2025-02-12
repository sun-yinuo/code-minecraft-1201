package net.minecraft.entity.passive;

import java.util.UUID;
import net.minecraft.block.BlockState;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityData;
import net.minecraft.entity.EntityDimensions;
import net.minecraft.entity.EntityPose;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.SpawnReason;
import net.minecraft.entity.ai.goal.ActiveTargetGoal;
import net.minecraft.entity.ai.goal.EscapeDangerGoal;
import net.minecraft.entity.ai.goal.FollowParentGoal;
import net.minecraft.entity.ai.goal.LookAroundGoal;
import net.minecraft.entity.ai.goal.LookAtEntityGoal;
import net.minecraft.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.entity.ai.goal.RevengeGoal;
import net.minecraft.entity.ai.goal.SwimGoal;
import net.minecraft.entity.ai.goal.UniversalAngerGoal;
import net.minecraft.entity.ai.goal.WanderAroundGoal;
import net.minecraft.entity.attribute.DefaultAttributeContainer;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.data.DataTracker;
import net.minecraft.entity.data.TrackedData;
import net.minecraft.entity.data.TrackedDataHandlerRegistry;
import net.minecraft.entity.mob.Angerable;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.registry.tag.BiomeTags;
import net.minecraft.registry.tag.BlockTags;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundEvent;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.TimeHelper;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.intprovider.UniformIntProvider;
import net.minecraft.util.math.random.Random;
import net.minecraft.world.LocalDifficulty;
import net.minecraft.world.ServerWorldAccess;
import net.minecraft.world.World;
import net.minecraft.world.WorldAccess;
import net.minecraft.world.biome.Biome;
import org.jetbrains.annotations.Nullable;

public class PolarBearEntity extends AnimalEntity implements Angerable {
	private static final TrackedData<Boolean> WARNING = DataTracker.registerData(PolarBearEntity.class, TrackedDataHandlerRegistry.BOOLEAN);
	private static final float field_30352 = 6.0F;
	private float lastWarningAnimationProgress;
	private float warningAnimationProgress;
	private int warningSoundCooldown;
	private static final UniformIntProvider ANGER_TIME_RANGE = TimeHelper.betweenSeconds(20, 39);
	private int angerTime;
	@Nullable
	private UUID angryAt;

	public PolarBearEntity(EntityType<? extends PolarBearEntity> entityType, World world) {
		super(entityType, world);
	}

	@Nullable
	@Override
	public PassiveEntity createChild(ServerWorld world, PassiveEntity entity) {
		return EntityType.POLAR_BEAR.create(world);
	}

	@Override
	public boolean isBreedingItem(ItemStack stack) {
		return false;
	}

	@Override
	protected void initGoals() {
		super.initGoals();
		this.goalSelector.add(0, new SwimGoal(this));
		this.goalSelector.add(1, new AttackGoal());
		this.goalSelector.add(1, new PolarBearEscapeDangerGoal());
		this.goalSelector.add(4, new FollowParentGoal(this, 1.25));
		this.goalSelector.add(5, new WanderAroundGoal(this, 1.0));
		this.goalSelector.add(6, new LookAtEntityGoal(this, PlayerEntity.class, 6.0F));
		this.goalSelector.add(7, new LookAroundGoal(this));
		this.targetSelector.add(1, new PolarBearRevengeGoal());
		this.targetSelector.add(2, new ProtectBabiesGoal());
		this.targetSelector.add(3, new ActiveTargetGoal(this, PlayerEntity.class, 10, true, false, this::shouldAngerAt));
		this.targetSelector.add(4, new ActiveTargetGoal(this, FoxEntity.class, 10, true, true, null));
		this.targetSelector.add(5, new UniversalAngerGoal<>(this, false));
	}

	public static DefaultAttributeContainer.Builder createPolarBearAttributes() {
		return MobEntity.createMobAttributes()
			.add(EntityAttributes.GENERIC_MAX_HEALTH, 30.0)
			.add(EntityAttributes.GENERIC_FOLLOW_RANGE, 20.0)
			.add(EntityAttributes.GENERIC_MOVEMENT_SPEED, 0.25)
			.add(EntityAttributes.GENERIC_ATTACK_DAMAGE, 6.0);
	}

	public static boolean canSpawn(EntityType<PolarBearEntity> type, WorldAccess world, SpawnReason spawnReason, BlockPos pos, Random random) {
		RegistryEntry<Biome> registryEntry = world.getBiome(pos);
		return !registryEntry.isIn(BiomeTags.POLAR_BEARS_SPAWN_ON_ALTERNATE_BLOCKS)
			? isValidNaturalSpawn(type, world, spawnReason, pos, random)
			: isLightLevelValidForNaturalSpawn(world, pos) && world.getBlockState(pos.down()).isIn(BlockTags.POLAR_BEARS_SPAWNABLE_ON_ALTERNATE);
	}

	@Override
	public void readCustomDataFromNbt(NbtCompound nbt) {
		super.readCustomDataFromNbt(nbt);
		this.readAngerFromNbt(this.getWorld(), nbt);
	}

	@Override
	public void writeCustomDataToNbt(NbtCompound nbt) {
		super.writeCustomDataToNbt(nbt);
		this.writeAngerToNbt(nbt);
	}

	@Override
	public void chooseRandomAngerTime() {
		this.setAngerTime(ANGER_TIME_RANGE.get(this.random));
	}

	@Override
	public void setAngerTime(int angerTime) {
		this.angerTime = angerTime;
	}

	@Override
	public int getAngerTime() {
		return this.angerTime;
	}

	@Override
	public void setAngryAt(@Nullable UUID angryAt) {
		this.angryAt = angryAt;
	}

	@Nullable
	@Override
	public UUID getAngryAt() {
		return this.angryAt;
	}

	@Override
	protected SoundEvent getAmbientSound() {
		return this.isBaby() ? SoundEvents.ENTITY_POLAR_BEAR_AMBIENT_BABY : SoundEvents.ENTITY_POLAR_BEAR_AMBIENT;
	}

	@Override
	protected SoundEvent getHurtSound(DamageSource source) {
		return SoundEvents.ENTITY_POLAR_BEAR_HURT;
	}

	@Override
	protected SoundEvent getDeathSound() {
		return SoundEvents.ENTITY_POLAR_BEAR_DEATH;
	}

	@Override
	protected void playStepSound(BlockPos pos, BlockState state) {
		this.playSound(SoundEvents.ENTITY_POLAR_BEAR_STEP, 0.15F, 1.0F);
	}

	protected void playWarningSound() {
		if (this.warningSoundCooldown <= 0) {
			this.playSound(SoundEvents.ENTITY_POLAR_BEAR_WARNING, 1.0F, this.getSoundPitch());
			this.warningSoundCooldown = 40;
		}
	}

	@Override
	protected void initDataTracker() {
		super.initDataTracker();
		this.dataTracker.startTracking(WARNING, false);
	}

	@Override
	public void tick() {
		super.tick();
		if (this.getWorld().isClient) {
			if (this.warningAnimationProgress != this.lastWarningAnimationProgress) {
				this.calculateDimensions();
			}

			this.lastWarningAnimationProgress = this.warningAnimationProgress;
			if (this.isWarning()) {
				this.warningAnimationProgress = MathHelper.clamp(this.warningAnimationProgress + 1.0F, 0.0F, 6.0F);
			} else {
				this.warningAnimationProgress = MathHelper.clamp(this.warningAnimationProgress - 1.0F, 0.0F, 6.0F);
			}
		}

		if (this.warningSoundCooldown > 0) {
			this.warningSoundCooldown--;
		}

		if (!this.getWorld().isClient) {
			this.tickAngerLogic((ServerWorld)this.getWorld(), true);
		}
	}

	@Override
	public EntityDimensions getDimensions(EntityPose pose) {
		if (this.warningAnimationProgress > 0.0F) {
			float f = this.warningAnimationProgress / 6.0F;
			float g = 1.0F + f;
			return super.getDimensions(pose).scaled(1.0F, g);
		} else {
			return super.getDimensions(pose);
		}
	}

	@Override
	public boolean tryAttack(Entity target) {
		boolean bl = target.damage(this.getDamageSources().mobAttack(this), (float)((int)this.getAttributeValue(EntityAttributes.GENERIC_ATTACK_DAMAGE)));
		if (bl) {
			this.applyDamageEffects(this, target);
		}

		return bl;
	}

	public boolean isWarning() {
		return this.dataTracker.get(WARNING);
	}

	public void setWarning(boolean warning) {
		this.dataTracker.set(WARNING, warning);
	}

	public float getWarningAnimationProgress(float tickDelta) {
		return MathHelper.lerp(tickDelta, this.lastWarningAnimationProgress, this.warningAnimationProgress) / 6.0F;
	}

	@Override
	protected float getBaseMovementSpeedMultiplier() {
		return 0.98F;
	}

	@Override
	public EntityData initialize(
		ServerWorldAccess world, LocalDifficulty difficulty, SpawnReason spawnReason, @Nullable EntityData entityData, @Nullable NbtCompound entityNbt
	) {
		if (entityData == null) {
			entityData = new PassiveData(1.0F);
		}

		return super.initialize(world, difficulty, spawnReason, entityData, entityNbt);
	}

	class AttackGoal extends MeleeAttackGoal {
		public AttackGoal() {
			super(PolarBearEntity.this, 1.25, true);
		}

		@Override
		protected void attack(LivingEntity target, double squaredDistance) {
			double d = this.getSquaredMaxAttackDistance(target);
			if (squaredDistance <= d && this.isCooledDown()) {
				this.resetCooldown();
				this.mob.tryAttack(target);
				PolarBearEntity.this.setWarning(false);
			} else if (squaredDistance <= d * 2.0) {
				if (this.isCooledDown()) {
					PolarBearEntity.this.setWarning(false);
					this.resetCooldown();
				}

				if (this.getCooldown() <= 10) {
					PolarBearEntity.this.setWarning(true);
					PolarBearEntity.this.playWarningSound();
				}
			} else {
				this.resetCooldown();
				PolarBearEntity.this.setWarning(false);
			}
		}

		@Override
		public void stop() {
			PolarBearEntity.this.setWarning(false);
			super.stop();
		}

		@Override
		protected double getSquaredMaxAttackDistance(LivingEntity entity) {
			return (double)(4.0F + entity.getWidth());
		}
	}

	class PolarBearEscapeDangerGoal extends EscapeDangerGoal {
		public PolarBearEscapeDangerGoal() {
			super(PolarBearEntity.this, 2.0);
		}

		@Override
		protected boolean isInDanger() {
			return this.mob.getAttacker() != null && this.mob.isBaby() || this.mob.isOnFire();
		}
	}

	class PolarBearRevengeGoal extends RevengeGoal {
		public PolarBearRevengeGoal() {
			super(PolarBearEntity.this);
		}

		@Override
		public void start() {
			super.start();
			if (PolarBearEntity.this.isBaby()) {
				this.callSameTypeForRevenge();
				this.stop();
			}
		}

		@Override
		protected void setMobEntityTarget(MobEntity mob, LivingEntity target) {
			if (mob instanceof PolarBearEntity && !mob.isBaby()) {
				super.setMobEntityTarget(mob, target);
			}
		}
	}

	/**
	 * A target goal on players when this polar bear is near a baby polar bear.
	 */
	class ProtectBabiesGoal extends ActiveTargetGoal<PlayerEntity> {
		public ProtectBabiesGoal() {
			super(PolarBearEntity.this, PlayerEntity.class, 20, true, true, null);
		}

		@Override
		public boolean canStart() {
			if (PolarBearEntity.this.isBaby()) {
				return false;
			} else {
				if (super.canStart()) {
					for (PolarBearEntity polarBearEntity : PolarBearEntity.this.getWorld()
						.getNonSpectatingEntities(PolarBearEntity.class, PolarBearEntity.this.getBoundingBox().expand(8.0, 4.0, 8.0))) {
						if (polarBearEntity.isBaby()) {
							return true;
						}
					}
				}

				return false;
			}
		}

		@Override
		protected double getFollowRange() {
			return super.getFollowRange() * 0.5;
		}
	}
}
