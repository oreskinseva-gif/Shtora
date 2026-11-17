package com.shtora.client.util;

import com.shtora.client.ModuleManager;
import com.shtora.client.modules.AntiBot;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.PlayerListEntry;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * Определение «ботов» — фейковых игроков (NPC) на серверах.
 *
 * Серверные NPC (Citizens и подобные) часто выдают себя тем, что:
 *  - их нет в списке игроков (Tab), либо их имя/UUID дублируется;
 *  - имя похоже на NPC/Bot/Shop и т.п.;
 *  - ping равен нулю или UUID не похож на настоящий;
 *  - они «спят», у них всегда полное здоровье, а голова смотрит строго в 0.
 *
 * Проверки не идеальны: это эвристики, поэтому каждая из них включается/выключается
 * отдельно в модуле AntiBot. Кэш имён обновляется раз в 100 мс, чтобы не считать
 * одно и то же на каждый кадр.
 */
public final class AntiBotUtil {
    private AntiBotUtil() {}

    private static final String[] BOT_WORDS = {
            "npc", "bot", "citizen", "shop", "guard", "hologram", "fake", "silent",
            "trader", "merchant", "banker", "quest", "market", "auction"
    };

    private static long lastScan;
    private static final Map<String, Integer> nameCount = new HashMap<>();
    private static final Map<UUID, Integer> uuidCount = new HashMap<>();

    /** Включён ли AntiBot (и есть ли вообще такой модуль). */
    public static boolean active() {
        AntiBot a = ModuleManager.antiBot;
        return a != null && a.isEnabled();
    }

    /** Является ли сущность ботом по включённым проверкам. */
    public static boolean isBot(MinecraftClient mc, Entity e) {
        AntiBot a = ModuleManager.antiBot;
        if (a == null || !a.isEnabled()) return false;
        // NPC-«игроки»; обычные мобы и животные под AntiBot не попадают
        if (!(e instanceof PlayerEntity p)) return false;
        if (p == mc.player) return false;

        if (a.name.get() && looksLikeBotName(p.getName().getString())) return true;

        PlayerListEntry entry = null;
        if (mc.getNetworkHandler() != null) {
            try { entry = mc.getNetworkHandler().getPlayerListEntry(p.getUuid()); } catch (Throwable ignored) {}
        }

        if (a.tabList.get() && entry == null && hasTabData(mc)) return true;
        if (a.uuid.get() && !isRealUuid(p.getUuid())) return true;
        if (a.ping.get() && entry != null && entry.getLatency() <= 0) return true;
        if (a.sleeping.get() && p.isSleeping()) return true;
        if (a.health.get() && (p.getHealth() <= 0f || p.getHealth() > p.getMaxHealth())) return true;
        if (a.pitch.get() && p.getPitch() == 0f && p.getYaw() == 0f) return true;

        if (a.duplicate.get()) {
            scan(mc);
            Integer byName = nameCount.get(p.getName().getString().toLowerCase(Locale.ROOT));
            Integer byUuid = uuidCount.get(p.getUuid());
            if (byName != null && byName > 1) return true;
            if (byUuid != null && byUuid > 1) return true;
        }
        return false;
    }

    private static boolean looksLikeBotName(String name) {
        if (name == null || name.isEmpty()) return false;
        String n = name.toLowerCase(Locale.ROOT);
        for (String w : BOT_WORDS) if (n.contains(w)) return true;
        // имя целиком из цифр (12345) — почти всегда бот
        boolean digits = true;
        for (int i = 0; i < n.length(); i++) if (!Character.isDigit(n.charAt(i))) { digits = false; break; }
        return digits;
    }

    /** UUID версии 4 — так выглядят настоящие игроки (v3 — офлайн-режим). */
    private static boolean isRealUuid(UUID id) {
        try { return id.version() == 4; } catch (Throwable t) { return true; }
    }

    /** Есть ли вообще данные списка игроков (иначе проверка Tab бессмысленна). */
    private static boolean hasTabData(MinecraftClient mc) {
        try { return mc.getNetworkHandler() != null && !mc.getNetworkHandler().getPlayerList().isEmpty(); }
        catch (Throwable t) { return false; }
    }

    /** Пересчитывает повторы имён и UUID среди игроков (не чаще 100 мс). */
    private static void scan(MinecraftClient mc) {
        long now = System.currentTimeMillis();
        if (now - lastScan < 100 && !nameCount.isEmpty()) return;
        lastScan = now;
        nameCount.clear();
        uuidCount.clear();
        Set<UUID> seen = new HashSet<>();
        if (mc.world != null) {
            for (Entity e : mc.world.getEntities()) {
                if (!(e instanceof PlayerEntity p)) continue;
                if (p == mc.player) continue;
                if (!seen.add(p.getUuid())) continue;
                String n = p.getName().getString().toLowerCase(Locale.ROOT);
                nameCount.merge(n, 1, Integer::sum);
                uuidCount.merge(p.getUuid(), 1, Integer::sum);
            }
        }
        // записи из Tab, которых нет в мире (скрытые боты); каждого игрока считаем один раз
        if (mc.getNetworkHandler() != null) {
            for (PlayerListEntry ple : mc.getNetworkHandler().getPlayerList()) {
                if (ple.getProfile() == null) continue;
                UUID id = ple.getProfile().getId();
                if (id == null || !seen.add(id)) continue;
                String n = ple.getProfile().getName();
                if (n == null) continue;
                nameCount.merge(n.toLowerCase(Locale.ROOT), 1, Integer::sum);
                uuidCount.merge(id, 1, Integer::sum);
            }
        }
    }

    /** Сбросить кэш (например, при выходе из мира). */
    public static void reset() {
        lastScan = 0;
        nameCount.clear();
        uuidCount.clear();
    }
}
