package com.example.projetjavafx;
import javafx.animation.PauseTransition;
import javafx.util.Duration;

import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.ListView;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.stage.Stage;
import javafx.util.Duration;
import java.io.File;
import java.util.HashSet;
import java.util.Set;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.control.Alert;
import javafx.scene.layout.VBox;
import javafx.geometry.Insets;
import java.util.Properties;
import java.util.regex.Pattern;
import javax.mail.*;
import javax.mail.internet.*;


public class ChatController {

    private KnowledgeBaseService knowledgeBaseService;

    public ChatController() {
        knowledgeBaseService = new KnowledgeBaseService();
    }


    private static final String SYSTEM_PROMPT = "Faire des reponses courtes, Tu es AskAlly, un assistant chatbot sympathique et professionnel spécialisé dans le dépannage technique. Ton rôle est de fournir des solutions claires, étape par étape, pour résoudre les problèmes informatiques courants.. \n\n";
    private ConversationStorage storage = new ConversationStorage();
    private int conversationCount = 0;
    private String currentConversationName = "";

    @FXML
    private TextArea outputArea;
    @FXML
    private TextField inputField;
    @FXML
    private Button sendButton;
    @FXML
    private ListView<String> conversationListView;

    private OllamaService ollamaService;
    private int thinkingMessagePosition = -1;
    private StringBuilder conversationHistory = new StringBuilder();
    private static final int MAX_HISTORY_LENGTH = 10; // Adjust based on your needs
    @FXML
    private Button btnEscalade;
    @FXML
    private Button btnFeedbackUtile;
    @FXML
    private Button btnFeedbackInutile;

    private String lastQuestion;
    private String lastAnswer;

    @FXML
    public void initialize() {
        ollamaService = new OllamaService();
        sendButton.setOnAction(event -> sendMessage());
        conversationListView.getSelectionModel().selectedItemProperty().addListener(
                (obs, oldVal, newVal) -> {
                    if (newVal != null) loadConversation(newVal);
                }
        );
        String[] savedChats = storage.listConversations();
        Set<String> loaded = new HashSet<>();
        for (String chatName : savedChats) {
            File f = new File("conversations/" + chatName + ".txt");
            if (f.exists()) {
                conversationListView.getItems().add(chatName);
                loaded.add(chatName);
                conversationCount = Math.max(conversationCount, extractChatNumber(chatName));
            }
        }
        // Nouvelle conversation initiale
        newConversation();

        // Ajoute le gestionnaire de feedback
        btnFeedbackUtile.setOnAction(event -> handleFeedbackUtile());
        btnFeedbackInutile.setOnAction(event -> handleFeedbackInutile());

        // Ajoute ceci pour sauvegarder à la fermeture
        Platform.runLater(() -> {
            Stage stage = (Stage) outputArea.getScene().getWindow();
            stage.setOnCloseRequest(event -> {
                saveCurrentConversation();
            });
        });
    }

    private void saveCurrentConversation() {
        if (!currentConversationName.isEmpty()) {
            String content = conversationHistory.toString().replace(SYSTEM_PROMPT, "").trim();
            if (!content.isEmpty()) {
                storage.save(currentConversationName, conversationHistory.toString());
            } else {
                // Si la conversation est vide, on la supprime
                storage.delete(currentConversationName);
                conversationListView.getItems().remove(currentConversationName);
            }
        }
    }

    @FXML
    private void newConversation() {
        // Sauvegarde ou supprime la conversation actuelle si elle est vide
        if (!currentConversationName.isEmpty() && conversationListView.getItems().contains(currentConversationName)) {
            String content = conversationHistory.toString().replace(SYSTEM_PROMPT, "").trim();
            if (!content.isEmpty()) {
                storage.save(currentConversationName, conversationHistory.toString());
            } else {
                storage.delete(currentConversationName);
                conversationListView.getItems().remove(currentConversationName);
            }
        }

        // Crée un nom basé sur la date
        LocalDate today = LocalDate.now();
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
        String dateName = "chat " + today.format(formatter);

        String finalName = dateName;
        int suffix = 1;
        while (conversationListView.getItems().contains(finalName)) {
            finalName = dateName + " (" + suffix + ")";
            suffix++;
        }

        currentConversationName = finalName;
        conversationHistory.setLength(0);
        conversationHistory.append(SYSTEM_PROMPT);
        outputArea.clear();

        conversationListView.getItems().add(currentConversationName);
        conversationListView.getSelectionModel().selectLast();
    }



