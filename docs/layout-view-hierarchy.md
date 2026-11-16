# UM RainGuard — View & ViewGroup Documentation

| | |
|---|---|
| **Document ID** | UI-01 — View layer reference |
| **Project** | WIA2007 Mobile App Dev — BudiBytes (`UM RainGuard`) |
| **Branch / commit** | `frontend` @ `1c1970c` ("Refined Frontend") |
| **Scope** | 26 files in `app/src/main/res/layout/`, `AndroidManifest.xml`, the 13 Activities that inflate them, and `MapSketchView` |
| **Method** | Static review of the source. Element/attribute counts were computed from the XML files themselves (see §15.2). Runtime behaviour comes from the Java sources, not from a device run. |
| **Out of scope** | Compose (not used), data layer, visual QA, animation |
| **Companion document** | [Activity lifecycle documentation plan](activity-lifecycle-plan.md) |

> This document describes **what exists today**. It does not propose rewritten layouts; recommendations in §12 are advisory.

---

## 1. Summary

### 1.1 The numbers

| Metric | Value | Comment |
|---|---|---|
| Screen layout files | 14 | 12 in use + 2 unused wrappers |
| Reusable layouts (header, items, overlays) | 12 | 1 header include, 7 item layouts, 4 overlays (2 dialogs, 1 bottom sheet, 1 popup) |
| Total layout files | 26 | 14 + 12 |
| Total XML elements | 538 | Including roots and `<include>` tags |
| Declared IDs | 251 | `@+id/...` |
| `LinearLayout` instances | 165 (31%) | The only layout manager used for structure |
| `TextView` instances | 239 (44%) | Most UI is text, not widgets |
| `ConstraintLayout` instances | **0** | Dependency is declared but never used |
| Max nesting depth | **6** (root = 0) | `screen_dashboard`, `screen_admin_dashboard`, `screen_my_reports` |
| Largest file | `screen_risk_overview.xml` | 64 elements, 498 lines |
| Hard-coded `android:text` values | 220 | Only `app_name` lives in `strings.xml` |
| Inline hex colours | 287 | vs. 15 named colours in `colors.xml` |
| `contentDescription` attributes | **1** | The sign-in banner image only |
| Layout `style=` references | 0 | No layout styles; every attribute is written per view |

### 1.2 Review scorecard (reviewer judgement)

| Dimension | Rating | Basis |
|---|---|---|
| Structural consistency | **Good** | 12 of 13 screens share one skeleton; IDs are stable and predictable |
| Reuse | **Fair** | Shared header and 7 item layouts exist, but the dashboard duplicates the header inline and three "stat card" groups are copy-pasted |
| Styling discipline | **Poor** | 287 inline colours; drawables used as generic backgrounds for buttons, inputs and spinners; no `dimens.xml` |
| Accessibility | **Poor** | 1 `contentDescription`, no `labelFor`, emoji used as icon glyphs, many touch targets < 48 dp |
| Internationalisation | **Poor** | 220 hard-coded strings; `strings.xml` holds only the app name |
| Performance | **Fair** | Shallow enough for these screens, but nested weights and full re-inflation on every filter change |
| System-bar handling | **Fragile** | Top inset only; `targetSdk 37` makes edge-to-edge mandatory |

### 1.3 Top findings

| ID | Sev. | Finding |
|---|---|---|
| F-01 | P1 | `screen_all_reports.xml` and `screen_campus_home.xml` are never inflated — each only wraps another layout. Their binding classes are generated but unused. |
| F-02 | P1 | `screen_dashboard.xml:12-106` is a hand-copied duplicate of `include_workspace_header.xml`; the other ten workspace layouts use the include. |
| F-03 | P1 | Accessibility: 1 `contentDescription`, 0 `labelFor`, emoji-only buttons, 32–38 dp tap targets. |
| F-04 | P1 | Edge-to-edge: only the status-bar inset is applied, and it is applied to `android.R.id.content`; the 64 dp bottom bar never receives a navigation-bar inset. |
| F-05 | P1 | Touch targets: 32–38 dp menu/bell/settings glyphs against the 48 dp minimum. |
| F-06 | P2 | No `ConstraintLayout` anywhere; 6-level nesting with nested `layout_weight` causes repeated measure passes. |

Full list in §12.

---

## 2. View and ViewGroup, as used in this app

### 2.1 Definitions

* A **`View`** is one rectangular thing that draws itself and can receive touch/key events. It has no children.
* A **`ViewGroup`** is a `View` that also **measures and positions children**. It decides each child's size (`onMeasure`) and position (`onLayout`).

Everything the user sees is a tree of these two roles. The tree always has exactly one root, which is attached to the Activity's window.

### 2.2 The two roles in this project

| Role | Classes used here | Count | What they do in this app |
|---|---|---|---|
| **ViewGroup** | `LinearLayout` | 165 | The workhorse: vertical page flow, horizontal rows, weighted columns, table rows |
| | `ScrollView` | 12 | One per in-use screen layout (11 workspace + sign-in); the 13 screens share 12 files |
| | `HorizontalScrollView` | 3 | Wide tables (reports, admin table) and the filter-pill strip |
| | `FrameLayout` | 4 | Stacking or simple hosts: sign-in banner, map host, and the two unused wrappers |
| | `<include>` | 12 | **Not a ViewGroup** — a compile-time copy of another layout file |
| **View** | `TextView` | 239 | Labels, values, section titles, pill badges, "buttons" (menu, bell, close, filter pills) |
| | `Button` | 29 | Primary/secondary actions |
| | `View` | 35 | Spacers and 1 dp / 4 dp dividers, not interactive |
| | `Spinner` | 16 | All filtering and selection |
| | `ProgressBar` | 9 | Risk-score bars in `screen_risk_overview` |
| | `EditText` | 8 | Search, date, description, dialog notes |
| | `Switch` | 3 | Notification preferences |
| | `CheckBox` | 1 | "Remember me" |
| | `ImageView` | 1 | Sign-in banner |
| | `MapSketchView` | 1 | Custom `View` that paints the hazard map in `onDraw` |

Deliberately absent: `RecyclerView`, `ListView`, `ConstraintLayout`, `RelativeLayout`, `CardView`, `Toolbar`, `CoordinatorLayout`, `NestedScrollView`, fragments, `BottomNavigationView`. Every list in the app is a `LinearLayout` filled by `addView()` at runtime.

### 2.3 The content root

`setContentView()` does not attach the layout to the window directly. It adds it to `android.R.id.content` — a `FrameLayout` owned by the `DecorView`:

```
DecorView (FrameLayout)
└─ LinearLayout (window decor: action bar slot — empty, NoActionBar theme)
   └─ FrameLayout  android.R.id.content        ← BaseActivity applies status-bar padding here
      └─ <your screen layout>                   ← the 26 files in this document
```

Both `BaseActivity.java:95-102` and `MainActivity.java:37-44` look up `android.R.id.content` and set top padding equal to the status-bar inset. That is why the custom header is not drawn under the status bar, and why no screen layout needs `fitsSystemWindows`.

### 2.4 What ViewBinding does — and does not do

`viewBinding = true` (`app/build.gradle.kts:28-30`) generates one class per layout (`ScreenDashboardBinding`, `ItemReportCardBinding`, …). A binding:

