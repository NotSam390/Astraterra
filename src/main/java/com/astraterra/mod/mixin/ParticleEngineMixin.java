package com.astraterra.mod.mixin;

import com.astraterra.mod.planet.Planet;
import com.astraterra.mod.planet.Planets;

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
 * by the gravity of the planet the client is on.
 */
@Mixin(ParticleEngine.class)
public class ParticleEngineMixin {

    @Inject(method = "add(Lnet/minecraft/client/particle/Particle;)V", at = @At("HEAD"))
    private void astraterra$planetParticleGravity(Particle particle, CallbackInfo ci) {
        ClientLevel level = Minecraft.getInstance().level;
        Planet planet = Planets.get(level);
        if (planet == null || planet.gravity() == 1.0) return;

        ParticleAccessor accessor = (ParticleAccessor) particle;
        accessor.astraterra$setGravity((float) (accessor.astraterra$getGravity() * planet.gravity()));
    }
}