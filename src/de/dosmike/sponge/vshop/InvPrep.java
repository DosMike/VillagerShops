package de.dosmike.sponge.vshop;

import java.util.LinkedList;
import java.util.List;

import org.spongepowered.api.entity.living.player.Player;
import org.spongepowered.api.item.inventory.Inventory;
import org.spongepowered.api.item.inventory.InventoryArchetypes;
import org.spongepowered.api.item.inventory.property.InventoryDimension;
import org.spongepowered.api.item.inventory.property.InventoryTitle;
import org.spongepowered.api.text.Text;

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
	
	public Inventory getInventory() {
		Inventory inv = Inventory.builder().of(InventoryArchetypes.CHEST)
//			.listener(type, listener)
			.property("title", new InventoryTitle(Text.of("[vShop]")))
			.property("inventorydimension", new InventoryDimension(9, (items.size()/9+1)*3-1))
			.build(VillagerShops.getInstance());
		
		//for some reason this class is no longer in the API, preventing any access i desire
		//the problem: inv is instance of CusotmInventory and thus can't be casted into GridInventory or similar
		CustomInventoryWrapper cinv = new CustomInventoryWrapper(inv);
		
		int row=0, col=0;
		for (int i = 0; i < items.size(); i++) {
			
			cinv.setItemStack(col, row,   items.get(i).getBuyDisplayItem());
			cinv.setItemStack(col, row+1, items.get(i).getSellDisplayItem());
			
			if (++col>=9) { col=0; row+=3; }
		}
		
		return inv;
	}
	
	/** @returns 0 for buy, 1 for sell, 2 for spacer */
	public int isSlotBuySell(int inventoryIndex) {
		return ((int)(inventoryIndex/9))%3;
	}
	
	/** @return the items index for the given inventory slot or -1 if the slot is a spacer */
	public int slotToIndex(int inventorySlot) {
		int y = (int)(inventorySlot/9);
		int a = y%3; y/=3;
		if (a==2) return -1;
		int x = inventorySlot%9;
		return y*9+x;
	}
	
	/** tries to buy or sell the item at index and returns the ammount of actuall items bought/sold
	 * use isSlotBuySell(int) to determ the actual action */
	public int itemClicked(Player player, int index, int buySell) {
		if (index<0 || index>=items.size() || buySell>1) return -1;
		  
		StockItem item = items.get(index);
		return (buySell==0 ? item.buy(player) : item.sell(player));
	}
}
