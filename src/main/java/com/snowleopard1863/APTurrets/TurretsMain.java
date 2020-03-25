// Original Code by snowleopard1863
// Most recent updates by Foxtrot2400, TylerS1066 and cccm5
// AP Turrets Plugin, AKA mounted guns

package com.snowleopard1863.APTurrets;

// Imports
import com.sk89q.worldguard.bukkit.WGBukkit;
import com.sk89q.worldguard.protection.managers.RegionManager;
import java.util.*;
import java.util.logging.Logger;
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
import org.bukkit.event.player.*;
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

public final class TurretsMain extends JavaPlugin implements Listener {
    private PluginDescriptionFile pdfile = this.getDescription();
    private Logger logger = Logger.getLogger("Minecraft");
    private List<Player> onTurrets = new ArrayList();
    private List<Player> reloading = new ArrayList();
    private List<Arrow> tracedArrows = new ArrayList();
    private HashMap<String, Location> loc = new HashMap<String, Location>();
    private Boolean Debug = false;
    private FileConfiguration config = this.getConfig();
    private boolean takeFromInventory; // Whether to take arrows from a player's inventory
    private boolean takeFromChest;     // Same as above, except for chests
    private boolean requireAmmo;       // Whether to require ammo at all
    private double costToPlace;        // Cost to place a turret
    private double damage;             // How much damage this should deal
    private double incindiaryChance;   // Chance to incinerate your enemies
    private double arrowVelocity;      // Velocity for arrows to go
    private int knockbackStrength;     // Knockback strength of the arrows
    private boolean useParticleTracers;// Whether to use tracers
    private double delayBetweenShots;  // Delay between each shot
    private static Economy economy;
    private static CraftManager craftManager;

    private String serverVersion;
    private static final Material[] INVENTORY_MATERIALS;
    private final ItemStack TURRETAMMO;

    public TurretsMain() {
        this.TURRETAMMO = new ItemStack(Material.ARROW, 1);
    }

    public void onEnable() {
        String packageName = getServer().getClass().getPackage().getName();
        serverVersion = packageName.substring(packageName.lastIndexOf(".") + 1);
        // Runs when the plugin starts
        this.logger.info(this.pdfile.getName() + " v" + this.pdfile.getVersion() + " has been enabled.");
        this.getServer().getPluginManager().registerEvents(this, this);
        // Set up our configuration file
        this.config.addDefault("Debug mode", false);
        this.config.addDefault("Cost to Place", 15000.0D);
        this.config.addDefault("Take arrows from inventory", true);
        this.config.addDefault("Take arrows from chest", true);
        this.config.addDefault("Require Ammo", true);
        this.config.addDefault("Damage per arrow", 2.5D);
        this.config.addDefault("Incindiary chance", 0.1D);
        this.config.addDefault("Knockback strength", 2);
        this.config.addDefault("Arrow velocity", 4.0D);
        this.config.addDefault("Particle tracers", true);
        this.config.addDefault("Delay between shots", 0.2D);
        this.config.options().copyDefaults(true);
        this.saveConfig();
        // Pull all of our configs from the configuration file
        this.Debug = this.getConfig().getBoolean("Debug mode");
        this.takeFromChest = this.getConfig().getBoolean("Take arrows from chest");
        this.takeFromInventory = this.getConfig().getBoolean("Take arrows from inventory");
        this.costToPlace = this.getConfig().getDouble("Cost to Place");
        this.requireAmmo = this.getConfig().getBoolean("Require Ammo");
        this.knockbackStrength = this.getConfig().getInt("Knockback strength");
        this.incindiaryChance = this.getConfig().getDouble("Incindiary chance");
        this.damage = this.getConfig().getDouble("Damage per arrow");
        this.arrowVelocity = this.getConfig().getDouble("Arrow velocity");
        this.useParticleTracers = this.getConfig().getBoolean("Particle tracers");
        this.delayBetweenShots = this.getConfig().getDouble("Delay between shots");
        // If we find vault, use it. Otherwise, no vault integration.
        if (this.getServer().getPluginManager().getPlugin("Vault") != null) {
            RegisteredServiceProvider<Economy> rsp = this.getServer().getServicesManager().getRegistration(Economy.class);
            if (rsp != null) {
                economy = (Economy)rsp.getProvider();
                this.logger.info("Found a compatible Vault plugin.");
            } else {
                this.logger.info("[WARNING] Could not find compatible Vault plugin. Disabling Vault integration.");
                economy = null;
            }
        } else {
            this.logger.info("Could not find compatible Vault plugin. Disabling Vault integration.");
            economy = null;
        }

        // Same deal with Movecraft. If we can find it, use it.
        if (this.getServer().getPluginManager().getPlugin("Movecraft") != null) {
            craftManager = CraftManager.getInstance();
            this.logger.info("Compatible Version Of Movecraft Found.");
        } else {
            this.logger.info("[WARNING] Could not find compatible Movecraft Version... Disabling");
            craftManager = null;
        }
        // If we want to use tracers, this sets it up for us
        if (this.useParticleTracers) {
            this.getServer().getScheduler().scheduleSyncRepeatingTask(this, new Runnable() {
                public void run() {
                    Iterator iterator = TurretsMain.this.tracedArrows.iterator();

                    while(iterator.hasNext()) {
                        Arrow a = (Arrow)iterator.next();
                        if (a.isOnGround() || a.isDead() || a.getTicksLived() > 100) {
                            a.removeMetadata("tracer", TurretsMain.this);
                            iterator.remove();
                            a.remove();
                        }

                        World world = a.getWorld();
                        world.spawnParticle(Particle.CRIT, a.getLocation(), 1, 0.0D, 0.0D, 0.0D, 0.0D);

                    }

                }
            }, 0L, 0L);
        }

    }

