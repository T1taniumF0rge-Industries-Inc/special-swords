package com.titan1um.specialswords.sword;
import com.titan1um.specialswords.SwordUtils;
import net.minecraft.entity.LivingEntity;import net.minecraft.entity.effect.*;import net.minecraft.entity.mob.HostileEntity;import net.minecraft.entity.player.PlayerEntity;import net.minecraft.item.ItemStack;import net.minecraft.server.MinecraftServer;import net.minecraft.server.network.ServerPlayerEntity;import net.minecraft.sound.*;import net.minecraft.util.Formatting;import java.util.*;import java.util.concurrent.ThreadLocalRandom;
public final class LifestealSword{
 private static final Map<UUID,StatusEffectInstance>SAVED=new HashMap<>();private LifestealSword(){}
 public static boolean matches(ItemStack s){return SwordUtils.isSpecialSword(s,SwordUtils.LIFESTEAL_SWORD);}
 public static void handleHit(ServerPlayerEntity p,LivingEntity t,ItemStack w){
  if(!(t instanceof PlayerEntity)&&!(t instanceof HostileEntity))return;if(ThreadLocalRandom.current().nextDouble()>=.25)return;
  StatusEffectInstance cur=p.getStatusEffect(StatusEffects.HEALTH_BOOST);int amp=cur==null?0:cur.getAmplifier()+1;int hearts=(amp+1)*2;
  p.addStatusEffect(new StatusEffectInstance(StatusEffects.HEALTH_BOOST,1200,amp,false,false,true));p.heal(hearts*2f);
  p.getEntityWorld().playSound(null,p.getX(),p.getY(),p.getZ(),SoundEvents.ENTITY_ENDER_DRAGON_GROWL,SoundCategory.PLAYERS,1f,1f);
  w.damage(10,p,net.minecraft.entity.EquipmentSlot.MAINHAND);SwordUtils.bar(p,"Lifesteal! +"+hearts+" hearts.",Formatting.GREEN);
 }
 public static void tick(MinecraftServer s){for(ServerPlayerEntity p:s.getPlayerManager().getPlayerList()){boolean h=matches(p.getMainHandStack())||matches(p.getOffHandStack());if(h){if(!SAVED.containsKey(p.getUuid()))SAVED.put(p.getUuid(),p.getStatusEffect(StatusEffects.REGENERATION));p.addStatusEffect(new StatusEffectInstance(StatusEffects.REGENERATION,40,1,false,false,false));}else if(SAVED.containsKey(p.getUuid())){StatusEffectInstance cur=p.getStatusEffect(StatusEffects.REGENERATION);if(cur!=null&&cur.getAmplifier()==1&&cur.getDuration()<=40)p.removeStatusEffect(StatusEffects.REGENERATION);StatusEffectInstance old=SAVED.remove(p.getUuid());if(old!=null)p.addStatusEffect(old);}}}
}