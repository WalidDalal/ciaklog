package com.project.ciaklog.dto.response;

import com.project.ciaklog.entity.ReactionType;
import lombok.Builder;
import lombok.Getter;

import java.util.Map;

@Getter
@Builder
public class ReactionSummaryResponse {
    // Conteggio per tipo — solo i tipi con almeno una reazione sono presenti
    private Map<ReactionType, Long> counts;

    // Il tipo scelto dall'utente corrente, null se non ha reagito.
    // null anche per un utente anonimo (nessun token)
    private ReactionType myReaction;
}
