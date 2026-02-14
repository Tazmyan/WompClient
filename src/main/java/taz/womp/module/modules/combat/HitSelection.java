package taz.womp.module.modules.combat;

import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.network.packet.s2c.play.PlayerPositionLookS2CPacket;
import taz.womp.event.EventListener;
import taz.womp.event.events.PacketReceiveEvent;
import taz.womp.event.events.TickEvent;
import taz.womp.module.Category;
import taz.womp.module.Module;
import taz.womp.module.setting.BooleanSetting;
import taz.womp.module.setting.MinMaxSetting;
import taz.womp.module.setting.NumberSetting;
import taz.womp.utils.EncryptedString;
import taz.womp.utils.KeyUtils;

public final class HitSelection extends Module {
    private final MinMaxSetting selectRange = new MinMaxSetting(EncryptedString.of("Select Distance"), 0.0, 6.0, 0.1, 2.0, 3.5);
    private final NumberSetting hitRange = new NumberSetting(EncryptedString.of("Hit Distance"), 0.0, 6.0, 2.85, 0.1);
    private final BooleanSetting criticalSpam = new BooleanSetting(EncryptedString.of("Stop On Jump"), true).setDescription(EncryptedString.of("Prevents while jumping to allow crit-spamming"));
    
    private boolean spaced = false;
    private PlayerEntity target = null;

    public HitSelection() {
        super(EncryptedString.of("HitSelection"), EncryptedString.of("Automatically spaces for optimal hit distance"), -1, Category.COMBAT);
        this.addSettings(selectRange, hitRange, criticalSpam);
    }

    @Override
    public void onEnable() {
        spaced = false;
        target = null;
        super.onEnable();
    }

    @Override
    public void onDisable() {

        mc.options.forwardKey.setPressed(KeyUtils.isKeyPressed(mc.options.forwardKey.getDefaultKey().getCode()));
        mc.options.backKey.setPressed(false);
        super.onDisable();
    }

    @EventListener
    public void onPacketReceive(final PacketReceiveEvent event) {
        if (mc.player == null || mc.world == null) return;
        if (event.packet instanceof PlayerPositionLookS2CPacket) {
            target = null;
        }
    }

    @EventListener
    public void onTick(final TickEvent event) {
        if (mc.player == null || mc.world == null) return;


        if (mc.player.getAttacking() instanceof PlayerEntity attackingPlayer && attackingPlayer != null) {
            if (mc.player.handSwingTicks == 1) {
                if (mc.player.age - mc.player.getLastAttackTime() > 5) return;
                if (mc.player.getVelocity().y > 0) return;
                
                target = attackingPlayer;
                spaced = true;
                
                if (target == null) return;
                if (target.isDead() || target.isInCreativeMode() || target.isSpectator() || target.isInvulnerable()) {
                    target = null;
                    return;
                }
                
                double distance = target.distanceTo(mc.player);
                if (distance >= selectRange.getCurrentMin() && distance <= selectRange.getCurrentMax()) {
                    if (!criticalSpam.getValue() || !mc.options.jumpKey.isPressed()) {
                        spaced = true;
                        mc.options.forwardKey.setPressed(false);
                        mc.options.backKey.setPressed(true);
                    }
                }
            }
        }


        if (spaced) {
            if (target != null) {
                if (target.distanceTo(mc.player) > 12) {
                    target = null;
                    return;
                }
                
                if (target.distanceTo(mc.player) >= hitRange.getValue()) {
                    spaced = false;
                    mc.options.forwardKey.setPressed(KeyUtils.isKeyPressed(mc.options.forwardKey.getDefaultKey().getCode()));
                    mc.options.backKey.setPressed(false);
                }
            } else {
                spaced = false;
                mc.options.forwardKey.setPressed(KeyUtils.isKeyPressed(mc.options.forwardKey.getDefaultKey().getCode()));
                mc.options.backKey.setPressed(!mc.options.backKey.isPressed());
            }
        }
    }
}
