package com.shtora.client.modules;

import com.shtora.client.Category;
import com.shtora.client.Module;
import com.shtora.client.setting.NumberSetting;
import net.minecraft.client.MinecraftClient;

public class Fly extends Module {
    public final NumberSetting speed = add(new NumberSetting("Speed", 1.0, 0.5, 5.0, 0.1));

    public Fly() { super("Fly", Category.MOVEMENT); }

    @Override
    public void onTick(MinecraftClient mc) {
        mc.player.getAbilities().allowFlying = true;
        mc.player.getAbilities().flying = true;
        mc.player.getAbilities().setFlySpeed(0.05f * (float) speed.get());
    }

    @Override
    public void onDisable(MinecraftClient mc) {
        mc.player.getAbilities().setFlySpeed(0.05f);
        if (mc.player.isCreative() || mc.player.isSpectator()) return;
        mc.player.getAbilities().allowFlying = false;
        mc.player.getAbilities().flying = false;
    }
}
