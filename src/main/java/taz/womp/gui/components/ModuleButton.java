package taz.womp.gui.components;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import taz.womp.Womp;
import taz.womp.gui.CategoryWindow;
import taz.womp.gui.Component;
import taz.womp.module.Module;
import taz.womp.module.setting.*;
import taz.womp.utils.*;

import java.awt.*;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public final class ModuleButton {
    public List<Component> settings;
    public CategoryWindow parent;
    public Module module;
    public int offset;
    public boolean extended;
    public int settingOffset;
    public Color currentColor;
    public Color currentAlpha;
    public Animation animation;
    private final Color ACCENT_COLOR;
    private final Color HOVER_COLOR;
    private final Color ENABLED_COLOR;
    private final Color DISABLED_COLOR;
    private float hoverAnimation;
    private float enabledAnimation;

    public ModuleButton(final CategoryWindow parent, final Module module, final int offset) {
        this.settings = new ArrayList<>();
        this.animation = new Animation(0.0);
        this.ACCENT_COLOR = new Color(65, 105, 225);
        this.HOVER_COLOR = new Color(255, 255, 255, 20);
        this.ENABLED_COLOR = new Color(120, 190, 255);
        this.DISABLED_COLOR = new Color(180, 180, 180);
        this.hoverAnimation = 0.0f;
        this.enabledAnimation = 0.0f;
        this.parent = parent;
        this.module = module;
        this.offset = offset;
        this.extended = false;
        this.settingOffset = parent.getHeight();
        for (final Setting setting : module.getSettings()) {
            if (setting instanceof BooleanSetting) {
                this.settings.add(new Checkbox(this, (Setting) setting, this.settingOffset));
            } else if (setting instanceof NumberSetting) {
                this.settings.add(new NumberBox(this, (Setting) setting, this.settingOffset));
            } else if (setting instanceof ModeSetting) {
                this.settings.add(new ModeBox(this, (Setting) setting, this.settingOffset));
            } else if (setting instanceof BindSetting) {
                this.settings.add(new Keybind(this, (Setting) setting, this.settingOffset));
            } else if (setting instanceof StringSetting) {
                this.settings.add(new TextBox(this, (Setting) setting, this.settingOffset));
            } else if (setting instanceof MinMaxSetting) {
                this.settings.add(new Slider(this, (Setting) setting, this.settingOffset));
            } else if (setting instanceof ItemSetting) {
                this.settings.add(new ItemBox(this, (Setting) setting, this.settingOffset));
            }
        }
    }

    public void render(final DrawContext drawContext, final int n, final int n2, final float n3) {
        if (this.parent.getY() + this.offset > MinecraftClient.getInstance().getWindow().getHeight()) {
            return;
        }
        final Iterator<Component> iterator = this.settings.iterator();
        while (iterator.hasNext()) {
            iterator.next().onUpdate();
        }
        this.updateAnimations(n, n2, n3);
        final int x = this.parent.getX();
        final int n4 = this.parent.getY() + this.offset;
        final int width = this.parent.getWidth();
        final int height = this.parent.getHeight();
        this.renderButtonBackground(drawContext, x, n4, width, height);
        this.renderIndicator(drawContext, x, n4, height);
        this.renderModuleInfo(drawContext, x, n4, width, height);
        if (this.extended) {
            this.renderSettings(drawContext, n, n2, n3);
        }
        if (this.isHovered(n, n2) && !this.parent.dragging) {
            Womp.INSTANCE.GUI.setTooltip(this.module.getDescription(), n + 10, n2 + 10);
        }
    }

    private void updateAnimations(final int n, final int n2, final float n3) {
        final float n4 = n3 * 0.05f;
        float n5;
        if (this.isHovered(n, n2) && !this.parent.dragging) {
            n5 = 1.0f;
        } else {
            n5 = 0.0f;
        }
        this.hoverAnimation = (float) MathUtil.exponentialInterpolate(this.hoverAnimation, n5, 0.05000000074505806, n4);
        float n6;
        if (this.module.isEnabled()) {
            n6 = 1.0f;
        } else {
            n6 = 0.0f;
        }
        this.enabledAnimation = (float) MathUtil.exponentialInterpolate(this.enabledAnimation, n6, 0.004999999888241291, n4);
        this.enabledAnimation = (float) MathUtil.clampValue(this.enabledAnimation, 0.0, 1.0);
    }

    private void renderButtonBackground(final DrawContext drawContext, final int n, final int n2, final int n3, final int n4) {
        final Color a = ColorUtil.a(new Color(25, 25, 30, 230), this.HOVER_COLOR, this.hoverAnimation);
        final boolean b = !this.parent.moduleButtons.isEmpty() && this.parent.moduleButtons.get(this.parent.moduleButtons.size() - 1) == this;
        if (b && !this.extended) {
            RenderUtils.renderRoundedQuad(drawContext.getMatrices(), a, n, n2, n + n3, n2 + n4, 0.0, 0.0, 6.0, 6.0, 50.0);
        } else if (b && this.extended) {
            RenderUtils.renderRoundedQuad(drawContext.getMatrices(), a, n, n2, n + n3, n2 + n4, 0.0, 0.0, 0.0, 0.0, 50.0);
        } else {
            drawContext.fill(n, n2, n + n3, n2 + n4, a.getRGB());
        }
        if (this.parent.moduleButtons.indexOf(this) > 0) {
            drawContext.fill(n + 4, n2, n + n3 - 4, n2 + 1, new Color(60, 60, 65, 100).getRGB());
        }
    }

    private void renderIndicator(final DrawContext drawContext, final int n, final int n2, final int n3) {
        Color color;
        if (this.module.isEnabled()) {
            color = Utils.getMainColor(255, Womp.INSTANCE.getModuleManager().a(this.module.getCategory()).indexOf(this.module));
        } else {
            color = this.ACCENT_COLOR;
        }
        final float n4 = 5.0f * this.enabledAnimation;
        if (n4 > 0.1f) {
            RenderUtils.renderRoundedQuad(drawContext.getMatrices(), ColorUtil.a(this.DISABLED_COLOR, color, this.enabledAnimation), n, n2 + 2, n + n4, n2 + n3 - 2, 1.5, 1.5, 1.5, 1.5, 60.0);
        }
    }

    private void renderModuleInfo(final DrawContext drawContext, final int n, final int n2, final int n3, final int n4) {
        TextRenderer.drawString(this.module.getName(), drawContext, n + 10, n2 + n4 / 2 - 6, ColorUtil.a(this.DISABLED_COLOR, this.ENABLED_COLOR, this.enabledAnimation).getRGB());
        final int n5 = n + n3 - 40;
        final int n6 = n2 + n4 / 2 - 6;
        RenderUtils.renderRoundedQuad(drawContext.getMatrices(), ColorUtil.a(new Color(60, 60, 65, 200), new Color(65, 105, 225, 100), this.enabledAnimation), n5, n6, n5 + 24.0f, n6 + 12.0f, 6.0, 6.0, 6.0, 6.0, 50.0);
        final float n7 = n5 + 6.0f + 12.0f * this.enabledAnimation;
        RenderUtils.renderCircle(drawContext.getMatrices(), ColorUtil.a(new Color(180, 180, 180), this.ENABLED_COLOR, this.enabledAnimation), n7, n6 + 6.0f, 5.0, 12);
        if (this.module.isEnabled()) {
            RenderUtils.renderCircle(drawContext.getMatrices(), new Color(this.ENABLED_COLOR.getRed(), this.ENABLED_COLOR.getGreen(), this.ENABLED_COLOR.getBlue(), 30), n7, n6 + 6.0f, 8.0, 16);
        }
    }

    private void renderSettings(final DrawContext drawContext, final int n, final int n2, final float n3) {
        this.updateSettingOffsets();
        final int n4 = this.parent.getY() + this.offset + this.parent.getHeight();
        final double animation = this.animation.getAnimation();
        RenderSystem.enableScissor(this.parent.getX(), Womp.mc.getWindow().getHeight() - (n4 + (int) animation), this.parent.getWidth(), (int) animation);
        for (Component component : this.settings) {
            if (component.setting.isVisible()) {
                component.render(drawContext, n, n2, n3);
            }
        }
        this.renderSliderControls(drawContext);
        RenderSystem.disableScissor();
    }

    private void renderSliderControls(final DrawContext drawContext) {
        for (final Component next : this.settings) {
            if (!next.setting.isVisible()) continue;
            if (next instanceof final NumberBox numberBox) {
                this.renderModernSliderKnob(drawContext, next.parentX() + Math.max(numberBox.lerpedOffsetX, 2.5), next.parentY() + numberBox.offset + next.parentOffset() + 27.5, numberBox.currentColor1);
            } else {
                if (!(next instanceof Slider)) {
                    continue;
                }
                this.renderModernSliderKnob(drawContext, next.parentX() + Math.max(((Slider) next).lerpedOffsetMinX, 2.5), next.parentY() + next.offset + next.parentOffset() + 27.5, ((Slider) next).accentColor1);
                this.renderModernSliderKnob(drawContext, next.parentX() + Math.max(((Slider) next).lerpedOffsetMaxX, 2.5), next.parentY() + next.offset + next.parentOffset() + 27.5, ((Slider) next).accentColor1);
            }
        }
    }

    private void renderModernSliderKnob(final DrawContext drawContext, final double n, final double n2, final Color color) {
        RenderUtils.renderCircle(drawContext.getMatrices(), new Color(0, 0, 0, 100), n, n2, 7.0, 18);
        RenderUtils.renderCircle(drawContext.getMatrices(), color, n, n2, 5.5, 16);
        RenderUtils.renderCircle(drawContext.getMatrices(), new Color(255, 255, 255, 70), n, n2 - 1.0, 3.0, 12);
    }

    public void onExtend() {
        final Iterator<ModuleButton> iterator = this.parent.moduleButtons.iterator();
        while (iterator.hasNext()) {
            iterator.next().extended = false;
        }
    }

    public void keyPressed(final int keyCode, final int scanCode, final int modifiers) {
        if(this.extended && settings != null) {
            for(Component c : settings) {
                if(c != null && c.setting != null && c.setting.isVisible()) {
                    try {
                        c.keyPressed(keyCode, scanCode, modifiers);
                    } catch (Exception e) {
                        System.out.println("[ERROR] Error in Component.keyPressed: " + e.getMessage());
                    }
                }
            }
        }
    }

    public void mouseDragged(final double n, final double n2, final int n3, final double n4, final double n5) {
        if (this.extended) {
            final Iterator<Component> iterator = this.settings.iterator();
            while (iterator.hasNext()) {
                Component c = iterator.next();
                if(c.setting.isVisible()) {
                    c.mouseDragged(n, n2, n3, n4, n5);
                }
            }
        }
    }

    public void mouseClicked(final double n, final double n2, final int button) {
        if (this.isHovered(n, n2)) {
            if (button == 0) {
                final int n4 = this.parent.getX() + this.parent.getWidth() - 30;
                final int n5 = this.parent.getY() + this.offset + this.parent.getHeight() / 2 - 3;

                if (n >= n4 && n <= n4 + 12 && n2 >= n5 && n2 <= n5 + 6) {
                    this.module.toggle();
                } else if (!this.module.getSettings().isEmpty() && n > this.parent.getX() + this.parent.getWidth() - 25) {
                    if (!this.extended) {
                        this.onExtend();
                    }
                    this.extended = !this.extended;
                } else {
                    this.module.toggle();
                }
            } else if (button == 1) {
                if (this.module.getSettings().isEmpty()) {
                    return;
                }
                if (!this.extended) {
                    this.onExtend();
                }
                this.extended = !this.extended;
            }
        }
        if (this.extended) {
            for (Component setting : this.settings) {
                if(setting.setting.isVisible()) {
                    setting.mouseClicked(n, n2, button);
                }
            }
        }
    }

    public void onGuiClose() {
        this.currentAlpha = null;
        this.currentColor = null;
        this.hoverAnimation = 0.0f;
        float enabledAnimation;
        if (this.module.isEnabled()) {
            enabledAnimation = 1.0f;
        } else {
            enabledAnimation = 0.0f;
        }
        this.enabledAnimation = enabledAnimation;
        final Iterator<Component> iterator = this.settings.iterator();
        while (iterator.hasNext()) {
            iterator.next().onGuiClose();
        }
    }

    public void mouseReleased(final double n, final double n2, final int n3) {
        if (this.extended) {
            final Iterator<Component> iterator = this.settings.iterator();
            while (iterator.hasNext()) {
                Component c = iterator.next();
                if(c.setting.isVisible()) {
                    c.mouseReleased(n, n2, n3);
                }
            }
        }
    }

    public boolean isHovered(final double n, final double n2) {
        return n > this.parent.getX() && n < this.parent.getX() + this.parent.getWidth() && n2 > this.parent.getY() + this.offset && n2 < this.parent.getY() + this.offset + this.parent.getHeight();
    }

    public void charTyped(char c, int modifiers) {
        if (this.extended) {
            for (Component component : this.settings) {
                if (component.setting.isVisible()) {
                    component.charTyped(c, modifiers);
                }
            }
        }
    }

    public void refresh() {
        for (Component component : this.settings) {
            if (component != null) {
                component.onUpdate();
            }
        }
    }

    public void updateSettingOffsets() {
        int currentOffset = this.parent.getHeight();
        for (Component component : this.settings) {
            if (component.setting.isVisible()) {
                component.offset = currentOffset;
                currentOffset += this.parent.getHeight();
            }
        }
        this.settingOffset = currentOffset;
    }
}