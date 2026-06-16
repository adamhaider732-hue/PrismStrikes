package online.prismsmp.strikes;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class StrikeCommand implements CommandExecutor, TabCompleter {

    private final PrismStrikes plugin;

    public StrikeCommand(PrismStrikes plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        if (!sender.hasPermission("prismstrikes.admin")) {
            sender.sendMessage(ChatColor.RED + "No permission.");
            return true;
        }

        if (args.length == 0) {
            sendHelp(sender);
            return true;
        }

        switch (args[0].toLowerCase()) {
            case "give" -> handleGive(sender, args);
            case "log" -> handleLog(sender, args);
            case "protect" -> handleProtect(sender, args);
            case "unprotect" -> handleUnprotect(sender, args);
            case "reload" -> handleReload(sender);
            case "list" -> handleList(sender);
            default -> sendHelp(sender);
        }

        return true;
    }

    // ---- /prismstrikes give <player> <tier|material> ----
    private void handleGive(CommandSender sender, String[] args) {
        if (args.length < 3) {
            sender.sendMessage(ChatColor.RED + "Usage: /prismstrikes give <player> <1|2|3|void_crystal|nuclear_core|...>");
            return;
        }

        Player target = Bukkit.getPlayer(args[1]);
        if (target == null) {
            sender.sendMessage(ChatColor.RED + "Player not found: " + args[1]);
            return;
        }

        String itemArg = args[2].toLowerCase();
        StrikeItem type = null;

        // Check if it's a tier number
        try {
            int tier = Integer.parseInt(itemArg);
            type = StrikeItem.strikeByTier(tier);
        } catch (NumberFormatException ignored) {}

        // Check if it's a material name
        if (type == null) {
            type = StrikeItem.fromId(itemArg);
        }

        if (type == null) {
            sender.sendMessage(ChatColor.RED + "Unknown item: " + itemArg);
            sender.sendMessage(ChatColor.GRAY + "Valid: 1, 2, 3, void_crystal, ancient_circuit, nuclear_core, celestial_alloy, quantum_shard, compressed_nether_star");
            return;
        }

        target.getInventory().addItem(plugin.getItemManager().createItem(type));
        sender.sendMessage(ChatColor.GREEN + "Gave " + type.getDisplayName() + ChatColor.GREEN + " to " + target.getName());
        plugin.getStrikeLogger().logCraft(target, type.getDisplayName() + " (admin give by " + sender.getName() + ")");
    }

    // ---- /prismstrikes log [player|recent] [count] ----
    private void handleLog(CommandSender sender, String[] args) {
        String filter = null;
        int count = 10;

        if (args.length >= 2) {
            if (args[1].equalsIgnoreCase("recent")) {
                count = args.length >= 3 ? parseInt(args[2], 10) : 10;
            } else {
                filter = args[1];
                count = args.length >= 3 ? parseInt(args[2], 10) : 10;
            }
        }

        List<String> entries = plugin.getStrikeLogger().getLog(filter, count);
        if (entries.isEmpty()) {
            sender.sendMessage(ChatColor.GRAY + "No log entries found.");
            return;
        }

        sender.sendMessage(ChatColor.GOLD + "--- Strike Log (" + entries.size() + " entries) ---");
        for (String entry : entries) {
            // Color code the log type
            String colored = entry
                    .replace("CRAFT", ChatColor.GREEN + "CRAFT" + ChatColor.GRAY)
                    .replace("USE  ", ChatColor.RED + "USE  " + ChatColor.GRAY)
                    .replace("DENY ", ChatColor.YELLOW + "DENY " + ChatColor.GRAY);
            sender.sendMessage(ChatColor.GRAY + colored);
        }
    }

    // ---- /prismstrikes protect <region> ----
    private void handleProtect(CommandSender sender, String[] args) {
        if (args.length < 2) {
            sender.sendMessage(ChatColor.RED + "Usage: /prismstrikes protect <region_name>");
            return;
        }

        String regionId = args[1];

        if (!plugin.getWorldGuardHook().regionExists(regionId)) {
            sender.sendMessage(ChatColor.RED + "WorldGuard region '" + regionId + "' not found in any world.");
            return;
        }

        if (plugin.getWorldGuardHook().protectRegion(regionId)) {
            sender.sendMessage(ChatColor.GREEN + "Region '" + regionId + "' is now protected from orbital strikes.");
        } else {
            sender.sendMessage(ChatColor.YELLOW + "Region '" + regionId + "' is already protected.");
        }
    }

    // ---- /prismstrikes unprotect <region> ----
    private void handleUnprotect(CommandSender sender, String[] args) {
        if (args.length < 2) {
            sender.sendMessage(ChatColor.RED + "Usage: /prismstrikes unprotect <region_name>");
            return;
        }

        String regionId = args[1];

        if (plugin.getWorldGuardHook().unprotectRegion(regionId)) {
            sender.sendMessage(ChatColor.GREEN + "Region '" + regionId + "' is no longer protected from orbital strikes.");
        } else {
            sender.sendMessage(ChatColor.YELLOW + "Region '" + regionId + "' was not in the protected list.");
        }
    }

    // ---- /prismstrikes reload ----
    private void handleReload(CommandSender sender) {
        plugin.reloadConfig();
        sender.sendMessage(ChatColor.GREEN + "PrismStrikes config reloaded.");
    }

    // ---- /prismstrikes list ----
    private void handleList(CommandSender sender) {
        sender.sendMessage(ChatColor.GOLD + "--- Protected Regions ---");
        List<String> regions = plugin.getConfig().getStringList("protection.protected-regions");
        if (regions.isEmpty()) {
            sender.sendMessage(ChatColor.GRAY + "No regions protected. Use /prismstrikes protect <region>");
        } else {
            for (String r : regions) {
                sender.sendMessage(ChatColor.GRAY + " - " + ChatColor.WHITE + r);
            }
        }

        sender.sendMessage(ChatColor.GOLD + "--- Blocked Worlds ---");
        for (String w : plugin.getConfig().getStringList("protection.blocked-worlds")) {
            sender.sendMessage(ChatColor.GRAY + " - " + ChatColor.WHITE + w);
        }
    }

    // ---- Help ----
    private void sendHelp(CommandSender sender) {
        sender.sendMessage(ChatColor.GOLD + "--- PrismStrikes Commands ---");
        sender.sendMessage(ChatColor.YELLOW + "/ps give <player> <1|2|3|material>" + ChatColor.GRAY + " — Give a strike or component");
        sender.sendMessage(ChatColor.YELLOW + "/ps log [player]" + ChatColor.GRAY + " — View strike log for a player");
        sender.sendMessage(ChatColor.YELLOW + "/ps log recent [count]" + ChatColor.GRAY + " — View recent log entries");
        sender.sendMessage(ChatColor.YELLOW + "/ps protect <region>" + ChatColor.GRAY + " — Block strikes in a WG region");
        sender.sendMessage(ChatColor.YELLOW + "/ps unprotect <region>" + ChatColor.GRAY + " — Unblock a WG region");
        sender.sendMessage(ChatColor.YELLOW + "/ps list" + ChatColor.GRAY + " — Show protected regions/worlds");
        sender.sendMessage(ChatColor.YELLOW + "/ps reload" + ChatColor.GRAY + " — Reload config");
    }

    private int parseInt(String s, int def) {
        try { return Integer.parseInt(s); }
        catch (NumberFormatException e) { return def; }
    }

    // ================================================================
    // Tab Completion
    // ================================================================

    @Override
    public List<String> onTabComplete(CommandSender sender, Command cmd, String alias, String[] args) {
        if (!sender.hasPermission("prismstrikes.admin")) return List.of();

        if (args.length == 1) {
            return filter(args[0], "give", "log", "protect", "unprotect", "list", "reload");
        }

        if (args.length == 2) {
            if (args[0].equalsIgnoreCase("give")) {
                return null; // Player names
            }
            if (args[0].equalsIgnoreCase("log")) {
                List<String> opts = new ArrayList<>();
                opts.add("recent");
                Bukkit.getOnlinePlayers().forEach(p -> opts.add(p.getName()));
                return filter(args[1], opts.toArray(new String[0]));
            }
            if (args[0].equalsIgnoreCase("protect") || args[0].equalsIgnoreCase("unprotect")) {
                return List.of("<region_name>");
            }
        }

        if (args.length == 3 && args[0].equalsIgnoreCase("give")) {
            return filter(args[2], "1", "2", "3", "void_crystal", "ancient_circuit",
                    "nuclear_core", "celestial_alloy", "quantum_shard", "compressed_nether_star");
        }

        return List.of();
    }

    private List<String> filter(String input, String... options) {
        List<String> results = new ArrayList<>();
        for (String opt : options) {
            if (opt.toLowerCase().startsWith(input.toLowerCase())) results.add(opt);
        }
        return results;
    }
}
