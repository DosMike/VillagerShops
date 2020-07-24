package de.dosmike.sponge.vshop.systems;

import de.dosmike.sponge.vshop.VillagerShops;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.api.Sponge;
import org.spongepowered.api.item.ItemType;
import org.spongepowered.api.item.inventory.ItemStack;

import java.util.*;
import java.util.stream.Collectors;

public class PluginItemServiceImpl implements PluginItemService {

    private static final Map<String, PluginItemFilter> registry = new HashMap<>();
    private static final Set<PluginItemFilter> enforcedFilters = new HashSet<>();

    @Override
    public void registerItemFilter(String pluginItemId, PluginItemFilter filter) {
        if (pluginItemId==null || !pluginItemId.matches("\\w+:\\w+"))
            throw new IllegalArgumentException("Please follow common id string syntax with namespace:itemid");
        if (filter == null)
            throw new IllegalArgumentException("Filter can't be null");
        pluginItemId = pluginItemId.toLowerCase();
        if (Sponge.getRegistry().getType(ItemType.class, pluginItemId).isPresent())
            VillagerShops.w("PluginItemId %s is ItemType ID. This is probably incorrect.", pluginItemId);
        if (registry.containsKey(pluginItemId))
            throw new IllegalStateException("Tried to register "+pluginItemId+" twice");
        if (filter.enforce())
            enforcedFilters.add(filter);
        registry.put(pluginItemId, filter);
        VillagerShops.l("Added Plugin Filter "+pluginItemId);
    }

    @Override
    public void unregisterItemFilter(String pluginItemId) {
        if (pluginItemId==null || !pluginItemId.matches("\\w+:\\w+"))
            throw new IllegalArgumentException("Please follow common id string syntax with namespace:itemid");
        PluginItemFilter filter = registry.remove(pluginItemId.toLowerCase());
        if (filter != null) enforcedFilters.remove(filter);
        VillagerShops.l("Removed Plugin Filter "+pluginItemId);
    }

    public static Optional<PluginItemFilter> getEnforcedFilter(ItemStack stack) {
        List<PluginItemFilter> filter = enforcedFilters.stream().filter(f->f.isItem(stack)).collect(Collectors.toList());
        if (filter.size()>1)
            throw new RuntimeException("Multiple filters tried to enforce on an item: "+filter.stream().map(f->getFilterID(f).orElse("?")).collect(Collectors.joining(", ")));
        if (filter.isEmpty())
            return Optional.empty();
        return Optional.of(filter.get(0));
    }
    public static Optional<PluginItemFilter> getItemFilter(@Nullable String pluginItemId) {
        if (pluginItemId == null) return Optional.empty();
        return Optional.ofNullable(registry.get(pluginItemId.toLowerCase()));
    }
    public static Optional<String> getFilterID(PluginItemFilter filter) {
        return registry.entrySet().stream()
                .filter(e->e.getValue().equals(filter))
                .map(Map.Entry::getKey)
                .findFirst();
    }
    public static Set<String> getRegisteredIds() {
        return new HashSet<>(registry.keySet());
    }

}
