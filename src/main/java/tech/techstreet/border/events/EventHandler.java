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

package tech.techstreet.border.events;

import org.bukkit.Bukkit;
import org.bukkit.event.Listener;
import tech.techstreet.border.BorderHoardersPlugin;
import tech.techstreet.border.events.player.PlayerClickListener;
import tech.techstreet.border.events.player.PlayerConnectionEvent;
import tech.techstreet.border.events.player.PlayerDamageEvent;
import tech.techstreet.border.events.player.PlayerHandleItemEvent;
import tech.techstreet.border.events.world.WorldBlockFallEvent;
import tech.techstreet.border.events.world.WorldPortalEvent;

public class EventHandler {
    private final BorderHoardersPlugin instance;

    /**
     * Constructor for EventHandler.
     *
     * @param instance The main plugin instance.
     */
    public EventHandler(BorderHoardersPlugin instance) {
        this.instance = instance;
    }

    /**
     * Register a single listener.
     *
     * @param listener The listener to register.
     */
    private void register(final Listener listener) {
        Bukkit.getServer().getPluginManager().registerEvents(listener, instance);
    }

    /**
     * Register multiple listeners.
     *
     * @param listeners The listeners to register.
     */
    private void register(final Listener... listeners) {
        for (final Listener listener : listeners) {
            this.register(listener);
        }
    }

    /**
     * Load and register all event listeners.
     */
    public void load() {
        register(
                new PlayerClickListener(),
                new PlayerConnectionEvent(),
                new PlayerHandleItemEvent(),
                new PlayerDamageEvent(),

                new WorldBlockFallEvent(),
                new WorldPortalEvent()
        );
    }
}