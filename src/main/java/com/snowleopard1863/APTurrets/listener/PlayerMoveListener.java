package com.snowleopard1863.APTurrets.listener;

import com.snowleopard1863.APTurrets.TurretsMain;
import org.bukkit.Location;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerMoveEvent;

public class PlayerMoveListener implements Listener {
    @EventHandler
    public void onPlayerMove(PlayerMoveEvent e){
        if (!TurretsMain.getInstance().getTurretManager().isOnTurret(e.getPlayer()))
            return;

        Location locationPre = e.getFrom();
        Location locationCur = e.getTo();
        if ((locationCur.getBlockX() != locationPre.getBlockX()) || locationCur.getBlockY() != locationPre.getBlockY() || locationCur.getBlockZ() != locationPre.getBlockZ()) {
            e.setCancelled(true);
        }
    }
}
