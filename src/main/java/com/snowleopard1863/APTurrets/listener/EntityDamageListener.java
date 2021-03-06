package com.snowleopard1863.APTurrets.listener;

import com.snowleopard1863.APTurrets.TurretsMain;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageEvent;

public class EntityDamageListener implements Listener {
    @EventHandler
    public void onEntityDamageEvent(EntityDamageEvent event) {
        // If a player gets hit while on a turret, demount them from the turret. If it's a player, stop them from gliding and/or sprinting
        if (!(event.getEntity() instanceof Player))
            return;

        Player player = (Player) event.getEntity();
        if (TurretsMain.getInstance().getTurretManager().isOnTurret(player))
            TurretsMain.getInstance().getTurretManager().demount(player, player.getLocation());

        if (!event.getEntity().hasMetadata("isTurretBullet"))
            return;

        player.setGliding(false);
        player.setSprinting(false);
    }
}
