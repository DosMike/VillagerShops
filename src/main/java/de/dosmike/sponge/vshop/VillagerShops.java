package de.dosmike.sponge.vshop;

import com.google.common.reflect.TypeToken;
import com.google.inject.Inject;
import de.dosmike.sponge.VersionChecker;
import de.dosmike.sponge.languageservice.API.LanguageService;
import de.dosmike.sponge.languageservice.API.PluginTranslation;
import de.dosmike.sponge.vshop.shops.NPCguard;
import de.dosmike.sponge.vshop.shops.NPCguardSerializer;
import de.dosmike.sponge.vshop.shops.StockItem;
import de.dosmike.sponge.vshop.shops.StockItemSerializer;
import de.dosmike.sponge.vshop.systems.IncomeLimiterService;
import de.dosmike.sponge.vshop.systems.LedgerManager;
import de.dosmike.sponge.vshop.systems.TranslationLoader;
import ninja.leaping.configurate.ConfigurationNode;
import ninja.leaping.configurate.ConfigurationOptions;
import ninja.leaping.configurate.commented.CommentedConfigurationNode;
import ninja.leaping.configurate.hocon.HoconConfigurationLoader;
import ninja.leaping.configurate.loader.ConfigurationLoader;
import ninja.leaping.configurate.objectmapping.serialize.TypeSerializerCollection;
import ninja.leaping.configurate.objectmapping.serialize.TypeSerializers;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.spongepowered.api.Sponge;
import org.spongepowered.api.command.CommandSource;
import org.spongepowered.api.config.ConfigDir;
import org.spongepowered.api.config.DefaultConfig;
import org.spongepowered.api.entity.Entity;
import org.spongepowered.api.entity.living.player.Player;
import org.spongepowered.api.event.Listener;
import org.spongepowered.api.event.game.state.GameInitializationEvent;
import org.spongepowered.api.event.game.state.GameStartedServerEvent;
import org.spongepowered.api.event.game.state.GameStoppingEvent;
import org.spongepowered.api.event.service.ChangeServiceProviderEvent;
import org.spongepowered.api.event.world.SaveWorldEvent;
import org.spongepowered.api.plugin.Plugin;
import org.spongepowered.api.plugin.PluginContainer;
import org.spongepowered.api.scheduler.SpongeExecutorService;
import org.spongepowered.api.scheduler.Task;
import org.spongepowered.api.service.economy.Currency;
import org.spongepowered.api.service.economy.EconomyService;
import org.spongepowered.api.service.user.UserStorageService;
import org.spongepowered.api.text.Text;
import org.spongepowered.api.text.format.TextColors;
import org.spongepowered.api.world.Location;
import org.spongepowered.api.world.World;

import java.io.File;
import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.file.Path;
import java.util.*;
import java.util.Map.Entry;
import java.util.concurrent.TimeUnit;

@Plugin(id = "vshop", name = "VillagerShops",
        version = "2.2.1")
public class VillagerShops {

    public static void main(String[] args) { System.err.println("This plugin can not be run as executable!");
    }

    static VillagerShops instance;

    public static VillagerShops getInstance() {
        return instance;
    }

    private EconomyService economyService = null;
    private PluginTranslation translator = null;
    private LanguageService languageService = null;
    private UserStorageService userStorage = null;
    private IncomeLimiterService incomeLimiter = null;
    private SpongeExecutorService asyncScheduler = null;
    private SpongeExecutorService syncScheduler = null;

    @Listener
    public void onChangeServiceProvider(ChangeServiceProviderEvent event) {
        if (event.getService().equals(EconomyService.class)) {
            economyService = (EconomyService) event.getNewProvider();
        } else if (event.getService().equals(LanguageService.class)) {
            languageService = (LanguageService) event.getNewProvider();
            translator = languageService.registerTranslation(this); //add this plugin to langswitch
        } else if (event.getService().equals(UserStorageService.class)) {
            userStorage = (UserStorageService) event.getNewProvider();
        }
    }

    public static PluginTranslation getTranslator() {
        return instance.translator;
    }
    static LanguageService getLangSwitch() {
        return instance.languageService;
    }
    public static EconomyService getEconomy() {
        return instance.economyService;
    }
    public static UserStorageService getUserStorage() {
        return instance.userStorage;
    }
    public static IncomeLimiterService getIncomeLimiter() {
        return instance.incomeLimiter;
    }
    public static SpongeExecutorService getAsyncScheduler() {
        return instance.asyncScheduler;
    }
    public static SpongeExecutorService getSyncScheduler() {
        return instance.syncScheduler;
    }

