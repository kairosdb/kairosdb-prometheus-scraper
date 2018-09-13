package org.kairosdb.plugin.prometheus;

import com.google.common.util.concurrent.ThreadFactoryBuilder;
import com.google.inject.AbstractModule;
import com.google.inject.Provides;
import com.google.inject.Singleton;

import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledThreadPoolExecutor;

public class PrometheusServerModule extends AbstractModule
{
	@Override
	protected void configure()
	{
		bind(Discovery.class).to(org.kairosdb.plugin.prometheus.ConfigurationDiscovery.class).in(Singleton.class);
		bind(PrometheusServer.class).in(Singleton.class);
	}

	@Provides
	public ScheduledExecutorService getExecutorService()
	{
		// todo make number of thread configurable
		return new ScheduledThreadPoolExecutor(10, new ThreadFactoryBuilder().setNameFormat("Prometheus-Scaper-%s").build());
	}
}
