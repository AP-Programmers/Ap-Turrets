package com.snowleopard1863.APTurrets;

import com.sk89q.worldguard.bukkit.WGBukkit;
import com.sk89q.worldguard.protection.managers.RegionManager;
import java.util.*;

import com.snowleopard1863.APTurrets.config.Config;
import com.snowleopard1863.APTurrets.exception.ArrowLaunchException;
import com.snowleopard1863.APTurrets.listener.PlayerInteractEntityListener;
import com.snowleopard1863.APTurrets.listener.PlayerInteractListener;
import com.snowleopard1863.APTurrets.listener.PlayerToggleSneakListener;
import com.snowleopard1863.APTurrets.listener.ProjectileHitListener;
import com.snowleopard1863.APTurrets.task.ArrowTracerTask;
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
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.Sign;
import org.bukkit.entity.Arrow;
import org.bukkit.entity.Boat;
import org.bukkit.entity.Horse;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.SignChangeEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.entity.ProjectileHitEvent;
import org.bukkit.event.player.*;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

public class TurretsMain extends JavaPlugin implements Listener {
    public final ArrayList<Player> onTurrets = new ArrayList<>();
    public final ArrayList<Player> reloading = new ArrayList<>();
    public final ArrayList<Arrow> tracedArrows = new ArrayList<>();
    private static Economy economy;
    private static CraftManager craftManager;

    private String serverVersion;
    private static final Material[] INVENTORY_MATERIALS;
    private final ItemStack TURRETAMMO;

    public TurretsMain() {
        this.TURRETAMMO = new ItemStack(Material.ARROW, 1);
    }

    private static TurretsMain instance;

    public static TurretsMain getInstance() {
        return instance;
    }

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
        getServer().getPluginManager().registerEvents(new PlayerInteractEntityListener(), this);
        getServer().getPluginManager().registerEvents(new PlayerInteractListener(), this);
        getServer().getPluginManager().registerEvents(new PlayerToggleSneakListener(), this);
        getServer().getPluginManager().registerEvents(new ProjectileHitListener(), this);

