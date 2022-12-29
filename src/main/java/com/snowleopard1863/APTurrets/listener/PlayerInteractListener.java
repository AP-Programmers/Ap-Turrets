package com.snowleopard1863.APTurrets.listener;

import com.snowleopard1863.APTurrets.TurretsMain;
import com.snowleopard1863.APTurrets.config.Config;

import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Tag;
import org.bukkit.block.BlockFace;
import org.bukkit.block.Sign;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerInteractEvent;
import org.jetbrains.annotations.NotNull;

public class PlayerInteractListener implements Listener {
    @EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = true)
    public void onClick(PlayerInteractEvent event) {
        TurretsMain.getInstance().getLogger().info("onClick");
        switch (event.getAction()) {
            case RIGHT_CLICK_AIR:
                rightClick(event);
                break;
            case RIGHT_CLICK_BLOCK:
                rightClickBlock(event);
                break;
            case LEFT_CLICK_BLOCK:
                leftClickBlock(event);
                break;
            case LEFT_CLICK_AIR:
            case PHYSICAL:
            default:
                break;
        }
    }

    private void leftClickBlock(@NotNull PlayerInteractEvent event) {
        TurretsMain.getInstance().getLogger().info("leftClickBlock");
        if (event.getPlayer().getInventory().getItemInMainHand().getType() != Material.STONE_BUTTON)
            return;

        TurretsMain.getInstance().getLogger().info("is stone button");
        Material type = event.getClickedBlock().getType();
        if(!Tag.SIGNS.isTagged(type))
            return;

        TurretsMain.getInstance().getLogger().info("is sign");
        Sign sign = (Sign) event.getClickedBlock().getState();
        if (!sign.getLine(0).equalsIgnoreCase("Mounted") || !sign.getLine(1).equalsIgnoreCase("Gun"))
            return;
    
        TurretsMain.getInstance().getLogger().info("is mounted gun");
        // If a player left clicks the mounted gun with a stone button in hand,
        //  show them the statistics for the plugin (Damage, Knockback, Velocity, etc.)
        event.getPlayer().sendMessage("\n"
                + ChatColor.GOLD + "Damage/Shot: " + ChatColor.GRAY + Config.Damage + "\n"
                + ChatColor.GOLD + "Delay Between Shots: " + ChatColor.GRAY + Config.DelayBetweenShots + "\n"
                + ChatColor.GOLD + "Velocity: " + ChatColor.GRAY + Config.ArrowVelocity + "\n"
                + ChatColor.GOLD + "Fire Chance: " + ChatColor.GRAY + Config.IncindiaryChance * 100.0D + "%\n"
                + ChatColor.GOLD + "Knockback: " + ChatColor.GRAY + Config.KnockbackStrength + "\n"
                + ChatColor.GOLD + "Cost to Place: $" + ChatColor.GRAY + Config.CostToPlace);
        event.setCancelled(true);
    }

    private void rightClickBlock(@NotNull PlayerInteractEvent event) {
        TurretsMain.getInstance().getLogger().info("rightClickBlock");
        Material type = event.getClickedBlock().getType();
        if(!Tag.SIGNS.isTagged(type))
            return;

        TurretsMain.getInstance().getLogger().info("is sign");
        Sign sign = (Sign) event.getClickedBlock().getState();
        if (!sign.getLine(0).equalsIgnoreCase("Mounted") || !sign.getLine(1).equalsIgnoreCase("Gun"))
            return;

        TurretsMain.getInstance().getLogger().info("is mounted gun");
        if (event.getClickedBlock().getRelative(BlockFace.DOWN).getType() == Material.SLIME_BLOCK)
            return;

        TurretsMain.getInstance().getLogger().info("is not slime");
        Location signPos = event.getClickedBlock().getLocation();
        signPos.setPitch(event.getPlayer().getLocation().getPitch());
        signPos.setDirection(event.getPlayer().getVelocity());
        TurretsMain.getInstance().getTurretManager().mount(event.getPlayer(), signPos);
    }

    private void rightClick(PlayerInteractEvent event) {
        TurretsMain.getInstance().getLogger().info("rightClick");
        Player player = event.getPlayer();
        if (!TurretsMain.getInstance().getTurretManager().isOnTurret(player) || !player.hasPermission("ap-turrets.use"))
            return;

        TurretsMain.getInstance().getLogger().info("is on turret & has permission");
        if (player.getInventory().getItemInMainHand().getType() == Material.MILK_BUCKET
                || player.getInventory().getItemInOffHand().getType() == Material.MILK_BUCKET) {
            // If the player tries to use milk to clear the effects, cancel the event to keep that from happening
            event.setCancelled(true);
            return;
        }

        TurretsMain.getInstance().getLogger().info("is not milk");
        if (player.getInventory().getItemInMainHand().getType() != Material.STONE_BUTTON
                && player.getInventory().getItemInOffHand().getType() != Material.STONE_BUTTON)
            return;

        TurretsMain.getInstance().getLogger().info("is stone button");
        if (TurretsMain.getInstance().getTurretManager().isReloading(player))
            return;

        TurretsMain.getInstance().getLogger().info("is not reloading");
        // Fires the turret and keeps them from interacting with something else and placing the button accidentally
        TurretsMain.getInstance().getTurretManager().fire(player);
        event.setCancelled(true);
    }
}
