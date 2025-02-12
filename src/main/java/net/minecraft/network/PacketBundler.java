package net.minecraft.network;

import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.DecoderException;
import io.netty.handler.codec.MessageToMessageDecoder;
import java.util.List;
import net.minecraft.network.packet.Packet;
import org.jetbrains.annotations.Nullable;

public class PacketBundler extends MessageToMessageDecoder<Packet<?>> {
	@Nullable
	private PacketBundleHandler.Bundler currentBundler;
	@Nullable
	private PacketBundleHandler bundleHandler;
	private final NetworkSide side;

	public PacketBundler(NetworkSide side) {
		this.side = side;
	}

	protected void decode(ChannelHandlerContext channelHandlerContext, Packet<?> packet, List<Object> list) throws Exception {
		PacketBundleHandler.BundlerGetter bundlerGetter = channelHandlerContext.channel().attr(PacketBundleHandler.KEY).get();
		if (bundlerGetter == null) {
			throw new DecoderException("Bundler not configured: " + packet);
		} else {
			PacketBundleHandler packetBundleHandler = bundlerGetter.getBundler(this.side);
			if (this.currentBundler != null) {
				if (this.bundleHandler != packetBundleHandler) {
					throw new DecoderException("Bundler handler changed during bundling");
				}

				Packet<?> packet2 = this.currentBundler.add(packet);
				if (packet2 != null) {
					this.bundleHandler = null;
					this.currentBundler = null;
					list.add(packet2);
				}
			} else {
				PacketBundleHandler.Bundler bundler = packetBundleHandler.createBundler(packet);
				if (bundler != null) {
					this.currentBundler = bundler;
					this.bundleHandler = packetBundleHandler;
				} else {
					list.add(packet);
				}
			}
		}
	}
}
