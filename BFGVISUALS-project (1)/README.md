# BFGVISUALS Launcher

Кастомный лаунчер Minecraft **1.21.1** на **JavaFX 21** (тёмный GUI, настройки визуала, `config.json`).

```
src/main/java/com/bfgvisuals/launcher/
  Main.java            – точка входа для jar/exe (без наследования Application)
  BFGVisualsApp.java   – главное окно (логотип, ник, версия, Играть / Настройки / Выход)
  SettingsStage.java   – окно настроек, вкладка «BFGVISUALS» (+ вкладка «Игра»)
  GameLauncher.java    – запуск Minecraft через ProcessBuilder
  LauncherConfig.java  – чтение/запись config.json (Gson)
src/main/resources/style.css – тёмная тема
.github/workflows/build.yml  – сборка jar + exe на GitHub
```

## 1. Сборка jar на GitHub (без установки чего-либо)

1. Создайте репозиторий на GitHub, загрузите в него **содержимое** этого архива (папка `.github` должна лежать в корне репозитория).
2. Откройте вкладку **Actions** → workflow **Build BFGVISUALS** (запускается сам после push, либо кнопка *Run workflow*).
3. Через 2–4 минуты внизу страницы запуска, в разделе **Artifacts**, появятся:
   * `BFGVISUALS-jar` → `bfgvisuals-1.0.0-all.jar`
   * `BFGVISUALS-exe` → `BFGVISUALS-windows.zip` (внутри `BFGVISUALS/BFGVISUALS.exe` со встроенной Java, ничего ставить не нужно)
4. Чтобы получить готовый релиз со ссылкой для скачивания: `git tag v1.0.0 && git push origin v1.0.0`.

> Сборка идёт на Windows-раннере: JavaFX кладёт в jar нативные библиотеки той ОС, на которой собирали. Jar с GitHub работает на Windows.

Запуск jar: `java -jar bfgvisuals-1.0.0-all.jar` (нужна Java 21+).

## 2. Сборка .exe локально

**jpackage (рекомендуется):**
```bat
mvn -DskipTests package
mkdir stage & copy target\bfgvisuals-1.0.0-all.jar stage\
jpackage --type app-image --name BFGVISUALS --input stage ^
  --main-jar bfgvisuals-1.0.0-all.jar --main-class com.bfgvisuals.launcher.Main --dest dist
```
Получится `dist\BFGVISUALS\BFGVISUALS.exe`. Для установщика замените `app-image` на `exe` (нужен установленный [WiX Toolset](https://wixtoolset.org/)) и добавьте `--win-shortcut --win-menu`.

**Launch4j:** Main class `com.bfgvisuals.launcher.Main`, jar `bfgvisuals-1.0.0-all.jar`, Min JRE `21`. Минус: Java должна быть установлена у пользователя.

## 3. Что делает «Играть»

```
java -Xmx<N>G -Xms<N>G -Djava.library.path=… -Dbfgvisuals.* -cp <libraries/**/*.jar + versions/1.21.1/1.21.1.jar>
     net.minecraft.client.main.Main --username <ник> --version 1.21.1
     --gameDir … --assetsDir … --assetIndex … --uuid … --accessToken 0 …
```
Помимо аргументов из ТЗ добавлены обязательные для 1.21.1 (`--gameDir`, `--assetsDir`, `--assetIndex`, `--uuid`, `--accessToken`) и полный classpath из `libraries/`: без них клиент не стартует. Нужна установленная версия 1.21.1 (один раз запустите её официальным лаунчером).

Это **оффлайн-запуск** (`--accessToken 0`): лицензионные серверы не пустят, зайти можно в одиночную игру и на оффлайн-серверы.

## 4. Как привязать визуалы к самому Minecraft

Лаунчер только **передаёт** настройки. Рисует их мод. Ваниль ничего не знает про «космос» или «трейлы», поэтому нужен клиентский мод (проще всего **Fabric**).

Лаунчер отдаёт настройки двумя способами:
* системные свойства JVM: `bfgvisuals.sky` (`vanilla|space|sunset|neon`), `bfgvisuals.color` (`#RRGGBB`), `bfgvisuals.trails`, `bfgvisuals.lines`, `bfgvisuals.glow`;
* файл `.minecraft/bfgvisuals.json` с теми же полями.

Шаги:
1. Установите Fabric Loader 1.21.1 и Fabric API.
2. Создайте мод по [шаблону Fabric](https://github.com/FabricMC/fabric-example-mod) (ветка 1.21) и прочитайте настройки — пример в `docs/BFGVisualsMod-example.java`.
3. Реализуйте эффекты: небо через `DimensionRenderingRegistry.registerSkyRenderer`, трейлы через частицы по тику клиента, свечение/линии через `WorldRenderEvents`/шейдеры.
4. Соберите мод и положите jar в `.minecraft/mods/`.
5. Чтобы лаунчер загружал Fabric, в `GameLauncher` нужно заменить `MAIN_CLASS` на `net.fabricmc.loader.impl.launch.knot.KnotClient` и добавить в classpath библиотеки Fabric Loader (Fabric Installer кладёт их в `libraries/` и создаёт `versions/fabric-loader-…-1.21.1/*.json`). Ванильный `net.minecraft.client.main.Main` моды **не загружает**.

Альтернатива без своего мода: шейдерпак (Iris) для неба и свечения, лаунчер при этом просто запускает игру.

## config.json

```json
{
  "nickname": "Player", "version": "1.21.1", "ramGb": 4,
  "sky": "Космос", "visualColor": "#8A2BE2",
  "trails": true, "beautifulLines": true, "glow": true,
  "minecraftDir": "C:\\Users\\you\\AppData\\Roaming\\.minecraft", "javaPath": ""
}
```
Файл создаётся рядом с лаунчером (или в `%APPDATA%\.bfgvisuals`, если рядом нельзя писать).
