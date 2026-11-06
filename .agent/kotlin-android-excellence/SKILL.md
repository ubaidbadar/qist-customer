---
name: kotlin-android-excellence
description: Apply production-grade Kotlin and Android engineering standards when implementing, fixing, reviewing, refactoring, or optimizing native Android apps. Prioritize simple maintainable code, excellent performance, safe state and data flow, consistent visual design, and verifiable correctness. Inspect the existing project before changing it; support Jetpack Compose and Views/XML.
---

# Kotlin Android Excellence

Use this skill for **every Kotlin/Android coding task**: a new feature, bug fix, UI change, integration, code review, refactor, or performance improvement. The goal is not to write the most code or create the most layers. The goal is the **simplest correct, efficient, cohesive, production-ready implementation**.

## Non-negotiable priorities

1. **Correctness and safety:** Preserve behavior, data integrity, security, and platform rules.
2. **Clarity and maintainability:** Plain, idiomatic Kotlin that another developer can understand and change quickly.
3. **Efficient by design:** No wasted main-thread work, unnecessary recomposition, duplicate state, redundant network/database calls, leaks, or battery-draining loops.
4. **Design consistency:** All screens must feel like one product. Reuse its theme, components, interaction patterns, navigation, and visual language.
5. **Minimal changes:** Solve the requested problem without unrelated migrations, restyling, dependencies, or speculative frameworks.
6. **Verified quality:** Compile, test, inspect the UI, and measure nontrivial performance claims when tools are available.

**Decision rule:** Prefer the smallest clear solution that meets the real requirements. Add an abstraction, helper, state holder, coroutine, cache, interface, module, or dependency only when it has a specific responsibility and a demonstrable benefit. Fewer lines are not automatically faster or better.

The skill is **project-agnostic**. Never assume a particular product, API, SDK level, backend, app architecture, navigation library, UI toolkit, offline requirement, color scheme, or feature set. Existing repository instructions and explicit user requirements take precedence unless they would cause a correctness or security issue.

## 1. Inspect and plan before editing

- Read `AGENTS.md`, `CLAUDE.md`, relevant documentation, existing skill/rule files, and the files around the requested feature.
- Inspect `settings.gradle.kts` / `settings.gradle`, module `build.gradle.kts` / `build.gradle`, the version catalog, `gradle.properties`, `AndroidManifest.xml`, relevant source, tests, and resources.
- Confirm installed Kotlin/Gradle/AGP/Compose versions, min/target SDK, build variants, Java toolchain, and dependencies **from the repo**, not assumptions.
- Identify the current UI stack: Compose, Views/XML, or a combination. Identify navigation, dependency injection, networking, local storage, background work, and app theme.
- Trace the complete affected path: screen → state owner → business logic → repository → network/database/platform service → UI result. Check all callers before changing contracts.
- Inspect `git status` and existing diffs. Preserve unrelated work. Never silently reset, overwrite, reformat, or undo someone else's changes.
- Before touching UI, inspect **several existing related screens**, shared components, theme tokens, icons, navigation, and visible states. Capture the established style instead of inventing a new one.
- For multi-file or risky work, make a short plan covering scope, data/state ownership, visual consistency, risks, and verification. Do not turn a small fix into a long planning exercise.

**Do not create new patterns simply because they are fashionable. Fit the project first.**

## 2. Write straightforward, idiomatic Kotlin

