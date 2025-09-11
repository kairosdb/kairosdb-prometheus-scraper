package org.kairosdb.plugin.prometheus;

import org.kairosdb.core.http.rest.json.RelativeTime;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.kairosdb.util.Preconditions.requireNonNullOrEmpty;

public class Client
{
	private URL m_url;
	private RelativeTime m_interval;
	private Map<String, String> m_tags = new HashMap<>();

	public Client(String url, String interval, Map<String, String> tags) throws MalformedURLException
	{
		requireNonNullOrEmpty(url, "url must not be null or empty");
		requireNonNullOrEmpty(interval, "interval must not be null or empty");
		this.m_url = new URL(url);
		this.m_tags.putAll(tags);

		Pattern regex = Pattern.compile("(\\d+)(.+)");
		Matcher matcher = regex.matcher(interval);
		if (!matcher.matches())
		{
			throw new IllegalArgumentException("Interval is invalid");
		}
		String value = matcher.group(1);
		String unit = matcher.group(2);
		this.m_interval = new RelativeTime(Integer.valueOf(value), Utils.convertShortToLongUnit(unit));
	}

	public URL getUrl()
	{
		return m_url;
	}

	public RelativeTime getInterval()
	{
		return m_interval;
	}

	public Map<String, String> getTags()
	{
		return m_tags;
	}

	@Override
	public boolean equals(Object o)
	{
		if (this == o) return true;
		if (!(o instanceof Client)) return false;
		Client client = (Client) o;
		return Objects.equals(m_url, client.m_url) && Objects.equals(m_interval, client.m_interval) && Objects.equals(m_tags, client.m_tags);
	}

	@Override
	public int hashCode()
	{
		return Objects.hash(m_url, m_interval, m_tags);
	}

	@Override
	public String toString()
	{
		return "Client{" +
				"m_url=" + m_url +
				", m_interval=" + m_interval +
				", m_tags=" + m_tags +
				'}';
	}
}
