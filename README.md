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

## 📸 Screenshot

<!-- TODO: sostituire con screenshot/GIF reali dell'app.
     Consigliati: Home, dettaglio film, Chat AI, Wrapped, pannello admin. -->

| Home | Chat AI |
|---|---|
| _screenshot qui_ | _screenshot qui_ |

---

## Funzionalità principali

| Area | Funzionalità |
|---|---|
| 🔐 Auth | Registrazione, login JWT, modifica profilo/bio |
| 🎬 Catalogo | Ricerca TMDB con paginazione, pagina dettaglio con voto medio CiakLog |
| 📚 Libreria | Da vedere / In visione / Visto, tracking stagione per le serie TV |
| ⭐ Recensioni | Voto + testo, modifica, paginazione |
| 💬 Community | Risposte alle recensioni, reazioni emoji (👍❤️😂😮) |
| 🏆 Classifiche | Top film, top serie, top critici |
| 🔥 Trending | Contenuti più discussi della settimana |
| 🤖 Chat AI | Assistente conversazionale che conosce la tua libreria, consiglio del giorno, recensione "a botta calda", Q&A sui film con gestione spoiler |
| 🎁 Wrapped | Recap annuale personale con statistiche + commento narrativo generato dall'AI |
| 🙈 Auto-nascondimento | L'autore può nascondere una propria recensione/risposta senza sanzioni |
| 🚩 Moderazione | Segnalazioni, auto-hide automatico, pannello admin con statistiche operative |
| 🌗 Tema | Dark / Light mode |

Per l'elenco completo degli endpoint, le regole di business e i dettagli architetturali → **[docs/ARCHITECTURE.md](docs/ARCHITECTURE.md)**

---

## Tech Stack

**Backend:** Java 17 · Spring Boot 3 · Spring Security + JWT · Spring Data JPA · MySQL 8 · TMDB API · Groq API (LLaMA 3.3 70B)

**Frontend:** React 19 · Vite · React Router v6 · Zustand · Axios

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

## Architettura (in breve)

**Ruoli utente:**
- `GUEST` — esplora, cerca, legge recensioni e classifiche
- `USER` — tutto il guest + libreria, recensioni, risposte, reazioni, chat AI, segnalazioni, Wrapped
- `ADMIN` — solo gestione segnalazioni, sospensione utenti, pannello dedicato (nessuna libreria o recensione)

Dettagli su endpoint, sistema a punti, moderazione e test → **[docs/ARCHITECTURE.md](docs/ARCHITECTURE.md)**

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
