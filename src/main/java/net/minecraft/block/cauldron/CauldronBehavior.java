package net.minecraft.block.cauldron;

import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import java.util.Map;
import java.util.function.Predicate;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.LeveledCauldronBlock;
import net.minecraft.block.ShulkerBoxBlock;
import net.minecraft.block.entity.BannerBlockEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.DyeableItem;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.ItemUsage;
import net.minecraft.item.Items;
import net.minecraft.potion.PotionUtil;
import net.minecraft.potion.Potions;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvent;
import net.minecraft.sound.SoundEvents;
import net.minecraft.stat.Stats;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.Util;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraft.world.event.GameEvent;

/**
 * Cauldron behaviors control what happens when a player interacts with
 * cauldrons using a specific item.
 * 
 * <p>To register new cauldron behaviors, you can add them to the corresponding
 * maps based on the cauldron type.
 * <div class="fabric"><table>
 * <caption>Behavior maps by cauldron type</caption>
 * <thead><tr>
 *     <th>Type</th>
 *     <th>Block</th>
 *     <th>Behavior map</th>
 * </tr></thead>
 * <tbody>
 *     <tr>
 *         <td>Empty</td>
 *         <td>{@link Blocks#CAULDRON minecraft:cauldron}</td>
 *         <td>{@link #EMPTY_CAULDRON_BEHAVIOR}</td>
 *     </tr>
 *     <tr>
 *         <td>Water</td>
 *         <td>{@link Blocks#WATER_CAULDRON minecraft:water_cauldron}</td>
 *         <td>{@link #WATER_CAULDRON_BEHAVIOR}</td>
 *     </tr>
 *     <tr>
 *         <td>Lava</td>
 *         <td>{@link Blocks#LAVA_CAULDRON minecraft:lava_cauldron}</td>
 *         <td>{@link #LAVA_CAULDRON_BEHAVIOR}</td>
 *     </tr>
 *     <tr>
 *         <td>Powder snow</td>
 *         <td>{@link Blocks#POWDER_SNOW_CAULDRON minecraft:powder_snow_cauldron}</td>
 *         <td>{@link #POWDER_SNOW_CAULDRON_BEHAVIOR}</td>
 *     </tr>
 * </tbody>
 * </table></div>
 */
