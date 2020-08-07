package de.dosmike.sponge.vshop;

import com.google.common.reflect.TypeToken;
import com.google.inject.Inject;
import de.dosmike.sponge.VersionChecker;
import de.dosmike.sponge.languageservice.API.LanguageService;
import de.dosmike.sponge.languageservice.API.PluginTranslation;
import de.dosmike.sponge.vshop.commands.CommandRegistra;
import de.dosmike.sponge.vshop.integrations.toomuchstock.PriceCalculator;
import de.dosmike.sponge.vshop.shops.ShopEntity;
import de.dosmike.sponge.vshop.shops.ShopEntitySerializer;
import de.dosmike.sponge.vshop.shops.StockItem;
import de.dosmike.sponge.vshop.shops.StockItemSerializer;
import de.dosmike.sponge.vshop.systems.*;
import ninja.leaping.configurate.ConfigurationNode;
import ninja.leaping.configurate.ConfigurationOptions;
import ninja.leaping.configurate.commented.CommentedConfigurationNode;
import ninja.leaping.configurate.hocon.HoconConfigurationLoader;
import ninja.leaping.configurate.loader.ConfigurationLoader;
import ninja.leaping.configurate.objectmapping.serialize.TypeSerializerCollection;
import ninja.leaping.configurate.objectmapping.serialize.TypeSerializers;
import org.slf4j.Logger;
import org.spongepowered.api.Sponge;
import org.spongepowered.api.config.ConfigDir;
import org.spongepowered.api.config.DefaultConfig;
import org.spongepowered.api.entity.Entity;
import org.spongepowered.api.entity.living.player.Player;
import org.spongepowered.api.event.Listener;
import org.spongepowered.api.event.game.state.*;
import org.spongepowered.api.event.service.ChangeServiceProviderEvent;
import org.spongepowered.api.plugin.Plugin;
import org.spongepowered.api.plugin.PluginContainer;
import org.spongepowered.api.scheduler.SpongeExecutorService;
import org.spongepowered.api.scheduler.Task;
import org.spongepowered.api.service.economy.EconomyService;
import org.spongepowered.api.service.permission.PermissionDescription;
import org.spongepowered.api.service.permission.PermissionService;
import org.spongepowered.api.service.user.UserStorageService;
import org.spongepowered.api.text.Text;
import org.spongepowered.api.text.format.TextColors;
import org.spongepowered.api.world.Location;
import org.spongepowered.api.world.World;

import java.io.*;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.FileTime;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.Map.Entry;
import java.util.concurrent.TimeUnit;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

@SuppressWarnings("UnstableApiUsage")
@Plugin(id = "vshop", name = "VillagerShops", version = "2.7")
public class VillagerShops {

    public static void main(String[] args) { System.err.println("This plugin can not be run as executable!");
    }

    //=====----- - - - Services, Scheduler, Constants and their getters

    static VillagerShops instance;
    public VillagerShops() { instance = this; }

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
    private static PermissionService permissions = null;
    private PriceCalculator priceCalculator = null;

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
    public static PriceCalculator getPriceCalculator() {
        return instance.priceCalculator;
    }
    public static SpongeExecutorService getAsyncScheduler() {
        return instance.asyncScheduler;
    }
    public static SpongeExecutorService getSyncScheduler() {
        return instance.syncScheduler;
    }
    public static Optional<PermissionService> getPermissions() {
        return Optional.ofNullable(permissions);
    }
    public static Optional<PermissionDescription.Builder> describePermission() {
        return getPermissions().map(p->p.newDescriptionBuilder(instance));
    }

    @Listener
    public void onChangeServiceProvider(ChangeServiceProviderEvent event) {
        if (event.getService().equals(EconomyService.class)) {
            l("Found Economy Service");
            economyService = (EconomyService) event.getNewProvider();
        } else if (event.getService().equals(LanguageService.class)) {
            l("Found Language Service");
            languageService = (LanguageService) event.getNewProvider();
            translator = languageService.registerTranslation(this); //add this plugin to langswitch
        } else if (event.getService().equals(UserStorageService.class)) {
            l("Found UserStorage Service");
            userStorage = (UserStorageService) event.getNewProvider();
        } else if (event.getService().equals(PermissionService.class)) {
            l("Found Permission Service");
            permissions = (PermissionService)event.getNewProvider();
        }
    }

    PluginContainer getContainer() {
        return Sponge.getPluginManager().fromInstance(this).orElseThrow(()->new InternalError("No plugin container for self returned"));
    }

