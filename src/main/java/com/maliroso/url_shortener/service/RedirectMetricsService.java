package com.maliroso.url_shortener.service;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.stereotype.Service;

@Service
public class RedirectMetricsService {

    private final Counter totalRedirectsCounter;

    public RedirectMetricsService(MeterRegistry meterRegistry){
        this.totalRedirectsCounter = Counter.builder("shortener_redirect_total")
                .description("Total URL redirects served")
                .register(meterRegistry);
    }

    public void recordRedirect(){
        totalRedirectsCounter.increment();
    }
}
