package net.minecraft.nbt.scanner;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.function.Consumer;
import net.minecraft.nbt.NbtByte;
import net.minecraft.nbt.NbtByteArray;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtDouble;
import net.minecraft.nbt.NbtElement;
import net.minecraft.nbt.NbtEnd;
import net.minecraft.nbt.NbtFloat;
import net.minecraft.nbt.NbtInt;
import net.minecraft.nbt.NbtIntArray;
import net.minecraft.nbt.NbtList;
import net.minecraft.nbt.NbtLong;
import net.minecraft.nbt.NbtLongArray;
import net.minecraft.nbt.NbtShort;
import net.minecraft.nbt.NbtString;
import net.minecraft.nbt.NbtType;
import org.jetbrains.annotations.Nullable;

/**
 * An NBT collector scans an NBT structure and builds an object
 * representation out of it.
 */
public class NbtCollector implements NbtScanner {
	private String currentKey = "";
	@Nullable
	private NbtElement root;
	private final Deque<Consumer<NbtElement>> stack = new ArrayDeque();

	@Nullable
	public NbtElement getRoot() {
		return this.root;
	}

	protected int getDepth() {
		return this.stack.size();
	}

	private void append(NbtElement nbt) {
		((Consumer)this.stack.getLast()).accept(nbt);
	}

	@Override
	public Result visitEnd() {
		this.append(NbtEnd.INSTANCE);
		return Result.CONTINUE;
	}

	@Override
	public Result visitString(String value) {
		this.append(NbtString.of(value));
		return Result.CONTINUE;
	}

	@Override
	public Result visitByte(byte value) {
		this.append(NbtByte.of(value));
		return Result.CONTINUE;
	}

	@Override
	public Result visitShort(short value) {
		this.append(NbtShort.of(value));
		return Result.CONTINUE;
	}

	@Override
	public Result visitInt(int value) {
		this.append(NbtInt.of(value));
		return Result.CONTINUE;
	}

	@Override
	public Result visitLong(long value) {
		this.append(NbtLong.of(value));
		return Result.CONTINUE;
	}

	@Override
	public Result visitFloat(float value) {
		this.append(NbtFloat.of(value));
		return Result.CONTINUE;
	}

	@Override
	public Result visitDouble(double value) {
		this.append(NbtDouble.of(value));
		return Result.CONTINUE;
	}

	@Override
	public Result visitByteArray(byte[] value) {
		this.append(new NbtByteArray(value));
		return Result.CONTINUE;
	}

	@Override
	public Result visitIntArray(int[] value) {
		this.append(new NbtIntArray(value));
		return Result.CONTINUE;
	}

	@Override
	public Result visitLongArray(long[] value) {
		this.append(new NbtLongArray(value));
		return Result.CONTINUE;
	}

	@Override
	public Result visitListMeta(NbtType<?> entryType, int length) {
		return Result.CONTINUE;
	}

	@Override
	public NestedResult startListItem(NbtType<?> type, int index) {
		this.pushStack(type);
		return NestedResult.ENTER;
	}

	@Override
	public NestedResult visitSubNbtType(NbtType<?> type) {
		return NestedResult.ENTER;
	}

	@Override
	public NestedResult startSubNbt(NbtType<?> type, String key) {
		this.currentKey = key;
		this.pushStack(type);
		return NestedResult.ENTER;
	}

	private void pushStack(NbtType<?> type) {
		if (type == NbtList.TYPE) {
			NbtList nbtList = new NbtList();
			this.append(nbtList);
			this.stack.addLast(nbtList::add);
		} else if (type == NbtCompound.TYPE) {
			NbtCompound nbtCompound = new NbtCompound();
			this.append(nbtCompound);
			this.stack.addLast((Consumer)nbt -> nbtCompound.put(this.currentKey, nbt));
		}
	}

	@Override
	public Result endNested() {
		this.stack.removeLast();
		return Result.CONTINUE;
	}

	@Override
	public Result start(NbtType<?> rootType) {
		if (rootType == NbtList.TYPE) {
			NbtList nbtList = new NbtList();
			this.root = nbtList;
			this.stack.addLast(nbtList::add);
		} else if (rootType == NbtCompound.TYPE) {
			NbtCompound nbtCompound = new NbtCompound();
			this.root = nbtCompound;
			this.stack.addLast((Consumer)nbt -> nbtCompound.put(this.currentKey, nbt));
		} else {
			this.stack.addLast((Consumer)nbt -> this.root = nbt);
		}

		return Result.CONTINUE;
	}
}
