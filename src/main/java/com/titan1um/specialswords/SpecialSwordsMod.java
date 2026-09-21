package com.titan1um.specialswords;

import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.event.player.UseItemCallback;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.LightningEntity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.SpawnReason;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.damage.DamageTypes;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.enchantment.Enchantments;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Formatting;
import net.minecraft.util.Hand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import net.minecraft.world.WorldEvents;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class SpecialSwordsMod {

    private static final String LIGHTNING_SWORD = "Lightning Sword";
    private static final String LIFESTEAL_SWORD = "Lifesteal Sword";
    private static final String DASH_SWORD = "Dash Sword";

    private static final double LIGHTNING_CHANCE = 0.14;
    private static final double LIFESTEAL_CHANCE = 0.07;
    private static final long FORWARD_COOLDOWN_MS = 3_000L;
    private static final long UPWARD_COOLDOWN_MS = 10_000L;

    private static final double FORWARD_DASH_DISTANCE = 7.0;
    private static final int FORWARD_DASH_TICKS = 6;

    private static final double UPWARD_DASH_HEIGHT = 14.0;
    private static final double UPWARD_PUSH_RADIUS = 2.0;
    private static final double LANDING_SMASH_RADIUS = 5.0;

    private static final int LIGHTNING_STRIKES = 25;
    private static final int LIGHTNING_INTERVAL_TICKS = 4;

    private static final int FORWARD_DURABILITY = 5;
    private static final int UPWARD_DURABILITY = 10;

    private static final int LIGHTNING_DURABILITY = 10;
    private static final int LIFESTEAL_DURABILITY = 10;

    private static final int DENSITY_LEVEL = 5;
    private static final int BREACH_LEVEL = 4;

    private static final Map<UUID, Long> forwardCooldowns =
            new HashMap<>();

    private static final Map<UUID, Long> upwardCooldowns =
            new HashMap<>();

    private static final Map<UUID, ForwardDashState> forwardDashes =
            new HashMap<>();

    private static final Map<UUID, UpwardDashState> upwardDashes =
            new HashMap<>();

    private static final List<LightningTask> lightningTasks =
            new ArrayList<>();

    public static void onInitialize() {
        UseItemCallback.EVENT.register(
                (player, world, hand) -> {
                    if (world.isClient()) {
                        return ActionResult.PASS;
                    }

                    if (!(player instanceof ServerPlayerEntity serverPlayer)) {
                        return ActionResult.PASS;
                    }

                    if (!player.isSneaking()) {
                        return ActionResult.PASS;
                    }

                    ItemStack stack =
                            player.getStackInHand(hand);

                    if (!isSpecialSword(stack, DASH_SWORD)) {
                        return ActionResult.PASS;
                    }

                    return activateDash(
                            serverPlayer,
                            stack
                    );
                }
        );

        ServerLivingEntityEvents.AFTER_DAMAGE.register(
                (
                        entity,
                        source,
                        baseDamageTaken,
                        damageTaken,
                        blocked
                ) -> handleSpecialSwordHit(
                        entity,
                        source,
                        damageTaken,
                        blocked
                )
        );

        ServerLivingEntityEvents.ALLOW_DAMAGE.register(
                (entity, source, amount) ->
                        allowDamage(entity, source)
        );

        ServerTickEvents.END_SERVER_TICK.register(
                SpecialSwordsMod::tickServer
        );
    }

    private static ActionResult activateDash(
            ServerPlayerEntity player,
            ItemStack stack
    ) {
        boolean upward =
                player.getPitch() <= -60.0F;

        if (upward) {
            long remaining =
                    getRemainingCooldown(
                            upwardCooldowns,
                            player.getUuid()
                    );

            if (remaining > 0) {
                sendCooldownMessage(
                        player,
                        "specialswords.upward_dash",
                        remaining
                );

                return ActionResult.FAIL;
            }

            performUpwardDash(
                    player,
                    stack
            );

            return ActionResult.SUCCESS;
        }

        long remaining =
                getRemainingCooldown(
                        forwardCooldowns,
                        player.getUuid()
                );

        if (remaining > 0) {
            sendCooldownMessage(
                    player,
                    "specialswords.forward_dash",
                    remaining
            );

            return ActionResult.FAIL;
        }

        performForwardDash(
                player,
                stack
        );

        return ActionResult.SUCCESS;
    }

    private static void performForwardDash(
            ServerPlayerEntity player,
            ItemStack stack
    ) {
        forwardCooldowns.put(
                player.getUuid(),
                System.currentTimeMillis()
                        + FORWARD_COOLDOWN_MS
        );

        Vec3d direction =
                player.getRotationVector().normalize();

        if (direction.lengthSquared() < 0.0001) {
            direction =
                    new Vec3d(0.0, 0.0, 1.0);
        }

        forwardDashes.put(
                player.getUuid(),
                new ForwardDashState(
                        direction,
                        0
                )
        );

        damageStack(
                stack,
                FORWARD_DURABILITY,
                player
        );

        sendSuccessMessage(
                player,
                "specialswords.forward_dash.success"
        );
    }

    private static void performUpwardDash(
            ServerPlayerEntity player,
            ItemStack stack
    ) {
        upwardCooldowns.put(
                player.getUuid(),
                System.currentTimeMillis()
                        + UPWARD_COOLDOWN_MS
        );

        pushNearbyEntities(
                player,
                UPWARD_PUSH_RADIUS
        );

        player.setVelocity(
                new Vec3d(
                        player.getVelocity().x,
                        1.62738,
                        player.getVelocity().z
                )
        );

        player.velocityDirty = true;
        player.fallDistance = 0.0F;

        upwardDashes.put(
                player.getUuid(),
                new UpwardDashState()
        );

        damageStack(
                stack,
                UPWARD_DURABILITY,
                player
        );

        sendSuccessMessage(
                player,
                "specialswords.upward_dash.success"
        );
    }

    private static void tickServer(
            MinecraftServer server
    ) {
        tickForwardDashes(server);
        tickUpwardDashes(server);
        tickLightning(server);
    }

    private static void tickForwardDashes(
            MinecraftServer server
    ) {
        Iterator<Map.Entry<UUID, ForwardDashState>> iterator =
                forwardDashes.entrySet().iterator();

        while (iterator.hasNext()) {
            Map.Entry<UUID, ForwardDashState> entry =
                    iterator.next();

            ServerPlayerEntity player =
                    server.getPlayerManager()
                            .getPlayer(entry.getKey());

            if (player == null) {
                iterator.remove();
                continue;
            }

            ForwardDashState state =
                    entry.getValue();

            if (state.ticks >= FORWARD_DASH_TICKS) {
                iterator.remove();
                continue;
            }

            double velocity =
                    FORWARD_DASH_DISTANCE
                            / FORWARD_DASH_TICKS;

            player.setVelocity(
                    state.direction.multiply(velocity)
            );

            player.velocityDirty = true;

            state.ticks++;
        }
    }

    private static void tickUpwardDashes(
            MinecraftServer server
    ) {
        Iterator<Map.Entry<UUID, UpwardDashState>> iterator =
                upwardDashes.entrySet().iterator();

        while (iterator.hasNext()) {
            Map.Entry<UUID, UpwardDashState> entry =
                    iterator.next();

            ServerPlayerEntity player =
                    server.getPlayerManager()
                            .getPlayer(entry.getKey());

            if (player == null) {
                iterator.remove();
                continue;
            }

            UpwardDashState state =
                    entry.getValue();

            state.ticks++;

            state.maximumFallDistance =
                    Math.max(
                            state.maximumFallDistance,
                            player.fallDistance
                    );

            if (player.getVelocity().y < -0.05) {
                state.startedFalling = true;
            }

            if (
                    state.startedFalling
                            && player.isOnGround()
                            && player.getVelocity().y <= 0.1
            ) {
                if (isHoldingDashSword(player)) {
                    performLandingSmash(
                            player,
                            state
                    );
                }

                player.fallDistance = 0.0F;
                iterator.remove();

                continue;
            }

            if (state.ticks >= 20 * 30) {
                player.fallDistance = 0.0F;
                iterator.remove();
            }
        }
    }

    private static void performLandingSmash(
            ServerPlayerEntity attacker,
            UpwardDashState state
    ) {
        ServerWorld world =
                attacker.getEntityWorld();

        DamageSource damageSource =
                world.getDamageSources()
                        .maceSmash(attacker);

        ItemStack mace =
                new ItemStack(Items.MACE);

        mace.addEnchantment(
                world.getRegistryManager()
                        .getOrThrow(
                                net.minecraft.registry.RegistryKeys.ENCHANTMENT
                        )
                        .getOrThrow(
                                Enchantments.DENSITY
                        ),
                DENSITY_LEVEL
        );

        mace.addEnchantment(
                world.getRegistryManager()
                        .getOrThrow(
                                net.minecraft.registry.RegistryKeys.ENCHANTMENT
                        )
                        .getOrThrow(
                                Enchantments.BREACH
                        ),
                BREACH_LEVEL
        );

        double fallDistance =
                Math.max(
                        UPWARD_DASH_HEIGHT,
                        state.maximumFallDistance
                );

        Box area =
                attacker.getBoundingBox()
                        .expand(LANDING_SMASH_RADIUS);

        List<LivingEntity> targets =
                world.getEntitiesByClass(
                        LivingEntity.class,
                        area,
                        target ->
                                target != attacker
                                        && target.isAlive()
                                        && target.squaredDistanceTo(
                                                attacker
                                        )
                                        <= LANDING_SMASH_RADIUS
                                        * LANDING_SMASH_RADIUS
                );

        BlockPos landingPos =
                attacker.getBlockPos();

        world.syncWorldEvent(
                attacker,
                WorldEvents.SMASH_ATTACK,
                landingPos,
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

        for (LivingEntity target : targets) {
            float damagePerBlock =
                    EnchantmentHelper
                            .getSmashDamagePerFallenBlock(
                                    world,
                                    mace,
                                    target,
                                    damageSource,
                                    0.5F
                            );

            float smashDamage =
                    8.0F
                            + (float) fallDistance
                            * damagePerBlock;

            smashDamage =
                    EnchantmentHelper.getDamage(
                            world,
                            mace,
                            target,
                            damageSource,
                            smashDamage
                    );

            target.damage(
                    world,
                    damageSource,
                    smashDamage
            );

            applyMaceKnockback(
                    attacker,
                    target
            );
        }
    }

    private static void applyMaceKnockback(
            Entity attacker,
            LivingEntity target
    ) {
        Vec3d difference =
                target.getEntityPos()
                        .subtract(attacker.getEntityPos());

        double horizontal =
                Math.sqrt(
                        difference.x * difference.x
                                + difference.z * difference.z
                );

        if (horizontal < 0.0001) {
            return;
        }

        double distance =
                Math.min(
                        horizontal,
                        LANDING_SMASH_RADIUS
                );

        double strength =
                1.0
                        - (
                        distance
                                / LANDING_SMASH_RADIUS
                );

        double knockback =
                0.75
                        * Math.max(
                        0.25,
                        strength
                );

        target.addVelocity(
                difference.x
                        / horizontal
                        * knockback,
                0.45,
                difference.z
                        / horizontal
                        * knockback
        );

        target.velocityDirty = true;
    }

    private static void pushNearbyEntities(
            ServerPlayerEntity player,
            double radius
    ) {
        ServerWorld world =
                player.getEntityWorld();

        Box area =
                player.getBoundingBox()
                        .expand(radius);

        List<LivingEntity> entities =
                world.getEntitiesByClass(
                        LivingEntity.class,
                        area,
                        target ->
                                target != player
                                        && target.isAlive()
                                        && target.squaredDistanceTo(
                                                player
                                        )
                                        <= radius * radius
                );

        for (LivingEntity target : entities) {
            Vec3d difference =
                    target.getEntityPos()
                            .subtract(player.getEntityPos());

            double horizontal =
                    Math.sqrt(
                            difference.x * difference.x
                                    + difference.z * difference.z
                    );

            if (horizontal < 0.0001) {
                continue;
            }

            double distance =
                    Math.min(horizontal, radius);

            double strength =
                    1.35
                            * (
                            1.0
                                    - distance / radius
                    );

            target.addVelocity(
                    difference.x
                            / horizontal
                            * strength,
                    0.55,
                    difference.z
                            / horizontal
                            * strength
            );

            target.velocityDirty = true;
        }
    }

    private static void handleSpecialSwordHit(
            LivingEntity entity,
            DamageSource source,
            float damageTaken,
            boolean blocked
    ) {
        if (blocked || damageTaken <= 0.0F) {
            return;
        }

        if (!(source.getAttacker()
                instanceof ServerPlayerEntity attacker)) {
            return;
        }

        ItemStack weapon =
                source.getWeaponStack();

        if (weapon == null || weapon.isEmpty()) {
            return;
        }

        if (!isNetheriteSword(weapon)) {
            return;
        }

        String name =
                getName(weapon);

        if (LIGHTNING_SWORD.equals(name)) {
            if (Math.random() >= LIGHTNING_CHANCE) {
                return;
            }

            triggerLightning(
                    attacker,
                    entity,
                    weapon
            );

            return;
        }

        if (LIFESTEAL_SWORD.equals(name)) {
            if (Math.random() >= LIFESTEAL_CHANCE) {
                return;
            }

            triggerLifesteal(
                    attacker,
                    weapon
            );
        }
    }

    private static void triggerLightning(
            ServerPlayerEntity attacker,
            LivingEntity target,
            ItemStack weapon
    ) {
        lightningTasks.add(
                new LightningTask(
                        target.getUuid(),
                        target.getEntityWorld().getRegistryKey(),
                        attacker.getUuid(),
                        0,
                        0
                )
        );

        damageStack(
                weapon,
                LIGHTNING_DURABILITY,
                attacker
        );

        sendSuccessMessage(
                attacker,
                "specialswords.lightning.success"
        );
    }

    private static void tickLightning(
            MinecraftServer server
    ) {
        Iterator<LightningTask> iterator =
                lightningTasks.iterator();

        while (iterator.hasNext()) {
            LightningTask task =
                    iterator.next();

            if (task.ticksUntilStrike > 0) {
                task.ticksUntilStrike--;
                continue;
            }

            ServerPlayerEntity attacker =
                    server.getPlayerManager()
                            .getPlayer(task.attacker);

            if (attacker == null) {
                iterator.remove();
                continue;
            }

            ServerWorld world =
                    server.getWorld(task.worldKey);

            if (world == null) {
                iterator.remove();
                continue;
            }

            Entity target =
                    world.getEntity(task.target);

            if (target == null
                    || !target.isAlive()) {
                iterator.remove();
                continue;
            }

            LightningEntity lightning =
                    EntityType.LIGHTNING_BOLT.create(
                            world,
                            SpawnReason.TRIGGERED
                    );

            if (lightning != null) {
                lightning.refreshPositionAfterTeleport(
                        target.getX(),
                        target.getY(),
                        target.getZ()
                );

                world.spawnEntity(lightning);
            }

            task.strikes++;

            if (task.strikes >= LIGHTNING_STRIKES) {
                iterator.remove();
                continue;
            }

            task.ticksUntilStrike =
                    LIGHTNING_INTERVAL_TICKS;
        }
    }

    private static void triggerLifesteal(
            ServerPlayerEntity player,
            ItemStack weapon
    ) {
        StatusEffectInstance current =
                player.getStatusEffect(
                        StatusEffects.HEALTH_BOOST
                );

        int amplifier =
                current == null
                        ? 0
                        : current.getAmplifier() + 1;

        int hearts =
                (amplifier + 1) * 2;

        float healAmount =
                hearts * 2.0F;

        player.addStatusEffect(
                new StatusEffectInstance(
                        StatusEffects.HEALTH_BOOST,
                        20 * 60,
                        amplifier,
                        false,
                        false,
                        true
                )
        );

        player.heal(healAmount);

        damageStack(
                weapon,
                LIFESTEAL_DURABILITY,
                player
        );

        sendSuccessMessage(
                player,
                "specialswords.lifesteal.success",
                hearts
        );
    }

    private static boolean allowDamage(
            LivingEntity entity,
            DamageSource source
    ) {
        if (!source.isOf(DamageTypes.FALL)) {
            return true;
        }

        if (!(entity instanceof ServerPlayerEntity player)) {
            return true;
        }

        if (isHoldingDashSword(player)) {
            return false;
        }

        if (upwardDashes.containsKey(player.getUuid())) {
            return false;
        }

        return true;
    }

    private static boolean isHoldingDashSword(
            ServerPlayerEntity player
    ) {
        return isSpecialSword(
                player.getMainHandStack(),
                DASH_SWORD
        ) || isSpecialSword(
                player.getOffHandStack(),
                DASH_SWORD
        );
    }

    private static void sendSuccessMessage(
            ServerPlayerEntity player,
            String key,
            Object... args
    ) {
        player.sendMessage(
                Text.translatable(
                        key,
                        args
                ).formatted(
                        Formatting.GREEN
                ),
                true
        );
    }

    private static void sendCooldownMessage(
            ServerPlayerEntity player,
            String abilityKey,
            long remaining
    ) {
        long seconds =
                (remaining + 999L) / 1000L;

        String key =
                seconds == 1
                        ? abilityKey + ".cooldown.single"
                        : abilityKey + ".cooldown.plural";

        player.sendMessage(
                Text.translatable(
                        key,
                        seconds
                ).formatted(
                        Formatting.RED
                ),
                true
        );
    }

    private static long getRemainingCooldown(
            Map<UUID, Long> cooldowns,
            UUID uuid
    ) {
        Long end =
                cooldowns.get(uuid);

        if (end == null) {
            return 0;
        }

        long remaining =
                end - System.currentTimeMillis();

        if (remaining <= 0) {
            cooldowns.remove(uuid);
            return 0;
        }

        return remaining;
    }

    private static void damageStack(
            ItemStack stack,
            int amount,
            ServerPlayerEntity player
    ) {
        stack.damage(
                amount,
                player,
                EquipmentSlot.MAINHAND
        );
    }

    public static int getSpecialSwordRenameCost(
            String name
    ) {
        return switch (name) {
            case LIGHTNING_SWORD -> 30;
            case LIFESTEAL_SWORD,
                 DASH_SWORD -> 35;
            default -> 0;
        };
    }

    private static boolean isSpecialSword(
            ItemStack stack,
            String name
    ) {
        return isNetheriteSword(stack)
                && name.equals(
                getName(stack)
        );
    }

    private static boolean isNetheriteSword(
            ItemStack stack
    ) {
        return stack.isOf(
                Items.NETHERITE_SWORD
        );
    }

    private static String getName(
            ItemStack stack
    ) {
        if (stack.getCustomName() == null) {
            return "";
        }

        return stack.getCustomName()
                .getString();
    }

    private static class ForwardDashState {
        private final Vec3d direction;
        private int ticks;

        private ForwardDashState(
                Vec3d direction,
                int ticks
        ) {
            this.direction = direction;
            this.ticks = ticks;
        }
    }

    private static class UpwardDashState {
        private int ticks;
        private boolean startedFalling;
        private double maximumFallDistance;

        private UpwardDashState() {
            this.ticks = 0;
            this.startedFalling = false;
            this.maximumFallDistance = 14.0;
        }
    }

    private static class LightningTask {
        private final UUID target;
        private final net.minecraft.registry.RegistryKey<World> worldKey;
        private final UUID attacker;
        private int strikes;
        private int ticksUntilStrike;

        private LightningTask(
                UUID target,
                net.minecraft.registry.RegistryKey<World> worldKey,
                UUID attacker,
                int strikes,
                int ticksUntilStrike
        ) {
            this.target = target;
            this.worldKey = worldKey;
            this.attacker = attacker;
            this.strikes = strikes;
            this.ticksUntilStrike = ticksUntilStrike;
        }
    }
}
