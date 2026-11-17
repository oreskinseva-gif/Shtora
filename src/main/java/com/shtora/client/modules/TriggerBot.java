package com.shtora.client.modules;

import com.shtora.client.Category;
import com.shtora.client.Module;
import com.shtora.client.setting.BoolSetting;
import com.shtora.client.setting.NumberSetting;
import com.shtora.client.util.CombatUtil;
import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.Entity;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.EntityHitResult;
import net.minecraft.util.hit.HitResult;
import java.util.concurrent.ThreadLocalRandom;

public class TriggerBot extends Module {
    public final BoolSetting players = add(new BoolSetting("Players", true));
    public final BoolSetting mobs = add(new BoolSetting("Mobs", true));
    public final BoolSetting animals = add(new BoolSetting("Animals", false));
    public final NumberSetting reach = add(new NumberSetting("Reach", 3.0, 2.0, 4.0, 0.1));
    public final NumberSetting delay = add(new NumberSetting("Delay (ms)", 60, 0, 300, 10));
    public final BoolSetting randomize = add(new BoolSetting("Randomize", true));
    public final BoolSetting visible = add(new BoolSetting("Visible only", true));

    private long nextAttack;

    public TriggerBot() { super("TriggerBot", Category.COMBAT); }

    @Override
    public void onTick(MinecraftClient mc) {
        if (mc.currentScreen != null || mc.interactionManager == null) return;
        HitResult hit = mc.crosshairTarget;
        if (hit == null || hit.getType() != HitResult.Type.ENTITY) return;
        Entity e = ((EntityHitResult) hit).getEntity();
        // AntiBot: NPC игнорируются (при выключенном AntiBot фильтр не срабатывает)
        if (!CombatUtil.isValidTarget(mc, e, players.get(), mobs.get(), animals.get())) return;
        if (mc.player.distanceTo(e) > reach.get()) return;
        if (visible.get() && !CombatUtil.hasLineOfSight(mc, e)) return;

        long now = System.currentTimeMillis();
        if (now < nextAttack) return;
        if (mc.player.getAttackCooldownProgress(0.5f) < 1.0f) return;

        mc.interactionManager.attackEntity(mc.player, e);
        mc.player.swingHand(Hand.MAIN_HAND);

        long d = (long) delay.get();
        if (randomize.get() && d > 0) d += ThreadLocalRandom.current().nextLong(0, Math.max(1, d / 2));
        nextAttack = now + d;
    }
}
