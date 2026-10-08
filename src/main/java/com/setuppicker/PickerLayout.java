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
	static final int COLLAPSED_WIDTH = HEADER_HEIGHT;
	static final int COLLAPSED_LENGTH = 96;
	static final int SCROLLBAR_WIDTH = 6;
	static final int SCROLL_THUMB_MIN = 12;

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
	 * @param setupCount number of rows to list (setups and section headings), after the search is applied
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
	 * @param uprightTab when collapsed, be a narrow upright tab rather than just the title bar
	 * @param setupCount number of rows to list (setups and section headings), after the search is applied
	 * @param maxRows    most rows to show before scrolling, or 0 for as many as fit down the bank's side
	 */
	public static PickerLayout compute(Rectangle bank, int canvasWidth, boolean preferLeft, int width,
		boolean collapsed, boolean uprightTab, int setupCount, int rowHeight, int maxRows)
	{
		final boolean upright = collapsed && uprightTab;
		if (upright)
		{
			// A narrow tab standing against the bank's edge with its title running up it
			width = COLLAPSED_WIDTH;
		}
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

		if (upright)
		{
			final Rectangle tab = new Rectangle(x, y, width, Math.min(COLLAPSED_LENGTH, bank.height));
			return new PickerLayout(tab, tab, null, null, null, rowHeight, 0);
		}
		return docked(x, y, width, collapsed, bank.y + bank.height, setupCount, rowHeight, maxRows);
	}

	/**
	 * The list of the bank's, dragged away from the bank's side to somewhere of its own.
	 *
	 * @param offset     where its top left corner is from the bank's, which is kept on the canvas
	 * @param setupCount number of rows to list (setups and section headings), after the search is applied
	 * @param maxRows    most rows to show before scrolling, or 0 for as many as fit above the canvas's bottom
	 */
	public static PickerLayout computeMoved(Rectangle bank, Rectangle canvas, Point offset, int width,
		boolean collapsed, int setupCount, int rowHeight, int maxRows)
	{
		final Point corner = clamp(new Point(bank.x + offset.x, bank.y + offset.y), width, canvas);
		return docked(corner.x, corner.y, width, collapsed, canvas.y + canvas.height, setupCount, rowHeight, maxRows);
	}

	/**
	 * The nearest place to a corner that keeps a list of this width, or at least its title bar, on the canvas,
	 * where it can be taken hold of again.
	 */
	public static Point clamp(Point corner, int width, Rectangle canvas)
	{
		final int x = Math.max(canvas.x, Math.min(corner.x, canvas.x + canvas.width - width));
		final int y = Math.max(canvas.y, Math.min(corner.y, canvas.y + canvas.height - HEADER_HEIGHT));
		return new Point(x, y);
	}

	/**
	 * @param bottom how far down the canvas the list may reach
	 */
	private static PickerLayout docked(int x, int y, int width, boolean collapsed, int bottom, int setupCount,
		int rowHeight, int maxRows)
	{
		final Rectangle header = new Rectangle(x, y, width, HEADER_HEIGHT);
		if (collapsed)
		{
			// just the title bar
			return new PickerLayout(header, header, null, null, null, rowHeight, 0);
		}
		final Rectangle search = new Rectangle(x + PADDING, y + HEADER_HEIGHT + PADDING, width - 2 * PADDING, SEARCH_HEIGHT);
		final int listY = search.y + search.height + PADDING;
		final int fittingRows = Math.max(1, (bottom - listY - PADDING) / rowHeight);
		// always leave one row's worth of space for the "no setups" message
		final int visibleRows = Math.max(1, Math.min(setupCount, maxRows > 0 ? Math.min(maxRows, fittingRows) : fittingRows));
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
	 * The area holding the rows, or null when collapsed.
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

	/**
	 * Whether this is the collapsed list in its upright tab form.
	 */
	public boolean isUprightTab()
	{
		return list == null && header.height > header.width;
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
	 * The strip down the right of the list that the scrollbar runs in, or null when everything fits.
	 *
	 * @param rowCount number of rows there are to scroll through
	 */
	public Rectangle getScrollbar(int rowCount)
	{
		if (list == null || rowCount <= visibleRows)
		{
			return null;
		}
		return new Rectangle(list.x + list.width - SCROLLBAR_WIDTH, list.y, SCROLLBAR_WIDTH, list.height);
	}

	/**
	 * The part of the scrollbar that stands for the rows in view, or null when everything fits.
	 *
	 * @param scroll index of the first visible row
	 */
	public Rectangle getScrollThumb(int rowCount, int scroll)
	{
		final Rectangle bar = getScrollbar(rowCount);
		if (bar == null)
		{
			return null;
		}
		final int height = Math.max(SCROLL_THUMB_MIN, bar.height * visibleRows / rowCount);
		final int maxScroll = rowCount - visibleRows;
		final int y = bar.y + (bar.height - height) * Math.max(0, Math.min(scroll, maxScroll)) / maxScroll;
		return new Rectangle(bar.x, y, bar.width, height);
	}

	/**
	 * The scroll position that puts the top of the scrollbar's thumb nearest to a height on the canvas, for
	 * dragging it.
	 */
	public int scrollAt(int rowCount, int thumbY)
	{
		final Rectangle thumb = getScrollThumb(rowCount, 0);
		if (thumb == null || list.height <= thumb.height)
		{
			return 0;
		}
		final int maxScroll = rowCount - visibleRows;
		final int travel = list.height - thumb.height;
		final int offset = Math.max(0, Math.min(thumbY - list.y, travel));
		return (offset * maxScroll + travel / 2) / travel;
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
