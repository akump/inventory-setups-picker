package com.setuppicker;

import java.awt.Color;

/**
 * One line of the picker's list: a setup, or the heading of the section the setups below it belong to.
 */
public final class PickerRow
{
	/**
	 * Heading for the setups that aren't in any section.
	 */
	public static final PickerRow UNASSIGNED = new PickerRow(null, "Unassigned", null, false);

	private final SetupEntry setup;
	private final String sectionName;
	private final Color sectionColor;
	private final boolean back;

	private PickerRow(SetupEntry setup, String sectionName, Color sectionColor, boolean back)
	{
		this.setup = setup;
		this.sectionName = sectionName;
		this.sectionColor = sectionColor;
		this.back = back;
	}

	public static PickerRow of(SetupEntry setup)
	{
		return new PickerRow(setup, null, null, false);
	}

	public static PickerRow header(SetupSection section)
	{
		return new PickerRow(null, section.getName(), section.getDisplayColor(), false);
	}

	/**
	 * The heading at the top of a section's own page, which leads back to the list of sections.
	 */
	public static PickerRow backFrom(PickerRow header)
	{
		return new PickerRow(null, header.sectionName, header.sectionColor, true);
	}

	public boolean isHeader()
	{
		return setup == null;
	}

	/**
	 * Whether this is the heading of a section's page, for going back to the list of sections.
	 */
	public boolean isBack()
	{
		return back;
	}

	/**
	 * Whether this heading and the other are for the same section.
	 */
	boolean sameSection(PickerRow other)
	{
		return (this == UNASSIGNED) == (other == UNASSIGNED) && sectionName.equals(other.sectionName);
	}

	/**
	 * The setup on this row, or null for a heading.
	 */
	public SetupEntry getSetup()
	{
		return setup;
	}

	/**
	 * The section name of a heading, or null for a setup.
	 */
	public String getSectionName()
	{
		return sectionName;
	}

	/**
	 * The color the user gave the section in Inventory Setups, or null for none.
	 */
	public Color getSectionColor()
	{
		return sectionColor;
	}
}
