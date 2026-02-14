package taz.womp.gui.components;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.util.math.MathHelper;
import org.lwjgl.glfw.GLFW;
import taz.womp.gui.Component;
import taz.womp.module.setting.NumberSetting;
import taz.womp.module.setting.Setting;
import taz.womp.utils.*;

import java.awt.*;

public final class NumberBox extends Component {
    public boolean dragging;
    public double offsetX;
    public double lerpedOffsetX;
    private float hoverAnimation;
    private final NumberSetting setting;
    public Color currentColor1;
    private boolean isEditing = false;
    private String currentText = "";
    private long lastCursorBlink = 0;
    private boolean cursorVisible = true;
    private final Color TEXT_COLOR;
    private final Color HOVER_COLOR;
    private final Color TRACK_BG_COLOR;

    public NumberBox(final ModuleButton moduleButton, final Setting setting, final int n) {
        super(moduleButton, setting, n);
        this.lerpedOffsetX = 0.0;
        this.hoverAnimation = 0.0f;
        this.TEXT_COLOR = new Color(230, 230, 230);
        this.HOVER_COLOR = new Color(255, 255, 255, 20);
        this.TRACK_BG_COLOR = new Color(60, 60, 65);
        this.setting = (NumberSetting) setting;
    }

    @Override
    public void onUpdate() {
        final Color mainColor = Utils.getMainColor(255, this.parent.settings.indexOf(this));
        if (this.currentColor1 == null) {
            this.currentColor1 = new Color(mainColor.getRed(), mainColor.getGreen(), mainColor.getBlue(), 0);
        } else {
            this.currentColor1 = new Color(mainColor.getRed(), mainColor.getGreen(), mainColor.getBlue(), this.currentColor1.getAlpha());
        }
        if (this.currentColor1.getAlpha() != 255) {
            this.currentColor1 = ColorUtil.a(0.05f, 255, this.currentColor1);
        }
        super.onUpdate();
    }

    @Override
    public void render(final DrawContext drawContext, final int n, final int n2, final float n3) {
        super.render(drawContext, n, n2, n3);
        this.updateAnimations(n, n2, n3);
        this.offsetX = (this.setting.getValue() - this.setting.getMin()) / (this.setting.getMax() - this.setting.getMin()) * this.parentWidth();
        this.lerpedOffsetX = MathUtil.approachValue((float) (0.5 * n3), this.lerpedOffsetX, this.offsetX);
        if (!this.parent.parent.dragging) {
            drawContext.fill(this.parentX(), this.parentY() + this.parentOffset() + this.offset, this.parentX() + this.parentWidth(), this.parentY() + this.parentOffset() + this.offset + this.parentHeight(), new Color(this.HOVER_COLOR.getRed(), this.HOVER_COLOR.getGreen(), this.HOVER_COLOR.getBlue(), (int) (this.HOVER_COLOR.getAlpha() * this.hoverAnimation)).getRGB());
        }
        final int n4 = this.parentY() + this.offset + this.parentOffset() + 25;
        final int n5 = this.parentX() + 5;
        RenderUtils.renderRoundedQuad(drawContext.getMatrices(), this.TRACK_BG_COLOR, n5, n4, n5 + (this.parentWidth() - 10), n4 + 4.0f, 2.0, 2.0, 2.0, 2.0, 50.0);
        if (this.lerpedOffsetX > 2.5) {
            RenderUtils.renderRoundedQuad(drawContext.getMatrices(), this.currentColor1, n5, n4, n5 + Math.max(this.lerpedOffsetX - 5.0, 0.0), n4 + 4.0f, 2.0, 2.0, 2.0, 2.0, 50.0);
        }
        final String displayValue = this.getDisplayValue();
        TextRenderer.drawString(this.setting.getName(), drawContext, this.parentX() + 5, this.parentY() + this.parentOffset() + this.offset + 9, this.TEXT_COLOR.getRGB());
        if (isEditing) {
            if (System.currentTimeMillis() - lastCursorBlink > 500) {
                cursorVisible = !cursorVisible;
                lastCursorBlink = System.currentTimeMillis();
            }
            String textToRender = currentText + (cursorVisible ? "|" : "");
            TextRenderer.drawString(textToRender, drawContext, this.parentX() + this.parentWidth() - TextRenderer.getWidth(textToRender) - 5, this.parentY() + this.parentOffset() + this.offset + 9, this.currentColor1.getRGB());
        } else {
            TextRenderer.drawString(displayValue, drawContext, this.parentX() + this.parentWidth() - TextRenderer.getWidth(displayValue) - 5, this.parentY() + this.parentOffset() + this.offset + 9, this.currentColor1.getRGB());
        }
    }

