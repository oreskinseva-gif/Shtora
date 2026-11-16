package com.shtora.client.gui;

import com.shtora.client.Category;
import com.shtora.client.Module;
import com.shtora.client.ModuleManager;
import com.shtora.client.setting.BoolSetting;
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
 * Меню клиента (правый Shift).
 *
 * Вид — как на образце (ThunderHack): тёмные плоские панели-категории, компактные
 * строки модулей, переключатель справа. Панели раскладываются «кирпичиками» в
 * несколько колонок, поэтому влезают и на узкий экран телефона.
 *
 * Управление:
 *  - ЛКМ по строке модуля — включить/выключить;
 *  - ПКМ по строке модуля — раскрыть настройки;
 *  - ЛКМ по шапке панели — свернуть/развернуть категорию;
 *  - ★ и 🗑 в шапке — избранное и удаление (удалённая панель подсвечена жёлтым);
 *  - сверху фильтры ВСЕ / ИЗБР / УДАЛ и кнопка CFG (конфиги).
 */
public class ClickGuiScreen extends Screen {
    // ------------------------------------------------------------------ геометрия
    private static final int TOP = 22;        // верхняя полоса
    private static final int FILTER = 15;     // полоса фильтров
    private static final int HEAD = 16;       // шапка панели
    private static final int ROW = 14;        // строка модуля
    private static final int GAP = 4;
    private static final int MARGIN = 8;
    private static final int BOT = 16;

    // ------------------------------------------------------------------ состояние
    private static final List<Category> ORDER = new ArrayList<>(List.of(Category.values()));
    private static final Set<Category> FAV = EnumSet.noneOf(Category.class);
    private static final Set<Category> DEL = EnumSet.noneOf(Category.class);
    private static final Set<Category> CLOSED = EnumSet.noneOf(Category.class);
    private static int filter;                        // 0 — все, 1 — избранное, 2 — удалённые

    private final Anim open = new Anim(0);
    private final Anim scroll = new Anim(0);
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

    private int contentTop, listH, panelW, cols;
    private int searchX, searchY, searchW, searchH;
    private int cfgX, cfgY, cfgW, cfgH;
    private int filterY, filterH;

    private double pressX, pressY;
    private float moved;
    private boolean pressBg;
    private NumberSetting dragSlider;

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
        int mk = GLFW.GLFW_KEY_RIGHT_SHIFT;
        try { if (ModuleManager.ui != null) mk = ModuleManager.ui.menuKey.get(); } catch (Throwable ignored) {}
        if (mk > 0 && keyCode == mk && !searching) { close(); return true; }
        if (keyCode == GLFW.GLFW_KEY_ESCAPE && !searching) { close(); return true; }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public boolean charTyped(char chr, int modifiers) {
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

        mouseXNow = mouseX;
        mouseYNow = mouseY;
        open.to(closing ? 0f : 1f, 14f, dt);
        if (closing && open.v < 0.03f) { client.setScreen(null); return; }
        float a = Draw.ease(open.v);

        TextRenderer tr = client.textRenderer;
        int acc = Theme.accent();
        var ui = ModuleManager.ui;
        if (ui != null && ui.blur.get()) applyBlur();

        drawBackdrop(ctx, a);

        for (Category c : ORDER) byCat.put(c, ModuleManager.byCategory(c));

        contentTop = TOP + FILTER + 4;
        listH = Math.max(40, height - contentTop - BOT);
        filterY = TOP + 2;
        filterH = FILTER;

        drawTopBar(ctx, tr, a, acc);
        drawFilters(ctx, tr, a, acc);

        chips.clear();
        visible.clear();
        if (searching && query.length() > 0) {
            layoutResults(tr);
            drawResults(ctx, tr, mouseX, mouseY, a, acc, dt);
        } else {
            layoutPanels(tr);
            drawPanels(ctx, tr, mouseX, mouseY, a, acc, dt);
        }
        drawBottomBar(ctx, tr, a);

        if (cfgOpen) {
            layoutCfg();
            drawCfg(ctx, tr, mouseX, mouseY, a, acc);
        }
    }

