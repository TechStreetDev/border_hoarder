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

package tech.techstreet.border.events.world;

import org.bukkit.Material;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityPortalEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerPortalEvent;
import tech.techstreet.border.lib.user.User;
import tech.techstreet.border.lib.user.UserManager;
import tech.techstreet.border.lib.user.UserState;

public class WorldPortalEvent implements Listener {

    /**
     * Prevents spawn portal from generating within the nether
     * This prevents players from reverse teleporting(Nether -> Spawn)
     */
    @EventHandler
    public void onEvent(PlayerPortalEvent event) {
        User user = UserManager.of(event.getPlayer());

        if (event.getFrom().getWorld().getName().equals("world_spawn")) {
            event.setCanCreatePortal(false);
            event.setCancelled(true);
            user.setState(UserState.PLAY);
        }
    }

    /**
     * Detects when a player enters a nether portal in the lobby and updates their state to PLAY.
     */
    @EventHandler
    public void onEvent(PlayerMoveEvent event) {
        User user = UserManager.of(event.getPlayer());

        if (event.getTo().getBlock().getType() == Material.NETHER_PORTAL && user.getState() == UserState.LOBBY) {
            user.setState(UserState.PLAY);
        }
    }

    /**
     * Prevents spawn portal from generating within the nether
     *
     * @param event The EntityPortalEvent to handle
     */
    @EventHandler
    public void onEvent(EntityPortalEvent event) {
        if (event.getFrom().getWorld().getName().equals("world_spawn")) {
            event.setCanCreatePortal(false);
            event.setCancelled(true);
        }
    }
}