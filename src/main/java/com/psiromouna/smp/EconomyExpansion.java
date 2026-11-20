package com.psiromouna.smp;

import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import org.bukkit.OfflinePlayer;

public final class EconomyExpansion extends PlaceholderExpansion {
    private final PsiromounaSMP plugin;

    public EconomyExpansion(PsiromounaSMP plugin) { this.plugin = plugin; }

    @Override public String getIdentifier() { return "psiromouna"; }
    @Override public String getAuthor() { return "Psiromouna SMP"; }
    @Override public String getVersion() { return plugin.getDescription().getVersion(); }
    @Override public boolean persist() { return true; }

    @Override public String onRequest(OfflinePlayer player, String params) {
        if (player == null || player.getUniqueId() == null) return "";
        return switch (params.toLowerCase()) {
            case "lefta" -> String.format(java.util.Locale.US, "%.0f", plugin.placeholderBalance(player.getUniqueId(), "lefta"));
            case "shards" -> String.format(java.util.Locale.US, "%.0f", plugin.placeholderBalance(player.getUniqueId(), "shards"));
            default -> null;
        };
    }
}
