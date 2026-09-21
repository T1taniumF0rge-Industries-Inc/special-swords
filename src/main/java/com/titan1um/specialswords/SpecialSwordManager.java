package com.titan1um.specialswords;

import com.titan1um.specialswords.sword.DashSword;
import com.titan1um.specialswords.sword.LifestealSword;
import com.titan1um.specialswords.sword.LightningSword;
import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.event.player.UseItemCallback;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.world.World;

public final class SpecialSwordManager {

    private SpecialSwordManager() {
    }

    public static void register() {
        UseItemCallback.EVENT.register(SpecialSwordManager::onUseItem);
        ServerLivingEntityEvents.AFTER_DAMAGE.register(SpecialSwordManager::onAfterDamage);
        ServerLivingEntityEvents.ALLOW_DAMAGE.register(SpecialSwordManager::allowDamage);
        ServerTickEvents.END_SERVER_TICK.register(SpecialSwordManager::onServerTick);
    }

    private static ActionResult onUseItem(PlayerEntity player, World world, Hand hand) {
        if (world.isClient() || !(player instanceof ServerPlayerEntity serverPlayer)) {
            return ActionResult.PASS;
        }

        if (!serverPlayer.isSneaking()) {
            return ActionResult.PASS;
        }

        return DashSword.tryActivate(serverPlayer, serverPlayer.getStackInHand(hand), hand);
    }

    private static void onAfterDamage(
            LivingEntity entity,
            DamageSource source,
            float baseDamageTaken,
            float damageTaken,
            boolean blocked
    ) {
        if (blocked || damageTaken <= 0.0F) {
            return;
        }

        if (!(source.getAttacker() instanceof ServerPlayerEntity attacker)) {
            return;
        }

        ItemStack weapon = source.getWeaponStack();

        if (weapon == null || weapon.isEmpty()) {
            return;
        }

        if (LightningSword.matches(weapon)) {
            LightningSword.handleHit(attacker, entity, weapon);
        } else if (LifestealSword.matches(weapon)) {
            LifestealSword.handleHit(attacker, entity, weapon);
        }
    }

    private static boolean allowDamage(LivingEntity entity, DamageSource source, float amount) {
        if (!source.isOf(net.minecraft.entity.damage.DamageTypes.FALL)) {
            return true;
        }

        if (!(entity instanceof ServerPlayerEntity player)) {
            return true;
        }

        return !DashSword.shouldCancelFallDamage(player);
    }

    private static void onServerTick(MinecraftServer server) {
        DashSword.tick(server);
        LightningSword.tick(server);
        LifestealSword.tick(server);
    }
}
