package net.frostytrix.fletcherstrestle.block;

import net.frostytrix.fletcherstrestle.FletcherTrestle;
import net.frostytrix.fletcherstrestle.block.custom.ArcheryTargetBlock;
import net.frostytrix.fletcherstrestle.block.custom.FlaxCropBlock;
import net.frostytrix.fletcherstrestle.block.custom.ShavingHorseBlock;
import net.frostytrix.fletcherstrestle.block.custom.SteamBoxBlock;
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

    private static <T extends Block> DeferredBlock<T> registerBlock(String name, Supplier<T> block){
        DeferredBlock<T> toReturn = BLOCKS.register(name, block);
        registerBlockItem(name, toReturn);
        return toReturn;
    }


    private static <T extends Block> void registerBlockItem(String name, DeferredBlock<T> block){
        ModItems.ITEMS.register(name, () -> new BlockItem(block.get(), new Item.Properties()));
    }

    public static void register(IEventBus eventBus) {
        BLOCKS.register(eventBus);
    }
}
