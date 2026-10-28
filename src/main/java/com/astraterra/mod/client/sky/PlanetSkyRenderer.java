package com.astraterra.mod.client.sky;

import com.astraterra.mod.Astraterra;
import com.astraterra.mod.planet.Planet;
import com.astraterra.mod.planet.Planets;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexBuffer;
import com.mojang.blaze3d.vertex.VertexFormat;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.ShaderInstance;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderLevelStageEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.joml.Matrix4f;
import org.joml.Quaternionf;
import org.joml.Vector3f;
import org.joml.Vector4f;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * The sky of every airless planet: a pure black void with a hard white sun,
 * thousands of steady colored stars, a Milky Way band and any number of fixed,
 * rotating, phased bodies (Earth from the Moon, ...).
 *
 * What is drawn comes from the planet's Planet.Sky (see Planets). Planets with
 * no Sky keep the vanilla sky and this class does nothing for them.
 *
 * Everything runs on the vanilla clock (level.getSunAngle), so /time set works.
 * Draw order, back to front: Milky Way, stars, sun, bodies.
 *
 * Sky.starsInDaylight: true = stars and Milky Way always visible (no sky glow),
 * false = hidden in daylight, fading in at dusk (far from the sun first).
 */
@Mod.EventBusSubscriber(modid = Astraterra.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE, value = Dist.CLIENT)
public class PlanetSkyRenderer {

    private static final float SKY_DISTANCE = 100.0F;

    // ----- stars
    // Star count and "visible in daylight" are per planet (Planet.Sky).
    // Share of stars packed along the Milky Way band instead of spread evenly.
    private static final float BAND_STAR_SHARE = 0.4F;
    // Stars are split into groups by closeness to the sun, each fading on its own schedule.
    private static final int STAR_BUCKETS = 16;
    // 0 = every star fades together. Higher = bigger gap between first and last star in.
    private static final float STAR_FADE_SPREAD = 0.5F;

    // ----- Milky Way
    private static final float MILKY_WAY_TILT_DEG = 62.0F;
    private static final float MILKY_WAY_RADIUS = SKY_DISTANCE * 0.96F;
    private static final float MILKY_WAY_BRIGHTNESS = 1.0F;

    // ----- sky bodies (size, position and spin are per body, see Planet.SkyBody)
    private static final int BODY_LAT_SEGMENTS = 32;
    private static final int BODY_LON_SEGMENTS = 32;

    /** Star meshes by star count, so planets with the same count share one. */
    private static final Map<Integer, VertexBuffer[]> STAR_BUFFERS = new HashMap<>();
    private static VertexBuffer milkyWayBuffer;

    private record Star(Vector3f dir, float size, float r, float g, float b) {}

    @SubscribeEvent
    public static void onRenderSky(RenderLevelStageEvent event) {
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_SKY) return;

        Minecraft mc = Minecraft.getInstance();
        ClientLevel level = mc.level;
        if (level == null) return;

        Planet planet = Planets.get(level);
        if (planet == null || planet.sky() == null) return;
        Planet.Sky sky = planet.sky();

        PoseStack poseStack = event.getPoseStack();
        Matrix4f projectionMatrix = event.getProjectionMatrix();
        float partialTick = event.getPartialTick();

        float sunAngle = level.getSunAngle(partialTick);
        float sunHeight = Mth.cos(sunAngle); // +1 noon, 0 horizon, -1 midnight
        float starLevel = sky.starsInDaylight() ? 1.0F : Daylight.starFactor(sunAngle);

        float prevFogStart = RenderSystem.getShaderFogStart();
        float prevFogEnd = RenderSystem.getShaderFogEnd();
        RenderSystem.setShaderFogStart(Float.MAX_VALUE);
        RenderSystem.setShaderFogEnd(Float.MAX_VALUE);

        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.depthMask(false);
        RenderSystem.disableCull();

