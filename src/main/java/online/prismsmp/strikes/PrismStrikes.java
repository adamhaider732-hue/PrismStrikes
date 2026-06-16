package online.prismsmp.strikes;

import org.bukkit.plugin.java.JavaPlugin;

public class PrismStrikes extends JavaPlugin {

    private ItemManager itemManager;
    private WorldGuardHook worldGuardHook;
    private StrikeExecutor strikeExecutor;
    private StrikeLogger strikeLogger;

    @Override
    public void onEnable() {
        // Save default config
        saveDefaultConfig();

        // Initialize systems
        itemManager = new ItemManager(this);
        worldGuardHook = new WorldGuardHook(this);
        strikeExecutor = new StrikeExecutor(this);
        strikeLogger = new StrikeLogger(this);

        // Register recipes
        itemManager.registerAllRecipes();

        // Register listeners
        getServer().getPluginManager().registerEvents(new StrikeListener(this), this);

        // Register commands
        StrikeCommand cmdHandler = new StrikeCommand(this);
        getCommand("prismstrikes").setExecutor(cmdHandler);
        getCommand("prismstrikes").setTabCompleter(cmdHandler);

        getLogger().info("PrismStrikes v" + getDescription().getVersion() + " enabled!");
        getLogger().info("Strike tiers loaded: 3 | Custom materials: 6 | Recipes: 9");
    }

    @Override
    public void onDisable() {
        // Cancel any active strikes
        if (strikeExecutor != null) {
            strikeExecutor.cancelAll();
        }
        getLogger().info("PrismStrikes disabled.");
    }

    // ---- Getters ----

    public ItemManager getItemManager() { return itemManager; }
    public WorldGuardHook getWorldGuardHook() { return worldGuardHook; }
    public StrikeExecutor getStrikeExecutor() { return strikeExecutor; }
    public StrikeLogger getStrikeLogger() { return strikeLogger; }
}
