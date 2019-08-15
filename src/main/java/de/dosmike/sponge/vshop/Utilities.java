package de.dosmike.sponge.vshop;

import org.jetbrains.annotations.Nullable;
import org.spongepowered.api.command.CommandSource;
import org.spongepowered.api.entity.living.player.Player;
import org.spongepowered.api.service.economy.Currency;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.*;

public class Utilities {

    /**
     * to prevent bugs from spamming actions, we'll use this to lock any actions
     * until 1 tick after the inventory handler finished
     */
    public static Set<UUID> actionUnstack = new HashSet<UUID>();

    /**
     * remembers what player is viewing what shop as Player <-> Shop mapping
     */
    static Map<UUID, UUID> openShops = new HashMap<UUID, UUID>();
    public static @Nullable UUID getOpenShopFor(Player player) {
        return openShops.get(player.getUniqueId());
    }
    public static void _openShops_remove(Player player) {
        openShops.remove(player.getUniqueId());
    }
    public static void _openShops_add(Player player, UUID shopID) {
        openShops.put(player.getUniqueId(), shopID);
    }

    public static org.spongepowered.api.service.economy.Currency CurrencyByName(String name) {
        if (name != null) {
            for (org.spongepowered.api.service.economy.Currency c : VillagerShops.getEconomy().getCurrencies()) if (c.getId().equals(name)) return c;
            for (Currency c : VillagerShops.getEconomy().getCurrencies()) if (c.getName().equalsIgnoreCase(name)) return c;
        }
        return VillagerShops.getEconomy().getDefaultCurrency();
    }

    /**
     * format a bigDecimal to a precision of 3, because everything else makes no sense in currency context
     */
    public static String nf(BigDecimal value) {
        return value.add(BigDecimal.ZERO).setScale(2, RoundingMode.HALF_UP).toPlainString();
//        return value.round(new MathContext(3, RoundingMode.HALF_EVEN)).toPlainString();
    }

    public static Locale playerLocale(CommandSource viewer) {
        return VillagerShops.getLangSwitch().getSelectedLocale(viewer);
    }

}
