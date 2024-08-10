package de.greensurvivors.greenbook.persistenddata;

import org.apache.commons.lang3.tuple.ImmutablePair;
import org.bukkit.persistence.PersistentDataAdapterContext;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.util.Vector;
import org.jetbrains.annotations.NotNull;

import java.util.Arrays;

public class AreaPersistentDataType implements PersistentDataType<int[], ImmutablePair<Vector, Vector>> {
    private final static @NotNull AreaPersistentDataType INSTANCE = new AreaPersistentDataType();

    private AreaPersistentDataType (){}

    @Override
    public @NotNull Class<int[]> getPrimitiveType() {
        return int[].class;
    }

    @SuppressWarnings("unchecked")
    @Override
    public @NotNull Class<ImmutablePair<Vector, Vector>> getComplexType() {
        return (Class<ImmutablePair<Vector, Vector>>)((Class<?>)ImmutablePair.class);
    }

    @Override
    public int @NotNull [] toPrimitive(@NotNull ImmutablePair<Vector, Vector> complex, @NotNull PersistentDataAdapterContext context) {
        return new int[] {
            complex.getLeft().getBlockX(), complex.getLeft().getBlockY(), complex.getLeft().getBlockY(),
            complex.getRight().getBlockX(), complex.getRight().getBlockY(), complex.getRight().getBlockY()};
    }

    @Override
    public @NotNull ImmutablePair<Vector, Vector> fromPrimitive(int @NotNull [] primitive, @NotNull PersistentDataAdapterContext context) {
        if (primitive.length < 6) {
            throw new IllegalArgumentException("Primitive int array has not enough elements! " + Arrays.toString(primitive));
        }

        return ImmutablePair.of(new Vector(primitive[0], primitive[1], primitive[2]), new Vector(primitive[3], primitive[4], primitive[5]));
    }

    public static @NotNull AreaPersistentDataType areaPersistentDataType() {
        return INSTANCE;
    }
}
