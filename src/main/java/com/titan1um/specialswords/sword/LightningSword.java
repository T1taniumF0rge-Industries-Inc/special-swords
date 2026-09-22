package com.titan1um.specialswords.sword;

import com.titan1um.specialswords.SwordUtils;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LightningEntity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.SpawnReason;
import net.minecraft.item.ItemStack;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.registry.RegistryKey;
import net.minecraft.util.Formatting;
import net.minecraft.world.World;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public final class LightningSword {

    private static final int PROC_PERCENT = 25;
    private static final int STRIKES = 10;
    private static final int STRIKE_INTERVAL_TICKS = 4;
    private static final long COOLDOWN_MS = 30_000L;
    private static final int DURABILITY_COST = 10;

    private static final List<LightningTask> TASKS = new ArrayList<>();
    private static final Map<UUID, Integer> STRIKE_COUNTS = new HashMap<>();
    private static final Map<UUID, Long> COOLDOWNS = new HashMap<>();

    private LightningSword() {
    }

    public static boolean matches(ItemStack stack) {
        return SwordUtils.isSpecialSword(stack, SwordUtils.LIGHTNING_SWORD);
    }

    public static void handleHit(
            ServerPlayerEntity attacker,
            LivingEntity target,
            ItemStack weapon
    ) {
        UUID uuid = attacker.getUuid();

        if (TASKS.stream().anyMatch(task -> task.attacker.equals(uuid))) {
            return;
        }

        if (!SwordUtils.rollPercent(attacker.getEntityWorld(), PROC_PERCENT)) {
            return;
        }

        long cooldown = remaining(COOLDOWNS, uuid);

        if (cooldown > 0L) {
            SwordUtils.actionBar(
                    attacker,
                    SwordUtils.localizedMessage(
                            attacker,
                            "specialswords.lightning.cooldown",
                            cooldownSeconds(cooldown)
                    ),
                    Formatting.RED
            );
            return;
        }

        TASKS.add(new LightningTask(
                uuid,
                target.getUuid(),
                target.getEntityWorld().getRegistryKey()
        ));

        weapon.damage(
                DURABILITY_COST,
                attacker,
                net.minecraft.entity.EquipmentSlot.MAINHAND
        );

        SwordUtils.actionBar(
                attacker,
                SwordUtils.localizedMessage(
                        attacker,
                        "specialswords.lightning.success"
                ),
                Formatting.GREEN
        );
    }

    public static void tick(MinecraftServer server) {
        Iterator<LightningTask> iterator = TASKS.iterator();

        while (iterator.hasNext()) {
            LightningTask task = iterator.next();

            if (remaining(COOLDOWNS, task.attacker) > 0L) {
                iterator.remove();
                continue;
            }

            if (task.delayTicks > 0) {
                task.delayTicks--;
                continue;
            }

            ServerWorld world = server.getWorld(task.worldKey);

            if (world == null) {
                iterator.remove();
                continue;
            }

            Entity entity = world.getEntity(task.target);

            if (!(entity instanceof LivingEntity target) || !target.isAlive()) {
                iterator.remove();
                continue;
            }

            LightningEntity lightning = EntityType.LIGHTNING_BOLT.create(
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

            int strikes = STRIKE_COUNTS.merge(task.attacker, 1, Integer::sum);
            task.strikes++;

            if (strikes >= STRIKES) {
                COOLDOWNS.put(
                        task.attacker,
                        System.currentTimeMillis() + COOLDOWN_MS
                );

                ServerPlayerEntity attacker =
                        server.getPlayerManager().getPlayer(task.attacker);

                if (attacker != null) {
                    SwordUtils.actionBar(
                            attacker,
                            SwordUtils.localizedMessage(
                                    attacker,
                                    "specialswords.lightning.max"
                            ),
                            Formatting.GREEN
                    );
                }

                STRIKE_COUNTS.remove(task.attacker);
            }

            if (task.strikes >= STRIKES) {
                iterator.remove();
            } else {
                task.delayTicks = STRIKE_INTERVAL_TICKS - 1;
            }
        }
    }

    public static void clearForPlayer(ServerPlayerEntity player) {
        UUID uuid = player.getUuid();

        TASKS.removeIf(task -> task.attacker.equals(uuid));
        STRIKE_COUNTS.remove(uuid);
        COOLDOWNS.remove(uuid);
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

    private static final class LightningTask {

        private final UUID attacker;
        private final UUID target;
        private final RegistryKey<World> worldKey;
        private int strikes;
        private int delayTicks;

        private LightningTask(
                UUID attacker,
                UUID target,
                RegistryKey<World> worldKey
        ) {
            this.attacker = attacker;
            this.target = target;
            this.worldKey = worldKey;
        }
    }
}
