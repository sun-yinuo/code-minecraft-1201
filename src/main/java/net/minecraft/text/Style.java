package net.minecraft.text;

import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonSerializationContext;
import com.google.gson.JsonSerializer;
import com.google.gson.JsonSyntaxException;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.lang.reflect.Type;
import java.util.Objects;
import java.util.Optional;
import net.minecraft.util.Formatting;
import net.minecraft.util.Identifier;
import net.minecraft.util.InvalidIdentifierException;
import net.minecraft.util.JsonHelper;
import org.jetbrains.annotations.Nullable;

/**
 * The style of a {@link Text}, representing cosmetic attributes. It includes
 * font, formatting, click/hover events (actions), color, etc.
 * 
 * <p>A style is immutable.
 * 
 * @see Text
 */
public class Style {
	/**
	 * An empty style.
	 */
	public static final Style EMPTY = new Style(null, null, null, null, null, null, null, null, null, null);
	public static final Codec<Style> CODEC = RecordCodecBuilder.create(
		instance -> instance.group(
					TextColor.CODEC.optionalFieldOf("color").forGetter(style -> Optional.ofNullable(style.color)),
					Codec.BOOL.optionalFieldOf("bold").forGetter(style -> Optional.ofNullable(style.bold)),
					Codec.BOOL.optionalFieldOf("italic").forGetter(style -> Optional.ofNullable(style.italic)),
					Codec.BOOL.optionalFieldOf("underlined").forGetter(style -> Optional.ofNullable(style.underlined)),
					Codec.BOOL.optionalFieldOf("strikethrough").forGetter(style -> Optional.ofNullable(style.strikethrough)),
					Codec.BOOL.optionalFieldOf("obfuscated").forGetter(style -> Optional.ofNullable(style.obfuscated)),
					Codec.STRING.optionalFieldOf("insertion").forGetter(style -> Optional.ofNullable(style.insertion)),
					Identifier.CODEC.optionalFieldOf("font").forGetter(style -> Optional.ofNullable(style.font))
				)
				.apply(instance, Style::of)
	);
	/**
	 * The identifier for the default font of a style.
	 */
	public static final Identifier DEFAULT_FONT_ID = new Identifier("minecraft", "default");
	@Nullable
	final TextColor color;
	@Nullable
	final Boolean bold;
	@Nullable
	final Boolean italic;
	@Nullable
	final Boolean underlined;
	@Nullable
	final Boolean strikethrough;
	@Nullable
	final Boolean obfuscated;
	@Nullable
	final ClickEvent clickEvent;
	@Nullable
	final HoverEvent hoverEvent;
	@Nullable
	final String insertion;
	@Nullable
	final Identifier font;

	private static Style of(
		Optional<TextColor> color,
		Optional<Boolean> bold,
		Optional<Boolean> italic,
		Optional<Boolean> underlined,
		Optional<Boolean> strikethrough,
		Optional<Boolean> obfuscated,
		Optional<String> insertion,
		Optional<Identifier> font
	) {
		return new Style(
			(TextColor)color.orElse(null),
			(Boolean)bold.orElse(null),
			(Boolean)italic.orElse(null),
			(Boolean)underlined.orElse(null),
			(Boolean)strikethrough.orElse(null),
			(Boolean)obfuscated.orElse(null),
			null,
			null,
			(String)insertion.orElse(null),
			(Identifier)font.orElse(null)
		);
	}

	Style(
		@Nullable TextColor color,
		@Nullable Boolean bold,
		@Nullable Boolean italic,
		@Nullable Boolean underlined,
		@Nullable Boolean strikethrough,
		@Nullable Boolean obfuscated,
		@Nullable ClickEvent clickEvent,
		@Nullable HoverEvent hoverEvent,
		@Nullable String insertion,
		@Nullable Identifier font
	) {
		this.color = color;
		this.bold = bold;
		this.italic = italic;
		this.underlined = underlined;
		this.strikethrough = strikethrough;
		this.obfuscated = obfuscated;
		this.clickEvent = clickEvent;
		this.hoverEvent = hoverEvent;
		this.insertion = insertion;
		this.font = font;
	}

	/**
	 * Returns the color of this style.
	 */
	@Nullable
	public TextColor getColor() {
		return this.color;
	}

	/**
	 * Returns whether the style has bold formatting.
	 * 
	 * @see Formatting#BOLD
	 */
	public boolean isBold() {
		return this.bold == Boolean.TRUE;
	}

