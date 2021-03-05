package com.snowleopard1863.APTurrets;

import org.bukkit.entity.Arrow;

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
}
