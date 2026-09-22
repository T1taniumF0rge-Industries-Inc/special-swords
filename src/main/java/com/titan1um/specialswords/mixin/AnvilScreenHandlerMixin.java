package com.titan1um.specialswords.mixin;

import com.titan1um.specialswords.SpecialSwordsMod;
import com.titan1um.specialswords.SpecialSwordManager;
import com.titan1um.specialswords.SwordUtils;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.item.ItemStack;
import net.minecraft.screen.AnvilScreenHandler;
import net.minecraft.screen.Property;
import net.minecraft.text.Text;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(AnvilScreenHandler.class)
public abstract class AnvilScreenHandlerMixin {

    @Shadow
    @Final
    private Property levelCost;

    @Inject(method = "updateResult", at = @At("TAIL"))
    private void specialSwords$updateResult(CallbackInfo ci) {
        AnvilScreenHandler handler = (AnvilScreenHandler) (Object) this;
        ItemStack input = handler.getSlot(0).getStack();
        ItemStack output = handler.getSlot(2).getStack();

        if (output.isEmpty() || !SwordUtils.isNetheriteSword(output)) {
            return;
        }

        if (!SpecialSwordManager.isRenamingEnabled()
                && isActualRename(input, output)) {
            handler.getSlot(2).setStack(ItemStack.EMPTY);
            levelCost.set(0);
            return;
        }

        Text customName = output.get(DataComponentTypes.CUSTOM_NAME);

        if (customName == null) {
            return;
        }

        String normalizedName =
                SwordUtils.normalizeSpecialSwordName(customName.getString());

        if (normalizedName.isEmpty()) {
            return;
        }

        int cost = SpecialSwordsMod.getSpecialSwordRenameCost(normalizedName);

        output.set(
                DataComponentTypes.CUSTOM_NAME,
                SwordUtils.decoratedName(normalizedName)
        );

        levelCost.set(cost);
    }

    private static boolean isActualRename(ItemStack input, ItemStack output) {
        Text inputName = input.get(DataComponentTypes.CUSTOM_NAME);
        Text outputName = output.get(DataComponentTypes.CUSTOM_NAME);

        if (outputName == null) {
            return false;
        }

        if (inputName == null) {
            return true;
        }

        return !inputName.getString().equals(outputName.getString());
    }
}
