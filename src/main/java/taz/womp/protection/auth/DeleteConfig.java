package taz.womp.protection.auth;

import com.google.gson.JsonObject;
import taz.womp.Womp;
import taz.womp.manager.ProtectionManager;
import taz.womp.utils.Encryption;
import taz.womp.utils.HttpClientUtil;

public class DeleteConfig {

    public static String run(String configName) {
        if (Womp.INSTANCE.getSession() == null || Womp.INSTANCE.getSession().getJwtToken() == null) {
            ProtectionManager.exit("S");
        }
        try {
            JsonObject payload = new JsonObject();
            payload.addProperty("delConfigName", configName);

            String encryptedResponse = HttpClientUtil.sendPostRequest("/deleteconfig", payload, Womp.INSTANCE.getSession().getJwtToken());
            String decryptedResponse = Encryption.decrypt(encryptedResponse);
            
            if (decryptedResponse == null) {
                return "Internal error whilst deleting config, please contact the developer.";
            }
            return decryptedResponse;
        } catch (Exception e) {
            e.printStackTrace();
            return "Internal error whilst deleting config, please contact the developer.";
        }
    }
}
