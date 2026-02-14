package taz.womp.module.setting;

public class StringSetting extends Setting {
    public String value;

    public StringSetting(final CharSequence charSequence, final String a) {
        super(charSequence);
        this.value = a;
    }

    public void setValue(final String a) {
        this.value = a;
        onValueChanged();
    }

    public String getValue() {
        return this.value;
    }

    public StringSetting setDescription(final CharSequence charSequence) {
        super.setDescription(charSequence);
        return this;
    }
}
