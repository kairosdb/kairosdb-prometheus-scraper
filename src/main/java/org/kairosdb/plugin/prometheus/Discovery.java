package org.kairosdb.plugin.prometheus;

import java.util.List;

public interface Discovery
{
	List<Client> getClients();
}
