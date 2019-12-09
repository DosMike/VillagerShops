package de.dosmike.sponge.vshop.shops;

import de.dosmike.sponge.vshop.PermissionRegistra;
import de.dosmike.sponge.vshop.Utilities;
import de.dosmike.sponge.vshop.VillagerShops;
import de.dosmike.sponge.vshop.systems.LedgerManager;
import org.spongepowered.api.Sponge;
import org.spongepowered.api.data.key.Keys;
import org.spongepowered.api.entity.Entity;
import org.spongepowered.api.entity.living.player.Player;
import org.spongepowered.api.service.economy.Currency;
import org.spongepowered.api.service.economy.account.UniqueAccount;
import org.spongepowered.api.text.Text;
import org.spongepowered.api.text.format.TextColors;
import org.spongepowered.api.world.Location;
import org.spongepowered.api.world.World;

import java.util.Optional;
import java.util.UUID;

public class InteractionHandler {

    /**
     * return true to cancel the event in the parent
     */
    public static boolean clickEntity(Player source, Entity target) {
        //try to get shop:
        Location<World> tl = target.getLocation();
        Optional<NPCguard> npc = VillagerShops.getNPCfromLocation(tl);

        if (npc.isPresent()) {
            NPCguard shop = npc.get();
            if (shop.playershopcontainer != null && !shop.playershopcontainer.getTileEntity().isPresent()) {
                VillagerShops.w("Found a shop that lost his container, cancelled interaction!");
                VillagerShops.w("Location: %s", shop.getLoc().toString());
                if (shop.getShopOwner().isPresent())
                    VillagerShops.w("Owner: %s", shop.getShopOwner().get().toString());
                VillagerShops.w("Container was supposed to be at %s", shop.playershopcontainer);
            } else if (shop.getPreparator().size() > 0) {
                if (Utilities.getOpenShopFor(source)!=null) return true;
                shop.updateStock();
                Utilities._openShops_add(source, shop.getIdentifier());
                boolean canEdit = /*shop.isShopOwner(source.getUniqueId()) ||*/ PermissionRegistra.ADMIN.hasPermission(source);
                //bound renderer for possibly localized title
                shop.getPreparator().createRenderer(source, shop.getDisplayName(), canEdit).open(source);
            }
            return true;
        }
        return false;
    }

    /**
     * tries to buy or sell the item and returns the amount of actual items bought/sold<br>
     * @param shop is required by the calling method, and thus is passed to prevent double lookup
     * @param amount is no longer related to the stack size added, but a menu state value
     */
    public static int shopItemClicked(Player player, NPCguard shop, StockItem item, boolean doBuy, int amount) {
        Optional<UniqueAccount> acc = VillagerShops.getEconomy().getOrCreateAccount(player.getUniqueId());
        if (!acc.isPresent()) return 0;
        Optional<UUID> shopOwner = shop.getShopOwner();
        Optional<UniqueAccount> acc2 = shopOwner.flatMap(uuid -> VillagerShops.getEconomy().getOrCreateAccount(uuid));
        if (shopOwner.isPresent() && !acc2.isPresent()) return 0;

        Currency currency = item.getCurrency();
        ShopResult result;
        double finalPrice;

        if (doBuy) {
            if (item.getBuyPrice() == null) return 0;
            result = item.buy(player, shop, amount);
            if (result.getTradedItems() > 0) {
                finalPrice = item.getBuyPrice() * (double) result.getTradedItems() / (double) item.getItem().getQuantity();
                player.sendMessage(VillagerShops.getTranslator().localText("shop.buy.message")
                        .replace("%balance%", Utilities.nf(acc.get().getBalance(currency), Utilities.playerLocale(player)))
                        .replace("%currency%", currency.getSymbol())
                        .replace("%payed%", Utilities.nf(finalPrice, Utilities.playerLocale(player)))
                        .replace("%amount%", result.getTradedItems())
                        .replace("%item%", item.getItem().get(Keys.DISPLAY_NAME).orElse(Text.of(item.getItem().getType().getTranslation().get(Utilities.playerLocale(player)))))
                        .resolve(player).orElse(Text.of("[items bought]")
                        ));
                if (shop.getShopOwner().isPresent()) {
                    LedgerManager.Transaction trans = new LedgerManager.Transaction(player.getUniqueId(), shop.getIdentifier(), finalPrice, item.getCurrency(), item.getItem().getType(), result.getTradedItems());
                    trans.toDatabase();
                    LedgerManager.backstuffChat(trans);
                }
                VillagerShops.audit("%s purchased %d %s for a total of %.2f %s",
                        Utilities.toString(player), result.getTradedItems(), item.toString(), finalPrice, item.getCurrency().getSymbol().toPlain());
            } else {
                player.sendMessage(Text.of(TextColors.RED, VillagerShops.getTranslator().local(result.getMessage()).resolve(player).orElse(result.getMessage())));
                VillagerShops.audit("%s failed to purchase the item %s: %s",
                        Utilities.toString(player), item.toString(),
                        VillagerShops.getTranslator().local(result.getMessage()).orLiteral(Sponge.getServer().getConsole()));
            }
        } else {
            if (item.getSellPrice() == null) return 0;
            result = item.sell(player, shop, amount);
            if (result.getTradedItems() > 0) {
                finalPrice = item.getSellPrice() * (double) result.getTradedItems() / (double) item.getItem().getQuantity();
                player.sendMessage(VillagerShops.getTranslator().localText("shop.sell.message")
                        .replace("%balance%", Utilities.nf(acc.get().getBalance(currency), Utilities.playerLocale(player)))
                        .replace("%currency%", currency.getSymbol())
                        .replace("%payed%", Utilities.nf(finalPrice, Utilities.playerLocale(player)))
                        .replace("%amount%", result.getTradedItems())
                        .replace("%item%", item.getItem().get(Keys.DISPLAY_NAME).orElse(Text.of(item.getItem().getType().getTranslation().get(Utilities.playerLocale(player)))))
                        .resolve(player).orElse(Text.of("[items sold]")
                        ));
                if (shop.getShopOwner().isPresent()) {
                    LedgerManager.Transaction trans = new LedgerManager.Transaction(player.getUniqueId(), shop.getIdentifier(), -finalPrice, item.getCurrency(), item.getItem().getType(), result.getTradedItems());
                    trans.toDatabase();
                    LedgerManager.backstuffChat(trans);
                }
                VillagerShops.audit("%s sold %d %s for a total of %.2f %s",
                        Utilities.toString(player), result.getTradedItems(), item.toString(), finalPrice, item.getCurrency().getSymbol().toPlain());
            } else {
                player.sendMessage(Text.of(TextColors.RED, VillagerShops.getTranslator().local(result.getMessage()).resolve(player).orElse(result.getMessage())));
                VillagerShops.audit("%s failed to sell the item %s: %s",
                        Utilities.toString(player), item.toString(),
                        VillagerShops.getTranslator().local(result.getMessage()).orLiteral(Sponge.getServer().getConsole()));
            }
        }
        return result.getTradedItems();
    }
}
