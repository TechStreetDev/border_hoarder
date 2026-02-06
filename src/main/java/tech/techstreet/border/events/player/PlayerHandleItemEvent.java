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
import org.bukkit.event.entity.EntityPickupItemEvent;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerItemHeldEvent;
import org.bukkit.inventory.ItemStack;
import tech.techstreet.border.BorderHoardersPlugin;

public class PlayerHandleItemEvent implements Listener {

    /**
     * Handles the PlayerItemHeldEvent.
     *
     * @param event The PlayerItemHeldEvent to handle.
     */
    @EventHandler
    public void onEvent(PlayerItemHeldEvent event) {
        Player player = event.getPlayer();
        ItemStack item = event.getPlayer().getActiveItem();
        BorderHoardersPlugin.getBorderHandler().addCompletedItem(player, item.getType());
    }

    /**
     * Handles the PlayerDropItemEvent.
     *
     * @param event The PlayerDropItemEvent to handle.
     */
    @EventHandler
    public void onEvent(PlayerDropItemEvent event) {
        Player player = event.getPlayer();
        ItemStack item = event.getItemDrop().getItemStack();
        BorderHoardersPlugin.getBorderHandler().addCompletedItem(player, item.getType());
    }

    /**
     * Handles the EntityPickupItemEvent.
     *
     * @param event The EntityPickupItemEvent to handle.
     */
    @EventHandler
    public void onEvent(EntityPickupItemEvent event) {
        if (event.getEntity() instanceof Player player) {
            ItemStack item = event.getItem().getItemStack();
            BorderHoardersPlugin.getBorderHandler().addCompletedItem(player, item.getType());
        }
    }

}