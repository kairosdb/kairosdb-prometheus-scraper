package org.kairosdb.plugin.prometheus;

import com.google.common.collect.ImmutableList;
import com.google.inject.Inject;
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
			Map<String, String> client = (Map)configValue.unwrapped();

			clients.add(
					new Client(client.get("url"), client.get("scrape-interval")));
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
