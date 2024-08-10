package de.greensurvivors.greenbook.features.wireless.network;

import de.greensurvivors.greenbook.GreenBook;
import de.greensurvivors.greenbook.utils.Utils;
import net.kyori.adventure.text.Component;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.LecternBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.LecternBlockEntity;
import org.bukkit.Location;
import org.bukkit.NamespacedKey;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.block.BlockFace;
import org.bukkit.block.BlockType;
import org.bukkit.block.Lectern;
import org.bukkit.block.data.type.WallSign;
import org.bukkit.craftbukkit.CraftWorld;
import org.bukkit.craftbukkit.block.impl.CraftLectern;
import org.bukkit.craftbukkit.inventory.CraftItemStack;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.ItemType;
import org.bukkit.inventory.LecternInventory;
import org.bukkit.inventory.meta.BookMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.Range;

/**
 * This wireless receiver will output the power state
 * it receives from its network via its attached block.
 * Be careful how many times you call {@link #setPowerState(byte)} as it might be pretty slow.
 */
public class FineWirelessReceiver extends AWirelessReceiver {
    private final @NotNull NamespacedKey bookKey;
    private final @NotNull Location attachedBlockLoc;

    /**
     * Creates a new fine wireless receiver.
     *
     * @param plugin   the greenBook plugin
     * @param location the location of the receiver
     * @param network  the network the receiver belongs to
     * @throws IllegalStateException when the block at the given location is not a wallSign or the attached block is not a lectern.
     */
    @SuppressWarnings("UnstableApiUsage") // block type
    protected FineWirelessReceiver(@NotNull GreenBook plugin, @NotNull Location location, @NotNull WirelessNetwork network) throws IllegalStateException {
        super(plugin, location, network);

        bookKey = new NamespacedKey(plugin, "bookData");

        if (location.getBlock().getBlockData() instanceof WallSign wallSign) { // todo move this check up before super when it becomes possible
            BlockFace face = wallSign.getFacing().getOppositeFace();
            attachedBlockLoc = location.add(face.getModX(), 0, face.getModZ());

            if (BlockType.LECTERN.asMaterial() == attachedBlockLoc.getBlock().getType() &&
                attachedBlockLoc.getBlock().getState(false) instanceof Lectern lectern &&
                lectern.getInventory() instanceof LecternInventory lecternInventory) {

                ItemStack book = lecternInventory.getBook();

                if (book != null && !book.isEmpty() && book.getType() == ItemType.WRITABLE_BOOK.asMaterial() &&
                    book.getItemMeta() instanceof BookMeta bookMeta) {

                    // fill pages with numbers until page count reaches 15
                    for (int i = bookMeta.getPageCount()-1; i < 15; i++) {
                        bookMeta.addPages(Component.text(i));
                    }

                    book.setItemMeta(bookMeta);
                    lecternInventory.setBook(book);
                    lectern.update(true, false);
                }
            } else { // this should already be checked in {@link AWirelessReceiver#createReceiver}
                throw new IllegalStateException("FineWirelessReceiver was not attached to a lectern at : " + attachedBlockLoc);
            }
        } else {
            throw new IllegalStateException("Receiver was not a wallSign at : " + location);
        }
    }

