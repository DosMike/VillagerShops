package de.dosmike.sponge.vshop.webapi.packets;

import org.spongepowered.api.item.inventory.ItemStackSnapshot;
import org.spongepowered.api.service.economy.Currency;

import de.dosmike.sponge.vshop.StockItem;
import de.dosmike.sponge.vshop.VillagerShops;
import valandur.webapi.shadow.com.fasterxml.jackson.databind.annotation.JsonDeserialize;

public class StockItemPacket extends apiPacket {
	
	@JsonDeserialize
	private Double buyPrice=null;
	public Double getBuyPrice() {
		return buyPrice;
	}
	
	@JsonDeserialize
	private Double sellPrice=null;
	public Double getSellPrice() {
		return sellPrice;
	}
	
	@JsonDeserialize
	private Integer stockLimit=null;
	public Integer getStockLimit() {
		return stockLimit;
	}
	@JsonDeserialize
	private Integer stockAmount=null;
	public Integer getStockAmount() {
		return stockAmount;
	}
	
	@JsonDeserialize
	private String currency=null;
	public String getCurrency() {
		return currency;
	}
	
	@JsonDeserialize
	private ItemStackSnapshot itemStack=null;
	public ItemStackSnapshot getItem() {
		return itemStack;
	}
	
	public StockItemPacket() {}
	public StockItemPacket(StockItem sitem) {
		buyPrice = sitem.getBuyPrice();
		sellPrice = sitem.getSellPrice();
		stockLimit = sitem.getMaxStock();
		if (stockLimit != null) stockAmount = sitem.getStocked(); 
		currency = sitem.getCurrency().getName();
		itemStack = sitem.getItem().createSnapshot();
	}
	
	public StockItem execute() {
		Currency c = VillagerShops.getInstance().CurrencyByName((String) currency);
		if (c==null || (buyPrice==null&&sellPrice==null)) return null;
		return new StockItem(getItem().createStack(), sellPrice, buyPrice, c, stockLimit); 
	}
}
