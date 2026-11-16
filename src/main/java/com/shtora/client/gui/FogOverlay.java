package com.shtora.client.gui;

import com.shtora.client.ModuleManager;
import com.shtora.client.modules.CustomFog;
import com.shtora.client.util.Draw;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;

/**
 * Атмосфера «CustomFog»: экранный слой поверх мира плюс частицы.
 *
 * Всё рисуется обычными прямоугольниками и градиентами, без шейдеров, поэтому
 * выглядит одинаково на любом телефоне и не просаживает fps.
 *
 * Важно: каждый режим занимает СВОЮ часть экрана и мягко сходит на нет, а не
 * заливает весь кадр — иначе картинка «растекается» и мир плохо видно.
 */
public final class FogOverlay {
    private FogOverlay() {}

    private static long start = System.nanoTime();

    public static void render(DrawContext ctx) {
        CustomFog f = ModuleManager.customFog;
        if (f == null || !f.isEnabled()) return;
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.player == null || mc.world == null) return;

        int w = ctx.getScaledWindowWidth();
        int h = ctx.getScaledWindowHeight();
        if (w <= 0 || h <= 0) return;

        float t = (System.nanoTime() - start) / 1e9f * (float) f.speed.get();
        float op = (float) f.opacity.get();
        float dn = (float) f.density.get();
        switch (f.mode.get()) {
            case "Lava" -> lava(ctx, w, h, t, op, dn);
            case "Water" -> water(ctx, w, h, t, op, dn);
            default -> aurora(ctx, w, h, t, op, dn);
        }
    }

    // ------------------------------------------------------------------ Aurora
    private static void aurora(DrawContext ctx, int w, int h, float t, float op, float dn) {
        int sky = Math.max(8, (int) (h * 0.60f));   // сияние живёт только в небе

        // лёгкое затемнение неба — к горизонту полностью сходит на нет
        Draw.vgrad(ctx, 0, 0, w, sky, 0x03060E, 0x000000, op * 0.30f);

        int step = 6;
        for (int x = 0; x < w; x += step) {
            float u = x / (float) w;
            float wave = (float) Math.sin(u * 5.5f + t * 0.60f) * 0.6f
                    + (float) Math.sin(u * 11.0f - t * 0.42f) * 0.4f;
            int top = (int) (h * (0.02f + 0.16f * (0.5f + 0.5f * wave)));
            int bot = Math.min(sky, top + Math.max(10, (int) (h * 0.26f)));
            float hue = 0.36f + 0.20f * (float) Math.sin(u * 3.2f + t * 0.30f);
            int col = Draw.hsb(hue, 0.72f, 1f);
            int a0 = (int) Math.round(op * 0.38f * dn * 255f);
            ctx.fillGradient(x, top, Math.min(w, x + step), bot, (a0 << 24) | (col & 0xFFFFFF), 0);
        }

        // светящаяся «шапка» у горизонта, узкая полоса
        int y0 = (int) (h * 0.54f);
        Draw.vgrad(ctx, 0, y0, w, Math.max(4, (int) (h * 0.14f)),
                Draw.hsb(0.50f, 0.65f, 0.70f), 0x000000, op * 0.16f);
    }

    // -------------------------------------------------------------------- Lava
    private static void lava(DrawContext ctx, int w, int h, float t, float op, float dn) {
        int y0 = (int) (h * 0.62f);                 // зарево поднимается только снизу
        Draw.vgrad(ctx, 0, y0, w, h - y0, 0x000000, 0xC24A08, op * 0.42f);

        // искры падают сверху, но разгораются ближе к низу
        int n = (int) (30 + 60 * dn);
        for (int i = 0; i < n; i++) {
            float sp = 70f + 150f * rnd(i, 1);
            float y = ((t * sp + rnd(i, 2) * (h + 60)) % (h + 60)) - 30f;
            if (y > h) continue;
            float x = rnd(i, 3) * w + (float) Math.sin(t * 0.9f + i * 1.7f) * 10f;
            float k = Draw.clamp(y / h);            // 0 сверху, 1 снизу
            int col = Draw.mix(0xFFD98A, 0xFF5A10, rnd(i, 5));
            int size = 1 + (int) (rnd(i, 4) * 2f);
            Draw.dot(ctx, (int) x, (int) y, size, col, op * (0.25f + 0.55f * k) * (0.45f + 0.55f * rnd(i, 6)));
            ctx.fill((int) x, (int) y - 6 - size * 2, (int) x + 1, (int) y,
                    Draw.argb(col, op * 0.16f * k * rnd(i, 7)));
        }
    }

    // ------------------------------------------------------------------- Water
    private static void water(DrawContext ctx, int w, int h, float t, float op, float dn) {
        // синева только у верхней кромки, чтобы кадр не «затягивало»
        Draw.vgrad(ctx, 0, 0, w, Math.max(6, (int) (h * 0.28f)), 0x123E5E, 0x000000, op * 0.22f);

        int n = (int) (40 + 60 * dn);
        for (int i = 0; i < n; i++) {
            float sp = 150f + 220f * rnd(i, 1);
            float y = ((t * sp + rnd(i, 2) * (h + 80)) % (h + 80)) - 40f;
            float x = rnd(i, 3) * w + (float) Math.sin(t * 1.4f + i) * 3.5f;
            float len = 5f + 12f * rnd(i, 4);
            int a = (int) (op * (80 + 90 * rnd(i, 6)));
            ctx.fill((int) x, (int) y, (int) x + 1, (int) (y + len), (a << 24) | 0xBFE9FF);
            ctx.fill((int) x, (int) (y + len), (int) x + 1, (int) (y + len + 2),
                    (Math.min(255, a + 70) << 24) | 0xEAF8FF);
        }

        // круги на «воде» у самого низа
        for (int i = 0; i < 7; i++) {
            float ph = (t * (0.5f + 0.35f * rnd(i, 8)) + rnd(i, 9)) % 1f;
            int cx = (int) (rnd(i, 10) * w);
            int cy = (int) (h * (0.88f + 0.10f * rnd(i, 11)));
            int rr = (int) (2 + ph * 11);
            Draw.outline(ctx, cx - rr, cy - rr / 3, rr * 2, Math.max(2, rr * 2 / 3), rr,
                    Draw.argb(0xCFEFFF, op * 0.26f * (1f - ph)));
        }
    }

    /** Устойчивое псевдослучайное число 0..1 — частицы не «дрожат» между кадрами. */
    private static float rnd(int i, int salt) {
        int x = i * 374761393 + salt * 668265263;
        x = (x ^ (x >> 13)) * 1274126177;
        x ^= (x >> 16);
        return (x & 0x7FFFFFFF) / (float) 0x7FFFFFFF;
    }
}
