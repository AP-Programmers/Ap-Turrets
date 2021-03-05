package com.snowleopard1863.APTurrets.listener;

import com.snowleopard1863.APTurrets.TurretsMain;
import com.snowleopard1863.APTurrets.config.Config;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.Sign;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;

public class PlayerInteractListener implements Listener {
    @EventHandler
    public void onClick(PlayerInteractEvent event) {
        if (event.getAction() == Action.RIGHT_CLICK_AIR || event.getAction() == Action.RIGHT_CLICK_BLOCK) {
            rightClick(event);
        }
        if (event.getAction() == Action.RIGHT_CLICK_BLOCK) {
            rightClickBlock(event);
        }
        if (event.getAction() == Action.LEFT_CLICK_BLOCK) {
            leftClickBlock(event);
        }
    }

    private void leftClickBlock(PlayerInteractEvent event) {
        if (event.getPlayer().getInventory().getItemInMainHand().getType() != Material.STONE_BUTTON)
            return;

        if(event.getClickedBlock().getType() != Material.SIGN_POST || event.getClickedBlock().getType() != Material.WALL_SIGN || event.getClickedBlock().getType() != Material.SIGN)
            return;

        Sign sign = (Sign) event.getClickedBlock().getState();
        if (sign.getLine(0).equalsIgnoreCase("Mounted") && sign.getLine(1).equalsIgnoreCase("Gun"))
            return;

        // If a player left clicks the mounted gun with a stone button in hand, show them the statistics for the plugin (Damage, Knockback, Velocity, etc.)
        event.getPlayer().sendMessage("\n" + ChatColor.GOLD + "Damage/Shot: " + ChatColor.GRAY + Config.Damage + "\n" + ChatColor.GOLD + "Delay Between Shots: " + ChatColor.GRAY + Config.DelayBetweenShots + "\n" + ChatColor.GOLD + "Velocity: " + ChatColor.GRAY + Config.ArrowVelocity + "\n" + ChatColor.GOLD + "Fire Chance: " + ChatColor.GRAY + Config.IncindiaryChance * 100.0D + "%\n" + ChatColor.GOLD + "Knockback: " + ChatColor.GRAY + Config.KnockbackStrength + "\n" + ChatColor.GOLD + "Cost to Place: $" + ChatColor.GRAY + Config.CostToPlace);
        event.setCancelled(true);
    }

    private void rightClickBlock(PlayerInteractEvent event) {
        if (event.getClickedBlock().getType() != Material.SIGN_POST || event.getClickedBlock().getType() != Material.WALL_SIGN || event.getClickedBlock().getType() != Material.SIGN)
            return;

        Sign sign = (Sign) event.getClickedBlock().getState();
        if (sign.getLine(0).equalsIgnoreCase("Mounted") && sign.getLine(1).equalsIgnoreCase("Gun"))
            return;

            Block b = sign.getLocation().subtract(0.0D, 1.0D, 0.0D).getBlock();
            if (b.getType() == Material.SLIME_BLOCK)
                return;

            Location signPos = event.getClickedBlock().getLocation();
            signPos.setPitch(event.getPlayer().getLocation().getPitch());
            signPos.setDirection(event.getPlayer().getVelocity());
            TurretsMain.getInstance().getTurretManager().mount(event.getPlayer(), signPos);
    }

    private void rightClick(PlayerInteractEvent event) {
        Player player = event.getPlayer();
        if (!TurretsMain.getInstance().getTurretManager().isOnTurret(player) || !player.hasPermission("ap-turrets.use"))
            return;

        if (player.getInventory().getItemInMainHand().getType() == Material.MILK_BUCKET || player.getInventory().getItemInOffHand().getType() == Material.MILK_BUCKET) {
            // If the player tries to use milk to clear the effects, cancel the event to keep that from happening
            event.setCancelled(true);
            return;
        }

        if (player.getInventory().getItemInMainHand().getType() != Material.STONE_BUTTON && player.getInventory().getItemInOffHand().getType() != Material.STONE_BUTTON)
            return;

        if (TurretsMain.getInstance().getTurretManager().isReloading(player))
            return;

        // Fires the turret and keeps them from interacting with something else and placing the button accidentally
        TurretsMain.getInstance().fireTurret(player);
        event.setCancelled(true);
    }
}
