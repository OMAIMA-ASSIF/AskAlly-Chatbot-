package com.example.projetjavafx;

import okhttp3.*;
import org.json.JSONObject;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.concurrent.TimeUnit; // Import TimeUnit

public class OllamaService {
    private static final String OLLAMA_URL = "http://localhost:11434/api/generate";
    private final OkHttpClient client;  // Declare client as final

    public OllamaService() {
        //  Initialize the OkHttpClient with increased timeouts
        this.client = new OkHttpClient.Builder()
                .connectTimeout(30, TimeUnit.SECONDS)
                .readTimeout(120, TimeUnit.SECONDS) // Augmenter à 2 minutes
                .writeTimeout(30, TimeUnit.SECONDS)
                .build();
    }

    public void generateStreamingResponse(String prompt, OllamaResponseCallback callback) {
        JSONObject requestBody = new JSONObject();
        requestBody.put("model", "llama3.2");
        requestBody.put("prompt", prompt);
        requestBody.put("stream", true);

        Request request = new Request.Builder()
                .url(OLLAMA_URL)
                .post(RequestBody.create(requestBody.toString(), MediaType.parse("application/json")))
                .build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                callback.onFailure(e);
                callback.onComplete();
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                if (!response.isSuccessful()) {
                    callback.onFailure(new IOException("HTTP error: " + response.code()));
                    callback.onComplete();
                    return;
                }

                try (BufferedReader reader = new BufferedReader(
                        new InputStreamReader(response.body().byteStream()))) {
                    String line;
                    StringBuilder fullResponse = new StringBuilder();
                    while ((line = reader.readLine()) != null) {
                        if (line.isEmpty()) continue;
                        try {
                            JSONObject jsonChunk = new JSONObject(line);
                            String chunk = jsonChunk.optString("response", "");
                            fullResponse.append(chunk);
                            callback.onSuccess(chunk);
                        } catch (Exception e) {
                            callback.onFailure(e);
                            break;
                        }
                    }
                } catch (IOException e) {
                    callback.onFailure(e);
                } finally {
                    callback.onComplete();
                }
            }
        });
    }

    public interface OllamaResponseCallback {
        void onSuccess(String response);
        void onFailure(Throwable throwable);

        void onComplete();
    }
}
