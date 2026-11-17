package com.shtora.client;

import com.shtora.client.modules.*;
import com.shtora.client.setting.BoolSetting;
import com.shtora.client.setting.ModeSetting;
import com.shtora.client.setting.NumberSetting;
import java.util.ArrayList;
import java.util.List;

public class ModuleManager {
    public static final List<Module> modules = new ArrayList<>();

    public static Interface ui;
    public static KillAura killAura;
    public static AutoMace autoMace;
    public static TargetEsp targetEsp;
    public static Optimization optimization;
    public static Esp esp;
    public static Scaffold scaffold;
    public static Tracers tracers;
    public static NameTags nameTags;
    public static BlockHighlight blockHighlight;
    public static Morph morph;
    public static CustomFog customFog;
    public static HudModule targetHud, armorHud, potionHud, keystrokes, coords, radar, invHud, itemCounter, infoHud;
    public static BoolSetting showBps, radarMobs, thPlayers, thMobs;
    public static ModeSetting thMode;
    public static NumberSetting thRadius;
    public static NumberSetting radarRange;

    public static void init() {
        ui = new Interface();
        killAura = new KillAura();
        autoMace = new AutoMace();
        targetEsp = new TargetEsp();
        optimization = new Optimization();
        esp = new Esp();
        scaffold = new Scaffold();
        tracers = new Tracers();
        nameTags = new NameTags();
        blockHighlight = new BlockHighlight();
        morph = new Morph();
        customFog = new CustomFog();
        invHud = new HudModule("InventoryHUD");
        itemCounter = new HudModule("ItemCounter");
        infoHud = new HudModule("InfoHUD");
        targetHud = new HudModule("TargetHUD");
        thMode = targetHud.with(new ModeSetting("Mode", "Radius", "Crosshair", "KillAura"));
        thRadius = targetHud.with(new NumberSetting("Radius", 10, 3, 40, 1));
        thPlayers = targetHud.with(new BoolSetting("Players", true));
        thMobs = targetHud.with(new BoolSetting("Mobs", false));
        armorHud = new HudModule("ArmorHUD");
        potionHud = new HudModule("PotionHUD");
        keystrokes = new HudModule("Keystrokes");
        coords = new HudModule("Coordinates");
        showBps = coords.with(new BoolSetting("Show BPS", true));
        radar = new HudModule("Radar");
        radarRange = radar.with(new NumberSetting("Range", 40, 10, 100, 5));
        radarMobs = radar.with(new BoolSetting("Mobs", false));

        // Combat
        modules.add(killAura);
        modules.add(autoMace);
        modules.add(new AimAssist());
        modules.add(new TriggerBot());
        modules.add(new Velocity());
        modules.add(new AutoTotem());
        modules.add(new Hitbox());
        // Movement
        modules.add(new Sprint());
        modules.add(new Fly());
        modules.add(new BHop());
        // Render
        modules.add(ui);
        modules.add(optimization);
        modules.add(new FullBright());
        modules.add(new Zoom());
        modules.add(new TimeChanger());
        modules.add(new NoWeather());
        modules.add(new ShowHitboxes());
        modules.add(esp);
        modules.add(targetEsp);
        modules.add(tracers);
        modules.add(nameTags);
        modules.add(blockHighlight);
        modules.add(new NoBob());
        modules.add(morph);
        modules.add(customFog);
        modules.add(targetHud);
        modules.add(armorHud);
        modules.add(potionHud);
        modules.add(keystrokes);
        modules.add(coords);
        modules.add(radar);
        modules.add(invHud);
        modules.add(itemCounter);
        modules.add(infoHud);
        // Player / Misc
        modules.add(scaffold);
        modules.add(new AutoRespawn());
        modules.add(new NoFall());
        modules.add(new AutoEat());
        modules.add(new AutoArmor());
        modules.add(new AutoTool());
        modules.add(new Sneak());
        modules.add(new AntiAFK());
        modules.add(new AutoWalk());
        modules.add(new Spin());
        modules.add(new Panic());

        // короткие подписи для меню (в стиле «карточек»)
        for (Module m : modules) m.description = describe(m.name);
    }

    /** Подпись под названием модуля. */
    private static String describe(String name) {
        switch (name) {
            case "KillAura": return "Attack players automatically";
            case "AutoMace": return "Smash attack with a mace";
            case "TargetESP": return "Highlight your current target";
            case "Optimization": return "FPS tweaks and less lag";
            case "AimAssist": return "Aim at targets smoothly";
            case "TriggerBot": return "Attack on crosshair";
            case "Velocity": return "Reduce knockback";
            case "AutoTotem": return "Refill totem in offhand";
            case "Hitbox": return "Resize entity hitboxes";
            case "Sprint": return "Auto sprint";
            case "Fly": return "Fly in survival";
            case "BHop": return "Bunny hop";
            case "Interface": return "Client interface settings";
            case "FullBright": return "Increase brightness";
            case "Zoom": return "Zoom with a key";
            case "Time": return "Change world time";
            case "NoWeather": return "Remove weather";
            case "Hitboxes": return "Show entity hitboxes";
            case "ESP": return "See entities through walls";
            case "Tracers": return "Draw entity tracers";
            case "NameTags": return "Show entity info";
            case "BlockHighlight": return "Outline targeted block";
            case "NoBob": return "Disable view bobbing";
            case "Morph": return "Change player model";
            case "CustomFog": return "Aurora / Lava / Water sky";
            case "TargetHUD": return "Show target info";
            case "ArmorHUD": return "Show armor durability";
            case "PotionHUD": return "Show potion effects";
            case "Keystrokes": return "Show pressed keys";
            case "Coordinates": return "Show your coordinates";
            case "Radar": return "Show nearby entities";
            case "InventoryHUD": return "Show inventory";
            case "ItemCounter": return "Count items in inventory";
            case "InfoHUD": return "Show client info";
            case "Scaffold": return "Place blocks under you";
            case "AutoRespawn": return "Respawn automatically";
            case "NoFall": return "Prevent fall damage";
            case "AutoEat": return "Eat when hungry";
            case "AutoArmor": return "Equip best armor";
            case "AutoTool": return "Switch to best tool";
            case "Sneak": return "Auto sneak";
            case "AntiAFK": return "Prevent AFK kick";
            case "AutoWalk": return "Walk automatically";
            case "Spin": return "Spin the player";
            case "Panic": return "Disable all modules";
            default: return "";
        }
    }

    public static List<Module> byCategory(Category c) {
        List<Module> out = new ArrayList<>();
        for (Module m : modules) if (m.category == c) out.add(m);
        return out;
    }
}
