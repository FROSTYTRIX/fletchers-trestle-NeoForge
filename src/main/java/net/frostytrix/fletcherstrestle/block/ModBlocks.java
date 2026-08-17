package net.frostytrix.fletcherstrestle.block;

import net.frostytrix.fletcherstrestle.FletcherTrestle;
import net.frostytrix.fletcherstrestle.block.custom.*;
import net.frostytrix.fletcherstrestle.item.ModItems;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.frostytrix.fletcherstrestle.block.custom.NailBlock;
import net.frostytrix.fletcherstrestle.block.custom.WeaponRackBlock;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.CarpetBlock;
import net.minecraft.world.level.block.SlabBlock;
import net.minecraft.world.level.block.StairBlock;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.function.Supplier;

public class ModBlocks {
    public static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(FletcherTrestle.MOD_ID);


    public static final DeferredBlock<Block> STEAM_BOX = registerBlock("steam_box",
            () -> new SteamBoxBlock(BlockBehaviour.Properties.of().sound(SoundType.WOOD).noOcclusion()));

    public static final DeferredBlock<Block> SHAVING_HORSE = registerBlock("shaving_horse",
            () -> new ShavingHorseBlock(Block.Properties.of()
                    .strength(2.0f)
                    .sound(SoundType.WOOD)
                    .noOcclusion()));

    public static final DeferredBlock<Block> FLAX_CROP = registerBlock("flax_crop",
            () -> new FlaxCropBlock(BlockBehaviour.Properties.ofFullCopy(Blocks.WHEAT).noOcclusion().noCollission()));

    public static final DeferredBlock<Block> ARCHERY_TARGET = registerBlock("archery_target",
            () -> new ArcheryTargetBlock(Block.Properties.of()
                    .strength(1.0f)
                    .sound(SoundType.WOOD)
                    .noOcclusion()));

    public static final DeferredBlock<Block> ROPE = registerBlock("rope",
            () -> new RopeBlock(Block.Properties.of()
                    .noCollission()
                    .sound(SoundType.WOOL)
                    .noOcclusion()
                    .strength(0.6f)));

    public static final DeferredBlock<Block> DIPPING_VAT = registerBlock("dipping_vat",
            () -> new DippingVatBlock(Block.Properties.of().sound(SoundType.WOOD).noOcclusion()));

    public static final DeferredBlock<Block> EAGLE_PERCH = registerBlock("eagle_perch",
            () -> new EaglePerchBlock(Block.Properties.of()
                    .strength(1.5f)
                    .sound(SoundType.WOOD)
                    .noOcclusion()));

    public static final DeferredBlock<Block> EAGLE_NEST = registerBlock("eagle_nest",
            () -> new EagleNestBlock(Block.Properties.of()
                    .strength(0.8f)
                    .sound(SoundType.GRASS)
                    .noOcclusion()));

    public static final DeferredBlock<Block> CROSSBOW_BENCH = registerBlock("crossbow_bench",
            () -> new CrossbowBenchBlock(Block.Properties.of()
                    .strength(2.5f)
                    .sound(SoundType.WOOD)
                    .noOcclusion()));

    // Arrow slit: solid cover with a firing port: arrows pass through, mobs/melee don't.
    public static final DeferredBlock<Block> ARROW_SLIT = registerBlock("arrow_slit",
            () -> new ArrowSlitBlock(BlockBehaviour.Properties.ofFullCopy(Blocks.STONE_BRICKS).noOcclusion()));

    // Wall-mounted display rack for a modular bow or crossbow. The weapon is
    // drawn by its block-entity renderer, so it shows its real assembly.
    public static final DeferredBlock<Block> WEAPON_RACK = registerBlock("weapon_rack",
            () -> new WeaponRackBlock(BlockBehaviour.Properties.ofFullCopy(Blocks.OAK_PLANKS).noOcclusion()));

    // A nail driven into a block face. Anchors a garland; see NailBlock.
    public static final DeferredBlock<Block> NAIL = registerBlock("nail",
            () -> new NailBlock(BlockBehaviour.Properties.ofFullCopy(Blocks.IRON_BARS).noOcclusion().strength(0.4f)));

    // ---------------------------------------------------------------
    // Linen: the decorative side of flax. Woven from flax string, so a flax
    // farm feeds the workshop's furnishings as well as its bowstrings.
    //
    // Wool-like (soft, flammable) and dyeable into all 16 colours, each with
    // its own texture (terracotta-style, not tinted). Every colour ships a
    // full building set: block, stairs, slab and carpet.
    // ---------------------------------------------------------------

    /** The four blocks that make up one linen colour. */
    public record LinenSet(DeferredBlock<Block> block,
                           DeferredBlock<StairBlock> stairs,
                           DeferredBlock<SlabBlock> slab,
                           DeferredBlock<Block> carpet) {
    }

    /** Undyed linen: the natural flax colour. */
    public static final LinenSet LINEN = registerLinenSet("linen");

    /** One set per vanilla dye colour, e.g. {@code red_linen}, {@code red_linen_stairs}. */
    public static final Map<DyeColor, LinenSet> DYED_LINEN = registerDyedLinen();

    /** Every linen set, undyed first: handy for datagen and creative-tab loops. */
    public static List<LinenSet> allLinenSets() {
        List<LinenSet> all = new ArrayList<>();
        all.add(LINEN);
        for (DyeColor colour : DyeColor.values()) {
            all.add(DYED_LINEN.get(colour));
        }
        return all;
    }

    private static Map<DyeColor, LinenSet> registerDyedLinen() {
        Map<DyeColor, LinenSet> map = new EnumMap<>(DyeColor.class);
        for (DyeColor colour : DyeColor.values()) {
            map.put(colour, registerLinenSet(colour.getName() + "_linen"));
        }
        return map;
    }

    private static LinenSet registerLinenSet(String baseName) {
        DeferredBlock<Block> block = registerBlock(baseName,
                () -> new Block(BlockBehaviour.Properties.ofFullCopy(Blocks.WHITE_WOOL)));
        DeferredBlock<StairBlock> stairs = registerBlock(baseName + "_stairs",
                () -> new StairBlock(block.get().defaultBlockState(),
                        BlockBehaviour.Properties.ofFullCopy(Blocks.WHITE_WOOL)));
        DeferredBlock<SlabBlock> slab = registerBlock(baseName + "_slab",
                () -> new SlabBlock(BlockBehaviour.Properties.ofFullCopy(Blocks.WHITE_WOOL)));
        DeferredBlock<Block> carpet = registerBlock(baseName + "_carpet",
                () -> new CarpetBlock(BlockBehaviour.Properties.ofFullCopy(Blocks.WHITE_CARPET)));
        return new LinenSet(block, stairs, slab, carpet);
    }

    private static <T extends Block> DeferredBlock<T> registerBlock(String name, Supplier<T> block) {
        DeferredBlock<T> toReturn = BLOCKS.register(name, block);
        registerBlockItem(name, toReturn);
        return toReturn;
    }


    private static <T extends Block> void registerBlockItem(String name, DeferredBlock<T> block) {
        ModItems.ITEMS.register(name, () -> new BlockItem(block.get(), new Item.Properties()));
    }

    public static void register(IEventBus eventBus) {
        BLOCKS.register(eventBus);
    }
}
