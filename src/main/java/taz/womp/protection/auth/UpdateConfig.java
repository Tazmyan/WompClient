package taz.womp.protection.auth;

import com.google.gson.JsonObject;
import taz.womp.Womp;
import taz.womp.manager.ProtectionManager;
import taz.womp.utils.Encryption;
import taz.womp.utils.HttpClientUtil;

public class UpdateConfig {

    public static String run(String configName, String configData) {
        if (Womp.INSTANCE.getSession() == null || Womp.INSTANCE.getSession().getJwtToken() == null) {
            ProtectionManager.exit("S"); 
        }
        try {
            JsonObject payload = new JsonObject();
            payload.addProperty("configName", configName);
            payload.addProperty("configData", configData);

            String encryptedResponse = HttpClientUtil.sendPostRequest("/updateconfig", payload, Womp.INSTANCE.getSession().getJwtToken());
            String decryptedResponse = Encryption.decrypt(encryptedResponse);

            if (decryptedResponse == null) {
                return "Internal error whilst updating config, please contact the developer.";
            }
            return decryptedResponse;
        } catch (Exception e) {
            e.printStackTrace();
            return "Internal error whilst updating config, please contact the developer.";
        }
    }
}
