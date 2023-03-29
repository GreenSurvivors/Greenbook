package de.greensurvivors.greenbook.utils2;

import de.greensurvivors.greenbook.GreenLogger;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Tag;
import org.bukkit.World;
import org.jetbrains.annotations.Nullable;

import java.util.EnumSet;
import java.util.HashSet;
import java.util.Set;
import java.util.logging.Level;

public final class LocationUtil {
    // Water types used for TRANSPARENT_MATERIALS and is-water-safe config option
    private static final Set<Material> WATER_TYPES = getAllMatchingMaterials("FLOWING_WATER", "WATER");
    // Types checked by isBlockDamaging
    private static final Set<Material> DAMAGING_TYPES = getAllMatchingMaterials("CACTUS", "CAMPFIRE", "FIRE", "MAGMA_BLOCK", "SOUL_CAMPFIRE", "SOUL_FIRE", "SWEET_BERRY_BUSH", "WITHER_ROSE");
    private static final Set<Material> LAVA_TYPES = getAllMatchingMaterials("FLOWING_LAVA", "LAVA", "STATIONARY_LAVA");
    private static final Material PORTAL = getMatchingMaterial("NETHER_PORTAL", "PORTAL");
    private static final Material LIGHT = getMatchingMaterial("LIGHT");

    // The player can stand inside these materials
    private static final Set<Material> HOLLOW_MATERIALS = EnumSet.noneOf(Material.class);

    static {
        // Materials from Material.isTransparent()
        for (final Material mat : Material.values()) {
            if (mat.isBlock() && !mat.isCollidable()) {
                HOLLOW_MATERIALS.add(mat);
            }
        }

        // Light blocks can be passed through and are not considered transparent for some reason
        if (LIGHT != null) {
            HOLLOW_MATERIALS.add(LIGHT);
        }
    }

    /**
     * Return a set containing <b>all</b> Materials that match one of the provided
     * names.
     *
     * @param names The names of the fields to search for
     * @return All matching enum fields
     */
    private static Set<Material> getAllMatchingMaterials(final String... names) {
        final HashSet<Material> set = new HashSet<>();

        for (final String name : names) {
            Material material = getMatchingMaterial(name);

            if (material != null){
                set.add(material);
            }
        }

        return set;
    }

    /**
     * Returns the Material matching the first provided enum name.
     * If no Material is found, this method returns null.
     *
     * @param names The names of the fields to search for
     * @return The first matching Material
     */
    private static Material getMatchingMaterial(final String... names) {
        for (final String name : names){
            try {
                return Material.valueOf(name);
            } catch (IllegalArgumentException ignored){
            }
        }

        return null;
    }

    public static void setIsWaterSafe(final boolean isWaterSafe) { //todo config
        if (isWaterSafe) {
            HOLLOW_MATERIALS.addAll(WATER_TYPES);
        } else {
            HOLLOW_MATERIALS.removeAll(WATER_TYPES);
        }
    }

    private static boolean isBlockHollowSafe(final World world, final int x, final int y, final int z) { //todo accept open doors and alike via hitbox
        final Material material = world.getBlockAt(x, y, z).getType();

        if (DAMAGING_TYPES.contains(material) || LAVA_TYPES.contains(material) || Tag.BEDS.isTagged(material)) {
            return false;
        }

        if (material == PORTAL) {
            return false;
        }

        return (y > world.getMaxHeight() || HOLLOW_MATERIALS.contains(world.getBlockAt(x, y, z).getType()));
    }


    public static @Nullable Location getSafeLiftDestination(final Location fromLoc, final boolean up, final Location toLoc) {
        if (toLoc == null || toLoc.getWorld() == null) {
            GreenLogger.log(Level.WARNING, "couldn't find safe destination for null.");
            return null;
        }

        final World world = toLoc.getWorld();
        int x = toLoc.getBlockX();
        int y = toLoc.getBlockY();
        int originY = fromLoc.getBlockY();
        int z = toLoc.getBlockZ();

        int maxY = Math.min(toLoc.getBlockY() + 5, world.getMaxHeight()+1);
        int minY = Math.max(toLoc.getBlockY() - 5, world.getMinHeight());

        if(up){ //never teleport to the same floor the origin lift is on
            minY = Math.max(minY, originY+1);
        } else {
            maxY = Math.min(maxY, originY -1);
        }

        //get floor y
        while (isBlockHollowSafe(world, x, y, z)){
            if (y <= minY){ //never teleport to the same floor the origin lift is on
                return null;
            }

            y--;
            GreenLogger.log(Level.INFO, "down: " + y);
        }

        //get 2 safe places above floor
        while (!isBlockHollowSafe(world, x, y, z) || !isBlockHollowSafe(world, x, y+1, z)){
            if (y >= maxY){ //never teleport to the same floor the origin lift is on
                return null;
            }

            y++;
        }

        //check if the block is unsafe to stand on
        final Material material = world.getBlockAt(x, y-1, z).getType();
        if (DAMAGING_TYPES.contains(material) || LAVA_TYPES.contains(material) || Tag.BEDS.isTagged(material) || material == PORTAL) {
            return null;
        }

        return new Location(world, x + 0.5,  y, z + 0.5, toLoc.getYaw(), toLoc.getPitch());
    }
}
