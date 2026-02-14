package taz.womp.utils;

import com.google.gson.Gson;
import com.google.gson.JsonObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import taz.womp.manager.ProtectionManager;
import net.minecraft.client.MinecraftClient;
import taz.womp.gui.LoginScreen;

public class HttpClientUtil {

    private static final String BASE_URL = "https://api.womp.top";
    private static final Gson GSON = new Gson();

    private static String getApiKey() {
        String part1 = "uF7k0303rjNsYXP8BVq3g";
        String part2 = "ZSbyH3HozkNYTTzgqUg8k";
        String part3 = "DnA3H6tkdaD3JPeS9k07Xs";
        return part1 + part2 + part3;
    }

    public static String sendPostRequest(String endpoint, JsonObject payload) throws Exception {
        return sendPostRequest(endpoint, payload, null);
    }

    public static String sendPostRequest(String endpoint, JsonObject payload, String jwtToken) throws Exception {
        if (!BASE_URL.equals("https://api.womp.top")) {
            ProtectionManager.exit("API URL tampering detected");
        }
        URL url = URI.create(BASE_URL + endpoint).toURL();
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("POST");
        conn.setRequestProperty("Content-Type", "application/json");
        conn.setRequestProperty("Accept", "text/plain");
        conn.setRequestProperty("x-api-key", getApiKey());

        if (jwtToken != null && !jwtToken.isEmpty()) {
            conn.setRequestProperty("Authorization", "Bearer " + jwtToken);
        }

        conn.setDoOutput(true);

        String jsonInputString = GSON.toJson(payload);

        try (OutputStream os = conn.getOutputStream()) {
            byte[] input = jsonInputString.getBytes(StandardCharsets.UTF_8);
            os.write(input, 0, input.length);
        }

        int responseCode = conn.getResponseCode();
        if (responseCode == HttpURLConnection.HTTP_OK) {
            try (BufferedReader br = new BufferedReader(
                    new InputStreamReader(conn.getInputStream(), StandardCharsets.UTF_8))) {
                StringBuilder response = new StringBuilder();
                String responseLine = null;
                while ((responseLine = br.readLine()) != null) {
                    response.append(responseLine.trim());
                }
                return response.toString();
            }
        } else if (responseCode == HttpURLConnection.HTTP_UNAUTHORIZED || responseCode == HttpURLConnection.HTTP_FORBIDDEN) {

            MinecraftClient.getInstance().execute(() -> MinecraftClient.getInstance().setScreen(new LoginScreen()));
            throw new IOException("Session expired or unauthorized. Please log in again.");
        } else {
            try (BufferedReader br = new BufferedReader(
                    new InputStreamReader(conn.getErrorStream(), StandardCharsets.UTF_8))) {
                StringBuilder errorResponse = new StringBuilder();
                String errorLine = null;
                while ((errorLine = br.readLine()) != null) {
                    errorResponse.append(errorLine.trim());
                }
                throw new IOException("HTTP Error: " + responseCode + ", Response: " + errorResponse.toString());
            }
        }
    }
}
