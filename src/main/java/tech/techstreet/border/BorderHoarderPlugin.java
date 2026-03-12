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

package tech.techstreet.border;

import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;
import tech.techstreet.border.command.CommandHandler;
import tech.techstreet.border.events.EventHandler;
import tech.techstreet.border.gui.item.Colours;
import tech.techstreet.border.lib.border.BorderHandler;
import tech.techstreet.border.lib.border.VersionHandler;

public final class BorderHoarderPlugin extends JavaPlugin {
    private static BorderHoarderPlugin instance;
    private static BorderHandler borderHandler;
    private static VersionHandler versionHandler;
    private static final String MINECRAFT_VERSION = "1.21.11";

    /**
     * Called when the plugin is enabled.
     */
    @Override
    public void onEnable() {
        instance = this;

        if (Bukkit.getServer().getMinecraftVersion().equals(MINECRAFT_VERSION)) {
            borderHandler = new BorderHandler(instance);
            borderHandler.load();
        } else {
            Bukkit.getConsoleSender().sendMessage(Component.text("[" + getPluginName() + "] Unsupported Minecraft version detected.", Colours.RED));
            Bukkit.getConsoleSender().sendMessage(Component.text("[" + getPluginName() + "] This plugin supports Minecraft " + MINECRAFT_VERSION + " only, you are running " + Bukkit.getServer().getMinecraftVersion(), Colours.RED));
        }

        saveDefaultConfig();
        reloadConfig();

        Bukkit.motd(Component.text("Border Hoarder v" + this.getDescription().getVersion()));
        CommandHandler commandHandler = new CommandHandler(instance);
        EventHandler eventHandler = new EventHandler(instance);
        versionHandler = new VersionHandler(instance);

        commandHandler.load();
        versionHandler.load();
        eventHandler.load();
    }

    /**
     * The version of Minecraft this plugin runs.
     *
     * @return the Minecraft version this plugin runs.
     */
    public static String getMinecraftVersion() {
        return MINECRAFT_VERSION;
    }

    /**
     * Gets the plugin name.
     * @return the plugin name.
     */
    public static String getPluginName() {
        return instance.getName();
    }

    /**
     * Gets the instance of the BorderExplorerPlugin.
     * @return the BorderExplorerPlugin instance.
     */
    public static BorderHoarderPlugin getInstance() {
        return instance;
    }

    /**
     * Gets the BorderHandler instance.
     * @return the BorderHandler instance.
     */
    public static BorderHandler getBorderHandler() {
        return borderHandler;
    }

    /**
     * Gets the VersionHandler instance.
     *
     * @return the VersionHandler instance.
     */
    public static VersionHandler getVersionHandler() {
        return versionHandler;
    }
}
