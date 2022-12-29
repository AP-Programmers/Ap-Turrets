package com.snowleopard1863.APTurrets.listener;

import org.bukkit.Effect;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Arrow;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.ProjectileHitEvent;

public class ProjectileHitListener implements Listener {

    @EventHandler
    public void onHit(ProjectileHitEvent event) {
        // If a bullet hits, play a sound for it and add effects
        if(!(event.getEntity() instanceof Arrow))
            return;

        Arrow arrow = (Arrow) event.getEntity();
        if (!arrow.hasMetadata("isTurretBullet"))
            return;

        World world = event.getEntity().getWorld();
        Location l = arrow.getLocation().getBlock().getLocation();
        world.playEffect(l, Effect.STEP_SOUND, l.getBlock().getType());
        world.playEffect(l, Effect.ANVIL_BREAK, 0, 0);
    }
}
