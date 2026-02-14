package taz.womp.module.modules.render;

import net.minecraft.client.render.Camera;
import net.minecraft.client.render.RenderTickCounter;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.RotationAxis;
import net.minecraft.util.math.Vec3d;
import taz.womp.Womp;
import taz.womp.event.EventListener;
import taz.womp.event.events.Render3DEvent;
import taz.womp.module.Category;
import taz.womp.module.Module;
import taz.womp.module.modules.combat.AntiBot;
import taz.womp.module.setting.BooleanSetting;
import taz.womp.module.setting.NumberSetting;
import taz.womp.utils.ColorUtil;
import taz.womp.utils.EncryptedString;
import taz.womp.utils.RenderUtils;

import java.awt.*;

public final class PlayerESP extends Module {
    private final NumberSetting alpha = new NumberSetting(EncryptedString.of("Alpha"), 0.0, 255.0, 100.0, 1.0);
    private final NumberSetting lineWidth = new NumberSetting(EncryptedString.of("Line width"), 1.0, 10.0, 1.0, 1.0);
    private final BooleanSetting tracers = new BooleanSetting(EncryptedString.of("Tracers"), false).setDescription(EncryptedString.of("Draws a line from your player to the other"));
    private final BooleanSetting enableRainbowEffect = new BooleanSetting(EncryptedString.of("Rainbow"), false);
    private final NumberSetting redColor = new NumberSetting(EncryptedString.of("Red"), 0.0, 255.0, 255.0, 1.0);
    private final NumberSetting greenColor = new NumberSetting(EncryptedString.of("Green"), 0.0, 255.0, 50.0, 1.0);
    private final NumberSetting blueColor = new NumberSetting(EncryptedString.of("Blue"), 0.0, 255.0, 100.0, 1.0);

    public PlayerESP() {
        super(EncryptedString.of("Player ESP"), EncryptedString.of("Renders players through walls"), -1, Category.RENDER);
        this.addSettings(this.alpha, this.lineWidth, this.tracers, this.enableRainbowEffect, this.redColor, this.greenColor, this.blueColor);
    }

    @Override
    public void onEnable() {
        super.onEnable();
    }

    @Override
    public void onDisable() {
        super.onDisable();
    }

    @EventListener
    public void onRender3D(final Render3DEvent render3DEvent) {
        for (final Object next : this.mc.world.getPlayers()) {
            if (next != this.mc.player) {
                PlayerEntity player = (PlayerEntity) next;
                

                AntiBot antiBot = (AntiBot) Womp.INSTANCE.getModuleManager().getModuleByClass(AntiBot.class);
                if (antiBot != null && antiBot.isEnabled() && AntiBot.isABot(player)) {
                    continue;
                }

                final Camera camera = RenderUtils.getCamera();
                if (camera != null) {
                    final MatrixStack a = render3DEvent.matrixStack;
                    render3DEvent.matrixStack.push();
                    final Vec3d pos = RenderUtils.getCameraPos();
                    a.multiply(RotationAxis.POSITIVE_X.rotationDegrees(camera.getPitch()));
                    a.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(camera.getYaw() + 180.0f));
                    a.translate(-pos.x, -pos.y, -pos.z);
                }
                final double lerp = MathHelper.lerp(RenderTickCounter.ONE.getTickDelta(true), player.prevX, player.getX());
                final double lerp2 = MathHelper.lerp(RenderTickCounter.ONE.getTickDelta(true), player.prevY, player.getY());
                final double lerp3 = MathHelper.lerp(RenderTickCounter.ONE.getTickDelta(true), player.prevZ, player.getZ());
                

                Color color = getColorWithAlpha(alpha.getIntValue());
                

                RenderUtils.renderFilledBox(
                    render3DEvent.matrixStack, 
                    (float) lerp - player.getWidth() / 2.0f, 
                    (float) lerp2, 
                    (float) lerp3 - player.getWidth() / 2.0f, 
                    (float) lerp + player.getWidth() / 2.0f, 
                    (float) lerp2 + player.getHeight(), 
                    (float) lerp3 + player.getWidth() / 2.0f, 
                    color
                );

                if (this.tracers.getValue()) {
                    RenderUtils.renderLine(
                        render3DEvent.matrixStack,
                        new Color(color.getRed(), color.getGreen(), color.getBlue(), 255),
                        mc.crosshairTarget.getPos(),
                        player.getLerpedPos(RenderTickCounter.ONE.getTickDelta(true))
                    );
                }
                
                render3DEvent.matrixStack.pop();
            }
        }
    }

    private Color getColorWithAlpha(final int a) {
        if (this.enableRainbowEffect.getValue()) {
            return ColorUtil.a(1, a);
        }
        return new Color(redColor.getIntValue(), greenColor.getIntValue(), blueColor.getIntValue(), a);
    }
}
