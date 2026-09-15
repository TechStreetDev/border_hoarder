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

package tech.techstreet.border.command.impl;

import cloud.commandframework.Command;
import cloud.commandframework.paper.PaperCommandManager;
import org.bukkit.Material;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import tech.techstreet.border.BorderHoarderPlugin;
import tech.techstreet.border.command.SystemCommand;

public class DebugCommand extends SystemCommand {

    /**
     * Creates a command for the given command manager.
     *
     * @param manager The command manager to create the command for.
     * @return The created command.
     */
    @Override
    public Command<CommandSender> createCommand(final PaperCommandManager<CommandSender> manager) {
        return manager.commandBuilder("debug")
                .literal("unlock")
                .literal("all")
                .handler(commandContext -> {
                    final Player player = (Player) commandContext.getSender();
                    for (final Material item : Material.values()) {
                        BorderHoarderPlugin.getBorderHandler().addCompletedItem(player, item);
                    }

                    player.sendMessage("Done.");
                }).build();
    }
}
