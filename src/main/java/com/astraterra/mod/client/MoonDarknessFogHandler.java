package com.astraterra.mod.client;

import com.astraterra.mod.Astraterra;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.ViewportEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = Astraterra.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE, value = Dist.CLIENT)
@SuppressWarnings({"unused", "removal", "deprecation"})
public class MoonDarknessFogHandler {
    private static final ResourceLocation MOON_DIMENSION = new ResourceLocation("astraterra:moon");
    private static final float DARK_FOG_START = 0.5F;
    private static final float DARK_FOG_END = 3.5F;
    private static final int LIGHT_THRESHOLD = 4;


    @SubscribeEvent
    public static void onComputeFogColor(ViewportEvent.ComputeFogColor event) { return; }
    @SubscribeEvent
    public static void onRenderFog(ViewportEvent.RenderFog event) { return; }
    @SubscribeEvent
    public static void onClientTick(net.minecraftforge.event.TickEvent.ClientTickEvent event) {
        if (event.phase != net.minecraftforge.event.TickEvent.Phase.END) return;
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null || mc.player == null) return;
        if (!mc.level.dimension().location().equals(MOON_DIMENSION)) return;
        if (mc.player.hasEffect(net.minecraft.world.effect.MobEffects.NIGHT_VISION)) return;
        long gt = mc.level.getGameTime();
        double cp = (gt % 1200.0) / 1200.0;
        float h = (float)Math.sin(Math.toRadians((float)(cp*360)-90));
        float t = net.minecraft.util.Mth.clamp((0.5F - h) / 0.8F, 0.0F, 1.0F);
        t = t * t * (3 - 2 * t);
        if (t > 0.05F) {
            mc.player.addEffect(new net.minecraft.world.effect.MobEffectInstance(net.minecraft.world.effect.MobEffects.DARKNESS, 30, (int)(t*2), false, false, false));
        }
    }
    @SubscribeEvent
    public static void onRenderGui(net.minecraftforge.client.event.RenderGuiEvent.Post event) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null || !mc.level.dimension().location().equals(MOON_DIMENSION)) return;
        if (mc.player != null && mc.player.hasEffect(net.minecraft.world.effect.MobEffects.NIGHT_VISION)) return;
        long gt = mc.level.getGameTime();
        double cp = (gt % 1200.0) / 1200.0;
        float h = (float)Math.sin(Math.toRadians((float)(cp*360)-90));
        float t = net.minecraft.util.Mth.clamp((0.3F - h) / 0.6F, 0.0F, 1.0F);
        t = t * t * (3 - 2 * t);
        if (t < 0.01F) return;
        int alpha = (int)(t * 255);
        int color = (alpha << 24) | 0x000000;
        event.getGuiGraphics().fill(0, 0, mc.getWindow().getGuiScaledWidth(), mc.getWindow().getGuiScaledHeight(), color);
    }
    private static float getNightFactor(Camera camera) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null) return 0;
        if (!mc.level.dimension().location().equals(MOON_DIMENSION)) return 0;
        long gt = mc.level.getGameTime();
        double cp = (gt % 1200.0) / 1200.0;
        float h = (float)Math.sin(Math.toRadians((float)(cp*360)-90));
        float t = net.minecraft.util.Mth.clamp((0.5F - h) / 0.4F, 0.0F, 1.0F);
        t = t * t * (3 - 2 * t);
        if (t < 0.01F) return 0;
        if (mc.level.getBrightness(net.minecraft.world.level.LightLayer.BLOCK, camera.getBlockPosition()) >= 4) return 0;
        return t;
    }
    private static boolean inDarkness(Camera camera) {
        return getNightFactor(camera) > 0.5F;
    }
}