package taz.womp.utils;

import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.RaycastContext;
import taz.womp.module.modules.combat.AimAssist.HitboxType;

public class RotationUtils {
    private static final MinecraftClient mc = MinecraftClient.getInstance();

    private static Rotation lastPlayerRotation = null;

    public static class Rotation {
        private final double yaw;
        private final double pitch;

        public Rotation(double yaw, double pitch) {
            this.yaw = yaw;
            this.pitch = pitch;
        }

        public double getYaw() {
            return yaw;
        }

        public double getPitch() {
            return pitch;
        }
    }

    public static Rotation getDirection(Entity target, boolean adaptive, double range, HitboxType hitboxType) {
        if (mc.player == null || target == null) return null;


        Vec3d targetPos = target.getPos();
        double targetX = targetPos.x;
        double targetY = targetPos.y;
        double targetZ = targetPos.z;

        if (adaptive && target instanceof LivingEntity) {
            Box box = target.getBoundingBox();
            double height = box.maxY - box.minY;
            
            switch (hitboxType) {
                case Head:
                    targetY = box.minY + height * 0.9;
                    break;
                case Chest:
                    targetY = box.minY + height * 0.5;
                    break;
                case Legs:
                    targetY = box.minY + height * 0.2;
                    break;
            }
        }


        double deltaX = targetX - mc.player.getX();
        double deltaY = targetY - (mc.player.getY() + mc.player.getEyeHeight(mc.player.getPose()));
        double deltaZ = targetZ - mc.player.getZ();


        double yaw = MathHelper.wrapDegrees(Math.toDegrees(Math.atan2(deltaZ, deltaX)) - 90);


        double horizontalDistance = Math.sqrt(deltaX * deltaX + deltaZ * deltaZ);
        double pitch = MathHelper.wrapDegrees(Math.toDegrees(Math.atan2(deltaY, horizontalDistance)));


        if (isTargetVisible(new Vec3d(targetX, targetY, targetZ))) {
            return new Rotation(yaw, pitch);
        }

        if (adaptive && target instanceof LivingEntity) {
            Box box = target.getBoundingBox();
            double height = box.maxY - box.minY;
            

            targetY = box.minY + height * 0.9;
            deltaY = targetY - (mc.player.getY() + mc.player.getEyeHeight(mc.player.getPose()));
            horizontalDistance = Math.sqrt(deltaX * deltaX + deltaZ * deltaZ);
            pitch = MathHelper.wrapDegrees(Math.toDegrees(Math.atan2(deltaY, horizontalDistance)));
            
            if (isTargetVisible(new Vec3d(targetX, targetY, targetZ))) {
                return new Rotation(yaw, pitch);
            }


            targetY = box.minY + height * 0.5;
            deltaY = targetY - (mc.player.getY() + mc.player.getEyeHeight(mc.player.getPose()));
            horizontalDistance = Math.sqrt(deltaX * deltaX + deltaZ * deltaZ);
            pitch = MathHelper.wrapDegrees(Math.toDegrees(Math.atan2(deltaY, horizontalDistance)));
            
            if (isTargetVisible(new Vec3d(targetX, targetY, targetZ))) {
                return new Rotation(yaw, pitch);
            }


            targetY = box.minY + height * 0.2;
            deltaY = targetY - (mc.player.getY() + mc.player.getEyeHeight(mc.player.getPose()));
            horizontalDistance = Math.sqrt(deltaX * deltaX + deltaZ * deltaZ);
            pitch = MathHelper.wrapDegrees(Math.toDegrees(Math.atan2(deltaY, horizontalDistance)));
            
            if (isTargetVisible(new Vec3d(targetX, targetY, targetZ))) {
                return new Rotation(yaw, pitch);
            }
        }

        return null;
    }

    public static Rotation getNeededRotations(Vec3d vec3d) {
        return getNeededRotations((float)vec3d.x, (float)vec3d.y, (float)vec3d.z);
    }

