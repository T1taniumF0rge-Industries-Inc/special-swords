package com.titan1um.specialswords.sword;

import com.titan1um.specialswords.SwordUtils;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.enchantment.Enchantments;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.LivingEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.network.packet.s2c.play.EntityVelocityUpdateS2CPacket;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Formatting;
import net.minecraft.util.Hand;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.WorldEvents;

import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public final class DashSword {

    private static final long FORWARD_COOLDOWN_MS = 3_000L;
    private static final long UPWARD_COOLDOWN_MS = 10_000L;

    private static final int FORWARD_DURABILITY = 5;
    private static final int UPWARD_DURABILITY = 10;
    private static final int MACE_DURABILITY = 15;

    private static final double FORWARD_SPEED = 1.17D;
    private static final int FORWARD_TICKS = 6;

    private static final double UPWARD_VELOCITY = 1.75D;

    private static final double WIND_RADIUS = 8.0D;
    private static final double LANDING_RADIUS = 5.0D;
    private static final double LANDING_VERTICAL_RADIUS = 5.0D;

    private static final Map<UUID, Long> FORWARD_COOLDOWNS = new HashMap<>();
    private static final Map<UUID, Long> UPWARD_COOLDOWNS = new HashMap<>();

    private static final Map<UUID, ForwardState> FORWARD_STATES = new HashMap<>();
    private static final Map<UUID, UpwardState> UPWARD_STATES = new HashMap<>();

    private DashSword() {
    }

    public static boolean matches(ItemStack stack) {
        return SwordUtils.isSpecialSword(stack, SwordUtils.DASH_SWORD);
    }

    public static ActionResult tryActivate(
            ServerPlayerEntity player,
            ItemStack stack,
            Hand hand
    ) {
        if (!matches(stack)) {
            return ActionResult.PASS;
        }

        if (player.getPitch() <= -60.0F) {
            return activateUpwardDash(player, stack, hand);
        }

        return activateForwardDash(player, stack, hand);
    }

    private static ActionResult activateForwardDash(
            ServerPlayerEntity player,
            ItemStack stack,
            Hand hand
    ) {
        long remaining = remaining(FORWARD_COOLDOWNS, player.getUuid());

        if (remaining > 0L) {
            sendCooldownMessage(
                    player,
                    "specialswords.forward_dash.cooldown",
                    remaining
            );
            return ActionResult.FAIL;
        }

        Vec3d direction = player.getRotationVector();
        direction = new Vec3d(direction.x, 0.0D, direction.z);

        if (direction.lengthSquared() < 0.0001D) {
            direction = new Vec3d(0.0D, 0.0D, 1.0D);
        }

        direction = direction.normalize();

        FORWARD_COOLDOWNS.put(
                player.getUuid(),
                System.currentTimeMillis() + FORWARD_COOLDOWN_MS
        );

        FORWARD_STATES.put(
                player.getUuid(),
                new ForwardState(direction)
        );

        stack.damage(FORWARD_DURABILITY, player, hand);
        playDashSound(player);

        SwordUtils.actionBar(
                player,
                Text.translatable("specialswords.forward_dash.success"),
                Formatting.GREEN
        );

        return ActionResult.SUCCESS;
    }

    private static ActionResult activateUpwardDash(
            ServerPlayerEntity player,
            ItemStack stack,
            Hand hand
    ) {
        long remaining = remaining(UPWARD_COOLDOWNS, player.getUuid());

        if (remaining > 0L) {
            sendCooldownMessage(
                    player,
                    "specialswords.upward_dash.cooldown",
                    remaining
            );
            return ActionResult.FAIL;
        }

        UPWARD_COOLDOWNS.put(
                player.getUuid(),
                System.currentTimeMillis() + UPWARD_COOLDOWN_MS
        );

        UPWARD_STATES.put(
                player.getUuid(),
                new UpwardState()
        );

        player.setVelocity(new Vec3d(
                player.getVelocity().x,
                UPWARD_VELOCITY,
                player.getVelocity().z
        ));
        player.velocityDirty = true;
        player.fallDistance = 0.0F;

        applyWindChargeStyleKnockback(player);

        stack.damage(UPWARD_DURABILITY, player, hand);
        playDashSound(player);

        SwordUtils.actionBar(
                player,
                Text.translatable("specialswords.upward_dash.success"),
                Formatting.GREEN
        );

        sendVelocity(player);

        return ActionResult.SUCCESS;
    }

    public static void tick(MinecraftServer server) {
        tickForwardDashes(server);
        tickUpwardDashes(server);
    }

    public static boolean shouldCancelFallDamage(ServerPlayerEntity player) {
        UUID uuid = player.getUuid();

        if (FORWARD_STATES.containsKey(uuid)) {
            return true;
        }

        if (UPWARD_STATES.containsKey(uuid)) {
            return true;
        }

        return matches(player.getMainHandStack());
    }

    private static void tickForwardDashes(MinecraftServer server) {
        Iterator<Map.Entry<UUID, ForwardState>> iterator =
                FORWARD_STATES.entrySet().iterator();

        while (iterator.hasNext()) {
            Map.Entry<UUID, ForwardState> entry = iterator.next();
            ServerPlayerEntity player =
                    server.getPlayerManager().getPlayer(entry.getKey());

            if (player == null) {
                iterator.remove();
                continue;
            }

            ForwardState state = entry.getValue();

            if (state.ticksElapsed >= FORWARD_TICKS
                    || player.horizontalCollision) {
                iterator.remove();
                continue;
            }

            player.setVelocity(state.direction.multiply(FORWARD_SPEED));
            player.velocityDirty = true;
            sendVelocity(player);

            state.ticksElapsed++;
        }
    }

    private static void tickUpwardDashes(MinecraftServer server) {
        Iterator<Map.Entry<UUID, UpwardState>> iterator =
                UPWARD_STATES.entrySet().iterator();

        while (iterator.hasNext()) {
            Map.Entry<UUID, UpwardState> entry = iterator.next();
            ServerPlayerEntity player =
                    server.getPlayerManager().getPlayer(entry.getKey());

            if (player == null) {
                iterator.remove();
                continue;
            }

            UpwardState state = entry.getValue();

            if (player.getVelocity().y < -0.05D) {
                state.startedFalling = true;
            }

            if (state.startedFalling && player.isOnGround()) {
                if (isHolding(player)) {
                    performMaceSmash(player);
                }

                iterator.remove();
                continue;
            }

            if (++state.ticksElapsed > 600) {
                iterator.remove();
            }
        }
    }

    private static void performMaceSmash(ServerPlayerEntity attacker) {
        ServerWorld world = attacker.getEntityWorld();

        List<LivingEntity> targets = world.getEntitiesByClass(
                LivingEntity.class,
                attacker.getBoundingBox().expand(
                        LANDING_RADIUS,
                        LANDING_VERTICAL_RADIUS,
                        LANDING_RADIUS
                ),
                target -> target != attacker
                        && target.isAlive()
                        && Math.abs(target.getY() - attacker.getY()) <= LANDING_VERTICAL_RADIUS
                        && target.getEntityPos().subtract(attacker.getEntityPos()).horizontalLengthSquared()
                        <= LANDING_RADIUS * LANDING_RADIUS
        );

        if (targets.isEmpty()) {
            return;
        }

        var damageSource = world.getDamageSources().maceSmash(attacker);

        ItemStack mace = new ItemStack(Items.MACE);

        var enchantmentRegistry =
                world.getRegistryManager().getOrThrow(
                        net.minecraft.registry.RegistryKeys.ENCHANTMENT
                );

        mace.addEnchantment(
                enchantmentRegistry.getOrThrow(Enchantments.DENSITY),
                5
        );

        mace.addEnchantment(
                enchantmentRegistry.getOrThrow(Enchantments.BREACH),
                4
        );

        world.syncWorldEvent(
                attacker,
                WorldEvents.SMASH_ATTACK,
                attacker.getBlockPos().down(),
                750
        );

        world.playSound(
                null,
                attacker.getX(),
                attacker.getY(),
                attacker.getZ(),
                SoundEvents.ITEM_MACE_SMASH_GROUND_HEAVY,
                SoundCategory.PLAYERS,
                1.0F,
                1.0F
        );

        boolean hitAnything = false;

        for (LivingEntity target : targets) {
            float damagePerFallenBlock =
                    EnchantmentHelper.getSmashDamagePerFallenBlock(
                            world,
                            mace,
                            target,
                            damageSource,
                            0.5F
                    );

            float damage = EnchantmentHelper.getDamage(
                    world,
                    mace,
                    target,
                    damageSource,
                    8.0F + 14.0F * damagePerFallenBlock
            );

            if (target.damage(world, damageSource, damage)) {
                hitAnything = true;
            }
        }

        if (hitAnything && matches(attacker.getMainHandStack())) {
            attacker.getMainHandStack().damage(
                    MACE_DURABILITY,
                    attacker,
                    EquipmentSlot.MAINHAND
            );
        }
    }

    private static void applyWindChargeStyleKnockback(
            ServerPlayerEntity player
    ) {
        ServerWorld world = player.getEntityWorld();

        for (Entity target : world.getOtherEntities(
                player,
                player.getBoundingBox().expand(WIND_RADIUS),
                entity -> entity != player
                        && entity.isAlive()
                        && entity.squaredDistanceTo(player)
                        <= WIND_RADIUS * WIND_RADIUS
        )) {
            Vec3d delta =
                    target.getEntityPos().subtract(player.getEntityPos());

            double horizontalDistance =
                    Math.sqrt(delta.x * delta.x + delta.z * delta.z);

            if (horizontalDistance < 0.001D) {
                continue;
            }

            double distance =
                    Math.min(WIND_RADIUS, horizontalDistance);

            double strength =
                    1.5D * (1.0D - distance / WIND_RADIUS);

            target.addVelocity(
                    delta.x / horizontalDistance * strength,
                    0.55D,
                    delta.z / horizontalDistance * strength
            );

            target.velocityDirty = true;
        }

        world.playSound(
                null,
                player.getX(),
                player.getY(),
                player.getZ(),
                SoundEvents.ENTITY_BREEZE_WIND_BURST,
                SoundCategory.PLAYERS,
                1.0F,
                1.0F
        );
    }

    private static void playDashSound(ServerPlayerEntity player) {
        player.getEntityWorld().playSound(
                null,
                player.getX(),
                player.getY(),
                player.getZ(),
                SoundEvents.ITEM_SPEAR_LUNGE_1,
                SoundCategory.PLAYERS,
                1.0F,
                1.0F
        );
    }

    private static void sendVelocity(ServerPlayerEntity player) {
        player.networkHandler.sendPacket(
                new EntityVelocityUpdateS2CPacket(player)
        );
    }

    private static void sendCooldownMessage(
            ServerPlayerEntity player,
            String translationKeyPrefix,
            long remainingMillis
    ) {
        long seconds = Math.max(1L, (remainingMillis + 999L) / 1000L);
        String key = seconds == 1L
                ? translationKeyPrefix + ".single"
                : translationKeyPrefix + ".plural";

        SwordUtils.actionBar(
                player,
                Text.translatable(key, seconds),
                Formatting.RED
        );
    }

    private static long remaining(
            Map<UUID, Long> cooldowns,
            UUID uuid
    ) {
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

    private static final class ForwardState {

        private final Vec3d direction;
        private int ticksElapsed;

        private ForwardState(Vec3d direction) {
            this.direction = direction;
        }
    }

    private static final class UpwardState {

        private int ticksElapsed;
        private boolean startedFalling;
    }
}
