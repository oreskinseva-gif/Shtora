package com.shtora.client.gui;

import com.shtora.client.Category;
import com.shtora.client.Module;
import com.shtora.client.ModuleManager;
import com.shtora.client.setting.BoolSetting;
import com.shtora.client.setting.KeybindSetting;
import com.shtora.client.setting.ModeSetting;
import com.shtora.client.setting.NumberSetting;
import com.shtora.client.setting.Setting;
import com.shtora.client.util.Anim;
import com.shtora.client.util.ConfigManager;
import com.shtora.client.util.Draw;
import com.shtora.client.util.Logo;
import com.shtora.client.util.Theme;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.text.Text;
import org.lwjgl.glfw.GLFW;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/**
 * Меню клиента — по образцу (см. ВОТ MENU.jpg).
 *
 * Вид: чисто чёрный фон, скруглённые тёмные карточки-категории, СИНИЙ
 * акцент, переключатели-пилюли, сверху по центру готический логотип SHTORA,
 * снизу — поле поиска и квадратики выбора акцентного цвета. Панель Client
 * подсвечена янтарным, как «Server modules» в макете.
 *
 * Управление:
 *  - ЛКМ по строке модуля — включить/выключить;
 *  - ПКМ по строке модуля — раскрыть настройки;
 *  - ЛКМ по шапке панели — свернуть/развернуть категорию;
 *  - ★ и 🗑 в шапке — избранное и удаление (удалённая панель подсвечена жёлтым);
 *  - сверху слева — фильтры ВСЕ / ИЗБР / УДАЛ; справа — кнопка CFG (конфиги);
 *  - снизу — поиск и квадратики акцентного цвета.
 */
public class ClickGuiScreen extends Screen {
    // ------------------------------------------------------------------ геометрия
    private static final int PAD = 6;         // внешний отступ
    private static final int TOPBAR = 24;     // верхняя полоса
    private static final int BOTBAR = 24;     // нижняя полоса
    private static final int HEAD = 18;       // шапка карточки
    private static final int ROW = 18;        // строка модуля
    private static final int GAP = 6;
    private static final int RAD = 6;         // скругление карточек

    // ------------------------------------------------------------------ акценты
    private static final int[] SWATCH = {0xFF5867, 0x2E86FF, 0xFFA738, 0xF853D5, 0x5EDE79, 0x915EF7};
    private static final float[] SWATCH_HUE = {355f, 210f, 36f, 318f, 133f, 262f};

    // ------------------------------------------------------------------ состояние
    private static final List<Category> ORDER = new ArrayList<>(List.of(Category.values()));
    private static final Set<Category> FAV = EnumSet.noneOf(Category.class);
    private static final Set<Category> DEL = EnumSet.noneOf(Category.class);
    private static final Set<Category> CLOSED = EnumSet.noneOf(Category.class);
    private static int filter;                        // 0 — все, 1 — избранное, 2 — удалённые

    private final Anim open = new Anim(0);
    private final Anim scroll = new Anim(0);
    private final Anim cfgAnim = new Anim(0);
    private final Map<Category, Anim> catOpen = new EnumMap<>(Category.class);
    private final Map<Category, float[]> box = new EnumMap<>(Category.class);
    private final Map<Category, List<Module>> byCat = new EnumMap<>(Category.class);
    private final List<Module> visible = new ArrayList<>();
    private final List<Module> results = new ArrayList<>();
    private final List<Chip> chips = new ArrayList<>();

    private static final class Chip {
        ModeSetting mo; int idx, x, y, w, h;
    }

    private final StringBuilder query = new StringBuilder();
    private boolean closing, searching;
    private float scrollTarget, maxScroll, contentH, frameDt = 0.016f;
    private long last = System.nanoTime();

    private int contentTop, contentBottom, listH, panelW, cols;
    private int searchX, searchY, searchW, searchH;
    private int swatchY, swatchW;
    private int cfgX, cfgY, cfgW, cfgH;
    private final int[] tabX = new int[3];
    private int tabY, tabW, tabH;

    private int mouseXNow, mouseYNow;
    private double pressX, pressY;
    private float moved;
    private boolean pressBg;
    private NumberSetting dragSlider;
    /** Настройка-клавиша, ожидающая нажатия. */
    private KeybindSetting listening;

    // ------------------------------------------------------------------ конфиги
    private boolean cfgOpen, cfgTyping;
    private final StringBuilder cfgName = new StringBuilder();
    private List<String> cfgList = new ArrayList<>();
    private float cfgScroll;
    private int cpx, cpy, cpw, cph;
    private int fldX, fldY, fldW, fldH;
    private int saveX, saveY, saveW, saveH;
    private int clearX, clearY, clearW, clearH;
    private int closeX, closeY, closeW, closeH;
    private int lstX, lstY, lstW, lstH;

    public ClickGuiScreen() {
        super(Text.empty());
    }

    @Override public boolean shouldPause() { return false; }
    @Override public void close() { closing = true; }
    @Override public void renderBackground(DrawContext ctx, int mx, int my, float delta) {}

    // ------------------------------------------------------------------ клавиатура
    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (listening != null) {
            listening.set(keyCode == GLFW.GLFW_KEY_ESCAPE ? 0 : keyCode);
            listening.listening = false;
            listening = null;
            return true;
        }
        if (cfgOpen) {
            if (cfgTyping) {
                if (keyCode == GLFW.GLFW_KEY_ESCAPE) { cfgTyping = false; return true; }
                if (keyCode == GLFW.GLFW_KEY_ENTER) { doSave(); return true; }
                if (keyCode == GLFW.GLFW_KEY_BACKSPACE) {
                    if (cfgName.length() > 0) cfgName.setLength(cfgName.length() - 1);
                    return true;
                }
            }
            if (keyCode == GLFW.GLFW_KEY_ESCAPE) { cfgOpen = false; return true; }
            return true;
        }
        if (searching) {
            if (keyCode == GLFW.GLFW_KEY_ESCAPE) { stopSearch(); return true; }
            if (keyCode == GLFW.GLFW_KEY_BACKSPACE) {
                if (query.length() > 0) query.setLength(query.length() - 1);
                return true;
            }
        }
        int mk = ModuleManager.ui == null ? GLFW.GLFW_KEY_RIGHT_SHIFT : ModuleManager.ui.menuKey.get();
        if (mk > 0 && keyCode == mk && !searching) { close(); return true; }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public boolean charTyped(char chr, int modifiers) {
        if (listening != null) return true;
        if (cfgOpen) {
            if (cfgTyping && chr >= 32 && cfgName.length() < 24) {
                cfgName.append(chr);
                return true;
            }
            return true;
        }
        if (searching && chr >= 32 && query.length() < 24) {
            query.append(chr);
            scrollTarget = 0f;
            return true;
        }
        return super.charTyped(chr, modifiers);
    }

