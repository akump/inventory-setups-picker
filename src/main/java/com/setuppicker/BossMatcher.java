package com.setuppicker;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import net.runelite.client.hiscore.HiscoreSkill;
import net.runelite.client.hiscore.HiscoreSkillType;

/**
 * Works out which boss a setup is for from its name: "Vorkath (dhcb)", "kq", "CoX CM" or "zuk". A setup is
 * matched to a boss when its name has the boss's name in it, or one of the names players call it by.
 */
final class BossMatcher
{
	// Names shorter than this have to be a word of their own in the setup's name, so "nex" doesn't match "next".
	// Longer ones can be anywhere in it, run together with other words or not.
	private static final int MIN_INSIDE_WORD = 6;

	// What players call the bosses besides their names, by the name of the boss in HiscoreSkill. Kept as strings
	// so a boss that's renamed or dropped from the client is just left out.
	private static final Map<String, String[]> NICKNAMES = new HashMap<>();

	static
	{
		nicknames("ABYSSAL_SIRE", "sire");
		nicknames("ALCHEMICAL_HYDRA", "hydra", "alch hydra");
		nicknames("AMOXLIATL", "amox");
		nicknames("ARAXXOR", "araxx", "rax");
		nicknames("BARROWS_CHESTS", "barrows");
		nicknames("BRYOPHYTA", "bryo");
		nicknames("CALVARION", "calv", "calvar ion");
		nicknames("CERBERUS", "cerb");
		nicknames("CHAMBERS_OF_XERIC", "cox", "chambers", "olm", "raids 1");
		nicknames("CHAMBERS_OF_XERIC_CHALLENGE_MODE", "cox cm", "cm cox", "cm", "challenge mode");
		nicknames("CHAOS_ELEMENTAL", "chaos ele");
		nicknames("COMMANDER_ZILYANA", "zilyana", "zily", "sara");
		nicknames("CORPOREAL_BEAST", "corp");
		nicknames("CRAZY_ARCHAEOLOGIST", "crazy arch");
		nicknames("DAGANNOTH_REX", "rex", "dks", "dagannoth kings", "dag kings");
		nicknames("DAGANNOTH_PRIME", "prime");
		nicknames("DAGANNOTH_SUPREME", "supreme");
		nicknames("DERANGED_ARCHAEOLOGIST", "deranged arch");
		nicknames("DOOM_OF_MOKHAIOTL", "doom", "mokhaiotl");
		nicknames("DUKE_SUCELLUS", "duke");
		nicknames("GENERAL_GRAARDOR", "graardor", "bandos");
		nicknames("GIANT_MOLE", "mole");
		nicknames("GROTESQUE_GUARDIANS", "grotesque", "guardians", "ggs", "gargs");
		nicknames("KALPHITE_QUEEN", "kq", "kalphite");
		nicknames("KING_BLACK_DRAGON", "kbd");
		nicknames("KREEARRA", "kree", "arma");
		nicknames("KRIL_TSUTSAROTH", "kril", "zammy");
		nicknames("LUNAR_CHESTS", "moons", "perilous moons", "moons of peril");
		nicknames("NIGHTMARE", "nm");
		nicknames("PHOSANIS_NIGHTMARE", "pnm", "phosani");
		nicknames("PHANTOM_MUSPAH", "muspah");
		nicknames("SARACHNIS", "sarach");
		nicknames("SCURRIUS", "scurri");
		nicknames("SHELLBANE_GRYPHON", "gryphon", "shellbane");
		nicknames("SOL_HEREDIT", "sol", "colosseum", "colo", "fortis");
		nicknames("TEMPOROSS", "tempo");
		nicknames("THE_CORRUPTED_GAUNTLET", "cg", "corrupted", "corrupted gauntlet", "cgauntlet");
		nicknames("THE_HUEYCOATL", "huey");
		nicknames("THE_LEVIATHAN", "levi");
		nicknames("THE_ROYAL_TITANS", "titans", "royal titans");
		nicknames("THE_WHISPERER", "whisp");
		nicknames("THEATRE_OF_BLOOD", "tob", "theatre", "verzik", "raids 2");
		nicknames("THEATRE_OF_BLOOD_HARD_MODE", "tob hm", "hm tob", "hmt");
		nicknames("THERMONUCLEAR_SMOKE_DEVIL", "thermy", "thermo", "smoke devil");
		nicknames("TOMBS_OF_AMASCUT", "toa", "tombs", "wardens", "raids 3");
		nicknames("TOMBS_OF_AMASCUT_EXPERT", "toa expert", "expert toa");
		nicknames("TZKAL_ZUK", "zuk", "inferno");
		nicknames("TZTOK_JAD", "jad", "fight caves", "caves");
		nicknames("VARDORVIS", "vard");
		nicknames("VORKATH", "vork");
		nicknames("WINTERTODT", "todt", "wt");
		nicknames("ZULRAH", "zul", "zully");
	}

