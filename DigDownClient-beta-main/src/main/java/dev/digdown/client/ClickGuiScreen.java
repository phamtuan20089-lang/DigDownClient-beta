package dev.digdown.client;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Random;
import java.util.Map;
import java.util.function.BooleanSupplier;

import org.joml.Matrix3x2fStack;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;

/**
 * Click GUI của DigDown: tab trên cùng, danh mục bên trái, thanh tìm kiếm, thẻ module có công tắc.
 * Click trái vào thẻ để bật/tắt module. Click phải vào thẻ có tùy chọn con để mở hoặc đóng
 * phần tùy chọn ngay bên dưới thẻ.
 * Mọi thay đổi đều có hoạt ảnh làm mượt: bật/tắt, đổi tab, đổi danh mục, mở tùy chọn con,
 * rê chuột, gõ tìm kiếm. Vẽ bằng GuiGraphics.
 */
public class ClickGuiScreen extends Screen {
    private static final int BG = 0xF0E8F7F2;
    private static final int CARD = 0xFFE0F2EA;
    private static final int CARD_HOVER = 0xFFCBE8DB;
    private static final int LINE = 0xFFB0D6C7;
    private static final int ACCENT = 0xFF24B78A;
    private static final int TEXT = 0xFF173A31;
    private static final int DIM = 0xFF55766A;
    private static final int SWITCH_OFF = 0xFF9AB9AC;

    private static final int HEADER = 30;
    private static final int SIDEBAR = 96;
    private static final int CARD_H = 32;
    private static final int GAP = 6;
    private static final int BAR_H = 16;
    private static final int SUB_INDENT = 14; // độ thụt vào của các tùy chọn con so với thẻ module

    // Độ bo góc (pixel)
    private static final int WINDOW_R = 14;
    private static final int CARD_R = 8;

    // Thời gian hoạt ảnh mở menu (mili giây)
    private static final long ANIM_MS = 220L;

    // Kích thước cửa sổ (pixel GUI). Kéo góc dưới bên phải để đổi kích cỡ, kéo thanh trên để di chuyển.
    private static final int DEFAULT_W = 420;
    private static final int DEFAULT_H = 250;
    private static final int MIN_W = 300;
    private static final int MIN_H = 180;
    private static final int GRIP = 14; // vùng kéo ở góc dưới bên phải

    // Màu viền
    private static final int WINDOW_BORDER = 0xFF9BCDB9;
    private static final int CARD_BORDER = 0xFFB0D6C7;

    // Tốc độ hoạt ảnh: càng lớn càng nhanh, khoảng 0,15 đến 0,3 giây là xong
    private static final float K_HOVER = 14F;  // rê chuột
    private static final float K_TOGGLE = 16F; // bật/tắt, đổi tab, đổi danh mục
    private static final float K_MOVE = 14F;   // trượt vị trí
    private static final float K_FADE = 12F;   // hiện dần, mờ dần
    private static final float K_EXPAND = 10F; // mở/đóng tùy chọn con
    private static final float K_PULSE = 6F;   // nháy khi vừa bấm
    private static final float ENTER_DY = 10F; // phần tử mới trượt lên từ dưới bao nhiêu pixel

    // Khóa cho các phần tử không phải module (dùng cho hoạt ảnh)
    private static final String CATEGORY = "category";

    private enum Tab {
        MODULES("MODULES"),
        SETTINGS("SETTINGS");

        private final String label;

        Tab(String label) {
            this.label = label;
        }
    }

    /** Một vùng bấm được, xây lại mỗi khung hình trong render(). Vùng thêm trước được ưu tiên. */
    private record Zone(int x1, int y1, int x2, int y2, Runnable action) {
        boolean contains(double x, double y) {
            return x >= x1 && x < x2 && y >= y1 && y < y2;
        }
    }

    /**
     * Một thẻ có công tắc. key là đối tượng dùng làm khóa hoạt ảnh (module hoặc tùy chọn).
     * settingsOf != null thì click phải vào thẻ sẽ mở/đóng tùy chọn con ngay bên dưới.
     */
    private record Card(Object key, String name, String desc, BooleanSupplier state, Runnable toggle,
                        Module settingsOf) {
    }

    /**
     * Một vị trí cần vẽ trong khung hình này: tọa độ đích, độ trong suốt đích và vùng cắt
     * (clipOwner != null thì chỉ hiện phần cao clipH bên dưới thẻ của clipOwner, dùng cho tùy chọn con).
     */
    private record Slot(Object key, Card card, float tx, float ty, float tw, float ta, Module clipOwner,
                        float clipH) {
    }

    /** Khóa của một giá trị hoạt ảnh: đối tượng sở hữu và tên thuộc tính. */
    private record Prop(Object owner, String name) {
    }

    // static để lần sau mở lại vẫn nhớ tab/danh mục đang chọn
    private static Tab currentTab = Tab.MODULES;
    private static Module.Category currentCategory = Module.Category.MOVEMENT;

