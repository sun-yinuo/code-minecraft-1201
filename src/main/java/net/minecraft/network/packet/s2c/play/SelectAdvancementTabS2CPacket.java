package net.minecraft.network.packet.s2c.play;

import net.minecraft.network.PacketByteBuf;
import net.minecraft.network.listener.ClientPlayPacketListener;
import net.minecraft.network.packet.Packet;
import net.minecraft.util.Identifier;
import org.jetbrains.annotations.Nullable;

public class SelectAdvancementTabS2CPacket implements Packet<ClientPlayPacketListener> {
	@Nullable
	private final Identifier tabId;

	public SelectAdvancementTabS2CPacket(@Nullable Identifier tabId) {
		this.tabId = tabId;
	}

	public void apply(ClientPlayPacketListener clientPlayPacketListener) {
		clientPlayPacketListener.onSelectAdvancementTab(this);
	}

	public SelectAdvancementTabS2CPacket(PacketByteBuf buf) {
		this.tabId = buf.readNullable(PacketByteBuf::readIdentifier);
	}

	@Override
	public void write(PacketByteBuf buf) {
		buf.writeNullable(this.tabId, PacketByteBuf::writeIdentifier);
	}

	@Nullable
	public Identifier getTabId() {
		return this.tabId;
	}
}
