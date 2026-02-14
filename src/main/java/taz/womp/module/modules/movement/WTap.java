package taz.womp.module.modules.movement;

import net.minecraft.network.packet.c2s.play.PlayerInteractEntityC2SPacket;
import org.lwjgl.glfw.GLFW;
import taz.womp.event.EventListener;
import taz.womp.event.events.HudEvent;
import taz.womp.event.events.PacketSendEvent;
import taz.womp.module.Category;
import taz.womp.module.Module;
import taz.womp.module.setting.MinMaxSetting;
import taz.womp.utils.EncryptedString;
import taz.womp.utils.TimerUtils;

public class WTap extends Module {
    private final MinMaxSetting delay = new MinMaxSetting(EncryptedString.of("Delay"), 0, 1000, 1, 230, 270);
    private final MinMaxSetting chance = new MinMaxSetting(EncryptedString.of("Chance"), 0, 100, 1, 90, 100);
    private final TimerUtils sprintTimer = new TimerUtils();
    private final TimerUtils tapTimer = new TimerUtils();
    private boolean holdingForward;
    private boolean sprinting;
    private int currentDelay;
    private boolean jumpedWhileHitting;

    public WTap() {
        super(EncryptedString.of("W-Tap"), EncryptedString.of("Automatically W Taps for you so the opponent takes more knockback"), -1, Category.MOVEMENT);
        addSettings(delay, chance);
    }

    @Override
    public void onEnable() {
        currentDelay = delay.getRandomIntInRange();
        jumpedWhileHitting = false;
        super.onEnable();
    }

    @Override
    public void onDisable() {
        mc.options.forwardKey.setPressed(GLFW.glfwGetKey(mc.getWindow().getHandle(), GLFW.GLFW_KEY_W) == 1);
        sprinting = false;
        holdingForward = false;
        jumpedWhileHitting = false;
        super.onDisable();
    }

    @EventListener
    public void onHud(HudEvent event) {
        if (mc.player == null || mc.world == null) return;
        if (GLFW.glfwGetKey(mc.getWindow().getHandle(), GLFW.GLFW_KEY_W) != 1) {
            sprinting = false;
            holdingForward = false;
            return;
        }
        if (mc.player.isOnGround()) jumpedWhileHitting = false;
        if (GLFW.glfwGetKey(mc.getWindow().getHandle(), GLFW.GLFW_KEY_SPACE) == 1) {
            if (holdingForward || sprinting) {
                mc.options.forwardKey.setPressed(true);
                holdingForward = false;
                sprinting = false;
                return;
            }
        }
        if (holdingForward && tapTimer.hasReached(1)) {
            mc.options.forwardKey.setPressed(false);
            sprintTimer.reset();
            sprinting = true;
            holdingForward = false;
        }
        if (sprinting && sprintTimer.hasReached(currentDelay)) {
            mc.options.forwardKey.setPressed(true);
            sprinting = false;
            currentDelay = delay.getRandomIntInRange();
        }
    }

    @EventListener
    public void onPacketSend(PacketSendEvent event) {
        if (!(event.packet instanceof PlayerInteractEntityC2SPacket packet)) return;
        try {
            java.lang.reflect.Method getType = PlayerInteractEntityC2SPacket.class.getDeclaredMethod("getType");
            getType.setAccessible(true);
            Object type = getType.invoke(packet);
            if (type.toString().equals("ATTACK")) {
                if (GLFW.glfwGetKey(mc.getWindow().getHandle(), GLFW.GLFW_KEY_SPACE) == 1) {
                    jumpedWhileHitting = true;
                }
                if (!jumpedWhileHitting && mc.options.forwardKey.isPressed() && mc.player.isSprinting()) {
                    if ((Math.random() * 100) > chance.getRandomIntInRange()) return;
                    sprintTimer.reset();
                    holdingForward = true;
                }
            }
        } catch (Exception ignored) {}
    }
} 