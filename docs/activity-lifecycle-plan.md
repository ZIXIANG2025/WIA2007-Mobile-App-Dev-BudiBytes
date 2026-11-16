# UM RainGuard — Activity Lifecycle Documentation Plan

| | |
|---|---|
| **Document ID** | LC-01 — lifecycle documentation plan and current-state baseline |
| **Project** | WIA2007 Mobile App Dev — BudiBytes (`UM RainGuard`) |
| **Branch / commit** | `frontend` @ `1c1970c` |
| **Scope** | 14 Activity classes (13 concrete + abstract `BaseActivity`), `AndroidManifest.xml`, navigation helpers, the state they read and write |
| **Purpose** | (a) Record what the app does today in every lifecycle callback. (b) Define the structure, content, owners and acceptance criteria for the full lifecycle documentation that the team will write. |
| **Status** | Baseline complete; authoring plan ready to execute (§9) |
| **Companion** | [View & ViewGroup documentation](layout-view-hierarchy.md) |

---

## 1. Why this document exists

The app has **13 screens, 13 Activity classes, and exactly 5 overridden lifecycle methods**. Most of the lifecycle is therefore *implicit*: the framework's default behaviour is doing the work, and no code documents the contract the screens rely on.

That creates three concrete problems for the team:

1. **Nothing records what state survives what.** Rotation, process death, and "Don't keep activities" each destroy a different amount of state, and the app handles none of them explicitly.
2. **Navigation is non-standard.** Screens call `startActivity()` + `finish()` rather than using the back stack conventionally, so the "where does Back go?" answer lives in one 14-line method (`BaseActivity.onBackPressed`) instead of being obvious.
3. **The eventual migration** (to `AppCompatActivity`, `OnBackPressedDispatcher`, `ActivityResultLauncher`, `ViewModel`, or Compose) needs a written baseline to migrate *from*. Without it, every refactor risks changing behaviour nobody documented.

This plan turns that implicit behaviour into a reference the team can review, test against, and hand in.

---

## 2. Baseline facts

### 2.1 Class graph

```
android.app.Activity
├─ MainActivity                      (launcher, sign-in, 105 lines, NOT a BaseActivity)
└─ BaseActivity  «abstract»          (404 lines: session, layout, header, nav, dialogs, back)
   ├─ DashboardActivity              (student or admin layout chosen at runtime)
   ├─ CampusHomeActivity             (reuses screen_dashboard)
   ├─ HazardMapActivity
   ├─ ReportSubmissionActivity
   ├─ MyReportsActivity
   │  └─ AllReportsActivity          (subclass: only flips isManagement)
   ├─ RiskOverviewActivity
   ├─ MaintenanceTasksActivity
   ├─ AnalyticsActivity
   ├─ NotificationsActivity
   ├─ SettingsActivity
   └─ ReportDetailsActivity
```

`BaseActivity` is abstract and is **not** declared in the manifest. It contributes three abstract members that every subclass implements: `getLayoutResourceId()`, `getInitialPage()`, `initView()`.

### 2.2 Manifest facts that shape the lifecycle

| Fact | Value | Lifecycle consequence |
|---|---|---|
| Launcher | `.ui.auth.MainActivity`, `exported="true"` | Only entry point from the launcher; everything else needs extras |
| Other activities | `exported="false"` | Only reachable through the app's own navigation (or from `adb shell`, which holds `START_ANY_ACTIVITY`). Starting one without session extras triggers the sign-in redirect. |
| `launchMode` | not set → `standard` | A new instance per `startActivity`, even for the same screen |
| `configChanges` | not set | Rotation, font-scale, locale, theme and dark-mode changes **recreate** the Activity |
| `screenOrientation` | not set | Portrait/landscape both allowed → rotation is a real path |
| `windowSoftInputMode` | `adjustResize` on all 13 | Keyboard resizes the window instead of panning; layout must survive it |
| `theme` | `@style/AppTheme` (platform `Theme.Material.Light.NoActionBar`) | Platform `Activity`, not `AppCompatActivity` |
| `targetSdk` | 37 (`app/build.gradle.kts:14`) | Edge-to-edge enforced; predictive back on by default |
| `applicationId` | `com.example.budibytes` | The runtime component name; `namespace` is `com.um.rainguard` |

Runtime component name for adb:

```
com.example.budibytes/com.um.rainguard.ui.auth.MainActivity
```

### 2.3 The extras contract

`NavigationHelper.navigateTo()` (`NavigationHelper.java:83-94`) is the only way screens are opened after sign-in. It always writes five extras:

