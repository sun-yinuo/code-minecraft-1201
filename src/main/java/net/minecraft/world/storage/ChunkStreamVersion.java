package net.minecraft.world.storage;

import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import java.io.BufferedOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.zip.DeflaterOutputStream;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;
import java.util.zip.InflaterInputStream;
import net.minecraft.util.FixedBufferInputStream;
import org.jetbrains.annotations.Nullable;

public class ChunkStreamVersion {
	private static final Int2ObjectMap<ChunkStreamVersion> VERSIONS = new Int2ObjectOpenHashMap<>();
	public static final ChunkStreamVersion GZIP = add(
		new ChunkStreamVersion(1, stream -> new FixedBufferInputStream(new GZIPInputStream(stream)), stream -> new BufferedOutputStream(new GZIPOutputStream(stream)))
	);
	public static final ChunkStreamVersion DEFLATE = add(
		new ChunkStreamVersion(
			2, stream -> new FixedBufferInputStream(new InflaterInputStream(stream)), stream -> new BufferedOutputStream(new DeflaterOutputStream(stream))
		)
	);
	public static final ChunkStreamVersion UNCOMPRESSED = add(new ChunkStreamVersion(3, stream -> stream, stream -> stream));
	private final int id;
	private final Wrapper<InputStream> inputStreamWrapper;
	private final Wrapper<OutputStream> outputStreamWrapper;

	private ChunkStreamVersion(int id, Wrapper<InputStream> inputStreamWrapper, Wrapper<OutputStream> outputStreamWrapper) {
		this.id = id;
		this.inputStreamWrapper = inputStreamWrapper;
		this.outputStreamWrapper = outputStreamWrapper;
	}

	private static ChunkStreamVersion add(ChunkStreamVersion version) {
		VERSIONS.put(version.id, version);
		return version;
	}

	@Nullable
	public static ChunkStreamVersion get(int id) {
		return VERSIONS.get(id);
	}

	public static boolean exists(int id) {
		return VERSIONS.containsKey(id);
	}

	public int getId() {
		return this.id;
	}

	public OutputStream wrap(OutputStream outputStream) throws IOException {
		return this.outputStreamWrapper.wrap(outputStream);
	}

	public InputStream wrap(InputStream inputStream) throws IOException {
		return this.inputStreamWrapper.wrap(inputStream);
	}

	@FunctionalInterface
	interface Wrapper<O> {
		O wrap(O object) throws IOException;
	}
}
