package com.snowleopard1863.APTurrets;


import com.sk89q.worldguard.bukkit.WGBukkit;
import com.sk89q.worldguard.protection.managers.RegionManager;
import net.countercraft.movecraft.craft.Craft;
import net.countercraft.movecraft.craft.CraftManager;
import net.countercraft.movecraft.utils.MovecraftLocation;
import net.milkbowl.vault.economy.Economy;
import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.Sign;
import org.bukkit.configuration.file.FileConfiguration;
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
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerToggleSneakEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.PluginDescriptionFile;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.logging.Logger;

public final class TurretsMain extends JavaPlugin implements Listener {
    private PluginDescriptionFile pdfile = getDescription();
    private Logger logger = Logger.getLogger("Minecraft");
    private List<Player> onTurrets = new ArrayList<>();
    private List<Player> reloading = new ArrayList<>();
    private List<Arrow> tracedArrows = new ArrayList<>();
    private Boolean Debug = false;
    private FileConfiguration config = getConfig();
    private boolean takeFromInventory, takeFromChest, requireAmmo;
    private double costToPlace, damage, incindiaryChance, arrowVelocity;
    private int knockbackStrength;
    private boolean useParticleTracers;
    private double delayBetweenShots;
    private static Economy economy;
    private static CraftManager craftManager;
    private String serverVersion;
    private static final Material[] INVENTORY_MATERIALS = new Material[]{Material.CHEST, Material.TRAPPED_CHEST, Material.FURNACE, Material.HOPPER, Material.DROPPER, Material.DISPENSER, Material.BREWING_STAND};
    private final ItemStack TURRETAMMO = new ItemStack(Material.ARROW, 1);

    @Override
    public void onEnable() {
        //Basic Setup
        String packageName = getServer().getClass().getPackage().getName();
        serverVersion = packageName.substring(packageName.lastIndexOf(".") + 1);
        final Plugin p = this;
        logger.info(pdfile.getName() + " v" + pdfile.getVersion() + " has been enbaled.");
        getServer().getPluginManager().registerEvents(this, this);
        //
        // Sets Default Config Values if None Exist
        //
        config.addDefault("Debug mode", false);
        config.addDefault("Cost to Place", 15000.00);
        config.addDefault("Take arrows from inventory", true);
        config.addDefault("Take arrows from chest", true);
        config.addDefault("Require Ammo", true);
        config.addDefault("Damage per arrow", 2.5);
        config.addDefault("Incindiary chance", 0.10);
        config.addDefault("Knockback strength", 2);
        config.addDefault("Arrow velocity", 4.0);
        config.addDefault("Particle tracers", true);
        config.addDefault("Delay between shots", 0.2);
        config.options().copyDefaults(true);
        this.saveConfig();
        //
        // Loads Config Values If They Exist
        //
        Debug = getConfig().getBoolean("Debug mode");
        takeFromChest = getConfig().getBoolean("Take arrows from chest");
        takeFromInventory = getConfig().getBoolean("Take arrows from inventory");
        costToPlace = getConfig().getDouble("Cost to Place");
        requireAmmo = getConfig().getBoolean("Require Ammo");
        knockbackStrength = getConfig().getInt("Knockback strength");
        incindiaryChance = getConfig().getDouble("Incindiary chance");
        damage = getConfig().getDouble("Damage per arrow");
        arrowVelocity = getConfig().getDouble("Arrow velocity");
        useParticleTracers = getConfig().getBoolean("Particle tracers");
        delayBetweenShots = getConfig().getDouble("Delay between shots");
        //
        // Vault Support
        //
        if (getServer().getPluginManager().getPlugin("Vault") != null) {
            RegisteredServiceProvider<Economy> rsp = getServer().getServicesManager().getRegistration(Economy.class);
            if (rsp != null) {
                economy = rsp.getProvider();
                logger.info("Found a compatible Vault plugin.");
            } else {
                logger.info("[WARNING] Could not find compatible Vault plugin. Disabling Vault integration.");
                economy = null;
            }
        } else {
            logger.info("Could not find compatible Vault plugin. Disabling Vault integration.");
            economy = null;
        }

        //
        // Movecraft Support
        //
        if (getServer().getPluginManager().getPlugin("Movecraft") != null) {
            craftManager = CraftManager.getInstance();
            logger.info("Compatible Version Of Movecraft Found.");
        } else {
            logger.info("[WARNING] Could not find compatible Movecraft Version... Disabling");
            craftManager = null;
        }


        if (useParticleTracers) {
            getServer().getScheduler().scheduleSyncRepeatingTask(this, new Runnable() {
                public void run() {
                    for (Iterator<Arrow> iterator = tracedArrows.iterator(); iterator.hasNext(); ) {
                        Arrow a = iterator.next();
                        if (a.isOnGround() || a.isDead() || a.getTicksLived() > 100) {
                            a.removeMetadata("tracer", p);
                            iterator.remove();
                            a.remove();
                        }
                        World world = a.getWorld();
                        world.spawnParticle(Particle.CRIT, a.getLocation(), 3, 0.0, 0.0, 0.0, 0);
                    }
                }
            }, 0, 0);
        }

    }

