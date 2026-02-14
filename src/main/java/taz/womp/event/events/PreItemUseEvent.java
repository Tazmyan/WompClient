package taz.womp.event.events;

import taz.womp.event.CancellableEvent;

public class PreItemUseEvent extends CancellableEvent {
    public int cooldown;

    public PreItemUseEvent(final int cooldown) {
        this.cooldown = cooldown;
    }
}