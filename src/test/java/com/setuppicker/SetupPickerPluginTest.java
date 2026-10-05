package com.setuppicker;

import net.runelite.client.RuneLite;
import net.runelite.client.externalplugins.ExternalPluginManager;

public class SetupPickerPluginTest
{
	public static void main(String[] args) throws Exception
	{
		ExternalPluginManager.loadBuiltin(SetupPickerPlugin.class);
		RuneLite.main(args);
	}
}
