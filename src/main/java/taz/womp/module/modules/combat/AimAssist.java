package taz.womp.module.modules.combat;

import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.Entity;
import net.minecraft.entity.decoration.EndCrystalEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.SwordItem;
import net.minecraft.util.math.Vec3d;
import org.lwjgl.glfw.GLFW;
import taz.womp.Womp;
import taz.womp.event.EventListener;
import taz.womp.event.events.TickEvent;
import taz.womp.module.Category;
import taz.womp.module.Module;
import taz.womp.module.setting.BooleanSetting;
import taz.womp.module.setting.MinMaxSetting;
import taz.womp.module.setting.ModeSetting;
import taz.womp.module.setting.NumberSetting;
import taz.womp.utils.EncryptedString;
import taz.womp.utils.RotationUtils;
import taz.womp.utils.TimerUtils;

public class AimAssist extends Module {
    private final MinecraftClient mc = MinecraftClient.getInstance();
    private final TimerUtils timer = new TimerUtils();
    private final TimerUtils targetAcquireTimer = new TimerUtils();
    private final TimerUtils resetSpeed = new TimerUtils();
    private Entity target = null;
    private float targetAcquireProgress = 0.0f;
    private boolean isTargetNew = false;

	private float sampledHSpeed = 0f;
	private float sampledVSpeed = 0f;
	private float sampledJitter = 0f;

    public enum HitboxType {
        Head, Chest, Legs
    }

    private final BooleanSetting targetPlayers = new BooleanSetting(EncryptedString.of("Target Players"), true);
    private final BooleanSetting targetCrystals = new BooleanSetting(EncryptedString.of("Target Crystals"), true);
    private final BooleanSetting seeOnly = new BooleanSetting(EncryptedString.of("See Only"), true);

    private final BooleanSetting enableVertical = new BooleanSetting(EncryptedString.of("Enable Vertical"), false);
    private final NumberSetting fov = new NumberSetting(EncryptedString.of("FOV"), 
            0.1, 180, 50, 0.1);
    private final NumberSetting range = new NumberSetting(EncryptedString.of("Range"), 
            0.1, 10, 5, 0.1);

    private final ModeSetting<HitboxType> hitbox = new ModeSetting<>(EncryptedString.of("Hitbox"),
         HitboxType.Head, HitboxType.values());

    private final BooleanSetting weaponOnly = new BooleanSetting(EncryptedString.of("Weapon Only"), false);
    private final BooleanSetting stickyAim = new BooleanSetting(EncryptedString.of("Sticky Targeting"), true);
    private final BooleanSetting onLeftClick = new BooleanSetting(EncryptedString.of("On Left Click"), false);
    private final BooleanSetting stopAtTargetVertical = new BooleanSetting(EncryptedString.of("Stop at Target Vert"), true);
    private final BooleanSetting stopAtTargetHorizontal = new BooleanSetting(EncryptedString.of("Stop at Target Horiz"), false);

    private final NumberSetting smoothing = new NumberSetting(EncryptedString.of("Smoothing"), 
            0.0, 1.0, 0.5, 0.05);
    private final MinMaxSetting horizontalSpeed = new MinMaxSetting(EncryptedString.of("Horizontal Speed"), 1, 10, 1, 5, 7);
    private final MinMaxSetting verticalSpeed = new MinMaxSetting(EncryptedString.of("Vertical Speed"), 1, 10, 1, 3, 5);
    private final NumberSetting acquireTime = new NumberSetting(EncryptedString.of("Acquire Time"), 
            0, 1000, 200, 10);

    private final MinMaxSetting jitter = new MinMaxSetting(EncryptedString.of("Jitter"), 0.0, 1.0, 0.01, 0.05, 0.15);

    
    private final float MAX_SPEED_VALUE = 4.5f;
    private final float MIN_SPEED_VALUE = 0.02f;

    public AimAssist() {
        super(EncryptedString.of("Aim Assist"),
                EncryptedString.of("Automatically aims at players"),
                -1,
                Category.COMBAT);

        
        addSettings(targetPlayers, targetCrystals, seeOnly, enableVertical, fov, range, hitbox, weaponOnly,
                    stickyAim, onLeftClick, stopAtTargetVertical, stopAtTargetHorizontal,
                    smoothing, horizontalSpeed, verticalSpeed, acquireTime, jitter);
        
        stopAtTargetVertical.setVisibility(() -> enableVertical.getValue());
        verticalSpeed.setVisibility(() -> enableVertical.getValue());
    }

    @Override
    public void onEnable() {
        super.onEnable();
        target = null;
        targetAcquireProgress = 0.0f;
        isTargetNew = false;
        timer.reset();
        resetSpeed.reset();
        targetAcquireTimer.reset();

		
		sampledHSpeed = convertSpeed((float) horizontalSpeed.getRandomDoubleInRange());
		sampledVSpeed = enableVertical.getValue() ? convertSpeed((float) verticalSpeed.getRandomDoubleInRange()) : 0f;
		sampledJitter = (float) jitter.getRandomDoubleInRange();
    }

