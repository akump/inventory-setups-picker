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
}
