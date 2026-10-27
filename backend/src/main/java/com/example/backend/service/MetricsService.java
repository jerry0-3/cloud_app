package com.example.backend.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.services.cloudwatch.CloudWatchClient;
import software.amazon.awssdk.services.cloudwatch.model.*;

import java.time.Instant;

@Service
public class MetricsService {

    private static final Logger log = LoggerFactory.getLogger(MetricsService.class);
    private final CloudWatchClient cloudWatchClient;

    @Value("${app.aws-enabled:false}")
    private boolean awsEnabled;

    public MetricsService(CloudWatchClient cloudWatchClient) {
        this.cloudWatchClient = cloudWatchClient;
    }

    public void recordEndpointLatency(String endpointName, long durationMs) {
        log.info("METRYKA [{}]: czas odpowiedzi = {} ms", endpointName, durationMs);

        if (awsEnabled) {
            try {
                Dimension dimension = Dimension.builder()
                        .name("Endpoint")
                        .value(endpointName)
                        .build();

                MetricDatum datum = MetricDatum.builder()
                        .metricName("ApiLatencyMs")
                        .unit(StandardUnit.MILLISECONDS)
                        .value((double) durationMs)
                        .timestamp(Instant.now())
                        .dimensions(dimension)
                        .build();

                PutMetricDataRequest request = PutMetricDataRequest.builder()
                        .namespace("CloudDemoApp")
                        .metricData(datum)
                        .build();

                cloudWatchClient.putMetricData(request);
            } catch (Exception e) {
                log.error("Błąd wysyłania metryki do CloudWatch: {}", e.getMessage());
            }
        }
    }
}