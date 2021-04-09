package com.snowleopard1863.APTurrets;

import com.snowleopard1863.APTurrets.config.Config;
import com.snowleopard1863.APTurrets.exception.ArrowLaunchException;
import net.countercraft.movecraft.craft.CraftManager;
import net.countercraft.movecraft.worldguard.MovecraftWorldGuard;
import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.Sign;
import org.bukkit.entity.Arrow;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.util.Vector;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.HashSet;

import static com.snowleopard1863.APTurrets.TurretsMain.PREFIX;

public class TurretManager {
    private final HashSet<Player> onTurrets = new HashSet<>();
    private final HashSet<Player> reloading = new HashSet<>();

    public void disable() {
        for(Player p : onTurrets) {
            if(p == null)
                continue;

            demount(p, p.getLocation());
        }
        onTurrets.clear();
        reloading.clear();
    }

    public void mount(Player player, @NotNull Location signPos) {
        if (signPos.getBlock().getType() != Material.SIGN && signPos.getBlock().getType() != Material.SIGN_POST && signPos.getBlock().getType() != Material.WALL_SIGN)
            return;

        if (onTurrets.contains(player)) {
            // If the player tries to mount an already mounted turret, tell them no
            player.sendMessage(PREFIX + "You are already on a turret!");
            return;
        }

        Sign sign = (Sign) signPos.getBlock().getState();
        sign.setLine(2, player.getName());
        sign.update();
        onTurrets.add(player);
        signPos.add(0.5D, 0.0D, 0.5D);
        signPos.setDirection(player.getEyeLocation().getDirection());
        player.teleport(signPos);

        // Effects to add zoom and lock a player from jumping
        player.addPotionEffect(new PotionEffect(PotionEffectType.SLOW, 1000000, 6));
        player.addPotionEffect(new PotionEffect(PotionEffectType.JUMP, 1000000, 200));
    }

    public void demount(Player player, @Nullable Location signPos) {
        // Demounting the player from the turret
        onTurrets.remove(player);
        reloading.remove(player);

        if(signPos != null) {
            if (signPos.getBlock().getType() == Material.SIGN || signPos.getBlock().getType() == Material.SIGN_POST || signPos.getBlock().getType() == Material.WALL_SIGN) {
                Sign sign = (Sign) signPos.getBlock().getState();
                sign.setLine(2, "");
                sign.update();
            }
        }

        // Remove potion effects and set their walking speed back to normal
        player.removePotionEffect(PotionEffectType.JUMP);
        player.removePotionEffect(PotionEffectType.SLOW);
    }

    public void fire(Player player) {
        if(!onTurrets.contains(player))
            return;

        if(player.isGliding() || player.isFlying())
            demount(player, player.getLocation());

        startReloading(player);

        // do ammo taking
        if(Config.RequireAmmo) {
            if(!takeAmmo(player))
                // If they run out of ammo, don't let them fire and play the empty sound
                player.getWorld().playSound(player.getLocation(), Sound.BLOCK_DISPENSER_FAIL, 1.0F, 2.0F);
        }

        if(runRaycast(player))
            return;

        Arrow arrow = launchArrow(player);

        if(Config.UseParticleTracers)
            TurretsMain.getInstance().getTracerManager().startTracing(arrow);
        else
            arrow.setCritical(true);

        World world = player.getWorld();
        world.playSound(player.getLocation(), Sound.ENTITY_FIREWORK_BLAST, 1.0F, 2.0F);
        world.playEffect(player.getLocation(), Effect.MOBSPAWNER_FLAMES, 0);
    }


    @NotNull
    private Arrow launchArrow(Player player) {
        Arrow arrow;
        try {
            Object nmsPlayer = player.getClass().getMethod("getHandle").invoke(player);
            Object nmsWorld = nmsPlayer.getClass().getMethod("getWorld").invoke(nmsPlayer);
            Object nmsArrow = TurretsMain.getInstance().getNMSUtils().getNMSClass("EntityTippedArrow").getConstructor(TurretsMain.getInstance().getNMSUtils().getNMSClass("World"), TurretsMain.getInstance().getNMSUtils().getNMSClass("EntityLiving")).newInstance(nmsWorld, nmsPlayer);
            nmsArrow.getClass().getMethod("setNoGravity", boolean.class).invoke(nmsArrow, true);
            nmsWorld.getClass().getMethod("addEntity", TurretsMain.getInstance().getNMSUtils().getNMSClass("Entity")).invoke(nmsWorld, nmsArrow);
            arrow = (Arrow) nmsArrow.getClass().getMethod("getBukkitEntity").invoke(nmsArrow);
        }
        catch (Throwable e) {
            throw new ArrowLaunchException("Something went wrong when trying to launch an arrow", e);
        }

        arrow.setShooter(player);
        Location offset = player.getLocation().add(player.getLocation().getDirection().multiply(4));
        arrow.setVelocity(offset.getDirection().multiply(Config.ArrowVelocity));
        arrow.setBounce(false);
        arrow.setMetadata("isTurretBullet", new FixedMetadataValue(TurretsMain.getInstance(), true));
        arrow.setKnockbackStrength(Config.KnockbackStrength);
        double rand = Math.random();
        if (rand <= Config.IncindiaryChance)
            arrow.setFireTicks(500);

        return arrow;
    }

