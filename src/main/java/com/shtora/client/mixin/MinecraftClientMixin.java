package com.shtora.client.mixin;

import com.shtora.client.gui.ShtoraTitleScreen;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.TitleScreen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Подменяет ванильный главный экран на {@link ShtoraTitleScreen}.
 * Миксин мягкий (require = 0): если сигнатура не совпадёт, игра просто покажет обычное меню.
 */
@Mixin(MinecraftClient.class)
public abstract class MinecraftClientMixin {
    @Inject(method = "setScreen", at = @At("HEAD"), cancellable = true, require = 0)
    private void shtora$titleScreen(Screen screen, CallbackInfo ci) {
        if (!(screen instanceof TitleScreen)) return;
        MinecraftClient mc = (MinecraftClient) (Object) this;
        // рекурсивный вызов безопасен: ShtoraTitleScreen не является TitleScreen
        mc.setScreen(new ShtoraTitleScreen());
        ci.cancel();
    }
}
