package dev.digdown.client;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import net.minecraft.client.Minecraft;

/** Lớp cơ sở cho mọi chức năng. Muốn thêm chức năng mới: kế thừa lớp này. */
public abstract class Module {

    public enum Category {
        MOVEMENT("Movement"),
        RENDER("Render"),
        MISC("Misc"),
        CLIENT("Client");

        private final String label;

        Category(String label) {
            this.label = label;
        }

        public String label() {
            return label;
        }
    }

    /** Một tùy chọn bật/tắt. Dùng cho tùy chọn con của module và cho cài đặt chung của client. */
    public static final class BoolSetting {
        private final String name;
        private final String description;
        private boolean value;

        public BoolSetting(String name, String description, boolean defaultValue) {
            this.name = name;
            this.description = description;
            this.value = defaultValue;
        }

        public String getName() {
            return name;
        }

        public String getDescription() {
            return description;
        }

        public boolean get() {
            return value;
        }

        public void set(boolean value) {
            this.value = value;
        }

        public void toggle() {
            value = !value;
        }
    }

    private final String name;
    private final String description;
    private final Category category;
    private final List<BoolSetting> settings = new ArrayList<>();
    private boolean enabled;

    protected Module(String name, String description, Category category) {
        this.name = name;
        this.description = description;
        this.category = category;
    }

    public String getName() {
        return name;
    }

    public String getDescription() {
        return description;
    }

    public Category getCategory() {
        return category;
    }

    public boolean isEnabled() {
        return enabled;
    }

    /** Các tùy chọn con của module (click phải vào thẻ module trong Click GUI để mở hoặc đóng ngay bên dưới, nếu danh sách này không rỗng). */
    public List<BoolSetting> getSettings() {
        return Collections.unmodifiableList(settings);
    }

    protected BoolSetting addSetting(String name, String description, boolean defaultValue) {
        BoolSetting setting = new BoolSetting(name, description, defaultValue);
        settings.add(setting);
        return setting;
    }

    public void toggle() {
        setEnabled(!enabled);
    }

    public void setEnabled(boolean enabled) {
        if (this.enabled == enabled) {
            return;
        }
        this.enabled = enabled;
        if (enabled) {
            onEnable();
        } else {
            onDisable();
        }
    }

    /** Được gọi một lần khi bật. */
    protected void onEnable() {
    }

    /** Được gọi một lần khi tắt. */
    protected void onDisable() {
    }

    /** Được gọi mỗi tick (20 lần/giây) khi module đang bật và đang trong thế giới. */
    public void onTick(Minecraft mc) {
    }
}
