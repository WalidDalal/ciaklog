# 🏗️ CiakLog — Documentazione tecnica

Questo documento approfondisce l'architettura, gli endpoint API, le regole di business e le scelte implementative del progetto. Per una panoramica generale vedi la [README](../README.md).

---

## 🗂️ Struttura del progetto

```
ciaklog/
├── backend/                          # Spring Boot
│   └── src/main/java/com/project/ciaklog/
│       ├── controller/               # REST endpoints
│       ├── service/                  # Business logic
│       │   └── impl/
│       ├── repository/               # JPA repositories
│       ├── entity/                   # JPA entities
│       ├── dto/                      # Request / Response DTOs
│       │   ├── request/
│       │   └── response/
│       ├── security/                 # JWT filter, UserDetails
│       ├── exception/                # GlobalExceptionHandler
│       └── config/                   # Security, Encoder
│
└── frontend/                         # React + Vite
    └── src/
        ├── pages/                    # HomePage, LibraryPage, SearchPage, MovieDetailPage,
        │                             # ChatAiPage, WrappedPage, CreditsPage, ProfilePage,
        │                             # SettingsPage, AdminPage, LoginPage, RegisterPage
        ├── components/               # Navbar, ChatWidget, HeroSection, StaticRating,
        │                             # ConfirmModal, ToastContainer, ErrorBoundary,
        │                             # ProtectedRoute
        ├── store/                    # authStore, chatStore, themeStore, toastStore
        ├── services/                 # api.js (Axios), auth.js
        └── hooks/                    # useApi
```

---

## 📡 API di riferimento

```
POST   /api/auth/register
POST   /api/auth/login

GET    /api/tmdb/search?q={query}&type={MOVIE|TV}
GET    /api/tmdb/{contentType}/{tmdbId}

GET    /api/library
POST   /api/library
PUT    /api/library/{id}
DELETE /api/library/{id}

GET    /api/reviews/media/{contentType}/{tmdbId}
POST   /api/reviews
PUT    /api/reviews/{id}

GET    /api/reviews/{reviewId}/comments
POST   /api/reviews/{reviewId}/comments
PUT    /api/comments/{id}
DELETE /api/comments/{id}

GET    /api/reviews/{reviewId}/reaction
PUT    /api/reviews/{reviewId}/reaction
DELETE /api/reviews/{reviewId}/reaction
GET    /api/comments/{commentId}/reaction
PUT    /api/comments/{commentId}/reaction
DELETE /api/comments/{commentId}/reaction

GET    /api/charts/films
GET    /api/charts/series
GET    /api/charts/users
GET    /api/charts/trending

GET    /api/wrapped

GET    /api/users/{username}
PUT    /api/users/me
DELETE /api/users/me

POST   /api/ai/chat
POST   /api/ai/chat/admin
GET    /api/ai/daily
POST   /api/ai/structure-review
POST   /api/ai/structure-comment
POST   /api/ai/movie-question
POST   /api/ai/reviews/{reviewId}/opinion
GET    /api/ai/empty-state-tip

POST   /api/reports
GET    /api/reports
GET    /api/reports/summary
PUT    /api/reports/{id}
POST   /api/reports/admin-hide

GET    /api/admin/operational-stats
GET    /api/admin/users
GET    /api/admin/users/{id}
PUT    /api/admin/users/{id}/suspend
PUT    /api/admin/users/{id}/reinstate
```

---

## 🎬 Libreria e serie TV

- Stati: `TO_WATCH` / `WATCHING` / `WATCHED`, con **limite di 3 elementi contemporaneamente in `WATCHING`**
- Per gli elementi con `status=WATCHING` e `contentType=TV` è possibile impostare/aggiornare la **stagione corrente** (`currentSeason`, campo opzionale), validata contro il numero reale di stagioni della serie recuperato da TMDB (o un tetto massimo di 50 se il dato TMDB non è disponibile)

---

## ⭐ Recensioni, community e sistema a punti

- Voto (1–5) + testo obbligatori per le recensioni in stato `WATCHED`
- **Sistema punti utente:** +10 per recensione creata, −15 per rimozione a seguito di segnalazione, −20 per sospensione. Lo score non scende mai sotto 0
- **Stati recensione:** `VISIBLE` / `HIDDEN` / `REMOVED`
- **Reazioni** (a recensioni e commenti): `LIKE` 👍 · `LOVE` ❤️ · `LAUGH` 😂 · `WOW` 😮
- **Classifiche:** Top 3 Film, Top 3 Serie, Top 3 Critici con media pesata (weighted average); `MIN_VOTES` minimo di voti per entrare in classifica (1 in ambiente di test, consigliato 3 in produzione)

---

## 🚩 Moderazione e segnalazioni

