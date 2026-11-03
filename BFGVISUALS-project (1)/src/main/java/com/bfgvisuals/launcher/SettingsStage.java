package com.bfgvisuals.launcher;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ColorPicker;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.Slider;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.paint.CycleMethod;
import javafx.scene.paint.LinearGradient;
import javafx.scene.paint.Stop;
import javafx.scene.shape.Rectangle;
import javafx.stage.DirectoryChooser;
import javafx.stage.Modality;
import javafx.stage.Stage;

import java.io.File;
import java.util.Objects;
import java.util.function.Consumer;

/** Окно настроек. Вкладка «BFGVISUALS» + вкладка «Игра». */
public class SettingsStage extends Stage {

    private final LauncherConfig cfg;
    private final Consumer<String> onAccentChanged;

    private Slider ramSlider;
    private Label ramValue;
    private ComboBox<String> skyBox;
    private CheckBox trailsCheck;
    private CheckBox linesCheck;
    private CheckBox glowCheck;
    private ColorPicker colorPicker;
    private Rectangle preview;

    private TextField mcDirField;
    private TextField javaField;

    public SettingsStage(Stage owner, LauncherConfig cfg, Consumer<String> onAccentChanged) {
        this.cfg = cfg;
        this.onAccentChanged = onAccentChanged;

        initOwner(owner);
        initModality(Modality.WINDOW_MODAL);
        setTitle("Настройки — BFGVISUALS");
        setResizable(false);

        TabPane tabs = new TabPane(visualsTab(), gameTab());
        tabs.setTabClosingPolicy(TabPane.TabClosingPolicy.UNAVAILABLE);

        Button save = new Button("Сохранить");
        save.getStyleClass().add("play-button");
        save.setPrefHeight(46);
        save.setMaxWidth(Double.MAX_VALUE);
        save.setOnAction(e -> {
            apply();
            close();
        });

        Button cancel = new Button("Отмена");
        cancel.getStyleClass().add("secondary-button");
        cancel.setOnAction(e -> {
            // откат цвета, если пользователь крутил палитру
            onAccentChanged.accept(cfg.visualColor);
            close();
        });
        cancel.setPrefHeight(46);

        HBox bottom = new HBox(12, cancel, save);
        HBox.setHgrow(save, Priority.ALWAYS);
        bottom.setPadding(new Insets(14, 20, 20, 20));

        VBox root = new VBox(tabs, bottom);
        root.getStyleClass().add("main-root");
        VBox.setVgrow(tabs, Priority.ALWAYS);

        Scene scene = new Scene(new StackPane(root), 540, 640);
        scene.getStylesheets().add(Objects.requireNonNull(
                getClass().getResource("/style.css")).toExternalForm());
        scene.getRoot().setStyle("-accent: " + cfg.visualColor + ";");
        setScene(scene);
    }

    // ------------------------------------------------------------ вкладка BFGVISUALS

    private Tab visualsTab() {
        // --- ОЗУ ---
        ramSlider = new Slider(2, 16, cfg.ramGb);
        ramSlider.setMajorTickUnit(2);
        ramSlider.setMinorTickCount(1);
        ramSlider.setSnapToTicks(true);
        ramSlider.setShowTickMarks(true);
        ramSlider.setShowTickLabels(true);
        ramValue = new Label(cfg.ramGb + " ГБ");
        ramValue.getStyleClass().add("value-label");
        ramSlider.valueProperty().addListener((o, a, b) -> ramValue.setText(b.intValue() + " ГБ"));

        HBox ramHeader = new HBox(sectionLabel("ВЫДЕЛЕННАЯ ОЗУ"), grow(), ramValue);
        ramHeader.setAlignment(Pos.CENTER_LEFT);

        // --- небо ---
        skyBox = new ComboBox<>();
        skyBox.getItems().addAll("Ванильное", "Космос", "Закат", "Неон");
        skyBox.setValue(skyBox.getItems().contains(cfg.sky) ? cfg.sky : "Ванильное");
        skyBox.setMaxWidth(Double.MAX_VALUE);

        // --- чекбоксы ---
        trailsCheck = new CheckBox("Трейлы за игроком");
        trailsCheck.setSelected(cfg.trails);
        linesCheck = new CheckBox("Красивые линии");
        linesCheck.setSelected(cfg.beautifulLines);
        glowCheck = new CheckBox("Свечение");
        glowCheck.setSelected(cfg.glow);

        // --- цвет ---
        colorPicker = new ColorPicker(Color.web(cfg.visualColor));
        colorPicker.setMaxWidth(Double.MAX_VALUE);
        colorPicker.valueProperty().addListener((o, a, c) -> {
            onAccentChanged.accept(toHex(c));
            updatePreview();
        });

        // --- превью неба ---
        preview = new Rectangle(440, 70);
        preview.setArcWidth(16);
        preview.setArcHeight(16);
        skyBox.valueProperty().addListener((o, a, b) -> updatePreview());
        updatePreview();

        VBox content = new VBox(10,
                ramHeader, ramSlider,
                spacer(8),
                sectionLabel("КАСТОМНОЕ НЕБО"), skyBox,
                spacer(8),
                sectionLabel("ВИЗУАЛ"), trailsCheck, linesCheck, glowCheck,
                spacer(8),
                sectionLabel("ЦВЕТ ВИЗУАЛА"), colorPicker,
                spacer(8),
                sectionLabel("ПРЕДПРОСМОТР"), preview);
        content.setPadding(new Insets(22, 24, 10, 24));

        Tab tab = new Tab("BFGVISUALS", content);
        return tab;
    }