| Extra key | Constant | Read in | Meaning |
|---|---|---|---|
| `screen_role` | `EXTRA_ROLE` | `BaseActivity.onCreate` | `"admin"` or `"student"` |
| `screen_email` | `EXTRA_EMAIL` | `BaseActivity.onCreate` | Signed-in identity |
| `screen_page` | `EXTRA_PAGE` | `BaseActivity.onCreate` | Which page this instance is (drives title, bottom-nav highlight, back routing) |
| `screen_report_id` | `EXTRA_REPORT_ID` | `BaseActivity.onCreate` | Only for `report-details` |
| `screen_return_page` | `EXTRA_RETURN_PAGE` | `BaseActivity.onCreate` | Where Back should go from details |

Because all of this arrives via `Intent`, it **survives Activity recreation and process death** — the framework re-delivers the original Intent. That is the single most important lifecycle property of the app: *the screen's identity is in the Intent, not in a field.*

### 2.4 The three state stores

| Store | Type | Lifetime | Holds |
|---|---|---|---|
| Intent extras | in-memory, re-delivered | Activity instance + process | role, email, page, report id, return page |
| `SharedPreferences` `rainguard-demo-session` | disk | until cleared | `remember`, `role`, `email` |
| `SharedPreferences` `rainguard-demo-data` | disk | until cleared | all reports as JSON, all notification preferences |
| `ReportRepository` | `static` singleton, in-memory | process | the live `List<ReportItem>` (seeded from prefs in its constructor) |
| View state (spinners, text, scroll) | in-memory, view tree | Activity instance | filters, search text, description, pending photo |

Only the first three survive reliably. The repository is re-seeded from prefs after process death, but **no lifecycle callback ever saves or restores the last row**.

### 2.5 Navigation model

Every navigation is "start the next screen, then finish this one":

```java
// BaseActivity.java:207-221
public void navigate(String target) {
    ...
    NavigationHelper.navigateTo(this, role, userEmail, target, null, target);
    finish();
}
```

Consequences to document:

* The back stack holds at most **two** Activities during a transition (the finishing caller plus the starting target), and one in steady state — `MainActivity` also calls `finish()` after sign-in.
* The back stack is **not** the navigation history. `BaseActivity.onBackPressed()` re-derives the destination from `currentPage` and `returnPage`, not from the stack.
* `finish()` is requested immediately after `startActivity()`, so the caller tears down while the target is starting. The two do not run concurrently (both are on the main thread), but the exact interleaving of the caller's `onPause`/`onStop`/`onDestroy` with the target's `onCreate`/`onStart`/`onResume` depends on the platform version and must be **recorded from Logcat** in journey J1, not assumed.

---

## 3. The lifecycle model

### 3.1 The full sequence

```
COLD START
  onCreate → onStart → onResume → [ RUNNING ]

USER LEAVES (home, other app)
  [ RUNNING ] → onPause → onStop
  … time passes …
  onRestart → onStart → onResume → [ RUNNING ]

PARTIALLY OBSCURED (dialog, translucent activity)
  [ RUNNING ] → onPause → … → onResume        (no onStop, no onRestart)

CONFIGURATION CHANGE (rotation, font scale, theme, locale)
  [ RUNNING ] → onPause → onStop → onSaveInstanceState → onDestroy
  onCreate(savedInstanceState) → onStart → onRestoreInstanceState → onResume → [ RUNNING ]

FINISH (back, or startActivity + finish)
  [ RUNNING ] → onPause → onStop → onDestroy
```

### 3.2 What each callback is for

| Callback | Framework intent | Typical work |
|---|---|---|
| `onCreate(Bundle)` | One-time construction | Inflate layout, bind views, read Intent/state, wire listeners |
| `onStart()` | Becoming visible | Start animations, register receivers |
| `onResume()` | Becoming interactive | Refresh data, re-enable sensors, re-apply state |
| `onPause()` | Losing focus (partially obscured) | Persist edits, pause camera/sensors, commit light state |
| `onStop()` | No longer visible | Release heavy resources, unregister listeners |
| `onRestart()` | Coming back from stopped | Re-arm what `onStop` released |
| `onDestroy()` | Instance ending | Cancel callbacks, dismiss dialogs, close cursors |
| `onSaveInstanceState(Bundle)` | Instance may be killed | Write transient UI state into the Bundle |
| `onRestoreInstanceState(Bundle)` | Recreated from a Bundle | Read that state back after `onStart` |
| `onNewIntent(Intent)` | Reused instance receives a new Intent | Only relevant with `singleTop`/`singleTask` — not used here |
| `onActivityResult(...)` | Child Activity returned a result | Handle SAF picker result (deprecated form) |
| `onBackPressed()` | Back gesture/button | Custom back routing (deprecated form) |
| `onRequestPermissionsResult(...)` | Runtime permission answer | Not used — the app uses SAF, no runtime permissions |
| `onConfigurationChanged(...)` | Handled config change | Not used — the app lets the system recreate |

---

