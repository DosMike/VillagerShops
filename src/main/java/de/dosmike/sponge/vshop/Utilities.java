package de.dosmike.sponge.vshop;

import org.jetbrains.annotations.Nullable;
import org.spongepowered.api.Sponge;
import org.spongepowered.api.command.CommandSource;
import org.spongepowered.api.entity.living.player.Player;
import org.spongepowered.api.item.inventory.ItemStack;
import org.spongepowered.api.service.economy.Currency;
import org.spongepowered.api.text.Text;

import javax.swing.text.NumberFormatter;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.text.NumberFormat;
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
    public static String nf(BigDecimal value, Locale locale) {
//        return value.add(BigDecimal.ZERO)
//                .setScale(2, RoundingMode.HALF_UP)
//                .toPlainString();
        return bigDecimalFormat(value, locale);
    }
    public static String nf(Double value, Locale locale) {
        return doubleFormat(value, locale);
    }

    private static String bigDecimalFormat(BigDecimal number, Locale formatLocale) {
//        DecimalFormatSymbols symbols = DecimalFormatSymbols.getInstance(formatLocale);
//        String[] ab = number.setScale(scale, RoundingMode.HALF_UP)
//                .toPlainString().split("\\.,");
//        StringBuilder sb = new StringBuilder();
//        //append group 1
//        int n = ab[0].length()%3; //group 1 size
//        sb.append(ab[0], 0, n);
//        sb.append(symbols.getGroupingSeparator());
//        //append other groups
//        for (;n<ab[0].length();n+=3) {
//            sb.append(ab[0], n, n+3);
//            sb.append(symbols.getGroupingSeparator());
//        }
//        //append decimal
//        if (ab.length>1 && !ab[1].isEmpty()) {
//            sb.append(symbols.getDecimalSeparator());
//            sb.append(ab[1]);
//        }
//        return sb.toString();
        //sponge is explicitly using big decimals, but double precision is probably enough?
        return doubleFormat(number.doubleValue(), formatLocale);
    }
    private static String doubleFormat(Double number, Locale formatLocale) {
        DecimalFormat formatter = new DecimalFormat();
        DecimalFormatSymbols symbols = DecimalFormatSymbols.getInstance(formatLocale);
        formatter.setDecimalFormatSymbols(symbols);
        formatter.setMaximumFractionDigits(2);
        formatter.setMinimumFractionDigits(2);
        return formatter.format(number);
    }

    public static Locale playerLocale(CommandSource viewer) {
        return VillagerShops.getLangSwitch().getSelectedLocale(viewer);
    }

}
