package org.kairosdb.plugin.prometheus;

import com.google.common.collect.ImmutableList;
import com.google.inject.Inject;
import com.typesafe.config.Config;
import com.typesafe.config.ConfigList;
import com.typesafe.config.ConfigObject;
import com.typesafe.config.ConfigValue;
import org.kairosdb.core.annotation.InjectProperty;

import java.net.MalformedURLException;
import java.util.*;

public class ConfigurationDiscovery implements org.kairosdb.plugin.prometheus.Discovery
{
	public static final String CLIENT_PROPERTY = "kairosdb.prometheus-server.clients";
	private final List<Client> clients = new ArrayList<>();

	@InjectProperty(prop = CLIENT_PROPERTY, optional = false)
	public void setConfiguration(ConfigList clientList) throws MalformedURLException
	{
		for (ConfigValue configValue : clientList)
		{
			//It seems I can pass anything for the path and it will work as the
			//object in the list has no key
			Config config = configValue.atPath("this");
			String url = config.getString("this.url");
			String interval = config.getString("this.scrape-interval");
			Map<String, String> tags = Map.of();
			if (config.hasPath("this.tags"))
				tags = (Map<String, String>) config.getValue("this.tags").unwrapped();

			clients.add(new Client(url, interval, tags));
		}

	}

	public ConfigurationDiscovery()
	{
	}

	@Override
	public List<Client> getClients()
	{
		return ImmutableList.copyOf(clients);
	}
}
