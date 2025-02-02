package net.minecraft.entity.decoration;

import com.mojang.datafixers.util.Pair;
import com.mojang.logging.LogUtils;
import it.unimi.dsi.fastutil.ints.IntSet;
import java.util.List;
import java.util.Optional;
import java.util.function.IntFunction;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.piston.PistonBehavior;
import net.minecraft.client.render.model.json.ModelTransformationMode;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.data.DataTracker;
import net.minecraft.entity.data.TrackedData;
import net.minecraft.entity.data.TrackedDataHandlerRegistry;
import net.minecraft.inventory.StackReference;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;
import net.minecraft.nbt.NbtHelper;
import net.minecraft.nbt.NbtOps;
import net.minecraft.network.listener.ClientPlayPacketListener;
import net.minecraft.network.packet.Packet;
import net.minecraft.network.packet.s2c.play.EntitySpawnS2CPacket;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.text.OrderedText;
import net.minecraft.text.Text;
import net.minecraft.text.Texts;
import net.minecraft.util.StringIdentifiable;
import net.minecraft.util.Util;
import net.minecraft.util.function.ValueLists;
import net.minecraft.util.math.AffineTransformation;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.ColorHelper;
import net.minecraft.util.math.MathHelper;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;
import org.joml.Quaternionf;
import org.joml.Vector3f;
import org.slf4j.Logger;

public abstract class DisplayEntity extends Entity {
	static final Logger field_42397 = LogUtils.getLogger();
	public static final int field_42384 = -1;
	private static final TrackedData<Integer> START_INTERPOLATION = DataTracker.registerData(DisplayEntity.class, TrackedDataHandlerRegistry.INTEGER);
	private static final TrackedData<Integer> INTERPOLATION_DURATION = DataTracker.registerData(DisplayEntity.class, TrackedDataHandlerRegistry.INTEGER);
	private static final TrackedData<Vector3f> TRANSLATION = DataTracker.registerData(DisplayEntity.class, TrackedDataHandlerRegistry.VECTOR3F);
	private static final TrackedData<Vector3f> SCALE = DataTracker.registerData(DisplayEntity.class, TrackedDataHandlerRegistry.VECTOR3F);
	private static final TrackedData<Quaternionf> LEFT_ROTATION = DataTracker.registerData(DisplayEntity.class, TrackedDataHandlerRegistry.QUATERNIONF);
	private static final TrackedData<Quaternionf> RIGHT_ROTATION = DataTracker.registerData(DisplayEntity.class, TrackedDataHandlerRegistry.QUATERNIONF);
	private static final TrackedData<Byte> BILLBOARD = DataTracker.registerData(DisplayEntity.class, TrackedDataHandlerRegistry.BYTE);
	private static final TrackedData<Integer> BRIGHTNESS = DataTracker.registerData(DisplayEntity.class, TrackedDataHandlerRegistry.INTEGER);
	private static final TrackedData<Float> VIEW_RANGE = DataTracker.registerData(DisplayEntity.class, TrackedDataHandlerRegistry.FLOAT);
	private static final TrackedData<Float> SHADOW_RADIUS = DataTracker.registerData(DisplayEntity.class, TrackedDataHandlerRegistry.FLOAT);
	private static final TrackedData<Float> SHADOW_STRENGTH = DataTracker.registerData(DisplayEntity.class, TrackedDataHandlerRegistry.FLOAT);
	private static final TrackedData<Float> WIDTH = DataTracker.registerData(DisplayEntity.class, TrackedDataHandlerRegistry.FLOAT);
	private static final TrackedData<Float> HEIGHT = DataTracker.registerData(DisplayEntity.class, TrackedDataHandlerRegistry.FLOAT);
	private static final TrackedData<Integer> GLOW_COLOR_OVERRIDE = DataTracker.registerData(DisplayEntity.class, TrackedDataHandlerRegistry.INTEGER);
	private static final IntSet RENDERING_DATA_IDS = IntSet.of(
		TRANSLATION.getId(),
		SCALE.getId(),
		LEFT_ROTATION.getId(),
		RIGHT_ROTATION.getId(),
		BILLBOARD.getId(),
		BRIGHTNESS.getId(),
		SHADOW_RADIUS.getId(),
		SHADOW_STRENGTH.getId()
	);
	private static final float field_42376 = 0.0F;
	private static final float field_42377 = 1.0F;
	private static final int field_42378 = -1;
	public static final String INTERPOLATION_DURATION_NBT_KEY = "interpolation_duration";
	public static final String START_INTERPOLATION_KEY = "start_interpolation";
	public static final String TRANSFORMATION_NBT_KEY = "transformation";
	public static final String BILLBOARD_NBT_KEY = "billboard";
	public static final String BRIGHTNESS_NBT_KEY = "brightness";
	public static final String VIEW_RANGE_NBT_KEY = "view_range";
	public static final String SHADOW_RADIUS_NBT_KEY = "shadow_radius";
	public static final String SHADOW_STRENGTH_NBT_KEY = "shadow_strength";
	public static final String WIDTH_NBT_KEY = "width";
	public static final String HEIGHT_NBT_KEY = "height";
	public static final String GLOW_COLOR_OVERRIDE_NBT_KEY = "glow_color_override";
	private final Quaternionf fixedRotation = new Quaternionf();
	private long interpolationStart = -2147483648L;
	private int interpolationDuration;
	private float lerpProgress;
	private Box visibilityBoundingBox;
	protected boolean renderingDataSet;
	private boolean startInterpolationSet;
	private boolean interpolationDurationSet;
	@Nullable
	private DisplayEntity.RenderState renderState;

