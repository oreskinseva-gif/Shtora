package com.shtora.client.gui;

import com.mojang.authlib.GameProfile;
import com.shtora.client.util.Anim;
import com.shtora.client.util.Draw;
import com.shtora.client.util.Logo;
import com.shtora.client.util.Theme;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.PlayerSkinDrawer;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.multiplayer.MultiplayerScreen;
import net.minecraft.client.gui.screen.option.OptionsScreen;
import net.minecraft.client.gui.screen.world.SelectWorldScreen;
import net.minecraft.client.realms.gui.screen.RealmsMainScreen;
import net.minecraft.client.session.Session;
import net.minecraft.client.util.SkinTextures;
import net.minecraft.text.Text;
import net.minecraft.util.Util;
import java.nio.charset.StandardCharsets;
import java.util.UUID;

/**
 * Главный экран Shtora.
 *
 * Вид — как на образце MAIN: по центру сверху логотип (готическая буква S и слово
 * SHTORA из шрифта «тетради смерти»), под ним слоган, ниже — столбик кнопок.
 * Внизу слева версия, внизу справа — «Открыть папку ресурсов».
 *
 * Фон переливается из тёмно-синего в голубой и обратно, поверх — звёзды и мягкое
 * свечение. Сначала короткое интро: буква S собирается из частиц, затем плавно
 * проявляется меню.
 */
public class ShtoraTitleScreen extends Screen {
    // ---------------------------------------------------------------- тайминги интро (секунды)
    private static final float T_WELCOME = 0.35f, T_WELCOME_END = 1.15f;
    private static final float T_OUTLINE = 0.45f, T_OUTLINE_END = 1.10f;
    private static final float T_PARTICLES = 0.85f, T_PARTICLE_SPREAD = 0.85f, T_PARTICLE_DUR = 0.95f;
    private static final float T_MENU = 2.70f, T_MENU_END = 3.30f;

    private static final int BTN = 5;
    private static final String VERSION = "SHTORA CLIENT 1.21.4";

    /** Интро показываем только при первом входе, при возврате из мира — сразу меню. */
    private static boolean introShown;

    private final long start;
    private final Anim[] btnAnim = new Anim[BTN];
    private final int[] btnX = new int[BTN], btnY = new int[BTN], btnW = new int[BTN], btnH = new int[BTN];
    private final String[] btnLabel = {"ОДИНОЧНАЯ ИГРА", "СЕТЕВАЯ ИГРА", "REALMS", "НАСТРОЙКИ", "ВЫХОД ИЗ ИГРЫ"};
    private final String[] btnLabelShort = {"ОДИНОЧНАЯ", "СЕТЕВАЯ", "REALMS", "НАСТРОЙКИ", "ВЫХОД"};

    /** Точки внутри буквы S (парами x,y в диапазоне 0..1) — к ним летят частицы. */
    private final float[] dots;
    private float menuA, introA;
    private int hoverBtn = -1;

    private int packX, packY, packW, packH;
    private boolean packHover;

    private SkinTextures skin;
    private String nick = "Player";

    public ShtoraTitleScreen() {
        super(Text.empty());
        start = introShown ? System.currentTimeMillis() - 10_000L : System.currentTimeMillis();
        introShown = true;
        dots = Logo.dots();
        for (int i = 0; i < BTN; i++) btnAnim[i] = new Anim(0);
    }

    @Override public boolean shouldCloseOnEsc() { return false; }
    @Override public void renderBackground(DrawContext ctx, int mouseX, int mouseY, float delta) {}

    @Override
    protected void init() {
        loadPlayer();
    }

    private void loadPlayer() {
        try {
            Session s = client.getSession();
            nick = s.getUsername() == null || s.getUsername().isEmpty() ? "Player" : s.getUsername();
            UUID id = s.getUuidOrNull();
            if (id == null) id = UUID.nameUUIDFromBytes(("OfflinePlayer:" + nick).getBytes(StandardCharsets.UTF_8));
            skin = client.getSkinProvider().getSkinTextures(new GameProfile(id, nick));
        } catch (Throwable t) {
            skin = null;
        }
    }

    // ---------------------------------------------------------------- рендер
    @Override
    public void render(DrawContext ctx, int mouseX, int mouseY, float delta) {
        TextRenderer tr = client.textRenderer;
        int sw = width, sh = height;
        int acc = Theme.accent();
        float t = (System.currentTimeMillis() - start) / 1000f;

        menuA = smooth((t - T_MENU) / (T_MENU_END - T_MENU));
        introA = 1f - menuA;

        drawSky(ctx, sw, sh, t);
        if (introA > 0.01f) drawIntro(ctx, tr, sw, sh, acc, t, introA);
        if (menuA > 0.01f) drawMenu(ctx, tr, sw, sh, acc, mouseX, mouseY, delta, menuA, t);
    }

