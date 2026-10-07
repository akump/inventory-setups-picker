package com.setuppicker;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import org.junit.Before;
import org.junit.Test;

public class PickerModelTest
{
	private PickerModel model;

	@Before
	public void setUp()
	{
		model = new PickerModel();
		final List<SetupEntry> setups = new ArrayList<>();
		for (String name : new String[]{"Vorkath", "Zulrah", "Vardorvis", "ToA 300", "Barrows", "Slayer melee", "Slayer range"})
		{
			setups.add(new SetupEntry(name, false, null, 1));
		}
		model.setSetups(setups);
		model.setVisibleRows(3);
	}

	private void type(String text)
	{
		for (char c : text.toCharArray())
		{
			model.typeChar(c);
		}
	}

	@Test
	public void typingFiltersIgnoringCase()
	{
		model.openPalette();
		type("SLAY");
		assertEquals(2, model.view().getRows().size());
		assertEquals("Slayer melee", model.getSelectedSetup().getName());
		assertEquals(7, model.view().getTotal());

		model.backspace();
		model.backspace();
		model.backspace();
		model.backspace();
		assertEquals(7, model.view().getRows().size());
	}

	@Test
	public void noMatchesSelectsNothing()
	{
		model.openPalette();
		type("zzz");
		assertTrue(model.view().getRows().isEmpty());
		assertNull(model.getSelectedSetup());
		model.moveSelection(1);
		assertNull(model.getSelectedSetup());
	}

	@Test
	public void selectionStaysInRangeAndOnScreen()
	{
		model.openPalette();
		model.moveSelection(-1);
		assertEquals(0, model.view().getSelected());

		model.moveSelection(4);
		assertEquals("Barrows", model.getSelectedSetup().getName());
		// 3 rows fit, so the selection is the last visible one
		assertEquals(2, model.view().getScroll());

		model.pageSelection(5);
		assertEquals("Slayer range", model.getSelectedSetup().getName());
		assertEquals(4, model.view().getScroll());

		model.pageSelection(-1);
		assertEquals(3, model.view().getSelected());
		assertEquals(3, model.view().getScroll());
	}

	@Test
	public void scrollIsClampedToTheList()
	{
		model.scrollBy(100);
		assertEquals(4, model.view().getScroll());
		model.scrollBy(-100);
		assertEquals(0, model.view().getScroll());

		// everything fits: nothing to scroll
		model.setVisibleRows(20);
		model.scrollBy(3);
		assertEquals(0, model.view().getScroll());
	}

	@Test
	public void openingThePaletteStartsFresh()
	{
		model.setSearchFocused(true);
		type("vor");
		model.openPalette();
		assertTrue(model.view().isPaletteOpen());
		assertTrue(model.view().isSearchFocused());
		assertEquals("", model.view().getQuery());
		assertEquals(7, model.view().getRows().size());

		model.resetSearch();
		assertFalse(model.view().isPaletteOpen());
		assertFalse(model.view().isSearchFocused());
	}

	@Test
	public void filterSurvivesSetupsReloading()
	{
		model.openPalette();
		type("vor");
		final List<SetupEntry> reloaded = new ArrayList<>();
		reloaded.add(new SetupEntry("Vorkath", true, null, 1));
		reloaded.add(new SetupEntry("Vorkath (dhcb)", false, null, 1));
		reloaded.add(new SetupEntry("Zulrah", false, null, 1));
		model.setSetups(reloaded);
		assertEquals(2, model.view().getRows().size());
		assertEquals("vor", model.view().getQuery());
	}

	@Test
	public void pickingFromTheUnfilteredListKeepsItsScrollPosition()
	{
		model.scrollBy(4);
		// what clicking a row does, followed by the reload Inventory Setups' config change triggers
		model.resetSearch();
		assertEquals(4, model.view().getScroll());
		model.setRows(model.view().getRows(), 7);
		assertEquals(4, model.view().getScroll());

		// the popup starts at the top, and leaves the list beside the bank where it was
		model.openPalette();
		assertEquals(0, model.view().getScroll());
		type("s");
		model.resetSearch();
		assertEquals(4, model.view().getScroll());
	}

