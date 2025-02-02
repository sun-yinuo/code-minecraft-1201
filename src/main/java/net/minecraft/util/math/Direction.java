package net.minecraft.util.math;

import com.google.common.collect.Iterators;
import com.mojang.serialization.DataResult;
import java.util.Arrays;
import java.util.Collection;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.function.Predicate;
import java.util.stream.Stream;
import net.minecraft.entity.Entity;
import net.minecraft.util.StringIdentifiable;
import net.minecraft.util.Util;
import net.minecraft.util.dynamic.Codecs;
import net.minecraft.util.math.random.Random;
import org.jetbrains.annotations.Nullable;
import org.joml.Matrix4f;
import org.joml.Quaternionf;
import org.joml.Vector3f;
import org.joml.Vector4f;

/**
 * An enum representing 6 cardinal directions in Minecraft.
 * 
 * <p>In Minecraft, the X axis determines the east-west direction, the Y axis determines
 * the up-down direction, and the Z axis determines the south-north direction (note
 * that positive-Z direction is south, not north).
 */
public enum Direction implements StringIdentifiable {
	DOWN(0, 1, -1, "down", AxisDirection.NEGATIVE, Axis.Y, new Vec3i(0, -1, 0)),
	UP(1, 0, -1, "up", AxisDirection.POSITIVE, Axis.Y, new Vec3i(0, 1, 0)),
	NORTH(2, 3, 2, "north", AxisDirection.NEGATIVE, Axis.Z, new Vec3i(0, 0, -1)),
	SOUTH(3, 2, 0, "south", AxisDirection.POSITIVE, Axis.Z, new Vec3i(0, 0, 1)),
	WEST(4, 5, 1, "west", AxisDirection.NEGATIVE, Axis.X, new Vec3i(-1, 0, 0)),
	EAST(5, 4, 3, "east", AxisDirection.POSITIVE, Axis.X, new Vec3i(1, 0, 0));

	public static final Codec<Direction> CODEC = StringIdentifiable.createCodec(Direction::values);
	public static final com.mojang.serialization.Codec<Direction> VERTICAL_CODEC = Codecs.validate(CODEC, Direction::validateVertical);
	private final int id;
	private final int idOpposite;
	private final int idHorizontal;
	private final String name;
	private final Axis axis;
	private final AxisDirection direction;
	private final Vec3i vector;
	private static final Direction[] ALL = values();
	private static final Direction[] VALUES = (Direction[])Arrays.stream(ALL).sorted(Comparator.comparingInt(direction -> direction.id)).toArray(Direction[]::new);
	private static final Direction[] HORIZONTAL = (Direction[])Arrays.stream(ALL)
		.filter(direction -> direction.getAxis().isHorizontal())
		.sorted(Comparator.comparingInt(direction -> direction.idHorizontal))
		.toArray(Direction[]::new);

	private Direction(int id, int idOpposite, int idHorizontal, String name, AxisDirection direction, Axis axis, Vec3i vector) {
		this.id = id;
		this.idHorizontal = idHorizontal;
		this.idOpposite = idOpposite;
		this.name = name;
		this.axis = axis;
		this.direction = direction;
		this.vector = vector;
	}

	public static Direction[] getEntityFacingOrder(Entity entity) {
		float f = entity.getPitch(1.0F) * (float) (Math.PI / 180.0);
		float g = -entity.getYaw(1.0F) * (float) (Math.PI / 180.0);
		float h = MathHelper.sin(f);
		float i = MathHelper.cos(f);
		float j = MathHelper.sin(g);
		float k = MathHelper.cos(g);
		boolean bl = j > 0.0F;
		boolean bl2 = h < 0.0F;
		boolean bl3 = k > 0.0F;
		float l = bl ? j : -j;
		float m = bl2 ? -h : h;
		float n = bl3 ? k : -k;
		float o = l * i;
		float p = n * i;
		Direction direction = bl ? EAST : WEST;
		Direction direction2 = bl2 ? UP : DOWN;
		Direction direction3 = bl3 ? SOUTH : NORTH;
		if (l > n) {
			if (m > o) {
				return listClosest(direction2, direction, direction3);
			} else {
				return p > m ? listClosest(direction, direction3, direction2) : listClosest(direction, direction2, direction3);
			}
		} else if (m > p) {
			return listClosest(direction2, direction3, direction);
		} else {
			return o > m ? listClosest(direction3, direction, direction2) : listClosest(direction3, direction2, direction);
		}
	}

