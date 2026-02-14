package taz.womp.mixin;

import net.minecraft.client.Keyboard;
import org.lwjgl.glfw.GLFW;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import taz.womp.event.events.KeyEvent;
import taz.womp.manager.EventManager;

@Mixin({Keyboard.class})
public class KeyboardMixin {
    @Inject(method = {"onKey"}, at = {@At("HEAD")}, cancellable = true)
    private void onPress(final long window, final int key, final int scancode, final int action, final int modifiers, final CallbackInfo ci) {
        if (key == GLFW.GLFW_KEY_UNKNOWN) return;
        final KeyEvent event = new KeyEvent(key, window, action);
        EventManager.b(event);
        if (event.isCancelled()) ci.cancel();
    }
}