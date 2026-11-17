package com.shtora.client.modules;

import com.shtora.client.Category;
import com.shtora.client.Module;
import com.shtora.client.setting.ModeSetting;
import com.shtora.client.setting.NumberSetting;

/**
 * «Custom fog» — красивая атмосфера поверх мира. Три режима:
 *  - Aurora — в небе переливается северное сияние;
 *  - Lava   — раскалённое зарево снизу и искры, падающие с неба;
 *  - Water  — вода в небе капает дождём (свои «модельки» капель и круги на воде).
 *
 * Рисуется как экранный слой в {@link com.shtora.client.gui.FogOverlay}, поэтому
 * работает без шейдеров и не ломает производительность на телефоне.
 */
public class CustomFog extends Module {
    public final ModeSetting mode = add(new ModeSetting("Mode", "Aurora", "Lava", "Water"));
    public final NumberSetting opacity = add(new NumberSetting("Opacity", 0.55, 0.10, 0.90, 0.05));
    public final NumberSetting density = add(new NumberSetting("Density", 0.60, 0.10, 1.00, 0.05));
    public final NumberSetting speed = add(new NumberSetting("Speed", 1.00, 0.20, 3.00, 0.10));

    public CustomFog() {
        super("CustomFog", Category.RENDER);
    }
}
