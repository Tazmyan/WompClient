package taz.womp.event.events;

import net.minecraft.client.gui.DrawContext;
import taz.womp.event.CancellableEvent;

public class HudEvent extends CancellableEvent {
    public final DrawContext context;
    public final float delta;

    public HudEvent(DrawContext context, float delta) {
        this.context = context;
        this.delta = delta;
    }
} 