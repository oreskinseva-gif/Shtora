package com.shtora.client.setting;

public class ModeSetting extends Setting {
    public final String[] modes;
    private int index;

    public ModeSetting(String name, String... modes) {
        super(name);
        this.modes = modes;
    }

    public String get() { return modes[index]; }
    public boolean is(String m) { return modes[index].equals(m); }
    public int index() { return index; }
    public void set(int i) { index = Math.max(0, Math.min(modes.length - 1, i)); }
    public void cycle() { index = (index + 1) % modes.length; }
}
