package taz.womp.module.modules.movement;

import net.minecraft.client.MinecraftClient;
import taz.womp.event.EventListener;
import taz.womp.event.events.TickEvent;
import taz.womp.module.Category;
import taz.womp.module.Module;
import taz.womp.module.setting.BooleanSetting;
import taz.womp.utils.EncryptedString;

public class Airstuck extends Module {
    private final MinecraftClient mc = MinecraftClient.getInstance();


    private final BooleanSetting stopHorizontal = new BooleanSetting(EncryptedString.of("Stop Horizontal"), true);

    public Airstuck() {
        super(EncryptedString.of("Airstuck"),
                EncryptedString.of("Stops your momentum when flying with an elytra"),
                -1,
                Category.MOVEMENT);


        addSettings(stopHorizontal);
    }

    @EventListener
    public void onTick(TickEvent event) {
        if (!isPlayerUsable()) return;


        if (!mc.player.isFallFlying()) {
            return;
        }

        double x = stopHorizontal.getValue() ? 0 : mc.player.getVelocity().x;
        double y = -0.05;
        double z = stopHorizontal.getValue() ? 0 : mc.player.getVelocity().z;

        mc.player.setVelocity(x, y, z);
    }
}
