package de.dosmike.sponge.vshop;

import java.io.File;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.Random;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.spongepowered.api.Sponge;
import org.spongepowered.api.config.DefaultConfig;
import org.spongepowered.api.entity.Entity;
import org.spongepowered.api.entity.living.player.Player;
import org.spongepowered.api.event.Listener;
import org.spongepowered.api.event.cause.Cause;
import org.spongepowered.api.event.game.state.GameInitializationEvent;
import org.spongepowered.api.event.game.state.GameStartedServerEvent;
import org.spongepowered.api.event.game.state.GameStoppingEvent;
import org.spongepowered.api.event.service.ChangeServiceProviderEvent;
import org.spongepowered.api.plugin.Plugin;
import org.spongepowered.api.scheduler.Task;
import org.spongepowered.api.service.economy.Currency;
import org.spongepowered.api.service.economy.EconomyService;
import org.spongepowered.api.world.Location;
import org.spongepowered.api.world.World;

import com.google.common.reflect.TypeToken;
import com.google.inject.Inject;

import de.dosmike.sponge.languageservice.API.LanguageService;
import de.dosmike.sponge.languageservice.API.PluginTranslation;
import ninja.leaping.configurate.ConfigurationNode;
import ninja.leaping.configurate.ConfigurationOptions;
import ninja.leaping.configurate.commented.CommentedConfigurationNode;
import ninja.leaping.configurate.loader.ConfigurationLoader;
import ninja.leaping.configurate.objectmapping.serialize.TypeSerializerCollection;
import ninja.leaping.configurate.objectmapping.serialize.TypeSerializers;

@Plugin(id="vshop", name="VillagerShops", version="1.3", authors={"DosMike"})
public class VillagerShops {
	
	public static void main(String[] args) { System.err.println("This plugin can not be run as executable!"); }
	
	static VillagerShops instance;
	public static VillagerShops getInstance() { return instance; }
	
	private EconomyService economyService = null;
	private PluginTranslation translator = null;
	@Listener
	public void onChangeServiceProvider(ChangeServiceProviderEvent event) {
		if (event.getService().equals(EconomyService.class)) {
			economyService = (EconomyService) event.getNewProvider();
		} else if (event.getService().equals(LanguageService.class)) {
			LanguageService languageService = (LanguageService) event.getNewProvider();
			translator = languageService.registerTranslation(this); //add this plugin to langswitch
		}
	}
	public static PluginTranslation getTranslator() { return instance.translator; }
	public static EconomyService getEconomy() { return instance.economyService; }
	
	@Inject
	private Logger logger;
	public static void l(String format, Object... args) { instance.logger.info(String.format(format, args)); }
	public static void w(String format, Object... args) { instance.logger.warn(String.format(format, args)); }
	
	public static Random rng = new Random(System.currentTimeMillis());
	
	/// --- === Main Plugin stuff === --- \\\ 
	
	static List<NPCguard> npcs = new LinkedList<NPCguard>();

	/** to prevent bugs from spamming actions, we'll use this to lock any actions
	 * until 1 tick after the inventory handler finished */
	static Set<UUID> actionUnstack = new HashSet<UUID>(); 
	
	/** remembers what player is viewing what shop as Player <-> Shop mapping */
	static Map<UUID, UUID> openShops = new HashMap<UUID, UUID>();
	
	@Inject
	@DefaultConfig(sharedRoot = false)
	private ConfigurationLoader<CommentedConfigurationNode> configManager;
	public static TypeSerializerCollection customSerializer = TypeSerializers.getDefaultSerializers().newChild();
	
	@Listener
	public void onServerInit(GameInitializationEvent event) {
		instance = this;
		
		customSerializer.registerType(TypeToken.of(StockItem.class), new StockItemSerializer());
		customSerializer.registerType(TypeToken.of(NPCguard.class), new NPCguardSerializer());
		
		Sponge.getEventManager().registerListeners(this, new EventListeners());
	}
	
	@Listener
	public void onServerStart(GameStartedServerEvent event) {
		CommandRegistra.register();
		
		loadConfigs();
		startTimers();
		
		l("VillagerShops is now ready!");
	}
	
	@Listener
	public void onServerStopping(GameStoppingEvent event) {
		terminateNPCs();
		saveConfigs();
		npcs.clear();
	}
	
