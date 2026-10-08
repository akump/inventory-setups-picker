package com.setuppicker;

/**
 * Notices a bank tag being opened, by whatever means, from the open tag changing. Bank Tags doesn't announce
 * it the way Inventory Setups does for its setups. Used from the client thread only.
 */
final class RecentTagTracker
{
	private String previous;

	/**
	 * @param active the bank tag that's open now, or "" for none
	 * @return whether it has just been opened
	 */
	boolean observe(String active)
	{
		final String current = active == null ? "" : active;
		final boolean opened = previous != null && !current.isEmpty() && !current.equals(previous);
		previous = current;
		return opened;
	}

	/**
	 * Forget the open tag, so that the next one seen isn't taken for newly opened: it may just be another
	 * profile's, or one restored on login.
	 */
	void reset()
	{
		previous = null;
	}
}
