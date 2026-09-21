package com.titan1um.specialswords.mixin;

import com.titan1um.specialswords.SpecialSwordsMod;

import net.minecraft.component.DataComponentTypes;
import net.minecraft.item.ItemStack;
import net.minecraft.screen.AnvilScreenHandler;
import net.minecraft.screen.Property;
import net.minecraft.screen.slot.Slot;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

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

    @Inject(
            method = "updateResult",
            at = @At("TAIL")
    )
    private void specialSwords$updateResult(
            CallbackInfo ci
    ) {
        AnvilScreenHandler handler =
                (AnvilScreenHandler) (Object) this;

        Slot outputSlot =
                handler.getSlot(2);

        ItemStack output =
                outputSlot.getStack();

        if (output.isEmpty()) {
            return;
        }

        String name =
                output.getCustomName() == null
                        ? ""
                        : output.getCustomName()
                                .getString();

        int specialCost =
                SpecialSwordsMod
                        .getSpecialSwordRenameCost(
                                name
                        );

        if (specialCost <= 0) {
            return;
        }

        output.set(
                DataComponentTypes.CUSTOM_NAME,
                Text.literal(name)
                        .formatted(
                                Formatting.RED,
                                Formatting.BOLD
                        )
        );

        levelCost.set(specialCost);
    }
}
