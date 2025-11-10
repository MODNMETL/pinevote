package com.modnmetl.pinevote.commands;

import com.modnmetl.pinevote.PineVotePlugin;
import com.modnmetl.pinevote.storage.VoteChoice;
import com.modnmetl.pinevote.storage.VoteDao;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.atomic.AtomicInteger;

public class PineVoteCommand implements CommandExecutor, TabCompleter {
    private final PineVotePlugin plugin;
    private final VoteDao dao;
    private final AtomicInteger yesCache;
    private final AtomicInteger noCache;
    private final ExecutorService dbPool;

    public PineVoteCommand(PineVotePlugin plugin, VoteDao dao, AtomicInteger yesCache, AtomicInteger noCache, ExecutorService dbPool) {
        this.plugin = plugin;
        this.dao = dao;
        this.yesCache = yesCache;
        this.noCache = noCache;
        this.dbPool = dbPool;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length == 0) {
            sender.sendMessage(ChatColor.YELLOW + "Usage: /" + label + " <yes|no|status|reset>");
            return true;
        }

        String sub = args[0].toLowerCase();
        switch (sub) {
            case "yes":
            case "no":
                if (!(sender instanceof Player p)) {
                    sender.sendMessage(ChatColor.RED + "Only players can vote.");
                    return true;
                }
                if (!sender.hasPermission("pinevote.vote")) {
                    sender.sendMessage(ChatColor.RED + "You don't have permission to vote.");
                    return true;
                }
                handleVote(p, VoteChoice.fromString(sub));
                return true;

            case "status":
                if (!sender.hasPermission("pinevote.status")) {
                    sender.sendMessage(ChatColor.RED + "You don't have permission.");
                    return true;
                }
                sender.sendMessage(ChatColor.GOLD + "PineVote » " + ChatColor.GREEN + "YES: " + yesCache.get() + ChatColor.GRAY + " | " + ChatColor.RED + "NO: " + noCache.get());
                return true;

            case "reset":
                if (!sender.hasPermission("pinevote.admin.reset")) {
                    sender.sendMessage(ChatColor.RED + "You don't have permission.");
                    return true;
                }
                dbPool.execute(() -> {
                    try {
                        dao.resetAll();
                        plugin.refreshCaches();
                        sender.sendMessage(ChatColor.YELLOW + "[PineVote] All votes reset.");
                    } catch (Exception e) {
                        sender.sendMessage(ChatColor.RED + "[PineVote] Reset failed: " + e.getMessage());
                    }
                });
                return true;

            default:
                sender.sendMessage(ChatColor.YELLOW + "Usage: /" + label + " <yes|no|status|reset>");
                return true;
        }
    }

    private void handleVote(Player p, VoteChoice choice) {
        if (choice == null) {
            p.sendMessage(ChatColor.RED + "Usage: /pinevote <yes|no>");
            return;
        }

        final UUID uuid = p.getUniqueId();
        final String ip = resolvePlayerIp(p);

        // run DB logic off-thread
        dbPool.execute(() -> {
            try {
                // Always block duplicate UUID votes
                if (dao.hasUuidVoted(uuid)) {
                    p.sendMessage(ChatColor.RED + "You have already voted. Your vote is private and cannot be changed.");
                    return;
                }

                // Read bypass permission node from config (default to legacy node)
                String bypassNode = plugin.getConfig().getString("permissions.bypass_node", "noaltsexploits.bypass");

                // If the player does NOT have the bypass permission, perform the IP duplicate check.
                if (ip != null && !p.hasPermission(bypassNode) && dao.hasIpVoted(ip)) {
                    p.sendMessage(ChatColor.RED + "A vote from your location has already been recorded. Duplicate votes are not allowed.");
                    return;
                }

                // Insert vote
                dao.insertVote(uuid, ip == null ? "unknown" : ip, choice);

                // update caches
                if (choice == VoteChoice.YES) {
                    yesCache.incrementAndGet();
                } else {
                    noCache.incrementAndGet();
                }

                p.sendMessage(ChatColor.GREEN + "Thanks — your " + (choice == VoteChoice.YES ? ChatColor.DARK_GREEN + "YES" : ChatColor.DARK_RED + "NO") + ChatColor.GREEN + " vote has been recorded.");

                // (optional) silently log to console for auditing
                if (plugin.getConfig().getBoolean("logging.audit_votes_to_console", true)) {
                    plugin.getLogger().info(p.getName() + " voted " + choice + " from IP " + ip);
                }

            } catch (Exception e) {
                p.sendMessage(ChatColor.RED + "Sorry, something went wrong recording your vote. Please try again.");
                plugin.getLogger().severe("Vote failed for " + p.getName() + ": " + e.getMessage());
            }
        });
    }

    private String resolvePlayerIp(Player p) {
        try {
            InetSocketAddress addr = p.getAddress();
            if (addr == null) return null;
            return Objects.requireNonNull(addr.getAddress()).getHostAddress();
        } catch (Exception e) {
            return null;
        }
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        List<String> list = new ArrayList<>();
        if (args.length == 1) {
            if (sender.hasPermission("pinevote.vote")) {
                list.add("yes");
                list.add("no");
            }
            if (sender.hasPermission("pinevote.status")) list.add("status");
            if (sender.hasPermission("pinevote.admin.reset")) list.add("reset");
        }
        return list;
    }
}
