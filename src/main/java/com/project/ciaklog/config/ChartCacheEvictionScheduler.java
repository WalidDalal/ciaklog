package com.project.ciaklog.config;

import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Caching;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

// Fix: @Cacheable su topFilms/topSeries/topUsers/trending in ChartServiceImpl
// non aveva nessuna scadenza — con il cache manager di default (in memoria,
// no TTL) i dati restavano quelli del primo calcolo finché non si riavviava
// il backend, anche pubblicando nuove recensioni che avrebbero cambiato la
// classifica.
//
// Fix (Homepage — "aggiornamento troppo lento"): NON è un bug nel senso di
// dato sbagliato, è un compromesso di caching — i 4 grafici sono ricalcolati
// a intervalli fissi, non ad ogni scrittura. Prima erano 2 minuti, troppo
// percepibile; portato a 15 secondi, che nella pratica sembra "quasi
// immediato" senza ricalcolare la classifica ad ogni singola azione (con
// tanti utenti, ricalcolare su OGNI recensione/commento/nascondi sarebbe
// uno spreco). Se in futuro serve vera immediatezza, la soluzione corretta è
// invalidazione event-driven (evict mirato nei service che scrivono review/
// commenti), non un intervallo ancora più corto.
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
