package de.dosmike.sponge.vshop;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.LinkedList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.spongepowered.api.data.DataQuery;
import org.spongepowered.api.data.key.Keys;
import org.spongepowered.api.entity.living.player.Player;
import org.spongepowered.api.item.inventory.Inventory;
import org.spongepowered.api.item.inventory.ItemStack;
import org.spongepowered.api.service.economy.Currency;
import org.spongepowered.api.service.economy.account.UniqueAccount;
import org.spongepowered.api.text.Text;
import org.spongepowered.api.text.format.TextColors;

public class StockItem {
	private ItemStack item;
	
	private Currency currency; //currency to use
	private Double sellprice=null, buyprice=null; //per single item in stack => stack price is item.quantity * price
	
	private int maxStock=0, stocked=0;
	
	public StockItem(ItemStack itemstack, Double sellfor, Double buyfor, Currency currency, int stockLimit) {
		item = itemstack.copy();
		if (sellfor!=null && sellfor>=0) sellprice = sellfor;
		if (buyfor!=null && buyfor>=0) buyprice = buyfor;
		this.currency = currency; 
		maxStock = stockLimit;
	}
	
	public ItemStack getItem() {
		return item.copy();
	}
	public Double getBuyPrice() {
		return buyprice;
	}
	public Double getSellPrice() {
		return sellprice;
	}
	public void setBuyPrice(Double price) {
		buyprice = price;
	}
	public void setSellPrice(Double price) {
		sellprice = price;
	}
	/** the currency this item is handled in */
	public Currency getCurrency() {
		return currency;
	}
	public int getMaxStock() {
		return maxStock;
	}
	public int getStocked() {
		return maxStock>0?stocked:item.getQuantity();
	}
	
	public void updateStock(Inventory container) {
		Inventory result = container.queryAny(item);
		int count = 0;
		for (Inventory s : result.slots()) count += s.totalItems();
		stocked = Math.min(maxStock, count);
	}
	
	/** create a Item with custom description adding the sell-price for a stack with the present size */
	public ItemStack getSellDisplayItem(int patchedSlot, UUID player) {
		if (sellprice == null) return ItemStack.of(FieldResolver.emptyHandItem(), 1); //nothing
		Text cs = currency.getSymbol();
		ItemStack dis = item.copy();
		List<Text> desc = dis.get(Keys.ITEM_LORE).orElse(new LinkedList<Text>());
		desc.add(Text.of(TextColors.GREEN, (item.getQuantity()>1
				? VillagerShops.getTranslator().localText("shop.item.sell.stack")
					.replace("%price%", String.format("%.2f", buyprice))
					.replace("%itemprice%", Text.of(String.format("%.2f", buyprice/(double)item.getQuantity())))
					.replace("%currency%", cs)
					.resolve(player).orElse(
						Text.of(TextColors.GREEN, "Sell for: ", TextColors.WHITE, String.format("%.2f", buyprice), cs, String.format(" (á %.2f", buyprice/(double)item.getQuantity()), cs, ')')
						)
				: VillagerShops.getTranslator().localText("shop.item.sell.one")
				.replace("%price%", String.format("%.2f", buyprice))
				.replace("%currency%", cs)
				.resolve(player).orElse(
					Text.of(TextColors.GREEN, "Sell for: ", TextColors.WHITE, String.format("%.2f", buyprice), cs)
					)
				)));
		if (maxStock>0) 
			desc.add(Text.of(TextColors.GRAY, 
					VillagerShops.getTranslator().localText("shop.item.stock")
						.replace("%amount%", getStocked())
						.replace("%max%", maxStock)
						.resolve(player).orElse(
							Text.of(String.format("In Stock: %d/%d", getStocked(), maxStock))
							))); 
		
		dis.offer(Keys.ITEM_LORE, desc);
		
		return ItemStack.builder().fromContainer(
                dis.toContainer().set(DataQuery.of("UnsafeData", "vShopSlotNum"), patchedSlot) ).build();
	}
	/** create a Item with custom description adding the buy-price for a stack with the present size */
	public ItemStack getBuyDisplayItem(int patchedSlot, UUID player) {
		if (buyprice == null) return ItemStack.of(FieldResolver.emptyHandItem(), 1); //nothing
		Text cs = currency.getSymbol();
		ItemStack dis = item.copy();
		List<Text> desc = dis.get(Keys.ITEM_LORE).orElse(new LinkedList<Text>());
		desc.add(Text.of(TextColors.RED, (item.getQuantity()>1
				? VillagerShops.getTranslator().localText("shop.item.buy.stack")
					.replace("%price%", String.format("%.2f", buyprice))
					.replace("%itemprice%", Text.of(String.format("%.2f", buyprice/(double)item.getQuantity())))
					.replace("%currency%", cs)
					.resolve(player).orElse(
						Text.of(TextColors.RED, "Buy for: ", TextColors.WHITE, String.format("%.2f", buyprice), cs, String.format(" (á %.2f", buyprice/(double)item.getQuantity()), cs, ')')
						)
				: VillagerShops.getTranslator().localText("shop.item.buy.one")
				.replace("%price%", String.format("%.2f", buyprice))
				.replace("%currency%", cs)
				.resolve(player).orElse(
					Text.of(TextColors.RED, "Buy for: ", TextColors.WHITE, String.format("%.2f", buyprice), cs)
					)
				)));
		if (maxStock>0) 
			desc.add(Text.of(TextColors.GRAY, 
				VillagerShops.getTranslator().localText("shop.item.stock")
					.replace("%amount%", getStocked())
					.replace("%max%", maxStock)
					.resolve(player).orElse(
						Text.of(String.format("In Stock: %d/%d", getStocked(), maxStock))
						)));
		
		dis.offer(Keys.ITEM_LORE, desc);

		return ItemStack.builder().fromContainer(
                dis.toContainer().set(DataQuery.of("UnsafeData", "vShopSlotNum"), patchedSlot) ).build();
	}
	
