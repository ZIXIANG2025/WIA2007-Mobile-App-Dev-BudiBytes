# UM RainGuard — Engineering Documentation

This folder holds the written reference for the **UI layer** of the UM RainGuard app
(`app/src/main`, Java + XML views, no Compose). It is documentation only — nothing here changes
runtime behaviour.

| Document | ID | What it answers |
|---|---|---|
| [layout-view-hierarchy.md](layout-view-hierarchy.md) | **UI-01** | What View/ViewGroup tree does each screen build, how is it measured, what is styled where, and what should be fixed? |
| [activity-lifecycle-plan.md](activity-lifecycle-plan.md) | **LC-01** | What happens in `onCreate` / `onPause` / `onBackPressed` / … today, what state is lost, and how will the full lifecycle documentation be written, reviewed and delivered? |

## Reading order

1. **UI-01 §1–§5** — the numbers, the concepts (`View` vs `ViewGroup`), and the one screen template
   that 12 of the 13 screens follow. Read this before opening any layout file.
2. **UI-01 §6–§11** — the per-screen trees, item layouts, overlays, and the views that are created in
   Java rather than XML.
3. **UI-01 §12** — the review findings (`F-01` … `F-20`), ordered by severity. No build blocker was
   found; start with `F-01` (two dead layouts) and `F-03` (accessibility).
4. **LC-01 §2–§4** — the lifecycle baseline: only `onCreate`, `onBackPressed` and two
   `onActivityResult` overrides exist in the whole app.
5. **LC-01 §5–§8** — the per-callback content plan, the per-Activity cards, the journeys, and the
   risk register.
6. **LC-01 §9** — the execution plan (phases, owners, acceptance criteria) for the full lifecycle
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

## Conventions used in these documents

* `path:line` always means "in this repository, at this line today (commit `1c1970c`)".
* **P0/P1/P2/P3** = build blocker / must fix / should fix / polish (no P0 found in this review).
* "Observed" = read from the source. "Recommended" = proposal, not implemented.
* Counts in tables are reproducible with the script in UI-01 §15.2.

## Maintaining these documents

| Change | Update |
|---|---|
| New screen or Activity | UI-01 §5 catalogue + §6 tree; LC-01 §6 card + §7 journey |
| New lifecycle override | LC-01 §4 matrix; add a page under `docs/lifecycle/callbacks/` |
| Palette / drawable change | UI-01 §10 |
| A finding is fixed | Move it out of UI-01 §12 / LC-01 §8 and note the fix in the commit message |
