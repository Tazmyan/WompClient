package taz.womp.mixin;

import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import taz.womp.Womp;
import taz.womp.imixin.IKeybinding;

@Mixin({KeyBinding.class})
public abstract class KeyBindingMixin implements IKeybinding {
    @Shadow
    private InputUtil.Key boundKey;

    @Shadow public abstract void setPressed(boolean pressed);

    @Override
    public boolean womp$isActuallyPressed() {
        return InputUtil.isKeyPressed(Womp.mc.getWindow().getHandle(), this.boundKey.getCode());
    }

    @Override
    public void womp$resetPressed() {
        this.setPressed(this.womp$isActuallyPressed());
    }
}