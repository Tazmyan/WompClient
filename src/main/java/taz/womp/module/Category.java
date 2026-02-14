package taz.womp.module;

import taz.womp.utils.EncryptedString;

public enum Category {
    COMBAT(EncryptedString.of("Combat")),
    MOVEMENT(EncryptedString.of("Movement")),
    MISC(EncryptedString.of("Misc")),
    RENDER(EncryptedString.of("Render")),
    CLIENT(EncryptedString.of("Client"));

    public final CharSequence name;

    Category(final CharSequence name) {
        this.name = name;
    }
}
