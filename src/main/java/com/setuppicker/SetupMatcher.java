package com.setuppicker;

/**
 * Decides whether a name matches what has been typed in the search box.
 *
 * <p>There are three ways to match, from strict to loose. The search uses the strictest one that finds
 * anything, so the looser ones only come into play when a search would otherwise come up empty.
 */
public final class SetupMatcher
{
	public enum Tier
	{
		/**
		 * The name contains the search text.
		 */
		EXACT,
		/**
		 * The name contains the search text give or take a slip: a wrong, missing or extra letter, or two
		 * letters swapped.
		 */
		TYPO,
		/**
		 * The name contains the search text's letters in order, with anything in between ("vkdh" for
		 * "Vorkath (dhcb)").
		 */
		SUBSEQUENCE
	}

	// Short searches are too easy to match by accident to allow slips in
	private static final int MIN_LENGTH_FOR_TYPO = 4;
	private static final int MIN_LENGTH_FOR_TWO_TYPOS = 8;
	private static final int MIN_LENGTH_FOR_SUBSEQUENCE = 3;

	private SetupMatcher()
	{
	}

	/**
	 * @param name   lower case
	 * @param needle the search text, lower case and not empty
	 */
	public static boolean matches(String name, String needle, Tier tier)
	{
		switch (tier)
		{
			case EXACT:
				return name.contains(needle);
			case TYPO:
				if (needle.length() < MIN_LENGTH_FOR_TYPO)
				{
					return false;
				}
				return closestSubstringDistance(name, needle) <= (needle.length() < MIN_LENGTH_FOR_TWO_TYPOS ? 1 : 2);
			default:
				return needle.length() >= MIN_LENGTH_FOR_SUBSEQUENCE && isSubsequence(name, needle);
		}
	}

	private static boolean isSubsequence(String name, String needle)
	{
		int matched = 0;
		for (int i = 0; i < name.length() && matched < needle.length(); i++)
		{
			if (name.charAt(i) == needle.charAt(matched))
			{
				matched++;
			}
		}
		return matched == needle.length();
	}

	/**
	 * The fewest single-letter edits (replace, insert, delete, or swap two neighbours) that turn the needle
	 * into some stretch of the name.
	 */
	static int closestSubstringDistance(String name, String needle)
	{
		final int n = needle.length();
		final int m = name.length();
		// distance[i][j]: the first i letters of the needle against the closest stretch of the name ending at j
		final int[][] distance = new int[n + 1][m + 1];
		for (int i = 1; i <= n; i++)
		{
			distance[i][0] = i;
		}
		// row 0 stays 0: a match may start anywhere in the name for free
		for (int i = 1; i <= n; i++)
		{
			for (int j = 1; j <= m; j++)
			{
				final int replace = needle.charAt(i - 1) == name.charAt(j - 1) ? 0 : 1;
				int best = Math.min(distance[i - 1][j - 1] + replace,
					Math.min(distance[i - 1][j] + 1, distance[i][j - 1] + 1));
				if (i > 1 && j > 1 && needle.charAt(i - 1) == name.charAt(j - 2) && needle.charAt(i - 2) == name.charAt(j - 1))
				{
					best = Math.min(best, distance[i - 2][j - 2] + 1);
				}
				distance[i][j] = best;
			}
		}
		int closest = n;
		for (int j = 0; j <= m; j++)
		{
			closest = Math.min(closest, distance[n][j]);
		}
		return closest;
	}
}
