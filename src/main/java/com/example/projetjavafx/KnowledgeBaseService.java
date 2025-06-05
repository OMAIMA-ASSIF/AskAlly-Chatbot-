package com.example.projetjavafx;

import java.sql.*;
import java.text.Normalizer;
import java.util.regex.Pattern;

public class KnowledgeBaseService {

    private static final String DB_URL = "jdbc:sqlite:knowledgebase.db";

    public KnowledgeBaseService() {
        try {
            // Charger le driver SQLite
            Class.forName("org.sqlite.JDBC");
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        }
    }

    // Méthode pour enlever les accents d'une chaîne
    public static String removeAccents(String text) {
        String normalized = Normalizer.normalize(text, Normalizer.Form.NFD);
        Pattern pattern = Pattern.compile("\\p{InCombiningDiacriticalMarks}+");
        return pattern.matcher(normalized).replaceAll("");
    }

    // Vérifie que tous les mots du keyword sont présents dans la question, sans tenir compte des accents
    /*public boolean containsAllKeywords(String question, String keyword) {
        String questionLower = removeAccents(question.toLowerCase());
        String[] keywordWords = removeAccents(keyword.toLowerCase()).split("\\s+");

        for (String word : keywordWords) {
            if (!questionLower.contains(word)) {
                return false;
            }
        }
        return true;
    }*/
    //on doit verifier si au moins un mot de input est dans une  cellule
    public boolean containsAnyKeywordPhrase(String question, String keyword) {
        String questionLowerNormalized = removeAccents(question.toLowerCase());

        // Split the keyword string by "/" to get individual keyword phrases/words
        // Use "\\s*\\/\\s*" to handle spaces around the slash
        String[] keywordPhrases = keyword.split("\\s*\\/\\s*");

        for (String phrase : keywordPhrases) {
            String phraseLowerNormalized = removeAccents(phrase.toLowerCase()).trim();

            // Skip empty phrases that might result from extra slashes
            if (phraseLowerNormalized.isEmpty()) {
                continue;
            }

            // Check if the normalized question contains the normalized phrase
            if (questionLowerNormalized.contains(phraseLowerNormalized)) {
                return true; // Found at least one match, so return true immediately
            }
        }
        return false; // No matching keyword phrase was found
    }


    // Cherche la solution correspondante à la question utilisateur en utilisant la recherche flexible
    public String findSolution(String userQuestion) {
        String sql = "SELECT keyword, solution FROM problems";

        try (Connection conn = DriverManager.getConnection(DB_URL);
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {

            while (rs.next()) {
                String keyword = rs.getString("keyword");
                String solution = rs.getString("solution");

                if (containsAnyKeywordPhrase(userQuestion, keyword)) {
                    return solution;
                }
            }

        } catch (SQLException e) {
            System.out.println(e.getMessage());
        }

        return null; // Pas de solution trouvée
    }

    // Sauvegarde le feedback utilisateur sur une solution donnée
    public void saveFeedback(String question, String solution, String feedback) {
        String sql = "INSERT INTO feedbacks (question, solution, feedback) VALUES (?, ?, ?)";
        try (Connection conn = DriverManager.getConnection(DB_URL);
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, question);
            pstmt.setString(2, solution);
            pstmt.setString(3, feedback);
            pstmt.executeUpdate();
            System.out.println("Feedback sauvegardé !");
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

}
