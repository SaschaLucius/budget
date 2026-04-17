---
description: "Scaffold a minimal Android finance tracker from this protocol. Keep scope tight and avoid non-essential features."
agent: "agent"
---

## Implementation Protocol: Finanz Android App

Minimal, clean Android finance tracker. **Kotlin + Jetpack Compose**, local **Room** database, **DataStore** for a small app setting, Compose Navigation for two destinations. No Hilt — manual DI via an Application class keeps it lean.

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
5. Create `AppNavigation.kt` — NavHost with two destinations:
   - `entry?transactionId={transactionId}` (`transactionId` optional; `null` = new entry)
   - `statistics`

---

### Phase 2 — Data Layer

6. `Transaction.kt` — Room `@Entity`:
   - `id: Long` (autoGenerate), `amount: Int` (always positive), `isExpense: Boolean`, `subject: String?`, `date: LocalDate`
7. `Converters.kt` — TypeConverter: `LocalDate` ↔ `Long` (epoch day)
8. `TransactionDao.kt`:
   - `insert(Transaction)`, `update(Transaction)`, `delete(Transaction)`
   - `getById(id: Long): Transaction?`
   - `queryByDateRange(from: LocalDate, to: LocalDate): Flow<List<Transaction>>`
   - Order results by `date DESC, id DESC` for stable display
9. `AppDatabase.kt` — Room DB version 1, entity `[Transaction]`, TypeConverters
10. `TransactionRepository.kt` — wraps `TransactionDao`, exposes suspend/Flow functions to ViewModels including `getById`

---

### Phase 3 — Settings

11. `SettingsDataStore.kt` — DataStore Preferences with one key:
    - `close_on_entry: Boolean` (default `false`). Exposes `Flow<Boolean>` and `suspend fun setCloseOnEntry(Boolean)`. This setting applies only when saving a new entry.

---

### Phase 4 — Entry Screen _(parallel with Phase 5)_

12. `EntryViewModel.kt` — holds `amount: String`, `subject: String`, `date: LocalDate` (default today), `editingId: Long?` (null = new entry), injected repository + settings
    - `fun load(id: Long)`: fetches the transaction via repository and pre-fills fields; called only when the `transactionId` route arg is present
    - `fun save(isExpense: Boolean)`: validates `amount` as non-empty, `toIntOrNull() != null`, and `> 0` → insert or update `Transaction`
    - After save:
      - if `editingId != null`, emit `navigateBack`
      - else if `close_on_entry` is `true`, emit `closeApp`
      - else clear `amount` and `subject` for quick next entry while keeping the selected date
    - `EntryScreen` collects one-off UI events and either navigates back or calls `(LocalContext.current as Activity).finish()` exactly once
    - Needs a `ViewModelProvider.Factory` that reads singletons from `FinanzApp` (the one boilerplate cost of skipping Hilt)
13. `EntryScreen.kt` (Composable):
    - Top bar: app title (or "Bearbeiten" in edit mode) + small gear icon → opens a simple settings dialog
    - Large numeric `OutlinedTextField` for amount (`KeyboardType.Number`); filter in `onValueChange` with `s.filter { it.isDigit() }` — `KeyboardType.Number` permits `.` and `-` on some IMEs, so the explicit filter is required
    - Show inline error only after save attempt when amount is empty, zero, or too large to fit `Int`
    - Secondary `OutlinedTextField` for subject (optional)
    - `DatePickerDialog`-backed date field showing date only (no time)
    - Two full-width large buttons: **"+"** (income) and **"−"** (expense)
    - Small `IconButton` (chart icon) → navigate to statistics
    - Settings dialog: single toggle "App bei Eingabe schließen"

---

### Phase 5 — Statistics Screen _(parallel with Phase 4)_

14. `Filter.kt` — sealed class:

- `ThisMonth`, `LastMonth`, `LastThreeMonths`
- No custom date range in v1; add it later only if it is actually needed

15. `StatisticsViewModel.kt`:

