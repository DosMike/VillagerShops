package de.dosmike.sponge.vshop.systems.pluginfilter;

import de.dosmike.sponge.vshop.systems.ShopType;
import org.spongepowered.api.item.inventory.ItemStack;
import org.spongepowered.api.item.inventory.ItemStackSnapshot;

import java.util.Optional;

public class DummyItemFilter implements PluginItemFilter {

    String itemId;
    private DummyItemFilter() {}
    private DummyItemFilter(String itemId) { this.itemId = itemId; }

    public static DummyItemFilter of(String pluginItemId) {
        return new DummyItemFilter(pluginItemId);
    }

    public PluginItemFilter get() throws FilterResolutionException {
        return PluginItemServiceImpl.getItemFilter(itemId).orElseThrow(()->new FilterResolutionException("Plugin Item Filter for ID "+itemId+" is no longer provided!"));
    }

    @Override
    public String toString() {
        return itemId;
    }

    @Override
    public boolean isItem(ItemStack item) {
        throw new IllegalStateException("Dummy Filter was not resolved");
    }

    @Override
    public ItemStack supply(int amount, ShopType shopType) {
        throw new IllegalStateException("Dummy Filter was not resolved");
    }

    @Override
    public Optional<ItemStackSnapshot> getDisplayItem(ShopType shopType) {
        throw new IllegalStateException("Dummy Filter was not resolved");
    }
}