    private final List<Zone> zones = new ArrayList<>();
    private final List<Zone> rightZones = new ArrayList<>(); // vùng click phải (mở/đóng tùy chọn con)
    private final long openedAt = System.currentTimeMillis();
    private float fade = 1.0F;
    private Module openedModule; // module đang mở tùy chọn con (null = không mở)
    private EditBox searchBox;

    // Kích thước và vị trí cửa sổ, được lưu vào config (posX/posY = -1 nghĩa là chưa đặt, sẽ căn giữa)
    static int winW = DEFAULT_W;
    static int winH = DEFAULT_H;
    static int posX = -1;
    static int posY = -1;

    // Trạng thái kéo
    private boolean resizing;
    private boolean moving;
    private double dragStartX;
    private double dragStartY;
    private int dragStartW;
    private int dragStartH;
    private int dragOffX;
    private int dragOffY;

    // Vùng cửa sổ đã vẽ ở khung hình trước, dùng để nhận biết chuột đang ở góc kéo hay thanh trên
    private int lastX;
    private int lastY;
    private int lastW;
    private int lastH;

    // ---- hoạt ảnh
    private final Map<Prop, Float> props = new HashMap<>(); // giá trị hiện tại của từng thuộc tính
    private Map<Object, Card> alive = new LinkedHashMap<>(); // phần tử đã vẽ khung trước, để mờ dần khi biến mất
    private long lastFrameNs = System.nanoTime();
    private float dt = 1F / 60F; // thời gian giữa hai khung hình (giây)

    // Tia sét trang trí nền: từng tia có chu kỳ spawn/strike/impact/fade độc lập.
    private static final Random LIGHTNING_RANDOM = new Random();
    private static long nextLightningAt = System.currentTimeMillis() + 900L;
    private static final List<LightningBolt> LIGHTNING = new ArrayList<>();
    private record LightningBolt(int startX, int endY, long bornAt, long seed) {}

    public ClickGuiScreen() {
        super(Component.literal("DigDown"));
        playClick(1.5F); // tiếng tick khi mở menu
    }

