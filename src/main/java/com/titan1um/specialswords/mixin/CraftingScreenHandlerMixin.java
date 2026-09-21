package com.titan1um.specialswords.mixin;

import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.inventory.CraftingResultInventory;
import net.minecraft.inventory.RecipeInputInventory;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.recipe.CraftingRecipe;
import net.minecraft.recipe.RecipeEntry;
import net.minecraft.screen.CraftingScreenHandler;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.server.world.ServerWorld;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(CraftingScreenHandler.class)
public abstract class CraftingScreenHandlerMixin {

    @Inject(
            method = "updateResult",
            at = @At("TAIL")
    )
    private static void specialSwords$filterRecipe(
            ScreenHandler handler,
            ServerWorld world,
            PlayerEntity player,
            RecipeInputInventory inventory,
            CraftingResultInventory result,
            RecipeEntry<CraftingRecipe> recipe,
            CallbackInfo ci
    ) {
        ItemStack output = result.getStack(0);

        if (output.isEmpty()) {
            return;
        }

        if (output.isOf(Items.MACE) && !matchesMaceRecipe(inventory)) {
            result.setStack(0, ItemStack.EMPTY);
            return;
        }

        if (isSpear(output) && !matchesDiamondSpearRecipe(inventory)) {
            result.setStack(0, ItemStack.EMPTY);
        }
    }

    private static boolean isSpear(ItemStack stack) {
        return stack.isOf(Items.WOODEN_SPEAR)
                || stack.isOf(Items.STONE_SPEAR)
                || stack.isOf(Items.COPPER_SPEAR)
                || stack.isOf(Items.IRON_SPEAR)
                || stack.isOf(Items.GOLDEN_SPEAR)
                || stack.isOf(Items.DIAMOND_SPEAR)
                || stack.isOf(Items.NETHERITE_SPEAR);
    }

    private static boolean matchesMaceRecipe(RecipeInputInventory inventory) {
        return item(inventory, 0, Items.WIND_CHARGE)
                && item(inventory, 1, Items.NETHERITE_INGOT)
                && item(inventory, 2, Items.WIND_CHARGE)
                && item(inventory, 3, Items.DIAMOND_BLOCK)
                && item(inventory, 4, Items.HEAVY_CORE)
                && item(inventory, 5, Items.DIAMOND_BLOCK)
                && item(inventory, 6, Items.ENCHANTED_GOLDEN_APPLE)
                && item(inventory, 7, Items.BREEZE_ROD)
                && item(inventory, 8, Items.ENCHANTED_GOLDEN_APPLE);
    }

    private static boolean matchesDiamondSpearRecipe(RecipeInputInventory inventory) {
        return item(inventory, 0, Items.NETHERITE_INGOT)
                && item(inventory, 1, Items.DIAMOND_BLOCK)
                && item(inventory, 2, Items.ENCHANTED_GOLDEN_APPLE)
                && item(inventory, 3, Items.DIAMOND_BLOCK)
                && item(inventory, 4, Items.STICK)
                && inventory.getStack(5).isEmpty()
                && item(inventory, 6, Items.ENCHANTED_GOLDEN_APPLE)
                && inventory.getStack(7).isEmpty()
                && item(inventory, 8, Items.STICK);
    }

    private static boolean item(
            RecipeInputInventory inventory,
            int slot,
            Item item
    ) {
        ItemStack stack = inventory.getStack(slot);
        return stack.isOf(item) && stack.getCount() == 1;
    }
}