    private void stopSearch() { searching = false; query.setLength(0); }

    private static boolean isClosed(Category c) { return CLOSED.contains(c); }

    private static void toggleClosed(Category c) {
        if (!CLOSED.remove(c)) CLOSED.add(c);
    }

    private static boolean matches(Category c) {
        if (filter == 2) return DEL.contains(c);
        if (DEL.contains(c)) return false;            // в «Все» и «Избранном» удалённых нет
        if (filter == 1) return FAV.contains(c);
        return true;
    }

    // ------------------------------------------------------------------ рендер
    @Override
    public void render(DrawContext ctx, int mouseX, int mouseY, float delta) {
        long now = System.nanoTime();
        float dt = Math.min(0.1f, (now - last) / 1e9f);
        last = now;
        frameDt = dt;
        float aspd = ModuleManager.ui == null ? 1f : (float) ModuleManager.ui.animSpeed.get();

        open.to(closing ? 0f : 1f, 14f * aspd, dt);
        if (closing && open.v < 0.03f) { client.setScreen(null); return; }
        float a = Draw.ease(open.v);
        cfgAnim.to(cfgOpen ? 1f : 0f, 16f * aspd, dt);

        TextRenderer tr = client.textRenderer;
        int acc = Theme.accent();
        var ui = ModuleManager.ui;
        if (ui != null && ui.blur.get()) applyBlur();

        drawBackdrop(ctx, a);

        mouseXNow = mouseX;
        mouseYNow = mouseY;

        for (Category c : ORDER) byCat.put(c, ModuleManager.byCategory(c));

        contentTop = TOPBAR + 2;
        contentBottom = height - BOTBAR - 2;
        listH = Math.max(40, contentBottom - contentTop);

        drawTopBar(ctx, tr, a, acc);

        chips.clear();
        visible.clear();
        if (searching && query.length() > 0) {
            layoutResults(tr);
            drawResults(ctx, tr, mouseX, mouseY, a, acc, dt);
        } else {
            layoutPanels(tr, aspd);
            drawPanels(ctx, tr, mouseX, mouseY, a, acc, dt, aspd);
        }
        drawBottomBar(ctx, tr, a, acc);

        if (cfgAnim.v > 0.02f) {
            layoutCfg();
            drawCfg(ctx, tr, mouseX, mouseY, a, acc);
        }
    }

    // ------------------------------------------------------------------ фон
    private void drawBackdrop(DrawContext ctx, float a) {
        ctx.fill(0, 0, width, height, Draw.argb(0x000000, 0.82f * a));
    }

    // ------------------------------------------------------------------ верх
    private void drawTopBar(DrawContext ctx, TextRenderer tr, float a, int acc) {
        // фильтры-вкладки слева
        String[] names = {"ВСЕ", "ИЗБР", "УДАЛ"};
        tabW = 34;
        tabH = 14;
        tabY = (TOPBAR - tabH) / 2;
        for (int i = 0; i < 3; i++) {
            tabX[i] = PAD + i * (tabW + 3);
            boolean sel = filter == i;
            int rgb = i == 2 ? Theme.DELETED : i == 1 ? Theme.STAR : acc;
            Draw.rrect(ctx, tabX[i], tabY, tabW, tabH, 5,
                    Draw.argb(Draw.mix(Theme.PANEL, rgb, sel ? 0.30f : 0.06f), (sel ? 0.96f : 0.80f) * a));
            Draw.outline(ctx, tabX[i], tabY, tabW, tabH, 5,
                    Draw.argb(Draw.mix(Theme.BORDER, rgb, sel ? 1f : 0.20f), a));
            Draw.text(ctx, tr, names[i], tabX[i] + (tabW - Draw.w(tr, names[i])) / 2, tabY + 3,
                    sel ? rgb : Theme.TEXT_DIM, a);
        }

        // кнопка CFG справа
        cfgW = 32;
        cfgH = 14;
        cfgX = width - PAD - cfgW;
        cfgY = (TOPBAR - cfgH) / 2;
        boolean hov = in(mouseXNow, mouseYNow, cfgX, cfgY, cfgW, cfgH);
        Draw.rrect(ctx, cfgX, cfgY, cfgW, cfgH, 5,
                Draw.argb(Draw.mix(Theme.PANEL, acc, cfgOpen ? 0.55f : hov ? 0.30f : 0.10f), 0.94f * a));
        Draw.outline(ctx, cfgX, cfgY, cfgW, cfgH, 5, Draw.argb(Draw.mix(Theme.BORDER, acc, cfgOpen ? 1f : 0.30f), a));
        Draw.text(ctx, tr, "CFG", cfgX + (cfgW - Draw.w(tr, "CFG")) / 2, cfgY + 3,
                cfgOpen ? 0x0A1626 : Theme.TEXT, a);

        // логотип по центру
        int ih = 13, iw = Logo.widthFor(ih);
        int ww = Logo.wordWidthFor(11);
        boolean bitmap = ww > 0;
        if (!bitmap) ww = Draw.gw(tr, "SHTORA");
        int total = iw + 5 + ww;
        int lx = (width - total) / 2;
        int leftEdge = tabX[2] + tabW + 6;
        int rightEdge = cfgX - 6;
        if (lx > leftEdge && lx + total < rightEdge) {
            Logo.body(ctx, lx, (TOPBAR - ih) / 2, iw, ih, acc, a);
            if (bitmap) Logo.word(ctx, lx + iw + 5, (TOPBAR - 11) / 2, ww, 11, Theme.TEXT, a);
            else Draw.gothic(ctx, tr, "SHTORA", lx + iw + 5, (TOPBAR - 8) / 2, Theme.TEXT, a);
        }
    }