    PluginContainer getContainer() {
        return Sponge.getPluginManager().fromInstance(this).get();
    }

    @Inject
    private Logger logger;

    public static void l(String format, Object... args) {
        instance.logger.info(String.format(format, args));
    }

    public static void w(String format, Object... args) {
        instance.logger.warn(String.format(format, args));
    }

    public static Random rng = new Random(System.currentTimeMillis());

    /// --- === Main Plugin stuff === --- \\\

    //save on world if the npcs list is marked dirty
    //this flag should be set every time the npcs list is changed, or a property of a shop changes
    //or a item was added or removed from the shop
    boolean npcsDirty = false;
    public void markNpcsDirty() {
        npcsDirty=true;
    }
    private static final List<NPCguard> npcs = new LinkedList<>();

    static void addNPCguard(NPCguard add) {
        synchronized (npcs) {
            npcs.add(add);
        }
        instance.npcsDirty = true;
    }

    public static Collection<NPCguard> getNPCguards() {
        synchronized (npcs) {
            List<NPCguard> res = new ArrayList<>(npcs.size());
            for (NPCguard g : npcs) res.add(g);
            return res;
        }
    }

    static void removeNPCguard(NPCguard remove) {
        synchronized (npcs) {
            npcs.remove(remove);
        }
        instance.npcsDirty = true;
    }

    @Inject
    @DefaultConfig(sharedRoot = false)
    private ConfigurationLoader<CommentedConfigurationNode> configManager;
    private static TypeSerializerCollection customSerializer = TypeSerializers.getDefaultSerializers().newChild();

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

        loadShops();
        loadConfigs();
        startTimers();

        //these two calls depend on loadConfig()
        VersionChecker.checkPluginVersion(getContainer());
        TranslationLoader.fetchTranslations();