    /** Фон: переливающийся синий градиент, звёзды, свечение снизу и плывущее пятно света. */
    private void drawSky(DrawContext ctx, int sw, int sh, float t) {
        int bands = 48;
        for (int i = 0; i < bands; i++) {
            float u0 = i / (float) bands, u1 = (i + 1) / (float) bands;
            ctx.fillGradient(0, Math.round(sh * u0), sw, Math.round(sh * u1) + 1,
                    Draw.argb(Theme.sky(u0, t), 1f), Draw.argb(Theme.sky(u1, t), 1f));
        }

        for (int i = 0; i < 90; i++) {
            int sx = (int) (rnd(i, 11) * sw);
            int sy = (int) (rnd(i, 12) * sh * 0.82f);
            float tw = 0.12f + 0.30f * (float) Math.abs(Math.sin(t * 0.7 + i * 1.7));
            ctx.fill(sx, sy, sx + 1, sy + 1, Draw.argb(0xCFE7FF, tw));
        }

        int glow = Math.max(24, sh / 4);
        Draw.vgrad(ctx, 0, sh - glow, sw, glow, Theme.SKY_DARK, Theme.SKY_GLOW, 0.20f);

        int cx = Math.round(sw * (0.5f + 0.30f * (float) Math.sin(t * 0.18f)));
        int cy = Math.round(sh * (0.34f + 0.10f * (float) Math.cos(t * 0.23f)));
        radial(ctx, cx, cy, Math.max(40, Math.min(sw, sh) / 3), Theme.SKY_GLOW, 0.10f);
    }

    /** Мягкое круглое пятно света (несколько концентрических кругов). */
    private static void radial(DrawContext ctx, int cx, int cy, int r, int rgb, float a) {
        int steps = 6;
        for (int i = steps; i >= 1; i--) {
            int rr = Math.max(1, r * i / steps);
            Draw.rrect(ctx, cx - rr, cy - rr, rr * 2, rr * 2, rr, Draw.argb(rgb, a * 0.22f));
        }
    }

    // ---------------------------------------------------------------- интро
    private void drawIntro(DrawContext ctx, TextRenderer tr, int sw, int sh, int acc, float t, float a) {
        int lh = Math.max(96, Math.min(210, Math.round(Math.min(sw, sh) * 0.30f)));
        int lw = Logo.widthFor(lh);
        int lx = (sw - lw) / 2, ly = Math.round(sh * 0.34f) - lh / 2;

        // буква плавно проявляется, а контур со свечением виден с самого начала
        int n = dots.length / 2;
        float prog = Draw.ease(Draw.clamp((t - T_PARTICLES) / (T_PARTICLE_SPREAD + T_PARTICLE_DUR)));
        if (prog > 0.02f) {
            Logo.body(ctx, lx, ly, lw, lh, Draw.mix(acc, 0xFFFFFF, 0.18f), prog * a);
        }
        float oa = smooth((t - T_OUTLINE) / (T_OUTLINE_END - T_OUTLINE)) * a;
        if (oa > 0.01f) {
            float pulse = 0.6f + 0.4f * (float) Math.sin(t * 2.4);
            Logo.line(ctx, lx, ly, lw, lh, acc, (0.5f + 0.35f * pulse) * oa);
        }
        if (n > 0 && t >= T_PARTICLES - 0.2f) {
            int ps = Math.max(1, lw / 34);
            for (int k = 0; k < n; k++) {
                float delay = T_PARTICLES + rnd(k, 1) * T_PARTICLE_SPREAD;
                float p = Draw.ease(Draw.clamp((t - delay) / T_PARTICLE_DUR));
                if (p <= 0.002f) continue;
                float tx = lx + dots[k * 2] * lw, ty = ly + dots[k * 2 + 1] * lh;
                float sx = tx + (rnd(k, 2) - 0.5f) * sw * 1.25f;
                float sy = ty + (rnd(k, 3) - 0.5f) * sh * 1.25f;
                float px = sx + (tx - sx) * p, py = sy + (ty - sy) * p;
                int sz = Math.max(1, Math.round(ps * (0.45f + 0.55f * p)));
                ctx.fill(Math.round(px), Math.round(py), Math.round(px) + sz, Math.round(py) + sz,
                        Draw.argb(acc, a * (0.30f + 0.55f * p)));
            }
        }

        // короткая вспышка, когда буква собралась целиком
        if (prog > 0.985f) {
            float flash = Draw.clamp(1f - Math.abs(t - (T_PARTICLES + T_PARTICLE_SPREAD + T_PARTICLE_DUR)) / 0.3f);
            if (flash > 0.01f) Logo.body(ctx, lx, ly, lw, lh, 0xFFFFFF, 0.22f * flash * a);
        }

        // «Добро пожаловать» + ник — ниже середины
        float wa = smooth((t - T_WELCOME) / (T_WELCOME_END - T_WELCOME)) * a;
        if (wa > 0.01f) {
            String l1 = "Добро пожаловать";
            String l2 = nick;
            int y = Math.round(sh * 0.62f);
            Draw.text(ctx, tr, l1, (sw - Draw.w(tr, l1)) / 2, y, Theme.TEXT, wa);
            Draw.text(ctx, tr, l2, (sw - Draw.w(tr, l2)) / 2, y + 16, acc, wa);
        }
    }

