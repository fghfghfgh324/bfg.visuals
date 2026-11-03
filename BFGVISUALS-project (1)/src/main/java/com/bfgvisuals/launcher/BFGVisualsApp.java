package com.bfgvisuals.launcher;

import javafx.animation.Animation;
import javafx.animation.KeyFrame;
import javafx.animation.KeyValue;
import javafx.animation.Timeline;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.effect.DropShadow;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.paint.CycleMethod;
import javafx.scene.paint.LinearGradient;
import javafx.scene.paint.Stop;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.scene.text.Text;
import javafx.scene.text.TextFlow;
import javafx.stage.Stage;
import javafx.util.Duration;

import java.io.IOException;
import java.util.Objects;

/** Главное окно лаунчера BFGVISUALS. */
public class BFGVisualsApp extends Application {

    private LauncherConfig config;
    private Stage stage;
    private Scene scene;

    private TextField nickField;
    private ComboBox<String> versionBox;
    private Button playButton;
    private Label statusLabel;
    private Text logoAccent;

    private Process gameProcess;

    public static void main(String[] args) {
        launch(args);
    }

    @Override
    public void start(Stage primaryStage) {
        this.stage = primaryStage;
        this.config = LauncherConfig.load();

        VBox root = new VBox(18);
        root.setAlignment(Pos.TOP_CENTER);
        root.setPadding(new Insets(34, 44, 30, 44));
        root.getStyleClass().add("main-root");

        root.getChildren().addAll(buildLogo(), buildSubtitle(), buildForm(), buildButtons(), buildStatus());

        StackPane wrapper = new StackPane(root);
        wrapper.getStyleClass().add("root-wrapper");

        scene = new Scene(wrapper, 520, 640);
        scene.getStylesheets().add(Objects.requireNonNull(
                getClass().getResource("/style.css"), "style.css not found").toExternalForm());
        applyAccent(config.visualColor);

        stage.setTitle("BFGVISUALS Launcher");
        stage.setScene(scene);
        stage.setResizable(false);
        stage.setOnCloseRequest(e -> saveFromUi());
        stage.show();
    }

    // ------------------------------------------------------------------ UI

    private TextFlow buildLogo() {
        Text bfg = new Text("BFG");
        bfg.setFont(Font.font("Segoe UI", FontWeight.EXTRA_BOLD, 64));
        bfg.setFill(Color.WHITE);

        logoAccent = new Text("VISUALS");
        logoAccent.setFont(Font.font("Segoe UI", FontWeight.EXTRA_BOLD, 64));
        updateLogoGradient(Color.web(config.visualColor));

        DropShadow glow = new DropShadow(28, Color.web(config.visualColor, 0.75));
        glow.setSpread(0.1);
        TextFlow logo = new TextFlow(bfg, logoAccent);
        logo.setTextAlignment(javafx.scene.text.TextAlignment.CENTER);
        logo.setEffect(glow);

        // Пульсация свечения логотипа
        Timeline pulse = new Timeline(
                new KeyFrame(Duration.ZERO, new KeyValue(glow.radiusProperty(), 18)),
                new KeyFrame(Duration.seconds(1.8), new KeyValue(glow.radiusProperty(), 38)));
        pulse.setAutoReverse(true);
        pulse.setCycleCount(Animation.INDEFINITE);
        pulse.play();
        logo.setUserData(glow);
        return logo;
    }

    private void updateLogoGradient(Color accent) {
        logoAccent.setFill(new LinearGradient(0, 0, 1, 1, true, CycleMethod.NO_CYCLE,
                new Stop(0, accent.brighter()),
                new Stop(1, accent.deriveColor(0, 1, 0.75, 1))));
    }

    private Label buildSubtitle() {
        Label l = new Label("Minecraft Visual Launcher");
        l.getStyleClass().add("subtitle");
        return l;
    }

    private VBox buildForm() {
        Label nickLabel = new Label("НИКНЕЙМ");
        nickLabel.getStyleClass().add("field-label");
        nickField = new TextField(config.nickname);
        nickField.setPromptText("Введите ник (3–16 символов)");
        nickField.setMaxWidth(Double.MAX_VALUE);

        Label verLabel = new Label("ВЕРСИЯ");
        verLabel.getStyleClass().add("field-label");
        versionBox = new ComboBox<>();
        versionBox.getItems().add("1.21.1");
        versionBox.setValue("1.21.1");
        versionBox.setMaxWidth(Double.MAX_VALUE);

        VBox form = new VBox(6, nickLabel, nickField, spacer(8), verLabel, versionBox);
        form.setPadding(new Insets(14, 0, 0, 0));
        return form;
    }

