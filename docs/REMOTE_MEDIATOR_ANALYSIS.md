# RemoteMediator Evaluation & Root Cause Analysis

This document provides a technical evaluation of whether `RemoteMediator` is the right approach for FrameFlow and analyzes the root causes of why the initial implementation was missing items from the Rick and Morty API.

---

## 1. Does RemoteMediator Miss Items From the API?

**Yes.** In the initial implementation, items were dropped, skipped, or permanently missed in four common user scenarios:

### Issue A: Search Sync Polluting Room Without Remote Keys (Halts Pagination)
* **What Happened**: When a user searched for characters, `CharacterRepository.syncSearchCharacters()` upserted API results directly into the `characters` table with `characterDao.upsertAll()`, but did **not** write any entries to `remote_keys`.
* **The Failure**: When the user cleared search and returned to the main feed, `CharacterDao.pagingSource()` queried:
  ```sql
  SELECT * FROM characters ORDER BY id ASC
  ```
  If any character loaded by search had an `id` near or past the current feed position (e.g. ID `250` while the feed was on page 1, IDs `1–20`), Room served character `250`.
* **Pagination Termination**: When Paging reached the end of the loaded chunk, `CharacterRemoteMediator.load(LoadType.APPEND)` called `getRemoteKeyForLastItem(state)` which queried `remoteKeysDao.getRemoteKeysForCharacterId(250)`. Because character `250` had no entry in `remote_keys`, it returned `null`.
* **The Code**:
  ```kotlin
  val nextKey = remoteKeys?.nextKey
      ?: return MediatorResult.Success(endOfPaginationReached = true)
  ```
  Because `remoteKeys` was `null`, it returned `endOfPaginationReached = true`.
* **Impact**: Paging permanently shut down. All subsequent pages from the API (potentially hundreds of characters) were never requested and completely missed.

---

### Issue B: Visual Gaps & Skips Due to `ORDER BY id ASC`
* **What Happened**: The main feed queried `SELECT * FROM characters ORDER BY id ASC`.
* **The Failure**: If a search query or detail screen fallback loaded character `#386`, and the main catalog was only on page 1 (`1–20`), Room ordered by `id ASC` and rendered `#386` directly after `#20`.
* **Impact**: Characters `21` through `385` appeared completely missing from the UI. Furthermore, scrolling to `#386` immediately triggered Issue A, locking pagination.

---

### Issue C: `LoadType.REFRESH` Dropping Preceding Pages When Scrolled Down
* **What Happened**:
  ```kotlin
  LoadType.REFRESH -> {
      val remoteKeys = getRemoteKeyClosestToCurrentPosition(state)
      remoteKeys?.nextKey?.minus(1) ?: STARTING_PAGE_INDEX
  }
  ```
  followed by:
  ```kotlin
  if (loadType == LoadType.REFRESH) {
      database.remoteKeysDao.clearRemoteKeys()
      database.characterDao.clearAll()
  }
  ```
* **The Failure**: If the user scrolled to page 4 (IDs `61–80`) and triggered a refresh (e.g., pull-to-refresh or process recreation):
  1. `page` was calculated as `4`.
  2. `clearAll()` wiped the entire database.
  3. The mediator fetched **only page 4** from the network.
  4. Room's `LimitOffsetPagingSource` maps row 0 to index 0 (`itemsBefore = 0`), meaning Room considers the start of the table as position 0 and **never triggers `PREPEND`**.
* **Impact**: Pages 1 through 3 (characters 1–60) were permanently wiped and never re-fetched. The feed restarted from item 61.

---

### Issue D: End of Pagination Crash on Last Page (HTTP 404)
* **What Happened**:
  ```kotlin
  val endOfPaginationReached = characters.isEmpty()
  ```
* **The Failure**: The Rick and Morty API has a fixed count of 42 pages (826 characters). On page 42, the API returns the final 6 characters; `characters.isEmpty()` is `false`.
* The mediator assumed more pages existed and scheduled `page = 43`.
* The Rick and Morty API does not return an empty array for out-of-range pages; it returns `HTTP 404 Not Found` (`{"error": "There is nothing here"}`).
* The mediator caught `HttpException` and returned `MediatorResult.Error`, showing a "Failed to load more characters" error banner and retry button instead of cleanly marking the end of the feed.

