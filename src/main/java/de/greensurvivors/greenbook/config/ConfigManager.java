package de.greensurvivors.greenbook.config;

import de.greensurvivors.greenbook.GreenBook;
import de.greensurvivors.greenbook.features.AFeature;
import io.papermc.paper.math.BlockPosition;
import io.papermc.paper.math.Position;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockCollisions;
import net.minecraft.world.phys.AABB;
import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.block.BlockType;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.craftbukkit.CraftWorld;
import org.bukkit.entity.Entity;
import org.bukkit.util.BoundingBox;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;
import java.util.regex.Pattern;

@SuppressWarnings("UnstableApiUsage") // Block type
public class ConfigManager {
    private final static @NotNull String MC_NAMESPACE = NamespacedKey.MINECRAFT.toUpperCase(Locale.ENGLISH) + ":";
    private final @NotNull GreenBook plugin;

    // the point at the beginning is for bedrock player if the proxy supports them.
    private final @NotNull ConfigOption<@NotNull Pattern> USERNAME_PATTERN = new ConfigOption<>("username_pattern",
        Pattern.compile("^.?[a-zA-Z0-9_]{3,16}$")); // todo mind that a pattern in yaml should use '<pattern>' not "<pattern>"
    private final @NotNull ConfigOption<@NotNull Set<@NotNull BlockType>> UNSAFE_MATS_STANDING_IN = new ConfigOption<>("unsafe_standing_in",
        ConcurrentHashMap.newKeySet());
    private final @NotNull ConfigOption<@NotNull Set<@NotNull BlockType>> UNSAFE_MATS_STANDING_ON = new ConfigOption<>("unsafe_standing_on",
        ConcurrentHashMap.newKeySet());
    private final @NotNull ConfigOption<@NotNull Boolean> IS_WATER_SAFE = new ConfigOption<>("is_water_safe", false);
    private final @NotNull ConfigOption<@NotNull Locale> LOCALE = new ConfigOption<>("language", Locale.getDefault());

    public ConfigManager(@NotNull GreenBook plugin) {
        this.plugin = plugin;

        /*
        // maybe one day we can
        Player p;
        p.damage(12, DamageSource.builder(DamageType.HOT_FLOOR).build());

        DamageType.HOT_FLOOR.getDamageEffect();*/

        // define fallback for sets
        // we can't do this in the constructor because of Tag#getValues()
        Set<BlockType> fallback_in = UNSAFE_MATS_STANDING_IN.getFallback();
        //fallback_in.addAll(Tag.FIRE.getValues())
        Tag.FIRE.getValues().stream().map(Material::asBlockType).forEach(fallback_in::add);
        //fallback_in.addAll(Tag.CAMPFIRES.getValues());
        Tag.CAMPFIRES.getValues().stream().map(Material::asBlockType).forEach(fallback_in::add);
        fallback_in.add(BlockType.SWEET_BERRY_BUSH);
        fallback_in.add(BlockType.WITHER_ROSE);
        fallback_in.add(BlockType.CACTUS);
        fallback_in.add(BlockType.POWDER_SNOW);
        fallback_in.add(BlockType.LAVA);
        //fallback_in.addAll(Tag.PORTALS.getValues());
        Tag.PORTALS.getValues().stream().map(Material::asBlockType).forEach(fallback_in::add);

        Set<BlockType> fallback_on = UNSAFE_MATS_STANDING_ON.getFallback();
        //fallback_on.addAll(Tag.CAMPFIRES.getValues());
        Tag.CAMPFIRES.getValues().stream().map(Material::asBlockType).forEach(fallback_on::add);
        fallback_on.add(BlockType.CACTUS);
        fallback_on.add(BlockType.MAGMA_BLOCK);
        fallback_on.add(BlockType.POINTED_DRIPSTONE);
    }

