package net.minecraft.block;

import net.minecraft.block.enums.DoubleBlockHalf;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.mob.RavagerEntity;
import net.minecraft.item.ItemPlacementContext;
import net.minecraft.item.ItemStack;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.state.StateManager;
import net.minecraft.state.property.IntProperty;
import net.minecraft.state.property.Properties;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.random.Random;
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.world.BlockView;
import net.minecraft.world.GameRules;
import net.minecraft.world.World;
import net.minecraft.world.WorldAccess;
import net.minecraft.world.WorldView;
import org.jetbrains.annotations.Nullable;

public class PitcherCropBlock extends TallPlantBlock implements Fertilizable {
	public static final IntProperty AGE = Properties.AGE_4;
	public static final int field_43240 = 4;
	private static final int field_43241 = 3;
	private static final int field_43391 = 1;
	private static final VoxelShape GROWN_UPPER_OUTLINE_SHAPE = Block.createCuboidShape(3.0, 0.0, 3.0, 13.0, 15.0, 13.0);
	private static final VoxelShape GROWN_LOWER_OUTLINE_SHAPE = Block.createCuboidShape(3.0, -1.0, 3.0, 13.0, 16.0, 13.0);
	private static final VoxelShape AGE_0_SHAPE = Block.createCuboidShape(5.0, -1.0, 5.0, 11.0, 3.0, 11.0);
	private static final VoxelShape LOWER_COLLISION_SHAPE = Block.createCuboidShape(3.0, -1.0, 3.0, 13.0, 5.0, 13.0);
	private static final VoxelShape[] UPPER_OUTLINE_SHAPES = new VoxelShape[]{Block.createCuboidShape(3.0, 0.0, 3.0, 13.0, 11.0, 13.0), GROWN_UPPER_OUTLINE_SHAPE};
	private static final VoxelShape[] LOWER_OUTLINE_SHAPES = new VoxelShape[]{
		AGE_0_SHAPE, Block.createCuboidShape(3.0, -1.0, 3.0, 13.0, 14.0, 13.0), GROWN_LOWER_OUTLINE_SHAPE, GROWN_LOWER_OUTLINE_SHAPE, GROWN_LOWER_OUTLINE_SHAPE
	};

	public PitcherCropBlock(Settings settings) {
		super(settings);
	}

	private boolean isFullyGrown(BlockState state) {
		return (Integer)state.get(AGE) >= 4;
	}

	@Override
	public boolean hasRandomTicks(BlockState state) {
		return state.get(HALF) == DoubleBlockHalf.LOWER && !this.isFullyGrown(state);
	}

	@Nullable
	@Override
	public BlockState getPlacementState(ItemPlacementContext ctx) {
		return this.getDefaultState();
	}

	@Override
	public BlockState getStateForNeighborUpdate(
		BlockState state, Direction direction, BlockState neighborState, WorldAccess world, BlockPos pos, BlockPos neighborPos
	) {
		return !state.canPlaceAt(world, pos) ? Blocks.AIR.getDefaultState() : state;
	}

	@Override
	public VoxelShape getCollisionShape(BlockState state, BlockView world, BlockPos pos, ShapeContext context) {
		if ((Integer)state.get(AGE) == 0) {
			return AGE_0_SHAPE;
		} else {
			return state.get(HALF) == DoubleBlockHalf.LOWER ? LOWER_COLLISION_SHAPE : super.getCollisionShape(state, world, pos, context);
		}
	}

	@Override
	public boolean canPlaceAt(BlockState state, WorldView world, BlockPos pos) {
		return !isLowerHalf(state)
			? super.canPlaceAt(state, world, pos)
			: this.canPlantOnTop(world.getBlockState(pos.down()), world, pos.down())
				&& canPlaceAt(world, pos)
				&& ((Integer)state.get(AGE) < 3 || isUpperHalf(world.getBlockState(pos.up())));
	}

	@Override
	protected boolean canPlantOnTop(BlockState floor, BlockView world, BlockPos pos) {
		return floor.isOf(Blocks.FARMLAND);
	}

	@Override
	protected void appendProperties(StateManager.Builder<Block, BlockState> builder) {
		builder.add(AGE);
		super.appendProperties(builder);
	}

