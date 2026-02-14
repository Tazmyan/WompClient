package taz.womp.mixin;

import net.minecraft.client.render.entity.EntityRenderDispatcher;
import net.minecraft.util.math.Box;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import taz.womp.Womp;
import taz.womp.module.Module;
import taz.womp.module.modules.combat.Hitbox;
import taz.womp.module.modules.combat.StaticHitboxes;

@Mixin({EntityRenderDispatcher.class})
public class EntityRenderDispatcherMixin {
    @ModifyVariable(method = {"renderHitbox"}, ordinal = 0, at = @At(value = "STORE", ordinal = 0))
    private static Box onRenderHitboxEditBox(final Box box) {
        final Module hitboxes = Womp.INSTANCE.MODULE_MANAGER.getModuleByClass(Hitbox.class);
        final Module staticHitboxes = Womp.INSTANCE.MODULE_MANAGER.getModuleByClass(StaticHitboxes.class);
        boolean hitboxRender = hitboxes.isEnabled() && ((Hitbox) hitboxes).getEnableRender().getValue();
        boolean staticHitboxRender = staticHitboxes.isEnabled() && ((StaticHitboxes) staticHitboxes).getEnableRender().getValue();
        if (hitboxRender) {
            return box.expand(((Hitbox) hitboxes).getHitboxExpansion());
        } else if (staticHitboxRender) {


            return box;
        } else {
            return box;
        }
    }
}