    @Override
    protected void init() {
        String old = searchBox != null ? searchBox.getValue() : "";
        searchBox = new EditBox(Minecraft.getInstance().font, 0, 0, 100, 10, Component.literal("Search"));
        searchBox.setBordered(false);
        searchBox.setMaxLength(24);
        searchBox.setHint(Component.literal("Search modules..."));
        searchBox.setValue(old);
        addWidget(searchBox);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    private static boolean animationOn() {
        return ClientSettings.GUI_ANIMATION.get();
    }

    private boolean animating() {
        return animationOn() && System.currentTimeMillis() - openedAt < ANIM_MS;
    }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        zones.clear();
        rightZones.clear();
        long now = System.nanoTime();
        dt = Math.min(0.05F, (now - lastFrameNs) / 1_000_000_000F);
        lastFrameNs = now;
        Font font = Minecraft.getInstance().font;

        // Tiến độ hoạt ảnh mở menu: 0 -> 1
        float t = animationOn() ? Math.min(1.0F, (System.currentTimeMillis() - openedAt) / (float) ANIM_MS) : 1.0F;
        fade = Math.min(1.0F, t * 1.6F);
        float scale = 0.85F + 0.15F * easeOutBack(t);

        int w = Math.min(width - 16, winW);
        int h = Math.min(height - 16, winH);
        int x = posX < 0 ? (width - w) / 2 : posX;
        int y = posY < 0 ? (height - h) / 2 : posY;
        x = clamp(x, 0, width - w);
        y = clamp(y, 0, height - h);
        posX = x;
        posY = y;
        lastX = x;
        lastY = y;
        lastW = w;
        lastH = h;

        // Phóng to từ giữa cửa sổ
        Matrix3x2fStack pose = g.pose();
        pose.pushMatrix();
        float pivotX = x + w / 2.0F;
        float pivotY = y + h / 2.0F;
        pose.translate(pivotX, pivotY);
        pose.scale(scale, scale);
        pose.translate(-pivotX, -pivotY);

        // Bóng đổ mềm quanh cửa sổ
        for (int i = 4; i >= 1; i--) {
            fillRounded(g, x - i, y - i + 3, x + w + i, y + h + i + 3, WINDOW_R + i,
                    mul(withAlpha(0xFF000000, 0.12F - i * 0.02F)));
        }

        // Khung cửa sổ bo góc: viền mờ và một đường sáng mỏng bên trong
        fillRounded(g, x, y, x + w, y + h, WINDOW_R, mul(BG));
        drawLightningBackground(g, x + 4, y + HEADER + 2, x + w - 4, y + h - 4);
        strokeRounded(g, x, y, x + w, y + h, WINDOW_R, mul(WINDOW_BORDER));
        strokeRounded(g, x + 1, y + 1, x + w - 1, y + h - 1, WINDOW_R - 1, mul(withAlpha(0xFFFFFFFF, 0.06F)));

        // ---- Thanh trên: logo + tên + tab + nút đóng
        fillRounded(g, x + 10, y + 6, x + 28, y + 24, 9, mul(ACCENT));
        drawText(g, font, "D", x + 10 + (18 - textWidth(font, "D")) / 2, y + 8, mul(TEXT));
        drawText(g, font, "C", x + 10 + (18 - textWidth(font, "C")) / 2, y + 16, mul(withAlpha(TEXT, 0.78F)));
        drawText(g, font, "DIGDOWN", x + 34, y + 11, mul(TEXT));

        int tx = x + 34 + textWidth(font, "DIGDOWN") + 14;
        for (Tab tab : Tab.values()) {
            int tw = textWidth(font, tab.label) + 16;
            boolean hoverNow = inside(mouseX, mouseY, tx, y + 5, tx + tw, y + HEADER - 4);
            float act = tween(tab, "act", currentTab == tab ? 1F : 0F, K_TOGGLE);
            float hov = tween(tab, "hov", hoverNow ? 1F : 0F, K_HOVER);
            float bgA = Math.max(act, hov);
            if (bgA > 0.02F) {
                fillRounded(g, tx, y + 5, tx + tw, y + HEADER - 4, CARD_R,
                        mul(withAlpha(lerpColor(CARD, CARD_HOVER, act), bgA)));
            }
            if (act > 0.02F) {
                strokeRounded(g, tx, y + 5, tx + tw, y + HEADER - 4, CARD_R, mul(withAlpha(ACCENT, act)));
            }
            drawText(g, font, tab.label, tx + 8, y + 11, mul(lerpColor(DIM, TEXT, act)));
            final Tab target = tab;
            zones.add(new Zone(tx, y + 5, tx + tw, y + HEADER - 4, () -> {
                currentTab = target;
                openedModule = null;
                playClick(1.0F);
            }));
            tx += tw + 4;
        }

        int closeX = x + w - 22;
        boolean closeHoverNow = inside(mouseX, mouseY, closeX, y + 8, closeX + 14, y + 22);
        float closeT = tween("close", "hov", closeHoverNow ? 1F : 0F, K_HOVER);
        fillRounded(g, closeX, y + 8, closeX + 14, y + 22, 7, mul(lerpColor(CARD, CARD_HOVER, closeT)));
        drawText(g, font, "x", closeX + (14 - textWidth(font, "x")) / 2, y + 11, mul(DIM));
        zones.add(new Zone(closeX, y + 8, closeX + 14, y + 22, () -> {
            playClick(0.8F);
            onClose();
        }));

        g.fill(x + 6, y + HEADER, x + w - 6, y + HEADER + 1, mul(LINE));

        // ---- Thân
        int bodyTop = y + HEADER + 1;
        int bottom = y + h - 14;
        boolean modulesTab = currentTab == Tab.MODULES;
        int contentLeft = x + 10;
        int contentRight = x + w - 10;
        int top = bodyTop + 8;
        List<Card> cards = new ArrayList<>();
        boolean searchVisible = false;

        if (modulesTab) {
            // Thanh danh mục bên trái
            List<Module.Category> cats = new ArrayList<>();
            for (Module.Category c : Module.Category.values()) {
                if (!ModuleManager.byCategory(c).isEmpty()) {
                    cats.add(c);
                }
            }
            if (!cats.isEmpty() && !cats.contains(currentCategory)) {
                currentCategory = cats.get(0);
            }
            boolean searchEmpty = searchBox == null || searchBox.getValue().isEmpty();
            int selIndex = cats.indexOf(currentCategory);
            int rx1 = x + 8;
            int rx2 = x + SIDEBAR - 6;

            // Khung sáng của danh mục đang chọn trượt mượt sang danh mục mới
            float indY = tween(CATEGORY, "y", bodyTop + 8 + Math.max(0, selIndex) * 26F, K_MOVE);
            float indA = tween(CATEGORY, "a", searchEmpty && selIndex >= 0 ? 1F : 0F, K_FADE);
            if (indA > 0.02F) {
                fillRounded(g, rx1, Math.round(indY), rx2, Math.round(indY) + 22, CARD_R,
                        mul(withAlpha(CARD_HOVER, indA)));
                strokeRounded(g, rx1, Math.round(indY), rx2, Math.round(indY) + 22, CARD_R,
                        mul(withAlpha(ACCENT, indA)));
            }

            int rowY = bodyTop + 8;
            for (Module.Category c : cats) {
                boolean selected = c == currentCategory && searchEmpty;
                boolean hoverNow = inside(mouseX, mouseY, rx1, rowY, rx2, rowY + 22);
                float hovT = tween(c, "hov", hoverNow ? 1F : 0F, K_HOVER);
                float selT = tween(c, "sel", selected ? 1F : 0F, K_TOGGLE);
                float bgA = hovT * (1F - selT);
                if (bgA > 0.02F) {
                    fillRounded(g, rx1, rowY, rx2, rowY + 22, CARD_R, mul(withAlpha(CARD, bgA)));
                }
                fillRounded(g, rx1 + 8, rowY + 7, rx1 + 16, rowY + 15, 4, mul(lerpColor(DIM, ACCENT, selT)));
                drawText(g, font, c.label().toUpperCase(Locale.ROOT), rx1 + 22, rowY + 7,
                        mul(lerpColor(DIM, TEXT, selT)));
                final Module.Category target = c;
                zones.add(new Zone(rx1, rowY, rx2, rowY + 22, () -> {
                    currentCategory = target;
                    openedModule = null;
                    if (searchBox != null) {
                        searchBox.setValue("");
                    }
                    playClick(1.0F);
                }));
                rowY += 26;
            }
            drawText(g, font, "v1.0.0", x + 14, y + h - 18, mul(DIM));
            g.fill(x + SIDEBAR, bodyTop + 4, x + SIDEBAR + 1, y + h - 8, mul(LINE));
            contentLeft = x + SIDEBAR + 10;

            // Danh sách module: theo danh mục, hoặc tìm kiếm trên tất cả module
            String query = searchBox == null ? "" : searchBox.getValue().trim().toLowerCase(Locale.ROOT);
            List<Module> source = new ArrayList<>();
            if (query.isEmpty()) {
                source.addAll(ModuleManager.byCategory(currentCategory));
            } else {
                for (Module m : ModuleManager.all()) {
                    if (m.getName().toLowerCase(Locale.ROOT).contains(query)) {
                        source.add(m);
                    }
                }
            }
            // Module đang mở tùy chọn con nhưng không còn trong danh sách thì đóng lại
            if (openedModule != null && !source.contains(openedModule)) {
                openedModule = null;
            }
            for (Module m : source) {
                cards.add(new Card(m, m.getName(), m.getDescription(), m::isEnabled, m::toggle,
                        m.getSettings().isEmpty() ? null : m));
            }
            String rightLabel = cards.size() + (cards.size() == 1 ? " MODULE" : " MODULES");

            // Thanh tìm kiếm: viền sáng dần khi đang gõ
            int barW = Math.max(60, Math.min(160, contentRight - contentLeft - 90));
            boolean focused = searchBox != null && searchBox.isFocused();
            float focusT = tween("search", "focus", focused ? 1F : 0F, K_TOGGLE);
            fillRounded(g, contentLeft, top, contentLeft + barW, top + BAR_H, CARD_R, mul(CARD));
            strokeRounded(g, contentLeft, top, contentLeft + barW, top + BAR_H, CARD_R, mul(CARD_BORDER));
            if (focusT > 0.02F) {
                strokeRounded(g, contentLeft, top, contentLeft + barW, top + BAR_H, CARD_R,
                        mul(withAlpha(ACCENT, focusT)));
            }
            if (searchBox != null) {
                searchBox.setX(contentLeft + 8);
                searchBox.setY(top + 4);
                searchBox.setWidth(barW - 16);
            }
            searchVisible = true;

            int rw = textWidth(font, rightLabel);
            drawText(g, font, rightLabel, contentRight - rw, top + 4, mul(DIM));
            top += BAR_H + 8;
        } else {
            for (Module.BoolSetting s : ClientSettings.all()) {
                cards.add(new Card(s, s.getName(), s.getDescription(), s::get, s::toggle, null));
            }
        }

        if (searchBox != null) {
            searchBox.visible = searchVisible;
            if (searchVisible) {
                searchBox.render(g, mouseX, mouseY, partialTick);
            }
        }

        // Logo mờ ở nền vùng nội dung
        Matrix3x2fStack marker = g.pose();
        int markWidth = textWidth(font, "DIGDOWN");
        marker.pushMatrix();
        marker.translate((contentLeft + contentRight) / 2.0F, (top + bottom) / 2.0F + 14);
        marker.scale(3.0F, 3.0F);
        g.drawString(font, UiFont.text("DIGDOWN"), -markWidth / 2, -4, mul(0x14FFFFFF));
        marker.popMatrix();

        // ---- Bố cục: tính vị trí đích của từng phần tử. Phần hiển thị sẽ trượt tới vị trí này.
        // Module đang mở (hoặc đang đóng dần) được vẽ toàn chiều rộng, các tùy chọn con là hàng
        // riêng nằm ngay bên dưới. Chiều cao vùng tùy chọn con tăng/giảm theo độ mở.
        int cols = (contentRight - contentLeft) >= 240 ? 2 : 1;
        float cardW = (contentRight - contentLeft - GAP * (cols - 1)) / (float) cols;
        float fullW = contentRight - contentLeft;
        List<Slot> slots = new ArrayList<>();
        float cy = top;
        int col = 0;
        for (Card card : cards) {
            Module mod = card.settingsOf();
            float exp = mod == null ? 0F : tween(mod, "exp", mod == openedModule ? 1F : 0F, K_EXPAND);
            if (exp > 0.002F) {
                if (col != 0) { // hàng đang dở thì xuống hàng mới
                    cy += CARD_H + GAP;
                    col = 0;
                }
                if (cy + CARD_H > bottom) {
                    break;
                }
                slots.add(new Slot(card.key(), card, contentLeft, cy, fullW, 1F, null, 0F));
                float rowsTop = cy + CARD_H + GAP;
                float regionH = mod.getSettings().size() * (CARD_H + GAP) * exp;
                int idx = 0;
                for (Module.BoolSetting s : mod.getSettings()) {
                    float ry = rowsTop + idx * (CARD_H + GAP);
                    idx++;
                    if (ry + CARD_H > bottom) {
                        break;
                    }
                    slots.add(new Slot(s, new Card(s, s.getName(), s.getDescription(), s::get, s::toggle, null),
                            contentLeft + SUB_INDENT, ry, contentRight - contentLeft - SUB_INDENT, exp, mod,
                            regionH));
                }
                cy = rowsTop + regionH;
            } else {
                if (cy + CARD_H > bottom) {
                    break;
                }
                slots.add(new Slot(card.key(), card, contentLeft + col * (cardW + GAP), cy, cardW, 1F, null, 0F));
                col++;
                if (col >= cols) {
                    col = 0;
                    cy += CARD_H + GAP;
                }
            }
        }

        // ---- Vẽ: mỗi phần tử trượt và hiện dần tới vị trí đích
        Map<Object, Card> next = new LinkedHashMap<>();
        for (Slot s : slots) {
            seed(s.key(), "x", s.tx());
            seed(s.key(), "y", s.ty() + ENTER_DY);
            seed(s.key(), "w", s.tw());
            seed(s.key(), "a", 0F);
            float px = tween(s.key(), "x", s.tx(), K_MOVE);
            float py = tween(s.key(), "y", s.ty(), K_MOVE);
            float pw = tween(s.key(), "w", s.tw(), K_MOVE);
            float pa = tween(s.key(), "a", s.ta(), K_FADE);
            if (s.clipOwner() == null) {
                drawCell(g, font, s.key(), s.card(), px, py, pw, pa, true, mouseX, mouseY);
            } else {
                // Tùy chọn con được cắt theo chiều cao đang mở, nên mở/đóng trông như kéo rèm
                float clipTop = prop(s.clipOwner(), "y") + CARD_H + GAP;
                g.enableScissor(Math.round(contentLeft), Math.round(clipTop),
                        Math.round(contentRight), Math.round(clipTop + s.clipH()));
                drawCell(g, font, s.key(), s.card(), px, py, pw, pa, true, mouseX, mouseY);
                if (pa > 0.02F) {
                    int railX = Math.round(px) - 6;
                    g.fill(railX, Math.round(py) + 6, railX + 2, Math.round(py) + CARD_H - 6,
                            mul(withAlpha(LINE, pa)));
                }
                g.disableScissor();
            }
            next.put(s.key(), s.card());
        }

        // Phần tử vừa biến mất (đổi tab, đổi danh mục, lọc, đóng tùy chọn): mờ dần tại chỗ cũ
        for (Map.Entry<Object, Card> e : alive.entrySet()) {
            Object key = e.getKey();
            if (next.containsKey(key)) {
                continue;
            }
            float pa = tween(key, "a", 0F, K_FADE);
            if (pa > 0.02F) {
                drawCell(g, font, key, e.getValue(), prop(key, "x"), prop(key, "y"), prop(key, "w"), pa,
                        false, mouseX, mouseY);
                next.put(key, e.getValue());
            }
        }
        alive = next;

        // Góc kéo đổi kích cỡ: ba chấm chéo ở góc dưới bên phải, sáng lên khi rê chuột vào
        float gripT = tween("grip", "hov", hitGrip(mouseX, mouseY) ? 1F : 0F, K_HOVER);
        int gripColor = mul(lerpColor(DIM, ACCENT, gripT));
        for (int i = 0; i < 3; i++) {
            int gx = x + w - 4 - i * 3;
            int gy = y + h - 4 - i * 3;
            g.fill(gx, gy, gx + 2, gy + 2, gripColor);
        }

        pose.popMatrix();
        super.render(g, mouseX, mouseY, partialTick);
    }

