## Plan: Finanz Android App

Minimal, clean Android finance tracker. **Kotlin + Jetpack Compose**, local **Room** database, **DataStore** for settings (including "done" months), Compose Navigation for two screens. No Hilt — manual DI via an Application class keeps it lean.

**App label:** "Budget" · **Amounts:** whole integers only (no decimals) · **Language:** German

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
   - `id: Long` (autoGenerate), `amount: Int` (always positive), `isExpense: Boolean`, `subject: String?`, `date: LocalDate`
7. `Converters.kt` — TypeConverter: `LocalDate` ↔ `Long` (epoch day)
8. `TransactionDao.kt`:
   - `insert(Transaction)`, `update(Transaction)`, `delete(Transaction)`, `queryByDateRange(from, to): Flow<List<Transaction>>`
9. `AppDatabase.kt` — Room DB version 1, entity `[Transaction]`, TypeConverters
10. `TransactionRepository.kt` — wraps `TransactionDao`, exposes suspend/Flow functions to ViewModels

---

### Phase 3 — Settings

11. `SettingsDataStore.kt` — DataStore Preferences with two keys:
    - `close_on_entry: Boolean` (default `false`). Exposes `Flow<Boolean>` and `suspend fun setCloseOnEntry(Boolean)`.
    - `done_months: Set<String>` (YYYY-MM strings, default empty). "Done" is a UI preference, not transactional data — DataStore is a cleaner fit than a Room entity. Exposes `Flow<Set<String>>` and `suspend fun toggleMonthDone(key: String)`.

---

### Phase 4 — Entry Screen _(parallel with Phase 5)_

12. `EntryViewModel.kt` — holds `amount: String`, `subject: String`, `date: LocalDate` (default today), `editingId: Long?` (null = new entry), injected repository + settings
    - `fun load(id: Long)`: pre-fills fields from existing transaction (edit mode)
    - `fun save(isExpense: Boolean)`: validates amount non-empty and digits-only → insert or update Transaction → emits `closeApp: SharedFlow<Unit>` when `close_on_entry` is `true`
    - `EntryScreen` collects `closeApp` in `LaunchedEffect(closeApp)` and calls `(LocalContext.current as Activity).finish()` exactly once
    - Needs a `ViewModelProvider.Factory` that reads singletons from `FinanzApp` (the one boilerplate cost of skipping Hilt)
13. `EntryScreen.kt` (Composable):
    - Top bar: app title (or "Bearbeiten" in edit mode) + small gear icon → opens Settings bottom sheet
    - Large numeric `OutlinedTextField` for amount (`KeyboardType.Number`); filter in `onValueChange` with `s.filter { it.isDigit() }` — `KeyboardType.Number` permits `.` and `-` on some IMEs, so the explicit filter is required
    - Secondary `OutlinedTextField` for subject (optional)
    - `DatePickerDialog`-backed date field showing date only (no time)
    - Two full-width large buttons: **"+"** (income) and **"−"** (expense)
    - Small `IconButton` (chart icon) → navigate to statistics
    - Settings bottom sheet: single toggle "App bei Eingabe schließen"

---

### Phase 5 — Statistics Screen _(parallel with Phase 4)_

14. `Filter.kt` — sealed class:
    - `ThisMonth`, `LastMonth`, `LastTwoMonths`, `LastThreeMonths`, `DateRange(from, to)`
    - Ship with the four fixed ranges first; `DateRange` with two `DatePickerDialog`s can be added later — the sealed class already accommodates it
15. `StatisticsViewModel.kt`:
    - State: `selectedFilter: Filter` (default `ThisMonth`), `transactions: List<Transaction>`, `monthStatuses: Map<YearMonth, Boolean>`, `balance: Int`
    - `fun setFilter(Filter)` → recalculates date range → queries repository
    - `fun toggleMonthDone(yearMonth: YearMonth)` → calls `SettingsDataStore.toggleMonthDone`; derives YYYY-MM key at the boundary
    - `balance` = Σ `(if isExpense → -amount else +amount)` for visible transactions
    - Use `YearMonth` throughout VM/UI layer; convert to primitive `Int` pair only when passing to DB queries
    - Needs a `ViewModelProvider.Factory` that reads singletons from `FinanzApp`
