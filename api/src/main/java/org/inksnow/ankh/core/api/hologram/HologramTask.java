package org.inksnow.ankh.core.api.hologram;

import org.bukkit.Location;
import org.bukkit.inventory.ItemStack;
import org.inksnow.ankh.core.api.util.IBuilder;

import javax.annotation.Nonnull;

public interface HologramTask {

  static @Nonnull Builder builder() {
    return HologramService.instance().builder();
  }

  void updateContent(@Nonnull HologramContent content);

  void delete();

  interface Builder extends IBuilder<Builder, HologramTask> {
    @Nonnull
    InnerContentBuilder content();

    @Nonnull
    Builder content(@Nonnull HologramContent content);

    @Nonnull
    Builder location(@Nonnull Location location);
  }

  interface InnerContentBuilder extends IBuilder<InnerContentBuilder, Builder> {
    @Nonnull
    InnerContentBuilder appendContent(@Nonnull String content);

    @Nonnull
    InnerContentBuilder appendItem(@Nonnull ItemStack item);
  }
}
