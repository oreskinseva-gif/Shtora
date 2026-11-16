package com.shtora.client.modules;

import com.shtora.client.Category;
import com.shtora.client.Module;
import com.shtora.client.setting.BoolSetting;
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
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import java.util.Locale;

public class NameTags extends Module {
    public final NumberSetting scale = add(new NumberSetting("Scale", 1.0, 0.6, 1.6, 0.1));
    public final BoolSetting mobs = add(new BoolSetting("Mobs", false));
    public final BoolSetting distance = add(new BoolSetting("Distance", true));

    public NameTags() { super("NameTags", Category.RENDER); }

    public void render2d(DrawContext ctx) {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.world == null || mc.player == null) return;
        TextRenderer tr = mc.textRenderer;
        float td = mc.getRenderTickCounter().getTickDelta(false);
        int acc = Theme.accent();
        for (Entity e : mc.world.getEntities()) {
            if (!CombatUtil.isValid(mc, e, true, mobs.get(), false)) continue;
            LivingEntity le = (LivingEntity) e;
            Vec3d head = e.getLerpedPos(td).add(0, e.getHeight() + 0.85, 0);
            float[] p = Projection.project(mc, head, td);
            if (p == null) continue;

            String name = e.getName().getString();
            String hp = String.format(Locale.ROOT, "%.0f", le.getHealth());
            String dist = distance.get() ? String.format(Locale.ROOT, "  %.0fm", mc.player.distanceTo(e)) : "";
            int w = Draw.w(tr, name) + Draw.w(tr, "  " + hp) + Draw.w(tr, dist) + 10;
            float f = MathHelper.clamp(le.getHealth() / Math.max(1f, le.getMaxHealth()), 0f, 1f);

            var ms = ctx.getMatrices();
            ms.push();
            ms.translate(p[0], p[1], 0f);
            float sc = (float) scale.get();
            ms.scale(sc, sc, 1f);
            Draw.rrect(ctx, -w / 2, -14, w, 14, 5, Draw.argb(0x101015, 0.78f));
            Draw.rrect(ctx, -w / 2 + 4, -3, Math.max(2, Math.round((w - 8) * f)), 2, 1,
                    Draw.argb(Draw.mix(0xFF5A5A, acc, f), 1f));
            int x = -w / 2 + 5;
            Draw.text(ctx, tr, name, x, -11, 0xFFFFFF, 1f);
            x += Draw.w(tr, name);
            Draw.text(ctx, tr, "  " + hp, x, -11, Draw.mix(0xFF5A5A, acc, f), 1f);
            x += Draw.w(tr, "  " + hp);
            if (!dist.isEmpty()) Draw.text(ctx, tr, dist, x, -11, 0x8A8A96, 1f);
            ms.pop();
        }
    }
}
