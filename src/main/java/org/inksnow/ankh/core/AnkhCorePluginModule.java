package org.inksnow.ankh.core;

import com.google.inject.AbstractModule;
import lombok.extern.slf4j.Slf4j;
import org.inksnow.ankh.core.api.AnkhServiceLoader;
import org.inksnow.ankh.core.api.block.BlockRegistry;
import org.inksnow.ankh.core.api.ioc.AnkhIocKey;
import org.inksnow.ankh.core.api.plugin.annotations.PluginModule;
import org.inksnow.ankh.core.api.storage.ChunkStorage;
import org.inksnow.ankh.core.api.storage.LocationStorage;
import org.inksnow.ankh.core.block.BlockRegisterService;
import org.inksnow.ankh.core.common.AnkhServiceLoaderImpl;
import org.inksnow.ankh.core.common.entity.LocationEmbedded;
import org.inksnow.ankh.core.common.entity.WorldChunkEmbedded;
import org.inksnow.ankh.core.ioc.BridgerKey;

@PluginModule
@Slf4j
public class AnkhCorePluginModule extends AbstractModule {
  static {
    ScreenPrinter.print(logger);
  }

  @Override
  protected void configure() {
    bind(AnkhIocKey.Factory.class).to(BridgerKey.Factory.class);
    bind(AnkhServiceLoader.class).to(AnkhServiceLoaderImpl.class);

    bind(LocationStorage.Factory.class).to(LocationEmbedded.Factory.class);
    bind(ChunkStorage.Factory.class).to(WorldChunkEmbedded.Factory.class);

    bind(BlockRegistry.class).to(BlockRegisterService.class);
  }
}