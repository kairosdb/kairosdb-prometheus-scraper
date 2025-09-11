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
import org.mockito.ArgumentCaptor;
import org.mockito.ArgumentMatcher;
import org.mockito.Mock;

import java.net.InetAddress;
import java.net.MalformedURLException;
import java.net.UnknownHostException;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.Properties;

import static org.assertj.core.api.Assertions.assertThat;
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

		Config config = ConfigFactory.parseString("{ kairosdb.prometheus-server.clients: [{url: \"file:///" + cwd + "/src/test/resources/timeseries.txt\", scrape-interval: \"30s\", tags: {host: test_host}}]}");

		TestScheduledExecutorService executor = new TestScheduledExecutorService();

		ConfigurationDiscovery discovery = new ConfigurationDiscovery();
		discovery.setConfiguration(config.getList(CLIENT_PROPERTY));
		PrometheusServer server = new PrometheusServer(mockEventBus, discovery, executor);

		server.start();

		executor.execute();

		ArgumentCaptor<DataPointEvent> eventCaptor = ArgumentCaptor.forClass(DataPointEvent.class);
		verify(mockPublisher, times(17)).post(eventCaptor.capture());
		DataPointEventComparator comparator = new DataPointEventComparator();

		Iterator<DataPointEvent> eventIterator = eventCaptor.getAllValues().iterator();

		// Quantiles
		assertThat(eventIterator.next()).usingComparator(comparator).isEqualTo(newDataPointEvent(
				"go_gc_duration_seconds",
				ImmutableSortedMap.of("instance", "1.2.3.4", "quantile", "0.0"),
				6.1708E-05));

		assertThat(eventIterator.next()).usingComparator(comparator).isEqualTo(newDataPointEvent(
				"go_gc_duration_seconds",
				ImmutableSortedMap.of("instance", "1.2.3.4", "quantile", "0.25"),
				8.1063e-05));
		assertThat(eventIterator.next()).usingComparator(comparator).isEqualTo(newDataPointEvent(
				"go_gc_duration_seconds",
				ImmutableSortedMap.of("instance", "1.2.3.4", "quantile", "0.5"),
				9.5992e-05));
		assertThat(eventIterator.next()).usingComparator(comparator).isEqualTo(newDataPointEvent(
				"go_gc_duration_seconds",
				ImmutableSortedMap.of("instance", "1.2.3.4", "quantile", "0.75"),
				0.000127407));
		assertThat(eventIterator.next()).usingComparator(comparator).isEqualTo(newDataPointEvent(
				"go_gc_duration_seconds",
				ImmutableSortedMap.of("instance", "1.2.3.4", "quantile", "1.0"),
				0.015248652));
		assertThat(eventIterator.next()).usingComparator(comparator).isEqualTo(newDataPointEvent(
				"go_gc_duration_seconds_sum",
				ImmutableSortedMap.of("instance", "1.2.3.4"),
				10.254398459));
		assertThat(eventIterator.next()).usingComparator(comparator).isEqualTo(newDataPointEvent(
				"go_gc_duration_seconds_count",
				ImmutableSortedMap.of("instance", "1.2.3.4"),
				52837));

		// Guage
		assertThat(eventIterator.next()).usingComparator(comparator).isEqualTo(newDataPointEvent(
				"go_goroutines",
				ImmutableSortedMap.of(),
				126));

		// Counter
		assertThat(eventIterator.next()).usingComparator(comparator).isEqualTo(newDataPointEvent(
				"go_memstats_alloc_bytes_total",
				ImmutableSortedMap.of(),
				1.52575345048e+11));

		// Histogram
		assertThat(eventIterator.next()).usingComparator(comparator).isEqualTo(newDataPointEvent(
				"http_request_duration_seconds",
				ImmutableSortedMap.of("le", "0.05"),
				24054));
		assertThat(eventIterator.next()).usingComparator(comparator).isEqualTo(newDataPointEvent(
				"http_request_duration_seconds",
				ImmutableSortedMap.of("le", "0.1"),
				33444));
		assertThat(eventIterator.next()).usingComparator(comparator).isEqualTo(newDataPointEvent(
				"http_request_duration_seconds",
				ImmutableSortedMap.of("le", "0.2"),
				100392));
		assertThat(eventIterator.next()).usingComparator(comparator).isEqualTo(newDataPointEvent(
				"http_request_duration_seconds",
				ImmutableSortedMap.of("le", "0.5"),
				129389));
		assertThat(eventIterator.next()).usingComparator(comparator).isEqualTo(newDataPointEvent(
				"http_request_duration_seconds",
				ImmutableSortedMap.of("le", "1.0"),
				133988));
		assertThat(eventIterator.next()).usingComparator(comparator).isEqualTo(newDataPointEvent(
				"http_request_duration_seconds",
				ImmutableSortedMap.of("le", "Infinity"),
				144320));
		assertThat(eventIterator.next()).usingComparator(comparator).isEqualTo(newDataPointEvent(
				"http_request_duration_seconds_sum",
				ImmutableSortedMap.of(),
				53423));
		assertThat(eventIterator.next()).usingComparator(comparator).isEqualTo(newDataPointEvent(
				"http_request_duration_seconds_count",
				ImmutableSortedMap.of(),
				144320));
	}

	private DataPointEvent newDataPointEvent(String metricName, ImmutableSortedMap<String, String> tags, double dataPoint) throws UnknownHostException
	{
		ImmutableSortedMap.Builder<String, String> builder = ImmutableSortedMap.naturalOrder();
		builder.put("host", "test_host").putAll(tags).build();
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

	private class DataPointEventComparator implements Comparator<DataPointEvent>
	{

		@Override
		public int compare(DataPointEvent event1, DataPointEvent event2)
		{
			if (!event1.getMetricName().equals(event2.getMetricName()))
			{
				return 1;
			}
			if (!event1.getTags().equals(event2.getTags()))
			{
				return 1;
			}
			if (event1.getDataPoint().getDoubleValue() != event2.getDataPoint().getDoubleValue())
			{
				return 1;
			}
			return 0;
		}
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