	/**
	 * Helper function that returns the 3 directions given, followed by the 3 opposite given in opposite order.
	 */
	private static Direction[] listClosest(Direction first, Direction second, Direction third) {
		return new Direction[]{first, second, third, third.getOpposite(), second.getOpposite(), first.getOpposite()};
	}

	public static Direction transform(Matrix4f matrix, Direction direction) {
		Vec3i vec3i = direction.getVector();
		Vector4f vector4f = matrix.transform(new Vector4f((float)vec3i.getX(), (float)vec3i.getY(), (float)vec3i.getZ(), 0.0F));
		return getFacing(vector4f.x(), vector4f.y(), vector4f.z());
	}

	/**
	 * {@return a shuffled collection of all directions}
	 */
	public static Collection<Direction> shuffle(Random random) {
		return Util.<Direction>copyShuffled(values(), random);
	}

	public static Stream<Direction> stream() {
		return Stream.of(ALL);
	}

	public Quaternionf getRotationQuaternion() {
		return switch (this) {
			case DOWN -> new Quaternionf().rotationX((float) Math.PI);
			case UP -> new Quaternionf();
			case NORTH -> new Quaternionf().rotationXYZ((float) (Math.PI / 2), 0.0F, (float) Math.PI);
			case SOUTH -> new Quaternionf().rotationX((float) (Math.PI / 2));
			case WEST -> new Quaternionf().rotationXYZ((float) (Math.PI / 2), 0.0F, (float) (Math.PI / 2));
			case EAST -> new Quaternionf().rotationXYZ((float) (Math.PI / 2), 0.0F, (float) (-Math.PI / 2));
		};
	}

	public int getId() {
		return this.id;
	}

	public int getHorizontal() {
		return this.idHorizontal;
	}

	public AxisDirection getDirection() {
		return this.direction;
	}

	public static Direction getLookDirectionForAxis(Entity entity, Axis axis) {
		return switch (axis) {
			case X -> EAST.pointsTo(entity.getYaw(1.0F)) ? EAST : WEST;
			case Z -> SOUTH.pointsTo(entity.getYaw(1.0F)) ? SOUTH : NORTH;
			case Y -> entity.getPitch(1.0F) < 0.0F ? UP : DOWN;
		};
	}

	public Direction getOpposite() {
		return byId(this.idOpposite);
	}

	public Direction rotateClockwise(Axis axis) {
		return switch (axis) {
			case X -> this != WEST && this != EAST ? this.rotateXClockwise() : this;
			case Z -> this != NORTH && this != SOUTH ? this.rotateZClockwise() : this;
			case Y -> this != UP && this != DOWN ? this.rotateYClockwise() : this;
		};
	}

	public Direction rotateCounterclockwise(Axis axis) {
		return switch (axis) {
			case X -> this != WEST && this != EAST ? this.rotateXCounterclockwise() : this;
			case Z -> this != NORTH && this != SOUTH ? this.rotateZCounterclockwise() : this;
			case Y -> this != UP && this != DOWN ? this.rotateYCounterclockwise() : this;
		};
	}

	public Direction rotateYClockwise() {
		return switch (this) {
			case NORTH -> EAST;
			case SOUTH -> WEST;
			case WEST -> NORTH;
			case EAST -> SOUTH;
			default -> throw new IllegalStateException("Unable to get Y-rotated facing of " + this);
		};
	}

	private Direction rotateXClockwise() {
		return switch (this) {
			case DOWN -> SOUTH;
			case UP -> NORTH;
			case NORTH -> DOWN;
			case SOUTH -> UP;
			default -> throw new IllegalStateException("Unable to get X-rotated facing of " + this);
		};
	}

	private Direction rotateXCounterclockwise() {
		return switch (this) {
			case DOWN -> NORTH;
			case UP -> SOUTH;
			case NORTH -> UP;
			case SOUTH -> DOWN;
			default -> throw new IllegalStateException("Unable to get X-rotated facing of " + this);
		};
	}

