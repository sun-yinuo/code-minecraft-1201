package net.minecraft.world.chunk.light;

import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.util.math.ChunkSectionPos;
import net.minecraft.world.chunk.ChunkNibbleArray;
import org.jetbrains.annotations.Nullable;

public interface ChunkLightingView extends LightingView {
	@Nullable
	ChunkNibbleArray getLightSection(ChunkSectionPos pos);

	int getLightLevel(BlockPos pos);

	public static enum Empty implements ChunkLightingView {
		INSTANCE;

		@Nullable
		@Override
		public ChunkNibbleArray getLightSection(ChunkSectionPos pos) {
			return null;
		}

		@Override
		public int getLightLevel(BlockPos pos) {
			return 0;
		}

		@Override
		public void checkBlock(BlockPos pos) {
		}

		@Override
		public boolean hasUpdates() {
			return false;
		}

		@Override
		public int doLightUpdates() {
			return 0;
		}

		@Override
		public void setSectionStatus(ChunkSectionPos pos, boolean notReady) {
		}

		@Override
		public void setColumnEnabled(ChunkPos pos, boolean retainData) {
		}

		@Override
		public void propagateLight(ChunkPos chunkPos) {
		}
	}
}
