package com.shtora.client.modules;

import com.shtora.client.Category;
import com.shtora.client.Module;
import net.minecraft.client.MinecraftClient;

public class AutoWalk extends Module {
    public AutoWalk() { super("AutoWalk", Category.MISC); }

    @Override
    public void onTick(MinecraftClient mc) {
        if (mc.currentScreen == null) mc.options.forwardKey.setPressed(true);
    }

    @Override
    public void onDisable(MinecraftClient mc) { mc.options.forwardKey.setPressed(false); }
}
