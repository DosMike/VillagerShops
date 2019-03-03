package de.dosmike.sponge.vshop;

import de.dosmike.sponge.megamenus.api.IMenu;
import de.dosmike.sponge.megamenus.api.MenuRenderer;
import de.dosmike.sponge.megamenus.api.listener.OnRenderStateListener;
import de.dosmike.sponge.megamenus.impl.GuiRenderer;
import de.dosmike.sponge.megamenus.impl.RenderManager;
import org.spongepowered.api.Sponge;
import org.spongepowered.api.data.key.Keys;
import org.spongepowered.api.entity.Entity;
import org.spongepowered.api.entity.living.player.Player;
import org.spongepowered.api.item.inventory.Container;
import org.spongepowered.api.item.inventory.type.OrderedInventory;
import org.spongepowered.api.service.economy.Currency;
import org.spongepowered.api.service.economy.account.UniqueAccount;
import org.spongepowered.api.text.Text;
import org.spongepowered.api.text.format.TextColors;
import org.spongepowered.api.world.Location;
import org.spongepowered.api.world.World;

import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

public class InteractionHandler {

    /**
     * return true to cancel the event in the parent
     */
    static boolean clickEntity(Player source, Entity target) {
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
                shop.updateStock();
                int idealHeight = Math.min(2 + ((int) Math.ceil((double) shop.getPreparator().size() / 9.0) - 1), 6);
                VillagerShops.openShops.put(source.getUniqueId(), shop.getIdentifier());
                //bound renderer for possibly localized title
                GuiRenderer renderer = (GuiRenderer) shop.getMenu().createGuiRenderer(idealHeight, true);
                renderer.getMenu().setTitle(Text.of(TextColors.DARK_AQUA,
                        VillagerShops.getTranslator().localText("shop.title")
                                .replace("%name%", Text.of(TextColors.RESET, shop.getDisplayName() == null ? Text.of() : shop.getDisplayName()))
                                .resolve(source).orElse(Text.of("[vShop] ", shop.getDisplayName() == null ? Text.of() : shop.getDisplayName()))));
                renderer.setRenderListener(new OnRenderStateListener() {
                    @Override
                    public boolean closed(MenuRenderer render, IMenu menu, Player viewer) {
                        VillagerShops.actionUnstack.remove(viewer.getUniqueId());
                        VillagerShops.openShops.remove(viewer.getUniqueId());
                        return false;
                    }
                });
                renderer.open(source);
            }
            return true;
        }
        return false;
    }

    /**
     * tries to buy or sell the item and returns the ammount of actuall items bought/sold<br>
     * @param shop is required by the calling method, and thus is passed to prevent double lookup
     */
    static int shopItemClicked(Player player, NPCguard shop, StockItem item, boolean doBuy) {
        Optional<UniqueAccount> acc = VillagerShops.getEconomy().getOrCreateAccount(player.getUniqueId());
        if (!acc.isPresent()) return 0;
        Optional<UUID> shopOwner = shop.getShopOwner();
        Optional<UniqueAccount> acc2 = shopOwner.flatMap(uuid -> VillagerShops.getEconomy().getOrCreateAccount(uuid));
        if (shopOwner.isPresent() && !acc2.isPresent()) return 0;

        Currency currency = item.getCurrency();
        ShopResult result;
        double finalPrice;

        if (doBuy) {
            result = item.buy(player, shop);
            if (result.getTradedItems() > 0) {
                finalPrice = item.getBuyPrice() * (double) result.getTradedItems() / (double) item.getItem().getQuantity();
                player.sendMessage(VillagerShops.getTranslator().localText("shop.buy.message")
                        .replace("%balance%", Text.of(TextColors.GOLD, VillagerShops.nf(acc.get().getBalance(currency)), currency.getSymbol(), TextColors.RESET))
                        .replace("%payed%", Text.of(TextColors.RED, "-", String.format("%.2f", finalPrice), TextColors.RESET))
                        .replace("%amount%", Text.of(TextColors.YELLOW, result.getTradedItems(), TextColors.RESET))
                        .replace("%item%", Text.of(item.getItem().get(Keys.DISPLAY_NAME)
                                .orElse(Text.of(item.getItem().getType().getTranslation().get(VillagerShops.playerLocale(player))))))
                        .resolve(player).orElse(Text.of("[items bought]")
                        ));
                if (shop.getShopOwner().isPresent()) {
                    LedgerManager.Transaction trans = new LedgerManager.Transaction(player.getUniqueId(), shop.getIdentifier(), finalPrice, item.getCurrency(), item.getItem().getType(), result.getTradedItems());
                    trans.toDatabase();
                    LedgerManager.backstuffChat(trans);
                }
            } else {
                player.sendMessage(Text.of(TextColors.RED, VillagerShops.getTranslator().local(result.getMessage()).resolve(player).orElse(result.getMessage())));
            }
        } else {
            result = item.sell(player, shop);
            if (result.getTradedItems() > 0) {
                finalPrice = item.getSellPrice() * (double) result.getTradedItems() / (double) item.getItem().getQuantity();
                player.sendMessage(VillagerShops.getTranslator().localText("shop.sell.message")
                        .replace("%balance%", Text.of(TextColors.GOLD, VillagerShops.nf(acc.get().getBalance(currency)), currency.getSymbol(), TextColors.RESET))
                        .replace("%payed%", Text.of(TextColors.GREEN, "+", String.format("%.2f", finalPrice), TextColors.RESET))
                        .replace("%amount%", Text.of(TextColors.YELLOW, result.getTradedItems(), TextColors.RESET))
                        .replace("%item%", Text.of(item.getItem().get(Keys.DISPLAY_NAME).orElse(Text.of(item.getItem().getType().getTranslation().get(VillagerShops.playerLocale(player))))))
                        .resolve(player).orElse(Text.of("[items bought]")
                        ));
                if (shop.getShopOwner().isPresent()) {
                    LedgerManager.Transaction trans = new LedgerManager.Transaction(player.getUniqueId(), shop.getIdentifier(), -finalPrice, item.getCurrency(), item.getItem().getType(), result.getTradedItems());
                    trans.toDatabase();
                    LedgerManager.backstuffChat(trans);
                }
            } else {
                player.sendMessage(Text.of(TextColors.RED, VillagerShops.getTranslator().local(result.getMessage()).resolve(player).orElse(result.getMessage())));
            }
        }
        return result.getTradedItems();
    }
}