	/**
	 * Returns whether the style has italic formatting.
	 * 
	 * @see Formatting#ITALIC
	 */
	public boolean isItalic() {
		return this.italic == Boolean.TRUE;
	}

	/**
	 * Returns whether the style has strikethrough formatting.
	 * 
	 * @see Formatting#STRIKETHROUGH
	 */
	public boolean isStrikethrough() {
		return this.strikethrough == Boolean.TRUE;
	}

	/**
	 * Returns whether the style has underline formatting.
	 * 
	 * @see Formatting#UNDERLINE
	 */
	public boolean isUnderlined() {
		return this.underlined == Boolean.TRUE;
	}

	/**
	 * Returns whether the style has obfuscated formatting.
	 * 
	 * @see Formatting#OBFUSCATED
	 */
	public boolean isObfuscated() {
		return this.obfuscated == Boolean.TRUE;
	}

	/**
	 * Returns if this is the empty style.
	 * 
	 * @see #EMPTY
	 */
	public boolean isEmpty() {
		return this == EMPTY;
	}

	/**
	 * Returns the click event of this style.
	 */
	@Nullable
	public ClickEvent getClickEvent() {
		return this.clickEvent;
	}

	/**
	 * Returns the hover event of this style.
	 */
	@Nullable
	public HoverEvent getHoverEvent() {
		return this.hoverEvent;
	}

	/**
	 * Returns the insertion text of the style.
	 * 
	 * <p>An insertion is inserted when a piece of text clicked while shift key
	 * is down in the chat HUD.
	 */
	@Nullable
	public String getInsertion() {
		return this.insertion;
	}

	/**
	 * Returns the font of this style.
	 */
	public Identifier getFont() {
		return this.font != null ? this.font : DEFAULT_FONT_ID;
	}

	/**
	 * Returns a new style with the color provided and all other attributes of
	 * this style.
	 * 
	 * @param color the new color
	 */
	public Style withColor(@Nullable TextColor color) {
		return new Style(
			color, this.bold, this.italic, this.underlined, this.strikethrough, this.obfuscated, this.clickEvent, this.hoverEvent, this.insertion, this.font
		);
	}

	/**
	 * Returns a new style with the color provided and all other attributes of
	 * this style.
	 * 
	 * @param color the new color
	 */
	public Style withColor(@Nullable Formatting color) {
		return this.withColor(color != null ? TextColor.fromFormatting(color) : null);
	}

	public Style withColor(int rgbColor) {
		return this.withColor(TextColor.fromRgb(rgbColor));
	}

	/**
	 * Returns a new style with the bold attribute provided and all other
	 * attributes of this style.
	 * 
	 * @param bold the new bold property
	 */
	public Style withBold(@Nullable Boolean bold) {
		return new Style(
			this.color, bold, this.italic, this.underlined, this.strikethrough, this.obfuscated, this.clickEvent, this.hoverEvent, this.insertion, this.font
		);
	}

	/**
	 * Returns a new style with the italic attribute provided and all other
	 * attributes of this style.
	 * 
	 * @param italic the new italic property
	 */
	public Style withItalic(@Nullable Boolean italic) {
		return new Style(
			this.color, this.bold, italic, this.underlined, this.strikethrough, this.obfuscated, this.clickEvent, this.hoverEvent, this.insertion, this.font
		);
	}

	/**
	 * Returns a new style with the underline attribute provided and all other
	 * attributes of this style.
	 */
	public Style withUnderline(@Nullable Boolean underline) {
		return new Style(
			this.color, this.bold, this.italic, underline, this.strikethrough, this.obfuscated, this.clickEvent, this.hoverEvent, this.insertion, this.font
		);
	}

	public Style withStrikethrough(@Nullable Boolean strikethrough) {
		return new Style(
			this.color, this.bold, this.italic, this.underlined, strikethrough, this.obfuscated, this.clickEvent, this.hoverEvent, this.insertion, this.font
		);
	}

	public Style withObfuscated(@Nullable Boolean obfuscated) {
		return new Style(
			this.color, this.bold, this.italic, this.underlined, this.strikethrough, obfuscated, this.clickEvent, this.hoverEvent, this.insertion, this.font
		);
	}

