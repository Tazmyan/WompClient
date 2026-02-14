package taz.womp.event.events;

import net.minecraft.entity.Entity;
import taz.womp.event.CancellableEvent;

public class AttackEvent extends CancellableEvent {
    public Entity target;
    
    public AttackEvent(Entity target) {
        this.target = target;
    }
}