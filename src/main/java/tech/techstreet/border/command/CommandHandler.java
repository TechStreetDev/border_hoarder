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

package tech.techstreet.border.command;

import cloud.commandframework.CommandTree;
import cloud.commandframework.execution.CommandExecutionCoordinator;
import cloud.commandframework.minecraft.extras.MinecraftExceptionHandler;
import cloud.commandframework.paper.PaperCommandManager;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.Style;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import tech.techstreet.border.BorderHoarderPlugin;
import tech.techstreet.border.command.impl.LobbyCommand;
import tech.techstreet.border.command.impl.SearchCommand;
import tech.techstreet.border.command.impl.UpdateCommand;

import java.util.function.Function;

public class CommandHandler {
    public static PaperCommandManager<CommandSender> manager;

    /**
     * Constructor for CommandHandler.
     *
     * @param instance The main plugin instance.
     */
    public CommandHandler(BorderHoarderPlugin instance) {
        final Function<CommandTree<CommandSender>, CommandExecutionCoordinator<CommandSender>> executionCoordinatorFunction =
                CommandExecutionCoordinator.simpleCoordinator();

        try {
            manager = new PaperCommandManager<>(
                    instance,
                    executionCoordinatorFunction,
                    Function.identity(),
                    Function.identity()
            );
        } catch (final Exception e) {
            Bukkit.getPluginManager().disablePlugin(instance);
            return;
        }

        new MinecraftExceptionHandler<CommandSender>()
                .withInvalidSyntaxHandler()
                .withInvalidSenderHandler()
                .withArgumentParsingHandler()
                .withNoPermissionHandler()
                .withHandler(MinecraftExceptionHandler.ExceptionType.COMMAND_EXECUTION,
                        (commandSender, e) -> Component.text()
                                .content("An internal error occurred while attempting to perform this command.")
                                .color(NamedTextColor.RED)
                                .build())

                .withHandler(MinecraftExceptionHandler.ExceptionType.INVALID_SYNTAX,
                        (commandSender, e) -> Component.text()
                                .content("/" + e.getMessage().replace("Invalid command syntax. Correct syntax is: ", ""))
                                .style(Style.style(TextDecoration.ITALIC).color(NamedTextColor.GRAY))
                                .build())

                .apply(manager, commandSender -> commandSender);
    }

    /**
     * Register a single command.
     *
     * @param cmd The command to register.
     */
    public static void register(final SystemCommand cmd) {
        manager.command(cmd.createCommand(manager));
    }

    /**
     * Register multiple commands.
     *
     * @param cmds The commands to register.
     */
    public static void register(final SystemCommand... cmds) {
        for (final SystemCommand cmd : cmds) {
            register(cmd);
        }
    }

    /**
     * Load and register all commands.
     */
    public void load() {
        register(
                new LobbyCommand(),
                new SearchCommand(),
                new UpdateCommand()
        );
    }
}