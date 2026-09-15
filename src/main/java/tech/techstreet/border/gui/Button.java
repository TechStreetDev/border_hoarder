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

import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;

import java.util.Objects;
import java.util.function.Consumer;

@SuppressWarnings("unused")
public class Button {
    private static int counter;
    private final int ID = counter++;

    private final ItemStack itemStack;
    private Boolean autoCancel;
    private Consumer<InventoryClickEvent> action;

    /**
     * Create a new button.
     *
     * @param itemStack The ItemStack representing the button.
     */
    public Button(final ItemStack itemStack) {
        this(itemStack, event -> {
        });
    }

    /**
     * Create a new button.
     *
     * @param itemStack The ItemStack representing the button.
     * @param action    The action to perform when the button is clicked.
     */
    public Button(final ItemStack itemStack, final Consumer<InventoryClickEvent> action) {
        this.itemStack = itemStack;
        this.action = action;
        this.autoCancel = false;
    }

    /**
     * Create a new button.
     *
     * @param itemStack  The ItemStack representing the button.
     * @param action     The action to perform when the button is clicked.
     * @param autoCancel Whether clicks on this button should be automatically cancelled.
     */
    public Button(final ItemStack itemStack, final Consumer<InventoryClickEvent> action, final Boolean autoCancel) {
        this.itemStack = itemStack;
        this.action = action;
        this.autoCancel = autoCancel;
    }

    /**
     * Get the ItemStack representing this button.
     *
     * @return The ItemStack.
     */
    public ItemStack getItemStack() {
        return itemStack;
    }

    /**
     * Get whether clicks on this button should be automatically cancelled.
     *
     * @return True if auto-cancel is enabled, false otherwise.
     */
    public Boolean getAutoCancel() {
        return autoCancel;
    }

    /**
     * Set whether clicks on this button should be automatically cancelled.
     *
     * @param autoCancel True to auto-cancel, false otherwise.
     */
    public void setAutoCancel(final Boolean autoCancel) {
        this.autoCancel = autoCancel;
    }

    /**
     * Set the action to be performed when the button is clicked.
     *
     * @param action The action to perform.
     */
    public void setAction(final Consumer<InventoryClickEvent> action) {
        this.action = action;
    }

    /**
     * Called when the button is clicked.
     *
     * @param event The inventory click event.
     */
    public void onClick(final InventoryClickEvent event) {
        action.accept(event);
    }

    /**
     * Equals based on unique ID.
     *
     * @param o The object to compare.
     * @return True if equal, false otherwise.
     */
    @Override
    public boolean equals(final Object o) {
        // We do not want equals collisions. The default hashcode would not fulfil this contract.

        if (this == o) {
            return true;
        }

        if (!(o instanceof final Button button)) {
            return false;
        }

        return ID == button.ID;
    }

    /**
     * Hashcode based on unique ID.
     *
     * @return The hashcode.
     */
    @Override
    public int hashCode() {
        return Objects.hash(ID);
    }

}