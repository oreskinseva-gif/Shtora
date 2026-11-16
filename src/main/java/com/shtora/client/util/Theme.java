package com.shtora.client.util;

import com.shtora.client.ModuleManager;

/** Палитра клиента: тёмные «плоские» панели + голубой акцент. */
public final class Theme {
    private Theme() {}

    // голубой (200°) — меняется ползунком Accent в модуле Interface
    public static final float DEFAULT_HUE = 200f / 360f;

    public static final int BG_TOP = 0x060A11;
    public static final int BG_BOTTOM = 0x0B1420;
    public static final int PANEL = 0x0D1521;
    public static final int PANEL_ALT = 0x122032;
    public static final int PANEL_HEAD = 0x15263A;
    public static final int BORDER = 0x1E3247;
    public static final int TEXT = 0xDCEAF7;
    public static final int TEXT_DIM = 0x7E90A6;

    // панели меню
    public static final int CARD = 0x0E1826;        // фон строки модуля
    public static final int CARD_HEAD = 0x15263A;   // шапка панели категории
    public static final int SIDEBAR = 0x0A1119;     // левая панель
    public static final int TOGGLE_OFF = 0x27384A;  // выключенный переключатель
    public static final int DESC = 0x6D7E93;        // подпись под названием модуля

    // папки, избранное и удалённое
    public static final int FOLDER = 0x0E1826;      // фон папки
    public static final int FOLDER_OPEN = 0x122032; // раскрытая папка
    public static final int STAR = 0xFFD24A;        // избранное (жёлтая звезда)
    public static final int TRASH = 0xFF6B6B;       // корзина (красноватая)
    public static final int DELETED = 0xFFC93C;     // удалённая папка (жёлтая подсветка)
    public static final int RESTORE = 0x6BE38A;     // вернуть из удалённых (зелёная)

    // фон-перелив: тёмно-синий -> голубой -> снова синий
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
        return Draw.hsb(base + shift * 0.10f, 0.85f, 1.0f);
    }
}
