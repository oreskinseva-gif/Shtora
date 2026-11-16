package com.shtora.client.gui;

import com.shtora.client.ShtoraClient;
import com.shtora.client.Module;
import com.shtora.client.ModuleManager;
import com.shtora.client.modules.HudModule;
import com.shtora.client.modules.Interface;
import com.shtora.client.util.Anim;
import com.shtora.client.util.CombatUtil;
import com.shtora.client.util.Draw;
import com.shtora.client.util.Theme;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.item.ItemStack;
import net.minecraft.util.hit.EntityHitResult;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

public class Hud {
    private static long last = System.nanoTime();
    private static boolean lmbWas, rmbWas;
    private static final ArrayDeque<Long> lClicks = new ArrayDeque<>(), rClicks = new ArrayDeque<>();
    private static final Anim hpAnim = new Anim(1f);

    public static void render(DrawContext ctx) {
        MinecraftClient mc = MinecraftClient.getInstance();
        long now = System.nanoTime();
        float dt = Math.min(0.1f, (now - last) / 1e9f);
        last = now;
        if (mc.player == null || mc.options.hudHidden) return;

        trackClicks(mc);
        TextRenderer tr = mc.textRenderer;
        Interface ui = ModuleManager.ui;
        int acc = Theme.accent();
        int sw = ctx.getScaledWindowWidth(), sh = ctx.getScaledWindowHeight();

        // 2D-оверлеи поверх мира
        if (ModuleManager.esp.isEnabled() && ModuleManager.esp.mode.is("2D Box")) safe(ModuleManager.esp, () -> ModuleManager.esp.render2d(ctx));
        if (ModuleManager.tracers.isEnabled()) safe(ModuleManager.tracers, () -> ModuleManager.tracers.render2d(ctx));
        if (ModuleManager.nameTags.isEnabled()) safe(ModuleManager.nameTags, () -> ModuleManager.nameTags.render2d(ctx));

        if (ui.isEnabled() && ui.watermark.get()) safe(ui, () -> watermark(ctx, tr, mc, acc));
        safe(ui, () -> arrayList(ctx, tr, dt, sw));

        if (ModuleManager.radar.isEnabled()) safe(ModuleManager.radar, () -> radar(ctx, mc, acc));
        if (ModuleManager.potionHud.isEnabled()) safe(ModuleManager.potionHud, () -> potions(ctx, tr, mc, acc, sh));
        if (ModuleManager.armorHud.isEnabled()) safe(ModuleManager.armorHud, () -> armor(ctx, tr, mc, sw, sh));
        if (ModuleManager.keystrokes.isEnabled()) safe(ModuleManager.keystrokes, () -> keystrokes(ctx, tr, mc, acc, sw, sh));
        if (ModuleManager.coords.isEnabled()) safe(ModuleManager.coords, () -> coords(ctx, tr, mc, acc, sw, sh));
        if (ModuleManager.invHud.isEnabled()) safe(ModuleManager.invHud, () -> inventory(ctx, tr, mc, sw));
        if (ModuleManager.infoHud.isEnabled()) safe(ModuleManager.infoHud, () -> info(ctx, tr, mc, acc, sw));
        if (ModuleManager.itemCounter.isEnabled()) safe(ModuleManager.itemCounter, () -> items(ctx, tr, mc, sw, sh));
        if (ModuleManager.targetHud.isEnabled()) safe(ModuleManager.targetHud, () -> target(ctx, tr, mc, acc, sw, sh, dt));
    }

    /** Если один виджет падает с ошибкой, он выключается, а остальные продолжают работать. */
    private static void safe(Module m, Runnable r) {
        try {
            r.run();
        } catch (Throwable t) {
            ShtoraClient.fail(MinecraftClient.getInstance(), m, t);
        }
    }

