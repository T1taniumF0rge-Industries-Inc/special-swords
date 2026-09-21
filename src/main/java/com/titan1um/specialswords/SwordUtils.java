package com.titan1um.specialswords;

import net.minecraft.component.DataComponentTypes;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.MutableText;
import net.minecraft.text.Style;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

public final class SwordUtils {

    public static final String LIGHTNING_SWORD = "Lightning Sword";
    public static final String LIFESTEAL_SWORD = "Lifesteal Sword";
    public static final String DASH_SWORD = "Dash Sword";

    private SwordUtils() {
    }

    public static boolean isNetheriteSword(ItemStack stack) {
        return stack.isOf(Items.NETHERITE_SWORD);
    }

    public static String normalizeSpecialSwordName(String name) {
        if (name == null) {
            return "";
        }

        String value = name.trim();

        if (value.startsWith("X ") && value.endsWith(" X") && value.length() > 3) {
            value = value.substring(2, value.length() - 2).trim();
        }

        return switch (value) {
            case LIGHTNING_SWORD, LIFESTEAL_SWORD, DASH_SWORD -> value;
            default -> "";
        };
    }

    public static String getSpecialSwordName(ItemStack stack) {
        Text customName = stack.get(DataComponentTypes.CUSTOM_NAME);

        if (customName == null) {
            return "";
        }

        return normalizeSpecialSwordName(customName.getString());
    }

    public static boolean isSpecialSword(ItemStack stack, String name) {
        return isNetheriteSword(stack) && name.equals(getSpecialSwordName(stack));
    }

    public static Text decoratedName(String name) {
        Formatting color = switch (name) {
            case LIGHTNING_SWORD -> Formatting.YELLOW;
            case LIFESTEAL_SWORD -> Formatting.DARK_RED;
            case DASH_SWORD -> Formatting.WHITE;
            default -> Formatting.WHITE;
        };

        Style plain = Style.EMPTY
                .withColor(color)
                .withItalic(false);

        Style obfuscated = plain.withObfuscated(true);

        MutableText text = Text.literal("X").setStyle(obfuscated);
        text.append(Text.literal(" ").setStyle(plain));
        text.append(Text.literal(name).setStyle(plain));
        text.append(Text.literal(" ").setStyle(plain));
        text.append(Text.literal("X").setStyle(obfuscated));

        return text;
    }

    public static void actionBar(ServerPlayerEntity player, String message, Formatting color) {
        player.sendMessage(Text.literal(message).formatted(color), true);
    }

    public static void cooldown(ServerPlayerEntity player, String ability, long remainingMillis) {
        long seconds = Math.max(1L, (remainingMillis + 999L) / 1000L);
        actionBar(
                player,
                ability + " is on cooldown! " + seconds + "s remaining.",
                Formatting.RED
        );
    }
}
