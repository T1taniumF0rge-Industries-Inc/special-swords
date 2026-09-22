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
import java.util.Map;
import java.util.UUID;

public final class LifestealSword {

    private static final int PROC_PERCENT = 10;
    private static final int MAX_HEALTH_BOOST_AMPLIFIER = 4;
    private static final int HEALTH_BOOST_DURATION = 20 * 60;
    private static final int DURABILITY_COST = 10;

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

        StatusEffectInstance currentBoost =
                attacker.getStatusEffect(StatusEffects.HEALTH_BOOST);

        int currentAmplifier = currentBoost == null
                ? -1
                : currentBoost.getAmplifier();

        boolean reachedMaximum = currentAmplifier < MAX_HEALTH_BOOST_AMPLIFIER;

        int amplifier = Math.min(
                currentAmplifier + 1,
                MAX_HEALTH_BOOST_AMPLIFIER
        );

        int heartsGranted = (amplifier + 1) * 2;

        attacker.addStatusEffect(new StatusEffectInstance(
                StatusEffects.HEALTH_BOOST,
                HEALTH_BOOST_DURATION,
                amplifier,
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

        if (reachedMaximum && amplifier == MAX_HEALTH_BOOST_AMPLIFIER) {
            SwordUtils.actionBar(
                    attacker,
                    Text.translatable("specialswords.lifesteal.max"),
                    Formatting.RED
            );
        }
    }

    public static void tick(MinecraftServer server) {
        for (ServerPlayerEntity player : server.getPlayerManager().getPlayerList()) {
            boolean holding = matches(player.getMainHandStack());

            if (holding) {
                if (!SAVED_REGENERATION.containsKey(player.getUuid())) {
                    SAVED_REGENERATION.put(
                            player.getUuid(),
                            player.getStatusEffect(StatusEffects.REGENERATION)
                    );
                }

                player.addStatusEffect(new StatusEffectInstance(
                        StatusEffects.REGENERATION,
                        40,
                        1,
                        false,
                        false,
                        false
                ));
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
}
