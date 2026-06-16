package online.prismsmp.strikes;

import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Sound;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.inventory.CraftItemEvent;
import org.bukkit.event.inventory.PrepareAnvilEvent;
import org.bukkit.event.inventory.PrepareItemCraftEvent;
import org.bukkit.event.inventory.PrepareGrindstoneEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.CraftingInventory;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.ShapedRecipe;
import org.bukkit.scheduler.BukkitRunnable;

import java.io.*;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

public class StrikeListener implements Listener {

    private final PrismStrikes plugin;
    private final Set<String> usedUUIDs = new HashSet<>();
    private final Set<UUID> fireCooldown = new HashSet<>();
    private final File usedFile;

    public StrikeListener(PrismStrikes plugin) {
        this.plugin = plugin;
        this.usedFile = new File(plugin.getDataFolder(), "used-uuids.txt");
        loadUsedUUIDs();
    }

    // ================================================================
    // Right-Click Activation
    // ================================================================

    @EventHandler
    public void onPlayerUse(PlayerInteractEvent event) {
        if (event.getAction() != Action.RIGHT_CLICK_AIR && event.getAction() != Action.RIGHT_CLICK_BLOCK) return;
        if (event.getHand() != EquipmentSlot.HAND) return;

        Player player = event.getPlayer();
        ItemStack held = player.getInventory().getItemInMainHand();
        ItemManager items = plugin.getItemManager();

        StrikeItem type = items.getType(held);
        if (type == null || !type.isStrike()) return;

        event.setCancelled(true);

        // Prevent double-fire (fishing rod can trigger twice)
        if (fireCooldown.contains(player.getUniqueId())) return;
        fireCooldown.add(player.getUniqueId());
        new BukkitRunnable() {
            @Override
            public void run() { fireCooldown.remove(player.getUniqueId()); }
        }.runTaskLater(plugin, 40); // 2 second cooldown

        int tier = type.getStrikeTier();

        if (!player.hasPermission("prismstrikes.use")) {
            player.sendMessage(ChatColor.RED + "You don't have permission to use orbital strikes.");
            plugin.getStrikeLogger().logDenied(player, tier, "No permission");
            return;
        }

        String itemUuid = items.getItemUUID(held);
        if (itemUuid == null) {
            player.sendMessage(ChatColor.RED + "This strike item is invalid.");
            plugin.getStrikeLogger().logDenied(player, tier, "No UUID on item");
            return;
        }
        if (usedUUIDs.contains(itemUuid)) {
            player.sendMessage(ChatColor.RED + "This strike has already been used.");
            plugin.getStrikeLogger().logDenied(player, tier, "Duplicate UUID: " + itemUuid);
            return;
        }

        Block targetBlock = player.getTargetBlockExact(200);
        if (targetBlock == null) {
            player.sendMessage(ChatColor.RED + "No target found. Aim at a location within 200 blocks.");
            return;
        }
        Location target = targetBlock.getLocation().add(0.5, 1, 0.5);

        String denial = plugin.getWorldGuardHook().checkLocation(target);
        if (denial != null) {
            player.sendMessage(ChatColor.RED + "✖ " + denial);
            plugin.getStrikeLogger().logDenied(player, tier, denial);
            return;
        }

        int reqLevels = plugin.getConfig().getInt("tiers." + tier + ".level-cost", 0);
        if (reqLevels > 0) {
            int playerLevel = player.getLevel();
            if (playerLevel < reqLevels) {
                player.sendMessage(ChatColor.RED + "✖ You need " + reqLevels + " XP Levels to deploy this. (You have " + playerLevel + ")");
                plugin.getStrikeLogger().logDenied(player, tier, "Levels " + playerLevel + " < " + reqLevels);
                return;
            }
            // Consume the levels
            player.setLevel(playerLevel - reqLevels);
            player.sendMessage(ChatColor.GOLD + "⚡ " + ChatColor.GRAY + reqLevels + " levels consumed.");
        }

        // All checks passed
        markUsed(itemUuid);
        held.setAmount(0);

        player.getWorld().playSound(player.getLocation(), Sound.ENTITY_ITEM_BREAK, 1.0f, 1.0f);
        player.sendMessage(ChatColor.RED + "⚡ Orbital Strike Incoming");

        plugin.getStrikeLogger().logUse(player, tier, type.getDisplayName(), target, itemUuid);
        plugin.getStrikeExecutor().execute(player, target, tier);
    }

    // ================================================================
    // Prevent repair
    // ================================================================

    @EventHandler
    public void onAnvil(PrepareAnvilEvent event) {
        ItemStack first = event.getInventory().getFirstItem();
        ItemStack second = event.getInventory().getSecondItem();
        if ((first != null && plugin.getItemManager().isCustomItem(first)) ||
            (second != null && plugin.getItemManager().isCustomItem(second))) {
            event.setResult(null);
        }
    }

    @EventHandler
    public void onGrindstone(PrepareGrindstoneEvent event) {
        for (ItemStack item : event.getInventory().getContents()) {
            if (item != null && plugin.getItemManager().isCustomItem(item)) {
                event.setResult(null);
                return;
            }
        }
    }

    // ================================================================
    // Craft Validation
    // ================================================================

