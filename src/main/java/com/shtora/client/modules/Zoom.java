package com.shtora.client.modules;

import com.shtora.client.Category;
import com.shtora.client.Module;
import com.shtora.client.setting.NumberSetting;
import net.minecraft.client.MinecraftClient;

public class Zoom extends Module {
    public final NumberSetting fov = add(new NumberSetting("FOV", 30, 30, 90, 5));
    private int prev = -1;

    public Zoom() { super("Zoom", Category.RENDER); }

    @Override
    public void onEnable(MinecraftClient mc) { prev = mc.options.getFov().getValue(); }

    @Override
    public void onTick(MinecraftClient mc) {
        if (prev < 0) prev = mc.options.getFov().getValue();
        mc.options.getFov().setValue((int) fov.get());
    }

    @Override
    public void onDisable(MinecraftClient mc) {
        if (prev >= 30) mc.options.getFov().setValue(prev);
        prev = -1;
    }
}
