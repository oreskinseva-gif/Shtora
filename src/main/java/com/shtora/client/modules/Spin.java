package com.shtora.client.modules;

import com.shtora.client.Category;
import com.shtora.client.Module;
import com.shtora.client.ModuleManager;
import com.shtora.client.setting.NumberSetting;
import com.shtora.client.util.Rotations;
import net.minecraft.client.MinecraftClient;

/** Персонаж крутится на месте для сервера и других игроков, камера остаётся на месте. */
public class Spin extends Module {
    public final NumberSetting speed = add(new NumberSetting("Speed", 40, 5, 120, 5));
    public final NumberSetting pitch = add(new NumberSetting("Pitch", 20, -90, 90, 5));
    private float yaw;
    private boolean active;

    public Spin() { super("Spin", Category.MISC); }

    @Override
    public void onTick(MinecraftClient mc) {
        if ((ModuleManager.killAura.isEnabled() && ModuleManager.killAura.isRotating())
                || (ModuleManager.scaffold.isEnabled() && ModuleManager.scaffold.isRotating())) return;
        if (!active) { yaw = mc.player.getYaw(); active = true; }
        yaw += (float) speed.get();
        Rotations.sendSilent(mc, yaw, (float) pitch.get());
    }

    @Override
    public void onDisable(MinecraftClient mc) {
        if (active) Rotations.restore(mc);
        active = false;
    }
}
