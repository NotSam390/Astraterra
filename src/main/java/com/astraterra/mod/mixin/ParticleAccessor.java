package com.astraterra.mod.mixin;

import net.minecraft.client.particle.Particle;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

/** Exposes Particle's protected "gravity" field to ParticleEngineMixin. */
@Mixin(Particle.class)
public interface ParticleAccessor {

    @Accessor("gravity")
    float astraterra$getGravity();

    @Accessor("gravity")
    void astraterra$setGravity(float gravity);
}