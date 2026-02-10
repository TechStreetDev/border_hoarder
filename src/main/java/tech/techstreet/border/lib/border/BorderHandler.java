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

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.Style;
import org.bukkit.*;
import org.bukkit.command.ConsoleCommandSender;
import org.bukkit.entity.Display;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.entity.TextDisplay;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scoreboard.*;
import tech.techstreet.border.BorderHoardersPlugin;
import tech.techstreet.border.gui.item.Colours;
import tech.techstreet.border.lib.border.tests.BorderItemTest;
import tech.techstreet.border.lib.item.BoarderItem;
import tech.techstreet.border.lib.user.User;
import tech.techstreet.border.lib.user.UserManager;
import tech.techstreet.border.lib.user.UserState;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;

public class BorderHandler {
    private List<BoarderItem> completedItems;
    private final HashMap<Float, Entity> textDisplays = new HashMap<>();
    private static final ConsoleCommandSender LOGGER = Bukkit.getConsoleSender();
    private static NamespacedKey counterKey;
    private final BorderHoardersPlugin instance;
    private Scoreboard scoreboard;
    private World spawnWorld;

    public BorderHandler(BorderHoardersPlugin instance) {
        this.instance = instance;
    }

    /**
     * Loads the border handler, initializes the spawn world, and sets up scheduled tasks.
     */
    public void load() {
        try {
            spawnWorld = new WorldCreator("world_spawn").createWorld();
            if (spawnWorld != null) {
                spawnWorld.setChunkForceLoaded(0, -1, true);
                for (Entity entity : spawnWorld.getEntities()) {
                    entity.remove();
                }

                spawnWorld.setDifficulty(Difficulty.PEACEFUL);
            }

            counterKey = new NamespacedKey(BorderHoardersPlugin.getInstance(), "counter");
            completedItems = ProgressHandler.loadWorld();
            instance.getServer().getScheduler().scheduleSyncRepeatingTask(instance, () -> {
                for (Player player : Bukkit.getOnlinePlayers()) {
                    for (ItemStack item : player.getInventory().getContents()) {
                        if (item != null) {
                            BorderHoardersPlugin.getBorderHandler().addCompletedItem(player, item.getType());
                        }
                    }
                }
            }, 40, 40);

            instance.getServer().getScheduler().runTask(instance, () -> BorderHoardersPlugin.getBorderHandler().syncBoarder());
            instance.getServer().getScheduler().scheduleSyncRepeatingTask(instance, () -> {
                // Write all auto saved player locations to the map.
                for (Player player : Bukkit.getOnlinePlayers()) {
                    User user = UserManager.of(player);

                    if (user.getState() == UserState.PLAY) {
                        ProgressHandler.updateState(player.getUniqueId(), player.getLocation(), UserState.PLAY);
                    } else {
                        ProgressHandler.updateState(player.getUniqueId(), ProgressHandler.getLastLocations().get(player.getUniqueId()), UserState.LOBBY);
                    }
                }

                // Save everything to file, once all players locations have been updated.
                ProgressHandler.saveWorld(completedItems);
            }, 1200, 1200);

            ScoreboardManager manager = Bukkit.getScoreboardManager();
            scoreboard = manager.getNewScoreboard();

            Objective objective = scoreboard.registerNewObjective(
                    "counter",
                    Criteria.DUMMY,
                    Component.text("Points", Colours.GREEN)
            );

            objective.setDisplaySlot(DisplaySlot.PLAYER_LIST);

            BorderItemTest.run();
            Bukkit.getScheduler().runTaskLater(instance, this::syncBoarder, 20);
        } catch (Exception e) {
            LOGGER.sendMessage(Component.text("[" + BorderHoardersPlugin.getPluginName() + "] Failed to retrieve saved items file.", Colours.RED));
            completedItems = null;
        }

    }

