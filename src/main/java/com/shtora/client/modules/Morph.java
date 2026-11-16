package com.shtora.client.modules;

import com.shtora.client.Category;
import com.shtora.client.Module;
import com.shtora.client.ModuleManager;
import com.shtora.client.setting.ModeSetting;
import net.minecraft.client.util.SkinTextures;
import net.minecraft.util.Identifier;
import java.util.HashMap;
import java.util.Map;

/**
 * Превращает твоего персонажа во внешний вид выбранного героя.
 * Меняется скин (только у тебя на экране, другие игроки видят обычный).
 */
public class Morph extends Module {
    public final ModeSetting character = add(new ModeSetting("Character",
            "Roblox", "CS:GO CT", "CS:GO T", "Brawl Shelly"));
    private final Map<String, SkinTextures> cache = new HashMap<>();

    public Morph() { super("Morph", Category.RENDER); }

    private static String file(String name) {
        return switch (name) {
            case "CS:GO CT" -> "csgo_ct";
            case "CS:GO T" -> "csgo_t";
            case "Brawl Shelly" -> "brawl_shelly";
            default -> "roblox";
        };
    }

    /** Скин для подмены или null, если Morph выключен. Вызывается из миксина. */
    public static SkinTextures current() {
        Morph m = ModuleManager.morph;
        if (m == null || !m.isEnabled()) return null;
        String name = m.character.get();
        return m.cache.computeIfAbsent(name, n -> new SkinTextures(
                Identifier.of("shtora", "textures/morph/" + file(n) + ".png"),
                null, null, null, SkinTextures.Model.WIDE, true));
    }
}
