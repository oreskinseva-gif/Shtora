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
    public final ModeSetting mode = add(new ModeSetting("Mode", "Normal", "Ninja", "Telly", "GodBridge"));
    public final ModeSetting bypass = add(new ModeSetting("Bypass", "None", "Grim", "NCP", "Vulcan", "Matrix", "Verus"));
    public final ModeSetting towerMode = add(new ModeSetting("Tower", "Vanilla", "NCP", "Motion", "None"));
    public final ModeSetting sprintMode = add(new ModeSetting("Sprint", "Allow", "Off", "Bypass"));
    public final ModeSetting swingMode = add(new ModeSetting("Swing", "Client", "None"));
    public final NumberSetting cps = add(new NumberSetting("CPS", 12, 1, 20, 1));
    public final NumberSetting expand = add(new NumberSetting("Expand", 0, 0, 6, 1));
    public final NumberSetting turn = add(new NumberSetting("Turn speed", 60, 10, 180, 5));
    public final NumberSetting smoothness = add(new NumberSetting("Smoothness", 0.4, 0.1, 1.0, 0.05));
    public final BoolSetting rotate = add(new BoolSetting("Rotate", true));
    public final BoolSetting silent = add(new BoolSetting("Silent", true));
    public final BoolSetting keepY = add(new BoolSetting("KeepY", false));
    public final BoolSetting safeWalk = add(new BoolSetting("SafeWalk", false));
    public final BoolSetting downwards = add(new BoolSetting("Downwards", false));
    public final BoolSetting switchBack = add(new BoolSetting("Switch back", true));
    public final BoolSetting raytrace = add(new BoolSetting("Raytrace", true));

    private static class Plan { BlockPos nb; Direction face; Vec3d hit; }

    private static final int HOLD = 14;

    private int prevSlot = -1, idle;
    private double credit;
    private boolean active, hasTarget;
    private float sy, sp, tYaw, tPitch;
    private double keepYLevel = -1;
    private int towerTicks;

    public Scaffold() { super("Scaffold", Category.PLAYER); }

    public boolean isRotating() { return active; }

    @Override
    public void onEnable(MinecraftClient mc) {
        prevSlot = mc.player != null ? mc.player.getInventory().selectedSlot : -1;
        credit = 1.0;
        active = false;
        hasTarget = false;
        idle = 0;
        keepYLevel = mc.player != null ? Math.floor(mc.player.getY() - 1.0) : -1;
        towerTicks = 0;
    }

    @Override
    public void onDisable(MinecraftClient mc) {
        if (active) Rotations.restore(mc);
        active = false;
        hasTarget = false;
        if (mode.is("Ninja")) mc.options.sneakKey.setPressed(false);
        if (switchBack.get() && prevSlot >= 0 && mc.player != null) mc.player.getInventory().selectedSlot = prevSlot;
        prevSlot = -1;
        keepYLevel = -1;
    }

    private boolean grimLike() { return bypass.is("Grim"); }

    @Override
    public void onTick(MinecraftClient mc) {
        if (mc.player == null || mc.world == null || mc.currentScreen != null || mc.interactionManager == null) return;
        credit = Math.min(2.0, credit + cps.get() / 20.0);

        int slot = findBlockSlot(mc);
        boolean ninja = mode.is("Ninja"), telly = mode.is("Telly"), god = mode.is("GodBridge");
        boolean moving = mc.options.forwardKey.isPressed() || mc.options.backKey.isPressed()
                || mc.options.leftKey.isPressed() || mc.options.rightKey.isPressed();

        if (!moving && keepY.get() && mc.player.isOnGround()) keepYLevel = Math.floor(mc.player.getY() - 1.0);

        if (sprintMode.is("Off") || (sprintMode.is("Bypass") && (grimLike() || bypass.is("Vulcan")))) {
            mc.player.setSprinting(false);
        } else if (god || telly) {
            if (moving) mc.player.setSprinting(true);
        }

        List<Plan> plans = new ArrayList<>();
        Plan plan = null;
        if (slot >= 0) {
            double yaw = Math.toRadians(mc.player.getYaw());
            double dx = moving ? -Math.sin(yaw) : 0, dz = moving ? Math.cos(yaw) : 0;
            double px = mc.player.getX(), py = mc.player.getY(), pz = mc.player.getZ();

            if (telly && moving) {
                if (mc.player.isOnGround()) mc.player.jump();
            }
            if (ninja || safeWalk.get()) {
                boolean edge = moving && mc.player.isOnGround()
                        && mc.world.getBlockState(BlockPos.ofFloored(px + dx * 0.35, py - 1, pz + dz * 0.35)).isReplaceable();
                if (ninja || safeWalk.get()) mc.options.sneakKey.setPressed(edge);
            } else if (!mc.options.sneakKey.isPressed()) {
                mc.options.sneakKey.setPressed(false);
            }

            int[] offsets;
            if (ninja) offsets = new int[]{1, 0};
            else if (telly) offsets = new int[]{0};
            else {
                int ex = (int) expand.get();
                if (grimLike()) ex = Math.min(ex, 1);
                offsets = new int[ex + 1];
                for (int i = 0; i < offsets.length; i++) offsets[i] = i;
            }

            double baseY = py - 1;
            if (keepY.get() && keepYLevel > -1000 && moving) baseY = keepYLevel;
            if (downwards.get() && mc.options.sneakKey.isPressed()) baseY = py - 2;

            for (int i : offsets) {
                BlockPos pos = BlockPos.ofFloored(px + dx * i, baseY, pz + dz * i);
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
                tPitch = bypass.is("Grim") ? Math.max(r[1], 80f) : r[1];
                if (god) tPitch = Math.max(tPitch, 75f);
                hasTarget = true;
            }
        } else {
            idle++;
        }

        boolean aimed = true;
        if (rotate.get()) {
            if (hasTarget && idle <= HOLD) aimed = aim(mc);
            else if (active || hasTarget) relax(mc);
        }

        if (plan == null || !aimed || credit < 1.0) return;

        mc.player.getInventory().selectedSlot = slot;
        for (Plan p : plans) {
            if (raytrace.get()) {
                // простая проверка дистанции/валидности уже есть в makePlans
            }
            ActionResult res = mc.interactionManager.interactBlock(mc.player, Hand.MAIN_HAND,
                    new BlockHitResult(p.hit, p.face, p.nb, false));
            if (!res.isAccepted()) continue;
            if (!swingMode.is("None")) mc.player.swingHand(Hand.MAIN_HAND);
            credit -= 1.0;
            handleTower(mc, moving, telly);
            break;
        }
    }

    private void handleTower(MinecraftClient mc, boolean moving, boolean telly) {
        if (moving || telly || towerMode.is("None")) { towerTicks = 0; return; }
        if (!mc.options.jumpKey.isPressed()) { towerTicks = 0; return; }
        Vec3d v = mc.player.getVelocity();
        switch (towerMode.get()) {
            case "NCP":
                if (mc.player.isOnGround()) { mc.player.setVelocity(v.x, 0.42, v.z); towerTicks = 0; }
                else if (towerTicks == 1) mc.player.setVelocity(v.x, 0.33, v.z);
                towerTicks++;
                break;
            case "Motion":
                mc.player.setVelocity(v.x, 0.42, v.z);
                break;
            default:
                if (mc.player.isOnGround()) mc.player.setVelocity(v.x, 0.42, v.z);
                break;
        }
    }

    private boolean aim(MinecraftClient mc) {
        float k = (float) smoothness.get();
        float cap = (float) turn.get();
        if (bypass.is("Grim") || bypass.is("Matrix")) { k = Math.max(k, 0.55f); cap = Math.min(cap, 80f); }
        if (silent.get() && !bypass.is("Grim")) {
            if (!active) { sy = mc.player.getYaw(); sp = mc.player.getPitch(); active = true; }
            sy += MathHelper.clamp(MathHelper.wrapDegrees(tYaw - sy) * k, -cap, cap);
            sp = MathHelper.clamp(sp + MathHelper.clamp((tPitch - sp) * k, -cap, cap), -90f, 90f);
            Rotations.sendSilent(mc, sy, sp);
            return Math.abs(MathHelper.wrapDegrees(tYaw - sy)) < 40f && Math.abs(tPitch - sp) < 45f;
        }
        if (active && silent.get()) { Rotations.restore(mc); active = false; }
        CombatUtil.rotateToward(mc, tYaw, tPitch, Math.max(2f, cap * k));
        return CombatUtil.yawError(mc, tYaw) < 40f && Math.abs(tPitch - mc.player.getPitch()) < 45f;
    }

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
            if (eye.distanceTo(hit) > 4.5) continue;
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