public interface CauldronBehavior {
	/**
	 * The cauldron behaviors for empty cauldrons.
	 * 
	 * @see #createMap
	 */
	Map<Item, CauldronBehavior> EMPTY_CAULDRON_BEHAVIOR = createMap();
	/**
	 * The cauldron behaviors for water cauldrons.
	 * 
	 * @see #createMap
	 */
	Map<Item, CauldronBehavior> WATER_CAULDRON_BEHAVIOR = createMap();
	/**
	 * The cauldron behaviors for lava cauldrons.
	 * 
	 * @see #createMap
	 */
	Map<Item, CauldronBehavior> LAVA_CAULDRON_BEHAVIOR = createMap();
	/**
	 * The cauldron behaviors for powder snow cauldrons.
	 * 
	 * @see #createMap
	 */
	Map<Item, CauldronBehavior> POWDER_SNOW_CAULDRON_BEHAVIOR = createMap();
	/**
	 * A behavior that fills cauldrons with water.
	 * 
	 * @see #fillCauldron
	 */
	CauldronBehavior FILL_WITH_WATER = (state, world, pos, player, hand, stack) -> fillCauldron(
			world, pos, player, hand, stack, Blocks.WATER_CAULDRON.getDefaultState().with(LeveledCauldronBlock.LEVEL, Integer.valueOf(3)), SoundEvents.ITEM_BUCKET_EMPTY
		);
	/**
	 * A behavior that fills cauldrons with lava.
	 * 
	 * @see #fillCauldron
	 */
	CauldronBehavior FILL_WITH_LAVA = (state, world, pos, player, hand, stack) -> fillCauldron(
			world, pos, player, hand, stack, Blocks.LAVA_CAULDRON.getDefaultState(), SoundEvents.ITEM_BUCKET_EMPTY_LAVA
		);
	/**
	 * A behavior that fills cauldrons with powder snow.
	 * 
	 * @see #fillCauldron
	 */
	CauldronBehavior FILL_WITH_POWDER_SNOW = (state, world, pos, player, hand, stack) -> fillCauldron(
			world,
			pos,
			player,
			hand,
			stack,
			Blocks.POWDER_SNOW_CAULDRON.getDefaultState().with(LeveledCauldronBlock.LEVEL, Integer.valueOf(3)),
			SoundEvents.ITEM_BUCKET_EMPTY_POWDER_SNOW
		);
	/**
	 * A behavior that cleans dyed shulker boxes.
	 */
	CauldronBehavior CLEAN_SHULKER_BOX = (state, world, pos, player, hand, stack) -> {
		Block block = Block.getBlockFromItem(stack.getItem());
		if (!(block instanceof ShulkerBoxBlock)) {
			return ActionResult.PASS;
		} else {
			if (!world.isClient) {
				ItemStack itemStack = new ItemStack(Blocks.SHULKER_BOX);
				if (stack.hasNbt()) {
					itemStack.setNbt(stack.getNbt().copy());
				}

				player.setStackInHand(hand, itemStack);
				player.incrementStat(Stats.CLEAN_SHULKER_BOX);
				LeveledCauldronBlock.decrementFluidLevel(state, world, pos);
			}

			return ActionResult.success(world.isClient);
		}
	};
	/**
	 * A behavior that cleans banners with patterns.
	 */
	CauldronBehavior CLEAN_BANNER = (state, world, pos, player, hand, stack) -> {
		if (BannerBlockEntity.getPatternCount(stack) <= 0) {
			return ActionResult.PASS;
		} else {
			if (!world.isClient) {
				ItemStack itemStack = stack.copyWithCount(1);
				BannerBlockEntity.loadFromItemStack(itemStack);
				if (!player.getAbilities().creativeMode) {
					stack.decrement(1);
				}

				if (stack.isEmpty()) {
					player.setStackInHand(hand, itemStack);
				} else if (player.getInventory().insertStack(itemStack)) {
					player.playerScreenHandler.syncState();
				} else {
					player.dropItem(itemStack, false);
				}

				player.incrementStat(Stats.CLEAN_BANNER);
				LeveledCauldronBlock.decrementFluidLevel(state, world, pos);
			}

			return ActionResult.success(world.isClient);
		}
	};
	/**
	 * A behavior that cleans {@linkplain DyeableItem dyeable items}.
	 */
	CauldronBehavior CLEAN_DYEABLE_ITEM = (state, world, pos, player, hand, stack) -> {
		if (!(stack.getItem() instanceof DyeableItem dyeableItem)) {
			return ActionResult.PASS;
		} else if (!dyeableItem.hasColor(stack)) {
			return ActionResult.PASS;
		} else {
			if (!world.isClient) {
				dyeableItem.removeColor(stack);
				player.incrementStat(Stats.CLEAN_ARMOR);
				LeveledCauldronBlock.decrementFluidLevel(state, world, pos);
			}

			return ActionResult.success(world.isClient);
		}
	};

	/**
	 * Creates a mutable map from {@linkplain Item items} to their
	 * corresponding cauldron behaviors.
	 * 
	 * <p>The default return value in the map is a cauldron behavior
	 * that returns {@link ActionResult#PASS} for all items.
	 * 
	 * @return the created map
	 */
	static Object2ObjectOpenHashMap<Item, CauldronBehavior> createMap() {
		return Util.make(new Object2ObjectOpenHashMap<>(), map -> map.defaultReturnValue((state, world, pos, player, hand, stack) -> ActionResult.PASS));
	}

	/**
	 * Called when a player interacts with a cauldron.
	 * 
	 * @return a {@linkplain ActionResult#isAccepted successful} action result if this behavior succeeds,
	 * {@link ActionResult#PASS} otherwise
	 * 
	 * @param state the current cauldron block state
	 * @param player the interacting player
	 * @param hand the hand interacting with the cauldron
	 * @param world the world where the cauldron is located
	 * @param pos the cauldron's position
	 * @param stack the stack in the player's hand
	 */
	ActionResult interact(BlockState state, World world, BlockPos pos, PlayerEntity player, Hand hand, ItemStack stack);

