// ПРИМЕР-ЗАГОТОВКА клиентского Fabric-мода (1.21.1). Не проверялась сборкой:
// имена классов Yarn/Fabric API могут слегка отличаться в вашей версии — сверяйтесь с шаблоном мода.
package com.bfgvisuals.mod;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.client.MinecraftClient;
import net.minecraft.particle.ParticleTypes;

import java.nio.file.Files;
import java.nio.file.Path;

public class BFGVisualsMod implements ClientModInitializer {

    /** Настройки от лаунчера: сперва -Dbfgvisuals.*, затем (если нет) bfgvisuals.json. */
    public static String sky = System.getProperty("bfgvisuals.sky", "vanilla");
    public static String color = System.getProperty("bfgvisuals.color", "#8A2BE2");
    public static boolean trails = Boolean.parseBoolean(System.getProperty("bfgvisuals.trails", "true"));
    public static boolean lines = Boolean.parseBoolean(System.getProperty("bfgvisuals.lines", "true"));
    public static boolean glow = Boolean.parseBoolean(System.getProperty("bfgvisuals.glow", "true"));

    @Override
    public void onInitializeClient() {
        // Если свойств нет (игру запустили не нашим лаунчером) — можно прочитать
        // <gameDir>/bfgvisuals.json любым JSON-парсером (Gson есть в Minecraft).
        Path file = MinecraftClient.getInstance().runDirectory.toPath().resolve("bfgvisuals.json");
        if (Files.exists(file) && System.getProperty("bfgvisuals.sky") == null) {
            // TODO: new Gson().fromJson(Files.readString(file), ...) и заполнить поля выше
        }

        // Трейлы за игроком: частицы в позиции игрока каждый тик
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            if (!trails || client.player == null || client.world == null) return;
            client.world.addParticle(ParticleTypes.END_ROD,
                    client.player.getX(), client.player.getY() + 0.1, client.player.getZ(),
                    0, 0, 0);
        });

        // Кастомное небо: DimensionRenderingRegistry.registerSkyRenderer(World.OVERWORLD, ctx -> { ... });
        // Рисуйте градиент/звёзды по значению `sky` и цвету `color` (см. fabric-rendering-v1).
    }
}
