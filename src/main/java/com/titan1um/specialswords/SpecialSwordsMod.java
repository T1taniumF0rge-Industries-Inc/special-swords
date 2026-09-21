package com.titan1um.specialswords;
import net.fabricmc.api.ModInitializer;
public class SpecialSwordsMod implements ModInitializer{
 @Override public void onInitialize(){SpecialSwordManager.register();}
 public static int getSpecialSwordRenameCost(String name){return switch(SwordUtils.normalizeSpecialSwordName(name)){case SwordUtils.LIGHTNING_SWORD->30;case SwordUtils.LIFESTEAL_SWORD,SwordUtils.DASH_SWORD->35;default->0;};}
 public static String normalizeSpecialSwordName(String name){return SwordUtils.normalizeSpecialSwordName(name);}
}