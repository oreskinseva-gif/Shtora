package com.shtora.client.keybind;

import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import org.lwjgl.glfw.GLFW;

/**
 * Привязки, которые видны в Настройки → Управление → Shtora.
 * По умолчанию меню открывается на клавишу U.
 */
public final class ShtoraKeys {
    public static final String CATEGORY = "key.category.shtora";

    public static final KeyBinding MENU = new KeyBinding(
            "key.shtora.menu",
            InputUtil.Type.KEYSYM,
            GLFW.GLFW_KEY_U,
            CATEGORY);

    private ShtoraKeys() {}

    public static void register() {
        KeyBindingHelper.registerKeyBinding(MENU);
    }

    /** Текущий код клавиши из менecraft-настроек (учитывает замену в Controls). */
    public static int menuCode() {
        try {
            return KeyBindingHelper.getBoundKeyOf(MENU).getCode();
        } catch (Throwable ignored) {
            return GLFW.GLFW_KEY_U;
        }
    }
}