	public DisplayEntity(EntityType<?> entityType, World world) {
		super(entityType, world);
		this.noClip = true;
		this.ignoreCameraFrustum = true;
		this.visibilityBoundingBox = this.getBoundingBox();
	}

	@Override
	public void onTrackedDataSet(TrackedData<?> data) {
		super.onTrackedDataSet(data);
		if (HEIGHT.equals(data) || WIDTH.equals(data)) {
			this.updateVisibilityBoundingBox();
		}

		if (START_INTERPOLATION.equals(data)) {
			this.startInterpolationSet = true;
		}

		if (INTERPOLATION_DURATION.equals(data)) {
			this.interpolationDurationSet = true;
		}

		if (RENDERING_DATA_IDS.contains(data.getId())) {
			this.renderingDataSet = true;
		}
	}

	private static AffineTransformation getTransformation(DataTracker dataTracker) {
		Vector3f vector3f = dataTracker.get(TRANSLATION);
		Quaternionf quaternionf = dataTracker.get(LEFT_ROTATION);
		Vector3f vector3f2 = dataTracker.get(SCALE);
		Quaternionf quaternionf2 = dataTracker.get(RIGHT_ROTATION);
		return new AffineTransformation(vector3f, quaternionf, vector3f2, quaternionf2);
	}

	@Override
	public void tick() {
		Entity entity = this.getVehicle();
		if (entity != null && entity.isRemoved()) {
			this.stopRiding();
		}

		if (this.getWorld().isClient) {
			if (this.startInterpolationSet) {
				this.startInterpolationSet = false;
				int i = this.getStartInterpolation();
				this.interpolationStart = (long)(this.age + i);
			}

			if (this.interpolationDurationSet) {
				this.interpolationDurationSet = false;
				this.interpolationDuration = this.getInterpolationDuration();
			}

			if (this.renderingDataSet) {
				this.renderingDataSet = false;
				boolean bl = this.interpolationDuration != 0;
				if (bl && this.renderState != null) {
					this.renderState = this.getLerpedRenderState(this.renderState, this.lerpProgress);
				} else {
					this.renderState = this.copyRenderState();
				}

				this.refreshData(bl, this.lerpProgress);
			}
		}
	}

	protected abstract void refreshData(boolean shouldLerp, float lerpProgress);

	@Override
	protected void initDataTracker() {
		this.dataTracker.startTracking(START_INTERPOLATION, 0);
		this.dataTracker.startTracking(INTERPOLATION_DURATION, 0);
		this.dataTracker.startTracking(TRANSLATION, new Vector3f());
		this.dataTracker.startTracking(SCALE, new Vector3f(1.0F, 1.0F, 1.0F));
		this.dataTracker.startTracking(RIGHT_ROTATION, new Quaternionf());
		this.dataTracker.startTracking(LEFT_ROTATION, new Quaternionf());
		this.dataTracker.startTracking(BILLBOARD, BillboardMode.FIXED.getIndex());
		this.dataTracker.startTracking(BRIGHTNESS, -1);
		this.dataTracker.startTracking(VIEW_RANGE, 1.0F);
		this.dataTracker.startTracking(SHADOW_RADIUS, 0.0F);
		this.dataTracker.startTracking(SHADOW_STRENGTH, 1.0F);
		this.dataTracker.startTracking(WIDTH, 0.0F);
		this.dataTracker.startTracking(HEIGHT, 0.0F);
		this.dataTracker.startTracking(GLOW_COLOR_OVERRIDE, -1);
	}

