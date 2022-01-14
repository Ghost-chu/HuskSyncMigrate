package com.ghostchu.husksync.husksyncmigrate;

import lombok.AllArgsConstructor;
import lombok.Data;
import net.md_5.bungee.config.Configuration;
import net.md_5.bungee.config.ConfigurationProvider;
import net.md_5.bungee.config.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class HuskSyncSettings {
    private HuskSyncMigrate plugin;
    private Configuration configuration;
    public HuskSyncSettings(HuskSyncMigrate plugin) throws IOException {
        this.plugin = plugin;
        File huskSyncConfig = new File(new File(plugin.getDataFolder().getParentFile(), "HuskSync"), "config.yml");
        if(!huskSyncConfig.exists())
            throw new IOException("HuskSync configuration not found at:"+huskSyncConfig);
        this.configuration = ConfigurationProvider.getProvider(YamlConfiguration.class).load(huskSyncConfig);
        plugin.getLogger().info("HuskSync configuration open successfully.");
    }

    public List<Cluster> getClusters(){
        if(configuration.getSection("clusters") == null){
            plugin.getLogger().warning("Failed to read clusters from HuskSync configuration :(");
            return Collections.emptyList();
        }
        List<Cluster> clustersList = new ArrayList<>();
        for (String clusters : configuration.getSection("clusters").getKeys()) {
            Configuration section = configuration.getSection("clusters").getSection(clusters);
            clustersList.add(new Cluster(clusters, section.getString("data_table"),section.getString("player_table")));
        }
        return clustersList;
    }

    public Database getDatabase(){
        Configuration section = configuration.getSection("data_storage_settings").getSection("mysql_settings");
        return new Database(section.getString("host"),
                section.getInt("port"),
                section.getString("database"),
                section.getString("username"),
                section.getString("password"));

    }
@AllArgsConstructor
@Data
    static class Database{
        private String host;
        private int port;
        private String database;
        private String username;
        private String password;
    }

    @AllArgsConstructor
    @Data
    static class Cluster{
        private String name;
        private String dataTable;
        private String playerTable;
    }
}
