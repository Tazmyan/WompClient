package taz.womp.protection.auth;

import taz.womp.protection.HWIDGrabber;
import taz.womp.utils.Encryption;
import taz.womp.utils.HttpClientUtil;
import com.google.gson.JsonObject;

public class ClientLogin {
    public static boolean checkHWID() {
        try {
            String hwid = HWIDGrabber.getHWID();
            if (hwid == null || hwid.trim().isEmpty()) {
                return false;
            }

            JsonObject payload = new JsonObject();
            payload.addProperty("hwid", hwid);

            String response = HttpClientUtil.sendPostRequest("/initial-login", payload);

            if (response == null || response.trim().isEmpty()) {
                return false;
            }

            String decrypted = Encryption.decrypt(response);

            boolean isValid = decrypted != null && decrypted.contains("HWID valid");

            return isValid;
        } catch (Exception e) {
            return false;
        }
    }

    public static String run(String username, String password) {
        try {
            if (username == null || username.trim().isEmpty() || password == null || password.trim().isEmpty()) {
                return "Invalid username or password";
            }

            String hwid = HWIDGrabber.getHWID();
            if (hwid == null || hwid.trim().isEmpty()) {
                return "Failed to get hardware ID";
            }

            JsonObject payload = new JsonObject();
            payload.addProperty("username", username);
            payload.addProperty("password", password);
            payload.addProperty("hwid", hwid);

            String encryptedResponse = HttpClientUtil.sendPostRequest("/auth/client", payload);
            if (encryptedResponse == null || encryptedResponse.trim().isEmpty()) {
                return "No response from server";
            }

            String decrypted = Encryption.decrypt(encryptedResponse);

            return decrypted;
        } catch (Exception e) {
            return "Internal error whilst logging in, please contact the developer.";
        }
    }
}