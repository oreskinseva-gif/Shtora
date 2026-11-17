package com.shtora.client.util;

import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.text.Style;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;

public final class Draw {
    private Draw() {}

    public static float clamp(float x) { return Math.max(0f, Math.min(1f, x)); }

    public static float ease(float x) { float t = 1f - clamp(x); return 1f - t * t * t; }

    /** Плавное ускорение-замедление (для интро и переходов). */
    public static float easeInOutCubic(float x) {
        x = clamp(x);
        return x < 0.5f ? 4f * x * x * x : 1f - (float) Math.pow(-2f * x + 2f, 3) / 2f;
    }

    /** Очень мягкое затухание — почти всё движение в начале. */
    public static float easeOutQuint(float x) { float t = 1f - clamp(x); return 1f - t * t * t * t * t; }

    public static float easeOutExpo(float x) {
        x = clamp(x);
        return x >= 1f ? 1f : 1f - (float) Math.pow(2f, -10f * x);
    }

    /** С легким «перелётом» — панели и кнопки выезжают живее. */
    public static float easeOutBack(float x) {
        x = clamp(x);
        float c1 = 1.70158f, c3 = c1 + 1f, t = x - 1f;
        return 1f + c3 * t * t * t + c1 * t * t;
    }

    /** Упругое покачивание в конце — для появления логотипа. */
    public static float easeOutElastic(float x) {
        x = clamp(x);
        if (x <= 0f) return 0f;
        if (x >= 1f) return 1f;
        float c4 = (float) (2.0 * Math.PI / 3.0);
        return (float) (Math.pow(2f, -10f * x) * Math.sin((x * 10f - 0.75f) * c4) + 1f);
    }

    /** Сглаживание 0..1 (smoothstep). */
    public static float smooth(float x) { x = clamp(x); return x * x * (3f - 2f * x); }

    public static int argb(int rgb, float a) {
        return ((int) (clamp(a) * 255f) << 24) | (rgb & 0xFFFFFF);
    }

    public static int mix(int c1, int c2, float t) {
        t = clamp(t);
        int r = (int) (((c1 >> 16) & 255) * (1 - t) + ((c2 >> 16) & 255) * t);
        int g = (int) (((c1 >> 8) & 255) * (1 - t) + ((c2 >> 8) & 255) * t);
        int b = (int) ((c1 & 255) * (1 - t) + (c2 & 255) * t);
        return (r << 16) | (g << 8) | b;
    }

    /** Скруглённый прямоугольник (построчно). */
    public static void rrect(DrawContext c, int x, int y, int w, int h, int r, int color) {
        if (w <= 0 || h <= 0) return;
        r = Math.max(0, Math.min(r, Math.min(w, h) / 2));
        c.fill(x, y + r, x + w, y + h - r, color);
        for (int i = 0; i < r; i++) {
            double dy = r - i - 0.5;
            int inset = (int) Math.round(r - Math.sqrt(r * r - dy * dy));
            c.fill(x + inset, y + i, x + w - inset, y + i + 1, color);
            c.fill(x + inset, y + h - i - 1, x + w - inset, y + h - i, color);
        }
    }

    private static int inset(int r, int i) {
        double dy = r - i - 0.5;
        return (int) Math.round(r - Math.sqrt(Math.max(0, r * r - dy * dy)));
    }

    /** Тонкая (1px) скруглённая рамка. */
    public static void outline(DrawContext c, int x, int y, int w, int h, int r, int color) {
        if (w <= 2 || h <= 2) return;
        r = Math.max(0, Math.min(r, Math.min(w, h) / 2));
        c.fill(x + r, y, x + w - r, y + 1, color);
        c.fill(x + r, y + h - 1, x + w - r, y + h, color);
        c.fill(x, y + r, x + 1, y + h - r, color);
        c.fill(x + w - 1, y + r, x + w, y + h - r, color);
        for (int i = 0; i < r; i++) {
            int o = inset(r, i);
            int prev = i == 0 ? r : inset(r, i - 1);
            int span = Math.max(1, prev - o);
            c.fill(x + o, y + i, x + o + span, y + i + 1, color);
            c.fill(x + w - o - span, y + i, x + w - o, y + i + 1, color);
            c.fill(x + o, y + h - i - 1, x + o + span, y + h - i, color);
            c.fill(x + w - o - span, y + h - i - 1, x + w - o, y + h - i, color);
        }
    }

    /** Прямоугольная рамка по двум углам. */
    public static void box(DrawContext c, int x1, int y1, int x2, int y2, int color) {
        c.fill(x1, y1, x2, y1 + 1, color);
        c.fill(x1, y2 - 1, x2, y2, color);
        c.fill(x1, y1, x1 + 1, y2, color);
        c.fill(x2 - 1, y1, x2, y2, color);
    }

