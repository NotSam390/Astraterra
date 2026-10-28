package com.astraterra.mod.planet;

import com.astraterra.mod.Astraterra;
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
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.common.ForgeMod;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.living.LivingEvent;
import net.minecraftforge.event.entity.living.LivingFallEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.UUID;

/**
 * Applies each planet's {@link Planet#gravity()} to every entity on it.
 *
 * - Living entities (players, mobs): Forge's ENTITY_GRAVITY attribute gets a modifier.
 * - Non-living entities (items, falling blocks, TNT, XP orbs, thrown projectiles):
 *   after vanilla applied full gravity, we give back (1 - gravity) * g.
 *   Runs on the server here, and on the client in PlanetGravityClient (the client
 *   simulates these entities too, so both sides must apply the same correction).
 * - Fall damage: vanilla formula, fed a fall distance scaled by gravity.
 */
@Mod.EventBusSubscriber(modid = Astraterra.MODID)
public class PlanetGravity {

    private static final UUID MODIFIER_ID = UUID.fromString("4f6c1d0e-8a52-4b7e-9c35-2d1a7e6b90f3");
    private static final String MODIFIER_NAME = "astraterra_planet_gravity";

    // ---------- living entities ----------

    @SubscribeEvent
    public static void onLivingTick(LivingEvent.LivingTickEvent event) {
        LivingEntity entity = event.getEntity();
        AttributeInstance attribute = entity.getAttribute(ForgeMod.ENTITY_GRAVITY.get());
        if (attribute == null) return;

        Planet planet = Planets.get(entity.level());
        boolean wantModifier = planet != null && planet.gravity() != 1.0;
        double wantAmount = wantModifier ? planet.gravity() - 1.0 : 0.0; // MULTIPLY_TOTAL: value * (1 + amount)

        AttributeModifier current = attribute.getModifier(MODIFIER_ID);
        if (current != null && (!wantModifier || current.getAmount() != wantAmount)) {
            attribute.removeModifier(MODIFIER_ID);
            current = null;
        }
        if (wantModifier && current == null) {
            attribute.addTransientModifier(new AttributeModifier(
                    MODIFIER_ID, MODIFIER_NAME, wantAmount, AttributeModifier.Operation.MULTIPLY_TOTAL));
        }
    }

    // ---------- fall damage ----------

    @SubscribeEvent
    public static void onFall(LivingFallEvent event) {
        Planet planet = Planets.get(event.getEntity().level());
        if (planet != null) {
            event.setDistance((float) (event.getDistance() * planet.gravity()));
        }
    }

    // ---------- non-living entities (server side) ----------

    @SubscribeEvent
    public static void onLevelTick(TickEvent.LevelTickEvent event) {
        if (event.phase != TickEvent.Phase.END || event.side.isClient()) return;
        if (!(event.level instanceof ServerLevel level)) return;

        Planet planet = Planets.get(level);
        if (planet == null || planet.gravity() == 1.0) return;

        for (Entity entity : level.getAllEntities()) {
            compensate(entity, planet.gravity());
        }
    }

    /** Shared by server and client. Call once per tick, after the entity has ticked. */
    public static void compensate(Entity entity, double gravity) {
        if (entity instanceof LivingEntity) return; // handled by the attribute
        if (entity.isNoGravity() || entity.isInWater() || entity.isInLava()) return;

        double g = vanillaGravity(entity);
        if (g == 0.0) return;

        Vec3 v = entity.getDeltaMovement();
        entity.setDeltaMovement(v.x, v.y + g * (1.0 - gravity), v.z);
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