	@Test
	public void clearingAFilterGoesBackToTheTop()
	{
		model.setSearchFocused(true);
		type("a");
		model.scrollBy(1);
		model.resetSearch();
		assertEquals(0, model.view().getScroll());
	}

	@Test
	public void scrollIsPulledBackWhenTheListShrinks()
	{
		model.scrollBy(4);
		model.setRows(model.view().getRows().subList(0, 4), 4);
		assertEquals(1, model.view().getScroll());
	}

	// Bossing: Vorkath, Zulrah / Slayer: Slayer melee, Vorkath / Unassigned: Barrows
	private void setSectionedRows()
	{
		final SetupEntry vorkath = new SetupEntry("Vorkath", false, null, 1);
		final List<PickerRow> rows = new ArrayList<>();
		rows.add(PickerRow.header(new SetupSection("Bossing", null, new ArrayList<>())));
		rows.add(PickerRow.of(vorkath));
		rows.add(PickerRow.of(new SetupEntry("Zulrah", false, null, 1)));
		rows.add(PickerRow.header(new SetupSection("Slayer", null, new ArrayList<>())));
		rows.add(PickerRow.of(new SetupEntry("Slayer melee", false, null, 1)));
		rows.add(PickerRow.of(vorkath));
		rows.add(PickerRow.UNASSIGNED);
		rows.add(PickerRow.of(new SetupEntry("Barrows", false, null, 1)));
		model.setRows(rows, 4);
	}

	private List<String> rowNames()
	{
		final List<String> names = new ArrayList<>();
		for (PickerRow row : model.view().getRows())
		{
			names.add(row.isBack() ? "<" + row.getSectionName() + ">"
				: row.isHeader() ? "[" + row.getSectionName() + "]" : row.getSetup().getName());
		}
		return names;
	}

	@Test
	public void selectionSkipsSectionHeadings()
	{
		setSectionedRows();
		model.openPalette();
		assertEquals(4, model.view().getTotal());
		assertEquals("Vorkath", model.getSelectedSetup().getName());
		assertEquals(1, model.view().getSelected());

		// nothing above the first heading
		model.moveSelection(-1);
		assertEquals(1, model.view().getSelected());

		model.moveSelection(2);
		assertEquals("Slayer melee", model.getSelectedSetup().getName());
		model.moveSelection(-1);
		assertEquals("Zulrah", model.getSelectedSetup().getName());

		model.pageSelection(5);
		assertEquals("Barrows", model.getSelectedSetup().getName());
		assertEquals(5, model.view().getScroll());

		// going back up to the first setup of a section brings its heading into view too
		model.moveSelection(-3);
		assertEquals("Slayer melee", model.getSelectedSetup().getName());
		assertEquals(3, model.view().getScroll());
	}

	@Test
	public void searchKeepsHeadingsOfSectionsWithMatches()
	{
		setSectionedRows();
		model.openPalette();
		type("vor");
		assertEquals(Arrays.asList("[Bossing]", "Vorkath", "[Slayer]", "Vorkath"), rowNames());
		assertEquals("Vorkath", model.getSelectedSetup().getName());

		model.openPalette();
		type("barr");
		assertEquals(Arrays.asList("[Unassigned]", "Barrows"), rowNames());
	}

	@Test
	public void sectionNameFindsAllOfItsSetups()
	{
		setSectionedRows();
		model.openPalette();
		type("boss");
		assertEquals(Arrays.asList("[Bossing]", "Vorkath", "Zulrah"), rowNames());

		// the section's own setups, plus the setup named after it elsewhere
		model.openPalette();
		type("slayer");
		assertEquals(Arrays.asList("[Slayer]", "Slayer melee", "Vorkath"), rowNames());

		// "Unassigned" isn't a section to search for
		model.openPalette();
		type("unass");
		assertTrue(model.view().getRows().isEmpty());
		assertNull(model.getSelectedSetup());
	}

