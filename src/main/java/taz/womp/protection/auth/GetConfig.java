package taz.womp.protection.auth;

import com.google.gson.JsonObject;
import taz.womp.Womp;
import taz.womp.manager.ProtectionManager;
import taz.womp.utils.Encryption;
import taz.womp.utils.HttpClientUtil;

public class GetConfig {

    public static String run() {
        if (Womp.INSTANCE.getSession() == null || Womp.INSTANCE.getSession().getJwtToken() == null) {
            ProtectionManager.exit(new StringBuilder().append("S").toString());
        }
        try {
            JsonObject payload = new JsonObject();

            String encryptedResponse = HttpClientUtil.sendPostRequest("/getconfigs", payload, Womp.INSTANCE.getSession().getJwtToken());
            String decryptedResponse = Encryption.decrypt(encryptedResponse);
            
            if (decryptedResponse == null) {
                return "Internal error whilst getting configs, please contact the developer.";
            }
            return decryptedResponse;
        } catch (Exception ex) {
            ex.printStackTrace();
            return "Internal error whilst getting configs, please contact the developer.";
        }
    }
}
