package com.setuppicker;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

/**
 * State of the picker. Written from the client thread (setup data, rendering) and the AWT thread
 * (mouse and keyboard), so every access is synchronized and readers work from an immutable {@link View}.
 */
public class PickerModel
{
	/**
	 * Immutable snapshot of what the picker should currently show.
	 */
	public static final class View
	{
		private final List<SetupEntry> setups;
		private final int total;
		private final String activeSetup;
		private final String query;
		private final boolean searchFocused;
		private final boolean paletteOpen;
		private final int scroll;
		private final int selected;

		private View(List<SetupEntry> setups, int total, String activeSetup, String query, boolean searchFocused,
			boolean paletteOpen, int scroll, int selected)
		{
			this.setups = setups;
			this.total = total;
			this.activeSetup = activeSetup;
			this.query = query;
			this.searchFocused = searchFocused;
			this.paletteOpen = paletteOpen;
			this.scroll = scroll;
			this.selected = selected;
		}

		/**
		 * The setups matching the search, in display order.
		 */
		public List<SetupEntry> getSetups()
		{
			return setups;
		}

		/**
		 * Number of setups before the search was applied.
		 */
		public int getTotal()
		{
			return total;
		}

		public String getActiveSetup()
		{
			return activeSetup;
		}

		public String getQuery()
		{
			return query;
		}

		public boolean isSearchFocused()
		{
			return searchFocused;
		}

		/**
		 * Whether the hotkey-opened popup is showing. It always has search focus while open.
		 */
		public boolean isPaletteOpen()
		{
			return paletteOpen;
		}

		/**
		 * Index into {@link #getSetups()} of the first visible row.
		 */
		public int getScroll()
		{
			return scroll;
		}

		/**
		 * Index into {@link #getSetups()} of the keyboard selection. Only meaningful while searching.
		 */
		public int getSelected()
		{
			return selected;
		}
	}

	private List<SetupEntry> setups = Collections.emptyList();
	private List<SetupEntry> filtered = Collections.emptyList();
	private String activeSetup = "";
	private String query = "";
	private boolean searchFocused;
	private boolean paletteOpen;
	private int scroll;
	private int selected;
	private int visibleRows = 1;
	private int scrollBeforePalette;

	public synchronized View view()
	{
		return new View(filtered, setups.size(), activeSetup, query, searchFocused, paletteOpen, scroll, selected);
	}

	public synchronized void setSetups(List<SetupEntry> newSetups)
	{
		setups = Collections.unmodifiableList(new ArrayList<>(newSetups));
		// a reload (e.g. after a setup is edited) shouldn't move the list under the user
		refilter(false);
	}

	public synchronized void setActiveSetup(String name)
	{
		activeSetup = name == null ? "" : name;
	}

	/**
	 * Told by the overlay how many rows fit, so scrolling can be clamped to it.
	 */
	public synchronized void setVisibleRows(int rows)
	{
		visibleRows = Math.max(1, rows);
		clamp();
	}

	public synchronized void scrollBy(int rows)
	{
		scroll += rows;
		clamp();
	}

	/**
	 * Focus or unfocus the search box of the list beside the bank.
	 */
	public synchronized void setSearchFocused(boolean focused)
	{
		searchFocused = focused;
		if (focused)
		{
			selected = 0;
			scroll = 0;
		}
	}

	/**
	 * Show the popup with an empty search, ready for typing.
	 */
	public synchronized void openPalette()
	{
		if (!paletteOpen)
		{
			scrollBeforePalette = query.isEmpty() ? scroll : 0;
		}
		paletteOpen = true;
		searchFocused = true;
		query = "";
		refilter(true);
	}

	public synchronized void typeChar(char c)
	{
		query += c;
		refilter(true);
	}

	public synchronized void backspace()
	{
		if (!query.isEmpty())
		{
			query = query.substring(0, query.length() - 1);
			refilter(true);
		}
	}

	public synchronized void moveSelection(int delta)
	{
		selected += delta;
		clamp();
		// keep the selection on screen
		if (selected < scroll)
		{
			scroll = selected;
		}
		else if (selected >= scroll + visibleRows)
		{
			scroll = selected - visibleRows + 1;
		}
	}

	/**
	 * Move the selection by whole pages of visible rows.
	 */
	public synchronized void pageSelection(int pages)
	{
		moveSelection(pages * visibleRows);
	}

	/**
	 * The setup the keyboard selection is on, or null when nothing matches the search.
	 */
	public synchronized SetupEntry getSelectedSetup()
	{
		return selected < filtered.size() ? filtered.get(selected) : null;
	}

	/**
	 * Drop the search text and focus and close the popup, e.g. after opening a setup.
	 */
	public synchronized void resetSearch()
	{
		// Back to the full list. If it was already showing, as when a setup is clicked in the list beside
		// the bank, it stays scrolled where it was.
		final boolean wasFiltered = !query.isEmpty();
		searchFocused = false;
		query = "";
		refilter(wasFiltered);
		if (paletteOpen)
		{
			// the popup scrolls on its own; put the list beside the bank back where it was left
			paletteOpen = false;
			scroll = scrollBeforePalette;
			clamp();
		}
	}

	/**
	 * @param resetPosition go back to the top, for when what's listed has changed because of the search
	 */
	private void refilter(boolean resetPosition)
	{
		if (query.isEmpty())
		{
			filtered = setups;
		}
		else
		{
			final String needle = query.toLowerCase(Locale.ROOT);
			final List<SetupEntry> matches = new ArrayList<>();
			for (SetupEntry setup : setups)
			{
				if (setup.getName().toLowerCase(Locale.ROOT).contains(needle))
				{
					matches.add(setup);
				}
			}
			filtered = Collections.unmodifiableList(matches);
		}
		if (resetPosition)
		{
			selected = 0;
			scroll = 0;
		}
		clamp();
	}

	private void clamp()
	{
		scroll = Math.max(0, Math.min(scroll, filtered.size() - visibleRows));
		selected = Math.max(0, Math.min(selected, filtered.size() - 1));
	}
}
