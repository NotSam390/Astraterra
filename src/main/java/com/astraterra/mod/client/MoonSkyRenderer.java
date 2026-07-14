package com.astraterra.mod.client;

import com.astraterra.mod.Astraterra;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexBuffer;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.blaze3d.vertex.VertexFormat;
import com.mojang.logging.LogUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderLevelStageEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.joml.Matrix4f;
import org.joml.Quaternionf;
import org.joml.Vector3f;
import org.joml.Vector4f;
import org.slf4j.Logger;

import java.util.Random;

/**
 * Draws the moon's entire sky by hand: stars, sun, and a fixed, tidally-locked
 * Earth with a slow phase cycle. Only runs while actually in the moon
 * dimension (MoonDimensionEffects disables vanilla's own sky rendering there,
 * so this is the only thing painting anything above the horizon).
 */
@Mod.EventBusSubscriber(modid = Astraterra.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE, value = Dist.CLIENT)
public class MoonSkyRenderer {

    private static final ResourceLocation MOON_DIMENSION = new ResourceLocation(Astraterra.MODID, "moon");
    private static final ResourceLocation EARTH_TEXTURE =
            new ResourceLocation(Astraterra.MODID, "textures/environment/earth.png");

    // 27 Minecraft days at 24000 ticks/day — matches the moon's real rotation
    // period, used for both the sun's rise/set cycle and the star field's slow
    // spin. Earth's phase is tied to the same cycle for simplicity.
    private static final double CYCLE_TICKS = 27.0 * 24000.0;

    private static final int STAR_COUNT = 4500; // ~3x vanilla's 1500
    private static final float SKY_DISTANCE = 100.0F;

    // Earth's fixed sky direction — tidally locked, does not move as the
    // player looks around or as time passes. Roughly "high in the north".
    private static final Vector3f EARTH_DIRECTION = new Vector3f(-0.35F, 0.55F, -0.75F).normalize();
    private static final float EARTH_ANGULAR_RADIUS_DEG = 1.0F; // ~2 degree apparent diameter
    private static final float SUN_ANGULAR_RADIUS_DEG = 0.25F;  // ~0.5 degree apparent diameter

    private static VertexBuffer starBuffer;

    // diagnostic only — logs once every ~100 frames instead of every frame
    private static int debugLogCounter = 0;
    private static final Logger LOGGER = LogUtils.getLogger();

    @SubscribeEvent
    public static void onRenderSky(RenderLevelStageEvent event) {
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_SKY) {
            return;
        }

        Minecraft mc = Minecraft.getInstance();
        ClientLevel level = mc.level;

        if (debugLogCounter++ % 100 == 0) {
            ResourceLocation currentDim = level == null ? null : level.dimension().location();
            String effectsClass = level == null ? null : level.effects().getClass().getName();
            String skyType = level == null ? null : String.valueOf(level.effects().skyType());
            LOGGER.info("[ASTRATERRA_SKY] AFTER_SKY fired. currentDim={} expected={} match={} effectsClass={} skyType={}",
                    currentDim, MOON_DIMENSION, MOON_DIMENSION.equals(currentDim), effectsClass, skyType);
        }

        if (level == null || !level.dimension().location().equals(MOON_DIMENSION)) {
            return;
        }

        PoseStack poseStack = event.getPoseStack();
        Matrix4f projectionMatrix = event.getProjectionMatrix();
        float partialTick = event.getPartialTick();
        double cycleProgress = ((level.getGameTime() + partialTick) % CYCLE_TICKS) / CYCLE_TICKS; // 0..1

        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.depthMask(false);
        RenderSystem.disableCull();

        renderStars(poseStack, projectionMatrix, cycleProgress);
        renderSun(poseStack, cycleProgress);
        renderEarth(poseStack, cycleProgress);

