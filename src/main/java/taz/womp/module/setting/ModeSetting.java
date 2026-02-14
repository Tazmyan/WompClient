package taz.womp.module.setting;

import java.util.Arrays;
import java.util.List;

public final class ModeSetting<T extends Enum<T>> extends Setting {
    public int index;
    private final List<T> possibleValues;
    private final int originalValue;
    private List<String> stringOptions = null;
    private int stringIndex = 0;

    public ModeSetting(final CharSequence charSequence, final T defaultValue, final T[] values) {
        super(charSequence != null ? charSequence : "Mode");
        
        if (values == null || values.length == 0) {
            throw new RuntimeException("No enum values provided");
        }
        
        this.possibleValues = Arrays.asList(values);
        

        if (defaultValue != null) {
            this.index = this.possibleValues.indexOf(defaultValue);
            if (this.index == -1) {
                this.index = 0;
            }
        } else {
            this.index = 0;
        }
        
        this.originalValue = this.index;
    }

    public ModeSetting(final CharSequence charSequence, final String[] options, int defaultIndex) {
        super(charSequence != null ? charSequence : "Mode");
        if (options == null || options.length == 0) {
            throw new RuntimeException("No options provided");
        }
        this.stringOptions = Arrays.asList(options);
        this.stringIndex = (defaultIndex >= 0 && defaultIndex < options.length) ? defaultIndex : 0;
        this.possibleValues = null;
        this.index = -1;
        this.originalValue = this.stringIndex;
    }

    public ModeSetting(final CharSequence charSequence, boolean defaultOn) {
        this(charSequence, new String[]{"Off", "On"}, defaultOn ? 1 : 0);
    }

    public Enum<T> getValue() {
        return this.possibleValues.get(this.index);
    }

    public void setMode(final Enum<T> enum1) {
        this.index = this.possibleValues.indexOf(enum1);
        onValueChanged();
    }

    public void setModeIndex(final int a) {
        this.index = a;
        onValueChanged();
    }

    public int getModeIndex() {
        return this.index;
    }

    public int getOriginalValue() {
        return this.originalValue;
    }

    public void cycleUp() {
        if (this.index < this.possibleValues.size() - 1) {
            ++this.index;
        } else {
            this.index = 0;
        }
        onValueChanged();
    }

    public void cycleDown() {
        if (this.index > 0) {
            --this.index;
        } else {
            this.index = this.possibleValues.size() - 1;
        }
        onValueChanged();
    }

    public boolean isMode(final Enum<T> enum1) {
        return this.index == this.possibleValues.indexOf(enum1);
    }

    public List<T> getPossibleValues() {
        return this.possibleValues;
    }

    public ModeSetting<T> setDescription(final CharSequence charSequence) {
        super.setDescription(charSequence);
        return this;
    }

    public String getStringValue() {
        return stringOptions != null ? stringOptions.get(stringIndex) : null;
    }

    public void setStringIndex(int idx) {
        if (stringOptions != null && idx >= 0 && idx < stringOptions.size()) {
            this.stringIndex = idx;
            onValueChanged();
        }
    }

    public int getStringIndex() {
        return stringIndex;
    }

    public void cycleStringUp() {
        if (stringOptions != null) {
            if (stringIndex < stringOptions.size() - 1) {
                ++stringIndex;
            } else {
                stringIndex = 0;
            }
            onValueChanged();
        }
    }

    public void cycleStringDown() {
        if (stringOptions != null) {
            if (stringIndex > 0) {
                --stringIndex;
            } else {
                stringIndex = stringOptions.size() - 1;
            }
            onValueChanged();
        }
    }

    public boolean isStringMode(String value) {
        return stringOptions != null && stringOptions.get(stringIndex).equals(value);
    }

    public List<String> getStringOptions() {
        return stringOptions;
    }
}