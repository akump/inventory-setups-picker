package com.setuppicker;

import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;
import java.awt.event.MouseWheelEvent;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import javax.inject.Inject;
import javax.inject.Singleton;
import javax.swing.SwingUtilities;
import net.runelite.api.Client;
import net.runelite.api.GameState;
import net.runelite.client.input.KeyListener;
import net.runelite.client.input.MouseAdapter;
import net.runelite.client.input.MouseWheelListener;

/**
 * Mouse and keyboard handling for the picker. Events that land on the picker, and all typing while its
 * search box has focus, are consumed so they don't also reach the game.
 *
 * <p>Key listeners run in the order they were registered, and other plugins' usually come first. Remapping
 * plugins rewrite key presses and consume typed characters before this listener sees them, so it works from
 * the physical key of each press instead. {@link ChatboxKeyGuard} keeps the chatbox out of the way meanwhile.
 */
@Singleton
public class PickerInput extends MouseAdapter implements KeyListener, MouseWheelListener
{
	private static final int MODIFIER_MASK = InputEvent.CTRL_DOWN_MASK | InputEvent.META_DOWN_MASK
		| InputEvent.ALT_DOWN_MASK | InputEvent.SHIFT_DOWN_MASK;

	private final Client client;
	private final SetupPickerPlugin plugin;
	private final SetupPickerConfig config;
	private final SetupPickerOverlay overlay;
	private final PickerModel model;
	private final PlatformModifier platformModifier;

	// The hotkey's own key press can also produce a typed character, which shouldn't end up in the search box
	private boolean swallowNextTyped;
	private final Set<Integer> swallowedPresses = new HashSet<>();

	@Inject
	PickerInput(Client client, SetupPickerPlugin plugin, SetupPickerConfig config, SetupPickerOverlay overlay,
		PickerModel model, PlatformModifier platformModifier)
	{
		this.platformModifier = platformModifier;
		this.client = client;
		this.plugin = plugin;
		this.config = config;
		this.overlay = overlay;
		this.model = model;
	}

	@Override
	public MouseEvent mousePressed(MouseEvent e)
	{
		final PickerLayout layout = overlay.getLayout();
		if (layout == null || !layout.getBounds().contains(e.getPoint()))
		{
			// clicking back into the game ends the search and dismisses the popup
			if (model.view().isSearchFocused())
			{
				model.resetSearch();
			}
			return e;
		}

		if (SwingUtilities.isLeftMouseButton(e))
		{
			if (layout.isPalette())
			{
				clickRow(layout, e);
			}
			else if (layout.getHeader().contains(e.getPoint()))
			{
				model.resetSearch();
				plugin.setCollapsed(!layout.isCollapsed());
			}
			else if (layout.getSearch() != null && layout.getSearch().contains(e.getPoint()))
			{
				model.setSearchFocused(true);
			}
			else
			{
				clickRow(layout, e);
			}
		}
		e.consume();
		return e;
	}

	private void clickRow(PickerLayout layout, MouseEvent e)
	{
		final int row = layout.rowAt(e.getPoint());
		final PickerModel.View view = model.view();
		final List<SetupEntry> setups = view.getSetups();
		if (row >= 0 && view.getScroll() + row < setups.size())
		{
			plugin.toggleSetup(setups.get(view.getScroll() + row));
			model.resetSearch();
		}
	}

	@Override
	public MouseWheelEvent mouseWheelMoved(MouseWheelEvent e)
	{
		final PickerLayout layout = overlay.getLayout();
		if (layout == null || !layout.getBounds().contains(e.getPoint()))
		{
			return e;
		}
		model.scrollBy(e.getWheelRotation());
		e.consume();
		return e;
	}

