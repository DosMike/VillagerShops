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
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

@Plugin(id = "vshop", name = "VillagerShops", version = "2.5")
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
    private static PermissionService permissions = null;

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
    public static Optional<PermissionService> getPermissions() {
        return Optional.ofNullable(permissions);
    }
    public static Optional<PermissionDescription.Builder> describePermission() {
        return getPermissions().map(p->p.newDescriptionBuilder(instance));
    }

    @Listener
    public void onChangeServiceProvider(ChangeServiceProviderEvent event) {
        if (event.getService().equals(EconomyService.class)) {
            economyService = (EconomyService) event.getNewProvider();
        } else if (event.getService().equals(LanguageService.class)) {
            languageService = (LanguageService) event.getNewProvider();
            translator = languageService.registerTranslation(this); //add this plugin to langswitch
        } else if (event.getService().equals(UserStorageService.class)) {
            userStorage = (UserStorageService) event.getNewProvider();
        } else if (event.getService().equals(PermissionService.class)) {
            permissions = (PermissionService)event.getNewProvider();
        }
    }

    PluginContainer getContainer() {
        return Sponge.getPluginManager().fromInstance(this).orElseThrow(()->new InternalError("No plugin container for self returned"));
    }

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

    public static Random rng = new Random(System.currentTimeMillis());

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

    /// --- === Main Plugin stuff === --- \\\

    //save on world if the npcs list is marked dirty
    //this flag should be set every time the npcs list is changed, or a property of a shop changes
    //or a item was added or removed from the shop
    AtomicBoolean npcsDirty = new AtomicBoolean(false);
    public void markNpcsDirty() {
        npcsDirty.set(true);
    }
    private static final List<NPCguard> npcs = new LinkedList<>();

    static void addNPCguard(NPCguard add) {
        synchronized (npcs) {
            npcs.add(add);
        }
        instance.npcsDirty.set(true);
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
        instance.npcsDirty.set(true);
    }

    @Inject
    @DefaultConfig(sharedRoot = false)
    private ConfigurationLoader<CommentedConfigurationNode> configManager;
    private static TypeSerializerCollection customSerializer = TypeSerializers.getDefaultSerializers().newChild();

    @Inject
    @ConfigDir(sharedRoot = false)
    private Path privateConfigDir;

    @Inject
    @ConfigDir(sharedRoot = true)
    private Path publicConfigDir;

    @Listener
    public void onServerPreInit(GamePreInitializationEvent event) {
        instance = this;

        incomeLimiter = new IncomeLimiterService();
        asyncScheduler = Sponge.getScheduler().createAsyncExecutor(this);
        syncScheduler = Sponge.getScheduler().createSyncExecutor(this);

        customSerializer.registerType(TypeToken.of(StockItem.class), new StockItemSerializer());
        customSerializer.registerType(TypeToken.of(NPCguard.class), new NPCguardSerializer());

        // This service needs to become available before plugins try to use it,
        // and those plugins need to use the service before shops load.
        // -> Plugins will have to use the service in GameInit
        Sponge.getServiceManager().setProvider(this, PluginItemService.class, new PluginItemServiceImpl());
    }

    @Listener
    public void onServerInit(GameInitializationEvent event) {
        Sponge.getEventManager().registerListeners(this, new EventListeners());

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
        TranslationLoader.fetchTranslations();

        l("VillagerShops is now ready!");
    }

    @Listener
    public void onServerStopping(GameStoppingEvent event) {
        terminateNPCs();
        saveConfigs();
        closeAuditLog();
    }

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
                root.getNode("NBTblacklist").isVirtual()) {
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
        synchronized (npcs) {
            npcsDirty.set(false);
            npcs.clear();

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
                    npcs.clear();
                    ConfigurationNode shopNode = root.getNode("shops");
                    if (!shopNode.isVirtual()) {
                        List<? extends ConfigurationNode> shopList = shopNode.getChildrenList();
                        for (int i = 0; i < shopList.size(); i++) {
                            //poke world uuid
                            String uuid = shopList.get(i).getNode("location").getNode("WorldUuid").getString();
                            //collect into list
                            List<ConfigurationNode> wlist = perWorld.get(uuid);
                            if (wlist == null) {
                                wlist = new LinkedList<>();
                                perWorld.put(uuid, wlist);
                            }
                            wlist.add(shopList.get(i).copy());
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

    void loadShops(UUID world) {
        //vshops.conf (more database than config)
        synchronized (npcs) {
            l("Loading shops from world_%s.conf", world.toString());
//            npcsDirty.set(false);
//            npcs.clear();

            Path configFile = privateConfigDir.resolve("world_"+world.toString()+".conf");
            //make backup
            try {
                Path backupFile = privateConfigDir.resolve("world_"+world.toString()+"_backup.conf");
                Files.deleteIfExists(backupFile);
                Files.copy(configFile, backupFile);
            } catch (IOException e) {
                w("Could not backup world_%s.conf!", world.toString());
            }

            ConfigurationOptions options = ConfigurationOptions.defaults().setSerializers(customSerializer);
            try {
                HoconConfigurationLoader loader = HoconConfigurationLoader.builder()
                        .setPath(configFile)
                        .setDefaultOptions(options).build();
                ConfigurationNode root = loader.load(options);
//                npcs.clear();
                ConfigurationNode shopNode = root.getNode("shops");
                if (!shopNode.isVirtual()) {
                    List<? extends ConfigurationNode> shopList = shopNode.getChildrenList();
                    for (int i = 0; i < shopList.size(); i++) {
                        try {
                            npcs.add(shopList.get(i).getValue(TypeToken.of(NPCguard.class)));
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

    void unloadShops(UUID world) {
        synchronized (npcs) {
            Set<NPCguard> toUnload = new HashSet<>();
            for (NPCguard npc : npcs) {
                if (npc.getLoc().getExtent().getUniqueId().equals(world))
                    toUnload.add(npc);
            }
            npcs.removeAll(toUnload);
            l(" > Shops for world %s unloaded", world.toString());
        }
    }

    void saveConfigs() {
        saveShops();
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

    void saveShops() {
        synchronized (npcs) {
            if (!npcsDirty.getAndSet(false)) {
                return;
            }
            l("Saving VillagerShops...");
            for (World w : Sponge.getServer().getWorlds()) {
                saveShops(w.getUniqueId());
            }
        }
    }
    @SuppressWarnings("serial")
    private void saveShops(UUID world) {
        ConfigurationOptions options = ConfigurationOptions.defaults().setSerializers(customSerializer);
        try {
            HoconConfigurationLoader loader = HoconConfigurationLoader.builder()
                    .setDefaultOptions(options)
                    .setPath(privateConfigDir.resolve("world_" + world.toString() + ".conf")).build();
            ConfigurationNode root = loader.createEmptyNode(options);
            List<NPCguard> worldNpcs = new LinkedList<>();
            for (NPCguard npc : npcs)
                if (npc.getLoc().getExtent().getUniqueId().equals(world))
                    worldNpcs.add(npc);
            root.getNode("shops").setValue(NPCguardSerializer.tokenListNPCguard, worldNpcs);
            loader.save(root);
        } catch (Exception e1) {
            Sponge.getServer().getBroadcastChannel().send(Text.of(TextColors.RED, "[VShop] Error: ", e1.getMessage()));
            e1.printStackTrace();
        }
        l(" > Shops for world %s saved", world.toString());
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