    // ------------------------------------------------------------------ низ
    private void drawBottomBar(DrawContext ctx, TextRenderer tr, float a, int acc) {
        swatchW = 11;
        int gap = 3;
        int swTotal = SWATCH.length * swatchW + (SWATCH.length - 1) * gap;
        int swX0 = width - PAD - swTotal;
        swatchY = height - BOTBAR + (BOTBAR - swatchW) / 2;

        searchH = 14;
        searchY = height - BOTBAR + (BOTBAR - searchH) / 2;
        searchX = PAD;
        searchW = Math.max(40, swX0 - 8 - searchX);

        // поле поиска
        Draw.rrect(ctx, searchX, searchY, searchW, searchH, 5,
                Draw.argb(Draw.mix(Theme.PANEL, acc, searching ? 0.18f : 0f), 0.94f * a));
        Draw.outline(ctx, searchX, searchY, searchW, searchH, 5,
                Draw.argb(Draw.mix(Theme.BORDER, acc, searching ? 1f : 0.22f), a));
        int icx = searchX + 6, icy = searchY + searchH / 2;
        Draw.outline(ctx, icx - 3, icy - 4, 6, 6, 3, Draw.argb(searching ? acc : Theme.TEXT_DIM, a));
        ctx.fill(icx + 2, icy + 2, icx + 5, icy + 3, Draw.argb(searching ? acc : Theme.TEXT_DIM, a));
        String shown = searching ? Draw.trim(tr, query.toString(), searchW - 22) : "Search...";
        Draw.text(ctx, tr, shown, searchX + 14, searchY + 3, searching ? Theme.TEXT : Theme.TEXT_DIM, a);
        if (searching && (System.currentTimeMillis() / 500) % 2 == 0) {
            int cx = searchX + 14 + Draw.w(tr, shown);
            ctx.fill(cx + 1, searchY + 3, cx + 2, searchY + searchH - 3, Draw.argb(acc, a));
        }

        // квадратики акцентного цвета
        int cur = nearestSwatch();
        for (int i = 0; i < SWATCH.length; i++) {
            int x = swX0 + i * (swatchW + gap);
            boolean sel = i == cur;
            Draw.rrect(ctx, x, swatchY, swatchW, swatchW, 3, Draw.argb(SWATCH[i], (sel ? 1f : 0.85f) * a));
            Draw.outline(ctx, x, swatchY, swatchW, swatchW, 3,
                    Draw.argb(sel ? 0xFFFFFF : Theme.BORDER, a));
        }
    }

    private int nearestSwatch() {
        float h = ModuleManager.ui == null ? Theme.DEFAULT_HUE * 360f : (float) ModuleManager.ui.hue.get();
        int best = 0;
        float bd = 999f;
        for (int i = 0; i < SWATCH_HUE.length; i++) {
            float d = Math.abs(((h - SWATCH_HUE[i]) % 360f + 540f) % 360f - 180f);
            if (d < bd) { bd = d; best = i; }
        }
        return best;
    }

    private void setAccent(int i) {
        if (ModuleManager.ui != null) ModuleManager.ui.hue.set(SWATCH_HUE[i]);
    }

    // ------------------------------------------------------------------ раскладка панелей
    private int panelHeight(TextRenderer tr, Category c) {
        int h = HEAD + 3;
        if (!isClosed(c)) {
            List<Module> ms = byCat.getOrDefault(c, ModuleManager.byCategory(c));
            for (Module m : ms) {
                h += ROW;
                if (m.expanded && !m.settings.isEmpty()) h += settingsHeight(tr, m, panelW - 16);
            }
        }
        return h + 3;
    }

    private void layoutPanels(TextRenderer tr, float aspd) {
        box.clear();
        cols = width < 360 ? 2 : width < 520 ? 3 : width < 700 ? 4 : 5;
        panelW = (width - 2 * PAD - (cols - 1) * GAP) / cols;

        float[] colH = new float[cols];
        for (Category c : ORDER) {
            if (!matches(c)) continue;
            int full = panelHeight(tr, c);
            Anim oa = catOpen.computeIfAbsent(c, k -> new Anim(isClosed(c) ? 0f : 1f));
            oa.to(isClosed(c) ? 0f : 1f, 12f * aspd, frameDt);
            int min = HEAD + 6;
            int h = Math.round(min + (full - min) * Draw.smooth(oa.v));
            int col = 0;
            for (int i = 1; i < cols; i++) if (colH[i] < colH[col] - 0.5f) col = i;
            float x = PAD + col * (panelW + GAP);
            float y = contentTop + colH[col] - scroll.v;
            box.put(c, new float[]{x, y, panelW, h});
            colH[col] += h + GAP;
        }
        float max = 0f;
        for (float v : colH) max = Math.max(max, v);
        contentH = max;
        maxScroll = Math.max(0f, contentH - listH);
        scrollTarget = Math.max(0f, Math.min(maxScroll, scrollTarget));
        scroll.to(scrollTarget, 22f, frameDt);
    }

    private void drawPanels(DrawContext ctx, TextRenderer tr, int mx, int my, float a, int acc, float dt, float aspd) {
        ctx.enableScissor(0, contentTop, width, contentBottom);
        for (Category c : ORDER) {
            if (!matches(c)) continue;
            float[] r = box.get(c);
            if (r == null) continue;
            if (r[1] + r[3] < contentTop - 4 || r[1] > contentBottom + 4) continue;
            drawPanel(ctx, tr, c, Math.round(r[0]), Math.round(r[1]), Math.round(r[2]), Math.round(r[3]),
                    mx, my, a, acc, dt, aspd);
        }
        ctx.disableScissor();
        if (maxScroll > 0.5f) {
            int sbh = Math.max(16, Math.round(listH * listH / Math.max(1f, contentH)));
            int sby = contentTop + Math.round((listH - sbh) * (scroll.v / maxScroll));
            Draw.rrect(ctx, width - 3, sby, 2, sbh, 1, Draw.argb(acc, 0.7f * a));
        }
    }

