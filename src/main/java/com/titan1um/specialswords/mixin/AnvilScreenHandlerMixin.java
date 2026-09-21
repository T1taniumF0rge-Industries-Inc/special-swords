package com.titan1um.specialswords.mixin;
import com.titan1um.specialswords.SpecialSwordsMod;import com.titan1um.specialswords.SwordUtils;import net.minecraft.component.DataComponentTypes;import net.minecraft.item.ItemStack;import net.minecraft.screen.AnvilScreenHandler;import net.minecraft.screen.Property;import net.minecraft.text.Text;import org.spongepowered.asm.mixin.*;import org.spongepowered.asm.mixin.injection.*;
@Mixin(AnvilScreenHandler.class)public abstract class AnvilScreenHandlerMixin{
 @Shadow @Final private Property levelCost;
 @Inject(method="updateResult",at=@At("TAIL"))private void update(org.spongepowered.asm.mixin.injection.callback.CallbackInfo ci){
  ItemStack o=((AnvilScreenHandler)(Object)this).getSlot(2).getStack();if(o.isEmpty()||!SwordUtils.isNetheriteSword(o))return;Text n=o.get(DataComponentTypes.CUSTOM_NAME);if(n==null)return;String s=SwordUtils.normalizeSpecialSwordName(n.getString());int c=SpecialSwordsMod.getSpecialSwordRenameCost(s);if(c<=0)return;o.set(DataComponentTypes.CUSTOM_NAME,SwordUtils.decoratedName(s));levelCost.set(c);
 }
}