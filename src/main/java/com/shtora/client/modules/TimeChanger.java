package com.shtora.client.modules;

import com.shtora.client.Category;
import com.shtora.client.Module;
import com.shtora.client.setting.NumberSetting;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.world.ClientWorld;

public class TimeChanger extends Module {
    public final NumberSetting time = add(new NumberSetting("Time", 6000, 0, 24000, 500));

    public TimeChanger() { super("Time", Category.RENDER); }

    @Override
    public void onFrame(MinecraftClient mc, float dt) {
        ((ClientWorld.Properties) mc.world.getLevelProperties()).setTimeOfDay((long) time.get());
    }
}
