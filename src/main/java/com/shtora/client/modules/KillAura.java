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

/**
 * KillAura с «обходами» (Bypass):
 *
 *  - None   — как раньше: бьём всё, что в радиусе;
 *  - Legit  — reach не больше 3.0, бьём только видимые цели, останавливаем спринт
 *             перед ударом, между ударами пауза и небольшой разброс поворота;
 *  - Strict — то же, но reach до 2.8, паузы длиннее, поворот сильнее «дрожит»,
 *             всегда видимый поворот (silent выключается) — это выглядит наиболее
 *             похоже на игру руками.
 *
 * ВАЖНО: ни один клиент не даёт 100% гарантии невидимости для современных
 * античитов (Matrix, Grim, Intave и т.п.). Обходы лишь убирают самые заметные
 * признаки автоматизации.
 */
public class KillAura extends Module {
    public final ModeSetting mode = add(new ModeSetting("Mode", "Free", "Focused"));
    public final ModeSetting bypass = add(new ModeSetting("Bypass", "None", "Legit", "Strict"));
    public final NumberSetting range = add(new NumberSetting("Range", 3.0, 2.0, 6.0, 0.1));
    public final NumberSetting fov = add(new NumberSetting("FOV", 180, 30, 360, 10));
    public final NumberSetting speed = add(new NumberSetting("Turn speed", 60, 10, 180, 5));
    public final BoolSetting players = add(new BoolSetting("Players", true));
    public final BoolSetting mobs = add(new BoolSetting("Mobs", true));
    public final BoolSetting animals = add(new BoolSetting("Animals", false));
    public final BoolSetting rotate = add(new BoolSetting("Rotate", true));
    public final BoolSetting silent = add(new BoolSetting("Silent", true));
    public final BoolSetting crits = add(new BoolSetting("Crits", false));
    public final BoolSetting walls = add(new BoolSetting("Through walls", false));
    public final BoolSetting randomize = add(new BoolSetting("Randomize", true));
    public final BoolSetting stopSprint = add(new BoolSetting("Stop sprint", true));

    public LivingEntity current;
    private LivingEntity locked;
    private boolean has;
    private float sy, sp;
    private long nextAttack;
    private float jy, jp;

    public KillAura() { super("KillAura", Category.COMBAT); }

    public boolean isRotating() { return has; }

    /** Максимальная дистанция удара с учётом обхода. */
    private double reach() {
        double r = range.get();
        if (bypass.is("Strict")) return Math.min(r, 2.8);
        if (bypass.is("Legit")) return Math.min(r, 3.0);
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

    /** Free: всегда ближайшая цель. Focused: держимся за выбранную, пока она жива и рядом. */
    private LivingEntity pick(MinecraftClient mc) {
        double r = reach();
        if (!mode.is("Focused")) { locked = null; return find(mc, r); }
        if (locked != null) {
            boolean ok = locked.isAlive() && !locked.isRemoved()
                    && mc.player.distanceTo(locked) <= r * 1.25
                    && CombatUtil.isValidTarget(mc, locked, players.get(), mobs.get(), animals.get());
            if (ok) return locked;
            locked = null;
        }
        HitResult hit = mc.crosshairTarget;
        if (hit != null && hit.getType() == HitResult.Type.ENTITY
                && ((EntityHitResult) hit).getEntity() instanceof LivingEntity le
                && CombatUtil.isValidTarget(mc, le, players.get(), mobs.get(), animals.get())
                && mc.player.distanceTo(le) <= r) {
            locked = le;
        } else {
            locked = find(mc, r);
        }
        return locked;
    }

    private LivingEntity find(MinecraftClient mc, double r) {
        return CombatUtil.findTarget(mc, r, (float) fov.get(),
                players.get(), mobs.get(), animals.get(), walls.get(), true);
    }

    /** Пауза между ударами, мс. У «легальных» обходов она длиннее и со случайным разбросом. */
    private long delay() {
        if (bypass.is("None")) return 0;
        ThreadLocalRandom r = ThreadLocalRandom.current();
        long base = bypass.is("Strict") ? 120 : 90;
        long spread = bypass.is("Strict") ? 70 : 55;
        return randomize.get() ? base + r.nextLong(spread) : base;
    }

    @Override
    public void onTick(MinecraftClient mc) {
        if (mc.currentScreen != null || mc.interactionManager == null) return;
        LivingEntity t = pick(mc);
        current = t;
        if (t == null) {
            if (has) { has = false; Rotations.restore(mc); }
            return;
        }

        // Обходы: не бьём через стены и только то, что реально видно
        if (bypass.is("Legit") || bypass.is("Strict")) {
            if (!CombatUtil.hasLineOfSight(mc, t)) return;
        }

        boolean random = randomize.get();
        boolean strict = bypass.is("Strict");
        boolean useSilent = silent.get() && !strict;

        // цель поворота с небольшим «дрожанием» — как у живого игрока
        float[] r = CombatUtil.rotationTo(mc, t);
        float ty = r[0], tp = r[1];
        if (random) {
            ThreadLocalRandom rr = ThreadLocalRandom.current();
            if (rr.nextInt(12) == 0) { jy = (rr.nextFloat() - 0.5f) * (strict ? 2.2f : 1.2f); jp = (rr.nextFloat() - 0.5f) * 1.2f; }
            ty += jy;
            tp = MathHelper.clamp(tp + jp, -90f, 90f);
        }

        boolean aimed = true;
        if (rotate.get()) {
            float step = (float) speed.get();
            if (random) step *= 0.85f + ThreadLocalRandom.current().nextFloat() * 0.3f;
            if (useSilent) {
                if (!has) { sy = mc.player.getYaw(); sp = mc.player.getPitch(); has = true; }
                sy += MathHelper.clamp(MathHelper.wrapDegrees(ty - sy), -step, step);
                sp = MathHelper.clamp(sp + MathHelper.clamp(tp - sp, -step, step), -90f, 90f);
                Rotations.sendSilent(mc, sy, sp);
                aimed = Math.abs(MathHelper.wrapDegrees(r[0] - sy)) < 15f;
            } else {
                CombatUtil.rotateToward(mc, ty, tp, step);
                aimed = CombatUtil.yawError(mc, r[0]) < (strict ? 8f : 25f);
            }
        }
        if (!aimed) return;

        long now = System.currentTimeMillis();
        if (now < nextAttack) return;
        if (mc.player.getAttackCooldownProgress(0.5f) < 1.0f) return;

        if (crits.get() && !mc.player.isTouchingWater() && !mc.player.isClimbing()) {
            if (mc.player.isOnGround()) { mc.player.jump(); return; }
            if (mc.player.fallDistance <= 0f) return;
        }

        // Обходы: спринт перед ударом сбрасываем (сервер видит обычный удар)
        if ((bypass.is("Legit") || strict) && stopSprint.get() && mc.player.isSprinting()) {
            mc.player.setSprinting(false);
        }

        mc.interactionManager.attackEntity(mc.player, t);
        mc.player.swingHand(Hand.MAIN_HAND);
        nextAttack = now + delay();
    }
}
