package taz.womp.module;

import net.minecraft.client.MinecraftClient;
import taz.womp.manager.EventManager;
import taz.womp.module.setting.Setting;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public abstract class Module implements Serializable {
    private final List<Setting> settings;
    protected EventManager EVENT_BUS;
    protected MinecraftClient mc;
    private CharSequence name;
    private CharSequence description;
    private boolean enabled;
    private int keybind;
    private Category category;

    public Module(final CharSequence name, final CharSequence description, final int keybind, final Category category) {
        this.settings = new ArrayList<Setting>();
        this.mc = MinecraftClient.getInstance();
        this.name = name;
        this.description = description;
        this.enabled = false;
        this.keybind = keybind;
        this.category = category;
    }

    public void toggle() {
        this.enabled = !this.enabled;
        if (this.enabled) {
            this.onEnable();
        } else {
            this.onDisable();
        }
    }

    public CharSequence getName() {
        return this.name;
    }

    public boolean isEnabled() {
        return this.enabled;
    }

    public CharSequence getDescription() {
        return this.description;
    }

    public int getKeybind() {
        return this.keybind;
    }

    public Category getCategory() {
        return this.category;
    }

    public void setCategory(final Category category) {
        this.category = category;
    }

    public void setName(final CharSequence name) {
        this.name = name;
    }

    public void setDescription(final CharSequence description) {
        this.description = description;
    }

    public void setKeybind(final int keybind) {
        this.keybind = keybind;
    }

    public List<Setting> getSettings() {
        return this.settings;
    }

    public void onEnable() {
    }

    public void onDisable() {
    }

    public void addSetting(final Setting setting) {
        setting.setOnChangeCallback(this::resetModule);
        this.settings.add(setting);
    }

    public void addSettings(final Setting... a) {
        for (Setting setting : a) {
            setting.setOnChangeCallback(this::resetModule);
        }
        this.settings.addAll(Arrays.asList(a));
    }

    public void toggle(final boolean enabled) {
        if (this.enabled != enabled) {
            this.enabled = enabled;
            if (enabled) {
                this.onEnable();
            } else {
                this.onDisable();
            }
        }
    }

    public void setEnabled(final boolean enabled) {
        this.enabled = enabled;
    }

    private void resetModule() {
        if (this.enabled) {
            this.onDisable();
            this.onEnable();
        }
    }

    protected boolean isPlayerUsable() {
        if (mc.player == null || mc.world == null) return false;
        if (mc.player.getHealth() <= 0) return false;
        if (mc.currentScreen != null && mc.currentScreen.getClass().getSimpleName().equals("DeathScreen")) return false;
        return true;
    }

    public void setEventBus(EventManager eventBus) {
        this.EVENT_BUS = eventBus;
    }
}
