package taz.womp;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import taz.womp.gui.LoginScreen;

public final class Main implements ModInitializer {
    public void onInitialize() {
        Womp.INSTANCE.postInit();
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            if (Womp.INSTANCE.getSession() == null || Womp.INSTANCE.getSession().getJwtToken() == null || Womp.INSTANCE.getSession().getJwtToken().isEmpty()) {
                if (!(client.currentScreen instanceof LoginScreen)) {
                    client.setScreen(new LoginScreen());
                }
            }
        });
    }
}