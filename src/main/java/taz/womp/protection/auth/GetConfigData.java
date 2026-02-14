package taz.womp.protection.auth;

import taz.womp.Womp;
import taz.womp.manager.ProtectionManager;
import taz.womp.utils.Encryption;
import taz.womp.utils.HttpClientUtil;

public class GetConfigData {
    public static class ConfigDataResult {
        public final String configData;
        public final String description;
        public ConfigDataResult(String configData, String description) {
            this.configData = configData;
            this.description = description;
        }
    }

    public static ConfigDataResult run(String configName) {
        if (Womp.INSTANCE.getSession() == null || Womp.INSTANCE.getSession().getJwtToken() == null) {
            ProtectionManager.exit("S"); 
        }
        try {
            com.google.gson.JsonObject payload = new com.google.gson.JsonObject();
            payload.addProperty("configName", configName);

            String encryptedResponse = HttpClientUtil.sendPostRequest("/getconfigdata", payload, Womp.INSTANCE.getSession().getJwtToken());
            String decryptedResponse = Encryption.decrypt(encryptedResponse);

            if (decryptedResponse == null) {
                return new ConfigDataResult(null, null);
            }
            com.google.gson.JsonObject obj = new com.google.gson.Gson().fromJson(decryptedResponse, com.google.gson.JsonObject.class);
            String configData = obj.has("config_data") ? obj.get("config_data").getAsString() : null;
            String description = obj.has("description") ? obj.get("description").getAsString() : null;
            return new ConfigDataResult(configData, description);
        } catch (Exception iOException) {
            iOException.printStackTrace();
            return new ConfigDataResult(null, null);
        }
    }
}
