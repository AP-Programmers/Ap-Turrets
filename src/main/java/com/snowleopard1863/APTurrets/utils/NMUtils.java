package com.snowleopard1863.APTurrets.utils;

public class NMUtils {
    public Class<?> getNMClass(String name) throws ClassNotFoundException {
        return Class.forName("net.minecraft." + name);
    }
}