    // ------------------------------------------------------------------ фон
    private void drawBackdrop(DrawContext ctx, float a) {
        ctx.fill(0, 0, width, height, Draw.argb(0x04070D, 0.66f * a));
        Draw.vgrad(ctx, 0, 0, width, Math.max(20, height / 3), 0x0A2036, 0x04070D, 0.30f * a);
    }

    // ------------------------------------------------------------------ верх и низ
    private void drawTopBar(DrawContext ctx, TextRenderer tr, float a, int acc) {
        int lh = 14, lw = Logo.widthFor(lh);
        Draw.rrect(ctx, MARGIN, 4, lw + 4, 15, 2, Draw.argb(acc, 0.16f * a));
        Logo.body(ctx, MARGIN + 2, 5, lw, lh, acc, a);

        int logoRight = MARGIN + lw + 6;
        int ww = Logo.wordWidthFor(10);
        if (width >= 350 && ww > 0) {
            Logo.word(ctx, logoRight + 3, 6, ww, 10, Theme.TEXT, a);
        }

        cfgW = 30;
        cfgH = 15;
        cfgX = width - MARGIN - cfgW;
        cfgY = 4;

        searchW = Math.min(130, Math.max(56, Math.round(width * 0.32f)));
        searchH = 15;
        searchY = 4;
        searchX = cfgX - 6 - searchW;
        int minX = logoRight + (width >= 350 ? ww + 8 : 4);
        if (searchX < minX) {
            searchX = minX;
            searchW = Math.max(40, cfgX - 6 - searchX);
        }

        // поиск
        Draw.rrect(ctx, searchX, searchY, searchW, searchH, 2,
                Draw.argb(Draw.mix(Theme.PANEL, acc, searching ? 0.20f : 0f), 0.92f * a));
        Draw.outline(ctx, searchX, searchY, searchW, searchH, 2,
                Draw.argb(Draw.mix(Theme.BORDER, acc, searching ? 1f : 0.25f), a));
        Draw.outline(ctx, searchX + 5, searchY + 4, 6, 6, 2, Draw.argb(Theme.TEXT_DIM, a));
        ctx.fill(searchX + 10, searchY + 9, searchX + 12, searchY + 11, Draw.argb(Theme.TEXT_DIM, a));
        String shown = searching ? Draw.trim(tr, query.toString(), searchW - 22) : "SEARCH";
        Draw.text(ctx, tr, shown, searchX + 16, searchY + 4, searching ? Theme.TEXT : Theme.TEXT_DIM, a);
        if (searching && (System.currentTimeMillis() / 500) % 2 == 0) {
            int cx = searchX + 16 + Draw.w(tr, shown);
            ctx.fill(cx + 1, searchY + 3, cx + 2, searchY + 12, Draw.argb(acc, a));
        }

        // кнопка CFG
        boolean hov = in(mouseXNow, mouseYNow, cfgX, cfgY, cfgW, cfgH);
        Draw.rrect(ctx, cfgX, cfgY, cfgW, cfgH, 2,
                Draw.argb(Draw.mix(Theme.PANEL, acc, cfgOpen ? 0.55f : hov ? 0.30f : 0.10f), 0.94f * a));
        Draw.outline(ctx, cfgX, cfgY, cfgW, cfgH, 2, Draw.argb(Draw.mix(Theme.BORDER, acc, cfgOpen ? 1f : 0.35f), a));
        Draw.text(ctx, tr, "CFG", cfgX + (cfgW - Draw.w(tr, "CFG")) / 2, cfgY + 4,
                cfgOpen ? 0x0A1626 : Theme.TEXT, a);

        ctx.fill(0, TOP - 1, width, TOP, Draw.argb(Theme.BORDER, 0.8f * a));
    }

