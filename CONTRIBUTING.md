# Contributing — Bug Hunter Android

Thanks for considering a contribution. Bug Hunter Android is a small, focused project — additive changes, brand consistency, green tests.

## Before you start

- For non-trivial work, open an issue first so we can align on scope.
- Check [CODE_OF_CONDUCT.md](CODE_OF_CONDUCT.md). Be kind.
- Security issues → [SECURITY.md](SECURITY.md), **not** a public issue.

## Setup

1. Install **Android Studio Koala** or newer (uses the embedded JDK 17).
2. Clone the repo. Studio will bootstrap the Gradle wrapper on first open.
3. Edit `gradle.properties` → set `bh.baseUrl` to a backend you control.

Outside Studio:
```bash
gradle wrapper --gradle-version 8.10.2
./gradlew assembleDebug
```

## Code style

- Match the surrounding code.
- Default to **no comments** unless the *why* is non-obvious.
- Compose screen shape: `XxxScreen(viewModel = hiltViewModel())` is the stateful entry, `XxxContent(state, onIntent)` is the stateless body.
- Hilt — `@HiltViewModel` on ViewModels, `@Inject constructor` on repos, one `@Module @InstallIn(SingletonComponent::class)` per feature subgraph.
- Threading — `viewModelScope.launch` for UI; `Dispatchers.IO` only inside repos for blocking I/O.
- DTOs are `internal` and Moshi-annotated (`@JsonClass(generateAdapter = true)`).
- Repositories return `Result2<T>`. ViewModels expose `StateFlow<UiState<T>>`.
- No new dependencies without a one-line rationale in the PR.

## Tests

```bash
./gradlew testDebugUnitTest             # JVM unit tests
./gradlew connectedDebugAndroidTest     # Compose UI tests (emulator/device required)
./gradlew :app:lintDebug                # Android Lint
```

All must pass on every PR.

## Adding an API endpoint

1. Add or extend the Retrofit interface under `core/network/api/`.
2. Add or extend Moshi DTOs under `core/network/dto/` with `@JsonClass(generateAdapter = true)`.
3. Add a repository round-trip test via MockWebServer.

## Pull requests

- One concern per PR.
- Title: short imperative ("Add X", "Fix Y", "Refactor Z").
- Body: what changed and why, in 2-5 sentences. Flag any change to the network layer, theme tokens, or navigation graph.
- Reference the related issue if there is one.
- Tests and lint must pass.

## License

By submitting a contribution you agree it will be licensed under the project's [MIT License](LICENSE.txt).