    @Override
    public void onLoad() {
        super.onLoad();
        logger = getLogger();
    }

    @Override
    public void onDisable() {
        for (Player player : onTurrets) {
            demount(player, player.getLocation());
            onTurrets.remove(player);
        }
        reloading.clear();
        tracedArrows.clear();
        logger.info(pdfile.getName() + " v" + pdfile.getVersion() + " has been disabled.");
    }


    @EventHandler
    public void onClick(PlayerInteractEvent event) {
        if (event.getAction() == Action.RIGHT_CLICK_AIR || event.getAction() == Action.RIGHT_CLICK_BLOCK) {
            if (Debug) {
                logger.info(event.getPlayer() + " has right clicked");
            }
            Player player = event.getPlayer();
            if (onTurrets.contains(player) && player.hasPermission("ap-turrets.use")) {
                if (Debug) {
                    logger.info(event.getPlayer() + " is on a turret");
                }
                //Prevents Players From Drinking Milk Buckets to Clear Potion Effects
                if (player.getInventory().getItemInMainHand().getType() == Material.MILK_BUCKET || player.getInventory().getItemInOffHand().getType() == Material.MILK_BUCKET) {
                    if (Debug) {
                        logger.info(event.getPlayer() + " has right clicked a milk bucket!");
                    }
                    event.setCancelled(true);
                }
                //Fires Turret If Player Is Not Reloading.
                if ((player.getInventory().getItemInMainHand().getType() == Material.STONE_BUTTON
                        || player.getInventory().getItemInOffHand().getType() == Material.STONE_BUTTON)) {

                    if (reloading.contains(player))
                        return;

                    fireTurret(player);
                    event.setCancelled(true);
                    if (Debug) {
                        logger.info(event.getPlayer() + " has started to shoot");
                    }
                }
            }
        }

        if (event.getAction() == Action.RIGHT_CLICK_BLOCK) {
            if (Debug) {
                logger.info("A block has been right clicked");
            }
            if (event.getClickedBlock().getType() == Material.SIGN_POST
                    || event.getClickedBlock().getType() == Material.WALL_SIGN
                    || event.getClickedBlock().getType() == Material.SIGN) {
                if (Debug) {
                    logger.info("A sign was clicked");
                }
                Sign sign = (Sign) event.getClickedBlock().getState();
                if ("Mounted".equalsIgnoreCase(sign.getLine(0)) && "Gun".equalsIgnoreCase(sign.getLine(1))) {
                    if (Debug) {
                        logger.info("A Mounted Gun sign has been clicked");
                    }
                    Block b = sign.getLocation().subtract(0,1,0).getBlock();
                    if (b.getType() != Material.SLIME_BLOCK)
                    {
                        Location signPos = event.getClickedBlock().getLocation();
                        signPos.setPitch(event.getPlayer().getLocation().getPitch());
                        signPos.setDirection(event.getPlayer().getVelocity());
                        mount(event.getPlayer(), signPos);
                    }
                }
            }
        }

        if (event.getAction() == Action.LEFT_CLICK_BLOCK && event.getPlayer().getInventory().getItemInMainHand().getType() == Material.STONE_BUTTON) {
            if (Debug) {
                logger.info("A block has been left clicked while holding a button");
            }
            if (event.getClickedBlock().getType() == Material.SIGN_POST
                    || event.getClickedBlock().getType() == Material.WALL_SIGN
                    || event.getClickedBlock().getType() == Material.SIGN) {
                if (Debug) {
                    logger.info("A sign was left clicked");
                }
                Sign sign = (Sign) event.getClickedBlock().getState();
                if ("Mounted".equalsIgnoreCase(sign.getLine(0)) && "Gun".equalsIgnoreCase(sign.getLine(1))) {
                    if (Debug) {
                        logger.info("A Mounted Gun sign has been left clicked");
                    }
                    event.setCancelled(true);
                    if (!sign.getLine(3).equals(""))
                        sendMessage(event.getPlayer(), "\n" +
                                ChatColor.GOLD + "Type: " + ChatColor.BLACK + sign.getLine(3) + "\n" +
                                ChatColor.GOLD + "Damage/Shot: " + ChatColor.GRAY + damage + "\n" +
                                ChatColor.GOLD + "Delay Between Shots: " + ChatColor.GRAY + delayBetweenShots + "\n" +
                                ChatColor.GOLD + "Velocity: " + ChatColor.GRAY + arrowVelocity + "\n" +
                                ChatColor.GOLD + "Fire Chance: " + ChatColor.GRAY + incindiaryChance * 100 + "%\n" +
                                ChatColor.GOLD + "Knockback: " + ChatColor.GRAY + knockbackStrength + "\n" +
                                ChatColor.GOLD + "Cost to Place: $" + ChatColor.GRAY + costToPlace
                        );
                    else
                        sendMessage(event.getPlayer(), "\n" +
                                ChatColor.GOLD + "Damage/Shot: " + ChatColor.GRAY + damage + "\n" +
                                ChatColor.GOLD + "Delay Between Shots: " + ChatColor.GRAY + delayBetweenShots + "\n" +
                                ChatColor.GOLD + "Velocity: " + ChatColor.GRAY + arrowVelocity + "\n" +
                                ChatColor.GOLD + "Fire Chance: " + ChatColor.GRAY + incindiaryChance * 100 + "%\n" +
                                ChatColor.GOLD + "Knockback: " + ChatColor.GRAY + knockbackStrength + "\n" +
                                ChatColor.GOLD + "Cost to Place: $" + ChatColor.GRAY + costToPlace
                        );
                }
            }
        }

    }

