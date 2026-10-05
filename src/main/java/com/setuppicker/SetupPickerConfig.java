package com.setuppicker;

import java.awt.event.KeyEvent;
import net.runelite.client.config.Config;
import net.runelite.client.config.ConfigGroup;
import net.runelite.client.config.ConfigItem;
import net.runelite.client.config.ConfigSection;
import net.runelite.client.config.Keybind;
import net.runelite.client.config.Range;

@ConfigGroup(SetupPickerConfig.GROUP)
public interface SetupPickerConfig extends Config
{
	String GROUP = "setuppicker";
	String KEY_COLLAPSED = "collapsed";

	enum Side
	{
		LEFT,
		RIGHT
	}

	@ConfigSection(
		name = "Beside the bank",
		description = "The list shown next to the bank interface",
		position = 10
	)
	String bankSection = "bank";

	@ConfigItem(
		keyName = "hotkey",
		name = "Open hotkey",
		description = "Opens the setup list anywhere in game. Only the key is used; the modifier comes from the next setting."
			+ " Type to filter, Up/Down to move, Enter to open the setup (or close it, if it's the one already open),"
			+ " Esc to cancel.",
		position = 0
	)
	default Keybind openKey()
	{
		return new Keybind(KeyEvent.VK_K, 0);
	}

	@ConfigItem(
		keyName = "requireModifier",
		name = "Require Ctrl/Cmd",
		description = "Require the platform modifier (Cmd on Mac, Ctrl on Windows) together with the open hotkey."
			+ " Follows the Platform Keys plugin's key profile when that plugin is on.",
		position = 1
	)
	default boolean requireModifier()
	{
		return true;
	}

	@Range(min = 3, max = 25)
	@ConfigItem(
		keyName = "popupRows",
		name = "Popup rows",
		description = "How many setups the popup shows before it scrolls",
		position = 2
	)
	default int popupRows()
	{
		return 10;
	}

	@ConfigItem(
		keyName = "showIcons",
		name = "Show icons",
		description = "Show each setup's icon next to its name",
		position = 3
	)
	default boolean showIcons()
	{
		return true;
	}

	@ConfigItem(
		keyName = "alphabetical",
		name = "Sort alphabetically",
		description = "List setups by name. When off, they keep the order of the Inventory Setups side panel's list.",
		position = 4
	)
	default boolean alphabetical()
	{
		return true;
	}

	@ConfigItem(
		keyName = "favoritesFirst",
		name = "Favorites first",
		description = "List favorited setups before the rest",
		position = 5
	)
	default boolean favoritesFirst()
	{
		return true;
	}

	@ConfigItem(
		keyName = "showBesideBank",
		name = "Show beside bank",
		description = "Also show the setup list next to the bank while it is open",
		section = bankSection,
		position = 0
	)
	default boolean showBesideBank()
	{
		return true;
	}

	@ConfigItem(
		keyName = "side",
		name = "Bank side",
		description = "Which side of the bank the list sits on. Falls back to the other side when there is no room.",
		section = bankSection,
		position = 1
	)
	default Side side()
	{
		return Side.LEFT;
	}

	@Range(min = 110, max = 300)
	@ConfigItem(
		keyName = "width",
		name = "Width",
		description = "Width of the list beside the bank in pixels",
		section = bankSection,
		position = 2
	)
	default int width()
	{
		return 160;
	}

	@ConfigItem(
		keyName = KEY_COLLAPSED,
		name = "",
		description = "",
		hidden = true
	)
	default boolean collapsed()
	{
		return false;
	}
}
