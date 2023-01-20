package org.kairosdb.plugin.prometheus;

import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;
import org.junit.Test;

import java.net.MalformedURLException;
import java.util.List;
import java.util.Properties;

import static org.hamcrest.Matchers.hasItems;
import static org.junit.Assert.assertThat;
import static org.kairosdb.plugin.prometheus.ConfigurationDiscovery.CLIENT_PROPERTY;

public class ConfigurationDiscoveryTest
{
	@Test
	public void test() throws MalformedURLException
	{
		Client client1 = new Client("http://host1:1234/metrics", "30s");
		Client client2 = new Client("http://host2:1234/metrics", "5h");
		Client client3 = new Client("http://host3:1234/metrics", "20m");

		Config config = ConfigFactory.parseString("{ kairosdb.prometheus-server.clients: [{url: \"http://host1:1234/metrics\", scrape-interval: \"30s\"}," +
				" {url: \"http://host2:1234/metrics\", scrape-interval: \"5h\"}," +
				" {url: \"http://host3:1234/metrics\", scrape-interval: \"20m\"}]}");

		ConfigurationDiscovery discovery = new ConfigurationDiscovery();
		discovery.setConfiguration(config.getList(CLIENT_PROPERTY));
		List<Client> discoveredClients = discovery.getClients();

		assertThat(discoveredClients, hasItems(client1, client2, client3));
	}
}