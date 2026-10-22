package com.astraterra.mod.mixin;

import com.astraterra.mod.MoonGravity;

import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleEngine;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Runs once per particle, right after its constructor chain has finished
 * (so the subclass's final gravity value is already set), and scales it
 * by the Moon factor when the client is in the Moon dimension.
 */
@Mixin(ParticleEngine.class)
public class ParticleEngineMixin {

    @Inject(method = "add(Lnet/minecraft/client/particle/Particle;)V", at = @At("HEAD"))
    private void astraterra$moonParticleGravity(Particle particle, CallbackInfo ci) {
        ClientLevel level = Minecraft.getInstance().level;
        if (level == null || !MoonGravity.isMoon(level)) return;

        ParticleAccessor accessor = (ParticleAccessor) particle;
        accessor.astraterra$setGravity((float) (accessor.astraterra$getGravity() * MoonGravity.FACTOR));
    }
}