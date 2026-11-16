package com.shtora.client.setting;

public abstract class Setting {
    public final String name;
    // положение на экране (заполняется при отрисовке меню)
    public int ux, uy, uw, uh;

    protected Setting(String name) { this.name = name; }
}