	@Override
	public VoxelShape getOutlineShape(BlockState state, BlockView world, BlockPos pos, ShapeContext context) {
		return state.get(HALF) == DoubleBlockHalf.UPPER
			? UPPER_OUTLINE_SHAPES[Math.min(Math.abs(4 - ((Integer)state.get(AGE) + 1)), UPPER_OUTLINE_SHAPES.length - 1)]
			: LOWER_OUTLINE_SHAPES[state.get(AGE)];
	}

	@Override
	public void onEntityCollision(BlockState state, World world, BlockPos pos, Entity entity) {
		if (entity instanceof RavagerEntity && world.getGameRules().getBoolean(GameRules.DO_MOB_GRIEFING)) {
			world.breakBlock(pos, true, entity);
		}

		super.onEntityCollision(state, world, pos, entity);
	}

	@Override
	public boolean canReplace(BlockState state, ItemPlacementContext context) {
		return false;
	}

	@Override
	public void onPlaced(World world, BlockPos pos, BlockState state, LivingEntity placer, ItemStack itemStack) {
	}

	@Override
	public void randomTick(BlockState state, ServerWorld world, BlockPos pos, Random random) {
		float f = CropBlock.getAvailableMoisture(this, world, pos);
		boolean bl = random.nextInt((int)(25.0F / f) + 1) == 0;
		if (bl) {
			this.tryGrow(world, state, pos, 1);
		}
	}

	private void tryGrow(ServerWorld world, BlockState state, BlockPos pos, int amount) {
		int i = Math.min((Integer)state.get(AGE) + amount, 4);
		if (this.canGrow(world, pos, state, i)) {
			world.setBlockState(pos, state.with(AGE, Integer.valueOf(i)), Block.NOTIFY_LISTENERS);
			if (i >= 3) {
				BlockPos blockPos = pos.up();
				world.setBlockState(
					blockPos, withWaterloggedState(world, pos, this.getDefaultState().with(AGE, Integer.valueOf(i)).with(HALF, DoubleBlockHalf.UPPER)), Block.NOTIFY_ALL
				);
			}
		}
	}

	private static boolean canGrowAt(WorldView world, BlockPos pos) {
		BlockState blockState = world.getBlockState(pos);
		return blockState.isAir() || blockState.isOf(Blocks.PITCHER_CROP);
	}

	private static boolean canPlaceAt(WorldView world, BlockPos pos) {
		return world.getBaseLightLevel(pos, 0) >= 8 || world.isSkyVisible(pos);
	}

	private static boolean isLowerHalf(BlockState state) {
		return state.isOf(Blocks.PITCHER_CROP) && state.get(HALF) == DoubleBlockHalf.LOWER;
	}

	private static boolean isUpperHalf(BlockState state) {
		return state.isOf(Blocks.PITCHER_CROP) && state.get(HALF) == DoubleBlockHalf.UPPER;
	}

	private boolean canGrow(WorldView world, BlockPos pos, BlockState state, int age) {
		return !this.isFullyGrown(state) && canPlaceAt(world, pos) && (age < 3 || canGrowAt(world, pos.up()));
	}

	@Nullable
	private PitcherCropBlock.LowerHalfContext getLowerHalfContext(WorldView world, BlockPos pos, BlockState state) {
		if (isLowerHalf(state)) {
			return new LowerHalfContext(pos, state);
		} else {
			BlockPos blockPos = pos.down();
			BlockState blockState = world.getBlockState(blockPos);
			return isLowerHalf(blockState) ? new LowerHalfContext(blockPos, blockState) : null;
		}
	}

	@Override
	public boolean isFertilizable(WorldView world, BlockPos pos, BlockState state, boolean isClient) {
		LowerHalfContext lowerHalfContext = this.getLowerHalfContext(world, pos, state);
		return lowerHalfContext == null ? false : this.canGrow(world, lowerHalfContext.pos, lowerHalfContext.state, (Integer)lowerHalfContext.state.get(AGE) + 1);
	}

	@Override
	public boolean canGrow(World world, Random random, BlockPos pos, BlockState state) {
		return true;
	}

	@Override
	public void grow(ServerWorld world, Random random, BlockPos pos, BlockState state) {
		LowerHalfContext lowerHalfContext = this.getLowerHalfContext(world, pos, state);
		if (lowerHalfContext != null) {
			this.tryGrow(world, lowerHalfContext.state, lowerHalfContext.pos, 1);
		}
	}

	static record LowerHalfContext(BlockPos pos, BlockState state) {
	}
}
