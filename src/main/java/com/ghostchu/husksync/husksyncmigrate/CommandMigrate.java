package com.ghostchu.husksync.husksyncmigrate;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.SneakyThrows;
import net.md_5.bungee.api.ChatColor;
import net.md_5.bungee.api.CommandSender;
import net.md_5.bungee.api.plugin.Command;

import java.sql.*;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public class CommandMigrate extends Command {
    private final HuskSyncMigrate plugin;

    public CommandMigrate(HuskSyncMigrate plugin) {
        super("husksyncmigrate", "husksyncmigrate");
        this.plugin = plugin;
    }

    public void validate(HuskSyncSettings.Cluster cluster) throws IllegalStateException, SQLException {
        if (!plugin.hasTable(cluster.getDataTable(), plugin.getMysqlConnection()) || !plugin.hasTable(cluster.getPlayerTable(), plugin.getMysqlConnection())) {
            throw new IllegalStateException("You must run at least once HuskSync under MySQL mode to create the tables we need");
        }

        if(plugin.selectTable(cluster.getPlayerTable(),plugin.getMysqlConnection()).next() || plugin.selectTable(cluster.getDataTable(),plugin.getMysqlConnection()).next()){
            throw new IllegalStateException("MySQL tables contains data, we require that tables must be empty, do not join the server after you switch to MySQL, please remove all records from "+cluster.getDataTable()+" and "+cluster.getPlayerTable()+" tables.");
        }

        if (!plugin.hasTable(cluster.getDataTable(), plugin.getSqliteConnection()) || !plugin.hasTable(cluster.getPlayerTable(), plugin.getSqliteConnection())) {
            throw new IllegalStateException("WHAT? SQLite database doesn't contains "+cluster.getPlayerTable()+" or "+cluster.getDataTable()+" tables? Check your SQLite database file!");
        }
    }

    @Override
    @SneakyThrows
    public void execute(CommandSender sender, String[] args) {
        // Reading the player table
        sender.sendMessage("Please stand by, We're checking the data before getting you ready...");

        List<HuskSyncSettings.Cluster> clusters = plugin.getHuskSyncSettings().getClusters();

        for (HuskSyncSettings.Cluster cluster : clusters) {
            try{
                validate(cluster);
            }catch (IllegalStateException exception){
                sender.sendMessage(ChatColor.RED+"Failed to validate cluster "+cluster.getName()+": "+exception.getMessage()+"; It is not safe to start the migrating.");
                return;
            }catch (SQLException exception){
                sender.sendMessage(ChatColor.RED+"SQL Error: "+exception.getMessage());
                exception.printStackTrace();
                return;
            }
        }

        for (HuskSyncSettings.Cluster cluster : clusters) {
            sender.sendMessage("Migrating cluster: "+cluster);
            try{
                migrate(cluster,sender);
            }catch (SQLException exception){
                sender.sendMessage("Failed to migrate cluster: "+cluster+", check the console!");
                exception.printStackTrace();
            }
        }


    }
    private void migrate(HuskSyncSettings.Cluster cluster, CommandSender sender) throws SQLException{
        int total = 0;
        int fails = 0;
        sender.sendMessage("Migrating player indexes...");
        ResultSet playerSet = plugin.selectTable(cluster.getPlayerTable(), plugin.getSqliteConnection());
        while (playerSet.next()) {
            total++;
            PlayerRecord record = new PlayerRecord(playerSet.getInt("id"), playerSet.getString("uuid"), playerSet.getString("username"));
            try {
                copyPlayerEntry(record, cluster.getPlayerTable(), plugin.getMysqlConnection());
            }catch (SQLException exception){
                sender.sendMessage("Migrating player index: "+record.getUsername()+" failed. Check the console! Skipping...");
                exception.printStackTrace();
                fails++;
            }
            sender.sendMessage(ChatColor.GRAY+"Migrating playa indexes, completed: "+total);
        }

        sender.sendMessage(ChatColor.GREEN+"Successfully migrated player index "+(total-fails)+" of "+total+", with "+fails+" fails.");
        total = fails = 0; // Reset counter

        sender.sendMessage("Migrating player data...");
        // Reading the data table
        ResultSet resultSet = plugin.selectTable(cluster.getDataTable(), plugin.getSqliteConnection());
        while (resultSet.next()) {
            total++;
             int player_id = resultSet.getInt("player_id");
             UUID dataVersionUUID = UUID.fromString(resultSet.getString("version_uuid"));
             Timestamp dataSaveTimestamp = resultSet.getTimestamp("timestamp");
             String serializedInventory = resultSet.getString("inventory");
             String serializedEnderChest = resultSet.getString("ender_chest");
             double health = resultSet.getDouble("health");
             double maxHealth = resultSet.getDouble("max_health");
             double healthScale = resultSet.getDouble("health_scale");
             int hunger = resultSet.getInt("hunger");
             float saturation = resultSet.getFloat("saturation");
             float saturationExhaustion = resultSet.getFloat("saturation_exhaustion");
             int selectedSlot = resultSet.getInt("selected_slot");
             String statusEffects = resultSet.getString("status_effects");
             int totalExperience = resultSet.getInt("total_experience");
             int expLevel = resultSet.getInt("exp_level");
             float expProgress = resultSet.getFloat("exp_progress");
             String gameMode = resultSet.getString("game_mode");
             boolean isFlying = resultSet.getBoolean("is_flying");
             String advancementData = resultSet.getString("advancements");
             String locationData = resultSet.getString("location");
             String statisticData = resultSet.getString("statistics");
             DataRecord record = new DataRecord(player_id, dataVersionUUID, dataSaveTimestamp, serializedInventory, serializedEnderChest,
                    health, maxHealth, healthScale, hunger, saturation, saturationExhaustion, selectedSlot, statusEffects,
                    totalExperience, expLevel, expProgress, gameMode, statisticData, isFlying, advancementData, locationData);
            copyDataEntry(record, plugin.getMysqlConnection(),cluster.getDataTable());
            sender.sendMessage(ChatColor.GRAY+"Migrating player sync data, completed: "+total);
        }
        sender.sendMessage(ChatColor.GREEN+"Successfully migrated player data "+(total-fails)+" of "+total+", with "+fails+" fails.");
        sender.sendMessage(ChatColor.GOLD+"All tasks has been finished, check if any errors appears. Change the storage source from SQLite to MySQL once you finished!");
    }

    private void copyPlayerEntry(PlayerRecord record, String table, Connection connection) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(
                "INSERT INTO " + table + " (`id`,`uuid`,`username`) VALUES(?,?,?);")) {
            statement.setInt(1, record.getId());
            statement.setString(2, record.getUuid());
            statement.setString(3, record.getUsername());
            statement.executeUpdate();
        }
    }

    private void copyDataEntry(DataRecord playerData, Connection connection, String table) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(
                "INSERT INTO " + table + " (`player_id`,`version_uuid`,`timestamp`,`inventory`,`ender_chest`,`health`,`max_health`,`health_scale`,`hunger`,`saturation`,`saturation_exhaustion`,`selected_slot`,`status_effects`,`total_experience`,`exp_level`,`exp_progress`,`game_mode`,`statistics`,`is_flying`,`advancements`,`location`) VALUES(?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?);")) {
            statement.setInt(1, playerData.getPlayer_id());
            statement.setString(2, playerData.getDataVersionUUID().toString());
            statement.setTimestamp(3, new Timestamp(Instant.now().getEpochSecond()));
            statement.setString(4, playerData.getInventory());
            statement.setString(5, playerData.getEnderChest());
            statement.setDouble(6, playerData.getHealth());
            statement.setDouble(7, playerData.getMaxHealth());
            statement.setDouble(8, playerData.getHealthScale());
            statement.setInt(9, playerData.getHunger());
            statement.setFloat(10, playerData.getSaturation());
            statement.setFloat(11, playerData.getSaturationExhaustion());
            statement.setInt(12, playerData.getSelectedSlot());
            statement.setString(13, playerData.getEffectData());
            statement.setInt(14, playerData.getTotalExperience());
            statement.setInt(15, playerData.getExpLevel());
            statement.setFloat(16, playerData.getExpProgress());
            statement.setString(17, playerData.getGameMode());
            statement.setString(18, playerData.getStatistics());
            statement.setBoolean(19, playerData.isFlying());
            statement.setString(20, playerData.getAdvancements());
            statement.setString(21, playerData.getLocation());
            statement.executeUpdate();
        }
    }

    @AllArgsConstructor
    @Builder
    @Data
    static class DataRecord {
        private int player_id;
        private UUID dataVersionUUID;
        private Timestamp timestamp;
        private String inventory;
        private String enderChest;
        private double health;
        private double maxHealth;
        private double healthScale;
        private int hunger;
        private float saturation;
        private float saturationExhaustion;
        private int selectedSlot;
        private String effectData;
        private int totalExperience;
        private int expLevel;
        private float expProgress;
        private String gameMode;
        private String statistics;
        private boolean isFlying;
        private String advancements;
        private String location;
    }

    @AllArgsConstructor
    @Builder
    @Data
    static class PlayerRecord {
        int id;
        String uuid;
        String username;
    }
}
