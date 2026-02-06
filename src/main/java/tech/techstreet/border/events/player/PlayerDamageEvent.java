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

package tech.techstreet.border.events.player;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.FoodLevelChangeEvent;
import tech.techstreet.border.lib.user.User;
import tech.techstreet.border.lib.user.UserManager;
import tech.techstreet.border.lib.user.UserState;

public class PlayerDamageEvent implements Listener {

    /**
     * Prevents players in the LOBBY state from taking damage.
     *
     * @param event The EntityDamageEvent to handle.
     */
    @EventHandler
    public void onEvent(EntityDamageEvent event) {
        if (event.getEntity() instanceof Player player) {
            User user = UserManager.of(player);
            if (user.getState() == UserState.LOBBY) {
                event.setCancelled(true);
            }
        }
    }

    /**
     * Prevents players in the LOBBY state from losing hunger.
     *
     * @param event The FoodLevelChangeEvent to handle.
     */
    @EventHandler
    public void onEvent(FoodLevelChangeEvent event) {
        if (event.getEntity() instanceof Player player) {
            User user = UserManager.of(player);
            if (user.getState() == UserState.LOBBY) {
                event.setCancelled(true);
            }
        }
    }
}