    @Override
    public void onDisable() {
        super.onDisable();
        target = null;
        targetAcquireProgress = 0.0f;
        isTargetNew = false;
    }

    private float convertSpeed(float speed) {
        
        float t = Math.max(0f, Math.min(1f, (speed - 1f) / 9f));
        float curved = (float) Math.pow(t, 1.4f);
        return MIN_SPEED_VALUE + (MAX_SPEED_VALUE - MIN_SPEED_VALUE) * curved;
    }

    @EventListener
    public void onTick(TickEvent event) {
        if (!isPlayerUsable()) return;

        
        if (weaponOnly.getValue() && !(mc.player.getMainHandStack().getItem() instanceof SwordItem)) {
            return;
        }

        
        if (onLeftClick.getValue() && GLFW.glfwGetMouseButton(mc.getWindow().getHandle(), GLFW.GLFW_MOUSE_BUTTON_LEFT) != GLFW.GLFW_PRESS) {
            return;
        }

        
        if (!stickyAim.getValue() || target == null) {
            Entity newTarget = findTarget();
            if (newTarget != null && newTarget != target) {
                target = newTarget;
                isTargetNew = true;
                targetAcquireProgress = 0.0f;
                targetAcquireTimer.reset();
				
				sampledHSpeed = convertSpeed((float) horizontalSpeed.getRandomDoubleInRange());
				sampledVSpeed = enableVertical.getValue() ? convertSpeed((float) verticalSpeed.getRandomDoubleInRange()) : 0f;
				sampledJitter = (float) jitter.getRandomDoubleInRange();
            } else if (newTarget == null) {
                target = null;
                isTargetNew = false;
                targetAcquireProgress = 0.0f;
            }
        } else {
            
            if (target != null) {
                Vec3d targetPos = target.getPos();
                double distance = mc.player.squaredDistanceTo(targetPos.x, targetPos.y, targetPos.z);
                if (distance > range.getValue() * range.getValue()) {
                    target = null;
                    isTargetNew = false;
                    targetAcquireProgress = 0.0f;
                    return;
                }
            }
        }

        if (target == null) return;

		
		if (resetSpeed.hasReached(250L)) {
			resetSpeed.reset();
			sampledHSpeed = convertSpeed((float) horizontalSpeed.getRandomDoubleInRange());
			sampledVSpeed = enableVertical.getValue() ? convertSpeed((float) verticalSpeed.getRandomDoubleInRange()) : 0f;
			sampledJitter = (float) jitter.getRandomDoubleInRange();
		}

        
        Vec3d targetPos = getTargetPosition(target);
        if (targetPos == null) return;

        
        if (seeOnly.getValue() && !stickyAim.getValue() && !RotationUtils.isTargetVisible(targetPos)) {
            return;
        }

        
        RotationUtils.Rotation needed = RotationUtils.getNeededRotations(targetPos);
        if (needed == null) return;

        float currentYaw = mc.player.getYaw();
        float currentPitch = mc.player.getPitch();
        float targetYaw = (float) needed.getYaw();
        float targetPitch = (float) needed.getPitch();

        
        float yawDiff = wrapDegrees(targetYaw - currentYaw);
        float pitchDiff = targetPitch - currentPitch;

        if (isTargetNew && acquireTime.getValue() > 0) {
            long acquireTimeMs = (long) acquireTime.getValue();
            if (targetAcquireTimer.hasReached(acquireTimeMs)) {
                targetAcquireProgress = 1.0f;
                isTargetNew = false;
            } else {
                targetAcquireProgress = (float) targetAcquireTimer.getCurrentMS() / acquireTimeMs;
                if (targetAcquireProgress > 1.0f) targetAcquireProgress = 1.0f;
            }
        } else {
            targetAcquireProgress = 1.0f;
            isTargetNew = false;
        }

        
		float hSpeed = sampledHSpeed;
		float vSpeed = enableVertical.getValue() ? sampledVSpeed : 0f;

        
        float smoothingValue = (float) smoothing.getValue();

        
        float baseYawStep = Math.min(Math.abs(yawDiff), hSpeed);
        float basePitchStep = Math.min(Math.abs(pitchDiff), vSpeed);

        
        float easeRadius = 8.0f; 
        float minEaseFactor = 1.0f - 0.6f * smoothingValue; 
        minEaseFactor = Math.max(0.4f, minEaseFactor);

        float yawEase = 1.0f;
        float absYaw = Math.abs(yawDiff);
        if (absYaw < easeRadius) {
            float k = 1.0f - (absYaw / easeRadius); 
            yawEase = 1.0f - (1.0f - minEaseFactor) * k;
        }

        float pitchEase = 1.0f;
        float absPitch = Math.abs(pitchDiff);
        if (absPitch < easeRadius) {
            float k = 1.0f - (absPitch / easeRadius);
            pitchEase = 1.0f - (1.0f - minEaseFactor) * k;
        }

        float yawStep = baseYawStep * yawEase;
        float pitchStep = basePitchStep * pitchEase;

        
        float snapThresholdYaw = 0.25f;
        float snapThresholdPitch = 0.25f;

        
		float jitterRangeYaw = sampledJitter;
        float jitterRangePitch = jitterRangeYaw; 
        float jitterYaw = jitterRangeYaw > 0 ? (float) ((Math.random() - 0.5) * 2.0 * jitterRangeYaw) : 0f;
        float jitterPitch = jitterRangePitch > 0 ? (float) ((Math.random() - 0.5) * 2.0 * jitterRangePitch) : 0f;

        
        float ms = (float) (mc.options.getMouseSensitivity().getValue() * 0.6F + 0.2F);
        float quantMultiplier = (float) (ms * ms * ms * 8.0F * 0.15D);
        float minQuant = Math.max(0.001f, quantMultiplier * 0.5f);

        float yawMove = Math.copySign(Math.min(Math.abs(yawDiff), Math.max(minQuant, yawStep)), yawDiff);
        float pitchMove = Math.copySign(Math.min(Math.abs(pitchDiff), Math.max(minQuant, pitchStep)), pitchDiff);

        float newYaw = currentYaw + yawMove + jitterYaw;
        float newPitch = currentPitch + pitchMove + jitterPitch;

        
        if (stopAtTargetHorizontal.getValue() && Math.abs(yawDiff) < snapThresholdYaw) newYaw = targetYaw;
        if (enableVertical.getValue() && stopAtTargetVertical.getValue() && Math.abs(pitchDiff) < snapThresholdPitch) newPitch = targetPitch;

        RotationUtils.Rotation currentRotation = new RotationUtils.Rotation(currentYaw, currentPitch);
        RotationUtils.Rotation targetRotation = new RotationUtils.Rotation(newYaw, newPitch);
        RotationUtils.Rotation quantizedRotation = RotationUtils.applySensitivityPatch(targetRotation, currentRotation);

        
        if (enableVertical.getValue()) {
            mc.player.setYaw((float) quantizedRotation.getYaw());
            mc.player.setPitch((float) quantizedRotation.getPitch());
        } else {
            mc.player.setYaw((float) quantizedRotation.getYaw());
        }
    }

    
    private float wrapDegrees(float degrees) {
        degrees = degrees % 360.0f;
        if (degrees >= 180.0f) degrees -= 360.0f;
        if (degrees < -180.0f) degrees += 360.0f;
        return degrees;
    }

