package com.shtora.client.modules;

import com.shtora.client.Category;
import com.shtora.client.Module;
import com.shtora.client.setting.BoolSetting;
import com.shtora.client.setting.ModeSetting;
import com.shtora.client.util.CombatUtil;
import com.shtora.client.util.Draw;
import com.shtora.client.util.Projection;
import com.shtora.client.util.Theme;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.entity.Entity;
import net.minecraft.util.math.Vec3d;

public class Tracers extends Module {
    public final ModeSetting from = add(new ModeSetting("From", "Bottom", "Center"));
    public final BoolSetting players = add(new BoolSetting("Players", true));
    public final BoolSetting mobs = add(new BoolSetting("Mobs", false));

    public Tracers() { super("Tracers", Category.RENDER); }

    public void render2d(DrawContext ctx) {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.world == null || mc.player == null) return;
        float td = mc.getRenderTickCounter().getTickDelta(false);
        int sw = ctx.getScaledWindowWidth(), sh = ctx.getScaledWindowHeight();
        int sx = sw / 2, sy = from.is("Bottom") ? sh : sh / 2;
        int acc = Theme.accent();
        int count = 0;
        for (Entity e : mc.world.getEntities()) {
            if (count >= 12) break;
            if (!CombatUtil.isValid(mc, e, players.get(), mobs.get(), false)) continue;
            Vec3d pos = e.getLerpedPos(td).add(0, e.getHeight() * 0.5, 0);
            float[] p = Projection.project(mc, pos, td);
            if (p == null) continue;
            int x = Math.max(0, Math.min(sw, Math.round(p[0])));
            int y = Math.max(0, Math.min(sh, Math.round(p[1])));
            double d = mc.player.distanceTo(e);
            int col = Draw.mix(acc, 0xFF5A5A, (float) Math.max(0, Math.min(1, 1.0 - d / 30.0)));
            Draw.line(ctx, sx, sy, x, y, Draw.argb(col, 0.9f));
            count++;
        }
    }
}
