package com.setuppicker;

import com.google.gson.Gson;
import java.awt.Color;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.function.IntUnaryOperator;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import org.junit.Test;

public class SetupHighlightTest
{
	private static final Gson GSON = new Gson();
	// 1, 2 and 3 are variants of one item, say a ring of dueling at different charges
	private static final IntUnaryOperator VARIATION = id -> id >= 1 && id <= 3 ? 1 : id;

	private static SetupHighlight.Slot item(int id)
	{
		return new SetupHighlight.Slot(id, 1);
	}

	private static SetupHighlight.Slot fuzzy(int id)
	{
		return new SetupHighlight.Slot(id, 1, true, SetupHighlight.StackCompare.None);
	}

	private static List<SetupHighlight.Slot> slots(SetupHighlight.Slot... slots)
	{
		return Arrays.asList(slots);
	}

	private static List<Integer> ids(List<SetupHighlight.Slot> slots)
	{
		final List<Integer> ids = new ArrayList<>();
		for (SetupHighlight.Slot slot : slots)
		{
			ids.add(slot.id);
		}
		return ids;
	}

	private static List<Integer> differences(boolean unordered, List<SetupHighlight.Slot> saved, List<SetupHighlight.Slot> current)
	{
		final SetupHighlight highlight = new SetupHighlight(Color.RED, unordered, saved, Collections.emptyList());
		return ids(highlight.differences(current, Collections.emptyList(), null, VARIATION));
	}

	@Test
	public void orderedComparesSlotBySlot()
	{
		final List<SetupHighlight.Slot> saved = slots(item(10), item(11), SetupHighlight.Slot.EMPTY);
		assertEquals(Collections.emptyList(), differences(false, saved, slots(item(10), item(11), item(12))));
		// swapped round: both are in the wrong place
		assertEquals(Arrays.asList(10, 11), differences(false, saved, slots(item(11), item(10))));
	}

	@Test
	public void unorderedLooksAnywhere()
	{
		final List<SetupHighlight.Slot> saved = slots(item(10), item(11));
		assertEquals(Collections.emptyList(), differences(true, saved, slots(item(11), SetupHighlight.Slot.EMPTY, item(10))));
		assertEquals(Collections.singletonList(11), differences(true, saved, slots(item(10))));
	}

	@Test
	public void unorderedCountsEachSlot()
	{
		final List<SetupHighlight.Slot> saved = slots(item(10), item(10), item(10));
		assertEquals(Collections.singletonList(10), differences(true, saved, slots(item(10), item(10))));
	}

	@Test
	public void fuzzyItemsMatchTheirVariants()
	{
		assertEquals(Collections.emptyList(), differences(false, slots(fuzzy(1)), slots(item(3))));
		assertEquals(Collections.singletonList(1), differences(false, slots(item(1)), slots(item(3))));
		assertEquals(Collections.emptyList(), differences(true, slots(fuzzy(1)), slots(item(3))));
		assertEquals(Collections.singletonList(1), differences(true, slots(item(1)), slots(item(3))));
	}

	@Test
	public void exactItemsPickBeforeFuzzyOnes()
	{
		// the fuzzy ring could do with the other variant, but the exact one can't
		final List<SetupHighlight.Slot> saved = slots(fuzzy(1), item(2));
		assertEquals(Collections.emptyList(), differences(true, saved, slots(item(2), item(3))));
	}

	@Test
	public void stackCompareChecksQuantity()
	{
		final SetupHighlight.Slot runes = new SetupHighlight.Slot(20, 100, false, SetupHighlight.StackCompare.Less_Than);
		assertEquals(Collections.singletonList(20), differences(false, slots(runes), slots(new SetupHighlight.Slot(20, 50))));
		assertEquals(Collections.emptyList(), differences(false, slots(runes), slots(new SetupHighlight.Slot(20, 500))));
		assertEquals(Collections.singletonList(20), differences(true, slots(runes), slots(new SetupHighlight.Slot(20, 50))));
		// without a stack compare, any amount does
		assertEquals(Collections.emptyList(), differences(false, slots(item(20)), slots(new SetupHighlight.Slot(20, 5))));
	}