- Follow the project's formatting and official Kotlin naming conventions. Names must say what the code does.
- Prefer `val`, immutable data, read-only exposed collections, and private mutation. Keep ownership clear.
- Use plain functions, `data class`, `sealed interface`/`sealed class`, `when`, and null-safe operations when they make intent clearer.
- Avoid `!!`, unchecked casts, broad nullable state, magic values, and assertions used to hide missing logic.
- Model actual states accurately. Do not invent fake default objects or flags when values can be derived.
- Use scope functions (`let`, `apply`, `also`, `run`) sparingly. Deeply nested chains and clever expression gymnastics usually harm readability.
- Prefer early returns and direct control flow over unnecessary nesting or wrapper classes.
- Keep APIs as private as practical; avoid public helpers or global singletons that expose implementation details.
- Do not add interfaces for one implementation, generic base classes, factories, managers, DSLs, utility objects, or delegation layers without a real need.
- A function or file should exist because it has meaningful logic, clear ownership, real reuse, or a useful boundary. Avoid trivial forwarding functions and single-use helper files made only to shorten a caller.
- Do not duplicate meaningful business logic. Extract code when duplication is real, not merely because two screens happen to look similar today.
- Keep closely related declarations together; split a file when doing so improves responsibility and navigation, not to satisfy arbitrary line counts.
- Explain **why** in comments when necessary; code should explain **what**. Remove obsolete comments, dead code, debugging artifacts, and abandoned alternatives.
- Avoid deprecated or experimental APIs when maintained stable alternatives exist **and are compatible with the installed versions**. Never upgrade dependencies speculatively.

### When choosing a solution

- Prefer standard library and project dependencies already present.
- Avoid unnecessarily creating collections or doing repeated scans in hot paths; choose appropriate data structures for lookup and iteration.
- A simple loop can be clearer and cheaper than a long sequence of `map`/`filter`/`flatMap`. Conversely, do not micro-optimize readable operations without cause.
- Avoid reflexively using `Sequence`, inline classes, custom delegates, concurrency, or caching as performance tricks.
- If adding code makes future modifications harder without improving correctness, performance, or genuine reuse, simplify it.

## 3. Architecture without overengineering

Keep responsibilities clear. A typical app uses:

`UI → ViewModel/state holder → repository → API / database / platform API`

- **UI** renders state and reports user actions. It must not perform expensive business logic, database access, or network calls while rendering.
- **ViewModel** owns screen-level presentation state and coordinates work that should survive Activity recreation. Reusable visual elements should receive values and callbacks instead of owning independent ViewModels.
- **Repository/data layer** mediates data sources, caching, synchronization, and application data rules. The UI should not bypass the established data layer.
- Add a domain/use-case layer only when business logic is genuinely complex or reused across multiple state owners. Do not create a chain of one-line pass-through classes.
- Follow **unidirectional data flow**: immutable state down, user actions up.
- Define **one source of truth** for each piece of data. Do not independently copy the same server record into multiple unrelated state holders and let them drift apart.
- Combine related screen state into a coherent UI model when useful; keep truly independent state local. **Do not impose an arbitrary limit on the number of state variables.** The goal is minimal, well-owned state.
- Derive booleans, totals, selections, step visibility, and other values from authoritative state where possible instead of maintaining redundant flags.
- Expose `StateFlow` or another project-established observable contract read-only. Keep `MutableStateFlow` and mutation private.
- State should represent loading, content, empty, and recoverable failure honestly. A failed load must not produce a permanently blank screen.
- Use the project's dependency injection approach; do not introduce an additional DI framework for a feature.
- Keep `Activity`, `Fragment`, `Service`, and `BroadcastReceiver` focused on platform/lifecycle coordination rather than becoming application-wide stores.
- Preserve navigation and back-stack contracts. Keep routes, deep links, and arguments typed or validated as supported by the project's navigation library.

### State ownership checks

Before adding a state variable, observer, query, or callback, ask:

1. Is this information already available from the screen's current state, repository, navigation argument, or saved record?
2. Can it be derived cheaply and correctly instead of duplicated?
3. Does this owner need it for its whole lifetime, or only a small child/control?
4. Who updates it, who observes it, and what happens on rotation, process death, and screen re-entry?
5. Will two collectors accidentally start the same upstream work?

Do not add a second source of truth merely to make a screen easier to code.

## 4. Jetpack Compose — clean UI and smooth rendering

*Apply when the repository uses Compose; otherwise follow its existing Views/XML conventions.*