    // ------------------------------------------------------------------ панель
    private void drawPanel(DrawContext ctx, TextRenderer tr, Category c, int x, int y, int w, int h,
                           int mx, int my, float a, int acc, float dt, float aspd) {
        boolean deleted = DEL.contains(c);
        boolean fav = FAV.contains(c);
        boolean closed = isClosed(c);
        boolean server = c == Category.CLIENT;             // «правая» панель — янтарная

        int edge = deleted ? Theme.DELETED : fav ? Draw.mix(Theme.BORDER, Theme.STAR, 0.5f)
                : server ? Theme.AMBER : Theme.BORDER;
        int headBg = Draw.mix(Theme.PANEL_HEAD, edge, deleted ? 0.24f : fav ? 0.10f : server ? 0.14f : 0.06f);

        if (server) Draw.glow(ctx, x, y, w, h, RAD, Theme.AMBER, 0.55f * a);

        Draw.rrect(ctx, x, y, w, h, RAD, Draw.argb(Theme.PANEL, 0.96f * a));
        // шапка (низ делаем ровным, чтобы не было зазора)
        Draw.rrect(ctx, x, y, w, HEAD, RAD, Draw.argb(headBg, 0.98f * a));
        ctx.fill(x, y + HEAD - RAD, x + w, y + HEAD, Draw.argb(headBg, 0.98f * a));
        ctx.fill(x + 1, y + HEAD - 1, x + w - 1, y + HEAD, Draw.argb(edge, 0.35f * a));
        Draw.outline(ctx, x, y, w, h, RAD, Draw.argb(edge, (server ? 0.95f : 0.85f) * a));

        // заголовок + акцентная полоска
        Draw.rrect(ctx, x + 7, y + (HEAD - 9) / 2, 2, 9, 1,
                Draw.argb(deleted ? Theme.DELETED : server ? Theme.AMBER : acc, a));
        String title = Draw.trim(tr, c.title, w - 46);
        Draw.text(ctx, tr, title, x + 13, y + 5, deleted ? Theme.DELETED : Theme.TEXT, a);
        List<Module> ms = byCat.getOrDefault(c, ModuleManager.byCategory(c));
        int on = 0;
        for (Module m : ms) if (m.isEnabled()) on++;
        Draw.text(ctx, tr, on + "/" + ms.size(), x + 15 + Draw.w(tr, title), y + 5, Theme.TEXT_DIM, a * 0.85f);

        // кнопки ★ и 🗑
        int cy = y + HEAD / 2;
        int trashCx = x + w - 9;
        int starCx = x + w - 22;
        boolean starHov = mx >= starCx - 5 && mx < starCx + 5 && my >= y && my < y + HEAD;
        boolean trashHov = mx >= trashCx - 5 && mx < trashCx + 5 && my >= y && my < y + HEAD;
        if (starHov) Draw.rrect(ctx, starCx - 5, y + 2, 10, HEAD - 4, 3, Draw.argb(0xFFFFFF, 0.07f * a));
        if (trashHov) Draw.rrect(ctx, trashCx - 5, y + 2, 10, HEAD - 4, 3, Draw.argb(0xFFFFFF, 0.07f * a));
        iconStar(ctx, starCx, cy, 4, Draw.argb(fav ? Theme.STAR : Theme.TEXT_DIM, a));
        if (deleted) iconRestore(ctx, trashCx, cy, 4, Draw.argb(Theme.RESTORE, a));
        else iconTrash(ctx, trashCx, cy, 4, Draw.argb(Theme.TRASH, a));

        Anim ca = catOpen.get(c);
        float openV = ca == null ? (closed ? 0f : 1f) : Draw.smooth(ca.v);
        if (openV <= 0.02f) return;

        ctx.enableScissor(x, y + HEAD, x + w, y + h);
        int innerX = x + 4, innerW = w - 8;
        int ry = y + HEAD + 2;
        int k = 0;
        for (Module m : ms) {
            float appear = Draw.clamp((openV - Math.min(k, 12) * 0.03f) / 0.35f);
            int rx = innerX + Math.round((1f - appear) * 5f);
            m.uiX = innerX;
            m.uiY = ry;
            m.uiW = innerW;
            m.uiH = ROW;
            m.toggleAnim.to(m.isEnabled() ? 1f : 0f, 14f * aspd, dt);
            m.expandAnim.to(m.expanded && !m.settings.isEmpty() ? 1f : 0f, 14f * aspd, dt);

            boolean hov = mx >= innerX && mx < innerX + innerW && my >= ry && my < ry + ROW
                    && my >= contentTop && my < contentBottom;
            m.hoverAnim.to(hov ? 1f : 0f, 16f * aspd, dt);
            visible.add(m);

            if (appear > 0.02f && ry + ROW >= contentTop && ry <= contentBottom)
                drawRow(ctx, tr, m, rx, ry, innerW, a * openV, acc, appear);
            ry += ROW;

            if (m.expanded && !m.settings.isEmpty()) {
                int sh = settingsHeight(tr, m, innerW - 8);
                if (appear > 0.02f && ry + sh >= contentTop && ry <= contentBottom)
                    drawSettings(ctx, tr, m, innerX, ry, innerW, a * openV, acc, dt, aspd);
                ry += sh;
            }
            k++;
        }
        ctx.disableScissor();
        ctx.enableScissor(0, contentTop, width, contentBottom);
    }

