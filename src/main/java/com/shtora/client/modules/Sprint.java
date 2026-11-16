package com.shtora.client.modules;

import com.shtora.client.Category;
import com.shtora.client.Module;
import net.minecraft.client.MinecraftClient;

public class Sprint extends Module {
    public Sprint() { super("Sprint", Category.MOVEMENT); }

    @Override
    public void onTick(MinecraftClient mc) {
        if (mc.options.forwardKey.isPressed() && !mc.player.horizontalCollision
                && mc.player.getHungerManager().getFoodLevel() > 6) {
            mc.player.setSprinting(true);
        }
    }
}
