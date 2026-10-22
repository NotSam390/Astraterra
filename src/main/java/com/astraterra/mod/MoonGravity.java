package com.astraterra.mod;

import java.util.UUID;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.ExperienceOrb;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.item.FallingBlockEntity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.item.PrimedTnt;
import net.minecraft.world.entity.projectile.ThrowableProjectile;
import net.minecraft.world.entity.projectile.ThrownExperienceBottle;
import net.minecraft.world.entity.projectile.ThrownPotion;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.common.ForgeMod;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.living.LivingEvent;
import net.minecraftforge.event.entity.living.LivingFallEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * Moon gravity = 1/6 of Overworld gravity, for every entity in the Moon dimension.
 *
 * - Living entities (players, mobs): Forge's ENTITY_GRAVITY attribute gets a modifier.
 * - Non-living entities (items, falling blocks, TNT, XP orbs, thrown projectiles):
 *   after vanilla applied full gravity, we give back (1 - FACTOR) * g.
 *   Runs on the server here, and on the client in MoonGravityClient (the client
 *   simulates these entities too, so both sides must apply the same correction).
 * - Fall damage: vanilla formula, fed a fall distance scaled by FACTOR.
 */
@Mod.EventBusSubscriber(modid = "astraterra")
public class MoonGravity {

    /** CHECK: must match your Moon dimension's id (data/astraterra/dimension/moon.json). */
    public static final ResourceKey<Level> MOON =
            ResourceKey.create(Registries.DIMENSION, new ResourceLocation("astraterra", "moon"));

    public static final double FACTOR = 1.0 / 6.0;

    private static final AttributeModifier MOON_GRAVITY = new AttributeModifier(
            UUID.fromString("4f6c1d0e-8a52-4b7e-9c35-2d1a7e6b90f3"),
            "astraterra_moon_gravity",
            FACTOR - 1.0, // MULTIPLY_TOTAL: value * (1 + amount) = value * FACTOR
            AttributeModifier.Operation.MULTIPLY_TOTAL);

    public static boolean isMoon(Level level) {
        return level.dimension().equals(MOON);
    }

    // ---------- living entities ----------

    @SubscribeEvent
    public static void onLivingTick(LivingEvent.LivingTickEvent event) {
        LivingEntity entity = event.getEntity();
        AttributeInstance gravity = entity.getAttribute(ForgeMod.ENTITY_GRAVITY.get());
        if (gravity == null) return;

        boolean onMoon = isMoon(entity.level());
        boolean hasModifier = gravity.hasModifier(MOON_GRAVITY);

        if (onMoon && !hasModifier) {
            gravity.addTransientModifier(MOON_GRAVITY);
        } else if (!onMoon && hasModifier) {
            gravity.removeModifier(MOON_GRAVITY);
        }
    }

    // ---------- fall damage ----------

    @SubscribeEvent
    public static void onFall(LivingFallEvent event) {
        if (isMoon(event.getEntity().level())) {
            event.setDistance((float) (event.getDistance() * FACTOR));
        }
    }

    // ---------- non-living entities (server side) ----------

    @SubscribeEvent
    public static void onLevelTick(TickEvent.LevelTickEvent event) {
        if (event.phase != TickEvent.Phase.END || event.side.isClient()) return;
        if (!(event.level instanceof ServerLevel level) || !isMoon(level)) return;

        for (Entity entity : level.getAllEntities()) {
            compensate(entity);
        }
    }

    /** Shared by server and client. Call once per tick, after the entity has ticked. */
    public static void compensate(Entity entity) {
        if (entity instanceof LivingEntity) return; // handled by the attribute
        if (entity.isNoGravity() || entity.isInWater() || entity.isInLava()) return;

        double g = vanillaGravity(entity);
        if (g == 0.0) return;

        Vec3 v = entity.getDeltaMovement();
        entity.setDeltaMovement(v.x, v.y + g * (1.0 - FACTOR), v.z);
    }

    /** Vanilla per-tick gravity constants (1.20.1). Verify against the decompiled source if something feels off. */
    private static double vanillaGravity(Entity e) {
        if (e instanceof ItemEntity) return 0.04;
        if (e instanceof FallingBlockEntity) return 0.04;
        if (e instanceof PrimedTnt) return 0.04;
        if (e instanceof ExperienceOrb) return 0.03;
        if (e instanceof ThrownPotion) return 0.05;
        if (e instanceof ThrownExperienceBottle) return 0.07;
        if (e instanceof ThrowableProjectile) return 0.03; // snowball, egg, ender pearl
        return 0.0;
    }
}