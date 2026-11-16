package com.shtora.client.modules;

import com.shtora.client.Category;
import com.shtora.client.Module;
import com.shtora.client.ModuleManager;
import net.minecraft.client.MinecraftClient;

/** Одним нажатием выключает все модули (и прячет HUD клиента). */
public class Panic extends Module {
    public Panic() { super("Panic", Category.MISC); }

    @Override
    public void onEnable(MinecraftClient mc) {
        for (Module m : ModuleManager.modules) {
            if (m != this && m.isEnabled()) m.setEnabled(false);
        }
        setEnabled(false);
    }
}
