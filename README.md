# 🎬 CiakLog

![Tests](https://github.com/WalidDalal/ciaklog/actions/workflows/tests.yml/badge.svg)

**Diario cinematografico personale con AI integrata**

---

## Cos'è CiakLog

CiakLog è un'applicazione web full-stack che combina tre anime:

- 📚 **Diario personale** — traccia film e serie con stati *Da vedere / In visione / Visto*, voti in ciak (1–5) e recensioni testuali
- 🌐 **Piattaforma social leggera** — recensioni pubbliche, classifiche, profili e trending visibili anche ai visitatori
- 🤖 **Assistente AI conversazionale** — conosce la tua libreria e suggerisce contenuti reali verificati su TMDB

Vantaggio rispetto ai competitor (Letterboxd, Trakt, Simkl): nessuno offre un assistente AI conversazionale integrato con libreria personale e recensioni pubbliche.

---

## Funzionalità principali

| Area | Funzionalità |
|---|---|
| 🔐 Auth | Registrazione, login JWT (8h), modifica profilo/bio inline |
| 🎬 Catalogo | Ricerca TMDB, pagina dettaglio con voto medio CiakLog |
| 📚 Libreria | 3 stati (TO_WATCH / WATCHING / WATCHED), limite 3 WATCHING |
| ⭐ Recensioni | Voto + testo obbligatori per WATCHED, paginazione, modifica |
| 🏆 Classifiche | Top Film, Top Serie, Top Critici con weighted average |
| 🔥 Trending | Contenuti più discussi negli ultimi 7 giorni |
| 🤖 Chat AI | Floating widget + pagina intera, memoria di sessione, card risultati |
| 🚩 Moderazione | Segnalazioni (recensioni e risposte) con categorie, auto-hide a 2 report, nascondi diretto admin, sistema sanzioni, pannello admin con card statistiche operative |
| 💬 Risposte | Rispondi a una recensione, moderabili come le recensioni, con cascata se la recensione madre sparisce |
| 😀 Reazioni | Emoji su recensioni e risposte (👍❤️😂😮), +1 punto per reazione ricevuta |
| 🙈 Auto-nascondimento | L'autore può nascondere una propria recensione/risposta senza sanzioni, resta visibile solo all'admin |
| 🎁 CiakLog Wrapped | Recap personale annuale con statistiche + commento narrativo AI |
| ✨ AI estesa | Recensione "a botta calda" (appunti → AI struttura), "Chiedi su questo film" (Q&A con gestione spoiler), parere AI su recensioni negative, suggerimento contestuale negli stati vuoti |
| 🌗 Tema | Dark / Light mode con persistenza |

---

## Tech Stack

**Backend**
- Java 17 + Spring Boot 3
- Spring Security + JWT
- Spring Data JPA + MySQL 8
- TMDB API per il catalogo
- Groq API (LLaMA 3.3 70B Versatile) per l'AI

**Frontend**
- React 19 + Vite
- React Router v6
- Zustand (authStore, chatStore, themeStore, toastStore)
- Axios con interceptors JWT
- jwt-decode per lettura token lato client

---

## Struttura del progetto

```
ciaklog/
├── src/main/java/com/project/ciaklog/
│   ├── controller/
│   ├── service/impl/
│   ├── repository/
│   ├── entity/
│   ├── dto/request/ e dto/response/
│   ├── security/
│   ├── exception/
│   └── config/
├── src/test/java/com/project/ciaklog/     # test di regressione (vedi sezione Testing)
├── pom.xml
│
└── frontend/
    └── src/
        ├── pages/
        ├── components/
        ├── store/
        ├── services/
        └── hooks/
```

---

## Setup locale

**Prerequisiti**: Java 17+, Node.js 18+, MySQL 8

**1. Clona il repository**
```bash
git clone https://github.com/WalidDalal/ciaklog.git
cd ciaklog
```

**2. Crea il database**
```sql
CREATE DATABASE ciaklog_db CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
```

**3. Configura il backend**
```bash
cp src/main/resources/application.properties.example src/main/resources/application.properties
```
Modifica `application.properties` con le tue chiavi (MySQL, TMDB, Groq).

**4. Avvia il backend**
```bash
./mvnw spring-boot:run
```
Parte su [http://localhost:8080](http://localhost:8080)

**5. Avvia il frontend**
```bash
cd frontend && npm install && npm run dev
```
Parte su [http://localhost:5173](http://localhost:5173)

---

## Come ottenere le API key

- **TMDB**: [themoviedb.org/settings/api](https://themoviedb.org/settings/api) — gratuito
- **Groq**: [console.groq.com](https://console.groq.com) — gratuito con rate limit

---

## Testing

Il progetto ha una suite di test di regressione (JUnit 5 + Mockito, con H2 in memoria per i test sulla persistenza) e una pipeline CI su GitHub Actions che li esegue automaticamente ad ogni push su `dev/*`, `develop` e `main`.

```bash
mvn test
```

I test coprono in particolare i punti più delicati emersi durante una revisione approfondita del codice:
- Visibilità delle recensioni nel profilo pubblico (nessuna recensione rimossa o nascosta dall'autore deve trapelare a un visitatore, salvo il caso admin)
- Coerenza tra libreria e recensioni (niente recensioni orfane rimuovendo un titolo dalla libreria)
- Soglia di auto-nascondimento delle segnalazioni (non deve contare segnalazioni già respinte in passato)
- Gestione dei token JWT non più validi (risposta 401 pulita, non un errore generico)
- Rate limiting sul login
- Blocco della modifica su commenti/recensioni già rimossi

---

## Architettura

**Ruoli utente**:
- `GUEST` — esplora, cerca, legge recensioni e classifiche
- `USER` — tutto il guest + libreria, recensioni, risposte, reazioni, chat AI, segnalazioni, Wrapped
- `ADMIN` — **solo** gestione segnalazioni, sospensione utenti, pannello dedicato con statistiche operative. Non ha libreria, non recensisce, non usa la chat di raccomandazione (403 esplicito lato backend) — ha una chat gestionale separata (`/api/ai/chat/admin`)

**Sistema a punti**: +10 per recensione creata (le risposte non danno punti alla creazione); +1 per ogni reazione ricevuta (su recensioni e risposte); −15 per rimozione da moderazione (segnalazione approvata o nascondi diretto, sottrae anche i punti delle reazioni ricevute); −20 per sospensione; azzeramento se sospensione permanente. Score non scende sotto 0. L'auto-nascondimento dall'autore non ha alcun impatto sul punteggio (non è una sanzione).

**Chat AI**: floating widget in basso a destra (nascosto su /chat) + pagina intera. Memoria di sessione via Zustand, rate limiting 20 req/ora in-memory con sliding window. Guardrail di contesto: risponde solo a domande su film/serie/piattaforma.

---

## API principali

```
POST   /api/auth/register
POST   /api/auth/login

GET    /api/users/{username}
PUT    /api/users/me
DELETE /api/users/me                        // elimina account (anonimizza)

GET    /api/tmdb/search?q={query}&type={type}&page={n}   // paginazione reale, conteggio da TMDB
GET    /api/tmdb/{contentType}/{tmdbId}

GET    /api/library
POST   /api/library
PUT    /api/library/{id}
PATCH  /api/library/{id}/season
DELETE /api/library/{id}

GET    /api/reviews/media/{contentType}/{tmdbId}
GET    /api/reviews/user/{username}
POST   /api/reviews
PUT    /api/reviews/{id}
DELETE /api/reviews/{id}
PATCH  /api/reviews/{id}/visibility          // auto-nascondimento autore

GET    /api/reviews/{reviewId}/comments      // risposte
POST   /api/reviews/{reviewId}/comments
PUT    /api/comments/{id}
DELETE /api/comments/{id}
PATCH  /api/comments/{id}/visibility

GET    /api/reviews/{reviewId}/reaction      // reazioni emoji
PUT    /api/reviews/{reviewId}/reaction
DELETE /api/reviews/{reviewId}/reaction
GET    /api/comments/{commentId}/reaction
PUT    /api/comments/{commentId}/reaction
DELETE /api/comments/{commentId}/reaction

GET    /api/charts/films
GET    /api/charts/series
GET    /api/charts/users
GET    /api/charts/trending

GET    /api/wrapped                          // CiakLog Wrapped

POST   /api/ai/chat
POST   /api/ai/chat/admin
GET    /api/ai/daily
POST   /api/ai/structure-review              // recensione "a botta calda"
POST   /api/ai/structure-comment             // stesso, per le risposte
POST   /api/ai/movie-question                // "Chiedi su questo film"
POST   /api/ai/reviews/{reviewId}/opinion    // parere su recensione negativa
GET    /api/ai/empty-state-tip               // suggerimento contestuale

POST   /api/reports
GET    /api/reports
PUT    /api/reports/{id}
POST   /api/reports/admin-hide               // nascondi direttamente

GET    /api/admin/operational-stats
GET    /api/admin/users
GET    /api/admin/users/{id}
PUT    /api/admin/users/{id}/suspend
PUT    /api/admin/users/{id}/reinstate
```

---

## Note di sviluppo

- `ddl-auto=validate` in produzione — gli indici DB vanno creati manualmente
- Rate limiting AI in-memory — adeguato per singolo nodo
- `MIN_VOTES=1` nell'ambiente attuale (test) — portare a 3 in produzione
- Le API key vanno nelle variabili d'ambiente, mai committate

---

## Autore

**Walid Dalal** — [github.com/WalidDalal](https://github.com/WalidDalal)

Progetto personale sviluppato per approfondire Spring Boot, React e l'integrazione di LLM in applicazioni web reali.
