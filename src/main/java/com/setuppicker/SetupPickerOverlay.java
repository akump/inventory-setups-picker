package com.setuppicker;

import java.awt.Dimension;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.Rectangle;
import javax.inject.Inject;
import javax.inject.Singleton;
import net.runelite.api.Client;
import net.runelite.api.gameval.InterfaceID;
import net.runelite.api.widgets.Widget;
import net.runelite.client.game.ItemManager;
import net.runelite.client.ui.FontManager;
import net.runelite.client.ui.overlay.Overlay;
import net.runelite.client.ui.overlay.OverlayLayer;
import net.runelite.client.ui.overlay.OverlayPosition;

/**
 * Draws the picker: as a popup while it has been opened with the hotkey, otherwise beside the bank
 * whenever the bank is open.
 */
@Singleton
public class SetupPickerOverlay extends Overlay
{
	private static final int PALETTE_WIDTH = 240;

	private final Client client;
	private final SetupPickerConfig config;
	private final PickerModel model;
	private final ItemManager itemManager;

	// Layout of the last rendered frame, read by the input listeners. Null while the picker isn't showing.
	private volatile PickerLayout layout;
	private volatile String status = "";
	private volatile Point mouse;

	@Inject
	SetupPickerOverlay(Client client, SetupPickerConfig config, PickerModel model, ItemManager itemManager)
	{
		this.client = client;
		this.config = config;
		this.model = model;
		this.itemManager = itemManager;
		// Positions itself, in canvas coordinates
		setPosition(OverlayPosition.DYNAMIC);
		setLayer(OverlayLayer.ABOVE_WIDGETS);
	}

	public PickerLayout getLayout()
	{
		return layout;
	}

	/**
	 * Message to show in place of the list when there are no setups.
	 */
	public void setStatus(String status)
	{
		this.status = status;
	}

	public void clearLayout()
	{
		layout = null;
		mouse = null;
	}

	/**
	 * @param mouse where the mouse is over the picker, which is kept from the game (see PickerInput), or null
	 *              when it's elsewhere
	 */
	public void setMouse(Point mouse)
	{
		this.mouse = mouse;
	}

	@Override
	public Dimension render(Graphics2D graphics)
	{
		final PickerModel.View view = model.view();
		final int rowHeight = config.showIcons() ? PickerLayout.ROW_HEIGHT_ICONS : PickerLayout.ROW_HEIGHT_TEXT;
		final PickerLayout newLayout;
		if (view.isPaletteOpen())
		{
			newLayout = PickerLayout.computePalette(new Rectangle(client.getCanvasWidth(), client.getCanvasHeight()),
				PALETTE_WIDTH, view.getRows().size(), rowHeight, config.popupRows());
		}
		else
		{
			final Widget bank = config.showBesideBank() ? client.getWidget(InterfaceID.Bankmain.UNIVERSE) : null;
			if (bank == null || bank.isHidden())
			{
				layout = null;
				return null;
			}
			newLayout = PickerLayout.compute(bank.getBounds(), client.getCanvasWidth(),
				config.side() == SetupPickerConfig.Side.LEFT, config.width(), config.collapsed(), config.verticalWhenCollapsed(),
				view.getRows().size(), rowHeight);
		}
		// clamps the scroll position to what now fits, so take the view afterwards
		model.setVisibleRows(newLayout.getVisibleRows());
		layout = newLayout;

		// over the picker the game doesn't know where the mouse is; anywhere else it does
		final Point hidden = mouse;
		final net.runelite.api.Point seen = client.getMouseCanvasPosition();
		// unless the picker has since moved out from under it
		final Point hover = hidden != null && newLayout.getBounds().contains(hidden) ? hidden : (seen == null ? null : new Point(seen.getX(), seen.getY()));
		final PickerTheme theme = new PickerTheme(config.backgroundColor(), config.headerColor(), config.borderColor(),
			config.accentColor(), config.textColor());
		final String title = config.source() == SetupPickerConfig.Source.BANK_TAGS ? "Bank tags" : "Setups";
		new PickerPainter(theme, title).paint(graphics, newLayout, model.view(),
			hover,
			config.showIcons() ? itemManager::getImage : null,
			FontManager.getRunescapeSmallFont(), status);
		return null;
	}
}
