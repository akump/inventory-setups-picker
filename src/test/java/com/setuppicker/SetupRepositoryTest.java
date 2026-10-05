package com.setuppicker;

import com.google.gson.Gson;
import java.awt.Color;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import net.runelite.api.gameval.ItemID;
import net.runelite.http.api.RuneLiteAPI;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import org.junit.Test;

public class SetupRepositoryTest
{
	// The client's gson, which is what serializes colors the way Inventory Setups saves them
	private final Gson gson = RuneLiteAPI.GSON;

	@Test
	public void readsFavoriteColorAndIcon()
	{
		final String json = "{\"inv\":[{\"id\":385,\"q\":5},null],\"eq\":[null,null,null,{\"id\":4151}],"
			+ "\"name\":\"Slayer\",\"hc\":\"#FFFF0000\",\"dc\":\"#FF00FF00\",\"fv\":true,\"iId\":4587}";
		final SetupEntry setup = SetupRepository.parseSetup(gson, "Slayer", json);
		assertEquals("Slayer", setup.getName());
		assertTrue(setup.isFavorite());
		assertEquals(Color.GREEN, setup.getDisplayColor());
		assertEquals(4587, setup.getIconItemId());
	}

	@Test
	public void iconFallsBackToWeapon()
	{
		final String json = "{\"inv\":[],\"eq\":[{\"id\":10828},null,null,{\"id\":4151,\"f\":true}],\"name\":\"Whip\",\"hc\":\"#FFFF0000\"}";
		final SetupEntry setup = SetupRepository.parseSetup(gson, "Whip", json);
		assertFalse(setup.isFavorite());
		assertNull(setup.getDisplayColor());
		assertEquals(4151, setup.getIconItemId());
	}

	@Test
	public void iconFallsBackToCakeWithoutWeapon()
	{
		final String json = "{\"inv\":[],\"eq\":[{\"id\":10828},null,null,null],\"name\":\"Skilling\",\"hc\":\"#FFFF0000\"}";
		assertEquals(ItemID._100GUIDE_GUIDECAKE, SetupRepository.parseSetup(gson, "Skilling", json).getIconItemId());
	}

	@Test
	public void missingOrBrokenDataStillListsTheSetup()
	{
		for (String json : new String[]{null, "", "not json", "[]", "{\"eq\":5}"})
		{
			final SetupEntry setup = SetupRepository.parseSetup(gson, "Mystery", json);
			assertEquals("Mystery", setup.getName());
			assertFalse(setup.isFavorite());
			assertEquals(ItemID._100GUIDE_GUIDECAKE, setup.getIconItemId());
		}
	}

	private static List<String> sortedNames(boolean alphabetical, boolean favoritesFirst)
	{
		final List<SetupEntry> setups = new ArrayList<>();
		setups.add(new SetupEntry("Zulrah", false, null, 1));
		setups.add(new SetupEntry("barrows", false, null, 1));
		setups.add(new SetupEntry("Vorkath", true, null, 1));
		setups.add(new SetupEntry("Araxxor", true, null, 1));
		setups.add(new SetupEntry("Cerberus", false, null, 1));
		return SetupRepository.sort(setups, alphabetical, favoritesFirst).stream().map(SetupEntry::getName).collect(Collectors.toList());
	}

	@Test
	public void sortsByNameIgnoringCaseWithFavoritesOnTop()
	{
		assertEquals(Arrays.asList("Araxxor", "barrows", "Cerberus", "Vorkath", "Zulrah"), sortedNames(true, false));
		assertEquals(Arrays.asList("Araxxor", "Vorkath", "barrows", "Cerberus", "Zulrah"), sortedNames(true, true));
		assertEquals(Arrays.asList("Vorkath", "Araxxor", "Zulrah", "barrows", "Cerberus"), sortedNames(false, true));
		assertEquals(Arrays.asList("Zulrah", "barrows", "Vorkath", "Araxxor", "Cerberus"), sortedNames(false, false));
	}

	@Test
	public void readsSections()
	{
		final String json = "[{\"name\":\"Bossing\",\"setups\":[\"Zulrah\",\"Vorkath\"],\"displayColor\":\"#FF00FF00\",\"isMaximized\":true},"
			+ "{\"name\":\"Empty\",\"setups\":[],\"isMaximized\":false}, 5, {\"setups\":[\"Zulrah\"]}]";
		final List<SetupSection> sections = SetupRepository.parseSections(gson, json);
		assertEquals(2, sections.size());
		assertEquals("Bossing", sections.get(0).getName());
		assertEquals(Color.GREEN, sections.get(0).getDisplayColor());
		assertEquals(Arrays.asList("Zulrah", "Vorkath"), sections.get(0).getSetupNames());
		assertEquals("Empty", sections.get(1).getName());
		assertNull(sections.get(1).getDisplayColor());
		assertTrue(sections.get(1).getSetupNames().isEmpty());

		for (String broken : new String[]{null, "", "not json", "{}", "[]"})
		{
			assertTrue(SetupRepository.parseSections(gson, broken).isEmpty());
		}
	}

	private static List<String> groupedNames(List<SetupSection> sections, boolean alphabetical, boolean favoritesFirst)
	{
		final List<SetupEntry> setups = new ArrayList<>();
		setups.add(new SetupEntry("Zulrah", false, null, 1));
		setups.add(new SetupEntry("Barrows", false, null, 1));
		setups.add(new SetupEntry("Vorkath", true, null, 1));
		setups.add(new SetupEntry("Araxxor", false, null, 1));
		SetupRepository.sort(setups, alphabetical, favoritesFirst);
		return SetupRepository.group(setups, sections, alphabetical, favoritesFirst).stream()
			.map(row -> row.isHeader() ? "[" + row.getSectionName() + "]" : row.getSetup().getName())
			.collect(Collectors.toList());
	}

	@Test
	public void groupsSetupsUnderTheirSections()
	{
		final List<SetupSection> sections = Arrays.asList(
			new SetupSection("Bossing", null, Arrays.asList("Zulrah", "Deleted setup", "Vorkath", "Araxxor")),
			new SetupSection("Empty", null, Arrays.asList()),
			new SetupSection("Dragons", null, Arrays.asList("Vorkath")));

		// a section keeps Inventory Setups' order unless sorting is on, and a setup can be in several
		assertEquals(Arrays.asList("[Bossing]", "Zulrah", "Vorkath", "Araxxor", "[Dragons]", "Vorkath", "[Unassigned]", "Barrows"),
			groupedNames(sections, false, false));
		assertEquals(Arrays.asList("[Bossing]", "Vorkath", "Araxxor", "Zulrah", "[Dragons]", "Vorkath", "[Unassigned]", "Barrows"),
			groupedNames(sections, true, true));

		// no heading for the leftovers when there are none
		assertEquals(Arrays.asList("[Everything]", "Zulrah", "Barrows", "Vorkath", "Araxxor"),
			groupedNames(Arrays.asList(new SetupSection("Everything", null, Arrays.asList("Zulrah", "Barrows", "Vorkath", "Araxxor"))), false, false));
	}

	@Test
	public void withoutSectionsInUseTheListIsFlat()
	{
		assertEquals(Arrays.asList("Zulrah", "Barrows", "Vorkath", "Araxxor"), groupedNames(new ArrayList<>(), false, false));
		assertEquals(Arrays.asList("Zulrah", "Barrows", "Vorkath", "Araxxor"),
			groupedNames(Arrays.asList(new SetupSection("Empty", null, Arrays.asList("Deleted setup"))), false, false));
	}
}
