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

package tech.techstreet.border.gui.item;

import net.kyori.adventure.text.Component;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;

public final class BaseItems {

    public static ItemStack fillerItem(final Material material) {
        final ItemStack filler = new ItemStack(material);
        filler.editMeta(meta -> {
            meta.displayName(Component.empty());
            meta.setHideTooltip(true);
        });

        return filler;
    }

    public static ItemStack invisibleFiller() {
        return fillerItem(Material.LIGHT_GRAY_STAINED_GLASS_PANE);
    }

    public static ItemStack previousPage() {
        ItemStack itemStack = new ItemStack(Material.ARROW);
        itemStack.editMeta(meta -> {
            meta.displayName(Component.text("Previous Page", Colours.RED));
        });

        return itemStack;
    }

    public static ItemStack nextPage() {
        ItemStack itemStack = new ItemStack(Material.ARROW);
        itemStack.editMeta(meta -> {
            meta.displayName(Component.text("Next Page", Colours.GREEN_LIGHT_2));
        });

        return itemStack;
    }
}
