package com.titan1um.specialswords.sword;
import com.titan1um.specialswords.SwordUtils;
import net.minecraft.entity.*;import net.minecraft.entity.mob.HostileEntity;import net.minecraft.entity.player.PlayerEntity;import net.minecraft.item.ItemStack;import net.minecraft.server.*;import net.minecraft.server.network.ServerPlayerEntity;import net.minecraft.server.world.ServerWorld;import net.minecraft.registry.RegistryKey;import net.minecraft.util.Formatting;import net.minecraft.world.World;import java.util.*;import java.util.concurrent.ThreadLocalRandom;
public final class LightningSword{
 private static final double CHANCE=.10;private static final int STRIKES=10,INTERVAL=4;private static final List<Task>TASKS=new ArrayList<>();private LightningSword(){}
 public static boolean matches(ItemStack s){return SwordUtils.isSpecialSword(s,SwordUtils.LIGHTNING_SWORD);}
 public static void handleHit(ServerPlayerEntity p,LivingEntity t,ItemStack w){
  if(!(t instanceof PlayerEntity)&&!(t instanceof HostileEntity))return;if(ThreadLocalRandom.current().nextDouble()>=CHANCE)return;
  TASKS.add(new Task(t.getUuid(),t.getEntityWorld().getRegistryKey()));w.damage(10,p,net.minecraft.entity.EquipmentSlot.MAINHAND);SwordUtils.bar(p,"Lightning Sword activated!",Formatting.GREEN);
 }
 public static void tick(MinecraftServer s){Iterator<Task>it=TASKS.iterator();while(it.hasNext()){Task q=it.next();if(q.delay-->0)continue;ServerWorld w=s.getWorld(q.world);if(w==null){it.remove();continue;}Entity e=w.getEntity(q.target);if(!(e instanceof LivingEntity t)||!t.isAlive()||(!(t instanceof PlayerEntity)&&!(t instanceof HostileEntity))){it.remove();continue;}LightningEntity b=EntityType.LIGHTNING_BOLT.create(w,SpawnReason.TRIGGERED);if(b!=null){b.refreshPositionAfterTeleport(t.getX(),t.getY(),t.getZ());w.spawnEntity(b);}if(++q.strikes>=STRIKES)it.remove();else q.delay=INTERVAL;}}
 private static final class Task{final UUID target;final RegistryKey<World>world;int strikes,delay;Task(UUID t,RegistryKey<World>w){target=t;world=w;delay=0;}}
}