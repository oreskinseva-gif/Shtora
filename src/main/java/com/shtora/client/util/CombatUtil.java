package com.shtora.client.util;

import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.mob.Monster;
import net.minecraft.entity.passive.AnimalEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;

public final class CombatUtil {
    private CombatUtil() {}

    public static boolean isValid(MinecraftClient mc, Entity e, boolean players, boolean mobs, boolean animals) {
        if (!(e instanceof LivingEntity le) || e == mc.player || !le.isAlive()) return false;
        if (e instanceof PlayerEntity p) return players && !p.isSpectator();
        if (e instanceof Monster) return mobs;
        if (e instanceof AnimalEntity) return animals;
        return false;
    }

    /** Ближайшая подходящая цель в радиусе и в поле зрения (fov — градусы). */
    public static LivingEntity findTarget(MinecraftClient mc, double range, float fov,
                                          boolean players, boolean mobs, boolean animals, boolean walls) {
        LivingEntity best = null;
        double bestDist = Double.MAX_VALUE;
        for (Entity e : mc.world.getEntities()) {
            if (!isValid(mc, e, players, mobs, animals)) continue;
            double dist = mc.player.distanceTo(e);
            if (dist > range || dist >= bestDist) continue;
            if (!walls && !mc.player.canSee(e)) continue;
            if (fov < 360f) {
                float yaw = rotationTo(mc, e)[0];
                if (Math.abs(MathHelper.wrapDegrees(yaw - mc.player.getYaw())) > fov / 2f) continue;
            }
            best = (LivingEntity) e;
            bestDist = dist;
        }
        return best;
    }

    public static float[] rotationTo(MinecraftClient mc, Entity t) {
        return rotationTo(mc, new Vec3d(t.getX(), t.getY() + t.getHeight() * 0.7, t.getZ()));
    }

    public static float[] rotationTo(MinecraftClient mc, Vec3d p) {
        Vec3d eye = mc.player.getEyePos();
        double dx = p.x - eye.x, dy = p.y - eye.y, dz = p.z - eye.z;
        double h = Math.sqrt(dx * dx + dz * dz);
        float yaw = (float) (Math.toDegrees(Math.atan2(dz, dx)) - 90.0);
        float pitch = (float) -Math.toDegrees(Math.atan2(dy, h));
        return new float[]{yaw, pitch};
    }

    /** Плавный поворот не быстрее maxStep градусов за тик. */
    public static void rotateToward(MinecraftClient mc, float yaw, float pitch, float maxStep) {
        float dyaw = MathHelper.wrapDegrees(yaw - mc.player.getYaw());
        float dpitch = pitch - mc.player.getPitch();
        mc.player.setYaw(mc.player.getYaw() + MathHelper.clamp(dyaw, -maxStep, maxStep));
        mc.player.setPitch(MathHelper.clamp(
                mc.player.getPitch() + MathHelper.clamp(dpitch, -maxStep, maxStep), -90f, 90f));
    }

    public static float yawError(MinecraftClient mc, float yaw) {
        return Math.abs(MathHelper.wrapDegrees(yaw - mc.player.getYaw()));
    }
}
