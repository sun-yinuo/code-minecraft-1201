package net.minecraft.item;

import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.network.packet.Packet;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;

/**
 * Represents an item which can contain extra data that is synced to the client.
 */
public class NetworkSyncedItem extends Item {
	public NetworkSyncedItem(Settings settings) {
		super(settings);
	}

	@Override
	public boolean isNetworkSynced() {
		return true;
	}

	/**
	 * Creates a packet that syncs additional item data to the client.
	 */
	@Nullable
	public Packet<?> createSyncPacket(ItemStack stack, World world, PlayerEntity player) {
		return null;
	}
}
