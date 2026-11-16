package com.shtora.client;

import com.shtora.client.setting.Setting;
import com.shtora.client.util.Anim;
import net.minecraft.client.MinecraftClient;
import java.util.ArrayList;
import java.util.List;

public abstract class Module {
    public final String name;
    public final Category category;
    public final List<Setting> settings = new ArrayList<>();
    /** Короткая подпись под названием модуля (показывается в меню). */
    public String description = "";
    private boolean enabled;

    // состояние интерфейса
    public boolean expanded;
    public final Anim toggleAnim = new Anim(0), expandAnim = new Anim(0), hoverAnim = new Anim(0), listAnim = new Anim(0);
    public int uiX, uiY, uiW, uiH;

    protected Module(String name, Category category) {
        this.name = name;
        this.category = category;
    }

    protected <T extends Setting> T add(T s) { settings.add(s); return s; }

    public boolean isEnabled() { return enabled; }

    protected void enableSilently() { enabled = true; toggleAnim.v = 1f; }

    public void setEnabled(boolean e) {
        if (enabled == e) return;
        enabled = e;
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.player == null) return;
        if (e) onEnable(mc); else onDisable(mc);
    }

    /** Тихо выключить модуль (например, после ошибки). */
    public void forceDisable() { enabled = false; }

    public void toggle() { setEnabled(!enabled); }

    public void onEnable(MinecraftClient mc) {}
    public void onDisable(MinecraftClient mc) {}
    public void onTick(MinecraftClient mc) {}
    /** Вызывается каждый кадр (для плавных вещей, dt в секундах). */
    public void onFrame(MinecraftClient mc, float dt) {}
}
