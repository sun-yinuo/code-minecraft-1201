package net.minecraft.data.family;

import com.google.common.collect.Maps;
import java.util.Map;
import java.util.Optional;
import net.minecraft.block.Block;
import net.minecraft.resource.featuretoggle.FeatureFlag;
import net.minecraft.resource.featuretoggle.FeatureFlags;
import net.minecraft.resource.featuretoggle.FeatureSet;
import net.minecraft.util.Util;
import org.jetbrains.annotations.Nullable;

public class BlockFamily {
	private final Block baseBlock;
	final Map<Variant, Block> variants = Maps.<Variant, Block>newHashMap();
	FeatureSet requiredFeatures = FeatureFlags.VANILLA_FEATURES;
	boolean generateModels = true;
	boolean generateRecipes = true;
	@Nullable
	String group;
	@Nullable
	String unlockCriterionName;

	BlockFamily(Block baseBlock) {
		this.baseBlock = baseBlock;
	}

	public Block getBaseBlock() {
		return this.baseBlock;
	}

	public Map<Variant, Block> getVariants() {
		return this.variants;
	}

	public Block getVariant(Variant variant) {
		return (Block)this.variants.get(variant);
	}

	public boolean shouldGenerateModels() {
		return this.generateModels;
	}

	public boolean shouldGenerateRecipes(FeatureSet enabledFeatures) {
		return this.generateRecipes && this.requiredFeatures.isSubsetOf(enabledFeatures);
	}

	public Optional<String> getGroup() {
		return Util.isBlank(this.group) ? Optional.empty() : Optional.of(this.group);
	}

	public Optional<String> getUnlockCriterionName() {
		return Util.isBlank(this.unlockCriterionName) ? Optional.empty() : Optional.of(this.unlockCriterionName);
	}

	public static class Builder {
		private final BlockFamily family;

		public Builder(Block baseBlock) {
			this.family = new BlockFamily(baseBlock);
		}

		public BlockFamily build() {
			return this.family;
		}

		public Builder button(Block block) {
			this.family.variants.put(Variant.BUTTON, block);
			return this;
		}

		public Builder chiseled(Block block) {
			this.family.variants.put(Variant.CHISELED, block);
			return this;
		}

		public Builder mosaic(Block block) {
			this.family.variants.put(Variant.MOSAIC, block);
			return this;
		}

		public Builder cracked(Block block) {
			this.family.variants.put(Variant.CRACKED, block);
			return this;
		}

		public Builder cut(Block block) {
			this.family.variants.put(Variant.CUT, block);
			return this;
		}

		public Builder door(Block block) {
			this.family.variants.put(Variant.DOOR, block);
			return this;
		}

		public Builder customFence(Block block) {
			this.family.variants.put(Variant.CUSTOM_FENCE, block);
			return this;
		}

		public Builder fence(Block block) {
			this.family.variants.put(Variant.FENCE, block);
			return this;
		}

		public Builder customFenceGate(Block block) {
			this.family.variants.put(Variant.CUSTOM_FENCE_GATE, block);
			return this;
		}

		public Builder fenceGate(Block block) {
			this.family.variants.put(Variant.FENCE_GATE, block);
			return this;
		}

		public Builder sign(Block block, Block wallBlock) {
			this.family.variants.put(Variant.SIGN, block);
			this.family.variants.put(Variant.WALL_SIGN, wallBlock);
			return this;
		}

		public Builder slab(Block block) {
			this.family.variants.put(Variant.SLAB, block);
			return this;
		}

		public Builder stairs(Block block) {
			this.family.variants.put(Variant.STAIRS, block);
			return this;
		}

		public Builder pressurePlate(Block block) {
			this.family.variants.put(Variant.PRESSURE_PLATE, block);
			return this;
		}

		public Builder polished(Block block) {
			this.family.variants.put(Variant.POLISHED, block);
			return this;
		}

		public Builder trapdoor(Block block) {
			this.family.variants.put(Variant.TRAPDOOR, block);
			return this;
		}

		public Builder wall(Block block) {
			this.family.variants.put(Variant.WALL, block);
			return this;
		}

		public Builder noGenerateModels() {
			this.family.generateModels = false;
			return this;
		}

		public Builder noGenerateRecipes() {
			this.family.generateRecipes = false;
			return this;
		}

		public Builder requires(FeatureFlag... features) {
			this.family.requiredFeatures = FeatureFlags.FEATURE_MANAGER.featureSetOf(features);
			return this;
		}

		public Builder group(String group) {
			this.family.group = group;
			return this;
		}

		public Builder unlockCriterionName(String unlockCriterionName) {
			this.family.unlockCriterionName = unlockCriterionName;
			return this;
		}
	}

	public static enum Variant {
		BUTTON("button"),
		CHISELED("chiseled"),
		CRACKED("cracked"),
		CUT("cut"),
		DOOR("door"),
		CUSTOM_FENCE("custom_fence"),
		FENCE("fence"),
		CUSTOM_FENCE_GATE("custom_fence_gate"),
		FENCE_GATE("fence_gate"),
		MOSAIC("mosaic"),
		SIGN("sign"),
		SLAB("slab"),
		STAIRS("stairs"),
		PRESSURE_PLATE("pressure_plate"),
		POLISHED("polished"),
		TRAPDOOR("trapdoor"),
		WALL("wall"),
		WALL_SIGN("wall_sign");

		private final String name;

		private Variant(String name) {
			this.name = name;
		}

		public String getName() {
			return this.name;
		}
	}
}