	@Override
	protected void readCustomDataFromNbt(NbtCompound nbt) {
		if (nbt.contains("transformation")) {
			AffineTransformation.ANY_CODEC
				.decode(NbtOps.INSTANCE, nbt.get("transformation"))
				.resultOrPartial(Util.addPrefix("Display entity", field_42397::error))
				.ifPresent(pair -> this.setTransformation((AffineTransformation)pair.getFirst()));
		}

		if (nbt.contains("interpolation_duration", NbtElement.NUMBER_TYPE)) {
			int i = nbt.getInt("interpolation_duration");
			this.setInterpolationDuration(i);
		}

		if (nbt.contains("start_interpolation", NbtElement.NUMBER_TYPE)) {
			int i = nbt.getInt("start_interpolation");
			this.setStartInterpolation(i);
		}

		if (nbt.contains("billboard", NbtElement.STRING_TYPE)) {
			BillboardMode.CODEC
				.decode(NbtOps.INSTANCE, nbt.get("billboard"))
				.resultOrPartial(Util.addPrefix("Display entity", field_42397::error))
				.ifPresent(pair -> this.setBillboardMode((BillboardMode)pair.getFirst()));
		}

		if (nbt.contains("view_range", NbtElement.NUMBER_TYPE)) {
			this.setViewRange(nbt.getFloat("view_range"));
		}

		if (nbt.contains("shadow_radius", NbtElement.NUMBER_TYPE)) {
			this.setShadowRadius(nbt.getFloat("shadow_radius"));
		}

		if (nbt.contains("shadow_strength", NbtElement.NUMBER_TYPE)) {
			this.setShadowStrength(nbt.getFloat("shadow_strength"));
		}

		if (nbt.contains("width", NbtElement.NUMBER_TYPE)) {
			this.setDisplayWidth(nbt.getFloat("width"));
		}

		if (nbt.contains("height", NbtElement.NUMBER_TYPE)) {
			this.setDisplayHeight(nbt.getFloat("height"));
		}

		if (nbt.contains("glow_color_override", NbtElement.NUMBER_TYPE)) {
			this.setGlowColorOverride(nbt.getInt("glow_color_override"));
		}

		if (nbt.contains("brightness", NbtElement.COMPOUND_TYPE)) {
			Brightness.CODEC
				.decode(NbtOps.INSTANCE, nbt.get("brightness"))
				.resultOrPartial(Util.addPrefix("Display entity", field_42397::error))
				.ifPresent(pair -> this.setBrightness((Brightness)pair.getFirst()));
		} else {
			this.setBrightness(null);
		}
	}

	private void setTransformation(AffineTransformation transformation) {
		this.dataTracker.set(TRANSLATION, transformation.getTranslation());
		this.dataTracker.set(LEFT_ROTATION, transformation.getLeftRotation());
		this.dataTracker.set(SCALE, transformation.getScale());
		this.dataTracker.set(RIGHT_ROTATION, transformation.getRightRotation());
	}

	@Override
	protected void writeCustomDataToNbt(NbtCompound nbt) {
		AffineTransformation.ANY_CODEC
			.encodeStart(NbtOps.INSTANCE, getTransformation(this.dataTracker))
			.result()
			.ifPresent(transformations -> nbt.put("transformation", transformations));
		BillboardMode.CODEC.encodeStart(NbtOps.INSTANCE, this.getBillboardMode()).result().ifPresent(billboard -> nbt.put("billboard", billboard));
		nbt.putInt("interpolation_duration", this.getInterpolationDuration());
		nbt.putFloat("view_range", this.getViewRange());
		nbt.putFloat("shadow_radius", this.getShadowRadius());
		nbt.putFloat("shadow_strength", this.getShadowStrength());
		nbt.putFloat("width", this.getDisplayWidth());
		nbt.putFloat("height", this.getDisplayHeight());
		nbt.putInt("glow_color_override", this.getGlowColorOverride());
		Brightness brightness = this.getBrightnessUnpacked();
		if (brightness != null) {
			Brightness.CODEC.encodeStart(NbtOps.INSTANCE, brightness).result().ifPresent(brightnessx -> nbt.put("brightness", brightnessx));
		}
	}

	@Override
	public Packet<ClientPlayPacketListener> createSpawnPacket() {
		return new EntitySpawnS2CPacket(this);
	}

	@Override
	public Box getVisibilityBoundingBox() {
		return this.visibilityBoundingBox;
	}

	@Override
	public PistonBehavior getPistonBehavior() {
		return PistonBehavior.IGNORE;
	}

	@Override
	public boolean canAvoidTraps() {
		return true;
	}

	public Quaternionf getFixedRotation() {
		return this.fixedRotation;
	}

	@Nullable
	public DisplayEntity.RenderState getRenderState() {
		return this.renderState;
	}

	private void setInterpolationDuration(int interpolationDuration) {
		this.dataTracker.set(INTERPOLATION_DURATION, interpolationDuration);
	}