    public static Rotation getNeededRotations(float x, float y, float z) {
        Vec3d eyePos = mc.player.getEyePos();
        double deltaX = x - eyePos.x;
        double deltaY = y - eyePos.y;
        double deltaZ = z - eyePos.z;

        double horizontalDistance = Math.sqrt(deltaX * deltaX + deltaZ * deltaZ);
        float yaw = (float) Math.toDegrees(Math.atan2(deltaZ, deltaX)) - 90;
        float pitch = (float) -Math.toDegrees(Math.atan2(deltaY, horizontalDistance));

        return new Rotation(
            mc.player.getYaw() + wrap(yaw - mc.player.getYaw()),
            mc.player.getPitch() + wrap(pitch - mc.player.getPitch())
        );
    }


    public static Rotation applySensitivityPatch(Rotation targetRotation, Rotation currentRotation) {
        float mouseSensitivity = (float) (mc.options.getMouseSensitivity().getValue() * (1 + Math.random() / 10000000) * 0.6F + 0.2F);
        float multiplier = (float)(mouseSensitivity * mouseSensitivity * mouseSensitivity * 8.0F * 0.15D);
        
        float deltaYaw = wrap((float)((float)targetRotation.getYaw() - (float)currentRotation.getYaw()));
        float deltaPitch = (float)((float)targetRotation.getPitch() - (float)currentRotation.getPitch());

        float patchedYaw = (float)currentRotation.getYaw() + (float) (Math.round(deltaYaw / multiplier) * multiplier);
        float patchedPitch = (float)currentRotation.getPitch() + (float) (Math.round(deltaPitch / multiplier) * multiplier);

        patchedPitch = MathHelper.clamp(patchedPitch, -90, 90);

        return new Rotation(patchedYaw, patchedPitch);
    }

    public static boolean setRotation(Rotation targetRotation, float smoothing, float random, float fov) {
        return setRotation(targetRotation, smoothing, smoothing, random, random, fov);
    }

    public static boolean setRotation(Rotation targetRotation, float horizontalSmoothing, float verticalSmoothing, float horizontalRandom, float verticalRandom, float fov) {
        Rotation currentSimulatedRotation = (lastPlayerRotation != null) ? lastPlayerRotation : new Rotation(mc.player.getYaw(), mc.player.getPitch());

        float deltaYaw = wrap((float)((float)targetRotation.getYaw() - (float)currentSimulatedRotation.getYaw()));
        float deltaPitch = (float)((float)targetRotation.getPitch() - (float)currentSimulatedRotation.getPitch());

        float yaw = (float)targetRotation.getYaw();
        float pitch = (float)targetRotation.getPitch();
        float lastYaw = (float)currentSimulatedRotation.getYaw();
        float lastPitch = (float)currentSimulatedRotation.getPitch();

        if (horizontalSmoothing != 0 || verticalSmoothing != 0) {
            float horizontalSpeed = horizontalSmoothing * 10;
            float verticalSpeed = verticalSmoothing * 10;

            float distance = (float)Math.sqrt(deltaYaw * deltaYaw + deltaPitch * deltaPitch);
            float distributionYaw = (float)Math.abs(deltaYaw / distance);
            float distributionPitch = (float)Math.abs(deltaPitch / distance);

            float maxYaw = horizontalSpeed * distributionYaw;
            float maxPitch = verticalSpeed * distributionPitch;

            float moveYaw = Math.max(Math.min(deltaYaw, maxYaw), -maxYaw);
            float movePitch = Math.max(Math.min(deltaPitch, maxPitch), -maxPitch);

            yaw = lastYaw + moveYaw;
            pitch = lastPitch + movePitch;
        }

        int iterations = 1;

        for (int i = 1; i <= iterations; i++) {


            Rotation rotations = new Rotation(yaw, pitch);
            Rotation fixedRotations = applySensitivityPatch(rotations, currentSimulatedRotation);

            yaw = (float)fixedRotations.getYaw();
            pitch = Math.max(-90, Math.min(90, (float)fixedRotations.getPitch()));
        }

        setRotation(yaw, pitch);
        lastPlayerRotation = new Rotation(yaw, pitch);

        return isInRange(new Rotation(mc.player.getYaw(), mc.player.getPitch()), targetRotation, 5);
    }

