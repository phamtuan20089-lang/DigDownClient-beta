package dev.digdown.client;

import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.FontDescription;
import net.minecraft.network.chat.Style;
import net.minecraft.resources.Identifier;

/**
 * Font riêng của DigDown (Baloo + Noto, định nghĩa trong assets/digdown/font/ui.json).
 * Mọi chữ của Click GUI và HUD đều đi qua UiFont.text().
 * Có thể tắt trong Click GUI: tab SETTINGS, mục "Custom font".
 */
public final class UiFont {
    private static final Style STYLE = Style.EMPTY.withFont(
            new FontDescription.Resource(Identifier.fromNamespaceAndPath("digdown", "ui")));

    private UiFont() {
    }

    public static Component text(String s) {
        return ClientSettings.CUSTOM_FONT.get() ? Component.literal(s).withStyle(STYLE) : Component.literal(s);
    }
}
