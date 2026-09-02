# FrameFlow 📸

FrameFlow is a modern, production-grade Android project designed to practice **offline-first image loading and pagination** using Jetpack Compose, Room, Hilt, Retrofit, and Paging 3 (with `RemoteMediator`). 

It fetches high-quality character assets from the ultra-reliable **Rick and Morty API** and caches them locally, serving as a single source of truth.

---

## 🛠️ Tech Stack & Architecture

- **UI**: 100% Jetpack Compose for a fully declarative, modern UI.
- **Image Loading**: [Coil](https://coil-kt.github.io/coil/) (Coroutine Image Loader) for smooth asynchronous image loading with circular load indicators and fallback states.
- **Dependency Injection**: [Hilt](https://developer.android.com/training/dependency-injection/hilt-android) for robust, compile-time-safe dependency injection.
- **Local Caching (Single Source of Truth)**: [Room Database](https://developer.android.com/training/data-storage/room) for persistent offline caching.
- **Networking**: [Retrofit 2](https://square.github.io/retrofit/) & OkHttp 3 with logging interception for API communication.
- **Pagination & Offline Caching**: [Paging 3](https://developer.android.com/topic/libraries/architecture/paging/v3-paged-data) with `RemoteMediator` to seamlessly coordinate remote API calls and database updates.

---

## 📂 Project Structure

```
FrameFlow/
│
├── gradle/
│   └── libs.versions.toml       # Consolidated project dependencies & versions
│
├── app/
│   ├── src/main/
│   │   ├── AndroidManifest.xml  # Configures internet permissions, Hilt app, and Main Launcher
│   │   │
│   │   ├── java/com/dennymathew/frameflow/
│   │   │   ├── FrameFlowApplication.kt   # Standard @HiltAndroidApp wrapper
│   │   │   ├── MainActivity.kt           # Entry point setting up Compose content
│   │   │   │
│   │   │   ├── data/
│   │   │   │   ├── local/                # Room persistence layer
│   │   │   │   │   ├── ImageDatabase.kt  # Room database definition
│   │   │   │   │   ├── CharacterEntity.kt # Local representation of a character
│   │   │   │   │   ├── CharacterDao.kt   # Operations for character caching
│   │   │   │   │   ├── RemoteKeysEntity.kt # Stores prev/next page keys for RemoteMediator
│   │   │   │   │   └── RemoteKeysDao.kt  # Operations for pagination keys
│   │   │   │   │
│   │   │   │   ├── remote/               # Networking layer
│   │   │   │   │   ├── RickAndMortyApi.kt # Retrofit endpoints (Rick and Morty API)
│   │   │   │   │   ├── RickAndMortyResponse.kt # Data Transfer Object (DTO)
│   │   │   │   │   └── CharacterRemoteMediator.kt # Core Paging 3 engine coordinating network + DB
│   │   │   │   │
│   │   │   │   └── repository/           # Repository pattern
│   │   │   │       └── CharacterRepository.kt # Exposes PagingData streams to UI
│   │   │   │
│   │   │   ├── di/
│   │   │   │   └── AppModule.kt          # Hilt module supplying Singletons (Retrofit, DB, etc.)
│   │   │   │
│   │   │   └── ui/
│   │   │       ├── CharacterViewModel.kt # List paging + hybrid search orchestration
│   │   │       ├── CharacterDetailViewModel.kt # Details loading state and retrieval by ID
│   │   │       ├── screens/
│   │   │       │   ├── CharacterGridScreen.kt # Responsive grid, pull-to-refresh, search, and item navigation
│   │   │       │   └── CharacterDetailScreen.kt # Detail UI with DB-first loading and network fallback
│   │   │       └── theme/
│   │   │           ├── Color.kt
│   │   │           └── Theme.kt          # Customized Material 3 Theme wrapper
│   │   │
│   │   └── res/values/
│   │       └── themes.xml                # Minimalist Material 3 parent theme definitions
│
├── docs/
│   └── ARCHITECTURE.md                    # Data-flow and RemoteMediator deep-dive
```

---

## 🚀 Key Patterns Implemented

### 1. RemoteMediator (Room as Single Source of Truth)
Instead of directly binding the network response to the UI, the network response is dumped into Room first. Paging 3 reads exclusively from the local SQLite database. This ensures complete offline accessibility once the data has been loaded once.

### 2. State-driven Compose Grid
The `CharacterGridScreen` explicitly handles all states of a paging stream:
- `LoadState.Loading` on REFRESH: Full-screen loading spinner.
- `LoadState.Error` on REFRESH: Full-screen error card with interactive **Retry** button.
- `LoadState.Loading` on APPEND: Row-level spinner at the footer of the grid.
- `LoadState.Error` on APPEND: Inline error banner in the grid with a dedicated retry option.

### 3. Asynchronous Image Loading with Coil
Images are loaded smoothly via Coil, utilizing `SubcomposeAsyncImage` to render crossfaded placeholders and loading animations so the grid remains buttery smooth during scrolling.

### 4. Hybrid Search (DB-first + Network Sync)
Search now returns local Room matches immediately for responsiveness, then syncs network results in the background and upserts them into Room. Because the UI observes Room paging data, newly synced matches appear automatically without switching screens or data sources.

---

## 🏁 How to Run & Build

1. Open Android Studio and choose **File > Open**.
2. Select the `/Users/denny/Workspace/dev/android/FrameFlow` folder.
3. Sync Gradle and run the `:app` module on your preferred emulator or physical device.
4. **No API keys or registration required!**

---

## 📘 Architecture Documentation

For the detailed architecture and data-flow write-up (including list paging, details loading, and search mechanisms), see:

- [`docs/ARCHITECTURE.md`](docs/ARCHITECTURE.md)
