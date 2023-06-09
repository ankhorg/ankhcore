package org.inksnow.ankh.core.item.debug;

import lombok.val;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.inksnow.ankh.core.api.AnkhCore;
import org.inksnow.ankh.core.common.AdventureAudiences;
import org.inksnow.ankh.core.inventory.storage.ReadonlyChestMenu;

import javax.inject.Inject;
import javax.inject.Singleton;

@Singleton
public class DebugToolsMenu {
  private static final String MENU_TITLE = AdventureAudiences.serialize(Component.text()
      .append(AnkhCore.PLUGIN_NAME_COMPONENT)
      .append(Component.text("debug tools", NamedTextColor.RED))
      .build());

  private final ItemStack[] defaultItems;

  @Inject
  private DebugToolsMenu(DebugRemoveItem debugRemoveItem, DebugChunkFixItem debugChunkFixItem) {
    defaultItems = new ItemStack[54];
    defaultItems[0] = debugRemoveItem.createItem();
    defaultItems[1] = debugChunkFixItem.createItem();
  }

  public void openForPlayer(Player player) {
    ReadonlyChestMenu.builder()
        .createInventory(holder -> {
          val inventory = Bukkit.createInventory(holder, 54, MENU_TITLE);
          for (int i = 0; i < defaultItems.length; i++) {
            val defaultItem = defaultItems[i];
            if (defaultItem != null) {
              inventory.setItem(i, defaultItems[i].clone());
            }
          }
          return inventory;
        })
        .build()
        .openForPlayer(player);
  }

  public void openForPlayer(Player... players) {
    for (Player player : players) {
      openForPlayer(player);
    }
  }
}
