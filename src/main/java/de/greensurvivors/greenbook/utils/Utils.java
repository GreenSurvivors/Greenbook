package de.greensurvivors.greenbook.utils;

import org.bukkit.Location;
import org.bukkit.block.BlockFace;
import org.bukkit.block.data.Rotatable;
import org.bukkit.permissions.Permissible;
import org.bukkit.permissions.Permission;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.Range;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;

public class Utils {


    /**
     * Check if CommandSender has any of the given permissions.
     *
     * @param permissible something with permissions to check for
     * @param permissions permissions to check
     * @return true if permissible has at least one of the given permissions.
     */
    public static boolean hasAnyPermission(@NotNull Permissible permissible, @NotNull Permission... permissions) {
        for (Permission perm : permissions) {
            if (permissible.hasPermission(perm))
                return true;
        }

        return false;
    }

    @Contract(value = "null -> null; !null -> !null", pure = true)
    public static <T> List<T> arrayPopFirstElementToList(T[] array) {
        if (array == null) {
            return null;
        }

        List<T> list = new ArrayList<>(List.of(array));
        list.removeFirst(); // don't include fist element in List

        return list;
    }

    @SuppressWarnings("UnstableApiUsage") // position
    @Contract(pure = true, value = "null, null -> true; null, !null -> false; !null, null -> false")
    public static boolean isSameBlockLocation(@Nullable Location loc1, @Nullable Location loc2) {
        if (loc1 == loc2) {
            // same object or both null
            return true;
        } else if (loc1 != null && loc2 != null) {
            // check world
            if (loc1.getWorld() == loc2.getWorld() || // same object or both null
                (loc1.getWorld() != null &&
                    loc2.getWorld() != null &&
                    loc1.getWorld().getUID().equals(loc2.getWorld().getUID()))) { // same world uuid; this supports reloading of worlds
                return loc1.blockX() == loc2.blockX() && // check block coordinates
                    loc1.blockY() == loc2.blockY() &&
                    loc1.blockZ() == loc2.blockZ();
            }
        }

        return false;
    }

    /**
     * Try to get a member of the enum given as an argument by the name
     *
     * @param enumName  name of the enum to find
     * @param enumClass the enum to check
     * @param <E>       the type of the enum to check
     * @return the member of the enum to check
     * @see #getEnumIgnoreCase(Class, String) for faster access, if you are sure the casing matches
     */
    public static @Nullable <E extends Enum<E>> E getEnum(final @NotNull Class<E> enumClass, final @NotNull String enumName) {
        try {
            // in my testing this is double as fast as iterating over EnumSet.allOf
            // but this depends of course on the size of the enum, iterating getting worse as bigger it is
            return Enum.valueOf(enumClass, enumName);
        } catch (IllegalArgumentException ignored) {
            return null;
        }
    }

    /**
     * Try to get a member of the enum given as an argument by the name
     *
     * @param enumName  name of the enum to find
     * @param enumClass the enum to check
     * @param <E>       the type of the enum to check
     * @return the member of the enum to check
     * @see #getEnum(Class, String) if you aren't sure the casing matches, but with slower access
     */
    public static @Nullable <E extends Enum<E>> E getEnumIgnoreCase(final @NotNull Class<E> enumClass, final @NotNull String enumName) {
        // enumClass.getEnumConstants() takes about 160% more time
        // and Enum.valueOf(enumClass, enumName) is case sensitive.
        for (E enumVal : EnumSet.allOf(enumClass)) {
            if (enumVal.name().equalsIgnoreCase(enumName)) {
                return enumVal;
            }
        }

        return null;
    }

    /**
     * @param rotatable
     * @return true if the sign is oriented along a cardinal direction false otherwise.
     */
    public static boolean isCardinal(final @NotNull Rotatable rotatable) {
        return isCardinal(rotatable.getRotation());
    }

    /**
     * @param blockFace
     * @return true if the blockFace is oriented along a cardinal direction false otherwise.
     */
    public static boolean isCardinal(final @NotNull BlockFace blockFace) {
        return switch (blockFace) {
            case NORTH, SOUTH, EAST, WEST -> true;
            default -> false;
        };
    }

    public static @NotNull BlockFace rotate90AroundY (final @NotNull BlockFace blockFace) {
        return switch (blockFace) {
            case NORTH -> BlockFace.EAST;
            case NORTH_NORTH_EAST -> BlockFace.EAST_SOUTH_EAST;
            case NORTH_EAST -> BlockFace.SOUTH_EAST;
            case EAST_NORTH_EAST -> BlockFace.SOUTH_SOUTH_EAST;
            case EAST -> BlockFace.SOUTH;
            case EAST_SOUTH_EAST -> BlockFace.SOUTH_SOUTH_WEST;
            case SOUTH_EAST -> BlockFace.SOUTH_WEST;
            case SOUTH_SOUTH_EAST -> BlockFace.WEST_SOUTH_WEST;
            case SOUTH -> BlockFace.WEST;
            case SOUTH_SOUTH_WEST -> BlockFace.WEST_NORTH_WEST;
            case SOUTH_WEST -> BlockFace.NORTH_WEST;
            case WEST_SOUTH_WEST -> BlockFace.NORTH_NORTH_WEST;
            case WEST -> BlockFace.NORTH;
            case WEST_NORTH_WEST -> BlockFace.NORTH_NORTH_EAST;
            case NORTH_WEST -> BlockFace.NORTH_EAST;
            case NORTH_NORTH_WEST -> BlockFace.EAST_NORTH_EAST;
            default -> blockFace;
        };
    }

    public static @NotNull BlockFace rotate270AroundY (final @NotNull BlockFace blockFace) {
        return switch (blockFace) {
            case NORTH -> BlockFace.WEST;
            case NORTH_NORTH_EAST -> BlockFace.WEST_NORTH_WEST;
            case NORTH_EAST -> BlockFace.NORTH_WEST;
            case EAST_NORTH_EAST -> BlockFace.NORTH_NORTH_WEST;
            case EAST -> BlockFace.NORTH;
            case EAST_SOUTH_EAST -> BlockFace.NORTH_NORTH_EAST;
            case SOUTH_EAST -> BlockFace.NORTH_EAST;
            case SOUTH_SOUTH_EAST -> BlockFace.EAST_NORTH_EAST;
            case SOUTH -> BlockFace.EAST;
            case SOUTH_SOUTH_WEST -> BlockFace.EAST_SOUTH_EAST;
            case SOUTH_WEST -> BlockFace.SOUTH_EAST;
            case WEST_SOUTH_WEST -> BlockFace.SOUTH_SOUTH_EAST;
            case WEST -> BlockFace.SOUTH;
            case WEST_NORTH_WEST -> BlockFace.SOUTH_SOUTH_WEST;
            case NORTH_WEST -> BlockFace.SOUTH_WEST;
            case NORTH_NORTH_WEST -> BlockFace.WEST_SOUTH_WEST;
            default -> blockFace;
        };
    }

    /**
     * Calculates the smallest integer greater than or equal to the result of dividing a by b.
     *
     * @param  a the dividend (must be greater than 0)
     * @param  b the divisor (must be greater than 0)
     * @return   the smallest integer greater than or equal to a/b
     */
    public static int fastDivCeil(final @Range(from = 1, to = Integer.MAX_VALUE) int a,
                                  final @Range(from = 1, to = Integer.MAX_VALUE) int b) {
        return (a - 1) / b + 1;
    }
}
