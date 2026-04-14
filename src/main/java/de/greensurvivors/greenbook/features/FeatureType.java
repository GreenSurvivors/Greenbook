package de.greensurvivors.greenbook.features;

import de.greensurvivors.greenbook.GreenBook;
import de.greensurvivors.greenbook.config.IFeatureConfigManager;
import de.greensurvivors.greenbook.features.bridge.BridgeConfigManager;
import de.greensurvivors.greenbook.features.bridge.BridgeFeature;
import de.greensurvivors.greenbook.features.coin.CoinCmdFeature;
import de.greensurvivors.greenbook.features.coin.CoinConfigManager;
import de.greensurvivors.greenbook.features.gate.GateConfigManager;
import de.greensurvivors.greenbook.features.gate.GateFeature;
import de.greensurvivors.greenbook.features.lift.LiftConfigManager;
import de.greensurvivors.greenbook.features.lift.LiftFeature;
import de.greensurvivors.greenbook.features.painting.PaintingConfigManager;
import de.greensurvivors.greenbook.features.painting.PaintingFeature;
import de.greensurvivors.greenbook.features.quotes.QuoteConfigManager;
import de.greensurvivors.greenbook.features.quotes.QuoteFeature;
import de.greensurvivors.greenbook.features.wireless.WirelessConfigManager;
import de.greensurvivors.greenbook.features.wireless.WirelessRedstoneFeature;
import net.kyori.adventure.key.Key;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;
import java.util.List;
import java.util.function.Function;

// this is NOT an enum, so this can be expanded by third parties.
public class FeatureType <ConfigManagerType extends IFeatureConfigManager> { // todo make sure every feature can be disabled; also the namespace here is hardcoded instead of fetched via the plugin meta
    public final static @NotNull FeatureType<BridgeConfigManager> BRIDGE = new FeatureType<>(Key.key("greenbook", "bridge"), BridgeFeature::new);
    public final static @NotNull FeatureType<CoinConfigManager> COIN = new FeatureType<>(Key.key("greenbook", "coin"), CoinCmdFeature::new);
    public final static @NotNull FeatureType<GateConfigManager> GATE = new FeatureType<>(Key.key("greenbook", "gate"), GateFeature::new);
    public final static @NotNull FeatureType<LiftConfigManager> LIFT = new FeatureType<>(Key.key("greenbook", "lift"), LiftFeature::new);
    public final static @NotNull FeatureType<PaintingConfigManager> PAINTING = new FeatureType<>(Key.key("greenbook", "painting"), PaintingFeature::new);
    public final static @NotNull FeatureType<QuoteConfigManager> QUOTES = new FeatureType<>(Key.key("greenbook", "quotes"), QuoteFeature::new);
    public final static @NotNull FeatureType<WirelessConfigManager> WIRELESS = new FeatureType<>(Key.key("greenbook", "wirelessredstone"), WirelessRedstoneFeature::new);

    protected final static @NotNull List<@NotNull FeatureType<?>> standardTypes = List.of(BRIDGE, COIN, GATE, LIFT, PAINTING, QUOTES, WIRELESS);

    protected final @NotNull Key featureKey;
    protected final @Nullable Function<@NotNull GreenBook, @NotNull AFeature<ConfigManagerType>> constructor;

    protected FeatureType(final @NotNull Key featureKey) {
        this.featureKey = featureKey;
        this.constructor = null;
    }

    protected FeatureType(final @NotNull Key featureKey, final @NotNull Function<@NotNull GreenBook, @NotNull AFeature<ConfigManagerType>> constructor) {
        this.featureKey = featureKey;
        this.constructor = constructor;
    }

    public static @NotNull Collection<@NotNull FeatureType<?>> getStandardTypes() {
        return standardTypes;
    }

    public @NotNull Key getFeatureKey() {
        return featureKey;
    }

    public @Nullable AFeature<ConfigManagerType> createNewInstance(@NotNull GreenBook plugin) {
        if (constructor != null) {
            return constructor.apply(plugin);
        } else {
            return null;
        }
    }

    @Override
    public boolean equals(final @Nullable Object obj) {
        if (obj == null) {
            return false;
        } else if (getClass() != obj.getClass()) {
            return false;
        }
        final @NotNull FeatureType<?> other = (FeatureType<?>) obj;
        return this.featureKey.equals(other.featureKey);
    }

    @Override
    public int hashCode() {
        return this.featureKey.hashCode();
    }
}
