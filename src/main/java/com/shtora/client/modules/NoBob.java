package com.shtora.client.modules;

import com.shtora.client.Category;
import com.shtora.client.Module;
import net.minecraft.client.MinecraftClient;

public class NoBob extends Module {
    private boolean prev = true;

    public NoBob() { super("NoBob", Category.RENDER); }

    @Override
    public void onEnable(MinecraftClient mc) { prev = mc.options.getBobView().getValue(); }

    @Override
    public void onTick(MinecraftClient mc) { mc.options.getBobView().setValue(false); }

    @Override
    public void onDisable(MinecraftClient mc) { mc.options.getBobView().setValue(prev); }
}
