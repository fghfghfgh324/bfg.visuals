package com.bfgvisuals.launcher;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Consumer;
import java.util.stream.Stream;

/**
 * Запускает Minecraft через ProcessBuilder:
 *
 *   java -Xmx<N>G -Xms<N>G -cp <путь_к_майнкрафту> net.minecraft.client.main.Main
 *        --username <ник> --version 1.21.1 [+ служебные аргументы]
 *
 * Для <путь_к_майнкрафту> собирается полный classpath: клиентский jar версии
 * + все .jar из libraries/. Без библиотек (LWJGL, Netty, Brigadier ...) клиент не стартует.
 *
 * Настройки визуала передаются в игру двумя способами:
 *   1) системные свойства -Dbfgvisuals.*  (читает мод через System.getProperty)
 *   2) файл <gameDir>/bfgvisuals.json     (читает мод при старте / по таймеру)
 */
public class GameLauncher {

    /** Главный класс ванильного клиента. Для Fabric: net.fabricmc.loader.impl.launch.knot.KnotClient */
    public static final String MAIN_CLASS = "net.minecraft.client.main.Main";

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    private final LauncherConfig cfg;
    private final Consumer<String> log;

    public GameLauncher(LauncherConfig cfg, Consumer<String> log) {
        this.cfg = cfg;
        this.log = log != null ? log : s -> {};
    }

    /** Собирает команду и стартует процесс. Бросает IOException с понятным текстом. */
    public Process launch() throws IOException {
        Path mcDir = Paths.get(cfg.minecraftDir);
        if (!Files.isDirectory(mcDir)) {
            throw new IOException("Папка Minecraft не найдена: " + mcDir
                    + "\nУкажите её в настройках или запустите игру официальным лаунчером один раз.");
        }

        String version = cfg.version;
        Path versionJar = mcDir.resolve("versions").resolve(version).resolve(version + ".jar");
        if (!Files.isRegularFile(versionJar)) {
            throw new IOException("Не найден клиент " + versionJar
                    + "\nУстановите Minecraft " + version + " через официальный лаунчер.");
        }

        List<Path> classpath = buildClasspath(mcDir, versionJar);
        Path nativesDir = prepareNativesDir(mcDir, version);

        writeVisualsFile(mcDir);

        List<String> cmd = new ArrayList<>();
        cmd.add(resolveJava());

        // --- память ---
        cmd.add("-Xmx" + cfg.ramGb + "G");
        cmd.add("-Xms" + cfg.ramGb + "G");

        // --- служебные JVM-флаги ---
        cmd.add("-Djava.library.path=" + nativesDir);
        cmd.add("-Dminecraft.launcher.brand=BFGVISUALS");
        cmd.add("-Dminecraft.launcher.version=1.0.0");

        // --- настройки визуала для мода ---
        cmd.add("-Dbfgvisuals.enabled=true");
        cmd.add("-Dbfgvisuals.sky=" + skyId(cfg.sky));
        cmd.add("-Dbfgvisuals.color=" + cfg.visualColor);
        cmd.add("-Dbfgvisuals.trails=" + cfg.trails);
        cmd.add("-Dbfgvisuals.lines=" + cfg.beautifulLines);
        cmd.add("-Dbfgvisuals.glow=" + cfg.glow);

        // --- classpath ---
        cmd.add("-cp");
        StringBuilder cp = new StringBuilder();
        for (Path p : classpath) {
            if (cp.length() > 0) cp.append(File.pathSeparatorChar);
            cp.append(p);
        }
        cmd.add(cp.toString());

        // --- главный класс и аргументы игры ---
        cmd.add(MAIN_CLASS);
        cmd.add("--username");
        cmd.add(cfg.nickname);
        cmd.add("--version");
        cmd.add(version);

        // Обязательные аргументы 1.21.1 для оффлайн-запуска
        cmd.add("--gameDir");
        cmd.add(mcDir.toString());
        cmd.add("--assetsDir");
        cmd.add(mcDir.resolve("assets").toString());
        cmd.add("--assetIndex");
        cmd.add(assetIndexFor(version));
        cmd.add("--uuid");
        cmd.add(offlineUuid(cfg.nickname));
        cmd.add("--accessToken");
        cmd.add("0");
        cmd.add("--userType");
        cmd.add("legacy");
        cmd.add("--versionType");
        cmd.add("BFGVISUALS");

        log.accept("Запуск: " + cmd.get(0) + " -Xmx" + cfg.ramGb + "G ... " + MAIN_CLASS);

        ProcessBuilder pb = new ProcessBuilder(cmd);
        pb.directory(mcDir.toFile());
        pb.redirectErrorStream(true);
        Process process = pb.start();

        // Читаем вывод игры в отдельном потоке (иначе буфер забьётся и игра зависнет)
        Thread reader = new Thread(() -> {
            try (BufferedReader br = new BufferedReader(
                    new InputStreamReader(process.getInputStream(), StandardCharsets.UTF_8))) {
                String line;
                while ((line = br.readLine()) != null) log.accept(line);
            } catch (IOException ignored) {
            }
        }, "bfgvisuals-game-output");
        reader.setDaemon(true);
        reader.start();

        return process;
    }

