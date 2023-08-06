package com.snowleopard1863.APTurrets.listener;

import com.snowleopard1863.APTurrets.TurretsMain;
import com.snowleopard1863.APTurrets.config.Config;

import net.countercraft.movecraft.craft.CraftManager;
import net.countercraft.movecraft.craft.PlayerCraft;
import net.countercraft.movecraft.util.MathUtils;

import org.bukkit.entity.Arrow;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.util.Vector;

public class EntityDamageEntityListener implements Listener {
    @EventHandler
    public void onEntityDamageByEntityEvent(EntityDamageByEntityEvent event) {
        // If the bullet hits something, make the event do the right amount of damage
        if (!(event.getDamager() instanceof Arrow))
            return;
        if (!event.getDamager().hasMetadata("isTurretBullet"))
            return;

        if (event.getEntity().hasMetadata("BeamingRespawn")) {
            event.setCancelled(true);
            event.getDamager().setVelocity(new Vector());
            event.getDamager().remove();
            return;
        }

        event.setDamage(Config.Damage);

        // If a player gets hit while on a turret, demount them from the turret.
        if (!(event.getEntity() instanceof Player)) {
            return;
        }

        Player player = (Player) event.getEntity();
        if (TurretsMain.getInstance().getTurretManager().isOnTurret(player)) {
            TurretsMain.getInstance().getTurretManager().demount(player, player.getLocation());
        }

        // If it's a player, stop them from gliding and/or sprinting
        player.setGliding(false);
        player.setSprinting(false);

        if (!Config.IgnorePilots)
            return;

        PlayerCraft craft = CraftManager.getInstance().getCraftByPlayer(player);
        if (craft == null)
            return;
        if (!MathUtils.locIsNearCraftFast(craft, MathUtils.bukkit2MovecraftLoc(player.getLocation())))
            return;

        event.setCancelled(true);
        event.getDamager().setVelocity(new Vector());
        event.getDamager().remove();
    }
}
