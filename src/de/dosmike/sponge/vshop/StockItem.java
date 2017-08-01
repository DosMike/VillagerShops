package de.dosmike.sponge.vshop;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.LinkedList;
import java.util.List;
import java.util.Optional;

import org.spongepowered.api.data.key.Keys;
import org.spongepowered.api.entity.living.player.Player;
import org.spongepowered.api.item.ItemTypes;
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
	
	public StockItem(ItemStack itemstack, Double sellfor, Double buyfor, Currency currency) {
		item = itemstack.copy();
		if (sellfor!=null && sellfor>=0) sellprice = sellfor;
		if (buyfor!=null && buyfor>=0) buyprice = buyfor;
		this.currency = currency; 
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
	
	/** create a Item with custom description adding the sell-price for a stack with the present size */
	public ItemStack getSellDisplayItem() {
		if (sellprice == null) return ItemStack.of(ItemTypes.AIR, 1); //nothing
		Text cs = currency.getSymbol();
		ItemStack dis = item.copy();
		List<Text> desc = dis.get(Keys.ITEM_LORE).orElse(new LinkedList<Text>());
		desc.add(Text.of(TextColors.GREEN, "Sell for: ", TextColors.WHITE, String.format("%.2f", sellprice), cs, (item.getQuantity()>1? 
				Text.of(String.format(" (á %.2f", sellprice/(double)item.getQuantity()), cs, ')')
				:"") ));
		
		dis.offer(Keys.ITEM_LORE, desc);
		return dis;
	}
	/** create a Item with custom description adding the buy-price for a stack with the present size */
	public ItemStack getBuyDisplayItem() {
		if (buyprice == null) return ItemStack.of(ItemTypes.AIR, 1); //nothing
		Text cs = currency.getSymbol();
		ItemStack dis = item.copy();
		List<Text> desc = dis.get(Keys.ITEM_LORE).orElse(new LinkedList<Text>());
		desc.add(Text.of(TextColors.RED, "Buy for: ", TextColors.WHITE, String.format("%.2f", buyprice), cs, (item.getQuantity()>1? 
				Text.of(String.format(" (á %.2f", buyprice/(double)item.getQuantity()), cs, ')')
				:"") ));
		
		dis.offer(Keys.ITEM_LORE, desc);
		return dis;
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
	
	/** Tries to take the represented item with the given quantity from the inventory
	 * @returns the amount of items taken out of the inventory */
	public int getFrom(Inventory inv) {
		return getFrom(inv, item.getQuantity());
	}
	
	/** checks how many of this item the player can afford with the default currency maxing out at the quantity given by the represented itemstack */
	public int canAfford(Player player) {
		Optional<UniqueAccount> acc = VillagerShops.getEconomy().getOrCreateAccount(player.getUniqueId());
		if (!acc.isPresent()) return 0;
		BigDecimal bd = acc.get().getBalance(currency);
		BigDecimal num = bd.divide(new BigDecimal(buyprice/(double)item.getQuantity()), 2, RoundingMode.HALF_DOWN);
		int amount = num.intValue();
		amount = (num.compareTo(BigDecimal.valueOf(item.getQuantity())) > 0 ? item.getQuantity() : amount);
//		VillagerShops.l("Can afford "+amount);
		return amount;
	}
	
	/** checks how many of this item the player can afford with the default currency */
	public int canAccept(Player player) {
		return item.getQuantity();	//there's no way to check how much money a account can hold
	}
	
	/** @returns the ammount of actually purchased items */
	public int buy(Player player) {
		return offerTo(player.getInventory(), canAfford(player));
	}
	
	/** @returns the ammount of actually sold items */
	public int sell(Player player) {
		return getFrom(player.getInventory(), canAccept(player));
	}
}
