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
import org.spongepowered.api.entity.Entity;
import org.spongepowered.api.entity.EntityType;
import org.spongepowered.api.entity.EntityTypes;
import org.spongepowered.api.entity.living.player.Player;
import org.spongepowered.api.event.Listener;
import org.spongepowered.api.event.cause.Cause;
import org.spongepowered.api.event.entity.InteractEntityEvent;
import org.spongepowered.api.event.game.state.GameInitializationEvent;
import org.spongepowered.api.event.game.state.GameStartedServerEvent;
import org.spongepowered.api.event.game.state.GameStoppingEvent;
import org.spongepowered.api.event.item.inventory.ClickInventoryEvent;
import org.spongepowered.api.event.item.inventory.DropItemEvent;
import org.spongepowered.api.event.item.inventory.InteractInventoryEvent;
import org.spongepowered.api.event.service.ChangeServiceProviderEvent;
import org.spongepowered.api.item.inventory.Inventory;
import org.spongepowered.api.item.inventory.ItemStack;
import org.spongepowered.api.item.inventory.ItemStackSnapshot;
import org.spongepowered.api.item.inventory.Slot;
import org.spongepowered.api.item.inventory.property.SlotIndex;
import org.spongepowered.api.item.inventory.transaction.SlotTransaction;
import org.spongepowered.api.plugin.Plugin;
import org.spongepowered.api.scheduler.Task;
import org.spongepowered.api.service.economy.EconomyService;
import org.spongepowered.api.text.serializer.TextSerializers;
import org.spongepowered.api.world.Location;
import org.spongepowered.api.world.World;

import com.flowpowered.math.vector.Vector3d;
import com.google.common.reflect.TypeToken;
import com.google.inject.Inject;

import ninja.leaping.configurate.ConfigurationNode;
import ninja.leaping.configurate.commented.CommentedConfigurationNode;
import ninja.leaping.configurate.hocon.HoconConfigurationLoader;
import ninja.leaping.configurate.loader.ConfigurationLoader;

@Plugin(id="vshop", name="VillagerShops", version="0.1", authors={"DosMike"}, url="http://itwookie.com")
public class VillagerShops {
	
	public static void main(String[] args) { System.err.println("This plugin can not be run as executable!"); }
	
	static VillagerShops instance;
	public static VillagerShops getInstance() { return instance; }
	
	private EconomyService economyService = null;
	@Listener
	public void onChangeServiceProvider(ChangeServiceProviderEvent event) {
		if (event.getService().equals(EconomyService.class)) {
			economyService = (EconomyService) event.getNewProviderRegistration().getProvider();
		}
	}
	public static EconomyService getEconomy() { return instance.economyService; }
	
	@Inject
	private Logger logger;
	public static void l(String format, Object... args) { instance.logger.info(String.format(format, args)); }
	public static void w(String format, Object... args) { instance.logger.warn(String.format(format, args)); }
	
	public static Random rng = new Random(System.currentTimeMillis());
	
	/// --- === Main Plugin stuff === --- \\\ 
	
	List<NPCguard> npcs = new LinkedList<NPCguard>();
	/** remembers what player is viewing what shop as Player <-> Shop mapping */
	Map<UUID, UUID> openShops = new HashMap<UUID, UUID>();
	
	@Listener
	public void onServerInit(GameInitializationEvent event) {
	}
	
	@Listener
	public void onServerStart(GameStartedServerEvent event) {
		instance = this;
		
		CommandRegistra.register();
		
		loadConfigs();
		startTimers();
	}
	
	@Listener
	public void onServerStopping(GameStoppingEvent event) {
		terminateNPCs();
		saveConfigs();
		npcs.clear();
	}
	
	@Listener
	public void onPlayerInteractEntity(InteractEntityEvent.Primary event) {
		Optional<Player> cause = event.getCause().first(Player.class);
		Entity target = event.getTargetEntity();
		if (cause.isPresent())
			if (InteractionHandler.clickEntity(cause.get(), target, InteractionHandler.Button.left))
				event.setCancelled(true);
	}
	@Listener
	public void onPlayerInteractEntity(InteractEntityEvent.Secondary event) {
		Optional<Player> cause = event.getCause().first(Player.class);
		Entity target = event.getTargetEntity();
		if (cause.isPresent())
			if (InteractionHandler.clickEntity(cause.get(), target, InteractionHandler.Button.right))
					event.setCancelled(true);
	}
	
	@Listener
	public void onInventoryClosed(InteractInventoryEvent.Close event) {
		Optional<Player> cause = event.getCause().first(Player.class);
		if (cause.isPresent()) {
			openShops.remove(cause.get().getUniqueId());
//			l("%s closed an inventory", cause.get().getName());
		} else {
//			l("An inventory was closed");
		}
	}
	