- Write small composables with one clear visual responsibility. Avoid giant composables containing the entire screen's rendering, networking, validation, and navigation.
- Split composables at meaningful UI boundaries, not into a forest of single-line wrappers. Reuse shared controls where they prevent style or behavior drift.
- Keep composables free of side effects during composition. Fetch through the screen's state owner; do not trigger network/database writes in rendering paths.
- Collect `Flow`/`StateFlow` lifecycle-aware, typically with `collectAsStateWithLifecycle` at an appropriate screen boundary. Pass the needed values down rather than recollecting the same source in each row.
- Hoist state to the lowest owner that actually needs to coordinate it. Use `remember` for composition-scoped state/caching and `rememberSaveable` for **small** restorable UI state. Keep durable data in repositories/databases.
- Use `LaunchedEffect` and `DisposableEffect` only for genuine lifecycle-bound side effects, with correct keys and cleanup. Avoid accidental work restart on every recomposition.
- Avoid duplicating derived state. Use ordinary expressions first; `remember`, `derivedStateOf`, or version-supported equivalents only for meaningful work/invalidation savings.
- Compose may recompose frequently: do not sort large datasets, parse responses, decode images, allocate heavy objects, or run expensive calculations in rendering.
- Use `LazyColumn`/`LazyRow`/lazy grids for large or unbounded lists. Supply stable, **unique, saveable** keys; use `contentType` when list items have different structures. Never use an index as identity for a reorderable list.
- Use plain `Row`/`Column` for small fixed groups when lazy virtualization adds needless overhead.
- Keep fast-changing state reads close to where they are used. Do not cause an entire screen to update for a small scrolling/animation value.
- Do not indiscriminately add `@Stable`, `@Immutable`, `remember`, custom equality logic, or manual memoization. Understand installed Compose compiler behavior and **profile before stability hacks**.
- Request appropriately sized images with the project's image-loading library and cache. Do not decode full-resolution images for thumbnails.
- Maintain correct modifier order, focus behavior, semantics, text input handling, and layout insets.
- Prefer previews for reusable components and states where the project supports them; previews are not a substitute for real device checks.

### If the project uses Views/XML

- Preserve its established UI approach; do not migrate to Compose without being asked.
- Use existing view binding and lifecycle patterns. Avoid repeated `findViewById` or listener/adapter setup in hot paths.
- Use `ListAdapter`/`DiffUtil` or the project's equivalent for changing lists; don't redraw an entire dataset for a one-item change without reason.
- Collect Flows with `repeatOnLifecycle` (or a compatible lifecycle-aware approach) and release Fragment view references and listeners correctly.
- Keep XML/layout nesting reasonable and avoid expensive main-thread view, image, or measurement work.

## 5. Visual design consistency — mandatory on every UI change

**The app must have one recognizable design language, not a different style on each screen.** Consistency is part of correctness, not optional polish.

### Discover and reuse the existing system

- Before designing a screen, inspect the theme and **at least two comparable screens or components** if they exist.
- Reuse established color roles, typography, font weights, spacing scale, shapes/corner radii, borders, elevation, icon style, surfaces, and motion.
- Use centralized tokens through the project's theme (`MaterialTheme` or its own design system). Do not scatter `Color(...)`, arbitrary hex codes, random `dp` values, custom font sizes, or new button styles throughout feature screens.
- If a required token/component is missing, add the **smallest reusable addition** to the existing system. Do not introduce a parallel theme or duplicate design primitive.
- Use the same input fields, labels, validation errors, buttons, dialogs, bottom sheets, cards, lists, tabs, snackbars, loading indicators, and empty-state patterns across comparable flows.
- Keep primary/secondary/destructive actions visually consistent in role, hierarchy, size, shape, pressed/disabled/loading states, and placement.
- Keep spacing and alignment consistent within and across screens. Align headings, content edges, cards, forms, and bottom actions to the same layout rules.
- Make navigation structure, app bars, back behavior, selected states, transitions, and screen titles coherent throughout the product.
- Reuse existing icon families and illustrations. Avoid mismatched icon stroke weights, random emoji, or decorative assets with a different style.
- **Do not introduce dark mode, dynamic color, gradients, animations, or a new palette just because the platform supports them.** Respect the modes and branding the project actually uses. If multiple themes exist, ensure every component handles them correctly.
- For new screens, use the established visual language. For a requested redesign, define/update shared tokens and primitives first, then apply them consistently to every affected screen.

