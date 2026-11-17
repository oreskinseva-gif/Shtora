package com.shtora.client;

import com.shtora.client.gui.ClickGuiScreen;
import com.shtora.client.gui.FogOverlay;
import com.shtora.client.keybind.ShtoraKeys;
import com.shtora.client.gui.Hud;
import com.shtora.client.util.Projection;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderEvents;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.option.NarratorMode;
import net.minecraft.client.util.InputUtil;
import net.minecraft.text.Text;
import org.lwjgl.glfw.GLFW;

public class ShtoraClient implements ClientModInitializer {
    public static final String NAME = "Shtora";
    private static boolean menuWasDown;
    private static long lastFrame = System.nanoTime();

    @Override
    public void onInitializeClient() {
        ShtoraKeys.register();
        ModuleManager.init();

        // экранный диктор клиенту не нужен: держим его выключенным
        ClientTickEvents.START_CLIENT_TICK.register(ShtoraClient::silenceNarrator);

        ClientTickEvents.END_CLIENT_TICK.register(mc -> {
            if (mc.player == null || mc.world == null) return;
            try { Projection.tick(mc); } catch (Throwable ignored) {}
            menuKey(mc);
            // каждый модуль изолирован: ошибка одного не ломает остальные
            for (Module m : ModuleManager.modules) {
                if (!m.isEnabled()) continue;
                try { m.onTick(mc); } catch (Throwable t) { fail(mc, m, t); }
            }
        });

        HudRenderCallback.EVENT.register((ctx, tickCounter) -> {
            try { FogOverlay.render(ctx); } catch (Throwable ignored) {}
            try { Hud.render(ctx); } catch (Throwable ignored) {}
        });

        WorldRenderEvents.AFTER_ENTITIES.register(ctx -> {
            MinecraftClient mc = MinecraftClient.getInstance();
            if (ModuleManager.esp.isEnabled()) {
                try { ModuleManager.esp.render(ctx); } catch (Throwable t) { fail(mc, ModuleManager.esp, t); }
            }
            if (ModuleManager.blockHighlight.isEnabled()) {
                try { ModuleManager.blockHighlight.render(ctx); } catch (Throwable t) { fail(mc, ModuleManager.blockHighlight, t); }
            }
        });

        WorldRenderEvents.START.register(ctx -> {
            long now = System.nanoTime();
            float dt = Math.min(0.1f, (now - lastFrame) / 1e9f);
            lastFrame = now;
            MinecraftClient mc = MinecraftClient.getInstance();
            if (mc.player == null || mc.world == null) return;
            for (Module m : ModuleManager.modules) {
                if (!m.isEnabled()) continue;
                try { m.onFrame(mc, dt); } catch (Throwable t) { fail(mc, m, t); }
            }
        });
    }

    /** Выключает модуль после ошибки и пишет об этом в чат, чтобы было видно, что именно сломалось. */
    public static void fail(MinecraftClient mc, Module m, Throwable t) {
        m.forceDisable();
        t.printStackTrace();
        if (mc != null && mc.inGameHud != null) {
            mc.inGameHud.getChatHud().addMessage(Text.literal(
                    "[Shtora] " + m.name + " выключен из-за ошибки: " + t.getClass().getSimpleName()
                            + (t.getMessage() != null ? " - " + t.getMessage() : "")));
        }
    }

    private static void silenceNarrator(MinecraftClient mc) {
        try {
            if (mc.options.getNarrator().getValue() != NarratorMode.OFF) {
                mc.options.getNarrator().setValue(NarratorMode.OFF);
            }
        } catch (Throwable ignored) {}
    }

    /** Клавиша открытия меню берётся из Настройки → Управление → Shtora. */
    private static void menuKey(MinecraftClient mc) {
        if (mc.currentScreen != null) { menuWasDown = true; return; }
        int key = ShtoraKeys.menuCode();
        if (key <= 0) key = GLFW.GLFW_KEY_U;
        boolean down = InputUtil.isKeyPressed(mc.getWindow().getHandle(), key);
        if (down && !menuWasDown) mc.setScreen(new ClickGuiScreen());
        menuWasDown = down;
    }
}
