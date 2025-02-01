package net.minecraft.entity.ai.control;

import java.util.Optional;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;

/**
 * The look control adjusts a mob's rotations to look at a target position.
 * look 控件调整生物的旋转以查看目标位置。
 */
public class LookControl implements Control {
	//实体
	protected final MobEntity entity;
	//YAW MAX
	protected float maxYawChange;
	//Pitch MAX
	protected float maxPitchChange;
	//时间计时器
	protected int lookAtTimer;
	//xyz
	protected double x;
	protected double y;
	protected double z;

	/**
	 * LookControl
	 * @param entity 设置entity
	 */
	public LookControl(MobEntity entity) {
		this.entity = entity;
	}

	/**
	 * lookAt
	 * @param direction Vec3d
	 */
	public void lookAt(Vec3d direction) {
		this.lookAt(direction.x, direction.y, direction.z);
	}

	/**
	 * lookAt
	 * @param entity Entity
	 */
	public void lookAt(Entity entity) {
		this.lookAt(entity.getX(), getLookingHeightFor(entity), entity.getZ());
	}

	/**
	 * lookAt
	 * @param entity Entity
	 * @param maxYawChange 限制
	 * @param maxPitchChange 限制
	 */
	public void lookAt(Entity entity, float maxYawChange, float maxPitchChange) {
		this.lookAt(entity.getX(), getLookingHeightFor(entity), entity.getZ(), maxYawChange, maxPitchChange);
	}

	/**
	 * lookAt
	 * @param x x
	 * @param y y
	 * @param z z
	 */
	public void lookAt(double x, double y, double z) {
		this.lookAt(x, y, z, (float)this.entity.getMaxLookYawChange(), (float)this.entity.getMaxLookPitchChange());
	}

	/**
	 * lookAt
	 * @param x x
	 * @param y y
	 * @param z z
	 * @param maxYawChange 限制
	 * @param maxPitchChange 限制
	 */
	public void lookAt(double x, double y, double z, float maxYawChange, float maxPitchChange) {
		this.x = x;
		this.y = y;
		this.z = z;
		this.maxYawChange = maxYawChange;
		this.maxPitchChange = maxPitchChange;
		this.lookAtTimer = 2;
	}


	public void tick() {
		if (this.shouldStayHorizontal()) {
			//归中
			this.entity.setPitch(0.0F);
		}

		//正在看
		if (this.lookAtTimer > 0) {
			//减了
			this.lookAtTimer--;
			//调整偏航角
			this.getTargetYaw().ifPresent(yaw -> this.entity.headYaw = this.changeAngle(this.entity.headYaw, yaw, this.maxYawChange));
			//调整俯仰角
			this.getTargetPitch().ifPresent(pitch -> this.entity.setPitch(this.changeAngle(this.entity.getPitch(), pitch, this.maxPitchChange)));
		} else {
			//恢复默认
			this.entity.headYaw = this.changeAngle(this.entity.headYaw, this.entity.bodyYaw, 10.0F);
		}

		//限制
		this.clampHeadYaw();
	}


	/**
	 * 头部旋转限制
	 */
	protected void clampHeadYaw() {
		//导航不空闲
		if (!this.entity.getNavigation().isIdle()) {
			//限制
			this.entity.headYaw = MathHelper.clampAngle(this.entity.headYaw, this.entity.bodyYaw, (float)this.entity.getMaxHeadRotation());
		}
	}

	/**
	 * 保持水平?
	 * @return always true
	 */
	protected boolean shouldStayHorizontal() {
		return true;
	}

	/**
	 * 正在查看特定位置?
	 * @return boolean
	 */
	public boolean isLookingAtSpecificPosition() {
		return this.lookAtTimer > 0;
	}

	/**
	 * getLookX
	 * @return x
	 */
	public double getLookX() {
		return this.x;
	}

	/**
	 * getLookY
	 * @return Y
	 */
	public double getLookY() {
		return this.y;
	}

	/**
	 * getLookZ
	 * @return Z
	 */
	public double getLookZ() {
		return this.z;
	}

	/**
	 * 获取目标Pitch
	 * @return Optional<Float>
	 */
	protected Optional<Float> getTargetPitch() {
		double d = this.x - this.entity.getX();
		double e = this.y - this.entity.getEyeY();
		double f = this.z - this.entity.getZ();
		double g = Math.sqrt(d * d + f * f);
		return !(Math.abs(e) > 1.0E-5F) && !(Math.abs(g) > 1.0E-5F) ? Optional.empty() : Optional.of((float)(-(MathHelper.atan2(e, g) * 180.0F / (float)Math.PI)));
	}

	/**
	 * 获取目标YAW
	 * @return Optional<Float>
	 */
	protected Optional<Float> getTargetYaw() {
		double d = this.x - this.entity.getX();
		double e = this.z - this.entity.getZ();
		return !(Math.abs(e) > 1.0E-5F) && !(Math.abs(d) > 1.0E-5F)
			? Optional.empty()
			: Optional.of((float)(MathHelper.atan2(e, d) * 180.0F / (float)Math.PI) - 90.0F);
	}

	/**
	 * Changes the angle from {@code from} to {@code to}, or by {@code max} degrees
	 * if {@code to} is too big a change.
	 */
	protected float changeAngle(float from, float to, float max) {
		float f = MathHelper.subtractAngles(from, to);
		float g = MathHelper.clamp(f, -max, max);
		return from + g;
	}

	private static double getLookingHeightFor(Entity entity) {
		return entity instanceof LivingEntity ? entity.getEyeY() : (entity.getBoundingBox().minY + entity.getBoundingBox().maxY) / 2.0;
	}
}
