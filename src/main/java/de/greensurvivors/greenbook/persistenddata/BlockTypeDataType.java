package de.greensurvivors.greenbook.persistenddata;

import org.bukkit.NamespacedKey;
import org.bukkit.Registry;
import org.bukkit.block.BlockType;
import org.bukkit.persistence.PersistentDataAdapterContext;
import org.bukkit.persistence.PersistentDataType;
import org.jetbrains.annotations.NotNull;

@SuppressWarnings("ALL") // block type
public class BlockTypeDataType implements PersistentDataType<String, BlockType> {
    private final static @NotNull BlockTypeDataType INSTANCE = new BlockTypeDataType();

    private BlockTypeDataType(){
    }

    public static @NotNull BlockTypeDataType materialDataType () {
        return INSTANCE;
    }

    @Override
    public @NotNull Class<String> getPrimitiveType() {
        return String.class;
    }

    @Override
    public @NotNull Class<BlockType> getComplexType() {
        return BlockType.class;
    }

    @Override
    public @NotNull String toPrimitive(@NotNull BlockType complex, @NotNull PersistentDataAdapterContext context) {
        return Registry.BLOCK.getKey(complex).asString();
    }

    @Override
    public @NotNull BlockType fromPrimitive(@NotNull String primitive, @NotNull PersistentDataAdapterContext context) throws IllegalArgumentException {
        NamespacedKey key = NamespacedKey.fromString(primitive);

        if (key != null) {
            BlockType complex = Registry.BLOCK.get(key);
            if (complex != null) {
                return complex;
            } else {
                throw new IllegalArgumentException("Primitive String '" + primitive + "' is not a valid BlockType!");
            }
        } else {
            throw new IllegalArgumentException("Primitive String '" + primitive + "' is not a valid NamespacedKey!");
        }
    }
}
