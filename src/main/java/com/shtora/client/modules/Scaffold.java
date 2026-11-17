package com.shtora.client.modules;

import com.shtora.client.Category;
import com.shtora.client.Module;
import com.shtora.client.setting.BoolSetting;
import com.shtora.client.setting.ModeSetting;
import com.shtora.client.setting.NumberSetting;
import com.shtora.client.util.CombatUtil;
import com.shtora.client.util.Rotations;
import net.minecraft.block.Block;
import net.minecraft.block.BlockWithEntity;
import net.minecraft.block.FallingBlock;
import net.minecraft.client.MinecraftClient;
import net.minecraft.item.BlockItem;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import java.util.ArrayList;
import java.util.List;

public class Scaffold extends Module {
    public final ModeSetting mode = add(new ModeSetting("Mode", "Normal", "Ninja", "Telly"));
    public final NumberSetting cps = add(new NumberSetting("CPS", 12, 1, 20, 1));
    public final NumberSetting expand = add(new NumberSetting("Expand", 0, 0, 4, 1));
    public final NumberSetting turn = add(new NumberSetting("Turn speed", 60, 10, 180, 5));
    public final NumberSetting smoothness = add(new NumberSetting("Smoothness", 0.4, 0.1, 1.0, 0.05));
    public final BoolSetting rotate = add(new BoolSetting("Rotate", true));
    public final BoolSetting silent = add(new BoolSetting("Silent", true));
    public final BoolSetting tower = add(new BoolSetting("Tower", true));
    public final BoolSetting switchBack = add(new BoolSetting("Switch back", true));

    private static class Plan { BlockPos nb; Direction face; Vec3d hit; }

    /** Сколько тиков держим угол после последней постановки, прежде чем плавно вернуть камеру. */
    private static final int HOLD = 14;

    private int prevSlot = -1, idle;
    private double credit;
    private boolean active, hasTarget;
    private float sy, sp, tYaw, tPitch;

    public Scaffold() { super("Scaffold", Category.PLAYER); }

    public boolean isRotating() { return active; }

    @Override
    public void onEnable(MinecraftClient mc) {
        prevSlot = mc.player.getInventory().selectedSlot;
        credit = 1.0;
        active = false;
        hasTarget = false;
        idle = 0;
    }

    @Override
    public void onDisable(MinecraftClient mc) {
        if (active) Rotations.restore(mc);
        active = false;
        hasTarget = false;
        if (mode.is("Ninja")) mc.options.sneakKey.setPressed(false);
        if (switchBack.get() && prevSlot >= 0) mc.player.getInventory().selectedSlot = prevSlot;
        prevSlot = -1;
    }

    @Override
    public void onTick(MinecraftClient mc) {
        if (mc.currentScreen != null || mc.interactionManager == null) return;
        credit = Math.min(2.0, credit + cps.get() / 20.0);

        int slot = findBlockSlot(mc);
        boolean ninja = mode.is("Ninja"), telly = mode.is("Telly");
        boolean moving = mc.options.forwardKey.isPressed() || mc.options.backKey.isPressed()
                || mc.options.leftKey.isPressed() || mc.options.rightKey.isPressed();

        List<Plan> plans = new ArrayList<>();
        Plan plan = null;
        if (slot >= 0) {
            double yaw = Math.toRadians(mc.player.getYaw());
            double dx = moving ? -Math.sin(yaw) : 0, dz = moving ? Math.cos(yaw) : 0;
            double px = mc.player.getX(), py = mc.player.getY(), pz = mc.player.getZ();

            if (telly && moving) {
                if (mc.player.isOnGround()) mc.player.jump();
                mc.player.setSprinting(true);
            }
            if (ninja) {
                boolean edge = moving && mc.player.isOnGround()
                        && mc.world.getBlockState(BlockPos.ofFloored(px + dx * 0.4, py - 1, pz + dz * 0.4)).isReplaceable();
                mc.options.sneakKey.setPressed(edge);
            }

            int[] offsets;
            if (ninja) offsets = new int[]{1, 0};
            else if (telly) offsets = new int[]{0};
            else {
                offsets = new int[(int) expand.get() + 1];
                for (int i = 0; i < offsets.length; i++) offsets[i] = i;
            }
            for (int i : offsets) {
                BlockPos pos = BlockPos.ofFloored(px + dx * i, py - 1, pz + dz * i);
                if (!mc.world.getBlockState(pos).isReplaceable()) continue;
                plans = makePlans(mc, pos);
                if (!plans.isEmpty()) { plan = plans.get(0); break; }
            }
        }

        if (plan != null) {
            idle = 0;
            if (rotate.get()) {
                float[] r = CombatUtil.rotationTo(mc, plan.hit);
                tYaw = r[0];
                tPitch = r[1];
                hasTarget = true;
            }
        } else {
            idle++;
        }

        // Поворот зациклен: пока мы строим, угол держится и плавно подстраивается под
        // следующий блок, а не возвращается к камере после каждой постановки.
        boolean aimed = true;
        if (rotate.get()) {
            if (hasTarget && idle <= HOLD) aimed = aim(mc);
            else if (active || hasTarget) relax(mc);
        }

        if (plan == null || !aimed || credit < 1.0) return;

        mc.player.getInventory().selectedSlot = slot;
        // если первая грань не приняла блок, пробуем остальные в этот же тик
        for (Plan p : plans) {
            ActionResult res = mc.interactionManager.interactBlock(mc.player, Hand.MAIN_HAND,
                    new BlockHitResult(p.hit, p.face, p.nb, false));
            if (!res.isAccepted()) continue;
            mc.player.swingHand(Hand.MAIN_HAND);
            credit -= 1.0;
            if (tower.get() && !telly && mc.options.jumpKey.isPressed() && !moving) {
                Vec3d v = mc.player.getVelocity();
                mc.player.setVelocity(v.x, 0.42, v.z);
            }
            break;
        }
    }

