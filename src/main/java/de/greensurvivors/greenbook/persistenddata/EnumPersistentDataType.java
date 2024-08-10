package de.greensurvivors.greenbook.persistenddata;

import de.greensurvivors.greenbook.utils.Utils;
import org.bukkit.persistence.PersistentDataAdapterContext;
import org.bukkit.persistence.PersistentDataType;
import org.jetbrains.annotations.NotNull;

public class EnumPersistentDataType<E extends Enum<E>> implements PersistentDataType<String, E> {
    private final @NotNull Class<E> enumClass;

    public EnumPersistentDataType(@NotNull Class<E> enumClass) {
        this.enumClass = enumClass;
    }

    @Override
    public @NotNull Class<String> getPrimitiveType() {
        return String.class;
    }

    @Override
    public @NotNull Class<E> getComplexType() {
        return enumClass;
    }

    @Override
    public @NotNull String toPrimitive(@NotNull E complex, @NotNull PersistentDataAdapterContext context) {
        return complex.name();
    }

    @Override
    public @NotNull E fromPrimitive(@NotNull String primitive, @NotNull PersistentDataAdapterContext context) throws IllegalArgumentException {
        E result = Utils.getEnumIgnoreCase(enumClass, primitive);

        if (result == null) {
            throw new IllegalArgumentException("");
        }

        return result;
    }
}
