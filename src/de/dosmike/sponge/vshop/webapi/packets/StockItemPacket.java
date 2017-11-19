package de.dosmike.sponge.vshop.webapi.packets;

import org.spongepowered.api.item.inventory.ItemStack;
import org.spongepowered.api.item.inventory.ItemStackSnapshot;
import org.spongepowered.api.service.economy.Currency;

import de.dosmike.sponge.vshop.StockItem;
import de.dosmike.sponge.vshop.VillagerShops;
import valandur.webapi.shadow.com.fasterxml.jackson.annotation.JsonIgnore;
import valandur.webapi.shadow.com.fasterxml.jackson.annotation.JsonProperty;

public class StockItemPacket extends apiPacket {
	
	@JsonProperty
	private Double buyPrice=null;
	@JsonIgnore
	public Double getBuyPrice() {
		return buyPrice;
	}
	
	@JsonProperty
	private Double sellPrice=null;
	@JsonIgnore
	public Double getSellPrice() {
		return sellPrice;
	}
	
	@JsonProperty
	private Integer stockLimit=null;
	@JsonIgnore
	public Integer getStockLimit() {
		return stockLimit;
	}
	@JsonProperty
	private Integer stockAmount=null;
	@JsonIgnore
	public Integer getStockAmount() {
		return stockAmount;
	}
	
	@JsonProperty
	private String currency=null;
	@JsonIgnore
	public String getCurrency() {
		return currency;
	}
	
	@JsonProperty
	private ItemStack itemStack=null;
	@JsonIgnore
	public ItemStackSnapshot getItem() {
		return itemStack.createSnapshot();
	}
	
	public StockItemPacket() {}
	public StockItemPacket(StockItem sitem) {
		buyPrice = sitem.getBuyPrice();
		sellPrice = sitem.getSellPrice();
		stockLimit = sitem.getMaxStock();
		if (stockLimit != null) stockAmount = sitem.getStocked(); 
		currency = sitem.getCurrency().getName();
		itemStack = sitem.getItem().copy();
	}
	
	@JsonIgnore
	public StockItem execute() {
		Currency c = VillagerShops.getInstance().CurrencyByName((String) currency);
		if (c==null || (buyPrice==null&&sellPrice==null)) return null;
		return new StockItem(getItem().createStack(), sellPrice, buyPrice, c, stockLimit); 
	}
}
