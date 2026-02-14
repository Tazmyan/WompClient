package taz.womp.module.modules.combat;

import net.minecraft.entity.EquipmentSlot;
import net.minecraft.item.ArmorItem;
import net.minecraft.item.Item;
import net.minecraft.item.Items;
import net.minecraft.screen.slot.SlotActionType;
import net.minecraft.util.Hand;
import taz.womp.event.EventListener;
import taz.womp.event.events.TickEvent;
import taz.womp.module.Category;
import taz.womp.module.Module;
import taz.womp.module.setting.BindSetting;
import taz.womp.module.setting.BooleanSetting;
import taz.womp.module.setting.NumberSetting;
import taz.womp.utils.EncryptedString;
import taz.womp.utils.InventoryUtil;
import taz.womp.utils.KeyUtils;

import java.util.function.Predicate;

public final class ElytraSwap extends Module {
    private final BindSetting activateKey = new BindSetting(EncryptedString.of("Activate Key"), 71, false);
    private final NumberSetting swapDelay = new NumberSetting(EncryptedString.of("Delay"), 0.0, 20.0, 0.0, 1.0);
    private final BooleanSetting switchBack = new BooleanSetting(EncryptedString.of("Switch Back"), true);
    private final NumberSetting switchDelay = new NumberSetting(EncryptedString.of("Switch Delay"), 0.0, 20.0, 0.0, 1.0);
    private final BooleanSetting moveToSlot = new BooleanSetting(EncryptedString.of("Move to slot"), true).setDescription(EncryptedString.of("If elytra is not in hotbar it will move it from inventory to preferred slot"));
    private final NumberSetting elytraSlot = new NumberSetting(EncryptedString.of("Elytra Slot"), 1.0, 9.0, 9.0, 1.0).getValue(EncryptedString.of("Your preferred elytra slot"));
    private boolean isSwapping;
    private boolean isSwinging;
    private boolean isItemSwapped;
    private int swapCounter;
    private int switchCounter;
    private int activationCooldown;
    private int originalSlot;

    public ElytraSwap() {
        super(EncryptedString.of("Elytra Swap"), EncryptedString.of("Seamlessly swap between an Elytra and a Chestplate with a configurable keybinding"), -1, Category.COMBAT);
        this.addSettings(this.activateKey, this.swapDelay, this.switchBack, this.switchDelay, this.moveToSlot, this.elytraSlot);
    }

    @Override
    public void onEnable() {
        this.resetState();
        super.onEnable();
    }

    @Override
    public void onDisable() {
        super.onDisable();
    }

    @EventListener
    public void onTick(final TickEvent event) {
        if (this.mc.currentScreen != null) {
            return;
        }
        if (this.mc.player == null) {
            return;
        }
        if (this.activationCooldown > 0) {
            --this.activationCooldown;
        } else if (KeyUtils.isKeyPressed(this.activateKey.getValue())) {
            this.isSwapping = true;
            this.activationCooldown = 4;
        }
        if (this.isSwapping) {
            if (this.originalSlot == -1) {
                this.originalSlot = this.mc.player.getInventory().selectedSlot;
            }
            if (this.swapCounter < this.swapDelay.getIntValue()) {
                ++this.swapCounter;
                return;
            }
            Predicate<Item> predicate;

            if (this.mc.player.getInventory().getArmorStack(EquipmentSlot.CHEST.getEntitySlotId()).isOf(Items.ELYTRA)) {
                predicate = (item -> item instanceof ArmorItem && ((ArmorItem) item).getSlotType() == EquipmentSlot.CHEST);
            } else {
                predicate = (item2 -> item2.equals(Items.ELYTRA));
            }

            if (!this.isItemSwapped) {
                if (!InventoryUtil.swapItem(predicate)) {
                    if (!this.moveToSlot.getValue()) {
                        this.resetState();
                        return;
                    }

                    int elytraInventorySlot = this.findElytraSlot();
                    if (elytraInventorySlot == -1) {
                        this.resetState();
                        return;
                    }

                    this.mc.interactionManager.clickSlot(this.mc.player.currentScreenHandler.syncId, elytraInventorySlot, this.elytraSlot.getIntValue() - 1, SlotActionType.SWAP, this.mc.player);
                    this.swapCounter = 0;
                    return;
                } else {
                    this.isItemSwapped = true;
                }
            }
            if (!this.isSwinging) {
                this.mc.interactionManager.interactItem(this.mc.player, Hand.MAIN_HAND);
                this.mc.player.swingHand(Hand.MAIN_HAND);
                this.isSwinging = true;
            }
            if (this.switchBack.getValue()) {
                this.handleSwitchBack();
            } else {
                this.resetState();
            }
        }
    }

    private void handleSwitchBack() {
        if (this.switchCounter < this.switchDelay.getIntValue()) {
            ++this.switchCounter;
            return;
        }
        InventoryUtil.swap(this.originalSlot);
        this.resetState();
    }

    private void resetState() {
        this.originalSlot = -1;
        this.switchCounter = 0;
        this.swapCounter = 0;
        this.isSwapping = false;
        this.isSwinging = false;
        this.isItemSwapped = false;
    }

    private int findElytraSlot() {
        for (int i = 9; i < 36; i++) {
            if (this.mc.player.getInventory().getStack(i).getItem() == Items.ELYTRA) {
                return i;
            }
        }
        return -1;
    }
}