    // ------------------------------------------------------------------ строка модуля
    private void drawRow(DrawContext ctx, TextRenderer tr, Module m, int x, int y, int w, float a, int acc, float appear) {
        a *= appear;
        float tg = m.toggleAnim.v;
        int bg = Draw.mix(Theme.CARD, Theme.CARD_HOVER, m.hoverAnim.v);
        bg = Draw.mix(bg, acc, 0.10f * tg);
        Draw.rrect(ctx, x, y, w, ROW - 2, 5, Draw.argb(bg, (0.80f + 0.20f * m.hoverAnim.v) * a));

        int tx = x + 7;
        int tc = Draw.mix(Theme.TEXT_DIM, Theme.TEXT, Math.max(tg, m.hoverAnim.v));
        Draw.text(ctx, tr, Draw.trim(tr, m.name, w - 42), tx, y + 5, tc, a);

        // переключатель-пилюля
        int tw = 16, th = 10;
        int sx = x + w - tw - 6, sy = y + (ROW - 2 - th) / 2;
        Draw.rrect(ctx, sx, sy, tw, th, th / 2, Draw.argb(Draw.mix(Theme.TOGGLE_OFF, acc, tg), a));
        Draw.outline(ctx, sx, sy, tw, th, th / 2, Draw.argb(Draw.mix(Theme.BORDER, acc, tg), a));
        int kr = 7;
        int kx = sx + 1 + Math.round(tg * (tw - kr - 2));
        int ky = sy + (th - kr) / 2;
        Draw.rrect(ctx, kx, ky, kr, kr, kr / 2, Draw.argb(Draw.mix(Theme.KNOB_OFF, Theme.knobOn(), tg), a));

        // «+» — есть настройки (слева от переключателя)
        if (!m.settings.isEmpty()) {
            int cx = x + w - 30, cyy = y + (ROW - 2) / 2;
            int cc = Draw.argb(Draw.mix(Theme.TEXT_DIM, acc, m.expandAnim.v), a);
            Draw.line(ctx, cx - 2, cyy, cx + 2, cyy, cc);
            if (m.expandAnim.v < 0.5f) Draw.line(ctx, cx, cyy - 2, cx, cyy + 2, cc);
        }
    }

    // ------------------------------------------------------------------ настройки
    private static int modeRows(TextRenderer tr, ModeSetting mo, int innerW) {
        int rows = 1, cx = 0;
        for (String m : mo.modes) {
            int w = Draw.w(tr, m) + 10;
            if (cx + w > innerW && cx > 0) { rows++; cx = 0; }
            cx += w + 3;
        }
        return rows;
    }

    private static int settingH(TextRenderer tr, Setting s, int w) {
        if (s instanceof NumberSetting) return 23;
        if (s instanceof KeybindSetting) return 20;
        if (s instanceof ModeSetting mo) return 13 + modeRows(tr, mo, w - 8) * 13 + 1;
        return 15;
    }

    private static int settingsHeight(TextRenderer tr, Module m, int w) {
        if (m.settings.isEmpty()) return 0;
        int h = 3;
        for (Setting s : m.settings) h += settingH(tr, s, w);
        return h;
    }

    private void drawSettings(DrawContext ctx, TextRenderer tr, Module m, int x, int y, int w,
                              float a, int acc, float dt, float aspd) {
        int innerX = x + 4, innerW = w - 8;
        float ea = a * m.expandAnim.v;
        int ry = y + 2;
        for (Setting s : m.settings) {
            int sh = settingH(tr, s, innerW);
            s.ux = innerX;
            s.uy = ry;
            s.uw = innerW;
            s.uh = sh;
            if (s instanceof BoolSetting b) {
                b.anim.to(b.get() ? 1f : 0f, 16f * aspd, dt);
                Draw.text(ctx, tr, Draw.trim(tr, b.name, innerW - 30), innerX + 4, ry + 4, Theme.TEXT, ea);
                pill(ctx, innerX + innerW - 22, ry + 2, 16, 10, b.anim.v, acc, ea);
            } else if (s instanceof NumberSetting n) {
                n.anim.to(n.frac(), 22f * aspd, dt);
                String vs = n.step >= 1 ? String.valueOf((int) Math.round(n.get()))
                        : String.format(Locale.ROOT, "%.2f", n.get());
                Draw.text(ctx, tr, Draw.trim(tr, n.name, innerW - 40), innerX + 4, ry + 2, Theme.TEXT, ea);
                Draw.text(ctx, tr, vs, innerX + innerW - 4 - Draw.w(tr, vs), ry + 2, acc, ea);
                int sx = innerX + 4, sw = innerW - 8, sy = ry + 15;
                Draw.rrect(ctx, sx, sy, sw, 3, 1, Draw.argb(0x000000, 0.45f * ea));
                int fw = Math.max(1, Math.round(n.anim.v * sw));
                Draw.rrect(ctx, sx, sy, fw, 3, 1, Draw.argb(acc, ea));
                Draw.rrect(ctx, sx + fw - 3, sy - 2, 6, 6, 3, Draw.argb(acc, ea));
            } else if (s instanceof KeybindSetting ks) {
                drawKeybind(ctx, tr, ks, innerX, ry, innerW, ea, acc, aspd, dt);
            } else if (s instanceof ModeSetting mo) {
                Draw.text(ctx, tr, Draw.trim(tr, mo.name, innerW - 8), innerX + 4, ry + 2, Theme.TEXT, ea);
                int cxp = innerX + 4, cyp = ry + 13;
                for (int i = 0; i < mo.modes.length; i++) {
                    int tw = Draw.w(tr, mo.modes[i]) + 10;
                    if (cxp + tw > innerX + innerW - 4 && cxp > innerX + 4) { cxp = innerX + 4; cyp += 13; }
                    boolean sel = i == mo.index();
                    Draw.rrect(ctx, cxp, cyp, tw, 12, 3, Draw.argb(sel ? acc : Theme.PANEL_ALT, (sel ? 0.92f : 0.85f) * ea));
                    Draw.outline(ctx, cxp, cyp, tw, 12, 3, Draw.argb(sel ? acc : Theme.BORDER, ea));
                    Draw.text(ctx, tr, mo.modes[i], cxp + 5, cyp + 2, sel ? 0x0A1626 : Theme.TEXT_DIM, ea);
                    Chip ch = new Chip();
                    ch.mo = mo; ch.idx = i; ch.x = cxp; ch.y = cyp; ch.w = tw; ch.h = 12;
                    chips.add(ch);
                    cxp += tw + 3;
                }
            }
            ry += sh;
        }
    }

