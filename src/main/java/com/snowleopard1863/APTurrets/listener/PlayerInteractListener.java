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
    @EventHandler(priority = EventPriority.NORMAL)
    public void onClick(@NotNull PlayerInteractEvent event) {
        event.setCancelled(switch (event.getAction()) {
            case RIGHT_CLICK_AIR -> rightClick(event);
            case RIGHT_CLICK_BLOCK -> rightClickBlock(event);
            case LEFT_CLICK_BLOCK -> leftClickBlock(event);
            default -> false;
        });
    }

    private boolean leftClickBlock(@NotNull PlayerInteractEvent event) {
        Material type = event.getClickedBlock().getType();
        if (!Tag.SIGNS.isTagged(type))
            return false;

        Sign sign = (Sign) event.getClickedBlock().getState();
        if (!sign.getLine(0).equalsIgnoreCase("Mounted") || !sign.getLine(1).equalsIgnoreCase("Gun"))
            return false;

        if (event.getPlayer().getInventory().getItemInMainHand().getType() != Material.STONE_BUTTON)
            return true;

        // If a player left-clicks the mounted gun with a stone button in hand, show them the statistics for the plugin (Damage, Knockback, Velocity, etc.)
        event.getPlayer().sendMessage("\n"
                + ChatColor.GOLD + "Damage/Shot: " + ChatColor.GRAY + Config.Damage + "\n"
                + ChatColor.GOLD + "Delay Between Shots: " + ChatColor.GRAY + Config.DelayBetweenShots + "\n"
                + ChatColor.GOLD + "Velocity: " + ChatColor.GRAY + Config.ArrowVelocity + "\n"
                + ChatColor.GOLD + "Fire Chance: " + ChatColor.GRAY + Config.IncindiaryChance * 100.0D + "%\n"
                + ChatColor.GOLD + "Knockback: " + ChatColor.GRAY + Config.KnockbackStrength + "\n"
                + ChatColor.GOLD + "Cost to Place: $" + ChatColor.GRAY + Config.CostToPlace);
        return true;
    }

    private boolean rightClickBlock(@NotNull PlayerInteractEvent event) {
        Material type = event.getClickedBlock().getType();
        if (!Tag.SIGNS.isTagged(type))
            return false;

        Sign sign = (Sign) event.getClickedBlock().getState();
        if (!sign.getLine(0).equalsIgnoreCase("Mounted") || !sign.getLine(1).equalsIgnoreCase("Gun"))
            return false;

        if (event.getClickedBlock().getRelative(BlockFace.DOWN).getType() == Material.SLIME_BLOCK)
            return true;

        Location signPos = event.getClickedBlock().getLocation();
        signPos.setPitch(event.getPlayer().getLocation().getPitch());
        signPos.setDirection(event.getPlayer().getVelocity());
        TurretsMain.getInstance().getTurretManager().mount(event.getPlayer(), signPos);
        return true;
    }

    private boolean rightClick(@NotNull PlayerInteractEvent event) {
        Player player = event.getPlayer();
        if (!TurretsMain.getInstance().getTurretManager().isOnTurret(player) || !player.hasPermission("ap-turrets.use"))
            return false;

        if (player.getInventory().getItemInMainHand().getType() == Material.MILK_BUCKET
                || player.getInventory().getItemInOffHand().getType() == Material.MILK_BUCKET) {
            // If the player tries to use milk to clear the effects, cancel the event to keep that from happening
            return true;
        }

        if (player.getInventory().getItemInMainHand().getType() != Material.STONE_BUTTON
                && player.getInventory().getItemInOffHand().getType() != Material.STONE_BUTTON)
            return false;

        if (TurretsMain.getInstance().getTurretManager().isReloading(player))
            return false;

        // Fires the turret and keeps them from interacting with something else and placing the button accidentally
        TurretsMain.getInstance().getTurretManager().fire(player);
        return true;
    }
}
