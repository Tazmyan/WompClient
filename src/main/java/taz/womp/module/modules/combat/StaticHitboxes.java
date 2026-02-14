package taz.womp.module.modules.combat;

import net.minecraft.entity.EntityPose;
import net.minecraft.entity.player.PlayerEntity;
import taz.womp.Womp;
import taz.womp.event.EventListener;
import taz.womp.event.events.TargetPoseEvent;
import taz.womp.module.Category;
import taz.womp.module.Module;
import taz.womp.utils.EncryptedString;
import taz.womp.module.setting.BooleanSetting;

public final class StaticHitboxes extends Module {
    private final BooleanSetting enableRender = new BooleanSetting(EncryptedString.of("Enable Render"), true);

    public StaticHitboxes() {
        super(EncryptedString.of("Static HitBoxes"), EncryptedString.of("Expands a Player's Hitbox"), -1, Category.COMBAT);
        this.addSettings(this.enableRender);
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
    public void onTargetPose(final TargetPoseEvent targetPoseEvent) {
        if (this.isEnabled() && this.enableRender.getValue() && targetPoseEvent.entity instanceof PlayerEntity player && !player.isMainPlayer()) {

            AntiBot antiBot = (AntiBot) Womp.INSTANCE.getModuleManager().getModuleByClass(AntiBot.class);
            if (antiBot != null && antiBot.isEnabled() && AntiBot.isABot(player)) {
                return;
            }
            targetPoseEvent.cir.setReturnValue(EntityPose.STANDING);
        }
    }

    public BooleanSetting getEnableRender() {
        return this.enableRender;
    }
}
