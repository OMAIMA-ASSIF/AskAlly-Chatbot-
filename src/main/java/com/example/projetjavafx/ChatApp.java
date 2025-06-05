package com.example.projetjavafx;
import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;
import java.io.InputStream;
import javafx.scene.image.Image;

public class ChatApp extends Application {
    @Override
    public void start(Stage stage) throws Exception {
        InputStream iconStream = getClass().getResourceAsStream("/com/example/projetjavafx/Logo.png");
        Image icon = new Image(iconStream);
        stage.getIcons().add(icon);
        Parent root = FXMLLoader.load(getClass().getResource("/com/example/projetjavafx/chat.fxml"));
        stage.setTitle("AskAlly");
        stage.setScene(new Scene(root, 600, 500));
        stage.show();
    }

    public static void main(String[] args) {
        try {
            Runtime.getRuntime().exec("ollama pull llama3.2");
        } catch (Exception e) {
            System.err.println("Note: Could not execute ollama pull command: " + e.getMessage());
            // Continue launching anyway - the app will handle missing model later
        }
        launch(args);
    }
}
