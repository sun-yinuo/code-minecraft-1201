package net.minecraft.entity.attribute;

import com.mojang.logging.LogUtils;
import java.util.Objects;
import java.util.UUID;
import java.util.function.Supplier;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.random.Random;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;

public class EntityAttributeModifier {
	private static final Logger LOGGER = LogUtils.getLogger();
	private final double value;
	private final Operation operation;
	private final Supplier<String> nameGetter;
	private final UUID uuid;

	public EntityAttributeModifier(String name, double value, Operation operation) {
		this(MathHelper.randomUuid(Random.createLocal()), (Supplier<String>)(() -> name), value, operation);
	}

	public EntityAttributeModifier(UUID uuid, String name, double value, Operation operation) {
		this(uuid, (Supplier<String>)(() -> name), value, operation);
	}

	public EntityAttributeModifier(UUID uuid, Supplier<String> nameGetter, double value, Operation operation) {
		this.uuid = uuid;
		this.nameGetter = nameGetter;
		this.value = value;
		this.operation = operation;
	}

	public UUID getId() {
		return this.uuid;
	}

	public String getName() {
		return (String)this.nameGetter.get();
	}

	public Operation getOperation() {
		return this.operation;
	}

	public double getValue() {
		return this.value;
	}

	public boolean equals(Object o) {
		if (this == o) {
			return true;
		} else if (o != null && this.getClass() == o.getClass()) {
			EntityAttributeModifier entityAttributeModifier = (EntityAttributeModifier)o;
			return Objects.equals(this.uuid, entityAttributeModifier.uuid);
		} else {
			return false;
		}
	}

	public int hashCode() {
		return this.uuid.hashCode();
	}

	public String toString() {
		return "AttributeModifier{amount=" + this.value + ", operation=" + this.operation + ", name='" + (String)this.nameGetter.get() + "', id=" + this.uuid + "}";
	}

	public NbtCompound toNbt() {
		NbtCompound nbtCompound = new NbtCompound();
		nbtCompound.putString("Name", this.getName());
		nbtCompound.putDouble("Amount", this.value);
		nbtCompound.putInt("Operation", this.operation.getId());
		nbtCompound.putUuid("UUID", this.uuid);
		return nbtCompound;
	}

	@Nullable
	public static EntityAttributeModifier fromNbt(NbtCompound nbt) {
		try {
			UUID uUID = nbt.getUuid("UUID");
			Operation operation = Operation.fromId(nbt.getInt("Operation"));
			return new EntityAttributeModifier(uUID, nbt.getString("Name"), nbt.getDouble("Amount"), operation);
		} catch (Exception var3) {
			LOGGER.warn("Unable to create attribute: {}", var3.getMessage());
			return null;
		}
	}

	/**
	 * Represents an operation which can be applied to an attribute modifier.
	 */
	public static enum Operation {
		/**
		 * Adds to the base value of an attribute.
		 */
		ADDITION(0),
		/**
		 * Multiplies the base value of the attribute.
		 * 
		 * <p>Is applied after addition.
		 */
		MULTIPLY_BASE(1),
		/**
		 * Multiplies the total value of the attribute.
		 * 
		 * <p>The total value is equal to the sum of all additions and base multiplications applied by an attribute modifier.
		 */
		MULTIPLY_TOTAL(2);

		private static final Operation[] VALUES = new Operation[]{ADDITION, MULTIPLY_BASE, MULTIPLY_TOTAL};
		private final int id;

		private Operation(int id) {
			this.id = id;
		}

		public int getId() {
			return this.id;
		}

		public static Operation fromId(int id) {
			if (id >= 0 && id < VALUES.length) {
				return VALUES[id];
			} else {
				throw new IllegalArgumentException("No operation with value " + id);
			}
		}
	}
}