## 4. Current implementation: the callback matrix

This is the central table of the document set. "Present" means an override exists **anywhere** in the class graph.

| Callback | Present? | Where | What it currently does |
|---|---|---|---|
| `onCreate(Bundle)` | ✅ | `BaseActivity.java:53-106`; `MainActivity.java:26-50` | Sets system-bar flags; opens prefs and the repository; validates role/email (redirects to sign-in if missing); resolves `currentPage`, `reportId`, `returnPage`; inflates the layout; applies the status-bar inset; fills header/bottom nav; calls `initView()`. `MainActivity` inflates the sign-in layout and wires the two-state form. |
| `onStart()` | ❌ | — | Default. No visibility-dependent work exists. |
| `onResume()` | ❌ | — | Default. **No screen ever refreshes on resume.** |
| `onPause()` | ❌ | — | Default. Nothing is committed here; every mutation is persisted immediately instead. |
| `onStop()` | ❌ | — | Default. No receivers, sensors or media to release. |
| `onRestart()` | ❌ | — | Default. |
| `onDestroy()` | ❌ | — | Default. Any open `AlertDialog`, `BottomSheetDialog` or `PopupWindow` is **not** dismissed. |
| `onSaveInstanceState(Bundle)` | ❌ | — | Default. No transient UI state is saved. |
| `onRestoreInstanceState(Bundle)` | ❌ | — | Default. |
| `onNewIntent(Intent)` | ❌ | — | Not needed while `launchMode` stays `standard`. |
| `onActivityResult(int,int,Intent)` | ✅ (deprecated) | `ReportSubmissionActivity.java:129-147`; `ReportDetailsActivity.java:351-372` | Takes a persistable read permission for the picked URI, then stores it in `selectedPhoto` / `selectedReport.completionPhotoUri` and updates the button/label. |
| `onBackPressed()` | ✅ (deprecated) | `BaseActivity.java:390-403` | Details → `returnPage`; any other non-dashboard page → dashboard; dashboard → `signOut()`; no session → `super`. |
| `onRequestPermissionsResult(...)` | ❌ | — | No runtime permissions requested (SAF document picker is used). |
| `onConfigurationChanged(...)` | ❌ | — | All configuration changes recreate the Activity. |

The `savedInstanceState` parameter is **never read** in either `onCreate` implementation. There is no `onSaveInstanceState`, no `ViewModel`, no `SavedStateHandle`. The only reason a recreated screen still shows the right report is that the report ID is in the Intent.

### 4.1 What is actually destroyed by each event

| Event | Activity instance | View state (text, spinners, scroll) | Fields (`selectedPhoto`, `selectedReport`) | Repository data | Intent extras |
|---|---|---|---|---|---|
| Home → app returns to foreground | kept | kept | kept | kept | kept |
| SAF picker opens | kept | kept | kept | kept | kept |
| Rotation / font scale / dark mode | **recreated** | **lost** | **lost** | kept (static) | kept |
| "Don't keep activities" | **recreated** | **lost** | **lost** | kept (static) | kept |
| Process death | **new process** | **lost** | **lost** | reloaded from prefs | kept |
| `navigate()` | finished | discarded | discarded | kept | new Intent |

This table is the reason `onSaveInstanceState` is the highest-value gap: three of the six rows lose user input that the user can see on screen.

---

## 5. Per-callback content plan

Each callback gets one page with the same structure (template in §10.1). This section defines what those sections must contain for *this* app.

### 5.1 `onCreate(Bundle savedInstanceState)`

**Contract.** Runs once per instance, after the Activity is attached and before it is visible. `savedInstanceState` is `null` on a cold start and non-null after a recreation. Do only construction here; anything that depends on window size or visibility belongs later.

**Must document.**

1. The guard: `BaseActivity` reads role/email from the Intent, falls back to `SharedPreferences` only when the extras are absent (`:69-78`), and redirects to `MainActivity` + `finish()` when neither exists (`:80-85`). Note that `onCreate` **continues to run after `finish()`** unless the method returns — it does return (`:84`), which is the correct pattern.
2. Layout selection: `getLayoutResourceId()` is polymorphic; `DashboardActivity` returns one of two layouts based on `isAdmin()`. Document the fact that layout choice is fixed for the life of the instance.
3. Inset application: top padding is set on `android.R.id.content`, not on the layout root (`:95-102`).
4. Header/nav population (`setupHeaderAndNavigation`, `setupBottomNavigation`) and then the subclass hook `initView()` — the template-method order matters because `initView` assumes the header exists.
5. Redirect-on-missing-data: `ReportDetailsActivity.initView()` navigates away and finishes when the report ID no longer resolves (`:42-47`).

**Test recipe.** Cold start from launcher; cold start of a non-exported screen via `adb am start` without extras (expect redirect); cold start with a bogus report ID (expect redirect to `returnPage`).

