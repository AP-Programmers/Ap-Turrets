package com.snowleopard1863.APTurrets.listener;

import com.snowleopard1863.APTurrets.TurretsMain;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerToggleSneakEvent;

public class PlayerToggleSneakListener implements Listener {
    @EventHandler
    public void onPlayerToggleSneakEvent(PlayerToggleSneakEvent event) {
        // If the player sneaks, demount them from the turret
        Player player = event.getPlayer();
        if(!player.isSneaking())
            return;

        if(!TurretsMain.getInstance().getTurretManager().isOnTurret(player))
            return;

        TurretsMain.getInstance().getTurretManager().demount(player, player.getLocation());
    }
}