    // ------------------------------------------------------------------ helpers

    private List<Path> buildClasspath(Path mcDir, Path versionJar) throws IOException {
        List<Path> result = new ArrayList<>();
        Path libs = mcDir.resolve("libraries");
        if (Files.isDirectory(libs)) {
            try (Stream<Path> s = Files.walk(libs)) {
                s.filter(p -> p.toString().endsWith(".jar"))
                        // natives-классификаторы грузятся из natives-папки / LWJGL сам
                        .filter(p -> !p.getFileName().toString().contains("-sources"))
                        .sorted()
                        .forEach(result::add);
            }
        } else {
            log.accept("ВНИМАНИЕ: папка libraries не найдена — игра, скорее всего, не запустится.");
        }
        result.add(versionJar); // клиент — последним
        return result;
    }

    /**
     * Папка natives. Для 1.21.1 LWJGL распаковывает натив сам, но папка нужна для -Djava.library.path.
     * Если в versions/<v>/natives пусто — это нормально.
     */
    private Path prepareNativesDir(Path mcDir, String version) throws IOException {
        Path natives = mcDir.resolve("versions").resolve(version).resolve("natives");
        Files.createDirectories(natives);
        return natives;
    }

    private String resolveJava() throws IOException {
        if (cfg.javaPath != null && !cfg.javaPath.isBlank()) {
            return cfg.javaPath;
        }
        // 1.21.1 требует Java 21+. Берём ту же, на которой работает лаунчер.
        String home = System.getProperty("java.home");
        boolean win = System.getProperty("os.name", "").toLowerCase().contains("win");
        Path java = Paths.get(home, "bin", win ? "java.exe" : "java");
        if (Files.isExecutable(java)) return java.toString();
        return "java";
    }

    private String assetIndexFor(String version) {
        // Для 1.21.1 индекс ассетов называется "17"; пробуем прочитать из version.json
        Path json = Paths.get(cfg.minecraftDir, "versions", version, version + ".json");
        try {
            if (Files.isRegularFile(json)) {
                @SuppressWarnings("unchecked")
                Map<String, Object> root = GSON.fromJson(Files.readString(json), Map.class);
                Object assets = root.get("assets");
                if (assets instanceof String s && !s.isBlank()) return s;
            }
        } catch (Exception ignored) {
        }
        return "17";
    }

    private static String offlineUuid(String nick) {
        return UUID.nameUUIDFromBytes(("OfflinePlayer:" + nick).getBytes(StandardCharsets.UTF_8)).toString();
    }

    private static String skyId(String display) {
        return switch (display) {
            case "Космос" -> "space";
            case "Закат" -> "sunset";
            case "Неон" -> "neon";
            default -> "vanilla";
        };
    }

    /** <gameDir>/bfgvisuals.json — мод читает этот файл. */
    private void writeVisualsFile(Path mcDir) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("enabled", true);
        m.put("sky", skyId(cfg.sky));
        m.put("color", cfg.visualColor);
        m.put("trails", cfg.trails);
        m.put("lines", cfg.beautifulLines);
        m.put("glow", cfg.glow);
        try {
            Files.writeString(mcDir.resolve("bfgvisuals.json"), GSON.toJson(m), StandardCharsets.UTF_8);
        } catch (IOException e) {
            log.accept("Не удалось записать bfgvisuals.json: " + e.getMessage());
        }
    }
}
