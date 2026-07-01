```markdown
# Event Sourcing & CQRS – 35 Minuten Konferenzvortrag

---

## 1. Motivation (~5 min)

### Startfolie: Das Problem mit CRUD
- Klassische Datenbank speichert immer nur den **aktuellen Zustand**
- `UPDATE accounts SET balance = 120` – was war vorher? Weg.
- Konkrete Fragen, die CRUD nicht beantworten kann:
  - „Was war der Kontostand am 3. März um 14:32 Uhr?"
  - „Wer hat wann welche Änderung ausgelöst?"
  - „Wie kam es zu diesem fehlerhaften Zustand?"

### Die Idee: Zustand ist eine Ableitung aus Events
- Nicht den Zustand speichern, sondern **was passiert ist**
- Folie: `UPDATE balance = 120` vs. Liste von Events
  ```
AccountOpened    { initialBalance: 0    }
MoneyDeposited   { amount: 100          }
MoneyDeposited   { amount: 50           }
MoneyWithdrawn   { amount: 30           }



─────────────────────────────────────────
aktueller State: balance = 120
  ```
- Analogie zu **Git**: ein Commit speichert das Delta, nicht den
  kompletten Dateibaum. `git log` ist der Event Store.

### Was wir "for free" kriegen (Versprechen, kommt am Ende wieder)
- ✅ Vollständiges Audit-Log – kostenlos, weil Events sowieso da sind
- ✅ Zeitreise: „Was war der Zustand am 1. Januar?"
- ✅ Debugging: Bug in Produktion? Events in Testumgebung replizieren
- ✅ Read Models: wegwerfbar, jederzeit neu aufbaubar
- ✅ Kein Schema-Migration-Stress für Read-Seite

---

## 2. Konzepte (~7 min)

### Aggregates
- Ein **Aggregate** ist die Konsistenzgrenze der Domäne
  - Beispiel: `BankAccount` – kapselt alle Regeln rund ums Konto
  - Hat eine eindeutige **AggregateId**
  - Entscheidet, welche Commands valide sind
- Folie: Was ist KEIN Aggregate? (keine anämischen Datenobjekte)

### Der Fluss: Command → Event → State
```
REST-Call / Command
│
▼
Aggregate laden     ← alle Events der AggregateId replizieren
│
▼
Businesslogik       ← darf ablehnen (Exception)
│
▼
Neues Event erzeugen
│
▼
Event persistieren  ← Event Store
│
▼
State aktualisieren ← apply()
```
- Wichtig: das Aggregate wird **nie direkt gemutiert**
- `raise()` → Event erzeugen + sofort auf State anwenden
- `apply()` → reine State-Mutation (wird auch beim Laden verwendet)

### Was steht in einem Event?
- Nur das **fachliche Delta** – nicht den kompletten Zustand
  ```
