package net.minecraft.block;

import java.util.function.BiPredicate;
import java.util.function.Function;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.block.entity.BlockEntityType;
import net.minecraft.state.property.DirectionProperty;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.world.WorldAccess;

public class DoubleBlockProperties {
	public static <S extends BlockEntity> PropertySource<S> toPropertySource(
		BlockEntityType<S> blockEntityType,
		Function<BlockState, Type> typeMapper,
		Function<BlockState, Direction> function,
		DirectionProperty directionProperty,
		BlockState state,
		WorldAccess world,
		BlockPos pos,
		BiPredicate<WorldAccess, BlockPos> fallbackTester
	) {
		S blockEntity = blockEntityType.get(world, pos);
		if (blockEntity == null) {
			return PropertyRetriever::getFallback;
		} else if (fallbackTester.test(world, pos)) {
			return PropertyRetriever::getFallback;
		} else {
			Type type = (Type)typeMapper.apply(state);
			boolean bl = type == Type.SINGLE;
			boolean bl2 = type == Type.FIRST;
			if (bl) {
				return new PropertySource.Single<>(blockEntity);
			} else {
				BlockPos blockPos = pos.offset((Direction)function.apply(state));
				BlockState blockState = world.getBlockState(blockPos);
				if (blockState.isOf(state.getBlock())) {
					Type type2 = (Type)typeMapper.apply(blockState);
					if (type2 != Type.SINGLE && type != type2 && blockState.get(directionProperty) == state.get(directionProperty)) {
						if (fallbackTester.test(world, blockPos)) {
							return PropertyRetriever::getFallback;
						}

						S blockEntity2 = blockEntityType.get(world, blockPos);
						if (blockEntity2 != null) {
							S blockEntity3 = bl2 ? blockEntity : blockEntity2;
							S blockEntity4 = bl2 ? blockEntity2 : blockEntity;
							return new PropertySource.Pair<>(blockEntity3, blockEntity4);
						}
					}
				}

				return new PropertySource.Single<>(blockEntity);
			}
		}
	}

	public interface PropertyRetriever<S, T> {
		T getFromBoth(S first, S second);

		T getFrom(S single);

		T getFallback();
	}

	public interface PropertySource<S> {
		<T> T apply(PropertyRetriever<? super S, T> retriever);

		public static final class Pair<S> implements PropertySource<S> {
			private final S first;
			private final S second;

			public Pair(S first, S second) {
				this.first = first;
				this.second = second;
			}

			@Override
			public <T> T apply(PropertyRetriever<? super S, T> propertyRetriever) {
				return propertyRetriever.getFromBoth(this.first, this.second);
			}
		}

		public static final class Single<S> implements PropertySource<S> {
			private final S single;

			public Single(S single) {
				this.single = single;
			}

			@Override
			public <T> T apply(PropertyRetriever<? super S, T> propertyRetriever) {
				return propertyRetriever.getFrom(this.single);
			}
		}
	}

	public static enum Type {
		SINGLE,
		FIRST,
		SECOND;
	}
}