    private int mouseXNow, mouseYNow;

    private void drawFilters(DrawContext ctx, TextRenderer tr, float a, int acc) {
        String[] names = {"ВСЕ", "ИЗБР", "УДАЛ"};
        int gap = 4;
        int w = (width - 2 * MARGIN - gap * 2) / 3;
        for (int i = 0; i < 3; i++) {
            int x = MARGIN + i * (w + gap);
            boolean sel = filter == i;
            int rgb = i == 2 ? Theme.DELETED : i == 1 ? Theme.STAR : acc;
            Draw.rrect(ctx, x, filterY, w, filterH, 2,
                    Draw.argb(Draw.mix(Theme.PANEL, rgb, sel ? 0.30f : 0.06f), (sel ? 0.95f : 0.80f) * a));
            Draw.outline(ctx, x, filterY, w, filterH, 2, Draw.argb(Draw.mix(Theme.BORDER, rgb, sel ? 1f : 0.25f), a));
            Draw.text(ctx, tr, names[i], x + (w - Draw.w(tr, names[i])) / 2, filterY + 4,
                    sel ? rgb : Theme.TEXT_DIM, a);
        }
    }

    private void drawBottomBar(DrawContext ctx, TextRenderer tr, float a) {
        Draw.text(ctx, tr, "ЛКМ — вкл/выкл   ПКМ — настройки", MARGIN, height - BOT + 4, Theme.TEXT_DIM, a * 0.8f);
        String right = "";
        try {
            if (client.player != null) right = client.player.getName().getString();
            if (client.getNetworkHandler() != null && client.player != null) {
                var e = client.getNetworkHandler().getPlayerListEntry(client.player.getUuid());
                if (e != null) right += "   " + e.getLatency() + "ms";
            }
        } catch (Throwable ignored) {}
        if (!right.isEmpty())
            Draw.text(ctx, tr, right, width - MARGIN - Draw.w(tr, right), height - BOT + 4, Theme.TEXT_DIM, a * 0.8f);
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

    private void layoutPanels(TextRenderer tr) {
        box.clear();
        cols = width < 350 ? 2 : width < 560 ? 3 : 4;
        panelW = (width - 2 * MARGIN - (cols - 1) * GAP) / cols;

        float[] colH = new float[cols];
        for (Category c : ORDER) {
            if (!matches(c)) continue;
            int h = panelHeight(tr, c);
            int col = 0;
            for (int i = 1; i < cols; i++) if (colH[i] < colH[col] - 0.5f) col = i;
            float x = MARGIN + col * (panelW + GAP);
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

    private void drawPanels(DrawContext ctx, TextRenderer tr, int mx, int my, float a, int acc, float dt) {
        ctx.enableScissor(0, contentTop, width, contentTop + listH);
        for (Category c : ORDER) {
            if (!matches(c)) continue;
            float[] r = box.get(c);
            if (r == null) continue;
            if (r[1] + r[3] < contentTop - 4 || r[1] > contentTop + listH + 4) continue;
            drawPanel(ctx, tr, c, Math.round(r[0]), Math.round(r[1]), Math.round(r[2]), Math.round(r[3]),
                    mx, my, a, acc, dt);
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
                           int mx, int my, float a, int acc, float dt) {
        boolean deleted = DEL.contains(c);
        boolean fav = FAV.contains(c);
        boolean closed = isClosed(c);

        int headBg = Draw.mix(Theme.PANEL_HEAD, deleted ? Theme.DELETED : fav ? Theme.STAR : acc,
                deleted ? 0.22f : fav ? 0.10f : 0.06f);
        int edge = deleted ? Theme.DELETED : fav ? Draw.mix(Theme.BORDER, Theme.STAR, 0.5f) : Theme.BORDER;

        Draw.rrect(ctx, x, y, w, h, 2, Draw.argb(Theme.PANEL, 0.94f * a));
        Draw.rrect(ctx, x, y, w, HEAD, 2, Draw.argb(headBg, 0.98f * a));
        ctx.fill(x, y + HEAD - 1, x + w, y + HEAD, Draw.argb(edge, 0.85f * a));
        Draw.outline(ctx, x, y, w, h, 2, Draw.argb(edge, 0.85f * a));

        // акцентная полоска слева у шапки
        Draw.rrect(ctx, x, y, 2, HEAD, 1, Draw.argb(deleted ? Theme.DELETED : acc, a));

        // заголовок
        String title = Draw.trim(tr, c.title, w - 44);
        Draw.text(ctx, tr, title, x + 6, y + 4, deleted ? Theme.DELETED : Theme.TEXT, a);
        List<Module> ms = byCat.getOrDefault(c, ModuleManager.byCategory(c));
        int on = 0;
        for (Module m : ms) if (m.isEnabled()) on++;
        Draw.text(ctx, tr, on + "/" + ms.size(), x + 8 + Draw.w(tr, title), y + 4, Theme.TEXT_DIM, a * 0.85f);

        // кнопки ★ и 🗑
        int cy = y + HEAD / 2;
        int trashCx = x + w - 9;
        int starCx = x + w - 22;
        boolean starHov = mx >= starCx - 5 && mx < starCx + 5 && my >= y && my < y + HEAD;
        boolean trashHov = mx >= trashCx - 5 && mx < trashCx + 5 && my >= y && my < y + HEAD;
        if (starHov) Draw.rrect(ctx, starCx - 5, y + 2, 10, HEAD - 4, 2, Draw.argb(0xFFFFFF, 0.07f * a));
        if (trashHov) Draw.rrect(ctx, trashCx - 5, y + 2, 10, HEAD - 4, 2, Draw.argb(0xFFFFFF, 0.07f * a));
        iconStar(ctx, starCx, cy, 4, Draw.argb(fav ? Theme.STAR : Theme.TEXT_DIM, a));
        if (deleted) iconRestore(ctx, trashCx, cy, 4, Draw.argb(Theme.RESTORE, a));
        else iconTrash(ctx, trashCx, cy, 4, Draw.argb(Theme.TRASH, a));

        if (closed) return;

        // модули
        int innerX = x + 4, innerW = w - 8;
        int ry = y + HEAD + 2;
        for (Module m : ms) {
            m.uiX = innerX;
            m.uiY = ry;
            m.uiW = innerW;
            m.uiH = ROW;
            m.toggleAnim.to(m.isEnabled() ? 1f : 0f, 14f, dt);
            m.expandAnim.to(m.expanded && !m.settings.isEmpty() ? 1f : 0f, 14f, dt);

            boolean hov = mx >= innerX && mx < innerX + innerW && my >= ry && my < ry + ROW
                    && my >= contentTop && my < contentTop + listH;
            m.hoverAnim.to(hov ? 1f : 0f, 16f, dt);
            visible.add(m);

            if (ry + ROW >= contentTop && ry <= contentTop + listH)
                drawRow(ctx, tr, m, innerX, ry, innerW, a, acc);
            ry += ROW;

            if (m.expanded && !m.settings.isEmpty()) {
                int sh = settingsHeight(tr, m, innerW - 8);
                if (ry + sh >= contentTop && ry <= contentTop + listH)
                    drawSettings(ctx, tr, m, innerX, ry, innerW, a, acc, dt);
                ry += sh;
            }
        }
    }

    // ------------------------------------------------------------------ строка модуля
    private void drawRow(DrawContext ctx, TextRenderer tr, Module m, int x, int y, int w, float a, int acc) {
        float tg = m.toggleAnim.v;
        int bg = Draw.mix(Theme.CARD, acc, 0.16f * tg);
        bg = Draw.mix(bg, 0xFFFFFF, 0.05f * m.hoverAnim.v);
        Draw.rrect(ctx, x, y, w, ROW - 1, 2, Draw.argb(bg, (0.55f + 0.35f * m.hoverAnim.v) * a));
        if (tg > 0.02f) Draw.rrect(ctx, x, y, 2, ROW - 1, 1, Draw.argb(acc, a));

        int tx = x + 6;
        Draw.text(ctx, tr, Draw.trim(tr, m.name, w - 30), tx, y + 3,
                Draw.mix(Theme.TEXT_DIM, 0xFFFFFF, tg), a);

        // переключатель
        int sx = x + w - 11, sy = y + (ROW - 1 - 8) / 2;
        Draw.rrect(ctx, sx, sy, 8, 8, 2, Draw.argb(Draw.mix(Theme.TOGGLE_OFF, acc, tg), a));
        Draw.outline(ctx, sx, sy, 8, 8, 2, Draw.argb(Draw.mix(Theme.BORDER, acc, tg), a));
        if (tg > 0.5f) {
            Draw.rrect(ctx, sx + 2, sy + 2, 4, 4, 1, Draw.argb(0x0A1626, a));
        }

        // «+» — есть настройки
        if (!m.settings.isEmpty()) {
            int cx = x + w - 18, cy = y + (ROW - 1) / 2;
            int cc = Draw.argb(Draw.mix(Theme.TEXT_DIM, acc, m.expandAnim.v), a);
            if (m.expandAnim.v < 0.5f) {
                Draw.line(ctx, cx - 2, cy, cx + 2, cy, cc);
                Draw.line(ctx, cx, cy - 2, cx, cy + 2, cc);
            } else {
                Draw.line(ctx, cx - 2, cy, cx + 2, cy, cc);
            }
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
                              float a, int acc, float dt) {
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
                b.anim.to(b.get() ? 1f : 0f, 16f, dt);
                Draw.text(ctx, tr, Draw.trim(tr, b.name, innerW - 24), innerX + 4, ry + 4, Theme.TEXT, ea);
                checkbox(ctx, innerX + innerW - 14, ry + 3, 10, b.anim.v, acc, ea);
            } else if (s instanceof NumberSetting n) {
                n.anim.to(n.frac(), 22f, dt);
                String vs = n.step >= 1 ? String.valueOf((int) Math.round(n.get()))
                        : String.format(Locale.ROOT, "%.2f", n.get());
                Draw.text(ctx, tr, Draw.trim(tr, n.name, innerW - 40), innerX + 4, ry + 2, Theme.TEXT, ea);
                Draw.text(ctx, tr, vs, innerX + innerW - 4 - Draw.w(tr, vs), ry + 2, acc, ea);
                int sx = innerX + 4, sw = innerW - 8, sy = ry + 15;
                Draw.rrect(ctx, sx, sy, sw, 3, 1, Draw.argb(0x000000, 0.45f * ea));
                int fw = Math.max(1, Math.round(n.anim.v * sw));
                Draw.rrect(ctx, sx, sy, fw, 3, 1, Draw.argb(acc, ea));
                Draw.rrect(ctx, sx + fw - 3, sy - 2, 6, 6, 3, Draw.argb(acc, ea));
            } else if (s instanceof ModeSetting mo) {
                Draw.text(ctx, tr, Draw.trim(tr, mo.name, innerW - 8), innerX + 4, ry + 2, Theme.TEXT, ea);
                int cxp = innerX + 4, cyp = ry + 13;
                for (int i = 0; i < mo.modes.length; i++) {
                    int tw = Draw.w(tr, mo.modes[i]) + 10;
                    if (cxp + tw > innerX + innerW - 4 && cxp > innerX + 4) { cxp = innerX + 4; cyp += 13; }
                    boolean sel = i == mo.index();
                    Draw.rrect(ctx, cxp, cyp, tw, 12, 2, Draw.argb(sel ? acc : Theme.PANEL_ALT, (sel ? 0.92f : 0.85f) * ea));
                    Draw.outline(ctx, cxp, cyp, tw, 12, 2, Draw.argb(sel ? acc : Theme.BORDER, ea));
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

    private static void checkbox(DrawContext ctx, int x, int y, int s, float on, int acc, float a) {
        Draw.rrect(ctx, x, y, s, s, 2, Draw.argb(Draw.mix(Theme.PANEL_ALT, acc, on), 0.95f * a));
        Draw.outline(ctx, x, y, s, s, 2, Draw.argb(Draw.mix(Theme.BORDER, acc, on), (0.8f + 0.2f * on) * a));
        if (on > 0.05f) {
            int col = Draw.argb(0x0A1626, on * a);
            Draw.line(ctx, x + 2, y + s / 2, x + s / 2 - 1, y + s - 3, col);
            Draw.line(ctx, x + s / 2 - 1, y + s - 3, x + s - 2, y + 3, col);
        }
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
            m.uiX = MARGIN;
            m.uiY = y;
            m.uiW = width - 2 * MARGIN;
            m.uiH = ROW;
            y += ROW;
            if (m.expanded && !m.settings.isEmpty()) y += settingsHeight(tr, m, width - 2 * MARGIN - 16);
            visible.add(m);
        }
        contentH = y - contentTop;
        maxScroll = Math.max(0f, contentH - listH);
        scrollTarget = Math.max(0f, Math.min(maxScroll, scrollTarget));
        scroll.to(scrollTarget, 22f, frameDt);
    }

    private void drawResults(DrawContext ctx, TextRenderer tr, int mouseX, int mouseY,
                             float a, int acc, float dt) {
        ctx.enableScissor(0, contentTop, width, contentTop + listH);
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
                    && mouseY >= contentTop && mouseY < contentTop + listH;
            m.hoverAnim.to(hov ? 1f : 0f, 16f, dt);
            if (y + ROW >= contentTop && y <= contentTop + listH)
                drawRow(ctx, tr, m, m.uiX, y, m.uiW, a, acc);
            int sy = y + ROW;
            if (m.expanded && !m.settings.isEmpty()) {
                int sh = settingsHeight(tr, m, m.uiW - 16);
                if (sy + sh >= contentTop && sy <= contentTop + listH)
                    drawSettings(ctx, tr, m, m.uiX + 4, sy, m.uiW - 8, a, acc, dt);
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
        ctx.fill(0, 0, width, height, Draw.argb(0x000000, 0.45f * a));
        Draw.rrect(ctx, cpx, cpy, cpw, cph, 3, Draw.argb(Theme.PANEL, 0.98f * a));
        Draw.rrect(ctx, cpx, cpy, cpw, 18, 3, Draw.argb(Theme.PANEL_HEAD, a));
        Draw.outline(ctx, cpx, cpy, cpw, cph, 3, Draw.argb(acc, 0.85f * a));
        Draw.text(ctx, tr, "CONFIGS", cpx + 8, cpy + 5, Theme.TEXT, a);
        Draw.text(ctx, tr, "×", cpx + cpw - 14, cpy + 4, Theme.TEXT_DIM, a);

        // поле имени
        Draw.rrect(ctx, fldX, fldY, fldW, fldH, 2,
                Draw.argb(Draw.mix(Theme.PANEL_ALT, acc, cfgTyping ? 0.25f : 0f), 0.95f * a));
        Draw.outline(ctx, fldX, fldY, fldW, fldH, 2, Draw.argb(cfgTyping ? acc : Theme.BORDER, a));
        String nm = cfgName.length() == 0 ? "config name" : cfgName.toString();
        Draw.text(ctx, tr, nm, fldX + 5, fldY + 4, cfgName.length() == 0 ? Theme.TEXT_DIM : Theme.TEXT, a);
        if (cfgTyping && (System.currentTimeMillis() / 500) % 2 == 0) {
            int cx = fldX + 5 + Draw.w(tr, cfgName.length() == 0 ? "" : cfgName.toString());
            ctx.fill(cx + 1, fldY + 3, cx + 2, fldY + fldH - 3, Draw.argb(acc, a));
        }

        cfgButton(ctx, tr, saveX, saveY, saveW, saveH, "SAVE", acc, a, true);
        cfgButton(ctx, tr, clearX, clearY, clearW, clearH, "CLEAR", acc, a, false);
        cfgButton(ctx, tr, closeX, closeY, closeW, closeH, "CLOSE", acc, a, false);

        // список конфигов
        ctx.enableScissor(lstX, lstY, lstX + lstW, lstY + lstH);
        if (cfgList.isEmpty()) {
            Draw.text(ctx, tr, "no configs yet", lstX + 4, lstY + 4, Theme.TEXT_DIM, a * 0.9f);
        }
        int ry = lstY - Math.round(cfgScroll);
        for (String name : cfgList) {
            if (ry + 14 >= lstY && ry <= lstY + lstH) {
                boolean hov = mx >= lstX && mx < lstX + lstW && my >= ry && my < ry + 14
                        && my >= lstY && my < lstY + lstH;
                Draw.rrect(ctx, lstX, ry, lstW, 13, 2, Draw.argb(hov ? Theme.PANEL_ALT : Theme.CARD, 0.9f * a));
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
        Draw.rrect(ctx, x, y, w, h, 2, Draw.argb(Draw.mix(Theme.PANEL_ALT, acc, primary ? 0.7f : 0.15f), 0.95f * a));
        Draw.outline(ctx, x, y, w, h, 2, Draw.argb(primary ? acc : Theme.BORDER, a));
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

    @Override
    public boolean mouseClicked(double mx, double my, int button) {
        mouseXNow = (int) mx;
        mouseYNow = (int) my;
        if (open.v < 0.9f || closing) return true;

        if (cfgOpen) {
            layoutCfg();
            // крестик
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

        // верхняя полоса
        if (my < TOP) {
            if (in(mx, my, cfgX, cfgY, cfgW, cfgH)) { openCfg(); return true; }
            if (in(mx, my, searchX, searchY, searchW, searchH)) { searching = true; return true; }
            if (searching) stopSearch();
            return true;
        }
        // фильтры
        if (my < filterY + filterH + 2) {
            int gap = 4, w = (width - 2 * MARGIN - gap * 2) / 3;
            for (int i = 0; i < 3; i++) {
                if (in(mx, my, MARGIN + i * (w + gap), filterY, w, filterH)) {
                    filter = i;
                    scrollTarget = 0f;
                    return true;
                }
            }
            return true;
        }
        if (my < contentTop || my > contentTop + listH) return true;

        // настройки раскрытых модулей
        for (Module m : visible) {
            if (m.expandAnim.v < 0.6f) continue;
            if (m.uiY + ROW < contentTop || m.uiY > contentTop + listH) continue;
            for (Setting s : m.settings) {
                if (!in(mx, my, s.ux, s.uy, s.uw, s.uh)) continue;
                if (s instanceof NumberSetting n) { dragSlider = n; setSlider(n, mx); return true; }
                if (s instanceof BoolSetting b) { b.toggle(); return true; }
            }
        }
        for (Chip c : chips) {
            if (c.y + c.h < contentTop || c.y > contentTop + listH) continue;
            if (in(mx, my, c.x, c.y, c.w, c.h)) { c.mo.set(c.idx); return true; }
        }

        // строки модулей
        for (Module m : visible) {
            if (!in(mx, my, m.uiX, m.uiY, m.uiW, ROW)) continue;
            if (button == 1) {
                if (!m.settings.isEmpty()) m.expanded = !m.expanded;
            } else if (!m.settings.isEmpty()
                    && mx >= m.uiX + m.uiW - 23 && mx < m.uiX + m.uiW - 14) {
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
                int cy = y + HEAD / 2;
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
    public boolean mouseDragged(double mx, double my, int button, double dx, double dy) {
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
