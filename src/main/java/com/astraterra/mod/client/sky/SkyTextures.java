package com.astraterra.mod.client.sky;

import com.astraterra.mod.Astraterra;
import com.mojang.blaze3d.platform.NativeImage;
import com.mojang.logging.LogUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;

import org.slf4j.Logger;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.function.Supplier;

/**
 * Textures for planet skies.
 *
 * Every sky body (Planet.SkyBody) points at a 2:1 equirectangular map in
 * assets/astraterra/textures/environment/. If the file exists it is used.
 * A few maps have a procedural fallback generated once at runtime, so they
 * work with no image file at all:
 *   earth_map.png   - procedural Earth
 *   milky_way.png   - procedural galaxy band (used by every starry sky)
 * Any other missing map: the body is simply not drawn (and a warning is logged once).
 */
public final class SkyTextures {

    private static final Logger LOGGER = LogUtils.getLogger();

    public static final ResourceLocation MILKY_WAY =
            new ResourceLocation(Astraterra.MODID, "textures/environment/milky_way.png");
    public static final ResourceLocation EARTH =
            new ResourceLocation(Astraterra.MODID, "textures/environment/earth_map.png");

    /** Procedural fallbacks, by the texture they stand in for. */
    private static final Map<ResourceLocation, Supplier<NativeImage>> FALLBACKS = Map.of(
            MILKY_WAY, SkyTextures::generateMilkyWay,
            EARTH, SkyTextures::generateEarth);

    private static final Map<ResourceLocation, Entry> ENTRIES = new HashMap<>();

    private SkyTextures() {}

    /** Returns null while the texture is still being generated, or if it doesn't exist. */
    public static ResourceLocation get(ResourceLocation texture) {
        return ENTRIES.computeIfAbsent(texture, Entry::new).get();
    }

    /**
     * Generation runs on a background thread so entering a planet never freezes
     * the game. The image is turned into a GPU texture on the render thread once ready.
     */
    private static final class Entry {
        private final ResourceLocation custom;
        private CompletableFuture<NativeImage> future;
        private ResourceLocation result;
        private boolean missing;

        Entry(ResourceLocation custom) {
            this.custom = custom;
        }

        ResourceLocation get() {
            if (result != null || missing) return result;
            Minecraft mc = Minecraft.getInstance();

            if (future == null) {
                if (mc.getResourceManager().getResource(custom).isPresent()) {
                    result = custom;
                    return result;
                }
                Supplier<NativeImage> fallback = FALLBACKS.get(custom);
                if (fallback == null) {
                    LOGGER.warn("[Astraterra] Sky texture {} not found and has no procedural fallback", custom);
                    missing = true;
                    return null;
                }
                future = CompletableFuture.supplyAsync(fallback);
            }
            if (!future.isDone() || future.isCompletedExceptionally()) return null;

            DynamicTexture texture = new DynamicTexture(future.join());
            texture.setFilter(true, false); // smooth, no mipmaps
            ResourceLocation generatedId = new ResourceLocation(custom.getNamespace(),
                    "generated/" + custom.getPath().replace(".png", ""));
            mc.getTextureManager().register(generatedId, texture);
            result = generatedId;
            return result;
        }
    }

    // ------------------------------------------------------------ Milky Way

