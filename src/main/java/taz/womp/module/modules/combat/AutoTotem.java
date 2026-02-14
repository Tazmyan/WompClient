package taz.womp.module.modules.combat;

import net.minecraft.item.Item;
import net.minecraft.item.Items;
import net.minecraft.screen.slot.SlotActionType;
import taz.womp.event.EventListener;
import taz.womp.event.events.TickEvent;
import taz.womp.module.Category;
import taz.womp.module.Module;
import taz.womp.module.setting.NumberSetting;
import taz.womp.utils.EncryptedString;

public final class AutoTotem extends Module {
    private final NumberSetting delay = new NumberSetting(EncryptedString.of("Delay"), 0.0, 5.0, 1.0, 1.0);
    private int delayCounter;

    public AutoTotem() {
        super(EncryptedString.of("Auto Totem"), EncryptedString.of("Automatically holds totem in your off hand"), -1, Category.COMBAT);
        this.addSettings(this.delay);
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
    public void onTick(final TickEvent event) {
        if (this.mc.player == null || this.mc.currentScreen != null) {
            return;
        }
        if (this.mc.player.getInventory().getStack(40).getItem() == Items.TOTEM_OF_UNDYING) {
            this.delayCounter = this.delay.getIntValue();
            return;
        }
        if (this.delayCounter > 0) {
            --this.delayCounter;
            return;
        }
        final int slot = this.findItemSlot(Items.TOTEM_OF_UNDYING);
        if (slot == -1) {
            return;
        }
        this.mc.interactionManager.clickSlot(this.mc.player.currentScreenHandler.syncId, convertSlotIndex(slot), 40, SlotActionType.SWAP, this.mc.player);
        this.delayCounter = this.delay.getIntValue();
    }

    public int findItemSlot(final Item item) {
        if (this.mc.player == null) {
            return -1;
        }
        for (int i = 0; i < 36; ++i) {
            if (this.mc.player.getInventory().getStack(i).isOf(item)) {
                return i;
            }
        }
        return -1;
    }

    private static int convertSlotIndex(final int slotIndex) {
        if (slotIndex < 9) {
            return 36 + slotIndex;
        }
        return slotIndex;
    }
}