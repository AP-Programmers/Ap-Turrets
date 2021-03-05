package com.snowleopard1863.APTurrets;

import com.snowleopard1863.APTurrets.config.Config;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Sign;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.HashSet;

import static com.snowleopard1863.APTurrets.TurretsMain.PREFIX;

public class TurretManager {
    private final HashSet<Player> onTurrets = new HashSet<>();
    private final HashSet<Player> reloading = new HashSet<>();

    public void disable() {
        for(Player p : onTurrets) {
            if(p == null)
                continue;

            demount(p, p.getLocation());
        }
        onTurrets.clear();
        reloading.clear();
    }

    public void mount(Player player, @NotNull Location signPos) {
        if (signPos.getBlock().getType() != Material.SIGN && signPos.getBlock().getType() != Material.SIGN_POST && signPos.getBlock().getType() != Material.WALL_SIGN)
            return;

        if (onTurrets.contains(player)) {
            // If the player tries to mount an already mounted turret, tell them no
            player.sendMessage(PREFIX + "You are already on a turret!");
            return;
        }

        Sign sign = (Sign) signPos.getBlock().getState();
        sign.setLine(2, player.getName());
        sign.update();
        onTurrets.add(player);
        signPos.add(0.5D, 0.0D, 0.5D);
        signPos.setDirection(player.getEyeLocation().getDirection());
        player.teleport(signPos);

        // Effects to add zoom and lock a player from jumping
        player.addPotionEffect(new PotionEffect(PotionEffectType.SLOW, 1000000, 6));
        player.addPotionEffect(new PotionEffect(PotionEffectType.JUMP, 1000000, 200));
    }

    public void demount(Player player, @Nullable Location signPos) {
        // Demounting the player from the turret
        onTurrets.remove(player);
        reloading.remove(player);

        if(signPos != null) {
            if (signPos.getBlock().getType() == Material.SIGN || signPos.getBlock().getType() == Material.SIGN_POST || signPos.getBlock().getType() == Material.WALL_SIGN) {
                Sign sign = (Sign) signPos.getBlock().getState();
                sign.setLine(2, "");
                sign.update();
            }
        }

        // Remove potion effects and set their walking speed back to normal
        player.removePotionEffect(PotionEffectType.JUMP);
        player.removePotionEffect(PotionEffectType.SLOW);
    }

    public void fire(Player player) {
        if(!onTurrets.contains(player))
            return;

        if(player.isGliding() || player.isFlying())
            demount(player, player.getLocation());

        startReloading(player);

        TurretsMain.getInstance().getTracerManager().startTracing(arrow);
    }

    private void startReloading(Player player) {
        reloading.add(player);
        Bukkit.getScheduler().scheduleSyncDelayedTask(TurretsMain.getInstance(), new Runnable() {
            @Override
            public void run() {
                reloading.remove(player);
            }
        }, ((int) (Config.DelayBetweenShots * 10.0)));
    }

    public boolean isOnTurret(Player p) {
        return onTurrets.contains(p);
    }

    public boolean isReloading(Player p) {
        return reloading.contains(p);
    }
}
