package de.greensurvivors.greenbook.config;

import com.google.gson.JsonParser;
import net.kyori.adventure.key.Key;
import org.apache.maven.artifact.versioning.ComparableVersion;
import org.bukkit.Bukkit;
import org.bukkit.NamespacedKey;
import org.bukkit.Registry;
import org.bukkit.block.BlockType;
import org.bukkit.block.data.BlockData;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.configurate.ConfigurationNode;
import org.spongepowered.configurate.serialize.ScalarSerializer;
import org.spongepowered.configurate.serialize.SerializationException;
import org.spongepowered.configurate.serialize.TypeSerializer;

import java.lang.reflect.Type;
import java.util.Locale;
import java.util.function.Predicate;
import java.util.regex.Pattern;

public abstract class Serializers {
    protected static class ComparableVersionSerializer implements TypeSerializer<ComparableVersion> {
        static final @NotNull ComparableVersionSerializer INSTANCE = new ComparableVersionSerializer();

        @Override
        public ComparableVersion deserialize(final @NotNull Type type, final @NotNull ConfigurationNode node) throws SerializationException {
            if (node.isNull()) {
                throw new SerializationException(String.class, "Null node for comparable version deserialization");
            }

            return new ComparableVersion(node.getString());
        }

        @Override
        public void serialize(final @NotNull Type type, final @Nullable ComparableVersion obj, final @NotNull ConfigurationNode node) throws SerializationException {
            if (obj != null) {
                node.set(String.class, obj.toString());
            } else {
                node.raw(null);
            }
        }
    }

    protected static class PatternSerializer implements TypeSerializer<Pattern> {
        static final @NotNull PatternSerializer INSTANCE = new PatternSerializer();

        @Override
        public Pattern deserialize(final @NotNull Type type, final @NotNull ConfigurationNode node) throws SerializationException {
            if (node.isNull()) {
                throw new SerializationException(String.class, "Null node for pattern deserialization");
            }

            return Pattern.compile(node.getString());
        }

        @Override
        public void serialize(final @NotNull Type type, final @Nullable Pattern obj, final @NotNull ConfigurationNode node) throws SerializationException {
            if (obj != null) {
                node.set(String.class, obj.pattern());
            } else {
                node.raw(null);
            }
        }
    }

    protected static class BlockDataSerializer implements TypeSerializer<BlockData> {
        static final @NotNull BlockDataSerializer INSTANCE = new BlockDataSerializer();

        @Override
        public BlockData deserialize(final @NotNull Type type, final @NotNull ConfigurationNode node) throws SerializationException {
            if (node.isNull()) {
                throw new SerializationException(String.class, "Null node for block data deserialization");
            }

            return Bukkit.createBlockData(node.getString());
        }

        @Override
        public void serialize(final @NotNull Type type, final @Nullable BlockData obj, final @NotNull ConfigurationNode node) throws SerializationException {
            if (obj != null) {

                node.set(String.class, obj.getAsString(true));
            } else {
                node.raw(null);
            }
        }
    }

    protected static class BlockTypeSerializer extends ScalarSerializer<BlockType> {
        static final @NotNull BlockTypeSerializer INSTANCE = new BlockTypeSerializer();

        protected BlockTypeSerializer() {
            super(BlockType.class);
        }

        @Override
        public BlockType deserialize(final @NotNull Type type, final @NotNull Object obj) {
            return Registry.BLOCK.getOrThrow(NamespacedKey.fromString(obj.toString()));
        }

        @Override
        protected @NotNull Object serialize(final @NotNull BlockType item, final @NotNull Predicate<Class<?>> typeSupported) {
            if (typeSupported.test(Key.class)) {
                return item.key();
            }

            return item.key().asMinimalString();
        }
    }

    protected static class LocaleSerializer extends ScalarSerializer<Locale> {
        static final @NotNull LocaleSerializer INSTANCE = new LocaleSerializer();

        protected LocaleSerializer() {
            super(Locale.class);
        }

        @Override
        public Locale deserialize(final @NotNull Type type, final @NotNull Object obj) {
            return Locale.forLanguageTag(obj.toString());
        }

        @Override
        protected @NotNull Object serialize(final Locale item, final @NotNull Predicate<@NotNull Class<?>> typeSupported) {
            return item.toLanguageTag();
        }
    }

    protected static class ItemStackSerializer implements TypeSerializer<ItemStack> { // todo use https://minecraft.wiki/w/Argument_types#minecraft:item_stack
        static final @NotNull ItemStackSerializer INSTANCE = new ItemStackSerializer();

        @Override
        public ItemStack deserialize(final @NotNull Type type, final @NotNull ConfigurationNode node) throws SerializationException {
            if (node.isNull()) {
                throw new SerializationException(String.class, "Null node for ItemStack deserialization");
            }

            return Bukkit.getUnsafe().deserializeItemFromJson(JsonParser.parseString(node.getString()).getAsJsonObject());
        }

        @Override
        public void serialize(final @NotNull Type type, final @Nullable ItemStack obj, final @NotNull ConfigurationNode node) throws SerializationException {
            if (obj != null) {
                node.set(String.class, Bukkit.getUnsafe().serializeItemAsJson(obj).toString());
            } else {
                node.raw(null);
            }
        }
    }
}