    // ---------------------------------------------------------------- водяной знак и список
    private static void watermark(DrawContext ctx, TextRenderer tr, MinecraftClient mc, int acc) {
        String brand = "SHTORA";
        String rest = " client  |  " + mc.getSession().getUsername() + "  |  " + mc.getCurrentFps() + " fps";
        int w = 20 + Draw.w(tr, brand) + Draw.w(tr, rest) + 6;
        Draw.rrect(ctx, 4, 4, w, 17, 2, Draw.argb(0x101015, 0.82f));
        ctx.fill(4, 4, 6, 21, Draw.argb(acc, 1f));
        Draw.text(ctx, tr, brand, 10, 9, acc, 1f);
        Draw.text(ctx, tr, rest, 10 + Draw.w(tr, brand), 9, 0xB4B4C0, 1f);
    }

    private static void arrayList(DrawContext ctx, TextRenderer tr, float dt, int sw) {
        Interface ui = ModuleManager.ui;
        List<Module> shown = new ArrayList<>();
        for (Module m : ModuleManager.modules) {
            boolean want = m.isEnabled() && ui.isEnabled() && ui.arrayList.get()
                    && m != ui && !(m instanceof HudModule);
            m.listAnim.to(want ? 1f : 0f, 12f, dt);
            if (m.listAnim.v > 0.01f) shown.add(m);
        }
        shown.sort(Comparator.comparingInt((Module m) -> -Draw.w(tr, m.name)));
        int y = 4, i = 0;
        for (Module m : shown) {
            float a = Draw.ease(m.listAnim.v);
            int w = Draw.w(tr, m.name) + 10;
            int x = sw - w - 4 + Math.round((1f - a) * (w + 6));
            int col = Theme.accent(i * 0.015f);
            Draw.rrect(ctx, x, y, w, 11, 3, Draw.argb(0x101015, 0.78f * a));
            Draw.rrect(ctx, x + w - 2, y + 1, 2, 9, 1, Draw.argb(col, a));
            Draw.text(ctx, tr, m.name, x + 4, y + 2, col, a);
            y += Math.round(12 * a);
            i++;
        }
    }

    // ---------------------------------------------------------------- виджеты
    private static void trackClicks(MinecraftClient mc) {
        long ms = System.currentTimeMillis();
        boolean l = mc.options.attackKey.isPressed(), r = mc.options.useKey.isPressed();
        if (l && !lmbWas) lClicks.add(ms);
        if (r && !rmbWas) rClicks.add(ms);
        lmbWas = l; rmbWas = r;
        while (!lClicks.isEmpty() && ms - lClicks.peek() > 1000) lClicks.poll();
        while (!rClicks.isEmpty() && ms - rClicks.peek() > 1000) rClicks.poll();
    }

    private static void key(DrawContext ctx, TextRenderer tr, int x, int y, int w, int h, String s, boolean on, int acc) {
        Draw.rrect(ctx, x, y, w, h, 4, Draw.argb(on ? Draw.mix(0x101015, acc, 0.55f) : 0x101015, 0.82f));
        Draw.text(ctx, tr, s, x + (w - Draw.w(tr, s)) / 2, y + (h - 8) / 2, on ? 0xFFFFFF : 0xB4B4C0, 1f);
    }

    private static void keystrokes(DrawContext ctx, TextRenderer tr, MinecraftClient mc, int acc, int sw, int sh) {
        int x = sw - 72, y = sh - 92;
        key(ctx, tr, x + 22, y, 20, 20, "W", mc.options.forwardKey.isPressed(), acc);
        key(ctx, tr, x, y + 22, 20, 20, "A", mc.options.leftKey.isPressed(), acc);
        key(ctx, tr, x + 22, y + 22, 20, 20, "S", mc.options.backKey.isPressed(), acc);
        key(ctx, tr, x + 44, y + 22, 20, 20, "D", mc.options.rightKey.isPressed(), acc);
        key(ctx, tr, x, y + 44, 31, 16, "L " + lClicks.size(), mc.options.attackKey.isPressed(), acc);
        key(ctx, tr, x + 33, y + 44, 31, 16, "R " + rClicks.size(), mc.options.useKey.isPressed(), acc);
    }

