package taz.womp.module.modules.combat;

import net.minecraft.entity.Entity;
import net.minecraft.entity.decoration.EndCrystalEntity;
import net.minecraft.entity.mob.MagmaCubeEntity;
import net.minecraft.entity.mob.SlimeEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.AxeItem;
import net.minecraft.item.Item;
import net.minecraft.item.SwordItem;
import net.minecraft.util.hit.EntityHitResult;
import net.minecraft.util.hit.HitResult;
import org.lwjgl.glfw.GLFW;
import taz.womp.Womp;
import taz.womp.event.EventListener;
import taz.womp.event.events.AttackEvent;
import taz.womp.event.events.TickEvent;
import taz.womp.module.Category;
import taz.womp.module.Module;
import taz.womp.module.setting.BooleanSetting;
import taz.womp.module.setting.MinMaxSetting;
import taz.womp.module.setting.ModeSetting;
import taz.womp.utils.EncryptedString;
import taz.womp.utils.TimerUtils;
import taz.womp.utils.WorldUtils;
import net.minecraft.util.math.BlockPos;

public final class Triggerbot extends Module {
    private final BooleanSetting onLeftClick = new BooleanSetting(EncryptedString.of("On Left Click"), false);
    private final BooleanSetting checkShield = new BooleanSetting(EncryptedString.of("Check Shield"), false);
    private final BooleanSetting onlyWeapon = new BooleanSetting(EncryptedString.of("Only Weapon"), true);
    private final BooleanSetting targetPlayers = new BooleanSetting(EncryptedString.of("Target Players"), true);
    private final BooleanSetting targetCrystals = new BooleanSetting(EncryptedString.of("Target Crystals"), true);
    private final BooleanSetting targetMobs = new BooleanSetting(EncryptedString.of("Target Mobs"), false);
    private final BooleanSetting realCPS = new BooleanSetting(EncryptedString.of("Real CPS"), true);
    private final MinMaxSetting cooldownPercent = new MinMaxSetting(EncryptedString.of("Cooldown"), 0, 100, 1, 80, 100);
    private final ModeSetting<AttackMode> attackMode = new ModeSetting<>(
        EncryptedString.of("Attack Mode"),
        AttackMode.Cooldown,
        AttackMode.values()
    );
    private final MinMaxSetting reactionTime = new MinMaxSetting(EncryptedString.of("Reaction Time"), 0, 250, 1, 0, 5);
    private final MinMaxSetting attackDelay = new MinMaxSetting(EncryptedString.of("Attack Delay"), 0, 1000, 1, 10, 25);
    private final ModeSetting<CritMode> critMode = new ModeSetting<>(
        EncryptedString.of("Criticals"),
        CritMode.Simple,
        CritMode.values()
    );
    private final TimerUtils timer = new TimerUtils();
    private final TimerUtils hitTimer = new TimerUtils();
    private final TimerUtils reactionTimer = new TimerUtils();
    private boolean sprintDesynced = false;

    private enum CritMode {
        Off, Simple, Smart, Desync
    }

    private enum AttackMode {
        Cooldown, Delay
    }

    public Triggerbot() {
        super(EncryptedString.of("Triggerbot"), EncryptedString.of("Automatically attacks entities you look at"), -1, Category.COMBAT);
        this.addSettings(
                onLeftClick, checkShield, onlyWeapon, targetPlayers, targetCrystals, targetMobs, realCPS, attackMode,cooldownPercent, attackDelay, reactionTime, critMode
        );
        attackDelay.setVisibility(() -> attackMode.getValue() == AttackMode.Delay);
        cooldownPercent.setVisibility(() -> attackMode.getValue() == AttackMode.Cooldown);
    }

    @Override
    public void onEnable() {
        super.onEnable();
        timer.reset();
    }

    @Override
    public void onDisable() {
        super.onDisable();
    }

