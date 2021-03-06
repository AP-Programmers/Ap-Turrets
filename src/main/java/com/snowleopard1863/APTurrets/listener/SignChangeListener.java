package com.snowleopard1863.APTurrets.listener;

import com.sk89q.worldguard.bukkit.WGBukkit;
import com.snowleopard1863.APTurrets.TurretsMain;
import com.snowleopard1863.APTurrets.config.Config;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.SignChangeEvent;

public class SignChangeListener implements Listener {
    @EventHandler
    public void eventSignChanged(SignChangeEvent event) {
        if(!event.getLine(0).equalsIgnoreCase("Mounted") || !event.getLine(1).equalsIgnoreCase("Gun"))
            return;

        Player player = event.getPlayer();
        Location location = player.getLocation();
        if (WGBukkit.getRegionManager(player.getWorld()).getApplicableRegions(location).size() <= 0 && !player.hasPermission("ap-turrets.regionoverride")) {
            // If the player can override regions, place the turret. Otherwise, require them to be in a region
            player.sendMessage("You must be inside a airspace or region.");
            event.setCancelled(true);
            return;
        }

        if (!player.hasPermission("ap-turrets.place")) {
            event.setCancelled(true);
            player.sendMessage(ChatColor.RED + "ERROR " + ChatColor.WHITE + "You Must Be Donor To Place Mounted Guns!");
        }

        if(TurretsMain.getInstance().getEconomy() == null) {
            player.sendMessage("Turret Created!");
        }

        // If they're allowed to place a turret, notify them that they have placed a turret and take money from their account
        if (!TurretsMain.getInstance().getEconomy().has(player, Config.CostToPlace)) {
            player.sendMessage("You Don't Have Enough Money To Place A Turret. Cost To Place: " + ChatColor.RED + Config.CostToPlace);
            event.setCancelled(true);
        }

        TurretsMain.getInstance().getEconomy().withdrawPlayer(player, Config.CostToPlace);
        player.sendMessage(ChatColor.AQUA + "[" + ChatColor.RED + "Mounted Gun" + ChatColor.AQUA + "] " + ChatColor.GOLD + "Mounted Gun Placed!" + ChatColor.GREEN + " $15,000 has been charged to your balance.");
        event.setLine(0, "Mounted");
        event.setLine(1, "Gun");
    }
}