    private static void coords(DrawContext ctx, TextRenderer tr, MinecraftClient mc, int acc, int sw, int sh) {
        String s = String.format(Locale.ROOT, "XYZ %.0f %.0f %.0f", mc.player.getX(), mc.player.getY(), mc.player.getZ());
        if (ModuleManager.showBps.get()) {
            double bps = Math.hypot(mc.player.getX() - mc.player.prevX, mc.player.getZ() - mc.player.prevZ) * 20.0;
            s += String.format(Locale.ROOT, "  |  %.1f b/s", bps);
        }
        int w = Draw.w(tr, s) + 10;
        int x = sw - w - 4, y = sh - 17;
        Draw.rrect(ctx, x, y, w, 13, 4, Draw.argb(0x101015, 0.82f));
        Draw.rrect(ctx, x, y + 2, 2, 9, 1, Draw.argb(acc, 1f));
        Draw.text(ctx, tr, s, x + 6, y + 3, 0xB4B4C0, 1f);
    }

    private static void armor(DrawContext ctx, TextRenderer tr, MinecraftClient mc, int sw, int sh) {
        EquipmentSlot[] slots = {EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET};
        int x = sw - 24, y = sh / 2 - 38;
        for (EquipmentSlot sl : slots) {
            ItemStack st = mc.player.getEquippedStack(sl);
            if (st.isEmpty()) continue;
            Draw.rrect(ctx, x - 2, y - 1, 20, 19, 4, Draw.argb(0x101015, 0.7f));
            ctx.drawItem(st, x, y);
            ctx.drawStackOverlay(tr, st, x, y);
            y += 21;
        }
    }

    private static void potions(DrawContext ctx, TextRenderer tr, MinecraftClient mc, int acc, int sh) {
        int y = sh / 2 - 30;
        for (StatusEffectInstance e : mc.player.getStatusEffects()) {
            String name = e.getEffectType().value().getName().getString() + " " + (e.getAmplifier() + 1);
            String time = e.isInfinite() ? "inf"
                    : String.format(Locale.ROOT, "%d:%02d", e.getDuration() / 1200, (e.getDuration() / 20) % 60);
            String s = name + "  " + time;
            int w = Draw.w(tr, s) + 10;
            Draw.rrect(ctx, 4, y, w, 13, 4, Draw.argb(0x101015, 0.82f));
            Draw.rrect(ctx, 4, y + 2, 2, 9, 1, Draw.argb(acc, 1f));
            Draw.text(ctx, tr, name, 10, y + 3, 0xFFFFFF, 1f);
            Draw.text(ctx, tr, time, 10 + Draw.w(tr, name + "  "), y + 3, acc, 1f);
            y += 15;
        }
    }

    private static void radar(DrawContext ctx, MinecraftClient mc, int acc) {
        int size = 72, x = 4, y = 26, half = size / 2, cx = x + half, cy = y + half;
        Draw.rrect(ctx, x, y, size, size, 6, Draw.argb(0x101015, 0.78f));
        ctx.fill(cx, y + 3, cx + 1, y + size - 3, Draw.argb(0xFFFFFF, 0.07f));
        ctx.fill(x + 3, cy, x + size - 3, cy + 1, Draw.argb(0xFFFFFF, 0.07f));
        Draw.rrect(ctx, cx - 1, cy - 1, 3, 3, 1, Draw.argb(acc, 1f));

        double range = ModuleManager.radarRange.get();
        double yaw = Math.toRadians(mc.player.getYaw());
        double sin = Math.sin(yaw), cos = Math.cos(yaw);
        for (Entity e : mc.world.getEntities()) {
            if (!CombatUtil.isValid(mc, e, true, ModuleManager.radarMobs.get(), false)) continue;
            double dx = e.getX() - mc.player.getX(), dz = e.getZ() - mc.player.getZ();
            double fwd = -dx * sin + dz * cos;
            double right = -dx * cos - dz * sin;
            double d = Math.hypot(fwd, right);
            if (d > range) continue;
            double k = (half - 5) / range;
            int px = cx + (int) Math.round(right * k);
            int py = cy - (int) Math.round(fwd * k);
            boolean player = e instanceof net.minecraft.entity.player.PlayerEntity;
            ctx.fill(px - 1, py - 1, px + 2, py + 2, Draw.argb(player ? 0xFF5A5A : 0xFFC04A, 1f));
        }
    }

