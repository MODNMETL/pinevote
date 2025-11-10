package com.modnmetl.pinevote;

import com.modnmetl.pinevote.commands.PineVoteCommand;
import com.modnmetl.pinevote.placeholders.PineVoteExpansion;
import com.modnmetl.pinevote.storage.Database;
import com.modnmetl.pinevote.storage.VoteDao;
import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

public final class PineVotePlugin extends JavaPlugin {

    private Database database;
    private VoteDao voteDao;

    private final AtomicInteger yesCache = new AtomicInteger(0);
    private final AtomicInteger noCache = new AtomicInteger(0);
    private ExecutorService dbPool;

    @Override
    public void onEnable() {
        saveDefaultConfig(); // reserved for future use
        try {
            File dbFile = new File(getDataFolder(), "pinevote.db");
            if (!getDataFolder().exists()) {
                // noinspection ResultOfMethodCallIgnored
                getDataFolder().mkdirs();
            }
            String url = "jdbc:sqlite:" + dbFile.getAbsolutePath();
            this.database = new Database(url);
            this.voteDao = new VoteDao(database);
            this.voteDao.init();
        } catch (Exception e) {
            getLogger().severe("Failed to initialise SQLite: " + e.getMessage());
            Bukkit.getPluginManager().disablePlugin(this);
            return;
        }

        this.dbPool = Executors.newFixedThreadPool(Math.max(2, Runtime.getRuntime().availableProcessors() / 2));

        // Warm caches
        refreshCaches();

        // Commands
        PineVoteCommand cmd = new PineVoteCommand(this, voteDao, yesCache, noCache, dbPool);
        getCommand("pinevote").setExecutor(cmd);
        getCommand("pinevote").setTabCompleter(cmd);

        // Placeholders (if PAPI is present)
        if (Bukkit.getPluginManager().getPlugin("PlaceholderAPI") != null) {
            new PineVoteExpansion(this, yesCache, noCache).register();
            getLogger().info("PlaceholderAPI detected; pinevote placeholders registered.");
        }

        // Periodic cache refresh (in case of external mutations)
        Bukkit.getScheduler().runTaskTimerAsynchronously(this, this::refreshCaches, 20L * 30, 20L * 30); // every 30s
    }

    @Override
    public void onDisable() {
        if (dbPool != null) dbPool.shutdownNow();
        if (database != null) database.closeQuietly();
    }

    public void refreshCaches() {
        try {
            yesCache.set(voteDao.countYes());
            noCache.set(voteDao.countNo());
        } catch (Exception e) {
            getLogger().warning("Failed to refresh caches: " + e.getMessage());
        }
    }
}
