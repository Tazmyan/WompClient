package taz.womp.gui;

import net.minecraft.client.gui.DrawContext;
import taz.womp.Womp;
import taz.womp.gui.components.ModuleButton;
import taz.womp.module.Category;
import taz.womp.module.Module;
import taz.womp.utils.*;

import java.awt.*;
import java.util.ArrayList;
import java.util.List;

public final class CategoryWindow {
    public List<ModuleButton> moduleButtons;
    public int x;
    public int y;
    private final int width;
    private final int height;
    public Color currentColor;
    private final Category category;
    public boolean dragging;
    public boolean extended;
    private int dragX;
    private int dragY;
    private int prevX;
    private int prevY;
    public ClickGUI parent;
    private float hoverAnimation;
    private List<ModuleButton> allModuleButtons;

    public boolean isClickHandled(double x, double y) {
        if (isHovered(x, y)) {
            return true;
        }

        if (extended) {
            for (ModuleButton button : moduleButtons) {
                if (button.isHovered(x, y)) {
                    return true;
                }
                if (button.extended) {
                    for (Component comp : button.settings) {
                        if (comp.isHovered(x, y)) {
                            return true;
                        }
                    }
                }
            }
        }
        return false;
    }

    public CategoryWindow(final int x, final int y, final int width, final int height, final Category category, final ClickGUI parent) {
        this.moduleButtons = new ArrayList<>();
        this.hoverAnimation = 0.0f;
        this.x = x;
        this.y = y;
        this.width = width;
        this.dragging = false;
        this.extended = true;
        this.height = height;
        this.category = category;
        this.parent = parent;
        this.prevX = x;
        this.prevY = y;
    }

    public void initModules() {
        final List<Module> modules = new ArrayList<>(Womp.INSTANCE.getModuleManager().a(this.category));
        modules.sort(java.util.Comparator.comparing(m -> m.getName().toString().toLowerCase()));
        int offset = this.height;
        this.moduleButtons.clear();
        this.allModuleButtons = new ArrayList<>();
        for (Module module : modules) {
            ModuleButton mb = new ModuleButton(this, module, offset);
            this.moduleButtons.add(mb);
            this.allModuleButtons.add(mb);
            offset += this.height;
        }
    }

    public void render(final DrawContext context, final int n, final int n2, final float n3) {
        final Color color = new Color(20, 25, 35, taz.womp.module.modules.client.WompModule.windowAlpha.getIntValue());
        if (this.currentColor == null) {
            this.currentColor = new Color(20, 25, 35, 0);
        } else {
            this.currentColor = ColorUtil.a(0.05f, color, this.currentColor);
        }
        float n4 = this.isHovered(n, n2) && !this.dragging ? 1.0F : 0.0F;
        this.hoverAnimation = (float) MathUtil.approachValue(n3 * 0.1f, this.hoverAnimation, n4);
        

        final Color baseColor = new Color(20, 25, 35, this.currentColor.getAlpha());
        final Color hoverColor = new Color(30, 40, 55, this.currentColor.getAlpha());
        final Color a = ColorUtil.a(baseColor, hoverColor, this.hoverAnimation);
        
        float n5 = this.extended ? 0.0F : 8.0F;
        float n6 = this.extended ? 0.0F : 8.0F;
        int windowHeight = this.height;
        if (this.extended) {
            for (ModuleButton button : this.moduleButtons) {
                windowHeight += button.animation.getAnimation();
            }
        }
        

        RenderUtils.renderRoundedQuad(context.getMatrices(), a, this.prevX, this.prevY, this.prevX + this.width, this.prevY + windowHeight, 8.0, 8.0, n5, n6, 60.0);
        

        final Color mainColor = Utils.getMainColor(255, this.category.ordinal());
        final Color borderColor = new Color(mainColor.getRed(), mainColor.getGreen(), mainColor.getBlue(), 120);
        RenderUtils.renderRoundedOutline(context, borderColor, this.prevX, this.prevY, this.prevX + this.width, this.prevY + windowHeight, 8.0, 8.0, n5, n6, 1.5, 40.0);
        
        final CharSequence f = this.category.name;
        

        String iconPath = "icons/categories/" + this.category.name.toString().toLowerCase() + ".png";
        net.minecraft.util.Identifier icon = net.minecraft.util.Identifier.of("icons", iconPath);
        int iconSize = 18;
        int iconX = this.prevX + 10;
        int iconY = this.prevY + 6;
        

        Color iconBg = new Color(mainColor.getRed(), mainColor.getGreen(), mainColor.getBlue(), 30);
        RenderUtils.renderRoundedQuad(context.getMatrices(), iconBg, iconX - 2, iconY - 2, iconX + iconSize + 2, iconY + iconSize + 2, 6, 6, 6, 6, 20);
        
        context.drawTexture(icon, iconX, iconY, 0, 0, iconSize, iconSize, iconSize, iconSize);
        
        final int n7 = this.prevX + iconSize + 16;
        final int n8 = this.prevY + 9;
        

        Color textShadow = new Color(0, 0, 0, 150);
        Color textGlow = new Color(mainColor.getRed(), mainColor.getGreen(), mainColor.getBlue(), 100);
        TextRenderer.drawString(f, context, n7 + 2, n8 + 2, textShadow.getRGB());
        TextRenderer.drawString(f, context, n7 + 1, n8 + 1, textGlow.getRGB());
        TextRenderer.drawString(f, context, n7, n8, mainColor.brighter().getRGB());
        this.updateButtons(n3);
        if (this.extended) {
            this.renderModuleButtons(context, n, n2, n3);
        }
    }

