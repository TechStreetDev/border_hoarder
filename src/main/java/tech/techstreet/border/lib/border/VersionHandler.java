/*
 * Copyright (C) 2026 TechStreetDev
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License version 3
 * as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program. If not, see <https://www.gnu.org/licenses/>.
 */

package tech.techstreet.border.lib.border;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.plugin.PluginDescriptionFile;
import org.bukkit.plugin.java.JavaPlugin;
import tech.techstreet.border.BorderHoarderPlugin;
import tech.techstreet.border.gui.item.Colours;

import java.io.*;
import java.net.URI;
import java.net.URL;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

public class VersionHandler {
    private final BorderHoarderPlugin instance;
    private final String version;
    private final String major;

    /**
     * Record to hold version information.
     *
     * @param name        The version name.
     * @param downloadUrl The URL to download the version.
     */
    private record VersionInfo(String name, String downloadUrl) {
    }

    /**
     * Constructor for VersionHandler.
     *
     * @param instance The main plugin instance.
     */
    public VersionHandler(BorderHoarderPlugin instance) {
        this.instance = instance;
        this.version = instance.getDescription().getVersion();
        this.major = instance.getDescription().getVersion().split("\\.")[0];
    }

    /**
     * Loads version updating checker.
     */
    public void load() {
        int interval = (20 * 60 * instance.getConfig().getInt("update.check-interval", 5)); // 5 minutes in ticks
        Bukkit.getScheduler().runTaskTimer(instance, () -> {
            try {
                VersionInfo latestVersion = fetchLatestVersion();
                if (latestVersion != null && !(latestVersion.name.equals(version))) {
                    if (instance.getConfig().getBoolean("update.auto-update", true)) {
                        Bukkit.getConsoleSender().sendMessage(Component.text("[" + BorderHoarderPlugin.getPluginName() + "] A new version (" + latestVersion.name + ") is available, running the auto updater!", Colours.GREEN));
                        update();
                    } else {
                        Bukkit.getConsoleSender().sendMessage(Component.text("[" + BorderHoarderPlugin.getPluginName() + "] A new version (" + latestVersion.name + ") is available! Run /update to update now.", Colours.GREEN));
                    }
                }
            } catch (InterruptedException | IOException e) {
                Bukkit.getConsoleSender().sendMessage(Component.text("[" + BorderHoarderPlugin.getPluginName() + "] Unable to check version information.", Colours.RED));
            }
        }, 0, interval);
    }

