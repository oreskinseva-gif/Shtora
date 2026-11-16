package com.shtora.client.util;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.Camera;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import org.joml.Quaternionf;
import org.joml.Vector3f;

/** Перевод мировых координат в координаты экрана (для 2D ESP, трейсеров и неймтегов). */
public final class Projection {
    private static float lastMul = 1f, mul = 1f;

    private Projection() {}

    /** Повторяет сглаживание FOV от скорости (спринт и т.п.), вызывать раз в тик. */
    public static void tick(MinecraftClient mc) {
        lastMul = mul;
        if (mc.player == null) { mul = 1f; return; }
        float walk = mc.player.getAbilities().getWalkSpeed();
        float f = 1f;
        if (mc.player.getAbilities().flying) f *= 1.1f;
        if (walk > 0f) f *= ((mc.player.getMovementSpeed() / walk) + 1f) / 2f;
        if (Float.isNaN(f) || Float.isInfinite(f)) f = 1f;
        float scale = mc.options.getFovEffectScale().getValue().floatValue();
        f = MathHelper.lerp(scale, 1f, f);
        mul += (f - mul) * 0.5f;
        mul = MathHelper.clamp(mul, 0.1f, 1.5f);
    }

    /** {x, y} в координатах окна (как у DrawContext) или null, если точка позади камеры. */
    public static float[] project(MinecraftClient mc, Vec3d world, float tickDelta) {
        Camera cam = mc.gameRenderer.getCamera();
        Vec3d c = cam.getPos();
        Vector3f v = new Vector3f((float) (world.x - c.x), (float) (world.y - c.y), (float) (world.z - c.z));
        v.rotate(new Quaternionf(cam.getRotation()).conjugate());
        float depth = -v.z;
        if (depth < 0.05f) return null;
        double fov = mc.options.getFov().getValue() * MathHelper.lerp(tickDelta, lastMul, mul);
        float tan = (float) Math.tan(Math.toRadians(fov) / 2.0);
        float aspect = (float) mc.getWindow().getFramebufferWidth() / (float) mc.getWindow().getFramebufferHeight();
        float nx = v.x / (depth * tan * aspect);
        float ny = v.y / (depth * tan);
        return new float[]{
                (nx * 0.5f + 0.5f) * mc.getWindow().getScaledWidth(),
                (0.5f - ny * 0.5f) * mc.getWindow().getScaledHeight()};
    }
}