        if (sky.milkyWay()) {
            renderMilkyWay(poseStack, projectionMatrix, sunAngle, sky.starsInDaylight() ? 1.0F : Daylight.starFactor(sunAngle));
        }
        renderStars(poseStack, projectionMatrix, sunAngle, starLevel, sky.starCount());
        renderSun(poseStack, sunAngle, sunHeight, sky.sunSizeDeg());
        for (Planet.SkyBody body : sky.bodies()) {
            float spin = body.dayTicks() <= 0 ? 0.0F
                    : ((level.getDayTime() % (long) body.dayTicks()) + partialTick) / body.dayTicks();
            renderBody(poseStack, sunAngle, body, spin);
        }

        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
        RenderSystem.enableCull();
        RenderSystem.depthMask(true);
        RenderSystem.disableBlend();
        RenderSystem.setShaderFogStart(prevFogStart);
        RenderSystem.setShaderFogEnd(prevFogEnd);
    }

    // ------------------------------------------------------------ Milky Way

    private static void renderMilkyWay(PoseStack poseStack, Matrix4f projectionMatrix, float sunAngle, float starLevel) {
        float brightness = starLevel * MILKY_WAY_BRIGHTNESS;
        if (brightness <= 0.01F) return;

        ShaderInstance shader = GameRenderer.getPositionTexShader();
        if (shader == null) return;

        ResourceLocation texture = SkyTextures.get(SkyTextures.MILKY_WAY);
        if (texture == null) return; // still being generated
        if (milkyWayBuffer == null) milkyWayBuffer = buildMilkyWayBuffer();

        poseStack.pushPose();
        poseStack.mulPose(Axis.ZP.rotation(sunAngle));
        poseStack.mulPose(Axis.XP.rotationDegrees(MILKY_WAY_TILT_DEG));

        RenderSystem.setShader(GameRenderer::getPositionTexShader);
        RenderSystem.setShaderTexture(0, texture);
        RenderSystem.setShaderColor(brightness, brightness, brightness, 1.0F);
        milkyWayBuffer.bind();
        milkyWayBuffer.drawWithShader(poseStack.last().pose(), projectionMatrix, shader);
        VertexBuffer.unbind();
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);

        poseStack.popPose();
    }

    private static VertexBuffer buildMilkyWayBuffer() {
        final int latSeg = 32;
        final int lonSeg = 64;

        BufferBuilder builder = Tesselator.getInstance().getBuilder();
        builder.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_TEX);
        for (int i = 0; i < latSeg; i++) {
            float v0 = (float) i / latSeg;
            float v1 = (float) (i + 1) / latSeg;
            float lat0 = (0.5F - v0) * (float) Math.PI;
            float lat1 = (0.5F - v1) * (float) Math.PI;
            for (int j = 0; j < lonSeg; j++) {
                float u0 = (float) j / lonSeg;
                float u1 = (float) (j + 1) / lonSeg;
                float lon0 = (u0 - 0.5F) * 2.0F * (float) Math.PI;
                float lon1 = (u1 - 0.5F) * 2.0F * (float) Math.PI;
                sphereVertex(builder, lat0, lon0, u0, v0);
                sphereVertex(builder, lat0, lon1, u1, v0);
                sphereVertex(builder, lat1, lon1, u1, v1);
                sphereVertex(builder, lat1, lon0, u0, v1);
            }
        }

        VertexBuffer buffer = new VertexBuffer(VertexBuffer.Usage.STATIC);
        buffer.bind();
        buffer.upload(builder.end());
        VertexBuffer.unbind();
        return buffer;
    }

    private static void sphereVertex(BufferBuilder builder, float lat, float lon, float u, float v) {
        float cosLat = Mth.cos(lat);
        builder.vertex(
                MILKY_WAY_RADIUS * cosLat * Mth.cos(lon),
                MILKY_WAY_RADIUS * Mth.sin(lat),
                MILKY_WAY_RADIUS * cosLat * Mth.sin(lon)
        ).uv(u, v).endVertex();
    }

    // ---------------------------------------------------------------- stars

    private static void renderStars(PoseStack poseStack, Matrix4f projectionMatrix, float sunAngle, float starLevel, int starCount) {
        if (starLevel <= 0.001F || starCount <= 0) return;

        ShaderInstance shader = GameRenderer.getPositionColorShader();
        if (shader == null) return;

        VertexBuffer[] starBuffers = STAR_BUFFERS.computeIfAbsent(starCount, PlanetSkyRenderer::buildStarBuffers);

        poseStack.pushPose();
        // Stars turn with the sun around the same axis. In this rotating frame the
        // sun is always at +Y, so a star's height (y) is how close it is to the sun.
        poseStack.mulPose(Axis.ZP.rotation(sunAngle));

        RenderSystem.setShader(GameRenderer::getPositionColorShader);
        for (int i = 0; i < STAR_BUCKETS; i++) {
            VertexBuffer buffer = starBuffers[i];
            if (buffer == null) continue;

            // 0 = opposite the sun, 1 = right next to the sun.
            float nearSun = (i + 0.5F) / STAR_BUCKETS;
            // Far-from-sun stars come in first, near-sun stars last (and leave first).
            float alpha = Mth.clamp((starLevel - nearSun * STAR_FADE_SPREAD) / (1.0F - STAR_FADE_SPREAD), 0.0F, 1.0F);
            alpha = alpha * alpha * (3.0F - 2.0F * alpha);
            if (alpha <= 0.01F) continue;

            RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, alpha);
            buffer.bind();
            buffer.drawWithShader(poseStack.last().pose(), projectionMatrix, shader);
            VertexBuffer.unbind();
        }
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);

        poseStack.popPose();
    }

    private static VertexBuffer[] buildStarBuffers(int starCount) {
        // Fixed seed: same sky every launch.
        RandomSource random = RandomSource.create(10842L);
        // Same tilt as the Milky Way mesh, so band stars sit inside the band.
        Quaternionf tilt = new Quaternionf().rotateX((float) Math.toRadians(MILKY_WAY_TILT_DEG));

        List<List<Star>> buckets = new ArrayList<>();
        for (int i = 0; i < STAR_BUCKETS; i++) buckets.add(new ArrayList<>());

        for (int n = 0; n < starCount; n++) {
            Vector3f dir;
            if (random.nextFloat() < BAND_STAR_SHARE) {
                float lat = (float) (random.nextGaussian() * 0.17);
                float lon = random.nextFloat() * (float) (Math.PI * 2.0);
                float cosLat = Mth.cos(lat);
                dir = new Vector3f(cosLat * Mth.cos(lon), Mth.sin(lat), cosLat * Mth.sin(lon));
                tilt.transform(dir);
            } else {
                double x, y, z, lenSq;
                do {
                    x = random.nextFloat() * 2.0F - 1.0F;
                    y = random.nextFloat() * 2.0F - 1.0F;
                    z = random.nextFloat() * 2.0F - 1.0F;
                    lenSq = x * x + y * y + z * z;
                } while (lenSq >= 1.0D || lenSq <= 0.01D);
                double invLen = 1.0 / Math.sqrt(lenSq);
                dir = new Vector3f((float) (x * invLen), (float) (y * invLen), (float) (z * invLen));
            }

            // Most stars are faint, a few are bright. Brightness also sets size.
            float mag = random.nextFloat();
            float bright = 0.25F + 0.75F * mag * mag * mag;
            float size = 0.11F + 0.20F * bright;

            // Real star colors: mostly white, some blue-white, yellow-orange and red.
            float roll = random.nextFloat();
            float r, g, b;
            if (roll < 0.14F) {
                r = 0.66F; g = 0.78F; b = 1.00F;
            } else if (roll < 0.70F) {
                r = 1.00F; g = 0.98F; b = 0.95F;
            } else if (roll < 0.90F) {
                r = 1.00F; g = 0.86F; b = 0.66F;
            } else {
                r = 1.00F; g = 0.62F; b = 0.52F;
            }

            int bucket = Mth.clamp((int) ((dir.y() + 1.0F) * 0.5F * STAR_BUCKETS), 0, STAR_BUCKETS - 1);
            buckets.get(bucket).add(new Star(dir, size, r * bright, g * bright, b * bright));
        }

        VertexBuffer[] buffers = new VertexBuffer[STAR_BUCKETS];
        for (int i = 0; i < STAR_BUCKETS; i++) {
            List<Star> stars = buckets.get(i);
            if (stars.isEmpty()) continue;

            BufferBuilder builder = Tesselator.getInstance().getBuilder();
            builder.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_COLOR);
            for (Star star : stars) addStarQuad(builder, star);

            VertexBuffer buffer = new VertexBuffer(VertexBuffer.Usage.STATIC);
            buffer.bind();
            buffer.upload(builder.end());
            VertexBuffer.unbind();
            buffers[i] = buffer;
        }
        return buffers;
    }

    private static void addStarQuad(BufferBuilder builder, Star star) {
        Vector3f dir = star.dir();
        float size = star.size();

        Vector3f up = Math.abs(dir.y()) < 0.99F ? new Vector3f(0, 1, 0) : new Vector3f(1, 0, 0);
        Vector3f tangent = new Vector3f(up).cross(dir).normalize().mul(size);
        Vector3f bitangent = new Vector3f(dir).cross(tangent).normalize().mul(size);

        float px = dir.x() * SKY_DISTANCE;
        float py = dir.y() * SKY_DISTANCE;
        float pz = dir.z() * SKY_DISTANCE;
        float r = star.r(), g = star.g(), b = star.b();

        builder.vertex(px - tangent.x() - bitangent.x(), py - tangent.y() - bitangent.y(), pz - tangent.z() - bitangent.z()).color(r, g, b, 1.0F).endVertex();
        builder.vertex(px + tangent.x() - bitangent.x(), py + tangent.y() - bitangent.y(), pz + tangent.z() - bitangent.z()).color(r, g, b, 1.0F).endVertex();
        builder.vertex(px + tangent.x() + bitangent.x(), py + tangent.y() + bitangent.y(), pz + tangent.z() + bitangent.z()).color(r, g, b, 1.0F).endVertex();
        builder.vertex(px - tangent.x() + bitangent.x(), py - tangent.y() + bitangent.y(), pz - tangent.z() + bitangent.z()).color(r, g, b, 1.0F).endVertex();
    }

    // ------------------------------------------------------------------ sun

    private static void renderSun(PoseStack poseStack, float sunAngle, float sunHeight, float sunSizeDeg) {
        // Fully opaque, never fades. The terrain hides it as it sets; skip it only
        // once it is clearly below the horizon.
        if (sunHeight < -0.05F) return;

        Vector4f worldPos = new Vector4f(
                -Mth.sin(sunAngle) * SKY_DISTANCE,
                Mth.cos(sunAngle) * SKY_DISTANCE,
                0.0F,
                1.0F);

        RenderSystem.setShader(GameRenderer::getPositionColorShader);
        // One hard-edged pure white disc. No glow, no rays, no color.
        drawDisc(poseStack, worldPos, SKY_DISTANCE, sunSizeDeg, 48);
    }

    private static void drawDisc(PoseStack poseStack, Vector4f worldPos, float distance, float angularRadiusDeg, int segments) {
        float radius = distance * (float) Math.tan(Math.toRadians(angularRadiusDeg));

        Vector3f normal = new Vector3f(worldPos.x(), worldPos.y(), worldPos.z()).normalize();
        Vector3f upRef = Math.abs(normal.y()) < 0.99F ? new Vector3f(0, 1, 0) : new Vector3f(1, 0, 0);
        Vector3f right = new Vector3f(upRef).cross(normal).normalize();
        Vector3f up = new Vector3f(normal).cross(right).normalize();
        Matrix4f pose = poseStack.last().pose();

        BufferBuilder builder = Tesselator.getInstance().getBuilder();
        builder.begin(VertexFormat.Mode.TRIANGLE_FAN, DefaultVertexFormat.POSITION_COLOR);
        builder.vertex(pose, worldPos.x(), worldPos.y(), worldPos.z()).color(1.0F, 1.0F, 1.0F, 1.0F).endVertex();
        for (int i = 0; i <= segments; i++) {
            double t = (i / (double) segments) * Math.PI * 2.0;
            float cos = (float) Math.cos(t);
            float sin = (float) Math.sin(t);
            float ox = (right.x() * cos + up.x() * sin) * radius;
            float oy = (right.y() * cos + up.y() * sin) * radius;
            float oz = (right.z() * cos + up.z() * sin) * radius;
            builder.vertex(pose, worldPos.x() + ox, worldPos.y() + oy, worldPos.z() + oz).color(1.0F, 1.0F, 1.0F, 1.0F).endVertex();
        }
        Tesselator.getInstance().end();
    }

    // ---------------------------------------------------------------- bodies

    private static void renderBody(PoseStack poseStack, float sunAngle, Planet.SkyBody body, float spin) {
        ResourceLocation texture = SkyTextures.get(body.texture());
        if (texture == null) return; // still being generated, or missing

        Vector3f direction = new Vector3f(body.dirX(), body.dirY(), body.dirZ());

        // Orientation of the body as seen from the player.
        // front = from the body toward the player; right/up span what the player sees.
        Vector3f front = new Vector3f(direction).negate();
        Vector3f right = new Vector3f(0, 1, 0).cross(front).normalize();
        Vector3f up = new Vector3f(front).cross(right).normalize();

        // Phase. The lit side faces the sun, but the body is fixed in the sky,
        // so we drive it from the sun angle: new at noon, full at midnight, half at
        // sunrise/sunset, lit on the side where the sun is.
        Vector3f west = new Vector3f(-1, 0, 0);
        west.sub(new Vector3f(front).mul(front.dot(west)));
        if (west.lengthSquared() < 1.0E-4F) west.set(right); else west.normalize();
        Vector3f light = new Vector3f(front).mul(-Mth.cos(sunAngle))
                .add(new Vector3f(west).mul(Mth.sin(sunAngle)))
                .normalize();

        Vector3f center = new Vector3f(direction).mul(SKY_DISTANCE);
        float radius = SKY_DISTANCE * (float) Math.tan(Math.toRadians(body.sizeDeg()));
        Matrix4f pose = poseStack.last().pose();

        RenderSystem.setShader(GameRenderer::getPositionTexColorShader);
        RenderSystem.setShaderTexture(0, texture);
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);

        // Only the hemisphere facing the player is drawn, so there is no overdraw.
        BufferBuilder builder = Tesselator.getInstance().getBuilder();
        builder.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_TEX_COLOR);
        for (int i = 0; i < BODY_LAT_SEGMENTS; i++) {
            for (int j = 0; j < BODY_LON_SEGMENTS; j++) {
                bodyVertex(builder, pose, center, right, up, front, light, radius, spin, i, j);
                bodyVertex(builder, pose, center, right, up, front, light, radius, spin, i + 1, j);
                bodyVertex(builder, pose, center, right, up, front, light, radius, spin, i + 1, j + 1);
                bodyVertex(builder, pose, center, right, up, front, light, radius, spin, i, j + 1);
            }
        }
        Tesselator.getInstance().end();
    }

    private static void bodyVertex(BufferBuilder builder, Matrix4f pose, Vector3f center,
                                    Vector3f right, Vector3f up, Vector3f front, Vector3f light,
                                    float radius, float rotation, int latIndex, int lonIndex) {
        float lat = ((float) latIndex / BODY_LAT_SEGMENTS - 0.5F) * (float) Math.PI;
        float lonLocal = ((float) lonIndex / BODY_LON_SEGMENTS - 0.5F) * (float) Math.PI;

        float cosLat = Mth.cos(lat);
        float nx = cosLat * Mth.sin(lonLocal);
        float ny = Mth.sin(lat);
        float nz = cosLat * Mth.cos(lonLocal);

        // Surface normal in world space.
        float wx = right.x() * nx + up.x() * ny + front.x() * nz;
        float wy = right.y() * nx + up.y() * ny + front.y() * nz;
        float wz = right.z() * nx + up.z() * ny + front.z() * nz;

        // Lit where the surface faces the light; slightly soft edge, dim blue night side.
        float d = wx * light.x() + wy * light.y() + wz * light.z();
        float t = Mth.clamp((d + 0.16F) / 0.32F, 0.0F, 1.0F);
        float lit = t * t * (3.0F - 2.0F * t);
        float r = Mth.lerp(lit, 0.05F, 1.0F);
        float g = Mth.lerp(lit, 0.07F, 1.0F);
        float b = Mth.lerp(lit, 0.14F, 1.0F);

        // Spinning = sliding the texture sideways.
        float u = 0.5F + lonLocal / (2.0F * (float) Math.PI) + rotation;
        float v = 0.5F - lat / (float) Math.PI;

        builder.vertex(pose, center.x() + radius * wx, center.y() + radius * wy, center.z() + radius * wz)
                .uv(u, v).color(r, g, b, 1.0F).endVertex();
    }
}