package com.titan1um.specialswords;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.text.MutableText;
import net.minecraft.text.Style;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.server.network.ServerPlayerEntity;
public final class SwordUtils{
 public static final String LIGHTNING_SWORD="Lightning Sword",LIFESTEAL_SWORD="Lifesteal Sword",DASH_SWORD="Dash Sword";
 private SwordUtils(){}
 public static boolean isNetheriteSword(ItemStack s){return s.isOf(Items.NETHERITE_SWORD);}
 public static String normalizeSpecialSwordName(String n){
  if(n==null)return ""; String v=n.trim();
  if(v.startsWith("X ")&&v.endsWith(" X")&&v.length()>3)v=v.substring(2,v.length()-2).trim();
  return switch(v){case LIGHTNING_SWORD,LIFESTEAL_SWORD,DASH_SWORD->v;default->"";};
 }
 public static String getSpecialSwordName(ItemStack s){Text n=s.get(DataComponentTypes.CUSTOM_NAME);return n==null?"":normalizeSpecialSwordName(n.getString());}
 public static boolean isSpecialSword(ItemStack s,String n){return isNetheriteSword(s)&&n.equals(getSpecialSwordName(s));}
 public static Text decoratedName(String n){
  Formatting c=switch(n){case LIGHTNING_SWORD->Formatting.YELLOW;case LIFESTEAL_SWORD->Formatting.DARK_RED;default->Formatting.WHITE;};
  Style plain=Style.EMPTY.withColor(c).withItalic(false),ob=plain.withObfuscated(true);
  MutableText t=Text.literal("X").setStyle(ob);t.append(Text.literal(" ").setStyle(plain));t.append(Text.literal(n).setStyle(plain));t.append(Text.literal(" ").setStyle(plain));t.append(Text.literal("X").setStyle(ob));return t;
 }
 public static void bar(ServerPlayerEntity p,String s,Formatting f){p.sendMessage(Text.literal(s).formatted(f),true);}
 public static void cooldown(ServerPlayerEntity p,String a,long ms){bar(p,a+" is on cooldown! "+Math.max(1,(ms+999)/1000)+"s remaining.",Formatting.RED);}
}