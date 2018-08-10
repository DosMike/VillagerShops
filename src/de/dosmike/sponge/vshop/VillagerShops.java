package de.dosmike.sponge.vshop;

import java.io.File;
import java.math.BigDecimal;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
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
import org.spongepowered.api.config.ConfigDir;
import org.spongepowered.api.config.DefaultConfig;
import org.spongepowered.api.entity.Entity;
import org.spongepowered.api.entity.living.player.Player;
import org.spongepowered.api.event.Listener;
import org.spongepowered.api.event.game.state.GameInitializationEvent;
import org.spongepowered.api.event.game.state.GamePostInitializationEvent;
import org.spongepowered.api.event.game.state.GameStartedServerEvent;
import org.spongepowered.api.event.game.state.GameStoppingEvent;
import org.spongepowered.api.event.service.ChangeServiceProviderEvent;
import org.spongepowered.api.event.world.SaveWorldEvent;
import org.spongepowered.api.plugin.Dependency;
import org.spongepowered.api.plugin.Plugin;
import org.spongepowered.api.plugin.PluginContainer;
import org.spongepowered.api.scheduler.SpongeExecutorService;
import org.spongepowered.api.scheduler.Task;
import org.spongepowered.api.service.economy.Currency;
import org.spongepowered.api.service.economy.EconomyService;
import org.spongepowered.api.service.user.UserStorageService;
import org.spongepowered.api.world.Location;
import org.spongepowered.api.world.World;

import com.google.common.reflect.TypeToken;
import com.google.inject.Inject;

import de.dosmike.sponge.languageservice.API.LanguageService;
import de.dosmike.sponge.languageservice.API.PluginTranslation;
import de.dosmike.sponge.vshop.webapi.VShopServlet;
import ninja.leaping.configurate.ConfigurationNode;
import ninja.leaping.configurate.ConfigurationOptions;
import ninja.leaping.configurate.commented.CommentedConfigurationNode;
import ninja.leaping.configurate.hocon.HoconConfigurationLoader;
import ninja.leaping.configurate.loader.ConfigurationLoader;
import ninja.leaping.configurate.objectmapping.serialize.TypeSerializerCollection;
import ninja.leaping.configurate.objectmapping.serialize.TypeSerializers;

@Plugin(id="vshop", name="VillagerShops",
	version="1.9-pre2", authors={"DosMike"},
	dependencies = {
		@Dependency(id="langswitch", optional=false),
		@Dependency(id="webapi", optional=true)
	})
public class VillagerShops {
	
	public static void main(String[] args) { System.err.println("This plugin can not be run as executable!"); }
	
	static VillagerShops instance;
	public static VillagerShops getInstance() { return instance; }
	
	private EconomyService economyService = null;
	private PluginTranslation translator = null;
	private UserStorageService userStorage = null;
	private IncomeLimiterService incomeLimiter = null;
	private SpongeExecutorService asyncScheduler = null;
	private SpongeExecutorService syncScheduler = null;
	
	@Listener
	public void onChangeServiceProvider(ChangeServiceProviderEvent event) {
		if (event.getService().equals(EconomyService.class)) {
			economyService = (EconomyService) event.getNewProvider();
		} else if (event.getService().equals(LanguageService.class)) {
			LanguageService languageService = (LanguageService) event.getNewProvider();
			translator = languageService.registerTranslation(this); //add this plugin to langswitch
		} else if (event.getService().equals(UserStorageService.class)) {
			userStorage = (UserStorageService) event.getNewProvider();
		}
	}
	public static PluginTranslation getTranslator() { return instance.translator; }
	public static EconomyService getEconomy() { return instance.economyService; }
	public static UserStorageService getUserStorage() { return instance.userStorage; }
	public static IncomeLimiterService getIncomeLimiter() { return instance.incomeLimiter; }
	public static SpongeExecutorService getAsyncScheduler() { return instance.asyncScheduler; }
	public static SpongeExecutorService getSyncScheduler() { return instance.syncScheduler; }
	
	public PluginContainer getContainer() { return Sponge.getPluginManager().fromInstance(this).get(); }
	
	@Inject
	private Logger logger;
	public static void l(String format, Object... args) { instance.logger.info(String.format(format, args)); }
	public static void w(String format, Object... args) { instance.logger.warn(String.format(format, args)); }
	
	public static Random rng = new Random(System.currentTimeMillis());
	
	/// --- === Main Plugin stuff === --- \\\ 

	//save on world if the npcs list is marked dirty
	//this flag should be set every time the npcs list is changed, or a property of a shop changes
	//or a item was added or removed from the shop
	boolean npcsDirty = false;  
	private static List<NPCguard> npcs = new LinkedList<NPCguard>();
	static void addNPCguard(NPCguard add) {
		synchronized(npcs) {
			npcs.add(add);
		}
		instance.npcsDirty = true;
	}
	static Collection<NPCguard> getNPCguards() {
		synchronized(npcs) {
			List<NPCguard> res = new ArrayList<>(npcs.size());
			for (NPCguard g : npcs) res.add(g);
			return res;
		}
	}
	static void removeNPCguard(NPCguard remove) {
		synchronized(npcs) {
			npcs.remove(remove);
		}
		instance.npcsDirty = true;
	}

