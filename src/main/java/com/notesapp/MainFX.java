package com.notesapp;
import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.stage.Stage;
import java.nio.file.Paths;
public class MainFX extends Application {
    @Override
    public void start(Stage stage) throws Exception {
        System.out.println("Starting JavaFX Application...");
        System.out.println("========================================");
        System.out.println("Launching School Notes App GUI");
        System.out.println("Current Working Directory: " + Paths.get("").toAbsolutePath());
        System.out.println("========================================");
        FXMLLoader loader = new FXMLLoader(getClass().getResource("/home.fxml"));
        Scene scene = new Scene(loader.load(), 1100, 750);
        stage.setScene(scene);
        stage.setTitle("School Notes App");
        stage.show();
    }
    public static void main(String[] args) {
        launch();
    }
}


