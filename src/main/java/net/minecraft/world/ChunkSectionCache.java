package net.minecraft.world;

import it.unimi.dsi.fastutil.longs.Long2ObjectFunction;
import it.unimi.dsi.fastutil.longs.Long2ObjectMap;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkSectionPos;
import net.minecraft.world.chunk.Chunk;
import net.minecraft.world.chunk.ChunkSection;
import org.jetbrains.annotations.Nullable;

public class ChunkSectionCache implements AutoCloseable {
	private final WorldAccess world;
	private final Long2ObjectMap<ChunkSection> cache = new Long2ObjectOpenHashMap<>();
	@Nullable
	private ChunkSection cachedSection;
	private long sectionPos;

	public ChunkSectionCache(WorldAccess world) {
		this.world = world;
	}

	@Nullable
	public ChunkSection getSection(BlockPos pos) {
		int i = this.world.getSectionIndex(pos.getY());
		if (i >= 0 && i < this.world.countVerticalSections()) {
			long l = ChunkSectionPos.toLong(pos);
			if (this.cachedSection == null || this.sectionPos != l) {
				this.cachedSection = this.cache.computeIfAbsent(l, (Long2ObjectFunction<? extends ChunkSection>)(lx -> {
					Chunk chunk = this.world.getChunk(ChunkSectionPos.getSectionCoord(pos.getX()), ChunkSectionPos.getSectionCoord(pos.getZ()));
					ChunkSection chunkSection = chunk.getSection(i);
					chunkSection.lock();
					return chunkSection;
				}));
				this.sectionPos = l;
			}

			return this.cachedSection;
		} else {
			return null;
		}
	}

	public BlockState getBlockState(BlockPos pos) {
		ChunkSection chunkSection = this.getSection(pos);
		if (chunkSection == null) {
			return Blocks.AIR.getDefaultState();
		} else {
			int i = ChunkSectionPos.getLocalCoord(pos.getX());
			int j = ChunkSectionPos.getLocalCoord(pos.getY());
			int k = ChunkSectionPos.getLocalCoord(pos.getZ());
			return chunkSection.getBlockState(i, j, k);
		}
	}

	public void close() {
		for (ChunkSection chunkSection : this.cache.values()) {
			chunkSection.unlock();
		}
	}
}
