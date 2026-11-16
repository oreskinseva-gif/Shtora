package com.shtora.client.modules;

import com.shtora.client.Category;
import com.shtora.client.Module;
import com.shtora.client.setting.NumberSetting;
import net.minecraft.client.MinecraftClient;

public class AntiAFK extends Module {
    public final NumberSetting interval = add(new NumberSetting("Interval (s)", 20, 5, 60, 1));
    private int ticks;

    public AntiAFK() { super("AntiAFK", Category.MISC); }

    @Override
    public void onTick(MinecraftClient mc) {
        if (++ticks >= interval.get() * 20) {
            ticks = 0;
            if (mc.player.isOnGround()) mc.player.jump();
        }
    }
}