	/**
	 * Registers the vanilla cauldron behaviors.
	 */
	static void registerBehavior() {
		registerBucketBehavior(EMPTY_CAULDRON_BEHAVIOR);
		EMPTY_CAULDRON_BEHAVIOR.put(Items.POTION, (CauldronBehavior)(state, world, pos, player, hand, stack) -> {
			if (PotionUtil.getPotion(stack) != Potions.WATER) {
				return ActionResult.PASS;
			} else {
				if (!world.isClient) {
					Item item = stack.getItem();
					player.setStackInHand(hand, ItemUsage.exchangeStack(stack, player, new ItemStack(Items.GLASS_BOTTLE)));
					player.incrementStat(Stats.USE_CAULDRON);
					player.incrementStat(Stats.USED.getOrCreateStat(item));
					world.setBlockState(pos, Blocks.WATER_CAULDRON.getDefaultState());
					world.playSound(null, pos, SoundEvents.ITEM_BOTTLE_EMPTY, SoundCategory.BLOCKS, 1.0F, 1.0F);
					world.emitGameEvent(null, GameEvent.FLUID_PLACE, pos);
				}

				return ActionResult.success(world.isClient);
			}
		});
		registerBucketBehavior(WATER_CAULDRON_BEHAVIOR);
		WATER_CAULDRON_BEHAVIOR.put(
			Items.BUCKET,
			(CauldronBehavior)(state, world, pos, player, hand, stack) -> emptyCauldron(
					state,
					world,
					pos,
					player,
					hand,
					stack,
					new ItemStack(Items.WATER_BUCKET),
					statex -> (Integer)statex.get(LeveledCauldronBlock.LEVEL) == 3,
					SoundEvents.ITEM_BUCKET_FILL
				)
		);
		WATER_CAULDRON_BEHAVIOR.put(Items.GLASS_BOTTLE, (CauldronBehavior)(state, world, pos, player, hand, stack) -> {
			if (!world.isClient) {
				Item item = stack.getItem();
				player.setStackInHand(hand, ItemUsage.exchangeStack(stack, player, PotionUtil.setPotion(new ItemStack(Items.POTION), Potions.WATER)));
				player.incrementStat(Stats.USE_CAULDRON);
				player.incrementStat(Stats.USED.getOrCreateStat(item));
				LeveledCauldronBlock.decrementFluidLevel(state, world, pos);
				world.playSound(null, pos, SoundEvents.ITEM_BOTTLE_FILL, SoundCategory.BLOCKS, 1.0F, 1.0F);
				world.emitGameEvent(null, GameEvent.FLUID_PICKUP, pos);
			}

			return ActionResult.success(world.isClient);
		});
		WATER_CAULDRON_BEHAVIOR.put(Items.POTION, (CauldronBehavior)(state, world, pos, player, hand, stack) -> {
			if ((Integer)state.get(LeveledCauldronBlock.LEVEL) != 3 && PotionUtil.getPotion(stack) == Potions.WATER) {
				if (!world.isClient) {
					player.setStackInHand(hand, ItemUsage.exchangeStack(stack, player, new ItemStack(Items.GLASS_BOTTLE)));
					player.incrementStat(Stats.USE_CAULDRON);
					player.incrementStat(Stats.USED.getOrCreateStat(stack.getItem()));
					world.setBlockState(pos, state.cycle(LeveledCauldronBlock.LEVEL));
					world.playSound(null, pos, SoundEvents.ITEM_BOTTLE_EMPTY, SoundCategory.BLOCKS, 1.0F, 1.0F);
					world.emitGameEvent(null, GameEvent.FLUID_PLACE, pos);
				}

				return ActionResult.success(world.isClient);
			} else {
				return ActionResult.PASS;
			}
		});
		WATER_CAULDRON_BEHAVIOR.put(Items.LEATHER_BOOTS, CLEAN_DYEABLE_ITEM);
		WATER_CAULDRON_BEHAVIOR.put(Items.LEATHER_LEGGINGS, CLEAN_DYEABLE_ITEM);
		WATER_CAULDRON_BEHAVIOR.put(Items.LEATHER_CHESTPLATE, CLEAN_DYEABLE_ITEM);
		WATER_CAULDRON_BEHAVIOR.put(Items.LEATHER_HELMET, CLEAN_DYEABLE_ITEM);
		WATER_CAULDRON_BEHAVIOR.put(Items.LEATHER_HORSE_ARMOR, CLEAN_DYEABLE_ITEM);
		WATER_CAULDRON_BEHAVIOR.put(Items.WHITE_BANNER, CLEAN_BANNER);
		WATER_CAULDRON_BEHAVIOR.put(Items.GRAY_BANNER, CLEAN_BANNER);
		WATER_CAULDRON_BEHAVIOR.put(Items.BLACK_BANNER, CLEAN_BANNER);
		WATER_CAULDRON_BEHAVIOR.put(Items.BLUE_BANNER, CLEAN_BANNER);
		WATER_CAULDRON_BEHAVIOR.put(Items.BROWN_BANNER, CLEAN_BANNER);
		WATER_CAULDRON_BEHAVIOR.put(Items.CYAN_BANNER, CLEAN_BANNER);
		WATER_CAULDRON_BEHAVIOR.put(Items.GREEN_BANNER, CLEAN_BANNER);
		WATER_CAULDRON_BEHAVIOR.put(Items.LIGHT_BLUE_BANNER, CLEAN_BANNER);
		WATER_CAULDRON_BEHAVIOR.put(Items.LIGHT_GRAY_BANNER, CLEAN_BANNER);
		WATER_CAULDRON_BEHAVIOR.put(Items.LIME_BANNER, CLEAN_BANNER);
		WATER_CAULDRON_BEHAVIOR.put(Items.MAGENTA_BANNER, CLEAN_BANNER);
		WATER_CAULDRON_BEHAVIOR.put(Items.ORANGE_BANNER, CLEAN_BANNER);
		WATER_CAULDRON_BEHAVIOR.put(Items.PINK_BANNER, CLEAN_BANNER);
		WATER_CAULDRON_BEHAVIOR.put(Items.PURPLE_BANNER, CLEAN_BANNER);
		WATER_CAULDRON_BEHAVIOR.put(Items.RED_BANNER, CLEAN_BANNER);
		WATER_CAULDRON_BEHAVIOR.put(Items.YELLOW_BANNER, CLEAN_BANNER);
		WATER_CAULDRON_BEHAVIOR.put(Items.WHITE_SHULKER_BOX, CLEAN_SHULKER_BOX);
		WATER_CAULDRON_BEHAVIOR.put(Items.GRAY_SHULKER_BOX, CLEAN_SHULKER_BOX);
		WATER_CAULDRON_BEHAVIOR.put(Items.BLACK_SHULKER_BOX, CLEAN_SHULKER_BOX);
		WATER_CAULDRON_BEHAVIOR.put(Items.BLUE_SHULKER_BOX, CLEAN_SHULKER_BOX);
		WATER_CAULDRON_BEHAVIOR.put(Items.BROWN_SHULKER_BOX, CLEAN_SHULKER_BOX);
		WATER_CAULDRON_BEHAVIOR.put(Items.CYAN_SHULKER_BOX, CLEAN_SHULKER_BOX);
		WATER_CAULDRON_BEHAVIOR.put(Items.GREEN_SHULKER_BOX, CLEAN_SHULKER_BOX);
		WATER_CAULDRON_BEHAVIOR.put(Items.LIGHT_BLUE_SHULKER_BOX, CLEAN_SHULKER_BOX);
		WATER_CAULDRON_BEHAVIOR.put(Items.LIGHT_GRAY_SHULKER_BOX, CLEAN_SHULKER_BOX);
		WATER_CAULDRON_BEHAVIOR.put(Items.LIME_SHULKER_BOX, CLEAN_SHULKER_BOX);
		WATER_CAULDRON_BEHAVIOR.put(Items.MAGENTA_SHULKER_BOX, CLEAN_SHULKER_BOX);
		WATER_CAULDRON_BEHAVIOR.put(Items.ORANGE_SHULKER_BOX, CLEAN_SHULKER_BOX);
		WATER_CAULDRON_BEHAVIOR.put(Items.PINK_SHULKER_BOX, CLEAN_SHULKER_BOX);
		WATER_CAULDRON_BEHAVIOR.put(Items.PURPLE_SHULKER_BOX, CLEAN_SHULKER_BOX);
		WATER_CAULDRON_BEHAVIOR.put(Items.RED_SHULKER_BOX, CLEAN_SHULKER_BOX);
		WATER_CAULDRON_BEHAVIOR.put(Items.YELLOW_SHULKER_BOX, CLEAN_SHULKER_BOX);
		LAVA_CAULDRON_BEHAVIOR.put(
			Items.BUCKET,
			(CauldronBehavior)(state, world, pos, player, hand, stack) -> emptyCauldron(
					state, world, pos, player, hand, stack, new ItemStack(Items.LAVA_BUCKET), statex -> true, SoundEvents.ITEM_BUCKET_FILL_LAVA
				)
		);
		registerBucketBehavior(LAVA_CAULDRON_BEHAVIOR);
		POWDER_SNOW_CAULDRON_BEHAVIOR.put(
			Items.BUCKET,
			(CauldronBehavior)(state, world, pos, player, hand, stack) -> emptyCauldron(
					state,
					world,
					pos,
					player,
					hand,
					stack,
					new ItemStack(Items.POWDER_SNOW_BUCKET),
					statex -> (Integer)statex.get(LeveledCauldronBlock.LEVEL) == 3,
					SoundEvents.ITEM_BUCKET_FILL_POWDER_SNOW
				)
		);
		registerBucketBehavior(POWDER_SNOW_CAULDRON_BEHAVIOR);
	}

