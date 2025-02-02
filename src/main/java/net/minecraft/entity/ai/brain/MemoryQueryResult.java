package net.minecraft.entity.ai.brain;

import com.mojang.datafixers.kinds.App;
import com.mojang.datafixers.kinds.K1;
import java.util.Optional;

/**
 * The result of a {@link MemoryQuery}. This is passed as a lambda argument to
 * {@link net.minecraft.entity.ai.brain.task.TaskTriggerer#task}. Use
 * {@link net.minecraft.entity.ai.brain.task.TaskTriggerer.TaskContext#getValue} to
 * get the value.
 * 
 * <p>It is also possible to set or forget the stored memory value using methods in
 * this class.
 * 
 * @see net.minecraft.entity.ai.brain.task.TaskTriggerer.TaskContext#getValue
 * @see net.minecraft.entity.ai.brain.task.TaskTriggerer.TaskContext#getOptionalValue
 */

/**
 * {@link MemoryQuery} 的结果。这作为 lambda 参数传递给
 * {@link net.minecraft.entity.ai.brain.task.TaskTriggerer#task}。用
 * {@link net.minecraft.entity.ai.brain.task.TaskTriggerer.TaskContext#getValue} 设置为
 * 获取值。
 *
 * <p>也可以使用
 * 这个类。
 *
 * @see net.minecraft.entity.ai.brain.task.TaskTriggerer.TaskContext#getValue
 * @see net.minecraft.entity.ai.brain.task.TaskTriggerer.TaskContext#getOptionalValue
 */
//记忆封装
public final class MemoryQueryResult<F extends K1, Value> {
	//大脑
	private final Brain<?> brain;
	//记忆类型
	private final MemoryModuleType<Value> memory;
	//value
	private final App<F, Value> value;

	public MemoryQueryResult(Brain<?> brain, MemoryModuleType<Value> memory, App<F, Value> value) {
		this.brain = brain;
		this.memory = memory;
		this.value = value;
	}

	public App<F, Value> getValue() {
		return this.value;
	}

	/**
	 * 将指定的值存储到记忆模块中
	 * @param value value
	 */
	public void remember(Value value) {
		this.brain.remember(this.memory, Optional.of(value));
	}

	/**
	 * 将 Optional 类型的值存储到记忆模块中
	 * @param value value
	 */
	public void remember(Optional<Value> value) {
		this.brain.remember(this.memory, value);
	}

	/**
	 * 指定的值存储到记忆模块中，并设置记忆的过期时间
	 * @param value value
	 * @param expiry 过期时间
	 */
	public void remember(Value value, long expiry) {
		this.brain.remember(this.memory, value, expiry);
	}

	/**
	 * 忘记
	 */
	public void forget() {
		this.brain.forget(this.memory);
	}
}
