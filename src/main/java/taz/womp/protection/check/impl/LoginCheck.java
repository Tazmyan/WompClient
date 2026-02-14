package taz.womp.protection.check.impl;

import net.minecraft.client.MinecraftClient;
import taz.womp.gui.LoginScreen;
import taz.womp.manager.ProtectionManager;
import taz.womp.protection.auth.ClientLogin;
import taz.womp.protection.check.Category;
import taz.womp.protection.check.Check;

public class LoginCheck extends Check {

    public LoginCheck() {
        super(Category.Normal);
    }

    @Override
    public void run() {
        try {

            if (!ClientLogin.checkHWID()) {
                ProtectionManager.exit("D");
                return;
            }
            

            if (taz.womp.Womp.INSTANCE.getSession() == null) {

                MinecraftClient.getInstance().execute(() -> {
                    MinecraftClient.getInstance().setScreen(new LoginScreen());
                });
            }
        } catch (Exception exception) {
            ProtectionManager.exit("Exception in GG");
        }
    }
}
