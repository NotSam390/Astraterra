package com.astraterra.mod.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.DimensionSpecialEffects;
import net.minecraft.world.phys.Vec3;
import javax.annotation.ParametersAreNonnullByDefault;

@ParametersAreNonnullByDefault
public class MoonDimensionEffects extends DimensionSpecialEffects {
    public MoonDimensionEffects() {
        super(Float.NaN, false, SkyType.NONE, false, false);
    }
    @Override
    public Vec3 getBrightnessDependentFogColor(Vec3 color, float brightness) {
        return new Vec3(0.02, 0.02, 0.02);
    }
    @Override public boolean isFoggyAt(int x, int z) { return false; }
    @Override public float[] getSunriseColor(float t, float p) { return null; }

    @Override
    public boolean forceBrightLightmap() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null) return false;
        if (mc.player != null && mc.player.hasEffect(net.minecraft.world.effect.MobEffects.NIGHT_VISION)) return true;
        long gt = mc.level.getGameTime();
        double cp = (gt % 1200.0) / 1200.0;
        float h = (float)Math.sin(Math.toRadians((float)(cp*360)-90));
        return h > 0.2F; // was -0.05 -> now 0.2 (sun 11° up) = day stays bright longer, night darker
    }
    @Override
    public boolean constantAmbientLight() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null) return false;
        if (mc.player != null && mc.player.hasEffect(net.minecraft.world.effect.MobEffects.NIGHT_VISION)) return true;
        long gt = mc.level.getGameTime();
        double cp = (gt % 1200.0) / 1200.0;
        float h = (float)Math.sin(Math.toRadians((float)(cp*360)-90));
        return h > 0.2F;
    }
}