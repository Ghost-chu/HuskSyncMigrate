package com.ghostchu.husksync.husksyncmigrate;

import lombok.Getter;
import lombok.SneakyThrows;
import net.md_5.bungee.api.plugin.Plugin;
import net.md_5.bungee.config.Configuration;
import net.md_5.bungee.config.ConfigurationProvider;
import net.md_5.bungee.config.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.sql.*;
import java.util.Properties;

@Getter
public final class HuskSyncMigrate extends Plugin {
    private Connection sqliteConnection;
    private Connection mysqlConnection;
    // these are main cluster default table name, change it if needs
    private HuskSyncSettings huskSyncSettings;

    @Override
    @SneakyThrows
    public void onEnable() {
        // Plugin startup logic
        if (!getDataFolder().exists())
            getDataFolder().mkdir();

        // Loading the SQLite database
        File huskSyncDatabase = new File(new File(getDataFolder().getParentFile(), "HuskSync"), "HuskSyncData.db");
        try {
            Class.forName("org.sqlite.JDBC");
            this.sqliteConnection = DriverManager.getConnection("jdbc:sqlite:" + huskSyncDatabase);
        } catch (SQLException e) {
            e.printStackTrace();
        }

        // Loading HuskSync settings
        this.huskSyncSettings = new HuskSyncSettings(this);

        Properties info = new Properties();
        HuskSyncSettings.Database database = huskSyncSettings.getDatabase();
        getLogger().info("MySQL infomation: "+database);
        info.setProperty("autoReconnect", "true");
        info.setProperty("user", database.getUsername());
        info.setProperty("password", database.getPassword());
        info.setProperty("useUnicode", "true");
        info.setProperty("characterEncoding", "utf8");
        try {
            this.mysqlConnection = DriverManager.getConnection("jdbc:mysql://" + database.getHost() + ":" + database.getPort() + "/" + database.getDatabase(), info);
        } catch (SQLException e) {
            e.printStackTrace();
            getLogger().warning("FAILED: MySQL server failed to connect.");
            return;
        }
        getLogger().info("Loading successfully! Type /husksyncmigrate when you get backup ready :)");
        getProxy().getPluginManager().registerCommand(this, new CommandMigrate(this));
    }

    @Override
    public void onDisable() {
        // Plugin shutdown logic
    }


    /**
     * Returns true if the table exists - QuickShop code
     *
     * @param table The table to check for
     * @return True if the table is found
     * @throws SQLException Throw exception when failed execute somethins on SQL
     */
    public boolean hasTable(String table, Connection connection) throws SQLException {
        boolean match = false;
        try (ResultSet rs = connection.getMetaData().getTables(null, null, "%", null)) {
            while (rs.next()) {
                if (table.equalsIgnoreCase(rs.getString("TABLE_NAME"))) {
                    match = true;
                    break;
                }
            }
        }
        return match;
    }

    public ResultSet selectTable(String table, Connection databaseConnection) throws SQLException {
        Statement st = databaseConnection.createStatement();
        String sql = "SELECT * FROM " + table;
        ResultSet resultSet = st.executeQuery(sql);
        //Resource closes will complete in this class
        return resultSet;
    }
}
