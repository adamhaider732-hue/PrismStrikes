package online.prismsmp.strikes;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.ShapedRecipe;
import org.bukkit.inventory.meta.Damageable;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;

import java.util.Arrays;
import java.util.UUID;

public class ItemManager {

    private final PrismStrikes plugin;
    private final NamespacedKey itemTypeKey;
    private final NamespacedKey uuidKey;

    public ItemManager(PrismStrikes plugin) {
        this.plugin = plugin;
        this.itemTypeKey = new NamespacedKey(plugin, "item_type");
        this.uuidKey = new NamespacedKey(plugin, "item_uuid");
    }

    public NamespacedKey getItemTypeKey() { return itemTypeKey; }
    public NamespacedKey getUuidKey() { return uuidKey; }

    public ItemStack createItem(StrikeItem type) {
        ItemStack item = new ItemStack(type.getMaterial(), 1);
        ItemMeta meta = item.getItemMeta();
        if (meta == null) return item;

        meta.setDisplayName(type.getDisplayName());
        meta.setLore(Arrays.asList(type.getLore()));

        PersistentDataContainer pdc = meta.getPersistentDataContainer();
        pdc.set(itemTypeKey, PersistentDataType.STRING, type.getId());

        if (type.isStrike()) {
            pdc.set(uuidKey, PersistentDataType.STRING, UUID.randomUUID().toString());
            meta.setEnchantmentGlintOverride(true);
            meta.addItemFlags(ItemFlag.HIDE_ENCHANTS);

            // Set fishing rod to 1 durability remaining (max 64, damage 63)
            if (meta instanceof Damageable dmg) {
                dmg.setDamage(63);
            }
        }

        item.setItemMeta(meta);
        return item;
    }

    public boolean isItem(ItemStack item, StrikeItem type) {
        if (item == null || !item.hasItemMeta()) return false;
        String id = item.getItemMeta().getPersistentDataContainer().get(itemTypeKey, PersistentDataType.STRING);
        return type.getId().equals(id);
    }

    public boolean isCustomItem(ItemStack item) {
        if (item == null || !item.hasItemMeta()) return false;
        return item.getItemMeta().getPersistentDataContainer().has(itemTypeKey);
    }

    public StrikeItem getType(ItemStack item) {
        if (item == null || !item.hasItemMeta()) return null;
        String id = item.getItemMeta().getPersistentDataContainer().get(itemTypeKey, PersistentDataType.STRING);
        return id == null ? null : StrikeItem.fromId(id);
    }

    public String getItemUUID(ItemStack item) {
        if (item == null || !item.hasItemMeta()) return null;
        return item.getItemMeta().getPersistentDataContainer().get(uuidKey, PersistentDataType.STRING);
    }

    public void registerAllRecipes() {
        registerVoidCrystal();
        registerAncientCircuit();
        registerNuclearCore();
        registerCelestialAlloy();
        registerQuantumShard();
        registerCompressedNetherStar();
        registerStrikeTier1();
        registerStrikeTier2();
        registerStrikeTier3();
        plugin.getLogger().info("Registered 9 custom recipes.");
    }

    private void registerVoidCrystal() {
        ShapedRecipe r = shaped("void_crystal", StrikeItem.VOID_CRYSTAL);
        r.shape("CAC", "AEA", "CAC");
        r.setIngredient('C', Material.CRYING_OBSIDIAN);
        r.setIngredient('A', Material.AMETHYST_SHARD);
        r.setIngredient('E', Material.ECHO_SHARD);
        Bukkit.addRecipe(r);
    }

    private void registerAncientCircuit() {
        ShapedRecipe r = shaped("ancient_circuit", StrikeItem.ANCIENT_CIRCUIT);
        r.shape("CRC", "RHR", "CRC");
        r.setIngredient('C', Material.COPPER_BLOCK);
        r.setIngredient('R', Material.REDSTONE_BLOCK);
        r.setIngredient('H', Material.HEART_OF_THE_SEA);
        Bukkit.addRecipe(r);
    }

    private void registerNuclearCore() {
        ShapedRecipe r = shaped("nuclear_core", StrikeItem.NUCLEAR_CORE);
        r.shape("BTB", "TNT", "BTB");
        r.setIngredient('B', Material.BLAZE_ROD);
        r.setIngredient('T', Material.TNT);
        r.setIngredient('N', Material.NETHER_STAR);
        Bukkit.addRecipe(r);
    }

    private void registerCelestialAlloy() {
        ShapedRecipe r = shaped("celestial_alloy", StrikeItem.CELESTIAL_ALLOY);
        r.shape("NGN", "GDG", "NGN");
        r.setIngredient('N', Material.NETHERITE_INGOT);
        r.setIngredient('G', Material.GOLD_BLOCK);
        r.setIngredient('D', Material.DIAMOND_BLOCK);
        Bukkit.addRecipe(r);
    }

    private void registerQuantumShard() {
        ShapedRecipe r = shaped("quantum_shard", StrikeItem.QUANTUM_SHARD);
        r.shape("NWN", "WTW", "NWN");
        r.setIngredient('N', Material.NETHERITE_INGOT);
        r.setIngredient('W', Material.WITHER_SKELETON_SKULL);
        r.setIngredient('T', Material.TOTEM_OF_UNDYING);
        Bukkit.addRecipe(r);
    }

    private void registerCompressedNetherStar() {
        ShapedRecipe r = shaped("compressed_nether_star", StrikeItem.COMPRESSED_NETHER_STAR);
        r.shape("GNG", "NBN", "GNG");
        r.setIngredient('G', Material.GOLD_BLOCK);
        r.setIngredient('N', Material.NETHER_STAR);
        r.setIngredient('B', Material.BEACON);
        Bukkit.addRecipe(r);
    }

    private void registerStrikeTier1() {
        ShapedRecipe r = shaped("strike_tier_1", StrikeItem.STRIKE_TIER_1);
        r.shape("ATA", "PNP", "ATA");
        r.setIngredient('A', Material.AMETHYST_SHARD);
        r.setIngredient('T', Material.TNT);
        r.setIngredient('P', Material.PRISMARINE_CRYSTALS);
        r.setIngredient('N', Material.NETHER_STAR);
        Bukkit.addRecipe(r);
    }

    private void registerStrikeTier2() {
        ShapedRecipe r = shaped("strike_tier_2", StrikeItem.STRIKE_TIER_2);
        r.shape("TFT", "GRG", "TFT");
        r.setIngredient('T', Material.TNT);
        r.setIngredient('F', Material.FIRE_CHARGE);
        r.setIngredient('G', Material.RAW_GOLD);
        r.setIngredient('R', Material.FISHING_ROD);
        Bukkit.addRecipe(r);
    }

    private void registerStrikeTier3() {
        ShapedRecipe r = shaped("strike_tier_3", StrikeItem.STRIKE_TIER_3);
        r.shape("ECE", "TRT", "ECE");
        r.setIngredient('E', Material.ENDER_EYE);
        r.setIngredient('C', Material.NETHER_STAR);
        r.setIngredient('T', Material.TNT);
        r.setIngredient('R', Material.FISHING_ROD);
        Bukkit.addRecipe(r);
    }

    private ShapedRecipe shaped(String key, StrikeItem type) {
        return new ShapedRecipe(new NamespacedKey(plugin, key), createItem(type));
    }
}
