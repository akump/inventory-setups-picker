package com.setuppicker;

import java.awt.Canvas;
import java.awt.Component;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.List;
import net.runelite.api.Client;
import net.runelite.api.GameState;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import org.junit.Before;
import org.junit.Test;

public class PickerInputTest
{
	private final Canvas canvas = new Canvas();
	private int platformModifier = InputEvent.CTRL_DOWN_MASK;
	private boolean requireModifier = true;
	private PickerModel model;
	private PickerInput input;

	@Before
	public void setUp()
	{
		final Client client = (Client) Proxy.newProxyInstance(getClass().getClassLoader(), new Class<?>[]{Client.class},
			(proxy, method, args) ->
			{
				switch (method.getName())
				{
					case "getCanvas":
						return canvas;
					case "getGameState":
						return GameState.LOGGED_IN;
					default:
						throw new UnsupportedOperationException(method.getName());
				}
			});
		final SetupPickerConfig config = new SetupPickerConfig()
		{
			@Override
			public boolean requireModifier()
			{
				return requireModifier;
			}
		};
		model = new PickerModel();
		final List<SetupEntry> setups = new ArrayList<>();
		for (String name : new String[]{"Vorkath", "Zulrah", "Wintertodt", "Slayer", "Barrows"})
		{
			setups.add(new SetupEntry(name, false, null, 1));
		}
		model.setSetups(setups);
		model.setVisibleRows(5);
		input = new PickerInput(client, null, config, new SetupPickerOverlay(client, config, model, null), model,
			new PlatformModifier(null)
			{
				@Override
				public int get()
				{
					return platformModifier;
				}
			});
	}

	private boolean send(Component target, int id, int modifiers, int keyCode, char keyChar)
	{
		// Real events carry the extended key code, which is what Keybind matches on; hand-made ones don't
		return input.dispatchKeyEvent(new KeyEvent(target, id, 0, modifiers, keyCode, keyChar)
		{
			@Override
			public int getExtendedKeyCode()
			{
				return getKeyCode();
			}
		});
	}

	// a full press, type, release of a character key, as AWT delivers it
	private void tap(int keyCode, char c)
	{
		assertTrue(send(canvas, KeyEvent.KEY_PRESSED, 0, keyCode, c));
		assertTrue(send(canvas, KeyEvent.KEY_TYPED, 0, KeyEvent.VK_UNDEFINED, c));
		assertTrue(send(canvas, KeyEvent.KEY_RELEASED, 0, keyCode, c));
	}

	private void openWithHotkey()
	{
		assertTrue(send(canvas, KeyEvent.KEY_PRESSED, InputEvent.CTRL_DOWN_MASK, KeyEvent.VK_K, (char) 0x0b));
		assertTrue(send(canvas, KeyEvent.KEY_TYPED, InputEvent.CTRL_DOWN_MASK, KeyEvent.VK_UNDEFINED, (char) 0x0b));
		assertTrue(send(canvas, KeyEvent.KEY_RELEASED, InputEvent.CTRL_DOWN_MASK, KeyEvent.VK_K, (char) 0x0b));
		assertTrue(model.view().isPaletteOpen());
	}

	@Test
	public void keysPassThroughWhileClosed()
	{
		assertFalse(send(canvas, KeyEvent.KEY_PRESSED, 0, KeyEvent.VK_W, 'w'));
		assertFalse(send(canvas, KeyEvent.KEY_TYPED, 0, KeyEvent.VK_UNDEFINED, 'w'));
		assertFalse(send(canvas, KeyEvent.KEY_RELEASED, 0, KeyEvent.VK_W, 'w'));
		assertFalse(send(canvas, KeyEvent.KEY_PRESSED, 0, KeyEvent.VK_ENTER, '\n'));
	}

	@Test
	public void lettersUsedForCameraRemappingAreTypedNotNavigated()
	{
		openWithHotkey();
		tap(KeyEvent.VK_S, 's');
		assertEquals("s", model.view().getQuery());
		assertEquals(0, model.view().getSelected());

		tap(KeyEvent.VK_W, 'w');
		assertEquals("sw", model.view().getQuery());
		tap(KeyEvent.VK_BACK_SPACE, '\b');
		assertEquals("s", model.view().getQuery());
		assertEquals(2, model.view().getSetups().size());
	}

	@Test
	public void arrowsNavigateAndEscapeCloses()
	{
		openWithHotkey();
		assertTrue(send(canvas, KeyEvent.KEY_PRESSED, 0, KeyEvent.VK_DOWN, KeyEvent.CHAR_UNDEFINED));
		assertTrue(send(canvas, KeyEvent.KEY_PRESSED, 0, KeyEvent.VK_DOWN, KeyEvent.CHAR_UNDEFINED));
		assertTrue(send(canvas, KeyEvent.KEY_PRESSED, 0, KeyEvent.VK_UP, KeyEvent.CHAR_UNDEFINED));
		assertEquals("Zulrah", model.getSelectedSetup().getName());

		assertTrue(send(canvas, KeyEvent.KEY_PRESSED, 0, KeyEvent.VK_ESCAPE, (char) 0x1b));
		assertFalse(model.view().isPaletteOpen());
		// the release of the key that closed it doesn't leak to the game either
		assertTrue(send(canvas, KeyEvent.KEY_RELEASED, 0, KeyEvent.VK_ESCAPE, (char) 0x1b));
		assertFalse(send(canvas, KeyEvent.KEY_PRESSED, 0, KeyEvent.VK_DOWN, KeyEvent.CHAR_UNDEFINED));
	}

