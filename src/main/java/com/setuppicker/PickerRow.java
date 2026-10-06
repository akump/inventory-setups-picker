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
	public static final PickerRow UNASSIGNED = new PickerRow(null, "Unassigned", null, false, false, 1);

	/**
	 * Heading for the recently used setups, when the list is in sections.
	 */
	public static final PickerRow RECENT = new PickerRow(null, "Recent", null, false, false, 2);

	private final SetupEntry setup;
	private final String sectionName;
	private final Color sectionColor;
	private final boolean back;
	private final boolean recent;

	// 0 for a setup or a section of the user's; otherwise which of the headings above this is, or leads back from
	private final int builtIn;

	private PickerRow(SetupEntry setup, String sectionName, Color sectionColor, boolean back, boolean recent, int builtIn)
	{
		this.recent = recent;
		this.builtIn = builtIn;
		this.setup = setup;
		this.sectionName = sectionName;
		this.sectionColor = sectionColor;
		this.back = back;
	}

	public static PickerRow of(SetupEntry setup)
	{
		return new PickerRow(setup, null, null, false, false, 0);
	}

	/**
	 * A setup listed at the top for having been used recently.
	 */
	public static PickerRow recent(SetupEntry setup)
	{
		return new PickerRow(setup, null, null, false, true, 0);
	}

	public static PickerRow header(SetupSection section)
	{
		return new PickerRow(null, section.getName(), section.getDisplayColor(), false, false, 0);
	}

	/**
	 * The heading at the top of a section's own page, which leads back to the list of sections.
	 */
	public static PickerRow backFrom(PickerRow header)
	{
		return new PickerRow(null, header.sectionName, header.sectionColor, true, false, header.builtIn);
	}

	public boolean isHeader()
	{
		return setup == null;
	}

	/**
	 * Whether this setup is listed here for having been used recently.
	 */
	public boolean isRecent()
	{
		return recent;
	}

	/**
	 * Whether this heading is one of the picker's own rather than a section from Inventory Setups.
	 */
	public boolean isBuiltIn()
	{
		return builtIn != 0;
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
		// a section the user happened to name "Unassigned" or "Recent" is not the built-in one
		return builtIn == other.builtIn && sectionName.equals(other.sectionName);
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