    public void onLoad() {
        super.onLoad();
        this.logger = this.getLogger();
    }

    public void onDisable() {
        // Runs whenever the plugin is disabled
        Iterator var1 = this.onTurrets.iterator();

        while(var1.hasNext()) { // This loops through and kicks all players off their turrets so they aren't just...stuck
            Player player = (Player)var1.next();
            this.demount(player, player.getLocation());
            this.onTurrets.remove(player);
        }

        this.reloading.clear();
        this.tracedArrows.clear();
        this.logger.info(this.pdfile.getName() + " v" + this.pdfile.getVersion() + " has been disabled.");
    }

    @EventHandler
    public void onClick(PlayerInteractEvent event) {
        // Runs whenever the player clicks
        if (event.getAction() == Action.RIGHT_CLICK_AIR || event.getAction() == Action.RIGHT_CLICK_BLOCK) {
            if (this.Debug) {
                this.logger.info(event.getPlayer() + " has right clicked");
            }

            Player player = event.getPlayer();
            if (this.onTurrets.contains(player) && player.hasPermission("ap-turrets.use")) {  // If the player is allowed,
                if (this.Debug) {
                    this.logger.info(event.getPlayer() + " is on a turret");
                }

                if (player.getInventory().getItemInMainHand().getType() == Material.MILK_BUCKET || player.getInventory().getItemInOffHand().getType() == Material.MILK_BUCKET) {
                    if (this.Debug) {
                        this.logger.info(event.getPlayer() + " has right clicked a milk bucket!");
                    }
                    // If the player tries to use milk to clear the effects, cancel the event to keep that from happening
                    event.setCancelled(true);
                }

                if (player.getInventory().getItemInMainHand().getType() == Material.STONE_BUTTON || player.getInventory().getItemInOffHand().getType() == Material.STONE_BUTTON) {
                    if (this.reloading.contains(player)) {
                        return;
                    }
                    // Fires the turret and keeps them from interacting with something else and placing the button accidentally
                    this.fireTurret(player);
                    event.setCancelled(true);
                    if (this.Debug) {
                        this.logger.info(event.getPlayer() + " has started to shoot");
                    }
                }
            }
        }

        Sign sign;
        if (event.getAction() == Action.RIGHT_CLICK_BLOCK) {
            if (this.Debug) {
                this.logger.info("A block has been right clicked");
            }

            if (event.getClickedBlock().getType() == Material.SIGN_POST || event.getClickedBlock().getType() == Material.WALL_SIGN || event.getClickedBlock().getType() == Material.SIGN) {

                if (this.Debug) {
                    this.logger.info("A sign was clicked");
                }

                sign = (Sign)event.getClickedBlock().getState();
                if ("Mounted".equalsIgnoreCase(sign.getLine(0)) && "Gun".equalsIgnoreCase(sign.getLine(1))) {
                    // If the player clicks a sign post, and that sign post says "Mounted Gun" on it, mount them to the turret
                    if (this.Debug) {
                        this.logger.info("A Mounted Gun sign has been clicked");
                    }

                    Block b = sign.getLocation().subtract(0.0D, 1.0D, 0.0D).getBlock();
                    if (b.getType() != Material.SLIME_BLOCK) {
                        Location signPos = event.getClickedBlock().getLocation();
                        signPos.setPitch(event.getPlayer().getLocation().getPitch());
                        signPos.setDirection(event.getPlayer().getVelocity());
                        this.mount(event.getPlayer(), signPos);
                    }
                }
            }
        }

        if (event.getAction() == Action.LEFT_CLICK_BLOCK && event.getPlayer().getInventory().getItemInMainHand().getType() == Material.STONE_BUTTON) {
            // If a player left clicks the mounted gun with a stone button in hand, show them the statistics for the plugin (Damage, Knockback, Velocity, etc.)
            if (this.Debug) {
                this.logger.info("A block has been left clicked while holding a button");
            }

            if (event.getClickedBlock().getType() == Material.SIGN_POST || event.getClickedBlock().getType() == Material.WALL_SIGN || event.getClickedBlock().getType() == Material.SIGN) {
                if (this.Debug) {
                    this.logger.info("A sign was left clicked");
                }

                sign = (Sign)event.getClickedBlock().getState();
                if ("Mounted".equalsIgnoreCase(sign.getLine(0)) && "Gun".equalsIgnoreCase(sign.getLine(1))) {
                    if (this.Debug) {
                        this.logger.info("A Mounted Gun sign has been left clicked");
                    }

                    event.setCancelled(true);
                    if (!sign.getLine(3).equals("")) {
                        this.sendMessage(event.getPlayer(), "\n" + ChatColor.GOLD + "Type: " + ChatColor.BLACK + sign.getLine(3) + "\n" + ChatColor.GOLD + "Damage/Shot: " + ChatColor.GRAY + this.damage + "\n" + ChatColor.GOLD + "Delay Between Shots: " + ChatColor.GRAY + this.delayBetweenShots + "\n" + ChatColor.GOLD + "Velocity: " + ChatColor.GRAY + this.arrowVelocity + "\n" + ChatColor.GOLD + "Fire Chance: " + ChatColor.GRAY + this.incindiaryChance * 100.0D + "%\n" + ChatColor.GOLD + "Knockback: " + ChatColor.GRAY + this.knockbackStrength + "\n" + ChatColor.GOLD + "Cost to Place: " + ChatColor.GRAY + "$" + this.costToPlace);
                    } else {
                        this.sendMessage(event.getPlayer(), "\n" + ChatColor.GOLD + "Damage/Shot: " + ChatColor.GRAY + this.damage + "\n" + ChatColor.GOLD + "Delay Between Shots: " + ChatColor.GRAY + this.delayBetweenShots + "\n" + ChatColor.GOLD + "Velocity: " + ChatColor.GRAY + this.arrowVelocity + "\n" + ChatColor.GOLD + "Fire Chance: " + ChatColor.GRAY + this.incindiaryChance * 100.0D + "%\n" + ChatColor.GOLD + "Knockback: " + ChatColor.GRAY + this.knockbackStrength + "\n" + ChatColor.GOLD + "Cost to Place: $" + ChatColor.GRAY + this.costToPlace);
                    }
                }
            }
        }

    }

