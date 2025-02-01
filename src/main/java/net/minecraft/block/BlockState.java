package net.minecraft.block;

import com.google.common.collect.ImmutableMap;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import net.fabricmc.fabric.api.block.v1.FabricBlockState;
import net.minecraft.registry.Registries;
import net.minecraft.state.property.Property;

public class BlockState extends AbstractBlock.AbstractBlockState implements FabricBlockState {
	public static final Codec<BlockState> CODEC = createCodec(Registries.BLOCK.getCodec(), Block::getDefaultState).stable();

	public BlockState(Block block, ImmutableMap<Property<?>, Comparable<?>> immutableMap, MapCodec<BlockState> mapCodec) {
		super(block, immutableMap, mapCodec);
	}

	@Override
	protected BlockState asBlockState() {
		return this;
	}
}
