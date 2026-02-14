package taz.womp.protection.auth;

import com.google.gson.JsonObject;
import taz.womp.Womp;
import taz.womp.manager.ProtectionManager;
import taz.womp.utils.Encryption;
import taz.womp.utils.HttpClientUtil;

import java.util.ArrayList;
import java.util.List;

public class ShareConfig {
    public static String run(String configName, String targetUsername) {
        if (Womp.INSTANCE.getSession() == null || Womp.INSTANCE.getSession().getJwtToken() == null) {
            ProtectionManager.exit("S"); 
        }
        try {
            JsonObject payload = new JsonObject();
            payload.addProperty("configName", configName);
            payload.addProperty("targetUsername", targetUsername.toLowerCase());
            String encryptedResponse = HttpClientUtil.sendPostRequest("/shareconfig", payload, Womp.INSTANCE.getSession().getJwtToken());
            String decryptedResponse = Encryption.decrypt(encryptedResponse);
            if (decryptedResponse == null) {
                return "Internal error whilst sharing config, please contact the developer.";
            }
            return decryptedResponse;
        } catch (Exception e) {
            String msg = e.getMessage();
            if (msg != null && msg.contains("HTTP Error: 404")) {
                return "User not found";
            }
            e.printStackTrace();
            return "Internal error whilst sharing config, please contact the developer.";
        }
    }

    public static List<String> getSharedUsers(String configName) {
        List<String> users = new ArrayList<>();
        if (Womp.INSTANCE.getSession() == null || Womp.INSTANCE.getSession().getJwtToken() == null) {
            ProtectionManager.exit("S");
        }
        try {
            JsonObject payload = new JsonObject();
            payload.addProperty("configName", configName);
            String encryptedResponse = HttpClientUtil.sendPostRequest("/getsharedusers", payload, Womp.INSTANCE.getSession().getJwtToken());
            String decryptedResponse = Encryption.decrypt(encryptedResponse);
            if (decryptedResponse == null) return users;
            
            com.google.gson.JsonArray arr = com.google.gson.JsonParser.parseString(decryptedResponse).getAsJsonArray();
            for (int i = 0; i < arr.size(); i++) {
                users.add(arr.get(i).getAsString());
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return users;
    }
} 