---

## 2. Is `RemoteMediator` the Right Approach?

Choosing between `RemoteMediator` and a pure network `PagingSource` depends on whether offline data persistence is a core product requirement:

| Feature / Consideration | Network-Only `PagingSource<Int, CharacterDto>` | `RemoteMediator` + Room (Current Architecture) |
| :--- | :--- | :--- |
| **Offline Viewing** | Images cached by Coil; character metadata not available offline. | Full offline browsing of previously viewed character metadata and searches. |
| **Architectural Complexity** | Low (~30 lines of code). No DAOs, no key tables, no DB transactions. | High. Requires Room schema, `RemoteKeysEntity`, transaction boundary management. |
| **State Synchronization** | Zero risk of stale database cache or key-mismatch bugs. | Search results and feed pagination must be cleanly partitioned in Room. |
| **Launch Performance** | Requires network call before rendering items. | Instant launch rendering previously cached items from SQLite. |
| **Recommendation** | Ideal for simple online catalogs where data is ephemeral. | **Right approach for FrameFlow**, aligning with `ARCHITECTURE.md` (offline-first single source of truth). |

---

## 3. Implemented Fixes

To resolve all missing items and state bugs while maintaining the offline-first architecture, the following fixes were applied:

### 1. Main Feed Isolated via `INNER JOIN remote_keys` ([`CharacterDao.kt`](file:///Users/denny/Workspace/dev/android/FrameFlow/app/src/main/java/com/dennymathew/frameflow/data/local/CharacterDao.kt))
```sql
SELECT characters.* FROM characters 
INNER JOIN remote_keys ON characters.id = remote_keys.characterId 
ORDER BY characters.id ASC
```
* **Benefit**: Guarantees that only characters loaded by feed pagination (which have valid `remote_keys`) appear in the main feed.
* Search results and detail fetches can still be cached in `characters` without leaking into the main feed, avoiding visual gaps and avoiding null key lookup failures.

### 2. Standardized `LoadType.REFRESH` and `LoadType.PREPEND` ([`CharacterRemoteMediator.kt`](file:///Users/denny/Workspace/dev/android/FrameFlow/app/src/main/java/com/dennymathew/frameflow/data/remote/CharacterRemoteMediator.kt))
* `LoadType.REFRESH` always fetches `STARTING_PAGE_INDEX` (page 1) when the database is cleared.
* `LoadType.PREPEND` immediately returns `MediatorResult.Success(endOfPaginationReached = true)` because pagination is forward-only from page 1.

### 3. Safe `APPEND` Termination ([`CharacterRemoteMediator.kt`](file:///Users/denny/Workspace/dev/android/FrameFlow/app/src/main/java/com/dennymathew/frameflow/data/remote/CharacterRemoteMediator.kt))
```kotlin
val nextKey = remoteKeys?.nextKey
    ?: return MediatorResult.Success(endOfPaginationReached = remoteKeys != null)
```
* If `remoteKeys` is `null` (data still populating), `endOfPaginationReached` is `false`, preventing premature pagination shutdown.

### 4. Added `PageInfo` Deserialization & HTTP 404 Graceful Handling ([`RickAndMortyResponse.kt`](file:///Users/denny/Workspace/dev/android/FrameFlow/app/src/main/java/com/dennymathew/frameflow/data/remote/RickAndMortyResponse.kt) & [`CharacterRemoteMediator.kt`](file:///Users/denny/Workspace/dev/android/FrameFlow/app/src/main/java/com/dennymathew/frameflow/data/remote/CharacterRemoteMediator.kt))
* Deserialized `info.next`. If `info.next == null` (e.g. on page 42), `endOfPaginationReached = true` is set immediately.
* Wrapped API call to treat HTTP 404 as `endOfPaginationReached = true` if an out-of-bounds page is ever requested, eliminating the bottom error banner on the last page.
* Added `response.info?.next == null` early-exit to [`CharacterRepository.syncSearchCharacters`](file:///Users/denny/Workspace/dev/android/FrameFlow/app/src/main/java/com/dennymathew/frameflow/data/repository/CharacterRepository.kt) to prevent redundant API calls when search results have fewer than 3 pages.
