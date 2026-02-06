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

package tech.techstreet.border.lib.user;

import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.scoreboard.Objective;
import org.bukkit.scoreboard.Scoreboard;
import org.jetbrains.annotations.Nullable;
import tech.techstreet.border.BorderHoardersPlugin;
import tech.techstreet.border.gui.Button;
import tech.techstreet.border.gui.Menu;
import tech.techstreet.border.gui.MenuInstance;
import tech.techstreet.border.lib.border.BorderHandler;
import tech.techstreet.border.lib.border.ProgressHandler;

import java.util.List;
import java.util.UUID;

public class User {
    private final Player player;
    private UserState state = null;
    private @Nullable MenuInstance menuInstance;

    /**
     * Constructs a new UserImpl instance for the given player.
     *
     * @param player the player associated with this user.
     */
    public User(final Player player) {
        this.player = player;
    }

    /**
     * Converts this user to its associated Player object.
     *
     * @return the Player object.
     */
    public Player asPlayer() {
        return player;
    }

    /**
     * Retrieves the current state of the user.
     *
     * @return the user state.
     */
    public UserState getState() {
        return state;
    }

    public void setState(UserState state) {
        // Save current location before changing state.
        BorderHoardersPlugin.getBorderHandler().saveLocation(player);
        this.state = state;

        if (state == UserState.PLAY) {
            Location location = ProgressHandler.getLastLocations().get(player.getUniqueId());
            if (location == null) location = new Location(Bukkit.getWorld("world"), 576.50, 67, -517.50);

            player.teleport(location);
            player.playSound(player.getLocation(), Sound.ENTITY_ENDERMAN_TELEPORT, 1.0F, 1.0F);
            player.setScoreboard(BorderHoardersPlugin.getBorderHandler().getScoreboard());
            player.setGameMode(GameMode.SURVIVAL);
        }

        if (state == UserState.LOBBY) {
            player.teleport(new Location(Bukkit.getWorld("world_spawn"), 0.5, 90, 0.5, 180, 0));
            player.playSound(player.getLocation(), Sound.ENTITY_ENDERMAN_TELEPORT, 1.0F, 1.0F);
            player.setScoreboard(BorderHoardersPlugin.getBorderHandler().getScoreboard());
            player.setGameMode(GameMode.SURVIVAL);
        }

        Scoreboard board = player.getScoreboard();
        Objective objective = board.getObjective("counter");
        if (objective == null) return;

        objective.getScore(player.getName()).setScore(getCounter());

        BorderHoardersPlugin.getBorderHandler().saveLocation(player); // Save state changes
        BorderHoardersPlugin.getBorderHandler().syncBoarder();
        Bukkit.getScheduler().runTaskLater(BorderHoardersPlugin.getInstance(), run -> BorderHoardersPlugin.getBorderHandler().syncBoarder(), 10L);
    }

    /**
     * Retrieves the currently open menu instance for a user.
     *
     * @return the menu instance.
     */
    @Nullable
    public MenuInstance getMenuInstance() {
        return menuInstance;
    }

    /**
     * Sets the menu instance for a user.
     *
     * @param menuInstance the current menu instance.
     */
    public void setMenuInstance(@Nullable final MenuInstance menuInstance) {
        if (menuInstance != null) {
            if (menuInstance.menu().getUser().getUniqueId() != this.getUniqueId()) {
                throw new RuntimeException("User must be the owner of the menu.");
            }

            this.menuInstance = menuInstance;
        } else {
            this.menuInstance = null;
        }
    }

    /**
     * This method creates and opens the menu for the user, it also applies any
     * translation based inventory titles and item modifications.
     *
     * @param menu the menu to open.
     */
    public void openMenu(final Menu menu) {
        if (menu == null || menu.getUser().getUniqueId() != this.getUniqueId()) {
            throw new RuntimeException("User must be the owner of the menu.");
        }

        final List<Button> buttons = menu.getItems(this);
        final Inventory inventory = Bukkit.createInventory(null, menu.getSlots(), menu.getTitle(this));

        int index = 0;
        for (final Button button : buttons) {
            button.setAutoCancel(menu.getCancel());
            inventory.setItem(index, button.getItemStack());
            index += 1;
        }

        final MenuInstance instance = new MenuInstance(menu, buttons);

        this.asPlayer().openInventory(inventory);
        this.setMenuInstance(instance);
        if (this.getMenuInstance() != null) this.getMenuInstance().menu().onOpen(this);
    }

    /**
     * Get the counter value for the user.
     *
     * @return the counter value.
     */
    public int getCounter() {
        PersistentDataContainer pdc = player.getPersistentDataContainer();
        return pdc.getOrDefault(BorderHandler.getCounterKey(), PersistentDataType.INTEGER, 0);
    }

    /**
     * Set the counter value for the user.
     *
     * @param value the counter value to set.
     */
    public void setCounter(int value) {
        player.getPersistentDataContainer().set(
                BorderHandler.getCounterKey(),
                PersistentDataType.INTEGER,
                value
        );

        Scoreboard board = player.getScoreboard();
        Objective objective = board.getObjective("counter");
        if (objective == null) return;

        objective.getScore(player.getName()).setScore(value);
    }

    /**
     * Increment the counter value for the user by 1.
     */
    public void incrementCounter() {
        setCounter(getCounter() + 1);
    }

    /**
     * Get the unique ID of the user.
     *
     * @return the unique UUID.
     */
    public UUID getUniqueId() {
        return player.getUniqueId();
    }
}