    /**
     * Repeats the given signal strength as the output of the block this receiver is attached to.
     *
     * @param newSignalStrength the new signal strength
     */
    @SuppressWarnings("UnstableApiUsage") // block type
    @Override
    public void setPowerState(@Range(from = 0, to = 15) byte newSignalStrength) { // todo check if we need a cooldown here
        if (attachedBlockLoc.getBlock().getType() == BlockType.LECTERN.asMaterial() &&
            attachedBlockLoc.getBlock().getState(false) instanceof Lectern lectern &&
            lectern.getInventory() instanceof LecternInventory lecternInventory) {
            ItemStack book = lecternInventory.getBook();
            final PersistentDataContainer container = lectern.getPersistentDataContainer();

            if (book != null && !book.isEmpty()) {
                setLecternPage(newSignalStrength, lectern, lecternInventory, book, container);
            } else { // no book in lectern
                if (newSignalStrength > 0) {
                    // we use #serializeAsBytes() here since paper recommends it, as it will have a correct DFU.
                    // this means you can't get a human-readable representation when using /data get block
                    final byte @Nullable [] bookData = container.get(bookKey, PersistentDataType.BYTE_ARRAY);

                    if (bookData != null) {
                        // removing it, even if it wasn't a book to remove invalid Data
                        container.remove(bookKey);

                        book = ItemStack.deserializeBytes(bookData);

                        // we can't set a book via API, so we have to dig into nms.
                        // don't trust lecternInventory.setBook(book) it will NOT work unless there was already a book in the lectern.
                        BlockPos pos = new BlockPos(lectern.getX(), lectern.getY(), lectern.getZ());
                        final ServerLevel level = ((CraftWorld) lectern.getWorld()).getHandle();
                        BlockEntity tileentity = ((org.bukkit.craftbukkit.block.CraftLectern)lectern).getTileEntity();

                        if (tileentity instanceof LecternBlockEntity tileentitylectern) {
                            tileentitylectern.setBook(((CraftItemStack)book).handle); // this works because paper backs up every itemStack by a nms equivalent
                            // note getState returns a nms state. This is not the same as a bukkitState.
                            // a nms state does represent block data, while a bukkitState represents the tile entity
                            LecternBlock.resetBookState(null, level, pos, ((org.bukkit.craftbukkit.block.impl.CraftLectern)lectern.getBlockData()).getState(),  true);
                            lectern.getWorld().playSound(lectern.getLocation(), Sound.ITEM_BOOK_PUT, SoundCategory.BLOCKS, 1, 1);
                        }

                        setLecternPage(newSignalStrength, lectern, lecternInventory, book, container);
                        //don't do lectern.update(true, true); as it would overwrite the book we set
                    }
                } else {
                    // nothing to do, the book is already gone
                }
            }
        } else { // how ??
            plugin.getLogger().warning("FineWirelessReceiver was not attached to a lectern. This should never happen.");
        }
    }

    /**
     * Helper methode.
     * Sets the page of a lectern based on the signal strength. If the signal strength is greater than 0,
     * the page is calculated based on the number of pages in the book. If the signal strength is 0,
     * the book is removed from the lectern and stored in the persistent data container.
     *
     * @param newSignalStrength the new signal strength
     * @param lectern the lectern to set the page of
     * @param lecternInventory the inventory of the lectern
     * @param book the book in the lectern
     * @param container the persistent data container of the lectern
     */
    private void setLecternPage(@Range(from = 0, to = 15) byte newSignalStrength,
                                @NotNull Lectern lectern,
                                @NotNull LecternInventory lecternInventory,
                                @NotNull ItemStack book,
                                @NotNull PersistentDataContainer container) {
        if (book.getItemMeta() instanceof BookMeta bookMeta) {
            int pageCount = bookMeta.getPageCount();

            if (newSignalStrength > 0) {
                if (pageCount == 1) {
                    // with a page of 1, the signal strength is always 15 (or 0 if the book is missing)
                    // nothing to do
                } else if (pageCount > 1) { // signalStrength = (int)(page / (pageCount - 1) * 14) + 1
                    int page;
                    if (pageCount > 14) {
                        // same as (int) Math.ceil((newSignalStrength - 1.0D) / 14.0D * (pageCount - 1.0D)) but with more precision and faster
                        page = Utils.fastDivCeil((newSignalStrength - 1) * (pageCount - 1),  14);
                    } else { // we don't need floor, since int division is the same, as long as the number is positive
                        page = (newSignalStrength - 1)  * (pageCount - 1) / 14;
                    }

                    lectern.setPage(page);
                    //don't do lectern.update(true, true); as it would overwrite the book we set
                } else { // pageCount <= 0 ??
                    // we don't construct the string here since in most cases the debug message will not get printed.
                    plugin.getComponentLogger().debug("Book has no pages in lectern at : {}", attachedBlockLoc);
                }
            } else { // remove book
                // we use #serializeAsBytes() here since paper recommends it, as it will have a correct DFU.
                // this means you can't get a human-readable representation when using /data get block
                container.set(bookKey, PersistentDataType.BYTE_ARRAY, book.serializeAsBytes());


                // we can't remove the book via API, so we have to dig into nms.
                LecternBlock.resetBookState(null, ((CraftWorld)lectern.getWorld()).getHandle(), new BlockPos(lectern.getX(), lectern.getY(), lectern.getZ()), ((CraftLectern)lectern.getBlockData()).getState(),  false);

                // this alone will not be enough and leave the lectern in an invalid state!
                lecternInventory.setBook(ItemStack.empty());
                //don't do lectern.update(true, true); as it would overwrite the book we just removed.
            }
        } else {
            plugin.getComponentLogger().warn("Invalid book in lectern at : " + attachedBlockLoc);
        }
    }
}
