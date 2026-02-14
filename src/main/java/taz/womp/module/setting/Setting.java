package taz.womp.module.setting;

import java.util.function.BooleanSupplier;

public abstract class Setting {
    private CharSequence name;
    public CharSequence description;
    private Runnable onChangeCallback;
    private BooleanSupplier visibilityCondition;
    private boolean hidden = false;

    public Setting(final CharSequence a) {
        this.name = a != null ? a : "Setting";
        this.visibilityCondition = () -> true;
    }

    public void getDescription(final CharSequence a) {
        this.description = a != null ? a : "Setting";
    }

    public CharSequence getName() {
        return this.name;
    }

    public CharSequence getDescription() {
        return this.description;
    }

    public Setting setDescription(final CharSequence description) {
        this.description = description;
        return this;
    }

    public void setOnChangeCallback(Runnable callback) {
        this.onChangeCallback = callback;
    }

    public Setting setVisibility(BooleanSupplier condition) {
        this.visibilityCondition = condition;
        return this;
    }

    public boolean isVisible() {
        return visibilityCondition.getAsBoolean();
    }

    protected void onValueChanged() {
        if (onChangeCallback != null) {
            onChangeCallback.run();
        }
    }

    public void setHidden(boolean hidden) { this.hidden = hidden; }
    public boolean isHidden() { return hidden; }
}