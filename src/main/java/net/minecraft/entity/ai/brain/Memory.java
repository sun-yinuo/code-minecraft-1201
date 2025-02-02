package net.minecraft.entity.ai.brain;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.Optional;
import net.minecraft.util.annotation.Debug;

//记忆
public class Memory<T> {
	//值
	private final T value;
	//有效期
	private long expiry;

	public Memory(T value, long expiry) {
		this.value = value;
		this.expiry = expiry;
	}

	/**
	 * 每个tick对有效期做减法
	 */
	public void tick() {
		if (this.isTimed()) {
			this.expiry--;
		}
	}

	/**
	 * Creates a memory without an expiry time.
	 * 创建不带有有效期的记忆
	 */
	public static <T> Memory<T> permanent(T value) {
		return new Memory<>(value, Long.MAX_VALUE);
	}

	/**
	 * Creates a memory that has an expiry time.
	 * 创建具有过期时间的记忆
	 */
	public static <T> Memory<T> timed(T value, long expiry) {
		return new Memory<>(value, expiry);
	}

	/**
	 * 获取有效期
	 * @return
	 */
	public long getExpiry() {
		return this.expiry;
	}

	public T getValue() {
		return this.value;
	}

	/**
	 * 判断是否过期
	 * @return 是否过期
	 */
	public boolean isExpired() {
		return this.expiry <= 0L;
	}

	public String toString() {
		return this.value + (this.isTimed() ? " (ttl: " + this.expiry + ")" : "");
	}

	/**
	 * 检查是否超过限制，目的可能是检查是否有效 ??
	 * @return ?
	 */
	@Debug //Debug
	public boolean isTimed() {
		return this.expiry != Long.MAX_VALUE;
	}

	public static <T> Codec<Memory<T>> createCodec(Codec<T> codec) {
		return RecordCodecBuilder.create(
			instance -> instance.group(
						codec.fieldOf("value").forGetter(memory -> memory.value),
						Codec.LONG.optionalFieldOf("ttl").forGetter(memory -> memory.isTimed() ? Optional.of(memory.expiry) : Optional.empty())
					)
					.apply(instance, (value, expiry) -> new Memory<>(value, (Long)expiry.orElse(Long.MAX_VALUE)))
		);
	}
}