	@Test
	public void hotkeyTogglesAndItsCharacterIsNotTyped()
	{
		openWithHotkey();
		assertEquals("", model.view().getQuery());
		assertTrue(send(canvas, KeyEvent.KEY_PRESSED, InputEvent.CTRL_DOWN_MASK, KeyEvent.VK_K, (char) 0x0b));
		assertFalse(model.view().isPaletteOpen());
	}

	@Test
	public void keyHeldBeforeOpeningIsStillReleasedToTheGame()
	{
		assertFalse(send(canvas, KeyEvent.KEY_PRESSED, 0, KeyEvent.VK_W, 'w'));
		openWithHotkey();
		assertFalse(send(canvas, KeyEvent.KEY_RELEASED, 0, KeyEvent.VK_W, 'w'));
	}

	@Test
	public void keysForOtherComponentsAreLeftAlone()
	{
		openWithHotkey();
		final Canvas sidePanelField = new Canvas();
		assertFalse(send(sidePanelField, KeyEvent.KEY_PRESSED, 0, KeyEvent.VK_S, 's'));
		assertFalse(send(sidePanelField, KeyEvent.KEY_TYPED, 0, KeyEvent.VK_UNDEFINED, 's'));
		assertEquals("", model.view().getQuery());
	}

	@Test
	public void hotkeyUsesThePlatformModifier()
	{
		platformModifier = InputEvent.META_DOWN_MASK;
		// Ctrl+K is not the shortcut on a Mac, and neither is K alone or Cmd+Shift+K
		assertFalse(send(canvas, KeyEvent.KEY_PRESSED, InputEvent.CTRL_DOWN_MASK, KeyEvent.VK_K, (char) 0x0b));
		assertFalse(send(canvas, KeyEvent.KEY_PRESSED, 0, KeyEvent.VK_K, 'k'));
		assertFalse(send(canvas, KeyEvent.KEY_PRESSED, InputEvent.META_DOWN_MASK | InputEvent.SHIFT_DOWN_MASK, KeyEvent.VK_K, 'K'));
		assertFalse(model.view().isPaletteOpen());

		assertTrue(send(canvas, KeyEvent.KEY_PRESSED, InputEvent.META_DOWN_MASK, KeyEvent.VK_K, 'k'));
		assertTrue(model.view().isPaletteOpen());
		// Cmd+K leaves a typed 'k' on some systems; it isn't put in the search box
		assertTrue(send(canvas, KeyEvent.KEY_TYPED, InputEvent.META_DOWN_MASK, KeyEvent.VK_UNDEFINED, 'k'));
		assertEquals("", model.view().getQuery());

		assertTrue(send(canvas, KeyEvent.KEY_PRESSED, InputEvent.META_DOWN_MASK, KeyEvent.VK_K, 'k'));
		assertFalse(model.view().isPaletteOpen());
	}

	@Test
	public void hotkeyWithoutModifierStaysTypeable()
	{
		requireModifier = false;
		assertFalse(send(canvas, KeyEvent.KEY_PRESSED, InputEvent.CTRL_DOWN_MASK, KeyEvent.VK_K, (char) 0x0b));
		assertTrue(send(canvas, KeyEvent.KEY_PRESSED, 0, KeyEvent.VK_K, 'k'));
		assertTrue(send(canvas, KeyEvent.KEY_TYPED, 0, KeyEvent.VK_UNDEFINED, 'k'));
		assertTrue(model.view().isPaletteOpen());
		assertEquals("", model.view().getQuery());

		// now it's a letter like any other
		tap(KeyEvent.VK_K, 'k');
		assertTrue(model.view().isPaletteOpen());
		assertEquals("k", model.view().getQuery());
	}

	@Test
	public void platformKeysProfileOverridesTheComputer()
	{
		assertEquals(InputEvent.META_DOWN_MASK, PlatformModifier.resolve(null, true));
		assertEquals(InputEvent.CTRL_DOWN_MASK, PlatformModifier.resolve(null, false));
		assertEquals(InputEvent.META_DOWN_MASK, PlatformModifier.resolve("AUTO", true));
		assertEquals(InputEvent.CTRL_DOWN_MASK, PlatformModifier.resolve("WINDOWS", true));
		assertEquals(InputEvent.META_DOWN_MASK, PlatformModifier.resolve("MAC", false));
	}
}
