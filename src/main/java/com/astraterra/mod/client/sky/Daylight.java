package com.astraterra.mod.client.sky;

import net.minecraft.util.Mth;

/**
 * The one place that decides how "night" it is on an airless planet.
 *
 * nightFactor: used by the lightmap and Earth. 0.0 = full day (sun about 3
 * degrees above the horizon), 1.0 = full night (sun about 6 degrees below).
 *
 * starFactor: used only by the stars, so they are mostly in by the time the
 * sun touches the horizon. 0.0 = sun about 7 degrees above the horizon,
 * 1.0 = sun about 5 degrees below.
 */
public final class Daylight {

    private Daylight() {}

    public static float nightFactor(float sunAngle) {
        float sunHeight = Mth.cos(sunAngle); // +1 noon, 0 horizon, -1 midnight
        float n = Mth.clamp((0.05F - sunHeight) / 0.15F, 0.0F, 1.0F);
        return n * n * (3.0F - 2.0F * n);
    }

    public static float starFactor(float sunAngle) {
        float sunHeight = Mth.cos(sunAngle);
        return Mth.clamp((0.12F - sunHeight) / 0.2F, 0.0F, 1.0F);
    }
}