package com.modnmetl.pinevote.placeholders;

import com.modnmetl.pinevote.PineVotePlugin;
import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import org.bukkit.OfflinePlayer;

import java.util.concurrent.atomic.AtomicInteger;

public class PineVoteExpansion extends PlaceholderExpansion {
    private final PineVotePlugin plugin;
    private final AtomicInteger yes;
    private final AtomicInteger no;

    public PineVoteExpansion(PineVotePlugin plugin, AtomicInteger yes, AtomicInteger no) {
        this.plugin = plugin;
        this.yes = yes;
        this.no = no;
    }

    @Override
    public String getIdentifier() {
        return "pinevote";
    }

    @Override
    public String getAuthor() {
        return "MODN METL JT";
    }

    @Override
    public String getVersion() {
        return plugin.getDescription().getVersion();
    }

    @Override
    public boolean persist() {
        return true; // stay registered on /papi reload
    }

    @Override
    public String onRequest(OfflinePlayer player, String params) {
        switch (params.toLowerCase()) {
            case "yes":   return Integer.toString(yes.get());
            case "no":    return Integer.toString(no.get());
            case "total": return Integer.toString(yes.get() + no.get());
            default:      return null;
        }
    }
}
