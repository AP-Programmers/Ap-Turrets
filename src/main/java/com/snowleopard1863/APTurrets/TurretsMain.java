package com.snowleopard1863.APTurrets;

import java.util.*;

import com.snowleopard1863.APTurrets.config.Config;
import com.snowleopard1863.APTurrets.exception.ArrowLaunchException;
import com.snowleopard1863.APTurrets.listener.*;
import com.snowleopard1863.APTurrets.task.ArrowTracerTask;
import com.snowleopard1863.APTurrets.utils.NMSUtils;
import net.countercraft.movecraft.craft.Craft;
import net.countercraft.movecraft.craft.CraftManager;
import net.countercraft.movecraft.MovecraftLocation;
import net.countercraft.movecraft.utils.HitBox;
import net.milkbowl.vault.economy.Economy;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Effect;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.Sign;
import org.bukkit.entity.Arrow;
import org.bukkit.entity.Player;
import org.bukkit.event.Listener;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

public class TurretsMain extends JavaPlugin implements Listener {
    public static final String PREFIX = ChatColor.AQUA + "[" + ChatColor.RED + "Mounted Gun" + ChatColor.AQUA + "] " + ChatColor.GOLD;
    public static Economy economy;
    private static CraftManager craftManager;

    private static final Material[] INVENTORY_MATERIALS;
    private final ItemStack TURRETAMMO;

    public TurretsMain() {
        this.TURRETAMMO = new ItemStack(Material.ARROW, 1);
    }

    private static TurretsMain instance;

    public static TurretsMain getInstance() {
        return instance;
    }

    private NMSUtils nmsUtils;
    private TurretManager turretManager;
    private TracerManager tracerManager;

