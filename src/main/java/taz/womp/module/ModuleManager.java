package taz.womp.module;

import net.minecraft.client.gui.screen.ChatScreen;
import net.minecraft.client.gui.screen.TitleScreen;
import net.minecraft.client.gui.screen.multiplayer.MultiplayerScreen;
import net.minecraft.client.gui.screen.world.SelectWorldScreen;
import taz.womp.Womp;
import taz.womp.event.EventListener;
import taz.womp.event.events.KeyEvent;
import taz.womp.module.modules.client.SelfDestruct;
import taz.womp.module.modules.client.WompModule;
import taz.womp.module.modules.combat.*;
import taz.womp.module.modules.misc.*;
import taz.womp.module.modules.movement.*;
import taz.womp.module.modules.render.*;
import taz.womp.module.setting.BindSetting;
import taz.womp.utils.EncryptedString;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public final class ModuleManager {
    private final List<Module> modules;

    public ModuleManager() {
        this.modules = new ArrayList<>();
        this.a();
    }

    public void a() {
        this.a(new AimAssist());
        this.a(new Triggerbot());
        this.a(new HitSelection());
        this.a(new KillAura());
        this.a(new Backtrack());
        this.a(new LagRange());
        this.a(new ElytraSwap());
        this.a(new MaceSwap());
        this.a(new Hitbox());
        this.a(new StaticHitboxes());
        this.a(new AntiBot());
        this.a(new AntiWeb());
        this.a(new AutoWeb());
        this.a(new AutoTotem());
        this.a(new HoverTotem());
        this.a(new AutoInventoryTotem());
        this.a(new AnchorMacro());
        this.a(new AutoCrystal());
        this.a(new DoubleAnchor());
        this.a(new WTap());
        this.a(new NoMissDelay());
        this.a(new Airstuck());
        this.a(new FastPlace());
        this.a(new Freecam());
        this.a(new AutoFirework());
        this.a(new ElytraGlide());
        this.a(new AutoTool());
        this.a(new AutoEat());
        this.a(new AutoMine());
        this.a(new KeyPearl());
        this.a(new NameProtect());
        this.a(new HUD());
        this.a(new PlayerESP());
        this.a(new StorageESP());
        this.a(new TargetHUD());
        this.a(new Keybinds());
        this.a(new WompModule());
        this.a(new SelfDestruct());
    }

    public List<Module> b() {
        return this.modules.stream().filter(Module::isEnabled).toList();
    }

    public List<Module> c() {
        return this.modules;
    }

    private void d() {
        Womp.INSTANCE.getEventBus().register(this);
        for (final Module next : this.modules) {
            next.addSetting(new BindSetting(EncryptedString.of("Keybind"), next.getKeybind(), true).setDescription(EncryptedString.of("Key to enabled the module")));
        }
    }

    public void init() {
        this.d();
        for (Module module : this.modules) {
            module.setEventBus(Womp.INSTANCE.getEventBus());
            Womp.INSTANCE.getEventBus().register(module);
        }
    }

    public List<Module> a(final Category category) {
        return this.modules.stream().filter(module -> module.getCategory() == category).toList();
    }

    public Module getModuleByClass(final Class<? extends Module> obj) {
        Objects.requireNonNull(obj);
        return this.modules.stream().filter(obj::isInstance).findFirst().orElse(null);
    }

    public void a(final Module module) {
        this.modules.add(module);
    }

    @EventListener
    public void a(final KeyEvent keyEvent) {
        if (Womp.mc.currentScreen instanceof ChatScreen) {
            return;
        }

        if (Womp.mc.player == null) {
			if (!(Womp.mc.currentScreen instanceof TitleScreen) && !(Womp.mc.currentScreen instanceof SelectWorldScreen) && !(Womp.mc.currentScreen instanceof MultiplayerScreen) && !(Womp.mc.currentScreen instanceof taz.womp.gui.ClickGUI)) {
                return;
            }
        }




		Module wompModule = this.getModuleByClass(WompModule.class);
		if (wompModule != null && keyEvent.mode == 1 && keyEvent.key == wompModule.getKeybind()) {
			if (Womp.mc.currentScreen instanceof taz.womp.gui.ClickGUI) {

				Womp.mc.setScreenAndRender(null);
				wompModule.toggle(false);
			} else {

				wompModule.toggle(true);
			}
			return;
		}

        if (!SelfDestruct.isActive) {
            this.modules.forEach(module -> {
                if (module.getKeybind() == keyEvent.key && keyEvent.mode == 1) {
                    module.toggle();
                }
            });
        }
    }
}
