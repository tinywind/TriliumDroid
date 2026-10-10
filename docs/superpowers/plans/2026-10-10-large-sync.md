# Large attachment sync implementation plan

> Execution: inline, following systematic-debugging and test-driven-development. The user authorized implementation, build, installation, and device verification in this checkout.

**Goal:** Complete initial synchronization on the connected S25, including large attachments, without deleting the existing app or server data.

**Architecture:** Stream sync JSON and decode each content value to a temporary file. Apply one entity at a time, writing large blobs in bounded chunks. Commit each response and its checkpoint atomically. Install the existing debug application ID alongside the official release because the release signing key is unavailable.

**Tech stack:** Kotlin, Android SQLite, Jackson Core streaming parser, JUnit, Gradle, GitHub Actions, ADB.

**Spec:** User request in this conversation: fork under tinywind, clone under ~/Projects, fix, build, install, preserve existing data, and verify large attachments.

## Constraints and review focus

- Preserve the original package and server contents. Never clear app data or uninstall the official package.
- Do not place passwords, tokens, personal notes, or device dumps in Git.
- Keep Java heap usage bounded when parsing attachments larger than the 256 MiB app heap.
- Verify malformed/truncated responses clean temporary files and do not advance checkpoints.
- Verify binary zero bytes, null and empty content, arbitrary JSON field order, and unknown metadata.
- Verify a large SQLite blob by size and hash, not merely a success notification.

## Tasks

- [x] Reproduce baseline failure: official alpha19 crashes repeatedly allocating 47,504,464 bytes at Sync.kt blob decoding; 256 MiB heap limit, server checkpoint 14670.
- [x] Fork tinywind/TriliumDroid and clone C:/Users/tinywind/Projects/TriliumDroid; use bugfix/stream-large-sync-attachments.
- [ ] Add streaming parser regressions, including a blob larger than the test JVM heap and interrupted input.
- [ ] Implement bounded content ingestion in StreamingSyncReader.java, ConnectionUtil.kt, and Sync.kt; add atomic page transaction and cleanup.
- [ ] Build a distinguishable debug APK using the existing .debug ID; run unit and focused Android storage tests.
- [ ] Install alongside official release, configure privately, and synchronize against the existing server.
- [ ] Verify completion, large attachment hashes, crash logs, and continued presence of the official app.
- [ ] Review changes and publish the working branch to the requested fork; report evidence and limitations.

## Progress

2026-10-10: Original app is not debuggable; run-as cannot extract its private database. Side-by-side installation preserves its data without relying on an unavailable release signing key. Local JDK is available through IntelliJ; no Android SDK found in the usual locations, so use GitHub Actions for Android builds.
