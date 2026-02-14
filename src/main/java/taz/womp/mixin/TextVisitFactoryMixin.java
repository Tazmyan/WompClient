package taz.womp.mixin;

import net.minecraft.text.TextVisitFactory;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import taz.womp.Womp;
import taz.womp.module.modules.misc.NameProtect;

@Mixin({TextVisitFactory.class})
public class TextVisitFactoryMixin {
    @ModifyArg(at = @At(value = "INVOKE", target = "Lnet/minecraft/text/TextVisitFactory;visitFormatted(Ljava/lang/String;ILnet/minecraft/text/Style;Lnet/minecraft/text/Style;Lnet/minecraft/text/CharacterVisitor;)Z", ordinal = 0), method = {"visitFormatted(Ljava/lang/String;ILnet/minecraft/text/Style;Lnet/minecraft/text/CharacterVisitor;)Z"}, index = 0)
    private static String adjustText(final String s) {
        final NameProtect nameprotect = (NameProtect) Womp.INSTANCE.MODULE_MANAGER.getModuleByClass(NameProtect.class);
        return nameprotect.isEnabled() && s.contains(Womp.mc.getSession().getUsername()) ? s.replace(Womp.mc.getSession().getUsername(), nameprotect.getFakeName()) : s;
    }
}