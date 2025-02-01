package net.minecraft.entity.ai.control;

import net.minecraft.block.BlockState;
import net.minecraft.entity.ai.pathing.EntityNavigation;
import net.minecraft.entity.ai.pathing.PathNodeMaker;
import net.minecraft.entity.ai.pathing.PathNodeType;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.registry.tag.BlockTags;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.shape.VoxelShape;

public class MoveControl implements Control {
	public static final float field_30197 = 5.0E-4F;
	public static final float REACHED_DESTINATION_DISTANCE_SQUARED = 2.5000003E-7F;
	protected static final int field_30199 = 90;

	//目标移动的实体
	protected final MobEntity entity;
	//目标XYZ,双精度浮点
	protected double targetX;
	protected double targetY;
	protected double targetZ;
	//速度,双精度浮点
	protected double speed;
	//前进移动,单精度浮点
	protected float forwardMovement;
	//侧向运动,单精度浮点
	protected float sidewaysMovement;
	//初始化运动状态WAIT,等待发令
	protected State state = State.WAIT;

	/**
	 * 赋值移动对象
	 * @param entity 移动对象
	 */
	public MoveControl(MobEntity entity) {
		this.entity = entity;
	}

	/**
	 * 在移动?,尽限MOVE_TO
	 * @return boolean
	 */
	public boolean isMoving() {
		return this.state == State.MOVE_TO;
	}

	/**
	 * 获取速度
	 * @return speed
	 */
	public double getSpeed() {
		return this.speed;
	}

	/**
	 * 移动
	 * @param x 目标x
	 * @param y 目标y
	 * @param z 目标z
	 * @param speed 速度
	 */
	public void moveTo(double x, double y, double z, double speed) {
		//赋值
		this.targetX = x;
		this.targetY = y;
		this.targetZ = z;
		this.speed = speed;

		//不在JUMPING状态
		if (this.state != State.JUMPING) {
			//改变状态为MOVE_TO
			this.state = State.MOVE_TO;
		}
	}


	/**
	 * 平移
	 * @param forward 前进运动量
	 * @param sideways 侧向运动量
	 */
	public void strafeTo(float forward, float sideways) {
		//改变状态为STRAFE
		this.state = State.STRAFE;
		//赋值
		this.forwardMovement = forward;
		this.sidewaysMovement = sideways;
		this.speed = 0.25;
	}