❌ { type: "AccountUpdated", balance: 120, owner: "Max", ... }
✅ { type: "MoneyDeposited", amount: 100 }
  ```
- Metadaten (aggregateId, occurredAt) stehen als Spalten im Event Store,
  nicht im Payload – keine Redundanz
- Folie: Tabelle `domain_events` mit den Spalten zeigen

### Optimistic Locking
- Zwei parallele Requests laden denselben State (version=2)
- Beide erzeugen ein neues Event mit version=3
- `UNIQUE(aggregate_id, version)` → einer schlägt fehl
- Kein Mutex, kein Pessimistic Lock nötig

---

## 3. Demo-Anwendung vorstellen (~5 min)

### Überblick Projektstruktur (Folie)
```
domain/
events/          DomainEvent (sealed), AccountOpenedEvent, ...
BankAccountAggregate
store/
StoredEvent      JPA-Entity → Tabelle domain_events
EventStore       save() / load() / loadAll()
readmodel/
BankAccountReadModel       JPA-Entity → Tabelle bank_account_read_model
BankAccountProjector       lauscht auf Events, befüllt Read Model
ReplayService              deleteAll() + loadAll() + project()
api/
BankAccountController      POST /accounts, deposit, withdraw, GET
StatisticController        GET /statistics/accounts/above-balance
ReplayController           DELETE + POST /admin/read-model
```

### Relevante Codestellen als Folien vorbereiten

1. **`DomainEvent.java`** – sealed interface, zwei ignorierte Metadaten-Felder
2. **`BankAccountAggregate.java`** – `raise()` vs. `apply()`, `reconstitute()`,
   version-Zähler nur für persistierte Events
3. **`EventStore.java`** – `save()` mit expectedVersion,
   Optimistic Locking durch UNIQUE-Constraint,
   `ApplicationEventPublisher` für Projektoren
4. **`BankAccountProjector.java`** – `@TransactionalEventListener(AFTER_COMMIT)`,
   Live-Pfad (`on()`) vs. Replay-Pfad (`project()`),
   kurz Eventual Consistency ansprechen
5. **`ReplayService.java`** – `deleteAll()` + `loadAll()` + `project()` in
   einer Transaktion

---

## 4. 🌟 Demo 🌟 (~15 min)

### 4a. Event Sourcing zeigen → `bank-account.http`
1. Requests der Reihe nach ausführen, **wichtige Responses erklären**:
   - Request 1 (POST /accounts): Response-Body ist eine UUID – die AggregateId
   - Request 3–5 (deposit/withdraw): neue Balance direkt in der Response
   - Request 7 (Überziehung): sprechende Fehlermeldung, HTTP 422
   - Request 8 (GET nach Fehler): Balance noch 120 € – kein Event, kein Schaden
2. Nach dem Durchlauf: **`domain_events`-Tabelle in pgAdmin zeigen**
   - Jede Zeile = ein Event
   - Payload: nur das fachliche Delta
   - version: aufsteigend pro AggregateId
   - occurred_at: Zeitstempel für Zeitreise-Queries

### 4b. Projektion einführen (Folie)
- „Wie fragen wir ab: alle Konten mit Kontostand > 100 €?"
- Auf dem Event Store direkt: alle Events aller Aggregates laden,
  replizieren, filtern → **nicht praktikabel**
- Lösung: **CQRS** – Command-Seite und Query-Seite trennen
- Flussdiagramm: Event Store → Projektor → Read Model → Query API
- Eventual Consistency kurz ansprechen: minimale Verzögerung zwischen
  Write und Read Model – in den meisten Szenarien kein Problem

### 4c. Projektion zeigen → `statistics.http`
1. Zwei Konten anlegen, unterschiedliche Beträge einzahlen
2. `GET /statistics/accounts/above-balance?threshold=100` ausführen
3. **`bank_account_read_model`-Tabelle in pgAdmin zeigen** – normale
   SQL-Tabelle, normale Abfragen möglich ✅
4. Beide Tabellen nebeneinander: Event Store vs. Read Model –
   zwei verschiedene Sichten auf dieselbe Wahrheit

### 4d. Replay einführen (Folie)
- Read Models sind **wegwerfbar** – das ist kein Bug, das ist das Feature
- Anwendungsfälle:
  - Bug im Projektor gefunden → korrigieren → neu aufbauen
  - Neues Read Model für neuen Usecase → Replay über alle alten Events
  - Datenbankausfall auf der Read-Seite → einfach neu aufbauen
  - Schema-Änderung im Read Model → kein Migrations-Stress
- Kein manuelles Datenpflegen, kein Datenverlust – die Events sind unveränderlich

### 4e. Replay zeigen → `replay.http`
1. Requests 1–5 ausführen: Daten anlegen, Statistik prüfen
2. **Beide Tabellen nebeneinander in pgAdmin anordnen** (domain_events links,
   bank_account_read_model rechts)
3. Request 6 ausführen: `DELETE /admin/read-model`
   - Rechte Tabelle: leer ← sichtbar in pgAdmin
   - Linke Tabelle: unverändert ← der Event Store ist unangetastet
4. Request 7 ausführen: Statistik-Abfrage → leeres Array `[]`
5. Request 8 ausführen: `POST /admin/read-model/replay`
   - Response zeigt Anzahl replizierter Events
6. Request 9 ausführen: Statistik-Abfrage → Ergebnis ist wieder korrekt ✅
   - Rechte Tabelle: wieder befüllt – live in pgAdmin sichtbar

---

## 5. Weitergedacht: Microservices (~3 min)

> Dieser Block kann bei Zeitdruck gekürzt werden – Folie genügt.

### Zwei Arten von Events
```
Intern (Domain Events):          Öffentlich (Integration Events):
AccountOpened                    AccountActivated
MoneyDeposited                   FundsTransferred
LimitChanged
PasswordChanged ← bleibt intern!
```
- **Domain Events**: reich, granular, implementation-detail-lastig → bleiben im Service
- **Integration Events**: schlankes, versioniertes, öffentliches API-Kontrakt
- Ein Integration Event kann aus mehreren Domain Events entstehen

### Kafka kommt hier ins Spiel – aber richtig
- Kafka **nicht** als Event Store (kein effizienter aggregateId-Lookup,
  Log Compaction würde History zerstören)
- Kafka **als Transport** für Integration Events zwischen Services ✅
- Jeder Service hat seinen eigenen internen Event Store (PostgreSQL)
- Jeder Service hält genau die Daten vor, die er zur Verarbeitung braucht

---

## 6. Fazit (~2 min)

### Die Versprechen vom Anfang – eingelöst?
- ✅ **Audit-Log**: jede Zeile in `domain_events` ist ein unveränderlicher
  Nachweis – DSGVO, Compliance, Debugging kostenlos dabei
- ✅ **Zeitreise**: Events bis zu einem Zeitpunkt replizieren →
  exakter historischer Zustand
- ✅ **Debugging**: Bug reproduzieren indem man die Events
  in einer Testumgebung repliziert → exakter Produktionszustand
- ✅ **Kein Schema-Migrations-Stress**: Read Model kaputt oder veraltet?
  Löschen, Projektor anpassen, Replay – fertig
- ✅ **Komplexe Abfragen**: Read Models für jeden Usecase optimierbar,
  ohne das Write-Modell anzufassen

### Take-Aways
```
Event Sourcing ≠ Kafka        (Kafka ist Transport, nicht Store)
Read Model     ≠ Source of Truth  (Event Store ist die Wahrheit)
Snapshots       = Optimierung, kein Ersatz für die History
CQRS            = Command-Seite und Query-Seite bewusst trennen
PostgreSQL reicht  – kein spezielles System nötig
```

### Wo anfangen?
- Mit einem einzigen Aggregate in einem Service starten
- PostgreSQL als Event Store – vertraute Infrastruktur
- Erstes Read Model nur dort, wo komplexe Queries nötig sind
- Nicht alles auf einmal – Event Sourcing ist ein Spektrum

---

## Zeitplan

| Block | Inhalt | Zeit |
|---|---|---|
| 1 | Motivation & Konzepte | ~7 min |
| 2 | Konzepte (Aggregate, Fluss, Events) | ~5 min |
| 3 | Demo-Anwendung vorstellen (Code-Folien) | ~5 min |
| 4a | Demo: Event Sourcing | ~4 min |
| 4b/c | Demo: Projektion | ~4 min |
| 4d/e | Demo: Replay | ~4 min |
| 5 | Microservices (optional kürzbar) | ~3 min |
| 6 | Fazit + Take-Aways | ~2 min |
| – | Puffer / Q&A-Einstieg | ~1 min |
| | **Gesamt** | **35 min** |
```