* holds a typed field per declared ID (`TextView workspaceTitle`) and a typed field per `<include>` with an ID (`IncludeWorkspaceHeaderBinding workspaceHeader`);
* calls `findViewById` **once** in `bind()`/`inflate()` and caches the result;
* does **not** create or own views, and does **not** replace `findViewById` for views inside code-built containers.

Because `BaseActivity` must choose the layout (`getLayoutResourceId()` depends on `isAdmin()`), subclasses cannot inflate in a field initialiser. They call `setContentView(id)` in the base class and then re-bind the already-attached root:

```java
// ReportDetailsActivity.java:49-52
View rootView = findViewById(R.id.main_root_container);
binding = ScreenReportDetailsBinding.bind(rootView);
```

`MainActivity` is the exception and uses the conventional `inflate()` + `setContentView(binding.getRoot())` pattern (`MainActivity.java:34-35`).

---

## 3. From XML to pixels

| # | Step | Where |
|---|---|---|
| 1 | Launcher intent resolves `.ui.auth.MainActivity` | `AndroidManifest.xml:7-15` |
| 2 | `ActivityThread` instantiates the Activity and calls `onCreate(null)` on cold start | framework |
| 3 | `MainActivity.onCreate` sets system-bar colours/flags, inflates `ScreenSignInBinding`, applies the top inset | `MainActivity.java:26-50` |
| 4 | Sign-in starts the next Activity with role/email/page extras | `MainActivity.java:95`, `NavigationHelper.java:83-94` |
| 5 | `BaseActivity.onCreate` validates extras (or falls back to `SharedPreferences`), then calls the abstract `getLayoutResourceId()` | `BaseActivity.java:66-93` |
| 6 | `setContentView()` inflates the XML with `LayoutInflater`, instantiates each tag through its 2- or 3-arg constructor, and attaches the root to `android.R.id.content` | framework |
| 7 | `BaseActivity` applies insets, fills the header texts from `NavigationHelper`, and builds the bottom bar | `BaseActivity.java:95-183` |
| 8 | Subclass `initView()` binds the root and populates dynamic containers | e.g. `DashboardActivity.java:40-51` |

Inflation is therefore **once per Activity instance**, in `onCreate`, on the main thread. There is no fragment re-inflation, no `<merge>` optimisation, and no view caching across navigations (each navigation starts a new Activity).

### 3.1 Where measure/layout/draw sit relative to the Activity lifecycle

A `View` has its own three-phase lifecycle, driven by the `ViewRootImpl` traversal rather than by Activity callbacks:

