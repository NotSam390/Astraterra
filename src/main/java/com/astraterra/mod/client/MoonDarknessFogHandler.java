package com.astraterra.mod.client;

import com.astraterra.mod.Astraterra;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.LightLayer;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.ViewportEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * Replaces the MobEffects.DARKNESS approach: instead of a full-screen
 * post-process (which also swallowed the sky and bled into GUI screens),
 * this closes terrain fog in tight around the player at night unless
 * there's real block light nearby. Sky renders before fog is applied, so
 * stars stay untouched; GUI rendering is unrelated to world fog entirely.
 */
@Mod.EventBusSubscriber(modid = Astraterra.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE, value = Dist.CLIENT)
public class MoonDarknessFogHandler {

    private static final ResourceLocation MOON_DIMENSION = new ResourceLocation(Astraterra.MODID, "moon");
    private static final float DARK_FOG_START = 0.5F;
    private static final float DARK_FOG_END = 3.5F;   // visibility radius w/ no light
    private static final int LIGHT_THRESHOLD = 4;      // min nearby block light to "count"

    @SubscribeEvent
    public static void onComputeFogColor(ViewportEvent.ComputeFogColor event) {
        if (!inDarkness(event.getCamera())) return;
        event.setRed(0.0F);
        event.setGreen(0.0F);
        event.setBlue(0.0F);
    }

    @SubscribeEvent
    public static void onRenderFog(ViewportEvent.RenderFog event) {
        if (!inDarkness(event.getCamera())) return;
        event.setNearPlaneDistance(DARK_FOG_START);
        event.setFarPlaneDistance(DARK_FOG_END);
        event.setCanceled(true);
    }

    private static boolean inDarkness(Camera camera) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null) return false;
        if (!mc.level.dimension().location().equals(MOON_DIMENSION)) return false;
        if (!MoonSkyRenderer.isNight(mc.level.getGameTime(), mc.getDeltaFrameTime())) return false;

        BlockPos pos = camera.getBlockPosition();
        return mc.level.getBrightness(LightLayer.BLOCK, pos) < LIGHT_THRESHOLD;
    }
}