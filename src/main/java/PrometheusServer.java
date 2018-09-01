import org.hawkular.agent.prometheus.PrometheusScraper;
import org.hawkular.agent.prometheus.types.Counter;
import org.hawkular.agent.prometheus.types.Gauge;
import org.hawkular.agent.prometheus.types.Histogram;
import org.hawkular.agent.prometheus.types.Histogram.Bucket;
import org.hawkular.agent.prometheus.types.Metric;
import org.hawkular.agent.prometheus.types.MetricFamily;
import org.hawkular.agent.prometheus.types.MetricType;
import org.hawkular.agent.prometheus.types.Summary;
import org.hawkular.agent.prometheus.types.Summary.Quantile;
import org.kairosdb.client.HttpClient;
import org.kairosdb.client.builder.MetricBuilder;

import java.io.IOException;
import java.net.InetAddress;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.UnknownHostException;
import java.util.List;
import java.util.Map.Entry;

public class PrometheusServer
{
    // Prometheus metrics - https://prometheus-operator-k8sdev.esp.proofpoint-lab.net/metrics
    private static final String KAIROS_INGEST = "http://localhost:8080";

    private final PrometheusScraper scraper;
    private final String kairosUrl;

    public PrometheusServer(String prometheusUrl, String kairosUrl)
            throws MalformedURLException
    {
        scraper = new PrometheusScraper(new URL(prometheusUrl));
        this.kairosUrl = kairosUrl;
    }

    public void processMetrics()
            throws IOException
    {
        MetricBuilder builder = MetricBuilder.getInstance();

        System.out.println("Scraping...");
        List<MetricFamily> scrape = scraper.scrape();
        int count = 0;
        for (MetricFamily metricFamily : scrape) {
            for (Metric metric : metricFamily.getMetrics()) {
                System.out.println(metric.getName());

                if (metricFamily.getType() == MetricType.SUMMARY) {
                    List<Quantile> quantiles = ((Summary) metric).getQuantiles();
                    for (Quantile quantile : quantiles) {
                        System.out.println("    " + quantile.getValue());
                        addMetricToBuilder(builder, metric, quantile.getValue(), "quantile", Double.toString(quantile.getQuantile()));
                    }
                }
                else if (metricFamily.getType() == MetricType.COUNTER) {
                    System.out.println("    " + ((Counter) metric).getValue());
                    addMetricToBuilder(builder, metric, ((Counter) metric).getValue());
                }
                else if (metricFamily.getType() == MetricType.GAUGE) {
                    System.out.println("    " + ((Gauge) metric).getValue());
                    addMetricToBuilder(builder, metric, ((Gauge) metric).getValue());
                }
                else if (metricFamily.getType() == MetricType.HISTOGRAM) {
                    System.out.println("    " + ((Histogram) metric).getBuckets());
                    for (Bucket bucket : ((Histogram) metric).getBuckets()) {
                        addMetricToBuilder(builder, metric, bucket.getCumulativeCount(), "bucket", Double.toString(bucket.getUpperBound()));
                    }
                }
                count++;
            }
        }
        System.out.println("Number of metrics = " + count);

        try (HttpClient client = new HttpClient(kairosUrl)) {
            client.pushMetrics(builder);
        }
    }

    private void addMetricToBuilder(MetricBuilder builder, Metric metric, double dataPoint)
            throws UnknownHostException
    {
        addMetricToBuilder(builder, metric, dataPoint, null, null);
    }
    private void addMetricToBuilder(MetricBuilder builder, Metric metric, double dataPoint, String tagName, String tagValue)
            throws UnknownHostException
    {
        org.kairosdb.client.builder.Metric newMetric = builder.addMetric(metric.getName());

        newMetric.addTag("host", getHostname()); // todo what host should this really be?
        for (Entry<String, String> tag : metric.getLabels().entrySet()) {
            System.out.println("    " + tag.getKey() + ": " + tag.getValue());
            newMetric.addTag(tag.getKey(), tag.getValue());
        }

        if (tagName != null && tagValue != null)
        {
            newMetric.addTag(tagName, tagValue);
        }

        newMetric.addDataPoint(dataPoint);
    }

    private static String getHostname()
            throws UnknownHostException
    {
        return InetAddress.getLocalHost().getHostName();
    }


    public static void main(String[] args)
            throws IOException, InterruptedException
    {
        // todo How to know when to break-up the metric name to get tags?

//        CloseableHttpClient closeableHttpClient = HttpClientBuilder.create().build();
//
//        HttpGet get = new HttpGet("https://k8sldap.esp.lab.ppops.net/metrics");
//        CloseableHttpResponse response = closeableHttpClient.execute(get);
//
//        InputStream contentStream = response.getEntity().getContent();
//        String metrics = CharStreams.toString(new InputStreamReader(contentStream, Charsets.UTF_8));
//
//        System.out.println(metrics);

//        PrometheusScraper scraper = new PrometheusScraper(new URL("http://localhost:1234/metrics"));


        PrometheusServer server = new PrometheusServer("https://k8sldap.esp.lab.ppops.net/metrics", KAIROS_INGEST);

        while (true) {

            server.processMetrics();
            Thread.sleep(30000);
        }
    }
}