	/**
	 * Registers the behavior for filled buckets in the specified behavior map.
	 */
	static void registerBucketBehavior(Map<Item, CauldronBehavior> behavior) {
		behavior.put(Items.LAVA_BUCKET, FILL_WITH_LAVA);
		behavior.put(Items.WATER_BUCKET, FILL_WITH_WATER);
		behavior.put(Items.POWDER_SNOW_BUCKET, FILL_WITH_POWDER_SNOW);
	}

	/**
	 * Empties a cauldron if it's full.
	 * 
	 * @return a {@linkplain ActionResult#isAccepted successful} action result if emptied, {@link ActionResult#PASS} otherwise
	 * 
	 * @param soundEvent the sound produced by emptying
	 * @param fullPredicate a predicate used to check if the cauldron can be emptied into the output stack
	 * @param output the item stack that replaces the interaction stack when the cauldron is emptied
	 * @param stack the stack in the player's hand
	 * @param hand the hand interacting with the cauldron
	 * @param player the interacting player
	 * @param pos the cauldron's position
	 * @param world the world where the cauldron is located
	 * @param state the cauldron block state
	 */
	static ActionResult emptyCauldron(
		BlockState state,
		World world,
		BlockPos pos,
		PlayerEntity player,
		Hand hand,
		ItemStack stack,
		ItemStack output,
		Predicate<BlockState> fullPredicate,
		SoundEvent soundEvent
	) {
		if (!fullPredicate.test(state)) {
			return ActionResult.PASS;
		} else {
			if (!world.isClient) {
				Item item = stack.getItem();
				player.setStackInHand(hand, ItemUsage.exchangeStack(stack, player, output));
				player.incrementStat(Stats.USE_CAULDRON);
				player.incrementStat(Stats.USED.getOrCreateStat(item));
				world.setBlockState(pos, Blocks.CAULDRON.getDefaultState());
				world.playSound(null, pos, soundEvent, SoundCategory.BLOCKS, 1.0F, 1.0F);
				world.emitGameEvent(null, GameEvent.FLUID_PICKUP, pos);
			}

			return ActionResult.success(world.isClient);
		}
	}

