package online.prismsmp.strikes;

import com.sk89q.worldedit.bukkit.BukkitAdapter;
import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldguard.WorldGuard;
import com.sk89q.worldguard.protection.ApplicableRegionSet;
import com.sk89q.worldguard.protection.managers.RegionManager;
import com.sk89q.worldguard.protection.regions.ProtectedRegion;
import com.sk89q.worldguard.protection.regions.RegionContainer;
import org.bukkit.Location;
import org.bukkit.World;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

public class WorldGuardHook {

    private final PrismStrikes plugin;

    public WorldGuardHook(PrismStrikes plugin) {
        this.plugin = plugin;
    }

    /**
     * Check if a strike can be used at the given location.
     * Returns null if allowed, or a denial reason string if blocked.
     */
    public String checkLocation(Location loc) {
        if (loc == null || loc.getWorld() == null) return "Invalid location";

        // Check blocked worlds
        List<String> blockedWorlds = plugin.getConfig().getStringList("protection.blocked-worlds");
        String worldName = loc.getWorld().getName();
        for (String blocked : blockedWorlds) {
            if (worldName.equalsIgnoreCase(blocked)) {
                return "Strikes are disabled in this world";
            }
        }

        // Check WorldGuard protected regions
        List<String> protectedRegions = plugin.getConfig().getStringList("protection.protected-regions");
        if (protectedRegions.isEmpty()) return null;

        try {
            RegionContainer container = WorldGuard.getInstance().getPlatform().getRegionContainer();
            RegionManager manager = container.get(BukkitAdapter.adapt(loc.getWorld()));
            if (manager == null) return null;

            BlockVector3 pos = BlockVector3.at(loc.getBlockX(), loc.getBlockY(), loc.getBlockZ());
            ApplicableRegionSet regions = manager.getApplicableRegions(pos);

            for (ProtectedRegion region : regions) {
                if (protectedRegions.contains(region.getId())) {
                    return "This area is protected from orbital strikes";
                }
            }
        } catch (Exception e) {
            plugin.getLogger().warning("WorldGuard check failed: " + e.getMessage());
        }

        return null; // Allowed
    }

    /**
     * Add a region to the protected list.
     */
    public boolean protectRegion(String regionId) {
        List<String> regions = new ArrayList<>(plugin.getConfig().getStringList("protection.protected-regions"));
        if (regions.contains(regionId)) return false;
        regions.add(regionId);
        plugin.getConfig().set("protection.protected-regions", regions);
        plugin.saveConfig();
        return true;
    }

    /**
     * Remove a region from the protected list.
     */
    public boolean unprotectRegion(String regionId) {
        List<String> regions = new ArrayList<>(plugin.getConfig().getStringList("protection.protected-regions"));
        if (!regions.contains(regionId)) return false;
        regions.remove(regionId);
        plugin.getConfig().set("protection.protected-regions", regions);
        plugin.saveConfig();
        return true;
    }

    /**
     * Check if a WorldGuard region exists in any world.
     */
    public boolean regionExists(String regionId) {
        RegionContainer container = WorldGuard.getInstance().getPlatform().getRegionContainer();
        for (World world : plugin.getServer().getWorlds()) {
            RegionManager manager = container.get(BukkitAdapter.adapt(world));
            if (manager != null && manager.hasRegion(regionId)) {
                return true;
            }
        }
        return false;
    }
}
