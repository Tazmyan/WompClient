package taz.womp.module.modules.misc;

import taz.womp.module.Category;
import taz.womp.module.Module;
import taz.womp.module.setting.StringSetting;
import taz.womp.utils.EncryptedString;

public class NameProtect extends Module {
    private final StringSetting fakeName = new StringSetting(EncryptedString.of("Fake Name"), "Player");

    public NameProtect() {
        super(EncryptedString.of("Name Protect"), EncryptedString.of("Replaces your name with given one."), -1, Category.MISC);
        this.addSettings(this.fakeName);
    }

    @Override
    public void onEnable() {
        super.onEnable();
    }

    @Override
    public void onDisable() {
        super.onDisable();
    }

    public String getFakeName() {
        return this.fakeName.getValue();
    }
}
