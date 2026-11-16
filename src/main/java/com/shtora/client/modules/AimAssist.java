package com.shtora.client.modules;

import com.shtora.client.Category;
import com.shtora.client.Module;
import com.shtora.client.setting.BoolSetting;
import com.shtora.client.setting.NumberSetting;
import com.shtora.client.util.CombatUtil;
import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.LivingEntity;
import net.minecraft.util.math.MathHelper;

public class AimAssist extends Module {
    public final NumberSetting range = add(new NumberSetting("Range", 4.5, 2.0, 6.0, 0.1));
    public final NumberSetting fov = add(new NumberSetting("FOV", 90, 20, 360, 10));
    public final NumberSetting speed = add(new NumberSetting("Speed", 6, 1, 20, 1));
    public final BoolSetting players = add(new BoolSetting("Players", true));
    public final BoolSetting mobs = add(new BoolSetting("Mobs", false));
    public final BoolSetting onClick = add(new BoolSetting("Only on click", true));

    public AimAssist() { super("AimAssist", Category.COMBAT); }

    /** Работает каждый кадр, а не раз в тик: движение получается плавным, без рывков. */
    @Override
    public void onFrame(MinecraftClient mc, float dt) {
        if (mc.currentScreen != null) return;
        if (onClick.get() && !mc.options.attackKey.isPressed()) return;
        LivingEntity t = CombatUtil.findTarget(mc, range.get(), (float) fov.get(),
                players.get(), mobs.get(), false, false);
        if (t == null) return;

        float[] r = CombatUtil.rotationTo(mc, t);
        float dyaw = MathHelper.wrapDegrees(r[0] - mc.player.getYaw());
        float dpitch = r[1] - mc.player.getPitch();
        if (Math.abs(dyaw) < 0.25f && Math.abs(dpitch) < 0.25f) return;

        // экспоненциальное сглаживание: быстро в начале, мягко у цели
        float k = 1f - (float) Math.exp(-speed.get() * 0.8 * dt);
        float cap = 600f * dt; // максимум градусов за кадр
        float ay = MathHelper.clamp(dyaw * k, -cap, cap);
        float ap = MathHelper.clamp(dpitch * k * 0.7f, -cap, cap);

        // changeLookDirection двигает и prevYaw/prevPitch, поэтому кадры не дёргаются
        mc.player.changeLookDirection(ay / 0.15, ap / 0.15);
    }
}
