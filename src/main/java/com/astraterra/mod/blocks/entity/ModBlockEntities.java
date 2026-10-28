package com.astraterra.mod.blocks.entity;

import com.astraterra.mod.Astraterra;
import com.astraterra.mod.blocks.ModBlocks;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public class ModBlockEntities {
    public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES =
            DeferredRegister.create(ForgeRegistries.BLOCK_ENTITY_TYPES, Astraterra.MODID);

    // One block entity type shared by both parts of the station
    public static final RegistryObject<BlockEntityType<OxygenStationBlockEntity>> OXYGEN_STATION =
            BLOCK_ENTITIES.register("oxygen_station", () ->
                    BlockEntityType.Builder.of(OxygenStationBlockEntity::new,
                            ModBlocks.OXYGEN_STATION.get(),
                            ModBlocks.OXYGEN_STATION_TANK.get()).build(null));

    public static void register(IEventBus eventBus) {
        BLOCK_ENTITIES.register(eventBus);
    }
}