    /** Линия из коротких прямоугольников (одна заливка на строку или столбец). */
    public static void line(DrawContext c, int x1, int y1, int x2, int y2, int color) {
        int dx = x2 - x1, dy = y2 - y1;
        if (Math.abs(dy) >= Math.abs(dx)) {
            int sy = dy >= 0 ? 1 : -1;
            for (int y = y1; y != y2 + sy; y += sy) {
                float t = dy == 0 ? 0f : (float) (y - y1) / dy;
                float tn = dy == 0 ? 0f : (float) (y + sy - y1) / dy;
                int xa = Math.round(x1 + dx * t), xb = Math.round(x1 + dx * Math.min(1f, Math.max(0f, tn)));
                c.fill(Math.min(xa, xb), Math.min(y, y + 1), Math.max(xa, xb) + 1, Math.min(y, y + 1) + 1, color);
            }
        } else {
            int sx = dx >= 0 ? 1 : -1;
            for (int x = x1; x != x2 + sx; x += sx) {
                float t = (float) (x - x1) / dx;
                float tn = (float) (x + sx - x1) / dx;
                int ya = Math.round(y1 + dy * t), yb = Math.round(y1 + dy * Math.min(1f, Math.max(0f, tn)));
                c.fill(Math.min(x, x + 1), Math.min(ya, yb), Math.min(x, x + 1) + 1, Math.max(ya, yb) + 1, color);
            }
        }
    }

    /** Шрифт клиента (Carlito), подключён через assets/shtora/font/ui.json. */
    public static final Identifier FONT = Identifier.of("shtora", "ui");
    /** Готический шрифт «тетради смерти» (assets/shtora/font/gothic.json). */
    public static final Identifier FONT_GOTHIC = Identifier.of("shtora", "gothic");

    public static Text styled(String s) { return styled(s, FONT); }

    public static Text styled(String s, Identifier font) {
        return Text.literal(s).setStyle(Style.EMPTY.withFont(font));
    }

    public static int w(TextRenderer tr, String s) { return tr.getWidth(styled(s)); }

    public static int w(TextRenderer tr, String s, Identifier font) { return tr.getWidth(styled(s, font)); }

    public static String trim(TextRenderer tr, String s, int max) { return trim(tr, s, max, FONT); }

    public static String trim(TextRenderer tr, String s, int max, Identifier font) {
        if (w(tr, s, font) <= max) return s;
        String t = s;
        while (!t.isEmpty() && w(tr, t, font) > max) t = t.substring(0, t.length() - 1);
        return t;
    }

    public static void text(DrawContext c, TextRenderer tr, String s, int x, int y, int rgb, float a) {
        text(c, tr, s, x, y, rgb, a, FONT);
    }

    public static void text(DrawContext c, TextRenderer tr, String s, int x, int y, int rgb, float a, Identifier font) {
        if (a < 0.06f) return; // слишком прозрачный текст в майне рисуется непрозрачным
        c.drawText(tr, styled(s, font), x, y, argb(rgb, a), false);
    }

    /** Готический текст (буквы латиницы; кириллица уходит в запасной шрифт). */
    public static void gothic(DrawContext c, TextRenderer tr, String s, int x, int y, int rgb, float a) {
        text(c, tr, s, x, y, rgb, a, FONT_GOTHIC);
    }

    public static int gw(TextRenderer tr, String s) { return w(tr, s, FONT_GOTHIC); }

    /** Вертикальный градиент на всю область. */
    public static void vgrad(DrawContext c, int x, int y, int w, int h, int top, int bottom, float a) {
        if (w <= 0 || h <= 0) return;
        c.fillGradient(x, y, x + w, y + h, argb(top, a), argb(bottom, a));
    }

    /** Горизонтальный градиент (слева направо). */
    public static void hgrad(DrawContext c, int x, int y, int w, int h, int left, int right, float a) {
        if (w <= 0 || h <= 0) return;
        int n = Math.max(1, Math.min(24, w / 3));
        int step = Math.max(1, w / n);
        for (int i = 0; i < n; i++) {
            int x0 = x + i * step;
            int x1 = i == n - 1 ? x + w : x0 + step;
            c.fill(x0, y, x1, y + h, argb(mix(left, right, i / (float) (n - 1)), a));
        }
    }

    /**
     * Мягкое свечение вокруг прямоугольника: несколько расширяющихся рамок с малой
     * прозрачностью. Дёшево (без шейдеров) и хорошо смотрится на скруглённых карточках.
     */
    public static void glow(DrawContext c, int x, int y, int w, int h, int r, int rgb, float a) {
        if (a < 0.01f) return;
        int layers = 5;
        for (int i = layers; i >= 1; i--) {
            int e = i * 2;
            outline(c, x - e, y - e, w + e * 2, h + e * 2, r + e,
                    argb(rgb, a * 0.16f / i));
        }
    }

    /** Точка со свечением (для искр и капель). */
    public static void dot(DrawContext c, int x, int y, int r, int rgb, float a) {
        if (a < 0.02f) return;
        r = Math.max(1, r);
        c.fill(x - r * 2, y - r * 2, x + r * 2, y + r * 2, argb(rgb, a * 0.10f));
        c.fill(x - r, y - r, x + r, y + r, argb(rgb, a * 0.35f));
        c.fill(x - Math.max(1, r / 2), y - Math.max(1, r / 2),
                x + Math.max(1, r / 2), y + Math.max(1, r / 2), argb(rgb, a));
    }

    public static int hsb(float h, float s, float v) {
        float r, g, b;
        float hh = (h - (float) Math.floor(h)) * 6f;
        int i = (int) hh;
        float f = hh - i, p = v * (1 - s), q = v * (1 - s * f), t = v * (1 - s * (1 - f));
        switch (i % 6) {
            case 0: r = v; g = t; b = p; break;
            case 1: r = q; g = v; b = p; break;
            case 2: r = p; g = v; b = t; break;
            case 3: r = p; g = q; b = v; break;
            case 4: r = t; g = p; b = v; break;
            default: r = v; g = p; b = q;
        }
        return ((int) (r * 255) << 16) | ((int) (g * 255) << 8) | (int) (b * 255);
    }
}
