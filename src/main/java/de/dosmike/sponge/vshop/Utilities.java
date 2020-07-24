package de.dosmike.sponge.vshop;

import com.flowpowered.math.TrigMath;
import com.flowpowered.math.vector.Vector3d;
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
import org.spongepowered.api.world.extent.Extent;

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
    public static final Set<UUID> actionUnstack = new HashSet<>();

    /**
     * remembers what player is viewing what shop as Player <-> Shop mapping
     */
    static final Map<UUID, UUID> openShops = new HashMap<>();
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
        return bigDecimalFormat(value, locale);
    }
    public static String nf(Double value, Locale locale) {
        return doubleFormat(value, locale);
    }

    private static String bigDecimalFormat(BigDecimal number, Locale formatLocale) {
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

    public static <W extends Extent> Location<W> centerOnBlock(Location<W> at) {
        //prevent "falling" though blocks because standing on a block is yanky
        double y = at.getPosition().getY();
        double mod = y-(int)y;
        if (mod<0.2 || mod>=0.8) y=Math.floor(y+0.5);
        //center on block
        return (Location<W>)at.getExtent().getLocation(new Vector3d(at.getBlockX()+0.5,y,at.getBlockZ()+0.5));
    }

    public static Vector3d directiond(Vector3d rotation) {
        double yaw = rotation.getY() * TrigMath.DEG_TO_RAD;
        return new Vector3d(-TrigMath.sin(yaw),0,TrigMath.cos(yaw)); //yanky hack mate
    }
    public static double clampAngleDeg(double angle) {
        while (angle<=-180) angle +=360;
        while (angle>180) angle -=360;
        return angle;
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