	public void tick() {
		//如果运动状态为STRAFE
		if (this.state == State.STRAFE) {

			//获取"GENERIC_MOVEMENT_SPEED"生物的地面移动速度
			float f = (float)this.entity.getAttributeValue(EntityAttributes.GENERIC_MOVEMENT_SPEED);
			//与目标速度做乘法,
			float g = (float)this.speed * f;
			//获取目标 前进/侧向 量
			float h = this.forwardMovement;
			float i = this.sidewaysMovement;

			//向量(h,i)的模
			float j = MathHelper.sqrt(h * h + i * i);
			//如果小于1
			if (j < 1.0F) {
				//定义为1
				j = 1.0F;
			}

			//j=速度/j
			j = g / j;
			// h = h*j
			h *= j;
			//i = i*j
			i *= j;

			//获取yaw
			float k = MathHelper.sin(this.entity.getYaw() * (float) (Math.PI / 180.0));
			float l = MathHelper.cos(this.entity.getYaw() * (float) (Math.PI / 180.0));
			//计算移动分量
			float m = h * l - i * k;
			float n = i * l + h * k;
			//可以实现吗？
			if (!this.isPosWalkable(m, n)) {
				//不行
				//调整移动量为向前移动
				this.forwardMovement = 1.0F;
				this.sidewaysMovement = 0.0F;
			}

			//set
			this.entity.setMovementSpeed(g);
			this.entity.setForwardSpeed(this.forwardMovement);
			this.entity.setSidewaysSpeed(this.sidewaysMovement);
			//还原状态
			this.state = State.WAIT;

			//如果是MOVE_TO
		} else if (this.state == State.MOVE_TO) {
			//还原
			this.state = State.WAIT;
			//取差
			double d = this.targetX - this.entity.getX();
			double e = this.targetZ - this.entity.getZ();
			double o = this.targetY - this.entity.getY();
			//距离
			double p = d * d + o * o + e * e;
			//过小
			if (p < 2.5000003E-7F) {
				//不动
				this.entity.setForwardSpeed(0.0F);
				return;
			}

			//方位角
			float n = (float)(MathHelper.atan2(e, d) * 180.0F / (float)Math.PI) - 90.0F;
			//设置偏航角
			this.entity.setYaw(this.wrapDegrees(this.entity.getYaw(), n, 90.0F));
			//设置速度
			this.entity.setMovementSpeed((float)(this.speed * this.entity.getAttributeValue(EntityAttributes.GENERIC_MOVEMENT_SPEED)));

			//获取实体现在的位置
			BlockPos blockPos = this.entity.getBlockPos();
			//所在位置的方块状态
			BlockState blockState = this.entity.getWorld().getBlockState(blockPos);
			//获取碰撞箱
			VoxelShape voxelShape = blockState.getCollisionShape(this.entity.getWorld(), blockPos);
			//目标高度超过最大跳跃高度 and 间距小于实体的距离
			if (o > (double)this.entity.getStepHeight() && d * d + e * e < (double)Math.max(1.0F, this.entity.getWidth())
					//有碰撞箱
				|| !voxelShape.isEmpty()
					//实体高度<方块顶部
					&& this.entity.getY() < voxelShape.getMax(Direction.Axis.Y) + (double)blockPos.getY()
					//不是门或者栅栏
					&& !blockState.isIn(BlockTags.DOORS)
					&& !blockState.isIn(BlockTags.FENCES)) {

				//跳
				this.entity.getJumpControl().setActive();
				this.state = State.JUMPING;
			}

			//跳
		} else if (this.state == State.JUMPING) {
			//速度*属性速度
			this.entity.setMovementSpeed((float)(this.speed * this.entity.getAttributeValue(EntityAttributes.GENERIC_MOVEMENT_SPEED)));
			//OnGround? \\ 实体落地
			if (this.entity.isOnGround()) {
				//WAIT
				this.state = State.WAIT;
			}
		} else {
			//不动
			this.entity.setForwardSpeed(0.0F);
		}
	}


	/**
	 * 可行走的吗？
	 * @param x x
	 * @param z z
	 * @return 可行吗？
	 */
	private boolean isPosWalkable(float x, float z) {
		//获取Navigation
		EntityNavigation entityNavigation = this.entity.getNavigation();
		//不是null
		if (entityNavigation != null) {
			PathNodeMaker pathNodeMaker = entityNavigation.getNodeMaker();
			if (pathNodeMaker != null
				&& pathNodeMaker.getDefaultNodeType(
						this.entity.getWorld(), MathHelper.floor(this.entity.getX() + (double)x), this.entity.getBlockY(), MathHelper.floor(this.entity.getZ() + (double)z)
					)
					!= PathNodeType.WALKABLE) {
				return false;
			}
		}

		return true;
	}

	protected float wrapDegrees(float from, float to, float max) {
		float f = MathHelper.wrapDegrees(to - from);
		if (f > max) {
			f = max;
		}

		if (f < -max) {
			f = -max;
		}

		float g = from + f;
		if (g < 0.0F) {
			g += 360.0F;
		} else if (g > 360.0F) {
			g -= 360.0F;
		}

		return g;
	}

	/**
	 *  获取目标X
	 * @return 目标X
	 */
	public double getTargetX() {
		return this.targetX;
	}

	/**
	 *  获取目标Y
	 * @return 目标Y
	 */
	public double getTargetY() {
		return this.targetY;
	}

	/**
	 *  获取目标Z
	 * @return 目标Z
	 */
	public double getTargetZ() {
		return this.targetZ;
	}

	/**
	 * 移动状态枚举
	 */
	protected static enum State {
		WAIT,
		MOVE_TO,
		STRAFE,
		JUMPING;
	}
}