    private static void inventory(DrawContext ctx, TextRenderer tr, MinecraftClient mc, int sw) {
        int x = sw / 2 - 81, y = 22;
        Draw.rrect(ctx, x - 3, y - 3, 168, 60, 7, Draw.argb(0x101015, 0.6f));
        Draw.outline(ctx, x - 3, y - 3, 168, 60, 7, Draw.argb(0xFFFFFF, 0.12f));
        for (int i = 9; i < 36; i++) {
            ItemStack st = mc.player.getInventory().getStack(i);
            if (st.isEmpty()) continue;
            int cx = x + ((i - 9) % 9) * 18, cy = y + ((i - 9) / 9) * 18;
            ctx.drawItem(st, cx, cy);
            ctx.drawStackOverlay(tr, st, cx, cy);
        }
    }

    private static void info(DrawContext ctx, TextRenderer tr, MinecraftClient mc, int acc, int sw) {
        int ping = 0;
        var h = mc.getNetworkHandler();
        if (h != null) {
            var e = h.getPlayerListEntry(mc.player.getUuid());
            if (e != null) ping = e.getLatency();
        }
        String time = java.time.LocalTime.now().format(java.time.format.DateTimeFormatter.ofPattern("HH:mm"));
        String s = mc.getCurrentFps() + " fps   " + ping + " ms   " + time;
        int w = Draw.w(tr, s) + 12;
        int x = (sw - w) / 2, y = 4;
        Draw.rrect(ctx, x, y, w, 15, 7, Draw.argb(0x101015, 0.7f));
        Draw.outline(ctx, x, y, w, 15, 7, Draw.argb(0xFFFFFF, 0.14f));
        Draw.text(ctx, tr, s, x + 6, y + 4, 0xE0E0EA, 1f);
    }

    private static void items(DrawContext ctx, TextRenderer tr, MinecraftClient mc, int sw, int sh) {
        net.minecraft.item.Item[] list = {
                net.minecraft.item.Items.TOTEM_OF_UNDYING, net.minecraft.item.Items.ENCHANTED_GOLDEN_APPLE,
                net.minecraft.item.Items.GOLDEN_APPLE, net.minecraft.item.Items.ENDER_PEARL};
        int x = sw / 2 + 100, y = sh - 24;
        for (net.minecraft.item.Item it : list) {
            int n = mc.player.getInventory().count(it);
            ItemStack st = new ItemStack(it);
            ctx.drawItem(st, x, y);
            String c = String.valueOf(n);
            Draw.text(ctx, tr, c, x + 17 - Draw.w(tr, c), y + 9, n > 0 ? 0xFFFFFF : 0xFF5A5A, 1f);
            x += 22;
        }
    }

    // ---------------------------------------------------------------- TargetHUD
    private static final Anim tAppear = new Anim(0), tHp = new Anim(1), tGhost = new Anim(1),
            tHurt = new Anim(0), tNum = new Anim(20);
    private static int tId = -1;
    private static String tName = "";
    private static float tMax = 20f, tAbs, tDist;

    private static LivingEntity pickTarget(MinecraftClient mc) {
        String mode = ModuleManager.thMode.get();
        if (mode.equals("KillAura")) {
            LivingEntity ka = ModuleManager.killAura.current;
            return (ModuleManager.killAura.isEnabled() && ka != null && ka.isAlive()) ? ka : null;
        }
        if (mode.equals("Crosshair")) {
            if (mc.crosshairTarget instanceof EntityHitResult hit && hit.getEntity() instanceof LivingEntity le
                    && le.isAlive()) return le;
            return null;
        }
        double radius = ModuleManager.thRadius.get();
        LivingEntity best = null;
        double bd = Double.MAX_VALUE;
        for (Entity e : mc.world.getEntities()) {
            if (!CombatUtil.isValid(mc, e, ModuleManager.thPlayers.get(), ModuleManager.thMobs.get(), false)) continue;
            double d = mc.player.distanceTo(e);
            if (d <= radius && d < bd) { bd = d; best = (LivingEntity) e; }
        }
        return best;
    }