    private boolean takeAmmo(Player player) {
        if(Config.TakeFromChest && CraftManager.getInstance() != null) {
            Block signBlock = player.getLocation().getBlock();
            if (signBlock.getType() == Material.WALL_SIGN || signBlock.getType() == Material.SIGN_POST) {
                Block adjacentBlock = getBlockSignAttachedTo(signBlock);
                if(adjacentBlock instanceof InventoryHolder) {
                    Inventory i = ((InventoryHolder) adjacentBlock.getState()).getInventory();
                    if(i.containsAtLeast(new ItemStack(Config.TurretAmmo), 1)) {
                        i.remove(Config.TurretAmmo);
                        return true;
                    }
                }
            }
        }
        if(Config.TakeFromInventory) {
            if(player.getInventory().containsAtLeast(Config.TurretAmmo, 1)) {
                player.getInventory().removeItem(Config.TurretAmmo);
                player.updateInventory();
                return true;
            }
        }
        return false;
    }

    private void startReloading(final Player player) {
        reloading.add(player);
        Bukkit.getScheduler().scheduleSyncDelayedTask(TurretsMain.getInstance(), new Runnable() {
            @Override
            public void run() {
                reloading.remove(player);
            }
        }, ((int) (Config.DelayBetweenShots * 10.0)));
    }

    private boolean runRaycast(@NotNull Player shooter) {
        Location shooterLoc = shooter.getLocation();
        Vector shooterVector = shooterLoc.getDirection();

        for(Player p : Bukkit.getOnlinePlayers()) {
            if(p == null || !p.isOnline() || p == shooter || p.getWorld() != shooter.getWorld())
                continue;

            PlayerInventory inv = p.getInventory();
            if(inv == null)
                continue;

           ItemStack chestplate = inv.getChestplate();
            if(chestplate == null || chestplate.getType() != Material.ELYTRA)
                continue;

            Vector v = p.getLocation().subtract(shooterLoc).toVector();
            if(v.angle(shooterVector) > Config.RaycastRadians)
                continue;

            double distSquared = p.getLocation().distanceSquared(shooterLoc);
            if(distSquared > Config.RaycastRange * Config.RaycastRange)
                continue;

            Block targetBlock = shooter.getTargetBlock(null, Config.RaycastRange);
            if(targetBlock.getLocation().distanceSquared(shooterLoc) < distSquared)
                continue;

            if(!MovecraftWorldGuard.getInstance().getWGUtils().isPVPAllowed(p.getLocation()))
                continue;

            // Time to hit them!
            if(Config.RaycastBreakElytra)
                chestplate.setDurability((short) 431);

            p.setGliding(false);
            p.setSprinting(false);
            p.damage(Config.Damage, shooter);

            World world = shooter.getWorld();
            world.playSound(shooter.getLocation(), Sound.ENTITY_BLAZE_HURT, 1.5F, 2.0F);
            world.playEffect(shooter.getLocation(), Effect.MOBSPAWNER_FLAMES, 0);

            return true;
        }
        return false;
    }

    public boolean isOnTurret(Player p) {
        return onTurrets.contains(p);
    }

    public boolean isReloading(Player p) {
        return reloading.contains(p);
    }


    @Nullable
    public static Block getBlockSignAttachedTo(@NotNull Block block) {
        // Get the block the sign is attached to
        if (block.getType().equals(Material.SIGN_POST)) {
            return block.getRelative(BlockFace.DOWN);
        }
        else {
            if (block.getType().equals(Material.WALL_SIGN)) {
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
            }

            return null;
        }
    }
}
