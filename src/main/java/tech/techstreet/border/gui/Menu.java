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

package tech.techstreet.border.gui;

import net.kyori.adventure.text.Component;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.jetbrains.annotations.NotNull;
import tech.techstreet.border.lib.user.User;

import java.util.List;

/**
 * Base class for all menus.
 */
@SuppressWarnings("unused")
public abstract class Menu {
    private final @NotNull User user;

    /**
     * Create a new menu for a user.
     *
     * @param user The owner of the menu.
     */
    public Menu(final @NotNull User user) {
        this.user = user;
    }

    /**
     * Get the number of slots in the menu.
     *
     * @return The number of slots in the menu.
     */
    public abstract Integer getSlots();

    /**
     * Get the buttons to be displayed in the menu.
     *
     * @param user The user viewing the menu.
     * @return The list of buttons to be displayed.
     */
    public abstract List<Button> getItems(User user);

    /**
     * Get the title of the menu.
     *
     * @param user The user viewing the menu.
     * @return The title of the menu.
     */
    public abstract Component getTitle(User user);

    /**
     * Whether clicks inside the menu should be cancelled.
     *
     * @return true if clicks should be cancelled, false otherwise.
     */
    public abstract Boolean getCancel();

    /**
     * Called when the menu is changed by the user.
     *
     * @param user  The user changing the menu.
     * @param event The inventory click event.
     */
    public void onChange(final User user, final InventoryClickEvent event) {
    }

    /**
     * Called when the menu is closed for the user.
     *
     * @param user  The user closing the menu.
     * @param event The inventory close event.
     */
    public void onClose(final User user, final InventoryCloseEvent event) {
    }

    /**
     * Called when the menu is opened for the user.
     *
     * @param user The user opening the menu.
     */
    public void onOpen(final User user) {
    }

    /**
     * Opens the menu for the owner and pushes all buttons inside.
     */
    @Deprecated
    public void open() {
        user.openMenu(this);
    }

    /**
     * Recalculates the buttons inside the menu and pushes it to the user,
     * you must call User#openMenu before updating the menu items.
     */
    public void update() {
        if (user.getMenuInstance() == null) {
            throw new RuntimeException("The user currently does not have a menu open, call User#openMenu first.");
        }

        if (!(user.getMenuInstance().menu().equals(this))) {
            throw new RuntimeException("The user currently does not have this menu open, call User#openMenu first.");
        }

        user.setMenuInstance(new MenuInstance(this, getItems(user)));

        int index = 0;
        for (final Button button : getItems(user)) {
            try {
                user.asPlayer().getOpenInventory().setItem(index, button.getItemStack());
                index += 1;
            } catch (final IllegalStateException e) {
                user.asPlayer().getInventory().remove(button.getItemStack());
            }
        }
    }

    /**
     * Returns the owner of the menu in a User interface.
     *
     * @return user interface for the owner.
     */
    public @NotNull User getUser() {
        return user;
    }

}