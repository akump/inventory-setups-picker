package com.setuppicker;

import java.awt.image.BufferedImage;
import net.runelite.client.hiscore.HiscoreSkill;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import org.junit.Test;

public class BossMatcherTest
{
	private static void assertBoss(HiscoreSkill boss, String setupName)
	{
		assertEquals(setupName, boss, BossMatcher.match(setupName));
	}

	@Test
	public void matchesTheBossByName()
	{
		assertBoss(HiscoreSkill.VORKATH, "Vorkath");
		assertBoss(HiscoreSkill.VORKATH, "Vorkath (dhcb)");
		assertBoss(HiscoreSkill.ZULRAH, "ZULRAH");
		assertBoss(HiscoreSkill.KALPHITE_QUEEN, "Kalphite Queen");
		assertBoss(HiscoreSkill.TOMBS_OF_AMASCUT, "Tombs of Amascut 300 invocation");
		assertBoss(HiscoreSkill.CHAMBERS_OF_XERIC, "Chambers of Xeric: solo");
	}

	@Test
	public void ignoresApostrophesAndPunctuation()
	{
		assertBoss(HiscoreSkill.KREEARRA, "Kree'arra");
		assertBoss(HiscoreSkill.KREEARRA, "kreearra");
		assertBoss(HiscoreSkill.KRIL_TSUTSAROTH, "K'ril Tsutsaroth");
		assertBoss(HiscoreSkill.VETION, "vetion");
		assertBoss(HiscoreSkill.TZTOK_JAD, "TzTok-Jad");
		assertBoss(HiscoreSkill.TZTOK_JAD, "tztok jad");
		assertBoss(HiscoreSkill.VORKATH, "Vorkath's");
	}

	@Test
	public void leavesOutTheThe()
	{
		assertBoss(HiscoreSkill.THE_LEVIATHAN, "Leviathan");
		assertBoss(HiscoreSkill.THE_WHISPERER, "whisperer tank");
		assertBoss(HiscoreSkill.THE_GAUNTLET, "Gauntlet");
	}

	@Test
	public void findsTheBossInsideOtherWords()
	{
		assertBoss(HiscoreSkill.VORKATH, "myvorkathspec");
		assertBoss(HiscoreSkill.VORKATH, "Vorkath2");
		assertBoss(HiscoreSkill.KALPHITE_QUEEN, "kalphitequeen");
		assertBoss(HiscoreSkill.ZULRAH, "zulrah-mage");
	}

	@Test
	public void matchesNicknames()
	{
		assertBoss(HiscoreSkill.KALPHITE_QUEEN, "kq");
		assertBoss(HiscoreSkill.KING_BLACK_DRAGON, "KBD");
		assertBoss(HiscoreSkill.CHAMBERS_OF_XERIC, "CoX");
		assertBoss(HiscoreSkill.THEATRE_OF_BLOOD, "ToB scythe");
		assertBoss(HiscoreSkill.TOMBS_OF_AMASCUT, "toa 400");
		assertBoss(HiscoreSkill.TZKAL_ZUK, "Zuk");
		assertBoss(HiscoreSkill.TZKAL_ZUK, "Inferno");
		assertBoss(HiscoreSkill.TZTOK_JAD, "jad");
		assertBoss(HiscoreSkill.CORPOREAL_BEAST, "corp");
		assertBoss(HiscoreSkill.ALCHEMICAL_HYDRA, "Hydra");
		assertBoss(HiscoreSkill.COMMANDER_ZILYANA, "Sara gwd");
		assertBoss(HiscoreSkill.GENERAL_GRAARDOR, "Bandos");
		assertBoss(HiscoreSkill.SOL_HEREDIT, "Colosseum");
		assertBoss(HiscoreSkill.LUNAR_CHESTS, "Moons of peril");
		assertBoss(HiscoreSkill.DUKE_SUCELLUS, "duke");
		assertBoss(HiscoreSkill.THE_CORRUPTED_GAUNTLET, "CG");
	}

	@Test
	public void prefersTheMostParticularBoss()
	{
		assertBoss(HiscoreSkill.PHOSANIS_NIGHTMARE, "Phosani's Nightmare");
		assertBoss(HiscoreSkill.NIGHTMARE, "Nightmare");
		assertBoss(HiscoreSkill.THE_CORRUPTED_GAUNTLET, "Corrupted Gauntlet");
		assertBoss(HiscoreSkill.CHAMBERS_OF_XERIC_CHALLENGE_MODE, "CoX CM");
		assertBoss(HiscoreSkill.CHAMBERS_OF_XERIC_CHALLENGE_MODE, "Chambers of Xeric: Challenge Mode");
		assertBoss(HiscoreSkill.THEATRE_OF_BLOOD_HARD_MODE, "ToB HM");
		assertBoss(HiscoreSkill.TOMBS_OF_AMASCUT_EXPERT, "ToA expert");
	}

	@Test
	public void keepsShortNicknamesToWholeWords()
	{
		assertNull(BossMatcher.match("Next clue"));
		assertNull(BossMatcher.match("Mining"));
		assertNull(BossMatcher.match("Barbarian fishing"));
		assertNull(BossMatcher.match("Cooking"));
		assertBoss(HiscoreSkill.NEX, "Nex");
	}

	@Test
	public void matchesNothingForOtherSetups()
	{
		assertNull(BossMatcher.match("Slayer melee"));
		assertNull(BossMatcher.match("Clue step 3"));
		assertNull(BossMatcher.match("Zamorak spear"));
		assertNull(BossMatcher.match(""));
		assertNull(BossMatcher.match("  --  "));
	}

	@Test
	public void fitsIconsKeepingTheirShape()
	{
		final BufferedImage wide = BossIcons.fit(new BufferedImage(30, 20, BufferedImage.TYPE_INT_ARGB), 18, 16);
		assertEquals(18, wide.getWidth());
		assertEquals(12, wide.getHeight());
		final BufferedImage tall = BossIcons.fit(new BufferedImage(20, 32, BufferedImage.TYPE_INT_ARGB), 18, 16);
		assertEquals(10, tall.getWidth());
		assertEquals(16, tall.getHeight());
		final BufferedImage small = new BufferedImage(12, 12, BufferedImage.TYPE_INT_ARGB);
		assertSame(small, BossIcons.fit(small, 18, 16));
	}
}
