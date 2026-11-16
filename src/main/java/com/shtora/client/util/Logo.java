package com.shtora.client.util;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.texture.NativeImage;
import net.minecraft.client.texture.NativeImageBackedTexture;
import net.minecraft.util.Identifier;
import java.io.InputStream;

/**
 * Буква S клиента в стиле «тетради смерти» (готическая фрактура).
 *
 * Буква один раз растеризуется в две картинки высокого разрешения:
 *  - body — залитая буква со сглаженными краями;
 *  - line — контур со свечением вокруг.
 * Источник формы — TTF-шрифт deathnote.ttf через {@link Glyph} (AWT). Если AWT
 * в среде нет, берётся запасное векторное начертание (две дуги + перемычка),
 * поэтому буква есть всегда.
 *
 * На экран картинки просто масштабируются с билинейной фильтрацией, поэтому буква
 * остаётся гладкой на любом размере и не выглядит пиксельной.
 */
public final class Logo {
    private Logo() {}

    /** размер картинки буквы в пикселях — чем больше, тем глаже при увеличении */
    private static final int TW = 512, TH = 943;
    /** отношение ширины картинки к высоте */
    public static final float RATIO = (float) TW / TH;

    private static final Identifier ID_LINE = Identifier.of("shtora", "logo_line");
    private static final Identifier ID_BODY = Identifier.of("shtora", "logo_body");
    private static final Identifier ID_WORD = Identifier.of("shtora", "logo_word");

    // ------------------------------------------------------------ запасное векторное начертание
    private static final float UW = 76f, UH = 140f;   // система координат буквы
    private static final float CX = 38f;              // центр буквы по горизонтали
    private static final float CY1 = 39.5f;           // центр верхней чаши
    private static final float CY2 = 100.5f;          // центр нижней чаши
    private static final float R = 27f;               // радиус чаш по осевой линии
    private static final float A1 = -52f, A2 = -232f; // верхняя дуга: начало и конец
    private static final float B1 = -52f, B2 = 152f;  // нижняя дуга: начало и конец
    private static final float HMAX = 10.5f;          // половина толщины штриха

    private static final float P1X, P1Y, P2X, P2Y;    // перемычка: начало и конец
    private static final float LEN1, LENSEG, LEN2, LENTOT;

    static {
        P1X = ptX(CX, R, A2); P1Y = ptY(CY1, R, A2);
        P2X = ptX(CX, R, B1); P2Y = ptY(CY2, R, B1);
        LEN1 = R * (float) Math.toRadians(Math.abs(A2 - A1));
        LEN2 = R * (float) Math.toRadians(Math.abs(B2 - B1));
        LENSEG = (float) Math.hypot(P2X - P1X, P2Y - P1Y);
        LENTOT = LEN1 + LENSEG + LEN2;
    }

    /** Половина толщины штриха: к концам буква чуть тоньше — так она похожа на шрифт. */
    private static float half(float s) {
        return HMAX * (0.82f + 0.18f * (float) Math.sin(Math.PI * s));
    }

    /** Расстояние до контура буквы: меньше нуля — внутри, больше — снаружи. */
    private static float sd(float px, float py) {
        float f1 = LEN1 / LENTOT;
        float f2 = (LEN1 + LENSEG) / LENTOT;
        float best = arcDist(px, py, CX, CY1, R, A1, A2, 0f, f1);
        best = Math.min(best, segDist(px, py, P1X, P1Y, P2X, P2Y, f1, f2));
        best = Math.min(best, arcDist(px, py, CX, CY2, R, B1, B2, f2, 1f));
        return best;
    }

    /** Расстояние до дуги окружности с круглыми концами. */
    private static float arcDist(float px, float py, float cx, float cy, float r,
                                 float a0, float a1, float s0, float s1) {
        float dist = (float) Math.hypot(px - cx, py - cy);
        float ang = (float) Math.toDegrees(Math.atan2(py - cy, px - cx));
        float span = a1 - a0;
        float rel = span >= 0f ? norm360(ang - a0) : norm360(a0 - ang);
        float spanAbs = Math.abs(span);
        if (rel <= spanAbs) {
            return Math.abs(dist - r) - half(s0 + (s1 - s0) * (rel / spanAbs));
        }
        float toEnd = spanAbs - rel;
        if (rel <= toEnd) {
            return (float) Math.hypot(px - ptX(cx, r, a0), py - ptY(cy, r, a0)) - half(s0);
        }
        return (float) Math.hypot(px - ptX(cx, r, a1), py - ptY(cy, r, a1)) - half(s1);
    }

