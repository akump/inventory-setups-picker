package com.setuppicker;

import java.awt.Color;
import java.awt.event.KeyEvent;
import net.runelite.client.config.Alpha;
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
	String KEY_RECENT = "recentSetups";

	enum Side
	{
		LEFT,
		RIGHT
	}

	@ConfigSection(
		name = "Beside the bank",
		description = "The list shown next to the bank interface",
		position = 20
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

	@ConfigItem(
		keyName = "startOnOpenSetup",
		name = "Start on open setup",
		description = "When a setup is already open, the popup opens with it selected, so pressing Enter closes it."
			+ " When off, the selection always starts at the top.",
		position = 2
	)
	default boolean startOnOpenSetup()
	{
		return true;
	}

	@Range(min = 3, max = 25)
	@ConfigItem(
		keyName = "popupRows",
		name = "Popup rows",
		description = "How many setups the popup shows before it scrolls",
		position = 3
	)
	default int popupRows()
	{
		return 10;
	}

	@ConfigItem(
		keyName = "showIcons",
		name = "Show icons",
		description = "Show each setup's icon next to its name",
		position = 4
	)
	default boolean showIcons()
	{
		return true;
	}

	@ConfigItem(
		keyName = "alphabetical",
		name = "Sort alphabetically",
		description = "List setups by name. When off, they keep the order of the Inventory Setups side panel's list.",
		position = 5
	)
	default boolean alphabetical()
	{
		return true;
	}

	@ConfigItem(
		keyName = "favoritesFirst",
		name = "Favorites first",
		description = "List favorited setups before the rest",
		position = 6
	)
	default boolean favoritesFirst()
	{
		return true;
	}

	@Range(max = 10)
	@ConfigItem(
		keyName = "recentCount",
		name = "Recently used",
		description = "How many of the setups you opened most recently to list at the top, latest first and marked"
			+ " with a clock. With Group by section on they get a Recent heading. 0 turns this off.",
		position = 7
	)
	default int recentCount()
	{
		return 0;
	}

	@ConfigItem(
		keyName = "fuzzySearch",
		name = "Fuzzy search",
		description = "When a search finds nothing as typed, also match names that are a typo away (\"vorkahh\") or that"
			+ " have the typed letters in order (\"vkdh\")",
		position = 8
	)
	default boolean fuzzySearch()
	{
		return true;
	}

	@ConfigItem(
		keyName = "groupBySection",
		name = "Group by section",
		description = "List setups under the sections you put them in in Inventory Setups, with the rest under Unassigned."
			+ " Typing a section's name lists its setups.",
		position = 9
	)
	default boolean groupBySection()
	{
		return true;
	}

	@ConfigItem(
		keyName = "sectionPages",
		name = "Sections as pages",
		description = "With Group by section on, list just the sections, and open one to see its setups on a page of"
			+ " their own. Click the section's name at the top, or press Left or Backspace, to go back. Typing in"
			+ " the list of sections still searches every setup.",
		position = 10
	)
	default boolean sectionPages()
	{
		return false;
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
		keyName = "verticalWhenCollapsed",
		name = "Vertical when collapsed",
		description = "Show the collapsed list as a narrow upright tab against the bank. When off, it collapses to its title bar.",
		section = bankSection,
		position = 3
	)
	default boolean verticalWhenCollapsed()
	{
		return false;
	}

	@ConfigSection(
		name = "Colors",
		description = "Colors of the popup and the list beside the bank, for matching a resource pack",
		position = 30,
		closedByDefault = true
	)
	String colorSection = "colors";

	@Alpha
	@ConfigItem(
		keyName = "backgroundColor",
		name = "Background",
		description = "Background of the list",
		section = colorSection,
		position = 0
	)
	default Color backgroundColor()
	{
		return PickerTheme.DEFAULT_BACKGROUND;
	}

	@ConfigItem(
		keyName = "headerColor",
		name = "Title bar",
		description = "Background of the title bar",
		section = colorSection,
		position = 1
	)
	default Color headerColor()
	{
		return PickerTheme.DEFAULT_HEADER;
	}

	@ConfigItem(
		keyName = "borderColor",
		name = "Border",
		description = "The frame, the search box outline and section dividers",
		section = colorSection,
		position = 2
	)
	default Color borderColor()
	{
		return PickerTheme.DEFAULT_BORDER;
	}

	@ConfigItem(
		keyName = "accentColor",
		name = "Accent",
		description = "The title, the keyboard selection, the focused search box and the open setup",
		section = colorSection,
		position = 3
	)
	default Color accentColor()
	{
		return PickerTheme.DEFAULT_ACCENT;
	}

	@ConfigItem(
		keyName = "textColor",
		name = "Text",
		description = "Setup names that have no color of their own in Inventory Setups",
		section = colorSection,
		position = 4
	)
	default Color textColor()
	{
		return PickerTheme.DEFAULT_TEXT;
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
