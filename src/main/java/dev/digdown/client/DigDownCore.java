package dev.digdown.client;

import org.lwjgl.glfw.GLFW;

import com.mojang.blaze3d.platform.InputConstants;

import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientLifecycleEvents;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.Identifier;

/** Điểm khởi tạo chung. Chỉ cần gọi DigDownCore.init() một lần trong onInitializeClient(). */
public final class DigDownCore {
    private static KeyMapping openGuiKey;

    private DigDownCore() {
    }

    public static void init() {
        BuiltinModules.registerAll();
        ConfigManager.load(); // khôi phục chức năng đang bật và các tùy chọn từ lần chơi trước
        DigDownHud.register();

        KeyMapping.Category category = KeyMapping.Category.register(
                Identifier.fromNamespaceAndPath("digdown", "main"));

        // Mặc định Right Shift. Trên điện thoại: gán nút điều khiển của Zalith vào phím này,
        // hoặc đổi phím trong Options > Controls > Key Binds > DigDown Client.
        openGuiKey = KeyBindingHelper.registerKeyBinding(new KeyMapping(
                "key.digdown.open_gui",
                InputConstants.Type.KEYSYM,
                GLFW.GLFW_KEY_RIGHT_SHIFT,
                category));

        ClientLifecycleEvents.CLIENT_STOPPING.register(client -> ConfigManager.save());
        ClientTickEvents.END_CLIENT_TICK.register(DigDownCore::onTick);
    }

    private static void onTick(Minecraft mc) {
        while (openGuiKey.consumeClick()) {
            if (mc.screen == null) {
                mc.setScreen(new ClickGuiScreen());
            }
        }
        if (mc.player != null) {
            ModuleManager.tick(mc);
        }
    }
}
