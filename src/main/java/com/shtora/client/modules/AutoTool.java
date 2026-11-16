package com.shtora.client.modules;

import com.shtora.client.Category;
import com.shtora.client.Module;
import net.minecraft.block.BlockState;
import net.minecraft.client.MinecraftClient;
import net.minecraft.item.ItemStack;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.HitResult;

/** При копании сам берёт самый быстрый инструмент из хотбара. */
public class AutoTool extends Module {
    private int prevSlot = -1;

    public AutoTool() { super("AutoTool", Category.PLAYER); }

    @Override
    public void onTick(MinecraftClient mc) {
        if (mc.currentScreen != null) return;
        boolean mining = mc.options.attackKey.isPressed()
                && mc.crosshairTarget instanceof BlockHitResult hit && hit.getType() == HitResult.Type.BLOCK;
        if (!mining) {
            if (prevSlot >= 0) { mc.player.getInventory().selectedSlot = prevSlot; prevSlot = -1; }
            return;
        }
        BlockHitResult hit = (BlockHitResult) mc.crosshairTarget;
        BlockState state = mc.world.getBlockState(hit.getBlockPos());
        int best = mc.player.getInventory().selectedSlot;
        float bestSpeed = mc.player.getInventory().getStack(best).getMiningSpeedMultiplier(state);
        for (int i = 0; i < 9; i++) {
            ItemStack st = mc.player.getInventory().getStack(i);
            float sp = st.getMiningSpeedMultiplier(state);
            if (sp > bestSpeed) { bestSpeed = sp; best = i; }
        }
        if (best != mc.player.getInventory().selectedSlot) {
            if (prevSlot < 0) prevSlot = mc.player.getInventory().selectedSlot;
            mc.player.getInventory().selectedSlot = best;
        }
    }

    @Override
    public void onDisable(MinecraftClient mc) {
        if (prevSlot >= 0) mc.player.getInventory().selectedSlot = prevSlot;
        prevSlot = -1;
    }
}
