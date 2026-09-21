package com.titan1um.specialswords.sword;

import com.titan1um.specialswords.SwordUtils;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.mob.HostileEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.Formatting;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

public final class LifestealSword {

    private static final double PROC_CHANCE = 0.10D;
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
        if (!(target instanceof PlayerEntity) && !(target instanceof HostileEntity)) {
            return;
        }

        if (ThreadLocalRandom.current().nextDouble() >= PROC_CHANCE) {
            return;
        }

        StatusEffectInstance currentBoost =
                attacker.getStatusEffect(StatusEffects.HEALTH_BOOST);

        int amplifier = currentBoost == null
                ? 0
                : currentBoost.getAmplifier() + 1;

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
                "Lifesteal! +" + heartsGranted + " hearts.",
                Formatting.GREEN
        );
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
