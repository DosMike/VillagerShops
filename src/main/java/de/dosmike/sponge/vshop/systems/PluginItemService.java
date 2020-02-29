package de.dosmike.sponge.vshop.systems;

public interface PluginItemService {

    /** Register an {@link ItemFilter} for your plugin.
     * @param pluginItemId a custom id you specify for your item. e.g.: myplugin:votekey
     * @param filter the ItemFilter handling this item
     */
    void registerItemFilter(String pluginItemId, ItemFilter filter);

    /** remove an {@link ItemFilter} by pluginid
     * @param pluginItemId the id of the filter to remove */
    void unregisterItemFilter(String pluginItemId);

}