	private int getInterpolationDuration() {
		return this.dataTracker.get(INTERPOLATION_DURATION);
	}

	private void setStartInterpolation(int startInterpolation) {
		this.dataTracker.set(START_INTERPOLATION, startInterpolation, true);
	}

	private int getStartInterpolation() {
		return this.dataTracker.get(START_INTERPOLATION);
	}

	private void setBillboardMode(BillboardMode billboardMode) {
		this.dataTracker.set(BILLBOARD, billboardMode.getIndex());
	}

	private BillboardMode getBillboardMode() {
		return (BillboardMode) BillboardMode.FROM_INDEX.apply(this.dataTracker.get(BILLBOARD));
	}

	private void setBrightness(@Nullable Brightness brightness) {
		this.dataTracker.set(BRIGHTNESS, brightness != null ? brightness.pack() : -1);
	}

	@Nullable
	private Brightness getBrightnessUnpacked() {
		int i = this.dataTracker.get(BRIGHTNESS);
		return i != -1 ? Brightness.unpack(i) : null;
	}

	private int getBrightness() {
		return this.dataTracker.get(BRIGHTNESS);
	}

	private void setViewRange(float viewRange) {
		this.dataTracker.set(VIEW_RANGE, viewRange);
	}

	private float getViewRange() {
		return this.dataTracker.get(VIEW_RANGE);
	}

	private void setShadowRadius(float shadowRadius) {
		this.dataTracker.set(SHADOW_RADIUS, shadowRadius);
	}

	private float getShadowRadius() {
		return this.dataTracker.get(SHADOW_RADIUS);
	}

	private void setShadowStrength(float shadowStrength) {
		this.dataTracker.set(SHADOW_STRENGTH, shadowStrength);
	}

	private float getShadowStrength() {
		return this.dataTracker.get(SHADOW_STRENGTH);
	}

	private void setDisplayWidth(float width) {
		this.dataTracker.set(WIDTH, width);
	}

	private float getDisplayWidth() {
		return this.dataTracker.get(WIDTH);
	}

	private void setDisplayHeight(float height) {
		this.dataTracker.set(HEIGHT, height);
	}

	private int getGlowColorOverride() {
		return this.dataTracker.get(GLOW_COLOR_OVERRIDE);
	}

	private void setGlowColorOverride(int glowColorOverride) {
		this.dataTracker.set(GLOW_COLOR_OVERRIDE, glowColorOverride);
	}

	public float getLerpProgress(float delta) {
		int i = this.interpolationDuration;
		if (i <= 0) {
			return 1.0F;
		} else {
			float f = (float)((long)this.age - this.interpolationStart);
			float g = f + delta;
			float h = MathHelper.clamp(MathHelper.getLerpProgress(g, 0.0F, (float)i), 0.0F, 1.0F);
			this.lerpProgress = h;
			return h;
		}
	}

	private float getDisplayHeight() {
		return this.dataTracker.get(HEIGHT);
	}

	@Override
	public void setPosition(double x, double y, double z) {
		super.setPosition(x, y, z);
		this.updateVisibilityBoundingBox();
	}

	private void updateVisibilityBoundingBox() {
		float f = this.getDisplayWidth();
		float g = this.getDisplayHeight();
		if (f != 0.0F && g != 0.0F) {
			this.ignoreCameraFrustum = false;
			float h = f / 2.0F;
			double d = this.getX();
			double e = this.getY();
			double i = this.getZ();
			this.visibilityBoundingBox = new Box(d - (double)h, e, i - (double)h, d + (double)h, e + (double)g, i + (double)h);
		} else {
			this.ignoreCameraFrustum = true;
		}
	}

	@Override
	public void setPitch(float pitch) {
		super.setPitch(pitch);
		this.updateFixedRotation();
	}

	@Override
	public void setYaw(float yaw) {
		super.setYaw(yaw);
		this.updateFixedRotation();
	}

	private void updateFixedRotation() {
		this.fixedRotation.rotationYXZ((float) (-Math.PI / 180.0) * this.getYaw(), (float) (Math.PI / 180.0) * this.getPitch(), 0.0F);
	}

	@Override
	public boolean shouldRender(double distance) {
		return distance < MathHelper.square((double)this.getViewRange() * 64.0 * getRenderDistanceMultiplier());
	}

	@Override
	public int getTeamColorValue() {
		int i = this.getGlowColorOverride();
		return i != -1 ? i : super.getTeamColorValue();
	}

	private RenderState copyRenderState() {
		return new RenderState(
			AbstractInterpolator.constant(getTransformation(this.dataTracker)),
			this.getBillboardMode(),
			this.getBrightness(),
			FloatLerper.constant(this.getShadowRadius()),
			FloatLerper.constant(this.getShadowStrength()),
			this.getGlowColorOverride()
		);
	}