### 5.2 `onStart()` and `onResume()`

**Contract.** `onStart` = visible, `onResume` = interactive and on top. Both can be called many times per instance.

**Current state.** Neither is overridden. The documentation must say so explicitly and record the consequences:

* Returning from the SAF picker does **not** re-read the repository (not needed today — the picker changes nothing — but it would matter if a background sync were added).
* Returning from `MainActivity`-initiated sign-out does not reach these Activities (they were finished).
* If the app is backgrounded while an admin action dialog is open, the dialog is still attached when the user returns.

**Test recipe.** Home → recents → app; log the callback order. Because no data can change while this screen is alive (navigation replaces it, it does not stack), a missing refresh is not observable today — record that as a limitation to revisit if the navigation model changes.

### 5.3 `onPause()` and `onStop()`

**Contract.** `onPause` runs when the Activity loses foreground (another Activity, a dialog over it, the SAF picker, split-screen). `onStop` runs when it is fully hidden. `onPause` must be fast; the next screen does not start until it returns.

**Current state.** Not overridden, and that is a *design choice*: every mutation calls `repository.persistReports()` immediately, so there is nothing to flush. Document it as "write-through persistence, not pause-time persistence", and document the trade-off: each vote/action writes the whole JSON array synchronously on the main thread (`ReportRepository.persistReports`), so a burst of taps writes repeatedly.

**Test recipe.** Open the SAF picker and observe `onPause` then `onStop` in Logcat; open the preferences bottom sheet and observe `onPause` **not** being called (a dialog does not pause the Activity — useful teaching point).

### 5.4 `onDestroy()`

**Contract.** Not guaranteed (the process can be killed without it). Called after `onStop` when the instance is finishing or being recreated.

**Current state.** Not overridden. The documentation must list what *should* be released and isn't:

* three dialog types created in `BaseActivity` / `ReportDetailsActivity` and never dismissed;
* the `PopupWindow` anchored to a header view;
* no listeners registered, so nothing else to unregister.

**Risk to record.** Rotating with an open `AlertDialog` produces `WindowLeaked` and, on some OEM builds, a crash. This is the most likely lifecycle bug a marker will hit by accident.

**Test recipe.** Open the navigation dialog → rotate → check Logcat for `WindowLeaked`; repeat with the bottom sheet and the profile popup.

### 5.5 `onSaveInstanceState(Bundle)` / `onRestoreInstanceState(Bundle)`

**Contract.** The Bundle is for *transient view state*, not for durable data. Since API 28 it can be called after `onStop`. It does not survive an explicit `finish()`.

**Current state.** Neither method exists, so the documentation's job is to make the **gap concrete**. The pages must enumerate exactly what the user loses on rotation, per screen:

| Screen | Lost on rotation today |
|---|---|
| Sign-in | selected role, typed email/password, error message, which of the two forms was visible |
| My reports / All reports | search text, 3 spinner selections, date filter, scroll position |
| Admin dashboard | search text, 3 spinner selections, date filter, table scroll position |
| Hazard map | 4 spinner selections, selected pill, map state |
| Report submission | 3 spinner selections, description text (the most costly loss), chosen photo URI and button label |
| Report details | scroll position, generated summary text (persisted, so recoverable), open dialog |
| Settings / notifications / tasks / analytics / risk | scroll position, spinner selection (tasks) |

**Test recipe.** Enter text/filters → rotate → confirm loss. Then enable **Don't keep activities** and background/foreground → confirm the same loss. Two different restore paths exist and both must be recorded: returning via **recents** after a process kill delivers a `Bundle` (which this app leaves empty), while launching again from the **launcher** creates a fresh task with `onCreate(null)`.

### 5.6 `onActivityResult(int requestCode, int resultCode, Intent data)`

**Contract.** Deprecated since API 30 in favour of `ActivityResultLauncher`; still functional. Called on the Activity that started the child, after it returns, before `onResume`.

**Current state.** Two implementations, both for `Intent.ACTION_OPEN_DOCUMENT` (SAF):

* `ReportSubmissionActivity` — `REQUEST_REPORT_PHOTO` (1001): stores `selectedPhoto`, updates `btnChoosePhoto` text.
* `ReportDetailsActivity` — `REQUEST_COMPLETION_PHOTO` (1002): stores `completionPhotoUri`, appends a history entry, persists, re-renders.

**What to document.**

1. `takePersistableUriPermission` is called for **every** result, before the request code is checked (`:137` / `:359`) — harmless but wrong-shaped; a result from another request would attempt the call.
2. The completion photo URI survives process death because it is persisted on the report; the submission form's `selectedPhoto` survives neither rotation nor process death — it is only a field.
3. Both handlers null-check `binding` / `selectedReport` because recreation can deliver a result to an instance whose `onCreate` redirected away.
4. Migration target: `registerForActivityResult(new ActivityResultContracts.OpenDocument(), …)`, which requires `ComponentActivity`.

