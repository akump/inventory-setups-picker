package com.setuppicker;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import java.awt.Color;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.IntUnaryOperator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A setup's inventory and equipment along with how it has Inventory Setups highlight what differs from what
 * the player has: its "Highlight differences", "Unordered highlighting" and highlight color, and each item's
 * fuzzy and stack compare settings. Works out the slots that its side panel colors in.
 */
final class SetupHighlight
{
	private static final Logger log = LoggerFactory.getLogger(SetupHighlight.class);

	// What Inventory Setups' highlight color starts out as
	static final Color DEFAULT_COLOR = Color.RED;

	/**
	 * How an item's quantity is compared, named as Inventory Setups saves it.
	 */
	enum StackCompare
	{
		None,
		Standard,
		Greater_Than,
		Less_Than;

		static StackCompare parse(String name)
		{
			for (StackCompare value : values())
			{
				if (value.name().equals(name))
				{
					return value;
				}
			}
			return None;
		}

		/**
		 * Whether the quantity alone is enough for the slot to be highlighted.
		 */
		boolean differs(int saved, int current)
		{
			final int compared = Integer.compare(current, saved);
			switch (this)
			{
				case Standard:
					return compared != 0;
				case Greater_Than:
					return compared > 0;
				case Less_Than:
					return compared < 0;
				default:
					return false;
			}
		}
	}

	/**
	 * An item in a slot, of the setup or of the player. An empty slot has an id of -1.
	 */
	static final class Slot
	{
		static final Slot EMPTY = new Slot(-1, 0);

		final int id;
		final int quantity;
		final boolean fuzzy;
		final StackCompare stackCompare;

		Slot(int id, int quantity)
		{
			this(id, quantity, false, StackCompare.None);
		}

		Slot(int id, int quantity, boolean fuzzy, StackCompare stackCompare)
		{
			this.id = id;
			this.quantity = quantity;
			this.fuzzy = fuzzy;
			this.stackCompare = stackCompare;
		}

		boolean isEmpty()
		{
			return id <= 0;
		}
	}

	private final Color color;
	private final boolean unordered;
	private final List<Slot> inventory;
	private final List<Slot> equipment;
	// null for a setup without a rune pouch
	private final List<Slot> runePouch;

	SetupHighlight(Color color, boolean unordered, List<Slot> inventory, List<Slot> equipment)
	{
		this(color, unordered, inventory, equipment, null);
	}

	SetupHighlight(Color color, boolean unordered, List<Slot> inventory, List<Slot> equipment, List<Slot> runePouch)
	{
		this.color = color == null ? DEFAULT_COLOR : color;
		this.unordered = unordered;
		this.inventory = inventory;
		this.equipment = equipment;
		this.runePouch = runePouch;
	}

	/**
	 * Reads a setup's saved json, or returns null when the setup doesn't highlight its differences or can't be
	 * read.
	 */
	static SetupHighlight parse(Gson gson, String json)
	{
		try
		{
			final JsonObject setup = json == null ? null : gson.fromJson(json, JsonObject.class);
			// left out when off
			if (setup == null || !setup.has("hd") || !setup.get("hd").getAsBoolean())
			{
				return null;
			}
			final Color color = setup.has("hc") ? gson.fromJson(setup.get("hc"), Color.class) : null;
			final boolean unordered = setup.has("uh") && setup.get("uh").getAsBoolean();
			// left out for a setup without a rune pouch
			final List<Slot> runePouch = setup.has("rp") && setup.get("rp").isJsonArray() ? slots(setup.getAsJsonArray("rp")) : null;
			return new SetupHighlight(color, unordered, slots(setup.getAsJsonArray("inv")), slots(setup.getAsJsonArray("eq")), runePouch);
		}
		catch (RuntimeException e)
		{
			log.debug("Couldn't read how a setup is highlighted", e);
			return null;
		}
	}

	private static List<Slot> slots(JsonArray items)
	{
		if (items == null)
		{
			return Collections.emptyList();
		}
		final List<Slot> slots = new ArrayList<>(items.size());
		for (JsonElement element : items)
		{
			// an empty slot is saved as null, or as an item of id -1
			if (element == null || !element.isJsonObject() || !element.getAsJsonObject().has("id"))
			{
				slots.add(Slot.EMPTY);
				continue;
			}
			final JsonObject item = element.getAsJsonObject();
			// each of these is left out when it is the default
			slots.add(new Slot(
				item.get("id").getAsInt(),
				item.has("q") ? item.get("q").getAsInt() : 1,
				item.has("f") && item.get("f").getAsBoolean(),
				item.has("sc") ? StackCompare.parse(item.get("sc").getAsString()) : StackCompare.None));
		}
		return slots;
	}

