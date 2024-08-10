package de.greensurvivors.greenbook.features;

import de.greensurvivors.greenbook.GreenBook;
import de.greensurvivors.greenbook.features.bridge.BridgeFeature;
import de.greensurvivors.greenbook.features.coin.CoinCmdFeature;
import de.greensurvivors.greenbook.features.gate.GateFeature;
import de.greensurvivors.greenbook.features.lift.LiftFeature;
import de.greensurvivors.greenbook.features.painting.PaintingFeature;
import de.greensurvivors.greenbook.features.quotes.QuoteFeature;
import de.greensurvivors.greenbook.features.wireless.WirelessRedstoneFeature;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;
import java.util.List;
import java.util.function.Function;

// this is NOT an enum, so this can be expanded by third parties.
public class FeatureType { // todo make sure ever feature can be disabled
    public final static FeatureType BRIDGE = new FeatureType("Bridges", BridgeFeature::new);
    public final static FeatureType COIN = new FeatureType("Coins", CoinCmdFeature::new);
    public final static FeatureType GATE = new FeatureType("Gates", GateFeature::new);
    public final static FeatureType LIFT = new FeatureType("Lift", LiftFeature::new);
    public final static FeatureType PAINTING = new FeatureType("Painting", PaintingFeature::new);
    public final static FeatureType QUOTES = new FeatureType("Quotes", QuoteFeature::new);
    public final static FeatureType WIRELESS = new FeatureType("WirelessRedstone", WirelessRedstoneFeature::new);

    protected final static @NotNull List<@NotNull FeatureType> standardTypes = List.of(BRIDGE, COIN, GATE, LIFT, PAINTING, QUOTES, WIRELESS);

    protected final @NotNull String featureName;
    protected final @Nullable Function<@NotNull GreenBook, @NotNull AFeature<?>> constructor;

    protected FeatureType(@NotNull String featureName) {
        this.featureName = featureName;
        this.constructor = null;
    }

    protected FeatureType(@NotNull String featureName, @NotNull Function<@NotNull GreenBook, @NotNull AFeature<?>> constructor) {
        this.featureName = featureName;
        this.constructor = constructor;
    }

    public static @NotNull Collection<@NotNull FeatureType> getStandardTypes() {
        return standardTypes;
    }

    public @NotNull String getFeatureName() {
        return featureName;
    }

    public @Nullable AFeature<?> createNewInstance(@NotNull GreenBook plugin) {
        if (constructor != null) {
            return constructor.apply(plugin);
        } else {
            return null;
        }
    }
}