    private void loadConversation(String name) {
        // Sauvegarde ou supprime la conversation actuelle si elle est vide
        if (!currentConversationName.isEmpty() && !name.equals(currentConversationName)
                && conversationListView.getItems().contains(currentConversationName)) {
            String content = conversationHistory.toString().replace(SYSTEM_PROMPT, "").trim();
            if (!content.isEmpty()) {
                storage.save(currentConversationName, conversationHistory.toString());
            } else {
                storage.delete(currentConversationName);
                conversationListView.getItems().remove(currentConversationName);
            }
        }

        // Charge la nouvelle conversation
        String saved = storage.load(name);
        conversationHistory.setLength(0);
        conversationHistory.append(saved);

        String displayText = saved.replace(SYSTEM_PROMPT, "").trim();
        outputArea.setText(displayText);

        currentConversationName = name;
    }


    private void sendMessage() {
        String message = inputField.getText();
        if (!message.isEmpty()) {
            conversationHistory.append("\nYou: ").append(message).append("\n");
            outputArea.appendText("\nYou: " + message + "\n");
            inputField.clear();

            // Chercher la réponse dans la base avec findSolution()
            String existingAnswer = knowledgeBaseService.findSolution(message);
            if (existingAnswer != null) {
                // Délai de 1.5 seconde avant d'afficher la réponse
                PauseTransition delay = new PauseTransition(Duration.seconds(1.5));
                delay.setOnFinished(event -> {
                    outputArea.appendText("AskAlly: " + existingAnswer + "\n");
                    conversationHistory.append("AskAlly: ").append(existingAnswer).append("\n");
                    storage.save(currentConversationName, conversationHistory.toString());
                    lastQuestion = message;
                    lastAnswer = existingAnswer;
                });
                delay.play();
            } else {
                // Sinon → passer à Ollama (comme avant)
                Timeline thinkingTimeline = new Timeline(new KeyFrame(Duration.seconds(1), event -> {
                    thinkingMessagePosition = outputArea.getLength();
                    outputArea.appendText("AskAlly : Thinking...\n");

                    String fullPrompt = conversationHistory.toString() + "AskAlly: ";

                    ollamaService.generateStreamingResponse(fullPrompt, new OllamaService.OllamaResponseCallback() {
                        private boolean firstChunk = true;
                        private StringBuilder aiResponse = new StringBuilder();

                        @Override
                        public void onSuccess(String chunk) {
                            Platform.runLater(() -> {
                                if (firstChunk) {
                                    if (thinkingMessagePosition >= 0) {
                                        outputArea.deleteText(thinkingMessagePosition, outputArea.getLength());
                                    }
                                    outputArea.appendText("AskAlly: " + chunk);
                                    firstChunk = false;
                                } else {
                                    outputArea.appendText(chunk);
                                }
                                aiResponse.append(chunk);
                            });
                        }

                        @Override
                        public void onFailure(Throwable throwable) {
                            Platform.runLater(() -> {
                                if (thinkingMessagePosition >= 0) {
                                    outputArea.deleteText(thinkingMessagePosition, outputArea.getLength());
                                }
                                outputArea.appendText("\nError: " + throwable.getMessage() + "\n");
                            });
                        }

                        @Override
                        public void onComplete() {
                            Platform.runLater(() -> {
                                if (aiResponse.length() > 0) {
                                    conversationHistory.append("AskAlly: ").append(aiResponse.toString()).append("\n");
                                    storage.save(currentConversationName, conversationHistory.toString());
                                    outputArea.appendText("\n");
                                    lastQuestion = message;
                                    lastAnswer = aiResponse.toString();

                                    if (conversationHistory.toString().split("\n").length > MAX_HISTORY_LENGTH * 2) {
                                        trimConversationHistory();
                                    }
                                } else {
                                    // Ici, l'IA n'a rien généré → proposer escalade
                                    outputArea.appendText("AskAlly: Je n'ai pas pu trouver de solution. Voulez-vous contacter un technicien humain ?\n");
                                    btnEscalade.setVisible(true);
                                    // Le bouton btnEscalade est caché par défaut, tu l'affiches ici
                                }
                            });
                        }

                    });
                }));
                thinkingTimeline.play();
            }
        }
    }

