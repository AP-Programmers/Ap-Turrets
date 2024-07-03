package com.snowleopard1863.APTurrets;

import org.bukkit.entity.Arrow;
import org.bukkit.entity.Player;
import org.bukkit.metadata.FixedMetadataValue;

import java.util.HashSet;

public class TracerManager {
    private final HashSet<Arrow> tracedArrows = new HashSet<>();

    public void disable() {
        tracedArrows.clear();
    }

    public boolean isTraced(Arrow a) {
        return tracedArrows.contains(a);
    }

    public HashSet<Arrow> getTracedArrows() {
        return tracedArrows;
    }

    public void removeTracedArrow(Arrow a) {
        tracedArrows.remove(a);
    }

    public void startTracing(Arrow arrow) {
        arrow.setMetadata("tracer", new FixedMetadataValue(TurretsMain.getInstance(), true));
        tracedArrows.add(arrow);
        arrow.setCritical(false);

        try {
            Object packet = TurretsMain.getInstance().getNMUtils().getNMClass("PacketPlayOutEntityDestroy").getConstructor(int[].class).newInstance(new int[]{arrow.getEntityId()});
            for(Player p : TurretsMain.getInstance().getServer().getOnlinePlayers()) {
                Object nmsPlayer = p.getClass().getMethod("getHandle").invoke(p);
                Object pConn = nmsPlayer.getClass().getField("playerConnection").get(nmsPlayer);
                pConn.getClass().getMethod("sendPacket", TurretsMain.getInstance().getNMUtils().getNMClass("Packet")).invoke(pConn, packet);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