	private static final List<Phrase> PHRASES = phrases();

	private BossMatcher()
	{
	}

	private static void nicknames(String boss, String... names)
	{
		NICKNAMES.put(boss, names);
	}

	/**
	 * The boss a setup of this name is for, or null for none.
	 */
	static HiscoreSkill match(String setupName)
	{
		final String words = normalize(setupName);
		if (words.isEmpty())
		{
			return null;
		}
		final String padded = " " + words + " ";
		final String compact = words.replace(" ", "");
		HiscoreSkill best = null;
		int bestLength = 0;
		int bestAt = Integer.MAX_VALUE;
		for (Phrase phrase : PHRASES)
		{
			final int at = phrase.compact.length() >= MIN_INSIDE_WORD || padded.contains(" " + phrase.words + " ")
				? compact.indexOf(phrase.compact) : -1;
			if (at < 0)
			{
				continue;
			}
			// the longest name found is the most particular, so "phosani's nightmare" over "nightmare"; between
			// equals, the first in the setup's name
			final int length = phrase.compact.length();
			if (length > bestLength || (length == bestLength && at < bestAt))
			{
				best = phrase.boss;
				bestLength = length;
				bestAt = at;
			}
		}
		return best;
	}

	/**
	 * Lowercase words of letters and digits, one space apart. Apostrophes go, along with an "'s" after a word,
	 * so "Kree'Arra" is "kreearra" and "Phosani's" is "phosani", any other mark parts words, and so does going
	 * from letters to digits, so "Vorkath2" is "vorkath 2".
	 */
	static String normalize(String name)
	{
		final String lower = name.toLowerCase(Locale.ROOT)
			.replaceAll("['’]s\\b", "")
			.replaceAll("['’]", "")
			.replaceAll("(?<=[a-z])(?=[0-9])|(?<=[0-9])(?=[a-z])", " ")
			.replaceAll("[^a-z0-9]+", " ");
		return lower.trim();
	}

	private static List<Phrase> phrases()
	{
		final List<Phrase> phrases = new ArrayList<>();
		for (HiscoreSkill skill : HiscoreSkill.values())
		{
			if (skill.getType() != HiscoreSkillType.BOSS)
			{
				continue;
			}
			final String name = normalize(skill.getName());
			phrases.add(new Phrase(skill, name));
			if (name.startsWith("the "))
			{
				phrases.add(new Phrase(skill, name.substring("the ".length())));
			}
			for (String nickname : NICKNAMES.getOrDefault(skill.name(), new String[0]))
			{
				phrases.add(new Phrase(skill, normalize(nickname)));
			}
		}
		return Collections.unmodifiableList(phrases);
	}

	private static final class Phrase
	{
		private final HiscoreSkill boss;
		private final String words;
		private final String compact;

		private Phrase(HiscoreSkill boss, String words)
		{
			this.boss = boss;
			this.words = words;
			this.compact = words.replace(" ", "");
		}
	}
}
