package com.astraterra.mod.planet;

import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;

import javax.annotation.Nullable;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * The list of every planet in the mod. Adding a planet = one entry here
 * plus its JSON files (see PLANETS.md at the repo root).
 */
public final class Planets {

    private static final Map<ResourceKey<Level>, Planet> BY_DIMENSION = new LinkedHashMap<>();

    public static final Planet MOON = register(Planet.builder("moon")
            .gravity(1.0 / 6.0)
            .noAtmosphere()
            .sky(Planet.space()
                    .stars(25000)
                    .sunSize(1.0F)
                    // Earth: fixed in the sky, spins once per Minecraft day.
                    .body("earth_map", -0.35F, 0.55F, -0.75F, 10.0F, 24000.0F))
            .build());

    // Example for the next one:
    //
    // public static final Planet MARS = register(Planet.builder("mars")
    //         .gravity(0.38)
    //         .noAtmosphere()
    //         .sky(Planet.space().stars(15000).sunSize(0.7F)
    //                 .body("phobos_map", 0.4F, 0.6F, 0.2F, 2.0F, 0.0F))
    //         .build());

    private Planets() {}

    private static Planet register(Planet planet) {
        BY_DIMENSION.put(planet.dimension(), planet);
        return planet;
    }

    /** The planet this level is, or null for Overworld/Nether/End/other mods. */
    @Nullable
    public static Planet get(@Nullable Level level) {
        return level == null ? null : BY_DIMENSION.get(level.dimension());
    }

    public static Collection<Planet> all() {
        return Collections.unmodifiableCollection(BY_DIMENSION.values());
    }
}
