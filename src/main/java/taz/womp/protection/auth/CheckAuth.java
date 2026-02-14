package taz.womp.protection.auth;

import com.google.gson.JsonObject;
import taz.womp.Womp;
import taz.womp.manager.ProtectionManager;
import taz.womp.utils.Encryption;
import taz.womp.utils.HttpClientUtil;

public class CheckAuth {

    public static void run() {
        if (Womp.INSTANCE.getSession() == null) {
            ProtectionManager.exit("S");
        }
        try {
            String jwtToken = Womp.INSTANCE.getSession().getJwtToken();
            JsonObject payload = new JsonObject();
            String hwid = taz.womp.protection.HWIDGrabber.getHWID();
            if (hwid != null && !hwid.trim().isEmpty()) {
                payload.addProperty("hwid", hwid);
            }

            String encryptedResponse = HttpClientUtil.sendPostRequest("/check", payload, jwtToken);
            String decryptedResponse = Encryption.decrypt(encryptedResponse);
            if (!decryptedResponse.contains("Successfully processed login!")) {
                ProtectionManager.exit("V " + decryptedResponse);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}