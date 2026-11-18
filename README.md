# UM RainGuard — WIA2007 Mobile App Development (BudiBytes)

A campus drainage and flood-reporting app for Universiti Malaya. Students, staff and lecturers report
blocked drains and flooded spots; admins verify the reports, dispatch a maintenance team and publish
alerts; verified hazards appear on the campus map. Java 11 + XML views, one Room database, no Compose.

## Quick start

| | |
|---|---|
| **Toolchain** | Android Studio with AGP 9.4.1 · Gradle 9.6.0 · **JDK 25** for the Gradle daemon (pinned in `gradle/gradle-daemon-jvm.properties`) · Android SDK 37, `minSdk 24` |
| **Build** | `./gradlew :app:assembleDebug` → `app/build/outputs/apk/debug/app-debug.apk` |
| **Run** | Open the project in Android Studio and press Run, or `./gradlew :app:installDebug` with a device attached |
| **Sign in** | Choose *Campus User* or *Admin*, any valid e-mail, password `RainGuard123!`. This is a **demo** sign-in, not real authentication. |
| **First launch** | Room seeds the demo content into `rainguard.db`: 3 accounts, 5 reports, their AI-triage rows and the admin actions behind their current status |
| **Reset the data** | Uninstall the app or clear its storage. The schema is version 1 with destructive migration, so any version bump also wipes it. |
| **Where the data lives** | `/data/data/com.example.budibytes/databases/rainguard.db` |

`JAVA_HOME` must point at a real JDK 25 installation before Gradle will start. A stale `JAVA_HOME` is
the most common first-run failure here. On Windows, use `gradlew.bat` wherever the commands below say
`./gradlew`.

## How to use `docs/` while you develop

Everything in `docs/` exists so that two people do not invent two different versions of the same
table, status or screen. [docs/README.md](docs/README.md) is the index; this section is how to work
with it day to day.

### The five documents

| Document | Open it when | What it is |
|---|---|---|
| [MS-01 — Module specification](docs/module-specification.md) | Before you write or change a feature module | **Normative.** The functions, inputs, outputs, error rules and done-criteria for the five modules, plus the shared schema, privacy and role rules. |
| [DB-01 — Database layer](docs/database-layer.md) | Before you touch a table, a DAO or a repository | **Descriptive.** The stack (SQLite + Room 2.6.1), the five tables, which module function each query serves, and the review findings `DB-F-01`…`DB-F-14`. |
| [UI-01 — View hierarchy](docs/layout-view-hierarchy.md) | Before you edit a layout or add a screen | **Descriptive.** The View/ViewGroup tree of every screen, what is styled where, and the findings `F-01`…`F-20`. |
| [LC-01 — Lifecycle plan](docs/activity-lifecycle-plan.md) | Before you add a lifecycle callback | **Descriptive.** What runs in `onCreate` / `onBackPressed` / … today, what state is lost, and the plan for the full lifecycle documentation. |
| [AI-01 — AI usage log](docs/ai-usage-log.md) | Before **every** commit | **Mandatory appendix.** One entry per commit: the tool, the prompts, what was used, and how it was checked. |

Each document states the commit it was written at in its header. Code moves faster than prose; when
they disagree, the disagreement is a bug — fix it in the same pull request.

### Before you write code

1. Find your module in **MS-01** (1–5) and read its **Functions** table. That table is the contract:
   inputs, outputs and the error rules the UI is allowed to rely on.
2. Check **DB-01 §4** to see whether a DAO already serves that function, and **DB-01 §6** for a
   finding that already covers the area — several are still open and marked.
3. Check **UI-01 §5–§6** before adding a screen, so you reuse the existing layout template instead of
   inventing a fourteenth one.
4. If MS-01 leaves the point open, decide it with the team and record the decision in MS-01
   *Open decisions* — a decision that lives only in a chat message will be re-litigated.
5. Write down which document ID your change implements, and put it in the pull request.

### When your change touches X, update Y

| You change | Update |
|---|---|
| A screen, Activity or layout | UI-01 §5 catalogue + §6 tree; LC-01 §6 card if it adds a lifecycle callback |
| A table, column or enum | MS-01 *Database tables used by the modules* + DB-01 §3; bump the `@Database` version |
| A DAO query or a repository method | DB-01 §4 — the function-to-DAO table |
| A module decision | MS-01 *Open decisions* (Status column) and Appendix A of the same file |
| A review finding is fixed | Move it out of UI-01 §12 / LC-01 §8 / DB-01 §6 and say so in the commit message |
| A new document, or a new section | docs/README.md index and its "Maintaining these documents" table |
| **Anything at all** | **Append one entry to AI-01 §5** — see the rule below |

### Three rules that keep the docs worth reading

* **Cite, don't paraphrase.** In code review and commit messages write "implements MS-01 Module 4
  `assignReport`" or "closes DB-F-04". A reference is checkable; a summary is not.
* **Fix the doc in the same commit as the code.** A stale document is worse than no document, because
  the next person trusts it.
* **Keep the numbers reproducible.** UI-01 §15.2, DB-01 §7 and MS-01 Appendix A each list the command
  or check that produced the tables, so anyone can re-run them.

## Repository layout

```
app/src/main/java/com/um/rainguard/
├── data/
│   ├── model/          Report, User, AiTriage, VerificationLog, Alert, HazardPublic + enums
│   └── repository/     ReportRepository (the only door to the database), DemoData (first-run seed)
├── database/           RainGuardDatabase + the 5 DAOs — persistence only, no business rules
├── ui/                 13 Activities, base/BaseActivity (header, navigation, session), hazardmap/
└── utils/              UIUtils (enum ↔ label, formatting), NavigationHelper, Constants
app/src/main/res/       layouts, drawables, values
docs/                   the five documents above, plus assets/
```

## Before you push

```bash
./gradlew :app:compileDebugJavaWithJavac   # fast: Java compile + Room query verification
./gradlew :app:assembleDebug               # packages the APK
```

Room checks every `@Query` against the schema at compile time, so a green compile catches the most
common data-layer mistake. There are **no real tests yet** — only the two generated templates
(`ExampleUnitTest`, `ExampleInstrumentedTest`) — so running the app is still your job. If you add a
test, say in the pull request what it covers.

## Working with AI tools

The assignment runs under **Level 2** of Universiti Malaya's AI policy: AI may help with drafting,
refactoring and explaining, but the design, the debugging and the testing stay with you. That is
policed through [AI-01](docs/ai-usage-log.md), not through good intentions:

* Every commit appends **one** entry to AI-01 §5 — even a commit where no AI was used.
* The entry names the tool and model, quotes the prompts, and says how the output was verified and
  changed.
* The commit message ends with the entry id, e.g. `AI-2026-11-18-01`.
* "It compiles" is not "it was tested". Say which is true.

## Contributing

Branch from `main`, keep one concern per pull request, and make sure the pull request says which
document it implements or which finding it closes. Anything that changes behaviour on a screen should
come with a line in the pull request describing what you actually ran.