    /** Плавное наведение к целевому углу (экспоненциальное сглаживание + ограничение скорости). */
    private boolean aim(MinecraftClient mc) {
        float k = (float) smoothness.get();
        float cap = (float) turn.get();
        if (silent.get()) {
            if (!active) { sy = mc.player.getYaw(); sp = mc.player.getPitch(); active = true; }
            sy += MathHelper.clamp(MathHelper.wrapDegrees(tYaw - sy) * k, -cap, cap);
            sp = MathHelper.clamp(sp + MathHelper.clamp((tPitch - sp) * k, -cap, cap), -90f, 90f);
            Rotations.sendSilent(mc, sy, sp);
            return Math.abs(MathHelper.wrapDegrees(tYaw - sy)) < 40f && Math.abs(tPitch - sp) < 45f;
        }
        CombatUtil.rotateToward(mc, tYaw, tPitch, Math.max(2f, cap * k));
        return CombatUtil.yawError(mc, tYaw) < 40f && Math.abs(tPitch - mc.player.getPitch()) < 45f;
    }

    /** Плавно вернуть модель к реальному углу камеры, когда строить больше нечего. */
    private void relax(MinecraftClient mc) {
        if (!active) { hasTarget = false; return; }
        float k = (float) smoothness.get();
        float cap = (float) turn.get();
        sy += MathHelper.clamp(MathHelper.wrapDegrees(mc.player.getYaw() - sy) * k, -cap, cap);
        sp += MathHelper.clamp((mc.player.getPitch() - sp) * k, -cap, cap);
        Rotations.sendSilent(mc, sy, sp);
        if (Math.abs(MathHelper.wrapDegrees(mc.player.getYaw() - sy)) < 2f
                && Math.abs(mc.player.getPitch() - sp) < 2f) {
            Rotations.restore(mc);
            active = false;
            hasTarget = false;
        }
    }

    private List<Plan> makePlans(MinecraftClient mc, BlockPos pos) {
        List<Plan> out = new ArrayList<>();
        Vec3d eye = mc.player.getEyePos();
        for (Direction dir : Direction.values()) {
            BlockPos nb = pos.offset(dir);
            if (mc.world.getBlockState(nb).isReplaceable()) continue;
            Direction face = dir.getOpposite();
            Vec3d hit = new Vec3d(nb.getX() + 0.5 + face.getOffsetX() * 0.5,
                    nb.getY() + 0.5 + face.getOffsetY() * 0.5,
                    nb.getZ() + 0.5 + face.getOffsetZ() * 0.5);
            if (eye.distanceTo(hit) > 4.3) continue;
            Plan p = new Plan();
            p.nb = nb; p.face = face; p.hit = hit;
            out.add(p);
        }
        return out;
    }

    private int findBlockSlot(MinecraftClient mc) {
        int cur = mc.player.getInventory().selectedSlot;
        if (isGoodBlock(mc, mc.player.getInventory().getStack(cur))) return cur;
        for (int i = 0; i < 9; i++) {
            if (isGoodBlock(mc, mc.player.getInventory().getStack(i))) return i;
        }
        return -1;
    }

    private boolean isGoodBlock(MinecraftClient mc, ItemStack s) {
        if (s.isEmpty() || !(s.getItem() instanceof BlockItem bi)) return false;
        Block b = bi.getBlock();
        if (b instanceof BlockWithEntity || b instanceof FallingBlock) return false;
        return b.getDefaultState().isFullCube(mc.world, BlockPos.ORIGIN);
    }
}