	/**
	 * Returns a new style with the click event provided and all other
	 * attributes of this style.
	 * 
	 * @param clickEvent the new click event
	 */
	public Style withClickEvent(@Nullable ClickEvent clickEvent) {
		return new Style(
			this.color, this.bold, this.italic, this.underlined, this.strikethrough, this.obfuscated, clickEvent, this.hoverEvent, this.insertion, this.font
		);
	}

	/**
	 * Returns a new style with the hover event provided and all other
	 * attributes of this style.
	 * 
	 * @param hoverEvent the new hover event
	 */
	public Style withHoverEvent(@Nullable HoverEvent hoverEvent) {
		return new Style(
			this.color, this.bold, this.italic, this.underlined, this.strikethrough, this.obfuscated, this.clickEvent, hoverEvent, this.insertion, this.font
		);
	}

	/**
	 * Returns a new style with the insertion provided and all other
	 * attributes of this style.
	 * 
	 * @param insertion the new insertion string
	 */
	public Style withInsertion(@Nullable String insertion) {
		return new Style(
			this.color, this.bold, this.italic, this.underlined, this.strikethrough, this.obfuscated, this.clickEvent, this.hoverEvent, insertion, this.font
		);
	}

	/**
	 * Returns a new style with the font provided and all other
	 * attributes of this style.
	 * 
	 * @param font the new font
	 */
	public Style withFont(@Nullable Identifier font) {
		return new Style(
			this.color, this.bold, this.italic, this.underlined, this.strikethrough, this.obfuscated, this.clickEvent, this.hoverEvent, this.insertion, font
		);
	}

	/**
	 * Returns a new style with the formatting provided and all other
	 * attributes of this style.
	 * 
	 * @param formatting the new formatting
	 */
	public Style withFormatting(Formatting formatting) {
		TextColor textColor = this.color;
		Boolean boolean_ = this.bold;
		Boolean boolean2 = this.italic;
		Boolean boolean3 = this.strikethrough;
		Boolean boolean4 = this.underlined;
		Boolean boolean5 = this.obfuscated;
		switch (formatting) {
			case OBFUSCATED:
				boolean5 = true;
				break;
			case BOLD:
				boolean_ = true;
				break;
			case STRIKETHROUGH:
				boolean3 = true;
				break;
			case UNDERLINE:
				boolean4 = true;
				break;
			case ITALIC:
				boolean2 = true;
				break;
			case RESET:
				return EMPTY;
			default:
				textColor = TextColor.fromFormatting(formatting);
		}

		return new Style(textColor, boolean_, boolean2, boolean4, boolean3, boolean5, this.clickEvent, this.hoverEvent, this.insertion, this.font);
	}

	/**
	 * Returns a new style with the formatting provided and some applicable
	 * attributes of this style.
	 * 
	 * <p>When a color formatting is passed for {@code formatting}, the other
	 * formattings, including bold, italic, strikethrough, underlined, and
	 * obfuscated, are all removed.
	 * 
	 * @param formatting the new formatting
	 */
	public Style withExclusiveFormatting(Formatting formatting) {
		TextColor textColor = this.color;
		Boolean boolean_ = this.bold;
		Boolean boolean2 = this.italic;
		Boolean boolean3 = this.strikethrough;
		Boolean boolean4 = this.underlined;
		Boolean boolean5 = this.obfuscated;
		switch (formatting) {
			case OBFUSCATED:
				boolean5 = true;
				break;
			case BOLD:
				boolean_ = true;
				break;
			case STRIKETHROUGH:
				boolean3 = true;
				break;
			case UNDERLINE:
				boolean4 = true;
				break;
			case ITALIC:
				boolean2 = true;
				break;
			case RESET:
				return EMPTY;
			default:
				boolean5 = false;
				boolean_ = false;
				boolean3 = false;
				boolean4 = false;
				boolean2 = false;
				textColor = TextColor.fromFormatting(formatting);
		}

		return new Style(textColor, boolean_, boolean2, boolean4, boolean3, boolean5, this.clickEvent, this.hoverEvent, this.insertion, this.font);
	}

