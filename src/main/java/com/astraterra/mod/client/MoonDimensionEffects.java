package com.astraterra.mod.client;

import net.minecraft.client.renderer.DimensionSpecialEffects;
import net.minecraft.world.phys.Vec3;

/**
 * Custom sky "effects" for the moon dimension.
 *
 * The actual black sky/fog COLOR is controlled by the biome json
 * (data/astraterra/worldgen/biome/moon.json -> effects.sky_color / fog_color),
 * not by this class — that's pure data, no code needed for the color itself.
 *
 * What this class DOES do is set skyType to NONE, which tells vanilla's
 * LevelRenderer to skip its own sky dome, sun, moon, vanilla stars, and clouds
 * entirely for this dimension. MoonSkyRenderer then draws everything from
 * scratch via RenderLevelStageEvent — custom stars, a custom sun, and Earth.
 *
 * Registered in ClientSetup and referenced by the dimension_type json via
 * "effects": "astraterra:moon".
 */
public class MoonDimensionEffects extends DimensionSpecialEffects {

    public MoonDimensionEffects() {
        super(
                Float.NaN,          // cloudLevel — irrelevant, we render no clouds
                false,               // hasGround — real terrain, not an End-style void floor plane
                SkyType.NONE,        // skyType — vanilla sky rendering fully disabled
                false,               // forceBrightLightmap
                false                // constantAmbientLight
        );
    }

    @Override
    public Vec3 getBrightnessDependentFogColor(Vec3 color, float brightness) {
        // Biome json already supplies black fog color, and there's no atmosphere
        // to scatter light — don't darken/tint it further based on brightness.
        return color;
    }

    @Override
    public boolean isFoggyAt(int x, int z) {
        // Vacuum — no distance-based fog cutoff, full render distance visibility.
        return false;
    }

    @Override
    public float[] getSunriseColor(float timeOfDay, float partialTicks) {
        // Tt here still produces a dawn/dusk glow color
        // independent of she base class's defaulkyType — that's the red-west/yellow-east horizon
        // glow that was showing up even with vanilla sky rendering fully
        // disabled. The moon has no atmosphere, so there's nothing to
        // scatter light into a sunrise/sunset gradient in the first place.
        return null;
    }
}