	@Override
	public void keyPressed(KeyEvent e)
	{
		final int code = keyCode(e);
		if (isHotkey(e, code))
		{
			if (model.view().isPaletteOpen())
			{
				model.resetSearch();
			}
			else if (client.getGameState() == GameState.LOGGED_IN)
			{
				model.openPalette();
			}
			else
			{
				return;
			}
			swallowNextTyped = true;
			swallow(e, code);
			return;
		}
		// any other key means the hotkey's typed character, if it had one, has been and gone
		swallowNextTyped = false;

		if (!model.view().isSearchFocused())
		{
			return;
		}

		switch (code)
		{
			case KeyEvent.VK_ESCAPE:
				model.resetSearch();
				break;
			case KeyEvent.VK_ENTER:
				final SetupEntry setup = model.getSelectedSetup();
				if (setup != null)
				{
					plugin.toggleSetup(setup);
				}
				model.resetSearch();
				break;
			case KeyEvent.VK_UP:
				model.moveSelection(-1);
				break;
			case KeyEvent.VK_DOWN:
				model.moveSelection(1);
				break;
			case KeyEvent.VK_TAB:
				model.moveSelection(e.isShiftDown() ? -1 : 1);
				break;
			case KeyEvent.VK_PAGE_UP:
				model.pageSelection(-1);
				break;
			case KeyEvent.VK_PAGE_DOWN:
				model.pageSelection(1);
				break;
			case KeyEvent.VK_BACK_SPACE:
				model.backspace();
				break;
			default:
				final char c = typedChar(e, code);
				if (c != 0)
				{
					model.typeChar(c);
				}
				break;
		}
		swallow(e, code);
	}

	@Override
	public void keyTyped(KeyEvent e)
	{
		// The search text is built from key presses (see typedChar), so typed events only need keeping from the game
		if (swallowNextTyped || model.view().isSearchFocused())
		{
			swallowNextTyped = false;
			e.consume();
		}
	}

	@Override
	public void keyReleased(KeyEvent e)
	{
		// Only releases of presses that were swallowed: a key that was already held when the search started
		// (say, a camera key) must still be seen being released by the game
		if (swallowedPresses.remove(keyCode(e)))
		{
			e.consume();
		}
	}

	private void swallow(KeyEvent e, int code)
	{
		swallowedPresses.add(code);
		e.consume();
	}

	/**
	 * The key that was physically pressed. Listeners registered before this one may have rewritten the event's
	 * key code (Key Remapping turns W into Up, for one), but they leave the extended key code alone, which is
	 * also what RuneLite's own keybinds match on.
	 */
	private static int keyCode(KeyEvent e)
	{
		final int extended = e.getExtendedKeyCode();
		return extended != KeyEvent.VK_UNDEFINED ? extended : e.getKeyCode();
	}

	/**
	 * The character a key press types, or 0 for none. Taken from the press rather than from typed events,
	 * because remapping plugins consume those, and blank the character of the presses they rewrite.
	 */
	private static char typedChar(KeyEvent e, int code)
	{
		if (e.isControlDown() || e.isMetaDown())
		{
			return 0;
		}
		final char c = e.getKeyChar();
		if (c != KeyEvent.CHAR_UNDEFINED)
		{
			return c >= ' ' && c != KeyEvent.VK_DELETE ? c : 0;
		}
		// a rewritten press: work the character out from the key
		if (code >= KeyEvent.VK_A && code <= KeyEvent.VK_Z)
		{
			return (char) ((e.isShiftDown() ? 'A' : 'a') + code - KeyEvent.VK_A);
		}
		if (code >= KeyEvent.VK_0 && code <= KeyEvent.VK_9)
		{
			return (char) ('0' + code - KeyEvent.VK_0);
		}
		if (code >= KeyEvent.VK_NUMPAD0 && code <= KeyEvent.VK_NUMPAD9)
		{
			return (char) ('0' + code - KeyEvent.VK_NUMPAD0);
		}
		switch (code)
		{
			case KeyEvent.VK_SPACE:
				return ' ';
			case KeyEvent.VK_MINUS:
				return '-';
			case KeyEvent.VK_EQUALS:
				return '=';
			default:
				return 0;
		}
	}

	private boolean isHotkey(KeyEvent e, int code)
	{
		if (code != config.openKey().getKeyCode() || code == KeyEvent.VK_UNDEFINED)
		{
			return false;
		}
		final int wanted = config.requireModifier() ? platformModifier.get() : 0;
		if ((e.getModifiersEx() & MODIFIER_MASK) != wanted)
		{
			return false;
		}
		// Without a modifier the hotkey is a plain key, which has to stay typeable in the search box
		return wanted != 0 || !model.view().isSearchFocused();
	}
}
