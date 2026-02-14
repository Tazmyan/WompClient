package taz.womp.event.events;

import net.minecraft.client.gui.screen.Screen;
import taz.womp.event.CancellableEvent;

public class SetScreenEvent extends CancellableEvent {
    public Screen screen;

    public SetScreenEvent(final Screen screen) {
        this.screen = screen;
    }
}