    @FXML
    private void handleFeedbackUtile() {
        conversationHistory.append("Feedback: Réponse utile pour la question '" + lastQuestion + "'\n");
        outputArea.appendText("Merci pour votre retour positif !\n");
        storage.save(currentConversationName, conversationHistory.toString());

        // Sauvegarder le feedback dans la base de données
        knowledgeBaseService.saveFeedback(lastQuestion, lastAnswer, "utile");
    }


    @FXML
    private void handleFeedbackInutile() {
        conversationHistory.append("Feedback: Réponse inutile pour la question '" + lastQuestion + "'\n");
        outputArea.appendText("Merci pour votre retour, nous essaierons de nous améliorer.\n");
        storage.save(currentConversationName, conversationHistory.toString());

        // Sauvegarder le feedback dans la base de données
        knowledgeBaseService.saveFeedback(lastQuestion, lastAnswer, "inutile");
    }



    private void trimConversationHistory() {
        String[] lines = conversationHistory.toString().split("\n");
        StringBuilder trimmed = new StringBuilder();

        // Always keep system prompt
        trimmed.append(SYSTEM_PROMPT);

        // Keep the most recent exchanges
        int start = Math.max(1, lines.length - MAX_HISTORY_LENGTH * 2);
        for (int i = start; i < lines.length; i++) {
            // Skip adding the system prompt again if it's already in the history
            if (!lines[i].startsWith("You are a helpful chatbot be brief in answers.")) {
                trimmed.append(lines[i]).append("\n");
            }
        }
        conversationHistory = trimmed;
    }
    private int extractChatNumber(String name) {
        try {
            return Integer.parseInt(name.replace("chat ", ""));
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    @FXML
    private void deleteConversation() {
        String selected = conversationListView.getSelectionModel().getSelectedItem();
        if (selected != null) {
            // Supprime du stockage (et donc du disque aussi)
            storage.delete(selected);

            // Supprime de la liste UI
            conversationListView.getItems().remove(selected);

            // Si la conversation supprimée était la conversation courante
            if (selected.equals(currentConversationName)) {
                outputArea.clear();
                conversationHistory.setLength(0);
                currentConversationName = "";

                // Crée une nouvelle conversation vide si plus aucune conversation n'existe
                if (conversationListView.getItems().isEmpty()) {
                    newConversation();
                } else {
                    // Sinon, charge la première conversation disponible
                    conversationListView.getSelectionModel().selectFirst();
                }
            }
        }
    }

    @FXML
    private void handleEscalation() {
        btnEscalade.setVisible(false);

        outputArea.appendText("Votre demande a été transmise à un technicien humain. Un agent vous contactera sous peu.\n");
        conversationHistory.append("Système: L'utilisateur a demandé une escalation vers un technicien humain.\n");

        // Ouvrir une fenêtre de formulaire pour l'envoi d'email
        openEmailForm();
    }

    private void openEmailForm() {
        try {
            // Créer la fenêtre de dialogue
            Stage emailStage = new Stage();
            emailStage.initModality(Modality.APPLICATION_MODAL);
            emailStage.setTitle("Contacter un technicien");

            // Créer les composants du formulaire
            VBox layout = new VBox(10);
            layout.setPadding(new Insets(20));

            // Champs du formulaire
            TextField emailField = new TextField();
            emailField.setPromptText("Votre adresse email");

            TextArea descriptionArea = new TextArea();
            descriptionArea.setPromptText("Description détaillée du problème");
            descriptionArea.setPrefHeight(150);

            Button sendButton = new Button("Envoyer");
            sendButton.setDefaultButton(true);

            // Ajouter les composants au layout
            layout.getChildren().addAll(
                    new Label("Email de contact:"),
                    emailField,
                    new Label("Description du problème:"),
                    descriptionArea,
                    sendButton
            );

            // Action du bouton d'envoi
            sendButton.setOnAction(e -> {
                if (isValidEmail(emailField.getText()) && !descriptionArea.getText().isEmpty()) {
                    // Envoyer l'email
                    boolean sent = sendEscalationEmail(
                            emailField.getText(),
                            descriptionArea.getText(),
                            currentConversationName
                    );

                    if (sent) {
                        Alert alert = new Alert(Alert.AlertType.INFORMATION);
                        alert.setTitle("Email envoyé");
                        alert.setHeaderText(null);
                        alert.setContentText("Votre demande a été envoyée avec succès. Un technicien vous contactera sous peu.");
                        alert.showAndWait();
                        emailStage.close();
                    } else {
                        Alert alert = new Alert(Alert.AlertType.ERROR);
                        alert.setTitle("Erreur");
                        alert.setHeaderText(null);
                        alert.setContentText("L'envoi de l'email a échoué. Veuillez réessayer plus tard.");
                        alert.showAndWait();
                    }
                } else {
                    Alert alert = new Alert(Alert.AlertType.WARNING);
                    alert.setTitle("Informations manquantes");
                    alert.setHeaderText(null);
                    alert.setContentText("Veuillez remplir tous les champs correctement.");
                    alert.showAndWait();
                }
            });

            // Configurer et afficher la scène
            Scene scene = new Scene(layout, 400, 350);
            emailStage.setScene(scene);
            emailStage.showAndWait();

        } catch (Exception e) {
            e.printStackTrace();
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setTitle("Erreur");
            alert.setHeaderText(null);
            alert.setContentText("Une erreur est survenue: " + e.getMessage());
            alert.showAndWait();
        }
    }

    // Vérifier si l'email est valide
    private boolean isValidEmail(String email) {
        String emailRegex = "^[a-zA-Z0-9_+&*-]+(?:\\.[a-zA-Z0-9_+&*-]+)*@(?:[a-zA-Z0-9-]+\\.)+[a-zA-Z]{2,7}$";
        Pattern pattern = Pattern.compile(emailRegex);
        return pattern.matcher(email).matches() && !email.isEmpty();
    }

    // Méthode pour envoyer l'email
    private boolean sendEscalationEmail(String userEmail, String problemDescription, String conversationId) {
        final String username = "aichaharani1@gmail.com"; // Adresse email de support
        final String password = "123456"; // Mot de passe (idéalement à stocker de façon sécurisée)

        // Propriétés pour la session email
        Properties props = new Properties();
        props.put("mail.smtp.auth", "true");
        props.put("mail.smtp.starttls.enable", "true");
        props.put("mail.smtp.host", "smtp.gmail.com"); // Serveur SMTP
        props.put("mail.smtp.port", "587"); // Port SMTP

        try {
            // Créer une session avec authentification
            Session session = Session.getInstance(props, new Authenticator() {
                @Override
                protected PasswordAuthentication getPasswordAuthentication() {
                    return new PasswordAuthentication(username, password);
                }
            });

            // Créer le message
            Message message = new MimeMessage(session);
            message.setFrom(new InternetAddress(username));
            message.setRecipients(Message.RecipientType.TO, InternetAddress.parse("techniciens@votreentreprise.com"));
            message.setSubject("[ESCALADE] Support technique requis - " + conversationId);

            // Contenu de l'email
            String emailContent = "Une demande d'escalade a été soumise:\n\n" +
                    "Email de contact: " + userEmail + "\n" +
                    "ID de conversation: " + conversationId + "\n\n" +
                    "Description du problème:\n" + problemDescription + "\n\n" +
                    "Historique de la conversation:\n" +
                    conversationHistory.toString().replace(SYSTEM_PROMPT, "").trim();

            message.setText(emailContent);

            // Envoyer l'email
            Transport.send(message);

            return true;
        } catch (MessagingException e) {
            e.printStackTrace();
            return false;
        }
    }



}