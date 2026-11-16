package com.shtora.client.setting;

import com.shtora.client.util.Anim;
import net.minecraft.client.util.InputUtil;
import java.util.Locale;

/**
 * Настройка-привязка клавиши.
 *
 * В меню выглядит как небольшая кнопка с названием клавиши. По нажатию переходит в
 * режим ожидания: следующая нажатая клавиша становится привязкой, ESC — снимает
 * привязку (NONE).
 */
public class KeybindSetting extends Setting {
    private int key;
    public final Anim anim;
    /** true, пока ждём нажатия клавиши. */
    public boolean listening;

    public KeybindSetting(String name, int def) {
        super(name);
        this.key = def;
        this.anim = new Anim(def > 0 ? 1f : 0f);
    }

    public int get() { return key; }

    public void set(int k) { key = Math.max(0, k); listening = false; }

    public boolean bound() { return key > 0; }

    public String display() { return keyName(key); }

    /** Человеческое название клавиши (RIGHT SHIFT, R, ...). */
    public static String keyName(int key) {
        if (key <= 0) return "NONE";
        try {
            String s = InputUtil.fromKeyCode(key, 0).getLocalizedText().getString();
            if (s != null && !s.isEmpty()) return s.toUpperCase(Locale.ROOT);
        } catch (Throwable ignored) {
        }
        return "KEY " + key;
    }
}