    /** Расстояние до отрезка (с той же толщиной штриха). */
    private static float segDist(float px, float py, float x1, float y1, float x2, float y2,
                                 float s0, float s1) {
        float dx = x2 - x1, dy = y2 - y1;
        float len2 = dx * dx + dy * dy;
        float t = len2 <= 0f ? 0f : ((px - x1) * dx + (py - y1) * dy) / len2;
        t = t < 0f ? 0f : (t > 1f ? 1f : t);
        float ds = (float) Math.hypot(px - (x1 + t * dx), py - (y1 + t * dy));
        return ds - half(s0 + (s1 - s0) * t);
    }

    private static float norm360(float a) {
        a = a % 360f;
        return a < 0f ? a + 360f : a;
    }

    private static float ptX(float c, float r, float deg) { return c + r * (float) Math.cos(Math.toRadians(deg)); }
    private static float ptY(float c, float r, float deg) { return c + r * (float) Math.sin(Math.toRadians(deg)); }

    // ------------------------------------------------------------ запекание картинки
    private static boolean ready;
    private static boolean failed;
    private static float[] dots = new float[0];
    private static int wordTW, wordTH;
    private static float wordRatio;

    /** Точки внутри буквы — к ним летят частицы в интро. Координаты 0..1. */
    public static float[] dots() {
        build();
        return dots;
    }

    private static void build() {
        if (ready || failed) return;
        try {
            // 1) покрытие буквы: картинка из сборки -> шрифт на месте -> вектор
            float[] cov = loadPng();
            if (cov == null) {
                try { cov = Glyph.coverage('S', TW, TH, 0.86f, 0.92f); } catch (Throwable ignored) {}
            }
            if (cov == null) cov = vectorCoverage();

            // 2) контур и свечение считаем из покрытия
            int glowR = Math.max(8, Math.round(15f / UW * TW));
            int bandR = Math.max(2, Math.round(1.9f / UW * TW));
            float[] glow = blur(blur(cov, TW, TH, glowR), TW, TH, glowR);
            float[] band = blur(cov, TW, TH, bandR);

            NativeImage body = new NativeImage(TW, TH, false);
            NativeImage line = new NativeImage(TW, TH, false);
            java.util.ArrayList<Float> pts = new java.util.ArrayList<>();
            int step = Math.max(4, TW / 36);

            for (int y = 0; y < TH; y++) {
                int base = y * TW;
                for (int x = 0; x < TW; x++) {
                    float c = cov[base + x];

                    float g = glow[base + x] * (1f - c) * 1.9f;
                    if (g > 1f) g = 1f;
                    float b = 1f - Math.abs(band[base + x] * 2f - 1f) * 1.5f;
                    if (b < 0f) b = 0f;

                    line.setColorArgb(x, y, white(Math.max(b, g * 0.7f)));
                    body.setColorArgb(x, y, white(c));

                    if (c > 0.6f && x % step == 0 && y % step == 0) {
                        pts.add((x + 0.5f) / TW);
                        pts.add((y + 0.5f) / TH);
                    }
                }
            }

            MinecraftClient mc = MinecraftClient.getInstance();
            register(mc, ID_LINE, line);
            register(mc, ID_BODY, body);
            loadWord(mc);

            float[] out = new float[pts.size()];
            for (int i = 0; i < out.length; i++) out[i] = pts.get(i);
            dots = out;
            ready = true;
        } catch (Throwable t) {
            failed = true;
            t.printStackTrace();
        }
    }

    /** Буква, запечённая при сборке (logo_s.png). null — если картинки нет. */
    private static float[] loadPng() {
        try (InputStream in = Logo.class.getResourceAsStream("/assets/shtora/textures/gui/logo_s.png")) {
            if (in == null) return null;
            NativeImage img = NativeImage.read(in);
            if (img.getWidth() != TW || img.getHeight() != TH) return null;
            float[] cov = new float[TW * TH];
            for (int y = 0; y < TH; y++) {
                int base = y * TW;
                for (int x = 0; x < TW; x++)
                    cov[base + x] = ((img.getColorArgb(x, y) >>> 24) & 255) / 255f;
            }
            return cov;
        } catch (Throwable t) {
            return null;
        }
    }

