package com.shtora.client.setting;

import com.shtora.client.util.Anim;
import com.shtora.client.util.Draw;

/**
 * Настройка-цвет: оттенок (0..360) и яркость (0.15..1).
 *
 * В меню рисуется как радужная полоса (оттенок) и полоса яркости, рядом — образец
 * получившегося цвета. Используется, например, для дыма в CustomFog.
 */
public class ColorSetting extends Setting {
    public static final double MIN_B = 0.15, MAX_B = 1.0;

    private double hue, bright;
    public final Anim animH, animB;

    public ColorSetting(String name, double hue, double bright) {
        super(name);
        this.hue = clampHue(hue);
        this.bright = clampB(bright);
        this.animH = new Anim((float) (this.hue / 360.0));
        this.animB = new Anim((float) ((this.bright - MIN_B) / (MAX_B - MIN_B)));
    }

    public static double clampHue(double h) { return Math.max(0.0, Math.min(360.0, h)); }
    public static double clampB(double b) { return Math.max(MIN_B, Math.min(MAX_B, b)); }

    public double hue() { return hue; }
    public double bright() { return bright; }
    public void setHue(double h) { hue = clampHue(h); }
    public void setBright(double b) { bright = clampB(b); }

    public float fracH() { return (float) (hue / 360.0); }
    public float fracB() { return (float) ((bright - MIN_B) / (MAX_B - MIN_B)); }

    /** Итоговый цвет (0xRRGGBB). */
    public int rgb() { return Draw.hsb((float) (hue / 360.0), 0.72f, (float) bright); }

    /** Строка для конфига: "hue;bright". */
    public String serialize() { return hue + ";" + bright; }

    public void deserialize(String s) {
        int i = s.indexOf(';');
        try {
            if (i < 0) {
                setHue(Double.parseDouble(s.trim()));
            } else {
                setHue(Double.parseDouble(s.substring(0, i).trim()));
                setBright(Double.parseDouble(s.substring(i + 1).trim()));
            }
        } catch (NumberFormatException ignored) {
        }
    }
}
