package org.kairosdb.plugin.prometheus;

import com.google.common.collect.ImmutableSortedMap;
import com.google.inject.Inject;
import org.hawkular.agent.prometheus.PrometheusScraper;
import org.hawkular.agent.prometheus.types.*;
import org.hawkular.agent.prometheus.types.Histogram.Bucket;
import org.hawkular.agent.prometheus.types.Summary.Quantile;
import org.kairosdb.core.KairosDBService;
import org.kairosdb.core.datapoints.DoubleDataPoint;
import org.kairosdb.core.http.rest.json.RelativeTime;
import org.kairosdb.eventbus.FilterEventBus;
import org.kairosdb.eventbus.Publisher;
import org.kairosdb.events.DataPointEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.InetAddress;
import java.net.URL;
import java.net.UnknownHostException;
import java.util.List;
import java.util.Map.Entry;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import static com.google.common.base.Preconditions.checkNotNull;

@SuppressWarnings("unused")
public class PrometheusServer implements KairosDBService
{
	private static final Logger logger = LoggerFactory.getLogger(PrometheusServer.class);

	private final org.kairosdb.plugin.prometheus.Discovery discovery;
	private final ScheduledExecutorService executor;
	private final Publisher<DataPointEvent> dataPointPublisher;
	private String m_prefix = "";


	public void setPrefix(String prefix)
	{
		m_prefix = prefix;
	}

	@Inject
	public PrometheusServer(FilterEventBus eventBus, org.kairosdb.plugin.prometheus.Discovery discovery, ScheduledExecutorService executor)
	{
		checkNotNull(eventBus, "eventBus must not be null");
		this.discovery = checkNotNull(discovery, "discovery must not be null");
		this.executor = checkNotNull(executor, "executor must not be null");
		this.dataPointPublisher = eventBus.createPublisher(DataPointEvent.class);
	}

	@Override
	public void start()
	{
		logger.info("Starting prometheus discovery");
		for (Client client : discovery.getClients())
		{
			RelativeTime interval = client.getInterval();
			logger.info("Prometheus discovery: Scheduling client " + client.getUrl());
			executor.scheduleWithFixedDelay(
					new ProcessMetrics(client.getUrl()),
					interval.getValue(),
					interval.getValue(),
					TimeUnit.valueOf(interval.getUnit().name()));
		}
	}

	@Override
	public void stop()
	{
	}

	private class ProcessMetrics implements Runnable
	{
		private PrometheusScraper scraper;
		private URL url;

		ProcessMetrics(URL url)
		{
			this.url = url;
			scraper = new PrometheusScraper(url);
		}

		@Override
		public void run()
		{
			List<MetricFamily> scrape;
			try
			{
				scrape = scraper.scrape();

				for (MetricFamily metricFamily : scrape)
				{
					for (Metric metric : metricFamily.getMetrics())
					{
						if (metricFamily.getType() == MetricType.SUMMARY)
						{
							Summary summary = (Summary) metric;
							List<Quantile> quantiles = summary.getQuantiles();
							for (Quantile quantile : quantiles)
							{
								addDataPoint(metric, quantile.getValue(), "quantile", Double.toString(quantile.getQuantile()));
							}
							addDataPoint(metric, metric.getName() + "_count", summary.getSampleCount());
							addDataPoint(metric, metric.getName() + "_sum", summary.getSampleSum());
						}
						else if (metricFamily.getType() == MetricType.COUNTER)
						{
							addDataPoint(metric, ((Counter) metric).getValue());
						}
						else if (metricFamily.getType() == MetricType.GAUGE)
						{
							addDataPoint(metric, ((Gauge) metric).getValue());
						}
						else if (metricFamily.getType() == MetricType.HISTOGRAM)
						{
							for (Bucket bucket : ((Histogram) metric).getBuckets())
							{
								addDataPoint(metric, metric.getName(), bucket.getCumulativeCount(), "le", Double.toString(bucket.getUpperBound()));
							}
							addDataPoint(metric, metric.getName() + "_sum", ((Histogram) metric).getSampleSum());
							addDataPoint(metric, metric.getName() + "_count", ((Histogram) metric).getSampleCount());
						}
					}
				}
			}
			catch (IOException e)
			{
				logger.error("Failed to scrape " + url, e);
			}
		}
	}

	private void addDataPoint(Metric metric, double dataPoint)
			throws UnknownHostException
	{
		addDataPoint(metric, dataPoint, null, null);
	}

	private void addDataPoint(Metric metric, String metricName, double dataPoint)
			throws UnknownHostException
	{
		addDataPoint(metric, metricName, dataPoint, null, null);
	}

	private void addDataPoint(Metric metric, double dataPoint, String tagName, String tagValue)
			throws UnknownHostException
	{
		addDataPoint(metric, null, dataPoint, tagName, tagValue);
	}

	private void addDataPoint(Metric metric, String metricName, double dataPoint, String tagName, String tagValue)
			throws UnknownHostException
	{
		metricName = metricName != null ? metricName : metric.getName();
		ImmutableSortedMap.Builder<String, String> builder = ImmutableSortedMap.naturalOrder();

		builder.put("host", getHostname()); // todo what host should this really be? Get from url?
		for (Entry<String, String> tag : metric.getLabels().entrySet())
		{
			builder.put(tag.getKey(), tag.getValue());
		}

		if (tagName != null && tagValue != null)
		{
			builder.put(tagName, tagValue);
		}

		long timestamp = System.currentTimeMillis(); // todo how to get prometheus timestamp
		dataPointPublisher.post(new DataPointEvent(m_prefix + metricName, builder.build(), new DoubleDataPoint(timestamp, dataPoint)));
	}

	private static String getHostname()
			throws UnknownHostException
	{
		return InetAddress.getLocalHost().getHostName();
	}
}
