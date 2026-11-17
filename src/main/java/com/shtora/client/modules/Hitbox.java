package com.shtora.client.modules;

import com.shtora.client.Category;
import com.shtora.client.Module;
import com.shtora.client.ModuleManager;
import com.shtora.client.setting.BoolSetting;
import com.shtora.client.setting.NumberSetting;
import com.shtora.client.util.CombatUtil;
import com.shtora.client.util.Rotations;
import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.Entity;
import net.minecraft.util.hit.EntityHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;

/**
 * Увеличивает хитбоксы целей на клиенте (по ним легче попасть).
 * Aim fix: если прицел попал только в увеличенную часть, серверу тихо отправляется угол
 * на настоящий хитбокс, поэтому со стороны сервера удар выглядит как по обычному хитбоксу.
 */
public class Hitbox extends Module {
    public final NumberSetting size = add(new NumberSetting("Size", 0.2, 0.0, 1.0, 0.05));
    public final BoolSetting players = add(new BoolSetting("Players", true));
    public final BoolSetting mobs = add(new BoolSetting("Mobs", false));
    public final BoolSetting aimFix = add(new BoolSetting("Aim fix", true));

    private boolean active;
    private float sy, sp;

    public Hitbox() { super("Hitbox", Category.COMBAT); }

    public boolean isRotating() { return active; }

    private static Box base(Entity e) {
        return e.getDimensions(e.getPose()).getBoxAt(e.getPos());
    }

    @Override
    public void onTick(MinecraftClient mc) {
        double s = size.get();
        for (Entity e : mc.world.getEntities()) {
            if (!CombatUtil.isValidTarget(mc, e, players.get(), mobs.get(), false)) continue;
            e.setBoundingBox(base(e).expand(s, s * 0.5, s));
        }

        if (!aimFix.get() || mc.currentScreen != null) { relax(mc); return; }
        // если поворотом уже управляет другой модуль, не мешаем
        if ((ModuleManager.killAura.isEnabled() && ModuleManager.killAura.isRotating())
                || (ModuleManager.scaffold.isEnabled() && ModuleManager.scaffold.isRotating())) return;

        HitResult hit = mc.crosshairTarget;
        Entity e = (hit != null && hit.getType() == HitResult.Type.ENTITY) ? ((EntityHitResult) hit).getEntity() : null;
        if (e == null || !CombatUtil.isValidTarget(mc, e, players.get(), mobs.get(), false)) { relax(mc); return; }

        Box real = base(e);
        Vec3d eye = mc.player.getEyePos();
        Vec3d end = eye.add(mc.player.getRotationVec(1.0f).multiply(6.0));
        if (real.contains(eye) || real.raycast(eye, end).isPresent()) { relax(mc); return; }

        // прицел попал только в «лишнюю» часть: тихо наводимся на центр настоящего хитбокса
        float[] r = CombatUtil.rotationTo(mc, real.getCenter());
        if (!active) { sy = mc.player.getYaw(); sp = mc.player.getPitch(); active = true; }
        sy += MathHelper.wrapDegrees(r[0] - sy) * 0.6f;
        sp += (r[1] - sp) * 0.6f;
        Rotations.sendSilent(mc, sy, sp);
    }

    private void relax(MinecraftClient mc) {
        if (!active) return;
        sy += MathHelper.wrapDegrees(mc.player.getYaw() - sy) * 0.5f;
        sp += (mc.player.getPitch() - sp) * 0.5f;
        Rotations.sendSilent(mc, sy, sp);
        if (Math.abs(MathHelper.wrapDegrees(mc.player.getYaw() - sy)) < 2f
                && Math.abs(mc.player.getPitch() - sp) < 2f) {
            Rotations.restore(mc);
            active = false;
        }
    }

    @Override
    public void onDisable(MinecraftClient mc) {
        if (active) Rotations.restore(mc);
        active = false;
        for (Entity e : mc.world.getEntities()) {
            if (e != mc.player) e.setBoundingBox(base(e));
        }
    }
}
