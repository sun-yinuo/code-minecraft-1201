package net.minecraft.block;

import net.minecraft.state.property.DirectionProperty;
import net.minecraft.state.property.Properties;

public abstract class FacingBlock extends Block {
	public static final DirectionProperty FACING = Properties.FACING;

	protected FacingBlock(Settings settings) {
		super(settings);
	}
}
