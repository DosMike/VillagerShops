package de.dosmike.sponge.vshop.shops;

import de.dosmike.sponge.vshop.PermissionRegistra;
import de.dosmike.sponge.vshop.Utilities;
import de.dosmike.sponge.vshop.VillagerShops;
import de.dosmike.sponge.vshop.menus.MShopSlot;
import de.dosmike.sponge.vshop.systems.LedgerManager;
import de.dosmike.sponge.vshop.systems.ShopType;
import de.dosmike.sponge.vshop.systems.pluginfilter.FilterResolutionException;
import org.spongepowered.api.Sponge;
import org.spongepowered.api.data.key.Keys;
import org.spongepowered.api.entity.living.player.Player;
import org.spongepowered.api.item.inventory.ItemStackSnapshot;
import org.spongepowered.api.service.economy.Currency;
import org.spongepowered.api.service.economy.account.UniqueAccount;
import org.spongepowered.api.text.Text;
import org.spongepowered.api.text.format.TextColors;

import java.util.Optional;
import java.util.UUID;

public class InteractionHandler {

	/**
	 * return true to cancel the event in the parent
	 */
	public static boolean clickEntity(Player source, UUID targetUniqueId) {
		return VillagerShops.getShopFromEntityId(targetUniqueId).map(shopEntity -> {
			if (shopEntity.playershopContainer != null && !shopEntity.playershopContainer.getTileEntity().isPresent()) {
				VillagerShops.w("Found a shop that lost his container, cancelled interaction!");
				VillagerShops.w("Location: %s", shopEntity.getLocation().toString());
				if (shopEntity.getShopOwner().isPresent())
					VillagerShops.w("Owner: %s", shopEntity.getShopOwner().get().toString());
				VillagerShops.w("Container was supposed to be at %s", shopEntity.playershopContainer);
			} else if (shopEntity.getMenu().size() > 0) {
				if (Utilities.getOpenShopFor(source) != null) return true;
				shopEntity.updateStock();
				Utilities._openShops_add(source, shopEntity.getIdentifier());
				boolean canEdit = /*shop.isShopOwner(source.getUniqueId()) ||*/ PermissionRegistra.ADMIN.hasPermission(source);
				//bound renderer for possibly localized title
				shopEntity.getMenu().createRenderer(source, shopEntity.getDisplayName(), canEdit).open(source);
			}
			return true;
		}).orElse(false);
	}

	/**
	 * tries to buy or sell the item and returns the amount of actual items bought/sold<br>
	 * somewhat of a bridge from {@link MShopSlot}s click listener -> {@link StockItem} functions
	 *
	 * @param shop   is required by the calling method, and thus is passed to prevent double lookup
	 * @param amount is no longer related to the stack size added, but a menu state value
	 */
	public static int shopItemClicked(Player player, ShopEntity shop, StockItem item, boolean doBuy, int amount) throws FilterResolutionException {
		Optional<UniqueAccount> customerAccount = VillagerShops.getEconomy().getOrCreateAccount(player.getUniqueId());
		if (!customerAccount.isPresent()) return 0;
		Optional<UUID> shopOwnerId = shop.getShopOwner();
		Optional<UniqueAccount> ownerAccount = shopOwnerId.flatMap(uuid -> VillagerShops.getEconomy().getOrCreateAccount(uuid));
		if (shopOwnerId.isPresent() && !ownerAccount.isPresent()) return 0;

		Currency currency = item.getCurrency();
		Purchase.Result result;

		ItemStackSnapshot displayItem = item.getItem(ShopType.fromInstance(shop)); //calls getDisplayItems

		if (doBuy) {
			if (item.getBuyPrice() == null) return 0;
			result = item.buy(player, shop, amount);
			if (result.getTradedItems() > 0) {
				player.sendMessage(VillagerShops.getTranslator().localText("shop.buy.message")
						.replace("%balance%", Utilities.nf(customerAccount.get().getBalance(currency), Utilities.playerLocale(player)))
						.replace("%currency%", currency.getSymbol())
						.replace("%payed%", Utilities.nf(result.finalPrice, Utilities.playerLocale(player)))
						.replace("%amount%", result.getTradedItems())
						.replace("%item%", displayItem.get(Keys.DISPLAY_NAME).orElse(Text.of(displayItem.getType().getTranslation().get(Utilities.playerLocale(player)))))
						.resolve(player).orElse(Text.of("[items bought]")
						));
				if (shop.getShopOwner().isPresent()) {
					LedgerManager.Transaction trans = new LedgerManager.Transaction(player.getUniqueId(), shop.getIdentifier(), (result.finalPrice.doubleValue()), item.getCurrency(), displayItem.getType(), result.getTradedItems());
					trans.toDatabase();
					LedgerManager.backstuffChat(trans);
				}
				VillagerShops.audit("%s purchased %d %s for a total of %.2f %s",
						Utilities.toString(player), result.getTradedItems(), item.toString(), result.finalPrice, item.getCurrency().getSymbol().toPlain());
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
				player.sendMessage(VillagerShops.getTranslator().localText("shop.sell.message")
						.replace("%balance%", Utilities.nf(customerAccount.get().getBalance(currency), Utilities.playerLocale(player)))
						.replace("%currency%", currency.getSymbol())
						.replace("%payed%", Utilities.nf(result.finalPrice, Utilities.playerLocale(player)))
						.replace("%amount%", result.getTradedItems())
						.replace("%item%", displayItem.get(Keys.DISPLAY_NAME).orElse(Text.of(displayItem.getType().getTranslation().get(Utilities.playerLocale(player)))))
						.resolve(player).orElse(Text.of("[items sold]")
						));
				if (shop.getShopOwner().isPresent()) {
					LedgerManager.Transaction trans = new LedgerManager.Transaction(player.getUniqueId(), shop.getIdentifier(), -(result.finalPrice.doubleValue()), item.getCurrency(), displayItem.getType(), result.getTradedItems());
					trans.toDatabase();
					LedgerManager.backstuffChat(trans);
				}
				VillagerShops.audit("%s sold %d %s for a total of %.2f %s",
						Utilities.toString(player), result.getTradedItems(), item.toString(), result.finalPrice, item.getCurrency().getSymbol().toPlain());
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