### Design quality gates

Review every affected screen for:

- **Hierarchy:** clear titles, labels, help text, primary action, secondary actions, and content grouping.
- **Consistency:** colors, text styles, padding/gaps, corners, buttons, cards, fields, icons, dialogs, and navigation match related screens.
- **States:** initial/loading/skeleton, empty, populated, error, retry, offline (if supported), disabled, submitting, and success are coherent and visually intentional.
- **Usability:** users can discover the next action; avoid unnecessary taps, navigation loops, repeated input, hidden essentials, and confusing confirm flows.
- **Responsive layout:** small and large screens, orientation changes, multi-window, edge-to-edge/system bars, display cutouts, keyboard/IME, and gesture navigation do not overlap or clip important controls.
- **Text and accessibility:** font scaling, long strings, translations/RTL where supported, contrast, readable line heights, clear errors, screen reader semantics, focus order, and touch targets of at least 48dp.
- **Interaction:** no accidental double submission, dead tap targets, click-through overlays, inconsistent dismissal behavior, or bottom buttons obscured by the keyboard.

When visual tooling is available, compare screenshots of related screens and inspect the actual running result—not only the composable source. Never claim the UI is pixel-perfect without visual verification.

## 6. Network and database efficiency

**Make the fewest correct requests—not the fewest requests at any cost.** Correctness and fresh authoritative data still matter.

- Before adding a fetch, check whether the repository, active screen state, shared cache, or previously loaded parent data already provides the answer.
- Load shared screen data once at the appropriate owner. Do not request the same list separately for every row, form field, sheet, or child element.
- Avoid accidental **N+1** calls: one list request followed by a request per item, or repeated database queries inside loops. Use appropriate API response shapes, batched reads, joins/relations, or repository-level aggregation where available.
- After a successful mutation, update the repository's source of truth using the **authoritative saved result** if it is complete. Avoid immediately refetching just to redisplay known data.
- **Refresh when necessary:** server-derived fields, permission changes, cross-device updates, incomplete mutation responses, stale caches, or contracts that require a new read. Never fake correctness by assuming a write succeeded.
- Share expensive upstream flows, cache reads, or deduplicate in-flight requests when there is a real duplicate-work problem. Avoid elaborate caches and locks without an explicit invalidation/concurrency story.
- Use pagination or incremental loading for large lists. Filter and aggregate close to the data when it reduces unnecessary transfer without breaking correctness.
- Debounce search only when useful and cancel superseded requests. Handle late answers so stale data cannot overwrite newer selection/search results.
- Use transactions for related database mutations. Choose useful indices and check query plans for hot Room/SQLite paths; avoid main-thread database work.
- Room is suitable for structured persisted data when required; DataStore for small preferences. Do not add persistence just to cache a trivial value.
- In offline-capable apps, define the authoritative local/remote model, freshness, synchronization, conflict, and retry behavior explicitly. Do not pretend an online-only write succeeded while offline.
- Retry transient failures selectively with bounded backoff; avoid unsafe retries of non-idempotent operations. Use idempotency when the protocol supports it.
- Keep loading, unauthorized, validation, timeout, network, and server errors distinguishable enough to act on.
- Do not put demo records, fake IDs, placeholder success responses, or hidden fallbacks into production logic. Real screens use real data and truthful empty/error states.

## 7. Coroutines, Flow, and lifecycle

