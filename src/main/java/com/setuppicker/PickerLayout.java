package com.setuppicker;

import java.awt.Point;
import java.awt.Rectangle;

/**
 * Where the picker's parts are on the canvas for one frame. Shared by painting and mouse hit-testing.
 */
public final class PickerLayout
{
	static final int HEADER_HEIGHT = 20;
	static final int SEARCH_HEIGHT = 20;
	static final int PADDING = 3;
	static final int GAP = 4;
	static final int ROW_HEIGHT_ICONS = 20;
	static final int ROW_HEIGHT_TEXT = 16;
	static final int FOOTER_HEIGHT = 16;

	private final Rectangle bounds;
	private final Rectangle header;
	private final Rectangle search;
	private final Rectangle list;
	private final Rectangle footer;
	private final int rowHeight;
	private final int visibleRows;

	private PickerLayout(Rectangle bounds, Rectangle header, Rectangle search, Rectangle list, Rectangle footer,
		int rowHeight, int visibleRows)
	{
		this.bounds = bounds;
		this.header = header;
		this.search = search;
		this.list = list;
		this.footer = footer;
		this.rowHeight = rowHeight;
		this.visibleRows = visibleRows;
	}

	/**
	 * The hotkey-opened popup: centered horizontally in the game area, in its upper part.
	 *
	 * @param area       the canvas
	 * @param setupCount number of setups to list, after the search is applied
	 * @param maxRows    most rows to show before scrolling
	 */
	public static PickerLayout computePalette(Rectangle area, int width, int setupCount, int rowHeight, int maxRows)
	{
		width = Math.min(width, area.width);
		final int chrome = HEADER_HEIGHT + SEARCH_HEIGHT + FOOTER_HEIGHT + 4 * PADDING;
		final int fittingRows = Math.max(1, (area.height - chrome) / rowHeight);
		final int visibleRows = Math.max(1, Math.min(setupCount, Math.min(maxRows, fittingRows)));

		final int x = area.x + (area.width - width) / 2;
		// Anchored where it would sit with a full list, so it doesn't jump around as typing narrows the results
		final int fullHeight = chrome + Math.min(maxRows, fittingRows) * rowHeight;
		final int y = area.y + Math.max(0, Math.min(area.height / 6, area.height - fullHeight));

		final Rectangle header = new Rectangle(x, y, width, HEADER_HEIGHT);
		final Rectangle search = new Rectangle(x + PADDING, y + HEADER_HEIGHT + PADDING, width - 2 * PADDING, SEARCH_HEIGHT);
		final Rectangle list = new Rectangle(x + PADDING, search.y + search.height + PADDING, width - 2 * PADDING, visibleRows * rowHeight);
		final Rectangle footer = new Rectangle(x + PADDING, list.y + list.height + PADDING, width - 2 * PADDING, FOOTER_HEIGHT);
		final Rectangle bounds = new Rectangle(x, y, width, footer.y + footer.height + PADDING - y);
		return new PickerLayout(bounds, header, search, list, footer, rowHeight, visibleRows);
	}

	/**
	 * The list docked beside the bank.
	 *
	 * @param bank       bounds of the bank interface
	 * @param preferLeft sit left of the bank when both sides have room
	 * @param setupCount number of setups to list, after the search is applied
	 */
	public static PickerLayout compute(Rectangle bank, int canvasWidth, boolean preferLeft, int width,
		boolean collapsed, int setupCount, int rowHeight)
	{
		final int leftX = bank.x - GAP - width;
		final int rightX = bank.x + bank.width + GAP;
		final boolean leftFits = leftX >= 0;
		final boolean rightFits = rightX + width <= canvasWidth;

		final int x;
		if (leftFits && (preferLeft || !rightFits))
		{
			x = leftX;
		}
		else if (rightFits)
		{
			x = rightX;
		}
		else
		{
			// No room on either side (fixed mode): sit on top of the bank's edge instead
			x = preferLeft ? bank.x : bank.x + bank.width - width;
		}
		final int y = bank.y;

		final Rectangle header = new Rectangle(x, y, width, HEADER_HEIGHT);
		if (collapsed)
		{
			return new PickerLayout(header, header, null, null, null, rowHeight, 0);
		}

		final Rectangle search = new Rectangle(x + PADDING, y + HEADER_HEIGHT + PADDING, width - 2 * PADDING, SEARCH_HEIGHT);
		final int listY = search.y + search.height + PADDING;
		final int maxRows = Math.max(1, (bank.y + bank.height - listY - PADDING) / rowHeight);
		// always leave one row's worth of space for the "no setups" message
		final int visibleRows = Math.max(1, Math.min(setupCount, maxRows));
		final Rectangle list = new Rectangle(x + PADDING, listY, width - 2 * PADDING, visibleRows * rowHeight);
		final Rectangle bounds = new Rectangle(x, y, width, list.y + list.height + PADDING - y);
		return new PickerLayout(bounds, header, search, list, null, rowHeight, visibleRows);
	}

	public Rectangle getBounds()
	{
		return bounds;
	}

	public Rectangle getHeader()
	{
		return header;
	}

	/**
	 * The search box, or null when collapsed.
	 */
	public Rectangle getSearch()
	{
		return search;
	}

	/**
	 * The area holding the setup rows, or null when collapsed.
	 */
	public Rectangle getList()
	{
		return list;
	}

	/**
	 * The key hint strip of the popup, or null for the list beside the bank.
	 */
	public Rectangle getFooter()
	{
		return footer;
	}

	public boolean isPalette()
	{
		return footer != null;
	}

	public boolean isCollapsed()
	{
		return list == null;
	}

	public int getRowHeight()
	{
		return rowHeight;
	}

	public int getVisibleRows()
	{
		return visibleRows;
	}

	public Rectangle getRow(int row)
	{
		return new Rectangle(list.x, list.y + row * rowHeight, list.width, rowHeight);
	}

	/**
	 * The visible row under the point, or -1.
	 */
	public int rowAt(Point p)
	{
		if (list == null || !list.contains(p))
		{
			return -1;
		}
		return (p.y - list.y) / rowHeight;
	}
}