	/** Try to add the represented itemstack to the target inventory without doing any economy changes.
	 * This function is able to give a fraction of the represented stack size. 
	 * @param quantity the amount of items supposed to give, overriding the default in item  
	 * @returns the amount of given items */
	public int offerTo(Inventory inv, int quantity) {
		ItemStack copy = item.copy();
		copy.setQuantity(quantity);
		inv.offer(copy);
		int c = copy.getQuantity();
//		VillagerShops.l("Rejected "+c);
		return quantity-c;
	}
	/** Try to add the represented itemstack to the target inventory without doing any economy changes.
	 * This function tried to give as much of this item as represented by the itemstack
	 * @returns the amount of given items */
	public int offerTo(Inventory inv) {
		return offerTo(inv, item.getQuantity());
	}
	
	/** Tries to take a certain amount of items represented by this item from the inventory 
	 * @param quantity the amount of items supposed to take, overriding the default in item  
	 * @returns the amount of items taken out of the inventory */
	public int getFrom(Inventory inv, int quantity) {
		int ammountLeft = quantity;
		Inventory result = inv.queryAny(item);
		for (Inventory s : result.slots()) {
			ItemStack onSlot = s.poll(ammountLeft).orElse(null);
			if (onSlot == null)continue;
			if (onSlot.getQuantity()<=ammountLeft) {
				ammountLeft -= onSlot.getQuantity();
			} else {
				onSlot.setQuantity(onSlot.getQuantity()-ammountLeft);
				ammountLeft = 0;
				s.offer(onSlot);
			}
			if (ammountLeft == 0) break;
		}
		return quantity-ammountLeft;
	}
	
	/** Tries to take the represented item with the given quantity from the inventory.
	 * If the item has max stock, it will get the minimum of remaining stock and available items. 
	 * @returns the amount of items taken out of the inventory */
	public int getFrom(Inventory inv) {
		return getFrom(inv, maxStock<=0?item.getQuantity():Math.min(item.getQuantity(), maxStock-stocked));
	}
	
