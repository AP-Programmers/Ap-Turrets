package com.snowleopard1863.APTurrets;

import com.sk89q.worldguard.protection.flags.Flags;
import com.snowleopard1863.APTurrets.config.Config;
import com.snowleopard1863.APTurrets.exception.ArrowLaunchException;
import net.countercraft.movecraft.craft.CraftManager;
import net.countercraft.movecraft.worldguard.MovecraftWorldGuard;
import net.countercraft.movecraft.worldguard.utils.WorldGuardUtils.State;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import org.bukkit.Bukkit;
import org.bukkit.Effect;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.Tag;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.Sign;
import org.bukkit.block.data.BlockData;
import org.bukkit.block.data.Directional;
import org.bukkit.block.data.Rotatable;
import org.bukkit.entity.Arrow;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.inventory.meta.Damageable;
import org.bukkit.inventory.meta.ItemMeta;
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
        for (Player p : onTurrets) {
            if (p == null)
                continue;

            demount(p, p.getLocation());
        }
        onTurrets.clear();
        reloading.clear();
    }

    public void mount(Player player, @NotNull Location signPos) {
        if (!Tag.SIGNS.isTagged(signPos.getBlock().getType()))
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

        if (signPos != null && Tag.SIGNS.isTagged(signPos.getBlock().getType())) {
            Sign sign = (Sign) signPos.getBlock().getState();
            sign.setLine(2, "");
            sign.update();
        }

        // Remove potion effects and set their walking speed back to normal
        player.removePotionEffect(PotionEffectType.JUMP);
        player.removePotionEffect(PotionEffectType.SLOW);
    }

    public void fire(Player player) {
        if (!onTurrets.contains(player))
            return;

        if (player.isGliding() || player.isFlying()) {
            demount(player, player.getLocation());
            return;
        }

        startReloading(player);

        if (Config.RequireAmmo && !takeAmmo(player)) {
            // If they run out of ammo, don't let them fire and play the empty sound
            player.getWorld().playSound(player.getLocation(), Sound.BLOCK_DISPENSER_FAIL, 1.0F, 2.0F);
            return;
        }

        // Run a raycast, if that fails fire normally
        if (runRaycast(player))
            return;

        Arrow arrow = launchArrow(player);
        arrow.setCritical(true);

        World world = player.getWorld();
        world.playSound(player.getLocation(), Sound.ENTITY_FIREWORK_ROCKET_BLAST, 1.0F, 2.0F);
        world.playEffect(player.getLocation(), Effect.MOBSPAWNER_FLAMES, 0);
    }

    @NotNull
    private Arrow launchArrow(Player player) {
        Arrow arrow;
        try {
            ServerPlayer nmsPlayer = (ServerPlayer) player.getClass().getMethod("getHandle").invoke(player);
            ServerLevel nmsWorld = nmsPlayer.getLevel();
            net.minecraft.world.entity.projectile.Arrow nmsArrow = new net.minecraft.world.entity.projectile.Arrow(nmsWorld, nmsPlayer);
            nmsArrow.setNoGravity(true);
            nmsWorld.addFreshEntity(nmsArrow);
            arrow = (Arrow) nmsArrow.getBukkitEntity();
        } catch (Exception e) {
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
        if (Config.TakeFromChest && CraftManager.getInstance() != null) {
            Block signBlock = player.getLocation().getBlock();
            if (Tag.SIGNS.isTagged(signBlock.getType())) {
                Block adjacentBlock = getBlockSignAttachedTo(signBlock);
                if (adjacentBlock instanceof InventoryHolder) {
                    Inventory i = ((InventoryHolder) adjacentBlock.getState()).getInventory();
                    if (i.containsAtLeast(new ItemStack(Config.TurretAmmo), 1)) {
                        i.remove(Config.TurretAmmo);
                        return true;
                    }
                }
            }
        }
        if (Config.TakeFromInventory && player.getInventory().containsAtLeast(Config.TurretAmmo, 1)) {
            player.getInventory().removeItem(Config.TurretAmmo);
            player.updateInventory();
            return true;
        }
        return false;
    }

    private void startReloading(final Player player) {
        reloading.add(player);
        Bukkit.getScheduler().scheduleSyncDelayedTask(TurretsMain.getInstance(), () -> reloading.remove(player),
                ((int) (Config.DelayBetweenShots * 10.0)));
    }

    private boolean runRaycast(@NotNull Player shooter) {
        Location shooterLoc = shooter.getLocation();
        Vector shooterVector = shooterLoc.getDirection();

        for (Player p : Bukkit.getOnlinePlayers()) {
            if (p == null || !p.isOnline() || p == shooter || p.getWorld() != shooter.getWorld())
                continue;

            // Check for elytra
            PlayerInventory inv = p.getInventory();
            if (inv == null)
                continue;
            ItemStack chestplate = inv.getChestplate();
            if (chestplate == null || chestplate.getType() != Material.ELYTRA)
                continue;

            // Check for angle
            Vector v = p.getLocation().subtract(shooterLoc).toVector();
            if (v.angle(shooterVector) > Config.RaycastRadians)
                continue;

            // Check for distance
            double distSquared = p.getLocation().distanceSquared(shooterLoc);
            if (distSquared > Config.RaycastRange * Config.RaycastRange)
                continue;

            // Check for WG PVP flag
            if (MovecraftWorldGuard.getInstance().getWGUtils().getState(null, p.getLocation(), Flags.PVP) == State.DENY)
                continue;

            // Check for block directly between
            Block targetBlock = shooter.getTargetBlock(null, Config.RaycastRange);
            if (targetBlock.getLocation().distanceSquared(shooterLoc) < distSquared)
                continue;

            // Time to hit them!
            if (Config.RaycastBreakElytra) {
                ItemMeta meta = chestplate.getItemMeta();
                if (meta instanceof Damageable) {
                    Damageable damageable = (Damageable) meta;
                    damageable.setDamage(431);
                    chestplate.setItemMeta((ItemMeta) damageable);
                }
            }

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
        BlockData data = block.getState().getBlockData();
        if (data instanceof Rotatable) {
            return block.getRelative(((Rotatable) data).getRotation());
        }
        if (data instanceof Directional) {
            return block.getRelative(((Directional) data).getFacing());
        }
        return null;
    }
}