    private static void target(DrawContext ctx, TextRenderer tr, MinecraftClient mc, int acc, int sw, int sh, float dt) {
        LivingEntity t = pickTarget(mc);
        if (t != null) {
            float max = Math.max(1f, t.getMaxHealth());
            float frac = Draw.clamp(t.getHealth() / max);
            if (t.getId() != tId) { // новая цель: плавно «выезжает» заново
                tId = t.getId();
                tHp.v = frac; tGhost.v = frac; tNum.v = t.getHealth(); tHurt.v = 0f;
                tAppear.v = Math.min(tAppear.v, 0.25f);
            }
            tName = t.getName().getString();
            tMax = max;
            tAbs = t.getAbsorptionAmount();
            tDist = mc.player.distanceTo(t);
            tHp.to(frac, 14f, dt);
            if (tGhost.v < frac) tGhost.v = frac; else tGhost.to(frac, 3f, dt);
            tNum.to(t.getHealth(), 10f, dt);
            tHurt.to(t.hurtTime > 0 ? 1f : 0f, t.hurtTime > 0 ? 30f : 7f, dt);
            tAppear.to(1f, 10f, dt);
        } else {
            tAppear.to(0f, 8f, dt);
            tHurt.to(0f, 7f, dt);
        }
        if (tAppear.v < 0.02f) return;

        float a = Draw.ease(tAppear.v);
        int w = 132, h = 38;
        int x = sw / 2 + 18 + Math.round((1f - a) * 18f);
        int y = sh / 2 + 12;
        float pulse = 0.5f + 0.5f * (float) Math.sin(System.currentTimeMillis() / 350.0);
        float hurt = tHurt.v;

        Draw.rrect(ctx, x - 1, y - 1, w + 2, h + 2, 7,
                Draw.argb(Draw.mix(acc, 0xFF5A5A, hurt), (0.20f + 0.20f * pulse + 0.45f * hurt) * a));
        Draw.rrect(ctx, x, y, w, h, 6, Draw.argb(0x101015, 0.88f * a));
        Draw.rrect(ctx, x, y + 6, 2, h - 12, 1, Draw.argb(acc, (0.55f + 0.45f * pulse) * a));

        Draw.text(ctx, tr, Draw.trim(tr, tName, w - 58), x + 9, y + 6, 0xFFFFFF, a);
        String hs = String.format(Locale.ROOT, "%.1f", tNum.v);
        Draw.text(ctx, tr, hs, x + w - 8 - Draw.w(tr, hs), y + 6, Draw.mix(0xFF5A5A, acc, tHp.v), a);

        int bx = x + 9, by = y + 19, bw = w - 18;
        Draw.rrect(ctx, bx, by, bw, 5, 2, Draw.argb(0x2C2C36, a));
        int gw = Math.max(2, Math.round(tGhost.v * bw));
        Draw.rrect(ctx, bx, by, gw, 5, 2, Draw.argb(0xFFB04A, 0.65f * a));
        int fw = Math.max(2, Math.round(tHp.v * bw));
        Draw.rrect(ctx, bx, by, fw, 5, 2, Draw.argb(Draw.mix(0xFF5A5A, acc, tHp.v), a));
        if (tAbs > 0f) {
            int aw = Math.min(bw, Math.round(Draw.clamp(tAbs / tMax) * bw));
            Draw.rrect(ctx, bx, by, aw, 2, 1, Draw.argb(0xFFD84A, a));
        }

        String ds = String.format(Locale.ROOT, "%.1f m", tDist);
        Draw.text(ctx, tr, ds, bx, y + 27, 0x8A8A96, a);
        String mx = "/ " + String.format(Locale.ROOT, "%.0f", tMax);
        Draw.text(ctx, tr, mx, x + w - 8 - Draw.w(tr, mx), y + 27, 0x8A8A96, a);
    }
}
