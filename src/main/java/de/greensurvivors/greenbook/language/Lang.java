package de.greensurvivors.greenbook.language;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.JoinConfiguration;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public enum Lang implements Cons {
	//wireless redstone
	//technical values
	SIGN_TRANSMITTER_ID("Mc1110"),
	SIGN_TRANSMITTER_NAME("Transmitter"),
	SIGN_RECEIVER_ID("Mc1111"),
	SIGN_RECEIVER_NAME("Reveiver"),
	//placing feedback
	NO_WALLSIGN("You have to place this at a wall."),

	// /coin command
	COIN_STOSS_SELF(String.format("&e%s &cdied, by trying to toss themselves a coin.", Lang.PLAYER)),
	COIN_TOSS_OTHER(String.format("&e%s &2tossed a coin to &e%s&2.", Lang.PLAYER, Lang.PLAYER2)),
	COIN_SET(String.format("&2Set coin item successfully to &6'&e%s&6'", VALUE)),
	COIN_NOT_ENOUGH("&cYou have not enough coins."),

	//lift - elevator
	//using feedback
	LIFT_CREATE_SUCCESS("&2Lift was successfully created."),
	LIFT_USED_STOP("&cYou cant depart from this kind of Lift."),
	LIFT_USED_UP("&6Moved a floor up."),
	LIFT_USED_DOWN("&6Moved a floor down."),
	LIFT_USED_FLOOR(String.format("&6Moved to floor %s", VALUE)),
	LIFT_DESTINATION_OBSTRUCTED("&cThe floor ist obstructed."),
	LIFT_DESTINATION_UNKNOWN("&cThis lift has no destination."),

	//readable bookshelfs
	//usage feedback
	SHELF_USE("&7You took a book."),
	//command feedback
	SHELF_ADD_BOOK("&2Successfully added a Book to the library:"),
	SHELF_REMOVED_BOOK("&2Successfully removed Book:"),
	SHELF_NO_BOOK(String.format("&2No Book with the id of &e%s&2 exists", VALUE)),
	SHELF_LIST_HEADER(String.format("&6-----------{Books &e%s&6/&e%s}-----------", VALUE, MAX)),
	LIST_FOOTER_OUTER("&2--"),
	LIST_FOOTER_INNER("&2---<*>---"),
	LIST_FOOTER_BACK(String.format("&6<<( &e%s&6 ) ", VALUE)),
	LIST_FOOTER_NEXT(String.format("&6 ( &e%s&6 )>>", VALUE)),
	LIST_FOOTER_NONE("-------"),
	SHELF_SET_EMPTYHAND(String.format("&2Reading books requires empty hand: &e%s", VALUE)),
	SHELF_SET_SNEAK(String.format("&2Reading books requires sneaking: &e%s", VALUE)),

	//painting switcher
	//usage feedback
	PAINTING_EDITING_OUTSIDERANGE("&cYou moved outside editing range and therefore stopped editing the painting."),
	PAINTING_EDITING_STOPPED("&6Stopped editing the painting."),
	PAINTING_EDITING_STARTED("&2Started editing a painting."),
	PAINTING_EDITING_INUSE("&cYou can't edit this painting, while another player is already modifying it."),
	//command feedback
	PAINTING_SET_RANGE(String.format("&2Range successfully set to &6'&e%s&6'&2 Blocks.", VALUE)),

	//help command
	HELP_WIKI("&7See more detailed information on the wiki page."),

	//plugin command
	PLUGIN_HEADER("&a-<(&6GreenTreasure&a)>-"),
	PLUGIN_VERSION(String.format("&aVersion&6: &e%s", VALUE)),

	//greenbook reload
	RELOADED("&aReloaded."),

	//misc
	NOT_PLAYER_SELF("&cYou have to be a player."),
	NO_NUMBER(String.format("&6'&e%s&6' &cis not a valid number.", VALUE)),
	NO_BOOL(String.format("&6'&e%s&6' &cis not a valid boolean.", VALUE)),
	NO_PERMISSION_COMMAND("&cYou have no permission to perform this command."),
	NO_PERMISSION_SOMETHING("&cYou have no permission to do that."),
	NO_SUCH_PLAYER(String.format("&cCould not get a valid player named %s", VALUE)),
	NO_ITEM_HOLDING("&cYou not are holding a item."),
	NOT_ENOUGH_ARGS("&cNot enough arguments."),
	UNKNOWN_ARGUMENT(String.format("&cUnknown or wrong argument &6'&e%s&6'&c. Try &e/fm help", VALUE)),
	UNKNOWN_ERROR("&cUnknown Error. What happened?");

	//the message
	private String value;

	Lang(String value) {
		this.value = value;
	}

	/**
	 * get String, colors are expressed via legacy ampersand
	 * @return value
	 */
	public String get() {
		return value;
	}

	/**
	 * Set value from language file
	 * @param value to set
	 */
	public void set(String value) {
		this.value = value;
	}


	/**
	 * Builds a Component from given text.
	 *
	 * @param args to build from - if null, null is also returned
	 * @return Component
	 */
	public static Component build(@Nullable String args) {
		return build(args, null, null, null, null);
	}

	/**
	 * Build a Component from given text with properties.
	 * There can only be one click action.
	 *
	 * @param text       to build from - if null, null is also returned
	 * @param command    to execute on click
	 * @param hover      to show when mouse hovers over the text
	 * @param suggestion to show on click
	 * @return Component
	 */
	public static Component build(String text, @Nullable String command, @Nullable Object hover, @Nullable String suggestion) {
		return build(text, command, hover, suggestion, null);
	}

	/**
	 * Build a Component from given text with properties.
	 * There can only be one click action.
	 *
	 * @param text       to build from - if null, null is also returned
	 * @param command    to execute on click
	 * @param hover      to show when mouse hovers over the text
	 * @param suggestion to show on click
	 * @param link       to open on click
	 * @return Component
	 */
	public static Component build(@Nullable String text, @Nullable String command, @Nullable Object hover, @Nullable String suggestion, @Nullable String link) {
		if (text != null) {
			TextComponent tc = rgb(Component.text(text));
			// command suggestion
			if (suggestion != null && !suggestion.isBlank()) {
				tc = tc.clickEvent(ClickEvent.suggestCommand(suggestion));
			}
			// run command
			if (command != null && !command.isBlank()) {
				tc = tc.clickEvent(ClickEvent.runCommand(command));
			}
			// open link
			if (link != null && !link.isBlank()) {
				tc = tc.clickEvent(ClickEvent.openUrl(link));
			}
			// hover effect
			if (hover != null) {
				if (hover instanceof Component component)
					tc = tc.hoverEvent(component);
				if (hover instanceof ItemStack item)
					tc = tc.hoverEvent(item.asHoverEvent());
				if (hover instanceof String s)
					tc = tc.hoverEvent(Component.text(s));
			}
			return tc;
		}
		return null;
	}

	/**
	 * Join Components together with new lines as separator.
	 *
	 * @param components to join together
	 * @return Component
	 */
	public static Component join(@NotNull Iterable<? extends Component> components) {
		return Component.join(JoinConfiguration.separator(Component.newline()), components);
	}

	/**
	 * Get a Component colored with rgb colors.
	 *
	 * @param tc to convert colors
	 * @return Component with all color codes converted
	 */
	private static TextComponent rgb(@NotNull TextComponent tc) {
		String text = tc.content();
		if (text.contains("&#")) {
			// find first hexColor
			int i = text.indexOf("&#");
			// substring first part (old color)
			tc = LegacyComponentSerializer.legacyAmpersand().deserialize(text.substring(0, i));
			//alternative: tc = LegacyComponentSerializer.legacy('&').deserialize(text.substring(0, i));

			//explicitly deny unset decorations. If not done the server trys to set them after reload
			//and items with this component as lore can't be stacked.
			for (TextDecoration t : TextDecoration.values()) {
				if (tc.decorations().containsKey(t))
					tc = tc.decoration(t, false);
			}

			// substring last part (new color)
			tc = tc.append(rgb(Component.text(text.substring(i + 8), TextColor.fromHexString(text.substring(i + 1, i + 8)))));
		} else {
			// text with legacy ChatColor
			tc = LegacyComponentSerializer.legacyAmpersand().deserialize(text);
		}
		return tc;
	}
}
