# kairosdb-prometheus-scaper
A KairosDB plugin that acts as a Prometheus server to scrape metrics from Prometheus clients.

This version has not been optimized for a really large number of clients. Currently it uses a separate thread for each client connection. 

##Discovery
Clients that the server scrapes are specified in the configuration file. Each client configuration starts with
 kairosdb.prometheus-server.client.[x] where [x] is a number identifing the client. For example:
 
```
	kairosdb.prometheus-server.client.0.url=http://localhost:1234/metrics
	kairosdb.prometheus-server.client.0.scrape-interval=30s
	kairosdb.prometheus-server.client.1.url=http://localhost:12345/metrics
	kairosdb.prometheus-server.client.1.scrape-interval=10m
    
```
	
	
| Property | Description|
|----------|------------|
|kairosdb.prometheus-server.client.[x].url| The client URL. The URL typically ends with "/metrics"|
 kairosdb.prometheus-server.client.[x].scrape-interval| How often to scrape the client for metrics. The time is specified as a number and a time unit. Time units are s=seconds, m=minutes, h=hours. For example: 30s.|
 
 
 *Tested on KariosDB version 1.2.1*