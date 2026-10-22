package com.astraterra.mod.client;

import net.minecraft.client.renderer.DimensionSpecialEffects;
import net.minecraft.world.phys.Vec3;

import javax.annotation.ParametersAreNonnullByDefault;

/**
 * Client-side look of the Moon dimension.
 *
 * forceBrightLightmap and constantAmbientLight are passed to the constructor as
 * plain "false" and are never overridden with time-dependent logic.
 * constantAmbientLight changes how block faces are shaded, and that shading is
 * baked into chunk meshes when they are built. If the value changes over time,
 * chunks built at different moments get different shading (hard brightness
 * borders between chunks).
 *
 * Day/night brightness comes from real world lighting (ambient_light 0.0 in
 * dimension_type/moon.json plus vanilla skylight), not from tricks in here.
 */
@ParametersAreNonnullByDefault
public class MoonDimensionEffects extends DimensionSpecialEffects {

    public MoonDimensionEffects() {
        // cloudLevel NaN = no clouds, hasGround false, no vanilla sky dome,
        // forceBrightLightmap false, constantAmbientLight false
        super(Float.NaN, false, SkyType.NONE, false, false);
    }

    @Override
    public Vec3 getBrightnessDependentFogColor(Vec3 color, float brightness) {
        // No atmosphere: the background behind the stars is (almost) black.
        return new Vec3(0.0, 0.0, 0.0);
    }

    @Override
    public boolean isFoggyAt(int x, int z) {
        return false;
    }

    @Override
    public float[] getSunriseColor(float timeOfDay, float partialTicks) {
        // No atmosphere, so no orange sunrise glow.
        return null;
    }
}