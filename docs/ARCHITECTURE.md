# FrameFlow Architecture

This document describes the current architecture in FrameFlow and how list loading, details, and search are designed.

## 1. High-level architecture

FrameFlow follows a layered architecture:

1. **UI layer (Compose)**  
   Screens render `PagingData` and state flows from ViewModels.
2. **Presentation layer (ViewModels)**  
   ViewModels coordinate user intent, paging flows, and background sync calls.
3. **Data layer (Repository)**  
   Repository decides whether data comes from Room, network, or both.
4. **Local layer (Room)**  
   Room stores character entities and paging remote keys.
5. **Remote layer (Retrofit API)**  
   Rick and Morty API provides paginated character data and details by ID.
   Coil downloads character avatars from the same host (see section 8).

## 2. Core components

- `MainActivity.kt`: Navigation host (`grid` and `detail/{id}` routes).
- `CharacterGridScreen.kt`: List UI, pull-to-refresh, and search text input.
- `CharacterDetailScreen.kt`: Character detail UI and loading states.
- `CharacterViewModel.kt`: List paging streams and search orchestration.
- `CharacterDetailViewModel.kt`: Detail loading for one character.
- `CharacterRepository.kt`: Data source coordination.
- `CharacterRemoteMediator.kt`: Offline-first paging sync (network -> Room).
- `CharacterDao.kt`: Room queries for full list, by-id lookup, and search paging.
- `RateLimitRetryInterceptor.kt`: Retries HTTP 429 responses for API and image requests.
- `AppModule.kt`: Hilt providers for Room, the OkHttp/Retrofit client, and Coil's `ImageLoader`.

## 3. List paging flow (offline-first)

Default list behavior (`query` empty) uses Paging 3 with `RemoteMediator`:

1. UI collects `charactersFlow`.
2. Pager reads from Room (`characterDao.pagingSource()`).
3. `CharacterRemoteMediator` fetches missing pages from network.
4. Network pages are written into Room with remote keys.
5. UI updates from Room automatically.

This makes Room the **single source of truth** for normal browsing.

## 4. How `RemoteMediator` works in FrameFlow

`CharacterRemoteMediator` is the coordinator between Paging, Room, and the API.
It does not render UI directly. Its job is to decide *which page to fetch next* and
atomically persist results so Room stays consistent.

### 4.1 Load types and page selection

Paging calls `load(loadType, state)` with one of three load types:

1. **REFRESH**
   - Used for first load or explicit refresh.
   - Starts from page 1 (or closest known key when available).
2. **APPEND**
   - Used when user scrolls near the end.
   - Looks up the last loaded item's `RemoteKeysEntity.nextKey`.
3. **PREPEND**
   - Used when loading items before the current start.
   - Looks up the first loaded item's `RemoteKeysEntity.prevKey`.

If `nextKey`/`prevKey` is missing where required, mediator returns
`endOfPaginationReached = true` for that direction.

### 4.2 Write path (single transaction)

After fetching one page from network:

1. If `loadType == REFRESH`, clear old `remote_keys` and `characters`.
2. Compute `prevKey` and `nextKey` for that page.
3. Insert new `RemoteKeysEntity` rows (one per character ID).
4. Upsert mapped `CharacterEntity` rows.

These writes happen in one `database.withTransaction { ... }`, preventing key/data mismatch.

### 4.3 Why remote keys are needed

`RemoteKeysEntity` stores page pointers per character:

- `characterId` -> links to `CharacterEntity.id`
- `prevKey` -> page before current item
- `nextKey` -> page after current item

This lets mediator continue pagination safely after process death, refresh, and DB invalidation.

### 4.4 End-of-pagination and loop safety

Mediator stops further loading when:

- API returns no results for a requested page, or
- required pagination keys are missing.

This defensive behavior avoids infinite APPEND/PREPEND loops and unnecessary repeated API calls.

## 5. Details flow

Details route is `detail/{id}`:

1. User taps a card in `CharacterGridScreen`.
2. Navigation passes selected `id` to detail route.
3. `CharacterDetailViewModel.load(id)` calls repository.
4. Repository tries Room first (`characterDao.getById(id)`), then network fallback.
5. Screen renders loading, not-found, or detail content.

## 6. Search mechanism (hybrid)

Search is intentionally hybrid for performance:

### 6.1 Immediate local results

- `CharacterViewModel` exposes `searchFlow`.
- `CharacterRepository.searchCharacters(name)` uses Room paging query:
  - `characterDao.searchPagingSource(query)`
- As the user types, cached matches appear quickly with low latency.

### 6.2 Background online sync

- On debounced query updates, `CharacterViewModel` triggers `syncSearchCharacters(query)`.
- Repository fetches matching pages from API and upserts into Room.
- Because search UI is reading from Room paging source, new matches appear automatically as DB updates arrive.

### 6.3 Practical behavior

- **Online**: local results first, then richer/fresher results as network sync completes.
- **Offline**: local cached matches still work.
- **No match**: screen shows a clear empty-state message.

## 7. Why this search design

This design balances:

- **Responsiveness**: instant local hits from SQLite.
- **Correctness/completeness**: online sync discovers newer or previously uncached items.
- **Resilience**: still functional when network is slow or unavailable.

## 8. Networking and rate limits

`rickandmortyapi.com` rate-limits bursts of requests and answers them with **HTTP 429 (Too Many
Requests)**. The first page of the grid (about 20 avatars) is fine, but a fast scroll asks for
dozens of avatars at once. Before this handling existed, those images failed and the cards showed
Coil's error placeholder (❌).

Two mechanisms keep requests under the limit and recover when they're not:

### 8.1 Fewer parallel image downloads

`AppModule.provideImageLoader` builds Coil's `ImageLoader` (installed app-wide by
`FrameFlowApplication`, which implements Coil's `ImageLoaderFactory`). Its OkHttp client:

- shares the API client's connection pool (`okHttpClient.newBuilder()`),
- drops the API client's interceptors, so image bytes aren't body-logged,
- uses its own `Dispatcher` with `maxRequestsPerHost = 4` (OkHttp's default is 5, shared with
  API calls), so avatars queue up instead of arriving at the server all at once.

Coil's memory and disk caches mean an avatar is downloaded once; scrolling back doesn't hit the
network again.

### 8.2 Retry on 429

`RateLimitRetryInterceptor` sits on both the Retrofit client and the image client. When a
response is 429 it closes it, waits, and sends the request again:

| Attempt | Wait before retrying |
|---|---|
| 1st retry | `Retry-After` seconds if the server sent the header, else 1 s |
| 2nd retry | `Retry-After`, else 2 s |
| 3rd retry | `Retry-After`, else 4 s |

- Every wait is capped at 10 s, so a large `Retry-After` can't stall a request for minutes.
- After 3 retries the 429 is returned as-is: Coil shows the error placeholder, and Paging shows
  its retry banner.
- Other status codes (404, 500, …) and successful responses pass through untouched.
- Waiting blocks only the OkHttp worker thread for that request, never the main thread.

The defaults (`maxRetries`, `baseDelayMillis`, `maxDelayMillis`) are constructor parameters, and
the sleep function is injectable, which is how `RateLimitRetryInterceptorTest` checks the
backoff without real delays or network.

## 9. Current constraints and extension points

- Current local search uses `LIKE` matching on `name`.
- For larger datasets, upgrade to **Room FTS** for faster full-text queries and better ranking.
- Current network sync for search limits page syncing (bounded fetch) to avoid over-fetching while typing.
