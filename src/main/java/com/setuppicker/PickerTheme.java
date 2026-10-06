package com.setuppicker;

import java.awt.Color;

/**
 * The colors the picker is drawn in. The defaults suit the game's own interfaces; they can be changed in the
 * plugin's settings, for instance to match a resource pack.
 */
public final class PickerTheme
{
	static final Color DEFAULT_BACKGROUND = new Color(40, 34, 26, 240);
	static final Color DEFAULT_HEADER = new Color(62, 53, 41);
	static final Color DEFAULT_BORDER = new Color(87, 80, 64);
	static final Color DEFAULT_ACCENT = new Color(255, 152, 31);
	static final Color DEFAULT_TEXT = new Color(235, 228, 210);

	public static final PickerTheme DEFAULT = new PickerTheme(DEFAULT_BACKGROUND, DEFAULT_HEADER, DEFAULT_BORDER,
		DEFAULT_ACCENT, DEFAULT_TEXT);

	private static final int ACTIVE_ROW_ALPHA = 70;
	private static final double MUTED_TEXT_FADE = 0.45;

	private final Color background;
	private final Color header;
	private final Color border;
	private final Color accent;
	private final Color text;
	private final Color mutedText;
	private final Color activeRow;

	/**
	 * @param accent the title, the keyboard selection, the focused search box and the open setup
	 */
	public PickerTheme(Color background, Color header, Color border, Color accent, Color text)
	{
		this.background = background;
		this.header = header;
		this.border = border;
		this.accent = accent;
		this.text = text;
		// Hints and placeholders: the text color, faded part of the way into the background
		this.mutedText = mix(text, background, MUTED_TEXT_FADE);
		this.activeRow = new Color(accent.getRed(), accent.getGreen(), accent.getBlue(), ACTIVE_ROW_ALPHA);
	}

	private static Color mix(Color from, Color to, double amount)
	{
		return new Color(
			(int) Math.round(from.getRed() + (to.getRed() - from.getRed()) * amount),
			(int) Math.round(from.getGreen() + (to.getGreen() - from.getGreen()) * amount),
			(int) Math.round(from.getBlue() + (to.getBlue() - from.getBlue()) * amount));
	}

	public Color getBackground()
	{
		return background;
	}

	public Color getHeader()
	{
		return header;
	}

	public Color getBorder()
	{
		return border;
	}

	public Color getAccent()
	{
		return accent;
	}

	public Color getText()
	{
		return text;
	}

	public Color getMutedText()
	{
		return mutedText;
	}

	public Color getActiveRow()
	{
		return activeRow;
	}
}
