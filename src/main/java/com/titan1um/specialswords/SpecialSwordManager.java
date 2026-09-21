package com.titan1um.specialswords;
import com.titan1um.specialswords.sword.*;
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
 private SpecialSwordManager(){}
 public static void register(){
  UseItemCallback.EVENT.register(SpecialSwordManager::use);
  ServerLivingEntityEvents.AFTER_DAMAGE.register(SpecialSwordManager::damage);
  ServerLivingEntityEvents.ALLOW_DAMAGE.register(SpecialSwordManager::allowDamage);
  ServerTickEvents.END_SERVER_TICK.register(SpecialSwordManager::tick);
 }
 private static ActionResult use(PlayerEntity p,World w,Hand h){
  if(w.isClient()||!(p instanceof ServerPlayerEntity sp)||!sp.isSneaking())return ActionResult.PASS;
  return DashSword.tryActivate(sp,sp.getStackInHand(h),h);
 }
 private static void damage(LivingEntity e,DamageSource s,float base,float taken,boolean blocked){
  if(blocked||taken<=0||!(s.getAttacker() instanceof ServerPlayerEntity p))return;
  ItemStack weapon=s.getWeaponStack();
  if(weapon==null||weapon.isEmpty())return;
  if(LightningSword.matches(weapon))LightningSword.handleHit(p,e,weapon);
  else if(LifestealSword.matches(weapon))LifestealSword.handleHit(p,e,weapon);
 }
 private static boolean allowDamage(LivingEntity e,DamageSource s){
  return !s.isOf(net.minecraft.entity.damage.DamageTypes.FALL)
   ||!(e instanceof ServerPlayerEntity p)
   ||!DashSword.shouldCancelFallDamage(p);
 }
 private static void tick(MinecraftServer s){DashSword.tick(s);LightningSword.tick(s);LifestealSword.tick(s);}
}