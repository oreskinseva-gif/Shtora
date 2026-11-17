package com.shtora.client.util;

import com.shtora.client.Module;
import com.shtora.client.ModuleManager;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import java.util.IdentityHashMap;
import java.util.Map;

/**
 * Клавиши модулей.
 *
 * У каждого модуля есть обычная клавиша Minecraft, поэтому её видно и можно менять
 * и в меню клиента (раздел настроек модуля), и в «Настройки → Управление» (раздел
 * Shtora). Это одна и та же привязка — оба места синхронизированы между собой.
 */
public final class Keybinds {
    private static final Map<Module, KeyBinding> MAP = new IdentityHashMap<>();

    private Keybinds() {}

    /** Создаёт клавиши модулей. Вызывается один раз при запуске клиента. */
    public static void init() {
        for (Module m : ModuleManager.modules) {
            if (m.bind == null) continue;
            try {
                KeyBinding kb = KeyBindingHelper.registerKeyBinding(
                        new KeyBinding(m.name, InputUtil.Type.KEYSYM, m.bind.get(), "Shtora"));
                MAP.put(m, kb);
            } catch (Throwable ignored) {
            }
        }
    }

    /** Переносит привязки из меню клиента в «Управление» (после смены клавиши в меню). */
    public static void pushAll() {
        for (Map.Entry<Module, KeyBinding> e : MAP.entrySet()) {
            KeyBinding kb = e.getValue();
            int code = e.getKey().bind.get();
            if (code != codeOf(kb)) {
                try { kb.setBoundKey(keyOf(code)); } catch (Throwable ignored) {}
            }
        }
    }

    /** Раз в тик: подхватывает смену клавиши из «Управления» и переключает модули. */
    public static void tick(MinecraftClient mc) {
        for (Map.Entry<Module, KeyBinding> e : MAP.entrySet()) {
            Module m = e.getKey();
            KeyBinding kb = e.getValue();
            int code = codeOf(kb);
            if (code != m.bind.get()) m.bind.set(code);      // поменяли в «Управлении»
            boolean pressed = kb.wasPressed();
            if (pressed && mc.currentScreen == null) m.toggle();
        }
    }

    private static int codeOf(KeyBinding kb) {
        try {
            InputUtil.Key k = KeyBindingHelper.getBoundKeyOf(kb);
            int c = k == null ? 0 : k.getCode();
            return c < 0 ? 0 : c;
        } catch (Throwable t) {
            return 0;
        }
    }

    private static InputUtil.Key keyOf(int code) {
        if (code <= 0) return InputUtil.UNKNOWN_KEY;
        try {
            return InputUtil.fromKeyCode(code, 0);
        } catch (Throwable t) {
            return InputUtil.UNKNOWN_KEY;
        }
    }
}
