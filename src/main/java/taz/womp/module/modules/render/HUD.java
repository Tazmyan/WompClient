package taz.womp.module.modules.render;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.network.PlayerListEntry;
import taz.womp.Womp;
import taz.womp.event.EventListener;
import taz.womp.event.events.Render2DEvent;
import taz.womp.module.Category;
import taz.womp.module.Module;
import taz.womp.module.setting.BooleanSetting;
import taz.womp.module.setting.ModeSetting;
import taz.womp.module.setting.NumberSetting;
import taz.womp.module.setting.Setting;
import taz.womp.utils.ColorUtil;
import taz.womp.utils.EncryptedString;
import taz.womp.utils.RenderUtils;
import taz.womp.utils.TextRenderer;

import java.awt.*;
import java.text.SimpleDateFormat;
import java.util.Comparator;
import java.util.Date;
import java.util.List;

public final class HUD
        extends Module {
    private static final CharSequence watermarkText = EncryptedString.of("Womp | Unreleased");
    private static final SimpleDateFormat timeFormatter = new SimpleDateFormat("HH:mm:ss");
    private final BooleanSetting showInfo = new BooleanSetting(EncryptedString.of("Info"), true).setDescription(EncryptedString.of("Shows system information"));
    private final BooleanSetting showModules = new BooleanSetting(EncryptedString.of("Modules"), true).setDescription(EncryptedString.of("Renders module array list"));
    private final BooleanSetting showTime = new BooleanSetting(EncryptedString.of("Time"), false).setDescription(EncryptedString.of("Shows current time"));
    private final BooleanSetting showCoordinates = new BooleanSetting(EncryptedString.of("Coordinates"), true).setDescription(EncryptedString.of("Shows player coordinates"));
    private final NumberSetting opacity = new NumberSetting(EncryptedString.of("Opacity"), 0.0, 1.0, 0.8f, 0.01f).getValue(EncryptedString.of("Controls the opacity of HUD elements"));
    private final NumberSetting cornerRadius = new NumberSetting(EncryptedString.of("Corner Radius"), 0.0, 5.0, 5.0, 0.5).getValue(EncryptedString.of("Controls the roundness of corners"));
    private final ModeSetting<ModuleListSorting> moduleSortingMode = new ModeSetting<>(EncryptedString.of("Sort Mode"), ModuleListSorting.LENGTH, ModuleListSorting.values()).setDescription(EncryptedString.of("How to sort the module list"));
    private final BooleanSetting enableRainbowEffect = new BooleanSetting(EncryptedString.of("Rainbow"), false).setDescription(EncryptedString.of("Enables rainbow coloring effect"));
    private final NumberSetting rainbowSpeed = new NumberSetting(EncryptedString.of("Rainbow Speed"), 0.1f, 10.0, 2.0, 0.1f).getValue(EncryptedString.of("Controls the speed of the rainbow effect"));
    private final Color primaryColor = new Color(65, 185, 255, 255);
    private static final Color PRIMARY_AQUA = new Color(120, 210, 255);

    public HUD() {
        super(EncryptedString.of("HUD"), EncryptedString.of(" Client information"), -1, Category.RENDER);
        Setting[] settingArray = new Setting[]{this.showInfo, this.showModules, this.showTime, this.showCoordinates, this.opacity, this.cornerRadius, this.moduleSortingMode, this.enableRainbowEffect, this.rainbowSpeed};
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
            DrawContext drawContext = render2DEvent.context;
            int width = this.mc.getWindow().getWidth();
            int height = this.mc.getWindow().getHeight();
            float bgOpacity = (float) (this.opacity != null ? this.opacity.getFloatValue() : 0.8f);
            float corner = (float) (this.cornerRadius != null ? this.cornerRadius.getFloatValue() * 2.0f : 10.0f);
            RenderUtils.unscaledProjection();

            int w = TextRenderer.getWidth(watermarkText);
            int boxH = 28;
            int boxW = w + 24;
            int boxX = 12;
            int boxY = 12;
            

            Color watermarkBg = new Color(20, 25, 35, (int)(bgOpacity * 200));
            RenderUtils.renderRoundedQuad(drawContext.getMatrices(), watermarkBg, boxX, boxY, boxX + boxW, boxY + boxH, corner + 2, corner + 2, corner + 2, corner + 2, 60);
            

            int textColor = this.primaryColor.getRGB();
            Color borderColor = new Color(textColor);
            Color glowColor = new Color(borderColor.getRed(), borderColor.getGreen(), borderColor.getBlue(), 80);
            RenderUtils.renderRoundedOutline(drawContext, glowColor, boxX - 1, boxY - 1, boxX + boxW + 1, boxY + boxH + 1, corner + 2, corner + 2, corner + 2, corner + 2, 1.5, 30);
            RenderUtils.renderRoundedOutline(drawContext, borderColor, boxX, boxY, boxX + boxW, boxY + boxH, corner + 2, corner + 2, corner + 2, corner + 2, 1.0, 40);
            

            TextRenderer.drawString(watermarkText, drawContext, boxX + 12, boxY + 8, textColor);
            int infoBoxY = 43;
            if (this.showInfo.getValue() && this.mc.player != null) {
                this.renderInfoModern(drawContext, infoBoxY, bgOpacity, corner);
            }
            if (this.showTime.getValue()) {
                this.renderTimeModern(drawContext, width, bgOpacity, corner);
            }
            if (this.showCoordinates.getValue() && this.mc.player != null) {
                this.renderCoordinatesModern(drawContext, height, bgOpacity, corner);
            }

            if (this.showModules.getValue()) {
                List<Module> modules = this.getSortedModules();
                int y = 20;
                int bgHeight = 20;
                int padding = 8;
                

                float baseHue = 0.0f;
                if (this.enableRainbowEffect.getValue()) {
                    baseHue = ColorUtil.rainbowHue(System.currentTimeMillis(), (float) this.rainbowSpeed.getFloatValue());
                }
                
                for (int i = 0; i < modules.size(); i++) {
                    Module module = modules.get(i);
                    if (!module.isEnabled()) continue;
                    String name = module.getName().toString();
                    int modWidth = TextRenderer.getWidth(name);
                    int x = width - modWidth - (padding * 2) - 4;
                    

                    Color bgColor = this.enableRainbowEffect.getValue()
                            ? ColorUtil.rainbowColor(baseHue, i * 0.015f, 0.3f, 0.15f, (int)(bgOpacity * 180))
                            : new Color(20, 25, 35, (int)(bgOpacity * 180));
                    
                    RenderUtils.renderRoundedQuad(drawContext.getMatrices(), bgColor, x - padding, y, x + modWidth + padding, y + bgHeight, corner, corner, corner, corner, 50);
                    

                    int accentColor = this.enableRainbowEffect.getValue()
                            ? ColorUtil.rainbowColor(baseHue, i * 0.015f, 0.8f, 1.0f, 255).getRGB()
                            : PRIMARY_AQUA.getRGB();
                    RenderUtils.renderRoundedQuad(drawContext.getMatrices(), new Color(accentColor), x - padding, y + 2, x - padding + 2, y + bgHeight - 2, 1.0, 1.0, 1.0, 1.0, 50);
                    

                    int moduleTextColor = this.enableRainbowEffect.getValue()
                            ? ColorUtil.rainbowColor(baseHue, i * 0.015f, 0.8f, 1.0f, 255).getRGB()
                            : this.primaryColor.getRGB();
                    
                    TextRenderer.drawString(name, drawContext, x, y + (bgHeight - 8) / 2 - 2, moduleTextColor);
                    y += bgHeight + 2;
                }
            }
            RenderUtils.scaledProjection();
        }
    }


    private void renderInfoModern(DrawContext drawContext, int boxY, float bgOpacity, float corner) {
        String string = this.getPingInfo();
        String string2 = "FPS: " + this.mc.getCurrentFps() + " | ";
        String string3 = this.mc.getCurrentServerEntry() == null ? "Single Player" : this.mc.getCurrentServerEntry().address;
        String fullInfo = string2 + string + string3;
        int totalWidth = TextRenderer.getWidth(fullInfo) + 24;
        int boxX = 12;
        int boxH = 26;
        

        float baseHue = 0.0f;
        if (this.enableRainbowEffect.getValue()) {
            baseHue = ColorUtil.rainbowHue(System.currentTimeMillis(), (float) this.rainbowSpeed.getFloatValue());
        }
        

        Color infoBg = this.enableRainbowEffect.getValue()
                ? ColorUtil.rainbowColor(baseHue, 0.0f, 0.3f, 0.15f, (int)(bgOpacity * 200))
                : new Color(20, 25, 35, (int)(bgOpacity * 200));
        RenderUtils.renderRoundedQuad(drawContext.getMatrices(), infoBg, boxX, boxY, boxX + totalWidth, boxY + boxH, corner + 2, corner + 2, corner + 2, corner + 2, 50);
        

        Color borderColor = this.enableRainbowEffect.getValue()
                ? ColorUtil.rainbowColor(baseHue, 0.0f, 0.8f, 1.0f, 60)
                : new Color(this.primaryColor.getRed(), this.primaryColor.getGreen(), this.primaryColor.getBlue(), 60);
        RenderUtils.renderRoundedOutline(drawContext, borderColor, boxX, boxY, boxX + totalWidth, boxY + boxH, corner + 2, corner + 2, corner + 2, corner + 2, 0.8, 25);
        
        int textColor = this.enableRainbowEffect.getValue()
                ? ColorUtil.rainbowColor(baseHue, 0.0f, 0.8f, 1.0f, 255).getRGB()
                : this.primaryColor.getRGB();
        TextRenderer.drawCenteredString(fullInfo, drawContext, boxX + totalWidth / 2, boxY + 8, textColor);
    }


    private void renderTimeModern(DrawContext drawContext, int n, float bgOpacity, float corner) {
        SimpleDateFormat simpleDateFormat = timeFormatter;
        String string = simpleDateFormat.format(new Date());
        int n2 = TextRenderer.getWidth(string);
        int n3 = n / 2;
        int boxW = n2 + 24;
        int boxH = 26;
        int boxX = (int)((float)n3 - (float)n2 / 2.0f - 6.0f);
        int boxY = 40;
        

        Color timeBg = new Color(20, 25, 35, (int)(bgOpacity * 200));
        RenderUtils.renderRoundedQuad(drawContext.getMatrices(), timeBg, boxX, boxY, boxX + boxW, boxY + boxH, corner + 2, corner + 2, corner + 2, corner + 2, 50);
        

        int n4 = this.primaryColor.getRGB();
        if (this.enableRainbowEffect.getValue()) {
            float baseHue = ColorUtil.rainbowHue(System.currentTimeMillis(), (float) this.rainbowSpeed.getFloatValue());
            n4 = ColorUtil.rainbowColor(baseHue, 0.0f, 0.8f, 1.0f, 255).getRGB();
        }
        Color borderColor = new Color(n4);
        borderColor = new Color(borderColor.getRed(), borderColor.getGreen(), borderColor.getBlue(), 80);
        RenderUtils.renderRoundedOutline(drawContext, borderColor, boxX, boxY, boxX + boxW, boxY + boxH, corner + 2, corner + 2, corner + 2, corner + 2, 1.0, 30);
        
        TextRenderer.drawString(string, drawContext, boxX + 12, boxY + 8, n4);
    }


    private void renderCoordinatesModern(DrawContext drawContext, int n, float bgOpacity, float corner) {
        Object[] objectArray = new Object[]{this.mc.player.getX()};
        String string = String.format("X: %.1f", objectArray);
        Object[] objectArray2 = new Object[]{this.mc.player.getY()};
        String string2 = String.format("Y: %.1f", objectArray2);
        Object[] objectArray3 = new Object[]{this.mc.player.getZ()};
        String string3 = String.format("Z: %.1f", objectArray3);
        String string4 = "";
        if (this.mc.world != null) {
            boolean bl = this.mc.world.getRegistryKey().getValue().getPath().contains("nether");
            boolean bl2 = this.mc.world.getRegistryKey().getValue().getPath().contains("overworld");
            if (bl) {
                Object[] objectArray4 = new Object[]{this.mc.player.getX() * 8.0, this.mc.player.getZ() * 8.0};
                string4 = String.format(" [%.1f, %.1f]", objectArray4);
            } else if (bl2) {
                Object[] objectArray5 = new Object[]{this.mc.player.getX() / 8.0, this.mc.player.getZ() / 8.0};
                string4 = String.format(" [%.1f, %.1f]", objectArray5);
            }
        }
        String string5 = string + " | " + string2 + " | " + string3 + string4;
        int n2 = TextRenderer.getWidth(string5);
        int boxX = 12;
        int boxH = 26;
        int boxY = n - 36;
        

        float baseHue = 0.0f;
        if (this.enableRainbowEffect.getValue()) {
            baseHue = ColorUtil.rainbowHue(System.currentTimeMillis(), (float) this.rainbowSpeed.getFloatValue());
        }
        

        Color coordsBg = this.enableRainbowEffect.getValue()
                ? ColorUtil.rainbowColor(baseHue, 0.0f, 0.3f, 0.15f, (int)(bgOpacity * 200))
                : new Color(20, 25, 35, (int)(bgOpacity * 200));
        RenderUtils.renderRoundedQuad(drawContext.getMatrices(), coordsBg, boxX, boxY, boxX + n2 + 24, boxY + boxH, corner + 2, corner + 2, corner + 2, corner + 2, 50);
        

        Color borderColor = this.enableRainbowEffect.getValue()
                ? ColorUtil.rainbowColor(baseHue, 0.0f, 0.8f, 1.0f, 60)
                : new Color(this.primaryColor.getRed(), this.primaryColor.getGreen(), this.primaryColor.getBlue(), 60);
        RenderUtils.renderRoundedOutline(drawContext, borderColor, boxX, boxY, boxX + n2 + 24, boxY + boxH, corner + 2, corner + 2, corner + 2, corner + 2, 0.8, 25);
        
        int textColor = this.enableRainbowEffect.getValue()
                ? ColorUtil.rainbowColor(baseHue, 0.0f, 0.8f, 1.0f, 255).getRGB()
                : this.primaryColor.getRGB();
        TextRenderer.drawString(string5, drawContext, boxX + 12, boxY + 8, textColor);
    }

    private List<Module> getSortedModules() {
        List<Module> list = Womp.INSTANCE.getModuleManager().b();
        ModuleListSorting n = (ModuleListSorting) this.moduleSortingMode.getValue();
        return switch (n) {
            case ALPHABETICAL -> list.stream().sorted(Comparator.comparing(module -> module.getName().toString())).toList();
            case LENGTH -> list.stream().sorted((module, module2) -> Integer.compare(TextRenderer.getWidth(module2.getName()), TextRenderer.getWidth(module.getName()))).toList();
            case CATEGORY -> list.stream().sorted(Comparator.comparing(Module::getCategory).thenComparing(module -> module.getName().toString())).toList();
        };
    }

    private String getPingInfo() {
        PlayerListEntry playerListEntry;
        String string = this.mc != null && this.mc.player != null && this.mc.getNetworkHandler() != null ? ((playerListEntry = this.mc.getNetworkHandler().getPlayerListEntry(this.mc.player.getUuid())) != null ? "Ping: " + playerListEntry.getLatency() + "ms | " : "Ping: " + "N/A | ") : "Ping: " + "N/A | ";
        return string;
    }

    enum ModuleListSorting {
        LENGTH("LENGTH", 0, "Length"),
        ALPHABETICAL("ALPHABETICAL", 1, "Alphabetical"),
        CATEGORY("CATEGORY", 2, "Category");

        private final String d;

        ModuleListSorting(final String name, final int ordinal, final String d) {
            this.d = d;
        }

        @Override
        public String toString() {
            return this.d;
        }
    }

}
