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

package tech.techstreet.border.events.player;

import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerPreLoginEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import tech.techstreet.border.BorderHoardersPlugin;
import tech.techstreet.border.lib.border.ProgressHandler;
import tech.techstreet.border.lib.user.User;
import tech.techstreet.border.lib.user.UserManager;
import tech.techstreet.border.lib.user.UserState;

public class PlayerConnectionEvent implements Listener {

    /**
     * Handles the AsyncPlayerPreLoginEvent to ensure server readiness before allowing player login.
     *
     * @param event The AsyncPlayerPreLoginEvent to handle
     */
    @EventHandler
    public void onEvent(AsyncPlayerPreLoginEvent event) {
        if (Bukkit.getWorld("world") == null || Bukkit.getWorld("world_nether") == null || Bukkit.getWorld("world_the_end") == null) {
            event.disallow(AsyncPlayerPreLoginEvent.Result.KICK_OTHER, Component.text("Server world is not setup correctly."));
            return;
        }

        if (BorderHoardersPlugin.getBorderHandler() == null || BorderHoardersPlugin.getBorderHandler().getCompletedItems() == null) {
            event.disallow(AsyncPlayerPreLoginEvent.Result.KICK_OTHER, Component.text("Server is not started yet."));
            return;
        }

        if (BorderHoardersPlugin.getBorderHandler().getSpawnWorld() == null) {
            event.disallow(AsyncPlayerPreLoginEvent.Result.KICK_OTHER, Component.text("Server world is not setup correctly."));
        }
    }

    /**
     * Handles the PlayerJoinEvent to update user data and sync border information.
     *
     * @param event The PlayerJoinEvent to handle
     */
    @EventHandler
    public void onEvent(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        UserManager.update(player);
        BorderHoardersPlugin.getBorderHandler().syncBoarder();
        User user = UserManager.of(player);

        // Send player back to previous state, but default to the lobby
        user.setState(ProgressHandler.getLastStates().getOrDefault(player.getUniqueId(), UserState.LOBBY));
    }

    /**
     * Handles the PlayerQuitEvent to log out the user and clean up resources.
     *
     * @param event The PlayerQuitEvent to handle
     */
    @EventHandler
    public void onEvent(PlayerQuitEvent event) {
        User user = UserManager.of(event.getPlayer());
        UserManager.logout(user.asPlayer());
    }
}