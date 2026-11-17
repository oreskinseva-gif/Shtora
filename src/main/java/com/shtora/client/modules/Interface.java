package com.shtora.client.modules;

import com.shtora.client.Category;
import com.shtora.client.Module;
import com.shtora.client.setting.BoolSetting;
import com.shtora.client.setting.NumberSetting;

public class Interface extends Module {
    public final BoolSetting watermark = add(new BoolSetting("Watermark", true));
    public final BoolSetting arrayList = add(new BoolSetting("Module list", true));
    public final BoolSetting blur = add(new BoolSetting("Blur", true));
    public final NumberSetting glass = add(new NumberSetting("Opacity", 0.88, 0.2, 0.9, 0.05));
    public final NumberSetting hue = add(new NumberSetting("Accent", 265, 0, 360, 1));
    /** Общая скорость анимаций меню (чем больше — тем резвее, но всё так же плавно). */
    public final NumberSetting animSpeed = add(new NumberSetting("Anim speed", 1.0, 0.5, 2.5, 0.1));

    public Interface() {
        super("Interface", Category.CLIENT);
        enableSilently();
    }
}
