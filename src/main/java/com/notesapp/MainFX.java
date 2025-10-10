package com.notesapp;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;
import java.net.URL;
import java.nio.file.Paths;

public class MainFX extends Application {

    @Override
    public void start(Stage stage) {
        System.out.println("========================================");
        System.out.println("🚀 Launching School Notes App GUI");
        System.out.println("Current Working Directory: " + System.getProperty("user.dir"));
        System.out.println("Java Version: " + System.getProperty("java.version"));
        System.out.println("Operating System: " + System.getProperty("os.name"));
        System.out.println("========================================");

        try {
            // --- Locate FXML file ---
            URL fxmlUrl = Thread.currentThread().getContextClassLoader().getResource("main.fxml");
            if (fxmlUrl == null) {
                System.err.println("❌ Could not find main.fxml in classpath (target/classes).");
                System.err.println("Make sure it's under src/main/resources/main.fxml");
                stage.setTitle("❌ FXML Missing");
                stage.setScene(new Scene(new javafx.scene.control.Label("FXML not found!"), 400, 200));
                stage.show();
                return;
            }

            System.out.println("✅ Found FXML: " + fxmlUrl);
            Parent root = FXMLLoader.load(fxmlUrl);
            Scene scene = new Scene(root, 1100, 700);

            // --- Load CSS file ---
            URL cssUrl = Thread.currentThread().getContextClassLoader().getResource("MainView.css");
            if (cssUrl != null) {
                scene.getStylesheets().add(cssUrl.toExternalForm());
                System.out.println("✅ Loaded MainView.css successfully.");
            } else {
                System.out.println("⚠️ MainView.css not found — continuing without styling.");
            }

            // --- Setup and show window ---
            stage.setTitle("School Notes App");
            stage.setScene(scene);
            stage.show();

            System.out.println("✅ GUI launched successfully!");
            System.out.println("If window appears empty, your FXML controls or layout may not have rendered correctly.");
        } catch (Exception e) {
            System.err.println("❌ Exception while launching GUI:");
            e.printStackTrace();
        }
    }

    public static void main(String[] args) {
        System.out.println("Starting JavaFX Application...");

        // --- Detect OS and suggest module-path if missing ---
        String os = System.getProperty("os.name").toLowerCase();
        String javafxPath = "";

        if (os.contains("win")) {
            javafxPath = "C:\\javafx-sdk-25\\lib";
        } else if (os.contains("mac")) {
            javafxPath = "/Library/Java/JavaVirtualMachines/javafx-sdk-25/lib";
        } else {
            javafxPath = "/usr/share/openjfx/lib";
        }

        System.out.println("💡 If you see a 'module not found' error, run with:");
        System.out.println("   java --enable-native-access=ALL-UNNAMED --module-path \"" + javafxPath + "\" --add-modules javafx.controls,javafx.fxml -cp target/classes com.notesapp.MainFX");
        System.out.println("========================================");

        launch(args);
    }
}