    public static @NotNull Map<String, Object> getMapFromConfigObject(final @Nullable Object object, @NotNull Function<Object, Boolean> invalidKeyFunction) throws NullPointerException, IllegalArgumentException {
        switch (object) {
            case Map<?, ?> rawMap -> {
                Map<String, Object> result = new HashMap<>(rawMap.size());

                for (Map.Entry<?, ?> rawEntry : rawMap.entrySet()) {
                    if (rawEntry.getKey() instanceof String key) {
                        result.put(key, rawEntry.getValue());
                    } else if (invalidKeyFunction.apply(rawEntry.getKey())) {
                        throw new IllegalArgumentException("");
                    }
                }

                return result;
            }
            case ConfigurationSection section -> {
                Set<String> keys = section.getKeys(false);
                Map<String, Object> result = new HashMap<>(keys.size());


                for (String key : keys) {
                    result.put(key, section.get(key));
                }

                return result;
            }
            case null -> throw new NullPointerException("");
            default -> throw new IllegalArgumentException("");
        }
    }

    /**
     * @param locationToCheckAt
     * @param entityBoundingBox
     * @return
     * @see net.minecraft.world.level.Level#findSupportingBlock(net.minecraft.world.entity.Entity, net.minecraft.world.phys.AABB)
     */
    // todo pretty sure we can optimize this if we would merge this with the isEntitySafeAt() methode
    public static @Nullable BlockPosition findSupportingBlockAt(final @NotNull Location locationToCheckAt, final @NotNull BoundingBox entityBoundingBox) { // todo try to go back to only API usage but hack the new BlockCollisions code is complicated
        AABB aabb = new AABB(
            entityBoundingBox.getMinX(), entityBoundingBox.getMinY(), entityBoundingBox.getMinZ(),
            entityBoundingBox.getMaxX(), entityBoundingBox.getMaxY(), entityBoundingBox.getMaxZ());

        BlockPosition resultBlockPos = null;
        double shortestDistanceSquared = Double.MAX_VALUE;
        // forEntity is a confusing name since it really means if we check for suffocation aka non-transparent blocks.
        // since an entity can stand on such like a Glas block, it has to be false.
        BlockCollisions<BlockPos> blockCollisions = new BlockCollisions<>(((CraftWorld) locationToCheckAt.getWorld()).getHandle(), (net.minecraft.world.entity.Entity) null, aabb, false, (blockPos, voxelShape) -> blockPos);

        while (blockCollisions.hasNext()) {
            BlockPos nmsPosNow = blockCollisions.next();
            BlockPosition posNow = Position.block(nmsPosNow.getX(), nmsPosNow.getY(), nmsPosNow.getZ());

            double dx = locationToCheckAt.x() - posNow.x();
            double dy = locationToCheckAt.y() - posNow.y();
            double dz = locationToCheckAt.z() - posNow.z();
            // we don't have to use the root here since we only do a comparison with the last value and (square) roots are expensive
            double newDistanceSquared = dx * dx + dy * dy + dz * dz;

            if (newDistanceSquared < shortestDistanceSquared || newDistanceSquared == shortestDistanceSquared && isNewBlockOrBetterOrder(resultBlockPos, posNow)) {
                resultBlockPos = posNow;
                shortestDistanceSquared = newDistanceSquared;
            }
        }

        return resultBlockPos;
    }

    private static boolean isNewBlockOrBetterOrder(final @Nullable BlockPosition resultBlock, final @NotNull BlockPosition block2) {
        boolean newBlockOrBetterOrder;

        if (resultBlock == null) {
            newBlockOrBetterOrder = true;
        } else { // This will
            if (resultBlock.y() == block2.y()) {
                if (resultBlock.z() == block2.z()) {
                    newBlockOrBetterOrder = resultBlock.x() - block2.x() < 0;
                } else {
                    newBlockOrBetterOrder = resultBlock.z() - block2.z() < 0;
                }
            } else {
                newBlockOrBetterOrder = resultBlock.y() - block2.y() < 0;
            }
        }
        return newBlockOrBetterOrder;
    }

    /**
     * Try to get an offline player from a name or a
     * string representation of a UUID.
     * <br>
     * I don't plan to support offline mode servers.
     * If you don't have a valid UUID working for you,
     * I'm not going through the hassle to service you.
     * <br>
     * Please note: this will return some kind of offline player,
     * But as it goes in Bukkit's API this player might have never played
     * on the server, nor is it a valid minecraft account to begin with.
     *
     * @param str username or a string representation of a UUID.
     * @return an offline player or null, if the argument couldn't be nighter a username nor an uuid
     */
    public @Nullable OfflinePlayer getPlayerFromString(@NotNull String str) {
        if (isUserName(str)) { //check valid names
            return Bukkit.getOfflinePlayer(str);
        } else {
            try {
                return Bukkit.getOfflinePlayer(UUID.fromString(str));
            } catch (IllegalArgumentException ignored) {
            }
        }

        return null;
    }

