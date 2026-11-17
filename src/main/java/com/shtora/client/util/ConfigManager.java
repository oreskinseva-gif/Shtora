package com.shtora.client.util;

import com.shtora.client.Module;
import com.shtora.client.ModuleManager;
import com.shtora.client.setting.BoolSetting;
import com.shtora.client.setting.ColorSetting;
import com.shtora.client.setting.KeybindSetting;
import com.shtora.client.setting.ModeSetting;
import com.shtora.client.setting.NumberSetting;
import com.shtora.client.setting.Setting;
import net.fabricmc.loader.api.FabricLoader;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Конфиги клиента (кнопка «CFG» в меню).
 *
 * Состояние сохраняется в простой текстовый файл:
 * <pre>
 *   hue=200.0
 *   m.KillAura=1
 *   m.KillAura.0=true
 *   m.KillAura.2=1
 * </pre>
 * где m.&lt;модуль&gt; — включён/выключен, m.&lt;модуль&gt;.&lt;номер&gt; — значение настройки
 * (номер — её позиция в списке настроек модуля).
 *
 * Файлы лежат в &lt;папка_конфигов_игры&gt;/shtora/configs/&lt;имя&gt;.cfg
 */
public final class ConfigManager {
    private ConfigManager() {}

    private static Path dir() {
        return FabricLoader.getInstance().getConfigDir().resolve("shtora").resolve("configs");
    }

    private static Path file(String name) {
        return dir().resolve(safe(name) + ".cfg");
    }

    /** Оставляет в имени только буквы, цифры, пробел, дефис и подчёркивание. */
    public static String safe(String name) {
        StringBuilder sb = new StringBuilder();
        for (char c : name.trim().toCharArray()) {
            if (Character.isLetterOrDigit(c) || c == ' ' || c == '-' || c == '_') sb.append(c);
        }
        String s = sb.toString().trim();
        if (s.length() > 24) s = s.substring(0, 24).trim();
        return s;
    }

    /** Имена сохранённых конфигов (по алфавиту). */
    public static List<String> list() {
        List<String> out = new ArrayList<>();
        Path d = dir();
        if (!Files.isDirectory(d)) return out;
        try (DirectoryStream<Path> ds = Files.newDirectoryStream(d, "*.cfg")) {
            for (Path p : ds) {
                String n = p.getFileName().toString();
                out.add(n.substring(0, n.length() - 4));
            }
        } catch (Throwable ignored) {
        }
        Collections.sort(out, String.CASE_INSENSITIVE_ORDER);
        return out;
    }

    /** Сохраняет текущие настройки под именем name. Возвращает true при успехе. */
    public static boolean save(String name) {
        String s = safe(name);
        if (s.isEmpty()) return false;
        StringBuilder sb = new StringBuilder("#SHTORA config\n");
        if (ModuleManager.ui != null) sb.append("hue=").append(ModuleManager.ui.hue.get()).append('\n');
        for (Module m : ModuleManager.modules) {
            sb.append("m.").append(m.name).append('=').append(m.isEnabled() ? 1 : 0).append('\n');
            for (int i = 0; i < m.settings.size(); i++) {
                Setting st = m.settings.get(i);
                String v;
                if (st instanceof BoolSetting b) v = b.get() ? "true" : "false";
                else if (st instanceof NumberSetting n) v = String.valueOf(n.get());
                else if (st instanceof ModeSetting mo) v = String.valueOf(mo.index());
                else if (st instanceof ColorSetting cs) v = cs.serialize();
                else if (st instanceof KeybindSetting ks) v = String.valueOf(ks.get());
                else continue;
                sb.append("m.").append(m.name).append('.').append(i).append('=').append(v).append('\n');
            }
        }
        try {
            Files.createDirectories(dir());
            Files.writeString(file(s), sb.toString(), StandardCharsets.UTF_8);
            return true;
        } catch (IOException e) {
            return false;
        }
    }

    /** Загружает конфиг по имени. Возвращает true, если файл прочитан. */
    public static boolean load(String name) {
        Path p = file(name);
        if (!Files.isRegularFile(p)) return false;
        try {
            for (String line : Files.readAllLines(p, StandardCharsets.UTF_8)) {
                int eq = line.indexOf('=');
                if (eq <= 0) continue;
                String key = line.substring(0, eq).trim();
                String val = line.substring(eq + 1).trim();
                if (key.equals("hue")) {
                    if (ModuleManager.ui != null) {
                        try { ModuleManager.ui.hue.set(Double.parseDouble(val)); } catch (NumberFormatException ignored) {}
                    }
                    continue;
                }
                if (!key.startsWith("m.")) continue;
                String rest = key.substring(2);
                int dot = rest.indexOf('.');
                if (dot < 0) {
                    Module m = byName(rest);
                    if (m != null) m.setEnabled(val.equals("1") || val.equalsIgnoreCase("true"));
                } else {
                    Module m = byName(rest.substring(0, dot));
                    if (m == null) continue;
                    int idx;
                    try { idx = Integer.parseInt(rest.substring(dot + 1)); } catch (NumberFormatException e) { continue; }
                    if (idx < 0 || idx >= m.settings.size()) continue;
                    apply(m.settings.get(idx), val);
                }
            }
            return true;
        } catch (IOException e) {
            return false;
        }
    }

    private static void apply(Setting s, String val) {
        try {
            if (s instanceof BoolSetting b) {
                boolean want = val.equalsIgnoreCase("true") || val.equals("1");
                if (b.get() != want) b.toggle();
            } else if (s instanceof NumberSetting n) {
                n.set(Double.parseDouble(val));
            } else if (s instanceof ModeSetting mo) {
                mo.set(Integer.parseInt(val));
            } else if (s instanceof ColorSetting cs) {
                cs.deserialize(val);
            } else if (s instanceof KeybindSetting ks) {
                ks.set(Integer.parseInt(val));
            }
        } catch (NumberFormatException ignored) {
        }
    }

    public static boolean delete(String name) {
        try {
            return Files.deleteIfExists(file(name));
        } catch (IOException e) {
            return false;
        }
    }

    /** Выключает все модули (кроме клиентских вроде Interface). */
    public static void disableAll() {
        for (Module m : ModuleManager.modules) {
            if (m == ModuleManager.ui) continue;
            m.setEnabled(false);
        }
    }

    private static Module byName(String name) {
        for (Module m : ModuleManager.modules) if (m.name.equals(name)) return m;
        return null;
    }
}
