package net.minecraft.world.event.listener;

import java.util.function.Consumer;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.ChunkSectionPos;
import net.minecraft.world.WorldView;
import net.minecraft.world.chunk.Chunk;
import net.minecraft.world.chunk.ChunkStatus;
import org.jetbrains.annotations.Nullable;

/**
 * A game event handler for an entity so that the listener stored can be
 * moved to the correct dispatcher or unregistered as the entity moves or
 * gets removed.
 */
public class EntityGameEventHandler<T extends GameEventListener> {
	private final T listener;
	@Nullable
	private ChunkSectionPos sectionPos;

	public EntityGameEventHandler(T listener) {
		this.listener = listener;
	}

	public void onEntitySetPosCallback(ServerWorld world) {
		this.onEntitySetPos(world);
	}

	public T getListener() {
		return this.listener;
	}

	public void onEntityRemoval(ServerWorld world) {
		updateDispatcher(world, this.sectionPos, dispatcher -> dispatcher.removeListener(this.listener));
	}

	public void onEntitySetPos(ServerWorld world) {
		this.listener.getPositionSource().getPos(world).map(ChunkSectionPos::from).ifPresent(sectionPos -> {
			if (this.sectionPos == null || !this.sectionPos.equals(sectionPos)) {
				updateDispatcher(world, this.sectionPos, dispatcher -> dispatcher.removeListener(this.listener));
				this.sectionPos = sectionPos;
				updateDispatcher(world, this.sectionPos, dispatcher -> dispatcher.addListener(this.listener));
			}
		});
	}

	private static void updateDispatcher(WorldView world, @Nullable ChunkSectionPos sectionPos, Consumer<GameEventDispatcher> dispatcherConsumer) {
		if (sectionPos != null) {
			Chunk chunk = world.getChunk(sectionPos.getSectionX(), sectionPos.getSectionZ(), ChunkStatus.FULL, false);
			if (chunk != null) {
				dispatcherConsumer.accept(chunk.getGameEventDispatcher(sectionPos.getSectionY()));
			}
		}
	}
}
