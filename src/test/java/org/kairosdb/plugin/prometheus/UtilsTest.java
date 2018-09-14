package org.kairosdb.plugin.prometheus;

import org.junit.Test;
import org.kairosdb.core.datastore.TimeUnit;

import static org.hamcrest.Matchers.equalTo;
import static org.junit.Assert.assertThat;

public class UtilsTest
{
	@Test
	public void testConvertShortToLongUnit()
	{
		assertThat(Utils.convertShortToLongUnit("s"), equalTo(TimeUnit.SECONDS));
		assertThat(Utils.convertShortToLongUnit("m"), equalTo(TimeUnit.MINUTES));
		assertThat(Utils.convertShortToLongUnit("h"), equalTo(TimeUnit.HOURS));
	}
}