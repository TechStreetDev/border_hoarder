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

package tech.techstreet.border.command.impl;

import cloud.commandframework.Command;
import cloud.commandframework.arguments.standard.StringArgument;
import cloud.commandframework.paper.PaperCommandManager;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import tech.techstreet.border.command.SystemCommand;
import tech.techstreet.border.gui.impl.MissingItemsMenu;
import tech.techstreet.border.lib.user.User;
import tech.techstreet.border.lib.user.UserManager;

public class SearchCommand extends SystemCommand {

    /**
     * Creates a command for the given command manager.
     *
     * @param manager The command manager to create the command for.
     * @return The created command.
     */
    @Override
    public Command<CommandSender> createCommand(final PaperCommandManager<CommandSender> manager) {
        return manager.commandBuilder("search")
                .argument(StringArgument.optional("query"))
                .handler(commandContext -> {
                    final Player player = (Player) commandContext.getSender();
                    final User user = UserManager.of(player);
                    final String query = commandContext.get("query");

                    user.openMenu(new MissingItemsMenu(user, 1, query));
                }).build();
    }
}
