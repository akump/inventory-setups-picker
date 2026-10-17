package com.setuppicker;

import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import net.runelite.client.config.Keybind;
import static org.junit.Assert.assertEquals;
import org.junit.Test;

public class WelcomeTest
{
	@Test
	public void theWelcomeNamesTheHotkeyAsItIsSet()
	{
		final Keybind k = new Keybind(KeyEvent.VK_K, 0);
		assertEquals("Inventory Setups Picker: press Ctrl+K to open any setup by typing its name, or use the list"
			+ " beside the bank.", SetupPickerPlugin.welcome(k, true, InputEvent.CTRL_DOWN_MASK));
		assertEquals(true, SetupPickerPlugin.welcome(k, true, InputEvent.META_DOWN_MASK).contains("press Cmd+K "));
		assertEquals(true, SetupPickerPlugin.welcome(k, false, InputEvent.META_DOWN_MASK).contains("press K "));
		assertEquals(true, SetupPickerPlugin.welcome(Keybind.NOT_SET, true, InputEvent.CTRL_DOWN_MASK)
			.contains("press the hotkey in its settings "));
	}
}