    /**
     * Vẽ một thẻ (hoặc hàng tùy chọn con) tại vị trí (fx, fy) rộng fw, độ trong suốt a.
     * Công tắc, viền, nền và núm đều đổi mượt theo trạng thái. Chỉ thẻ đang hiện mới được bấm.
     */
    /** Nền mint sạch với các tia sét ngắn đánh xuống, chạy theo chu kỳ độc lập. */
    private static void drawLightningBackground(GuiGraphics g, int left, int top, int right, int bottom) {
        long now = System.currentTimeMillis();
        if (now >= nextLightningAt) {
            int count = LIGHTNING_RANDOM.nextInt(1, 3);
            for (int i = 0; i < count; i++) {
                int sx = left + LIGHTNING_RANDOM.nextInt(Math.max(1, right - left));
                int ey = top + LIGHTNING_RANDOM.nextInt(Math.max(1, bottom - top));
                LIGHTNING.add(new LightningBolt(sx, ey, now, LIGHTNING_RANDOM.nextLong()));
            }
            nextLightningAt = now + LIGHTNING_RANDOM.nextInt(900, 1900);
        }
        LIGHTNING.removeIf(b -> now - b.bornAt() > 420L);
        for (LightningBolt bolt : LIGHTNING) {
            float age = (now - bolt.bornAt()) / 420F; // spawn -> strike/impact -> fade
            float alpha = age < 0.18F ? age / 0.18F : age > 0.62F ? (1F - age) / 0.38F : 1F;
            alpha = Math.max(0F, Math.min(1F, alpha)) * 0.24F; // nhẹ để không che module
            Random r = new Random(bolt.seed());
            int px = bolt.startX();
            int py = top;
            int steps = Math.max(5, (bolt.endY() - top) / 5);
            for (int i = 0; i < steps; i++) {
                py = top + i * 5;
                px += r.nextInt(-5, 6);
                if (px < left || px > right) px = Math.max(left, Math.min(right, px));
                g.fill(px, py, px + 2, py + 4, withAlpha(0xFF54C9AE, alpha));
                if (i > 2 && r.nextFloat() < 0.13F) {
                    int branchX = px;
                    int branchY = py;
                    int dir = r.nextBoolean() ? 1 : -1;
                    for (int j = 0; j < 3 + r.nextInt(4); j++) {
                        branchX += dir * r.nextInt(2, 5);
                        branchY += 3;
                        g.fill(branchX, branchY, branchX + 1, branchY + 3, withAlpha(0xFF76DCC7, alpha * 0.75F));
                    }
                }
            }
            // Lóe impact ngắn ở đầu cuối, chỉ trong khoảng 90ms đầu của pha strike.
            if (age > 0.16F && age < 0.28F) {
                int flash = withAlpha(0xFFB7F5E7, alpha * 0.45F);
                g.fill(bolt.startX() - 3, bolt.endY() - 2, bolt.startX() + 4, bolt.endY() + 3, flash);
            }
        }
    }

