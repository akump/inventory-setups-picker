package com.setuppicker;

import java.awt.Color;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.Polygon;
import java.awt.Rectangle;
import java.awt.image.BufferedImage;
import java.util.List;
import java.util.function.IntFunction;

/**
 * Draws the picker. Has no dependency on the game client so it can be rendered in tests.
 */
public final class PickerPainter
{
	private static final Color BACKGROUND = new Color(40, 34, 26, 240);
	private static final Color BORDER_OUTER = new Color(14, 13, 15);
	private static final Color BORDER_INNER = new Color(87, 80, 64);
	private static final Color HEADER_BACKGROUND = new Color(62, 53, 41);
	private static final Color FIELD_BACKGROUND = new Color(0, 0, 0, 110);
	private static final Color ACCENT = new Color(255, 152, 31);
	private static final Color TEXT = new Color(235, 228, 210);
	private static final Color TEXT_MUTED = new Color(150, 142, 125);
	private static final Color ROW_HOVER = new Color(255, 255, 255, 28);
	private static final Color ROW_ACTIVE = new Color(255, 152, 31, 70);
	private static final Color FAVORITE = new Color(255, 215, 0);
	private static final Color SCROLLBAR = new Color(120, 110, 90);

	private static final int ICON_WIDTH = 18;
	private static final int ICON_HEIGHT = 16;
	private static final int STAR_SIZE = 7;
	private static final int SCROLLBAR_WIDTH = 3;

	private PickerPainter()
	{
	}

	/**
	 * @param mouse  mouse position on the canvas, or null
	 * @param icons  item id to image, or null to draw no icons. May return null for an image that isn't ready.
	 * @param status message for the empty list when there are no setups at all
	 */
	public static void paint(Graphics2D g, PickerLayout layout, PickerModel.View view, Point mouse,
		IntFunction<BufferedImage> icons, Font font, String status)
	{
		g.setFont(font);
		final FontMetrics fm = g.getFontMetrics();

		final Rectangle bounds = layout.getBounds();
		g.setColor(BACKGROUND);
		g.fill(bounds);

		paintHeader(g, fm, layout, view, mouse);

		if (!layout.isCollapsed())
		{
			paintSearch(g, fm, layout.getSearch(), view);
			paintList(g, fm, layout, view, mouse, icons, status);
		}
		if (layout.isPalette())
		{
			final SetupEntry selected = view.getSelected() < view.getSetups().size() ? view.getSetups().get(view.getSelected()) : null;
			final boolean closes = selected != null && selected.getName().equals(view.getActiveSetup());
			g.setColor(TEXT_MUTED);
			g.drawString(truncate(fm, "Up/Down: move   Enter: " + (closes ? "close" : "open") + "   Esc: cancel",
				layout.getFooter().width - 8), layout.getFooter().x + 4, baseline(fm, layout.getFooter()));
		}

		g.setColor(BORDER_INNER);
		g.drawRect(bounds.x + 1, bounds.y + 1, bounds.width - 3, bounds.height - 3);
		g.setColor(BORDER_OUTER);
		g.drawRect(bounds.x, bounds.y, bounds.width - 1, bounds.height - 1);
	}

	private static void paintHeader(Graphics2D g, FontMetrics fm, PickerLayout layout, PickerModel.View view, Point mouse)
	{
		final Rectangle header = layout.getHeader();
		g.setColor(HEADER_BACKGROUND);
		g.fill(header);
		if (!layout.isPalette() && mouse != null && header.contains(mouse))
		{
			g.setColor(ROW_HOVER);
			g.fill(header);
		}

		g.setColor(ACCENT);
		final String title = view.getTotal() > 0 ? "Setups (" + view.getTotal() + ")" : "Setups";
		g.drawString(title, header.x + 7, baseline(fm, header));
		if (layout.isPalette())
		{
			return;
		}

		// collapse arrow: points down when open, right when collapsed
		final int cx = header.x + header.width - 11;
		final int cy = header.y + header.height / 2;
		final Polygon arrow = new Polygon();
		if (layout.isCollapsed())
		{
			arrow.addPoint(cx - 2, cy - 4);
			arrow.addPoint(cx + 3, cy);
			arrow.addPoint(cx - 2, cy + 4);
		}
		else
		{
			arrow.addPoint(cx - 4, cy - 2);
			arrow.addPoint(cx + 4, cy - 2);
			arrow.addPoint(cx, cy + 3);
		}
		g.fill(arrow);
	}

