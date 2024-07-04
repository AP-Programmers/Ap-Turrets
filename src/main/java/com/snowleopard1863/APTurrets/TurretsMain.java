package com.snowleopard1863.APTurrets;

import com.snowleopard1863.APTurrets.config.Config;
import com.snowleopard1863.APTurrets.listener.*;
import net.milkbowl.vault.economy.Economy;
import org.bukkit.ChatColor;
import org.bukkit.event.Listener;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.bukkit.plugin.java.JavaPlugin;

public class TurretsMain extends JavaPlugin implements Listener {
    public static final String PREFIX = ChatColor.AQUA + "[" + ChatColor.RED + "Mounted Gun" + ChatColor.AQUA + "] " + ChatColor.GOLD;

    private Economy economy;
    private static TurretsMain instance;

    public static TurretsMain getInstance() {
        return instance;
    }

    private TurretManager turretManager;

    public void onEnable() {
        saveDefaultConfig();

        Config.Debug = getConfig().getBoolean("Debug", false);
        Config.TakeFromChest = getConfig().getBoolean("TakeFromChest", true);
        Config.TakeFromInventory = getConfig().getBoolean("TakeFromInventory", true);
        Config.RequireAmmo = getConfig().getBoolean("RequireAmmo", true);
        Config.CostToPlace = getConfig().getDouble("CostToPlace", 15000.00D);
        Config.KnockbackStrength = getConfig().getInt("KnockbackStrength", 2);
        Config.IncindiaryChance = getConfig().getDouble("IncindiaryChance", 0.1D);
        Config.Damage = getConfig().getDouble("Damage", 2.5D);
        Config.ArrowVelocity = getConfig().getDouble("ArrowVelocity", 4.0D);
        Config.DelayBetweenShots = getConfig().getDouble("DelayBetweenShots", 0.2D);
        Config.DoRaycast = getConfig().getBoolean("DoRaycast", false);
        Config.RaycastRadians = getConfig().getDouble("RaycastAngle", 5.0D) / 180.0 * Math.PI;
        Config.RaycastBreakElytra = getConfig().getBoolean("RaycastBreakElytra", false);
        Config.RaycastRange = getConfig().getInt("RaycastRange", 160);
        Config.IgnorePilots = getConfig().getBoolean("IgnorePilots", true);


        if (getServer().getPluginManager().getPlugin("Vault") != null) {
            RegisteredServiceProvider<Economy> rsp = getServer().getServicesManager().getRegistration(Economy.class);
            if (rsp != null) {
                economy = rsp.getProvider();
                getLogger().info("Found a compatible Vault plugin.");
            }
            else {
                getLogger().info("[WARNING] Could not find compatible Vault plugin. Disabling Vault integration.");
                economy = null;
            }
        }
        else {
            getLogger().info("Could not find compatible Vault plugin. Disabling Vault integration.");
            economy = null;
        }


        if (getServer().getPluginManager().getPlugin("Movecraft") != null) {
            getLogger().info("Compatible Version Of Movecraft Found.");
        }
        else {
            getLogger().info("[WARNING] Could not find compatible Movecraft Version... Disabling");
            getServer().shutdown();
            return;
        }

        getServer().getPluginManager().registerEvents(this, this);
        getServer().getPluginManager().registerEvents(new EntityDamageEntityListener(), this);
        getServer().getPluginManager().registerEvents(new PlayerDeathListener(), this);
        getServer().getPluginManager().registerEvents(new PlayerInteractEntityListener(), this);
        getServer().getPluginManager().registerEvents(new PlayerInteractListener(), this);
        getServer().getPluginManager().registerEvents(new PlayerMoveListener(), this);
        getServer().getPluginManager().registerEvents(new PlayerQuitListener(), this);
        getServer().getPluginManager().registerEvents(new PlayerToggleSneakListener(), this);
        getServer().getPluginManager().registerEvents(new ProjectileHitListener(), this);
        getServer().getPluginManager().registerEvents(new SignChangeListener(), this);

        turretManager = new TurretManager();
        getLogger().info(getDescription().getName() + " v" + getDescription().getVersion() + " has been enabled.");
        instance = this;
    }

    public void onDisable() {
        turretManager.disable();
        getLogger().info(getDescription().getName() + " v" + getDescription().getVersion() + " has been disabled.");
    }

    public Economy getEconomy() {
        return economy;
    }

    public TurretManager getTurretManager() {
        return turretManager;
    }
}