    // ------------------------------------------------------------------ клавиша
    private void drawKeybind(DrawContext ctx, TextRenderer tr, KeybindSetting ks, int x, int y, int w,
                             float ea, int acc, float aspd, float dt) {
        ks.anim.to(ks.bound() ? 1f : 0f, 18f * aspd, dt);
        Draw.text(ctx, tr, Draw.trim(tr, ks.name, w - 76), x + 4, y + 4, Theme.TEXT, ea);
        int bw = Math.min(w - 8, 74), bx = x + w - bw - 4, by = y + 2, bh = 14;
        boolean listen = ks.listening;
        float pulse = 0.5f + 0.5f * (float) Math.sin(System.currentTimeMillis() / 130.0);
        Draw.rrect(ctx, bx, by, bw, bh, 4,
                Draw.argb(Draw.mix(Theme.PANEL_ALT, acc, listen ? 0.45f : 0.12f), ea));
        Draw.outline(ctx, bx, by, bw, bh, 4,
                Draw.argb(listen ? Draw.mix(acc, 0xFFFFFF, pulse * 0.6f) : Draw.mix(Theme.BORDER, acc, ks.anim.v), ea));
        String lbl = listen ? "PRESS KEY" : ks.display();
        lbl = Draw.trim(tr, lbl, bw - 8);
        Draw.text(ctx, tr, lbl, bx + (bw - Draw.w(tr, lbl)) / 2, by + 3, listen ? acc : Theme.TEXT, ea);
    }

    /** Переключатель-пилюля. */
    private static void pill(DrawContext ctx, int x, int y, int w, int h, float on, int acc, float a) {
        Draw.rrect(ctx, x, y, w, h, h / 2, Draw.argb(Draw.mix(Theme.TOGGLE_OFF, acc, on), a));
        Draw.outline(ctx, x, y, w, h, h / 2, Draw.argb(Draw.mix(Theme.BORDER, acc, on), a));
        int kr = h - 3;
        int kx = x + 1 + Math.round(on * (w - kr - 2));
        Draw.rrect(ctx, kx, y + 1, kr, kr, kr / 2, Draw.argb(Draw.mix(Theme.KNOB_OFF, Theme.knobOn(), on), a));
    }

    // ------------------------------------------------------------------ иконки
    private static void iconStar(DrawContext ctx, int cx, int cy, int r, int color) {
        for (int i = 0; i <= r; i++) {
            int t = Math.max(1, (r - i) / 2 + 1);
            ctx.fill(cx + i, cy - t, cx + i + 1, cy + t + 1, color);
            ctx.fill(cx - i, cy - t, cx - i + 1, cy + t + 1, color);
            ctx.fill(cx - t, cy + i, cx + t + 1, cy + i + 1, color);
            ctx.fill(cx - t, cy - i, cx + t + 1, cy - i + 1, color);
        }
    }

    private static void iconTrash(DrawContext ctx, int cx, int cy, int r, int color) {
        ctx.fill(cx - r, cy - r + 1, cx + r + 1, cy - r + 3, color);
        ctx.fill(cx - 1, cy - r - 1, cx + 2, cy - r + 1, color);
        ctx.fill(cx - r + 1, cy - r + 3, cx - r + 3, cy + r, color);
        ctx.fill(cx + r - 1, cy - r + 3, cx + r + 1, cy + r, color);
        ctx.fill(cx - r + 1, cy + r - 1, cx + r + 1, cy + r + 1, color);
    }

    private static void iconRestore(DrawContext ctx, int cx, int cy, int r, int color) {
        Draw.line(ctx, cx - r, cy, cx + r, cy, color);
        Draw.line(ctx, cx - r, cy, cx - r + r / 2, cy - r / 2, color);
        Draw.line(ctx, cx - r, cy, cx - r + r / 2, cy + r / 2, color);
    }

    // ------------------------------------------------------------------ поиск
    private void layoutResults(TextRenderer tr) {
        results.clear();
        String q = query.toString().toLowerCase(Locale.ROOT);
        for (Module m : ModuleManager.modules)
            if (m.name.toLowerCase(Locale.ROOT).contains(q)) results.add(m);

        int y = contentTop;
        for (Module m : results) {
            m.uiX = PAD;
            m.uiY = y;
            m.uiW = width - 2 * PAD;
            m.uiH = ROW;
            y += ROW;
            if (m.expanded && !m.settings.isEmpty()) y += settingsHeight(tr, m, width - 2 * PAD - 16);
            visible.add(m);
        }
        contentH = y - contentTop;
        maxScroll = Math.max(0f, contentH - listH);
        scrollTarget = Math.max(0f, Math.min(maxScroll, scrollTarget));
        scroll.to(scrollTarget, 22f, frameDt);
    }

    private void drawResults(DrawContext ctx, TextRenderer tr, int mouseX, int mouseY,
                             float a, int acc, float dt) {
        ctx.enableScissor(0, contentTop, width, contentBottom);
        if (results.isEmpty()) {
            String s = "Nothing found";
            Draw.text(ctx, tr, s, (width - Draw.w(tr, s)) / 2, contentTop + listH / 2 - 4, Theme.TEXT_DIM, a);
        }
        for (Module m : results) {
            int y = Math.round(m.uiY - scroll.v);
            m.uiY = y;
            m.toggleAnim.to(m.isEnabled() ? 1f : 0f, 14f, dt);
            m.expandAnim.to(m.expanded && !m.settings.isEmpty() ? 1f : 0f, 14f, dt);
            boolean hov = mouseX >= m.uiX && mouseX < m.uiX + m.uiW && mouseY >= y && mouseY < y + ROW
                    && mouseY >= contentTop && mouseY < contentBottom;
            m.hoverAnim.to(hov ? 1f : 0f, 16f, dt);
            if (y + ROW >= contentTop && y <= contentBottom)
                drawRow(ctx, tr, m, m.uiX, y, m.uiW, a, acc, 1f);
            int sy = y + ROW;
            if (m.expanded && !m.settings.isEmpty()) {
                int sh = settingsHeight(tr, m, m.uiW - 16);
                if (sy + sh >= contentTop && sy <= contentBottom)
                    drawSettings(ctx, tr, m, m.uiX + 4, sy, m.uiW - 8, a, acc, dt, 1f);
            }
        }
        ctx.disableScissor();
        if (maxScroll > 0.5f) {
            int sbh = Math.max(16, Math.round(listH * listH / Math.max(1f, contentH)));
            int sby = contentTop + Math.round((listH - sbh) * (scroll.v / maxScroll));
            Draw.rrect(ctx, width - 3, sby, 2, sbh, 1, Draw.argb(acc, 0.7f * a));
        }
    }

    // ------------------------------------------------------------------ конфиги
    private void openCfg() {
        cfgOpen = true;
        cfgTyping = false;
        cfgList = ConfigManager.list();
        cfgScroll = 0f;
    }

