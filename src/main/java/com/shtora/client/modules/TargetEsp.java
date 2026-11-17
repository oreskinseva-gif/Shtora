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
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.util.hit.EntityHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;

/** Подсвечивает только текущую цель (KillAura или прицел). */
public class TargetEsp extends Module {
    public final ModeSetting source = add(new ModeSetting("Source", "KillAura", "Crosshair"));
    public final BoolSetting box = add(new BoolSetting("Box", true));
    public final BoolSetting health = add(new BoolSetting("Health", true));
    public final BoolSetting name = add(new BoolSetting("Name", true));
    public final NumberSetting range = add(new NumberSetting("Range", 6.0, 2.0, 16.0, 0.5));

    public TargetEsp() { super("TargetESP", Category.RENDER); }

    public LivingEntity target(MinecraftClient mc) {
        if (mc == null || mc.player == null || mc.world == null) return null;
        if (source.is("Crosshair")) {
            HitResult hit = mc.crosshairTarget;
            if (hit != null && hit.getType() == HitResult.Type.ENTITY
                    && ((EntityHitResult) hit).getEntity() instanceof LivingEntity le
                    && CombatUtil.isValid(mc, le, true, true, false)) return le;
            return null;
        }
        LivingEntity t = ModuleManager.killAura == null ? null : ModuleManager.killAura.current;
        if (t != null && t.isAlive() && mc.player.distanceTo(t) <= range.get()
                && CombatUtil.isValid(mc, t, true, true, false)) return t;
        return null;
    }

    /** 2D-рамка поверх мира, рисуется из Hud. */
    public void render2d(DrawContext ctx) {
        MinecraftClient mc = MinecraftClient.getInstance();
        LivingEntity t = target(mc);
        if (t == null) return;

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
        if (x2 - x1 < 4 || y2 - y1 < 4) return;
        if (x2 < 0 || y2 < 0 || x1 > ctx.getScaledWindowWidth() || y1 > ctx.getScaledWindowHeight()) return;

        int acc = Theme.accent();
        if (box.get()) {
            Draw.box(ctx, x1 - 2, y1 - 2, x2 + 2, y2 + 2, Draw.argb(0x000000, 0.55f));
            Draw.box(ctx, x1 - 1, y1 - 1, x2 + 1, y2 + 1, Draw.argb(acc, 0.95f));
        }
        TextRenderer tr = mc.textRenderer;
        String label = t.getName().getString();
        if (name.get()) {
            int w = Draw.w(tr, label);
            ctx.fill(x1 - 1, y1 - 12, x1 + w + 3, y1 - 1, Draw.argb(0x000000, 0.65f));
            Draw.text(ctx, tr, label, x1 + 1, y1 - 11, acc, 1f);
        }
        if (health.get()) {
            float f = MathHelper.clamp(t.getHealth() / Math.max(1f, t.getMaxHealth()), 0f, 1f);
            int hx = x2 + 4, hy = y1, hh = y2 - y1;
            ctx.fill(hx - 1, hy - 1, hx + 3, hy + hh + 1, Draw.argb(0x000000, 0.65f));
            int fh = Math.round(hh * f);
            ctx.fill(hx, hy + hh - fh, hx + 2, hy + hh,
                    Draw.argb(Draw.mix(0xFF5A5A, 0x66FF99, f), 1f));
        }
    }
}
