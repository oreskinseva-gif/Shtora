package com.shtora.client.modules;

import com.shtora.client.Category;
import com.shtora.client.Module;
import net.minecraft.client.MinecraftClient;

public class Sneak extends Module {
    public Sneak() { super("Sneak", Category.PLAYER); }

    @Override
    public void onTick(MinecraftClient mc) { mc.options.sneakKey.setPressed(true); }

    @Override
    public void onDisable(MinecraftClient mc) { mc.options.sneakKey.setPressed(false); }
}
