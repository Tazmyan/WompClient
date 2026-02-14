package taz.womp.module.modules.misc;

import net.minecraft.block.CobwebBlock;
import net.minecraft.item.Items;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;
import taz.womp.event.EventListener;
import taz.womp.event.events.Render3DEvent;
import taz.womp.event.events.TickEvent;
import taz.womp.module.Category;
import taz.womp.module.Module;
import taz.womp.module.setting.BooleanSetting;
import taz.womp.module.setting.MinMaxSetting;
import taz.womp.utils.EncryptedString;

import java.awt.*;

public final class AntiWeb extends Module {
    private final MinMaxSetting delay = new MinMaxSetting(EncryptedString.of("Delay"), 0, 20, 0, 10, 10);
    private final BooleanSetting autoSwap = new BooleanSetting(EncryptedString.of("Auto Swap Water"), true);
    private int wait;
    private boolean awaitingPickup;

    public AntiWeb() {
        super(EncryptedString.of("AntiWeb"), EncryptedString.of("Places water on cobwebs you're stuck in"), -1, Category.MISC);
        this.addSettings(delay, autoSwap);
    }

    @Override
    public void onEnable() {
        wait = 0;
        awaitingPickup = false;
        super.onEnable();
    }

    @EventListener
    public void onTick(final TickEvent event) {
        if (mc.player == null || mc.world == null) return;

        if (wait > 0) { wait--; }

        Vec3d feetCenter = getCobwebFeetCenter();
        

        if (awaitingPickup && feetCenter == null) {

            ActionResult res = mc.interactionManager.interactItem(mc.player, Hand.MAIN_HAND);
            if (res.isAccepted() && res.shouldSwingHand()) mc.player.swingHand(Hand.MAIN_HAND);
            awaitingPickup = false;
        }

        if (feetCenter == null) {
            return;
        }

        if (wait > 0) return;

        HitResult hit = mc.crosshairTarget;
        if (hit instanceof BlockHitResult bhr) {
            if (bhr.getSide() == Direction.UP && mc.world.getBlockState(bhr.getBlockPos()).getBlock() instanceof CobwebBlock) {
                if (autoSwap.getValue() && mc.player.getMainHandStack().getItem() != Items.WATER_BUCKET) {
                    taz.womp.utils.InventoryUtil.swap(Items.WATER_BUCKET);
                    return;
                }
                if (mc.player.getMainHandStack().getItem() == Items.WATER_BUCKET) {
                    ActionResult res = mc.interactionManager.interactItem(mc.player, Hand.MAIN_HAND);
                    if (res.isAccepted() && res.shouldSwingHand()) mc.player.swingHand(Hand.MAIN_HAND);
                    awaitingPickup = true;
                    wait = delay.getRandomIntInRange();
                }
            }
        }
    }

    @EventListener
    public void onRender3D(final Render3DEvent event) {
        if (mc.player == null || mc.world == null) return;
        Vec3d feetCenter = getCobwebFeetCenter();
        if (feetCenter != null) {
            renderBox(feetCenter, new Color(255, 50, 50, 120));
        }
    }

    private Vec3d getCobwebFeetCenter() {
        Box box = mc.player.getBoundingBox();
        Vec3d pos = mc.player.getPos();
        Vec3d bestPosition = null;
        double closestDistance = Double.MAX_VALUE;
        

        for (int xOff = -1; xOff <= 1; xOff++) {
            for (int zOff = -1; zOff <= 1; zOff++) {
                BlockPos feet = BlockPos.ofFloored(new Vec3d(pos.x + xOff, pos.y, pos.z + zOff));
                if (!(mc.world.getBlockState(feet).getBlock() instanceof CobwebBlock)) continue;
                
                Vec3d center = feet.toCenterPos();
                float r = (float) (box.getAverageSideLength() / 2f);
                

                if (box.intersects(center.x - r, center.y - r, center.z - r, center.x + r, center.y + r, center.z + r)) {

                    double distance = center.distanceTo(pos);
                    if (distance < closestDistance) {
                        closestDistance = distance;
                        bestPosition = center;
                    }
                }
            }
        }
        return bestPosition;
    }

    private void renderBox(Vec3d center, Color color) {



    }
}


