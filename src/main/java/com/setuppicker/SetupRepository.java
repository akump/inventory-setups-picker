package com.setuppicker;

import com.google.common.hash.Hashing;
import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import java.awt.Color;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.inject.Inject;
import javax.inject.Singleton;
import net.runelite.api.EquipmentInventorySlot;
import net.runelite.api.gameval.ItemID;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.eventbus.EventBus;
import net.runelite.client.events.PluginMessage;
import net.runelite.client.plugins.banktags.BankTagsService;
import net.runelite.client.util.Text;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Talks to the Inventory Setups plugin. Hub plugins live in separate classloaders, so everything goes
 * through its PluginMessage API (see InventorySetupsPluginMessageHandler in that plugin) or its saved config.
 * Also reads and opens the tag tabs of the client's own Bank Tags plugin.
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
	private static final int MAX_RECENT = 10;

	static final String CONFIG_KEY_SETUP_PREFIX = "setupsV3_";
	// and all the sections together, as a json array
	static final String CONFIG_KEY_SECTIONS = "sections";

	// Bank Tags keeps its tabs as a csv of their names, and each tab's icon under icon_<standardized name>
	static final String BANK_TAGS_CONFIG_GROUP = "banktags";
	static final String BANK_TAGS_TABS_KEY = "tagtabs";
	static final String BANK_TAGS_ICON_PREFIX = "icon_";

	private final EventBus eventBus;
	private final ConfigManager configManager;
	private final Gson gson;
	private final BankTagsService bankTagsService;

	@Inject
	SetupRepository(EventBus eventBus, ConfigManager configManager, Gson gson, BankTagsService bankTagsService)
	{
		this.eventBus = eventBus;
		this.configManager = configManager;
		this.gson = gson;
		this.bankTagsService = bankTagsService;
	}

	/**
	 * All bank tag tabs, in the order of the Bank Tags plugin's tabs. Unlike Inventory Setups, Bank Tags is part
	 * of the client, so its service can be used directly.
	 */
	public List<SetupEntry> loadBankTags()
	{
		final String savedTabs = configManager.getConfiguration(BANK_TAGS_CONFIG_GROUP, BANK_TAGS_TABS_KEY);
		final List<String> names = Text.fromCSV(savedTabs == null ? "" : savedTabs);
		final List<SetupEntry> tags = new ArrayList<>(names.size());
		for (String name : names)
		{
			// Same fallback as Bank Tags' own tabs: the spade
			int iconItemId = ItemID.SPADE;
			try
			{
				final String icon = configManager.getConfiguration(BANK_TAGS_CONFIG_GROUP, BANK_TAGS_ICON_PREFIX + Text.standardize(name));
				if (icon != null)
				{
					iconItemId = Integer.parseInt(icon);
				}
			}
			catch (NumberFormatException e)
			{
				log.debug("Couldn't read the icon of bank tag {}", name, e);
			}
			tags.add(SetupEntry.bankTag(name, iconItemId));
		}
		return tags;
	}

	/**
	 * Name of the bank tag currently open, or "" when there is none. Call on the client thread.
	 */
	public String queryActiveBankTag()
	{
		final String active = bankTagsService.getActiveTag();
		return active == null ? "" : active;
	}

	/**
	 * Open a bank tag, along with its layout if it has one, as if its tab was clicked.
	 */
	public void openBankTag(String name)
	{
		bankTagsService.openBankTag(name, BankTagsService.OPTION_ALLOW_MODIFICATIONS);
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
	 * Puts a setup at the front of the recently used, which are kept latest first and capped in number.
	 *
	 * @param name the setup's key: its name, or for a bank tag {@link SetupEntry#bankTagKey}
	 * @return the new list, or the same one if the setup was already at the front
	 */
	static List<String> markUsed(List<String> recent, String name)
	{
		if (!recent.isEmpty() && recent.get(0).equals(name))
		{
			return recent;
		}
		final List<String> updated = new ArrayList<>(recent.size() + 1);
		updated.add(name);
		for (String other : recent)
		{
			if (!other.equals(name) && updated.size() < MAX_RECENT)
			{
				updated.add(other);
			}
		}
		return updated;
	}

	/**
	 * The keys of the recently used setups and bank tags saved by {@link #saveRecent}, latest first.
	 */
	public List<String> loadRecent()
	{
		try
		{
			final String[] names = gson.fromJson(configManager.getConfiguration(SetupPickerConfig.GROUP, SetupPickerConfig.KEY_RECENT), String[].class);
			return names == null ? Collections.emptyList() : Arrays.asList(names);
		}
		catch (RuntimeException e)
		{
			log.debug("Couldn't read the recently used setups", e);
			return Collections.emptyList();
		}
	}

	public void saveRecent(List<String> recent)
	{
		configManager.setConfiguration(SetupPickerConfig.GROUP, SetupPickerConfig.KEY_RECENT, gson.toJson(recent));
	}

	/**
	 * Builds the list entry for a setup from its saved json. The name comes from the API, so a setup whose
	 * json is missing or unreadable still gets listed, just without its icon, color, favorite flag and notes.
	 */
	static SetupEntry parseSetup(Gson gson, String name, String json)
	{
		boolean favorite = false;
		Color displayColor = null;
		int iconItemId = -1;
		String notes = "";
		int spellbook = SetupEntry.NO_SPELLBOOK;
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
				if (setup.has("notes") && setup.get("notes").isJsonPrimitive())
				{
					notes = setup.get("notes").getAsString().trim();
				}
				// Left out when it's the standard one, which is 0. 4 is for "none".
				spellbook = setup.has("sb") ? setup.get("sb").getAsInt() : 0;
				if (spellbook < 0 || spellbook > 3)
				{
					spellbook = SetupEntry.NO_SPELLBOOK;
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
		return new SetupEntry(name, favorite, displayColor, iconItemId, notes, spellbook);
	}

	/**
	 * The sections setups are grouped into, in the order of Inventory Setups' side panel. There is no API for
	 * these, so they are read from where Inventory Setups saves them.
	 */
	public List<SetupSection> loadSections()
	{
		return parseSections(gson, configManager.getConfiguration(CONFIG_GROUP, CONFIG_KEY_SECTIONS));
	}

	/**
	 * Reads the saved sections, skipping any that can't be made sense of.
	 */
	static List<SetupSection> parseSections(Gson gson, String json)
	{
		final List<SetupSection> sections = new ArrayList<>();
		try
		{
			final JsonArray saved = json == null ? null : gson.fromJson(json, JsonArray.class);
			if (saved == null)
			{
				return sections;
			}
			for (JsonElement element : saved)
			{
				if (!element.isJsonObject() || !element.getAsJsonObject().has("name"))
				{
					continue;
				}
				final JsonObject section = element.getAsJsonObject();
				final List<String> setupNames = new ArrayList<>();
				if (section.has("setups") && section.get("setups").isJsonArray())
				{
					for (JsonElement setup : section.getAsJsonArray("setups"))
					{
						setupNames.add(setup.getAsString());
					}
				}
				final Color displayColor = section.has("displayColor") ? gson.fromJson(section.get("displayColor"), Color.class) : null;
				sections.add(new SetupSection(section.get("name").getAsString(), displayColor, Collections.unmodifiableList(setupNames)));
			}
		}
		catch (RuntimeException e)
		{
			log.debug("Couldn't read the saved sections", e);
		}
		return sections;
	}

	/**
	 * Lays the setups out under a heading per section, followed by the ones in no section. A setup is listed
	 * under every section it is in. Without any sections in use, it's just the setups.
	 *
	 * @param setups       every setup, in the order to list them
	 * @param alphabetical sort each section by name, rather than keeping the order it has in Inventory Setups
	 */
	static List<PickerRow> group(List<SetupEntry> setups, List<SetupSection> sections, boolean alphabetical, boolean favoritesFirst)
	{
		return group(setups, sections, alphabetical, favoritesFirst, Collections.emptyList());
	}

	/**
	 * @param setups may have bank tags among them. Those aren't in any section, so when the list is in sections
	 *               they go under a heading of their own at the end.
	 * @param recent keys of the recently used setups to list at the top, latest first. Empty for none. When
	 *               the list is in sections they get a heading of their own and are listed in their sections
	 *               as well; otherwise they are simply moved to the top.
	 */
	static List<PickerRow> group(List<SetupEntry> setups, List<SetupSection> sections, boolean alphabetical, boolean favoritesFirst,
		List<String> recent)
	{
		final Map<String, SetupEntry> byKey = new HashMap<>();
		for (SetupEntry setup : setups)
		{
			byKey.put(setup.getKey(), setup);
		}

		final List<PickerRow> rows = new ArrayList<>();
		final Set<String> assigned = new HashSet<>();
		for (SetupSection section : sections)
		{
			final List<SetupEntry> members = new ArrayList<>();
			for (String name : section.getSetupNames())
			{
				// a setup's key is its name, so this never finds a bank tag that happens to be called the same
				final SetupEntry setup = byKey.get(name);
				if (setup != null)
				{
					members.add(setup);
				}
			}
			if (members.isEmpty())
			{
				continue;
			}
			rows.add(PickerRow.header(section));
			for (SetupEntry setup : sort(members, alphabetical, favoritesFirst))
			{
				rows.add(PickerRow.of(setup));
				assigned.add(setup.getKey());
			}
		}

		final boolean sectioned = !rows.isEmpty();

		final List<PickerRow> recentRows = new ArrayList<>();
		for (String key : recent)
		{
			// one that has since been deleted or renamed is just left out
			final SetupEntry setup = byKey.get(key);
			if (setup != null)
			{
				recentRows.add(PickerRow.recent(setup));
				if (!sectioned)
				{
					assigned.add(key);
				}
			}
		}
		if (!recentRows.isEmpty())
		{
			if (sectioned)
			{
				recentRows.add(0, PickerRow.RECENT);
			}
			rows.addAll(0, recentRows);
		}

		if (!sectioned)
		{
			for (SetupEntry setup : setups)
			{
				if (!assigned.contains(setup.getKey()))
				{
					rows.add(PickerRow.of(setup));
				}
			}
			return rows;
		}
		addUnder(rows, PickerRow.UNASSIGNED, setups, assigned, false);
		addUnder(rows, PickerRow.BANK_TAGS, setups, assigned, true);
		return rows;
	}

	/**
	 * Lists the setups that aren't in a section, or the bank tags, under a heading that is left out if there are none.
	 */
	private static void addUnder(List<PickerRow> rows, PickerRow heading, List<SetupEntry> setups, Set<String> assigned, boolean bankTags)
	{
		boolean headed = false;
		for (SetupEntry setup : setups)
		{
			if (setup.isBankTag() != bankTags || assigned.contains(setup.getKey()))
			{
				continue;
			}
			if (!headed)
			{
				rows.add(heading);
				headed = true;
			}
			rows.add(PickerRow.of(setup));
		}
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
