package com.buyhomes.plugin;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Handles per-player home data: home locations and how many extra
 * home slots each player has bought. Data is stored as one YAML
 * file per player under plugins/BuyHomes/playerdata/.
 */
public class HomeManager {

    private final BuyHomesPlugin plugin;
    private final File dataFolder;
    private final Map<UUID, PlayerHomes> cache = new HashMap<>();

    public HomeManager(BuyHomesPlugin plugin) {
        this.plugin = plugin;
        this.dataFolder = new File(plugin.getDataFolder(), "playerdata");
        if (!dataFolder.exists()) {
            dataFolder.mkdirs();
        }
    }

    /** Base free homes every player starts with (from config.yml). */
    public int getBaseHomes() {
        return plugin.getConfig().getInt("base-homes", 1);
    }

    /** Absolute cap on total homes a player can ever have. -1 = unlimited. */
    public int getMaxHomes() {
        return plugin.getConfig().getInt("max-homes", 3);
    }

    /** Cost in the server economy for one additional home slot. */
    public double getCostPerHome() {
        return plugin.getConfig().getDouble("cost-per-home", 40000);
    }

    public int getMaxSlotsFor(Player player) {
        PlayerHomes data = getData(player.getUniqueId());
        return getBaseHomes() + data.getPurchasedSlots();
    }

    public PlayerHomes getData(UUID uuid) {
        return cache.computeIfAbsent(uuid, id -> load(id));
    }

    private File fileFor(UUID uuid) {
        return new File(dataFolder, uuid.toString() + ".yml");
    }

    private PlayerHomes load(UUID uuid) {
        File file = fileFor(uuid);
        PlayerHomes data = new PlayerHomes();
        if (!file.exists()) {
            return data;
        }
        YamlConfiguration yml = YamlConfiguration.loadConfiguration(file);
        data.setPurchasedSlots(yml.getInt("purchased-slots", 0));

        if (yml.isConfigurationSection("homes")) {
            for (String name : yml.getConfigurationSection("homes").getKeys(false)) {
                String path = "homes." + name + ".";
                String worldName = yml.getString(path + "world");
                World world = worldName != null ? Bukkit.getWorld(worldName) : null;
                if (world == null) {
                    continue; // world not loaded (yet); skip this home rather than crash
                }
                double x = yml.getDouble(path + "x");
                double y = yml.getDouble(path + "y");
                double z = yml.getDouble(path + "z");
                float yaw = (float) yml.getDouble(path + "yaw");
                float pitch = (float) yml.getDouble(path + "pitch");
                data.getHomes().put(name.toLowerCase(), new Location(world, x, y, z, yaw, pitch));
            }
        }
        return data;
    }

    public void save(UUID uuid) {
        PlayerHomes data = cache.get(uuid);
        if (data == null) {
            return;
        }
        YamlConfiguration yml = new YamlConfiguration();
        yml.set("purchased-slots", data.getPurchasedSlots());

        Map<String, Location> homes = data.getHomes();
        for (Map.Entry<String, Location> entry : homes.entrySet()) {
            String path = "homes." + entry.getKey() + ".";
            Location loc = entry.getValue();
            yml.set(path + "world", loc.getWorld().getName());
            yml.set(path + "x", loc.getX());
            yml.set(path + "y", loc.getY());
            yml.set(path + "z", loc.getZ());
            yml.set(path + "yaw", (double) loc.getYaw());
            yml.set(path + "pitch", (double) loc.getPitch());
        }

        try {
            yml.save(fileFor(uuid));
        } catch (IOException e) {
            plugin.getLogger().severe("Could not save home data for " + uuid + ": " + e.getMessage());
        }
    }

    public void saveAll() {
        for (UUID uuid : cache.keySet()) {
            save(uuid);
        }
    }

    /** Simple in-memory holder for one player's homes + purchased slot count. */
    public static class PlayerHomes {
        private int purchasedSlots = 0;
        private final Map<String, Location> homes = new LinkedHashMap<>();

        public int getPurchasedSlots() {
            return purchasedSlots;
        }

        public void setPurchasedSlots(int purchasedSlots) {
            this.purchasedSlots = purchasedSlots;
        }

        public Map<String, Location> getHomes() {
            return homes;
        }
    }
}
