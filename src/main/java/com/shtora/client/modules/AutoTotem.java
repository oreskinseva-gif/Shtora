package com.shtora.client.modules;

import com.shtora.client.Category;
import com.shtora.client.Module;
import com.shtora.client.setting.NumberSetting;
import net.minecraft.client.MinecraftClient;
import net.minecraft.item.Items;
import net.minecraft.screen.slot.SlotActionType;

public class AutoTotem extends Module {
    public final NumberSetting delay = add(new NumberSetting("Delay (ticks)", 2, 0, 10, 1));
    private int wait;

    public AutoTotem() { super("AutoTotem", Category.COMBAT); }

    @Override
    public void onTick(MinecraftClient mc) {
        if (mc.interactionManager == null) return;
        if (mc.player.getOffHandStack().isOf(Items.TOTEM_OF_UNDYING)) { wait = 0; return; }
        if (++wait <= delay.get()) return;
        var handler = mc.player.playerScreenHandler;
        for (int i = 9; i <= 44; i++) {
            if (handler.getSlot(i).getStack().isOf(Items.TOTEM_OF_UNDYING)) {
                // кнопка 40 = обмен со слотом второй руки
                mc.interactionManager.clickSlot(handler.syncId, i, 40, SlotActionType.SWAP, mc.player);
                wait = 0;
                return;
            }
        }
    }
}