	private RenderState getLerpedRenderState(RenderState state, float lerpProgress) {
		AffineTransformation affineTransformation = state.transformation.interpolate(lerpProgress);
		float f = state.shadowRadius.lerp(lerpProgress);
		float g = state.shadowStrength.lerp(lerpProgress);
		return new RenderState(
			new AffineTransformationInterpolator(affineTransformation, getTransformation(this.dataTracker)),
			this.getBillboardMode(),
			this.getBrightness(),
			new FloatLerperImpl(f, this.getShadowRadius()),
			new FloatLerperImpl(g, this.getShadowStrength()),
			this.getGlowColorOverride()
		);
	}

	@FunctionalInterface
	public interface AbstractInterpolator<T> {
		static <T> AbstractInterpolator<T> constant(T value) {
			return delta -> value;
		}

		T interpolate(float delta);
	}

	static record AffineTransformationInterpolator(AffineTransformation previous, AffineTransformation current)
		implements AbstractInterpolator<AffineTransformation> {
		public AffineTransformation interpolate(float f) {
			return (double)f >= 1.0 ? this.current : this.previous.interpolate(this.current, f);
		}
	}

	static record ArgbLerper(int previous, int current) implements IntLerper {
		@Override
		public int lerp(float delta) {
			return ColorHelper.Argb.lerp(delta, this.previous, this.current);
		}
	}

	public static enum BillboardMode implements StringIdentifiable {
		FIXED((byte)0, "fixed"),
		VERTICAL((byte)1, "vertical"),
		HORIZONTAL((byte)2, "horizontal"),
		CENTER((byte)3, "center");

		public static final com.mojang.serialization.Codec<BillboardMode> CODEC = StringIdentifiable.createCodec(BillboardMode::values);
		public static final IntFunction<BillboardMode> FROM_INDEX = ValueLists.createIdToValueFunction(
			BillboardMode::getIndex, values(), ValueLists.OutOfBoundsHandling.ZERO
		);
		private final byte index;
		private final String name;

		private BillboardMode(byte index, String name) {
			this.name = name;
			this.index = index;
		}

		@Override
		public String asString() {
			return this.name;
		}

		byte getIndex() {
			return this.index;
		}
	}

	public static class BlockDisplayEntity extends DisplayEntity {
		public static final String BLOCK_STATE_NBT_KEY = "block_state";
		private static final TrackedData<BlockState> BLOCK_STATE = DataTracker.registerData(
			BlockDisplayEntity.class, TrackedDataHandlerRegistry.BLOCK_STATE
		);
		@Nullable
		private DisplayEntity.BlockDisplayEntity.Data data;

		public BlockDisplayEntity(EntityType<?> entityType, World world) {
			super(entityType, world);
		}

		@Override
		protected void initDataTracker() {
			super.initDataTracker();
			this.dataTracker.startTracking(BLOCK_STATE, Blocks.AIR.getDefaultState());
		}

		@Override
		public void onTrackedDataSet(TrackedData<?> data) {
			super.onTrackedDataSet(data);
			if (data.equals(BLOCK_STATE)) {
				this.renderingDataSet = true;
			}
		}

		private BlockState getBlockState() {
			return this.dataTracker.get(BLOCK_STATE);
		}

		private void setBlockState(BlockState state) {
			this.dataTracker.set(BLOCK_STATE, state);
		}

		@Override
		protected void readCustomDataFromNbt(NbtCompound nbt) {
			super.readCustomDataFromNbt(nbt);
			this.setBlockState(NbtHelper.toBlockState(this.getWorld().createCommandRegistryWrapper(RegistryKeys.BLOCK), nbt.getCompound("block_state")));
		}

		@Override
		protected void writeCustomDataToNbt(NbtCompound nbt) {
			super.writeCustomDataToNbt(nbt);
			nbt.put("block_state", NbtHelper.fromBlockState(this.getBlockState()));
		}

		@Nullable
		public DisplayEntity.BlockDisplayEntity.Data getData() {
			return this.data;
		}

		@Override
		protected void refreshData(boolean shouldLerp, float lerpProgress) {
			this.data = new Data(this.getBlockState());
		}

		public static record Data(BlockState blockState) {
		}
	}

	@FunctionalInterface
	public interface FloatLerper {
		static FloatLerper constant(float value) {
			return delta -> value;
		}

		float lerp(float delta);
	}

	static record FloatLerperImpl(float previous, float current) implements FloatLerper {
		@Override
		public float lerp(float delta) {
			return MathHelper.lerp(delta, this.previous, this.current);
		}
	}

	@FunctionalInterface
	public interface IntLerper {
		static IntLerper constant(int value) {
			return delta -> value;
		}

