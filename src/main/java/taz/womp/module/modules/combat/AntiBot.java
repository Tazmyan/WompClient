package taz.womp.module.modules.combat;

import net.minecraft.client.network.PlayerListEntry;
import net.minecraft.entity.player.PlayerEntity;
import taz.womp.Womp;
import taz.womp.event.EventListener;
import taz.womp.event.events.TickEvent;
import taz.womp.module.Category;
import taz.womp.module.Module;
import taz.womp.module.setting.BooleanSetting;
import taz.womp.module.setting.NumberSetting;
import taz.womp.utils.EncryptedString;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

public final class AntiBot extends Module {
    private final BooleanSetting tabListCheck = new BooleanSetting(EncryptedString.of("Tab List Check"), true);
    private final BooleanSetting teamCheck = new BooleanSetting(EncryptedString.of("Team Check"), true);
    private final BooleanSetting skinCheck = new BooleanSetting(EncryptedString.of("Skin Check"), false);
    private final NumberSetting ticksExisted = new NumberSetting(EncryptedString.of("Ticks Existed"), 0.0, 100.0, 20.0, 1.0);

    private static final Set<UUID> bots = new HashSet<>();

    public AntiBot() {
        super(EncryptedString.of("AntiBot"), EncryptedString.of("Detects and avoids attacking bots"), -1, Category.COMBAT);
        this.addSettings(this.tabListCheck, this.teamCheck, this.skinCheck, this.ticksExisted);
    }

    public static boolean isABot(PlayerEntity player) {
        if (player == null) return true;
        if (player.getUuid().version() != 4) return true; 

        AntiBot antiBot = (AntiBot) Womp.INSTANCE.getModuleManager().getModuleByClass(AntiBot.class);
        if (antiBot == null || !antiBot.isEnabled()) return false;

        return antiBot.isBot(player);
    }

    @Override
    public void onEnable() {
        bots.clear();
        super.onEnable();
    }

    @EventListener
    public void onTick(TickEvent event) {
        if (mc.world == null) return;
        bots.clear();
        for (PlayerEntity player : mc.world.getPlayers()) {
            if (isBot(player)) {
                bots.add(player.getUuid());
            }
        }
    }

    public boolean isBot(PlayerEntity player) {
        if (player == mc.player) return false;

        if (tabListCheck.getValue()) {
            PlayerListEntry entry = mc.getNetworkHandler().getPlayerListEntry(player.getUuid());
            if (entry == null) {
                return true;
            }
        }

        if (teamCheck.getValue()) {
            if (player.getScoreboardTeam() == null) {
                return true;
            }
        }

        if (skinCheck.getValue()) {
            PlayerListEntry entry = mc.getNetworkHandler().getPlayerListEntry(player.getUuid());
            if (entry == null || !entry.getSkinTextures().texture().toString().startsWith("http://textures.minecraft.net/texture/")) {
                return true;
            }
        }

        if (player.age < ticksExisted.getIntValue()) {
            return true;
        }

        return false;
    }
} 