	Color getColor()
	{
		return color;
	}

	/**
	 * The setup's items whose slots Inventory Setups highlights, given what the player has.
	 *
	 * @param currentInventory  the player's inventory, a slot each
	 * @param currentEquipment  what the player is wearing, by equipment slot
	 * @param currentRunePouch  the runes in the player's rune pouch, or null when they aren't carrying one
	 * @param variation         maps an item to the one id its variants share, for fuzzy items
	 */
	List<Slot> differences(List<Slot> currentInventory, List<Slot> currentEquipment, List<Slot> currentRunePouch,
		IntUnaryOperator variation)
	{
		final List<Slot> differences = new ArrayList<>();
		if (unordered)
		{
			unorderedDifferences(currentInventory, variation, differences);
		}
		else
		{
			orderedDifferences(inventory, currentInventory, variation, differences);
		}
		// Inventory Setups compares equipment slot by slot either way
		orderedDifferences(equipment, currentEquipment, variation, differences);
		if (runePouch != null)
		{
			runePouchDifferences(currentRunePouch, differences);
		}
		return differences;
	}

	/**
	 * Like Inventory Setups, each rune is looked for anywhere in the pouch, and without a pouch all of them are
	 * missing.
	 */
	private void runePouchDifferences(List<Slot> currentRunePouch, List<Slot> differences)
	{
		for (Slot wanted : runePouch)
		{
			if (wanted.isEmpty())
			{
				continue;
			}
			Slot have = null;
			if (currentRunePouch != null)
			{
				for (Slot slot : currentRunePouch)
				{
					if (slot.id == wanted.id)
					{
						have = slot;
						break;
					}
				}
			}
			if (have == null || wanted.stackCompare.differs(wanted.quantity, have.quantity))
			{
				differences.add(wanted);
			}
		}
	}

	private static void orderedDifferences(List<Slot> saved, List<Slot> current, IntUnaryOperator variation, List<Slot> differences)
	{
		for (int i = 0; i < saved.size(); i++)
		{
			final Slot wanted = saved.get(i);
			// a slot that should be empty has nothing in the bank to point at
			if (wanted.isEmpty())
			{
				continue;
			}
			final Slot have = i < current.size() ? current.get(i) : Slot.EMPTY;
			final boolean sameItem = wanted.fuzzy
				? variation.applyAsInt(wanted.id) == variation.applyAsInt(have.id)
				: wanted.id == have.id;
			if (!sameItem || wanted.stackCompare.differs(wanted.quantity, have.quantity))
			{
				differences.add(wanted);
			}
		}
	}

	/**
	 * Each item of the setup's inventory is matched to one anywhere in the player's, exact matches first and
	 * then, for fuzzy items, any variant.
	 */
	private void unorderedDifferences(List<Slot> currentInventory, IntUnaryOperator variation, List<Slot> differences)
	{
		// the quantities of the player's items, a slot each, by item
		final Map<Integer, List<Integer>> current = new HashMap<>();
		for (Slot have : currentInventory)
		{
			if (!have.isEmpty())
			{
				current.computeIfAbsent(have.id, id -> new ArrayList<>()).add(have.quantity);
			}
		}

		// the items that must be exact pick first, then the fuzzy ones take what exact matches are left
		final List<Slot> variants = new ArrayList<>();
		for (boolean fuzzy : new boolean[]{false, true})
		{
			for (Slot wanted : inventory)
			{
				if (wanted.isEmpty() || wanted.fuzzy != fuzzy)
				{
					continue;
				}
				final Integer quantity = take(current.get(wanted.id));
				if (quantity == null && fuzzy)
				{
					variants.add(wanted);
				}
				else if (quantity == null || wanted.stackCompare.differs(wanted.quantity, quantity))
				{
					differences.add(wanted);
				}
			}
		}

		for (Slot wanted : variants)
		{
			Integer quantity = null;
			final int base = variation.applyAsInt(wanted.id);
			for (Map.Entry<Integer, List<Integer>> entry : current.entrySet())
			{
				if (!entry.getValue().isEmpty() && variation.applyAsInt(entry.getKey()) == base)
				{
					quantity = take(entry.getValue());
					break;
				}
			}
			if (quantity == null || wanted.stackCompare.differs(wanted.quantity, quantity))
			{
				differences.add(wanted);
			}
		}
	}

	private static Integer take(List<Integer> quantities)
	{
		return quantities == null || quantities.isEmpty() ? null : quantities.remove(quantities.size() - 1);
	}
}
