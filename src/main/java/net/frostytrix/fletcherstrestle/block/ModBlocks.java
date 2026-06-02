package net.frostytrix.fletcherstrestle.block;

import net.frostytrix.fletcherstrestle.FletcherTrestle;
import net.frostytrix.fletcherstrestle.block.custom.*;
import net.frostytrix.fletcherstrestle.item.ModItems;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
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

    // Arrow slit: solid cover with a firing port — arrows pass through, mobs/melee don't.
    public static final DeferredBlock<Block> ARROW_SLIT = registerBlock("arrow_slit",
            () -> new ArrowSlitBlock(BlockBehaviour.Properties.ofFullCopy(Blocks.STONE_BRICKS).noOcclusion()));

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
