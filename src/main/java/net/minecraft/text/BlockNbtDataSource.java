package net.minecraft.text;

import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import java.util.stream.Stream;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.command.argument.BlockPosArgumentType;
import net.minecraft.command.argument.PosArgument;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import org.jetbrains.annotations.Nullable;

public record BlockNbtDataSource(String rawPos, @Nullable PosArgument pos) implements NbtDataSource {
	public BlockNbtDataSource(String rawPath) {
		this(rawPath, parsePos(rawPath));
	}

	@Nullable
	private static PosArgument parsePos(String string) {
		try {
			return BlockPosArgumentType.blockPos().parse(new StringReader(string));
		} catch (CommandSyntaxException var2) {
			return null;
		}
	}

	@Override
	public Stream<NbtCompound> get(ServerCommandSource source) {
		if (this.pos != null) {
			ServerWorld serverWorld = source.getWorld();
			BlockPos blockPos = this.pos.toAbsoluteBlockPos(source);
			if (serverWorld.canSetBlock(blockPos)) {
				BlockEntity blockEntity = serverWorld.getBlockEntity(blockPos);
				if (blockEntity != null) {
					return Stream.of(blockEntity.createNbtWithIdentifyingData());
				}
			}
		}

		return Stream.empty();
	}

	public String toString() {
		return "block=" + this.rawPos;
	}

	public boolean equals(Object o) {
		if (this == o) {
			return true;
		} else {
			if (o instanceof BlockNbtDataSource blockNbtDataSource && this.rawPos.equals(blockNbtDataSource.rawPos)) {
				return true;
			}

			return false;
		}
	}

	public int hashCode() {
		return this.rawPos.hashCode();
	}
}
