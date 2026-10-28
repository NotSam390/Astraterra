package com.astraterra.mod.registry;

import com.astraterra.mod.Astraterra;
import com.astraterra.mod.block.OxygenStationBlock;
import com.astraterra.mod.block.OxygenStationTankBlock;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.SandBlock;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;
import java.util.function.Supplier;

public class ModBlocks {
    public static final DeferredRegister<Block> BLOCKS =
            DeferredRegister.create(ForgeRegistries.BLOCKS, Astraterra.MODID);

    public static final RegistryObject<Block> MOON_SAND = registerBlock("moon_sand",
            () -> new SandBlock(0x60606c, BlockBehaviour.Properties.copy(Blocks.SAND).sound(SoundType.SAND)));

    public static final RegistryObject<Block> MOON_ROCK = registerBlock("moon_rock",
            () -> new Block(BlockBehaviour.Properties.copy(Blocks.STONE).sound(SoundType.STONE)));

    public static final RegistryObject<Block> CRATER_ROCK = registerBlock("crater_rock",
            () -> new Block(BlockBehaviour.Properties.copy(Blocks.STONE).sound(SoundType.STONE)));

    // Oxygen station multiblock: base (bottom) + tank (top)
    public static final RegistryObject<Block> OXYGEN_STATION = registerBlock("oxygen_station",
            () -> new OxygenStationBlock(BlockBehaviour.Properties.copy(Blocks.IRON_BLOCK).noOcclusion()));

    public static final RegistryObject<Block> OXYGEN_STATION_TANK = registerBlock("oxygen_station_tank",
            () -> new OxygenStationTankBlock(BlockBehaviour.Properties.copy(Blocks.IRON_BLOCK).noOcclusion()));

    private static <T extends Block> RegistryObject<T> registerBlock(String name, Supplier<T> block) {
        RegistryObject<T> toReturn = BLOCKS.register(name, block);
        registerBlockItem(name, toReturn);
        return toReturn;
    }

    private static <T extends Block> RegistryObject<Item> registerBlockItem(String name, RegistryObject<T> block) {
        return ModItems.ITEMS.register(name, () -> new BlockItem(block.get(), new Item.Properties()));
    }

    public static void register(IEventBus eventBus) {
        BLOCKS.register(eventBus);
    }
}