		int lerp(float delta);
	}

	static record IntLerperImpl(int previous, int current) implements IntLerper {
		@Override
		public int lerp(float delta) {
			return MathHelper.lerp(delta, this.previous, this.current);
		}
	}

	public static class ItemDisplayEntity extends DisplayEntity {
		private static final String ITEM_NBT_KEY = "item";
		private static final String ITEM_DISPLAY_NBT_KEY = "item_display";
		private static final TrackedData<ItemStack> ITEM = DataTracker.registerData(ItemDisplayEntity.class, TrackedDataHandlerRegistry.ITEM_STACK);
		private static final TrackedData<Byte> ITEM_DISPLAY = DataTracker.registerData(ItemDisplayEntity.class, TrackedDataHandlerRegistry.BYTE);
		private final StackReference stackReference = new StackReference() {
			@Override
			public ItemStack get() {
				return ItemDisplayEntity.this.getItemStack();
			}

			@Override
			public boolean set(ItemStack stack) {
				ItemDisplayEntity.this.setItemStack(stack);
				return true;
			}
		};
		@Nullable
		private DisplayEntity.ItemDisplayEntity.Data data;

		public ItemDisplayEntity(EntityType<?> entityType, World world) {
			super(entityType, world);
		}

		@Override
		protected void initDataTracker() {
			super.initDataTracker();
			this.dataTracker.startTracking(ITEM, ItemStack.EMPTY);
			this.dataTracker.startTracking(ITEM_DISPLAY, ModelTransformationMode.NONE.getIndex());
		}

		@Override
		public void onTrackedDataSet(TrackedData<?> data) {
			super.onTrackedDataSet(data);
			if (ITEM.equals(data) || ITEM_DISPLAY.equals(data)) {
				this.renderingDataSet = true;
			}
		}

		ItemStack getItemStack() {
			return this.dataTracker.get(ITEM);
		}

		void setItemStack(ItemStack stack) {
			this.dataTracker.set(ITEM, stack);
		}

		private void setTransformationMode(ModelTransformationMode transformationMode) {
			this.dataTracker.set(ITEM_DISPLAY, transformationMode.getIndex());
		}

		private ModelTransformationMode getTransformationMode() {
			return (ModelTransformationMode)ModelTransformationMode.FROM_INDEX.apply(this.dataTracker.get(ITEM_DISPLAY));
		}

		@Override
		protected void readCustomDataFromNbt(NbtCompound nbt) {
			super.readCustomDataFromNbt(nbt);
			this.setItemStack(ItemStack.fromNbt(nbt.getCompound("item")));
			if (nbt.contains("item_display", NbtElement.STRING_TYPE)) {
				ModelTransformationMode.CODEC
					.decode(NbtOps.INSTANCE, nbt.get("item_display"))
					.resultOrPartial(Util.addPrefix("Display entity", DisplayEntity.field_42397::error))
					.ifPresent(mode -> this.setTransformationMode((ModelTransformationMode)mode.getFirst()));
			}
		}

		@Override
		protected void writeCustomDataToNbt(NbtCompound nbt) {
			super.writeCustomDataToNbt(nbt);
			nbt.put("item", this.getItemStack().writeNbt(new NbtCompound()));
			ModelTransformationMode.CODEC.encodeStart(NbtOps.INSTANCE, this.getTransformationMode()).result().ifPresent(nbtx -> nbt.put("item_display", nbtx));
		}

		@Override
		public StackReference getStackReference(int mappedIndex) {
			return mappedIndex == 0 ? this.stackReference : StackReference.EMPTY;
		}

		@Nullable
		public DisplayEntity.ItemDisplayEntity.Data getData() {
			return this.data;
		}

		@Override
		protected void refreshData(boolean shouldLerp, float lerpProgress) {
			this.data = new Data(this.getItemStack(), this.getTransformationMode());
		}

		public static record Data(ItemStack itemStack, ModelTransformationMode itemTransform) {
		}
	}

	public static record RenderState(
		AbstractInterpolator<AffineTransformation> transformation,
		BillboardMode billboardConstraints,
		int brightnessOverride,
		FloatLerper shadowRadius,
		FloatLerper shadowStrength,
		int glowColorOverride
	) {
	}

