package de.dosmike.sponge.vshop;

import java.util.LinkedList;
import java.util.List;
import java.util.UUID;

import org.spongepowered.api.item.inventory.Inventory;
import org.spongepowered.api.item.inventory.property.InventoryDimension;
import org.spongepowered.api.item.inventory.property.SlotIndex;
import org.spongepowered.api.item.inventory.type.OrderedInventory;

public class InvPrep {
	
	List<StockItem> items = new LinkedList<StockItem>();
	
	public void addItem(StockItem si) {
		items.add(si);
	}
	
	public void removeIndex(int i) {
		items.remove(i);
	}
	
	public StockItem getItem(int index) {
		return items.get(index);
	}
	public int size() {
		return items.size();
	}
	
	public Inventory getInventory(Inventory.Builder ib, UUID player) {
		Inventory inv = ib
			.property("inventorydimension", new InventoryDimension(9, (int)Math.ceil((double)items.size()/9.0)*3-1))
			.build(VillagerShops.getInstance());
		
		//for some reason this class is no longer in the API, preventing any access i desire
		//the problem: inv is instance of CusotmInventory and thus can't be casted into GridInventory or similar
		CustomInventoryWrapper cinv = new CustomInventoryWrapper(inv);
		
		int row=0, col=0;
		for (int i = 0; i < items.size(); i++) {
			
			cinv.setItemStack(col, row,   items.get(i).getBuyDisplayItem(row*9+col, player));
			cinv.setItemStack(col, row+1, items.get(i).getSellDisplayItem((row+1)*9+col, player));
			
			if (++col>=9) { col=0; row+=3; }
		}
		
		return inv;
	}
	
	/** @returns 0 for buy, 1 for sell, 2 for spacer */
	public static int isSlotBuySell(int inventoryIndex) {
		return ((int)(inventoryIndex/9))%3;
	}
	
	/** @return the items index for the given inventory slot or -1 if the slot is a spacer */
	public static int slotToIndex(int inventorySlot) {
		int y = (int)(inventorySlot/9);
		int a = y%3; y/=3;
		if (a==2) return -1;
		int x = inventorySlot%9;
		return y*9+x;
	}
	
	public void updateStock(Inventory container) {
		for (StockItem item : items) item.updateStock(container);
	}
	
	/** dealing with open inventories this will target a OrderedInventory 
	 * (more precisely a OrderedInventoryAdapter at runtime) */
	public void updateInventory(Inventory view, UUID player) {
		if (!(view instanceof OrderedInventory)) {
			VillagerShops.w("Can't update view");
			return;
		}
		OrderedInventory oi = (OrderedInventory)view;
		int row=0, col=0;
		for (int i = 0; i < items.size(); i++) {
			
			oi.set(new SlotIndex(row*9+col),		items.get(i).getBuyDisplayItem(row*9+col, player));
			oi.set(new SlotIndex((row+1)*9+col),	items.get(i).getSellDisplayItem((row+1)*9+col, player));
			
			if (++col>=9) { col=0; row+=3; }
		}
	}

	public void setItem(int index, StockItem element) {
		items.set(index, element);
	}
	
//	/** tries to buy or sell the item at index and returns the ammount of actuall items bought/sold
//	 * use isSlotBuySell(int) to determ the actual action */
//	public int itemClicked(Player player, int index, int buySell) {
//		if (index<0 || index>=items.size() || buySell>1) return -1;
//		
//		Optional<UniqueAccount> acc = VillagerShops.getEconomy().getOrCreateAccount(player.getUniqueId());
//		if (!acc.isPresent()) return 0;
//		  
//		StockItem item = items.get(index);
//		Currency currency = item.getCurrency();
//		double amount;
//		double finalPrice;
//		
//		if (buySell==0) {
//			amount = item.buy(player);
//			finalPrice = item.getBuyPrice()*amount/(double)item.getItem().getQuantity();
//			acc.get().withdraw(
//					currency, 
//					BigDecimal.valueOf(finalPrice), 
//					Cause.builder().named("PURCHASED ITEMS", VillagerShops.getInstance()).build());
//		} else {
//			amount = item.sell(player);
//			finalPrice = item.getSellPrice()*amount/(double)item.getItem().getQuantity();
//			acc.get().deposit(
//					currency, 
//					BigDecimal.valueOf(finalPrice), 
//					Cause.builder().named("SOLD ITEMS", VillagerShops.getInstance()).build());
//		}
//		if (amount > 0) {
//			player.sendMessage(Text.of(TextColors.GOLD, acc.get().getBalance(currency), currency.getSymbol(), TextColors.RESET, " (",
//					(buySell==0?Text.of(TextColors.RED, "-", String.format("%.2f", finalPrice)):Text.of(TextColors.GREEN, "+", String.format("%.2f", finalPrice))),
//					"): ", (buySell==0?"Buying ":"Selling "),
//					TextColors.YELLOW, (int)amount, "x ", TextColors.RESET, 
//					item.getItem().get(Keys.DISPLAY_NAME).orElse(Text.of(item.getItem().getType().getTranslation().get()))
//					));
//		} else {
//			player.sendMessage(Text.of(TextColors.RED, "Not enough ", buySell==0?"money":"items"));
//		}
//		
//		return (int)amount;
//	}
}