    private void updateAnimations(final int n, final int n2, final float n3) {
        float n4;
        if (this.isHovered(n, n2) && !this.parent.parent.dragging) {
            n4 = 1.0f;
        } else {
            n4 = 0.0f;
        }
        this.hoverAnimation = (float) MathUtil.exponentialInterpolate(this.hoverAnimation, n4, 0.25, n3 * 0.05f);
    }

    private String getDisplayValue() {
        final double a = this.setting.getValue();
        final double c = this.setting.getFormat();

        if (c < 1.0) {
            int decimalPlaces = (int) Math.round(Math.log10(1 / c));
            return String.format("%." + decimalPlaces + "f", a);
        }

        if (c >= 1.0) {
            return String.format("%.0f", a);
        }
        return String.valueOf(a);
    }

    @Override
    public void onGuiClose() {
        this.currentColor1 = null;
        this.hoverAnimation = 0.0f;
        super.onGuiClose();
    }

    private void slide(final double n) {
        this.setting.getValue(MathUtil.roundToNearest(MathHelper.clamp((n - (this.parentX() + 5)) / (this.parentWidth() - 10), 0.0, 1.0) * (this.setting.getMax() - this.setting.getMin()) + this.setting.getMin(), this.setting.getFormat()));
    }

    @Override
    public void keyPressed(int keyCode, int scanCode, int modifiers) {
        if (isEditing) {
            if (keyCode == GLFW.GLFW_KEY_ENTER || keyCode == GLFW.GLFW_KEY_KP_ENTER) {
                try {
                    double newValue = Double.parseDouble(currentText);
                    if (newValue >= setting.getMin() && newValue <= setting.getMax()) {
                        setting.getValue(newValue);
                    }
                } catch (NumberFormatException e) {
                }
                isEditing = false;
                currentText = "";
                Component.focusedComponent = null;
            } else if (keyCode == GLFW.GLFW_KEY_BACKSPACE) {
                if (!currentText.isEmpty()) {
                    currentText = currentText.substring(0, currentText.length() - 1);
                }
            } else if (keyCode == GLFW.GLFW_KEY_ESCAPE) {
                isEditing = false;
                currentText = "";
                Component.focusedComponent = null;
            }
        }

        if (modifiers == GLFW.GLFW_MOD_CONTROL && keyCode == GLFW.GLFW_KEY_R) {
            if (isHovered(mc.mouse.getX(), mc.mouse.getY())) {
                setting.getValue(setting.getDefaultValue());
            }
        }
    }

    @Override
    public void charTyped(char c, int modifiers) {
        if (isEditing) {
            if (Character.isDigit(c) || c == '.' || c == '-') {
                currentText += c;
            }
        }
    }

    @Override
    public void mouseClicked(final double n, final double n2, final int n3) {
        if (this.isHovered(n, n2)) {
            if (n3 == 0) {
                this.dragging = true;
            } else if (n3 == 1) {
                if (Component.focusedComponent != null && Component.focusedComponent != this) {
                    Component.focusedComponent.stopEditing();
                }

                isEditing = !isEditing;
                currentText = isEditing ? getDisplayValue() : "";

                if(isEditing) {
                    Component.focusedComponent = this;
                    lastCursorBlink = System.currentTimeMillis();
                    cursorVisible = true;
                } else {
                    Component.focusedComponent = null;
                }
            }
        }
        super.mouseClicked(n, n2, n3);
    }

    @Override
    public void mouseReleased(final double n, final double n2, final int n3) {
        if (n3 == 0) {
            this.dragging = false;
        }
        super.mouseReleased(n, n2, n3);
    }

    @Override
    public void mouseDragged(final double n, final double n2, final int n3, final double n4, final double n5) {
        if (this.dragging) {
            this.slide(n);
        }
        super.mouseDragged(n, n2, n3, n4, n5);
    }

    @Override
    public void stopEditing() {
        isEditing = false;
        currentText = "";
        Component.focusedComponent = null;
    }
}
