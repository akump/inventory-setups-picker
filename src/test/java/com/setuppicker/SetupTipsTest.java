package com.setuppicker;

import com.google.gson.Gson;
import java.util.EnumSet;
import java.util.Set;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import org.junit.Test;

public class SetupTipsTest
{
	private static final Gson GSON = new Gson();
	// what the plugin starts out with
	private static final SetupTips.PickerOptions DEFAULTS = new SetupTips.PickerOptions(true, false, false);
	private static final SetupTips.PickerOptions ALL_ON = new SetupTips.PickerOptions(true, true, true);

	private static SetupTips.SetupOptions setup(boolean filterBank, boolean highlight, int spellbook, boolean notes)
	{
		return new SetupTips.SetupOptions(filterBank, highlight, spellbook, notes);
	}

	private static SetupTips.Tip choose(SetupTips.SetupOptions setup, SetupTips.PickerOptions picker, Set<SetupTips.Tip> shown)
	{
		return SetupTips.choose(setup, picker, shown::contains);
	}

	@Test
	public void givesTheMostUsefulTipFirst()
	{
		final SetupTips.SetupOptions bare = setup(false, false, 2, true);
		assertEquals(SetupTips.Tip.FILTER_BANK, choose(bare, DEFAULTS, EnumSet.noneOf(SetupTips.Tip.class)));
		assertEquals(SetupTips.Tip.HIGHLIGHT_DIFFERENCES, choose(bare, DEFAULTS, EnumSet.of(SetupTips.Tip.FILTER_BANK)));
		assertEquals(SetupTips.Tip.SHOW_SPELLBOOK, choose(bare, DEFAULTS,
			EnumSet.of(SetupTips.Tip.FILTER_BANK, SetupTips.Tip.HIGHLIGHT_DIFFERENCES)));
		assertEquals(SetupTips.Tip.SHOW_NOTES, choose(bare, DEFAULTS,
			EnumSet.of(SetupTips.Tip.FILTER_BANK, SetupTips.Tip.HIGHLIGHT_DIFFERENCES, SetupTips.Tip.SHOW_SPELLBOOK)));
		assertNull(choose(bare, DEFAULTS, EnumSet.allOf(SetupTips.Tip.class)));
	}

	@Test
	public void noTipForASetupThatUsesEverything()
	{
		assertNull(choose(setup(true, true, 2, true), ALL_ON, EnumSet.noneOf(SetupTips.Tip.class)));
	}

	@Test
	public void onlyTipsAboutWhatIsOff()
	{
		final Set<SetupTips.Tip> none = EnumSet.noneOf(SetupTips.Tip.class);
		// the bank highlighting turned off here makes the setup's own setting moot
		assertNull(choose(setup(true, false, 0, false), new SetupTips.PickerOptions(false, false, false), none));
		// the standard spellbook and none aren't worth a tip
		assertNull(choose(setup(true, true, 0, false), DEFAULTS, none));
		assertNull(choose(setup(true, true, 4, false), DEFAULTS, none));
		assertEquals(SetupTips.Tip.SHOW_SPELLBOOK, choose(setup(true, true, 3, false), DEFAULTS, none));
		assertNull(choose(setup(true, true, 3, false), ALL_ON, none));
		assertEquals(SetupTips.Tip.SHOW_NOTES, choose(setup(true, true, 0, true), DEFAULTS, none));
	}

	@Test
	public void namesTheSetupAndSpellbook()
	{
		final String text = SetupTips.Tip.SHOW_SPELLBOOK.text("Vorkath", setup(true, true, 2, false));
		assertTrue(text, text.startsWith("Vorkath is for the Lunar spellbook."));
	}

	@Test
	public void readsTheSavedSetup()
	{
		final SetupTips.SetupOptions bare = SetupTips.SetupOptions.parse(GSON, "{\"name\":\"a\",\"inv\":[]}");
		assertFalse(bare.filterBank);
		assertFalse(bare.highlightDifferences);
		assertEquals(0, bare.spellbook);
		assertFalse(bare.hasNotes);

		final SetupTips.SetupOptions full = SetupTips.SetupOptions.parse(GSON,
			"{\"fb\":true,\"hd\":true,\"sb\":1,\"notes\":\"  bring a spade \"}");
		assertTrue(full.filterBank);
		assertTrue(full.highlightDifferences);
		assertEquals(1, full.spellbook);
		assertTrue(full.hasNotes);

		assertFalse(SetupTips.SetupOptions.parse(GSON, "{\"notes\":\"   \"}").hasNotes);
		assertNull(SetupTips.SetupOptions.parse(GSON, null));
		assertNull(SetupTips.SetupOptions.parse(GSON, "not json"));
	}
}
