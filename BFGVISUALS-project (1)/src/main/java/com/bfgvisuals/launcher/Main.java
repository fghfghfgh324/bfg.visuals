package com.bfgvisuals.launcher;

/**
 * Точка входа для fat-jar / jpackage / Launch4j.
 * Класс НЕ наследует javafx.application.Application — иначе при запуске из
 * обычного jar JavaFX выдаёт "JavaFX runtime components are missing".
 */
public final class Main {
    private Main() {}

    public static void main(String[] args) {
        BFGVisualsApp.main(args);
    }
}
