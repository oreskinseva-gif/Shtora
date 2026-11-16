package com.shtora.client.modules;

import com.shtora.client.Category;
import com.shtora.client.Module;
import net.minecraft.client.MinecraftClient;

public class BHop extends Module {
    public BHop() { super("BHop", Category.MOVEMENT); }

    @Override
    public void onTick(MinecraftClient mc) {
        if (mc.options.forwardKey.isPressed() && mc.player.isOnGround()) mc.player.jump();
    }
}
