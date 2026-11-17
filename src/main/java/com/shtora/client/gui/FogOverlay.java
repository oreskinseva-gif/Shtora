package com.shtora.client.gui;

import com.shtora.client.ModuleManager;
import com.shtora.client.modules.CustomFog;
import com.shtora.client.util.Draw;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;

/**
 * Визуальный дым «CustomFog».
 *
 * Поверх мира рисуется мягкий градиент (цвет начала → цвет конца) и множество
 * дрейфующих «клубов» из мягких кругов. Клубы рождаются у кромки начала и уходят
 * к кромке конца, по пути меняя цвет от start к end. Всё — обычные прямоугольники,
 * без шейдеров, поэтому fps на телефоне не страдает.
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
        float sz = (float) f.size.get();
        float hf = (float) f.height.get();
        int cStart = f.start.rgb();
        int cEnd = f.end.rgb();
        boolean rise = f.direction.is("Rise");

        int region = Math.max(24, Math.round(h * hf));
        int y0 = rise ? h - region : 0;
        int y1 = rise ? h : region;

        // основной градиент: у начала цвет start, у конца — end
        if (rise) Draw.vgrad(ctx, 0, y0, w, region, cEnd, cStart, op * 0.32f);
        else Draw.vgrad(ctx, 0, y0, w, region, cStart, cEnd, op * 0.32f);

        int scale = Math.min(w, h);
        int n = (int) (22 + 46 * dn);
        for (int i = 0; i < n; i++) {
            float sp = 0.05f + 0.16f * rnd(i, 1);                 // скорость подъёма/оседания
            float p = (t * sp + rnd(i, 2)) % 1f;                   // 0 — начало, 1 — конец
            float y = rise ? (h - p * region) : (p * region);
            float x = rnd(i, 3) * w + (float) Math.sin(t * 0.35f + i * 1.7f) * (10f + 14f * rnd(i, 7));

            float r = (0.10f + 0.20f * rnd(i, 4)) * scale * sz;
            // у самой кромки клуб мягко проявляется и мягко исчезает у другой
            float edge = Math.min(1f, Math.min(p, 1f - p) * 4f);
            float a = op * (0.10f + 0.16f * rnd(i, 6)) * edge;
            if (a < 0.01f) continue;

            int col = Draw.mix(cStart, cEnd, p);
            softCircle(ctx, Math.round(x), Math.round(y), Math.max(3, Math.round(r)), col, a);
        }
    }

    /** Мягкий круг из нескольких вложенных скруглённых прямоугольников. */
    private static void softCircle(DrawContext ctx, int cx, int cy, int r, int rgb, float a) {
        int steps = 4;
        for (int i = steps; i >= 1; i--) {
            int rr = Math.max(1, r * i / steps);
            Draw.rrect(ctx, cx - rr, cy - rr, rr * 2, rr * 2, rr, Draw.argb(rgb, a * 0.26f));
        }
    }

    /** Устойчивое псевдослучайное число 0..1 — клубы не «дрожат» между кадрами. */
    private static float rnd(int i, int salt) {
        int x = i * 374761393 + salt * 668265263;
        x = (x ^ (x >> 13)) * 1274126177;
        x ^= (x >> 16);
        return (x & 0x7FFFFFFF) / (float) 0x7FFFFFFF;
    }
}