**Test recipe.** Pick a photo; rotate while the picker is open (rare but possible on low-memory devices); pick a photo, rotate the form, confirm the label resets.

### 5.7 `onBackPressed()`

**Contract.** Deprecated in API 33 (`OnBackPressedCallback` / `OnBackInvokedCallback` are the replacements). Called for the button and (subject to predictive-back behaviour) the gesture.

**Current state.** One override, four branches (`BaseActivity.java:390-403`):

```
details            → navigateTo(returnPage); finish()
non-dashboard      → navigateTo("dashboard"); finish()
dashboard          → signOut()            (clears prefs, starts MainActivity, finish)
no session         → super.onBackPressed()
```

**What to document.** A routing table (below) plus the interaction with `finish()`-based navigation: because every screen finishes itself when it navigates, Back never reveals a stale screen — it always constructs a fresh instance of the destination. That is unusual and worth stating as an explicit design decision.

| Current screen | Back goes to | Mechanism |
|---|---|---|
| Sign-in (`MainActivity`) | system default → leaves app | no override |
| Dashboard | sign-out → sign-in | `signOut()` |
| Notifications, reports, map, report, risk, settings, all-reports, tasks, analytics, home | dashboard | `navigateTo("dashboard")` |
| Report details | `returnPage` (the page that opened it) | `navigateTo(returnPage)` |

**Test recipe.** Open details from notifications → Back → expect notifications; from notifications → Back → expect dashboard; from dashboard → Back → expect sign-in and cleared session; verify predictive back on API 35+.

### 5.8 Callbacks that are deliberately absent

The document should have one page for "what we do not implement, and why", so reviewers do not read absence as oversight:

| Callback | Why absent |
|---|---|
| `onNewIntent` | No `singleTop`/`singleTask` launch modes |
| `onRequestPermissionsResult` | No dangerous permissions; SAF grants per-URI access |
| `onConfigurationChanged` | Full recreation is the chosen strategy; handling config manually would require re-laying out 26 layouts |
| `onPostCreate`, `onUserLeaveHint`, `onTrimMemory` | No use case yet |

---

## 6. Per-activity lifecycle cards

Each Activity gets a one-page card with these fields. The table below is the pre-filled skeleton; the authoring task is to verify and expand each row.

| Activity | Page key | Layout | Extras it depends on | State it owns (UI-only) | Exits |
|---|---|---|---|---|---|
| `MainActivity` | — | `screen_sign_in` | none (launcher) | selected role, email, password, remember flag, which form is visible | `navigateTo(dashboard)` + `finish()` |
| `DashboardActivity` | `dashboard` | `screen_dashboard` **or** `screen_admin_dashboard` | role, email | admin filters, search, table scroll | `navigate(...)` + `finish()` |
| `CampusHomeActivity` | `home` | `screen_dashboard` | role, email | none | `navigate(...)` |
| `HazardMapActivity` | `map` | `screen_hazard_map` | role, email | 4 spinners, selected pill | `navigate(...)`; details |
| `ReportSubmissionActivity` | `report` | `screen_report_submission` | role, email | 3 spinners, description, `selectedPhoto` | `navigateTo("reports")` + `finish()`; also `btn_confirm_existing` |
| `MyReportsActivity` | `reports` | `screen_my_reports` | role, email | 4 filters, search, table scroll | details |
| `AllReportsActivity` | `all-reports` | `screen_my_reports` (inherited) | role, email | same as parent | details |
| `RiskOverviewActivity` | `risk` | `screen_risk_overview` | role, email | scroll | details |
| `MaintenanceTasksActivity` | `tasks` | `screen_maintenance_tasks` | role, email | spinner filter | details |
| `AnalyticsActivity` | `analytics` | `screen_analytics` | role, email | scroll | none |
| `NotificationsActivity` | `notifications` | `screen_notifications` | role, email | scroll | details |
| `SettingsActivity` | `settings` | `screen_settings` | role, email | none | sign-out |
| `ReportDetailsActivity` | `report-details` | `screen_report_details` | role, email, **report id**, **return page** | scroll, open dialog | back-routing only |

The card template (§10.2) adds: "callbacks implemented", "callbacks deliberately absent", "state lost on rotation", "state lost on process death", "known defects", "test steps", "screenshots".

### 6.1 Priority order for authoring