- **Categorie di segnalazione:** `SPAM` · `INAPPROPRIATE_CONTENT` · `OFF_TOPIC` · `OTHER`
- **Target:** una segnalazione può puntare a una `REVIEW` o a un `COMMENT` — distinti nella dashboard admin per evitare ambiguità su cosa si sta esaminando
- **Stati segnalazione:** `PENDING` · `APPROVED` · `REJECTED` · `ARCHIVED` (una segnalazione pendente passa automaticamente ad `ARCHIVED` quando l'autore del contenuto segnalato viene eliminato o sospeso permanentemente — non si applica se a sparire è il segnalante)

## 👤 Stati utente

`ACTIVE` / `SUSPENDED` / `PERMANENTLY_SUSPENDED` / `DELETED`

L'eliminazione account (`DELETE /api/users/me`) **anonimizza** username, email e password invece di cancellare fisicamente la riga (per preservare l'integrità di recensioni e commenti collegati). È un'operazione irreversibile: da `DELETED` non esiste `reinstate`.

**Ruoli:**
- `GUEST` — esplora, cerca, legge recensioni e classifiche (nessuna autenticazione)
- `USER` — tutto il guest + libreria, recensioni, chat AI, segnalazioni
- `ADMIN` — gestione segnalazioni, sospensione utenti, pannello dedicato

---

## 🤖 Assistente AI — dettaglio

Oltre alla chat conversazionale generica, l'AI copre diversi compiti puntuali:

| Endpoint | Cosa fa |
|---|---|
| `POST /api/ai/chat` | Chat generale con memoria di sessione, consapevole della libreria dell'utente |
| `POST /api/ai/chat/admin` | Variante della chat ad uso del pannello amministrativo |
| `GET /api/ai/daily` | Consiglio del giorno, con cache lato server per evitare chiamate ripetute all'LLM |
| `POST /api/ai/structure-review` | Riformatta un testo libero dell'utente in una recensione più curata |
| `POST /api/ai/structure-comment` | Come sopra, per i commenti |
| `POST /api/ai/movie-question` | Q&A puntuale su un contenuto specifico |
| `POST /api/ai/reviews/{reviewId}/opinion` | L'AI commenta/valuta una recensione esistente |
| `GET /api/ai/empty-state-tip` | Tip mostrato quando l'utente non ha ancora contenuti in libreria |

**Dettagli implementativi:**
- Modello: `llama-3.3-70b-versatile` via Groq API
- Memoria di sessione limitata alle ultime 10 coppie di messaggi (`MAX_HISTORY_MESSAGES`)
- Contesto libreria utente limitato ai 15 titoli più recenti (`LIBRARY_PROFILE_LIMIT`), per contenere il prompt
- Cache del consiglio del giorno valida 24h (`DailyRecommendationCache`)
- Retry automatico (max 2 tentativi, backoff 1.5s) in caso di errore dall'API Groq
- Gestione di un marcatore di spoiler nelle risposte generate
- Le chiamate vengono tracciate in `AiRecommendationLog` e sono soggette a rate limiting dedicato (`AiRateLimitFilter`, max 20 richieste/ora per utente, in-memory)

---

## 🎁 CiakLog Wrapped

`GET /api/wrapped` + pagina `WrappedPage.jsx`: genera un riepilogo annuale personale in stile "Spotify Wrapped" con le statistiche di attività dell'utente (libreria e recensioni) durante l'anno.

---

## 🔒 Sicurezza

- **JWT** per l'autenticazione, token con validità 8h
- **Rate limiting login** (`LoginRateLimitFilter` + `LoginRateLimiter`) — protezione da tentativi di brute-force
- **Rate limiting AI** (`AiRateLimitFilter`) — in-memory, max 20 richieste/ora per utente, adeguato per singolo nodo
- **Documentazione API automatica** — `OpenApiConfig` espone OpenAPI/Swagger
- **Gestione errori centralizzata** — `GlobalExceptionHandler` con eccezioni dedicate: `ResourceNotFoundException`, `ForbiddenException`, `UnauthorizedException`, `DuplicateResourceException`, `BusinessRuleException`, `LimitExceededException`, `TooManyRequestsException`, `AiServiceUnavailableException`
- **Log sospensioni manuali** (`ManualSuspensionLog`) — traccia le sospensioni applicate dagli admin, distinte da quelle automatiche generate dalle segnalazioni
- **Scheduler cache classifiche** (`ChartCacheEvictionScheduler`) — invalida periodicamente la cache delle classifiche

---

## 🔧 Versioni delle dipendenze principali

Dal `package.json` del frontend: `react@19.2.7`, `react-router-dom@6.30.4`, `zustand@5.0.3`, `axios@1.7.9`, `jwt-decode@4.0.0`.

Dev dependencies: `vite@8.1.0`, `oxlint@1.69.0`, `@vitejs/plugin-react@6.0.2`.

> Nota: il numero di versione di Vite (`^8.1.0`) è insolito rispetto alle release note; se non è intenzionale vale la pena controllare il lockfile — potrebbe trattarsi di una versione pre-release o di un refuso.
