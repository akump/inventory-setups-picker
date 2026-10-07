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
		private final boolean querySelected;
		private final int caret;
		private final int scroll;
		private final int selected;

		private final String activeBankTag;
		private final boolean mixed;
		private final boolean scrollbarDragged;
		private final boolean notesShown;

		private View(List<PickerRow> rows, int total, String activeSetup, String activeBankTag, boolean mixed, String query,
			boolean searchFocused, boolean paletteOpen, boolean sectionPages, int scroll, int selected, boolean querySelected,
			int caret, boolean scrollbarDragged, boolean notesShown)
		{
			this.notesShown = notesShown;
			this.scrollbarDragged = scrollbarDragged;
			this.activeBankTag = activeBankTag;
			this.mixed = mixed;
			this.caret = caret;
			this.querySelected = querySelected;
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

		/**
		 * Whether this is the setup that's open in Inventory Setups, or the bank tag that's open in the bank.
		 */
		public boolean isActive(SetupEntry setup)
		{
			return setup.getName().equals(setup.isBankTag() ? activeBankTag : activeSetup);
		}

		/**
		 * Whether both inventory setups and bank tags are listed, so that they need telling apart.
		 */
		public boolean isMixed()
		{
			return mixed;
		}

		/**
		 * Whether setups' notes are shown: a mark on those that have any, and the notes of the one pointed at.
		 */
		public boolean isNotesShown()
		{
			return notesShown;
		}

		/**
		 * Whether the scrollbar is being dragged with the mouse.
		 */
		public boolean isScrollbarDragged()
		{
			return scrollbarDragged;
		}

		public String getQuery()
		{
			return query;
		}

		/**
		 * Whether the whole search text is highlighted, after select all: the next thing typed replaces it.
		 */
		public boolean isQuerySelected()
		{
			return querySelected;
		}

		/**
		 * Where in the search text typing goes: the number of characters before the cursor.
		 */
		public int getCaret()
		{
			return caret;
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
	private String activeBankTag = "";
	// A setup and a bank tag can be open at once. Which was opened later, for the popup to start on.
	private boolean bankTagOpenedLast;
	private boolean mixed;
	private boolean scrollbarDragged;
	private boolean showNotes;
	private String query = "";
	private boolean searchFocused;
	private boolean paletteOpen;
	private int scroll;
	private int selected;
	private int visibleRows = 1;
	private boolean fuzzySearch;
	private boolean querySelected;
	private int caret;
	private boolean startOnActiveSetup = true;
	// Set while the popup should keep its starting selection in view, until the user scrolls or searches
	private boolean revealSelection;
	private int scrollBeforePalette;
	private boolean sectionPages;
	// heading of the section whose page is showing, or null for the list of sections
	private PickerRow openSection;
	private PickerRow sectionBeforePalette;

	public synchronized View view()
	{
		return new View(filtered, total, activeSetup, activeBankTag, mixed, query, searchFocused, paletteOpen, isPaged(),
			scroll, selected, querySelected, caret, scrollbarDragged, showNotes);
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
			caret = 0;
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
		caret = 0;
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
		boolean setups = false;
		boolean bankTags = false;
		for (PickerRow row : rows)
		{
			if (!row.isHeader())
			{
				setups |= !row.getSetup().isBankTag();
				bankTags |= row.getSetup().isBankTag();
			}
		}
		mixed = setups && bankTags;
		// a reload (e.g. after a setup is edited) shouldn't move the list under the user
		refilter(false);
	}

	/**
	 * Whether to show the notes setups have in Inventory Setups.
	 */
	public synchronized void setShowNotes(boolean show)
	{
		showNotes = show;
	}

	/**
	 * Whether the popup opens with the selection on the setup that's already open, rather than at the top.
	 */
	public synchronized void setStartOnActiveSetup(boolean start)
	{
		startOnActiveSetup = start;
	}

	/**
	 * Whether a search that finds nothing as typed may match names that are a typo or a few letters away.
	 */
	public synchronized void setFuzzySearch(boolean fuzzy)
	{
		if (fuzzySearch != fuzzy)
		{
			fuzzySearch = fuzzy;
			refilter(false);
		}
	}

	public synchronized void setActiveSetup(String name)
	{
		final String active = name == null ? "" : name;
		if (!active.isEmpty() && !active.equals(activeSetup))
		{
			bankTagOpenedLast = false;
		}
		activeSetup = active;
	}

	/**
	 * @param name the bank tag that's open in the bank, or "" for none
	 */
	public synchronized void setActiveBankTag(String name)
	{
		final String active = name == null ? "" : name;
		if (!active.isEmpty() && !active.equals(activeBankTag))
		{
			bankTagOpenedLast = true;
		}
		activeBankTag = active;
	}

	private boolean isActive(SetupEntry setup)
	{
		return setup.getName().equals(setup.isBankTag() ? activeBankTag : activeSetup);
	}

	/**
	 * Told by the overlay how many rows fit, so scrolling can be clamped to it.
	 */
	public synchronized void setVisibleRows(int rows)
	{
		visibleRows = Math.max(1, rows);
		if (revealSelection && selected >= scroll + visibleRows)
		{
			scroll = selected - visibleRows + 1;
		}
		clamp();
	}

	public synchronized void scrollBy(int rows)
	{
		scrollTo(scroll + rows);
	}

	/**
	 * @param row the row to have at the top, as near as the list can be scrolled to it
	 */
	public synchronized void scrollTo(int row)
	{
		revealSelection = false;
		scroll = row;
		clamp();
	}

	/**
	 * Whether the scrollbar is being dragged, for it to be drawn as held.
	 */
	public synchronized void setScrollbarDragged(boolean dragged)
	{
		scrollbarDragged = dragged;
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
		caret = 0;
		refilter(true);
		selectActiveSetup();
	}

	/**
	 * Start on the setup that's already open, when it's listed, so that it only takes Enter to close it. With
	 * both a setup and a bank tag open, it's whichever was opened later.
	 */
	private void selectActiveSetup()
	{
		if (!startOnActiveSetup)
		{
			return;
		}
		int open = -1;
		for (int i = 0; i < filtered.size(); i++)
		{
			final PickerRow row = filtered.get(i);
			if (row.isHeader() || !isActive(row.getSetup()))
			{
				continue;
			}
			if (row.getSetup().isBankTag() == bankTagOpenedLast)
			{
				open = i;
				break;
			}
			// the one opened earlier will do if the later one isn't listed
			if (open < 0)
			{
				open = i;
			}
		}
		if (open >= 0)
		{
			selected = open;
			// The popup's size isn't known until it is next drawn, so it is scrolled into view then
			revealSelection = true;
		}
	}

	/**
	 * Highlight the whole search text, so that typing replaces it and Backspace clears it.
	 */
	public synchronized void selectQuery()
	{
		querySelected = searchFocused && !query.isEmpty();
	}

	/**
	 * Move the cursor through the search text. After select all, it lands at the end moved towards.
	 *
	 * @param delta characters to move by; anything past either end stops there
	 */
	public synchronized void moveCaret(int delta)
	{
		if (querySelected)
		{
			querySelected = false;
			caret = delta < 0 ? 0 : query.length();
			return;
		}
		caret = (int) Math.max(0, Math.min((long) caret + delta, query.length()));
	}

	/**
	 * Delete the character after the cursor, or everything after select all.
	 */
	public synchronized void deleteForward()
	{
		if (querySelected)
		{
			query = "";
			caret = 0;
		}
		else if (caret < query.length())
		{
			query = query.substring(0, caret) + query.substring(caret + 1);
		}
		else
		{
			return;
		}
		refilter(true);
	}

	public synchronized void typeChar(char c)
	{
		if (querySelected)
		{
			query = "";
			caret = 0;
		}
		query = query.substring(0, caret) + c + query.substring(caret);
		caret++;
		refilter(true);
	}

	public synchronized void backspace()
	{
		if (!query.isEmpty())
		{
			if (querySelected)
			{
				query = "";
				caret = 0;
			}
			else if (caret > 0)
			{
				query = query.substring(0, caret - 1) + query.substring(caret);
				caret--;
			}
			else
			{
				// at the start of the text: nothing before the cursor to delete
				return;
			}
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
		caret = 0;
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
		// a reload of the setups leaves the highlight alone; anything that changed the search has used it up
		querySelected &= !resetPosition && !query.isEmpty();
		revealSelection = false;
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
			List<PickerRow> matches = match(needle, SetupMatcher.Tier.EXACT);
			if (fuzzySearch)
			{
				// Only when the search would otherwise find nothing: allow for a typo, then for letters left out
				if (matches.isEmpty())
				{
					matches = match(needle, SetupMatcher.Tier.TYPO);
				}
				if (matches.isEmpty())
				{
					matches = match(needle, SetupMatcher.Tier.SUBSEQUENCE);
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

	private List<PickerRow> match(String needle, SetupMatcher.Tier tier)
	{
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
				sectionMatches = !row.isBuiltIn()
					&& SetupMatcher.matches(row.getSectionName().toLowerCase(Locale.ROOT), needle, tier);
			}
			else if (sectionMatches || SetupMatcher.matches(row.getSetup().getName().toLowerCase(Locale.ROOT), needle, tier))
			{
				if (pendingHeader != null)
				{
					matches.add(pendingHeader);
					pendingHeader = null;
				}
				matches.add(row);
			}
		}
		return matches;
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
