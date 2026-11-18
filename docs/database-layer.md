# UM RainGuard — Database Layer (Room) and PR #4 Review

| | |
|---|---|
| **Document ID** | DB-01 — Data layer reference and pull-request review |
| **Project** | WIA2007 Mobile App Dev — BudiBytes (`UM RainGuard`) |
| **Reviewed** | PR [#4](https://github.com/leejiancheng41-create/WIA2007-Mobile-App-Dev-BudiBytes/pull/4) `feature/changes` @ `bcc40ed` → `main` @ `da950c8` · 26 files, +762 / −1 · mergeable, 1 commit, no review comments |
| **Scope** | The 23 new files in `app/src/main/java/com/um/rainguard/database/`, the four dependency lines in `app/build.gradle.kts`, plus `README.md` and `gradle/gradle-daemon-jvm.properties` — and the existing `com.um.rainguard.data` package it overlaps with (§5) |
| **Method** | Every file read. `:app:compileDebugJavaWithJavac` executed (**BUILD SUCCESSFUL**) and Room's generated `_Impl` classes inspected, so the DDL, the enum mapping and the index list below are copied from code Room produced, not from the annotations alone. The `INSERT OR REPLACE` hazard in DB-F-01 was reproduced against the same schema with SQLite. |
| **Out of scope** | Feature modules 1–5 (not written yet), the AI provider, push notifications, authentication, visual QA |
| **Companion document** | [Module specification](module-specification.md) (MS-01) |

> **Verdict.** The five tables, their names and every enum match MS-01 almost statement for statement;
> the privacy split (`HazardPublic` vs `Report`) is implemented properly, not just described.
> What does **not** match is the contract around the schema: the spec asks for Kotlin + `Flow` +
> `Result` and a Repository/ViewModel per module, and this is Java + `LiveData` + `void` with no
> Repository at all. Two defects are worth blocking on: `ReportDao.upsert()` destroys the audit
> trail (DB-F-01), and nothing in the app reads this database yet (DB-F-03).

---

## 1. What PR #4 actually adds

| Part | Files | What it is |
|---|---|---|
| Entities | 5 | `User`, `Report`, `AiTriage`, `VerificationLog`, `Alert` — one `@Entity` each, one table each |
| DAOs | 5 | `UserDao`, `ReportDao`, `AiTriageDao`, `VerificationLogDao`, `AlertDao` |
| Database | 1 | `RainGuardDatabase` — the abstract `RoomDatabase` + singleton + write executor |
| Enums | 6 | `ReportStatus`, `ReportCategory`, `Severity`, `AlertLevel`, `AiStatus`, `TaskStatus` |
| Lookup constants | 2 | `CampusZones`, `MaintenanceTeams` (fixed string lists) |
| Projection | 1 | `HazardPublic` — the public, reporter-free view of a report |
| Support | 3 | `UserRole`, `ReviewAction`, `AlertSource` |
| Build | 1 | Room 2.6.1 + Lifecycle 2.6.2 added to `app/build.gradle.kts` |
| Unrelated | 2 | `README.md` (adds a docs link), `gradle/gradle-daemon-jvm.properties` (generated, see DB-F-11) |

The PR is a **data layer only**. It drops the schema in place and wires nothing to it. Judged against
the owner table in MS-01 — where `database` belongs to Xuan Pei and the five feature packages are
still `TBD` — that scope is correct and deliberate.

It is also not the app's *first* data layer. `com.um.rainguard.data` has been feeding every screen
since the frontend work, and the two packages overlap almost completely — see §5.

## 2. Tech stack for the database

There is no server, no cloud and no SQL dialect of the team's own choosing. Data lives in **SQLite on
the phone**, behind **Room**. That is the whole stack.

| Layer | Technology | Version | Notes |
|---|---|---|---|
| Storage engine | SQLite — Android's embedded engine | platform (`minSdk 24`) | One file: `rainguard.db` in the app's private storage (`/data/data/com.example.budibytes/databases/`). No credentials, no network, no port. |
| Mapping layer | **Room** | 2.6.1 | `androidx.room:room-runtime` + `room-compiler` (`app/build.gradle.kts:45-46`) |
| Code generation | `annotationProcessor` | javac | The project is Java (`sourceCompatibility 11`), so **kapt/KSP are not used**. Room generates `ReportDao_Impl` etc. at compile time and fails the build if a query does not match the schema. |
| Observable queries | AndroidX `LiveData` | 2.6.2 | DAO methods returning `LiveData` re-emit when the table changes — that is how MS-01's "appears on the map without restarting the app" is meant to work. |
| UI state | AndroidX `ViewModel` | 2.6.2 | Declared as a dependency; **no ViewModel class exists yet**. |
| Threading | `ExecutorService` (4 threads) | JDK | `RainGuardDatabase.databaseExecutor`; writes must go through it because Room forbids main-thread I/O. |
| Build | AGP 9.4.1 · Gradle 9.6.0 · compileSdk 37 · Java 11 | | |
| Backend | **none** | — | Matches MS-01's assumption "Room only now"; `String` UUID keys + epoch-millisecond times are what would make a later cloud sync a mapping exercise rather than a schema change. |

### 2.1 How a query flows

```
Activity  ──►  (future) Repository  ──►  ReportDao  ──►  Room-generated ReportDao_Impl
                                                              │
                                            SQLite (rainguard.db on the device)
                                                              │
              LiveData<List<Report>>  ◄───────────────────────┘   re-emitted on write
```

Today the middle box is missing, and the `Activity` talks to `data.repository.ReportRepository`
instead — see DB-F-03.

### 2.2 Why there are no `@TypeConverters`

Nine enum types (14 columns) are held in the schema, and Room stores each as its **`name()` text**
without a converter — built-in behaviour since Room 2.3. Room generated the conversion itself; from
`ReportDao_Impl`:

```java
statement.bindString(3, __ReportCategory_enumToString(entity.category));
statement.bindString(4, __Severity_enumToString(entity.severity));
statement.bindString(13, __ReportStatus_enumToString(entity.status));
```

So `status` in the database file is the literal string `'PENDING_MODERATOR'`, and a hand-written
`SELECT ... WHERE status = 'VERIFIED'` works. The comment in `RainGuardDatabase.java:23` is correct.

This is also why MS-01's "never type these as free strings" rule is enforceable: an invalid status
cannot be inserted through the DAO, and would be an unknown enum name on read.

### 2.3 What the storage layer looks like

Copied from `RainGuardDatabase_Impl.createAllTables()` (reformatted for width; column order, types
and keys are as generated):

| Table | Primary key | Foreign keys | Indices |
|---|---|---|---|
| `users` | `userId` TEXT | — | unique `email` |
| `reports` | `reportId` TEXT | — | `reporterId`, `status`, `zone` |
| `ai_triage` | `triageId` TEXT | `reportId` → `reports.reportId` **ON DELETE CASCADE** | unique `reportId` |
| `verification_logs` | `logId` TEXT | `reportId` → `reports.reportId` **ON DELETE CASCADE** | `reportId`, `actorId` |
| `alerts` | `alertId` TEXT | — | `isActive`, `level` |

Column types in use: `TEXT` for ids, enum names, place names, uris and the comma/line separated
lists; `INTEGER` for epoch milliseconds, counters and booleans (`isActive` is `INTEGER` 0/1);
`REAL` for `latitude`, `longitude` and `confidence`. **No `BLOB`** — photos are stored as a `Uri`
string, never as bytes, which is the right call for a local demo database.

Three columns are deliberately denormalised CSV/line-separated strings rather than child tables:
`User.subscribedZones` (`"KK8,Faculty buildings"`), `AiTriage.possibleDuplicateIds`, and
`Alert.affectedAreas` / `precautions`. That is a real trade-off: it keeps the schema to five tables,
but it cannot be indexed or joined, and every reader must call `.split(",")`. Acceptable at this
scale; recorded here so it is a decision rather than an accident.

## 3. Table-by-table conformance with MS-01

| MS-01 requirement | Code | Match |
|---|---|---|
| Five tables named `users`, `reports`, `ai_triage`, `verification_logs`, `alerts` | Exactly those `tableName` values | ✅ |
| `users` holds email, role, display name, alert preferences, subscribed zones | `users` + `notifyHeavyRain` / `notifyFlashFlood` / `weeklySummary` / `subscribedZones` | ✅ |
| `reports` holds the hazard report, location, status, votes, maintenance fields | `reports` + lat/long **and** `pinX`/`pinY`, `stillThereCount`/`clearedCount`, `assignedTeam`… `resolvedAt` | ✅ |
| `ai_triage` holds summary, suggested severity, safety advice, failure status, override | `ai_triage` + `modelName`, `latencyMs`, `confidence`, `errorMessage` | ✅ |
| `verification_logs` = one row per admin action, doubles as the timeline | `verification_logs` with `fromStatus`/`toStatus`/`team` | ✅ |
| `alerts` holds title, message, level, affected zones, precautions | `alerts` + `source`, `relatedReportId`, `expiresAt`, `isActive` | ✅ |
| Six report statuses, exact names | `ReportStatus` — identical, plus `PUBLIC_STATUSES` helper | ✅ |
| Alert levels `INFO`/`ADVISORY`/`SEVERE`/`EMERGENCY` | `AlertLevel` — identical | ✅ |
| `ReviewAction` values `VERIFY`/`REJECT`/`ASSIGN`/`UPDATE_PROGRESS`/`RESOLVE`/`OVERRIDE_AI` | `ReviewAction` — identical | ✅ |
| AI failure recorded as `FAILED` or `TIMEOUT` | `AiStatus.{SUCCESS, FAILED, TIMEOUT}` | ✅ |
| Task states `ASSIGNED`, `INSPECTION_IN_PROGRESS`, `REPAIR_IN_PROGRESS`, `COMPLETED` | `TaskStatus` + `AWAITING_ASSIGNMENT` | ✅ |
| Three maintenance teams, exact names | `MaintenanceTeams.ALL` — identical | ✅ |
| Zones "KK1 to KK12, Faculty buildings, Central campus" | `CampusZones.ALL` — exactly 14 entries | ✅ |
| Categories = drain categories + flooded road, fallen tree, other | `ReportCategory` — 7 values incl. `FLOODED_ROAD`, `FALLEN_TREE` | ✅ |
| IDs are UUID strings | `UUID.randomUUID().toString()` in every entity | ✅ |
| Times are epoch milliseconds | `long createdAt = System.currentTimeMillis()`, `Long resolvedAt/expiresAt/…` | ✅ |
| `reporterId` private; public uses `HazardPublic` with no reporter fields | `HazardPublic` + `ReportDao.PUBLIC_COLUMNS` + `Report.toPublic()` — no `reporterId`, no email | ✅ |
| "Each module package contains a Repository, a ViewModel and its screens" | No Repository, no ViewModel, no module packages | ❌ |
| Function outputs typed `Flow<List<…>>` / `Result<…>` (Kotlin) | `LiveData` / `void` (Java) | ❌ |
| Open decision: "Programming language — Kotlin" | Java 11 | ❌ |

**17 of the 20 statements above are satisfied exactly** — all five tables, every enum, both constant
lists, the UUID/epoch convention and the public/private split. The three misses are not schema
detail; they are the language and layering decisions MS-01 assumed and this repository made
differently.

## 4. Are the 31 module functions serviceable?

MS-01 lists 31 functions across the five modules. They are *repository*-level contracts, so a DAO is
never a complete answer — but it shows how much of each contract the data layer can already support.

| | Supported by a DAO today | Partial | No query exists | Not a database concern |
|---|---|---|---|---|
| Module 1 — Hazard Reporting | 4 | 1 | 1 | 1 |
| Module 2 — Campus Hazard Map | 1 | 1 | 2 | 0 |
| Module 3 — AI Triage | 1 | 2 | 1 | 0 |
| Module 4 — Verification | 2 | 6 | 1 | 0 |
| Module 5 — Alerts | 5 | 1 | 1 | 0 |
| **Total (31)** | **13** | **11** | **6** | **1** |

| Module | Function | Data layer status |
|---|---|---|
| 1 | `createReport` | `ReportDao.upsert` — the ID is generated by `new Report()`, so the caller never receives a `Result` |
| 1 | `uploadPhoto` | Not a DB concern; `photoUri` column exists |
| 1 | `findSimilarReport` | **Missing** — no "same zone + category, still open" query |
| 1 | `confirmStillThere` / `markCleared` | `addStillThere` / `addCleared` (atomic `+ 1`) |
| 1 | `getMyReports` | `observeByReporter` |
| 1 | `getReportDetails` | `getById` + `VerificationLogDao.observeForReport`; the owner/admin check must live in a repository |
| 2 | `getMapHazards` | Query exists, but it takes **no** zone/category/severity arguments |
| 2 | `getHazardDetails` | `observePublicHazard` / `getPublicHazard` (public statuses only) |
| 2 | `startReportAtLocation` | **Missing** — nothing derives a zone from lat/long |
| 2 | `getHazardCounts` | **Missing** — no count-by-severity query |
| 3 | `triageReport` | `AiTriageDao.upsert` + `ReportDao.updateStatus`, but as two separate writes |
| 3 | `getAiSuggestion` | `getByReportId` / `observeByReportId` |
| 3 | `findDuplicates` | **Missing** — the `possibleDuplicateIds` column exists, the search does not |
| 3 | `recordModeratorOverride` | Two writes (`ai_triage` + `verification_logs`), no shared transaction |
| 4 | `getReviewQueue` | `observeReviewQueue` (oldest first, `PENDING_MODERATOR`) |
| 4 | `getAllReports` | `observeAll` — no status/category/severity/date/search filtering |
| 4 | `verifyReport` … `resolveReport` | `updateStatus` / `update` + `VerificationLogDao.insert`, never in one transaction |
| 4 | `getActivity` | `observeRecent(limit)` |
| 4 | `getAnalytics` | **Missing** — no status counts, no completed-vs-total, no hotspot query |
| 5 | `getActiveAlerts` | `observeActive` — ignores `expiresAt` and the user's zones |
| 5 | `getAlertDetails` / `createAlert` / `deactivateAlert` | `getById` / `upsert` / `deactivate` |
| 5 | `getPreferences` / `savePreferences` | `UserDao.getById` / `upsert` |
| 5 | `getRiskOverview` | **Missing** — MS-01 says "sample data for now", so no table is expected |

## 5. The `data/` package — does it overlap with the database?

**Yes, and completely.** `com.um.rainguard.data` is not a helper package sitting beside the database;
it is an earlier, fully working implementation of the same responsibility. It is what the app
actually uses today.

| File | Lines | What it is |
|---|---|---|
| `data/model/ReportItem.java` | 57 | The report record — 21 public fields, 18 of them `String` |
| `data/model/HistoryEntry.java` | 12 | One row of the report timeline (a date and a line of text) |
| `data/repository/ReportRepository.java` | 232 | Static singleton: an in-memory `List<ReportItem>`, persisted as one JSON string, seeded with five demo reports |

`ReportRepository` already provides the operations a `hazardreporting` repository is asked for in
MS-01: list all, find by id, add, filter by status/category/risk/date/search text, and count by
status/risk/task status. So this is not a partial overlap or a layering mistake — the same job is
implemented twice, in two incompatible ways.

### 5.1 What each side owns

| Concern | `data/` — in use today | `database/` — PR #4 |
|---|---|---|
| Report record | `ReportItem`, 21 public fields | `Report`, 23 columns |
| Value types | everything a `String`: `"High"`, `"In progress"`, `"2026-10-24"`, `"2h ago"` | typed enums; epoch `long` |
| Identity | display ids `"RG-1042"`, numbered by scanning the list in `ReportSubmissionActivity` | UUID `reportId` |
| Who reported it | `owner`, the reporter's **email address**, on the object every screen receives | `reporterId` UUID, never in `HazardPublic` |
| Timeline | `ReportItem.history` inside the report JSON | `verification_logs` table + `ReviewAction` |
| Vote counters | `votes` / `cleared` ints incremented in the Activity | `stillThereCount` / `clearedCount`, atomic `+ 1` SQL |
| AI suggestion | one `summary` string | `ai_triage` table |
| Persistence | `SharedPreferences`, file `rainguard-demo-data`, key `reports` — the whole list is re-serialised on every change | SQLite file `rainguard.db` through Room |
| Change notification | none; screens re-read the list and rebuild their views by hand | `LiveData` |
| Constraints | none — any string is accepted | primary keys, foreign keys, indices, enums |
| Coverage | reports only | reports, users, ai_triage, verification_logs, alerts |

Three related things also live outside the database, in the same preferences file that MS-01 assigns
to `users`:

* the notification preferences, keyed per account as `settings.<email>.heavyRain` and friends
  (`BaseActivity` line 291);
* the subscribed zones, as one `|`-separated string;
* a second hard-coded copy of the 14 zone names (`BaseActivity` lines 296-299) which is exactly
  `CampusZones.ALL` — two sources for a list that must not drift.

### 5.2 Field mapping, if the screens are migrated

| `ReportItem` | `database` | Note |
|---|---|---|
| `id` (`"RG-1042"`) | `reportId` (UUID) | The human-readable id has no column; keep the "RG-" formatter or add a `displayId` |
| `place` | `locationName` (+ `zone`) | `zone` is a required field the old model never had |
| `category` (`"Clogged"`) | `category` (`ReportCategory`) | free string → enum |
| `type` (`"Clogged drain · leaves"`) | — | no equivalent; it is a free-text subtype |
| `status` (`"Submitted"`) | `status` (`ReportStatus`) | UI wording → enum name |
| `risk` (`"High"`) | `severity` (`Severity`) | renamed |
| `time` (`"2h ago"`) | — | derived from `createdAt` |
| `createdAt` (`"2026-10-24"`) | `createdAt` (epoch ms) | formatted date → millis |
| `description` | `description` | |
| `owner` (email) | `reporterId` (UUID) | the email is replaced by an id; this is the privacy fix |
| `votes` | `stillThereCount` | renamed |
| `cleared` | `clearedCount` | renamed |
| `team` | `assignedTeam` | renamed |
| `instructions` | `instructions` | |
| `taskStatus` (`"Inspection in Progress"`) | `taskStatus` (`TaskStatus`) | UI wording → enum name |
| `summary` | `ai_triage.summary` | moves to another table |
| `resolution` | `resolutionNotes` | renamed |
| `resolvedAt` (`"2026-10-23 11:30"`) | `resolvedAt` (epoch ms) | formatted stamp → millis |
| `photoUri` | `photoUri` | |
| `completionPhotoUri` | `completionPhotoUri` | |
| `history` (`HistoryEntry` list) | `verification_logs` rows | embedded list → table |

`latitude`, `longitude`, `pinX`/`pinY`, `reporterId` and every alert, user and AI column have no
counterpart in `data/` at all.

### 5.3 Consequences

* **MS-01's shared rules are already broken by the code that ships.** Statuses are free strings, ids
  are not UUIDs, and the reporter's email travels with the model every screen receives. Nothing
  enforces the enums.
* **The app's real source of truth is a JSON blob.** `BaseActivity` builds `ReportRepository` for 12
  of the 13 Activities and 10 of them call it directly; nothing calls
  `RainGuardDatabase.getInstance()`. Until the screens move, the Room schema is unused code.
* **Two models cannot both survive.** Retiring `ReportItem` and `ReportRepository` and routing each
  screen through a module repository over `ReportDao` is the work that makes PR #4 real; extending
  `data/` would only deepen the split.
* **The email is not displayed anywhere** — `owner` is used for ownership filtering in
  `NotificationsActivity` and `ReportRepository.visibleReports`, and no layout binds it. So this is
  not a visible leak today, but it is one field away from being one, and the privacy rules cannot be
  enforced while the public model carries it.

## 6. Findings

Severity follows UI-01: **P0** build blocker · **P1** must fix · **P2** should fix · **P3** polish.
No P0 was found — the PR compiles and Room's query verification passes.

| ID | Sev. | Finding | Where |
|---|---|---|---|
| DB-F-01 | **P1** | `ReportDao.upsert()` is `INSERT OR REPLACE`. Both child tables cascade on delete and Room enables `PRAGMA foreign_keys`, so re-saving an existing report **deletes its `ai_triage` row and its entire `verification_logs` timeline** — the audit trail MS-01 Module 4 requires. Reproduced: `(reports, logs) = (1, 1)` before, `(1, 0)` after. | `ReportDao.java:19-20` |
| DB-F-02 | **P1** | No `@Transaction` anywhere. Every Module 4 action is two calls (status + log row); if the second fails the report has moved with no audit row, contradicting "each action adds exactly one `VerificationLog` row". | all DAOs |
| DB-F-03 | **P1** | Nothing reads this database, and the layer it competes with is already live: `BaseActivity` builds `data.repository.ReportRepository` (in-memory list, persisted as JSON in `SharedPreferences`) over `data.model.ReportItem` for 12 of the 13 Activities, and 10 call it directly. No code calls `RainGuardDatabase.getInstance()`. Two models of one object — full comparison in §5. | `ui/**`, `data/**` |
| DB-F-04 | **P2** | The picture-map coordinates are stored but cannot be read publicly: `HazardPublic` and `PUBLIC_COLUMNS` omit `pinX`/`pinY`, and `Report.toPublic()` does not copy them. MS-01 Module 2 says "the database stores both, so either works" — today only the lat/long route works. | `HazardPublic.java`, `ReportDao.java:16` |
| DB-F-05 | **P2** | Module 2's filters and counts have no query (`getMapHazards`, `getHazardCounts`). | `ReportDao` |
| DB-F-06 | **P2** | Module 5's alert contract is incomplete: `observeActive()` filters only `isActive`, so expired alerts still show and per-user zone filtering is impossible at query level. | `AlertDao.java:19` |
| DB-F-07 | **P2** | The language mismatch is now closed — MS-01 was updated on 18 Nov 2026 to record **Java 11** as decided — but the layering is still missing: no Repository or ViewModel exists in any module package, `Result<T>` is not implemented anywhere, and the DAOs return `void`, so a caller cannot tell success from failure. | whole PR |
| DB-F-08 | **P2** | `exportSchema = false` + `fallbackToDestructiveMigration()`: no schema history to diff or test, and any `version` bump silently wipes every row. Fine for a demo, must not ship. | `RainGuardDatabase.java:30,53` |
| DB-F-09 | **P3** | Module 4 analytics and each module's Repository remain unimplemented (expected — module owners are `TBD`). | — |
| DB-F-10 | **P3** | `reports.reporterId`, `verification_logs.actorId` and `alerts.createdBy` are not foreign keys, so referential integrity is entirely the repositories' job. Reasonable for privacy and for a future cloud sync; it should be written down. | entities |
| DB-F-11 | **P3** | `gradle/gradle-daemon-jvm.properties` pins the Gradle daemon to **JDK 25** via hard-coded foojay URLs. It is machine-generated (`#This file is generated by updateDaemonJvm`) and unrelated to the database; committing it makes every teammate and CI job download that JDK. | `gradle/` |
| DB-F-12 | **P3** | The four new dependencies are literal strings while every other dependency uses the version catalog (`libs.versions.toml`). `app/build.gradle.kts` still has no trailing newline. | `app/build.gradle.kts:45-48` |
| DB-F-13 | **P3** | `docs/` was not updated by the PR: the index listed only UI-01 and LC-01 and there was no data-layer page. *(Addressed by this documentation update: MS-01 and DB-01 added, index updated.)* | `docs/README.md` |
| DB-F-14 | **P3** | `README.md` reads "Documentation To for building this APP". | `README.md:3` |

### 6.1 How to fix DB-F-01 and DB-F-02

```java
// DB-F-01 — Room 2.5+ has a real upsert: it inserts, and on a primary-key conflict updates that
// row in place. No DELETE, therefore no cascade on ai_triage or verification_logs.
@Upsert
void upsert(Report report);

// DB-F-02 — an action spans two DAOs, so the transaction belongs where both are reachable.
// The repository is the natural place; the outcome is one atomic commit.
db.runInTransaction(() -> {
    reportDao.updateStatus(reportId, ReportStatus.VERIFIED, now);
    verificationLogDao.insert(log);
});
```

The entity always carries a `reportId` (generated in the field initialiser), so `@Upsert` takes the
insert path for new reports and the update path for existing ones. Room 2.6.1 is already on the
classpath; `@Upsert` needs 2.5.0 or newer.

## 7. How to verify this review

```bash
# 1. It compiles, and Room generates 6 classes (5 DAOs + the database)
JAVA_HOME=<jdk-25> ./gradlew :app:compileDebugJavaWithJavac
ls app/build/generated/ap_generated_sources/debug/out/com/um/rainguard/database/

# 2. The generated DDL, including the ON DELETE CASCADE clauses
grep -n "REFERENCES" app/build/generated/ap_generated_sources/debug/out/com/um/rainguard/database/RainGuardDatabase_Impl.java

# 3. The enum mapping (no TypeConverters on the classpath)
grep -n "enumToString" app/build/generated/ap_generated_sources/debug/out/com/um/rainguard/database/ReportDao_Impl.java

# 4. DB-F-01, against the same schema
sqlite3 :memory: "PRAGMA foreign_keys=ON;
  CREATE TABLE reports(reportId TEXT PRIMARY KEY, status TEXT NOT NULL);
  CREATE TABLE verification_logs(logId TEXT PRIMARY KEY, reportId TEXT NOT NULL,
    FOREIGN KEY(reportId) REFERENCES reports(reportId) ON DELETE CASCADE);
  INSERT INTO reports VALUES('R1','PENDING_AI');
  INSERT INTO verification_logs VALUES('L1','R1');
  INSERT OR REPLACE INTO reports VALUES('R1','PENDING_MODERATOR');
  SELECT COUNT(*) FROM verification_logs;"   -- prints 0
```

On this machine the build needs `JAVA_HOME` set to a real JDK: the inherited value pointed at a
`jdk-21.0.5.11` directory that does not exist, while `gradle-daemon-jvm.properties` wants JDK 25.

## 8. Open questions for the team

1. **The login is still a demo.** `MainActivity` accepts any syntactically valid email, takes the
   role from a selection, and passes the identity to the next screen as an Intent extra
   (`Constants.EXTRA_EMAIL`). There is no password, token or server check, and the preference file is
   literally named `rainguard-demo-session`. MS-01 now records **real login** as decided, so this
   screen needs an authentication provider before Module 4's "only an admin may…" rules mean anything.
2. **Who owns the migration from `ReportRepository` to Room** (§5)? Until that happens the app has two
   sources of truth, and the seeded demo data in `ReportRepository` is the only data any screen shows.
   Retiring `ReportItem` is the single change that turns PR #4 from unused code into the data layer.
3. **Picture map or real map?** If the picture map stays in scope, `pinX`/`pinY` must be added to
   `HazardPublic` and to `PUBLIC_COLUMNS`; it is a two-line change now and a schema audit later.
4. **Is `applicationId` (`com.example.budibytes`) meant to stay?** It differs from the namespace
   (`com.um.rainguard`) and it names the database's private directory. Pre-existing, but it becomes
   visible the first time anyone inspects `rainguard.db` on a device.
5. **When does the database stop being disposable?** `fallbackToDestructiveMigration()` is the right
   default while the schema churns; name the commit after which migrations are mandatory.
