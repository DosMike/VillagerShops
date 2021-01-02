package de.dosmike.sponge.vshop;

import de.dosmike.sponge.vshop.integrations.protection.ProtectionAccessLevel;
import de.dosmike.sponge.vshop.menus.ShopMenuManager;
import de.dosmike.sponge.vshop.systems.ItemNBTCleaner;
import ninja.leaping.configurate.ConfigurationNode;
import ninja.leaping.configurate.ConfigurationOptions;
import ninja.leaping.configurate.commented.CommentedConfigurationNode;
import ninja.leaping.configurate.hocon.HoconConfigurationLoader;
import org.spongepowered.api.Sponge;
import org.spongepowered.api.util.TypeTokens;

import java.io.IOException;
import java.util.Map;

/**
 * intermediate class to store config values more accessible
 */
public class ConfigSettings {

	private static boolean allowAutoDownloads = false;
	private static ShopMenuManager.QuantityValues shopsDefaultStackSize = ShopMenuManager.QuantityValues.FULL;
	private static boolean smartClickEnabled = true;
	private static boolean recordAuditLogs = false;
	private static boolean animateShops = false;
	private static ProtectionAccessLevel protectionAccessLevel = ProtectionAccessLevel.CONTAINER;
	private static boolean protectionAllowWilderness = false;

	public static boolean isAutoDownloadingAllowed() {
		return allowAutoDownloads;
	}

	public static ShopMenuManager.QuantityValues getShopsDefaultStackSize() {
		return shopsDefaultStackSize;
	}

	public static boolean isSmartClickEnabled() {
		return smartClickEnabled;
	}

	public static boolean recordAuditLogs() {
		return recordAuditLogs;
	}

	public static boolean areShopsAnimated() {
		return animateShops;
	}

	public static boolean isProtectionEnabled() {
		return protectionAccessLevel != ProtectionAccessLevel.IGNORED;
	}

	public static boolean protectionAllowWilderness() {
		return protectionAllowWilderness;
	}

	public static ProtectionAccessLevel requiredProtectionAccessLevel() {
		return protectionAccessLevel;
	}

	@SuppressWarnings("UnstableApiUsage")
	static void loadFromConfig(ConfigurationNode node) {
		allowAutoDownloads = node.getNode("AutoDownload").getBoolean(false);
		shopsDefaultStackSize = ShopMenuManager.QuantityValues.fromString(node.getNode("DefaultStackSize").getString(""));
		smartClickEnabled = node.getNode("SmartClick").getBoolean(false);
		recordAuditLogs = node.getNode("AuditLogs").getBoolean(false);
		animateShops = node.getNode("AnimateShops").getBoolean(true);
		protectionAccessLevel = ProtectionAccessLevel.fromString(node.getNode("ProtectionIntegration").getString());
		protectionAllowWilderness = node.getNode("ProtectionAllowWilderness").getBoolean(false);

		ItemNBTCleaner.clear();
		try {
			node.getNode("NBTblacklist").getList(TypeTokens.STRING_TOKEN).forEach(ItemNBTCleaner::addQuery);
		} catch (Exception e) {
			VillagerShops.w("Could not read NBT blacklist: %s", e.getMessage());
		}
	}

	/**
	 * @return true if new options were detected and merged from the default config
	 */
	static boolean injectNewOptions(CommentedConfigurationNode node) throws IOException {
		boolean changed = false;

		HoconConfigurationLoader defaultLoader = HoconConfigurationLoader.builder()
				.setURL(Sponge.getAssetManager().getAsset(VillagerShops.getInstance(), "default_settings.conf").get().getUrl())
				.build();

		CommentedConfigurationNode defaults = defaultLoader.load(ConfigurationOptions.defaults());

		for (Map.Entry<Object, ? extends CommentedConfigurationNode> defaultEntry : defaults.getChildrenMap().entrySet()) {
			CommentedConfigurationNode entry = node.getNode(defaultEntry.getKey());
			if (entry.isVirtual()) {
				entry.mergeValuesFrom(defaultEntry.getValue());
				changed = true;
			}
		}

		return changed;
	}

}
