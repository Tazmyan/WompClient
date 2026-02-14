package taz.womp.protection.auth;

import com.google.gson.JsonObject;
import taz.womp.Womp;
import taz.womp.manager.ProtectionManager;
import taz.womp.utils.Encryption;
import taz.womp.utils.HttpClientUtil;

public class KeepAlive
{
    public static int run() {
        if (Womp.INSTANCE.getSession() == null || Womp.INSTANCE.getSession().getJwtToken() == null) {
            ProtectionManager.exit("S"); 
        }
        try {
            JsonObject payload = new JsonObject();

            String encryptedResponse = HttpClientUtil.sendPostRequest("/keepAlive", payload, Womp.INSTANCE.getSession().getJwtToken());
            String decryptedResponse = Encryption.decrypt(encryptedResponse);
            
            if (decryptedResponse != null && !decryptedResponse.contains("JAR file not found.")) {
                return Integer.parseInt(decryptedResponse);
            }
            ProtectionManager.exit("N");
            return 0;
        } catch (Exception ex) {
            ex.printStackTrace();
            return 0;
        }
    }
}
