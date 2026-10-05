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
		private final List<PickerRow> rows;
		private final int total;
		private final String activeSetup;
		private final String query;
		private final boolean searchFocused;
		private final boolean paletteOpen;
		private final boolean sectionPages;
		private final int scroll;
		private final int selected;

		private View(List<PickerRow> rows, int total, String activeSetup, String query, boolean searchFocused,
			boolean paletteOpen, boolean sectionPages, int scroll, int selected)
		{
			this.sectionPages = sectionPages;
			this.rows = rows;
			this.total = total;
			this.activeSetup = activeSetup;
			this.query = query;
			this.searchFocused = searchFocused;
			this.paletteOpen = paletteOpen;
			this.scroll = scroll;
			this.selected = selected;
		}

		/**
		 * The setups matching the search in display order, each run of them under its section heading when
		 * they are grouped by section.
		 */
		public List<PickerRow> getRows()
		{
			return rows;
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
		 * Whether section headings can be picked, to open the section's own page or to go back from it.
		 */
		public boolean isSectionPages()
		{
			return sectionPages;
		}

		/**
		 * Index into {@link #getRows()} of the first visible row.
		 */
		public int getScroll()
		{
			return scroll;
		}

		/**
		 * Index into {@link #getRows()} of the keyboard selection, which is only on a heading if those can be
		 * picked. Only meaningful while searching.
		 */
		public int getSelected()
		{
			return selected;
		}
	}

	private List<PickerRow> rows = Collections.emptyList();
	private List<PickerRow> filtered = Collections.emptyList();
	private int total;
	private String activeSetup = "";
	private String query = "";
	private boolean searchFocused;
	private boolean paletteOpen;
	private int scroll;
	private int selected;
	private int visibleRows = 1;
	private int scrollBeforePalette;
	private boolean sectionPages;
	// heading of the section whose page is showing, or null for the list of sections
	private PickerRow openSection;
	private PickerRow sectionBeforePalette;

	public synchronized View view()
	{
		return new View(filtered, total, activeSetup, query, searchFocused, paletteOpen, isPaged(), scroll, selected);
	}

	/**
	 * Show each section on a page of its own, reached from a list of the sections, rather than everything in
	 * one list. Makes no difference when the setups aren't grouped by section.
	 */
	public synchronized void setSectionPages(boolean pages)
	{
		if (sectionPages != pages)
		{
			sectionPages = pages;
			openSection = null;
			sectionBeforePalette = null;
			refilter(true);
		}
	}

	/**
	 * Go to a section's page.
	 *
	 * @param header the section's heading, from the rows of a {@link View}
	 */
	public synchronized void openSection(PickerRow header)
	{
		if (isPaged() && header.isHeader())
		{
			openSection = header;
			query = "";
			refilter(true);
		}
	}

	/**
	 * Go back from a section's page to the list of sections, with the section that was open selected.
	 */
	public synchronized void closeSection()
	{
		final PickerRow closed = openSection;
		if (closed == null)
		{
			return;
		}
		openSection = null;
		query = "";
		refilter(true);
		for (int i = 0; i < filtered.size(); i++)
		{
			if (filtered.get(i).isHeader() && filtered.get(i).sameSection(closed))
			{
				moveSelection(i - selected);
				break;
			}
		}
	}

	/**
	 * List the setups as they are, without section headings.
	 */
	public synchronized void setSetups(List<SetupEntry> setups)
	{
		final List<PickerRow> newRows = new ArrayList<>(setups.size());
		for (SetupEntry setup : setups)
		{
			newRows.add(PickerRow.of(setup));
		}
		setRows(newRows, setups.size());
	}

	/**
	 * @param newRows  the setups to list. Every heading among them must be followed by at least one setup.
	 * @param newTotal number of setups there are, which is less than the setup rows when some are in several sections
	 */
	public synchronized void setRows(List<PickerRow> newRows, int newTotal)
	{
		rows = Collections.unmodifiableList(new ArrayList<>(newRows));
		total = newTotal;
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
			clamp();
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
			sectionBeforePalette = openSection;
		}
		openSection = null;
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
		else
		{
			// nothing left to delete: back out of the section's page
			closeSection();
		}
	}

	public synchronized void moveSelection(int delta)
	{
		final int target = Math.max(0, Math.min(selected + delta, filtered.size() - 1));
		// going up, step over a heading to the setup above it. Going down is clamp's job.
		selected = isSkipped(target) && delta < 0 && target > 0 ? target - 1 : target;
		clamp();
		// keep the selection on screen, along with its heading when it's the first of a section
		if (selected < scroll)
		{
			scroll = isSkipped(selected - 1) && visibleRows > 1 ? selected - 1 : selected;
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
		return selected < filtered.size() ? filtered.get(selected).getSetup() : null;
	}

	/**
	 * The row the keyboard selection is on, or null when nothing is listed.
	 */
	public synchronized PickerRow getSelectedRow()
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
		if (paletteOpen)
		{
			openSection = sectionBeforePalette;
		}
		refilter(wasFiltered);
		if (paletteOpen)
		{
			// the popup scrolls and changes page on its own; put the list beside the bank back where it was left
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
		final List<PickerRow> page = isPaged() ? page() : null;
		if (page != null)
		{
			filtered = page;
		}
		else if (query.isEmpty())
		{
			filtered = rows;
		}
		else
		{
			final String needle = query.toLowerCase(Locale.ROOT);
			final List<PickerRow> matches = new ArrayList<>();
			// the heading of the section being gone through, until one of its setups matches and it gets listed
			PickerRow pendingHeader = null;
			boolean sectionMatches = false;
			for (PickerRow row : rows)
			{
				if (row.isHeader())
				{
					pendingHeader = row;
					// a section's name finds all of its setups
					sectionMatches = row != PickerRow.UNASSIGNED && row.getSectionName().toLowerCase(Locale.ROOT).contains(needle);
				}
				else if (sectionMatches || row.getSetup().getName().toLowerCase(Locale.ROOT).contains(needle))
				{
					if (pendingHeader != null)
					{
						matches.add(pendingHeader);
						pendingHeader = null;
					}
					matches.add(row);
				}
			}
			filtered = Collections.unmodifiableList(matches);
		}
		if (resetPosition)
		{
			// start on the first setup rather than on its heading, even where headings can be picked
			selected = filtered.size() > 1 && filtered.get(0).isHeader() && !filtered.get(1).isHeader() ? 1 : 0;
			scroll = 0;
		}
		clamp();
	}

	/**
	 * Whether sections are being shown as pages: they have to be both wanted and there.
	 */
	private boolean isPaged()
	{
		return sectionPages && !rows.isEmpty() && rows.get(0).isHeader();
	}

	/**
	 * What to list when sections are pages, or null when it's the same as when they aren't: a search across
	 * all of them.
	 */
	private List<PickerRow> page()
	{
		final String needle = query.toLowerCase(Locale.ROOT);
		final List<PickerRow> page = new ArrayList<>();
		if (openSection == null)
		{
			if (!query.isEmpty())
			{
				return null;
			}
			// the list of sections
			for (PickerRow row : rows)
			{
				if (row.isHeader())
				{
					page.add(row);
				}
			}
			return Collections.unmodifiableList(page);
		}

		boolean inSection = false;
		for (PickerRow row : rows)
		{
			if (row.isHeader())
			{
				inSection = row.sameSection(openSection);
				if (inSection)
				{
					page.add(PickerRow.backFrom(row));
				}
			}
			else if (inSection && row.getSetup().getName().toLowerCase(Locale.ROOT).contains(needle))
			{
				page.add(row);
			}
		}
		if (page.isEmpty())
		{
			// the section has gone, or has no setups left
			openSection = null;
			return page();
		}
		return Collections.unmodifiableList(page);
	}

	private void clamp()
	{
		scroll = Math.max(0, Math.min(scroll, filtered.size() - visibleRows));
		selected = Math.max(0, Math.min(selected, filtered.size() - 1));
		if (isSkipped(selected))
		{
			// a heading always has a setup below it
			selected++;
		}
	}

	/**
	 * Whether the row is a heading that the selection can't rest on.
	 */
	private boolean isSkipped(int index)
	{
		return !isPaged() && index >= 0 && index < filtered.size() && filtered.get(index).isHeader();
	}
}
