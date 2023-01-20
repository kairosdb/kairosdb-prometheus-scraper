package org.kairosdb.plugin.prometheus;

import org.kairosdb.core.http.rest.json.RelativeTime;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.kairosdb.util.Preconditions.requireNonNullOrEmpty;

public class Client
{
	private URL url;
	private RelativeTime interval;

	public Client(String url, String interval) throws MalformedURLException
	{
		requireNonNullOrEmpty(url, "url must not be null or empty");
		requireNonNullOrEmpty(interval, "interval must not be null or empty");
		this.url = new URL(url);

		Pattern regex = Pattern.compile("(\\d+)(.+)");
		Matcher matcher = regex.matcher(interval);
		if (!matcher.matches())
		{
			throw new IllegalArgumentException("Interval is invalid");
		}
		String value = matcher.group(1);
		String unit = matcher.group(2);
		this.interval = new RelativeTime(Integer.valueOf(value), Utils.convertShortToLongUnit(unit));
	}

	public URL getUrl()
	{
		return url;
	}

	public RelativeTime getInterval()
	{
		return interval;
	}

	@Override
	public boolean equals(Object o)
	{
		if (this == o) return true;
		if (o == null || getClass() != o.getClass()) return false;
		Client client = (Client) o;
		return Objects.equals(url, client.url) &&
				Objects.equals(interval, client.interval);
	}

	@Override
	public int hashCode()
	{
		return Objects.hash(url, interval);
	}

	@Override
	public String toString()
	{
		return com.google.common.base.MoreObjects.toStringHelper(this)
				.add("url", url)
				.add("interval", interval)
				.toString();
	}
}
