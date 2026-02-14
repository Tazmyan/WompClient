package taz.womp.module.modules.misc;

import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Items;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;
import taz.womp.Womp;
import taz.womp.event.EventListener;
import taz.womp.event.events.Render3DEvent;
import taz.womp.event.events.TickEvent;
import taz.womp.module.Category;
import taz.womp.module.Module;
import taz.womp.module.setting.BooleanSetting;
import taz.womp.module.setting.MinMaxSetting;
import taz.womp.module.setting.NumberSetting;
import taz.womp.utils.EncryptedString;

public final class AutoWeb extends Module {
    private final NumberSetting range = new NumberSetting(EncryptedString.of("Range"), 0.0, 6.0, 3.5, 0.1);
    private final NumberSetting fov = new NumberSetting(EncryptedString.of("FOV"), 0.0, 360.0, 180.0, 1.0);
    private final MinMaxSetting placeDelay = new MinMaxSetting(EncryptedString.of("Web Delay"), 0, 40, 5, 10, 10);
    private final BooleanSetting includeHead = new BooleanSetting(EncryptedString.of("Include Head"), false);
    private final BooleanSetting autoSwap = new BooleanSetting(EncryptedString.of("Auto Swap Web"), true);

    private int wait;
    private BlockPos lastPlacePos;

    public AutoWeb() {
        super(EncryptedString.of("Auto Web"), EncryptedString.of("Places cobwebs on enemies"), -1, Category.MISC);
        this.addSettings(range, fov, placeDelay, includeHead, autoSwap);
    }

    @Override
    public void onEnable() {
        wait = 0;
        lastPlacePos = null;
        super.onEnable();
    }

    @EventListener
    public void onRender3D(final Render3DEvent event) {
        if (mc.player == null || mc.world == null) return;
        PlayerEntity target = findTarget();
        if (target == null) return;

        Vec3d feet = getBestFeetPlacement(target);
        Vec3d head = includeHead.getValue() ? getBestHeadPlacement(target) : null;

        if (feet != null) lastPlacePos = BlockPos.ofFloored(feet).down();
        if (head != null) lastPlacePos = BlockPos.ofFloored(head).down();
    }

    @EventListener
    public void onTick(final TickEvent event) {
        if (mc.player == null || mc.world == null) return;
        if (wait > 0) { wait--; }
        if (lastPlacePos == null) return;

        if (wait > 0) return;

        HitResult hit = mc.crosshairTarget;
        if (hit instanceof BlockHitResult bhr) {
            if (bhr.getSide() == Direction.UP && bhr.getBlockPos().equals(lastPlacePos)) {
                if (autoSwap.getValue() && mc.player.getMainHandStack().getItem() != Items.COBWEB) {
                    taz.womp.utils.InventoryUtil.swap(Items.COBWEB);
                    return;
                }
                if (mc.player.getMainHandStack().getItem() == Items.COBWEB) {
                    ActionResult res = mc.interactionManager.interactItem(mc.player, Hand.MAIN_HAND);
                    if (res.isAccepted() && res.shouldSwingHand()) mc.player.swingHand(Hand.MAIN_HAND);
                    wait = placeDelay.getRandomIntInRange();
                }
            }
        }
    }

    private PlayerEntity findTarget() {
        PlayerEntity best = null;
        double bestDist = range.getValue();
        for (PlayerEntity p : mc.world.getPlayers()) {
            if (p == mc.player) continue;
            if (!p.isAlive()) continue;
            if (Womp.INSTANCE.getModuleManager().getModuleByClass(taz.womp.module.modules.combat.AntiBot.class) instanceof taz.womp.module.modules.combat.AntiBot antiBot && antiBot.isEnabled()) {
                if (taz.womp.module.modules.combat.AntiBot.isABot(p)) continue;
            }
            double d = mc.player.distanceTo(p);
            if (d > bestDist) continue;
            if (!isInFov(p)) continue;
            best = p;
            bestDist = d;
        }
        return best;
    }

    private boolean isInFov(PlayerEntity p) {
        Vec3d enemyEye = p.getPos().add(0, p.getEyeHeight(p.getPose()), 0);
        Vec3d look = mc.player.getRotationVector();
        Vec3d toEnemy = enemyEye.subtract(mc.player.getEyePos()).normalize();
        double angle = Math.toDegrees(Math.acos(Math.max(-1, Math.min(1, look.dotProduct(toEnemy)))));
        return angle <= fov.getValue() * 0.5;
    }

    private Vec3d getBestFeetPlacement(PlayerEntity player) {
        Vec3d feet = player.getPos();
        Box bb = player.getBoundingBox();
        Vec3d best = null;
        double bestDist = Double.MAX_VALUE;
        for (int xo = -5; xo <= 5; xo++) {
            for (int zo = -5; zo <= 5; zo++) {
                BlockPos bp = BlockPos.ofFloored(new Vec3d(feet.x + xo, feet.y, feet.z + zo));
                if (!mc.world.getBlockState(bp).isAir()) continue;
                Vec3d center = bp.toCenterPos();
                float r = (float) (bb.getAverageSideLength() / 2f);
                if (!bb.intersects(center.x - r, center.y - r, center.z - r, center.x + r, center.y + r, center.z + r)) continue;
                double dist = center.distanceTo(player.getPos());
                if (dist < bestDist && dist > 0.5) {
                    bestDist = dist;
                    best = center;
                }
            }
        }
        return best;
    }

    private Vec3d getBestHeadPlacement(PlayerEntity player) {
        Vec3d eye = player.getPos().add(0, player.getEyeHeight(player.getPose()), 0);
        Box bb = player.getBoundingBox();
        Vec3d best = null;
        double bestDist = Double.MAX_VALUE;
        for (int xo = -5; xo <= 5; xo++) {
            for (int zo = -5; zo <= 5; zo++) {
                BlockPos bp = BlockPos.ofFloored(new Vec3d(eye.x + xo, eye.y, eye.z + zo));
                if (mc.world.getBlockState(bp.down()).isAir()) continue;
                Vec3d center = bp.toCenterPos();
                float r = (float) (bb.getAverageSideLength() / 2f);
                if (!bb.intersects(center.x - r, center.y - r, center.z - r, center.x + r, center.y + r, center.z + r)) continue;
                double dist = center.distanceTo(player.getPos());
                if (dist < bestDist && dist > 0.5) {
                    bestDist = dist;
                    best = center;
                }
            }
        }
        return best;
    }
}


