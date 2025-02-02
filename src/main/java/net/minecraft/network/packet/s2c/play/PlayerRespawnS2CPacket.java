package net.minecraft.network.packet.s2c.play;

import java.util.Optional;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.network.listener.ClientPlayPacketListener;
import net.minecraft.network.packet.Packet;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.util.math.GlobalPos;
import net.minecraft.world.GameMode;
import net.minecraft.world.World;
import net.minecraft.world.dimension.DimensionType;
import org.jetbrains.annotations.Nullable;

public class PlayerRespawnS2CPacket implements Packet<ClientPlayPacketListener> {
	public static final byte KEEP_ATTRIBUTES = 1;
	public static final byte KEEP_TRACKED_DATA = 2;
	public static final byte KEEP_ALL = 3;
	private final RegistryKey<DimensionType> dimensionType;
	private final RegistryKey<World> dimension;
	private final long sha256Seed;
	private final GameMode gameMode;
	@Nullable
	private final GameMode previousGameMode;
	private final boolean debugWorld;
	private final boolean flatWorld;
	private final byte flag;
	private final Optional<GlobalPos> lastDeathPos;
	private final int portalCooldown;

	public PlayerRespawnS2CPacket(
		RegistryKey<DimensionType> dimensionType,
		RegistryKey<World> dimension,
		long sha256Seed,
		GameMode gameMode,
		@Nullable GameMode previousGameMode,
		boolean debugWorld,
		boolean flatWorld,
		byte flag,
		Optional<GlobalPos> lastDeathPos,
		int portalCooldown
	) {
		this.dimensionType = dimensionType;
		this.dimension = dimension;
		this.sha256Seed = sha256Seed;
		this.gameMode = gameMode;
		this.previousGameMode = previousGameMode;
		this.debugWorld = debugWorld;
		this.flatWorld = flatWorld;
		this.flag = flag;
		this.lastDeathPos = lastDeathPos;
		this.portalCooldown = portalCooldown;
	}

	public PlayerRespawnS2CPacket(PacketByteBuf buf) {
		this.dimensionType = buf.readRegistryKey(RegistryKeys.DIMENSION_TYPE);
		this.dimension = buf.readRegistryKey(RegistryKeys.WORLD);
		this.sha256Seed = buf.readLong();
		this.gameMode = GameMode.byId(buf.readUnsignedByte());
		this.previousGameMode = GameMode.getOrNull(buf.readByte());
		this.debugWorld = buf.readBoolean();
		this.flatWorld = buf.readBoolean();
		this.flag = buf.readByte();
		this.lastDeathPos = buf.readOptional(PacketByteBuf::readGlobalPos);
		this.portalCooldown = buf.readVarInt();
	}

	@Override
	public void write(PacketByteBuf buf) {
		buf.writeRegistryKey(this.dimensionType);
		buf.writeRegistryKey(this.dimension);
		buf.writeLong(this.sha256Seed);
		buf.writeByte(this.gameMode.getId());
		buf.writeByte(GameMode.getId(this.previousGameMode));
		buf.writeBoolean(this.debugWorld);
		buf.writeBoolean(this.flatWorld);
		buf.writeByte(this.flag);
		buf.writeOptional(this.lastDeathPos, PacketByteBuf::writeGlobalPos);
		buf.writeVarInt(this.portalCooldown);
	}

	public void apply(ClientPlayPacketListener clientPlayPacketListener) {
		clientPlayPacketListener.onPlayerRespawn(this);
	}

	public RegistryKey<DimensionType> getDimensionType() {
		return this.dimensionType;
	}

	public RegistryKey<World> getDimension() {
		return this.dimension;
	}

	public long getSha256Seed() {
		return this.sha256Seed;
	}

	public GameMode getGameMode() {
		return this.gameMode;
	}

	@Nullable
	public GameMode getPreviousGameMode() {
		return this.previousGameMode;
	}

	public boolean isDebugWorld() {
		return this.debugWorld;
	}

	public boolean isFlatWorld() {
		return this.flatWorld;
	}

	public boolean hasFlag(byte flag) {
		return (this.flag & flag) != 0;
	}

	public Optional<GlobalPos> getLastDeathPos() {
		return this.lastDeathPos;
	}

	public int getPortalCooldown() {
		return this.portalCooldown;
	}
}
