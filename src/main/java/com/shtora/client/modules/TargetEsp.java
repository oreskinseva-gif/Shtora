package com.shtora.client.modules;

import com.shtora.client.Category;
import com.shtora.client.Module;
import com.shtora.client.ModuleManager;
import com.shtora.client.setting.BoolSetting;
import com.shtora.client.setting.ModeSetting;
import com.shtora.client.setting.NumberSetting;
import com.shtora.client.util.CombatUtil;
import com.shtora.client.util.Draw;
import com.shtora.client.util.Projection;
import com.shtora.client.util.Theme;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.entity.LivingEntity;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;

/**
 * TargetESP — подсвечивает текущую цель: уголки/рамка вокруг неё, заливка,
 * полоса здоровья и метка-ромб над головой.
 *
 * Цель берётся у KillAura (если он включён и у него есть цель), иначе —
 * ближайший подходящий игрок/моб в радиусе.
 */
public class TargetEsp extends Module {
    public final ModeSetting style = add(new ModeSetting("Style", "Corners", "Box", "Glow"));
    public final NumberSetting range = add(new NumberSetting("Range", 12, 3, 40, 1));
    public final BoolSetting players = add(new BoolSetting("Players", true));
    public final BoolSetting mobs = add(new BoolSetting("Mobs", true));
    public final BoolSetting animals = add(new BoolSetting("Animals", false));
    public final BoolSetting fill = add(new BoolSetting("Fill", true));
    public final BoolSetting health = add(new BoolSetting("Health bar", true));
    public final BoolSetting marker = add(new BoolSetting("Marker", true));

    private LivingEntity last;
    private float appear;

    public TargetEsp() { super("TargetESP", Category.RENDER); }

    /** Текущая цель: сначала цель KillAura, потом ближайшая подходящая. */
    private LivingEntity target(MinecraftClient mc) {
        LivingEntity t = ModuleManager.killAura == null ? null : ModuleManager.killAura.current;
        if (t != null && t.isAlive() && !t.isRemoved() && mc.player.distanceTo(t) <= range.get()) return t;
        return CombatUtil.findTarget(mc, range.get(), 360f,
                players.get(), mobs.get(), animals.get(), true);
    }

    /** 2D-подсветка цели поверх экрана (видна и сквозь стены). */
    public void render2d(DrawContext ctx) {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.world == null || mc.player == null) return;
        LivingEntity t = target(mc);
        if (t == null) { appear = 0f; last = null; return; }
        if (t != last) { last = t; appear = 0f; }
        appear = Math.min(1f, appear + 0.14f);
        float ea = Draw.ease(appear);

        float td = mc.getRenderTickCounter().getTickDelta(false);
        Vec3d pos = t.getLerpedPos(td);
        Box b = t.getDimensions(t.getPose()).getBoxAt(pos);
        float minX = 1e9f, minY = 1e9f, maxX = -1e9f, maxY = -1e9f;
        for (int i = 0; i < 8; i++) {
            double cx = (i & 1) == 0 ? b.minX : b.maxX;
            double cy = (i & 2) == 0 ? b.minY : b.maxY;
            double cz = (i & 4) == 0 ? b.minZ : b.maxZ;
            float[] p = Projection.project(mc, new Vec3d(cx, cy, cz), td);
            if (p == null) return;
            minX = Math.min(minX, p[0]); maxX = Math.max(maxX, p[0]);
            minY = Math.min(minY, p[1]); maxY = Math.max(maxY, p[1]);
        }
        int x1 = Math.round(minX), y1 = Math.round(minY), x2 = Math.round(maxX), y2 = Math.round(maxY);
        int sw = ctx.getScaledWindowWidth(), sh = ctx.getScaledWindowHeight();
        if (x2 < 0 || y2 < 0 || x1 > sw || y1 > sh) return;
        if (x2 - x1 < 3 || y2 - y1 < 3) return;

        int acc = Theme.accent();
        int soft = Draw.argb(acc, 0.20f * ea);

        // заливка
        if (fill.get()) ctx.fill(x1, y1, x2, y2, soft);

        if (style.is("Box")) {
            Draw.box(ctx, x1 - 1, y1 - 1, x2 + 1, y2 + 1, Draw.argb(0x000000, 0.55f * ea));
            Draw.box(ctx, x1, y1, x2, y2, Draw.argb(acc, ea));
        } else if (style.is("Glow")) {
            Draw.glow(ctx, x1, y1, x2 - x1, y2 - y1, 4, acc, 0.9f * ea);
            Draw.box(ctx, x1, y1, x2, y2, Draw.argb(acc, ea));
        } else {
            // уголки
            int len = Math.max(4, Math.min(12, (x2 - x1) / 4));
            int c = Draw.argb(acc, ea);
            int cs = Draw.argb(0x000000, 0.5f * ea);
            // верх-лево
            Draw.line(ctx, x1 - 1, y1 - 1, x1 + len, y1 - 1, cs);
            Draw.line(ctx, x1 - 1, y1 - 1, x1 - 1, y1 + len, cs);
            Draw.line(ctx, x1, y1, x1 + len, y1, c);
            Draw.line(ctx, x1, y1, x1, y1 + len, c);
            // верх-право
            Draw.line(ctx, x2 - len, y1, x2, y1, c);
            Draw.line(ctx, x2, y1, x2, y1 + len, c);
            // низ-лево
            Draw.line(ctx, x1, y2 - len, x1, y2, c);
            Draw.line(ctx, x1, y2, x1 + len, y2, c);
            // низ-право
            Draw.line(ctx, x2 - len, y2, x2, y2, c);
            Draw.line(ctx, x2, y2 - len, x2, y2, c);
        }

        // полоса здоровья слева
        if (health.get()) {
            float f = MathHelper.clamp(t.getHealth() / Math.max(1f, t.getMaxHealth()), 0f, 1f);
            int hh = y2 - y1;
            ctx.fill(x1 - 5, y1 - 1, x1 - 2, y2 + 1, Draw.argb(0x000000, 0.6f * ea));
            int fh = Math.round(hh * f);
            ctx.fill(x1 - 4, y2 - fh, x1 - 3, y2, Draw.argb(Draw.mix(0xFF5A5A, 0x66FF99, f), ea));
        }

        // метка-ромб над головой (покачивается)
        if (marker.get()) {
            int cx = (x1 + x2) / 2;
            int by = y1 - 8 - Math.round(2f * (float) Math.sin(System.currentTimeMillis() / 260.0));
            int r = 4;
            for (int i = 0; i <= r; i++) {
                ctx.fill(cx - i, by - (r - i), cx - i + 1, by - (r - i) + 1, Draw.argb(acc, ea));
                ctx.fill(cx + i, by - (r - i), cx + i + 1, by - (r - i) + 1, Draw.argb(acc, ea));
                ctx.fill(cx - i, by + (r - i), cx - i + 1, by + (r - i) + 1, Draw.argb(acc, ea));
                ctx.fill(cx + i, by + (r - i), cx + i + 1, by + (r - i) + 1, Draw.argb(acc, ea));
            }
        }
    }
}
