package de.dosmike.sponge.vshop.shops;

import com.google.common.reflect.TypeToken;
import de.dosmike.sponge.vshop.Utilities;
import de.dosmike.sponge.vshop.VillagerShops;
import de.dosmike.sponge.vshop.systems.pluginfilter.DummyItemFilter;
import de.dosmike.sponge.vshop.systems.ShopType;
import de.dosmike.sponge.vshop.systems.pluginfilter.FilterResolutionException;
import ninja.leaping.configurate.ConfigurationNode;
import ninja.leaping.configurate.objectmapping.ObjectMappingException;
import ninja.leaping.configurate.objectmapping.serialize.TypeSerializer;
import org.jetbrains.annotations.NotNull;
import org.spongepowered.api.item.inventory.ItemStackSnapshot;

@SuppressWarnings("UnstableApiUsage")
public class StockItemSerializer implements TypeSerializer<StockItem> {

	@Override
	public void serialize(@NotNull TypeToken<?> arg0, StockItem item, @NotNull ConfigurationNode value) throws ObjectMappingException {
		ItemStackSnapshot saveItem;
		try {
			saveItem = item.getItem(ShopType.AdminShop);
		} catch (FilterResolutionException e) {
			VillagerShops.w("Did not save a shop listing: %s", e.getMessage());
			return;
		}
		if (item.getBuyPrice() != null) value.getNode("buyprice").setValue(item.getBuyPrice());
		if (item.getSellPrice() != null) value.getNode("sellprice").setValue(item.getSellPrice());
		if (item.getCurrency() != null) value.getNode("currency").setValue(item.getCurrency().getId());
		if (item.getMaxStock() > 0) value.getNode("stocklimit").setValue(item.getMaxStock());
		value.getNode("nbtfilter").setValue(item.getNbtFilter().toString());
		if (item.getNbtFilter().equals(StockItem.FilterOptions.OREDICT)) {
			value.getNode("oredict").setValue(item.getFilterNameExtra().get());
		} else if (item.getNbtFilter().equals(StockItem.FilterOptions.PLUGIN)) {
			value.getNode("pluginitem").setValue(item.getFilterNameExtra().get());
			value.getNode("itemstack").setValue(TypeToken.of(ItemStackSnapshot.class), saveItem);
		} else {
			value.getNode("itemstack").setValue(TypeToken.of(ItemStackSnapshot.class), saveItem);
		}
	}

	@Override
	public StockItem deserialize(@NotNull TypeToken<?> arg0, ConfigurationNode ii) throws ObjectMappingException {
		StockItem.FilterOptions filter = StockItem.FilterOptions.of(ii.getNode("nbtfilter").getString("NORMAL"));
		if (filter.equals(StockItem.FilterOptions.OREDICT)) {
			try {
				return new StockItem(
						ii.getNode("oredict").getString(),
						ii.getNode("sellprice").getDouble(-1),
						ii.getNode("buyprice").getDouble(-1),
						Utilities.CurrencyByName(ii.getNode("currency").getString(null)),
						ii.getNode("stocklimit").getInt(0)
				);
			} catch (IllegalArgumentException e) {
				throw new ObjectMappingException(e);
			}
		} else if (filter.equals(StockItem.FilterOptions.PLUGIN)) {
			try {
				return new StockItem(
						ii.getNode("itemstack").getValue(TypeToken.of(ItemStackSnapshot.class)),
						DummyItemFilter.of(ii.getNode("pluginitem").getString()),
						ii.getNode("sellprice").getDouble(-1),
						ii.getNode("buyprice").getDouble(-1),
						Utilities.CurrencyByName(ii.getNode("currency").getString(null)),
						ii.getNode("stocklimit").getInt(0)
				);
			} catch (IllegalArgumentException e) {
				throw new ObjectMappingException(e);
			}
		} else {
			return new StockItem(
					ii.getNode("itemstack").getValue(TypeToken.of(ItemStackSnapshot.class)),
					ii.getNode("sellprice").getDouble(-1),
					ii.getNode("buyprice").getDouble(-1),
					Utilities.CurrencyByName(ii.getNode("currency").getString(null)),
					ii.getNode("stocklimit").getInt(0),
					filter
			);
		}
	}
}