	private static void paintSearch(Graphics2D g, FontMetrics fm, Rectangle search, PickerModel.View view)
	{
		g.setColor(FIELD_BACKGROUND);
		g.fill(search);
		g.setColor(view.isSearchFocused() ? ACCENT : BORDER_INNER);
		g.drawRect(search.x, search.y, search.width - 1, search.height - 1);

		final int textX = search.x + 5;
		final int maxWidth = search.width - 10;
		if (view.getQuery().isEmpty() && !view.isSearchFocused())
		{
			g.setColor(TEXT_MUTED);
			g.drawString("Search...", textX, baseline(fm, search));
			return;
		}

		// show the end of a long query, since that's where the typing happens
		String query = view.getQuery();
		while (!query.isEmpty() && fm.stringWidth(query) > maxWidth - 4)
		{
			query = query.substring(1);
		}
		g.setColor(TEXT);
		g.drawString(query, textX, baseline(fm, search));

		if (view.isSearchFocused() && System.currentTimeMillis() / 500 % 2 == 0)
		{
			final int caretX = textX + fm.stringWidth(query) + 1;
			g.drawLine(caretX, search.y + 4, caretX, search.y + search.height - 5);
		}
	}

	private static void paintList(Graphics2D g, FontMetrics fm, PickerLayout layout, PickerModel.View view, Point mouse,
		IntFunction<BufferedImage> icons, String status)
	{
		final Rectangle list = layout.getList();
		final List<SetupEntry> setups = view.getSetups();

		if (setups.isEmpty())
		{
			final String message = view.getTotal() > 0 ? "No matches" : status;
			g.setColor(TEXT_MUTED);
			g.drawString(truncate(fm, message, list.width - 8), list.x + 4, baseline(fm, list));
			return;
		}

		final boolean scrollable = setups.size() > layout.getVisibleRows();
		final int rowWidth = list.width - (scrollable ? SCROLLBAR_WIDTH + 2 : 0);

		for (int i = 0; i < layout.getVisibleRows(); i++)
		{
			final int index = view.getScroll() + i;
			if (index >= setups.size())
			{
				break;
			}
			final SetupEntry setup = setups.get(index);
			final Rectangle row = layout.getRow(i);
			row.width = rowWidth;

			final boolean active = setup.getName().equals(view.getActiveSetup());
			final boolean hovered = mouse != null && row.contains(mouse);
			final boolean selected = view.isSearchFocused() && index == view.getSelected();
			if (active)
			{
				g.setColor(ROW_ACTIVE);
				g.fill(row);
			}
			if (hovered || selected)
			{
				g.setColor(ROW_HOVER);
				g.fill(row);
			}
			if (selected)
			{
				g.setColor(ACCENT);
				g.drawRect(row.x, row.y, row.width - 1, row.height - 1);
			}

			int textX = row.x + 4;
			if (icons != null)
			{
				final BufferedImage icon = icons.apply(setup.getIconItemId());
				if (icon != null)
				{
					g.drawImage(icon, row.x + 2, row.y + (row.height - ICON_HEIGHT) / 2, ICON_WIDTH, ICON_HEIGHT, null);
				}
				textX = row.x + 2 + ICON_WIDTH + 4;
			}

			int textRight = row.x + row.width - 4;
			if (setup.isFavorite())
			{
				textRight -= STAR_SIZE + 3;
				paintStar(g, textRight + 3 + STAR_SIZE / 2, row.y + row.height / 2);
			}

			g.setColor(setup.getDisplayColor() != null ? setup.getDisplayColor() : (active ? ACCENT : TEXT));
			g.drawString(truncate(fm, setup.getName(), textRight - textX), textX, baseline(fm, row));
		}

		if (scrollable)
		{
			final int barHeight = Math.max(8, list.height * layout.getVisibleRows() / setups.size());
			final int maxScroll = setups.size() - layout.getVisibleRows();
			final int barY = list.y + (list.height - barHeight) * view.getScroll() / maxScroll;
			g.setColor(SCROLLBAR);
			g.fillRect(list.x + list.width - SCROLLBAR_WIDTH, barY, SCROLLBAR_WIDTH, barHeight);
		}
	}

	private static void paintStar(Graphics2D g, int cx, int cy)
	{
		final Polygon star = new Polygon();
		for (int i = 0; i < 10; i++)
		{
			final double radius = i % 2 == 0 ? STAR_SIZE / 2.0 + 0.5 : STAR_SIZE / 4.0;
			final double angle = -Math.PI / 2 + i * Math.PI / 5;
			star.addPoint((int) Math.round(cx + radius * Math.cos(angle)), (int) Math.round(cy + radius * Math.sin(angle)));
		}
		g.setColor(FAVORITE);
		g.fill(star);
	}

	private static int baseline(FontMetrics fm, Rectangle box)
	{
		return box.y + (box.height - fm.getAscent() - fm.getDescent()) / 2 + fm.getAscent();
	}

	private static String truncate(FontMetrics fm, String text, int maxWidth)
	{
		if (fm.stringWidth(text) <= maxWidth)
		{
			return text;
		}
		final String ellipsis = "...";
		String cut = text;
		while (!cut.isEmpty() && fm.stringWidth(cut + ellipsis) > maxWidth)
		{
			cut = cut.substring(0, cut.length() - 1);
		}
		return cut + ellipsis;
	}
}