	//TODO + why this is important:
	// in case a admin/plugin opens a inventory over another inventory we will still assume we're in the shop inventory
	@Listener
	public void onInventoryOpened(InteractInventoryEvent.Open event) {
		Optional<Player> cause = event.getCause().first(Player.class);
		if (cause.isPresent()) {
//			openShops.remove(cause.get().getUniqueId());
			l("%s opened and inventory %s", cause.get().getName(), event.getTargetInventory().getName());
		} else {
			w("Can't get player opening inventory");
		}
	}
	@Listener
	public void onInventoryClick(ClickInventoryEvent event) {
		Optional<Player> clicker = event.getCause().first(Player.class);
		if (!clicker.isPresent()) return;
		if (!openShops.containsKey(clicker.get().getUniqueId())) return;
		
		int slotIndex=-1;
		boolean inTargetInventory=false;
		
		//small algorithm to determ in what inventory the event occurred
		//thanks for this great API so far ;D
		
		//first find the target inventory, for me it's always 2, 5, 8,...
		//rows high, so i can go through the sub inventories of the displayed
		//inventory and just pick the one matching by size
		Set<Inventory> inventoryset=new HashSet<>();
		event.getTargetInventory().spliterator().forEachRemaining(ti -> {
			inventoryset.add(ti);
		});
		Inventory target=null;
		for (Inventory ti : inventoryset) {
			if ((ti.capacity()/9+1)%3==0)
				target = ti;
		}
		if (target==null) {
			VillagerShops.w("Unable to find shop target inventory");
		}
		
		//now for each action (that's probably just one) i get the slot
		//because that's the only way to get the slot index
		//now as we got that slot we check if the item in that slot
		//is also in the target inventory.
		//by this i can tell if the player clicked the top inventory or his own
		ItemStackSnapshot isnap = event.getCursorTransaction().getOriginal();
		for (SlotTransaction action : event.getTransactions()) {
			action.setValid(false);
			Slot thisSlot = action.getSlot();
			if (isnap.isEmpty()) isnap = action.getOriginal();
			for (SlotIndex si : thisSlot.getProperties(SlotIndex.class)) {
				for (Inventory oneslot : target.slots()) {
					if (oneslot.capacity()!=1) {
						l("Slot exceeds capacity of 1!");
					}
					Optional<ItemStack> b = oneslot.peek();
					if (b.isPresent() && (b.get().createSnapshot().equals(isnap)))
						inTargetInventory=true;
				}
				if (inTargetInventory) {
					slotIndex = si.getValue();
					l("Found slot %d in target inventory", si.getValue());
					break;
				}
			}
			if (inTargetInventory) break;
		}
		
		//thank's for reading this...
		//if your head did not explode: congrats :D
		//if you're a sponge dev, please add fromInventory()
		// and toInventory() to the transaction... that's kinda what
		// I'd expect from the transaction to provide the most ...
		
		//clear cursor
		event.getCursorTransaction().setValid(false);
		event.getCursorTransaction().setCustom(ItemStackSnapshot.NONE);
		
		if (!inTargetInventory) return;
		
		InteractionHandler.clickInventory(clicker.get(), slotIndex);

		event.setCancelled(true);
	}
	
	@Listener
	public void onDropItem(DropItemEvent event) {
		Optional<Player> clicker = event.getCause().first(Player.class);
		if (!clicker.isPresent()) return;
		if (!openShops.containsKey(clicker.get().getUniqueId())) return;
		event.setCancelled(true);
	}
	
	@SuppressWarnings("unchecked")	//if nobody plays with the config it works perfectly fine
	public void loadConfigs() {
		npcs.clear();
		File folder = new File("config/vshop/");
		if (!folder.exists() || !folder.isDirectory()) folder.mkdirs();
		File[] fls = folder.listFiles();
		for (File f : fls) {
			if (f.getName().endsWith(".conf")) {
				ConfigurationLoader<CommentedConfigurationNode> loader = HoconConfigurationLoader.builder().setFile(f).build();
				ConfigurationNode rootNode;
				try {
					String name = f.getName();
					NPCguard npc = new NPCguard(UUID.fromString(name.substring(0, name.length()-5)));
					InvPrep ip = new InvPrep();
					StockItem si;
				    rootNode = loader.load();
				    List<? extends ConfigurationNode> items = rootNode.getNode("items").getChildrenList();
				    for (ConfigurationNode ii : items) {
				    	si = new StockItem(
				    			ii.getNode("itemstack").getValue(TypeToken.of(ItemStack.class)),
				    			ii.getNode("buyprice").isVirtual() ? null : ii.getNode("buyprice").getDouble(),
				    			ii.getNode("sellprice").isVirtual() ? null : ii.getNode("sellprice").getDouble()	);
				    	ip.addItem(si);
				    }
				    npc.setPreparator(ip);
				    npc.setLoc(rootNode.getNode("location").getValue(TypeToken.of(Location.class)));
				    npc.setRot(
				    		new Vector3d(0.0, 
				    		rootNode.getNode("rotation").getDouble(0.0),
				    		0.0
				    		));
				    npc.setNpcType((EntityType)FieldResolver.getFinalStaticByName(EntityTypes.class, rootNode.getNode("entitytype").getString("VILLAGER")));
				    npc.setVariant(rootNode.getNode("variant").getString("NONE"));
					npc.setDisplayName(TextSerializers.FORMATTING_CODE.deserialize(rootNode.getNode("displayName").getString("VillagerShop")));
				    npcs.add(npc);
				} catch(Exception e) {
				    throw new RuntimeException("Unable to load "+f.getName() +"\n Because: " + e.getMessage(), e);
				}
			}
		}
	}
	
