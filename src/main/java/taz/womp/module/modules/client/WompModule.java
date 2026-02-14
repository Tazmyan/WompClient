package taz.womp.module.modules.client;

import net.minecraft.network.packet.s2c.play.OpenScreenS2CPacket;
import taz.womp.Womp;
import taz.womp.event.EventListener;
import taz.womp.event.events.PacketReceiveEvent;
import taz.womp.gui.ClickGUI;
import taz.womp.module.Category;
import taz.womp.module.Module;
import taz.womp.module.setting.BooleanSetting;
import taz.womp.module.setting.ModeSetting;
import taz.womp.module.setting.NumberSetting;
import taz.womp.utils.EncryptedString;

import java.util.Random;
import java.util.concurrent.CompletableFuture;

public final class WompModule extends Module {
    public static final NumberSetting redColor = new NumberSetting(EncryptedString.of("Red"), 0.0, 255.0, 120.0, 1.0);
    public static final NumberSetting greenColor = new NumberSetting(EncryptedString.of("Green"), 0.0, 255.0, 190.0, 1.0);
    public static final NumberSetting blueColor = new NumberSetting(EncryptedString.of("Blue"), 0.0, 255.0, 255.0, 1.0);
    public static final NumberSetting windowAlpha = new NumberSetting(EncryptedString.of("Window Alpha"), 0.0, 255.0, 170.0, 1.0);
    public static final BooleanSetting enableBreathingEffect = new BooleanSetting(EncryptedString.of("Breathing"), false).setDescription(EncryptedString.of("Color breathing effect"));
    public static final BooleanSetting enableRainbowEffect = new BooleanSetting(EncryptedString.of("Rainbow"), false).setDescription(EncryptedString.of("Enables Rainbow mode"));
    public static final BooleanSetting renderBackground = new BooleanSetting(EncryptedString.of("Background"), true).setDescription(EncryptedString.of("Renders the background of the ClickGUI"));
    public static final BooleanSetting useCustomFont = new BooleanSetting(EncryptedString.of("Custom Font"), true);
    public static final NumberSetting cornerRoundness = new NumberSetting(EncryptedString.of("Roundness"), 1.0, 10.0, 5.0, 1.0);
    public static final ModeSetting<AnimationMode> animationMode = new ModeSetting<>(EncryptedString.of("Animations"), AnimationMode.NORMAL, AnimationMode.values());
    public static final BooleanSetting enableMSAA = new BooleanSetting(EncryptedString.of("MSAA"), true).setDescription(EncryptedString.of("Anti Aliasing"));

    public WompModule() {
        super(EncryptedString.of("Womp"), EncryptedString.of("Settings for the client"), 344, Category.CLIENT);
        this.addSettings(WompModule.redColor, WompModule.greenColor, WompModule.blueColor, WompModule.windowAlpha, WompModule.renderBackground, WompModule.cornerRoundness, WompModule.animationMode, WompModule.enableMSAA);
        for (var setting : this.getSettings()) {
            setting.setOnChangeCallback(this::updateGuiLive);
        }
    }

    private void updateGuiLive() {
        if (this.isEnabled() && Womp.INSTANCE.GUI != null) {
            Womp.INSTANCE.GUI.refresh();
        }
    }

    @Override
    public void onEnable() {
        Womp.INSTANCE.screen = this.mc.currentScreen;
        if (Womp.INSTANCE.GUI != null) {
            this.mc.setScreenAndRender(Womp.INSTANCE.GUI);
        }
        if (new Random().nextInt(3) == 1) {
            CompletableFuture.runAsync(() -> {
            });
        }
        super.onEnable();
    }

    @Override
    public void onDisable() {
        if (this.mc.currentScreen instanceof ClickGUI) {
            this.mc.setScreenAndRender(null);
        }
        super.onDisable();
    }

    @EventListener
    public void onPacketReceive(final PacketReceiveEvent packetReceiveEvent) {
        if (packetReceiveEvent.packet instanceof OpenScreenS2CPacket) {
            packetReceiveEvent.cancel();
        }
    }

    protected void resetModule() {
        if (this.isEnabled() && Womp.INSTANCE.GUI != null) {
        }
    }

    public enum AnimationMode {
        NORMAL("Normal", 0),
        POSITIVE("Positive", 1),
        OFF("Off", 2);

        AnimationMode(final String name, final int ordinal) {
        }
    }

}