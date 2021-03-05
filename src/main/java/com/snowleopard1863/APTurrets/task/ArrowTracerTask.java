package com.snowleopard1863.APTurrets.task;

import com.snowleopard1863.APTurrets.TurretsMain;
import org.bukkit.Particle;
import org.bukkit.entity.Arrow;

public class ArrowTracerTask implements Runnable {
    @Override
    public void run() {
        for(Arrow a : TurretsMain.getInstance().getTracerManager().getTracedArrows()) {
            a.getWorld().spawnParticle(Particle.CRIT, a.getLocation(), 1, 0.0D, 0.0D, 0.0D, 0.0D);

            if (a.isOnGround() || a.isDead() || a.getTicksLived() > 100) {
                TurretsMain.getInstance().getTracerManager().removeTracedArrow(a);
                a.remove();
            }
        }

    }
}
