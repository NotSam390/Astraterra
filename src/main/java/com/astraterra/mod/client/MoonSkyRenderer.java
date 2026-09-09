package com.astraterra.mod.client;

import com.astraterra.mod.Astraterra;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexBuffer;
import com.mojang.blaze3d.vertex.VertexFormat;
import com.mojang.logging.LogUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.client.multiplayer.ClientLevel;
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
import org.slf4j.Logger;
import java.util.ArrayList;
import java.util.List;

@Mod.EventBusSubscriber(modid = Astraterra.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE, value = Dist.CLIENT)
public class MoonSkyRenderer {
    private static final ResourceLocation MOON_DIMENSION = new ResourceLocation("astraterra:moon");
    private static final ResourceLocation EARTH_TEXTURE = new ResourceLocation("astraterra:textures/environment/earth.png");
    private static final double CYCLE_TICKS = 1200.0;
    private static final int STAR_COUNT = 4500;
    private static final float SKY_DISTANCE = 100.0F;
    private static final Vector3f EARTH_DIRECTION = new Vector3f(-0.35F, 0.55F, -0.75F).normalize();
    private static final float EARTH_ANGULAR_RADIUS_DEG = 20.0F;
    private static final float SUN_ANGULAR_RADIUS_DEG = 2.0F;
    public static boolean isNight(long gameTime, float partialTick) {
        double cycleProgress = ((gameTime + partialTick) % CYCLE_TICKS) / CYCLE_TICKS;
        float sunAngleDeg = (float) (cycleProgress * 360.0) - 90.0F;
        float sunHeight = (float) Math.sin(Math.toRadians(sunAngleDeg));
        return sunHeight <= -0.05F;
    }
    private static VertexBuffer starBuffer;
    private static final List<Vector3f> starDirs = new ArrayList<>();
    private static final List<Float> starSizes = new ArrayList<>();
    private static int debugLogCounter = 0;
    private static final Logger LOGGER = LogUtils.getLogger();
    @SubscribeEvent
    public static void onRenderSky(RenderLevelStageEvent event) {
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_SKY) return;
        Minecraft mc = Minecraft.getInstance();
        ClientLevel level = mc.level;
        if (level == null || !level.dimension().location().equals(MOON_DIMENSION)) return;
        if (debugLogCounter++ % 100 == 0) {
            LOGGER.info("[ASTRATERRA_SKY] dim={} effects={}", level.dimension().location(), level.effects().getClass().getSimpleName());
        }
        float prevFogStart = RenderSystem.getShaderFogStart();
        float prevFogEnd = RenderSystem.getShaderFogEnd();
        RenderSystem.setShaderFogStart(Float.MAX_VALUE);
        RenderSystem.setShaderFogEnd(Float.MAX_VALUE);
        PoseStack poseStack = event.getPoseStack();
        Matrix4f projectionMatrix = event.getProjectionMatrix();
        float partialTick = event.getPartialTick();
        double cycleProgress = ((level.getGameTime() + partialTick) % CYCLE_TICKS) / CYCLE_TICKS;
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
        RenderSystem.setShaderFogStart(prevFogStart);
        RenderSystem.setShaderFogEnd(prevFogEnd);
    }
    private static void ensureStarData() {
        if (!starDirs.isEmpty()) return;
        RandomSource random = RandomSource.create();
        for (int i = 0; i < STAR_COUNT; ) {
            double x = random.nextFloat() * 2.0F - 1.0F;
            double y = random.nextFloat() * 2.0F - 1.0F;
            double z = random.nextFloat() * 2.0F - 1.0F;
            double lenSq = x * x + y * y + z * z;
            if (lenSq >= 1.0D || lenSq <= 0.01D) continue;
            double invLen = 1.0 / Math.sqrt(lenSq);
            Vector3f dir = new Vector3f((float)(x*invLen), (float)(y*invLen), (float)(z*invLen));
            starDirs.add(dir);
            starSizes.add(0.25F + random.nextFloat() * 0.1F);
            i++;
        }
    }
    private static void renderStars(PoseStack poseStack, Matrix4f projectionMatrix, double cycleProgress) {
        float sunAngleDeg = (float) (cycleProgress * 360.0) - 90.0F;
        float sunAngleRad = (float) Math.toRadians(sunAngleDeg);
        Vector3f sunDir = new Vector3f((float)Math.cos(sunAngleRad), (float)Math.sin(sunAngleRad), 0.0F).normalize();
        float sunHeight = sunDir.y();
        float baseT = Mth.clamp((sunHeight + 0.1F) / 0.35F, 0.0F, 1.0F);
        baseT = baseT * baseT * (3.0F - 2.0F * baseT);
        float baseAlpha = 1.0F - baseT;
        if (baseAlpha <= 0.02F) return;
        ensureStarData();
        BufferBuilder builder = Tesselator.getInstance().getBuilder();
        builder.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_COLOR);
        int drawn = 0;
        for (int i = 0; i < starDirs.size(); i++) {
            Vector3f dir = starDirs.get(i);
            float size = starSizes.get(i);
            float dot = dir.dot(sunDir);
            float proximity = (dot + 1.0F) * 0.5F;
            float individualAlpha = baseAlpha * (1.0F - 0.85F * proximity);
            individualAlpha *= Mth.clamp(1.0F - proximity * 0.5F * baseT, 0.0F, 1.0F);
            if (individualAlpha < 0.02F) continue;
            Vector3f pos = new Vector3f(dir).mul(SKY_DISTANCE);
            Vector3f up = Math.abs(dir.y()) < 0.99F ? new Vector3f(0, 1, 0) : new Vector3f(1, 0, 0);
            Vector3f tangent = new Vector3f(up).cross(dir).normalize().mul(size);
            Vector3f bitangent = new Vector3f(dir).cross(tangent).normalize().mul(size);
            float px = pos.x(), py = pos.y(), pz = pos.z();
            builder.vertex(px - tangent.x() - bitangent.x(), py - tangent.y() - bitangent.y(), pz - tangent.z() - bitangent.z()).color(1.0F,1.0F,1.0F,individualAlpha).endVertex();
            builder.vertex(px + tangent.x() - bitangent.x(), py + tangent.y() - bitangent.y(), pz + tangent.z() - bitangent.z()).color(1.0F,1.0F,1.0F,individualAlpha).endVertex();
            builder.vertex(px + tangent.x() + bitangent.x(), py + tangent.y() + bitangent.y(), pz + tangent.z() + bitangent.z()).color(1.0F,1.0F,1.0F,individualAlpha).endVertex();
            builder.vertex(px - tangent.x() + bitangent.x(), py - tangent.y() + bitangent.y(), pz - tangent.z() + bitangent.z()).color(1.0F,1.0F,1.0F,individualAlpha).endVertex();
            drawn++;
        }
        if (drawn == 0) {
            builder.end();
            return;
        }
        poseStack.pushPose();
        poseStack.mulPose(new Quaternionf().rotateY((float)(cycleProgress * Math.PI * 2.0)));
        if (starBuffer == null) starBuffer = new VertexBuffer(VertexBuffer.Usage.DYNAMIC);
        starBuffer.bind();
        starBuffer.upload(builder.end());
        VertexBuffer.unbind();
        RenderSystem.setShader(GameRenderer::getPositionColorShader);
        RenderSystem.setShaderColor(1.0F,1.0F,1.0F,1.0F);
        starBuffer.bind();
        starBuffer.drawWithShader(poseStack.last().pose(), projectionMatrix, RenderSystem.getShader());
        VertexBuffer.unbind();
        poseStack.popPose();
    }
    private static VertexBuffer buildStarBuffer() {
        RandomSource random = RandomSource.create();
        BufferBuilder builder = Tesselator.getInstance().getBuilder();
        builder.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION);
        for (int i = 0; i < STAR_COUNT; i++) {
            double x = random.nextFloat() * 2.0F - 1.0F;
            double y = random.nextFloat() * 2.0F - 1.0F;
            double z = random.nextFloat() * 2.0F - 1.0F;
            double size = 0.15F + random.nextFloat() * 0.1F;
            double lenSq = x * x + y * y + z * z;
            if (lenSq >= 1.0D || lenSq <= 0.01D) { i--; continue; }
            double invLen = 1.0 / Math.sqrt(lenSq);
            x *= invLen * SKY_DISTANCE; y *= invLen * SKY_DISTANCE; z *= invLen * SKY_DISTANCE;
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
    private static void renderSun(PoseStack poseStack, double cycleProgress) {
        float sunAngleDeg = (float) (cycleProgress * 360.0) - 90.0F;
        float sunAngleRad = (float) Math.toRadians(sunAngleDeg);
        Vector3f sunDir = new Vector3f((float) Math.cos(sunAngleRad), (float) Math.sin(sunAngleRad), 0.0F);
        float sunHeight = sunDir.y();
        float alpha = Mth.clamp((sunHeight + 0.05F) / 0.10F, 0.0F, 1.0F);
        alpha = alpha * alpha * (3 - 2 * alpha);
        if (alpha <= 0.01F) return;
        poseStack.pushPose();
        Vector4f worldPos = new Vector4f(sunDir.x() * SKY_DISTANCE, sunDir.y() * SKY_DISTANCE, sunDir.z() * SKY_DISTANCE, 1.0F);
        RenderSystem.setShader(GameRenderer::getPositionColorShader);
        drawBillboardDisc(poseStack, worldPos, SKY_DISTANCE, SUN_ANGULAR_RADIUS_DEG * 3.0F, 1.0F, 0.95F, 0.75F, 0.25F * alpha, 24);
        drawBillboardDisc(poseStack, worldPos, SKY_DISTANCE, SUN_ANGULAR_RADIUS_DEG, 1.0F, 1.0F, 1.0F, alpha, 24);
        poseStack.popPose();
    }
    private static void renderEarth(PoseStack poseStack, double cycleProgress) {
        poseStack.pushPose();
        Vector4f worldPos = new Vector4f(EARTH_DIRECTION.x() * SKY_DISTANCE, EARTH_DIRECTION.y() * SKY_DISTANCE, EARTH_DIRECTION.z() * SKY_DISTANCE, 1.0F);
        float sunAngleDeg = (float) (cycleProgress * 360.0) - 90.0F;
        float sunHeight = (float) Math.sin(Math.toRadians(sunAngleDeg));
        float brightness = Mth.clamp((sunHeight + 1.0F) * 0.5F, 0.0F, 1.0F);
        brightness = 0.03F + 0.97F * brightness;
        RenderSystem.setShader(GameRenderer::getPositionTexColorShader);
        RenderSystem.setShaderTexture(0, EARTH_TEXTURE);
        RenderSystem.setShaderColor(brightness, brightness, brightness, 1.0F);
        drawBillboardTexturedDisc(poseStack, worldPos, SKY_DISTANCE, EARTH_ANGULAR_RADIUS_DEG, 32);
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
        poseStack.popPose();
    }
    private static Vector3f[] billboardBasis(Vector4f worldPos) {
        Vector3f normal = new Vector3f(worldPos.x(), worldPos.y(), worldPos.z()).normalize();
        Vector3f up = Math.abs(normal.y()) < 0.99F ? new Vector3f(0, 1, 0) : new Vector3f(1, 0, 0);
        Vector3f right = new Vector3f(up).cross(normal).normalize();
        Vector3f trueUp = new Vector3f(normal).cross(right).normalize();
        return new Vector3f[]{right, trueUp};
    }
    private static void drawBillboardDisc(PoseStack poseStack, Vector4f worldPos, float distance, float angularRadiusDeg, float r, float g, float b, float a, int segments) {
        float radius = distance * (float) Math.tan(Math.toRadians(angularRadiusDeg));
        Vector3f[] basis = billboardBasis(worldPos);
        Vector3f right = basis[0], up = basis[1];
        Matrix4f pose = poseStack.last().pose();
        BufferBuilder builder = Tesselator.getInstance().getBuilder();
        builder.begin(VertexFormat.Mode.TRIANGLE_FAN, DefaultVertexFormat.POSITION_COLOR);
        builder.vertex(pose, worldPos.x(), worldPos.y(), worldPos.z()).color(r, g, b, a).endVertex();
        float edgeAlpha = a < 0.99F ? 0.0F : a;
        for (int i = 0; i <= segments; i++) {
            double t = (i / (double) segments) * Math.PI * 2.0;
            float ox = (float) (right.x() * Math.cos(t) + up.x() * Math.sin(t)) * radius;
            float oy = (float) (right.y() * Math.cos(t) + up.y() * Math.sin(t)) * radius;
            float oz = (float) (right.z() * Math.cos(t) + up.z() * Math.sin(t)) * radius;
            builder.vertex(pose, worldPos.x() + ox, worldPos.y() + oy, worldPos.z() + oz).color(r, g, b, edgeAlpha).endVertex();
        }
        Tesselator.getInstance().end();
    }
    private static void drawBillboardTexturedDisc(PoseStack poseStack, Vector4f worldPos, float distance, float angularRadiusDeg, int segments) {
        float radius = distance * (float) Math.tan(Math.toRadians(angularRadiusDeg));
        Vector3f[] basis = billboardBasis(worldPos);
        Vector3f right = basis[0], up = basis[1];
        Matrix4f pose = poseStack.last().pose();
        BufferBuilder builder = Tesselator.getInstance().getBuilder();
        builder.begin(VertexFormat.Mode.TRIANGLE_FAN, DefaultVertexFormat.POSITION_TEX_COLOR);
        builder.vertex(pose, worldPos.x(), worldPos.y(), worldPos.z()).uv(0.5F, 0.5F).color(1.0F, 1.0F, 1.0F, 1.0F).endVertex();
        for (int i = 0; i <= segments; i++) {
            double t = (i / (double) segments) * Math.PI * 2.0;
            float cx = (float) Math.cos(t), cy = (float) Math.sin(t);
            float ox = (right.x() * cx + up.x() * cy) * radius;
            float oy = (right.y() * cx + up.y() * cy) * radius;
            float oz = (right.z() * cx + up.z() * cy) * radius;
            builder.vertex(pose, worldPos.x() + ox, worldPos.y() + oy, worldPos.z() + oz).uv(0.5F + cx * 0.5F, 0.5F + cy * 0.5F).color(1.0F, 1.0F, 1.0F, 1.0F).endVertex();
        }
        Tesselator.getInstance().end();
    }
}