    @EventListener
    public void onTick(final TickEvent event) {
        if (mc.currentScreen != null) return;
        if (mc.player == null || mc.world == null) return;
        if (onLeftClick.getValue() && GLFW.glfwGetMouseButton(mc.getWindow().getHandle(), GLFW.GLFW_MOUSE_BUTTON_LEFT) != GLFW.GLFW_PRESS) return;
        if (mc.player.isUsingItem()) return; 
        if (!mc.player.isOnGround() && mc.player.getVelocity().y > 0) return;

        Item item = mc.player.getMainHandStack().getItem();

        if (onlyWeapon.getValue() && !(item instanceof SwordItem || item instanceof AxeItem)) {
            return;
        }

        Entity target = null;
        if (mc.crosshairTarget instanceof EntityHitResult hit && mc.crosshairTarget.getType() == HitResult.Type.ENTITY) {
            Entity entity = hit.getEntity();
            if (!isValidTarget(entity)) return;
            target = entity;
        }
        if (target == null) {
            reactionTimer.reset();
            return;
        }

        float cooldownProgress = mc.player.getAttackCooldownProgress(0.5F);
        AttackMode currentAttackMode = (AttackMode) attackMode.getValue();
        if (currentAttackMode == AttackMode.Cooldown) {
            if (cooldownProgress < (cooldownPercent.getRandomFloatInRange() / 100f)) {
                hitTimer.reset();
                return;
            }
        }
        if (currentAttackMode == AttackMode.Delay) {
            if (!hitTimer.hasReached(getRandomAttackDelay())) return;
        }

        if (!reactionTimer.hasReached(getRandomReactionTime())) return;

        boolean canCrit = false;
        CritMode critModeValue = (CritMode) critMode.getValue();
        boolean inCobweb = isInCobweb();
        switch (critModeValue) {
            case Simple:
                canCrit = taz.womp.utils.WorldUtils.isCrit(mc.player, target);
                break;
            case Smart:
                if (sprintDesynced || mc.player.isFallFlying() || inCobweb) {
                    canCrit = true;
                } else {
                    if (!mc.options.forwardKey.isPressed() && mc.player.hurtTime != 0) {
                        canCrit = mc.player.fallDistance > 0;
                    } else {
                        canCrit = mc.player.fallDistance > 0 && !mc.player.isOnGround();
                    }
                }
                break;
            case Desync:
                if (sprintDesynced) {
                    canCrit = true;
                } else {
                    canCrit = canCriticalHit() && !mc.player.isOnGround();
                }
                break;
            default:
                canCrit = false;
        }
        boolean canAttack = (currentAttackMode == AttackMode.Delay || cooldownProgress > (cooldownPercent.getRandomFloatInRange() / 100f)) || (critModeValue != CritMode.Off && canCrit);
        if (canAttack) {
            WorldUtils.hitEntity(target, true);
            if (realCPS.getValue()) {
                taz.womp.utils.MouseSimulation.mouseClick(GLFW.GLFW_MOUSE_BUTTON_LEFT);
            }
            hitTimer.reset();
        }
    }

    @EventListener
    public void onAttack(final AttackEvent event) {
        if (onLeftClick.getValue() && GLFW.glfwGetMouseButton(mc.getWindow().getHandle(), GLFW.GLFW_MOUSE_BUTTON_LEFT) != GLFW.GLFW_PRESS) {
            event.cancel();
        }
        sprintDesynced = true;
    }

    private boolean isValidTarget(Entity entity) {
        if (entity == mc.player) return false;
        if (entity instanceof PlayerEntity) {
            if (!targetPlayers.getValue()) return false;
            taz.womp.module.modules.combat.AntiBot antiBot = (taz.womp.module.modules.combat.AntiBot) Womp.INSTANCE.getModuleManager().getModuleByClass(taz.womp.module.modules.combat.AntiBot.class);
            if (antiBot != null && antiBot.isEnabled() && taz.womp.module.modules.combat.AntiBot.isABot((PlayerEntity) entity)) return false;
        } else if (entity instanceof EndCrystalEntity) {
            if (!targetCrystals.getValue()) return false;
        } else if (entity instanceof MagmaCubeEntity || entity instanceof SlimeEntity) {
            if (!targetMobs.getValue()) return false;
        } else {
            return false;
        }
        if (checkShield.getValue() && entity instanceof PlayerEntity player) {
            if (player.isBlocking()) return false;
        }
        return true;
    }

    private boolean isInCobweb() {
        if (mc.world == null || mc.player == null) return false;
        BlockPos pos = mc.player.getBlockPos();
        return mc.world.getBlockState(pos).getBlock().getTranslationKey().toLowerCase().contains("cobweb") ||
               mc.world.getBlockState(pos.up()).getBlock().getTranslationKey().toLowerCase().contains("cobweb") ||
               mc.world.getBlockState(pos.down()).getBlock().getTranslationKey().toLowerCase().contains("cobweb");
    }

    private boolean canCriticalHit() {
        if (mc.player == null || mc.world == null) return false;
        return (mc.player.fallDistance > 0) &&
                !mc.player.isSubmergedInWater() &&
                !mc.player.isInLava() &&
                !mc.player.isClimbing() &&
                !mc.player.hasVehicle() &&
                !mc.player.hasStatusEffect(net.minecraft.entity.effect.StatusEffects.BLINDNESS);
    }

    private int getRandomReactionTime() {
        int min = reactionTime.getMinInt();
        int max = reactionTime.getMaxInt();
        if (min >= max) return min;
        return min + (int)(Math.random() * (max - min + 1));
    }

    private int getRandomAttackDelay() {
        int min = attackDelay.getMinInt();
        int max = attackDelay.getMaxInt();
        if (min >= max) return min;
        return min + (int)(Math.random() * (max - min + 1));
    }
}
