package com.astraterra.mod;

import com.mojang.logging.LogUtils;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import com.astraterra.mod.worldgen.structure.AsterraStructureTypes;
import org.slf4j.Logger;

@Mod(Astraterra.MODID)
public class Astraterra
{
    public static final String MODID = "astraterra";
    private static final Logger LOGGER = LogUtils.getLogger();

    public Astraterra(FMLJavaModLoadingContext context)
    {
        IEventBus modEventBus = context.getModEventBus();

        modEventBus.addListener(this::commonSetup);

        MinecraftForge.EVENT_BUS.register(this);

        LOGGER.info("Astraterra initializing...");
    }

    private void commonSetup(final FMLCommonSetupEvent event)
    {
        event.enqueueWork(AsterraStructureTypes::register);
        LOGGER.info("Astraterra common setup complete.");
    }
}