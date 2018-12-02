package de.dosmike.sponge.vshop;

import com.google.common.reflect.TypeToken;
import ninja.leaping.configurate.ConfigurationNode;
import ninja.leaping.configurate.objectmapping.ObjectMappingException;
import ninja.leaping.configurate.objectmapping.serialize.TypeSerializer;
import org.spongepowered.api.item.inventory.ItemStack;

public class StockItemSerializer implements TypeSerializer<StockItem> {

    @Override
    public void serialize(TypeToken<?> arg0, StockItem item, ConfigurationNode value) throws ObjectMappingException {
        value.getNode("itemstack").setValue(TypeToken.of(ItemStack.class), item.getItem());
        if (item.getBuyPrice() != null) value.getNode("buyprice").setValue(item.getBuyPrice());
        if (item.getSellPrice() != null) value.getNode("sellprice").setValue(item.getSellPrice());
        if (item.getCurrency() != null) value.getNode("currency").setValue(item.getCurrency().getId());
        if (item.getMaxStock() > 0) value.getNode("stocklimit").setValue(item.getMaxStock());
    }

    @Override
    public StockItem deserialize(TypeToken<?> arg0, ConfigurationNode ii) throws ObjectMappingException {
        return new StockItem(
                ii.getNode("itemstack").getValue(TypeToken.of(ItemStack.class)),
                ii.getNode("sellprice").getDouble(-1),
                ii.getNode("buyprice").getDouble(-1),
                VillagerShops.getInstance().CurrencyByName(ii.getNode("currency").getString(null)),
                ii.getNode("stocklimit").getInt(0)
        );
    }
}