    @EventHandler
    public void onEntityInteract(PlayerInteractEntityEvent e) {
        Player p = e.getPlayer();
        if (this.onTurrets.contains(p) && (e.getRightClicked() instanceof Boat || e.getRightClicked() instanceof Horse)) {
            // If someone tries to hop on a horse while already on a gun, demount them from the gun
            this.demount(p, p.getLocation());
            if (this.Debug) {
                this.logger.info("Player: " + p.getName() + "Has Mounted An Entity.");
            }
        }

    }

    @EventHandler
    public void eventSignChanged(SignChangeEvent event) {
        Player player = event.getPlayer();
        Plugin wg = this.getServer().getPluginManager().getPlugin("WorldGuard");
        Location location = player.getLocation();
        RegionManager rm = WGBukkit.getRegionManager(player.getWorld());
        if ("Mounted".equalsIgnoreCase(event.getLine(0)) && "Gun".equalsIgnoreCase(event.getLine(1))) {
            if (rm.getApplicableRegions(location).size() <= 0 && !player.hasPermission("ap-turrets.regionoverride")) {
                // If the player can override regions, place the turret. Otherwise, require them to be in a region
                this.sendMessage(player, "You must be inside a airspace or region.");
                event.setCancelled(true);
                if (this.Debug) {
                    this.logger.info("A Mounted Gun sign failed to place");
                }

                return;
            }

            if (player.hasPermission("ap-turrets.place")) {
                // If they're allowed to place a turret, notify them that they have placed a turret and take money from their account
                if (economy != null) {
                    if (economy.has(player, this.costToPlace)) {
                        economy.withdrawPlayer(player, costToPlace);
                        player.sendMessage(ChatColor.AQUA + "[" + ChatColor.RED + "Mounted Gun" + ChatColor.AQUA + "] " + ChatColor.GOLD + "Mounted Gun Placed!" + ChatColor.GREEN + " $15,000 has been charged to your balance.");
                        event.setLine(0, "Mounted");
                        event.setLine(1, "Gun");
                        if (this.Debug) {
                            this.logger.info("A Mounted Gun sign has been place");
                        }
                    } else {
                        if (this.Debug) {
                            this.logger.info("A Mounted Gun sign failed to place");
                        }

                        this.sendMessage(player, "You Don't Have Enough Money To Place A Turret. Cost To Place: " + ChatColor.RED + this.costToPlace);
                    }
                } else {
                    this.sendMessage(player, "Turret Created!");
                    if (this.Debug) {
                        this.logger.info("A Mounted Gun sign has been placed for free due to no vault instalation");
                    }
                }
            } else {
                if (this.Debug) {
                    this.logger.info("A Mounted Gun sign failed to place");
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
            }, (long)((int)(this.delayBetweenShots * 10.0D)));
            boolean hasAmmoBeenTaken = !this.requireAmmo || this.takeAmmo(player);
            if (!hasAmmoBeenTaken) {
                // If they run out of ammo, don't let them fire and play the empty sound
                player.getWorld().playSound(player.getLocation(), Sound.BLOCK_DISPENSER_FAIL, 1.0F, 2.0F);
                if (this.Debug) {
                    this.logger.info(player + " is out of ammo");
                }

            } else {
                // Otherwise, fire the gun and launch the arrow of death at their target
                Arrow arrow = this.launchArrow(player);
                arrow.setShooter(player);
                Location offsetLocation = player.getLocation().add(player.getLocation().getDirection().multiply(4)); // Just in front of the player
                arrow.setVelocity(offsetLocation.getDirection().multiply(this.arrowVelocity));
                arrow.setBounce(false);
                arrow.setMetadata("isTurretBullet", new FixedMetadataValue(this, true));
                arrow.setKnockbackStrength(this.knockbackStrength);
                double rand = Math.random();
                if (rand <= this.incindiaryChance) {
                    arrow.setFireTicks(500);
                }

                if (this.useParticleTracers) {
                    arrow.setMetadata("tracer", new FixedMetadataValue(this, true));
                    this.tracedArrows.add(arrow);
                    arrow.setCritical(false);
                    //PacketPlayOutEntityDestroy packet = new PacketPlayOutEntityDestroy(new int[]{arrow.getEntityId()});
                    Iterator var7 = this.getServer().getOnlinePlayers().iterator();

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

                if (this.Debug) {
                    this.logger.info("Mounted Gun Fired.");
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
    public void onPlayerToggleSneakEvent(PlayerToggleSneakEvent event) {
        // If the player sneaks, demount them from the turret
        Player player = event.getPlayer();
        if (this.Debug) {
            this.logger.info(player + " sneaked");
        }

        if (player.isSneaking() && this.onTurrets.contains(player)) {
            this.demount(player, player.getLocation());
            if (this.Debug) {
                this.logger.info(player + " got out of their turret");
            }
        }

    }

    @EventHandler
    public void onHit(ProjectileHitEvent event) {
        // If a bullet hits, play a sound for it and add effects
        if (event.getEntity() instanceof Arrow) {
            Arrow arrow = (Arrow)event.getEntity();
            if (arrow.hasMetadata("isTurretBullet")) {
                if (this.Debug) {
                    this.logger.info("A bullet has landed");
                }

                Location arrowLoc = arrow.getLocation();
                World world = event.getEntity().getWorld();
                Location l = arrowLoc.getBlock().getLocation();
                arrow.getWorld().playEffect(l, Effect.STEP_SOUND, world.getBlockTypeIdAt(l));
                world.playEffect(l, Effect.TILE_BREAK, l.subtract(0.0D, 1.0D, 0.0D).getBlock().getTypeId(), 0);
            }
        }

    }

    @EventHandler
    public void onEntityDamageEvent(EntityDamageEvent event) {
        // If a player gets hit while on a turret, demount them from the turret. If it's a player, stop them from gliding and/or sprinting
        if (this.Debug) {
            this.logger.info("An entity was damaged");
        }

        if (event.getEntity() instanceof Player) {
            if (this.Debug) {
                this.logger.info("It was a player");
            }

            Player player = (Player)event.getEntity();
            if (this.onTurrets.contains(player)) {
                if (this.Debug) {
                    this.logger.info("on a turret");
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
            event.setDamage(this.damage);
            if (this.Debug) {
                Arrow a = (Arrow)event.getDamager();
                Player shooter = (Player)a.getShooter();
                this.logger.info(event.getEntity() + " was shot by " + shooter.getName() + " for " + event.getDamage() + " It should be doing " + this.damage);
            }
        }

    }

    public void mount(Player player, Location signPos) {
        if (signPos.getBlock().getType() != Material.SIGN && signPos.getBlock().getType() != Material.SIGN_POST && signPos.getBlock().getType() != Material.WALL_SIGN) {
            this.logger.warning("Sign not found!");
        } else {
            if (this.Debug) {
                this.logger.info("Sign detected");
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
                if (this.Debug) {
                    this.logger.info("1 player per turret");
                }
            } else {
                if (this.Debug) {
                    this.logger.info(player.getName() + " is now on a turret");
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
        if (this.Debug) {
            this.logger.info(player.getName() + " is being taken off a turret");
        }

        this.onTurrets.remove(player);
        this.reloading.remove(player);
        if (signPos.getBlock().getType() != Material.SIGN && signPos.getBlock().getType() != Material.SIGN_POST && signPos.getBlock().getType() != Material.WALL_SIGN) {
            this.logger.warning("Sign not found!");
        } else {
            if (this.Debug) {
                this.logger.info("sign found and updated");
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
        if (this.takeFromChest) {
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

        if (this.takeFromInventory && player.getInventory().containsAtLeast(this.TURRETAMMO, 1)) {
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

    public static Location movecraftLocationToBukkitLocation(MovecraftLocation movecraftLoc, World world) {
        // Location converter
        return new Location(world, (double)movecraftLoc.getX(), (double)movecraftLoc.getY(), (double)movecraftLoc.getZ());
    }

    public static ArrayList<Location> movecraftLocationToBukkitLocation(List<MovecraftLocation> movecraftLocations, World world) {
        ArrayList<Location> locations = new ArrayList();
        Iterator var3 = movecraftLocations.iterator();

        while(var3.hasNext()) {
            MovecraftLocation movecraftLoc = (MovecraftLocation)var3.next();
            locations.add(movecraftLocationToBukkitLocation(movecraftLoc, world));
        }

        return locations;
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

    private static class ArrowLaunchException extends RuntimeException {
        public ArrowLaunchException(String message, Throwable cause) {
            super(message, cause);
        }
    }
    static {
        INVENTORY_MATERIALS = new Material[]{Material.CHEST, Material.TRAPPED_CHEST, Material.FURNACE, Material.HOPPER, Material.DROPPER, Material.DISPENSER, Material.BREWING_STAND};
    }
}
