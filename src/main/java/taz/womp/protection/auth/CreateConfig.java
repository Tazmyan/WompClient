package taz.womp.protection.auth;

import com.google.gson.JsonObject;
import taz.womp.Womp;
import taz.womp.manager.ProtectionManager;
import taz.womp.utils.Encryption;
import taz.womp.utils.HttpClientUtil;

public class CreateConfig {

    public static String run(String configName, String configData, String description, int isPublic) {
        if (Womp.INSTANCE.getSession() == null || Womp.INSTANCE.getSession().getJwtToken() == null) {
            ProtectionManager.exit("S");
        }
        try {
            JsonObject payload = new JsonObject();
            payload.addProperty("configName", configName);
            payload.addProperty("configData", configData);
            payload.addProperty("public", isPublic);
            String encryptedResponse = HttpClientUtil.sendPostRequest("/createconfig", payload, Womp.INSTANCE.getSession().getJwtToken());
            String decryptedResponse = Encryption.decrypt(encryptedResponse);
            
            if (decryptedResponse == null) {
                return "Internal error whilst creating config, please contact the developer.";
            }
            return decryptedResponse;
        } catch (Exception e) {
            e.printStackTrace();
            return "Internal error whilst creating config, please contact the developer.";
        }
    }
}
