package com.astraterra.mod.client;

import com.astraterra.mod.Astraterra;
import com.astraterra.mod.planet.Planet;
import com.astraterra.mod.planet.PlanetGravity;
import com.astraterra.mod.planet.Planets;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.world.entity.Entity;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * Client simulates items, falling blocks, etc. itself between server syncs,
 * so the same correction has to run here or they'd visibly fall at full speed.
 */
@Mod.EventBusSubscriber(modid = Astraterra.MODID, value = Dist.CLIENT)
public class PlanetGravityClient {

    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;

        Minecraft mc = Minecraft.getInstance();
        ClientLevel level = mc.level;
        if (level == null || mc.isPaused()) return;

        Planet planet = Planets.get(level);
        if (planet == null || planet.gravity() == 1.0) return;

        for (Entity entity : level.entitiesForRendering()) {
            PlanetGravity.compensate(entity, planet.gravity());
        }
    }
}
