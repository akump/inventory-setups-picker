package com.setuppicker;

import com.google.inject.Binder;
import com.google.inject.Provides;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import javax.inject.Inject;
import net.runelite.api.GameState;
import net.runelite.api.events.ClientTick;
import net.runelite.api.events.GameStateChanged;
import net.runelite.api.events.WidgetClosed;
import net.runelite.api.events.WidgetLoaded;
import net.runelite.api.gameval.InterfaceID;
import net.runelite.client.callback.ClientThread;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.events.ConfigChanged;
import net.runelite.client.events.PluginChanged;
import net.runelite.client.events.PluginMessage;
import net.runelite.client.events.ProfileChanged;
import net.runelite.client.input.KeyManager;
import net.runelite.client.input.MouseManager;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;
import net.runelite.client.plugins.PluginManager;
import net.runelite.client.ui.overlay.OverlayManager;

@PluginDescriptor(
	name = "Inventory Setups Picker",
	description = "Open your Inventory Setups from a hotkey-driven, searchable list in game",
	tags = {"inventory", "setups", "bank", "gear", "picker", "hotkey", "search"}
)
public class SetupPickerPlugin extends Plugin
{
	private static final String INVENTORY_SETUPS_PLUGIN_NAME = "Inventory Setups";

	@Inject
	private ClientThread clientThread;

	@Inject
	private ConfigManager configManager;

	@Inject
	private PluginManager pluginManager;

	@Inject
	private OverlayManager overlayManager;

	@Inject
	private MouseManager mouseManager;

	@Inject
	private KeyManager keyManager;

	@Inject
	private ChatboxKeyGuard chatboxKeyGuard;

	@Inject
	private SetupPickerConfig config;

	@Inject
	private SetupRepository repository;

	@Inject
	private PickerModel model;

	@Inject
	private SetupPickerOverlay overlay;

	@Inject
	private PickerInput input;

	private final AtomicBoolean refreshQueued = new AtomicBoolean();

	@Override
	public void configure(Binder binder)
	{
		binder.bind(PickerModel.class).asEagerSingleton();
	}

	@Provides
	SetupPickerConfig provideConfig(ConfigManager configManager)
	{
		return configManager.getConfig(SetupPickerConfig.class);
	}

	@Override
	protected void startUp()
	{
		overlayManager.add(overlay);
		mouseManager.registerMouseListener(input);
		mouseManager.registerMouseWheelListener(input);
		keyManager.registerKeyListener(input);

		queueRefresh();
		// Inventory Setups only announces the active setup when it changes, so ask once for the current one
		clientThread.invokeLater(() -> model.setActiveSetup(repository.queryActiveSetup()));
	}

	@Override
	protected void shutDown()
	{
		overlayManager.remove(overlay);
		mouseManager.unregisterMouseListener(input);
		mouseManager.unregisterMouseWheelListener(input);
		keyManager.unregisterKeyListener(input);
		clientThread.invoke(chatboxKeyGuard::restore);
		overlay.clearLayout();
		model.resetSearch();
	}

	@Subscribe
	public void onPluginMessage(PluginMessage message)
	{
		if (!SetupRepository.NAMESPACE.equals(message.getNamespace()))
		{
			return;
		}
		if (SetupRepository.MSG_SETUPS_CHANGED.equals(message.getName()))
		{
			queueRefresh();
		}
		else if (SetupRepository.MSG_ACTIVE_SETUP_CHANGED.equals(message.getName()))
		{
			final Object active = message.getData().get(SetupRepository.DATA_ACTIVE_SETUP);
			model.setActiveSetup(active instanceof String ? (String) active : "");
		}
	}

	@Subscribe
	public void onConfigChanged(ConfigChanged event)
	{
		// Favoriting, recoloring or changing a setup's icon rewrites its saved json without a setups-changed message
		final boolean setupEdited = SetupRepository.CONFIG_GROUP.equals(event.getGroup())
			&& event.getKey().startsWith(SetupRepository.CONFIG_KEY_SETUP_PREFIX);
		if (setupEdited || SetupPickerConfig.GROUP.equals(event.getGroup()))
		{
			queueRefresh();
		}
	}

	@Subscribe
	public void onProfileChanged(ProfileChanged event)
	{
		queueRefresh();
	}

	@Subscribe
	public void onPluginChanged(PluginChanged event)
	{
		if (INVENTORY_SETUPS_PLUGIN_NAME.equals(event.getPlugin().getName()))
		{
			queueRefresh();
		}
	}

	@Subscribe
	public void onWidgetLoaded(WidgetLoaded event)
	{
		if (event.getGroupId() == InterfaceID.BANKMAIN)
		{
			queueRefresh();
		}
	}

	@Subscribe
	public void onWidgetClosed(WidgetClosed event)
	{
		// the popup is independent of the bank, so only a search in the list beside it ends here
		if (event.getGroupId() == InterfaceID.BANKMAIN && !model.view().isPaletteOpen())
		{
			model.resetSearch();
		}
	}

	@Subscribe
	public void onClientTick(ClientTick event)
	{
		chatboxKeyGuard.sync(model.view().isSearchFocused());
	}

	@Subscribe
	public void onGameStateChanged(GameStateChanged event)
	{
		// Nothing is drawn outside the game, so don't leave the keyboard captured by an invisible popup
		if (event.getGameState() != GameState.LOGGED_IN)
		{
			model.resetSearch();
		}
	}

	/**
	 * Open the setup, or close it if it's the one already open.
	 */
	void toggleSetup(SetupEntry setup)
	{
		if (setup.getName().equals(model.view().getActiveSetup()))
		{
			clientThread.invoke(() -> repository.close(setup.getName()));
		}
		else
		{
			openSetup(setup);
		}
	}

	void openSetup(SetupEntry setup)
	{
		clientThread.invoke(() -> repository.open(setup.getName()));
	}

	void setCollapsed(boolean collapsed)
	{
		configManager.setConfiguration(SetupPickerConfig.GROUP, SetupPickerConfig.KEY_COLLAPSED, collapsed);
	}

	// Several triggers tend to fire together (e.g. every setup's config key on a profile switch), so
	// collapse them into one reload on the client thread.
	private void queueRefresh()
	{
		if (refreshQueued.compareAndSet(false, true))
		{
			clientThread.invokeLater(() ->
			{
				refreshQueued.set(false);
				final List<SetupEntry> setups = repository.loadSetups(config.alphabetical(), config.favoritesFirst());
				model.setSetups(setups);
				overlay.setStatus(isInventorySetupsEnabled() ? "No setups yet" : "Inventory Setups is off");
			});
		}
	}

	private boolean isInventorySetupsEnabled()
	{
		for (Plugin plugin : pluginManager.getPlugins())
		{
			if (INVENTORY_SETUPS_PLUGIN_NAME.equals(plugin.getName()))
			{
				return pluginManager.isPluginEnabled(plugin);
			}
		}
		return false;
	}
}
