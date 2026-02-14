package taz.womp.module.modules.misc;

import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.util.Hand;
import taz.womp.event.EventListener;
import taz.womp.event.events.TickEvent;
import taz.womp.module.Category;
import taz.womp.module.Module;
import taz.womp.module.setting.BindSetting;
import taz.womp.module.setting.BooleanSetting;
import taz.womp.module.setting.MinMaxSetting;
import taz.womp.utils.EncryptedString;
import taz.womp.utils.KeyUtils;

public final class AutoFirework extends Module {
    private final BindSetting activateKey = new BindSetting(EncryptedString.of("Activate Key"), -1, false);
    private final MinMaxSetting delay = new MinMaxSetting(EncryptedString.of("Delay"), 0, 20, 1, 2, 5);
    private final BooleanSetting switchBack = new BooleanSetting(EncryptedString.of("Switch Back"), true);
    private final MinMaxSetting switchDelay = new MinMaxSetting(EncryptedString.of("Switch Delay"), 0, 20, 1, 2, 5);
	private final BooleanSetting hold = new BooleanSetting(EncryptedString.of("Hold Activation"), false);

    private boolean actionTaken = false;
    private int previousSelectedSlot = -1;
    private int switchDelayCounter = 0;
    private boolean pendingUse = false;
    private int useKeyTicks = 0;
    private boolean useKeyActive = false;
    private int pendingDelay = 0;

    public AutoFirework() {
        super(EncryptedString.of("Auto Firework"), EncryptedString.of("Switches to a firework and uses it when you press a bind."), -1, Category.MISC);
		this.addSettings(this.activateKey, this.delay, this.switchBack, this.switchDelay, this.hold);
        this.switchDelay.setDescription(EncryptedString.of("Delay after using firework before switching back."));
    }

    @Override
    public void onDisable() {
        resetState();
        super.onDisable();
    }

    @EventListener
    public void onTick(final TickEvent event) {
        if (mc.player == null || mc.currentScreen != null) return;
        if (!this.isEnabled()) return;
        if (this.activateKey.getValue() == -1) return;

		boolean keyPressed = KeyUtils.isKeyPressed(this.activateKey.getValue());
		if (!keyPressed && !pendingUse && !useKeyActive && !actionTaken) return;

        if (!mc.player.isFallFlying()) {
            resetState();
            return;
        }


        if (useKeyActive) {
            useKeyTicks++;
            if (useKeyTicks >= 2) {
                mc.options.useKey.setPressed(false);
                useKeyActive = false;
                useKeyTicks = 0;
                this.switchDelayCounter = 0;
                this.actionTaken = true;

				if (switchBack.getValue() && previousSelectedSlot != -1) {
					mc.player.getInventory().selectedSlot = this.previousSelectedSlot;

					if (shouldHoldRepeat(keyPressed)) {
						scheduleNextUse();
					} else {
						resetState();
					}
				} else {

					if (shouldHoldRepeat(keyPressed)) {
						scheduleNextUse();
					} else {
						resetState();
					}
				}
            }
            return;
        }

        if (pendingUse) {
            if (pendingDelay > 0) {
                pendingDelay--;
                return;
            }
            Hand fireworkHand = getFireworkInHand();
            if (fireworkHand != null) {
                mc.options.useKey.setPressed(true);
                useKeyActive = true;
                useKeyTicks = 0;
                pendingUse = false;
                return;
            } else {
                resetState();
                return;
            }
        }

		if (actionTaken) {
			if (switchBack.getValue() && previousSelectedSlot != -1) {
				handleSwitchBack();
			} else {

				if (!pendingUse) {
					resetState();
				}
			}
			return;
		}

        if (mc.player.getMainHandStack().getItem() == Items.FIREWORK_ROCKET) {
            if (keyPressed || pendingUse) {
                mc.options.useKey.setPressed(true);
                useKeyActive = true;
                useKeyTicks = 0;
                return;
            }
        } else if (mc.player.getOffHandStack().getItem() == Items.FIREWORK_ROCKET) {
            if (keyPressed || pendingUse) {
                mc.options.useKey.setPressed(true);
                useKeyActive = true;
                useKeyTicks = 0;
                return;
            }
        }

        int fireworkSlot = findFireworkInHotbar();
        if (fireworkSlot != -1 && keyPressed) {
            this.previousSelectedSlot = mc.player.getInventory().selectedSlot;
            mc.player.getInventory().selectedSlot = fireworkSlot;
            this.pendingUse = true;
            this.pendingDelay = delay.getRandomIntInRange();
        }
    }

    private void handleSwitchBack() {
		if (this.switchDelayCounter < this.switchDelay.getRandomIntInRange()) {
            ++this.switchDelayCounter;
            return;
        }
		mc.player.getInventory().selectedSlot = this.previousSelectedSlot;

		if (shouldHoldRepeat(KeyUtils.isKeyPressed(this.activateKey.getValue()))) {
			scheduleNextUse();
		} else {
			resetState();
		}
    }

    private void resetState() {
        this.actionTaken = false;
        this.previousSelectedSlot = -1;
        this.switchDelayCounter = 0;
        this.pendingUse = false;
        this.useKeyTicks = 0;
        this.useKeyActive = false;
        this.pendingDelay = 0;
    }

	private boolean shouldHoldRepeat(boolean keyPressed) {
		return this.hold.getValue() && keyPressed && mc.player != null && mc.player.isFallFlying();
	}

	private void scheduleNextUse() {
		this.actionTaken = false;
		this.pendingUse = true;
		this.pendingDelay = delay.getRandomIntInRange();
		this.previousSelectedSlot = -1;
		this.switchDelayCounter = 0;
		this.useKeyActive = false;
		this.useKeyTicks = 0;
	}

    private Hand getFireworkInHand() {
        if (mc.player.getMainHandStack().getItem() == Items.FIREWORK_ROCKET) {
            return Hand.MAIN_HAND;
        }
        if (mc.player.getOffHandStack().getItem() == Items.FIREWORK_ROCKET) {
            return Hand.OFF_HAND;
        }
        return null;
    }

    private int findFireworkInHotbar() {
        for (int i = 0; i < 9; i++) {
            ItemStack stack = mc.player.getInventory().getStack(i);
            if (stack.getItem() == Items.FIREWORK_ROCKET) {
                return i;
            }
        }
        return -1;
    }
}