    private Vec3d getTargetPosition(Entity entity) {
        if (entity == null) return null;

        
        final var box = entity.getBoundingBox();
        final double minY = box.minY;
        final double maxY = box.maxY;
        final double height = maxY - minY;
        final double centerX = (box.minX + box.maxX) * 0.5;
        final double centerZ = (box.minZ + box.maxZ) * 0.5;

        HitboxType hitboxType = (HitboxType) hitbox.getValue();
        switch (hitboxType) {
            case Head:
                
                return new Vec3d(centerX, maxY - Math.max(0.08, height * 0.1), centerZ);
            case Chest:
                return new Vec3d(centerX, minY + height * 0.5, centerZ);
            case Legs:
                return new Vec3d(centerX, minY + height * 0.2, centerZ);
            default:
                
                return entity.getPos();
        }
    }

    private Entity findTarget() {
        Entity target = null;
        double minDistance = range.getValue();

        for (Entity entity : mc.world.getEntities()) {
            if (!isValidTarget(entity)) continue;

            Vec3d targetPos = getTargetPosition(entity);
            if (targetPos == null) continue;

            double distance = mc.player.squaredDistanceTo(targetPos.x, targetPos.y, targetPos.z);
            if (distance > minDistance * minDistance) continue;

            
            if (seeOnly.getValue() && !RotationUtils.isTargetVisible(targetPos)) continue;

            
            RotationUtils.Rotation rotation = RotationUtils.getNeededRotations(targetPos);
            if (rotation == null) continue;

            
            if (!RotationUtils.isInRange(new RotationUtils.Rotation(mc.player.getYaw(), mc.player.getPitch()), rotation, (float)fov.getValue())) continue;

            target = entity;
            minDistance = Math.sqrt(distance);
        }

        return target;
    }

    private boolean isValidTarget(Entity entity) {
        if (!(entity instanceof PlayerEntity) && !(entity instanceof EndCrystalEntity)) {
            return false;
        }

        if (entity == mc.player) {
            return false;
        }

        if (entity instanceof PlayerEntity) {
            if (!targetPlayers.getValue()) return false;
            
            AntiBot antiBot = (AntiBot) Womp.INSTANCE.getModuleManager().getModuleByClass(AntiBot.class);
            if (antiBot != null && antiBot.isEnabled() && AntiBot.isABot((PlayerEntity) entity)) return false;
        } else if (entity instanceof EndCrystalEntity) {
            if (!targetCrystals.getValue()) return false;
        }

        return true;
    }
} 