    /**
     * Fetches the latest version information from the ResourceLab API.
     *
     * @return VersionInfo containing the latest version name and download URL, or null if no new version is available or an error occurs.
     * @throws InterruptedException if the thread is interrupted while waiting for the HTTP response.
     * @throws IOException          if an I/O error occurs when sending or receiving the HTTP request.
     */
    private VersionInfo fetchLatestVersion() throws InterruptedException, IOException {
        HttpClient client = HttpClient.newHttpClient();
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("https://resourcelab.io/api/v1/resources/1b414ea9"))
                .GET()
                .build();

        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() != 200) {
            return null;
        }

        JsonObject root = JsonParser.parseString(response.body()).getAsJsonObject();
        JsonArray versions = root.getAsJsonArray("versions");

        if (versions == null || versions.isEmpty()) {
            return null;
        }

        for (JsonElement element : versions) {
            JsonObject versionObj = element.getAsJsonObject();
            String versionName = versionObj.get("name").getAsString();

            if (versionName.equals(version)) return null;
            if (versionName.startsWith(major + "."))
                return new VersionInfo(versionName, versionObj.get("download_url").getAsString());
        }

        return null;
    }

    /**
     * Finds the real plugin jar file for the given plugin instance by comparing the plugin.yml information with the jar files in the plugins folder.
     *
     * @param plugin The JavaPlugin instance to find the jar file for.
     * @return The File object representing the real plugin jar, or null if not found.
     */
    public File findRealPluginJar(JavaPlugin plugin) {
        File pluginsFolder = Bukkit.getPluginsFolder();

        String pluginName = plugin.getDescription().getName();
        String pluginVersion = plugin.getDescription().getVersion();

        File[] files = pluginsFolder.listFiles((dir, name) -> name.endsWith(".jar"));

        if (files == null) return null;

        for (File file : files) {
            try (JarFile jar = new JarFile(file)) {
                JarEntry entry = jar.getJarEntry("plugin.yml");
                if (entry == null) continue;

                try (InputStream in = jar.getInputStream(entry)) {
                    PluginDescriptionFile desc = new PluginDescriptionFile(in);
                    if (desc.getName().equals(pluginName)
                            && desc.getVersion().equals(pluginVersion)) {
                        return file;
                    }
                }
            } catch (Exception ignored) {
            }
        }

        return null;
    }

    /**
     * Downloads the latest version and schedules a server restart.
     */
    public void update() {
        Bukkit.getScheduler().runTaskAsynchronously(instance, () -> {
            try {
                VersionInfo latestVersion = fetchLatestVersion();
                File currentFile = findRealPluginJar(instance);

                if (latestVersion == null) {
                    throw new IOException("No new version available or failed to fetch version information.");
                }

                File pluginsFolder = Bukkit.getPluginsFolder();
                File tempZip = new File(pluginsFolder, "BorderHoarder-update.zip");

                // Download ZIP
                try (InputStream in = new URL(latestVersion.downloadUrl).openStream();
                     FileOutputStream out = new FileOutputStream(tempZip)) {

                    byte[] buffer = new byte[8192];
                    int bytesRead;

                    while ((bytesRead = in.read(buffer)) != -1) {
                        out.write(buffer, 0, bytesRead);
                    }
                }

                // Extract only the plugin jar from the downloaded ZIP
                boolean extracted = false;
                try (ZipInputStream zipIn = new ZipInputStream(new FileInputStream(tempZip))) {
                    ZipEntry entry;

                    while ((entry = zipIn.getNextEntry()) != null) {

                        if (entry.getName().startsWith("plugins/")
                                && entry.getName().endsWith(".jar")) {

                            String jarName = new File(entry.getName()).getName();
                            File newJar = new File(pluginsFolder, jarName);

                            try (FileOutputStream out = new FileOutputStream(newJar)) {

                                byte[] buffer = new byte[8192];
                                int bytesRead;

                                while ((bytesRead = zipIn.read(buffer)) != -1) {
                                    out.write(buffer, 0, bytesRead);
                                }
                            }

                            extracted = true;
                            break;
                        }
                    }
                }

                tempZip.delete();
                if (!extracted) {
                    Bukkit.getConsoleSender().sendMessage(Component.text("[" + BorderHoarderPlugin.getPluginName() + "] Update failed, aborting.", Colours.RED));
                } else {
                    if (!currentFile.delete()) {
                        Bukkit.getConsoleSender().sendMessage(Component.text("[" + BorderHoarderPlugin.getPluginName() + "] Update downloaded, but could not delete old jar. Please delete manually.", Colours.RED));
                    }

                    Bukkit.getConsoleSender().sendMessage(Component.text("[" + BorderHoarderPlugin.getPluginName() + "] Update downloaded successfully, restarting in 15 seconds!", Colours.GREEN));
                    Bukkit.broadcast(Component.text("The plugin is updating, restarting in 15 seconds!"));
                    Bukkit.getScheduler().runTaskLater(BorderHoarderPlugin.getInstance(), Bukkit::restart, (20 * 15)); // Restart after 15 seconds
                }
            } catch (Exception e) {
                Bukkit.getConsoleSender().sendMessage(Component.text("[" + BorderHoarderPlugin.getPluginName() + "] Failed to update plugin.", Colours.RED));
            }
        });
    }
}