package org.inksnow.ankh.loader;

import org.inksnow.ankh.cloud.AnkhCloudLoader;
import org.inksnow.ankh.core.api.AnkhCoreLoader;
import org.inksnow.ankh.core.api.plugin.AnkhBukkitPlugin;
import org.inksnow.ankh.core.api.plugin.AnkhPluginContainer;
import org.inksnow.ankh.loader.internal.AnkhBukkitPluginInternal;

public class AnkhCoreLoaderPlugin extends AnkhBukkitPlugin implements AnkhCoreLoader {
  public static final AnkhPluginContainer container;

  static {
    AnkhCloudLoader.initial();
    AnkhLoggerLoader.initial(AnkhCoreLoaderPlugin.class.getClassLoader());
    AnkhBukkitPluginInternal.ensureLoaded();
    container = AnkhBukkitPlugin.initial(AnkhCoreLoaderPlugin.class);
  }

  @Override
  public AnkhPluginContainer getContainer() {
    return container;
  }

  @Override
  protected void acceptEnable() {
    AnkhCloudLoader.load(this);
  }
}
