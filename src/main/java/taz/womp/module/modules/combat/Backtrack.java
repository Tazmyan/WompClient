package taz.womp.module.modules.combat;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayNetworkHandler;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.network.packet.Packet;
import net.minecraft.network.packet.s2c.play.EntityVelocityUpdateS2CPacket;
import net.minecraft.network.packet.s2c.play.PlayerPositionLookS2CPacket;
import taz.womp.event.EventListener;
import taz.womp.event.events.AttackEvent;
import taz.womp.event.events.PacketReceiveEvent;
import taz.womp.event.events.Render3DEvent;
import taz.womp.event.events.TickEvent;
import taz.womp.module.Category;
import taz.womp.module.Module;
import taz.womp.module.setting.BooleanSetting;
import taz.womp.module.setting.MinMaxSetting;
import taz.womp.module.setting.ModeSetting;
import taz.womp.module.setting.NumberSetting;
import taz.womp.utils.EncryptedString;
import taz.womp.utils.RenderUtils;
import taz.womp.utils.TimerUtils;

import java.awt.*;
import java.util.concurrent.ConcurrentLinkedQueue;

public class Backtrack extends Module {
    private enum DelayType { Dynamic, Static }
    private final MinecraftClient mc = MinecraftClient.getInstance();


    private final ModeSetting<DelayType> delayType = new ModeSetting<>(
            EncryptedString.of("Backtrack Type"),
            DelayType.Dynamic,
            DelayType.values()
    );
    private final MinMaxSetting range = new MinMaxSetting(EncryptedString.of("Range"), 0.0, 10.0, 0.1, 1.5, 6.0);
    private final MinMaxSetting latency = new MinMaxSetting(EncryptedString.of("Latency"), 0.0, 2500.0, 1.0, 100.0, 150.0);
    private final BooleanSetting flushOnVelocity = new BooleanSetting(EncryptedString.of("Flush On Velocity"), true);
    private final NumberSetting flushDelay = new NumberSetting(EncryptedString.of("Delay On Velocity)"), 0.0, 500.0, 50.0, 1.0);

    private final ConcurrentLinkedQueue<PacketData> packetQueue = new ConcurrentLinkedQueue<>();
    private final TimerUtils waitTimer = new TimerUtils();

    private Entity target;
    private double currentDelayMs = 100.0;


    private net.minecraft.util.math.Vec3d currentPosition = new net.minecraft.util.math.Vec3d(0.0, 0.0, 0.0);
    private net.minecraft.util.math.Vec3d oldRenderPosition = new net.minecraft.util.math.Vec3d(0.0, 0.0, 0.0);

    public Backtrack() {
        super(EncryptedString.of("Backtrack"), EncryptedString.of("Delay incoming packets to fake higher ping"), -1, Category.COMBAT);
        addSettings(delayType, range, latency, flushOnVelocity, flushDelay);
    }

    @Override
    public void onEnable() {
        super.onEnable();
        resetState(false);
        currentDelayMs = 100.0;
        waitTimer.reset();
    }

    @Override
    public void onDisable() {
        super.onDisable();
        resetState(true);
        oldRenderPosition = new net.minecraft.util.math.Vec3d(0.0, 0.0, 0.0);
        currentPosition = new net.minecraft.util.math.Vec3d(0.0, 0.0, 0.0);
    }

    @EventListener
    public void onPacketReceive(PacketReceiveEvent event) {
        if (!isPlayerUsable()) return;


        if (!waitTimer.hasReached((long) flushDelay.getValue())) {
            return;
        }

        Packet<?> packet = event.packet;


        if (packet instanceof PlayerPositionLookS2CPacket) {
            resetState(true);
            waitTimer.reset();
            return;
        }


        if (flushOnVelocity.getValue() && packet instanceof EntityVelocityUpdateS2CPacket velocityPacket) {
            if (mc.player != null && velocityPacket.getEntityId() == mc.player.getId()) {
                resetState(true);
                waitTimer.reset();
                return;
            }
        }


        event.cancel();
        packetQueue.add(new PacketData(packet, System.currentTimeMillis()));
    }

