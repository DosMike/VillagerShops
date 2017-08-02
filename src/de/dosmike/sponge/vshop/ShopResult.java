package de.dosmike.sponge.vshop;

import org.spongepowered.api.text.Text;
import org.spongepowered.api.text.format.TextColors;

public class ShopResult {
	int count;
	String msg;
	
	ShopResult(int items, String message) {
		count = items;
		msg = message;
	};
	
	public int getTradedItems() {
		return count;
	}
	public Text getMessage() {
		return Text.of(TextColors.RED, msg);
	}
	
	public static final ShopResult CUSTOMER_LOW_BALANCE = new ShopResult(0, "You do not have enough money");
	public static final ShopResult SHOPOWNER_LOW_BALANCE = new ShopResult(0, "The shop owner ran out of money");
	public static final ShopResult CUSTOMER_MISSING_ITEMS = new ShopResult(0, "You do not have enough items");
	public static final ShopResult SHOPOWNER_MISSING_ITEMS = new ShopResult(0, "The items is currently out of stock");
	public static final ShopResult CUSTOMER_INVENTORY_FULL = new ShopResult(0, "You can not carry any more items");
	public static final ShopResult SHOPOWNER_INVENTORY_FULL = new ShopResult(0, "The shop can not stock more items");
	public static ShopResult OK(int items) { return new ShopResult(items, null); }
}
