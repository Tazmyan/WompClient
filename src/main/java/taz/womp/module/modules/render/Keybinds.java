package taz.womp.module.modules.render;

import net.minecraft.client.gui.DrawContext;
import taz.womp.Womp;
import taz.womp.event.EventListener;
import taz.womp.event.events.Render2DEvent;
import taz.womp.module.Category;
import taz.womp.module.Module;
import taz.womp.module.setting.BooleanSetting;
import taz.womp.module.setting.NumberSetting;
import taz.womp.module.setting.Setting;
import taz.womp.utils.ColorUtil;
import taz.womp.utils.EncryptedString;
import taz.womp.utils.KeyUtils;
import taz.womp.utils.RenderUtils;
import taz.womp.utils.TextRenderer;

import java.awt.*;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public final class Keybinds extends Module {
    
    private final NumberSetting opacity = new NumberSetting(EncryptedString.of("Opacity"), 0.0, 1.0, 0.8f, 0.01f).getValue(EncryptedString.of("Controls the opacity of keybind elements"));
    private final NumberSetting cornerRadius = new NumberSetting(EncryptedString.of("Corner Radius"), 0.0, 5.0, 3.0, 0.5).getValue(EncryptedString.of("Controls the roundness of corners"));
    private final BooleanSetting enableRainbowEffect = new BooleanSetting(EncryptedString.of("Rainbow"), false).setDescription(EncryptedString.of("Enables rainbow coloring effect"));
    private final NumberSetting rainbowSpeed = new NumberSetting(EncryptedString.of("Rainbow Speed"), 0.1f, 10.0, 2.0, 0.1f).getValue(EncryptedString.of("Controls the speed of the rainbow effect"));
    private final NumberSetting scale = new NumberSetting(EncryptedString.of("Scale"), 0.5, 2.0, 1.0, 0.1f);
    private final NumberSetting xOffset = new NumberSetting(EncryptedString.of("X Offset"), -500.0, 500.0, 12.0, 1.0);
    private final NumberSetting yOffset = new NumberSetting(EncryptedString.of("Y Offset"), -1000.0, 1000.0, -200.0, 1.0);
    
    private final Color primaryColor = new Color(65, 185, 255, 255);
    private static final Color PRIMARY_AQUA = new Color(120, 210, 255);

    public Keybinds() {
        super(EncryptedString.of("Keybinds"), EncryptedString.of("Shows modules with keybinds"), -1, Category.RENDER);
        Setting[] settingArray = new Setting[]{this.opacity, this.cornerRadius, this.enableRainbowEffect, this.rainbowSpeed, this.scale, this.xOffset, this.yOffset};
        this.addSettings(settingArray);
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
    public void onRender2D(Render2DEvent render2DEvent) {
        if (this.mc.currentScreen != Womp.INSTANCE.GUI) {
            List<Module> keybindModules = getKeybindModules();
            if (keybindModules.isEmpty()) {
                return;
            }
            
            DrawContext drawContext = render2DEvent.context;
            int width = this.mc.getWindow().getWidth();
            int height = this.mc.getWindow().getHeight();
            float bgOpacity = (float) (this.opacity != null ? this.opacity.getFloatValue() : 0.8f);
            float corner = (float) (this.cornerRadius != null ? this.cornerRadius.getFloatValue() * 2.0f : 6.0f);
            
            RenderUtils.unscaledProjection();
            renderKeybinds(drawContext, width, height, keybindModules, bgOpacity, corner);
            RenderUtils.scaledProjection();
        }
    }

    private void renderKeybinds(DrawContext drawContext, int width, int height, List<Module> modules, float bgOpacity, float corner) {
        float scaleValue = this.scale.getFloatValue();
        int x = (int) (this.xOffset.getIntValue() * scaleValue);
        int y = height + (int) (this.yOffset.getIntValue() * scaleValue);
        int padding = (int) (8 * scaleValue);
        

        float baseHue = 0.0f;
        if (this.enableRainbowEffect.getValue()) {
            baseHue = ColorUtil.rainbowHue(System.currentTimeMillis(), (float) this.rainbowSpeed.getFloatValue());
        }
        
        for (int i = 0; i < modules.size(); i++) {
            Module module = modules.get(i);
            String moduleString = module.getName().toString() + " | " + KeyUtils.getKey(module.getKeybind()).toString();
            int textWidth = TextRenderer.getWidth(moduleString);
            int boxWidth = textWidth + (padding * 2);
            int boxHeight = 20;
            

            Color bgColor = this.enableRainbowEffect.getValue()
                    ? ColorUtil.rainbowColor(baseHue, i * 0.015f, 0.3f, 0.15f, (int)(bgOpacity * 180))
                    : new Color(20, 25, 35, (int)(bgOpacity * 180));
            

            int scaledBoxWidth = (int) (boxWidth * scaleValue);
            int scaledBoxHeight = (int) (boxHeight * scaleValue);
            int scaledPadding = (int) (padding * scaleValue);
            int scaledAccentWidth = (int) (2 * scaleValue);
            
            RenderUtils.renderRoundedQuad(drawContext.getMatrices(), bgColor, x, y, x + scaledBoxWidth, y + scaledBoxHeight, corner, corner, corner, corner, 50);
            

            int accentColor = this.enableRainbowEffect.getValue()
                    ? ColorUtil.rainbowColor(baseHue, i * 0.015f, 0.8f, 1.0f, 255).getRGB()
                    : PRIMARY_AQUA.getRGB();
            RenderUtils.renderRoundedQuad(drawContext.getMatrices(), new Color(accentColor), x, y + scaledAccentWidth, x + scaledAccentWidth, y + scaledBoxHeight - scaledAccentWidth, 1.0, 1.0, 1.0, 1.0, 50);
            

            int moduleTextColor = this.enableRainbowEffect.getValue()
                    ? ColorUtil.rainbowColor(baseHue, i * 0.015f, 0.8f, 1.0f, 255).getRGB()
                    : this.primaryColor.getRGB();
            
            TextRenderer.drawString(moduleString, drawContext, x + scaledPadding, y + (scaledBoxHeight - 8) / 2 - 2, moduleTextColor);
            y += scaledBoxHeight + (int) (2 * scaleValue);
        }
    }

    private List<Module> getKeybindModules() {
        List<Module> modules = Womp.INSTANCE.getModuleManager().c();
        List<Module> keybindModules = new ArrayList<>();
        
        for (Module module : modules) {
            if (module instanceof taz.womp.module.modules.client.WompModule) {
                continue;
            }

            if (module.getKeybind() != -1 && module.getKeybind() != 0) {
                String keyName = KeyUtils.getKey(module.getKeybind()).toString();

                if (!keyName.equals("None")) {
                    keybindModules.add(module);
                }
            }
        }
        

        keybindModules.sort(Comparator.comparing(module -> module.getName().toString()));
        
        return keybindModules;
    }

}
