package com.jiggneshgaming.azyron.cosmetics;

import com.jiggneshgaming.azyron.config.AzyronConfig;
import com.jiggneshgaming.azyron.core.config.AzyronConfigModel;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.particle.ParticleTypes;

/**
 * AZYRON CLIENT — client-side cosmetic auras.
 *
 * <p>Selected in the cosmetics menu; rendered as lightweight particles
 * orbiting the player. Pure vanilla particle API — no mixins, works on
 * PojavLauncher, visible only to the local player (cosmetics are not yet
 * synced to other players; see docs/COSMETICS.md).</p>
 */
public final class AuraManager {

    private static int ticksSinceSpawn;
    private static float orbitAngle;

    private AuraManager() {
    }

    public static void tick() {
        AzyronConfigModel config = AzyronConfig.get();
        String aura = config.aura == null ? "NONE" : config.aura;
        if ("NONE".equals(aura)) {
            return;
        }
        MinecraftClient client = MinecraftClient.getInstance();
        PlayerEntity player = client.player;
        ClientWorld world = client.world;
        if (player == null || world == null || client.particleManager == null) {
            return;
        }
        if (player.isSpectator()) {
            return;
        }

        ticksSinceSpawn++;
        orbitAngle += 0.28F;
        // Spawn a few orbiting motes per tick; ~100 particles/second max.
        for (int i = 0; i < 2; i++) {
            float angle = orbitAngle + i * (float) Math.PI;
            double radius = 0.75D + 0.1D * Math.sin(orbitAngle * 0.7D);
            double x = player.getX() + Math.cos(angle) * radius;
            double z = player.getZ() + Math.sin(angle) * radius;
            double y = player.getY() + 0.1D + (Math.sin(orbitAngle * 1.3D + i) + 1.0D) * 0.75D;
            double vx = Math.cos(angle + Math.PI / 2) * 0.012D;
            double vz = Math.sin(angle + Math.PI / 2) * 0.012D;
            double vy = 0.008D + 0.006D * Math.sin(orbitAngle * 2.0D + i * 1.7D);
            spawnFor(aura, client, x, y, z, vx, vy, vz);
        }
    }

    private static void spawnFor(String aura, MinecraftClient client,
                                 double x, double y, double z,
                                 double vx, double vy, double vz) {
        try {
            switch (aura) {
                case "END_SPARKLE":
                    client.particleManager.addParticle(ParticleTypes.END_ROD, x, y, z, vx, vy, vz);
                    break;
                case "SOUL_FLAME":
                    client.particleManager.addParticle(ParticleTypes.SOUL_FIRE_FLAME, x, y, z, vx * 0.6D, vy * 0.8D, vz * 0.6D);
                    break;
                case "EMBER":
                    client.particleManager.addParticle(ParticleTypes.FLAME, x, y, z, vx * 0.5D, vy * 0.7D, vz * 0.5D);
                    break;
                case "SNOWFLAKE":
                    client.particleManager.addParticle(ParticleTypes.SNOWFLAKE, x, y, z, vx, vy * 0.4D, vz);
                    break;
                default:
                    break;
            }
        } catch (Throwable ignored) {
            // Particle API unavailable — cosmetics simply don't render.
        }
    }

    public static void reset() {
        ticksSinceSpawn = 0;
        orbitAngle = 0F;
    }
}
