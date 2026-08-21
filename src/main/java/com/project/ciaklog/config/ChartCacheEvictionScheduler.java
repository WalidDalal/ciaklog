package com.project.ciaklog.config;

import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Caching;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

// @Cacheable su topFilms/topSeries/topUsers/trending in ChartServiceImpl
// non ha TTL di default — questo scheduler invalida la cache ogni 15s,
// bilanciando freschezza e carico (ricalcolare su ogni scrittura sarebbe
// uno spreco con tanti utenti). Se serve immediatezza vera, la soluzione
// corretta è invalidazione event-driven, non un intervallo più corto.
@Component
public class ChartCacheEvictionScheduler {

    @Caching(evict = {
            @CacheEvict(cacheNames = "topFilms", allEntries = true),
            @CacheEvict(cacheNames = "topSeries", allEntries = true),
            @CacheEvict(cacheNames = "topUsers", allEntries = true),
            @CacheEvict(cacheNames = "trending", allEntries = true),
    })
    @Scheduled(fixedRate = 15 * 1000) // ogni 15 secondi
    public void evictChartCaches() {
        // nessun corpo necessario — l'eviction avviene via annotazioni
    }
}
