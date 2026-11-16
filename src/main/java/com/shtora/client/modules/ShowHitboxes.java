package com.shtora.client.modules;

import com.shtora.client.Category;
import com.shtora.client.Module;
import net.minecraft.client.MinecraftClient;

public class ShowHitboxes extends Module {
    public ShowHitboxes() { super("Hitboxes", Category.RENDER); }

    @Override
    public void onEnable(MinecraftClient mc) { mc.getEntityRenderDispatcher().setRenderHitboxes(true); }

    @Override
    public void onDisable(MinecraftClient mc) { mc.getEntityRenderDispatcher().setRenderHitboxes(false); }
}
