package com.shtora.client.util;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayNetworkHandler;
import net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket;
import net.minecraft.util.math.MathHelper;

public final class Rotations {
    private static boolean hasLast;
    private static float lastYaw;

    private Rotations() {}

    /**
     * Тихий поворот: камера стоит на месте, сервер получает новый угол,
     * а тело и голова модели плавно (с интерполяцией между тиками) поворачиваются.
     */
    public static void sendSilent(MinecraftClient mc, float yaw, float pitch) {
        ClientPlayNetworkHandler h = mc.getNetworkHandler();
        if (h == null || mc.player == null) return;
        pitch = MathHelper.clamp(pitch, -90f, 90f);
        h.sendPacket(new PlayerMoveC2SPacket.LookAndOnGround(
                yaw, pitch, mc.player.isOnGround(), mc.player.horizontalCollision));
        float from = hasLast ? lastYaw : mc.player.bodyYaw;
        mc.player.prevBodyYaw = from;
        mc.player.bodyYaw = yaw;
        mc.player.prevHeadYaw = from;
        mc.player.headYaw = yaw;
        lastYaw = yaw;
        hasLast = true;
    }

    /** Вернуть серверу реальный угол камеры. */
    public static void restore(MinecraftClient mc) {
        if (mc.player != null) sendSilent(mc, mc.player.getYaw(), mc.player.getPitch());
        hasLast = false;
    }
}
