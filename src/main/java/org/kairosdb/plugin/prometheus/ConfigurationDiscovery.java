package org.kairosdb.plugin.prometheus;

import com.google.common.collect.ImmutableList;
import com.google.inject.Inject;

import java.net.MalformedURLException;
import java.util.*;

public class ConfigurationDiscovery implements org.kairosdb.plugin.prometheus.Discovery
{
	private static final String CLIENT_PROPERTY = "kairosdb.prometheus-server.client.";
	private final List<Client> clients = new ArrayList<>();

	@Inject
	public ConfigurationDiscovery(Properties properties) throws MalformedURLException
	{
		Set<String> clientNames = new HashSet<>();
		for (String property : properties.stringPropertyNames())
		{
			if (property.startsWith(CLIENT_PROPERTY))
			{
				clientNames.add(property.substring(0, property.lastIndexOf(".")));

			}
		}

		for (String clientName : clientNames)
		{
			clients.add(
					new Client(
							properties.getProperty(clientName + ".url"),
							properties.getProperty(clientName + ".scrape-interval")));
		}
	}

	@Override
	public List<Client> getClients()
	{
		return ImmutableList.copyOf(clients);
	}
}
