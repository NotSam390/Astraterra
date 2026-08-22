package com.astraterra.mod.client;

import com.astraterra.mod.Astraterra;
import com.mojang.logging.LogUtils;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RegisterDimensionSpecialEffectsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.slf4j.Logger;

/**
 * Client-only setup. DimensionSpecialEffects.EFFECTS is private, so custom
 * sky effects can't be registered by writing to it directly — Forge exposes
 * RegisterDimensionSpecialEffectsEvent specifically for this instead.
 */
@Mod.EventBusSubscriber(modid = Astraterra.MODID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public class ClientSetup {

    private static final Logger LOGGER = LogUtils.getLogger();

    @SubscribeEvent
    public static void onRegisterDimensionEffects(RegisterDimensionSpecialEffectsEvent event) {
        ResourceLocation id = new ResourceLocation(Astraterra.MODID, "moon");
        event.register(id, new MoonDimensionEffects());
        LOGGER.info("[ASTRATERRA_SKY] Registered custom dimension effects for {}", id);
    }
}