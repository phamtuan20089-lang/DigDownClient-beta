package dev.digdown.client;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Properties;

import net.fabricmc.loader.api.FabricLoader;

/**
 * Lưu trạng thái của DigDown vào config/digdown.properties: chức năng nào đang bật, các tùy chọn,
 * và kích cỡ, vị trí của Click GUI. Đọc khi game khởi động, ghi mỗi khi có thay đổi và khi thoát game.
 */
public final class ConfigManager {
    private static final String FILE_NAME = "digdown.properties";
    // Tăng số này khi đổi kích cỡ mặc định của Click GUI: config cũ (phiên bản thấp hơn) sẽ bị bỏ qua phần kích cỡ/vị trí
    private static final int GUI_LAYOUT_VERSION = 2;

    private ConfigManager() {
    }

    private static Path file() {
        return FabricLoader.getInstance().getConfigDir().resolve(FILE_NAME);
    }

    /** Đọc config. Khóa nào không có trong file thì giữ giá trị mặc định của khóa đó. */
    public static void load() {
        Path path = file();
        if (!Files.exists(path)) {
            return;
        }
        Properties p = new Properties();
        try (Reader reader = Files.newBufferedReader(path, StandardCharsets.UTF_8)) {
            p.load(reader);
        } catch (IOException e) {
            DigDownClient.LOGGER.warn("Could not read " + path + ", using defaults", e);
            return;
        }

        for (Module m : ModuleManager.all()) {
            String prefix = "module." + m.getName();
            m.setEnabled(readBool(p, prefix, m.isEnabled()));
            for (Module.BoolSetting s : m.getSettings()) {
                s.set(readBool(p, prefix + "." + s.getName(), s.get()));
            }
        }
        for (Module.BoolSetting s : ClientSettings.all()) {
            s.set(readBool(p, "client." + s.getName(), s.get()));
        }

        if (readInt(p, "gui.layout", 0) >= GUI_LAYOUT_VERSION) {
            ClickGuiScreen.winW = readInt(p, "gui.width", ClickGuiScreen.winW);
            ClickGuiScreen.winH = readInt(p, "gui.height", ClickGuiScreen.winH);
            ClickGuiScreen.posX = readInt(p, "gui.x", ClickGuiScreen.posX);
            ClickGuiScreen.posY = readInt(p, "gui.y", ClickGuiScreen.posY);
        }
    }

    /** Ghi toàn bộ trạng thái ra file. Lỗi khi ghi chỉ ghi log, không làm game lỗi. */
    public static void save() {
        Properties p = new Properties();
        for (Module m : ModuleManager.all()) {
            String prefix = "module." + m.getName();
            p.setProperty(prefix, Boolean.toString(m.isEnabled()));
            for (Module.BoolSetting s : m.getSettings()) {
                p.setProperty(prefix + "." + s.getName(), Boolean.toString(s.get()));
            }
        }
        for (Module.BoolSetting s : ClientSettings.all()) {
            p.setProperty("client." + s.getName(), Boolean.toString(s.get()));
        }
        p.setProperty("gui.layout", Integer.toString(GUI_LAYOUT_VERSION));
        p.setProperty("gui.width", Integer.toString(ClickGuiScreen.winW));
        p.setProperty("gui.height", Integer.toString(ClickGuiScreen.winH));
        p.setProperty("gui.x", Integer.toString(ClickGuiScreen.posX));
        p.setProperty("gui.y", Integer.toString(ClickGuiScreen.posY));

        Path path = file();
        try {
            Files.createDirectories(path.getParent());
            try (Writer writer = Files.newBufferedWriter(path, StandardCharsets.UTF_8)) {
                p.store(writer, "DigDown Client settings");
            }
        } catch (IOException e) {
            DigDownClient.LOGGER.warn("Could not save " + path, e);
        }
    }

    private static boolean readBool(Properties p, String key, boolean fallback) {
        String value = p.getProperty(key);
        return value == null ? fallback : Boolean.parseBoolean(value.trim());
    }

    private static int readInt(Properties p, String key, int fallback) {
        String value = p.getProperty(key);
        if (value == null) {
            return fallback;
        }
        try {
            return Integer.parseInt(value.trim());
        } catch (NumberFormatException e) {
            return fallback;
        }
    }
}