    private void drawCell(GuiGraphics g, Font font, Object key, Card card,
                          float fx, float fy, float fw, float a, boolean interactive, int mouseX, int mouseY) {
        if (a < 0.02F) {
            return;
        }
        int x1 = Math.round(fx);
        int y1 = Math.round(fy);
        int x2 = Math.round(fx + fw);
        int y2 = y1 + CARD_H;

        boolean hoverNow = interactive && inside(mouseX, mouseY, x1, y1, x2, y2);
        float hover = tween(key, "hover", hoverNow ? 1F : 0F, K_HOVER);
        float onT = tween(key, "on", card.state().getAsBoolean() ? 1F : 0F, K_TOGGLE);
        float pulse = tween(key, "pulse", 0F, K_PULSE);

        // Nền đổi màu mượt khi rê chuột, sáng nhẹ rồi tắt khi vừa bấm
        fillRounded(g, x1, y1, x2, y2, CARD_R, mul(withAlpha(lerpColor(CARD, CARD_HOVER, hover), a)));
        if (pulse * a > 0.02F) {
            fillRounded(g, x1, y1, x2, y2, CARD_R, mul(withAlpha(ACCENT, a * pulse * 0.12F)));
        }

        // Viền mờ của thẻ
        strokeRounded(g, x1, y1, x2, y2, CARD_R, mul(withAlpha(CARD_BORDER, a)));

        // Viền sáng: hiện dần khi bật, nháy khi vừa bấm
        float ring = Math.max(onT, pulse);
        if (ring * a > 0.02F) {
            strokeRounded(g, x1, y1, x2, y2, CARD_R, mul(withAlpha(ACCENT, a * ring)));
        }

        // Công tắc: màu và núm trượt mượt, núm phình nhẹ ở giữa lúc chuyển
        int sx = x2 - 30;
        int sy = y1 + (CARD_H - 10) / 2;
        fillRounded(g, sx, sy, sx + 22, sy + 10, 5, mul(withAlpha(lerpColor(SWITCH_OFF, ACCENT, onT), a)));
        int kw = 8 + Math.round(8F * onT * (1F - onT));
        int kx = Math.round(sx + 1 + 12 * onT) + (8 - kw) / 2;
        fillRounded(g, kx, sy + 1, kx + kw, sy + 9, 4, mul(withAlpha(TEXT, a)));

        // Chữ
        int textLeft = x1 + 8;
        int textRight = sx - 6;
        drawText(g, font, card.name(), textLeft, y1 + 7, mul(withAlpha(TEXT, a)));
        drawText(g, font, fit(font, card.desc(), textRight - textLeft), textLeft, y1 + 19,
                mul(withAlpha(DIM, a)));

        if (interactive && a > 0.5F) {
            zones.add(new Zone(x1, y1, x2, y2, () -> {
                card.toggle().run();
                ConfigManager.save();
                props.put(new Prop(key, "pulse"), 1F);
                playClick(card.state().getAsBoolean() ? 1.3F : 0.8F);
            }));
            if (card.settingsOf() != null) {
                final Module target = card.settingsOf();
                rightZones.add(new Zone(x1, y1, x2, y2, () -> {
                    openedModule = openedModule == target ? null : target;
                    playClick(openedModule == null ? 0.9F : 1.1F);
                }));
            }
        }
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        // Bỏ qua click trong lúc menu đang phóng to (khoảng 0,2 giây)
        if (animating()) {
            return true;
        }
        double mx = event.x();
        double my = event.y();
        if (event.button() == 0 && hitGrip(mx, my)) {
            resizing = true;
            dragStartX = mx;
            dragStartY = my;
            dragStartW = lastW;
            dragStartH = lastH;
            return true;
        }
        if (event.button() == 0) {
            for (Zone zone : zones) {
                if (zone.contains(mx, my)) {
                    if (searchBox != null) {
                        searchBox.setFocused(false);
                    }
                    zone.action().run();
                    return true;
                }
            }
            // Kéo thanh trên (chỗ trống, không phải tab hay nút đóng) để di chuyển cửa sổ
            if (hitHeader(mx, my)) {
                moving = true;
                dragOffX = (int) (mx - lastX);
                dragOffY = (int) (my - lastY);
                return true;
            }
        } else if (event.button() == 1) {
            // Chuột phải vào góc kéo: đặt lại kích cỡ mặc định
            if (hitGrip(mx, my)) {
                winW = DEFAULT_W;
                winH = DEFAULT_H;
                ConfigManager.save();
                playClick(0.9F);
                return true;
            }
            for (Zone zone : rightZones) {
                if (zone.contains(mx, my)) {
                    if (searchBox != null) {
                        searchBox.setFocused(false);
                    }
                    zone.action().run();
                    return true;
                }
            }
        }
        return super.mouseClicked(event, doubleClick);
    }

