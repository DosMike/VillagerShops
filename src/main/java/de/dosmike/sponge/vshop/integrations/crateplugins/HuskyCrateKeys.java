package de.dosmike.sponge.vshop.integrations.crateplugins;

import com.codehusky.huskycrates.HuskyCrates;
import de.dosmike.sponge.vshop.VillagerShops;
import de.dosmike.sponge.vshop.systems.pluginfilter.PluginItemService;

import java.util.HashSet;
import java.util.Set;

public class HuskyCrateKeys implements KeysFilterProvider {

	private static final String hckid = "huskycrates:";

	private static final Set<String> knownKeyIDs = new HashSet<>();

	HuskyCrateKeys() {
		VillagerShops.l(" > HuskyCrate Integration was loaded");
	}

	@Override
	public void updateFilters(PluginItemService pis) {
		if (HuskyCrates.registry == null) { return; }
		Set<String> activeKeyIDs = HuskyCrates.registry
				.getAllKeys()
				.keySet();
		Set<String> retiredKeyIDs = new HashSet<>(knownKeyIDs);
		retiredKeyIDs.removeAll(activeKeyIDs);
		Set<String> newKeyIDs = new HashSet<>(activeKeyIDs);
		newKeyIDs.removeAll(knownKeyIDs);

		if (retiredKeyIDs.isEmpty() && newKeyIDs.isEmpty()) return;
		VillagerShops.l("HuskyCrate Keys Changed:");
		retiredKeyIDs.forEach(key -> {
			pis.unregisterItemFilter(hckid + key);
			knownKeyIDs.remove(key);
		});
		newKeyIDs.forEach(key -> {
			pis.registerItemFilter(hckid + key, new HuskyCrateKeyFilter(key));
			knownKeyIDs.add(key);
		});
	}
}
