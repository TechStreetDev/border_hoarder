/*
 * Copyright (C) 2026 TechStreetDev
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published
 * by the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program. If not, see <https://www.gnu.org/licenses/>.
 */

package tech.techstreet.border.lib.border.tests;

import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.command.ConsoleCommandSender;
import tech.techstreet.border.BorderHoardersPlugin;
import tech.techstreet.border.gui.item.Colours;
import tech.techstreet.border.lib.item.BlacklistedItems;
import tech.techstreet.border.lib.item.BoarderItem;

import java.io.File;
import java.nio.file.Files;
import java.util.Arrays;
import java.util.List;

public class BorderItemTest {
    private static final ConsoleCommandSender LOGGER = Bukkit.getConsoleSender();

    /**
     * Load and run the BorderItem tests.
     */
    public static void run() {
        // Test for items missing from both BlacklistedItems & BoarderItem
        List<String> itemStrings = new java.util.ArrayList<>(Arrays.stream(Material.values())
                .map(Material::name)
                .toList());

        for (BlacklistedItems blacklistedItem : BlacklistedItems.values()) itemStrings.remove(blacklistedItem.name());
        for (BoarderItem boarderItem : BoarderItem.values()) itemStrings.remove(boarderItem.name());

        if (!itemStrings.isEmpty()) {
            try {
                for (String itemString : itemStrings) {
                    LOGGER.sendMessage(Component.text("[" + BorderHoardersPlugin.getPluginName() + "] Missing: Item is not in BlacklistedItems or BoarderItem enum: " + itemString, Colours.RED));
                }

                File file = new File("missing-output.txt");
                Files.writeString(file.toPath(), String.join(",\n", itemStrings));
            } catch (Exception e) {
                LOGGER.sendMessage(Component.text("[" + BorderHoardersPlugin.getPluginName() + "] Failed to write missing items to file.", Colours.RED));
            }
        }

        // Test for items in both BlacklistedItems & BorderItem
        List<String> blacklistedItems = Arrays.stream(BlacklistedItems.values())
                .map(BlacklistedItems::name)
                .toList();

        for (BoarderItem boarderItem : BoarderItem.values()) {
            if (blacklistedItems.contains(boarderItem.name())) {
                LOGGER.sendMessage(Component.text("[" + BorderHoardersPlugin.getPluginName() + "] Conflict: Item in both BlacklistedItems & BoarderItem enum: " + boarderItem.name(), Colours.RED));
            }
        }

        // Test for items which are no longer in Minecraft
        List<String> items = new java.util.ArrayList<>(Arrays.stream(Material.values())
                .map(Material::name)
                .toList());

        for (BoarderItem boarderItem : BoarderItem.values()) {
            if (!items.contains(boarderItem.name())) {
                LOGGER.sendMessage(Component.text("[" + BorderHoardersPlugin.getPluginName() + "] Deprecated: Item in BoarderItem enum which is no longer in Minecraft: " + boarderItem.name(), Colours.RED));
            }
        }

        for (BlacklistedItems blacklistedItem : BlacklistedItems.values()) {
            if (!items.contains(blacklistedItem.name())) {
                LOGGER.sendMessage(Component.text("[" + BorderHoardersPlugin.getPluginName() + "] Deprecated: Item in BlacklistedItems enum which is no longer in Minecraft: " + blacklistedItem.name(), Colours.RED));
            }
        }
    }
}