    @Override
    public boolean mouseDragged(MouseButtonEvent event, double dragX, double dragY) {
        if (resizing) {
            // Giữ nguyên góc trên bên trái, chỉ kéo góc dưới bên phải; không cho cửa sổ tràn khỏi màn hình
            int maxW = Math.max(MIN_W, Math.min(width - 16, width - posX - 8));
            int maxH = Math.max(MIN_H, Math.min(height - 16, height - posY - 8));
            winW = clamp((int) (dragStartW + (event.x() - dragStartX)), MIN_W, maxW);
            winH = clamp((int) (dragStartH + (event.y() - dragStartY)), MIN_H, maxH);
            return true;
        }
        if (moving) {
            posX = clamp((int) (event.x() - dragOffX), 0, width - lastW);
            posY = clamp((int) (event.y() - dragOffY), 0, height - lastH);
            return true;
        }
        return super.mouseDragged(event, dragX, dragY);
    }

    @Override
    public boolean mouseReleased(MouseButtonEvent event) {
        if (resizing || moving) {
            resizing = false;
            moving = false;
            ConfigManager.save();
            return true;
        }
        return super.mouseReleased(event);
    }

    // ---------------------------------------------------------------- hoạt ảnh

    /**
     * Làm mượt giá trị hiện tại về target theo hàm mũ, k càng lớn thì càng nhanh.
     * Lần đầu gặp thuộc tính thì đặt thẳng vào target, không chạy hoạt ảnh.
     */
    private float tween(Object owner, String name, float target, float k) {
        Prop p = new Prop(owner, name);
        float cur = props.getOrDefault(p, target);
        float next = cur + (target - cur) * (1F - (float) Math.exp(-k * dt));
        if (Math.abs(target - next) < 0.002F) {
            next = target;
        }
        props.put(p, next);
        return next;
    }