    public boolean isUserName(@NotNull String text) {
        return USERNAME_PATTERN.getValueOrFallback().matcher(text).matches();
    }

    public void reload() {
        @Nullable String langTag = plugin.getConfig().getString(LOCALE.getPath());

        if (langTag != null) {
            Locale locale = Locale.forLanguageTag(langTag.replace("_", "-"));

            // fall back if locale is undefined
            if (locale.getLanguage().isEmpty()) {
                LOCALE.setValue(null);
            } else {
                LOCALE.setValue(locale);
            }
        }

        plugin.getMessageManager().reload(LOCALE.getValueOrFallback());

        for (AFeature<?> feature : plugin.getFeatureRegistry().getAllFeatures()) {
            feature.getFeatureConfig().reloadConfig();
        }

        Set<BlockType> temp = ConcurrentHashMap.newKeySet();
        temp.addAll(getBlockTypesFromConfig(plugin.getConfig(), UNSAFE_MATS_STANDING_IN.getPath()));
        UNSAFE_MATS_STANDING_IN.setValue(temp);

        temp = ConcurrentHashMap.newKeySet();
        temp.addAll(getBlockTypesFromConfig(plugin.getConfig(), UNSAFE_MATS_STANDING_ON.getPath()));
        UNSAFE_MATS_STANDING_ON.setValue(temp);
    }

    public @NotNull Set<@NotNull BlockType> getBlockTypesFromConfig(final @NotNull FileConfiguration config, final @NotNull String key) {
        // load BlockType set
        List<?> objects = config.getList(key);
        /* we need two sets, in case a remove entry happens before an add entry, like in case of:
         - -STONE
         - *
        */

        if (objects == null || objects.isEmpty()) {
            return new HashSet<>();
        }

        Set<BlockType> addSet = new HashSet<>();
        Set<BlockType> removeSet = new HashSet<>();

        Iterable<Tag<BlockType>> tagCache = null;
        for (Object object : objects) {
            switch (object) {
                case BlockType material -> addSet.add(material);
                case String string -> {
                    if (string.equals("*")) {
                        Registry.BLOCK.forEach(addSet::add);
                    } else {
                        boolean add = true;

                        if (string.startsWith("-")) {
                            add = false;
                            string = string.substring(1);
                        }

                        BlockType blockType = Registry.BLOCK.get(NamespacedKey.fromString(string));

                        if (blockType != null) {
                            if (add) {
                                addSet.add(blockType);
                            } else {
                                removeSet.add(blockType);
                            }
                        } else { //try tags
                            // lazy initialisation
                            if (tagCache == null) {
                                tagCache = plugin.getServer().getTags(Tag.REGISTRY_BLOCKS, BlockType.class);
                            }

                            string = string.toUpperCase(Locale.ENGLISH);
                            string = string.replaceAll("\\s+", "_");

                            if (!string.startsWith(MC_NAMESPACE)) {
                                string = MC_NAMESPACE + string;
                            }

                            boolean found = false;

                            for (Tag<BlockType> tag : tagCache) {
                                if (tag.getKey().asString().equalsIgnoreCase(string)) {

                                    if (add) {
                                        addSet.addAll(tag.getValues());
                                    } else {
                                        removeSet.addAll(tag.getValues());
                                    }
                                    found = true;
                                    break;
                                }
                            }

                            if (!found) {
                                plugin.getLogger().warning("Couldn't get BlockType \"" + string + "\" for block list. Ignoring.");
                            }
                        }
                    }
                }
                case null -> plugin.getLogger().warning("Couldn't get empty BlockType for block list. Ignoring.");
                default ->
                    plugin.getLogger().warning("Couldn't get BlockType \"" + object + "\" for block list. Ignoring.");
            }
        }
        addSet.removeAll(removeSet);

        return addSet;
    }

