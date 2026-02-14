package taz.womp.module.modules.movement;

import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.item.Items;
import taz.womp.event.EventListener;
import taz.womp.event.events.PreItemUseEvent;
import taz.womp.module.Category;
import taz.womp.module.Module;
import taz.womp.module.setting.MinMaxSetting;
import taz.womp.utils.EncryptedString;

public final class ElytraGlide extends Module {
    private int glideDelay = 0;
    private boolean waitingToGlide = false;
    private int jumpStage = 0;
    private int jumpDelay = 0; 
    private final MinMaxSetting delay = new MinMaxSetting(EncryptedString.of("Delay"), 1, 20, 1, 2, 5);
    private int jumpKeyTicks = 0;
    private boolean jumpKeyActive = false;
    private int jumpPressStage = 0;
    private int jumpPressTicks = 0;

    public ElytraGlide() {
        super(EncryptedString.of("AutoElytra"), EncryptedString.of("Starts flying when attempting to use a firework"), -1, Category.MOVEMENT);
        this.addSettings(delay);
    }

    @EventListener
    public void onPreItemUse(final PreItemUseEvent event) {
        ClientPlayerEntity player = mc.player;
        if (player == null || mc.world == null) return;

        boolean hasFirework = player.getMainHandStack().getItem() == Items.FIREWORK_ROCKET ||
                               player.getOffHandStack().getItem() == Items.FIREWORK_ROCKET;
        if (hasFirework &&
            player.getInventory().getArmorStack(EquipmentSlot.CHEST.getEntitySlotId()).getItem() == Items.ELYTRA &&
            !player.isFallFlying()) {

            if (player.isOnGround()) {
                jumpPressStage = 1;
                jumpPressTicks = 0;
                waitingToGlide = true;
                glideDelay = delay.getRandomIntInRange();
                return;
            }

            if ((player.getVelocity().y < -0.08 || player.fallDistance > 0.5 || player.getVelocity().y > 0)) {
                mc.options.jumpKey.setPressed(true);
                jumpKeyActive = true;
                jumpKeyTicks = 0;
                waitingToGlide = false;
                jumpStage = 0;
            }
        }
    }

    @EventListener
    public void onTick(taz.womp.event.events.TickEvent event) {

        if (jumpPressStage > 0) {
            jumpPressTicks++;
            if (jumpPressStage == 1) {
                mc.options.jumpKey.setPressed(true);
                if (jumpPressTicks >= 2) {
                    jumpPressStage = 2;
                    jumpPressTicks = 0;
                }
            } else if (jumpPressStage == 2) {
                mc.options.jumpKey.setPressed(false);
                if (jumpPressTicks >= 2) {
                    jumpPressStage = 3;
                    jumpPressTicks = 0;
                }
            } else if (jumpPressStage == 3) {
                mc.options.jumpKey.setPressed(true);
                if (jumpPressTicks >= 2) {
                    jumpPressStage = 4;
                    jumpPressTicks = 0;
                }
            } else if (jumpPressStage == 4) {
                mc.options.jumpKey.setPressed(false);
                jumpPressStage = 0;
                jumpPressTicks = 0;
            }
        }
        if (jumpKeyActive) {
            jumpKeyTicks++;
            if (jumpKeyTicks >= 2) {
                mc.options.jumpKey.setPressed(false);
                jumpKeyActive = false;
                jumpKeyTicks = 0;
            }
        }
        if (!waitingToGlide || mc.player == null) return;
        if (jumpStage == 1) {
            if (jumpDelay > 0) {
                jumpDelay--;
                return;
            }
            if (!mc.player.isOnGround()) {
                mc.options.jumpKey.setPressed(true);
                jumpKeyActive = true;
                jumpKeyTicks = 0;
                jumpStage = 2;
                glideDelay = delay.getRandomIntInRange();
                return;
            }
        }
        if (jumpStage == 2 && !mc.player.isOnGround() && mc.player.getVelocity().y < 0) {
            if (glideDelay > 0) {
                glideDelay--;
                return;
            }
            waitingToGlide = false;
            jumpStage = 0;
        }
    }
}
