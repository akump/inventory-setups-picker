package com.setuppicker;

import java.awt.event.InputEvent;
import javax.inject.Inject;
import javax.inject.Singleton;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.util.OSType;

/**
 * The "primary" shortcut modifier of the platform being played on: Cmd on a Mac, Ctrl elsewhere.
 *
 * <p>The Platform Keys plugin has a setting to force one platform's keys regardless of the computer. When that
 * plugin hasn't been switched off its choice is followed, so this plugin's shortcut uses the same modifier as its bank search.
 */
@Singleton
public class PlatformModifier
{
	// Platform Keys' config group, its profile setting, and the values that setting takes (its KeyProfile enum)
	private static final String PLATFORM_KEYS_GROUP = "platformkeys";
	private static final String PLATFORM_KEYS_PROFILE = "profile";
	private static final String PROFILE_MAC = "MAC";
	private static final String PROFILE_WINDOWS = "WINDOWS";
	// Where RuneLite records whether the Platform Keys plugin is switched on
	private static final String RUNELITE_GROUP = "runelite";
	private static final String PLATFORM_KEYS_ENABLED = "platformkeysplugin";

	private final ConfigManager configManager;

	@Inject
	PlatformModifier(ConfigManager configManager)
	{
		this.configManager = configManager;
	}

	/**
	 * @return {@link InputEvent#META_DOWN_MASK} or {@link InputEvent#CTRL_DOWN_MASK}
	 */
	public int get()
	{
		// plugins count as on until they have been switched off, so there may be no value at all
		final boolean platformKeysEnabled = !"false".equals(configManager.getConfiguration(RUNELITE_GROUP, PLATFORM_KEYS_ENABLED));
		return resolve(platformKeysEnabled ? configManager.getConfiguration(PLATFORM_KEYS_GROUP, PLATFORM_KEYS_PROFILE) : null,
			OSType.getOSType() == OSType.MacOS);
	}

	/**
	 * @param platformKeysProfile Platform Keys' profile setting, or null when that plugin isn't in use
	 */
	static int resolve(String platformKeysProfile, boolean onMac)
	{
		final boolean mac;
		if (PROFILE_MAC.equals(platformKeysProfile))
		{
			mac = true;
		}
		else if (PROFILE_WINDOWS.equals(platformKeysProfile))
		{
			mac = false;
		}
		else
		{
			// no override, or Platform Keys' own auto-detect
			mac = onMac;
		}
		return mac ? InputEvent.META_DOWN_MASK : InputEvent.CTRL_DOWN_MASK;
	}
}
