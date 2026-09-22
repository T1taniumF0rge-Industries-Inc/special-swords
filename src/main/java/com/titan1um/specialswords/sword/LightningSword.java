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
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.world.World;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.UUID;

public final class LightningSword {

    private static final int PROC_PERCENT = 10;
    private static final int STRIKES = 10;
    private static final int STRIKE_INTERVAL_TICKS = 4;
    private static final int DURABILITY_COST = 10;

    private static final List<LightningTask> TASKS = new ArrayList<>();

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
        if (!SwordUtils.rollPercent(attacker.getEntityWorld(), PROC_PERCENT)) {
            return;
        }

        TASKS.add(new LightningTask(
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
                Text.translatable("specialswords.lightning.success"),
                Formatting.GREEN
        );
    }

    public static void tick(MinecraftServer server) {
        Iterator<LightningTask> iterator = TASKS.iterator();

        while (iterator.hasNext()) {
            LightningTask task = iterator.next();

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

            task.strikes++;

            if (task.strikes >= STRIKES) {
                iterator.remove();
            } else {
                task.delayTicks = STRIKE_INTERVAL_TICKS - 1;
            }
        }
    }

    private static final class LightningTask {

        private final UUID target;
        private final RegistryKey<World> worldKey;
        private int strikes;
        private int delayTicks;

        private LightningTask(UUID target, RegistryKey<World> worldKey) {
            this.target = target;
            this.worldKey = worldKey;
        }
    }
}