        RenderSystem.enableCull();
        RenderSystem.depthMask(true);
        RenderSystem.disableBlend();
    }

    // ------------------------------------------------------------------
    // Stars
    // ------------------------------------------------------------------

    private static void renderStars(PoseStack poseStack, Matrix4f projectionMatrix, double cycleProgress) {
        if (starBuffer == null) {
            starBuffer = buildStarBuffer();
        }

        poseStack.pushPose();
        // Slow rotation over the full 27-day cycle — independent of the
        // sun's own position so the field visibly turns over that period.
        poseStack.mulPose(new Quaternionf().rotateY((float) (cycleProgress * Math.PI * 2.0)));

        RenderSystem.setShader(GameRenderer::getPositionShader);
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
        starBuffer.bind();
        starBuffer.drawWithShader(poseStack.last().pose(), projectionMatrix, RenderSystem.getShader());
        VertexBuffer.unbind();

        poseStack.popPose();
    }

    private static VertexBuffer buildStarBuffer() {
        Random random = new Random(10842L); // fixed seed — same field every launch
        BufferBuilder builder = Tesselator.getInstance().getBuilder();
        builder.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION);

        for (int i = 0; i < STAR_COUNT; i++) {
            double x = random.nextFloat() * 2.0F - 1.0F;
            double y = random.nextFloat() * 2.0F - 1.0F;
            double z = random.nextFloat() * 2.0F - 1.0F;
            double size = 0.15F + random.nextFloat() * 0.1F;
            double lengthSq = x * x + y * y + z * z;

            if (lengthSq >= 1.0D || lengthSq <= 0.01D) {
                i--;
                continue;
            }

            double length = 1.0 / Math.sqrt(lengthSq);
            x *= length * SKY_DISTANCE;
            y *= length * SKY_DISTANCE;
            z *= length * SKY_DISTANCE;

            double rotation = random.nextFloat() * Math.PI * 2.0;
            // build a small quad facing roughly toward the origin
            Vector3f normal = new Vector3f((float) x, (float) y, (float) z).normalize();
            Vector3f up = Math.abs(normal.y()) < 0.99F ? new Vector3f(0, 1, 0) : new Vector3f(1, 0, 0);
            Vector3f tangent = new Vector3f(up).cross(normal).normalize().mul((float) size);
            Vector3f bitangent = new Vector3f(normal).cross(tangent).normalize().mul((float) size);

            float px = (float) x, py = (float) y, pz = (float) z;
            builder.vertex(px - tangent.x() - bitangent.x(), py - tangent.y() - bitangent.y(), pz - tangent.z() - bitangent.z()).endVertex();
            builder.vertex(px + tangent.x() - bitangent.x(), py + tangent.y() - bitangent.y(), pz + tangent.z() - bitangent.z()).endVertex();
            builder.vertex(px + tangent.x() + bitangent.x(), py + tangent.y() + bitangent.y(), pz + tangent.z() + bitangent.z()).endVertex();
            builder.vertex(px - tangent.x() + bitangent.x(), py - tangent.y() + bitangent.y(), pz - tangent.z() + bitangent.z()).endVertex();
        }

        VertexBuffer buffer = new VertexBuffer(VertexBuffer.Usage.STATIC);
        buffer.bind();
        buffer.upload(builder.end());
        VertexBuffer.unbind();
        return buffer;
    }

    // ------------------------------------------------------------------
    // Sun (+ corona) — untextured, no art asset needed
    // ------------------------------------------------------------------

    private static void renderSun(PoseStack poseStack, double cycleProgress) {
        // Rise/set over the full 27-day cycle, rotating around the same axis
        // vanilla uses for its own sun so "up" behaves the way you'd expect.
        float sunAngleDeg = (float) (cycleProgress * 360.0) - 90.0F;
        float sunAngleRad = (float) Math.toRadians(sunAngleDeg);
        Vector3f sunDir = new Vector3f(
                (float) Math.cos(sunAngleRad),
                (float) Math.sin(sunAngleRad),
                0.0F
        );

        if (sunDir.y() <= -0.05F) {
            return; // below the horizon for this part of the cycle — don't draw
        }

        poseStack.pushPose();
        Matrix4f pose = poseStack.last().pose();
        Vector4f worldPos = new Vector4f(sunDir.x() * SKY_DISTANCE, sunDir.y() * SKY_DISTANCE, sunDir.z() * SKY_DISTANCE, 1.0F);

        RenderSystem.setShader(GameRenderer::getPositionColorShader);

        // Soft corona first (larger, lower alpha, white-yellow), then the
        // bright disc on top. Both billboarded to face the camera.
        drawBillboardDisc(poseStack, worldPos, SKY_DISTANCE, SUN_ANGULAR_RADIUS_DEG * 3.0F, 1.0F, 0.95F, 0.75F, 0.25F, 24);
        drawBillboardDisc(poseStack, worldPos, SKY_DISTANCE, SUN_ANGULAR_RADIUS_DEG, 1.0F, 1.0F, 1.0F, 1.0F, 24);

        poseStack.popPose();
    }

    // ------------------------------------------------------------------
    // Earth — fixed direction, textured, with a phase shadow overlay
    // ------------------------------------------------------------------

    private static void renderEarth(PoseStack poseStack, double cycleProgress) {
        poseStack.pushPose();
        Vector4f worldPos = new Vector4f(
                EARTH_DIRECTION.x() * SKY_DISTANCE,
                EARTH_DIRECTION.y() * SKY_DISTANCE,
                EARTH_DIRECTION.z() * SKY_DISTANCE,
                1.0F
        );

        RenderSystem.setShader(GameRenderer::getPositionTexColorShader);
        RenderSystem.setShaderTexture(0, EARTH_TEXTURE);
        drawBillboardTexturedDisc(poseStack, worldPos, SKY_DISTANCE, EARTH_ANGULAR_RADIUS_DEG, 32);

        // Phase shadow: a dark lune shape whose width tracks the same cycle
        // as the sun. 0.5 = full Earth (no shadow), 0.0/1.0 = new Earth
        // (fully shadowed).
        float phaseCos = (float) Math.cos(cycleProgress * Math.PI * 2.0);
        RenderSystem.setShader(GameRenderer::getPositionColorShader);
        drawPhaseShadow(poseStack, worldPos, SKY_DISTANCE, EARTH_ANGULAR_RADIUS_DEG, phaseCos, 32);

        poseStack.popPose();
    }

    // ------------------------------------------------------------------
    // Shared billboard helpers
    // ------------------------------------------------------------------

    /** Basis vectors for a quad/disc that always faces the camera, given a world-space center. */
    private static Vector3f[] billboardBasis(Vector4f worldPos) {
        Vector3f normal = new Vector3f(worldPos.x(), worldPos.y(), worldPos.z()).normalize();
        Vector3f up = Math.abs(normal.y()) < 0.99F ? new Vector3f(0, 1, 0) : new Vector3f(1, 0, 0);
        Vector3f right = new Vector3f(up).cross(normal).normalize();
        Vector3f trueUp = new Vector3f(normal).cross(right).normalize();
        return new Vector3f[] { right, trueUp };
    }

    private static void drawBillboardDisc(PoseStack poseStack, Vector4f worldPos, float distance, float angularRadiusDeg,
                                          float r, float g, float b, float a, int segments) {
        float radius = distance * (float) Math.tan(Math.toRadians(angularRadiusDeg));
        Vector3f[] basis = billboardBasis(worldPos);
        Vector3f right = basis[0], up = basis[1];

        Matrix4f pose = poseStack.last().pose();
        BufferBuilder builder = Tesselator.getInstance().getBuilder();
        builder.begin(VertexFormat.Mode.TRIANGLE_FAN, DefaultVertexFormat.POSITION_COLOR);
        builder.vertex(pose, worldPos.x(), worldPos.y(), worldPos.z()).color(r, g, b, a).endVertex();
        for (int i = 0; i <= segments; i++) {
            double t = (i / (double) segments) * Math.PI * 2.0;
            float ox = (float) (right.x() * Math.cos(t) + up.x() * Math.sin(t)) * radius;
            float oy = (float) (right.y() * Math.cos(t) + up.y() * Math.sin(t)) * radius;
            float oz = (float) (right.z() * Math.cos(t) + up.z() * Math.sin(t)) * radius;
            builder.vertex(pose, worldPos.x() + ox, worldPos.y() + oy, worldPos.z() + oz)
                    .color(r, g, b, i == 0 || i == segments ? a : a * 0.0F)
                    .endVertex();
        }
        Tesselator.getInstance().end();
    }

    private static void drawBillboardTexturedDisc(PoseStack poseStack, Vector4f worldPos, float distance,
                                                  float angularRadiusDeg, int segments) {
        float radius = distance * (float) Math.tan(Math.toRadians(angularRadiusDeg));
        Vector3f[] basis = billboardBasis(worldPos);
        Vector3f right = basis[0], up = basis[1];

        Matrix4f pose = poseStack.last().pose();
        BufferBuilder builder = Tesselator.getInstance().getBuilder();
        builder.begin(VertexFormat.Mode.TRIANGLE_FAN, DefaultVertexFormat.POSITION_TEX_COLOR);
        builder.vertex(pose, worldPos.x(), worldPos.y(), worldPos.z())
                .uv(0.5F, 0.5F).color(1.0F, 1.0F, 1.0F, 1.0F).endVertex();
        for (int i = 0; i <= segments; i++) {
            double t = (i / (double) segments) * Math.PI * 2.0;
            float cx = (float) Math.cos(t), cy = (float) Math.sin(t);
            float ox = (right.x() * cx + up.x() * cy) * radius;
            float oy = (right.y() * cx + up.y() * cy) * radius;
            float oz = (right.z() * cx + up.z() * cy) * radius;
            builder.vertex(pose, worldPos.x() + ox, worldPos.y() + oy, worldPos.z() + oz)
                    .uv(0.5F + cx * 0.5F, 0.5F + cy * 0.5F)
                    .color(1.0F, 1.0F, 1.0F, 1.0F)
                    .endVertex();
        }
        Tesselator.getInstance().end();
    }

    /**
     * Draws a dark "lune" overlay over the Earth disc to fake a lit phase.
     * phaseCos ranges -1 (new / fully shadowed) to +1 (full / no shadow);
     * the shadow's inner edge is an ellipse whose horizontal squash follows
     * phaseCos, the classic two-arc method used for 2D moon-phase rendering.
     */
    private static void drawPhaseShadow(PoseStack poseStack, Vector4f worldPos, float distance,
                                        float angularRadiusDeg, float phaseCos, int segments) {
        if (phaseCos >= 0.999F) {
            return; // full Earth, no shadow to draw
        }

        float radius = distance * (float) Math.tan(Math.toRadians(angularRadiusDeg));
        Vector3f[] basis = billboardBasis(worldPos);
        Vector3f right = basis[0], up = basis[1];
        Matrix4f pose = poseStack.last().pose();

        float shadowAlpha = 0.85F;
        BufferBuilder builder = Tesselator.getInstance().getBuilder();
        builder.begin(VertexFormat.Mode.TRIANGLE_STRIP, DefaultVertexFormat.POSITION_COLOR);

        // Walk the top-to-bottom edge of the disc; at each height, the
        // shadow spans from the terminator (squashed by phaseCos) out to
        // the disc edge on whichever side is dark.
        boolean shadowOnPositiveX = phaseCos < 0.0F;
        for (int i = 0; i <= segments; i++) {
            double t = -Math.PI / 2.0 + (i / (double) segments) * Math.PI; // -90deg..90deg
            float cy = (float) Math.sin(t);
            float discEdgeX = (float) Math.cos(t);
            float terminatorX = discEdgeX * phaseCos;

            float edgeX = shadowOnPositiveX ? discEdgeX : -discEdgeX;
            float nearX = terminatorX;

            addPhaseVertex(builder, pose, worldPos, right, up, radius, nearX, cy, shadowAlpha);
            addPhaseVertex(builder, pose, worldPos, right, up, radius, edgeX, cy, shadowAlpha);
        }
        Tesselator.getInstance().end();
    }

    private static void addPhaseVertex(BufferBuilder builder, Matrix4f pose, Vector4f worldPos,
                                       Vector3f right, Vector3f up, float radius, float u, float v, float alpha) {
        float ox = (right.x() * u + up.x() * v) * radius;
        float oy = (right.y() * u + up.y() * v) * radius;
        float oz = (right.z() * u + up.z() * v) * radius;
        builder.vertex(pose, worldPos.x() + ox, worldPos.y() + oy, worldPos.z() + oz)
                .color(0.0F, 0.0F, 0.0F, alpha)
                .endVertex();
    }
}