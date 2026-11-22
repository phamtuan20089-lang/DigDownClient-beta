package dev.digdown.client;

import java.util.List;

/** Cài đặt chung của client, hiện ở tab SETTINGS trong Click GUI. */
public final class ClientSettings {
    public static final Module.BoolSetting GUI_SOUND =
            new Module.BoolSetting("GUI sounds", "Play a tick when toggling", true);
    public static final Module.BoolSetting GUI_ANIMATION =
            new Module.BoolSetting("GUI animation", "Animate the menu when it opens", true);
    public static final Module.BoolSetting CUSTOM_FONT =
            new Module.BoolSetting("Custom font", "Use the DigDown font", true);

    private ClientSettings() {
    }

    public static List<Module.BoolSetting> all() {
        return List.of(GUI_SOUND, GUI_ANIMATION, CUSTOM_FONT);
    }
}