    /**
     * Слово «SHTORA» в готическом шрифте (logo_word.png), запечённое при сборке.
     * Если картинки нет — ширина остаётся 0, и меню нарисует слово обычным шрифтом.
     */
    private static void loadWord(MinecraftClient mc) {
        try (InputStream in = Logo.class.getResourceAsStream("/assets/shtora/textures/gui/logo_word.png")) {
            if (in == null) return;
            NativeImage img = NativeImage.read(in);
            wordTW = img.getWidth();
            wordTH = img.getHeight();
            if (wordTW <= 0 || wordTH <= 0) return;
            register(mc, ID_WORD, img);
            wordRatio = (float) wordTW / wordTH;
        } catch (Throwable ignored) {}
    }

    /** Ширина готического слова при заданной высоте (0 — картинки нет). */
    public static int wordWidthFor(int height) {
        build();
        return wordRatio <= 0f ? 0 : Math.max(1, Math.round(height * wordRatio));
    }

    /** Готическое слово «SHTORA» (гладкое при любом размере). */
    public static void word(DrawContext ctx, int x, int y, int w, int h, int rgb, float a) {
        if (a < 0.02f || w <= 0 || h <= 0) return;
        build();
        if (wordRatio <= 0f) return;
        ctx.drawTexture(RenderLayer::getGuiTextured, ID_WORD, x, y, 0f, 0f, w, h,
                wordTW, wordTH, wordTW, wordTH, Draw.argb(rgb, a));
    }

    /** Запасная буква из двух дуг и перемычки (если AWT/шрифт недоступны). */
    private static float[] vectorCoverage() {
        float[] cov = new float[TW * TH];
        float pix = UW / TW;
        for (int j = 0; j < TH; j++) {
            float py = (j + 0.5f) * UH / TH;
            int base = j * TW;
            for (int i = 0; i < TW; i++) {
                float px = (i + 0.5f) * UW / TW;
                cov[base + i] = clamp(0.5f - sd(px, py) / pix);
            }
        }
        return cov;
    }

    /** Разделительное размытие (box blur по двум проходам) — даёт мягкое свечение. */
    private static float[] blur(float[] src, int w, int h, int r) {
        float[] tmp = new float[src.length];
        float[] out = new float[src.length];
        float inv = 1f / (2 * r + 1);
        for (int y = 0; y < h; y++) {
            int base = y * w;
            float sum = 0f;
            for (int x = -r; x <= r; x++) sum += src[base + idx(x, w)];
            for (int x = 0; x < w; x++) {
                tmp[base + x] = sum * inv;
                sum += src[base + idx(x + r + 1, w)] - src[base + idx(x - r, w)];
            }
        }
        for (int x = 0; x < w; x++) {
            float sum = 0f;
            for (int y = -r; y <= r; y++) sum += tmp[idx(y, h) * w + x];
            for (int y = 0; y < h; y++) {
                out[y * w + x] = sum * inv;
                sum += tmp[idx(y + r + 1, h) * w + x] - tmp[idx(y - r, h) * w + x];
            }
        }
        return out;
    }

    private static int idx(int i, int n) { return i < 0 ? 0 : (i >= n ? n - 1 : i); }

    private static void register(MinecraftClient mc, Identifier id, NativeImage image) {
        NativeImageBackedTexture tex = new NativeImageBackedTexture(image);
        mc.getTextureManager().registerTexture(id, tex);
        tex.setFilter(true, false); // гладкое растягивание вместо квадратиков
    }

    private static float clamp(float v) { return v < 0f ? 0f : (v > 1f ? 1f : v); }

    private static int white(float a) {
        int al = (int) (clamp(a) * 255f + 0.5f);
        return (al << 24) | 0xFFFFFF;
    }

    // ------------------------------------------------------------ отрисовка
    /** Контур буквы вместе с мягким свечением вокруг. */
    public static void line(DrawContext ctx, int x, int y, int w, int h, int rgb, float a) {
        draw(ctx, ID_LINE, x, y, w, h, rgb, a);
    }

    /** Залитая буква. */
    public static void body(DrawContext ctx, int x, int y, int w, int h, int rgb, float a) {
        draw(ctx, ID_BODY, x, y, w, h, rgb, a);
    }

    private static void draw(DrawContext ctx, Identifier id, int x, int y, int w, int h, int rgb, float a) {
        if (a < 0.02f || w <= 0 || h <= 0) return;
        build();
        if (failed) return;
        // 13-аргументный overload: regionWidth/regionHeight — рисуем ВСЮ текстуру, а не её угол
        ctx.drawTexture(RenderLayer::getGuiTextured, id, x, y, 0f, 0f, w, h, TW, TH, TW, TH, Draw.argb(rgb, a));
    }

    /** Размер буквы при заданной высоте. */
    public static int widthFor(int height) { return Math.max(1, Math.round(height * RATIO)); }
}
