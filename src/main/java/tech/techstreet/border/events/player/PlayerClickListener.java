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

import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import tech.techstreet.border.BorderHoarderPlugin;
import tech.techstreet.border.gui.Button;
import tech.techstreet.border.gui.MenuInstance;
import tech.techstreet.border.gui.impl.MissingItemsMenu;
import tech.techstreet.border.lib.user.User;
import tech.techstreet.border.lib.user.UserManager;
import tech.techstreet.border.lib.user.UserState;

public class PlayerClickListener implements Listener {

    /**
     * Handles player interactions with the world.
     *
     * @param event The PlayerInteractEvent to handle
     */
    @EventHandler
    public void onEvent(PlayerInteractEvent event) {
        final Player player = event.getPlayer();
        final User user = UserManager.of(player);

        if (user.getState() == UserState.LOBBY) {
            event.setCancelled(true);

            if (event.getAction() == Action.RIGHT_CLICK_BLOCK) {
                if (event.getClickedBlock() != null && event.getClickedBlock().getType() == Material.WAXED_WEATHERED_COPPER_CHEST) {
                    player.playSound(player.getLocation(), Sound.BLOCK_COPPER_CHEST_WEATHERED_OPEN, 1, 1);
                    user.openMenu(new MissingItemsMenu(user, 1));
                }
            }
        }
    }

    /**
     * Handles inventory click events for menu interactions.
     *
     * @param event The InventoryClickEvent to handle
     */
    @EventHandler
    public void onEvent(InventoryClickEvent event) {
        final Player player = (Player) event.getWhoClicked();
        final User user = UserManager.of(player);
        final MenuInstance instance = user.getMenuInstance();

        if (instance != null && instance.buttons() != null) {
            instance.menu().onChange(user, event);

            if (!(event.getClickedInventory() instanceof PlayerInventory)) {
                try {
                    final Button button = instance.buttons().get(event.getSlot());
                    button.onClick(event);

                    if (button.getAutoCancel()) {
                        event.setCancelled(true);
                        return;
                    }
                } catch (Exception e) {
                    event.setCancelled(true);
                    return;
                }
            }
        }

        for (ItemStack item : player.getInventory().getContents()) {
            if (item != null) {
                BorderHoarderPlugin.getBorderHandler().addCompletedItem(player, item.getType());
            }
        }
    }

    /**
     * Handles inventory close events for menu interactions.
     *
     * @param event The InventoryCloseEvent to handle
     */
    @EventHandler
    public void onEvent(InventoryCloseEvent event) {
        final Player player = (Player) event.getPlayer();
        if (event.getReason() == InventoryCloseEvent.Reason.DISCONNECT) return;
        if (!player.isOnline()) return;
        final User user;

        try {
            user = UserManager.of(player);
        } catch (final Exception ignored) { // Error handle if the player assigned to the inventory has gone offline
            return;
        }

        if (event.getReason() != InventoryCloseEvent.Reason.OPEN_NEW) {
            if (user.getMenuInstance() != null) {
                final MenuInstance instance = user.getMenuInstance();
                instance.menu().onClose(user, event);
                user.setMenuInstance(null);
            }
        }
    }
}