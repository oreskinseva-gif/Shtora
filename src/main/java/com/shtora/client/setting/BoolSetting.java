package com.shtora.client.setting;

import com.shtora.client.util.Anim;

public class BoolSetting extends Setting {
    private boolean value;
    public final Anim anim;

    public BoolSetting(String name, boolean def) {
        super(name);
        this.value = def;
        this.anim = new Anim(def ? 1f : 0f);
    }

    public boolean get() { return value; }
    public void toggle() { value = !value; }
}
