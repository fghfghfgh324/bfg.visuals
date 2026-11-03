package com.bfgvisuals.launcher;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * Настройки лаунчера. Сохраняются в config.json рядом с лаунчером
 * (или в %APPDATA%/.bfgvisuals, если рядом писать нельзя).
 */
public class LauncherConfig {

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    // ---- сохраняемые поля ----
    public String nickname = "Player";
    public String version = "1.21.1";
    public int ramGb = 4;                 // 2..16
    public String sky = "Ванильное";      // Ванильное | Космос | Закат | Неон
    public String visualColor = "#8A2BE2"; // hex
    public boolean trails = true;
    public boolean beautifulLines = true;
    public boolean glow = true;
    public String minecraftDir = defaultMinecraftDir().toString();
    public String javaPath = "";          // пусто = java из JAVA_HOME / PATH

    // ---- загрузка / сохранение ----

    public static Path configPath() {
        Path local = Paths.get("config.json").toAbsolutePath();
        Path dir = local.getParent();
        if (dir != null && Files.isWritable(dir)) {
            return local;
        }
        String appData = System.getenv("APPDATA");
        Path base = appData != null
                ? Paths.get(appData, ".bfgvisuals")
                : Paths.get(System.getProperty("user.home"), ".bfgvisuals");
        return base.resolve("config.json");
    }

    public static LauncherConfig load() {
        Path path = configPath();
        if (Files.exists(path)) {
            try (Reader r = Files.newBufferedReader(path, StandardCharsets.UTF_8)) {
                LauncherConfig cfg = GSON.fromJson(r, LauncherConfig.class);
                if (cfg != null) {
                    cfg.sanitize();
                    return cfg;
                }
            } catch (Exception e) {
                System.err.println("Не удалось прочитать config.json, используются значения по умолчанию: " + e);
            }
        }
        return new LauncherConfig();
    }

    public void save() {
        sanitize();
        Path path = configPath();
        try {
            Files.createDirectories(path.getParent());
            try (Writer w = Files.newBufferedWriter(path, StandardCharsets.UTF_8)) {
                GSON.toJson(this, w);
            }
        } catch (IOException e) {
            System.err.println("Не удалось сохранить config.json: " + e);
        }
    }

    /** Защита от кривых значений в руками отредактированном config.json. */
    private void sanitize() {
        if (nickname == null || nickname.isBlank()) nickname = "Player";
        if (version == null || version.isBlank()) version = "1.21.1";
        ramGb = Math.max(2, Math.min(16, ramGb));
        if (sky == null) sky = "Ванильное";
        if (visualColor == null || !visualColor.matches("#[0-9a-fA-F]{6}")) visualColor = "#8A2BE2";
        if (minecraftDir == null || minecraftDir.isBlank()) minecraftDir = defaultMinecraftDir().toString();
        if (javaPath == null) javaPath = "";
    }

    public static Path defaultMinecraftDir() {
        String os = System.getProperty("os.name", "").toLowerCase();
        if (os.contains("win")) {
            String appData = System.getenv("APPDATA");
            if (appData != null) return Paths.get(appData, ".minecraft");
        } else if (os.contains("mac")) {
            return Paths.get(System.getProperty("user.home"), "Library", "Application Support", "minecraft");
        }
        return Paths.get(System.getProperty("user.home"), ".minecraft");
    }
}