| Priority | Activity | Why first |
|---|---|---|
| 1 | `BaseActivity` | Every screen inherits its `onCreate` and `onBackPressed`; documenting it once covers 12 screens |
| 2 | `MainActivity` | The only entry point; the only Activity that is not a `BaseActivity`; holds the credential check |
| 3 | `ReportDetailsActivity` | An action dialog, a SAF result, role-dependent visibility, a state machine in view visibility |
| 4 | `ReportSubmissionActivity` | The only screen with unsaved user input that is costly to lose |
| 5 | `DashboardActivity` | Two layouts in one class; the most-traversed screen |
| 6 | Remaining eight | Mostly template-derived; cards should be short |

---

## 7. Journeys to document end-to-end

A journey page is worth more than a per-callback page for reviewers, because it shows the callbacks *in order*. Seven journeys cover every path in the app.

### J1 — Cold start and sign-in

```
Launcher
  → MainActivity.onCreate(null)         ← savedInstanceState is null
      binding.inflate(screen_sign_in)
      role_selection VISIBLE / login_form GONE
  → onStart → onResume                  ← [RUNNING]
  tap "Continue as Campus User"
      role_selection GONE / login_form VISIBLE
      email pre-filled "student@um.edu.my"
  tap "Sign In"
      validate email + password "RainGuard123!"
      prefs: remember / role / email
      NavigationHelper.navigateTo(DashboardActivity, {...})
  → DashboardActivity.onCreate(extras)
      ← cross-activity ordering (caller pause vs. target create) varies by API level; log it
  → MainActivity.onPause → onStop → onDestroy   ← finish() requested after startActivity
  → DashboardActivity.onStart → onResume        ← [RUNNING]
```

Document: the interleaving of the finishing sign-in screen with the starting dashboard (recorded, not assumed); that no transition animation state is preserved.

### J2 — Navigate between workspace screens

`dashboard` → bell icon → `NotificationsActivity`: `navigate()` reads the role, checks the page is allowed, starts the target with five extras, finishes the caller. Back returns via `onBackPressed`, not via the stack.

### J3 — List → details → back

`MyReportsActivity` table row → `navigateTo("report-details", id, "reports")` → details reads `selectedReportId` and `returnPage`. Back re-creates `MyReportsActivity` with `page=reports`; filters are **reset** (documented consequence of `finish()`-based navigation).

### J4 — SAF photo picker

`startActivityForResult(ACTION_OPEN_DOCUMENT, 1001)` → `onPause` → `onStop` → picker → `onStart` → `onActivityResult` → `onResume`. Document the deprecated API, the persistable permission, and what happens if the process is killed while the picker is open (result is delivered to a recreated instance; the form text is gone).

### J5 — Rotation on the submission form

`onPause` → `onStop` → `onSaveInstanceState` (no-op) → `onDestroy` → new `onCreate` → `onStart` → `onRestoreInstanceState` (no-op) → `onResume`. Result: description text, spinner choices and chosen photo are gone. This journey is the core evidence for the `onSaveInstanceState` task.

### J6 — Process death / "Don't keep activities"

Enable Developer Options → *Don't keep activities*, background the app, return. New process, repository re-seeds from `SharedPreferences`, Intent extras are re-delivered, all UI state resets. Document the difference between "recreated from Bundle" and "relaunched from Intent".

### J7 — Back and sign-out

Dashboard Back → `signOut()` clears prefs, starts `MainActivity` with `CLEAR_TOP | NEW_TASK`, finishes. Then Back from sign-in leaves the app. Document that this is the **only** path that clears stored session prefs, and that the missing-session guard in `BaseActivity` (`:80-85`) can also land on sign-in — without clearing anything.

---

## 8. Lifecycle risk register

These are the findings the documentation must surface. They are recorded now so the authoring task does not have to rediscover them.

