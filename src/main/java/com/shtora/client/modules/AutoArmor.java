package com.shtora.client.modules;

import com.shtora.client.Category;
import com.shtora.client.Module;
import com.shtora.client.setting.NumberSetting;
import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.Registries;
import net.minecraft.screen.slot.SlotActionType;

/** Надевает броню из инвентаря в пустые слоты (лучшую по материалу). */
public class AutoArmor extends Module {
    public final NumberSetting delay = add(new NumberSetting("Delay (ticks)", 5, 1, 20, 1));
    private int wait;

    public AutoArmor() { super("AutoArmor", Category.PLAYER); }

    private static String suffix(EquipmentSlot s) {
        return switch (s) {
            case HEAD -> "_helmet";
            case CHEST -> "_chestplate";
            case LEGS -> "_leggings";
            default -> "_boots";
        };
    }

    private static int rank(String id) {
        if (id.startsWith("netherite")) return 6;
        if (id.startsWith("diamond")) return 5;
        if (id.startsWith("iron")) return 4;
        if (id.startsWith("chainmail")) return 3;
        if (id.startsWith("turtle") || id.startsWith("golden")) return 2;
        return 1;
    }

    @Override
    public void onTick(MinecraftClient mc) {
        if (mc.interactionManager == null || mc.currentScreen != null) return;
        if (++wait < delay.get()) return;
        wait = 0;
        EquipmentSlot[] slots = {EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET};
        var handler = mc.player.playerScreenHandler;
        for (EquipmentSlot es : slots) {
            if (!mc.player.getEquippedStack(es).isEmpty()) continue;
            int best = -1, bestRank = 0;
            for (int i = 9; i <= 44; i++) {
                ItemStack st = handler.getSlot(i).getStack();
                if (st.isEmpty()) continue;
                String id = Registries.ITEM.getId(st.getItem()).getPath();
                if (!id.endsWith(suffix(es))) continue;
                int r = rank(id);
                if (r > bestRank) { bestRank = r; best = i; }
            }
            if (best >= 0) {
                mc.interactionManager.clickSlot(handler.syncId, best, 0, SlotActionType.QUICK_MOVE, mc.player);
                return; // одна вещь за цикл
            }
        }
    }
}
