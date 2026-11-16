package com.shtora.client.modules;

import com.shtora.client.Category;
import com.shtora.client.Module;
import com.shtora.client.setting.NumberSetting;
import net.minecraft.client.MinecraftClient;
import net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket;

public class NoFall extends Module {
    public final NumberSetting distance = add(new NumberSetting("Fall distance", 2.5, 1.5, 10.0, 0.5));

    public NoFall() { super("NoFall", Category.PLAYER); }

    @Override
    public void onTick(MinecraftClient mc) {
        if (mc.getNetworkHandler() == null) return;
        if (mc.player.fallDistance > distance.get() && !mc.player.isOnGround()) {
            mc.getNetworkHandler().sendPacket(
                    new PlayerMoveC2SPacket.OnGroundOnly(true, mc.player.horizontalCollision));
        }
    }
}