        l("VillagerShops is now ready!");
    }

    long lastSave = System.currentTimeMillis();

    @Listener
    public void onWorldsSave(SaveWorldEvent event) {
        saveShops();
    }

    @Listener
    public void onServerStopping(GameStoppingEvent event) {
        terminateNPCs();
        saveShops();
    }

    public void loadConfigs() {
        //settings.conf
        HoconConfigurationLoader loader = HoconConfigurationLoader.builder()
                .setPath(privateConfigDir.resolve("settings.conf"))
                .build();
        try {
            CommentedConfigurationNode root = loader.load(ConfigurationOptions.defaults());
            if (root.getNode("DefaultStackSize").isVirtual()) {
                HoconConfigurationLoader defaultLoader = HoconConfigurationLoader.builder()
                        .setURL(Sponge.getAssetManager().getAsset(this, "default_settings.conf").get().getUrl())
                        .build();
                root.mergeValuesFrom(defaultLoader.load());
                loader.save(root);
            }
            VersionChecker.setVersionCheckingEnabled(getContainer().getId(), root.getNode("VersionChecking").getBoolean(false));
            ConfigSettings.loadFromConfig(root);
        } catch (IOException e) {
            new RuntimeException("Could not load settings.conf", e).printStackTrace();
        }
    }

    private void loadShops() {
        //vshops.conf (more database than config)
        synchronized (npcs) {
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
                npcs.clear();
                ConfigurationNode shopNode = root.getNode("shops");
                if (!shopNode.isVirtual())
                    npcs.addAll(
                        shopNode.getValue(NPCguardSerializer.tokenListNPCguard)
                    );
            } catch (Exception e1) {
                e1.printStackTrace();
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
                root.getNode("income").getChildrenMap().forEach((k, v) -> {
                    try {
                        earningValues.put(UUID.fromString((String) k), new BigDecimal(v.getString("0")));
                    } catch (Exception invalid) {
                        invalid.printStackTrace();
                    }
                });
                root.getNode("spending").getChildrenMap().forEach((k, v) -> {
                    try {
                        spendingValues.put(UUID.fromString((String) k), new BigDecimal(v.getString("0")));
                    } catch (Exception invalid) {
                        invalid.printStackTrace();
                    }
                });
                long dayStamp = root.getNode("timestamp").getLong(System.currentTimeMillis());

                incomeLimiter.loadFromConfig(earningValues, spendingValues, dayStamp);
            }
        } catch (Exception e1) {/**/}
    }

    @SuppressWarnings("serial")
    void saveShops() {
        synchronized (npcs) {
            if (!npcsDirty || System.currentTimeMillis() - lastSave < 10000) { //not more than every 10 seconds
                return;
            }
            l("Saving VillagerShops...");
            lastSave = System.currentTimeMillis();

            ConfigurationOptions options = ConfigurationOptions.defaults().setSerializers(customSerializer);
            try {
                ConfigurationNode root = configManager.createEmptyNode(options);
                root.getNode("shops").setValue(NPCguardSerializer.tokenListNPCguard, npcs);
                configManager.save(root);
            } catch (Exception e1) {
                Sponge.getServer().getBroadcastChannel().send(Text.of(TextColors.RED, "[VShop] Error: ", e1.getMessage()));
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
            sub.setValue(new TypeToken<Map<String, String>>() {
            }, ser);
            sub = root.getNode("spending");
            ser.clear();
            for (Entry<UUID, BigDecimal> e : incomeLimiter.getSpendings().entrySet()) {
                ser.put(e.getKey().toString(), e.getValue().toString());
            }
            sub.setValue(new TypeToken<Map<String, String>>() {
            }, ser);
            root.getNode("timestamp").setValue(incomeLimiter.forTime());

            limitManager.save(root);
        } catch (Exception e1) {
            Sponge.getServer().getBroadcastChannel().send(Text.of(TextColors.RED, "[VShop] Error: ", e1.getMessage()));
            e1.printStackTrace();
        }
    }

    private static void terminateNPCs() {
        stopTimers();

        closeShopInventories();

        synchronized (npcs) {
            for (NPCguard npc : npcs) {
                Entity e = npc.getLe();
                if (e != null) e.remove();
            }
        }
    }

    static void stopTimers() {
        Set<Task> tasks = Sponge.getScheduler().getScheduledTasks(instance);
        for (Task t : tasks) if (!t.isAsynchronous()) t.cancel(); //async tasks work in the database
    }

    static long ledgerChatTimer = System.currentTimeMillis();

    static void startTimers() {
        getSyncScheduler().scheduleWithFixedDelay(
                () -> {
                    synchronized (npcs) {
                        for (NPCguard npc : npcs)
                            npc.tick();
                    }
                }, 100, 100, TimeUnit.MILLISECONDS);
        getSyncScheduler().scheduleWithFixedDelay(
                () -> {
                    if (System.currentTimeMillis() - ledgerChatTimer > 15000) {
                        LedgerManager.dumpChat();
                        ledgerChatTimer = System.currentTimeMillis();
                    }
                }, 1, 1, TimeUnit.SECONDS);
    }

    /**
     * returns the NPCguard index
     */
    public static Optional<NPCguard> getNPCfromLocation(Location<World> loc2) {
        synchronized (npcs) {
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
        synchronized (npcs) {
            for (NPCguard shop : npcs) {
                if (shop.getIdentifier().equals(uid))
                    return Optional.of(shop);
            }
        }
        return Optional.empty();
    }

    public static boolean isNPCused(Entity ent) {
        synchronized (npcs) {
            for (NPCguard npc : npcs) {
                //if (ent.equals(npc.getLe()))
                if (npc.getLe() != null && npc.getLe().isLoaded() && ent.getUniqueId().equals(npc.getLe().getUniqueId()))
                    return true;
            }
        }
        return false;
    }

    public static void closeShopInventories() {
        for (Entry<UUID, UUID> shop : Utilities.openShops.entrySet()) {
            Sponge.getServer().getPlayer(shop.getKey())
                    .ifPresent(Player::closeInventory);
        }
        Utilities.openShops.clear();
    }

    public static void closeShopInventories(UUID shopID) {
        List<UUID> rem = new LinkedList<>();
        for (Entry<UUID, UUID> shop : Utilities.openShops.entrySet()) {
            if (shop.getValue().equals(shopID)) {
                Player p = Sponge.getServer().getPlayer(shop.getKey()).orElse(null);
                if (p != null) {
                    p.closeInventory();
                    rem.add(shop.getKey());
                }
            }
        }
        for (UUID r : rem) {
            Utilities.openShops.remove(r);
            Utilities.actionUnstack.remove(r);
        }
    }

    public static void closeShopInventory(UUID player) {
        Player p = Sponge.getServer().getPlayer(player).orElse(null);
        if (p != null) {
            p.closeInventory();
            Utilities.openShops.remove(player);
            Utilities.actionUnstack.remove(player);
        }
    }
}
