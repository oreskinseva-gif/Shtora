package com.shtora.client.modules;

import com.shtora.client.Category;
import com.shtora.client.Module;
import net.minecraft.client.MinecraftClient;

public class AutoRespawn extends Module {
    public AutoRespawn() { super("AutoRespawn", Category.PLAYER); }

    @Override
    public void onTick(MinecraftClient mc) {
        if (mc.player.isDead()) mc.player.requestRespawn();
    }
}
