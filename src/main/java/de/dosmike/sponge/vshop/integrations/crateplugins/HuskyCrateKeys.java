package de.dosmike.sponge.vshop.integrations.crateplugins;

import com.codehusky.huskycrates.HuskyCrates;
import de.dosmike.sponge.vshop.systems.PluginItemService;

import java.util.HashSet;
import java.util.Set;

public class HuskyCrateKeys implements KeysFilterProvider {

    private static final String hckid = "huskycrates:";

    private static Set<String> knownKeyIDs = new HashSet<>();

    HuskyCrateKeys() {

    }

    @Override
    public void updateFilters(PluginItemService pis) {
        Set<String> activeKeyIDs = HuskyCrates.registry.getAllKeys().keySet();
        Set<String> retiredKeyIDs = new HashSet<>(knownKeyIDs);
        retiredKeyIDs.removeAll(activeKeyIDs);
        Set<String> newKeyIDs = new HashSet<>(activeKeyIDs);
        newKeyIDs.removeAll(knownKeyIDs);

        retiredKeyIDs.forEach(key->{
            pis.unregisterItemFilter(hckid+key);
            knownKeyIDs.remove(key);
        });
        newKeyIDs.forEach(key->{
            pis.registerItemFilter(hckid+key, new HuskyCrateKeyFilter(key));
            knownKeyIDs.add(key);
        });
    }
}
