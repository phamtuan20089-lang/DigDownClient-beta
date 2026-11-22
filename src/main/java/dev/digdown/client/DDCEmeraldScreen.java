package dev.digdown.client;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.text.Text;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/** DDC Emerald visual ClickGUI prototype. UI toggles are visual-only. */
public class DDCEmeraldScreen extends Screen {
    private static final int BG = 0xD916292B;
    private static final int PANEL = 0xD9223438;
    private static final int PANEL_HOVER = 0xE02A4145;
    private static final int EMERALD = 0xFF20D9B0;
    private static final int EMERALD_DIM = 0xFF149E82;
    private static final int TEXT = 0xFFEAF8F5;
    private static final int MUTED = 0xFF9BB8B2;

    private final List<ModuleEntry> modules = new ArrayList<>();
    private final Random random = new Random();
    private long lastStrikeAt = 0;
    private long strikeStart = 0;
    private int strikeX = 0;
    private boolean strikeActive = false;
    private List<int[]> strikePoints = new ArrayList<>();
    private String selectedCategory = "Combat";
    private String selectedModule = "KillAura";
    private int panelX, panelY, panelW, panelH;

    public DDCEmeraldScreen() {
        super(Text.literal("DDC Emerald"));
        modules.add(new ModuleEntry("KillAura", "Combat module", true));
        modules.add(new ModuleEntry("AimAssist", "Aim helper", false));
        modules.add(new ModuleEntry("TriggerBot", "Click helper", false));
        modules.add(new ModuleEntry("AntiBot", "Entity filter", true));
        modules.add(new ModuleEntry("AutoTotem", "Totem helper", false));
        modules.add(new ModuleEntry("AutoPotion", "Potion helper", false));
    }

    @Override
    public void render(DrawContext ctx, int mouseX, int mouseY, float delta) {
        super.render(ctx, mouseX, mouseY, delta);
        int sw = width, sh = height;
        ctx.fill(0, 0, sw, sh, 0xB80B181A);
        // Soft mint backdrop glow, intentionally flat and lightweight.
        ctx.fill(0, 0, sw, sh, 0x2011B99A);
        panelW = Math.min(930, sw - 36); panelH = Math.min(570, sh - 36);
        panelX = (sw - panelW) / 2; panelY = (sh - panelH) / 2;
        rounded(ctx, panelX, panelY, panelW, panelH, 12, 0xE6122528);
        roundedOutline(ctx, panelX, panelY, panelW, panelH, 2, 0xFF42E8C1);
        renderLightning(ctx, panelX, panelY, panelW, panelH);
        renderHeader(ctx);
        renderSidebar(ctx, mouseX, mouseY);
        renderModules(ctx, mouseX, mouseY);
        renderSettings(ctx);
        renderFooter(ctx);
    }

    private void renderHeader(DrawContext ctx) {
        ctx.drawText(textRenderer, "D", panelX + 24, panelY + 18, 0xFF25E4B4, false);
        ctx.drawText(textRenderer, "C", panelX + 31, panelY + 39, 0xFF7AF4D2, false);
        ctx.drawText(textRenderer, "DigDown Client", panelX + 24, panelY + 61, TEXT, false);
        rounded(ctx, panelX + 238, panelY + 20, 340, 28, 14, 0xB52D4146);
        ctx.drawText(textRenderer, "Search modules...", panelX + 254, panelY + 30, MUTED, false);
        ctx.drawText(textRenderer, "✦     ◉     ⚙     ×", panelX + panelW - 148, panelY + 28, TEXT, false);
    }

    private void renderSidebar(DrawContext ctx, int mx, int my) {
        String[] cats = {"Home", "Combat", "Movement", "Render", "Player", "Misc", "Config"};
        int x = panelX + 18, y = panelY + 100, w = 166, h = 42;
        for (String cat : cats) {
            boolean active = cat.equals(selectedCategory);
            boolean hover = mx >= x && mx <= x+w && my >= y && my <= y+h;
            rounded(ctx, x, y, w, h, 10, active ? 0xD025B99A : (hover ? PANEL_HOVER : PANEL));
            if (active) roundedOutline(ctx, x, y, w, h, 1, EMERALD);
            ctx.drawText(textRenderer, cat, x + 14, y + 15, active ? EMERALD : TEXT, false);
            y += 48;
        }
    }


    private void renderModules(DrawContext ctx, int mx, int my) {
        int x = panelX + 196, y = panelY + 100, w = 320, h = 50;
        for (ModuleEntry module : modules) {
            boolean hover = mx >= x && mx <= x+w && my >= y && my <= y+h;
            boolean active = module.name.equals(selectedModule);
            rounded(ctx, x, y, w, h, 10, active ? 0xD01B554A : (hover ? PANEL_HOVER : PANEL));
            if (active) roundedOutline(ctx, x, y, w, h, 1, EMERALD);
            ctx.drawText(textRenderer, module.name, x + 14, y + 12, TEXT, false);
            ctx.drawText(textRenderer, module.description, x + 14, y + 29, MUTED, false);
            drawToggle(ctx, x + w - 48, y + 14, module.enabled);
            y += 58;
        }
    }