    /**
     * Saves the player's current location based on their state.
     *
     * @param player the player whose location is to be saved
     */
    public void saveLocation(Player player) {
        User user = UserManager.of(player);

        if (user.getState() == UserState.PLAY) {
            ProgressHandler.updateState(player.getUniqueId(), player.getLocation(), UserState.PLAY);
        }

        if (user.getState() == UserState.LOBBY) {
            ProgressHandler.updateState(player.getUniqueId(), ProgressHandler.getLastLocations().get(player.getUniqueId()), UserState.LOBBY);
        }

        ProgressHandler.saveWorld(completedItems);
    }

    /**
     * Synchronizes the world borders and updates the display texts.
     */
    public void syncBoarder() {
        try {
            World world = Bukkit.getWorld("world");
            World worldNether = Bukkit.getWorld("world_nether");
            World worldEnd = Bukkit.getWorld("world_the_end");

            if (world == null || worldNether == null || worldEnd == null) {
                LOGGER.sendMessage(Component.text("[" + BorderHoardersPlugin.getPluginName() + "] Cannot sync border, one or more worlds are missing.", Colours.RED));
                return;
            }

            world.setSpawnLocation(576, 67, -517);

            WorldBorder borderWorld = world.getWorldBorder();
            borderWorld.setCenter(576.50, -517.50);
            borderWorld.setSize((1 + (2 * getTotalItems())), 1);

            WorldBorder borderNether = worldNether.getWorldBorder();
            borderNether.setCenter(72.5, -64.5);
            borderNether.setSize((1 + (2 * getTotalItems())), 1);

            WorldBorder borderEnd = worldEnd.getWorldBorder();
            borderEnd.setCenter(0.5, 0.5);
            borderEnd.setSize((1 + (32 * getTotalItems())), 1);

            WorldBorder borderSpawn = spawnWorld.getWorldBorder();
            borderSpawn.setCenter(0.5, 0.5);
            borderSpawn.setSize(100, 0);

            for (Entity entity : spawnWorld.getEntities()) {
                if (entity instanceof TextDisplay && !(textDisplays.containsValue(entity))) {
                    entity.remove();
                }
            }

            int unlocked = (1 + (2 * getTotalItems())) * (1 + (2 * getTotalItems()));
            int available = (1 + (2 * BoarderItem.values().length)) * (1 + (2 * BoarderItem.values().length));
            int deaths = 0;

            for (OfflinePlayer player : Bukkit.getOfflinePlayers()) {
                deaths += player.getStatistic(Statistic.DEATHS);
            }

            createHologramText(92.0f, Component.text("Items Collected: " + getTotalItems() + "/" + BoarderItem.values().length + " (" + (int) (((double) getTotalItems() / (double) BoarderItem.values().length) * 100) + "%)"));
            createHologramText(91.7f, Component.text("Area Unlocked: " + formatNumber(unlocked) + " / " + formatNumber(available) + " (" + (int) (((double) unlocked / (double) available) * 100) + "%)"));
            createHologramText(91.4f, Component.text("Deaths: " + formatNumber(deaths)));

            createWallText(0.5f, 94.0f, -12.0f, Component.text("Border Hoarders"));
            createWallText(0.5f, 93.7f, -12.0f, Component.text("by TechStreet"));

        } catch (Exception e) {
            LOGGER.sendMessage(Component.text("[" + BorderHoardersPlugin.getPluginName() + "] Cannot sync border, " + e, Colours.RED));
        }
    }

    /**
     * Formats a number into a human-readable string with appropriate suffixes.
     *
     * @param value the number to format
     * @return the formatted string
     */
    public static String formatNumber(long value) {
        if (value < 1_000) return String.valueOf(value);

        final String[] units = {"K", "M", "B", "T"};
        double v = value;
        int unitIndex = -1;

        while (v >= 1_000 && unitIndex < units.length - 1) {
            v /= 1_000;
            unitIndex++;
        }

        return String.format(v % 1 == 0 ? "%.0f%s" : "%.1f%s", v, units[unitIndex]);
    }