	/** to prevent bugs from spamming actions, we'll use this to lock any actions
	 * until 1 tick after the inventory handler finished */
	static Set<UUID> actionUnstack = new HashSet<UUID>(); 
	
	/** remembers what player is viewing what shop as Player <-> Shop mapping */
	static Map<UUID, UUID> openShops = new HashMap<UUID, UUID>();
	
	@Inject
	@DefaultConfig(sharedRoot = false)
	private ConfigurationLoader<CommentedConfigurationNode> configManager;
	public static TypeSerializerCollection customSerializer = TypeSerializers.getDefaultSerializers().newChild();
	
	@Inject
	@ConfigDir(sharedRoot = false)
	private Path privateConfigDir;
	
	@Listener
	public void onServerInit(GameInitializationEvent event) {
		instance = this;
		
		incomeLimiter = new IncomeLimiterService();
		asyncScheduler = Sponge.getScheduler().createAsyncExecutor(this);
		syncScheduler = Sponge.getScheduler().createSyncExecutor(this);
		
		customSerializer.registerType(TypeToken.of(StockItem.class), new StockItemSerializer());
		customSerializer.registerType(TypeToken.of(NPCguard.class), new NPCguardSerializer());
		
		Sponge.getEventManager().registerListeners(this, new EventListeners());
	}
	/** Dependency management does not really work within sponge, it relies way to heavily on alphabetical 
	 * sorting event though i specified deps EVERYWHERE i could imagine. <br>
	 * So In order for this hook to load reliably after webapi and before that's continuing i have to use this
	 * intermediate event, whether i like it or not */
	@Listener
	public void onServerPostInit(GamePostInitializationEvent event) {
		try {
			//trick here:
			// if we can't get the class for name an exception is thrown preventing the real depending code from even being looked at.
			// this requires the entry point to not be obfuscated, but for an API that's normally not the case anyways. 
			if (Sponge.getPluginManager().getPlugin("webapi").isPresent())
				VShopServlet.init();
		} catch(Exception e) {
			l("WebAPI not found, skipping");
		}
	}
	
	@Listener
	public void onServerStart(GameStartedServerEvent event) {
		l("Registering commands...");
		CommandRegistra.register();
		
		try {
			userStorage = Sponge.getServiceManager().provide(UserStorageService.class).get();
		} catch (Exception e) {
			w("Unable to fetch user service, sponge:humans won't be skinnable");
			e.printStackTrace();
		}
		
		loadConfigs();
		startTimers();
		
		l("VillagerShops is now ready!");
	}
	
	long lastSave = System.currentTimeMillis();
	@Listener
	public void onWorldsSave(SaveWorldEvent event) {
		if (npcsDirty && System.currentTimeMillis()-lastSave > 10000) { //not more than every 10 seconds
			l("Saving VillagerShops...");
			lastSave = System.currentTimeMillis();
			saveConfigs();
		}
	}
	
	@Listener
	public void onServerStopping(GameStoppingEvent event) {
		terminateNPCs();
		saveConfigs();
//		npcs.clear(); //could interfere with a later save?
	}
	
	public void loadConfigs() {
		synchronized(npcs) {
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
			npcsDirty = false;
		}
		try {
			ConfigurationLoader<CommentedConfigurationNode> limitManager = HoconConfigurationLoader.builder()
					.setPath(privateConfigDir.resolve("incomeLimits.conf")).build();
			ConfigurationNode root = limitManager.load();
			if (!root.getNode("income").isVirtual() && !root.getNode("timestamp").isVirtual()) {
				Map<UUID, BigDecimal> earningValues = new HashMap<>();
				Map<UUID, BigDecimal> spendingValues = new HashMap<>();
				root.getNode("income").getChildrenMap().forEach((k,v)->{
					try {
						earningValues.put(UUID.fromString((String)k), new BigDecimal(v.getString("0")));
					} catch (Exception invalid) {
						invalid.printStackTrace();
					}
				});
				root.getNode("spending").getChildrenMap().forEach((k,v)->{
					try {
						spendingValues.put(UUID.fromString((String)k), new BigDecimal(v.getString("0")));
					} catch (Exception invalid) {
						invalid.printStackTrace();
					}
				});
				long dayStamp = root.getNode("timestamp").getLong(System.currentTimeMillis());
				
				incomeLimiter.loadFromConfig(earningValues, spendingValues, dayStamp);
			}
		} catch (Exception e1) {
		}
	}
	public Currency CurrencyByName(String name) {
		if (name != null) {
			for (Currency c : economyService.getCurrencies()) if (c.getId().equals(name)) return c;
			for (Currency c : economyService.getCurrencies()) if (c.getName().equalsIgnoreCase(name)) return c;
		}
		 return economyService.getDefaultCurrency();
	}
	
