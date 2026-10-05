package com.setuppicker;

import javax.inject.Inject;
import javax.inject.Singleton;
import net.runelite.api.Client;
import net.runelite.api.gameval.InterfaceID;
import net.runelite.api.widgets.Widget;

/**
 * Takes the keyboard away from the chatbox while the picker's search box has it.
 *
 * <p>The chatbox reads keys through its key listener. The core Key Remapping plugin only acts while that
 * listener is set (its "chatbox focused" check), so with it removed Key Remapping stands down too: it stops
 * turning W/A/S/D into arrow keys, and Enter no longer unlocks its "Press Enter to Chat" input when all it was
 * meant to do was pick a setup. The listener is put back as soon as the search box loses focus.
 *
 * <p>Client thread only.
 */
@Singleton
public class ChatboxKeyGuard
{
	private final Client client;
	private Object[] savedListener;

	@Inject
	ChatboxKeyGuard(Client client)
	{
		this.client = client;
	}

	/**
	 * @param searchFocused whether the picker currently wants the keyboard
	 */
	public void sync(boolean searchFocused)
	{
		if (searchFocused)
		{
			suspend();
		}
		else
		{
			restore();
		}
	}

	private void suspend()
	{
		final Widget chatbox = client.getWidget(InterfaceID.Chatbox.UNIVERSE);
		if (chatbox == null)
		{
			return;
		}
		// Also re-suspends if the game rebuilt the chatbox, and so set the listener again, while searching
		final Object[] listener = chatbox.getOnKeyListener();
		if (listener != null)
		{
			savedListener = listener;
			chatbox.setOnKeyListener((Object[]) null);
		}
	}

	public void restore()
	{
		if (savedListener == null)
		{
			return;
		}
		final Widget chatbox = client.getWidget(InterfaceID.Chatbox.UNIVERSE);
		// If the game has set a listener in the meantime, that one is current: leave it
		if (chatbox != null && chatbox.getOnKeyListener() == null)
		{
			chatbox.setOnKeyListener(savedListener);
		}
		savedListener = null;
	}
}
