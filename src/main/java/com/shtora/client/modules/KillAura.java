package com.shtora.client.modules;

import com.shtora.client.Category;
import com.shtora.client.Module;
import com.shtora.client.setting.BoolSetting;
import com.shtora.client.setting.ModeSetting;
import com.shtora.client.setting.NumberSetting;
import com.shtora.client.util.CombatUtil;
import com.shtora.client.util.Rotations;
import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.LivingEntity;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.EntityHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.MathHelper;
import java.util.concurrent.ThreadLocalRandom;

public class KillAura extends Module {
    public final ModeSetting mode = add(new ModeSetting("Mode", "Free", "Focused"));
    public final ModeSetting bypass = add(new ModeSetting("Bypass", "None", "Legit", "Strict", "Grim", "Matrix", "Vulcan", "NCP", "Intave"));
    public final ModeSetting attackMode = add(new ModeSetting("Attack", "Cooldown", "CPS"));
    public final NumberSetting cps = add(new NumberSetting("CPS", 12, 1, 20, 1));
    public final ModeSetting swingMode = add(new ModeSetting("Swing", "Client", "Silent", "None"));
    public final NumberSetting range = add(new NumberSetting("Range", 3.8, 2.0, 6.0, 0.1));
    public final NumberSetting fov = add(new NumberSetting("FOV", 360, 30, 360, 10));
    public final NumberSetting speed = add(new NumberSetting("Turn speed", 60, 10, 180, 5));
    public final BoolSetting players = add(new BoolSetting("Players", true));
    public final BoolSetting mobs = add(new BoolSetting("Mobs", true));
    public final BoolSetting animals = add(new BoolSetting("Animals", false));
    public final BoolSetting rotate = add(new BoolSetting("Rotate", true));
    public final BoolSetting silent = add(new BoolSetting("Silent", true));
    public final BoolSetting crits = add(new BoolSetting("Crits", false));
    public final BoolSetting walls = add(new BoolSetting("Through walls", false));
    public final BoolSetting raytrace = add(new BoolSetting("Raytrace", false));
    public final BoolSetting keepSprint = add(new BoolSetting("Keep sprint", true));
    public final BoolSetting randomize = add(new BoolSetting("Randomize", true));
    public final BoolSetting moveFix = add(new BoolSetting("Move fix", false));

    public LivingEntity current;
    private LivingEntity locked;
    private boolean has;
    private float sy, sp;
    private long nextAttack;
    private float jy, jp;

    public KillAura() { super("KillAura", Category.COMBAT); }

    public boolean isRotating() { return has; }

    private boolean strictLike() { return bypass.is("Strict") || bypass.is("Grim") || bypass.is("Matrix") || bypass.is("Vulcan") || bypass.is("NCP") || bypass.is("Intave"); }
    private boolean legitLike() { return bypass.is("Legit") || strictLike(); }

    private double reach() {
        double r = range.get();
        if (bypass.is("Strict") || bypass.is("Grim") || bypass.is("Intave")) return Math.min(r, 3.0);
        if (bypass.is("Matrix") || bypass.is("Vulcan") || bypass.is("NCP")) return Math.min(r, 3.1);
        if (bypass.is("Legit")) return Math.min(r, 3.2);
        return r;
    }

    @Override
    public void onDisable(MinecraftClient mc) {
        current = null;
        locked = null;
        if (has) Rotations.restore(mc);
        has = false;
        nextAttack = 0;
    }

    private LivingEntity pick(MinecraftClient mc) {
        double r = reach();
        if (!mode.is("Focused")) { locked = null; return find(mc, r); }
        if (locked != null) {
            boolean ok = locked.isAlive() && !locked.isRemoved()
                    && mc.player.distanceTo(locked) <= r * 1.25
                    && CombatUtil.isValid(mc, locked, players.get(), mobs.get(), animals.get());
            if (ok) return locked;
            locked = null;
        }
        HitResult hit = mc.crosshairTarget;
        if (hit != null && hit.getType() == HitResult.Type.ENTITY
                && ((EntityHitResult) hit).getEntity() instanceof LivingEntity le
                && CombatUtil.isValid(mc, le, players.get(), mobs.get(), animals.get())
                && mc.player.distanceTo(le) <= r) {
            locked = le;
        } else {
            locked = find(mc, r);
        }
        return locked;
    }

    private LivingEntity find(MinecraftClient mc, double r) {
        return CombatUtil.findTarget(mc, r, (float) fov.get(),
                players.get(), mobs.get(), animals.get(), walls.get() && !legitLike());
    }

