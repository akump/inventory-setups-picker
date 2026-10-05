package com.setuppicker;

import com.google.common.hash.Hashing;
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
import javax.inject.Inject;
import javax.inject.Singleton;
import net.runelite.api.EquipmentInventorySlot;
import net.runelite.api.gameval.ItemID;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.eventbus.EventBus;
import net.runelite.client.events.PluginMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Talks to the Inventory Setups plugin. Hub plugins live in separate classloaders, so everything goes
 * through its PluginMessage API (see InventorySetupsPluginMessageHandler in that plugin) or its saved config.
 */
@Singleton
public class SetupRepository
{
	private static final Logger log = LoggerFactory.getLogger(SetupRepository.class);

	static final String NAMESPACE = "inventory-setups";
	static final String MSG_SETUPS_CHANGED = "setups-changed";
	static final String MSG_ACTIVE_SETUP_CHANGED = "active-setup-changed";
	private static final String MSG_GET_SETUPS = "get-setups";
	private static final String MSG_GET_ACTIVE_SETUP_CONTENTS = "get-active-setup-contents";
	private static final String MSG_VIEW = "view";
	private static final String MSG_CLEAR = "clear";
	private static final String DATA_SETUPS = "setups";
	private static final String DATA_SETUP = "setup";
	static final String DATA_ACTIVE_SETUP = "activeSetup";

	// Inventory Setups stores each setup as json under setupsV3_<murmur3_128 of the name>
	static final String CONFIG_GROUP = "inventorysetups";
	static final String CONFIG_KEY_SETUP_PREFIX = "setupsV3_";

	private final EventBus eventBus;
	private final ConfigManager configManager;
	private final Gson gson;

	@Inject
	SetupRepository(EventBus eventBus, ConfigManager configManager, Gson gson)
	{
		this.eventBus = eventBus;
		this.configManager = configManager;
		this.gson = gson;
	}

	/**
	 * All setups. Empty if Inventory Setups isn't running.
	 *
	 * @param alphabetical sort by name, rather than keeping the order of Inventory Setups' own list
	 */
	public List<SetupEntry> loadSetups(boolean alphabetical, boolean favoritesFirst)
	{
		// post() is synchronous: Inventory Setups fills the collection before it returns
		final List<String> names = new ArrayList<>();
		final Map<String, Object> data = new HashMap<>();
		data.put(DATA_SETUPS, names);
		eventBus.post(new PluginMessage(NAMESPACE, MSG_GET_SETUPS, data));

		final List<SetupEntry> setups = new ArrayList<>(names.size());
		for (String name : names)
		{
			final String hash = Hashing.murmur3_128().hashUnencodedChars(name).toString();
			setups.add(parseSetup(gson, name, configManager.getConfiguration(CONFIG_GROUP, CONFIG_KEY_SETUP_PREFIX + hash)));
		}
		return Collections.unmodifiableList(sort(setups, alphabetical, favoritesFirst));
	}

	static List<SetupEntry> sort(List<SetupEntry> setups, boolean alphabetical, boolean favoritesFirst)
	{
		if (alphabetical)
		{
			setups.sort((a, b) -> String.CASE_INSENSITIVE_ORDER.compare(a.getName(), b.getName()));
		}
		if (favoritesFirst)
		{
			// stable, so favorites and the rest each keep the order they have by now
			setups.sort((a, b) -> Boolean.compare(b.isFavorite(), a.isFavorite()));
		}
		return setups;
	}

	/**
	 * Builds the list entry for a setup from its saved json. The name comes from the API, so a setup whose
	 * json is missing or unreadable still gets listed, just without its icon, color and favorite flag.
	 */
	static SetupEntry parseSetup(Gson gson, String name, String json)
	{
		boolean favorite = false;
		Color displayColor = null;
		int iconItemId = -1;
		try
		{
			final JsonObject setup = json == null ? null : gson.fromJson(json, JsonObject.class);
			if (setup != null)
			{
				favorite = setup.has("fv") && setup.get("fv").getAsBoolean();
				if (setup.has("dc"))
				{
					displayColor = gson.fromJson(setup.get("dc"), Color.class);
				}
				if (setup.has("iId"))
				{
					iconItemId = setup.get("iId").getAsInt();
				}
				if (iconItemId <= 0)
				{
					// Same fallback as Inventory Setups' icon view: the weapon
					iconItemId = itemIdAt(setup.getAsJsonArray("eq"), EquipmentInventorySlot.WEAPON.getSlotIdx());
				}
			}
		}
		catch (RuntimeException e)
		{
			log.debug("Couldn't read saved data for setup {}", name, e);
		}
		if (iconItemId <= 0)
		{
			iconItemId = ItemID._100GUIDE_GUIDECAKE;
		}
		return new SetupEntry(name, favorite, displayColor, iconItemId);
	}

	private static int itemIdAt(JsonArray items, int index)
	{
		if (items == null || index >= items.size())
		{
			return -1;
		}
		// empty slots are saved as null
		final JsonElement item = items.get(index);
		return item.isJsonObject() && item.getAsJsonObject().has("id") ? item.getAsJsonObject().get("id").getAsInt() : -1;
	}

	/**
	 * Name of the setup currently open in Inventory Setups, or "" when there is none or it can't be told
	 * (the API only reports setups that filter the bank). Call on the client thread, where the reply is
	 * synchronous.
	 */
	public String queryActiveSetup()
	{
		final Map<String, Object> data = new HashMap<>();
		data.put("equipmentItemIds", new ArrayList<Integer>());
		data.put("inventoryItemIds", new ArrayList<Integer>());
		data.put("additionalItemIds", new ArrayList<Integer>());
		eventBus.post(new PluginMessage(NAMESPACE, MSG_GET_ACTIVE_SETUP_CONTENTS, data));
		final Object active = data.get(DATA_ACTIVE_SETUP);
		return active instanceof String ? (String) active : "";
	}

	/**
	 * Open a setup in Inventory Setups, as if picked from its side panel.
	 */
	public void open(String name)
	{
		eventBus.post(new PluginMessage(NAMESPACE, MSG_VIEW, Map.of(DATA_SETUP, name)));
	}

	/**
	 * Close a setup, if it is still the one that's open.
	 */
	public void close(String name)
	{
		eventBus.post(new PluginMessage(NAMESPACE, MSG_CLEAR, Map.of(DATA_SETUP, name)));
	}
}
