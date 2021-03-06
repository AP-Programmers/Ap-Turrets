package com.snowleopard1863.APTurrets.config;

import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;

public class Config {
    public static boolean Debug = false;
    public static boolean TakeFromInventory = true;
    public static boolean TakeFromChest = true;
    public static boolean RequireAmmo = true;
    public static double CostToPlace = 15000.0D;
    public static double Damage = 2.5D;
    public static double IncindiaryChance = 0.1D;
    public static double ArrowVelocity = 4.0D;
    public static int KnockbackStrength = 2;
    public static boolean UseParticleTracers = true;
    public static double DelayBetweenShots = 0.2D;

    public static ItemStack TurretAmmo = new ItemStack(Material.ARROW, 1);
}