	public void loadConfigs() {
		npcs.clear();
		
		//move legacy config
		File lc = new File("config/vhop.conf");
		if (lc.exists() && lc.isFile()) {
			w("Found legacy config, moving config/vshop.conf to config/cshop/vshop.conf");
			try {
				File nfc = new File("config/vshop/vshop.conf");
				nfc.getParentFile().mkdirs();
				lc.renameTo(nfc);
			} catch (Exception e) {
				logger.error("VillagerShops was unable to move your config to the new location - You'll have to do this manually or your shops won't load!");
			}
		}
		
		ConfigurationOptions options = ConfigurationOptions.defaults().setSerializers(customSerializer);
		try {
			ConfigurationNode root = configManager.load(options);
			npcs = root.getNode("shops").getValue(NPCguardSerializer.tokenListNPCguard);
		} catch (Exception e1) {
			e1.printStackTrace();
		} finally {
			if (npcs == null) npcs = new LinkedList<>();
		}
	}
	Currency CurrencyByName(String name) {
		if (name != null) for (Currency c : economyService.getCurrencies()) if (c.getName().equals(name)) return c; return economyService.getDefaultCurrency();
	}
	
	public void saveConfigs() {
		ConfigurationOptions options = ConfigurationOptions.defaults().setSerializers(customSerializer);
		try {
			ConfigurationNode root = configManager.createEmptyNode(options);
			root.getNode("shops").setValue(NPCguardSerializer.tokenListNPCguard, npcs);
			configManager.save(root);
		} catch (Exception e1) {
			e1.printStackTrace();
		}
	}
	
	public static void terminateNPCs() {
		stopTimers();

		closeShopInventories();
		
		for (NPCguard npc : npcs) {
			Entity e = npc.getLe();
			if (e != null) e.remove();
		}
		//npcs.clear();
	}
	public static void stopTimers() {
		Set<Task> tasks = Sponge.getScheduler().getScheduledTasks(instance);
		for (Task t : tasks) t.cancel();
	}
	
	public static void startTimers() {
		Sponge.getScheduler().createSyncExecutor(instance).scheduleWithFixedDelay(
				new Runnable() {
					@Override
					public void run() {
						for (NPCguard npc : npcs)
							npc.tick();

					}
				}, 100, 100, TimeUnit.MILLISECONDS);
	}
	
	/** returns the NPCguard index */
	public static Optional<NPCguard> getNPCfromLocation(Location<World> loc2) {
		for (int i = 0; i < npcs.size(); i++) {
			Location<World> loc1 = npcs.get(i).getLoc(); 
			if (loc1.getBlockPosition().equals(loc2.getBlockPosition()) && loc1.getExtent().equals(loc2.getExtent())) {
				return Optional.of(npcs.get(i));
			}
		}
		return Optional.empty();
	}
	public static Optional<NPCguard> getNPCfromShopUUID(UUID uid) {
		for (NPCguard shop : npcs) {
			if (shop.getIdentifier().equals(uid)) 
				return Optional.of(shop);
		}
		return Optional.empty();
	}
	
	public static boolean isNPCused(Entity ent) {
		for (NPCguard npc : npcs) {
			if (ent.equals(npc.getLe()))
				return true;
		}
		return false;
	}
	
	public static void closeShopInventories() {
		for (Entry<UUID,UUID> shop : openShops.entrySet()) {
			Player p = Sponge.getServer().getPlayer(shop.getKey()).orElse(null);
			if (p != null) p.closeInventory(Cause.builder().named("PLUGIN", instance).build());
		}
		openShops.clear();
	}
	public static void closeShopInventories(UUID shopID) {
		List<UUID> rem = new LinkedList<UUID>();
		for (Entry<UUID,UUID> shop : openShops.entrySet()) {
			if (shop.getValue().equals(shopID)) {
				Player p = Sponge.getServer().getPlayer(shop.getKey()).orElse(null);
				if (p != null) {
					p.closeInventory(Cause.builder().named("PLUGIN", instance).build());
					rem.add(shop.getKey());
				}
			}
		}
		for (UUID r : rem) {
			openShops.remove(r);
			actionUnstack.remove(r);
		}
	}
	public static void closeShopInventory(UUID player) {
		Player p = Sponge.getServer().getPlayer(player).orElse(null);
		if (p != null) {
			p.closeInventory(Cause.builder().named("PLUGIN", instance).build());
			openShops.remove(player);
			actionUnstack.remove(player);
		}
	}
}
