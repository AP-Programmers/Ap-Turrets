package com.snowleopard1863.APTurrets.task;

import com.snowleopard1863.APTurrets.TurretsMain;
import org.bukkit.Particle;
import org.bukkit.entity.Arrow;

import java.util.Iterator;

public class ArrowTracerTask implements Runnable {
    @Override
    public void run() {
        Iterator<Arrow> iterator = TurretsMain.getInstance().tracedArrows.iterator();

        while(iterator.hasNext()) {
            Arrow a = iterator.next();
            if (a.isOnGround() || a.isDead() || a.getTicksLived() > 100) {
                a.removeMetadata("tracer", TurretsMain.getInstance());
                iterator.remove();
                a.remove();
            }

            a.getWorld().spawnParticle(Particle.CRIT, a.getLocation(), 1, 0.0D, 0.0D, 0.0D, 0.0D);
        }
    }
}
