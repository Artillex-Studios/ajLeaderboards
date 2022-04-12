package us.ajg0702.leaderboards.cache.methods;

import us.ajg0702.leaderboards.Debug;
import us.ajg0702.leaderboards.LeaderboardPlugin;
import us.ajg0702.leaderboards.boards.TimedType;
import us.ajg0702.leaderboards.cache.Cache;
import us.ajg0702.leaderboards.cache.CacheMethod;
import us.ajg0702.utils.common.ConfigFile;

import java.io.File;
import java.sql.*;
import java.util.List;
import java.util.Locale;

public class H2Method implements CacheMethod {
    private Connection conn;
    private LeaderboardPlugin plugin;
    private ConfigFile config;
    private Cache cacheInstance;
    @Override
    public Connection getConnection() {
        try {
            if(conn.isClosed()) {
                plugin.getLogger().warning("H2 connection is dead, making a new one");
                init(plugin, config, cacheInstance);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return conn;
    }

    @Override
    public void init(LeaderboardPlugin plugin, ConfigFile config, Cache cacheInstance) {
        this.plugin = plugin;
        this.config = config;
        this.cacheInstance = cacheInstance;
        try {
            Class.forName("us.ajg0702.leaderboards.libs.h2.Driver");
        } catch (ClassNotFoundException e1) {
            e1.printStackTrace();
        }
        String url = "jdbc:h2:"+plugin.getDataFolder().getAbsolutePath()+File.separator+"cache;DATABASE_TO_UPPER=false";
        try {
            conn = DriverManager.getConnection(url);
        } catch (SQLException e) {
            plugin.getLogger().severe("Unnable to create cache file! The plugin will not work correctly!");
            e.printStackTrace();
            return;
        }
        List<String> tables = cacheInstance.getDbTableList();

        try(Statement statement = conn.createStatement()) {
            //ResultSet rs = conn.getMetaData().getTables(null, null, "", null);
            for(String tableName : tables) {
                int version;
                if(!tableName.startsWith(cacheInstance.getTablePrefix())) continue;
                try {
                    ResultSet rs = conn.createStatement().executeQuery("SELECT TABLE_NAME,COLUMN_NAME,REMARKS\n" +
                            " FROM INFORMATION_SCHEMA.COLUMNS where TABLE_NAME='"+tableName+"'");
                    rs.next();
                    version = Integer.parseInt(rs.getString("REMARKS"));
                    rs.close();
                } catch(NumberFormatException e) {
                    version = 0;
                } catch(SQLException e) {
                    if(e.getMessage().contains("Column 'COMMENT' not found")) {
                        version = 0;
                    } else {
                        throw e;
                    }
                }
                Debug.info("Table version for "+tableName+" is: "+version);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void close(Connection connection) {}

    @Override
    public int getMaxConnections() {
        return 1;
    }

    @Override
    public void shutdown() {
        try {
            conn.close();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    @Override
    public String formatStatement(String s) {
        return s.replaceAll("'", "`");
    }

    @Override
    public String getName() {
        return "h2";
    }

    @Override
    public boolean requiresClose() {
        return false;
    }

    public void newConnection() {
        shutdown();
        init(plugin, config, cacheInstance);
    }
}
