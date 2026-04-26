package com.g4vrk.react.database;

import java.io.File;
import java.sql.*;
import java.util.*;

public class ViolationDatabase {
    private final SolarAI plugin;
    private Connection connection;
    private final File dbFile;

    public ViolationDatabase(SolarAI plugin) {
        this.plugin = plugin;
        this.dbFile = new File(plugin.getDataFolder(), "violations.db");
        initDatabase();
    }

    private void initDatabase() {
        try {
            if (!plugin.getDataFolder().exists()) {
                plugin.getDataFolder().mkdirs();
            }

            Class.forName("org.sqlite.JDBC");
            connection = DriverManager.getConnection("jdbc:sqlite:" + dbFile.getAbsolutePath());

            try (Statement stmt = connection.createStatement()) {
              
                stmt.execute(
                    "CREATE TABLE IF NOT EXISTS violations (" +
                    "id INTEGER PRIMARY KEY AUTOINCREMENT," +
                    "uuid TEXT NOT NULL," +
                    "player_name TEXT NOT NULL," +
                    "probability REAL NOT NULL," +
                    "timestamp BIGINT NOT NULL" +
                    ")"
                );
                
                stmt.execute("CREATE INDEX IF NOT EXISTS idx_uuid ON violations (uuid)");
                stmt.execute("CREATE INDEX IF NOT EXISTS idx_timestamp ON violations (timestamp)");
            }

        } catch (Exception e) {
            plugin.getLogger().severe("Ошибка инициализации базы данных: " + e.getMessage());
            e.printStackTrace();
        }
    }

    public void saveViolation(UUID uuid, String playerName, double probability) {
        try {
            try (PreparedStatement ps = connection.prepareStatement(
                "INSERT INTO violations (uuid, player_name, probability, timestamp) VALUES (?, ?, ?, ?)"
            )) {
                ps.setString(1, uuid.toString());
                ps.setString(2, playerName);
                ps.setDouble(3, probability);
                ps.setLong(4, System.currentTimeMillis());
                ps.executeUpdate();
            }
            
            cleanOldViolationsForPlayer(uuid);
            
        } catch (SQLException e) {
           
        }
    }

    public List<ViolationRecord> getRecentViolations(UUID uuid, int limit) {
        List<ViolationRecord> violations = new ArrayList<>();
        try (PreparedStatement ps = connection.prepareStatement(
            "SELECT probability, timestamp FROM violations WHERE uuid = ? ORDER BY timestamp DESC LIMIT ?"
        )) {
            ps.setString(1, uuid.toString());
            ps.setInt(2, limit);

            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    violations.add(new ViolationRecord(
                        rs.getDouble("probability"),
                        rs.getLong("timestamp")
                    ));
                }
            }
        } catch (SQLException e) {
            
        }
        return violations;
    }

    public double getAverageRisk(UUID uuid) {
        try (PreparedStatement ps = connection.prepareStatement(
            "SELECT AVG(probability) as avg FROM violations WHERE uuid = ? AND timestamp > ?"
        )) {
            ps.setString(1, uuid.toString());
            ps.setLong(2, System.currentTimeMillis() - (24 * 60 * 60 * 1000)); 

            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return rs.getDouble("avg");
                }
            }
        } catch (SQLException e) {
            
        }
        return 0.0;
    }

    public long getLastViolationTime(UUID uuid) {
        try (PreparedStatement ps = connection.prepareStatement(
            "SELECT MAX(timestamp) as last_time FROM violations WHERE uuid = ?"
        )) {
            ps.setString(1, uuid.toString());

            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return rs.getLong("last_time");
                }
            }
        } catch (SQLException e) {
          
        }
        return 0L;
    }

    public int getViolationCount(UUID uuid) {
        try (PreparedStatement ps = connection.prepareStatement(
            "SELECT COUNT(*) as count FROM violations WHERE uuid = ?"
        )) {
            ps.setString(1, uuid.toString());

            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt("count");
                }
            }
        } catch (SQLException e) {
            
        }
        return 0;
    }

    public Map<UUID, PlayerViolationData> getTopViolators(int limit) {
        Map<UUID, PlayerViolationData> topViolators = new LinkedHashMap<>();
        try (PreparedStatement ps = connection.prepareStatement(
            "SELECT uuid, player_name, AVG(probability) as avg_prob, COUNT(*) as count, MAX(timestamp) as last_time " +
            "FROM violations " +
            "WHERE timestamp > ? " +
            "GROUP BY uuid " +
            "ORDER BY avg_prob DESC " +
            "LIMIT ?"
        )) {
            ps.setLong(1, System.currentTimeMillis() - (24 * 60 * 60 * 1000)); // 24 часа
            ps.setInt(2, limit);

            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    UUID uuid = UUID.fromString(rs.getString("uuid"));
                    String playerName = rs.getString("player_name");
                    double avgProb = rs.getDouble("avg_prob");
                    int count = rs.getInt("count");
                    long lastTime = rs.getLong("last_time");

                    topViolators.put(uuid, new PlayerViolationData(playerName, avgProb, count, lastTime));
                }
            }
        } catch (SQLException e) {
            
        }
        return topViolators;
    }

    public void cleanOldViolations(long olderThanMillis) {
        try (PreparedStatement ps = connection.prepareStatement(
            "DELETE FROM violations WHERE timestamp < ?"
        )) {
            ps.setLong(1, System.currentTimeMillis() - olderThanMillis);
            int deleted = ps.executeUpdate();
            
        } catch (SQLException e) {
           
        }
    }

    private void cleanOldViolationsForPlayer(UUID uuid) {
        try (PreparedStatement ps = connection.prepareStatement(
            "DELETE FROM violations WHERE uuid = ? AND id NOT IN (" +
            "SELECT id FROM violations WHERE uuid = ? ORDER BY timestamp DESC LIMIT 15" +
            ")"
        )) {
            ps.setString(1, uuid.toString());
            ps.setString(2, uuid.toString());
            int deleted = ps.executeUpdate();
            if (deleted > 0) {
                
            }
        } catch (SQLException e) {
           
        }
    }

    public void close() {
        try {
            if (connection != null && !connection.isClosed()) {
                connection.close();
            }
        } catch (SQLException e) {
            
        }
    }

    public static class ViolationRecord {
        private final double probability;
        private final long timestamp;

        public ViolationRecord(double probability, long timestamp) {
            this.probability = probability;
            this.timestamp = timestamp;
        }

        public double getProbability() {
            return probability;
        }

        public long getTimestamp() {
            return timestamp;
        }
    }

    public static class PlayerViolationData {
        private final String playerName;
        private final double averageProbability;
        private final int violationCount;
        private final long lastViolationTime;

        public PlayerViolationData(String playerName, double averageProbability, int violationCount, long lastViolationTime) {
            this.playerName = playerName;
            this.averageProbability = averageProbability;
            this.violationCount = violationCount;
            this.lastViolationTime = lastViolationTime;
        }

        public String getPlayerName() {
            return playerName;
        }

        public double getAverageProbability() {
            return averageProbability;
        }

        public int getViolationCount() {
            return violationCount;
        }

        public long getLastViolationTime() {
            return lastViolationTime;
        }
    }
}
