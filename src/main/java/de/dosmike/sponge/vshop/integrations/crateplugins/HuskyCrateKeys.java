package de.dosmike.sponge.vshop.integrations.crateplugins;

import de.dosmike.sponge.vshop.systems.PluginItemService;
import org.spongepowered.api.data.DataQuery;

public class HuskyCrateKeys implements KeysFilterProvider {

    private static final DataQuery HCKID = DataQuery.of("UnsafeData","HCKEYID"); // as String
    private static final DataQuery HCKUUID = DataQuery.of("UnsafeData","HCKEYUUID"); // as String
    private static final String hckid = "huskycrates:";

    HuskyCrateKeys() {
        //TODO register HuskyCrates.registry update listener (maybe hook #reload)
    }

    @Override
    public void registerFilters(PluginItemService pis) {

    }
}
