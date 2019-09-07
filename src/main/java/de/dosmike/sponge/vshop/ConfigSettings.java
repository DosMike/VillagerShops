package de.dosmike.sponge.vshop;

import de.dosmike.sponge.vshop.menus.InvPrep;
import ninja.leaping.configurate.ConfigurationNode;

/** intermediate class to store config values more accessible */
public class ConfigSettings {

    private static boolean allowAutoDownloads=false;
    private static InvPrep.QuantityValues shopsDefaultStackSize= InvPrep.QuantityValues.FULL;
    private static boolean smartClickEnabled=true;

    public static boolean isAutoDownloadingAllowed() {
        return allowAutoDownloads;
    }
    public static InvPrep.QuantityValues getShopsDefaultStackSize() {
        return shopsDefaultStackSize;
    }
    public static boolean isSmartClickEnabled() {
        return smartClickEnabled;
    }

    static void loadFromConfig(ConfigurationNode node) {
        allowAutoDownloads = node.getNode("AutoDownload").getBoolean(false);
        shopsDefaultStackSize = InvPrep.QuantityValues.fromString(node.getNode("DefaultStackSize").getString(""));
        smartClickEnabled = node.getNode("SmartClick").getBoolean(false);
    }

}
