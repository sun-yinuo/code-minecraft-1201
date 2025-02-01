package net.minecraft.nbt.scanner;

import net.minecraft.nbt.NbtType;

/**
 * A simple NBT scanner visits all elements shallowly, allowing
 * implementations to override it and perform more actions.
 */
public interface SimpleNbtScanner extends NbtScanner {
	/**
	 * The simple NBT scanner that performs no action.
	 */
	SimpleNbtScanner NOOP = new SimpleNbtScanner() {
	};

	@Override
	default Result visitEnd() {
		return Result.CONTINUE;
	}

	@Override
	default Result visitString(String value) {
		return Result.CONTINUE;
	}

	@Override
	default Result visitByte(byte value) {
		return Result.CONTINUE;
	}

	@Override
	default Result visitShort(short value) {
		return Result.CONTINUE;
	}

	@Override
	default Result visitInt(int value) {
		return Result.CONTINUE;
	}

	@Override
	default Result visitLong(long value) {
		return Result.CONTINUE;
	}

	@Override
	default Result visitFloat(float value) {
		return Result.CONTINUE;
	}

	@Override
	default Result visitDouble(double value) {
		return Result.CONTINUE;
	}

	@Override
	default Result visitByteArray(byte[] value) {
		return Result.CONTINUE;
	}

	@Override
	default Result visitIntArray(int[] value) {
		return Result.CONTINUE;
	}

	@Override
	default Result visitLongArray(long[] value) {
		return Result.CONTINUE;
	}

	@Override
	default Result visitListMeta(NbtType<?> entryType, int length) {
		return Result.CONTINUE;
	}

	@Override
	default NestedResult startListItem(NbtType<?> type, int index) {
		return NestedResult.SKIP;
	}

	@Override
	default NestedResult visitSubNbtType(NbtType<?> type) {
		return NestedResult.SKIP;
	}

	@Override
	default NestedResult startSubNbt(NbtType<?> type, String key) {
		return NestedResult.SKIP;
	}

	@Override
	default Result endNested() {
		return Result.CONTINUE;
	}

	@Override
	default Result start(NbtType<?> rootType) {
		return Result.CONTINUE;
	}
}
