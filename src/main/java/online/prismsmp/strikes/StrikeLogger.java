package online.prismsmp.strikes;

import org.bukkit.Location;
import org.bukkit.entity.Player;

import java.io.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

public class StrikeLogger {

    private final PrismStrikes plugin;
    private final File logFile;
    private final DateTimeFormatter fmt = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    public StrikeLogger(PrismStrikes plugin) {
        this.plugin = plugin;
        this.logFile = new File(plugin.getDataFolder(), "strikes.log");
    }

    public void logCraft(Player player, String itemName) {
        String msg = String.format("[%s] CRAFT | %s (%s) crafted %s at %s",
                now(), player.getName(), player.getUniqueId(),
                itemName, formatLoc(player.getLocation()));
        write(msg);
    }

    public void logUse(Player player, int tier, String tierName, Location target, String itemUuid) {
        String msg = String.format("[%s] USE   | %s (%s) deployed Tier %d (%s) at %s in %s [uuid:%s]",
                now(), player.getName(), player.getUniqueId(),
                tier, tierName,
                formatCoords(target), target.getWorld().getName(),
                itemUuid);
        write(msg);
    }

    public void logDenied(Player player, int tier, String reason) {
        String msg = String.format("[%s] DENY  | %s attempted Tier %d strike — %s",
                now(), player.getName(), tier, reason);
        write(msg);
    }

    /**
     * Get recent log entries, optionally filtered by player name.
     */
    public List<String> getLog(String playerFilter, int maxLines) {
        List<String> results = new ArrayList<>();
        if (!logFile.exists()) return results;

        try (BufferedReader reader = new BufferedReader(new FileReader(logFile))) {
            List<String> allLines = new ArrayList<>();
            String line;
            while ((line = reader.readLine()) != null) {
                if (playerFilter == null || line.toLowerCase().contains(playerFilter.toLowerCase())) {
                    allLines.add(line);
                }
            }
            // Return last N lines
            int start = Math.max(0, allLines.size() - maxLines);
            results.addAll(allLines.subList(start, allLines.size()));
        } catch (IOException e) {
            plugin.getLogger().warning("Failed to read log file: " + e.getMessage());
        }
        return results;
    }

    private void write(String message) {
        if (plugin.getConfig().getBoolean("logging.log-to-console", true)) {
            plugin.getLogger().info(message);
        }

        if (plugin.getConfig().getBoolean("logging.log-to-file", true)) {
            try {
                logFile.getParentFile().mkdirs();
                try (FileWriter fw = new FileWriter(logFile, true);
                     BufferedWriter bw = new BufferedWriter(fw)) {
                    bw.write(message);
                    bw.newLine();
                }
            } catch (IOException e) {
                plugin.getLogger().warning("Failed to write to strike log: " + e.getMessage());
            }
        }
    }

    private String now() { return LocalDateTime.now().format(fmt); }

    private String formatLoc(Location loc) {
        return String.format("%s %d,%d,%d", loc.getWorld().getName(),
                loc.getBlockX(), loc.getBlockY(), loc.getBlockZ());
    }

    private String formatCoords(Location loc) {
        return String.format("%d, %d, %d", loc.getBlockX(), loc.getBlockY(), loc.getBlockZ());
    }
}
