package de.greensurvivors.greenbook.features.coin;

import de.greensurvivors.greenbook.GreenBook;
import de.greensurvivors.greenbook.config.AFeatureConfigData;
import de.greensurvivors.greenbook.config.AYamlFeatureConfigManager;
import de.greensurvivors.greenbook.features.FeatureType;
import io.leangen.geantyref.TypeToken;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.ItemType;
import org.jetbrains.annotations.NotNull;
import org.spongepowered.configurate.objectmapping.ConfigSerializable;

import java.util.concurrent.CompletableFuture;

public class CoinConfigManager extends AYamlFeatureConfigManager<CoinConfigManager.CoinConfigData> {

    protected CoinConfigManager(@NotNull GreenBook plugin) {
        super(plugin, FeatureType.COIN, TypeToken.get(CoinConfigData.class));
    }

    public @NotNull ItemStack getCoinItem() {
        return configData.coinItem.clone();
    }

    public @NotNull CompletableFuture<Void> setCoinItem(final @NotNull ItemStack item) {
        configData.coinItem = item;
        return saveAndReload();
    }

    @ConfigSerializable
    protected static class CoinConfigData extends AFeatureConfigData {
        // Set default itemstack, so we should not run into null exceptions.
        // However, it should never come into play, since at least the config should provide a default value itself.
        protected @NotNull ItemStack coinItem = ItemType.GOLD_NUGGET.createItemStack(1);
    }
}