	/** About Money: checks how many of this item the player can afford with the default currency maxing out at the quantity given by the represented itemstack */
	public int canAfford(UUID player, double price) {
		Optional<UniqueAccount> acc = VillagerShops.getEconomy().getOrCreateAccount(player);
		if (!acc.isPresent()) return 0;
//		VillagerShops.l(acc.get().getDisplayName()+ " has " + acc.get().getBalance(currency).toPlainString());
		BigDecimal bd = acc.get().getBalance(currency);
		BigDecimal num = bd.divide(new BigDecimal(price/(double)item.getQuantity()), 2, RoundingMode.HALF_DOWN);
		int amount = num.intValue();
		amount = (num.compareTo(BigDecimal.valueOf(item.getQuantity())) > 0 ? item.getQuantity() : amount);
//		VillagerShops.l("Can afford "+amount);
		return amount;
	}
	
	/** About Money: checks how many of this item the player can afford with the default currency */
	public int canAccept(UUID player) {
		return item.getQuantity();	//there's no way to check how much money a account can hold
	} 
	
	/** @returns the ammount of actually purchased items, -1 if the stock is empty */
	public ShopResult buy(Player player, NPCguard shop) {
		Optional<Inventory> stock = shop.getStockInventory();
		if (!stock.isPresent()) {
			int amount = canAfford(player.getUniqueId(), buyprice);
			if (amount == 0) return ShopResult.CUSTOMER_LOW_BALANCE;
			amount = offerTo(player.getInventory(), amount);
			if (amount == 0) return ShopResult.CUSTOMER_INVENTORY_FULL;
			return ShopResult.OK(amount);
		} else {
			//let's say we could afford 5 apples from a shelf
			int amount = canAfford(player.getUniqueId(), buyprice);
			if (amount == 0) return ShopResult.CUSTOMER_LOW_BALANCE;
			int took = getFrom(stock.get(), amount); //we take all the apples we can afford
			if (took < 1 && amount > 0) return ShopResult.SHOPOWNER_MISSING_ITEMS;
			int stocked = offerTo(player.getInventory(), took);		//and try so put them in our backpack, but only 3 fit
			offerTo(stock.get(), took-stocked);	//so we put the other 2 apples back on the shelf
			if (stocked == 0) return ShopResult.CUSTOMER_INVENTORY_FULL;
			else return ShopResult.OK(stocked);
		}
	}
	
	/** @returns the ammount of actually sold items, -1 if the stock is full or other can't afford */
	public ShopResult sell(Player player, NPCguard shop) {
		Optional<Inventory> stock = shop.getStockInventory();
		if (!stock.isPresent()) {
			int amount = getFrom(player.getInventory(), canAccept(player.getUniqueId()));
			if (amount == 0) return ShopResult.CUSTOMER_MISSING_ITEMS;
			else return ShopResult.OK(amount);
		} else {
			//let's say we have 5 apples, the shelf owner might be able to afford 4
			int amount;
			if (shop.getShopOwner().isPresent()) { //there might be a stock, but no owner in the future
				amount = canAfford(shop.getShopOwner().get(), sellprice);
				if (amount < 1) return ShopResult.SHOPOWNER_LOW_BALANCE;
			} else amount = canAccept(player.getUniqueId());
			if (maxStock>0) amount = Math.min(amount, maxStock-stocked); //if the stock is limited we do not want to exceed the limit for this item
			if (amount < 1) return ShopResult.SHOPOWNER_INVENTORY_FULL; // if we can't sell any items NOW, the stock limit most likely was reached
			 
			int took = getFrom(player.getInventory(), amount); //we want to put them on a shelf
			if (took == 0) return ShopResult.CUSTOMER_MISSING_ITEMS;
			int stocked = offerTo(stock.get(), took);		//and we stuff them onto the shelf, but only 3 fit
			offerTo(player.getInventory(), took-stocked);	//so we take the other 2 apples back
			if (stocked == 0) return ShopResult.SHOPOWNER_INVENTORY_FULL;
			else return ShopResult.OK(stocked);
		}
	}
}
