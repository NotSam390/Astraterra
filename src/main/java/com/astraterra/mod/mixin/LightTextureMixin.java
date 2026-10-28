package com.astraterra.mod.mixin;

import com.astraterra.mod.client.sky.Daylight;
import com.astraterra.mod.planet.Planet;
import com.astraterra.mod.planet.Planets;
import com.mojang.blaze3d.platform.NativeImage;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.world.effect.MobEffects;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Makes nights dark on planets with Planet.darkNights(), using real lighting.
 *
 * The lightmap is a 16x16 image: x = block light level, y = sky light level.
 * Row y = 0 is "no sky light at all", i.e. what you see lit by block light only.
 * At night we blend every row toward row 0, so the sky stops lighting the world
 * and only block light (torches, lamps) remains. Light sources still work,
 * everything else goes dark. Day is untouched (night factor 0).
 *
 * Injected right before vanilla uploads the image to the GPU, inside the branch
 * that only runs when vanilla actually rebuilt the lightmap, so it is never
 * applied twice to the same pixels.
 */
@Mixin(LightTexture.class)
public abstract class LightTextureMixin {

    @Shadow
    @Final
    private NativeImage lightPixels;

    @Inject(
            method = "updateLightTexture",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/texture/DynamicTexture;upload()V")
    )
    private void astraterra$darkenPlanetNight(float partialTick, CallbackInfo ci) {
        Minecraft mc = Minecraft.getInstance();
        ClientLevel level = mc.level;
        Planet planet = Planets.get(level);
        if (planet == null || !planet.darkNights()) return;
        if (mc.player != null && mc.player.hasEffect(MobEffects.NIGHT_VISION)) return;

        float night = Daylight.nightFactor(level.getSunAngle(partialTick));
        if (night <= 0.001F) return;

        for (int x = 0; x < 16; x++) {
            int base = this.lightPixels.getPixelRGBA(x, 0);
            for (int y = 1; y < 16; y++) {
                int cur = this.lightPixels.getPixelRGBA(x, y);
                this.lightPixels.setPixelRGBA(x, y, astraterra$lerpColor(cur, base, night));
            }
        }
    }

    // Colors are packed ABGR. Blend each channel separately, keep alpha opaque.
    private static int astraterra$lerpColor(int from, int to, float t) {
        int r = astraterra$lerpChannel(from & 0xFF, to & 0xFF, t);
        int g = astraterra$lerpChannel((from >> 8) & 0xFF, (to >> 8) & 0xFF, t);
        int b = astraterra$lerpChannel((from >> 16) & 0xFF, (to >> 16) & 0xFF, t);
        return 0xFF000000 | (b << 16) | (g << 8) | r;
    }

    private static int astraterra$lerpChannel(int from, int to, float t) {
        return Math.round(from + (to - from) * t);
    }
}