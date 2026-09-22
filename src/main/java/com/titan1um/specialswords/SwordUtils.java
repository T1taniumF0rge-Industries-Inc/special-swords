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
                .withItalic(false)
                .withObfuscated(false);

        Style obfuscated = plain.withObfuscated(true);

        MutableText text = Text.literal("X").setStyle(obfuscated);
        text.append(Text.literal(" ").setStyle(plain));
        text.append(Text.literal(name).setStyle(plain));
        text.append(Text.literal(" ").setStyle(plain));
        text.append(Text.literal("X").setStyle(obfuscated));

        return text;
    }

    public static boolean rollPercent(net.minecraft.server.world.ServerWorld world, int percent) {
        if (percent <= 0) {
            return false;
        }

        if (percent >= 100) {
            return true;
        }

        return world.getRandom().nextInt(100) < percent;
    }

    public static void actionBar(ServerPlayerEntity player, String message, Formatting color) {
        player.sendMessage(Text.literal(message).formatted(color), true);
    }

    public static void actionBar(ServerPlayerEntity player, Text message, Formatting color) {
        player.sendMessage(message.copy().formatted(color), true);
    }

    public static Text localizedMessage(
            ServerPlayerEntity player,
            String key,
            Object... args
    ) {
        boolean french = player.getClientOptions().language().toLowerCase().startsWith("fr");

        String template = switch (key) {
            case "specialswords.lightning.success" -> french
                    ? "Vous avez invoqué la foudre qui a frappé vos adversaires !"
                    : "You have summoned lightning that struck your opponents!";
            case "specialswords.lightning.max" -> french
                    ? "Vous venez de lancer votre 10e éclair. La foudre est maintenant en recharge."
                    : "You just struck your 10th lightning strike. Now on cooldown.";
            case "specialswords.lightning.cooldown" -> french
                    ? "La foudre est en recharge ! Plus que %s secondes…"
                    : "Lightning is on cooldown! %s seconds remaining…";
            case "specialswords.lifesteal.success" -> french
                    ? "Vol de vie utilisé et %s cœurs supplémentaires accordés !"
                    : "Lifesteal has been used and granted you %s extra hearts!";
            case "specialswords.lifesteal.max" -> french
                    ? "Le vol de vie a atteint sa capacité maximale de 10 cœurs supplémentaires ! Il sera en recharge lorsque vos cœurs supplémentaires auront disparu !"
                    : "Lifesteal reached its max capacity of 10 extra hearts! It will be on cooldown once your extra hearts vanish!";
            case "specialswords.lifesteal.cooldown" -> french
                    ? "Le vol de vie est en recharge ! Plus que %s secondes…"
                    : "Lifesteal is on cooldown! %s seconds remaining…";
            case "specialswords.forward_dash.success" -> french
                    ? "Dash en avant utilisé ! En recharge."
                    : "Forward Dash has been used! On cooldown.";
            case "specialswords.upward_dash.success" -> french
                    ? "Dash vers le haut utilisé ! En recharge."
                    : "Upward Dash has been used! On cooldown.";
            case "specialswords.forward_dash.cooldown.single" -> french
                    ? "Le Dash en avant est en recharge ! Plus que %s seconde…"
                    : "Forward Dash is on cooldown! %s second remaining…";
            case "specialswords.forward_dash.cooldown.plural" -> french
                    ? "Le Dash en avant est en recharge ! Plus que %s secondes…"
                    : "Forward Dash is on cooldown! %s seconds remaining…";
            case "specialswords.upward_dash.cooldown.single" -> french
                    ? "Le Dash vers le haut est en recharge ! Plus que %s seconde…"
                    : "Upward Dash is on cooldown! %s second remaining…";
            case "specialswords.upward_dash.cooldown.plural" -> french
                    ? "Le Dash vers le haut est en recharge ! Plus que %s secondes…"
                    : "Upward Dash is on cooldown! %s seconds remaining…";
            default -> key;
        };

        return Text.literal(args.length == 0 ? template : template.formatted(args));
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
