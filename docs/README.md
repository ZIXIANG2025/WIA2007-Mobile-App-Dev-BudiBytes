# UM RainGuard — Engineering Documentation

This folder holds the written reference for the UM RainGuard app (`app/src/main`, Java + XML views,
no Compose): the **UI layer**, the **data layer**, and the **module specification** the two are
measured against. It is documentation only — nothing here changes runtime behaviour.

| Document | ID | What it answers |
|---|---|---|
| [module-specification.md](module-specification.md) | **MS-01** | What must each of the five modules do — functions, inputs, outputs, error rules, done-criteria — and what are the shared schema, privacy and role rules? (Normative; converted from the team's Word document.) |
| [database-layer.md](database-layer.md) | **DB-01** | What is the database built on, what are the five tables, which module functions can it already serve, and does PR #4 match MS-01? |
| [layout-view-hierarchy.md](layout-view-hierarchy.md) | **UI-01** | What View/ViewGroup tree does each screen build, how is it measured, what is styled where, and what should be fixed? |
| [activity-lifecycle-plan.md](activity-lifecycle-plan.md) | **LC-01** | What happens in `onCreate` / `onPause` / `onBackPressed` / … today, what state is lost, and how will the full lifecycle documentation be written, reviewed and delivered? |

## Reading order

1. **MS-01** — the specification. Read it before writing a feature module, so nobody redefines the
   tables, the statuses or the enums. The function tables in modules 1–5 are the contract.
2. **DB-01 §2–§4** — the stack (SQLite + Room 2.6.1), the five tables, and which of MS-01's 31 module
   functions the DAOs can already serve.
3. **DB-01 §5** — why the app already has a second data layer: `data/` vs `database/`, side by side,
   with the field mapping for the migration.
4. **DB-01 §6** — the review of PR #4 (`DB-F-01` … `DB-F-14`), ordered by severity. Start with
   `DB-F-01` (a save destroys the audit trail) and `DB-F-03` (the database is not wired to the UI).
5. **UI-01 §1–§5** — the numbers, the concepts (`View` vs `ViewGroup`), and the one screen template
   that 12 of the 13 screens follow. Read this before opening any layout file.
6. **UI-01 §6–§11** — the per-screen trees, item layouts, overlays, and the views that are created in
   Java rather than XML.
7. **UI-01 §12** — the review findings (`F-01` … `F-20`), ordered by severity. No build blocker was
   found; start with `F-01` (two dead layouts) and `F-03` (accessibility).
8. **LC-01 §2–§4** — the lifecycle baseline: only `onCreate`, `onBackPressed` and two
   `onActivityResult` overrides exist in the whole app.
9. **LC-01 §5–§8** — the per-callback content plan, the per-Activity cards, the journeys, and the
   risk register.
10. **LC-01 §9** — the execution plan (phases, owners, acceptance criteria) for the full lifecycle
    documentation set under `docs/lifecycle/`.

## At a glance

| Metric | Value |
|---|---|
| Activities | 13 concrete + 1 abstract base (`BaseActivity`) |
| Layout files | 26 (14 screen files: 12 used + 2 unused; 1 header include; 7 item layouts; 4 overlays) |
| XML elements / IDs | 538 / 251 |
| `LinearLayout` / `ConstraintLayout` | 165 / 0 |
| Lifecycle methods overridden | `onCreate` (2 classes), `onBackPressed` (1), `onActivityResult` (2) = **5** |
| Lifecycle methods absent | `onStart`, `onResume`, `onPause`, `onStop`, `onRestart`, `onDestroy`, `onSaveInstanceState`, `onRestoreInstanceState`, `onNewIntent` |
| Database tables / DAOs | 5 / 5 (23 files in `com.um.rainguard.database`) |
| Data layers present | **2** — `data/` (in use by 10 screens) and `database/` (unused, PR #4); see DB-01 §5 |
| Module functions specified / DAO-backed | 31 / 13 fully, 11 partial, 6 none, 1 not a DB concern |

## Conventions used in these documents

* `path:line` always means "in this repository, at this line today". UI-01 and LC-01 were written at
  commit `1c1970c`; MS-01 and DB-01 were written at `bcc40ed` (PR #4).
* **P0/P1/P2/P3** = build blocker / must fix / should fix / polish (no P0 found in either review).
* "Observed" = read from the source. "Recommended" = proposal, not implemented.
* Counts in tables are reproducible with the script in UI-01 §15.2; DB-01 §7 lists its own
  verification commands.

## Maintaining these documents

| Change | Update |
|---|---|
| New screen or Activity | UI-01 §5 catalogue + §6 tree; LC-01 §6 card + §7 journey |
| New lifecycle override | LC-01 §4 matrix; add a page under `docs/lifecycle/callbacks/` |
| Palette / drawable change | UI-01 §10 |
| New table, column or enum | MS-01 "Database tables used by the modules" + DB-01 §3; bump the `@Database` version |
| New DAO query or module function | DB-01 §4 table |
| A screen moves off `ReportItem` and onto Room | Cross off its row in DB-01 §5.2; when the last row goes, delete `data/` and §5 |
| A finding is fixed | Move it out of UI-01 §12 / LC-01 §8 / DB-01 §6 and note the fix in the commit message |
| The Word specification changes | Update it, then re-run the conversion described in MS-01 Appendix A; if a decision is settled, record it in MS-01 "Open decisions" and list it in Appendix A |
