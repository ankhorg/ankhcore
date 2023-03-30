package org.inksnow.ankh.core.common.config;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.val;
import org.bukkit.configuration.ConfigurationSection;
import org.inksnow.ankh.core.api.AnkhCoreLoader;

import javax.inject.Inject;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.StringJoiner;

public class AnkhConfig {
  @Getter
  private final int tickRate;
  @Getter
  private final DatabaseConfig database;
  @Getter
  private final PlayerShellConfig playerShell;
  @Getter
  private final ServiceConfig service;

  @Inject
  private AnkhConfig(AnkhCoreLoader loaderPlugin) {
    loaderPlugin.saveDefaultConfig();
    val configuration = loaderPlugin.getConfig();

    this.tickRate = loadTickRate(configuration);
    this.database = new DatabaseConfig(required(configuration.getConfigurationSection("database"), "database"));
    this.playerShell = new PlayerShellConfig(required(configuration.getConfigurationSection("player-shell"), "player-shell"));
    this.service = new ServiceConfig(required(configuration.getConfigurationSection("service"), "service"));
  }

  private static <R> R required(R value, String path) {
    if (value == null) {
      throw new IllegalStateException("required config key '" + path + "' not found");
    } else {
      return value;
    }
  }

  private int loadTickRate(ConfigurationSection configuration) {
    return configuration.getInt("tick-rate", 1);
  }

  public static class DatabaseConfig {
    @Getter
    private final DriverType driver;
    @Getter
    private final String url;
    @Getter
    private final String username;
    @Getter
    private final String password;

    private DatabaseConfig(ConfigurationSection configuration) {
      this.driver = loadDriver(configuration);
      this.url = required(configuration.getString("url"), "database.url");
      this.username = required(configuration.getString("username"), "database.username");
      this.password = required(configuration.getString("password"), "database.password");
    }

    private DriverType loadDriver(ConfigurationSection configuration) {
      val typeString = required(configuration.getString("driver"), "database.driver");
      try {
        return DriverType.valueOf(typeString);
      } catch (IllegalArgumentException e) {
        val joiner = new StringJoiner(", ");
        for (DriverType value : DriverType.values()) {
          joiner.add(value.name());
        }
        throw new IllegalStateException("config key 'database.driver' value is not supported. support values: " + joiner);
      }
    }

    @AllArgsConstructor
    public enum DriverType {
      H2("org.h2.Driver", "org.hibernate.dialect.H2Dialect"),
      MARIADB("org.mariadb.jdbc.Driver", "org.hibernate.dialect.MariaDBDialect"),
      POSTGRESQL("org.postgresql.Driver", "org.hibernate.dialect.PostgreSQLDialect");

      @Getter
      private final String driverClass;
      @Getter
      private final String dialectClass;
    }
  }

  public static class PlayerShellConfig {
    @Getter
    private final boolean enable;
    @Getter
    private final String prefix;

    public PlayerShellConfig(ConfigurationSection configuration) {
      this.enable = configuration.getBoolean("enable");
      this.prefix = required(configuration.getString("prefix"), "player-shell.prefix");
    }
  }

  public static class ServiceConfig {
    private final Map<String, String> map;
    @Getter
    private final String hologram;
    @Getter
    private final String script;
    @Getter
    private final String worldStorage;

    public ServiceConfig(ConfigurationSection configuration) {
      val keySet = configuration.getKeys(false);
      val map = new HashMap<String, String>(keySet.size());
      for (String key : keySet) {
        val value = configuration.getString(key);
        map.put(key, (value == null || value.isEmpty()) ? null : value);
      }
      this.map = Collections.unmodifiableMap(map);
      this.hologram = get("hologram");
      this.script = get("script");
      this.worldStorage = get("world-storage");
    }

    public Map<String, String> get() {
      return this.map;
    }

    public String get(String name) {
      return map.get(name);
    }
  }
}