package com.shtora.client.modules;

import com.shtora.client.Category;
import com.shtora.client.Module;
import com.shtora.client.setting.BoolSetting;
import com.shtora.client.setting.NumberSetting;
import com.shtora.client.util.CombatUtil;
import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.LivingEntity;
import net.minecraft.item.Items;
import net.minecraft.util.Hand;

/**
 * AutoMace — бьёт мaces по цели в прыжке/падении (smash-урон).
 * Работает только когда в основной руке булава (Mace).
 */
public class AutoMace extends Module {
    public final BoolSetting players = add(new BoolSetting("Players", true));
    public final BoolSetting mobs = add(new BoolSetting("Mobs", true));
    public final BoolSetting autoJump = add(new BoolSetting("Auto jump", true));
    public final BoolSetting rotate = add(new BoolSetting("Rotate", true));
    public final NumberSetting range = add(new NumberSetting("Range", 3.6, 2.0, 6.0, 0.1));
    public final NumberSetting minFall = add(new NumberSetting("Min fall", 1.5, 0.5, 8.0, 0.1));
    public final NumberSetting delay = add(new NumberSetting("Delay (ms)", 600, 150, 2000, 50));
    public final NumberSetting turnSpeed = add(new NumberSetting("Turn speed", 45, 5, 180, 5));

    private long next;

    public AutoMace() { super("AutoMace", Category.COMBAT); }

    @Override
    public void onDisable(MinecraftClient mc) { next = 0; }

    @Override
    public void onTick(MinecraftClient mc) {
        if (mc.player == null || mc.world == null || mc.currentScreen != null || mc.interactionManager == null) return;
        if (!mc.player.getMainHandStack().isOf(Items.MACE)) return;

        LivingEntity t = CombatUtil.findTarget(mc, range.get(), 360f,
                players.get(), mobs.get(), false, false);
        if (t == null) return;

        if (rotate.get()) {
            float[] r = CombatUtil.rotationTo(mc, t);
            CombatUtil.rotateToward(mc, r[0], r[1], (float) turnSpeed.get());
            if (CombatUtil.yawError(mc, r[0]) > 30f) return;
        }

        // Чтобы получить урон разбивания, нужно падать. Прыгаем, если ещё на земле.
        if (mc.player.isOnGround() && mc.player.fallDistance < minFall.get()) {
            if (autoJump.get()) mc.player.jump();
            return;
        }
        if (mc.player.fallDistance < minFall.get()) return;

        long now = System.currentTimeMillis();
        if (now < next) return;
        if (mc.player.getAttackCooldownProgress(0.5f) < 1.0f) return;

        mc.interactionManager.attackEntity(mc.player, t);
        mc.player.swingHand(Hand.MAIN_HAND);
        next = now + Math.max(150L, Math.round(delay.get()));
    }
}
