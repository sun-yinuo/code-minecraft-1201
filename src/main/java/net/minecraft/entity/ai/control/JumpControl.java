package net.minecraft.entity.ai.control;

import net.minecraft.entity.mob.MobEntity;

//跳的控制
public class JumpControl implements Control {
	//目标
	private final MobEntity entity;
	//激活？
	protected boolean active;

	/**
	 * JumpControl
	 * @param entity 目标
	 */
	public JumpControl(MobEntity entity) {
		this.entity = entity;
	}

	/**
	 * 激活
	 */
	public void setActive() {
		this.active = true;
	}


	public void tick() {
		//set
		this.entity.setJumping(this.active);
		//取消激活状态
		this.active = false;
	}
}