	public static class TextDisplayEntity extends DisplayEntity {
		public static final String TEXT_NBT_KEY = "text";
		private static final String LINE_WIDTH_NBT_KEY = "line_width";
		private static final String TEXT_OPACITY_NBT_KEY = "text_opacity";
		private static final String BACKGROUND_NBT_KEY = "background";
		private static final String SHADOW_NBT_KEY = "shadow";
		private static final String SEE_THROUGH_NBT_KEY = "see_through";
		private static final String DEFAULT_BACKGROUND_NBT_KEY = "default_background";
		private static final String ALIGNMENT_NBT_KEY = "alignment";
		public static final byte SHADOW_FLAG = 1;
		public static final byte SEE_THROUGH_FLAG = 2;
		public static final byte DEFAULT_BACKGROUND_FLAG = 4;
		public static final byte LEFT_ALIGNMENT_FLAG = 8;
		public static final byte RIGHT_ALIGNMENT_FLAG = 16;
		private static final byte INITIAL_TEXT_OPACITY = -1;
		public static final int INITIAL_BACKGROUND = 1073741824;
		private static final TrackedData<Text> TEXT = DataTracker.registerData(TextDisplayEntity.class, TrackedDataHandlerRegistry.TEXT_COMPONENT);
		private static final TrackedData<Integer> LINE_WIDTH = DataTracker.registerData(TextDisplayEntity.class, TrackedDataHandlerRegistry.INTEGER);
		private static final TrackedData<Integer> BACKGROUND = DataTracker.registerData(TextDisplayEntity.class, TrackedDataHandlerRegistry.INTEGER);
		private static final TrackedData<Byte> TEXT_OPACITY = DataTracker.registerData(TextDisplayEntity.class, TrackedDataHandlerRegistry.BYTE);
		private static final TrackedData<Byte> TEXT_DISPLAY_FLAGS = DataTracker.registerData(TextDisplayEntity.class, TrackedDataHandlerRegistry.BYTE);
		private static final IntSet TEXT_RENDERING_DATA_IDS = IntSet.of(
			TEXT.getId(), LINE_WIDTH.getId(), BACKGROUND.getId(), TEXT_OPACITY.getId(), TEXT_DISPLAY_FLAGS.getId()
		);
		@Nullable
		private DisplayEntity.TextDisplayEntity.TextLines textLines;
		@Nullable
		private DisplayEntity.TextDisplayEntity.Data data;

		public TextDisplayEntity(EntityType<?> entityType, World world) {
			super(entityType, world);
		}

		@Override
		protected void initDataTracker() {
			super.initDataTracker();
			this.dataTracker.startTracking(TEXT, Text.empty());
			this.dataTracker.startTracking(LINE_WIDTH, 200);
			this.dataTracker.startTracking(BACKGROUND, 1073741824);
			this.dataTracker.startTracking(TEXT_OPACITY, (byte)-1);
			this.dataTracker.startTracking(TEXT_DISPLAY_FLAGS, (byte)0);
		}

		@Override
		public void onTrackedDataSet(TrackedData<?> data) {
			super.onTrackedDataSet(data);
			if (TEXT_RENDERING_DATA_IDS.contains(data.getId())) {
				this.renderingDataSet = true;
			}
		}

		private Text getText() {
			return this.dataTracker.get(TEXT);
		}

		private void setText(Text text) {
			this.dataTracker.set(TEXT, text);
		}

		private int getLineWidth() {
			return this.dataTracker.get(LINE_WIDTH);
		}

		private void setLineWidth(int lineWidth) {
			this.dataTracker.set(LINE_WIDTH, lineWidth);
		}

		private byte getTextOpacity() {
			return this.dataTracker.get(TEXT_OPACITY);
		}

		private void setTextOpacity(byte textOpacity) {
			this.dataTracker.set(TEXT_OPACITY, textOpacity);
		}

		private int getBackground() {
			return this.dataTracker.get(BACKGROUND);
		}

		private void setBackground(int background) {
			this.dataTracker.set(BACKGROUND, background);
		}

		private byte getDisplayFlags() {
			return this.dataTracker.get(TEXT_DISPLAY_FLAGS);
		}

		private void setDisplayFlags(byte flags) {
			this.dataTracker.set(TEXT_DISPLAY_FLAGS, flags);
		}

		private static byte readFlag(byte flags, NbtCompound nbt, String nbtKey, byte flag) {
			return nbt.getBoolean(nbtKey) ? (byte)(flags | flag) : flags;
		}