	/**
	 * Returns a new style with the formattings provided and all other
	 * attributes of this style.
	 * 
	 * @param formattings an array of new formattings
	 */
	public Style withFormatting(Formatting... formattings) {
		TextColor textColor = this.color;
		Boolean boolean_ = this.bold;
		Boolean boolean2 = this.italic;
		Boolean boolean3 = this.strikethrough;
		Boolean boolean4 = this.underlined;
		Boolean boolean5 = this.obfuscated;

		for (Formatting formatting : formattings) {
			switch (formatting) {
				case OBFUSCATED:
					boolean5 = true;
					break;
				case BOLD:
					boolean_ = true;
					break;
				case STRIKETHROUGH:
					boolean3 = true;
					break;
				case UNDERLINE:
					boolean4 = true;
					break;
				case ITALIC:
					boolean2 = true;
					break;
				case RESET:
					return EMPTY;
				default:
					textColor = TextColor.fromFormatting(formatting);
			}
		}

		return new Style(textColor, boolean_, boolean2, boolean4, boolean3, boolean5, this.clickEvent, this.hoverEvent, this.insertion, this.font);
	}

	/**
	 * Returns a new style with the undefined attributes of this style filled
	 * by the {@code parent} style.
	 * 
	 * @param parent the parent style
	 */
	public Style withParent(Style parent) {
		if (this == EMPTY) {
			return parent;
		} else {
			return parent == EMPTY
				? this
				: new Style(
					this.color != null ? this.color : parent.color,
					this.bold != null ? this.bold : parent.bold,
					this.italic != null ? this.italic : parent.italic,
					this.underlined != null ? this.underlined : parent.underlined,
					this.strikethrough != null ? this.strikethrough : parent.strikethrough,
					this.obfuscated != null ? this.obfuscated : parent.obfuscated,
					this.clickEvent != null ? this.clickEvent : parent.clickEvent,
					this.hoverEvent != null ? this.hoverEvent : parent.hoverEvent,
					this.insertion != null ? this.insertion : parent.insertion,
					this.font != null ? this.font : parent.font
				);
		}
	}

	public String toString() {
		final StringBuilder stringBuilder = new StringBuilder("{");

		class Writer {
			private boolean shouldAppendComma;

			private void appendComma() {
				if (this.shouldAppendComma) {
					stringBuilder.append(',');
				}

				this.shouldAppendComma = true;
			}

			void append(String key, @Nullable Boolean value) {
				if (value != null) {
					this.appendComma();
					if (!value) {
						stringBuilder.append('!');
					}

					stringBuilder.append(key);
				}
			}

			void append(String key, @Nullable Object value) {
				if (value != null) {
					this.appendComma();
					stringBuilder.append(key);
					stringBuilder.append('=');
					stringBuilder.append(value);
				}
			}
		}

		Writer writer = new Writer();
		writer.append("color", this.color);
		writer.append("bold", this.bold);
		writer.append("italic", this.italic);
		writer.append("underlined", this.underlined);
		writer.append("strikethrough", this.strikethrough);
		writer.append("obfuscated", this.obfuscated);
		writer.append("clickEvent", this.clickEvent);
		writer.append("hoverEvent", this.hoverEvent);
		writer.append("insertion", this.insertion);
		writer.append("font", this.font);
		stringBuilder.append("}");
		return stringBuilder.toString();
	}

	public boolean equals(Object o) {
		if (this == o) {
			return true;
		} else {
			return !(o instanceof Style style)
				? false
				: this.isBold() == style.isBold()
					&& Objects.equals(this.getColor(), style.getColor())
					&& this.isItalic() == style.isItalic()
					&& this.isObfuscated() == style.isObfuscated()
					&& this.isStrikethrough() == style.isStrikethrough()
					&& this.isUnderlined() == style.isUnderlined()
					&& Objects.equals(this.getClickEvent(), style.getClickEvent())
					&& Objects.equals(this.getHoverEvent(), style.getHoverEvent())
					&& Objects.equals(this.getInsertion(), style.getInsertion())
					&& Objects.equals(this.getFont(), style.getFont());
		}
	}

	public int hashCode() {
		return Objects.hash(
			new Object[]{this.color, this.bold, this.italic, this.underlined, this.strikethrough, this.obfuscated, this.clickEvent, this.hoverEvent, this.insertion}
		);
	}