    public void onEnable() {
        saveDefaultConfig();

        Config.Debug = getConfig().getBoolean("Debug mode", false);
        Config.TakeFromChest = getConfig().getBoolean("Take arrows from chest", true);
        Config.TakeFromInventory = getConfig().getBoolean("Take arrows from inventory", true);
        Config.RequireAmmo = getConfig().getBoolean("Require Ammo", true);
        Config.CostToPlace = getConfig().getDouble("Cost to Place", 15000.00D);
        Config.KnockbackStrength = getConfig().getInt("Knockback strength", 2);
        Config.IncindiaryChance = getConfig().getDouble("Incindiary chance", 0.1D);
        Config.Damage = getConfig().getDouble("Damage per arrow", 2.5D);
        Config.ArrowVelocity = getConfig().getDouble("Arrow velocity", 4.0D);
        Config.UseParticleTracers = getConfig().getBoolean("Particle tracers", true);
        Config.DelayBetweenShots = getConfig().getDouble("Delay between shots", 0.2D);


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
            craftManager = CraftManager.getInstance();
            getLogger().info("Compatible Version Of Movecraft Found.");
        }
        else {
            getLogger().info("[WARNING] Could not find compatible Movecraft Version... Disabling");
            craftManager = null;
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

    public void onLoad() {
        super.onLoad();
    }

    public void onDisable() {
        turretManager.disable();
        tracerManager.disable();
        getLogger().info(getDescription().getName() + " v" + getDescription().getVersion() + " has been disabled.");
    }

    public void fireTurret(final Player player) {
        // If the player starts to glide, demount them from the turret
        if (player.isGliding()) {
            turretManager.demount(player, player.getLocation());
        } else {

            boolean hasAmmoBeenTaken = !Config.RequireAmmo || this.takeAmmo(player);
            if (!hasAmmoBeenTaken) {
                // If they run out of ammo, don't let them fire and play the empty sound
                player.getWorld().playSound(player.getLocation(), Sound.BLOCK_DISPENSER_FAIL, 1.0F, 2.0F);
                if (Config.Debug) {
                    getLogger().info(player + " is out of ammo");
                }

            } else {
                // Otherwise, fire the gun and launch the arrow of death at their target
                Arrow arrow = this.launchArrow(player);
                arrow.setShooter(player);
                Location offsetLocation = player.getLocation().add(player.getLocation().getDirection().multiply(4)); // Just in front of the player
                arrow.setVelocity(offsetLocation.getDirection().multiply(Config.ArrowVelocity));
                arrow.setBounce(false);
                arrow.setMetadata("isTurretBullet", new FixedMetadataValue(this, true));
                arrow.setKnockbackStrength(Config.KnockbackStrength);
                double rand = Math.random();
                if (rand <= Config.IncindiaryChance) {
                    arrow.setFireTicks(500);
                }

                if (Config.UseParticleTracers) {
                    arrow.setMetadata("tracer", new FixedMetadataValue(this, true));
                    tracedArrows.add(arrow);
                    arrow.setCritical(false);
                    //PacketPlayOutEntityDestroy packet = new PacketPlayOutEntityDestroy(new int[]{arrow.getEntityId()});
                    Iterator var7 = getServer().getOnlinePlayers().iterator();

                    try {
                        Object packet = getNMSUtils().getNMSClass("PacketPlayOutEntityDestroy").getConstructor(int[].class).newInstance(new int[]{arrow.getEntityId()});
                        while (var7.hasNext()) {
                            Player p = (Player) var7.next();
                            Object nmsPlayer = p.getClass().getMethod("getHandle").invoke(p);
                            Object pConn = nmsPlayer.getClass().getField("playerConnection").get(nmsPlayer);
                            pConn.getClass().getMethod("sendPacket", getNMSUtils().getNMSClass("Packet")).invoke(pConn, packet);
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                } else {
                    arrow.setCritical(true);
                }

                World world = player.getWorld();
                world.playSound(player.getLocation(), Sound.ENTITY_FIREWORK_BLAST, 1.0F, 2.0F);
                world.playEffect(player.getLocation(), Effect.MOBSPAWNER_FLAMES, 0);

                if (Config.Debug) {
                    getLogger().info("Mounted Gun Fired.");
                }

            }
        }
    }

    private Arrow launchArrow(Player bukkitPlayer) {
        try {
            Object nmsPlayer = bukkitPlayer.getClass().getMethod("getHandle").invoke(bukkitPlayer);
            Object nmsWorld = nmsPlayer.getClass().getMethod("getWorld").invoke(nmsPlayer);
            Object nmsArrow = getNMSUtils().getNMSClass("EntityTippedArrow").getConstructor(getNMSUtils().getNMSClass("World"), getNMSUtils().getNMSClass("EntityLiving")).newInstance(nmsWorld, nmsPlayer);
            nmsArrow.getClass().getMethod("setNoGravity", boolean.class).invoke(nmsArrow, true);
            nmsWorld.getClass().getMethod("addEntity", getNMSUtils().getNMSClass("Entity")).invoke(nmsWorld, nmsArrow);
            return (Arrow) nmsArrow.getClass().getMethod("getBukkitEntity").invoke(nmsArrow);
        } catch (Throwable e) {
            throw new ArrowLaunchException("Something went wrong when trying to launch an arrow", e);
        }
    }

    public boolean takeAmmo(Player player) {
        // Take ammo from the chest, then from the player, and if both are empty return false and report empty ammo
        if (Config.TakeFromChest) {
            Block signBlock = player.getLocation().getBlock();
            if (signBlock.getType() == Material.WALL_SIGN || signBlock.getType() == Material.SIGN_POST) {
                Sign s = (Sign)signBlock.getState();
                if (craftManager != null) {
                    Craft c = craftManager.getCraftByPlayer(player);
                    if (c != null) {
                        Inventory i = firstInventory(c, this.TURRETAMMO, Material.CHEST, Material.TRAPPED_CHEST);
                        if (i != null) {
                            i.removeItem(new ItemStack[]{this.TURRETAMMO});
                            return true;
                        }
                    }
                }

                Block adjacentBlock = getBlockSignAttachedTo(signBlock);
                if (adjacentBlock.getState() instanceof InventoryHolder) {
                    InventoryHolder inventoryHolder = (InventoryHolder)adjacentBlock.getState();
                    if (inventoryHolder.getInventory().containsAtLeast(this.TURRETAMMO, 1)) {
                        inventoryHolder.getInventory().removeItem(new ItemStack[]{this.TURRETAMMO});
                        return true;
                    }
                }
            }
        }

        if (Config.TakeFromInventory && player.getInventory().containsAtLeast(this.TURRETAMMO, 1)) {
            player.getInventory().removeItem(new ItemStack[]{this.TURRETAMMO});
            player.updateInventory();
            return true;
        } else {
            return false;
        }
    }

    public static Block getBlockSignAttachedTo(Block block) {
        // Get the block the sign is attached to
        if (block.getType().equals(Material.SIGN_POST)) {
            return block.getRelative(BlockFace.DOWN);
        } else {
            if (block.getType().equals(Material.WALL_SIGN)) {
                switch(block.getData()) {
                    case 2:
                        return block.getRelative(BlockFace.SOUTH);
                    case 3:
                        return block.getRelative(BlockFace.NORTH);
                    case 4:
                        return block.getRelative(BlockFace.EAST);
                    case 5:
                        return block.getRelative(BlockFace.WEST);
                }
            }

            return null;
        }
    }

    public static ArrayList<Location> movecraftLocationToBukkitLocation(HitBox movecraftLocations, World world) {
        ArrayList<Location> locations = new ArrayList();

        for(MovecraftLocation ml : movecraftLocations) {
            locations.add(ml.toBukkit(world));
        }

        return locations;
    }

    public static Inventory firstInventory(Craft craft, ItemStack item, Material... lookup) {
        if (craft == null) {
            throw new IllegalArgumentException("craft must not be null");
        } else {
            Iterator var3 = movecraftLocationToBukkitLocation(craft.getHitBox(), craft.getW()).iterator();

            while(var3.hasNext()) {
                Location loc = (Location)var3.next();
                Material[] var5 = lookup;
                int var6 = lookup.length;

                for(int var7 = 0; var7 < var6; ++var7) {
                    Material m = var5[var7];
                    if (loc.getBlock().getType() == m) {
                        Inventory inv = ((InventoryHolder)loc.getBlock().getState()).getInventory();
                        if (item == null) {
                            return inv;
                        }

                        ListIterator var10 = inv.iterator();

                        while(var10.hasNext()) {
                            ItemStack i = (ItemStack)var10.next();
                            if (item.getType() == Material.AIR && (i == null || i.getType() == Material.AIR) || i != null && i.isSimilar(item)) {
                                return inv;
                            }
                        }
                    }
                }
            }

            return null;
        }
    }

    static {
        INVENTORY_MATERIALS = new Material[]{Material.CHEST, Material.TRAPPED_CHEST, Material.FURNACE, Material.HOPPER, Material.DROPPER, Material.DISPENSER, Material.BREWING_STAND};
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
