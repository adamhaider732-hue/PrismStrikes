package online.prismsmp.strikes;

import org.bukkit.ChatColor;
import org.bukkit.Material;

public enum StrikeItem {

    VOID_CRYSTAL(Material.AMETHYST_SHARD, "void_crystal",
            ChatColor.DARK_PURPLE + "" + ChatColor.BOLD + "Void Crystal",
            new String[]{"", ChatColor.GRAY + "A crystallized fragment of the void.",
                    ChatColor.GRAY + "Hums with unstable energy.", "", ChatColor.DARK_GRAY + "Orbital Strike Component"}),

    ANCIENT_CIRCUIT(Material.PRISMARINE_CRYSTALS, "ancient_circuit",
            ChatColor.DARK_AQUA + "" + ChatColor.BOLD + "Ancient Circuit",
            new String[]{"", ChatColor.GRAY + "Recovered from deep ocean ruins.",
                    ChatColor.GRAY + "Still carries faint electrical pulses.", "", ChatColor.DARK_GRAY + "Orbital Strike Component"}),

    NUCLEAR_CORE(Material.FIRE_CHARGE, "nuclear_core",
            ChatColor.RED + "" + ChatColor.BOLD + "Nuclear Core",
            new String[]{"", ChatColor.GRAY + "An unstable core of concentrated",
                    ChatColor.GRAY + "explosive energy. Handle with care.", "", ChatColor.DARK_GRAY + "Orbital Strike Component"}),

    CELESTIAL_ALLOY(Material.RAW_GOLD, "celestial_alloy",
            ChatColor.GOLD + "" + ChatColor.BOLD + "Celestial Alloy",
            new String[]{"", ChatColor.GRAY + "A legendary metal forged from the",
                    ChatColor.GRAY + "rarest materials known to exist.", "", ChatColor.DARK_GRAY + "Orbital Strike Component"}),

    QUANTUM_SHARD(Material.ENDER_EYE, "quantum_shard",
            ChatColor.DARK_PURPLE + "" + ChatColor.BOLD + "Quantum Shard",
            new String[]{"", ChatColor.GRAY + "Contains enough energy to",
                    ChatColor.GRAY + "tear reality apart.", "", ChatColor.DARK_GRAY + "Orbital Strike Component"}),

    COMPRESSED_NETHER_STAR(Material.NETHER_STAR, "compressed_nether_star",
            ChatColor.WHITE + "" + ChatColor.BOLD + "Compressed Nether Star",
            new String[]{"", ChatColor.GRAY + "Multiple Nether Stars fused into",
                    ChatColor.GRAY + "a single impossibly dense core.", "", ChatColor.DARK_GRAY + "Orbital Strike Component"}),

    STRIKE_TIER_1(Material.FISHING_ROD, "strike_tier_1",
            ChatColor.YELLOW + "" + ChatColor.BOLD + "Orbital Strike Beacon",
            new String[]{"", ChatColor.GRAY + "Tier 1 Orbital Strike", "",
                    ChatColor.GOLD + "⚡ " + ChatColor.WHITE + "~20 block radius",
                    ChatColor.GOLD + "⚡ " + ChatColor.WHITE + "Single-use",
                    ChatColor.GOLD + "⚡ " + ChatColor.WHITE + "Costs 10 XP Levels", "", ChatColor.RED + "▸ Right-click to deploy"}),

    STRIKE_TIER_2(Material.FISHING_ROD, "strike_tier_2",
            ChatColor.GOLD + "" + ChatColor.BOLD + "Heavy Orbital Strike",
            new String[]{"", ChatColor.GRAY + "Tier 2 Orbital Strike", "",
                    ChatColor.GOLD + "⚡ " + ChatColor.WHITE + "~40 block radius",
                    ChatColor.GOLD + "⚡ " + ChatColor.WHITE + "Single-use",
                    ChatColor.GOLD + "⚡ " + ChatColor.WHITE + "Costs 20 XP Levels", "", ChatColor.RED + "▸ Right-click to deploy"}),

    STRIKE_TIER_3(Material.FISHING_ROD, "strike_tier_3",
            ChatColor.LIGHT_PURPLE + "" + ChatColor.BOLD + "Cataclysm Orbital Strike",
            new String[]{"", ChatColor.GRAY + "Tier 3 Orbital Strike", "",
                    ChatColor.GOLD + "⚡ " + ChatColor.WHITE + "~55 block radius",
                    ChatColor.GOLD + "⚡ " + ChatColor.WHITE + "Single-use",
                    ChatColor.GOLD + "⚡ " + ChatColor.WHITE + "Costs 30 XP Levels", "", ChatColor.DARK_RED + "" + ChatColor.BOLD + "▸ Right-click to deploy"});

    private final Material material;
    private final String id;
    private final String displayName;
    private final String[] lore;

    StrikeItem(Material material, String id, String displayName, String[] lore) {
        this.material = material; this.id = id; this.displayName = displayName; this.lore = lore;
    }

    public Material getMaterial() { return material; }
    public String getId() { return id; }
    public String getDisplayName() { return displayName; }
    public String[] getLore() { return lore; }
    public boolean isStrike() { return this == STRIKE_TIER_1 || this == STRIKE_TIER_2 || this == STRIKE_TIER_3; }

    public int getStrikeTier() {
        return switch (this) { case STRIKE_TIER_1 -> 1; case STRIKE_TIER_2 -> 2; case STRIKE_TIER_3 -> 3; default -> 0; };
    }

    public static StrikeItem fromId(String id) {
        for (StrikeItem item : values()) { if (item.id.equals(id)) return item; } return null;
    }

    public static StrikeItem strikeByTier(int tier) {
        return switch (tier) { case 1 -> STRIKE_TIER_1; case 2 -> STRIKE_TIER_2; case 3 -> STRIKE_TIER_3; default -> null; };
    }
}