    /** Đặt giá trị bắt đầu cho phần tử mới xuất hiện (không ghi đè nếu đã có). */
    private void seed(Object owner, String name, float value) {
        props.putIfAbsent(new Prop(owner, name), value);
    }

    private float prop(Object owner, String name) {
        return props.getOrDefault(new Prop(owner, name), 0F);
    }

    /** Đổi độ trong suốt của màu, a từ 0 (trong suốt) đến 1 (giữ nguyên). */
    private static int withAlpha(int argb, float a) {
        int base = argb >>> 24;
        int na = Math.round(base * Math.max(0F, Math.min(1F, a)));
        return (na << 24) | (argb & 0x00FFFFFF);
    }

    /** Trộn hai màu: t = 0 ra màu a, t = 1 ra màu b. */
    private static int lerpColor(int a, int b, float t) {
        float k = Math.max(0F, Math.min(1F, t));
        int out = 0;
        for (int shift = 24; shift >= 0; shift -= 8) {
            int ca = (a >>> shift) & 0xFF;
            int cb = (b >>> shift) & 0xFF;
            out |= (Math.round(ca + (cb - ca) * k) & 0xFF) << shift;
        }
        return out;
    }

    /** Chuột có nằm trong góc kéo đổi kích cỡ không (dựa trên khung hình vừa vẽ). */
    private boolean hitGrip(double mx, double my) {
        return mx >= lastX + lastW - GRIP && mx < lastX + lastW
                && my >= lastY + lastH - GRIP && my < lastY + lastH;
    }

