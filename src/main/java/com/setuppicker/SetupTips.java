package com.setuppicker;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import java.util.function.Predicate;
import javax.inject.Inject;
import javax.inject.Singleton;
import net.runelite.api.ChatMessageType;
import net.runelite.api.Client;
import net.runelite.api.GameState;
import net.runelite.client.chat.ChatColorType;
import net.runelite.client.chat.ChatMessageBuilder;
import net.runelite.client.chat.ChatMessageManager;
import net.runelite.client.chat.QueuedMessage;
import net.runelite.client.config.ConfigManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Tells the player, once each, about the options of Inventory Setups and of this plugin that would do more with
 * a setup they have just opened.
 */
@Singleton
public class SetupTips
{
	private static final Logger log = LoggerFactory.getLogger(SetupTips.class);

	// Each tip that has been given is saved under this and its name, so that it's given once per profile
	static final String KEY_SHOWN_PREFIX = "tipShown_";

	private static final String[] SPELLBOOKS = {"Standard", "Ancient", "Lunar", "Arceuus"};

	/**
	 * The options of a setup that the tips are about, from its saved json.
	 */
	static final class SetupOptions
	{
		final boolean filterBank;
		final boolean highlightDifferences;
		// as Inventory Setups numbers them, with 4 for none
		final int spellbook;
		final boolean hasNotes;

		SetupOptions(boolean filterBank, boolean highlightDifferences, int spellbook, boolean hasNotes)
		{
			this.filterBank = filterBank;
			this.highlightDifferences = highlightDifferences;
			this.spellbook = spellbook;
			this.hasNotes = hasNotes;
		}

		/**
		 * @return null when the json is missing or can't be read
		 */
		static SetupOptions parse(Gson gson, String json)
		{
			try
			{
				final JsonObject setup = json == null ? null : gson.fromJson(json, JsonObject.class);
				if (setup == null)
				{
					return null;
				}
				// each is left out when it is the default
				return new SetupOptions(
					setup.has("fb") && setup.get("fb").getAsBoolean(),
					setup.has("hd") && setup.get("hd").getAsBoolean(),
					setup.has("sb") ? setup.get("sb").getAsInt() : 0,
					setup.has("notes") && setup.get("notes").isJsonPrimitive() && !setup.get("notes").getAsString().trim().isEmpty());
			}
			catch (RuntimeException e)
			{
				log.debug("Couldn't read a setup's options", e);
				return null;
			}
		}
	}

	/**
	 * What this plugin has turned on that the tips depend on.
	 */
	static final class PickerOptions
	{
		final boolean highlightDifferences;
		final boolean showSpellbook;
		final boolean showNotes;

		PickerOptions(boolean highlightDifferences, boolean showSpellbook, boolean showNotes)
		{
			this.highlightDifferences = highlightDifferences;
			this.showSpellbook = showSpellbook;
			this.showNotes = showNotes;
		}
	}

	/**
	 * The tips, most useful first. Only the first that applies is given when a setup is opened.
	 */
	enum Tip
	{
		FILTER_BANK
			{
				@Override
				boolean applies(SetupOptions setup, PickerOptions picker)
				{
					return !setup.filterBank;
				}

				@Override
				String text(String setupName, SetupOptions setup)
				{
					return setupName + " doesn't filter the bank. Turn on Filter bank for it in Inventory Setups, and opening"
						+ " it shows just its items in the bank.";
				}
			},
		HIGHLIGHT_DIFFERENCES
			{
				@Override
				boolean applies(SetupOptions setup, PickerOptions picker)
				{
					return picker.highlightDifferences && !setup.highlightDifferences;
				}

				@Override
				String text(String setupName, SetupOptions setup)
				{
					return "Turn on Highlight differences for " + setupName + " in Inventory Setups, and the items you"
						+ " haven't got yet are outlined in the bank.";
				}
			},
		SHOW_SPELLBOOK
			{
				@Override
				boolean applies(SetupOptions setup, PickerOptions picker)
				{
					// the standard spellbook is what most setups are left on without anyone choosing it
					return !picker.showSpellbook && setup.spellbook > 0 && setup.spellbook < SPELLBOOKS.length;
				}

				@Override
				String text(String setupName, SetupOptions setup)
				{
					return setupName + " is for the " + SPELLBOOKS[setup.spellbook] + " spellbook. Turn on Show spellbook in"
						+ " Inventory Setups Picker's settings to see in the bank when you're on another.";
				}
			},
		SHOW_NOTES
			{
				@Override
				boolean applies(SetupOptions setup, PickerOptions picker)
				{
					return !picker.showNotes && setup.hasNotes;
				}

				@Override
				String text(String setupName, SetupOptions setup)
				{
					return setupName + " has notes. Turn on Show notes in Inventory Setups Picker's settings to read them"
						+ " beside the list.";
				}
			};

		abstract boolean applies(SetupOptions setup, PickerOptions picker);

		abstract String text(String setupName, SetupOptions setup);

		String key()
		{
			return KEY_SHOWN_PREFIX + name().toLowerCase();
		}
	}

	/**
	 * The tip to give for a setup that has just been opened, or null for none.
	 *
	 * @param shown whether a tip has been given before
	 */
	static Tip choose(SetupOptions setup, PickerOptions picker, Predicate<Tip> shown)
	{
		for (Tip tip : Tip.values())
		{
			if (tip.applies(setup, picker) && !shown.test(tip))
			{
				return tip;
			}
		}
		return null;
	}

	private final Client client;
	private final ConfigManager configManager;
	private final ChatMessageManager chatMessageManager;
	private final SetupPickerConfig config;
	private final Gson gson;

	@Inject
	SetupTips(Client client, ConfigManager configManager, ChatMessageManager chatMessageManager, SetupPickerConfig config, Gson gson)
	{
		this.client = client;
		this.configManager = configManager;
		this.chatMessageManager = chatMessageManager;
		this.config = config;
		this.gson = gson;
	}

	/**
	 * Gives a tip about a setup that has just been opened, if there's one it hasn't had.
	 *
	 * @param json the setup's saved json
	 */
	public void onSetupOpened(String name, String json)
	{
		// the chatbox isn't there to read it in, so keep the tip for another time
		if (!config.showTips() || client.getGameState() != GameState.LOGGED_IN)
		{
			return;
		}
		final SetupOptions setup = SetupOptions.parse(gson, json);
		if (setup == null)
		{
			return;
		}
		final PickerOptions picker = new PickerOptions(config.highlightDifferences(), config.showSpellbook(), config.showNotes());
		final Tip tip = choose(setup, picker, t -> configManager.getConfiguration(SetupPickerConfig.GROUP, t.key()) != null);
		if (tip == null)
		{
			return;
		}
		configManager.setConfiguration(SetupPickerConfig.GROUP, tip.key(), true);
		chatMessageManager.queue(QueuedMessage.builder()
			.type(ChatMessageType.CONSOLE)
			.runeLiteFormattedMessage(new ChatMessageBuilder()
				.append(ChatColorType.HIGHLIGHT).append("Inventory Setups Picker tip: ")
				.append(ChatColorType.NORMAL).append(tip.text(name, setup))
				.build())
			.build());
	}
}
