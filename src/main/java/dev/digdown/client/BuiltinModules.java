package dev.digdown.client;

import net.minecraft.client.Minecraft;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;

/** Nơi khai báo các chức năng có sẵn. Thêm chức năng mới: viết class mới rồi thêm vào registerAll(). */
public final class BuiltinModules {

    private BuiltinModules() {
    }

    /** Module HUD duy nhất: DigDownHud đọc trạng thái và các tùy chọn từ đây. */
    public static Hud hud;

    public static void registerAll() {
        ModuleManager.register(new Sprint());
        ModuleManager.register(new AutoWalk());
        ModuleManager.register(new Fullbright());

        hud = new Hud();
        hud.setEnabled(true); // HUD bật sẵn
        ModuleManager.register(hud);
    }

    /** Gom toàn bộ HUD thành một chức năng, các dòng hiển thị là tùy chọn con (click phải vào HUD để chỉnh). */
    public static final class Hud extends Module {
        public final BoolSetting showName = addSetting("Client name", "Show the DigDown label", true);
        public final BoolSetting showFps = addSetting("FPS", "Show frames per second", true);
        public final BoolSetting showCoords = addSetting("Coordinates", "Show your XYZ position", true);

        public Hud() {
            super("HUD", "Name, FPS and coordinates overlay", Category.CLIENT);
        }
    }

    /** Tự chạy nhanh khi đang đi tiến. */
    public static final class Sprint extends Module {
        public Sprint() {
            super("Sprint", "Always sprint while moving forward", Category.MOVEMENT);
        }

        @Override
        public void onTick(Minecraft mc) {
            if (mc.player != null && mc.options.keyUp.isDown()) {
                mc.options.keySprint.setDown(true);
            }
        }

        @Override
        protected void onDisable() {
            Minecraft.getInstance().options.keySprint.setDown(false);
        }
    }

    /** Tự đi tiến, tiện khi cầm điện thoại bằng một tay. */
    public static final class AutoWalk extends Module {
        public AutoWalk() {
            super("Auto Walk", "Walk forward without holding a key", Category.MOVEMENT);
        }

        @Override
        public void onTick(Minecraft mc) {
            if (mc.player != null) {
                mc.options.keyUp.setDown(true);
            }
        }

        @Override
        protected void onDisable() {
            Minecraft.getInstance().options.keyUp.setDown(false);
        }
    }

    /** Nhìn rõ trong bóng tối bằng hiệu ứng night vision phía client. */
    public static final class Fullbright extends Module {
        public Fullbright() {
            super("Fullbright", "See clearly in the dark", Category.RENDER);
        }

        @Override
        public void onTick(Minecraft mc) {
            if (mc.player == null) {
                return;
            }
            MobEffectInstance current = mc.player.getEffect(MobEffects.NIGHT_VISION);
            if (current == null || current.getDuration() < 400) {
                mc.player.addEffect(new MobEffectInstance(MobEffects.NIGHT_VISION, 100000, 0, false, false, false));
            }
        }

        @Override
        protected void onDisable() {
            Minecraft mc = Minecraft.getInstance();
            if (mc.player != null) {
                mc.player.removeEffect(MobEffects.NIGHT_VISION);
            }
        }
    }
}
