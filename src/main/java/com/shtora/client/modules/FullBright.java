package com.shtora.client.modules;

import com.shtora.client.Category;
import com.shtora.client.Module;
import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;

public class FullBright extends Module {
    public FullBright() { super("FullBright", Category.RENDER); }

    @Override
    public void onTick(MinecraftClient mc) {
        mc.player.addStatusEffect(new StatusEffectInstance(StatusEffects.NIGHT_VISION, 400, 0, false, false, false));
    }

    @Override
    public void onDisable(MinecraftClient mc) {
        mc.player.removeStatusEffect(StatusEffects.NIGHT_VISION);
    }
}
