package com.astraterra.mod.client.sky;

import com.astraterra.mod.planet.Planet;
import net.minecraft.client.renderer.DimensionSpecialEffects;
import net.minecraft.world.phys.Vec3;

import javax.annotation.ParametersAreNonnullByDefault;

/**
 * Client-side look of a planet dimension. One instance per planet is
 * registered in ClientSetup under the planet's id, which is what the
 * dimension_type JSON's "effects" field points at.
 *
 * Planets with a custom Sky get a black, cloudless, sunrise-less sky (the
 * stars/sun/bodies are drawn by PlanetSkyRenderer). Planets without one keep
 * a normal Overworld-like sky.
 *
 * forceBrightLightmap and constantAmbientLight are passed to the constructor as
 * plain "false" and are never overridden with time-dependent logic.
 * constantAmbientLight changes how block faces are shaded, and that shading is
 * baked into chunk meshes when they are built. If the value changes over time,
 * chunks built at different moments get different shading (hard brightness
 * borders between chunks).
 */
@ParametersAreNonnullByDefault
public class PlanetDimensionEffects extends DimensionSpecialEffects {

    private final Planet planet;
    private final boolean space;

    public PlanetDimensionEffects(Planet planet) {
        // Space sky: cloudLevel NaN = no clouds, no vanilla sky dome.
        super(planet.sky() != null ? Float.NaN : 192.0F, planet.sky() == null,
                planet.sky() != null ? SkyType.NONE : SkyType.NORMAL, false, false);
        this.planet = planet;
        this.space = planet.sky() != null;
    }

    public Planet planet() {
        return planet;
    }

    @Override
    public Vec3 getBrightnessDependentFogColor(Vec3 color, float brightness) {
        if (space) return Vec3.ZERO; // no atmosphere: black behind the stars
        return color.multiply(brightness * 0.94F + 0.06F, brightness * 0.94F + 0.06F, brightness * 0.91F + 0.09F);
    }

    @Override
    public boolean isFoggyAt(int x, int z) {
        return false;
    }

    @Override
    public float[] getSunriseColor(float timeOfDay, float partialTicks) {
        return space ? null : super.getSunriseColor(timeOfDay, partialTicks);
    }
}