| View phase | What runs | When, relative to the Activity |
|---|---|---|
| Construction | `View`/`ViewGroup` constructors, attribute parsing from XML | `onCreate`, inside `setContentView` |
| Attach | `onAttachedToWindow`, traversal scheduled | `onCreate` → `onStart` |
| Measure | `onMeasure` (parent asks each child how big it wants to be) | first traversal after the window is added, before the first frame |
| Layout | `onLayout` (parent assigns each child's left/top/right/bottom) | same traversal as measure |
| Draw | `onDraw` (each view paints itself, parents draw children) | same traversal, then again whenever `invalidate()` is called |
| Re-measure | `requestLayout()` → measure + layout again | next frame — e.g. every `setText()`, `setVisibility()` or `addView()` in the filter code |

Two consequences worth knowing when reading the Java:

* `setVisibility(View.GONE)` in a filter method does not just hide pixels — it invalidates the parent's measurement, so the whole `page_content` subtree is re-measured on the next frame. Repeated calls inside a loop (as in `setupReportDetailsUI`) coalesce into one traversal, but the traversal is not free.
* `MapSketchView` never calls `requestLayout()`; `setMapReports()` calls `invalidate()` only, which is correct because the view's size does not depend on the data (it draws in normalised coordinates).

The companion lifecycle document covers what happens to this tree when the Activity is recreated — in short: it is thrown away and inflated again.

---

## 4. The screen template

### 4.1 Skeleton

Twelve of thirteen screens are the same 4-part column. Only `screen_sign_in.xml` (used by the launcher `MainActivity`) differs: it has no header and no bottom bar.

```
LinearLayout  @+id/main_root_container     vertical, match_parent × match_parent, bg = @color/background
│
├─ <include>  @+id/workspace_header        → include_workspace_header.xml  (header + disclaimer)
│
├─ ScrollView @+id/page_scroll             match_parent wide × 0dp high, layout_weight = 1, fillViewport = true
│  └─ LinearLayout @+id/page_content       vertical, match_parent × wrap_content, 16dp side / 4dp top / 24dp bottom padding
│     └─ … screen-specific blocks …
│
└─ LinearLayout @+id/bottom_navigation     horizontal, match_parent × 64dp, bg = @color/surface, gravity = center
```

Three properties make the vertical fill work and are worth understanding:

| Property | Why it is needed |
|---|---|
| `page_scroll` height `0dp` + `layout_weight="1"` | `0dp` tells `LinearLayout` to skip measuring the child's intrinsic height, so the weight alone decides the height and one measure pass is saved. With `wrap_content` the weight still applies, but the child is measured twice (content first, then the distributed delta), and a tall child can end up larger than the viewport. |
| `fillViewport="true"` | Stretches `page_content` to at least the viewport height, so short pages do not leave a gap above the bottom bar. |
| `bottom_navigation` fixed `64dp` | A fixed height gives the `ScrollView` a deterministic budget; the bar is not part of the scrolling content, so it never scrolls away. |

`bottom_navigation` is an **empty `LinearLayout` in XML**. `BaseActivity.setupBottomNavigation()` (`:168-183`) removes all children and adds up to five `TextView`s, each with `weight = 1` and `MATCH_PARENT` height. `screen_dashboard.xml` is the only file that also sets `android:visibility="gone"` on the bar (so the preview looks clean); `BaseActivity` sets it back to `VISIBLE`, so the end state is identical on every screen.

### 4.2 Measurement rules used in these layouts

| Rule | Where it appears | Consequence |
|---|---|---|
| `match_parent` for full-width blocks | most roots and cards | Simple, predictable; children cannot be wider than the parent |
| `wrap_content` for text | nearly every `TextView` | Height follows font scale, but `wrap_content` inside a `ScrollView` is required |
| `0dp` + `layout_weight` | `page_scroll`, header title block, table cards, button pairs | Correct use of weights for *width*; also used for one *height* (`page_scroll`) |
| Fixed `dp` widths | table columns 180/100/90/100/100 dp; 32–44 dp icon buttons | Keeps columns aligned but never adapts to content or font scale |
| Negative margin | `screen_dashboard.xml:201` `layout_marginTop="-14dp"` | Overlaps the weather card onto the hero card; a deliberate visual trick, fragile under large font scales |
| Fixed `dp` heights | 44/48 dp buttons, 64 dp nav bar, 260 dp map, 210 dp banner | Not `minHeight`; text clipped at 2× font scale |
| `layout_marginHorizontal` / `Start` / `End` | cards and pills | Mostly RTL-aware; a few places use `paddingEnd` with `tools:ignore="RtlSymmetry"` |

### 4.3 The shared header

`include_workspace_header.xml` — 11 elements, depth 3, 7 IDs, all consumed by `BaseActivity`:

```
LinearLayout (vertical, match_parent × wrap_content)
├─ LinearLayout (horizontal, bg surface, 14dp h / 10dp v padding, gravity center_vertical)
│  ├─ TextView @+id/workspace_menu          38×38dp, "☰", 22sp   → opens navigation dialog
│  ├─ LinearLayout (0dp width, weight 1, vertical, 8dp start padding)
│  │  ├─ TextView @+id/workspace_breadcrumb 11sp muted, e.g. "Universiti Malaya / Campus safety"
│  │  └─ TextView @+id/workspace_title      18sp bold, page title from NavigationHelper
│  └─ LinearLayout (horizontal, wrap_content)
│     ├─ TextView @+id/mock_data_badge      "Mock data", 11sp bold, #C2410C, runtime background
│     ├─ TextView @+id/workspace_notifications 32×32dp, "🔔", 14sp → navigate("notifications")
│     └─ TextView @+id/workspace_profile    "🛡 Campus User", runtime background → PopupWindow
└─ TextView @+id/demo_warning_banner        full width, 12sp muted, ⚠ demo disclaimer
```

The header is styled almost entirely **at runtime** by `BaseActivity.setupHeaderAndNavigation()` (`:118-166`) — breadcrumb text, title, badge colours, `GradientDrawable` backgrounds, click listeners. The XML only supplies size and typography.

`screen_dashboard.xml` contains an equivalent copy of this subtree inline (lines 12–106). Both copies declare the same 7 IDs. Because they live in different layouts and only one is inflated per screen, there is no ID collision — but there are now two files to keep in sync.

### 4.4 The sign-in screen is the exception

`screen_sign_in.xml` root is a `ScrollView` (no header, no bottom bar, no `main_root_container`). Its 39 elements form a marketing-style page:

```
ScrollView (fillViewport, bg background)
└─ LinearLayout (vertical, 20dp h / 16dp top / 28dp bottom padding)
   ├─ LinearLayout (horizontal)            shield badge + "UM RainGuard" + subtitle
   ├─ FrameLayout 210dp tall
   │  └─ ImageView  src=@drawable/campus_rain  ← 1024×768 JPEG in drawable-nodpi (see F-19)
   ├─ LinearLayout  bg #EBF3F5, 20dp padding   hero: "Stay Ahead of Rain…" + 2 paragraphs
   ├─ TextView  "CAMPUS SAFETY PORTAL" 12sp bold
   ├─ TextView  "Welcome to UM RainGuard" 24sp bold
   ├─ TextView  subtitle 14sp
   ├─ LinearLayout @+id/role_selection   ← visible by default
   │  ├─ LinearLayout  card: "UM Campus User" + Button @+id/choose_student
   │  └─ LinearLayout  card (12dp top margin): "Admin / Maintenance Team" + Button @+id/choose_admin
   ├─ LinearLayout @+id/login_form  visibility=gone
   │  ├─ TextView @+id/login_role_title
   │  ├─ TextView   demo-credentials notice (bg #EBF3F5)
   │  ├─ EditText @+id/email_input      48dp, textEmailAddress
   │  ├─ EditText @+id/password_input   48dp, textPassword
   │  ├─ CheckBox @+id/remember_me
   │  ├─ TextView @+id/login_error      gone, #DC2626
   │  ├─ Button   @+id/sign_in
   │  └─ Button   @+id/back_to_roles    transparent background
   └─ TextView   footer disclaimer 12sp
```

The screen is a **two-state form**: `role_selection` and `login_form` are swapped by `MainActivity` through `setVisibility()` (`MainActivity.java:52-74`). No second Activity, no fragment — just two sibling containers whose visibility toggles.

---

## 5. Layout catalogue

`Depth` counts nesting levels with the root at 0. `Elements` includes the root and any `<include>` tag but not the included file's contents.

### 5.1 Screens

| Layout | Inflated by | Root | Depth | Elements | IDs | Notes |
|---|---|---|---|---|---|---|
| `screen_sign_in.xml` | `MainActivity` | `ScrollView` | 5 | 39 | 11 | Only screen without the template; 2-state form; banner is a JPEG in `drawable-nodpi` |
| `screen_dashboard.xml` | `DashboardActivity` (student), `CampusHomeActivity` | `LinearLayout` | 6 | 55 | 29 | Header duplicated inline; negative-margin card; `tools:context` wrong package |
| `screen_admin_dashboard.xml` | `DashboardActivity` (admin) | `LinearLayout` | 6 | 59 | 27 | 6 stat cards in 3 rows; 3 spinners; wide `HorizontalScrollView` table |
| `screen_my_reports.xml` | `MyReportsActivity`, `AllReportsActivity` | `LinearLayout` | 6 | 27 | 15 | 4 filters + scrollable table; used by two Activities |
| `screen_all_reports.xml` | — (unused) | `FrameLayout` | 1 | 2 | 0 | Wraps `screen_my_reports`; dead |
| `screen_campus_home.xml` | — (unused) | `FrameLayout` | 1 | 2 | 0 | Wraps `screen_dashboard`; dead |
| `screen_report_details.xml` | `ReportDetailsActivity` | `LinearLayout` | 5 | 58 | 36 | Densest control surface: 10 buttons, visibility driven by role × status |
| `screen_hazard_map.xml` | `HazardMapActivity` | `LinearLayout` | 5 | 37 | 20 | 4 spinners, pill strip, custom `MapSketchView` |
| `screen_report_submission.xml` | `ReportSubmissionActivity` | `LinearLayout` | 4 | 25 | 16 | 3 spinners, multiline `EditText`, duplicate-report box |
| `screen_risk_overview.xml` | `RiskOverviewActivity` | `LinearLayout` | 5 | 64 | 6 | Largest file: 3 hand-written cards × 3 `ProgressBar`s |
| `screen_maintenance_tasks.xml` | `MaintenanceTasksActivity` | `LinearLayout` | 3 | 10 | 8 | 1 spinner + container; simplest workspace |
| `screen_analytics.xml` | `AnalyticsActivity` | `LinearLayout` | 4 | 23 | 8 | Half static (3 hotspot cards), half built in code (breakdown rows) |
| `screen_notifications.xml` | `NotificationsActivity` | `LinearLayout` | 5 | 15 | 7 | Static warning card + item container |
| `screen_settings.xml` | `SettingsActivity` | `LinearLayout` | 4 | 19 | 9 | 3 read-only rows + 2 buttons |

### 5.2 Reusable and overlay layouts

| Layout | Kind | Root | Depth | Elements | IDs | Used by |
|---|---|---|---|---|---|---|
| `include_workspace_header.xml` | partial | `LinearLayout` | 3 | 11 | 7 | 10 screens via `<include>`; dashboard inline |
| `item_report_card.xml` | list item | `LinearLayout` | 2 | 12 | 9 | Dashboard, campus home, hazard map |
| `item_report_table_row.xml` | table row | `LinearLayout` | 2 | 9 | 7 | My reports, all reports, admin dashboard |
| `item_risk_report_card.xml` | list item | `LinearLayout` | 3 | 10 | 5 | Risk overview |
| `item_task_card.xml` | list item | `LinearLayout` | 1 | 5 | 4 | Maintenance tasks |
| `item_notification_card.xml` | list item | `LinearLayout` | 3 | 6 | 3 | Notifications |
| `item_activity_entry.xml` | list item | `LinearLayout` | 2 | 5 | 2 | Admin dashboard, analytics |
| `item_history_entry.xml` | timeline row | `LinearLayout` | 2 | 5 | 2 | Report details |
| `dialog_navigation_menu.xml` | `AlertDialog` content | `LinearLayout` | 3 | 10 | 5 | `BaseActivity.showNavigationMenu()` |
| `dialog_admin_action.xml` | `AlertDialog` content | `LinearLayout` | 1 | 7 | 6 | `ReportDetailsActivity.showActionDialog()` |
| `bottom_sheet_preferences.xml` | `BottomSheetDialog` content | `LinearLayout` | 3 | 19 | 6 | `BaseActivity.showPreferencesBottomSheet()` |
| `popup_profile.xml` | `PopupWindow` content | `LinearLayout` | 1 | 4 | 3 | `BaseActivity.showProfileBubble()` |

---

## 6. Screen hierarchies in detail

### 6.1 Dashboard — student (`screen_dashboard.xml`)

The richest page and the only one with an overlap effect. IDs are shown because the Java code addresses every one of them.

```
LinearLayout @+id/main_root_container          vertical · background
├─ LinearLayout                                header (inline copy of the include)
│  ├─ TextView @+id/workspace_menu             38dp ☰
│  ├─ LinearLayout  0dp + weight 1
│  │  ├─ TextView @+id/workspace_breadcrumb
│  │  └─ TextView @+id/workspace_title
│  └─ LinearLayout
│     ├─ TextView @+id/mock_data_badge
│     ├─ TextView @+id/workspace_notifications 32dp 🔔
│     └─ TextView @+id/workspace_profile
├─ TextView @+id/demo_warning_banner
├─ ScrollView @+id/page_scroll                 0dp + weight 1
│  └─ LinearLayout @+id/page_content           padding 16/4/16/24
│     ├─ LinearLayout @+id/dash_hero_card      navy, 20dp h padding, bottom 24dp
│     │  └─ LinearLayout horizontal
│     │     ├─ LinearLayout 0dp + weight 1     subtitle + title
│     │     └─ LinearLayout                    settings (36dp) + bell (36dp) TextViews
│     ├─ LinearLayout @+id/dash_weather_card   marginTop = -14dp  ← overlap
│     │  ├─ LinearLayout horizontal            icon + 3 lines + chevron
│     │  └─ TextView @+id/dash_rain_alert      warning background
│     ├─ LinearLayout horizontal               3 stat cards, weight 1 each
│     │  ├─ LinearLayout @+id/stat_open_card        → TextView @+id/stat_open_count
│     │  ├─ LinearLayout @+id/stat_progress_card    → TextView @+id/stat_progress_count
│     │  └─ LinearLayout @+id/stat_resolved_card    → TextView @+id/stat_resolved_count
│     ├─ LinearLayout                          AI brief card (title + body + footnote)
│     ├─ LinearLayout @+id/dash_action_card    amber, horizontal: 📷 + title/subtitle + ›
│     ├─ LinearLayout horizontal               "Nearby reports" + @+id/dash_view_map_link
│     └─ LinearLayout @+id/nearby_reports_container   ← item_report_card added here
└─ LinearLayout @+id/bottom_navigation         64dp, visibility=gone in XML
```

Runtime shape: `nearby_reports_container` receives up to **3** `item_report_card` views (`DashboardActivity.java:70-78`). Each card root gets an `OnClickListener` to report details and, for non-resolved reports, two observation buttons.

### 6.2 Admin dashboard — delta only

Same skeleton and header. `page_content` adds:

```
TextView  "Campus drainage operations" 22sp bold
TextView  subtitle 13sp
TextView  role pill (secondary_button_background)
LinearLayout horizontal  ── 2 stat cards  (@+id/admin_stat_all_submitted_card / _awaiting_review_card)
LinearLayout horizontal  ── 2 stat cards  (_verified_active_card / _high_risk_card)
LinearLayout horizontal  ── 2 stat cards  (_in_progress_card / _resolved_card)
LinearLayout warning bg   "Pre-rain inspection priority"  (4dp View strip + text column)
TextView  "All drainage reports" 22sp
TextView  @+id/admin_reports_subtitle
View      10dp spacer
EditText  @+id/admin_search_input
Spinner   @+id/admin_status_spinner / admin_category_spinner / admin_risk_spinner
EditText  @+id/admin_date_input
TextView  @+id/admin_reports_empty_text  (gone)
HorizontalScrollView @+id/admin_table_scroll
└─ LinearLayout vertical, wrap_content
   ├─ LinearLayout horizontal  header row: 5 TextViews at 180/100/90/100/100 dp
   └─ LinearLayout @+id/admin_table_body    ← item_report_table_row added here
LinearLayout horizontal   scroll-hint strip (◄ ▬ ►)
View      18dp spacer
TextView  "Recent activity"
LinearLayout @+id/admin_recent_activity_container  ← item_activity_entry added here
```

The fixed column widths (180/100/90/100/100 dp = 570 dp) exceed the width of most phones in portrait, so the table scrolls horizontally on typical devices. The `◄ ▬ ►` strip is a static hint, not a scrollbar.

### 6.3 Report list / table — `screen_my_reports.xml`

The table is the reusable pattern for all reports screens:

```
HorizontalScrollView @+id/reports_table_scroll   scrollbars=none
└─ LinearLayout vertical (wrap_content)          bg card_background
   ├─ LinearLayout horizontal (wrap_content)     bg #EBF3F5, 12dp h / 10dp v
   │  ├─ TextView 180dp  "Report / Location"
   │  ├─ TextView 100dp  "Category"
   │  ├─ TextView  90dp  "Risk"
   │  ├─ TextView 100dp  "Status"
   │  └─ TextView 100dp  "Action"
   └─ LinearLayout @+id/reports_table_body vertical
      └─ item_report_table_row.xml × N
         ├─ LinearLayout 180dp vertical  → @+id/row_report_id / _place / _date
         ├─ TextView 100dp @+id/row_report_category
         ├─ TextView  90dp @+id/row_report_risk
         ├─ TextView 100dp @+id/row_report_status
         └─ Button   100dp @+id/row_btn_view_details
```

Two independent `wrap_content` widths are what make header and body align: both columns are pinned to the same `dp`, and the outer container is `wrap_content` so it can grow beyond the screen.

`AllReportsActivity` reuses this layout with `isManagement = true` (`AllReportsActivity.java:12-15`), which only changes headings and the repository query — the view tree is identical. That is why `screen_all_reports.xml` is redundant.

### 6.4 Report details — `screen_report_details.xml`

The only screen where UI is a function of **role + status**. Visibility switches drive it:

```
page_content
├─ TextView @+id/detail_header_info                 "RG-1042 · 2026-10-24, 16:12 MYT"
├─ TextView @+id/detail_place_title                 22sp bold
├─ LinearLayout horizontal                          @+id/detail_risk_tag + @+id/detail_status_pill
├─ View 1dp divider
├─ TextView @+id/detail_original_category           "ORIGINAL REPORT · CLOGGED"
├─ TextView @+id/detail_description
├─ TextView @+id/detail_no_photo_text
├─ LinearLayout @+id/detail_photo_container         ← ImageViews added in code
├─ View 1dp divider
├─ LinearLayout @+id/detail_safety_advice_card      warning bg + 4dp strip
├─ View 1dp divider
├─ LinearLayout @+id/detail_maintenance_container   gone → team + task pill
├─ TextView  "Report history & updates"
├─ LinearLayout @+id/detail_history_container       ← item_history_entry × N
├─ LinearLayout @+id/detail_community_section       student only
│  └─ Button @+id/btn_detail_still_there · @+id/btn_detail_cleared
└─ LinearLayout @+id/detail_admin_section           gone / admin only
   ├─ LinearLayout @+id/detail_admin_summary_card   AI summary + @+id/btn_generate_summary
   ├─ LinearLayout @+id/detail_duplicate_box        gone unless duplicates found
   │  └─ LinearLayout @+id/detail_duplicates_container  ← TextViews in code
   ├─ TextView "Administrator actions" 17sp bold
   ├─ Button @+id/btn_admin_under_review    36dp  gone
   ├─ Button @+id/btn_admin_verify          48dp  gone
   ├─ Button @+id/btn_admin_reject          36dp  gone
   ├─ Button @+id/btn_admin_assign          48dp  gone
   ├─ Button @+id/btn_admin_update_progress 36dp  gone
   ├─ Button @+id/btn_admin_complete        48dp  gone
   ├─ Button @+id/btn_admin_add_photo       36dp  gone
   └─ TextView @+id/detail_closed_text      gone
```

All seven action buttons ship `visibility="gone"` and are revealed in role/status-specific combinations by `setupAdminActionButtons()` (`ReportDetailsActivity.java:203-243`). This is a **state machine expressed in view visibility** — the closest thing to a screen state diagram in the codebase, and the best candidate for the screen-state section of the companion lifecycle document.

### 6.5 Hazard map

```
page_content
├─ LinearLayout horizontal          2×2 filter grid (each cell 0dp + weight 1)
│  ├─ "Location" + Spinner @+id/map_zone_spinner     │ "Category" + @+id/map_category_spinner
│  └─ "Severity" + Spinner @+id/map_risk_spinner     │ "Status"   + @+id/map_status_spinner
├─ TextView "CAMPUS DRAINAGE" 11sp + TextView "Hotspot map" 22sp
├─ View 10dp
├─ HorizontalScrollView @+id/map_pills_scroll
│  └─ LinearLayout @+id/map_pills_container horizontal
│     └─ 5 TextViews: @+id/pill_all, pill_high_risk, pill_stagnant, pill_clogged, pill_resolved
├─ LinearLayout scroll-hint strip (◄ ▬ ►)
├─ FrameLayout @+id/map_view_container 260dp
│  └─ MapSketchView @+id/map_sketch_view match_parent × match_parent
├─ TextView @+id/map_empty_text (gone)
└─ LinearLayout @+id/map_results_container  ← item_report_card × N (action row hidden)
```

`MapSketchView` is a hand-drawn `View`: it paints a rounded border, two cubic road paths, up to **5** pins (fixed normalised coordinates in `MapSketchView.java:83-85`), a tooltip and a legend, all in `onDraw`. It has no `onMeasure`, no padding handling, and no accessibility node — filtered results beyond five still appear in the list but never on the canvas.

### 6.6 Report submission

```
page_content
├─ TextView "Report a drainage issue" 22sp
├─ View 10dp
├─ TextView "Campus location" + Spinner @+id/place_spinner
├─ TextView "Hazard category" + Spinner @+id/category_spinner
├─ TextView "Observed severity" + Spinner @+id/risk_spinner
├─ LinearLayout @+id/active_report_box   warning bg
│  ├─ TextView @+id/active_report_title
│  ├─ TextView @+id/active_report_subtitle
│  ├─ Button   @+id/btn_confirm_existing
│  └─ Button   @+id/btn_continue_new
├─ TextView "Description" + EditText @+id/description_input  minLines=4
├─ TextView "Report photo (optional)"
├─ Button   @+id/btn_choose_photo  start|center, 44dp
└─ Button   @+id/btn_submit_report
```

Label + control pairs are separate siblings in a vertical `LinearLayout`; there is no `labelFor` association and no wrapper group, which is why accessibility services cannot connect the label to its `Spinner`.

### 6.7 Risk overview

`page_content` is three **hand-written** cards (Faculty of Engineering, KK8 walkway, Main Library), each:

```
LinearLayout card (card_background, 15dp)
├─ TextView place 16sp bold
├─ TextView "Demo risk score NN/100" 14sp bold
├─ View 4dp
├─ ( ×3 ) LinearLayout horizontal [label 0dp+weight 1, value]
│        + ProgressBar 6dp, max 100, progressTint #1B4965
├─ View 4dp
└─ TextView advice 12sp
```

then `risk_active_reports_container` receives `item_risk_report_card` views, sorted High→Low and numbered by rank. The three static cards are near-identical ~140-line blocks; only the numbers, place names and advice strings differ.

### 6.8 Tasks, notifications, settings, analytics

| Screen | Structure | Dynamic part |
|---|---|---|
| `screen_maintenance_tasks` | Title, subtitle, filter `Spinner`, empty `TextView`, container | `item_task_card` per task |
| `screen_notifications` | Title + Preferences `Button`, static warning card, container | `item_notification_card` per report |
| `screen_settings` | Title, divider, three `TextView` label/value rows, 2 buttons | Values set in code |
| `screen_analytics` | Title, subtitle, breakdown heading + container, spacer, hotspot heading + 3 static cards, tasks summary, activity heading + container | 6 × (row + progress bar) and `item_activity_entry` list |

`screen_analytics` is the only layout whose visible structure is **half XML and half Java**: the six status-breakdown rows and their `ProgressBar`s do not exist in XML at all (`AnalyticsActivity.java:52-91`).

---

## 7. Item layouts — the ID contract

Every item layout is inflated with `inflate(inflater, parent, false)` and then mutated field-by-field. The IDs below are therefore a hard contract with the Java code; renaming one breaks compilation of the binding, not just the UI.

| Layout | Root params | IDs consumed | Consumer |
|---|---|---|---|
| `item_report_card` | `match_parent × wrap_content`, 12dp bottom margin, card bg | `report_dot`, `report_place`, `report_time`, `report_subtitle`, `report_status_pill`, `report_confirmed_text`, `report_action_row`, `btn_still_there`, `btn_cleared` | Dashboard, CampusHome, HazardMap |
| `item_report_table_row` | `wrap_content × wrap_content`, 12dp padding | `row_report_id`, `row_report_place`, `row_report_date`, `row_report_category`, `row_report_risk`, `row_report_status`, `row_btn_view_details` | MyReports, AllReports, AdminDashboard |
| `item_risk_report_card` | `match_parent × wrap_content`, 12dp bottom margin | `risk_rank_num`, `risk_item_place`, `risk_item_subtitle`, `risk_item_tag`, `risk_btn_view_details` | RiskOverview |
| `item_task_card` | `match_parent × wrap_content` | `task_title`, `task_team`, `task_status`, `task_instructions` | MaintenanceTasks |
| `item_notification_card` | `match_parent × wrap_content` | `notif_item_title`, `notif_item_message`, `notif_btn_view_details` | Notifications |
| `item_activity_entry` | `match_parent × wrap_content`, 4dp vertical padding | `activity_text`, `activity_date` | AdminDashboard, Analytics |
| `item_history_entry` | `match_parent × wrap_content` | `history_text`, `history_date` | ReportDetails |

Note that `item_report_card`'s action row is **always inflated**; screens that do not want community actions set `report_action_row` to `GONE` rather than using a variant layout (HazardMap does this at `HazardMapActivity.java:166`).

---

## 8. Overlays

The app has no custom `DialogFragment` or `BottomSheetDialogFragment`. Every overlay is built imperatively from an Activity method:

| Overlay | Type | Layout | Built in | Lifetime risk |
|---|---|---|---|---|
| Navigation menu | `android.app.AlertDialog` | `dialog_navigation_menu` | `BaseActivity.showNavigationMenu()` | Not dismissed in `onDestroy` |
| Admin action | `android.app.AlertDialog` | `dialog_admin_action` | `ReportDetailsActivity.showActionDialog()` | Same; dialog holds Activity context |
| Preferences | `com.google.android.material.bottomsheet.BottomSheetDialog` | `bottom_sheet_preferences` | `BaseActivity.showPreferencesBottomSheet()` | Same; the sheet is a `Dialog` |
| Profile | `android.widget.PopupWindow` | `popup_profile` | `BaseActivity.showProfileBubble()` | Anchored to a header view that may be detached on rotation |

`popup_profile` is the only layout with a hard-coded root width (`240dp`) and its own `elevation`. The popup is positioned with `showAsDropDown(anchor, -120dp, 4dp)` — a magic offset that assumes a right-aligned badge.

`bottom_sheet_preferences` declares `@+id/zones_flex_container` as a plain vertical `LinearLayout`. `BaseActivity` implements a **manual flex layout** inside it: 14 zone names are split into rows with an estimated width of `zone.length() * 8 + 24` dp against a 280 dp line budget (`BaseActivity.java:306-347`). Any font-scale or localisation change breaks the estimate.

---

## 9. Views created in Java (not in any XML)

A significant part of the interactive surface never appears in a layout file: every bottom-navigation tab, the navigation-dialog rows, the zone pills, the analytics breakdown rows and the photo/duplicate views are all constructed at runtime. This matters when you review "the layouts" — the XML is not the whole UI.

| Created in | What | Container | Layout params |
|---|---|---|---|
| `BaseActivity.addNavItem()` | 1 `TextView` per bottom-nav tab (5 max), glyph + `\n` + label, 10sp | `bottom_navigation` | `0 × MATCH_PARENT`, weight 1 |
| `BaseActivity.showNavigationMenu()` | 1 `TextView` row per nav entry, 15sp, runtime background | `nav_dialog_items_container` | `MATCH_PARENT × WRAP_CONTENT`, 4dp bottom margin |
| `BaseActivity.showPreferencesBottomSheet()` | 14 zone-pill `TextView`s inside row `LinearLayout`s | `zones_flex_container` | `WRAP_CONTENT × WRAP_CONTENT`, 6dp end/bottom margins |
| `AnalyticsActivity.setupStatusBreakdown()` | 6 × (row `LinearLayout` + label + count + `ProgressBar` 7dp) | `analytics_status_container` | `MATCH_PARENT × WRAP_CONTENT`, progress `MATCH_PARENT × 7dp` |
| `AnalyticsActivity.setupRecentActivity()` | empty-state `TextView` when no history | `analytics_activity_container` | default |
| `ReportDetailsActivity.addPhotoToContainer()` | `ImageView` (`maxHeight` 280dp, `CENTER_INSIDE`) or error `TextView` | `detail_photo_container` | default |
| `ReportDetailsActivity.setupDuplicateSuggestions()` | one `TextView` per duplicate | `detail_duplicates_container` | default |

Because these use `new TextView(this)` with `android.app.Activity` as the context, they inherit the Activity theme but **none** of the XML styling conventions (text size is set in `sp` via `setTextSize`, colours via `Constants`), and they are not visible to Layout Inspector's XML view — only to its live hierarchy.

---

## 10. Styling and resources

### 10.1 Colour sources (three parallel systems)

| System | Where | Examples |
|---|---|---|
| Named colours | `res/values/colors.xml` (15 entries) | `@color/background`, `@color/primary` |
| Java constants | `Constants.java` | `NAVY`, `INK`, `MUTED`, `BG`, `BORDER` (same RGB values as `colors.xml`) |
| Inline literals | 287 attributes + many `Color.rgb(...)` calls | `#0F172A`, `#64748B`, `#EBF3F5`, `#D97706` |

`colors.xml` values and `Constants` values are duplicates (`background` = `#F5F8FA` = `Constants.BG`), so a palette change requires editing both, plus the 287 inline literals.

### 10.2 Drawables

| File | Type | Applied to |
|---|---|---|
| `card_background.xml` | `<shape>` 12dp radius + 1dp border | 51 usages: cards, spinners, search/date inputs |
| `primary_button_background.xml` | `<selector>` navy / pressed-hover-focused darker | Primary buttons, hero cards |
| `secondary_button_background.xml` | `<selector>` white / muted | Secondary buttons, inputs, scroll-hint bar |
| `warning_background.xml` | light amber shape | Warning cards and disclaimer strips |
| `campus_rain.jpg` | JPEG, 1024×768, 188 KB, `drawable-nodpi` | Sign-in banner only |
| `ic_launcher_*.xml` / `.webp` | adaptive icon layers + raster fallbacks | Launcher |

Four shape drawables and one bitmap (51 + 14 + 28 + 5 + 1 references) serve the whole app, which is efficient — but the shapes are generic, so a `Button` with `android:background="@drawable/primary_button_background"` loses the platform ripple/state animator, and a `Spinner` with `card_background` loses its dropdown affordance entirely (no arrow is drawn).

### 10.3 Typography and spacing

There is no `dimens.xml` and no `styles.xml` layout style. The conventions are copy-pasted constants:

* **Type scale:** 10–11sp metadata, 12–13sp body, 14–15sp emphasis, 16–18sp card titles, 22–24sp page titles.
* **Spacing scale:** 2, 4, 6, 8, 10, 12, 14, 16, 18, 20, 24, 28 dp — applied as `padding*`/`layout_margin*` per element.
* **Corner radius:** 8dp (buttons), 12dp (cards), 16dp (hero), 20dp (pills, drawn in code).

Changing the type scale (e.g. for a 200 % font-size accessibility test) would require editing every one of these literals.

---

## 11. ViewBinding map

| Layout | Binding class | Where it is used |
|---|---|---|
| `screen_sign_in` | `ScreenSignInBinding` | `MainActivity` (inflate) |
| `screen_dashboard` | `ScreenDashboardBinding` | `DashboardActivity`, `CampusHomeActivity` (bind) |
| `screen_admin_dashboard` | `ScreenAdminDashboardBinding` | `DashboardActivity` (bind) |
| `screen_my_reports` | `ScreenMyReportsBinding` | `MyReportsActivity` (bind) |
| `screen_all_reports` | `ScreenAllReportsBinding` | **never referenced** |
| `screen_campus_home` | `ScreenCampusHomeBinding` | **never referenced** |
| `screen_report_details` | `ScreenReportDetailsBinding` | `ReportDetailsActivity` |
| `screen_hazard_map` | `ScreenHazardMapBinding` | `HazardMapActivity` |
| `screen_report_submission` | `ScreenReportSubmissionBinding` | `ReportSubmissionActivity` |
| `screen_risk_overview` | `ScreenRiskOverviewBinding` | `RiskOverviewActivity` |
| `screen_maintenance_tasks` | `ScreenMaintenanceTasksBinding` | `MaintenanceTasksActivity` |
| `screen_analytics` | `ScreenAnalyticsBinding` | `AnalyticsActivity` |
| `screen_notifications` | `ScreenNotificationsBinding` | `NotificationsActivity` |
| `screen_settings` | `ScreenSettingsBinding` | `SettingsActivity` |
| `include_workspace_header` | `IncludeWorkspaceHeaderBinding` | Field `workspaceHeader` in the 10 binding classes that include it — **not read by any Java code** (header is driven by `findViewById`) |
| `item_*` (7) | `Item…Binding` | Inflated per row |
| `dialog_navigation_menu` | `DialogNavigationMenuBinding` | `BaseActivity` |
| `dialog_admin_action` | `DialogAdminActionBinding` | `ReportDetailsActivity` |
| `bottom_sheet_preferences` | `BottomSheetPreferencesBinding` | `BaseActivity` |
| `popup_profile` | `PopupProfileBinding` | `BaseActivity` |

The unused `IncludeWorkspaceHeaderBinding.workspaceHeader` field is a symptom of F-02: the header is bound but addressed through `findViewById`, because `BaseActivity` is generic and does not know the concrete binding type.

---

## 12. Findings and recommendations

### 12.1 P1 — correctness and maintenance

**F-01 Dead layouts.** `screen_all_reports.xml` and `screen_campus_home.xml` each contain only a `FrameLayout` wrapping another screen layout, and neither is inflated: `AllReportsActivity` inherits `R.layout.screen_my_reports`, `CampusHomeActivity` returns `R.layout.screen_dashboard`. Their binding classes are generated and never used. Delete both files (and the extra `FrameLayout` level they would add).

**F-02 Duplicated header.** `screen_dashboard.xml:12-106` re-implements `include_workspace_header.xml`. Ten workspace layouts include the partial; `screen_dashboard` (used by two Activities) copies it. Any header change (e.g. a new badge) must be made twice, and the two copies are already drifting (the dashboard's `tools:context` points at a non-existent class). Replace lines 12–106 with the same `<include>` used elsewhere.

**F-03 Accessibility.** Only one `contentDescription` exists (`screen_sign_in.xml:63`). All navigation affordances are emoji inside `TextView`s (`☰`, `🔔`, `🛡`, `✕`, `⚙`, `◄`, `►`, `●`) with no `contentDescription` and no `labelFor` on the 8 `EditText`s or 16 `Spinner`s. TalkBack will read the glyph or nothing. Add `contentDescription` to every clickable glyph, `labelFor` on label/control pairs, and consider `ImageButton` + vector icons.

**F-04 Edge-to-edge / insets.** The app targets SDK 37 (`app/build.gradle.kts:14`), where edge-to-edge is enforced: the window draws behind the status and navigation bars and `setStatusBarColor`/`setNavigationBarColor` have no effect. The app compensates only for the **status bar** and only on `android.R.id.content` (`BaseActivity.java:97-101`, `MainActivity.java:39-43`). Consequences: the 64 dp `bottom_navigation` sits under the gesture/nav bar, and the sign-in footer can be obscured. Apply bottom/left/right insets to the root layout, not just top padding to the content frame.

**F-05 Touch targets.** `workspace_menu` 38 dp, `workspace_notifications` 32 dp, hero `dash_settings_btn`/`dash_bell_btn` 36 dp, dialog close `✕` ≈ 35 dp, bottom-nav labels are 10 sp text in a 64 dp row (fine) but have no ripple. The Material minimum is 48 dp.

### 12.2 P2 — quality and performance

**F-06 All-`LinearLayout` structure.** 165 `LinearLayout`s, zero `ConstraintLayout`s despite the dependency (`app/build.gradle.kts:40`). Depth reaches 6 in three screens and nested weights appear in the header title block, the stat rows and the table cards. Weights force a second measure pass; three nested weighted rows per screen is measurable on low-end devices but not currently a user-visible problem.

**F-07 Copy-paste groups.** Three stat cards (`screen_dashboard`), six stat cards (`screen_admin_dashboard`), three risk cards with three `ProgressBar`s each (`screen_risk_overview`, 64 elements), and five filter pills (`screen_hazard_map`) are structurally identical blocks with different strings. These are ideal `<include>` + `<merge>` or custom-view candidates; today a styling change means 3–6 edits.

**F-08 Inline colours.** 287 literal colours and many `Color.rgb()` calls. Status colours are computed at runtime (`UIUtils.statusColor` / `statusBgColor`) while the XML hard-codes the same palette, so light/dark theming is impossible to centralise.

**F-09 Backgrounds on widgets.** `Button`, `EditText` and `Spinner` all use `@drawable/card_background` or `secondary_button_background` as `android:background`. This discards the platform state animator/ripple, removes the spinner arrow, and leaves the search `EditText` with no focus indicator. Use `backgroundTint` + a Material theme, or dedicated selectors with a ripple layer.

**F-10 Fixed table geometry.** Columns are hard-coded at 180/100/90/100/100 dp (`screen_my_reports.xml:141-179`, same in the admin table). At 200 % font scale the text wraps inside fixed widths and rows grow unevenly; the header and body stay aligned only because both use the same constants.

**F-11 Negative margin.** `screen_dashboard.xml:201` `layout_marginTop="-14dp"` creates the overlap effect. It works, but it is invisible to design tooling and breaks if the hero card's padding or the weather card's content height changes.

### 12.3 P3 — polish and tooling

| ID | Finding | Evidence |
|---|---|---|
| F-12 | `tools:context` points at a non-existent class `com.um.rainguard.DashboardActivity` (the real one is `…ui.dashboard.DashboardActivity`) | `screen_dashboard.xml:8` |
| F-13 | Date "pickers" are free-text `EditText`s with a 📅 in the hint and no validation | `screen_my_reports.xml:94-107`, `screen_admin_dashboard.xml:360-373` |
| F-14 | Demo credentials are baked into the layout: the email field ships `android:text="student@um.edu.my"` and the password is printed in a visible notice, so autofill competes with a hard-coded default | `screen_sign_in.xml:262`, `:276` |
| F-15 | 3 `Switch`es with `tools:ignore="UseSwitchCompatOrMaterialXml"` — platform `Switch`, not `SwitchMaterial`, in a platform-themed app that nevertheless uses Material Components | `bottom_sheet_preferences.xml:65-108` |
| F-16 | Dead theme resources: `Theme.BudiBytes` / `Base.Theme.BudiBytes` are defined in `values/themes.xml` and `values-night/themes.xml` but the manifest uses `@style/AppTheme`, a Light-only platform theme. Dark mode is inert, and `values-night` has no effect. | `AndroidManifest.xml:6`, `styles.xml:2` |
| F-17 | `MapSketchView` silently drops reports beyond the 5th (fixed pin array) and exposes no accessibility node | `MapSketchView.java:83-89` |
| F-18 | Text-to-structure ratio: 239 `TextView`s vs 29 `Button`s — most "buttons" are not buttons, so focus order, pressed states and screen-reader roles are wrong | all layouts |
| F-19 | Banner bitmap is a 1024×768 JPEG (188 KB) in `drawable-nodpi`, so it is never density-scaled and is decoded at full resolution on every sign-in; a WebP in a density bucket (or a vector/placeholder) would be cheaper | `res/drawable-nodpi/campus_rain.jpg`, `screen_sign_in.xml:60-65` |
| F-20 | Launcher icons exist in all density buckets plus an adaptive icon, but the manifest declares no `android:icon` / `android:roundIcon`, so the launcher shows the default Android icon | `AndroidManifest.xml:2-6`, `res/mipmap-*/ic_launcher.*` |

### 12.4 Suggested order of work (advisory)

1. F-01 + F-02 (delete dead layouts, de-duplicate header) — pure deletion, low risk.
2. F-04 (insets) + F-05 (tap targets) — user-visible on modern devices.
3. F-03 (content descriptions, `labelFor`) — cheap, high accessibility payoff.
4. F-07/F-08 (extract stat-card/risk-card includes, move colours to `colors.xml`) — reduces future diff size.
5. F-06/F-10 only if profiling or large-font testing demands it.

---

## 13. How to verify this document

| Check | Tool / command |
|---|---|
| View tree matches §6 | Android Studio **Layout Inspector** (live, not static preview — much of the tree is added at runtime) |
| Nesting depth and weights | Layout Inspector → *View Hierarchy* → enable *Show all nodes*; or `dumpsys gfxinfo` for measure counts |
| Resource resolution | `./gradlew :app:processDebugResources` (all referenced resources resolve at review time) |
| Accessibility | Layout Inspector → *Accessibility* tab; TalkBack pass over each screen; `Accessibility Scanner` |
| Tap targets | Enable *Show tap target* in Developer Options, or Layout Inspector bounds |
| Font-scale resilience | `adb shell settings put system font_scale 1.3` then relaunch; repeat at 2.0 |
| RTL | `adb shell settings put system debug.force_rtl 1` (API < 33) or Developer Options → *Force RTL layout direction* |
| Table alignment | Rotate + font scale 1.3, then compare header/body columns |

> **Caution when inspecting build output.** `app/build/intermediates/packaged_res/` contains resources that no longer exist in `src/` (for example `bg_card.xml`, `bg_pill_*.xml`, `screen_report_list.xml`), which means the build directory predates the current layouts. Run `./gradlew clean` before drawing conclusions from anything under `app/build/`; only `app/src/main/res/` is authoritative.

---

## 14. Open questions for the team

1. `CampusHomeActivity` renders `screen_dashboard` — the same layout as the student `DashboardActivity`. Is it a distinct screen in the product, or a leftover route that should share one Activity?
2. Should the admin dashboard and "All reports" screen stay separate, or is `screen_all_reports` intended to replace the shared `screen_my_reports` usage?
3. Is dark mode in scope? If not, `values-night/themes.xml` and the `Theme.BudiBytes` styles should be removed rather than left inert.
4. Is `targetSdk 37` intentional for a course project? It forces edge-to-edge handling that the current code has not implemented.

---

## 15. Appendix

### 15.1 Per-file element counts

| File | Depth | Elements | `LinearLayout` | `TextView` | `Button` | `View` | IDs |
|---|---|---|---|---|---|---|---|
| `bottom_sheet_preferences` | 3 | 19 | 7 | 5 | 1 | 3 | 6 |
| `dialog_admin_action` | 1 | 7 | 1 | 3 | 0 | 0 | 6 |
| `dialog_navigation_menu` | 3 | 10 | 4 | 3 | 1 | 2 | 5 |
| `include_workspace_header` | 3 | 11 | 4 | 7 | 0 | 0 | 7 |
| `item_activity_entry` | 2 | 5 | 2 | 3 | 0 | 0 | 2 |
| `item_history_entry` | 2 | 5 | 2 | 2 | 0 | 1 | 2 |
| `item_notification_card` | 3 | 6 | 3 | 2 | 1 | 0 | 3 |
| `item_report_card` | 2 | 12 | 4 | 6 | 2 | 0 | 9 |
| `item_report_table_row` | 2 | 9 | 2 | 6 | 1 | 0 | 7 |
| `item_risk_report_card` | 3 | 10 | 4 | 5 | 1 | 0 | 5 |
| `item_task_card` | 1 | 5 | 1 | 4 | 0 | 0 | 4 |
| `popup_profile` | 1 | 4 | 1 | 2 | 1 | 0 | 3 |
| `screen_admin_dashboard` | 6 | 59 | 19 | 28 | 0 | 4 | 27 |
| `screen_all_reports` | 1 | 2 | 0 | 0 | 0 | 0 | 0 |
| `screen_analytics` | 4 | 23 | 8 | 12 | 0 | 1 | 8 |
| `screen_campus_home` | 1 | 2 | 0 | 0 | 0 | 0 | 0 |
| `screen_dashboard` | 6 | 55 | 22 | 32 | 0 | 0 | 29 |
| `screen_hazard_map` | 5 | 37 | 12 | 14 | 0 | 2 | 20 |
| `screen_maintenance_tasks` | 3 | 10 | 4 | 3 | 0 | 0 | 8 |
| `screen_my_reports` | 6 | 27 | 7 | 10 | 0 | 2 | 15 |
| `screen_notifications` | 5 | 15 | 7 | 3 | 1 | 2 | 7 |
| `screen_report_details` | 5 | 58 | 15 | 26 | 10 | 5 | 36 |
| `screen_report_submission` | 4 | 25 | 4 | 8 | 4 | 3 | 16 |
| `screen_risk_overview` | 5 | 64 | 16 | 29 | 0 | 8 | 6 |
| `screen_settings` | 4 | 19 | 6 | 7 | 2 | 2 | 9 |
| `screen_sign_in` | 5 | 39 | 10 | 19 | 4 | 0 | 11 |
| **Total** | | **538** | **165** | **239** | **29** | **35** | **251** |

### 15.2 Regenerating the inventory

The counts above were produced from the XML itself. To refresh them after layout changes:

```powershell
# Depth, element count and ID count per layout (root depth = 0)
Get-ChildItem app\src\main\res\layout\*.xml | ForEach-Object {
  [xml]$x = Get-Content -Raw $_.FullName
  $nodes = $x.SelectNodes('//*')
  $depth = ($nodes | ForEach-Object { $_.Ancestors().Count } | Measure-Object -Maximum).Maximum
  $ids   = ([regex]::Matches((Get-Content -Raw $_.FullName), 'android:id="@\+id/')).Count
  "{0,-32} depth={1} elements={2} ids={3}" -f $_.Name, $depth, $nodes.Count, $ids
}
```

For the element-type totals, replace the counter with a hashtable keyed on `$n.LocalName`.

### 15.3 References

* Android Developers — *Layouts*, *ViewGroup*, *LinearLayout*, *ScrollView*, *ViewBinding*: <https://developer.android.com/develop/ui/views/layout/declaring-layout>
* Android Developers — *Support different screen sizes* / *Window insets*: <https://developer.android.com/develop/ui/views/layout/insets>
* Material Design — *Accessibility* (48 dp touch targets, contrast): <https://m3.material.io/foundations/accessible-design>
* `android/skills` (the repository shared with the task) is a set of task-oriented agent skills organised by domain (`jetpack-compose`, `navigation`, `performance`, `build-system`, …); this document is therefore derived directly from the source tree above rather than from a skill.
