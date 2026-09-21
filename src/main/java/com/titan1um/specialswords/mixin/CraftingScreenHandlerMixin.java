package com.titan1um.specialswords.mixin;
import net.minecraft.entity.player.PlayerEntity;import net.minecraft.inventory.*;import net.minecraft.item.*;import net.minecraft.recipe.*;import net.minecraft.screen.*;import net.minecraft.server.world.ServerWorld;import org.spongepowered.asm.mixin.*;import org.spongepowered.asm.mixin.injection.*;
@Mixin(CraftingScreenHandler.class)public abstract class CraftingScreenHandlerMixin{
 @Inject(method="updateResult",at=@At("TAIL"))private static void filter(ScreenHandler h,ServerWorld w,PlayerEntity p,RecipeInputInventory i,CraftingResultInventory r,RecipeEntry<CraftingRecipe> q,org.spongepowered.asm.mixin.injection.callback.CallbackInfo ci){
  ItemStack o=r.getStack(0);if(o.isEmpty())return;if(o.isOf(Items.MACE)&&!mace(i)){r.setStack(0,ItemStack.EMPTY);return;}if(spear(o)&&!diamond(i))r.setStack(0,ItemStack.EMPTY);
 }
 static boolean spear(ItemStack s){return s.isOf(Items.WOODEN_SPEAR)||s.isOf(Items.STONE_SPEAR)||s.isOf(Items.COPPER_SPEAR)||s.isOf(Items.IRON_SPEAR)||s.isOf(Items.GOLDEN_SPEAR)||s.isOf(Items.DIAMOND_SPEAR)||s.isOf(Items.NETHERITE_SPEAR);}
 static boolean mace(RecipeInputInventory i){return a(i,0,Items.WIND_CHARGE)&&a(i,1,Items.NETHERITE_INGOT)&&a(i,2,Items.WIND_CHARGE)&&a(i,3,Items.DIAMOND_BLOCK)&&a(i,4,Items.HEAVY_CORE)&&a(i,5,Items.DIAMOND_BLOCK)&&a(i,6,Items.ENCHANTED_GOLDEN_APPLE)&&a(i,7,Items.BREEZE_ROD)&&a(i,8,Items.ENCHANTED_GOLDEN_APPLE);}
 static boolean diamond(RecipeInputInventory i){return a(i,0,Items.NETHERITE_INGOT)&&a(i,1,Items.DIAMOND_BLOCK)&&a(i,2,Items.ENCHANTED_GOLDEN_APPLE)&&a(i,3,Items.DIAMOND_BLOCK)&&a(i,4,Items.STICK)&&i.getStack(5).isEmpty()&&a(i,6,Items.ENCHANTED_GOLDEN_APPLE)&&i.getStack(7).isEmpty()&&a(i,8,Items.STICK);}
 static boolean a(RecipeInputInventory i,int n,Item x){return i.getStack(n).isOf(x)&&i.getStack(n).getCount()==1;}
}