    //=====----- - - - Logging shortcuts

    @Inject
    private Logger logger;

    public static void l(String format, Object... args) {
        instance.logger.info(String.format(format, args));
    }
    public static void w(String format, Object... args) {
        instance.logger.warn(String.format(format, args));
    }
    public static void critical(String format, Object... args) {
        instance.logger.error(String.format(format, args));
    }

    public static final Random rng = new Random(System.currentTimeMillis());

    //=====----- - - - Auto Logs (basically a super detailed log of user actions)

    private PrintWriter auditLog = null;
    private static final SimpleDateFormat auditTimestampFormatter = new SimpleDateFormat("yyyy-MM-dd kk:mm:ss");
    public static void audit(String format, Object... args) {
        if (instance.auditLog == null) return;
        String line = "[" + auditTimestampFormatter.format(new Date()) + "] " + String.format(format, args);
        instance.auditLog.println(line);
        instance.auditLog.flush();
    }
    private void createAuditLog() {
        //prepare audit logs
        privateConfigDir.toFile().mkdirs();
        Path auditFile = privateConfigDir.resolve("audit.log");
        if (Files.exists(auditFile)) {
            ZipOutputStream zos=null;
            FileInputStream fis=null;
            try {
                FileTime ft = Files.getLastModifiedTime(auditFile);
                String formatTime = new SimpleDateFormat("yyyy-MM-dd_kk-mm-ss").format(Date.from(ft.toInstant()));
                File zipFile = privateConfigDir.resolve("audit_" + formatTime + ".zip").toFile();
                zos = new ZipOutputStream(new FileOutputStream(zipFile));
                fis = new FileInputStream(auditFile.toFile());
                zos.putNextEntry(new ZipEntry("audit.log"));
                byte[] buffer = new byte[1024]; int read;
                while ((read=fis.read(buffer))>0) {
                    zos.write(buffer,0,read);
                }
                zos.closeEntry();
                zos.setComment("Automatically created by VillagerShops");
            } catch (IOException e) {
                w("Could not archive old audit log!");
            } finally {
                try {fis.close();} catch (Exception ignore) {}
                try {zos.flush();} catch (Exception ignore) {}
                try {zos.close();} catch (Exception ignore) {}
            }
            try {
                Files.delete(auditFile);
            } catch (IOException ignore) {}
        }
        try {
            auditLog = new PrintWriter(new OutputStreamWriter(new FileOutputStream(auditFile.toFile())));
        } catch (FileNotFoundException e) {
            w("Could not create audit log!");
        }
    }
    private void closeAuditLog() {
        if (auditLog != null) {
            auditLog.flush();
            auditLog.close();
            auditLog = null;
        }
    }

    //=====----- - - - Shop registry and management

    //save on world if the npcs list is marked dirty
    //this flag should be set every time the npcs list is changed, or a property of a shop changes
    //or a item was added or removed from the shop
    Set<UUID> shopsDirty = new HashSet<>();
    public void markShopsDirty(UUID inWorld) {
        shopsDirty.add(inWorld);
    }
    public void markShopsDirty(ShopEntity fromShop) {
        Location<World> loc = fromShop.getLocation();
        if (loc != null) shopsDirty.add(loc.getExtent().getUniqueId());
    }
    private static final List<ShopEntity> shops = new LinkedList<>();

    public static void addShop(ShopEntity shopEntity, boolean createIfAbsent) {
        if (createIfAbsent) shopEntity.findOrCreate();
        synchronized (shops) {
            shops.add(shopEntity);
        }
        instance.markShopsDirty(shopEntity);
    }

    public static Collection<ShopEntity> getShops() {
        synchronized (shops) {
            return new ArrayList<>(shops);
        }
    }

    public static void removeShop(ShopEntity shopEntity) {
        synchronized (shops) {
            shops.remove(shopEntity);
        }
        instance.markShopsDirty(shopEntity);
    }

    /** @return true if there is any shop at this location */
    public static boolean isLocationOccupied(Location<World> location) {
        synchronized (shops) {
            return shops.stream()
                    .anyMatch(shopEntity -> {
                        Location<World> shopLocation = shopEntity.getLocation();
                        return shopLocation.getPosition().distanceSquared(location.getPosition()) < 0.2 && // about half a block radius
                            shopLocation.getExtent().equals(location.getExtent());
                    });
        }
    }

    public static Optional<ShopEntity> getShopFromShopId(UUID shopId) {
        if (shopId == null) return Optional.empty();
        synchronized (shops) {
            return shops.stream()
                    .filter(shopEntity -> Objects.equals(shopId, shopEntity.getIdentifier()))
                    .findFirst();
        }
    }