    @EventHandler
    public void onPrepareCraft(PrepareItemCraftEvent event) {
        if (!(event.getRecipe() instanceof ShapedRecipe shaped)) return;
        if (!shaped.getKey().getNamespace().equals("prismstrikes")) return;

        String key = shaped.getKey().getKey();
        CraftingInventory inv = event.getInventory();
        ItemStack[] matrix = inv.getMatrix();

        boolean valid = switch (key) {
            case "strike_tier_1" -> validateTier1(matrix);
            case "strike_tier_2" -> validateTier2(matrix);
            case "strike_tier_3" -> validateTier3(matrix);
            default -> true;
        };

        if (!valid) {
            inv.setResult(null);
        } else if (key.startsWith("strike_tier_")) {
            int tier = Integer.parseInt(key.substring("strike_tier_".length()));
            StrikeItem strikeType = StrikeItem.strikeByTier(tier);
            if (strikeType != null) {
                inv.setResult(plugin.getItemManager().createItem(strikeType));
            }
        }
    }

    @EventHandler
    public void onCraft(CraftItemEvent event) {
        if (!(event.getRecipe() instanceof ShapedRecipe shaped)) return;
        if (!shaped.getKey().getNamespace().equals("prismstrikes")) return;

        String key = shaped.getKey().getKey();
        Player player = (Player) event.getWhoClicked();

        if (!player.hasPermission("prismstrikes.craft")) {
            event.setCancelled(true);
            player.sendMessage(ChatColor.RED + "You don't have permission to craft orbital strike items.");
            return;
        }

        if (key.startsWith("strike_tier_")) {
            int tier = Integer.parseInt(key.substring("strike_tier_".length()));
            StrikeItem strikeType = StrikeItem.strikeByTier(tier);
            if (strikeType == null) return;

            if (event.isShiftClick()) {
                event.setCancelled(true);
                if (player.getInventory().firstEmpty() != -1) {
                    ItemStack result = plugin.getItemManager().createItem(strikeType);
                    player.getInventory().addItem(result);
                    consumeMatrix(event.getInventory());
                    plugin.getStrikeLogger().logCraft(player, strikeType.getDisplayName());
                } else {
                    player.sendMessage(ChatColor.RED + "Your inventory is full.");
                }
            } else {
                event.getInventory().setResult(plugin.getItemManager().createItem(strikeType));
                plugin.getStrikeLogger().logCraft(player, strikeType.getDisplayName());
            }
        } else {
            StrikeItem type = plugin.getItemManager().getType(event.getInventory().getResult());
            if (type != null) {
                plugin.getStrikeLogger().logCraft(player, type.getDisplayName());
            }
        }
    }

    // ================================================================
    // Recipe Validation
    // ================================================================

    private boolean validateTier1(ItemStack[] m) {
        return check(m[0], "void_crystal") && check(m[2], "void_crystal")
            && check(m[6], "void_crystal") && check(m[8], "void_crystal")
            && check(m[3], "ancient_circuit") && check(m[5], "ancient_circuit");
    }

    private boolean validateTier2(ItemStack[] m) {
        return check(m[1], "nuclear_core") && check(m[7], "nuclear_core")
            && check(m[3], "celestial_alloy") && check(m[5], "celestial_alloy")
            && check(m[4], "strike_tier_1");
    }

    private boolean validateTier3(ItemStack[] m) {
        return check(m[0], "quantum_shard") && check(m[2], "quantum_shard")
            && check(m[6], "quantum_shard") && check(m[8], "quantum_shard")
            && check(m[1], "compressed_nether_star") && check(m[7], "compressed_nether_star")
            && check(m[4], "strike_tier_2");
    }

    private boolean check(ItemStack item, String expectedId) {
        if (item == null || !item.hasItemMeta()) return false;
        String id = item.getItemMeta().getPersistentDataContainer()
                .get(plugin.getItemManager().getItemTypeKey(),
                     org.bukkit.persistence.PersistentDataType.STRING);
        return expectedId.equals(id);
    }

    private void consumeMatrix(CraftingInventory inv) {
        ItemStack[] matrix = inv.getMatrix();
        for (int i = 0; i < matrix.length; i++) {
            if (matrix[i] != null && matrix[i].getAmount() > 0) {
                matrix[i].setAmount(matrix[i].getAmount() - 1);
            }
        }
        inv.setMatrix(matrix);
    }

    // ================================================================
    // Used UUID Persistence
    // ================================================================

    private void markUsed(String uuid) {
        usedUUIDs.add(uuid);
        try {
            usedFile.getParentFile().mkdirs();
            try (FileWriter fw = new FileWriter(usedFile, true);
                 BufferedWriter bw = new BufferedWriter(fw)) {
                bw.write(uuid);
                bw.newLine();
            }
        } catch (IOException e) {
            plugin.getLogger().warning("Failed to save used UUID: " + e.getMessage());
        }
    }

    private void loadUsedUUIDs() {
        if (!usedFile.exists()) return;
        try (BufferedReader br = new BufferedReader(new FileReader(usedFile))) {
            String line;
            while ((line = br.readLine()) != null) {
                if (!line.isBlank()) usedUUIDs.add(line.trim());
            }
            plugin.getLogger().info("Loaded " + usedUUIDs.size() + " used strike UUIDs.");
        } catch (IOException e) {
            plugin.getLogger().warning("Failed to load used UUIDs: " + e.getMessage());
        }
    }
}
