package com.snowleopard1863.APTurrets.listener;

import com.snowleopard1863.APTurrets.config.Config;
import org.bukkit.entity.Arrow;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;

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
            return;
        }

        event.setDamage(Config.Damage);
    }
}