    private void doSave() {
        String n = ConfigManager.safe(cfgName.toString());
        if (!n.isEmpty()) {
            ConfigManager.save(n);
            cfgList = ConfigManager.list();
            cfgTyping = false;
        }
    }

    private void layoutCfg() {
        cpw = Math.min(250, width - 20);
        cph = Math.min(250, height - 40);
        cpx = (width - cpw) / 2;
        cpy = (height - cph) / 2;

        fldX = cpx + 8;
        fldY = cpy + 24;
        fldW = cpw - 16;
        fldH = 15;

        int bw = (cpw - 24) / 3;
        saveW = bw;
        saveX = cpx + 8;
        saveY = fldY + fldH + 5;
        saveH = 15;
        clearW = bw;
        clearX = saveX + bw + 4;
        clearY = saveY;
        clearH = 15;
        closeW = cpw - 16 - 2 * (bw + 4);
        closeX = clearX + bw + 4;
        closeY = saveY;
        closeH = 15;

        lstX = cpx + 8;
        lstY = saveY + saveH + 6;
        lstW = cpw - 16;
        lstH = Math.max(16, cpy + cph - 8 - lstY);
    }

    private void drawCfg(DrawContext ctx, TextRenderer tr, int mx, int my, float a, int acc) {
        a *= Draw.ease(cfgAnim.v);
        ctx.fill(0, 0, width, height, Draw.argb(0x000000, 0.45f * a));
        Draw.rrect(ctx, cpx, cpy, cpw, cph, RAD, Draw.argb(Theme.PANEL, 0.98f * a));
        Draw.rrect(ctx, cpx, cpy, cpw, 18, RAD, Draw.argb(Theme.PANEL_HEAD, a));
        ctx.fill(cpx, cpy + 18 - RAD, cpx + cpw, cpy + 18, Draw.argb(Theme.PANEL_HEAD, a));
        Draw.outline(ctx, cpx, cpy, cpw, cph, RAD, Draw.argb(acc, 0.85f * a));
        Draw.text(ctx, tr, "CONFIGS", cpx + 8, cpy + 5, Theme.TEXT, a);
        Draw.text(ctx, tr, "×", cpx + cpw - 14, cpy + 4, Theme.TEXT_DIM, a);

        Draw.rrect(ctx, fldX, fldY, fldW, fldH, 4,
                Draw.argb(Draw.mix(Theme.PANEL_ALT, acc, cfgTyping ? 0.25f : 0f), 0.95f * a));
        Draw.outline(ctx, fldX, fldY, fldW, fldH, 4, Draw.argb(cfgTyping ? acc : Theme.BORDER, a));
        String nm = cfgName.length() == 0 ? "config name" : cfgName.toString();
        Draw.text(ctx, tr, nm, fldX + 5, fldY + 4, cfgName.length() == 0 ? Theme.TEXT_DIM : Theme.TEXT, a);
        if (cfgTyping && (System.currentTimeMillis() / 500) % 2 == 0) {
            int cx = fldX + 5 + Draw.w(tr, cfgName.length() == 0 ? "" : cfgName.toString());
            ctx.fill(cx + 1, fldY + 3, cx + 2, fldY + fldH - 3, Draw.argb(acc, a));
        }

        cfgButton(ctx, tr, saveX, saveY, saveW, saveH, "SAVE", acc, a, true);
        cfgButton(ctx, tr, clearX, clearY, clearW, clearH, "CLEAR", acc, a, false);
        cfgButton(ctx, tr, closeX, closeY, closeW, closeH, "CLOSE", acc, a, false);

        ctx.enableScissor(lstX, lstY, lstX + lstW, lstY + lstH);
        if (cfgList.isEmpty()) {
            Draw.text(ctx, tr, "no configs yet", lstX + 4, lstY + 4, Theme.TEXT_DIM, a * 0.9f);
        }
        int ry = lstY - Math.round(cfgScroll);
        for (String name : cfgList) {
            if (ry + 14 >= lstY && ry <= lstY + lstH) {
                boolean hov = mx >= lstX && mx < lstX + lstW && my >= ry && my < ry + 14
                        && my >= lstY && my < lstY + lstH;
                Draw.rrect(ctx, lstX, ry, lstW, 13, 4, Draw.argb(hov ? Theme.PANEL_ALT : Theme.CARD, 0.9f * a));
                Draw.text(ctx, tr, Draw.trim(tr, name, lstW - 68), lstX + 5, ry + 3, Theme.TEXT, a);
                Draw.text(ctx, tr, "LOAD", lstX + lstW - 56, ry + 3, acc, a);
                Draw.text(ctx, tr, "DEL", lstX + lstW - 24, ry + 3, Theme.TRASH, a);
            }
            ry += 15;
        }
        ctx.disableScissor();
        if (cfgList.size() * 15 > lstH) {
            Draw.rrect(ctx, lstX + lstW - 2, lstY, 2, lstH, 1, Draw.argb(Theme.BORDER, a));
        }
    }

    private void cfgButton(DrawContext ctx, TextRenderer tr, int x, int y, int w, int h,
                           String label, int acc, float a, boolean primary) {
        Draw.rrect(ctx, x, y, w, h, 4, Draw.argb(Draw.mix(Theme.PANEL_ALT, acc, primary ? 0.7f : 0.15f), 0.95f * a));
        Draw.outline(ctx, x, y, w, h, 4, Draw.argb(primary ? acc : Theme.BORDER, a));
        Draw.text(ctx, tr, label, x + (w - Draw.w(tr, label)) / 2, y + 4, primary ? 0x0A1626 : Theme.TEXT, a);
    }

    // ------------------------------------------------------------------ ввод
    private static boolean in(double mx, double my, int x, int y, int w, int h) {
        return mx >= x && mx < x + w && my >= y && my < y + h;
    }

    private void setSlider(NumberSetting s, double mx) {
        double f = Math.max(0.0, Math.min(1.0, (mx - (s.ux + 4)) / (double) Math.max(1, s.uw - 8)));
        double raw = s.min + f * (s.max - s.min);
        s.set(Math.round(raw / s.step) * s.step);
    }

