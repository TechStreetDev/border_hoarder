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

package tech.techstreet.border.lib.user;

import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.HashMap;

public class UserManager {
    private static final HashMap<Player, User> users = new HashMap<>();

    /**
     * Get the user data for a player, creating it if it doesn't exist.
     *
     * @param player The player to get the user data for.
     * @return The User object for the player.
     */
    public static User of(@NotNull final Player player) {
        if (!users.containsKey(player)) {
            final User user = new User(player);
            users.put(player, user);
            return user;
        }

        return users.get(player);
    }

    /**
     * Update the user data for a player.
     *
     * @param player The player to update.
     */
    public static void update(@NotNull final Player player) {
        final User user = new User(player);
        users.put(player, user);
    }

    /**
     * Logout a player and remove their user data.
     *
     * @param player The player to logout.
     */
    public static void logout(final Player player) {
        users.remove(player);
    }
}
