package com.shtora.client.modules;

import com.shtora.client.Category;
import com.shtora.client.Module;
import com.shtora.client.setting.BoolSetting;
import com.shtora.client.setting.ModeSetting;
import com.shtora.client.util.CombatUtil;
import com.shtora.client.util.Draw;
import com.shtora.client.util.Projection;
import com.shtora.client.util.Theme;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderContext;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.VertexRendering;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;

public class Esp extends Module {
    public final ModeSetting mode = add(new ModeSetting("Mode", "2D Box", "3D Box"));
    public final BoolSetting players = add(new BoolSetting("Players", true));
    public final BoolSetting mobs = add(new BoolSetting("Mobs", true));
    public final BoolSetting animals = add(new BoolSetting("Animals", false));
    public final BoolSetting health = add(new BoolSetting("Health bar", true));
    public final ModeSetting color = add(new ModeSetting("Color", "Accent", "Red", "White"));

    public Esp() { super("ESP", Category.RENDER); }

    private int rgb() {
        if (color.is("Red")) return 0xFF5A5A;
        if (color.is("White")) return 0xFFFFFF;
        return Theme.accent();
    }

    /** 2D-рамки поверх экрана: видны и сквозь стены. */
    public void render2d(DrawContext ctx) {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.world == null || mc.player == null) return;
        float td = mc.getRenderTickCounter().getTickDelta(false);
        int base = rgb();
        for (Entity e : mc.world.getEntities()) {
            if (!CombatUtil.isValid(mc, e, players.get(), mobs.get(), animals.get())) continue;
            Vec3d pos = e.getLerpedPos(td);
            Box b = e.getDimensions(e.getPose()).getBoxAt(pos);
            float minX = 1e9f, minY = 1e9f, maxX = -1e9f, maxY = -1e9f;
            boolean ok = true;
            for (int i = 0; i < 8 && ok; i++) {
                double cx = (i & 1) == 0 ? b.minX : b.maxX;
                double cy = (i & 2) == 0 ? b.minY : b.maxY;
                double cz = (i & 4) == 0 ? b.minZ : b.maxZ;
                float[] p = Projection.project(mc, new Vec3d(cx, cy, cz), td);
                if (p == null) { ok = false; break; }
                minX = Math.min(minX, p[0]); maxX = Math.max(maxX, p[0]);
                minY = Math.min(minY, p[1]); maxY = Math.max(maxY, p[1]);
            }
            if (!ok) continue;
            int x1 = Math.round(minX), y1 = Math.round(minY), x2 = Math.round(maxX), y2 = Math.round(maxY);
            if (x2 < 0 || y2 < 0 || x1 > ctx.getScaledWindowWidth() || y1 > ctx.getScaledWindowHeight()) continue;
            if (x2 - x1 < 3 || y2 - y1 < 3) continue;

            int col = (e instanceof PlayerEntity) ? base : Draw.mix(base, 0xFFC04A, 0.6f);
            Draw.box(ctx, x1 - 1, y1 - 1, x2 + 1, y2 + 1, Draw.argb(0x000000, 0.6f));
            Draw.box(ctx, x1, y1, x2, y2, Draw.argb(col, 1f));
            if (health.get() && e instanceof LivingEntity le) {
                float f = MathHelper.clamp(le.getHealth() / Math.max(1f, le.getMaxHealth()), 0f, 1f);
                int hh = y2 - y1;
                ctx.fill(x1 - 5, y1 - 1, x1 - 2, y2 + 1, Draw.argb(0x000000, 0.6f));
                int fh = Math.round(hh * f);
                ctx.fill(x1 - 4, y2 - fh, x1 - 3, y2, Draw.argb(Draw.mix(0xFF5A5A, 0x66FF99, f), 1f));
            }
        }
    }

    /** 3D-рамки в мире (не видны сквозь стены). */
    public void render(WorldRenderContext ctx) {
        if (!mode.is("3D Box")) return;
        MinecraftClient mc = MinecraftClient.getInstance();
        MatrixStack ms = ctx.matrixStack();
        VertexConsumerProvider vcp = ctx.consumers();
        if (mc.world == null || mc.player == null || ms == null || vcp == null) return;

        int c = rgb();
        float r = ((c >> 16) & 255) / 255f, g = ((c >> 8) & 255) / 255f, b = (c & 255) / 255f;
        Vec3d cam = ctx.camera().getPos();
        float td = mc.getRenderTickCounter().getTickDelta(false);
        VertexConsumer vc = vcp.getBuffer(RenderLayer.getLines());

        ms.push();
        ms.translate(-cam.x, -cam.y, -cam.z);
        for (Entity e : mc.world.getEntities()) {
            if (!CombatUtil.isValid(mc, e, players.get(), mobs.get(), animals.get())) continue;
            double ox = MathHelper.lerp(td, e.prevX, e.getX()) - e.getX();
            double oy = MathHelper.lerp(td, e.prevY, e.getY()) - e.getY();
            double oz = MathHelper.lerp(td, e.prevZ, e.getZ()) - e.getZ();
            VertexRendering.drawBox(ms, vc, e.getBoundingBox().offset(ox, oy, oz), r, g, b, 1f);
        }
        ms.pop();
        if (vcp instanceof VertexConsumerProvider.Immediate imm) imm.draw(RenderLayer.getLines());
    }
}
