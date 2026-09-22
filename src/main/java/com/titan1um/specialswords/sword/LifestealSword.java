package com.titan1um.specialswords.sword;

import com.titan1um.specialswords.SwordUtils;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.item.ItemStack;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.UUID;

public final class LifestealSword {

    private static final int PROC_PERCENT = 10;
    private static final int MAX_HEALTH_BOOST_AMPLIFIER = 4;
    private static final int HEALTH_BOOST_DURATION = 20 * 60;
    private static final int INFINITE_EFFECT_DURATION = -1;
    private static final long MAX_COOLDOWN_MS = 60_000L;
    private static final int DURABILITY_COST = 10;

    private static final Map<UUID, Integer> LIFESTEAL_LEVELS = new HashMap<>();
    private static final Map<UUID, Long> MAX_COOLDOWNS = new HashMap<>();
    private static final Map<UUID, StatusEffectInstance> SAVED_REGENERATION = new HashMap<>();

    private LifestealSword() {
    }

    public static boolean matches(ItemStack stack) {
        return SwordUtils.isSpecialSword(stack, SwordUtils.LIFESTEAL_SWORD);
    }

    public static void handleHit(
            ServerPlayerEntity attacker,
            LivingEntity target,
            ItemStack weapon
    ) {
        if (!SwordUtils.rollPercent(attacker.getEntityWorld(), PROC_PERCENT)) {
            return;
        }

        long cooldown = remaining(MAX_COOLDOWNS, attacker.getUuid());

        if (cooldown > 0L) {
            SwordUtils.actionBar(
                    attacker,
                    Text.translatable(
                            "specialswords.lifesteal.cooldown",
                            cooldownSeconds(cooldown)
                    ),
                    Formatting.RED
            );
            return;
        }

        int currentLevel = getCurrentLevel(attacker);

        if (currentLevel >= MAX_HEALTH_BOOST_AMPLIFIER) {
            return;
        }

        int newLevel = currentLevel + 1;
        int heartsGranted = newLevel * 2;

        LIFESTEAL_LEVELS.put(attacker.getUuid(), newLevel);

        attacker.addStatusEffect(new StatusEffectInstance(
                StatusEffects.HEALTH_BOOST,
                HEALTH_BOOST_DURATION,
                newLevel - 1,
                false,
                false,
                true
        ));

        attacker.heal(heartsGranted * 2.0F);

        attacker.getEntityWorld().playSound(
                null,
                attacker.getX(),
                attacker.getY(),
                attacker.getZ(),
                SoundEvents.ENTITY_ENDER_DRAGON_GROWL,
                SoundCategory.PLAYERS,
                1.0F,
                1.0F
        );

        weapon.damage(
                DURABILITY_COST,
                attacker,
                net.minecraft.entity.EquipmentSlot.MAINHAND
        );

        SwordUtils.actionBar(
                attacker,
                Text.translatable(
                        "specialswords.lifesteal.success",
                        heartsGranted
                ),
                Formatting.GREEN
        );

        if (newLevel == MAX_HEALTH_BOOST_AMPLIFIER) {
            SwordUtils.actionBar(
                    attacker,
                    Text.translatable("specialswords.lifesteal.max"),
                    Formatting.GREEN
            );
        }
    }

