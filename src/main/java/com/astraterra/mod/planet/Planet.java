package com.astraterra.mod.planet;

import com.astraterra.mod.Astraterra;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.Level;

import java.util.ArrayList;
import java.util.List;

/**
 * Everything the Java side needs to know about one planet.
 *
 * The world itself (terrain, biome, dimension type) lives in JSON under
 * data/astraterra/. This object only holds the things JSON can't express:
 * gravity, whether you can breathe, and how the sky is drawn.
 *
 * Planets are declared in {@link Planets}. Gravity, oxygen, the sky renderer,
 * the dimension effects and the lighting mixin all read from here, so adding
 * a planet never needs a new Java class for any of those.
 */
public final class Planet {

    private final String name;
    private final ResourceKey<Level> dimension;
    private final double gravity;
    private final boolean breathable;
    private final boolean darkNights;
    private final Sky sky;

    private Planet(Builder b) {
        this.name = b.name;
        this.dimension = ResourceKey.create(Registries.DIMENSION, new ResourceLocation(Astraterra.MODID, b.name));
        this.gravity = b.gravity;
        this.breathable = b.breathable;
        this.darkNights = b.darkNights;
        this.sky = b.sky;
    }

    public static Builder builder(String name) {
        return new Builder(name);
    }

    /** Path of the dimension, e.g. "moon". Must match data/astraterra/dimension/&lt;name&gt;.json. */
    public String name() { return name; }

    /** astraterra:&lt;name&gt; - also used as the "effects" id in the dimension_type JSON. */
    public ResourceLocation id() { return dimension.location(); }

    public ResourceKey<Level> dimension() { return dimension; }

    /** Multiple of Overworld gravity. 1.0 = normal, 1/6 = Moon. */
    public double gravity() { return gravity; }

    /** False = players need a filled space suit or they suffocate. */
    public boolean breathable() { return breathable; }

    /** True = at night only block light (torches...) lights the world. */
    public boolean darkNights() { return darkNights; }

    /** Custom sky, or null to use the vanilla one. */
    public Sky sky() { return sky; }

    @Override
    public String toString() { return "Planet[" + id() + "]"; }

    // ------------------------------------------------------------ sky

    /**
     * How the sky of an airless planet looks: black, stars, a sun disc and
     * any number of bodies (planets/moons) hanging in the sky.
     */
    public record Sky(int starCount, boolean starsInDaylight, boolean milkyWay,
                      float sunSizeDeg, List<SkyBody> bodies) {}

    /**
     * A planet or moon drawn in the sky as a lit, spinning, phased sphere.
     *
     * @param texture   2:1 equirectangular map, e.g. astraterra:textures/environment/earth_map.png
     * @param dirX/Y/Z  where it sits in the sky (normalized for you)
     * @param sizeDeg   angular radius in degrees
     * @param dayTicks  one full spin every this many ticks (0 = no spin)
     */
    public record SkyBody(ResourceLocation texture, float dirX, float dirY, float dirZ,
                          float sizeDeg, float dayTicks) {
        public SkyBody {
            float len = (float) Math.sqrt(dirX * dirX + dirY * dirY + dirZ * dirZ);
            dirX /= len;
            dirY /= len;
            dirZ /= len;
        }
    }

    // ------------------------------------------------------------ builder

    public static final class Builder {
        private final String name;
        private double gravity = 1.0;
        private boolean breathable = true;
        private boolean darkNights = false;
        private Sky sky = null;

        private Builder(String name) {
            this.name = name;
        }

        public Builder gravity(double gravity) {
            this.gravity = gravity;
            return this;
        }

        /** No air (needs a space suit), dark nights. */
        public Builder noAtmosphere() {
            this.breathable = false;
            this.darkNights = true;
            return this;
        }

        public Builder breathable(boolean breathable) {
            this.breathable = breathable;
            return this;
        }

        public Builder darkNights(boolean darkNights) {
            this.darkNights = darkNights;
            return this;
        }

        public Builder sky(SkyBuilder sky) {
            this.sky = sky.build();
            return this;
        }

        public Planet build() {
            return new Planet(this);
        }
    }

    public static SkyBuilder space() {
        return new SkyBuilder();
    }

    public static final class SkyBuilder {
        private int starCount = 25000;
        private boolean starsInDaylight = false;
        private boolean milkyWay = true;
        private float sunSizeDeg = 1.0F;
        private final List<SkyBody> bodies = new ArrayList<>();

        private SkyBuilder() {}

        public SkyBuilder stars(int count) { this.starCount = count; return this; }
        public SkyBuilder starsInDaylight(boolean value) { this.starsInDaylight = value; return this; }
        public SkyBuilder milkyWay(boolean value) { this.milkyWay = value; return this; }
        public SkyBuilder sunSize(float degrees) { this.sunSizeDeg = degrees; return this; }

        /** @param texture file name in assets/astraterra/textures/environment/, without .png */
        public SkyBuilder body(String texture, float dirX, float dirY, float dirZ, float sizeDeg, float dayTicks) {
            bodies.add(new SkyBody(
                    new ResourceLocation(Astraterra.MODID, "textures/environment/" + texture + ".png"),
                    dirX, dirY, dirZ, sizeDeg, dayTicks));
            return this;
        }

        private Sky build() {
            return new Sky(starCount, starsInDaylight, milkyWay, sunSizeDeg, List.copyOf(bodies));
        }
    }
}