- Use structured concurrency and the correct scope: `viewModelScope` for ViewModel work; lifecycle-aware scopes for visible UI; explicitly managed long-lived scopes only when required.
- Never use `GlobalScope` or `runBlocking` on the main thread. Avoid unmanaged threads, timers, and fire-and-forget jobs that outlive their owner.
- Suspend functions in repositories/data sources must be **main-safe**. Offload actually blocking IO or expensive CPU work with the appropriate dispatcher; don't wrap already main-safe suspending APIs in redundant dispatcher hops.
- Never swallow `CancellationException`, including inside broad `catch` or `runCatching`; propagate cancellation correctly.
- Use `StateFlow`, `Flow`, `stateIn`, `shareIn`, `combine`, `debounce`, `collectLatest`, and `distinctUntilChanged` only when their semantics solve the actual problem—not as mandatory ceremony.
- Avoid duplicate Flow collectors that independently repeat expensive upstream work. Decide whether the source should be cold, hot, shared, or cached.
- Keep coroutine cancellation, ownership, stale-result handling, and error propagation explicit. Do not invent counters/mutexes if a simpler single-owner flow solves the race.
- Release listeners, observers, receivers, sensors, WebSocket connections, callbacks, cursors, streams, and other resources when their owner ends.
- Verify background → foreground, rotation, navigation away/back, and process recreation for duplicate work and lost state.

## 8. Android platform, security, and resource use

- Keep the UI thread responsive: no blocking I/O, heavy bitmap work, expensive parsing, or large calculations there. Investigate ANRs rather than masking them.
- Keep startup lean. Defer optional initialization and noncritical requests so the first useful screen appears quickly.
- Never retain `Activity`, `Fragment`, or View references in objects that outlive them. Treat `Context` lifetimes deliberately.
- Prefer event-driven work to polling. Avoid unnecessary wake locks, continuous sensors/location, repeated network wakeups, and idle CPU use.
- Use `WorkManager` for durable, deferrable work when suitable. Use foreground services only when Android's current platform rules and the feature genuinely require one. Do not assume precise scheduling from ordinary background work.
- Request only permissions genuinely needed by the feature, at the correct time; handle denial and revocation. Never weaken platform safeguards for convenience.
- Treat untrusted data carefully. Validate boundaries, enforce server-side authorization, protect secrets with established secure storage, use TLS, and avoid logging credentials, tokens, private data, or sensitive API bodies.
- Respect installed SDK and OS-version capabilities; implement supported fallbacks where needed. Do not make unsupported promises about device management, persistence, or background execution.
- Keep release optimization configured appropriately (R8/resource shrinking when compatible). Do not alter signing, publishing, or security-sensitive build settings unless requested.
- Preserve app size, battery, memory, and network budgets; avoid new dependencies for a feature the current stack already supports.

## 9. Performance improvement protocol

**Build efficiently by default, but measure before claiming an optimization.**

1. Identify the actual cost: repeated request/query, main-thread stall, excessive recomposition, layout work, memory allocation, startup delay, network transfer, or battery activity.
2. Prefer removing unnecessary work and fixing algorithmic complexity before caching, parallelizing, or applying low-level tricks.
3. Record a baseline when practical: Network Inspector/logs, SQL query counts, Layout Inspector, Compose tooling, Android Studio Profiler, Perfetto/System Trace, memory allocations, or Macrobenchmark.
4. Make a focused change and compare the same device, build type, dataset, and user journey.
5. Check that behavior, data consistency, visuals, accessibility, and battery use did not regress.
6. Report actual results and measurement conditions; otherwise say **not measured**.

- Use release-like builds for credible timing measurements; debug builds can misrepresent performance.
- For startup, scrolling, and critical journeys, consider Macrobenchmark and app-specific Baseline Profiles when the project warrants them.
- Never assert “zero crashes,” “no memory leaks,” “perfect performance,” “60 FPS,” or “fully optimized” without evidence.
- Do not perform sweeping refactors because a profiler surfaced a narrow hot path.

## 10. Testing and completion requirements

Choose tests proportional to the change and risk:

