package com.shtora.client.modules;

import com.shtora.client.Category;
import com.shtora.client.Module;
import com.shtora.client.setting.Setting;

/** Модуль, который только включает отрисовку виджета в Hud. */
public class HudModule extends Module {
    public HudModule(String name) { super(name, Category.RENDER); }

    public <T extends Setting> T with(T s) { return add(s); }
}