	@Test
	public void sectionsOpenAsPages()
	{
		setSectionedRows();
		model.setSectionPages(true);
		model.openPalette();
		assertTrue(model.view().isSectionPages());
		assertEquals(Arrays.asList("[Bossing]", "[Slayer]", "[Unassigned]"), rowNames());
		assertEquals(4, model.view().getTotal());

		// headings are what there is to pick
		model.moveSelection(1);
		assertEquals("Slayer", model.getSelectedRow().getSectionName());
		assertNull(model.getSelectedSetup());

		model.openSection(model.getSelectedRow());
		assertEquals(Arrays.asList("<Slayer>", "Slayer melee", "Vorkath"), rowNames());
		assertEquals("Slayer melee", model.getSelectedSetup().getName());
		model.moveSelection(-1);
		assertTrue(model.getSelectedRow().isBack());

		// searching a page stays within it
		type("vor");
		assertEquals(Arrays.asList("<Slayer>", "Vorkath"), rowNames());
		assertEquals("Vorkath", model.getSelectedSetup().getName());
		type("zzz");
		assertEquals(Arrays.asList("<Slayer>"), rowNames());

		// deleting past the start of the search goes back, to the section that was open
		for (int i = 0; i < 6; i++)
		{
			model.backspace();
		}
		assertEquals(Arrays.asList("<Slayer>", "Slayer melee", "Vorkath"), rowNames());
		model.backspace();
		assertEquals(Arrays.asList("[Bossing]", "[Slayer]", "[Unassigned]"), rowNames());
		assertEquals("Slayer", model.getSelectedRow().getSectionName());

		model.openSection(PickerRow.UNASSIGNED);
		assertEquals(Arrays.asList("<Unassigned>", "Barrows"), rowNames());
		model.closeSection();
		assertEquals("Unassigned", model.getSelectedRow().getSectionName());
	}

	@Test
	public void searchingTheListOfSectionsFindsSetupsEverywhere()
	{
		setSectionedRows();
		model.setSectionPages(true);
		model.openPalette();
		type("vor");
		assertEquals(Arrays.asList("[Bossing]", "Vorkath", "[Slayer]", "Vorkath"), rowNames());
		// Enter goes for the setup, though its heading could be picked as well
		assertEquals("Vorkath", model.getSelectedSetup().getName());
		model.moveSelection(-1);
		assertEquals("Bossing", model.getSelectedRow().getSectionName());
		model.moveSelection(2);
		assertEquals("Slayer", model.getSelectedRow().getSectionName());
	}

	@Test
	public void popupAndBankListKeepTheirOwnPage()
	{
		setSectionedRows();
		model.setSectionPages(true);
		model.openSection(model.view().getRows().get(0));
		assertEquals(Arrays.asList("<Bossing>", "Vorkath", "Zulrah"), rowNames());

		// the popup starts from the list of sections
		model.openPalette();
		assertEquals(Arrays.asList("[Bossing]", "[Slayer]", "[Unassigned]"), rowNames());
		model.openSection(model.view().getRows().get(1));
		model.resetSearch();
		assertEquals(Arrays.asList("<Bossing>", "Vorkath", "Zulrah"), rowNames());

		// picking a setup beside the bank leaves its page open, and so does a reload
		model.resetSearch();
		setSectionedRows();
		assertEquals(Arrays.asList("<Bossing>", "Vorkath", "Zulrah"), rowNames());

		// unless the section is gone
		model.setRows(model.view().getRows().subList(1, 3), 2);
		assertFalse(model.view().isSectionPages());
		assertEquals(Arrays.asList("Vorkath", "Zulrah"), rowNames());
	}