    @EventListener
    public void onTick(TickEvent event) {
        if (!isPlayerUsable()) {
            resetState(false);
            return;
        }


        if (delayType.isMode(DelayType.Dynamic)) {
            if (target != null && target.isAlive()) {
                double dist = mc.player.distanceTo(target);
                currentDelayMs = clamp(dist * 200.0, latency.getMinValue(), latency.getMaxValue());
            }
        } else {
            currentDelayMs = latency.getRandomDoubleInRange();
        }


        processPacketQueue(false);
    }

    @EventListener
    public void onAttack(AttackEvent event) {
        if (!isPlayerUsable()) return;
        Entity attacked = event.target;
        if (!(attacked instanceof PlayerEntity)) return;
        if (!isEntityInRange(attacked)) return;

        if (attacked != target) {
            oldRenderPosition = new net.minecraft.util.math.Vec3d(0.0, 0.0, 0.0);
            currentPosition = new net.minecraft.util.math.Vec3d(0.0, 0.0, 0.0);
            resetState(true);
        }
        target = attacked;
    }

    @EventListener
    public void onRender3D(Render3DEvent event) {
        if (!isPlayerUsable()) return;
        if (target == null || !target.isAlive()) return;


        net.minecraft.util.math.Vec3d targetPos = target.getPos();
        if (targetPos != null) {
            currentPosition = targetPos;
        }

        if (currentPosition.y == 0.0) return;

        double lerpSpeed = Math.min(oldRenderPosition.distanceTo(currentPosition) * 0.1, event.tickDelta);
        lerpSpeed = Math.min(lerpSpeed, oldRenderPosition.distanceTo(currentPosition) * 0.01);
        oldRenderPosition = oldRenderPosition.lerp(currentPosition, lerpSpeed);


        float halfWidth = target.getWidth() / 2.0f;
        float height = target.getHeight();
        Color boxColor = new Color(255, 255, 255, 50);
        RenderUtils.renderFilledBox(
                event.matrixStack,
                (float) oldRenderPosition.x - halfWidth,
                (float) oldRenderPosition.y,
                (float) oldRenderPosition.z - halfWidth,
                (float) oldRenderPosition.x + halfWidth,
                (float) oldRenderPosition.y + height,
                (float) oldRenderPosition.z + halfWidth,
                boxColor
        );
    }

    private void processPacketQueue(boolean force) {
        if (mc.getNetworkHandler() == null) return;
        packetQueue.removeIf(data -> {
            if (force || System.currentTimeMillis() - data.timestamp >= currentDelayMs) {
                mc.execute(() -> {
                    try {
                        ClientPlayNetworkHandler handler = mc.getNetworkHandler();
                        if (handler != null) {
                            @SuppressWarnings("unchecked")
                            Packet<ClientPlayNetworkHandler> p = (Packet<ClientPlayNetworkHandler>) data.packet;
                            p.apply(handler);
                        }
                    } catch (Throwable ignored) {
                    }
                });
                return true;
            }
            return false;
        });
    }

    private void resetState(boolean flush) {
        if (flush) {
            processPacketQueue(true);
        } else {
            packetQueue.clear();
        }
        target = null;
    }

    private boolean isEntityInRange(Entity entity) {
        if (mc.player == null) return false;
        double d = mc.player.distanceTo(entity);
        double min = range.getMinValue();
        double max = range.getMaxValue();
        return d >= min && d <= max;
    }

    private static double clamp(double v, double min, double max) {
        return Math.max(min, Math.min(max, v));
    }

    private static class PacketData {
        final Packet<?> packet;
        final long timestamp;
        PacketData(Packet<?> packet, long timestamp) {
            this.packet = packet;
            this.timestamp = timestamp;
        }
    }
}


