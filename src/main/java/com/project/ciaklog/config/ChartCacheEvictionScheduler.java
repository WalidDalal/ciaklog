package com.project.ciaklog.config;

import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Caching;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

// Fix: @Cacheable su topFilms/topSeries/topUsers/trending in ChartServiceImpl
// non aveva nessuna scadenza — con il cache manager di default (in memoria,
// no TTL) i dati restavano quelli del primo calcolo finché non si riavviava
// il backend, anche pubblicando nuove recensioni che avrebbero cambiato la
// classifica. Ogni 10 minuti si svuotano tutte e 4 le cache insieme: la
// prossima richiesta le ricalcola fresche.
@Component
public class ChartCacheEvictionScheduler {

    @Caching(evict = {
            @CacheEvict(cacheNames = "topFilms", allEntries = true),
            @CacheEvict(cacheNames = "topSeries", allEntries = true),
            @CacheEvict(cacheNames = "topUsers", allEntries = true),
            @CacheEvict(cacheNames = "trending", allEntries = true),
    })
    @Scheduled(fixedRate = 10 * 60 * 1000) // ogni 10 minuti
    public void evictChartCaches() {
        // nessun corpo necessario — l'eviction avviene via annotazioni
    }
}
