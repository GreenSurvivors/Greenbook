package de.greensurvivors.greenbook.features.bridge;

import de.greensurvivors.greenbook.GreenBook;
import de.greensurvivors.greenbook.commands.GreenBookCmd;
import de.greensurvivors.greenbook.features.AFeature;
import de.greensurvivors.greenbook.features.FeatureType;
import de.greensurvivors.greenbook.language.StandardLangPath;
import de.greensurvivors.greenbook.persistenddata.EnumPersistentDataType;
import de.greensurvivors.greenbook.utils.Utils;
import io.papermc.paper.command.brigadier.Commands;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.NamespacedKey;
import org.bukkit.Tag;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.Sign;
import org.bukkit.block.data.BlockData;
import org.bukkit.block.data.Directional;
import org.bukkit.block.data.Rotatable;
import org.bukkit.block.data.type.WallSign;
import org.bukkit.block.sign.Side;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockPhysicsEvent;
import org.bukkit.event.block.SignChangeEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class BridgeFeature extends AFeature<BridgeConfig> implements Listener {
    private final static @NotNull EnumPersistentDataType<BlockFace> BRIDGE_EXPECTED_DIRECTION_DATA_TYPE = new EnumPersistentDataType<>(BlockFace.class);
    private final @NotNull NamespacedKey blockTypeKey, amountKey, stateKey, expectedBridgeDirectionKey, lastPowerStateKey;
    // save amount of stored blocks
    // save state (on/off)

    public BridgeFeature(@NotNull GreenBook plugin) {
        super(plugin, FeatureType.BRIDGE, new BridgeConfig(plugin));

        blockTypeKey = new NamespacedKey(plugin, "bridgeBlockType");
        amountKey = new NamespacedKey(plugin, "bridgeBlockAmount");
        stateKey = new NamespacedKey(plugin, "bridgeState");
        expectedBridgeDirectionKey = new NamespacedKey(plugin, "expectedBridgeDirection");

        lastPowerStateKey = new NamespacedKey(plugin, "bridgeLastPowerStateKey");

        Bukkit.getPluginManager().registerEvents(this, plugin);
    }

    @Override
    public void registerCommands(@NotNull Commands commandsRegistrar, @NotNull GreenBookCmd mainCommand) {

    }

    @Override
    public void onDisable() {

    }

    @Override
    public void onEnable() {

    }

    @EventHandler(ignoreCancelled = true)
    private void onSignChange(@NotNull SignChangeEvent event) {
        if (!getFeatureConfig().isEnabled()) {
            return;
        }

        Component line1 = event.line(1);
        Player ePlayer = event.getPlayer();

        //if the 2nd line exists
        if (line1 != null) {
            //and is a bridge label
            BridgeType type = BridgeType.fromLabel(line1);
            if (type != null) {
                if (event.getBlock().getState(false) instanceof Sign sign) { // danger no snapshot
                    if (Utils.isCardinal((org.bukkit.block.data.type.Sign) sign.getBlockData())) {
                        // only handle front sides
                        if (event.getSide() == Side.FRONT) {
                            // check permission
                            if (ePlayer.hasPermission(BridePermissions.BRIDGE_CREATE.getPermission())) {
                                //set the line with the right casing
                                event.line(1, type.getLabel());

                                plugin.getMessageManager().sendLang(ePlayer, BridgeLangPath.BRIDGE_CREATE_SUCCESS); // todo add bridgeType here

                                // auto wax to don't annoy users opening editing screen every time they try to use a bridge
                                sign.setWaxed(true);
                                sign.update(); //todo test if we have to call update if we don't use a snapshot
                            } else { //no permission
                                plugin.getMessageManager().sendLang(ePlayer, StandardLangPath.NO_PERMISSION);

                                event.setCancelled(true);
                                event.getBlock().breakNaturally();
                            }
                        } else { //backside
                            plugin.getMessageManager().sendLang(ePlayer, StandardLangPath.ERROR_SIGN_BACKSIDE);
                            event.line(1, Component.empty());
                            event.setCancelled(true);
                        }
                    } else {
                        plugin.getMessageManager().sendLang(ePlayer, BridgeLangPath.BRIDGE_NOT_CARDINAL);

                        event.setCancelled(true);
                        event.getBlock().breakNaturally();
                    }
                } // not a sign - how?
            }  //no bridge type
        } //no 2nd line
    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.MONITOR)
    private void onSignPowerChange(@NotNull BlockPhysicsEvent event) { // todo
        if (!getFeatureConfig().isEnabled()) {
            return;
        }

        Block eBlock = event.getBlock();
        if (Tag.ALL_SIGNS.isTagged(event.getChangedType())) { // fast check to "fail" fast for non-relevant changes
            Sign sign = ((Sign) eBlock.getState(false)); // danger no snapshot
            PersistentDataContainer container = sign.getPersistentDataContainer();

            Boolean wasPowered = container.get(lastPowerStateKey, PersistentDataType.BOOLEAN);

            if (wasPowered == null) {
                BridgeType type = getTypeFromSign(sign);

                if (type == BridgeType.BRIDGE) {
                    wasPowered = Boolean.FALSE;
                } else {
                    return;
                }
            }


            // todo Eigencraft compatible?
            //((CraftWorld)eBlock.getWorld()).getHandle().getBestNeighborSignal(); <-- maybe better for wireless
            //((CraftWorld)eBlock.getWorld()).getHandle().getSignal(pos.relative(direction), direction); <-- this could be better if we just check for the block who caused the Blockphysics event. However we also would need to keep track what the last power source was
            // same could be said for eBlock.getBlockPower(blockface)
            // todo chck indirct powerd vs block power

            boolean isPowered = eBlock.isBlockIndirectlyPowered();
            if (isPowered != wasPowered) {
                container.set(lastPowerStateKey, PersistentDataType.BOOLEAN, isPowered);

                if (isPowered) { // todo

                    // set blocks
                } else {
                    // remove blocks
                }
            }

            // todo do we need to update the state here?
        }
    }

    /**
     * a bridge can be used by directly clicking a sign
     */
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    private void onRightClickSign(final @NotNull PlayerInteractEvent event) {
        if (!getFeatureConfig().isEnabled()) {
            return;
        }

        //don't fire for offhand
        if (event.getHand() == EquipmentSlot.HAND &&
            //right-clicked a block. Should ensure the getBlock() is not null
            event.getAction() == Action.RIGHT_CLICK_BLOCK) {
            //cache the clicked block
            Block eBlock = event.getClickedBlock();

            //is the clicked block a sign?
            if (eBlock != null && eBlock.getState(false) instanceof Sign signState) {
                // let player edit signs if shifting

                BridgeType type = getTypeFromSign(signState);
                if (type != null) {
                    if (signState.isWaxed() || !event.getPlayer().isSneaking()) {
                        if (eBlock.getBlockData() instanceof org.bukkit.block.data.type.Sign signData && Utils.isCardinal(signData)) {
                            flipState(signState);
                        } else { // wrongly rotated
                            plugin.getMessageManager().sendLang(event.getPlayer(), BridgeLangPath.BRIDGE_NOT_CARDINAL);
                        }
                    } //player will edit sign instead of using the bridge
                }
            } //not interacted with a sign
        } //wrong hand or not a block
    }

    private void flipState(final @NotNull Sign signState) { //todo
    }

    private @Nullable BridgeType getTypeFromSign(@NotNull Sign sign) {
        return BridgeType.fromLabel(sign.getSide(Side.FRONT).line(1));
    }


    // get BlockData on creation
    private @Nullable BlockData getAndSetBridgeBlockType(@NotNull Sign sign) { // todo get and check the other sign
        final @NotNull Block signBlock = sign.getBlock();
        final @NotNull PersistentDataContainer container = sign.getPersistentDataContainer();

        @Nullable BlockFace blockFace = container.get(expectedBridgeDirectionKey, BRIDGE_EXPECTED_DIRECTION_DATA_TYPE);

        if (blockFace == null) {
            blockFace = getFeatureConfig().getExpectedBridgeDirectionFromComponent(sign.getSide(Side.FRONT).line(2));
        }

        if (blockFace != null) {
            container.set(expectedBridgeDirectionKey, BRIDGE_EXPECTED_DIRECTION_DATA_TYPE, blockFace);

            final @NotNull Block bridgeBlock = signBlock.getRelative(blockFace);
            final @NotNull BlockData bridgeBlockData = signBlock.getBlockData();

            if (getFeatureConfig().isAllowedBlock(bridgeBlockData) && !bridgeBlock.getType().isAir()) {
                return bridgeBlockData;
            } else {
                return null;
            }
        }

        if (sign.getBlockData() instanceof WallSign wallSign) {
            blockFace = wallSign.getFacing().getOppositeFace();
            final @NotNull Block bridgeBlock = signBlock.getRelative(blockFace);
            final @NotNull BlockData bridgeBlockData = signBlock.getBlockData();

            if (getFeatureConfig().isAllowedBlock(bridgeBlockData) && !bridgeBlock.getType().isAir()) {
                container.set(expectedBridgeDirectionKey, BRIDGE_EXPECTED_DIRECTION_DATA_TYPE, blockFace);
                return bridgeBlockData;
            }
        }

        if (sign.getY() + 1 < sign.getWorld().getMaxHeight()) {
            blockFace = BlockFace.UP;
            final @NotNull Block above = signBlock.getRelative(blockFace);
            final @NotNull BlockData bridgeBlockData = signBlock.getBlockData();

            if (getFeatureConfig().isAllowedBlock(bridgeBlockData) && !above.getType().isAir()) {
                container.set(expectedBridgeDirectionKey, BRIDGE_EXPECTED_DIRECTION_DATA_TYPE, blockFace);
                return bridgeBlockData;
            }
        }

        if (sign.getY() > sign.getWorld().getMinHeight()) {
            blockFace = BlockFace.DOWN;
            final @NotNull Block above = signBlock.getRelative(blockFace);
            final @NotNull BlockData bridgeBlockData = signBlock.getBlockData();

            if (getFeatureConfig().isAllowedBlock(bridgeBlockData) && !above.getType().isAir()) {
                container.set(expectedBridgeDirectionKey, BRIDGE_EXPECTED_DIRECTION_DATA_TYPE, blockFace);
                return bridgeBlockData;
            }
        }

        return null;
    }

    private @Nullable Sign getOppositeSign(@NotNull Sign sign) {
        if (sign.getBlockData() instanceof Directional directional) {
            /*
            Slab;
            ArgumentTypes.blockState();

            BlockStateParser.BlockResult blockResult = BlockStateParser.parseForBlock(this.blocks, new StringReader(string), true);
             new BlockInput(blockResult.blockState(), blockResult.properties().keySet(), blockResult.nbt());

            BlockEntity tileEntity = (result.tag == null) ? null : BlockEntity.loadStatic(BlockPos.ZERO, result.getState(), result.tag, CraftRegistry.getMinecraftRegistry());
            return CraftBlockStates.getBlockState(null, BlockPos.ZERO, result.getState(), tileEntity);*/

            return null; // todo

        } else if (sign.getBlockData() instanceof Rotatable rotatable) {
            if (Utils.isCardinal(rotatable)) {

                return null; // todo
            } else { // todo
                return null;
            }
        } else { // unknown
            return null;
        }
    }
}
