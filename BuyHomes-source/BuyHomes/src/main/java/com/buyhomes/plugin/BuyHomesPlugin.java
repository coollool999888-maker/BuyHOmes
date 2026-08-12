package com.buyhomes.plugin;

import net.milkbowl.vault.economy.Economy;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.bukkit.plugin.java.JavaPlugin;

public final class BuyHomesPlugin extends JavaPlugin {

    private static BuyHomesPlugin instance;
    private Economy economy;
    private HomeManager homeManager;

    @Override
    public void onEnable() {
        instance = this;

        saveDefaultConfig();

        if (!setupEconomy()) {
            getLogger().warning("No Vault economy found! /buyhomes will not work until " +
                    "Vault and an economy plugin (e.g. EssentialsX) are installed.");
        }

        this.homeManager = new HomeManager(this);

        CommandHandler handler = new CommandHandler(this, homeManager);
        getCommand("sethome").setExecutor(handler);
        getCommand("home").setExecutor(handler);
        getCommand("delhome").setExecutor(handler);
        getCommand("homes").setExecutor(handler);
        getCommand("buyhomes").setExecutor(handler);

        getLogger().info("BuyHomes enabled.");
    }

    @Override
    public void onDisable() {
        if (homeManager != null) {
            homeManager.saveAll();
        }
        getLogger().info("BuyHomes disabled.");
    }

    private boolean setupEconomy() {
        if (getServer().getPluginManager().getPlugin("Vault") == null) {
            return false;
        }
        RegisteredServiceProvider<Economy> rsp = getServer().getServicesManager().getRegistration(Economy.class);
        if (rsp == null) {
            return false;
        }
        this.economy = rsp.getProvider();
        return true;
    }

    public Economy getEconomy() {
        return economy;
    }

    public static BuyHomesPlugin getInstance() {
        return instance;
    }
}