	@SuppressWarnings("serial")
	public void saveConfigs() {
		synchronized(npcs) {
			ConfigurationOptions options = ConfigurationOptions.defaults().setSerializers(customSerializer);
			try {
				ConfigurationNode root = configManager.createEmptyNode(options);
				root.getNode("shops").setValue(NPCguardSerializer.tokenListNPCguard, npcs);
				configManager.save(root);
			} catch (Exception e1) {
				e1.printStackTrace();
			}
			npcsDirty = false;
		}
		try {
			ConfigurationLoader<CommentedConfigurationNode> limitManager = HoconConfigurationLoader.builder()
					.setPath(privateConfigDir.resolve("incomeLimits.conf")).build();
			ConfigurationNode root = limitManager.createEmptyNode(ConfigurationOptions.defaults());
			ConfigurationNode sub = root.getNode("income");
			Map<String, String> ser = new HashMap<>();
			for (Entry<UUID, BigDecimal> e : incomeLimiter.getEarnings().entrySet()) {
				ser.put(e.getKey().toString(), e.getValue().toString());
			}
			sub.setValue(new TypeToken<Map<String, String>>() {}, ser);
			sub = root.getNode("spending");
			ser.clear();
			for (Entry<UUID, BigDecimal> e : incomeLimiter.getSpendings().entrySet()) {
				ser.put(e.getKey().toString(), e.getValue().toString());
			}
			sub.setValue(new TypeToken<Map<String, String>>() {}, ser);
			root.getNode("timestamp").setValue(incomeLimiter.forTime());
			
			limitManager.save(root);
		} catch (Exception e1) {
			e1.printStackTrace();
		}
	}
	
	public static void terminateNPCs() {
		stopTimers();

		closeShopInventories();
		
		synchronized(npcs) {
			for (NPCguard npc : npcs) {
				Entity e = npc.getLe();
				if (e != null) e.remove();
			}
		}
		//npcs.clear();
	}
	public static void stopTimers() {
		Set<Task> tasks = Sponge.getScheduler().getScheduledTasks(instance);
		for (Task t : tasks) if (!t.isAsynchronous()) t.cancel(); //async tasks work in the database
	}
	
	static long ledgerChatTimer = System.currentTimeMillis();
	public static void startTimers() {
		getSyncScheduler().scheduleWithFixedDelay(
				new Runnable() {
					@Override
					public void run() {
						synchronized(npcs) {
							for (NPCguard npc : npcs)
								npc.tick();
						}
					}
				}, 100, 100, TimeUnit.MILLISECONDS);
		getSyncScheduler().scheduleWithFixedDelay(
				new Runnable() {
					@Override
					public void run() {
						if (System.currentTimeMillis()-ledgerChatTimer > 15000) {
							LedgerManager.dumpChat();
							ledgerChatTimer = System.currentTimeMillis();
						}
					}
				}, 1, 1, TimeUnit.SECONDS);
	}
	
	/** returns the NPCguard index */
	public static Optional<NPCguard> getNPCfromLocation(Location<World> loc2) {
		synchronized(npcs) {
			for (int i = 0; i < npcs.size(); i++) {
				Location<World> loc1 = npcs.get(i).getLoc(); 
				if (loc1.getBlockPosition().equals(loc2.getBlockPosition()) && loc1.getExtent().equals(loc2.getExtent())) {
					return Optional.of(npcs.get(i));
				}
			}
		}
		return Optional.empty();
	}
	public static Optional<NPCguard> getNPCfromShopUUID(UUID uid) {
		synchronized(npcs) {
			for (NPCguard shop : npcs) {
				if (shop.getIdentifier().equals(uid)) 
					return Optional.of(shop);
			}
		}
		return Optional.empty();
	}
	
	public static boolean isNPCused(Entity ent) {
		synchronized(npcs) {
			for (NPCguard npc : npcs) {
				//if (ent.equals(npc.getLe()))
				if (npc.getLe() != null && npc.getLe().isLoaded() && ent.getUniqueId().equals(npc.getLe().getUniqueId()))
					return true;
			}
		}
		return false;
	}
	
	public static void closeShopInventories() {
		for (Entry<UUID,UUID> shop : openShops.entrySet()) {
			Player p = Sponge.getServer().getPlayer(shop.getKey()).orElse(null);
			if (p != null) p.closeInventory();
		}
		openShops.clear();
	}
	public static void closeShopInventories(UUID shopID) {
		List<UUID> rem = new LinkedList<UUID>();
		for (Entry<UUID,UUID> shop : openShops.entrySet()) {
			if (shop.getValue().equals(shopID)) {
				Player p = Sponge.getServer().getPlayer(shop.getKey()).orElse(null);
				if (p != null) {
					p.closeInventory();
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
			p.closeInventory();
			openShops.remove(player);
			actionUnstack.remove(player);
		}
	}
}