    /** Chuột có nằm trên thanh trên của cửa sổ không. */
    private boolean hitHeader(double mx, double my) {
        return mx >= lastX + 6 && mx < lastX + lastW - 6 && my >= lastY && my < lastY + HEADER;
    }

    private static int clamp(int v, int lo, int hi) {
        return Math.max(lo, Math.min(hi, v));
    }

    // ---------------------------------------------------------------- chữ (font DigDown)

    private static void drawText(GuiGraphics g, Font font, String s, int x, int y, int color) {
        g.drawString(font, UiFont.text(s), x, y, color);
    }

    private static int textWidth(Font font, String s) {
        return font.width(UiFont.text(s));
    }

    /** Cắt chuỗi và thêm "..." nếu quá rộng. */
    private static String fit(Font font, String s, int maxWidth) {
        if (textWidth(font, s) <= maxWidth) {
            return s;
        }
        String cut = s;
        while (!cut.isEmpty() && textWidth(font, cut + "...") > maxWidth) {
            cut = cut.substring(0, cut.length() - 1);
        }
        return cut + "...";
    }

    // ---------------------------------------------------------------- tiện ích

    /** Tiếng tick của giao diện. pitch cao = tiếng cao hơn. Tắt được trong tab SETTINGS. */
    private static void playClick(float pitch) {
        if (!ClientSettings.GUI_SOUND.get()) {
            return;
        }
        Minecraft.getInstance().getSoundManager()
                .play(SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK, pitch));
    }

    /** Nhân độ trong suốt của màu với hệ số mờ dần của hoạt ảnh mở menu. */
    private int mul(int argb) {
        int a = (int) ((argb >>> 24) * fade);
        if (a < 8) {
            a = 8; // alpha quá thấp có thể bị game vẽ sai
        }
        return (a << 24) | (argb & 0x00FFFFFF);
    }

    /** Chuyển động "bật" nhẹ: vượt qua 1 một chút rồi về 1. */
    private static float easeOutBack(float t) {
        float c1 = 1.70158F;
        float c3 = c1 + 1.0F;
        float p = t - 1.0F;
        return 1.0F + c3 * p * p * p + c1 * p * p;
    }

    private static boolean inside(int px, int py, int x1, int y1, int x2, int y2) {
        return px >= x1 && px < x2 && py >= y1 && py < y2;
    }

    /** Khoảng thụt vào của hàng thứ i (tính từ mép trên) để tạo góc bo bán kính r. */
    private static int inset(int r, int i) {
        double dy = r - i - 0.5;
        return r - (int) Math.round(Math.sqrt(r * r - dy * dy));
    }

    /** Hình chữ nhật đặc bo góc. */
    private static void fillRounded(GuiGraphics g, int x1, int y1, int x2, int y2, int r, int color) {
        r = Math.min(r, Math.min((x2 - x1) / 2, (y2 - y1) / 2));
        if (r <= 0) {
            g.fill(x1, y1, x2, y2, color);
            return;
        }
        for (int i = 0; i < r; i++) {
            int in = inset(r, i);
            g.fill(x1 + in, y1 + i, x2 - in, y1 + i + 1, color);
            g.fill(x1 + in, y2 - i - 1, x2 - in, y2 - i, color);
        }
        g.fill(x1, y1 + r, x2, y2 - r, color);
    }

    /** Viền 1 pixel bo góc (không tô phần bên trong). */
    private static void strokeRounded(GuiGraphics g, int x1, int y1, int x2, int y2, int r, int color) {
        r = Math.min(r, Math.min((x2 - x1) / 2, (y2 - y1) / 2));
        if (r <= 0) {
            g.fill(x1, y1, x2, y1 + 1, color);
            g.fill(x1, y2 - 1, x2, y2, color);
            g.fill(x1, y1, x1 + 1, y2, color);
            g.fill(x2 - 1, y1, x2, y2, color);
            return;
        }
        int edge = inset(r, 0);
        g.fill(x1 + edge, y1, x2 - edge, y1 + 1, color);
        g.fill(x1 + edge, y2 - 1, x2 - edge, y2, color);
        for (int i = 1; i < r; i++) {
            int cur = inset(r, i);
            int wid = Math.max(1, inset(r, i - 1) - cur);
            // góc trên
            g.fill(x1 + cur, y1 + i, x1 + cur + wid, y1 + i + 1, color);
            g.fill(x2 - cur - wid, y1 + i, x2 - cur, y1 + i + 1, color);
            // góc dưới
            g.fill(x1 + cur, y2 - i - 1, x1 + cur + wid, y2 - i, color);
            g.fill(x2 - cur - wid, y2 - i - 1, x2 - cur, y2 - i, color);
        }
        g.fill(x1, y1 + r, x1 + 1, y2 - r, color);
        g.fill(x2 - 1, y1 + r, x2, y2 - r, color);
    }
}