    /** Переводит в режим ожидания только одну настройку-клавишу. */
    private void setListening(KeybindSetting ks) {
        for (Module m : ModuleManager.modules)
            for (Setting s : m.settings)
                if (s instanceof KeybindSetting k) k.listening = false;
        ks.listening = true;
        listening = ks;
    }

    @Override
    public boolean mouseClicked(double smx, double smy, int button) {
        double mx = smx;
        double my = smy;
        mouseXNow = (int) mx;
        mouseYNow = (int) my;
        if (open.v < 0.9f || closing) return true;
        if (listening != null) { listening.listening = false; listening = null; }

        if (cfgOpen) {
            layoutCfg();
            if (in(mx, my, cpx + cpw - 18, cpy, 18, 18)) { cfgOpen = false; cfgTyping = false; return true; }
            if (in(mx, my, fldX, fldY, fldW, fldH)) { cfgTyping = true; return true; }
            cfgTyping = false;
            if (in(mx, my, saveX, saveY, saveW, saveH)) { doSave(); return true; }
            if (in(mx, my, clearX, clearY, clearW, clearH)) { ConfigManager.disableAll(); return true; }
            if (in(mx, my, closeX, closeY, closeW, closeH)) { cfgOpen = false; return true; }
            if (in(mx, my, lstX, lstY, lstW, lstH)) {
                int idx = (int) ((my - lstY + cfgScroll) / 15);
                if (idx >= 0 && idx < cfgList.size()) {
                    String name = cfgList.get(idx);
                    if (mx >= lstX + lstW - 28) {
                        ConfigManager.delete(name);
                        cfgList = ConfigManager.list();
                    } else {
                        ConfigManager.load(name);
                        cfgName.setLength(0);
                        cfgName.append(name);
                    }
                    return true;
                }
            }
            return true;
        }

        // верхняя полоса: фильтры и CFG
        if (my < TOPBAR) {
            for (int i = 0; i < 3; i++) {
                if (in(mx, my, tabX[i], tabY, tabW, tabH)) {
                    filter = i;
                    scrollTarget = 0f;
                    return true;
                }
            }
            if (in(mx, my, cfgX, cfgY, cfgW, cfgH)) { openCfg(); return true; }
            return true;
        }

        // нижняя полоса: поиск и квадратики цвета
        if (my >= height - BOTBAR) {
            int gap = 3;
            int swTotal = SWATCH.length * swatchW + (SWATCH.length - 1) * gap;
            int swX0 = width - PAD - swTotal;
            for (int i = 0; i < SWATCH.length; i++) {
                if (in(mx, my, swX0 + i * (swatchW + gap), swatchY, swatchW, swatchW)) {
                    setAccent(i);
                    return true;
                }
            }
            if (in(mx, my, searchX, searchY, searchW, searchH)) { searching = true; return true; }
            if (searching) stopSearch();
            return true;
        }

        if (my < contentTop || my > contentBottom) return true;

        // настройки раскрытых модулей
        for (Module m : visible) {
            if (m.expandAnim.v < 0.6f) continue;
            if (m.uiY + ROW < contentTop || m.uiY > contentBottom) continue;
            for (Setting s : m.settings) {
                if (!in(mx, my, s.ux, s.uy, s.uw, s.uh)) continue;
                if (s instanceof NumberSetting n) { dragSlider = n; setSlider(n, mx); return true; }
                if (s instanceof BoolSetting b) { b.toggle(); return true; }
                if (s instanceof KeybindSetting ks) { setListening(ks); return true; }
            }
        }
        for (Chip c : chips) {
            if (c.y + c.h < contentTop || c.y > contentBottom) continue;
            if (in(mx, my, c.x, c.y, c.w, c.h)) { c.mo.set(c.idx); return true; }
        }

        // строки модулей
        for (Module m : visible) {
            if (!in(mx, my, m.uiX, m.uiY, m.uiW, ROW)) continue;
            if (button == 1) {
                if (!m.settings.isEmpty()) m.expanded = !m.expanded;
            } else if (!m.settings.isEmpty()
                    && mx >= m.uiX + m.uiW - 34 && mx < m.uiX + m.uiW - 22) {
                m.expanded = !m.expanded;      // «+» — раскрыть настройки
            } else {
                m.toggle();
            }
            return true;
        }

        // шапки панелей
        if (!(searching && query.length() > 0)) {
            for (Category c : ORDER) {
                if (!matches(c)) continue;
                float[] r = box.get(c);
                if (r == null) continue;
                int x = Math.round(r[0]), y = Math.round(r[1]), w = Math.round(r[2]);
                if (!in(mx, my, x, y, w, HEAD)) continue;
                if (in(mx, my, x + w - 27, y, 12, HEAD)) {
                    if (!FAV.remove(c)) FAV.add(c);
                    return true;
                }
                if (in(mx, my, x + w - 14, y, 14, HEAD)) {
                    if (!DEL.remove(c)) DEL.add(c);
                    return true;
                }
                toggleClosed(c);
                return true;
            }
        }

        pressBg = true;
        pressX = mx;
        pressY = my;
        moved = 0;
        return true;
    }

    @Override
    public boolean mouseDragged(double smx, double smy, int button, double dx, double dy) {
        double mx = smx;
        if (dragSlider != null) { setSlider(dragSlider, mx); return true; }
        if (pressBg) {
            moved += (float) (Math.abs(dx) + Math.abs(dy));
            scrollTarget = Math.max(0f, Math.min(maxScroll, scrollTarget - (float) dy));
        }
        return true;
    }

    @Override
    public boolean mouseReleased(double mx, double my, int button) {
        if (dragSlider != null) { dragSlider = null; return true; }
        if (pressBg && moved < 6) {
            if (searching) stopSearch();
        }
        pressBg = false;
        return true;
    }

    @Override
    public boolean mouseScrolled(double mx, double my, double h, double v) {
        if (cfgOpen) {
            float max = Math.max(0f, cfgList.size() * 15f - lstH);
            cfgScroll = Math.max(0f, Math.min(max, cfgScroll - (float) v * 15f));
            return true;
        }
        scrollTarget = Math.max(0f, Math.min(maxScroll, scrollTarget - (float) v * 22f));
        return true;
    }
}
