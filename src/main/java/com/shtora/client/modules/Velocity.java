package com.shtora.client.modules;

import com.shtora.client.Category;
import com.shtora.client.Module;
import com.shtora.client.setting.NumberSetting;
import net.minecraft.client.MinecraftClient;
import net.minecraft.util.math.Vec3d;

public class Velocity extends Module {
    public final NumberSetting horizontal = add(new NumberSetting("Horizontal %", 40, 0, 100, 5));
    public final NumberSetting vertical = add(new NumberSetting("Vertical %", 100, 0, 100, 5));
    private boolean handled;

    public Velocity() { super("Velocity", Category.COMBAT); }

    @Override
    public void onTick(MinecraftClient mc) {
        int hurt = mc.player.hurtTime;
        if (hurt == 0) { handled = false; return; }
        if (hurt >= 9 && !handled) {
            handled = true;
            Vec3d v = mc.player.getVelocity();
            double h = horizontal.get() / 100.0, vv = vertical.get() / 100.0;
            mc.player.setVelocity(v.x * h, v.y * vv, v.z * h);
        }
    }
}
