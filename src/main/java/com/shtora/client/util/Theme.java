package com.shtora.client.util;

import com.shtora.client.ModuleManager;

/**
 * Палитра клиента — по образцу: чисто чёрный фон, тёмные скруглённые
 * карточки-категории, мягкий лавандовый акцент и янтарная подсветка
 * правой панели. Акцент меняется квадратиками внизу меню (или ползунком
 * Accent в модуле Interface).
 */
public final class Theme {
    private Theme() {}

    // лавандовый (265°) — как в макете; меняется квадратиками внизу меню
    public static final float DEFAULT_HUE = 265f / 360f;

    public static final int BG_TOP = 0x000000;
    public static final int BG_BOTTOM = 0x000000;
    public static final int PANEL = 0x111012;
    public static final int PANEL_ALT = 0x181719;
    public static final int PANEL_HEAD = 0x1A171C;
    public static final int BORDER = 0x26252A;
    public static final int TEXT = 0xD5D4D6;
    public static final int TEXT_DIM = 0x6E6C74;

    // панели меню
    public static final int CARD = 0x181719;        // фон строки модуля
    public static final int CARD_HOVER = 0x242325;  // строка под курсором
    public static final int CARD_HEAD = 0x1A171C;   // шапка панели категории
    public static final int SIDEBAR = 0x0D0D0D;     // левая панель
    public static final int TOGGLE_OFF = 0x242325;  // выключенный переключатель
    public static final int KNOB_OFF = 0x49484A;    // бегунок выключенного
    public static final int DESC = 0x6E6C74;        // подпись под названием модуля

    // папки, избранное и удалённое
    public static final int FOLDER = 0x181719;      // фон папки
    public static final int FOLDER_OPEN = 0x242325; // раскрытая папка
    public static final int STAR = 0xFFD24A;        // избранное (жёлтая звезда)
    public static final int TRASH = 0xFF6B6B;       // корзина (красноватая)
    public static final int DELETED = 0xFFC93C;     // удалённая папка (жёлтая подсветка)
    public static final int RESTORE = 0x6BE38A;     // вернуть из удалённых (зелёная)

    /** Янтарный — подсветка правой панели (в макете «Server modules»). */
    public static final int AMBER = 0xFFB956;

    // фон-перелив заставки: тёмно-синий -> голубой -> снова синий
    public static final int SKY_DARK = 0x061127;
    public static final int SKY_MID = 0x123A78;
    public static final int SKY_LIGHT = 0x2E86E0;
    public static final int SKY_GLOW = 0x63C8FF;

    /**
     * Цвет фона на высоте u (0..1): тёмно-синий -> голубой -> снова синий.
     * Медленно переливается во времени t (секунды). Общий для меню и заставки.
     */
    public static int sky(float u, float t) {
        float ph = (float) Math.sin((u * 1.6f + t * 0.05f) * Math.PI * 2.0);
        float k = 0.5f + 0.5f * ph;
        int c = Draw.mix(SKY_DARK, SKY_LIGHT, k);
        return Draw.mix(c, SKY_GLOW, 0.12f * k);
    }

    public static int accent() { return accent(0f); }

    /** Акцентный цвет; shift даёт небольшой сдвиг оттенка (волна в списке модулей). */
    public static int accent(float shift) {
        float base = ModuleManager.ui == null ? DEFAULT_HUE : (float) ModuleManager.ui.hue.get() / 360f;
        return Draw.hsb(base + shift * 0.10f, 0.45f, 1.0f);
    }

    /** Бегунок включённого переключателя — чуть светлее акцента. */
    public static int knobOn() { return Draw.mix(accent(), 0xFFFFFF, 0.35f); }
}
