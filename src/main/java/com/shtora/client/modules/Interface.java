package com.shtora.client.modules;

import com.shtora.client.Category;
import com.shtora.client.Module;
import com.shtora.client.setting.BoolSetting;
import com.shtora.client.setting.KeybindSetting;
import com.shtora.client.setting.NumberSetting;
import org.lwjgl.glfw.GLFW;

public class Interface extends Module {
    public final BoolSetting watermark = add(new BoolSetting("Watermark", true));
    public final BoolSetting arrayList = add(new BoolSetting("Module list", true));
    public final BoolSetting blur = add(new BoolSetting("Blur", true));
    public final NumberSetting glass = add(new NumberSetting("Opacity", 0.88, 0.2, 0.9, 0.05));
    public final NumberSetting hue = add(new NumberSetting("Accent", 210, 0, 360, 1));
    public final KeybindSetting menuKey = add(new KeybindSetting("Menu key", GLFW.GLFW_KEY_RIGHT_SHIFT));

    public Interface() {
        super("Interface", Category.CLIENT);
        enableSilently();
    }
}
