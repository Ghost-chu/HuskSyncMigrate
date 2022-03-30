# HuskSyncMigrate
HuskSync mysql migrate helper.

---

## Usage

Step 1. Shutdown proxy, Setup mysql credentials in HuskSync configuration (You will need set datasource to MySQL in this step to make sure HuskSync to create the tables, otherwise migrate will fail).

Step 2. Enable firewall to block any player join request (you won't want some player data got overwrite or table have new data write), if you don't, HuskSync may write new data into tables and you will got an error while you trying to migrate.

Step 3. Type "/husksyncmigrate" in proxy console.

Step 4. Once you see "All tasks finished", you can delete this plugin and restart the proxy to enjoy the life (but do not delete the SQLite file, this plugin still in BETA and may not stable, backup always good :))

