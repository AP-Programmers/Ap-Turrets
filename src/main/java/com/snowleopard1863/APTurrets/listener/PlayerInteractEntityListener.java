package com.snowleopard1863.APTurrets.listener;

import com.snowleopard1863.APTurrets.TurretsMain;
import org.bukkit.entity.Boat;
import org.bukkit.entity.Horse;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerInteractEntityEvent;

public class PlayerInteractEntityListener implements Listener {
    @EventHandler
    public void onEntityInteract(PlayerInteractEntityEvent e) {
        Player p = e.getPlayer();
        if(!TurretsMain.getInstance().getTurretManager().isOnTurret(p))
            return;
        if(!(e.getRightClicked() instanceof Boat || e.getRightClicked() instanceof Horse))
            return;

        // If someone tries to hop on a horse or boat while already on a gun, demount them from the gun
        TurretsMain.getInstance().getTurretManager().demount(p, p.getLocation());
    }
}
