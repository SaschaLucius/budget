## Plan: Finanz Android App

Minimal, clean Android finance tracker. **Kotlin + Jetpack Compose**, local **Room** database, **DataStore** for settings, Compose Navigation for two screens. No Hilt — manual DI via an Application class keeps it lean.

---

### Phase 1 — Project Scaffold

1. Initialize Android project at `/Users/slucius/Repositories/budget/` with package `com.lukulent.finanzapp`, minSdk 26 (Android 8.0 — supports `java.time.LocalDate` natively)
2. Set up `build.gradle.kts` (app) with dependencies:
   - Compose BOM (latest stable), `compose-ui`, `material3`, `compose-navigation`
   - Room + KSP plugin
   - DataStore Preferences
3. Set up `settings.gradle.kts` and project-level `build.gradle.kts`
4. Create `FinanzApp : Application` class (provides DB and repository singletons)
5. Create `AppNavigation.kt` — NavHost with two routes: `entry` and `statistics`

---

### Phase 2 — Data Layer

6. `Transaction.kt` — Room `@Entity`:
   - `id: Long` (autoGenerate), `amount: Double` (always positive), `isExpense: Boolean`, `subject: String?`, `date: LocalDate`
7. `MonthStatus.kt` — Room `@Entity(primaryKeys = ["year","month"])`:
   - `year: Int`, `month: Int`, `isDone: Boolean`
8. `Converters.kt` — TypeConverter: `LocalDate` ↔ `Long` (epoch day)
9. `TransactionDao.kt`:
   - `insert(Transaction)`, `delete(Transaction)`, `queryByDateRange(from, to): Flow<List<Transaction>>`
10. `MonthStatusDao.kt`:
    - `upsert(MonthStatus)`, `getByYearMonth(year, month): Flow<MonthStatus?>`
11. `AppDatabase.kt` — Room DB version 1, entities `[Transaction, MonthStatus]`, TypeConverters
12. `TransactionRepository.kt` — wraps both DAOs, exposes suspend/Flow functions to ViewModels

---

### Phase 3 — Settings

13. `SettingsDataStore.kt` — DataStore Preferences with single key `close_on_entry: Boolean` (default `false`). Exposes `Flow<Boolean>` and `suspend fun setCloseOnEntry(Boolean)`.

---

### Phase 4 — Entry Screen _(parallel with Phase 5)_

14. `EntryViewModel.kt` — holds `amount: String`, `subject: String`, `date: LocalDate` (default today), injected repository + settings
    - `fun save(isExpense: Boolean)`: validates amount non-empty → persists Transaction → emits `closeApp: Boolean` event (reads setting)
15. `EntryScreen.kt` (Composable):
    - Top bar: app title + small gear icon → opens Settings bottom sheet
    - Large numeric `OutlinedTextField` for amount (keyboardType = Number)
    - Secondary `OutlinedTextField` for subject (optional)
    - `DatePickerDialog`-backed date field showing date only (no time)
    - Two full-width large buttons: **"+"** (income) and **"−"** (expense)
    - Small `IconButton` (chart icon) → navigate to statistics
    - Settings bottom sheet: single toggle "App bei Eingabe schließen"

---

### Phase 5 — Statistics Screen _(parallel with Phase 4)_

16. `Filter.kt` — sealed class:
    - `ThisMonth`, `LastMonth`, `LastTwoMonths`, `LastThreeMonths`, `DateRange(from, to)`
17. `StatisticsViewModel.kt`:
    - State: `selectedFilter: Filter` (default `ThisMonth`), `transactions: List<Transaction>`, `monthStatuses: Map<YearMonth, Boolean>`, `balance: Double`
    - `fun setFilter(Filter)` → recalculates date range → queries repository
    - `fun toggleMonthDone(year, month)` → upserts MonthStatus
    - `balance` = Σ `(if isExpense → -amount else +amount)` for visible transactions
18. `StatisticsScreen.kt` (Composable):
    - Top bar: "Statistik" title + back arrow → Entry
    - Filter button (chip showing current period label) → bottom sheet / dropdown with 5 options; DateRange option shows two `DatePickerDialog`s
    - `LazyColumn` grouping transactions by `YearMonth`:
      - **Month header row**: month name + year + done-toggle (`Switch` or checkbox). Toggle is per-month regardless of filter span
      - **Transaction rows**: date (short format), subject (if present), amount with "+" or "−" prefix but **no color**
    - Sticky bottom bar: "Gesamt: {balance}" — no color, plain text

---

### Phase 6 — Wire-up & Polish

19. Register `FinanzApp` in `AndroidManifest.xml`, set `MainActivity` as launcher
20. Connect `MainActivity` → `AppNavigation`
21. Handle back-press on Statistics → Entry (system back)
22. Implement close-on-entry: after successful save in `EntryViewModel`, if setting is true → `finish()` the Activity via a `SideEffect`/`LaunchedEffect` in `EntryScreen`
23. Ensure `amount` field rejects invalid input (non-numeric, empty) with inline error text

---

### Relevant files (to create)

| Path                                                           | Purpose                      |
| -------------------------------------------------------------- | ---------------------------- |
| `app/src/main/java/com/lukulent/finanzapp/FinanzApp.kt`        | Application class, manual DI |
| `app/src/main/java/…/data/db/AppDatabase.kt`                   | Room DB                      |
| `app/src/main/java/…/data/db/TransactionDao.kt`                | DAO                          |
| `app/src/main/java/…/data/db/MonthStatusDao.kt`                | DAO                          |
| `app/src/main/java/…/data/db/Converters.kt`                    | TypeConverter                |
| `app/src/main/java/…/data/model/Transaction.kt`                | Entity                       |
| `app/src/main/java/…/data/model/MonthStatus.kt`                | Entity                       |
| `app/src/main/java/…/data/repository/TransactionRepository.kt` | Repository                   |
| `app/src/main/java/…/settings/SettingsDataStore.kt`            | DataStore                    |
| `app/src/main/java/…/ui/entry/EntryViewModel.kt`               | VM                           |
| `app/src/main/java/…/ui/entry/EntryScreen.kt`                  | UI                           |
| `app/src/main/java/…/ui/statistics/StatisticsViewModel.kt`     | VM                           |
| `app/src/main/java/…/ui/statistics/StatisticsScreen.kt`        | UI                           |
| `app/src/main/java/…/navigation/AppNavigation.kt`              | Nav graph                    |
| `app/src/main/java/…/MainActivity.kt`                          | Entry point                  |
| `app/build.gradle.kts`                                         | Dependencies                 |
| `build.gradle.kts` + `settings.gradle.kts`                     | Project config               |

---

### Verification

1. App builds with `./gradlew assembleDebug` without errors
2. Entry screen: tapping "+" with a valid amount saves a transaction (verify via Statistics)
3. Statistics screen: transactions appear, grouped by month with correct month name
4. Filter switching updates the list and recalculates balance correctly
5. Done-toggle persists across app restart
6. "Close on entry" setting: when enabled, tapping +/- exits the app; when disabled, stays open
7. Date field pre-fills today; custom date persists to saved transaction

---

### Decisions

- **No categories, no time, no colored amounts** — explicitly out of scope
- `amount` stored as positive `Double`, sign determined by `isExpense` flag
- "Done" is purely visual/informational, does not lock a month for new entries
- minSdk 26 — avoids core library desugaring complexity for `LocalDate`
- No Hilt — keeps boilerplate minimal for a single-developer small app