    private boolean canSee(MinecraftClient mc, LivingEntity t) {
        if (!legitLike() && !raytrace.get()) return true;
        try { return mc.player.canSee(t); } catch (Throwable ignored) { return true; }
    }

    private long delay() {
        if (attackMode.is("Cooldown") && !legitLike()) return 0;
        double c = Math.max(1, cps.get());
        if (bypass.is("Grim")) c = Math.min(c, 10);
        if (bypass.is("Strict") || bypass.is("Intave")) c = Math.min(c, 11);
        if (bypass.is("Matrix") || bypass.is("Vulcan")) c = Math.min(c, 13);
        long base = (long) (1000.0 / c);
        if (randomize.get()) {
            long spread = Math.max(10, base / 3);
            base += ThreadLocalRandom.current().nextLong(-spread, spread + 1);
        }
        return Math.max(35, base);
    }

    private boolean lookingAt(MinecraftClient mc, LivingEntity t) {
        if (!raytrace.get() && !bypass.is("Grim")) return true;
        HitResult hit = mc.crosshairTarget;
        if (hit instanceof EntityHitResult ehr) return ehr.getEntity() == t;
        return !bypass.is("Grim");
    }

    @Override
    public void onTick(MinecraftClient mc) {
        if (mc.player == null || mc.world == null || mc.currentScreen != null || mc.interactionManager == null) return;
        LivingEntity t = pick(mc);
        current = t;
        if (t == null) {
            if (has) { has = false; Rotations.restore(mc); }
            return;
        }

        if (!canSee(mc, t)) {
            if (legitLike()) { if (has) { has = false; Rotations.restore(mc); } return; }
        }

        boolean strict = strictLike();
        boolean useSilent = silent.get() && !bypass.is("Grim") && !bypass.is("Strict");

        float[] r = CombatUtil.rotationTo(mc, t);
        float ty = r[0], tp = r[1];
        if (randomize.get() || legitLike()) {
            ThreadLocalRandom rr = ThreadLocalRandom.current();
            if (rr.nextInt(10) == 0) { jy = (rr.nextFloat() - 0.5f) * (strict ? 2.0f : 1.0f); jp = (rr.nextFloat() - 0.5f) * 1.0f; }
            ty += jy;
            tp = MathHelper.clamp(tp + jp, -90f, 90f);
        }

        boolean aimed = true;
        if (rotate.get()) {
            float step = (float) speed.get();
            if (randomize.get()) step *= 0.85f + ThreadLocalRandom.current().nextFloat() * 0.3f;
            if (bypass.is("Grim") || bypass.is("Matrix") || bypass.is("Vulcan")) step = Math.min(step, 70f);
            if (useSilent) {
                if (!has) { sy = mc.player.getYaw(); sp = mc.player.getPitch(); has = true; }
                sy += MathHelper.clamp(MathHelper.wrapDegrees(ty - sy), -step, step);
                sp = MathHelper.clamp(sp + MathHelper.clamp(tp - sp, -step, step), -90f, 90f);
                Rotations.sendSilent(mc, sy, sp);
                aimed = Math.abs(MathHelper.wrapDegrees(r[0] - sy)) < (bypass.is("Grim") ? 8f : 15f);
            } else {
                if (has) { has = false; Rotations.restore(mc); }
                CombatUtil.rotateToward(mc, ty, tp, step);
                aimed = CombatUtil.yawError(mc, r[0]) < (strict ? 10f : 25f);
            }
        }
        if (!aimed) return;
        if (!lookingAt(mc, t) && raytrace.get()) return;

        long now = System.currentTimeMillis();
        if (attackMode.is("Cooldown")) {
            if (mc.player.getAttackCooldownProgress(0.5f) < 1.0f) return;
            if (legitLike() && now < nextAttack) return;
        } else {
            if (now < nextAttack) return;
        }

        if (crits.get() && !mc.player.isTouchingWater() && !mc.player.isClimbing()) {
            if (mc.player.isOnGround()) { mc.player.jump(); return; }
            if (mc.player.fallDistance <= 0f && !bypass.is("Grim")) return;
        }

        if (!keepSprint.get() && mc.player.isSprinting()) {
            mc.player.setSprinting(false);
        }
        if (bypass.is("Legit") || bypass.is("NCP")) {
            if (mc.player.isSprinting() && !keepSprint.get()) mc.player.setSprinting(false);
        }

        mc.interactionManager.attackEntity(mc.player, t);
        if (!swingMode.is("None")) mc.player.swingHand(Hand.MAIN_HAND);
        nextAttack = now + delay();
        if (attackMode.is("Cooldown") && legitLike() && nextAttack <= now) nextAttack = now + 80;
    }
}
