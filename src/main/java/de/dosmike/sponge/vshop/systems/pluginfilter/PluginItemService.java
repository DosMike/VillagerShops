package de.dosmike.sponge.vshop.systems.pluginfilter;

import org.spongepowered.api.event.game.state.GameLoadCompleteEvent;
import org.spongepowered.api.event.service.ChangeServiceProviderEvent;

public interface PluginItemService {

	/**
	 * Register an {@link PluginItemFilter} for your plugin.
	 * Registering filters has to happen before VillagerShop tries to load shops. Shops
	 * are loaded with worlds, so the latest possible event for registering would be
	 * {@link GameLoadCompleteEvent}. It's recommended to register the filters directly
	 * within {@link ChangeServiceProviderEvent}
	 *
	 * @param pluginItemId a custom id you specify for your item. e.g.: myplugin:votekey
	 * @param filter       the ItemFilter handling this item
	 */
	void registerItemFilter(String pluginItemId, PluginItemFilter filter);

	/**
	 * Remove an {@link PluginItemFilter} by it's id.
	 *
	 * @param pluginItemId the id of the filter to remove
	 */
	void unregisterItemFilter(String pluginItemId);

}
