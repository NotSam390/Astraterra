package com.astraterra.mod;

import com.astraterra.mod.registry.ModBlockEntities;
import com.astraterra.mod.registry.ModBlocks;
import com.astraterra.mod.registry.ModCreativeTabs;
import com.astraterra.mod.registry.ModItems;
import com.astraterra.mod.registry.ModMenus;
import com.astraterra.mod.registry.ModStructures;
import com.mojang.logging.LogUtils;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import org.slf4j.Logger;

/**
 * Entry point. Only wires the registers to the mod event bus; everything
 * else listens for its own events (@Mod.EventBusSubscriber classes).
 */
@Mod(Astraterra.MODID)
public class Astraterra {
    public static final String MODID = "astraterra";
    private static final Logger LOGGER = LogUtils.getLogger();

    public Astraterra(FMLJavaModLoadingContext context) {
        IEventBus modEventBus = context.getModEventBus();

        ModBlocks.register(modEventBus);
        ModItems.register(modEventBus);
        ModBlockEntities.register(modEventBus);
        ModMenus.register(modEventBus);
        ModCreativeTabs.register(modEventBus);
        ModStructures.register(modEventBus);

        LOGGER.info("Astraterra initializing...");
    }
}