	/**
	 * A JSON serializer for {@link Style}.
	 */
	public static class Serializer implements JsonDeserializer<Style>, JsonSerializer<Style> {
		@Nullable
		public Style deserialize(JsonElement jsonElement, Type type, JsonDeserializationContext jsonDeserializationContext) throws JsonParseException {
			if (jsonElement.isJsonObject()) {
				JsonObject jsonObject = jsonElement.getAsJsonObject();
				if (jsonObject == null) {
					return null;
				} else {
					Boolean boolean_ = parseNullableBoolean(jsonObject, "bold");
					Boolean boolean2 = parseNullableBoolean(jsonObject, "italic");
					Boolean boolean3 = parseNullableBoolean(jsonObject, "underlined");
					Boolean boolean4 = parseNullableBoolean(jsonObject, "strikethrough");
					Boolean boolean5 = parseNullableBoolean(jsonObject, "obfuscated");
					TextColor textColor = parseColor(jsonObject);
					String string = parseInsertion(jsonObject);
					ClickEvent clickEvent = getClickEvent(jsonObject);
					HoverEvent hoverEvent = getHoverEvent(jsonObject);
					Identifier identifier = getFont(jsonObject);
					return new Style(textColor, boolean_, boolean2, boolean3, boolean4, boolean5, clickEvent, hoverEvent, string, identifier);
				}
			} else {
				return null;
			}
		}

		@Nullable
		private static Identifier getFont(JsonObject root) {
			if (root.has("font")) {
				String string = JsonHelper.getString(root, "font");

				try {
					return new Identifier(string);
				} catch (InvalidIdentifierException var3) {
					throw new JsonSyntaxException("Invalid font name: " + string);
				}
			} else {
				return null;
			}
		}

		@Nullable
		private static HoverEvent getHoverEvent(JsonObject root) {
			if (root.has("hoverEvent")) {
				JsonObject jsonObject = JsonHelper.getObject(root, "hoverEvent");
				HoverEvent hoverEvent = HoverEvent.fromJson(jsonObject);
				if (hoverEvent != null && hoverEvent.getAction().isParsable()) {
					return hoverEvent;
				}
			}

			return null;
		}

		@Nullable
		private static ClickEvent getClickEvent(JsonObject root) {
			if (root.has("clickEvent")) {
				JsonObject jsonObject = JsonHelper.getObject(root, "clickEvent");
				String string = JsonHelper.getString(jsonObject, "action", null);
				ClickEvent.Action action = string == null ? null : ClickEvent.Action.byName(string);
				String string2 = JsonHelper.getString(jsonObject, "value", null);
				if (action != null && string2 != null && action.isUserDefinable()) {
					return new ClickEvent(action, string2);
				}
			}

			return null;
		}

		@Nullable
		private static String parseInsertion(JsonObject root) {
			return JsonHelper.getString(root, "insertion", null);
		}

		@Nullable
		private static TextColor parseColor(JsonObject root) {
			if (root.has("color")) {
				String string = JsonHelper.getString(root, "color");
				return TextColor.parse(string);
			} else {
				return null;
			}
		}

		@Nullable
		private static Boolean parseNullableBoolean(JsonObject root, String key) {
			return root.has(key) ? root.get(key).getAsBoolean() : null;
		}

		@Nullable
		public JsonElement serialize(Style style, Type type, JsonSerializationContext jsonSerializationContext) {
			if (style.isEmpty()) {
				return null;
			} else {
				JsonObject jsonObject = new JsonObject();
				if (style.bold != null) {
					jsonObject.addProperty("bold", style.bold);
				}

				if (style.italic != null) {
					jsonObject.addProperty("italic", style.italic);
				}

				if (style.underlined != null) {
					jsonObject.addProperty("underlined", style.underlined);
				}

				if (style.strikethrough != null) {
					jsonObject.addProperty("strikethrough", style.strikethrough);
				}

				if (style.obfuscated != null) {
					jsonObject.addProperty("obfuscated", style.obfuscated);
				}

				if (style.color != null) {
					jsonObject.addProperty("color", style.color.getName());
				}

				if (style.insertion != null) {
					jsonObject.add("insertion", jsonSerializationContext.serialize(style.insertion));
				}

				if (style.clickEvent != null) {
					JsonObject jsonObject2 = new JsonObject();
					jsonObject2.addProperty("action", style.clickEvent.getAction().getName());
					jsonObject2.addProperty("value", style.clickEvent.getValue());
					jsonObject.add("clickEvent", jsonObject2);
				}

				if (style.hoverEvent != null) {
					jsonObject.add("hoverEvent", style.hoverEvent.toJson());
				}

				if (style.font != null) {
					jsonObject.addProperty("font", style.font.toString());
				}

				return jsonObject;
			}
		}
	}
}