    private void updatePreview() {
        Color c = colorPicker != null ? colorPicker.getValue() : Color.web(cfg.visualColor);
        Color top;
        Color bottom;
        switch (skyBox.getValue()) {
            case "Космос" -> { top = Color.web("#02010a"); bottom = Color.web("#1b1042"); }
            case "Закат"  -> { top = Color.web("#3b1d5a"); bottom = Color.web("#ff8a3d"); }
            case "Неон"   -> { top = Color.web("#0a0020"); bottom = c.deriveColor(0, 1, 0.9, 1); }
            default       -> { top = Color.web("#4a90d9"); bottom = Color.web("#bfe3ff"); }
        }
        preview.setFill(new LinearGradient(0, 0, 0, 1, true, CycleMethod.NO_CYCLE,
                new Stop(0, top), new Stop(1, bottom)));
        preview.setStroke(c);
        preview.setStrokeWidth(2);
    }

    // ------------------------------------------------------------ вкладка «Игра»

    private Tab gameTab() {
        mcDirField = new TextField(cfg.minecraftDir);
        Button browse = new Button("Обзор…");
        browse.getStyleClass().add("secondary-button");
        browse.setOnAction(e -> {
            DirectoryChooser dc = new DirectoryChooser();
            dc.setTitle("Папка .minecraft");
            File cur = new File(mcDirField.getText());
            if (cur.isDirectory()) dc.setInitialDirectory(cur);
            File dir = dc.showDialog(this);
            if (dir != null) mcDirField.setText(dir.getAbsolutePath());
        });
        HBox mcRow = new HBox(8, mcDirField, browse);
        HBox.setHgrow(mcDirField, Priority.ALWAYS);

        javaField = new TextField(cfg.javaPath);
        javaField.setPromptText("Пусто = Java, на которой запущен лаунчер (нужна 21+)");

        VBox content = new VBox(10,
                sectionLabel("ПАПКА MINECRAFT (.minecraft)"), mcRow,
                spacer(8),
                sectionLabel("ПУТЬ К JAVA (необязательно)"), javaField);
        content.setPadding(new Insets(22, 24, 10, 24));
        return new Tab("Игра", content);
    }

    // ------------------------------------------------------------ сохранение

    private void apply() {
        cfg.ramGb = (int) Math.round(ramSlider.getValue());
        cfg.sky = skyBox.getValue();
        cfg.trails = trailsCheck.isSelected();
        cfg.beautifulLines = linesCheck.isSelected();
        cfg.glow = glowCheck.isSelected();
        cfg.visualColor = toHex(colorPicker.getValue());
        cfg.minecraftDir = mcDirField.getText().trim();
        cfg.javaPath = javaField.getText().trim();
        cfg.save();
    }

    // ------------------------------------------------------------ helpers

    private static String toHex(Color c) {
        return String.format("#%02X%02X%02X",
                (int) Math.round(c.getRed() * 255),
                (int) Math.round(c.getGreen() * 255),
                (int) Math.round(c.getBlue() * 255));
    }

    private static Label sectionLabel(String text) {
        Label l = new Label(text);
        l.getStyleClass().add("field-label");
        return l;
    }

    private static Region spacer(double h) {
        Region r = new Region();
        r.setMinHeight(h);
        return r;
    }

    private static Region grow() {
        Region r = new Region();
        HBox.setHgrow(r, Priority.ALWAYS);
        return r;
    }
}
