package taz.womp.module.modules.combat;

import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.decoration.ArmorStandEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.AxeItem;
import net.minecraft.item.Item;
import net.minecraft.item.SwordItem;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import org.lwjgl.glfw.GLFW;
import taz.womp.Womp;
import taz.womp.event.EventListener;
import taz.womp.event.events.Render3DEvent;
import taz.womp.event.events.TickEvent;
import taz.womp.event.events.AttackEvent;
import taz.womp.module.Category;
import taz.womp.module.Module;
import taz.womp.module.setting.BooleanSetting;
import taz.womp.module.setting.ModeSetting;
import taz.womp.module.setting.NumberSetting;
import taz.womp.module.setting.MinMaxSetting;
import taz.womp.utils.EncryptedString;
import taz.womp.utils.RotationUtils;
import taz.womp.utils.WorldUtils;
import taz.womp.utils.TimerUtils;
import taz.womp.utils.MouseSimulation;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public class KillAura extends Module {
    private final MinecraftClient mc = MinecraftClient.getInstance();
    

    private final BooleanSetting onlyWeapon = new BooleanSetting(EncryptedString.of("Only Weapon"), true);
    private final BooleanSetting checkShield = new BooleanSetting(EncryptedString.of("Check Shield"), false);
    private final NumberSetting range = new NumberSetting(EncryptedString.of("Range"), 0.1, 6.0, 3.4, 0.1);
    

    private enum AttackMode { Cooldown, Delay }
    private final ModeSetting<AttackMode> attackMode = new ModeSetting<>(EncryptedString.of("Attack Mode"), AttackMode.Cooldown, AttackMode.values());
    private final MinMaxSetting cooldownPercent = new MinMaxSetting(EncryptedString.of("Cooldown"), 0, 100, 1, 80, 100);
    private final MinMaxSetting attackDelay = new MinMaxSetting(EncryptedString.of("Attack Delay"), 0, 1000, 1, 10, 25);
    

    private enum SortMode { Distance, HurtTime, Health, Rotation }
    private final ModeSetting<SortMode> targetSorting = new ModeSetting<>(EncryptedString.of("Target Sorting"), SortMode.Distance, SortMode.values());
    

    private enum AimMode { Regular, Adaptive, Linear, Blatant }
    private final ModeSetting<AimMode> aimMode = new ModeSetting<>(EncryptedString.of("Aim Mode"), AimMode.Regular, AimMode.values());
    private final BooleanSetting silentAim = new BooleanSetting(EncryptedString.of("Silent Aim"), true);
    

    private final NumberSetting attackJitter = new NumberSetting(EncryptedString.of("Attack Jitter"), 0.0, 50.0, 5.0, 1.0);
    private final NumberSetting rotationJitter = new NumberSetting(EncryptedString.of("Rotation Jitter"), 0.0, 10.0, 2.0, 0.1);
    private final NumberSetting multiPoint = new NumberSetting(EncryptedString.of("Multi Point"), 0.0, 1.0, 0.5, 0.1);
    private final BooleanSetting stickyTargeting = new BooleanSetting(EncryptedString.of("Sticky Targeting"), true);
    private final BooleanSetting simulateClicks = new BooleanSetting(EncryptedString.of("Simulate Clicks"), true);
    

    private final BooleanSetting requireWeapon = new BooleanSetting(EncryptedString.of("Require Weapon"), false);
    

    private final TimerUtils hitTimer = new TimerUtils();
    private final TimerUtils targetAcquireTimer = new TimerUtils();
    private LivingEntity currentTarget = null;

    public KillAura() {
        super(EncryptedString.of("KillAura"), EncryptedString.of("Automatically attacks entities in front of you"), -1, Category.COMBAT);
        this.addSettings(onlyWeapon, checkShield, range, attackMode, cooldownPercent, attackDelay, targetSorting, 
                        aimMode, silentAim, attackJitter, rotationJitter, multiPoint, stickyTargeting, simulateClicks, requireWeapon);
        

        attackDelay.setVisibility(() -> attackMode.getValue() == AttackMode.Delay);
        cooldownPercent.setVisibility(() -> attackMode.getValue() == AttackMode.Cooldown);
    }

    @Override
    public void onEnable() {
        super.onEnable();
        currentTarget = null;
        hitTimer.reset();
        targetAcquireTimer.reset();
    }

    @Override
    public void onDisable() {
        super.onDisable();
        currentTarget = null;
    }

    @EventListener
    public void onRender3D(final Render3DEvent event) {
        if (mc.player == null || mc.world == null || mc.currentScreen != null) return;
        

        updateTarget();
        
        if (currentTarget != null) {

            aimAtTarget(currentTarget);
        }
    }

    @EventListener
    public void onTick(final TickEvent event) {
        if (mc.currentScreen != null) return;
        if (mc.player == null || mc.world == null) return;
        if (mc.player.isUsingItem()) return;

        Item item = mc.player.getMainHandStack().getItem();
        if (onlyWeapon.getValue() && !(item instanceof SwordItem || item instanceof AxeItem)) {
            return;
        }


        if (requireWeapon.getValue() && !(mc.player.getMainHandStack().getItem() instanceof SwordItem)) {
            return;
        }

        if (currentTarget == null) return;


        AttackMode mode = (AttackMode) attackMode.getValue();
        boolean shouldAttack = false;
        
        switch (mode) {
            case Cooldown -> {
                float cooldownProgress = mc.player.getAttackCooldownProgress(0.5F);
                float threshold = cooldownPercent.getRandomFloatInRange() / 100f;
                shouldAttack = cooldownProgress >= threshold;
            }
            case Delay -> {
                int delay = getRandomAttackDelay();
                shouldAttack = hitTimer.hasReached(Math.max(1, delay));
            }
        }

        if (!shouldAttack) {
            if (mode != AttackMode.Delay) hitTimer.reset();
            return;
        }


        if (attackJitter.getValue() > 0 && Math.random() < attackJitter.getValue() / 100.0) {
            return;
        }


        WorldUtils.hitEntity(currentTarget, true);
        

        if (simulateClicks.getValue()) {
            MouseSimulation.mouseClick(GLFW.GLFW_MOUSE_BUTTON_LEFT);
        }
        
        hitTimer.reset();
    }

    @EventListener
    public void onAttack(final AttackEvent event) {

        if (currentTarget != null && mc.player.distanceTo(currentTarget) <= range.getValue()) {

        }
    }

    private int getRandomAttackDelay() {
        int min = attackDelay.getMinInt();
        int max = attackDelay.getMaxInt();
        if (min >= max) return min;
        return min + (int)(Math.random() * (max - min + 1));
    }

    private void updateTarget() {
        if (!stickyTargeting.getValue() || currentTarget == null) {
            LivingEntity newTarget = findBestTarget();
            if (newTarget != null && newTarget != currentTarget) {
                currentTarget = newTarget;
                targetAcquireTimer.reset();
            } else if (newTarget == null) {
                currentTarget = null;
            }
        } else {

            if (currentTarget != null) {
                double distance = mc.player.distanceTo(currentTarget);
                if (distance > range.getValue() || !isValidTarget((LivingEntity) currentTarget)) {
                    currentTarget = null;
                }
            }
        }
    }

    private LivingEntity findBestTarget() {
        return (LivingEntity) toList(mc.world.getEntities()).stream()
                .filter(entity -> {
                    if (entity == mc.player) return false;
                    if (mc.player.distanceTo(entity) >= range.getValue()) return false;
                    if (!(entity instanceof LivingEntity)) return false;
                    if (entity instanceof ArmorStandEntity) return false;
                    if (entity instanceof PlayerEntity player) {
                        if (!isValidTarget(player)) return false;
                    }
                    return true;
                })
                .min(Comparator.comparingDouble(entity -> {
                    SortMode sortMode = (SortMode) targetSorting.getValue();
                    switch (sortMode) {
                        case HurtTime -> { return ((LivingEntity) entity).hurtTime; }
                        case Distance -> { return mc.player.distanceTo(entity); }
                        case Health -> { return ((LivingEntity) entity).getHealth(); }
                        case Rotation -> {
                            Vec3d eyePos = entity.getEyePos();
                            Vec3d playerEyePos = mc.player.getEyePos();
                            double angle = Math.abs(MathHelper.wrapDegrees(
                                (float) Math.toDegrees(Math.atan2(eyePos.z - playerEyePos.z, eyePos.x - playerEyePos.x)) - 90.0f) - mc.player.getYaw());
                            return angle;
                        }
                        default -> { return mc.player.distanceTo(entity); }
                    }
                }))
                .orElse(null);
    }

    private void aimAtTarget(LivingEntity target) {
        Vec3d aimPos = getMultiPointTargetPosition(target);
        if (aimPos == null) return;
        
        RotationUtils.Rotation needed = RotationUtils.getNeededRotations(aimPos);
        if (needed == null) return;

        float currentYaw = mc.player.getYaw();
        float currentPitch = mc.player.getPitch();
        float targetYaw = (float) needed.getYaw();
        float targetPitch = (float) needed.getPitch();


        float yawDiff = wrapDegrees(targetYaw - currentYaw);
        float pitchDiff = targetPitch - currentPitch;


        AimMode mode = (AimMode) aimMode.getValue();
        
        float hSpeed, vSpeed;
        switch (mode) {
            case Regular -> {

                hSpeed = 0.12f;
                vSpeed = 0.08f;
            }
            case Adaptive -> {
                hSpeed = 0.08f;
                vSpeed = 0.06f;
            }
            case Linear -> {
                hSpeed = 0.15f;
                vSpeed = 0.10f;
            }
            case Blatant -> {
                hSpeed = 0.20f;
                vSpeed = 0.15f;
            }
            default -> {
                hSpeed = 0.12f;
                vSpeed = 0.08f;
            }
        }


        float yawStep = Math.abs(yawDiff) * hSpeed;
        float pitchStep = Math.abs(pitchDiff) * vSpeed;


        if (rotationJitter.getValue() > 0) {
            targetYaw += (float) ((Math.random() - 0.5) * rotationJitter.getValue());
            targetPitch += (float) ((Math.random() - 0.5) * rotationJitter.getValue() * 0.5);
        }


        float newYaw = currentYaw + Math.copySign(Math.min(Math.abs(yawDiff), Math.max(0.01f, yawStep)), yawDiff);
        float newPitch = currentPitch + Math.copySign(Math.min(Math.abs(pitchDiff), Math.max(0.01f, pitchStep)), pitchDiff);


        RotationUtils.Rotation quantized = RotationUtils.applySensitivityPatch(
            new RotationUtils.Rotation(newYaw, newPitch), 
            new RotationUtils.Rotation(currentYaw, currentPitch));

        if (!silentAim.getValue()) {
            mc.player.setYaw((float) quantized.getYaw());
            mc.player.setPitch((float) quantized.getPitch());
        } else {

            try {

                mc.player.setBodyYaw((float) quantized.getYaw());
                mc.player.setHeadYaw((float) quantized.getYaw());


                if (mc.options != null && mc.options.getPerspective() != null && !mc.options.getPerspective().isFirstPerson()) {
                    mc.player.setYaw((float) quantized.getYaw());
                    mc.player.setPitch((float) quantized.getPitch());
                }
            } catch (Throwable ignored) {}
        }
    }

    private Vec3d getMultiPointTargetPosition(Entity entity) {
        if (entity == null) return null;
        final var box = entity.getBoundingBox();
        final double minY = box.minY;
        final double maxY = box.maxY;
        final double height = maxY - minY;
        final double centerX = (box.minX + box.maxX) * 0.5;
        final double centerZ = (box.minZ + box.maxZ) * 0.5;
        

        double multiPointValue = multiPoint.getValue();
        if (multiPointValue > 0) {

            double offsetX = (Math.random() - 0.5) * (box.maxX - box.minX) * multiPointValue;
            double offsetY = Math.random() * height * multiPointValue;
            double offsetZ = (Math.random() - 0.5) * (box.maxZ - box.minZ) * multiPointValue;
            
            return new Vec3d(centerX + offsetX, minY + offsetY, centerZ + offsetZ);
        }
        
        return new Vec3d(centerX, minY + height * 0.5, centerZ);
    }

    private boolean isValidTarget(PlayerEntity player) {
        if (player == mc.player) return false;
        if (player.isDead() || player.isInCreativeMode() || player.isSpectator()) return false;
        
        taz.womp.module.modules.combat.AntiBot antiBot = (taz.womp.module.modules.combat.AntiBot) 
            Womp.INSTANCE.getModuleManager().getModuleByClass(taz.womp.module.modules.combat.AntiBot.class);
        if (antiBot != null && antiBot.isEnabled() && taz.womp.module.modules.combat.AntiBot.isABot(player)) {
            return false;
        }
        
        if (checkShield.getValue() && player.isBlocking()) return false;
        return true;
    }

    private boolean isValidTarget(LivingEntity entity) {
        if (entity instanceof PlayerEntity player) {
            return isValidTarget(player);
        }
        return true;
    }

    private float wrapDegrees(float degrees) {
        degrees = degrees % 360.0f;
        if (degrees >= 180.0f) degrees -= 360.0f;
        if (degrees < -180.0f) degrees += 360.0f;
        return degrees;
    }

    public static <T> List<T> toList(Iterable<T> it) {
        return it == null ? List.of() : new ArrayList<>() {{ it.forEach(this::add); }};
    }
}