    // ---------------------------------------------------------------- меню
    private void drawMenu(DrawContext ctx, TextRenderer tr, int sw, int sh, int acc,
                          int mx, int my, float delta, float a, float t) {
        // верхняя акцентная линия с плавным затуханием к краям
        for (int i = 0; i < 48; i++) {
            float f = 1f - Math.abs(i / 47f - 0.5f) * 2f;
            ctx.fill(sw * i / 48, 0, sw * (i + 1) / 48 + 1, 2, Draw.argb(acc, a * (0.30f + 0.70f * f)));
        }

        // логотип по центру: готическая S + готическое слово SHTORA
        int lh = Math.max(24, Math.min(44, Math.round(sh * 0.10f)));
        int lw = Logo.widthFor(lh);
        int wlh = Math.max(14, Math.round(lh * 0.70f));
        int ww = Logo.wordWidthFor(wlh);
        boolean hasWord = ww > 0;
        int gap = hasWord ? Math.max(5, lh / 4) : 0;
        int textW = hasWord ? ww : Draw.gw(tr, "SHTORA");
        int totalW = lw + gap + textW;
        int lx = Math.max(8, (sw - totalW) / 2);
        int ly = Math.max(7, Math.round(sh * 0.05f));

        Draw.glow(ctx, lx - 3, ly - 3, lw + 6, lh + 6, 7, acc, a * 0.45f);
        Logo.body(ctx, lx, ly, lw, lh, Draw.mix(acc, 0xFFFFFF, 0.28f), a);
        int ty = ly + (lh - wlh) / 2;
        if (hasWord) {
            Draw.glow(ctx, lx + lw + gap - 3, ty - 3, ww + 6, wlh + 6, 5, acc, a * 0.30f);
            Logo.word(ctx, lx + lw + gap, ty, ww, wlh, Theme.TEXT, a);
        } else {
            Draw.gothic(ctx, tr, "SHTORA", lx + lw + gap, ty + (wlh - 9) / 2, Theme.TEXT, a);
        }

        // слоган
        String slogan = "Shtora client free!";
        Draw.text(ctx, tr, slogan, (sw - Draw.w(tr, slogan)) / 2, ly + lh + 6, Theme.TEXT_DIM, a * 0.85f);

        // ник справа сверху (если хватает места)
        if (!nick.isEmpty()) {
            int nw = 8 + 14 + 4 + Draw.w(tr, nick) + 8;
            int nx = sw - 10 - nw, ny = 9;
            if (nx > lx + totalW + 10) {
                Draw.rrect(ctx, nx, ny, nw, 18, 5, Draw.argb(Theme.PANEL_ALT, 0.78f * a));
                Draw.outline(ctx, nx, ny, nw, 18, 5, Draw.argb(Draw.mix(Theme.BORDER, acc, 0.5f), a));
                int hx = nx + 3, hy = ny + 2;
                if (skin != null) {
                    try { PlayerSkinDrawer.draw(ctx, skin, hx, hy, 14); }
                    catch (Throwable ignored) { ctx.fill(hx, hy, hx + 14, hy + 14, Draw.argb(0x141B28, a)); }
                } else {
                    ctx.fill(hx, hy, hx + 14, hy + 14, Draw.argb(0x141B28, a));
                }
                Draw.text(ctx, tr, nick, hx + 18, ny + 5, Theme.TEXT, a);
            }
        }

        // столбик кнопок
        int gapY = 6;
        int top = ly + lh + 20;
        int bottom = sh - 32;
        int need = BTN * 16 + (BTN - 1) * gapY;
        int space = Math.max(need, bottom - top);
        int bh = Math.max(16, Math.min(30, (space - (BTN - 1) * gapY) / BTN));
        int bw = Math.min(250, sw - 36);
        if (bw < 110) bw = Math.max(70, sw - 14);
        int x0 = (sw - bw) / 2;
        int totalH = BTN * bh + (BTN - 1) * gapY;
        int y0 = top + Math.max(0, (space - totalH) / 2);

        hoverBtn = -1;
        for (int i = 0; i < BTN; i++) {
            btnX[i] = x0;
            btnY[i] = y0 + i * (bh + gapY);
            btnW[i] = bw;
            btnH[i] = bh;
            if (in(mx, my, btnX[i], btnY[i], bw, bh)) hoverBtn = i;
        }
        for (int i = 0; i < BTN; i++) {
            btnAnim[i].to(i == hoverBtn ? 1f : 0f, 16f, delta);
            String label = bw < 190 ? btnLabelShort[i] : btnLabel[i];
            drawButton(ctx, tr, btnX[i], btnY[i], bw, bh, label, btnAnim[i].v, a, acc);
        }

        // низ: версия слева, папка ресурсов справа
        Draw.text(ctx, tr, VERSION, 10, sh - 14, Theme.TEXT_DIM, a * 0.85f);
        String pack = "ОТКРЫТЬ ПАПКУ РЕСУРСОВ";
        packW = Draw.w(tr, pack);
        packH = 10;
        packX = sw - 10 - packW;
        packY = sh - 14;
        packHover = in(mx, my, packX - 5, packY - 4, packW + 10, packH + 8);
        Draw.text(ctx, tr, pack, packX, packY, packHover ? acc : Theme.TEXT_DIM, a * 0.9f);
        if (packHover) ctx.fill(packX, packY + 11, packX + packW, packY + 12, Draw.argb(acc, a));
    }

