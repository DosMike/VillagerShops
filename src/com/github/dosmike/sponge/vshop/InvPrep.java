package com.github.dosmike.sponge.vshop;

import java.util.LinkedList;
import java.util.List;

import org.spongepowered.api.entity.living.player.Player;
import org.spongepowered.api.item.inventory.custom.CustomInventory;
import org.spongepowered.api.item.inventory.property.SlotIndex;

public class InvPrep {
	List<StockItem> items = new LinkedList<StockItem>();
	
	public void addItem(StockItem si) {
		items.add(si);
	}
	
	public void removeIndex(int i) {
		items.remove(i);
	}
	
	public CustomInventory getInventory() {
		CustomInventory r = CustomInventory.builder()
				.size( ((int)(items.size()/9)+1)*3-9 )
				.build();
		
		int of = 0, ii=0;
		for (int i = 0; i < items.size(); i++) {
			r.set(new SlotIndex(of+ii),   items.get(i).getBuyDisplayItem());
			r.set(new SlotIndex(of+ii+9), items.get(i).getSellDisplayItem());
			
			ii++;
			if (ii>9) { of+=3*9; ii=0; }
		}
		
		return r;
	}
	
	/** @returns 0 for buy, 1 for sell, 2 for spacer */
	public int isSlotBuySell(int inventoryIndex) {
		return ((int)(inventoryIndex/9))%3;
	}
	
	public int itemClicked(Player player, int inventoryIndex) {
		int y = (int)(inventoryIndex/9);
		int a = y%3; y/=3;
		if (a==2) return -1;
		int x = inventoryIndex%9;
		int i = y*9+x;
		
		return (a==0?items.get(i).buy(player):items.get(i).sell(player));
	}
	
}
