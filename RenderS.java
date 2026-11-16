import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.font.FontRenderContext;
import java.awt.font.GlyphVector;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.io.File;
import javax.imageio.ImageIO;

/**
 * Рисует готические надписи из шрифта в PNG прямо во время сборки (GitHub Actions).
 *
 * Запуск:  java RenderS.java <deathnote.ttf> <папка-назначения>
 * Пишет две картинки (белое на прозрачном):
 *  - logo_s.png    — буква S (512x943);
 *  - logo_word.png — слово SHTORA вплотную к буквам.
 * Мод берёт эти картинки, поэтому надписи гарантированно готические и гладкие
 * даже там, где в JRE нет java.desktop, и не зависят от размера шрифта в игре.
 */
public class RenderS {
    public static void main(String[] args) throws Exception {
        File ttf = new File(args[0]);
        File outDir = new File(args[1]);
        outDir.mkdirs();

        Font base = Font.createFont(Font.TRUETYPE_FONT, ttf);

        FontRenderContext frc;
        BufferedImage probe = new BufferedImage(1, 1, BufferedImage.TYPE_INT_ARGB);
        Graphics2D pg = probe.createGraphics();
        frc = pg.getFontRenderContext();
        pg.dispose();

        // ---------------------------------------------------------------- буква S
        int W = 512, H = 943;
        float fillW = 0.86f, fillH = 0.92f;

        Font font = base.deriveFont(1000f);
        GlyphVector gv = font.createGlyphVector(frc, new char[]{'S'});
        Rectangle2D b = gv.getVisualBounds();
        double s = Math.min((W * fillW) / b.getWidth(), (H * fillH) / b.getHeight());
        double tx = (W - b.getWidth() * s) / 2.0 - b.getX() * s;
        double ty = (H - b.getHeight() * s) / 2.0 - b.getY() * s;

        BufferedImage img = new BufferedImage(W, H, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = img.createGraphics();
        hints(g);
        g.setColor(Color.WHITE);
        g.translate(tx, ty);
        g.scale(s, s);
        g.fill(gv.getOutline());
        g.dispose();

        ImageIO.write(img, "png", new File(outDir, "logo_s.png"));
        System.out.println("logo_s.png: " + W + "x" + H);

        // ------------------------------------------------------------ слово SHTORA
        Font wfont = base.deriveFont(320f);
        GlyphVector wg = wfont.createGlyphVector(frc, "SHTORA".toCharArray());
        Rectangle2D wb = wg.getVisualBounds();
        int pad = 10;
        int WW = (int) Math.ceil(wb.getWidth()) + pad * 2;
        int WH = (int) Math.ceil(wb.getHeight()) + pad * 2;

        BufferedImage wimg = new BufferedImage(Math.max(1, WW), Math.max(1, WH), BufferedImage.TYPE_INT_ARGB);
        Graphics2D wg2 = wimg.createGraphics();
        hints(wg2);
        wg2.setColor(Color.WHITE);
        wg2.translate(pad - wb.getX(), pad - wb.getY());
        wg2.fill(wg.getOutline());
        wg2.dispose();

        ImageIO.write(wimg, "png", new File(outDir, "logo_word.png"));
        System.out.println("logo_word.png: " + WW + "x" + WH);
    }

    private static void hints(Graphics2D g) {
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
        g.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL, RenderingHints.VALUE_STROKE_PURE);
        g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
    }
}