    //
    // Prevents Players From Mounting Entities To De-Mount A Gun.
    //
    @EventHandler
    public void onEntityInteract(PlayerInteractEntityEvent e) {
        Player p = e.getPlayer();
        if (onTurrets.contains(p) && (e.getRightClicked() instanceof Boat || e.getRightClicked() instanceof Horse)) {
            demount(p, p.getLocation());
            if (Debug) logger.info("Player: " + p.getName() + "Has Mounted An Entity.");
        }
    }


    
    @EventHandler
    public void eventSignChanged(SignChangeEvent event) {
        //get player who placed the sign
        Player player = event.getPlayer();
        Plugin wg = getServer().getPluginManager().getPlugin("WorldGuard");
        Location location = player.getLocation();
        RegionManager rm = WGBukkit.getRegionManager(player.getWorld());
        //check if the sign matches the cases for a turret
        if ("Mounted".equalsIgnoreCase(event.getLine(0)) && "Gun".equalsIgnoreCase(event.getLine(1))) {
        	if (rm.getApplicableRegions(location).size() <= 0) {
        		   sendMessage(player,"You must be inside a airspace or region.");
                   event.setCancelled(true);
                   if (Debug) {
                       logger.info("A Mounted Gun sign failed to place");
                   }
                   return;

        	}
            //check if player has permission to place a turret, than check if they have enough money to place the sign
            if (player.hasPermission("ap-turrets.place")) {
                if (economy != null) {
                    if (economy.has(player, costToPlace)) {
                        //if true charge player a configurable amount and send a message
                        economy.withdrawPlayer(player, 15000);
                        sendMessage(player, "Turret Created!");
                        event.setLine(0, "Mounted");
                        event.setLine(1, "Gun");
                        if (Debug) {
                            logger.info("A Mounted Gun sign has been place");
                        }
                    } else {
                        if (Debug) {
                            logger.info("A Mounted Gun sign failed to place");
                        }
                        //if false, clear the sign and return a permision error
                       
                        sendMessage(player, "You Don't Have Enough Money To Place A Turret. Cost To Place: " + ChatColor.RED + costToPlace);
                    }
                } else {
                    sendMessage(player, "Turret Created!");
                    if (Debug) {
                        logger.info("A Mounted Gun sign has been placed for free due to no vault instalation");
                    }
                }
            } else {
                if (Debug) {
                    logger.info("A Mounted Gun sign failed to place");
                }
                //if false, clear the sign and return a permision error
                event.setCancelled(true);
                sendMessage(player, ChatColor.RED + "ERROR " + ChatColor.WHITE + "You Must Be Donor To Place Mounted Guns!");
            }
        }
    }


