<div align="center">

# 🎬 CiakLog

**Diario cinematografico personale con AI integrata**

[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.x-6DB33F?style=flat&logo=springboot&logoColor=white)](https://spring.io/projects/spring-boot)
[![React](https://img.shields.io/badge/React-19-61DAFB?style=flat&logo=react&logoColor=black)](https://react.dev)
[![MySQL](https://img.shields.io/badge/MySQL-8.0-4479A1?style=flat&logo=mysql&logoColor=white)](https://www.mysql.com)
[![JWT](https://img.shields.io/badge/JWT-Auth-000000?style=flat&logo=jsonwebtokens)](https://jwt.io)

</div>

---

## 📖 Cos'è CiakLog

CiakLog è un'applicazione web full-stack che combina tre anime:

- 📚 **Diario personale** — traccia film e serie con stati *Da vedere / In visione / Visto*, voti in ciak (1–5) e recensioni testuali
- 🌐 **Piattaforma social leggera** — recensioni pubbliche, classifiche, profili e trending visibili anche ai visitatori
- 🤖 **Assistente AI conversazionale** — conosce la tua libreria e suggerisce contenuti reali verificati su TMDB

> **Vantaggio rispetto ai competitor** (Letterboxd, Trakt, Simkl): nessuno offre un assistente AI conversazionale integrato con libreria personale e recensioni pubbliche.

---

## 📸 Screenshot

<!-- TODO: sostituire con screenshot/GIF reali dell'app prima di pubblicare.
     Consigliati almeno: Home, pagina dettaglio film, Chat AI, Wrapped. -->

| Home | Chat AI |
|---|---|
| _screenshot qui_ | _screenshot qui_ |

---

## ✨ Funzionalità principali

| Area | Funzionalità |
|---|---|
| 🔐 Auth | Registrazione, login JWT, modifica profilo/bio |
| 🎬 Catalogo | Ricerca TMDB, pagina dettaglio con voto medio CiakLog |
| 📚 Libreria | Da vedere / In visione / Visto, tracking stagione per le serie TV |
| ⭐ Recensioni | Voto + testo, modifica, paginazione |
| 💬 Community | Commenti e reazioni rapide (👍❤️😂😮) alle recensioni |
| 🏆 Classifiche | Top film, top serie, top critici |
| 🔥 Trending | Contenuti più discussi della settimana |
| 🤖 Chat AI | Assistente conversazionale che conosce la tua libreria, consiglio del giorno, aiuto nella scrittura delle recensioni |
| 🎁 Wrapped | Riepilogo annuale personale in stile "Spotify Wrapped" |
| 🚩 Moderazione | Segnalazioni e pannello admin dedicato |
| 🌗 Tema | Dark / Light mode |

Per l'elenco completo degli endpoint, delle regole di business e delle scelte architetturali → **[docs/ARCHITECTURE.md](docs/ARCHITECTURE.md)**

---

## 🛠️ Tech Stack

**Backend:** Java 17 · Spring Boot 3 · Spring Security + JWT · Spring Data JPA · MySQL 8 · TMDB API · Groq API (LLaMA 3.3 70B)

**Frontend:** React 19 · Vite · React Router v6 · Zustand · Axios

---

## 🚀 Setup locale

### Prerequisiti
- Java 17+
- Node.js 18+
- MySQL 8

### 1. Clona il repository

```bash
git clone https://github.com/WalidDalal/ciaklog.git
cd ciaklog
```

### 2. Backend

```bash
cd backend
```

Crea il database:
```sql
CREATE DATABASE ciaklog_db CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
```

Copia e configura le variabili d'ambiente:
```bash
cp src/main/resources/application.properties.example src/main/resources/application.properties
```

Modifica `application.properties` con le tue chiavi:
```properties
spring.datasource.password=LA_TUA_PASSWORD_MYSQL
jwt.secret=UNA_STRINGA_CASUALE_DI_ALMENO_32_CARATTERI
tmdb.api.key=LA_TUA_TMDB_API_KEY
groq.api.key=LA_TUA_GROQ_API_KEY
```

Avvia:
```bash
./mvnw spring-boot:run
```

Il backend parte su `http://localhost:8080`.

### 3. Frontend

```bash
cd frontend
npm install
npm run dev
```

Il frontend parte su `http://localhost:5173`.

---

## 🔑 Come ottenere le API key

| Servizio | Link | Note |
|---|---|---|
| TMDB | [themoviedb.org/settings/api](https://www.themoviedb.org/settings/api) | Gratuito |
| Groq | [console.groq.com](https://console.groq.com) | Gratuito con rate limit |

---

## 🏗️ Architettura (in breve)

```
Browser (React)
    │  HTTP/JSON
    ▼
Spring Boot REST API
    ├── Spring Security (JWT filter)
    ├── Controller → Service → Repository
    ├── MySQL (JPA/Hibernate)
    ├── TMDB API (catalogo)
    └── Groq API (LLM)
```

**Ruoli utente:** `GUEST` (esplora e legge) · `USER` (libreria, recensioni, chat AI) · `ADMIN` (moderazione, pannello dedicato)

Dettagli su endpoint, enum di stato, regole di business e scelte implementative → **[docs/ARCHITECTURE.md](docs/ARCHITECTURE.md)**

---

## 📝 Note di sviluppo

- `ddl-auto=validate` in produzione — gli indici DB vanno creati manualmente (o passa a `update` al primo avvio)
- `MIN_VOTES=1` nel file corrente (ambiente di test) — portare a 3 in produzione
- Le API key vanno nelle variabili d'ambiente, **mai committate**

---

## 👤 Autore

**Walid Dalal** — [github.com/WalidDalal](https://github.com/WalidDalal)

Progetto personale sviluppato per approfondire Spring Boot, React e l'integrazione di LLM in applicazioni web reali.