    private void renderModuleButtons(final DrawContext context, final int n, final int n2, final float n3) {
        String query = (parent != null && parent.isSearchBarActive()) ? parent.getSearchQuery() : "";
        if (this.allModuleButtons == null || this.allModuleButtons.isEmpty()) return;
        for (ModuleButton module : this.allModuleButtons) {
            if (module == null || module.module == null || module.module.getName() == null) continue;
            if (query.isEmpty() || module.module.getName().toString().toLowerCase().contains(query.toLowerCase())) {
                module.render(context, n, n2, n3);
            }
        }
    }

    public void keyPressed(final int keyCode, final int scanCode, final int modifiers) {
        try {
            String query = (parent != null && parent.isSearchBarActive()) ? parent.getSearchQuery() : "";
            if (allModuleButtons != null) {
                for (ModuleButton moduleButton : this.allModuleButtons) {
                    if (moduleButton == null || moduleButton.module == null || moduleButton.module.getName() == null) continue;
                    if (query.isEmpty() || moduleButton.module.getName().toString().toLowerCase().contains(query.toLowerCase())) {
                        try {
                            moduleButton.keyPressed(keyCode, scanCode, modifiers);
                        } catch (Exception e) {

                        }
                    }
                }
            }
        } catch (Exception e) {
        }
    }

    public void onGuiClose() {
        this.currentColor = null;
        for (ModuleButton moduleButton : this.moduleButtons) {
            moduleButton.onGuiClose();
        }
        this.dragging = false;
    }

    public void mouseClicked(final double x, final double y, final int button) {
        if (this.isHovered(x, y)) {

            switch (button) {
                case 0:
                    if (!this.parent.isDraggingAlready()) {
                        this.dragging = true;
                        this.dragX = (int) (x - this.x);
                        this.dragY = (int) (y - this.y);
                    }
                    break;

                case 1:
                    this.extended = !this.extended;
                    break;

                default:

                    break;
            }
        }
        if (this.extended) {
            String query = (parent != null && parent.isSearchBarActive()) ? parent.getSearchQuery() : "";
            for (ModuleButton moduleButton : this.allModuleButtons) {
                if (moduleButton == null || moduleButton.module == null || moduleButton.module.getName() == null) continue;
                if (query.isEmpty() || moduleButton.module.getName().toString().toLowerCase().contains(query.toLowerCase())) {
                    moduleButton.mouseClicked(x, y, button);
                }
            }
        }
    }

