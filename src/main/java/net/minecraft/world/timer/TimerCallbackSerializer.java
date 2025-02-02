package net.minecraft.world.timer;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.Maps;
import com.mojang.logging.LogUtils;
import java.util.Map;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.Identifier;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;

public class TimerCallbackSerializer<C> {
	private static final Logger LOGGER = LogUtils.getLogger();
	public static final TimerCallbackSerializer<MinecraftServer> INSTANCE = new TimerCallbackSerializer<MinecraftServer>()
		.registerSerializer(new FunctionTimerCallback.Serializer())
		.registerSerializer(new FunctionTagTimerCallback.Serializer());
	private final Map<Identifier, TimerCallback.Serializer<C, ?>> serializersByType = Maps.<Identifier, TimerCallback.Serializer<C, ?>>newHashMap();
	private final Map<Class<?>, TimerCallback.Serializer<C, ?>> serializersByClass = Maps.<Class<?>, TimerCallback.Serializer<C, ?>>newHashMap();

	public TimerCallbackSerializer<C> registerSerializer(TimerCallback.Serializer<C, ?> serializer) {
		this.serializersByType.put(serializer.getId(), serializer);
		this.serializersByClass.put(serializer.getCallbackClass(), serializer);
		return this;
	}

	private <T extends TimerCallback<C>> TimerCallback.Serializer<C, T> getSerializer(Class<?> clazz) {
		return (TimerCallback.Serializer<C, T>)this.serializersByClass.get(clazz);
	}

	public <T extends TimerCallback<C>> NbtCompound serialize(T callback) {
		TimerCallback.Serializer<C, T> serializer = this.getSerializer(callback.getClass());
		NbtCompound nbtCompound = new NbtCompound();
		serializer.serialize(nbtCompound, callback);
		nbtCompound.putString("Type", serializer.getId().toString());
		return nbtCompound;
	}

	@Nullable
	public TimerCallback<C> deserialize(NbtCompound nbt) {
		Identifier identifier = Identifier.tryParse(nbt.getString("Type"));
		TimerCallback.Serializer<C, ?> serializer = (TimerCallback.Serializer<C, ?>)this.serializersByType.get(identifier);
		if (serializer == null) {
			LOGGER.error("Failed to deserialize timer callback: {}", nbt);
			return null;
		} else {
			try {
				return serializer.deserialize(nbt);
			} catch (Exception var5) {
				LOGGER.error("Failed to deserialize timer callback: {}", nbt, var5);
				return null;
			}
		}
	}
}
