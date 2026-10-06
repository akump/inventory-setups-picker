package com.setuppicker;

import java.awt.Color;
import java.util.List;

/**
 * A section from Inventory Setups: a named group of setups. A setup can be in several, or none.
 */
public final class SetupSection
{
	private final String name;
	private final Color displayColor;
	private final List<String> setupNames;

	public SetupSection(String name, Color displayColor, List<String> setupNames)
	{
		this.name = name;
		this.displayColor = displayColor;
		this.setupNames = setupNames;
	}

	public String getName()
	{
		return name;
	}

	/**
	 * The color the user gave the section in Inventory Setups, or null for none.
	 */
	public Color getDisplayColor()
	{
		return displayColor;
	}

	/**
	 * Names of the section's setups, in the order Inventory Setups lists them.
	 */
	public List<String> getSetupNames()
	{
		return setupNames;
	}
}
