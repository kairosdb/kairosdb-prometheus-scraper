package org.kairosdb.plugin.prometheus;

import org.kairosdb.core.datastore.TimeUnit;

public class Utils
{
	private Utils()
	{
	}

	public static TimeUnit convertShortToLongUnit(String shortUnit)
	{
		switch (shortUnit)
		{
			case "s":
				return TimeUnit.SECONDS;
			case "m":
				return TimeUnit.MINUTES;
			case "h":
				return TimeUnit.HOURS;
			default:
				throw new IllegalArgumentException("Invalid time unit: " + shortUnit);
		}
	}
}