    public static boolean setPitch(Rotation targetRotation, float smoothing, float random, float fov) {
        Rotation currentSimulatedRotation = (lastPlayerRotation != null) ? lastPlayerRotation : new Rotation(mc.player.getYaw(), mc.player.getPitch());

        float deltaPitch = (float)((float)targetRotation.getPitch() - (float)currentSimulatedRotation.getPitch());
        float pitch = (float)targetRotation.getPitch();
        float lastPitch = (float)currentSimulatedRotation.getPitch();

        if (smoothing != 0) {
            float rotationSpeed = smoothing * 10;
            float movePitch = Math.max(Math.min(deltaPitch, rotationSpeed), -rotationSpeed);
            pitch = lastPitch + movePitch;
        }

        int iterations = 1;

        for (int i = 1; i <= iterations; i++) {


            Rotation rotations = new Rotation(mc.player.getYaw(), pitch);
            Rotation fixedRotations = applySensitivityPatch(rotations, currentSimulatedRotation);
            pitch = Math.max(-90, Math.min(90, (float)fixedRotations.getPitch()));
        }

        setRotation(mc.player.getYaw(), pitch);
        lastPlayerRotation = new Rotation(mc.player.getYaw(), pitch);

        return Math.abs(mc.player.getPitch() - (float)targetRotation.getPitch()) < 5;
    }

    public static boolean setYaw(Rotation targetRotation, float smoothing, float random, float fov) {
        Rotation currentSimulatedRotation = (lastPlayerRotation != null) ? lastPlayerRotation : new Rotation(mc.player.getYaw(), mc.player.getPitch());

        float deltaYaw = wrap((float)((float)targetRotation.getYaw() - (float)currentSimulatedRotation.getYaw()));
        float yaw = (float)targetRotation.getYaw();
        float lastYaw = (float)currentSimulatedRotation.getYaw();

        if (smoothing != 0) {
            float rotationSpeed = smoothing * 10;
            float moveYaw = Math.max(Math.min(deltaYaw, rotationSpeed), -rotationSpeed);
            yaw = lastYaw + moveYaw;
        }

        int iterations = 1;

        for (int i = 1; i <= iterations; i++) {


            Rotation rotations = new Rotation(yaw, mc.player.getPitch());
            Rotation fixedRotations = applySensitivityPatch(rotations, currentSimulatedRotation);
            yaw = (float)fixedRotations.getYaw();
        }

        setRotation(yaw, mc.player.getPitch());
        lastPlayerRotation = new Rotation(yaw, mc.player.getPitch());

        return Math.abs(mc.player.getYaw() - (float)targetRotation.getYaw()) < 5;
    }

    public static void setRotation(float yaw, float pitch) {
        mc.player.setYaw(yaw);
        mc.player.setPitch(pitch);
    }

    public static boolean isInRange(Rotation rotation1, Rotation rotation2, float range) {
        return Math.abs((float)rotation2.getYaw() - (float)rotation1.getYaw()) < range && 
               Math.abs((float)rotation2.getPitch() - (float)rotation1.getPitch()) < range;
    }

    public static float wrap(float value) {
        value = value % 360;
        if (value >= 180) {
            value -= 360;
        }
        if (value < -180) {
            value += 360;
        }
        return value;
    }

    public static float interpolate(float start, float end, float factor) {
        return start + (end - start) * factor;
    }

    public static float randomInRange(float min, float max) {
        return min + (float) (Math.random() * (max - min));
    }

    public static Vec3d getPlayerLookVec(Rotation rotation) {
        float yaw = (float) Math.toRadians(-(float)rotation.getYaw() - 180);
        float pitch = (float) Math.toRadians(-(float)rotation.getPitch());
        float cosPitch = -MathHelper.cos(pitch);
        return new Vec3d(
            MathHelper.sin(yaw) * cosPitch,
            -MathHelper.sin(pitch),
            MathHelper.cos(yaw) * cosPitch
        );
    }

    public static boolean isTargetVisible(Vec3d targetPos) {
        if (mc.player == null) return false;
        Vec3d eyePos = mc.player.getEyePos();
        return mc.world.raycast(new RaycastContext(
            eyePos, 
            targetPos,
            RaycastContext.ShapeType.COLLIDER,
            RaycastContext.FluidHandling.NONE,
            mc.player
        )).getType() == net.minecraft.util.hit.BlockHitResult.Type.MISS;
    }
} 