package com.example.projetjavafx;

import javafx.application.Platform;
import javafx.scene.control.Alert;

import java.io.*;
import java.util.HashMap;
import java.util.Map;

public class ConversationStorage {
    private static final String SAVE_DIR = "conversations";
    private final Map<String, String> memoryStore = new HashMap<>();

    public ConversationStorage() {
        File dir = new File(SAVE_DIR);
        if (!dir.exists()) {
            dir.mkdir();
        }
    }
    public synchronized void save(String name, String content) {
        File dir = new File(SAVE_DIR);
        if (!dir.exists() && !dir.mkdirs()) {
            showAlert("Erreur", "Impossible de créer le dossier de sauvegarde");
            return;
        }

        File file = new File(SAVE_DIR + "/" + name + ".txt");
        File tempFile = new File(SAVE_DIR + "/" + name + ".tmp");

        try {
            // Écrit d'abord dans un fichier temporaire
            try (BufferedWriter writer = new BufferedWriter(new FileWriter(tempFile))) {
                writer.write(content);
            }

            // Remplace le fichier existant de manière atomique
            if (file.exists() && !file.delete()) {
                throw new IOException("Échec de suppression de l'ancien fichier");
            }

            if (!tempFile.renameTo(file)) {
                throw new IOException("Échec du renommage du fichier temporaire");
            }

            memoryStore.put(name, content);
        } catch (IOException e) {
            showAlert("Erreur de Sauvegarde", "Impossible de sauvegarder la conversation: " + e.getMessage());
            tempFile.delete(); // Nettoyage
        }
    }

    private void showAlert(String title, String message) {
        Platform.runLater(() -> {
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setTitle(title);
            alert.setHeaderText(null);
            alert.setContentText(message);
            alert.showAndWait();
        });
    }
    public String load(String name) {
        try (BufferedReader reader = new BufferedReader(new FileReader(SAVE_DIR + "/" + name + ".txt"))) {
            StringBuilder content = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                content.append(line).append("\n");
            }
            return content.toString();
        } catch (IOException e) {
            return memoryStore.getOrDefault(name, "");
        }
    }

    public String[] listConversations() {
        File dir = new File(SAVE_DIR);
        String[] files = dir.list((d, name) -> name.endsWith(".txt"));
        if (files == null) return new String[0];

        // Enlève l'extension .txt pour l'affichage
        for (int i = 0; i < files.length; i++) {
            files[i] = files[i].replace(".txt", "");
        }
        return files;
    }

    public void delete(String name) {
        memoryStore.remove(name);
        File file = new File(SAVE_DIR + "/" + name + ".txt");
        if (file.exists()) {
            file.delete();
        }
    }




}
