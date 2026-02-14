package taz.womp.module.modules.combat;

import net.minecraft.enchantment.Enchantments;
import net.minecraft.item.AxeItem;
import net.minecraft.item.Item;
import net.minecraft.item.Items;
import net.minecraft.item.SwordItem;
import taz.womp.event.EventListener;
import taz.womp.event.events.AttackEvent;
import taz.womp.event.events.TickEvent;
import taz.womp.module.Category;
import taz.womp.module.Module;
import taz.womp.module.setting.BooleanSetting;
import taz.womp.module.setting.NumberSetting;
import taz.womp.utils.EncryptedString;
import taz.womp.utils.InventoryUtil;

public final class MaceSwap extends Module {
    private final BooleanSetting enableWindBurst = new BooleanSetting(EncryptedString.of("Wind Burst"), true);
    private final BooleanSetting enableBreach = new BooleanSetting(EncryptedString.of("Breach"), true);
    private final BooleanSetting onlySword = new BooleanSetting(EncryptedString.of("Only Sword"), false);
    private final BooleanSetting onlyAxe = new BooleanSetting(EncryptedString.of("Only Axe"), false);
    private final BooleanSetting switchBack = new BooleanSetting(EncryptedString.of("Switch Back"), true);
    private final NumberSetting switchDelay = new NumberSetting(EncryptedString.of("Switch Delay"), 0.0, 20.0, 0.0, 1.0);
    private boolean isSwitching;
    private int previousSlot;
    private int currentSwitchDelay;

    public MaceSwap() {
        super(EncryptedString.of("Mace Swap"), EncryptedString.of("Switches to a mace when attacking."), -1, Category.COMBAT);
        this.addSettings(this.enableWindBurst, this.enableBreach, this.onlySword, this.onlyAxe, this.switchBack, this.switchDelay);
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
        if (this.isSwitching) {
            if (this.switchBack.getValue()) {
                this.performSwitchBack();
            } else {
                this.resetState();
            }
        }
    }

    @EventListener
    public void onAttack(final AttackEvent attackEvent) {
        if (this.mc.player == null || this.isSwitching) {
            return;
        }
        if (!this.isValidWeapon()) {
            return;
        }
        int maceSlot = -1;
        final boolean windBurst = this.enableWindBurst.getValue();
        final boolean breach = this.enableBreach.getValue();
        if (windBurst && breach) {
            maceSlot = InventoryUtil.findItemWithEnchantments(Items.MACE, Enchantments.WIND_BURST, Enchantments.BREACH);
        } else if (windBurst) {
            maceSlot = InventoryUtil.findItemWithEnchantment(Items.MACE, Enchantments.WIND_BURST);
        } else if (breach) {
            maceSlot = InventoryUtil.findItemWithEnchantment(Items.MACE, Enchantments.BREACH);
        } else {
            maceSlot = InventoryUtil.findItem(Items.MACE);
        }
        if (maceSlot != -1) {
            this.previousSlot = this.mc.player.getInventory().selectedSlot;
            InventoryUtil.swap(maceSlot);
            this.isSwitching = true;
        }
    }

    private boolean isValidWeapon() {
        final Item item = this.mc.player.getMainHandStack().getItem();
        if (this.onlySword.getValue() && this.onlyAxe.getValue()) {
            return item instanceof SwordItem || item instanceof AxeItem;
        }
        return (!this.onlySword.getValue() || item instanceof SwordItem) && (!this.onlyAxe.getValue() || item instanceof AxeItem);
    }

    private void performSwitchBack() {
        if (this.currentSwitchDelay < this.switchDelay.getIntValue()) {
            ++this.currentSwitchDelay;
            return;
        }
        InventoryUtil.swap(this.previousSlot);
        this.resetState();
    }

    private void resetState() {
        this.previousSlot = -1;
        this.currentSwitchDelay = 0;
        this.isSwitching = false;
    }
}