| ID | Risk | Evidence | Impact | Doc section that must state it |
|---|---|---|---|---|
| L-01 | No `onSaveInstanceState`; unsaved form state is lost on every configuration change | no override anywhere; `ReportSubmissionActivity` fields | High — user retypes a description | §5.5, J5 |
| L-02 | Open dialogs/sheets/popups are not dismissed in `onDestroy` | `showNavigationMenu`, `showActionDialog`, `showPreferencesBottomSheet`, `showProfileBubble` | High — `WindowLeaked`, occasional crash on rotate | §5.4 |
| L-03 | `onBackPressed()` deprecated; `targetSdk 37` makes predictive back the default | `BaseActivity.java:390`; `app/build.gradle.kts:14` | High — back routing may be bypassed by the gesture | §5.7, J7 |
| L-04 | `startActivityForResult` deprecated; no `ActivityResultLauncher` | 2 call sites | Medium — fails only when the activity is recreated mid-pick | §5.6, J4 |
| L-05 | Session is resolved in `onCreate` only; a role change requires a new Activity | `BaseActivity.java:66-91` | Medium — no way to hot-swap roles | §5.1 |
| L-06 | "Remember me" is written but never used to skip sign-in | `MainActivity.java:26-50` always shows role selection; the `BaseActivity.java:69-78` fallback only runs when a workspace Activity is launched without extras | Medium — dead behaviour, confusing to mark | §5.1, J1 |
| L-07 | Rotation resets every filter and search field on 5 screens | no saved state | Medium — annoying, not data-destructive | §5.5 |
| L-08 | Every mutation rewrites the entire report list as JSON on the main thread | `ReportRepository.persistReports` | Low/Medium — jank on large lists | §5.3 |
| L-09 | No `onPause`/`onResume` hook, so any future background sync has nowhere to land | none | Low today, design debt | §5.2 |
| L-10 | Edge-to-edge insets are applied once in `onCreate`; bottom inset never applied | `BaseActivity.java:97-101` | Medium — bottom bar under gesture bar on API 35+ | §5.1 (and UI doc F-04) |
| L-11 | `finish()`-based navigation makes Back history-free and destroys filter state | `navigate()` | Medium — behavioural quirk to document, not a bug | §2.5, J3 |
| L-12 | Repository is a static singleton; tests and process-death behaviour depend on it | `ReportRepository.getInstance` | Low — but must be documented for testability | §2.4 |

---

## 9. Execution plan

### 9.1 Deliverable structure

```
docs/
├─ README.md                            ← index (exists)
├─ layout-view-hierarchy.md             ← UI-01 (exists)
├─ activity-lifecycle-plan.md           ← this document (LC-01)
└─ lifecycle/
   ├─ 00-overview.md                    ← the §3+§4 content, expanded with diagrams
   ├─ callbacks/
   │  ├─ 01-oncreate.md
   │  ├─ 02-onstart.md  03-onresume.md  04-onpause.md  05-onstop.md
   │  ├─ 06-onrestart.md  07-ondestroy.md
   │  ├─ 08-onsaveinstancestate.md  09-onrestoreinstancestate.md
   │  ├─ 10-onactivityresult.md  11-onbackpressed.md
   │  └─ 12-not-implemented.md
   ├─ activities/
   │  ├─ base-activity.md  main-activity.md  dashboard-activity.md
   │  ├─ report-submission-activity.md  report-details-activity.md
   │  └─ … one per class (13 concrete + the base)
   ├─ journeys/
   │  ├─ j1-cold-start.md  j2-navigate.md  j3-list-to-details.md
   │  ├─ j4-photo-picker.md  j5-rotation.md  j6-process-death.md  j7-back-signout.md
   └─ appendices/
      ├─ device-test-matrix.md
      └─ glossary.md
```

### 9.2 Phases, effort and acceptance criteria

| Phase | Output | Effort | Acceptance criteria |
|---|---|---|---|
| **P0 — Set-up** | Folder tree, templates (§10), diagram style, glossary stub | 0.5 day | Templates committed; a sample page exists for `onCreate`; naming conventions agreed |
| **P1 — Callback reference** | 11 callback pages + "not implemented" page | 1.5 days | Each page follows the §10.1 template; every claim cites `file:line`; each has a state-loss table and a test recipe |
| **P2 — Activity cards** | 13 cards | 1 day | Every card lists extras read, UI state owned, callbacks present/absent, state-lost tables, test steps |
| **P3 — Journeys** | 7 journey pages + screenshots | 1 day | Each journey shows the callback order, the observed UI before/after, and links to the relevant callback pages |
| **P4 — Verification & review** | Device matrix results, cross-links, index update | 0.5 day | Runs on 2 API levels + 1 OEM; all links resolve; a second team member has reviewed; no `TODO` left |

Total: **~4.5 person-days**, parallelisable across 3–4 members (P1 pages are independent; P2 cards map 2–3 per person).

### 9.3 Suggested ownership split (4 members)

| Member | Owns |
|---|---|
| A | P0, `00-overview.md`, `onCreate`, `onStart`, `onResume`, `BaseActivity` + `DashboardActivity` cards |
| B | `onPause`, `onStop`, `onRestart`, `onDestroy`, dialogs risk, `MainActivity` + `CampusHomeActivity` cards |
| C | `onSaveInstanceState`, `onRestoreInstanceState`, rotation journey, submission + details cards |
| D | `onActivityResult`, `onBackPressed`, journeys J2/J3/J7, remaining 8 cards, device matrix |

### 9.4 Definition of Done (per page)

1. Opens with a one-paragraph contract (what the callback guarantees).
2. States what **this app** does — or explicitly says "not overridden, framework default".
3. Every behavioural claim points at `path:line`.
4. Includes at least one diagram or ordered sequence.
5. Includes a reproducible test recipe (adb command or exact taps).
6. Distinguishes **observed** behaviour from **recommended** behaviour.
7. Linked from `docs/README.md` and from its parent page.
8. Reviewed by someone who did not write it.

