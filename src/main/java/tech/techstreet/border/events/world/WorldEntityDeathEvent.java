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

package tech.techstreet.border.events.world;

import org.bukkit.Material;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.inventory.ItemStack;

public class WorldEntityDeathEvent implements Listener {

    /**
     * Handles the EntityDeathEvent.
     *
     * @param event The EntityDeathEvent to handle.
     */
    @EventHandler
    public void onEvent(EntityDeathEvent event) {
        Entity entity = event.getEntity();
        if (entity.getType() == EntityType.ELDER_GUARDIAN) {
            event.getDrops().add(new ItemStack(Material.TIDE_ARMOR_TRIM_SMITHING_TEMPLATE));
        }
    }
}