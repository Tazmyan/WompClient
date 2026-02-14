package taz.womp.event.events;

import taz.womp.event.CancellableEvent;

public class MouseScrolledEvent extends CancellableEvent {
    public double amount;

    public MouseScrolledEvent(final double amount) {
        this.amount = amount;
    }
}