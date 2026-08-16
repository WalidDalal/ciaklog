package com.project.ciaklog.dto.response;

import com.project.ciaklog.entity.ContentType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

// Fix (🔴 FIX — "Consiglio del giorno" che sparisce dal secondo giro in poi):
// @Builder da solo genera SOLO un costruttore "tutti i campi" a uso interno
// del builder, e toglie quello vuoto implicito che Java metterebbe di
// default. Jackson (usato in AiServiceImpl.toDailyDTO per rileggere i
// suggerimenti salvati in cache da JSON) ha bisogno di un costruttore vuoto
// per istanziare l'oggetto prima di popolarlo — senza, ogni
// mapper.readValue(...) su questa classe falliva silenziosamente (eccezione
// ingoiata da un catch che ripiegava su lista vuota). La prima chiamata
// "funzionava" perché quella non passa da Jackson: gli oggetti vengono
// costruiti direttamente in Java via builder in resolveTitlesOnTmdb. Solo le
// chiamate successive, che rileggono la cache salvata, richiedevano la
// deserializzazione — da qui il "funziona una volta, poi sparisce".
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TmdbSearchResultResponse {
    private Long tmdbId;
    private String title;
    private ContentType contentType;
    private String posterPath;
    private Integer releaseYear;
    private Double tmdbRating;

    // "spiegazione del perché" — valorizzato solo quando
    // il suggerimento viene dalla chat/daily AI, null nella ricerca TMDB normale.
    // @Setter perché il risultato arriva già costruito da TmdbService e va
    // solo arricchito con il motivo dopo, non ricreato da capo.
    private String reason;
}