    private VBox buildButtons() {
        playButton = new Button("ИГРАТЬ");
        playButton.getStyleClass().add("play-button");
        playButton.setMaxWidth(Double.MAX_VALUE);
        playButton.setPrefHeight(64);
        playButton.setDefaultButton(true);
        playButton.setOnAction(e -> onPlay());

        Button settingsButton = new Button("Настройки визуала");
        settingsButton.getStyleClass().add("secondary-button");
        settingsButton.setMaxWidth(Double.MAX_VALUE);
        settingsButton.setOnAction(e -> openSettings());

        Button exitButton = new Button("Выход");
        exitButton.getStyleClass().addAll("secondary-button", "exit-button");
        exitButton.setMaxWidth(Double.MAX_VALUE);
        exitButton.setOnAction(e -> {
            saveFromUi();
            Platform.exit();
        });

        VBox box = new VBox(12, playButton, settingsButton, exitButton);
        box.setPadding(new Insets(10, 0, 0, 0));
        return box;
    }

    private Label buildStatus() {
        statusLabel = new Label("Готов к запуску");
        statusLabel.getStyleClass().add("status");
        statusLabel.setWrapText(true);
        return statusLabel;
    }

    private static javafx.scene.layout.Region spacer(double h) {
        javafx.scene.layout.Region r = new javafx.scene.layout.Region();
        r.setMinHeight(h);
        return r;
    }

    // ------------------------------------------------------------------ actions

    private void saveFromUi() {
        config.nickname = nickField.getText().trim();
        config.version = versionBox.getValue();
        config.save();
    }

    private void openSettings() {
        saveFromUi();
        SettingsStage settings = new SettingsStage(stage, config, this::applyAccent);
        settings.showAndWait();
        applyAccent(config.visualColor);
    }

    /** Меняет акцентный цвет всего интерфейса. */
    private void applyAccent(String hex) {
        scene.getRoot().setStyle("-accent: " + hex + ";");
        updateLogoGradient(Color.web(hex));
    }

    private void onPlay() {
        String nick = nickField.getText().trim();
        if (!nick.matches("[A-Za-z0-9_]{3,16}")) {
            showError("Неверный ник",
                    "Ник должен быть от 3 до 16 символов: латинские буквы, цифры и «_».");
            return;
        }
        saveFromUi();

        playButton.setDisable(true);
        playButton.setText("ЗАПУСК…");
        setStatus("Запускаем Minecraft " + config.version + "…");

        // Запуск не в UI-потоке, чтобы окно не зависало
        Thread t = new Thread(() -> {
            try {
                GameLauncher launcher = new GameLauncher(config, System.out::println);
                gameProcess = launcher.launch();
                Platform.runLater(() -> {
                    playButton.setText("ИГРА ЗАПУЩЕНА");
                    setStatus("Игра запущена. Приятной игры, " + nick + "!");
                });
                int code = gameProcess.waitFor();
                Platform.runLater(() -> {
                    playButton.setDisable(false);
                    playButton.setText("ИГРАТЬ");
                    setStatus(code == 0 ? "Игра закрыта" : "Игра завершилась с кодом " + code
                            + " (см. консоль лаунчера)");
                });
            } catch (IOException ex) {
                Platform.runLater(() -> {
                    resetPlay();
                    setStatus("Ошибка запуска");
                    showError("Не удалось запустить игру", ex.getMessage());
                });
            } catch (InterruptedException ex) {
                Thread.currentThread().interrupt();
            }
        }, "bfgvisuals-launch");
        t.setDaemon(true);
        t.start();
    }

    private void resetPlay() {
        playButton.setDisable(false);
        playButton.setText("ИГРАТЬ");
    }

    private void setStatus(String text) {
        statusLabel.setText(text);
    }

    private void showError(String header, String text) {
        Alert a = new Alert(Alert.AlertType.ERROR);
        a.initOwner(stage);
        a.setTitle("BFGVISUALS");
        a.setHeaderText(header);
        a.setContentText(text);
        a.getDialogPane().getStylesheets().add(scene.getStylesheets().get(0));
        a.getDialogPane().getStyleClass().add("dialog");
        a.showAndWait();
    }
}