    private static NativeImage generateMilkyWay() {
        final int w = 2048;
        final int h = 1024;
        NativeImage image = new NativeImage(NativeImage.Format.RGBA, w, h, false);

        float[] cosLon = new float[w];
        float[] sinLon = new float[w];
        float[] lonArr = new float[w];
        for (int px = 0; px < w; px++) {
            float lon = ((px + 0.5F) / w - 0.5F) * 2.0F * (float) Math.PI;
            lonArr[px] = lon;
            cosLon[px] = (float) Math.cos(lon);
            sinLon[px] = (float) Math.sin(lon);
        }

        for (int py = 0; py < h; py++) {
            float lat = (0.5F - (py + 0.5F) / h) * (float) Math.PI;
            float cosLat = (float) Math.cos(lat);
            float sinLat = (float) Math.sin(lat);

            float band = (float) (Math.exp(-sq(lat / 0.22F)) * 0.8 + Math.exp(-sq(lat / 0.60F)) * 0.2);
            float coreLat = (float) Math.exp(-sq(lat / 0.12F));
            float laneLat1 = (float) Math.exp(-sq(lat / 0.13F));
            float laneLat2 = (float) Math.exp(-sq(lat / 0.09F));

            for (int px = 0; px < w; px++) {
                if (band < 0.02F) {
                    image.setPixelRGBA(px, py, 0xFF000000);
                    continue;
                }
                float lon = lonArr[px];
                float dx = cosLat * cosLon[px];
                float dy = sinLat;
                float dz = cosLat * sinLon[px];

                // Bright bulge toward lon 0, fading along the band.
                float core = (float) Math.exp(-sq(lon / 0.95F));

                // Star clouds: patchy, contrasty clumps.
                float cl1 = fbm(dx * 2.6F, dy * 7.0F, dz * 2.6F, 5, 11);
                float cl2 = fbm(dx * 6.0F + 3.0F, dy * 14.0F, dz * 6.0F, 4, 17);
                float patch = (float) Math.pow(Mth.clamp((cl1 - 0.30F) / 0.45F, 0.0F, 1.0F), 1.4);
                float dens = band * (0.40F + 0.75F * core * (0.6F + 0.4F * coreLat))
                        * (0.25F + 1.0F * patch) * (0.70F + 0.6F * cl2);
                if (dens < 0.012F) {
                    image.setPixelRGBA(px, py, 0xFF000000);
                    continue;
                }

                // Dust: thin dark filaments (ridged noise) plus a broad central rift.
                float r1 = 1.0F - Math.abs(2.0F * fbm(dx * 3.0F + 40.0F, dy * 16.0F, dz * 3.0F, 5, 53) - 1.0F);
                float fil = smoothstep(0.72F, 0.93F, r1) * laneLat1;
                float r2 = 1.0F - Math.abs(2.0F * fbm(dx * 7.0F + 9.0F, dy * 26.0F, dz * 7.0F, 4, 97) - 1.0F);
                float fil2 = smoothstep(0.78F, 0.95F, r2) * laneLat2 * 0.7F;
                float rift = (float) Math.exp(-sq((lat - 0.03F * (float) Math.sin(lon * 2.0F)) / 0.03F)) * (0.35F + 0.65F * core);
                dens *= Mth.clamp(1.0F - 0.85F * Math.max(fil, fil2) - 0.40F * rift, 0.0F, 1.0F);

                // Fine grain = unresolved star dust, plus the odd texel-sized star.
                float grain = fbm(dx * 70.0F, dy * 70.0F, dz * 70.0F, 2, 131);
                dens *= 0.70F + 0.60F * grain;
                float spark = hash(px, py, 0, 977);
                float sparkle = spark > 0.992F ? spark * band * 0.96F : 0.0F;

                float b = Mth.clamp(dens * 1.20F, 0.0F, 1.0F);

                // Warm core, blue-white arms, faint red nebulae.
                float t = Mth.clamp(core * 1.1F, 0.0F, 1.0F);
                float r = Mth.lerp(t, 0.70F, 1.00F) * b;
                float g = Mth.lerp(t, 0.82F, 0.90F) * b;
                float bl = Mth.lerp(t, 1.00F, 0.74F) * b;

                float neb = smoothstep(0.60F, 0.78F, fbm(dx * 3.5F + 70.0F, dy * 9.0F, dz * 3.5F, 4, 613)) * band;
                float nebAmount = neb * 0.35F * Mth.clamp(dens, 0.0F, 1.0F);
                r += 0.55F * nebAmount + sparkle;
                g += 0.14F * nebAmount + sparkle;
                bl += 0.20F * nebAmount + sparkle;

                image.setPixelRGBA(px, py, pack(r, g, bl));
            }
        }
        return image;
    }

    // ---------------------------------------------------------------- Earth

