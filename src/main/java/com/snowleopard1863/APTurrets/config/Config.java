package com.snowleopard1863.APTurrets.config;

public class Config {
    public static boolean Debug = false;
    public static boolean takeFromInventory; // Whether to take arrows from a player's inventory
    public static boolean takeFromChest;     // Same as above, except for chests
    public static boolean requireAmmo;       // Whether to require ammo at all
    public static double costToPlace;        // Cost to place a turret
    public static double damage;             // How much damage this should deal
    public static double incindiaryChance;   // Chance to incinerate your enemies
    public static double arrowVelocity;      // Velocity for arrows to go
    public static int knockbackStrength;     // Knockback strength of the arrows
    public static boolean useParticleTracers;// Whether to use tracers
    public static double delayBetweenShots;  // Delay between each shot

}
