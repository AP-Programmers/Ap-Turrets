package com.snowleopard1863.APTurrets.listener;

import com.snowleopard1863.APTurrets.TurretsMain;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;

public class PlayerDeathListener implements Listener {
    @EventHandler
    public void onPlayerDeath(PlayerDeathEvent e) {
        if (!TurretsMain.getInstance().getTurretManager().isOnTurret(e.getEntity().getKiller()))
            return;

        e.setDeathMessage(e.getEntity().getDisplayName() + " was gunned down by " + e.getEntity().getKiller().getDisplayName() + ".");
    }
}