    /**
     * Retrieves the BoarderItem enum corresponding to the given Material.
     *
     * @param material the material to look up
     * @return the corresponding BoarderItem, or null if not found
     */
    private BoarderItem getFromMaterial(Material material) {
        try {
            return BoarderItem.valueOf(material.name());
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * Gets the list of completed BoarderItems.
     *
     * @return the list of completed items
     */
    public List<BoarderItem> getCompletedItems() {
        return completedItems;
    }

    /**
     * Gets the list of missing BoarderItems.
     *
     * @return the list of missing items
     */
    public List<BoarderItem> getMissingItems() {
        return Arrays.stream(BoarderItem.values())
                .filter(item -> !completedItems.contains(item))
                .toList();
    }

    /**
     * Creates or updates a TextDisplay at the specified Y-coordinate with the given component.
     *
     * @param y         the Y-coordinate for the TextDisplay
     * @param component the text component to display
     */
    public void createHologramText(float y, Component component) {
        TextDisplay armorStand = (TextDisplay) textDisplays.getOrDefault(y, null);
        if (armorStand == null) armorStand = spawnWorld.spawn(new Location(spawnWorld, 4.5, y, -6), TextDisplay.class);
        textDisplays.put(y, armorStand);

        armorStand.text(component);
        armorStand.setBillboard(Display.Billboard.CENTER);
        armorStand.setAlignment(TextDisplay.TextAlignment.CENTER);
        armorStand.setSeeThrough(true);
        armorStand.setShadowed(true);
    }

    /**
     * Creates or updates a TextDisplay at the specified Y-coordinate with the given component, intended for wall display.
     *
     * @param x         the X-coordinate for the TextDisplay
     * @param y         the Y-coordinate for the TextDisplay
     * @param z         the Z-coordinate for the TextDisplay
     * @param component the text component to display
     */
    public void createWallText(float x, float y, float z, Component component) {
        TextDisplay armorStand = (TextDisplay) textDisplays.getOrDefault(y, null);
        if (armorStand == null) armorStand = spawnWorld.spawn(new Location(spawnWorld, x, y, z), TextDisplay.class);
        textDisplays.put(y, armorStand);

        armorStand.text(component);
        armorStand.setAlignment(TextDisplay.TextAlignment.CENTER);
        armorStand.setSeeThrough(true);
        armorStand.setShadowed(false);
    }

    /**
     * Adds a completed item for the specified player.
     *
     * @param player the player who collected the item
     * @param item   the material of the collected item
     */
    public void addCompletedItem(Player player, Material item) {
        BoarderItem boarderItem = getFromMaterial(item);
        if (UserManager.of(player).getState() != UserState.PLAY) return;
        if (boarderItem == null) return;
        if (!completedItems.contains(boarderItem)) {
            ItemStack itemStack = new ItemStack(item, 1);
            completedItems.add(boarderItem);
            syncBoarder();

            ProgressHandler.saveWorld(completedItems);
            for (Player p : Bukkit.getOnlinePlayers())
                p.playSound(p.getLocation(), Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 1, 1);

            UserManager.of(player).incrementCounter();
            Bukkit.broadcast(Component.text("⇵ ", Colours.LIME_DARK)
                    .append(Component.text(player.getName() + " has collected the item: ", Colours.LIME)
                            .append(itemStack.effectiveName().style(Style.style().build()).color(Colours.LIME))));
        }
    }

    /**
     * Gets the scoreboard used for displaying player information.
     *
     * @return the scoreboard
     */
    public Scoreboard getScoreboard() {
        return scoreboard;
    }

    /**
     * Gets the spawn world.
     *
     * @return the spawn world
     */
    public World getSpawnWorld() {
        return spawnWorld;
    }

    /**
     * Gets the NamespacedKey used for the counter.
     *
     * @return the counter NamespacedKey
     */
    public static NamespacedKey getCounterKey() {
        return counterKey;
    }

    /**
     * Gets the total number of completed items.
     *
     * @return the total completed items
     */
    public int getTotalItems() {
        return completedItems.size();
    }
}