### 9.5 Milestones

| Milestone | Definition | Target |
|---|---|---|
| M1 | Overview + callback matrix published | end of P1 |
| M2 | All 13 cards published | end of P2 |
| M3 | Journeys + screenshots published | end of P3 |
| M4 | Reviewed and frozen for submission | end of P4 |

---

## 10. Templates

### 10.1 Callback page template

```markdown
# onCreate(Bundle savedInstanceState)

## Contract
…

## When it runs
… (diagram or ordered list)

## In this app
| Class | Line | What it does |
…

## State it reads / writes
…

## What is lost if this is wrong
…

## Known issues
- [L-0x] …

## Test recipe
1. `adb shell am start -n com.example.budibytes/…`
2. Rotate: `adb shell settings put system accelerometer_rotation 0` + `adb shell settings put system user_rotation 1`
3. Expect: …

## References
- developer.android.com/guide/components/activities/activity-lifecycle
```

### 10.2 Activity card template

| Field | Content |
|---|---|
| Class / page key / layout | |
| Extras consumed | |
| Initialisation order (`onCreate` → header → `initView`) | |
| Callbacks implemented | |
| Callbacks deliberately absent | |
| UI state owned (not persisted) | |
| State lost on rotation | |
| State lost on process death | |
| Exit paths | |
| Known defects / risks | |
| Test steps | |
| Screenshots | |

### 10.3 Journey template

1. **Goal** — one sentence.
2. **Preconditions** — signed-in role, starting screen.
3. **Sequence** — numbered steps annotated with the callback each step triggers.
4. **Observed UI** — before/after screenshots.
5. **State ledger** — what survives, what resets.
6. **Failure modes** — rotate here, kill there.
7. **Links** — callback pages, activity cards.

---

## 11. Appendix

### 11.1 Test recipes

```bash
# Launch the only exported screen
adb shell am start -n com.example.budibytes/com.um.rainguard.ui.auth.MainActivity

# Launch a non-exported screen with the full extras contract (role must be student or admin).
# adb shell normally holds START_ANY_ACTIVITY so this works; otherwise navigate through the UI.
adb shell am start -n com.example.budibytes/com.um.rainguard.ui.reportdetails.ReportDetailsActivity \
  --es screen_role admin --es screen_email admin@um.edu.my \
  --es screen_page report-details --es screen_return_page dashboard \
  --es screen_report_id RG-1042

# Rotation
adb shell settings put system accelerometer_rotation 0
adb shell settings put system user_rotation 1     # landscape
adb shell settings put system user_rotation 0     # portrait

# Font scale (Accessibility)
adb shell settings put system font_scale 1.3
adb shell settings put system font_scale 1.0

# Reproduce process death / recreation
adb shell am kill com.example.budibytes        # kill while backgrounded
adb shell am force-stop com.example.budibytes  # hard stop

# Watch lifecycle callbacks
adb logcat | findstr /i "ActivityManager rainguard"
```

In Android Studio: **Logcat** filtered on `ActivityTaskManager` and `rainguard`; **Layout Inspector** for the post-recreation tree; Developer Options → **Don't keep activities** for the recreation path.

### 11.2 Lifecycle vocabulary

| Term | Meaning in this app |
|---|---|
| **Recreation** | Same task, new Activity instance, same Intent, `savedInstanceState` possibly non-null |
| **Process death** | OS killed the process; task survives; new process re-delivers the Intent; static `ReportRepository` is re-seeded from prefs |
| **Configuration change** | Rotation, font scale, locale, theme, dark mode, keyboard — all recreate here |
| **Write-through persistence** | Every mutation calls `repository.persistReports()` immediately; `onPause` has nothing to flush |
| **Template method** | `BaseActivity.onCreate` calls the abstract `getLayoutResourceId` / `getInitialPage` / `initView` |
| **Finish-based navigation** | `startActivity(); finish();` — the back stack is not the history |

### 11.3 References

* Android Developers — *The activity lifecycle*: <https://developer.android.com/guide/components/activities/activity-lifecycle>
* Android Developers — *Save UI states* (`onSaveInstanceState`, `ViewModel`, `SavedStateHandle`): <https://developer.android.com/topic/libraries/architecture/saving-states>
* Android Developers — *Back navigation / predictive back*: <https://developer.android.com/guide/navigation/custom-back>
* Android Developers — *Activity Result APIs*: <https://developer.android.com/training/basics/intents/result>
* Android Developers — *Handle configuration changes*: <https://developer.android.com/guide/topics/resources/runtime-changes>
* `android/skills` — task-oriented agent skills (used as background context for Android conventions; no lifecycle reference skill is included in that repository, so this plan is grounded in the source above and the official docs).
