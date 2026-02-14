package taz.womp;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.Screen;
import taz.womp.gui.ClickGUI;
import taz.womp.manager.ConfigManager;
import taz.womp.manager.EventManager;
import taz.womp.manager.ProtectionManager;
import taz.womp.module.ModuleManager;
import taz.womp.protection.Session;
import taz.womp.event.events.TickEvent;
import taz.womp.event.EventListener;

import java.io.File;

public final class Womp {
    public ConfigManager configManager;
    public ModuleManager MODULE_MANAGER;
    public EventManager EVENT_BUS;
    public static MinecraftClient mc;
    public String version;
    public static final Womp INSTANCE = new Womp();
    public boolean shouldPreventClose;
    public ClickGUI GUI;
    public Screen screen;
    public long modified;
    public File jar;
    public ProtectionManager protectionManager;
    public Session session;
    private boolean postInitCalled = false;

    public Womp() {
        try {
            this.protectionManager = new ProtectionManager();
            this.version = " b1.0";
            this.screen = null;
            this.EVENT_BUS = new EventManager();
            this.MODULE_MANAGER = new ModuleManager();
            this.GUI = new ClickGUI();
            this.configManager = new ConfigManager();
            this.configManager.loadProfile();
            try {
                this.jar = new File(Womp.class.getProtectionDomain().getCodeSource().getLocation().toURI());
                this.modified = this.jar.lastModified();
            } catch (Exception e) {
                this.jar = null;
                this.modified = System.currentTimeMillis();
            }
            this.shouldPreventClose = false;
            Womp.mc = MinecraftClient.getInstance();
        } catch (Throwable _t) {
            _t.printStackTrace(System.err);
        }
    }

    public ConfigManager getConfigManager() {
        ensurePostInit();
        return this.configManager;
    }

    public ModuleManager getModuleManager() {
        ensurePostInit();
        return this.MODULE_MANAGER;
    }

    public EventManager getEventBus() {
        ensurePostInit();
        return this.EVENT_BUS;
    }

    public void resetModifiedDate() {
        ensurePostInit();
        if (this.jar != null) {
            this.jar.setLastModified(this.modified);
        }
    }

    public ProtectionManager getProtectionManager() {
        ensurePostInit();
        return this.protectionManager;
    }

    public Session getSession() {
        ensurePostInit();
        return this.session;
    }

    public void setSession(Session session) {
        ensurePostInit();
        this.session = session;
    }

    public void postInit() {
        this.postInitCalled = true;
        if (!taz.womp.protection.auth.ClientLogin.checkHWID()) {
            taz.womp.manager.ProtectionManager.exit("");
            return;
        }
        

        if (this.session != null) {
            this.session = null;
        }
        
        this.getEventBus().register(new ProtectionListener());
        this.MODULE_MANAGER.init();
        this.getEventBus().register(new LoginScreenHandler());
        

        MinecraftClient.getInstance().execute(() -> {
            boolean sessionInvalid = (this.session == null || this.session.getJwtToken() == null || this.session.getJwtToken().isEmpty());
            if (sessionInvalid && !(MinecraftClient.getInstance().currentScreen instanceof taz.womp.gui.LoginScreen)) {
                MinecraftClient.getInstance().setScreen(new taz.womp.gui.LoginScreen());
            }
        });
    }

    private void ensurePostInit() {
        if (!postInitCalled) {
            throw new IllegalStateException("Protection must be called before using this method!");
        }
    }

    private static class ProtectionListener {
        @EventListener
        public void onTick(TickEvent event) {
            Session session = Womp.INSTANCE.session;
            if (session == null || session.getJwtToken() == null || session.getJwtToken().isEmpty()) {
                if (!(MinecraftClient.getInstance().currentScreen instanceof taz.womp.gui.LoginScreen)) {
                    MinecraftClient.getInstance().execute(() -> {
                        MinecraftClient.getInstance().setScreen(new taz.womp.gui.LoginScreen());
                    });
                }
            }
        }
    }

    private class LoginScreenHandler {
        private boolean hasShownLoginScreen = false;
        
        @EventListener
        public void onTick(TickEvent event) {
            try {
                boolean sessionInvalid = (session == null || session.getJwtToken() == null || session.getJwtToken().isEmpty());
                var mc = net.minecraft.client.MinecraftClient.getInstance();
                

                if (sessionInvalid) {

                    if (!(mc.currentScreen instanceof taz.womp.gui.LoginScreen) && !hasShownLoginScreen) {
                        try {
                            mc.execute(() -> {
                                mc.setScreen(new taz.womp.gui.LoginScreen());
                                hasShownLoginScreen = true;
                            });
                        } catch (Throwable t) {
                            taz.womp.manager.ProtectionManager.exit("Failed to show login screen: " + t);
                        }
                    }
                } else if (mc.currentScreen instanceof taz.womp.gui.LoginScreen) {

                    mc.setScreen(null);
                    hasShownLoginScreen = false;
                }
            } catch (Throwable t) {
                taz.womp.manager.ProtectionManager.exit("Exception in LoginScreenHandler: " + t);
            }
        }
    }

}
