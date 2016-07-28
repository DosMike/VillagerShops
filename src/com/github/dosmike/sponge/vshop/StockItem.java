package com.github.dosmike.sponge.vshop;

import java.math.BigDecimal;
import java.util.LinkedList;
import java.util.List;
import java.util.Optional;

import org.spongepowered.api.data.key.Keys;
import org.spongepowered.api.entity.living.player.Player;
import org.spongepowered.api.item.inventory.Inventory;
import org.spongepowered.api.item.inventory.ItemStack;
import org.spongepowered.api.service.economy.account.UniqueAccount;
import org.spongepowered.api.text.Text;
import org.spongepowered.api.text.format.TextColors;

public class StockItem {
	ItemStack item;
	Double sellprice=null, buyprice=null; //per single item in stack => stack price is item.quantity * price
	
	public StockItem(ItemStack itemstack, Double sellfor, Double buyfor) {
		item = itemstack.copy();
		sellprice = sellfor;
		buyprice = buyfor;
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
	
	public ItemStack getSellDisplayItem() {
		Text cs = VillagerShops.getEconomy().getDefaultCurrency().getSymbol();
		ItemStack dis = item.copy();
		List<Text> desc = dis.get(Keys.ITEM_LORE).orElse(new LinkedList<Text>());
		desc.add(Text.of(TextColors.GREEN, "Get: ", TextColors.WHITE, String.format("%.2f", sellprice*item.getQuantity()), cs, (item.getQuantity()>1? 
				Text.of(String.format(" (á %.2f", sellprice), cs, ')')
				:"") ));
		
		dis.offer(Keys.ITEM_LORE, desc);
		return dis;
	}
	public ItemStack getBuyDisplayItem() {
		Text cs = VillagerShops.getEconomy().getDefaultCurrency().getSymbol();
		ItemStack dis = item.copy();
		List<Text> desc = dis.get(Keys.ITEM_LORE).orElse(new LinkedList<Text>());
		desc.add(Text.of(TextColors.RED, "Pay: ", TextColors.WHITE, String.format("%.2f", sellprice*item.getQuantity()), cs, (item.getQuantity()>1? 
				Text.of(String.format(" (á %.2f", sellprice), cs, ')')
				:"") ));
		
		dis.offer(Keys.ITEM_LORE, desc);
		return dis;
	}
	
	/**
	 * @param quantity the amount of items supposed to give, overriding the default in item  
	 * @returns the amount of given items */
	public int offerTo(Inventory inv, int quantity) {
		ItemStack copy = item.copy();
		copy.setQuantity(quantity);
		inv.offer(copy);
		return item.getQuantity()-copy.getQuantity();
	}
	/** @returns the amount of given items */
	public int offerTo(Inventory inv) {
		return offerTo(inv, item.getQuantity());
	}
	
	/** 
	 * @param quantity the amount of items supposed to take, overriding the default in item  
	 * @returns the amount of items taken out of the inventory */
	public int getFrom(Inventory inv, int quantity) {
		int ammountLeft = quantity;
		ItemStack sizeless = item.copy();
		sizeless.setQuantity(-1);
		Inventory result = inv.query(sizeless);
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
	
	/** @returns the amount of items taken out of the inventory */
	public int getFrom(Inventory inv) {
		return getFrom(inv, item.getQuantity());
	}
	
	/** checks how many of this item the player can afford with the default currency */
	public int canAfford(Player player) {
		Optional<UniqueAccount> acc = VillagerShops.getEconomy().getOrCreateAccount(player.getUniqueId());
		if (!acc.isPresent()) return 0;
		BigDecimal bd = acc.get().getBalance(VillagerShops.getEconomy().getDefaultCurrency());
		BigDecimal num = bd.divide(new BigDecimal(buyprice));
		int ammount = num.intValue();
		return (ammount > item.getQuantity() ? item.getQuantity() : ammount);
	}
	
	/** checks how many of this item the player can afford with the default currency */
	public int canAccept(Player player) {
		return item.getQuantity();	//there's no way to check how much money a account can hold
	}
	
	/** @returns the ammount of purchased items */
	public int buy(Player player) {
		return offerTo(player.getInventory(), canAfford(player));
	}
	
	/** @returns the ammount of sold items */
	public int sell(Player player) {
		return getFrom(player.getInventory(), canAccept(player));
	}
}
