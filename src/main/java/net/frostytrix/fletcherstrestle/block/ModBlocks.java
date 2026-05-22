package net.frostytrix.fletcherstrestle.block;

import net.frostytrix.fletcherstrestle.FletcherTrestle;
import net.frostytrix.fletcherstrestle.block.custom.*;
import net.frostytrix.fletcherstrestle.item.ModItems;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredRegister;

// 26.1: BlockBehaviour.Properties must have its id set before the Block
// constructor runs (effectiveDrops() does a requireNonNull on the id).
// DeferredRegister.Blocks.registerBlock(name, factory, propsSupplier) is
// the ergonomic path — the helper bakes the id into the Properties for
// you before invoking the factory. Same story for items below (see
// ModItems.registerItem).
public class ModBlocks {
    public static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(FletcherTrestle.MOD_ID);

    public static final DeferredBlock<SteamBoxBlock> STEAM_BOX = BLOCKS.registerBlock("steam_box",
            SteamBoxBlock::new,
            () -> BlockBehaviour.Properties.of().sound(SoundType.WOOD).noOcclusion());

    public static final DeferredBlock<ShavingHorseBlock> SHAVING_HORSE = BLOCKS.registerBlock("shaving_horse",
            ShavingHorseBlock::new,
            () -> BlockBehaviour.Properties.of()
                    .strength(2.0f)
                    .sound(SoundType.WOOD)
                    .noOcclusion());

    // 26.1: Properties.ofFullCopy(Blocks.WHEAT) copies vanilla wheat's light-
    // emission lambda, which reads CropBlock.AGE (0–7) directly from the state.
    // Our FlaxCropBlock uses its own 0–8 age property, so that lambda explodes
    // during state construction. Build fresh Properties with crop-equivalent
    // settings instead of copying from WHEAT.
    public static final DeferredBlock<FlaxCropBlock> FLAX_CROP = BLOCKS.registerBlock("flax_crop",
            FlaxCropBlock::new,
            () -> BlockBehaviour.Properties.of()
                    .mapColor(net.minecraft.world.level.material.MapColor.PLANT)
                    .noCollision()
                    .randomTicks()
                    .instabreak()
                    .sound(SoundType.CROP)
                    .pushReaction(net.minecraft.world.level.material.PushReaction.DESTROY)
                    .noOcclusion());

    public static final DeferredBlock<ArcheryTargetBlock> ARCHERY_TARGET = BLOCKS.registerBlock("archery_target",
            ArcheryTargetBlock::new,
            () -> BlockBehaviour.Properties.of()
                    .strength(1.0f)
                    .sound(SoundType.WOOD)
                    .noOcclusion());

    public static final DeferredBlock<RopeBlock> ROPE = BLOCKS.registerBlock("rope",
            RopeBlock::new,
            () -> BlockBehaviour.Properties.of()
                    .noCollision()
                    .sound(SoundType.WOOL)
                    .noOcclusion()
                    .strength(0.6f));

    public static final DeferredBlock<DippingVatBlock> DIPPING_VAT = BLOCKS.registerBlock("dipping_vat",
            DippingVatBlock::new,
            () -> BlockBehaviour.Properties.of().sound(SoundType.WOOD).noOcclusion());

    public static final DeferredBlock<EaglePerchBlock> EAGLE_PERCH = BLOCKS.registerBlock("eagle_perch",
            EaglePerchBlock::new,
            () -> BlockBehaviour.Properties.of()
                    .strength(1.5f)
                    .sound(SoundType.WOOD)
                    .noOcclusion());

    public static final DeferredBlock<EagleNestBlock> EAGLE_NEST = BLOCKS.registerBlock("eagle_nest",
            EagleNestBlock::new,
            () -> BlockBehaviour.Properties.of()
                    .strength(0.8f)
                    .sound(SoundType.GRASS)
                    .noOcclusion());

    // Register matching BlockItems for everything except FLAX_CROP (its
    // item is FLAX_SEEDS, registered explicitly in ModItems).
    static {
        registerBlockItem("steam_box", STEAM_BOX);
        registerBlockItem("shaving_horse", SHAVING_HORSE);
        registerBlockItem("archery_target", ARCHERY_TARGET);
        registerBlockItem("rope", ROPE);
        registerBlockItem("dipping_vat", DIPPING_VAT);
        registerBlockItem("eagle_perch", EAGLE_PERCH);
        registerBlockItem("eagle_nest", EAGLE_NEST);
    }

    private static <T extends Block> void registerBlockItem(String name, DeferredBlock<T> block) {
        // 26.1: registerSimpleBlockItem(holder) makes a BlockItem with the id
        // matching the block, no manual Properties juggling needed.
        ModItems.ITEMS.registerSimpleBlockItem(block);
    }

    public static void register(IEventBus eventBus) {
        BLOCKS.register(eventBus);
    }
}
