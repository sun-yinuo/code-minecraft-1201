package net.minecraft.block;

import com.mojang.authlib.GameProfile;
import net.minecraft.block.entity.SkullBlockEntity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;
import net.minecraft.nbt.NbtHelper;
import net.minecraft.util.Util;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;

public class PlayerSkullBlock extends SkullBlock {
	public PlayerSkullBlock(Settings settings) {
		super(Type.PLAYER, settings);
	}

	@Override
	public void onPlaced(World world, BlockPos pos, BlockState state, @Nullable LivingEntity placer, ItemStack itemStack) {
		super.onPlaced(world, pos, state, placer, itemStack);
		if (world.getBlockEntity(pos) instanceof SkullBlockEntity skullBlockEntity) {
			GameProfile gameProfile = null;
			if (itemStack.hasNbt()) {
				NbtCompound nbtCompound = itemStack.getNbt();
				if (nbtCompound.contains("SkullOwner", NbtElement.COMPOUND_TYPE)) {
					gameProfile = NbtHelper.toGameProfile(nbtCompound.getCompound("SkullOwner"));
				} else if (nbtCompound.contains("SkullOwner", NbtElement.STRING_TYPE) && !Util.isBlank(nbtCompound.getString("SkullOwner"))) {
					gameProfile = new GameProfile(null, nbtCompound.getString("SkullOwner"));
				}
			}

			skullBlockEntity.setOwner(gameProfile);
		}
	}
}