	/**
	 * Fills a cauldron from a bucket stack.
	 * 
	 * <p>The filled bucket stack will be replaced by an empty bucket in the player's
	 * inventory.
	 * 
	 * @return a {@linkplain ActionResult#isAccepted successful} action result
	 * 
	 * @param pos the cauldron's position
	 * @param world the world where the cauldron is located
	 * @param soundEvent the sound produced by filling
	 * @param hand the hand interacting with the cauldron
	 * @param player the interacting player
	 * @param state the filled cauldron state
	 * @param stack the filled bucket stack in the player's hand
	 */
	static ActionResult fillCauldron(World world, BlockPos pos, PlayerEntity player, Hand hand, ItemStack stack, BlockState state, SoundEvent soundEvent) {
		if (!world.isClient) {
			Item item = stack.getItem();
			player.setStackInHand(hand, ItemUsage.exchangeStack(stack, player, new ItemStack(Items.BUCKET)));
			player.incrementStat(Stats.FILL_CAULDRON);
			player.incrementStat(Stats.USED.getOrCreateStat(item));
			world.setBlockState(pos, state);
			world.playSound(null, pos, soundEvent, SoundCategory.BLOCKS, 1.0F, 1.0F);
			world.emitGameEvent(null, GameEvent.FLUID_PLACE, pos);
		}

		return ActionResult.success(world.isClient);
	}
}
