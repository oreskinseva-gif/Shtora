package com.shtora.client.modules;

import com.shtora.client.Category;
import com.shtora.client.Module;
import net.minecraft.client.MinecraftClient;

public class NoWeather extends Module {
    public NoWeather() { super("NoWeather", Category.RENDER); }

    @Override
    public void onFrame(MinecraftClient mc, float dt) {
        mc.world.setRainGradient(0f);
        mc.world.setThunderGradient(0f);
    }
}
