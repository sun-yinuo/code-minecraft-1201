package net.minecraft.block.entity;

import com.google.common.collect.Iterables;
import com.mojang.authlib.GameProfile;
import com.mojang.authlib.minecraft.MinecraftSessionService;
import com.mojang.authlib.properties.Property;
import java.util.concurrent.Executor;
import java.util.function.Consumer;
import net.minecraft.block.BlockState;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;
import net.minecraft.nbt.NbtHelper;
import net.minecraft.network.packet.s2c.play.BlockEntityUpdateS2CPacket;
import net.minecraft.util.ApiServices;
import net.minecraft.util.Identifier;
import net.minecraft.util.StringHelper;
import net.minecraft.util.UserCache;
import net.minecraft.util.Util;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;

public class SkullBlockEntity extends BlockEntity {
	public static final String SKULL_OWNER_KEY = "SkullOwner";
	public static final String NOTE_BLOCK_SOUND_KEY = "note_block_sound";
	@Nullable
	private static UserCache userCache;
	@Nullable
	private static MinecraftSessionService sessionService;
	@Nullable
	private static Executor executor;
	@Nullable
	private GameProfile owner;
	@Nullable
	private Identifier noteBlockSound;
	private int poweredTicks;
	private boolean powered;

	public SkullBlockEntity(BlockPos pos, BlockState state) {
		super(BlockEntityType.SKULL, pos, state);
	}

	public static void setServices(ApiServices apiServices, Executor executor) {
		userCache = apiServices.userCache();
		sessionService = apiServices.sessionService();
		SkullBlockEntity.executor = executor;
	}

	public static void clearServices() {
		userCache = null;
		sessionService = null;
		executor = null;
	}

	@Override
	protected void writeNbt(NbtCompound nbt) {
		super.writeNbt(nbt);
		if (this.owner != null) {
			NbtCompound nbtCompound = new NbtCompound();
			NbtHelper.writeGameProfile(nbtCompound, this.owner);
			nbt.put("SkullOwner", nbtCompound);
		}

		if (this.noteBlockSound != null) {
			nbt.putString("note_block_sound", this.noteBlockSound.toString());
		}
	}

	@Override
	public void readNbt(NbtCompound nbt) {
		super.readNbt(nbt);
		if (nbt.contains("SkullOwner", NbtElement.COMPOUND_TYPE)) {
			this.setOwner(NbtHelper.toGameProfile(nbt.getCompound("SkullOwner")));
		} else if (nbt.contains("ExtraType", NbtElement.STRING_TYPE)) {
			String string = nbt.getString("ExtraType");
			if (!StringHelper.isEmpty(string)) {
				this.setOwner(new GameProfile(null, string));
			}
		}

		if (nbt.contains("note_block_sound", NbtElement.STRING_TYPE)) {
			this.noteBlockSound = Identifier.tryParse(nbt.getString("note_block_sound"));
		}
	}

	public static void tick(World world, BlockPos pos, BlockState state, SkullBlockEntity blockEntity) {
		if (world.isReceivingRedstonePower(pos)) {
			blockEntity.powered = true;
			blockEntity.poweredTicks++;
		} else {
			blockEntity.powered = false;
		}
	}

	public float getPoweredTicks(float tickDelta) {
		return this.powered ? (float)this.poweredTicks + tickDelta : (float)this.poweredTicks;
	}

	@Nullable
	public GameProfile getOwner() {
		return this.owner;
	}

	@Nullable
	public Identifier getNoteBlockSound() {
		return this.noteBlockSound;
	}

	public BlockEntityUpdateS2CPacket toUpdatePacket() {
		return BlockEntityUpdateS2CPacket.create(this);
	}

	@Override
	public NbtCompound toInitialChunkDataNbt() {
		return this.createNbt();
	}

	public void setOwner(@Nullable GameProfile owner) {
		synchronized (this) {
			this.owner = owner;
		}

		this.loadOwnerProperties();
	}

	private void loadOwnerProperties() {
		loadProperties(this.owner, owner -> {
			this.owner = owner;
			this.markDirty();
		});
	}

	public static void loadProperties(@Nullable GameProfile owner, Consumer<GameProfile> callback) {
		if (owner != null
			&& !StringHelper.isEmpty(owner.getName())
			&& (!owner.isComplete() || !owner.getProperties().containsKey("textures"))
			&& userCache != null
			&& sessionService != null) {
			userCache.findByNameAsync(owner.getName(), profile -> Util.getMainWorkerExecutor().execute(() -> Util.ifPresentOrElse(profile, profilex -> {
						Property property = Iterables.getFirst(profilex.getProperties().get("textures"), null);
						if (property == null) {
							MinecraftSessionService minecraftSessionService = sessionService;
							if (minecraftSessionService == null) {
								return;
							}

							profilex = minecraftSessionService.fillProfileProperties(profilex, true);
						}

						GameProfile gameProfilexx = profilex;
						Executor executor = SkullBlockEntity.executor;
						if (executor != null) {
							executor.execute(() -> {
								UserCache userCache = SkullBlockEntity.userCache;
								if (userCache != null) {
									userCache.add(gameProfilexx);
									callback.accept(gameProfilexx);
								}
							});
						}
					}, () -> {
						Executor executor = SkullBlockEntity.executor;
						if (executor != null) {
							executor.execute(() -> callback.accept(owner));
						}
					})));
		} else {
			callback.accept(owner);
		}
	}
}