- **Unit tests:** validation, calculations, parsing, reducers/state updates, business rules, failures, and edge cases.
- **Repository/ViewModel tests:** loading, success, empty, failure, cancellation, caching, concurrent inputs, and state consistency using fake dependencies/test dispatchers where appropriate.
- **UI tests:** primary interactions, form validation, loading/retry states, screen restoration, navigation, and accessibility behavior.
- **Integration tests:** database migrations/transactions, API serialization/contracts, background workers, permissions, and platform-specific integrations as applicable.
- **Visual verification:** check affected screens against the shared design system in representative states, sizes, and theme modes. Use screenshots or previews when available.
- **Device checks:** at least the critical impacted flow; include keyboard, orientation, background/resume, poor connection, and supported OS differences when relevant.

Use the **actual project's** Gradle tasks. Examples only (names vary by modules, variants, and configured tools):

```bash
./gradlew :app:assembleDebug
./gradlew :app:testDebugUnitTest
./gradlew :app:lintDebug
# If configured: detekt / ktlint / connectedAndroidTest / macrobenchmark
```

- Run the relevant checks if the environment supports them. Don't silently skip failed tests, suppress warnings, or delete tests to make a build green.
- If a device/emulator, SDK, network service, signing key, or build tool is unavailable, state that limitation precisely. Do not claim checks passed when they were not run.
- Review the final diff for unrelated edits, duplicate queries, duplicated state, lifecycle leaks, redundant wrappers, questionable permissions, hardcoded styling, accessibility regressions, and unexpected visual changes.

### Final delivery format

Keep the report concise and factual:

1. What changed and why.
2. How existing architecture and design consistency were preserved.
3. What was verified: **passed / failed / not run**.
4. Any remaining important limitation or risk.
5. If relevant, measured performance changes; distinguish measurement from expectation.

Explain non-obvious Kotlin or Android decisions in clear, simple language without a tutorial unless the user asks.

## Explicitly avoid

- Overengineering: needless interfaces, use cases, helper files, abstraction hierarchies, wrappers, states, caches, and dependencies.
- Copy-pasted logic or one-off visual styles that fragment the design system.
- Screens that create their own competing colors, fonts, radii, button styles, loading behavior, or navigation conventions.
- Fetching/collecting the same resource independently in multiple rows; repeated read-after-write without a correctness reason.
- Networking/database work in composable bodies; expensive UI-thread work; unmanaged jobs or listeners.
- Invalid state represented by combinations of unrelated booleans when a clearer state model fits.
- Hardcoded demo data, fake success, silent exception handling, swallowing cancellation, and unsafe null assertions.
- Unrequested migrations, wholesale UI rewrites, theme changes, package upgrades, or unrelated cleanup.
- Optimizing for code cleverness or raw line count rather than clarity, real resource use, and correctness.
- Unverified claims about performance, visual correctness, security, or test success.

## Authoritative references (consult as relevant)

- Android architecture: https://developer.android.com/topic/architecture/recommendations
- Data layer: https://developer.android.com/topic/architecture/data-layer
- Kotlin conventions: https://kotlinlang.org/docs/coding-conventions.html
- Compose performance: https://developer.android.com/develop/ui/compose/performance
- Compose best practices: https://developer.android.com/develop/ui/compose/performance/bestpractices
- Compose themes/design systems: https://developer.android.com/develop/ui/compose/designsystems/custom
- Material 3 theming: https://developer.android.com/develop/ui/compose/designsystems/material3
- Compose accessibility: https://developer.android.com/develop/ui/compose/accessibility
- Android coroutine practices: https://developer.android.com/kotlin/coroutines/coroutines-best-practices
- Saved UI state: https://developer.android.com/develop/ui/compose/state-saving
- Offline-first data: https://developer.android.com/topic/architecture/data-layer/offline-first
- Background work: https://developer.android.com/develop/background-work/background-tasks/persistent
- Macrobenchmark: https://developer.android.com/topic/performance/benchmarking/macrobenchmark-overview
- Baseline Profiles: https://developer.android.com/develop/ui/compose/performance/baseline-profiles

Use current official guidance **only after checking version compatibility** with the actual repository. Do not treat a documentation example as a reason to restructure working code.
