package net.minecraft.entity.ai.control;

import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.util.math.MathHelper;

//飞行控制
public class FlightMoveControl extends MoveControl {
	//Pitch上界
	private final int maxPitchChange;
	//无重力？
	private final boolean noGravity;

	/**
	 * 控制
	 * @param entity 实体
	 * @param maxPitchChange Pitch上界
	 * @param noGravity 无重力？
	 */
	public FlightMoveControl(MobEntity entity, int maxPitchChange, boolean noGravity) {
		super(entity);
		this.maxPitchChange = maxPitchChange;
		this.noGravity = noGravity;
	}

	@Override
	public void tick() {
		//状态MOVE_TO？
		if (this.state == State.MOVE_TO) {
			//还原
			this.state = State.WAIT;

			//无重力状态
			this.entity.setNoGravity(true);
			//计算差
			double d = this.targetX - this.entity.getX();
			double e = this.targetY - this.entity.getY();
			double f = this.targetZ - this.entity.getZ();
			//算距离
			double g = d * d + e * e + f * f;
			//到达了
			if (g < 2.5000003E-7F) {
				//不动
				this.entity.setUpwardSpeed(0.0F);
				this.entity.setForwardSpeed(0.0F);
				return;
			}//没到，继续

			//YAW调整
			float h = (float)(MathHelper.atan2(f, d) * 180.0F / (float)Math.PI) - 90.0F;
			//限制在-180-180
			this.entity.setYaw(this.wrapDegrees(this.entity.getYaw(), h, 90.0F));


			float i;
			//在地面？
			if (this.entity.isOnGround()) {
				//设置地面速度
				i = (float)(this.speed * this.entity.getAttributeValue(EntityAttributes.GENERIC_MOVEMENT_SPEED));
			} else {
				//设置飞行速度
				i = (float)(this.speed * this.entity.getAttributeValue(EntityAttributes.GENERIC_FLYING_SPEED));
			}

			//set
			this.entity.setMovementSpeed(i);

			//俯仰角
			double j = Math.sqrt(d * d + f * f);
			if (Math.abs(e) > 1.0E-5F || Math.abs(j) > 1.0E-5F) {
				float k = (float)(-(MathHelper.atan2(e, j) * 180.0F / (float)Math.PI));
				this.entity.setPitch(this.wrapDegrees(this.entity.getPitch(), k, (float)this.maxPitchChange));
				this.entity.setUpwardSpeed(e > 0.0 ? i : -i);
			}
		} else {
			//恢复重力
			if (!this.noGravity) {
				this.entity.setNoGravity(false);
			}

			//停止运动
			this.entity.setUpwardSpeed(0.0F);
			this.entity.setForwardSpeed(0.0F);
		}
	}
}
