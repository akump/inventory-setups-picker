package com.setuppicker;

import java.awt.KeyEventDispatcher;
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
import net.runelite.client.input.MouseAdapter;
import net.runelite.client.input.MouseWheelListener;

/**
 * Mouse and keyboard handling for the picker. Events that land on the picker, and all typing while its
 * search box has focus, are consumed so they don't also reach the game.
 *
 * <p>Keys are taken as an AWT {@link KeyEventDispatcher} rather than through RuneLite's KeyManager. KeyManager
 * listeners run in registration order, so plugins registered earlier get the keys first: Key Remapping turns
 * W/A/S/D into arrow keys and eats the typed characters before a later listener ever sees them. The
 * dispatcher runs ahead of all of that, on the untouched events.
 */
@Singleton
public class PickerInput extends MouseAdapter implements KeyEventDispatcher, MouseWheelListener
{
	private final Client client;
	private final SetupPickerPlugin plugin;
	private final SetupPickerConfig config;
	private final SetupPickerOverlay overlay;
	private final PickerModel model;

	// The hotkey's own key press can also produce a typed character, which shouldn't end up in the search box
	private boolean swallowNextTyped;
	private final Set<Integer> swallowedPresses = new HashSet<>();

	@Inject
	PickerInput(Client client, SetupPickerPlugin plugin, SetupPickerConfig config, SetupPickerOverlay overlay, PickerModel model)
	{
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
	public boolean dispatchKeyEvent(KeyEvent e)
	{
		// only keys headed for the game, not e.g. a text field in the side panel
		if (e.getComponent() != client.getCanvas())
		{
			return false;
		}

		final boolean handled;
		switch (e.getID())
		{
			case KeyEvent.KEY_PRESSED:
				handled = keyPressed(e);
				if (handled)
				{
					swallowedPresses.add(e.getKeyCode());
				}
				break;
			case KeyEvent.KEY_TYPED:
				handled = keyTyped(e);
				break;
			case KeyEvent.KEY_RELEASED:
				// Only releases of presses that were swallowed: a key that was already held when the search
				// started (say, a camera key) must still be seen being released by the game
				handled = swallowedPresses.remove(e.getKeyCode());
				break;
			default:
				handled = false;
				break;
		}
		if (handled)
		{
			e.consume();
		}
		return handled;
	}

	private boolean keyPressed(KeyEvent e)
	{
		if (config.hotkey().matches(e))
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
				return false;
			}
			swallowNextTyped = true;
			return true;
		}
		// any other key means the hotkey's typed character, if it had one, has been and gone
		swallowNextTyped = false;

		if (!model.view().isSearchFocused())
		{
			return false;
		}

		switch (e.getKeyCode())
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
				break;
		}
		return true;
	}

	private boolean keyTyped(KeyEvent e)
	{
		if (swallowNextTyped)
		{
			swallowNextTyped = false;
			return true;
		}
		if (!model.view().isSearchFocused())
		{
			return false;
		}
		final char c = e.getKeyChar();
		if (c != KeyEvent.CHAR_UNDEFINED && c >= ' ' && c != KeyEvent.VK_DELETE && !e.isControlDown() && !e.isMetaDown())
		{
			model.typeChar(c);
		}
		return true;
	}
}
