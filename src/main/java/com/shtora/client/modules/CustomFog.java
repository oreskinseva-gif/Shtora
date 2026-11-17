package com.shtora.client.modules;

import com.shtora.client.Category;
import com.shtora.client.Module;
import com.shtora.client.setting.ColorSetting;
import com.shtora.client.setting.ModeSetting;
import com.shtora.client.setting.NumberSetting;

/**
 * «Custom fog» — объёмный визуальный дым поверх мира.
 *
 * Дым состоит из мягких дрейфующих «клубов» и плавного градиента: цвет начала
 * (низ / ближняя кромка) и цвет конца (верх / дальняя кромка) задаются отдельно,
 * поэтому можно сделать любой переход — от голубой дымки до оранжевого зарева.
 *
 * Всё рисуется экранными прямоугольниками в {@link com.shtora.client.gui.FogOverlay},
 * без шейдеров, поэтому работает на телефоне.
 */
public class CustomFog extends Module {
    public final ColorSetting start = add(new ColorSetting("Start color", 200, 0.90));
    public final ColorSetting end = add(new ColorSetting("End color", 225, 0.30));
    public final NumberSetting opacity = add(new NumberSetting("Opacity", 0.55, 0.05, 0.95, 0.05));
    public final NumberSetting density = add(new NumberSetting("Density", 0.60, 0.10, 1.00, 0.05));
    public final NumberSetting speed = add(new NumberSetting("Speed", 1.00, 0.20, 3.00, 0.10));
    public final NumberSetting size = add(new NumberSetting("Cloud size", 1.20, 0.50, 3.00, 0.10));
    public final NumberSetting height = add(new NumberSetting("Height", 0.75, 0.20, 1.00, 0.05));
    public final ModeSetting direction = add(new ModeSetting("Direction", "Rise", "Sink"));

    public CustomFog() {
        super("CustomFog", Category.RENDER);
    }
}
