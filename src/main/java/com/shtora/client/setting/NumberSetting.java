package com.shtora.client.setting;

import com.shtora.client.util.Anim;

public class NumberSetting extends Setting {
    private double value;
    public final double min, max, step;
    public final Anim anim;

    public NumberSetting(String name, double def, double min, double max, double step) {
        super(name);
        this.value = def;
        this.min = min;
        this.max = max;
        this.step = step;
        this.anim = new Anim((float) ((def - min) / (max - min)));
    }

    public double get() { return value; }

    public void set(double v) { value = Math.max(min, Math.min(max, v)); }

    public float frac() { return (float) ((value - min) / (max - min)); }
}