    public static void tick(MinecraftServer server) {
        Iterator<Map.Entry<UUID, Integer>> iterator =
                LIFESTEAL_LEVELS.entrySet().iterator();

        while (iterator.hasNext()) {
            Map.Entry<UUID, Integer> entry = iterator.next();
            UUID uuid = entry.getKey();

            ServerPlayerEntity player =
                    server.getPlayerManager().getPlayer(uuid);

            if (player == null) {
                iterator.remove();
                MAX_COOLDOWNS.remove(uuid);
                SAVED_REGENERATION.remove(uuid);
                continue;
            }

            int level = entry.getValue();

            if (level >= MAX_HEALTH_BOOST_AMPLIFIER
                    && player.getStatusEffect(StatusEffects.HEALTH_BOOST) == null
                    && !MAX_COOLDOWNS.containsKey(uuid)) {
                MAX_COOLDOWNS.put(
                        uuid,
                        System.currentTimeMillis() + MAX_COOLDOWN_MS
                );
            }

            if (level >= MAX_HEALTH_BOOST_AMPLIFIER
                    && MAX_COOLDOWNS.containsKey(uuid)
                    && remaining(MAX_COOLDOWNS, uuid) == 0L) {
                iterator.remove();
            }
        }

        for (ServerPlayerEntity player : server.getPlayerManager().getPlayerList()) {
            boolean holding = matches(player.getMainHandStack());

            UUID uuid = player.getUuid();

            if (holding) {
                if (!SAVED_REGENERATION.containsKey(uuid)) {
                    SAVED_REGENERATION.put(
                            uuid,
                            player.getStatusEffect(StatusEffects.REGENERATION)
                    );
                }

                if (player.getStatusEffect(StatusEffects.REGENERATION) == null) {
                    player.addStatusEffect(new StatusEffectInstance(
                            StatusEffects.REGENERATION,
                            INFINITE_EFFECT_DURATION,
                            1,
                            false,
                            false,
                            false
                    ));
                }

                continue;
            }

            if (!SAVED_REGENERATION.containsKey(player.getUuid())) {
                continue;
            }

            StatusEffectInstance current =
                    player.getStatusEffect(StatusEffects.REGENERATION);

            if (current != null
                    && current.getAmplifier() == 1
                    && current.getDuration() <= 40) {
                player.removeStatusEffect(StatusEffects.REGENERATION);
            }

            StatusEffectInstance saved =
                    SAVED_REGENERATION.remove(player.getUuid());

            if (saved != null) {
                player.addStatusEffect(saved);
            }
        }
    }

    public static void clearForPlayer(ServerPlayerEntity player) {
        UUID uuid = player.getUuid();

        if (LIFESTEAL_LEVELS.remove(uuid) != null) {
            StatusEffectInstance boost =
                    player.getStatusEffect(StatusEffects.HEALTH_BOOST);

            if (boost != null && boost.getAmplifier() <= MAX_HEALTH_BOOST_AMPLIFIER) {
                player.removeStatusEffect(StatusEffects.HEALTH_BOOST);
            }
        }

        StatusEffectInstance currentRegen =
                player.getStatusEffect(StatusEffects.REGENERATION);

        if (currentRegen != null
                && currentRegen.getAmplifier() == 1
                && currentRegen.getDuration() <= 40) {
            player.removeStatusEffect(StatusEffects.REGENERATION);
        }

        MAX_COOLDOWNS.remove(uuid);
        SAVED_REGENERATION.remove(uuid);
    }

    private static int getCurrentLevel(ServerPlayerEntity player) {
        Integer trackedLevel = LIFESTEAL_LEVELS.get(player.getUuid());

        if (trackedLevel != null) {
            return trackedLevel;
        }

        StatusEffectInstance currentBoost =
                player.getStatusEffect(StatusEffects.HEALTH_BOOST);

        if (currentBoost == null) {
            return 0;
        }

        return Math.min(
                currentBoost.getAmplifier() + 1,
                MAX_HEALTH_BOOST_AMPLIFIER
        );
    }

    private static long remaining(Map<UUID, Long> cooldowns, UUID uuid) {
        Long endTime = cooldowns.get(uuid);

        if (endTime == null) {
            return 0L;
        }

        long remaining = endTime - System.currentTimeMillis();

        if (remaining <= 0L) {
            cooldowns.remove(uuid);
            return 0L;
        }

        return remaining;
    }

    private static long cooldownSeconds(long remainingMillis) {
        return Math.max(1L, (remainingMillis + 999L) / 1000L);
    }
}
