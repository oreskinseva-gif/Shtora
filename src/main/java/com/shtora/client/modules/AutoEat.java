package com.shtora.client.modules;

import com.shtora.client.Category;
import com.shtora.client.Module;
import com.shtora.client.setting.NumberSetting;
import net.minecraft.client.MinecraftClient;
import net.minecraft.component.DataComponentTypes;

public class AutoEat extends Module {
    public final NumberSetting hunger = add(new NumberSetting("Hunger", 14, 1, 19, 1));
    private boolean eating;
    private int prevSlot = -1;

    public AutoEat() { super("AutoEat", Category.PLAYER); }

    private int findFood(MinecraftClient mc) {
        for (int i = 0; i < 9; i++) {
            if (mc.player.getInventory().getStack(i).contains(DataComponentTypes.FOOD)) return i;
        }
        return -1;
    }

    @Override
    public void onTick(MinecraftClient mc) {
        if (mc.currentScreen != null) { stop(mc); return; }
        int food = mc.player.getHungerManager().getFoodLevel();
        if (!eating) {
            if (food > hunger.get()) return;
            int slot = findFood(mc);
            if (slot < 0) return;
            prevSlot = mc.player.getInventory().selectedSlot;
            mc.player.getInventory().selectedSlot = slot;
            mc.options.useKey.setPressed(true);
            eating = true;
        } else if (food >= 20 || findFood(mc) < 0) {
            stop(mc);
        }
    }

    private void stop(MinecraftClient mc) {
        if (!eating) return;
        mc.options.useKey.setPressed(false);
        if (prevSlot >= 0) mc.player.getInventory().selectedSlot = prevSlot;
        prevSlot = -1;
        eating = false;
    }

    @Override
    public void onDisable(MinecraftClient mc) { stop(mc); }
}
