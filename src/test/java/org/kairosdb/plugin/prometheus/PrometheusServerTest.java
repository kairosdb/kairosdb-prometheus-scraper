package org.kairosdb.plugin.prometheus;

import com.google.common.collect.ImmutableSortedMap;
import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;
import org.junit.Before;
import org.junit.Test;
import org.kairosdb.core.datapoints.DoubleDataPoint;
import org.kairosdb.eventbus.FilterEventBus;
import org.kairosdb.eventbus.Publisher;
import org.kairosdb.events.DataPointEvent;
import org.mockito.ArgumentMatcher;
import org.mockito.Mock;

import java.net.InetAddress;
import java.net.MalformedURLException;
import java.net.UnknownHostException;
import java.util.Properties;

import static org.kairosdb.plugin.prometheus.ConfigurationDiscovery.CLIENT_PROPERTY;
import static org.mockito.Mockito.*;
import static org.mockito.MockitoAnnotations.initMocks;

public class PrometheusServerTest
{
	@Mock
	private FilterEventBus mockEventBus;

	@Mock
	private Publisher<DataPointEvent> mockPublisher;

	@Before
	public void setup()
	{
		initMocks(this);
		when(mockEventBus.createPublisher(DataPointEvent.class)).thenReturn(mockPublisher);
	}

	@Test
	public void test() throws MalformedURLException, UnknownHostException
	{
		String cwd = System.getProperty("user.dir");
		Properties properties = new Properties();
		properties.put("kairosdb.prometheus-server.client.0.url", "file:///" + cwd + "/src/test/resources/timeseries.txt");
		properties.put("kairosdb.prometheus-server.client.0.scrape-interval", "30s");

		Config config = ConfigFactory.parseString("{ kairosdb.prometheus-server.clients: [{url: \"file:///" + cwd + "/src/test/resources/timeseries.txt\", scrape-interval: \"30s\"}]}");

		TestScheduledExecutorService executor = new TestScheduledExecutorService();

		ConfigurationDiscovery discovery = new ConfigurationDiscovery();
		discovery.setConfiguration(config.getList(CLIENT_PROPERTY));
		PrometheusServer server = new PrometheusServer(mockEventBus, discovery, executor);

		server.start();

		executor.execute();

		// Quantiles
		verify(mockPublisher).post(argThat(new DataPointEventMatcher(newDataPointEvent(
				"go_gc_duration_seconds",
				ImmutableSortedMap.of("instance", "1.2.3.4", "quantile", "0.0"),
				6.1708E-05))));
		verify(mockPublisher).post(argThat(new DataPointEventMatcher(newDataPointEvent(
				"go_gc_duration_seconds",
				ImmutableSortedMap.of("instance", "1.2.3.4", "quantile", "0.25"),
				8.1063e-05))));
		verify(mockPublisher).post(argThat(new DataPointEventMatcher(newDataPointEvent(
				"go_gc_duration_seconds",
				ImmutableSortedMap.of("instance", "1.2.3.4", "quantile", "0.5"),
				9.5992e-05))));
		verify(mockPublisher).post(argThat(new DataPointEventMatcher(newDataPointEvent(
				"go_gc_duration_seconds",
				ImmutableSortedMap.of("instance", "1.2.3.4", "quantile", "0.75"),
				0.000127407))));
		verify(mockPublisher).post(argThat(new DataPointEventMatcher(newDataPointEvent(
				"go_gc_duration_seconds_sum",
				ImmutableSortedMap.of(),
				10.254398459))));
		verify(mockPublisher).post(argThat(new DataPointEventMatcher(newDataPointEvent(
				"go_gc_duration_seconds_count",
				ImmutableSortedMap.of(),
				52837))));

		// Guage
		verify(mockPublisher).post(argThat(new DataPointEventMatcher(newDataPointEvent(
				"go_goroutines",
				ImmutableSortedMap.of(),
				126))));

		// Counter
		verify(mockPublisher).post(argThat(new DataPointEventMatcher(newDataPointEvent(
				"go_memstats_alloc_bytes_total",
				ImmutableSortedMap.of(),
				1.52575345048e+11))));

		// Histogram
		verify(mockPublisher).post(argThat(new DataPointEventMatcher(newDataPointEvent(
				"http_request_duration_seconds",
				ImmutableSortedMap.of("le", "0.05"),
				24054))));
		verify(mockPublisher).post(argThat(new DataPointEventMatcher(newDataPointEvent(
				"http_request_duration_seconds",
				ImmutableSortedMap.of("le", "0.1"),
				33444))));
		verify(mockPublisher).post(argThat(new DataPointEventMatcher(newDataPointEvent(
				"http_request_duration_seconds",
				ImmutableSortedMap.of("le", "0.2"),
				100392))));
		verify(mockPublisher).post(argThat(new DataPointEventMatcher(newDataPointEvent(
				"http_request_duration_seconds",
				ImmutableSortedMap.of("le", "0.5"),
				129389))));
		verify(mockPublisher).post(argThat(new DataPointEventMatcher(newDataPointEvent(
				"http_request_duration_seconds",
				ImmutableSortedMap.of("le", "1.0"),
				133988))));
		verify(mockPublisher).post(argThat(new DataPointEventMatcher(newDataPointEvent(
				"http_request_duration_seconds",
				ImmutableSortedMap.of("le", "Infinity"),
				144320))));
		verify(mockPublisher).post(argThat(new DataPointEventMatcher(newDataPointEvent(
				"http_request_duration_seconds_sum",
				ImmutableSortedMap.of(),
				53423))));
		verify(mockPublisher).post(argThat(new DataPointEventMatcher(newDataPointEvent(
				"http_request_duration_seconds_count",
				ImmutableSortedMap.of(),
				144320))));
	}

	private DataPointEvent newDataPointEvent(String metricName, ImmutableSortedMap<String, String> tags, double dataPoint) throws UnknownHostException
	{
		ImmutableSortedMap.Builder<String, String> builder = ImmutableSortedMap.naturalOrder();
		builder.put("host", getHostname()).putAll(tags).build();
		return new DataPointEvent(
				metricName,
				builder.build(),
				new DoubleDataPoint(System.currentTimeMillis(), dataPoint));
	}

	private static String getHostname()
			throws UnknownHostException
	{
		return InetAddress.getLocalHost().getHostName();
	}

	private class DataPointEventMatcher implements ArgumentMatcher<DataPointEvent>
	{
		private DataPointEvent event;
		private String errorMessage;

		DataPointEventMatcher(DataPointEvent event)
		{
			this.event = event;
		}

		@Override
		public boolean matches(DataPointEvent dataPointEvent)
		{
			if (!event.getMetricName().equals(dataPointEvent.getMetricName()))
			{
				errorMessage = "Metric names don't match: " + event.getMetricName() + " != " + dataPointEvent.getMetricName();
				return false;
			}
			if (!event.getTags().equals(dataPointEvent.getTags()))
			{
				errorMessage = "Tags don't match: " + event.getTags() + " != " + dataPointEvent.getTags();
				return false;
			}
			if (event.getDataPoint().getDoubleValue() != dataPointEvent.getDataPoint().getDoubleValue())
			{
				errorMessage = "Data points don't match: " + event.getDataPoint().getDoubleValue() + " != " + dataPointEvent.getDataPoint().getDoubleValue();
				return false;
			}
			return true;
		}

		@Override
		public String toString()
		{
			if (errorMessage != null)
			{
				return errorMessage;
			}
			return "";
		}
	}

}