    /**
     * @param entity
     * @param world
     * @param x
     * @param y
     * @param z
     * @return
     * @see net.minecraft.world.level.CollisionGetter#getBlockCollisions(net.minecraft.world.entity.Entity, AABB)
     */
    public boolean isEntitySafeAt(@NotNull Entity entity, @NotNull World world, double x, double y, double z) { // todo adapt to the new BlockCollisions system but still stay on API
        //start looking for unsafe blocks one Block under the entity.
        if (y - 1 < world.getMinHeight()) {
            return false;
        }

        final @NotNull Location locToTest = new Location(world, x, y, z);

        if (!world.getWorldBorder().isInside(locToTest)) {
            return false;
        }

        // get entity bounding box at new location
        final @NotNull Location entityLocationNow = entity.getLocation();
        final @NotNull BoundingBox entityBoundingBox = entity.getBoundingBox().
            shift(entityLocationNow.x() - x, entityLocationNow.y() - y, entityLocationNow.z() - z);
        // check block to stand on
        // shift the bounding box ever so slightly under the entity
        BlockPosition supportingBlock = findSupportingBlockAt(locToTest, entityBoundingBox.clone().shift(0, -1.0E-6D, 0));
        if (supportingBlock == null) {
            return false;
        } else if (UNSAFE_MATS_STANDING_ON.getValueOrFallback().contains(world.getBlockAt(supportingBlock.blockX(), supportingBlock.blockY(), supportingBlock.blockZ()).getType().asBlockType())) {
            return false;
        }

        // we just assume if water would be at eye height it would have flown down if there wasn't a block with collision
        final @NotNull Material matAtLoc = locToTest.getBlock().getType();
        if ((matAtLoc == Material.WATER || matAtLoc == Material.BUBBLE_COLUMN) && !IS_WATER_SAFE.getValueOrFallback()) {
            return false;
        }

        final int minX = (int) Math.floor(entityBoundingBox.getMinX());
        final int maxX = (int) Math.ceil(entityBoundingBox.getMaxX());
        //end looking at the height of the entity
        final int minY = (int) Math.max(world.getMinHeight(), Math.floor(entityBoundingBox.getMinY()));
        final int maxY = (int) Math.min(world.getMaxHeight(), entityBoundingBox.getMaxY());
        final int minZ = (int) Math.floor(entityBoundingBox.getMinZ());
        final int maxZ = (int) Math.ceil(entityBoundingBox.getMaxZ());

        for (int xCheck = minX; xCheck <= maxX; xCheck++) {
            for (int yCheck = minY; yCheck <= maxY; yCheck++) {

                for (int zCheck = minZ; zCheck <= maxZ; zCheck++) {
                    final Block blockCheck = world.getBlockAt(xCheck, yCheck, zCheck);
                    final org.bukkit.util.VoxelShape blockVoxelShape = blockCheck.getCollisionShape();

                    if (UNSAFE_MATS_STANDING_IN.getValueOrFallback().contains(blockCheck.getType().asBlockType())) {
                        if (!blockCheck.isPassable()) {
                            // overlaps or border to border
                            // because you don't need to stand IN the cactus to take damage.
                            for (BoundingBox box : blockVoxelShape.getBoundingBoxes()) {
                                double minX1 = box.getMinX();
                                double minY1 = box.getMinY();
                                double minZ1 = box.getMinZ();
                                double maxX1 = box.getMaxX();
                                double maxY1 = box.getMaxY();
                                double maxZ1 = box.getMaxZ();

                                double minX2 = entityBoundingBox.getMinX();
                                double minY2 = entityBoundingBox.getMinY();
                                double minZ2 = entityBoundingBox.getMinZ();
                                double maxX2 = entityBoundingBox.getMaxX();
                                double maxY2 = entityBoundingBox.getMaxY();
                                double maxZ2 = entityBoundingBox.getMaxZ();

                                if ((minX1 - maxX2) < -1.0E-7 && (maxX1 - minX2) > 1.0E-7 &&
                                    (minY1 - maxY2) < -1.0E-7 && (maxY1 - minY2) > 1.0E-7 &&
                                    (minZ1 - maxZ2) < -1.0E-7 && (maxZ1 - minZ2) > 1.0E-7) {
                                    return false;
                                }
                            }
                        } else if (blockVoxelShape.overlaps(entityBoundingBox)) {
                            return false;
                        }
                    } else if (!blockCheck.isPassable()) { // blockCheck.isCollidable()
                        if (blockVoxelShape.overlaps(entityBoundingBox)) {
                            return false;
                        }
                    }
                }
            }
        }

        return true;
    }
}
