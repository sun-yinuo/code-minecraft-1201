package net.minecraft.world.gen.structure;

import com.mojang.serialization.Codec;
import java.util.Optional;
import net.minecraft.structure.BuriedTreasureGenerator;
import net.minecraft.structure.StructurePiecesCollector;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.Heightmap;

public class BuriedTreasureStructure extends Structure {
	public static final Codec<BuriedTreasureStructure> CODEC = createCodec(BuriedTreasureStructure::new);

	public BuriedTreasureStructure(Config config) {
		super(config);
	}

	@Override
	public Optional<StructurePosition> getStructurePosition(Context context) {
		return getStructurePosition(context, Heightmap.Type.OCEAN_FLOOR_WG, collector -> addPieces(collector, context));
	}

	private static void addPieces(StructurePiecesCollector collector, Context context) {
		BlockPos blockPos = new BlockPos(context.chunkPos().getOffsetX(9), 90, context.chunkPos().getOffsetZ(9));
		collector.addPiece(new BuriedTreasureGenerator.Piece(blockPos));
	}

	@Override
	public StructureType<?> getType() {
		return StructureType.BURIED_TREASURE;
	}
}
