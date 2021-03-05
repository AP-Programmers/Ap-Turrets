package com.snowleopard1863.APTurrets.utils;

public class NMSUtils {
    private final String serverVersion;

    public NMSUtils(String packageName) {
        serverVersion = packageName.substring(packageName.lastIndexOf(".") + 1);
    }

    public Class<?> getNMSClass(String name) throws ClassNotFoundException {
        return Class.forName("net.minecraft.server." + serverVersion + "." + name);
    }
}
