package taz.womp.module.modules.combat;

import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.network.packet.Packet;
import net.minecraft.network.packet.c2s.play.PlayerInteractEntityC2SPacket;
import net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket;
import net.minecraft.network.packet.s2c.play.EntityVelocityUpdateS2CPacket;
import net.minecraft.network.packet.s2c.play.HealthUpdateS2CPacket;
import net.minecraft.network.packet.s2c.play.PlayerPositionLookS2CPacket;
import net.minecraft.util.math.Vec3d;
import taz.womp.event.EventListener;
import taz.womp.event.events.PacketReceiveEvent;
import taz.womp.event.events.PacketSendEvent;
import taz.womp.event.events.Render3DEvent;
import taz.womp.event.events.TickEvent;
import taz.womp.module.Category;
import taz.womp.module.Module;
import taz.womp.module.setting.BooleanSetting;
import taz.womp.module.setting.NumberSetting;
import taz.womp.utils.EncryptedString;
import taz.womp.utils.RenderUtils;
import taz.womp.utils.TimerUtils;

import java.awt.*;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.ConcurrentLinkedQueue;

public class LagRange extends Module {
    private final MinecraftClient mc = MinecraftClient.getInstance();


    private final NumberSetting fov = new NumberSetting(EncryptedString.of("FOV"), 0.0, 360.0, 140.0, 1.0);
    private final NumberSetting delay = new NumberSetting(EncryptedString.of("Delay"), 0.0, 2500.0, 250.0, 1.0);
    private final NumberSetting distance = new NumberSetting(EncryptedString.of("Distance"), 0.0, 6.0, 3.0, 0.1);
    private final NumberSetting leniency = new NumberSetting(EncryptedString.of("Extra Leniency"), 0.0, 1.0, 0.1, 0.01);
    private final BooleanSetting realPos = new BooleanSetting(EncryptedString.of("Real Position"), false);
    private final BooleanSetting onlyWhileLagging = new BooleanSetting(EncryptedString.of("Only While Lagging"), false);

    public final ConcurrentLinkedQueue<DelayedPacket> packetQueue = new ConcurrentLinkedQueue<>();
    private final TimerUtils waitTimer = new TimerUtils();
    private Vec3d currentPosition = new Vec3d(0.0, 0.0, 0.0);
    private Vec3d oldPlayerPosition = new Vec3d(0.0, 0.0, 0.0);

    public LagRange() {
        super(EncryptedString.of("LagRange"), EncryptedString.of("Use lag to maintain distance from enemies"), -1, Category.COMBAT);
        addSettings(fov, delay, distance, leniency, realPos, onlyWhileLagging);
    }

    @Override
    public void onDisable() {
        super.onDisable();
        handlePackets(false, false);
    }

    @EventListener
    public void onTick(TickEvent e) {
        if (!isPlayerUsable() || mc.currentScreen != null) return;

        PlayerEntity target = (PlayerEntity) getClosestPlayerWithinRange((float) (distance.getValue() + 2.0));
        if (target == null) {
            handlePackets(false, false);
            waitTimer.reset();
            return;
        }
        if (!waitTimer.hasReached(250)) {
            handlePackets(false, false);
            return;
        }

        if (mc.player.distanceTo(target) > distance.getValue()) {
            handlePackets(false, true);
        } else {
            handlePackets(true, false);
        }

        if (currentPosition.distanceTo(target.getPos()) + leniency.getValue() < mc.player.distanceTo(target)) {
            handlePackets(false, false);
        }
    }

    @EventListener
    public void onRender3D(Render3DEvent event) {
        if (!isPlayerUsable()) return;
        if (!realPos.getValue()) return;
        if (currentPosition.y == 0.0) return;

        Color color = packetQueue.isEmpty() ? new Color(255, 0, 0) : new Color(255, 255, 255);
        oldPlayerPosition = oldPlayerPosition.lerp(currentPosition, 0.1);
        if (onlyWhileLagging.getValue() && color.equals(new Color(255, 0, 0))) return;

        PlayerEntity target = (PlayerEntity) getClosestPlayerWithinRange((float) (distance.getValue() + 2.0));
        if (target == null) return;


        final net.minecraft.client.render.Camera camera = taz.womp.utils.RenderUtils.getCamera();
        if (camera != null) {
            final net.minecraft.client.util.math.MatrixStack matrices = event.matrixStack;
            matrices.push();
            final net.minecraft.util.math.Vec3d camPos = taz.womp.utils.RenderUtils.getCameraPos();
            matrices.multiply(net.minecraft.util.math.RotationAxis.POSITIVE_X.rotationDegrees(camera.getPitch()));
            matrices.multiply(net.minecraft.util.math.RotationAxis.POSITIVE_Y.rotationDegrees(camera.getYaw() + 180.0f));
            matrices.translate(-camPos.x, -camPos.y, -camPos.z);

            RenderUtils.renderFilledBox(
                    event.matrixStack,
                    (float) oldPlayerPosition.x - mc.player.getWidth() / 2.0f,
                    (float) oldPlayerPosition.y,
                    (float) oldPlayerPosition.z - mc.player.getWidth() / 2.0f,
                    (float) oldPlayerPosition.x + mc.player.getWidth() / 2.0f,
                    (float) oldPlayerPosition.y + mc.player.getHeight(),
                    (float) oldPlayerPosition.z + mc.player.getWidth() / 2.0f,
                    color
            );

            matrices.pop();
        }
    }

