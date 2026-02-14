package taz.womp.event;

public interface Event {
    default boolean isCancelled() {
        return false;
    }
}