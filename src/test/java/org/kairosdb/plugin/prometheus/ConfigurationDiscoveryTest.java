package org.kairosdb.plugin.prometheus;

import org.junit.Test;

import java.net.MalformedURLException;
import java.util.List;
import java.util.Properties;

import static org.hamcrest.Matchers.hasItems;
import static org.junit.Assert.assertThat;

public class ConfigurationDiscoveryTest
{
	@Test
	public void test() throws MalformedURLException
	{
		Client client1 = new Client("http://host1:1234/metrics", "30s");
		Client client2 = new Client("http://host2:1234/metrics", "5h");
		Client client3 = new Client("http://host3:1234/metrics", "20m");

		Properties properties = new Properties();
		properties.put("kairosdb.prometheus-server.client.0.url", "http://host1:1234/metrics");
		properties.put("kairosdb.prometheus-server.client.0.scrape-interval", "30s");
		properties.put("kairosdb.prometheus-server.client.1.url", "http://host2:1234/metrics");
		properties.put("kairosdb.prometheus-server.client.1.scrape-interval", "5h");
		properties.put("kairosdb.prometheus-server.client.2.url", "http://host3:1234/metrics");
		properties.put("kairosdb.prometheus-server.client.2.scrape-interval", "20m");


		ConfigurationDiscovery discovery = new ConfigurationDiscovery(properties);
		List<Client> discoveredClients = discovery.getClients();

		assertThat(discoveredClients, hasItems(client1, client2, client3));
	}
}