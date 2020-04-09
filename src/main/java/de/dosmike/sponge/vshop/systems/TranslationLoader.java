package de.dosmike.sponge.vshop.systems;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.stream.JsonReader;
import de.dosmike.sponge.langswitch.LangSwitch;
import de.dosmike.sponge.vshop.ConfigSettings;
import de.dosmike.sponge.vshop.VillagerShops;
import org.spongepowered.api.Platform;
import org.spongepowered.api.Sponge;
import org.spongepowered.api.config.ConfigRoot;
import org.spongepowered.api.plugin.PluginContainer;
import org.spongepowered.api.scheduler.Task;
import org.spongepowered.api.text.Text;
import org.spongepowered.api.text.action.TextActions;
import org.spongepowered.api.text.format.TextColors;
import org.spongepowered.api.text.format.TextStyles;

import javax.net.ssl.HttpsURLConnection;
import java.io.*;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * if enabled in settings.conf fetchTranslations is called.
 * This method checks if any translation is present and in case there isn't starts the FileFetcher.
 * This file fetcher will first read the directory listing for translations on the master branch,
 * followed by downloading these files into the configuration directory.
 *
 * The user agent will expose the used plugin, sponge and minecraft version to github (not github users)
 */
public class TranslationLoader {

    private static Task notificationTask = null;
    private static void startTranslationNotifications() {
        if (notificationTask != null) return;
        notificationTask = Task.builder()
                .name("Translation notification")
                .interval(15, TimeUnit.SECONDS)
                .async()
                .execute(()->{
                    Sponge.getServer().getBroadcastChannel().send(Text.of(TextColors.RED, "[vshop] VillagerShops is missing translations!"));
                    Sponge.getServer().getBroadcastChannel().send(Text.of(TextColors.RED, "[vshop] Enable auto download in the settings.conf or download them from github"));
                })
                .submit(VillagerShops.getInstance());
    }
    private static void stopTranslationNotifications() {
        if (notificationTask == null) return;
        notificationTask.cancel();
        notificationTask = null;
    }

    private static HttpsURLConnection prepareConnectionTo(String remoteurl) {
        try {
            HttpsURLConnection con = (HttpsURLConnection)new URL(remoteurl).openConnection();
            con.setRequestProperty("Accept", "application/vnd.github.v3+json");
            con.setRequestProperty("User-Agent", UserAgent);
            con.setRequestProperty("Accept-Encoding", "identity");
            return con;
        } catch (Exception e) {
            VillagerShops.w("Could not connect to GitHub.com");
        }
        return null;
    }

    private static class FileFetcher implements Runnable {
        @Override
        public void run() {
            HttpsURLConnection con = prepareConnectionTo("https://api.github.com/repos/DosMike/VillagerShops/contents/release/config/vshop/Lang");
            if (con != null) {
                Map<String, String> gitUrl = new HashMap<>();
                try {
                    if (con.getResponseCode() != 200) {
                        VillagerShops.l("Connection to GitHub was rejected: %d %s", con.getResponseCode(), con.getResponseMessage());
                        VillagerShops.l("The remaining Rate-Limit is currently %s", con.getHeaderField("X-RateLimit-Remaining"));
                        return;
                    }
                    JsonReader reader = new JsonReader(new InputStreamReader(con.getInputStream()));
                    JsonParser parser = new JsonParser();
                    JsonElement root = parser.parse(reader);
                    root.getAsJsonArray().forEach(elem -> {
                        JsonObject file = elem.getAsJsonObject();
                        String name = file.get("name").getAsString();
                        gitUrl.put(name, file.get("download_url").getAsString());
                    });
                    reader.close();

                    HttpsURLConnection dl;
                    for (Map.Entry<String, String> e : gitUrl.entrySet()) {
                        File f = new File(configRoot, e.getKey());
                        dl = prepareConnectionTo(e.getValue());
                        if (dl == null || dl.getResponseCode() != 200) {
                            VillagerShops.w("Could not download file %s!", e.getKey());
                            continue;
                        }
                        OutputStream out = null;
                        InputStream in = null;
                        VillagerShops.l("Downloading %s...", e.getValue());
                        try {
                            out = new FileOutputStream(f);
                            in = dl.getInputStream();
                            byte[] buffer = new byte[512]; int r;
                            while ((r = in.read(buffer))>=0) {
                                out.write(buffer,0,r);
                            }
                        } catch (IOException ee) {
                            ee.printStackTrace();
                        } finally {
                            try { out.flush(); } catch (Exception ignore) {}
                            try { out.close(); } catch (Exception ignore) {}
                            try { in.close(); } catch (Exception ignore) {}
                        }
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
            LangSwitch.forceReloadTranslations();
            VillagerShops.l("Translation download finished");
            VillagerShops.w("If translations do not load automatically, reload LangSwitch or get support in my discord");
            try {
                Sponge.getServer().getBroadcastChannel().send(
                        Text.of(TextColors.YELLOW, "[vshop] Translations should have reloaded. If not you can get support in my ",
                                Text.builder("Discord")
                                        .onClick(TextActions.openUrl(new URL("https://discord.gg/E592Gdu")))
                                        .style(TextStyles.UNDERLINE)
                                        .color(TextColors.BLUE)
                                        .build())
                );
            } catch (MalformedURLException ignore) {}
            configRoot = null;
            UserAgent = null;
        }
    }

    private static String UserAgent = null;
    private static File configRoot = null;

    /**
     * Builds the user-agent string, containing plugin, sponge and server version,
     * before checking if translations are present.<br/>
     * If there are no translations and settings.conf allows auto download, the
     * download task FileFetcher will be started.<br/>
     * If there are no translations and downloads are not allowed, a periodic message
     * will be scheduled.<br/>
     * Otherwise nothing happens.
     * @param forceDownload true, if <code>/vshop reload --translations</code> was called
     */
    public static void fetchTranslations(boolean forceDownload) {
        if (UserAgent == null) {
            ConfigRoot cfgRoot = Sponge.getConfigManager().getPluginConfig(VillagerShops.getInstance());

            PluginContainer contThis = Sponge.getPluginManager().fromInstance(VillagerShops.getInstance()).get();
            PluginContainer contImp = Sponge.getPlatform().getContainer(Platform.Component.IMPLEMENTATION);
            PluginContainer contApi = Sponge.getPlatform().getContainer(Platform.Component.API);
            PluginContainer contGame = Sponge.getPlatform().getContainer(Platform.Component.GAME);

            String strThis = contThis.getName() + "/" + contThis.getVersion().get();
            String strImp = contImp.getName() + "/" + contImp.getVersion().get();
            String strApi = contApi.getName() + "/" + contApi.getVersion().get();
            String strGame = contGame.getName() + "/" + contGame.getVersion().get();

            UserAgent = strThis + " (Plugin by DosMike) " + strApi + " " + strImp + " " + strGame;
            VillagerShops.l("User-Agent: %s", UserAgent);

            configRoot = cfgRoot.getDirectory().resolve("Lang").toFile();
        }

        stopTranslationNotifications(); //in case it is still running
        File[] translations = configRoot.listFiles();
        if (translations == null || translations.length == 0 || forceDownload) {
            VillagerShops.w("There are currently no translations on the server");
            if (ConfigSettings.isAutoDownloadingAllowed() || forceDownload) {
                VillagerShops.l("Starting download job...");
                configRoot.mkdirs();
                VillagerShops.getAsyncScheduler().execute(new FileFetcher());
            } else {
                startTranslationNotifications();
            }
        } else {
            //no version checking because git performs line-end conversions
            //so hashes might not be equal even if readable content is the same
        }

    }

}
