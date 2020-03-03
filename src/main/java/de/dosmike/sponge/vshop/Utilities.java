package de.dosmike.sponge.vshop;

import org.jetbrains.annotations.Nullable;
import org.spongepowered.api.command.CommandSource;
import org.spongepowered.api.data.DataQuery;
import org.spongepowered.api.data.key.Keys;
import org.spongepowered.api.entity.living.player.Player;
import org.spongepowered.api.entity.living.player.User;
import org.spongepowered.api.item.inventory.ItemStack;
import org.spongepowered.api.item.inventory.ItemStackSnapshot;
import org.spongepowered.api.service.economy.Currency;
import org.spongepowered.api.world.Location;
import org.spongepowered.api.world.World;

import java.math.BigDecimal;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.*;
import java.util.stream.Collectors;

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

    public static Currency CurrencyByName(String name) {
        if (name != null) {
            for (Currency c : VillagerShops.getEconomy().getCurrencies()) if (c.getId().equals(name)) return c;
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

    /** Custom toString name [id]<br>
     * Player is User and CommandSource, hence separate override to avoid casting */
    public static String toString(Player player) {
        return toString((User)player);
    }
    /** Custom toString name [id] */
    public static String toString(User user) {
        if(user.hasPermission("vshop.permission.probe.op"))
            return String.format("%s [%s, OP]", user.getName(), user.getUniqueId().toString());
        else if (user.hasPermission(PermissionRegistra.ADMIN.getId()))
            return String.format("%s [%s, Admin]", user.getName(), user.getUniqueId().toString());
        else
            return String.format("%s [%s]", user.getName(), user.getUniqueId().toString());
    }
    /** Custom toString name [id] */
    public static String toString(CommandSource src) {
        if (!(src instanceof Player))
            return String.format("%s [Console]", src.getName());
        if(src.hasPermission("vshop.permission.probe.op"))
            return String.format("%s [%s, OP]", src.getName(), ((Player) src).getUniqueId().toString());
        else if (PermissionRegistra.ADMIN.hasPermission(src))
            return String.format("%s [%s, Admin]", src.getName(), ((Player) src).getUniqueId().toString());
        else
            return String.format("%s [%s]", src.getName(), ((Player) src).getUniqueId().toString());
    }
    /** Custom toString item(:meta) { more } */
    public static String toString(ItemStack item) {
        return toString(item.createSnapshot());
    }
    /** Custom toString item(:meta) { more } */
    public static String toString(ItemStackSnapshot item) {
        StringBuilder sb = new StringBuilder();
        sb.append(item.getType().getId());
        if (item.get(Keys.ITEM_DURABILITY).isPresent()) {
            sb.append(" { durability: ");
            int durability = item.get(Keys.ITEM_DURABILITY).orElse(-1);
            if (durability<0)
                sb.append('?');
            else
                sb.append(durability);
            sb.append(", ");
        } else {
            int meta = item.toContainer().getInt(DataQuery.of("UnsafeDamage")).orElse(-1);
            if (meta >= 0) {
                sb.append(':');
                sb.append(meta);
            }
            sb.append(" { ");
        }
        item.get(Keys.DISPLAY_NAME).ifPresent(dn->{
            sb.append("name: ");
            sb.append(dn.toPlain());
            sb.append(", ");
        });
        String enchants = item.get(Keys.ITEM_ENCHANTMENTS).map(list->
           list.stream()
                   .map(element->element.getType().getName()+" "+element.getLevel())
                   .collect(Collectors.joining(", ", "[ ", " ]"))
        ).orElse("");
        if (!enchants.isEmpty()) {
            sb.append("enchantments: ");
            sb.append(enchants);
            sb.append(", ");
        }
        sb.append("quantity: ");
        sb.append(item.getQuantity());
        sb.append(" }");
        return sb.toString();
    }
    /** location world@x/y/z */
    public static String toString(Location<World> location) {
        return String.format("%s@%d/%d/%d",
                location.getExtent().getName(),
                location.getBlockX(),
                location.getBlockY(),
                location.getBlockZ()
            );
    }

}
