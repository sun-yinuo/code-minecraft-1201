package net.minecraft.datafixer.fix;

import com.mojang.datafixers.DataFixUtils;
import com.mojang.logging.LogUtils;
import com.mojang.serialization.Dynamic;
import it.unimi.dsi.fastutil.objects.Object2IntMap;
import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.StringNbtReader;
import org.slf4j.Logger;

public class BlockStateFlattening {
	private static final Logger LOGGER = LogUtils.getLogger();
	private static final Dynamic<?>[] OLD_STATE_TO_DYNAMIC = new Dynamic[4096];
	private static final Dynamic<?>[] OLD_BLOCK_TO_DYNAMIC = new Dynamic[256];
	private static final Object2IntMap<Dynamic<?>> OLD_STATE_TO_ID = DataFixUtils.make(
		new Object2IntOpenHashMap<>(), object2IntOpenHashMap -> object2IntOpenHashMap.defaultReturnValue(-1)
	);
	private static final Object2IntMap<String> OLD_BLOCK_TO_ID = DataFixUtils.make(
		new Object2IntOpenHashMap<>(), object2IntOpenHashMap -> object2IntOpenHashMap.defaultReturnValue(-1)
	);
	static final String FILTER_ME = "%%FILTER_ME%%";

	private static void putStates(int oldId, String newStateStr, String... oldStateStrings) {
		Dynamic<?> dynamic = parseState(newStateStr);
		OLD_STATE_TO_DYNAMIC[oldId] = dynamic;
		int i = oldId >> 4;
		if (OLD_BLOCK_TO_DYNAMIC[i] == null) {
			OLD_BLOCK_TO_DYNAMIC[i] = dynamic;
		}

		for (String string : oldStateStrings) {
			Dynamic<?> dynamic2 = parseState(string);
			String string2 = dynamic2.get("Name").asString("");
			OLD_BLOCK_TO_ID.putIfAbsent(string2, oldId);
			OLD_STATE_TO_ID.put(dynamic2, oldId);
		}
	}

	private static void fillEmptyStates() {
		for (int i = 0; i < OLD_STATE_TO_DYNAMIC.length; i++) {
			if (OLD_STATE_TO_DYNAMIC[i] == null) {
				OLD_STATE_TO_DYNAMIC[i] = OLD_BLOCK_TO_DYNAMIC[i >> 4];
			}
		}
	}

	public static Dynamic<?> lookupState(Dynamic<?> dynamic) {
		int i = OLD_STATE_TO_ID.getInt(dynamic);
		if (i >= 0 && i < OLD_STATE_TO_DYNAMIC.length) {
			Dynamic<?> dynamic2 = OLD_STATE_TO_DYNAMIC[i];
			return dynamic2 == null ? dynamic : dynamic2;
		} else {
			return dynamic;
		}
	}

	public static String lookupBlock(String oldBlockName) {
		int i = OLD_BLOCK_TO_ID.getInt(oldBlockName);
		if (i >= 0 && i < OLD_STATE_TO_DYNAMIC.length) {
			Dynamic<?> dynamic = OLD_STATE_TO_DYNAMIC[i];
			return dynamic == null ? oldBlockName : dynamic.get("Name").asString("");
		} else {
			return oldBlockName;
		}
	}

	public static String lookupStateBlock(int stateId) {
		if (stateId >= 0 && stateId < OLD_STATE_TO_DYNAMIC.length) {
			Dynamic<?> dynamic = OLD_STATE_TO_DYNAMIC[stateId];
			return dynamic == null ? "minecraft:air" : dynamic.get("Name").asString("");
		} else {
			return "minecraft:air";
		}
	}

	public static Dynamic<?> parseState(String stateStr) {
		try {
			return new Dynamic<>(NbtOps.INSTANCE, StringNbtReader.parse(stateStr.replace('\'', '"')));
		} catch (Exception var2) {
			LOGGER.error("Parsing {}", stateStr, var2);
			throw new RuntimeException(var2);
		}
	}

	public static Dynamic<?> lookupState(int stateId) {
		Dynamic<?> dynamic = null;
		if (stateId >= 0 && stateId < OLD_STATE_TO_DYNAMIC.length) {
			dynamic = OLD_STATE_TO_DYNAMIC[stateId];
		}

		return dynamic == null ? OLD_STATE_TO_DYNAMIC[0] : dynamic;
	}

	/**
	 * Adds states to flatten from the blocks which had numeric IDs between 0 and 15 before 1.13.
	 */
	static void putStatesFromBlocks0To15() {
		putStates(0, "{Name:'minecraft:air'}", "{Name:'minecraft:air'}");
		putStates(16, "{Name:'minecraft:stone'}", "{Name:'minecraft:stone',Properties:{variant:'stone'}}");
		putStates(17, "{Name:'minecraft:granite'}", "{Name:'minecraft:stone',Properties:{variant:'granite'}}");
		putStates(18, "{Name:'minecraft:polished_granite'}", "{Name:'minecraft:stone',Properties:{variant:'smooth_granite'}}");
		putStates(19, "{Name:'minecraft:diorite'}", "{Name:'minecraft:stone',Properties:{variant:'diorite'}}");
		putStates(20, "{Name:'minecraft:polished_diorite'}", "{Name:'minecraft:stone',Properties:{variant:'smooth_diorite'}}");
		putStates(21, "{Name:'minecraft:andesite'}", "{Name:'minecraft:stone',Properties:{variant:'andesite'}}");
		putStates(22, "{Name:'minecraft:polished_andesite'}", "{Name:'minecraft:stone',Properties:{variant:'smooth_andesite'}}");
		putStates(
			32,
			"{Name:'minecraft:grass_block',Properties:{snowy:'false'}}",
			"{Name:'minecraft:grass',Properties:{snowy:'false'}}",
			"{Name:'minecraft:grass',Properties:{snowy:'true'}}"
		);
		putStates(
			48,
			"{Name:'minecraft:dirt'}",
			"{Name:'minecraft:dirt',Properties:{snowy:'false',variant:'dirt'}}",
			"{Name:'minecraft:dirt',Properties:{snowy:'true',variant:'dirt'}}"
		);
		putStates(
			49,
			"{Name:'minecraft:coarse_dirt'}",
			"{Name:'minecraft:dirt',Properties:{snowy:'false',variant:'coarse_dirt'}}",
			"{Name:'minecraft:dirt',Properties:{snowy:'true',variant:'coarse_dirt'}}"
		);
		putStates(
			50,
			"{Name:'minecraft:podzol',Properties:{snowy:'false'}}",
			"{Name:'minecraft:dirt',Properties:{snowy:'false',variant:'podzol'}}",
			"{Name:'minecraft:dirt',Properties:{snowy:'true',variant:'podzol'}}"
		);
		putStates(64, "{Name:'minecraft:cobblestone'}", "{Name:'minecraft:cobblestone'}");
		putStates(80, "{Name:'minecraft:oak_planks'}", "{Name:'minecraft:planks',Properties:{variant:'oak'}}");
		putStates(81, "{Name:'minecraft:spruce_planks'}", "{Name:'minecraft:planks',Properties:{variant:'spruce'}}");
		putStates(82, "{Name:'minecraft:birch_planks'}", "{Name:'minecraft:planks',Properties:{variant:'birch'}}");
		putStates(83, "{Name:'minecraft:jungle_planks'}", "{Name:'minecraft:planks',Properties:{variant:'jungle'}}");
		putStates(84, "{Name:'minecraft:acacia_planks'}", "{Name:'minecraft:planks',Properties:{variant:'acacia'}}");
		putStates(85, "{Name:'minecraft:dark_oak_planks'}", "{Name:'minecraft:planks',Properties:{variant:'dark_oak'}}");
		putStates(96, "{Name:'minecraft:oak_sapling',Properties:{stage:'0'}}", "{Name:'minecraft:sapling',Properties:{stage:'0',type:'oak'}}");
		putStates(97, "{Name:'minecraft:spruce_sapling',Properties:{stage:'0'}}", "{Name:'minecraft:sapling',Properties:{stage:'0',type:'spruce'}}");
		putStates(98, "{Name:'minecraft:birch_sapling',Properties:{stage:'0'}}", "{Name:'minecraft:sapling',Properties:{stage:'0',type:'birch'}}");
		putStates(99, "{Name:'minecraft:jungle_sapling',Properties:{stage:'0'}}", "{Name:'minecraft:sapling',Properties:{stage:'0',type:'jungle'}}");
		putStates(100, "{Name:'minecraft:acacia_sapling',Properties:{stage:'0'}}", "{Name:'minecraft:sapling',Properties:{stage:'0',type:'acacia'}}");
		putStates(101, "{Name:'minecraft:dark_oak_sapling',Properties:{stage:'0'}}", "{Name:'minecraft:sapling',Properties:{stage:'0',type:'dark_oak'}}");
		putStates(104, "{Name:'minecraft:oak_sapling',Properties:{stage:'1'}}", "{Name:'minecraft:sapling',Properties:{stage:'1',type:'oak'}}");
		putStates(105, "{Name:'minecraft:spruce_sapling',Properties:{stage:'1'}}", "{Name:'minecraft:sapling',Properties:{stage:'1',type:'spruce'}}");
		putStates(106, "{Name:'minecraft:birch_sapling',Properties:{stage:'1'}}", "{Name:'minecraft:sapling',Properties:{stage:'1',type:'birch'}}");
		putStates(107, "{Name:'minecraft:jungle_sapling',Properties:{stage:'1'}}", "{Name:'minecraft:sapling',Properties:{stage:'1',type:'jungle'}}");
		putStates(108, "{Name:'minecraft:acacia_sapling',Properties:{stage:'1'}}", "{Name:'minecraft:sapling',Properties:{stage:'1',type:'acacia'}}");
		putStates(109, "{Name:'minecraft:dark_oak_sapling',Properties:{stage:'1'}}", "{Name:'minecraft:sapling',Properties:{stage:'1',type:'dark_oak'}}");
		putStates(112, "{Name:'minecraft:bedrock'}", "{Name:'minecraft:bedrock'}");
		putStates(128, "{Name:'minecraft:water',Properties:{level:'0'}}", "{Name:'minecraft:flowing_water',Properties:{level:'0'}}");
		putStates(129, "{Name:'minecraft:water',Properties:{level:'1'}}", "{Name:'minecraft:flowing_water',Properties:{level:'1'}}");
		putStates(130, "{Name:'minecraft:water',Properties:{level:'2'}}", "{Name:'minecraft:flowing_water',Properties:{level:'2'}}");
		putStates(131, "{Name:'minecraft:water',Properties:{level:'3'}}", "{Name:'minecraft:flowing_water',Properties:{level:'3'}}");
		putStates(132, "{Name:'minecraft:water',Properties:{level:'4'}}", "{Name:'minecraft:flowing_water',Properties:{level:'4'}}");
		putStates(133, "{Name:'minecraft:water',Properties:{level:'5'}}", "{Name:'minecraft:flowing_water',Properties:{level:'5'}}");
		putStates(134, "{Name:'minecraft:water',Properties:{level:'6'}}", "{Name:'minecraft:flowing_water',Properties:{level:'6'}}");
		putStates(135, "{Name:'minecraft:water',Properties:{level:'7'}}", "{Name:'minecraft:flowing_water',Properties:{level:'7'}}");
		putStates(136, "{Name:'minecraft:water',Properties:{level:'8'}}", "{Name:'minecraft:flowing_water',Properties:{level:'8'}}");
		putStates(137, "{Name:'minecraft:water',Properties:{level:'9'}}", "{Name:'minecraft:flowing_water',Properties:{level:'9'}}");
		putStates(138, "{Name:'minecraft:water',Properties:{level:'10'}}", "{Name:'minecraft:flowing_water',Properties:{level:'10'}}");
		putStates(139, "{Name:'minecraft:water',Properties:{level:'11'}}", "{Name:'minecraft:flowing_water',Properties:{level:'11'}}");
		putStates(140, "{Name:'minecraft:water',Properties:{level:'12'}}", "{Name:'minecraft:flowing_water',Properties:{level:'12'}}");
		putStates(141, "{Name:'minecraft:water',Properties:{level:'13'}}", "{Name:'minecraft:flowing_water',Properties:{level:'13'}}");
		putStates(142, "{Name:'minecraft:water',Properties:{level:'14'}}", "{Name:'minecraft:flowing_water',Properties:{level:'14'}}");
		putStates(143, "{Name:'minecraft:water',Properties:{level:'15'}}", "{Name:'minecraft:flowing_water',Properties:{level:'15'}}");
		putStates(144, "{Name:'minecraft:water',Properties:{level:'0'}}", "{Name:'minecraft:water',Properties:{level:'0'}}");
		putStates(145, "{Name:'minecraft:water',Properties:{level:'1'}}", "{Name:'minecraft:water',Properties:{level:'1'}}");
		putStates(146, "{Name:'minecraft:water',Properties:{level:'2'}}", "{Name:'minecraft:water',Properties:{level:'2'}}");
		putStates(147, "{Name:'minecraft:water',Properties:{level:'3'}}", "{Name:'minecraft:water',Properties:{level:'3'}}");
		putStates(148, "{Name:'minecraft:water',Properties:{level:'4'}}", "{Name:'minecraft:water',Properties:{level:'4'}}");
		putStates(149, "{Name:'minecraft:water',Properties:{level:'5'}}", "{Name:'minecraft:water',Properties:{level:'5'}}");
		putStates(150, "{Name:'minecraft:water',Properties:{level:'6'}}", "{Name:'minecraft:water',Properties:{level:'6'}}");
		putStates(151, "{Name:'minecraft:water',Properties:{level:'7'}}", "{Name:'minecraft:water',Properties:{level:'7'}}");
		putStates(152, "{Name:'minecraft:water',Properties:{level:'8'}}", "{Name:'minecraft:water',Properties:{level:'8'}}");
		putStates(153, "{Name:'minecraft:water',Properties:{level:'9'}}", "{Name:'minecraft:water',Properties:{level:'9'}}");
		putStates(154, "{Name:'minecraft:water',Properties:{level:'10'}}", "{Name:'minecraft:water',Properties:{level:'10'}}");
		putStates(155, "{Name:'minecraft:water',Properties:{level:'11'}}", "{Name:'minecraft:water',Properties:{level:'11'}}");
		putStates(156, "{Name:'minecraft:water',Properties:{level:'12'}}", "{Name:'minecraft:water',Properties:{level:'12'}}");
		putStates(157, "{Name:'minecraft:water',Properties:{level:'13'}}", "{Name:'minecraft:water',Properties:{level:'13'}}");
		putStates(158, "{Name:'minecraft:water',Properties:{level:'14'}}", "{Name:'minecraft:water',Properties:{level:'14'}}");
		putStates(159, "{Name:'minecraft:water',Properties:{level:'15'}}", "{Name:'minecraft:water',Properties:{level:'15'}}");
		putStates(160, "{Name:'minecraft:lava',Properties:{level:'0'}}", "{Name:'minecraft:flowing_lava',Properties:{level:'0'}}");
		putStates(161, "{Name:'minecraft:lava',Properties:{level:'1'}}", "{Name:'minecraft:flowing_lava',Properties:{level:'1'}}");
		putStates(162, "{Name:'minecraft:lava',Properties:{level:'2'}}", "{Name:'minecraft:flowing_lava',Properties:{level:'2'}}");
		putStates(163, "{Name:'minecraft:lava',Properties:{level:'3'}}", "{Name:'minecraft:flowing_lava',Properties:{level:'3'}}");
		putStates(164, "{Name:'minecraft:lava',Properties:{level:'4'}}", "{Name:'minecraft:flowing_lava',Properties:{level:'4'}}");
		putStates(165, "{Name:'minecraft:lava',Properties:{level:'5'}}", "{Name:'minecraft:flowing_lava',Properties:{level:'5'}}");
		putStates(166, "{Name:'minecraft:lava',Properties:{level:'6'}}", "{Name:'minecraft:flowing_lava',Properties:{level:'6'}}");
		putStates(167, "{Name:'minecraft:lava',Properties:{level:'7'}}", "{Name:'minecraft:flowing_lava',Properties:{level:'7'}}");
		putStates(168, "{Name:'minecraft:lava',Properties:{level:'8'}}", "{Name:'minecraft:flowing_lava',Properties:{level:'8'}}");
		putStates(169, "{Name:'minecraft:lava',Properties:{level:'9'}}", "{Name:'minecraft:flowing_lava',Properties:{level:'9'}}");
		putStates(170, "{Name:'minecraft:lava',Properties:{level:'10'}}", "{Name:'minecraft:flowing_lava',Properties:{level:'10'}}");
		putStates(171, "{Name:'minecraft:lava',Properties:{level:'11'}}", "{Name:'minecraft:flowing_lava',Properties:{level:'11'}}");
		putStates(172, "{Name:'minecraft:lava',Properties:{level:'12'}}", "{Name:'minecraft:flowing_lava',Properties:{level:'12'}}");
		putStates(173, "{Name:'minecraft:lava',Properties:{level:'13'}}", "{Name:'minecraft:flowing_lava',Properties:{level:'13'}}");
		putStates(174, "{Name:'minecraft:lava',Properties:{level:'14'}}", "{Name:'minecraft:flowing_lava',Properties:{level:'14'}}");
		putStates(175, "{Name:'minecraft:lava',Properties:{level:'15'}}", "{Name:'minecraft:flowing_lava',Properties:{level:'15'}}");
		putStates(176, "{Name:'minecraft:lava',Properties:{level:'0'}}", "{Name:'minecraft:lava',Properties:{level:'0'}}");
		putStates(177, "{Name:'minecraft:lava',Properties:{level:'1'}}", "{Name:'minecraft:lava',Properties:{level:'1'}}");
		putStates(178, "{Name:'minecraft:lava',Properties:{level:'2'}}", "{Name:'minecraft:lava',Properties:{level:'2'}}");
		putStates(179, "{Name:'minecraft:lava',Properties:{level:'3'}}", "{Name:'minecraft:lava',Properties:{level:'3'}}");
		putStates(180, "{Name:'minecraft:lava',Properties:{level:'4'}}", "{Name:'minecraft:lava',Properties:{level:'4'}}");
		putStates(181, "{Name:'minecraft:lava',Properties:{level:'5'}}", "{Name:'minecraft:lava',Properties:{level:'5'}}");
		putStates(182, "{Name:'minecraft:lava',Properties:{level:'6'}}", "{Name:'minecraft:lava',Properties:{level:'6'}}");
		putStates(183, "{Name:'minecraft:lava',Properties:{level:'7'}}", "{Name:'minecraft:lava',Properties:{level:'7'}}");
		putStates(184, "{Name:'minecraft:lava',Properties:{level:'8'}}", "{Name:'minecraft:lava',Properties:{level:'8'}}");
		putStates(185, "{Name:'minecraft:lava',Properties:{level:'9'}}", "{Name:'minecraft:lava',Properties:{level:'9'}}");
		putStates(186, "{Name:'minecraft:lava',Properties:{level:'10'}}", "{Name:'minecraft:lava',Properties:{level:'10'}}");
		putStates(187, "{Name:'minecraft:lava',Properties:{level:'11'}}", "{Name:'minecraft:lava',Properties:{level:'11'}}");
		putStates(188, "{Name:'minecraft:lava',Properties:{level:'12'}}", "{Name:'minecraft:lava',Properties:{level:'12'}}");
		putStates(189, "{Name:'minecraft:lava',Properties:{level:'13'}}", "{Name:'minecraft:lava',Properties:{level:'13'}}");
		putStates(190, "{Name:'minecraft:lava',Properties:{level:'14'}}", "{Name:'minecraft:lava',Properties:{level:'14'}}");
		putStates(191, "{Name:'minecraft:lava',Properties:{level:'15'}}", "{Name:'minecraft:lava',Properties:{level:'15'}}");
		putStates(192, "{Name:'minecraft:sand'}", "{Name:'minecraft:sand',Properties:{variant:'sand'}}");
		putStates(193, "{Name:'minecraft:red_sand'}", "{Name:'minecraft:sand',Properties:{variant:'red_sand'}}");
		putStates(208, "{Name:'minecraft:gravel'}", "{Name:'minecraft:gravel'}");
		putStates(224, "{Name:'minecraft:gold_ore'}", "{Name:'minecraft:gold_ore'}");
		putStates(240, "{Name:'minecraft:iron_ore'}", "{Name:'minecraft:iron_ore'}");
	}

	/**
	 * Adds states to flatten from the blocks which had numeric IDs between 16 and 31 before 1.13.
	 */
	static void putStatesFromBlocks16To31() {
		putStates(256, "{Name:'minecraft:coal_ore'}", "{Name:'minecraft:coal_ore'}");
		putStates(272, "{Name:'minecraft:oak_log',Properties:{axis:'y'}}", "{Name:'minecraft:log',Properties:{axis:'y',variant:'oak'}}");
		putStates(273, "{Name:'minecraft:spruce_log',Properties:{axis:'y'}}", "{Name:'minecraft:log',Properties:{axis:'y',variant:'spruce'}}");
		putStates(274, "{Name:'minecraft:birch_log',Properties:{axis:'y'}}", "{Name:'minecraft:log',Properties:{axis:'y',variant:'birch'}}");
		putStates(275, "{Name:'minecraft:jungle_log',Properties:{axis:'y'}}", "{Name:'minecraft:log',Properties:{axis:'y',variant:'jungle'}}");
		putStates(276, "{Name:'minecraft:oak_log',Properties:{axis:'x'}}", "{Name:'minecraft:log',Properties:{axis:'x',variant:'oak'}}");
		putStates(277, "{Name:'minecraft:spruce_log',Properties:{axis:'x'}}", "{Name:'minecraft:log',Properties:{axis:'x',variant:'spruce'}}");
		putStates(278, "{Name:'minecraft:birch_log',Properties:{axis:'x'}}", "{Name:'minecraft:log',Properties:{axis:'x',variant:'birch'}}");
		putStates(279, "{Name:'minecraft:jungle_log',Properties:{axis:'x'}}", "{Name:'minecraft:log',Properties:{axis:'x',variant:'jungle'}}");
		putStates(280, "{Name:'minecraft:oak_log',Properties:{axis:'z'}}", "{Name:'minecraft:log',Properties:{axis:'z',variant:'oak'}}");
		putStates(281, "{Name:'minecraft:spruce_log',Properties:{axis:'z'}}", "{Name:'minecraft:log',Properties:{axis:'z',variant:'spruce'}}");
		putStates(282, "{Name:'minecraft:birch_log',Properties:{axis:'z'}}", "{Name:'minecraft:log',Properties:{axis:'z',variant:'birch'}}");
		putStates(283, "{Name:'minecraft:jungle_log',Properties:{axis:'z'}}", "{Name:'minecraft:log',Properties:{axis:'z',variant:'jungle'}}");
		putStates(284, "{Name:'minecraft:oak_bark'}", "{Name:'minecraft:log',Properties:{axis:'none',variant:'oak'}}");
		putStates(285, "{Name:'minecraft:spruce_bark'}", "{Name:'minecraft:log',Properties:{axis:'none',variant:'spruce'}}");
		putStates(286, "{Name:'minecraft:birch_bark'}", "{Name:'minecraft:log',Properties:{axis:'none',variant:'birch'}}");
		putStates(287, "{Name:'minecraft:jungle_bark'}", "{Name:'minecraft:log',Properties:{axis:'none',variant:'jungle'}}");
		putStates(
			288,
			"{Name:'minecraft:oak_leaves',Properties:{check_decay:'false',decayable:'true'}}",
			"{Name:'minecraft:leaves',Properties:{check_decay:'false',decayable:'true',variant:'oak'}}"
		);
		putStates(
			289,
			"{Name:'minecraft:spruce_leaves',Properties:{check_decay:'false',decayable:'true'}}",
			"{Name:'minecraft:leaves',Properties:{check_decay:'false',decayable:'true',variant:'spruce'}}"
		);
		putStates(
			290,
			"{Name:'minecraft:birch_leaves',Properties:{check_decay:'false',decayable:'true'}}",
			"{Name:'minecraft:leaves',Properties:{check_decay:'false',decayable:'true',variant:'birch'}}"
		);
		putStates(
			291,
			"{Name:'minecraft:jungle_leaves',Properties:{check_decay:'false',decayable:'true'}}",
			"{Name:'minecraft:leaves',Properties:{check_decay:'false',decayable:'true',variant:'jungle'}}"
		);
		putStates(
			292,
			"{Name:'minecraft:oak_leaves',Properties:{check_decay:'false',decayable:'false'}}",
			"{Name:'minecraft:leaves',Properties:{check_decay:'false',decayable:'false',variant:'oak'}}"
		);
		putStates(
			293,
			"{Name:'minecraft:spruce_leaves',Properties:{check_decay:'false',decayable:'false'}}",
			"{Name:'minecraft:leaves',Properties:{check_decay:'false',decayable:'false',variant:'spruce'}}"
		);
		putStates(
			294,
			"{Name:'minecraft:birch_leaves',Properties:{check_decay:'false',decayable:'false'}}",
			"{Name:'minecraft:leaves',Properties:{check_decay:'false',decayable:'false',variant:'birch'}}"
		);
		putStates(
			295,
			"{Name:'minecraft:jungle_leaves',Properties:{check_decay:'false',decayable:'false'}}",
			"{Name:'minecraft:leaves',Properties:{check_decay:'false',decayable:'false',variant:'jungle'}}"
		);
		putStates(
			296,
			"{Name:'minecraft:oak_leaves',Properties:{check_decay:'true',decayable:'true'}}",
			"{Name:'minecraft:leaves',Properties:{check_decay:'true',decayable:'true',variant:'oak'}}"
		);
		putStates(
			297,
			"{Name:'minecraft:spruce_leaves',Properties:{check_decay:'true',decayable:'true'}}",
			"{Name:'minecraft:leaves',Properties:{check_decay:'true',decayable:'true',variant:'spruce'}}"
		);
		putStates(
			298,
			"{Name:'minecraft:birch_leaves',Properties:{check_decay:'true',decayable:'true'}}",
			"{Name:'minecraft:leaves',Properties:{check_decay:'true',decayable:'true',variant:'birch'}}"
		);
		putStates(
			299,
			"{Name:'minecraft:jungle_leaves',Properties:{check_decay:'true',decayable:'true'}}",
			"{Name:'minecraft:leaves',Properties:{check_decay:'true',decayable:'true',variant:'jungle'}}"
		);
		putStates(
			300,
			"{Name:'minecraft:oak_leaves',Properties:{check_decay:'true',decayable:'false'}}",
			"{Name:'minecraft:leaves',Properties:{check_decay:'true',decayable:'false',variant:'oak'}}"
		);
		putStates(
			301,
			"{Name:'minecraft:spruce_leaves',Properties:{check_decay:'true',decayable:'false'}}",
			"{Name:'minecraft:leaves',Properties:{check_decay:'true',decayable:'false',variant:'spruce'}}"
		);
		putStates(
			302,
			"{Name:'minecraft:birch_leaves',Properties:{check_decay:'true',decayable:'false'}}",
			"{Name:'minecraft:leaves',Properties:{check_decay:'true',decayable:'false',variant:'birch'}}"
		);
		putStates(
			303,
			"{Name:'minecraft:jungle_leaves',Properties:{check_decay:'true',decayable:'false'}}",
			"{Name:'minecraft:leaves',Properties:{check_decay:'true',decayable:'false',variant:'jungle'}}"
		);
		putStates(304, "{Name:'minecraft:sponge'}", "{Name:'minecraft:sponge',Properties:{wet:'false'}}");
		putStates(305, "{Name:'minecraft:wet_sponge'}", "{Name:'minecraft:sponge',Properties:{wet:'true'}}");
		putStates(320, "{Name:'minecraft:glass'}", "{Name:'minecraft:glass'}");
		putStates(336, "{Name:'minecraft:lapis_ore'}", "{Name:'minecraft:lapis_ore'}");
		putStates(352, "{Name:'minecraft:lapis_block'}", "{Name:'minecraft:lapis_block'}");
		putStates(
			368,
			"{Name:'minecraft:dispenser',Properties:{facing:'down',triggered:'false'}}",
			"{Name:'minecraft:dispenser',Properties:{facing:'down',triggered:'false'}}"
		);
		putStates(
			369, "{Name:'minecraft:dispenser',Properties:{facing:'up',triggered:'false'}}", "{Name:'minecraft:dispenser',Properties:{facing:'up',triggered:'false'}}"
		);
		putStates(
			370,
			"{Name:'minecraft:dispenser',Properties:{facing:'north',triggered:'false'}}",
			"{Name:'minecraft:dispenser',Properties:{facing:'north',triggered:'false'}}"
		);
		putStates(
			371,
			"{Name:'minecraft:dispenser',Properties:{facing:'south',triggered:'false'}}",
			"{Name:'minecraft:dispenser',Properties:{facing:'south',triggered:'false'}}"
		);
		putStates(
			372,
			"{Name:'minecraft:dispenser',Properties:{facing:'west',triggered:'false'}}",
			"{Name:'minecraft:dispenser',Properties:{facing:'west',triggered:'false'}}"
		);
		putStates(
			373,
			"{Name:'minecraft:dispenser',Properties:{facing:'east',triggered:'false'}}",
			"{Name:'minecraft:dispenser',Properties:{facing:'east',triggered:'false'}}"
		);
		putStates(
			376, "{Name:'minecraft:dispenser',Properties:{facing:'down',triggered:'true'}}", "{Name:'minecraft:dispenser',Properties:{facing:'down',triggered:'true'}}"
		);
		putStates(
			377, "{Name:'minecraft:dispenser',Properties:{facing:'up',triggered:'true'}}", "{Name:'minecraft:dispenser',Properties:{facing:'up',triggered:'true'}}"
		);
		putStates(
			378,
			"{Name:'minecraft:dispenser',Properties:{facing:'north',triggered:'true'}}",
			"{Name:'minecraft:dispenser',Properties:{facing:'north',triggered:'true'}}"
		);
		putStates(
			379,
			"{Name:'minecraft:dispenser',Properties:{facing:'south',triggered:'true'}}",
			"{Name:'minecraft:dispenser',Properties:{facing:'south',triggered:'true'}}"
		);
		putStates(
			380, "{Name:'minecraft:dispenser',Properties:{facing:'west',triggered:'true'}}", "{Name:'minecraft:dispenser',Properties:{facing:'west',triggered:'true'}}"
		);
		putStates(
			381, "{Name:'minecraft:dispenser',Properties:{facing:'east',triggered:'true'}}", "{Name:'minecraft:dispenser',Properties:{facing:'east',triggered:'true'}}"
		);
		putStates(384, "{Name:'minecraft:sandstone'}", "{Name:'minecraft:sandstone',Properties:{type:'sandstone'}}");
		putStates(385, "{Name:'minecraft:chiseled_sandstone'}", "{Name:'minecraft:sandstone',Properties:{type:'chiseled_sandstone'}}");
		putStates(386, "{Name:'minecraft:cut_sandstone'}", "{Name:'minecraft:sandstone',Properties:{type:'smooth_sandstone'}}");
		putStates(400, "{Name:'minecraft:note_block'}", "{Name:'minecraft:noteblock'}");
		putStates(
			416,
			"{Name:'minecraft:red_bed',Properties:{facing:'south',occupied:'false',part:'foot'}}",
			"{Name:'minecraft:bed',Properties:{facing:'south',occupied:'false',part:'foot'}}",
			"{Name:'minecraft:bed',Properties:{facing:'south',occupied:'true',part:'foot'}}"
		);
		putStates(
			417,
			"{Name:'minecraft:red_bed',Properties:{facing:'west',occupied:'false',part:'foot'}}",
			"{Name:'minecraft:bed',Properties:{facing:'west',occupied:'false',part:'foot'}}",
			"{Name:'minecraft:bed',Properties:{facing:'west',occupied:'true',part:'foot'}}"
		);
		putStates(
			418,
			"{Name:'minecraft:red_bed',Properties:{facing:'north',occupied:'false',part:'foot'}}",
			"{Name:'minecraft:bed',Properties:{facing:'north',occupied:'false',part:'foot'}}",
			"{Name:'minecraft:bed',Properties:{facing:'north',occupied:'true',part:'foot'}}"
		);
		putStates(
			419,
			"{Name:'minecraft:red_bed',Properties:{facing:'east',occupied:'false',part:'foot'}}",
			"{Name:'minecraft:bed',Properties:{facing:'east',occupied:'false',part:'foot'}}",
			"{Name:'minecraft:bed',Properties:{facing:'east',occupied:'true',part:'foot'}}"
		);
		putStates(
			424,
			"{Name:'minecraft:red_bed',Properties:{facing:'south',occupied:'false',part:'head'}}",
			"{Name:'minecraft:bed',Properties:{facing:'south',occupied:'false',part:'head'}}"
		);
		putStates(
			425,
			"{Name:'minecraft:red_bed',Properties:{facing:'west',occupied:'false',part:'head'}}",
			"{Name:'minecraft:bed',Properties:{facing:'west',occupied:'false',part:'head'}}"
		);
		putStates(
			426,
			"{Name:'minecraft:red_bed',Properties:{facing:'north',occupied:'false',part:'head'}}",
			"{Name:'minecraft:bed',Properties:{facing:'north',occupied:'false',part:'head'}}"
		);
		putStates(
			427,
			"{Name:'minecraft:red_bed',Properties:{facing:'east',occupied:'false',part:'head'}}",
			"{Name:'minecraft:bed',Properties:{facing:'east',occupied:'false',part:'head'}}"
		);
		putStates(
			428,
			"{Name:'minecraft:red_bed',Properties:{facing:'south',occupied:'true',part:'head'}}",
			"{Name:'minecraft:bed',Properties:{facing:'south',occupied:'true',part:'head'}}"
		);
		putStates(
			429,
			"{Name:'minecraft:red_bed',Properties:{facing:'west',occupied:'true',part:'head'}}",
			"{Name:'minecraft:bed',Properties:{facing:'west',occupied:'true',part:'head'}}"
		);
		putStates(
			430,
			"{Name:'minecraft:red_bed',Properties:{facing:'north',occupied:'true',part:'head'}}",
			"{Name:'minecraft:bed',Properties:{facing:'north',occupied:'true',part:'head'}}"
		);
		putStates(
			431,
			"{Name:'minecraft:red_bed',Properties:{facing:'east',occupied:'true',part:'head'}}",
			"{Name:'minecraft:bed',Properties:{facing:'east',occupied:'true',part:'head'}}"
		);
		putStates(
			432,
			"{Name:'minecraft:powered_rail',Properties:{powered:'false',shape:'north_south'}}",
			"{Name:'minecraft:golden_rail',Properties:{powered:'false',shape:'north_south'}}"
		);
		putStates(
			433,
			"{Name:'minecraft:powered_rail',Properties:{powered:'false',shape:'east_west'}}",
			"{Name:'minecraft:golden_rail',Properties:{powered:'false',shape:'east_west'}}"
		);
		putStates(
			434,
			"{Name:'minecraft:powered_rail',Properties:{powered:'false',shape:'ascending_east'}}",
			"{Name:'minecraft:golden_rail',Properties:{powered:'false',shape:'ascending_east'}}"
		);
		putStates(
			435,
			"{Name:'minecraft:powered_rail',Properties:{powered:'false',shape:'ascending_west'}}",
			"{Name:'minecraft:golden_rail',Properties:{powered:'false',shape:'ascending_west'}}"
		);
		putStates(
			436,
			"{Name:'minecraft:powered_rail',Properties:{powered:'false',shape:'ascending_north'}}",
			"{Name:'minecraft:golden_rail',Properties:{powered:'false',shape:'ascending_north'}}"
		);
		putStates(
			437,
			"{Name:'minecraft:powered_rail',Properties:{powered:'false',shape:'ascending_south'}}",
			"{Name:'minecraft:golden_rail',Properties:{powered:'false',shape:'ascending_south'}}"
		);
		putStates(
			440,
			"{Name:'minecraft:powered_rail',Properties:{powered:'true',shape:'north_south'}}",
			"{Name:'minecraft:golden_rail',Properties:{powered:'true',shape:'north_south'}}"
		);
		putStates(
			441,
			"{Name:'minecraft:powered_rail',Properties:{powered:'true',shape:'east_west'}}",
			"{Name:'minecraft:golden_rail',Properties:{powered:'true',shape:'east_west'}}"
		);
		putStates(
			442,
			"{Name:'minecraft:powered_rail',Properties:{powered:'true',shape:'ascending_east'}}",
			"{Name:'minecraft:golden_rail',Properties:{powered:'true',shape:'ascending_east'}}"
		);
		putStates(
			443,
			"{Name:'minecraft:powered_rail',Properties:{powered:'true',shape:'ascending_west'}}",
			"{Name:'minecraft:golden_rail',Properties:{powered:'true',shape:'ascending_west'}}"
		);
		putStates(
			444,
			"{Name:'minecraft:powered_rail',Properties:{powered:'true',shape:'ascending_north'}}",
			"{Name:'minecraft:golden_rail',Properties:{powered:'true',shape:'ascending_north'}}"
		);
		putStates(
			445,
			"{Name:'minecraft:powered_rail',Properties:{powered:'true',shape:'ascending_south'}}",
			"{Name:'minecraft:golden_rail',Properties:{powered:'true',shape:'ascending_south'}}"
		);
		putStates(
			448,
			"{Name:'minecraft:detector_rail',Properties:{powered:'false',shape:'north_south'}}",
			"{Name:'minecraft:detector_rail',Properties:{powered:'false',shape:'north_south'}}"
		);
		putStates(
			449,
			"{Name:'minecraft:detector_rail',Properties:{powered:'false',shape:'east_west'}}",
			"{Name:'minecraft:detector_rail',Properties:{powered:'false',shape:'east_west'}}"
		);
		putStates(
			450,
			"{Name:'minecraft:detector_rail',Properties:{powered:'false',shape:'ascending_east'}}",
			"{Name:'minecraft:detector_rail',Properties:{powered:'false',shape:'ascending_east'}}"
		);
		putStates(
			451,
			"{Name:'minecraft:detector_rail',Properties:{powered:'false',shape:'ascending_west'}}",
			"{Name:'minecraft:detector_rail',Properties:{powered:'false',shape:'ascending_west'}}"
		);
		putStates(
			452,
			"{Name:'minecraft:detector_rail',Properties:{powered:'false',shape:'ascending_north'}}",
			"{Name:'minecraft:detector_rail',Properties:{powered:'false',shape:'ascending_north'}}"
		);
		putStates(
			453,
			"{Name:'minecraft:detector_rail',Properties:{powered:'false',shape:'ascending_south'}}",
			"{Name:'minecraft:detector_rail',Properties:{powered:'false',shape:'ascending_south'}}"
		);
		putStates(
			456,
			"{Name:'minecraft:detector_rail',Properties:{powered:'true',shape:'north_south'}}",
			"{Name:'minecraft:detector_rail',Properties:{powered:'true',shape:'north_south'}}"
		);
		putStates(
			457,
			"{Name:'minecraft:detector_rail',Properties:{powered:'true',shape:'east_west'}}",
			"{Name:'minecraft:detector_rail',Properties:{powered:'true',shape:'east_west'}}"
		);
		putStates(
			458,
			"{Name:'minecraft:detector_rail',Properties:{powered:'true',shape:'ascending_east'}}",
			"{Name:'minecraft:detector_rail',Properties:{powered:'true',shape:'ascending_east'}}"
		);
		putStates(
			459,
			"{Name:'minecraft:detector_rail',Properties:{powered:'true',shape:'ascending_west'}}",
			"{Name:'minecraft:detector_rail',Properties:{powered:'true',shape:'ascending_west'}}"
		);
		putStates(
			460,
			"{Name:'minecraft:detector_rail',Properties:{powered:'true',shape:'ascending_north'}}",
			"{Name:'minecraft:detector_rail',Properties:{powered:'true',shape:'ascending_north'}}"
		);
		putStates(
			461,
			"{Name:'minecraft:detector_rail',Properties:{powered:'true',shape:'ascending_south'}}",
			"{Name:'minecraft:detector_rail',Properties:{powered:'true',shape:'ascending_south'}}"
		);
		putStates(
			464,
			"{Name:'minecraft:sticky_piston',Properties:{extended:'false',facing:'down'}}",
			"{Name:'minecraft:sticky_piston',Properties:{extended:'false',facing:'down'}}"
		);
		putStates(
			465,
			"{Name:'minecraft:sticky_piston',Properties:{extended:'false',facing:'up'}}",
			"{Name:'minecraft:sticky_piston',Properties:{extended:'false',facing:'up'}}"
		);
		putStates(
			466,
			"{Name:'minecraft:sticky_piston',Properties:{extended:'false',facing:'north'}}",
			"{Name:'minecraft:sticky_piston',Properties:{extended:'false',facing:'north'}}"
		);
		putStates(
			467,
			"{Name:'minecraft:sticky_piston',Properties:{extended:'false',facing:'south'}}",
			"{Name:'minecraft:sticky_piston',Properties:{extended:'false',facing:'south'}}"
		);
		putStates(
			468,
			"{Name:'minecraft:sticky_piston',Properties:{extended:'false',facing:'west'}}",
			"{Name:'minecraft:sticky_piston',Properties:{extended:'false',facing:'west'}}"
		);
		putStates(
			469,
			"{Name:'minecraft:sticky_piston',Properties:{extended:'false',facing:'east'}}",
			"{Name:'minecraft:sticky_piston',Properties:{extended:'false',facing:'east'}}"
		);
		putStates(
			472,
			"{Name:'minecraft:sticky_piston',Properties:{extended:'true',facing:'down'}}",
			"{Name:'minecraft:sticky_piston',Properties:{extended:'true',facing:'down'}}"
		);
		putStates(
			473,
			"{Name:'minecraft:sticky_piston',Properties:{extended:'true',facing:'up'}}",
			"{Name:'minecraft:sticky_piston',Properties:{extended:'true',facing:'up'}}"
		);
		putStates(
			474,
			"{Name:'minecraft:sticky_piston',Properties:{extended:'true',facing:'north'}}",
			"{Name:'minecraft:sticky_piston',Properties:{extended:'true',facing:'north'}}"
		);
		putStates(
			475,
			"{Name:'minecraft:sticky_piston',Properties:{extended:'true',facing:'south'}}",
			"{Name:'minecraft:sticky_piston',Properties:{extended:'true',facing:'south'}}"
		);
		putStates(
			476,
			"{Name:'minecraft:sticky_piston',Properties:{extended:'true',facing:'west'}}",
			"{Name:'minecraft:sticky_piston',Properties:{extended:'true',facing:'west'}}"
		);
		putStates(
			477,
			"{Name:'minecraft:sticky_piston',Properties:{extended:'true',facing:'east'}}",
			"{Name:'minecraft:sticky_piston',Properties:{extended:'true',facing:'east'}}"
		);
		putStates(480, "{Name:'minecraft:cobweb'}", "{Name:'minecraft:web'}");
		putStates(496, "{Name:'minecraft:dead_bush'}", "{Name:'minecraft:tallgrass',Properties:{type:'dead_bush'}}");
		putStates(497, "{Name:'minecraft:grass'}", "{Name:'minecraft:tallgrass',Properties:{type:'tall_grass'}}");
		putStates(498, "{Name:'minecraft:fern'}", "{Name:'minecraft:tallgrass',Properties:{type:'fern'}}");
	}

	/**
	 * Adds states to flatten from the blocks which had numeric IDs between 32 and 47 before 1.13.
	 */
	static void putStatesFromBlocks32To47() {
		putStates(512, "{Name:'minecraft:dead_bush'}", "{Name:'minecraft:deadbush'}");
		putStates(
			528, "{Name:'minecraft:piston',Properties:{extended:'false',facing:'down'}}", "{Name:'minecraft:piston',Properties:{extended:'false',facing:'down'}}"
		);
		putStates(529, "{Name:'minecraft:piston',Properties:{extended:'false',facing:'up'}}", "{Name:'minecraft:piston',Properties:{extended:'false',facing:'up'}}");
		putStates(
			530, "{Name:'minecraft:piston',Properties:{extended:'false',facing:'north'}}", "{Name:'minecraft:piston',Properties:{extended:'false',facing:'north'}}"
		);
		putStates(
			531, "{Name:'minecraft:piston',Properties:{extended:'false',facing:'south'}}", "{Name:'minecraft:piston',Properties:{extended:'false',facing:'south'}}"
		);
		putStates(
			532, "{Name:'minecraft:piston',Properties:{extended:'false',facing:'west'}}", "{Name:'minecraft:piston',Properties:{extended:'false',facing:'west'}}"
		);
		putStates(
			533, "{Name:'minecraft:piston',Properties:{extended:'false',facing:'east'}}", "{Name:'minecraft:piston',Properties:{extended:'false',facing:'east'}}"
		);
		putStates(536, "{Name:'minecraft:piston',Properties:{extended:'true',facing:'down'}}", "{Name:'minecraft:piston',Properties:{extended:'true',facing:'down'}}");
		putStates(537, "{Name:'minecraft:piston',Properties:{extended:'true',facing:'up'}}", "{Name:'minecraft:piston',Properties:{extended:'true',facing:'up'}}");
		putStates(
			538, "{Name:'minecraft:piston',Properties:{extended:'true',facing:'north'}}", "{Name:'minecraft:piston',Properties:{extended:'true',facing:'north'}}"
		);
		putStates(
			539, "{Name:'minecraft:piston',Properties:{extended:'true',facing:'south'}}", "{Name:'minecraft:piston',Properties:{extended:'true',facing:'south'}}"
		);
		putStates(540, "{Name:'minecraft:piston',Properties:{extended:'true',facing:'west'}}", "{Name:'minecraft:piston',Properties:{extended:'true',facing:'west'}}");
		putStates(541, "{Name:'minecraft:piston',Properties:{extended:'true',facing:'east'}}", "{Name:'minecraft:piston',Properties:{extended:'true',facing:'east'}}");
		putStates(
			544,
			"{Name:'minecraft:piston_head',Properties:{facing:'down',short:'false',type:'normal'}}",
			"{Name:'minecraft:piston_head',Properties:{facing:'down',short:'false',type:'normal'}}",
			"{Name:'minecraft:piston_head',Properties:{facing:'down',short:'true',type:'normal'}}"
		);
		putStates(
			545,
			"{Name:'minecraft:piston_head',Properties:{facing:'up',short:'false',type:'normal'}}",
			"{Name:'minecraft:piston_head',Properties:{facing:'up',short:'false',type:'normal'}}",
			"{Name:'minecraft:piston_head',Properties:{facing:'up',short:'true',type:'normal'}}"
		);
		putStates(
			546,
			"{Name:'minecraft:piston_head',Properties:{facing:'north',short:'false',type:'normal'}}",
			"{Name:'minecraft:piston_head',Properties:{facing:'north',short:'false',type:'normal'}}",
			"{Name:'minecraft:piston_head',Properties:{facing:'north',short:'true',type:'normal'}}"
		);
		putStates(
			547,
			"{Name:'minecraft:piston_head',Properties:{facing:'south',short:'false',type:'normal'}}",
			"{Name:'minecraft:piston_head',Properties:{facing:'south',short:'false',type:'normal'}}",
			"{Name:'minecraft:piston_head',Properties:{facing:'south',short:'true',type:'normal'}}"
		);
		putStates(
			548,
			"{Name:'minecraft:piston_head',Properties:{facing:'west',short:'false',type:'normal'}}",
			"{Name:'minecraft:piston_head',Properties:{facing:'west',short:'false',type:'normal'}}",
			"{Name:'minecraft:piston_head',Properties:{facing:'west',short:'true',type:'normal'}}"
		);
		putStates(
			549,
			"{Name:'minecraft:piston_head',Properties:{facing:'east',short:'false',type:'normal'}}",
			"{Name:'minecraft:piston_head',Properties:{facing:'east',short:'false',type:'normal'}}",
			"{Name:'minecraft:piston_head',Properties:{facing:'east',short:'true',type:'normal'}}"
		);
		putStates(
			552,
			"{Name:'minecraft:piston_head',Properties:{facing:'down',short:'false',type:'sticky'}}",
			"{Name:'minecraft:piston_head',Properties:{facing:'down',short:'false',type:'sticky'}}",
			"{Name:'minecraft:piston_head',Properties:{facing:'down',short:'true',type:'sticky'}}"
		);
		putStates(
			553,
			"{Name:'minecraft:piston_head',Properties:{facing:'up',short:'false',type:'sticky'}}",
			"{Name:'minecraft:piston_head',Properties:{facing:'up',short:'false',type:'sticky'}}",
			"{Name:'minecraft:piston_head',Properties:{facing:'up',short:'true',type:'sticky'}}"
		);
		putStates(
			554,
			"{Name:'minecraft:piston_head',Properties:{facing:'north',short:'false',type:'sticky'}}",
			"{Name:'minecraft:piston_head',Properties:{facing:'north',short:'false',type:'sticky'}}",
			"{Name:'minecraft:piston_head',Properties:{facing:'north',short:'true',type:'sticky'}}"
		);
		putStates(
			555,
			"{Name:'minecraft:piston_head',Properties:{facing:'south',short:'false',type:'sticky'}}",
			"{Name:'minecraft:piston_head',Properties:{facing:'south',short:'false',type:'sticky'}}",
			"{Name:'minecraft:piston_head',Properties:{facing:'south',short:'true',type:'sticky'}}"
		);
		putStates(
			556,
			"{Name:'minecraft:piston_head',Properties:{facing:'west',short:'false',type:'sticky'}}",
			"{Name:'minecraft:piston_head',Properties:{facing:'west',short:'false',type:'sticky'}}",
			"{Name:'minecraft:piston_head',Properties:{facing:'west',short:'true',type:'sticky'}}"
		);
		putStates(
			557,
			"{Name:'minecraft:piston_head',Properties:{facing:'east',short:'false',type:'sticky'}}",
			"{Name:'minecraft:piston_head',Properties:{facing:'east',short:'false',type:'sticky'}}",
			"{Name:'minecraft:piston_head',Properties:{facing:'east',short:'true',type:'sticky'}}"
		);
		putStates(560, "{Name:'minecraft:white_wool'}", "{Name:'minecraft:wool',Properties:{color:'white'}}");
		putStates(561, "{Name:'minecraft:orange_wool'}", "{Name:'minecraft:wool',Properties:{color:'orange'}}");
		putStates(562, "{Name:'minecraft:magenta_wool'}", "{Name:'minecraft:wool',Properties:{color:'magenta'}}");
		putStates(563, "{Name:'minecraft:light_blue_wool'}", "{Name:'minecraft:wool',Properties:{color:'light_blue'}}");
		putStates(564, "{Name:'minecraft:yellow_wool'}", "{Name:'minecraft:wool',Properties:{color:'yellow'}}");
		putStates(565, "{Name:'minecraft:lime_wool'}", "{Name:'minecraft:wool',Properties:{color:'lime'}}");
		putStates(566, "{Name:'minecraft:pink_wool'}", "{Name:'minecraft:wool',Properties:{color:'pink'}}");
		putStates(567, "{Name:'minecraft:gray_wool'}", "{Name:'minecraft:wool',Properties:{color:'gray'}}");
		putStates(568, "{Name:'minecraft:light_gray_wool'}", "{Name:'minecraft:wool',Properties:{color:'silver'}}");
		putStates(569, "{Name:'minecraft:cyan_wool'}", "{Name:'minecraft:wool',Properties:{color:'cyan'}}");
		putStates(570, "{Name:'minecraft:purple_wool'}", "{Name:'minecraft:wool',Properties:{color:'purple'}}");
		putStates(571, "{Name:'minecraft:blue_wool'}", "{Name:'minecraft:wool',Properties:{color:'blue'}}");
		putStates(572, "{Name:'minecraft:brown_wool'}", "{Name:'minecraft:wool',Properties:{color:'brown'}}");
		putStates(573, "{Name:'minecraft:green_wool'}", "{Name:'minecraft:wool',Properties:{color:'green'}}");
		putStates(574, "{Name:'minecraft:red_wool'}", "{Name:'minecraft:wool',Properties:{color:'red'}}");
		putStates(575, "{Name:'minecraft:black_wool'}", "{Name:'minecraft:wool',Properties:{color:'black'}}");
		putStates(
			576,
			"{Name:'minecraft:moving_piston',Properties:{facing:'down',type:'normal'}}",
			"{Name:'minecraft:piston_extension',Properties:{facing:'down',type:'normal'}}"
		);
		putStates(
			577, "{Name:'minecraft:moving_piston',Properties:{facing:'up',type:'normal'}}", "{Name:'minecraft:piston_extension',Properties:{facing:'up',type:'normal'}}"
		);
		putStates(
			578,
			"{Name:'minecraft:moving_piston',Properties:{facing:'north',type:'normal'}}",
			"{Name:'minecraft:piston_extension',Properties:{facing:'north',type:'normal'}}"
		);
		putStates(
			579,
			"{Name:'minecraft:moving_piston',Properties:{facing:'south',type:'normal'}}",
			"{Name:'minecraft:piston_extension',Properties:{facing:'south',type:'normal'}}"
		);
		putStates(
			580,
			"{Name:'minecraft:moving_piston',Properties:{facing:'west',type:'normal'}}",
			"{Name:'minecraft:piston_extension',Properties:{facing:'west',type:'normal'}}"
		);
		putStates(
			581,
			"{Name:'minecraft:moving_piston',Properties:{facing:'east',type:'normal'}}",
			"{Name:'minecraft:piston_extension',Properties:{facing:'east',type:'normal'}}"
		);
		putStates(
			584,
			"{Name:'minecraft:moving_piston',Properties:{facing:'down',type:'sticky'}}",
			"{Name:'minecraft:piston_extension',Properties:{facing:'down',type:'sticky'}}"
		);
		putStates(
			585, "{Name:'minecraft:moving_piston',Properties:{facing:'up',type:'sticky'}}", "{Name:'minecraft:piston_extension',Properties:{facing:'up',type:'sticky'}}"
		);
		putStates(
			586,
			"{Name:'minecraft:moving_piston',Properties:{facing:'north',type:'sticky'}}",
			"{Name:'minecraft:piston_extension',Properties:{facing:'north',type:'sticky'}}"
		);
		putStates(
			587,
			"{Name:'minecraft:moving_piston',Properties:{facing:'south',type:'sticky'}}",
			"{Name:'minecraft:piston_extension',Properties:{facing:'south',type:'sticky'}}"
		);
		putStates(
			588,
			"{Name:'minecraft:moving_piston',Properties:{facing:'west',type:'sticky'}}",
			"{Name:'minecraft:piston_extension',Properties:{facing:'west',type:'sticky'}}"
		);
		putStates(
			589,
			"{Name:'minecraft:moving_piston',Properties:{facing:'east',type:'sticky'}}",
			"{Name:'minecraft:piston_extension',Properties:{facing:'east',type:'sticky'}}"
		);
		putStates(592, "{Name:'minecraft:dandelion'}", "{Name:'minecraft:yellow_flower',Properties:{type:'dandelion'}}");
		putStates(608, "{Name:'minecraft:poppy'}", "{Name:'minecraft:red_flower',Properties:{type:'poppy'}}");
		putStates(609, "{Name:'minecraft:blue_orchid'}", "{Name:'minecraft:red_flower',Properties:{type:'blue_orchid'}}");
		putStates(610, "{Name:'minecraft:allium'}", "{Name:'minecraft:red_flower',Properties:{type:'allium'}}");
		putStates(611, "{Name:'minecraft:azure_bluet'}", "{Name:'minecraft:red_flower',Properties:{type:'houstonia'}}");
		putStates(612, "{Name:'minecraft:red_tulip'}", "{Name:'minecraft:red_flower',Properties:{type:'red_tulip'}}");
		putStates(613, "{Name:'minecraft:orange_tulip'}", "{Name:'minecraft:red_flower',Properties:{type:'orange_tulip'}}");
		putStates(614, "{Name:'minecraft:white_tulip'}", "{Name:'minecraft:red_flower',Properties:{type:'white_tulip'}}");
		putStates(615, "{Name:'minecraft:pink_tulip'}", "{Name:'minecraft:red_flower',Properties:{type:'pink_tulip'}}");
		putStates(616, "{Name:'minecraft:oxeye_daisy'}", "{Name:'minecraft:red_flower',Properties:{type:'oxeye_daisy'}}");
		putStates(624, "{Name:'minecraft:brown_mushroom'}", "{Name:'minecraft:brown_mushroom'}");
		putStates(640, "{Name:'minecraft:red_mushroom'}", "{Name:'minecraft:red_mushroom'}");
		putStates(656, "{Name:'minecraft:gold_block'}", "{Name:'minecraft:gold_block'}");
		putStates(672, "{Name:'minecraft:iron_block'}", "{Name:'minecraft:iron_block'}");
		putStates(
			688, "{Name:'minecraft:stone_slab',Properties:{type:'double'}}", "{Name:'minecraft:double_stone_slab',Properties:{seamless:'false',variant:'stone'}}"
		);
		putStates(
			689,
			"{Name:'minecraft:sandstone_slab',Properties:{type:'double'}}",
			"{Name:'minecraft:double_stone_slab',Properties:{seamless:'false',variant:'sandstone'}}"
		);
		putStates(
			690,
			"{Name:'minecraft:petrified_oak_slab',Properties:{type:'double'}}",
			"{Name:'minecraft:double_stone_slab',Properties:{seamless:'false',variant:'wood_old'}}"
		);
		putStates(
			691,
			"{Name:'minecraft:cobblestone_slab',Properties:{type:'double'}}",
			"{Name:'minecraft:double_stone_slab',Properties:{seamless:'false',variant:'cobblestone'}}"
		);
		putStates(
			692, "{Name:'minecraft:brick_slab',Properties:{type:'double'}}", "{Name:'minecraft:double_stone_slab',Properties:{seamless:'false',variant:'brick'}}"
		);
		putStates(
			693,
			"{Name:'minecraft:stone_brick_slab',Properties:{type:'double'}}",
			"{Name:'minecraft:double_stone_slab',Properties:{seamless:'false',variant:'stone_brick'}}"
		);
		putStates(
			694,
			"{Name:'minecraft:nether_brick_slab',Properties:{type:'double'}}",
			"{Name:'minecraft:double_stone_slab',Properties:{seamless:'false',variant:'nether_brick'}}"
		);
		putStates(
			695, "{Name:'minecraft:quartz_slab',Properties:{type:'double'}}", "{Name:'minecraft:double_stone_slab',Properties:{seamless:'false',variant:'quartz'}}"
		);
		putStates(696, "{Name:'minecraft:smooth_stone'}", "{Name:'minecraft:double_stone_slab',Properties:{seamless:'true',variant:'stone'}}");
		putStates(697, "{Name:'minecraft:smooth_sandstone'}", "{Name:'minecraft:double_stone_slab',Properties:{seamless:'true',variant:'sandstone'}}");
		putStates(
			698,
			"{Name:'minecraft:petrified_oak_slab',Properties:{type:'double'}}",
			"{Name:'minecraft:double_stone_slab',Properties:{seamless:'true',variant:'wood_old'}}"
		);
		putStates(
			699,
			"{Name:'minecraft:cobblestone_slab',Properties:{type:'double'}}",
			"{Name:'minecraft:double_stone_slab',Properties:{seamless:'true',variant:'cobblestone'}}"
		);
		putStates(
			700, "{Name:'minecraft:brick_slab',Properties:{type:'double'}}", "{Name:'minecraft:double_stone_slab',Properties:{seamless:'true',variant:'brick'}}"
		);
		putStates(
			701,
			"{Name:'minecraft:stone_brick_slab',Properties:{type:'double'}}",
			"{Name:'minecraft:double_stone_slab',Properties:{seamless:'true',variant:'stone_brick'}}"
		);
		putStates(
			702,
			"{Name:'minecraft:nether_brick_slab',Properties:{type:'double'}}",
			"{Name:'minecraft:double_stone_slab',Properties:{seamless:'true',variant:'nether_brick'}}"
		);
		putStates(703, "{Name:'minecraft:smooth_quartz'}", "{Name:'minecraft:double_stone_slab',Properties:{seamless:'true',variant:'quartz'}}");
		putStates(704, "{Name:'minecraft:stone_slab',Properties:{type:'bottom'}}", "{Name:'minecraft:stone_slab',Properties:{half:'bottom',variant:'stone'}}");
		putStates(705, "{Name:'minecraft:sandstone_slab',Properties:{type:'bottom'}}", "{Name:'minecraft:stone_slab',Properties:{half:'bottom',variant:'sandstone'}}");
		putStates(
			706, "{Name:'minecraft:petrified_oak_slab',Properties:{type:'bottom'}}", "{Name:'minecraft:stone_slab',Properties:{half:'bottom',variant:'wood_old'}}"
		);
		putStates(
			707, "{Name:'minecraft:cobblestone_slab',Properties:{type:'bottom'}}", "{Name:'minecraft:stone_slab',Properties:{half:'bottom',variant:'cobblestone'}}"
		);
		putStates(708, "{Name:'minecraft:brick_slab',Properties:{type:'bottom'}}", "{Name:'minecraft:stone_slab',Properties:{half:'bottom',variant:'brick'}}");
		putStates(
			709, "{Name:'minecraft:stone_brick_slab',Properties:{type:'bottom'}}", "{Name:'minecraft:stone_slab',Properties:{half:'bottom',variant:'stone_brick'}}"
		);
		putStates(
			710, "{Name:'minecraft:nether_brick_slab',Properties:{type:'bottom'}}", "{Name:'minecraft:stone_slab',Properties:{half:'bottom',variant:'nether_brick'}}"
		);
		putStates(711, "{Name:'minecraft:quartz_slab',Properties:{type:'bottom'}}", "{Name:'minecraft:stone_slab',Properties:{half:'bottom',variant:'quartz'}}");
		putStates(712, "{Name:'minecraft:stone_slab',Properties:{type:'top'}}", "{Name:'minecraft:stone_slab',Properties:{half:'top',variant:'stone'}}");
		putStates(713, "{Name:'minecraft:sandstone_slab',Properties:{type:'top'}}", "{Name:'minecraft:stone_slab',Properties:{half:'top',variant:'sandstone'}}");
		putStates(714, "{Name:'minecraft:petrified_oak_slab',Properties:{type:'top'}}", "{Name:'minecraft:stone_slab',Properties:{half:'top',variant:'wood_old'}}");
		putStates(715, "{Name:'minecraft:cobblestone_slab',Properties:{type:'top'}}", "{Name:'minecraft:stone_slab',Properties:{half:'top',variant:'cobblestone'}}");
		putStates(716, "{Name:'minecraft:brick_slab',Properties:{type:'top'}}", "{Name:'minecraft:stone_slab',Properties:{half:'top',variant:'brick'}}");
		putStates(717, "{Name:'minecraft:stone_brick_slab',Properties:{type:'top'}}", "{Name:'minecraft:stone_slab',Properties:{half:'top',variant:'stone_brick'}}");
		putStates(718, "{Name:'minecraft:nether_brick_slab',Properties:{type:'top'}}", "{Name:'minecraft:stone_slab',Properties:{half:'top',variant:'nether_brick'}}");
		putStates(719, "{Name:'minecraft:quartz_slab',Properties:{type:'top'}}", "{Name:'minecraft:stone_slab',Properties:{half:'top',variant:'quartz'}}");
		putStates(720, "{Name:'minecraft:bricks'}", "{Name:'minecraft:brick_block'}");
		putStates(736, "{Name:'minecraft:tnt',Properties:{unstable:'false'}}", "{Name:'minecraft:tnt',Properties:{explode:'false'}}");
		putStates(737, "{Name:'minecraft:tnt',Properties:{unstable:'true'}}", "{Name:'minecraft:tnt',Properties:{explode:'true'}}");
		putStates(752, "{Name:'minecraft:bookshelf'}", "{Name:'minecraft:bookshelf'}");
	}

	/**
	 * Adds states to flatten from the blocks which had numeric IDs between 48 and 63 before 1.13.
	 */
	static void putStatesFromBlocks48To63() {
		putStates(768, "{Name:'minecraft:mossy_cobblestone'}", "{Name:'minecraft:mossy_cobblestone'}");
		putStates(784, "{Name:'minecraft:obsidian'}", "{Name:'minecraft:obsidian'}");
		putStates(801, "{Name:'minecraft:wall_torch',Properties:{facing:'east'}}", "{Name:'minecraft:torch',Properties:{facing:'east'}}");
		putStates(802, "{Name:'minecraft:wall_torch',Properties:{facing:'west'}}", "{Name:'minecraft:torch',Properties:{facing:'west'}}");
		putStates(803, "{Name:'minecraft:wall_torch',Properties:{facing:'south'}}", "{Name:'minecraft:torch',Properties:{facing:'south'}}");
		putStates(804, "{Name:'minecraft:wall_torch',Properties:{facing:'north'}}", "{Name:'minecraft:torch',Properties:{facing:'north'}}");
		putStates(805, "{Name:'minecraft:torch'}", "{Name:'minecraft:torch',Properties:{facing:'up'}}");
		putStates(
			816,
			"{Name:'minecraft:fire',Properties:{age:'0',east:'false',north:'false',south:'false',up:'false',west:'false'}}",
			"{Name:'minecraft:fire',Properties:{age:'0',east:'false',north:'false',south:'false',up:'false',west:'false'}}",
			"{Name:'minecraft:fire',Properties:{age:'0',east:'false',north:'false',south:'false',up:'false',west:'true'}}",
			"{Name:'minecraft:fire',Properties:{age:'0',east:'false',north:'false',south:'false',up:'true',west:'false'}}",
			"{Name:'minecraft:fire',Properties:{age:'0',east:'false',north:'false',south:'false',up:'true',west:'true'}}",
			"{Name:'minecraft:fire',Properties:{age:'0',east:'false',north:'false',south:'true',up:'false',west:'false'}}",
			"{Name:'minecraft:fire',Properties:{age:'0',east:'false',north:'false',south:'true',up:'false',west:'true'}}",
			"{Name:'minecraft:fire',Properties:{age:'0',east:'false',north:'false',south:'true',up:'true',west:'false'}}",
			"{Name:'minecraft:fire',Properties:{age:'0',east:'false',north:'false',south:'true',up:'true',west:'true'}}",
			"{Name:'minecraft:fire',Properties:{age:'0',east:'false',north:'true',south:'false',up:'false',west:'false'}}",
			"{Name:'minecraft:fire',Properties:{age:'0',east:'false',north:'true',south:'false',up:'false',west:'true'}}",
			"{Name:'minecraft:fire',Properties:{age:'0',east:'false',north:'true',south:'false',up:'true',west:'false'}}",
			"{Name:'minecraft:fire',Properties:{age:'0',east:'false',north:'true',south:'false',up:'true',west:'true'}}",
			"{Name:'minecraft:fire',Properties:{age:'0',east:'false',north:'true',south:'true',up:'false',west:'false'}}",
			"{Name:'minecraft:fire',Properties:{age:'0',east:'false',north:'true',south:'true',up:'false',west:'true'}}",
			"{Name:'minecraft:fire',Properties:{age:'0',east:'false',north:'true',south:'true',up:'true',west:'false'}}",
			"{Name:'minecraft:fire',Properties:{age:'0',east:'false',north:'true',south:'true',up:'true',west:'true'}}",
			"{Name:'minecraft:fire',Properties:{age:'0',east:'true',north:'false',south:'false',up:'false',west:'false'}}",
			"{Name:'minecraft:fire',Properties:{age:'0',east:'true',north:'false',south:'false',up:'false',west:'true'}}",
			"{Name:'minecraft:fire',Properties:{age:'0',east:'true',north:'false',south:'false',up:'true',west:'false'}}",
			"{Name:'minecraft:fire',Properties:{age:'0',east:'true',north:'false',south:'false',up:'true',west:'true'}}",
			"{Name:'minecraft:fire',Properties:{age:'0',east:'true',north:'false',south:'true',up:'false',west:'false'}}",
			"{Name:'minecraft:fire',Properties:{age:'0',east:'true',north:'false',south:'true',up:'false',west:'true'}}",
			"{Name:'minecraft:fire',Properties:{age:'0',east:'true',north:'false',south:'true',up:'true',west:'false'}}",
			"{Name:'minecraft:fire',Properties:{age:'0',east:'true',north:'false',south:'true',up:'true',west:'true'}}",
			"{Name:'minecraft:fire',Properties:{age:'0',east:'true',north:'true',south:'false',up:'false',west:'false'}}",
			"{Name:'minecraft:fire',Properties:{age:'0',east:'true',north:'true',south:'false',up:'false',west:'true'}}",
			"{Name:'minecraft:fire',Properties:{age:'0',east:'true',north:'true',south:'false',up:'true',west:'false'}}",
			"{Name:'minecraft:fire',Properties:{age:'0',east:'true',north:'true',south:'false',up:'true',west:'true'}}",
			"{Name:'minecraft:fire',Properties:{age:'0',east:'true',north:'true',south:'true',up:'false',west:'false'}}",
			"{Name:'minecraft:fire',Properties:{age:'0',east:'true',north:'true',south:'true',up:'false',west:'true'}}",
			"{Name:'minecraft:fire',Properties:{age:'0',east:'true',north:'true',south:'true',up:'true',west:'false'}}",
			"{Name:'minecraft:fire',Properties:{age:'0',east:'true',north:'true',south:'true',up:'true',west:'true'}}"
		);
		putStates(
			817,
			"{Name:'minecraft:fire',Properties:{age:'1',east:'false',north:'false',south:'false',up:'false',west:'false'}}",
			"{Name:'minecraft:fire',Properties:{age:'1',east:'false',north:'false',south:'false',up:'false',west:'false'}}",
			"{Name:'minecraft:fire',Properties:{age:'1',east:'false',north:'false',south:'false',up:'false',west:'true'}}",
			"{Name:'minecraft:fire',Properties:{age:'1',east:'false',north:'false',south:'false',up:'true',west:'false'}}",
			"{Name:'minecraft:fire',Properties:{age:'1',east:'false',north:'false',south:'false',up:'true',west:'true'}}",
			"{Name:'minecraft:fire',Properties:{age:'1',east:'false',north:'false',south:'true',up:'false',west:'false'}}",
			"{Name:'minecraft:fire',Properties:{age:'1',east:'false',north:'false',south:'true',up:'false',west:'true'}}",
			"{Name:'minecraft:fire',Properties:{age:'1',east:'false',north:'false',south:'true',up:'true',west:'false'}}",
			"{Name:'minecraft:fire',Properties:{age:'1',east:'false',north:'false',south:'true',up:'true',west:'true'}}",
			"{Name:'minecraft:fire',Properties:{age:'1',east:'false',north:'true',south:'false',up:'false',west:'false'}}",
			"{Name:'minecraft:fire',Properties:{age:'1',east:'false',north:'true',south:'false',up:'false',west:'true'}}",
			"{Name:'minecraft:fire',Properties:{age:'1',east:'false',north:'true',south:'false',up:'true',west:'false'}}",
			"{Name:'minecraft:fire',Properties:{age:'1',east:'false',north:'true',south:'false',up:'true',west:'true'}}",
			"{Name:'minecraft:fire',Properties:{age:'1',east:'false',north:'true',south:'true',up:'false',west:'false'}}",
			"{Name:'minecraft:fire',Properties:{age:'1',east:'false',north:'true',south:'true',up:'false',west:'true'}}",
			"{Name:'minecraft:fire',Properties:{age:'1',east:'false',north:'true',south:'true',up:'true',west:'false'}}",
			"{Name:'minecraft:fire',Properties:{age:'1',east:'false',north:'true',south:'true',up:'true',west:'true'}}",
			"{Name:'minecraft:fire',Properties:{age:'1',east:'true',north:'false',south:'false',up:'false',west:'false'}}",
			"{Name:'minecraft:fire',Properties:{age:'1',east:'true',north:'false',south:'false',up:'false',west:'true'}}",
			"{Name:'minecraft:fire',Properties:{age:'1',east:'true',north:'false',south:'false',up:'true',west:'false'}}",
			"{Name:'minecraft:fire',Properties:{age:'1',east:'true',north:'false',south:'false',up:'true',west:'true'}}",
			"{Name:'minecraft:fire',Properties:{age:'1',east:'true',north:'false',south:'true',up:'false',west:'false'}}",
			"{Name:'minecraft:fire',Properties:{age:'1',east:'true',north:'false',south:'true',up:'false',west:'true'}}",
			"{Name:'minecraft:fire',Properties:{age:'1',east:'true',north:'false',south:'true',up:'true',west:'false'}}",
			"{Name:'minecraft:fire',Properties:{age:'1',east:'true',north:'false',south:'true',up:'true',west:'true'}}",
			"{Name:'minecraft:fire',Properties:{age:'1',east:'true',north:'true',south:'false',up:'false',west:'false'}}",
			"{Name:'minecraft:fire',Properties:{age:'1',east:'true',north:'true',south:'false',up:'false',west:'true'}}",
			"{Name:'minecraft:fire',Properties:{age:'1',east:'true',north:'true',south:'false',up:'true',west:'false'}}",
			"{Name:'minecraft:fire',Properties:{age:'1',east:'true',north:'true',south:'false',up:'true',west:'true'}}",
			"{Name:'minecraft:fire',Properties:{age:'1',east:'true',north:'true',south:'true',up:'false',west:'false'}}",
			"{Name:'minecraft:fire',Properties:{age:'1',east:'true',north:'true',south:'true',up:'false',west:'true'}}",
			"{Name:'minecraft:fire',Properties:{age:'1',east:'true',north:'true',south:'true',up:'true',west:'false'}}",
			"{Name:'minecraft:fire',Properties:{age:'1',east:'true',north:'true',south:'true',up:'true',west:'true'}}"
		);
		putStates(
			818,
			"{Name:'minecraft:fire',Properties:{age:'2',east:'false',north:'false',south:'false',up:'false',west:'false'}}",
			"{Name:'minecraft:fire',Properties:{age:'2',east:'false',north:'false',south:'false',up:'false',west:'false'}}",
			"{Name:'minecraft:fire',Properties:{age:'2',east:'false',north:'false',south:'false',up:'false',west:'true'}}",
			"{Name:'minecraft:fire',Properties:{age:'2',east:'false',north:'false',south:'false',up:'true',west:'false'}}",
			"{Name:'minecraft:fire',Properties:{age:'2',east:'false',north:'false',south:'false',up:'true',west:'true'}}",
			"{Name:'minecraft:fire',Properties:{age:'2',east:'false',north:'false',south:'true',up:'false',west:'false'}}",
			"{Name:'minecraft:fire',Properties:{age:'2',east:'false',north:'false',south:'true',up:'false',west:'true'}}",
			"{Name:'minecraft:fire',Properties:{age:'2',east:'false',north:'false',south:'true',up:'true',west:'false'}}",
			"{Name:'minecraft:fire',Properties:{age:'2',east:'false',north:'false',south:'true',up:'true',west:'true'}}",
			"{Name:'minecraft:fire',Properties:{age:'2',east:'false',north:'true',south:'false',up:'false',west:'false'}}",
			"{Name:'minecraft:fire',Properties:{age:'2',east:'false',north:'true',south:'false',up:'false',west:'true'}}",
			"{Name:'minecraft:fire',Properties:{age:'2',east:'false',north:'true',south:'false',up:'true',west:'false'}}",
			"{Name:'minecraft:fire',Properties:{age:'2',east:'false',north:'true',south:'false',up:'true',west:'true'}}",
			"{Name:'minecraft:fire',Properties:{age:'2',east:'false',north:'true',south:'true',up:'false',west:'false'}}",
			"{Name:'minecraft:fire',Properties:{age:'2',east:'false',north:'true',south:'true',up:'false',west:'true'}}",
			"{Name:'minecraft:fire',Properties:{age:'2',east:'false',north:'true',south:'true',up:'true',west:'false'}}",
			"{Name:'minecraft:fire',Properties:{age:'2',east:'false',north:'true',south:'true',up:'true',west:'true'}}",
			"{Name:'minecraft:fire',Properties:{age:'2',east:'true',north:'false',south:'false',up:'false',west:'false'}}",
			"{Name:'minecraft:fire',Properties:{age:'2',east:'true',north:'false',south:'false',up:'false',west:'true'}}",
			"{Name:'minecraft:fire',Properties:{age:'2',east:'true',north:'false',south:'false',up:'true',west:'false'}}",
			"{Name:'minecraft:fire',Properties:{age:'2',east:'true',north:'false',south:'false',up:'true',west:'true'}}",
			"{Name:'minecraft:fire',Properties:{age:'2',east:'true',north:'false',south:'true',up:'false',west:'false'}}",
			"{Name:'minecraft:fire',Properties:{age:'2',east:'true',north:'false',south:'true',up:'false',west:'true'}}",
			"{Name:'minecraft:fire',Properties:{age:'2',east:'true',north:'false',south:'true',up:'true',west:'false'}}",
			"{Name:'minecraft:fire',Properties:{age:'2',east:'true',north:'false',south:'true',up:'true',west:'true'}}",
			"{Name:'minecraft:fire',Properties:{age:'2',east:'true',north:'true',south:'false',up:'false',west:'false'}}",
			"{Name:'minecraft:fire',Properties:{age:'2',east:'true',north:'true',south:'false',up:'false',west:'true'}}",
			"{Name:'minecraft:fire',Properties:{age:'2',east:'true',north:'true',south:'false',up:'true',west:'false'}}",
			"{Name:'minecraft:fire',Properties:{age:'2',east:'true',north:'true',south:'false',up:'true',west:'true'}}",
			"{Name:'minecraft:fire',Properties:{age:'2',east:'true',north:'true',south:'true',up:'false',west:'false'}}",
			"{Name:'minecraft:fire',Properties:{age:'2',east:'true',north:'true',south:'true',up:'false',west:'true'}}",
			"{Name:'minecraft:fire',Properties:{age:'2',east:'true',north:'true',south:'true',up:'true',west:'false'}}",
			"{Name:'minecraft:fire',Properties:{age:'2',east:'true',north:'true',south:'true',up:'true',west:'true'}}"
		);
		putStates(
			819,
			"{Name:'minecraft:fire',Properties:{age:'3',east:'false',north:'false',south:'false',up:'false',west:'false'}}",
			"{Name:'minecraft:fire',Properties:{age:'3',east:'false',north:'false',south:'false',up:'false',west:'false'}}",
			"{Name:'minecraft:fire',Properties:{age:'3',east:'false',north:'false',south:'false',up:'false',west:'true'}}",
			"{Name:'minecraft:fire',Properties:{age:'3',east:'false',north:'false',south:'false',up:'true',west:'false'}}",
			"{Name:'minecraft:fire',Properties:{age:'3',east:'false',north:'false',south:'false',up:'true',west:'true'}}",
			"{Name:'minecraft:fire',Properties:{age:'3',east:'false',north:'false',south:'true',up:'false',west:'false'}}",
			"{Name:'minecraft:fire',Properties:{age:'3',east:'false',north:'false',south:'true',up:'false',west:'true'}}",
			"{Name:'minecraft:fire',Properties:{age:'3',east:'false',north:'false',south:'true',up:'true',west:'false'}}",
			"{Name:'minecraft:fire',Properties:{age:'3',east:'false',north:'false',south:'true',up:'true',west:'true'}}",
			"{Name:'minecraft:fire',Properties:{age:'3',east:'false',north:'true',south:'false',up:'false',west:'false'}}",
			"{Name:'minecraft:fire',Properties:{age:'3',east:'false',north:'true',south:'false',up:'false',west:'true'}}",
			"{Name:'minecraft:fire',Properties:{age:'3',east:'false',north:'true',south:'false',up:'true',west:'false'}}",
			"{Name:'minecraft:fire',Properties:{age:'3',east:'false',north:'true',south:'false',up:'true',west:'true'}}",
			"{Name:'minecraft:fire',Properties:{age:'3',east:'false',north:'true',south:'true',up:'false',west:'false'}}",
			"{Name:'minecraft:fire',Properties:{age:'3',east:'false',north:'true',south:'true',up:'false',west:'true'}}",
			"{Name:'minecraft:fire',Properties:{age:'3',east:'false',north:'true',south:'true',up:'true',west:'false'}}",
			"{Name:'minecraft:fire',Properties:{age:'3',east:'false',north:'true',south:'true',up:'true',west:'true'}}",
			"{Name:'minecraft:fire',Properties:{age:'3',east:'true',north:'false',south:'false',up:'false',west:'false'}}",
			"{Name:'minecraft:fire',Properties:{age:'3',east:'true',north:'false',south:'false',up:'false',west:'true'}}",
			"{Name:'minecraft:fire',Properties:{age:'3',east:'true',north:'false',south:'false',up:'true',west:'false'}}",
			"{Name:'minecraft:fire',Properties:{age:'3',east:'true',north:'false',south:'false',up:'true',west:'true'}}",
			"{Name:'minecraft:fire',Properties:{age:'3',east:'true',north:'false',south:'true',up:'false',west:'false'}}",
			"{Name:'minecraft:fire',Properties:{age:'3',east:'true',north:'false',south:'true',up:'false',west:'true'}}",
			"{Name:'minecraft:fire',Properties:{age:'3',east:'true',north:'false',south:'true',up:'true',west:'false'}}",
			"{Name:'minecraft:fire',Properties:{age:'3',east:'true',north:'false',south:'true',up:'true',west:'true'}}",
			"{Name:'minecraft:fire',Properties:{age:'3',east:'true',north:'true',south:'false',up:'false',west:'false'}}",
			"{Name:'minecraft:fire',Properties:{age:'3',east:'true',north:'true',south:'false',up:'false',west:'true'}}",
			"{Name:'minecraft:fire',Properties:{age:'3',east:'true',north:'true',south:'false',up:'true',west:'false'}}",
			"{Name:'minecraft:fire',Properties:{age:'3',east:'true',north:'true',south:'false',up:'true',west:'true'}}",
			"{Name:'minecraft:fire',Properties:{age:'3',east:'true',north:'true',south:'true',up:'false',west:'false'}}",
			"{Name:'minecraft:fire',Properties:{age:'3',east:'true',north:'true',south:'true',up:'false',west:'true'}}",
			"{Name:'minecraft:fire',Properties:{age:'3',east:'true',north:'true',south:'true',up:'true',west:'false'}}",
			"{Name:'minecraft:fire',Properties:{age:'3',east:'true',north:'true',south:'true',up:'true',west:'true'}}"
		);
		putStates(
			820,
			"{Name:'minecraft:fire',Properties:{age:'4',east:'false',north:'false',south:'false',up:'false',west:'false'}}",
			"{Name:'minecraft:fire',Properties:{age:'4',east:'false',north:'false',south:'false',up:'false',west:'false'}}",
			"{Name:'minecraft:fire',Properties:{age:'4',east:'false',north:'false',south:'false',up:'false',west:'true'}}",
			"{Name:'minecraft:fire',Properties:{age:'4',east:'false',north:'false',south:'false',up:'true',west:'false'}}",
			"{Name:'minecraft:fire',Properties:{age:'4',east:'false',north:'false',south:'false',up:'true',west:'true'}}",
			"{Name:'minecraft:fire',Properties:{age:'4',east:'false',north:'false',south:'true',up:'false',west:'false'}}",
			"{Name:'minecraft:fire',Properties:{age:'4',east:'false',north:'false',south:'true',up:'false',west:'true'}}",
			"{Name:'minecraft:fire',Properties:{age:'4',east:'false',north:'false',south:'true',up:'true',west:'false'}}",
			"{Name:'minecraft:fire',Properties:{age:'4',east:'false',north:'false',south:'true',up:'true',west:'true'}}",
			"{Name:'minecraft:fire',Properties:{age:'4',east:'false',north:'true',south:'false',up:'false',west:'false'}}",
			"{Name:'minecraft:fire',Properties:{age:'4',east:'false',north:'true',south:'false',up:'false',west:'true'}}",
			"{Name:'minecraft:fire',Properties:{age:'4',east:'false',north:'true',south:'false',up:'true',west:'false'}}",
			"{Name:'minecraft:fire',Properties:{age:'4',east:'false',north:'true',south:'false',up:'true',west:'true'}}",
			"{Name:'minecraft:fire',Properties:{age:'4',east:'false',north:'true',south:'true',up:'false',west:'false'}}",
			"{Name:'minecraft:fire',Properties:{age:'4',east:'false',north:'true',south:'true',up:'false',west:'true'}}",
			"{Name:'minecraft:fire',Properties:{age:'4',east:'false',north:'true',south:'true',up:'true',west:'false'}}",
			"{Name:'minecraft:fire',Properties:{age:'4',east:'false',north:'true',south:'true',up:'true',west:'true'}}",
			"{Name:'minecraft:fire',Properties:{age:'4',east:'true',north:'false',south:'false',up:'false',west:'false'}}",
			"{Name:'minecraft:fire',Properties:{age:'4',east:'true',north:'false',south:'false',up:'false',west:'true'}}",
			"{Name:'minecraft:fire',Properties:{age:'4',east:'true',north:'false',south:'false',up:'true',west:'false'}}",
			"{Name:'minecraft:fire',Properties:{age:'4',east:'true',north:'false',south:'false',up:'true',west:'true'}}",
			"{Name:'minecraft:fire',Properties:{age:'4',east:'true',north:'false',south:'true',up:'false',west:'false'}}",
			"{Name:'minecraft:fire',Properties:{age:'4',east:'true',north:'false',south:'true',up:'false',west:'true'}}",
			"{Name:'minecraft:fire',Properties:{age:'4',east:'true',north:'false',south:'true',up:'true',west:'false'}}",
			"{Name:'minecraft:fire',Properties:{age:'4',east:'true',north:'false',south:'true',up:'true',west:'true'}}",
			"{Name:'minecraft:fire',Properties:{age:'4',east:'true',north:'true',south:'false',up:'false',west:'false'}}",
			"{Name:'minecraft:fire',Properties:{age:'4',east:'true',north:'true',south:'false',up:'false',west:'true'}}",
			"{Name:'minecraft:fire',Properties:{age:'4',east:'true',north:'true',south:'false',up:'true',west:'false'}}",
			"{Name:'minecraft:fire',Properties:{age:'4',east:'true',north:'true',south:'false',up:'true',west:'true'}}",
			"{Name:'minecraft:fire',Properties:{age:'4',east:'true',north:'true',south:'true',up:'false',west:'false'}}",
			"{Name:'minecraft:fire',Properties:{age:'4',east:'true',north:'true',south:'true',up:'false',west:'true'}}",
			"{Name:'minecraft:fire',Properties:{age:'4',east:'true',north:'true',south:'true',up:'true',west:'false'}}",
			"{Name:'minecraft:fire',Properties:{age:'4',east:'true',north:'true',south:'true',up:'true',west:'true'}}"
		);
		putStates(
			821,
			"{Name:'minecraft:fire',Properties:{age:'5',east:'false',north:'false',south:'false',up:'false',west:'false'}}",
			"{Name:'minecraft:fire',Properties:{age:'5',east:'false',north:'false',south:'false',up:'false',west:'false'}}",
			"{Name:'minecraft:fire',Properties:{age:'5',east:'false',north:'false',south:'false',up:'false',west:'true'}}",
			"{Name:'minecraft:fire',Properties:{age:'5',east:'false',north:'false',south:'false',up:'true',west:'false'}}",
			"{Name:'minecraft:fire',Properties:{age:'5',east:'false',north:'false',south:'false',up:'true',west:'true'}}",
			"{Name:'minecraft:fire',Properties:{age:'5',east:'false',north:'false',south:'true',up:'false',west:'false'}}",
			"{Name:'minecraft:fire',Properties:{age:'5',east:'false',north:'false',south:'true',up:'false',west:'true'}}",
			"{Name:'minecraft:fire',Properties:{age:'5',east:'false',north:'false',south:'true',up:'true',west:'false'}}",
			"{Name:'minecraft:fire',Properties:{age:'5',east:'false',north:'false',south:'true',up:'true',west:'true'}}",
			"{Name:'minecraft:fire',Properties:{age:'5',east:'false',north:'true',south:'false',up:'false',west:'false'}}",
			"{Name:'minecraft:fire',Properties:{age:'5',east:'false',north:'true',south:'false',up:'false',west:'true'}}",
			"{Name:'minecraft:fire',Properties:{age:'5',east:'false',north:'true',south:'false',up:'true',west:'false'}}",
			"{Name:'minecraft:fire',Properties:{age:'5',east:'false',north:'true',south:'false',up:'true',west:'true'}}",
			"{Name:'minecraft:fire',Properties:{age:'5',east:'false',north:'true',south:'true',up:'false',west:'false'}}",
			"{Name:'minecraft:fire',Properties:{age:'5',east:'false',north:'true',south:'true',up:'false',west:'true'}}",
			"{Name:'minecraft:fire',Properties:{age:'5',east:'false',north:'true',south:'true',up:'true',west:'false'}}",
			"{Name:'minecraft:fire',Properties:{age:'5',east:'false',north:'true',south:'true',up:'true',west:'true'}}",
			"{Name:'minecraft:fire',Properties:{age:'5',east:'true',north:'false',south:'false',up:'false',west:'false'}}",
			"{Name:'minecraft:fire',Properties:{age:'5',east:'true',north:'false',south:'false',up:'false',west:'true'}}",
			"{Name:'minecraft:fire',Properties:{age:'5',east:'true',north:'false',south:'false',up:'true',west:'false'}}",
			"{Name:'minecraft:fire',Properties:{age:'5',east:'true',north:'false',south:'false',up:'true',west:'true'}}",
			"{Name:'minecraft:fire',Properties:{age:'5',east:'true',north:'false',south:'true',up:'false',west:'false'}}",
			"{Name:'minecraft:fire',Properties:{age:'5',east:'true',north:'false',south:'true',up:'false',west:'true'}}",
			"{Name:'minecraft:fire',Properties:{age:'5',east:'true',north:'false',south:'true',up:'true',west:'false'}}",
			"{Name:'minecraft:fire',Properties:{age:'5',east:'true',north:'false',south:'true',up:'true',west:'true'}}",
			"{Name:'minecraft:fire',Properties:{age:'5',east:'true',north:'true',south:'false',up:'false',west:'false'}}",
			"{Name:'minecraft:fire',Properties:{age:'5',east:'true',north:'true',south:'false',up:'false',west:'true'}}",
			"{Name:'minecraft:fire',Properties:{age:'5',east:'true',north:'true',south:'false',up:'true',west:'false'}}",
			"{Name:'minecraft:fire',Properties:{age:'5',east:'true',north:'true',south:'false',up:'true',west:'true'}}",
			"{Name:'minecraft:fire',Properties:{age:'5',east:'true',north:'true',south:'true',up:'false',west:'false'}}",
			"{Name:'minecraft:fire',Properties:{age:'5',east:'true',north:'true',south:'true',up:'false',west:'true'}}",
			"{Name:'minecraft:fire',Properties:{age:'5',east:'true',north:'true',south:'true',up:'true',west:'false'}}",
			"{Name:'minecraft:fire',Properties:{age:'5',east:'true',north:'true',south:'true',up:'true',west:'true'}}"
		);
		putStates(
			822,
			"{Name:'minecraft:fire',Properties:{age:'6',east:'false',north:'false',south:'false',up:'false',west:'false'}}",
			"{Name:'minecraft:fire',Properties:{age:'6',east:'false',north:'false',south:'false',up:'false',west:'false'}}",
			"{Name:'minecraft:fire',Properties:{age:'6',east:'false',north:'false',south:'false',up:'false',west:'true'}}",
			"{Name:'minecraft:fire',Properties:{age:'6',east:'false',north:'false',south:'false',up:'true',west:'false'}}",
			"{Name:'minecraft:fire',Properties:{age:'6',east:'false',north:'false',south:'false',up:'true',west:'true'}}",
			"{Name:'minecraft:fire',Properties:{age:'6',east:'false',north:'false',south:'true',up:'false',west:'false'}}",
			"{Name:'minecraft:fire',Properties:{age:'6',east:'false',north:'false',south:'true',up:'false',west:'true'}}",
			"{Name:'minecraft:fire',Properties:{age:'6',east:'false',north:'false',south:'true',up:'true',west:'false'}}",
			"{Name:'minecraft:fire',Properties:{age:'6',east:'false',north:'false',south:'true',up:'true',west:'true'}}",
			"{Name:'minecraft:fire',Properties:{age:'6',east:'false',north:'true',south:'false',up:'false',west:'false'}}",
			"{Name:'minecraft:fire',Properties:{age:'6',east:'false',north:'true',south:'false',up:'false',west:'true'}}",
			"{Name:'minecraft:fire',Properties:{age:'6',east:'false',north:'true',south:'false',up:'true',west:'false'}}",
			"{Name:'minecraft:fire',Properties:{age:'6',east:'false',north:'true',south:'false',up:'true',west:'true'}}",
			"{Name:'minecraft:fire',Properties:{age:'6',east:'false',north:'true',south:'true',up:'false',west:'false'}}",
			"{Name:'minecraft:fire',Properties:{age:'6',east:'false',north:'true',south:'true',up:'false',west:'true'}}",
			"{Name:'minecraft:fire',Properties:{age:'6',east:'false',north:'true',south:'true',up:'true',west:'false'}}",
			"{Name:'minecraft:fire',Properties:{age:'6',east:'false',north:'true',south:'true',up:'true',west:'true'}}",
			"{Name:'minecraft:fire',Properties:{age:'6',east:'true',north:'false',south:'false',up:'false',west:'false'}}",
			"{Name:'minecraft:fire',Properties:{age:'6',east:'true',north:'false',south:'false',up:'false',west:'true'}}",
			"{Name:'minecraft:fire',Properties:{age:'6',east:'true',north:'false',south:'false',up:'true',west:'false'}}",
			"{Name:'minecraft:fire',Properties:{age:'6',east:'true',north:'false',south:'false',up:'true',west:'true'}}",
			"{Name:'minecraft:fire',Properties:{age:'6',east:'true',north:'false',south:'true',up:'false',west:'false'}}",
			"{Name:'minecraft:fire',Properties:{age:'6',east:'true',north:'false',south:'true',up:'false',west:'true'}}",
			"{Name:'minecraft:fire',Properties:{age:'6',east:'true',north:'false',south:'true',up:'true',west:'false'}}",
			"{Name:'minecraft:fire',Properties:{age:'6',east:'true',north:'false',south:'true',up:'true',west:'true'}}",
			"{Name:'minecraft:fire',Properties:{age:'6',east:'true',north:'true',south:'false',up:'false',west:'false'}}",
			"{Name:'minecraft:fire',Properties:{age:'6',east:'true',north:'true',south:'false',up:'false',west:'true'}}",
			"{Name:'minecraft:fire',Properties:{age:'6',east:'true',north:'true',south:'false',up:'true',west:'false'}}",
			"{Name:'minecraft:fire',Properties:{age:'6',east:'true',north:'true',south:'false',up:'true',west:'true'}}",
			"{Name:'minecraft:fire',Properties:{age:'6',east:'true',north:'true',south:'true',up:'false',west:'false'}}",
			"{Name:'minecraft:fire',Properties:{age:'6',east:'true',north:'true',south:'true',up:'false',west:'true'}}",
			"{Name:'minecraft:fire',Properties:{age:'6',east:'true',north:'true',south:'true',up:'true',west:'false'}}",
			"{Name:'minecraft:fire',Properties:{age:'6',east:'true',north:'true',south:'true',up:'true',west:'true'}}"
		);
		putStates(
			823,
			"{Name:'minecraft:fire',Properties:{age:'7',east:'false',north:'false',south:'false',up:'false',west:'false'}}",
			"{Name:'minecraft:fire',Properties:{age:'7',east:'false',north:'false',south:'false',up:'false',west:'false'}}",
			"{Name:'minecraft:fire',Properties:{age:'7',east:'false',north:'false',south:'false',up:'false',west:'true'}}",
			"{Name:'minecraft:fire',Properties:{age:'7',east:'false',north:'false',south:'false',up:'true',west:'false'}}",
			"{Name:'minecraft:fire',Properties:{age:'7',east:'false',north:'false',south:'false',up:'true',west:'true'}}",
			"{Name:'minecraft:fire',Properties:{age:'7',east:'false',north:'false',south:'true',up:'false',west:'false'}}",
			"{Name:'minecraft:fire',Properties:{age:'7',east:'false',north:'false',south:'true',up:'false',west:'true'}}",
			"{Name:'minecraft:fire',Properties:{age:'7',east:'false',north:'false',south:'true',up:'true',west:'false'}}",
			"{Name:'minecraft:fire',Properties:{age:'7',east:'false',north:'false',south:'true',up:'true',west:'true'}}",
			"{Name:'minecraft:fire',Properties:{age:'7',east:'false',north:'true',south:'false',up:'false',west:'false'}}",
			"{Name:'minecraft:fire',Properties:{age:'7',east:'false',north:'true',south:'false',up:'false',west:'true'}}",
			"{Name:'minecraft:fire',Properties:{age:'7',east:'false',north:'true',south:'false',up:'true',west:'false'}}",
			"{Name:'minecraft:fire',Properties:{age:'7',east:'false',north:'true',south:'false',up:'true',west:'true'}}",
			"{Name:'minecraft:fire',Properties:{age:'7',east:'false',north:'true',south:'true',up:'false',west:'false'}}",
			"{Name:'minecraft:fire',Properties:{age:'7',east:'false',north:'true',south:'true',up:'false',west:'true'}}",
			"{Name:'minecraft:fire',Properties:{age:'7',east:'false',north:'true',south:'true',up:'true',west:'false'}}",
			"{Name:'minecraft:fire',Properties:{age:'7',east:'false',north:'true',south:'true',up:'true',west:'true'}}",
			"{Name:'minecraft:fire',Properties:{age:'7',east:'true',north:'false',south:'false',up:'false',west:'false'}}",
			"{Name:'minecraft:fire',Properties:{age:'7',east:'true',north:'false',south:'false',up:'false',west:'true'}}",
			"{Name:'minecraft:fire',Properties:{age:'7',east:'true',north:'false',south:'false',up:'true',west:'false'}}",
			"{Name:'minecraft:fire',Properties:{age:'7',east:'true',north:'false',south:'false',up:'true',west:'true'}}",
			"{Name:'minecraft:fire',Properties:{age:'7',east:'true',north:'false',south:'true',up:'false',west:'false'}}",
			"{Name:'minecraft:fire',Properties:{age:'7',east:'true',north:'false',south:'true',up:'false',west:'true'}}",
			"{Name:'minecraft:fire',Properties:{age:'7',east:'true',north:'false',south:'true',up:'true',west:'false'}}",
			"{Name:'minecraft:fire',Properties:{age:'7',east:'true',north:'false',south:'true',up:'true',west:'true'}}",
			"{Name:'minecraft:fire',Properties:{age:'7',east:'true',north:'true',south:'false',up:'false',west:'false'}}",
			"{Name:'minecraft:fire',Properties:{age:'7',east:'true',north:'true',south:'false',up:'false',west:'true'}}",
			"{Name:'minecraft:fire',Properties:{age:'7',east:'true',north:'true',south:'false',up:'true',west:'false'}}",
			"{Name:'minecraft:fire',Properties:{age:'7',east:'true',north:'true',south:'false',up:'true',west:'true'}}",
			"{Name:'minecraft:fire',Properties:{age:'7',east:'true',north:'true',south:'true',up:'false',west:'false'}}",
			"{Name:'minecraft:fire',Properties:{age:'7',east:'true',north:'true',south:'true',up:'false',west:'true'}}",
			"{Name:'minecraft:fire',Properties:{age:'7',east:'true',north:'true',south:'true',up:'true',west:'false'}}",
			"{Name:'minecraft:fire',Properties:{age:'7',east:'true',north:'true',south:'true',up:'true',west:'true'}}"
		);
		putStates(
			824,
			"{Name:'minecraft:fire',Properties:{age:'8',east:'false',north:'false',south:'false',up:'false',west:'false'}}",
			"{Name:'minecraft:fire',Properties:{age:'8',east:'false',north:'false',south:'false',up:'false',west:'false'}}",
			"{Name:'minecraft:fire',Properties:{age:'8',east:'false',north:'false',south:'false',up:'false',west:'true'}}",
			"{Name:'minecraft:fire',Properties:{age:'8',east:'false',north:'false',south:'false',up:'true',west:'false'}}",
			"{Name:'minecraft:fire',Properties:{age:'8',east:'false',north:'false',south:'false',up:'true',west:'true'}}",
			"{Name:'minecraft:fire',Properties:{age:'8',east:'false',north:'false',south:'true',up:'false',west:'false'}}",
			"{Name:'minecraft:fire',Properties:{age:'8',east:'false',north:'false',south:'true',up:'false',west:'true'}}",
			"{Name:'minecraft:fire',Properties:{age:'8',east:'false',north:'false',south:'true',up:'true',west:'false'}}",
			"{Name:'minecraft:fire',Properties:{age:'8',east:'false',north:'false',south:'true',up:'true',west:'true'}}",
			"{Name:'minecraft:fire',Properties:{age:'8',east:'false',north:'true',south:'false',up:'false',west:'false'}}",
			"{Name:'minecraft:fire',Properties:{age:'8',east:'false',north:'true',south:'false',up:'false',west:'true'}}",
			"{Name:'minecraft:fire',Properties:{age:'8',east:'false',north:'true',south:'false',up:'true',west:'false'}}",
			"{Name:'minecraft:fire',Properties:{age:'8',east:'false',north:'true',south:'false',up:'true',west:'true'}}",
			"{Name:'minecraft:fire',Properties:{age:'8',east:'false',north:'true',south:'true',up:'false',west:'false'}}",
			"{Name:'minecraft:fire',Properties:{age:'8',east:'false',north:'true',south:'true',up:'false',west:'true'}}",
			"{Name:'minecraft:fire',Properties:{age:'8',east:'false',north:'true',south:'true',up:'true',west:'false'}}",
			"{Name:'minecraft:fire',Properties:{age:'8',east:'false',north:'true',south:'true',up:'true',west:'true'}}",
			"{Name:'minecraft:fire',Properties:{age:'8',east:'true',north:'false',south:'false',up:'false',west:'false'}}",
			"{Name:'minecraft:fire',Properties:{age:'8',east:'true',north:'false',south:'false',up:'false',west:'true'}}",
			"{Name:'minecraft:fire',Properties:{age:'8',east:'true',north:'false',south:'false',up:'true',west:'false'}}",
			"{Name:'minecraft:fire',Properties:{age:'8',east:'true',north:'false',south:'false',up:'true',west:'true'}}",
			"{Name:'minecraft:fire',Properties:{age:'8',east:'true',north:'false',south:'true',up:'false',west:'false'}}",
			"{Name:'minecraft:fire',Properties:{age:'8',east:'true',north:'false',south:'true',up:'false',west:'true'}}",
			"{Name:'minecraft:fire',Properties:{age:'8',east:'true',north:'false',south:'true',up:'true',west:'false'}}",
			"{Name:'minecraft:fire',Properties:{age:'8',east:'true',north:'false',south:'true',up:'true',west:'true'}}",
			"{Name:'minecraft:fire',Properties:{age:'8',east:'true',north:'true',south:'false',up:'false',west:'false'}}",
			"{Name:'minecraft:fire',Properties:{age:'8',east:'true',north:'true',south:'false',up:'false',west:'true'}}",
			"{Name:'minecraft:fire',Properties:{age:'8',east:'true',north:'true',south:'false',up:'true',west:'false'}}",
			"{Name:'minecraft:fire',Properties:{age:'8',east:'true',north:'true',south:'false',up:'true',west:'true'}}",
			"{Name:'minecraft:fire',Properties:{age:'8',east:'true',north:'true',south:'true',up:'false',west:'false'}}",
			"{Name:'minecraft:fire',Properties:{age:'8',east:'true',north:'true',south:'true',up:'false',west:'true'}}",
			"{Name:'minecraft:fire',Properties:{age:'8',east:'true',north:'true',south:'true',up:'true',west:'false'}}",
			"{Name:'minecraft:fire',Properties:{age:'8',east:'true',north:'true',south:'true',up:'true',west:'true'}}"
		);
		putStates(
			825,
			"{Name:'minecraft:fire',Properties:{age:'9',east:'false',north:'false',south:'false',up:'false',west:'false'}}",
			"{Name:'minecraft:fire',Properties:{age:'9',east:'false',north:'false',south:'false',up:'false',west:'false'}}",
			"{Name:'minecraft:fire',Properties:{age:'9',east:'false',north:'false',south:'false',up:'false',west:'true'}}",
			"{Name:'minecraft:fire',Properties:{age:'9',east:'false',north:'false',south:'false',up:'true',west:'false'}}",
			"{Name:'minecraft:fire',Properties:{age:'9',east:'false',north:'false',south:'false',up:'true',west:'true'}}",
			"{Name:'minecraft:fire',Properties:{age:'9',east:'false',north:'false',south:'true',up:'false',west:'false'}}",
			"{Name:'minecraft:fire',Properties:{age:'9',east:'false',north:'false',south:'true',up:'false',west:'true'}}",
			"{Name:'minecraft:fire',Properties:{age:'9',east:'false',north:'false',south:'true',up:'true',west:'false'}}",
			"{Name:'minecraft:fire',Properties:{age:'9',east:'false',north:'false',south:'true',up:'true',west:'true'}}",
			"{Name:'minecraft:fire',Properties:{age:'9',east:'false',north:'true',south:'false',up:'false',west:'false'}}",
			"{Name:'minecraft:fire',Properties:{age:'9',east:'false',north:'true',south:'false',up:'false',west:'true'}}",
			"{Name:'minecraft:fire',Properties:{age:'9',east:'false',north:'true',south:'false',up:'true',west:'false'}}",
			"{Name:'minecraft:fire',Properties:{age:'9',east:'false',north:'true',south:'false',up:'true',west:'true'}}",
			"{Name:'minecraft:fire',Properties:{age:'9',east:'false',north:'true',south:'true',up:'false',west:'false'}}",
			"{Name:'minecraft:fire',Properties:{age:'9',east:'false',north:'true',south:'true',up:'false',west:'true'}}",
			"{Name:'minecraft:fire',Properties:{age:'9',east:'false',north:'true',south:'true',up:'true',west:'false'}}",
			"{Name:'minecraft:fire',Properties:{age:'9',east:'false',north:'true',south:'true',up:'true',west:'true'}}",
			"{Name:'minecraft:fire',Properties:{age:'9',east:'true',north:'false',south:'false',up:'false',west:'false'}}",
			"{Name:'minecraft:fire',Properties:{age:'9',east:'true',north:'false',south:'false',up:'false',west:'true'}}",
			"{Name:'minecraft:fire',Properties:{age:'9',east:'true',north:'false',south:'false',up:'true',west:'false'}}",
			"{Name:'minecraft:fire',Properties:{age:'9',east:'true',north:'false',south:'false',up:'true',west:'true'}}",
			"{Name:'minecraft:fire',Properties:{age:'9',east:'true',north:'false',south:'true',up:'false',west:'false'}}",
			"{Name:'minecraft:fire',Properties:{age:'9',east:'true',north:'false',south:'true',up:'false',west:'true'}}",
			"{Name:'minecraft:fire',Properties:{age:'9',east:'true',north:'false',south:'true',up:'true',west:'false'}}",
			"{Name:'minecraft:fire',Properties:{age:'9',east:'true',north:'false',south:'true',up:'true',west:'true'}}",
			"{Name:'minecraft:fire',Properties:{age:'9',east:'true',north:'true',south:'false',up:'false',west:'false'}}",
			"{Name:'minecraft:fire',Properties:{age:'9',east:'true',north:'true',south:'false',up:'false',west:'true'}}",
			"{Name:'minecraft:fire',Properties:{age:'9',east:'true',north:'true',south:'false',up:'true',west:'false'}}",
			"{Name:'minecraft:fire',Properties:{age:'9',east:'true',north:'true',south:'false',up:'true',west:'true'}}",
			"{Name:'minecraft:fire',Properties:{age:'9',east:'true',north:'true',south:'true',up:'false',west:'false'}}",
			"{Name:'minecraft:fire',Properties:{age:'9',east:'true',north:'true',south:'true',up:'false',west:'true'}}",
			"{Name:'minecraft:fire',Properties:{age:'9',east:'true',north:'true',south:'true',up:'true',west:'false'}}",
			"{Name:'minecraft:fire',Properties:{age:'9',east:'true',north:'true',south:'true',up:'true',west:'true'}}"
		);
		putStates(
			826,
			"{Name:'minecraft:fire',Properties:{age:'10',east:'false',north:'false',south:'false',up:'false',west:'false'}}",
			"{Name:'minecraft:fire',Properties:{age:'10',east:'false',north:'false',south:'false',up:'false',west:'false'}}",
			"{Name:'minecraft:fire',Properties:{age:'10',east:'false',north:'false',south:'false',up:'false',west:'true'}}",
			"{Name:'minecraft:fire',Properties:{age:'10',east:'false',north:'false',south:'false',up:'true',west:'false'}}",
			"{Name:'minecraft:fire',Properties:{age:'10',east:'false',north:'false',south:'false',up:'true',west:'true'}}",
			"{Name:'minecraft:fire',Properties:{age:'10',east:'false',north:'false',south:'true',up:'false',west:'false'}}",
			"{Name:'minecraft:fire',Properties:{age:'10',east:'false',north:'false',south:'true',up:'false',west:'true'}}",
			"{Name:'minecraft:fire',Properties:{age:'10',east:'false',north:'false',south:'true',up:'true',west:'false'}}",
			"{Name:'minecraft:fire',Properties:{age:'10',east:'false',north:'false',south:'true',up:'true',west:'true'}}",
			"{Name:'minecraft:fire',Properties:{age:'10',east:'false',north:'true',south:'false',up:'false',west:'false'}}",
			"{Name:'minecraft:fire',Properties:{age:'10',east:'false',north:'true',south:'false',up:'false',west:'true'}}",
			"{Name:'minecraft:fire',Properties:{age:'10',east:'false',north:'true',south:'false',up:'true',west:'false'}}",
			"{Name:'minecraft:fire',Properties:{age:'10',east:'false',north:'true',south:'false',up:'true',west:'true'}}",
			"{Name:'minecraft:fire',Properties:{age:'10',east:'false',north:'true',south:'true',up:'false',west:'false'}}",
			"{Name:'minecraft:fire',Properties:{age:'10',east:'false',north:'true',south:'true',up:'false',west:'true'}}",
			"{Name:'minecraft:fire',Properties:{age:'10',east:'false',north:'true',south:'true',up:'true',west:'false'}}",
			"{Name:'minecraft:fire',Properties:{age:'10',east:'false',north:'true',south:'true',up:'true',west:'true'}}",
			"{Name:'minecraft:fire',Properties:{age:'10',east:'true',north:'false',south:'false',up:'false',west:'false'}}",
			"{Name:'minecraft:fire',Properties:{age:'10',east:'true',north:'false',south:'false',up:'false',west:'true'}}",
			"{Name:'minecraft:fire',Properties:{age:'10',east:'true',north:'false',south:'false',up:'true',west:'false'}}",
			"{Name:'minecraft:fire',Properties:{age:'10',east:'true',north:'false',south:'false',up:'true',west:'true'}}",
			"{Name:'minecraft:fire',Properties:{age:'10',east:'true',north:'false',south:'true',up:'false',west:'false'}}",
			"{Name:'minecraft:fire',Properties:{age:'10',east:'true',north:'false',south:'true',up:'false',west:'true'}}",
			"{Name:'minecraft:fire',Properties:{age:'10',east:'true',north:'false',south:'true',up:'true',west:'false'}}",
			"{Name:'minecraft:fire',Properties:{age:'10',east:'true',north:'false',south:'true',up:'true',west:'true'}}",
			"{Name:'minecraft:fire',Properties:{age:'10',east:'true',north:'true',south:'false',up:'false',west:'false'}}",
			"{Name:'minecraft:fire',Properties:{age:'10',east:'true',north:'true',south:'false',up:'false',west:'true'}}",
			"{Name:'minecraft:fire',Properties:{age:'10',east:'true',north:'true',south:'false',up:'true',west:'false'}}",
			"{Name:'minecraft:fire',Properties:{age:'10',east:'true',north:'true',south:'false',up:'true',west:'true'}}",
			"{Name:'minecraft:fire',Properties:{age:'10',east:'true',north:'true',south:'true',up:'false',west:'false'}}",
			"{Name:'minecraft:fire',Properties:{age:'10',east:'true',north:'true',south:'true',up:'false',west:'true'}}",
			"{Name:'minecraft:fire',Properties:{age:'10',east:'true',north:'true',south:'true',up:'true',west:'false'}}",
			"{Name:'minecraft:fire',Properties:{age:'10',east:'true',north:'true',south:'true',up:'true',west:'true'}}"
		);
		putStates(
			827,
			"{Name:'minecraft:fire',Properties:{age:'11',east:'false',north:'false',south:'false',up:'false',west:'false'}}",
			"{Name:'minecraft:fire',Properties:{age:'11',east:'false',north:'false',south:'false',up:'false',west:'false'}}",
			"{Name:'minecraft:fire',Properties:{age:'11',east:'false',north:'false',south:'false',up:'false',west:'true'}}",
			"{Name:'minecraft:fire',Properties:{age:'11',east:'false',north:'false',south:'false',up:'true',west:'false'}}",
			"{Name:'minecraft:fire',Properties:{age:'11',east:'false',north:'false',south:'false',up:'true',west:'true'}}",
			"{Name:'minecraft:fire',Properties:{age:'11',east:'false',north:'false',south:'true',up:'false',west:'false'}}",
			"{Name:'minecraft:fire',Properties:{age:'11',east:'false',north:'false',south:'true',up:'false',west:'true'}}",
			"{Name:'minecraft:fire',Properties:{age:'11',east:'false',north:'false',south:'true',up:'true',west:'false'}}",
			"{Name:'minecraft:fire',Properties:{age:'11',east:'false',north:'false',south:'true',up:'true',west:'true'}}",
			"{Name:'minecraft:fire',Properties:{age:'11',east:'false',north:'true',south:'false',up:'false',west:'false'}}",
			"{Name:'minecraft:fire',Properties:{age:'11',east:'false',north:'true',south:'false',up:'false',west:'true'}}",
			"{Name:'minecraft:fire',Properties:{age:'11',east:'false',north:'true',south:'false',up:'true',west:'false'}}",
			"{Name:'minecraft:fire',Properties:{age:'11',east:'false',north:'true',south:'false',up:'true',west:'true'}}",
			"{Name:'minecraft:fire',Properties:{age:'11',east:'false',north:'true',south:'true',up:'false',west:'false'}}",
			"{Name:'minecraft:fire',Properties:{age:'11',east:'false',north:'true',south:'true',up:'false',west:'true'}}",
			"{Name:'minecraft:fire',Properties:{age:'11',east:'false',north:'true',south:'true',up:'true',west:'false'}}",
			"{Name:'minecraft:fire',Properties:{age:'11',east:'false',north:'true',south:'true',up:'true',west:'true'}}",
			"{Name:'minecraft:fire',Properties:{age:'11',east:'true',north:'false',south:'false',up:'false',west:'false'}}",
			"{Name:'minecraft:fire',Properties:{age:'11',east:'true',north:'false',south:'false',up:'false',west:'true'}}",
			"{Name:'minecraft:fire',Properties:{age:'11',east:'true',north:'false',south:'false',up:'true',west:'false'}}",
			"{Name:'minecraft:fire',Properties:{age:'11',east:'true',north:'false',south:'false',up:'true',west:'true'}}",
			"{Name:'minecraft:fire',Properties:{age:'11',east:'true',north:'false',south:'true',up:'false',west:'false'}}",
			"{Name:'minecraft:fire',Properties:{age:'11',east:'true',north:'false',south:'true',up:'false',west:'true'}}",
			"{Name:'minecraft:fire',Properties:{age:'11',east:'true',north:'false',south:'true',up:'true',west:'false'}}",
			"{Name:'minecraft:fire',Properties:{age:'11',east:'true',north:'false',south:'true',up:'true',west:'true'}}",
			"{Name:'minecraft:fire',Properties:{age:'11',east:'true',north:'true',south:'false',up:'false',west:'false'}}",
			"{Name:'minecraft:fire',Properties:{age:'11',east:'true',north:'true',south:'false',up:'false',west:'true'}}",
			"{Name:'minecraft:fire',Properties:{age:'11',east:'true',north:'true',south:'false',up:'true',west:'false'}}",
			"{Name:'minecraft:fire',Properties:{age:'11',east:'true',north:'true',south:'false',up:'true',west:'true'}}",
			"{Name:'minecraft:fire',Properties:{age:'11',east:'true',north:'true',south:'true',up:'false',west:'false'}}",
			"{Name:'minecraft:fire',Properties:{age:'11',east:'true',north:'true',south:'true',up:'false',west:'true'}}",
			"{Name:'minecraft:fire',Properties:{age:'11',east:'true',north:'true',south:'true',up:'true',west:'false'}}",
			"{Name:'minecraft:fire',Properties:{age:'11',east:'true',north:'true',south:'true',up:'true',west:'true'}}"
		);
		putStates(
			828,
			"{Name:'minecraft:fire',Properties:{age:'12',east:'false',north:'false',south:'false',up:'false',west:'false'}}",
			"{Name:'minecraft:fire',Properties:{age:'12',east:'false',north:'false',south:'false',up:'false',west:'false'}}",
			"{Name:'minecraft:fire',Properties:{age:'12',east:'false',north:'false',south:'false',up:'false',west:'true'}}",
			"{Name:'minecraft:fire',Properties:{age:'12',east:'false',north:'false',south:'false',up:'true',west:'false'}}",
			"{Name:'minecraft:fire',Properties:{age:'12',east:'false',north:'false',south:'false',up:'true',west:'true'}}",
			"{Name:'minecraft:fire',Properties:{age:'12',east:'false',north:'false',south:'true',up:'false',west:'false'}}",
			"{Name:'minecraft:fire',Properties:{age:'12',east:'false',north:'false',south:'true',up:'false',west:'true'}}",
			"{Name:'minecraft:fire',Properties:{age:'12',east:'false',north:'false',south:'true',up:'true',west:'false'}}",
			"{Name:'minecraft:fire',Properties:{age:'12',east:'false',north:'false',south:'true',up:'true',west:'true'}}",
			"{Name:'minecraft:fire',Properties:{age:'12',east:'false',north:'true',south:'false',up:'false',west:'false'}}",
			"{Name:'minecraft:fire',Properties:{age:'12',east:'false',north:'true',south:'false',up:'false',west:'true'}}",
			"{Name:'minecraft:fire',Properties:{age:'12',east:'false',north:'true',south:'false',up:'true',west:'false'}}",
			"{Name:'minecraft:fire',Properties:{age:'12',east:'false',north:'true',south:'false',up:'true',west:'true'}}",
			"{Name:'minecraft:fire',Properties:{age:'12',east:'false',north:'true',south:'true',up:'false',west:'false'}}",
			"{Name:'minecraft:fire',Properties:{age:'12',east:'false',north:'true',south:'true',up:'false',west:'true'}}",
			"{Name:'minecraft:fire',Properties:{age:'12',east:'false',north:'true',south:'true',up:'true',west:'false'}}",
			"{Name:'minecraft:fire',Properties:{age:'12',east:'false',north:'true',south:'true',up:'true',west:'true'}}",
			"{Name:'minecraft:fire',Properties:{age:'12',east:'true',north:'false',south:'false',up:'false',west:'false'}}",
			"{Name:'minecraft:fire',Properties:{age:'12',east:'true',north:'false',south:'false',up:'false',west:'true'}}",
			"{Name:'minecraft:fire',Properties:{age:'12',east:'true',north:'false',south:'false',up:'true',west:'false'}}",
			"{Name:'minecraft:fire',Properties:{age:'12',east:'true',north:'false',south:'false',up:'true',west:'true'}}",
			"{Name:'minecraft:fire',Properties:{age:'12',east:'true',north:'false',south:'true',up:'false',west:'false'}}",
			"{Name:'minecraft:fire',Properties:{age:'12',east:'true',north:'false',south:'true',up:'false',west:'true'}}",
			"{Name:'minecraft:fire',Properties:{age:'12',east:'true',north:'false',south:'true',up:'true',west:'false'}}",
			"{Name:'minecraft:fire',Properties:{age:'12',east:'true',north:'false',south:'true',up:'true',west:'true'}}",
			"{Name:'minecraft:fire',Properties:{age:'12',east:'true',north:'true',south:'false',up:'false',west:'false'}}",
			"{Name:'minecraft:fire',Properties:{age:'12',east:'true',north:'true',south:'false',up:'false',west:'true'}}",
			"{Name:'minecraft:fire',Properties:{age:'12',east:'true',north:'true',south:'false',up:'true',west:'false'}}",
			"{Name:'minecraft:fire',Properties:{age:'12',east:'true',north:'true',south:'false',up:'true',west:'true'}}",
			"{Name:'minecraft:fire',Properties:{age:'12',east:'true',north:'true',south:'true',up:'false',west:'false'}}",
			"{Name:'minecraft:fire',Properties:{age:'12',east:'true',north:'true',south:'true',up:'false',west:'true'}}",
			"{Name:'minecraft:fire',Properties:{age:'12',east:'true',north:'true',south:'true',up:'true',west:'false'}}",
			"{Name:'minecraft:fire',Properties:{age:'12',east:'true',north:'true',south:'true',up:'true',west:'true'}}"
		);
		putStates(
			829,
			"{Name:'minecraft:fire',Properties:{age:'13',east:'false',north:'false',south:'false',up:'false',west:'false'}}",
			"{Name:'minecraft:fire',Properties:{age:'13',east:'false',north:'false',south:'false',up:'false',west:'false'}}",
			"{Name:'minecraft:fire',Properties:{age:'13',east:'false',north:'false',south:'false',up:'false',west:'true'}}",
			"{Name:'minecraft:fire',Properties:{age:'13',east:'false',north:'false',south:'false',up:'true',west:'false'}}",
			"{Name:'minecraft:fire',Properties:{age:'13',east:'false',north:'false',south:'false',up:'true',west:'true'}}",
			"{Name:'minecraft:fire',Properties:{age:'13',east:'false',north:'false',south:'true',up:'false',west:'false'}}",
			"{Name:'minecraft:fire',Properties:{age:'13',east:'false',north:'false',south:'true',up:'false',west:'true'}}",
			"{Name:'minecraft:fire',Properties:{age:'13',east:'false',north:'false',south:'true',up:'true',west:'false'}}",
			"{Name:'minecraft:fire',Properties:{age:'13',east:'false',north:'false',south:'true',up:'true',west:'true'}}",
			"{Name:'minecraft:fire',Properties:{age:'13',east:'false',north:'true',south:'false',up:'false',west:'false'}}",
			"{Name:'minecraft:fire',Properties:{age:'13',east:'false',north:'true',south:'false',up:'false',west:'true'}}",
			"{Name:'minecraft:fire',Properties:{age:'13',east:'false',north:'true',south:'false',up:'true',west:'false'}}",
			"{Name:'minecraft:fire',Properties:{age:'13',east:'false',north:'true',south:'false',up:'true',west:'true'}}",
			"{Name:'minecraft:fire',Properties:{age:'13',east:'false',north:'true',south:'true',up:'false',west:'false'}}",
			"{Name:'minecraft:fire',Properties:{age:'13',east:'false',north:'true',south:'true',up:'false',west:'true'}}",
			"{Name:'minecraft:fire',Properties:{age:'13',east:'false',north:'true',south:'true',up:'true',west:'false'}}",
			"{Name:'minecraft:fire',Properties:{age:'13',east:'false',north:'true',south:'true',up:'true',west:'true'}}",
			"{Name:'minecraft:fire',Properties:{age:'13',east:'true',north:'false',south:'false',up:'false',west:'false'}}",
			"{Name:'minecraft:fire',Properties:{age:'13',east:'true',north:'false',south:'false',up:'false',west:'true'}}",
			"{Name:'minecraft:fire',Properties:{age:'13',east:'true',north:'false',south:'false',up:'true',west:'false'}}",
			"{Name:'minecraft:fire',Properties:{age:'13',east:'true',north:'false',south:'false',up:'true',west:'true'}}",
			"{Name:'minecraft:fire',Properties:{age:'13',east:'true',north:'false',south:'true',up:'false',west:'false'}}",
			"{Name:'minecraft:fire',Properties:{age:'13',east:'true',north:'false',south:'true',up:'false',west:'true'}}",
			"{Name:'minecraft:fire',Properties:{age:'13',east:'true',north:'false',south:'true',up:'true',west:'false'}}",
			"{Name:'minecraft:fire',Properties:{age:'13',east:'true',north:'false',south:'true',up:'true',west:'true'}}",
			"{Name:'minecraft:fire',Properties:{age:'13',east:'true',north:'true',south:'false',up:'false',west:'false'}}",
			"{Name:'minecraft:fire',Properties:{age:'13',east:'true',north:'true',south:'false',up:'false',west:'true'}}",
			"{Name:'minecraft:fire',Properties:{age:'13',east:'true',north:'true',south:'false',up:'true',west:'false'}}",
			"{Name:'minecraft:fire',Properties:{age:'13',east:'true',north:'true',south:'false',up:'true',west:'true'}}",
			"{Name:'minecraft:fire',Properties:{age:'13',east:'true',north:'true',south:'true',up:'false',west:'false'}}",
			"{Name:'minecraft:fire',Properties:{age:'13',east:'true',north:'true',south:'true',up:'false',west:'true'}}",
			"{Name:'minecraft:fire',Properties:{age:'13',east:'true',north:'true',south:'true',up:'true',west:'false'}}",
			"{Name:'minecraft:fire',Properties:{age:'13',east:'true',north:'true',south:'true',up:'true',west:'true'}}"
		);
		putStates(
			830,
			"{Name:'minecraft:fire',Properties:{age:'14',east:'false',north:'false',south:'false',up:'false',west:'false'}}",
			"{Name:'minecraft:fire',Properties:{age:'14',east:'false',north:'false',south:'false',up:'false',west:'false'}}",
			"{Name:'minecraft:fire',Properties:{age:'14',east:'false',north:'false',south:'false',up:'false',west:'true'}}",
			"{Name:'minecraft:fire',Properties:{age:'14',east:'false',north:'false',south:'false',up:'true',west:'false'}}",
			"{Name:'minecraft:fire',Properties:{age:'14',east:'false',north:'false',south:'false',up:'true',west:'true'}}",
			"{Name:'minecraft:fire',Properties:{age:'14',east:'false',north:'false',south:'true',up:'false',west:'false'}}",
			"{Name:'minecraft:fire',Properties:{age:'14',east:'false',north:'false',south:'true',up:'false',west:'true'}}",
			"{Name:'minecraft:fire',Properties:{age:'14',east:'false',north:'false',south:'true',up:'true',west:'false'}}",
			"{Name:'minecraft:fire',Properties:{age:'14',east:'false',north:'false',south:'true',up:'true',west:'true'}}",
			"{Name:'minecraft:fire',Properties:{age:'14',east:'false',north:'true',south:'false',up:'false',west:'false'}}",
			"{Name:'minecraft:fire',Properties:{age:'14',east:'false',north:'true',south:'false',up:'false',west:'true'}}",
			"{Name:'minecraft:fire',Properties:{age:'14',east:'false',north:'true',south:'false',up:'true',west:'false'}}",
			"{Name:'minecraft:fire',Properties:{age:'14',east:'false',north:'true',south:'false',up:'true',west:'true'}}",
			"{Name:'minecraft:fire',Properties:{age:'14',east:'false',north:'true',south:'true',up:'false',west:'false'}}",
			"{Name:'minecraft:fire',Properties:{age:'14',east:'false',north:'true',south:'true',up:'false',west:'true'}}",
			"{Name:'minecraft:fire',Properties:{age:'14',east:'false',north:'true',south:'true',up:'true',west:'false'}}",
			"{Name:'minecraft:fire',Properties:{age:'14',east:'false',north:'true',south:'true',up:'true',west:'true'}}",
			"{Name:'minecraft:fire',Properties:{age:'14',east:'true',north:'false',south:'false',up:'false',west:'false'}}",
			"{Name:'minecraft:fire',Properties:{age:'14',east:'true',north:'false',south:'false',up:'false',west:'true'}}",
			"{Name:'minecraft:fire',Properties:{age:'14',east:'true',north:'false',south:'false',up:'true',west:'false'}}",
			"{Name:'minecraft:fire',Properties:{age:'14',east:'true',north:'false',south:'false',up:'true',west:'true'}}",
			"{Name:'minecraft:fire',Properties:{age:'14',east:'true',north:'false',south:'true',up:'false',west:'false'}}",
			"{Name:'minecraft:fire',Properties:{age:'14',east:'true',north:'false',south:'true',up:'false',west:'true'}}",
			"{Name:'minecraft:fire',Properties:{age:'14',east:'true',north:'false',south:'true',up:'true',west:'false'}}",
			"{Name:'minecraft:fire',Properties:{age:'14',east:'true',north:'false',south:'true',up:'true',west:'true'}}",
			"{Name:'minecraft:fire',Properties:{age:'14',east:'true',north:'true',south:'false',up:'false',west:'false'}}",
			"{Name:'minecraft:fire',Properties:{age:'14',east:'true',north:'true',south:'false',up:'false',west:'true'}}",
			"{Name:'minecraft:fire',Properties:{age:'14',east:'true',north:'true',south:'false',up:'true',west:'false'}}",
			"{Name:'minecraft:fire',Properties:{age:'14',east:'true',north:'true',south:'false',up:'true',west:'true'}}",
			"{Name:'minecraft:fire',Properties:{age:'14',east:'true',north:'true',south:'true',up:'false',west:'false'}}",
			"{Name:'minecraft:fire',Properties:{age:'14',east:'true',north:'true',south:'true',up:'false',west:'true'}}",
			"{Name:'minecraft:fire',Properties:{age:'14',east:'true',north:'true',south:'true',up:'true',west:'false'}}",
			"{Name:'minecraft:fire',Properties:{age:'14',east:'true',north:'true',south:'true',up:'true',west:'true'}}"
		);
		putStates(
			831,
			"{Name:'minecraft:fire',Properties:{age:'15',east:'false',north:'false',south:'false',up:'false',west:'false'}}",
			"{Name:'minecraft:fire',Properties:{age:'15',east:'false',north:'false',south:'false',up:'false',west:'false'}}",
			"{Name:'minecraft:fire',Properties:{age:'15',east:'false',north:'false',south:'false',up:'false',west:'true'}}",
			"{Name:'minecraft:fire',Properties:{age:'15',east:'false',north:'false',south:'false',up:'true',west:'false'}}",
			"{Name:'minecraft:fire',Properties:{age:'15',east:'false',north:'false',south:'false',up:'true',west:'true'}}",
			"{Name:'minecraft:fire',Properties:{age:'15',east:'false',north:'false',south:'true',up:'false',west:'false'}}",
			"{Name:'minecraft:fire',Properties:{age:'15',east:'false',north:'false',south:'true',up:'false',west:'true'}}",
			"{Name:'minecraft:fire',Properties:{age:'15',east:'false',north:'false',south:'true',up:'true',west:'false'}}",
			"{Name:'minecraft:fire',Properties:{age:'15',east:'false',north:'false',south:'true',up:'true',west:'true'}}",
			"{Name:'minecraft:fire',Properties:{age:'15',east:'false',north:'true',south:'false',up:'false',west:'false'}}",
			"{Name:'minecraft:fire',Properties:{age:'15',east:'false',north:'true',south:'false',up:'false',west:'true'}}",
			"{Name:'minecraft:fire',Properties:{age:'15',east:'false',north:'true',south:'false',up:'true',west:'false'}}",
			"{Name:'minecraft:fire',Properties:{age:'15',east:'false',north:'true',south:'false',up:'true',west:'true'}}",
			"{Name:'minecraft:fire',Properties:{age:'15',east:'false',north:'true',south:'true',up:'false',west:'false'}}",
			"{Name:'minecraft:fire',Properties:{age:'15',east:'false',north:'true',south:'true',up:'false',west:'true'}}",
			"{Name:'minecraft:fire',Properties:{age:'15',east:'false',north:'true',south:'true',up:'true',west:'false'}}",
			"{Name:'minecraft:fire',Properties:{age:'15',east:'false',north:'true',south:'true',up:'true',west:'true'}}",
			"{Name:'minecraft:fire',Properties:{age:'15',east:'true',north:'false',south:'false',up:'false',west:'false'}}",
			"{Name:'minecraft:fire',Properties:{age:'15',east:'true',north:'false',south:'false',up:'false',west:'true'}}",
			"{Name:'minecraft:fire',Properties:{age:'15',east:'true',north:'false',south:'false',up:'true',west:'false'}}",
			"{Name:'minecraft:fire',Properties:{age:'15',east:'true',north:'false',south:'false',up:'true',west:'true'}}",
			"{Name:'minecraft:fire',Properties:{age:'15',east:'true',north:'false',south:'true',up:'false',west:'false'}}",
			"{Name:'minecraft:fire',Properties:{age:'15',east:'true',north:'false',south:'true',up:'false',west:'true'}}",
			"{Name:'minecraft:fire',Properties:{age:'15',east:'true',north:'false',south:'true',up:'true',west:'false'}}",
			"{Name:'minecraft:fire',Properties:{age:'15',east:'true',north:'false',south:'true',up:'true',west:'true'}}",
			"{Name:'minecraft:fire',Properties:{age:'15',east:'true',north:'true',south:'false',up:'false',west:'false'}}",
			"{Name:'minecraft:fire',Properties:{age:'15',east:'true',north:'true',south:'false',up:'false',west:'true'}}",
			"{Name:'minecraft:fire',Properties:{age:'15',east:'true',north:'true',south:'false',up:'true',west:'false'}}",
			"{Name:'minecraft:fire',Properties:{age:'15',east:'true',north:'true',south:'false',up:'true',west:'true'}}",
			"{Name:'minecraft:fire',Properties:{age:'15',east:'true',north:'true',south:'true',up:'false',west:'false'}}",
			"{Name:'minecraft:fire',Properties:{age:'15',east:'true',north:'true',south:'true',up:'false',west:'true'}}",
			"{Name:'minecraft:fire',Properties:{age:'15',east:'true',north:'true',south:'true',up:'true',west:'false'}}",
			"{Name:'minecraft:fire',Properties:{age:'15',east:'true',north:'true',south:'true',up:'true',west:'true'}}"
		);
		putStates(832, "{Name:'minecraft:mob_spawner'}", "{Name:'minecraft:mob_spawner'}");
		putStates(
			848,
			"{Name:'minecraft:oak_stairs',Properties:{facing:'east',half:'bottom',shape:'straight'}}",
			"{Name:'minecraft:oak_stairs',Properties:{facing:'east',half:'bottom',shape:'inner_left'}}",
			"{Name:'minecraft:oak_stairs',Properties:{facing:'east',half:'bottom',shape:'inner_right'}}",
			"{Name:'minecraft:oak_stairs',Properties:{facing:'east',half:'bottom',shape:'outer_left'}}",
			"{Name:'minecraft:oak_stairs',Properties:{facing:'east',half:'bottom',shape:'outer_right'}}",
			"{Name:'minecraft:oak_stairs',Properties:{facing:'east',half:'bottom',shape:'straight'}}"
		);
		putStates(
			849,
			"{Name:'minecraft:oak_stairs',Properties:{facing:'west',half:'bottom',shape:'straight'}}",
			"{Name:'minecraft:oak_stairs',Properties:{facing:'west',half:'bottom',shape:'inner_left'}}",
			"{Name:'minecraft:oak_stairs',Properties:{facing:'west',half:'bottom',shape:'inner_right'}}",
			"{Name:'minecraft:oak_stairs',Properties:{facing:'west',half:'bottom',shape:'outer_left'}}",
			"{Name:'minecraft:oak_stairs',Properties:{facing:'west',half:'bottom',shape:'outer_right'}}",
			"{Name:'minecraft:oak_stairs',Properties:{facing:'west',half:'bottom',shape:'straight'}}"
		);
		putStates(
			850,
			"{Name:'minecraft:oak_stairs',Properties:{facing:'south',half:'bottom',shape:'straight'}}",
			"{Name:'minecraft:oak_stairs',Properties:{facing:'south',half:'bottom',shape:'inner_left'}}",
			"{Name:'minecraft:oak_stairs',Properties:{facing:'south',half:'bottom',shape:'inner_right'}}",
			"{Name:'minecraft:oak_stairs',Properties:{facing:'south',half:'bottom',shape:'outer_left'}}",
			"{Name:'minecraft:oak_stairs',Properties:{facing:'south',half:'bottom',shape:'outer_right'}}",
			"{Name:'minecraft:oak_stairs',Properties:{facing:'south',half:'bottom',shape:'straight'}}"
		);
		putStates(
			851,
			"{Name:'minecraft:oak_stairs',Properties:{facing:'north',half:'bottom',shape:'straight'}}",
			"{Name:'minecraft:oak_stairs',Properties:{facing:'north',half:'bottom',shape:'inner_left'}}",
			"{Name:'minecraft:oak_stairs',Properties:{facing:'north',half:'bottom',shape:'inner_right'}}",
			"{Name:'minecraft:oak_stairs',Properties:{facing:'north',half:'bottom',shape:'outer_left'}}",
			"{Name:'minecraft:oak_stairs',Properties:{facing:'north',half:'bottom',shape:'outer_right'}}",
			"{Name:'minecraft:oak_stairs',Properties:{facing:'north',half:'bottom',shape:'straight'}}"
		);
		putStates(
			852,
			"{Name:'minecraft:oak_stairs',Properties:{facing:'east',half:'top',shape:'straight'}}",
			"{Name:'minecraft:oak_stairs',Properties:{facing:'east',half:'top',shape:'inner_left'}}",
			"{Name:'minecraft:oak_stairs',Properties:{facing:'east',half:'top',shape:'inner_right'}}",
			"{Name:'minecraft:oak_stairs',Properties:{facing:'east',half:'top',shape:'outer_left'}}",
			"{Name:'minecraft:oak_stairs',Properties:{facing:'east',half:'top',shape:'outer_right'}}",
			"{Name:'minecraft:oak_stairs',Properties:{facing:'east',half:'top',shape:'straight'}}"
		);
		putStates(
			853,
			"{Name:'minecraft:oak_stairs',Properties:{facing:'west',half:'top',shape:'straight'}}",
			"{Name:'minecraft:oak_stairs',Properties:{facing:'west',half:'top',shape:'inner_left'}}",
			"{Name:'minecraft:oak_stairs',Properties:{facing:'west',half:'top',shape:'inner_right'}}",
			"{Name:'minecraft:oak_stairs',Properties:{facing:'west',half:'top',shape:'outer_left'}}",
			"{Name:'minecraft:oak_stairs',Properties:{facing:'west',half:'top',shape:'outer_right'}}",
			"{Name:'minecraft:oak_stairs',Properties:{facing:'west',half:'top',shape:'straight'}}"
		);
		putStates(
			854,
			"{Name:'minecraft:oak_stairs',Properties:{facing:'south',half:'top',shape:'straight'}}",
			"{Name:'minecraft:oak_stairs',Properties:{facing:'south',half:'top',shape:'inner_left'}}",
			"{Name:'minecraft:oak_stairs',Properties:{facing:'south',half:'top',shape:'inner_right'}}",
			"{Name:'minecraft:oak_stairs',Properties:{facing:'south',half:'top',shape:'outer_left'}}",
			"{Name:'minecraft:oak_stairs',Properties:{facing:'south',half:'top',shape:'outer_right'}}",
			"{Name:'minecraft:oak_stairs',Properties:{facing:'south',half:'top',shape:'straight'}}"
		);
		putStates(
			855,
			"{Name:'minecraft:oak_stairs',Properties:{facing:'north',half:'top',shape:'straight'}}",
			"{Name:'minecraft:oak_stairs',Properties:{facing:'north',half:'top',shape:'inner_left'}}",
			"{Name:'minecraft:oak_stairs',Properties:{facing:'north',half:'top',shape:'inner_right'}}",
			"{Name:'minecraft:oak_stairs',Properties:{facing:'north',half:'top',shape:'outer_left'}}",
			"{Name:'minecraft:oak_stairs',Properties:{facing:'north',half:'top',shape:'outer_right'}}",
			"{Name:'minecraft:oak_stairs',Properties:{facing:'north',half:'top',shape:'straight'}}"
		);
		putStates(866, "{Name:'minecraft:chest',Properties:{facing:'north',type:'single'}}", "{Name:'minecraft:chest',Properties:{facing:'north'}}");
		putStates(867, "{Name:'minecraft:chest',Properties:{facing:'south',type:'single'}}", "{Name:'minecraft:chest',Properties:{facing:'south'}}");
		putStates(868, "{Name:'minecraft:chest',Properties:{facing:'west',type:'single'}}", "{Name:'minecraft:chest',Properties:{facing:'west'}}");
		putStates(869, "{Name:'minecraft:chest',Properties:{facing:'east',type:'single'}}", "{Name:'minecraft:chest',Properties:{facing:'east'}}");
		putStates(
			880,
			"{Name:'minecraft:redstone_wire',Properties:{east:'none',north:'none',power:'0',south:'none',west:'none'}}",
			"{Name:'minecraft:redstone_wire',Properties:{east:'none',north:'none',power:'0',south:'none',west:'none'}}",
			"{Name:'minecraft:redstone_wire',Properties:{east:'none',north:'none',power:'0',south:'none',west:'side'}}",
			"{Name:'minecraft:redstone_wire',Properties:{east:'none',north:'none',power:'0',south:'none',west:'up'}}",
			"{Name:'minecraft:redstone_wire',Properties:{east:'none',north:'none',power:'0',south:'side',west:'none'}}",
			"{Name:'minecraft:redstone_wire',Properties:{east:'none',north:'none',power:'0',south:'side',west:'side'}}",
			"{Name:'minecraft:redstone_wire',Properties:{east:'none',north:'none',power:'0',south:'side',west:'up'}}",
			"{Name:'minecraft:redstone_wire',Properties:{east:'none',north:'none',power:'0',south:'up',west:'none'}}",
			"{Name:'minecraft:redstone_wire',Properties:{east:'none',north:'none',power:'0',south:'up',west:'side'}}",
			"{Name:'minecraft:redstone_wire',Properties:{east:'none',north:'none',power:'0',south:'up',west:'up'}}",
			"{Name:'minecraft:redstone_wire',Properties:{east:'none',north:'side',power:'0',south:'none',west:'none'}}",
			"{Name:'minecraft:redstone_wire',Properties:{east:'none',north:'side',power:'0',south:'none',west:'side'}}",
			"{Name:'minecraft:redstone_wire',Properties:{east:'none',north:'side',power:'0',south:'none',west:'up'}}",
			"{Name:'minecraft:redstone_wire',Properties:{east:'none',north:'side',power:'0',south:'side',west:'none'}}",
			"{Name:'minecraft:redstone_wire',Properties:{east:'none',north:'side',power:'0',south:'side',west:'side'}}",
			"{Name:'minecraft:redstone_wire',Properties:{east:'none',north:'side',power:'0',south:'side',west:'up'}}",
			"{Name:'minecraft:redstone_wire',Properties:{east:'none',north:'side',power:'0',south:'up',west:'none'}}",
			"{Name:'minecraft:redstone_wire',Properties:{east:'none',north:'side',power:'0',south:'up',west:'side'}}",
			"{Name:'minecraft:redstone_wire',Properties:{east:'none',north:'side',power:'0',south:'up',west:'up'}}",
			"{Name:'minecraft:redstone_wire',Properties:{east:'none',north:'up',power:'0',south:'none',west:'none'}}",
			"{Name:'minecraft:redstone_wire',Properties:{east:'none',north:'up',power:'0',south:'none',west:'side'}}",
			"{Name:'minecraft:redstone_wire',Properties:{east:'none',north:'up',power:'0',south:'none',west:'up'}}",
			"{Name:'minecraft:redstone_wire',Properties:{east:'none',north:'up',power:'0',south:'side',west:'none'}}",
			"{Name:'minecraft:redstone_wire',Properties:{east:'none',north:'up',power:'0',south:'side',west:'side'}}",
			"{Name:'minecraft:redstone_wire',Properties:{east:'none',north:'up',power:'0',south:'side',west:'up'}}",
			"{Name:'minecraft:redstone_wire',Properties:{east:'none',north:'up',power:'0',south:'up',west:'none'}}",
			"{Name:'minecraft:redstone_wire',Properties:{east:'none',north:'up',power:'0',south:'up',west:'side'}}",
			"{Name:'minecraft:redstone_wire',Properties:{east:'none',north:'up',power:'0',south:'up',west:'up'}}",
			"{Name:'minecraft:redstone_wire',Properties:{east:'side',north:'none',power:'0',south:'none',west:'none'}}",
			"{Name:'minecraft:redstone_wire',Properties:{east:'side',north:'none',power:'0',south:'none',west:'side'}}",
			"{Name:'minecraft:redstone_wire',Properties:{east:'side',north:'none',power:'0',south:'none',west:'up'}}",
			"{Name:'minecraft:redstone_wire',Properties:{east:'side',north:'none',power:'0',south:'side',west:'none'}}",
			"{Name:'minecraft:redstone_wire',Properties:{east:'side',north:'none',power:'0',south:'side',west:'side'}}",
			"{Name:'minecraft:redstone_wire',Properties:{east:'side',north:'none',power:'0',south:'side',west:'up'}}",
			"{Name:'minecraft:redstone_wire',Properties:{east:'side',north:'none',power:'0',south:'up',west:'none'}}",
			"{Name:'minecraft:redstone_wire',Properties:{east:'side',north:'none',power:'0',south:'up',west:'side'}}",
			"{Name:'minecraft:redstone_wire',Properties:{east:'side',north:'none',power:'0',south:'up',west:'up'}}",
			"{Name:'minecraft:redstone_wire',Properties:{east:'side',north:'side',power:'0',south:'none',west:'none'}}",
			"{Name:'minecraft:redstone_wire',Properties:{east:'side',north:'side',power:'0',south:'none',west:'side'}}",
			"{Name:'minecraft:redstone_wire',Properties:{east:'side',north:'side',power:'0',south:'none',west:'up'}}",
			"{Name:'minecraft:redstone_wire',Properties:{east:'side',north:'side',power:'0',south:'side',west:'none'}}",
			"{Name:'minecraft:redstone_wire',Properties:{east:'side',north:'side',power:'0',south:'side',west:'side'}}",
			"{Name:'minecraft:redstone_wire',Properties:{east:'side',north:'side',power:'0',south:'side',west:'up'}}",
			"{Name:'minecraft:redstone_wire',Properties:{east:'side',north:'side',power:'0',south:'up',west:'none'}}",
			"{Name:'minecraft:redstone_wire',Properties:{east:'side',north:'side',power:'0',south:'up',west:'side'}}",
			"{Name:'minecraft:redstone_wire',Properties:{east:'side',north:'side',power:'0',south:'up',west:'up'}}",
			"{Name:'minecraft:redstone_wire',Properties:{east:'side',north:'up',power:'0',south:'none',west:'none'}}",
			"{Name:'minecraft:redstone_wire',Properties:{east:'side',north:'up',power:'0',south:'none',west:'side'}}",
			"{Name:'minecraft:redstone_wire',Properties:{east:'side',north:'up',power:'0',south:'none',west:'up'}}",
			"{Name:'minecraft:redstone_wire',Properties:{east:'side',north:'up',power:'0',south:'side',west:'none'}}",
			"{Name:'minecraft:redstone_wire',Properties:{east:'side',north:'up',power:'0',south:'side',west:'side'}}",
			"{Name:'minecraft:redstone_wire',Properties:{east:'side',north:'up',power:'0',south:'side',west:'up'}}",
			"{Name:'minecraft:redstone_wire',Properties:{east:'side',north:'up',power:'0',south:'up',west:'none'}}",
			"{Name:'minecraft:redstone_wire',Properties:{east:'side',north:'up',power:'0',south:'up',west:'side'}}",
			"{Name:'minecraft:redstone_wire',Properties:{east:'side',north:'up',power:'0',south:'up',west:'up'}}",
			"{Name:'minecraft:redstone_wire',Properties:{east:'up',north:'none',power:'0',south:'none',west:'none'}}",
			"{Name:'minecraft:redstone_wire',Properties:{east:'up',north:'none',power:'0',south:'none',west:'side'}}",
			"{Name:'minecraft:redstone_wire',Properties:{east:'up',north:'none',power:'0',south:'none',west:'up'}}",
			"{Name:'minecraft:redstone_wire',Properties:{east:'up',north:'none',power:'0',south:'side',west:'none'}}",
			"{Name:'minecraft:redstone_wire',Properties:{east:'up',north:'none',power:'0',south:'side',west:'side'}}",
			"{Name:'minecraft:redstone_wire',Properties:{east:'up',north:'none',power:'0',south:'side',west:'up'}}",
			"{Name:'minecraft:redstone_wire',Properties:{east:'up',north:'none',power:'0',south:'up',west:'none'}}",
			"{Name:'minecraft:redstone_wire',Properties:{east:'up',north:'none',power:'0',south:'up',west:'side'}}",
			"{Name:'minecraft:redstone_wire',Properties:{east:'up',north:'none',power:'0',south:'up',west:'up'}}",
			"{Name:'minecraft:redstone_wire',Properties:{east:'up',north:'side',power:'0',south:'none',west:'none'}}",
			"{Name:'minecraft:redstone_wire',Properties:{east:'up',north:'side',power:'0',south:'none',west:'side'}}",
			"{Name:'minecraft:redstone_wire',Properties:{east:'up',north:'side',power:'0',south:'none',west:'up'}}",
			"{Name:'minecraft:redstone_wire',Properties:{east:'up',north:'side',power:'0',south:'side',west:'none'}}",
			"{Name:'minecraft:redstone_wire',Properties:{east:'up',north:'side',power:'0',south:'side',west:'side'}}",
			"{Name:'minecraft:redstone_wire',Properties:{east:'up',north:'side',power:'0',south:'side',west:'up'}}",
			"{Name:'minecraft:redstone_wire',Properties:{east:'up',north:'side',power:'0',south:'up',west:'none'}}",
			"{Name:'minecraft:redstone_wire',Properties:{east:'up',north:'side',power:'0',south:'up',west:'side'}}",
			"{Name:'minecraft:redstone_wire',Properties:{east:'up',north:'side',power:'0',south:'up',west:'up'}}",
			"{Name:'minecraft:redstone_wire',Properties:{east:'up',north:'up',power:'0',south:'none',west:'none'}}",
			"{Name:'minecraft:redstone_wire',Properties:{east:'up',north:'up',power:'0',south:'none',west:'side'}}",
			"{Name:'minecraft:redstone_wire',Properties:{east:'up',north:'up',power:'0',south:'none',west:'up'}}",
			"{Name:'minecraft:redstone_wire',Properties:{east:'up',north:'up',power:'0',south:'side',west:'none'}}",
			"{Name:'minecraft:redstone_wire',Properties:{east:'up',north:'up',power:'0',south:'side',west:'side'}}",
			"{Name:'minecraft:redstone_wire',Properties:{east:'up',north:'up',power:'0',south:'side',west:'up'}}",
			"{Name:'minecraft:redstone_wire',Properties:{east:'up',north:'up',power:'0',south:'up',west:'none'}}",
			"{Name:'minecraft:redstone_wire',Properties:{east:'up',north:'up',power:'0',south:'up',west:'side'}}",
			"{Name:'minecraft:redstone_wire',Properties:{east:'up',north:'up',power:'0',south:'up',west:'up'}}"
		);
		putStates(
			881,
			"{Name:'minecraft:redstone_wire',Properties:{east:'none',north:'none',power:'1',south:'none',west:'none'}}",
			"{Name:'minecraft:redstone_wire',Properties:{east:'none',north:'none',power:'1',south:'none',west:'none'}}",
			"{Name:'minecraft:redstone_wire',Properties:{east:'none',north:'none',power:'1',south:'none',west:'side'}}",
			"{Name:'minecraft:redstone_wire',Properties:{east:'none',north:'none',power:'1',south:'none',west:'up'}}",
			"{Name:'minecraft:redstone_wire',Properties:{east:'none',north:'none',power:'1',south:'side',west:'none'}}",
			"{Name:'minecraft:redstone_wire',Properties:{east:'none',north:'none',power:'1',south:'side',west:'side'}}",
			"{Name:'minecraft:redstone_wire',Properties:{east:'none',north:'none',power:'1',south:'side',west:'up'}}",
			"{Name:'minecraft:redstone_wire',Properties:{east:'none',north:'none',power:'1',south:'up',west:'none'}}",
			"{Name:'minecraft:redstone_wire',Properties:{east:'none',north:'none',power:'1',south:'up',west:'side'}}",
			"{Name:'minecraft:redstone_wire',Properties:{east:'none',north:'none',power:'1',south:'up',west:'up'}}",
			"{Name:'minecraft:redstone_wire',Properties:{east:'none',north:'side',power:'1',south:'none',west:'none'}}",
			"{Name:'minecraft:redstone_wire',Properties:{east:'none',north:'side',power:'1',south:'none',west:'side'}}",
			"{Name:'minecraft:redstone_wire',Properties:{east:'none',north:'side',power:'1',south:'none',west:'up'}}",
			"{Name:'minecraft:redstone_wire',Properties:{east:'none',north:'side',power:'1',south:'side',west:'none'}}",
			"{Name:'minecraft:redstone_wire',Properties:{east:'none',north:'side',power:'1',south:'side',west:'side'}}",
			"{Name:'minecraft:redstone_wire',Properties:{east:'none',north:'side',power:'1',south:'side',west:'up'}}",
			"{Name:'minecraft:redstone_wire',Properties:{east:'none',north:'side',power:'1',south:'up',west:'none'}}",
			"{Name:'minecraft:redstone_wire',Properties:{east:'none',north:'side',power:'1',south:'up',west:'side'}}",
			"{Name:'minecraft:redstone_wire',Properties:{east:'none',north:'side',power:'1',south:'up',west:'up'}}",
			"{Name:'minecraft:redstone_wire',Properties:{east:'none',north:'up',power:'1',south:'none',west:'none'}}",
			"{Name:'minecraft:redstone_wire',Properties:{east:'none',north:'up',power:'1',south:'none',west:'side'}}",
			"{Name:'minecraft:redstone_wire',Properties:{east:'none',north:'up',power:'1',south:'none',west:'up'}}",
			"{Name:'minecraft:redstone_wire',Properties:{east:'none',north:'up',power:'1',south:'side',west:'none'}}",
			"{Name:'minecraft:redstone_wire',Properties:{east:'none',north:'up',power:'1',south:'side',west:'side'}}",
			"{Name:'minecraft:redstone_wire',Properties:{east:'none',north:'up',power:'1',south:'side',west:'up'}}",
			"{Name:'minecraft:redstone_wire',Properties:{east:'none',north:'up',power:'1',south:'up',west:'none'}}",
			"{Name:'minecraft:redstone_wire',Properties:{east:'none',north:'up',power:'1',south:'up',west:'side'}}",
			"{Name:'minecraft:redstone_wire',Properties:{east:'none',north:'up',power:'1',south:'up',west:'up'}}",
			"{Name:'minecraft:redstone_wire',Properties:{east:'side',north:'none',power:'1',south:'none',west:'none'}}",
			"{Name:'minecraft:redstone_wire',Properties:{east:'side',north:'none',power:'1',south:'none',west:'side'}}",
			"{Name:'minecraft:redstone_wire',Properties:{east:'side',north:'none',power:'1',south:'none',west:'up'}}",
			"{Name:'minecraft:redstone_wire',Properties:{east:'side',north:'none',power:'1',south:'side',west:'none'}}",
			"{Name:'minecraft:redstone_wire',Properties:{east:'side',north:'none',power:'1',south:'side',west:'side'}}",
			"{Name:'minecraft:redstone_wire',Properties:{east:'side',north:'none',power:'1',south:'side',west:'up'}}",
			"{Name:'minecraft:redstone_wire',Properties:{east:'side',north:'none',power:'1',south:'up',west:'none'}}",
			"{Name:'minecraft:redstone_wire',Properties:{east:'side',north:'none',power:'1',south:'up',west:'side'}}",
			"{Name:'minecraft:redstone_wire',Properties:{east:'side',north:'none',power:'1',south:'up',west:'up'}}",
			"{Name:'minecraft:redstone_wire',Properties:{east:'side',north:'side',power:'1',south:'none',west:'none'}}",
			"{Name:'minecraft:redstone_wire',Properties:{east:'side',north:'side',power:'1',south:'none',west:'side'}}",
			"{Name:'minecraft:redstone_wire',Properties:{east:'side',north:'side',power:'1',south:'none',west:'up'}}",
			"{Name:'minecraft:redstone_wire',Properties:{east:'side',north:'side',power:'1',south:'side',west:'none'}}",
			"{Name:'minecraft:redstone_wire',Properties:{east:'side',north:'side',power:'1',south:'side',west:'side'}}",
			"{Name:'minecraft:redstone_wire',Properties:{east:'side',north:'side',power:'1',south:'side',west:'up'}}",
			"{Name:'minecraft:redstone_wire',Properties:{east:'side',north:'side',power:'1',south:'up',west:'none'}}",
			"{Name:'minecraft:redstone_wire',Properties:{east:'side',north:'side',power:'1',south:'up',west:'side'}}",
			"{Name:'minecraft:redstone_wire',Properties:{east:'side',north:'side',power:'1',south:'up',west:'up'}}",
			"{Name:'minecraft:redstone_wire',Properties:{east:'side',north:'up',power:'1',south:'none',west:'none'}}",
			"{Name:'minecraft:redstone_wire',Properties:{east:'side',north:'up',power:'1',south:'none',west:'side'}}",
			"{Name:'minecraft:redstone_wire',Properties:{east:'side',north:'up',power:'1',south:'none',west:'up'}}",
			"{Name:'minecraft:redstone_wire',Properties:{east:'side',north:'up',power:'1',south:'side',west:'none'}}",
			"{Name:'minecraft:redstone_wire',Properties:{east:'side',north:'up',power:'1',south:'side',west:'side'}}",
			"{Name:'minecraft:redstone_wire',Properties:{east:'side',north:'up',power:'1',south:'side',west:'up'}}",
			"{Name:'minecraft:redstone_wire',Properties:{east:'side',north:'up',power:'1',south:'up',west:'none'}}",
			"{Name:'minecraft:redstone_wire',Properties:{east:'side',north:'up',power:'1',south:'up',west:'side'}}",
			"{Name:'minecraft:redstone_wire',Properties:{east:'side',north:'up',power:'1',south:'up',west:'up'}}",
			"{Name:'minecraft:redstone_wire',Properties:{east:'up',north:'none',power:'1',south:'none',west:'none'}}",
			"{Name:'minecraft:redstone_wire',Properties:{east:'up',north:'none',power:'1',south:'none',west:'side'}}",
			"{Name:'minecraft:redstone_wire',Properties:{east:'up',north:'none',power:'1',south:'none',west:'up'}}",
			"{Name:'minecraft:redstone_wire',Properties:{east:'up',north:'none',power:'1',south:'side',west:'none'}}",
			"{Name:'minecraft:redstone_wire',Properties:{east:'up',north:'none',power:'1',south:'side',west:'side'}}",
			"{Name:'minecraft:redstone_wire',Properties:{east:'up',north:'none',power:'1',south:'side',west:'up'}}",
			"{Name:'minecraft:redstone_wire',Properties:{east:'up',north:'none',power:'1',south:'up',west:'none'}}",
			"{Name:'minecraft:redstone_wire',Properties:{east:'up',north:'none',power:'1',south:'up',west:'side'}}",
			"{Name:'minecraft:redstone_wire',Properties:{east:'up',north:'none',power:'1',south:'up',west:'up'}}",
			"{Name:'minecraft:redstone_wire',Properties:{east:'up',north:'side',power:'1',south:'none',west:'none'}}",
			"{Name:'minecraft:redstone_wire',Properties:{east:'up',north:'side',power:'1',south:'none',west:'side'}}",
			"{Name:'minecraft:redstone_wire',Properties:{east:'up',north:'side',power:'1',south:'none',west:'up'}}",
			"{Name:'minecraft:redstone_wire',Properties:{east:'up',north:'side',power:'1',south:'side',west:'none'}}",
			"{Name:'minecraft:redstone_wire',Properties:{east:'up',north:'side',power:'1',south:'side',west:'side'}}",
			"{Name:'minecraft:redstone_wire',Properties:{east:'up',north:'side',power:'1',south:'side',west:'up'}}",
			"{Name:'minecraft:redstone_wire',Properties:{east:'up',north:'side',power:'1',south:'up',west:'none'}}",
			"{Name:'minecraft:redstone_wire',Properties:{east:'up',north:'side',power:'1',south:'up',west:'side'}}",
			"{Name:'minecraft:redstone_wire',Properties:{east:'up',north:'side',power:'1',south:'up',west:'up'}}",
			"{Name:'minecraft:redstone_wire',Properties:{east:'up',north:'up',power:'1',south:'none',west:'none'}}",
			"{Name:'minecraft:redstone_wire',Properties:{east:'up',north:'up',power:'1',south:'none',west:'side'}}",
			"{Name:'minecraft:redstone_wire',Properties:{east:'up',north:'up',power:'1',south:'none',west:'up'}}",
			"{Name:'minecraft:redstone_wire',Properties:{east:'up',north:'up',power:'1',south:'side',west:'none'}}",
			"{Name:'minecraft:redstone_wire',Properties:{east:'up',north:'up',power:'1',south:'side',west:'side'}}",
			"{Name:'minecraft:redstone_wire',Properties:{east:'up',north:'up',power:'1',south:'side',west:'up'}}",
			"{Name:'minecraft:redstone_wire',Properties:{east:'up',north:'up',power:'1',south:'up',west:'none'}}",
			"{Name:'minecraft:redstone_wire',Properties:{east:'up',north:'up',power:'1',south:'up',west:'side'}}",
			"{Name:'minecraft:redstone_wire',Properties:{east:'up',north:'up',power:'1',south:'up',west:'up'}}"
		);
		putStates(
			882,
			"{Name:'minecraft:redstone_wire',Properties:{east:'none',north:'none',power:'2',south:'none',west:'none'}}",
			"{Name:'minecraft:redstone_wire',Properties:{east:'none',north:'none',power:'2',south:'none',west:'none'}}",
			"{Name:'minecraft:redstone_wire',Properties:{east:'none',north:'none',power:'2',south:'none',west:'side'}}",
			"{Name:'minecraft:redstone_wire',Properties:{east:'none',north:'none',power:'2',south:'none',west:'up'}}",
			"{Name:'minecraft:redstone_wire',Properties:{east:'none',north:'none',power:'2',south:'side',west:'none'}}",
			"{Name:'minecraft:redstone_wire',Properties:{east:'none',north:'none',power:'2',south:'side',west:'side'}}",
			"{Name:'minecraft:redstone_wire',Properties:{east:'none',north:'none',power:'2',south:'side',west:'up'}}",
			"{Name:'minecraft:redstone_wire',Properties:{east:'none',north:'none',power:'2',south:'up',west:'none'}}",
			"{Name:'minecraft:redstone_wire',Properties:{east:'none',north:'none',power:'2',south:'up',west:'side'}}",
			"{Name:'minecraft:redstone_wire',Properties:{east:'none',north:'none',power:'2',south:'up',west:'up'}}",
			"{Name:'minecraft:redstone_wire',Properties:{east:'none',north:'side',power:'2',south:'none',west:'none'}}",
			"{Name:'minecraft:redstone_wire',Properties:{east:'none',north:'side',power:'2',south:'none',west:'side'}}",
			"{Name:'minecraft:redstone_wire',Properties:{east:'none',north:'side',power:'2',south:'none',west:'up'}}",
			"{Name:'minecraft:redstone_wire',Properties:{east:'none',north:'side',power:'2',south:'side',west:'none'}}",
			"{Name:'minecraft:redstone_wire',Properties:{east:'none',north:'side',power:'2',south:'side',west:'side'}}",
			"{Name:'minecraft:redstone_wire',Properties:{east:'none',north:'side',power:'2',south:'side',west:'up'}}",
			"{Name:'minecraft:redstone_wire',Properties:{east:'none',north:'side',power:'2',south:'up',west:'none'}}",
			"{Name:'minecraft:redstone_wire',Properties:{east:'none',north:'side',power:'2',south:'up',west:'side'}}",
			"{Name:'minecraft:redstone_wire',Properties:{east:'none',north:'side',power:'2',south:'up',west:'up'}}",
			"{Name:'minecraft:redstone_wire',Properties:{east:'none',north:'up',power:'2',south:'none',west:'none'}}",
			"{Name:'minecraft:redstone_wire',Properties:{east:'none',north:'up',power:'2',south:'none',west:'side'}}",
			"{Name:'minecraft:redstone_wire',Properties:{east:'none',north:'up',power:'2',south:'none',west:'up'}}",
			"{Name:'minecraft:redstone_wire',Properties:{east:'none',north:'up',power:'2',south:'side',west:'none'}}",
			"{Name:'minecraft:redstone_wire',Properties:{east:'none',north:'up',power:'2',south:'side',west:'side'}}",
			"{Name:'minecraft:redstone_wire',Properties:{east:'none',north:'up',power:'2',south:'side',west:'up'}}",
			"{Name:'minecraft:redstone_wire',Properties:{east:'none',north:'up',power:'2',south:'up',west:'none'}}",
			"{Name:'minecraft:redstone_wire',Properties:{east:'none',north:'up',power:'2',south:'up',west:'side'}}",
			"{Name:'minecraft:redstone_wire',Properties:{east:'none',north:'up',power:'2',south:'up',west:'up'}}",
			"{Name:'minecraft:redstone_wire',Properties:{east:'side',north:'none',power:'2',south:'none',west:'none'}}",
			"{Name:'minecraft:redstone_wire',Properties:{east:'side',north:'none',power:'2',south:'none',west:'side'}}",
			"{Name:'minecraft:redstone_wire',Properties:{east:'side',north:'none',power:'2',south:'none',west:'up'}}",
			"{Name:'minecraft:redstone_wire',Properties:{east:'side',north:'none',power:'2',south:'side',west:'none'}}",
			"{Name:'minecraft:redstone_wire',Properties:{east:'side',north:'none',power:'2',south:'side',west:'side'}}",
			"{Name:'minecraft:redstone_wire',Properties:{east:'side',north:'none',power:'2',south:'side',west:'up'}}",
			"{Name:'minecraft:redstone_wire',Properties:{east:'side',north:'none',power:'2',south:'up',west:'none'}}",
			"{Name:'minecraft:redstone_wire',Properties:{east:'side',north:'none',power:'2',south:'up',west:'side'}}",
			"{Name:'minecraft:redstone_wire',Properties:{east:'side',north:'none',power:'2',south:'up',west:'up'}}",
			"{Name:'minecraft:redstone_wire',Properties:{east:'side',north:'side',power:'2',south:'none',west:'none'}}",
			"{Name:'minecraft:redstone_wire',Properties:{east:'side',north:'side',power:'2',south:'none',west:'side'}}",
			"{Name:'minecraft:redstone_wire',Properties:{east:'side',north:'side',power:'2',south:'none',west:'up'}}",
			"{Name:'minecraft:redstone_wire',Properties:{east:'side',north:'side',power:'2',south:'side',west:'none'}}",
			"{Name:'minecraft:redstone_wire',Properties:{east:'side',north:'side',power:'2',south:'side',west:'side'}}",
			"{Name:'minecraft:redstone_wire',Properties:{east:'side',north:'side',power:'2',south:'side',west:'up'}}",
			"{Name:'minecraft:redstone_wire',Properties:{east:'side',north:'side',power:'2',south:'up',west:'none'}}",
			"{Name:'minecraft:redstone_wire',Properties:{east:'side',north:'side',power:'2',south:'up',west:'side'}}",
			"{Name:'minecraft:redstone_wire',Properties:{east:'side',north:'side',power:'2',south:'up',west:'up'}}",
			"{Name:'minecraft:redstone_wire',Properties:{east:'side',north:'up',power:'2',south:'none',west:'none'}}",
			"{Name:'minecraft:redstone_wire',Properties:{east:'side',north:'up',power:'2',south:'none',west:'side'}}",
			"{Name:'minecraft:redstone_wire',Properties:{east:'side',north:'up',power:'2',south:'none',west:'up'}}",
			"{Name:'minecraft:redstone_wire',Properties:{east:'side',north:'up',power:'2',south:'side',west:'none'}}",
			"{Name:'minecraft:redstone_wire',Properties:{east:'side',north:'up',power:'2',south:'side',west:'side'}}",
			"{Name:'minecraft:redstone_wire',Properties:{east:'side',north:'up',power:'2',south:'side',west:'up'}}",
			"{Name:'minecraft:redstone_wire',Properties:{east:'side',north:'up',power:'2',south:'up',west:'none'}}",
			"{Name:'minecraft:redstone_wire',Properties:{east:'side',north:'up',power:'2',south:'up',west:'side'}}",
			"{Name:'minecraft:redstone_wire',Properties:{east:'side',north:'up',power:'2',south:'up',west:'up'}}",
			"{Name:'minecraft:redstone_wire',Properties:{east:'up',north:'none',power:'2',south:'none',west:'none'}}",
			"{Name:'minecraft:redstone_wire',Properties:{east:'up',north:'none',power:'2',south:'none',west:'side'}}",
			"{Name:'minecraft:redstone_wire',Properties:{east:'up',north:'none',power:'2',south:'none',west:'up'}}",
			"{Name:'minecraft:redstone_wire',Properties:{east:'up',north:'none',power:'2',south:'side',west:'none'}}",
			"{Name:'minecraft:redstone_wire',Properties:{east:'up',north:'none',power:'2',south:'side',west:'side'}}",
			"{Name:'minecraft:redstone_wire',Properties:{east:'up',north:'none',power:'2',south:'side',west:'up'}}",
			"{Name:'minecraft:redstone_wire',Properties:{east:'up',north:'none',power:'2',south:'up',west:'none'}}",
			"{Name:'minecraft:redstone_wire',Properties:{east:'up',north:'none',power:'2',south:'up',west:'side'}}",
			"{Name:'minecraft:redstone_wire',Properties:{east:'up',north:'none',power:'2',south:'up',west:'up'}}",
			"{Name:'minecraft:redstone_wire',Properties:{east:'up',north:'side',power:'2',south:'none',west:'none'}}",
			"{Name:'minecraft:redstone_wire',Properties:{east:'up',north:'side',power:'2',south:'none',west:'side'}}",
			"{Name:'minecraft:redstone_wire',Properties:{east:'up',north:'side',power:'2',south:'none',west:'up'}}",
			"{Name:'minecraft:redstone_wire',Properties:{east:'up',north:'side',power:'2',south:'side',west:'none'}}",
			"{Name:'minecraft:redstone_wire',Properties:{east:'up',north:'side',power:'2',south:'side',west:'side'}}",
			"{Name:'minecraft:redstone_wire',Properties:{east:'up',north:'side',power:'2',south:'side',west:'up'}}",
			"{Name:'minecraft:redstone_wire',Properties:{east:'up',north:'side',power:'2',south:'up',west:'none'}}",
			"{Name:'minecraft:redstone_wire',Properties:{east:'up',north:'side',power:'2',south:'up',west:'side'}}",
			"{Name:'minecraft:redstone_wire',Properties:{east:'up',north:'side',power:'2',south:'up',west:'up'}}",
			"{Name:'minecraft:redstone_wire',Properties:{east:'up',north:'up',power:'2',south:'none',west:'none'}}",
			"{Name:'minecraft:redstone_wire',Properties:{east:'up',north:'up',power:'2',south:'none',west:'side'}}",
			"{Name:'minecraft:redstone_wire',Properties:{east:'up',north:'up',power:'2',south:'none',west:'up'}}",
			"{Name:'minecraft:redstone_wire',Properties:{east:'up',north:'up',power:'2',south:'side',west:'none'}}",
			"{Name:'minecraft:redstone_wire',Properties:{east:'up',north:'up',power:'2',south:'side',west:'side'}}",
			"{Name:'minecraft:redstone_wire',Properties:{east:'up',north:'up',power:'2',south:'side',west:'up'}}",
			"{Name:'minecraft:redstone_wire',Properties:{east:'up',north:'up',power:'2',south:'up',west:'none'}}",
			"{Name:'minecraft:redstone_wire',Properties:{east:'up',north:'up',power:'2',south:'up',west:'side'}}",
			"{Name:'minecraft:redstone_wire',Properties:{east:'up',north:'up',power:'2',south:'up',west:'up'}}"
		);
		putStates(
			883,
			"{Name:'minecraft:redstone_wire',Properties:{east:'none',north:'none',power:'3',south:'none',west:'none'}}",
			"{Name:'minecraft:redstone_wire',Properties:{east:'none',north:'none',power:'3',south:'none',west:'none'}}",
			"{Name:'minecraft:redstone_wire',Properties:{east:'none',north:'none',power:'3',south:'none',west:'side'}}",
			"{Name:'minecraft:redstone_wire',Properties:{east:'none',north:'none',power:'3',south:'none',west:'up'}}",
			"{Name:'minecraft:redstone_wire',Properties:{east:'none',north:'none',power:'3',south:'side',west:'none'}}",
			"{Name:'minecraft:redstone_wire',Properties:{east:'none',north:'none',power:'3',south:'side',west:'side'}}",
			"{Name:'minecraft:redstone_wire',Properties:{east:'none',north:'none',power:'3',south:'side',west:'up'}}",
			"{Name:'minecraft:redstone_wire',Properties:{east:'none',north:'none',power:'3',south:'up',west:'none'}}",
			"{Name:'minecraft:redstone_wire',Properties:{east:'none',north:'none',power:'3',south:'up',west:'side'}}",
			"{Name:'minecraft:redstone_wire',Properties:{east:'none',north:'none',power:'3',south:'up',west:'up'}}",
			"{Name:'minecraft:redstone_wire',Properties:{east:'none',north:'side',power:'3',south:'none',west:'none'}}",
			"{Name:'minecraft:redstone_wire',Properties:{east:'none',north:'side',power:'3',south:'none',west:'side'}}",
			"{Name:'minecraft:redstone_wire',Properties:{east:'none',north:'side',power:'3',south:'none',west:'up'}}",
			"{Name:'minecraft:redstone_wire',Properties:{east:'none',north:'side',power:'3',south:'side',west:'none'}}",
			"{Name:'minecraft:redstone_wire',Properties:{east:'none',north:'side',power:'3',south:'side',west:'side'}}",
			"{Name:'minecraft:redstone_wire',Properties:{east:'none',north:'side',power:'3',south:'side',west:'up'}}",
			"{Name:'minecraft:redstone_wire',Properties:{east:'none',north:'side',power:'3',south:'up',west:'none'}}",
			"{Name:'minecraft:redstone_wire',Properties:{east:'none',north:'side',power:'3',south:'up',west:'side'}}",
			"{Name:'minecraft:redstone_wire',Properties:{east:'none',north:'side',power:'3',south:'up',west:'up'}}",
			"{Name:'minecraft:redstone_wire',Properties:{east:'none',north:'up',power:'3',south:'none',west:'none'}}",
			"{Name:'minecraft:redstone_wire',Properties:{east:'none',north:'up',power:'3',south:'none',west:'side'}}",
			"{Name:'minecraft:redstone_wire',Properties:{east:'none',north:'up',power:'3',south:'none',west:'up'}}",
			"{Name:'minecraft:redstone_wire',Properties:{east:'none',north:'up',power:'3',south:'side',west:'none'}}",
			"{Name:'minecraft:redstone_wire',Properties:{east:'none',north:'up',power:'3',south:'side',west:'side'}}",
			"{Name:'minecraft:redstone_wire',Properties:{east:'none',north:'up',power:'3',south:'side',west:'up'}}",
			"{Name:'minecraft:redstone_wire',Properties:{east:'none',north:'up',power:'3',south:'up',west:'none'}}",
			"{Name:'minecraft:redstone_wire',Properties:{east:'none',north:'up',power:'3',south:'up',west:'side'}}",
			"{Name:'minecraft:redstone_wire',Properties:{east:'none',north:'up',power:'3',south:'up',west:'up'}}",
			"{Name:'minecraft:redstone_wire',Properties:{east:'side',north:'none',power:'3',south:'none',west:'none'}}",
			"{Name:'minecraft:redstone_wire',Properties:{east:'side',north:'none',power:'3',south:'none',west:'side'}}",
			"{Name:'minecraft:redstone_wire',Properties:{east:'side',north:'none',power:'3',south:'none',west:'up'}}",
			"{Name:'minecraft:redstone_wire',Properties:{east:'side',north:'none',power:'3',south:'side',west:'none'}}",
			"{Name:'minecraft:redstone_wire',Properties:{east:'side',north:'none',power:'3',south:'side',west:'side'}}",
			"{Name:'minecraft:redstone_wire',Properties:{east:'side',north:'none',power:'3',south:'side',west:'up'}}",
			"{Name:'minecraft:redstone_wire',Properties:{east:'side',north:'none',power:'3',south:'up',west:'none'}}",
			"{Name:'minecraft:redstone_wire',Properties:{east:'side',north:'none',power:'3',south:'up',west:'side'}}",
			"{Name:'minecraft:redstone_wire',Properties:{east:'side',north:'none',power:'3',south:'up',west:'up'}}",
			"{Name:'minecraft:redstone_wire',Properties:{east:'side',north:'side',power:'3',south:'none',west:'none'}}",
			"{Name:'minecraft:redstone_wire',Properties:{east:'side',north:'side',power:'3',south:'none',west:'side'}}",
			"{Name:'minecraft:redstone_wire',Properties:{east:'side',north:'side',power:'3',south:'none',west:'up'}}",
			"{Name:'minecraft:redstone_wire',Properties:{east:'side',north:'side',power:'3',south:'side',west:'none'}}",
			"{Name:'minecraft:redstone_wire',Properties:{east:'side',north:'side',power:'3',south:'side',west:'side'}}",
			"{Name:'minecraft:redstone_wire',Properties:{east:'side',north:'side',power:'3',south:'side',west:'up'}}",
			"{Name:'minecraft:redstone_wire',Properties:{east:'side',north:'side',power:'3',south:'up',west:'none'}}",
			"{Name:'minecraft:redstone_wire',Properties:{east:'side',north:'side',power:'3',south:'up',west:'side'}}",
			"{Name:'minecraft:redstone_wire',Properties:{east:'side',north:'side',power:'3',south:'up',west:'up'}}",
			"{Name:'minecraft:redstone_wire',Properties:{east:'side',north:'up',power:'3',south:'none',west:'none'}}",
			"{Name:'minecraft:redstone_wire',Properties:{east:'side',north:'up',power:'3',south:'none',west:'side'}}",
			"{Name:'minecraft:redstone_wire',Properties:{east:'side',north:'up',power:'3',south:'none',west:'up'}}",
			"{Name:'minecraft:redstone_wire',Properties:{east:'side',north:'up',power:'3',south:'side',west:'none'}}",
			"{Name:'minecraft:redstone_wire',Properties:{east:'side',north:'up',power:'3',south:'side',west:'side'}}",
			"{Name:'minecraft:redstone_wire',Properties:{east:'side',north:'up',power:'3',south:'side',west:'up'}}",
			"{Name:'minecraft:redstone_wire',Properties:{east:'side',north:'up',power:'3',south:'up',west:'none'}}",
			"{Name:'minecraft:redstone_wire',Properties:{east:'side',north:'up',power:'3',south:'up',west:'side'}}",
			"{Name:'minecraft:redstone_wire',Properties:{east:'side',north:'up',power:'3',south:'up',west:'up'}}",
			"{Name:'minecraft:redstone_wire',Properties:{east:'up',north:'none',power:'3',south:'none',west:'none'}}",
			"{Name:'minecraft:redstone_wire',Properties:{east:'up',north:'none',power:'3',south:'none',west:'side'}}",
			"{Name:'minecraft:redstone_wire',Properties:{east:'up',north:'none',power:'3',south:'none',west:'up'}}",
			"{Name:'minecraft:redstone_wire',Properties:{east:'up',north:'none',power:'3',south:'side',west:'none'}}",
			"{Name:'minecraft:redstone_wire',Properties:{east:'up',north:'none',power:'3',south:'side',west:'side'}}",
			"{Name:'minecraft:redstone_wire',Properties:{east:'up',north:'none',power:'3',south:'side',west:'up'}}",
			"{Name:'minecraft:redstone_wire',Properties:{east:'up',north:'none',power:'3',south:'up',west:'none'}}",
			"{Name:'minecraft:redstone_wire',Properties:{east:'up',north:'none',power:'3',south:'up',west:'side'}}",
			"{Name:'minecraft:redstone_wire',Properties:{east:'up',north:'none',power:'3',south:'up',west:'up'}}",
			"{Name:'minecraft:redstone_wire',Properties:{east:'up',north:'side',power:'3',south:'none',west:'none'}}",
			"{Name:'minecraft:redstone_wire',Properties:{east:'up',north:'side',power:'3',south:'none',west:'side'}}",
			"{Name:'minecraft:redstone_wire',Properties:{east:'up',north:'side',power:'3',south:'none',west:'up'}}",
			"{Name:'minecraft:redstone_wire',Properties:{east:'up',north:'side',power:'3',south:'side',west:'none'}}",
			"{Name:'minecraft:redstone_wire',Properties:{east:'up',north:'side',power:'3',south:'side',west:'side'}}",
			"{Name:'minecraft:redstone_wire',Properties:{east:'up',north:'side',power:'3',south:'side',west:'up'}}",
			"{Name:'minecraft:redstone_wire',Properties:{east:'up',north:'side',power:'3',south:'up',west:'none'}}",
			"{Name:'minecraft:redstone_wire',Properties:{east:'up',north:'side',power:'3',south:'up',west:'side'}}",
			"{Name:'minecraft:redstone_wire',Properties:{east:'up',north:'side',power:'3',south:'up',west:'up'}}",
			"{Name:'minecraft:redstone_wire',Properties:{east:'up',north:'up',power:'3',south:'none',west:'none'}}",
			"{Name:'minecraft:redstone_wire',Properties:{east:'up',north:'up',power:'3',south:'none',west:'side'}}",
			"{Name:'minecraft:redstone_wire',Properties:{east:'up',north:'up',power:'3',south:'none',west:'up'}}",
			"{Name:'minecraft:redstone_wire',Properties:{east:'up',north:'up',power:'3',south:'side',west:'none'}}",
			"{Name:'minecraft:redstone_wire',Properties:{east:'up',north:'up',power:'3',south:'side',west:'side'}}",
			"{Name:'minecraft:redstone_wire',Properties:{east:'up',north:'up',power:'3',south:'side',west:'up'}}",
			"{Name:'minecraft:redstone_wire',Properties:{east:'up',north:'up',power:'3',south:'up',west:'none'}}",
			"{Name:'minecraft:redstone_wire',Properties:{east:'up',north:'up',power:'3',south:'up',west:'side'}}",
			"{Name:'minecraft:redstone_wire',Properties:{east:'up',north:'up',power:'3',south:'up',west:'up'}}"
		);
		putStates(
			884,
			"{Name:'minecraft:redstone_wire',Properties:{east:'none',north:'none',power:'4',south:'none',west:'none'}}",
			"{Name:'minecraft:redstone_wire',Properties:{east:'none',north:'none',power:'4',south:'none',west:'none'}}",
			"{Name:'minecraft:redstone_wire',Properties:{east:'none',north:'none',power:'4',south:'none',west:'side'}}",
			"{Name:'minecraft:redstone_wire',Properties:{east:'none',north:'none',power:'4',south:'none',west:'up'}}",
			"{Name:'minecraft:redstone_wire',Properties:{east:'none',north:'none',power:'4',south:'side',west:'none'}}",
			"{Name:'minecraft:redstone_wire',Properties:{east:'none',north:'none',power:'4',south:'side',west:'side'}}",
			"{Name:'minecraft:redstone_wire',Properties:{east:'none',north:'none',power:'4',south:'side',west:'up'}}",
			"{Name:'minecraft:redstone_wire',Properties:{east:'none',north:'none',power:'4',south:'up',west:'none'}}",
			"{Name:'minecraft:redstone_wire',Properties:{east:'none',north:'none',power:'4',south:'up',west:'side'}}",
			"{Name:'minecraft:redstone_wire',Properties:{east:'none',north:'none',power:'4',south:'up',west:'up'}}",
			"{Name:'minecraft:redstone_wire',Properties:{east:'none',north:'side',power:'4',south:'none',west:'none'}}",
			"{Name:'minecraft:redstone_wire',Properties:{east:'none',north:'side',power:'4',south:'none',west:'side'}}",
			"{Name:'minecraft:redstone_wire',Properties:{east:'none',north:'side',power:'4',south:'none',west:'up'}}",
			"{Name:'minecraft:redstone_wire',Properties:{east:'none',north:'side',power:'4',south:'side',west:'none'}}",
			"{Name:'minecraft:redstone_wire',Properties:{east:'none',north:'side',power:'4',south:'side',west:'side'}}",
			"{Name:'minecraft:redstone_wire',Properties:{east:'none',north:'side',power:'4',south:'side',west:'up'}}",
			"{Name:'minecraft:redstone_wire',Properties:{east:'none',north:'side',power:'4',south:'up',west:'none'}}",
			"{Name:'minecraft:redstone_wire',Properties:{east:'none',north:'side',power:'4',south:'up',west:'side'}}",
			"{Name:'minecraft:redstone_wire',Properties:{east:'none',north:'side',power:'4',south:'up',west:'up'}}",
			"{Name:'minecraft:redstone_wire',Properties:{east:'none',north:'up',power:'4',south:'none',west:'none'}}",
			"{Name:'minecraft:redstone_wire',Properties:{east:'none',north:'up',power:'4',south:'none',west:'side'}}",
			"{Name:'minecraft:redstone_wire',Properties:{east:'none',north:'up',power:'4',south:'none',west:'up'}}",
			"{Name:'minecraft:redstone_wire',Properties:{east:'none',north:'up',power:'4',south:'side',west:'none'}}",
			"{Name:'minecraft:redstone_wire',Properties:{east:'none',north:'up',power:'4',south:'side',west:'side'}}",
			"{Name:'minecraft:redstone_wire',Properties:{east:'none',north:'up',power:'4',south:'side',west:'up'}}",
			"{Name:'minecraft:redstone_wire',Properties:{east:'none',north:'up',power:'4',south:'up',west:'none'}}",
			"{Name:'minecraft:redstone_wire',Properties:{east:'none',north:'up',power:'4',south:'up',west:'side'}}",
			"{Name:'minecraft:redstone_wire',Properties:{east:'none',north:'up',power:'4',south:'up',west:'up'}}",
			"{Name:'minecraft:redstone_wire',Properties:{east:'side',north:'none',power:'4',south:'none',west:'none'}}",
			"{Name:'minecraft:redstone_wire',Properties:{east:'side',north:'none',power:'4',south:'none',west:'side'}}",
			"{Name:'minecraft:redstone_wire',Properties:{east:'side',north:'none',power:'4',south:'none',west:'up'}}",
			"{Name:'minecraft:redstone_wire',Properties:{east:'side',north:'none',power:'4',south:'side',west:'none'}}",
			"{Name:'minecraft:redstone_wire',Properties:{east:'side',north:'none',power:'4',south:'side',west:'side'}}",
			"{Name:'minecraft:redstone_wire',Properties:{east:'side',north:'none',power:'4',south:'side',west:'up'}}",
			"{Name:'minecraft:redstone_wire',Properties:{east:'side',north:'none',power:'4',south:'up',west:'none'}}",
			"{Name:'minecraft:redstone_wire',Properties:{east:'side',north:'none',power:'4',south:'up',west:'side'}}",
			"{Name:'minecraft:redstone_wire',Properties:{east:'side',north:'none',power:'4',south:'up',west:'up'}}",
			"{Name:'minecraft:redstone_wire',Properties:{east:'side',north:'side',power:'4',south:'none',west:'none'}}",
			"{Name:'minecraft:redstone_wire',Properties:{east:'side',north:'side',power:'4',south:'none',west:'side'}}",
			"{Name:'minecraft:redstone_wire',Properties:{east:'side',north:'side',power:'4',south:'none',west:'up'}}",
			"{Name:'minecraft:redstone_wire',Properties:{east:'side',north:'side',power:'4',south:'side',west:'none'}}",
			"{Name:'minecraft:redstone_wire',Properties:{east:'side',north:'side',power:'4',south:'side',west:'side'}}",
			"{Name:'minecraft:redstone_wire',Properties:{east:'side',north:'side',power:'4',south:'side',west:'up'}}",
			"{Name:'minecraft:redstone_wire',Properties:{east:'side',north:'side',power:'4',south:'up',west:'none'}}",
			"{Name:'minecraft:redstone_wire',Properties:{east:'side',north:'side',power:'4',south:'up',west:'side'}}",
			"{Name:'minecraft:redstone_wire',Properties:{east:'side',north:'side',power:'4',south:'up',west:'up'}}",
			"{Name:'minecraft:redstone_wire',Properties:{east:'side',north:'up',power:'4',south:'none',west:'none'}}",
			"{Name:'minecraft:redstone_wire',Properties:{east:'side',north:'up',power:'4',south:'none',west:'side'}}",
			"{Name:'minecraft:redstone_wire',Properties:{east:'side',north:'up',power:'4',south:'none',west:'up'}}",
			"{Name:'minecraft:redstone_wire',Properties:{east:'side',north:'up',power:'4',south:'side',west:'none'}}",
			"{Name:'minecraft:redstone_wire',Properties:{east:'side',north:'up',power:'4',south:'side',west:'side'}}",
			"{Name:'minecraft:redstone_wire',Properties:{east:'side',north:'up',power:'4',south:'side',west:'up'}}",
			"{Name:'minecraft:redstone_wire',Properties:{east:'side',north:'up',power:'4',south:'up',west:'none'}}",
			"{Name:'minecraft:redstone_wire',Properties:{east:'side',north:'up',power:'4',south:'up',west:'side'}}",
			"{Name:'minecraft:redstone_wire',Properties:{east:'side',north:'up',power:'4',south:'up',west:'up'}}",
			"{Name:'minecraft:redstone_wire',Properties:{east:'up',north:'none',power:'4',south:'none',west:'none'}}",
			"{Name:'minecraft:redstone_wire',Properties:{east:'up',north:'none',power:'4',south:'none',west:'side'}}",
			"{Name:'minecraft:redstone_wire',Properties:{east:'up',north:'none',power:'4',south:'none',west:'up'}}",
			"{Name:'minecraft:redstone_wire',Properties:{east:'up',north:'none',power:'4',south:'side',west:'none'}}",
			"{Name:'minecraft:redstone_wire',Properties:{east:'up',north:'none',power:'4',south:'side',west:'side'}}",
			"{Name:'minecraft:redstone_wire',Properties:{east:'up',north:'none',power:'4',south:'side',west:'up'}}",
			"{Name:'minecraft:redstone_wire',Properties:{east:'up',north:'none',power:'4',south:'up',west:'none'}}",
			"{Name:'minecraft:redstone_wire',Properties:{east:'up',north:'none',power:'4',south:'up',west:'side'}}",
			"{Name:'minecraft:redstone_wire',Properties:{east:'up',north:'none',power:'4',south:'up',west:'up'}}",
			"{Name:'minecraft:redstone_wire',Properties:{east:'up',north:'side',power:'4',south:'none',west:'none'}}",
			"{Name:'minecraft:redstone_wire',Properties:{east:'up',north:'side',power:'4',south:'none',west:'side'}}",
			"{Name:'minecraft:redstone_wire',Properties:{east:'up',north:'side',power:'4',south:'none',west:'up'}}",
			"{Name:'minecraft:redstone_wire',Properties:{east:'up',north:'side',power:'4',south:'side',west:'none'}}",
			"{Name:'minecraft:redstone_wire',Properties:{east:'up',north:'side',power:'4',south:'side',west:'side'}}",
			"{Name:'minecraft:redstone_wire',Properties:{east:'up',north:'side',power:'4',south:'side',west:'up'}}",
			"{Name:'minecraft:redstone_wire',Properties:{east:'up',north:'side',power:'4',south:'up',west:'none'}}",
			"{Name:'minecraft:redstone_wire',Properties:{east:'up',north:'side',power:'4',south:'up',west:'side'}}",
			"{Name:'minecraft:redstone_wire',Properties:{east:'up',north:'side',power:'4',south:'up',west:'up'}}",
			"{Name:'minecraft:redstone_wire',Properties:{east:'up',north:'up',power:'4',south:'none',west:'none'}}",
			"{Name:'minecraft:redstone_wire',Properties:{east:'up',north:'up',power:'4',south:'none',west:'side'}}",
			"{Name:'minecraft:redstone_wire',Properties:{east:'up',north:'up',power:'4',south:'none',west:'up'}}",
			"{Name:'minecraft:redstone_wire',Properties:{east:'up',north:'up',power:'4',south:'side',west:'none'}}",
			"{Name:'minecraft:redstone_wire',Properties:{east:'up',north:'up',power:'4',south:'side',west:'side'}}",
			"{Name:'minecraft:redstone_wire',Properties:{east:'up',north:'up',power:'4',south:'side',west:'up'}}",
			"{Name:'minecraft:redstone_wire',Properties:{east:'up',north:'up',power:'4',south:'up',west:'none'}}",
			"{Name:'minecraft:redstone_wire',Properties:{east:'up',north:'up',power:'4',south:'up',west:'side'}}",
			"{Name:'minecraft:redstone_wire',Properties:{east:'up',north:'up',power:'4',south:'up',west:'up'}}"
		);
		putStates(
			885,
			"{Name:'minecraft:redstone_wire',Properties:{east:'none',north:'none',power:'5',south:'none',west:'none'}}",
			"{Name:'minecraft:redstone_wire',Properties:{east:'none',north:'none',power:'5',south:'none',west:'none'}}",
			"{Name:'minecraft:redstone_wire',Properties:{east:'none',north:'none',power:'5',south:'none',west:'side'}}",
			"{Name:'minecraft:redstone_wire',Properties:{east:'none',north:'none',power:'5',south:'none',west:'up'}}",
			"{Name:'minecraft:redstone_wire',Properties:{east:'none',north:'none',power:'5',south:'side',west:'none'}}",
			"{Name:'minecraft:redstone_wire',Properties:{east:'none',north:'none',power:'5',south:'side',west:'side'}}",
			"{Name:'minecraft:redstone_wire',Properties:{east:'none',north:'none',power:'5',south:'side',west:'up'}}",
			"{Name:'minecraft:redstone_wire',Properties:{east:'none',north:'none',power:'5',south:'up',west:'none'}}",
			"{Name:'minecraft:redstone_wire',Properties:{east:'none',north:'none',power:'5',south:'up',west:'side'}}",
			"{Name:'minecraft:redstone_wire',Properties:{east:'none',north:'none',power:'5',south:'up',west:'up'}}",
			"{Name:'minecraft:redstone_wire',Properties:{east:'none',north:'side',power:'5',south:'none',west:'none'}}",
			"{Name:'minecraft:redstone_wire',Properties:{east:'none',north:'side',power:'5',south:'none',west:'side'}}",
			"{Name:'minecraft:redstone_wire',Properties:{east:'none',north:'side',power:'5',south:'none',west:'up'}}",
			"{Name:'minecraft:redstone_wire',Properties:{east:'none',north:'side',power:'5',south:'side',west:'none'}}",
			"{Name:'minecraft:redstone_wire',Properties:{east:'none',north:'side',power:'5',south:'side',west:'side'}}",
			"{Name:'minecraft:redstone_wire',Properties:{east:'none',north:'side',power:'5',south:'side',west:'up'}}",
			"{Name:'minecraft:redstone_wire',Properties:{east:'none',north:'side',power:'5',south:'up',west:'none'}}",
			"{Name:'minecraft:redstone_wire',Properties:{east:'none',north:'side',power:'5',south:'up',west:'side'}}",
			"{Name:'minecraft:redstone_wire',Properties:{east:'none',north:'side',power:'5',south:'up',west:'up'}}",
			"{Name:'minecraft:redstone_wire',Properties:{east:'none',north:'up',power:'5',south:'none',west:'none'}}",
			"{Name:'minecraft:redstone_wire',Properties:{east:'none',north:'up',power:'5',south:'none',west:'side'}}",
			"{Name:'minecraft:redstone_wire',Properties:{east:'none',north:'up',power:'5',south:'none',west:'up'}}",
			"{Name:'minecraft:redstone_wire',Properties:{east:'none',north:'up',power:'5',south:'side',west:'none'}}",
			"{Name:'minecraft:redstone_wire',Properties:{east:'none',north:'up',power:'5',south:'side',west:'side'}}",
			"{Name:'minecraft:redstone_wire',Properties:{east:'none',north:'up',power:'5',south:'side',west:'up'}}",
			"{Name:'minecraft:redstone_wire',Properties:{east:'none',north:'up',power:'5',south:'up',west:'none'}}",
			"{Name:'minecraft:redstone_wire',Properties:{east:'none',north:'up',power:'5',south:'up',west:'side'}}",
			"{Name:'minecraft:redstone_wire',Properties:{east:'none',north:'up',power:'5',south:'up',west:'up'}}",
			"{Name:'minecraft:redstone_wire',Properties:{east:'side',north:'none',power:'5',south:'none',west:'none'}}",
			"{Name:'minecraft:redstone_wire',Properties:{east:'side',north:'none',power:'5',south:'none',west:'side'}}",
			"{Name:'minecraft:redstone_wire',Properties:{east:'side',north:'none',power:'5',south:'none',west:'up'}}",
			"{Name:'minecraft:redstone_wire',Properties:{east:'side',north:'none',power:'5',south:'side',west:'none'}}",
			"{Name:'minecraft:redstone_wire',Properties:{east:'side',north:'none',power:'5',south:'side',west:'side'}}",
			"{Name:'minecraft:redstone_wire',Properties:{east:'side',north:'none',power:'5',south:'side',west:'up'}}",
			"{Name:'minecraft:redstone_wire',Properties:{east:'side',north:'none',power:'5',south:'up',west:'none'}}",
			"{Name:'minecraft:redstone_wire',Properties:{east:'side',north:'none',power:'5',south:'up',west:'side'}}",
			"{Name:'minecraft:redstone_wire',Properties:{east:'side',north:'none',power:'5',south:'up',west:'up'}}",
			"{Name:'minecraft:redstone_wire',Properties:{east:'side',north:'side',power:'5',south:'none',west:'none'}}",
			"{Name:'minecraft:redstone_wire',Properties:{east:'side',north:'side',power:'5',south:'none',west:'side'}}",
			"{Name:'minecraft:redstone_wire',Properties:{east:'side',north:'side',power:'5',south:'none',west:'up'}}",
			"{Name:'minecraft:redstone_wire',Properties:{east:'side',north:'side',power:'5',south:'side',west:'none'}}",
			"{Name:'minecraft:redstone_wire',Properties:{east:'side',north:'side',power:'5',south:'side',west:'side'}}",
			"{Name:'minecraft:redstone_wire',Properties:{east:'side',north:'side',power:'5',south:'side',west:'up'}}",
			"{Name:'minecraft:redstone_wire',Properties:{east:'side',north:'side',power:'5',south:'up',west:'none'}}",
			"{Name:'minecraft:redstone_wire',Properties:{east:'side',north:'side',power:'5',south:'up',west:'side'}}",
			"{Name:'minecraft:redstone_wire',Properties:{east:'side',north:'side',power:'5',south:'up',west:'up'}}",
			"{Name:'minecraft:redstone_wire',Properties:{east:'side',north:'up',power:'5',south:'none',west:'none'}}",
			"{Name:'minecraft:redstone_wire',Properties:{east:'side',north:'up',power:'5',south:'none',west:'side'}}",
			"{Name:'minecraft:redstone_wire',Properties:{east:'side',north:'up',power:'5',south:'none',west:'up'}}",
			"{Name:'minecraft:redstone_wire',Properties:{east:'side',north:'up',power:'5',south:'side',west:'none'}}",
			"{Name:'minecraft:redstone_wire',Properties:{east:'side',north:'up',power:'5',south:'side',west:'side'}}",
			"{Name:'minecraft:redstone_wire',Properties:{east:'side',north:'up',power:'5',south:'side',west:'up'}}",
			"{Name:'minecraft:redstone_wire',Properties:{east:'side',north:'up',power:'5',south:'up',west:'none'}}",
			"{Name:'minecraft:redstone_wire',Properties:{east:'side',north:'up',power:'5',south:'up',west:'side'}}",
			"{Name:'minecraft:redstone_wire',Properties:{east:'side',north:'up',power:'5',south:'up',west:'up'}}",
			"{Name:'minecraft:redstone_wire',Properties:{east:'up',north:'none',power:'5',south:'none',west:'none'}}",
			"{Name:'minecraft:redstone_wire',Properties:{east:'up',north:'none',power:'5',south:'none',west:'side'}}",
			"{Name:'minecraft:redstone_wire',Properties:{east:'up',north:'none',power:'5',south:'none',west:'up'}}",
			"{Name:'minecraft:redstone_wire',Properties:{east:'up',north:'none',power:'5',south:'side',west:'none'}}",
			"{Name:'minecraft:redstone_wire',Properties:{east:'up',north:'none',power:'5',south:'side',west:'side'}}",
			"{Name:'minecraft:redstone_wire',Properties:{east:'up',north:'none',power:'5',south:'side',west:'up'}}",
			"{Name:'minecraft:redstone_wire',Properties:{east:'up',north:'none',power:'5',south:'up',west:'none'}}",
			"{Name:'minecraft:redstone_wire',Properties:{east:'up',north:'none',power:'5',south:'up',west:'side'}}",
			"{Name:'minecraft:redstone_wire',Properties:{east:'up',north:'none',power:'5',south:'up',west:'up'}}",
			"{Name:'minecraft:redstone_wire',Properties:{east:'up',north:'side',power:'5',south:'none',west:'none'}}",
			"{Name:'minecraft:redstone_wire',Properties:{east:'up',north:'side',power:'5',south:'none',west:'side'}}",
			"{Name:'minecraft:redstone_wire',Properties:{east:'up',north:'side',power:'5',south:'none',west:'up'}}",
			"{Name:'minecraft:redstone_wire',Properties:{east:'up',north:'side',power:'5',south:'side',west:'none'}}",
			"{Name:'minecraft:redstone_wire',Properties:{east:'up',north:'side',power:'5',south:'side',west:'side'}}",
			"{Name:'minecraft:redstone_wire',Properties:{east:'up',north:'side',power:'5',south:'side',west:'up'}}",
			"{Name:'minecraft:redstone_wire',Properties:{east:'up',north:'side',power:'5',south:'up',west:'none'}}",
			"{Name:'minecraft:redstone_wire',Properties:{east:'up',north:'side',power:'5',south:'up',west:'side'}}",
			"{Name:'minecraft:redstone_wire',Properties:{east:'up',north:'side',power:'5',south:'up',west:'up'}}",
			"{Name:'minecraft:redstone_wire',Properties:{east:'up',north:'up',power:'5',south:'none',west:'none'}}",
			"{Name:'minecraft:redstone_wire',Properties:{east:'up',north:'up',power:'5',south:'none',west:'side'}}",
			"{Name:'minecraft:redstone_wire',Properties:{east:'up',north:'up',power:'5',south:'none',west:'up'}}",
			"{Name:'minecraft:redstone_wire',Properties:{east:'up',north:'up',power:'5',south:'side',west:'none'}}",
			"{Name:'minecraft:redstone_wire',Properties:{east:'up',north:'up',power:'5',south:'side',west:'side'}}",
			"{Name:'minecraft:redstone_wire',Properties:{east:'up',north:'up',power:'5',south:'side',west:'up'}}",
			"{Name:'minecraft:redstone_wire',Properties:{east:'up',north:'up',power:'5',south:'up',west:'none'}}",
			"{Name:'minecraft:redstone_wire',Properties:{east:'up',north:'up',power:'5',south:'up',west:'side'}}",
			"{Name:'minecraft:redstone_wire',Properties:{east:'up',north:'up',power:'5',south:'up',west:'up'}}"
		);
		putStates(
			886,
			"{Name:'minecraft:redstone_wire',Properties:{east:'none',north:'none',power:'6',south:'none',west:'none'}}",
			"{Name:'minecraft:redstone_wire',Properties:{east:'none',north:'none',power:'6',south:'none',west:'none'}}",
			"{Name:'minecraft:redstone_wire',Properties:{east:'none',north:'none',power:'6',south:'none',west:'side'}}",
			"{Name:'minecraft:redstone_wire',Properties:{east:'none',north:'none',power:'6',south:'none',west:'up'}}",
			"{Name:'minecraft:redstone_wire',Properties:{east:'none',north:'none',power:'6',south:'side',west:'none'}}",
			"{Name:'minecraft:redstone_wire',Properties:{east:'none',north:'none',power:'6',south:'side',west:'side'}}",
			"{Name:'minecraft:redstone_wire',Properties:{east:'none',north:'none',power:'6',south:'side',west:'up'}}",
			"{Name:'minecraft:redstone_wire',Properties:{east:'none',north:'none',power:'6',south:'up',west:'none'}}",
			"{Name:'minecraft:redstone_wire',Properties:{east:'none',north:'none',power:'6',south:'up',west:'side'}}",
			"{Name:'minecraft:redstone_wire',Properties:{east:'none',north:'none',power:'6',south:'up',west:'up'}}",
			"{Name:'minecraft:redstone_wire',Properties:{east:'none',north:'side',power:'6',south:'none',west:'none'}}",
			"{Name:'minecraft:redstone_wire',Properties:{east:'none',north:'side',power:'6',south:'none',west:'side'}}",
			"{Name:'minecraft:redstone_wire',Properties:{east:'none',north:'side',power:'6',south:'none',west:'up'}}",
			"{Name:'minecraft:redstone_wire',Properties:{east:'none',north:'side',power:'6',south:'side',west:'none'}}",
			"{Name:'minecraft:redstone_wire',Properties:{east:'none',north:'side',power:'6',south:'side',west:'side'}}",
			"{Name:'minecraft:redstone_wire',Properties:{east:'none',north:'side',power:'6',south:'side',west:'up'}}",
			"{Name:'minecraft:redstone_wire',Properties:{east:'none',north:'side',power:'6',south:'up',west:'none'}}",
			"{Name:'minecraft:redstone_wire',Properties:{east:'none',north:'side',power:'6',south:'up',west:'side'}}",
			"{Name:'minecraft:redstone_wire',Properties:{east:'none',north:'side',power:'6',south:'up',west:'up'}}",
			"{Name:'minecraft:redstone_wire',Properties:{east:'none',north:'up',power:'6',south:'none',west:'none'}}",
			"{Name:'minecraft:redstone_wire',Properties:{east:'none',north:'up',power:'6',south:'none',west:'side'}}",
			"{Name:'minecraft:redstone_wire',Properties:{east:'none',north:'up',power:'6',south:'none',west:'up'}}",
			"{Name:'minecraft:redstone_wire',Properties:{east:'none',north:'up',power:'6',south:'side',west:'none'}}",
			"{Name:'minecraft:redstone_wire',Properties:{east:'none',north:'up',power:'6',south:'side',west:'side'}}",
			"{Name:'minecraft:redstone_wire',Properties:{east:'none',north:'up',power:'6',south:'side',west:'up'}}",
			"{Name:'minecraft:redstone_wire',Properties:{east:'none',north:'up',power:'6',south:'up',west:'none'}}",
			"{Name:'minecraft:redstone_wire',Properties:{east:'none',north:'up',power:'6',south:'up',west:'side'}}",
			"{Name:'minecraft:redstone_wire',Properties:{east:'none',north:'up',power:'6',south:'up',west:'up'}}",
			"{Name:'minecraft:redstone_wire',Properties:{east:'side',north:'none',power:'6',south:'none',west:'none'}}",
			"{Name:'minecraft:redstone_wire',Properties:{east:'side',north:'none',power:'6',south:'none',west:'side'}}",
			"{Name:'minecraft:redstone_wire',Properties:{east:'side',north:'none',power:'6',south:'none',west:'up'}}",
			"{Name:'minecraft:redstone_wire',Properties:{east:'side',north:'none',power:'6',south:'side',west:'none'}}",
			"{Name:'minecraft:redstone_wire',Properties:{east:'side',north:'none',power:'6',south:'side',west:'side'}}",
			"{Name:'minecraft:redstone_wire',Properties:{east:'side',north:'none',power:'6',south:'side',west:'up'}}",
			"{Name:'minecraft:redstone_wire',Properties:{east:'side',north:'none',power:'6',south:'up',west:'none'}}",
			"{Name:'minecraft:redstone_wire',Properties:{east:'side',north:'none',power:'6',south:'up',west:'side'}}",
			"{Name:'minecraft:redstone_wire',Properties:{east:'side',north:'none',power:'6',south:'up',west:'up'}}",
			"{Name:'minecraft:redstone_wire',Properties:{east:'side',north:'side',power:'6',south:'none',west:'none'}}",
			"{Name:'minecraft:redstone_wire',Properties:{east:'side',north:'side',power:'6',south:'none',west:'side'}}",
			"{Name:'minecraft:redstone_wire',Properties:{east:'side',north:'side',power:'6',south:'none',west:'up'}}",
			"{Name:'minecraft:redstone_wire',Properties:{east:'side',north:'side',power:'6',south:'side',west:'none'}}",
			"{Name:'minecraft:redstone_wire',Properties:{east:'side',north:'side',power:'6',south:'side',west:'side'}}",
			"{Name:'minecraft:redstone_wire',Properties:{east:'side',north:'side',power:'6',south:'side',west:'up'}}",
			"{Name:'minecraft:redstone_wire',Properties:{east:'side',north:'side',power:'6',south:'up',west:'none'}}",
			"{Name:'minecraft:redstone_wire',Properties:{east:'side',north:'side',power:'6',south:'up',west:'side'}}",
			"{Name:'minecraft:redstone_wire',Properties:{east:'side',north:'side',power:'6',south:'up',west:'up'}}",
			"{Name:'minecraft:redstone_wire',Properties:{east:'side',north:'up',power:'6',south:'none',west:'none'}}",
			"{Name:'minecraft:redstone_wire',Properties:{east:'side',north:'up',power:'6',south:'none',west:'side'}}",
			"{Name:'minecraft:redstone_wire',Properties:{east:'side',north:'up',power:'6',south:'none',west:'up'}}",
			"{Name:'minecraft:redstone_wire',Properties:{east:'side',north:'up',power:'6',south:'side',west:'none'}}",
			"{Name:'minecraft:redstone_wire',Properties:{east:'side',north:'up',power:'6',south:'side',west:'side'}}",
			"{Name:'minecraft:redstone_wire',Properties:{east:'side',north:'up',power:'6',south:'side',west:'up'}}",
			"{Name:'minecraft:redstone_wire',Properties:{east:'side',north:'up',power:'6',south:'up',west:'none'}}",
			"{Name:'minecraft:redstone_wire',Properties:{east:'side',north:'up',power:'6',south:'up',west:'side'}}",
			"{Name:'minecraft:redstone_wire',Properties:{east:'side',north:'up',power:'6',south:'up',west:'up'}}",
			"{Name:'minecraft:redstone_wire',Properties:{east:'up',north:'none',power:'6',south:'none',west:'none'}}",
			"{Name:'minecraft:redstone_wire',Properties:{east:'up',north:'none',power:'6',south:'none',west:'side'}}",
			"{Name:'minecraft:redstone_wire',Properties:{east:'up',north:'none',power:'6',south:'none',west:'up'}}",
			"{Name:'minecraft:redstone_wire',Properties:{east:'up',north:'none',power:'6',south:'side',west:'none'}}",
			"{Name:'minecraft:redstone_wire',Properties:{east:'up',north:'none',power:'6',south:'side',west:'side'}}",
			"{Name:'minecraft:redstone_wire',Properties:{east:'up',north:'none',power:'6',south:'side',west:'up'}}",
			"{Name:'minecraft:redstone_wire',Properties:{east:'up',north:'none',power:'6',south:'up',west:'none'}}",
			"{Name:'minecraft:redstone_wire',Properties:{east:'up',north:'none',power:'6',south:'up',west:'side'}}",
			"{Name:'minecraft:redstone_wire',Properties:{east:'up',north:'none',power:'6',south:'up',west:'up'}}",
			"{Name:'minecraft:redstone_wire',Properties:{east:'up',north:'side',power:'6',south:'none',west:'none'}}",
			"{Name:'minecraft:redstone_wire',Properties:{east:'up',north:'side',power:'6',south:'none',west:'side'}}",
			"{Name:'minecraft:redstone_wire',Properties:{east:'up',north:'side',power:'6',south:'none',west:'up'}}",
			"{Name:'minecraft:redstone_wire',Properties:{east:'up',north:'side',power:'6',south:'side',west:'none'}}",
			"{Name:'minecraft:redstone_wire',Properties:{east:'up',north:'side',power:'6',south:'side',west:'side'}}",
			"{Name:'minecraft:redstone_wire',Properties:{east:'up',north:'side',power:'6',south:'side',west:'up'}}",
			"{Name:'minecraft:redstone_wire',Properties:{east:'up',north:'side',power:'6',south:'up',west:'none'}}",
			"{Name:'minecraft:redstone_wire',Properties:{east:'up',north:'side',power:'6',south:'up',west:'side'}}",
			"{Name:'minecraft:redstone_wire',Properties:{east:'up',north:'side',power:'6',south:'up',west:'up'}}",
			"{Name:'minecraft:redstone_wire',Properties:{east:'up',north:'up',power:'6',south:'none',west:'none'}}",
			"{Name:'minecraft:redstone_wire',Properties:{east:'up',north:'up',power:'6',south:'none',west:'side'}}",
			"{Name:'minecraft:redstone_wire',Properties:{east:'up',north:'up',power:'6',south:'none',west:'up'}}",
			"{Name:'minecraft:redstone_wire',Properties:{east:'up',north:'up',power:'6',south:'side',west:'none'}}",
			"{Name:'minecraft:redstone_wire',Properties:{east:'up',north:'up',power:'6',south:'side',west:'side'}}",
			"{Name:'minecraft:redstone_wire',Properties:{east:'up',north:'up',power:'6',south:'side',west:'up'}}",
			"{Name:'minecraft:redstone_wire',Properties:{east:'up',north:'up',power:'6',south:'up',west:'none'}}",
			"{Name:'minecraft:redstone_wire',Properties:{east:'up',north:'up',power:'6',south:'up',west:'side'}}",
			"{Name:'minecraft:redstone_wire',Properties:{east:'up',north:'up',power:'6',south:'up',west:'up'}}"
		);
		putStates(
			887,
			"{Name:'minecraft:redstone_wire',Properties:{east:'none',north:'none',power:'7',south:'none',west:'none'}}",
			"{Name:'minecraft:redstone_wire',Properties:{east:'none',north:'none',power:'7',south:'none',west:'none'}}",
			"{Name:'minecraft:redstone_wire',Properties:{east:'none',north:'none',power:'7',south:'none',west:'side'}}",
			"{Name:'minecraft:redstone_wire',Properties:{east:'none',north:'none',power:'7',south:'none',west:'up'}}",
			"{Name:'minecraft:redstone_wire',Properties:{east:'none',north:'none',power:'7',south:'side',west:'none'}}",
			"{Name:'minecraft:redstone_wire',Properties:{east:'none',north:'none',power:'7',south:'side',west:'side'}}",
			"{Name:'minecraft:redstone_wire',Properties:{east:'none',north:'none',power:'7',south:'side',west:'up'}}",
			"{Name:'minecraft:redstone_wire',Properties:{east:'none',north:'none',power:'7',south:'up',west:'none'}}",
			"{Name:'minecraft:redstone_wire',Properties:{east:'none',north:'none',power:'7',south:'up',west:'side'}}",
			"{Name:'minecraft:redstone_wire',Properties:{east:'none',north:'none',power:'7',south:'up',west:'up'}}",
			"{Name:'minecraft:redstone_wire',Properties:{east:'none',north:'side',power:'7',south:'none',west:'none'}}",
			"{Name:'minecraft:redstone_wire',Properties:{east:'none',north:'side',power:'7',south:'none',west:'side'}}",
			"{Name:'minecraft:redstone_wire',Properties:{east:'none',north:'side',power:'7',south:'none',west:'up'}}",
			"{Name:'minecraft:redstone_wire',Properties:{east:'none',north:'side',power:'7',south:'side',west:'none'}}",
			"{Name:'minecraft:redstone_wire',Properties:{east:'none',north:'side',power:'7',south:'side',west:'side'}}",
			"{Name:'minecraft:redstone_wire',Properties:{east:'none',north:'side',power:'7',south:'side',west:'up'}}",
			"{Name:'minecraft:redstone_wire',Properties:{east:'none',north:'side',power:'7',south:'up',west:'none'}}",
			"{Name:'minecraft:redstone_wire',Properties:{east:'none',north:'side',power:'7',south:'up',west:'side'}}",
			"{Name:'minecraft:redstone_wire',Properties:{east:'none',north:'side',power:'7',south:'up',west:'up'}}",
			"{Name:'minecraft:redstone_wire',Properties:{east:'none',north:'up',power:'7',south:'none',west:'none'}}",
			"{Name:'minecraft:redstone_wire',Properties:{east:'none',north:'up',power:'7',south:'none',west:'side'}}",
			"{Name:'minecraft:redstone_wire',Properties:{east:'none',north:'up',power:'7',south:'none',west:'up'}}",
			"{Name:'minecraft:redstone_wire',Properties:{east:'none',north:'up',power:'7',south:'side',west:'none'}}",
			"{Name:'minecraft:redstone_wire',Properties:{east:'none',north:'up',power:'7',south:'side',west:'side'}}",
			"{Name:'minecraft:redstone_wire',Properties:{east:'none',north:'up',power:'7',south:'side',west:'up'}}",
			"{Name:'minecraft:redstone_wire',Properties:{east:'none',north:'up',power:'7',south:'up',west:'none'}}",
			"{Name:'minecraft:redstone_wire',Properties:{east:'none',north:'up',power:'7',south:'up',west:'side'}}",
			"{Name:'minecraft:redstone_wire',Properties:{east:'none',north:'up',power:'7',south:'up',west:'up'}}",
			"{Name:'minecraft:redstone_wire',Properties:{east:'side',north:'none',power:'7',south:'none',west:'none'}}",
			"{Name:'minecraft:redstone_wire',Properties:{east:'side',north:'none',power:'7',south:'none',west:'side'}}",
			"{Name:'minecraft:redstone_wire',Properties:{east:'side',north:'none',power:'7',south:'none',west:'up'}}",
			"{Name:'minecraft:redstone_wire',Properties:{east:'side',north:'none',power:'7',south:'side',west:'none'}}",
			"{Name:'minecraft:redstone_wire',Properties:{east:'side',north:'none',power:'7',south:'side',west:'side'}}",
			"{Name:'minecraft:redstone_wire',Properties:{east:'side',north:'none',power:'7',south:'side',west:'up'}}",
			"{Name:'minecraft:redstone_wire',Properties:{east:'side',north:'none',power:'7',south:'up',west:'none'}}",
			"{Name:'minecraft:redstone_wire',Properties:{east:'side',north:'none',power:'7',south:'up',west:'side'}}",
			"{Name:'minecraft:redstone_wire',Properties:{east:'side',north:'none',power:'7',south:'up',west:'up'}}",
			"{Name:'minecraft:redstone_wire',Properties:{east:'side',north:'side',power:'7',south:'none',west:'none'}}",
			"{Name:'minecraft:redstone_wire',Properties:{east:'side',north:'side',power:'7',south:'none',west:'side'}}",
			"{Name:'minecraft:redstone_wire',Properties:{east:'side',north:'side',power:'7',south:'none',west:'up'}}",
			"{Name:'minecraft:redstone_wire',Properties:{east:'side',north:'side',power:'7',south:'side',west:'none'}}",
			"{Name:'minecraft:redstone_wire',Properties:{east:'side',north:'side',power:'7',south:'side',west:'side'}}",
			"{Name:'minecraft:redstone_wire',Properties:{east:'side',north:'side',power:'7',south:'side',west:'up'}}",
			"{Name:'minecraft:redstone_wire',Properties:{east:'side',north:'side',power:'7',south:'up',west:'none'}}",
			"{Name:'minecraft:redstone_wire',Properties:{east:'side',north:'side',power:'7',south:'up',west:'side'}}",
			"{Name:'minecraft:redstone_wire',Properties:{east:'side',north:'side',power:'7',south:'up',west:'up'}}",
			"{Name:'minecraft:redstone_wire',Properties:{east:'side',north:'up',power:'7',south:'none',west:'none'}}",
			"{Name:'minecraft:redstone_wire',Properties:{east:'side',north:'up',power:'7',south:'none',west:'side'}}",
			"{Name:'minecraft:redstone_wire',Properties:{east:'side',north:'up',power:'7',south:'none',west:'up'}}",
			"{Name:'minecraft:redstone_wire',Properties:{east:'side',north:'up',power:'7',south:'side',west:'none'}}",
			"{Name:'minecraft:redstone_wire',Properties:{east:'side',north:'up',power:'7',south:'side',west:'side'}}",
			"{Name:'minecraft:redstone_wire',Properties:{east:'side',north:'up',power:'7',south:'side',west:'up'}}",
			"{Name:'minecraft:redstone_wire',Properties:{east:'side',north:'up',power:'7',south:'up',west:'none'}}",
			"{Name:'minecraft:redstone_wire',Properties:{east:'side',north:'up',power:'7',south:'up',west:'side'}}",
			"{Name:'minecraft:redstone_wire',Properties:{east:'side',north:'up',power:'7',south:'up',west:'up'}}",
			"{Name:'minecraft:redstone_wire',Properties:{east:'up',north:'none',power:'7',south:'none',west:'none'}}",
			"{Name:'minecraft:redstone_wire',Properties:{east:'up',north:'none',power:'7',south:'none',west:'side'}}",
			"{Name:'minecraft:redstone_wire',Properties:{east:'up',north:'none',power:'7',south:'none',west:'up'}}",
			"{Name:'minecraft:redstone_wire',Properties:{east:'up',north:'none',power:'7',south:'side',west:'none'}}",
			"{Name:'minecraft:redstone_wire',Properties:{east:'up',north:'none',power:'7',south:'side',west:'side'}}",
			"{Name:'minecraft:redstone_wire',Properties:{east:'up',north:'none',power:'7',south:'side',west:'up'}}",
			"{Name:'minecraft:redstone_wire',Properties:{east:'up',north:'none',power:'7',south:'up',west:'none'}}",
			"{Name:'minecraft:redstone_wire',Properties:{east:'up',north:'none',power:'7',south:'up',west:'side'}}",
			"{Name:'minecraft:redstone_wire',Properties:{east:'up',north:'none',power:'7',south:'up',west:'up'}}",
			"{Name:'minecraft:redstone_wire',Properties:{east:'up',north:'side',power:'7',south:'none',west:'none'}}",
			"{Name:'minecraft:redstone_wire',Properties:{east:'up',north:'side',power:'7',south:'none',west:'side'}}",
			"{Name:'minecraft:redstone_wire',Properties:{east:'up',north:'side',power:'7',south:'none',west:'up'}}",
			"{Name:'minecraft:redstone_wire',Properties:{east:'up',north:'side',power:'7',south:'side',west:'none'}}",
			"{Name:'minecraft:redstone_wire',Properties:{east:'up',north:'side',power:'7',south:'side',west:'side'}}",
			"{Name:'minecraft:redstone_wire',Properties:{east:'up',north:'side',power:'7',south:'side',west:'up'}}",
			"{Name:'minecraft:redstone_wire',Properties:{east:'up',north:'side',power:'7',south:'up',west:'none'}}",
			"{Name:'minecraft:redstone_wire',Properties:{east:'up',north:'side',power:'7',south:'up',west:'side'}}",
			"{Name:'minecraft:redstone_wire',Properties:{east:'up',north:'side',power:'7',south:'up',west:'up'}}",
			"{Name:'minecraft:redstone_wire',Properties:{east:'up',north:'up',power:'7',south:'none',west:'none'}}",
			"{Name:'minecraft:redstone_wire',Properties:{east:'up',north:'up',power:'7',south:'none',west:'side'}}",
			"{Name:'minecraft:redstone_wire',Properties:{east:'up',north:'up',power:'7',south:'none',west:'up'}}",
			"{Name:'minecraft:redstone_wire',Properties:{east:'up',north:'up',power:'7',south:'side',west:'none'}}",
			"{Name:'minecraft:redstone_wire',Properties:{east:'up',north:'up',power:'7',south:'side',west:'side'}}",
			"{Name:'minecraft:redstone_wire',Properties:{east:'up',north:'up',power:'7',south:'side',west:'up'}}",
			"{Name:'minecraft:redstone_wire',Properties:{east:'up',north:'up',power:'7',south:'up',west:'none'}}",
			"{Name:'minecraft:redstone_wire',Properties:{east:'up',north:'up',power:'7',south:'up',west:'side'}}",
			"{Name:'minecraft:redstone_wire',Properties:{east:'up',north:'up',power:'7',south:'up',west:'up'}}"
		);
		putStates(
			888,
			"{Name:'minecraft:redstone_wire',Properties:{east:'none',north:'none',power:'8',south:'none',west:'none'}}",
			"{Name:'minecraft:redstone_wire',Properties:{east:'none',north:'none',power:'8',south:'none',west:'none'}}",
			"{Name:'minecraft:redstone_wire',Properties:{east:'none',north:'none',power:'8',south:'none',west:'side'}}",
			"{Name:'minecraft:redstone_wire',Properties:{east:'none',north:'none',power:'8',south:'none',west:'up'}}",
			"{Name:'minecraft:redstone_wire',Properties:{east:'none',north:'none',power:'8',south:'side',west:'none'}}",
			"{Name:'minecraft:redstone_wire',Properties:{east:'none',north:'none',power:'8',south:'side',west:'side'}}",
			"{Name:'minecraft:redstone_wire',Properties:{east:'none',north:'none',power:'8',south:'side',west:'up'}}",
			"{Name:'minecraft:redstone_wire',Properties:{east:'none',north:'none',power:'8',south:'up',west:'none'}}",
			"{Name:'minecraft:redstone_wire',Properties:{east:'none',north:'none',power:'8',south:'up',west:'side'}}",
			"{Name:'minecraft:redstone_wire',Properties:{east:'none',north:'none',power:'8',south:'up',west:'up'}}",
			"{Name:'minecraft:redstone_wire',Properties:{east:'none',north:'side',power:'8',south:'none',west:'none'}}",
			"{Name:'minecraft:redstone_wire',Properties:{east:'none',north:'side',power:'8',south:'none',west:'side'}}",
			"{Name:'minecraft:redstone_wire',Properties:{east:'none',north:'side',power:'8',south:'none',west:'up'}}",
			"{Name:'minecraft:redstone_wire',Properties:{east:'none',north:'side',power:'8',south:'side',west:'none'}}",
			"{Name:'minecraft:redstone_wire',Properties:{east:'none',north:'side',power:'8',south:'side',west:'side'}}",
			"{Name:'minecraft:redstone_wire',Properties:{east:'none',north:'side',power:'8',south:'side',west:'up'}}",
			"{Name:'minecraft:redstone_wire',Properties:{east:'none',north:'side',power:'8',south:'up',west:'none'}}",
			"{Name:'minecraft:redstone_wire',Properties:{east:'none',north:'side',power:'8',south:'up',west:'side'}}",
			"{Name:'minecraft:redstone_wire',Properties:{east:'none',north:'side',power:'8',south:'up',west:'up'}}",
			"{Name:'minecraft:redstone_wire',Properties:{east:'none',north:'up',power:'8',south:'none',west:'none'}}",
			"{Name:'minecraft:redstone_wire',Properties:{east:'none',north:'up',power:'8',south:'none',west:'side'}}",
			"{Name:'minecraft:redstone_wire',Properties:{east:'none',north:'up',power:'8',south:'none',west:'up'}}",
			"{Name:'minecraft:redstone_wire',Properties:{east:'none',north:'up',power:'8',south:'side',west:'none'}}",
			"{Name:'minecraft:redstone_wire',Properties:{east:'none',north:'up',power:'8',south:'side',west:'side'}}",
			"{Name:'minecraft:redstone_wire',Properties:{east:'none',north:'up',power:'8',south:'side',west:'up'}}",
			"{Name:'minecraft:redstone_wire',Properties:{east:'none',north:'up',power:'8',south:'up',west:'none'}}",
			"{Name:'minecraft:redstone_wire',Properties:{east:'none',north:'up',power:'8',south:'up',west:'side'}}",
			"{Name:'minecraft:redstone_wire',Properties:{east:'none',north:'up',power:'8',south:'up',west:'up'}}",
			"{Name:'minecraft:redstone_wire',Properties:{east:'side',north:'none',power:'8',south:'none',west:'none'}}",
			"{Name:'minecraft:redstone_wire',Properties:{east:'side',north:'none',power:'8',south:'none',west:'side'}}",
			"{Name:'minecraft:redstone_wire',Properties:{east:'side',north:'none',power:'8',south:'none',west:'up'}}",
			"{Name:'minecraft:redstone_wire',Properties:{east:'side',north:'none',power:'8',south:'side',west:'none'}}",
			"{Name:'minecraft:redstone_wire',Properties:{east:'side',north:'none',power:'8',south:'side',west:'side'}}",
			"{Name:'minecraft:redstone_wire',Properties:{east:'side',north:'none',power:'8',south:'side',west:'up'}}",
			"{Name:'minecraft:redstone_wire',Properties:{east:'side',north:'none',power:'8',south:'up',west:'none'}}",
			"{Name:'minecraft:redstone_wire',Properties:{east:'side',north:'none',power:'8',south:'up',west:'side'}}",
			"{Name:'minecraft:redstone_wire',Properties:{east:'side',north:'none',power:'8',south:'up',west:'up'}}",
			"{Name:'minecraft:redstone_wire',Properties:{east:'side',north:'side',power:'8',south:'none',west:'none'}}",
			"{Name:'minecraft:redstone_wire',Properties:{east:'side',north:'side',power:'8',south:'none',west:'side'}}",
			"{Name:'minecraft:redstone_wire',Properties:{east:'side',north:'side',power:'8',south:'none',west:'up'}}",
			"{Name:'minecraft:redstone_wire',Properties:{east:'side',north:'side',power:'8',south:'side',west:'none'}}",
			"{Name:'minecraft:redstone_wire',Properties:{east:'side',north:'side',power:'8',south:'side',west:'side'}}",
			"{Name:'minecraft:redstone_wire',Properties:{east:'side',north:'side',power:'8',south:'side',west:'up'}}",
			"{Name:'minecraft:redstone_wire',Properties:{east:'side',north:'side',power:'8',south:'up',west:'none'}}",
			"{Name:'minecraft:redstone_wire',Properties:{east:'side',north:'side',power:'8',south:'up',west:'side'}}",
			"{Name:'minecraft:redstone_wire',Properties:{east:'side',north:'side',power:'8',south:'up',west:'up'}}",
			"{Name:'minecraft:redstone_wire',Properties:{east:'side',north:'up',power:'8',south:'none',west:'none'}}",
			"{Name:'minecraft:redstone_wire',Properties:{east:'side',north:'up',power:'8',south:'none',west:'side'}}",
			"{Name:'minecraft:redstone_wire',Properties:{east:'side',north:'up',power:'8',south:'none',west:'up'}}",
			"{Name:'minecraft:redstone_wire',Properties:{east:'side',north:'up',power:'8',south:'side',west:'none'}}",
			"{Name:'minecraft:redstone_wire',Properties:{east:'side',north:'up',power:'8',south:'side',west:'side'}}",
			"{Name:'minecraft:redstone_wire',Properties:{east:'side',north:'up',power:'8',south:'side',west:'up'}}",
			"{Name:'minecraft:redstone_wire',Properties:{east:'side',north:'up',power:'8',south:'up',west:'none'}}",
			"{Name:'minecraft:redstone_wire',Properties:{east:'side',north:'up',power:'8',south:'up',west:'side'}}",
			"{Name:'minecraft:redstone_wire',Properties:{east:'side',north:'up',power:'8',south:'up',west:'up'}}",
			"{Name:'minecraft:redstone_wire',Properties:{east:'up',north:'none',power:'8',south:'none',west:'none'}}",
			"{Name:'minecraft:redstone_wire',Properties:{east:'up',north:'none',power:'8',south:'none',west:'side'}}",
			"{Name:'minecraft:redstone_wire',Properties:{east:'up',north:'none',power:'8',south:'none',west:'up'}}",
			"{Name:'minecraft:redstone_wire',Properties:{east:'up',north:'none',power:'8',south:'side',west:'none'}}",
			"{Name:'minecraft:redstone_wire',Properties:{east:'up',north:'none',power:'8',south:'side',west:'side'}}",
			"{Name:'minecraft:redstone_wire',Properties:{east:'up',north:'none',power:'8',south:'side',west:'up'}}",
			"{Name:'minecraft:redstone_wire',Properties:{east:'up',north:'none',power:'8',south:'up',west:'none'}}",
			"{Name:'minecraft:redstone_wire',Properties:{east:'up',north:'none',power:'8',south:'up',west:'side'}}",
			"{Name:'minecraft:redstone_wire',Properties:{east:'up',north:'none',power:'8',south:'up',west:'up'}}",
			"{Name:'minecraft:redstone_wire',Properties:{east:'up',north:'side',power:'8',south:'none',west:'none'}}",
			"{Name:'minecraft:redstone_wire',Properties:{east:'up',north:'side',power:'8',south:'none',west:'side'}}",
			"{Name:'minecraft:redstone_wire',Properties:{east:'up',north:'side',power:'8',south:'none',west:'up'}}",
			"{Name:'minecraft:redstone_wire',Properties:{east:'up',north:'side',power:'8',south:'side',west:'none'}}",
			"{Name:'minecraft:redstone_wire',Properties:{east:'up',north:'side',power:'8',south:'side',west:'side'}}",
			"{Name:'minecraft:redstone_wire',Properties:{east:'up',north:'side',power:'8',south:'side',west:'up'}}",
			"{Name:'minecraft:redstone_wire',Properties:{east:'up',north:'side',power:'8',south:'up',west:'none'}}",
			"{Name:'minecraft:redstone_wire',Properties:{east:'up',north:'side',power:'8',south:'up',west:'side'}}",
			"{Name:'minecraft:redstone_wire',Properties:{east:'up',north:'side',power:'8',south:'up',west:'up'}}",
			"{Name:'minecraft:redstone_wire',Properties:{east:'up',north:'up',power:'8',south:'none',west:'none'}}",
			"{Name:'minecraft:redstone_wire',Properties:{east:'up',north:'up',power:'8',south:'none',west:'side'}}",
			"{Name:'minecraft:redstone_wire',Properties:{east:'up',north:'up',power:'8',south:'none',west:'up'}}",
			"{Name:'minecraft:redstone_wire',Properties:{east:'up',north:'up',power:'8',south:'side',west:'none'}}",
			"{Name:'minecraft:redstone_wire',Properties:{east:'up',north:'up',power:'8',south:'side',west:'side'}}",
			"{Name:'minecraft:redstone_wire',Properties:{east:'up',north:'up',power:'8',south:'side',west:'up'}}",
			"{Name:'minecraft:redstone_wire',Properties:{east:'up',north:'up',power:'8',south:'up',west:'none'}}",
			"{Name:'minecraft:redstone_wire',Properties:{east:'up',north:'up',power:'8',south:'up',west:'side'}}",
			"{Name:'minecraft:redstone_wire',Properties:{east:'up',north:'up',power:'8',south:'up',west:'up'}}"
		);
		putStates(
			889,
			"{Name:'minecraft:redstone_wire',Properties:{east:'none',north:'none',power:'9',south:'none',west:'none'}}",
			"{Name:'minecraft:redstone_wire',Properties:{east:'none',north:'none',power:'9',south:'none',west:'none'}}",
			"{Name:'minecraft:redstone_wire',Properties:{east:'none',north:'none',power:'9',south:'none',west:'side'}}",
			"{Name:'minecraft:redstone_wire',Properties:{east:'none',north:'none',power:'9',south:'none',west:'up'}}",
			"{Name:'minecraft:redstone_wire',Properties:{east:'none',north:'none',power:'9',south:'side',west:'none'}}",
			"{Name:'minecraft:redstone_wire',Properties:{east:'none',north:'none',power:'9',south:'side',west:'side'}}",
			"{Name:'minecraft:redstone_wire',Properties:{east:'none',north:'none',power:'9',south:'side',west:'up'}}",
			"{Name:'minecraft:redstone_wire',Properties:{east:'none',north:'none',power:'9',south:'up',west:'none'}}",
			"{Name:'minecraft:redstone_wire',Properties:{east:'none',north:'none',power:'9',south:'up',west:'side'}}",
			"{Name:'minecraft:redstone_wire',Properties:{east:'none',north:'none',power:'9',south:'up',west:'up'}}",
			"{Name:'minecraft:redstone_wire',Properties:{east:'none',north:'side',power:'9',south:'none',west:'none'}}",
			"{Name:'minecraft:redstone_wire',Properties:{east:'none',north:'side',power:'9',south:'none',west:'side'}}",
			"{Name:'minecraft:redstone_wire',Properties:{east:'none',north:'side',power:'9',south:'none',west:'up'}}",
			"{Name:'minecraft:redstone_wire',Properties:{east:'none',north:'side',power:'9',south:'side',west:'none'}}",
			"{Name:'minecraft:redstone_wire',Properties:{east:'none',north:'side',power:'9',south:'side',west:'side'}}",
			"{Name:'minecraft:redstone_wire',Properties:{east:'none',north:'side',power:'9',south:'side',west:'up'}}",
			"{Name:'minecraft:redstone_wire',Properties:{east:'none',north:'side',power:'9',south:'up',west:'none'}}",
			"{Name:'minecraft:redstone_wire',Properties:{east:'none',north:'side',power:'9',south:'up',west:'side'}}",
			"{Name:'minecraft:redstone_wire',Properties:{east:'none',north:'side',power:'9',south:'up',west:'up'}}",
			"{Name:'minecraft:redstone_wire',Properties:{east:'none',north:'up',power:'9',south:'none',west:'none'}}",
			"{Name:'minecraft:redstone_wire',Properties:{east:'none',north:'up',power:'9',south:'none',west:'side'}}",
			"{Name:'minecraft:redstone_wire',Properties:{east:'none',north:'up',power:'9',south:'none',west:'up'}}",
			"{Name:'minecraft:redstone_wire',Properties:{east:'none',north:'up',power:'9',south:'side',west:'none'}}",
			"{Name:'minecraft:redstone_wire',Properties:{east:'none',north:'up',power:'9',south:'side',west:'side'}}",
			"{Name:'minecraft:redstone_wire',Properties:{east:'none',north:'up',power:'9',south:'side',west:'up'}}",
			"{Name:'minecraft:redstone_wire',Properties:{east:'none',north:'up',power:'9',south:'up',west:'none'}}",
			"{Name:'minecraft:redstone_wire',Properties:{east:'none',north:'up',power:'9',south:'up',west:'side'}}",
			"{Name:'minecraft:redstone_wire',Properties:{east:'none',north:'up',power:'9',south:'up',west:'up'}}",
			"{Name:'minecraft:redstone_wire',Properties:{east:'side',north:'none',power:'9',south:'none',west:'none'}}",
			"{Name:'minecraft:redstone_wire',Properties:{east:'side',north:'none',power:'9',south:'none',west:'side'}}",
			"{Name:'minecraft:redstone_wire',Properties:{east:'side',north:'none',power:'9',south:'none',west:'up'}}",
			"{Name:'minecraft:redstone_wire',Properties:{east:'side',north:'none',power:'9',south:'side',west:'none'}}",
			"{Name:'minecraft:redstone_wire',Properties:{east:'side',north:'none',power:'9',south:'side',west:'side'}}",
			"{Name:'minecraft:redstone_wire',Properties:{east:'side',north:'none',power:'9',south:'side',west:'up'}}",
			"{Name:'minecraft:redstone_wire',Properties:{east:'side',north:'none',power:'9',south:'up',west:'none'}}",
			"{Name:'minecraft:redstone_wire',Properties:{east:'side',north:'none',power:'9',south:'up',west:'side'}}",
			"{Name:'minecraft:redstone_wire',Properties:{east:'side',north:'none',power:'9',south:'up',west:'up'}}",
			"{Name:'minecraft:redstone_wire',Properties:{east:'side',north:'side',power:'9',south:'none',west:'none'}}",
			"{Name:'minecraft:redstone_wire',Properties:{east:'side',north:'side',power:'9',south:'none',west:'side'}}",
			"{Name:'minecraft:redstone_wire',Properties:{east:'side',north:'side',power:'9',south:'none',west:'up'}}",
			"{Name:'minecraft:redstone_wire',Properties:{east:'side',north:'side',power:'9',south:'side',west:'none'}}",
			"{Name:'minecraft:redstone_wire',Properties:{east:'side',north:'side',power:'9',south:'side',west:'side'}}",
			"{Name:'minecraft:redstone_wire',Properties:{east:'side',north:'side',power:'9',south:'side',west:'up'}}",
			"{Name:'minecraft:redstone_wire',Properties:{east:'side',north:'side',power:'9',south:'up',west:'none'}}",
			"{Name:'minecraft:redstone_wire',Properties:{east:'side',north:'side',power:'9',south:'up',west:'side'}}",
			"{Name:'minecraft:redstone_wire',Properties:{east:'side',north:'side',power:'9',south:'up',west:'up'}}",
			"{Name:'minecraft:redstone_wire',Properties:{east:'side',north:'up',power:'9',south:'none',west:'none'}}",
			"{Name:'minecraft:redstone_wire',Properties:{east:'side',north:'up',power:'9',south:'none',west:'side'}}",
			"{Name:'minecraft:redstone_wire',Properties:{east:'side',north:'up',power:'9',south:'none',west:'up'}}",
			"{Name:'minecraft:redstone_wire',Properties:{east:'side',north:'up',power:'9',south:'side',west:'none'}}",
			"{Name:'minecraft:redstone_wire',Properties:{east:'side',north:'up',power:'9',south:'side',west:'side'}}",
			"{Name:'minecraft:redstone_wire',Properties:{east:'side',north:'up',power:'9',south:'side',west:'up'}}",
			"{Name:'minecraft:redstone_wire',Properties:{east:'side',north:'up',power:'9',south:'up',west:'none'}}",
			"{Name:'minecraft:redstone_wire',Properties:{east:'side',north:'up',power:'9',south:'up',west:'side'}}",
			"{Name:'minecraft:redstone_wire',Properties:{east:'side',north:'up',power:'9',south:'up',west:'up'}}",
			"{Name:'minecraft:redstone_wire',Properties:{east:'up',north:'none',power:'9',south:'none',west:'none'}}",
			"{Name:'minecraft:redstone_wire',Properties:{east:'up',north:'none',power:'9',south:'none',west:'side'}}",
			"{Name:'minecraft:redstone_wire',Properties:{east:'up',north:'none',power:'9',south:'none',west:'up'}}",
			"{Name:'minecraft:redstone_wire',Properties:{east:'up',north:'none',power:'9',south:'side',west:'none'}}",
			"{Name:'minecraft:redstone_wire',Properties:{east:'up',north:'none',power:'9',south:'side',west:'side'}}",
			"{Name:'minecraft:redstone_wire',Properties:{east:'up',north:'none',power:'9',south:'side',west:'up'}}",
			"{Name:'minecraft:redstone_wire',Properties:{east:'up',north:'none',power:'9',south:'up',west:'none'}}",
			"{Name:'minecraft:redstone_wire',Properties:{east:'up',north:'none',power:'9',south:'up',west:'side'}}",
			"{Name:'minecraft:redstone_wire',Properties:{east:'up',north:'none',power:'9',south:'up',west:'up'}}",
			"{Name:'minecraft:redstone_wire',Properties:{east:'up',north:'side',power:'9',south:'none',west:'none'}}",
			"{Name:'minecraft:redstone_wire',Properties:{east:'up',north:'side',power:'9',south:'none',west:'side'}}",
			"{Name:'minecraft:redstone_wire',Properties:{east:'up',north:'side',power:'9',south:'none',west:'up'}}",
			"{Name:'minecraft:redstone_wire',Properties:{east:'up',north:'side',power:'9',south:'side',west:'none'}}",
			"{Name:'minecraft:redstone_wire',Properties:{east:'up',north:'side',power:'9',south:'side',west:'side'}}",
			"{Name:'minecraft:redstone_wire',Properties:{east:'up',north:'side',power:'9',south:'side',west:'up'}}",
			"{Name:'minecraft:redstone_wire',Properties:{east:'up',north:'side',power:'9',south:'up',west:'none'}}",
			"{Name:'minecraft:redstone_wire',Properties:{east:'up',north:'side',power:'9',south:'up',west:'side'}}",
			"{Name:'minecraft:redstone_wire',Properties:{east:'up',north:'side',power:'9',south:'up',west:'up'}}",
			"{Name:'minecraft:redstone_wire',Properties:{east:'up',north:'up',power:'9',south:'none',west:'none'}}",
			"{Name:'minecraft:redstone_wire',Properties:{east:'up',north:'up',power:'9',south:'none',west:'side'}}",
			"{Name:'minecraft:redstone_wire',Properties:{east:'up',north:'up',power:'9',south:'none',west:'up'}}",
			"{Name:'minecraft:redstone_wire',Properties:{east:'up',north:'up',power:'9',south:'side',west:'none'}}",
			"{Name:'minecraft:redstone_wire',Properties:{east:'up',north:'up',power:'9',south:'side',west:'side'}}",
			"{Name:'minecraft:redstone_wire',Properties:{east:'up',north:'up',power:'9',south:'side',west:'up'}}",
			"{Name:'minecraft:redstone_wire',Properties:{east:'up',north:'up',power:'9',south:'up',west:'none'}}",
			"{Name:'minecraft:redstone_wire',Properties:{east:'up',north:'up',power:'9',south:'up',west:'side'}}",
			"{Name:'minecraft:redstone_wire',Properties:{east:'up',north:'up',power:'9',south:'up',west:'up'}}"
		);
		putStates(
			890,
			"{Name:'minecraft:redstone_wire',Properties:{east:'none',north:'none',power:'10',south:'none',west:'none'}}",
			"{Name:'minecraft:redstone_wire',Properties:{east:'none',north:'none',power:'10',south:'none',west:'none'}}",
			"{Name:'minecraft:redstone_wire',Properties:{east:'none',north:'none',power:'10',south:'none',west:'side'}}",
			"{Name:'minecraft:redstone_wire',Properties:{east:'none',north:'none',power:'10',south:'none',west:'up'}}",
			"{Name:'minecraft:redstone_wire',Properties:{east:'none',north:'none',power:'10',south:'side',west:'none'}}",
			"{Name:'minecraft:redstone_wire',Properties:{east:'none',north:'none',power:'10',south:'side',west:'side'}}",
			"{Name:'minecraft:redstone_wire',Properties:{east:'none',north:'none',power:'10',south:'side',west:'up'}}",
			"{Name:'minecraft:redstone_wire',Properties:{east:'none',north:'none',power:'10',south:'up',west:'none'}}",
			"{Name:'minecraft:redstone_wire',Properties:{east:'none',north:'none',power:'10',south:'up',west:'side'}}",
			"{Name:'minecraft:redstone_wire',Properties:{east:'none',north:'none',power:'10',south:'up',west:'up'}}",
			"{Name:'minecraft:redstone_wire',Properties:{east:'none',north:'side',power:'10',south:'none',west:'none'}}",
			"{Name:'minecraft:redstone_wire',Properties:{east:'none',north:'side',power:'10',south:'none',west:'side'}}",
			"{Name:'minecraft:redstone_wire',Properties:{east:'none',north:'side',power:'10',south:'none',west:'up'}}",
			"{Name:'minecraft:redstone_wire',Properties:{east:'none',north:'side',power:'10',south:'side',west:'none'}}",
			"{Name:'minecraft:redstone_wire',Properties:{east:'none',north:'side',power:'10',south:'side',west:'side'}}",
			"{Name:'minecraft:redstone_wire',Properties:{east:'none',north:'side',power:'10',south:'side',west:'up'}}",
			"{Name:'minecraft:redstone_wire',Properties:{east:'none',north:'side',power:'10',south:'up',west:'none'}}",
			"{Name:'minecraft:redstone_wire',Properties:{east:'none',north:'side',power:'10',south:'up',west:'side'}}",
			"{Name:'minecraft:redstone_wire',Properties:{east:'none',north:'side',power:'10',south:'up',west:'up'}}",
			"{Name:'minecraft:redstone_wire',Properties:{east:'none',north:'up',power:'10',south:'none',west:'none'}}",
			"{Name:'minecraft:redstone_wire',Properties:{east:'none',north:'up',power:'10',south:'none',west:'side'}}",
			"{Name:'minecraft:redstone_wire',Properties:{east:'none',north:'up',power:'10',south:'none',west:'up'}}",
			"{Name:'minecraft:redstone_wire',Properties:{east:'none',north:'up',power:'10',south:'side',west:'none'}}",
			"{Name:'minecraft:redstone_wire',Properties:{east:'none',north:'up',power:'10',south:'side',west:'side'}}",
			"{Name:'minecraft:redstone_wire',Properties:{east:'none',north:'up',power:'10',south:'side',west:'up'}}",
			"{Name:'minecraft:redstone_wire',Properties:{east:'none',north:'up',power:'10',south:'up',west:'none'}}",
			"{Name:'minecraft:redstone_wire',Properties:{east:'none',north:'up',power:'10',south:'up',west:'side'}}",
			"{Name:'minecraft:redstone_wire',Properties:{east:'none',north:'up',power:'10',south:'up',west:'up'}}",
			"{Name:'minecraft:redstone_wire',Properties:{east:'side',north:'none',power:'10',south:'none',west:'none'}}",
			"{Name:'minecraft:redstone_wire',Properties:{east:'side',north:'none',power:'10',south:'none',west:'side'}}",
			"{Name:'minecraft:redstone_wire',Properties:{east:'side',north:'none',power:'10',south:'none',west:'up'}}",
			"{Name:'minecraft:redstone_wire',Properties:{east:'side',north:'none',power:'10',south:'side',west:'none'}}",
			"{Name:'minecraft:redstone_wire',Properties:{east:'side',north:'none',power:'10',south:'side',west:'side'}}",
			"{Name:'minecraft:redstone_wire',Properties:{east:'side',north:'none',power:'10',south:'side',west:'up'}}",
			"{Name:'minecraft:redstone_wire',Properties:{east:'side',north:'none',power:'10',south:'up',west:'none'}}",
			"{Name:'minecraft:redstone_wire',Properties:{east:'side',north:'none',power:'10',south:'up',west:'side'}}",
			"{Name:'minecraft:redstone_wire',Properties:{east:'side',north:'none',power:'10',south:'up',west:'up'}}",
			"{Name:'minecraft:redstone_wire',Properties:{east:'side',north:'side',power:'10',south:'none',west:'none'}}",
			"{Name:'minecraft:redstone_wire',Properties:{east:'side',north:'side',power:'10',south:'none',west:'side'}}",
			"{Name:'minecraft:redstone_wire',Properties:{east:'side',north:'side',power:'10',south:'none',west:'up'}}",
			"{Name:'minecraft:redstone_wire',Properties:{east:'side',north:'side',power:'10',south:'side',west:'none'}}",
			"{Name:'minecraft:redstone_wire',Properties:{east:'side',north:'side',power:'10',south:'side',west:'side'}}",
			"{Name:'minecraft:redstone_wire',Properties:{east:'side',north:'side',power:'10',south:'side',west:'up'}}",
			"{Name:'minecraft:redstone_wire',Properties:{east:'side',north:'side',power:'10',south:'up',west:'none'}}",
			"{Name:'minecraft:redstone_wire',Properties:{east:'side',north:'side',power:'10',south:'up',west:'side'}}",
			"{Name:'minecraft:redstone_wire',Properties:{east:'side',north:'side',power:'10',south:'up',west:'up'}}",
			"{Name:'minecraft:redstone_wire',Properties:{east:'side',north:'up',power:'10',south:'none',west:'none'}}",
			"{Name:'minecraft:redstone_wire',Properties:{east:'side',north:'up',power:'10',south:'none',west:'side'}}",
			"{Name:'minecraft:redstone_wire',Properties:{east:'side',north:'up',power:'10',south:'none',west:'up'}}",
			"{Name:'minecraft:redstone_wire',Properties:{east:'side',north:'up',power:'10',south:'side',west:'none'}}",
			"{Name:'minecraft:redstone_wire',Properties:{east:'side',north:'up',power:'10',south:'side',west:'side'}}",
			"{Name:'minecraft:redstone_wire',Properties:{east:'side',north:'up',power:'10',south:'side',west:'up'}}",
			"{Name:'minecraft:redstone_wire',Properties:{east:'side',north:'up',power:'10',south:'up',west:'none'}}",
			"{Name:'minecraft:redstone_wire',Properties:{east:'side',north:'up',power:'10',south:'up',west:'side'}}",
			"{Name:'minecraft:redstone_wire',Properties:{east:'side',north:'up',power:'10',south:'up',west:'up'}}",
			"{Name:'minecraft:redstone_wire',Properties:{east:'up',north:'none',power:'10',south:'none',west:'none'}}",
			"{Name:'minecraft:redstone_wire',Properties:{east:'up',north:'none',power:'10',south:'none',west:'side'}}",
			"{Name:'minecraft:redstone_wire',Properties:{east:'up',north:'none',power:'10',south:'none',west:'up'}}",
			"{Name:'minecraft:redstone_wire',Properties:{east:'up',north:'none',power:'10',south:'side',west:'none'}}",
			"{Name:'minecraft:redstone_wire',Properties:{east:'up',north:'none',power:'10',south:'side',west:'side'}}",
			"{Name:'minecraft:redstone_wire',Properties:{east:'up',north:'none',power:'10',south:'side',west:'up'}}",
			"{Name:'minecraft:redstone_wire',Properties:{east:'up',north:'none',power:'10',south:'up',west:'none'}}",
			"{Name:'minecraft:redstone_wire',Properties:{east:'up',north:'none',power:'10',south:'up',west:'side'}}",
			"{Name:'minecraft:redstone_wire',Properties:{east:'up',north:'none',power:'10',south:'up',west:'up'}}",
			"{Name:'minecraft:redstone_wire',Properties:{east:'up',north:'side',power:'10',south:'none',west:'none'}}",
			"{Name:'minecraft:redstone_wire',Properties:{east:'up',north:'side',power:'10',south:'none',west:'side'}}",
			"{Name:'minecraft:redstone_wire',Properties:{east:'up',north:'side',power:'10',south:'none',west:'up'}}",
			"{Name:'minecraft:redstone_wire',Properties:{east:'up',north:'side',power:'10',south:'side',west:'none'}}",
			"{Name:'minecraft:redstone_wire',Properties:{east:'up',north:'side',power:'10',south:'side',west:'side'}}",
			"{Name:'minecraft:redstone_wire',Properties:{east:'up',north:'side',power:'10',south:'side',west:'up'}}",
			"{Name:'minecraft:redstone_wire',Properties:{east:'up',north:'side',power:'10',south:'up',west:'none'}}",
			"{Name:'minecraft:redstone_wire',Properties:{east:'up',north:'side',power:'10',south:'up',west:'side'}}",
			"{Name:'minecraft:redstone_wire',Properties:{east:'up',north:'side',power:'10',south:'up',west:'up'}}",
			"{Name:'minecraft:redstone_wire',Properties:{east:'up',north:'up',power:'10',south:'none',west:'none'}}",
			"{Name:'minecraft:redstone_wire',Properties:{east:'up',north:'up',power:'10',south:'none',west:'side'}}",
			"{Name:'minecraft:redstone_wire',Properties:{east:'up',north:'up',power:'10',south:'none',west:'up'}}",
			"{Name:'minecraft:redstone_wire',Properties:{east:'up',north:'up',power:'10',south:'side',west:'none'}}",
			"{Name:'minecraft:redstone_wire',Properties:{east:'up',north:'up',power:'10',south:'side',west:'side'}}",
			"{Name:'minecraft:redstone_wire',Properties:{east:'up',north:'up',power:'10',south:'side',west:'up'}}",
			"{Name:'minecraft:redstone_wire',Properties:{east:'up',north:'up',power:'10',south:'up',west:'none'}}",
			"{Name:'minecraft:redstone_wire',Properties:{east:'up',north:'up',power:'10',south:'up',west:'side'}}",
			"{Name:'minecraft:redstone_wire',Properties:{east:'up',north:'up',power:'10',south:'up',west:'up'}}"
		);
		putStates(
			891,
			"{Name:'minecraft:redstone_wire',Properties:{east:'none',north:'none',power:'11',south:'none',west:'none'}}",
			"{Name:'minecraft:redstone_wire',Properties:{east:'none',north:'none',power:'11',south:'none',west:'none'}}",
			"{Name:'minecraft:redstone_wire',Properties:{east:'none',north:'none',power:'11',south:'none',west:'side'}}",
			"{Name:'minecraft:redstone_wire',Properties:{east:'none',north:'none',power:'11',south:'none',west:'up'}}",
			"{Name:'minecraft:redstone_wire',Properties:{east:'none',north:'none',power:'11',south:'side',west:'none'}}",
			"{Name:'minecraft:redstone_wire',Properties:{east:'none',north:'none',power:'11',south:'side',west:'side'}}",
			"{Name:'minecraft:redstone_wire',Properties:{east:'none',north:'none',power:'11',south:'side',west:'up'}}",
			"{Name:'minecraft:redstone_wire',Properties:{east:'none',north:'none',power:'11',south:'up',west:'none'}}",
			"{Name:'minecraft:redstone_wire',Properties:{east:'none',north:'none',power:'11',south:'up',west:'side'}}",
			"{Name:'minecraft:redstone_wire',Properties:{east:'none',north:'none',power:'11',south:'up',west:'up'}}",
			"{Name:'minecraft:redstone_wire',Properties:{east:'none',north:'side',power:'11',south:'none',west:'none'}}",
			"{Name:'minecraft:redstone_wire',Properties:{east:'none',north:'side',power:'11',south:'none',west:'side'}}",
			"{Name:'minecraft:redstone_wire',Properties:{east:'none',north:'side',power:'11',south:'none',west:'up'}}",
			"{Name:'minecraft:redstone_wire',Properties:{east:'none',north:'side',power:'11',south:'side',west:'none'}}",
			"{Name:'minecraft:redstone_wire',Properties:{east:'none',north:'side',power:'11',south:'side',west:'side'}}",
			"{Name:'minecraft:redstone_wire',Properties:{east:'none',north:'side',power:'11',south:'side',west:'up'}}",
			"{Name:'minecraft:redstone_wire',Properties:{east:'none',north:'side',power:'11',south:'up',west:'none'}}",
			"{Name:'minecraft:redstone_wire',Properties:{east:'none',north:'side',power:'11',south:'up',west:'side'}}",
			"{Name:'minecraft:redstone_wire',Properties:{east:'none',north:'side',power:'11',south:'up',west:'up'}}",
			"{Name:'minecraft:redstone_wire',Properties:{east:'none',north:'up',power:'11',south:'none',west:'none'}}",
			"{Name:'minecraft:redstone_wire',Properties:{east:'none',north:'up',power:'11',south:'none',west:'side'}}",
			"{Name:'minecraft:redstone_wire',Properties:{east:'none',north:'up',power:'11',south:'none',west:'up'}}",
			"{Name:'minecraft:redstone_wire',Properties:{east:'none',north:'up',power:'11',south:'side',west:'none'}}",
			"{Name:'minecraft:redstone_wire',Properties:{east:'none',north:'up',power:'11',south:'side',west:'side'}}",
			"{Name:'minecraft:redstone_wire',Properties:{east:'none',north:'up',power:'11',south:'side',west:'up'}}",
			"{Name:'minecraft:redstone_wire',Properties:{east:'none',north:'up',power:'11',south:'up',west:'none'}}",
			"{Name:'minecraft:redstone_wire',Properties:{east:'none',north:'up',power:'11',south:'up',west:'side'}}",
			"{Name:'minecraft:redstone_wire',Properties:{east:'none',north:'up',power:'11',south:'up',west:'up'}}",
			"{Name:'minecraft:redstone_wire',Properties:{east:'side',north:'none',power:'11',south:'none',west:'none'}}",
			"{Name:'minecraft:redstone_wire',Properties:{east:'side',north:'none',power:'11',south:'none',west:'side'}}",
			"{Name:'minecraft:redstone_wire',Properties:{east:'side',north:'none',power:'11',south:'none',west:'up'}}",
			"{Name:'minecraft:redstone_wire',Properties:{east:'side',north:'none',power:'11',south:'side',west:'none'}}",
			"{Name:'minecraft:redstone_wire',Properties:{east:'side',north:'none',power:'11',south:'side',west:'side'}}",
			"{Name:'minecraft:redstone_wire',Properties:{east:'side',north:'none',power:'11',south:'side',west:'up'}}",
			"{Name:'minecraft:redstone_wire',Properties:{east:'side',north:'none',power:'11',south:'up',west:'none'}}",
			"{Name:'minecraft:redstone_wire',Properties:{east:'side',north:'none',power:'11',south:'up',west:'side'}}",
			"{Name:'minecraft:redstone_wire',Properties:{east:'side',north:'none',power:'11',south:'up',west:'up'}}",
			"{Name:'minecraft:redstone_wire',Properties:{east:'side',north:'side',power:'11',south:'none',west:'none'}}",
			"{Name:'minecraft:redstone_wire',Properties:{east:'side',north:'side',power:'11',south:'none',west:'side'}}",
			"{Name:'minecraft:redstone_wire',Properties:{east:'side',north:'side',power:'11',south:'none',west:'up'}}",
			"{Name:'minecraft:redstone_wire',Properties:{east:'side',north:'side',power:'11',south:'side',west:'none'}}",
			"{Name:'minecraft:redstone_wire',Properties:{east:'side',north:'side',power:'11',south:'side',west:'side'}}",
			"{Name:'minecraft:redstone_wire',Properties:{east:'side',north:'side',power:'11',south:'side',west:'up'}}",
			"{Name:'minecraft:redstone_wire',Properties:{east:'side',north:'side',power:'11',south:'up',west:'none'}}",
			"{Name:'minecraft:redstone_wire',Properties:{east:'side',north:'side',power:'11',south:'up',west:'side'}}",
			"{Name:'minecraft:redstone_wire',Properties:{east:'side',north:'side',power:'11',south:'up',west:'up'}}",
			"{Name:'minecraft:redstone_wire',Properties:{east:'side',north:'up',power:'11',south:'none',west:'none'}}",
			"{Name:'minecraft:redstone_wire',Properties:{east:'side',north:'up',power:'11',south:'none',west:'side'}}",
			"{Name:'minecraft:redstone_wire',Properties:{east:'side',north:'up',power:'11',south:'none',west:'up'}}",
			"{Name:'minecraft:redstone_wire',Properties:{east:'side',north:'up',power:'11',south:'side',west:'none'}}",
			"{Name:'minecraft:redstone_wire',Properties:{east:'side',north:'up',power:'11',south:'side',west:'side'}}",
			"{Name:'minecraft:redstone_wire',Properties:{east:'side',north:'up',power:'11',south:'side',west:'up'}}",
			"{Name:'minecraft:redstone_wire',Properties:{east:'side',north:'up',power:'11',south:'up',west:'none'}}",
			"{Name:'minecraft:redstone_wire',Properties:{east:'side',north:'up',power:'11',south:'up',west:'side'}}",
			"{Name:'minecraft:redstone_wire',Properties:{east:'side',north:'up',power:'11',south:'up',west:'up'}}",
			"{Name:'minecraft:redstone_wire',Properties:{east:'up',north:'none',power:'11',south:'none',west:'none'}}",
			"{Name:'minecraft:redstone_wire',Properties:{east:'up',north:'none',power:'11',south:'none',west:'side'}}",
			"{Name:'minecraft:redstone_wire',Properties:{east:'up',north:'none',power:'11',south:'none',west:'up'}}",
			"{Name:'minecraft:redstone_wire',Properties:{east:'up',north:'none',power:'11',south:'side',west:'none'}}",
			"{Name:'minecraft:redstone_wire',Properties:{east:'up',north:'none',power:'11',south:'side',west:'side'}}",
			"{Name:'minecraft:redstone_wire',Properties:{east:'up',north:'none',power:'11',south:'side',west:'up'}}",
			"{Name:'minecraft:redstone_wire',Properties:{east:'up',north:'none',power:'11',south:'up',west:'none'}}",
			"{Name:'minecraft:redstone_wire',Properties:{east:'up',north:'none',power:'11',south:'up',west:'side'}}",
			"{Name:'minecraft:redstone_wire',Properties:{east:'up',north:'none',power:'11',south:'up',west:'up'}}",
			"{Name:'minecraft:redstone_wire',Properties:{east:'up',north:'side',power:'11',south:'none',west:'none'}}",
			"{Name:'minecraft:redstone_wire',Properties:{east:'up',north:'side',power:'11',south:'none',west:'side'}}",
			"{Name:'minecraft:redstone_wire',Properties:{east:'up',north:'side',power:'11',south:'none',west:'up'}}",
			"{Name:'minecraft:redstone_wire',Properties:{east:'up',north:'side',power:'11',south:'side',west:'none'}}",
			"{Name:'minecraft:redstone_wire',Properties:{east:'up',north:'side',power:'11',south:'side',west:'side'}}",
			"{Name:'minecraft:redstone_wire',Properties:{east:'up',north:'side',power:'11',south:'side',west:'up'}}",
			"{Name:'minecraft:redstone_wire',Properties:{east:'up',north:'side',power:'11',south:'up',west:'none'}}",
			"{Name:'minecraft:redstone_wire',Properties:{east:'up',north:'side',power:'11',south:'up',west:'side'}}",
			"{Name:'minecraft:redstone_wire',Properties:{east:'up',north:'side',power:'11',south:'up',west:'up'}}",
			"{Name:'minecraft:redstone_wire',Properties:{east:'up',north:'up',power:'11',south:'none',west:'none'}}",
			"{Name:'minecraft:redstone_wire',Properties:{east:'up',north:'up',power:'11',south:'none',west:'side'}}",
			"{Name:'minecraft:redstone_wire',Properties:{east:'up',north:'up',power:'11',south:'none',west:'up'}}",
			"{Name:'minecraft:redstone_wire',Properties:{east:'up',north:'up',power:'11',south:'side',west:'none'}}",
			"{Name:'minecraft:redstone_wire',Properties:{east:'up',north:'up',power:'11',south:'side',west:'side'}}",
			"{Name:'minecraft:redstone_wire',Properties:{east:'up',north:'up',power:'11',south:'side',west:'up'}}",
			"{Name:'minecraft:redstone_wire',Properties:{east:'up',north:'up',power:'11',south:'up',west:'none'}}",
			"{Name:'minecraft:redstone_wire',Properties:{east:'up',north:'up',power:'11',south:'up',west:'side'}}",
			"{Name:'minecraft:redstone_wire',Properties:{east:'up',north:'up',power:'11',south:'up',west:'up'}}"
		);
		putStates(
			892,
			"{Name:'minecraft:redstone_wire',Properties:{east:'none',north:'none',power:'12',south:'none',west:'none'}}",
			"{Name:'minecraft:redstone_wire',Properties:{east:'none',north:'none',power:'12',south:'none',west:'none'}}",
			"{Name:'minecraft:redstone_wire',Properties:{east:'none',north:'none',power:'12',south:'none',west:'side'}}",
			"{Name:'minecraft:redstone_wire',Properties:{east:'none',north:'none',power:'12',south:'none',west:'up'}}",
			"{Name:'minecraft:redstone_wire',Properties:{east:'none',north:'none',power:'12',south:'side',west:'none'}}",
			"{Name:'minecraft:redstone_wire',Properties:{east:'none',north:'none',power:'12',south:'side',west:'side'}}",
			"{Name:'minecraft:redstone_wire',Properties:{east:'none',north:'none',power:'12',south:'side',west:'up'}}",
			"{Name:'minecraft:redstone_wire',Properties:{east:'none',north:'none',power:'12',south:'up',west:'none'}}",
			"{Name:'minecraft:redstone_wire',Properties:{east:'none',north:'none',power:'12',south:'up',west:'side'}}",
			"{Name:'minecraft:redstone_wire',Properties:{east:'none',north:'none',power:'12',south:'up',west:'up'}}",
			"{Name:'minecraft:redstone_wire',Properties:{east:'none',north:'side',power:'12',south:'none',west:'none'}}",
			"{Name:'minecraft:redstone_wire',Properties:{east:'none',north:'side',power:'12',south:'none',west:'side'}}",
			"{Name:'minecraft:redstone_wire',Properties:{east:'none',north:'side',power:'12',south:'none',west:'up'}}",
			"{Name:'minecraft:redstone_wire',Properties:{east:'none',north:'side',power:'12',south:'side',west:'none'}}",
			"{Name:'minecraft:redstone_wire',Properties:{east:'none',north:'side',power:'12',south:'side',west:'side'}}",
			"{Name:'minecraft:redstone_wire',Properties:{east:'none',north:'side',power:'12',south:'side',west:'up'}}",
			"{Name:'minecraft:redstone_wire',Properties:{east:'none',north:'side',power:'12',south:'up',west:'none'}}",
			"{Name:'minecraft:redstone_wire',Properties:{east:'none',north:'side',power:'12',south:'up',west:'side'}}",
			"{Name:'minecraft:redstone_wire',Properties:{east:'none',north:'side',power:'12',south:'up',west:'up'}}",
			"{Name:'minecraft:redstone_wire',Properties:{east:'none',north:'up',power:'12',south:'none',west:'none'}}",
			"{Name:'minecraft:redstone_wire',Properties:{east:'none',north:'up',power:'12',south:'none',west:'side'}}",
			"{Name:'minecraft:redstone_wire',Properties:{east:'none',north:'up',power:'12',south:'none',west:'up'}}",
			"{Name:'minecraft:redstone_wire',Properties:{east:'none',north:'up',power:'12',south:'side',west:'none'}}",
			"{Name:'minecraft:redstone_wire',Properties:{east:'none',north:'up',power:'12',south:'side',west:'side'}}",
			"{Name:'minecraft:redstone_wire',Properties:{east:'none',north:'up',power:'12',south:'side',west:'up'}}",
			"{Name:'minecraft:redstone_wire',Properties:{east:'none',north:'up',power:'12',south:'up',west:'none'}}",
			"{Name:'minecraft:redstone_wire',Properties:{east:'none',north:'up',power:'12',south:'up',west:'side'}}",
			"{Name:'minecraft:redstone_wire',Properties:{east:'none',north:'up',power:'12',south:'up',west:'up'}}",
			"{Name:'minecraft:redstone_wire',Properties:{east:'side',north:'none',power:'12',south:'none',west:'none'}}",
			"{Name:'minecraft:redstone_wire',Properties:{east:'side',north:'none',power:'12',south:'none',west:'side'}}",
			"{Name:'minecraft:redstone_wire',Properties:{east:'side',north:'none',power:'12',south:'none',west:'up'}}",
			"{Name:'minecraft:redstone_wire',Properties:{east:'side',north:'none',power:'12',south:'side',west:'none'}}",
			"{Name:'minecraft:redstone_wire',Properties:{east:'side',north:'none',power:'12',south:'side',west:'side'}}",
			"{Name:'minecraft:redstone_wire',Properties:{east:'side',north:'none',power:'12',south:'side',west:'up'}}",
			"{Name:'minecraft:redstone_wire',Properties:{east:'side',north:'none',power:'12',south:'up',west:'none'}}",
			"{Name:'minecraft:redstone_wire',Properties:{east:'side',north:'none',power:'12',south:'up',west:'side'}}",
			"{Name:'minecraft:redstone_wire',Properties:{east:'side',north:'none',power:'12',south:'up',west:'up'}}",
			"{Name:'minecraft:redstone_wire',Properties:{east:'side',north:'side',power:'12',south:'none',west:'none'}}",
			"{Name:'minecraft:redstone_wire',Properties:{east:'side',north:'side',power:'12',south:'none',west:'side'}}",
			"{Name:'minecraft:redstone_wire',Properties:{east:'side',north:'side',power:'12',south:'none',west:'up'}}",
			"{Name:'minecraft:redstone_wire',Properties:{east:'side',north:'side',power:'12',south:'side',west:'none'}}",
			"{Name:'minecraft:redstone_wire',Properties:{east:'side',north:'side',power:'12',south:'side',west:'side'}}",
			"{Name:'minecraft:redstone_wire',Properties:{east:'side',north:'side',power:'12',south:'side',west:'up'}}",
			"{Name:'minecraft:redstone_wire',Properties:{east:'side',north:'side',power:'12',south:'up',west:'none'}}",
			"{Name:'minecraft:redstone_wire',Properties:{east:'side',north:'side',power:'12',south:'up',west:'side'}}",
			"{Name:'minecraft:redstone_wire',Properties:{east:'side',north:'side',power:'12',south:'up',west:'up'}}",
			"{Name:'minecraft:redstone_wire',Properties:{east:'side',north:'up',power:'12',south:'none',west:'none'}}",
			"{Name:'minecraft:redstone_wire',Properties:{east:'side',north:'up',power:'12',south:'none',west:'side'}}",
			"{Name:'minecraft:redstone_wire',Properties:{east:'side',north:'up',power:'12',south:'none',west:'up'}}",
			"{Name:'minecraft:redstone_wire',Properties:{east:'side',north:'up',power:'12',south:'side',west:'none'}}",
			"{Name:'minecraft:redstone_wire',Properties:{east:'side',north:'up',power:'12',south:'side',west:'side'}}",
			"{Name:'minecraft:redstone_wire',Properties:{east:'side',north:'up',power:'12',south:'side',west:'up'}}",
			"{Name:'minecraft:redstone_wire',Properties:{east:'side',north:'up',power:'12',south:'up',west:'none'}}",
			"{Name:'minecraft:redstone_wire',Properties:{east:'side',north:'up',power:'12',south:'up',west:'side'}}",
			"{Name:'minecraft:redstone_wire',Properties:{east:'side',north:'up',power:'12',south:'up',west:'up'}}",
			"{Name:'minecraft:redstone_wire',Properties:{east:'up',north:'none',power:'12',south:'none',west:'none'}}",
			"{Name:'minecraft:redstone_wire',Properties:{east:'up',north:'none',power:'12',south:'none',west:'side'}}",
			"{Name:'minecraft:redstone_wire',Properties:{east:'up',north:'none',power:'12',south:'none',west:'up'}}",
			"{Name:'minecraft:redstone_wire',Properties:{east:'up',north:'none',power:'12',south:'side',west:'none'}}",
			"{Name:'minecraft:redstone_wire',Properties:{east:'up',north:'none',power:'12',south:'side',west:'side'}}",
			"{Name:'minecraft:redstone_wire',Properties:{east:'up',north:'none',power:'12',south:'side',west:'up'}}",
			"{Name:'minecraft:redstone_wire',Properties:{east:'up',north:'none',power:'12',south:'up',west:'none'}}",
			"{Name:'minecraft:redstone_wire',Properties:{east:'up',north:'none',power:'12',south:'up',west:'side'}}",
			"{Name:'minecraft:redstone_wire',Properties:{east:'up',north:'none',power:'12',south:'up',west:'up'}}",
			"{Name:'minecraft:redstone_wire',Properties:{east:'up',north:'side',power:'12',south:'none',west:'none'}}",
			"{Name:'minecraft:redstone_wire',Properties:{east:'up',north:'side',power:'12',south:'none',west:'side'}}",
			"{Name:'minecraft:redstone_wire',Properties:{east:'up',north:'side',power:'12',south:'none',west:'up'}}",
			"{Name:'minecraft:redstone_wire',Properties:{east:'up',north:'side',power:'12',south:'side',west:'none'}}",
			"{Name:'minecraft:redstone_wire',Properties:{east:'up',north:'side',power:'12',south:'side',west:'side'}}",
			"{Name:'minecraft:redstone_wire',Properties:{east:'up',north:'side',power:'12',south:'side',west:'up'}}",
			"{Name:'minecraft:redstone_wire',Properties:{east:'up',north:'side',power:'12',south:'up',west:'none'}}",
			"{Name:'minecraft:redstone_wire',Properties:{east:'up',north:'side',power:'12',south:'up',west:'side'}}",
			"{Name:'minecraft:redstone_wire',Properties:{east:'up',north:'side',power:'12',south:'up',west:'up'}}",
			"{Name:'minecraft:redstone_wire',Properties:{east:'up',north:'up',power:'12',south:'none',west:'none'}}",
			"{Name:'minecraft:redstone_wire',Properties:{east:'up',north:'up',power:'12',south:'none',west:'side'}}",
			"{Name:'minecraft:redstone_wire',Properties:{east:'up',north:'up',power:'12',south:'none',west:'up'}}",
			"{Name:'minecraft:redstone_wire',Properties:{east:'up',north:'up',power:'12',south:'side',west:'none'}}",
			"{Name:'minecraft:redstone_wire',Properties:{east:'up',north:'up',power:'12',south:'side',west:'side'}}",
			"{Name:'minecraft:redstone_wire',Properties:{east:'up',north:'up',power:'12',south:'side',west:'up'}}",
			"{Name:'minecraft:redstone_wire',Properties:{east:'up',north:'up',power:'12',south:'up',west:'none'}}",
			"{Name:'minecraft:redstone_wire',Properties:{east:'up',north:'up',power:'12',south:'up',west:'side'}}",
			"{Name:'minecraft:redstone_wire',Properties:{east:'up',north:'up',power:'12',south:'up',west:'up'}}"
		);
		putStates(
			893,
			"{Name:'minecraft:redstone_wire',Properties:{east:'none',north:'none',power:'13',south:'none',west:'none'}}",
			"{Name:'minecraft:redstone_wire',Properties:{east:'none',north:'none',power:'13',south:'none',west:'none'}}",
			"{Name:'minecraft:redstone_wire',Properties:{east:'none',north:'none',power:'13',south:'none',west:'side'}}",
			"{Name:'minecraft:redstone_wire',Properties:{east:'none',north:'none',power:'13',south:'none',west:'up'}}",
			"{Name:'minecraft:redstone_wire',Properties:{east:'none',north:'none',power:'13',south:'side',west:'none'}}",
			"{Name:'minecraft:redstone_wire',Properties:{east:'none',north:'none',power:'13',south:'side',west:'side'}}",
			"{Name:'minecraft:redstone_wire',Properties:{east:'none',north:'none',power:'13',south:'side',west:'up'}}",
			"{Name:'minecraft:redstone_wire',Properties:{east:'none',north:'none',power:'13',south:'up',west:'none'}}",
			"{Name:'minecraft:redstone_wire',Properties:{east:'none',north:'none',power:'13',south:'up',west:'side'}}",
			"{Name:'minecraft:redstone_wire',Properties:{east:'none',north:'none',power:'13',south:'up',west:'up'}}",
			"{Name:'minecraft:redstone_wire',Properties:{east:'none',north:'side',power:'13',south:'none',west:'none'}}",
			"{Name:'minecraft:redstone_wire',Properties:{east:'none',north:'side',power:'13',south:'none',west:'side'}}",
			"{Name:'minecraft:redstone_wire',Properties:{east:'none',north:'side',power:'13',south:'none',west:'up'}}",
			"{Name:'minecraft:redstone_wire',Properties:{east:'none',north:'side',power:'13',south:'side',west:'none'}}",
			"{Name:'minecraft:redstone_wire',Properties:{east:'none',north:'side',power:'13',south:'side',west:'side'}}",
			"{Name:'minecraft:redstone_wire',Properties:{east:'none',north:'side',power:'13',south:'side',west:'up'}}",
			"{Name:'minecraft:redstone_wire',Properties:{east:'none',north:'side',power:'13',south:'up',west:'none'}}",
			"{Name:'minecraft:redstone_wire',Properties:{east:'none',north:'side',power:'13',south:'up',west:'side'}}",
			"{Name:'minecraft:redstone_wire',Properties:{east:'none',north:'side',power:'13',south:'up',west:'up'}}",
			"{Name:'minecraft:redstone_wire',Properties:{east:'none',north:'up',power:'13',south:'none',west:'none'}}",
			"{Name:'minecraft:redstone_wire',Properties:{east:'none',north:'up',power:'13',south:'none',west:'side'}}",
			"{Name:'minecraft:redstone_wire',Properties:{east:'none',north:'up',power:'13',south:'none',west:'up'}}",
			"{Name:'minecraft:redstone_wire',Properties:{east:'none',north:'up',power:'13',south:'side',west:'none'}}",
			"{Name:'minecraft:redstone_wire',Properties:{east:'none',north:'up',power:'13',south:'side',west:'side'}}",
			"{Name:'minecraft:redstone_wire',Properties:{east:'none',north:'up',power:'13',south:'side',west:'up'}}",
			"{Name:'minecraft:redstone_wire',Properties:{east:'none',north:'up',power:'13',south:'up',west:'none'}}",
			"{Name:'minecraft:redstone_wire',Properties:{east:'none',north:'up',power:'13',south:'up',west:'side'}}",
			"{Name:'minecraft:redstone_wire',Properties:{east:'none',north:'up',power:'13',south:'up',west:'up'}}",
			"{Name:'minecraft:redstone_wire',Properties:{east:'side',north:'none',power:'13',south:'none',west:'none'}}",
			"{Name:'minecraft:redstone_wire',Properties:{east:'side',north:'none',power:'13',south:'none',west:'side'}}",
			"{Name:'minecraft:redstone_wire',Properties:{east:'side',north:'none',power:'13',south:'none',west:'up'}}",
			"{Name:'minecraft:redstone_wire',Properties:{east:'side',north:'none',power:'13',south:'side',west:'none'}}",
			"{Name:'minecraft:redstone_wire',Properties:{east:'side',north:'none',power:'13',south:'side',west:'side'}}",
			"{Name:'minecraft:redstone_wire',Properties:{east:'side',north:'none',power:'13',south:'side',west:'up'}}",
			"{Name:'minecraft:redstone_wire',Properties:{east:'side',north:'none',power:'13',south:'up',west:'none'}}",
			"{Name:'minecraft:redstone_wire',Properties:{east:'side',north:'none',power:'13',south:'up',west:'side'}}",
			"{Name:'minecraft:redstone_wire',Properties:{east:'side',north:'none',power:'13',south:'up',west:'up'}}",
			"{Name:'minecraft:redstone_wire',Properties:{east:'side',north:'side',power:'13',south:'none',west:'none'}}",
			"{Name:'minecraft:redstone_wire',Properties:{east:'side',north:'side',power:'13',south:'none',west:'side'}}",
			"{Name:'minecraft:redstone_wire',Properties:{east:'side',north:'side',power:'13',south:'none',west:'up'}}",
			"{Name:'minecraft:redstone_wire',Properties:{east:'side',north:'side',power:'13',south:'side',west:'none'}}",
			"{Name:'minecraft:redstone_wire',Properties:{east:'side',north:'side',power:'13',south:'side',west:'side'}}",
			"{Name:'minecraft:redstone_wire',Properties:{east:'side',north:'side',power:'13',south:'side',west:'up'}}",
			"{Name:'minecraft:redstone_wire',Properties:{east:'side',north:'side',power:'13',south:'up',west:'none'}}",
			"{Name:'minecraft:redstone_wire',Properties:{east:'side',north:'side',power:'13',south:'up',west:'side'}}",
			"{Name:'minecraft:redstone_wire',Properties:{east:'side',north:'side',power:'13',south:'up',west:'up'}}",
			"{Name:'minecraft:redstone_wire',Properties:{east:'side',north:'up',power:'13',south:'none',west:'none'}}",
			"{Name:'minecraft:redstone_wire',Properties:{east:'side',north:'up',power:'13',south:'none',west:'side'}}",
			"{Name:'minecraft:redstone_wire',Properties:{east:'side',north:'up',power:'13',south:'none',west:'up'}}",
			"{Name:'minecraft:redstone_wire',Properties:{east:'side',north:'up',power:'13',south:'side',west:'none'}}",
			"{Name:'minecraft:redstone_wire',Properties:{east:'side',north:'up',power:'13',south:'side',west:'side'}}",
			"{Name:'minecraft:redstone_wire',Properties:{east:'side',north:'up',power:'13',south:'side',west:'up'}}",
			"{Name:'minecraft:redstone_wire',Properties:{east:'side',north:'up',power:'13',south:'up',west:'none'}}",
			"{Name:'minecraft:redstone_wire',Properties:{east:'side',north:'up',power:'13',south:'up',west:'side'}}",
			"{Name:'minecraft:redstone_wire',Properties:{east:'side',north:'up',power:'13',south:'up',west:'up'}}",
			"{Name:'minecraft:redstone_wire',Properties:{east:'up',north:'none',power:'13',south:'none',west:'none'}}",
			"{Name:'minecraft:redstone_wire',Properties:{east:'up',north:'none',power:'13',south:'none',west:'side'}}",
			"{Name:'minecraft:redstone_wire',Properties:{east:'up',north:'none',power:'13',south:'none',west:'up'}}",
			"{Name:'minecraft:redstone_wire',Properties:{east:'up',north:'none',power:'13',south:'side',west:'none'}}",
			"{Name:'minecraft:redstone_wire',Properties:{east:'up',north:'none',power:'13',south:'side',west:'side'}}",
			"{Name:'minecraft:redstone_wire',Properties:{east:'up',north:'none',power:'13',south:'side',west:'up'}}",
			"{Name:'minecraft:redstone_wire',Properties:{east:'up',north:'none',power:'13',south:'up',west:'none'}}",
			"{Name:'minecraft:redstone_wire',Properties:{east:'up',north:'none',power:'13',south:'up',west:'side'}}",
			"{Name:'minecraft:redstone_wire',Properties:{east:'up',north:'none',power:'13',south:'up',west:'up'}}",
			"{Name:'minecraft:redstone_wire',Properties:{east:'up',north:'side',power:'13',south:'none',west:'none'}}",
			"{Name:'minecraft:redstone_wire',Properties:{east:'up',north:'side',power:'13',south:'none',west:'side'}}",
			"{Name:'minecraft:redstone_wire',Properties:{east:'up',north:'side',power:'13',south:'none',west:'up'}}",
			"{Name:'minecraft:redstone_wire',Properties:{east:'up',north:'side',power:'13',south:'side',west:'none'}}",
			"{Name:'minecraft:redstone_wire',Properties:{east:'up',north:'side',power:'13',south:'side',west:'side'}}",
			"{Name:'minecraft:redstone_wire',Properties:{east:'up',north:'side',power:'13',south:'side',west:'up'}}",
			"{Name:'minecraft:redstone_wire',Properties:{east:'up',north:'side',power:'13',south:'up',west:'none'}}",
			"{Name:'minecraft:redstone_wire',Properties:{east:'up',north:'side',power:'13',south:'up',west:'side'}}",
			"{Name:'minecraft:redstone_wire',Properties:{east:'up',north:'side',power:'13',south:'up',west:'up'}}",
			"{Name:'minecraft:redstone_wire',Properties:{east:'up',north:'up',power:'13',south:'none',west:'none'}}",
			"{Name:'minecraft:redstone_wire',Properties:{east:'up',north:'up',power:'13',south:'none',west:'side'}}",
			"{Name:'minecraft:redstone_wire',Properties:{east:'up',north:'up',power:'13',south:'none',west:'up'}}",
			"{Name:'minecraft:redstone_wire',Properties:{east:'up',north:'up',power:'13',south:'side',west:'none'}}",
			"{Name:'minecraft:redstone_wire',Properties:{east:'up',north:'up',power:'13',south:'side',west:'side'}}",
			"{Name:'minecraft:redstone_wire',Properties:{east:'up',north:'up',power:'13',south:'side',west:'up'}}",
			"{Name:'minecraft:redstone_wire',Properties:{east:'up',north:'up',power:'13',south:'up',west:'none'}}",
			"{Name:'minecraft:redstone_wire',Properties:{east:'up',north:'up',power:'13',south:'up',west:'side'}}",
			"{Name:'minecraft:redstone_wire',Properties:{east:'up',north:'up',power:'13',south:'up',west:'up'}}"
		);
		putStates(
			894,
			"{Name:'minecraft:redstone_wire',Properties:{east:'none',north:'none',power:'14',south:'none',west:'none'}}",
			"{Name:'minecraft:redstone_wire',Properties:{east:'none',north:'none',power:'14',south:'none',west:'none'}}",
			"{Name:'minecraft:redstone_wire',Properties:{east:'none',north:'none',power:'14',south:'none',west:'side'}}",
			"{Name:'minecraft:redstone_wire',Properties:{east:'none',north:'none',power:'14',south:'none',west:'up'}}",
			"{Name:'minecraft:redstone_wire',Properties:{east:'none',north:'none',power:'14',south:'side',west:'none'}}",
			"{Name:'minecraft:redstone_wire',Properties:{east:'none',north:'none',power:'14',south:'side',west:'side'}}",
			"{Name:'minecraft:redstone_wire',Properties:{east:'none',north:'none',power:'14',south:'side',west:'up'}}",
			"{Name:'minecraft:redstone_wire',Properties:{east:'none',north:'none',power:'14',south:'up',west:'none'}}",
			"{Name:'minecraft:redstone_wire',Properties:{east:'none',north:'none',power:'14',south:'up',west:'side'}}",
			"{Name:'minecraft:redstone_wire',Properties:{east:'none',north:'none',power:'14',south:'up',west:'up'}}",
			"{Name:'minecraft:redstone_wire',Properties:{east:'none',north:'side',power:'14',south:'none',west:'none'}}",
			"{Name:'minecraft:redstone_wire',Properties:{east:'none',north:'side',power:'14',south:'none',west:'side'}}",
			"{Name:'minecraft:redstone_wire',Properties:{east:'none',north:'side',power:'14',south:'none',west:'up'}}",
			"{Name:'minecraft:redstone_wire',Properties:{east:'none',north:'side',power:'14',south:'side',west:'none'}}",
			"{Name:'minecraft:redstone_wire',Properties:{east:'none',north:'side',power:'14',south:'side',west:'side'}}",
			"{Name:'minecraft:redstone_wire',Properties:{east:'none',north:'side',power:'14',south:'side',west:'up'}}",
			"{Name:'minecraft:redstone_wire',Properties:{east:'none',north:'side',power:'14',south:'up',west:'none'}}",
			"{Name:'minecraft:redstone_wire',Properties:{east:'none',north:'side',power:'14',south:'up',west:'side'}}",
			"{Name:'minecraft:redstone_wire',Properties:{east:'none',north:'side',power:'14',south:'up',west:'up'}}",
			"{Name:'minecraft:redstone_wire',Properties:{east:'none',north:'up',power:'14',south:'none',west:'none'}}",
			"{Name:'minecraft:redstone_wire',Properties:{east:'none',north:'up',power:'14',south:'none',west:'side'}}",
			"{Name:'minecraft:redstone_wire',Properties:{east:'none',north:'up',power:'14',south:'none',west:'up'}}",
			"{Name:'minecraft:redstone_wire',Properties:{east:'none',north:'up',power:'14',south:'side',west:'none'}}",
			"{Name:'minecraft:redstone_wire',Properties:{east:'none',north:'up',power:'14',south:'side',west:'side'}}",
			"{Name:'minecraft:redstone_wire',Properties:{east:'none',north:'up',power:'14',south:'side',west:'up'}}",
			"{Name:'minecraft:redstone_wire',Properties:{east:'none',north:'up',power:'14',south:'up',west:'none'}}",
			"{Name:'minecraft:redstone_wire',Properties:{east:'none',north:'up',power:'14',south:'up',west:'side'}}",
			"{Name:'minecraft:redstone_wire',Properties:{east:'none',north:'up',power:'14',south:'up',west:'up'}}",
			"{Name:'minecraft:redstone_wire',Properties:{east:'side',north:'none',power:'14',south:'none',west:'none'}}",
			"{Name:'minecraft:redstone_wire',Properties:{east:'side',north:'none',power:'14',south:'none',west:'side'}}",
			"{Name:'minecraft:redstone_wire',Properties:{east:'side',north:'none',power:'14',south:'none',west:'up'}}",
			"{Name:'minecraft:redstone_wire',Properties:{east:'side',north:'none',power:'14',south:'side',west:'none'}}",
			"{Name:'minecraft:redstone_wire',Properties:{east:'side',north:'none',power:'14',south:'side',west:'side'}}",
			"{Name:'minecraft:redstone_wire',Properties:{east:'side',north:'none',power:'14',south:'side',west:'up'}}",
			"{Name:'minecraft:redstone_wire',Properties:{east:'side',north:'none',power:'14',south:'up',west:'none'}}",
			"{Name:'minecraft:redstone_wire',Properties:{east:'side',north:'none',power:'14',south:'up',west:'side'}}",
			"{Name:'minecraft:redstone_wire',Properties:{east:'side',north:'none',power:'14',south:'up',west:'up'}}",
			"{Name:'minecraft:redstone_wire',Properties:{east:'side',north:'side',power:'14',south:'none',west:'none'}}",
			"{Name:'minecraft:redstone_wire',Properties:{east:'side',north:'side',power:'14',south:'none',west:'side'}}",
			"{Name:'minecraft:redstone_wire',Properties:{east:'side',north:'side',power:'14',south:'none',west:'up'}}",
			"{Name:'minecraft:redstone_wire',Properties:{east:'side',north:'side',power:'14',south:'side',west:'none'}}",
			"{Name:'minecraft:redstone_wire',Properties:{east:'side',north:'side',power:'14',south:'side',west:'side'}}",
			"{Name:'minecraft:redstone_wire',Properties:{east:'side',north:'side',power:'14',south:'side',west:'up'}}",
			"{Name:'minecraft:redstone_wire',Properties:{east:'side',north:'side',power:'14',south:'up',west:'none'}}",
			"{Name:'minecraft:redstone_wire',Properties:{east:'side',north:'side',power:'14',south:'up',west:'side'}}",
			"{Name:'minecraft:redstone_wire',Properties:{east:'side',north:'side',power:'14',south:'up',west:'up'}}",
			"{Name:'minecraft:redstone_wire',Properties:{east:'side',north:'up',power:'14',south:'none',west:'none'}}",
			"{Name:'minecraft:redstone_wire',Properties:{east:'side',north:'up',power:'14',south:'none',west:'side'}}",
			"{Name:'minecraft:redstone_wire',Properties:{east:'side',north:'up',power:'14',south:'none',west:'up'}}",
			"{Name:'minecraft:redstone_wire',Properties:{east:'side',north:'up',power:'14',south:'side',west:'none'}}",
			"{Name:'minecraft:redstone_wire',Properties:{east:'side',north:'up',power:'14',south:'side',west:'side'}}",
			"{Name:'minecraft:redstone_wire',Properties:{east:'side',north:'up',power:'14',south:'side',west:'up'}}",
			"{Name:'minecraft:redstone_wire',Properties:{east:'side',north:'up',power:'14',south:'up',west:'none'}}",
			"{Name:'minecraft:redstone_wire',Properties:{east:'side',north:'up',power:'14',south:'up',west:'side'}}",
			"{Name:'minecraft:redstone_wire',Properties:{east:'side',north:'up',power:'14',south:'up',west:'up'}}",
			"{Name:'minecraft:redstone_wire',Properties:{east:'up',north:'none',power:'14',south:'none',west:'none'}}",
			"{Name:'minecraft:redstone_wire',Properties:{east:'up',north:'none',power:'14',south:'none',west:'side'}}",
			"{Name:'minecraft:redstone_wire',Properties:{east:'up',north:'none',power:'14',south:'none',west:'up'}}",
			"{Name:'minecraft:redstone_wire',Properties:{east:'up',north:'none',power:'14',south:'side',west:'none'}}",
			"{Name:'minecraft:redstone_wire',Properties:{east:'up',north:'none',power:'14',south:'side',west:'side'}}",
			"{Name:'minecraft:redstone_wire',Properties:{east:'up',north:'none',power:'14',south:'side',west:'up'}}",
			"{Name:'minecraft:redstone_wire',Properties:{east:'up',north:'none',power:'14',south:'up',west:'none'}}",
			"{Name:'minecraft:redstone_wire',Properties:{east:'up',north:'none',power:'14',south:'up',west:'side'}}",
			"{Name:'minecraft:redstone_wire',Properties:{east:'up',north:'none',power:'14',south:'up',west:'up'}}",
			"{Name:'minecraft:redstone_wire',Properties:{east:'up',north:'side',power:'14',south:'none',west:'none'}}",
			"{Name:'minecraft:redstone_wire',Properties:{east:'up',north:'side',power:'14',south:'none',west:'side'}}",
			"{Name:'minecraft:redstone_wire',Properties:{east:'up',north:'side',power:'14',south:'none',west:'up'}}",
			"{Name:'minecraft:redstone_wire',Properties:{east:'up',north:'side',power:'14',south:'side',west:'none'}}",
			"{Name:'minecraft:redstone_wire',Properties:{east:'up',north:'side',power:'14',south:'side',west:'side'}}",
			"{Name:'minecraft:redstone_wire',Properties:{east:'up',north:'side',power:'14',south:'side',west:'up'}}",
			"{Name:'minecraft:redstone_wire',Properties:{east:'up',north:'side',power:'14',south:'up',west:'none'}}",
			"{Name:'minecraft:redstone_wire',Properties:{east:'up',north:'side',power:'14',south:'up',west:'side'}}",
			"{Name:'minecraft:redstone_wire',Properties:{east:'up',north:'side',power:'14',south:'up',west:'up'}}",
			"{Name:'minecraft:redstone_wire',Properties:{east:'up',north:'up',power:'14',south:'none',west:'none'}}",
			"{Name:'minecraft:redstone_wire',Properties:{east:'up',north:'up',power:'14',south:'none',west:'side'}}",
			"{Name:'minecraft:redstone_wire',Properties:{east:'up',north:'up',power:'14',south:'none',west:'up'}}",
			"{Name:'minecraft:redstone_wire',Properties:{east:'up',north:'up',power:'14',south:'side',west:'none'}}",
			"{Name:'minecraft:redstone_wire',Properties:{east:'up',north:'up',power:'14',south:'side',west:'side'}}",
			"{Name:'minecraft:redstone_wire',Properties:{east:'up',north:'up',power:'14',south:'side',west:'up'}}",
			"{Name:'minecraft:redstone_wire',Properties:{east:'up',north:'up',power:'14',south:'up',west:'none'}}",
			"{Name:'minecraft:redstone_wire',Properties:{east:'up',north:'up',power:'14',south:'up',west:'side'}}",
			"{Name:'minecraft:redstone_wire',Properties:{east:'up',north:'up',power:'14',south:'up',west:'up'}}"
		);
		putStates(
			895,
			"{Name:'minecraft:redstone_wire',Properties:{east:'none',north:'none',power:'15',south:'none',west:'none'}}",
			"{Name:'minecraft:redstone_wire',Properties:{east:'none',north:'none',power:'15',south:'none',west:'none'}}",
			"{Name:'minecraft:redstone_wire',Properties:{east:'none',north:'none',power:'15',south:'none',west:'side'}}",
			"{Name:'minecraft:redstone_wire',Properties:{east:'none',north:'none',power:'15',south:'none',west:'up'}}",
			"{Name:'minecraft:redstone_wire',Properties:{east:'none',north:'none',power:'15',south:'side',west:'none'}}",
			"{Name:'minecraft:redstone_wire',Properties:{east:'none',north:'none',power:'15',south:'side',west:'side'}}",
			"{Name:'minecraft:redstone_wire',Properties:{east:'none',north:'none',power:'15',south:'side',west:'up'}}",
			"{Name:'minecraft:redstone_wire',Properties:{east:'none',north:'none',power:'15',south:'up',west:'none'}}",
			"{Name:'minecraft:redstone_wire',Properties:{east:'none',north:'none',power:'15',south:'up',west:'side'}}",
			"{Name:'minecraft:redstone_wire',Properties:{east:'none',north:'none',power:'15',south:'up',west:'up'}}",
			"{Name:'minecraft:redstone_wire',Properties:{east:'none',north:'side',power:'15',south:'none',west:'none'}}",
			"{Name:'minecraft:redstone_wire',Properties:{east:'none',north:'side',power:'15',south:'none',west:'side'}}",
			"{Name:'minecraft:redstone_wire',Properties:{east:'none',north:'side',power:'15',south:'none',west:'up'}}",
			"{Name:'minecraft:redstone_wire',Properties:{east:'none',north:'side',power:'15',south:'side',west:'none'}}",
			"{Name:'minecraft:redstone_wire',Properties:{east:'none',north:'side',power:'15',south:'side',west:'side'}}",
			"{Name:'minecraft:redstone_wire',Properties:{east:'none',north:'side',power:'15',south:'side',west:'up'}}",
			"{Name:'minecraft:redstone_wire',Properties:{east:'none',north:'side',power:'15',south:'up',west:'none'}}",
			"{Name:'minecraft:redstone_wire',Properties:{east:'none',north:'side',power:'15',south:'up',west:'side'}}",
			"{Name:'minecraft:redstone_wire',Properties:{east:'none',north:'side',power:'15',south:'up',west:'up'}}",
			"{Name:'minecraft:redstone_wire',Properties:{east:'none',north:'up',power:'15',south:'none',west:'none'}}",
			"{Name:'minecraft:redstone_wire',Properties:{east:'none',north:'up',power:'15',south:'none',west:'side'}}",
			"{Name:'minecraft:redstone_wire',Properties:{east:'none',north:'up',power:'15',south:'none',west:'up'}}",
			"{Name:'minecraft:redstone_wire',Properties:{east:'none',north:'up',power:'15',south:'side',west:'none'}}",
			"{Name:'minecraft:redstone_wire',Properties:{east:'none',north:'up',power:'15',south:'side',west:'side'}}",
			"{Name:'minecraft:redstone_wire',Properties:{east:'none',north:'up',power:'15',south:'side',west:'up'}}",
			"{Name:'minecraft:redstone_wire',Properties:{east:'none',north:'up',power:'15',south:'up',west:'none'}}",
			"{Name:'minecraft:redstone_wire',Properties:{east:'none',north:'up',power:'15',south:'up',west:'side'}}",
			"{Name:'minecraft:redstone_wire',Properties:{east:'none',north:'up',power:'15',south:'up',west:'up'}}",
			"{Name:'minecraft:redstone_wire',Properties:{east:'side',north:'none',power:'15',south:'none',west:'none'}}",
			"{Name:'minecraft:redstone_wire',Properties:{east:'side',north:'none',power:'15',south:'none',west:'side'}}",
			"{Name:'minecraft:redstone_wire',Properties:{east:'side',north:'none',power:'15',south:'none',west:'up'}}",
			"{Name:'minecraft:redstone_wire',Properties:{east:'side',north:'none',power:'15',south:'side',west:'none'}}",
			"{Name:'minecraft:redstone_wire',Properties:{east:'side',north:'none',power:'15',south:'side',west:'side'}}",
			"{Name:'minecraft:redstone_wire',Properties:{east:'side',north:'none',power:'15',south:'side',west:'up'}}",
			"{Name:'minecraft:redstone_wire',Properties:{east:'side',north:'none',power:'15',south:'up',west:'none'}}",
			"{Name:'minecraft:redstone_wire',Properties:{east:'side',north:'none',power:'15',south:'up',west:'side'}}",
			"{Name:'minecraft:redstone_wire',Properties:{east:'side',north:'none',power:'15',south:'up',west:'up'}}",
			"{Name:'minecraft:redstone_wire',Properties:{east:'side',north:'side',power:'15',south:'none',west:'none'}}",
			"{Name:'minecraft:redstone_wire',Properties:{east:'side',north:'side',power:'15',south:'none',west:'side'}}",
			"{Name:'minecraft:redstone_wire',Properties:{east:'side',north:'side',power:'15',south:'none',west:'up'}}",
			"{Name:'minecraft:redstone_wire',Properties:{east:'side',north:'side',power:'15',south:'side',west:'none'}}",
			"{Name:'minecraft:redstone_wire',Properties:{east:'side',north:'side',power:'15',south:'side',west:'side'}}",
			"{Name:'minecraft:redstone_wire',Properties:{east:'side',north:'side',power:'15',south:'side',west:'up'}}",
			"{Name:'minecraft:redstone_wire',Properties:{east:'side',north:'side',power:'15',south:'up',west:'none'}}",
			"{Name:'minecraft:redstone_wire',Properties:{east:'side',north:'side',power:'15',south:'up',west:'side'}}",
			"{Name:'minecraft:redstone_wire',Properties:{east:'side',north:'side',power:'15',south:'up',west:'up'}}",
			"{Name:'minecraft:redstone_wire',Properties:{east:'side',north:'up',power:'15',south:'none',west:'none'}}",
			"{Name:'minecraft:redstone_wire',Properties:{east:'side',north:'up',power:'15',south:'none',west:'side'}}",
			"{Name:'minecraft:redstone_wire',Properties:{east:'side',north:'up',power:'15',south:'none',west:'up'}}",
			"{Name:'minecraft:redstone_wire',Properties:{east:'side',north:'up',power:'15',south:'side',west:'none'}}",
			"{Name:'minecraft:redstone_wire',Properties:{east:'side',north:'up',power:'15',south:'side',west:'side'}}",
			"{Name:'minecraft:redstone_wire',Properties:{east:'side',north:'up',power:'15',south:'side',west:'up'}}",
			"{Name:'minecraft:redstone_wire',Properties:{east:'side',north:'up',power:'15',south:'up',west:'none'}}",
			"{Name:'minecraft:redstone_wire',Properties:{east:'side',north:'up',power:'15',south:'up',west:'side'}}",
			"{Name:'minecraft:redstone_wire',Properties:{east:'side',north:'up',power:'15',south:'up',west:'up'}}",
			"{Name:'minecraft:redstone_wire',Properties:{east:'up',north:'none',power:'15',south:'none',west:'none'}}",
			"{Name:'minecraft:redstone_wire',Properties:{east:'up',north:'none',power:'15',south:'none',west:'side'}}",
			"{Name:'minecraft:redstone_wire',Properties:{east:'up',north:'none',power:'15',south:'none',west:'up'}}",
			"{Name:'minecraft:redstone_wire',Properties:{east:'up',north:'none',power:'15',south:'side',west:'none'}}",
			"{Name:'minecraft:redstone_wire',Properties:{east:'up',north:'none',power:'15',south:'side',west:'side'}}",
			"{Name:'minecraft:redstone_wire',Properties:{east:'up',north:'none',power:'15',south:'side',west:'up'}}",
			"{Name:'minecraft:redstone_wire',Properties:{east:'up',north:'none',power:'15',south:'up',west:'none'}}",
			"{Name:'minecraft:redstone_wire',Properties:{east:'up',north:'none',power:'15',south:'up',west:'side'}}",
			"{Name:'minecraft:redstone_wire',Properties:{east:'up',north:'none',power:'15',south:'up',west:'up'}}",
			"{Name:'minecraft:redstone_wire',Properties:{east:'up',north:'side',power:'15',south:'none',west:'none'}}",
			"{Name:'minecraft:redstone_wire',Properties:{east:'up',north:'side',power:'15',south:'none',west:'side'}}",
			"{Name:'minecraft:redstone_wire',Properties:{east:'up',north:'side',power:'15',south:'none',west:'up'}}",
			"{Name:'minecraft:redstone_wire',Properties:{east:'up',north:'side',power:'15',south:'side',west:'none'}}",
			"{Name:'minecraft:redstone_wire',Properties:{east:'up',north:'side',power:'15',south:'side',west:'side'}}",
			"{Name:'minecraft:redstone_wire',Properties:{east:'up',north:'side',power:'15',south:'side',west:'up'}}",
			"{Name:'minecraft:redstone_wire',Properties:{east:'up',north:'side',power:'15',south:'up',west:'none'}}",
			"{Name:'minecraft:redstone_wire',Properties:{east:'up',north:'side',power:'15',south:'up',west:'side'}}",
			"{Name:'minecraft:redstone_wire',Properties:{east:'up',north:'side',power:'15',south:'up',west:'up'}}",
			"{Name:'minecraft:redstone_wire',Properties:{east:'up',north:'up',power:'15',south:'none',west:'none'}}",
			"{Name:'minecraft:redstone_wire',Properties:{east:'up',north:'up',power:'15',south:'none',west:'side'}}",
			"{Name:'minecraft:redstone_wire',Properties:{east:'up',north:'up',power:'15',south:'none',west:'up'}}",
			"{Name:'minecraft:redstone_wire',Properties:{east:'up',north:'up',power:'15',south:'side',west:'none'}}",
			"{Name:'minecraft:redstone_wire',Properties:{east:'up',north:'up',power:'15',south:'side',west:'side'}}",
			"{Name:'minecraft:redstone_wire',Properties:{east:'up',north:'up',power:'15',south:'side',west:'up'}}",
			"{Name:'minecraft:redstone_wire',Properties:{east:'up',north:'up',power:'15',south:'up',west:'none'}}",
			"{Name:'minecraft:redstone_wire',Properties:{east:'up',north:'up',power:'15',south:'up',west:'side'}}",
			"{Name:'minecraft:redstone_wire',Properties:{east:'up',north:'up',power:'15',south:'up',west:'up'}}"
		);
		putStates(896, "{Name:'minecraft:diamond_ore'}", "{Name:'minecraft:diamond_ore'}");
		putStates(912, "{Name:'minecraft:diamond_block'}", "{Name:'minecraft:diamond_block'}");
		putStates(928, "{Name:'minecraft:crafting_table'}", "{Name:'minecraft:crafting_table'}");
		putStates(944, "{Name:'minecraft:wheat',Properties:{age:'0'}}", "{Name:'minecraft:wheat',Properties:{age:'0'}}");
		putStates(945, "{Name:'minecraft:wheat',Properties:{age:'1'}}", "{Name:'minecraft:wheat',Properties:{age:'1'}}");
		putStates(946, "{Name:'minecraft:wheat',Properties:{age:'2'}}", "{Name:'minecraft:wheat',Properties:{age:'2'}}");
		putStates(947, "{Name:'minecraft:wheat',Properties:{age:'3'}}", "{Name:'minecraft:wheat',Properties:{age:'3'}}");
		putStates(948, "{Name:'minecraft:wheat',Properties:{age:'4'}}", "{Name:'minecraft:wheat',Properties:{age:'4'}}");
		putStates(949, "{Name:'minecraft:wheat',Properties:{age:'5'}}", "{Name:'minecraft:wheat',Properties:{age:'5'}}");
		putStates(950, "{Name:'minecraft:wheat',Properties:{age:'6'}}", "{Name:'minecraft:wheat',Properties:{age:'6'}}");
		putStates(951, "{Name:'minecraft:wheat',Properties:{age:'7'}}", "{Name:'minecraft:wheat',Properties:{age:'7'}}");
		putStates(960, "{Name:'minecraft:farmland',Properties:{moisture:'0'}}", "{Name:'minecraft:farmland',Properties:{moisture:'0'}}");
		putStates(961, "{Name:'minecraft:farmland',Properties:{moisture:'1'}}", "{Name:'minecraft:farmland',Properties:{moisture:'1'}}");
		putStates(962, "{Name:'minecraft:farmland',Properties:{moisture:'2'}}", "{Name:'minecraft:farmland',Properties:{moisture:'2'}}");
		putStates(963, "{Name:'minecraft:farmland',Properties:{moisture:'3'}}", "{Name:'minecraft:farmland',Properties:{moisture:'3'}}");
		putStates(964, "{Name:'minecraft:farmland',Properties:{moisture:'4'}}", "{Name:'minecraft:farmland',Properties:{moisture:'4'}}");
		putStates(965, "{Name:'minecraft:farmland',Properties:{moisture:'5'}}", "{Name:'minecraft:farmland',Properties:{moisture:'5'}}");
		putStates(966, "{Name:'minecraft:farmland',Properties:{moisture:'6'}}", "{Name:'minecraft:farmland',Properties:{moisture:'6'}}");
		putStates(967, "{Name:'minecraft:farmland',Properties:{moisture:'7'}}", "{Name:'minecraft:farmland',Properties:{moisture:'7'}}");
		putStates(978, "{Name:'minecraft:furnace',Properties:{facing:'north',lit:'false'}}", "{Name:'minecraft:furnace',Properties:{facing:'north'}}");
		putStates(979, "{Name:'minecraft:furnace',Properties:{facing:'south',lit:'false'}}", "{Name:'minecraft:furnace',Properties:{facing:'south'}}");
		putStates(980, "{Name:'minecraft:furnace',Properties:{facing:'west',lit:'false'}}", "{Name:'minecraft:furnace',Properties:{facing:'west'}}");
		putStates(981, "{Name:'minecraft:furnace',Properties:{facing:'east',lit:'false'}}", "{Name:'minecraft:furnace',Properties:{facing:'east'}}");
		putStates(994, "{Name:'minecraft:furnace',Properties:{facing:'north',lit:'true'}}", "{Name:'minecraft:lit_furnace',Properties:{facing:'north'}}");
		putStates(995, "{Name:'minecraft:furnace',Properties:{facing:'south',lit:'true'}}", "{Name:'minecraft:lit_furnace',Properties:{facing:'south'}}");
		putStates(996, "{Name:'minecraft:furnace',Properties:{facing:'west',lit:'true'}}", "{Name:'minecraft:lit_furnace',Properties:{facing:'west'}}");
		putStates(997, "{Name:'minecraft:furnace',Properties:{facing:'east',lit:'true'}}", "{Name:'minecraft:lit_furnace',Properties:{facing:'east'}}");
		putStates(1008, "{Name:'minecraft:sign',Properties:{rotation:'0'}}", "{Name:'minecraft:standing_sign',Properties:{rotation:'0'}}");
		putStates(1009, "{Name:'minecraft:sign',Properties:{rotation:'1'}}", "{Name:'minecraft:standing_sign',Properties:{rotation:'1'}}");
		putStates(1010, "{Name:'minecraft:sign',Properties:{rotation:'2'}}", "{Name:'minecraft:standing_sign',Properties:{rotation:'2'}}");
		putStates(1011, "{Name:'minecraft:sign',Properties:{rotation:'3'}}", "{Name:'minecraft:standing_sign',Properties:{rotation:'3'}}");
		putStates(1012, "{Name:'minecraft:sign',Properties:{rotation:'4'}}", "{Name:'minecraft:standing_sign',Properties:{rotation:'4'}}");
		putStates(1013, "{Name:'minecraft:sign',Properties:{rotation:'5'}}", "{Name:'minecraft:standing_sign',Properties:{rotation:'5'}}");
		putStates(1014, "{Name:'minecraft:sign',Properties:{rotation:'6'}}", "{Name:'minecraft:standing_sign',Properties:{rotation:'6'}}");
		putStates(1015, "{Name:'minecraft:sign',Properties:{rotation:'7'}}", "{Name:'minecraft:standing_sign',Properties:{rotation:'7'}}");
		putStates(1016, "{Name:'minecraft:sign',Properties:{rotation:'8'}}", "{Name:'minecraft:standing_sign',Properties:{rotation:'8'}}");
		putStates(1017, "{Name:'minecraft:sign',Properties:{rotation:'9'}}", "{Name:'minecraft:standing_sign',Properties:{rotation:'9'}}");
		putStates(1018, "{Name:'minecraft:sign',Properties:{rotation:'10'}}", "{Name:'minecraft:standing_sign',Properties:{rotation:'10'}}");
		putStates(1019, "{Name:'minecraft:sign',Properties:{rotation:'11'}}", "{Name:'minecraft:standing_sign',Properties:{rotation:'11'}}");
		putStates(1020, "{Name:'minecraft:sign',Properties:{rotation:'12'}}", "{Name:'minecraft:standing_sign',Properties:{rotation:'12'}}");
		putStates(1021, "{Name:'minecraft:sign',Properties:{rotation:'13'}}", "{Name:'minecraft:standing_sign',Properties:{rotation:'13'}}");
		putStates(1022, "{Name:'minecraft:sign',Properties:{rotation:'14'}}", "{Name:'minecraft:standing_sign',Properties:{rotation:'14'}}");
		putStates(1023, "{Name:'minecraft:sign',Properties:{rotation:'15'}}", "{Name:'minecraft:standing_sign',Properties:{rotation:'15'}}");
	}

	/**
	 * Adds states to flatten from the blocks which had numeric IDs between 64 and 79 before 1.13.
	 */
	static void putStatesFromBlocks64To79() {
		putStates(
			1024,
			"{Name:'minecraft:oak_door',Properties:{facing:'east',half:'lower',hinge:'right',open:'false',powered:'false'}}",
			"{Name:'minecraft:wooden_door',Properties:{facing:'east',half:'lower',hinge:'left',open:'false',powered:'false'}}",
			"{Name:'minecraft:wooden_door',Properties:{facing:'east',half:'lower',hinge:'left',open:'false',powered:'true'}}",
			"{Name:'minecraft:wooden_door',Properties:{facing:'east',half:'lower',hinge:'right',open:'false',powered:'false'}}",
			"{Name:'minecraft:wooden_door',Properties:{facing:'east',half:'lower',hinge:'right',open:'false',powered:'true'}}"
		);
		putStates(
			1025,
			"{Name:'minecraft:oak_door',Properties:{facing:'south',half:'lower',hinge:'right',open:'false',powered:'false'}}",
			"{Name:'minecraft:wooden_door',Properties:{facing:'south',half:'lower',hinge:'left',open:'false',powered:'false'}}",
			"{Name:'minecraft:wooden_door',Properties:{facing:'south',half:'lower',hinge:'left',open:'false',powered:'true'}}",
			"{Name:'minecraft:wooden_door',Properties:{facing:'south',half:'lower',hinge:'right',open:'false',powered:'false'}}",
			"{Name:'minecraft:wooden_door',Properties:{facing:'south',half:'lower',hinge:'right',open:'false',powered:'true'}}"
		);
		putStates(
			1026,
			"{Name:'minecraft:oak_door',Properties:{facing:'west',half:'lower',hinge:'right',open:'false',powered:'false'}}",
			"{Name:'minecraft:wooden_door',Properties:{facing:'west',half:'lower',hinge:'left',open:'false',powered:'false'}}",
			"{Name:'minecraft:wooden_door',Properties:{facing:'west',half:'lower',hinge:'left',open:'false',powered:'true'}}",
			"{Name:'minecraft:wooden_door',Properties:{facing:'west',half:'lower',hinge:'right',open:'false',powered:'false'}}",
			"{Name:'minecraft:wooden_door',Properties:{facing:'west',half:'lower',hinge:'right',open:'false',powered:'true'}}"
		);
		putStates(
			1027,
			"{Name:'minecraft:oak_door',Properties:{facing:'north',half:'lower',hinge:'right',open:'false',powered:'false'}}",
			"{Name:'minecraft:wooden_door',Properties:{facing:'north',half:'lower',hinge:'left',open:'false',powered:'false'}}",
			"{Name:'minecraft:wooden_door',Properties:{facing:'north',half:'lower',hinge:'left',open:'false',powered:'true'}}",
			"{Name:'minecraft:wooden_door',Properties:{facing:'north',half:'lower',hinge:'right',open:'false',powered:'false'}}",
			"{Name:'minecraft:wooden_door',Properties:{facing:'north',half:'lower',hinge:'right',open:'false',powered:'true'}}"
		);
		putStates(
			1028,
			"{Name:'minecraft:oak_door',Properties:{facing:'east',half:'lower',hinge:'right',open:'true',powered:'false'}}",
			"{Name:'minecraft:wooden_door',Properties:{facing:'east',half:'lower',hinge:'left',open:'true',powered:'false'}}",
			"{Name:'minecraft:wooden_door',Properties:{facing:'east',half:'lower',hinge:'left',open:'true',powered:'true'}}",
			"{Name:'minecraft:wooden_door',Properties:{facing:'east',half:'lower',hinge:'right',open:'true',powered:'false'}}",
			"{Name:'minecraft:wooden_door',Properties:{facing:'east',half:'lower',hinge:'right',open:'true',powered:'true'}}"
		);
		putStates(
			1029,
			"{Name:'minecraft:oak_door',Properties:{facing:'south',half:'lower',hinge:'right',open:'true',powered:'false'}}",
			"{Name:'minecraft:wooden_door',Properties:{facing:'south',half:'lower',hinge:'left',open:'true',powered:'false'}}",
			"{Name:'minecraft:wooden_door',Properties:{facing:'south',half:'lower',hinge:'left',open:'true',powered:'true'}}",
			"{Name:'minecraft:wooden_door',Properties:{facing:'south',half:'lower',hinge:'right',open:'true',powered:'false'}}",
			"{Name:'minecraft:wooden_door',Properties:{facing:'south',half:'lower',hinge:'right',open:'true',powered:'true'}}"
		);
		putStates(
			1030,
			"{Name:'minecraft:oak_door',Properties:{facing:'west',half:'lower',hinge:'right',open:'true',powered:'false'}}",
			"{Name:'minecraft:wooden_door',Properties:{facing:'west',half:'lower',hinge:'left',open:'true',powered:'false'}}",
			"{Name:'minecraft:wooden_door',Properties:{facing:'west',half:'lower',hinge:'left',open:'true',powered:'true'}}",
			"{Name:'minecraft:wooden_door',Properties:{facing:'west',half:'lower',hinge:'right',open:'true',powered:'false'}}",
			"{Name:'minecraft:wooden_door',Properties:{facing:'west',half:'lower',hinge:'right',open:'true',powered:'true'}}"
		);
		putStates(
			1031,
			"{Name:'minecraft:oak_door',Properties:{facing:'north',half:'lower',hinge:'right',open:'true',powered:'false'}}",
			"{Name:'minecraft:wooden_door',Properties:{facing:'north',half:'lower',hinge:'left',open:'true',powered:'false'}}",
			"{Name:'minecraft:wooden_door',Properties:{facing:'north',half:'lower',hinge:'left',open:'true',powered:'true'}}",
			"{Name:'minecraft:wooden_door',Properties:{facing:'north',half:'lower',hinge:'right',open:'true',powered:'false'}}",
			"{Name:'minecraft:wooden_door',Properties:{facing:'north',half:'lower',hinge:'right',open:'true',powered:'true'}}"
		);
		putStates(
			1032,
			"{Name:'minecraft:oak_door',Properties:{facing:'east',half:'upper',hinge:'left',open:'false',powered:'false'}}",
			"{Name:'minecraft:wooden_door',Properties:{facing:'east',half:'upper',hinge:'left',open:'false',powered:'false'}}",
			"{Name:'minecraft:wooden_door',Properties:{facing:'east',half:'upper',hinge:'left',open:'true',powered:'false'}}",
			"{Name:'minecraft:wooden_door',Properties:{facing:'north',half:'upper',hinge:'left',open:'false',powered:'false'}}",
			"{Name:'minecraft:wooden_door',Properties:{facing:'north',half:'upper',hinge:'left',open:'true',powered:'false'}}",
			"{Name:'minecraft:wooden_door',Properties:{facing:'south',half:'upper',hinge:'left',open:'false',powered:'false'}}",
			"{Name:'minecraft:wooden_door',Properties:{facing:'south',half:'upper',hinge:'left',open:'true',powered:'false'}}",
			"{Name:'minecraft:wooden_door',Properties:{facing:'west',half:'upper',hinge:'left',open:'false',powered:'false'}}",
			"{Name:'minecraft:wooden_door',Properties:{facing:'west',half:'upper',hinge:'left',open:'true',powered:'false'}}"
		);
		putStates(
			1033,
			"{Name:'minecraft:oak_door',Properties:{facing:'east',half:'upper',hinge:'right',open:'false',powered:'false'}}",
			"{Name:'minecraft:wooden_door',Properties:{facing:'east',half:'upper',hinge:'right',open:'false',powered:'false'}}",
			"{Name:'minecraft:wooden_door',Properties:{facing:'east',half:'upper',hinge:'right',open:'true',powered:'false'}}",
			"{Name:'minecraft:wooden_door',Properties:{facing:'north',half:'upper',hinge:'right',open:'false',powered:'false'}}",
			"{Name:'minecraft:wooden_door',Properties:{facing:'north',half:'upper',hinge:'right',open:'true',powered:'false'}}",
			"{Name:'minecraft:wooden_door',Properties:{facing:'south',half:'upper',hinge:'right',open:'false',powered:'false'}}",
			"{Name:'minecraft:wooden_door',Properties:{facing:'south',half:'upper',hinge:'right',open:'true',powered:'false'}}",
			"{Name:'minecraft:wooden_door',Properties:{facing:'west',half:'upper',hinge:'right',open:'false',powered:'false'}}",
			"{Name:'minecraft:wooden_door',Properties:{facing:'west',half:'upper',hinge:'right',open:'true',powered:'false'}}"
		);
		putStates(
			1034,
			"{Name:'minecraft:oak_door',Properties:{facing:'east',half:'upper',hinge:'left',open:'false',powered:'true'}}",
			"{Name:'minecraft:wooden_door',Properties:{facing:'east',half:'upper',hinge:'left',open:'false',powered:'true'}}",
			"{Name:'minecraft:wooden_door',Properties:{facing:'east',half:'upper',hinge:'left',open:'true',powered:'true'}}",
			"{Name:'minecraft:wooden_door',Properties:{facing:'north',half:'upper',hinge:'left',open:'false',powered:'true'}}",
			"{Name:'minecraft:wooden_door',Properties:{facing:'north',half:'upper',hinge:'left',open:'true',powered:'true'}}",
			"{Name:'minecraft:wooden_door',Properties:{facing:'south',half:'upper',hinge:'left',open:'false',powered:'true'}}",
			"{Name:'minecraft:wooden_door',Properties:{facing:'south',half:'upper',hinge:'left',open:'true',powered:'true'}}",
			"{Name:'minecraft:wooden_door',Properties:{facing:'west',half:'upper',hinge:'left',open:'false',powered:'true'}}",
			"{Name:'minecraft:wooden_door',Properties:{facing:'west',half:'upper',hinge:'left',open:'true',powered:'true'}}"
		);
		putStates(
			1035,
			"{Name:'minecraft:oak_door',Properties:{facing:'east',half:'upper',hinge:'right',open:'false',powered:'true'}}",
			"{Name:'minecraft:wooden_door',Properties:{facing:'east',half:'upper',hinge:'right',open:'false',powered:'true'}}",
			"{Name:'minecraft:wooden_door',Properties:{facing:'east',half:'upper',hinge:'right',open:'true',powered:'true'}}",
			"{Name:'minecraft:wooden_door',Properties:{facing:'north',half:'upper',hinge:'right',open:'false',powered:'true'}}",
			"{Name:'minecraft:wooden_door',Properties:{facing:'north',half:'upper',hinge:'right',open:'true',powered:'true'}}",
			"{Name:'minecraft:wooden_door',Properties:{facing:'south',half:'upper',hinge:'right',open:'false',powered:'true'}}",
			"{Name:'minecraft:wooden_door',Properties:{facing:'south',half:'upper',hinge:'right',open:'true',powered:'true'}}",
			"{Name:'minecraft:wooden_door',Properties:{facing:'west',half:'upper',hinge:'right',open:'false',powered:'true'}}",
			"{Name:'minecraft:wooden_door',Properties:{facing:'west',half:'upper',hinge:'right',open:'true',powered:'true'}}"
		);
		putStates(1036, "{Name:'minecraft:oak_door',Properties:{facing:'east',half:'upper',hinge:'left',open:'true',powered:'false'}}");
		putStates(1037, "{Name:'minecraft:oak_door',Properties:{facing:'south',half:'upper',hinge:'left',open:'true',powered:'false'}}");
		putStates(1038, "{Name:'minecraft:oak_door',Properties:{facing:'west',half:'upper',hinge:'left',open:'true',powered:'false'}}");
		putStates(1039, "{Name:'minecraft:oak_door',Properties:{facing:'north',half:'upper',hinge:'left',open:'true',powered:'false'}}");
		putStates(1042, "{Name:'minecraft:ladder',Properties:{facing:'north'}}", "{Name:'minecraft:ladder',Properties:{facing:'north'}}");
		putStates(1043, "{Name:'minecraft:ladder',Properties:{facing:'south'}}", "{Name:'minecraft:ladder',Properties:{facing:'south'}}");
		putStates(1044, "{Name:'minecraft:ladder',Properties:{facing:'west'}}", "{Name:'minecraft:ladder',Properties:{facing:'west'}}");
		putStates(1045, "{Name:'minecraft:ladder',Properties:{facing:'east'}}", "{Name:'minecraft:ladder',Properties:{facing:'east'}}");
		putStates(1056, "{Name:'minecraft:rail',Properties:{shape:'north_south'}}", "{Name:'minecraft:rail',Properties:{shape:'north_south'}}");
		putStates(1057, "{Name:'minecraft:rail',Properties:{shape:'east_west'}}", "{Name:'minecraft:rail',Properties:{shape:'east_west'}}");
		putStates(1058, "{Name:'minecraft:rail',Properties:{shape:'ascending_east'}}", "{Name:'minecraft:rail',Properties:{shape:'ascending_east'}}");
		putStates(1059, "{Name:'minecraft:rail',Properties:{shape:'ascending_west'}}", "{Name:'minecraft:rail',Properties:{shape:'ascending_west'}}");
		putStates(1060, "{Name:'minecraft:rail',Properties:{shape:'ascending_north'}}", "{Name:'minecraft:rail',Properties:{shape:'ascending_north'}}");
		putStates(1061, "{Name:'minecraft:rail',Properties:{shape:'ascending_south'}}", "{Name:'minecraft:rail',Properties:{shape:'ascending_south'}}");
		putStates(1062, "{Name:'minecraft:rail',Properties:{shape:'south_east'}}", "{Name:'minecraft:rail',Properties:{shape:'south_east'}}");
		putStates(1063, "{Name:'minecraft:rail',Properties:{shape:'south_west'}}", "{Name:'minecraft:rail',Properties:{shape:'south_west'}}");
		putStates(1064, "{Name:'minecraft:rail',Properties:{shape:'north_west'}}", "{Name:'minecraft:rail',Properties:{shape:'north_west'}}");
		putStates(1065, "{Name:'minecraft:rail',Properties:{shape:'north_east'}}", "{Name:'minecraft:rail',Properties:{shape:'north_east'}}");
		putStates(
			1072,
			"{Name:'minecraft:cobblestone_stairs',Properties:{facing:'east',half:'bottom',shape:'straight'}}",
			"{Name:'minecraft:stone_stairs',Properties:{facing:'east',half:'bottom',shape:'inner_left'}}",
			"{Name:'minecraft:stone_stairs',Properties:{facing:'east',half:'bottom',shape:'inner_right'}}",
			"{Name:'minecraft:stone_stairs',Properties:{facing:'east',half:'bottom',shape:'outer_left'}}",
			"{Name:'minecraft:stone_stairs',Properties:{facing:'east',half:'bottom',shape:'outer_right'}}",
			"{Name:'minecraft:stone_stairs',Properties:{facing:'east',half:'bottom',shape:'straight'}}"
		);
		putStates(
			1073,
			"{Name:'minecraft:cobblestone_stairs',Properties:{facing:'west',half:'bottom',shape:'straight'}}",
			"{Name:'minecraft:stone_stairs',Properties:{facing:'west',half:'bottom',shape:'inner_left'}}",
			"{Name:'minecraft:stone_stairs',Properties:{facing:'west',half:'bottom',shape:'inner_right'}}",
			"{Name:'minecraft:stone_stairs',Properties:{facing:'west',half:'bottom',shape:'outer_left'}}",
			"{Name:'minecraft:stone_stairs',Properties:{facing:'west',half:'bottom',shape:'outer_right'}}",
			"{Name:'minecraft:stone_stairs',Properties:{facing:'west',half:'bottom',shape:'straight'}}"
		);
		putStates(
			1074,
			"{Name:'minecraft:cobblestone_stairs',Properties:{facing:'south',half:'bottom',shape:'straight'}}",
			"{Name:'minecraft:stone_stairs',Properties:{facing:'south',half:'bottom',shape:'inner_left'}}",
			"{Name:'minecraft:stone_stairs',Properties:{facing:'south',half:'bottom',shape:'inner_right'}}",
			"{Name:'minecraft:stone_stairs',Properties:{facing:'south',half:'bottom',shape:'outer_left'}}",
			"{Name:'minecraft:stone_stairs',Properties:{facing:'south',half:'bottom',shape:'outer_right'}}",
			"{Name:'minecraft:stone_stairs',Properties:{facing:'south',half:'bottom',shape:'straight'}}"
		);
		putStates(
			1075,
			"{Name:'minecraft:cobblestone_stairs',Properties:{facing:'north',half:'bottom',shape:'straight'}}",
			"{Name:'minecraft:stone_stairs',Properties:{facing:'north',half:'bottom',shape:'inner_left'}}",
			"{Name:'minecraft:stone_stairs',Properties:{facing:'north',half:'bottom',shape:'inner_right'}}",
			"{Name:'minecraft:stone_stairs',Properties:{facing:'north',half:'bottom',shape:'outer_left'}}",
			"{Name:'minecraft:stone_stairs',Properties:{facing:'north',half:'bottom',shape:'outer_right'}}",
			"{Name:'minecraft:stone_stairs',Properties:{facing:'north',half:'bottom',shape:'straight'}}"
		);
		putStates(
			1076,
			"{Name:'minecraft:cobblestone_stairs',Properties:{facing:'east',half:'top',shape:'straight'}}",
			"{Name:'minecraft:stone_stairs',Properties:{facing:'east',half:'top',shape:'inner_left'}}",
			"{Name:'minecraft:stone_stairs',Properties:{facing:'east',half:'top',shape:'inner_right'}}",
			"{Name:'minecraft:stone_stairs',Properties:{facing:'east',half:'top',shape:'outer_left'}}",
			"{Name:'minecraft:stone_stairs',Properties:{facing:'east',half:'top',shape:'outer_right'}}",
			"{Name:'minecraft:stone_stairs',Properties:{facing:'east',half:'top',shape:'straight'}}"
		);
		putStates(
			1077,
			"{Name:'minecraft:cobblestone_stairs',Properties:{facing:'west',half:'top',shape:'straight'}}",
			"{Name:'minecraft:stone_stairs',Properties:{facing:'west',half:'top',shape:'inner_left'}}",
			"{Name:'minecraft:stone_stairs',Properties:{facing:'west',half:'top',shape:'inner_right'}}",
			"{Name:'minecraft:stone_stairs',Properties:{facing:'west',half:'top',shape:'outer_left'}}",
			"{Name:'minecraft:stone_stairs',Properties:{facing:'west',half:'top',shape:'outer_right'}}",
			"{Name:'minecraft:stone_stairs',Properties:{facing:'west',half:'top',shape:'straight'}}"
		);
		putStates(
			1078,
			"{Name:'minecraft:cobblestone_stairs',Properties:{facing:'south',half:'top',shape:'straight'}}",
			"{Name:'minecraft:stone_stairs',Properties:{facing:'south',half:'top',shape:'inner_left'}}",
			"{Name:'minecraft:stone_stairs',Properties:{facing:'south',half:'top',shape:'inner_right'}}",
			"{Name:'minecraft:stone_stairs',Properties:{facing:'south',half:'top',shape:'outer_left'}}",
			"{Name:'minecraft:stone_stairs',Properties:{facing:'south',half:'top',shape:'outer_right'}}",
			"{Name:'minecraft:stone_stairs',Properties:{facing:'south',half:'top',shape:'straight'}}"
		);
		putStates(
			1079,
			"{Name:'minecraft:cobblestone_stairs',Properties:{facing:'north',half:'top',shape:'straight'}}",
			"{Name:'minecraft:stone_stairs',Properties:{facing:'north',half:'top',shape:'inner_left'}}",
			"{Name:'minecraft:stone_stairs',Properties:{facing:'north',half:'top',shape:'inner_right'}}",
			"{Name:'minecraft:stone_stairs',Properties:{facing:'north',half:'top',shape:'outer_left'}}",
			"{Name:'minecraft:stone_stairs',Properties:{facing:'north',half:'top',shape:'outer_right'}}",
			"{Name:'minecraft:stone_stairs',Properties:{facing:'north',half:'top',shape:'straight'}}"
		);
		putStates(1090, "{Name:'minecraft:wall_sign',Properties:{facing:'north'}}", "{Name:'minecraft:wall_sign',Properties:{facing:'north'}}");
		putStates(1091, "{Name:'minecraft:wall_sign',Properties:{facing:'south'}}", "{Name:'minecraft:wall_sign',Properties:{facing:'south'}}");
		putStates(1092, "{Name:'minecraft:wall_sign',Properties:{facing:'west'}}", "{Name:'minecraft:wall_sign',Properties:{facing:'west'}}");
		putStates(1093, "{Name:'minecraft:wall_sign',Properties:{facing:'east'}}", "{Name:'minecraft:wall_sign',Properties:{facing:'east'}}");
		putStates(
			1104,
			"{Name:'minecraft:lever',Properties:{face:'ceiling',facing:'west',powered:'false'}}",
			"{Name:'minecraft:lever',Properties:{facing:'down_x',powered:'false'}}"
		);
		putStates(
			1105,
			"{Name:'minecraft:lever',Properties:{face:'wall',facing:'east',powered:'false'}}",
			"{Name:'minecraft:lever',Properties:{facing:'east',powered:'false'}}"
		);
		putStates(
			1106,
			"{Name:'minecraft:lever',Properties:{face:'wall',facing:'west',powered:'false'}}",
			"{Name:'minecraft:lever',Properties:{facing:'west',powered:'false'}}"
		);
		putStates(
			1107,
			"{Name:'minecraft:lever',Properties:{face:'wall',facing:'south',powered:'false'}}",
			"{Name:'minecraft:lever',Properties:{facing:'south',powered:'false'}}"
		);
		putStates(
			1108,
			"{Name:'minecraft:lever',Properties:{face:'wall',facing:'north',powered:'false'}}",
			"{Name:'minecraft:lever',Properties:{facing:'north',powered:'false'}}"
		);
		putStates(
			1109,
			"{Name:'minecraft:lever',Properties:{face:'floor',facing:'north',powered:'false'}}",
			"{Name:'minecraft:lever',Properties:{facing:'up_z',powered:'false'}}"
		);
		putStates(
			1110,
			"{Name:'minecraft:lever',Properties:{face:'floor',facing:'west',powered:'false'}}",
			"{Name:'minecraft:lever',Properties:{facing:'up_x',powered:'false'}}"
		);
		putStates(
			1111,
			"{Name:'minecraft:lever',Properties:{face:'ceiling',facing:'north',powered:'false'}}",
			"{Name:'minecraft:lever',Properties:{facing:'down_z',powered:'false'}}"
		);
		putStates(
			1112,
			"{Name:'minecraft:lever',Properties:{face:'ceiling',facing:'west',powered:'true'}}",
			"{Name:'minecraft:lever',Properties:{facing:'down_x',powered:'true'}}"
		);
		putStates(
			1113, "{Name:'minecraft:lever',Properties:{face:'wall',facing:'east',powered:'true'}}", "{Name:'minecraft:lever',Properties:{facing:'east',powered:'true'}}"
		);
		putStates(
			1114, "{Name:'minecraft:lever',Properties:{face:'wall',facing:'west',powered:'true'}}", "{Name:'minecraft:lever',Properties:{facing:'west',powered:'true'}}"
		);
		putStates(
			1115,
			"{Name:'minecraft:lever',Properties:{face:'wall',facing:'south',powered:'true'}}",
			"{Name:'minecraft:lever',Properties:{facing:'south',powered:'true'}}"
		);
		putStates(
			1116,
			"{Name:'minecraft:lever',Properties:{face:'wall',facing:'north',powered:'true'}}",
			"{Name:'minecraft:lever',Properties:{facing:'north',powered:'true'}}"
		);
		putStates(
			1117,
			"{Name:'minecraft:lever',Properties:{face:'floor',facing:'north',powered:'true'}}",
			"{Name:'minecraft:lever',Properties:{facing:'up_z',powered:'true'}}"
		);
		putStates(
			1118,
			"{Name:'minecraft:lever',Properties:{face:'floor',facing:'west',powered:'true'}}",
			"{Name:'minecraft:lever',Properties:{facing:'up_x',powered:'true'}}"
		);
		putStates(
			1119,
			"{Name:'minecraft:lever',Properties:{face:'ceiling',facing:'north',powered:'true'}}",
			"{Name:'minecraft:lever',Properties:{facing:'down_z',powered:'true'}}"
		);
		putStates(
			1120, "{Name:'minecraft:stone_pressure_plate',Properties:{powered:'false'}}", "{Name:'minecraft:stone_pressure_plate',Properties:{powered:'false'}}"
		);
		putStates(1121, "{Name:'minecraft:stone_pressure_plate',Properties:{powered:'true'}}", "{Name:'minecraft:stone_pressure_plate',Properties:{powered:'true'}}");
		putStates(
			1136,
			"{Name:'minecraft:iron_door',Properties:{facing:'east',half:'lower',hinge:'right',open:'false',powered:'false'}}",
			"{Name:'minecraft:iron_door',Properties:{facing:'east',half:'lower',hinge:'left',open:'false',powered:'false'}}",
			"{Name:'minecraft:iron_door',Properties:{facing:'east',half:'lower',hinge:'left',open:'false',powered:'true'}}",
			"{Name:'minecraft:iron_door',Properties:{facing:'east',half:'lower',hinge:'right',open:'false',powered:'false'}}",
			"{Name:'minecraft:iron_door',Properties:{facing:'east',half:'lower',hinge:'right',open:'false',powered:'true'}}"
		);
		putStates(
			1137,
			"{Name:'minecraft:iron_door',Properties:{facing:'south',half:'lower',hinge:'right',open:'false',powered:'false'}}",
			"{Name:'minecraft:iron_door',Properties:{facing:'south',half:'lower',hinge:'left',open:'false',powered:'false'}}",
			"{Name:'minecraft:iron_door',Properties:{facing:'south',half:'lower',hinge:'left',open:'false',powered:'true'}}",
			"{Name:'minecraft:iron_door',Properties:{facing:'south',half:'lower',hinge:'right',open:'false',powered:'false'}}",
			"{Name:'minecraft:iron_door',Properties:{facing:'south',half:'lower',hinge:'right',open:'false',powered:'true'}}"
		);
		putStates(
			1138,
			"{Name:'minecraft:iron_door',Properties:{facing:'west',half:'lower',hinge:'right',open:'false',powered:'false'}}",
			"{Name:'minecraft:iron_door',Properties:{facing:'west',half:'lower',hinge:'left',open:'false',powered:'false'}}",
			"{Name:'minecraft:iron_door',Properties:{facing:'west',half:'lower',hinge:'left',open:'false',powered:'true'}}",
			"{Name:'minecraft:iron_door',Properties:{facing:'west',half:'lower',hinge:'right',open:'false',powered:'false'}}",
			"{Name:'minecraft:iron_door',Properties:{facing:'west',half:'lower',hinge:'right',open:'false',powered:'true'}}"
		);
		putStates(
			1139,
			"{Name:'minecraft:iron_door',Properties:{facing:'north',half:'lower',hinge:'right',open:'false',powered:'false'}}",
			"{Name:'minecraft:iron_door',Properties:{facing:'north',half:'lower',hinge:'left',open:'false',powered:'false'}}",
			"{Name:'minecraft:iron_door',Properties:{facing:'north',half:'lower',hinge:'left',open:'false',powered:'true'}}",
			"{Name:'minecraft:iron_door',Properties:{facing:'north',half:'lower',hinge:'right',open:'false',powered:'false'}}",
			"{Name:'minecraft:iron_door',Properties:{facing:'north',half:'lower',hinge:'right',open:'false',powered:'true'}}"
		);
		putStates(
			1140,
			"{Name:'minecraft:iron_door',Properties:{facing:'east',half:'lower',hinge:'right',open:'true',powered:'false'}}",
			"{Name:'minecraft:iron_door',Properties:{facing:'east',half:'lower',hinge:'left',open:'true',powered:'false'}}",
			"{Name:'minecraft:iron_door',Properties:{facing:'east',half:'lower',hinge:'left',open:'true',powered:'true'}}",
			"{Name:'minecraft:iron_door',Properties:{facing:'east',half:'lower',hinge:'right',open:'true',powered:'false'}}",
			"{Name:'minecraft:iron_door',Properties:{facing:'east',half:'lower',hinge:'right',open:'true',powered:'true'}}"
		);
		putStates(
			1141,
			"{Name:'minecraft:iron_door',Properties:{facing:'south',half:'lower',hinge:'right',open:'true',powered:'false'}}",
			"{Name:'minecraft:iron_door',Properties:{facing:'south',half:'lower',hinge:'left',open:'true',powered:'false'}}",
			"{Name:'minecraft:iron_door',Properties:{facing:'south',half:'lower',hinge:'left',open:'true',powered:'true'}}",
			"{Name:'minecraft:iron_door',Properties:{facing:'south',half:'lower',hinge:'right',open:'true',powered:'false'}}",
			"{Name:'minecraft:iron_door',Properties:{facing:'south',half:'lower',hinge:'right',open:'true',powered:'true'}}"
		);
		putStates(
			1142,
			"{Name:'minecraft:iron_door',Properties:{facing:'west',half:'lower',hinge:'right',open:'true',powered:'false'}}",
			"{Name:'minecraft:iron_door',Properties:{facing:'west',half:'lower',hinge:'left',open:'true',powered:'false'}}",
			"{Name:'minecraft:iron_door',Properties:{facing:'west',half:'lower',hinge:'left',open:'true',powered:'true'}}",
			"{Name:'minecraft:iron_door',Properties:{facing:'west',half:'lower',hinge:'right',open:'true',powered:'false'}}",
			"{Name:'minecraft:iron_door',Properties:{facing:'west',half:'lower',hinge:'right',open:'true',powered:'true'}}"
		);
		putStates(
			1143,
			"{Name:'minecraft:iron_door',Properties:{facing:'north',half:'lower',hinge:'right',open:'true',powered:'false'}}",
			"{Name:'minecraft:iron_door',Properties:{facing:'north',half:'lower',hinge:'left',open:'true',powered:'false'}}",
			"{Name:'minecraft:iron_door',Properties:{facing:'north',half:'lower',hinge:'left',open:'true',powered:'true'}}",
			"{Name:'minecraft:iron_door',Properties:{facing:'north',half:'lower',hinge:'right',open:'true',powered:'false'}}",
			"{Name:'minecraft:iron_door',Properties:{facing:'north',half:'lower',hinge:'right',open:'true',powered:'true'}}"
		);
		putStates(
			1144,
			"{Name:'minecraft:iron_door',Properties:{facing:'east',half:'upper',hinge:'left',open:'false',powered:'false'}}",
			"{Name:'minecraft:iron_door',Properties:{facing:'east',half:'upper',hinge:'left',open:'false',powered:'false'}}",
			"{Name:'minecraft:iron_door',Properties:{facing:'east',half:'upper',hinge:'left',open:'true',powered:'false'}}",
			"{Name:'minecraft:iron_door',Properties:{facing:'north',half:'upper',hinge:'left',open:'false',powered:'false'}}",
			"{Name:'minecraft:iron_door',Properties:{facing:'north',half:'upper',hinge:'left',open:'true',powered:'false'}}",
			"{Name:'minecraft:iron_door',Properties:{facing:'south',half:'upper',hinge:'left',open:'false',powered:'false'}}",
			"{Name:'minecraft:iron_door',Properties:{facing:'south',half:'upper',hinge:'left',open:'true',powered:'false'}}",
			"{Name:'minecraft:iron_door',Properties:{facing:'west',half:'upper',hinge:'left',open:'false',powered:'false'}}",
			"{Name:'minecraft:iron_door',Properties:{facing:'west',half:'upper',hinge:'left',open:'true',powered:'false'}}"
		);
		putStates(
			1145,
			"{Name:'minecraft:iron_door',Properties:{facing:'east',half:'upper',hinge:'right',open:'false',powered:'false'}}",
			"{Name:'minecraft:iron_door',Properties:{facing:'east',half:'upper',hinge:'right',open:'false',powered:'false'}}",
			"{Name:'minecraft:iron_door',Properties:{facing:'east',half:'upper',hinge:'right',open:'true',powered:'false'}}",
			"{Name:'minecraft:iron_door',Properties:{facing:'north',half:'upper',hinge:'right',open:'false',powered:'false'}}",
			"{Name:'minecraft:iron_door',Properties:{facing:'north',half:'upper',hinge:'right',open:'true',powered:'false'}}",
			"{Name:'minecraft:iron_door',Properties:{facing:'south',half:'upper',hinge:'right',open:'false',powered:'false'}}",
			"{Name:'minecraft:iron_door',Properties:{facing:'south',half:'upper',hinge:'right',open:'true',powered:'false'}}",
			"{Name:'minecraft:iron_door',Properties:{facing:'west',half:'upper',hinge:'right',open:'false',powered:'false'}}",
			"{Name:'minecraft:iron_door',Properties:{facing:'west',half:'upper',hinge:'right',open:'true',powered:'false'}}"
		);
		putStates(
			1146,
			"{Name:'minecraft:iron_door',Properties:{facing:'east',half:'upper',hinge:'left',open:'false',powered:'true'}}",
			"{Name:'minecraft:iron_door',Properties:{facing:'east',half:'upper',hinge:'left',open:'false',powered:'true'}}",
			"{Name:'minecraft:iron_door',Properties:{facing:'east',half:'upper',hinge:'left',open:'true',powered:'true'}}",
			"{Name:'minecraft:iron_door',Properties:{facing:'north',half:'upper',hinge:'left',open:'false',powered:'true'}}",
			"{Name:'minecraft:iron_door',Properties:{facing:'north',half:'upper',hinge:'left',open:'true',powered:'true'}}",
			"{Name:'minecraft:iron_door',Properties:{facing:'south',half:'upper',hinge:'left',open:'false',powered:'true'}}",
			"{Name:'minecraft:iron_door',Properties:{facing:'south',half:'upper',hinge:'left',open:'true',powered:'true'}}",
			"{Name:'minecraft:iron_door',Properties:{facing:'west',half:'upper',hinge:'left',open:'false',powered:'true'}}",
			"{Name:'minecraft:iron_door',Properties:{facing:'west',half:'upper',hinge:'left',open:'true',powered:'true'}}"
		);
		putStates(
			1147,
			"{Name:'minecraft:iron_door',Properties:{facing:'east',half:'upper',hinge:'right',open:'false',powered:'true'}}",
			"{Name:'minecraft:iron_door',Properties:{facing:'east',half:'upper',hinge:'right',open:'false',powered:'true'}}",
			"{Name:'minecraft:iron_door',Properties:{facing:'east',half:'upper',hinge:'right',open:'true',powered:'true'}}",
			"{Name:'minecraft:iron_door',Properties:{facing:'north',half:'upper',hinge:'right',open:'false',powered:'true'}}",
			"{Name:'minecraft:iron_door',Properties:{facing:'north',half:'upper',hinge:'right',open:'true',powered:'true'}}",
			"{Name:'minecraft:iron_door',Properties:{facing:'south',half:'upper',hinge:'right',open:'false',powered:'true'}}",
			"{Name:'minecraft:iron_door',Properties:{facing:'south',half:'upper',hinge:'right',open:'true',powered:'true'}}",
			"{Name:'minecraft:iron_door',Properties:{facing:'west',half:'upper',hinge:'right',open:'false',powered:'true'}}",
			"{Name:'minecraft:iron_door',Properties:{facing:'west',half:'upper',hinge:'right',open:'true',powered:'true'}}"
		);
		putStates(1148, "{Name:'minecraft:iron_door',Properties:{facing:'east',half:'upper',hinge:'left',open:'true',powered:'false'}}");
		putStates(1149, "{Name:'minecraft:iron_door',Properties:{facing:'south',half:'upper',hinge:'left',open:'true',powered:'false'}}");
		putStates(1150, "{Name:'minecraft:iron_door',Properties:{facing:'west',half:'upper',hinge:'left',open:'true',powered:'false'}}");
		putStates(1151, "{Name:'minecraft:iron_door',Properties:{facing:'north',half:'upper',hinge:'left',open:'true',powered:'false'}}");
		putStates(1152, "{Name:'minecraft:oak_pressure_plate',Properties:{powered:'false'}}", "{Name:'minecraft:wooden_pressure_plate',Properties:{powered:'false'}}");
		putStates(1153, "{Name:'minecraft:oak_pressure_plate',Properties:{powered:'true'}}", "{Name:'minecraft:wooden_pressure_plate',Properties:{powered:'true'}}");
		putStates(1168, "{Name:'minecraft:redstone_ore',Properties:{lit:'false'}}", "{Name:'minecraft:redstone_ore'}");
		putStates(1184, "{Name:'minecraft:redstone_ore',Properties:{lit:'true'}}", "{Name:'minecraft:lit_redstone_ore'}");
		putStates(
			1201, "{Name:'minecraft:redstone_wall_torch',Properties:{facing:'east',lit:'false'}}", "{Name:'minecraft:unlit_redstone_torch',Properties:{facing:'east'}}"
		);
		putStates(
			1202, "{Name:'minecraft:redstone_wall_torch',Properties:{facing:'west',lit:'false'}}", "{Name:'minecraft:unlit_redstone_torch',Properties:{facing:'west'}}"
		);
		putStates(
			1203,
			"{Name:'minecraft:redstone_wall_torch',Properties:{facing:'south',lit:'false'}}",
			"{Name:'minecraft:unlit_redstone_torch',Properties:{facing:'south'}}"
		);
		putStates(
			1204,
			"{Name:'minecraft:redstone_wall_torch',Properties:{facing:'north',lit:'false'}}",
			"{Name:'minecraft:unlit_redstone_torch',Properties:{facing:'north'}}"
		);
		putStates(1205, "{Name:'minecraft:redstone_torch',Properties:{lit:'false'}}", "{Name:'minecraft:unlit_redstone_torch',Properties:{facing:'up'}}");
		putStates(
			1217, "{Name:'minecraft:redstone_wall_torch',Properties:{facing:'east',lit:'true'}}", "{Name:'minecraft:redstone_torch',Properties:{facing:'east'}}"
		);
		putStates(
			1218, "{Name:'minecraft:redstone_wall_torch',Properties:{facing:'west',lit:'true'}}", "{Name:'minecraft:redstone_torch',Properties:{facing:'west'}}"
		);
		putStates(
			1219, "{Name:'minecraft:redstone_wall_torch',Properties:{facing:'south',lit:'true'}}", "{Name:'minecraft:redstone_torch',Properties:{facing:'south'}}"
		);
		putStates(
			1220, "{Name:'minecraft:redstone_wall_torch',Properties:{facing:'north',lit:'true'}}", "{Name:'minecraft:redstone_torch',Properties:{facing:'north'}}"
		);
		putStates(1221, "{Name:'minecraft:redstone_torch',Properties:{lit:'true'}}", "{Name:'minecraft:redstone_torch',Properties:{facing:'up'}}");
		putStates(
			1232,
			"{Name:'minecraft:stone_button',Properties:{face:'ceiling',facing:'north',powered:'false'}}",
			"{Name:'minecraft:stone_button',Properties:{facing:'down',powered:'false'}}"
		);
		putStates(
			1233,
			"{Name:'minecraft:stone_button',Properties:{face:'wall',facing:'east',powered:'false'}}",
			"{Name:'minecraft:stone_button',Properties:{facing:'east',powered:'false'}}"
		);
		putStates(
			1234,
			"{Name:'minecraft:stone_button',Properties:{face:'wall',facing:'west',powered:'false'}}",
			"{Name:'minecraft:stone_button',Properties:{facing:'west',powered:'false'}}"
		);
		putStates(
			1235,
			"{Name:'minecraft:stone_button',Properties:{face:'wall',facing:'south',powered:'false'}}",
			"{Name:'minecraft:stone_button',Properties:{facing:'south',powered:'false'}}"
		);
		putStates(
			1236,
			"{Name:'minecraft:stone_button',Properties:{face:'wall',facing:'north',powered:'false'}}",
			"{Name:'minecraft:stone_button',Properties:{facing:'north',powered:'false'}}"
		);
		putStates(
			1237,
			"{Name:'minecraft:stone_button',Properties:{face:'floor',facing:'north',powered:'false'}}",
			"{Name:'minecraft:stone_button',Properties:{facing:'up',powered:'false'}}"
		);
		putStates(
			1240,
			"{Name:'minecraft:stone_button',Properties:{face:'ceiling',facing:'north',powered:'true'}}",
			"{Name:'minecraft:stone_button',Properties:{facing:'down',powered:'true'}}"
		);
		putStates(
			1241,
			"{Name:'minecraft:stone_button',Properties:{face:'wall',facing:'east',powered:'true'}}",
			"{Name:'minecraft:stone_button',Properties:{facing:'east',powered:'true'}}"
		);
		putStates(
			1242,
			"{Name:'minecraft:stone_button',Properties:{face:'wall',facing:'west',powered:'true'}}",
			"{Name:'minecraft:stone_button',Properties:{facing:'west',powered:'true'}}"
		);
		putStates(
			1243,
			"{Name:'minecraft:stone_button',Properties:{face:'wall',facing:'south',powered:'true'}}",
			"{Name:'minecraft:stone_button',Properties:{facing:'south',powered:'true'}}"
		);
		putStates(
			1244,
			"{Name:'minecraft:stone_button',Properties:{face:'wall',facing:'north',powered:'true'}}",
			"{Name:'minecraft:stone_button',Properties:{facing:'north',powered:'true'}}"
		);
		putStates(
			1245,
			"{Name:'minecraft:stone_button',Properties:{face:'floor',facing:'north',powered:'true'}}",
			"{Name:'minecraft:stone_button',Properties:{facing:'up',powered:'true'}}"
		);
		putStates(1248, "{Name:'minecraft:snow',Properties:{layers:'1'}}", "{Name:'minecraft:snow_layer',Properties:{layers:'1'}}");
		putStates(1249, "{Name:'minecraft:snow',Properties:{layers:'2'}}", "{Name:'minecraft:snow_layer',Properties:{layers:'2'}}");
		putStates(1250, "{Name:'minecraft:snow',Properties:{layers:'3'}}", "{Name:'minecraft:snow_layer',Properties:{layers:'3'}}");
		putStates(1251, "{Name:'minecraft:snow',Properties:{layers:'4'}}", "{Name:'minecraft:snow_layer',Properties:{layers:'4'}}");
		putStates(1252, "{Name:'minecraft:snow',Properties:{layers:'5'}}", "{Name:'minecraft:snow_layer',Properties:{layers:'5'}}");
		putStates(1253, "{Name:'minecraft:snow',Properties:{layers:'6'}}", "{Name:'minecraft:snow_layer',Properties:{layers:'6'}}");
		putStates(1254, "{Name:'minecraft:snow',Properties:{layers:'7'}}", "{Name:'minecraft:snow_layer',Properties:{layers:'7'}}");
		putStates(1255, "{Name:'minecraft:snow',Properties:{layers:'8'}}", "{Name:'minecraft:snow_layer',Properties:{layers:'8'}}");
		putStates(1264, "{Name:'minecraft:ice'}", "{Name:'minecraft:ice'}");
	}

	/**
	 * Adds states to flatten from the blocks which had numeric IDs between 80 and 95 before 1.13.
	 */
	static void putStatesFromBlocks80To95() {
		putStates(1280, "{Name:'minecraft:snow_block'}", "{Name:'minecraft:snow'}");
		putStates(1296, "{Name:'minecraft:cactus',Properties:{age:'0'}}", "{Name:'minecraft:cactus',Properties:{age:'0'}}");
		putStates(1297, "{Name:'minecraft:cactus',Properties:{age:'1'}}", "{Name:'minecraft:cactus',Properties:{age:'1'}}");
		putStates(1298, "{Name:'minecraft:cactus',Properties:{age:'2'}}", "{Name:'minecraft:cactus',Properties:{age:'2'}}");
		putStates(1299, "{Name:'minecraft:cactus',Properties:{age:'3'}}", "{Name:'minecraft:cactus',Properties:{age:'3'}}");
		putStates(1300, "{Name:'minecraft:cactus',Properties:{age:'4'}}", "{Name:'minecraft:cactus',Properties:{age:'4'}}");
		putStates(1301, "{Name:'minecraft:cactus',Properties:{age:'5'}}", "{Name:'minecraft:cactus',Properties:{age:'5'}}");
		putStates(1302, "{Name:'minecraft:cactus',Properties:{age:'6'}}", "{Name:'minecraft:cactus',Properties:{age:'6'}}");
		putStates(1303, "{Name:'minecraft:cactus',Properties:{age:'7'}}", "{Name:'minecraft:cactus',Properties:{age:'7'}}");
		putStates(1304, "{Name:'minecraft:cactus',Properties:{age:'8'}}", "{Name:'minecraft:cactus',Properties:{age:'8'}}");
		putStates(1305, "{Name:'minecraft:cactus',Properties:{age:'9'}}", "{Name:'minecraft:cactus',Properties:{age:'9'}}");
		putStates(1306, "{Name:'minecraft:cactus',Properties:{age:'10'}}", "{Name:'minecraft:cactus',Properties:{age:'10'}}");
		putStates(1307, "{Name:'minecraft:cactus',Properties:{age:'11'}}", "{Name:'minecraft:cactus',Properties:{age:'11'}}");
		putStates(1308, "{Name:'minecraft:cactus',Properties:{age:'12'}}", "{Name:'minecraft:cactus',Properties:{age:'12'}}");
		putStates(1309, "{Name:'minecraft:cactus',Properties:{age:'13'}}", "{Name:'minecraft:cactus',Properties:{age:'13'}}");
		putStates(1310, "{Name:'minecraft:cactus',Properties:{age:'14'}}", "{Name:'minecraft:cactus',Properties:{age:'14'}}");
		putStates(1311, "{Name:'minecraft:cactus',Properties:{age:'15'}}", "{Name:'minecraft:cactus',Properties:{age:'15'}}");
		putStates(1312, "{Name:'minecraft:clay'}", "{Name:'minecraft:clay'}");
		putStates(1328, "{Name:'minecraft:sugar_cane',Properties:{age:'0'}}", "{Name:'minecraft:reeds',Properties:{age:'0'}}");
		putStates(1329, "{Name:'minecraft:sugar_cane',Properties:{age:'1'}}", "{Name:'minecraft:reeds',Properties:{age:'1'}}");
		putStates(1330, "{Name:'minecraft:sugar_cane',Properties:{age:'2'}}", "{Name:'minecraft:reeds',Properties:{age:'2'}}");
		putStates(1331, "{Name:'minecraft:sugar_cane',Properties:{age:'3'}}", "{Name:'minecraft:reeds',Properties:{age:'3'}}");
		putStates(1332, "{Name:'minecraft:sugar_cane',Properties:{age:'4'}}", "{Name:'minecraft:reeds',Properties:{age:'4'}}");
		putStates(1333, "{Name:'minecraft:sugar_cane',Properties:{age:'5'}}", "{Name:'minecraft:reeds',Properties:{age:'5'}}");
		putStates(1334, "{Name:'minecraft:sugar_cane',Properties:{age:'6'}}", "{Name:'minecraft:reeds',Properties:{age:'6'}}");
		putStates(1335, "{Name:'minecraft:sugar_cane',Properties:{age:'7'}}", "{Name:'minecraft:reeds',Properties:{age:'7'}}");
		putStates(1336, "{Name:'minecraft:sugar_cane',Properties:{age:'8'}}", "{Name:'minecraft:reeds',Properties:{age:'8'}}");
		putStates(1337, "{Name:'minecraft:sugar_cane',Properties:{age:'9'}}", "{Name:'minecraft:reeds',Properties:{age:'9'}}");
		putStates(1338, "{Name:'minecraft:sugar_cane',Properties:{age:'10'}}", "{Name:'minecraft:reeds',Properties:{age:'10'}}");
		putStates(1339, "{Name:'minecraft:sugar_cane',Properties:{age:'11'}}", "{Name:'minecraft:reeds',Properties:{age:'11'}}");
		putStates(1340, "{Name:'minecraft:sugar_cane',Properties:{age:'12'}}", "{Name:'minecraft:reeds',Properties:{age:'12'}}");
		putStates(1341, "{Name:'minecraft:sugar_cane',Properties:{age:'13'}}", "{Name:'minecraft:reeds',Properties:{age:'13'}}");
		putStates(1342, "{Name:'minecraft:sugar_cane',Properties:{age:'14'}}", "{Name:'minecraft:reeds',Properties:{age:'14'}}");
		putStates(1343, "{Name:'minecraft:sugar_cane',Properties:{age:'15'}}", "{Name:'minecraft:reeds',Properties:{age:'15'}}");
		putStates(1344, "{Name:'minecraft:jukebox',Properties:{has_record:'false'}}", "{Name:'minecraft:jukebox',Properties:{has_record:'false'}}");
		putStates(1345, "{Name:'minecraft:jukebox',Properties:{has_record:'true'}}", "{Name:'minecraft:jukebox',Properties:{has_record:'true'}}");
		putStates(
			1360,
			"{Name:'minecraft:oak_fence',Properties:{east:'false',north:'false',south:'false',west:'false'}}",
			"{Name:'minecraft:fence',Properties:{east:'false',north:'false',south:'false',west:'false'}}",
			"{Name:'minecraft:fence',Properties:{east:'false',north:'false',south:'false',west:'true'}}",
			"{Name:'minecraft:fence',Properties:{east:'false',north:'false',south:'true',west:'false'}}",
			"{Name:'minecraft:fence',Properties:{east:'false',north:'false',south:'true',west:'true'}}",
			"{Name:'minecraft:fence',Properties:{east:'false',north:'true',south:'false',west:'false'}}",
			"{Name:'minecraft:fence',Properties:{east:'false',north:'true',south:'false',west:'true'}}",
			"{Name:'minecraft:fence',Properties:{east:'false',north:'true',south:'true',west:'false'}}",
			"{Name:'minecraft:fence',Properties:{east:'false',north:'true',south:'true',west:'true'}}",
			"{Name:'minecraft:fence',Properties:{east:'true',north:'false',south:'false',west:'false'}}",
			"{Name:'minecraft:fence',Properties:{east:'true',north:'false',south:'false',west:'true'}}",
			"{Name:'minecraft:fence',Properties:{east:'true',north:'false',south:'true',west:'false'}}",
			"{Name:'minecraft:fence',Properties:{east:'true',north:'false',south:'true',west:'true'}}",
			"{Name:'minecraft:fence',Properties:{east:'true',north:'true',south:'false',west:'false'}}",
			"{Name:'minecraft:fence',Properties:{east:'true',north:'true',south:'false',west:'true'}}",
			"{Name:'minecraft:fence',Properties:{east:'true',north:'true',south:'true',west:'false'}}",
			"{Name:'minecraft:fence',Properties:{east:'true',north:'true',south:'true',west:'true'}}"
		);
		putStates(1376, "{Name:'minecraft:carved_pumpkin',Properties:{facing:'south'}}", "{Name:'minecraft:pumpkin',Properties:{facing:'south'}}");
		putStates(1377, "{Name:'minecraft:carved_pumpkin',Properties:{facing:'west'}}", "{Name:'minecraft:pumpkin',Properties:{facing:'west'}}");
		putStates(1378, "{Name:'minecraft:carved_pumpkin',Properties:{facing:'north'}}", "{Name:'minecraft:pumpkin',Properties:{facing:'north'}}");
		putStates(1379, "{Name:'minecraft:carved_pumpkin',Properties:{facing:'east'}}", "{Name:'minecraft:pumpkin',Properties:{facing:'east'}}");
		putStates(1392, "{Name:'minecraft:netherrack'}", "{Name:'minecraft:netherrack'}");
		putStates(1408, "{Name:'minecraft:soul_sand'}", "{Name:'minecraft:soul_sand'}");
		putStates(1424, "{Name:'minecraft:glowstone'}", "{Name:'minecraft:glowstone'}");
		putStates(1441, "{Name:'minecraft:portal',Properties:{axis:'x'}}", "{Name:'minecraft:portal',Properties:{axis:'x'}}");
		putStates(1442, "{Name:'minecraft:portal',Properties:{axis:'z'}}", "{Name:'minecraft:portal',Properties:{axis:'z'}}");
		putStates(1456, "{Name:'minecraft:jack_o_lantern',Properties:{facing:'south'}}", "{Name:'minecraft:lit_pumpkin',Properties:{facing:'south'}}");
		putStates(1457, "{Name:'minecraft:jack_o_lantern',Properties:{facing:'west'}}", "{Name:'minecraft:lit_pumpkin',Properties:{facing:'west'}}");
		putStates(1458, "{Name:'minecraft:jack_o_lantern',Properties:{facing:'north'}}", "{Name:'minecraft:lit_pumpkin',Properties:{facing:'north'}}");
		putStates(1459, "{Name:'minecraft:jack_o_lantern',Properties:{facing:'east'}}", "{Name:'minecraft:lit_pumpkin',Properties:{facing:'east'}}");
		putStates(1472, "{Name:'minecraft:cake',Properties:{bites:'0'}}", "{Name:'minecraft:cake',Properties:{bites:'0'}}");
		putStates(1473, "{Name:'minecraft:cake',Properties:{bites:'1'}}", "{Name:'minecraft:cake',Properties:{bites:'1'}}");
		putStates(1474, "{Name:'minecraft:cake',Properties:{bites:'2'}}", "{Name:'minecraft:cake',Properties:{bites:'2'}}");
		putStates(1475, "{Name:'minecraft:cake',Properties:{bites:'3'}}", "{Name:'minecraft:cake',Properties:{bites:'3'}}");
		putStates(1476, "{Name:'minecraft:cake',Properties:{bites:'4'}}", "{Name:'minecraft:cake',Properties:{bites:'4'}}");
		putStates(1477, "{Name:'minecraft:cake',Properties:{bites:'5'}}", "{Name:'minecraft:cake',Properties:{bites:'5'}}");
		putStates(1478, "{Name:'minecraft:cake',Properties:{bites:'6'}}", "{Name:'minecraft:cake',Properties:{bites:'6'}}");
		putStates(
			1488,
			"{Name:'minecraft:repeater',Properties:{delay:'1',facing:'south',locked:'false',powered:'false'}}",
			"{Name:'minecraft:unpowered_repeater',Properties:{delay:'1',facing:'south',locked:'false'}}",
			"{Name:'minecraft:unpowered_repeater',Properties:{delay:'1',facing:'south',locked:'true'}}"
		);
		putStates(
			1489,
			"{Name:'minecraft:repeater',Properties:{delay:'1',facing:'west',locked:'false',powered:'false'}}",
			"{Name:'minecraft:unpowered_repeater',Properties:{delay:'1',facing:'west',locked:'false'}}",
			"{Name:'minecraft:unpowered_repeater',Properties:{delay:'1',facing:'west',locked:'true'}}"
		);
		putStates(
			1490,
			"{Name:'minecraft:repeater',Properties:{delay:'1',facing:'north',locked:'false',powered:'false'}}",
			"{Name:'minecraft:unpowered_repeater',Properties:{delay:'1',facing:'north',locked:'false'}}",
			"{Name:'minecraft:unpowered_repeater',Properties:{delay:'1',facing:'north',locked:'true'}}"
		);
		putStates(
			1491,
			"{Name:'minecraft:repeater',Properties:{delay:'1',facing:'east',locked:'false',powered:'false'}}",
			"{Name:'minecraft:unpowered_repeater',Properties:{delay:'1',facing:'east',locked:'false'}}",
			"{Name:'minecraft:unpowered_repeater',Properties:{delay:'1',facing:'east',locked:'true'}}"
		);
		putStates(
			1492,
			"{Name:'minecraft:repeater',Properties:{delay:'2',facing:'south',locked:'false',powered:'false'}}",
			"{Name:'minecraft:unpowered_repeater',Properties:{delay:'2',facing:'south',locked:'false'}}",
			"{Name:'minecraft:unpowered_repeater',Properties:{delay:'2',facing:'south',locked:'true'}}"
		);
		putStates(
			1493,
			"{Name:'minecraft:repeater',Properties:{delay:'2',facing:'west',locked:'false',powered:'false'}}",
			"{Name:'minecraft:unpowered_repeater',Properties:{delay:'2',facing:'west',locked:'false'}}",
			"{Name:'minecraft:unpowered_repeater',Properties:{delay:'2',facing:'west',locked:'true'}}"
		);
		putStates(
			1494,
			"{Name:'minecraft:repeater',Properties:{delay:'2',facing:'north',locked:'false',powered:'false'}}",
			"{Name:'minecraft:unpowered_repeater',Properties:{delay:'2',facing:'north',locked:'false'}}",
			"{Name:'minecraft:unpowered_repeater',Properties:{delay:'2',facing:'north',locked:'true'}}"
		);
		putStates(
			1495,
			"{Name:'minecraft:repeater',Properties:{delay:'2',facing:'east',locked:'false',powered:'false'}}",
			"{Name:'minecraft:unpowered_repeater',Properties:{delay:'2',facing:'east',locked:'false'}}",
			"{Name:'minecraft:unpowered_repeater',Properties:{delay:'2',facing:'east',locked:'true'}}"
		);
		putStates(
			1496,
			"{Name:'minecraft:repeater',Properties:{delay:'3',facing:'south',locked:'false',powered:'false'}}",
			"{Name:'minecraft:unpowered_repeater',Properties:{delay:'3',facing:'south',locked:'false'}}",
			"{Name:'minecraft:unpowered_repeater',Properties:{delay:'3',facing:'south',locked:'true'}}"
		);
		putStates(
			1497,
			"{Name:'minecraft:repeater',Properties:{delay:'3',facing:'west',locked:'false',powered:'false'}}",
			"{Name:'minecraft:unpowered_repeater',Properties:{delay:'3',facing:'west',locked:'false'}}",
			"{Name:'minecraft:unpowered_repeater',Properties:{delay:'3',facing:'west',locked:'true'}}"
		);
		putStates(
			1498,
			"{Name:'minecraft:repeater',Properties:{delay:'3',facing:'north',locked:'false',powered:'false'}}",
			"{Name:'minecraft:unpowered_repeater',Properties:{delay:'3',facing:'north',locked:'false'}}",
			"{Name:'minecraft:unpowered_repeater',Properties:{delay:'3',facing:'north',locked:'true'}}"
		);
		putStates(
			1499,
			"{Name:'minecraft:repeater',Properties:{delay:'3',facing:'east',locked:'false',powered:'false'}}",
			"{Name:'minecraft:unpowered_repeater',Properties:{delay:'3',facing:'east',locked:'false'}}",
			"{Name:'minecraft:unpowered_repeater',Properties:{delay:'3',facing:'east',locked:'true'}}"
		);
		putStates(
			1500,
			"{Name:'minecraft:repeater',Properties:{delay:'4',facing:'south',locked:'false',powered:'false'}}",
			"{Name:'minecraft:unpowered_repeater',Properties:{delay:'4',facing:'south',locked:'false'}}",
			"{Name:'minecraft:unpowered_repeater',Properties:{delay:'4',facing:'south',locked:'true'}}"
		);
		putStates(
			1501,
			"{Name:'minecraft:repeater',Properties:{delay:'4',facing:'west',locked:'false',powered:'false'}}",
			"{Name:'minecraft:unpowered_repeater',Properties:{delay:'4',facing:'west',locked:'false'}}",
			"{Name:'minecraft:unpowered_repeater',Properties:{delay:'4',facing:'west',locked:'true'}}"
		);
		putStates(
			1502,
			"{Name:'minecraft:repeater',Properties:{delay:'4',facing:'north',locked:'false',powered:'false'}}",
			"{Name:'minecraft:unpowered_repeater',Properties:{delay:'4',facing:'north',locked:'false'}}",
			"{Name:'minecraft:unpowered_repeater',Properties:{delay:'4',facing:'north',locked:'true'}}"
		);
		putStates(
			1503,
			"{Name:'minecraft:repeater',Properties:{delay:'4',facing:'east',locked:'false',powered:'false'}}",
			"{Name:'minecraft:unpowered_repeater',Properties:{delay:'4',facing:'east',locked:'false'}}",
			"{Name:'minecraft:unpowered_repeater',Properties:{delay:'4',facing:'east',locked:'true'}}"
		);
		putStates(
			1504,
			"{Name:'minecraft:repeater',Properties:{delay:'1',facing:'south',locked:'false',powered:'true'}}",
			"{Name:'minecraft:powered_repeater',Properties:{delay:'1',facing:'south',locked:'false'}}",
			"{Name:'minecraft:powered_repeater',Properties:{delay:'1',facing:'south',locked:'true'}}"
		);
		putStates(
			1505,
			"{Name:'minecraft:repeater',Properties:{delay:'1',facing:'west',locked:'false',powered:'true'}}",
			"{Name:'minecraft:powered_repeater',Properties:{delay:'1',facing:'west',locked:'false'}}",
			"{Name:'minecraft:powered_repeater',Properties:{delay:'1',facing:'west',locked:'true'}}"
		);
		putStates(
			1506,
			"{Name:'minecraft:repeater',Properties:{delay:'1',facing:'north',locked:'false',powered:'true'}}",
			"{Name:'minecraft:powered_repeater',Properties:{delay:'1',facing:'north',locked:'false'}}",
			"{Name:'minecraft:powered_repeater',Properties:{delay:'1',facing:'north',locked:'true'}}"
		);
		putStates(
			1507,
			"{Name:'minecraft:repeater',Properties:{delay:'1',facing:'east',locked:'false',powered:'true'}}",
			"{Name:'minecraft:powered_repeater',Properties:{delay:'1',facing:'east',locked:'false'}}",
			"{Name:'minecraft:powered_repeater',Properties:{delay:'1',facing:'east',locked:'true'}}"
		);
		putStates(
			1508,
			"{Name:'minecraft:repeater',Properties:{delay:'2',facing:'south',locked:'false',powered:'true'}}",
			"{Name:'minecraft:powered_repeater',Properties:{delay:'2',facing:'south',locked:'false'}}",
			"{Name:'minecraft:powered_repeater',Properties:{delay:'2',facing:'south',locked:'true'}}"
		);
		putStates(
			1509,
			"{Name:'minecraft:repeater',Properties:{delay:'2',facing:'west',locked:'false',powered:'true'}}",
			"{Name:'minecraft:powered_repeater',Properties:{delay:'2',facing:'west',locked:'false'}}",
			"{Name:'minecraft:powered_repeater',Properties:{delay:'2',facing:'west',locked:'true'}}"
		);
		putStates(
			1510,
			"{Name:'minecraft:repeater',Properties:{delay:'2',facing:'north',locked:'false',powered:'true'}}",
			"{Name:'minecraft:powered_repeater',Properties:{delay:'2',facing:'north',locked:'false'}}",
			"{Name:'minecraft:powered_repeater',Properties:{delay:'2',facing:'north',locked:'true'}}"
		);
		putStates(
			1511,
			"{Name:'minecraft:repeater',Properties:{delay:'2',facing:'east',locked:'false',powered:'true'}}",
			"{Name:'minecraft:powered_repeater',Properties:{delay:'2',facing:'east',locked:'false'}}",
			"{Name:'minecraft:powered_repeater',Properties:{delay:'2',facing:'east',locked:'true'}}"
		);
		putStates(
			1512,
			"{Name:'minecraft:repeater',Properties:{delay:'3',facing:'south',locked:'false',powered:'true'}}",
			"{Name:'minecraft:powered_repeater',Properties:{delay:'3',facing:'south',locked:'false'}}",
			"{Name:'minecraft:powered_repeater',Properties:{delay:'3',facing:'south',locked:'true'}}"
		);
		putStates(
			1513,
			"{Name:'minecraft:repeater',Properties:{delay:'3',facing:'west',locked:'false',powered:'true'}}",
			"{Name:'minecraft:powered_repeater',Properties:{delay:'3',facing:'west',locked:'false'}}",
			"{Name:'minecraft:powered_repeater',Properties:{delay:'3',facing:'west',locked:'true'}}"
		);
		putStates(
			1514,
			"{Name:'minecraft:repeater',Properties:{delay:'3',facing:'north',locked:'false',powered:'true'}}",
			"{Name:'minecraft:powered_repeater',Properties:{delay:'3',facing:'north',locked:'false'}}",
			"{Name:'minecraft:powered_repeater',Properties:{delay:'3',facing:'north',locked:'true'}}"
		);
		putStates(
			1515,
			"{Name:'minecraft:repeater',Properties:{delay:'3',facing:'east',locked:'false',powered:'true'}}",
			"{Name:'minecraft:powered_repeater',Properties:{delay:'3',facing:'east',locked:'false'}}",
			"{Name:'minecraft:powered_repeater',Properties:{delay:'3',facing:'east',locked:'true'}}"
		);
		putStates(
			1516,
			"{Name:'minecraft:repeater',Properties:{delay:'4',facing:'south',locked:'false',powered:'true'}}",
			"{Name:'minecraft:powered_repeater',Properties:{delay:'4',facing:'south',locked:'false'}}",
			"{Name:'minecraft:powered_repeater',Properties:{delay:'4',facing:'south',locked:'true'}}"
		);
		putStates(
			1517,
			"{Name:'minecraft:repeater',Properties:{delay:'4',facing:'west',locked:'false',powered:'true'}}",
			"{Name:'minecraft:powered_repeater',Properties:{delay:'4',facing:'west',locked:'false'}}",
			"{Name:'minecraft:powered_repeater',Properties:{delay:'4',facing:'west',locked:'true'}}"
		);
		putStates(
			1518,
			"{Name:'minecraft:repeater',Properties:{delay:'4',facing:'north',locked:'false',powered:'true'}}",
			"{Name:'minecraft:powered_repeater',Properties:{delay:'4',facing:'north',locked:'false'}}",
			"{Name:'minecraft:powered_repeater',Properties:{delay:'4',facing:'north',locked:'true'}}"
		);
		putStates(
			1519,
			"{Name:'minecraft:repeater',Properties:{delay:'4',facing:'east',locked:'false',powered:'true'}}",
			"{Name:'minecraft:powered_repeater',Properties:{delay:'4',facing:'east',locked:'false'}}",
			"{Name:'minecraft:powered_repeater',Properties:{delay:'4',facing:'east',locked:'true'}}"
		);
		putStates(1520, "{Name:'minecraft:white_stained_glass'}", "{Name:'minecraft:stained_glass',Properties:{color:'white'}}");
		putStates(1521, "{Name:'minecraft:orange_stained_glass'}", "{Name:'minecraft:stained_glass',Properties:{color:'orange'}}");
		putStates(1522, "{Name:'minecraft:magenta_stained_glass'}", "{Name:'minecraft:stained_glass',Properties:{color:'magenta'}}");
		putStates(1523, "{Name:'minecraft:light_blue_stained_glass'}", "{Name:'minecraft:stained_glass',Properties:{color:'light_blue'}}");
		putStates(1524, "{Name:'minecraft:yellow_stained_glass'}", "{Name:'minecraft:stained_glass',Properties:{color:'yellow'}}");
		putStates(1525, "{Name:'minecraft:lime_stained_glass'}", "{Name:'minecraft:stained_glass',Properties:{color:'lime'}}");
		putStates(1526, "{Name:'minecraft:pink_stained_glass'}", "{Name:'minecraft:stained_glass',Properties:{color:'pink'}}");
		putStates(1527, "{Name:'minecraft:gray_stained_glass'}", "{Name:'minecraft:stained_glass',Properties:{color:'gray'}}");
		putStates(1528, "{Name:'minecraft:light_gray_stained_glass'}", "{Name:'minecraft:stained_glass',Properties:{color:'silver'}}");
		putStates(1529, "{Name:'minecraft:cyan_stained_glass'}", "{Name:'minecraft:stained_glass',Properties:{color:'cyan'}}");
		putStates(1530, "{Name:'minecraft:purple_stained_glass'}", "{Name:'minecraft:stained_glass',Properties:{color:'purple'}}");
		putStates(1531, "{Name:'minecraft:blue_stained_glass'}", "{Name:'minecraft:stained_glass',Properties:{color:'blue'}}");
		putStates(1532, "{Name:'minecraft:brown_stained_glass'}", "{Name:'minecraft:stained_glass',Properties:{color:'brown'}}");
		putStates(1533, "{Name:'minecraft:green_stained_glass'}", "{Name:'minecraft:stained_glass',Properties:{color:'green'}}");
		putStates(1534, "{Name:'minecraft:red_stained_glass'}", "{Name:'minecraft:stained_glass',Properties:{color:'red'}}");
		putStates(1535, "{Name:'minecraft:black_stained_glass'}", "{Name:'minecraft:stained_glass',Properties:{color:'black'}}");
	}

	/**
	 * Adds states to flatten from the blocks which had numeric IDs between 96 and 111 before 1.13.
	 */
	static void putStatesFromBlocks96To111() {
		putStates(
			1536,
			"{Name:'minecraft:oak_trapdoor',Properties:{facing:'north',half:'bottom',open:'false'}}",
			"{Name:'minecraft:trapdoor',Properties:{facing:'north',half:'bottom',open:'false'}}"
		);
		putStates(
			1537,
			"{Name:'minecraft:oak_trapdoor',Properties:{facing:'south',half:'bottom',open:'false'}}",
			"{Name:'minecraft:trapdoor',Properties:{facing:'south',half:'bottom',open:'false'}}"
		);
		putStates(
			1538,
			"{Name:'minecraft:oak_trapdoor',Properties:{facing:'west',half:'bottom',open:'false'}}",
			"{Name:'minecraft:trapdoor',Properties:{facing:'west',half:'bottom',open:'false'}}"
		);
		putStates(
			1539,
			"{Name:'minecraft:oak_trapdoor',Properties:{facing:'east',half:'bottom',open:'false'}}",
			"{Name:'minecraft:trapdoor',Properties:{facing:'east',half:'bottom',open:'false'}}"
		);
		putStates(
			1540,
			"{Name:'minecraft:oak_trapdoor',Properties:{facing:'north',half:'bottom',open:'true'}}",
			"{Name:'minecraft:trapdoor',Properties:{facing:'north',half:'bottom',open:'true'}}"
		);
		putStates(
			1541,
			"{Name:'minecraft:oak_trapdoor',Properties:{facing:'south',half:'bottom',open:'true'}}",
			"{Name:'minecraft:trapdoor',Properties:{facing:'south',half:'bottom',open:'true'}}"
		);
		putStates(
			1542,
			"{Name:'minecraft:oak_trapdoor',Properties:{facing:'west',half:'bottom',open:'true'}}",
			"{Name:'minecraft:trapdoor',Properties:{facing:'west',half:'bottom',open:'true'}}"
		);
		putStates(
			1543,
			"{Name:'minecraft:oak_trapdoor',Properties:{facing:'east',half:'bottom',open:'true'}}",
			"{Name:'minecraft:trapdoor',Properties:{facing:'east',half:'bottom',open:'true'}}"
		);
		putStates(
			1544,
			"{Name:'minecraft:oak_trapdoor',Properties:{facing:'north',half:'top',open:'false'}}",
			"{Name:'minecraft:trapdoor',Properties:{facing:'north',half:'top',open:'false'}}"
		);
		putStates(
			1545,
			"{Name:'minecraft:oak_trapdoor',Properties:{facing:'south',half:'top',open:'false'}}",
			"{Name:'minecraft:trapdoor',Properties:{facing:'south',half:'top',open:'false'}}"
		);
		putStates(
			1546,
			"{Name:'minecraft:oak_trapdoor',Properties:{facing:'west',half:'top',open:'false'}}",
			"{Name:'minecraft:trapdoor',Properties:{facing:'west',half:'top',open:'false'}}"
		);
		putStates(
			1547,
			"{Name:'minecraft:oak_trapdoor',Properties:{facing:'east',half:'top',open:'false'}}",
			"{Name:'minecraft:trapdoor',Properties:{facing:'east',half:'top',open:'false'}}"
		);
		putStates(
			1548,
			"{Name:'minecraft:oak_trapdoor',Properties:{facing:'north',half:'top',open:'true'}}",
			"{Name:'minecraft:trapdoor',Properties:{facing:'north',half:'top',open:'true'}}"
		);
		putStates(
			1549,
			"{Name:'minecraft:oak_trapdoor',Properties:{facing:'south',half:'top',open:'true'}}",
			"{Name:'minecraft:trapdoor',Properties:{facing:'south',half:'top',open:'true'}}"
		);
		putStates(
			1550,
			"{Name:'minecraft:oak_trapdoor',Properties:{facing:'west',half:'top',open:'true'}}",
			"{Name:'minecraft:trapdoor',Properties:{facing:'west',half:'top',open:'true'}}"
		);
		putStates(
			1551,
			"{Name:'minecraft:oak_trapdoor',Properties:{facing:'east',half:'top',open:'true'}}",
			"{Name:'minecraft:trapdoor',Properties:{facing:'east',half:'top',open:'true'}}"
		);
		putStates(1552, "{Name:'minecraft:infested_stone'}", "{Name:'minecraft:monster_egg',Properties:{variant:'stone'}}");
		putStates(1553, "{Name:'minecraft:infested_cobblestone'}", "{Name:'minecraft:monster_egg',Properties:{variant:'cobblestone'}}");
		putStates(1554, "{Name:'minecraft:infested_stone_bricks'}", "{Name:'minecraft:monster_egg',Properties:{variant:'stone_brick'}}");
		putStates(1555, "{Name:'minecraft:infested_mossy_stone_bricks'}", "{Name:'minecraft:monster_egg',Properties:{variant:'mossy_brick'}}");
		putStates(1556, "{Name:'minecraft:infested_cracked_stone_bricks'}", "{Name:'minecraft:monster_egg',Properties:{variant:'cracked_brick'}}");
		putStates(1557, "{Name:'minecraft:infested_chiseled_stone_bricks'}", "{Name:'minecraft:monster_egg',Properties:{variant:'chiseled_brick'}}");
		putStates(1568, "{Name:'minecraft:stone_bricks'}", "{Name:'minecraft:stonebrick',Properties:{variant:'stonebrick'}}");
		putStates(1569, "{Name:'minecraft:mossy_stone_bricks'}", "{Name:'minecraft:stonebrick',Properties:{variant:'mossy_stonebrick'}}");
		putStates(1570, "{Name:'minecraft:cracked_stone_bricks'}", "{Name:'minecraft:stonebrick',Properties:{variant:'cracked_stonebrick'}}");
		putStates(1571, "{Name:'minecraft:chiseled_stone_bricks'}", "{Name:'minecraft:stonebrick',Properties:{variant:'chiseled_stonebrick'}}");
		putStates(
			1584,
			"{Name:'minecraft:brown_mushroom_block',Properties:{north:'false',east:'false',south:'false',west:'false',up:'false',down:'false'}}",
			"{Name:'minecraft:brown_mushroom_block',Properties:{variant:'all_inside'}}"
		);
		putStates(
			1585,
			"{Name:'minecraft:brown_mushroom_block',Properties:{north:'true',east:'false',south:'false',west:'true',up:'true',down:'false'}}",
			"{Name:'minecraft:brown_mushroom_block',Properties:{variant:'north_west'}}"
		);
		putStates(
			1586,
			"{Name:'minecraft:brown_mushroom_block',Properties:{north:'true',east:'false',south:'false',west:'false',up:'true',down:'false'}}",
			"{Name:'minecraft:brown_mushroom_block',Properties:{variant:'north'}}"
		);
		putStates(
			1587,
			"{Name:'minecraft:brown_mushroom_block',Properties:{north:'true',east:'true',south:'false',west:'false',up:'true',down:'false'}}",
			"{Name:'minecraft:brown_mushroom_block',Properties:{variant:'north_east'}}"
		);
		putStates(
			1588,
			"{Name:'minecraft:brown_mushroom_block',Properties:{north:'false',east:'false',south:'false',west:'true',up:'true',down:'false'}}",
			"{Name:'minecraft:brown_mushroom_block',Properties:{variant:'west'}}"
		);
		putStates(
			1589,
			"{Name:'minecraft:brown_mushroom_block',Properties:{north:'false',east:'false',south:'false',west:'false',up:'true',down:'false'}}",
			"{Name:'minecraft:brown_mushroom_block',Properties:{variant:'center'}}"
		);
		putStates(
			1590,
			"{Name:'minecraft:brown_mushroom_block',Properties:{north:'false',east:'true',south:'false',west:'false',up:'true',down:'false'}}",
			"{Name:'minecraft:brown_mushroom_block',Properties:{variant:'east'}}"
		);
		putStates(
			1591,
			"{Name:'minecraft:brown_mushroom_block',Properties:{north:'false',east:'false',south:'true',west:'true',up:'true',down:'false'}}",
			"{Name:'minecraft:brown_mushroom_block',Properties:{variant:'south_west'}}"
		);
		putStates(
			1592,
			"{Name:'minecraft:brown_mushroom_block',Properties:{north:'false',east:'false',south:'true',west:'false',up:'true',down:'false'}}",
			"{Name:'minecraft:brown_mushroom_block',Properties:{variant:'south'}}"
		);
		putStates(
			1593,
			"{Name:'minecraft:brown_mushroom_block',Properties:{north:'false',east:'true',south:'true',west:'false',up:'true',down:'false'}}",
			"{Name:'minecraft:brown_mushroom_block',Properties:{variant:'south_east'}}"
		);
		putStates(
			1594,
			"{Name:'minecraft:mushroom_stem',Properties:{north:'true',east:'true',south:'true',west:'true',up:'false',down:'false'}}",
			"{Name:'minecraft:brown_mushroom_block',Properties:{variant:'stem'}}"
		);
		putStates(1595, "{Name:'minecraft:brown_mushroom_block',Properties:{north:'false',east:'false',south:'false',west:'false',up:'false',down:'false'}}");
		putStates(1596, "{Name:'minecraft:brown_mushroom_block',Properties:{north:'false',east:'false',south:'false',west:'false',up:'false',down:'false'}}");
		putStates(1597, "{Name:'minecraft:brown_mushroom_block',Properties:{north:'false',east:'false',south:'false',west:'false',up:'false',down:'false'}}");
		putStates(
			1598,
			"{Name:'minecraft:brown_mushroom_block',Properties:{north:'true',east:'true',south:'true',west:'true',up:'true',down:'true'}}",
			"{Name:'minecraft:brown_mushroom_block',Properties:{variant:'all_outside'}}"
		);
		putStates(
			1599,
			"{Name:'minecraft:mushroom_stem',Properties:{north:'true',east:'true',south:'true',west:'true',up:'true',down:'true'}}",
			"{Name:'minecraft:brown_mushroom_block',Properties:{variant:'all_stem'}}"
		);
		putStates(
			1600,
			"{Name:'minecraft:red_mushroom_block',Properties:{north:'false',east:'false',south:'false',west:'false',up:'false',down:'false'}}",
			"{Name:'minecraft:red_mushroom_block',Properties:{variant:'all_inside'}}"
		);
		putStates(
			1601,
			"{Name:'minecraft:red_mushroom_block',Properties:{north:'true',east:'false',south:'false',west:'true',up:'true',down:'false'}}",
			"{Name:'minecraft:red_mushroom_block',Properties:{variant:'north_west'}}"
		);
		putStates(
			1602,
			"{Name:'minecraft:red_mushroom_block',Properties:{north:'true',east:'false',south:'false',west:'false',up:'true',down:'false'}}",
			"{Name:'minecraft:red_mushroom_block',Properties:{variant:'north'}}"
		);
		putStates(
			1603,
			"{Name:'minecraft:red_mushroom_block',Properties:{north:'true',east:'true',south:'false',west:'false',up:'true',down:'false'}}",
			"{Name:'minecraft:red_mushroom_block',Properties:{variant:'north_east'}}"
		);
		putStates(
			1604,
			"{Name:'minecraft:red_mushroom_block',Properties:{north:'false',east:'false',south:'false',west:'true',up:'true',down:'false'}}",
			"{Name:'minecraft:red_mushroom_block',Properties:{variant:'west'}}"
		);
		putStates(
			1605,
			"{Name:'minecraft:red_mushroom_block',Properties:{north:'false',east:'false',south:'false',west:'false',up:'true',down:'false'}}",
			"{Name:'minecraft:red_mushroom_block',Properties:{variant:'center'}}"
		);
		putStates(
			1606,
			"{Name:'minecraft:red_mushroom_block',Properties:{north:'false',east:'true',south:'false',west:'false',up:'true',down:'false'}}",
			"{Name:'minecraft:red_mushroom_block',Properties:{variant:'east'}}"
		);
		putStates(
			1607,
			"{Name:'minecraft:red_mushroom_block',Properties:{north:'false',east:'false',south:'true',west:'true',up:'true',down:'false'}}",
			"{Name:'minecraft:red_mushroom_block',Properties:{variant:'south_west'}}"
		);
		putStates(
			1608,
			"{Name:'minecraft:red_mushroom_block',Properties:{north:'false',east:'false',south:'true',west:'false',up:'true',down:'false'}}",
			"{Name:'minecraft:red_mushroom_block',Properties:{variant:'south'}}"
		);
		putStates(
			1609,
			"{Name:'minecraft:red_mushroom_block',Properties:{north:'false',east:'true',south:'true',west:'false',up:'true',down:'false'}}",
			"{Name:'minecraft:red_mushroom_block',Properties:{variant:'south_east'}}"
		);
		putStates(
			1610,
			"{Name:'minecraft:mushroom_stem',Properties:{north:'true',east:'true',south:'true',west:'true',up:'false',down:'false'}}",
			"{Name:'minecraft:red_mushroom_block',Properties:{variant:'stem'}}"
		);
		putStates(1611, "{Name:'minecraft:red_mushroom_block',Properties:{north:'false',east:'false',south:'false',west:'false',up:'false',down:'false'}}");
		putStates(1612, "{Name:'minecraft:red_mushroom_block',Properties:{north:'false',east:'false',south:'false',west:'false',up:'false',down:'false'}}");
		putStates(1613, "{Name:'minecraft:red_mushroom_block',Properties:{north:'false',east:'false',south:'false',west:'false',up:'false',down:'false'}}");
		putStates(
			1614,
			"{Name:'minecraft:red_mushroom_block',Properties:{north:'true',east:'true',south:'true',west:'true',up:'true',down:'true'}}",
			"{Name:'minecraft:red_mushroom_block',Properties:{variant:'all_outside'}}"
		);
		putStates(
			1615,
			"{Name:'minecraft:mushroom_stem',Properties:{north:'true',east:'true',south:'true',west:'true',up:'true',down:'true'}}",
			"{Name:'minecraft:red_mushroom_block',Properties:{variant:'all_stem'}}"
		);
		putStates(
			1616,
			"{Name:'minecraft:iron_bars',Properties:{east:'false',north:'false',south:'false',west:'false'}}",
			"{Name:'minecraft:iron_bars',Properties:{east:'false',north:'false',south:'false',west:'false'}}",
			"{Name:'minecraft:iron_bars',Properties:{east:'false',north:'false',south:'false',west:'true'}}",
			"{Name:'minecraft:iron_bars',Properties:{east:'false',north:'false',south:'true',west:'false'}}",
			"{Name:'minecraft:iron_bars',Properties:{east:'false',north:'false',south:'true',west:'true'}}",
			"{Name:'minecraft:iron_bars',Properties:{east:'false',north:'true',south:'false',west:'false'}}",
			"{Name:'minecraft:iron_bars',Properties:{east:'false',north:'true',south:'false',west:'true'}}",
			"{Name:'minecraft:iron_bars',Properties:{east:'false',north:'true',south:'true',west:'false'}}",
			"{Name:'minecraft:iron_bars',Properties:{east:'false',north:'true',south:'true',west:'true'}}",
			"{Name:'minecraft:iron_bars',Properties:{east:'true',north:'false',south:'false',west:'false'}}",
			"{Name:'minecraft:iron_bars',Properties:{east:'true',north:'false',south:'false',west:'true'}}",
			"{Name:'minecraft:iron_bars',Properties:{east:'true',north:'false',south:'true',west:'false'}}",
			"{Name:'minecraft:iron_bars',Properties:{east:'true',north:'false',south:'true',west:'true'}}",
			"{Name:'minecraft:iron_bars',Properties:{east:'true',north:'true',south:'false',west:'false'}}",
			"{Name:'minecraft:iron_bars',Properties:{east:'true',north:'true',south:'false',west:'true'}}",
			"{Name:'minecraft:iron_bars',Properties:{east:'true',north:'true',south:'true',west:'false'}}",
			"{Name:'minecraft:iron_bars',Properties:{east:'true',north:'true',south:'true',west:'true'}}"
		);
		putStates(
			1632,
			"{Name:'minecraft:glass_pane',Properties:{east:'false',north:'false',south:'false',west:'false'}}",
			"{Name:'minecraft:glass_pane',Properties:{east:'false',north:'false',south:'false',west:'false'}}",
			"{Name:'minecraft:glass_pane',Properties:{east:'false',north:'false',south:'false',west:'true'}}",
			"{Name:'minecraft:glass_pane',Properties:{east:'false',north:'false',south:'true',west:'false'}}",
			"{Name:'minecraft:glass_pane',Properties:{east:'false',north:'false',south:'true',west:'true'}}",
			"{Name:'minecraft:glass_pane',Properties:{east:'false',north:'true',south:'false',west:'false'}}",
			"{Name:'minecraft:glass_pane',Properties:{east:'false',north:'true',south:'false',west:'true'}}",
			"{Name:'minecraft:glass_pane',Properties:{east:'false',north:'true',south:'true',west:'false'}}",
			"{Name:'minecraft:glass_pane',Properties:{east:'false',north:'true',south:'true',west:'true'}}",
			"{Name:'minecraft:glass_pane',Properties:{east:'true',north:'false',south:'false',west:'false'}}",
			"{Name:'minecraft:glass_pane',Properties:{east:'true',north:'false',south:'false',west:'true'}}",
			"{Name:'minecraft:glass_pane',Properties:{east:'true',north:'false',south:'true',west:'false'}}",
			"{Name:'minecraft:glass_pane',Properties:{east:'true',north:'false',south:'true',west:'true'}}",
			"{Name:'minecraft:glass_pane',Properties:{east:'true',north:'true',south:'false',west:'false'}}",
			"{Name:'minecraft:glass_pane',Properties:{east:'true',north:'true',south:'false',west:'true'}}",
			"{Name:'minecraft:glass_pane',Properties:{east:'true',north:'true',south:'true',west:'false'}}",
			"{Name:'minecraft:glass_pane',Properties:{east:'true',north:'true',south:'true',west:'true'}}"
		);
		putStates(1648, "{Name:'minecraft:melon_block'}", "{Name:'minecraft:melon_block'}");
		putStates(
			1664,
			"{Name:'minecraft:pumpkin_stem',Properties:{age:'0'}}",
			"{Name:'minecraft:pumpkin_stem',Properties:{age:'0',facing:'east'}}",
			"{Name:'minecraft:pumpkin_stem',Properties:{age:'0',facing:'north'}}",
			"{Name:'minecraft:pumpkin_stem',Properties:{age:'0',facing:'south'}}",
			"{Name:'minecraft:pumpkin_stem',Properties:{age:'0',facing:'up'}}",
			"{Name:'minecraft:pumpkin_stem',Properties:{age:'0',facing:'west'}}"
		);
		putStates(
			1665,
			"{Name:'minecraft:pumpkin_stem',Properties:{age:'1'}}",
			"{Name:'minecraft:pumpkin_stem',Properties:{age:'1',facing:'east'}}",
			"{Name:'minecraft:pumpkin_stem',Properties:{age:'1',facing:'north'}}",
			"{Name:'minecraft:pumpkin_stem',Properties:{age:'1',facing:'south'}}",
			"{Name:'minecraft:pumpkin_stem',Properties:{age:'1',facing:'up'}}",
			"{Name:'minecraft:pumpkin_stem',Properties:{age:'1',facing:'west'}}"
		);
		putStates(
			1666,
			"{Name:'minecraft:pumpkin_stem',Properties:{age:'2'}}",
			"{Name:'minecraft:pumpkin_stem',Properties:{age:'2',facing:'east'}}",
			"{Name:'minecraft:pumpkin_stem',Properties:{age:'2',facing:'north'}}",
			"{Name:'minecraft:pumpkin_stem',Properties:{age:'2',facing:'south'}}",
			"{Name:'minecraft:pumpkin_stem',Properties:{age:'2',facing:'up'}}",
			"{Name:'minecraft:pumpkin_stem',Properties:{age:'2',facing:'west'}}"
		);
		putStates(
			1667,
			"{Name:'minecraft:pumpkin_stem',Properties:{age:'3'}}",
			"{Name:'minecraft:pumpkin_stem',Properties:{age:'3',facing:'east'}}",
			"{Name:'minecraft:pumpkin_stem',Properties:{age:'3',facing:'north'}}",
			"{Name:'minecraft:pumpkin_stem',Properties:{age:'3',facing:'south'}}",
			"{Name:'minecraft:pumpkin_stem',Properties:{age:'3',facing:'up'}}",
			"{Name:'minecraft:pumpkin_stem',Properties:{age:'3',facing:'west'}}"
		);
		putStates(
			1668,
			"{Name:'minecraft:pumpkin_stem',Properties:{age:'4'}}",
			"{Name:'minecraft:pumpkin_stem',Properties:{age:'4',facing:'east'}}",
			"{Name:'minecraft:pumpkin_stem',Properties:{age:'4',facing:'north'}}",
			"{Name:'minecraft:pumpkin_stem',Properties:{age:'4',facing:'south'}}",
			"{Name:'minecraft:pumpkin_stem',Properties:{age:'4',facing:'up'}}",
			"{Name:'minecraft:pumpkin_stem',Properties:{age:'4',facing:'west'}}"
		);
		putStates(
			1669,
			"{Name:'minecraft:pumpkin_stem',Properties:{age:'5'}}",
			"{Name:'minecraft:pumpkin_stem',Properties:{age:'5',facing:'east'}}",
			"{Name:'minecraft:pumpkin_stem',Properties:{age:'5',facing:'north'}}",
			"{Name:'minecraft:pumpkin_stem',Properties:{age:'5',facing:'south'}}",
			"{Name:'minecraft:pumpkin_stem',Properties:{age:'5',facing:'up'}}",
			"{Name:'minecraft:pumpkin_stem',Properties:{age:'5',facing:'west'}}"
		);
		putStates(
			1670,
			"{Name:'minecraft:pumpkin_stem',Properties:{age:'6'}}",
			"{Name:'minecraft:pumpkin_stem',Properties:{age:'6',facing:'east'}}",
			"{Name:'minecraft:pumpkin_stem',Properties:{age:'6',facing:'north'}}",
			"{Name:'minecraft:pumpkin_stem',Properties:{age:'6',facing:'south'}}",
			"{Name:'minecraft:pumpkin_stem',Properties:{age:'6',facing:'up'}}",
			"{Name:'minecraft:pumpkin_stem',Properties:{age:'6',facing:'west'}}"
		);
		putStates(
			1671,
			"{Name:'minecraft:pumpkin_stem',Properties:{age:'7'}}",
			"{Name:'minecraft:pumpkin_stem',Properties:{age:'7',facing:'east'}}",
			"{Name:'minecraft:pumpkin_stem',Properties:{age:'7',facing:'north'}}",
			"{Name:'minecraft:pumpkin_stem',Properties:{age:'7',facing:'south'}}",
			"{Name:'minecraft:pumpkin_stem',Properties:{age:'7',facing:'up'}}",
			"{Name:'minecraft:pumpkin_stem',Properties:{age:'7',facing:'west'}}"
		);
		putStates(
			1680,
			"{Name:'minecraft:melon_stem',Properties:{age:'0'}}",
			"{Name:'minecraft:melon_stem',Properties:{age:'0',facing:'east'}}",
			"{Name:'minecraft:melon_stem',Properties:{age:'0',facing:'north'}}",
			"{Name:'minecraft:melon_stem',Properties:{age:'0',facing:'south'}}",
			"{Name:'minecraft:melon_stem',Properties:{age:'0',facing:'up'}}",
			"{Name:'minecraft:melon_stem',Properties:{age:'0',facing:'west'}}"
		);
		putStates(
			1681,
			"{Name:'minecraft:melon_stem',Properties:{age:'1'}}",
			"{Name:'minecraft:melon_stem',Properties:{age:'1',facing:'east'}}",
			"{Name:'minecraft:melon_stem',Properties:{age:'1',facing:'north'}}",
			"{Name:'minecraft:melon_stem',Properties:{age:'1',facing:'south'}}",
			"{Name:'minecraft:melon_stem',Properties:{age:'1',facing:'up'}}",
			"{Name:'minecraft:melon_stem',Properties:{age:'1',facing:'west'}}"
		);
		putStates(
			1682,
			"{Name:'minecraft:melon_stem',Properties:{age:'2'}}",
			"{Name:'minecraft:melon_stem',Properties:{age:'2',facing:'east'}}",
			"{Name:'minecraft:melon_stem',Properties:{age:'2',facing:'north'}}",
			"{Name:'minecraft:melon_stem',Properties:{age:'2',facing:'south'}}",
			"{Name:'minecraft:melon_stem',Properties:{age:'2',facing:'up'}}",
			"{Name:'minecraft:melon_stem',Properties:{age:'2',facing:'west'}}"
		);
		putStates(
			1683,
			"{Name:'minecraft:melon_stem',Properties:{age:'3'}}",
			"{Name:'minecraft:melon_stem',Properties:{age:'3',facing:'east'}}",
			"{Name:'minecraft:melon_stem',Properties:{age:'3',facing:'north'}}",
			"{Name:'minecraft:melon_stem',Properties:{age:'3',facing:'south'}}",
			"{Name:'minecraft:melon_stem',Properties:{age:'3',facing:'up'}}",
			"{Name:'minecraft:melon_stem',Properties:{age:'3',facing:'west'}}"
		);
		putStates(
			1684,
			"{Name:'minecraft:melon_stem',Properties:{age:'4'}}",
			"{Name:'minecraft:melon_stem',Properties:{age:'4',facing:'east'}}",
			"{Name:'minecraft:melon_stem',Properties:{age:'4',facing:'north'}}",
			"{Name:'minecraft:melon_stem',Properties:{age:'4',facing:'south'}}",
			"{Name:'minecraft:melon_stem',Properties:{age:'4',facing:'up'}}",
			"{Name:'minecraft:melon_stem',Properties:{age:'4',facing:'west'}}"
		);
		putStates(
			1685,
			"{Name:'minecraft:melon_stem',Properties:{age:'5'}}",
			"{Name:'minecraft:melon_stem',Properties:{age:'5',facing:'east'}}",
			"{Name:'minecraft:melon_stem',Properties:{age:'5',facing:'north'}}",
			"{Name:'minecraft:melon_stem',Properties:{age:'5',facing:'south'}}",
			"{Name:'minecraft:melon_stem',Properties:{age:'5',facing:'up'}}",
			"{Name:'minecraft:melon_stem',Properties:{age:'5',facing:'west'}}"
		);
		putStates(
			1686,
			"{Name:'minecraft:melon_stem',Properties:{age:'6'}}",
			"{Name:'minecraft:melon_stem',Properties:{age:'6',facing:'east'}}",
			"{Name:'minecraft:melon_stem',Properties:{age:'6',facing:'north'}}",
			"{Name:'minecraft:melon_stem',Properties:{age:'6',facing:'south'}}",
			"{Name:'minecraft:melon_stem',Properties:{age:'6',facing:'up'}}",
			"{Name:'minecraft:melon_stem',Properties:{age:'6',facing:'west'}}"
		);
		putStates(
			1687,
			"{Name:'minecraft:melon_stem',Properties:{age:'7'}}",
			"{Name:'minecraft:melon_stem',Properties:{age:'7',facing:'east'}}",
			"{Name:'minecraft:melon_stem',Properties:{age:'7',facing:'north'}}",
			"{Name:'minecraft:melon_stem',Properties:{age:'7',facing:'south'}}",
			"{Name:'minecraft:melon_stem',Properties:{age:'7',facing:'up'}}",
			"{Name:'minecraft:melon_stem',Properties:{age:'7',facing:'west'}}"
		);
		putStates(
			1696,
			"{Name:'minecraft:vine',Properties:{east:'false',north:'false',south:'false',up:'true',west:'false'}}",
			"{Name:'minecraft:vine',Properties:{east:'false',north:'false',south:'false',up:'false',west:'false'}}",
			"{Name:'minecraft:vine',Properties:{east:'false',north:'false',south:'false',up:'true',west:'false'}}"
		);
		putStates(
			1697,
			"{Name:'minecraft:vine',Properties:{east:'false',north:'false',south:'true',up:'true',west:'false'}}",
			"{Name:'minecraft:vine',Properties:{east:'false',north:'false',south:'true',up:'false',west:'false'}}",
			"{Name:'minecraft:vine',Properties:{east:'false',north:'false',south:'true',up:'true',west:'false'}}"
		);
		putStates(
			1698,
			"{Name:'minecraft:vine',Properties:{east:'false',north:'false',south:'false',up:'true',west:'true'}}",
			"{Name:'minecraft:vine',Properties:{east:'false',north:'false',south:'false',up:'false',west:'true'}}",
			"{Name:'minecraft:vine',Properties:{east:'false',north:'false',south:'false',up:'true',west:'true'}}"
		);
		putStates(
			1699,
			"{Name:'minecraft:vine',Properties:{east:'false',north:'false',south:'true',up:'true',west:'true'}}",
			"{Name:'minecraft:vine',Properties:{east:'false',north:'false',south:'true',up:'false',west:'true'}}",
			"{Name:'minecraft:vine',Properties:{east:'false',north:'false',south:'true',up:'true',west:'true'}}"
		);
		putStates(
			1700,
			"{Name:'minecraft:vine',Properties:{east:'false',north:'true',south:'false',up:'true',west:'false'}}",
			"{Name:'minecraft:vine',Properties:{east:'false',north:'true',south:'false',up:'false',west:'false'}}",
			"{Name:'minecraft:vine',Properties:{east:'false',north:'true',south:'false',up:'true',west:'false'}}"
		);
		putStates(
			1701,
			"{Name:'minecraft:vine',Properties:{east:'false',north:'true',south:'true',up:'true',west:'false'}}",
			"{Name:'minecraft:vine',Properties:{east:'false',north:'true',south:'true',up:'false',west:'false'}}",
			"{Name:'minecraft:vine',Properties:{east:'false',north:'true',south:'true',up:'true',west:'false'}}"
		);
		putStates(
			1702,
			"{Name:'minecraft:vine',Properties:{east:'false',north:'true',south:'false',up:'true',west:'true'}}",
			"{Name:'minecraft:vine',Properties:{east:'false',north:'true',south:'false',up:'false',west:'true'}}",
			"{Name:'minecraft:vine',Properties:{east:'false',north:'true',south:'false',up:'true',west:'true'}}"
		);
		putStates(
			1703,
			"{Name:'minecraft:vine',Properties:{east:'false',north:'true',south:'true',up:'true',west:'true'}}",
			"{Name:'minecraft:vine',Properties:{east:'false',north:'true',south:'true',up:'false',west:'true'}}",
			"{Name:'minecraft:vine',Properties:{east:'false',north:'true',south:'true',up:'true',west:'true'}}"
		);
		putStates(
			1704,
			"{Name:'minecraft:vine',Properties:{east:'true',north:'false',south:'false',up:'true',west:'false'}}",
			"{Name:'minecraft:vine',Properties:{east:'true',north:'false',south:'false',up:'false',west:'false'}}",
			"{Name:'minecraft:vine',Properties:{east:'true',north:'false',south:'false',up:'true',west:'false'}}"
		);
		putStates(
			1705,
			"{Name:'minecraft:vine',Properties:{east:'true',north:'false',south:'true',up:'true',west:'false'}}",
			"{Name:'minecraft:vine',Properties:{east:'true',north:'false',south:'true',up:'false',west:'false'}}",
			"{Name:'minecraft:vine',Properties:{east:'true',north:'false',south:'true',up:'true',west:'false'}}"
		);
		putStates(
			1706,
			"{Name:'minecraft:vine',Properties:{east:'true',north:'false',south:'false',up:'true',west:'true'}}",
			"{Name:'minecraft:vine',Properties:{east:'true',north:'false',south:'false',up:'false',west:'true'}}",
			"{Name:'minecraft:vine',Properties:{east:'true',north:'false',south:'false',up:'true',west:'true'}}"
		);
		putStates(
			1707,
			"{Name:'minecraft:vine',Properties:{east:'true',north:'false',south:'true',up:'true',west:'true'}}",
			"{Name:'minecraft:vine',Properties:{east:'true',north:'false',south:'true',up:'false',west:'true'}}",
			"{Name:'minecraft:vine',Properties:{east:'true',north:'false',south:'true',up:'true',west:'true'}}"
		);
		putStates(
			1708,
			"{Name:'minecraft:vine',Properties:{east:'true',north:'true',south:'false',up:'true',west:'false'}}",
			"{Name:'minecraft:vine',Properties:{east:'true',north:'true',south:'false',up:'false',west:'false'}}",
			"{Name:'minecraft:vine',Properties:{east:'true',north:'true',south:'false',up:'true',west:'false'}}"
		);
		putStates(
			1709,
			"{Name:'minecraft:vine',Properties:{east:'true',north:'true',south:'true',up:'true',west:'false'}}",
			"{Name:'minecraft:vine',Properties:{east:'true',north:'true',south:'true',up:'false',west:'false'}}",
			"{Name:'minecraft:vine',Properties:{east:'true',north:'true',south:'true',up:'true',west:'false'}}"
		);
		putStates(
			1710,
			"{Name:'minecraft:vine',Properties:{east:'true',north:'true',south:'false',up:'true',west:'true'}}",
			"{Name:'minecraft:vine',Properties:{east:'true',north:'true',south:'false',up:'false',west:'true'}}",
			"{Name:'minecraft:vine',Properties:{east:'true',north:'true',south:'false',up:'true',west:'true'}}"
		);
		putStates(
			1711,
			"{Name:'minecraft:vine',Properties:{east:'true',north:'true',south:'true',up:'true',west:'true'}}",
			"{Name:'minecraft:vine',Properties:{east:'true',north:'true',south:'true',up:'false',west:'true'}}",
			"{Name:'minecraft:vine',Properties:{east:'true',north:'true',south:'true',up:'true',west:'true'}}"
		);
		putStates(
			1712,
			"{Name:'minecraft:oak_fence_gate',Properties:{facing:'south',in_wall:'false',open:'false',powered:'false'}}",
			"{Name:'minecraft:fence_gate',Properties:{facing:'south',in_wall:'false',open:'false',powered:'false'}}",
			"{Name:'minecraft:fence_gate',Properties:{facing:'south',in_wall:'true',open:'false',powered:'false'}}"
		);
		putStates(
			1713,
			"{Name:'minecraft:oak_fence_gate',Properties:{facing:'west',in_wall:'false',open:'false',powered:'false'}}",
			"{Name:'minecraft:fence_gate',Properties:{facing:'west',in_wall:'false',open:'false',powered:'false'}}",
			"{Name:'minecraft:fence_gate',Properties:{facing:'west',in_wall:'true',open:'false',powered:'false'}}"
		);
		putStates(
			1714,
			"{Name:'minecraft:oak_fence_gate',Properties:{facing:'north',in_wall:'false',open:'false',powered:'false'}}",
			"{Name:'minecraft:fence_gate',Properties:{facing:'north',in_wall:'false',open:'false',powered:'false'}}",
			"{Name:'minecraft:fence_gate',Properties:{facing:'north',in_wall:'true',open:'false',powered:'false'}}"
		);
		putStates(
			1715,
			"{Name:'minecraft:oak_fence_gate',Properties:{facing:'east',in_wall:'false',open:'false',powered:'false'}}",
			"{Name:'minecraft:fence_gate',Properties:{facing:'east',in_wall:'false',open:'false',powered:'false'}}",
			"{Name:'minecraft:fence_gate',Properties:{facing:'east',in_wall:'true',open:'false',powered:'false'}}"
		);
		putStates(
			1716,
			"{Name:'minecraft:oak_fence_gate',Properties:{facing:'south',in_wall:'false',open:'true',powered:'false'}}",
			"{Name:'minecraft:fence_gate',Properties:{facing:'south',in_wall:'false',open:'true',powered:'false'}}",
			"{Name:'minecraft:fence_gate',Properties:{facing:'south',in_wall:'true',open:'true',powered:'false'}}"
		);
		putStates(
			1717,
			"{Name:'minecraft:oak_fence_gate',Properties:{facing:'west',in_wall:'false',open:'true',powered:'false'}}",
			"{Name:'minecraft:fence_gate',Properties:{facing:'west',in_wall:'false',open:'true',powered:'false'}}",
			"{Name:'minecraft:fence_gate',Properties:{facing:'west',in_wall:'true',open:'true',powered:'false'}}"
		);
		putStates(
			1718,
			"{Name:'minecraft:oak_fence_gate',Properties:{facing:'north',in_wall:'false',open:'true',powered:'false'}}",
			"{Name:'minecraft:fence_gate',Properties:{facing:'north',in_wall:'false',open:'true',powered:'false'}}",
			"{Name:'minecraft:fence_gate',Properties:{facing:'north',in_wall:'true',open:'true',powered:'false'}}"
		);
		putStates(
			1719,
			"{Name:'minecraft:oak_fence_gate',Properties:{facing:'east',in_wall:'false',open:'true',powered:'false'}}",
			"{Name:'minecraft:fence_gate',Properties:{facing:'east',in_wall:'false',open:'true',powered:'false'}}",
			"{Name:'minecraft:fence_gate',Properties:{facing:'east',in_wall:'true',open:'true',powered:'false'}}"
		);
		putStates(
			1720,
			"{Name:'minecraft:oak_fence_gate',Properties:{facing:'south',in_wall:'false',open:'false',powered:'true'}}",
			"{Name:'minecraft:fence_gate',Properties:{facing:'south',in_wall:'false',open:'false',powered:'true'}}",
			"{Name:'minecraft:fence_gate',Properties:{facing:'south',in_wall:'true',open:'false',powered:'true'}}"
		);
		putStates(
			1721,
			"{Name:'minecraft:oak_fence_gate',Properties:{facing:'west',in_wall:'false',open:'false',powered:'true'}}",
			"{Name:'minecraft:fence_gate',Properties:{facing:'west',in_wall:'false',open:'false',powered:'true'}}",
			"{Name:'minecraft:fence_gate',Properties:{facing:'west',in_wall:'true',open:'false',powered:'true'}}"
		);
		putStates(
			1722,
			"{Name:'minecraft:oak_fence_gate',Properties:{facing:'north',in_wall:'false',open:'false',powered:'true'}}",
			"{Name:'minecraft:fence_gate',Properties:{facing:'north',in_wall:'false',open:'false',powered:'true'}}",
			"{Name:'minecraft:fence_gate',Properties:{facing:'north',in_wall:'true',open:'false',powered:'true'}}"
		);
		putStates(
			1723,
			"{Name:'minecraft:oak_fence_gate',Properties:{facing:'east',in_wall:'false',open:'false',powered:'true'}}",
			"{Name:'minecraft:fence_gate',Properties:{facing:'east',in_wall:'false',open:'false',powered:'true'}}",
			"{Name:'minecraft:fence_gate',Properties:{facing:'east',in_wall:'true',open:'false',powered:'true'}}"
		);
		putStates(
			1724,
			"{Name:'minecraft:oak_fence_gate',Properties:{facing:'south',in_wall:'false',open:'true',powered:'true'}}",
			"{Name:'minecraft:fence_gate',Properties:{facing:'south',in_wall:'false',open:'true',powered:'true'}}",
			"{Name:'minecraft:fence_gate',Properties:{facing:'south',in_wall:'true',open:'true',powered:'true'}}"
		);
		putStates(
			1725,
			"{Name:'minecraft:oak_fence_gate',Properties:{facing:'west',in_wall:'false',open:'true',powered:'true'}}",
			"{Name:'minecraft:fence_gate',Properties:{facing:'west',in_wall:'false',open:'true',powered:'true'}}",
			"{Name:'minecraft:fence_gate',Properties:{facing:'west',in_wall:'true',open:'true',powered:'true'}}"
		);
		putStates(
			1726,
			"{Name:'minecraft:oak_fence_gate',Properties:{facing:'north',in_wall:'false',open:'true',powered:'true'}}",
			"{Name:'minecraft:fence_gate',Properties:{facing:'north',in_wall:'false',open:'true',powered:'true'}}",
			"{Name:'minecraft:fence_gate',Properties:{facing:'north',in_wall:'true',open:'true',powered:'true'}}"
		);
		putStates(
			1727,
			"{Name:'minecraft:oak_fence_gate',Properties:{facing:'east',in_wall:'false',open:'true',powered:'true'}}",
			"{Name:'minecraft:fence_gate',Properties:{facing:'east',in_wall:'false',open:'true',powered:'true'}}",
			"{Name:'minecraft:fence_gate',Properties:{facing:'east',in_wall:'true',open:'true',powered:'true'}}"
		);
		putStates(
			1728,
			"{Name:'minecraft:brick_stairs',Properties:{facing:'east',half:'bottom',shape:'straight'}}",
			"{Name:'minecraft:brick_stairs',Properties:{facing:'east',half:'bottom',shape:'inner_left'}}",
			"{Name:'minecraft:brick_stairs',Properties:{facing:'east',half:'bottom',shape:'inner_right'}}",
			"{Name:'minecraft:brick_stairs',Properties:{facing:'east',half:'bottom',shape:'outer_left'}}",
			"{Name:'minecraft:brick_stairs',Properties:{facing:'east',half:'bottom',shape:'outer_right'}}",
			"{Name:'minecraft:brick_stairs',Properties:{facing:'east',half:'bottom',shape:'straight'}}"
		);
		putStates(
			1729,
			"{Name:'minecraft:brick_stairs',Properties:{facing:'west',half:'bottom',shape:'straight'}}",
			"{Name:'minecraft:brick_stairs',Properties:{facing:'west',half:'bottom',shape:'inner_left'}}",
			"{Name:'minecraft:brick_stairs',Properties:{facing:'west',half:'bottom',shape:'inner_right'}}",
			"{Name:'minecraft:brick_stairs',Properties:{facing:'west',half:'bottom',shape:'outer_left'}}",
			"{Name:'minecraft:brick_stairs',Properties:{facing:'west',half:'bottom',shape:'outer_right'}}",
			"{Name:'minecraft:brick_stairs',Properties:{facing:'west',half:'bottom',shape:'straight'}}"
		);
		putStates(
			1730,
			"{Name:'minecraft:brick_stairs',Properties:{facing:'south',half:'bottom',shape:'straight'}}",
			"{Name:'minecraft:brick_stairs',Properties:{facing:'south',half:'bottom',shape:'inner_left'}}",
			"{Name:'minecraft:brick_stairs',Properties:{facing:'south',half:'bottom',shape:'inner_right'}}",
			"{Name:'minecraft:brick_stairs',Properties:{facing:'south',half:'bottom',shape:'outer_left'}}",
			"{Name:'minecraft:brick_stairs',Properties:{facing:'south',half:'bottom',shape:'outer_right'}}",
			"{Name:'minecraft:brick_stairs',Properties:{facing:'south',half:'bottom',shape:'straight'}}"
		);
		putStates(
			1731,
			"{Name:'minecraft:brick_stairs',Properties:{facing:'north',half:'bottom',shape:'straight'}}",
			"{Name:'minecraft:brick_stairs',Properties:{facing:'north',half:'bottom',shape:'inner_left'}}",
			"{Name:'minecraft:brick_stairs',Properties:{facing:'north',half:'bottom',shape:'inner_right'}}",
			"{Name:'minecraft:brick_stairs',Properties:{facing:'north',half:'bottom',shape:'outer_left'}}",
			"{Name:'minecraft:brick_stairs',Properties:{facing:'north',half:'bottom',shape:'outer_right'}}",
			"{Name:'minecraft:brick_stairs',Properties:{facing:'north',half:'bottom',shape:'straight'}}"
		);
		putStates(
			1732,
			"{Name:'minecraft:brick_stairs',Properties:{facing:'east',half:'top',shape:'straight'}}",
			"{Name:'minecraft:brick_stairs',Properties:{facing:'east',half:'top',shape:'inner_left'}}",
			"{Name:'minecraft:brick_stairs',Properties:{facing:'east',half:'top',shape:'inner_right'}}",
			"{Name:'minecraft:brick_stairs',Properties:{facing:'east',half:'top',shape:'outer_left'}}",
			"{Name:'minecraft:brick_stairs',Properties:{facing:'east',half:'top',shape:'outer_right'}}",
			"{Name:'minecraft:brick_stairs',Properties:{facing:'east',half:'top',shape:'straight'}}"
		);
		putStates(
			1733,
			"{Name:'minecraft:brick_stairs',Properties:{facing:'west',half:'top',shape:'straight'}}",
			"{Name:'minecraft:brick_stairs',Properties:{facing:'west',half:'top',shape:'inner_left'}}",
			"{Name:'minecraft:brick_stairs',Properties:{facing:'west',half:'top',shape:'inner_right'}}",
			"{Name:'minecraft:brick_stairs',Properties:{facing:'west',half:'top',shape:'outer_left'}}",
			"{Name:'minecraft:brick_stairs',Properties:{facing:'west',half:'top',shape:'outer_right'}}",
			"{Name:'minecraft:brick_stairs',Properties:{facing:'west',half:'top',shape:'straight'}}"
		);
		putStates(
			1734,
			"{Name:'minecraft:brick_stairs',Properties:{facing:'south',half:'top',shape:'straight'}}",
			"{Name:'minecraft:brick_stairs',Properties:{facing:'south',half:'top',shape:'inner_left'}}",
			"{Name:'minecraft:brick_stairs',Properties:{facing:'south',half:'top',shape:'inner_right'}}",
			"{Name:'minecraft:brick_stairs',Properties:{facing:'south',half:'top',shape:'outer_left'}}",
			"{Name:'minecraft:brick_stairs',Properties:{facing:'south',half:'top',shape:'outer_right'}}",
			"{Name:'minecraft:brick_stairs',Properties:{facing:'south',half:'top',shape:'straight'}}"
		);
		putStates(
			1735,
			"{Name:'minecraft:brick_stairs',Properties:{facing:'north',half:'top',shape:'straight'}}",
			"{Name:'minecraft:brick_stairs',Properties:{facing:'north',half:'top',shape:'inner_left'}}",
			"{Name:'minecraft:brick_stairs',Properties:{facing:'north',half:'top',shape:'inner_right'}}",
			"{Name:'minecraft:brick_stairs',Properties:{facing:'north',half:'top',shape:'outer_left'}}",
			"{Name:'minecraft:brick_stairs',Properties:{facing:'north',half:'top',shape:'outer_right'}}",
			"{Name:'minecraft:brick_stairs',Properties:{facing:'north',half:'top',shape:'straight'}}"
		);
		putStates(
			1744,
			"{Name:'minecraft:stone_brick_stairs',Properties:{facing:'east',half:'bottom',shape:'straight'}}",
			"{Name:'minecraft:stone_brick_stairs',Properties:{facing:'east',half:'bottom',shape:'inner_left'}}",
			"{Name:'minecraft:stone_brick_stairs',Properties:{facing:'east',half:'bottom',shape:'inner_right'}}",
			"{Name:'minecraft:stone_brick_stairs',Properties:{facing:'east',half:'bottom',shape:'outer_left'}}",
			"{Name:'minecraft:stone_brick_stairs',Properties:{facing:'east',half:'bottom',shape:'outer_right'}}",
			"{Name:'minecraft:stone_brick_stairs',Properties:{facing:'east',half:'bottom',shape:'straight'}}"
		);
		putStates(
			1745,
			"{Name:'minecraft:stone_brick_stairs',Properties:{facing:'west',half:'bottom',shape:'straight'}}",
			"{Name:'minecraft:stone_brick_stairs',Properties:{facing:'west',half:'bottom',shape:'inner_left'}}",
			"{Name:'minecraft:stone_brick_stairs',Properties:{facing:'west',half:'bottom',shape:'inner_right'}}",
			"{Name:'minecraft:stone_brick_stairs',Properties:{facing:'west',half:'bottom',shape:'outer_left'}}",
			"{Name:'minecraft:stone_brick_stairs',Properties:{facing:'west',half:'bottom',shape:'outer_right'}}",
			"{Name:'minecraft:stone_brick_stairs',Properties:{facing:'west',half:'bottom',shape:'straight'}}"
		);
		putStates(
			1746,
			"{Name:'minecraft:stone_brick_stairs',Properties:{facing:'south',half:'bottom',shape:'straight'}}",
			"{Name:'minecraft:stone_brick_stairs',Properties:{facing:'south',half:'bottom',shape:'inner_left'}}",
			"{Name:'minecraft:stone_brick_stairs',Properties:{facing:'south',half:'bottom',shape:'inner_right'}}",
			"{Name:'minecraft:stone_brick_stairs',Properties:{facing:'south',half:'bottom',shape:'outer_left'}}",
			"{Name:'minecraft:stone_brick_stairs',Properties:{facing:'south',half:'bottom',shape:'outer_right'}}",
			"{Name:'minecraft:stone_brick_stairs',Properties:{facing:'south',half:'bottom',shape:'straight'}}"
		);
		putStates(
			1747,
			"{Name:'minecraft:stone_brick_stairs',Properties:{facing:'north',half:'bottom',shape:'straight'}}",
			"{Name:'minecraft:stone_brick_stairs',Properties:{facing:'north',half:'bottom',shape:'inner_left'}}",
			"{Name:'minecraft:stone_brick_stairs',Properties:{facing:'north',half:'bottom',shape:'inner_right'}}",
			"{Name:'minecraft:stone_brick_stairs',Properties:{facing:'north',half:'bottom',shape:'outer_left'}}",
			"{Name:'minecraft:stone_brick_stairs',Properties:{facing:'north',half:'bottom',shape:'outer_right'}}",
			"{Name:'minecraft:stone_brick_stairs',Properties:{facing:'north',half:'bottom',shape:'straight'}}"
		);
		putStates(
			1748,
			"{Name:'minecraft:stone_brick_stairs',Properties:{facing:'east',half:'top',shape:'straight'}}",
			"{Name:'minecraft:stone_brick_stairs',Properties:{facing:'east',half:'top',shape:'inner_left'}}",
			"{Name:'minecraft:stone_brick_stairs',Properties:{facing:'east',half:'top',shape:'inner_right'}}",
			"{Name:'minecraft:stone_brick_stairs',Properties:{facing:'east',half:'top',shape:'outer_left'}}",
			"{Name:'minecraft:stone_brick_stairs',Properties:{facing:'east',half:'top',shape:'outer_right'}}",
			"{Name:'minecraft:stone_brick_stairs',Properties:{facing:'east',half:'top',shape:'straight'}}"
		);
		putStates(
			1749,
			"{Name:'minecraft:stone_brick_stairs',Properties:{facing:'west',half:'top',shape:'straight'}}",
			"{Name:'minecraft:stone_brick_stairs',Properties:{facing:'west',half:'top',shape:'inner_left'}}",
			"{Name:'minecraft:stone_brick_stairs',Properties:{facing:'west',half:'top',shape:'inner_right'}}",
			"{Name:'minecraft:stone_brick_stairs',Properties:{facing:'west',half:'top',shape:'outer_left'}}",
			"{Name:'minecraft:stone_brick_stairs',Properties:{facing:'west',half:'top',shape:'outer_right'}}",
			"{Name:'minecraft:stone_brick_stairs',Properties:{facing:'west',half:'top',shape:'straight'}}"
		);
		putStates(
			1750,
			"{Name:'minecraft:stone_brick_stairs',Properties:{facing:'south',half:'top',shape:'straight'}}",
			"{Name:'minecraft:stone_brick_stairs',Properties:{facing:'south',half:'top',shape:'inner_left'}}",
			"{Name:'minecraft:stone_brick_stairs',Properties:{facing:'south',half:'top',shape:'inner_right'}}",
			"{Name:'minecraft:stone_brick_stairs',Properties:{facing:'south',half:'top',shape:'outer_left'}}",
			"{Name:'minecraft:stone_brick_stairs',Properties:{facing:'south',half:'top',shape:'outer_right'}}",
			"{Name:'minecraft:stone_brick_stairs',Properties:{facing:'south',half:'top',shape:'straight'}}"
		);
		putStates(
			1751,
			"{Name:'minecraft:stone_brick_stairs',Properties:{facing:'north',half:'top',shape:'straight'}}",
			"{Name:'minecraft:stone_brick_stairs',Properties:{facing:'north',half:'top',shape:'inner_left'}}",
			"{Name:'minecraft:stone_brick_stairs',Properties:{facing:'north',half:'top',shape:'inner_right'}}",
			"{Name:'minecraft:stone_brick_stairs',Properties:{facing:'north',half:'top',shape:'outer_left'}}",
			"{Name:'minecraft:stone_brick_stairs',Properties:{facing:'north',half:'top',shape:'outer_right'}}",
			"{Name:'minecraft:stone_brick_stairs',Properties:{facing:'north',half:'top',shape:'straight'}}"
		);
		putStates(
			1760,
			"{Name:'minecraft:mycelium',Properties:{snowy:'false'}}",
			"{Name:'minecraft:mycelium',Properties:{snowy:'false'}}",
			"{Name:'minecraft:mycelium',Properties:{snowy:'true'}}"
		);
		putStates(1776, "{Name:'minecraft:lily_pad'}", "{Name:'minecraft:waterlily'}");
	}

	/**
	 * Adds states to flatten from the blocks which had numeric IDs between 112 and 127 before 1.13.
	 */
	static void putStatesFromBlocks112To127() {
		putStates(1792, "{Name:'minecraft:nether_bricks'}", "{Name:'minecraft:nether_brick'}");
		putStates(
			1808,
			"{Name:'minecraft:nether_brick_fence',Properties:{east:'false',north:'false',south:'false',west:'false'}}",
			"{Name:'minecraft:nether_brick_fence',Properties:{east:'false',north:'false',south:'false',west:'false'}}",
			"{Name:'minecraft:nether_brick_fence',Properties:{east:'false',north:'false',south:'false',west:'true'}}",
			"{Name:'minecraft:nether_brick_fence',Properties:{east:'false',north:'false',south:'true',west:'false'}}",
			"{Name:'minecraft:nether_brick_fence',Properties:{east:'false',north:'false',south:'true',west:'true'}}",
			"{Name:'minecraft:nether_brick_fence',Properties:{east:'false',north:'true',south:'false',west:'false'}}",
			"{Name:'minecraft:nether_brick_fence',Properties:{east:'false',north:'true',south:'false',west:'true'}}",
			"{Name:'minecraft:nether_brick_fence',Properties:{east:'false',north:'true',south:'true',west:'false'}}",
			"{Name:'minecraft:nether_brick_fence',Properties:{east:'false',north:'true',south:'true',west:'true'}}",
			"{Name:'minecraft:nether_brick_fence',Properties:{east:'true',north:'false',south:'false',west:'false'}}",
			"{Name:'minecraft:nether_brick_fence',Properties:{east:'true',north:'false',south:'false',west:'true'}}",
			"{Name:'minecraft:nether_brick_fence',Properties:{east:'true',north:'false',south:'true',west:'false'}}",
			"{Name:'minecraft:nether_brick_fence',Properties:{east:'true',north:'false',south:'true',west:'true'}}",
			"{Name:'minecraft:nether_brick_fence',Properties:{east:'true',north:'true',south:'false',west:'false'}}",
			"{Name:'minecraft:nether_brick_fence',Properties:{east:'true',north:'true',south:'false',west:'true'}}",
			"{Name:'minecraft:nether_brick_fence',Properties:{east:'true',north:'true',south:'true',west:'false'}}",
			"{Name:'minecraft:nether_brick_fence',Properties:{east:'true',north:'true',south:'true',west:'true'}}"
		);
		putStates(
			1824,
			"{Name:'minecraft:nether_brick_stairs',Properties:{facing:'east',half:'bottom',shape:'straight'}}",
			"{Name:'minecraft:nether_brick_stairs',Properties:{facing:'east',half:'bottom',shape:'inner_left'}}",
			"{Name:'minecraft:nether_brick_stairs',Properties:{facing:'east',half:'bottom',shape:'inner_right'}}",
			"{Name:'minecraft:nether_brick_stairs',Properties:{facing:'east',half:'bottom',shape:'outer_left'}}",
			"{Name:'minecraft:nether_brick_stairs',Properties:{facing:'east',half:'bottom',shape:'outer_right'}}",
			"{Name:'minecraft:nether_brick_stairs',Properties:{facing:'east',half:'bottom',shape:'straight'}}"
		);
		putStates(
			1825,
			"{Name:'minecraft:nether_brick_stairs',Properties:{facing:'west',half:'bottom',shape:'straight'}}",
			"{Name:'minecraft:nether_brick_stairs',Properties:{facing:'west',half:'bottom',shape:'inner_left'}}",
			"{Name:'minecraft:nether_brick_stairs',Properties:{facing:'west',half:'bottom',shape:'inner_right'}}",
			"{Name:'minecraft:nether_brick_stairs',Properties:{facing:'west',half:'bottom',shape:'outer_left'}}",
			"{Name:'minecraft:nether_brick_stairs',Properties:{facing:'west',half:'bottom',shape:'outer_right'}}",
			"{Name:'minecraft:nether_brick_stairs',Properties:{facing:'west',half:'bottom',shape:'straight'}}"
		);
		putStates(
			1826,
			"{Name:'minecraft:nether_brick_stairs',Properties:{facing:'south',half:'bottom',shape:'straight'}}",
			"{Name:'minecraft:nether_brick_stairs',Properties:{facing:'south',half:'bottom',shape:'inner_left'}}",
			"{Name:'minecraft:nether_brick_stairs',Properties:{facing:'south',half:'bottom',shape:'inner_right'}}",
			"{Name:'minecraft:nether_brick_stairs',Properties:{facing:'south',half:'bottom',shape:'outer_left'}}",
			"{Name:'minecraft:nether_brick_stairs',Properties:{facing:'south',half:'bottom',shape:'outer_right'}}",
			"{Name:'minecraft:nether_brick_stairs',Properties:{facing:'south',half:'bottom',shape:'straight'}}"
		);
		putStates(
			1827,
			"{Name:'minecraft:nether_brick_stairs',Properties:{facing:'north',half:'bottom',shape:'straight'}}",
			"{Name:'minecraft:nether_brick_stairs',Properties:{facing:'north',half:'bottom',shape:'inner_left'}}",
			"{Name:'minecraft:nether_brick_stairs',Properties:{facing:'north',half:'bottom',shape:'inner_right'}}",
			"{Name:'minecraft:nether_brick_stairs',Properties:{facing:'north',half:'bottom',shape:'outer_left'}}",
			"{Name:'minecraft:nether_brick_stairs',Properties:{facing:'north',half:'bottom',shape:'outer_right'}}",
			"{Name:'minecraft:nether_brick_stairs',Properties:{facing:'north',half:'bottom',shape:'straight'}}"
		);
		putStates(
			1828,
			"{Name:'minecraft:nether_brick_stairs',Properties:{facing:'east',half:'top',shape:'straight'}}",
			"{Name:'minecraft:nether_brick_stairs',Properties:{facing:'east',half:'top',shape:'inner_left'}}",
			"{Name:'minecraft:nether_brick_stairs',Properties:{facing:'east',half:'top',shape:'inner_right'}}",
			"{Name:'minecraft:nether_brick_stairs',Properties:{facing:'east',half:'top',shape:'outer_left'}}",
			"{Name:'minecraft:nether_brick_stairs',Properties:{facing:'east',half:'top',shape:'outer_right'}}",
			"{Name:'minecraft:nether_brick_stairs',Properties:{facing:'east',half:'top',shape:'straight'}}"
		);
		putStates(
			1829,
			"{Name:'minecraft:nether_brick_stairs',Properties:{facing:'west',half:'top',shape:'straight'}}",
			"{Name:'minecraft:nether_brick_stairs',Properties:{facing:'west',half:'top',shape:'inner_left'}}",
			"{Name:'minecraft:nether_brick_stairs',Properties:{facing:'west',half:'top',shape:'inner_right'}}",
			"{Name:'minecraft:nether_brick_stairs',Properties:{facing:'west',half:'top',shape:'outer_left'}}",
			"{Name:'minecraft:nether_brick_stairs',Properties:{facing:'west',half:'top',shape:'outer_right'}}",
			"{Name:'minecraft:nether_brick_stairs',Properties:{facing:'west',half:'top',shape:'straight'}}"
		);
		putStates(
			1830,
			"{Name:'minecraft:nether_brick_stairs',Properties:{facing:'south',half:'top',shape:'straight'}}",
			"{Name:'minecraft:nether_brick_stairs',Properties:{facing:'south',half:'top',shape:'inner_left'}}",
			"{Name:'minecraft:nether_brick_stairs',Properties:{facing:'south',half:'top',shape:'inner_right'}}",
			"{Name:'minecraft:nether_brick_stairs',Properties:{facing:'south',half:'top',shape:'outer_left'}}",
			"{Name:'minecraft:nether_brick_stairs',Properties:{facing:'south',half:'top',shape:'outer_right'}}",
			"{Name:'minecraft:nether_brick_stairs',Properties:{facing:'south',half:'top',shape:'straight'}}"
		);
		putStates(
			1831,
			"{Name:'minecraft:nether_brick_stairs',Properties:{facing:'north',half:'top',shape:'straight'}}",
			"{Name:'minecraft:nether_brick_stairs',Properties:{facing:'north',half:'top',shape:'inner_left'}}",
			"{Name:'minecraft:nether_brick_stairs',Properties:{facing:'north',half:'top',shape:'inner_right'}}",
			"{Name:'minecraft:nether_brick_stairs',Properties:{facing:'north',half:'top',shape:'outer_left'}}",
			"{Name:'minecraft:nether_brick_stairs',Properties:{facing:'north',half:'top',shape:'outer_right'}}",
			"{Name:'minecraft:nether_brick_stairs',Properties:{facing:'north',half:'top',shape:'straight'}}"
		);
		putStates(1840, "{Name:'minecraft:nether_wart',Properties:{age:'0'}}", "{Name:'minecraft:nether_wart',Properties:{age:'0'}}");
		putStates(1841, "{Name:'minecraft:nether_wart',Properties:{age:'1'}}", "{Name:'minecraft:nether_wart',Properties:{age:'1'}}");
		putStates(1842, "{Name:'minecraft:nether_wart',Properties:{age:'2'}}", "{Name:'minecraft:nether_wart',Properties:{age:'2'}}");
		putStates(1843, "{Name:'minecraft:nether_wart',Properties:{age:'3'}}", "{Name:'minecraft:nether_wart',Properties:{age:'3'}}");
		putStates(1856, "{Name:'minecraft:enchanting_table'}", "{Name:'minecraft:enchanting_table'}");
		putStates(
			1872,
			"{Name:'minecraft:brewing_stand',Properties:{has_bottle_0:'false',has_bottle_1:'false',has_bottle_2:'false'}}",
			"{Name:'minecraft:brewing_stand',Properties:{has_bottle_0:'false',has_bottle_1:'false',has_bottle_2:'false'}}"
		);
		putStates(
			1873,
			"{Name:'minecraft:brewing_stand',Properties:{has_bottle_0:'true',has_bottle_1:'false',has_bottle_2:'false'}}",
			"{Name:'minecraft:brewing_stand',Properties:{has_bottle_0:'true',has_bottle_1:'false',has_bottle_2:'false'}}"
		);
		putStates(
			1874,
			"{Name:'minecraft:brewing_stand',Properties:{has_bottle_0:'false',has_bottle_1:'true',has_bottle_2:'false'}}",
			"{Name:'minecraft:brewing_stand',Properties:{has_bottle_0:'false',has_bottle_1:'true',has_bottle_2:'false'}}"
		);
		putStates(
			1875,
			"{Name:'minecraft:brewing_stand',Properties:{has_bottle_0:'true',has_bottle_1:'true',has_bottle_2:'false'}}",
			"{Name:'minecraft:brewing_stand',Properties:{has_bottle_0:'true',has_bottle_1:'true',has_bottle_2:'false'}}"
		);
		putStates(
			1876,
			"{Name:'minecraft:brewing_stand',Properties:{has_bottle_0:'false',has_bottle_1:'false',has_bottle_2:'true'}}",
			"{Name:'minecraft:brewing_stand',Properties:{has_bottle_0:'false',has_bottle_1:'false',has_bottle_2:'true'}}"
		);
		putStates(
			1877,
			"{Name:'minecraft:brewing_stand',Properties:{has_bottle_0:'true',has_bottle_1:'false',has_bottle_2:'true'}}",
			"{Name:'minecraft:brewing_stand',Properties:{has_bottle_0:'true',has_bottle_1:'false',has_bottle_2:'true'}}"
		);
		putStates(
			1878,
			"{Name:'minecraft:brewing_stand',Properties:{has_bottle_0:'false',has_bottle_1:'true',has_bottle_2:'true'}}",
			"{Name:'minecraft:brewing_stand',Properties:{has_bottle_0:'false',has_bottle_1:'true',has_bottle_2:'true'}}"
		);
		putStates(
			1879,
			"{Name:'minecraft:brewing_stand',Properties:{has_bottle_0:'true',has_bottle_1:'true',has_bottle_2:'true'}}",
			"{Name:'minecraft:brewing_stand',Properties:{has_bottle_0:'true',has_bottle_1:'true',has_bottle_2:'true'}}"
		);
		putStates(1888, "{Name:'minecraft:cauldron',Properties:{level:'0'}}", "{Name:'minecraft:cauldron',Properties:{level:'0'}}");
		putStates(1889, "{Name:'minecraft:cauldron',Properties:{level:'1'}}", "{Name:'minecraft:cauldron',Properties:{level:'1'}}");
		putStates(1890, "{Name:'minecraft:cauldron',Properties:{level:'2'}}", "{Name:'minecraft:cauldron',Properties:{level:'2'}}");
		putStates(1891, "{Name:'minecraft:cauldron',Properties:{level:'3'}}", "{Name:'minecraft:cauldron',Properties:{level:'3'}}");
		putStates(1904, "{Name:'minecraft:end_portal'}", "{Name:'minecraft:end_portal'}");
		putStates(
			1920,
			"{Name:'minecraft:end_portal_frame',Properties:{eye:'false',facing:'south'}}",
			"{Name:'minecraft:end_portal_frame',Properties:{eye:'false',facing:'south'}}"
		);
		putStates(
			1921,
			"{Name:'minecraft:end_portal_frame',Properties:{eye:'false',facing:'west'}}",
			"{Name:'minecraft:end_portal_frame',Properties:{eye:'false',facing:'west'}}"
		);
		putStates(
			1922,
			"{Name:'minecraft:end_portal_frame',Properties:{eye:'false',facing:'north'}}",
			"{Name:'minecraft:end_portal_frame',Properties:{eye:'false',facing:'north'}}"
		);
		putStates(
			1923,
			"{Name:'minecraft:end_portal_frame',Properties:{eye:'false',facing:'east'}}",
			"{Name:'minecraft:end_portal_frame',Properties:{eye:'false',facing:'east'}}"
		);
		putStates(
			1924,
			"{Name:'minecraft:end_portal_frame',Properties:{eye:'true',facing:'south'}}",
			"{Name:'minecraft:end_portal_frame',Properties:{eye:'true',facing:'south'}}"
		);
		putStates(
			1925,
			"{Name:'minecraft:end_portal_frame',Properties:{eye:'true',facing:'west'}}",
			"{Name:'minecraft:end_portal_frame',Properties:{eye:'true',facing:'west'}}"
		);
		putStates(
			1926,
			"{Name:'minecraft:end_portal_frame',Properties:{eye:'true',facing:'north'}}",
			"{Name:'minecraft:end_portal_frame',Properties:{eye:'true',facing:'north'}}"
		);
		putStates(
			1927,
			"{Name:'minecraft:end_portal_frame',Properties:{eye:'true',facing:'east'}}",
			"{Name:'minecraft:end_portal_frame',Properties:{eye:'true',facing:'east'}}"
		);
		putStates(1936, "{Name:'minecraft:end_stone'}", "{Name:'minecraft:end_stone'}");
		putStates(1952, "{Name:'minecraft:dragon_egg'}", "{Name:'minecraft:dragon_egg'}");
		putStates(1968, "{Name:'minecraft:redstone_lamp',Properties:{lit:'false'}}", "{Name:'minecraft:redstone_lamp'}");
		putStates(1984, "{Name:'minecraft:redstone_lamp',Properties:{lit:'true'}}", "{Name:'minecraft:lit_redstone_lamp'}");
		putStates(2000, "{Name:'minecraft:oak_slab',Properties:{type:'double'}}", "{Name:'minecraft:double_wooden_slab',Properties:{variant:'oak'}}");
		putStates(2001, "{Name:'minecraft:spruce_slab',Properties:{type:'double'}}", "{Name:'minecraft:double_wooden_slab',Properties:{variant:'spruce'}}");
		putStates(2002, "{Name:'minecraft:birch_slab',Properties:{type:'double'}}", "{Name:'minecraft:double_wooden_slab',Properties:{variant:'birch'}}");
		putStates(2003, "{Name:'minecraft:jungle_slab',Properties:{type:'double'}}", "{Name:'minecraft:double_wooden_slab',Properties:{variant:'jungle'}}");
		putStates(2004, "{Name:'minecraft:acacia_slab',Properties:{type:'double'}}", "{Name:'minecraft:double_wooden_slab',Properties:{variant:'acacia'}}");
		putStates(2005, "{Name:'minecraft:dark_oak_slab',Properties:{type:'double'}}", "{Name:'minecraft:double_wooden_slab',Properties:{variant:'dark_oak'}}");
		putStates(2016, "{Name:'minecraft:oak_slab',Properties:{type:'bottom'}}", "{Name:'minecraft:wooden_slab',Properties:{half:'bottom',variant:'oak'}}");
		putStates(2017, "{Name:'minecraft:spruce_slab',Properties:{type:'bottom'}}", "{Name:'minecraft:wooden_slab',Properties:{half:'bottom',variant:'spruce'}}");
		putStates(2018, "{Name:'minecraft:birch_slab',Properties:{type:'bottom'}}", "{Name:'minecraft:wooden_slab',Properties:{half:'bottom',variant:'birch'}}");
		putStates(2019, "{Name:'minecraft:jungle_slab',Properties:{type:'bottom'}}", "{Name:'minecraft:wooden_slab',Properties:{half:'bottom',variant:'jungle'}}");
		putStates(2020, "{Name:'minecraft:acacia_slab',Properties:{type:'bottom'}}", "{Name:'minecraft:wooden_slab',Properties:{half:'bottom',variant:'acacia'}}");
		putStates(2021, "{Name:'minecraft:dark_oak_slab',Properties:{type:'bottom'}}", "{Name:'minecraft:wooden_slab',Properties:{half:'bottom',variant:'dark_oak'}}");
		putStates(2024, "{Name:'minecraft:oak_slab',Properties:{type:'top'}}", "{Name:'minecraft:wooden_slab',Properties:{half:'top',variant:'oak'}}");
		putStates(2025, "{Name:'minecraft:spruce_slab',Properties:{type:'top'}}", "{Name:'minecraft:wooden_slab',Properties:{half:'top',variant:'spruce'}}");
		putStates(2026, "{Name:'minecraft:birch_slab',Properties:{type:'top'}}", "{Name:'minecraft:wooden_slab',Properties:{half:'top',variant:'birch'}}");
		putStates(2027, "{Name:'minecraft:jungle_slab',Properties:{type:'top'}}", "{Name:'minecraft:wooden_slab',Properties:{half:'top',variant:'jungle'}}");
		putStates(2028, "{Name:'minecraft:acacia_slab',Properties:{type:'top'}}", "{Name:'minecraft:wooden_slab',Properties:{half:'top',variant:'acacia'}}");
		putStates(2029, "{Name:'minecraft:dark_oak_slab',Properties:{type:'top'}}", "{Name:'minecraft:wooden_slab',Properties:{half:'top',variant:'dark_oak'}}");
		putStates(2032, "{Name:'minecraft:cocoa',Properties:{age:'0',facing:'south'}}", "{Name:'minecraft:cocoa',Properties:{age:'0',facing:'south'}}");
		putStates(2033, "{Name:'minecraft:cocoa',Properties:{age:'0',facing:'west'}}", "{Name:'minecraft:cocoa',Properties:{age:'0',facing:'west'}}");
		putStates(2034, "{Name:'minecraft:cocoa',Properties:{age:'0',facing:'north'}}", "{Name:'minecraft:cocoa',Properties:{age:'0',facing:'north'}}");
		putStates(2035, "{Name:'minecraft:cocoa',Properties:{age:'0',facing:'east'}}", "{Name:'minecraft:cocoa',Properties:{age:'0',facing:'east'}}");
		putStates(2036, "{Name:'minecraft:cocoa',Properties:{age:'1',facing:'south'}}", "{Name:'minecraft:cocoa',Properties:{age:'1',facing:'south'}}");
		putStates(2037, "{Name:'minecraft:cocoa',Properties:{age:'1',facing:'west'}}", "{Name:'minecraft:cocoa',Properties:{age:'1',facing:'west'}}");
		putStates(2038, "{Name:'minecraft:cocoa',Properties:{age:'1',facing:'north'}}", "{Name:'minecraft:cocoa',Properties:{age:'1',facing:'north'}}");
		putStates(2039, "{Name:'minecraft:cocoa',Properties:{age:'1',facing:'east'}}", "{Name:'minecraft:cocoa',Properties:{age:'1',facing:'east'}}");
		putStates(2040, "{Name:'minecraft:cocoa',Properties:{age:'2',facing:'south'}}", "{Name:'minecraft:cocoa',Properties:{age:'2',facing:'south'}}");
		putStates(2041, "{Name:'minecraft:cocoa',Properties:{age:'2',facing:'west'}}", "{Name:'minecraft:cocoa',Properties:{age:'2',facing:'west'}}");
		putStates(2042, "{Name:'minecraft:cocoa',Properties:{age:'2',facing:'north'}}", "{Name:'minecraft:cocoa',Properties:{age:'2',facing:'north'}}");
		putStates(2043, "{Name:'minecraft:cocoa',Properties:{age:'2',facing:'east'}}", "{Name:'minecraft:cocoa',Properties:{age:'2',facing:'east'}}");
	}

	/**
	 * Adds states to flatten from the blocks which had numeric IDs between 128 and 143 before 1.13.
	 */
	static void putStatesFromBlocks128To143() {
		putStates(
			2048,
			"{Name:'minecraft:sandstone_stairs',Properties:{facing:'east',half:'bottom',shape:'straight'}}",
			"{Name:'minecraft:sandstone_stairs',Properties:{facing:'east',half:'bottom',shape:'inner_left'}}",
			"{Name:'minecraft:sandstone_stairs',Properties:{facing:'east',half:'bottom',shape:'inner_right'}}",
			"{Name:'minecraft:sandstone_stairs',Properties:{facing:'east',half:'bottom',shape:'outer_left'}}",
			"{Name:'minecraft:sandstone_stairs',Properties:{facing:'east',half:'bottom',shape:'outer_right'}}",
			"{Name:'minecraft:sandstone_stairs',Properties:{facing:'east',half:'bottom',shape:'straight'}}"
		);
		putStates(
			2049,
			"{Name:'minecraft:sandstone_stairs',Properties:{facing:'west',half:'bottom',shape:'straight'}}",
			"{Name:'minecraft:sandstone_stairs',Properties:{facing:'west',half:'bottom',shape:'inner_left'}}",
			"{Name:'minecraft:sandstone_stairs',Properties:{facing:'west',half:'bottom',shape:'inner_right'}}",
			"{Name:'minecraft:sandstone_stairs',Properties:{facing:'west',half:'bottom',shape:'outer_left'}}",
			"{Name:'minecraft:sandstone_stairs',Properties:{facing:'west',half:'bottom',shape:'outer_right'}}",
			"{Name:'minecraft:sandstone_stairs',Properties:{facing:'west',half:'bottom',shape:'straight'}}"
		);
		putStates(
			2050,
			"{Name:'minecraft:sandstone_stairs',Properties:{facing:'south',half:'bottom',shape:'straight'}}",
			"{Name:'minecraft:sandstone_stairs',Properties:{facing:'south',half:'bottom',shape:'inner_left'}}",
			"{Name:'minecraft:sandstone_stairs',Properties:{facing:'south',half:'bottom',shape:'inner_right'}}",
			"{Name:'minecraft:sandstone_stairs',Properties:{facing:'south',half:'bottom',shape:'outer_left'}}",
			"{Name:'minecraft:sandstone_stairs',Properties:{facing:'south',half:'bottom',shape:'outer_right'}}",
			"{Name:'minecraft:sandstone_stairs',Properties:{facing:'south',half:'bottom',shape:'straight'}}"
		);
		putStates(
			2051,
			"{Name:'minecraft:sandstone_stairs',Properties:{facing:'north',half:'bottom',shape:'straight'}}",
			"{Name:'minecraft:sandstone_stairs',Properties:{facing:'north',half:'bottom',shape:'inner_left'}}",
			"{Name:'minecraft:sandstone_stairs',Properties:{facing:'north',half:'bottom',shape:'inner_right'}}",
			"{Name:'minecraft:sandstone_stairs',Properties:{facing:'north',half:'bottom',shape:'outer_left'}}",
			"{Name:'minecraft:sandstone_stairs',Properties:{facing:'north',half:'bottom',shape:'outer_right'}}",
			"{Name:'minecraft:sandstone_stairs',Properties:{facing:'north',half:'bottom',shape:'straight'}}"
		);
		putStates(
			2052,
			"{Name:'minecraft:sandstone_stairs',Properties:{facing:'east',half:'top',shape:'straight'}}",
			"{Name:'minecraft:sandstone_stairs',Properties:{facing:'east',half:'top',shape:'inner_left'}}",
			"{Name:'minecraft:sandstone_stairs',Properties:{facing:'east',half:'top',shape:'inner_right'}}",
			"{Name:'minecraft:sandstone_stairs',Properties:{facing:'east',half:'top',shape:'outer_left'}}",
			"{Name:'minecraft:sandstone_stairs',Properties:{facing:'east',half:'top',shape:'outer_right'}}",
			"{Name:'minecraft:sandstone_stairs',Properties:{facing:'east',half:'top',shape:'straight'}}"
		);
		putStates(
			2053,
			"{Name:'minecraft:sandstone_stairs',Properties:{facing:'west',half:'top',shape:'straight'}}",
			"{Name:'minecraft:sandstone_stairs',Properties:{facing:'west',half:'top',shape:'inner_left'}}",
			"{Name:'minecraft:sandstone_stairs',Properties:{facing:'west',half:'top',shape:'inner_right'}}",
			"{Name:'minecraft:sandstone_stairs',Properties:{facing:'west',half:'top',shape:'outer_left'}}",
			"{Name:'minecraft:sandstone_stairs',Properties:{facing:'west',half:'top',shape:'outer_right'}}",
			"{Name:'minecraft:sandstone_stairs',Properties:{facing:'west',half:'top',shape:'straight'}}"
		);
		putStates(
			2054,
			"{Name:'minecraft:sandstone_stairs',Properties:{facing:'south',half:'top',shape:'straight'}}",
			"{Name:'minecraft:sandstone_stairs',Properties:{facing:'south',half:'top',shape:'inner_left'}}",
			"{Name:'minecraft:sandstone_stairs',Properties:{facing:'south',half:'top',shape:'inner_right'}}",
			"{Name:'minecraft:sandstone_stairs',Properties:{facing:'south',half:'top',shape:'outer_left'}}",
			"{Name:'minecraft:sandstone_stairs',Properties:{facing:'south',half:'top',shape:'outer_right'}}",
			"{Name:'minecraft:sandstone_stairs',Properties:{facing:'south',half:'top',shape:'straight'}}"
		);
		putStates(
			2055,
			"{Name:'minecraft:sandstone_stairs',Properties:{facing:'north',half:'top',shape:'straight'}}",
			"{Name:'minecraft:sandstone_stairs',Properties:{facing:'north',half:'top',shape:'inner_left'}}",
			"{Name:'minecraft:sandstone_stairs',Properties:{facing:'north',half:'top',shape:'inner_right'}}",
			"{Name:'minecraft:sandstone_stairs',Properties:{facing:'north',half:'top',shape:'outer_left'}}",
			"{Name:'minecraft:sandstone_stairs',Properties:{facing:'north',half:'top',shape:'outer_right'}}",
			"{Name:'minecraft:sandstone_stairs',Properties:{facing:'north',half:'top',shape:'straight'}}"
		);
		putStates(2064, "{Name:'minecraft:emerald_ore'}", "{Name:'minecraft:emerald_ore'}");
		putStates(2082, "{Name:'minecraft:ender_chest',Properties:{facing:'north'}}", "{Name:'minecraft:ender_chest',Properties:{facing:'north'}}");
		putStates(2083, "{Name:'minecraft:ender_chest',Properties:{facing:'south'}}", "{Name:'minecraft:ender_chest',Properties:{facing:'south'}}");
		putStates(2084, "{Name:'minecraft:ender_chest',Properties:{facing:'west'}}", "{Name:'minecraft:ender_chest',Properties:{facing:'west'}}");
		putStates(2085, "{Name:'minecraft:ender_chest',Properties:{facing:'east'}}", "{Name:'minecraft:ender_chest',Properties:{facing:'east'}}");
		putStates(
			2096,
			"{Name:'minecraft:tripwire_hook',Properties:{attached:'false',facing:'south',powered:'false'}}",
			"{Name:'minecraft:tripwire_hook',Properties:{attached:'false',facing:'south',powered:'false'}}"
		);
		putStates(
			2097,
			"{Name:'minecraft:tripwire_hook',Properties:{attached:'false',facing:'west',powered:'false'}}",
			"{Name:'minecraft:tripwire_hook',Properties:{attached:'false',facing:'west',powered:'false'}}"
		);
		putStates(
			2098,
			"{Name:'minecraft:tripwire_hook',Properties:{attached:'false',facing:'north',powered:'false'}}",
			"{Name:'minecraft:tripwire_hook',Properties:{attached:'false',facing:'north',powered:'false'}}"
		);
		putStates(
			2099,
			"{Name:'minecraft:tripwire_hook',Properties:{attached:'false',facing:'east',powered:'false'}}",
			"{Name:'minecraft:tripwire_hook',Properties:{attached:'false',facing:'east',powered:'false'}}"
		);
		putStates(
			2100,
			"{Name:'minecraft:tripwire_hook',Properties:{attached:'true',facing:'south',powered:'false'}}",
			"{Name:'minecraft:tripwire_hook',Properties:{attached:'true',facing:'south',powered:'false'}}"
		);
		putStates(
			2101,
			"{Name:'minecraft:tripwire_hook',Properties:{attached:'true',facing:'west',powered:'false'}}",
			"{Name:'minecraft:tripwire_hook',Properties:{attached:'true',facing:'west',powered:'false'}}"
		);
		putStates(
			2102,
			"{Name:'minecraft:tripwire_hook',Properties:{attached:'true',facing:'north',powered:'false'}}",
			"{Name:'minecraft:tripwire_hook',Properties:{attached:'true',facing:'north',powered:'false'}}"
		);
		putStates(
			2103,
			"{Name:'minecraft:tripwire_hook',Properties:{attached:'true',facing:'east',powered:'false'}}",
			"{Name:'minecraft:tripwire_hook',Properties:{attached:'true',facing:'east',powered:'false'}}"
		);
		putStates(
			2104,
			"{Name:'minecraft:tripwire_hook',Properties:{attached:'false',facing:'south',powered:'true'}}",
			"{Name:'minecraft:tripwire_hook',Properties:{attached:'false',facing:'south',powered:'true'}}"
		);
		putStates(
			2105,
			"{Name:'minecraft:tripwire_hook',Properties:{attached:'false',facing:'west',powered:'true'}}",
			"{Name:'minecraft:tripwire_hook',Properties:{attached:'false',facing:'west',powered:'true'}}"
		);
		putStates(
			2106,
			"{Name:'minecraft:tripwire_hook',Properties:{attached:'false',facing:'north',powered:'true'}}",
			"{Name:'minecraft:tripwire_hook',Properties:{attached:'false',facing:'north',powered:'true'}}"
		);
		putStates(
			2107,
			"{Name:'minecraft:tripwire_hook',Properties:{attached:'false',facing:'east',powered:'true'}}",
			"{Name:'minecraft:tripwire_hook',Properties:{attached:'false',facing:'east',powered:'true'}}"
		);
		putStates(
			2108,
			"{Name:'minecraft:tripwire_hook',Properties:{attached:'true',facing:'south',powered:'true'}}",
			"{Name:'minecraft:tripwire_hook',Properties:{attached:'true',facing:'south',powered:'true'}}"
		);
		putStates(
			2109,
			"{Name:'minecraft:tripwire_hook',Properties:{attached:'true',facing:'west',powered:'true'}}",
			"{Name:'minecraft:tripwire_hook',Properties:{attached:'true',facing:'west',powered:'true'}}"
		);
		putStates(
			2110,
			"{Name:'minecraft:tripwire_hook',Properties:{attached:'true',facing:'north',powered:'true'}}",
			"{Name:'minecraft:tripwire_hook',Properties:{attached:'true',facing:'north',powered:'true'}}"
		);
		putStates(
			2111,
			"{Name:'minecraft:tripwire_hook',Properties:{attached:'true',facing:'east',powered:'true'}}",
			"{Name:'minecraft:tripwire_hook',Properties:{attached:'true',facing:'east',powered:'true'}}"
		);
		putStates(
			2112,
			"{Name:'minecraft:tripwire',Properties:{attached:'false',disarmed:'false',east:'false',north:'false',powered:'false',south:'false',west:'false'}}",
			"{Name:'minecraft:tripwire',Properties:{attached:'false',disarmed:'false',east:'false',north:'false',powered:'false',south:'false',west:'false'}}",
			"{Name:'minecraft:tripwire',Properties:{attached:'false',disarmed:'false',east:'false',north:'false',powered:'false',south:'false',west:'true'}}",
			"{Name:'minecraft:tripwire',Properties:{attached:'false',disarmed:'false',east:'false',north:'false',powered:'false',south:'true',west:'false'}}",
			"{Name:'minecraft:tripwire',Properties:{attached:'false',disarmed:'false',east:'false',north:'false',powered:'false',south:'true',west:'true'}}",
			"{Name:'minecraft:tripwire',Properties:{attached:'false',disarmed:'false',east:'false',north:'true',powered:'false',south:'false',west:'false'}}",
			"{Name:'minecraft:tripwire',Properties:{attached:'false',disarmed:'false',east:'false',north:'true',powered:'false',south:'false',west:'true'}}",
			"{Name:'minecraft:tripwire',Properties:{attached:'false',disarmed:'false',east:'false',north:'true',powered:'false',south:'true',west:'false'}}",
			"{Name:'minecraft:tripwire',Properties:{attached:'false',disarmed:'false',east:'false',north:'true',powered:'false',south:'true',west:'true'}}",
			"{Name:'minecraft:tripwire',Properties:{attached:'false',disarmed:'false',east:'true',north:'false',powered:'false',south:'false',west:'false'}}",
			"{Name:'minecraft:tripwire',Properties:{attached:'false',disarmed:'false',east:'true',north:'false',powered:'false',south:'false',west:'true'}}",
			"{Name:'minecraft:tripwire',Properties:{attached:'false',disarmed:'false',east:'true',north:'false',powered:'false',south:'true',west:'false'}}",
			"{Name:'minecraft:tripwire',Properties:{attached:'false',disarmed:'false',east:'true',north:'false',powered:'false',south:'true',west:'true'}}",
			"{Name:'minecraft:tripwire',Properties:{attached:'false',disarmed:'false',east:'true',north:'true',powered:'false',south:'false',west:'false'}}",
			"{Name:'minecraft:tripwire',Properties:{attached:'false',disarmed:'false',east:'true',north:'true',powered:'false',south:'false',west:'true'}}",
			"{Name:'minecraft:tripwire',Properties:{attached:'false',disarmed:'false',east:'true',north:'true',powered:'false',south:'true',west:'false'}}",
			"{Name:'minecraft:tripwire',Properties:{attached:'false',disarmed:'false',east:'true',north:'true',powered:'false',south:'true',west:'true'}}"
		);
		putStates(
			2113,
			"{Name:'minecraft:tripwire',Properties:{attached:'false',disarmed:'false',east:'false',north:'false',powered:'true',south:'false',west:'false'}}",
			"{Name:'minecraft:tripwire',Properties:{attached:'false',disarmed:'false',east:'false',north:'false',powered:'true',south:'false',west:'false'}}",
			"{Name:'minecraft:tripwire',Properties:{attached:'false',disarmed:'false',east:'false',north:'false',powered:'true',south:'false',west:'true'}}",
			"{Name:'minecraft:tripwire',Properties:{attached:'false',disarmed:'false',east:'false',north:'false',powered:'true',south:'true',west:'false'}}",
			"{Name:'minecraft:tripwire',Properties:{attached:'false',disarmed:'false',east:'false',north:'false',powered:'true',south:'true',west:'true'}}",
			"{Name:'minecraft:tripwire',Properties:{attached:'false',disarmed:'false',east:'false',north:'true',powered:'true',south:'false',west:'false'}}",
			"{Name:'minecraft:tripwire',Properties:{attached:'false',disarmed:'false',east:'false',north:'true',powered:'true',south:'false',west:'true'}}",
			"{Name:'minecraft:tripwire',Properties:{attached:'false',disarmed:'false',east:'false',north:'true',powered:'true',south:'true',west:'false'}}",
			"{Name:'minecraft:tripwire',Properties:{attached:'false',disarmed:'false',east:'false',north:'true',powered:'true',south:'true',west:'true'}}",
			"{Name:'minecraft:tripwire',Properties:{attached:'false',disarmed:'false',east:'true',north:'false',powered:'true',south:'false',west:'false'}}",
			"{Name:'minecraft:tripwire',Properties:{attached:'false',disarmed:'false',east:'true',north:'false',powered:'true',south:'false',west:'true'}}",
			"{Name:'minecraft:tripwire',Properties:{attached:'false',disarmed:'false',east:'true',north:'false',powered:'true',south:'true',west:'false'}}",
			"{Name:'minecraft:tripwire',Properties:{attached:'false',disarmed:'false',east:'true',north:'false',powered:'true',south:'true',west:'true'}}",
			"{Name:'minecraft:tripwire',Properties:{attached:'false',disarmed:'false',east:'true',north:'true',powered:'true',south:'false',west:'false'}}",
			"{Name:'minecraft:tripwire',Properties:{attached:'false',disarmed:'false',east:'true',north:'true',powered:'true',south:'false',west:'true'}}",
			"{Name:'minecraft:tripwire',Properties:{attached:'false',disarmed:'false',east:'true',north:'true',powered:'true',south:'true',west:'false'}}",
			"{Name:'minecraft:tripwire',Properties:{attached:'false',disarmed:'false',east:'true',north:'true',powered:'true',south:'true',west:'true'}}"
		);
		putStates(
			2114, "{Name:'minecraft:tripwire',Properties:{attached:'false',disarmed:'false',east:'false',north:'false',powered:'false',south:'false',west:'false'}}"
		);
		putStates(
			2115, "{Name:'minecraft:tripwire',Properties:{attached:'false',disarmed:'false',east:'false',north:'false',powered:'true',south:'false',west:'false'}}"
		);
		putStates(
			2116,
			"{Name:'minecraft:tripwire',Properties:{attached:'true',disarmed:'false',east:'false',north:'false',powered:'false',south:'false',west:'false'}}",
			"{Name:'minecraft:tripwire',Properties:{attached:'true',disarmed:'false',east:'false',north:'false',powered:'false',south:'false',west:'false'}}",
			"{Name:'minecraft:tripwire',Properties:{attached:'true',disarmed:'false',east:'false',north:'false',powered:'false',south:'false',west:'true'}}",
			"{Name:'minecraft:tripwire',Properties:{attached:'true',disarmed:'false',east:'false',north:'false',powered:'false',south:'true',west:'false'}}",
			"{Name:'minecraft:tripwire',Properties:{attached:'true',disarmed:'false',east:'false',north:'false',powered:'false',south:'true',west:'true'}}",
			"{Name:'minecraft:tripwire',Properties:{attached:'true',disarmed:'false',east:'false',north:'true',powered:'false',south:'false',west:'false'}}",
			"{Name:'minecraft:tripwire',Properties:{attached:'true',disarmed:'false',east:'false',north:'true',powered:'false',south:'false',west:'true'}}",
			"{Name:'minecraft:tripwire',Properties:{attached:'true',disarmed:'false',east:'false',north:'true',powered:'false',south:'true',west:'false'}}",
			"{Name:'minecraft:tripwire',Properties:{attached:'true',disarmed:'false',east:'false',north:'true',powered:'false',south:'true',west:'true'}}",
			"{Name:'minecraft:tripwire',Properties:{attached:'true',disarmed:'false',east:'true',north:'false',powered:'false',south:'false',west:'false'}}",
			"{Name:'minecraft:tripwire',Properties:{attached:'true',disarmed:'false',east:'true',north:'false',powered:'false',south:'false',west:'true'}}",
			"{Name:'minecraft:tripwire',Properties:{attached:'true',disarmed:'false',east:'true',north:'false',powered:'false',south:'true',west:'false'}}",
			"{Name:'minecraft:tripwire',Properties:{attached:'true',disarmed:'false',east:'true',north:'false',powered:'false',south:'true',west:'true'}}",
			"{Name:'minecraft:tripwire',Properties:{attached:'true',disarmed:'false',east:'true',north:'true',powered:'false',south:'false',west:'false'}}",
			"{Name:'minecraft:tripwire',Properties:{attached:'true',disarmed:'false',east:'true',north:'true',powered:'false',south:'false',west:'true'}}",
			"{Name:'minecraft:tripwire',Properties:{attached:'true',disarmed:'false',east:'true',north:'true',powered:'false',south:'true',west:'false'}}",
			"{Name:'minecraft:tripwire',Properties:{attached:'true',disarmed:'false',east:'true',north:'true',powered:'false',south:'true',west:'true'}}"
		);
		putStates(
			2117,
			"{Name:'minecraft:tripwire',Properties:{attached:'true',disarmed:'false',east:'false',north:'false',powered:'true',south:'false',west:'false'}}",
			"{Name:'minecraft:tripwire',Properties:{attached:'true',disarmed:'false',east:'false',north:'false',powered:'true',south:'false',west:'false'}}",
			"{Name:'minecraft:tripwire',Properties:{attached:'true',disarmed:'false',east:'false',north:'false',powered:'true',south:'false',west:'true'}}",
			"{Name:'minecraft:tripwire',Properties:{attached:'true',disarmed:'false',east:'false',north:'false',powered:'true',south:'true',west:'false'}}",
			"{Name:'minecraft:tripwire',Properties:{attached:'true',disarmed:'false',east:'false',north:'false',powered:'true',south:'true',west:'true'}}",
			"{Name:'minecraft:tripwire',Properties:{attached:'true',disarmed:'false',east:'false',north:'true',powered:'true',south:'false',west:'false'}}",
			"{Name:'minecraft:tripwire',Properties:{attached:'true',disarmed:'false',east:'false',north:'true',powered:'true',south:'false',west:'true'}}",
			"{Name:'minecraft:tripwire',Properties:{attached:'true',disarmed:'false',east:'false',north:'true',powered:'true',south:'true',west:'false'}}",
			"{Name:'minecraft:tripwire',Properties:{attached:'true',disarmed:'false',east:'false',north:'true',powered:'true',south:'true',west:'true'}}",
			"{Name:'minecraft:tripwire',Properties:{attached:'true',disarmed:'false',east:'true',north:'false',powered:'true',south:'false',west:'false'}}",
			"{Name:'minecraft:tripwire',Properties:{attached:'true',disarmed:'false',east:'true',north:'false',powered:'true',south:'false',west:'true'}}",
			"{Name:'minecraft:tripwire',Properties:{attached:'true',disarmed:'false',east:'true',north:'false',powered:'true',south:'true',west:'false'}}",
			"{Name:'minecraft:tripwire',Properties:{attached:'true',disarmed:'false',east:'true',north:'false',powered:'true',south:'true',west:'true'}}",
			"{Name:'minecraft:tripwire',Properties:{attached:'true',disarmed:'false',east:'true',north:'true',powered:'true',south:'false',west:'false'}}",
			"{Name:'minecraft:tripwire',Properties:{attached:'true',disarmed:'false',east:'true',north:'true',powered:'true',south:'false',west:'true'}}",
			"{Name:'minecraft:tripwire',Properties:{attached:'true',disarmed:'false',east:'true',north:'true',powered:'true',south:'true',west:'false'}}",
			"{Name:'minecraft:tripwire',Properties:{attached:'true',disarmed:'false',east:'true',north:'true',powered:'true',south:'true',west:'true'}}"
		);
		putStates(
			2118, "{Name:'minecraft:tripwire',Properties:{attached:'true',disarmed:'false',east:'false',north:'false',powered:'false',south:'false',west:'false'}}"
		);
		putStates(
			2119, "{Name:'minecraft:tripwire',Properties:{attached:'true',disarmed:'false',east:'false',north:'false',powered:'true',south:'false',west:'false'}}"
		);
		putStates(
			2120,
			"{Name:'minecraft:tripwire',Properties:{attached:'false',disarmed:'true',east:'false',north:'false',powered:'false',south:'false',west:'false'}}",
			"{Name:'minecraft:tripwire',Properties:{attached:'false',disarmed:'true',east:'false',north:'false',powered:'false',south:'false',west:'false'}}",
			"{Name:'minecraft:tripwire',Properties:{attached:'false',disarmed:'true',east:'false',north:'false',powered:'false',south:'false',west:'true'}}",
			"{Name:'minecraft:tripwire',Properties:{attached:'false',disarmed:'true',east:'false',north:'false',powered:'false',south:'true',west:'false'}}",
			"{Name:'minecraft:tripwire',Properties:{attached:'false',disarmed:'true',east:'false',north:'false',powered:'false',south:'true',west:'true'}}",
			"{Name:'minecraft:tripwire',Properties:{attached:'false',disarmed:'true',east:'false',north:'true',powered:'false',south:'false',west:'false'}}",
			"{Name:'minecraft:tripwire',Properties:{attached:'false',disarmed:'true',east:'false',north:'true',powered:'false',south:'false',west:'true'}}",
			"{Name:'minecraft:tripwire',Properties:{attached:'false',disarmed:'true',east:'false',north:'true',powered:'false',south:'true',west:'false'}}",
			"{Name:'minecraft:tripwire',Properties:{attached:'false',disarmed:'true',east:'false',north:'true',powered:'false',south:'true',west:'true'}}",
			"{Name:'minecraft:tripwire',Properties:{attached:'false',disarmed:'true',east:'true',north:'false',powered:'false',south:'false',west:'false'}}",
			"{Name:'minecraft:tripwire',Properties:{attached:'false',disarmed:'true',east:'true',north:'false',powered:'false',south:'false',west:'true'}}",
			"{Name:'minecraft:tripwire',Properties:{attached:'false',disarmed:'true',east:'true',north:'false',powered:'false',south:'true',west:'false'}}",
			"{Name:'minecraft:tripwire',Properties:{attached:'false',disarmed:'true',east:'true',north:'false',powered:'false',south:'true',west:'true'}}",
			"{Name:'minecraft:tripwire',Properties:{attached:'false',disarmed:'true',east:'true',north:'true',powered:'false',south:'false',west:'false'}}",
			"{Name:'minecraft:tripwire',Properties:{attached:'false',disarmed:'true',east:'true',north:'true',powered:'false',south:'false',west:'true'}}",
			"{Name:'minecraft:tripwire',Properties:{attached:'false',disarmed:'true',east:'true',north:'true',powered:'false',south:'true',west:'false'}}",
			"{Name:'minecraft:tripwire',Properties:{attached:'false',disarmed:'true',east:'true',north:'true',powered:'false',south:'true',west:'true'}}"
		);
		putStates(
			2121,
			"{Name:'minecraft:tripwire',Properties:{attached:'false',disarmed:'true',east:'false',north:'false',powered:'true',south:'false',west:'false'}}",
			"{Name:'minecraft:tripwire',Properties:{attached:'false',disarmed:'true',east:'false',north:'false',powered:'true',south:'false',west:'false'}}",
			"{Name:'minecraft:tripwire',Properties:{attached:'false',disarmed:'true',east:'false',north:'false',powered:'true',south:'false',west:'true'}}",
			"{Name:'minecraft:tripwire',Properties:{attached:'false',disarmed:'true',east:'false',north:'false',powered:'true',south:'true',west:'false'}}",
			"{Name:'minecraft:tripwire',Properties:{attached:'false',disarmed:'true',east:'false',north:'false',powered:'true',south:'true',west:'true'}}",
			"{Name:'minecraft:tripwire',Properties:{attached:'false',disarmed:'true',east:'false',north:'true',powered:'true',south:'false',west:'false'}}",
			"{Name:'minecraft:tripwire',Properties:{attached:'false',disarmed:'true',east:'false',north:'true',powered:'true',south:'false',west:'true'}}",
			"{Name:'minecraft:tripwire',Properties:{attached:'false',disarmed:'true',east:'false',north:'true',powered:'true',south:'true',west:'false'}}",
			"{Name:'minecraft:tripwire',Properties:{attached:'false',disarmed:'true',east:'false',north:'true',powered:'true',south:'true',west:'true'}}",
			"{Name:'minecraft:tripwire',Properties:{attached:'false',disarmed:'true',east:'true',north:'false',powered:'true',south:'false',west:'false'}}",
			"{Name:'minecraft:tripwire',Properties:{attached:'false',disarmed:'true',east:'true',north:'false',powered:'true',south:'false',west:'true'}}",
			"{Name:'minecraft:tripwire',Properties:{attached:'false',disarmed:'true',east:'true',north:'false',powered:'true',south:'true',west:'false'}}",
			"{Name:'minecraft:tripwire',Properties:{attached:'false',disarmed:'true',east:'true',north:'false',powered:'true',south:'true',west:'true'}}",
			"{Name:'minecraft:tripwire',Properties:{attached:'false',disarmed:'true',east:'true',north:'true',powered:'true',south:'false',west:'false'}}",
			"{Name:'minecraft:tripwire',Properties:{attached:'false',disarmed:'true',east:'true',north:'true',powered:'true',south:'false',west:'true'}}",
			"{Name:'minecraft:tripwire',Properties:{attached:'false',disarmed:'true',east:'true',north:'true',powered:'true',south:'true',west:'false'}}",
			"{Name:'minecraft:tripwire',Properties:{attached:'false',disarmed:'true',east:'true',north:'true',powered:'true',south:'true',west:'true'}}"
		);
		putStates(
			2122, "{Name:'minecraft:tripwire',Properties:{attached:'false',disarmed:'true',east:'false',north:'false',powered:'false',south:'false',west:'false'}}"
		);
		putStates(
			2123, "{Name:'minecraft:tripwire',Properties:{attached:'false',disarmed:'true',east:'false',north:'false',powered:'true',south:'false',west:'false'}}"
		);
		putStates(
			2124,
			"{Name:'minecraft:tripwire',Properties:{attached:'true',disarmed:'true',east:'false',north:'false',powered:'false',south:'false',west:'false'}}",
			"{Name:'minecraft:tripwire',Properties:{attached:'true',disarmed:'true',east:'false',north:'false',powered:'false',south:'false',west:'false'}}",
			"{Name:'minecraft:tripwire',Properties:{attached:'true',disarmed:'true',east:'false',north:'false',powered:'false',south:'false',west:'true'}}",
			"{Name:'minecraft:tripwire',Properties:{attached:'true',disarmed:'true',east:'false',north:'false',powered:'false',south:'true',west:'false'}}",
			"{Name:'minecraft:tripwire',Properties:{attached:'true',disarmed:'true',east:'false',north:'false',powered:'false',south:'true',west:'true'}}",
			"{Name:'minecraft:tripwire',Properties:{attached:'true',disarmed:'true',east:'false',north:'true',powered:'false',south:'false',west:'false'}}",
			"{Name:'minecraft:tripwire',Properties:{attached:'true',disarmed:'true',east:'false',north:'true',powered:'false',south:'false',west:'true'}}",
			"{Name:'minecraft:tripwire',Properties:{attached:'true',disarmed:'true',east:'false',north:'true',powered:'false',south:'true',west:'false'}}",
			"{Name:'minecraft:tripwire',Properties:{attached:'true',disarmed:'true',east:'false',north:'true',powered:'false',south:'true',west:'true'}}",
			"{Name:'minecraft:tripwire',Properties:{attached:'true',disarmed:'true',east:'true',north:'false',powered:'false',south:'false',west:'false'}}",
			"{Name:'minecraft:tripwire',Properties:{attached:'true',disarmed:'true',east:'true',north:'false',powered:'false',south:'false',west:'true'}}",
			"{Name:'minecraft:tripwire',Properties:{attached:'true',disarmed:'true',east:'true',north:'false',powered:'false',south:'true',west:'false'}}",
			"{Name:'minecraft:tripwire',Properties:{attached:'true',disarmed:'true',east:'true',north:'false',powered:'false',south:'true',west:'true'}}",
			"{Name:'minecraft:tripwire',Properties:{attached:'true',disarmed:'true',east:'true',north:'true',powered:'false',south:'false',west:'false'}}",
			"{Name:'minecraft:tripwire',Properties:{attached:'true',disarmed:'true',east:'true',north:'true',powered:'false',south:'false',west:'true'}}",
			"{Name:'minecraft:tripwire',Properties:{attached:'true',disarmed:'true',east:'true',north:'true',powered:'false',south:'true',west:'false'}}",
			"{Name:'minecraft:tripwire',Properties:{attached:'true',disarmed:'true',east:'true',north:'true',powered:'false',south:'true',west:'true'}}"
		);
		putStates(
			2125,
			"{Name:'minecraft:tripwire',Properties:{attached:'true',disarmed:'true',east:'false',north:'false',powered:'true',south:'false',west:'false'}}",
			"{Name:'minecraft:tripwire',Properties:{attached:'true',disarmed:'true',east:'false',north:'false',powered:'true',south:'false',west:'false'}}",
			"{Name:'minecraft:tripwire',Properties:{attached:'true',disarmed:'true',east:'false',north:'false',powered:'true',south:'false',west:'true'}}",
			"{Name:'minecraft:tripwire',Properties:{attached:'true',disarmed:'true',east:'false',north:'false',powered:'true',south:'true',west:'false'}}",
			"{Name:'minecraft:tripwire',Properties:{attached:'true',disarmed:'true',east:'false',north:'false',powered:'true',south:'true',west:'true'}}",
			"{Name:'minecraft:tripwire',Properties:{attached:'true',disarmed:'true',east:'false',north:'true',powered:'true',south:'false',west:'false'}}",
			"{Name:'minecraft:tripwire',Properties:{attached:'true',disarmed:'true',east:'false',north:'true',powered:'true',south:'false',west:'true'}}",
			"{Name:'minecraft:tripwire',Properties:{attached:'true',disarmed:'true',east:'false',north:'true',powered:'true',south:'true',west:'false'}}",
			"{Name:'minecraft:tripwire',Properties:{attached:'true',disarmed:'true',east:'false',north:'true',powered:'true',south:'true',west:'true'}}",
			"{Name:'minecraft:tripwire',Properties:{attached:'true',disarmed:'true',east:'true',north:'false',powered:'true',south:'false',west:'false'}}",
			"{Name:'minecraft:tripwire',Properties:{attached:'true',disarmed:'true',east:'true',north:'false',powered:'true',south:'false',west:'true'}}",
			"{Name:'minecraft:tripwire',Properties:{attached:'true',disarmed:'true',east:'true',north:'false',powered:'true',south:'true',west:'false'}}",
			"{Name:'minecraft:tripwire',Properties:{attached:'true',disarmed:'true',east:'true',north:'false',powered:'true',south:'true',west:'true'}}",
			"{Name:'minecraft:tripwire',Properties:{attached:'true',disarmed:'true',east:'true',north:'true',powered:'true',south:'false',west:'false'}}",
			"{Name:'minecraft:tripwire',Properties:{attached:'true',disarmed:'true',east:'true',north:'true',powered:'true',south:'false',west:'true'}}",
			"{Name:'minecraft:tripwire',Properties:{attached:'true',disarmed:'true',east:'true',north:'true',powered:'true',south:'true',west:'false'}}",
			"{Name:'minecraft:tripwire',Properties:{attached:'true',disarmed:'true',east:'true',north:'true',powered:'true',south:'true',west:'true'}}"
		);
		putStates(
			2126, "{Name:'minecraft:tripwire',Properties:{attached:'true',disarmed:'true',east:'false',north:'false',powered:'false',south:'false',west:'false'}}"
		);
		putStates(2128, "{Name:'minecraft:emerald_block'}", "{Name:'minecraft:emerald_block'}");
		putStates(
			2144,
			"{Name:'minecraft:spruce_stairs',Properties:{facing:'east',half:'bottom',shape:'straight'}}",
			"{Name:'minecraft:spruce_stairs',Properties:{facing:'east',half:'bottom',shape:'inner_left'}}",
			"{Name:'minecraft:spruce_stairs',Properties:{facing:'east',half:'bottom',shape:'inner_right'}}",
			"{Name:'minecraft:spruce_stairs',Properties:{facing:'east',half:'bottom',shape:'outer_left'}}",
			"{Name:'minecraft:spruce_stairs',Properties:{facing:'east',half:'bottom',shape:'outer_right'}}",
			"{Name:'minecraft:spruce_stairs',Properties:{facing:'east',half:'bottom',shape:'straight'}}"
		);
		putStates(
			2145,
			"{Name:'minecraft:spruce_stairs',Properties:{facing:'west',half:'bottom',shape:'straight'}}",
			"{Name:'minecraft:spruce_stairs',Properties:{facing:'west',half:'bottom',shape:'inner_left'}}",
			"{Name:'minecraft:spruce_stairs',Properties:{facing:'west',half:'bottom',shape:'inner_right'}}",
			"{Name:'minecraft:spruce_stairs',Properties:{facing:'west',half:'bottom',shape:'outer_left'}}",
			"{Name:'minecraft:spruce_stairs',Properties:{facing:'west',half:'bottom',shape:'outer_right'}}",
			"{Name:'minecraft:spruce_stairs',Properties:{facing:'west',half:'bottom',shape:'straight'}}"
		);
		putStates(
			2146,
			"{Name:'minecraft:spruce_stairs',Properties:{facing:'south',half:'bottom',shape:'straight'}}",
			"{Name:'minecraft:spruce_stairs',Properties:{facing:'south',half:'bottom',shape:'inner_left'}}",
			"{Name:'minecraft:spruce_stairs',Properties:{facing:'south',half:'bottom',shape:'inner_right'}}",
			"{Name:'minecraft:spruce_stairs',Properties:{facing:'south',half:'bottom',shape:'outer_left'}}",
			"{Name:'minecraft:spruce_stairs',Properties:{facing:'south',half:'bottom',shape:'outer_right'}}",
			"{Name:'minecraft:spruce_stairs',Properties:{facing:'south',half:'bottom',shape:'straight'}}"
		);
		putStates(
			2147,
			"{Name:'minecraft:spruce_stairs',Properties:{facing:'north',half:'bottom',shape:'straight'}}",
			"{Name:'minecraft:spruce_stairs',Properties:{facing:'north',half:'bottom',shape:'inner_left'}}",
			"{Name:'minecraft:spruce_stairs',Properties:{facing:'north',half:'bottom',shape:'inner_right'}}",
			"{Name:'minecraft:spruce_stairs',Properties:{facing:'north',half:'bottom',shape:'outer_left'}}",
			"{Name:'minecraft:spruce_stairs',Properties:{facing:'north',half:'bottom',shape:'outer_right'}}",
			"{Name:'minecraft:spruce_stairs',Properties:{facing:'north',half:'bottom',shape:'straight'}}"
		);
		putStates(
			2148,
			"{Name:'minecraft:spruce_stairs',Properties:{facing:'east',half:'top',shape:'straight'}}",
			"{Name:'minecraft:spruce_stairs',Properties:{facing:'east',half:'top',shape:'inner_left'}}",
			"{Name:'minecraft:spruce_stairs',Properties:{facing:'east',half:'top',shape:'inner_right'}}",
			"{Name:'minecraft:spruce_stairs',Properties:{facing:'east',half:'top',shape:'outer_left'}}",
			"{Name:'minecraft:spruce_stairs',Properties:{facing:'east',half:'top',shape:'outer_right'}}",
			"{Name:'minecraft:spruce_stairs',Properties:{facing:'east',half:'top',shape:'straight'}}"
		);
		putStates(
			2149,
			"{Name:'minecraft:spruce_stairs',Properties:{facing:'west',half:'top',shape:'straight'}}",
			"{Name:'minecraft:spruce_stairs',Properties:{facing:'west',half:'top',shape:'inner_left'}}",
			"{Name:'minecraft:spruce_stairs',Properties:{facing:'west',half:'top',shape:'inner_right'}}",
			"{Name:'minecraft:spruce_stairs',Properties:{facing:'west',half:'top',shape:'outer_left'}}",
			"{Name:'minecraft:spruce_stairs',Properties:{facing:'west',half:'top',shape:'outer_right'}}",
			"{Name:'minecraft:spruce_stairs',Properties:{facing:'west',half:'top',shape:'straight'}}"
		);
		putStates(
			2150,
			"{Name:'minecraft:spruce_stairs',Properties:{facing:'south',half:'top',shape:'straight'}}",
			"{Name:'minecraft:spruce_stairs',Properties:{facing:'south',half:'top',shape:'inner_left'}}",
			"{Name:'minecraft:spruce_stairs',Properties:{facing:'south',half:'top',shape:'inner_right'}}",
			"{Name:'minecraft:spruce_stairs',Properties:{facing:'south',half:'top',shape:'outer_left'}}",
			"{Name:'minecraft:spruce_stairs',Properties:{facing:'south',half:'top',shape:'outer_right'}}",
			"{Name:'minecraft:spruce_stairs',Properties:{facing:'south',half:'top',shape:'straight'}}"
		);
		putStates(
			2151,
			"{Name:'minecraft:spruce_stairs',Properties:{facing:'north',half:'top',shape:'straight'}}",
			"{Name:'minecraft:spruce_stairs',Properties:{facing:'north',half:'top',shape:'inner_left'}}",
			"{Name:'minecraft:spruce_stairs',Properties:{facing:'north',half:'top',shape:'inner_right'}}",
			"{Name:'minecraft:spruce_stairs',Properties:{facing:'north',half:'top',shape:'outer_left'}}",
			"{Name:'minecraft:spruce_stairs',Properties:{facing:'north',half:'top',shape:'outer_right'}}",
			"{Name:'minecraft:spruce_stairs',Properties:{facing:'north',half:'top',shape:'straight'}}"
		);
		putStates(
			2160,
			"{Name:'minecraft:birch_stairs',Properties:{facing:'east',half:'bottom',shape:'straight'}}",
			"{Name:'minecraft:birch_stairs',Properties:{facing:'east',half:'bottom',shape:'inner_left'}}",
			"{Name:'minecraft:birch_stairs',Properties:{facing:'east',half:'bottom',shape:'inner_right'}}",
			"{Name:'minecraft:birch_stairs',Properties:{facing:'east',half:'bottom',shape:'outer_left'}}",
			"{Name:'minecraft:birch_stairs',Properties:{facing:'east',half:'bottom',shape:'outer_right'}}",
			"{Name:'minecraft:birch_stairs',Properties:{facing:'east',half:'bottom',shape:'straight'}}"
		);
		putStates(
			2161,
			"{Name:'minecraft:birch_stairs',Properties:{facing:'west',half:'bottom',shape:'straight'}}",
			"{Name:'minecraft:birch_stairs',Properties:{facing:'west',half:'bottom',shape:'inner_left'}}",
			"{Name:'minecraft:birch_stairs',Properties:{facing:'west',half:'bottom',shape:'inner_right'}}",
			"{Name:'minecraft:birch_stairs',Properties:{facing:'west',half:'bottom',shape:'outer_left'}}",
			"{Name:'minecraft:birch_stairs',Properties:{facing:'west',half:'bottom',shape:'outer_right'}}",
			"{Name:'minecraft:birch_stairs',Properties:{facing:'west',half:'bottom',shape:'straight'}}"
		);
		putStates(
			2162,
			"{Name:'minecraft:birch_stairs',Properties:{facing:'south',half:'bottom',shape:'straight'}}",
			"{Name:'minecraft:birch_stairs',Properties:{facing:'south',half:'bottom',shape:'inner_left'}}",
			"{Name:'minecraft:birch_stairs',Properties:{facing:'south',half:'bottom',shape:'inner_right'}}",
			"{Name:'minecraft:birch_stairs',Properties:{facing:'south',half:'bottom',shape:'outer_left'}}",
			"{Name:'minecraft:birch_stairs',Properties:{facing:'south',half:'bottom',shape:'outer_right'}}",
			"{Name:'minecraft:birch_stairs',Properties:{facing:'south',half:'bottom',shape:'straight'}}"
		);
		putStates(
			2163,
			"{Name:'minecraft:birch_stairs',Properties:{facing:'north',half:'bottom',shape:'straight'}}",
			"{Name:'minecraft:birch_stairs',Properties:{facing:'north',half:'bottom',shape:'inner_left'}}",
			"{Name:'minecraft:birch_stairs',Properties:{facing:'north',half:'bottom',shape:'inner_right'}}",
			"{Name:'minecraft:birch_stairs',Properties:{facing:'north',half:'bottom',shape:'outer_left'}}",
			"{Name:'minecraft:birch_stairs',Properties:{facing:'north',half:'bottom',shape:'outer_right'}}",
			"{Name:'minecraft:birch_stairs',Properties:{facing:'north',half:'bottom',shape:'straight'}}"
		);
		putStates(
			2164,
			"{Name:'minecraft:birch_stairs',Properties:{facing:'east',half:'top',shape:'straight'}}",
			"{Name:'minecraft:birch_stairs',Properties:{facing:'east',half:'top',shape:'inner_left'}}",
			"{Name:'minecraft:birch_stairs',Properties:{facing:'east',half:'top',shape:'inner_right'}}",
			"{Name:'minecraft:birch_stairs',Properties:{facing:'east',half:'top',shape:'outer_left'}}",
			"{Name:'minecraft:birch_stairs',Properties:{facing:'east',half:'top',shape:'outer_right'}}",
			"{Name:'minecraft:birch_stairs',Properties:{facing:'east',half:'top',shape:'straight'}}"
		);
		putStates(
			2165,
			"{Name:'minecraft:birch_stairs',Properties:{facing:'west',half:'top',shape:'straight'}}",
			"{Name:'minecraft:birch_stairs',Properties:{facing:'west',half:'top',shape:'inner_left'}}",
			"{Name:'minecraft:birch_stairs',Properties:{facing:'west',half:'top',shape:'inner_right'}}",
			"{Name:'minecraft:birch_stairs',Properties:{facing:'west',half:'top',shape:'outer_left'}}",
			"{Name:'minecraft:birch_stairs',Properties:{facing:'west',half:'top',shape:'outer_right'}}",
			"{Name:'minecraft:birch_stairs',Properties:{facing:'west',half:'top',shape:'straight'}}"
		);
		putStates(
			2166,
			"{Name:'minecraft:birch_stairs',Properties:{facing:'south',half:'top',shape:'straight'}}",
			"{Name:'minecraft:birch_stairs',Properties:{facing:'south',half:'top',shape:'inner_left'}}",
			"{Name:'minecraft:birch_stairs',Properties:{facing:'south',half:'top',shape:'inner_right'}}",
			"{Name:'minecraft:birch_stairs',Properties:{facing:'south',half:'top',shape:'outer_left'}}",
			"{Name:'minecraft:birch_stairs',Properties:{facing:'south',half:'top',shape:'outer_right'}}",
			"{Name:'minecraft:birch_stairs',Properties:{facing:'south',half:'top',shape:'straight'}}"
		);
		putStates(
			2167,
			"{Name:'minecraft:birch_stairs',Properties:{facing:'north',half:'top',shape:'straight'}}",
			"{Name:'minecraft:birch_stairs',Properties:{facing:'north',half:'top',shape:'inner_left'}}",
			"{Name:'minecraft:birch_stairs',Properties:{facing:'north',half:'top',shape:'inner_right'}}",
			"{Name:'minecraft:birch_stairs',Properties:{facing:'north',half:'top',shape:'outer_left'}}",
			"{Name:'minecraft:birch_stairs',Properties:{facing:'north',half:'top',shape:'outer_right'}}",
			"{Name:'minecraft:birch_stairs',Properties:{facing:'north',half:'top',shape:'straight'}}"
		);
		putStates(
			2176,
			"{Name:'minecraft:jungle_stairs',Properties:{facing:'east',half:'bottom',shape:'straight'}}",
			"{Name:'minecraft:jungle_stairs',Properties:{facing:'east',half:'bottom',shape:'inner_left'}}",
			"{Name:'minecraft:jungle_stairs',Properties:{facing:'east',half:'bottom',shape:'inner_right'}}",
			"{Name:'minecraft:jungle_stairs',Properties:{facing:'east',half:'bottom',shape:'outer_left'}}",
			"{Name:'minecraft:jungle_stairs',Properties:{facing:'east',half:'bottom',shape:'outer_right'}}",
			"{Name:'minecraft:jungle_stairs',Properties:{facing:'east',half:'bottom',shape:'straight'}}"
		);
		putStates(
			2177,
			"{Name:'minecraft:jungle_stairs',Properties:{facing:'west',half:'bottom',shape:'straight'}}",
			"{Name:'minecraft:jungle_stairs',Properties:{facing:'west',half:'bottom',shape:'inner_left'}}",
			"{Name:'minecraft:jungle_stairs',Properties:{facing:'west',half:'bottom',shape:'inner_right'}}",
			"{Name:'minecraft:jungle_stairs',Properties:{facing:'west',half:'bottom',shape:'outer_left'}}",
			"{Name:'minecraft:jungle_stairs',Properties:{facing:'west',half:'bottom',shape:'outer_right'}}",
			"{Name:'minecraft:jungle_stairs',Properties:{facing:'west',half:'bottom',shape:'straight'}}"
		);
		putStates(
			2178,
			"{Name:'minecraft:jungle_stairs',Properties:{facing:'south',half:'bottom',shape:'straight'}}",
			"{Name:'minecraft:jungle_stairs',Properties:{facing:'south',half:'bottom',shape:'inner_left'}}",
			"{Name:'minecraft:jungle_stairs',Properties:{facing:'south',half:'bottom',shape:'inner_right'}}",
			"{Name:'minecraft:jungle_stairs',Properties:{facing:'south',half:'bottom',shape:'outer_left'}}",
			"{Name:'minecraft:jungle_stairs',Properties:{facing:'south',half:'bottom',shape:'outer_right'}}",
			"{Name:'minecraft:jungle_stairs',Properties:{facing:'south',half:'bottom',shape:'straight'}}"
		);
		putStates(
			2179,
			"{Name:'minecraft:jungle_stairs',Properties:{facing:'north',half:'bottom',shape:'straight'}}",
			"{Name:'minecraft:jungle_stairs',Properties:{facing:'north',half:'bottom',shape:'inner_left'}}",
			"{Name:'minecraft:jungle_stairs',Properties:{facing:'north',half:'bottom',shape:'inner_right'}}",
			"{Name:'minecraft:jungle_stairs',Properties:{facing:'north',half:'bottom',shape:'outer_left'}}",
			"{Name:'minecraft:jungle_stairs',Properties:{facing:'north',half:'bottom',shape:'outer_right'}}",
			"{Name:'minecraft:jungle_stairs',Properties:{facing:'north',half:'bottom',shape:'straight'}}"
		);
		putStates(
			2180,
			"{Name:'minecraft:jungle_stairs',Properties:{facing:'east',half:'top',shape:'straight'}}",
			"{Name:'minecraft:jungle_stairs',Properties:{facing:'east',half:'top',shape:'inner_left'}}",
			"{Name:'minecraft:jungle_stairs',Properties:{facing:'east',half:'top',shape:'inner_right'}}",
			"{Name:'minecraft:jungle_stairs',Properties:{facing:'east',half:'top',shape:'outer_left'}}",
			"{Name:'minecraft:jungle_stairs',Properties:{facing:'east',half:'top',shape:'outer_right'}}",
			"{Name:'minecraft:jungle_stairs',Properties:{facing:'east',half:'top',shape:'straight'}}"
		);
		putStates(
			2181,
			"{Name:'minecraft:jungle_stairs',Properties:{facing:'west',half:'top',shape:'straight'}}",
			"{Name:'minecraft:jungle_stairs',Properties:{facing:'west',half:'top',shape:'inner_left'}}",
			"{Name:'minecraft:jungle_stairs',Properties:{facing:'west',half:'top',shape:'inner_right'}}",
			"{Name:'minecraft:jungle_stairs',Properties:{facing:'west',half:'top',shape:'outer_left'}}",
			"{Name:'minecraft:jungle_stairs',Properties:{facing:'west',half:'top',shape:'outer_right'}}",
			"{Name:'minecraft:jungle_stairs',Properties:{facing:'west',half:'top',shape:'straight'}}"
		);
		putStates(
			2182,
			"{Name:'minecraft:jungle_stairs',Properties:{facing:'south',half:'top',shape:'straight'}}",
			"{Name:'minecraft:jungle_stairs',Properties:{facing:'south',half:'top',shape:'inner_left'}}",
			"{Name:'minecraft:jungle_stairs',Properties:{facing:'south',half:'top',shape:'inner_right'}}",
			"{Name:'minecraft:jungle_stairs',Properties:{facing:'south',half:'top',shape:'outer_left'}}",
			"{Name:'minecraft:jungle_stairs',Properties:{facing:'south',half:'top',shape:'outer_right'}}",
			"{Name:'minecraft:jungle_stairs',Properties:{facing:'south',half:'top',shape:'straight'}}"
		);
		putStates(
			2183,
			"{Name:'minecraft:jungle_stairs',Properties:{facing:'north',half:'top',shape:'straight'}}",
			"{Name:'minecraft:jungle_stairs',Properties:{facing:'north',half:'top',shape:'inner_left'}}",
			"{Name:'minecraft:jungle_stairs',Properties:{facing:'north',half:'top',shape:'inner_right'}}",
			"{Name:'minecraft:jungle_stairs',Properties:{facing:'north',half:'top',shape:'outer_left'}}",
			"{Name:'minecraft:jungle_stairs',Properties:{facing:'north',half:'top',shape:'outer_right'}}",
			"{Name:'minecraft:jungle_stairs',Properties:{facing:'north',half:'top',shape:'straight'}}"
		);
		putStates(
			2192,
			"{Name:'minecraft:command_block',Properties:{conditional:'false',facing:'down'}}",
			"{Name:'minecraft:command_block',Properties:{conditional:'false',facing:'down'}}"
		);
		putStates(
			2193,
			"{Name:'minecraft:command_block',Properties:{conditional:'false',facing:'up'}}",
			"{Name:'minecraft:command_block',Properties:{conditional:'false',facing:'up'}}"
		);
		putStates(
			2194,
			"{Name:'minecraft:command_block',Properties:{conditional:'false',facing:'north'}}",
			"{Name:'minecraft:command_block',Properties:{conditional:'false',facing:'north'}}"
		);
		putStates(
			2195,
			"{Name:'minecraft:command_block',Properties:{conditional:'false',facing:'south'}}",
			"{Name:'minecraft:command_block',Properties:{conditional:'false',facing:'south'}}"
		);
		putStates(
			2196,
			"{Name:'minecraft:command_block',Properties:{conditional:'false',facing:'west'}}",
			"{Name:'minecraft:command_block',Properties:{conditional:'false',facing:'west'}}"
		);
		putStates(
			2197,
			"{Name:'minecraft:command_block',Properties:{conditional:'false',facing:'east'}}",
			"{Name:'minecraft:command_block',Properties:{conditional:'false',facing:'east'}}"
		);
		putStates(
			2200,
			"{Name:'minecraft:command_block',Properties:{conditional:'true',facing:'down'}}",
			"{Name:'minecraft:command_block',Properties:{conditional:'true',facing:'down'}}"
		);
		putStates(
			2201,
			"{Name:'minecraft:command_block',Properties:{conditional:'true',facing:'up'}}",
			"{Name:'minecraft:command_block',Properties:{conditional:'true',facing:'up'}}"
		);
		putStates(
			2202,
			"{Name:'minecraft:command_block',Properties:{conditional:'true',facing:'north'}}",
			"{Name:'minecraft:command_block',Properties:{conditional:'true',facing:'north'}}"
		);
		putStates(
			2203,
			"{Name:'minecraft:command_block',Properties:{conditional:'true',facing:'south'}}",
			"{Name:'minecraft:command_block',Properties:{conditional:'true',facing:'south'}}"
		);
		putStates(
			2204,
			"{Name:'minecraft:command_block',Properties:{conditional:'true',facing:'west'}}",
			"{Name:'minecraft:command_block',Properties:{conditional:'true',facing:'west'}}"
		);
		putStates(
			2205,
			"{Name:'minecraft:command_block',Properties:{conditional:'true',facing:'east'}}",
			"{Name:'minecraft:command_block',Properties:{conditional:'true',facing:'east'}}"
		);
		putStates(2208, "{Name:'minecraft:beacon'}", "{Name:'minecraft:beacon'}");
		putStates(
			2224,
			"{Name:'minecraft:cobblestone_wall',Properties:{east:'false',north:'false',south:'false',up:'false',west:'false'}}",
			"{Name:'minecraft:cobblestone_wall',Properties:{east:'false',north:'false',south:'false',up:'false',variant:'cobblestone',west:'false'}}",
			"{Name:'minecraft:cobblestone_wall',Properties:{east:'false',north:'false',south:'false',up:'false',variant:'cobblestone',west:'true'}}",
			"{Name:'minecraft:cobblestone_wall',Properties:{east:'false',north:'false',south:'false',up:'true',variant:'cobblestone',west:'false'}}",
			"{Name:'minecraft:cobblestone_wall',Properties:{east:'false',north:'false',south:'false',up:'true',variant:'cobblestone',west:'true'}}",
			"{Name:'minecraft:cobblestone_wall',Properties:{east:'false',north:'false',south:'true',up:'false',variant:'cobblestone',west:'false'}}",
			"{Name:'minecraft:cobblestone_wall',Properties:{east:'false',north:'false',south:'true',up:'false',variant:'cobblestone',west:'true'}}",
			"{Name:'minecraft:cobblestone_wall',Properties:{east:'false',north:'false',south:'true',up:'true',variant:'cobblestone',west:'false'}}",
			"{Name:'minecraft:cobblestone_wall',Properties:{east:'false',north:'false',south:'true',up:'true',variant:'cobblestone',west:'true'}}",
			"{Name:'minecraft:cobblestone_wall',Properties:{east:'false',north:'true',south:'false',up:'false',variant:'cobblestone',west:'false'}}",
			"{Name:'minecraft:cobblestone_wall',Properties:{east:'false',north:'true',south:'false',up:'false',variant:'cobblestone',west:'true'}}",
			"{Name:'minecraft:cobblestone_wall',Properties:{east:'false',north:'true',south:'false',up:'true',variant:'cobblestone',west:'false'}}",
			"{Name:'minecraft:cobblestone_wall',Properties:{east:'false',north:'true',south:'false',up:'true',variant:'cobblestone',west:'true'}}",
			"{Name:'minecraft:cobblestone_wall',Properties:{east:'false',north:'true',south:'true',up:'false',variant:'cobblestone',west:'false'}}",
			"{Name:'minecraft:cobblestone_wall',Properties:{east:'false',north:'true',south:'true',up:'false',variant:'cobblestone',west:'true'}}",
			"{Name:'minecraft:cobblestone_wall',Properties:{east:'false',north:'true',south:'true',up:'true',variant:'cobblestone',west:'false'}}",
			"{Name:'minecraft:cobblestone_wall',Properties:{east:'false',north:'true',south:'true',up:'true',variant:'cobblestone',west:'true'}}",
			"{Name:'minecraft:cobblestone_wall',Properties:{east:'true',north:'false',south:'false',up:'false',variant:'cobblestone',west:'false'}}",
			"{Name:'minecraft:cobblestone_wall',Properties:{east:'true',north:'false',south:'false',up:'false',variant:'cobblestone',west:'true'}}",
			"{Name:'minecraft:cobblestone_wall',Properties:{east:'true',north:'false',south:'false',up:'true',variant:'cobblestone',west:'false'}}",
			"{Name:'minecraft:cobblestone_wall',Properties:{east:'true',north:'false',south:'false',up:'true',variant:'cobblestone',west:'true'}}",
			"{Name:'minecraft:cobblestone_wall',Properties:{east:'true',north:'false',south:'true',up:'false',variant:'cobblestone',west:'false'}}",
			"{Name:'minecraft:cobblestone_wall',Properties:{east:'true',north:'false',south:'true',up:'false',variant:'cobblestone',west:'true'}}",
			"{Name:'minecraft:cobblestone_wall',Properties:{east:'true',north:'false',south:'true',up:'true',variant:'cobblestone',west:'false'}}",
			"{Name:'minecraft:cobblestone_wall',Properties:{east:'true',north:'false',south:'true',up:'true',variant:'cobblestone',west:'true'}}",
			"{Name:'minecraft:cobblestone_wall',Properties:{east:'true',north:'true',south:'false',up:'false',variant:'cobblestone',west:'false'}}",
			"{Name:'minecraft:cobblestone_wall',Properties:{east:'true',north:'true',south:'false',up:'false',variant:'cobblestone',west:'true'}}",
			"{Name:'minecraft:cobblestone_wall',Properties:{east:'true',north:'true',south:'false',up:'true',variant:'cobblestone',west:'false'}}",
			"{Name:'minecraft:cobblestone_wall',Properties:{east:'true',north:'true',south:'false',up:'true',variant:'cobblestone',west:'true'}}",
			"{Name:'minecraft:cobblestone_wall',Properties:{east:'true',north:'true',south:'true',up:'false',variant:'cobblestone',west:'false'}}",
			"{Name:'minecraft:cobblestone_wall',Properties:{east:'true',north:'true',south:'true',up:'false',variant:'cobblestone',west:'true'}}",
			"{Name:'minecraft:cobblestone_wall',Properties:{east:'true',north:'true',south:'true',up:'true',variant:'cobblestone',west:'false'}}",
			"{Name:'minecraft:cobblestone_wall',Properties:{east:'true',north:'true',south:'true',up:'true',variant:'cobblestone',west:'true'}}"
		);
		putStates(
			2225,
			"{Name:'minecraft:mossy_cobblestone_wall',Properties:{east:'false',north:'false',south:'false',up:'false',west:'false'}}",
			"{Name:'minecraft:cobblestone_wall',Properties:{east:'false',north:'false',south:'false',up:'false',variant:'mossy_cobblestone',west:'false'}}",
			"{Name:'minecraft:cobblestone_wall',Properties:{east:'false',north:'false',south:'false',up:'false',variant:'mossy_cobblestone',west:'true'}}",
			"{Name:'minecraft:cobblestone_wall',Properties:{east:'false',north:'false',south:'false',up:'true',variant:'mossy_cobblestone',west:'false'}}",
			"{Name:'minecraft:cobblestone_wall',Properties:{east:'false',north:'false',south:'false',up:'true',variant:'mossy_cobblestone',west:'true'}}",
			"{Name:'minecraft:cobblestone_wall',Properties:{east:'false',north:'false',south:'true',up:'false',variant:'mossy_cobblestone',west:'false'}}",
			"{Name:'minecraft:cobblestone_wall',Properties:{east:'false',north:'false',south:'true',up:'false',variant:'mossy_cobblestone',west:'true'}}",
			"{Name:'minecraft:cobblestone_wall',Properties:{east:'false',north:'false',south:'true',up:'true',variant:'mossy_cobblestone',west:'false'}}",
			"{Name:'minecraft:cobblestone_wall',Properties:{east:'false',north:'false',south:'true',up:'true',variant:'mossy_cobblestone',west:'true'}}",
			"{Name:'minecraft:cobblestone_wall',Properties:{east:'false',north:'true',south:'false',up:'false',variant:'mossy_cobblestone',west:'false'}}",
			"{Name:'minecraft:cobblestone_wall',Properties:{east:'false',north:'true',south:'false',up:'false',variant:'mossy_cobblestone',west:'true'}}",
			"{Name:'minecraft:cobblestone_wall',Properties:{east:'false',north:'true',south:'false',up:'true',variant:'mossy_cobblestone',west:'false'}}",
			"{Name:'minecraft:cobblestone_wall',Properties:{east:'false',north:'true',south:'false',up:'true',variant:'mossy_cobblestone',west:'true'}}",
			"{Name:'minecraft:cobblestone_wall',Properties:{east:'false',north:'true',south:'true',up:'false',variant:'mossy_cobblestone',west:'false'}}",
			"{Name:'minecraft:cobblestone_wall',Properties:{east:'false',north:'true',south:'true',up:'false',variant:'mossy_cobblestone',west:'true'}}",
			"{Name:'minecraft:cobblestone_wall',Properties:{east:'false',north:'true',south:'true',up:'true',variant:'mossy_cobblestone',west:'false'}}",
			"{Name:'minecraft:cobblestone_wall',Properties:{east:'false',north:'true',south:'true',up:'true',variant:'mossy_cobblestone',west:'true'}}",
			"{Name:'minecraft:cobblestone_wall',Properties:{east:'true',north:'false',south:'false',up:'false',variant:'mossy_cobblestone',west:'false'}}",
			"{Name:'minecraft:cobblestone_wall',Properties:{east:'true',north:'false',south:'false',up:'false',variant:'mossy_cobblestone',west:'true'}}",
			"{Name:'minecraft:cobblestone_wall',Properties:{east:'true',north:'false',south:'false',up:'true',variant:'mossy_cobblestone',west:'false'}}",
			"{Name:'minecraft:cobblestone_wall',Properties:{east:'true',north:'false',south:'false',up:'true',variant:'mossy_cobblestone',west:'true'}}",
			"{Name:'minecraft:cobblestone_wall',Properties:{east:'true',north:'false',south:'true',up:'false',variant:'mossy_cobblestone',west:'false'}}",
			"{Name:'minecraft:cobblestone_wall',Properties:{east:'true',north:'false',south:'true',up:'false',variant:'mossy_cobblestone',west:'true'}}",
			"{Name:'minecraft:cobblestone_wall',Properties:{east:'true',north:'false',south:'true',up:'true',variant:'mossy_cobblestone',west:'false'}}",
			"{Name:'minecraft:cobblestone_wall',Properties:{east:'true',north:'false',south:'true',up:'true',variant:'mossy_cobblestone',west:'true'}}",
			"{Name:'minecraft:cobblestone_wall',Properties:{east:'true',north:'true',south:'false',up:'false',variant:'mossy_cobblestone',west:'false'}}",
			"{Name:'minecraft:cobblestone_wall',Properties:{east:'true',north:'true',south:'false',up:'false',variant:'mossy_cobblestone',west:'true'}}",
			"{Name:'minecraft:cobblestone_wall',Properties:{east:'true',north:'true',south:'false',up:'true',variant:'mossy_cobblestone',west:'false'}}",
			"{Name:'minecraft:cobblestone_wall',Properties:{east:'true',north:'true',south:'false',up:'true',variant:'mossy_cobblestone',west:'true'}}",
			"{Name:'minecraft:cobblestone_wall',Properties:{east:'true',north:'true',south:'true',up:'false',variant:'mossy_cobblestone',west:'false'}}",
			"{Name:'minecraft:cobblestone_wall',Properties:{east:'true',north:'true',south:'true',up:'false',variant:'mossy_cobblestone',west:'true'}}",
			"{Name:'minecraft:cobblestone_wall',Properties:{east:'true',north:'true',south:'true',up:'true',variant:'mossy_cobblestone',west:'false'}}",
			"{Name:'minecraft:cobblestone_wall',Properties:{east:'true',north:'true',south:'true',up:'true',variant:'mossy_cobblestone',west:'true'}}"
		);
		putStates(
			2240,
			"{Name:'minecraft:potted_cactus'}",
			"{Name:'minecraft:flower_pot',Properties:{contents:'acacia_sapling',legacy_data:'0'}}",
			"{Name:'minecraft:flower_pot',Properties:{contents:'allium',legacy_data:'0'}}",
			"{Name:'minecraft:flower_pot',Properties:{contents:'birch_sapling',legacy_data:'0'}}",
			"{Name:'minecraft:flower_pot',Properties:{contents:'blue_orchid',legacy_data:'0'}}",
			"{Name:'minecraft:flower_pot',Properties:{contents:'cactus',legacy_data:'0'}}",
			"{Name:'minecraft:flower_pot',Properties:{contents:'dandelion',legacy_data:'0'}}",
			"{Name:'minecraft:flower_pot',Properties:{contents:'dark_oak_sapling',legacy_data:'0'}}",
			"{Name:'minecraft:flower_pot',Properties:{contents:'dead_bush',legacy_data:'0'}}",
			"{Name:'minecraft:flower_pot',Properties:{contents:'empty',legacy_data:'0'}}",
			"{Name:'minecraft:flower_pot',Properties:{contents:'fern',legacy_data:'0'}}",
			"{Name:'minecraft:flower_pot',Properties:{contents:'houstonia',legacy_data:'0'}}",
			"{Name:'minecraft:flower_pot',Properties:{contents:'jungle_sapling',legacy_data:'0'}}",
			"{Name:'minecraft:flower_pot',Properties:{contents:'mushroom_brown',legacy_data:'0'}}",
			"{Name:'minecraft:flower_pot',Properties:{contents:'mushroom_red',legacy_data:'0'}}",
			"{Name:'minecraft:flower_pot',Properties:{contents:'oak_sapling',legacy_data:'0'}}",
			"{Name:'minecraft:flower_pot',Properties:{contents:'orange_tulip',legacy_data:'0'}}",
			"{Name:'minecraft:flower_pot',Properties:{contents:'oxeye_daisy',legacy_data:'0'}}",
			"{Name:'minecraft:flower_pot',Properties:{contents:'pink_tulip',legacy_data:'0'}}",
			"{Name:'minecraft:flower_pot',Properties:{contents:'red_tulip',legacy_data:'0'}}",
			"{Name:'minecraft:flower_pot',Properties:{contents:'rose',legacy_data:'0'}}",
			"{Name:'minecraft:flower_pot',Properties:{contents:'spruce_sapling',legacy_data:'0'}}",
			"{Name:'minecraft:flower_pot',Properties:{contents:'white_tulip',legacy_data:'0'}}"
		);
		putStates(
			2241,
			"{Name:'minecraft:potted_cactus'}",
			"{Name:'minecraft:flower_pot',Properties:{contents:'acacia_sapling',legacy_data:'1'}}",
			"{Name:'minecraft:flower_pot',Properties:{contents:'allium',legacy_data:'1'}}",
			"{Name:'minecraft:flower_pot',Properties:{contents:'birch_sapling',legacy_data:'1'}}",
			"{Name:'minecraft:flower_pot',Properties:{contents:'blue_orchid',legacy_data:'1'}}",
			"{Name:'minecraft:flower_pot',Properties:{contents:'cactus',legacy_data:'1'}}",
			"{Name:'minecraft:flower_pot',Properties:{contents:'dandelion',legacy_data:'1'}}",
			"{Name:'minecraft:flower_pot',Properties:{contents:'dark_oak_sapling',legacy_data:'1'}}",
			"{Name:'minecraft:flower_pot',Properties:{contents:'dead_bush',legacy_data:'1'}}",
			"{Name:'minecraft:flower_pot',Properties:{contents:'empty',legacy_data:'1'}}",
			"{Name:'minecraft:flower_pot',Properties:{contents:'fern',legacy_data:'1'}}",
			"{Name:'minecraft:flower_pot',Properties:{contents:'houstonia',legacy_data:'1'}}",
			"{Name:'minecraft:flower_pot',Properties:{contents:'jungle_sapling',legacy_data:'1'}}",
			"{Name:'minecraft:flower_pot',Properties:{contents:'mushroom_brown',legacy_data:'1'}}",
			"{Name:'minecraft:flower_pot',Properties:{contents:'mushroom_red',legacy_data:'1'}}",
			"{Name:'minecraft:flower_pot',Properties:{contents:'oak_sapling',legacy_data:'1'}}",
			"{Name:'minecraft:flower_pot',Properties:{contents:'orange_tulip',legacy_data:'1'}}",
			"{Name:'minecraft:flower_pot',Properties:{contents:'oxeye_daisy',legacy_data:'1'}}",
			"{Name:'minecraft:flower_pot',Properties:{contents:'pink_tulip',legacy_data:'1'}}",
			"{Name:'minecraft:flower_pot',Properties:{contents:'red_tulip',legacy_data:'1'}}",
			"{Name:'minecraft:flower_pot',Properties:{contents:'rose',legacy_data:'1'}}",
			"{Name:'minecraft:flower_pot',Properties:{contents:'spruce_sapling',legacy_data:'1'}}",
			"{Name:'minecraft:flower_pot',Properties:{contents:'white_tulip',legacy_data:'1'}}"
		);
		putStates(
			2242,
			"{Name:'minecraft:potted_cactus'}",
			"{Name:'minecraft:flower_pot',Properties:{contents:'acacia_sapling',legacy_data:'2'}}",
			"{Name:'minecraft:flower_pot',Properties:{contents:'allium',legacy_data:'2'}}",
			"{Name:'minecraft:flower_pot',Properties:{contents:'birch_sapling',legacy_data:'2'}}",
			"{Name:'minecraft:flower_pot',Properties:{contents:'blue_orchid',legacy_data:'2'}}",
			"{Name:'minecraft:flower_pot',Properties:{contents:'cactus',legacy_data:'2'}}",
			"{Name:'minecraft:flower_pot',Properties:{contents:'dandelion',legacy_data:'2'}}",
			"{Name:'minecraft:flower_pot',Properties:{contents:'dark_oak_sapling',legacy_data:'2'}}",
			"{Name:'minecraft:flower_pot',Properties:{contents:'dead_bush',legacy_data:'2'}}",
			"{Name:'minecraft:flower_pot',Properties:{contents:'empty',legacy_data:'2'}}",
			"{Name:'minecraft:flower_pot',Properties:{contents:'fern',legacy_data:'2'}}",
			"{Name:'minecraft:flower_pot',Properties:{contents:'houstonia',legacy_data:'2'}}",
			"{Name:'minecraft:flower_pot',Properties:{contents:'jungle_sapling',legacy_data:'2'}}",
			"{Name:'minecraft:flower_pot',Properties:{contents:'mushroom_brown',legacy_data:'2'}}",
			"{Name:'minecraft:flower_pot',Properties:{contents:'mushroom_red',legacy_data:'2'}}",
			"{Name:'minecraft:flower_pot',Properties:{contents:'oak_sapling',legacy_data:'2'}}",
			"{Name:'minecraft:flower_pot',Properties:{contents:'orange_tulip',legacy_data:'2'}}",
			"{Name:'minecraft:flower_pot',Properties:{contents:'oxeye_daisy',legacy_data:'2'}}",
			"{Name:'minecraft:flower_pot',Properties:{contents:'pink_tulip',legacy_data:'2'}}",
			"{Name:'minecraft:flower_pot',Properties:{contents:'red_tulip',legacy_data:'2'}}",
			"{Name:'minecraft:flower_pot',Properties:{contents:'rose',legacy_data:'2'}}",
			"{Name:'minecraft:flower_pot',Properties:{contents:'spruce_sapling',legacy_data:'2'}}",
			"{Name:'minecraft:flower_pot',Properties:{contents:'white_tulip',legacy_data:'2'}}"
		);
		putStates(
			2243,
			"{Name:'minecraft:potted_cactus'}",
			"{Name:'minecraft:flower_pot',Properties:{contents:'acacia_sapling',legacy_data:'3'}}",
			"{Name:'minecraft:flower_pot',Properties:{contents:'allium',legacy_data:'3'}}",
			"{Name:'minecraft:flower_pot',Properties:{contents:'birch_sapling',legacy_data:'3'}}",
			"{Name:'minecraft:flower_pot',Properties:{contents:'blue_orchid',legacy_data:'3'}}",
			"{Name:'minecraft:flower_pot',Properties:{contents:'cactus',legacy_data:'3'}}",
			"{Name:'minecraft:flower_pot',Properties:{contents:'dandelion',legacy_data:'3'}}",
			"{Name:'minecraft:flower_pot',Properties:{contents:'dark_oak_sapling',legacy_data:'3'}}",
			"{Name:'minecraft:flower_pot',Properties:{contents:'dead_bush',legacy_data:'3'}}",
			"{Name:'minecraft:flower_pot',Properties:{contents:'empty',legacy_data:'3'}}",
			"{Name:'minecraft:flower_pot',Properties:{contents:'fern',legacy_data:'3'}}",
			"{Name:'minecraft:flower_pot',Properties:{contents:'houstonia',legacy_data:'3'}}",
			"{Name:'minecraft:flower_pot',Properties:{contents:'jungle_sapling',legacy_data:'3'}}",
			"{Name:'minecraft:flower_pot',Properties:{contents:'mushroom_brown',legacy_data:'3'}}",
			"{Name:'minecraft:flower_pot',Properties:{contents:'mushroom_red',legacy_data:'3'}}",
			"{Name:'minecraft:flower_pot',Properties:{contents:'oak_sapling',legacy_data:'3'}}",
			"{Name:'minecraft:flower_pot',Properties:{contents:'orange_tulip',legacy_data:'3'}}",
			"{Name:'minecraft:flower_pot',Properties:{contents:'oxeye_daisy',legacy_data:'3'}}",
			"{Name:'minecraft:flower_pot',Properties:{contents:'pink_tulip',legacy_data:'3'}}",
			"{Name:'minecraft:flower_pot',Properties:{contents:'red_tulip',legacy_data:'3'}}",
			"{Name:'minecraft:flower_pot',Properties:{contents:'rose',legacy_data:'3'}}",
			"{Name:'minecraft:flower_pot',Properties:{contents:'spruce_sapling',legacy_data:'3'}}",
			"{Name:'minecraft:flower_pot',Properties:{contents:'white_tulip',legacy_data:'3'}}"
		);
		putStates(
			2244,
			"{Name:'minecraft:potted_cactus'}",
			"{Name:'minecraft:flower_pot',Properties:{contents:'acacia_sapling',legacy_data:'4'}}",
			"{Name:'minecraft:flower_pot',Properties:{contents:'allium',legacy_data:'4'}}",
			"{Name:'minecraft:flower_pot',Properties:{contents:'birch_sapling',legacy_data:'4'}}",
			"{Name:'minecraft:flower_pot',Properties:{contents:'blue_orchid',legacy_data:'4'}}",
			"{Name:'minecraft:flower_pot',Properties:{contents:'cactus',legacy_data:'4'}}",
			"{Name:'minecraft:flower_pot',Properties:{contents:'dandelion',legacy_data:'4'}}",
			"{Name:'minecraft:flower_pot',Properties:{contents:'dark_oak_sapling',legacy_data:'4'}}",
			"{Name:'minecraft:flower_pot',Properties:{contents:'dead_bush',legacy_data:'4'}}",
			"{Name:'minecraft:flower_pot',Properties:{contents:'empty',legacy_data:'4'}}",
			"{Name:'minecraft:flower_pot',Properties:{contents:'fern',legacy_data:'4'}}",
			"{Name:'minecraft:flower_pot',Properties:{contents:'houstonia',legacy_data:'4'}}",
			"{Name:'minecraft:flower_pot',Properties:{contents:'jungle_sapling',legacy_data:'4'}}",
			"{Name:'minecraft:flower_pot',Properties:{contents:'mushroom_brown',legacy_data:'4'}}",
			"{Name:'minecraft:flower_pot',Properties:{contents:'mushroom_red',legacy_data:'4'}}",
			"{Name:'minecraft:flower_pot',Properties:{contents:'oak_sapling',legacy_data:'4'}}",
			"{Name:'minecraft:flower_pot',Properties:{contents:'orange_tulip',legacy_data:'4'}}",
			"{Name:'minecraft:flower_pot',Properties:{contents:'oxeye_daisy',legacy_data:'4'}}",
			"{Name:'minecraft:flower_pot',Properties:{contents:'pink_tulip',legacy_data:'4'}}",
			"{Name:'minecraft:flower_pot',Properties:{contents:'red_tulip',legacy_data:'4'}}",
			"{Name:'minecraft:flower_pot',Properties:{contents:'rose',legacy_data:'4'}}",
			"{Name:'minecraft:flower_pot',Properties:{contents:'spruce_sapling',legacy_data:'4'}}",
			"{Name:'minecraft:flower_pot',Properties:{contents:'white_tulip',legacy_data:'4'}}"
		);
		putStates(
			2245,
			"{Name:'minecraft:potted_cactus'}",
			"{Name:'minecraft:flower_pot',Properties:{contents:'acacia_sapling',legacy_data:'5'}}",
			"{Name:'minecraft:flower_pot',Properties:{contents:'allium',legacy_data:'5'}}",
			"{Name:'minecraft:flower_pot',Properties:{contents:'birch_sapling',legacy_data:'5'}}",
			"{Name:'minecraft:flower_pot',Properties:{contents:'blue_orchid',legacy_data:'5'}}",
			"{Name:'minecraft:flower_pot',Properties:{contents:'cactus',legacy_data:'5'}}",
			"{Name:'minecraft:flower_pot',Properties:{contents:'dandelion',legacy_data:'5'}}",
			"{Name:'minecraft:flower_pot',Properties:{contents:'dark_oak_sapling',legacy_data:'5'}}",
			"{Name:'minecraft:flower_pot',Properties:{contents:'dead_bush',legacy_data:'5'}}",
			"{Name:'minecraft:flower_pot',Properties:{contents:'empty',legacy_data:'5'}}",
			"{Name:'minecraft:flower_pot',Properties:{contents:'fern',legacy_data:'5'}}",
			"{Name:'minecraft:flower_pot',Properties:{contents:'houstonia',legacy_data:'5'}}",
			"{Name:'minecraft:flower_pot',Properties:{contents:'jungle_sapling',legacy_data:'5'}}",
			"{Name:'minecraft:flower_pot',Properties:{contents:'mushroom_brown',legacy_data:'5'}}",
			"{Name:'minecraft:flower_pot',Properties:{contents:'mushroom_red',legacy_data:'5'}}",
			"{Name:'minecraft:flower_pot',Properties:{contents:'oak_sapling',legacy_data:'5'}}",
			"{Name:'minecraft:flower_pot',Properties:{contents:'orange_tulip',legacy_data:'5'}}",
			"{Name:'minecraft:flower_pot',Properties:{contents:'oxeye_daisy',legacy_data:'5'}}",
			"{Name:'minecraft:flower_pot',Properties:{contents:'pink_tulip',legacy_data:'5'}}",
			"{Name:'minecraft:flower_pot',Properties:{contents:'red_tulip',legacy_data:'5'}}",
			"{Name:'minecraft:flower_pot',Properties:{contents:'rose',legacy_data:'5'}}",
			"{Name:'minecraft:flower_pot',Properties:{contents:'spruce_sapling',legacy_data:'5'}}",
			"{Name:'minecraft:flower_pot',Properties:{contents:'white_tulip',legacy_data:'5'}}"
		);
		putStates(
			2246,
			"{Name:'minecraft:potted_cactus'}",
			"{Name:'minecraft:flower_pot',Properties:{contents:'acacia_sapling',legacy_data:'6'}}",
			"{Name:'minecraft:flower_pot',Properties:{contents:'allium',legacy_data:'6'}}",
			"{Name:'minecraft:flower_pot',Properties:{contents:'birch_sapling',legacy_data:'6'}}",
			"{Name:'minecraft:flower_pot',Properties:{contents:'blue_orchid',legacy_data:'6'}}",
			"{Name:'minecraft:flower_pot',Properties:{contents:'cactus',legacy_data:'6'}}",
			"{Name:'minecraft:flower_pot',Properties:{contents:'dandelion',legacy_data:'6'}}",
			"{Name:'minecraft:flower_pot',Properties:{contents:'dark_oak_sapling',legacy_data:'6'}}",
			"{Name:'minecraft:flower_pot',Properties:{contents:'dead_bush',legacy_data:'6'}}",
			"{Name:'minecraft:flower_pot',Properties:{contents:'empty',legacy_data:'6'}}",
			"{Name:'minecraft:flower_pot',Properties:{contents:'fern',legacy_data:'6'}}",
			"{Name:'minecraft:flower_pot',Properties:{contents:'houstonia',legacy_data:'6'}}",
			"{Name:'minecraft:flower_pot',Properties:{contents:'jungle_sapling',legacy_data:'6'}}",
			"{Name:'minecraft:flower_pot',Properties:{contents:'mushroom_brown',legacy_data:'6'}}",
			"{Name:'minecraft:flower_pot',Properties:{contents:'mushroom_red',legacy_data:'6'}}",
			"{Name:'minecraft:flower_pot',Properties:{contents:'oak_sapling',legacy_data:'6'}}",
			"{Name:'minecraft:flower_pot',Properties:{contents:'orange_tulip',legacy_data:'6'}}",
			"{Name:'minecraft:flower_pot',Properties:{contents:'oxeye_daisy',legacy_data:'6'}}",
			"{Name:'minecraft:flower_pot',Properties:{contents:'pink_tulip',legacy_data:'6'}}",
			"{Name:'minecraft:flower_pot',Properties:{contents:'red_tulip',legacy_data:'6'}}",
			"{Name:'minecraft:flower_pot',Properties:{contents:'rose',legacy_data:'6'}}",
			"{Name:'minecraft:flower_pot',Properties:{contents:'spruce_sapling',legacy_data:'6'}}",
			"{Name:'minecraft:flower_pot',Properties:{contents:'white_tulip',legacy_data:'6'}}"
		);
		putStates(
			2247,
			"{Name:'minecraft:potted_cactus'}",
			"{Name:'minecraft:flower_pot',Properties:{contents:'acacia_sapling',legacy_data:'7'}}",
			"{Name:'minecraft:flower_pot',Properties:{contents:'allium',legacy_data:'7'}}",
			"{Name:'minecraft:flower_pot',Properties:{contents:'birch_sapling',legacy_data:'7'}}",
			"{Name:'minecraft:flower_pot',Properties:{contents:'blue_orchid',legacy_data:'7'}}",
			"{Name:'minecraft:flower_pot',Properties:{contents:'cactus',legacy_data:'7'}}",
			"{Name:'minecraft:flower_pot',Properties:{contents:'dandelion',legacy_data:'7'}}",
			"{Name:'minecraft:flower_pot',Properties:{contents:'dark_oak_sapling',legacy_data:'7'}}",
			"{Name:'minecraft:flower_pot',Properties:{contents:'dead_bush',legacy_data:'7'}}",
			"{Name:'minecraft:flower_pot',Properties:{contents:'empty',legacy_data:'7'}}",
			"{Name:'minecraft:flower_pot',Properties:{contents:'fern',legacy_data:'7'}}",
			"{Name:'minecraft:flower_pot',Properties:{contents:'houstonia',legacy_data:'7'}}",
			"{Name:'minecraft:flower_pot',Properties:{contents:'jungle_sapling',legacy_data:'7'}}",
			"{Name:'minecraft:flower_pot',Properties:{contents:'mushroom_brown',legacy_data:'7'}}",
			"{Name:'minecraft:flower_pot',Properties:{contents:'mushroom_red',legacy_data:'7'}}",
			"{Name:'minecraft:flower_pot',Properties:{contents:'oak_sapling',legacy_data:'7'}}",
			"{Name:'minecraft:flower_pot',Properties:{contents:'orange_tulip',legacy_data:'7'}}",
			"{Name:'minecraft:flower_pot',Properties:{contents:'oxeye_daisy',legacy_data:'7'}}",
			"{Name:'minecraft:flower_pot',Properties:{contents:'pink_tulip',legacy_data:'7'}}",
			"{Name:'minecraft:flower_pot',Properties:{contents:'red_tulip',legacy_data:'7'}}",
			"{Name:'minecraft:flower_pot',Properties:{contents:'rose',legacy_data:'7'}}",
			"{Name:'minecraft:flower_pot',Properties:{contents:'spruce_sapling',legacy_data:'7'}}",
			"{Name:'minecraft:flower_pot',Properties:{contents:'white_tulip',legacy_data:'7'}}"
		);
		putStates(
			2248,
			"{Name:'minecraft:potted_cactus'}",
			"{Name:'minecraft:flower_pot',Properties:{contents:'acacia_sapling',legacy_data:'8'}}",
			"{Name:'minecraft:flower_pot',Properties:{contents:'allium',legacy_data:'8'}}",
			"{Name:'minecraft:flower_pot',Properties:{contents:'birch_sapling',legacy_data:'8'}}",
			"{Name:'minecraft:flower_pot',Properties:{contents:'blue_orchid',legacy_data:'8'}}",
			"{Name:'minecraft:flower_pot',Properties:{contents:'cactus',legacy_data:'8'}}",
			"{Name:'minecraft:flower_pot',Properties:{contents:'dandelion',legacy_data:'8'}}",
			"{Name:'minecraft:flower_pot',Properties:{contents:'dark_oak_sapling',legacy_data:'8'}}",
			"{Name:'minecraft:flower_pot',Properties:{contents:'dead_bush',legacy_data:'8'}}",
			"{Name:'minecraft:flower_pot',Properties:{contents:'empty',legacy_data:'8'}}",
			"{Name:'minecraft:flower_pot',Properties:{contents:'fern',legacy_data:'8'}}",
			"{Name:'minecraft:flower_pot',Properties:{contents:'houstonia',legacy_data:'8'}}",
			"{Name:'minecraft:flower_pot',Properties:{contents:'jungle_sapling',legacy_data:'8'}}",
			"{Name:'minecraft:flower_pot',Properties:{contents:'mushroom_brown',legacy_data:'8'}}",
			"{Name:'minecraft:flower_pot',Properties:{contents:'mushroom_red',legacy_data:'8'}}",
			"{Name:'minecraft:flower_pot',Properties:{contents:'oak_sapling',legacy_data:'8'}}",
			"{Name:'minecraft:flower_pot',Properties:{contents:'orange_tulip',legacy_data:'8'}}",
			"{Name:'minecraft:flower_pot',Properties:{contents:'oxeye_daisy',legacy_data:'8'}}",
			"{Name:'minecraft:flower_pot',Properties:{contents:'pink_tulip',legacy_data:'8'}}",
			"{Name:'minecraft:flower_pot',Properties:{contents:'red_tulip',legacy_data:'8'}}",
			"{Name:'minecraft:flower_pot',Properties:{contents:'rose',legacy_data:'8'}}",
			"{Name:'minecraft:flower_pot',Properties:{contents:'spruce_sapling',legacy_data:'8'}}",
			"{Name:'minecraft:flower_pot',Properties:{contents:'white_tulip',legacy_data:'8'}}"
		);
		putStates(
			2249,
			"{Name:'minecraft:potted_cactus'}",
			"{Name:'minecraft:flower_pot',Properties:{contents:'acacia_sapling',legacy_data:'9'}}",
			"{Name:'minecraft:flower_pot',Properties:{contents:'allium',legacy_data:'9'}}",
			"{Name:'minecraft:flower_pot',Properties:{contents:'birch_sapling',legacy_data:'9'}}",
			"{Name:'minecraft:flower_pot',Properties:{contents:'blue_orchid',legacy_data:'9'}}",
			"{Name:'minecraft:flower_pot',Properties:{contents:'cactus',legacy_data:'9'}}",
			"{Name:'minecraft:flower_pot',Properties:{contents:'dandelion',legacy_data:'9'}}",
			"{Name:'minecraft:flower_pot',Properties:{contents:'dark_oak_sapling',legacy_data:'9'}}",
			"{Name:'minecraft:flower_pot',Properties:{contents:'dead_bush',legacy_data:'9'}}",
			"{Name:'minecraft:flower_pot',Properties:{contents:'empty',legacy_data:'9'}}",
			"{Name:'minecraft:flower_pot',Properties:{contents:'fern',legacy_data:'9'}}",
			"{Name:'minecraft:flower_pot',Properties:{contents:'houstonia',legacy_data:'9'}}",
			"{Name:'minecraft:flower_pot',Properties:{contents:'jungle_sapling',legacy_data:'9'}}",
			"{Name:'minecraft:flower_pot',Properties:{contents:'mushroom_brown',legacy_data:'9'}}",
			"{Name:'minecraft:flower_pot',Properties:{contents:'mushroom_red',legacy_data:'9'}}",
			"{Name:'minecraft:flower_pot',Properties:{contents:'oak_sapling',legacy_data:'9'}}",
			"{Name:'minecraft:flower_pot',Properties:{contents:'orange_tulip',legacy_data:'9'}}",
			"{Name:'minecraft:flower_pot',Properties:{contents:'oxeye_daisy',legacy_data:'9'}}",
			"{Name:'minecraft:flower_pot',Properties:{contents:'pink_tulip',legacy_data:'9'}}",
			"{Name:'minecraft:flower_pot',Properties:{contents:'red_tulip',legacy_data:'9'}}",
			"{Name:'minecraft:flower_pot',Properties:{contents:'rose',legacy_data:'9'}}",
			"{Name:'minecraft:flower_pot',Properties:{contents:'spruce_sapling',legacy_data:'9'}}",
			"{Name:'minecraft:flower_pot',Properties:{contents:'white_tulip',legacy_data:'9'}}"
		);
		putStates(
			2250,
			"{Name:'minecraft:potted_cactus'}",
			"{Name:'minecraft:flower_pot',Properties:{contents:'acacia_sapling',legacy_data:'10'}}",
			"{Name:'minecraft:flower_pot',Properties:{contents:'allium',legacy_data:'10'}}",
			"{Name:'minecraft:flower_pot',Properties:{contents:'birch_sapling',legacy_data:'10'}}",
			"{Name:'minecraft:flower_pot',Properties:{contents:'blue_orchid',legacy_data:'10'}}",
			"{Name:'minecraft:flower_pot',Properties:{contents:'cactus',legacy_data:'10'}}",
			"{Name:'minecraft:flower_pot',Properties:{contents:'dandelion',legacy_data:'10'}}",
			"{Name:'minecraft:flower_pot',Properties:{contents:'dark_oak_sapling',legacy_data:'10'}}",
			"{Name:'minecraft:flower_pot',Properties:{contents:'dead_bush',legacy_data:'10'}}",
			"{Name:'minecraft:flower_pot',Properties:{contents:'empty',legacy_data:'10'}}",
			"{Name:'minecraft:flower_pot',Properties:{contents:'fern',legacy_data:'10'}}",
			"{Name:'minecraft:flower_pot',Properties:{contents:'houstonia',legacy_data:'10'}}",
			"{Name:'minecraft:flower_pot',Properties:{contents:'jungle_sapling',legacy_data:'10'}}",
			"{Name:'minecraft:flower_pot',Properties:{contents:'mushroom_brown',legacy_data:'10'}}",
			"{Name:'minecraft:flower_pot',Properties:{contents:'mushroom_red',legacy_data:'10'}}",
			"{Name:'minecraft:flower_pot',Properties:{contents:'oak_sapling',legacy_data:'10'}}",
			"{Name:'minecraft:flower_pot',Properties:{contents:'orange_tulip',legacy_data:'10'}}",
			"{Name:'minecraft:flower_pot',Properties:{contents:'oxeye_daisy',legacy_data:'10'}}",
			"{Name:'minecraft:flower_pot',Properties:{contents:'pink_tulip',legacy_data:'10'}}",
			"{Name:'minecraft:flower_pot',Properties:{contents:'red_tulip',legacy_data:'10'}}",
			"{Name:'minecraft:flower_pot',Properties:{contents:'rose',legacy_data:'10'}}",
			"{Name:'minecraft:flower_pot',Properties:{contents:'spruce_sapling',legacy_data:'10'}}",
			"{Name:'minecraft:flower_pot',Properties:{contents:'white_tulip',legacy_data:'10'}}"
		);
		putStates(
			2251,
			"{Name:'minecraft:potted_cactus'}",
			"{Name:'minecraft:flower_pot',Properties:{contents:'acacia_sapling',legacy_data:'11'}}",
			"{Name:'minecraft:flower_pot',Properties:{contents:'allium',legacy_data:'11'}}",
			"{Name:'minecraft:flower_pot',Properties:{contents:'birch_sapling',legacy_data:'11'}}",
			"{Name:'minecraft:flower_pot',Properties:{contents:'blue_orchid',legacy_data:'11'}}",
			"{Name:'minecraft:flower_pot',Properties:{contents:'cactus',legacy_data:'11'}}",
			"{Name:'minecraft:flower_pot',Properties:{contents:'dandelion',legacy_data:'11'}}",
			"{Name:'minecraft:flower_pot',Properties:{contents:'dark_oak_sapling',legacy_data:'11'}}",
			"{Name:'minecraft:flower_pot',Properties:{contents:'dead_bush',legacy_data:'11'}}",
			"{Name:'minecraft:flower_pot',Properties:{contents:'empty',legacy_data:'11'}}",
			"{Name:'minecraft:flower_pot',Properties:{contents:'fern',legacy_data:'11'}}",
			"{Name:'minecraft:flower_pot',Properties:{contents:'houstonia',legacy_data:'11'}}",
			"{Name:'minecraft:flower_pot',Properties:{contents:'jungle_sapling',legacy_data:'11'}}",
			"{Name:'minecraft:flower_pot',Properties:{contents:'mushroom_brown',legacy_data:'11'}}",
			"{Name:'minecraft:flower_pot',Properties:{contents:'mushroom_red',legacy_data:'11'}}",
			"{Name:'minecraft:flower_pot',Properties:{contents:'oak_sapling',legacy_data:'11'}}",
			"{Name:'minecraft:flower_pot',Properties:{contents:'orange_tulip',legacy_data:'11'}}",
			"{Name:'minecraft:flower_pot',Properties:{contents:'oxeye_daisy',legacy_data:'11'}}",
			"{Name:'minecraft:flower_pot',Properties:{contents:'pink_tulip',legacy_data:'11'}}",
			"{Name:'minecraft:flower_pot',Properties:{contents:'red_tulip',legacy_data:'11'}}",
			"{Name:'minecraft:flower_pot',Properties:{contents:'rose',legacy_data:'11'}}",
			"{Name:'minecraft:flower_pot',Properties:{contents:'spruce_sapling',legacy_data:'11'}}",
			"{Name:'minecraft:flower_pot',Properties:{contents:'white_tulip',legacy_data:'11'}}"
		);
		putStates(
			2252,
			"{Name:'minecraft:potted_cactus'}",
			"{Name:'minecraft:flower_pot',Properties:{contents:'acacia_sapling',legacy_data:'12'}}",
			"{Name:'minecraft:flower_pot',Properties:{contents:'allium',legacy_data:'12'}}",
			"{Name:'minecraft:flower_pot',Properties:{contents:'birch_sapling',legacy_data:'12'}}",
			"{Name:'minecraft:flower_pot',Properties:{contents:'blue_orchid',legacy_data:'12'}}",
			"{Name:'minecraft:flower_pot',Properties:{contents:'cactus',legacy_data:'12'}}",
			"{Name:'minecraft:flower_pot',Properties:{contents:'dandelion',legacy_data:'12'}}",
			"{Name:'minecraft:flower_pot',Properties:{contents:'dark_oak_sapling',legacy_data:'12'}}",
			"{Name:'minecraft:flower_pot',Properties:{contents:'dead_bush',legacy_data:'12'}}",
			"{Name:'minecraft:flower_pot',Properties:{contents:'empty',legacy_data:'12'}}",
			"{Name:'minecraft:flower_pot',Properties:{contents:'fern',legacy_data:'12'}}",
			"{Name:'minecraft:flower_pot',Properties:{contents:'houstonia',legacy_data:'12'}}",
			"{Name:'minecraft:flower_pot',Properties:{contents:'jungle_sapling',legacy_data:'12'}}",
			"{Name:'minecraft:flower_pot',Properties:{contents:'mushroom_brown',legacy_data:'12'}}",
			"{Name:'minecraft:flower_pot',Properties:{contents:'mushroom_red',legacy_data:'12'}}",
			"{Name:'minecraft:flower_pot',Properties:{contents:'oak_sapling',legacy_data:'12'}}",
			"{Name:'minecraft:flower_pot',Properties:{contents:'orange_tulip',legacy_data:'12'}}",
			"{Name:'minecraft:flower_pot',Properties:{contents:'oxeye_daisy',legacy_data:'12'}}",
			"{Name:'minecraft:flower_pot',Properties:{contents:'pink_tulip',legacy_data:'12'}}",
			"{Name:'minecraft:flower_pot',Properties:{contents:'red_tulip',legacy_data:'12'}}",
			"{Name:'minecraft:flower_pot',Properties:{contents:'rose',legacy_data:'12'}}",
			"{Name:'minecraft:flower_pot',Properties:{contents:'spruce_sapling',legacy_data:'12'}}",
			"{Name:'minecraft:flower_pot',Properties:{contents:'white_tulip',legacy_data:'12'}}"
		);
		putStates(
			2253,
			"{Name:'minecraft:potted_cactus'}",
			"{Name:'minecraft:flower_pot',Properties:{contents:'acacia_sapling',legacy_data:'13'}}",
			"{Name:'minecraft:flower_pot',Properties:{contents:'allium',legacy_data:'13'}}",
			"{Name:'minecraft:flower_pot',Properties:{contents:'birch_sapling',legacy_data:'13'}}",
			"{Name:'minecraft:flower_pot',Properties:{contents:'blue_orchid',legacy_data:'13'}}",
			"{Name:'minecraft:flower_pot',Properties:{contents:'cactus',legacy_data:'13'}}",
			"{Name:'minecraft:flower_pot',Properties:{contents:'dandelion',legacy_data:'13'}}",
			"{Name:'minecraft:flower_pot',Properties:{contents:'dark_oak_sapling',legacy_data:'13'}}",
			"{Name:'minecraft:flower_pot',Properties:{contents:'dead_bush',legacy_data:'13'}}",
			"{Name:'minecraft:flower_pot',Properties:{contents:'empty',legacy_data:'13'}}",
			"{Name:'minecraft:flower_pot',Properties:{contents:'fern',legacy_data:'13'}}",
			"{Name:'minecraft:flower_pot',Properties:{contents:'houstonia',legacy_data:'13'}}",
			"{Name:'minecraft:flower_pot',Properties:{contents:'jungle_sapling',legacy_data:'13'}}",
			"{Name:'minecraft:flower_pot',Properties:{contents:'mushroom_brown',legacy_data:'13'}}",
			"{Name:'minecraft:flower_pot',Properties:{contents:'mushroom_red',legacy_data:'13'}}",
			"{Name:'minecraft:flower_pot',Properties:{contents:'oak_sapling',legacy_data:'13'}}",
			"{Name:'minecraft:flower_pot',Properties:{contents:'orange_tulip',legacy_data:'13'}}",
			"{Name:'minecraft:flower_pot',Properties:{contents:'oxeye_daisy',legacy_data:'13'}}",
			"{Name:'minecraft:flower_pot',Properties:{contents:'pink_tulip',legacy_data:'13'}}",
			"{Name:'minecraft:flower_pot',Properties:{contents:'red_tulip',legacy_data:'13'}}",
			"{Name:'minecraft:flower_pot',Properties:{contents:'rose',legacy_data:'13'}}",
			"{Name:'minecraft:flower_pot',Properties:{contents:'spruce_sapling',legacy_data:'13'}}",
			"{Name:'minecraft:flower_pot',Properties:{contents:'white_tulip',legacy_data:'13'}}"
		);
		putStates(
			2254,
			"{Name:'minecraft:potted_cactus'}",
			"{Name:'minecraft:flower_pot',Properties:{contents:'acacia_sapling',legacy_data:'14'}}",
			"{Name:'minecraft:flower_pot',Properties:{contents:'allium',legacy_data:'14'}}",
			"{Name:'minecraft:flower_pot',Properties:{contents:'birch_sapling',legacy_data:'14'}}",
			"{Name:'minecraft:flower_pot',Properties:{contents:'blue_orchid',legacy_data:'14'}}",
			"{Name:'minecraft:flower_pot',Properties:{contents:'cactus',legacy_data:'14'}}",
			"{Name:'minecraft:flower_pot',Properties:{contents:'dandelion',legacy_data:'14'}}",
			"{Name:'minecraft:flower_pot',Properties:{contents:'dark_oak_sapling',legacy_data:'14'}}",
			"{Name:'minecraft:flower_pot',Properties:{contents:'dead_bush',legacy_data:'14'}}",
			"{Name:'minecraft:flower_pot',Properties:{contents:'empty',legacy_data:'14'}}",
			"{Name:'minecraft:flower_pot',Properties:{contents:'fern',legacy_data:'14'}}",
			"{Name:'minecraft:flower_pot',Properties:{contents:'houstonia',legacy_data:'14'}}",
			"{Name:'minecraft:flower_pot',Properties:{contents:'jungle_sapling',legacy_data:'14'}}",
			"{Name:'minecraft:flower_pot',Properties:{contents:'mushroom_brown',legacy_data:'14'}}",
			"{Name:'minecraft:flower_pot',Properties:{contents:'mushroom_red',legacy_data:'14'}}",
			"{Name:'minecraft:flower_pot',Properties:{contents:'oak_sapling',legacy_data:'14'}}",
			"{Name:'minecraft:flower_pot',Properties:{contents:'orange_tulip',legacy_data:'14'}}",
			"{Name:'minecraft:flower_pot',Properties:{contents:'oxeye_daisy',legacy_data:'14'}}",
			"{Name:'minecraft:flower_pot',Properties:{contents:'pink_tulip',legacy_data:'14'}}",
			"{Name:'minecraft:flower_pot',Properties:{contents:'red_tulip',legacy_data:'14'}}",
			"{Name:'minecraft:flower_pot',Properties:{contents:'rose',legacy_data:'14'}}",
			"{Name:'minecraft:flower_pot',Properties:{contents:'spruce_sapling',legacy_data:'14'}}",
			"{Name:'minecraft:flower_pot',Properties:{contents:'white_tulip',legacy_data:'14'}}"
		);
		putStates(
			2255,
			"{Name:'minecraft:potted_cactus'}",
			"{Name:'minecraft:flower_pot',Properties:{contents:'acacia_sapling',legacy_data:'15'}}",
			"{Name:'minecraft:flower_pot',Properties:{contents:'allium',legacy_data:'15'}}",
			"{Name:'minecraft:flower_pot',Properties:{contents:'birch_sapling',legacy_data:'15'}}",
			"{Name:'minecraft:flower_pot',Properties:{contents:'blue_orchid',legacy_data:'15'}}",
			"{Name:'minecraft:flower_pot',Properties:{contents:'cactus',legacy_data:'15'}}",
			"{Name:'minecraft:flower_pot',Properties:{contents:'dandelion',legacy_data:'15'}}",
			"{Name:'minecraft:flower_pot',Properties:{contents:'dark_oak_sapling',legacy_data:'15'}}",
			"{Name:'minecraft:flower_pot',Properties:{contents:'dead_bush',legacy_data:'15'}}",
			"{Name:'minecraft:flower_pot',Properties:{contents:'empty',legacy_data:'15'}}",
			"{Name:'minecraft:flower_pot',Properties:{contents:'fern',legacy_data:'15'}}",
			"{Name:'minecraft:flower_pot',Properties:{contents:'houstonia',legacy_data:'15'}}",
			"{Name:'minecraft:flower_pot',Properties:{contents:'jungle_sapling',legacy_data:'15'}}",
			"{Name:'minecraft:flower_pot',Properties:{contents:'mushroom_brown',legacy_data:'15'}}",
			"{Name:'minecraft:flower_pot',Properties:{contents:'mushroom_red',legacy_data:'15'}}",
			"{Name:'minecraft:flower_pot',Properties:{contents:'oak_sapling',legacy_data:'15'}}",
			"{Name:'minecraft:flower_pot',Properties:{contents:'orange_tulip',legacy_data:'15'}}",
			"{Name:'minecraft:flower_pot',Properties:{contents:'oxeye_daisy',legacy_data:'15'}}",
			"{Name:'minecraft:flower_pot',Properties:{contents:'pink_tulip',legacy_data:'15'}}",
			"{Name:'minecraft:flower_pot',Properties:{contents:'red_tulip',legacy_data:'15'}}",
			"{Name:'minecraft:flower_pot',Properties:{contents:'rose',legacy_data:'15'}}",
			"{Name:'minecraft:flower_pot',Properties:{contents:'spruce_sapling',legacy_data:'15'}}",
			"{Name:'minecraft:flower_pot',Properties:{contents:'white_tulip',legacy_data:'15'}}"
		);
		putStates(2256, "{Name:'minecraft:carrots',Properties:{age:'0'}}", "{Name:'minecraft:carrots',Properties:{age:'0'}}");
		putStates(2257, "{Name:'minecraft:carrots',Properties:{age:'1'}}", "{Name:'minecraft:carrots',Properties:{age:'1'}}");
		putStates(2258, "{Name:'minecraft:carrots',Properties:{age:'2'}}", "{Name:'minecraft:carrots',Properties:{age:'2'}}");
		putStates(2259, "{Name:'minecraft:carrots',Properties:{age:'3'}}", "{Name:'minecraft:carrots',Properties:{age:'3'}}");
		putStates(2260, "{Name:'minecraft:carrots',Properties:{age:'4'}}", "{Name:'minecraft:carrots',Properties:{age:'4'}}");
		putStates(2261, "{Name:'minecraft:carrots',Properties:{age:'5'}}", "{Name:'minecraft:carrots',Properties:{age:'5'}}");
		putStates(2262, "{Name:'minecraft:carrots',Properties:{age:'6'}}", "{Name:'minecraft:carrots',Properties:{age:'6'}}");
		putStates(2263, "{Name:'minecraft:carrots',Properties:{age:'7'}}", "{Name:'minecraft:carrots',Properties:{age:'7'}}");
		putStates(2272, "{Name:'minecraft:potatoes',Properties:{age:'0'}}", "{Name:'minecraft:potatoes',Properties:{age:'0'}}");
		putStates(2273, "{Name:'minecraft:potatoes',Properties:{age:'1'}}", "{Name:'minecraft:potatoes',Properties:{age:'1'}}");
		putStates(2274, "{Name:'minecraft:potatoes',Properties:{age:'2'}}", "{Name:'minecraft:potatoes',Properties:{age:'2'}}");
		putStates(2275, "{Name:'minecraft:potatoes',Properties:{age:'3'}}", "{Name:'minecraft:potatoes',Properties:{age:'3'}}");
		putStates(2276, "{Name:'minecraft:potatoes',Properties:{age:'4'}}", "{Name:'minecraft:potatoes',Properties:{age:'4'}}");
		putStates(2277, "{Name:'minecraft:potatoes',Properties:{age:'5'}}", "{Name:'minecraft:potatoes',Properties:{age:'5'}}");
		putStates(2278, "{Name:'minecraft:potatoes',Properties:{age:'6'}}", "{Name:'minecraft:potatoes',Properties:{age:'6'}}");
		putStates(2279, "{Name:'minecraft:potatoes',Properties:{age:'7'}}", "{Name:'minecraft:potatoes',Properties:{age:'7'}}");
		putStates(
			2288,
			"{Name:'minecraft:oak_button',Properties:{face:'ceiling',facing:'north',powered:'false'}}",
			"{Name:'minecraft:wooden_button',Properties:{facing:'down',powered:'false'}}"
		);
		putStates(
			2289,
			"{Name:'minecraft:oak_button',Properties:{face:'wall',facing:'east',powered:'false'}}",
			"{Name:'minecraft:wooden_button',Properties:{facing:'east',powered:'false'}}"
		);
		putStates(
			2290,
			"{Name:'minecraft:oak_button',Properties:{face:'wall',facing:'west',powered:'false'}}",
			"{Name:'minecraft:wooden_button',Properties:{facing:'west',powered:'false'}}"
		);
		putStates(
			2291,
			"{Name:'minecraft:oak_button',Properties:{face:'wall',facing:'south',powered:'false'}}",
			"{Name:'minecraft:wooden_button',Properties:{facing:'south',powered:'false'}}"
		);
		putStates(
			2292,
			"{Name:'minecraft:oak_button',Properties:{face:'wall',facing:'north',powered:'false'}}",
			"{Name:'minecraft:wooden_button',Properties:{facing:'north',powered:'false'}}"
		);
		putStates(
			2293,
			"{Name:'minecraft:oak_button',Properties:{face:'floor',facing:'north',powered:'false'}}",
			"{Name:'minecraft:wooden_button',Properties:{facing:'up',powered:'false'}}"
		);
		putStates(
			2296,
			"{Name:'minecraft:oak_button',Properties:{face:'ceiling',facing:'north',powered:'true'}}",
			"{Name:'minecraft:wooden_button',Properties:{facing:'down',powered:'true'}}"
		);
		putStates(
			2297,
			"{Name:'minecraft:oak_button',Properties:{face:'wall',facing:'east',powered:'true'}}",
			"{Name:'minecraft:wooden_button',Properties:{facing:'east',powered:'true'}}"
		);
		putStates(
			2298,
			"{Name:'minecraft:oak_button',Properties:{face:'wall',facing:'west',powered:'true'}}",
			"{Name:'minecraft:wooden_button',Properties:{facing:'west',powered:'true'}}"
		);
		putStates(
			2299,
			"{Name:'minecraft:oak_button',Properties:{face:'wall',facing:'south',powered:'true'}}",
			"{Name:'minecraft:wooden_button',Properties:{facing:'south',powered:'true'}}"
		);
		putStates(
			2300,
			"{Name:'minecraft:oak_button',Properties:{face:'wall',facing:'north',powered:'true'}}",
			"{Name:'minecraft:wooden_button',Properties:{facing:'north',powered:'true'}}"
		);
		putStates(
			2301,
			"{Name:'minecraft:oak_button',Properties:{face:'floor',facing:'north',powered:'true'}}",
			"{Name:'minecraft:wooden_button',Properties:{facing:'up',powered:'true'}}"
		);
	}

	/**
	 * Adds states to flatten from the blocks which had numeric IDs between 144 and 159 before 1.13.
	 */
	static void putStatesFromBlocks144To159() {
		putStates(2304, "{Name:'%%FILTER_ME%%',Properties:{facing:'down',nodrop:'false'}}", "{Name:'minecraft:skull',Properties:{facing:'down',nodrop:'false'}}");
		putStates(2305, "{Name:'%%FILTER_ME%%',Properties:{facing:'up',nodrop:'false'}}", "{Name:'minecraft:skull',Properties:{facing:'up',nodrop:'false'}}");
		putStates(2306, "{Name:'%%FILTER_ME%%',Properties:{facing:'north',nodrop:'false'}}", "{Name:'minecraft:skull',Properties:{facing:'north',nodrop:'false'}}");
		putStates(2307, "{Name:'%%FILTER_ME%%',Properties:{facing:'south',nodrop:'false'}}", "{Name:'minecraft:skull',Properties:{facing:'south',nodrop:'false'}}");
		putStates(2308, "{Name:'%%FILTER_ME%%',Properties:{facing:'west',nodrop:'false'}}", "{Name:'minecraft:skull',Properties:{facing:'west',nodrop:'false'}}");
		putStates(2309, "{Name:'%%FILTER_ME%%',Properties:{facing:'east',nodrop:'false'}}", "{Name:'minecraft:skull',Properties:{facing:'east',nodrop:'false'}}");
		putStates(2312, "{Name:'%%FILTER_ME%%',Properties:{facing:'down',nodrop:'true'}}", "{Name:'minecraft:skull',Properties:{facing:'down',nodrop:'true'}}");
		putStates(2313, "{Name:'%%FILTER_ME%%',Properties:{facing:'up',nodrop:'true'}}", "{Name:'minecraft:skull',Properties:{facing:'up',nodrop:'true'}}");
		putStates(2314, "{Name:'%%FILTER_ME%%',Properties:{facing:'north',nodrop:'true'}}", "{Name:'minecraft:skull',Properties:{facing:'north',nodrop:'true'}}");
		putStates(2315, "{Name:'%%FILTER_ME%%',Properties:{facing:'south',nodrop:'true'}}", "{Name:'minecraft:skull',Properties:{facing:'south',nodrop:'true'}}");
		putStates(2316, "{Name:'%%FILTER_ME%%',Properties:{facing:'west',nodrop:'true'}}", "{Name:'minecraft:skull',Properties:{facing:'west',nodrop:'true'}}");
		putStates(2317, "{Name:'%%FILTER_ME%%',Properties:{facing:'east',nodrop:'true'}}", "{Name:'minecraft:skull',Properties:{facing:'east',nodrop:'true'}}");
		putStates(2320, "{Name:'minecraft:anvil',Properties:{facing:'south'}}", "{Name:'minecraft:anvil',Properties:{damage:'0',facing:'south'}}");
		putStates(2321, "{Name:'minecraft:anvil',Properties:{facing:'west'}}", "{Name:'minecraft:anvil',Properties:{damage:'0',facing:'west'}}");
		putStates(2322, "{Name:'minecraft:anvil',Properties:{facing:'north'}}", "{Name:'minecraft:anvil',Properties:{damage:'0',facing:'north'}}");
		putStates(2323, "{Name:'minecraft:anvil',Properties:{facing:'east'}}", "{Name:'minecraft:anvil',Properties:{damage:'0',facing:'east'}}");
		putStates(2324, "{Name:'minecraft:chipped_anvil',Properties:{facing:'south'}}", "{Name:'minecraft:anvil',Properties:{damage:'1',facing:'south'}}");
		putStates(2325, "{Name:'minecraft:chipped_anvil',Properties:{facing:'west'}}", "{Name:'minecraft:anvil',Properties:{damage:'1',facing:'west'}}");
		putStates(2326, "{Name:'minecraft:chipped_anvil',Properties:{facing:'north'}}", "{Name:'minecraft:anvil',Properties:{damage:'1',facing:'north'}}");
		putStates(2327, "{Name:'minecraft:chipped_anvil',Properties:{facing:'east'}}", "{Name:'minecraft:anvil',Properties:{damage:'1',facing:'east'}}");
		putStates(2328, "{Name:'minecraft:damaged_anvil',Properties:{facing:'south'}}", "{Name:'minecraft:anvil',Properties:{damage:'2',facing:'south'}}");
		putStates(2329, "{Name:'minecraft:damaged_anvil',Properties:{facing:'west'}}", "{Name:'minecraft:anvil',Properties:{damage:'2',facing:'west'}}");
		putStates(2330, "{Name:'minecraft:damaged_anvil',Properties:{facing:'north'}}", "{Name:'minecraft:anvil',Properties:{damage:'2',facing:'north'}}");
		putStates(2331, "{Name:'minecraft:damaged_anvil',Properties:{facing:'east'}}", "{Name:'minecraft:anvil',Properties:{damage:'2',facing:'east'}}");
		putStates(2338, "{Name:'minecraft:trapped_chest',Properties:{facing:'north',type:'single'}}", "{Name:'minecraft:trapped_chest',Properties:{facing:'north'}}");
		putStates(2339, "{Name:'minecraft:trapped_chest',Properties:{facing:'south',type:'single'}}", "{Name:'minecraft:trapped_chest',Properties:{facing:'south'}}");
		putStates(2340, "{Name:'minecraft:trapped_chest',Properties:{facing:'west',type:'single'}}", "{Name:'minecraft:trapped_chest',Properties:{facing:'west'}}");
		putStates(2341, "{Name:'minecraft:trapped_chest',Properties:{facing:'east',type:'single'}}", "{Name:'minecraft:trapped_chest',Properties:{facing:'east'}}");
		putStates(
			2352, "{Name:'minecraft:light_weighted_pressure_plate',Properties:{power:'0'}}", "{Name:'minecraft:light_weighted_pressure_plate',Properties:{power:'0'}}"
		);
		putStates(
			2353, "{Name:'minecraft:light_weighted_pressure_plate',Properties:{power:'1'}}", "{Name:'minecraft:light_weighted_pressure_plate',Properties:{power:'1'}}"
		);
		putStates(
			2354, "{Name:'minecraft:light_weighted_pressure_plate',Properties:{power:'2'}}", "{Name:'minecraft:light_weighted_pressure_plate',Properties:{power:'2'}}"
		);
		putStates(
			2355, "{Name:'minecraft:light_weighted_pressure_plate',Properties:{power:'3'}}", "{Name:'minecraft:light_weighted_pressure_plate',Properties:{power:'3'}}"
		);
		putStates(
			2356, "{Name:'minecraft:light_weighted_pressure_plate',Properties:{power:'4'}}", "{Name:'minecraft:light_weighted_pressure_plate',Properties:{power:'4'}}"
		);
		putStates(
			2357, "{Name:'minecraft:light_weighted_pressure_plate',Properties:{power:'5'}}", "{Name:'minecraft:light_weighted_pressure_plate',Properties:{power:'5'}}"
		);
		putStates(
			2358, "{Name:'minecraft:light_weighted_pressure_plate',Properties:{power:'6'}}", "{Name:'minecraft:light_weighted_pressure_plate',Properties:{power:'6'}}"
		);
		putStates(
			2359, "{Name:'minecraft:light_weighted_pressure_plate',Properties:{power:'7'}}", "{Name:'minecraft:light_weighted_pressure_plate',Properties:{power:'7'}}"
		);
		putStates(
			2360, "{Name:'minecraft:light_weighted_pressure_plate',Properties:{power:'8'}}", "{Name:'minecraft:light_weighted_pressure_plate',Properties:{power:'8'}}"
		);
		putStates(
			2361, "{Name:'minecraft:light_weighted_pressure_plate',Properties:{power:'9'}}", "{Name:'minecraft:light_weighted_pressure_plate',Properties:{power:'9'}}"
		);
		putStates(
			2362, "{Name:'minecraft:light_weighted_pressure_plate',Properties:{power:'10'}}", "{Name:'minecraft:light_weighted_pressure_plate',Properties:{power:'10'}}"
		);
		putStates(
			2363, "{Name:'minecraft:light_weighted_pressure_plate',Properties:{power:'11'}}", "{Name:'minecraft:light_weighted_pressure_plate',Properties:{power:'11'}}"
		);
		putStates(
			2364, "{Name:'minecraft:light_weighted_pressure_plate',Properties:{power:'12'}}", "{Name:'minecraft:light_weighted_pressure_plate',Properties:{power:'12'}}"
		);
		putStates(
			2365, "{Name:'minecraft:light_weighted_pressure_plate',Properties:{power:'13'}}", "{Name:'minecraft:light_weighted_pressure_plate',Properties:{power:'13'}}"
		);
		putStates(
			2366, "{Name:'minecraft:light_weighted_pressure_plate',Properties:{power:'14'}}", "{Name:'minecraft:light_weighted_pressure_plate',Properties:{power:'14'}}"
		);
		putStates(
			2367, "{Name:'minecraft:light_weighted_pressure_plate',Properties:{power:'15'}}", "{Name:'minecraft:light_weighted_pressure_plate',Properties:{power:'15'}}"
		);
		putStates(
			2368, "{Name:'minecraft:heavy_weighted_pressure_plate',Properties:{power:'0'}}", "{Name:'minecraft:heavy_weighted_pressure_plate',Properties:{power:'0'}}"
		);
		putStates(
			2369, "{Name:'minecraft:heavy_weighted_pressure_plate',Properties:{power:'1'}}", "{Name:'minecraft:heavy_weighted_pressure_plate',Properties:{power:'1'}}"
		);
		putStates(
			2370, "{Name:'minecraft:heavy_weighted_pressure_plate',Properties:{power:'2'}}", "{Name:'minecraft:heavy_weighted_pressure_plate',Properties:{power:'2'}}"
		);
		putStates(
			2371, "{Name:'minecraft:heavy_weighted_pressure_plate',Properties:{power:'3'}}", "{Name:'minecraft:heavy_weighted_pressure_plate',Properties:{power:'3'}}"
		);
		putStates(
			2372, "{Name:'minecraft:heavy_weighted_pressure_plate',Properties:{power:'4'}}", "{Name:'minecraft:heavy_weighted_pressure_plate',Properties:{power:'4'}}"
		);
		putStates(
			2373, "{Name:'minecraft:heavy_weighted_pressure_plate',Properties:{power:'5'}}", "{Name:'minecraft:heavy_weighted_pressure_plate',Properties:{power:'5'}}"
		);
		putStates(
			2374, "{Name:'minecraft:heavy_weighted_pressure_plate',Properties:{power:'6'}}", "{Name:'minecraft:heavy_weighted_pressure_plate',Properties:{power:'6'}}"
		);
		putStates(
			2375, "{Name:'minecraft:heavy_weighted_pressure_plate',Properties:{power:'7'}}", "{Name:'minecraft:heavy_weighted_pressure_plate',Properties:{power:'7'}}"
		);
		putStates(
			2376, "{Name:'minecraft:heavy_weighted_pressure_plate',Properties:{power:'8'}}", "{Name:'minecraft:heavy_weighted_pressure_plate',Properties:{power:'8'}}"
		);
		putStates(
			2377, "{Name:'minecraft:heavy_weighted_pressure_plate',Properties:{power:'9'}}", "{Name:'minecraft:heavy_weighted_pressure_plate',Properties:{power:'9'}}"
		);
		putStates(
			2378, "{Name:'minecraft:heavy_weighted_pressure_plate',Properties:{power:'10'}}", "{Name:'minecraft:heavy_weighted_pressure_plate',Properties:{power:'10'}}"
		);
		putStates(
			2379, "{Name:'minecraft:heavy_weighted_pressure_plate',Properties:{power:'11'}}", "{Name:'minecraft:heavy_weighted_pressure_plate',Properties:{power:'11'}}"
		);
		putStates(
			2380, "{Name:'minecraft:heavy_weighted_pressure_plate',Properties:{power:'12'}}", "{Name:'minecraft:heavy_weighted_pressure_plate',Properties:{power:'12'}}"
		);
		putStates(
			2381, "{Name:'minecraft:heavy_weighted_pressure_plate',Properties:{power:'13'}}", "{Name:'minecraft:heavy_weighted_pressure_plate',Properties:{power:'13'}}"
		);
		putStates(
			2382, "{Name:'minecraft:heavy_weighted_pressure_plate',Properties:{power:'14'}}", "{Name:'minecraft:heavy_weighted_pressure_plate',Properties:{power:'14'}}"
		);
		putStates(
			2383, "{Name:'minecraft:heavy_weighted_pressure_plate',Properties:{power:'15'}}", "{Name:'minecraft:heavy_weighted_pressure_plate',Properties:{power:'15'}}"
		);
		putStates(
			2384,
			"{Name:'minecraft:comparator',Properties:{facing:'south',mode:'compare',powered:'false'}}",
			"{Name:'minecraft:unpowered_comparator',Properties:{facing:'south',mode:'compare',powered:'false'}}"
		);
		putStates(
			2385,
			"{Name:'minecraft:comparator',Properties:{facing:'west',mode:'compare',powered:'false'}}",
			"{Name:'minecraft:unpowered_comparator',Properties:{facing:'west',mode:'compare',powered:'false'}}"
		);
		putStates(
			2386,
			"{Name:'minecraft:comparator',Properties:{facing:'north',mode:'compare',powered:'false'}}",
			"{Name:'minecraft:unpowered_comparator',Properties:{facing:'north',mode:'compare',powered:'false'}}"
		);
		putStates(
			2387,
			"{Name:'minecraft:comparator',Properties:{facing:'east',mode:'compare',powered:'false'}}",
			"{Name:'minecraft:unpowered_comparator',Properties:{facing:'east',mode:'compare',powered:'false'}}"
		);
		putStates(
			2388,
			"{Name:'minecraft:comparator',Properties:{facing:'south',mode:'subtract',powered:'false'}}",
			"{Name:'minecraft:unpowered_comparator',Properties:{facing:'south',mode:'subtract',powered:'false'}}"
		);
		putStates(
			2389,
			"{Name:'minecraft:comparator',Properties:{facing:'west',mode:'subtract',powered:'false'}}",
			"{Name:'minecraft:unpowered_comparator',Properties:{facing:'west',mode:'subtract',powered:'false'}}"
		);
		putStates(
			2390,
			"{Name:'minecraft:comparator',Properties:{facing:'north',mode:'subtract',powered:'false'}}",
			"{Name:'minecraft:unpowered_comparator',Properties:{facing:'north',mode:'subtract',powered:'false'}}"
		);
		putStates(
			2391,
			"{Name:'minecraft:comparator',Properties:{facing:'east',mode:'subtract',powered:'false'}}",
			"{Name:'minecraft:unpowered_comparator',Properties:{facing:'east',mode:'subtract',powered:'false'}}"
		);
		putStates(
			2392,
			"{Name:'minecraft:comparator',Properties:{facing:'south',mode:'compare',powered:'true'}}",
			"{Name:'minecraft:unpowered_comparator',Properties:{facing:'south',mode:'compare',powered:'true'}}"
		);
		putStates(
			2393,
			"{Name:'minecraft:comparator',Properties:{facing:'west',mode:'compare',powered:'true'}}",
			"{Name:'minecraft:unpowered_comparator',Properties:{facing:'west',mode:'compare',powered:'true'}}"
		);
		putStates(
			2394,
			"{Name:'minecraft:comparator',Properties:{facing:'north',mode:'compare',powered:'true'}}",
			"{Name:'minecraft:unpowered_comparator',Properties:{facing:'north',mode:'compare',powered:'true'}}"
		);
		putStates(
			2395,
			"{Name:'minecraft:comparator',Properties:{facing:'east',mode:'compare',powered:'true'}}",
			"{Name:'minecraft:unpowered_comparator',Properties:{facing:'east',mode:'compare',powered:'true'}}"
		);
		putStates(
			2396,
			"{Name:'minecraft:comparator',Properties:{facing:'south',mode:'subtract',powered:'true'}}",
			"{Name:'minecraft:unpowered_comparator',Properties:{facing:'south',mode:'subtract',powered:'true'}}"
		);
		putStates(
			2397,
			"{Name:'minecraft:comparator',Properties:{facing:'west',mode:'subtract',powered:'true'}}",
			"{Name:'minecraft:unpowered_comparator',Properties:{facing:'west',mode:'subtract',powered:'true'}}"
		);
		putStates(
			2398,
			"{Name:'minecraft:comparator',Properties:{facing:'north',mode:'subtract',powered:'true'}}",
			"{Name:'minecraft:unpowered_comparator',Properties:{facing:'north',mode:'subtract',powered:'true'}}"
		);
		putStates(
			2399,
			"{Name:'minecraft:comparator',Properties:{facing:'east',mode:'subtract',powered:'true'}}",
			"{Name:'minecraft:unpowered_comparator',Properties:{facing:'east',mode:'subtract',powered:'true'}}"
		);
		putStates(
			2400,
			"{Name:'minecraft:comparator',Properties:{facing:'south',mode:'compare',powered:'false'}}",
			"{Name:'minecraft:powered_comparator',Properties:{facing:'south',mode:'compare',powered:'false'}}"
		);
		putStates(
			2401,
			"{Name:'minecraft:comparator',Properties:{facing:'west',mode:'compare',powered:'false'}}",
			"{Name:'minecraft:powered_comparator',Properties:{facing:'west',mode:'compare',powered:'false'}}"
		);
		putStates(
			2402,
			"{Name:'minecraft:comparator',Properties:{facing:'north',mode:'compare',powered:'false'}}",
			"{Name:'minecraft:powered_comparator',Properties:{facing:'north',mode:'compare',powered:'false'}}"
		);
		putStates(
			2403,
			"{Name:'minecraft:comparator',Properties:{facing:'east',mode:'compare',powered:'false'}}",
			"{Name:'minecraft:powered_comparator',Properties:{facing:'east',mode:'compare',powered:'false'}}"
		);
		putStates(
			2404,
			"{Name:'minecraft:comparator',Properties:{facing:'south',mode:'subtract',powered:'false'}}",
			"{Name:'minecraft:powered_comparator',Properties:{facing:'south',mode:'subtract',powered:'false'}}"
		);
		putStates(
			2405,
			"{Name:'minecraft:comparator',Properties:{facing:'west',mode:'subtract',powered:'false'}}",
			"{Name:'minecraft:powered_comparator',Properties:{facing:'west',mode:'subtract',powered:'false'}}"
		);
		putStates(
			2406,
			"{Name:'minecraft:comparator',Properties:{facing:'north',mode:'subtract',powered:'false'}}",
			"{Name:'minecraft:powered_comparator',Properties:{facing:'north',mode:'subtract',powered:'false'}}"
		);
		putStates(
			2407,
			"{Name:'minecraft:comparator',Properties:{facing:'east',mode:'subtract',powered:'false'}}",
			"{Name:'minecraft:powered_comparator',Properties:{facing:'east',mode:'subtract',powered:'false'}}"
		);
		putStates(
			2408,
			"{Name:'minecraft:comparator',Properties:{facing:'south',mode:'compare',powered:'true'}}",
			"{Name:'minecraft:powered_comparator',Properties:{facing:'south',mode:'compare',powered:'true'}}"
		);
		putStates(
			2409,
			"{Name:'minecraft:comparator',Properties:{facing:'west',mode:'compare',powered:'true'}}",
			"{Name:'minecraft:powered_comparator',Properties:{facing:'west',mode:'compare',powered:'true'}}"
		);
		putStates(
			2410,
			"{Name:'minecraft:comparator',Properties:{facing:'north',mode:'compare',powered:'true'}}",
			"{Name:'minecraft:powered_comparator',Properties:{facing:'north',mode:'compare',powered:'true'}}"
		);
		putStates(
			2411,
			"{Name:'minecraft:comparator',Properties:{facing:'east',mode:'compare',powered:'true'}}",
			"{Name:'minecraft:powered_comparator',Properties:{facing:'east',mode:'compare',powered:'true'}}"
		);
		putStates(
			2412,
			"{Name:'minecraft:comparator',Properties:{facing:'south',mode:'subtract',powered:'true'}}",
			"{Name:'minecraft:powered_comparator',Properties:{facing:'south',mode:'subtract',powered:'true'}}"
		);
		putStates(
			2413,
			"{Name:'minecraft:comparator',Properties:{facing:'west',mode:'subtract',powered:'true'}}",
			"{Name:'minecraft:powered_comparator',Properties:{facing:'west',mode:'subtract',powered:'true'}}"
		);
		putStates(
			2414,
			"{Name:'minecraft:comparator',Properties:{facing:'north',mode:'subtract',powered:'true'}}",
			"{Name:'minecraft:powered_comparator',Properties:{facing:'north',mode:'subtract',powered:'true'}}"
		);
		putStates(
			2415,
			"{Name:'minecraft:comparator',Properties:{facing:'east',mode:'subtract',powered:'true'}}",
			"{Name:'minecraft:powered_comparator',Properties:{facing:'east',mode:'subtract',powered:'true'}}"
		);
		putStates(2416, "{Name:'minecraft:daylight_detector',Properties:{inverted:'false',power:'0'}}", "{Name:'minecraft:daylight_detector',Properties:{power:'0'}}");
		putStates(2417, "{Name:'minecraft:daylight_detector',Properties:{inverted:'false',power:'1'}}", "{Name:'minecraft:daylight_detector',Properties:{power:'1'}}");
		putStates(2418, "{Name:'minecraft:daylight_detector',Properties:{inverted:'false',power:'2'}}", "{Name:'minecraft:daylight_detector',Properties:{power:'2'}}");
		putStates(2419, "{Name:'minecraft:daylight_detector',Properties:{inverted:'false',power:'3'}}", "{Name:'minecraft:daylight_detector',Properties:{power:'3'}}");
		putStates(2420, "{Name:'minecraft:daylight_detector',Properties:{inverted:'false',power:'4'}}", "{Name:'minecraft:daylight_detector',Properties:{power:'4'}}");
		putStates(2421, "{Name:'minecraft:daylight_detector',Properties:{inverted:'false',power:'5'}}", "{Name:'minecraft:daylight_detector',Properties:{power:'5'}}");
		putStates(2422, "{Name:'minecraft:daylight_detector',Properties:{inverted:'false',power:'6'}}", "{Name:'minecraft:daylight_detector',Properties:{power:'6'}}");
		putStates(2423, "{Name:'minecraft:daylight_detector',Properties:{inverted:'false',power:'7'}}", "{Name:'minecraft:daylight_detector',Properties:{power:'7'}}");
		putStates(2424, "{Name:'minecraft:daylight_detector',Properties:{inverted:'false',power:'8'}}", "{Name:'minecraft:daylight_detector',Properties:{power:'8'}}");
		putStates(2425, "{Name:'minecraft:daylight_detector',Properties:{inverted:'false',power:'9'}}", "{Name:'minecraft:daylight_detector',Properties:{power:'9'}}");
		putStates(
			2426, "{Name:'minecraft:daylight_detector',Properties:{inverted:'false',power:'10'}}", "{Name:'minecraft:daylight_detector',Properties:{power:'10'}}"
		);
		putStates(
			2427, "{Name:'minecraft:daylight_detector',Properties:{inverted:'false',power:'11'}}", "{Name:'minecraft:daylight_detector',Properties:{power:'11'}}"
		);
		putStates(
			2428, "{Name:'minecraft:daylight_detector',Properties:{inverted:'false',power:'12'}}", "{Name:'minecraft:daylight_detector',Properties:{power:'12'}}"
		);
		putStates(
			2429, "{Name:'minecraft:daylight_detector',Properties:{inverted:'false',power:'13'}}", "{Name:'minecraft:daylight_detector',Properties:{power:'13'}}"
		);
		putStates(
			2430, "{Name:'minecraft:daylight_detector',Properties:{inverted:'false',power:'14'}}", "{Name:'minecraft:daylight_detector',Properties:{power:'14'}}"
		);
		putStates(
			2431, "{Name:'minecraft:daylight_detector',Properties:{inverted:'false',power:'15'}}", "{Name:'minecraft:daylight_detector',Properties:{power:'15'}}"
		);
		putStates(2432, "{Name:'minecraft:redstone_block'}", "{Name:'minecraft:redstone_block'}");
		putStates(2448, "{Name:'minecraft:nether_quartz_ore'}", "{Name:'minecraft:quartz_ore'}");
		putStates(2464, "{Name:'minecraft:hopper',Properties:{enabled:'true',facing:'down'}}", "{Name:'minecraft:hopper',Properties:{enabled:'true',facing:'down'}}");
		putStates(
			2466, "{Name:'minecraft:hopper',Properties:{enabled:'true',facing:'north'}}", "{Name:'minecraft:hopper',Properties:{enabled:'true',facing:'north'}}"
		);
		putStates(
			2467, "{Name:'minecraft:hopper',Properties:{enabled:'true',facing:'south'}}", "{Name:'minecraft:hopper',Properties:{enabled:'true',facing:'south'}}"
		);
		putStates(2468, "{Name:'minecraft:hopper',Properties:{enabled:'true',facing:'west'}}", "{Name:'minecraft:hopper',Properties:{enabled:'true',facing:'west'}}");
		putStates(2469, "{Name:'minecraft:hopper',Properties:{enabled:'true',facing:'east'}}", "{Name:'minecraft:hopper',Properties:{enabled:'true',facing:'east'}}");
		putStates(
			2472, "{Name:'minecraft:hopper',Properties:{enabled:'false',facing:'down'}}", "{Name:'minecraft:hopper',Properties:{enabled:'false',facing:'down'}}"
		);
		putStates(
			2474, "{Name:'minecraft:hopper',Properties:{enabled:'false',facing:'north'}}", "{Name:'minecraft:hopper',Properties:{enabled:'false',facing:'north'}}"
		);
		putStates(
			2475, "{Name:'minecraft:hopper',Properties:{enabled:'false',facing:'south'}}", "{Name:'minecraft:hopper',Properties:{enabled:'false',facing:'south'}}"
		);
		putStates(
			2476, "{Name:'minecraft:hopper',Properties:{enabled:'false',facing:'west'}}", "{Name:'minecraft:hopper',Properties:{enabled:'false',facing:'west'}}"
		);
		putStates(
			2477, "{Name:'minecraft:hopper',Properties:{enabled:'false',facing:'east'}}", "{Name:'minecraft:hopper',Properties:{enabled:'false',facing:'east'}}"
		);
		putStates(2480, "{Name:'minecraft:quartz_block'}", "{Name:'minecraft:quartz_block',Properties:{variant:'default'}}");
		putStates(2481, "{Name:'minecraft:chiseled_quartz_block'}", "{Name:'minecraft:quartz_block',Properties:{variant:'chiseled'}}");
		putStates(2482, "{Name:'minecraft:quartz_pillar',Properties:{axis:'y'}}", "{Name:'minecraft:quartz_block',Properties:{variant:'lines_y'}}");
		putStates(2483, "{Name:'minecraft:quartz_pillar',Properties:{axis:'x'}}", "{Name:'minecraft:quartz_block',Properties:{variant:'lines_x'}}");
		putStates(2484, "{Name:'minecraft:quartz_pillar',Properties:{axis:'z'}}", "{Name:'minecraft:quartz_block',Properties:{variant:'lines_z'}}");
		putStates(
			2496,
			"{Name:'minecraft:quartz_stairs',Properties:{facing:'east',half:'bottom',shape:'straight'}}",
			"{Name:'minecraft:quartz_stairs',Properties:{facing:'east',half:'bottom',shape:'inner_left'}}",
			"{Name:'minecraft:quartz_stairs',Properties:{facing:'east',half:'bottom',shape:'inner_right'}}",
			"{Name:'minecraft:quartz_stairs',Properties:{facing:'east',half:'bottom',shape:'outer_left'}}",
			"{Name:'minecraft:quartz_stairs',Properties:{facing:'east',half:'bottom',shape:'outer_right'}}",
			"{Name:'minecraft:quartz_stairs',Properties:{facing:'east',half:'bottom',shape:'straight'}}"
		);
		putStates(
			2497,
			"{Name:'minecraft:quartz_stairs',Properties:{facing:'west',half:'bottom',shape:'straight'}}",
			"{Name:'minecraft:quartz_stairs',Properties:{facing:'west',half:'bottom',shape:'inner_left'}}",
			"{Name:'minecraft:quartz_stairs',Properties:{facing:'west',half:'bottom',shape:'inner_right'}}",
			"{Name:'minecraft:quartz_stairs',Properties:{facing:'west',half:'bottom',shape:'outer_left'}}",
			"{Name:'minecraft:quartz_stairs',Properties:{facing:'west',half:'bottom',shape:'outer_right'}}",
			"{Name:'minecraft:quartz_stairs',Properties:{facing:'west',half:'bottom',shape:'straight'}}"
		);
		putStates(
			2498,
			"{Name:'minecraft:quartz_stairs',Properties:{facing:'south',half:'bottom',shape:'straight'}}",
			"{Name:'minecraft:quartz_stairs',Properties:{facing:'south',half:'bottom',shape:'inner_left'}}",
			"{Name:'minecraft:quartz_stairs',Properties:{facing:'south',half:'bottom',shape:'inner_right'}}",
			"{Name:'minecraft:quartz_stairs',Properties:{facing:'south',half:'bottom',shape:'outer_left'}}",
			"{Name:'minecraft:quartz_stairs',Properties:{facing:'south',half:'bottom',shape:'outer_right'}}",
			"{Name:'minecraft:quartz_stairs',Properties:{facing:'south',half:'bottom',shape:'straight'}}"
		);
		putStates(
			2499,
			"{Name:'minecraft:quartz_stairs',Properties:{facing:'north',half:'bottom',shape:'straight'}}",
			"{Name:'minecraft:quartz_stairs',Properties:{facing:'north',half:'bottom',shape:'inner_left'}}",
			"{Name:'minecraft:quartz_stairs',Properties:{facing:'north',half:'bottom',shape:'inner_right'}}",
			"{Name:'minecraft:quartz_stairs',Properties:{facing:'north',half:'bottom',shape:'outer_left'}}",
			"{Name:'minecraft:quartz_stairs',Properties:{facing:'north',half:'bottom',shape:'outer_right'}}",
			"{Name:'minecraft:quartz_stairs',Properties:{facing:'north',half:'bottom',shape:'straight'}}"
		);
		putStates(
			2500,
			"{Name:'minecraft:quartz_stairs',Properties:{facing:'east',half:'top',shape:'straight'}}",
			"{Name:'minecraft:quartz_stairs',Properties:{facing:'east',half:'top',shape:'inner_left'}}",
			"{Name:'minecraft:quartz_stairs',Properties:{facing:'east',half:'top',shape:'inner_right'}}",
			"{Name:'minecraft:quartz_stairs',Properties:{facing:'east',half:'top',shape:'outer_left'}}",
			"{Name:'minecraft:quartz_stairs',Properties:{facing:'east',half:'top',shape:'outer_right'}}",
			"{Name:'minecraft:quartz_stairs',Properties:{facing:'east',half:'top',shape:'straight'}}"
		);
		putStates(
			2501,
			"{Name:'minecraft:quartz_stairs',Properties:{facing:'west',half:'top',shape:'straight'}}",
			"{Name:'minecraft:quartz_stairs',Properties:{facing:'west',half:'top',shape:'inner_left'}}",
			"{Name:'minecraft:quartz_stairs',Properties:{facing:'west',half:'top',shape:'inner_right'}}",
			"{Name:'minecraft:quartz_stairs',Properties:{facing:'west',half:'top',shape:'outer_left'}}",
			"{Name:'minecraft:quartz_stairs',Properties:{facing:'west',half:'top',shape:'outer_right'}}",
			"{Name:'minecraft:quartz_stairs',Properties:{facing:'west',half:'top',shape:'straight'}}"
		);
		putStates(
			2502,
			"{Name:'minecraft:quartz_stairs',Properties:{facing:'south',half:'top',shape:'straight'}}",
			"{Name:'minecraft:quartz_stairs',Properties:{facing:'south',half:'top',shape:'inner_left'}}",
			"{Name:'minecraft:quartz_stairs',Properties:{facing:'south',half:'top',shape:'inner_right'}}",
			"{Name:'minecraft:quartz_stairs',Properties:{facing:'south',half:'top',shape:'outer_left'}}",
			"{Name:'minecraft:quartz_stairs',Properties:{facing:'south',half:'top',shape:'outer_right'}}",
			"{Name:'minecraft:quartz_stairs',Properties:{facing:'south',half:'top',shape:'straight'}}"
		);
		putStates(
			2503,
			"{Name:'minecraft:quartz_stairs',Properties:{facing:'north',half:'top',shape:'straight'}}",
			"{Name:'minecraft:quartz_stairs',Properties:{facing:'north',half:'top',shape:'inner_left'}}",
			"{Name:'minecraft:quartz_stairs',Properties:{facing:'north',half:'top',shape:'inner_right'}}",
			"{Name:'minecraft:quartz_stairs',Properties:{facing:'north',half:'top',shape:'outer_left'}}",
			"{Name:'minecraft:quartz_stairs',Properties:{facing:'north',half:'top',shape:'outer_right'}}",
			"{Name:'minecraft:quartz_stairs',Properties:{facing:'north',half:'top',shape:'straight'}}"
		);
		putStates(
			2512,
			"{Name:'minecraft:activator_rail',Properties:{powered:'false',shape:'north_south'}}",
			"{Name:'minecraft:activator_rail',Properties:{powered:'false',shape:'north_south'}}"
		);
		putStates(
			2513,
			"{Name:'minecraft:activator_rail',Properties:{powered:'false',shape:'east_west'}}",
			"{Name:'minecraft:activator_rail',Properties:{powered:'false',shape:'east_west'}}"
		);
		putStates(
			2514,
			"{Name:'minecraft:activator_rail',Properties:{powered:'false',shape:'ascending_east'}}",
			"{Name:'minecraft:activator_rail',Properties:{powered:'false',shape:'ascending_east'}}"
		);
		putStates(
			2515,
			"{Name:'minecraft:activator_rail',Properties:{powered:'false',shape:'ascending_west'}}",
			"{Name:'minecraft:activator_rail',Properties:{powered:'false',shape:'ascending_west'}}"
		);
		putStates(
			2516,
			"{Name:'minecraft:activator_rail',Properties:{powered:'false',shape:'ascending_north'}}",
			"{Name:'minecraft:activator_rail',Properties:{powered:'false',shape:'ascending_north'}}"
		);
		putStates(
			2517,
			"{Name:'minecraft:activator_rail',Properties:{powered:'false',shape:'ascending_south'}}",
			"{Name:'minecraft:activator_rail',Properties:{powered:'false',shape:'ascending_south'}}"
		);
		putStates(
			2520,
			"{Name:'minecraft:activator_rail',Properties:{powered:'true',shape:'north_south'}}",
			"{Name:'minecraft:activator_rail',Properties:{powered:'true',shape:'north_south'}}"
		);
		putStates(
			2521,
			"{Name:'minecraft:activator_rail',Properties:{powered:'true',shape:'east_west'}}",
			"{Name:'minecraft:activator_rail',Properties:{powered:'true',shape:'east_west'}}"
		);
		putStates(
			2522,
			"{Name:'minecraft:activator_rail',Properties:{powered:'true',shape:'ascending_east'}}",
			"{Name:'minecraft:activator_rail',Properties:{powered:'true',shape:'ascending_east'}}"
		);
		putStates(
			2523,
			"{Name:'minecraft:activator_rail',Properties:{powered:'true',shape:'ascending_west'}}",
			"{Name:'minecraft:activator_rail',Properties:{powered:'true',shape:'ascending_west'}}"
		);
		putStates(
			2524,
			"{Name:'minecraft:activator_rail',Properties:{powered:'true',shape:'ascending_north'}}",
			"{Name:'minecraft:activator_rail',Properties:{powered:'true',shape:'ascending_north'}}"
		);
		putStates(
			2525,
			"{Name:'minecraft:activator_rail',Properties:{powered:'true',shape:'ascending_south'}}",
			"{Name:'minecraft:activator_rail',Properties:{powered:'true',shape:'ascending_south'}}"
		);
		putStates(
			2528, "{Name:'minecraft:dropper',Properties:{facing:'down',triggered:'false'}}", "{Name:'minecraft:dropper',Properties:{facing:'down',triggered:'false'}}"
		);
		putStates(
			2529, "{Name:'minecraft:dropper',Properties:{facing:'up',triggered:'false'}}", "{Name:'minecraft:dropper',Properties:{facing:'up',triggered:'false'}}"
		);
		putStates(
			2530, "{Name:'minecraft:dropper',Properties:{facing:'north',triggered:'false'}}", "{Name:'minecraft:dropper',Properties:{facing:'north',triggered:'false'}}"
		);
		putStates(
			2531, "{Name:'minecraft:dropper',Properties:{facing:'south',triggered:'false'}}", "{Name:'minecraft:dropper',Properties:{facing:'south',triggered:'false'}}"
		);
		putStates(
			2532, "{Name:'minecraft:dropper',Properties:{facing:'west',triggered:'false'}}", "{Name:'minecraft:dropper',Properties:{facing:'west',triggered:'false'}}"
		);
		putStates(
			2533, "{Name:'minecraft:dropper',Properties:{facing:'east',triggered:'false'}}", "{Name:'minecraft:dropper',Properties:{facing:'east',triggered:'false'}}"
		);
		putStates(
			2536, "{Name:'minecraft:dropper',Properties:{facing:'down',triggered:'true'}}", "{Name:'minecraft:dropper',Properties:{facing:'down',triggered:'true'}}"
		);
		putStates(
			2537, "{Name:'minecraft:dropper',Properties:{facing:'up',triggered:'true'}}", "{Name:'minecraft:dropper',Properties:{facing:'up',triggered:'true'}}"
		);
		putStates(
			2538, "{Name:'minecraft:dropper',Properties:{facing:'north',triggered:'true'}}", "{Name:'minecraft:dropper',Properties:{facing:'north',triggered:'true'}}"
		);
		putStates(
			2539, "{Name:'minecraft:dropper',Properties:{facing:'south',triggered:'true'}}", "{Name:'minecraft:dropper',Properties:{facing:'south',triggered:'true'}}"
		);
		putStates(
			2540, "{Name:'minecraft:dropper',Properties:{facing:'west',triggered:'true'}}", "{Name:'minecraft:dropper',Properties:{facing:'west',triggered:'true'}}"
		);
		putStates(
			2541, "{Name:'minecraft:dropper',Properties:{facing:'east',triggered:'true'}}", "{Name:'minecraft:dropper',Properties:{facing:'east',triggered:'true'}}"
		);
		putStates(2544, "{Name:'minecraft:white_terracotta'}", "{Name:'minecraft:stained_hardened_clay',Properties:{color:'white'}}");
		putStates(2545, "{Name:'minecraft:orange_terracotta'}", "{Name:'minecraft:stained_hardened_clay',Properties:{color:'orange'}}");
		putStates(2546, "{Name:'minecraft:magenta_terracotta'}", "{Name:'minecraft:stained_hardened_clay',Properties:{color:'magenta'}}");
		putStates(2547, "{Name:'minecraft:light_blue_terracotta'}", "{Name:'minecraft:stained_hardened_clay',Properties:{color:'light_blue'}}");
		putStates(2548, "{Name:'minecraft:yellow_terracotta'}", "{Name:'minecraft:stained_hardened_clay',Properties:{color:'yellow'}}");
		putStates(2549, "{Name:'minecraft:lime_terracotta'}", "{Name:'minecraft:stained_hardened_clay',Properties:{color:'lime'}}");
		putStates(2550, "{Name:'minecraft:pink_terracotta'}", "{Name:'minecraft:stained_hardened_clay',Properties:{color:'pink'}}");
		putStates(2551, "{Name:'minecraft:gray_terracotta'}", "{Name:'minecraft:stained_hardened_clay',Properties:{color:'gray'}}");
		putStates(2552, "{Name:'minecraft:light_gray_terracotta'}", "{Name:'minecraft:stained_hardened_clay',Properties:{color:'silver'}}");
		putStates(2553, "{Name:'minecraft:cyan_terracotta'}", "{Name:'minecraft:stained_hardened_clay',Properties:{color:'cyan'}}");
		putStates(2554, "{Name:'minecraft:purple_terracotta'}", "{Name:'minecraft:stained_hardened_clay',Properties:{color:'purple'}}");
		putStates(2555, "{Name:'minecraft:blue_terracotta'}", "{Name:'minecraft:stained_hardened_clay',Properties:{color:'blue'}}");
		putStates(2556, "{Name:'minecraft:brown_terracotta'}", "{Name:'minecraft:stained_hardened_clay',Properties:{color:'brown'}}");
		putStates(2557, "{Name:'minecraft:green_terracotta'}", "{Name:'minecraft:stained_hardened_clay',Properties:{color:'green'}}");
		putStates(2558, "{Name:'minecraft:red_terracotta'}", "{Name:'minecraft:stained_hardened_clay',Properties:{color:'red'}}");
		putStates(2559, "{Name:'minecraft:black_terracotta'}", "{Name:'minecraft:stained_hardened_clay',Properties:{color:'black'}}");
	}

	/**
	 * Adds states to flatten from the blocks which had numeric IDs between 160 and 175 before 1.13.
	 */
	static void putStatesFromBlocks160To175() {
		putStates(
			2560,
			"{Name:'minecraft:white_stained_glass_pane',Properties:{east:'false',north:'false',south:'false',west:'false'}}",
			"{Name:'minecraft:stained_glass_pane',Properties:{color:'white',east:'false',north:'false',south:'false',west:'false'}}",
			"{Name:'minecraft:stained_glass_pane',Properties:{color:'white',east:'false',north:'false',south:'false',west:'true'}}",
			"{Name:'minecraft:stained_glass_pane',Properties:{color:'white',east:'false',north:'false',south:'true',west:'false'}}",
			"{Name:'minecraft:stained_glass_pane',Properties:{color:'white',east:'false',north:'false',south:'true',west:'true'}}",
			"{Name:'minecraft:stained_glass_pane',Properties:{color:'white',east:'false',north:'true',south:'false',west:'false'}}",
			"{Name:'minecraft:stained_glass_pane',Properties:{color:'white',east:'false',north:'true',south:'false',west:'true'}}",
			"{Name:'minecraft:stained_glass_pane',Properties:{color:'white',east:'false',north:'true',south:'true',west:'false'}}",
			"{Name:'minecraft:stained_glass_pane',Properties:{color:'white',east:'false',north:'true',south:'true',west:'true'}}",
			"{Name:'minecraft:stained_glass_pane',Properties:{color:'white',east:'true',north:'false',south:'false',west:'false'}}",
			"{Name:'minecraft:stained_glass_pane',Properties:{color:'white',east:'true',north:'false',south:'false',west:'true'}}",
			"{Name:'minecraft:stained_glass_pane',Properties:{color:'white',east:'true',north:'false',south:'true',west:'false'}}",
			"{Name:'minecraft:stained_glass_pane',Properties:{color:'white',east:'true',north:'false',south:'true',west:'true'}}",
			"{Name:'minecraft:stained_glass_pane',Properties:{color:'white',east:'true',north:'true',south:'false',west:'false'}}",
			"{Name:'minecraft:stained_glass_pane',Properties:{color:'white',east:'true',north:'true',south:'false',west:'true'}}",
			"{Name:'minecraft:stained_glass_pane',Properties:{color:'white',east:'true',north:'true',south:'true',west:'false'}}",
			"{Name:'minecraft:stained_glass_pane',Properties:{color:'white',east:'true',north:'true',south:'true',west:'true'}}"
		);
		putStates(
			2561,
			"{Name:'minecraft:orange_stained_glass_pane',Properties:{east:'false',north:'false',south:'false',west:'false'}}",
			"{Name:'minecraft:stained_glass_pane',Properties:{color:'orange',east:'false',north:'false',south:'false',west:'false'}}",
			"{Name:'minecraft:stained_glass_pane',Properties:{color:'orange',east:'false',north:'false',south:'false',west:'true'}}",
			"{Name:'minecraft:stained_glass_pane',Properties:{color:'orange',east:'false',north:'false',south:'true',west:'false'}}",
			"{Name:'minecraft:stained_glass_pane',Properties:{color:'orange',east:'false',north:'false',south:'true',west:'true'}}",
			"{Name:'minecraft:stained_glass_pane',Properties:{color:'orange',east:'false',north:'true',south:'false',west:'false'}}",
			"{Name:'minecraft:stained_glass_pane',Properties:{color:'orange',east:'false',north:'true',south:'false',west:'true'}}",
			"{Name:'minecraft:stained_glass_pane',Properties:{color:'orange',east:'false',north:'true',south:'true',west:'false'}}",
			"{Name:'minecraft:stained_glass_pane',Properties:{color:'orange',east:'false',north:'true',south:'true',west:'true'}}",
			"{Name:'minecraft:stained_glass_pane',Properties:{color:'orange',east:'true',north:'false',south:'false',west:'false'}}",
			"{Name:'minecraft:stained_glass_pane',Properties:{color:'orange',east:'true',north:'false',south:'false',west:'true'}}",
			"{Name:'minecraft:stained_glass_pane',Properties:{color:'orange',east:'true',north:'false',south:'true',west:'false'}}",
			"{Name:'minecraft:stained_glass_pane',Properties:{color:'orange',east:'true',north:'false',south:'true',west:'true'}}",
			"{Name:'minecraft:stained_glass_pane',Properties:{color:'orange',east:'true',north:'true',south:'false',west:'false'}}",
			"{Name:'minecraft:stained_glass_pane',Properties:{color:'orange',east:'true',north:'true',south:'false',west:'true'}}",
			"{Name:'minecraft:stained_glass_pane',Properties:{color:'orange',east:'true',north:'true',south:'true',west:'false'}}",
			"{Name:'minecraft:stained_glass_pane',Properties:{color:'orange',east:'true',north:'true',south:'true',west:'true'}}"
		);
		putStates(
			2562,
			"{Name:'minecraft:magenta_stained_glass_pane',Properties:{east:'false',north:'false',south:'false',west:'false'}}",
			"{Name:'minecraft:stained_glass_pane',Properties:{color:'magenta',east:'false',north:'false',south:'false',west:'false'}}",
			"{Name:'minecraft:stained_glass_pane',Properties:{color:'magenta',east:'false',north:'false',south:'false',west:'true'}}",
			"{Name:'minecraft:stained_glass_pane',Properties:{color:'magenta',east:'false',north:'false',south:'true',west:'false'}}",
			"{Name:'minecraft:stained_glass_pane',Properties:{color:'magenta',east:'false',north:'false',south:'true',west:'true'}}",
			"{Name:'minecraft:stained_glass_pane',Properties:{color:'magenta',east:'false',north:'true',south:'false',west:'false'}}",
			"{Name:'minecraft:stained_glass_pane',Properties:{color:'magenta',east:'false',north:'true',south:'false',west:'true'}}",
			"{Name:'minecraft:stained_glass_pane',Properties:{color:'magenta',east:'false',north:'true',south:'true',west:'false'}}",
			"{Name:'minecraft:stained_glass_pane',Properties:{color:'magenta',east:'false',north:'true',south:'true',west:'true'}}",
			"{Name:'minecraft:stained_glass_pane',Properties:{color:'magenta',east:'true',north:'false',south:'false',west:'false'}}",
			"{Name:'minecraft:stained_glass_pane',Properties:{color:'magenta',east:'true',north:'false',south:'false',west:'true'}}",
			"{Name:'minecraft:stained_glass_pane',Properties:{color:'magenta',east:'true',north:'false',south:'true',west:'false'}}",
			"{Name:'minecraft:stained_glass_pane',Properties:{color:'magenta',east:'true',north:'false',south:'true',west:'true'}}",
			"{Name:'minecraft:stained_glass_pane',Properties:{color:'magenta',east:'true',north:'true',south:'false',west:'false'}}",
			"{Name:'minecraft:stained_glass_pane',Properties:{color:'magenta',east:'true',north:'true',south:'false',west:'true'}}",
			"{Name:'minecraft:stained_glass_pane',Properties:{color:'magenta',east:'true',north:'true',south:'true',west:'false'}}",
			"{Name:'minecraft:stained_glass_pane',Properties:{color:'magenta',east:'true',north:'true',south:'true',west:'true'}}"
		);
		putStates(
			2563,
			"{Name:'minecraft:light_blue_stained_glass_pane',Properties:{east:'false',north:'false',south:'false',west:'false'}}",
			"{Name:'minecraft:stained_glass_pane',Properties:{color:'light_blue',east:'false',north:'false',south:'false',west:'false'}}",
			"{Name:'minecraft:stained_glass_pane',Properties:{color:'light_blue',east:'false',north:'false',south:'false',west:'true'}}",
			"{Name:'minecraft:stained_glass_pane',Properties:{color:'light_blue',east:'false',north:'false',south:'true',west:'false'}}",
			"{Name:'minecraft:stained_glass_pane',Properties:{color:'light_blue',east:'false',north:'false',south:'true',west:'true'}}",
			"{Name:'minecraft:stained_glass_pane',Properties:{color:'light_blue',east:'false',north:'true',south:'false',west:'false'}}",
			"{Name:'minecraft:stained_glass_pane',Properties:{color:'light_blue',east:'false',north:'true',south:'false',west:'true'}}",
			"{Name:'minecraft:stained_glass_pane',Properties:{color:'light_blue',east:'false',north:'true',south:'true',west:'false'}}",
			"{Name:'minecraft:stained_glass_pane',Properties:{color:'light_blue',east:'false',north:'true',south:'true',west:'true'}}",
			"{Name:'minecraft:stained_glass_pane',Properties:{color:'light_blue',east:'true',north:'false',south:'false',west:'false'}}",
			"{Name:'minecraft:stained_glass_pane',Properties:{color:'light_blue',east:'true',north:'false',south:'false',west:'true'}}",
			"{Name:'minecraft:stained_glass_pane',Properties:{color:'light_blue',east:'true',north:'false',south:'true',west:'false'}}",
			"{Name:'minecraft:stained_glass_pane',Properties:{color:'light_blue',east:'true',north:'false',south:'true',west:'true'}}",
			"{Name:'minecraft:stained_glass_pane',Properties:{color:'light_blue',east:'true',north:'true',south:'false',west:'false'}}",
			"{Name:'minecraft:stained_glass_pane',Properties:{color:'light_blue',east:'true',north:'true',south:'false',west:'true'}}",
			"{Name:'minecraft:stained_glass_pane',Properties:{color:'light_blue',east:'true',north:'true',south:'true',west:'false'}}",
			"{Name:'minecraft:stained_glass_pane',Properties:{color:'light_blue',east:'true',north:'true',south:'true',west:'true'}}"
		);
		putStates(
			2564,
			"{Name:'minecraft:yellow_stained_glass_pane',Properties:{east:'false',north:'false',south:'false',west:'false'}}",
			"{Name:'minecraft:stained_glass_pane',Properties:{color:'yellow',east:'false',north:'false',south:'false',west:'false'}}",
			"{Name:'minecraft:stained_glass_pane',Properties:{color:'yellow',east:'false',north:'false',south:'false',west:'true'}}",
			"{Name:'minecraft:stained_glass_pane',Properties:{color:'yellow',east:'false',north:'false',south:'true',west:'false'}}",
			"{Name:'minecraft:stained_glass_pane',Properties:{color:'yellow',east:'false',north:'false',south:'true',west:'true'}}",
			"{Name:'minecraft:stained_glass_pane',Properties:{color:'yellow',east:'false',north:'true',south:'false',west:'false'}}",
			"{Name:'minecraft:stained_glass_pane',Properties:{color:'yellow',east:'false',north:'true',south:'false',west:'true'}}",
			"{Name:'minecraft:stained_glass_pane',Properties:{color:'yellow',east:'false',north:'true',south:'true',west:'false'}}",
			"{Name:'minecraft:stained_glass_pane',Properties:{color:'yellow',east:'false',north:'true',south:'true',west:'true'}}",
			"{Name:'minecraft:stained_glass_pane',Properties:{color:'yellow',east:'true',north:'false',south:'false',west:'false'}}",
			"{Name:'minecraft:stained_glass_pane',Properties:{color:'yellow',east:'true',north:'false',south:'false',west:'true'}}",
			"{Name:'minecraft:stained_glass_pane',Properties:{color:'yellow',east:'true',north:'false',south:'true',west:'false'}}",
			"{Name:'minecraft:stained_glass_pane',Properties:{color:'yellow',east:'true',north:'false',south:'true',west:'true'}}",
			"{Name:'minecraft:stained_glass_pane',Properties:{color:'yellow',east:'true',north:'true',south:'false',west:'false'}}",
			"{Name:'minecraft:stained_glass_pane',Properties:{color:'yellow',east:'true',north:'true',south:'false',west:'true'}}",
			"{Name:'minecraft:stained_glass_pane',Properties:{color:'yellow',east:'true',north:'true',south:'true',west:'false'}}",
			"{Name:'minecraft:stained_glass_pane',Properties:{color:'yellow',east:'true',north:'true',south:'true',west:'true'}}"
		);
		putStates(
			2565,
			"{Name:'minecraft:lime_stained_glass_pane',Properties:{east:'false',north:'false',south:'false',west:'false'}}",
			"{Name:'minecraft:stained_glass_pane',Properties:{color:'lime',east:'false',north:'false',south:'false',west:'false'}}",
			"{Name:'minecraft:stained_glass_pane',Properties:{color:'lime',east:'false',north:'false',south:'false',west:'true'}}",
			"{Name:'minecraft:stained_glass_pane',Properties:{color:'lime',east:'false',north:'false',south:'true',west:'false'}}",
			"{Name:'minecraft:stained_glass_pane',Properties:{color:'lime',east:'false',north:'false',south:'true',west:'true'}}",
			"{Name:'minecraft:stained_glass_pane',Properties:{color:'lime',east:'false',north:'true',south:'false',west:'false'}}",
			"{Name:'minecraft:stained_glass_pane',Properties:{color:'lime',east:'false',north:'true',south:'false',west:'true'}}",
			"{Name:'minecraft:stained_glass_pane',Properties:{color:'lime',east:'false',north:'true',south:'true',west:'false'}}",
			"{Name:'minecraft:stained_glass_pane',Properties:{color:'lime',east:'false',north:'true',south:'true',west:'true'}}",
			"{Name:'minecraft:stained_glass_pane',Properties:{color:'lime',east:'true',north:'false',south:'false',west:'false'}}",
			"{Name:'minecraft:stained_glass_pane',Properties:{color:'lime',east:'true',north:'false',south:'false',west:'true'}}",
			"{Name:'minecraft:stained_glass_pane',Properties:{color:'lime',east:'true',north:'false',south:'true',west:'false'}}",
			"{Name:'minecraft:stained_glass_pane',Properties:{color:'lime',east:'true',north:'false',south:'true',west:'true'}}",
			"{Name:'minecraft:stained_glass_pane',Properties:{color:'lime',east:'true',north:'true',south:'false',west:'false'}}",
			"{Name:'minecraft:stained_glass_pane',Properties:{color:'lime',east:'true',north:'true',south:'false',west:'true'}}",
			"{Name:'minecraft:stained_glass_pane',Properties:{color:'lime',east:'true',north:'true',south:'true',west:'false'}}",
			"{Name:'minecraft:stained_glass_pane',Properties:{color:'lime',east:'true',north:'true',south:'true',west:'true'}}"
		);
		putStates(
			2566,
			"{Name:'minecraft:pink_stained_glass_pane',Properties:{east:'false',north:'false',south:'false',west:'false'}}",
			"{Name:'minecraft:stained_glass_pane',Properties:{color:'pink',east:'false',north:'false',south:'false',west:'false'}}",
			"{Name:'minecraft:stained_glass_pane',Properties:{color:'pink',east:'false',north:'false',south:'false',west:'true'}}",
			"{Name:'minecraft:stained_glass_pane',Properties:{color:'pink',east:'false',north:'false',south:'true',west:'false'}}",
			"{Name:'minecraft:stained_glass_pane',Properties:{color:'pink',east:'false',north:'false',south:'true',west:'true'}}",
			"{Name:'minecraft:stained_glass_pane',Properties:{color:'pink',east:'false',north:'true',south:'false',west:'false'}}",
			"{Name:'minecraft:stained_glass_pane',Properties:{color:'pink',east:'false',north:'true',south:'false',west:'true'}}",
			"{Name:'minecraft:stained_glass_pane',Properties:{color:'pink',east:'false',north:'true',south:'true',west:'false'}}",
			"{Name:'minecraft:stained_glass_pane',Properties:{color:'pink',east:'false',north:'true',south:'true',west:'true'}}",
			"{Name:'minecraft:stained_glass_pane',Properties:{color:'pink',east:'true',north:'false',south:'false',west:'false'}}",
			"{Name:'minecraft:stained_glass_pane',Properties:{color:'pink',east:'true',north:'false',south:'false',west:'true'}}",
			"{Name:'minecraft:stained_glass_pane',Properties:{color:'pink',east:'true',north:'false',south:'true',west:'false'}}",
			"{Name:'minecraft:stained_glass_pane',Properties:{color:'pink',east:'true',north:'false',south:'true',west:'true'}}",
			"{Name:'minecraft:stained_glass_pane',Properties:{color:'pink',east:'true',north:'true',south:'false',west:'false'}}",
			"{Name:'minecraft:stained_glass_pane',Properties:{color:'pink',east:'true',north:'true',south:'false',west:'true'}}",
			"{Name:'minecraft:stained_glass_pane',Properties:{color:'pink',east:'true',north:'true',south:'true',west:'false'}}",
			"{Name:'minecraft:stained_glass_pane',Properties:{color:'pink',east:'true',north:'true',south:'true',west:'true'}}"
		);
		putStates(
			2567,
			"{Name:'minecraft:gray_stained_glass_pane',Properties:{east:'false',north:'false',south:'false',west:'false'}}",
			"{Name:'minecraft:stained_glass_pane',Properties:{color:'gray',east:'false',north:'false',south:'false',west:'false'}}",
			"{Name:'minecraft:stained_glass_pane',Properties:{color:'gray',east:'false',north:'false',south:'false',west:'true'}}",
			"{Name:'minecraft:stained_glass_pane',Properties:{color:'gray',east:'false',north:'false',south:'true',west:'false'}}",
			"{Name:'minecraft:stained_glass_pane',Properties:{color:'gray',east:'false',north:'false',south:'true',west:'true'}}",
			"{Name:'minecraft:stained_glass_pane',Properties:{color:'gray',east:'false',north:'true',south:'false',west:'false'}}",
			"{Name:'minecraft:stained_glass_pane',Properties:{color:'gray',east:'false',north:'true',south:'false',west:'true'}}",
			"{Name:'minecraft:stained_glass_pane',Properties:{color:'gray',east:'false',north:'true',south:'true',west:'false'}}",
			"{Name:'minecraft:stained_glass_pane',Properties:{color:'gray',east:'false',north:'true',south:'true',west:'true'}}",
			"{Name:'minecraft:stained_glass_pane',Properties:{color:'gray',east:'true',north:'false',south:'false',west:'false'}}",
			"{Name:'minecraft:stained_glass_pane',Properties:{color:'gray',east:'true',north:'false',south:'false',west:'true'}}",
			"{Name:'minecraft:stained_glass_pane',Properties:{color:'gray',east:'true',north:'false',south:'true',west:'false'}}",
			"{Name:'minecraft:stained_glass_pane',Properties:{color:'gray',east:'true',north:'false',south:'true',west:'true'}}",
			"{Name:'minecraft:stained_glass_pane',Properties:{color:'gray',east:'true',north:'true',south:'false',west:'false'}}",
			"{Name:'minecraft:stained_glass_pane',Properties:{color:'gray',east:'true',north:'true',south:'false',west:'true'}}",
			"{Name:'minecraft:stained_glass_pane',Properties:{color:'gray',east:'true',north:'true',south:'true',west:'false'}}",
			"{Name:'minecraft:stained_glass_pane',Properties:{color:'gray',east:'true',north:'true',south:'true',west:'true'}}"
		);
		putStates(
			2568,
			"{Name:'minecraft:light_gray_stained_glass_pane',Properties:{east:'false',north:'false',south:'false',west:'false'}}",
			"{Name:'minecraft:stained_glass_pane',Properties:{color:'silver',east:'false',north:'false',south:'false',west:'false'}}",
			"{Name:'minecraft:stained_glass_pane',Properties:{color:'silver',east:'false',north:'false',south:'false',west:'true'}}",
			"{Name:'minecraft:stained_glass_pane',Properties:{color:'silver',east:'false',north:'false',south:'true',west:'false'}}",
			"{Name:'minecraft:stained_glass_pane',Properties:{color:'silver',east:'false',north:'false',south:'true',west:'true'}}",
			"{Name:'minecraft:stained_glass_pane',Properties:{color:'silver',east:'false',north:'true',south:'false',west:'false'}}",
			"{Name:'minecraft:stained_glass_pane',Properties:{color:'silver',east:'false',north:'true',south:'false',west:'true'}}",
			"{Name:'minecraft:stained_glass_pane',Properties:{color:'silver',east:'false',north:'true',south:'true',west:'false'}}",
			"{Name:'minecraft:stained_glass_pane',Properties:{color:'silver',east:'false',north:'true',south:'true',west:'true'}}",
			"{Name:'minecraft:stained_glass_pane',Properties:{color:'silver',east:'true',north:'false',south:'false',west:'false'}}",
			"{Name:'minecraft:stained_glass_pane',Properties:{color:'silver',east:'true',north:'false',south:'false',west:'true'}}",
			"{Name:'minecraft:stained_glass_pane',Properties:{color:'silver',east:'true',north:'false',south:'true',west:'false'}}",
			"{Name:'minecraft:stained_glass_pane',Properties:{color:'silver',east:'true',north:'false',south:'true',west:'true'}}",
			"{Name:'minecraft:stained_glass_pane',Properties:{color:'silver',east:'true',north:'true',south:'false',west:'false'}}",
			"{Name:'minecraft:stained_glass_pane',Properties:{color:'silver',east:'true',north:'true',south:'false',west:'true'}}",
			"{Name:'minecraft:stained_glass_pane',Properties:{color:'silver',east:'true',north:'true',south:'true',west:'false'}}",
			"{Name:'minecraft:stained_glass_pane',Properties:{color:'silver',east:'true',north:'true',south:'true',west:'true'}}"
		);
		putStates(
			2569,
			"{Name:'minecraft:cyan_stained_glass_pane',Properties:{east:'false',north:'false',south:'false',west:'false'}}",
			"{Name:'minecraft:stained_glass_pane',Properties:{color:'cyan',east:'false',north:'false',south:'false',west:'false'}}",
			"{Name:'minecraft:stained_glass_pane',Properties:{color:'cyan',east:'false',north:'false',south:'false',west:'true'}}",
			"{Name:'minecraft:stained_glass_pane',Properties:{color:'cyan',east:'false',north:'false',south:'true',west:'false'}}",
			"{Name:'minecraft:stained_glass_pane',Properties:{color:'cyan',east:'false',north:'false',south:'true',west:'true'}}",
			"{Name:'minecraft:stained_glass_pane',Properties:{color:'cyan',east:'false',north:'true',south:'false',west:'false'}}",
			"{Name:'minecraft:stained_glass_pane',Properties:{color:'cyan',east:'false',north:'true',south:'false',west:'true'}}",
			"{Name:'minecraft:stained_glass_pane',Properties:{color:'cyan',east:'false',north:'true',south:'true',west:'false'}}",
			"{Name:'minecraft:stained_glass_pane',Properties:{color:'cyan',east:'false',north:'true',south:'true',west:'true'}}",
			"{Name:'minecraft:stained_glass_pane',Properties:{color:'cyan',east:'true',north:'false',south:'false',west:'false'}}",
			"{Name:'minecraft:stained_glass_pane',Properties:{color:'cyan',east:'true',north:'false',south:'false',west:'true'}}",
			"{Name:'minecraft:stained_glass_pane',Properties:{color:'cyan',east:'true',north:'false',south:'true',west:'false'}}",
			"{Name:'minecraft:stained_glass_pane',Properties:{color:'cyan',east:'true',north:'false',south:'true',west:'true'}}",
			"{Name:'minecraft:stained_glass_pane',Properties:{color:'cyan',east:'true',north:'true',south:'false',west:'false'}}",
			"{Name:'minecraft:stained_glass_pane',Properties:{color:'cyan',east:'true',north:'true',south:'false',west:'true'}}",
			"{Name:'minecraft:stained_glass_pane',Properties:{color:'cyan',east:'true',north:'true',south:'true',west:'false'}}",
			"{Name:'minecraft:stained_glass_pane',Properties:{color:'cyan',east:'true',north:'true',south:'true',west:'true'}}"
		);
		putStates(
			2570,
			"{Name:'minecraft:purple_stained_glass_pane',Properties:{east:'false',north:'false',south:'false',west:'false'}}",
			"{Name:'minecraft:stained_glass_pane',Properties:{color:'purple',east:'false',north:'false',south:'false',west:'false'}}",
			"{Name:'minecraft:stained_glass_pane',Properties:{color:'purple',east:'false',north:'false',south:'false',west:'true'}}",
			"{Name:'minecraft:stained_glass_pane',Properties:{color:'purple',east:'false',north:'false',south:'true',west:'false'}}",
			"{Name:'minecraft:stained_glass_pane',Properties:{color:'purple',east:'false',north:'false',south:'true',west:'true'}}",
			"{Name:'minecraft:stained_glass_pane',Properties:{color:'purple',east:'false',north:'true',south:'false',west:'false'}}",
			"{Name:'minecraft:stained_glass_pane',Properties:{color:'purple',east:'false',north:'true',south:'false',west:'true'}}",
			"{Name:'minecraft:stained_glass_pane',Properties:{color:'purple',east:'false',north:'true',south:'true',west:'false'}}",
			"{Name:'minecraft:stained_glass_pane',Properties:{color:'purple',east:'false',north:'true',south:'true',west:'true'}}",
			"{Name:'minecraft:stained_glass_pane',Properties:{color:'purple',east:'true',north:'false',south:'false',west:'false'}}",
			"{Name:'minecraft:stained_glass_pane',Properties:{color:'purple',east:'true',north:'false',south:'false',west:'true'}}",
			"{Name:'minecraft:stained_glass_pane',Properties:{color:'purple',east:'true',north:'false',south:'true',west:'false'}}",
			"{Name:'minecraft:stained_glass_pane',Properties:{color:'purple',east:'true',north:'false',south:'true',west:'true'}}",
			"{Name:'minecraft:stained_glass_pane',Properties:{color:'purple',east:'true',north:'true',south:'false',west:'false'}}",
			"{Name:'minecraft:stained_glass_pane',Properties:{color:'purple',east:'true',north:'true',south:'false',west:'true'}}",
			"{Name:'minecraft:stained_glass_pane',Properties:{color:'purple',east:'true',north:'true',south:'true',west:'false'}}",
			"{Name:'minecraft:stained_glass_pane',Properties:{color:'purple',east:'true',north:'true',south:'true',west:'true'}}"
		);
		putStates(
			2571,
			"{Name:'minecraft:blue_stained_glass_pane',Properties:{east:'false',north:'false',south:'false',west:'false'}}",
			"{Name:'minecraft:stained_glass_pane',Properties:{color:'blue',east:'false',north:'false',south:'false',west:'false'}}",
			"{Name:'minecraft:stained_glass_pane',Properties:{color:'blue',east:'false',north:'false',south:'false',west:'true'}}",
			"{Name:'minecraft:stained_glass_pane',Properties:{color:'blue',east:'false',north:'false',south:'true',west:'false'}}",
			"{Name:'minecraft:stained_glass_pane',Properties:{color:'blue',east:'false',north:'false',south:'true',west:'true'}}",
			"{Name:'minecraft:stained_glass_pane',Properties:{color:'blue',east:'false',north:'true',south:'false',west:'false'}}",
			"{Name:'minecraft:stained_glass_pane',Properties:{color:'blue',east:'false',north:'true',south:'false',west:'true'}}",
			"{Name:'minecraft:stained_glass_pane',Properties:{color:'blue',east:'false',north:'true',south:'true',west:'false'}}",
			"{Name:'minecraft:stained_glass_pane',Properties:{color:'blue',east:'false',north:'true',south:'true',west:'true'}}",
			"{Name:'minecraft:stained_glass_pane',Properties:{color:'blue',east:'true',north:'false',south:'false',west:'false'}}",
			"{Name:'minecraft:stained_glass_pane',Properties:{color:'blue',east:'true',north:'false',south:'false',west:'true'}}",
			"{Name:'minecraft:stained_glass_pane',Properties:{color:'blue',east:'true',north:'false',south:'true',west:'false'}}",
			"{Name:'minecraft:stained_glass_pane',Properties:{color:'blue',east:'true',north:'false',south:'true',west:'true'}}",
			"{Name:'minecraft:stained_glass_pane',Properties:{color:'blue',east:'true',north:'true',south:'false',west:'false'}}",
			"{Name:'minecraft:stained_glass_pane',Properties:{color:'blue',east:'true',north:'true',south:'false',west:'true'}}",
			"{Name:'minecraft:stained_glass_pane',Properties:{color:'blue',east:'true',north:'true',south:'true',west:'false'}}",
			"{Name:'minecraft:stained_glass_pane',Properties:{color:'blue',east:'true',north:'true',south:'true',west:'true'}}"
		);
		putStates(
			2572,
			"{Name:'minecraft:brown_stained_glass_pane',Properties:{east:'false',north:'false',south:'false',west:'false'}}",
			"{Name:'minecraft:stained_glass_pane',Properties:{color:'brown',east:'false',north:'false',south:'false',west:'false'}}",
			"{Name:'minecraft:stained_glass_pane',Properties:{color:'brown',east:'false',north:'false',south:'false',west:'true'}}",
			"{Name:'minecraft:stained_glass_pane',Properties:{color:'brown',east:'false',north:'false',south:'true',west:'false'}}",
			"{Name:'minecraft:stained_glass_pane',Properties:{color:'brown',east:'false',north:'false',south:'true',west:'true'}}",
			"{Name:'minecraft:stained_glass_pane',Properties:{color:'brown',east:'false',north:'true',south:'false',west:'false'}}",
			"{Name:'minecraft:stained_glass_pane',Properties:{color:'brown',east:'false',north:'true',south:'false',west:'true'}}",
			"{Name:'minecraft:stained_glass_pane',Properties:{color:'brown',east:'false',north:'true',south:'true',west:'false'}}",
			"{Name:'minecraft:stained_glass_pane',Properties:{color:'brown',east:'false',north:'true',south:'true',west:'true'}}",
			"{Name:'minecraft:stained_glass_pane',Properties:{color:'brown',east:'true',north:'false',south:'false',west:'false'}}",
			"{Name:'minecraft:stained_glass_pane',Properties:{color:'brown',east:'true',north:'false',south:'false',west:'true'}}",
			"{Name:'minecraft:stained_glass_pane',Properties:{color:'brown',east:'true',north:'false',south:'true',west:'false'}}",
			"{Name:'minecraft:stained_glass_pane',Properties:{color:'brown',east:'true',north:'false',south:'true',west:'true'}}",
			"{Name:'minecraft:stained_glass_pane',Properties:{color:'brown',east:'true',north:'true',south:'false',west:'false'}}",
			"{Name:'minecraft:stained_glass_pane',Properties:{color:'brown',east:'true',north:'true',south:'false',west:'true'}}",
			"{Name:'minecraft:stained_glass_pane',Properties:{color:'brown',east:'true',north:'true',south:'true',west:'false'}}",
			"{Name:'minecraft:stained_glass_pane',Properties:{color:'brown',east:'true',north:'true',south:'true',west:'true'}}"
		);
		putStates(
			2573,
			"{Name:'minecraft:green_stained_glass_pane',Properties:{east:'false',north:'false',south:'false',west:'false'}}",
			"{Name:'minecraft:stained_glass_pane',Properties:{color:'green',east:'false',north:'false',south:'false',west:'false'}}",
			"{Name:'minecraft:stained_glass_pane',Properties:{color:'green',east:'false',north:'false',south:'false',west:'true'}}",
			"{Name:'minecraft:stained_glass_pane',Properties:{color:'green',east:'false',north:'false',south:'true',west:'false'}}",
			"{Name:'minecraft:stained_glass_pane',Properties:{color:'green',east:'false',north:'false',south:'true',west:'true'}}",
			"{Name:'minecraft:stained_glass_pane',Properties:{color:'green',east:'false',north:'true',south:'false',west:'false'}}",
			"{Name:'minecraft:stained_glass_pane',Properties:{color:'green',east:'false',north:'true',south:'false',west:'true'}}",
			"{Name:'minecraft:stained_glass_pane',Properties:{color:'green',east:'false',north:'true',south:'true',west:'false'}}",
			"{Name:'minecraft:stained_glass_pane',Properties:{color:'green',east:'false',north:'true',south:'true',west:'true'}}",
			"{Name:'minecraft:stained_glass_pane',Properties:{color:'green',east:'true',north:'false',south:'false',west:'false'}}",
			"{Name:'minecraft:stained_glass_pane',Properties:{color:'green',east:'true',north:'false',south:'false',west:'true'}}",
			"{Name:'minecraft:stained_glass_pane',Properties:{color:'green',east:'true',north:'false',south:'true',west:'false'}}",
			"{Name:'minecraft:stained_glass_pane',Properties:{color:'green',east:'true',north:'false',south:'true',west:'true'}}",
			"{Name:'minecraft:stained_glass_pane',Properties:{color:'green',east:'true',north:'true',south:'false',west:'false'}}",
			"{Name:'minecraft:stained_glass_pane',Properties:{color:'green',east:'true',north:'true',south:'false',west:'true'}}",
			"{Name:'minecraft:stained_glass_pane',Properties:{color:'green',east:'true',north:'true',south:'true',west:'false'}}",
			"{Name:'minecraft:stained_glass_pane',Properties:{color:'green',east:'true',north:'true',south:'true',west:'true'}}"
		);
		putStates(
			2574,
			"{Name:'minecraft:red_stained_glass_pane',Properties:{east:'false',north:'false',south:'false',west:'false'}}",
			"{Name:'minecraft:stained_glass_pane',Properties:{color:'red',east:'false',north:'false',south:'false',west:'false'}}",
			"{Name:'minecraft:stained_glass_pane',Properties:{color:'red',east:'false',north:'false',south:'false',west:'true'}}",
			"{Name:'minecraft:stained_glass_pane',Properties:{color:'red',east:'false',north:'false',south:'true',west:'false'}}",
			"{Name:'minecraft:stained_glass_pane',Properties:{color:'red',east:'false',north:'false',south:'true',west:'true'}}",
			"{Name:'minecraft:stained_glass_pane',Properties:{color:'red',east:'false',north:'true',south:'false',west:'false'}}",
			"{Name:'minecraft:stained_glass_pane',Properties:{color:'red',east:'false',north:'true',south:'false',west:'true'}}",
			"{Name:'minecraft:stained_glass_pane',Properties:{color:'red',east:'false',north:'true',south:'true',west:'false'}}",
			"{Name:'minecraft:stained_glass_pane',Properties:{color:'red',east:'false',north:'true',south:'true',west:'true'}}",
			"{Name:'minecraft:stained_glass_pane',Properties:{color:'red',east:'true',north:'false',south:'false',west:'false'}}",
			"{Name:'minecraft:stained_glass_pane',Properties:{color:'red',east:'true',north:'false',south:'false',west:'true'}}",
			"{Name:'minecraft:stained_glass_pane',Properties:{color:'red',east:'true',north:'false',south:'true',west:'false'}}",
			"{Name:'minecraft:stained_glass_pane',Properties:{color:'red',east:'true',north:'false',south:'true',west:'true'}}",
			"{Name:'minecraft:stained_glass_pane',Properties:{color:'red',east:'true',north:'true',south:'false',west:'false'}}",
			"{Name:'minecraft:stained_glass_pane',Properties:{color:'red',east:'true',north:'true',south:'false',west:'true'}}",
			"{Name:'minecraft:stained_glass_pane',Properties:{color:'red',east:'true',north:'true',south:'true',west:'false'}}",
			"{Name:'minecraft:stained_glass_pane',Properties:{color:'red',east:'true',north:'true',south:'true',west:'true'}}"
		);
		putStates(
			2575,
			"{Name:'minecraft:black_stained_glass_pane',Properties:{east:'false',north:'false',south:'false',west:'false'}}",
			"{Name:'minecraft:stained_glass_pane',Properties:{color:'black',east:'false',north:'false',south:'false',west:'false'}}",
			"{Name:'minecraft:stained_glass_pane',Properties:{color:'black',east:'false',north:'false',south:'false',west:'true'}}",
			"{Name:'minecraft:stained_glass_pane',Properties:{color:'black',east:'false',north:'false',south:'true',west:'false'}}",
			"{Name:'minecraft:stained_glass_pane',Properties:{color:'black',east:'false',north:'false',south:'true',west:'true'}}",
			"{Name:'minecraft:stained_glass_pane',Properties:{color:'black',east:'false',north:'true',south:'false',west:'false'}}",
			"{Name:'minecraft:stained_glass_pane',Properties:{color:'black',east:'false',north:'true',south:'false',west:'true'}}",
			"{Name:'minecraft:stained_glass_pane',Properties:{color:'black',east:'false',north:'true',south:'true',west:'false'}}",
			"{Name:'minecraft:stained_glass_pane',Properties:{color:'black',east:'false',north:'true',south:'true',west:'true'}}",
			"{Name:'minecraft:stained_glass_pane',Properties:{color:'black',east:'true',north:'false',south:'false',west:'false'}}",
			"{Name:'minecraft:stained_glass_pane',Properties:{color:'black',east:'true',north:'false',south:'false',west:'true'}}",
			"{Name:'minecraft:stained_glass_pane',Properties:{color:'black',east:'true',north:'false',south:'true',west:'false'}}",
			"{Name:'minecraft:stained_glass_pane',Properties:{color:'black',east:'true',north:'false',south:'true',west:'true'}}",
			"{Name:'minecraft:stained_glass_pane',Properties:{color:'black',east:'true',north:'true',south:'false',west:'false'}}",
			"{Name:'minecraft:stained_glass_pane',Properties:{color:'black',east:'true',north:'true',south:'false',west:'true'}}",
			"{Name:'minecraft:stained_glass_pane',Properties:{color:'black',east:'true',north:'true',south:'true',west:'false'}}",
			"{Name:'minecraft:stained_glass_pane',Properties:{color:'black',east:'true',north:'true',south:'true',west:'true'}}"
		);
		putStates(
			2576,
			"{Name:'minecraft:acacia_leaves',Properties:{check_decay:'false',decayable:'true'}}",
			"{Name:'minecraft:leaves2',Properties:{check_decay:'false',decayable:'true',variant:'acacia'}}"
		);
		putStates(
			2577,
			"{Name:'minecraft:dark_oak_leaves',Properties:{check_decay:'false',decayable:'true'}}",
			"{Name:'minecraft:leaves2',Properties:{check_decay:'false',decayable:'true',variant:'dark_oak'}}"
		);
		putStates(
			2580,
			"{Name:'minecraft:acacia_leaves',Properties:{check_decay:'false',decayable:'false'}}",
			"{Name:'minecraft:leaves2',Properties:{check_decay:'false',decayable:'false',variant:'acacia'}}"
		);
		putStates(
			2581,
			"{Name:'minecraft:dark_oak_leaves',Properties:{check_decay:'false',decayable:'false'}}",
			"{Name:'minecraft:leaves2',Properties:{check_decay:'false',decayable:'false',variant:'dark_oak'}}"
		);
		putStates(
			2584,
			"{Name:'minecraft:acacia_leaves',Properties:{check_decay:'true',decayable:'true'}}",
			"{Name:'minecraft:leaves2',Properties:{check_decay:'true',decayable:'true',variant:'acacia'}}"
		);
		putStates(
			2585,
			"{Name:'minecraft:dark_oak_leaves',Properties:{check_decay:'true',decayable:'true'}}",
			"{Name:'minecraft:leaves2',Properties:{check_decay:'true',decayable:'true',variant:'dark_oak'}}"
		);
		putStates(
			2588,
			"{Name:'minecraft:acacia_leaves',Properties:{check_decay:'true',decayable:'false'}}",
			"{Name:'minecraft:leaves2',Properties:{check_decay:'true',decayable:'false',variant:'acacia'}}"
		);
		putStates(
			2589,
			"{Name:'minecraft:dark_oak_leaves',Properties:{check_decay:'true',decayable:'false'}}",
			"{Name:'minecraft:leaves2',Properties:{check_decay:'true',decayable:'false',variant:'dark_oak'}}"
		);
		putStates(2592, "{Name:'minecraft:acacia_log',Properties:{axis:'y'}}", "{Name:'minecraft:log2',Properties:{axis:'y',variant:'acacia'}}");
		putStates(2593, "{Name:'minecraft:dark_oak_log',Properties:{axis:'y'}}", "{Name:'minecraft:log2',Properties:{axis:'y',variant:'dark_oak'}}");
		putStates(2596, "{Name:'minecraft:acacia_log',Properties:{axis:'x'}}", "{Name:'minecraft:log2',Properties:{axis:'x',variant:'acacia'}}");
		putStates(2597, "{Name:'minecraft:dark_oak_log',Properties:{axis:'x'}}", "{Name:'minecraft:log2',Properties:{axis:'x',variant:'dark_oak'}}");
		putStates(2600, "{Name:'minecraft:acacia_log',Properties:{axis:'z'}}", "{Name:'minecraft:log2',Properties:{axis:'z',variant:'acacia'}}");
		putStates(2601, "{Name:'minecraft:dark_oak_log',Properties:{axis:'z'}}", "{Name:'minecraft:log2',Properties:{axis:'z',variant:'dark_oak'}}");
		putStates(2604, "{Name:'minecraft:acacia_bark'}", "{Name:'minecraft:log2',Properties:{axis:'none',variant:'acacia'}}");
		putStates(2605, "{Name:'minecraft:dark_oak_bark'}", "{Name:'minecraft:log2',Properties:{axis:'none',variant:'dark_oak'}}");
		putStates(
			2608,
			"{Name:'minecraft:acacia_stairs',Properties:{facing:'east',half:'bottom',shape:'straight'}}",
			"{Name:'minecraft:acacia_stairs',Properties:{facing:'east',half:'bottom',shape:'inner_left'}}",
			"{Name:'minecraft:acacia_stairs',Properties:{facing:'east',half:'bottom',shape:'inner_right'}}",
			"{Name:'minecraft:acacia_stairs',Properties:{facing:'east',half:'bottom',shape:'outer_left'}}",
			"{Name:'minecraft:acacia_stairs',Properties:{facing:'east',half:'bottom',shape:'outer_right'}}",
			"{Name:'minecraft:acacia_stairs',Properties:{facing:'east',half:'bottom',shape:'straight'}}"
		);
		putStates(
			2609,
			"{Name:'minecraft:acacia_stairs',Properties:{facing:'west',half:'bottom',shape:'straight'}}",
			"{Name:'minecraft:acacia_stairs',Properties:{facing:'west',half:'bottom',shape:'inner_left'}}",
			"{Name:'minecraft:acacia_stairs',Properties:{facing:'west',half:'bottom',shape:'inner_right'}}",
			"{Name:'minecraft:acacia_stairs',Properties:{facing:'west',half:'bottom',shape:'outer_left'}}",
			"{Name:'minecraft:acacia_stairs',Properties:{facing:'west',half:'bottom',shape:'outer_right'}}",
			"{Name:'minecraft:acacia_stairs',Properties:{facing:'west',half:'bottom',shape:'straight'}}"
		);
		putStates(
			2610,
			"{Name:'minecraft:acacia_stairs',Properties:{facing:'south',half:'bottom',shape:'straight'}}",
			"{Name:'minecraft:acacia_stairs',Properties:{facing:'south',half:'bottom',shape:'inner_left'}}",
			"{Name:'minecraft:acacia_stairs',Properties:{facing:'south',half:'bottom',shape:'inner_right'}}",
			"{Name:'minecraft:acacia_stairs',Properties:{facing:'south',half:'bottom',shape:'outer_left'}}",
			"{Name:'minecraft:acacia_stairs',Properties:{facing:'south',half:'bottom',shape:'outer_right'}}",
			"{Name:'minecraft:acacia_stairs',Properties:{facing:'south',half:'bottom',shape:'straight'}}"
		);
		putStates(
			2611,
			"{Name:'minecraft:acacia_stairs',Properties:{facing:'north',half:'bottom',shape:'straight'}}",
			"{Name:'minecraft:acacia_stairs',Properties:{facing:'north',half:'bottom',shape:'inner_left'}}",
			"{Name:'minecraft:acacia_stairs',Properties:{facing:'north',half:'bottom',shape:'inner_right'}}",
			"{Name:'minecraft:acacia_stairs',Properties:{facing:'north',half:'bottom',shape:'outer_left'}}",
			"{Name:'minecraft:acacia_stairs',Properties:{facing:'north',half:'bottom',shape:'outer_right'}}",
			"{Name:'minecraft:acacia_stairs',Properties:{facing:'north',half:'bottom',shape:'straight'}}"
		);
		putStates(
			2612,
			"{Name:'minecraft:acacia_stairs',Properties:{facing:'east',half:'top',shape:'straight'}}",
			"{Name:'minecraft:acacia_stairs',Properties:{facing:'east',half:'top',shape:'inner_left'}}",
			"{Name:'minecraft:acacia_stairs',Properties:{facing:'east',half:'top',shape:'inner_right'}}",
			"{Name:'minecraft:acacia_stairs',Properties:{facing:'east',half:'top',shape:'outer_left'}}",
			"{Name:'minecraft:acacia_stairs',Properties:{facing:'east',half:'top',shape:'outer_right'}}",
			"{Name:'minecraft:acacia_stairs',Properties:{facing:'east',half:'top',shape:'straight'}}"
		);
		putStates(
			2613,
			"{Name:'minecraft:acacia_stairs',Properties:{facing:'west',half:'top',shape:'straight'}}",
			"{Name:'minecraft:acacia_stairs',Properties:{facing:'west',half:'top',shape:'inner_left'}}",
			"{Name:'minecraft:acacia_stairs',Properties:{facing:'west',half:'top',shape:'inner_right'}}",
			"{Name:'minecraft:acacia_stairs',Properties:{facing:'west',half:'top',shape:'outer_left'}}",
			"{Name:'minecraft:acacia_stairs',Properties:{facing:'west',half:'top',shape:'outer_right'}}",
			"{Name:'minecraft:acacia_stairs',Properties:{facing:'west',half:'top',shape:'straight'}}"
		);
		putStates(
			2614,
			"{Name:'minecraft:acacia_stairs',Properties:{facing:'south',half:'top',shape:'straight'}}",
			"{Name:'minecraft:acacia_stairs',Properties:{facing:'south',half:'top',shape:'inner_left'}}",
			"{Name:'minecraft:acacia_stairs',Properties:{facing:'south',half:'top',shape:'inner_right'}}",
			"{Name:'minecraft:acacia_stairs',Properties:{facing:'south',half:'top',shape:'outer_left'}}",
			"{Name:'minecraft:acacia_stairs',Properties:{facing:'south',half:'top',shape:'outer_right'}}",
			"{Name:'minecraft:acacia_stairs',Properties:{facing:'south',half:'top',shape:'straight'}}"
		);
		putStates(
			2615,
			"{Name:'minecraft:acacia_stairs',Properties:{facing:'north',half:'top',shape:'straight'}}",
			"{Name:'minecraft:acacia_stairs',Properties:{facing:'north',half:'top',shape:'inner_left'}}",
			"{Name:'minecraft:acacia_stairs',Properties:{facing:'north',half:'top',shape:'inner_right'}}",
			"{Name:'minecraft:acacia_stairs',Properties:{facing:'north',half:'top',shape:'outer_left'}}",
			"{Name:'minecraft:acacia_stairs',Properties:{facing:'north',half:'top',shape:'outer_right'}}",
			"{Name:'minecraft:acacia_stairs',Properties:{facing:'north',half:'top',shape:'straight'}}"
		);
		putStates(
			2624,
			"{Name:'minecraft:dark_oak_stairs',Properties:{facing:'east',half:'bottom',shape:'straight'}}",
			"{Name:'minecraft:dark_oak_stairs',Properties:{facing:'east',half:'bottom',shape:'inner_left'}}",
			"{Name:'minecraft:dark_oak_stairs',Properties:{facing:'east',half:'bottom',shape:'inner_right'}}",
			"{Name:'minecraft:dark_oak_stairs',Properties:{facing:'east',half:'bottom',shape:'outer_left'}}",
			"{Name:'minecraft:dark_oak_stairs',Properties:{facing:'east',half:'bottom',shape:'outer_right'}}",
			"{Name:'minecraft:dark_oak_stairs',Properties:{facing:'east',half:'bottom',shape:'straight'}}"
		);
		putStates(
			2625,
			"{Name:'minecraft:dark_oak_stairs',Properties:{facing:'west',half:'bottom',shape:'straight'}}",
			"{Name:'minecraft:dark_oak_stairs',Properties:{facing:'west',half:'bottom',shape:'inner_left'}}",
			"{Name:'minecraft:dark_oak_stairs',Properties:{facing:'west',half:'bottom',shape:'inner_right'}}",
			"{Name:'minecraft:dark_oak_stairs',Properties:{facing:'west',half:'bottom',shape:'outer_left'}}",
			"{Name:'minecraft:dark_oak_stairs',Properties:{facing:'west',half:'bottom',shape:'outer_right'}}",
			"{Name:'minecraft:dark_oak_stairs',Properties:{facing:'west',half:'bottom',shape:'straight'}}"
		);
		putStates(
			2626,
			"{Name:'minecraft:dark_oak_stairs',Properties:{facing:'south',half:'bottom',shape:'straight'}}",
			"{Name:'minecraft:dark_oak_stairs',Properties:{facing:'south',half:'bottom',shape:'inner_left'}}",
			"{Name:'minecraft:dark_oak_stairs',Properties:{facing:'south',half:'bottom',shape:'inner_right'}}",
			"{Name:'minecraft:dark_oak_stairs',Properties:{facing:'south',half:'bottom',shape:'outer_left'}}",
			"{Name:'minecraft:dark_oak_stairs',Properties:{facing:'south',half:'bottom',shape:'outer_right'}}",
			"{Name:'minecraft:dark_oak_stairs',Properties:{facing:'south',half:'bottom',shape:'straight'}}"
		);
		putStates(
			2627,
			"{Name:'minecraft:dark_oak_stairs',Properties:{facing:'north',half:'bottom',shape:'straight'}}",
			"{Name:'minecraft:dark_oak_stairs',Properties:{facing:'north',half:'bottom',shape:'inner_left'}}",
			"{Name:'minecraft:dark_oak_stairs',Properties:{facing:'north',half:'bottom',shape:'inner_right'}}",
			"{Name:'minecraft:dark_oak_stairs',Properties:{facing:'north',half:'bottom',shape:'outer_left'}}",
			"{Name:'minecraft:dark_oak_stairs',Properties:{facing:'north',half:'bottom',shape:'outer_right'}}",
			"{Name:'minecraft:dark_oak_stairs',Properties:{facing:'north',half:'bottom',shape:'straight'}}"
		);
		putStates(
			2628,
			"{Name:'minecraft:dark_oak_stairs',Properties:{facing:'east',half:'top',shape:'straight'}}",
			"{Name:'minecraft:dark_oak_stairs',Properties:{facing:'east',half:'top',shape:'inner_left'}}",
			"{Name:'minecraft:dark_oak_stairs',Properties:{facing:'east',half:'top',shape:'inner_right'}}",
			"{Name:'minecraft:dark_oak_stairs',Properties:{facing:'east',half:'top',shape:'outer_left'}}",
			"{Name:'minecraft:dark_oak_stairs',Properties:{facing:'east',half:'top',shape:'outer_right'}}",
			"{Name:'minecraft:dark_oak_stairs',Properties:{facing:'east',half:'top',shape:'straight'}}"
		);
		putStates(
			2629,
			"{Name:'minecraft:dark_oak_stairs',Properties:{facing:'west',half:'top',shape:'straight'}}",
			"{Name:'minecraft:dark_oak_stairs',Properties:{facing:'west',half:'top',shape:'inner_left'}}",
			"{Name:'minecraft:dark_oak_stairs',Properties:{facing:'west',half:'top',shape:'inner_right'}}",
			"{Name:'minecraft:dark_oak_stairs',Properties:{facing:'west',half:'top',shape:'outer_left'}}",
			"{Name:'minecraft:dark_oak_stairs',Properties:{facing:'west',half:'top',shape:'outer_right'}}",
			"{Name:'minecraft:dark_oak_stairs',Properties:{facing:'west',half:'top',shape:'straight'}}"
		);
		putStates(
			2630,
			"{Name:'minecraft:dark_oak_stairs',Properties:{facing:'south',half:'top',shape:'straight'}}",
			"{Name:'minecraft:dark_oak_stairs',Properties:{facing:'south',half:'top',shape:'inner_left'}}",
			"{Name:'minecraft:dark_oak_stairs',Properties:{facing:'south',half:'top',shape:'inner_right'}}",
			"{Name:'minecraft:dark_oak_stairs',Properties:{facing:'south',half:'top',shape:'outer_left'}}",
			"{Name:'minecraft:dark_oak_stairs',Properties:{facing:'south',half:'top',shape:'outer_right'}}",
			"{Name:'minecraft:dark_oak_stairs',Properties:{facing:'south',half:'top',shape:'straight'}}"
		);
		putStates(
			2631,
			"{Name:'minecraft:dark_oak_stairs',Properties:{facing:'north',half:'top',shape:'straight'}}",
			"{Name:'minecraft:dark_oak_stairs',Properties:{facing:'north',half:'top',shape:'inner_left'}}",
			"{Name:'minecraft:dark_oak_stairs',Properties:{facing:'north',half:'top',shape:'inner_right'}}",
			"{Name:'minecraft:dark_oak_stairs',Properties:{facing:'north',half:'top',shape:'outer_left'}}",
			"{Name:'minecraft:dark_oak_stairs',Properties:{facing:'north',half:'top',shape:'outer_right'}}",
			"{Name:'minecraft:dark_oak_stairs',Properties:{facing:'north',half:'top',shape:'straight'}}"
		);
		putStates(2640, "{Name:'minecraft:slime_block'}", "{Name:'minecraft:slime'}");
		putStates(2656, "{Name:'minecraft:barrier'}", "{Name:'minecraft:barrier'}");
		putStates(
			2672,
			"{Name:'minecraft:iron_trapdoor',Properties:{facing:'north',half:'bottom',open:'false'}}",
			"{Name:'minecraft:iron_trapdoor',Properties:{facing:'north',half:'bottom',open:'false'}}"
		);
		putStates(
			2673,
			"{Name:'minecraft:iron_trapdoor',Properties:{facing:'south',half:'bottom',open:'false'}}",
			"{Name:'minecraft:iron_trapdoor',Properties:{facing:'south',half:'bottom',open:'false'}}"
		);
		putStates(
			2674,
			"{Name:'minecraft:iron_trapdoor',Properties:{facing:'west',half:'bottom',open:'false'}}",
			"{Name:'minecraft:iron_trapdoor',Properties:{facing:'west',half:'bottom',open:'false'}}"
		);
		putStates(
			2675,
			"{Name:'minecraft:iron_trapdoor',Properties:{facing:'east',half:'bottom',open:'false'}}",
			"{Name:'minecraft:iron_trapdoor',Properties:{facing:'east',half:'bottom',open:'false'}}"
		);
		putStates(
			2676,
			"{Name:'minecraft:iron_trapdoor',Properties:{facing:'north',half:'bottom',open:'true'}}",
			"{Name:'minecraft:iron_trapdoor',Properties:{facing:'north',half:'bottom',open:'true'}}"
		);
		putStates(
			2677,
			"{Name:'minecraft:iron_trapdoor',Properties:{facing:'south',half:'bottom',open:'true'}}",
			"{Name:'minecraft:iron_trapdoor',Properties:{facing:'south',half:'bottom',open:'true'}}"
		);
		putStates(
			2678,
			"{Name:'minecraft:iron_trapdoor',Properties:{facing:'west',half:'bottom',open:'true'}}",
			"{Name:'minecraft:iron_trapdoor',Properties:{facing:'west',half:'bottom',open:'true'}}"
		);
		putStates(
			2679,
			"{Name:'minecraft:iron_trapdoor',Properties:{facing:'east',half:'bottom',open:'true'}}",
			"{Name:'minecraft:iron_trapdoor',Properties:{facing:'east',half:'bottom',open:'true'}}"
		);
		putStates(
			2680,
			"{Name:'minecraft:iron_trapdoor',Properties:{facing:'north',half:'top',open:'false'}}",
			"{Name:'minecraft:iron_trapdoor',Properties:{facing:'north',half:'top',open:'false'}}"
		);
		putStates(
			2681,
			"{Name:'minecraft:iron_trapdoor',Properties:{facing:'south',half:'top',open:'false'}}",
			"{Name:'minecraft:iron_trapdoor',Properties:{facing:'south',half:'top',open:'false'}}"
		);
		putStates(
			2682,
			"{Name:'minecraft:iron_trapdoor',Properties:{facing:'west',half:'top',open:'false'}}",
			"{Name:'minecraft:iron_trapdoor',Properties:{facing:'west',half:'top',open:'false'}}"
		);
		putStates(
			2683,
			"{Name:'minecraft:iron_trapdoor',Properties:{facing:'east',half:'top',open:'false'}}",
			"{Name:'minecraft:iron_trapdoor',Properties:{facing:'east',half:'top',open:'false'}}"
		);
		putStates(
			2684,
			"{Name:'minecraft:iron_trapdoor',Properties:{facing:'north',half:'top',open:'true'}}",
			"{Name:'minecraft:iron_trapdoor',Properties:{facing:'north',half:'top',open:'true'}}"
		);
		putStates(
			2685,
			"{Name:'minecraft:iron_trapdoor',Properties:{facing:'south',half:'top',open:'true'}}",
			"{Name:'minecraft:iron_trapdoor',Properties:{facing:'south',half:'top',open:'true'}}"
		);
		putStates(
			2686,
			"{Name:'minecraft:iron_trapdoor',Properties:{facing:'west',half:'top',open:'true'}}",
			"{Name:'minecraft:iron_trapdoor',Properties:{facing:'west',half:'top',open:'true'}}"
		);
		putStates(
			2687,
			"{Name:'minecraft:iron_trapdoor',Properties:{facing:'east',half:'top',open:'true'}}",
			"{Name:'minecraft:iron_trapdoor',Properties:{facing:'east',half:'top',open:'true'}}"
		);
		putStates(2688, "{Name:'minecraft:prismarine'}", "{Name:'minecraft:prismarine',Properties:{variant:'prismarine'}}");
		putStates(2689, "{Name:'minecraft:prismarine_bricks'}", "{Name:'minecraft:prismarine',Properties:{variant:'prismarine_bricks'}}");
		putStates(2690, "{Name:'minecraft:dark_prismarine'}", "{Name:'minecraft:prismarine',Properties:{variant:'dark_prismarine'}}");
		putStates(2704, "{Name:'minecraft:sea_lantern'}", "{Name:'minecraft:sea_lantern'}");
		putStates(2720, "{Name:'minecraft:hay_block',Properties:{axis:'y'}}", "{Name:'minecraft:hay_block',Properties:{axis:'y'}}");
		putStates(2724, "{Name:'minecraft:hay_block',Properties:{axis:'x'}}", "{Name:'minecraft:hay_block',Properties:{axis:'x'}}");
		putStates(2728, "{Name:'minecraft:hay_block',Properties:{axis:'z'}}", "{Name:'minecraft:hay_block',Properties:{axis:'z'}}");
		putStates(2736, "{Name:'minecraft:white_carpet'}", "{Name:'minecraft:carpet',Properties:{color:'white'}}");
		putStates(2737, "{Name:'minecraft:orange_carpet'}", "{Name:'minecraft:carpet',Properties:{color:'orange'}}");
		putStates(2738, "{Name:'minecraft:magenta_carpet'}", "{Name:'minecraft:carpet',Properties:{color:'magenta'}}");
		putStates(2739, "{Name:'minecraft:light_blue_carpet'}", "{Name:'minecraft:carpet',Properties:{color:'light_blue'}}");
		putStates(2740, "{Name:'minecraft:yellow_carpet'}", "{Name:'minecraft:carpet',Properties:{color:'yellow'}}");
		putStates(2741, "{Name:'minecraft:lime_carpet'}", "{Name:'minecraft:carpet',Properties:{color:'lime'}}");
		putStates(2742, "{Name:'minecraft:pink_carpet'}", "{Name:'minecraft:carpet',Properties:{color:'pink'}}");
		putStates(2743, "{Name:'minecraft:gray_carpet'}", "{Name:'minecraft:carpet',Properties:{color:'gray'}}");
		putStates(2744, "{Name:'minecraft:light_gray_carpet'}", "{Name:'minecraft:carpet',Properties:{color:'silver'}}");
		putStates(2745, "{Name:'minecraft:cyan_carpet'}", "{Name:'minecraft:carpet',Properties:{color:'cyan'}}");
		putStates(2746, "{Name:'minecraft:purple_carpet'}", "{Name:'minecraft:carpet',Properties:{color:'purple'}}");
		putStates(2747, "{Name:'minecraft:blue_carpet'}", "{Name:'minecraft:carpet',Properties:{color:'blue'}}");
		putStates(2748, "{Name:'minecraft:brown_carpet'}", "{Name:'minecraft:carpet',Properties:{color:'brown'}}");
		putStates(2749, "{Name:'minecraft:green_carpet'}", "{Name:'minecraft:carpet',Properties:{color:'green'}}");
		putStates(2750, "{Name:'minecraft:red_carpet'}", "{Name:'minecraft:carpet',Properties:{color:'red'}}");
		putStates(2751, "{Name:'minecraft:black_carpet'}", "{Name:'minecraft:carpet',Properties:{color:'black'}}");
		putStates(2752, "{Name:'minecraft:terracotta'}", "{Name:'minecraft:hardened_clay'}");
		putStates(2768, "{Name:'minecraft:coal_block'}", "{Name:'minecraft:coal_block'}");
		putStates(2784, "{Name:'minecraft:packed_ice'}", "{Name:'minecraft:packed_ice'}");
		putStates(
			2800,
			"{Name:'minecraft:sunflower',Properties:{half:'lower'}}",
			"{Name:'minecraft:double_plant',Properties:{facing:'east',half:'lower',variant:'sunflower'}}",
			"{Name:'minecraft:double_plant',Properties:{facing:'north',half:'lower',variant:'sunflower'}}",
			"{Name:'minecraft:double_plant',Properties:{facing:'south',half:'lower',variant:'sunflower'}}",
			"{Name:'minecraft:double_plant',Properties:{facing:'west',half:'lower',variant:'sunflower'}}"
		);
		putStates(
			2801,
			"{Name:'minecraft:lilac',Properties:{half:'lower'}}",
			"{Name:'minecraft:double_plant',Properties:{facing:'east',half:'lower',variant:'syringa'}}",
			"{Name:'minecraft:double_plant',Properties:{facing:'north',half:'lower',variant:'syringa'}}",
			"{Name:'minecraft:double_plant',Properties:{facing:'south',half:'lower',variant:'syringa'}}",
			"{Name:'minecraft:double_plant',Properties:{facing:'west',half:'lower',variant:'syringa'}}"
		);
		putStates(
			2802,
			"{Name:'minecraft:tall_grass',Properties:{half:'lower'}}",
			"{Name:'minecraft:double_plant',Properties:{facing:'east',half:'lower',variant:'double_grass'}}",
			"{Name:'minecraft:double_plant',Properties:{facing:'north',half:'lower',variant:'double_grass'}}",
			"{Name:'minecraft:double_plant',Properties:{facing:'south',half:'lower',variant:'double_grass'}}",
			"{Name:'minecraft:double_plant',Properties:{facing:'west',half:'lower',variant:'double_grass'}}"
		);
		putStates(
			2803,
			"{Name:'minecraft:large_fern',Properties:{half:'lower'}}",
			"{Name:'minecraft:double_plant',Properties:{facing:'east',half:'lower',variant:'double_fern'}}",
			"{Name:'minecraft:double_plant',Properties:{facing:'north',half:'lower',variant:'double_fern'}}",
			"{Name:'minecraft:double_plant',Properties:{facing:'south',half:'lower',variant:'double_fern'}}",
			"{Name:'minecraft:double_plant',Properties:{facing:'west',half:'lower',variant:'double_fern'}}"
		);
		putStates(
			2804,
			"{Name:'minecraft:rose_bush',Properties:{half:'lower'}}",
			"{Name:'minecraft:double_plant',Properties:{facing:'east',half:'lower',variant:'double_rose'}}",
			"{Name:'minecraft:double_plant',Properties:{facing:'north',half:'lower',variant:'double_rose'}}",
			"{Name:'minecraft:double_plant',Properties:{facing:'south',half:'lower',variant:'double_rose'}}",
			"{Name:'minecraft:double_plant',Properties:{facing:'west',half:'lower',variant:'double_rose'}}"
		);
		putStates(
			2805,
			"{Name:'minecraft:peony',Properties:{half:'lower'}}",
			"{Name:'minecraft:double_plant',Properties:{facing:'east',half:'lower',variant:'paeonia'}}",
			"{Name:'minecraft:double_plant',Properties:{facing:'north',half:'lower',variant:'paeonia'}}",
			"{Name:'minecraft:double_plant',Properties:{facing:'south',half:'lower',variant:'paeonia'}}",
			"{Name:'minecraft:double_plant',Properties:{facing:'west',half:'lower',variant:'paeonia'}}"
		);
		putStates(
			2808,
			"{Name:'minecraft:peony',Properties:{half:'upper'}}",
			"{Name:'minecraft:double_plant',Properties:{facing:'south',half:'upper',variant:'double_fern'}}",
			"{Name:'minecraft:double_plant',Properties:{facing:'south',half:'upper',variant:'double_grass'}}",
			"{Name:'minecraft:double_plant',Properties:{facing:'south',half:'upper',variant:'double_rose'}}",
			"{Name:'minecraft:double_plant',Properties:{facing:'south',half:'upper',variant:'paeonia'}}",
			"{Name:'minecraft:double_plant',Properties:{facing:'south',half:'upper',variant:'sunflower'}}",
			"{Name:'minecraft:double_plant',Properties:{facing:'south',half:'upper',variant:'syringa'}}"
		);
		putStates(
			2809,
			"{Name:'minecraft:peony',Properties:{half:'upper'}}",
			"{Name:'minecraft:double_plant',Properties:{facing:'west',half:'upper',variant:'double_fern'}}",
			"{Name:'minecraft:double_plant',Properties:{facing:'west',half:'upper',variant:'double_grass'}}",
			"{Name:'minecraft:double_plant',Properties:{facing:'west',half:'upper',variant:'double_rose'}}",
			"{Name:'minecraft:double_plant',Properties:{facing:'west',half:'upper',variant:'paeonia'}}",
			"{Name:'minecraft:double_plant',Properties:{facing:'west',half:'upper',variant:'sunflower'}}",
			"{Name:'minecraft:double_plant',Properties:{facing:'west',half:'upper',variant:'syringa'}}"
		);
		putStates(
			2810,
			"{Name:'minecraft:peony',Properties:{half:'upper'}}",
			"{Name:'minecraft:double_plant',Properties:{facing:'north',half:'upper',variant:'double_fern'}}",
			"{Name:'minecraft:double_plant',Properties:{facing:'north',half:'upper',variant:'double_grass'}}",
			"{Name:'minecraft:double_plant',Properties:{facing:'north',half:'upper',variant:'double_rose'}}",
			"{Name:'minecraft:double_plant',Properties:{facing:'north',half:'upper',variant:'paeonia'}}",
			"{Name:'minecraft:double_plant',Properties:{facing:'north',half:'upper',variant:'sunflower'}}",
			"{Name:'minecraft:double_plant',Properties:{facing:'north',half:'upper',variant:'syringa'}}"
		);
		putStates(
			2811,
			"{Name:'minecraft:peony',Properties:{half:'upper'}}",
			"{Name:'minecraft:double_plant',Properties:{facing:'east',half:'upper',variant:'double_fern'}}",
			"{Name:'minecraft:double_plant',Properties:{facing:'east',half:'upper',variant:'double_grass'}}",
			"{Name:'minecraft:double_plant',Properties:{facing:'east',half:'upper',variant:'double_rose'}}",
			"{Name:'minecraft:double_plant',Properties:{facing:'east',half:'upper',variant:'paeonia'}}",
			"{Name:'minecraft:double_plant',Properties:{facing:'east',half:'upper',variant:'sunflower'}}",
			"{Name:'minecraft:double_plant',Properties:{facing:'east',half:'upper',variant:'syringa'}}"
		);
	}

	/**
	 * Adds states to flatten from the blocks which had numeric IDs between 176 and 191 before 1.13.
	 */
	static void putStatesFromBlocks176To191() {
		putStates(2816, "{Name:'minecraft:white_banner',Properties:{rotation:'0'}}", "{Name:'minecraft:standing_banner',Properties:{rotation:'0'}}");
		putStates(2817, "{Name:'minecraft:white_banner',Properties:{rotation:'1'}}", "{Name:'minecraft:standing_banner',Properties:{rotation:'1'}}");
		putStates(2818, "{Name:'minecraft:white_banner',Properties:{rotation:'2'}}", "{Name:'minecraft:standing_banner',Properties:{rotation:'2'}}");
		putStates(2819, "{Name:'minecraft:white_banner',Properties:{rotation:'3'}}", "{Name:'minecraft:standing_banner',Properties:{rotation:'3'}}");
		putStates(2820, "{Name:'minecraft:white_banner',Properties:{rotation:'4'}}", "{Name:'minecraft:standing_banner',Properties:{rotation:'4'}}");
		putStates(2821, "{Name:'minecraft:white_banner',Properties:{rotation:'5'}}", "{Name:'minecraft:standing_banner',Properties:{rotation:'5'}}");
		putStates(2822, "{Name:'minecraft:white_banner',Properties:{rotation:'6'}}", "{Name:'minecraft:standing_banner',Properties:{rotation:'6'}}");
		putStates(2823, "{Name:'minecraft:white_banner',Properties:{rotation:'7'}}", "{Name:'minecraft:standing_banner',Properties:{rotation:'7'}}");
		putStates(2824, "{Name:'minecraft:white_banner',Properties:{rotation:'8'}}", "{Name:'minecraft:standing_banner',Properties:{rotation:'8'}}");
		putStates(2825, "{Name:'minecraft:white_banner',Properties:{rotation:'9'}}", "{Name:'minecraft:standing_banner',Properties:{rotation:'9'}}");
		putStates(2826, "{Name:'minecraft:white_banner',Properties:{rotation:'10'}}", "{Name:'minecraft:standing_banner',Properties:{rotation:'10'}}");
		putStates(2827, "{Name:'minecraft:white_banner',Properties:{rotation:'11'}}", "{Name:'minecraft:standing_banner',Properties:{rotation:'11'}}");
		putStates(2828, "{Name:'minecraft:white_banner',Properties:{rotation:'12'}}", "{Name:'minecraft:standing_banner',Properties:{rotation:'12'}}");
		putStates(2829, "{Name:'minecraft:white_banner',Properties:{rotation:'13'}}", "{Name:'minecraft:standing_banner',Properties:{rotation:'13'}}");
		putStates(2830, "{Name:'minecraft:white_banner',Properties:{rotation:'14'}}", "{Name:'minecraft:standing_banner',Properties:{rotation:'14'}}");
		putStates(2831, "{Name:'minecraft:white_banner',Properties:{rotation:'15'}}", "{Name:'minecraft:standing_banner',Properties:{rotation:'15'}}");
		putStates(2834, "{Name:'minecraft:white_wall_banner',Properties:{facing:'north'}}", "{Name:'minecraft:wall_banner',Properties:{facing:'north'}}");
		putStates(2835, "{Name:'minecraft:white_wall_banner',Properties:{facing:'south'}}", "{Name:'minecraft:wall_banner',Properties:{facing:'south'}}");
		putStates(2836, "{Name:'minecraft:white_wall_banner',Properties:{facing:'west'}}", "{Name:'minecraft:wall_banner',Properties:{facing:'west'}}");
		putStates(2837, "{Name:'minecraft:white_wall_banner',Properties:{facing:'east'}}", "{Name:'minecraft:wall_banner',Properties:{facing:'east'}}");
		putStates(
			2848, "{Name:'minecraft:daylight_detector',Properties:{inverted:'true',power:'0'}}", "{Name:'minecraft:daylight_detector_inverted',Properties:{power:'0'}}"
		);
		putStates(
			2849, "{Name:'minecraft:daylight_detector',Properties:{inverted:'true',power:'1'}}", "{Name:'minecraft:daylight_detector_inverted',Properties:{power:'1'}}"
		);
		putStates(
			2850, "{Name:'minecraft:daylight_detector',Properties:{inverted:'true',power:'2'}}", "{Name:'minecraft:daylight_detector_inverted',Properties:{power:'2'}}"
		);
		putStates(
			2851, "{Name:'minecraft:daylight_detector',Properties:{inverted:'true',power:'3'}}", "{Name:'minecraft:daylight_detector_inverted',Properties:{power:'3'}}"
		);
		putStates(
			2852, "{Name:'minecraft:daylight_detector',Properties:{inverted:'true',power:'4'}}", "{Name:'minecraft:daylight_detector_inverted',Properties:{power:'4'}}"
		);
		putStates(
			2853, "{Name:'minecraft:daylight_detector',Properties:{inverted:'true',power:'5'}}", "{Name:'minecraft:daylight_detector_inverted',Properties:{power:'5'}}"
		);
		putStates(
			2854, "{Name:'minecraft:daylight_detector',Properties:{inverted:'true',power:'6'}}", "{Name:'minecraft:daylight_detector_inverted',Properties:{power:'6'}}"
		);
		putStates(
			2855, "{Name:'minecraft:daylight_detector',Properties:{inverted:'true',power:'7'}}", "{Name:'minecraft:daylight_detector_inverted',Properties:{power:'7'}}"
		);
		putStates(
			2856, "{Name:'minecraft:daylight_detector',Properties:{inverted:'true',power:'8'}}", "{Name:'minecraft:daylight_detector_inverted',Properties:{power:'8'}}"
		);
		putStates(
			2857, "{Name:'minecraft:daylight_detector',Properties:{inverted:'true',power:'9'}}", "{Name:'minecraft:daylight_detector_inverted',Properties:{power:'9'}}"
		);
		putStates(
			2858,
			"{Name:'minecraft:daylight_detector',Properties:{inverted:'true',power:'10'}}",
			"{Name:'minecraft:daylight_detector_inverted',Properties:{power:'10'}}"
		);
		putStates(
			2859,
			"{Name:'minecraft:daylight_detector',Properties:{inverted:'true',power:'11'}}",
			"{Name:'minecraft:daylight_detector_inverted',Properties:{power:'11'}}"
		);
		putStates(
			2860,
			"{Name:'minecraft:daylight_detector',Properties:{inverted:'true',power:'12'}}",
			"{Name:'minecraft:daylight_detector_inverted',Properties:{power:'12'}}"
		);
		putStates(
			2861,
			"{Name:'minecraft:daylight_detector',Properties:{inverted:'true',power:'13'}}",
			"{Name:'minecraft:daylight_detector_inverted',Properties:{power:'13'}}"
		);
		putStates(
			2862,
			"{Name:'minecraft:daylight_detector',Properties:{inverted:'true',power:'14'}}",
			"{Name:'minecraft:daylight_detector_inverted',Properties:{power:'14'}}"
		);
		putStates(
			2863,
			"{Name:'minecraft:daylight_detector',Properties:{inverted:'true',power:'15'}}",
			"{Name:'minecraft:daylight_detector_inverted',Properties:{power:'15'}}"
		);
		putStates(2864, "{Name:'minecraft:red_sandstone'}", "{Name:'minecraft:red_sandstone',Properties:{type:'red_sandstone'}}");
		putStates(2865, "{Name:'minecraft:chiseled_red_sandstone'}", "{Name:'minecraft:red_sandstone',Properties:{type:'chiseled_red_sandstone'}}");
		putStates(2866, "{Name:'minecraft:cut_red_sandstone'}", "{Name:'minecraft:red_sandstone',Properties:{type:'smooth_red_sandstone'}}");
		putStates(
			2880,
			"{Name:'minecraft:red_sandstone_stairs',Properties:{facing:'east',half:'bottom',shape:'straight'}}",
			"{Name:'minecraft:red_sandstone_stairs',Properties:{facing:'east',half:'bottom',shape:'inner_left'}}",
			"{Name:'minecraft:red_sandstone_stairs',Properties:{facing:'east',half:'bottom',shape:'inner_right'}}",
			"{Name:'minecraft:red_sandstone_stairs',Properties:{facing:'east',half:'bottom',shape:'outer_left'}}",
			"{Name:'minecraft:red_sandstone_stairs',Properties:{facing:'east',half:'bottom',shape:'outer_right'}}",
			"{Name:'minecraft:red_sandstone_stairs',Properties:{facing:'east',half:'bottom',shape:'straight'}}"
		);
		putStates(
			2881,
			"{Name:'minecraft:red_sandstone_stairs',Properties:{facing:'west',half:'bottom',shape:'straight'}}",
			"{Name:'minecraft:red_sandstone_stairs',Properties:{facing:'west',half:'bottom',shape:'inner_left'}}",
			"{Name:'minecraft:red_sandstone_stairs',Properties:{facing:'west',half:'bottom',shape:'inner_right'}}",
			"{Name:'minecraft:red_sandstone_stairs',Properties:{facing:'west',half:'bottom',shape:'outer_left'}}",
			"{Name:'minecraft:red_sandstone_stairs',Properties:{facing:'west',half:'bottom',shape:'outer_right'}}",
			"{Name:'minecraft:red_sandstone_stairs',Properties:{facing:'west',half:'bottom',shape:'straight'}}"
		);
		putStates(
			2882,
			"{Name:'minecraft:red_sandstone_stairs',Properties:{facing:'south',half:'bottom',shape:'straight'}}",
			"{Name:'minecraft:red_sandstone_stairs',Properties:{facing:'south',half:'bottom',shape:'inner_left'}}",
			"{Name:'minecraft:red_sandstone_stairs',Properties:{facing:'south',half:'bottom',shape:'inner_right'}}",
			"{Name:'minecraft:red_sandstone_stairs',Properties:{facing:'south',half:'bottom',shape:'outer_left'}}",
			"{Name:'minecraft:red_sandstone_stairs',Properties:{facing:'south',half:'bottom',shape:'outer_right'}}",
			"{Name:'minecraft:red_sandstone_stairs',Properties:{facing:'south',half:'bottom',shape:'straight'}}"
		);
		putStates(
			2883,
			"{Name:'minecraft:red_sandstone_stairs',Properties:{facing:'north',half:'bottom',shape:'straight'}}",
			"{Name:'minecraft:red_sandstone_stairs',Properties:{facing:'north',half:'bottom',shape:'inner_left'}}",
			"{Name:'minecraft:red_sandstone_stairs',Properties:{facing:'north',half:'bottom',shape:'inner_right'}}",
			"{Name:'minecraft:red_sandstone_stairs',Properties:{facing:'north',half:'bottom',shape:'outer_left'}}",
			"{Name:'minecraft:red_sandstone_stairs',Properties:{facing:'north',half:'bottom',shape:'outer_right'}}",
			"{Name:'minecraft:red_sandstone_stairs',Properties:{facing:'north',half:'bottom',shape:'straight'}}"
		);
		putStates(
			2884,
			"{Name:'minecraft:red_sandstone_stairs',Properties:{facing:'east',half:'top',shape:'straight'}}",
			"{Name:'minecraft:red_sandstone_stairs',Properties:{facing:'east',half:'top',shape:'inner_left'}}",
			"{Name:'minecraft:red_sandstone_stairs',Properties:{facing:'east',half:'top',shape:'inner_right'}}",
			"{Name:'minecraft:red_sandstone_stairs',Properties:{facing:'east',half:'top',shape:'outer_left'}}",
			"{Name:'minecraft:red_sandstone_stairs',Properties:{facing:'east',half:'top',shape:'outer_right'}}",
			"{Name:'minecraft:red_sandstone_stairs',Properties:{facing:'east',half:'top',shape:'straight'}}"
		);
		putStates(
			2885,
			"{Name:'minecraft:red_sandstone_stairs',Properties:{facing:'west',half:'top',shape:'straight'}}",
			"{Name:'minecraft:red_sandstone_stairs',Properties:{facing:'west',half:'top',shape:'inner_left'}}",
			"{Name:'minecraft:red_sandstone_stairs',Properties:{facing:'west',half:'top',shape:'inner_right'}}",
			"{Name:'minecraft:red_sandstone_stairs',Properties:{facing:'west',half:'top',shape:'outer_left'}}",
			"{Name:'minecraft:red_sandstone_stairs',Properties:{facing:'west',half:'top',shape:'outer_right'}}",
			"{Name:'minecraft:red_sandstone_stairs',Properties:{facing:'west',half:'top',shape:'straight'}}"
		);
		putStates(
			2886,
			"{Name:'minecraft:red_sandstone_stairs',Properties:{facing:'south',half:'top',shape:'straight'}}",
			"{Name:'minecraft:red_sandstone_stairs',Properties:{facing:'south',half:'top',shape:'inner_left'}}",
			"{Name:'minecraft:red_sandstone_stairs',Properties:{facing:'south',half:'top',shape:'inner_right'}}",
			"{Name:'minecraft:red_sandstone_stairs',Properties:{facing:'south',half:'top',shape:'outer_left'}}",
			"{Name:'minecraft:red_sandstone_stairs',Properties:{facing:'south',half:'top',shape:'outer_right'}}",
			"{Name:'minecraft:red_sandstone_stairs',Properties:{facing:'south',half:'top',shape:'straight'}}"
		);
		putStates(
			2887,
			"{Name:'minecraft:red_sandstone_stairs',Properties:{facing:'north',half:'top',shape:'straight'}}",
			"{Name:'minecraft:red_sandstone_stairs',Properties:{facing:'north',half:'top',shape:'inner_left'}}",
			"{Name:'minecraft:red_sandstone_stairs',Properties:{facing:'north',half:'top',shape:'inner_right'}}",
			"{Name:'minecraft:red_sandstone_stairs',Properties:{facing:'north',half:'top',shape:'outer_left'}}",
			"{Name:'minecraft:red_sandstone_stairs',Properties:{facing:'north',half:'top',shape:'outer_right'}}",
			"{Name:'minecraft:red_sandstone_stairs',Properties:{facing:'north',half:'top',shape:'straight'}}"
		);
		putStates(
			2896,
			"{Name:'minecraft:red_sandstone_slab',Properties:{type:'double'}}",
			"{Name:'minecraft:double_stone_slab2',Properties:{seamless:'false',variant:'red_sandstone'}}"
		);
		putStates(2904, "{Name:'minecraft:smooth_red_sandstone'}", "{Name:'minecraft:double_stone_slab2',Properties:{seamless:'true',variant:'red_sandstone'}}");
		putStates(
			2912,
			"{Name:'minecraft:red_sandstone_slab',Properties:{type:'bottom'}}",
			"{Name:'minecraft:stone_slab2',Properties:{half:'bottom',variant:'red_sandstone'}}"
		);
		putStates(
			2920, "{Name:'minecraft:red_sandstone_slab',Properties:{type:'top'}}", "{Name:'minecraft:stone_slab2',Properties:{half:'top',variant:'red_sandstone'}}"
		);
		putStates(
			2928,
			"{Name:'minecraft:spruce_fence_gate',Properties:{facing:'south',in_wall:'false',open:'false',powered:'false'}}",
			"{Name:'minecraft:spruce_fence_gate',Properties:{facing:'south',in_wall:'false',open:'false',powered:'false'}}",
			"{Name:'minecraft:spruce_fence_gate',Properties:{facing:'south',in_wall:'true',open:'false',powered:'false'}}"
		);
		putStates(
			2929,
			"{Name:'minecraft:spruce_fence_gate',Properties:{facing:'west',in_wall:'false',open:'false',powered:'false'}}",
			"{Name:'minecraft:spruce_fence_gate',Properties:{facing:'west',in_wall:'false',open:'false',powered:'false'}}",
			"{Name:'minecraft:spruce_fence_gate',Properties:{facing:'west',in_wall:'true',open:'false',powered:'false'}}"
		);
		putStates(
			2930,
			"{Name:'minecraft:spruce_fence_gate',Properties:{facing:'north',in_wall:'false',open:'false',powered:'false'}}",
			"{Name:'minecraft:spruce_fence_gate',Properties:{facing:'north',in_wall:'false',open:'false',powered:'false'}}",
			"{Name:'minecraft:spruce_fence_gate',Properties:{facing:'north',in_wall:'true',open:'false',powered:'false'}}"
		);
		putStates(
			2931,
			"{Name:'minecraft:spruce_fence_gate',Properties:{facing:'east',in_wall:'false',open:'false',powered:'false'}}",
			"{Name:'minecraft:spruce_fence_gate',Properties:{facing:'east',in_wall:'false',open:'false',powered:'false'}}",
			"{Name:'minecraft:spruce_fence_gate',Properties:{facing:'east',in_wall:'true',open:'false',powered:'false'}}"
		);
		putStates(
			2932,
			"{Name:'minecraft:spruce_fence_gate',Properties:{facing:'south',in_wall:'false',open:'true',powered:'false'}}",
			"{Name:'minecraft:spruce_fence_gate',Properties:{facing:'south',in_wall:'false',open:'true',powered:'false'}}",
			"{Name:'minecraft:spruce_fence_gate',Properties:{facing:'south',in_wall:'true',open:'true',powered:'false'}}"
		);
		putStates(
			2933,
			"{Name:'minecraft:spruce_fence_gate',Properties:{facing:'west',in_wall:'false',open:'true',powered:'false'}}",
			"{Name:'minecraft:spruce_fence_gate',Properties:{facing:'west',in_wall:'false',open:'true',powered:'false'}}",
			"{Name:'minecraft:spruce_fence_gate',Properties:{facing:'west',in_wall:'true',open:'true',powered:'false'}}"
		);
		putStates(
			2934,
			"{Name:'minecraft:spruce_fence_gate',Properties:{facing:'north',in_wall:'false',open:'true',powered:'false'}}",
			"{Name:'minecraft:spruce_fence_gate',Properties:{facing:'north',in_wall:'false',open:'true',powered:'false'}}",
			"{Name:'minecraft:spruce_fence_gate',Properties:{facing:'north',in_wall:'true',open:'true',powered:'false'}}"
		);
		putStates(
			2935,
			"{Name:'minecraft:spruce_fence_gate',Properties:{facing:'east',in_wall:'false',open:'true',powered:'false'}}",
			"{Name:'minecraft:spruce_fence_gate',Properties:{facing:'east',in_wall:'false',open:'true',powered:'false'}}",
			"{Name:'minecraft:spruce_fence_gate',Properties:{facing:'east',in_wall:'true',open:'true',powered:'false'}}"
		);
		putStates(
			2936,
			"{Name:'minecraft:spruce_fence_gate',Properties:{facing:'south',in_wall:'false',open:'false',powered:'true'}}",
			"{Name:'minecraft:spruce_fence_gate',Properties:{facing:'south',in_wall:'false',open:'false',powered:'true'}}",
			"{Name:'minecraft:spruce_fence_gate',Properties:{facing:'south',in_wall:'true',open:'false',powered:'true'}}"
		);
		putStates(
			2937,
			"{Name:'minecraft:spruce_fence_gate',Properties:{facing:'west',in_wall:'false',open:'false',powered:'true'}}",
			"{Name:'minecraft:spruce_fence_gate',Properties:{facing:'west',in_wall:'false',open:'false',powered:'true'}}",
			"{Name:'minecraft:spruce_fence_gate',Properties:{facing:'west',in_wall:'true',open:'false',powered:'true'}}"
		);
		putStates(
			2938,
			"{Name:'minecraft:spruce_fence_gate',Properties:{facing:'north',in_wall:'false',open:'false',powered:'true'}}",
			"{Name:'minecraft:spruce_fence_gate',Properties:{facing:'north',in_wall:'false',open:'false',powered:'true'}}",
			"{Name:'minecraft:spruce_fence_gate',Properties:{facing:'north',in_wall:'true',open:'false',powered:'true'}}"
		);
		putStates(
			2939,
			"{Name:'minecraft:spruce_fence_gate',Properties:{facing:'east',in_wall:'false',open:'false',powered:'true'}}",
			"{Name:'minecraft:spruce_fence_gate',Properties:{facing:'east',in_wall:'false',open:'false',powered:'true'}}",
			"{Name:'minecraft:spruce_fence_gate',Properties:{facing:'east',in_wall:'true',open:'false',powered:'true'}}"
		);
		putStates(
			2940,
			"{Name:'minecraft:spruce_fence_gate',Properties:{facing:'south',in_wall:'false',open:'true',powered:'true'}}",
			"{Name:'minecraft:spruce_fence_gate',Properties:{facing:'south',in_wall:'false',open:'true',powered:'true'}}",
			"{Name:'minecraft:spruce_fence_gate',Properties:{facing:'south',in_wall:'true',open:'true',powered:'true'}}"
		);
		putStates(
			2941,
			"{Name:'minecraft:spruce_fence_gate',Properties:{facing:'west',in_wall:'false',open:'true',powered:'true'}}",
			"{Name:'minecraft:spruce_fence_gate',Properties:{facing:'west',in_wall:'false',open:'true',powered:'true'}}",
			"{Name:'minecraft:spruce_fence_gate',Properties:{facing:'west',in_wall:'true',open:'true',powered:'true'}}"
		);
		putStates(
			2942,
			"{Name:'minecraft:spruce_fence_gate',Properties:{facing:'north',in_wall:'false',open:'true',powered:'true'}}",
			"{Name:'minecraft:spruce_fence_gate',Properties:{facing:'north',in_wall:'false',open:'true',powered:'true'}}",
			"{Name:'minecraft:spruce_fence_gate',Properties:{facing:'north',in_wall:'true',open:'true',powered:'true'}}"
		);
		putStates(
			2943,
			"{Name:'minecraft:spruce_fence_gate',Properties:{facing:'east',in_wall:'false',open:'true',powered:'true'}}",
			"{Name:'minecraft:spruce_fence_gate',Properties:{facing:'east',in_wall:'false',open:'true',powered:'true'}}",
			"{Name:'minecraft:spruce_fence_gate',Properties:{facing:'east',in_wall:'true',open:'true',powered:'true'}}"
		);
		putStates(
			2944,
			"{Name:'minecraft:birch_fence_gate',Properties:{facing:'south',in_wall:'false',open:'false',powered:'false'}}",
			"{Name:'minecraft:birch_fence_gate',Properties:{facing:'south',in_wall:'false',open:'false',powered:'false'}}",
			"{Name:'minecraft:birch_fence_gate',Properties:{facing:'south',in_wall:'true',open:'false',powered:'false'}}"
		);
		putStates(
			2945,
			"{Name:'minecraft:birch_fence_gate',Properties:{facing:'west',in_wall:'false',open:'false',powered:'false'}}",
			"{Name:'minecraft:birch_fence_gate',Properties:{facing:'west',in_wall:'false',open:'false',powered:'false'}}",
			"{Name:'minecraft:birch_fence_gate',Properties:{facing:'west',in_wall:'true',open:'false',powered:'false'}}"
		);
		putStates(
			2946,
			"{Name:'minecraft:birch_fence_gate',Properties:{facing:'north',in_wall:'false',open:'false',powered:'false'}}",
			"{Name:'minecraft:birch_fence_gate',Properties:{facing:'north',in_wall:'false',open:'false',powered:'false'}}",
			"{Name:'minecraft:birch_fence_gate',Properties:{facing:'north',in_wall:'true',open:'false',powered:'false'}}"
		);
		putStates(
			2947,
			"{Name:'minecraft:birch_fence_gate',Properties:{facing:'east',in_wall:'false',open:'false',powered:'false'}}",
			"{Name:'minecraft:birch_fence_gate',Properties:{facing:'east',in_wall:'false',open:'false',powered:'false'}}",
			"{Name:'minecraft:birch_fence_gate',Properties:{facing:'east',in_wall:'true',open:'false',powered:'false'}}"
		);
		putStates(
			2948,
			"{Name:'minecraft:birch_fence_gate',Properties:{facing:'south',in_wall:'false',open:'true',powered:'false'}}",
			"{Name:'minecraft:birch_fence_gate',Properties:{facing:'south',in_wall:'false',open:'true',powered:'false'}}",
			"{Name:'minecraft:birch_fence_gate',Properties:{facing:'south',in_wall:'true',open:'true',powered:'false'}}"
		);
		putStates(
			2949,
			"{Name:'minecraft:birch_fence_gate',Properties:{facing:'west',in_wall:'false',open:'true',powered:'false'}}",
			"{Name:'minecraft:birch_fence_gate',Properties:{facing:'west',in_wall:'false',open:'true',powered:'false'}}",
			"{Name:'minecraft:birch_fence_gate',Properties:{facing:'west',in_wall:'true',open:'true',powered:'false'}}"
		);
		putStates(
			2950,
			"{Name:'minecraft:birch_fence_gate',Properties:{facing:'north',in_wall:'false',open:'true',powered:'false'}}",
			"{Name:'minecraft:birch_fence_gate',Properties:{facing:'north',in_wall:'false',open:'true',powered:'false'}}",
			"{Name:'minecraft:birch_fence_gate',Properties:{facing:'north',in_wall:'true',open:'true',powered:'false'}}"
		);
		putStates(
			2951,
			"{Name:'minecraft:birch_fence_gate',Properties:{facing:'east',in_wall:'false',open:'true',powered:'false'}}",
			"{Name:'minecraft:birch_fence_gate',Properties:{facing:'east',in_wall:'false',open:'true',powered:'false'}}",
			"{Name:'minecraft:birch_fence_gate',Properties:{facing:'east',in_wall:'true',open:'true',powered:'false'}}"
		);
		putStates(
			2952,
			"{Name:'minecraft:birch_fence_gate',Properties:{facing:'south',in_wall:'false',open:'false',powered:'true'}}",
			"{Name:'minecraft:birch_fence_gate',Properties:{facing:'south',in_wall:'false',open:'false',powered:'true'}}",
			"{Name:'minecraft:birch_fence_gate',Properties:{facing:'south',in_wall:'true',open:'false',powered:'true'}}"
		);
		putStates(
			2953,
			"{Name:'minecraft:birch_fence_gate',Properties:{facing:'west',in_wall:'false',open:'false',powered:'true'}}",
			"{Name:'minecraft:birch_fence_gate',Properties:{facing:'west',in_wall:'false',open:'false',powered:'true'}}",
			"{Name:'minecraft:birch_fence_gate',Properties:{facing:'west',in_wall:'true',open:'false',powered:'true'}}"
		);
		putStates(
			2954,
			"{Name:'minecraft:birch_fence_gate',Properties:{facing:'north',in_wall:'false',open:'false',powered:'true'}}",
			"{Name:'minecraft:birch_fence_gate',Properties:{facing:'north',in_wall:'false',open:'false',powered:'true'}}",
			"{Name:'minecraft:birch_fence_gate',Properties:{facing:'north',in_wall:'true',open:'false',powered:'true'}}"
		);
		putStates(
			2955,
			"{Name:'minecraft:birch_fence_gate',Properties:{facing:'east',in_wall:'false',open:'false',powered:'true'}}",
			"{Name:'minecraft:birch_fence_gate',Properties:{facing:'east',in_wall:'false',open:'false',powered:'true'}}",
			"{Name:'minecraft:birch_fence_gate',Properties:{facing:'east',in_wall:'true',open:'false',powered:'true'}}"
		);
		putStates(
			2956,
			"{Name:'minecraft:birch_fence_gate',Properties:{facing:'south',in_wall:'false',open:'true',powered:'true'}}",
			"{Name:'minecraft:birch_fence_gate',Properties:{facing:'south',in_wall:'false',open:'true',powered:'true'}}",
			"{Name:'minecraft:birch_fence_gate',Properties:{facing:'south',in_wall:'true',open:'true',powered:'true'}}"
		);
		putStates(
			2957,
			"{Name:'minecraft:birch_fence_gate',Properties:{facing:'west',in_wall:'false',open:'true',powered:'true'}}",
			"{Name:'minecraft:birch_fence_gate',Properties:{facing:'west',in_wall:'false',open:'true',powered:'true'}}",
			"{Name:'minecraft:birch_fence_gate',Properties:{facing:'west',in_wall:'true',open:'true',powered:'true'}}"
		);
		putStates(
			2958,
			"{Name:'minecraft:birch_fence_gate',Properties:{facing:'north',in_wall:'false',open:'true',powered:'true'}}",
			"{Name:'minecraft:birch_fence_gate',Properties:{facing:'north',in_wall:'false',open:'true',powered:'true'}}",
			"{Name:'minecraft:birch_fence_gate',Properties:{facing:'north',in_wall:'true',open:'true',powered:'true'}}"
		);
		putStates(
			2959,
			"{Name:'minecraft:birch_fence_gate',Properties:{facing:'east',in_wall:'false',open:'true',powered:'true'}}",
			"{Name:'minecraft:birch_fence_gate',Properties:{facing:'east',in_wall:'false',open:'true',powered:'true'}}",
			"{Name:'minecraft:birch_fence_gate',Properties:{facing:'east',in_wall:'true',open:'true',powered:'true'}}"
		);
		putStates(
			2960,
			"{Name:'minecraft:jungle_fence_gate',Properties:{facing:'south',in_wall:'false',open:'false',powered:'false'}}",
			"{Name:'minecraft:jungle_fence_gate',Properties:{facing:'south',in_wall:'false',open:'false',powered:'false'}}",
			"{Name:'minecraft:jungle_fence_gate',Properties:{facing:'south',in_wall:'true',open:'false',powered:'false'}}"
		);
		putStates(
			2961,
			"{Name:'minecraft:jungle_fence_gate',Properties:{facing:'west',in_wall:'false',open:'false',powered:'false'}}",
			"{Name:'minecraft:jungle_fence_gate',Properties:{facing:'west',in_wall:'false',open:'false',powered:'false'}}",
			"{Name:'minecraft:jungle_fence_gate',Properties:{facing:'west',in_wall:'true',open:'false',powered:'false'}}"
		);
		putStates(
			2962,
			"{Name:'minecraft:jungle_fence_gate',Properties:{facing:'north',in_wall:'false',open:'false',powered:'false'}}",
			"{Name:'minecraft:jungle_fence_gate',Properties:{facing:'north',in_wall:'false',open:'false',powered:'false'}}",
			"{Name:'minecraft:jungle_fence_gate',Properties:{facing:'north',in_wall:'true',open:'false',powered:'false'}}"
		);
		putStates(
			2963,
			"{Name:'minecraft:jungle_fence_gate',Properties:{facing:'east',in_wall:'false',open:'false',powered:'false'}}",
			"{Name:'minecraft:jungle_fence_gate',Properties:{facing:'east',in_wall:'false',open:'false',powered:'false'}}",
			"{Name:'minecraft:jungle_fence_gate',Properties:{facing:'east',in_wall:'true',open:'false',powered:'false'}}"
		);
		putStates(
			2964,
			"{Name:'minecraft:jungle_fence_gate',Properties:{facing:'south',in_wall:'false',open:'true',powered:'false'}}",
			"{Name:'minecraft:jungle_fence_gate',Properties:{facing:'south',in_wall:'false',open:'true',powered:'false'}}",
			"{Name:'minecraft:jungle_fence_gate',Properties:{facing:'south',in_wall:'true',open:'true',powered:'false'}}"
		);
		putStates(
			2965,
			"{Name:'minecraft:jungle_fence_gate',Properties:{facing:'west',in_wall:'false',open:'true',powered:'false'}}",
			"{Name:'minecraft:jungle_fence_gate',Properties:{facing:'west',in_wall:'false',open:'true',powered:'false'}}",
			"{Name:'minecraft:jungle_fence_gate',Properties:{facing:'west',in_wall:'true',open:'true',powered:'false'}}"
		);
		putStates(
			2966,
			"{Name:'minecraft:jungle_fence_gate',Properties:{facing:'north',in_wall:'false',open:'true',powered:'false'}}",
			"{Name:'minecraft:jungle_fence_gate',Properties:{facing:'north',in_wall:'false',open:'true',powered:'false'}}",
			"{Name:'minecraft:jungle_fence_gate',Properties:{facing:'north',in_wall:'true',open:'true',powered:'false'}}"
		);
		putStates(
			2967,
			"{Name:'minecraft:jungle_fence_gate',Properties:{facing:'east',in_wall:'false',open:'true',powered:'false'}}",
			"{Name:'minecraft:jungle_fence_gate',Properties:{facing:'east',in_wall:'false',open:'true',powered:'false'}}",
			"{Name:'minecraft:jungle_fence_gate',Properties:{facing:'east',in_wall:'true',open:'true',powered:'false'}}"
		);
		putStates(
			2968,
			"{Name:'minecraft:jungle_fence_gate',Properties:{facing:'south',in_wall:'false',open:'false',powered:'true'}}",
			"{Name:'minecraft:jungle_fence_gate',Properties:{facing:'south',in_wall:'false',open:'false',powered:'true'}}",
			"{Name:'minecraft:jungle_fence_gate',Properties:{facing:'south',in_wall:'true',open:'false',powered:'true'}}"
		);
		putStates(
			2969,
			"{Name:'minecraft:jungle_fence_gate',Properties:{facing:'west',in_wall:'false',open:'false',powered:'true'}}",
			"{Name:'minecraft:jungle_fence_gate',Properties:{facing:'west',in_wall:'false',open:'false',powered:'true'}}",
			"{Name:'minecraft:jungle_fence_gate',Properties:{facing:'west',in_wall:'true',open:'false',powered:'true'}}"
		);
		putStates(
			2970,
			"{Name:'minecraft:jungle_fence_gate',Properties:{facing:'north',in_wall:'false',open:'false',powered:'true'}}",
			"{Name:'minecraft:jungle_fence_gate',Properties:{facing:'north',in_wall:'false',open:'false',powered:'true'}}",
			"{Name:'minecraft:jungle_fence_gate',Properties:{facing:'north',in_wall:'true',open:'false',powered:'true'}}"
		);
		putStates(
			2971,
			"{Name:'minecraft:jungle_fence_gate',Properties:{facing:'east',in_wall:'false',open:'false',powered:'true'}}",
			"{Name:'minecraft:jungle_fence_gate',Properties:{facing:'east',in_wall:'false',open:'false',powered:'true'}}",
			"{Name:'minecraft:jungle_fence_gate',Properties:{facing:'east',in_wall:'true',open:'false',powered:'true'}}"
		);
		putStates(
			2972,
			"{Name:'minecraft:jungle_fence_gate',Properties:{facing:'south',in_wall:'false',open:'true',powered:'true'}}",
			"{Name:'minecraft:jungle_fence_gate',Properties:{facing:'south',in_wall:'false',open:'true',powered:'true'}}",
			"{Name:'minecraft:jungle_fence_gate',Properties:{facing:'south',in_wall:'true',open:'true',powered:'true'}}"
		);
		putStates(
			2973,
			"{Name:'minecraft:jungle_fence_gate',Properties:{facing:'west',in_wall:'false',open:'true',powered:'true'}}",
			"{Name:'minecraft:jungle_fence_gate',Properties:{facing:'west',in_wall:'false',open:'true',powered:'true'}}",
			"{Name:'minecraft:jungle_fence_gate',Properties:{facing:'west',in_wall:'true',open:'true',powered:'true'}}"
		);
		putStates(
			2974,
			"{Name:'minecraft:jungle_fence_gate',Properties:{facing:'north',in_wall:'false',open:'true',powered:'true'}}",
			"{Name:'minecraft:jungle_fence_gate',Properties:{facing:'north',in_wall:'false',open:'true',powered:'true'}}",
			"{Name:'minecraft:jungle_fence_gate',Properties:{facing:'north',in_wall:'true',open:'true',powered:'true'}}"
		);
		putStates(
			2975,
			"{Name:'minecraft:jungle_fence_gate',Properties:{facing:'east',in_wall:'false',open:'true',powered:'true'}}",
			"{Name:'minecraft:jungle_fence_gate',Properties:{facing:'east',in_wall:'false',open:'true',powered:'true'}}",
			"{Name:'minecraft:jungle_fence_gate',Properties:{facing:'east',in_wall:'true',open:'true',powered:'true'}}"
		);
		putStates(
			2976,
			"{Name:'minecraft:dark_oak_fence_gate',Properties:{facing:'south',in_wall:'false',open:'false',powered:'false'}}",
			"{Name:'minecraft:dark_oak_fence_gate',Properties:{facing:'south',in_wall:'false',open:'false',powered:'false'}}",
			"{Name:'minecraft:dark_oak_fence_gate',Properties:{facing:'south',in_wall:'true',open:'false',powered:'false'}}"
		);
		putStates(
			2977,
			"{Name:'minecraft:dark_oak_fence_gate',Properties:{facing:'west',in_wall:'false',open:'false',powered:'false'}}",
			"{Name:'minecraft:dark_oak_fence_gate',Properties:{facing:'west',in_wall:'false',open:'false',powered:'false'}}",
			"{Name:'minecraft:dark_oak_fence_gate',Properties:{facing:'west',in_wall:'true',open:'false',powered:'false'}}"
		);
		putStates(
			2978,
			"{Name:'minecraft:dark_oak_fence_gate',Properties:{facing:'north',in_wall:'false',open:'false',powered:'false'}}",
			"{Name:'minecraft:dark_oak_fence_gate',Properties:{facing:'north',in_wall:'false',open:'false',powered:'false'}}",
			"{Name:'minecraft:dark_oak_fence_gate',Properties:{facing:'north',in_wall:'true',open:'false',powered:'false'}}"
		);
		putStates(
			2979,
			"{Name:'minecraft:dark_oak_fence_gate',Properties:{facing:'east',in_wall:'false',open:'false',powered:'false'}}",
			"{Name:'minecraft:dark_oak_fence_gate',Properties:{facing:'east',in_wall:'false',open:'false',powered:'false'}}",
			"{Name:'minecraft:dark_oak_fence_gate',Properties:{facing:'east',in_wall:'true',open:'false',powered:'false'}}"
		);
		putStates(
			2980,
			"{Name:'minecraft:dark_oak_fence_gate',Properties:{facing:'south',in_wall:'false',open:'true',powered:'false'}}",
			"{Name:'minecraft:dark_oak_fence_gate',Properties:{facing:'south',in_wall:'false',open:'true',powered:'false'}}",
			"{Name:'minecraft:dark_oak_fence_gate',Properties:{facing:'south',in_wall:'true',open:'true',powered:'false'}}"
		);
		putStates(
			2981,
			"{Name:'minecraft:dark_oak_fence_gate',Properties:{facing:'west',in_wall:'false',open:'true',powered:'false'}}",
			"{Name:'minecraft:dark_oak_fence_gate',Properties:{facing:'west',in_wall:'false',open:'true',powered:'false'}}",
			"{Name:'minecraft:dark_oak_fence_gate',Properties:{facing:'west',in_wall:'true',open:'true',powered:'false'}}"
		);
		putStates(
			2982,
			"{Name:'minecraft:dark_oak_fence_gate',Properties:{facing:'north',in_wall:'false',open:'true',powered:'false'}}",
			"{Name:'minecraft:dark_oak_fence_gate',Properties:{facing:'north',in_wall:'false',open:'true',powered:'false'}}",
			"{Name:'minecraft:dark_oak_fence_gate',Properties:{facing:'north',in_wall:'true',open:'true',powered:'false'}}"
		);
		putStates(
			2983,
			"{Name:'minecraft:dark_oak_fence_gate',Properties:{facing:'east',in_wall:'false',open:'true',powered:'false'}}",
			"{Name:'minecraft:dark_oak_fence_gate',Properties:{facing:'east',in_wall:'false',open:'true',powered:'false'}}",
			"{Name:'minecraft:dark_oak_fence_gate',Properties:{facing:'east',in_wall:'true',open:'true',powered:'false'}}"
		);
		putStates(
			2984,
			"{Name:'minecraft:dark_oak_fence_gate',Properties:{facing:'south',in_wall:'false',open:'false',powered:'true'}}",
			"{Name:'minecraft:dark_oak_fence_gate',Properties:{facing:'south',in_wall:'false',open:'false',powered:'true'}}",
			"{Name:'minecraft:dark_oak_fence_gate',Properties:{facing:'south',in_wall:'true',open:'false',powered:'true'}}"
		);
		putStates(
			2985,
			"{Name:'minecraft:dark_oak_fence_gate',Properties:{facing:'west',in_wall:'false',open:'false',powered:'true'}}",
			"{Name:'minecraft:dark_oak_fence_gate',Properties:{facing:'west',in_wall:'false',open:'false',powered:'true'}}",
			"{Name:'minecraft:dark_oak_fence_gate',Properties:{facing:'west',in_wall:'true',open:'false',powered:'true'}}"
		);
		putStates(
			2986,
			"{Name:'minecraft:dark_oak_fence_gate',Properties:{facing:'north',in_wall:'false',open:'false',powered:'true'}}",
			"{Name:'minecraft:dark_oak_fence_gate',Properties:{facing:'north',in_wall:'false',open:'false',powered:'true'}}",
			"{Name:'minecraft:dark_oak_fence_gate',Properties:{facing:'north',in_wall:'true',open:'false',powered:'true'}}"
		);
		putStates(
			2987,
			"{Name:'minecraft:dark_oak_fence_gate',Properties:{facing:'east',in_wall:'false',open:'false',powered:'true'}}",
			"{Name:'minecraft:dark_oak_fence_gate',Properties:{facing:'east',in_wall:'false',open:'false',powered:'true'}}",
			"{Name:'minecraft:dark_oak_fence_gate',Properties:{facing:'east',in_wall:'true',open:'false',powered:'true'}}"
		);
		putStates(
			2988,
			"{Name:'minecraft:dark_oak_fence_gate',Properties:{facing:'south',in_wall:'false',open:'true',powered:'true'}}",
			"{Name:'minecraft:dark_oak_fence_gate',Properties:{facing:'south',in_wall:'false',open:'true',powered:'true'}}",
			"{Name:'minecraft:dark_oak_fence_gate',Properties:{facing:'south',in_wall:'true',open:'true',powered:'true'}}"
		);
		putStates(
			2989,
			"{Name:'minecraft:dark_oak_fence_gate',Properties:{facing:'west',in_wall:'false',open:'true',powered:'true'}}",
			"{Name:'minecraft:dark_oak_fence_gate',Properties:{facing:'west',in_wall:'false',open:'true',powered:'true'}}",
			"{Name:'minecraft:dark_oak_fence_gate',Properties:{facing:'west',in_wall:'true',open:'true',powered:'true'}}"
		);
		putStates(
			2990,
			"{Name:'minecraft:dark_oak_fence_gate',Properties:{facing:'north',in_wall:'false',open:'true',powered:'true'}}",
			"{Name:'minecraft:dark_oak_fence_gate',Properties:{facing:'north',in_wall:'false',open:'true',powered:'true'}}",
			"{Name:'minecraft:dark_oak_fence_gate',Properties:{facing:'north',in_wall:'true',open:'true',powered:'true'}}"
		);
		putStates(
			2991,
			"{Name:'minecraft:dark_oak_fence_gate',Properties:{facing:'east',in_wall:'false',open:'true',powered:'true'}}",
			"{Name:'minecraft:dark_oak_fence_gate',Properties:{facing:'east',in_wall:'false',open:'true',powered:'true'}}",
			"{Name:'minecraft:dark_oak_fence_gate',Properties:{facing:'east',in_wall:'true',open:'true',powered:'true'}}"
		);
		putStates(
			2992,
			"{Name:'minecraft:acacia_fence_gate',Properties:{facing:'south',in_wall:'false',open:'false',powered:'false'}}",
			"{Name:'minecraft:acacia_fence_gate',Properties:{facing:'south',in_wall:'false',open:'false',powered:'false'}}",
			"{Name:'minecraft:acacia_fence_gate',Properties:{facing:'south',in_wall:'true',open:'false',powered:'false'}}"
		);
		putStates(
			2993,
			"{Name:'minecraft:acacia_fence_gate',Properties:{facing:'west',in_wall:'false',open:'false',powered:'false'}}",
			"{Name:'minecraft:acacia_fence_gate',Properties:{facing:'west',in_wall:'false',open:'false',powered:'false'}}",
			"{Name:'minecraft:acacia_fence_gate',Properties:{facing:'west',in_wall:'true',open:'false',powered:'false'}}"
		);
		putStates(
			2994,
			"{Name:'minecraft:acacia_fence_gate',Properties:{facing:'north',in_wall:'false',open:'false',powered:'false'}}",
			"{Name:'minecraft:acacia_fence_gate',Properties:{facing:'north',in_wall:'false',open:'false',powered:'false'}}",
			"{Name:'minecraft:acacia_fence_gate',Properties:{facing:'north',in_wall:'true',open:'false',powered:'false'}}"
		);
		putStates(
			2995,
			"{Name:'minecraft:acacia_fence_gate',Properties:{facing:'east',in_wall:'false',open:'false',powered:'false'}}",
			"{Name:'minecraft:acacia_fence_gate',Properties:{facing:'east',in_wall:'false',open:'false',powered:'false'}}",
			"{Name:'minecraft:acacia_fence_gate',Properties:{facing:'east',in_wall:'true',open:'false',powered:'false'}}"
		);
		putStates(
			2996,
			"{Name:'minecraft:acacia_fence_gate',Properties:{facing:'south',in_wall:'false',open:'true',powered:'false'}}",
			"{Name:'minecraft:acacia_fence_gate',Properties:{facing:'south',in_wall:'false',open:'true',powered:'false'}}",
			"{Name:'minecraft:acacia_fence_gate',Properties:{facing:'south',in_wall:'true',open:'true',powered:'false'}}"
		);
		putStates(
			2997,
			"{Name:'minecraft:acacia_fence_gate',Properties:{facing:'west',in_wall:'false',open:'true',powered:'false'}}",
			"{Name:'minecraft:acacia_fence_gate',Properties:{facing:'west',in_wall:'false',open:'true',powered:'false'}}",
			"{Name:'minecraft:acacia_fence_gate',Properties:{facing:'west',in_wall:'true',open:'true',powered:'false'}}"
		);
		putStates(
			2998,
			"{Name:'minecraft:acacia_fence_gate',Properties:{facing:'north',in_wall:'false',open:'true',powered:'false'}}",
			"{Name:'minecraft:acacia_fence_gate',Properties:{facing:'north',in_wall:'false',open:'true',powered:'false'}}",
			"{Name:'minecraft:acacia_fence_gate',Properties:{facing:'north',in_wall:'true',open:'true',powered:'false'}}"
		);
		putStates(
			2999,
			"{Name:'minecraft:acacia_fence_gate',Properties:{facing:'east',in_wall:'false',open:'true',powered:'false'}}",
			"{Name:'minecraft:acacia_fence_gate',Properties:{facing:'east',in_wall:'false',open:'true',powered:'false'}}",
			"{Name:'minecraft:acacia_fence_gate',Properties:{facing:'east',in_wall:'true',open:'true',powered:'false'}}"
		);
		putStates(
			3000,
			"{Name:'minecraft:acacia_fence_gate',Properties:{facing:'south',in_wall:'false',open:'false',powered:'true'}}",
			"{Name:'minecraft:acacia_fence_gate',Properties:{facing:'south',in_wall:'false',open:'false',powered:'true'}}",
			"{Name:'minecraft:acacia_fence_gate',Properties:{facing:'south',in_wall:'true',open:'false',powered:'true'}}"
		);
		putStates(
			3001,
			"{Name:'minecraft:acacia_fence_gate',Properties:{facing:'west',in_wall:'false',open:'false',powered:'true'}}",
			"{Name:'minecraft:acacia_fence_gate',Properties:{facing:'west',in_wall:'false',open:'false',powered:'true'}}",
			"{Name:'minecraft:acacia_fence_gate',Properties:{facing:'west',in_wall:'true',open:'false',powered:'true'}}"
		);
		putStates(
			3002,
			"{Name:'minecraft:acacia_fence_gate',Properties:{facing:'north',in_wall:'false',open:'false',powered:'true'}}",
			"{Name:'minecraft:acacia_fence_gate',Properties:{facing:'north',in_wall:'false',open:'false',powered:'true'}}",
			"{Name:'minecraft:acacia_fence_gate',Properties:{facing:'north',in_wall:'true',open:'false',powered:'true'}}"
		);
		putStates(
			3003,
			"{Name:'minecraft:acacia_fence_gate',Properties:{facing:'east',in_wall:'false',open:'false',powered:'true'}}",
			"{Name:'minecraft:acacia_fence_gate',Properties:{facing:'east',in_wall:'false',open:'false',powered:'true'}}",
			"{Name:'minecraft:acacia_fence_gate',Properties:{facing:'east',in_wall:'true',open:'false',powered:'true'}}"
		);
		putStates(
			3004,
			"{Name:'minecraft:acacia_fence_gate',Properties:{facing:'south',in_wall:'false',open:'true',powered:'true'}}",
			"{Name:'minecraft:acacia_fence_gate',Properties:{facing:'south',in_wall:'false',open:'true',powered:'true'}}",
			"{Name:'minecraft:acacia_fence_gate',Properties:{facing:'south',in_wall:'true',open:'true',powered:'true'}}"
		);
		putStates(
			3005,
			"{Name:'minecraft:acacia_fence_gate',Properties:{facing:'west',in_wall:'false',open:'true',powered:'true'}}",
			"{Name:'minecraft:acacia_fence_gate',Properties:{facing:'west',in_wall:'false',open:'true',powered:'true'}}",
			"{Name:'minecraft:acacia_fence_gate',Properties:{facing:'west',in_wall:'true',open:'true',powered:'true'}}"
		);
		putStates(
			3006,
			"{Name:'minecraft:acacia_fence_gate',Properties:{facing:'north',in_wall:'false',open:'true',powered:'true'}}",
			"{Name:'minecraft:acacia_fence_gate',Properties:{facing:'north',in_wall:'false',open:'true',powered:'true'}}",
			"{Name:'minecraft:acacia_fence_gate',Properties:{facing:'north',in_wall:'true',open:'true',powered:'true'}}"
		);
		putStates(
			3007,
			"{Name:'minecraft:acacia_fence_gate',Properties:{facing:'east',in_wall:'false',open:'true',powered:'true'}}",
			"{Name:'minecraft:acacia_fence_gate',Properties:{facing:'east',in_wall:'false',open:'true',powered:'true'}}",
			"{Name:'minecraft:acacia_fence_gate',Properties:{facing:'east',in_wall:'true',open:'true',powered:'true'}}"
		);
		putStates(
			3008,
			"{Name:'minecraft:spruce_fence',Properties:{east:'false',north:'false',south:'false',west:'false'}}",
			"{Name:'minecraft:spruce_fence',Properties:{east:'false',north:'false',south:'false',west:'false'}}",
			"{Name:'minecraft:spruce_fence',Properties:{east:'false',north:'false',south:'false',west:'true'}}",
			"{Name:'minecraft:spruce_fence',Properties:{east:'false',north:'false',south:'true',west:'false'}}",
			"{Name:'minecraft:spruce_fence',Properties:{east:'false',north:'false',south:'true',west:'true'}}",
			"{Name:'minecraft:spruce_fence',Properties:{east:'false',north:'true',south:'false',west:'false'}}",
			"{Name:'minecraft:spruce_fence',Properties:{east:'false',north:'true',south:'false',west:'true'}}",
			"{Name:'minecraft:spruce_fence',Properties:{east:'false',north:'true',south:'true',west:'false'}}",
			"{Name:'minecraft:spruce_fence',Properties:{east:'false',north:'true',south:'true',west:'true'}}",
			"{Name:'minecraft:spruce_fence',Properties:{east:'true',north:'false',south:'false',west:'false'}}",
			"{Name:'minecraft:spruce_fence',Properties:{east:'true',north:'false',south:'false',west:'true'}}",
			"{Name:'minecraft:spruce_fence',Properties:{east:'true',north:'false',south:'true',west:'false'}}",
			"{Name:'minecraft:spruce_fence',Properties:{east:'true',north:'false',south:'true',west:'true'}}",
			"{Name:'minecraft:spruce_fence',Properties:{east:'true',north:'true',south:'false',west:'false'}}",
			"{Name:'minecraft:spruce_fence',Properties:{east:'true',north:'true',south:'false',west:'true'}}",
			"{Name:'minecraft:spruce_fence',Properties:{east:'true',north:'true',south:'true',west:'false'}}",
			"{Name:'minecraft:spruce_fence',Properties:{east:'true',north:'true',south:'true',west:'true'}}"
		);
		putStates(
			3024,
			"{Name:'minecraft:birch_fence',Properties:{east:'false',north:'false',south:'false',west:'false'}}",
			"{Name:'minecraft:birch_fence',Properties:{east:'false',north:'false',south:'false',west:'false'}}",
			"{Name:'minecraft:birch_fence',Properties:{east:'false',north:'false',south:'false',west:'true'}}",
			"{Name:'minecraft:birch_fence',Properties:{east:'false',north:'false',south:'true',west:'false'}}",
			"{Name:'minecraft:birch_fence',Properties:{east:'false',north:'false',south:'true',west:'true'}}",
			"{Name:'minecraft:birch_fence',Properties:{east:'false',north:'true',south:'false',west:'false'}}",
			"{Name:'minecraft:birch_fence',Properties:{east:'false',north:'true',south:'false',west:'true'}}",
			"{Name:'minecraft:birch_fence',Properties:{east:'false',north:'true',south:'true',west:'false'}}",
			"{Name:'minecraft:birch_fence',Properties:{east:'false',north:'true',south:'true',west:'true'}}",
			"{Name:'minecraft:birch_fence',Properties:{east:'true',north:'false',south:'false',west:'false'}}",
			"{Name:'minecraft:birch_fence',Properties:{east:'true',north:'false',south:'false',west:'true'}}",
			"{Name:'minecraft:birch_fence',Properties:{east:'true',north:'false',south:'true',west:'false'}}",
			"{Name:'minecraft:birch_fence',Properties:{east:'true',north:'false',south:'true',west:'true'}}",
			"{Name:'minecraft:birch_fence',Properties:{east:'true',north:'true',south:'false',west:'false'}}",
			"{Name:'minecraft:birch_fence',Properties:{east:'true',north:'true',south:'false',west:'true'}}",
			"{Name:'minecraft:birch_fence',Properties:{east:'true',north:'true',south:'true',west:'false'}}",
			"{Name:'minecraft:birch_fence',Properties:{east:'true',north:'true',south:'true',west:'true'}}"
		);
		putStates(
			3040,
			"{Name:'minecraft:jungle_fence',Properties:{east:'false',north:'false',south:'false',west:'false'}}",
			"{Name:'minecraft:jungle_fence',Properties:{east:'false',north:'false',south:'false',west:'false'}}",
			"{Name:'minecraft:jungle_fence',Properties:{east:'false',north:'false',south:'false',west:'true'}}",
			"{Name:'minecraft:jungle_fence',Properties:{east:'false',north:'false',south:'true',west:'false'}}",
			"{Name:'minecraft:jungle_fence',Properties:{east:'false',north:'false',south:'true',west:'true'}}",
			"{Name:'minecraft:jungle_fence',Properties:{east:'false',north:'true',south:'false',west:'false'}}",
			"{Name:'minecraft:jungle_fence',Properties:{east:'false',north:'true',south:'false',west:'true'}}",
			"{Name:'minecraft:jungle_fence',Properties:{east:'false',north:'true',south:'true',west:'false'}}",
			"{Name:'minecraft:jungle_fence',Properties:{east:'false',north:'true',south:'true',west:'true'}}",
			"{Name:'minecraft:jungle_fence',Properties:{east:'true',north:'false',south:'false',west:'false'}}",
			"{Name:'minecraft:jungle_fence',Properties:{east:'true',north:'false',south:'false',west:'true'}}",
			"{Name:'minecraft:jungle_fence',Properties:{east:'true',north:'false',south:'true',west:'false'}}",
			"{Name:'minecraft:jungle_fence',Properties:{east:'true',north:'false',south:'true',west:'true'}}",
			"{Name:'minecraft:jungle_fence',Properties:{east:'true',north:'true',south:'false',west:'false'}}",
			"{Name:'minecraft:jungle_fence',Properties:{east:'true',north:'true',south:'false',west:'true'}}",
			"{Name:'minecraft:jungle_fence',Properties:{east:'true',north:'true',south:'true',west:'false'}}",
			"{Name:'minecraft:jungle_fence',Properties:{east:'true',north:'true',south:'true',west:'true'}}"
		);
		putStates(
			3056,
			"{Name:'minecraft:dark_oak_fence',Properties:{east:'false',north:'false',south:'false',west:'false'}}",
			"{Name:'minecraft:dark_oak_fence',Properties:{east:'false',north:'false',south:'false',west:'false'}}",
			"{Name:'minecraft:dark_oak_fence',Properties:{east:'false',north:'false',south:'false',west:'true'}}",
			"{Name:'minecraft:dark_oak_fence',Properties:{east:'false',north:'false',south:'true',west:'false'}}",
			"{Name:'minecraft:dark_oak_fence',Properties:{east:'false',north:'false',south:'true',west:'true'}}",
			"{Name:'minecraft:dark_oak_fence',Properties:{east:'false',north:'true',south:'false',west:'false'}}",
			"{Name:'minecraft:dark_oak_fence',Properties:{east:'false',north:'true',south:'false',west:'true'}}",
			"{Name:'minecraft:dark_oak_fence',Properties:{east:'false',north:'true',south:'true',west:'false'}}",
			"{Name:'minecraft:dark_oak_fence',Properties:{east:'false',north:'true',south:'true',west:'true'}}",
			"{Name:'minecraft:dark_oak_fence',Properties:{east:'true',north:'false',south:'false',west:'false'}}",
			"{Name:'minecraft:dark_oak_fence',Properties:{east:'true',north:'false',south:'false',west:'true'}}",
			"{Name:'minecraft:dark_oak_fence',Properties:{east:'true',north:'false',south:'true',west:'false'}}",
			"{Name:'minecraft:dark_oak_fence',Properties:{east:'true',north:'false',south:'true',west:'true'}}",
			"{Name:'minecraft:dark_oak_fence',Properties:{east:'true',north:'true',south:'false',west:'false'}}",
			"{Name:'minecraft:dark_oak_fence',Properties:{east:'true',north:'true',south:'false',west:'true'}}",
			"{Name:'minecraft:dark_oak_fence',Properties:{east:'true',north:'true',south:'true',west:'false'}}",
			"{Name:'minecraft:dark_oak_fence',Properties:{east:'true',north:'true',south:'true',west:'true'}}"
		);
	}

	/**
	 * Adds states to flatten from the blocks which had numeric IDs between 192 and 207 before 1.13.
	 */
	static void putStatesFromBlocks192To207() {
		putStates(
			3072,
			"{Name:'minecraft:acacia_fence',Properties:{east:'false',north:'false',south:'false',west:'false'}}",
			"{Name:'minecraft:acacia_fence',Properties:{east:'false',north:'false',south:'false',west:'false'}}",
			"{Name:'minecraft:acacia_fence',Properties:{east:'false',north:'false',south:'false',west:'true'}}",
			"{Name:'minecraft:acacia_fence',Properties:{east:'false',north:'false',south:'true',west:'false'}}",
			"{Name:'minecraft:acacia_fence',Properties:{east:'false',north:'false',south:'true',west:'true'}}",
			"{Name:'minecraft:acacia_fence',Properties:{east:'false',north:'true',south:'false',west:'false'}}",
			"{Name:'minecraft:acacia_fence',Properties:{east:'false',north:'true',south:'false',west:'true'}}",
			"{Name:'minecraft:acacia_fence',Properties:{east:'false',north:'true',south:'true',west:'false'}}",
			"{Name:'minecraft:acacia_fence',Properties:{east:'false',north:'true',south:'true',west:'true'}}",
			"{Name:'minecraft:acacia_fence',Properties:{east:'true',north:'false',south:'false',west:'false'}}",
			"{Name:'minecraft:acacia_fence',Properties:{east:'true',north:'false',south:'false',west:'true'}}",
			"{Name:'minecraft:acacia_fence',Properties:{east:'true',north:'false',south:'true',west:'false'}}",
			"{Name:'minecraft:acacia_fence',Properties:{east:'true',north:'false',south:'true',west:'true'}}",
			"{Name:'minecraft:acacia_fence',Properties:{east:'true',north:'true',south:'false',west:'false'}}",
			"{Name:'minecraft:acacia_fence',Properties:{east:'true',north:'true',south:'false',west:'true'}}",
			"{Name:'minecraft:acacia_fence',Properties:{east:'true',north:'true',south:'true',west:'false'}}",
			"{Name:'minecraft:acacia_fence',Properties:{east:'true',north:'true',south:'true',west:'true'}}"
		);
		putStates(
			3088,
			"{Name:'minecraft:spruce_door',Properties:{facing:'east',half:'lower',hinge:'right',open:'false',powered:'false'}}",
			"{Name:'minecraft:spruce_door',Properties:{facing:'east',half:'lower',hinge:'left',open:'false',powered:'false'}}",
			"{Name:'minecraft:spruce_door',Properties:{facing:'east',half:'lower',hinge:'left',open:'false',powered:'true'}}",
			"{Name:'minecraft:spruce_door',Properties:{facing:'east',half:'lower',hinge:'right',open:'false',powered:'false'}}",
			"{Name:'minecraft:spruce_door',Properties:{facing:'east',half:'lower',hinge:'right',open:'false',powered:'true'}}"
		);
		putStates(
			3089,
			"{Name:'minecraft:spruce_door',Properties:{facing:'south',half:'lower',hinge:'right',open:'false',powered:'false'}}",
			"{Name:'minecraft:spruce_door',Properties:{facing:'south',half:'lower',hinge:'left',open:'false',powered:'false'}}",
			"{Name:'minecraft:spruce_door',Properties:{facing:'south',half:'lower',hinge:'left',open:'false',powered:'true'}}",
			"{Name:'minecraft:spruce_door',Properties:{facing:'south',half:'lower',hinge:'right',open:'false',powered:'false'}}",
			"{Name:'minecraft:spruce_door',Properties:{facing:'south',half:'lower',hinge:'right',open:'false',powered:'true'}}"
		);
		putStates(
			3090,
			"{Name:'minecraft:spruce_door',Properties:{facing:'west',half:'lower',hinge:'right',open:'false',powered:'false'}}",
			"{Name:'minecraft:spruce_door',Properties:{facing:'west',half:'lower',hinge:'left',open:'false',powered:'false'}}",
			"{Name:'minecraft:spruce_door',Properties:{facing:'west',half:'lower',hinge:'left',open:'false',powered:'true'}}",
			"{Name:'minecraft:spruce_door',Properties:{facing:'west',half:'lower',hinge:'right',open:'false',powered:'false'}}",
			"{Name:'minecraft:spruce_door',Properties:{facing:'west',half:'lower',hinge:'right',open:'false',powered:'true'}}"
		);
		putStates(
			3091,
			"{Name:'minecraft:spruce_door',Properties:{facing:'north',half:'lower',hinge:'right',open:'false',powered:'false'}}",
			"{Name:'minecraft:spruce_door',Properties:{facing:'north',half:'lower',hinge:'left',open:'false',powered:'false'}}",
			"{Name:'minecraft:spruce_door',Properties:{facing:'north',half:'lower',hinge:'left',open:'false',powered:'true'}}",
			"{Name:'minecraft:spruce_door',Properties:{facing:'north',half:'lower',hinge:'right',open:'false',powered:'false'}}",
			"{Name:'minecraft:spruce_door',Properties:{facing:'north',half:'lower',hinge:'right',open:'false',powered:'true'}}"
		);
		putStates(
			3092,
			"{Name:'minecraft:spruce_door',Properties:{facing:'east',half:'lower',hinge:'right',open:'true',powered:'false'}}",
			"{Name:'minecraft:spruce_door',Properties:{facing:'east',half:'lower',hinge:'left',open:'true',powered:'false'}}",
			"{Name:'minecraft:spruce_door',Properties:{facing:'east',half:'lower',hinge:'left',open:'true',powered:'true'}}",
			"{Name:'minecraft:spruce_door',Properties:{facing:'east',half:'lower',hinge:'right',open:'true',powered:'false'}}",
			"{Name:'minecraft:spruce_door',Properties:{facing:'east',half:'lower',hinge:'right',open:'true',powered:'true'}}"
		);
		putStates(
			3093,
			"{Name:'minecraft:spruce_door',Properties:{facing:'south',half:'lower',hinge:'right',open:'true',powered:'false'}}",
			"{Name:'minecraft:spruce_door',Properties:{facing:'south',half:'lower',hinge:'left',open:'true',powered:'false'}}",
			"{Name:'minecraft:spruce_door',Properties:{facing:'south',half:'lower',hinge:'left',open:'true',powered:'true'}}",
			"{Name:'minecraft:spruce_door',Properties:{facing:'south',half:'lower',hinge:'right',open:'true',powered:'false'}}",
			"{Name:'minecraft:spruce_door',Properties:{facing:'south',half:'lower',hinge:'right',open:'true',powered:'true'}}"
		);
		putStates(
			3094,
			"{Name:'minecraft:spruce_door',Properties:{facing:'west',half:'lower',hinge:'right',open:'true',powered:'false'}}",
			"{Name:'minecraft:spruce_door',Properties:{facing:'west',half:'lower',hinge:'left',open:'true',powered:'false'}}",
			"{Name:'minecraft:spruce_door',Properties:{facing:'west',half:'lower',hinge:'left',open:'true',powered:'true'}}",
			"{Name:'minecraft:spruce_door',Properties:{facing:'west',half:'lower',hinge:'right',open:'true',powered:'false'}}",
			"{Name:'minecraft:spruce_door',Properties:{facing:'west',half:'lower',hinge:'right',open:'true',powered:'true'}}"
		);
		putStates(
			3095,
			"{Name:'minecraft:spruce_door',Properties:{facing:'north',half:'lower',hinge:'right',open:'true',powered:'false'}}",
			"{Name:'minecraft:spruce_door',Properties:{facing:'north',half:'lower',hinge:'left',open:'true',powered:'false'}}",
			"{Name:'minecraft:spruce_door',Properties:{facing:'north',half:'lower',hinge:'left',open:'true',powered:'true'}}",
			"{Name:'minecraft:spruce_door',Properties:{facing:'north',half:'lower',hinge:'right',open:'true',powered:'false'}}",
			"{Name:'minecraft:spruce_door',Properties:{facing:'north',half:'lower',hinge:'right',open:'true',powered:'true'}}"
		);
		putStates(
			3096,
			"{Name:'minecraft:spruce_door',Properties:{facing:'east',half:'upper',hinge:'left',open:'false',powered:'false'}}",
			"{Name:'minecraft:spruce_door',Properties:{facing:'east',half:'upper',hinge:'left',open:'false',powered:'false'}}",
			"{Name:'minecraft:spruce_door',Properties:{facing:'east',half:'upper',hinge:'left',open:'true',powered:'false'}}",
			"{Name:'minecraft:spruce_door',Properties:{facing:'north',half:'upper',hinge:'left',open:'false',powered:'false'}}",
			"{Name:'minecraft:spruce_door',Properties:{facing:'north',half:'upper',hinge:'left',open:'true',powered:'false'}}",
			"{Name:'minecraft:spruce_door',Properties:{facing:'south',half:'upper',hinge:'left',open:'false',powered:'false'}}",
			"{Name:'minecraft:spruce_door',Properties:{facing:'south',half:'upper',hinge:'left',open:'true',powered:'false'}}",
			"{Name:'minecraft:spruce_door',Properties:{facing:'west',half:'upper',hinge:'left',open:'false',powered:'false'}}",
			"{Name:'minecraft:spruce_door',Properties:{facing:'west',half:'upper',hinge:'left',open:'true',powered:'false'}}"
		);
		putStates(
			3097,
			"{Name:'minecraft:spruce_door',Properties:{facing:'east',half:'upper',hinge:'right',open:'false',powered:'false'}}",
			"{Name:'minecraft:spruce_door',Properties:{facing:'east',half:'upper',hinge:'right',open:'false',powered:'false'}}",
			"{Name:'minecraft:spruce_door',Properties:{facing:'east',half:'upper',hinge:'right',open:'true',powered:'false'}}",
			"{Name:'minecraft:spruce_door',Properties:{facing:'north',half:'upper',hinge:'right',open:'false',powered:'false'}}",
			"{Name:'minecraft:spruce_door',Properties:{facing:'north',half:'upper',hinge:'right',open:'true',powered:'false'}}",
			"{Name:'minecraft:spruce_door',Properties:{facing:'south',half:'upper',hinge:'right',open:'false',powered:'false'}}",
			"{Name:'minecraft:spruce_door',Properties:{facing:'south',half:'upper',hinge:'right',open:'true',powered:'false'}}",
			"{Name:'minecraft:spruce_door',Properties:{facing:'west',half:'upper',hinge:'right',open:'false',powered:'false'}}",
			"{Name:'minecraft:spruce_door',Properties:{facing:'west',half:'upper',hinge:'right',open:'true',powered:'false'}}"
		);
		putStates(
			3098,
			"{Name:'minecraft:spruce_door',Properties:{facing:'east',half:'upper',hinge:'left',open:'false',powered:'true'}}",
			"{Name:'minecraft:spruce_door',Properties:{facing:'east',half:'upper',hinge:'left',open:'false',powered:'true'}}",
			"{Name:'minecraft:spruce_door',Properties:{facing:'east',half:'upper',hinge:'left',open:'true',powered:'true'}}",
			"{Name:'minecraft:spruce_door',Properties:{facing:'north',half:'upper',hinge:'left',open:'false',powered:'true'}}",
			"{Name:'minecraft:spruce_door',Properties:{facing:'north',half:'upper',hinge:'left',open:'true',powered:'true'}}",
			"{Name:'minecraft:spruce_door',Properties:{facing:'south',half:'upper',hinge:'left',open:'false',powered:'true'}}",
			"{Name:'minecraft:spruce_door',Properties:{facing:'south',half:'upper',hinge:'left',open:'true',powered:'true'}}",
			"{Name:'minecraft:spruce_door',Properties:{facing:'west',half:'upper',hinge:'left',open:'false',powered:'true'}}",
			"{Name:'minecraft:spruce_door',Properties:{facing:'west',half:'upper',hinge:'left',open:'true',powered:'true'}}"
		);
		putStates(
			3099,
			"{Name:'minecraft:spruce_door',Properties:{facing:'east',half:'upper',hinge:'right',open:'false',powered:'true'}}",
			"{Name:'minecraft:spruce_door',Properties:{facing:'east',half:'upper',hinge:'right',open:'false',powered:'true'}}",
			"{Name:'minecraft:spruce_door',Properties:{facing:'east',half:'upper',hinge:'right',open:'true',powered:'true'}}",
			"{Name:'minecraft:spruce_door',Properties:{facing:'north',half:'upper',hinge:'right',open:'false',powered:'true'}}",
			"{Name:'minecraft:spruce_door',Properties:{facing:'north',half:'upper',hinge:'right',open:'true',powered:'true'}}",
			"{Name:'minecraft:spruce_door',Properties:{facing:'south',half:'upper',hinge:'right',open:'false',powered:'true'}}",
			"{Name:'minecraft:spruce_door',Properties:{facing:'south',half:'upper',hinge:'right',open:'true',powered:'true'}}",
			"{Name:'minecraft:spruce_door',Properties:{facing:'west',half:'upper',hinge:'right',open:'false',powered:'true'}}",
			"{Name:'minecraft:spruce_door',Properties:{facing:'west',half:'upper',hinge:'right',open:'true',powered:'true'}}"
		);
		putStates(
			3104,
			"{Name:'minecraft:birch_door',Properties:{facing:'east',half:'lower',hinge:'right',open:'false',powered:'false'}}",
			"{Name:'minecraft:birch_door',Properties:{facing:'east',half:'lower',hinge:'left',open:'false',powered:'false'}}",
			"{Name:'minecraft:birch_door',Properties:{facing:'east',half:'lower',hinge:'left',open:'false',powered:'true'}}",
			"{Name:'minecraft:birch_door',Properties:{facing:'east',half:'lower',hinge:'right',open:'false',powered:'false'}}",
			"{Name:'minecraft:birch_door',Properties:{facing:'east',half:'lower',hinge:'right',open:'false',powered:'true'}}"
		);
		putStates(
			3105,
			"{Name:'minecraft:birch_door',Properties:{facing:'south',half:'lower',hinge:'right',open:'false',powered:'false'}}",
			"{Name:'minecraft:birch_door',Properties:{facing:'south',half:'lower',hinge:'left',open:'false',powered:'false'}}",
			"{Name:'minecraft:birch_door',Properties:{facing:'south',half:'lower',hinge:'left',open:'false',powered:'true'}}",
			"{Name:'minecraft:birch_door',Properties:{facing:'south',half:'lower',hinge:'right',open:'false',powered:'false'}}",
			"{Name:'minecraft:birch_door',Properties:{facing:'south',half:'lower',hinge:'right',open:'false',powered:'true'}}"
		);
		putStates(
			3106,
			"{Name:'minecraft:birch_door',Properties:{facing:'west',half:'lower',hinge:'right',open:'false',powered:'false'}}",
			"{Name:'minecraft:birch_door',Properties:{facing:'west',half:'lower',hinge:'left',open:'false',powered:'false'}}",
			"{Name:'minecraft:birch_door',Properties:{facing:'west',half:'lower',hinge:'left',open:'false',powered:'true'}}",
			"{Name:'minecraft:birch_door',Properties:{facing:'west',half:'lower',hinge:'right',open:'false',powered:'false'}}",
			"{Name:'minecraft:birch_door',Properties:{facing:'west',half:'lower',hinge:'right',open:'false',powered:'true'}}"
		);
		putStates(
			3107,
			"{Name:'minecraft:birch_door',Properties:{facing:'north',half:'lower',hinge:'right',open:'false',powered:'false'}}",
			"{Name:'minecraft:birch_door',Properties:{facing:'north',half:'lower',hinge:'left',open:'false',powered:'false'}}",
			"{Name:'minecraft:birch_door',Properties:{facing:'north',half:'lower',hinge:'left',open:'false',powered:'true'}}",
			"{Name:'minecraft:birch_door',Properties:{facing:'north',half:'lower',hinge:'right',open:'false',powered:'false'}}",
			"{Name:'minecraft:birch_door',Properties:{facing:'north',half:'lower',hinge:'right',open:'false',powered:'true'}}"
		);
		putStates(
			3108,
			"{Name:'minecraft:birch_door',Properties:{facing:'east',half:'lower',hinge:'right',open:'true',powered:'false'}}",
			"{Name:'minecraft:birch_door',Properties:{facing:'east',half:'lower',hinge:'left',open:'true',powered:'false'}}",
			"{Name:'minecraft:birch_door',Properties:{facing:'east',half:'lower',hinge:'left',open:'true',powered:'true'}}",
			"{Name:'minecraft:birch_door',Properties:{facing:'east',half:'lower',hinge:'right',open:'true',powered:'false'}}",
			"{Name:'minecraft:birch_door',Properties:{facing:'east',half:'lower',hinge:'right',open:'true',powered:'true'}}"
		);
		putStates(
			3109,
			"{Name:'minecraft:birch_door',Properties:{facing:'south',half:'lower',hinge:'right',open:'true',powered:'false'}}",
			"{Name:'minecraft:birch_door',Properties:{facing:'south',half:'lower',hinge:'left',open:'true',powered:'false'}}",
			"{Name:'minecraft:birch_door',Properties:{facing:'south',half:'lower',hinge:'left',open:'true',powered:'true'}}",
			"{Name:'minecraft:birch_door',Properties:{facing:'south',half:'lower',hinge:'right',open:'true',powered:'false'}}",
			"{Name:'minecraft:birch_door',Properties:{facing:'south',half:'lower',hinge:'right',open:'true',powered:'true'}}"
		);
		putStates(
			3110,
			"{Name:'minecraft:birch_door',Properties:{facing:'west',half:'lower',hinge:'right',open:'true',powered:'false'}}",
			"{Name:'minecraft:birch_door',Properties:{facing:'west',half:'lower',hinge:'left',open:'true',powered:'false'}}",
			"{Name:'minecraft:birch_door',Properties:{facing:'west',half:'lower',hinge:'left',open:'true',powered:'true'}}",
			"{Name:'minecraft:birch_door',Properties:{facing:'west',half:'lower',hinge:'right',open:'true',powered:'false'}}",
			"{Name:'minecraft:birch_door',Properties:{facing:'west',half:'lower',hinge:'right',open:'true',powered:'true'}}"
		);
		putStates(
			3111,
			"{Name:'minecraft:birch_door',Properties:{facing:'north',half:'lower',hinge:'right',open:'true',powered:'false'}}",
			"{Name:'minecraft:birch_door',Properties:{facing:'north',half:'lower',hinge:'left',open:'true',powered:'false'}}",
			"{Name:'minecraft:birch_door',Properties:{facing:'north',half:'lower',hinge:'left',open:'true',powered:'true'}}",
			"{Name:'minecraft:birch_door',Properties:{facing:'north',half:'lower',hinge:'right',open:'true',powered:'false'}}",
			"{Name:'minecraft:birch_door',Properties:{facing:'north',half:'lower',hinge:'right',open:'true',powered:'true'}}"
		);
		putStates(
			3112,
			"{Name:'minecraft:birch_door',Properties:{facing:'east',half:'upper',hinge:'left',open:'false',powered:'false'}}",
			"{Name:'minecraft:birch_door',Properties:{facing:'east',half:'upper',hinge:'left',open:'false',powered:'false'}}",
			"{Name:'minecraft:birch_door',Properties:{facing:'east',half:'upper',hinge:'left',open:'true',powered:'false'}}",
			"{Name:'minecraft:birch_door',Properties:{facing:'north',half:'upper',hinge:'left',open:'false',powered:'false'}}",
			"{Name:'minecraft:birch_door',Properties:{facing:'north',half:'upper',hinge:'left',open:'true',powered:'false'}}",
			"{Name:'minecraft:birch_door',Properties:{facing:'south',half:'upper',hinge:'left',open:'false',powered:'false'}}",
			"{Name:'minecraft:birch_door',Properties:{facing:'south',half:'upper',hinge:'left',open:'true',powered:'false'}}",
			"{Name:'minecraft:birch_door',Properties:{facing:'west',half:'upper',hinge:'left',open:'false',powered:'false'}}",
			"{Name:'minecraft:birch_door',Properties:{facing:'west',half:'upper',hinge:'left',open:'true',powered:'false'}}"
		);
		putStates(
			3113,
			"{Name:'minecraft:birch_door',Properties:{facing:'east',half:'upper',hinge:'right',open:'false',powered:'false'}}",
			"{Name:'minecraft:birch_door',Properties:{facing:'east',half:'upper',hinge:'right',open:'false',powered:'false'}}",
			"{Name:'minecraft:birch_door',Properties:{facing:'east',half:'upper',hinge:'right',open:'true',powered:'false'}}",
			"{Name:'minecraft:birch_door',Properties:{facing:'north',half:'upper',hinge:'right',open:'false',powered:'false'}}",
			"{Name:'minecraft:birch_door',Properties:{facing:'north',half:'upper',hinge:'right',open:'true',powered:'false'}}",
			"{Name:'minecraft:birch_door',Properties:{facing:'south',half:'upper',hinge:'right',open:'false',powered:'false'}}",
			"{Name:'minecraft:birch_door',Properties:{facing:'south',half:'upper',hinge:'right',open:'true',powered:'false'}}",
			"{Name:'minecraft:birch_door',Properties:{facing:'west',half:'upper',hinge:'right',open:'false',powered:'false'}}",
			"{Name:'minecraft:birch_door',Properties:{facing:'west',half:'upper',hinge:'right',open:'true',powered:'false'}}"
		);
		putStates(
			3114,
			"{Name:'minecraft:birch_door',Properties:{facing:'east',half:'upper',hinge:'left',open:'false',powered:'true'}}",
			"{Name:'minecraft:birch_door',Properties:{facing:'east',half:'upper',hinge:'left',open:'false',powered:'true'}}",
			"{Name:'minecraft:birch_door',Properties:{facing:'east',half:'upper',hinge:'left',open:'true',powered:'true'}}",
			"{Name:'minecraft:birch_door',Properties:{facing:'north',half:'upper',hinge:'left',open:'false',powered:'true'}}",
			"{Name:'minecraft:birch_door',Properties:{facing:'north',half:'upper',hinge:'left',open:'true',powered:'true'}}",
			"{Name:'minecraft:birch_door',Properties:{facing:'south',half:'upper',hinge:'left',open:'false',powered:'true'}}",
			"{Name:'minecraft:birch_door',Properties:{facing:'south',half:'upper',hinge:'left',open:'true',powered:'true'}}",
			"{Name:'minecraft:birch_door',Properties:{facing:'west',half:'upper',hinge:'left',open:'false',powered:'true'}}",
			"{Name:'minecraft:birch_door',Properties:{facing:'west',half:'upper',hinge:'left',open:'true',powered:'true'}}"
		);
		putStates(
			3115,
			"{Name:'minecraft:birch_door',Properties:{facing:'east',half:'upper',hinge:'right',open:'false',powered:'true'}}",
			"{Name:'minecraft:birch_door',Properties:{facing:'east',half:'upper',hinge:'right',open:'false',powered:'true'}}",
			"{Name:'minecraft:birch_door',Properties:{facing:'east',half:'upper',hinge:'right',open:'true',powered:'true'}}",
			"{Name:'minecraft:birch_door',Properties:{facing:'north',half:'upper',hinge:'right',open:'false',powered:'true'}}",
			"{Name:'minecraft:birch_door',Properties:{facing:'north',half:'upper',hinge:'right',open:'true',powered:'true'}}",
			"{Name:'minecraft:birch_door',Properties:{facing:'south',half:'upper',hinge:'right',open:'false',powered:'true'}}",
			"{Name:'minecraft:birch_door',Properties:{facing:'south',half:'upper',hinge:'right',open:'true',powered:'true'}}",
			"{Name:'minecraft:birch_door',Properties:{facing:'west',half:'upper',hinge:'right',open:'false',powered:'true'}}",
			"{Name:'minecraft:birch_door',Properties:{facing:'west',half:'upper',hinge:'right',open:'true',powered:'true'}}"
		);
		putStates(
			3120,
			"{Name:'minecraft:jungle_door',Properties:{facing:'east',half:'lower',hinge:'right',open:'false',powered:'false'}}",
			"{Name:'minecraft:jungle_door',Properties:{facing:'east',half:'lower',hinge:'left',open:'false',powered:'false'}}",
			"{Name:'minecraft:jungle_door',Properties:{facing:'east',half:'lower',hinge:'left',open:'false',powered:'true'}}",
			"{Name:'minecraft:jungle_door',Properties:{facing:'east',half:'lower',hinge:'right',open:'false',powered:'false'}}",
			"{Name:'minecraft:jungle_door',Properties:{facing:'east',half:'lower',hinge:'right',open:'false',powered:'true'}}"
		);
		putStates(
			3121,
			"{Name:'minecraft:jungle_door',Properties:{facing:'south',half:'lower',hinge:'right',open:'false',powered:'false'}}",
			"{Name:'minecraft:jungle_door',Properties:{facing:'south',half:'lower',hinge:'left',open:'false',powered:'false'}}",
			"{Name:'minecraft:jungle_door',Properties:{facing:'south',half:'lower',hinge:'left',open:'false',powered:'true'}}",
			"{Name:'minecraft:jungle_door',Properties:{facing:'south',half:'lower',hinge:'right',open:'false',powered:'false'}}",
			"{Name:'minecraft:jungle_door',Properties:{facing:'south',half:'lower',hinge:'right',open:'false',powered:'true'}}"
		);
		putStates(
			3122,
			"{Name:'minecraft:jungle_door',Properties:{facing:'west',half:'lower',hinge:'right',open:'false',powered:'false'}}",
			"{Name:'minecraft:jungle_door',Properties:{facing:'west',half:'lower',hinge:'left',open:'false',powered:'false'}}",
			"{Name:'minecraft:jungle_door',Properties:{facing:'west',half:'lower',hinge:'left',open:'false',powered:'true'}}",
			"{Name:'minecraft:jungle_door',Properties:{facing:'west',half:'lower',hinge:'right',open:'false',powered:'false'}}",
			"{Name:'minecraft:jungle_door',Properties:{facing:'west',half:'lower',hinge:'right',open:'false',powered:'true'}}"
		);
		putStates(
			3123,
			"{Name:'minecraft:jungle_door',Properties:{facing:'north',half:'lower',hinge:'right',open:'false',powered:'false'}}",
			"{Name:'minecraft:jungle_door',Properties:{facing:'north',half:'lower',hinge:'left',open:'false',powered:'false'}}",
			"{Name:'minecraft:jungle_door',Properties:{facing:'north',half:'lower',hinge:'left',open:'false',powered:'true'}}",
			"{Name:'minecraft:jungle_door',Properties:{facing:'north',half:'lower',hinge:'right',open:'false',powered:'false'}}",
			"{Name:'minecraft:jungle_door',Properties:{facing:'north',half:'lower',hinge:'right',open:'false',powered:'true'}}"
		);
		putStates(
			3124,
			"{Name:'minecraft:jungle_door',Properties:{facing:'east',half:'lower',hinge:'right',open:'true',powered:'false'}}",
			"{Name:'minecraft:jungle_door',Properties:{facing:'east',half:'lower',hinge:'left',open:'true',powered:'false'}}",
			"{Name:'minecraft:jungle_door',Properties:{facing:'east',half:'lower',hinge:'left',open:'true',powered:'true'}}",
			"{Name:'minecraft:jungle_door',Properties:{facing:'east',half:'lower',hinge:'right',open:'true',powered:'false'}}",
			"{Name:'minecraft:jungle_door',Properties:{facing:'east',half:'lower',hinge:'right',open:'true',powered:'true'}}"
		);
		putStates(
			3125,
			"{Name:'minecraft:jungle_door',Properties:{facing:'south',half:'lower',hinge:'right',open:'true',powered:'false'}}",
			"{Name:'minecraft:jungle_door',Properties:{facing:'south',half:'lower',hinge:'left',open:'true',powered:'false'}}",
			"{Name:'minecraft:jungle_door',Properties:{facing:'south',half:'lower',hinge:'left',open:'true',powered:'true'}}",
			"{Name:'minecraft:jungle_door',Properties:{facing:'south',half:'lower',hinge:'right',open:'true',powered:'false'}}",
			"{Name:'minecraft:jungle_door',Properties:{facing:'south',half:'lower',hinge:'right',open:'true',powered:'true'}}"
		);
		putStates(
			3126,
			"{Name:'minecraft:jungle_door',Properties:{facing:'west',half:'lower',hinge:'right',open:'true',powered:'false'}}",
			"{Name:'minecraft:jungle_door',Properties:{facing:'west',half:'lower',hinge:'left',open:'true',powered:'false'}}",
			"{Name:'minecraft:jungle_door',Properties:{facing:'west',half:'lower',hinge:'left',open:'true',powered:'true'}}",
			"{Name:'minecraft:jungle_door',Properties:{facing:'west',half:'lower',hinge:'right',open:'true',powered:'false'}}",
			"{Name:'minecraft:jungle_door',Properties:{facing:'west',half:'lower',hinge:'right',open:'true',powered:'true'}}"
		);
		putStates(
			3127,
			"{Name:'minecraft:jungle_door',Properties:{facing:'north',half:'lower',hinge:'right',open:'true',powered:'false'}}",
			"{Name:'minecraft:jungle_door',Properties:{facing:'north',half:'lower',hinge:'left',open:'true',powered:'false'}}",
			"{Name:'minecraft:jungle_door',Properties:{facing:'north',half:'lower',hinge:'left',open:'true',powered:'true'}}",
			"{Name:'minecraft:jungle_door',Properties:{facing:'north',half:'lower',hinge:'right',open:'true',powered:'false'}}",
			"{Name:'minecraft:jungle_door',Properties:{facing:'north',half:'lower',hinge:'right',open:'true',powered:'true'}}"
		);
		putStates(
			3128,
			"{Name:'minecraft:jungle_door',Properties:{facing:'east',half:'upper',hinge:'left',open:'false',powered:'false'}}",
			"{Name:'minecraft:jungle_door',Properties:{facing:'east',half:'upper',hinge:'left',open:'false',powered:'false'}}",
			"{Name:'minecraft:jungle_door',Properties:{facing:'east',half:'upper',hinge:'left',open:'true',powered:'false'}}",
			"{Name:'minecraft:jungle_door',Properties:{facing:'north',half:'upper',hinge:'left',open:'false',powered:'false'}}",
			"{Name:'minecraft:jungle_door',Properties:{facing:'north',half:'upper',hinge:'left',open:'true',powered:'false'}}",
			"{Name:'minecraft:jungle_door',Properties:{facing:'south',half:'upper',hinge:'left',open:'false',powered:'false'}}",
			"{Name:'minecraft:jungle_door',Properties:{facing:'south',half:'upper',hinge:'left',open:'true',powered:'false'}}",
			"{Name:'minecraft:jungle_door',Properties:{facing:'west',half:'upper',hinge:'left',open:'false',powered:'false'}}",
			"{Name:'minecraft:jungle_door',Properties:{facing:'west',half:'upper',hinge:'left',open:'true',powered:'false'}}"
		);
		putStates(
			3129,
			"{Name:'minecraft:jungle_door',Properties:{facing:'east',half:'upper',hinge:'right',open:'false',powered:'false'}}",
			"{Name:'minecraft:jungle_door',Properties:{facing:'east',half:'upper',hinge:'right',open:'false',powered:'false'}}",
			"{Name:'minecraft:jungle_door',Properties:{facing:'east',half:'upper',hinge:'right',open:'true',powered:'false'}}",
			"{Name:'minecraft:jungle_door',Properties:{facing:'north',half:'upper',hinge:'right',open:'false',powered:'false'}}",
			"{Name:'minecraft:jungle_door',Properties:{facing:'north',half:'upper',hinge:'right',open:'true',powered:'false'}}",
			"{Name:'minecraft:jungle_door',Properties:{facing:'south',half:'upper',hinge:'right',open:'false',powered:'false'}}",
			"{Name:'minecraft:jungle_door',Properties:{facing:'south',half:'upper',hinge:'right',open:'true',powered:'false'}}",
			"{Name:'minecraft:jungle_door',Properties:{facing:'west',half:'upper',hinge:'right',open:'false',powered:'false'}}",
			"{Name:'minecraft:jungle_door',Properties:{facing:'west',half:'upper',hinge:'right',open:'true',powered:'false'}}"
		);
		putStates(
			3130,
			"{Name:'minecraft:jungle_door',Properties:{facing:'east',half:'upper',hinge:'left',open:'false',powered:'true'}}",
			"{Name:'minecraft:jungle_door',Properties:{facing:'east',half:'upper',hinge:'left',open:'false',powered:'true'}}",
			"{Name:'minecraft:jungle_door',Properties:{facing:'east',half:'upper',hinge:'left',open:'true',powered:'true'}}",
			"{Name:'minecraft:jungle_door',Properties:{facing:'north',half:'upper',hinge:'left',open:'false',powered:'true'}}",
			"{Name:'minecraft:jungle_door',Properties:{facing:'north',half:'upper',hinge:'left',open:'true',powered:'true'}}",
			"{Name:'minecraft:jungle_door',Properties:{facing:'south',half:'upper',hinge:'left',open:'false',powered:'true'}}",
			"{Name:'minecraft:jungle_door',Properties:{facing:'south',half:'upper',hinge:'left',open:'true',powered:'true'}}",
			"{Name:'minecraft:jungle_door',Properties:{facing:'west',half:'upper',hinge:'left',open:'false',powered:'true'}}",
			"{Name:'minecraft:jungle_door',Properties:{facing:'west',half:'upper',hinge:'left',open:'true',powered:'true'}}"
		);
		putStates(
			3131,
			"{Name:'minecraft:jungle_door',Properties:{facing:'east',half:'upper',hinge:'right',open:'false',powered:'true'}}",
			"{Name:'minecraft:jungle_door',Properties:{facing:'east',half:'upper',hinge:'right',open:'false',powered:'true'}}",
			"{Name:'minecraft:jungle_door',Properties:{facing:'east',half:'upper',hinge:'right',open:'true',powered:'true'}}",
			"{Name:'minecraft:jungle_door',Properties:{facing:'north',half:'upper',hinge:'right',open:'false',powered:'true'}}",
			"{Name:'minecraft:jungle_door',Properties:{facing:'north',half:'upper',hinge:'right',open:'true',powered:'true'}}",
			"{Name:'minecraft:jungle_door',Properties:{facing:'south',half:'upper',hinge:'right',open:'false',powered:'true'}}",
			"{Name:'minecraft:jungle_door',Properties:{facing:'south',half:'upper',hinge:'right',open:'true',powered:'true'}}",
			"{Name:'minecraft:jungle_door',Properties:{facing:'west',half:'upper',hinge:'right',open:'false',powered:'true'}}",
			"{Name:'minecraft:jungle_door',Properties:{facing:'west',half:'upper',hinge:'right',open:'true',powered:'true'}}"
		);
		putStates(
			3136,
			"{Name:'minecraft:acacia_door',Properties:{facing:'east',half:'lower',hinge:'right',open:'false',powered:'false'}}",
			"{Name:'minecraft:acacia_door',Properties:{facing:'east',half:'lower',hinge:'left',open:'false',powered:'false'}}",
			"{Name:'minecraft:acacia_door',Properties:{facing:'east',half:'lower',hinge:'left',open:'false',powered:'true'}}",
			"{Name:'minecraft:acacia_door',Properties:{facing:'east',half:'lower',hinge:'right',open:'false',powered:'false'}}",
			"{Name:'minecraft:acacia_door',Properties:{facing:'east',half:'lower',hinge:'right',open:'false',powered:'true'}}"
		);
		putStates(
			3137,
			"{Name:'minecraft:acacia_door',Properties:{facing:'south',half:'lower',hinge:'right',open:'false',powered:'false'}}",
			"{Name:'minecraft:acacia_door',Properties:{facing:'south',half:'lower',hinge:'left',open:'false',powered:'false'}}",
			"{Name:'minecraft:acacia_door',Properties:{facing:'south',half:'lower',hinge:'left',open:'false',powered:'true'}}",
			"{Name:'minecraft:acacia_door',Properties:{facing:'south',half:'lower',hinge:'right',open:'false',powered:'false'}}",
			"{Name:'minecraft:acacia_door',Properties:{facing:'south',half:'lower',hinge:'right',open:'false',powered:'true'}}"
		);
		putStates(
			3138,
			"{Name:'minecraft:acacia_door',Properties:{facing:'west',half:'lower',hinge:'right',open:'false',powered:'false'}}",
			"{Name:'minecraft:acacia_door',Properties:{facing:'west',half:'lower',hinge:'left',open:'false',powered:'false'}}",
			"{Name:'minecraft:acacia_door',Properties:{facing:'west',half:'lower',hinge:'left',open:'false',powered:'true'}}",
			"{Name:'minecraft:acacia_door',Properties:{facing:'west',half:'lower',hinge:'right',open:'false',powered:'false'}}",
			"{Name:'minecraft:acacia_door',Properties:{facing:'west',half:'lower',hinge:'right',open:'false',powered:'true'}}"
		);
		putStates(
			3139,
			"{Name:'minecraft:acacia_door',Properties:{facing:'north',half:'lower',hinge:'right',open:'false',powered:'false'}}",
			"{Name:'minecraft:acacia_door',Properties:{facing:'north',half:'lower',hinge:'left',open:'false',powered:'false'}}",
			"{Name:'minecraft:acacia_door',Properties:{facing:'north',half:'lower',hinge:'left',open:'false',powered:'true'}}",
			"{Name:'minecraft:acacia_door',Properties:{facing:'north',half:'lower',hinge:'right',open:'false',powered:'false'}}",
			"{Name:'minecraft:acacia_door',Properties:{facing:'north',half:'lower',hinge:'right',open:'false',powered:'true'}}"
		);
		putStates(
			3140,
			"{Name:'minecraft:acacia_door',Properties:{facing:'east',half:'lower',hinge:'right',open:'true',powered:'false'}}",
			"{Name:'minecraft:acacia_door',Properties:{facing:'east',half:'lower',hinge:'left',open:'true',powered:'false'}}",
			"{Name:'minecraft:acacia_door',Properties:{facing:'east',half:'lower',hinge:'left',open:'true',powered:'true'}}",
			"{Name:'minecraft:acacia_door',Properties:{facing:'east',half:'lower',hinge:'right',open:'true',powered:'false'}}",
			"{Name:'minecraft:acacia_door',Properties:{facing:'east',half:'lower',hinge:'right',open:'true',powered:'true'}}"
		);
		putStates(
			3141,
			"{Name:'minecraft:acacia_door',Properties:{facing:'south',half:'lower',hinge:'right',open:'true',powered:'false'}}",
			"{Name:'minecraft:acacia_door',Properties:{facing:'south',half:'lower',hinge:'left',open:'true',powered:'false'}}",
			"{Name:'minecraft:acacia_door',Properties:{facing:'south',half:'lower',hinge:'left',open:'true',powered:'true'}}",
			"{Name:'minecraft:acacia_door',Properties:{facing:'south',half:'lower',hinge:'right',open:'true',powered:'false'}}",
			"{Name:'minecraft:acacia_door',Properties:{facing:'south',half:'lower',hinge:'right',open:'true',powered:'true'}}"
		);
		putStates(
			3142,
			"{Name:'minecraft:acacia_door',Properties:{facing:'west',half:'lower',hinge:'right',open:'true',powered:'false'}}",
			"{Name:'minecraft:acacia_door',Properties:{facing:'west',half:'lower',hinge:'left',open:'true',powered:'false'}}",
			"{Name:'minecraft:acacia_door',Properties:{facing:'west',half:'lower',hinge:'left',open:'true',powered:'true'}}",
			"{Name:'minecraft:acacia_door',Properties:{facing:'west',half:'lower',hinge:'right',open:'true',powered:'false'}}",
			"{Name:'minecraft:acacia_door',Properties:{facing:'west',half:'lower',hinge:'right',open:'true',powered:'true'}}"
		);
		putStates(
			3143,
			"{Name:'minecraft:acacia_door',Properties:{facing:'north',half:'lower',hinge:'right',open:'true',powered:'false'}}",
			"{Name:'minecraft:acacia_door',Properties:{facing:'north',half:'lower',hinge:'left',open:'true',powered:'false'}}",
			"{Name:'minecraft:acacia_door',Properties:{facing:'north',half:'lower',hinge:'left',open:'true',powered:'true'}}",
			"{Name:'minecraft:acacia_door',Properties:{facing:'north',half:'lower',hinge:'right',open:'true',powered:'false'}}",
			"{Name:'minecraft:acacia_door',Properties:{facing:'north',half:'lower',hinge:'right',open:'true',powered:'true'}}"
		);
		putStates(
			3144,
			"{Name:'minecraft:acacia_door',Properties:{facing:'east',half:'upper',hinge:'left',open:'false',powered:'false'}}",
			"{Name:'minecraft:acacia_door',Properties:{facing:'east',half:'upper',hinge:'left',open:'false',powered:'false'}}",
			"{Name:'minecraft:acacia_door',Properties:{facing:'east',half:'upper',hinge:'left',open:'true',powered:'false'}}",
			"{Name:'minecraft:acacia_door',Properties:{facing:'north',half:'upper',hinge:'left',open:'false',powered:'false'}}",
			"{Name:'minecraft:acacia_door',Properties:{facing:'north',half:'upper',hinge:'left',open:'true',powered:'false'}}",
			"{Name:'minecraft:acacia_door',Properties:{facing:'south',half:'upper',hinge:'left',open:'false',powered:'false'}}",
			"{Name:'minecraft:acacia_door',Properties:{facing:'south',half:'upper',hinge:'left',open:'true',powered:'false'}}",
			"{Name:'minecraft:acacia_door',Properties:{facing:'west',half:'upper',hinge:'left',open:'false',powered:'false'}}",
			"{Name:'minecraft:acacia_door',Properties:{facing:'west',half:'upper',hinge:'left',open:'true',powered:'false'}}"
		);
		putStates(
			3145,
			"{Name:'minecraft:acacia_door',Properties:{facing:'east',half:'upper',hinge:'right',open:'false',powered:'false'}}",
			"{Name:'minecraft:acacia_door',Properties:{facing:'east',half:'upper',hinge:'right',open:'false',powered:'false'}}",
			"{Name:'minecraft:acacia_door',Properties:{facing:'east',half:'upper',hinge:'right',open:'true',powered:'false'}}",
			"{Name:'minecraft:acacia_door',Properties:{facing:'north',half:'upper',hinge:'right',open:'false',powered:'false'}}",
			"{Name:'minecraft:acacia_door',Properties:{facing:'north',half:'upper',hinge:'right',open:'true',powered:'false'}}",
			"{Name:'minecraft:acacia_door',Properties:{facing:'south',half:'upper',hinge:'right',open:'false',powered:'false'}}",
			"{Name:'minecraft:acacia_door',Properties:{facing:'south',half:'upper',hinge:'right',open:'true',powered:'false'}}",
			"{Name:'minecraft:acacia_door',Properties:{facing:'west',half:'upper',hinge:'right',open:'false',powered:'false'}}",
			"{Name:'minecraft:acacia_door',Properties:{facing:'west',half:'upper',hinge:'right',open:'true',powered:'false'}}"
		);
		putStates(
			3146,
			"{Name:'minecraft:acacia_door',Properties:{facing:'east',half:'upper',hinge:'left',open:'false',powered:'true'}}",
			"{Name:'minecraft:acacia_door',Properties:{facing:'east',half:'upper',hinge:'left',open:'false',powered:'true'}}",
			"{Name:'minecraft:acacia_door',Properties:{facing:'east',half:'upper',hinge:'left',open:'true',powered:'true'}}",
			"{Name:'minecraft:acacia_door',Properties:{facing:'north',half:'upper',hinge:'left',open:'false',powered:'true'}}",
			"{Name:'minecraft:acacia_door',Properties:{facing:'north',half:'upper',hinge:'left',open:'true',powered:'true'}}",
			"{Name:'minecraft:acacia_door',Properties:{facing:'south',half:'upper',hinge:'left',open:'false',powered:'true'}}",
			"{Name:'minecraft:acacia_door',Properties:{facing:'south',half:'upper',hinge:'left',open:'true',powered:'true'}}",
			"{Name:'minecraft:acacia_door',Properties:{facing:'west',half:'upper',hinge:'left',open:'false',powered:'true'}}",
			"{Name:'minecraft:acacia_door',Properties:{facing:'west',half:'upper',hinge:'left',open:'true',powered:'true'}}"
		);
		putStates(
			3147,
			"{Name:'minecraft:acacia_door',Properties:{facing:'east',half:'upper',hinge:'right',open:'false',powered:'true'}}",
			"{Name:'minecraft:acacia_door',Properties:{facing:'east',half:'upper',hinge:'right',open:'false',powered:'true'}}",
			"{Name:'minecraft:acacia_door',Properties:{facing:'east',half:'upper',hinge:'right',open:'true',powered:'true'}}",
			"{Name:'minecraft:acacia_door',Properties:{facing:'north',half:'upper',hinge:'right',open:'false',powered:'true'}}",
			"{Name:'minecraft:acacia_door',Properties:{facing:'north',half:'upper',hinge:'right',open:'true',powered:'true'}}",
			"{Name:'minecraft:acacia_door',Properties:{facing:'south',half:'upper',hinge:'right',open:'false',powered:'true'}}",
			"{Name:'minecraft:acacia_door',Properties:{facing:'south',half:'upper',hinge:'right',open:'true',powered:'true'}}",
			"{Name:'minecraft:acacia_door',Properties:{facing:'west',half:'upper',hinge:'right',open:'false',powered:'true'}}",
			"{Name:'minecraft:acacia_door',Properties:{facing:'west',half:'upper',hinge:'right',open:'true',powered:'true'}}"
		);
		putStates(
			3152,
			"{Name:'minecraft:dark_oak_door',Properties:{facing:'east',half:'lower',hinge:'right',open:'false',powered:'false'}}",
			"{Name:'minecraft:dark_oak_door',Properties:{facing:'east',half:'lower',hinge:'left',open:'false',powered:'false'}}",
			"{Name:'minecraft:dark_oak_door',Properties:{facing:'east',half:'lower',hinge:'left',open:'false',powered:'true'}}",
			"{Name:'minecraft:dark_oak_door',Properties:{facing:'east',half:'lower',hinge:'right',open:'false',powered:'false'}}",
			"{Name:'minecraft:dark_oak_door',Properties:{facing:'east',half:'lower',hinge:'right',open:'false',powered:'true'}}"
		);
		putStates(
			3153,
			"{Name:'minecraft:dark_oak_door',Properties:{facing:'south',half:'lower',hinge:'right',open:'false',powered:'false'}}",
			"{Name:'minecraft:dark_oak_door',Properties:{facing:'south',half:'lower',hinge:'left',open:'false',powered:'false'}}",
			"{Name:'minecraft:dark_oak_door',Properties:{facing:'south',half:'lower',hinge:'left',open:'false',powered:'true'}}",
			"{Name:'minecraft:dark_oak_door',Properties:{facing:'south',half:'lower',hinge:'right',open:'false',powered:'false'}}",
			"{Name:'minecraft:dark_oak_door',Properties:{facing:'south',half:'lower',hinge:'right',open:'false',powered:'true'}}"
		);
		putStates(
			3154,
			"{Name:'minecraft:dark_oak_door',Properties:{facing:'west',half:'lower',hinge:'right',open:'false',powered:'false'}}",
			"{Name:'minecraft:dark_oak_door',Properties:{facing:'west',half:'lower',hinge:'left',open:'false',powered:'false'}}",
			"{Name:'minecraft:dark_oak_door',Properties:{facing:'west',half:'lower',hinge:'left',open:'false',powered:'true'}}",
			"{Name:'minecraft:dark_oak_door',Properties:{facing:'west',half:'lower',hinge:'right',open:'false',powered:'false'}}",
			"{Name:'minecraft:dark_oak_door',Properties:{facing:'west',half:'lower',hinge:'right',open:'false',powered:'true'}}"
		);
		putStates(
			3155,
			"{Name:'minecraft:dark_oak_door',Properties:{facing:'north',half:'lower',hinge:'right',open:'false',powered:'false'}}",
			"{Name:'minecraft:dark_oak_door',Properties:{facing:'north',half:'lower',hinge:'left',open:'false',powered:'false'}}",
			"{Name:'minecraft:dark_oak_door',Properties:{facing:'north',half:'lower',hinge:'left',open:'false',powered:'true'}}",
			"{Name:'minecraft:dark_oak_door',Properties:{facing:'north',half:'lower',hinge:'right',open:'false',powered:'false'}}",
			"{Name:'minecraft:dark_oak_door',Properties:{facing:'north',half:'lower',hinge:'right',open:'false',powered:'true'}}"
		);
		putStates(
			3156,
			"{Name:'minecraft:dark_oak_door',Properties:{facing:'east',half:'lower',hinge:'right',open:'true',powered:'false'}}",
			"{Name:'minecraft:dark_oak_door',Properties:{facing:'east',half:'lower',hinge:'left',open:'true',powered:'false'}}",
			"{Name:'minecraft:dark_oak_door',Properties:{facing:'east',half:'lower',hinge:'left',open:'true',powered:'true'}}",
			"{Name:'minecraft:dark_oak_door',Properties:{facing:'east',half:'lower',hinge:'right',open:'true',powered:'false'}}",
			"{Name:'minecraft:dark_oak_door',Properties:{facing:'east',half:'lower',hinge:'right',open:'true',powered:'true'}}"
		);
		putStates(
			3157,
			"{Name:'minecraft:dark_oak_door',Properties:{facing:'south',half:'lower',hinge:'right',open:'true',powered:'false'}}",
			"{Name:'minecraft:dark_oak_door',Properties:{facing:'south',half:'lower',hinge:'left',open:'true',powered:'false'}}",
			"{Name:'minecraft:dark_oak_door',Properties:{facing:'south',half:'lower',hinge:'left',open:'true',powered:'true'}}",
			"{Name:'minecraft:dark_oak_door',Properties:{facing:'south',half:'lower',hinge:'right',open:'true',powered:'false'}}",
			"{Name:'minecraft:dark_oak_door',Properties:{facing:'south',half:'lower',hinge:'right',open:'true',powered:'true'}}"
		);
		putStates(
			3158,
			"{Name:'minecraft:dark_oak_door',Properties:{facing:'west',half:'lower',hinge:'right',open:'true',powered:'false'}}",
			"{Name:'minecraft:dark_oak_door',Properties:{facing:'west',half:'lower',hinge:'left',open:'true',powered:'false'}}",
			"{Name:'minecraft:dark_oak_door',Properties:{facing:'west',half:'lower',hinge:'left',open:'true',powered:'true'}}",
			"{Name:'minecraft:dark_oak_door',Properties:{facing:'west',half:'lower',hinge:'right',open:'true',powered:'false'}}",
			"{Name:'minecraft:dark_oak_door',Properties:{facing:'west',half:'lower',hinge:'right',open:'true',powered:'true'}}"
		);
		putStates(
			3159,
			"{Name:'minecraft:dark_oak_door',Properties:{facing:'north',half:'lower',hinge:'right',open:'true',powered:'false'}}",
			"{Name:'minecraft:dark_oak_door',Properties:{facing:'north',half:'lower',hinge:'left',open:'true',powered:'false'}}",
			"{Name:'minecraft:dark_oak_door',Properties:{facing:'north',half:'lower',hinge:'left',open:'true',powered:'true'}}",
			"{Name:'minecraft:dark_oak_door',Properties:{facing:'north',half:'lower',hinge:'right',open:'true',powered:'false'}}",
			"{Name:'minecraft:dark_oak_door',Properties:{facing:'north',half:'lower',hinge:'right',open:'true',powered:'true'}}"
		);
		putStates(
			3160,
			"{Name:'minecraft:dark_oak_door',Properties:{facing:'east',half:'upper',hinge:'left',open:'false',powered:'false'}}",
			"{Name:'minecraft:dark_oak_door',Properties:{facing:'east',half:'upper',hinge:'left',open:'false',powered:'false'}}",
			"{Name:'minecraft:dark_oak_door',Properties:{facing:'east',half:'upper',hinge:'left',open:'true',powered:'false'}}",
			"{Name:'minecraft:dark_oak_door',Properties:{facing:'north',half:'upper',hinge:'left',open:'false',powered:'false'}}",
			"{Name:'minecraft:dark_oak_door',Properties:{facing:'north',half:'upper',hinge:'left',open:'true',powered:'false'}}",
			"{Name:'minecraft:dark_oak_door',Properties:{facing:'south',half:'upper',hinge:'left',open:'false',powered:'false'}}",
			"{Name:'minecraft:dark_oak_door',Properties:{facing:'south',half:'upper',hinge:'left',open:'true',powered:'false'}}",
			"{Name:'minecraft:dark_oak_door',Properties:{facing:'west',half:'upper',hinge:'left',open:'false',powered:'false'}}",
			"{Name:'minecraft:dark_oak_door',Properties:{facing:'west',half:'upper',hinge:'left',open:'true',powered:'false'}}"
		);
		putStates(
			3161,
			"{Name:'minecraft:dark_oak_door',Properties:{facing:'east',half:'upper',hinge:'right',open:'false',powered:'false'}}",
			"{Name:'minecraft:dark_oak_door',Properties:{facing:'east',half:'upper',hinge:'right',open:'false',powered:'false'}}",
			"{Name:'minecraft:dark_oak_door',Properties:{facing:'east',half:'upper',hinge:'right',open:'true',powered:'false'}}",
			"{Name:'minecraft:dark_oak_door',Properties:{facing:'north',half:'upper',hinge:'right',open:'false',powered:'false'}}",
			"{Name:'minecraft:dark_oak_door',Properties:{facing:'north',half:'upper',hinge:'right',open:'true',powered:'false'}}",
			"{Name:'minecraft:dark_oak_door',Properties:{facing:'south',half:'upper',hinge:'right',open:'false',powered:'false'}}",
			"{Name:'minecraft:dark_oak_door',Properties:{facing:'south',half:'upper',hinge:'right',open:'true',powered:'false'}}",
			"{Name:'minecraft:dark_oak_door',Properties:{facing:'west',half:'upper',hinge:'right',open:'false',powered:'false'}}",
			"{Name:'minecraft:dark_oak_door',Properties:{facing:'west',half:'upper',hinge:'right',open:'true',powered:'false'}}"
		);
		putStates(
			3162,
			"{Name:'minecraft:dark_oak_door',Properties:{facing:'east',half:'upper',hinge:'left',open:'false',powered:'true'}}",
			"{Name:'minecraft:dark_oak_door',Properties:{facing:'east',half:'upper',hinge:'left',open:'false',powered:'true'}}",
			"{Name:'minecraft:dark_oak_door',Properties:{facing:'east',half:'upper',hinge:'left',open:'true',powered:'true'}}",
			"{Name:'minecraft:dark_oak_door',Properties:{facing:'north',half:'upper',hinge:'left',open:'false',powered:'true'}}",
			"{Name:'minecraft:dark_oak_door',Properties:{facing:'north',half:'upper',hinge:'left',open:'true',powered:'true'}}",
			"{Name:'minecraft:dark_oak_door',Properties:{facing:'south',half:'upper',hinge:'left',open:'false',powered:'true'}}",
			"{Name:'minecraft:dark_oak_door',Properties:{facing:'south',half:'upper',hinge:'left',open:'true',powered:'true'}}",
			"{Name:'minecraft:dark_oak_door',Properties:{facing:'west',half:'upper',hinge:'left',open:'false',powered:'true'}}",
			"{Name:'minecraft:dark_oak_door',Properties:{facing:'west',half:'upper',hinge:'left',open:'true',powered:'true'}}"
		);
		putStates(
			3163,
			"{Name:'minecraft:dark_oak_door',Properties:{facing:'east',half:'upper',hinge:'right',open:'false',powered:'true'}}",
			"{Name:'minecraft:dark_oak_door',Properties:{facing:'east',half:'upper',hinge:'right',open:'false',powered:'true'}}",
			"{Name:'minecraft:dark_oak_door',Properties:{facing:'east',half:'upper',hinge:'right',open:'true',powered:'true'}}",
			"{Name:'minecraft:dark_oak_door',Properties:{facing:'north',half:'upper',hinge:'right',open:'false',powered:'true'}}",
			"{Name:'minecraft:dark_oak_door',Properties:{facing:'north',half:'upper',hinge:'right',open:'true',powered:'true'}}",
			"{Name:'minecraft:dark_oak_door',Properties:{facing:'south',half:'upper',hinge:'right',open:'false',powered:'true'}}",
			"{Name:'minecraft:dark_oak_door',Properties:{facing:'south',half:'upper',hinge:'right',open:'true',powered:'true'}}",
			"{Name:'minecraft:dark_oak_door',Properties:{facing:'west',half:'upper',hinge:'right',open:'false',powered:'true'}}",
			"{Name:'minecraft:dark_oak_door',Properties:{facing:'west',half:'upper',hinge:'right',open:'true',powered:'true'}}"
		);
		putStates(3168, "{Name:'minecraft:end_rod',Properties:{facing:'down'}}", "{Name:'minecraft:end_rod',Properties:{facing:'down'}}");
		putStates(3169, "{Name:'minecraft:end_rod',Properties:{facing:'up'}}", "{Name:'minecraft:end_rod',Properties:{facing:'up'}}");
		putStates(3170, "{Name:'minecraft:end_rod',Properties:{facing:'north'}}", "{Name:'minecraft:end_rod',Properties:{facing:'north'}}");
		putStates(3171, "{Name:'minecraft:end_rod',Properties:{facing:'south'}}", "{Name:'minecraft:end_rod',Properties:{facing:'south'}}");
		putStates(3172, "{Name:'minecraft:end_rod',Properties:{facing:'west'}}", "{Name:'minecraft:end_rod',Properties:{facing:'west'}}");
		putStates(3173, "{Name:'minecraft:end_rod',Properties:{facing:'east'}}", "{Name:'minecraft:end_rod',Properties:{facing:'east'}}");
		putStates(
			3184,
			"{Name:'minecraft:chorus_plant',Properties:{down:'false',east:'false',north:'false',south:'false',up:'false',west:'false'}}",
			"{Name:'minecraft:chorus_plant',Properties:{down:'false',east:'false',north:'false',south:'false',up:'false',west:'false'}}",
			"{Name:'minecraft:chorus_plant',Properties:{down:'false',east:'false',north:'false',south:'false',up:'false',west:'true'}}",
			"{Name:'minecraft:chorus_plant',Properties:{down:'false',east:'false',north:'false',south:'false',up:'true',west:'false'}}",
			"{Name:'minecraft:chorus_plant',Properties:{down:'false',east:'false',north:'false',south:'false',up:'true',west:'true'}}",
			"{Name:'minecraft:chorus_plant',Properties:{down:'false',east:'false',north:'false',south:'true',up:'false',west:'false'}}",
			"{Name:'minecraft:chorus_plant',Properties:{down:'false',east:'false',north:'false',south:'true',up:'false',west:'true'}}",
			"{Name:'minecraft:chorus_plant',Properties:{down:'false',east:'false',north:'false',south:'true',up:'true',west:'false'}}",
			"{Name:'minecraft:chorus_plant',Properties:{down:'false',east:'false',north:'false',south:'true',up:'true',west:'true'}}",
			"{Name:'minecraft:chorus_plant',Properties:{down:'false',east:'false',north:'true',south:'false',up:'false',west:'false'}}",
			"{Name:'minecraft:chorus_plant',Properties:{down:'false',east:'false',north:'true',south:'false',up:'false',west:'true'}}",
			"{Name:'minecraft:chorus_plant',Properties:{down:'false',east:'false',north:'true',south:'false',up:'true',west:'false'}}",
			"{Name:'minecraft:chorus_plant',Properties:{down:'false',east:'false',north:'true',south:'false',up:'true',west:'true'}}",
			"{Name:'minecraft:chorus_plant',Properties:{down:'false',east:'false',north:'true',south:'true',up:'false',west:'false'}}",
			"{Name:'minecraft:chorus_plant',Properties:{down:'false',east:'false',north:'true',south:'true',up:'false',west:'true'}}",
			"{Name:'minecraft:chorus_plant',Properties:{down:'false',east:'false',north:'true',south:'true',up:'true',west:'false'}}",
			"{Name:'minecraft:chorus_plant',Properties:{down:'false',east:'false',north:'true',south:'true',up:'true',west:'true'}}",
			"{Name:'minecraft:chorus_plant',Properties:{down:'false',east:'true',north:'false',south:'false',up:'false',west:'false'}}",
			"{Name:'minecraft:chorus_plant',Properties:{down:'false',east:'true',north:'false',south:'false',up:'false',west:'true'}}",
			"{Name:'minecraft:chorus_plant',Properties:{down:'false',east:'true',north:'false',south:'false',up:'true',west:'false'}}",
			"{Name:'minecraft:chorus_plant',Properties:{down:'false',east:'true',north:'false',south:'false',up:'true',west:'true'}}",
			"{Name:'minecraft:chorus_plant',Properties:{down:'false',east:'true',north:'false',south:'true',up:'false',west:'false'}}",
			"{Name:'minecraft:chorus_plant',Properties:{down:'false',east:'true',north:'false',south:'true',up:'false',west:'true'}}",
			"{Name:'minecraft:chorus_plant',Properties:{down:'false',east:'true',north:'false',south:'true',up:'true',west:'false'}}",
			"{Name:'minecraft:chorus_plant',Properties:{down:'false',east:'true',north:'false',south:'true',up:'true',west:'true'}}",
			"{Name:'minecraft:chorus_plant',Properties:{down:'false',east:'true',north:'true',south:'false',up:'false',west:'false'}}",
			"{Name:'minecraft:chorus_plant',Properties:{down:'false',east:'true',north:'true',south:'false',up:'false',west:'true'}}",
			"{Name:'minecraft:chorus_plant',Properties:{down:'false',east:'true',north:'true',south:'false',up:'true',west:'false'}}",
			"{Name:'minecraft:chorus_plant',Properties:{down:'false',east:'true',north:'true',south:'false',up:'true',west:'true'}}",
			"{Name:'minecraft:chorus_plant',Properties:{down:'false',east:'true',north:'true',south:'true',up:'false',west:'false'}}",
			"{Name:'minecraft:chorus_plant',Properties:{down:'false',east:'true',north:'true',south:'true',up:'false',west:'true'}}",
			"{Name:'minecraft:chorus_plant',Properties:{down:'false',east:'true',north:'true',south:'true',up:'true',west:'false'}}",
			"{Name:'minecraft:chorus_plant',Properties:{down:'false',east:'true',north:'true',south:'true',up:'true',west:'true'}}",
			"{Name:'minecraft:chorus_plant',Properties:{down:'true',east:'false',north:'false',south:'false',up:'false',west:'false'}}",
			"{Name:'minecraft:chorus_plant',Properties:{down:'true',east:'false',north:'false',south:'false',up:'false',west:'true'}}",
			"{Name:'minecraft:chorus_plant',Properties:{down:'true',east:'false',north:'false',south:'false',up:'true',west:'false'}}",
			"{Name:'minecraft:chorus_plant',Properties:{down:'true',east:'false',north:'false',south:'false',up:'true',west:'true'}}",
			"{Name:'minecraft:chorus_plant',Properties:{down:'true',east:'false',north:'false',south:'true',up:'false',west:'false'}}",
			"{Name:'minecraft:chorus_plant',Properties:{down:'true',east:'false',north:'false',south:'true',up:'false',west:'true'}}",
			"{Name:'minecraft:chorus_plant',Properties:{down:'true',east:'false',north:'false',south:'true',up:'true',west:'false'}}",
			"{Name:'minecraft:chorus_plant',Properties:{down:'true',east:'false',north:'false',south:'true',up:'true',west:'true'}}",
			"{Name:'minecraft:chorus_plant',Properties:{down:'true',east:'false',north:'true',south:'false',up:'false',west:'false'}}",
			"{Name:'minecraft:chorus_plant',Properties:{down:'true',east:'false',north:'true',south:'false',up:'false',west:'true'}}",
			"{Name:'minecraft:chorus_plant',Properties:{down:'true',east:'false',north:'true',south:'false',up:'true',west:'false'}}",
			"{Name:'minecraft:chorus_plant',Properties:{down:'true',east:'false',north:'true',south:'false',up:'true',west:'true'}}",
			"{Name:'minecraft:chorus_plant',Properties:{down:'true',east:'false',north:'true',south:'true',up:'false',west:'false'}}",
			"{Name:'minecraft:chorus_plant',Properties:{down:'true',east:'false',north:'true',south:'true',up:'false',west:'true'}}",
			"{Name:'minecraft:chorus_plant',Properties:{down:'true',east:'false',north:'true',south:'true',up:'true',west:'false'}}",
			"{Name:'minecraft:chorus_plant',Properties:{down:'true',east:'false',north:'true',south:'true',up:'true',west:'true'}}",
			"{Name:'minecraft:chorus_plant',Properties:{down:'true',east:'true',north:'false',south:'false',up:'false',west:'false'}}",
			"{Name:'minecraft:chorus_plant',Properties:{down:'true',east:'true',north:'false',south:'false',up:'false',west:'true'}}",
			"{Name:'minecraft:chorus_plant',Properties:{down:'true',east:'true',north:'false',south:'false',up:'true',west:'false'}}",
			"{Name:'minecraft:chorus_plant',Properties:{down:'true',east:'true',north:'false',south:'false',up:'true',west:'true'}}",
			"{Name:'minecraft:chorus_plant',Properties:{down:'true',east:'true',north:'false',south:'true',up:'false',west:'false'}}",
			"{Name:'minecraft:chorus_plant',Properties:{down:'true',east:'true',north:'false',south:'true',up:'false',west:'true'}}",
			"{Name:'minecraft:chorus_plant',Properties:{down:'true',east:'true',north:'false',south:'true',up:'true',west:'false'}}",
			"{Name:'minecraft:chorus_plant',Properties:{down:'true',east:'true',north:'false',south:'true',up:'true',west:'true'}}",
			"{Name:'minecraft:chorus_plant',Properties:{down:'true',east:'true',north:'true',south:'false',up:'false',west:'false'}}",
			"{Name:'minecraft:chorus_plant',Properties:{down:'true',east:'true',north:'true',south:'false',up:'false',west:'true'}}",
			"{Name:'minecraft:chorus_plant',Properties:{down:'true',east:'true',north:'true',south:'false',up:'true',west:'false'}}",
			"{Name:'minecraft:chorus_plant',Properties:{down:'true',east:'true',north:'true',south:'false',up:'true',west:'true'}}",
			"{Name:'minecraft:chorus_plant',Properties:{down:'true',east:'true',north:'true',south:'true',up:'false',west:'false'}}",
			"{Name:'minecraft:chorus_plant',Properties:{down:'true',east:'true',north:'true',south:'true',up:'false',west:'true'}}",
			"{Name:'minecraft:chorus_plant',Properties:{down:'true',east:'true',north:'true',south:'true',up:'true',west:'false'}}",
			"{Name:'minecraft:chorus_plant',Properties:{down:'true',east:'true',north:'true',south:'true',up:'true',west:'true'}}"
		);
		putStates(3200, "{Name:'minecraft:chorus_flower',Properties:{age:'0'}}", "{Name:'minecraft:chorus_flower',Properties:{age:'0'}}");
		putStates(3201, "{Name:'minecraft:chorus_flower',Properties:{age:'1'}}", "{Name:'minecraft:chorus_flower',Properties:{age:'1'}}");
		putStates(3202, "{Name:'minecraft:chorus_flower',Properties:{age:'2'}}", "{Name:'minecraft:chorus_flower',Properties:{age:'2'}}");
		putStates(3203, "{Name:'minecraft:chorus_flower',Properties:{age:'3'}}", "{Name:'minecraft:chorus_flower',Properties:{age:'3'}}");
		putStates(3204, "{Name:'minecraft:chorus_flower',Properties:{age:'4'}}", "{Name:'minecraft:chorus_flower',Properties:{age:'4'}}");
		putStates(3205, "{Name:'minecraft:chorus_flower',Properties:{age:'5'}}", "{Name:'minecraft:chorus_flower',Properties:{age:'5'}}");
		putStates(3216, "{Name:'minecraft:purpur_block'}", "{Name:'minecraft:purpur_block'}");
		putStates(3232, "{Name:'minecraft:purpur_pillar',Properties:{axis:'y'}}", "{Name:'minecraft:purpur_pillar',Properties:{axis:'y'}}");
		putStates(3236, "{Name:'minecraft:purpur_pillar',Properties:{axis:'x'}}", "{Name:'minecraft:purpur_pillar',Properties:{axis:'x'}}");
		putStates(3240, "{Name:'minecraft:purpur_pillar',Properties:{axis:'z'}}", "{Name:'minecraft:purpur_pillar',Properties:{axis:'z'}}");
		putStates(
			3248,
			"{Name:'minecraft:purpur_stairs',Properties:{facing:'east',half:'bottom',shape:'straight'}}",
			"{Name:'minecraft:purpur_stairs',Properties:{facing:'east',half:'bottom',shape:'inner_left'}}",
			"{Name:'minecraft:purpur_stairs',Properties:{facing:'east',half:'bottom',shape:'inner_right'}}",
			"{Name:'minecraft:purpur_stairs',Properties:{facing:'east',half:'bottom',shape:'outer_left'}}",
			"{Name:'minecraft:purpur_stairs',Properties:{facing:'east',half:'bottom',shape:'outer_right'}}",
			"{Name:'minecraft:purpur_stairs',Properties:{facing:'east',half:'bottom',shape:'straight'}}"
		);
		putStates(
			3249,
			"{Name:'minecraft:purpur_stairs',Properties:{facing:'west',half:'bottom',shape:'straight'}}",
			"{Name:'minecraft:purpur_stairs',Properties:{facing:'west',half:'bottom',shape:'inner_left'}}",
			"{Name:'minecraft:purpur_stairs',Properties:{facing:'west',half:'bottom',shape:'inner_right'}}",
			"{Name:'minecraft:purpur_stairs',Properties:{facing:'west',half:'bottom',shape:'outer_left'}}",
			"{Name:'minecraft:purpur_stairs',Properties:{facing:'west',half:'bottom',shape:'outer_right'}}",
			"{Name:'minecraft:purpur_stairs',Properties:{facing:'west',half:'bottom',shape:'straight'}}"
		);
		putStates(
			3250,
			"{Name:'minecraft:purpur_stairs',Properties:{facing:'south',half:'bottom',shape:'straight'}}",
			"{Name:'minecraft:purpur_stairs',Properties:{facing:'south',half:'bottom',shape:'inner_left'}}",
			"{Name:'minecraft:purpur_stairs',Properties:{facing:'south',half:'bottom',shape:'inner_right'}}",
			"{Name:'minecraft:purpur_stairs',Properties:{facing:'south',half:'bottom',shape:'outer_left'}}",
			"{Name:'minecraft:purpur_stairs',Properties:{facing:'south',half:'bottom',shape:'outer_right'}}",
			"{Name:'minecraft:purpur_stairs',Properties:{facing:'south',half:'bottom',shape:'straight'}}"
		);
		putStates(
			3251,
			"{Name:'minecraft:purpur_stairs',Properties:{facing:'north',half:'bottom',shape:'straight'}}",
			"{Name:'minecraft:purpur_stairs',Properties:{facing:'north',half:'bottom',shape:'inner_left'}}",
			"{Name:'minecraft:purpur_stairs',Properties:{facing:'north',half:'bottom',shape:'inner_right'}}",
			"{Name:'minecraft:purpur_stairs',Properties:{facing:'north',half:'bottom',shape:'outer_left'}}",
			"{Name:'minecraft:purpur_stairs',Properties:{facing:'north',half:'bottom',shape:'outer_right'}}",
			"{Name:'minecraft:purpur_stairs',Properties:{facing:'north',half:'bottom',shape:'straight'}}"
		);
		putStates(
			3252,
			"{Name:'minecraft:purpur_stairs',Properties:{facing:'east',half:'top',shape:'straight'}}",
			"{Name:'minecraft:purpur_stairs',Properties:{facing:'east',half:'top',shape:'inner_left'}}",
			"{Name:'minecraft:purpur_stairs',Properties:{facing:'east',half:'top',shape:'inner_right'}}",
			"{Name:'minecraft:purpur_stairs',Properties:{facing:'east',half:'top',shape:'outer_left'}}",
			"{Name:'minecraft:purpur_stairs',Properties:{facing:'east',half:'top',shape:'outer_right'}}",
			"{Name:'minecraft:purpur_stairs',Properties:{facing:'east',half:'top',shape:'straight'}}"
		);
		putStates(
			3253,
			"{Name:'minecraft:purpur_stairs',Properties:{facing:'west',half:'top',shape:'straight'}}",
			"{Name:'minecraft:purpur_stairs',Properties:{facing:'west',half:'top',shape:'inner_left'}}",
			"{Name:'minecraft:purpur_stairs',Properties:{facing:'west',half:'top',shape:'inner_right'}}",
			"{Name:'minecraft:purpur_stairs',Properties:{facing:'west',half:'top',shape:'outer_left'}}",
			"{Name:'minecraft:purpur_stairs',Properties:{facing:'west',half:'top',shape:'outer_right'}}",
			"{Name:'minecraft:purpur_stairs',Properties:{facing:'west',half:'top',shape:'straight'}}"
		);
		putStates(
			3254,
			"{Name:'minecraft:purpur_stairs',Properties:{facing:'south',half:'top',shape:'straight'}}",
			"{Name:'minecraft:purpur_stairs',Properties:{facing:'south',half:'top',shape:'inner_left'}}",
			"{Name:'minecraft:purpur_stairs',Properties:{facing:'south',half:'top',shape:'inner_right'}}",
			"{Name:'minecraft:purpur_stairs',Properties:{facing:'south',half:'top',shape:'outer_left'}}",
			"{Name:'minecraft:purpur_stairs',Properties:{facing:'south',half:'top',shape:'outer_right'}}",
			"{Name:'minecraft:purpur_stairs',Properties:{facing:'south',half:'top',shape:'straight'}}"
		);
		putStates(
			3255,
			"{Name:'minecraft:purpur_stairs',Properties:{facing:'north',half:'top',shape:'straight'}}",
			"{Name:'minecraft:purpur_stairs',Properties:{facing:'north',half:'top',shape:'inner_left'}}",
			"{Name:'minecraft:purpur_stairs',Properties:{facing:'north',half:'top',shape:'inner_right'}}",
			"{Name:'minecraft:purpur_stairs',Properties:{facing:'north',half:'top',shape:'outer_left'}}",
			"{Name:'minecraft:purpur_stairs',Properties:{facing:'north',half:'top',shape:'outer_right'}}",
			"{Name:'minecraft:purpur_stairs',Properties:{facing:'north',half:'top',shape:'straight'}}"
		);
		putStates(3264, "{Name:'minecraft:purpur_slab',Properties:{type:'double'}}", "{Name:'minecraft:purpur_double_slab',Properties:{variant:'default'}}");
		putStates(3280, "{Name:'minecraft:purpur_slab',Properties:{type:'bottom'}}", "{Name:'minecraft:purpur_slab',Properties:{half:'bottom',variant:'default'}}");
		putStates(3288, "{Name:'minecraft:purpur_slab',Properties:{type:'top'}}", "{Name:'minecraft:purpur_slab',Properties:{half:'top',variant:'default'}}");
		putStates(3296, "{Name:'minecraft:end_stone_bricks'}", "{Name:'minecraft:end_bricks'}");
		putStates(3312, "{Name:'minecraft:beetroots',Properties:{age:'0'}}", "{Name:'minecraft:beetroots',Properties:{age:'0'}}");
		putStates(3313, "{Name:'minecraft:beetroots',Properties:{age:'1'}}", "{Name:'minecraft:beetroots',Properties:{age:'1'}}");
		putStates(3314, "{Name:'minecraft:beetroots',Properties:{age:'2'}}", "{Name:'minecraft:beetroots',Properties:{age:'2'}}");
		putStates(3315, "{Name:'minecraft:beetroots',Properties:{age:'3'}}", "{Name:'minecraft:beetroots',Properties:{age:'3'}}");
	}

	/**
	 * Adds states to flatten from the blocks which had numeric IDs between 208 and 223 before 1.13.
	 */
	static void putStatesFromBlocks208To223() {
		putStates(3328, "{Name:'minecraft:grass_path'}", "{Name:'minecraft:grass_path'}");
		putStates(3344, "{Name:'minecraft:end_gateway'}", "{Name:'minecraft:end_gateway'}");
		putStates(
			3360,
			"{Name:'minecraft:repeating_command_block',Properties:{conditional:'false',facing:'down'}}",
			"{Name:'minecraft:repeating_command_block',Properties:{conditional:'false',facing:'down'}}"
		);
		putStates(
			3361,
			"{Name:'minecraft:repeating_command_block',Properties:{conditional:'false',facing:'up'}}",
			"{Name:'minecraft:repeating_command_block',Properties:{conditional:'false',facing:'up'}}"
		);
		putStates(
			3362,
			"{Name:'minecraft:repeating_command_block',Properties:{conditional:'false',facing:'north'}}",
			"{Name:'minecraft:repeating_command_block',Properties:{conditional:'false',facing:'north'}}"
		);
		putStates(
			3363,
			"{Name:'minecraft:repeating_command_block',Properties:{conditional:'false',facing:'south'}}",
			"{Name:'minecraft:repeating_command_block',Properties:{conditional:'false',facing:'south'}}"
		);
		putStates(
			3364,
			"{Name:'minecraft:repeating_command_block',Properties:{conditional:'false',facing:'west'}}",
			"{Name:'minecraft:repeating_command_block',Properties:{conditional:'false',facing:'west'}}"
		);
		putStates(
			3365,
			"{Name:'minecraft:repeating_command_block',Properties:{conditional:'false',facing:'east'}}",
			"{Name:'minecraft:repeating_command_block',Properties:{conditional:'false',facing:'east'}}"
		);
		putStates(
			3368,
			"{Name:'minecraft:repeating_command_block',Properties:{conditional:'true',facing:'down'}}",
			"{Name:'minecraft:repeating_command_block',Properties:{conditional:'true',facing:'down'}}"
		);
		putStates(
			3369,
			"{Name:'minecraft:repeating_command_block',Properties:{conditional:'true',facing:'up'}}",
			"{Name:'minecraft:repeating_command_block',Properties:{conditional:'true',facing:'up'}}"
		);
		putStates(
			3370,
			"{Name:'minecraft:repeating_command_block',Properties:{conditional:'true',facing:'north'}}",
			"{Name:'minecraft:repeating_command_block',Properties:{conditional:'true',facing:'north'}}"
		);
		putStates(
			3371,
			"{Name:'minecraft:repeating_command_block',Properties:{conditional:'true',facing:'south'}}",
			"{Name:'minecraft:repeating_command_block',Properties:{conditional:'true',facing:'south'}}"
		);
		putStates(
			3372,
			"{Name:'minecraft:repeating_command_block',Properties:{conditional:'true',facing:'west'}}",
			"{Name:'minecraft:repeating_command_block',Properties:{conditional:'true',facing:'west'}}"
		);
		putStates(
			3373,
			"{Name:'minecraft:repeating_command_block',Properties:{conditional:'true',facing:'east'}}",
			"{Name:'minecraft:repeating_command_block',Properties:{conditional:'true',facing:'east'}}"
		);
		putStates(
			3376,
			"{Name:'minecraft:chain_command_block',Properties:{conditional:'false',facing:'down'}}",
			"{Name:'minecraft:chain_command_block',Properties:{conditional:'false',facing:'down'}}"
		);
		putStates(
			3377,
			"{Name:'minecraft:chain_command_block',Properties:{conditional:'false',facing:'up'}}",
			"{Name:'minecraft:chain_command_block',Properties:{conditional:'false',facing:'up'}}"
		);
		putStates(
			3378,
			"{Name:'minecraft:chain_command_block',Properties:{conditional:'false',facing:'north'}}",
			"{Name:'minecraft:chain_command_block',Properties:{conditional:'false',facing:'north'}}"
		);
		putStates(
			3379,
			"{Name:'minecraft:chain_command_block',Properties:{conditional:'false',facing:'south'}}",
			"{Name:'minecraft:chain_command_block',Properties:{conditional:'false',facing:'south'}}"
		);
		putStates(
			3380,
			"{Name:'minecraft:chain_command_block',Properties:{conditional:'false',facing:'west'}}",
			"{Name:'minecraft:chain_command_block',Properties:{conditional:'false',facing:'west'}}"
		);
		putStates(
			3381,
			"{Name:'minecraft:chain_command_block',Properties:{conditional:'false',facing:'east'}}",
			"{Name:'minecraft:chain_command_block',Properties:{conditional:'false',facing:'east'}}"
		);
		putStates(
			3384,
			"{Name:'minecraft:chain_command_block',Properties:{conditional:'true',facing:'down'}}",
			"{Name:'minecraft:chain_command_block',Properties:{conditional:'true',facing:'down'}}"
		);
		putStates(
			3385,
			"{Name:'minecraft:chain_command_block',Properties:{conditional:'true',facing:'up'}}",
			"{Name:'minecraft:chain_command_block',Properties:{conditional:'true',facing:'up'}}"
		);
		putStates(
			3386,
			"{Name:'minecraft:chain_command_block',Properties:{conditional:'true',facing:'north'}}",
			"{Name:'minecraft:chain_command_block',Properties:{conditional:'true',facing:'north'}}"
		);
		putStates(
			3387,
			"{Name:'minecraft:chain_command_block',Properties:{conditional:'true',facing:'south'}}",
			"{Name:'minecraft:chain_command_block',Properties:{conditional:'true',facing:'south'}}"
		);
		putStates(
			3388,
			"{Name:'minecraft:chain_command_block',Properties:{conditional:'true',facing:'west'}}",
			"{Name:'minecraft:chain_command_block',Properties:{conditional:'true',facing:'west'}}"
		);
		putStates(
			3389,
			"{Name:'minecraft:chain_command_block',Properties:{conditional:'true',facing:'east'}}",
			"{Name:'minecraft:chain_command_block',Properties:{conditional:'true',facing:'east'}}"
		);
		putStates(3392, "{Name:'minecraft:frosted_ice',Properties:{age:'0'}}", "{Name:'minecraft:frosted_ice',Properties:{age:'0'}}");
		putStates(3393, "{Name:'minecraft:frosted_ice',Properties:{age:'1'}}", "{Name:'minecraft:frosted_ice',Properties:{age:'1'}}");
		putStates(3394, "{Name:'minecraft:frosted_ice',Properties:{age:'2'}}", "{Name:'minecraft:frosted_ice',Properties:{age:'2'}}");
		putStates(3395, "{Name:'minecraft:frosted_ice',Properties:{age:'3'}}", "{Name:'minecraft:frosted_ice',Properties:{age:'3'}}");
		putStates(3408, "{Name:'minecraft:magma_block'}", "{Name:'minecraft:magma'}");
		putStates(3424, "{Name:'minecraft:nether_wart_block'}", "{Name:'minecraft:nether_wart_block'}");
		putStates(3440, "{Name:'minecraft:red_nether_bricks'}", "{Name:'minecraft:red_nether_brick'}");
		putStates(3456, "{Name:'minecraft:bone_block',Properties:{axis:'y'}}", "{Name:'minecraft:bone_block',Properties:{axis:'y'}}");
		putStates(3460, "{Name:'minecraft:bone_block',Properties:{axis:'x'}}", "{Name:'minecraft:bone_block',Properties:{axis:'x'}}");
		putStates(3464, "{Name:'minecraft:bone_block',Properties:{axis:'z'}}", "{Name:'minecraft:bone_block',Properties:{axis:'z'}}");
		putStates(3472, "{Name:'minecraft:structure_void'}", "{Name:'minecraft:structure_void'}");
		putStates(
			3488, "{Name:'minecraft:observer',Properties:{facing:'down',powered:'false'}}", "{Name:'minecraft:observer',Properties:{facing:'down',powered:'false'}}"
		);
		putStates(
			3489, "{Name:'minecraft:observer',Properties:{facing:'up',powered:'false'}}", "{Name:'minecraft:observer',Properties:{facing:'up',powered:'false'}}"
		);
		putStates(
			3490, "{Name:'minecraft:observer',Properties:{facing:'north',powered:'false'}}", "{Name:'minecraft:observer',Properties:{facing:'north',powered:'false'}}"
		);
		putStates(
			3491, "{Name:'minecraft:observer',Properties:{facing:'south',powered:'false'}}", "{Name:'minecraft:observer',Properties:{facing:'south',powered:'false'}}"
		);
		putStates(
			3492, "{Name:'minecraft:observer',Properties:{facing:'west',powered:'false'}}", "{Name:'minecraft:observer',Properties:{facing:'west',powered:'false'}}"
		);
		putStates(
			3493, "{Name:'minecraft:observer',Properties:{facing:'east',powered:'false'}}", "{Name:'minecraft:observer',Properties:{facing:'east',powered:'false'}}"
		);
		putStates(
			3496, "{Name:'minecraft:observer',Properties:{facing:'down',powered:'true'}}", "{Name:'minecraft:observer',Properties:{facing:'down',powered:'true'}}"
		);
		putStates(3497, "{Name:'minecraft:observer',Properties:{facing:'up',powered:'true'}}", "{Name:'minecraft:observer',Properties:{facing:'up',powered:'true'}}");
		putStates(
			3498, "{Name:'minecraft:observer',Properties:{facing:'north',powered:'true'}}", "{Name:'minecraft:observer',Properties:{facing:'north',powered:'true'}}"
		);
		putStates(
			3499, "{Name:'minecraft:observer',Properties:{facing:'south',powered:'true'}}", "{Name:'minecraft:observer',Properties:{facing:'south',powered:'true'}}"
		);
		putStates(
			3500, "{Name:'minecraft:observer',Properties:{facing:'west',powered:'true'}}", "{Name:'minecraft:observer',Properties:{facing:'west',powered:'true'}}"
		);
		putStates(
			3501, "{Name:'minecraft:observer',Properties:{facing:'east',powered:'true'}}", "{Name:'minecraft:observer',Properties:{facing:'east',powered:'true'}}"
		);
		putStates(3504, "{Name:'minecraft:white_shulker_box',Properties:{facing:'down'}}", "{Name:'minecraft:white_shulker_box',Properties:{facing:'down'}}");
		putStates(3505, "{Name:'minecraft:white_shulker_box',Properties:{facing:'up'}}", "{Name:'minecraft:white_shulker_box',Properties:{facing:'up'}}");
		putStates(3506, "{Name:'minecraft:white_shulker_box',Properties:{facing:'north'}}", "{Name:'minecraft:white_shulker_box',Properties:{facing:'north'}}");
		putStates(3507, "{Name:'minecraft:white_shulker_box',Properties:{facing:'south'}}", "{Name:'minecraft:white_shulker_box',Properties:{facing:'south'}}");
		putStates(3508, "{Name:'minecraft:white_shulker_box',Properties:{facing:'west'}}", "{Name:'minecraft:white_shulker_box',Properties:{facing:'west'}}");
		putStates(3509, "{Name:'minecraft:white_shulker_box',Properties:{facing:'east'}}", "{Name:'minecraft:white_shulker_box',Properties:{facing:'east'}}");
		putStates(3520, "{Name:'minecraft:orange_shulker_box',Properties:{facing:'down'}}", "{Name:'minecraft:orange_shulker_box',Properties:{facing:'down'}}");
		putStates(3521, "{Name:'minecraft:orange_shulker_box',Properties:{facing:'up'}}", "{Name:'minecraft:orange_shulker_box',Properties:{facing:'up'}}");
		putStates(3522, "{Name:'minecraft:orange_shulker_box',Properties:{facing:'north'}}", "{Name:'minecraft:orange_shulker_box',Properties:{facing:'north'}}");
		putStates(3523, "{Name:'minecraft:orange_shulker_box',Properties:{facing:'south'}}", "{Name:'minecraft:orange_shulker_box',Properties:{facing:'south'}}");
		putStates(3524, "{Name:'minecraft:orange_shulker_box',Properties:{facing:'west'}}", "{Name:'minecraft:orange_shulker_box',Properties:{facing:'west'}}");
		putStates(3525, "{Name:'minecraft:orange_shulker_box',Properties:{facing:'east'}}", "{Name:'minecraft:orange_shulker_box',Properties:{facing:'east'}}");
		putStates(3536, "{Name:'minecraft:magenta_shulker_box',Properties:{facing:'down'}}", "{Name:'minecraft:magenta_shulker_box',Properties:{facing:'down'}}");
		putStates(3537, "{Name:'minecraft:magenta_shulker_box',Properties:{facing:'up'}}", "{Name:'minecraft:magenta_shulker_box',Properties:{facing:'up'}}");
		putStates(3538, "{Name:'minecraft:magenta_shulker_box',Properties:{facing:'north'}}", "{Name:'minecraft:magenta_shulker_box',Properties:{facing:'north'}}");
		putStates(3539, "{Name:'minecraft:magenta_shulker_box',Properties:{facing:'south'}}", "{Name:'minecraft:magenta_shulker_box',Properties:{facing:'south'}}");
		putStates(3540, "{Name:'minecraft:magenta_shulker_box',Properties:{facing:'west'}}", "{Name:'minecraft:magenta_shulker_box',Properties:{facing:'west'}}");
		putStates(3541, "{Name:'minecraft:magenta_shulker_box',Properties:{facing:'east'}}", "{Name:'minecraft:magenta_shulker_box',Properties:{facing:'east'}}");
		putStates(
			3552, "{Name:'minecraft:light_blue_shulker_box',Properties:{facing:'down'}}", "{Name:'minecraft:light_blue_shulker_box',Properties:{facing:'down'}}"
		);
		putStates(3553, "{Name:'minecraft:light_blue_shulker_box',Properties:{facing:'up'}}", "{Name:'minecraft:light_blue_shulker_box',Properties:{facing:'up'}}");
		putStates(
			3554, "{Name:'minecraft:light_blue_shulker_box',Properties:{facing:'north'}}", "{Name:'minecraft:light_blue_shulker_box',Properties:{facing:'north'}}"
		);
		putStates(
			3555, "{Name:'minecraft:light_blue_shulker_box',Properties:{facing:'south'}}", "{Name:'minecraft:light_blue_shulker_box',Properties:{facing:'south'}}"
		);
		putStates(
			3556, "{Name:'minecraft:light_blue_shulker_box',Properties:{facing:'west'}}", "{Name:'minecraft:light_blue_shulker_box',Properties:{facing:'west'}}"
		);
		putStates(
			3557, "{Name:'minecraft:light_blue_shulker_box',Properties:{facing:'east'}}", "{Name:'minecraft:light_blue_shulker_box',Properties:{facing:'east'}}"
		);
		putStates(3568, "{Name:'minecraft:yellow_shulker_box',Properties:{facing:'down'}}", "{Name:'minecraft:yellow_shulker_box',Properties:{facing:'down'}}");
		putStates(3569, "{Name:'minecraft:yellow_shulker_box',Properties:{facing:'up'}}", "{Name:'minecraft:yellow_shulker_box',Properties:{facing:'up'}}");
		putStates(3570, "{Name:'minecraft:yellow_shulker_box',Properties:{facing:'north'}}", "{Name:'minecraft:yellow_shulker_box',Properties:{facing:'north'}}");
		putStates(3571, "{Name:'minecraft:yellow_shulker_box',Properties:{facing:'south'}}", "{Name:'minecraft:yellow_shulker_box',Properties:{facing:'south'}}");
		putStates(3572, "{Name:'minecraft:yellow_shulker_box',Properties:{facing:'west'}}", "{Name:'minecraft:yellow_shulker_box',Properties:{facing:'west'}}");
		putStates(3573, "{Name:'minecraft:yellow_shulker_box',Properties:{facing:'east'}}", "{Name:'minecraft:yellow_shulker_box',Properties:{facing:'east'}}");
	}

	/**
	 * Adds states to flatten from the blocks which had numeric IDs between 224 and 239 before 1.13.
	 */
	static void putStatesFromBlocks224To239() {
		putStates(3584, "{Name:'minecraft:lime_shulker_box',Properties:{facing:'down'}}", "{Name:'minecraft:lime_shulker_box',Properties:{facing:'down'}}");
		putStates(3585, "{Name:'minecraft:lime_shulker_box',Properties:{facing:'up'}}", "{Name:'minecraft:lime_shulker_box',Properties:{facing:'up'}}");
		putStates(3586, "{Name:'minecraft:lime_shulker_box',Properties:{facing:'north'}}", "{Name:'minecraft:lime_shulker_box',Properties:{facing:'north'}}");
		putStates(3587, "{Name:'minecraft:lime_shulker_box',Properties:{facing:'south'}}", "{Name:'minecraft:lime_shulker_box',Properties:{facing:'south'}}");
		putStates(3588, "{Name:'minecraft:lime_shulker_box',Properties:{facing:'west'}}", "{Name:'minecraft:lime_shulker_box',Properties:{facing:'west'}}");
		putStates(3589, "{Name:'minecraft:lime_shulker_box',Properties:{facing:'east'}}", "{Name:'minecraft:lime_shulker_box',Properties:{facing:'east'}}");
		putStates(3600, "{Name:'minecraft:pink_shulker_box',Properties:{facing:'down'}}", "{Name:'minecraft:pink_shulker_box',Properties:{facing:'down'}}");
		putStates(3601, "{Name:'minecraft:pink_shulker_box',Properties:{facing:'up'}}", "{Name:'minecraft:pink_shulker_box',Properties:{facing:'up'}}");
		putStates(3602, "{Name:'minecraft:pink_shulker_box',Properties:{facing:'north'}}", "{Name:'minecraft:pink_shulker_box',Properties:{facing:'north'}}");
		putStates(3603, "{Name:'minecraft:pink_shulker_box',Properties:{facing:'south'}}", "{Name:'minecraft:pink_shulker_box',Properties:{facing:'south'}}");
		putStates(3604, "{Name:'minecraft:pink_shulker_box',Properties:{facing:'west'}}", "{Name:'minecraft:pink_shulker_box',Properties:{facing:'west'}}");
		putStates(3605, "{Name:'minecraft:pink_shulker_box',Properties:{facing:'east'}}", "{Name:'minecraft:pink_shulker_box',Properties:{facing:'east'}}");
		putStates(3616, "{Name:'minecraft:gray_shulker_box',Properties:{facing:'down'}}", "{Name:'minecraft:gray_shulker_box',Properties:{facing:'down'}}");
		putStates(3617, "{Name:'minecraft:gray_shulker_box',Properties:{facing:'up'}}", "{Name:'minecraft:gray_shulker_box',Properties:{facing:'up'}}");
		putStates(3618, "{Name:'minecraft:gray_shulker_box',Properties:{facing:'north'}}", "{Name:'minecraft:gray_shulker_box',Properties:{facing:'north'}}");
		putStates(3619, "{Name:'minecraft:gray_shulker_box',Properties:{facing:'south'}}", "{Name:'minecraft:gray_shulker_box',Properties:{facing:'south'}}");
		putStates(3620, "{Name:'minecraft:gray_shulker_box',Properties:{facing:'west'}}", "{Name:'minecraft:gray_shulker_box',Properties:{facing:'west'}}");
		putStates(3621, "{Name:'minecraft:gray_shulker_box',Properties:{facing:'east'}}", "{Name:'minecraft:gray_shulker_box',Properties:{facing:'east'}}");
		putStates(3632, "{Name:'minecraft:light_gray_shulker_box',Properties:{facing:'down'}}", "{Name:'minecraft:silver_shulker_box',Properties:{facing:'down'}}");
		putStates(3633, "{Name:'minecraft:light_gray_shulker_box',Properties:{facing:'up'}}", "{Name:'minecraft:silver_shulker_box',Properties:{facing:'up'}}");
		putStates(3634, "{Name:'minecraft:light_gray_shulker_box',Properties:{facing:'north'}}", "{Name:'minecraft:silver_shulker_box',Properties:{facing:'north'}}");
		putStates(3635, "{Name:'minecraft:light_gray_shulker_box',Properties:{facing:'south'}}", "{Name:'minecraft:silver_shulker_box',Properties:{facing:'south'}}");
		putStates(3636, "{Name:'minecraft:light_gray_shulker_box',Properties:{facing:'west'}}", "{Name:'minecraft:silver_shulker_box',Properties:{facing:'west'}}");
		putStates(3637, "{Name:'minecraft:light_gray_shulker_box',Properties:{facing:'east'}}", "{Name:'minecraft:silver_shulker_box',Properties:{facing:'east'}}");
		putStates(3648, "{Name:'minecraft:cyan_shulker_box',Properties:{facing:'down'}}", "{Name:'minecraft:cyan_shulker_box',Properties:{facing:'down'}}");
		putStates(3649, "{Name:'minecraft:cyan_shulker_box',Properties:{facing:'up'}}", "{Name:'minecraft:cyan_shulker_box',Properties:{facing:'up'}}");
		putStates(3650, "{Name:'minecraft:cyan_shulker_box',Properties:{facing:'north'}}", "{Name:'minecraft:cyan_shulker_box',Properties:{facing:'north'}}");
		putStates(3651, "{Name:'minecraft:cyan_shulker_box',Properties:{facing:'south'}}", "{Name:'minecraft:cyan_shulker_box',Properties:{facing:'south'}}");
		putStates(3652, "{Name:'minecraft:cyan_shulker_box',Properties:{facing:'west'}}", "{Name:'minecraft:cyan_shulker_box',Properties:{facing:'west'}}");
		putStates(3653, "{Name:'minecraft:cyan_shulker_box',Properties:{facing:'east'}}", "{Name:'minecraft:cyan_shulker_box',Properties:{facing:'east'}}");
		putStates(3664, "{Name:'minecraft:purple_shulker_box',Properties:{facing:'down'}}", "{Name:'minecraft:purple_shulker_box',Properties:{facing:'down'}}");
		putStates(3665, "{Name:'minecraft:purple_shulker_box',Properties:{facing:'up'}}", "{Name:'minecraft:purple_shulker_box',Properties:{facing:'up'}}");
		putStates(3666, "{Name:'minecraft:purple_shulker_box',Properties:{facing:'north'}}", "{Name:'minecraft:purple_shulker_box',Properties:{facing:'north'}}");
		putStates(3667, "{Name:'minecraft:purple_shulker_box',Properties:{facing:'south'}}", "{Name:'minecraft:purple_shulker_box',Properties:{facing:'south'}}");
		putStates(3668, "{Name:'minecraft:purple_shulker_box',Properties:{facing:'west'}}", "{Name:'minecraft:purple_shulker_box',Properties:{facing:'west'}}");
		putStates(3669, "{Name:'minecraft:purple_shulker_box',Properties:{facing:'east'}}", "{Name:'minecraft:purple_shulker_box',Properties:{facing:'east'}}");
		putStates(3680, "{Name:'minecraft:blue_shulker_box',Properties:{facing:'down'}}", "{Name:'minecraft:blue_shulker_box',Properties:{facing:'down'}}");
		putStates(3681, "{Name:'minecraft:blue_shulker_box',Properties:{facing:'up'}}", "{Name:'minecraft:blue_shulker_box',Properties:{facing:'up'}}");
		putStates(3682, "{Name:'minecraft:blue_shulker_box',Properties:{facing:'north'}}", "{Name:'minecraft:blue_shulker_box',Properties:{facing:'north'}}");
		putStates(3683, "{Name:'minecraft:blue_shulker_box',Properties:{facing:'south'}}", "{Name:'minecraft:blue_shulker_box',Properties:{facing:'south'}}");
		putStates(3684, "{Name:'minecraft:blue_shulker_box',Properties:{facing:'west'}}", "{Name:'minecraft:blue_shulker_box',Properties:{facing:'west'}}");
		putStates(3685, "{Name:'minecraft:blue_shulker_box',Properties:{facing:'east'}}", "{Name:'minecraft:blue_shulker_box',Properties:{facing:'east'}}");
		putStates(3696, "{Name:'minecraft:brown_shulker_box',Properties:{facing:'down'}}", "{Name:'minecraft:brown_shulker_box',Properties:{facing:'down'}}");
		putStates(3697, "{Name:'minecraft:brown_shulker_box',Properties:{facing:'up'}}", "{Name:'minecraft:brown_shulker_box',Properties:{facing:'up'}}");
		putStates(3698, "{Name:'minecraft:brown_shulker_box',Properties:{facing:'north'}}", "{Name:'minecraft:brown_shulker_box',Properties:{facing:'north'}}");
		putStates(3699, "{Name:'minecraft:brown_shulker_box',Properties:{facing:'south'}}", "{Name:'minecraft:brown_shulker_box',Properties:{facing:'south'}}");
		putStates(3700, "{Name:'minecraft:brown_shulker_box',Properties:{facing:'west'}}", "{Name:'minecraft:brown_shulker_box',Properties:{facing:'west'}}");
		putStates(3701, "{Name:'minecraft:brown_shulker_box',Properties:{facing:'east'}}", "{Name:'minecraft:brown_shulker_box',Properties:{facing:'east'}}");
		putStates(3712, "{Name:'minecraft:green_shulker_box',Properties:{facing:'down'}}", "{Name:'minecraft:green_shulker_box',Properties:{facing:'down'}}");
		putStates(3713, "{Name:'minecraft:green_shulker_box',Properties:{facing:'up'}}", "{Name:'minecraft:green_shulker_box',Properties:{facing:'up'}}");
		putStates(3714, "{Name:'minecraft:green_shulker_box',Properties:{facing:'north'}}", "{Name:'minecraft:green_shulker_box',Properties:{facing:'north'}}");
		putStates(3715, "{Name:'minecraft:green_shulker_box',Properties:{facing:'south'}}", "{Name:'minecraft:green_shulker_box',Properties:{facing:'south'}}");
		putStates(3716, "{Name:'minecraft:green_shulker_box',Properties:{facing:'west'}}", "{Name:'minecraft:green_shulker_box',Properties:{facing:'west'}}");
		putStates(3717, "{Name:'minecraft:green_shulker_box',Properties:{facing:'east'}}", "{Name:'minecraft:green_shulker_box',Properties:{facing:'east'}}");
		putStates(3728, "{Name:'minecraft:red_shulker_box',Properties:{facing:'down'}}", "{Name:'minecraft:red_shulker_box',Properties:{facing:'down'}}");
		putStates(3729, "{Name:'minecraft:red_shulker_box',Properties:{facing:'up'}}", "{Name:'minecraft:red_shulker_box',Properties:{facing:'up'}}");
		putStates(3730, "{Name:'minecraft:red_shulker_box',Properties:{facing:'north'}}", "{Name:'minecraft:red_shulker_box',Properties:{facing:'north'}}");
		putStates(3731, "{Name:'minecraft:red_shulker_box',Properties:{facing:'south'}}", "{Name:'minecraft:red_shulker_box',Properties:{facing:'south'}}");
		putStates(3732, "{Name:'minecraft:red_shulker_box',Properties:{facing:'west'}}", "{Name:'minecraft:red_shulker_box',Properties:{facing:'west'}}");
		putStates(3733, "{Name:'minecraft:red_shulker_box',Properties:{facing:'east'}}", "{Name:'minecraft:red_shulker_box',Properties:{facing:'east'}}");
		putStates(3744, "{Name:'minecraft:black_shulker_box',Properties:{facing:'down'}}", "{Name:'minecraft:black_shulker_box',Properties:{facing:'down'}}");
		putStates(3745, "{Name:'minecraft:black_shulker_box',Properties:{facing:'up'}}", "{Name:'minecraft:black_shulker_box',Properties:{facing:'up'}}");
		putStates(3746, "{Name:'minecraft:black_shulker_box',Properties:{facing:'north'}}", "{Name:'minecraft:black_shulker_box',Properties:{facing:'north'}}");
		putStates(3747, "{Name:'minecraft:black_shulker_box',Properties:{facing:'south'}}", "{Name:'minecraft:black_shulker_box',Properties:{facing:'south'}}");
		putStates(3748, "{Name:'minecraft:black_shulker_box',Properties:{facing:'west'}}", "{Name:'minecraft:black_shulker_box',Properties:{facing:'west'}}");
		putStates(3749, "{Name:'minecraft:black_shulker_box',Properties:{facing:'east'}}", "{Name:'minecraft:black_shulker_box',Properties:{facing:'east'}}");
		putStates(
			3760, "{Name:'minecraft:white_glazed_terracotta',Properties:{facing:'south'}}", "{Name:'minecraft:white_glazed_terracotta',Properties:{facing:'south'}}"
		);
		putStates(
			3761, "{Name:'minecraft:white_glazed_terracotta',Properties:{facing:'west'}}", "{Name:'minecraft:white_glazed_terracotta',Properties:{facing:'west'}}"
		);
		putStates(
			3762, "{Name:'minecraft:white_glazed_terracotta',Properties:{facing:'north'}}", "{Name:'minecraft:white_glazed_terracotta',Properties:{facing:'north'}}"
		);
		putStates(
			3763, "{Name:'minecraft:white_glazed_terracotta',Properties:{facing:'east'}}", "{Name:'minecraft:white_glazed_terracotta',Properties:{facing:'east'}}"
		);
		putStates(
			3776, "{Name:'minecraft:orange_glazed_terracotta',Properties:{facing:'south'}}", "{Name:'minecraft:orange_glazed_terracotta',Properties:{facing:'south'}}"
		);
		putStates(
			3777, "{Name:'minecraft:orange_glazed_terracotta',Properties:{facing:'west'}}", "{Name:'minecraft:orange_glazed_terracotta',Properties:{facing:'west'}}"
		);
		putStates(
			3778, "{Name:'minecraft:orange_glazed_terracotta',Properties:{facing:'north'}}", "{Name:'minecraft:orange_glazed_terracotta',Properties:{facing:'north'}}"
		);
		putStates(
			3779, "{Name:'minecraft:orange_glazed_terracotta',Properties:{facing:'east'}}", "{Name:'minecraft:orange_glazed_terracotta',Properties:{facing:'east'}}"
		);
		putStates(
			3792, "{Name:'minecraft:magenta_glazed_terracotta',Properties:{facing:'south'}}", "{Name:'minecraft:magenta_glazed_terracotta',Properties:{facing:'south'}}"
		);
		putStates(
			3793, "{Name:'minecraft:magenta_glazed_terracotta',Properties:{facing:'west'}}", "{Name:'minecraft:magenta_glazed_terracotta',Properties:{facing:'west'}}"
		);
		putStates(
			3794, "{Name:'minecraft:magenta_glazed_terracotta',Properties:{facing:'north'}}", "{Name:'minecraft:magenta_glazed_terracotta',Properties:{facing:'north'}}"
		);
		putStates(
			3795, "{Name:'minecraft:magenta_glazed_terracotta',Properties:{facing:'east'}}", "{Name:'minecraft:magenta_glazed_terracotta',Properties:{facing:'east'}}"
		);
		putStates(
			3808,
			"{Name:'minecraft:light_blue_glazed_terracotta',Properties:{facing:'south'}}",
			"{Name:'minecraft:light_blue_glazed_terracotta',Properties:{facing:'south'}}"
		);
		putStates(
			3809,
			"{Name:'minecraft:light_blue_glazed_terracotta',Properties:{facing:'west'}}",
			"{Name:'minecraft:light_blue_glazed_terracotta',Properties:{facing:'west'}}"
		);
		putStates(
			3810,
			"{Name:'minecraft:light_blue_glazed_terracotta',Properties:{facing:'north'}}",
			"{Name:'minecraft:light_blue_glazed_terracotta',Properties:{facing:'north'}}"
		);
		putStates(
			3811,
			"{Name:'minecraft:light_blue_glazed_terracotta',Properties:{facing:'east'}}",
			"{Name:'minecraft:light_blue_glazed_terracotta',Properties:{facing:'east'}}"
		);
		putStates(
			3824, "{Name:'minecraft:yellow_glazed_terracotta',Properties:{facing:'south'}}", "{Name:'minecraft:yellow_glazed_terracotta',Properties:{facing:'south'}}"
		);
		putStates(
			3825, "{Name:'minecraft:yellow_glazed_terracotta',Properties:{facing:'west'}}", "{Name:'minecraft:yellow_glazed_terracotta',Properties:{facing:'west'}}"
		);
		putStates(
			3826, "{Name:'minecraft:yellow_glazed_terracotta',Properties:{facing:'north'}}", "{Name:'minecraft:yellow_glazed_terracotta',Properties:{facing:'north'}}"
		);
		putStates(
			3827, "{Name:'minecraft:yellow_glazed_terracotta',Properties:{facing:'east'}}", "{Name:'minecraft:yellow_glazed_terracotta',Properties:{facing:'east'}}"
		);
	}

	/**
	 * Adds states to flatten from the blocks which had numeric IDs between 240 and 255 before 1.13.
	 */
	static void putStatesFromBlocks240To255() {
		putStates(
			3840, "{Name:'minecraft:lime_glazed_terracotta',Properties:{facing:'south'}}", "{Name:'minecraft:lime_glazed_terracotta',Properties:{facing:'south'}}"
		);
		putStates(
			3841, "{Name:'minecraft:lime_glazed_terracotta',Properties:{facing:'west'}}", "{Name:'minecraft:lime_glazed_terracotta',Properties:{facing:'west'}}"
		);
		putStates(
			3842, "{Name:'minecraft:lime_glazed_terracotta',Properties:{facing:'north'}}", "{Name:'minecraft:lime_glazed_terracotta',Properties:{facing:'north'}}"
		);
		putStates(
			3843, "{Name:'minecraft:lime_glazed_terracotta',Properties:{facing:'east'}}", "{Name:'minecraft:lime_glazed_terracotta',Properties:{facing:'east'}}"
		);
		putStates(
			3856, "{Name:'minecraft:pink_glazed_terracotta',Properties:{facing:'south'}}", "{Name:'minecraft:pink_glazed_terracotta',Properties:{facing:'south'}}"
		);
		putStates(
			3857, "{Name:'minecraft:pink_glazed_terracotta',Properties:{facing:'west'}}", "{Name:'minecraft:pink_glazed_terracotta',Properties:{facing:'west'}}"
		);
		putStates(
			3858, "{Name:'minecraft:pink_glazed_terracotta',Properties:{facing:'north'}}", "{Name:'minecraft:pink_glazed_terracotta',Properties:{facing:'north'}}"
		);
		putStates(
			3859, "{Name:'minecraft:pink_glazed_terracotta',Properties:{facing:'east'}}", "{Name:'minecraft:pink_glazed_terracotta',Properties:{facing:'east'}}"
		);
		putStates(
			3872, "{Name:'minecraft:gray_glazed_terracotta',Properties:{facing:'south'}}", "{Name:'minecraft:gray_glazed_terracotta',Properties:{facing:'south'}}"
		);
		putStates(
			3873, "{Name:'minecraft:gray_glazed_terracotta',Properties:{facing:'west'}}", "{Name:'minecraft:gray_glazed_terracotta',Properties:{facing:'west'}}"
		);
		putStates(
			3874, "{Name:'minecraft:gray_glazed_terracotta',Properties:{facing:'north'}}", "{Name:'minecraft:gray_glazed_terracotta',Properties:{facing:'north'}}"
		);
		putStates(
			3875, "{Name:'minecraft:gray_glazed_terracotta',Properties:{facing:'east'}}", "{Name:'minecraft:gray_glazed_terracotta',Properties:{facing:'east'}}"
		);
		putStates(
			3888,
			"{Name:'minecraft:light_gray_glazed_terracotta',Properties:{facing:'south'}}",
			"{Name:'minecraft:silver_glazed_terracotta',Properties:{facing:'south'}}"
		);
		putStates(
			3889, "{Name:'minecraft:light_gray_glazed_terracotta',Properties:{facing:'west'}}", "{Name:'minecraft:silver_glazed_terracotta',Properties:{facing:'west'}}"
		);
		putStates(
			3890,
			"{Name:'minecraft:light_gray_glazed_terracotta',Properties:{facing:'north'}}",
			"{Name:'minecraft:silver_glazed_terracotta',Properties:{facing:'north'}}"
		);
		putStates(
			3891, "{Name:'minecraft:light_gray_glazed_terracotta',Properties:{facing:'east'}}", "{Name:'minecraft:silver_glazed_terracotta',Properties:{facing:'east'}}"
		);
		putStates(
			3904, "{Name:'minecraft:cyan_glazed_terracotta',Properties:{facing:'south'}}", "{Name:'minecraft:cyan_glazed_terracotta',Properties:{facing:'south'}}"
		);
		putStates(
			3905, "{Name:'minecraft:cyan_glazed_terracotta',Properties:{facing:'west'}}", "{Name:'minecraft:cyan_glazed_terracotta',Properties:{facing:'west'}}"
		);
		putStates(
			3906, "{Name:'minecraft:cyan_glazed_terracotta',Properties:{facing:'north'}}", "{Name:'minecraft:cyan_glazed_terracotta',Properties:{facing:'north'}}"
		);
		putStates(
			3907, "{Name:'minecraft:cyan_glazed_terracotta',Properties:{facing:'east'}}", "{Name:'minecraft:cyan_glazed_terracotta',Properties:{facing:'east'}}"
		);
		putStates(
			3920, "{Name:'minecraft:purple_glazed_terracotta',Properties:{facing:'south'}}", "{Name:'minecraft:purple_glazed_terracotta',Properties:{facing:'south'}}"
		);
		putStates(
			3921, "{Name:'minecraft:purple_glazed_terracotta',Properties:{facing:'west'}}", "{Name:'minecraft:purple_glazed_terracotta',Properties:{facing:'west'}}"
		);
		putStates(
			3922, "{Name:'minecraft:purple_glazed_terracotta',Properties:{facing:'north'}}", "{Name:'minecraft:purple_glazed_terracotta',Properties:{facing:'north'}}"
		);
		putStates(
			3923, "{Name:'minecraft:purple_glazed_terracotta',Properties:{facing:'east'}}", "{Name:'minecraft:purple_glazed_terracotta',Properties:{facing:'east'}}"
		);
		putStates(
			3936, "{Name:'minecraft:blue_glazed_terracotta',Properties:{facing:'south'}}", "{Name:'minecraft:blue_glazed_terracotta',Properties:{facing:'south'}}"
		);
		putStates(
			3937, "{Name:'minecraft:blue_glazed_terracotta',Properties:{facing:'west'}}", "{Name:'minecraft:blue_glazed_terracotta',Properties:{facing:'west'}}"
		);
		putStates(
			3938, "{Name:'minecraft:blue_glazed_terracotta',Properties:{facing:'north'}}", "{Name:'minecraft:blue_glazed_terracotta',Properties:{facing:'north'}}"
		);
		putStates(
			3939, "{Name:'minecraft:blue_glazed_terracotta',Properties:{facing:'east'}}", "{Name:'minecraft:blue_glazed_terracotta',Properties:{facing:'east'}}"
		);
		putStates(
			3952, "{Name:'minecraft:brown_glazed_terracotta',Properties:{facing:'south'}}", "{Name:'minecraft:brown_glazed_terracotta',Properties:{facing:'south'}}"
		);
		putStates(
			3953, "{Name:'minecraft:brown_glazed_terracotta',Properties:{facing:'west'}}", "{Name:'minecraft:brown_glazed_terracotta',Properties:{facing:'west'}}"
		);
		putStates(
			3954, "{Name:'minecraft:brown_glazed_terracotta',Properties:{facing:'north'}}", "{Name:'minecraft:brown_glazed_terracotta',Properties:{facing:'north'}}"
		);
		putStates(
			3955, "{Name:'minecraft:brown_glazed_terracotta',Properties:{facing:'east'}}", "{Name:'minecraft:brown_glazed_terracotta',Properties:{facing:'east'}}"
		);
		putStates(
			3968, "{Name:'minecraft:green_glazed_terracotta',Properties:{facing:'south'}}", "{Name:'minecraft:green_glazed_terracotta',Properties:{facing:'south'}}"
		);
		putStates(
			3969, "{Name:'minecraft:green_glazed_terracotta',Properties:{facing:'west'}}", "{Name:'minecraft:green_glazed_terracotta',Properties:{facing:'west'}}"
		);
		putStates(
			3970, "{Name:'minecraft:green_glazed_terracotta',Properties:{facing:'north'}}", "{Name:'minecraft:green_glazed_terracotta',Properties:{facing:'north'}}"
		);
		putStates(
			3971, "{Name:'minecraft:green_glazed_terracotta',Properties:{facing:'east'}}", "{Name:'minecraft:green_glazed_terracotta',Properties:{facing:'east'}}"
		);
		putStates(
			3984, "{Name:'minecraft:red_glazed_terracotta',Properties:{facing:'south'}}", "{Name:'minecraft:red_glazed_terracotta',Properties:{facing:'south'}}"
		);
		putStates(3985, "{Name:'minecraft:red_glazed_terracotta',Properties:{facing:'west'}}", "{Name:'minecraft:red_glazed_terracotta',Properties:{facing:'west'}}");
		putStates(
			3986, "{Name:'minecraft:red_glazed_terracotta',Properties:{facing:'north'}}", "{Name:'minecraft:red_glazed_terracotta',Properties:{facing:'north'}}"
		);
		putStates(3987, "{Name:'minecraft:red_glazed_terracotta',Properties:{facing:'east'}}", "{Name:'minecraft:red_glazed_terracotta',Properties:{facing:'east'}}");
		putStates(
			4000, "{Name:'minecraft:black_glazed_terracotta',Properties:{facing:'south'}}", "{Name:'minecraft:black_glazed_terracotta',Properties:{facing:'south'}}"
		);
		putStates(
			4001, "{Name:'minecraft:black_glazed_terracotta',Properties:{facing:'west'}}", "{Name:'minecraft:black_glazed_terracotta',Properties:{facing:'west'}}"
		);
		putStates(
			4002, "{Name:'minecraft:black_glazed_terracotta',Properties:{facing:'north'}}", "{Name:'minecraft:black_glazed_terracotta',Properties:{facing:'north'}}"
		);
		putStates(
			4003, "{Name:'minecraft:black_glazed_terracotta',Properties:{facing:'east'}}", "{Name:'minecraft:black_glazed_terracotta',Properties:{facing:'east'}}"
		);
		putStates(4016, "{Name:'minecraft:white_concrete'}", "{Name:'minecraft:concrete',Properties:{color:'white'}}");
		putStates(4017, "{Name:'minecraft:orange_concrete'}", "{Name:'minecraft:concrete',Properties:{color:'orange'}}");
		putStates(4018, "{Name:'minecraft:magenta_concrete'}", "{Name:'minecraft:concrete',Properties:{color:'magenta'}}");
		putStates(4019, "{Name:'minecraft:light_blue_concrete'}", "{Name:'minecraft:concrete',Properties:{color:'light_blue'}}");
		putStates(4020, "{Name:'minecraft:yellow_concrete'}", "{Name:'minecraft:concrete',Properties:{color:'yellow'}}");
		putStates(4021, "{Name:'minecraft:lime_concrete'}", "{Name:'minecraft:concrete',Properties:{color:'lime'}}");
		putStates(4022, "{Name:'minecraft:pink_concrete'}", "{Name:'minecraft:concrete',Properties:{color:'pink'}}");
		putStates(4023, "{Name:'minecraft:gray_concrete'}", "{Name:'minecraft:concrete',Properties:{color:'gray'}}");
		putStates(4024, "{Name:'minecraft:light_gray_concrete'}", "{Name:'minecraft:concrete',Properties:{color:'silver'}}");
		putStates(4025, "{Name:'minecraft:cyan_concrete'}", "{Name:'minecraft:concrete',Properties:{color:'cyan'}}");
		putStates(4026, "{Name:'minecraft:purple_concrete'}", "{Name:'minecraft:concrete',Properties:{color:'purple'}}");
		putStates(4027, "{Name:'minecraft:blue_concrete'}", "{Name:'minecraft:concrete',Properties:{color:'blue'}}");
		putStates(4028, "{Name:'minecraft:brown_concrete'}", "{Name:'minecraft:concrete',Properties:{color:'brown'}}");
		putStates(4029, "{Name:'minecraft:green_concrete'}", "{Name:'minecraft:concrete',Properties:{color:'green'}}");
		putStates(4030, "{Name:'minecraft:red_concrete'}", "{Name:'minecraft:concrete',Properties:{color:'red'}}");
		putStates(4031, "{Name:'minecraft:black_concrete'}", "{Name:'minecraft:concrete',Properties:{color:'black'}}");
		putStates(4032, "{Name:'minecraft:white_concrete_powder'}", "{Name:'minecraft:concrete_powder',Properties:{color:'white'}}");
		putStates(4033, "{Name:'minecraft:orange_concrete_powder'}", "{Name:'minecraft:concrete_powder',Properties:{color:'orange'}}");
		putStates(4034, "{Name:'minecraft:magenta_concrete_powder'}", "{Name:'minecraft:concrete_powder',Properties:{color:'magenta'}}");
		putStates(4035, "{Name:'minecraft:light_blue_concrete_powder'}", "{Name:'minecraft:concrete_powder',Properties:{color:'light_blue'}}");
		putStates(4036, "{Name:'minecraft:yellow_concrete_powder'}", "{Name:'minecraft:concrete_powder',Properties:{color:'yellow'}}");
		putStates(4037, "{Name:'minecraft:lime_concrete_powder'}", "{Name:'minecraft:concrete_powder',Properties:{color:'lime'}}");
		putStates(4038, "{Name:'minecraft:pink_concrete_powder'}", "{Name:'minecraft:concrete_powder',Properties:{color:'pink'}}");
		putStates(4039, "{Name:'minecraft:gray_concrete_powder'}", "{Name:'minecraft:concrete_powder',Properties:{color:'gray'}}");
		putStates(4040, "{Name:'minecraft:light_gray_concrete_powder'}", "{Name:'minecraft:concrete_powder',Properties:{color:'silver'}}");
		putStates(4041, "{Name:'minecraft:cyan_concrete_powder'}", "{Name:'minecraft:concrete_powder',Properties:{color:'cyan'}}");
		putStates(4042, "{Name:'minecraft:purple_concrete_powder'}", "{Name:'minecraft:concrete_powder',Properties:{color:'purple'}}");
		putStates(4043, "{Name:'minecraft:blue_concrete_powder'}", "{Name:'minecraft:concrete_powder',Properties:{color:'blue'}}");
		putStates(4044, "{Name:'minecraft:brown_concrete_powder'}", "{Name:'minecraft:concrete_powder',Properties:{color:'brown'}}");
		putStates(4045, "{Name:'minecraft:green_concrete_powder'}", "{Name:'minecraft:concrete_powder',Properties:{color:'green'}}");
		putStates(4046, "{Name:'minecraft:red_concrete_powder'}", "{Name:'minecraft:concrete_powder',Properties:{color:'red'}}");
		putStates(4047, "{Name:'minecraft:black_concrete_powder'}", "{Name:'minecraft:concrete_powder',Properties:{color:'black'}}");
		putStates(4080, "{Name:'minecraft:structure_block',Properties:{mode:'save'}}", "{Name:'minecraft:structure_block',Properties:{mode:'save'}}");
		putStates(4081, "{Name:'minecraft:structure_block',Properties:{mode:'load'}}", "{Name:'minecraft:structure_block',Properties:{mode:'load'}}");
		putStates(4082, "{Name:'minecraft:structure_block',Properties:{mode:'corner'}}", "{Name:'minecraft:structure_block',Properties:{mode:'corner'}}");
		putStates(4083, "{Name:'minecraft:structure_block',Properties:{mode:'data'}}", "{Name:'minecraft:structure_block',Properties:{mode:'data'}}");
		fillEmptyStates();
	}

	static {
		OLD_STATE_TO_ID.defaultReturnValue(-1);
		putStatesFromBlocks0To15();
		putStatesFromBlocks16To31();
		putStatesFromBlocks32To47();
		putStatesFromBlocks48To63();
		putStatesFromBlocks64To79();
		putStatesFromBlocks80To95();
		putStatesFromBlocks96To111();
		putStatesFromBlocks112To127();
		putStatesFromBlocks128To143();
		putStatesFromBlocks144To159();
		putStatesFromBlocks160To175();
		putStatesFromBlocks176To191();
		putStatesFromBlocks192To207();
		putStatesFromBlocks208To223();
		putStatesFromBlocks224To239();
		putStatesFromBlocks240To255();
	}
}
