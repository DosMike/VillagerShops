package de.dosmike.sponge.vshop;

import de.dosmike.sponge.vshop.menus.ShopMenuManager;
import de.dosmike.sponge.vshop.systems.ItemNBTCleaner;
import ninja.leaping.configurate.ConfigurationNode;
import org.spongepowered.api.util.TypeTokens;

/** intermediate class to store config values more accessible */
public class ConfigSettings {

    private static boolean allowAutoDownloads=false;
    private static ShopMenuManager.QuantityValues shopsDefaultStackSize= ShopMenuManager.QuantityValues.FULL;
    private static boolean smartClickEnabled=true;
    private static boolean recordAuditLogs=false;
    private static boolean animateShops=false;

    public static boolean isAutoDownloadingAllowed() {
        return allowAutoDownloads;
    }
    public static ShopMenuManager.QuantityValues getShopsDefaultStackSize() {
        return shopsDefaultStackSize;
    }
    public static boolean isSmartClickEnabled() {
        return smartClickEnabled;
    }
    public static boolean recordAuditLogs() { return recordAuditLogs; }
    public static boolean areShopsAnimated() { return animateShops; }

    @SuppressWarnings("UnstableApiUsage")
    static void loadFromConfig(ConfigurationNode node) {
        allowAutoDownloads = node.getNode("AutoDownload").getBoolean(false);
        shopsDefaultStackSize = ShopMenuManager.QuantityValues.fromString(node.getNode("DefaultStackSize").getString(""));
        smartClickEnabled = node.getNode("SmartClick").getBoolean(false);
        recordAuditLogs = node.getNode("AuditLogs").getBoolean(false);
        animateShops = node.getNode("AnimateShops").getBoolean(true);

        ItemNBTCleaner.clear();
        try {
            node.getNode("NBTblacklist").getList(TypeTokens.STRING_TOKEN).forEach(ItemNBTCleaner::addQuery);
        } catch (Exception e) {
            VillagerShops.w("Could not read NBT blacklist: %s", e.getMessage());
        }
    }

}