- State: `selectedFilter: Filter` (default `ThisMonth`), `transactions: List<Transaction>`, `balance: Int`
- `fun setFilter(Filter)` → recalculates `LocalDate` range → queries repository
- `fun delete(transaction: Transaction)` → deletes the selected transaction
- `balance` = Σ `(if isExpense → -amount else +amount)` for visible transactions
- Use `YearMonth` only for UI grouping; DB queries use `LocalDate` boundaries directly
- Needs a `ViewModelProvider.Factory` that reads singletons from `FinanzApp`

16. `StatisticsScreen.kt` (Composable):
    - Top bar: "Statistik" title + back arrow → Entry
    - Filter button (chip showing current period label) → dropdown with 3 fixed options
    - `LazyColumn` grouping transactions by `YearMonth`:
      - **Month header row**: month name + year
      - **Transaction rows**: date (short format), subject (if present), amount with "+" or "−" prefix but **no color**
      - **Long-press** on a transaction row → confirmation dialog → delete
      - **Tap** on a transaction row → navigate to `entry?transactionId=...` in edit mode (pre-filled)
    - Sticky bottom bar: "Gesamt: {balance}" — no color, plain text

---

### Phase 6 — Wire-up & Polish

17. Register `FinanzApp` in `AndroidManifest.xml`, set `MainActivity` as launcher
18. Connect `MainActivity` → `AppNavigation`
19. Ensure `amount` field shows inline error text when empty, zero, or too large to fit `Int` on save; `s.filter { it.isDigit() }` (step 13) prevents most bad characters at input time

---

### Relevant files (to create)

| Path                                                           | Purpose                      |
| -------------------------------------------------------------- | ---------------------------- |
| `app/src/main/java/com/lukulent/finanzapp/FinanzApp.kt`        | Application class, manual DI |
| `app/src/main/java/…/data/db/AppDatabase.kt`                   | Room DB                      |
| `app/src/main/java/…/data/db/TransactionDao.kt`                | DAO                          |
| `app/src/main/java/…/data/db/Converters.kt`                    | TypeConverter                |
| `app/src/main/java/…/data/model/Transaction.kt`                | Entity                       |
| `app/src/main/java/…/data/repository/TransactionRepository.kt` | Repository                   |
| `app/src/main/java/…/settings/SettingsDataStore.kt`            | DataStore setting            |
| `app/src/main/java/…/ui/entry/EntryViewModel.kt`               | VM                           |
| `app/src/main/java/…/ui/entry/EntryScreen.kt`                  | UI                           |
| `app/src/main/java/…/ui/statistics/Filter.kt`                  | Filter types                 |
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
5. Tapping an existing transaction opens Entry in edit mode; saving updates the transaction and returns to Statistics
6. Long-press delete removes a transaction only after confirmation
7. "Close on entry" persists across app restart and only applies to new-entry saves; edit saves always return to Statistics
8. Date field pre-fills today; custom date persists to saved transaction

---

### Decisions

- **No categories, no time, no colored amounts, no decimal amounts, no done-month tracking, no custom date range in v1** — explicitly out of scope
- `amount` stored as positive `Int` (whole euros/units), validated with `toIntOrNull()` and `> 0`; sign determined by `isExpense` flag; `balance` is `Int` as well
- `close_on_entry` is only for the new-entry flow; edit saves always return to Statistics
- DataStore is used only for app settings; transaction data stays in Room
- The app still has two destinations; edit mode is handled through an optional `transactionId` on the Entry route
- minSdk 26 — avoids core library desugaring complexity for `LocalDate`
- No Hilt — keeps boilerplate minimal; each VM needs a `ViewModelProvider.Factory` reading singletons from `FinanzApp` (the one boilerplate cost)
- `YearMonth` is used only for UI grouping; DB queries use `LocalDate` ranges directly
- App class, package, and label use three names (`FinanzApp`, `finanzapp`, "Budget") — rename all three together if ever renaming
