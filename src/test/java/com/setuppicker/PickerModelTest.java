package com.setuppicker;

import java.util.ArrayList;
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
		assertEquals(2, model.view().getSetups().size());
		assertEquals("Slayer melee", model.getSelectedSetup().getName());
		assertEquals(7, model.view().getTotal());

		model.backspace();
		model.backspace();
		model.backspace();
		model.backspace();
		assertEquals(7, model.view().getSetups().size());
	}

	@Test
	public void noMatchesSelectsNothing()
	{
		model.openPalette();
		type("zzz");
		assertTrue(model.view().getSetups().isEmpty());
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
		assertEquals(7, model.view().getSetups().size());

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
		assertEquals(2, model.view().getSetups().size());
		assertEquals("vor", model.view().getQuery());
	}

	@Test
	public void pickingFromTheUnfilteredListKeepsItsScrollPosition()
	{
		model.scrollBy(4);
		// what clicking a row does, followed by the reload Inventory Setups' config change triggers
		model.resetSearch();
		assertEquals(4, model.view().getScroll());
		model.setSetups(new ArrayList<>(model.view().getSetups()));
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
		final List<SetupEntry> fewer = new ArrayList<>(model.view().getSetups().subList(0, 4));
		model.setSetups(fewer);
		assertEquals(1, model.view().getScroll());
	}
}