	@Test
	public void equipmentIsAlwaysBySlot()
	{
		final SetupHighlight highlight = new SetupHighlight(Color.RED, true, Collections.emptyList(),
			slots(SetupHighlight.Slot.EMPTY, item(30)));
		assertEquals(Collections.singletonList(30), ids(highlight.differences(Collections.emptyList(), slots(item(30)), null, VARIATION)));
		assertEquals(Collections.emptyList(), ids(highlight.differences(Collections.emptyList(), slots(SetupHighlight.Slot.EMPTY, item(30)), null, VARIATION)));
	}

	@Test
	public void parsesTheSavedSetup()
	{
		assertNull(SetupHighlight.parse(GSON, "{\"name\":\"a\",\"inv\":[{\"id\":10}]}"));
		assertNull(SetupHighlight.parse(GSON, null));
		assertNull(SetupHighlight.parse(GSON, "not json"));

		final SetupHighlight highlight = SetupHighlight.parse(GSON, "{\"name\":\"a\",\"hd\":true,\"uh\":true,"
			+ "\"hc\":{\"value\":-16711936},\"inv\":[{\"id\":10},null,{\"id\":20,\"q\":100,\"f\":true,\"sc\":\"Less_Than\"}],"
			+ "\"eq\":[{\"id\":-1},{\"id\":30}]}");
		assertNotNull(highlight);
		assertEquals(Color.GREEN, highlight.getColor());
		assertEquals(Arrays.asList(10, 20, 30), ids(highlight.differences(Collections.emptyList(), Collections.emptyList(), null, VARIATION)));
		// unordered, with the runes' quantity compared
		assertEquals(Collections.singletonList(20), ids(highlight.differences(
			slots(new SetupHighlight.Slot(20, 5), item(10)), slots(SetupHighlight.Slot.EMPTY, item(30)), null, VARIATION)));
	}

	@Test
	public void runePouchLooksAnywhereInThePouch()
	{
		final SetupHighlight.Slot laws = new SetupHighlight.Slot(563, 100, false, SetupHighlight.StackCompare.Less_Than);
		final SetupHighlight highlight = new SetupHighlight(Color.RED, false, Collections.emptyList(), Collections.emptyList(),
			slots(item(560), laws, SetupHighlight.Slot.EMPTY));
		final List<SetupHighlight.Slot> none = Collections.emptyList();
		// no pouch at all: every rune is missing
		assertEquals(Arrays.asList(560, 563), ids(highlight.differences(none, none, null, VARIATION)));
		assertEquals(Collections.emptyList(), ids(highlight.differences(none, none,
			slots(new SetupHighlight.Slot(563, 500), new SetupHighlight.Slot(560, 3)), VARIATION)));
		assertEquals(Collections.singletonList(563), ids(highlight.differences(none, none,
			slots(new SetupHighlight.Slot(563, 50), new SetupHighlight.Slot(560, 3)), VARIATION)));
		assertEquals(Collections.singletonList(560), ids(highlight.differences(none, none,
			slots(new SetupHighlight.Slot(563, 500), SetupHighlight.Slot.EMPTY), VARIATION)));
	}

	@Test
	public void parsesTheRunePouch()
	{
		final SetupHighlight highlight = SetupHighlight.parse(GSON, "{\"hd\":true,\"inv\":[],\"eq\":[],"
			+ "\"rp\":[{\"id\":560,\"q\":20},{\"id\":-1}]}");
		final List<SetupHighlight.Slot> none = Collections.emptyList();
		assertEquals(Collections.singletonList(560), ids(highlight.differences(none, none, null, VARIATION)));
		// a setup without one doesn't mind whether the player has one
		final SetupHighlight without = SetupHighlight.parse(GSON, "{\"hd\":true,\"inv\":[],\"eq\":[]}");
		assertEquals(Collections.emptyList(), ids(without.differences(none, none, null, VARIATION)));
	}

	@Test
	public void defaultsToRedWithoutAColor()
	{
		assertEquals(SetupHighlight.DEFAULT_COLOR, SetupHighlight.parse(GSON, "{\"hd\":true,\"inv\":[]}").getColor());
	}
}