    private void renderSettings(DrawContext ctx) {
        int x = panelX + 536, y = panelY + 100, w = panelW - 554, h = panelH - 122;
        rounded(ctx, x, y, w, h, 10, 0xB9223539);
        ctx.drawText(textRenderer, selectedModule, x + 18, y + 18, TEXT, false);
        rounded(ctx, x + 16, y + 44, 98, 25, 8, 0xD01FB89A);
        ctx.drawText(textRenderer, "General", x + 32, y + 52, TEXT, false);
        ctx.drawText(textRenderer, "Targets", x + 130, y + 52, MUTED, false);
        ctx.drawText(textRenderer, "Modes", x + 225, y + 52, MUTED, false);
        String[] labels = {"Range", "Speed", "Rotation"};
        for (int i=0; i<labels.length; i++) {
            int yy = y + 98 + i*62;
            ctx.drawText(textRenderer, labels[i], x + 18, yy, TEXT, false);
            rounded(ctx, x + 18, yy + 20, w - 80, 5, 2, 0xFF40565B);
            rounded(ctx, x + 18, yy + 20, (int)((w - 80) * (0.30f + i*0.18f)), 5, 2, EMERALD);
        }
        String[] toggles = {"Only Players", "Through Walls", "Keep Sprint"};
        for (int i=0; i<toggles.length; i++) {
            int yy = y + h - 92 + i*28;
            ctx.drawText(textRenderer, toggles[i], x + 18, yy, TEXT, false);
            drawToggle(ctx, x + w - 46, yy - 1, i != 1);
        }
    }

    private void renderFooter(DrawContext ctx) {
        ctx.drawText(textRenderer, "●  DDC v1.0.0", panelX + 24, panelY + panelH - 25, EMERALD, false);
        ctx.drawText(textRenderer, "Minecraft 1.21.x", panelX + 136, panelY + panelH - 25, MUTED, false);
    }

    private void renderLightning(DrawContext ctx, int x, int y, int w, int h) {
        long now = System.currentTimeMillis();
        if (!strikeActive && now - lastStrikeAt > 850 + random.nextInt(1500)) {
            strikeActive = true; strikeStart = now; lastStrikeAt = now;
            strikeX = x + 20 + random.nextInt(Math.max(1, w - 40));
            strikePoints = new ArrayList<>();
            int px = strikeX, py = y - 5;
            strikePoints.add(new int[]{px, py});
            while (py < y + h - 15) {
                py += 12 + random.nextInt(22);
                px += random.nextInt(25) - 12;
                px = Math.max(x + 8, Math.min(x + w - 8, px));
                strikePoints.add(new int[]{px, Math.min(y+h-15, py)});
            }
        }
        if (strikeActive) {
            long age = now - strikeStart;
            if (age > 230) { strikeActive = false; return; }
            int alpha = age < 65 ? 255 : Math.max(0, 255 - (int)((age-65)*1.5));
            int color = (alpha << 24) | 0x00CFFFF0;
            for (int i=1; i<strikePoints.size(); i++) {
                int[] a = strikePoints.get(i-1), b = strikePoints.get(i);
                drawPixelLine(ctx, a[0], a[1], b[0], b[1], color);
                drawPixelLine(ctx, a[0]+1, a[1], b[0]+1, b[1], 0x5530E6C0);
            }
        }
    }


    private void drawPixelLine(DrawContext ctx, int x0, int y0, int x1, int y1, int color) {
        int dx = Math.abs(x1-x0), sx = x0<x1 ? 1 : -1;
        int dy = -Math.abs(y1-y0), sy = y0<y1 ? 1 : -1;
        int err = dx+dy;
        while (true) {
            ctx.fill(x0, y0, x0+2, y0+2, color);
            if (x0==x1 && y0==y1) break;
            int e2=2*err;
            if (e2>=dy) { err+=dy; x0+=sx; }
            if (e2<=dx) { err+=dx; y0+=sy; }
        }
    }

    private void drawToggle(DrawContext ctx, int x, int y, boolean enabled) {
        rounded(ctx, x, y, 36, 18, 9, enabled ? 0xFF18CBA5 : 0xFF506267);
        ctx.fill(x + (enabled ? 20 : 2), y + 2, x + (enabled ? 34 : 16), y + 16, 0xFFFFFFFF);
    }

    private void rounded(DrawContext ctx, int x, int y, int w, int h, int r, int color) {
        if (w<=0 || h<=0) return;
        ctx.fill(x+r, y, x+w-r, y+h, color);
        ctx.fill(x, y+r, x+w, y+h-r, color);
        // Small stepped corners approximating rounded rectangles without shaders.
        for (int i=0; i<r; i++) {
            int inset = r - (int)Math.sqrt(Math.max(0, r*r - (r-i)*(r-i)));
            ctx.fill(x+inset, y+i, x+w-inset, y+i+1, color);
            ctx.fill(x+inset, y+h-i-1, x+w-inset, y+h-i, color);
        }
    }

    private void roundedOutline(DrawContext ctx, int x, int y, int w, int h, int thickness, int color) {
        ctx.fill(x, y, x+w, y+thickness, color);
        ctx.fill(x, y+h-thickness, x+w, y+h, color);
        ctx.fill(x, y, x+thickness, y+h, color);
        ctx.fill(x+w-thickness, y, x+w, y+h, color);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button == 0) {
            int x = panelX + 196, y = panelY + 100, w = 320, h = 50;
            for (ModuleEntry module : modules) {
                if (mouseX >= x && mouseX <= x+w && mouseY >= y && mouseY <= y+h) {
                    module.enabled = !module.enabled;
                    selectedModule = module.name;
                    return true;
                }
                y += 58;
            }
            int sx = panelX + 18, sy = panelY + 100, sw = 166, sh = 42;
            String[] cats = {"Home", "Combat", "Movement", "Render", "Player", "Misc", "Config"};
            for (String cat : cats) {
                if (mouseX >= sx && mouseX <= sx+sw && mouseY >= sy && mouseY <= sy+sh) {
                    selectedCategory = cat;
                    return true;
                }
                sy += 48;
            }
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }


    @Override
    public boolean shouldPause() { return false; }


    private static class ModuleEntry {
        final String name, description;
        boolean enabled;
        ModuleEntry(String name, String description, boolean enabled) {
            this.name=name; this.description=description; this.enabled=enabled;
        }
    }
}
