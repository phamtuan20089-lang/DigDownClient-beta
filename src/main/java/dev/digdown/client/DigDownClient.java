package dev.digdown.client;

import net.fabricmc.api.ClientModInitializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class DigDownClient implements ClientModInitializer {

    public static final String MOD_ID = "digdown";
    public static final String MOD_NAME = "DigDown Client";
    public static final Logger LOGGER =
            LoggerFactory.getLogger(MOD_NAME);

    @Override
    public void onInitializeClient() {
        DigDownCore.init();
        LOGGER.info("DigDown Client initialized.");
        LOGGER.info("Default profile: PC");
    }
}