16. `StatisticsScreen.kt` (Composable):
    - Top bar: "Statistik" title + back arrow → Entry
    - Filter button (chip showing current period label) → bottom sheet / dropdown with 4 fixed options (+ `DateRange` once added)
    - `LazyColumn` grouping transactions by `YearMonth`:
      - **Month header row**: month name + year + done-toggle (`Switch` or checkbox). Toggle is per-month regardless of filter span
      - **Transaction rows**: date (short format), subject (if present), amount with "+" or "−" prefix but **no color**
      - **Long-press** on a transaction row → confirmation dialog → delete
      - **Tap** on a transaction row → navigate to Entry screen in edit mode (pre-filled)
    - Sticky bottom bar: "Gesamt: {balance}" — no color, plain text

---

### Phase 6 — Wire-up & Polish

17. Register `FinanzApp` in `AndroidManifest.xml`, set `MainActivity` as launcher
18. Connect `MainActivity` → `AppNavigation`
19. Ensure `amount` field shows inline error text when empty or non-numeric on save; `s.filter { it.isDigit() }` (step 13) prevents bad characters at input time

---

### Relevant files (to create)

| Path                                                           | Purpose                            |
| -------------------------------------------------------------- | ---------------------------------- |
| `app/src/main/java/com/lukulent/finanzapp/FinanzApp.kt`        | Application class, manual DI       |
| `app/src/main/java/…/data/db/AppDatabase.kt`                   | Room DB                            |
| `app/src/main/java/…/data/db/TransactionDao.kt`                | DAO                                |
| `app/src/main/java/…/data/db/Converters.kt`                    | TypeConverter                      |
| `app/src/main/java/…/data/model/Transaction.kt`                | Entity                             |
| `app/src/main/java/…/data/repository/TransactionRepository.kt` | Repository                         |
| `app/src/main/java/…/settings/SettingsDataStore.kt`            | DataStore (settings + done months) |
| `app/src/main/java/…/ui/entry/EntryViewModel.kt`               | VM                                 |
| `app/src/main/java/…/ui/entry/EntryScreen.kt`                  | UI                                 |
| `app/src/main/java/…/ui/statistics/StatisticsViewModel.kt`     | VM                                 |
| `app/src/main/java/…/ui/statistics/StatisticsScreen.kt`        | UI                                 |
| `app/src/main/java/…/navigation/AppNavigation.kt`              | Nav graph                          |
| `app/src/main/java/…/MainActivity.kt`                          | Entry point                        |
| `app/build.gradle.kts`                                         | Dependencies                       |
| `build.gradle.kts` + `settings.gradle.kts`                     | Project config                     |

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

- **No categories, no time, no colored amounts, no decimal amounts** — explicitly out of scope
- `amount` stored as positive `Int` (whole euros/units), sign determined by `isExpense` flag; `balance` is `Int` as well
- "Done" state stored in DataStore as `Set<String>` (YYYY-MM keys) — it is a UI preference, not transactional data; keeps Room schema to a single entity
- "Done" is purely visual/informational, does not lock a month for new entries
- minSdk 26 — avoids core library desugaring complexity for `LocalDate`
- No Hilt — keeps boilerplate minimal; each VM needs a `ViewModelProvider.Factory` reading singletons from `FinanzApp` (the one boilerplate cost)
- `YearMonth` used in VM/UI layer; converted to `(year: Int, month: Int)` at DB boundary if needed
- App class, package, and label use three names (`FinanzApp`, `finanzapp`, "Budget") — rename all three together if ever renaming