    private static NativeImage generateEarth() {
        final int w = 1024;
        final int h = 512;
        NativeImage image = new NativeImage(NativeImage.Format.RGBA, w, h, false);

        for (int py = 0; py < h; py++) {
            float lat = (0.5F - (py + 0.5F) / h) * (float) Math.PI;
            float cosLat = (float) Math.cos(lat);
            float sinLat = (float) Math.sin(lat);

            for (int px = 0; px < w; px++) {
                float lon = ((px + 0.5F) / w - 0.5F) * 2.0F * (float) Math.PI;
                float dx = cosLat * (float) Math.cos(lon);
                float dy = sinLat;
                float dz = cosLat * (float) Math.sin(lon);

                // Continents
                float c = fbm(dx * 2.0F + 5.0F, dy * 2.0F, dz * 2.0F, 5, 101);
                float landMask = smoothstep(0.535F, 0.555F, c);

                // Ocean: shallow near coasts, deep far from them
                float depth = Mth.clamp((0.54F - c) / 0.15F, 0.0F, 1.0F);
                float or = Mth.lerp(depth, 0.10F, 0.02F);
                float og = Mth.lerp(depth, 0.38F, 0.10F);
                float ob = Mth.lerp(depth, 0.70F, 0.38F);

                // Land: green where wet, tan where dry, grey-brown on high ground
                float moist = fbm(dx * 3.5F + 9.0F, dy * 3.5F, dz * 3.5F, 4, 211);
                float dry = smoothstep(0.45F, 0.62F, moist);
                float lr = Mth.lerp(dry, 0.20F, 0.60F);
                float lg = Mth.lerp(dry, 0.45F, 0.50F);
                float lb = Mth.lerp(dry, 0.17F, 0.30F);
                float elev = Mth.clamp((c - 0.56F) / 0.12F, 0.0F, 1.0F) * 0.6F;
                lr = Mth.lerp(elev, lr, 0.48F);
                lg = Mth.lerp(elev, lg, 0.42F);
                lb = Mth.lerp(elev, lb, 0.36F);

                float r = Mth.lerp(landMask, or, lr);
                float g = Mth.lerp(landMask, og, lg);
                float b = Mth.lerp(landMask, ob, lb);

                // Polar ice with a ragged edge
                float jag = (fbm(dx * 6.0F, dy * 6.0F, dz * 6.0F, 3, 307) - 0.5F) * 0.25F;
                float ice = smoothstep(1.12F, 1.30F, Math.abs(lat) + jag);
                r = Mth.lerp(ice, r, 0.93F);
                g = Mth.lerp(ice, g, 0.96F);
                b = Mth.lerp(ice, b, 1.0F);

                // Clouds
                float cl = 0.65F * fbm(dx * 3.2F + 31.0F, dy * 3.2F, dz * 3.2F, 5, 307)
                        + 0.35F * fbm(dx * 8.0F + 5.0F, dy * 8.0F, dz * 8.0F, 3, 401);
                float cloud = smoothstep(0.50F, 0.68F, cl) * 0.88F;
                r = Mth.lerp(cloud, r, 1.0F);
                g = Mth.lerp(cloud, g, 1.0F);
                b = Mth.lerp(cloud, b, 1.0F);

                image.setPixelRGBA(px, py, pack(r, g, b));
            }
        }
        return image;
    }

    // ---------------------------------------------------------------- noise

    private static float hash(int x, int y, int z, int seed) {
        int h = x * 374761393 + y * 668265263 + z * 1274126177 + seed * 362437;
        h = (h ^ (h >>> 13)) * 1274126177;
        h ^= h >>> 16;
        return (h & 0xFFFFFF) / (float) 0xFFFFFF;
    }

    private static float valueNoise(float x, float y, float z, int seed) {
        int xi = Mth.floor(x);
        int yi = Mth.floor(y);
        int zi = Mth.floor(z);
        float u = smooth(x - xi);
        float v = smooth(y - yi);
        float w = smooth(z - zi);

        float c000 = hash(xi, yi, zi, seed);
        float c100 = hash(xi + 1, yi, zi, seed);
        float c010 = hash(xi, yi + 1, zi, seed);
        float c110 = hash(xi + 1, yi + 1, zi, seed);
        float c001 = hash(xi, yi, zi + 1, seed);
        float c101 = hash(xi + 1, yi, zi + 1, seed);
        float c011 = hash(xi, yi + 1, zi + 1, seed);
        float c111 = hash(xi + 1, yi + 1, zi + 1, seed);

        float x00 = Mth.lerp(u, c000, c100);
        float x10 = Mth.lerp(u, c010, c110);
        float x01 = Mth.lerp(u, c001, c101);
        float x11 = Mth.lerp(u, c011, c111);
        float y0 = Mth.lerp(v, x00, x10);
        float y1 = Mth.lerp(v, x01, x11);
        return Mth.lerp(w, y0, y1);
    }

    private static float fbm(float x, float y, float z, int octaves, int seed) {
        float sum = 0.0F;
        float amp = 0.5F;
        float norm = 0.0F;
        float freq = 1.0F;
        for (int o = 0; o < octaves; o++) {
            sum += amp * valueNoise(x * freq, y * freq, z * freq, seed + o * 31);
            norm += amp;
            amp *= 0.5F;
            freq *= 2.03F;
        }
        return sum / norm;
    }

    private static float smooth(float t) {
        return t * t * (3.0F - 2.0F * t);
    }

    private static float smoothstep(float edge0, float edge1, float x) {
        float t = Mth.clamp((x - edge0) / (edge1 - edge0), 0.0F, 1.0F);
        return t * t * (3.0F - 2.0F * t);
    }

    private static float sq(float x) {
        return x * x;
    }

    // NativeImage pixels are packed ABGR.
    private static int pack(float r, float g, float b) {
        int ri = (int) (Mth.clamp(r, 0.0F, 1.0F) * 255.0F + 0.5F);
        int gi = (int) (Mth.clamp(g, 0.0F, 1.0F) * 255.0F + 0.5F);
        int bi = (int) (Mth.clamp(b, 0.0F, 1.0F) * 255.0F + 0.5F);
        return 0xFF000000 | (bi << 16) | (gi << 8) | ri;
    }
}