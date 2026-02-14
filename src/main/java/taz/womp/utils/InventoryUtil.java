package taz.womp.utils;

import net.minecraft.enchantment.Enchantment;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.RegistryKey;
import net.minecraft.screen.GenericContainerScreenHandler;
import net.minecraft.screen.ScreenHandler;
import taz.womp.Womp;
import taz.womp.mixin.ClientPlayerInteractionManagerAccessor;

import java.util.function.Predicate;

public final class InventoryUtil {
    public static void swap(final int selectedSlot) {
        if (selectedSlot < 0 || selectedSlot > 8) {
            return;
        }
        Womp.mc.player.getInventory().selectedSlot = selectedSlot;
        ((ClientPlayerInteractionManagerAccessor) Womp.mc.interactionManager).syncSlot();
    }

    public static boolean swapStack(final Predicate<ItemStack> predicate) {
        final PlayerInventory getInventory = Womp.mc.player.getInventory();
        for (int i = 0; i < 9; ++i) {
            if (predicate.test(getInventory.getStack(i))) {
                getInventory.selectedSlot = i;
                return true;
            }
        }
        return false;
    }

    public static boolean swapItem(final Predicate<Item> predicate) {
        final PlayerInventory getInventory = Womp.mc.player.getInventory();
        for (int i = 0; i < 9; ++i) {
            if (predicate.test(getInventory.getStack(i).getItem())) {
                getInventory.selectedSlot = i;
                return true;
            }
        }
        return false;
    }

    public static boolean swap(Item item) {
        return InventoryUtil.swapItem((Item item2) -> item2 == item);
    }

    public static int getSlot(final Item obj) {
        final ScreenHandler currentScreenHandler = Womp.mc.player.currentScreenHandler;
        if (Womp.mc.player.currentScreenHandler instanceof GenericContainerScreenHandler) {
            int n = 0;
            for (int i = 0; i < ((GenericContainerScreenHandler) Womp.mc.player.currentScreenHandler).getRows() * 9; ++i) {
                if (currentScreenHandler.getSlot(i).getStack().getItem().equals(obj)) {
                    ++n;
                }
            }
            return n;
        }
        return 0;
    }

    public static int findItem(Item item) {
        PlayerInventory inventory = Womp.mc.player.getInventory();
        for (int i = 0; i < 9; i++) {
            if (inventory.getStack(i).getItem() == item) {
                return i;
            }
        }
        return -1;
    }

    public static int findItemWithEnchantment(Item item, RegistryKey<Enchantment> enchantment) {
        PlayerInventory inventory = Womp.mc.player.getInventory();
        for (int i = 0; i < 9; i++) {
            ItemStack stack = inventory.getStack(i);
            if (stack.getItem() == item && EnchantmentUtil.hasEnchantment(stack, enchantment)) {
                return i;
            }
        }
        return -1;
    }

    @SafeVarargs
    public static int findItemWithEnchantments(Item item, RegistryKey<Enchantment>... enchantments) {
        PlayerInventory inventory = Womp.mc.player.getInventory();
        for (int i = 0; i < 9; i++) {
            ItemStack stack = inventory.getStack(i);
            if (stack.getItem() == item) {
                boolean allFound = true;
                for (RegistryKey<Enchantment> enchantment : enchantments) {
                    if (!EnchantmentUtil.hasEnchantment(stack, enchantment)) {
                        allFound = false;
                        break;
                    }
                }
                if (allFound) {
                    return i;
                }
            }
        }
        return -1;
    }
}