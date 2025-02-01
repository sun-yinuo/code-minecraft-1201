package net.minecraft.entity.ai.control;

import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.util.math.MathHelper;

//水中运动控制
public class AquaticMoveControl extends MoveControl {
	private static final float field_40123 = 10.0F;
	private static final float field_40124 = 60.0F;

	//俯仰
	private final int pitchChange;
	//偏航
	private final int yawChange;
	//水中速度
	private final float speedInWater;
	//空气中速度
	private final float speedInAir;
	//具有浮力 boolean
	private final boolean buoyant;

	/**
	 * 运动控制
	 * @param entity entity
	 * @param pitchChange 俯仰
	 * @param yawChange 偏航
	 * @param speedInWater 水中速度
	 * @param speedInAir 空气中速度
	 * @param buoyant 具有浮力？
	 */
	public AquaticMoveControl(MobEntity entity, int pitchChange, int yawChange, float speedInWater, float speedInAir, boolean buoyant) {
		super(entity);
		this.pitchChange = pitchChange;
		this.yawChange = yawChange;
		this.speedInWater = speedInWater;
		this.speedInAir = speedInAir;
		this.buoyant = buoyant;
	}

	@Override
	public void tick() {
		//有浮力,在水中
		if (this.buoyant && this.entity.isTouchingWater()) {
			//增加垂直速度
			this.entity.setVelocity(this.entity.getVelocity().add(0.0, 0.005, 0.0));
		}

		//状态为MOVE_TO,导航不为Idle
		if (this.state == State.MOVE_TO && !this.entity.getNavigation().isIdle()) {
			//计算差值
			double d = this.targetX - this.entity.getX();
			double e = this.targetY - this.entity.getY();
			double f = this.targetZ - this.entity.getZ();
			//计算距离
			double g = d * d + e * e + f * f;
			//到达了，停止移动
			if (g < 2.5000003E-7F) {
				//不移动了
				this.entity.setForwardSpeed(0.0F);
			} else {
				// 调整偏航角
				//计算目标点在 XZ 平面上的方向角度
				float h = (float)(MathHelper.atan2(f, d) * 180.0F / (float)Math.PI) - 90.0F;
				this.entity.setYaw(this.wrapDegrees(this.entity.getYaw(), h, (float)this.yawChange));
				this.entity.bodyYaw = this.entity.getYaw();
				this.entity.headYaw = this.entity.getYaw();
				// 计算移动速度
				float i = (float)(this.speed * this.entity.getAttributeValue(EntityAttributes.GENERIC_MOVEMENT_SPEED));
				//接触水了
				if (this.entity.isTouchingWater()) {
					//设置速度
					this.entity.setMovementSpeed(i * this.speedInWater);
					//计算XZ平面上的距离
					double j = Math.sqrt(d * d + f * f);


					if (Math.abs(e) > 1.0E-5F || Math.abs(j) > 1.0E-5F) {
						//计算俯仰角
						float k = -((float)(MathHelper.atan2(e, j) * 180.0F / (float)Math.PI));
						//限制在-pitchChange～pitchChange
						k = MathHelper.clamp(MathHelper.wrapDegrees(k), (float)(-this.pitchChange), (float)this.pitchChange);
						this.entity.setPitch(this.wrapDegrees(this.entity.getPitch(), k, 5.0F));
					}
					//余弦值
					float k = MathHelper.cos(this.entity.getPitch() * (float) (Math.PI / 180.0));
					//正弦值
					float l = MathHelper.sin(this.entity.getPitch() * (float) (Math.PI / 180.0));
					//根据俯仰角计算前进速度
					this.entity.forwardSpeed = k * i;
					//根据俯仰角计算上升速度
					this.entity.upwardSpeed = -l * i;


					//在陆地上
				} else {
					//计算偏航角差值
					float m = Math.abs(MathHelper.wrapDegrees(this.entity.getYaw() - h));
					//计算速度调整因子
					float n = method_45335(m);
					//设置移动速度
					this.entity.setMovementSpeed(i * this.speedInAir * n);
				}
			}
		} else {
			//不动
			this.entity.setMovementSpeed(0.0F);
			this.entity.setSidewaysSpeed(0.0F);
			this.entity.setUpwardSpeed(0.0F);
			this.entity.setForwardSpeed(0.0F);
		}
	}

	private static float method_45335(float f) {
		return 1.0F - MathHelper.clamp((f - 10.0F) / 50.0F, 0.0F, 1.0F);
	}
}
