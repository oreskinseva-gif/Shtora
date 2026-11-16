package com.shtora.client.util;

import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.font.FontRenderContext;
import java.awt.font.GlyphVector;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.io.InputStream;

/**
 * Растеризация одной буквы из TTF-шрифта средствами AWT (java.desktop).
 *
 * Класс специально вынесен отдельно: в части Android-сборок JRE нет java.desktop,
 * и тогда {@link Logo} получает null и рисует букву запасным векторным способом,
 * а не падает целиком.
 */
public final class Glyph {
    private Glyph() {}

    /** Шрифт в стиле «тетради смерти» (чёрная готика / фрактура). */
    private static final String RES = "/assets/shtora/font/deathnote.ttf";

    /**
     * Покрытие буквы (0..1) в картинке W×H.
     * fillW/fillH — какую долю ширины/высоты занимает буква (остальное — поля).
     * null — если AWT или шрифт недоступны.
     */
    public static float[] coverage(char glyph, int W, int H, float fillW, float fillH) {
        InputStream in = Glyph.class.getResourceAsStream(RES);
        if (in == null) return null;
        try (InputStream is = in) {
            Font font = Font.createFont(Font.TRUETYPE_FONT, is).deriveFont(1000f);

            BufferedImage probe = new BufferedImage(1, 1, BufferedImage.TYPE_INT_ARGB);
            Graphics2D pg = probe.createGraphics();
            FontRenderContext frc = pg.getFontRenderContext();
            pg.dispose();

            GlyphVector gv = font.createGlyphVector(frc, new char[]{glyph});
            Rectangle2D b = gv.getVisualBounds();
            if (b.getWidth() <= 0 || b.getHeight() <= 0) return null;

            double s = Math.min((W * fillW) / b.getWidth(), (H * fillH) / b.getHeight());
            double tx = (W - b.getWidth() * s) / 2.0 - b.getX() * s;
            double ty = (H - b.getHeight() * s) / 2.0 - b.getY() * s;

            BufferedImage img = new BufferedImage(W, H, BufferedImage.TYPE_INT_ARGB);
            Graphics2D g = img.createGraphics();
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
            g.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL, RenderingHints.VALUE_STROKE_PURE);
            g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
            g.setColor(Color.WHITE);
            g.translate(tx, ty);
            g.scale(s, s);
            g.fill(gv.getOutline());
            g.dispose();

            float[] cov = new float[W * H];
            int[] row = new int[W];
            for (int y = 0; y < H; y++) {
                img.getRGB(0, y, W, 1, row, 0, W);
                int base = y * W;
                for (int x = 0; x < W; x++) cov[base + x] = ((row[x] >>> 24) & 255) / 255f;
            }
            return cov;
        } catch (Throwable t) {
            return null;
        }
    }
}
