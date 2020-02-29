package de.dosmike.sponge.vshop.systems;

import de.dosmike.sponge.vshop.VillagerShops;
import org.spongepowered.api.Sponge;
import org.spongepowered.api.item.ItemType;

import java.util.*;

public class PluginItemServiceImpl implements PluginItemService {

    private static Map<String, ItemFilter> registry = new HashMap<>();

    @Override
    public void registerItemFilter(String pluginItemId, ItemFilter filter) {
        if (pluginItemId==null || !pluginItemId.matches("\\w+:\\w+"))
            throw new IllegalArgumentException("Please follow common id string syntax with namespace:itemid");
        if (filter == null)
            throw new IllegalArgumentException("Filter can't be null");
        pluginItemId = pluginItemId.toLowerCase();
        if (Sponge.getRegistry().getType(ItemType.class, pluginItemId).isPresent())
            VillagerShops.w("PluginItemId %s is ItemType ID. This is probably incorrect.", pluginItemId);
        if (registry.containsKey(pluginItemId))
            throw new IllegalStateException("Tried to register "+pluginItemId+" twice");
        registry.put(pluginItemId, filter);
    }

    @Override
    public void unregisterItemFilter(String pluginItemId) {
        if (pluginItemId==null || !pluginItemId.matches("\\w+:\\w+"))
            throw new IllegalArgumentException("Please follow common id string syntax with namespace:itemid");
        registry.remove(pluginItemId.toLowerCase());
    }

    public static Optional<ItemFilter> getItemFilter(String pluginItemId) {
        return Optional.ofNullable(registry.get(pluginItemId.toLowerCase()));
    }
    public static Set<String> getRegisteredIds() {
        return new HashSet<>(registry.keySet());
    }

}
