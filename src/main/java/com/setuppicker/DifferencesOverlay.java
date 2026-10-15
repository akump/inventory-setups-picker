package com.setuppicker;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.Shape;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import javax.inject.Inject;
import javax.inject.Singleton;
import net.runelite.api.Client;
import net.runelite.api.EnumComposition;
import net.runelite.api.EnumID;
import net.runelite.api.Item;
import net.runelite.api.ItemContainer;
import net.runelite.api.gameval.InterfaceID;
import net.runelite.api.gameval.InventoryID;
import net.runelite.api.gameval.ItemID;
import net.runelite.api.gameval.VarbitID;
import net.runelite.api.widgets.Widget;
import net.runelite.client.game.ItemManager;
import net.runelite.client.game.ItemVariationMapping;
import net.runelite.client.ui.overlay.Overlay;
import net.runelite.client.ui.overlay.OverlayLayer;
import net.runelite.client.ui.overlay.OverlayPosition;

/**
 * Outlines the items in the bank whose slots Inventory Setups' side panel highlights for the open setup, in the
 * setup's highlight color. Only for setups with "Highlight differences" on.
 */
@Singleton
public class DifferencesOverlay extends Overlay
{
	// how much of the highlight color shows through over the item
	private static final int FILL_ALPHA = 50;

	private static final int[] RUNE_POUCHES = {
		ItemID.BH_RUNE_POUCH, ItemID.BH_RUNE_POUCH_TROUVER, ItemID.DIVINE_RUNE_POUCH, ItemID.DIVINE_RUNE_POUCH_TROUVER,
	};
	// the fourth slot is the divine rune pouch's, and stays empty in the others
	private static final int[] RUNE_POUCH_TYPES = {
		VarbitID.RUNE_POUCH_TYPE_1, VarbitID.RUNE_POUCH_TYPE_2, VarbitID.RUNE_POUCH_TYPE_3, VarbitID.RUNE_POUCH_TYPE_4,
	};
	private static final int[] RUNE_POUCH_QUANTITIES = {
		VarbitID.RUNE_POUCH_QUANTITY_1, VarbitID.RUNE_POUCH_QUANTITY_2, VarbitID.RUNE_POUCH_QUANTITY_3, VarbitID.RUNE_POUCH_QUANTITY_4,
	};

	private final Client client;
	private final SetupPickerConfig config;
	private final ItemManager itemManager;

	// null when there's no open setup, or it doesn't highlight its differences
	private volatile SetupHighlight highlight;

	@Inject
	DifferencesOverlay(Client client, SetupPickerConfig config, ItemManager itemManager)
	{
		this.client = client;
		this.config = config;
		this.itemManager = itemManager;
		setPosition(OverlayPosition.DYNAMIC);
		setLayer(OverlayLayer.ABOVE_WIDGETS);
		// drawn first, so that the picker and anything else over the bank goes on top of the outlines
		setPriority(PRIORITY_LOW);
	}

	public void setHighlight(SetupHighlight highlight)
	{
		this.highlight = highlight;
	}

	private List<SetupHighlight.Slot> current(int containerId)
	{
		final ItemContainer container = client.getItemContainer(containerId);
		if (container == null)
		{
			return Collections.emptyList();
		}
		final Item[] items = container.getItems();
		final List<SetupHighlight.Slot> slots = new ArrayList<>(items.length);
		for (Item item : items)
		{
			slots.add(new SetupHighlight.Slot(item.getId(), item.getQuantity()));
		}
		return slots;
	}

	/**
	 * The runes in the rune pouch in the player's inventory, or null when there's no rune pouch there.
	 */
	private List<SetupHighlight.Slot> currentRunePouch()
	{
		final ItemContainer inventory = client.getItemContainer(InventoryID.INV);
		boolean carried = false;
		for (int pouch : RUNE_POUCHES)
		{
			carried |= inventory != null && inventory.contains(pouch);
		}
		if (!carried)
		{
			return null;
		}
		// the varbits hold each rune by its position in this enum, 0 for none
		final EnumComposition runes = client.getEnum(EnumID.RUNEPOUCH_RUNE);
		final List<SetupHighlight.Slot> slots = new ArrayList<>(RUNE_POUCH_TYPES.length);
		for (int i = 0; i < RUNE_POUCH_TYPES.length; i++)
		{
			final int type = client.getVarbitValue(RUNE_POUCH_TYPES[i]);
			slots.add(type == 0 ? SetupHighlight.Slot.EMPTY
				: new SetupHighlight.Slot(runes.getIntValue(type), client.getVarbitValue(RUNE_POUCH_QUANTITIES[i])));
		}
		return slots;
	}

	@Override
	public Dimension render(Graphics2D graphics)
	{
		final SetupHighlight highlight = this.highlight;
		if (!config.highlightDifferences() || highlight == null)
		{
			return null;
		}
		final Widget items = client.getWidget(InterfaceID.Bankmain.ITEMS);
		if (items == null || items.isHidden())
		{
			return null;
		}
		final List<SetupHighlight.Slot> differences = highlight.differences(
			current(InventoryID.INV), current(InventoryID.WORN), currentRunePouch(), ItemVariationMapping::map);
		if (differences.isEmpty())
		{
			return null;
		}

		// the bank has items unnoted, and placeholders for those that have run out
		final Set<Integer> exact = new HashSet<>();
		final Set<Integer> variants = new HashSet<>();
		for (SetupHighlight.Slot slot : differences)
		{
			final int id = itemManager.canonicalize(slot.id);
			exact.add(id);
			if (slot.fuzzy)
			{
				variants.add(ItemVariationMapping.map(id));
			}
		}

		final Widget[] children = items.getDynamicChildren();
		if (children == null)
		{
			return null;
		}
		final Color color = config.overrideDifferenceColor() ? config.differenceColor() : highlight.getColor();
		final Color fill = new Color(color.getRed(), color.getGreen(), color.getBlue(), FILL_ALPHA);
		// the bank scrolls, so leave out what has been scrolled out of sight
		final Shape clip = graphics.getClip();
		graphics.clip(items.getBounds());
		for (Widget child : children)
		{
			if (child == null || child.isHidden() || child.getItemId() <= 0)
			{
				continue;
			}
			final int id = itemManager.canonicalize(child.getItemId());
			if (!exact.contains(id) && !variants.contains(ItemVariationMapping.map(id)))
			{
				continue;
			}
			final Rectangle slot = child.getBounds();
			graphics.setColor(fill);
			graphics.fill(slot);
			graphics.setColor(color);
			graphics.drawRect(slot.x, slot.y, slot.width - 1, slot.height - 1);
		}
		graphics.setClip(clip);
		return null;
	}
}
