package de.greensurvivors.greenbook.config;

import com.google.gson.JsonParser;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
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
import org.spongepowered.configurate.serialize.SerializationException;
import org.spongepowered.configurate.serialize.TypeSerializer;

import java.lang.reflect.Type;
import java.util.Locale;
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

    protected static class ComponentSerializer implements TypeSerializer<Component> {
        static final @NotNull ComponentSerializer INSTANCE = new ComponentSerializer();

        @Override
        public Component deserialize(final @NotNull Type type, final @NotNull ConfigurationNode node) throws SerializationException {
            if (node.isNull()) {
                throw new SerializationException(String.class, "Null node for component deserialization");
            }

            return MiniMessage.miniMessage().deserialize(node.getString());
        }

        @Override
        public void serialize(final @NotNull Type type, final @Nullable Component obj, final @NotNull ConfigurationNode node) throws SerializationException {
            if (obj != null) {
                node.set(String.class, MiniMessage.miniMessage().serialize(obj));
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

                node.set(String.class, obj.getAsString());
            } else {
                node.raw(null);
            }
        }
    }

    protected static class BlockTypeSerializer implements TypeSerializer<BlockType> {
        static final @NotNull BlockTypeSerializer INSTANCE = new BlockTypeSerializer();

        @Override
        public BlockType deserialize(final @NotNull Type type, final @NotNull ConfigurationNode node) throws SerializationException {
            if (node.isNull()) {
                throw new SerializationException(String.class, "Null node for locale deserialization");
            }

            return Registry.BLOCK.getOrThrow(NamespacedKey.fromString(node.getString()));
        }

        @Override
        public void serialize(final @NotNull Type type, final @Nullable BlockType obj, final @NotNull ConfigurationNode node) throws SerializationException {
            if (obj != null) {

                node.set(String.class, obj.key().toString());
            } else {
                node.raw(null);
            }
        }
    }

    protected static class LocaleSerializer implements TypeSerializer<Locale> {
        static final @NotNull LocaleSerializer INSTANCE = new LocaleSerializer();

        @Override
        public Locale deserialize(final @NotNull Type type, final @NotNull ConfigurationNode node) throws SerializationException {
            if (node.isNull()) {
                throw new SerializationException(String.class, "Null node for locale deserialization");
            }

            return Locale.forLanguageTag(node.getString());
        }

        @Override
        public void serialize(final @NotNull Type type, final @Nullable Locale obj, final @NotNull ConfigurationNode node) throws SerializationException {
            if (obj != null) {

                node.set(String.class, obj.toLanguageTag());
            } else {
                node.raw(null);
            }
        }
    }

    protected static class ItemStackSerializer implements TypeSerializer<ItemStack> {
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
