package com.shtora.client.modules;

import com.shtora.client.Category;
import com.shtora.client.Module;
import com.shtora.client.setting.BoolSetting;

/**
 * AntiBot — фильтр фейковых игроков (NPC).
 *
 * Пока модуль включён, KillAura, AimAssist, TriggerBot и Hitbox не действуют на
 * сущности, которые {@link com.shtora.client.util.AntiBotUtil} считает ботами.
 * ESP, Tracers, NameTags и HUD при этом продолжают их показывать.
 */
public class AntiBot extends Module {
    public final BoolSetting tabList = add(new BoolSetting("Tab list", true));
    public final BoolSetting duplicate = add(new BoolSetting("Duplicates", true));
    public final BoolSetting name = add(new BoolSetting("Name check", true));
    public final BoolSetting uuid = add(new BoolSetting("UUID check", false));
    public final BoolSetting ping = add(new BoolSetting("Ping check", false));
    public final BoolSetting sleeping = add(new BoolSetting("Sleeping", false));
    public final BoolSetting health = add(new BoolSetting("Health", false));
    public final BoolSetting pitch = add(new BoolSetting("Pitch check", false));

    public AntiBot() { super("AntiBot", Category.COMBAT); }
}