	@Test
	public void pagesNeedSections()
	{
		model.setSectionPages(true);
		assertFalse(model.view().isSectionPages());
		assertEquals(7, model.view().getRows().size());

		// and headings can't be opened unless pages are on
		model.setSectionPages(false);
		setSectionedRows();
		model.openSection(model.view().getRows().get(0));
		assertEquals(8, model.view().getRows().size());
	}

	@Test
	public void popupStartsOnTheOpenSetup()
	{
		model.setActiveSetup("Slayer melee");
		model.openPalette();
		assertEquals("Slayer melee", model.getSelectedSetup().getName());
		// and it is scrolled into view once the popup knows how many rows it shows
		model.setVisibleRows(3);
		assertEquals(3, model.view().getScroll());
		assertTrue(model.view().getSelected() < model.view().getScroll() + 3);

		// scrolling away with the wheel isn't fought
		model.scrollBy(-3);
		model.setVisibleRows(3);
		assertEquals(0, model.view().getScroll());

		// searching goes back to the best match as usual
		type("v");
		assertEquals("Vorkath", model.getSelectedSetup().getName());
	}

	@Test
	public void aBankTagIsOpenOnlyWhenTheTagOfItsNameIs()
	{
		final SetupEntry setup = new SetupEntry("Zulrah", false, null, 1);
		final SetupEntry tag = SetupEntry.bankTag("Zulrah", 1);
		// only setups listed: nothing to tell apart
		model.setSetups(Arrays.asList(setup));
		assertFalse(model.view().isMixed());
		model.setSetups(Arrays.asList(setup, tag));
		assertTrue(model.view().isMixed());

		model.setActiveSetup("Zulrah");
		assertTrue(model.view().isActive(setup));
		assertFalse(model.view().isActive(tag));

		model.setActiveSetup("");
		model.setActiveBankTag("Zulrah");
		assertFalse(model.view().isActive(setup));
		assertTrue(model.view().isActive(tag));
		// the popup starts on the tag, not on the setup listed above it
		model.openPalette();
		assertTrue(model.getSelectedSetup().isBankTag());
	}

	@Test
	public void popupStartsAtTheTopWithNothingOpen()
	{
		model.openPalette();
		assertEquals(0, model.view().getSelected());
		model.setActiveSetup("A setup that was deleted");
		model.openPalette();
		assertEquals(0, model.view().getSelected());
	}

	@Test
	public void fuzzySearchOnlyStepsInWhenNothingMatches()
	{
		model.setFuzzySearch(true);
		model.openPalette();
		// an exact match is all that's shown, though "Vardorvis" has v, o, r in order too
		type("vor");
		assertEquals(1, model.view().getRows().size());
		assertEquals("Vorkath", model.getSelectedSetup().getName());
		model.resetSearch();

		// a wrong letter, a missing one, and two swapped
		for (String typo : new String[]{"vorkahh", "zulah", "slyaer"})
		{
			model.openPalette();
			type(typo);
			assertFalse(typo, model.view().getRows().isEmpty());
			model.resetSearch();
		}
		model.openPalette();
		type("zulah");
		assertEquals("Zulrah", model.getSelectedSetup().getName());
		model.resetSearch();

		// letters in order
		model.openPalette();
		type("brws");
		assertEquals("Barrows", model.getSelectedSetup().getName());
		model.resetSearch();

		// nothing like it at all
		model.openPalette();
		type("qqqq");
		assertTrue(model.view().getRows().isEmpty());
	}

	@Test
	public void fuzzySearchCanBeTurnedOff()
	{
		model.setFuzzySearch(false);
		model.openPalette();
		type("zulah");
		assertTrue(model.view().getRows().isEmpty());
	}

	@Test
	public void startingOnTheOpenSetupCanBeTurnedOff()
	{
		model.setStartOnActiveSetup(false);
		model.setActiveSetup("Slayer melee");
		model.openPalette();
		assertEquals(0, model.view().getSelected());
		assertEquals(0, model.view().getScroll());
	}
}
