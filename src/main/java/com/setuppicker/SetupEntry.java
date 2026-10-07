package com.setuppicker;

import java.awt.Color;

/**
 * One row in the picker: the bits of an inventory setup, or of a bank tag tab, needed to list it.
 */
public final class SetupEntry
{
	// Starts the key of a bank tag. Not something a name can have in it, so a setup and a tag never share a key.
	private static final char BANK_TAG_KEY_PREFIX = '\u0001';

	private final String name;
	private final boolean favorite;
	private final Color displayColor;
	private final int iconItemId;
	private final boolean bankTag;

	public SetupEntry(String name, boolean favorite, Color displayColor, int iconItemId)
	{
		this(name, favorite, displayColor, iconItemId, false);
	}

	private SetupEntry(String name, boolean favorite, Color displayColor, int iconItemId, boolean bankTag)
	{
		this.name = name;
		this.favorite = favorite;
		this.displayColor = displayColor;
		this.iconItemId = iconItemId;
		this.bankTag = bankTag;
	}

	/**
	 * The entry for a tag tab of the Bank Tags plugin, which has neither a color nor a favorite flag.
	 */
	public static SetupEntry bankTag(String name, int iconItemId)
	{
		return new SetupEntry(name, false, null, iconItemId, true);
	}

	/**
	 * The key a bank tag of this name goes by among the recently used.
	 */
	static String bankTagKey(String name)
	{
		return BANK_TAG_KEY_PREFIX + name;
	}

	public String getName()
	{
		return name;
	}

	/**
	 * Whether this is a bank tag tab rather than an inventory setup.
	 */
	public boolean isBankTag()
	{
		return bankTag;
	}

	/**
	 * What tells this entry apart from every other: its name, marked when it's a bank tag's, as a setup and a
	 * tag can be called the same.
	 */
	public String getKey()
	{
		return bankTag ? bankTagKey(name) : name;
	}

	public boolean isFavorite()
	{
		return favorite;
	}

	/**
	 * The color the user gave the setup in Inventory Setups, or null for none.
	 */
	public Color getDisplayColor()
	{
		return displayColor;
	}

	public int getIconItemId()
	{
		return iconItemId;
	}
}