    private void drawButton(DrawContext ctx, TextRenderer tr, int x, int y, int w, int h,
                            String label, float hov, float a, int acc) {
        int bg = Draw.mix(Draw.mix(Theme.PANEL, 0x000000, 0.30f), acc, 0.16f * hov);
        if (hov > 0.01f) Draw.glow(ctx, x, y, w, h, 6, acc, a * 0.45f * hov);
        Draw.rrect(ctx, x, y, w, h, 6, Draw.argb(bg, (0.86f + 0.10f * hov) * a));
        Draw.outline(ctx, x, y, w, h, 6, Draw.argb(Draw.mix(Theme.BORDER, acc, hov), (0.7f + 0.3f * hov) * a));
        if (hov > 0.02f)
            Draw.rrect(ctx, x, y, w, h, 6, Draw.argb(acc, 0.10f * hov * a));

        int barW = 3 + Math.round(hov * 4);
        Draw.rrect(ctx, x, y, barW, h, 3, Draw.argb(acc, a));

        String lbl = Draw.trim(tr, label, w - barW - 30);
        Draw.text(ctx, tr, lbl, x + barW + 10, y + (h - 8) / 2, Draw.mix(Theme.TEXT, 0xFFFFFF, hov), a);

        int ax = x + w - 13 + Math.round(hov * 3), cy = y + h / 2;
        int ac = Draw.argb(acc, (0.4f + 0.6f * hov) * a);
        Draw.line(ctx, ax, cy - 3, ax + 3, cy, ac);
        Draw.line(ctx, ax + 3, cy, ax, cy + 3, ac);
    }

    // ---------------------------------------------------------------- ввод
    private static boolean in(double mx, double my, int x, int y, int w, int h) {
        return mx >= x && mx < x + w && my >= y && my < y + h;
    }

    @Override
    public boolean mouseClicked(double mx, double my, int button) {
        if (menuA < 0.9f) return true;
        if (in(mx, my, packX - 5, packY - 4, packW + 10, packH + 8)) { openResourcePacks(); return true; }
        for (int i = 0; i < BTN; i++) {
            if (!in(mx, my, btnX[i], btnY[i], btnW[i], btnH[i])) continue;
            switch (i) {
                case 0 -> client.setScreen(new SelectWorldScreen(this));
                case 1 -> client.setScreen(new MultiplayerScreen(this));
                case 2 -> {
                    try { client.setScreen(new RealmsMainScreen(this)); } catch (Throwable ignored) {}
                }
                case 3 -> client.setScreen(new OptionsScreen(this, client.options));
                case 4 -> client.scheduleStop();
            }
            return true;
        }
        return true;
    }

    private void openResourcePacks() {
        try { Util.getOperatingSystem().open(client.getResourcePackDir()); }
        catch (Throwable ignored) {}
    }

    // ---------------------------------------------------------------- утилиты
    private static float smooth(float x) {
        x = Draw.clamp(x);
        return x * x * (3f - 2f * x);
    }

    /** Устойчивое псевдослучайное число 0..1 по индексу — частицы не «дрожат» между кадрами. */
    private static float rnd(int i, int salt) {
        int x = i * 374761393 + salt * 668265263;
        x = (x ^ (x >> 13)) * 1274126177;
        x ^= (x >> 16);
        return (x & 0x7FFFFFFF) / (float) 0x7FFFFFFF;
    }
}