    public void mouseDragged(final double n, final double n2, final int n3, final double n4, final double n5) {
        if (this.extended) {
            for (ModuleButton moduleButton : this.moduleButtons) {
                moduleButton.mouseDragged(n, n2, n3, n4, n5);
            }
        }
    }

    public void updateButtons(final float n) {
        int height = this.height;
        if (this.extended) {
            for (final ModuleButton next : this.moduleButtons) {
                final Animation animation = next.animation;
                double n2;
                if (next.extended) {
                    n2 = this.height * (next.settings.stream().filter(s -> s.setting.isVisible()).count() + 1);
                } else {
                    n2 = this.height;
                }
                animation.animate(0.5 * n, n2);
                final double animation2 = next.animation.getAnimation();
                next.offset = height;

                height += (int) animation2;
            }
        }
    }

    public void mouseReleased(final double n, final double n2, final int n3) {
        if (n3 == 0 && this.dragging) {
            this.dragging = false;
        }
        for (ModuleButton moduleButton : this.moduleButtons) {
            moduleButton.mouseReleased(n, n2, n3);
        }
    }

    public void mouseScrolled(final double n, final double n2, final double n3, final double n4) {
        int screenHeight = net.minecraft.client.MinecraftClient.getInstance().getWindow().getHeight();
        int minY = 16;
        int maxY = screenHeight - this.height - 16;
        int newY = (int) (this.y + n4 * 20.0);
        if (newY < minY) newY = minY;
        if (newY > maxY) newY = maxY;
        this.prevX = this.x;
        this.prevY = newY;
        this.setY(newY);
    }

    public int getX() {
        return this.prevX;
    }

    public int getY() {
        return this.prevY;
    }

    public void setY(final int y) {
        this.y = y;
    }

    public void setX(final int x) {
        this.x = x;
    }

    public int getWidth() {
        return this.width;
    }

    public int getHeight() {
        return this.height;
    }

    public boolean isHovered(final double n, final double n2) {
        return n > this.x && n < this.x + this.width && n2 > this.y && n2 < this.y + this.height;
    }

    public boolean isPrevHovered(final double n, final double n2) {
        return n > this.prevX && n < this.prevX + this.width && n2 > this.prevY && n2 < this.prevY + this.height;
    }

    public void updatePosition(final double n, final double n2, final float n3) {
        this.prevX = this.x;
        this.prevY = this.y;
        if (this.dragging) {
            double n4;
            if (this.isHovered(n, n2)) {
                n4 = this.x;
            } else {
                n4 = this.prevX;
            }
            this.x = (int) MathUtil.approachValue(0.3f * n3, n4, n - this.dragX);
            double n5;
            if (this.isHovered(n, n2)) {
                n5 = this.y;
            } else {
                n5 = this.prevY;
            }
            this.y = (int) MathUtil.approachValue(0.3f * n3, n5, n2 - this.dragY);
        }
    }
    public void refresh() {
        try {
            if (parent != null && parent.isSearchBarActive() && parent.getSearchQuery() != null) {
                String query = parent.getSearchQuery().toLowerCase();
                if (allModuleButtons != null) {
                    moduleButtons = allModuleButtons.stream()
                        .filter(mb -> mb != null && mb.module != null && mb.module.getName() != null && 
                               mb.module.getName().toString().toLowerCase().contains(query))
                        .collect(java.util.stream.Collectors.toList());
                }
            } else {
                if (allModuleButtons != null) {
                    moduleButtons = new java.util.ArrayList<>(allModuleButtons);
                } else {
                    moduleButtons = new java.util.ArrayList<>();
                }
            }
        } catch (Exception e) {

            if (moduleButtons == null) {
                moduleButtons = new java.util.ArrayList<>();
            }
        }
    }
}