	private Direction rotateZClockwise() {
		return switch (this) {
			case DOWN -> WEST;
			case UP -> EAST;
			default -> throw new IllegalStateException("Unable to get Z-rotated facing of " + this);
			case WEST -> UP;
			case EAST -> DOWN;
		};
	}

	private Direction rotateZCounterclockwise() {
		return switch (this) {
			case DOWN -> EAST;
			case UP -> WEST;
			default -> throw new IllegalStateException("Unable to get Z-rotated facing of " + this);
			case WEST -> DOWN;
			case EAST -> UP;
		};
	}

	public Direction rotateYCounterclockwise() {
		return switch (this) {
			case NORTH -> WEST;
			case SOUTH -> EAST;
			case WEST -> SOUTH;
			case EAST -> NORTH;
			default -> throw new IllegalStateException("Unable to get CCW facing of " + this);
		};
	}

	public int getOffsetX() {
		return this.vector.getX();
	}

	public int getOffsetY() {
		return this.vector.getY();
	}

	public int getOffsetZ() {
		return this.vector.getZ();
	}

	public Vector3f getUnitVector() {
		return new Vector3f((float)this.getOffsetX(), (float)this.getOffsetY(), (float)this.getOffsetZ());
	}

	public String getName() {
		return this.name;
	}

	public Axis getAxis() {
		return this.axis;
	}

	/**
	 * {@return a direction with the given {@code name}, or {@code null} if there is
	 * no such direction}
	 */
	@Nullable
	public static Direction byName(@Nullable String name) {
		return (Direction)CODEC.byId(name);
	}

	public static Direction byId(int id) {
		return VALUES[MathHelper.abs(id % VALUES.length)];
	}

	public static Direction fromHorizontal(int value) {
		return HORIZONTAL[MathHelper.abs(value % HORIZONTAL.length)];
	}

	@Nullable
	public static Direction fromVector(int x, int y, int z) {
		if (x == 0) {
			if (y == 0) {
				if (z > 0) {
					return SOUTH;
				}

				if (z < 0) {
					return NORTH;
				}
			} else if (z == 0) {
				if (y > 0) {
					return UP;
				}

				return DOWN;
			}
		} else if (y == 0 && z == 0) {
			if (x > 0) {
				return EAST;
			}

			return WEST;
		}

		return null;
	}

	public static Direction fromRotation(double rotation) {
		return fromHorizontal(MathHelper.floor(rotation / 90.0 + 0.5) & 3);
	}

	public static Direction from(Axis axis, AxisDirection direction) {
		return switch (axis) {
			case X -> direction == AxisDirection.POSITIVE ? EAST : WEST;
			case Z -> direction == AxisDirection.POSITIVE ? SOUTH : NORTH;
			case Y -> direction == AxisDirection.POSITIVE ? UP : DOWN;
		};
	}

	public float asRotation() {
		return (float)((this.idHorizontal & 3) * 90);
	}

	public static Direction random(Random random) {
		return Util.getRandom(ALL, random);
	}

	public static Direction getFacing(double x, double y, double z) {
		return getFacing((float)x, (float)y, (float)z);
	}

	public static Direction getFacing(float x, float y, float z) {
		Direction direction = NORTH;
		float f = Float.MIN_VALUE;

		for (Direction direction2 : ALL) {
			float g = x * (float)direction2.vector.getX() + y * (float)direction2.vector.getY() + z * (float)direction2.vector.getZ();
			if (g > f) {
				f = g;
				direction = direction2;
			}
		}

		return direction;
	}

	public String toString() {
		return this.name;
	}

	@Override
	public String asString() {
		return this.name;
	}

	private static DataResult<Direction> validateVertical(Direction direction) {
		return direction.getAxis().isVertical() ? DataResult.success(direction) : DataResult.error(() -> "Expected a vertical direction");
	}

	public static Direction get(AxisDirection direction, Axis axis) {
		for (Direction direction2 : ALL) {
			if (direction2.getDirection() == direction && direction2.getAxis() == axis) {
				return direction2;
			}
		}

		throw new IllegalArgumentException("No such direction: " + direction + " " + axis);
	}

