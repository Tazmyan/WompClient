package taz.womp.module.modules.combat;

import net.minecraft.entity.player.PlayerEntity;
import taz.womp.Womp;
import taz.womp.event.EventListener;
import taz.womp.event.events.TargetMarginEvent;
import taz.womp.module.Category;
import taz.womp.module.Module;
import taz.womp.module.setting.BooleanSetting;
import taz.womp.module.setting.NumberSetting;
import taz.womp.utils.EncryptedString;

public final class Hitbox extends Module {
    private final NumberSetting expand = new NumberSetting(EncryptedString.of("Expand"), 0.0, 2.0, 0.5, 0.05);
    private final BooleanSetting enableRender = new BooleanSetting(EncryptedString.of("Enable Render"), true);

    public Hitbox() {
        super(EncryptedString.of("HitBox"), EncryptedString.of("Expands a player's hitbox."), -1, Category.COMBAT);
        this.addSettings(this.enableRender, this.expand);
    }

    @Override
    public void onEnable() {
        super.onEnable();
    }

    @Override
    public void onDisable() {
        super.onDisable();
    }

    @EventListener
    public void onTargetMargin(final TargetMarginEvent targetMarginEvent) {
        if (targetMarginEvent.entity instanceof PlayerEntity player) {

            AntiBot antiBot = (AntiBot) Womp.INSTANCE.getModuleManager().getModuleByClass(AntiBot.class);
            if (antiBot != null && antiBot.isEnabled() && AntiBot.isABot(player)) {
                return;
            }
            targetMarginEvent.cir.setReturnValue((float) this.expand.getValue());
        }
    }

    public double getHitboxExpansion() {
        if (!this.enableRender.getValue()) {
            return 0.0;
        }
        return this.expand.getValue();
    }

    public BooleanSetting getEnableRender() {
        return this.enableRender;
    }
}