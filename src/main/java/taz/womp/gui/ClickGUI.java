package taz.womp.gui;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.text.Text;
import taz.womp.Womp;
import taz.womp.module.Category;
import taz.womp.utils.ColorUtil;
import taz.womp.utils.RenderUtils;
import taz.womp.utils.TextRenderer;
import taz.womp.module.modules.client.SelfDestruct;

import java.awt.*;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public final class ClickGUI extends Screen {
    public List<CategoryWindow> windows;
    public Color currentColor;
    private CharSequence tooltipText;
    private int tooltipX;
    private int tooltipY;
    private final Color DESCRIPTION_BG;
    private String searchQuery = "";
    private boolean searchBarActive = false;
    private long lastBlink = 0;
    private boolean showCursor = true;
    private final String SEARCH_PLACEHOLDER = "Search...";
    private long lastSearchInputTime = 0;
    private static final long SEARCH_REFRESH_DELAY_MS = 300;
    private boolean searchNeedsRefresh = false;
    private static final net.minecraft.util.Identifier CONFIGS_ICON = net.minecraft.util.Identifier.of("icons", "icons/other/configs.png");
    private static final int CONFIGS_BTN_SIZE = 32;
    private static final int CONFIGS_BTN_MARGIN = 16;
    private boolean hoveringConfigsBtn = false;

    public ClickGUI() {
        super(Text.empty());
        this.windows = new ArrayList<>();
        this.tooltipText = null;
        this.DESCRIPTION_BG = new Color(40, 40, 40, 200);
    }

    @Override
    protected void init() {
        this.rebuild();
        super.init();
    }

    public boolean isDraggingAlready() {
        for (CategoryWindow window : this.windows) {
            if (window.dragging) {
                return true;
            }
        }
        return false;
    }

    public void setTooltip(final CharSequence tooltipText, final int tooltipX, final int tooltipY) {
        this.tooltipText = tooltipText;
        this.tooltipX = tooltipX;
        this.tooltipY = tooltipY;
    }

    public void setInitialFocus() {
        if (this.client == null) {
            return;
        }
        super.setInitialFocus();
    }

    public void render(final DrawContext drawContext, final int n, final int n2, final float n3) {
        if (Womp.mc.currentScreen == this) {
            if (Womp.INSTANCE.screen != null && Womp.INSTANCE.screen != this && !(Womp.INSTANCE.screen instanceof ClickGUI)) {
                Womp.INSTANCE.screen.render(drawContext, 0, 0, n3);
            }
            if (this.currentColor == null) {
                this.currentColor = new Color(0, 0, 0, 0);
            } else {
                this.currentColor = new Color(0, 0, 0, this.currentColor.getAlpha());
            }
            final int alpha = this.currentColor.getAlpha();
            int n4;
            if (taz.womp.module.modules.client.WompModule.renderBackground.getValue()) {
                n4 = 200;
            } else {
                n4 = 0;
            }
            if (alpha != n4) {
                int n5;
                if (taz.womp.module.modules.client.WompModule.renderBackground.getValue()) {
                    n5 = 200;
                } else {
                    n5 = 0;
                }
                this.currentColor = ColorUtil.a(0.05f, n5, this.currentColor);
            }
            if (Womp.mc.currentScreen instanceof ClickGUI) {

                int width = Womp.mc.getWindow().getWidth();
                int height = Womp.mc.getWindow().getHeight();
                

                for (int i = 0; i < height; i += 2) {
                    float progress = (float) i / height;
                    int gradientAlpha = (int) (this.currentColor.getAlpha() * (0.3f + 0.7f * progress));
                    Color gradientColor = new Color(15, 20, 35, gradientAlpha);
                    drawContext.fill(0, i, width, i + 2, gradientColor.getRGB());
                }
            }
            RenderUtils.unscaledProjection();
            final int n6 = n * (int) MinecraftClient.getInstance().getWindow().getScaleFactor();
            final int n7 = n2 * (int) MinecraftClient.getInstance().getWindow().getScaleFactor();
            super.render(drawContext, n6, n7, n3);
            for (final CategoryWindow next : this.windows) {
                next.render(drawContext, n6, n7, n3);
                next.updatePosition(n6, n7, n3);
            }
            int barWidth = 220;
            int barHeight = 28;
            int winWidth = Womp.mc.getWindow().getWidth();
            int winHeight = Womp.mc.getWindow().getHeight();
            int barX = winWidth / 2 - barWidth / 2;
            int barY = winHeight - barHeight - 24;
            

            Color barBg = new Color(20, 25, 35, taz.womp.module.modules.client.WompModule.windowAlpha.getIntValue());
            Color barBorder = searchBarActive ? new Color(120, 210, 255, 180) : new Color(60, 70, 85, 120);
            

            RenderUtils.renderRoundedQuad(drawContext.getMatrices(), barBg, barX, barY, barX + barWidth, barY + barHeight, 12.0, 12.0, 12.0, 12.0, 50.0);
            

            float borderIntensity = searchBarActive ? 1.0f : 0.3f;
            Color glowColor = new Color(120, 210, 255, (int)(borderIntensity * 100));
            RenderUtils.renderRoundedOutline(drawContext, glowColor, barX - 1, barY - 1, barX + barWidth + 1, barY + barHeight + 1, 12.0, 12.0, 12.0, 12.0, 1.5, 30.0);
            RenderUtils.renderRoundedOutline(drawContext, barBorder, barX, barY, barX + barWidth, barY + barHeight, 12.0, 12.0, 12.0, 12.0, 1.0, 30.0);

            net.minecraft.util.Identifier searchIcon = net.minecraft.util.Identifier.of("icons", "icons/search.png");
            int iconSize = 14;
            int iconX = barX + 6;
            int iconY = barY + (barHeight - iconSize) / 2;
            drawContext.drawTexture(searchIcon, iconX, iconY, 0, 0, iconSize, iconSize, iconSize, iconSize);

            int textX = barX + 6 + iconSize + 4;
            int textY = barY + (barHeight / 2) - 7;
            String display = searchQuery.isEmpty() && !searchBarActive ? SEARCH_PLACEHOLDER : searchQuery;
            Color textColor = searchQuery.isEmpty() && !searchBarActive ? new Color(120,120,120,180) : Color.WHITE;
            TextRenderer.drawString(display, drawContext, textX, textY, textColor.getRGB());
            if (searchBarActive) {
                long now = System.currentTimeMillis();
                if (now - lastBlink > 500) {
                    showCursor = !showCursor;
                    lastBlink = now;
                }
                if (showCursor) {
                    int cursorX = textX + TextRenderer.getWidth(searchQuery);
                    TextRenderer.drawString("|", drawContext, cursorX, textY, Color.WHITE.getRGB());
                }
            }
            if (this.tooltipText != null) {
                this.renderTooltip(drawContext, this.tooltipText, this.tooltipX, this.tooltipY);
                this.tooltipText = null;
            }

            if (searchBarActive && searchNeedsRefresh) {
                long now = System.currentTimeMillis();
                if (now - lastSearchInputTime >= SEARCH_REFRESH_DELAY_MS) {
                    refreshSearch();
                }
            }
            int btnX = winWidth - CONFIGS_BTN_SIZE - CONFIGS_BTN_MARGIN;
            int btnY = CONFIGS_BTN_MARGIN;
            hoveringConfigsBtn = n >= btnX && n <= btnX + CONFIGS_BTN_SIZE && n2 >= btnY && n2 <= btnY + CONFIGS_BTN_SIZE;
            

            Color btnBg = hoveringConfigsBtn ? new Color(30, 40, 55, 240) : new Color(20, 25, 35, 200);
            Color btnBorder = hoveringConfigsBtn ? new Color(120, 210, 255, 150) : new Color(60, 70, 85, 100);
            

            RenderUtils.renderRoundedQuad(drawContext.getMatrices(), btnBg, btnX, btnY, btnX + CONFIGS_BTN_SIZE, btnY + CONFIGS_BTN_SIZE, 10, 10, 10, 10, 30);
            RenderUtils.renderRoundedOutline(drawContext, btnBorder, btnX, btnY, btnX + CONFIGS_BTN_SIZE, btnY + CONFIGS_BTN_SIZE, 10, 10, 10, 10, 1.5, 20);
            

            if (hoveringConfigsBtn) {
                Color iconGlow = new Color(120, 210, 255, 80);
                RenderUtils.renderRoundedQuad(drawContext.getMatrices(), iconGlow, btnX + 2, btnY + 2, btnX + CONFIGS_BTN_SIZE - 2, btnY + CONFIGS_BTN_SIZE - 2, 8, 8, 8, 8, 15);
            }
            
            drawContext.drawTexture(CONFIGS_ICON, btnX + 4, btnY + 4, 0, 0, 24, 24, 24, 24);
            RenderUtils.scaledProjection();
        }
    }

    @Override
    public boolean charTyped(char chr, int modifiers) {
        if (SelfDestruct.isActive) {
            return true;
        }
        if (searchBarActive) {
            if (chr >= ' ' && chr != 127) {
                searchQuery += chr;
                lastSearchInputTime = System.currentTimeMillis();
                searchNeedsRefresh = true;
            }

            return true;
        }
        if (Component.focusedComponent != null) {
            Component.focusedComponent.charTyped(chr, modifiers);
            return true;
        }
        return true;
    }

    public boolean mouseClicked(final double n, final double n2, final int n3) {
        if (SelfDestruct.isActive) {
            return true;
        }
        double scaleFactor = MinecraftClient.getInstance().getWindow().getScaleFactor();
        double mouseX = n * scaleFactor;
        double mouseY = n2 * scaleFactor;
        double winWidth = Womp.mc.getWindow().getWidth();
        double winHeight = Womp.mc.getWindow().getHeight();
        int barWidth = 180;
        int barHeight = 22;
        int barX = (int) (winWidth / 2 - barWidth / 2);
        int barY = (int) (winHeight - barHeight - 24);
        

        if (mouseX >= barX && mouseX <= barX + barWidth && mouseY >= barY && mouseY <= barY + barHeight) {
            searchBarActive = true;
            this.setInitialFocus();
            Component.focusedComponent = null;
            return true;
        } else if (searchBarActive) {

            searchBarActive = false;
        }
        
        int btnX = (int) (winWidth - CONFIGS_BTN_SIZE - CONFIGS_BTN_MARGIN);
        int btnY = CONFIGS_BTN_MARGIN;
        if (mouseX >= btnX && mouseX <= btnX + CONFIGS_BTN_SIZE && mouseY >= btnY && mouseY <= btnY + CONFIGS_BTN_SIZE) {

            taz.womp.module.modules.client.WompModule clickGuiModule = (taz.womp.module.modules.client.WompModule) taz.womp.Womp.INSTANCE.getModuleManager().getModuleByClass(taz.womp.module.modules.client.WompModule.class);
            if (clickGuiModule != null) clickGuiModule.setEnabled(false);
            MinecraftClient.getInstance().setScreen(new ConfigScreen());
            return true;
        }
        
        final double n4 = mouseX;
        final double n5 = mouseY;

        boolean clickWasOnAWindow = false;
        for (CategoryWindow window : this.windows) {
            if (window.isClickHandled(n4, n5)) {
                clickWasOnAWindow = true;
                break;
            }
        }

        if (!clickWasOnAWindow && Component.focusedComponent != null) {
            Component.focusedComponent.stopEditing();
            Component.focusedComponent = null;
        }

        final Iterator<CategoryWindow> iterator = this.windows.iterator();
        while (iterator.hasNext()) {
            iterator.next().mouseClicked(n4, n5, n3);
        }
        return super.mouseClicked(n4, n5, n3);
    }

    public boolean mouseDragged(final double n, final double n2, final int n3, final double n4, final double n5) {
        if (SelfDestruct.isActive) {
            return true;
        }
        final double n6 = n * (int) MinecraftClient.getInstance().getWindow().getScaleFactor();
        final double n7 = n2 * (int) MinecraftClient.getInstance().getWindow().getScaleFactor();
        final Iterator<CategoryWindow> iterator = this.windows.iterator();
        while (iterator.hasNext()) {
            iterator.next().mouseDragged(n6, n7, n3, n4, n5);
        }
        return super.mouseDragged(n6, n7, n3, n4, n5);
    }

    public boolean mouseScrolled(final double n, final double n2, final double n3, final double n4) {
        if (SelfDestruct.isActive) {
            return true;
        }
        final double n5 = n2 * MinecraftClient.getInstance().getWindow().getScaleFactor();
        final Iterator<CategoryWindow> iterator = this.windows.iterator();
        while (iterator.hasNext()) {
            iterator.next().mouseScrolled(n, n5, n3, n4);
        }
        return super.mouseScrolled(n, n5, n3, n4);
    }

    public boolean shouldPause() {
        return false;
    }

    public void close() {
        if (Womp.mc.currentScreen != this) {
            Womp.INSTANCE.getModuleManager().getModuleByClass(taz.womp.module.modules.client.WompModule.class).setEnabled(false);
        }
        this.onGuiClose();
    }

    public void onGuiClose() {
        if (Component.focusedComponent != null) {
            Component.focusedComponent.stopEditing();
        }
        searchBarActive = false;
        Womp.mc.setScreenAndRender(Womp.INSTANCE.screen);
        this.currentColor = null;
        final Iterator<CategoryWindow> iterator = this.windows.iterator();
        while (iterator.hasNext()) {
            iterator.next().onGuiClose();
        }
    }

    public boolean mouseReleased(final double n, final double n2, final int n3) {
        if (SelfDestruct.isActive) {
            return true;
        }
        final double n4 = n * (int) MinecraftClient.getInstance().getWindow().getScaleFactor();
        final double n5 = n2 * (int) MinecraftClient.getInstance().getWindow().getScaleFactor();
        final Iterator<CategoryWindow> iterator = this.windows.iterator();
        while (iterator.hasNext()) {
            iterator.next().mouseReleased(n4, n5, n3);
        }
        return super.mouseReleased(n4, n5, n3);
    }

    private void renderTooltip(final DrawContext drawContext, final CharSequence charSequence, int n, final int n2) {
        if (charSequence == null || charSequence.length() == 0) {
            return;
        }
        final int a = TextRenderer.getWidth(charSequence);
        final int framebufferWidth = Womp.mc.getWindow().getFramebufferWidth();
        if (n + a + 10 > framebufferWidth) {
            n = framebufferWidth - a - 10;
        }
        RenderUtils.renderRoundedQuad(drawContext.getMatrices(), this.DESCRIPTION_BG, n - 5, n2 - 5, n + a + 5, n2 + 15, 6.0, 6.0, 6.0, 6.0, 50.0);
        TextRenderer.drawString(charSequence, drawContext, n, n2, Color.WHITE.getRGB());
    }

    public void refresh() {
        this.currentColor = null;
        for (CategoryWindow window : this.windows) {
            window.refresh();
        }
    }

    public void rebuild() {
        this.windows.clear();
        int n = 50;
        final Category[] values = Category.values();
        for (Category value : values) {
            CategoryWindow window = new CategoryWindow(n, 50, 230, 30, value, this);
            window.initModules();
            this.windows.add(window);
            n += 250;
        }
    }

    @Override
    public void removed() {
        super.removed();
        taz.womp.module.modules.client.WompModule module =
            (taz.womp.module.modules.client.WompModule)
            taz.womp.Womp.INSTANCE.getModuleManager().getModuleByClass(taz.womp.module.modules.client.WompModule.class);
        if (module != null && module.isEnabled()) {
            module.setEnabled(false);
        }
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (SelfDestruct.isActive) {
            return true;
        }
        if (searchBarActive) {
            if (keyCode == 256) { // ESC
                searchBarActive = false;
                return true;
            } else if (keyCode == 259) { // Backspace
                if (!searchQuery.isEmpty()) {
                    searchQuery = searchQuery.substring(0, searchQuery.length() - 1);
                    lastSearchInputTime = System.currentTimeMillis();
                    searchNeedsRefresh = true;
                }
                return true;
            }
            return true;
        }

        if (keyCode == 256) { // ESC
            this.close();
            return true;
        }

        if (Component.focusedComponent != null) {
            Component.focusedComponent.keyPressed(keyCode, scanCode, modifiers);
            return true;
        }

        for (CategoryWindow window : windows) {
            window.keyPressed(keyCode, scanCode, modifiers);
        }

        return true;
    }

    public boolean isSearchBarActive() {
        return searchBarActive;
    }

    public String getSearchQuery() {
        return searchQuery;
    }

    private void refreshSearch() {
        try {
            if (windows != null) {
                for (CategoryWindow window : windows) {
                    if (window != null) {
                        window.refresh();
                    }
                }
            }
            searchNeedsRefresh = false;
        } catch (Exception e) {
            searchNeedsRefresh = false;
        }
    }

    static {
    }
}