        String packageName = getServer().getClass().getPackage().getName();
        serverVersion = packageName.substring(packageName.lastIndexOf(".") + 1);
        getLogger().info(getDescription().getName() + " v" + getDescription().getVersion() + " has been enabled.");
        instance = this;
    }

    public void onLoad() {
        super.onLoad();
    }

    public void onDisable() {
        // This loops through and kicks all players off their turrets so they aren't just...stuck
        for (Player player : onTurrets) {
            demount(player, player.getLocation());
            onTurrets.remove(player);
        }

        reloading.clear();
        tracedArrows.clear();
        getLogger().info(getDescription().getName() + " v" + getDescription().getVersion() + " has been disabled.");
    }

    @EventHandler
    public void eventSignChanged(SignChangeEvent event) {
        Player player = event.getPlayer();
        Location location = player.getLocation();
        RegionManager rm = WGBukkit.getRegionManager(player.getWorld());
        if ("Mounted".equalsIgnoreCase(event.getLine(0)) && "Gun".equalsIgnoreCase(event.getLine(1))) {
            if (rm.getApplicableRegions(location).size() <= 0 && !player.hasPermission("ap-turrets.regionoverride")) {
                // If the player can override regions, place the turret. Otherwise, require them to be in a region
                this.sendMessage(player, "You must be inside a airspace or region.");
                event.setCancelled(true);
                if (Config.Debug) {
                    getLogger().info("A Mounted Gun sign failed to place");
                }

                return;
            }

            if (player.hasPermission("ap-turrets.place")) {
                // If they're allowed to place a turret, notify them that they have placed a turret and take money from their account
                if (economy != null) {
                    if (economy.has(player, Config.CostToPlace)) {
                        economy.withdrawPlayer(player, Config.CostToPlace);
                        player.sendMessage(ChatColor.AQUA + "[" + ChatColor.RED + "Mounted Gun" + ChatColor.AQUA + "] " + ChatColor.GOLD + "Mounted Gun Placed!" + ChatColor.GREEN + " $15,000 has been charged to your balance.");
                        event.setLine(0, "Mounted");
                        event.setLine(1, "Gun");
                        if (Config.Debug) {
                            getLogger().info("A Mounted Gun sign has been place");
                        }
                    } else {
                        if (Config.Debug) {
                            getLogger().info("A Mounted Gun sign failed to place");
                        }

                        this.sendMessage(player, "You Don't Have Enough Money To Place A Turret. Cost To Place: " + ChatColor.RED + Config.CostToPlace);
                    }
                } else {
                    this.sendMessage(player, "Turret Created!");
                    if (Config.Debug) {
                        getLogger().info("A Mounted Gun sign has been placed for free due to no vault instalation");
                    }
                }
            } else {
                if (Config.Debug) {
                    getLogger().info("A Mounted Gun sign failed to place");
                }

                event.setCancelled(true);
                this.sendMessage(player, ChatColor.RED + "ERROR " + ChatColor.WHITE + "You Must Be Donor To Place Mounted Guns!");
            }
        }

    }

    public void fireTurret(final Player player) {
        // If the player starts to glide, demount them from the turret
        if (player.isGliding()) {
            this.demount(player, player.getLocation());
        } else {
            this.reloading.add(player);
            Bukkit.getScheduler().scheduleSyncDelayedTask(this, new Runnable() {
                public void run() {
                    TurretsMain.this.reloading.remove(player);
                }
            }, (long)((int)(Config.DelayBetweenShots * 10.0D)));
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
                    this.tracedArrows.add(arrow);
                    arrow.setCritical(false);
                    //PacketPlayOutEntityDestroy packet = new PacketPlayOutEntityDestroy(new int[]{arrow.getEntityId()});
                    Iterator var7 = getServer().getOnlinePlayers().iterator();

                    try {
                        Object packet = getNMSClass("PacketPlayOutEntityDestroy").getConstructor(int[].class).newInstance(new int[]{arrow.getEntityId()});
                        while (var7.hasNext()) {
                            Player p = (Player) var7.next();
                            Object nmsPlayer = p.getClass().getMethod("getHandle").invoke(p);
                            Object pConn = nmsPlayer.getClass().getField("playerConnection").get(nmsPlayer);
                            pConn.getClass().getMethod("sendPacket", getNMSClass("Packet")).invoke(pConn, packet);
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
            Object nmsArrow = getNMSClass("EntityTippedArrow").getConstructor(getNMSClass("World"), getNMSClass("EntityLiving")).newInstance(nmsWorld, nmsPlayer);
            nmsArrow.getClass().getMethod("setNoGravity", boolean.class).invoke(nmsArrow, true);
            nmsWorld.getClass().getMethod("addEntity", getNMSClass("Entity")).invoke(nmsWorld, nmsArrow);
            return (Arrow) nmsArrow.getClass().getMethod("getBukkitEntity").invoke(nmsArrow);
        } catch (Throwable e) {
            throw new ArrowLaunchException("Something went wrong when trying to launch an arrow", e);
        }
    }

    @EventHandler
    public void onEntityDamageEvent(EntityDamageEvent event) {
        // If a player gets hit while on a turret, demount them from the turret. If it's a player, stop them from gliding and/or sprinting
        if (Config.Debug) {
            getLogger().info("An entity was damaged");
        }

        if (event.getEntity() instanceof Player) {
            if (Config.Debug) {
                getLogger().info("It was a player");
            }

            Player player = (Player)event.getEntity();
            if (this.onTurrets.contains(player)) {
                if (Config.Debug) {
                    getLogger().info("on a turret");
                }

                this.demount(player, player.getLocation());
            }

            if (event.getEntity().hasMetadata("isTurretBullet") && player.isGliding()) {
                player.setGliding(false);
                player.setSprinting(false);
            }
        }

    }

    @EventHandler
    public void onEntityDamageByEntityEvent(EntityDamageByEntityEvent event) {
        // If the bullet hits something, make the event do the right amount of damage
        if (event.getDamager() instanceof Arrow && event.getDamager().hasMetadata("isTurretBullet")) {
            if(event.getEntity().hasMetadata("BeamingRespawn")) {
                event.setCancelled(true);
                return;
            }
            event.setDamage(Config.Damage);
            if (Config.Debug) {
                Arrow a = (Arrow)event.getDamager();
                Player shooter = (Player)a.getShooter();
                getLogger().info(event.getEntity() + " was shot by " + shooter.getName() + " for " + event.getDamage() + " It should be doing " + Config.Damage);
            }
        }

    }

    public void mount(Player player, Location signPos) {
        if (signPos.getBlock().getType() != Material.SIGN && signPos.getBlock().getType() != Material.SIGN_POST && signPos.getBlock().getType() != Material.WALL_SIGN) {
            getLogger().warning("Sign not found!");
        } else {
            if (Config.Debug) {
                getLogger().info("Sign detected");
            }

            Sign sign = (Sign)signPos.getBlock().getState();
            if (this.onTurrets.contains(player)) {
                // If the player tries to mount an already mounted turret, tell them no
                if(onTurrets.contains(player)){
                    // this.sendMessage(player, "You are already on this turret!"); // We aren't using a message here, but if you want to, this is the one I made up for it
                }
                else {
                    this.sendMessage(player, "You May Only Have One Person Mounted Per Turret.");
                }
                if (Config.Debug) {
                    getLogger().info("1 player per turret");
                }
            } else {
                if (Config.Debug) {
                    getLogger().info(player.getName() + " is now on a turret");
                }

                sign.setLine(2, player.getName());
                sign.update();
                this.onTurrets.add(player);
                signPos.add(0.5D, 0.0D, 0.5D);
                signPos.setDirection(player.getEyeLocation().getDirection());
                player.teleport(signPos);
                // Effects to add zoom and lock a player from jumping
                player.addPotionEffect(new PotionEffect(PotionEffectType.SLOW, 1000000, 6));
                //player.setWalkSpeed(0.0F); //Previously this was used. If you use it, you don't have to use the onPlayerMove event, but it breaks the zoom effect that slowness gives you.
                player.addPotionEffect(new PotionEffect(PotionEffectType.JUMP, 1000000, 200));

            }
        }

    }

    @EventHandler
    public void onPlayerMove(PlayerMoveEvent e){
        // Whenever the player moves, stop them from doing so.
        Player player = e.getPlayer();
        if (this.onTurrets.contains(player)){
            Location locationPre = e.getFrom();
            Location locationCur = e.getTo();
            if ((locationCur.getBlockX() != locationPre.getBlockX()) || locationCur.getBlockY() != locationPre.getBlockY() || locationCur.getBlockZ() != locationPre.getBlockZ()){
                e.setCancelled(true); // This can reportedly be buggy. I didn't notice any during testing time, but if you do, this is the alternative:
                // player.teleport(locationPre);
                // This will probably cause problems if a player tries to move on a cruising craft however, while cancelling the event will guarantee the player will stick to the craft

            }
        }
    }

    public void demount(Player player, Location signPos) {
        // Demounting the player from the turret
        if (Config.Debug) {
            getLogger().info(player.getName() + " is being taken off a turret");
        }

        this.onTurrets.remove(player);
        this.reloading.remove(player);
        if (signPos.getBlock().getType() != Material.SIGN && signPos.getBlock().getType() != Material.SIGN_POST && signPos.getBlock().getType() != Material.WALL_SIGN) {
            if (Config.Debug) {
                getLogger().warning("Sign not found!");
            }
        } else {
            if (Config.Debug) {
                getLogger().info("sign found and updated");
            }

            Sign sign = (Sign)signPos.getBlock().getState();
            sign.setLine(2, "");
            sign.update();
        }

        signPos.subtract(-0.5D, 0.0D, -0.5D);
        player.setWalkSpeed(0.2F);
        // Remove potion effects and set their walking speed back to normal
        player.removePotionEffect(PotionEffectType.JUMP);
        player.removePotionEffect(PotionEffectType.SLOW);
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent e) {
        // If the player quits, demount them from the turret
        this.demount(e.getPlayer(), e.getPlayer().getLocation());
    }

    public void sendMessage(Player p, String message) {
        // Message formatter
        p.sendMessage(ChatColor.AQUA + "[" + ChatColor.RED + "Mounted Gun" + ChatColor.AQUA + "] " + ChatColor.GOLD + message);
    }

    @EventHandler
    public void onPlayerDeath(PlayerDeathEvent e) {
        if (this.onTurrets.contains(e.getEntity().getKiller())) {
            e.setDeathMessage(e.getEntity().getDisplayName() + " was gunned down by " + e.getEntity().getKiller().getDisplayName() + ".");
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

    private Class<?> getNMSClass(String name) throws ClassNotFoundException {
        return Class.forName("net.minecraft.server." + serverVersion + "." + name);
    }

    static {
        INVENTORY_MATERIALS = new Material[]{Material.CHEST, Material.TRAPPED_CHEST, Material.FURNACE, Material.HOPPER, Material.DROPPER, Material.DISPENSER, Material.BREWING_STAND};
    }
}
