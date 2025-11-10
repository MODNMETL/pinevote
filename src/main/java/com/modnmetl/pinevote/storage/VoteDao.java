package com.modnmetl.pinevote.storage;

import java.sql.*;

public class VoteDao {
    private final Database db;

    public VoteDao(Database db) {
        this.db = db;
    }

    public void init() throws SQLException {
        try (Connection c = db.getConnection(); Statement st = c.createStatement()) {
            st.executeUpdate("PRAGMA journal_mode=WAL");
            st.executeUpdate("PRAGMA synchronous=NORMAL");

            // Does the table already exist?
            String existingSql = null;
            try (PreparedStatement ps = c.prepareStatement(
                    "SELECT sql FROM sqlite_master WHERE type='table' AND name='votes'")) {
                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) {
                        existingSql = rs.getString(1);
                    }
                }
            }

            if (existingSql == null) {
                // Fresh create with NO unique on ip
                st.executeUpdate(
                        "CREATE TABLE IF NOT EXISTS votes (" +
                                " uuid TEXT NOT NULL UNIQUE," +
                                " ip TEXT NOT NULL," +
                                " choice TEXT NOT NULL CHECK(choice IN ('YES','NO'))," +
                                " created_at INTEGER NOT NULL" +
                                ")"
                );
                st.executeUpdate("CREATE INDEX IF NOT EXISTS idx_votes_choice ON votes(choice)");
                return;
            }

            // Table exists: migrate ONLY if old schema had UNIQUE on ip
            if (existingSql != null && existingSql.contains("ip TEXT NOT NULL UNIQUE")) {
                c.setAutoCommit(false);
                try (Statement tx = c.createStatement()) {
                    tx.executeUpdate("ALTER TABLE votes RENAME TO votes_old");

                    tx.executeUpdate(
                            "CREATE TABLE votes (" +
                                    " uuid TEXT NOT NULL UNIQUE," +
                                    " ip TEXT NOT NULL," +
                                    " choice TEXT NOT NULL CHECK(choice IN ('YES','NO'))," +
                                    " created_at INTEGER NOT NULL" +
                                    ")"
                    );
                    tx.executeUpdate("CREATE INDEX IF NOT EXISTS idx_votes_choice ON votes(choice)");

                    tx.executeUpdate(
                            "INSERT INTO votes(uuid, ip, choice, created_at) " +
                                    "SELECT uuid, ip, choice, created_at FROM votes_old"
                    );

                    tx.executeUpdate("DROP TABLE votes_old");
                    c.commit();
                } catch (SQLException e) {
                    c.rollback();
                    throw e;
                } finally {
                    c.setAutoCommit(true);
                }
            }
        }
    }

    public boolean hasUuidVoted(java.util.UUID uuid) throws SQLException {
        try (Connection c = db.getConnection();
             PreparedStatement ps = c.prepareStatement(
                     "SELECT 1 FROM votes WHERE uuid = ? LIMIT 1")) {
            ps.setString(1, uuid.toString());
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next();
            }
        }
    }

    public boolean hasIpVoted(String ip) throws SQLException {
        try (Connection c = db.getConnection();
             PreparedStatement ps = c.prepareStatement(
                     "SELECT 1 FROM votes WHERE ip = ? LIMIT 1")) {
            ps.setString(1, ip);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next();
            }
        }
    }

    public void insertVote(java.util.UUID uuid, String ip, VoteChoice choice) throws SQLException {
        try (Connection c = db.getConnection();
             PreparedStatement ps = c.prepareStatement(
                     "INSERT INTO votes(uuid, ip, choice, created_at) VALUES(?,?,?,?)")) {
            ps.setString(1, uuid.toString());
            ps.setString(2, ip);
            ps.setString(3, choice.name());
            ps.setLong(4, System.currentTimeMillis());
            ps.executeUpdate();
        }
    }

    public int countYes() throws SQLException {
        return countByChoice(VoteChoice.YES);
    }

    public int countNo() throws SQLException {
        return countByChoice(VoteChoice.NO);
    }

    private int countByChoice(VoteChoice choice) throws SQLException {
        try (Connection c = db.getConnection();
             PreparedStatement ps = c.prepareStatement(
                     "SELECT COUNT(*) FROM votes WHERE choice = ?")) {
            ps.setString(1, choice.name());
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? rs.getInt(1) : 0;
            }
        }
    }

    public void resetAll() throws SQLException {
        try (Connection c = db.getConnection(); Statement st = c.createStatement()) {
            st.executeUpdate("DELETE FROM votes");
        }
    }
}