    @EventListener
    public void onPacketReceive(PacketReceiveEvent event) {
        if (!isPlayerUsable()) return;
        Packet<?> packet = event.packet;

        if (packet instanceof PlayerPositionLookS2CPacket
                || packet instanceof HealthUpdateS2CPacket health && mc.player != null && health.getHealth() != mc.player.getHealth()
                || packet instanceof EntityVelocityUpdateS2CPacket vel && mc.player != null && vel.getEntityId() == mc.player.getId()) {
            handlePackets(false, false);
            waitTimer.reset();
        }
    }

    @EventListener
    public void onPacketSend(PacketSendEvent event) {
        if (!isPlayerUsable() || mc.currentScreen != null) return;
        if (mc.player.age < 10) {
            handlePackets(false, false);
            return;
        }

        Packet<?> packet = event.packet;

        PlayerEntity target = (PlayerEntity) getClosestPlayerWithinRange((float) (distance.getValue() + 2.0));
        if (target == null) {
            handlePackets(false, false);
            waitTimer.reset();
            return;
        }
        if (!waitTimer.hasReached(250)) {
            handlePackets(false, false);
            return;
        }
        if (packet instanceof PlayerInteractEntityC2SPacket) {
            handlePackets(false, false);
            waitTimer.reset();
            return;
        }

        packetQueue.offer(new DelayedPacket(packet, System.currentTimeMillis()));
        event.cancel();
    }

    private void handlePackets(boolean useDelay, boolean findCloserPosition) {
        if (mc.player == null || mc.world == null || packetQueue.isEmpty()) return;
        PlayerEntity target = (PlayerEntity) getClosestPlayerWithinRange((float) (distance.getValue() + 2.0));

        List<DelayedPacket> toRemove = new ArrayList<>();
        for (DelayedPacket dp : packetQueue) {
            boolean timePassed = System.currentTimeMillis() - dp.receiveTime >= (useDelay ? (long) delay.getValue() : -1000L);
            if (timePassed) {
                try {
                    mc.getNetworkHandler().getConnection().send(dp.packet, null);
                } catch (Throwable ignored) {}
                toRemove.add(dp);
                if (dp.packet instanceof PlayerMoveC2SPacket move) {
                    Vec3d pos = mc.player.getPos();
                    if (useDelay) {
                        currentPosition = new Vec3d(move.getX(pos.x), move.getY(pos.y), move.getZ(pos.z));
                    } else {
                        currentPosition = mc.player.getLerpedPos(MinecraftClient.getInstance().getRenderTickCounter().getTickDelta(false));
                    }
                    if (target != null && findCloserPosition && !useDelay) {
                        if (target.squaredDistanceTo(move.getX(pos.x), move.getY(pos.y), move.getZ(pos.z)) <= Math.pow(distance.getValue(), 2)) {
                            break;
                        }
                    }
                }
            }
        }
        packetQueue.removeAll(toRemove);
    }

    private Entity getClosestPlayerWithinRange(float range) {
        if (mc.player == null || mc.world == null) return null;
        Vec3d eye = mc.player.getEyePos();
        Vec3d look = mc.player.getRotationVector();
        double halfFov = Math.toRadians(fov.getValue() / 2.0);
        return mc.world.getPlayers()
                .stream()
                .filter(p -> p != mc.player && mc.player.distanceTo(p) <= range)
                .filter(p -> {
                    Vec3d to = p.getPos().add(0, p.getEyeHeight(p.getPose()), 0).subtract(eye).normalize();
                    double dot = look.normalize().dotProduct(to);
                    double angle = Math.acos(dot);
                    return angle <= halfFov;
                })
                .min(Comparator.comparingDouble(mc.player::distanceTo))
                .orElse(null);
    }

    private static class DelayedPacket {
        final Packet<?> packet;
        final long receiveTime;
        DelayedPacket(Packet<?> packet, long receiveTime) {
            this.packet = packet;
            this.receiveTime = receiveTime;
        }
    }
}


