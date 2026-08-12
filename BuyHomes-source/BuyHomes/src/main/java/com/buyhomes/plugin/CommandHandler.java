package com.buyhomes.plugin;

import net.milkbowl.vault.economy.Economy;
import net.milkbowl.vault.economy.EconomyResponse;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.Map;
import java.util.UUID;

public class CommandHandler implements CommandExecutor {

    private final BuyHomesPlugin plugin;
    private final HomeManager homes;

    public CommandHandler(BuyHomesPlugin plugin, HomeManager homes) {
        this.plugin = plugin;
        this.homes = homes;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage(msg("players-only"));
            return true;
        }
        Player player = (Player) sender;

        switch (command.getName().toLowerCase()) {
            case "sethome":
                return handleSetHome(player, args);
            case "home":
                return handleHome(player, args);
            case "delhome":
                return handleDelHome(player, args);
            case "homes":
                return handleHomesList(player);
            case "buyhomes":
                return handleBuyHomes(player);
            default:
                return false;
        }
    }

    private boolean handleSetHome(Player player, String[] args) {
        if (args.length < 1) {
            player.sendMessage(msg("usage-sethome"));
            return true;
        }
        String name = args[0].toLowerCase();
        HomeManager.PlayerHomes data = homes.getData(player.getUniqueId());
        int maxSlots = homes.getMaxSlotsFor(player);

        boolean isNewHome = !data.getHomes().containsKey(name);
        boolean unlimited = maxSlots < 0 || player.hasPermission("buyhomes.admin");

        if (isNewHome && !unlimited && data.getHomes().size() >= maxSlots) {
            player.sendMessage(msg("home-limit-reached").replace("%max%", String.valueOf(maxSlots)));
            return true;
        }

        data.getHomes().put(name, player.getLocation().clone());
        homes.save(player.getUniqueId());
        player.sendMessage(msg("home-set").replace("%name%", name));
        return true;
    }

    private boolean handleHome(Player player, String[] args) {
        if (args.length < 1) {
            player.sendMessage(msg("usage-home"));
            return true;
        }
        String name = args[0].toLowerCase();
        HomeManager.PlayerHomes data = homes.getData(player.getUniqueId());
        Location loc = data.getHomes().get(name);
        if (loc == null) {
            player.sendMessage(msg("home-not-found").replace("%name%", name));
            return true;
        }
        player.teleport(loc);
        player.sendMessage(msg("teleported").replace("%name%", name));
        return true;
    }

    private boolean handleDelHome(Player player, String[] args) {
        if (args.length < 1) {
            player.sendMessage(msg("usage-delhome"));
            return true;
        }
        String name = args[0].toLowerCase();
        HomeManager.PlayerHomes data = homes.getData(player.getUniqueId());
        if (data.getHomes().remove(name) == null) {
            player.sendMessage(msg("home-not-found").replace("%name%", name));
            return true;
        }
        homes.save(player.getUniqueId());
        player.sendMessage(msg("home-deleted").replace("%name%", name));
        return true;
    }

    private boolean handleHomesList(Player player) {
        HomeManager.PlayerHomes data = homes.getData(player.getUniqueId());
        int maxSlots = homes.getMaxSlotsFor(player);

        if (data.getHomes().isEmpty()) {
            player.sendMessage(msg("no-homes"));
            return true;
        }

        StringBuilder list = new StringBuilder();
        for (String name : data.getHomes().keySet()) {
            if (list.length() > 0) list.append("&7, &f");
            list.append(name);
        }

        String out = msg("homes-list")
                .replace("%used%", String.valueOf(data.getHomes().size()))
                .replace("%max%", maxSlots < 0 ? "\u221e" : String.valueOf(maxSlots))
                .replace("%list%", ChatColor.translateAlternateColorCodes('&', list.toString()));
        player.sendMessage(out);
        return true;
    }

    private boolean handleBuyHomes(Player player) {
        Economy economy = plugin.getEconomy();
        if (economy == null) {
            player.sendMessage(msg("no-economy"));
            return true;
        }

        HomeManager.PlayerHomes data = homes.getData(player.getUniqueId());
        int maxHomes = homes.getMaxHomes(); // hard cap from config, default 3
        int currentTotal = homes.getBaseHomes() + data.getPurchasedSlots();

        if (maxHomes >= 0 && currentTotal >= maxHomes && !player.hasPermission("buyhomes.admin")) {
            player.sendMessage(msg("max-reached").replace("%max%", String.valueOf(maxHomes)));
            return true;
        }

        double cost = homes.getCostPerHome();

        if (!economy.has(player, cost)) {
            player.sendMessage(msg("not-enough-money").replace("%cost%", economy.format(cost)));
            return true;
        }

        EconomyResponse response = economy.withdrawPlayer(player, cost);
        if (!response.transactionSuccess()) {
            player.sendMessage(msg("not-enough-money").replace("%cost%", economy.format(cost)));
            return true;
        }

        data.setPurchasedSlots(data.getPurchasedSlots() + 1);
        homes.save(player.getUniqueId());

        int newTotal = homes.getBaseHomes() + data.getPurchasedSlots();
        player.sendMessage(msg("bought")
                .replace("%cost%", economy.format(cost))
                .replace("%total%", String.valueOf(newTotal)));
        return true;
    }

    private String msg(String key) {
        String raw = plugin.getConfig().getString("messages." + key, key);
        return ChatColor.translateAlternateColorCodes('&', raw);
    }
}
