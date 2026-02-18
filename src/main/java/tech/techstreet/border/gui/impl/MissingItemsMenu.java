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

package tech.techstreet.border.gui.impl;

import net.kyori.adventure.text.Component;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.inventory.ItemRarity;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;
import tech.techstreet.border.BorderHoarderPlugin;
import tech.techstreet.border.gui.Button;
import tech.techstreet.border.gui.Menu;
import tech.techstreet.border.gui.item.BaseItems;
import tech.techstreet.border.gui.item.Colours;
import tech.techstreet.border.lib.item.BoarderItem;
import tech.techstreet.border.lib.user.User;

import java.util.ArrayList;
import java.util.List;

public class MissingItemsMenu extends Menu {
    private static final int VALUES_PER_PAGE = 45;
    private final List<BoarderItem> missingItems;
    private final boolean isSearch;
    private int page;

    public MissingItemsMenu(User user, int page) {
        super(user);
        this.page = page;
        this.isSearch = false;
        this.missingItems = BorderHoarderPlugin.getBorderHandler().getMissingItems();
    }

    public MissingItemsMenu(User user, int page, String query) {
        super(user);
        this.page = page;
        this.isSearch = true;
        this.missingItems = BorderHoarderPlugin.getBorderHandler().getMissingItems()
                .stream().filter(item -> item.name().toLowerCase().replaceAll("_", "").contains(query)).toList();
    }

    @Override
    public Integer getSlots() {
        return 54;
    }

    @Override
    public List<Button> getItems(final User user) {
        List<Button> buttons = new ArrayList<>();
        for (int i = 0; i < getSlots(); i++)
            buttons.add(new Button(BaseItems.invisibleFiller(), e -> e.setCancelled(true)));

        int index = 0;
        int startingIndex = (page * VALUES_PER_PAGE) - VALUES_PER_PAGE;

        for (int i = startingIndex; i < (startingIndex + VALUES_PER_PAGE); i++) {
            if (i >= missingItems.size()) {
                break;
            }

            BoarderItem result = missingItems.get(i);
            Material material = Material.valueOf(result.name());
            ItemStack icon = getItemStack(material, result);

            buttons.set(index, new Button(icon, e -> e.setCancelled(true)));

            index += 1;
        }

        if (page >= 2) {
            buttons.set(45, new Button(BaseItems.previousPage(), e -> {
                e.setCancelled(true);
                page = (page - 1);

                user.asPlayer().playSound(user.asPlayer().getLocation(), Sound.BLOCK_NOTE_BLOCK_HARP, 1, 1);
                if (user.getMenuInstance() != null) user.getMenuInstance().menu().update();
            }));
        }

        if ((startingIndex + VALUES_PER_PAGE) < missingItems.size()) {
            buttons.set(53, new Button(BaseItems.nextPage(), e -> {
                e.setCancelled(true);
                page = (page + 1);

                user.asPlayer().playSound(user.asPlayer().getLocation(), Sound.BLOCK_NOTE_BLOCK_HARP, 1, 1);
                if (user.getMenuInstance() != null) user.getMenuInstance().menu().update();
            }));
        }

        return buttons;
    }

    @Override
    public Component getTitle(final User user) {
        return Component.text("Missing Items (" + missingItems.size() + ")");
    }

    @Override
    public void onClose(User user, InventoryCloseEvent event) {
        if (!isSearch) {
            user.asPlayer().playSound(user.asPlayer().getLocation(), Sound.BLOCK_COPPER_CHEST_WEATHERED_CLOSE, 1, 1);
        }
    }

    @Override
    public Boolean getCancel() {
        return true;
    }

    @NotNull
    private static ItemStack getItemStack(Material material, BoarderItem result) {
        ItemStack icon;

        try {
            icon = new ItemStack(material);
            icon.editMeta(meta -> {
                meta.setRarity(ItemRarity.COMMON);
            });
        } catch (Exception e) {
            icon = new ItemStack(Material.BARRIER);
            icon.editMeta(meta -> {
                meta.displayName(Component.text("Unknown Item", Colours.RED));
                meta.lore(List.of(
                        Component.text("This item is unknown to the server.", Colours.GRAY),
                        Component.text("Please contact an administrator.", Colours.GRAY),
                        Component.text(""),
                        Component.text("Item " + result.name(), Colours.GRAY)
                ));
            });
        }
        return icon;
    }
}