    public void fireTurret(final Player player) {
        if (player.isGliding()) {
            demount(player, player.getLocation());
            return;
        }
        reloading.add(player);
        Bukkit.getScheduler().scheduleSyncDelayedTask(this, new Runnable() {
            public void run() {
                reloading.remove(player);
            }
        }, (int) (delayBetweenShots * 10));
        boolean hasAmmoBeenTaken;
        hasAmmoBeenTaken = !requireAmmo || takeAmmo(player);
        if (!hasAmmoBeenTaken) {
            player.getWorld().playSound(player.getLocation(), Sound.BLOCK_DISPENSER_FAIL, 1, 2);
            if (Debug) logger.info(player + " is out of ammo");
            return;
        }
        Arrow arrow = launchArrow(player);
        arrow.setShooter(player);
        arrow.setVelocity(player.getLocation().getDirection().multiply(arrowVelocity));
        arrow.setBounce(false);
        arrow.setMetadata("isTurretBullet", new FixedMetadataValue(this, true));
        arrow.setKnockbackStrength(knockbackStrength);
        double rand = Math.random();
        if (rand <= incindiaryChance) {
            arrow.setFireTicks(500);
        }
        if (useParticleTracers) {
            arrow.setMetadata("tracer", new FixedMetadataValue(this, true));
            tracedArrows.add(arrow);
            arrow.setCritical(false);
            arrow.getEntityId();
            //PacketPlayOutEntityDestroy packet = new PacketPlayOutEntityDestroy(arrow.getEntityId());
            try {
                Object packet = getNMSClass("PacketPlayOutEntityDestroy").getConstructor(int.class).newInstance(arrow.getEntityId());
                for (Player p : getServer().getOnlinePlayers()) {
                    Object nmsPlayer = p.getClass().getMethod("getHandle").invoke(p);
                    Object pConn = nmsPlayer.getClass().getField("playerConnection").get(nmsPlayer);
                    pConn.getClass().getMethod("sendPacket", getNMSClass("Packet")).invoke(pConn, packet);
                }

            } catch (Throwable e) {
                e.printStackTrace();
            }

        } else {
            arrow.setCritical(true);
        }
        World world = player.getWorld();
        world.playSound(player.getLocation(), Sound.ENTITY_FIREWORK_BLAST, 1, 2);
        world.playEffect(player.getLocation(), Effect.MOBSPAWNER_FLAMES, 0);

        if (Debug) logger.info("Mounted Gun Fired.");
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
    public void onPlayerToggleSneakEvent(PlayerToggleSneakEvent event) {
        Player player = event.getPlayer();
        if (Debug) {
            logger.info(player + " sneaked");
        }
        if (player.isSneaking() && onTurrets.contains(player)) {
            demount(player, player.getLocation());
            if (Debug) {
                logger.info(player + " got out of their turret");
            }
        }
    }

    @SuppressWarnings("deprecation")
    @EventHandler
    public void onHit(ProjectileHitEvent event) {
        if (event.getEntity() instanceof Arrow) {
            Arrow arrow = (Arrow) event.getEntity();
            if (arrow.hasMetadata("isTurretBullet")) {
                if (Debug) {
                    logger.info("A bullet has landed");
                }

                Location arrowLoc = arrow.getLocation();
                World world = event.getEntity().getWorld();
                Location l = arrowLoc.getBlock().getLocation();
                arrow.getWorld().playEffect(l, Effect.STEP_SOUND, world.getBlockTypeIdAt(l));
                world.playEffect(l, Effect.TILE_BREAK, l.subtract(0, 1, 0).getBlock().getTypeId(), 0);
            }
        }
    }

    @EventHandler
    public void onEntityDamageEvent(EntityDamageEvent event) {
        if (Debug) {
            logger.info("An entity was damaged");
        }

        if (event.getEntity() instanceof Player) {
            if (Debug) {
                logger.info("It was a player");
            }
            Player player = (Player) event.getEntity();
            if (onTurrets.contains(player)) {
                if (Debug) {
                    logger.info("on a turret");
                }
                demount(player, player.getLocation());
            }

            if (event.getEntity().hasMetadata("isTurretBullet")) {
                if (player.isGliding()) {
                    player.setGliding(false);
                    player.setSprinting(false);
                }
            }
        }
    }
    
    @EventHandler
    public void onEntityDamageByEntityEvent(EntityDamageByEntityEvent event) {
        if (event.getDamager() instanceof Arrow) {
            //if (event.getDamager().getCustomName() == "Bullet") {
            if (event.getDamager().hasMetadata("isTurretBullet")) {
                event.setDamage(damage);
                if (Debug) {
                    Arrow a = (Arrow) event.getDamager();
                    Player shooter = (Player) a.getShooter();
                    logger.info(event.getEntity() + " was shot by " + shooter.getName() + " for " + event.getDamage() + " It should be doing " + damage);
                }
            }
        }
    }

    public void mount(Player player, Location signPos) {
        if (signPos.getBlock().getType() == Material.SIGN || signPos.getBlock().getType() == Material.SIGN_POST
                || signPos.getBlock().getType() == Material.WALL_SIGN) {
            if (Debug) {
                logger.info("Sign detected");
            }
            Sign sign = (Sign) signPos.getBlock().getState();
            if (onTurrets.contains(player)) {
                sendMessage(player, "You May Only Have One Person Mounted Per Turret.");
                if (Debug) {
                    logger.info("1 player per turret");
                }
            } else {
                if (Debug) {
                    logger.info(player.getName() + " is now on a turret");
                }
                sign.setLine(2, player.getName());
                sign.update();
                onTurrets.add(player);
                signPos.add(0.5, 0, 0.5);
                signPos.setDirection(player.getEyeLocation().getDirection());
                player.teleport(signPos);
                player.setWalkSpeed(0);
                //player.addPotionEffect(new PotionEffect(PotionEffectType.SLOW, 1000000, 6));
                player.addPotionEffect(new PotionEffect(PotionEffectType.JUMP, 1000000, 200));
            }
        } else {
            logger.warning("Sign not found!");
        }
    }

    public void demount(Player player, Location signPos) {
        if (Debug) {
            logger.info(player.getName() + " is being taken off a turret");
        }
        onTurrets.remove(player);
        reloading.remove(player);
        if (signPos.getBlock().getType() == Material.SIGN || signPos.getBlock().getType() == Material.SIGN_POST
                || signPos.getBlock().getType() == Material.WALL_SIGN) {
            if (Debug) {
                logger.info("sign found and updated");
            }
            Sign sign = (Sign) signPos.getBlock().getState();
            sign.setLine(2, "");
            sign.update();
        } else {
            logger.warning("Sign not found!");
        }
        signPos.subtract(-0.5, 0, -0.5);
        player.setWalkSpeed(0.2f);
        player.removePotionEffect(PotionEffectType.JUMP);
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent e) {
        demount(e.getPlayer(), e.getPlayer().getLocation());
    }

    public void sendMessage(Player p, String message) {
        p.sendMessage(ChatColor.GRAY + "[" + ChatColor.DARK_RED + "APTurrets" + ChatColor.GRAY + "] " + ChatColor.WHITE + message);
    }

    @EventHandler
    public void onPlayerDeath(PlayerDeathEvent e) {
        if (onTurrets.contains(e.getEntity().getKiller())) {
            e.setDeathMessage(e.getEntity().getDisplayName() + " was gunned down by " + e.getEntity().getKiller().getDisplayName() + ".");
        }
    }

    /**
     * Takes Ammo From Player's Inventory For Firing The Turret.
     *
     * @param player Player Who Is Having Ammo Thier Ammo Taken
     * @return Ammo Successfully Taken
     */
    public boolean takeAmmo(Player player) {
        if (takeFromChest) {
            Block signBlock = player.getLocation().getBlock();
            if (signBlock.getType() == Material.WALL_SIGN || signBlock.getType() == Material.SIGN_POST) {
                Sign s = (Sign) signBlock.getState();

                if (craftManager != null)
                {
                    Craft c = craftManager.getCraftByPlayer(player);
                    if (c != null) {
                        Inventory i = firstInventory(c, TURRETAMMO, Material.CHEST, Material.TRAPPED_CHEST);
                        if (i != null) {
                            i.removeItem(TURRETAMMO);
                            return true;
                        }
                    }
                }

                Block adjacentBlock = getBlockSignAttachedTo(signBlock);
                if (adjacentBlock.getState() instanceof InventoryHolder) {
                    InventoryHolder inventoryHolder = (InventoryHolder) adjacentBlock.getState();
                    if (inventoryHolder.getInventory().containsAtLeast(TURRETAMMO, 1)) {
                        inventoryHolder.getInventory().removeItem(TURRETAMMO);
                        return true;
                    }
                }
            }
        }

        if (takeFromInventory) {
            if (player.getInventory().containsAtLeast(TURRETAMMO, 1)) {
                player.getInventory().removeItem(TURRETAMMO);
                player.updateInventory();
                return true;
            }
        }
        return false;
    }

    /**
     * Checks The Block That Is Being Used As A Support. Eg: Block Sign Is Placed Against
     *
     * @param block Block That You Are Checking For Support
     * @return Block That Is Supporting
     */
    public static Block getBlockSignAttachedTo(Block block) {
        if (block.getType().equals(Material.SIGN_POST))
            return block.getRelative(BlockFace.DOWN);

        if (block.getType().equals(Material.WALL_SIGN))
            switch (block.getData()) {
                case 2:
                    return block.getRelative(BlockFace.SOUTH);
                case 3:
                    return block.getRelative(BlockFace.NORTH);
                case 4:
                    return block.getRelative(BlockFace.EAST);
                case 5:
                    return block.getRelative(BlockFace.WEST);
            }
        return null;
    }

    /**
     * Converts a Movecraft Location Object to a Bukkit Location Object
     *
     * @param movecraftLoc Movecraft Location Object
     * @param world        World The Craft Is In
     * @return Location Object
     */
    public static Location movecraftLocationToBukkitLocation(MovecraftLocation movecraftLoc, World world) {
        return new Location(world, movecraftLoc.getX(), movecraftLoc.getY(), movecraftLoc.getZ());
    }

    /**
     * Converts a list of movecraftLocation Object to a bukkit Location Object
     *
     * @param movecraftLocations the movecraftLocations to be converted
     * @param world              the world of the location
     * @return the converted location
     */
    public static ArrayList<Location> movecraftLocationToBukkitLocation(List<MovecraftLocation> movecraftLocations, World world) {
        ArrayList<Location> locations = new ArrayList<Location>();
        for (MovecraftLocation movecraftLoc : movecraftLocations) {
            locations.add(movecraftLocationToBukkitLocation(movecraftLoc, world));
        }
        return locations;
    }

    /**
     * Converts a list of movecraftLocation Object to a bukkit Location Object
     *
     * @param movecraftLocations the movecraftLocations to be converted
     * @param world              the world of the location
     * @return the converted location
     */
    public static ArrayList<Location> movecraftLocationToBukkitLocation(MovecraftLocation[] movecraftLocations, World world) {
        ArrayList<Location> locations = new ArrayList<Location>();
        for (MovecraftLocation movecraftLoc : movecraftLocations) {
            locations.add(movecraftLocationToBukkitLocation(movecraftLoc, world));
        }
        return locations;
    }

    public static MovecraftLocation bukkitLocationToMovecraftLocation(Location loc) {
        return new MovecraftLocation(loc.getBlockX(), loc.getBlockY(), loc.getBlockZ());
    }

    /**
     * Gets the first inventory of a lookup material type on a craft holding a specific item, returns null if none found
     * an input of null for item searches without checking inventory contents
     * an input of an ItemStack with type set to Material.AIR for searches for empty space in an inventory
     *
     * @param craft the craft to scan
     * @param item the item to look for during the scan
     * @param lookup the materials to compare against while scanning
     * @return the first inventory matching a lookup material on the craft
     */
    public static Inventory firstInventory(Craft craft, ItemStack item, Material... lookup){
        if(craft == null)
            throw new IllegalArgumentException("craft must not be null");

        for(Location loc : movecraftLocationToBukkitLocation(craft.getBlockList(),craft.getW()))
            for(Material m : lookup)
                if(loc.getBlock().getType() == m)
                {
                    Inventory inv = ((InventoryHolder)loc.getBlock().getState()).getInventory();
                    if(item==null)
                        return inv;
                    for(ItemStack i : inv)
                        if((item.getType()==Material.AIR  && (i==null || i.getType()==Material.AIR)) || (i!=null && i.isSimilar(item)))
                            return inv;
                }
        return null;
    }

    private Class<?> getNMSClass(String name) throws ClassNotFoundException {
        return Class.forName("net.minecraft.server." + serverVersion + "." + name);
    }

    private static class ArrowLaunchException extends RuntimeException {
        public ArrowLaunchException(String message, Throwable cause) {
            super(message, cause);
        }
    }
}

