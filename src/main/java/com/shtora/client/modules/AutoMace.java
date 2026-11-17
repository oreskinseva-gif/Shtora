package com.shtora.client.modules;

import com.shtora.client.Category;
import com.shtora.client.Module;
import com.shtora.client.ModuleManager;
import com.shtora.client.setting.BoolSetting;
import com.shtora.client.setting.NumberSetting;
import com.shtora.client.util.CombatUtil;
import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.LivingEntity;
import net.minecraft.item.Items;
import net.minecraft.util.Hand;

/**
 * AutoMace — сам берёт булаву (mace) из хотбара и бьёт цель.
 *
 * Цель берётся у KillAura (если есть), иначе — ближайший подходящий игрок/моб.
 * Умеет бить только в падении (смэш наносит больше урона) и подпрыгивать,
 * чтобы это падение начать. Когда цель пропадает — возвращает прежний слот.
 */
public class AutoMace extends Module {
    public final NumberSetting range = add(new NumberSetting("Range", 3.2, 2.0, 6.0, 0.1));
    public final NumberSetting cps = add(new NumberSetting("CPS", 10, 1, 20, 1));
    public final BoolSetting smashOnly = add(new BoolSetting("Smash only", true));
    public final BoolSetting autoJump = add(new BoolSetting("Auto jump", true));
    public final BoolSetting rotate = add(new BoolSetting("Rotate", true));
    public final BoolSetting players = add(new BoolSetting("Players", true));
    public final BoolSetting mobs = add(new BoolSetting("Mobs", true));
    public final BoolSetting animals = add(new BoolSetting("Animals", false));
    public final BoolSetting switchBack = add(new BoolSetting("Switch back", true));

    private int prevSlot = -1;
    private long next;

    public AutoMace() { super("AutoMace", Category.COMBAT); }

    /** Слот хотбара с булавой или -1, если её нет. */
    private int maceSlot(MinecraftClient mc) {
        for (int i = 0; i < 9; i++) {
            if (mc.player.getInventory().getStack(i).isOf(Items.MACE)) return i;
        }
        return -1;
    }

    private LivingEntity target(MinecraftClient mc) {
        LivingEntity t = ModuleManager.killAura == null ? null : ModuleManager.killAura.current;
        if (t != null && t.isAlive() && !t.isRemoved() && mc.player.distanceTo(t) <= range.get()) return t;
        return CombatUtil.findTarget(mc, range.get(), 360f,
                players.get(), mobs.get(), animals.get(), true);
    }

    private void restore(MinecraftClient mc) {
        if (prevSlot >= 0 && mc.player != null) {
            mc.player.getInventory().selectedSlot = prevSlot;
            prevSlot = -1;
        }
    }

    @Override
    public void onDisable(MinecraftClient mc) { restore(mc); next = 0; }

    @Override
    public void onTick(MinecraftClient mc) {
        if (mc.player == null || mc.world == null || mc.currentScreen != null || mc.interactionManager == null) return;

        LivingEntity t = target(mc);
        int slot = maceSlot(mc);
        if (t == null || slot < 0) { if (switchBack.get()) restore(mc); return; }

        // берём булаву
        if (mc.player.getInventory().selectedSlot != slot) {
            if (prevSlot < 0) prevSlot = mc.player.getInventory().selectedSlot;
            mc.player.getInventory().selectedSlot = slot;
        }

        // смэш: бьём только в падении, при желании сами подпрыгиваем
        if (smashOnly.get()) {
            if (mc.player.isOnGround()) {
                if (autoJump.get()) mc.player.jump();
                return;
            }
            if (mc.player.fallDistance <= 0f) return;
        }

        long now = System.currentTimeMillis();
        if (now < next) return;
        if (mc.player.getAttackCooldownProgress(0.5f) < 1.0f) return;

        if (rotate.get()) {
            float[] r = CombatUtil.rotationTo(mc, t);
            CombatUtil.rotateToward(mc, r[0], r[1], 180f);
        }

        mc.interactionManager.attackEntity(mc.player, t);
        mc.player.swingHand(Hand.MAIN_HAND);
        next = now + (long) (1000.0 / Math.max(1.0, cps.get()));
    }
}
