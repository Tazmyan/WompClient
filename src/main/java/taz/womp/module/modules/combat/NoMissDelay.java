package taz.womp.module.modules.combat;

import net.minecraft.item.AxeItem;
import net.minecraft.item.SwordItem;
import net.minecraft.util.hit.HitResult;
import taz.womp.event.EventListener;
import taz.womp.event.events.AttackEvent;
import taz.womp.event.events.BlockBreakingEvent;
import taz.womp.module.Category;
import taz.womp.module.Module;
import taz.womp.module.setting.BooleanSetting;
import taz.womp.utils.EncryptedString;

public class NoMissDelay extends Module {
    private final BooleanSetting onlyWeapon = new BooleanSetting(EncryptedString.of("Only Weapon"), true);
    private final BooleanSetting onMiss = new BooleanSetting(EncryptedString.of("On Miss"), true);
    private final BooleanSetting onBlock = new BooleanSetting(EncryptedString.of("On Block"), false);

    public NoMissDelay() {
        super(EncryptedString.of("NoMissDelay"), EncryptedString.of("Removes attack delay after missing."), -1, Category.COMBAT);
        addSettings(onlyWeapon, onMiss, onBlock);
    }

    @EventListener
    public void onAttack(AttackEvent event) {
        if (mc.player == null || mc.crosshairTarget == null) {
            return;
        }

        if (onlyWeapon.getValue() && !(mc.player.getMainHandStack().getItem() instanceof SwordItem || mc.player.getMainHandStack().getItem() instanceof AxeItem)) {
            return;
        }

        switch (mc.crosshairTarget.getType()) {
            case MISS -> {
                if (onMiss.getValue()) event.cancel();
            }
            case BLOCK -> {
                if (onBlock.getValue()) event.cancel();
            }
            case ENTITY -> {
            }
        }
    }

    @EventListener
    public void onBlockBreaking(BlockBreakingEvent event) {
        if (mc.player == null || mc.crosshairTarget == null) {
            return;
        }

        if (onlyWeapon.getValue() && !(mc.player.getMainHandStack().getItem() instanceof SwordItem || mc.player.getMainHandStack().getItem() instanceof AxeItem)) {
            return;
        }

        if (mc.crosshairTarget.getType() == HitResult.Type.BLOCK && onBlock.getValue()) {
            event.cancel();
        }
    }
} 