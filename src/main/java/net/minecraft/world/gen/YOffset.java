package net.minecraft.world.gen;

import com.mojang.datafixers.util.Either;
import com.mojang.serialization.Codec;
import java.util.function.Function;
import net.minecraft.util.dynamic.Codecs;
import net.minecraft.world.dimension.DimensionType;

public interface YOffset {
	Codec<YOffset> OFFSET_CODEC = Codecs.xor(Fixed.CODEC, Codecs.xor(AboveBottom.CODEC, BelowTop.CODEC))
		.xmap(YOffset::fromEither, YOffset::map);
	YOffset BOTTOM = aboveBottom(0);
	YOffset TOP = belowTop(0);

	static YOffset fixed(int offset) {
		return new Fixed(offset);
	}

	static YOffset aboveBottom(int offset) {
		return new AboveBottom(offset);
	}

	static YOffset belowTop(int offset) {
		return new BelowTop(offset);
	}

	static YOffset getBottom() {
		return BOTTOM;
	}

	static YOffset getTop() {
		return TOP;
	}

	private static YOffset fromEither(Either<Fixed, Either<AboveBottom, BelowTop>> either) {
		return either.map(Function.identity(), eitherx -> eitherx.map(Function.identity(), Function.identity()));
	}

	private static Either<Fixed, Either<AboveBottom, BelowTop>> map(YOffset yOffset) {
		return yOffset instanceof Fixed
			? Either.left((Fixed)yOffset)
			: Either.right(yOffset instanceof AboveBottom ? Either.left((AboveBottom)yOffset) : Either.right((BelowTop)yOffset));
	}

	int getY(HeightContext context);

	public static record AboveBottom(int offset) implements YOffset {
		public static final Codec<AboveBottom> CODEC = Codec.intRange(DimensionType.MIN_HEIGHT, DimensionType.MAX_COLUMN_HEIGHT)
			.fieldOf("above_bottom")
			.<AboveBottom>xmap(AboveBottom::new, AboveBottom::offset)
			.codec();

		@Override
		public int getY(HeightContext context) {
			return context.getMinY() + this.offset;
		}

		public String toString() {
			return this.offset + " above bottom";
		}
	}

	public static record BelowTop(int offset) implements YOffset {
		public static final Codec<BelowTop> CODEC = Codec.intRange(DimensionType.MIN_HEIGHT, DimensionType.MAX_COLUMN_HEIGHT)
			.fieldOf("below_top")
			.<BelowTop>xmap(BelowTop::new, BelowTop::offset)
			.codec();

		@Override
		public int getY(HeightContext context) {
			return context.getHeight() - 1 + context.getMinY() - this.offset;
		}

		public String toString() {
			return this.offset + " below top";
		}
	}

	public static record Fixed(int y) implements YOffset {
		public static final Codec<Fixed> CODEC = Codec.intRange(DimensionType.MIN_HEIGHT, DimensionType.MAX_COLUMN_HEIGHT)
			.fieldOf("absolute")
			.<Fixed>xmap(Fixed::new, Fixed::y)
			.codec();

		@Override
		public int getY(HeightContext context) {
			return this.y;
		}

		public String toString() {
			return this.y + " absolute";
		}
	}
}
