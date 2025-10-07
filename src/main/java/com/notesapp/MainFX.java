package com.notesapp;
import javafx.application.Application; import javafx.fxml.FXMLLoader; import javafx.scene.*; import javafx.stage.Stage;

public class MainFX extends Application {
  @Override public void start(Stage stage) throws Exception {
    Parent root = FXMLLoader.load(getClass().getResource("/main.fxml"));
    Scene scene = new Scene(root, 1100, 700);
    scene.getStylesheets().add(getClass().getResource("/styles.css").toExternalForm());
    stage.setTitle("School Notes App");
    stage.setScene(scene);
    stage.show();
  }
  public static void main(String[] args){ launch(args); }
}
