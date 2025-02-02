package net.minecraft.data.client;

import com.google.gson.JsonElement;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;
import net.minecraft.block.Block;
import net.minecraft.util.Identifier;

/**
 * Represents a model with texture variables defined.
 */
public class TexturedModel {
	public static final Factory CUBE_ALL = makeFactory(TextureMap::all, Models.CUBE_ALL);
	public static final Factory CUBE_MIRRORED_ALL = makeFactory(TextureMap::all, Models.CUBE_MIRRORED_ALL);
	public static final Factory CUBE_COLUMN = makeFactory(TextureMap::sideEnd, Models.CUBE_COLUMN);
	public static final Factory CUBE_COLUMN_HORIZONTAL = makeFactory(TextureMap::sideEnd, Models.CUBE_COLUMN_HORIZONTAL);
	public static final Factory CUBE_BOTTOM_TOP = makeFactory(TextureMap::sideTopBottom, Models.CUBE_BOTTOM_TOP);
	public static final Factory CUBE_TOP = makeFactory(TextureMap::sideAndTop, Models.CUBE_TOP);
	public static final Factory ORIENTABLE = makeFactory(TextureMap::sideFrontTop, Models.ORIENTABLE);
	public static final Factory ORIENTABLE_WITH_BOTTOM = makeFactory(TextureMap::sideFrontTopBottom, Models.ORIENTABLE_WITH_BOTTOM);
	public static final Factory CARPET = makeFactory(TextureMap::wool, Models.CARPET);
	public static final Factory FLOWERBED_1 = makeFactory(TextureMap::flowerbed, Models.FLOWERBED_1);
	public static final Factory FLOWERBED_2 = makeFactory(TextureMap::flowerbed, Models.FLOWERBED_2);
	public static final Factory FLOWERBED_3 = makeFactory(TextureMap::flowerbed, Models.FLOWERBED_3);
	public static final Factory FLOWERBED_4 = makeFactory(TextureMap::flowerbed, Models.FLOWERBED_4);
	public static final Factory TEMPLATE_GLAZED_TERRACOTTA = makeFactory(TextureMap::pattern, Models.TEMPLATE_GLAZED_TERRACOTTA);
	public static final Factory CORAL_FAN = makeFactory(TextureMap::fan, Models.CORAL_FAN);
	public static final Factory PARTICLE = makeFactory(TextureMap::particle, Models.PARTICLE);
	public static final Factory TEMPLATE_ANVIL = makeFactory(TextureMap::top, Models.TEMPLATE_ANVIL);
	public static final Factory LEAVES = makeFactory(TextureMap::all, Models.LEAVES);
	public static final Factory TEMPLATE_LANTERN = makeFactory(TextureMap::lantern, Models.TEMPLATE_LANTERN);
	public static final Factory TEMPLATE_HANGING_LANTERN = makeFactory(TextureMap::lantern, Models.TEMPLATE_HANGING_LANTERN);
	public static final Factory TEMPLATE_SEAGRASS = makeFactory(TextureMap::texture, Models.TEMPLATE_SEAGRASS);
	public static final Factory END_FOR_TOP_CUBE_COLUMN = makeFactory(TextureMap::sideAndEndForTop, Models.CUBE_COLUMN);
	public static final Factory END_FOR_TOP_CUBE_COLUMN_HORIZONTAL = makeFactory(TextureMap::sideAndEndForTop, Models.CUBE_COLUMN_HORIZONTAL);
	public static final Factory SIDE_TOP_BOTTOM_WALL = makeFactory(TextureMap::wallSideTopBottom, Models.CUBE_BOTTOM_TOP);
	public static final Factory SIDE_END_WALL = makeFactory(TextureMap::wallSideEnd, Models.CUBE_COLUMN);
	private final TextureMap textures;
	private final Model model;

	private TexturedModel(TextureMap textures, Model model) {
		this.textures = textures;
		this.model = model;
	}

	public Model getModel() {
		return this.model;
	}

	public TextureMap getTextures() {
		return this.textures;
	}

	public TexturedModel textures(Consumer<TextureMap> texturesConsumer) {
		texturesConsumer.accept(this.textures);
		return this;
	}

	public Identifier upload(Block block, BiConsumer<Identifier, Supplier<JsonElement>> writer) {
		return this.model.upload(block, this.textures, writer);
	}

	public Identifier upload(Block block, String suffix, BiConsumer<Identifier, Supplier<JsonElement>> writer) {
		return this.model.upload(block, suffix, this.textures, writer);
	}

	public static Factory makeFactory(Function<Block, TextureMap> texturesGetter, Model model) {
		return block -> new TexturedModel((TextureMap)texturesGetter.apply(block), model);
	}

	public static TexturedModel getCubeAll(Identifier id) {
		return new TexturedModel(TextureMap.all(id), Models.CUBE_ALL);
	}

	@FunctionalInterface
	public interface Factory {
		TexturedModel get(Block block);

		default Identifier upload(Block block, BiConsumer<Identifier, Supplier<JsonElement>> writer) {
			return this.get(block).upload(block, writer);
		}

		default Identifier upload(Block block, String suffix, BiConsumer<Identifier, Supplier<JsonElement>> writer) {
			return this.get(block).upload(block, suffix, writer);
		}

		default Factory andThen(Consumer<TextureMap> consumer) {
			return block -> this.get(block).textures(consumer);
		}
	}
}
