package org.kairosdb.plugin.prometheus;

import org.junit.Test;
import org.kairosdb.core.datastore.TimeUnit;
import org.kairosdb.core.http.rest.json.RelativeTime;

import java.net.MalformedURLException;
import java.net.URL;

import static org.hamcrest.Matchers.equalTo;
import static org.junit.Assert.assertThat;

public class ClientTest
{
	@Test
	public void test_constructor() throws MalformedURLException
	{
		Client client = new Client("http://foo.bar", "10m");

		assertThat(client.getUrl(), equalTo(new URL("http://foo.bar")));
		assertThat(client.getInterval(), equalTo(new RelativeTime(10, TimeUnit.MINUTES)));
	}

	@Test(expected = IllegalArgumentException.class)
	public void test_constructor_invalidInterval() throws MalformedURLException
	{
		Client client = new Client("http://foo.bar", "10u");

		assertThat(client.getInterval(), equalTo(new RelativeTime(10, TimeUnit.MINUTES)));
	}
}