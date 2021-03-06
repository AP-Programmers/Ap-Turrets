package com.snowleopard1863.APTurrets;

import com.snowleopard1863.APTurrets.config.Config;
import com.snowleopard1863.APTurrets.listener.*;
import com.snowleopard1863.APTurrets.task.ArrowTracerTask;
import com.snowleopard1863.APTurrets.utils.NMSUtils;
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

    private NMSUtils nmsUtils;
    private TurretManager turretManager;
    private TracerManager tracerManager;

    public void onEnable() {
        saveDefaultConfig();

        Config.Debug = getConfig().getBoolean("Debug", false);
        Config.TakeFromChest = getConfig().getBoolean("TakeFromChest", true);
        Config.TakeFromInventory = getConfig().getBoolean("TakeFromInventory", true);
        Config.RequireAmmo = getConfig().getBoolean("RequireAmmo", true);
        Config.CostToPlace = getConfig().getDouble("CostToPlace", 15000.00D);
        Config.KnockbackStrength = getConfig().getInt("KnockbackStrength", 2);
        Config.IncindiaryChance = getConfig().getDouble("ncindiaryChance", 0.1D);
        Config.Damage = getConfig().getDouble("Damage", 2.5D);
        Config.ArrowVelocity = getConfig().getDouble("ArrowVelocity", 4.0D);
        Config.UseParticleTracers = getConfig().getBoolean("UseParticleTracers", true);
        Config.DelayBetweenShots = getConfig().getDouble("DelayBetweenShots", 0.2D);


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

        if (Config.UseParticleTracers) {
            getServer().getScheduler().scheduleSyncRepeatingTask(this, new ArrowTracerTask(), 0L, 0L);
        }

        getServer().getPluginManager().registerEvents(this, this);
        getServer().getPluginManager().registerEvents(new EntityDamageEntityListener(), this);
        getServer().getPluginManager().registerEvents(new EntityDamageListener(), this);
        getServer().getPluginManager().registerEvents(new PlayerDeathListener(), this);
        getServer().getPluginManager().registerEvents(new PlayerInteractEntityListener(), this);
        getServer().getPluginManager().registerEvents(new PlayerInteractListener(), this);
        getServer().getPluginManager().registerEvents(new PlayerMoveListener(), this);
        getServer().getPluginManager().registerEvents(new PlayerQuitListener(), this);
        getServer().getPluginManager().registerEvents(new PlayerToggleSneakListener(), this);
        getServer().getPluginManager().registerEvents(new ProjectileHitListener(), this);
        getServer().getPluginManager().registerEvents(new SignChangeListener(), this);

        nmsUtils = new NMSUtils(getServer().getClass().getPackage().getName());
        turretManager = new TurretManager();
        tracerManager = new TracerManager();
        getLogger().info(getDescription().getName() + " v" + getDescription().getVersion() + " has been enabled.");
        instance = this;
    }

    public void onDisable() {
        turretManager.disable();
        tracerManager.disable();
        getLogger().info(getDescription().getName() + " v" + getDescription().getVersion() + " has been disabled.");
    }

    public Economy getEconomy() {
        return economy;
    }

    public NMSUtils getNMSUtils() {
        return nmsUtils;
    }

    public TurretManager getTurretManager() {
        return turretManager;
    }

    public TracerManager getTracerManager() {
        return tracerManager;
    }
}