		@Override
		protected void readCustomDataFromNbt(NbtCompound nbt) {
			super.readCustomDataFromNbt(nbt);
			if (nbt.contains("line_width", NbtElement.NUMBER_TYPE)) {
				this.setLineWidth(nbt.getInt("line_width"));
			}

			if (nbt.contains("text_opacity", NbtElement.NUMBER_TYPE)) {
				this.setTextOpacity(nbt.getByte("text_opacity"));
			}

			if (nbt.contains("background", NbtElement.NUMBER_TYPE)) {
				this.setBackground(nbt.getInt("background"));
			}

			byte b = readFlag((byte)0, nbt, "shadow", (byte)1);
			b = readFlag(b, nbt, "see_through", (byte)2);
			b = readFlag(b, nbt, "default_background", (byte)4);
			Optional<TextAlignment> optional = TextAlignment.CODEC
				.decode(NbtOps.INSTANCE, nbt.get("alignment"))
				.resultOrPartial(Util.addPrefix("Display entity", DisplayEntity.field_42397::error))
				.map(Pair::getFirst);
			if (optional.isPresent()) {
				b = switch ((TextAlignment)optional.get()) {
					case CENTER -> b;
					case LEFT -> (byte)(b | 8);
					case RIGHT -> (byte)(b | 16);
				};
			}

			this.setDisplayFlags(b);
			if (nbt.contains("text", NbtElement.STRING_TYPE)) {
				String string = nbt.getString("text");

				try {
					Text text = Text.Serializer.fromJson(string);
					if (text != null) {
						ServerCommandSource serverCommandSource = this.getCommandSource().withLevel(2);
						Text text2 = Texts.parse(serverCommandSource, text, this, 0);
						this.setText(text2);
					} else {
						this.setText(Text.empty());
					}
				} catch (Exception var8) {
					DisplayEntity.field_42397.warn("Failed to parse display entity text {}", string, var8);
				}
			}
		}

		private static void writeFlag(byte flags, NbtCompound nbt, String nbtKey, byte flag) {
			nbt.putBoolean(nbtKey, (flags & flag) != 0);
		}

		@Override
		protected void writeCustomDataToNbt(NbtCompound nbt) {
			super.writeCustomDataToNbt(nbt);
			nbt.putString("text", Text.Serializer.toJson(this.getText()));
			nbt.putInt("line_width", this.getLineWidth());
			nbt.putInt("background", this.getBackground());
			nbt.putByte("text_opacity", this.getTextOpacity());
			byte b = this.getDisplayFlags();
			writeFlag(b, nbt, "shadow", (byte)1);
			writeFlag(b, nbt, "see_through", (byte)2);
			writeFlag(b, nbt, "default_background", (byte)4);
			TextAlignment.CODEC
				.encodeStart(NbtOps.INSTANCE, getAlignment(b))
				.result()
				.ifPresent(nbtElement -> nbt.put("alignment", nbtElement));
		}

		@Override
		protected void refreshData(boolean shouldLerp, float lerpProgress) {
			if (shouldLerp && this.data != null) {
				this.data = this.getLerpedRenderState(this.data, lerpProgress);
			} else {
				this.data = this.copyData();
			}

			this.textLines = null;
		}

		@Nullable
		public DisplayEntity.TextDisplayEntity.Data getData() {
			return this.data;
		}

		private Data copyData() {
			return new Data(
				this.getText(),
				this.getLineWidth(),
				IntLerper.constant(this.getTextOpacity()),
				IntLerper.constant(this.getBackground()),
				this.getDisplayFlags()
			);
		}

		private Data getLerpedRenderState(Data data, float lerpProgress) {
			int i = data.backgroundColor.lerp(lerpProgress);
			int j = data.textOpacity.lerp(lerpProgress);
			return new Data(
				this.getText(),
				this.getLineWidth(),
				new IntLerperImpl(j, this.getTextOpacity()),
				new ArgbLerper(i, this.getBackground()),
				this.getDisplayFlags()
			);
		}

		public TextLines splitLines(LineSplitter splitter) {
			if (this.textLines == null) {
				if (this.data != null) {
					this.textLines = splitter.split(this.data.text(), this.data.lineWidth());
				} else {
					this.textLines = new TextLines(List.of(), 0);
				}
			}

			return this.textLines;
		}

		public static TextAlignment getAlignment(byte flags) {
			if ((flags & 8) != 0) {
				return TextAlignment.LEFT;
			} else {
				return (flags & 16) != 0 ? TextAlignment.RIGHT : TextAlignment.CENTER;
			}
		}

		public static record Data(Text text, int lineWidth, IntLerper textOpacity, IntLerper backgroundColor, byte flags) {
		}

		@FunctionalInterface
		public interface LineSplitter {
			TextLines split(Text text, int lineWidth);
		}

		public static enum TextAlignment implements StringIdentifiable {
			CENTER("center"),
			LEFT("left"),
			RIGHT("right");

			public static final com.mojang.serialization.Codec<TextAlignment> CODEC = StringIdentifiable.createCodec(
				TextAlignment::values
			);
			private final String name;

			private TextAlignment(String name) {
				this.name = name;
			}

			@Override
			public String asString() {
				return this.name;
			}
		}

		public static record TextLine(OrderedText contents, int width) {
		}

		public static record TextLines(List<TextLine> lines, int width) {
		}
	}
}
