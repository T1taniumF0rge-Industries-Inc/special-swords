package com.titan1um.specialswords;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.fabricmc.api.ModInitializer;

public class SpecialSwordsMod implements ModInitializer {

    public static final Logger LOGGER = LoggerFactory.getLogger("Special Swords");

    @Override
    public void onInitialize() {
        try {
            SpecialSwordManager.register();
            LOGGER.info("[Special Swords] Mod loaded successfully.");
        } catch (Throwable throwable) {
            LOGGER.error("[Special Swords] Failed to load the mod.", throwable);
            throw throwable;
        }
    }

    public static int getSpecialSwordRenameCost(String name) {
        return switch (SwordUtils.normalizeSpecialSwordName(name)) {
            case SwordUtils.LIGHTNING_SWORD -> 30;
            case SwordUtils.LIFESTEAL_SWORD, SwordUtils.DASH_SWORD -> 35;
            default -> 30;
        };
    }

    public static String normalizeSpecialSwordName(String name) {
        return SwordUtils.normalizeSpecialSwordName(name);
    }
}