	public void saveConfigs() {
		File folder = new File("config/vshop/");
		if (!folder.exists() || !folder.isDirectory()) folder.mkdirs();
		
		for (NPCguard npc : npcs) {
			try {
				File f = new File (folder, npc.getIdentifier().toString()+".conf");
				ConfigurationLoader<CommentedConfigurationNode> loader = HoconConfigurationLoader.builder().setFile(f).build();
				ConfigurationNode rootNode = loader.createEmptyNode();
				ConfigurationNode child = rootNode.getNode("items");
				int i = 0; for (StockItem si : npc.getPreparator().items) {
					ConfigurationNode item = child.getNode("item"+i);
					item.getNode("itemstack").setValue(TypeToken.of(ItemStack.class), si.item);
					if (si.getBuyPrice() != null) item.getNode("buyprice").setValue(si.getBuyPrice());
					if (si.getSellPrice() != null) item.getNode("sellprice").setValue(si.getSellPrice());
				i++; }
				rootNode.getNode("location").setValue(TypeToken.of(Location.class), npc.getLoc());
				rootNode.getNode("rotation").setValue(npc.getRot().getY()); // we only need the yaw rotationb
				rootNode.getNode("entitytype").setValue(npc.getNpcType().getName());
				rootNode.getNode("variant").setValue(npc.getVariantName());
				rootNode.getNode("displayName").setValue(TextSerializers.FORMATTING_CODE.serialize(npc.getDisplayName()));
				loader.save(rootNode);
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
	}
	
	public void terminateNPCs() {
		Set<Task> tasks = Sponge.getScheduler().getScheduledTasks(this);
		for (Task t : tasks) t.cancel();

		closeShopInventories();
		
		for (NPCguard npc : npcs) {
			Entity e = npc.getLe();
			if (e != null) e.remove();
		}
		//npcs.clear();
	}
	
	public void startTimers() {
		Sponge.getScheduler().createSyncExecutor(this).scheduleWithFixedDelay(
				new Runnable() {
					@Override
					public void run() {
						for (NPCguard npc : npcs)
							npc.tick();

					}
				}, 100, 100, TimeUnit.MILLISECONDS);
	}
	
	/** returns the NPCguard index */
	public Optional<NPCguard> getNPCfromLocation(Location<World> loc2) {
		for (int i = 0; i < npcs.size(); i++) {
			Location<World> loc1 = npcs.get(i).getLoc(); 
			if (loc1.getBlockPosition().equals(loc2.getBlockPosition()) && loc1.getExtent().equals(loc2.getExtent())) {
				return Optional.of(npcs.get(i));
			}
		}
		return Optional.empty();
	}
	public Optional<NPCguard> getNPCfromShopUUID(UUID uid) {
		for (NPCguard shop : npcs) {
			if (shop.getIdentifier().equals(uid)) 
				return Optional.of(shop);
		}
		return Optional.empty();
	}
	
	public boolean isNPCused(Entity ent) {
		for (NPCguard npc : npcs) {
			if (ent.equals(npc.getLe()))
				return true;
		}
		return false;
	}
	
	public void closeShopInventories() {
		for (Entry<UUID,UUID> shop : openShops.entrySet()) {
			Player p = Sponge.getServer().getPlayer(shop.getKey()).orElse(null);
			if (p != null) p.closeInventory(Cause.builder().named("PLUGIN", this).build());
		}
		openShops.clear();
	}
	public void closeShopInventories(UUID shopID) {
		List<UUID> rem = new LinkedList<UUID>();
		for (Entry<UUID,UUID> shop : openShops.entrySet()) {
			if (shop.getValue().equals(shopID)) {
				Player p = Sponge.getServer().getPlayer(shop.getKey()).orElse(null);
				if (p != null) {
					p.closeInventory(Cause.builder().named("PLUGIN", this).build());
					rem.add(shop.getKey());
				}
			}
		}
		for (UUID r : rem) openShops.remove(r);
	}
	public void closeShopInventory(UUID player) {
		Player p = Sponge.getServer().getPlayer(player).orElse(null);
		if (p != null) {
			p.closeInventory(Cause.builder().named("PLUGIN", this).build());
			openShops.remove(player);
		}
	}
}
