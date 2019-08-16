package de.dosmike.sponge.vshop.shops;

import com.google.common.reflect.TypeToken;
import de.dosmike.sponge.vshop.Utilities;
import ninja.leaping.configurate.ConfigurationNode;
import ninja.leaping.configurate.objectmapping.ObjectMappingException;
import ninja.leaping.configurate.objectmapping.serialize.TypeSerializer;
import org.spongepowered.api.item.inventory.ItemStack;

public class StockItemSerializer implements TypeSerializer<StockItem> {

    @Override
    public void serialize(TypeToken<?> arg0, StockItem item, ConfigurationNode value) throws ObjectMappingException {
        if (item.getBuyPrice() != null) value.getNode("buyprice").setValue(item.getBuyPrice());
        if (item.getSellPrice() != null) value.getNode("sellprice").setValue(item.getSellPrice());
        if (item.getCurrency() != null) value.getNode("currency").setValue(item.getCurrency().getId());
        if (item.getMaxStock() > 0) value.getNode("stocklimit").setValue(item.getMaxStock());
        value.getNode("nbtfilter").setValue(item.getNbtFilter().toString());
        if (item.getNbtFilter().equals(StockItem.FilterOptions.OREDICT)) {
            value.getNode("oredict").setValue(item.getOreDictEntry().get());
        } else {
            value.getNode("itemstack").setValue(TypeToken.of(ItemStack.class), item.getItem());
        }
    }

    @Override
    public StockItem deserialize(TypeToken<?> arg0, ConfigurationNode ii) throws ObjectMappingException {
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
        } else {
            return new StockItem(
                    ii.getNode("itemstack").getValue(TypeToken.of(ItemStack.class)),
                    ii.getNode("sellprice").getDouble(-1),
                    ii.getNode("buyprice").getDouble(-1),
                    Utilities.CurrencyByName(ii.getNode("currency").getString(null)),
                    ii.getNode("stocklimit").getInt(0),
                    filter
            );
        }
    }
}
