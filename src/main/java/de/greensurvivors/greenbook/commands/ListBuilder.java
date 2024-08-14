package de.greensurvivors.greenbook.commands;

import de.greensurvivors.greenbook.GreenBook;
import de.greensurvivors.greenbook.language.MessageManager;
import de.greensurvivors.greenbook.language.StandardLangPath;
import de.greensurvivors.greenbook.language.StandartPlaceHolders;
import net.kyori.adventure.builder.AbstractBuilder;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.JoinConfiguration;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.Range;

import java.util.ArrayList;
import java.util.List;

public class ListBuilder implements AbstractBuilder<Component> {
    private final @NotNull GreenBook plugin;
    private @NotNull Component title;
    private @Nullable Integer pageNow, maxPages;
    private @Nullable String backCommand, nextCommand, lastCommand;
    private @Nullable List<@NotNull Component> entries;
    private @Nullable Integer entryRangeStart, entryRangeEnd;

    public ListBuilder(final @NotNull GreenBook plugin, final @NotNull Component title) {
        this.plugin = plugin;
        this.title = title;
    }

    public @NotNull ListBuilder paged(final @Nullable @Range(from = 1, to = Integer.MAX_VALUE) Integer pageNow,
                                      final @Nullable @Range(from = 1, to = Integer.MAX_VALUE) Integer maxPages) throws IllegalArgumentException {
        if (pageNow != null && pageNow < 1) {
            throw new IllegalArgumentException("Page number must be at least 1!");
        }
        if (maxPages != null && maxPages < 1) {
            throw new IllegalArgumentException("Maximum page number must be at least 1!");
        }
        if (pageNow != null && maxPages != null && pageNow > maxPages) {
            throw new IllegalArgumentException("Current page can't be bigger as maximum number of pages");
        }

        this.pageNow = pageNow;
        this.maxPages = maxPages;

        return this;
    }

    public @NotNull ListBuilder title(@NotNull Component title) {
        this.title = title;

        return this;
    }

    public @NotNull ListBuilder pageBackCommand(final @NotNull String command) {
        this.backCommand = command;

        return this;
    }

    public @NotNull ListBuilder pageNextCommand(final @NotNull String command) {
        this.nextCommand = command;

        return this;
    }

    public ListBuilder pageLastCommand(@NotNull String command) {
        this.lastCommand = command;
        return this;
    }

    public @NotNull ListBuilder setEntries(final @Nullable List<@NotNull Component> entries) {
        if (entries == null) {
            this.entries = null;
        } else {
            this.entries = new ArrayList<>(entries);
        }

        return this;
    }

    public @NotNull ListBuilder addEntry(final @NotNull Component entry) {
        if (entries == null) {
            entries = new ArrayList<>();
        }

        entries.add(entry);

        return this;
    }

    public @NotNull ListBuilder restrictListToRange(@Nullable @Range(from = 0, to = Integer.MAX_VALUE) Integer from, @Nullable @Range(from = 0, to = Integer.MAX_VALUE) Integer to) {
        if (from != null && from < 0) {
            throw new IllegalArgumentException("From-Index must be at least 0!");
        }
        if (to != null && to < 0) {
            throw new IllegalArgumentException("To-Index must be at least 0!");
        }

        this.entryRangeStart = from;
        this.entryRangeEnd = to;

        return this;
    }

    @Override
    public @NotNull Component build() {
        TextComponent.Builder messageBuilder = Component.text();
        MessageManager messageManager = plugin.getMessageManager();

        // header
        if (pageNow != null && maxPages != null) {
            messageBuilder.append(
                messageManager.getLang(StandardLangPath.LIST_HEADER_PAGED,
                    Placeholder.component(StandartPlaceHolders.TEXT.getPlaceholder(), title),
                    Placeholder.unparsed(StandartPlaceHolders.NUMBER.getPlaceholder(), String.valueOf(pageNow)),
                    Placeholder.component(StandartPlaceHolders.MAX.getPlaceholder(),
                        lastCommand == null ?
                            Component.text(maxPages) :
                            Component.text(maxPages).clickEvent(ClickEvent.suggestCommand(lastCommand))))
            ).appendNewline();
        } else {
            messageBuilder.append(
                messageManager.getLang(StandardLangPath.LIST_HEADER_PLAIN,
                    Placeholder.component(StandartPlaceHolders.TEXT.getPlaceholder(), title))
            ).appendNewline();
        }

        // body
        if (entries != null) {
            if (entryRangeStart == null || entryRangeEnd == null) {
                messageBuilder.append(Component.join(JoinConfiguration.newlines(), entries)
                ).appendNewline();
            } else {
                if (entryRangeStart > entryRangeEnd) {
                    throw new IllegalArgumentException("Range start must be lower or equal than upper bound!");
                }

                if (entryRangeEnd > entries.size()) {
                    if (entryRangeStart >= entries.size()) {
                        throw new IllegalArgumentException("Range out of bound of entries!");
                    } else {
                        // just use max number of entries
                        entryRangeEnd = entries.size() - 1;
                    }
                }

                for (int i = entryRangeStart; i < entryRangeEnd; i++) {
                    messageBuilder.append(entries.get(i)).appendNewline();
                }
            }
        }

        // footer
        messageBuilder.append(messageManager.getLang(StandardLangPath.LIST_FOOTER_OUTER));
        if (pageNow != null && maxPages != null) {
            if (pageNow > 1) {
                messageBuilder.append(messageManager.getLang(StandardLangPath.LIST_FOOTER_BACK,
                    Placeholder.component(StandartPlaceHolders.NUMBER.getPlaceholder(),
                        backCommand == null ?
                            Component.text(pageNow - 1) :
                            Component.text(pageNow - 1).clickEvent(ClickEvent.suggestCommand(backCommand)))
                ));
            } else {
                messageBuilder.append(messageManager.getLang(StandardLangPath.LIST_FOOTER_NONE));
            }

            messageBuilder.append(messageManager.getLang(StandardLangPath.LIST_FOOTER_INNER));

            if (pageNow < maxPages) {
                messageBuilder.append(messageManager.getLang(StandardLangPath.LIST_FOOTER_NEXT,
                    Placeholder.component(StandartPlaceHolders.NUMBER.getPlaceholder(),
                        nextCommand == null ?
                            Component.text(pageNow + 1) :
                            Component.text(pageNow + 1).clickEvent(ClickEvent.suggestCommand(nextCommand)))
                ));
            } else {
                messageBuilder.append(messageManager.getLang(StandardLangPath.LIST_FOOTER_NONE));
            }
        } else {
            messageBuilder.append(messageManager.getLang(StandardLangPath.LIST_FOOTER_NONE));
            messageBuilder.append(messageManager.getLang(StandardLangPath.LIST_FOOTER_INNER));
            messageBuilder.append(messageManager.getLang(StandardLangPath.LIST_FOOTER_NONE));
        }
        messageBuilder.append(messageManager.getLang(StandardLangPath.LIST_FOOTER_OUTER));

        return messageBuilder.build();
    }
}
