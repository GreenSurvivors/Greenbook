package de.greensurvivors.greenbook.utils;

import de.greensurvivors.greenbook.GreenLogger;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Tag;
import org.bukkit.World;
import org.bukkit.entity.Entity;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Set;
import java.util.logging.Level;

/**
 * general util for handling location problems
 * currently used by lift-listener to find a safe location to teleport to
 */
public final class LocationUtil {
    // Water types used for TRANSPARENT_MATERIALS and is-water-safe config option
    // I have no idea how Tag.FLUIDS_WATER works.
    private static final Set<Material> WATER_TYPES = Set.of(Material.WATER, Material.LEGACY_WATER, Material.LEGACY_STATIONARY_WATER);
    // Types checked by isBlockDamaging
    private static final Set<Material> DAMAGING_TYPES = Set.of(Material.CACTUS, Material.FIRE, Material.SOUL_FIRE, Material.CAMPFIRE, Material.SOUL_CAMPFIRE, Material.SWEET_BERRY_BUSH, Material.WITHER_ROSE);
    // I have no idea how Tag.FLUIDS_LAVA works.
    private static final Set<Material> LAVA_TYPES = Set.of(Material.LAVA, Material.LEGACY_LAVA, Material.LEGACY_STATIONARY_LAVA);

    private static boolean isWaterSafe = false;

    public static void setIsWaterSafe(final boolean newIsWaterSafe) { //todo config
        isWaterSafe = newIsWaterSafe;
    }

    private static boolean isEntitySafeAt(final @NotNull Entity toTest, final @NotNull World world, final double x, final double y, final double z) {
        //start looking for unsafe blocks one Block under the entity.
        final int minY = Math.max(world.getMinHeight(), (int)y-1);
        //end looking at the height of the entity
        final int maxY = Math.min(world.getMaxHeight(), (int)(y + toTest.getBoundingBox().getHeight()));

        //test for unsafe blocks at the new postion
        for (int i = minY; i <= maxY; i++){
            Material material = world.getType((int)x, i, (int)z);

            if (Tag.PORTALS.isTagged(material)) {
                return false;
            }

            if (DAMAGING_TYPES.contains(material) || LAVA_TYPES.contains(material) || Tag.BEDS.isTagged(material)) {
                return false;
            }

            if (!isWaterSafe && WATER_TYPES.contains(material)){
                return false;
            }
        }

        //test if there is enough room but also a floor to stand on

        return !toTest.collidesAt(new Location(world, x, y, z)) && toTest.collidesAt(new Location(world, x, y-1, z)) ;
    }


    //might be null if no location was found
    /**
     * try to get a safe location to teleport to, up to 5 blocks difference
     * @param toTest player to try to get the Destination for
     * @param dy the vertical distance counting form the postion of the Player toTest. negative means downwards.
     * @return safe (no damage will be taken) location to teleport to, will be null, if no was found
     */
    public static @Nullable Location getSafeLiftDestination(final @NotNull Entity toTest, double dy) {
        //the end location we try to get a safe location around
        final Location toLoc = toTest.getLocation().add(0, dy, 0);

        if (toLoc.getWorld() == null) {
            GreenLogger.log(Level.WARNING, "couldn't find safe destination for null.");
            return null;
        }

        final World world = toLoc.getWorld();
        final double x = toLoc.getX();
        int y = toLoc.getBlockY();
        //y to start from, is imported to not teleport to the same y level
        int originY = toTest.getLocation().getBlockY();
        final double z = toLoc.getZ();

        int maxY = Math.min(toLoc.getBlockY() + 5, world.getMaxHeight()+1);
        int minY = Math.max(toLoc.getBlockY() - 5, world.getMinHeight());

        //if the from-location gets over or under the end-location into play
        if(dy > 0){ //never teleport to the same floor the origin lift is on
            minY = Math.max(minY, originY+1);
        } else {
            maxY = Math.min(maxY, originY -1);
        }

        boolean foundSafeLoc = false;

        if (isEntitySafeAt(toTest, world, x, y, z)){ //this is purly optimizing, starting with i = 0 would have the same effect, but we would check the same location twice.
            foundSafeLoc = true;
        }  else {
            for (int i = 1; i <= 5; i++){ //todo there is probably optimizing to be had here, since we are checking if a block was safe at least double.
                if (y + i <= maxY && //never teleport to the same floor the origin lift is on
                        isEntitySafeAt(toTest, world, x, y+i, z)){ //check if the new location + i blocks is safe to stand on
                    foundSafeLoc = true;
                    y += i;
                    break;
                }

                if (y - i >= minY && //never teleport to the same floor the origin lift is on
                        isEntitySafeAt(toTest, world, x, y-i, z)){ //check if the new location - i blocks is safe to stand on
                    foundSafeLoc = true;
                    y -= i;
                    break;
                }
            }
        }

        //no location where found.
        if (!foundSafeLoc){
            return null;
        }

        return new Location(world, x,  y, z, toLoc.getYaw(), toLoc.getPitch());
    }
}
