package com.jiggneshgaming.azyron.sprint;

import com.jiggneshgaming.azyron.config.AzyronConfig;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;

/**
 * AZYRON CLIENT — toggle sprint.
 *
 * <p>When enabled, the player is forced to keep sprinting (like holding
 * Ctrl permanently), respecting hunger and spectator/flying states.</p>
 */
public final class ToggleSprint {

    private ToggleSprint() {
    }

    public static void tick() {
        if (!AzyronConfig.get().toggleSprint) {
            return;
        }
        MinecraftClient client = MinecraftClient.getInstance();
        ClientPlayerEntity player = client.player;
        if (player == null) {
            return;
        }
        if (player.isSpectator()) {
            return;
        }
        boolean sprintForced = canSprint(player);
        if (sprintForced && !player.isSprinting()) {
            player.setSprinting(true);
        }
    }

    private static boolean canSprint(ClientPlayerEntity player) {
        try {
            // Sprint requires food level > 6 (vanilla hunger rules).
            int food = player.getHungerManager().getFoodLevel();
            if (food <= 6) {
                boolean invulnerable = false;
                try {
                    invulnerable = player.getAbilities().invulnerable;
                } catch (Throwable ignored) {
                    // abilities unavailable — keep hunger rule strict
                }
                if (!invulnerable) {
                    return false;
                }
            }
        } catch (Throwable ignored) {
            // If the hunger API moved, err on the side of sprinting.
        }
        return !player.isUsingItem();
    }

    public static boolean isActive() {
        return AzyronConfig.get().toggleSprint;
    }
}