	public Vec3i getVector() {
		return this.vector;
	}

	/**
	 * {@return whether the given yaw points to the direction}
	 * 
	 * @implNote This returns whether the yaw can make an acute angle with the direction.
	 * 
	 * <p>This always returns {@code false} for vertical directions.
	 */
	public boolean pointsTo(float yaw) {
		float f = yaw * (float) (Math.PI / 180.0);
		float g = -MathHelper.sin(f);
		float h = MathHelper.cos(f);
		return (float)this.vector.getX() * g + (float)this.vector.getZ() * h > 0.0F;
	}

	public static enum Axis implements StringIdentifiable, Predicate<Direction> {
		X("x") {
			@Override
			public int choose(int x, int y, int z) {
				return x;
			}

			@Override
			public double choose(double x, double y, double z) {
				return x;
			}
		},
		Y("y") {
			@Override
			public int choose(int x, int y, int z) {
				return y;
			}

			@Override
			public double choose(double x, double y, double z) {
				return y;
			}
		},
		Z("z") {
			@Override
			public int choose(int x, int y, int z) {
				return z;
			}

			@Override
			public double choose(double x, double y, double z) {
				return z;
			}
		};

		public static final Axis[] VALUES = values();
		public static final Codec<Axis> CODEC = StringIdentifiable.createCodec(Axis::values);
		private final String name;

		Axis(String name) {
			this.name = name;
		}

		@Nullable
		public static Direction.Axis fromName(String name) {
			return (Axis)CODEC.byId(name);
		}

		public String getName() {
			return this.name;
		}

		public boolean isVertical() {
			return this == Y;
		}

		public boolean isHorizontal() {
			return this == X || this == Z;
		}

		public String toString() {
			return this.name;
		}

		public static Axis pickRandomAxis(Random random) {
			return Util.getRandom(VALUES, random);
		}

		public boolean test(@Nullable Direction direction) {
			return direction != null && direction.getAxis() == this;
		}

		public Type getType() {
			return switch (this) {
				case X, Z -> Type.HORIZONTAL;
				case Y -> Type.VERTICAL;
			};
		}

		@Override
		public String asString() {
			return this.name;
		}

		public abstract int choose(int x, int y, int z);

		public abstract double choose(double x, double y, double z);
	}

	public static enum AxisDirection {
		POSITIVE(1, "Towards positive"),
		NEGATIVE(-1, "Towards negative");

		private final int offset;
		private final String description;

		private AxisDirection(int offset, String description) {
			this.offset = offset;
			this.description = description;
		}

		public int offset() {
			return this.offset;
		}

		public String getDescription() {
			return this.description;
		}

		public String toString() {
			return this.description;
		}

		public AxisDirection getOpposite() {
			return this == POSITIVE ? NEGATIVE : POSITIVE;
		}
	}

	public static enum Type implements Iterable<Direction>, Predicate<Direction> {
		HORIZONTAL(new Direction[]{Direction.NORTH, Direction.EAST, Direction.SOUTH, Direction.WEST}, new Axis[]{Axis.X, Axis.Z}),
		VERTICAL(new Direction[]{Direction.UP, Direction.DOWN}, new Axis[]{Axis.Y});

		private final Direction[] facingArray;
		private final Axis[] axisArray;

		private Type(Direction[] facingArray, Axis[] axisArray) {
			this.facingArray = facingArray;
			this.axisArray = axisArray;
		}

		public Direction random(Random random) {
			return Util.getRandom(this.facingArray, random);
		}

		public Axis randomAxis(Random random) {
			return Util.getRandom(this.axisArray, random);
		}

		public boolean test(@Nullable Direction direction) {
			return direction != null && direction.getAxis().getType() == this;
		}

		public Iterator<Direction> iterator() {
			return Iterators.forArray(this.facingArray);
		}

		public Stream<Direction> stream() {
			return Arrays.stream(this.facingArray);
		}

		public List<Direction> getShuffled(Random random) {
			return Util.copyShuffled(this.facingArray, random);
		}
	}
}