    public static Optional<ShopEntity> getShopFromEntityId(UUID entityId) {
        if (entityId == null) return Optional.empty();
        synchronized (shops) {
            return shops.stream()
                    .filter(shopEntity -> Objects.equals(entityId,shopEntity.getEntityUniqueID().orElse(null)))
                    .findFirst();
        }
    }

    public static boolean isEntityShop(Entity entity) {
        synchronized (shops) {
            return shops.stream()
                    .anyMatch(shopEntity -> shopEntity.getEntityUniqueID().map(id->id.equals(entity.getUniqueId())).orElse(false));
        }
    }

    //=====----- - - - Configuration

    @Inject
    @DefaultConfig(sharedRoot = false)
    private ConfigurationLoader<CommentedConfigurationNode> configManager;
    private static final TypeSerializerCollection customSerializer = TypeSerializers.getDefaultSerializers().newChild();

    @Inject
    @ConfigDir(sharedRoot = false)
    private Path privateConfigDir;

    @Inject
    @ConfigDir(sharedRoot = true)
    private Path publicConfigDir;

    public void loadConfigs() {
        //settings.conf
        HoconConfigurationLoader loader = HoconConfigurationLoader.builder()
                .setPath(privateConfigDir.resolve("settings.conf"))
                .build();
        try {
            CommentedConfigurationNode root = loader.load(ConfigurationOptions.defaults());
            if (root.getNode("DefaultStackSize").isVirtual() ||
                    root.getNode("SmartClick").isVirtual() ||
                    root.getNode("AuditLogs").isVirtual() ||
                    root.getNode("NBTblacklist").isVirtual() ||
                    root.getNode("AnimateShops").isVirtual()) {
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

        //update tasks in case config changed (animate shops)
        stopTimers();
        startTimers();

        closeAuditLog();
        if (ConfigSettings.recordAuditLogs())
            createAuditLog();

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

    private void updateShopConfigs() {
        //vshops.conf (more database than config)
        synchronized (shops) {
            shopsDirty.clear();
            shops.clear();

            Path configFile = privateConfigDir.resolve("vshop.conf");
            //move legacy config
            Path lc = publicConfigDir.resolve("vhop.conf");
            if (Files.exists(lc) && Files.isRegularFile(lc)) {
                w("Found legacy config, moving config/vshop.conf to config/cshop/vshop.conf");
                try {
                    Files.createDirectories(privateConfigDir);
                    Files.move(lc, configFile);
                } catch (Exception e) {
                    logger.error("VillagerShops was unable to move your config to the new location - You'll have to do this manually or your shops won't load!");
                }
            }
            if (Files.exists(configFile) && Files.isRegularFile(configFile)) {
                //make backup
                try {
                    Path backupFile = privateConfigDir.resolve("vshop_backup.conf");
                    Files.deleteIfExists(backupFile);
                    Files.copy(configFile, backupFile);
                } catch (IOException e) {
                    w("Could not backup vshop.conf");
                }

                boolean noErrors = true;
                ConfigurationOptions options = ConfigurationOptions.defaults().setSerializers(customSerializer);
                Map<String, List<ConfigurationNode>> perWorld = new HashMap<>();
                try {
                    ConfigurationNode root = configManager.load(options);
                    shops.clear();
                    ConfigurationNode shopNode = root.getNode("shops");
                    if (!shopNode.isVirtual()) {
                        List<? extends ConfigurationNode> shopList = shopNode.getChildrenList();
                        for (ConfigurationNode configurationNode : shopList) {
                            //poke world uuid
                            String uuid = configurationNode.getNode("location").getNode("WorldUuid").getString();
                            //collect into list
                            List<ConfigurationNode> wlist = perWorld.computeIfAbsent(uuid, k -> new LinkedList<>());
                            wlist.add(configurationNode.copy());
                        }
                    }
                } catch (Exception e1) {
                    e1.printStackTrace();
                    noErrors = false;
                }
                //dump out per world configs
                for (Entry<String, List<ConfigurationNode>> entry : perWorld.entrySet()) {
                    try {
                        HoconConfigurationLoader loader = HoconConfigurationLoader.builder()
                                .setDefaultOptions(options)
                                .setPath(privateConfigDir.resolve("world_" + entry.getKey() + ".conf")).build();
                        ConfigurationNode wroot = loader.createEmptyNode(options);
                        wroot.getNode("shops").setValue(entry.getValue());
                        loader.save(wroot);
                    } catch (IOException e) {
                        w("Could not dump shop config for world %s", entry.getKey());
                        noErrors = false;
                    }
                }
                if (noErrors) {
                    try {
                        Files.delete(configFile); // is now converted into perWorld configs
                    } catch (IOException e) {
                        w("Could not delete original config - changes will not persist!");
                    }
                }
            }
        }
    }

    void loadWorldShops(UUID worldId) {
        synchronized (shops) {
            l("Loading shops from world_%s.conf", worldId.toString());

            Path configFile = privateConfigDir.resolve("world_"+worldId.toString()+".conf");
            //make backup
            try {
                Path backupFile = privateConfigDir.resolve("world_"+worldId.toString()+"_backup.conf");
                Files.deleteIfExists(backupFile);
                Files.copy(configFile, backupFile);
            } catch (IOException e) {
                w("Could not backup world_%s.conf!", worldId.toString());
            }

            ConfigurationOptions options = ConfigurationOptions.defaults().setSerializers(customSerializer);
            try {
                HoconConfigurationLoader loader = HoconConfigurationLoader.builder()
                        .setPath(configFile)
                        .setDefaultOptions(options).build();
                ConfigurationNode root = loader.load(options);
                ConfigurationNode shopNode = root.getNode("shops");
                if (!shopNode.isVirtual()) {
                    List<? extends ConfigurationNode> shopList = shopNode.getChildrenList();
                    for (int i = 0; i < shopList.size(); i++) {
                        try {
                            shops.add(shopList.get(i).getValue(TypeToken.of(ShopEntity.class)));
                        } catch (Exception e) {
                            System.err.println("Could not load shop "+(i+1)+":");
                            e.printStackTrace(System.err);
                            w("Trying to continue parsing the config...");
                        }
                    }
                }
            } catch (Exception e1) {
                e1.printStackTrace();
            }
        }
    }

    void unloadWorldShops(UUID worldId) {
        synchronized (shops) {
            if (shopsDirty.contains(worldId)) {
                saveWorldShops(worldId);
                shopsDirty.remove(worldId);
            }
            Set<ShopEntity> toUnload = new HashSet<>();
            for (ShopEntity shopEntity : shops) {
                if (shopEntity.getLocation().getExtent().getUniqueId().equals(worldId)) {
                    toUnload.add(shopEntity);
                }
            }
            shops.removeAll(toUnload);
            l(" > Shops for world %s unloaded", worldId.toString());
        }
    }

    public void saveConfigs() {
        saveShops();
        try {
            ConfigurationLoader<CommentedConfigurationNode> limitManager = HoconConfigurationLoader.builder()
                    .setPath(privateConfigDir.resolve("incomeLimits.conf")).build();
            ConfigurationNode root = limitManager.createEmptyNode(ConfigurationOptions.defaults());

            ConfigurationNode sub = root.getNode("income");
            Map<String, String> serialized = new HashMap<>();
            for (Entry<UUID, BigDecimal> earnings : incomeLimiter.getEarnings().entrySet()) {
                serialized.put(earnings.getKey().toString(), earnings.getValue().toString());
            }
            sub.setValue(new TypeToken<Map<String, String>>() {}, serialized);

            sub = root.getNode("spending");
            serialized.clear();
            for (Entry<UUID, BigDecimal> spendings : incomeLimiter.getSpendings().entrySet()) {
                serialized.put(spendings.getKey().toString(), spendings.getValue().toString());
            }
            sub.setValue(new TypeToken<Map<String, String>>() {}, serialized);

            root.getNode("timestamp").setValue(incomeLimiter.forTime());

            limitManager.save(root);
        } catch (Exception e1) {
            Sponge.getServer().getBroadcastChannel().send(Text.of(TextColors.RED, "[VShop] Error: ", e1.getMessage()));
            e1.printStackTrace();
        }
    }

    void saveShops() {
        synchronized (shops) {
            if (shopsDirty.isEmpty()) return;
            l("Saving VillagerShops...");
//            for (World world : Sponge.getServer().getWorlds()) {
//                saveWorldShops(world.getUniqueId());
//            }
            while (!shopsDirty.isEmpty()) {
                UUID world = shopsDirty.iterator().next();
                saveWorldShops(world);
                shopsDirty.remove(world);
            }
        }
    }
    private void saveWorldShops(UUID worldId) {
        ConfigurationOptions options = ConfigurationOptions.defaults().setSerializers(customSerializer);
        try {
            HoconConfigurationLoader loader = HoconConfigurationLoader.builder()
                    .setDefaultOptions(options)
                    .setPath(privateConfigDir.resolve("world_" + worldId.toString() + ".conf")).build();
            ConfigurationNode root = loader.createEmptyNode(options);
            List<ShopEntity> worldShops = new LinkedList<>();
            for (ShopEntity shopEntity : shops) {
                if (shopEntity.getLocation().getExtent().getUniqueId().equals(worldId)) {
                    worldShops.add(shopEntity);
                }
            }
            root.getNode("shops").setValue(ShopEntitySerializer.tokenListNPCguard, worldShops);
            loader.save(root);
        } catch (Exception e1) {
            Sponge.getServer().getBroadcastChannel().send(Text.of(TextColors.RED, "[VShop] Error: ", e1.getMessage()));
            e1.printStackTrace();
        }
        l(" > Shops for world %s saved", worldId.toString());
    }

    //=====----- - - - Lifecycle Events

    @Listener
    public void onServerPreInit(GamePreInitializationEvent event) {
        instance = this;

        incomeLimiter = new IncomeLimiterService();
        asyncScheduler = Sponge.getScheduler().createAsyncExecutor(this);
        syncScheduler = Sponge.getScheduler().createSyncExecutor(this);

        customSerializer.registerType(TypeToken.of(StockItem.class), new StockItemSerializer());
        customSerializer.registerType(TypeToken.of(ShopEntity.class), new ShopEntitySerializer());

        // This service needs to become available before plugins try to use it,
        // and those plugins need to use the service before shops load.
        // -> Plugins will have to use the service in GameInit
        Sponge.getServiceManager().setProvider(this, PluginItemService.class, new PluginItemServiceImpl());
    }

    @Listener
    public void onServerInit(GameInitializationEvent event) {
        Sponge.getEventManager().registerListeners(this, new EventListeners());

        //Some are provided earlier than when plugins are initialized
        if (userStorage == null)
            userStorage = Sponge.getServiceManager().provide(UserStorageService.class).orElseThrow(()->new IllegalStateException("Could not find UserStorageService"));
        if (permissions == null)
            permissions = Sponge.getServiceManager().provide(PermissionService.class).orElseThrow(()->new IllegalStateException("Could not find PermissionService"));
        if (priceCalculator == null)
            priceCalculator = PriceCalculator.get();

        l("Registering commands...");
        CommandRegistra.register();
        l("Loading configs...");
        loadConfigs();
    }

    @Listener
    public void onLoadComplete(GameLoadCompleteEvent event) {
        //Last GameState before worlds load, so configs have to be converted here (or earlier)
        updateShopConfigs();
    }

    @Listener
    public void onServerStart(GameStartedServerEvent event) {
        l("Starting timers...");
        startTimers();

        //these two calls depend on loadConfig()
        VersionChecker.checkPluginVersion(getContainer());
        TranslationLoader.fetchTranslations(false);

        l("VillagerShops is now ready!");
    }

    @Listener
    public void onServerStopping(GameStoppingEvent event) {
        stopTimers();
        saveConfigs();
        closeAuditLog();
    }

    //=====----- - - - Utility functions

    static void stopTimers() {
        Set<Task> tasks = Sponge.getScheduler().getScheduledTasks(instance);
        for (Task t : tasks) if (!t.isAsynchronous()) t.cancel(); //async tasks work in the database
    }

    static void startTimers() {
        if (ConfigSettings.areShopsAnimated())
            getSyncScheduler().scheduleWithFixedDelay(
                    () -> {
                        synchronized (shops) {
                            for (ShopEntity npc : shops)
                                npc.tick();
                        }
                    }, 100, 100, TimeUnit.MILLISECONDS);
        getSyncScheduler().scheduleWithFixedDelay( LedgerManager::dumpChat, 15, 1, TimeUnit.SECONDS );
    }

    public static void closeShopInventories(UUID shopID) {
        List<UUID> remaining = new LinkedList<>();
        for (Entry<UUID, UUID> shop : Utilities.openShops.entrySet()) {
            if (shop.getValue().equals(shopID)) {
                Player p = Sponge.getServer().getPlayer(shop.getKey()).orElse(null);
                if (p != null) {
                    p.closeInventory();
                    remaining.add(shop.getKey());
                }
            }
        }
        for (UUID r : remaining) {
            Utilities.openShops.remove(r);
            Utilities.actionUnstack.remove(r);
        }
    }
}
