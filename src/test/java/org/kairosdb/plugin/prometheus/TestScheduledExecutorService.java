package org.kairosdb.plugin.prometheus;

import java.util.Collection;
import java.util.List;
import java.util.concurrent.*;

public class TestScheduledExecutorService implements ScheduledExecutorService
{
	private Runnable runnable;
	@Override
	public ScheduledFuture<?> schedule(Runnable runnable, long l, TimeUnit timeUnit)
	{
		return null;
	}

	@Override
	public <V> ScheduledFuture<V> schedule(Callable<V> callable, long l, TimeUnit timeUnit)
	{
		return null;
	}

	@Override
	public ScheduledFuture<?> scheduleAtFixedRate(Runnable runnable, long l, long l1, TimeUnit timeUnit)
	{
		return null;
	}

	@Override
	public ScheduledFuture<?> scheduleWithFixedDelay(Runnable runnable, long l, long l1, TimeUnit timeUnit)
	{
		this.runnable = runnable;
		return null;
	}

	@Override
	public void shutdown()
	{

	}

	@Override
	public List<Runnable> shutdownNow()
	{
		return null;
	}

	@Override
	public boolean isShutdown()
	{
		return false;
	}

	@Override
	public boolean isTerminated()
	{
		return false;
	}

	@Override
	public boolean awaitTermination(long l, TimeUnit timeUnit) throws InterruptedException
	{
		return false;
	}

	@Override
	public <T> Future<T> submit(Callable<T> callable)
	{
		return null;
	}

	@Override
	public <T> Future<T> submit(Runnable runnable, T t)
	{
		return null;
	}

	@Override
	public Future<?> submit(Runnable runnable)
	{
		return null;
	}

	@Override
	public <T> List<Future<T>> invokeAll(Collection<? extends Callable<T>> collection) throws InterruptedException
	{
		return null;
	}

	@Override
	public <T> List<Future<T>> invokeAll(Collection<? extends Callable<T>> collection, long l, TimeUnit timeUnit) throws InterruptedException
	{
		return null;
	}

	@Override
	public <T> T invokeAny(Collection<? extends Callable<T>> collection) throws InterruptedException, ExecutionException
	{
		return null;
	}

	@Override
	public <T> T invokeAny(Collection<? extends Callable<T>> collection, long l, TimeUnit timeUnit) throws InterruptedException, ExecutionException, TimeoutException
	{
		return null;
	}

	@Override
	public void execute(Runnable runnable)
	{

	}

	public void execute()
	{
		runnable.run();
	}
}
