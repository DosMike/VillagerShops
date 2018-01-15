package de.dosmike.sponge.vshop;

import java.math.BigDecimal;
import java.util.LinkedList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.spongepowered.api.Sponge;
import org.spongepowered.api.data.DataQuery;
import org.spongepowered.api.data.key.Keys;
import org.spongepowered.api.entity.living.player.Player;
import org.spongepowered.api.item.inventory.Inventory;
import org.spongepowered.api.item.inventory.ItemStack;
import org.spongepowered.api.item.inventory.Slot;
import org.spongepowered.api.item.inventory.entity.Hotbar;
import org.spongepowered.api.item.inventory.entity.MainPlayerInventory;
import org.spongepowered.api.service.economy.Currency;
import org.spongepowered.api.service.economy.account.Account;
import org.spongepowered.api.service.economy.account.UniqueAccount;
import org.spongepowered.api.service.economy.transaction.ResultType;
import org.spongepowered.api.service.economy.transaction.TransactionResult;
import org.spongepowered.api.text.Text;
import org.spongepowered.api.text.format.TextColors;

public class StockItem {
	private ItemStack item;
	
	private Currency currency; //currency to use
	private Double sellprice=null, buyprice=null; //This IS the STACK price
	
	//caution: maxStock does not actually hold information about the size of the stock container!
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
		if (sellprice == null) return ItemStack.empty(); //nothing
		Text cs = currency.getSymbol();
		ItemStack dis = item.copy();
		List<Text> desc = dis.get(Keys.ITEM_LORE).orElse(new LinkedList<Text>());
		desc.add(Text.of(TextColors.GREEN, (item.getQuantity()>1
				? VillagerShops.getTranslator().localText("shop.item.sell.stack")
					.replace("%price%", String.format("%.2f", sellprice))
					.replace("%itemprice%", Text.of(String.format("%.2f", sellprice/(double)item.getQuantity())))
					.replace("%currency%", cs)
					.resolve(player).orElse(
						Text.of(TextColors.GREEN, "Sell for: ", TextColors.WHITE, String.format("%.2f", sellprice), cs, String.format(" (á %.2f", sellprice/(double)item.getQuantity()), cs, ')')
						)
				: VillagerShops.getTranslator().localText("shop.item.sell.one")
				.replace("%price%", String.format("%.2f", sellprice))
				.replace("%currency%", cs)
				.resolve(player).orElse(
					Text.of(TextColors.GREEN, "Sell for: ", TextColors.WHITE, String.format("%.2f", sellprice), cs)
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
		if (buyprice == null) return ItemStack.empty(); //nothing
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
	
	public ShopResult buy(Player player, NPCguard shop) {
		Optional<UniqueAccount> account = VillagerShops.getEconomy().getOrCreateAccount(player.getUniqueId());
		if (!account.isPresent()) return ShopResult.GENERIC_FAILURE;
		Account acc = account.get();
		
		Inventory playerInv = player.getInventory().query(MainPlayerInventory.class).union(player.getInventory().query(Hotbar.class));
		int amount = Math.min(invSpace(playerInv), item.getQuantity()); //buy at max the configured amount
		if (amount <= 0) return ShopResult.CUSTOMER_INVENTORY_FULL;
		
		Optional<Inventory> stock = shop.getStockInventory();
		if (stock.isPresent()) {
			amount = Math.min(amount, invSupply(stock.get())); //reduce to what the stock can offer
			if (amount <= 0) return ShopResult.SHOPOWNER_MISSING_ITEMS;
			
			//shop owner account
			Optional<UUID> owner = shop.getShopOwner();
			if (!owner.isPresent()) return ShopResult.GENERIC_FAILURE;
			Optional<UniqueAccount> account2 = VillagerShops.getEconomy().getOrCreateAccount(owner.get());
			if (!account2.isPresent()) return ShopResult.GENERIC_FAILURE;
			
			//account transaction
			BigDecimal price = new BigDecimal(amount * buyprice/item.getQuantity());
			TransactionResult res = acc.transfer(account2.get(), currency, price, Sponge.getCauseStackManager().getCurrentCause());
			
			if (res.getResult().equals(ResultType.ACCOUNT_NO_FUNDS)) return ShopResult.CUSTOMER_LOW_BALANCE;
			else if (res.getResult().equals(ResultType.ACCOUNT_NO_SPACE)) return ShopResult.SHOPOWNER_HIGH_BALANCE;
			else if (!res.getResult().equals(ResultType.SUCCESS)) return ShopResult.GENERIC_FAILURE;
			
			//item transaction
			ItemStack stack = getItem();
			stack.setQuantity(amount);
			playerInv.offer(stack);
			getFrom(stock.get(), amount);
			
			return ShopResult.OK(amount);
		} else {
			//account transaction
			BigDecimal price = new BigDecimal(amount * buyprice/item.getQuantity());
			TransactionResult res = acc.withdraw(currency, price, Sponge.getCauseStackManager().getCurrentCause());
			
			if (res.getResult().equals(ResultType.ACCOUNT_NO_FUNDS)) return ShopResult.CUSTOMER_LOW_BALANCE;
			else if (!res.getResult().equals(ResultType.SUCCESS)) return ShopResult.GENERIC_FAILURE;
			
			//item transaction
			ItemStack stack = getItem();
			stack.setQuantity(amount);
			playerInv.offer(stack);
			
			return ShopResult.OK(amount);
		}
	}
	public ShopResult sell(Player player, NPCguard shop) {
		Optional<UniqueAccount> account = VillagerShops.getEconomy().getOrCreateAccount(player.getUniqueId());
		if (!account.isPresent()) return ShopResult.GENERIC_FAILURE;
		Account acc = account.get();
		
		Inventory playerInv = player.getInventory().query(MainPlayerInventory.class).union(player.getInventory().query(Hotbar.class));
		int amount = Math.min(invSupply(playerInv), item.getQuantity()); //buy at max the configured amount
		if (amount <= 0) return ShopResult.CUSTOMER_MISSING_ITEMS;
		
		Optional<Inventory> stock = shop.getStockInventory();
		if (stock.isPresent()) {
			amount = Math.min(amount, invSpace(stock.get())); //reduce to what the stock can offer
			if (maxStock > 0) amount = Math.min(amount, maxStock-stocked); //if we have a stock we may not exceed the stock limit (empty space for selling) - could be negative
			if (amount <= 0) return ShopResult.SHOPOWNER_INVENTORY_FULL;
			
			//shop owner account
			Optional<UUID> owner = shop.getShopOwner();
			if (!owner.isPresent()) return ShopResult.GENERIC_FAILURE;
			Optional<UniqueAccount> account2 = VillagerShops.getEconomy().getOrCreateAccount(owner.get());
			if (!account2.isPresent()) return ShopResult.GENERIC_FAILURE;
			
			//account transaction
			BigDecimal price = new BigDecimal(amount * sellprice/item.getQuantity());
			TransactionResult res = account2.get().transfer(acc, currency, price, Sponge.getCauseStackManager().getCurrentCause());
			
			if (res.getResult().equals(ResultType.ACCOUNT_NO_FUNDS)) return ShopResult.SHOPOWNER_LOW_BALANCE;
			else if (res.getResult().equals(ResultType.ACCOUNT_NO_SPACE)) return ShopResult.CUSTOMER_HIGH_BALANCE;
			else if (!res.getResult().equals(ResultType.SUCCESS)) return ShopResult.GENERIC_FAILURE;
			
			//item transaction
			ItemStack stack = getItem();
			stack.setQuantity(amount);
			stock.get().offer(stack);
			getFrom(playerInv, amount);
			
			return ShopResult.OK(amount);
		} else {
			//account transaction
			BigDecimal price = new BigDecimal(amount * sellprice/item.getQuantity());
			TransactionResult res = acc.deposit(currency, price, Sponge.getCauseStackManager().getCurrentCause());
			
			if (res.getResult().equals(ResultType.ACCOUNT_NO_SPACE)) return ShopResult.CUSTOMER_HIGH_BALANCE;
			else if (!res.getResult().equals(ResultType.SUCCESS)) return ShopResult.GENERIC_FAILURE;
			
			//item transaction
			getFrom(playerInv, amount);
			
			return ShopResult.OK(amount);
		}
	}
	
	/** figure out how much of item the inventory can hold<br><br>
	 * A few months ago there Sponge asked what could be changes about the inventory API
	 * it is only now, that I realize a inventory.tryOffer(ItemStack) or inventory.capacityFor(ItemStack) would be nice
	 * count for each slot, how many of the item it could accept
	 */
	private int invSpace(Inventory i) {
		ItemStack detector = getItem();
		int space = 0, c;
		Inventory result = i.queryAny(detector);
		for (Inventory s : result.slots()) {
			Slot slot = (Slot) s;
			c = slot.getStackSize();
			if (c > 0) space += (detector.getMaxStackQuantity() - c);
		}
		space += (i.capacity()-i.size())*detector.getMaxStackQuantity();
		return space;
	}
	/** figure out how much of item the inventory can supply
	 */
	private int invSupply(Inventory i) {
		ItemStack detector = getItem();
		int available = 0;
		available = i.queryAny(detector).totalItems();
		return available;
	}
}
