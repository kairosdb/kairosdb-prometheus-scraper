package org.kairosdb.plugin.prometheus;

import com.google.common.collect.ImmutableSortedMap;
import com.google.inject.Inject;
import org.hawkular.agent.prometheus.PrometheusScraper;
import org.hawkular.agent.prometheus.types.*;
import org.hawkular.agent.prometheus.types.Histogram.Bucket;
import org.hawkular.agent.prometheus.types.Summary.Quantile;
import org.kairosdb.core.KairosDBService;
import org.kairosdb.core.annotation.InjectProperty;
import org.kairosdb.core.datapoints.DoubleDataPoint;
import org.kairosdb.core.http.rest.json.RelativeTime;
import org.kairosdb.eventbus.FilterEventBus;
import org.kairosdb.eventbus.Publisher;
import org.kairosdb.events.DataPointEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
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

	private final org.kairosdb.plugin.prometheus.Discovery m_discovery;
	private final ScheduledExecutorService m_executor;
	private final Publisher<DataPointEvent> m_dataPointPublisher;
	private String m_prefix = "";
	private int m_ttl = 0;


	@InjectProperty(prop = "kairosdb.prometheus-server.prefix", optional = true)
	public void setPrefix(String prefix)
	{
		m_prefix = prefix;
	}

	@InjectProperty(prop = "kairosdb.prometheus-server.ttl", optional = true)
	public void setTtl(int ttl)
	{
		m_ttl = ttl;
	}



	@Inject
	public PrometheusServer(FilterEventBus eventBus, org.kairosdb.plugin.prometheus.Discovery discovery, ScheduledExecutorService executor)
	{
		checkNotNull(eventBus, "eventBus must not be null");
		this.m_discovery = checkNotNull(discovery, "discovery must not be null");
		this.m_executor = checkNotNull(executor, "executor must not be null");
		this.m_dataPointPublisher = eventBus.createPublisher(DataPointEvent.class);
	}

	@Override
	public void start()
	{
		logger.info("Starting prometheus discovery");
		for (Client client : m_discovery.getClients())
		{
			RelativeTime interval = client.getInterval();
			logger.info("Prometheus discovery: Scheduling client " + client.getUrl());
			m_executor.scheduleWithFixedDelay(
					new ProcessMetrics(client),
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
		private PrometheusScraper m_scraper;
		private Client m_client;
		private long m_scrapeTime;

		ProcessMetrics(Client client)
		{
			m_client = client;
			m_scraper = new PrometheusScraper(m_client.getUrl());
		}

		@Override
		public void run()
		{
			List<MetricFamily> scrape;
			try
			{
				scrape = m_scraper.scrape();

				m_scrapeTime = System.currentTimeMillis();

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
							addDataPoint(metric, metric.getName() + "_sum", summary.getSampleSum());
							addDataPoint(metric, metric.getName() + "_count", summary.getSampleCount());
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
				logger.error("Failed to scrape " + m_client.getUrl(), e);
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

			builder.putAll(m_client.getTags());
			for (Entry<String, String> tag : metric.getLabels().entrySet())
			{
				builder.put(tag.getKey(), tag.getValue());
			}

			if (tagName != null && tagValue != null)
			{
				builder.put(tagName, tagValue);
			}

			m_dataPointPublisher.post(new DataPointEvent(m_prefix + metricName, builder.build(), new DoubleDataPoint(m_scrapeTime, dataPoint), m_ttl));
		}
	}
}
